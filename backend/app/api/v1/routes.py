from datetime import date, datetime, timedelta, timezone
import random
from threading import Lock
import jwt
from fastapi import APIRouter, Depends, Header, HTTPException, Query, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import distinct, or_, select
from sqlalchemy.orm import Session
from app.core.config import settings
from app.core.database import get_db
from app.core.security import create_access_token, hash_password, verify_password
from app.models.entities import Religion, Language, Scripture, Verse, User, UserPreference, ActiveVerseState, VerseSelectionHistory, Favorite, Device, Feedback
from app.schemas.api import AuthInput, TokenOut, UserOut, CatalogOut, ReligionOut, ScriptureItem, VerseOut, PreferenceInput, PreferenceOut, FavoriteInput, DeviceInput, FcmTokenInput, DeviceVersionInput, VerseCreate, FeedbackInput, FeedbackOut
from app.core.firebase import send_to_tokens, send_to_topic

router = APIRouter()
# Lock-screen wallpaper behaviour. verse_and_image = "Change Verse and Image Together" (default); keep_image = "Keep Same Image";
# colors = both options off, so the verse is drawn on the multi-theme colour backgrounds. Legacy values are mapped, not rejected.
WALLPAPER_MODES = {"verse_and_image", "keep_image", "colors"}
WALLPAPER_MODE_ALIASES = {"change_verse": "verse_and_image", "keep_same_verse": "verse_and_image"}
_ACTIVE_VERSE_LOCKS: dict[str, Lock] = {}
_ACTIVE_VERSE_LOCKS_GUARD = Lock()


def _active_verse_lock(user_id: str) -> Lock:
    with _ACTIVE_VERSE_LOCKS_GUARD:
        return _ACTIVE_VERSE_LOCKS.setdefault(user_id, Lock())


bearer = HTTPBearer()
optional_bearer = HTTPBearer(auto_error=False)


def current_user(credentials: HTTPAuthorizationCredentials = Depends(bearer), db: Session = Depends(get_db)) -> User:
    try:
        payload = jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        user_id = payload["sub"]  # str — 10-char alphanumeric
    except (jwt.PyJWTError, KeyError):
        raise HTTPException(status_code=401, detail="Invalid or expired access token", headers={"WWW-Authenticate": "Bearer"})
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=401, detail="User no longer exists")
    return user


def verse_out(v: Verse) -> dict:
    return {"id": v.id, "scripture_id": v.scripture_id, "language_id": v.language_id,
            "scripture": v.scripture.name, "religion": v.scripture.religion.name,
            "book": v.book, "chapter": v.chapter, "verse_number": v.verse_number,
            "language": v.language.code, "text": v.text, "translation": v.translation,
            "source": v.source, "license": v.license, "is_demo": v.is_demo,
            "topics": [t.code for t in v.topics], "explanation_available": bool(v.simple_explanation)}


def verse_query(db: Session):
    return select(Verse).join(Verse.scripture).join(Scripture.religion).join(Verse.language).where(Verse.is_active.is_(True))


def _religion_language_ids(db: Session, religion_id: int) -> list[int]:
    from app.models.entities import ReligionLanguage
    return list(db.scalars(select(ReligionLanguage.language_id).where(ReligionLanguage.religion_id == religion_id).order_by(ReligionLanguage.sort_order)).all())


def pool_for(db: Session, *, religion_id: int | None, language_id: int | None, scripture_ids: list[int] | None = None) -> list[Verse]:
    """Every active verse of the tradition in the language, ordered by id.

    Never crosses traditions. When the tradition has nothing in the requested language (missing
    translation) it falls back to the tradition's own other languages, English first, never to another tradition.
    """
    def fetch(language: int | None) -> list[Verse]:
        query = verse_query(db)
        if religion_id is not None: query = query.where(Scripture.religion_id == religion_id)
        if scripture_ids: query = query.where(Verse.scripture_id.in_(scripture_ids))
        if language is not None: query = query.where(Verse.language_id == language)
        return list(db.scalars(query.order_by(Verse.id)).all())
    verses = fetch(language_id)
    if verses or language_id is None or religion_id is None:
        return verses
    english = db.scalar(select(Language.id).where(Language.code == "en"))
    for fallback in [x for x in [english, *_religion_language_ids(db, religion_id)] if x and x != language_id]:
        verses = fetch(fallback)
        if verses: return verses
    return []


