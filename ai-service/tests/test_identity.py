from unittest.mock import patch

SAME_PERSON_EMBEDDING_A = [0.1] * 512
SAME_PERSON_EMBEDDING_B = [0.101] * 512  # cosine distance ~0, "same person"
DIFFERENT_PERSON_EMBEDDING = [-0.9] * 512  # cosine distance large, "different person"

VALID_SELFIE = "base64-selfie-bytes"


def test_embed_returns_a_512_length_embedding_vector(client):
    with patch("app.identity.compute_embedding", return_value=SAME_PERSON_EMBEDDING_A):
        response = client.post("/identity/embed", json={"selfieBlob": VALID_SELFIE})

    assert response.status_code == 200
    body = response.json()
    assert len(body["embedding"]) == 512
    assert all(isinstance(x, float) for x in body["embedding"])


def test_compare_returns_match_true_for_same_person_under_threshold(client):
    with patch("app.identity.compute_embedding", return_value=SAME_PERSON_EMBEDDING_B):
        response = client.post(
            "/identity/compare",
            json={
                "selfieBlob": VALID_SELFIE,
                "storedEmbedding": SAME_PERSON_EMBEDDING_A,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["match"] is True
    assert body["distance"] < 0.30


def test_compare_returns_match_false_for_different_person_at_or_above_threshold(client):
    with patch(
        "app.identity.compute_embedding", return_value=DIFFERENT_PERSON_EMBEDDING
    ):
        response = client.post(
            "/identity/compare",
            json={
                "selfieBlob": VALID_SELFIE,
                "storedEmbedding": SAME_PERSON_EMBEDDING_A,
            },
        )

    assert response.status_code == 200
    body = response.json()
    assert body["match"] is False
    assert body["distance"] >= 0.30


def test_embed_rejects_missing_selfie_blob_with_422(client):
    response = client.post("/identity/embed", json={})

    assert response.status_code == 422


def test_compare_rejects_missing_stored_embedding_with_422(client):
    response = client.post("/identity/compare", json={"selfieBlob": VALID_SELFIE})

    assert response.status_code == 422


def test_embedding_the_same_selfie_twice_is_deterministic_enough_for_compare(client):
    with patch("app.identity.compute_embedding", return_value=SAME_PERSON_EMBEDDING_A):
        first = client.post("/identity/embed", json={"selfieBlob": VALID_SELFIE}).json()
        second_compare = client.post(
            "/identity/compare",
            json={"selfieBlob": VALID_SELFIE, "storedEmbedding": first["embedding"]},
        )

    assert second_compare.json()["distance"] < 0.01
