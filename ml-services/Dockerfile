# syntax=docker/dockerfile:1.7
FROM python:3.11-slim AS runtime

# OpenCV + ffmpeg system deps
# Railway supports BuildKit cache mounts — use the service ID (not a custom key name)
# as Railway requires the format: id=<railway-service-id>
# Set RAILWAY_DOCKERFILE_PATH in service variables if this file isn't at the root
RUN --mount=type=cache,id=ml-service-apt-cache,target=/var/cache/apt \
    --mount=type=cache,id=ml-service-apt-lib,target=/var/lib/apt \
    rm -f /etc/apt/apt.conf.d/docker-clean && \
    apt-get update && apt-get install -y --no-install-recommends \
        libgl1 \
        libglib2.0-0 \
        libsm6 \
        libxext6 \
        libxrender1 \
        ffmpeg \
        curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# pip cache mount — Railway's PIP_CACHE_DIR is /root/.cache/pip
RUN --mount=type=cache,id=ml-service-pip-cache,target=/root/.cache/pip \
    pip install --upgrade pip

COPY requirements.txt .
RUN --mount=type=cache,id=ml-service-pip-cache,target=/root/.cache/pip \
    pip install -r requirements.txt

COPY app ./app
COPY scripts ./scripts
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh && \
    mkdir -p /app/models

ENV PYTHONUNBUFFERED=1

# Railway dynamically assigns the PORT env variable at runtime.
# Do NOT hardcode 8000 — read $PORT and fall back to 8000 for local dev.
# The EXPOSE directive is documentation-only on Railway; actual port binding
# is controlled by the PORT variable Railway injects.
ARG PORT=8000
ENV PORT=${PORT}
EXPOSE ${PORT}

ENTRYPOINT ["./docker-entrypoint.sh"]
# Use the Railway-injected $PORT at runtime
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]