def pick_verse(db: Session, *, religion_id: int | None = None, scripture_id: int | None = None, language_id: int | None = None,
               scripture_ids: list[int] | None = None, on: date | None = None) -> Verse:
    verses = pool_for(db, religion_id=religion_id, language_id=language_id, scripture_ids=[scripture_id] if scripture_id else scripture_ids)
    if not verses:
        raise HTTPException(status_code=404, detail="No published verses match these preferences")
    return verses[((on or date.today()).toordinal() - 1) % len(verses)]


def client_day(client_date: str | None) -> date:
    """The caller's local date (clamped to +-1 day of UTC so a bad client cannot jump the calendar)."""
    from datetime import datetime, timezone
    utc_today = datetime.now(timezone.utc).date()
    if not client_date:
        return utc_today
    try:
        d = date.fromisoformat(client_date)
    except ValueError:
        raise HTTPException(status_code=422, detail="client_date must be YYYY-MM-DD")
    return d if abs((d - utc_today).days) <= 1 else utc_today


def user_from_credentials(db: Session, credentials: HTTPAuthorizationCredentials | None) -> User | None:
    if credentials is None: return None
    try:
        payload = jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        user = db.get(User, payload["sub"])
    except (jwt.PyJWTError, KeyError):
        raise HTTPException(status_code=401, detail="Invalid or expired access token")
    return user  # unknown user -> anonymous for public endpoints; write endpoints use current_user and still return 401


def preference_pool(db: Session, user: User | None, religion_id: int | None, language_id: int | None) -> tuple[int | None, int | None, list[int]]:
    """Explicit query values win; otherwise the signed-in user's saved tradition, language and scriptures."""
    scripture_ids: list[int] = []
    if user is not None:
        prefs = db.get(UserPreference, user.id)
        if prefs:
            if religion_id is None: religion_id = prefs.religion_id
            if language_id is None: language_id = prefs.language_id
            if religion_id == prefs.religion_id:
                scripture_ids = [int(x) for x in prefs.scripture_ids.split(",") if x.strip().isdigit()]
    if religion_id is None:
        first = db.scalar(select(Religion).order_by(Religion.id))
        religion_id = first.id if first else None
    if language_id is None and religion_id is not None:
        langs = _religion_language_ids(db, religion_id)
        language_id = langs[0] if langs else db.scalar(select(Language.id).order_by(Language.id))
    return religion_id, language_id, scripture_ids


@router.get("/health")
def health(): return {"status": "ok", "service": "scripture-daily-api"}


@router.post("/auth/register", response_model=TokenOut, status_code=201)
def register(data: AuthInput, db: Session = Depends(get_db)):
    email = data.email.lower()
    if db.scalar(select(User).where(User.email == email)):
        raise HTTPException(status_code=409, detail="An account with this email already exists")
    user = User(email=email, password_hash=hash_password(data.password))  # id auto-generated by _new_uid
    db.add(user); db.flush()
    first_religion = db.scalar(select(Religion).order_by(Religion.id))
    first_language = db.scalar(select(Language).order_by(Language.id))
    first_scripture = db.scalar(select(Scripture).order_by(Scripture.id))
    if first_religion and first_language and first_scripture:
        db.add(UserPreference(user_id=user.id, religion_id=first_religion.id, language_id=first_language.id, scripture_ids=str(first_scripture.id)))
    db.commit(); db.refresh(user)
    return {"access_token": create_access_token(user.id), "token_type": "bearer"}


