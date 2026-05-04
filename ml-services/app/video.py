"""Extract sampled frames from an uploaded video."""
import tempfile
from dataclasses import dataclass
from typing import List

import cv2
import numpy as np
from fastapi import HTTPException


MAX_VIDEO_SECONDS = 15.0
MAX_VIDEO_BYTES = 50 * 1024 * 1024  # 50 MB
FRAMES_PER_SECOND_SAMPLE = 1.0      # extract 1 frame per second
DEDUP_SIMILARITY_THRESHOLD = 0.95   # skip near-identical frames


@dataclass
class ExtractedFrame:
    image_bgr: np.ndarray
    timestamp_sec: float


def _frame_similarity(a: np.ndarray, b: np.ndarray) -> float:
    """Cheap perceptual similarity via downscaled mean-abs-diff. Returns [0,1]."""
    small_a = cv2.resize(a, (32, 32))
    small_b = cv2.resize(b, (32, 32))
    diff = np.mean(np.abs(small_a.astype(np.int16) - small_b.astype(np.int16)))
    return 1.0 - (diff / 255.0)


def extract_frames(video_bytes: bytes) -> List[ExtractedFrame]:
    """
    Extract frames at 1fps, drop near-duplicates.
    Returns list of ExtractedFrame. Raises HTTPException on bad input.
    """
    if len(video_bytes) > MAX_VIDEO_BYTES:
        raise HTTPException(413, f"Video exceeds {MAX_VIDEO_BYTES} bytes")

    if not video_bytes:
        raise HTTPException(400, "Empty video file")

    # OpenCV needs a path; write to a temp file
    with tempfile.NamedTemporaryFile(suffix=".mp4", delete=True) as tmp:
        tmp.write(video_bytes)
        tmp.flush()

        cap = cv2.VideoCapture(tmp.name)
        if not cap.isOpened():
            raise HTTPException(400, "Unable to decode video")

        fps = cap.get(cv2.CAP_PROP_FPS) or 30.0
        frame_count = cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0
        duration_sec = frame_count / fps if fps > 0 else 0

        if duration_sec > MAX_VIDEO_SECONDS + 0.5:
            cap.release()
            raise HTTPException(400, f"Video exceeds {MAX_VIDEO_SECONDS}s (got {duration_sec:.1f}s)")

        sample_every_n = max(1, int(round(fps / FRAMES_PER_SECOND_SAMPLE)))

        frames: List[ExtractedFrame] = []
        last_kept: np.ndarray | None = None
        idx = 0

        while True:
            ok, frame = cap.read()
            if not ok:
                break
            if idx % sample_every_n == 0:
                if last_kept is None or _frame_similarity(last_kept, frame) < DEDUP_SIMILARITY_THRESHOLD:
                    frames.append(ExtractedFrame(
                        image_bgr=frame,
                        timestamp_sec=idx / fps if fps > 0 else 0.0,
                    ))
                    last_kept = frame
            idx += 1

        cap.release()

    return frames