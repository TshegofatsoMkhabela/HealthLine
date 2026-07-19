import logging
import os
from contextlib import asynccontextmanager

from fastapi import FastAPI
from pydantic import BaseModel

from app.identity import EMBEDDING_MODEL, MODEL_STUB_ENV_VAR
from app.identity import router as identity_router

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(_app: FastAPI):
    # Load the face-recognition model once at startup rather than on the first
    # real request, so a request doesn't eat the model-load cost on top of
    # Render's own cold-start latency (see research/deepface-login-recheck.md).
    if os.environ.get(MODEL_STUB_ENV_VAR) == "true":
        logger.info("IDENTITY_MODEL_STUB=true — skipping real model warm-load")
    else:
        try:
            from deepface import DeepFace

            DeepFace.build_model(EMBEDDING_MODEL)
        except ImportError:
            logger.warning(
                "deepface not installed — skipping model warm-load (expected in tests)"
            )
    yield


app = FastAPI(title="HealthLine AI Triage Service", lifespan=lifespan)
app.include_router(identity_router)


class TriagePayload(BaseModel):
    age: int
    bloodType: str
    allergies: list[str]
    medications: list[str]
    chronicConditions: list[str]
    specialNeeds: list[str]


class TriageSummary(BaseModel):
    summary: str
    priorityCode: str


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.post("/triage/summarize")
def summarize(payload: TriagePayload) -> TriageSummary:
    return TriageSummary(
        summary=_build_summary(payload),
        priorityCode=_priority_code(payload),
    )


def _priority_code(payload: TriagePayload) -> str:
    if payload.allergies:
        return "CODE_RED"
    if payload.chronicConditions:
        return "CODE_YELLOW"
    return "CODE_GREEN"


def _build_summary(payload: TriagePayload) -> str:
    parts = [f"{payload.age}yo, {payload.bloodType}"]
    if payload.medications:
        parts.append(f"on {', '.join(payload.medications)}")
    if payload.allergies:
        parts.append(f"CRITICAL: severe {', '.join(payload.allergies)} allergy")
    elif payload.chronicConditions:
        parts.append(f"managing {', '.join(payload.chronicConditions)}")
    return " — ".join(parts)
