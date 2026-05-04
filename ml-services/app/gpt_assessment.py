"""
GPT-4o Vision-powered surveyor assessment.

Takes detection + cost output and the original images, asks GPT-4o to produce
a plain-language assessment report suitable for adjuster review.

Per FRS §3.2: every OpenAI invocation must include a deterministic system
prompt version identifier for reproducibility.
"""
import base64
import json
import os
from datetime import datetime, timezone
from typing import List, Optional, Tuple

from openai import OpenAI

from app.schemas import (
    CostEstimate,
    ImageObservation,
    ImageResult,
    NarrativePhotoConsistency,
    SurveyorAssessment,
)


# System prompt is versioned per FRS §3.2. Bump the version if you change the text.
SYSTEM_PROMPT_VERSION = "surveyor-v1.0.0"

SYSTEM_PROMPT = """You are an expert motor insurance surveyor in India, reviewing damage \
photos and an AI-generated pre-assessment. Your job:

1. Produce a brief, plain-language summary of the damage visible across the photos.
2. For each image, note anything the AI detector may have missed or gotten wrong.
3. Give a severity verdict: minor, moderate, severe, or total_loss.
4. Give a repair recommendation: repair, replace, write-off, or inspect.
5. If a claimant narrative is provided, flag any inconsistency between what the \
narrative says and what the photos actually show.
6. Rate your own confidence in your assessment on a 0.0-1.0 scale.

You MUST respond with valid JSON matching this exact schema — no markdown, no \
explanation outside the JSON:

{
  "summary": "string",
  "severity_verdict": "minor|moderate|severe|total_loss",
  "repair_recommendation": "repair|replace|write-off|inspect",
  "image_observations": [
    {
      "image_id": "string (must match provided image_id)",
      "observation": "string",
      "additional_damage_noted": ["string", ...],
      "disputes_detection": boolean,
      "dispute_reason": "string or null"
    }
  ],
  "narrative_consistency": {
    "consistent": boolean,
    "confidence": number 0.0-1.0,
    "mismatches": ["string", ...]
  } or null if no narrative provided,
  "assessment_confidence": number 0.0-1.0
}

Be factual. Do not speculate beyond what is visible. If the photos are unclear, \
say so and lower your assessment_confidence accordingly."""


def _encode_image_to_data_url(image_bytes: bytes, mime: str = "image/jpeg") -> str:
    """OpenAI Vision API accepts images as base64 data URLs."""
    b64 = base64.b64encode(image_bytes).decode("utf-8")
    return f"data:{mime};base64,{b64}"


def _build_user_prompt(
    image_results: List[ImageResult],
    cost_estimate: Optional[CostEstimate],
    narrative: Optional[str],
) -> str:
    """Summarize the YOLO findings for GPT as structured context."""
    lines = ["CONTEXT FROM AI DETECTION MODULE:\n"]
    for img in image_results:
        lines.append(f"\nImage '{img.image_id}' ({img.width}x{img.height}):")
        if not img.detections:
            lines.append("  - No damage detected by CV model.")
        for det in img.detections:
            lines.append(
                f"  - {det.label} (confidence {det.confidence:.2f}, "
                f"severity={det.severity}, "
                f"bbox_area={det.bbox.area_fraction:.1%})"
            )

    if cost_estimate:
        total_inr = cost_estimate.total_paise / 100
        lines.append(
            f"\nAI-generated cost estimate: ₹{total_inr:,.2f} "
            f"(range ₹{cost_estimate.total_low_paise/100:,.0f} – "
            f"₹{cost_estimate.total_high_paise/100:,.0f})"
        )
        if cost_estimate.requires_human_review:
            lines.append(
                f"Flagged for review: {'; '.join(cost_estimate.review_reasons)}"
            )

    if narrative:
        lines.append(f"\nCLAIMANT NARRATIVE:\n\"{narrative}\"")
    else:
        lines.append("\nNo claimant narrative provided.")

    lines.append(
        "\nProvide your surveyor assessment as JSON per the schema in the system prompt."
    )
    return "\n".join(lines)


def generate_assessment(
    image_results: List[ImageResult],
    images_raw: List[Tuple[str, bytes]],   # [(image_id, bytes), ...]
    cost_estimate: Optional[CostEstimate],
    narrative: Optional[str] = None,
) -> Optional[SurveyorAssessment]:
    """
    Call GPT-4o Vision with all images + YOLO context.
    Returns None if API key missing or call fails — this is a best-effort enhancement.
    """
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        return None  # silently skip if no key configured

    client = OpenAI(api_key=api_key)

    # Build the multimodal message: text context + all images inline
    content_parts = [
        {
            "type": "text",
            "text": _build_user_prompt(image_results, cost_estimate, narrative),
        }
    ]
    for image_id, raw_bytes in images_raw:
        content_parts.append({
            "type": "image_url",
            "image_url": {
                "url": _encode_image_to_data_url(raw_bytes),
                "detail": "high",   # use "low" to save cost; "high" for better accuracy
            },
        })

    try:
        response = client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": content_parts},
            ],
            response_format={"type": "json_object"},
            max_tokens=1500,
            temperature=0.2,   # low temp for consistent, factual output
        )
    except Exception as e:
        # Don't let GPT failures kill the whole request — detection + cost already succeeded
        print(f"[gpt_assessment] OpenAI call failed: {e}")
        return None

    try:
        raw_json = response.choices[0].message.content
        parsed = json.loads(raw_json)
    except (json.JSONDecodeError, AttributeError, IndexError) as e:
        print(f"[gpt_assessment] Failed to parse GPT response: {e}")
        return None

    # Map the parsed JSON into our Pydantic model
    try:
        image_obs = [
            ImageObservation(**obs) for obs in parsed.get("image_observations", [])
        ]
        narrative_consistency = None
        if parsed.get("narrative_consistency"):
            narrative_consistency = NarrativePhotoConsistency(
                **parsed["narrative_consistency"]
            )

        return SurveyorAssessment(
            summary=parsed["summary"],
            severity_verdict=parsed["severity_verdict"],
            repair_recommendation=parsed["repair_recommendation"],
            image_observations=image_obs,
            narrative_consistency=narrative_consistency,
            assessment_confidence=float(parsed["assessment_confidence"]),
            system_prompt_version=SYSTEM_PROMPT_VERSION,
            generated_at=datetime.now(timezone.utc).isoformat(),
        )
    except (KeyError, TypeError, ValueError) as e:
        print(f"[gpt_assessment] Schema mismatch in GPT response: {e}")
        return None