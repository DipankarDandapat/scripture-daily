"""Centralised key-value configuration (ads, refresh intervals, feature flags) for the Android app."""
import json
from fastapi import APIRouter, Depends, Header, HTTPException, Query, Request, Response
from fastapi.responses import HTMLResponse, JSONResponse
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.entities import AppConfigEntry
from app.services.app_config import evaluate_gate, public_config, set_config, validate_config_value
from app.services.maintenance_preview import render_maintenance_preview

router = APIRouter()

# Keys whose value is a dict and should be merged (not replaced) on both PUT and PATCH.
_MERGE_KEYS = {"ads_placements"}


class ConfigOut(BaseModel):
    version: str
    updated_at: str | None
    config: dict


class ConfigWrite(BaseModel):
    value: object
    description: str | None = None
    is_public: bool | None = None


def _admin(x_admin_token: str | None = Header(default=None)) -> None:
    if not settings.admin_token or x_admin_token != settings.admin_token:
        raise HTTPException(status_code=403, detail="Invalid admin token")


def _config_response(request: Request, db: Session):
    config, version, updated = public_config(db)
    etag = f'"{version}"'
    headers = {"ETag": etag, "Cache-Control": "no-cache"}
    if request.headers.get("if-none-match") == etag:
        return Response(status_code=304, headers=headers)
    return JSONResponse({"version": version, "updated_at": updated.isoformat() if updated else None, "config": config}, headers=headers)


@router.get("/app/config", response_model=ConfigOut)
def get_app_config(request: Request, db: Session = Depends(get_db)):
    """The single configuration document the Android app loads on startup (update prompt, maintenance mode, ads, feature flags).

    Send ``If-None-Match`` with the last ETag to get a cheap 304 when nothing changed. Replaces Firebase Remote Config.
    """
    return _config_response(request, db)


@router.get("/config", response_model=ConfigOut)
def get_config(request: Request, db: Session = Depends(get_db)):
    """Same document as ``GET /app/config``; kept for app versions released before the move to ``/app/config``."""
    return _config_response(request, db)


@router.get("/app/config/evaluate")
def evaluate_app_config(version: str = Query(..., pattern=r"^\d{1,4}(\.\d{1,4}){0,3}$"), db: Session = Depends(get_db)):
    """Which screen an app with ``version`` installed would open right now: ``mandatory_update``, ``maintenance``,
    ``optional_update`` or ``app``. Handy for checking an admin change before users see it."""
    config, _, _ = public_config(db)
    return evaluate_gate(config, version)


# ── Admin shortcuts for the update prompt and maintenance mode (thin wrappers over the key-value store) ──
class AppUpdateWrite(BaseModel):
    enabled: bool | None = None
    latest_version: str | None = None
    minimum_supported_version: str | None = None
    force_update: bool | None = None
    title: str | None = None
    message: str | None = None
    mandatory_title: str | None = None
    mandatory_message: str | None = None
    play_store_url: str | None = None
    optional_reminder_hours: int | None = None


class MaintenanceWrite(BaseModel):
    title: str | None = None
    message: str | None = None
    estimated_end_time: str | None = None
    clear_estimated_end_time: bool = False
    allow_retry: bool | None = None
    enforce_cached_for_hours: int | None = None


def _current(db: Session, key: str) -> dict:
    row = db.get(AppConfigEntry, key)
    return json.loads(row.value) if row else {}


def _patch(db: Session, key: str, changes: dict) -> dict:
    set_config(db, key, changes, merge=True)
    return _current(db, key)


@router.get("/admin/app-update", dependencies=[Depends(_admin)])
def admin_get_app_update(db: Session = Depends(get_db)):
    return _current(db, "app_update")


@router.patch("/admin/app-update", dependencies=[Depends(_admin)])
def admin_patch_app_update(body: AppUpdateWrite, db: Session = Depends(get_db)):
    """Change any subset of the update settings, e.g. ``{"latest_version": "1.5.0"}`` or ``{"minimum_supported_version": "1.5.0"}``."""
    return _patch(db, "app_update", body.model_dump(exclude_none=True))


