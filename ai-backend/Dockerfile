# syntax=docker/dockerfile:1
FROM python:3.12-slim-bookworm@sha256:e697c81897302a4c4333bade000d98038fbb21eb064f8ee1fd42d02ebcd9d337 AS dependencies
WORKDIR /app
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    FASTEMBED_CACHE_PATH=/opt/embedding-cache \
    HF_HUB_OFFLINE=1
COPY requirements.lock requirements.txt ./
RUN python -m pip install --no-cache-dir --require-hashes -r requirements.lock \
    && python -m pip check \
    && python -m pip install --dry-run --no-index -r requirements.txt
# Download only public model weights. Do not instantiate the business vector store.
RUN HF_HUB_OFFLINE=0 python -c "from fastembed import TextEmbedding; m = TextEmbedding('BAAI/bge-small-zh-v1.5'); assert len(list(m.embed(['build warmup']))[0]) == 512"
COPY main.py SystemPrompt.md storage-version ./
COPY agent/ ./agent/
COPY common/ ./common/
COPY conversation/ ./conversation/
COPY eval/ ./eval/
COPY kb/ ./kb/
COPY state/ ./state/
COPY scripts/ ./scripts/
RUN mkdir -p agent/db common/db conversation/db kb/db common/logs

FROM dependencies AS test
COPY requirements-test.lock ./
RUN python -m pip install --no-cache-dir --require-hashes -r requirements.lock -r requirements-test.lock \
    && python -m pip check
COPY tests/ ./tests/
ENV RUN_MILVUS_TESTS=1
CMD ["python", "-m", "pytest", "tests", "-q"]

FROM dependencies AS runtime
ARG VCS_REF
ARG STORAGE_VERSION=1
LABEL org.opencontainers.image.source="https://github.com/2986577461/ITHome" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.ithome.storage-version="${STORAGE_VERSION}"
EXPOSE 8000
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD ["python", "scripts/smoke.py", "--live"]
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000", "--no-access-log"]
