import base64
import hashlib
import io
import os

import numpy as np
from fastapi import APIRouter
from PIL import Image
from pydantic import BaseModel

# Integration-test-only escape hatch: when set to "true", compute_embedding()
# returns a deterministic fake vector instead of calling the real DeepFace model.
# Lets the backend<->ai-service HTTP/JSON contract be tested for real (real
# process, real network call) without installing DeepFace/tensorflow or needing
# real face-image fixtures. MUST stay unset in production — never set by
# render.yaml or any prod config, only by the identity-integration CI job.
MODEL_STUB_ENV_VAR = "IDENTITY_MODEL_STUB"

# DeepFace's framework-default cosine-distance threshold for Facenet512 (see
# research/deepface-login-recheck.md) — not custom-tuned, no labeled validation
# set exists for this project.
FACE_MATCH_THRESHOLD = 0.30
EMBEDDING_MODEL = "Facenet512"

router = APIRouter(prefix="/identity", tags=["identity"])


class EmbedRequest(BaseModel):
    selfieBlob: str


class EmbedResponse(BaseModel):
    embedding: list[float]


class CompareRequest(BaseModel):
    selfieBlob: str
    storedEmbedding: list[float]


class CompareResponse(BaseModel):
    match: bool
    distance: float


def _stub_embedding(selfie_blob: str) -> list[float]:
    """Deterministic fake embedding for IDENTITY_MODEL_STUB=true. Same input always
    yields the same vector; different inputs yield vectors far enough apart to land
    on opposite sides of FACE_MATCH_THRESHOLD.
    """
    seed = int(hashlib.sha256(selfie_blob.encode()).hexdigest(), 16) % (2**32)
    rng = np.random.default_rng(seed)
    return rng.normal(size=512).tolist()


def compute_embedding(selfie_blob: str) -> list[float]:
    """Decode a base64 selfie and compute its Facenet512 embedding via DeepFace.

    Imports DeepFace lazily so the test suite (which always patches this
    function directly) never needs the model installed to run.
    """
    if os.environ.get(MODEL_STUB_ENV_VAR) == "true":
        return _stub_embedding(selfie_blob)

    from deepface import DeepFace

    image_bytes = base64.b64decode(selfie_blob)
    image = np.array(Image.open(io.BytesIO(image_bytes)).convert("RGB"))

    result = DeepFace.represent(
        img_path=image,
        model_name=EMBEDDING_MODEL,
        enforce_detection=True,
    )
    return result[0]["embedding"]


def cosine_distance(a: list[float], b: list[float]) -> float:
    a_arr = np.asarray(a, dtype=float)
    b_arr = np.asarray(b, dtype=float)
    similarity = np.dot(a_arr, b_arr) / (np.linalg.norm(a_arr) * np.linalg.norm(b_arr))
    return float(1 - similarity)


@router.post("/embed", response_model=EmbedResponse)
def embed(request: EmbedRequest) -> EmbedResponse:
    embedding = compute_embedding(request.selfieBlob)
    return EmbedResponse(embedding=embedding)


@router.post("/compare", response_model=CompareResponse)
def compare(request: CompareRequest) -> CompareResponse:
    new_embedding = compute_embedding(request.selfieBlob)
    distance = cosine_distance(new_embedding, request.storedEmbedding)
    return CompareResponse(match=distance < FACE_MATCH_THRESHOLD, distance=distance)
