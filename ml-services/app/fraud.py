"""
Python-side fraud signals for FR-5.

Computes image-based signals only:
  1. EXIF timestamp vs declared incident time (deterministic hard rule)
  2. Camera model consistency across uploaded images
  3. Duplicate image detection (perceptual hashing)
  4. Error Level Analysis (ELA) for digital tampering
  5. Narrative↔photo consistency (lifted from GPT-4o assessment)

Returns a FraudResult with a partial 0-100 score. Behavioral and network
signals are Spring Boot's responsibility and are merged server-side.
"""
from __future__ import annotations

import io
from collections import Counter
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Dict, List, Optional, Tuple

import exifread
import imagehash
import numpy as np
from PIL import Image

from app.schemas import (
    FraudResult,
    FraudSignal,
    ImageResult,
    SurveyorAssessment,
)

FRAUD_MODEL_VERSION = "fraud-rules-v1.0"

# Signal weights for the composite score. Tune based on real data later.
# Weights must sum to something sensible; we normalize at the end.
WEIGHTS = {
    "exif_timestamp_check": 0.30,
    "camera_consistency": 0.10,
    "duplicate_images": 0.20,
    "ela_tampering": 0.20,
    "narrative_consistency": 0.20,
}


# ============================================================================
# Signal 1: EXIF timestamp check  (deterministic hard rule per AC-5.1.2)
# ============================================================================

def _parse_exif_datetime(tags: dict) -> Optional[datetime]:
    """Extract DateTimeOriginal (or fallbacks) from exifread tags."""
    for tag_name in ("EXIF DateTimeOriginal", "Image DateTime", "EXIF DateTimeDigitized"):
        if tag_name in tags:
            raw = str(tags[tag_name]).strip()
            # EXIF format: "2026:04:24 13:45:22"
            try:
                return datetime.strptime(raw, "%Y:%m:%d %H:%M:%S")
            except ValueError:
                continue
    return None


def _parse_incident_time(incident_time_str: Optional[str]) -> Optional[datetime]:
    """Parse ISO 8601 timestamp string to naive datetime (ignore tz for comparison)."""
    if not incident_time_str:
        return None
    try:
        dt = datetime.fromisoformat(incident_time_str.replace("Z", "+00:00"))
        # Normalize to naive for comparison with EXIF (EXIF has no timezone)
        if dt.tzinfo:
            dt = dt.astimezone(timezone.utc).replace(tzinfo=None)
        return dt
    except ValueError:
        return None


def check_exif_timestamp(
        images_raw: List[Tuple[str, bytes]],
        incident_time_str: Optional[str],
) -> Tuple[FraudSignal, List[str]]:
    """
    Signal 1 + hard rule.
    Returns (signal, camera_models_seen).
    """
    rule_hits: List[str] = []
    details: Dict = {
        "per_image": [],
        "incident_time": incident_time_str,
    }
    score = 0.0
    cameras_seen: List[str] = []
    exif_missing_count = 0
    images_without_exif: List[str] = []

    incident_time = _parse_incident_time(incident_time_str)

    for image_id, raw_bytes in images_raw:
        tags = exifread.process_file(io.BytesIO(raw_bytes), details=False)
        exif_dt = _parse_exif_datetime(tags)

        # Capture camera model for Signal 2
        camera = None
        if "Image Model" in tags:
            camera = str(tags["Image Model"]).strip()
            cameras_seen.append(camera)

        per_image = {
            "image_id": image_id,
            "exif_datetime": exif_dt.isoformat() if exif_dt else None,
            "camera_model": camera,
        }

        if exif_dt is None:
            exif_missing_count += 1
            images_without_exif.append(image_id)
            per_image["exif_status"] = "missing"
        else:
            per_image["exif_status"] = "present"

            # ---- Hard rule: EXIF > incident + 24h ⇒ score ≥ 85 (AC-5.1.2) ----
            if incident_time and exif_dt > incident_time + timedelta(hours=24):
                delta = exif_dt - incident_time
                rule_hit = (
                    f"EXIF timestamp of {image_id} is {delta.total_seconds() / 3600:.1f}h "
                    f"AFTER declared incident time"
                )
                rule_hits.append(rule_hit)
                per_image["rule_hit"] = "exif_after_incident"
                score = max(score, 85.0)

            # ---- Soft signal: EXIF significantly before incident ----
            elif incident_time and exif_dt < incident_time - timedelta(days=30):
                per_image["rule_hit"] = "exif_long_before_incident"
                score = max(score, 40.0)

        details["per_image"].append(per_image)

    # Missing-EXIF soft signal
    if exif_missing_count > 0:
        frac_missing = exif_missing_count / len(images_raw)
        # All EXIF stripped? Suspicious. Some missing? Mildly suspicious.
        score = max(score, frac_missing * 30.0)
        details["exif_missing_fraction"] = round(frac_missing, 2)
        details["images_without_exif"] = images_without_exif

    # If no incident_time was provided, we can only check presence
    if incident_time is None:
        details["note"] = "incident_time not provided; only EXIF presence checked"

    signal = FraudSignal(
        name="exif_timestamp_check",
        score=min(score, 100.0),
        weight=WEIGHTS["exif_timestamp_check"],
        rule_hits=rule_hits,
        details=details,
    )
    return signal, cameras_seen


