"""Load the model and print its class names so we know what it detects."""
from ultralytics import YOLO

model = YOLO("../models/damage-model.pt")
print("Model task:", model.task)
print("Classes this model knows:")
for class_id, class_name in model.names.items():
    print(f"  {class_id}: {class_name}")