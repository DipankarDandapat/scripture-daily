"""Studio template catalogue and the Wisdom Tree reading history."""
from datetime import date, timedelta

from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import or_, select
from sqlalchemy.orm import Session

from app.api.v1.routes import client_day, current_user, user_from_credentials
from app.core.database import get_db
from app.models.entities import Religion, ReadingDay, StudioTemplate, User, UserPreference
<<<<<<< HEAD
from app.schemas.api import TemplateOut, TreeOut, TreeReadInput
from app.services.tree import tree_state
=======
from app.content.tree_activities import ACTIVITY_EVENTS
from app.schemas.api import TemplateOut, TreeActivityEventInput, TreeOut, TreeReadInput, TreeSettingsInput
from app.services.tree_activities import build_tree, get_setting, reading_days, record_event, set_enabled
>>>>>>> settings_redesign

router = APIRouter()
optional_bearer = HTTPBearer(auto_error=False)


@router.get("/studio/templates", response_model=list[TemplateOut])
def studio_templates(
    kind: str = Query(default="all", pattern="^(all|share|video)$"),
    religion_id: int | None = None, religion_code: str | None = None,
    credentials: HTTPAuthorizationCredentials | None = Depends(optional_bearer), db: Session = Depends(get_db),
):
    """Common templates plus the ones tagged for the user's tradition (and nothing from other traditions).

    The tradition comes from ``religion_code`` / ``religion_id`` or, failing that, the signed-in user's saved
    preference. With no tradition known only common templates are returned.
    """
    code = religion_code.lower() if religion_code else None
    if code is None and religion_id is not None:
        religion = db.get(Religion, religion_id)
        if religion is None:
            raise HTTPException(status_code=404, detail="Tradition not found")
        code = religion.code
    if code is None:
        user = user_from_credentials(db, credentials)
        prefs = db.get(UserPreference, user.id) if user else None
        if prefs:
            religion = db.get(Religion, prefs.religion_id)
            code = religion.code if religion else None
    query = select(StudioTemplate).where(StudioTemplate.is_active.is_(True))
    query = query.where(or_(StudioTemplate.tradition.is_(None), StudioTemplate.tradition == code) if code else StudioTemplate.tradition.is_(None))
    if kind != "all":
        query = query.where(StudioTemplate.kind.in_((kind, "both")))
    return list(db.scalars(query.order_by(StudioTemplate.sort_order, StudioTemplate.id)).all())


# ── Wisdom Tree ────────────────────────────────────────────────────────────
<<<<<<< HEAD
def _days(db: Session, user: User) -> set[date]:
    return set(db.scalars(select(ReadingDay.day).where(ReadingDay.user_id == user.id)).all())


@router.get("/tree", response_model=TreeOut)
def get_tree(client_date: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    return tree_state(_days(db, user), client_day(client_date))
=======
@router.get("/tree", response_model=TreeOut)
def get_tree(client_date: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    return build_tree(db, user, client_day(client_date))


@router.put("/tree/settings", response_model=TreeOut)
def set_tree_settings(body: TreeSettingsInput, client_date: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Enable / disable the Wisdom Tree. Re-enabling after a disable restarts the journey from the first activity."""
    today = client_day(client_date)
    set_enabled(db, user, body.enabled, today)
    return build_tree(db, user, today)


@router.post("/tree/activity", response_model=TreeOut)
def report_tree_activity(body: TreeActivityEventInput, client_date: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Report a client-side activity (share, explain, story, studio, truths, wallpaper, widget, discover)."""
    event = body.event.lower()
    if event not in ACTIVITY_EVENTS:
        raise HTTPException(status_code=422, detail=f"event must be one of: {', '.join(sorted(ACTIVITY_EVENTS))}")
    today = client_day(client_date)
    if get_setting(db, user).enabled:
        record_event(db, user, event, today)
    return build_tree(db, user, today)
>>>>>>> settings_redesign


@router.post("/tree/read", response_model=TreeOut)
def record_reading(body: TreeReadInput | None = None, client_date: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Record today's completed reading (idempotent per day) and merge any dates this device recorded offline."""
    today = client_day(client_date)
<<<<<<< HEAD
    have = _days(db, user)
    wanted = {today} if (body is None or body.mark_today) else set()
=======
    setting = get_setting(db, user)
    have = set(db.scalars(select(ReadingDay.day).where(ReadingDay.user_id == user.id)).all())  # full history, even before a restart
    wanted = {today} if (body is None or (body.mark_today and setting.enabled)) else set()
>>>>>>> settings_redesign
    horizon = today - timedelta(days=800)
    for raw in (body.dates if body else []):
        try:
            d = date.fromisoformat(raw)
        except ValueError:
            raise HTTPException(status_code=422, detail=f"Invalid date: {raw}")
        if horizon <= d <= today:  # no future dates, nothing absurdly old
            wanted.add(d)
    for d in sorted(wanted - have):
        db.add(ReadingDay(user_id=user.id, day=d))
    db.commit()
<<<<<<< HEAD
    return tree_state(have | wanted, today)
=======
    return build_tree(db, user, today)
>>>>>>> settings_redesign
