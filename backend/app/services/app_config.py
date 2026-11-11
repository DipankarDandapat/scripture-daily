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
    for key in _NESTED_DEFAULT_KEYS:
        _add_missing_subkeys(db, key)
    return added


# Object-valued keys whose new sub-keys are added to existing rows (values already edited by an admin are kept).
_NESTED_DEFAULT_KEYS = ("app_update", "maintenance_mode", "ambient_wisdom")


def _add_missing_subkeys(db: Session, key: str) -> None:
    row = db.get(AppConfigEntry, key)
    if row is None:
        return
    try:
        current = json.loads(row.value)
    except Exception:
        return
    default = DEFAULTS[key][0]
    if not isinstance(current, dict) or not isinstance(default, dict):
        return
    missing = {k: v for k, v in default.items() if k not in current}
    if missing:
        row.value = json.dumps({**current, **missing}, sort_keys=True)
        row.updated_at = datetime.now(timezone.utc)
        db.commit()


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


_VERSION_RE = re.compile(r"^\d{1,4}(\.\d{1,4}){0,3}$")
_HTTP_RE = re.compile(r"^(https://|market://)\S+$")


def parse_version(value: str) -> tuple[int, ...]:
    """"1.4.2" -> (1, 4, 2). Missing segments compare as 0, so "1.4" == "1.4.0"."""
    parts = [int(p) for p in str(value).strip().split(".") if p.isdigit()]
    while parts and parts[-1] == 0:
        parts.pop()
    return tuple(parts)


def is_older(installed: str, other: str) -> bool:
    return parse_version(installed) < parse_version(other)


def evaluate_gate(config: dict, installed_version: str) -> dict:
    """The decision the Android app makes on startup, mirrored here for admin previews and tests.

    Priority (documented in docs/APP_CONFIG_AND_MAINTENANCE.md): mandatory update > maintenance > optional update > normal.
    """
    update = config.get("app_update") or {}
    maintenance = config.get("maintenance_mode") or {}
    status = "up_to_date"
    if update.get("enabled", True):
        latest = str(update.get("latest_version") or installed_version)
        minimum = str(update.get("minimum_supported_version") or "0")
        if is_older(installed_version, minimum) or (update.get("force_update") and is_older(installed_version, latest)):
            status = "mandatory"
        elif is_older(installed_version, latest):
            status = "optional"
    else:
        status = "disabled"
    if status == "mandatory":
        screen = "mandatory_update"
    elif maintenance.get("enabled"):
        screen = "maintenance"
    elif status == "optional":
        screen = "optional_update"
    else:
        screen = "app"
    return {"installed_version": installed_version, "update_status": status, "screen": screen}


def _check_text(key: str, obj: dict, name: str, max_len: int, required: bool = False) -> None:
    if name not in obj:
        if required:
            _bad(f"{key}.{name} is required")
        return
    v = obj[name]
    if not isinstance(v, str) or (required and not v.strip()) or len(v) > max_len:
        _bad(f"{key}.{name} must be text of at most {max_len} characters")


def _validate_app_update(value: object) -> None:
    if not isinstance(value, dict):
        _bad("app_update must be an object")
    for name in ("enabled", "force_update"):
        if name in value and not isinstance(value[name], bool):
            _bad(f"app_update.{name} must be true or false")
    for name in ("latest_version", "minimum_supported_version"):
        if name in value and not (isinstance(value[name], str) and _VERSION_RE.match(value[name])):
            _bad(f"app_update.{name} must look like 1.4.2")
    if "latest_version" in value and "minimum_supported_version" in value and \
            is_older(value["latest_version"], value["minimum_supported_version"]):
        _bad("app_update.minimum_supported_version cannot be newer than latest_version")
    for name, n in (("title", 80), ("mandatory_title", 80), ("message", 400), ("mandatory_message", 400)):
        _check_text("app_update", value, name, n)
    if "play_store_url" in value and not (isinstance(value["play_store_url"], str) and _HTTP_RE.match(value["play_store_url"])):
        _bad("app_update.play_store_url must start with https:// or market://")
    if "optional_reminder_hours" in value and not (isinstance(value["optional_reminder_hours"], int) and 0 <= value["optional_reminder_hours"] <= 720):
        _bad("app_update.optional_reminder_hours must be an integer between 0 and 720")


def _validate_maintenance(value: object) -> None:
    if not isinstance(value, dict):
        _bad("maintenance_mode must be an object")
    for name in ("enabled", "allow_retry"):
        if name in value and not isinstance(value[name], bool):
            _bad(f"maintenance_mode.{name} must be true or false")
    _check_text("maintenance_mode", value, "title", 80)
    _check_text("maintenance_mode", value, "message", 600)
    end = value.get("estimated_end_time")
    if end is not None:
        try:
            parsed = datetime.fromisoformat(str(end).replace("Z", "+00:00"))
        except ValueError:
            parsed = None
        if parsed is None or parsed.tzinfo is None:
            _bad("maintenance_mode.estimated_end_time must be ISO-8601 with a timezone, e.g. 2026-11-12T06:30:00Z, or null")
    if "enforce_cached_for_hours" in value and not (isinstance(value["enforce_cached_for_hours"], int) and 0 <= value["enforce_cached_for_hours"] <= 168):
        _bad("maintenance_mode.enforce_cached_for_hours must be an integer between 0 and 168")


def validate_config_value(key: str, value: object) -> None:
    """Reject malformed configuration so a bad edit can never crash apps in the field."""
    if key == "app_update":
        _validate_app_update(value)
    if key == "maintenance_mode":
        _validate_maintenance(value)
    if key == "ambient_wisdom":
        if not isinstance(value, dict) or any(not isinstance(v, bool) for v in value.values()):
            _bad("ambient_wisdom must be an object of true/false switches")
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
