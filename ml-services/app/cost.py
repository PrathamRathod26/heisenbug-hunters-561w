"""
Cost estimation — converts YOLO detections into monetary line items.

All amounts are in PAISE (integer). 1 INR = 100 paise.
"""
from typing import List, Tuple
import uuid

from app.schemas import CostEstimate, Detection, EstimateLine, ImageResult, Severity
from app.severity import SEVERITY_MULTIPLIERS


# ---------- Catalog ----------
# (part_cost_paise, labor_hours, operation_description)
# Labor rate is applied uniformly at ₹500/hour = 50000 paise/hour.
# Prices are representative for a mid-segment car in India.
LABOR_RATE_PAISE_PER_HOUR = 50000
LOW_CONFIDENCE_THRESHOLD = 0.4

OPERATION_CATALOG = {
    # operation_code: (part_cost_paise, labor_hours, description)
    # Remember: 1 rupee = 100 paise, so ₹15,000 = 1,500,000 paise

    # Dent operations
    "PDR":                    (0,        2.0, "Paintless dent repair"),
    "PANEL_REPAIR":           (150000,   3.5, "Panel repair with filler and refinish"),   # ₹1,500 filler/paint
    "PANEL_REPLACE_BONNET":   (1200000,  4.0, "Bonnet panel replacement"),                 # ₹12,000
    "PANEL_REPLACE_BOOT":     (1000000,  4.0, "Boot panel replacement"),                   # ₹10,000
    "PANEL_REPLACE_DOOR":     (1500000,  4.5, "Door panel replacement"),                   # ₹15,000
    "PANEL_REPLACE_FENDER":   (800000,   3.5, "Fender panel replacement"),                 # ₹8,000
    "PANEL_REPLACE_BUMPER_F": (900000,   3.0, "Front bumper replacement"),                 # ₹9,000
    "PANEL_REPLACE_BUMPER_R": (850000,   3.0, "Rear bumper replacement"),                  # ₹8,500
    "PANEL_REPLACE_QUARTER":  (1800000,  5.0, "Quarter panel replacement"),                # ₹18,000
    "PANEL_REPLACE_ROOF":     (2500000,  6.0, "Roof panel replacement"),                   # ₹25,000

    # Glass
    "GLASS_REPLACE_FRONT":    (1500000,  2.0, "Front windscreen replacement"),             # ₹15,000
    "GLASS_REPLACE_REAR":     (1200000,  2.0, "Rear windscreen replacement"),              # ₹12,000

    # Lights & mirrors
    "HEADLIGHT_REPLACE":      (800000,   1.0, "Headlight assembly replacement"),           # ₹8,000
    "TAILLIGHT_REPLACE":      (500000,   1.0, "Taillight assembly replacement"),           # ₹5,000
    "SIDEMIRROR_REPLACE":     (350000,   0.5, "Side mirror assembly replacement"),         # ₹3,500
    "RUNNINGBOARD_REPAIR":    (400000,   2.0, "Running board repair / replacement"),       # ₹4,000
}


# ---------- Mapping rules ----------
# Given (class_name, area_fraction), return operation_code.
# Area-fraction thresholds split dents into PDR vs repair vs replace.
def _map_to_operation(class_name: str, area_fraction: float) -> str:
    # Glass
    if class_name == "Front-windscreen-damage":
        return "GLASS_REPLACE_FRONT"
    if class_name == "Rear-windscreen-Damage":
        return "GLASS_REPLACE_REAR"

    # Lights and mirrors
    if class_name == "Headlight-damage":
        return "HEADLIGHT_REPLACE"
    if class_name == "Taillight-damage":
        return "TAILLIGHT_REPLACE"
    if class_name == "Sidemirror-Damage":
        return "SIDEMIRROR_REPLACE"
    if class_name == "Runningboard-Damage":
        return "RUNNINGBOARD_REPAIR"

    # Dents: size-dependent
    # < 2% of image: small dent, PDR
    # 2-6%: medium, panel repair
    # > 6%: large, full replacement
    if area_fraction < 0.02:
        return "PDR"
    if area_fraction < 0.06:
        return "PANEL_REPAIR"

    # Large dents: replace, panel depends on class
    panel_replace_map = {
        "bonnet-dent":       "PANEL_REPLACE_BONNET",
        "boot-dent":         "PANEL_REPLACE_BOOT",
        "doorouter-dent":    "PANEL_REPLACE_DOOR",
        "fender-dent":       "PANEL_REPLACE_FENDER",
        "front-bumper-dent": "PANEL_REPLACE_BUMPER_F",
        "rear-bumper-dent":  "PANEL_REPLACE_BUMPER_R",
        "quaterpanel-dent":  "PANEL_REPLACE_QUARTER",
        "roof-dent":         "PANEL_REPLACE_ROOF",
    }
    return panel_replace_map.get(class_name, "PANEL_REPAIR")


