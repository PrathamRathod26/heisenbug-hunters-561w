"""Response models - what the API returns to the caller."""
from typing import List,Optional
from pydantic import BaseModel, Field
from enum import Enum

class ImageObservation(BaseModel):
    """GPT-4o's observation for a single image."""
    image_id: str
    observation: str                          # plain-language description
    additional_damage_noted: List[str] = []   # damages GPT saw that YOLO missed
    disputes_detection: bool = False          # GPT disagrees with a YOLO detection
    dispute_reason: Optional[str] = None


class NarrativePhotoConsistency(BaseModel):
    """Match between policyholder narrative and photos. Null if no narrative given."""
    consistent: bool
    confidence: float                         # GPT's own confidence in this verdict
    mismatches: List[str] = []                # specific inconsistencies found


class SurveyorAssessment(BaseModel):
    """GPT-4o-generated assessment, intended for adjuster review."""
    model_config = {"protected_namespaces": ()}

    summary: str                              # 2-3 sentence summary
    severity_verdict: str                     # minor / moderate / severe / total_loss
    repair_recommendation: str                # repair / replace / write-off / inspect
    image_observations: List[ImageObservation]
    narrative_consistency: Optional[NarrativePhotoConsistency] = None
    assessment_confidence: float              # GPT's confidence in its own answer [0,1]
    model_used: str = "gpt-4o"
    system_prompt_version: str                # required by FRS for auditability
    generated_at: str                         # ISO 8601 timestamp

class BoundingBox(BaseModel):
    """Pixel coordinates of the detection."""
    x1: float
    y1: float
    x2: float
    y2: float
    area_fraction: float = Field(..., description="bbox area / image area")

class Severity(str, Enum):
    MINOR = "minor"
    MODERATE = "moderate"
    SEVERE = "severe"
    TOTAL_LOSS_CANDIDATE = "total_loss_candidate"

class Detection(BaseModel):
    label: str
    confidence: float
    bbox: BoundingBox
    severity: Severity                     # NEW
    severity_score: float


class ImageSource(str, Enum):
    UPLOAD = "upload"
    VIDEO = "video"

class ImageResult(BaseModel):
    image_id: str
    source: ImageSource = ImageSource.UPLOAD
    source_timestamp_sec: float | None = None  # only set for video frames
    width: int
    height: int
    detections: List[Detection]
    latency_ms: int

class EstimateLine(BaseModel):
    model_config = {"protected_namespaces": ()}

    detection_id: str
    label: str
    severity: Severity                     # NEW
    operation_code: str
    operation_description: str
    base_cost_paise: int                   # NEW — cost before severity multiplier
    severity_multiplier: float             # NEW — applied on top of base cost
    part_cost_paise: int = 0
    labor_hours: float = 0.0
    labor_rate_paise_per_hour: int = 0
    labor_cost_paise: int = 0
    line_total_paise: int

class CostEstimate(BaseModel):
    currency: str = "INR"
    lines: List[EstimateLine]
    subtotal_paise: int
    tax_paise: int
    discount_paise: int = 0
    total_paise: int
    total_low_paise: int                       # lower bound of confidence interval
    total_high_paise: int
    gst_rate: float = 0.18
    catalog_version: str = "parts-catalog-v1"
    assumptions: List[str] = []
    requires_human_review: bool = False
    review_reasons: List[str] = []


class FraudSignal(BaseModel):
    """One signal's contribution to the composite fraud score."""
    name: str  # e.g. "exif_timestamp_check"
    score: float  # 0-100, higher = more suspicious
    weight: float  # relative weight in composite
    rule_hits: List[str] = []  # deterministic rules that fired
    details: dict = {}  # signal-specific evidence


class FraudResult(BaseModel):
    """
    Python-computed fraud signals. Partial by design — behavioral and
    network signals must be added by Spring Boot per FR-5 to produce
    the final composite score.

    NFR-S-011: MUST NOT be shown to the policyholder verbatim.
    """
    model_config = {"protected_namespaces": ()}

    partial_score: float  # 0-100, what Python can compute
    signals: List[FraudSignal]
    top_features: List[str]  # top-N contributing signals
    rule_hits: List[str]  # all hard-rule hits across signals
    requires_investigation: bool  # any hard rule fired
    completeness: float  # 0.0-1.0, fraction of signals successfully computed
    fraud_model_version: str  # ruleset version
    caveat: str = (
        "Partial score from image-based signals only. "
        "Spring Boot must combine with behavioral and network signals "
        "per FR-5 for final fraud decision. Do not display to policyholder."
    )

class AnalyzeResponse(BaseModel):
    model_config = {"protected_namespaces": ()}

    model_version: str
    images: List[ImageResult]
    total_detections: int
    cost_estimate: Optional[CostEstimate] = None
    surveyor_assessment: Optional[SurveyorAssessment] = None
    fraud_result: Optional[FraudResult] = None
    claim_aggregate_severity: Optional[Severity] = None   # NEW — Pydantic will accept the string value
    processing_time_ms: int




