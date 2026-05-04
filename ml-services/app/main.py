"""FastAPI application entrypoint."""
import io
import time
import cv2
import numpy as np

from contextlib import asynccontextmanager
from PIL import Image
from typing import List, Optional
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from app.detector import detector
from app.schemas import AnalyzeResponse, ImageSource, Severity
from app.video import extract_frames
from app.cost import estimate as estimate_cost
from dotenv import load_dotenv
from app.fraud import compute_fraud_result
from app.gpt_assessment import generate_assessment
load_dotenv()
load_dotenv()   # reads .env into environment variables


MIN_IMAGES = 3
MAX_IMAGES = 15


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load the model once when the app starts."""
    detector.load()
    yield
    # cleanup on shutdown would go here


app = FastAPI(
    title="UC32 Damage Detection Service",
    version="0.1.0",
    lifespan=lifespan,
)


@app.get("/api/v1/health")
def health():
    return {
        "status": "ok",
        "model_loaded": detector.model is not None,
        "model_version": detector.version,
    }


def _load_image(upload: UploadFile) -> np.ndarray:
    """Read UploadFile bytes -> BGR numpy array."""
    content = upload.file.read()
    if not content:
        raise HTTPException(400, f"Empty file: {upload.filename}")
    try:
        pil = Image.open(io.BytesIO(content)).convert("RGB")
    except Exception as e:
        raise HTTPException(400, f"Unreadable image {upload.filename}: {e}")
    rgb = np.array(pil)
    bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)
    return bgr


@app.post("/api/v1/analyze", response_model=AnalyzeResponse)
async def analyze(
        images: List[UploadFile] = File(...),
        video: Optional[UploadFile] = File(None),
        narrative: Optional[str] = Form(None),
        incident_time: Optional[str] = Form(None),     # NEW — ISO 8601
):
    if len(images) < MIN_IMAGES or len(images) > MAX_IMAGES:
        raise HTTPException(
            status_code=400,
            detail=f"Need {MIN_IMAGES}-{MAX_IMAGES} images, got {len(images)}",
        )

    start = time.perf_counter()
    results = []
    total_detections = 0

    # Process uploaded images
    images_raw: List[tuple[str, bytes]] = []
    for upload in images:
        image_id = upload.filename or "unknown"
        raw_bytes = await upload.read()  # read once, keep bytes
        images_raw.append((image_id, raw_bytes))

        # Convert bytes -> numpy for the detector
        pil = Image.open(io.BytesIO(raw_bytes)).convert("RGB")
        rgb = np.array(pil)
        bgr = cv2.cvtColor(rgb, cv2.COLOR_RGB2BGR)

        result = detector.detect(bgr, image_id=image_id)
        result.source = ImageSource.UPLOAD
        results.append(result)
        total_detections += len(result.detections)

    # Process video frames if provided
    if video is not None:
        video_bytes = await video.read()
        frames = extract_frames(video_bytes)
        video_name = video.filename or "video"
        for i, frame in enumerate(frames):
            frame_id = f"{video_name}-frame-{i:03d}"
            result = detector.detect(frame.image_bgr, image_id=frame_id)
            result.source = ImageSource.VIDEO
            result.source_timestamp_sec = frame.timestamp_sec
            results.append(result)
            total_detections += len(result.detections)

    cost_estimate = estimate_cost(results) if total_detections > 0 else None

    surveyor_assessment = generate_assessment(
        image_results=results,
        images_raw=images_raw,
        cost_estimate=cost_estimate,
        narrative=narrative,
    )

    # Compute fraud signals (Python-side only — Spring Boot merges with behavioral/network)
    fraud_result = compute_fraud_result(
        images_raw=images_raw,
        incident_time_str=incident_time,
        surveyor_assessment=surveyor_assessment,
    )

    processing_time_ms = int((time.perf_counter() - start) * 1000)

    # Compute per-claim aggregate severity (FR-3 postcondition)
    SEVERITY_RANK = {
        Severity.MINOR: 0,
        Severity.MODERATE: 1,
        Severity.SEVERE: 2,
        Severity.TOTAL_LOSS_CANDIDATE: 3,
    }

    claim_aggregate_severity: Optional[Severity] = None
    highest_rank = -1

    for img in results:
        for det in img.detections:
            # Normalize to Severity enum regardless of what Pydantic gave us
            if isinstance(det.severity, Severity):
                sev_enum = det.severity
            else:
                # Fallback: convert string to enum
                try:
                    sev_enum = Severity(det.severity)
                except ValueError:
                    continue  # unknown severity value, skip

            rank = SEVERITY_RANK.get(sev_enum, -1)
            if rank > highest_rank:
                highest_rank = rank
                claim_aggregate_severity = sev_enum

    return AnalyzeResponse(
        model_version=detector.version,
        images=results,
        total_detections=total_detections,
        cost_estimate=cost_estimate,
        surveyor_assessment=surveyor_assessment,
        fraud_result=fraud_result,  # NEW
        processing_time_ms=processing_time_ms,
        claim_aggregate_severity=claim_aggregate_severity,
    )