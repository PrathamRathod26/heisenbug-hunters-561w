"""Smoke test: run the model on one image and print detections."""
from ultralytics import YOLO

model = YOLO("../models/damage-model.pt")
results = model.predict("test.webp", conf=0.5, save=True)

for r in results:
    print(f"\nDetections in image:")
    if r.boxes is None or len(r.boxes) == 0:
        print("  (nothing detected)")
        continue
    for box in r.boxes:
        cls_id = int(box.cls.item())
        confidence = float(box.conf.item())
        x1, y1, x2, y2 = [float(v) for v in box.xyxy[0].tolist()]
        print(f"  {model.names[cls_id]}: {confidence:.2%} at ({x1:.0f},{y1:.0f},{x2:.0f},{y2:.0f})")