@router.post("/auth/login", response_model=TokenOut)
def login(data: AuthInput, db: Session = Depends(get_db)):
    user = db.scalar(select(User).where(User.email == data.email.lower()))
    if user is None or not verify_password(data.password, user.password_hash):
        raise HTTPException(status_code=401, detail="Incorrect email or password")
    return {"access_token": create_access_token(user.id), "token_type": "bearer"}


@router.get("/users/me", response_model=UserOut)
def me(user: User = Depends(current_user)): return user


@router.get("/religions", response_model=list[ReligionOut])
def religions(db: Session = Depends(get_db)):
    rows = db.scalars(select(Religion).order_by(Religion.id)).all()
    return [
        ReligionOut(
            id=r.id, name=r.name, code=r.code,
            scriptures=[ScriptureItem(id=s.id, name=s.name, code=s.code) for s in r.scriptures]
        ) for r in rows
    ]


@router.get("/languages", response_model=list[CatalogOut])
def languages(religion_id: int | None = None, db: Session = Depends(get_db)):
    if religion_id is not None:
        from sqlalchemy import join
        religion = db.get(Religion, religion_id)
        if religion is None:
            raise HTTPException(status_code=404, detail="Religion not found")
        from app.models.entities import ReligionLanguage
        query = (
            select(Language)
            .join(ReligionLanguage, ReligionLanguage.language_id == Language.id)
            .where(ReligionLanguage.religion_id == religion_id)
            .order_by(ReligionLanguage.sort_order)
        )
        langs = db.scalars(query).all()
    else:
        langs = db.scalars(select(Language).order_by(Language.id)).all()
    return [{"id": l.id, "name": l.name, "code": l.code, "native_script": l.native_script} for l in langs]


@router.get("/scriptures", response_model=list[CatalogOut])
def scriptures(religion_id: int | None = None, db: Session = Depends(get_db)):
    query = select(Scripture).order_by(Scripture.id)
    if religion_id is not None: query = query.where(Scripture.religion_id == religion_id)
    return [{"id": s.id, "name": s.name, "code": s.code, "religion": s.religion.name} for s in db.scalars(query).all()]


@router.get("/verses/today", response_model=VerseOut)
def today(client_date: str | None = None, religion_id: int | None = None, language_id: int | None = None, credentials: HTTPAuthorizationCredentials | None = Depends(optional_bearer), db: Session = Depends(get_db)):
    """The verse of the day, drawn from the user's whole tradition in their language (never from another tradition)."""
    user = user_from_credentials(db, credentials)
    religion_id, language_id, scripture_ids = preference_pool(db, user, religion_id, language_id)
    if language_id is not None and religion_id is not None and language_id not in _religion_language_ids(db, religion_id):
        language_id = None  # not offered for this tradition: fall back inside it
    return verse_out(pick_verse(db, religion_id=religion_id, language_id=language_id, scripture_ids=scripture_ids, on=client_day(client_date)))


@router.get("/verses/random", response_model=VerseOut)
def random_verse(religion_id: int | None = None, scripture_id: int | None = None, language_id: int | None = None, exclude_id: int | None = None, db: Session = Depends(get_db)):
    import random
    if scripture_id is not None:
        scripture = db.get(Scripture, scripture_id)
        if scripture is None: raise HTTPException(status_code=404, detail="Scripture not found")
        if religion_id is not None and scripture.religion_id != religion_id:
            raise HTTPException(status_code=422, detail="That scripture does not belong to the selected tradition")
        religion_id = scripture.religion_id
    verses = pool_for(db, religion_id=religion_id, language_id=language_id, scripture_ids=[scripture_id] if scripture_id else None)
    if not verses: raise HTTPException(status_code=404, detail="No published verses match these preferences")
    candidates = [v for v in verses if v.id != exclude_id] or verses  # prefer something different, but never fail
    return verse_out(random.choice(candidates))


