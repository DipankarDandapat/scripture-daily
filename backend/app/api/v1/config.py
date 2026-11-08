"""Centralised key-value configuration (ads, refresh intervals, feature flags) for the Android app."""
from fastapi import APIRouter, Depends, Header, HTTPException, Request, Response
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.models.entities import AppConfigEntry
from app.services.app_config import public_config, set_config

router = APIRouter()


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
    import json
    return [{"key": r.key, "value": json.loads(r.value), "description": r.description, "is_public": r.is_public,
             "updated_at": r.updated_at.isoformat() if r.updated_at else None} for r in rows]


@router.put("/admin/config/{key}", dependencies=[Depends(_admin)])
def admin_set_config(key: str, body: ConfigWrite, db: Session = Depends(get_db)):
    row = set_config(db, key, body.value, body.description, body.is_public)
    return {"key": row.key, "updated": True}


@router.delete("/admin/config/{key}", status_code=204, dependencies=[Depends(_admin)])
def admin_delete_config(key: str, db: Session = Depends(get_db)):
    row = db.get(AppConfigEntry, key)
    if row is None:
        raise HTTPException(status_code=404, detail="Config key not found")
    db.delete(row); db.commit()
    return Response(status_code=204)
