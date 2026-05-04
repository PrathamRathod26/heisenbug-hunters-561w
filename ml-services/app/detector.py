"""YOLO model wrapper. Loads once at startup, reused for every request."""
import time
from pathlib import Path

import cv2
import numpy as np
from ultralytics import YOLO

from app.schemas import BoundingBox, Detection, ImageResult
from app.severity import classify_severity, compute_severity_score


class DamageDetector:
    def __init__(self, model_path: str = "models/damage-model.pt"):
        self.model_path = model_path
        self.model = None
        self.version = "yolov11n-car-damage-v1"

    def load(self) -> None:
        """Call once at app startup."""
        path = Path(self.model_path)
        if not path.exists():
            raise FileNotFoundError(f"Model not found at {path.resolve()}")
        print(f"Loading model from {path}...")
        self.model = YOLO(str(path))
        print(f"Model loaded. Classes: {len(self.model.names)}")

    def detect(self, image_bgr: np.ndarray, image_id: str) -> ImageResult:
        """Run detection on one image. Returns structured result."""
        if self.model is None:
            raise RuntimeError("Model not loaded. Call load() first.")

        h, w = image_bgr.shape[:2]
        start = time.perf_counter()

        results = self.model.predict(
            source=image_bgr,
            conf=0.25,
            iou=0.45,
            verbose=False,
        )

        latency_ms = int((time.perf_counter() - start) * 1000)

        detections = []
        for r in results:
            if r.boxes is None:
                continue
            for box in r.boxes:
                cls_id = int(box.cls.item())
                label = self.model.names[cls_id]
                conf = float(box.conf.item())
                x1, y1, x2, y2 = [float(v) for v in box.xyxy[0].tolist()]
                area_frac = ((x2 - x1) * (y2 - y1)) / (w * h)

                # NEW — compute severity
                sev_score = compute_severity_score(label, area_frac, conf)
                severity = classify_severity(sev_score)

                detections.append(Detection(
                    label=label,
                    confidence=conf,
                    bbox=BoundingBox(
                        x1=x1, y1=y1, x2=x2, y2=y2,
                        area_fraction=area_frac,
                    ),
                    severity=severity,
                    severity_score=round(sev_score, 3),
                ))

        return ImageResult(
            image_id=image_id,
            width=w,
            height=h,
            detections=detections,
            latency_ms=latency_ms,
        )


# Module-level singleton. main.py will call .load() at startup.
detector = DamageDetector()