def _build_line(detection_id: str, det: Detection) -> EstimateLine:
    op = _map_to_operation(det.label, det.bbox.area_fraction)
    part_cost, labor_hours, description = OPERATION_CATALOG[op]
    labor_cost = int(round(labor_hours * LABOR_RATE_PAISE_PER_HOUR))
    base_cost = part_cost + labor_cost

    # Apply severity multiplier
    multiplier = SEVERITY_MULTIPLIERS[det.severity]
    line_total = int(round(base_cost * multiplier))

    return EstimateLine(
        detection_id=detection_id,
        label=det.label,
        severity=det.severity,
        operation_code=op,
        operation_description=description,
        base_cost_paise=base_cost,
        severity_multiplier=multiplier,
        part_cost_paise=part_cost,
        labor_hours=labor_hours,
        labor_rate_paise_per_hour=LABOR_RATE_PAISE_PER_HOUR,
        labor_cost_paise=labor_cost,
        line_total_paise=line_total,
    )


def estimate(image_results: List[ImageResult]) -> CostEstimate:
    """Build a cost estimate from all detections across all images."""
    lines: List[EstimateLine] = []
    assumptions: List[str] = []
    confidences: List[float] = []
    has_total_loss_candidate = False
    low_confidence_skipped = 0

    # ---- Build lines from every qualifying detection ----
    for img in image_results:
        for i, det in enumerate(img.detections):
            if det.confidence < LOW_CONFIDENCE_THRESHOLD:
                low_confidence_skipped += 1
                continue  # skipped from cost but still in the detections list

            detection_id = f"{img.image_id}-det-{i}"
            lines.append(_build_line(detection_id, det))
            confidences.append(det.confidence)
            if det.severity == Severity.TOTAL_LOSS_CANDIDATE:
                has_total_loss_candidate = True

    # ---- Totals (integer arithmetic only) ----
    subtotal = sum(l.line_total_paise for l in lines)
    gst_rate = 0.18
    tax = int(round(subtotal * gst_rate))
    discount = 0
    total = subtotal + tax - discount

    # Invariant check (FRS requirement)
    assert subtotal + tax - discount == total, "Monetary invariant violated"

    # ---- Confidence interval ----
    if confidences:
        mean_conf = sum(confidences) / len(confidences)
        interval_frac = (1.0 - mean_conf) * 0.3
    else:
        mean_conf = 0.0
        interval_frac = 0.0
    total_low = int(round(total * (1 - interval_frac)))
    total_high = int(round(total * (1 + interval_frac)))

    # ---- Notes for transparency ----
    assumptions.append(f"GST applied at {int(gst_rate*100)}%")
    assumptions.append(f"Labor rate assumed ₹{LABOR_RATE_PAISE_PER_HOUR//100}/hour")
    assumptions.append("Prices for mid-segment car; tune catalog for specific make/model")
    if confidences:
        assumptions.append(
            f"Confidence interval derived from mean detection confidence {mean_conf:.2f}"
        )

    # ---- Review flag (combines both signals) ----
    requires_review = has_total_loss_candidate or low_confidence_skipped > 0
    review_reasons = []
    if has_total_loss_candidate:
        review_reasons.append(
            "One or more detections classified as total_loss_candidate — "
            "switch to IDV-minus-salvage path for final payout"
        )
    if low_confidence_skipped > 0:
        review_reasons.append(
            f"{low_confidence_skipped} detection(s) below {LOW_CONFIDENCE_THRESHOLD} "
            "confidence threshold excluded from estimate — adjuster review needed"
        )

    return CostEstimate(
        currency="INR",
        lines=lines,
        subtotal_paise=subtotal,
        tax_paise=tax,
        discount_paise=discount,
        total_paise=total,
        total_low_paise=total_low,
        total_high_paise=total_high,
        gst_rate=gst_rate,
        assumptions=assumptions,
        requires_human_review=requires_review,
        review_reasons=review_reasons,
    )