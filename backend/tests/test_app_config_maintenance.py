"""App update prompt + maintenance mode served by GET /api/v1/app/config (replaces Firebase Remote Config)."""
import os
import tempfile
import uuid
os.environ.setdefault("SD_DATABASE_URL", f"sqlite:///{tempfile.gettempdir()}/scripture_test_{uuid.uuid4().hex}.db")
os.environ.setdefault("SD_JWT_SECRET", "test-secret-with-at-least-32-characters")

import pytest
from fastapi.testclient import TestClient

from app.core.config import settings
from app.main import app
from app.services.app_config import evaluate_gate, is_older, parse_version

ADMIN = {"X-Admin-Token": settings.admin_token}


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


@pytest.fixture(autouse=True)
def _reset(client):
    yield
    client.post("/api/v1/admin/maintenance/disable", headers=ADMIN)
    client.patch("/api/v1/admin/app-update", headers=ADMIN, json={"enabled": True, "latest_version": "1.4.2",
                                                                    "minimum_supported_version": "1.0.0", "force_update": False})


def test_app_config_endpoint_serves_update_and_maintenance_with_etag(client):
    r = client.get("/api/v1/app/config")
    assert r.status_code == 200
    cfg = r.json()["config"]
    assert cfg["app_update"]["play_store_url"].startswith("https://play.google.com/")
    assert cfg["app_update"]["latest_version"] and cfg["app_update"]["minimum_supported_version"]
    assert cfg["maintenance_mode"]["enabled"] is False and cfg["maintenance_mode"]["title"] == "We'll Be Back Soon!"
    assert cfg["ambient_wisdom"]["live_wallpaper_available"] is True
    assert "ads_enabled" in cfg   # one document for everything: no second config system
    etag = r.headers["ETag"]
    assert client.get("/api/v1/app/config", headers={"If-None-Match": etag}).status_code == 304
    # The legacy endpoint stays for already-installed app versions and returns the same document.
    assert client.get("/api/v1/config").json()["config"] == cfg


def test_version_comparison():
    assert parse_version("1.4") == parse_version("1.4.0") == (1, 4)
    assert is_older("1.4.2", "1.10.0") and not is_older("1.10.0", "1.9.9") and not is_older("1.4.2", "1.4.2")


@pytest.mark.parametrize("update,maintenance,installed,screen,status", [
    ({"latest_version": "1.4.2", "minimum_supported_version": "1.0.0"}, False, "1.4.2", "app", "up_to_date"),
    ({"latest_version": "1.5.0", "minimum_supported_version": "1.0.0"}, False, "1.4.2", "optional_update", "optional"),
    ({"latest_version": "1.5.0", "minimum_supported_version": "1.5.0"}, False, "1.4.2", "mandatory_update", "mandatory"),
    ({"latest_version": "1.5.0", "minimum_supported_version": "1.0.0", "force_update": True}, False, "1.4.2", "mandatory_update", "mandatory"),
    ({"latest_version": "1.4.0", "minimum_supported_version": "1.0.0"}, False, "1.4.2", "app", "up_to_date"),  # newer than latest
    ({"enabled": False, "latest_version": "9.0.0", "minimum_supported_version": "9.0.0"}, False, "1.4.2", "app", "disabled"),
    # priority: mandatory update > maintenance > optional update
    ({"latest_version": "1.5.0", "minimum_supported_version": "1.5.0"}, True, "1.4.2", "mandatory_update", "mandatory"),
    ({"latest_version": "1.5.0", "minimum_supported_version": "1.0.0"}, True, "1.4.2", "maintenance", "optional"),
    ({"latest_version": "1.4.2", "minimum_supported_version": "1.0.0"}, True, "1.4.2", "maintenance", "up_to_date"),
])
def test_gate_scenarios(update, maintenance, installed, screen, status):
    decision = evaluate_gate({"app_update": {"enabled": True, **update}, "maintenance_mode": {"enabled": maintenance}}, installed)
    assert decision["screen"] == screen and decision["update_status"] == status


