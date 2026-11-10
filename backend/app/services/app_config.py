"""Key-value configuration store (``app_config`` table) with validation for the advertising keys."""
import hashlib
import json
import re
from datetime import datetime, timezone

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.content.app_config import DEFAULTS, FORMATS, TEST_UNITS
from app.models.entities import AppConfigEntry

_UNIT_RE = re.compile(r"^ca-app-pub-\d{16}/\d{10}$")
_RATINGS = {"G", "PG", "T", "MA"}


def ensure_app_config(db: Session) -> int:
    """Insert any default key that does not exist yet. Existing (edited) rows are never overwritten."""
    have = set(db.scalars(select(AppConfigEntry.key)).all())
    added = 0
    for key, (value, description) in DEFAULTS.items():
        if key not in have:
            db.add(AppConfigEntry(key=key, value=json.dumps(value, sort_keys=True), description=description, is_public=True))
            added += 1
    if added:
        db.commit()
    _merge_new_placements(db)
    _remove_blocked_screen(db, "share_studio")
    return added


# Placements added after the initial DB seed — merged into the existing ads_placements row.
_NEW_PLACEMENTS = {
    "studio_share_rewarded":           {"enabled": False, "screen": "share_studio", "format": "REWARDED",              "ad_unit_id": ""},
    "studio_whatsapp_rewarded":        {"enabled": False, "screen": "share_studio", "format": "REWARDED",              "ad_unit_id": ""},
    "studio_save_image_rewarded":      {"enabled": False, "screen": "share_studio", "format": "REWARDED",              "ad_unit_id": ""},
    "studio_generate_video_rewarded":  {"enabled": False, "screen": "share_studio", "format": "REWARDED_INTERSTITIAL", "ad_unit_id": ""},
    "studio_share_video_rewarded":     {"enabled": False, "screen": "share_studio", "format": "REWARDED",              "ad_unit_id": ""},
    "studio_whatsapp_video_rewarded":  {"enabled": False, "screen": "share_studio", "format": "REWARDED",              "ad_unit_id": ""},
    "studio_save_video_rewarded":      {"enabled": False, "screen": "share_studio", "format": "REWARDED",              "ad_unit_id": ""},
    "studio_make_another_interstitial":{"enabled": False, "screen": "share_studio", "format": "INTERSTITIAL",         "ad_unit_id": ""},
}


def _merge_new_placements(db: Session) -> None:
    """Add any missing studio placements into the existing ads_placements DB row without overwriting existing ones."""
    row = db.get(AppConfigEntry, "ads_placements")
    if row is None:
        return
    try:
        current: dict = json.loads(row.value)
    except Exception:
        return
    missing = {k: v for k, v in _NEW_PLACEMENTS.items() if k not in current}
    if not missing:
        return
    current.update(missing)
    row.value = json.dumps(current, sort_keys=True)
    row.updated_at = datetime.now(timezone.utc)
    db.commit()


def _remove_blocked_screen(db: Session, screen: str) -> None:
    """Remove a screen from ads_blocked_screens if it is still listed there."""
    row = db.get(AppConfigEntry, "ads_blocked_screens")
    if row is None:
        return
    try:
        screens: list = json.loads(row.value)
    except Exception:
        return
    if screen not in screens:
        return
    screens.remove(screen)
    row.value = json.dumps(screens)
    row.updated_at = datetime.now(timezone.utc)
    db.commit()


def public_config(db: Session) -> tuple[dict, str, datetime | None]:
    """(config dict, version hash, latest update time) for every public key."""
    rows = db.scalars(select(AppConfigEntry).where(AppConfigEntry.is_public.is_(True)).order_by(AppConfigEntry.key)).all()
    config = {r.key: json.loads(r.value) for r in rows}
    version = hashlib.sha1(json.dumps(config, sort_keys=True, separators=(",", ":")).encode()).hexdigest()[:16]
    stamps = [r.updated_at for r in rows if r.updated_at]
    return config, version, (max(stamps) if stamps else None)


def _bad(msg: str):
    raise HTTPException(status_code=422, detail=msg)


