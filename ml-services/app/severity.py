"""
Per-detection severity classification.

Severity is a derived signal, not predicted by the YOLO model. We combine:
  - class weight (some damage types are inherently worse)
  - bbox area fraction (bigger damage = worse)
  - detection confidence (penalize low-confidence predictions slightly)

This produces a numeric score, thresholded into minor/moderate/severe.
"""
from app.schemas import Severity


# Per-class base severity weight. Higher = worse even at the same size.
# Tunable by admin per FRS §4.4.
CLASS_WEIGHTS = {
    # --- Structural dents: hardest and most expensive ---
    "roof-dent":          5.0,  # structural, hardest to fix
    "quaterpanel-dent":   4.0,  # often structural, welded panel

    # --- Major visible panels ---
    "doorouter-dent":     3.5,  # door panel, visible, often replaced
    "bonnet-dent":        3.0,
    "boot-dent":          3.0,

    # --- Front/rear dents: usually replaced anyway ---
    "fender-dent":        2.5,
    "front-bumper-dent":  2.0,  # bumpers designed to deform
    "rear-bumper-dent":   2.0,

    # --- Glass: critical but standard part swap ---
    "Front-windscreen-damage":  3.0,
    "Rear-windscreen-Damage":   3.0,

    # --- Lights & mirrors: contained part replacement ---
    "Headlight-damage":   2.5,
    "Taillight-damage":   2.5,
    "Sidemirror-Damage":  1.5,
    "Runningboard-Damage": 2.0,
}


def compute_severity_score(class_name: str, area_fraction: float, confidence: float) -> float:
    """
    Compute a raw severity score. Higher = worse.
    Typical range: 0.0 (trivial) to 8.0+ (catastrophic).
    """
    weight = CLASS_WEIGHTS.get(class_name, 1.0)
    # Core formula: class weight × area × confidence, scaled
    return weight * area_fraction * confidence * 10.0


def classify_severity(score: float) -> Severity:
    if score >= 8.0:
        return Severity.TOTAL_LOSS_CANDIDATE  # add this enum value
    if score >= 3.0:
        return Severity.SEVERE
    if score >= 1.0:
        return Severity.MODERATE
    return Severity.MINOR


# Severity → cost multiplier. Applied on top of base operation cost.
# A severe dent in the same panel costs more than a minor one of the same type,
# because more labor / ancillary parts / paint blending are needed.
SEVERITY_MULTIPLIERS = {
    Severity.MINOR:                 0.75,
    Severity.MODERATE:              1.00,
    Severity.SEVERE:                1.40,
    Severity.TOTAL_LOSS_CANDIDATE:  2.00,   # major structural work, aligned panels, paint blending across multiple panels
}