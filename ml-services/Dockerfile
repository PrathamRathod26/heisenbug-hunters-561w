# syntax=docker/dockerfile:1.7
FROM python:3.11-slim AS runtime

# OpenCV + ffmpeg system deps. libgl1 (not libgl1-mesa-glx) on bookworm.
# BuildKit cache mounts so re-builds reuse the apt download cache instead of
# re-fetching every package; the default `docker-clean` hook would defeat the
# cache, so we drop it before apt-get update.
RUN --mount=type=cache,id=cacheKey=ml-service-apt-cache,target=/var/cache/apt \
    --mount=type=cache,id=cacheKey=ml-service-apt-lib,target=/var/lib/apt \
    rm -f /etc/apt/apt.conf.d/docker-clean && \
    apt-get update && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
        libsm6 \
        libxext6 \
        libxrender1 \
        ffmpeg \
        curl

WORKDIR /app

# pip deps first for layer caching. Cache mount keeps wheels across builds
# (lives outside the image, so final size is unaffected).
COPY requirements.txt .
RUN --mount=type=cache,id=cacheKey=ml-service-pip-cache,target=/root/.cache/pip \
    pip install --upgrade pip && \
    pip install -r requirements.txt

COPY app ./app
COPY scripts ./scripts
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh && \
    mkdir -p /app/models

ENV PYTHONUNBUFFERED=1
EXPOSE 8000

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