def _check_unit_map(name: str, value: object, allow_blank: bool) -> None:
    if not isinstance(value, dict):
        _bad(f"{name} must be an object of format -> ad unit id")
    for fmt, unit in value.items():
        if fmt not in TEST_UNITS:
            _bad(f"{name}: unknown format {fmt}")
        if not isinstance(unit, str) or not ((allow_blank and unit == "") or _UNIT_RE.match(unit)):
            _bad(f"{name}.{fmt}: invalid ad unit id")


def validate_config_value(key: str, value: object) -> None:
    """Reject malformed advertising configuration so a bad edit can never crash apps in the field."""
    if key in {"ads_enabled", "ads_use_test_ids", "wisdom_tree_default_enabled", "meta_ads_enabled"} and not isinstance(value, bool):
        _bad(f"{key} must be true or false")
    if key == "ads_app_id" and not (isinstance(value, str) and re.match(r"^ca-app-pub-\d{16}~\d{10}$", value)):
        _bad("ads_app_id must look like ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY")
    if key == "ads_test_units":
        _check_unit_map(key, value, allow_blank=False)
    if key == "ads_production_units":
        _check_unit_map(key, value, allow_blank=True)
    if key == "config_refresh_min_interval_seconds" and not (isinstance(value, int) and 0 <= value <= 86400):
        _bad(f"{key} must be an integer between 0 and 86400")
    if key == "ads_global":
        if not isinstance(value, dict):
            _bad("ads_global must be an object")
        for name in ("max_ads_per_screen", "min_seconds_between_fullscreen_ads", "min_seconds_between_app_open_ads"):
            if name in value and not (isinstance(value[name], int) and value[name] >= 0):
                _bad(f"ads_global.{name} must be a non-negative integer")
    if key == "ads_saved_verses":
        if not isinstance(value, dict):
            _bad("ads_saved_verses must be an object")
        for name in ("first_ad_after_items", "repeat_interval_items", "max_inline_ads"):
            if name in value and not (isinstance(value[name], int) and value[name] >= 0):
                _bad(f"ads_saved_verses.{name} must be a non-negative integer")
        if value.get("repeat_format", "BANNER_ADAPTIVE_INLINE") not in FORMATS:
            _bad("ads_saved_verses.repeat_format is not a supported format")
    if key == "ads_placements":
        if not isinstance(value, dict):
            _bad("ads_placements must be an object")
        for pid, p in value.items():
            if not isinstance(p, dict) or not isinstance(p.get("enabled"), bool):
                _bad(f"ads_placements.{pid}.enabled must be true or false")
            if p.get("format") not in FORMATS:
                _bad(f"ads_placements.{pid}.format is not a supported format")
            unit = p.get("ad_unit_id", "")
            if unit and not _UNIT_RE.match(unit):
                _bad(f"ads_placements.{pid}.ad_unit_id is invalid")
    if key == "ads_privacy":
        if not isinstance(value, dict) or value.get("max_ad_content_rating", "PG") not in _RATINGS:
            _bad("ads_privacy.max_ad_content_rating must be one of G, PG, T, MA")


def set_config(db: Session, key: str, value: object, description: str | None = None, is_public: bool | None = None, merge: bool = False) -> AppConfigEntry:
    if not re.match(r"^[a-z][a-z0-9_]{1,119}$", key):
        _bad("key must be lower_snake_case")
    row = db.get(AppConfigEntry, key)
    # Merge: validate the partial input first, then merge into the full existing value
    if merge and isinstance(value, dict) and row is not None:
        try:
            existing = json.loads(row.value)
        except Exception:
            existing = {}
        if isinstance(existing, dict):
            validate_config_value(key, value)   # validate only the incoming partial dict
            value = {**existing, **value}        # merge: existing keys preserved, incoming keys overwrite
    validate_config_value(key, value)
    if row is None:
        row = AppConfigEntry(key=key, value="null", description=description or "", is_public=True if is_public is None else is_public)
        db.add(row)
    row.value = json.dumps(value, sort_keys=True)
    if description is not None:
        row.description = description
    if is_public is not None:
        row.is_public = is_public
    row.updated_at = datetime.now(timezone.utc)
    db.commit()
    return row