def _maintenance_changes(body: MaintenanceWrite | None) -> dict:
    if body is None:
        return {}
    changes = body.model_dump(exclude_none=True, exclude={"clear_estimated_end_time"})
    if body.clear_estimated_end_time:
        changes["estimated_end_time"] = None
    return changes


@router.get("/admin/maintenance", dependencies=[Depends(_admin)])
def admin_get_maintenance(db: Session = Depends(get_db)):
    return _current(db, "maintenance_mode")


@router.post("/admin/maintenance/enable", dependencies=[Depends(_admin)])
def admin_enable_maintenance(body: MaintenanceWrite | None = None, db: Session = Depends(get_db)):
    """Turn maintenance mode on (optionally updating the texts / estimated end time in the same call)."""
    return _patch(db, "maintenance_mode", {**_maintenance_changes(body), "enabled": True})


@router.post("/admin/maintenance/disable", dependencies=[Depends(_admin)])
def admin_disable_maintenance(db: Session = Depends(get_db)):
    return _patch(db, "maintenance_mode", {"enabled": False})


@router.patch("/admin/maintenance", dependencies=[Depends(_admin)])
def admin_patch_maintenance(body: MaintenanceWrite, db: Session = Depends(get_db)):
    """Edit the maintenance texts, estimated end time or retry button without changing whether it is on."""
    return _patch(db, "maintenance_mode", _maintenance_changes(body))


@router.get("/admin/maintenance/preview", dependencies=[Depends(_admin)], response_class=HTMLResponse)
def admin_preview_maintenance(theme: str = Query("light", pattern="^(light|dark)$"), tz: str = Query("UTC", max_length=64),
                              db: Session = Depends(get_db)):
    """A phone-sized HTML preview of the maintenance screen exactly as configured (whether or not it is enabled)."""
    return HTMLResponse(render_maintenance_preview(_current(db, "maintenance_mode"), theme=theme, tz_name=tz))


@router.get("/admin/config", dependencies=[Depends(_admin)])
def admin_list_config(db: Session = Depends(get_db)):
    rows = db.scalars(select(AppConfigEntry).order_by(AppConfigEntry.key)).all()
    return [{"key": r.key, "value": json.loads(r.value), "description": r.description, "is_public": r.is_public,
             "updated_at": r.updated_at.isoformat() if r.updated_at else None} for r in rows]


@router.put("/admin/config/{key}", dependencies=[Depends(_admin)])
def admin_set_config(key: str, body: ConfigWrite, db: Session = Depends(get_db)):
    """Replace a config value. For dict keys (e.g. ads_placements) the incoming
    dict is **merged** into the existing one so callers never need to send the full object."""
    row = set_config(db, key, body.value, body.description, body.is_public, merge=True)
    return {"key": row.key, "updated": True}


@router.patch("/admin/config/{key}", dependencies=[Depends(_admin)])
def admin_patch_config(key: str, body: ConfigWrite, db: Session = Depends(get_db)):
    """Merge a partial dict into an existing dict config value.
    Only works for keys whose current value is a JSON object (e.g. ads_placements, ads_global).
    Non-dict keys fall back to a full replace (same as PUT)."""
    row = set_config(db, key, body.value, body.description, body.is_public, merge=True)
    return {"key": row.key, "updated": True, "merged": isinstance(body.value, dict)}


def _maybe_merge(db: Session, key: str, incoming: object) -> object:
    """Merge incoming dict into existing dict value. Returns incoming unchanged for non-dict types."""
    if not isinstance(incoming, dict):
        return incoming
    row = db.get(AppConfigEntry, key)
    if row is None:
        return incoming
    try:
        existing = json.loads(row.value)
    except Exception:
        return incoming
    if not isinstance(existing, dict):
        return incoming
    return {**existing, **incoming}


@router.delete("/admin/config/{key}", status_code=204, dependencies=[Depends(_admin)])
def admin_delete_config(key: str, db: Session = Depends(get_db)):
    row = db.get(AppConfigEntry, key)
    if row is None:
        raise HTTPException(status_code=404, detail="Config key not found")
    db.delete(row); db.commit()
    return Response(status_code=204)