# ============================================================================
# Signal 2: Camera model consistency
# ============================================================================

def check_camera_consistency(cameras_seen: List[str]) -> FraudSignal:
    """
    A single claim should typically have one or two cameras (phone + maybe a
    DSLR). Three or more distinct models is unusual and weakly suspicious.
    """
    rule_hits: List[str] = []
    details: Dict = {"cameras_seen": cameras_seen}
    score = 0.0

    # Count distinct non-empty models
    distinct = Counter(cameras_seen)
    unique_count = len(distinct)
    details["unique_cameras"] = unique_count
    details["distribution"] = dict(distinct)

    if unique_count >= 3:
        score = 50.0
        rule_hits.append(f"{unique_count} distinct camera models in one claim")
    elif unique_count == 2:
        score = 15.0

    return FraudSignal(
        name="camera_consistency",
        score=score,
        weight=WEIGHTS["camera_consistency"],
        rule_hits=rule_hits,
        details=details,
    )


# ============================================================================
# Signal 3: Duplicate image detection (perceptual hash)
# ============================================================================

def check_duplicate_images(images_raw: List[Tuple[str, bytes]]) -> FraudSignal:
    """
    Compute perceptual hash for each image. Pairs with Hamming distance <=4
    are near-duplicates. Re-using images across claims or submitting the same
    photo under different filenames is a classic fraud pattern.
    """
    rule_hits: List[str] = []
    details: Dict = {"duplicate_pairs": []}
    score = 0.0

    hashes: List[Tuple[str, imagehash.ImageHash]] = []
    for image_id, raw_bytes in images_raw:
        try:
            pil = Image.open(io.BytesIO(raw_bytes))
            h = imagehash.phash(pil, hash_size=16)
            hashes.append((image_id, h))
        except Exception:
            continue

    # Compare every pair
    n_duplicates = 0
    for i in range(len(hashes)):
        for j in range(i + 1, len(hashes)):
            id_a, hash_a = hashes[i]
            id_b, hash_b = hashes[j]
            distance = int(hash_a - hash_b)  # Hamming distance (cast numpy -> python int)
            if distance <= 4:
                n_duplicates += 1
                details["duplicate_pairs"].append({
                    "image_a": id_a,
                    "image_b": id_b,
                    "hamming_distance": distance,
                })
                rule_hits.append(
                    f"Near-duplicate images: {id_a} and {id_b} (distance {distance})"
                )

    if n_duplicates >= 2:
        # Multiple dup pairs is very suspicious
        score = 80.0
    elif n_duplicates == 1:
        score = 50.0

    details["duplicate_pair_count"] = n_duplicates
    return FraudSignal(
        name="duplicate_images",
        score=score,
        weight=WEIGHTS["duplicate_images"],
        rule_hits=rule_hits,
        details=details,
    )


# ============================================================================
# Signal 4: Error Level Analysis (ELA)
# ============================================================================

def _ela_score_single(raw_bytes: bytes) -> Optional[float]:
    """
    Compute ELA score for one image. Returns 0-100 tampering indicator,
    or None if the image can't be processed.

    Method: re-save image at JPEG quality 90; measure mean absolute pixel
    difference between original and re-saved. Regions edited after initial
    save compress differently, creating high local error values.
    """
    try:
        original = Image.open(io.BytesIO(raw_bytes)).convert("RGB")
    except Exception:
        return None

    # Re-save as JPEG at q=90
    buf = io.BytesIO()
    original.save(buf, format="JPEG", quality=90)
    buf.seek(0)
    resaved = Image.open(buf).convert("RGB")

    # Pixel-wise difference
    arr_orig = np.array(original, dtype=np.int16)
    arr_resaved = np.array(resaved, dtype=np.int16)
    diff = np.abs(arr_orig - arr_resaved)

    # Mean error scaled to 0-100 tampering indicator
    # Typical untampered JPEG: mean_diff ~2-5; tampered regions spike much higher
    mean_diff = float(diff.mean())
    max_diff = float(diff.max())

    # Score heuristic: normal images ~0-20, tampered often 30+
    # Combine mean (global) and max (local spike) signals
    score = min(100.0, mean_diff * 5 + (max_diff / 255.0) * 20)
    return score


