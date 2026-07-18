from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def _payload(**overrides):
    base = {
        "age": 21,
        "bloodType": "AB+",
        "allergies": [],
        "medications": [],
        "chronicConditions": [],
        "specialNeeds": [],
    }
    base.update(overrides)
    return base


def test_allergy_present_returns_code_red_and_mentions_allergy():
    response = client.post("/triage/summarize", json=_payload(allergies=["Penicillin"]))

    assert response.status_code == 200
    body = response.json()
    assert body["priorityCode"] == "CODE_RED"
    assert "Penicillin" in body["summary"]


def test_chronic_condition_without_allergy_returns_code_yellow():
    response = client.post(
        "/triage/summarize", json=_payload(chronicConditions=["Type 2 Diabetes"])
    )

    assert response.status_code == 200
    assert response.json()["priorityCode"] == "CODE_YELLOW"


def test_no_allergy_or_chronic_condition_returns_code_green():
    response = client.post("/triage/summarize", json=_payload())

    assert response.status_code == 200
    assert response.json()["priorityCode"] == "CODE_GREEN"


def test_summary_is_coherent_sentence_not_a_raw_field_dump():
    response = client.post("/triage/summarize", json=_payload(age=21, bloodType="AB+"))

    summary = response.json()["summary"]
    assert "21" in summary
    assert "AB+" in summary
    assert "{" not in summary
    assert "[" not in summary


def test_empty_allergies_and_medications_leave_no_dangling_artifacts():
    response = client.post("/triage/summarize", json=_payload())

    summary = response.json()["summary"]
    assert "on ," not in summary
    assert "on ." not in summary
    assert not summary.rstrip().endswith("on")
