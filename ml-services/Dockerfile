# syntax=docker/dockerfile:1.7
FROM python:3.11-slim AS runtime

# ── Get your service ID from: Railway Dashboard → your service → Settings → Service ID
# Replace YOUR_SERVICE_ID_HERE with that value (e.g. a1b2c3d4-e5f6-...)
ARG RAILWAY_SERVICE_ID=3ffc787e-fa23-48e1-9f22-06394b06d179

RUN --mount=type=cache,id=s/YOUR_SERVICE_ID_HERE-apt-cache,target=/var/cache/apt \
    --mount=type=cache,id=s/YOUR_SERVICE_ID_HERE-apt-lib,target=/var/lib/apt \
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

COPY requirements.txt .
RUN --mount=type=cache,id=s/YOUR_SERVICE_ID_HERE-pip-cache,target=/root/.cache/pip \
    pip install --upgrade pip && \
    pip install -r requirements.txt

COPY app ./app
COPY scripts ./scripts
COPY docker-entrypoint.sh ./
RUN chmod +x docker-entrypoint.sh && \
    mkdir -p /app/models

ENV PYTHONUNBUFFERED=1
ARG PORT=8000
ENV PORT=${PORT}
EXPOSE ${PORT}

ENTRYPOINT ["./docker-entrypoint.sh"]
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT}"]