"""Run this once to download the pre-trained YOLO car damage model."""
from huggingface_hub import hf_hub_download
import shutil
import os

HERE = os.path.dirname(os.path.abspath(__file__))
MODELS_DIR = os.path.abspath(os.path.join(HERE, "..", "models"))
os.makedirs(MODELS_DIR, exist_ok=True)

print("Downloading model from Hugging Face...")
src = hf_hub_download(
    repo_id="vineetsarpal/yolov11n-car-damage",
    filename="best.pt",
    token=os.getenv("HF_TOKEN"),
)

dest = os.path.join(MODELS_DIR, "damage-model.pt")
shutil.copy(src, dest)

size_mb = os.path.getsize(dest) / (1024 * 1024)
print(f"Done. Saved to {dest} ({size_mb:.1f} MB)")
