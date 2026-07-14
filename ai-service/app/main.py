from fastapi import FastAPI

app = FastAPI(title="HealthLine AI Triage Service")


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}
