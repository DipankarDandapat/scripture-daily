"""Journal Reflection: verse questions plus the signed-in user's saved reflections (create, list, view, edit, delete)."""
from datetime import date, datetime, timedelta, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, Response
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.api.v1.routes import client_day, current_user, get_or_create_preferences, pick_verse, verse_query
from app.core.database import get_db
from app.models.entities import ActiveVerseState, JournalReflection, ReflectionQuestion, User, Verse
from app.schemas.api import ReflectionListOut, ReflectionOut, ReflectionQuestionOut, ReflectionSaveInput, ReflectionUpdateInput, TodayReflectionOut
from app.services.reflections import generic_question, verse_reference

router = APIRouter()
MAX_ANSWER = 500


def _question_for(db: Session, verse: Verse) -> tuple[ReflectionQuestion | None, str, bool]:
    row = db.scalar(select(ReflectionQuestion).where(ReflectionQuestion.verse_id == verse.id, ReflectionQuestion.is_active.is_(True)))
    if row is not None:
        return row, row.reflection_question, False
    return None, generic_question(verse.language.code), True  # never a blank section


def _verse_or_404(db: Session, verse_id: int) -> Verse:
    verse = db.scalar(verse_query(db).where(Verse.id == verse_id))
    if verse is None:
        raise HTTPException(status_code=404, detail="Verse not found")
    return verse


def _reflection_out(r: JournalReflection) -> ReflectionOut:
    return ReflectionOut(
        id=r.id, verse_id=r.verse_id, question_id=r.reflection_question_id, reflection_date=r.reflection_date.isoformat(),
        answer=r.reflection_answer, question=r.question_text, verse_text=r.verse_text, verse_reference=r.verse_reference,
        scripture=r.scripture_name, language=r.language_code,
        created_at=r.created_at.isoformat() if r.created_at else "", updated_at=r.updated_at.isoformat() if r.updated_at else "")


def _clean_answer(raw: str) -> str:
    answer = raw.strip()
    if not answer:
        raise HTTPException(status_code=422, detail="Please write something before saving your reflection")
    if len(answer) > MAX_ANSWER:
        raise HTTPException(status_code=422, detail=f"Your reflection can be at most {MAX_ANSWER} characters")
    return answer


def _owned(db: Session, user: User, reflection_id: int) -> JournalReflection:
    row = db.scalar(select(JournalReflection).where(JournalReflection.id == reflection_id, JournalReflection.user_id == user.id))
    if row is None:
        raise HTTPException(status_code=404, detail="Reflection not found")
    return row


def _todays_verse(db: Session, user: User) -> Verse:
    prefs = get_or_create_preferences(user, db)
    state = db.get(ActiveVerseState, user.id)
    if state is not None and state.religion_id == prefs.religion_id and state.language_id == prefs.language_id:
        verse = db.scalar(verse_query(db).where(Verse.id == state.verse_id))
        if verse is not None:
            return verse
    ids = [int(x) for x in prefs.scripture_ids.split(",") if x.strip().isdigit()]
    return pick_verse(db, religion_id=prefs.religion_id, language_id=prefs.language_id, scripture_ids=ids)


@router.get("/verses/{verse_id}/reflection-question", response_model=ReflectionQuestionOut)
def verse_reflection_question(verse_id: int, db: Session = Depends(get_db)):
    verse = _verse_or_404(db, verse_id)
    row, text, fallback = _question_for(db, verse)
    return ReflectionQuestionOut(verse_id=verse.id, question_id=row.id if row else None, question=text, language=verse.language.code, is_fallback=fallback)


