from datetime import date
import jwt
from fastapi import APIRouter, Depends, Header, HTTPException, Query, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import or_, select
from sqlalchemy.orm import Session
from app.core.config import settings
from app.core.database import get_db
from app.core.security import create_access_token, hash_password, verify_password
from app.models.entities import Religion, Language, Scripture, Verse, User, UserPreference, Favorite, Device, Feedback
from app.schemas.api import AuthInput, TokenOut, UserOut, CatalogOut, ReligionOut, ScriptureItem, VerseOut, PreferenceInput, PreferenceOut, FavoriteInput, DeviceInput, VerseCreate, FeedbackInput, FeedbackOut

router = APIRouter()
bearer = HTTPBearer()
optional_bearer = HTTPBearer(auto_error=False)


def current_user(credentials: HTTPAuthorizationCredentials = Depends(bearer), db: Session = Depends(get_db)) -> User:
    try:
        payload = jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        user_id = int(payload["sub"])
    except (jwt.PyJWTError, KeyError, ValueError):
        raise HTTPException(status_code=401, detail="Invalid or expired access token", headers={"WWW-Authenticate": "Bearer"})
    user = db.get(User, user_id)
    if user is None:
        raise HTTPException(status_code=401, detail="User no longer exists")
    return user


def verse_out(v: Verse) -> dict:
    return {"id": v.id, "scripture": v.scripture.name, "religion": v.scripture.religion.name,
            "book": v.book, "chapter": v.chapter, "verse_number": v.verse_number,
            "language": v.language.code, "text": v.text, "translation": v.translation,
            "source": v.source, "license": v.license, "is_demo": v.is_demo}


def verse_query(db: Session):
    return select(Verse).join(Verse.scripture).join(Scripture.religion).join(Verse.language).where(Verse.is_active.is_(True))


def pick_verse(db: Session, *, religion_id: int | None = None, scripture_id: int | None = None, language_id: int | None = None) -> Verse:
    query = verse_query(db)
    if religion_id is not None: query = query.where(Scripture.religion_id == religion_id)
    if scripture_id is not None: query = query.where(Verse.scripture_id == scripture_id)
    if language_id is not None: query = query.where(Verse.language_id == language_id)
    verses = list(db.scalars(query.order_by(Verse.id)).all())
    if not verses:
        raise HTTPException(status_code=404, detail="No published verses match these preferences")
    return verses[(date.today().toordinal() - 1) % len(verses)]


@router.get("/health")
def health(): return {"status": "ok", "service": "scripture-daily-api"}


@router.post("/auth/register", response_model=TokenOut, status_code=201)
def register(data: AuthInput, db: Session = Depends(get_db)):
    email = data.email.lower()
    if db.scalar(select(User).where(User.email == email)):
        raise HTTPException(status_code=409, detail="An account with this email already exists")
    user = User(email=email, password_hash=hash_password(data.password))
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
def languages(db: Session = Depends(get_db)):
    return db.scalars(select(Language).order_by(Language.id)).all()


@router.get("/scriptures", response_model=list[CatalogOut])
def scriptures(religion_id: int | None = None, db: Session = Depends(get_db)):
    query = select(Scripture).order_by(Scripture.id)
    if religion_id is not None: query = query.where(Scripture.religion_id == religion_id)
    return [{"id": s.id, "name": s.name, "code": s.code, "religion": s.religion.name} for s in db.scalars(query).all()]


@router.get("/verses/today", response_model=VerseOut)
def today(credentials: HTTPAuthorizationCredentials | None = Depends(optional_bearer), db: Session = Depends(get_db)):
    religion_id: int | None = None
    scripture_id: int | None = None
    language_id: int | None = None
    if credentials:
        try:
            payload = jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
            user = db.get(User, int(payload["sub"]))
        except (jwt.PyJWTError, KeyError, ValueError):
            raise HTTPException(status_code=401, detail="Invalid or expired access token")
        if user is None:
            raise HTTPException(status_code=401, detail="User no longer exists")
        prefs = db.get(UserPreference, user.id)
        if prefs:
            religion_id = prefs.religion_id
            language_id = prefs.language_id
            # Pick first scripture_id from comma-separated list
            ids = [int(x) for x in prefs.scripture_ids.split(",") if x.strip().isdigit()]
            scripture_id = ids[0] if ids else None
    if scripture_id is None:
        selected_scripture = db.scalar(select(Scripture).where(Scripture.religion_id == religion_id).order_by(Scripture.id)) if religion_id else db.scalar(select(Scripture).order_by(Scripture.id))
        if selected_scripture is None:
            raise HTTPException(status_code=404, detail="No scripture matches this religion")
        scripture_id = selected_scripture.id
        religion_id = selected_scripture.religion_id
    if language_id is None:
        language = db.scalar(select(Language).order_by(Language.id))
        if language is None:
            raise HTTPException(status_code=404, detail="No language is available")
        language_id = language.id
    return verse_out(pick_verse(db, religion_id=religion_id, scripture_id=scripture_id, language_id=language_id))


@router.get("/verses/random", response_model=VerseOut)
def random_verse(religion_id: int | None = None, scripture_id: int | None = None, language_id: int | None = None, db: Session = Depends(get_db)):
    from sqlalchemy import func
    query = verse_query(db)
    if religion_id is not None: query = query.where(Scripture.religion_id == religion_id)
    if scripture_id is not None: query = query.where(Verse.scripture_id == scripture_id)
    if language_id is not None: query = query.where(Verse.language_id == language_id)
    v = db.scalar(query.order_by(func.random()).limit(1))
    if v is None: raise HTTPException(status_code=404, detail="No published verses match these preferences")
    return verse_out(v)


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
    if data.frequency not in {"daily", "6h", "1h"}: raise HTTPException(status_code=422, detail="frequency must be daily, 6h, or 1h")
    ids = [int(x) for x in data.scripture_ids.split(",") if x.strip().isdigit()]
    if not ids: raise HTTPException(status_code=422, detail="scripture_ids must contain at least one valid ID")
    for sid in ids:
        scripture = db.get(Scripture, sid)
        if not scripture or scripture.religion_id != data.religion_id:
            raise HTTPException(status_code=422, detail=f"Scripture {sid} must belong to the selected religion")
    if not db.get(Language, data.language_id): raise HTTPException(status_code=422, detail="Unknown language")
    prefs = get_or_create_preferences(user, db)
    for key, value in data.model_dump().items(): setattr(prefs, key, value)
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
    row = db.scalar(select(Device).where(Device.user_id == user.id, Device.device_token == data.device_token))
    if row is None: db.add(Device(user_id=user.id, **data.model_dump()))
    else: row.platform, row.device_model = data.platform, data.device_model
    db.commit()
    return {"ok": True}


@router.post("/feedback", response_model=FeedbackOut, status_code=201)
def submit_feedback(data: FeedbackInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    row = Feedback(user_id=user.id, rating=data.rating, comment=data.comment, consent=data.consent)
    db.add(row); db.commit(); db.refresh(row)
    return row


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