@router.get("/verses/search", response_model=list[VerseOut])
def search_verses(q: str = Query(min_length=2, max_length=120), language_id: int | None = None, scripture_id: int | None = None, db: Session = Depends(get_db)):
    query = verse_query(db).where(or_(Verse.text.ilike(f"%{q}%"), Verse.translation.ilike(f"%{q}%")))
    if language_id is not None: query = query.where(Verse.language_id == language_id)
    if scripture_id is not None: query = query.where(Verse.scripture_id == scripture_id)
    return [verse_out(v) for v in db.scalars(query.order_by(Verse.id).limit(100)).all()]


@router.get("/scriptures/{scripture_id}/chapters/{chapter}/verses", response_model=list[VerseOut])
def chapter_verses(scripture_id: int, chapter: int, language_id: int | None = None, db: Session = Depends(get_db)):
    query = verse_query(db).where(Verse.scripture_id == scripture_id, Verse.chapter == chapter)
    if language_id is not None: query = query.where(Verse.language_id == language_id)
    return [verse_out(v) for v in db.scalars(query.order_by(Verse.verse_number)).all()]


def _frequency_hours(value: str) -> int:
    return {"1h": 1, "6h": 6, "12h": 12, "24h": 24, "daily": 24}.get(value, 24)


def _aware(value: datetime) -> datetime:
    return value.replace(tzinfo=timezone.utc) if value.tzinfo is None else value


def _active_payload(state: ActiveVerseState, verse: Verse) -> dict:
    return {
        "success": True,
        "data": {
            "verse_id": verse.id,
            "scripture_id": verse.scripture_id,
            "tradition_id": state.religion_id,
            "language_id": state.language_id,
            "book": verse.book,
            "chapter": verse.chapter,
            "verse_number": verse.verse_number,
            "text": verse.text,
            "translation": verse.translation,
            "selected_at": _aware(state.selected_at).isoformat(),
            "next_update_at": _aware(state.next_update_at).isoformat(),
            "selection_mode": state.selection_source,
            "verse": verse_out(verse),
        },
    }


def _select_active_verse(user: User, prefs: UserPreference, db: Session, source: str, exclude_id: int | None = None) -> dict:
    scripture_ids = [int(x) for x in prefs.scripture_ids.split(",") if x.strip().isdigit()]
    candidates = pool_for(db, religion_id=prefs.religion_id, language_id=prefs.language_id, scripture_ids=scripture_ids or None)
    if not candidates:
        raise HTTPException(status_code=404, detail="No published verses match these preferences")
    eligible = [item for item in candidates if item.id != exclude_id]
    chosen = random.choice(eligible or candidates)
    now = datetime.now(timezone.utc)
    state = db.get(ActiveVerseState, user.id)
    if state is None:
        state = ActiveVerseState(user_id=user.id, verse_id=chosen.id, religion_id=prefs.religion_id,
            language_id=prefs.language_id, selected_at=now,
            next_update_at=now + timedelta(hours=_frequency_hours(prefs.frequency)), selection_source=source)
        db.add(state)
    else:
        state.verse_id = chosen.id
        state.religion_id = prefs.religion_id
        state.language_id = prefs.language_id
        state.selected_at = now
        state.next_update_at = now + timedelta(hours=_frequency_hours(prefs.frequency))
        state.selection_source = source
    db.add(VerseSelectionHistory(user_id=user.id, verse_id=chosen.id, selected_at=now, selection_source=source))
    db.commit()
    db.refresh(state)
    return _active_payload(state, chosen)