def test_admin_controls_update_without_an_app_release(client):
    assert client.patch("/api/v1/admin/app-update", json={"latest_version": "1.5.0"}).status_code == 403
    r = client.patch("/api/v1/admin/app-update", headers=ADMIN, json={"latest_version": "1.5.0", "title": "New features"})
    assert r.status_code == 200 and r.json()["latest_version"] == "1.5.0" and r.json()["play_store_url"]  # merged, nothing lost
    assert client.get("/api/v1/app/config/evaluate", params={"version": "1.4.2"}).json()["screen"] == "optional_update"
    client.patch("/api/v1/admin/app-update", headers=ADMIN, json={"minimum_supported_version": "1.5.0"})
    assert client.get("/api/v1/app/config/evaluate", params={"version": "1.4.2"}).json()["screen"] == "mandatory_update"
    assert client.get("/api/v1/app/config/evaluate", params={"version": "1.5.0"}).json()["screen"] == "app"


@pytest.mark.parametrize("body", [
    {"latest_version": "one.two"}, {"minimum_supported_version": "2.0.0"},  # minimum newer than latest
    {"force_update": "maybe"}, {"play_store_url": "javascript:alert(1)"}, {"title": "x" * 200}, {"optional_reminder_hours": -1},
])
def test_invalid_update_config_is_rejected(client, body):
    assert client.patch("/api/v1/admin/app-update", headers=ADMIN, json=body).status_code == 422
    assert client.patch("/api/v1/admin/config/app_update", headers=ADMIN, json={"value": body}).status_code == 422
    assert client.get("/api/v1/app/config").json()["config"]["app_update"]["minimum_supported_version"] == "1.0.0"


def test_maintenance_enable_edit_disable(client):
    assert client.post("/api/v1/admin/maintenance/enable").status_code == 403
    r = client.post("/api/v1/admin/maintenance/enable", headers=ADMIN,
                    json={"estimated_end_time": "2026-11-12T06:30:00Z", "message": "Database upgrade in progress."})
    assert r.status_code == 200 and r.json()["enabled"] is True and r.json()["title"] == "We'll Be Back Soon!"
    cfg = client.get("/api/v1/app/config").json()["config"]["maintenance_mode"]
    assert cfg["enabled"] and cfg["estimated_end_time"] == "2026-11-12T06:30:00Z" and cfg["message"] == "Database upgrade in progress."
    assert client.get("/api/v1/app/config/evaluate", params={"version": "1.4.2"}).json()["screen"] == "maintenance"
    # edit without toggling
    r = client.patch("/api/v1/admin/maintenance", headers=ADMIN, json={"allow_retry": False, "clear_estimated_end_time": True})
    assert r.json()["enabled"] is True and r.json()["allow_retry"] is False and r.json()["estimated_end_time"] is None
    assert client.patch("/api/v1/admin/maintenance", headers=ADMIN, json={"estimated_end_time": "tomorrow"}).status_code == 422
    assert client.patch("/api/v1/admin/maintenance", headers=ADMIN, json={"estimated_end_time": "2026-11-12T06:30:00"}).status_code == 422  # no tz
    assert client.post("/api/v1/admin/maintenance/disable", headers=ADMIN).json()["enabled"] is False
    assert client.get("/api/v1/app/config/evaluate", params={"version": "1.4.2"}).json()["screen"] == "app"


def test_maintenance_preview_renders_configured_texts(client):
    client.patch("/api/v1/admin/maintenance", headers=ADMIN, json={"title": "Back <soon>", "estimated_end_time": "2026-11-12T06:30:00Z", "allow_retry": True})
    assert client.get("/api/v1/admin/maintenance/preview").status_code == 403
    r = client.get("/api/v1/admin/maintenance/preview", headers=ADMIN, params={"theme": "dark", "tz": "Asia/Kolkata"})
    assert r.status_code == 200 and "text/html" in r.headers["content-type"]
    assert "Back &lt;soon&gt;" in r.text and "12:00" in r.text and "Check Again" in r.text and "Close App" in r.text


def test_ambient_wisdom_kill_switches_are_validated(client):
    assert client.patch("/api/v1/admin/config/ambient_wisdom", headers=ADMIN, json={"value": {"live_wallpaper_available": "no"}}).status_code == 422
    assert client.patch("/api/v1/admin/config/ambient_wisdom", headers=ADMIN, json={"value": {"live_wallpaper_available": False}}).status_code == 200
    cfg = client.get("/api/v1/app/config").json()["config"]["ambient_wisdom"]
    assert cfg["live_wallpaper_available"] is False and cfg["activity_aware_available"] is True   # merged, not replaced
    client.patch("/api/v1/admin/config/ambient_wisdom", headers=ADMIN, json={"value": {"live_wallpaper_available": True}})