def check_ela(images_raw: List[Tuple[str, bytes]]) -> FraudSignal:
    rule_hits: List[str] = []
    details: Dict = {"per_image": []}
    scores: List[float] = []

    for image_id, raw_bytes in images_raw:
        ela = _ela_score_single(raw_bytes)
        if ela is None:
            details["per_image"].append({"image_id": image_id, "ela_score": None})
            continue
        details["per_image"].append({"image_id": image_id, "ela_score": round(ela, 2)})
        scores.append(ela)

        if ela >= 50:
            rule_hits.append(
                f"High ELA tampering indicator on {image_id}: {ela:.1f}"
            )

    # Signal score = max ELA across images, since one tampered image is enough
    signal_score = max(scores) if scores else 0.0

    return FraudSignal(
        name="ela_tampering",
        score=signal_score,
        weight=WEIGHTS["ela_tampering"],
        rule_hits=rule_hits,
        details=details,
    )


# ============================================================================
# Signal 5: Narrative consistency (lifted from GPT-4o assessment)
# ============================================================================

def check_narrative_consistency(
        assessment: Optional[SurveyorAssessment],
) -> FraudSignal:
    """
    Already computed by GPT-4o in the surveyor assessment; we just lift it
    into the fraud result and convert to 0-100 score.
    """
    rule_hits: List[str] = []
    details: Dict = {}
    score = 0.0

    if assessment is None or assessment.narrative_consistency is None:
        details["status"] = "not_available"
        return FraudSignal(
            name="narrative_consistency",
            score=0.0,
            weight=WEIGHTS["narrative_consistency"],
            rule_hits=[],
            details=details,
        )

    nc = assessment.narrative_consistency
    details["consistent"] = nc.consistent
    details["gpt_confidence"] = nc.confidence
    details["mismatches"] = nc.mismatches

    if not nc.consistent:
        # Weight the score by GPT's own confidence in its verdict
        score = 70.0 * nc.confidence
        for mismatch in nc.mismatches:
            rule_hits.append(f"Narrative mismatch: {mismatch}")

    return FraudSignal(
        name="narrative_consistency",
        score=score,
        weight=WEIGHTS["narrative_consistency"],
        rule_hits=rule_hits,
        details=details,
    )


# ============================================================================
# Composite score + orchestration
# ============================================================================

def compute_fraud_result(
        images_raw: List[Tuple[str, bytes]],
        incident_time_str: Optional[str],
        surveyor_assessment: Optional[SurveyorAssessment],
) -> FraudResult:
    """Run all signals and combine into a partial fraud score."""
    signals: List[FraudSignal] = []
    successes = 0
    total_signals = 5

    # Signal 1 + 2 (EXIF also captures camera models)
    try:
        exif_signal, cameras_seen = check_exif_timestamp(images_raw, incident_time_str)
        signals.append(exif_signal)
        successes += 1
    except Exception as e:
        print(f"[fraud] exif check failed: {e}")
        cameras_seen = []

    try:
        signals.append(check_camera_consistency(cameras_seen))
        successes += 1
    except Exception as e:
        print(f"[fraud] camera check failed: {e}")

    try:
        signals.append(check_duplicate_images(images_raw))
        successes += 1
    except Exception as e:
        print(f"[fraud] duplicate check failed: {e}")

    try:
        signals.append(check_ela(images_raw))
        successes += 1
    except Exception as e:
        print(f"[fraud] ela check failed: {e}")

    try:
        signals.append(check_narrative_consistency(surveyor_assessment))
        successes += 1
    except Exception as e:
        print(f"[fraud] narrative check failed: {e}")

    # ---- Composite score: weighted average, then rule overlay ----
    if signals:
        total_weight = sum(s.weight for s in signals)
        weighted_sum = sum(s.score * s.weight for s in signals)
        raw_composite = weighted_sum / total_weight if total_weight else 0.0
    else:
        raw_composite = 0.0

    # Hard rule overlay: if any rule_hits exist, enforce minimum score
    all_rule_hits = []
    for s in signals:
        all_rule_hits.extend(s.rule_hits)

    composite = raw_composite
    # AC-5.1.2: EXIF > incident by >24h → score ≥ 85
    for s in signals:
        if s.name == "exif_timestamp_check" and s.score >= 85:
            composite = max(composite, 85.0)

    # Top features: signals with highest weighted contribution
    signals_sorted = sorted(
        signals,
        key=lambda s: s.score * s.weight,
        reverse=True,
    )
    top_features = [s.name for s in signals_sorted[:3] if s.score > 0]

    completeness = successes / total_signals

    return FraudResult(
        partial_score=round(min(composite, 100.0), 2),
        signals=signals,
        top_features=top_features,
        rule_hits=all_rule_hits,
        requires_investigation=bool(all_rule_hits),
        completeness=round(completeness, 2),
        fraud_model_version=FRAUD_MODEL_VERSION,
    )