@router.get("/verses/active")
def get_active_verse(user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Return the persisted active verse; rotate only when its scheduled deadline has passed."""
    with _active_verse_lock(user.id):
        # PostgreSQL locks this stable user row across API workers; the process lock also serialises SQLite requests.
        db.execute(select(User.id).where(User.id == user.id).with_for_update()).first()
        prefs = get_or_create_preferences(user, db)
        state = db.get(ActiveVerseState, user.id)
        now = datetime.now(timezone.utc)
        if state is not None:
            state_next = _aware(state.next_update_at)
            same_preferences = state.religion_id == prefs.religion_id and state.language_id == prefs.language_id
            active = db.get(Verse, state.verse_id)
            if same_preferences and active is not None and state_next > now:
                return _active_payload(state, active)
            previous_id = state.verse_id
        else:
            previous_id = None
        return _select_active_verse(user, prefs, db, "scheduled", previous_id)


@router.get("/verses/active/current")
def get_current_active_verse(user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Return the user's current verse without rotating it, regardless of its scheduled deadline.

    If the user has no active selection yet (or preferences changed), select the initial matching verse.
    Pull-to-refresh uses this endpoint so a UI refresh never advances a valid active verse.
    """
    with _active_verse_lock(user.id):
        db.execute(select(User.id).where(User.id == user.id).with_for_update()).first()
        prefs = get_or_create_preferences(user, db)
        state = db.get(ActiveVerseState, user.id)
        if state is not None:
            same_preferences = state.religion_id == prefs.religion_id and state.language_id == prefs.language_id
            active = db.get(Verse, state.verse_id)
            if same_preferences and active is not None:
                return _active_payload(state, active)
            previous_id = state.verse_id
        else:
            previous_id = None
        return _select_active_verse(user, prefs, db, "initial", previous_id)


@router.post("/verses/discover")
def discover_active_verse(user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Explicit user action: select and persist one new verse, avoiding immediate repetition."""
    with _active_verse_lock(user.id):
        db.execute(select(User.id).where(User.id == user.id).with_for_update()).first()
        prefs = get_or_create_preferences(user, db)
        current = db.get(ActiveVerseState, user.id)
        return _select_active_verse(user, prefs, db, "manual", current.verse_id if current else None)


@router.post("/verses/active/refresh")
def refresh_active_verse_if_due(user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Scheduler-safe operation: keep the active verse unless the configured deadline has passed."""
    return get_active_verse(user, db)



@router.get("/verses/{verse_id}", response_model=VerseOut)
def get_verse(verse_id: int, db: Session = Depends(get_db)):
    v = db.scalar(verse_query(db).where(Verse.id == verse_id))
    if v is None: raise HTTPException(status_code=404, detail="Verse not found")
    return verse_out(v)


def get_or_create_preferences(user: User, db: Session) -> UserPreference:
    prefs = db.get(UserPreference, user.id)
    if prefs is None:
        religion = db.scalar(select(Religion).order_by(Religion.id)); language = db.scalar(select(Language).order_by(Language.id)); scripture = db.scalar(select(Scripture).order_by(Scripture.id))
        if not religion or not language or not scripture: raise HTTPException(status_code=503, detail="Scripture catalog is not initialized")
        prefs = UserPreference(user_id=user.id, religion_id=religion.id, language_id=language.id, scripture_ids=str(scripture.id))
        db.add(prefs); db.commit(); db.refresh(prefs)
    return prefs


@router.get("/users/preferences", response_model=PreferenceOut)
def read_preferences(user: User = Depends(current_user), db: Session = Depends(get_db)):
    return get_or_create_preferences(user, db)


@router.put("/users/preferences", response_model=PreferenceOut)
def save_preferences(data: PreferenceInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    with _active_verse_lock(user.id):
        db.execute(select(User.id).where(User.id == user.id).with_for_update()).first()
        if data.frequency not in {"daily", "1h", "6h", "12h", "24h"}: raise HTTPException(status_code=422, detail="frequency must be daily, 1h, 6h, 12h, or 24h")
        mode = WALLPAPER_MODE_ALIASES.get(data.wallpaper_mode, data.wallpaper_mode)
        if mode not in WALLPAPER_MODES: raise HTTPException(status_code=422, detail="wallpaper_mode must be verse_and_image, keep_image or colors")
        data.wallpaper_mode = mode
        ids = [int(x) for x in data.scripture_ids.split(",") if x.strip().isdigit()]
        if not ids: raise HTTPException(status_code=422, detail="scripture_ids must contain at least one valid ID")
        for sid in ids:
            scripture = db.get(Scripture, sid)
            if not scripture or scripture.religion_id != data.religion_id:
                raise HTTPException(status_code=422, detail=f"Scripture {sid} must belong to the selected religion")
        if not db.get(Language, data.language_id): raise HTTPException(status_code=422, detail="Unknown language")
        if data.language_id not in _religion_language_ids(db, data.religion_id):
            raise HTTPException(status_code=422, detail="That language is not available for the selected tradition")
        prefs = get_or_create_preferences(user, db)
        old_religion, old_language, old_frequency = prefs.religion_id, prefs.language_id, prefs.frequency
        for key, value in data.model_dump().items(): setattr(prefs, key, value)
        active = db.get(ActiveVerseState, user.id)
        if active is not None:
            if old_religion != data.religion_id or old_language != data.language_id:
                db.delete(active)
            elif old_frequency != data.frequency:
                now = datetime.now(timezone.utc)
                active.next_update_at = now + timedelta(hours=_frequency_hours(data.frequency))
        db.commit(); db.refresh(prefs)
        return prefs

@router.get("/favorites", response_model=list[VerseOut])
def list_favorites(user: User = Depends(current_user), db: Session = Depends(get_db)):
    rows = db.scalars(select(Favorite).where(Favorite.user_id == user.id).order_by(Favorite.created_at.desc())).all()
    return [verse_out(row.verse) for row in rows]


@router.post("/favorites", status_code=201)
def add_favorite(data: FavoriteInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    if not db.get(Verse, data.verse_id): raise HTTPException(status_code=404, detail="Verse not found")
    existing = db.scalar(select(Favorite).where(Favorite.user_id == user.id, Favorite.verse_id == data.verse_id))
    if existing is None: db.add(Favorite(user_id=user.id, verse_id=data.verse_id)); db.commit()
    return {"ok": True, "verse_id": data.verse_id}


@router.delete("/favorites/{verse_id}", status_code=204)
def remove_favorite(verse_id: int, user: User = Depends(current_user), db: Session = Depends(get_db)):
    row = db.scalar(select(Favorite).where(Favorite.user_id == user.id, Favorite.verse_id == verse_id))
    if row: db.delete(row); db.commit()


@router.post("/devices", status_code=201)
def register_device(data: DeviceInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    now = datetime.now(timezone.utc)
    row = db.scalar(select(Device).where(Device.user_id == user.id, Device.device_token == data.device_token))
    if row is None:
        row = Device(user_id=user.id, created_at=now, last_seen_at=now, **data.model_dump())
        db.add(row)
    else:
        row.platform = data.platform
        row.device_model = data.device_model
        row.last_seen_at = now
        if data.fcm_token:
            row.fcm_token = data.fcm_token
    db.commit()
    return {"ok": True}


@router.put("/devices/fcm-token", status_code=200)
def update_fcm_token(data: FcmTokenInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Update (or set for the first time) the FCM push token for all of this user's devices."""
    rows = db.scalars(select(Device).where(Device.user_id == user.id)).all()
    if not rows:
        now = datetime.now(timezone.utc)
        db.add(Device(user_id=user.id, device_token=f"fcm_{user.id}", platform="android", fcm_token=data.fcm_token, created_at=now, last_seen_at=now))
    else:
        for row in rows:
            row.fcm_token = data.fcm_token
    db.commit()
    return {"ok": True}


@router.put("/devices/version", status_code=200)
def update_device_version(data: DeviceVersionInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Called on every cold app launch. Updates app_version, app_version_code, os_version
    and last_seen_at for every device row belonging to this user.
    Creates a placeholder row if none exists yet (e.g. FCM-only installs).
    """
    now = datetime.now(timezone.utc)
    rows = db.scalars(select(Device).where(Device.user_id == user.id)).all()
    if not rows:
        db.add(Device(
            user_id=user.id,
            device_token=f"ver_{user.id}",
            platform="android",
            app_version=data.app_version,
            app_version_code=data.app_version_code,
            os_version=data.os_version,
            created_at=now,
            last_seen_at=now,
        ))
    else:
        for row in rows:
            row.app_version = data.app_version
            row.app_version_code = data.app_version_code
            if data.os_version:
                row.os_version = data.os_version
            row.last_seen_at = now
    db.commit()
    return {"ok": True}


@router.post("/notify/all", status_code=202)
def notify_all_users(
    title: str,
    body: str,
    verse_id: int | None = None,
    topic: str | None = None,
    x_admin_token: str | None = Header(default=None),
    db: Session = Depends(get_db)
):
    """Send a push notification to all users.

    - If `topic` is provided (e.g. 'all_users'), sends via Firebase topic broadcast (no token list needed).
    - Otherwise sends to every FCM token stored in the devices table.
    Requires X-Admin-Token header.
    """
    if not settings.admin_token or x_admin_token != settings.admin_token:
        raise HTTPException(status_code=403, detail="Invalid admin token")

    data = {"verse_id": str(verse_id)} if verse_id else {}

    if topic:
        message_id = send_to_topic(topic, title, body, data)
        return {"method": "topic", "topic": topic, "message_id": message_id}

    tokens = list(db.scalars(
        select(Device.fcm_token)
        .where(Device.fcm_token.isnot(None))
        .distinct()
    ).all())

    if not tokens:
        return {"method": "multicast", "queued": 0, "success": 0, "failure": 0, "detail": "No FCM tokens stored yet"}

    result = send_to_tokens(tokens, title, body, data)
    return {"method": "multicast", "queued": len(tokens), **result}


@router.post("/feedback", response_model=FeedbackOut, status_code=201)
def submit_feedback(data: FeedbackInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    row = Feedback(user_id=user.id, rating=data.rating, comment=data.comment, consent=data.consent)
    db.add(row); db.commit(); db.refresh(row)
    return row


@router.get("/verses/today/history", response_model=list[VerseOut])
def today_history(count: int = Query(default=7, ge=1, le=30), client_date: str | None = None,
                  religion_id: int | None = None, language_id: int | None = None,
                  credentials: HTTPAuthorizationCredentials | None = Depends(optional_bearer),
                  db: Session = Depends(get_db)):
    """Return the last `count` daily verses (today + previous days) for the user's preferences."""
    user = user_from_credentials(db, credentials)
    religion_id, language_id, scripture_ids = preference_pool(db, user, religion_id, language_id)
    verses = pool_for(db, religion_id=religion_id, language_id=language_id, scripture_ids=scripture_ids)
    if not verses:
        return []
    today_ord = client_day(client_date).toordinal() - 1
    indices = {(today_ord - i) % len(verses) for i in range(count)}
    ordered = sorted(indices, key=lambda i: -(today_ord - i) % len(verses))
    return [verse_out(verses[i]) for i in ordered]


@router.get("/scriptures/{scripture_id}/chapters", response_model=list[int])
def scripture_chapters(scripture_id: int, language_id: int | None = None, db: Session = Depends(get_db)):
    """Return sorted distinct chapter numbers for a scripture."""
    query = (select(distinct(Verse.chapter))
             .where(Verse.scripture_id == scripture_id, Verse.is_active.is_(True)))
    if language_id is not None:
        query = query.where(Verse.language_id == language_id)
    rows = db.scalars(query.order_by(Verse.chapter)).all()
    return list(rows)


@router.post("/admin/verses", response_model=VerseOut, status_code=201)
def publish_verse(data: VerseCreate, x_admin_token: str | None = Header(default=None), db: Session = Depends(get_db)):
    if not settings.admin_token or x_admin_token != settings.admin_token: raise HTTPException(status_code=403, detail="Invalid admin token")
    if not db.get(Scripture, data.scripture_id) or not db.get(Language, data.language_id): raise HTTPException(status_code=422, detail="Unknown scripture or language")
    row = Verse(**data.model_dump(), is_demo=False)
    db.add(row)
    try: db.commit(); db.refresh(row)
    except Exception:
        db.rollback(); raise HTTPException(status_code=409, detail="This verse reference already exists")
    return verse_out(row)
