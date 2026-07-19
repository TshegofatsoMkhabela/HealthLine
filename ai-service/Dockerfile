FROM python:3.13-slim
WORKDIR /app

# opencv-python (a DeepFace dependency) is built expecting a desktop/GUI environment and
# needs these shared libraries even though we never use any GUI feature — python:3.13-slim
# doesn't ship them. Without this, `import cv2` fails at the first real /identity/embed or
# /identity/compare call with "ImportError: libxcb.so.1: cannot open shared object file".
RUN apt-get update && apt-get install -y --no-install-recommends \
    libgl1 \
    libglib2.0-0 \
    libsm6 \
    libxext6 \
    libxrender1 \
    libxcb1 \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY app ./app
EXPOSE 8000
CMD ["sh", "-c", "uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000}"]
