from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="HealthLine AI Triage Service")


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