@router.get("/reflections/today", response_model=TodayReflectionOut)
def todays_reflection(verse_id: int | None = None, client_date: str | None = None, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """The question for today's verse (or the given verse) plus today's saved answer, if there already is one."""
    today = client_day(client_date)
    verse = _verse_or_404(db, verse_id) if verse_id is not None else _todays_verse(db, user)
    row, text, fallback = _question_for(db, verse)
    existing = db.scalar(select(JournalReflection).where(JournalReflection.user_id == user.id, JournalReflection.verse_id == verse.id, JournalReflection.reflection_date == today))
    return TodayReflectionOut(
        reflection_date=today.isoformat(), verse_id=verse.id, verse_text=verse.text, verse_reference=verse_reference(verse), scripture=verse.scripture.name,
        language=verse.language.code, question_id=row.id if row else None, question=text, is_fallback=fallback, max_length=MAX_ANSWER,
        existing=_reflection_out(existing) if existing else None)


@router.post("/reflections", response_model=ReflectionOut, status_code=201)
def save_reflection(body: ReflectionSaveInput, response: Response, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Save a reflection. One entry per user, verse and day: saving again the same day updates that entry (HTTP 200)."""
    answer = _clean_answer(body.answer)
    verse = _verse_or_404(db, body.verse_id)
    day = client_day(body.reflection_date)
    row, text, _fallback = _question_for(db, verse)
    existing = db.scalar(select(JournalReflection).where(JournalReflection.user_id == user.id, JournalReflection.verse_id == verse.id, JournalReflection.reflection_date == day))
    if existing is not None:
        existing.reflection_answer = answer
        existing.updated_at = datetime.now(timezone.utc)
        db.commit(); db.refresh(existing)
        response.status_code = 200
        return _reflection_out(existing)
    entry = JournalReflection(
        user_id=user.id, verse_id=verse.id, reflection_question_id=row.id if row else None, reflection_date=day, reflection_answer=answer,
        question_text=text, verse_text=verse.text, verse_reference=verse_reference(verse), scripture_name=verse.scripture.name, language_code=verse.language.code)
    db.add(entry); db.commit(); db.refresh(entry)
    return _reflection_out(entry)


@router.get("/reflections", response_model=ReflectionListOut)
def list_reflections(
    limit: int = Query(default=20, ge=1, le=100), offset: int = Query(default=0, ge=0),
    month: str | None = Query(default=None, pattern=r"^\d{4}-(0[1-9]|1[0-2])$"), on: str | None = Query(default=None, alias="date"),
    user: User = Depends(current_user), db: Session = Depends(get_db),
):
    """The user's reflections, newest reflection date first. Filter by ``month`` (YYYY-MM) or a single ``date``."""
    q = select(JournalReflection).where(JournalReflection.user_id == user.id)
    if month:
        year, mon = int(month[:4]), int(month[5:])
        start = date(year, mon, 1)
        end = date(year + (mon == 12), 1 if mon == 12 else mon + 1, 1)
        q = q.where(JournalReflection.reflection_date >= start, JournalReflection.reflection_date < end)
    if on:
        try:
            q = q.where(JournalReflection.reflection_date == date.fromisoformat(on))
        except ValueError:
            raise HTTPException(status_code=422, detail="date must be YYYY-MM-DD")
    total = db.scalar(select(func.count()).select_from(q.subquery())) or 0
    rows = db.scalars(q.order_by(JournalReflection.reflection_date.desc(), JournalReflection.id.desc()).limit(limit).offset(offset)).all()
    return ReflectionListOut(items=[_reflection_out(r) for r in rows], total=total, limit=limit, offset=offset, has_more=offset + len(rows) < total)


@router.get("/reflections/dates", response_model=list[str])
def reflection_dates(days: int = Query(default=60, ge=1, le=800), user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Distinct dates (YYYY-MM-DD) on which the user saved a reflection in the last ``days`` days (for the date strip)."""
    since = client_day(None) - timedelta(days=days)
    rows = db.scalars(select(JournalReflection.reflection_date).where(JournalReflection.user_id == user.id, JournalReflection.reflection_date >= since).distinct()).all()
    return sorted(d.isoformat() for d in rows)


@router.get("/reflections/{reflection_id}", response_model=ReflectionOut)
def get_reflection(reflection_id: int, user: User = Depends(current_user), db: Session = Depends(get_db)):
    return _reflection_out(_owned(db, user, reflection_id))


@router.put("/reflections/{reflection_id}", response_model=ReflectionOut)
def update_reflection(reflection_id: int, body: ReflectionUpdateInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    row = _owned(db, user, reflection_id)
    row.reflection_answer = _clean_answer(body.answer)
    row.updated_at = datetime.now(timezone.utc)
    db.commit(); db.refresh(row)
    return _reflection_out(row)


@router.delete("/reflections/{reflection_id}", status_code=204)
def delete_reflection(reflection_id: int, user: User = Depends(current_user), db: Session = Depends(get_db)):
    row = _owned(db, user, reflection_id)
    db.delete(row); db.commit()
    return Response(status_code=204)
