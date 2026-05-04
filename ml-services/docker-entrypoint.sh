#!/bin/sh
set -e

MODEL_PATH="/app/models/damage-model.pt"

# ── Model download ────────────────────────────────────────────────────────────
if [ ! -f "$MODEL_PATH" ]; then
    echo "[entrypoint] YOLO model missing at $MODEL_PATH — downloading from Hugging Face..."

    # Retry up to 3 times — Railway containers can have transient network blips
    # on cold starts, especially during first deploy.
    attempt=0
    max_attempts=3
    until python /app/scripts/download_model.py; do
        attempt=$((attempt + 1))
        if [ "$attempt" -ge "$max_attempts" ]; then
            echo "[entrypoint] ERROR: model download failed after $max_attempts attempts. Aborting."
            exit 1
        fi
        echo "[entrypoint] download attempt $attempt failed — retrying in 5s..."
        sleep 5
    done

    # Verify the file actually landed and isn't a 0-byte stub
    if [ ! -s "$MODEL_PATH" ]; then
        echo "[entrypoint] ERROR: model file exists but is empty. Aborting."
        exit 1
    fi

    echo "[entrypoint] model downloaded successfully."
fi

# ── Runtime checks ────────────────────────────────────────────────────────────
# Respect Railway's injected PORT; fall back to 8000 for local runs.
export PORT="${PORT:-8000}"

echo "[entrypoint] model OK | PORT=${PORT} | starting: $*"
exec "$@"