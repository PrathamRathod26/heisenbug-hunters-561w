#!/bin/sh
set -e

MODEL_PATH="/app/models/damage-model.pt"

if [ ! -f "$MODEL_PATH" ]; then
    echo "[entrypoint] YOLO model missing at $MODEL_PATH — downloading from Hugging Face..."
    python /app/scripts/download_model.py
fi

echo "[entrypoint] starting: $*"
exec "$@"
