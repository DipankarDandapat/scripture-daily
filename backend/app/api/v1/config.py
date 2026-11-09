"""Centralised key-value configuration (ads, refresh intervals, feature flags) for the Android app."""
import json
from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.entities import AppConfigEntry
from app.services.app_config import public_config, set_config, validate_config_value

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


@router.get("/config", response_model=ConfigOut)
def get_config(request: Request, db: Session = Depends(get_db)):
    """Every public configuration key. Send ``If-None-Match`` with the last ETag to get a cheap 304 when nothing changed."""
    config, version, updated = public_config(db)
    etag = f'"{version}"'
    headers = {"ETag": etag, "Cache-Control": "no-cache"}
    if request.headers.get("if-none-match") == etag:
        return Response(status_code=304, headers=headers)
    return JSONResponse({"version": version, "updated_at": updated.isoformat() if updated else None, "config": config}, headers=headers)


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
