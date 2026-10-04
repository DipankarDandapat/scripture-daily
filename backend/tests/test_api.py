import os
import tempfile
import uuid
os.environ["SD_DATABASE_URL"] = f"sqlite:///{tempfile.gettempdir()}/scripture_test_{uuid.uuid4().hex}.db"
os.environ["SD_JWT_SECRET"] = "test-secret-with-at-least-32-characters"
from fastapi.testclient import TestClient
from app.main import app


def test_core_flow():
    with TestClient(app) as client:
        assert client.get("/api/v1/health").json()["status"] == "ok"
        religions = client.get("/api/v1/religions").json()
        langs = client.get("/api/v1/languages").json()
        assert len(religions) == 3 and len(langs) == 2
        scriptures = client.get("/api/v1/scriptures", params={"religion_id": 1}).json()
        assert scriptures and scriptures[0]["name"] == "Bhagavad Gita"
        verse = client.get("/api/v1/verses/today", params={"scripture_id": 1, "language_id": 1}).json()
        assert verse["scripture"] == "Bhagavad Gita" and verse["is_demo"] is True
        assert client.get("/api/v1/verses/search", params={"q": "patience"}).status_code == 200
        reg = client.post("/api/v1/auth/register", json={"email": "reader@example.com", "password": "correct-horse-8"})
        assert reg.status_code == 201
        headers = {"Authorization": "Bearer " + reg.json()["access_token"]}
        assert client.get("/api/v1/users/me", headers=headers).json()["email"] == "reader@example.com"
        assert client.get("/api/v1/users/preferences", headers=headers).status_code == 200
        assert client.put("/api/v1/users/preferences", headers=headers, json={"religion_id": 1, "language_id": 2, "scripture_id": 1, "frequency": "6h", "notifications_enabled": True}).status_code == 200
        preferred_verse = client.get("/api/v1/verses/today", headers=headers).json()
        assert preferred_verse["language"] == "hi" and preferred_verse["scripture"] == "Bhagavad Gita"
        assert client.post("/api/v1/favorites", headers=headers, json={"verse_id": verse["id"]}).status_code == 201
        assert len(client.get("/api/v1/favorites", headers=headers).json()) == 1
        assert client.delete(f"/api/v1/favorites/{verse['id']}", headers=headers).status_code == 204
        assert client.get("/api/v1/favorites", headers=headers).json() == []
        assert client.get("/api/v1/favorites").status_code == 401


def test_invalid_preference_combination_is_rejected():
    with TestClient(app) as client:
        token = client.post("/api/v1/auth/register", json={"email": "reader2@example.com", "password": "correct-horse-8"}).json()["access_token"]
        response = client.put("/api/v1/users/preferences", headers={"Authorization": f"Bearer {token}"}, json={"religion_id": 2, "language_id": 1, "scripture_id": 1, "frequency": "daily", "notifications_enabled": False})
        assert response.status_code == 422
