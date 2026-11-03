"""Feature endpoints: mood-based verses, Universal Truths, challenges + badges, stories, explanations."""
import random
from datetime import date, datetime, timedelta, timezone

import jwt
from fastapi import APIRouter, Depends, HTTPException, Query
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.api.v1.routes import current_user, verse_out, verse_query
from app.core.config import settings
from app.core.database import get_db
from app.models.entities import (
    Challenge, ChallengeDay, ChallengeProgress, Language, MOODS, MoodCheckin, Religion, Scripture, Story,
    StorySceneTranslation, StoryTranslation, Topic, User, UserChallenge, UserPreference, Verse, VerseMood, VerseTopic,
)
from app.schemas.api import (
    BadgeOut, ChallengeDayOut, ChallengeDetailOut, ChallengeOut, ChallengeProgressOut, CompleteDayInput, CompleteDayOut,
    ExplanationOut, MoodCheckinInput, MoodCheckinOut, MoodOut, MoodSummaryOut, MoodVerseOut, StoryDetailOut, StoryOut,
    StorySceneOut, TopicOut, TruthCard, UniversalTruthOut,
)

router = APIRouter()
optional_bearer = HTTPBearer(auto_error=False)

MOOD_META = {
    "anxious": ("Anxious", "😟", "Take a slow breath. Here is a verse to steady your heart."),
    "happy": ("Happy", "😊", "Wonderful! Here is a verse to carry your joy further."),
    "sad": ("Sad", "😔", "It is okay to feel low. Here is a verse to hold you gently."),
    "angry": ("Angry", "😠", "Pause for a moment. Here is a verse to cool the fire."),
}


# ── helpers ──────────────────────────────────────────────────────────────────
def optional_user(credentials: HTTPAuthorizationCredentials | None = Depends(optional_bearer), db: Session = Depends(get_db)) -> User | None:
    if credentials is None:
        return None
    try:
        payload = jwt.decode(credentials.credentials, settings.jwt_secret, algorithms=[settings.jwt_algorithm])
        user = db.get(User, payload["sub"])
    except (jwt.PyJWTError, KeyError):
        raise HTTPException(status_code=401, detail="Invalid or expired access token")
    if user is None:
        raise HTTPException(status_code=401, detail="User no longer exists")
    return user


def _prefs(user: User | None, db: Session) -> tuple[int | None, int | None, list[int]]:
    """(religion_id, language_id, scripture_ids) from the signed-in user's saved preferences."""
    if user is None:
        return None, None, []
    p = db.get(UserPreference, user.id)
    if p is None:
        return None, None, []
    return p.religion_id, p.language_id, [int(x) for x in p.scripture_ids.split(",") if x.strip().isdigit()]


def _english_id(db: Session) -> int | None:
    return db.scalar(select(Language.id).where(Language.code == "en"))


def _pick(items: list, key: str, seed: int | None):
    """Deterministic pick that rotates daily; an explicit seed (shuffle) gives a different pick."""
    if not items:
        return None
    base = date.today().toordinal() + sum(ord(c) for c in key)
    return items[(base + (seed or 0)) % len(items)]


def _language_or_404(db: Session, language_id: int | None) -> Language | None:
    if language_id is None:
        return None
    lang = db.get(Language, language_id)
    if lang is None:
        raise HTTPException(status_code=404, detail="Language not found")
    return lang


# ── Moods ────────────────────────────────────────────────────────────────────
@router.get("/moods", response_model=list[MoodOut])
def list_moods():
    return [MoodOut(code=c, label=m[0], emoji=m[1], message=m[2]) for c, m in MOOD_META.items()]


@router.get("/mood/verse", response_model=MoodVerseOut)
def verse_for_mood(
    mood: str = Query(min_length=2, max_length=20),
    religion_id: int | None = None, scripture_id: int | None = None, language_id: int | None = None,
    shuffle: int = Query(default=0, ge=0, le=10_000),
    user: User | None = Depends(optional_user), db: Session = Depends(get_db),
):
    """A verse matching how the user feels. Explicit query params win over saved preferences."""
    mood = mood.lower()
    if mood not in MOODS:
        raise HTTPException(status_code=422, detail=f"mood must be one of: {', '.join(MOODS)}")
    p_religion, p_language, p_scriptures = _prefs(user, db)
    religion_id = religion_id if religion_id is not None else p_religion
    language_id = language_id if language_id is not None else p_language
    query = (verse_query(db).join(VerseMood, VerseMood.verse_id == Verse.id).where(VerseMood.mood == mood))
    if scripture_id is not None:
        query = query.where(Verse.scripture_id == scripture_id)
    else:
        if religion_id is not None:
            query = query.where(Scripture.religion_id == religion_id)
        if p_scriptures and religion_id == p_religion:
            query = query.where(Verse.scripture_id.in_(p_scriptures))
    if language_id is not None:
        query = query.where(Verse.language_id == language_id)
    verses = list(db.scalars(query.order_by(Verse.id)).all())
    if not verses:
        raise HTTPException(status_code=404, detail="No verses match this mood for your preferences")
    # shuffle=0 -> the same verse all day; shuffle>0 -> a different verse each tap
    chosen = _pick(verses, mood, shuffle) if shuffle == 0 else verses[(date.today().toordinal() + shuffle * 7 + len(mood)) % len(verses)]
    return MoodVerseOut(mood=mood, message=MOOD_META[mood][2], verse=verse_out(chosen))


@router.post("/mood/checkins", response_model=MoodCheckinOut, status_code=201)
def record_mood(data: MoodCheckinInput, user: User = Depends(current_user), db: Session = Depends(get_db)):
    mood = data.mood.lower()
    if mood not in MOODS:
        raise HTTPException(status_code=422, detail=f"mood must be one of: {', '.join(MOODS)}")
    if data.verse_id is not None and db.get(Verse, data.verse_id) is None:
        raise HTTPException(status_code=404, detail="Verse not found")
    row = MoodCheckin(user_id=user.id, mood=mood, verse_id=data.verse_id)
    db.add(row); db.commit(); db.refresh(row)
    return MoodCheckinOut(id=row.id, mood=row.mood, verse_id=row.verse_id, created_at=row.created_at.isoformat())


@router.get("/mood/summary", response_model=MoodSummaryOut)
def mood_summary(days: int = Query(default=30, ge=1, le=365), user: User = Depends(current_user), db: Session = Depends(get_db)):
    since = datetime.now(timezone.utc) - timedelta(days=days)
    rows = db.scalars(select(MoodCheckin).where(MoodCheckin.user_id == user.id, MoodCheckin.created_at >= since.replace(tzinfo=None))
                      .order_by(MoodCheckin.created_at.desc(), MoodCheckin.id.desc())).all()
    counts = {m: 0 for m in MOODS}
    for r in rows:
        counts[r.mood] = counts.get(r.mood, 0) + 1
    top = max(counts, key=counts.get) if rows else None
    return MoodSummaryOut(days=days, total=len(rows), counts=counts, top_mood=top,
                          recent=[MoodCheckinOut(id=r.id, mood=r.mood, verse_id=r.verse_id, created_at=r.created_at.isoformat()) for r in rows[:10]])


# ── Explain like I'm 5 ───────────────────────────────────────────────────────
@router.get("/verses/{verse_id}/explain", response_model=ExplanationOut)
def explain_verse(verse_id: int, db: Session = Depends(get_db)):
    v = db.scalar(verse_query(db).where(Verse.id == verse_id))
    if v is None:
        raise HTTPException(status_code=404, detail="Verse not found")
    if not v.simple_explanation:
        raise HTTPException(status_code=404, detail="No simple explanation is available for this verse yet")
    return ExplanationOut(verse_id=v.id, level="simple", language=v.language.code, text=v.simple_explanation)


# ── Universal Truths ─────────────────────────────────────────────────────────
def _topic_stats(db: Session, topic: Topic) -> TopicOut:
    rows = db.execute(
        select(Scripture.religion_id, func.count(Verse.id.distinct()))
        .select_from(VerseTopic).join(Verse, Verse.id == VerseTopic.verse_id).join(Scripture, Scripture.id == Verse.scripture_id)
        .where(VerseTopic.topic_id == topic.id, Verse.is_active.is_(True)).group_by(Scripture.religion_id)).all()
    return TopicOut(code=topic.code, name=topic.name, icon=topic.icon, description=topic.description,
                    religion_count=len(rows), verse_count=sum(r[1] for r in rows))


@router.get("/universal-truths/topics", response_model=list[TopicOut])
def truth_topics(db: Session = Depends(get_db)):
    topics = db.scalars(select(Topic).order_by(Topic.sort_order)).all()
    out = [_topic_stats(db, t) for t in topics]
    return [t for t in out if t.religion_count >= 2]


@router.get("/universal-truths/{topic_code}", response_model=UniversalTruthOut)
def universal_truth(
    topic_code: str, language_id: int | None = None, seed: int = Query(default=0, ge=0, le=10_000),
    db: Session = Depends(get_db),
):
    """One verse per tradition about the same topic, ready to show side by side."""
    topic = db.scalar(select(Topic).where(Topic.code == topic_code.lower()))
    if topic is None:
        raise HTTPException(status_code=404, detail="Topic not found")
    lang = _language_or_404(db, language_id) or db.get(Language, _english_id(db))
    english_id = _english_id(db)
    cards: list[TruthCard] = []
    for religion in db.scalars(select(Religion).order_by(Religion.id)).all():
        def verses_in(language: int) -> list[Verse]:
            q = (verse_query(db).join(VerseTopic, VerseTopic.verse_id == Verse.id)
                 .where(VerseTopic.topic_id == topic.id, Scripture.religion_id == religion.id, Verse.language_id == language))
            return list(db.scalars(q.order_by(Verse.id)).all())
        verses, fallback = verses_in(lang.id), False
        if not verses and english_id and english_id != lang.id:
            verses, fallback = verses_in(english_id), True
        chosen = _pick(verses, f"{topic.code}{religion.code}", seed)
        if chosen is not None:
            cards.append(TruthCard(religion_id=religion.id, religion=religion.name, religion_code=religion.code,
                                   language_fallback=fallback, verse=verse_out(chosen)))
    if len(cards) < 2:
        raise HTTPException(status_code=404, detail="Not enough traditions have verses on this topic yet")
    return UniversalTruthOut(topic=_topic_stats(db, topic), language=lang.code, cards=cards)


# ── Challenges & badges ──────────────────────────────────────────────────────
def _effective_today(client_date: str | None) -> date:
    """The user's local date if the client sends it (clamped to ±1 day of UTC), else the UTC date."""
    utc_today = datetime.now(timezone.utc).date()
    if client_date:
        try:
            d = date.fromisoformat(client_date)
            if abs((d - utc_today).days) <= 1:
                return d
        except ValueError:
            raise HTTPException(status_code=422, detail="client_date must be YYYY-MM-DD")
    return utc_today


def _progress(db: Session, challenge: Challenge, user: User | None, today: date | None = None) -> ChallengeProgressOut:
    if user is None:
        return ChallengeProgressOut(joined=False, completed_days=[], next_day=1)
    enrol = db.get(UserChallenge, (user.id, challenge.id))
    days = sorted(db.scalars(select(ChallengeProgress.day_number).where(
        ChallengeProgress.user_id == user.id, ChallengeProgress.challenge_id == challenge.id)).all())
    done = len(days) >= challenge.total_days
    next_day = None if done else (max(days) + 1 if days else 1)
    last_on = db.scalar(select(func.max(ChallengeProgress.completed_on)).where(
        ChallengeProgress.user_id == user.id, ChallengeProgress.challenge_id == challenge.id))
    wait = bool(settings.challenge_one_per_day and not done and last_on is not None and last_on >= (today or _effective_today(None)))
    return ChallengeProgressOut(joined=enrol is not None, completed_days=days, next_day=next_day, completed=done,
                                badge_earned=bool(enrol and enrol.completed_at), available_tomorrow=wait)


def _challenge_out(db: Session, c: Challenge, user: User | None, today: date | None = None) -> dict:
    return dict(id=c.id, code=c.code, title=c.title, subtitle=c.subtitle, description=c.description, total_days=c.total_days,
                icon=c.icon, color=c.color, badge_name=c.badge_name, badge_icon=c.badge_icon, progress=_progress(db, c, user, today))


def _get_challenge(db: Session, ident: str) -> Challenge:
    q = select(Challenge).where(Challenge.is_active.is_(True))
    q = q.where(Challenge.id == int(ident)) if ident.isdigit() else q.where(Challenge.code == ident)
    c = db.scalar(q)
    if c is None:
        raise HTTPException(status_code=404, detail="Challenge not found")
    return c


@router.get("/challenges", response_model=list[ChallengeOut])
def list_challenges(client_date: str | None = Query(default=None), user: User | None = Depends(optional_user), db: Session = Depends(get_db)):
    today = _effective_today(client_date)
    rows = db.scalars(select(Challenge).where(Challenge.is_active.is_(True)).order_by(Challenge.sort_order)).all()
    return [_challenge_out(db, c, user, today) for c in rows]


def _day_verse(db: Session, challenge: Challenge, day: ChallengeDay, language_id: int | None, religion_id: int | None) -> Verse | None:
    """A verse on the day's topic from the user's tradition + language; widens gradually if nothing matches."""
    english_id = _english_id(db)
    attempts = [(religion_id, language_id), (None, language_id), (None, english_id)]
    for rel, lang in attempts:
        q = verse_query(db).join(VerseTopic, VerseTopic.verse_id == Verse.id).where(VerseTopic.topic_id == day.topic_id)
        if rel is not None:
            q = q.where(Scripture.religion_id == rel)
        if lang is not None:
            q = q.where(Verse.language_id == lang)
        verses = list(db.scalars(q.order_by(Verse.id)).all())
        if verses:
            return verses[(challenge.id * 7 + day.day_number * 3) % len(verses)]
    return None


@router.get("/challenges/{ident}", response_model=ChallengeDetailOut)
def challenge_detail(ident: str, language_id: int | None = None, religion_id: int | None = None, client_date: str | None = Query(default=None),
                     user: User | None = Depends(optional_user), db: Session = Depends(get_db)):
    c = _get_challenge(db, ident)
    today = _effective_today(client_date)
    p_religion, p_language, _ = _prefs(user, db)
    language_id = language_id if language_id is not None else p_language
    religion_id = religion_id if religion_id is not None else p_religion
    progress = _progress(db, c, user, today)
    done = set(progress.completed_days)
    days = []
    for d in c.days:
        status = "completed" if d.day_number in done else ("available" if d.day_number == (progress.next_day or 0) else "locked")
        verse = _day_verse(db, c, d, language_id, religion_id) if status != "locked" or d.day_number <= 1 else None
        days.append(ChallengeDayOut(day_number=d.day_number, title=d.title, reflection=d.reflection, action=d.action,
                                    topic=d.topic.code, status=status, verse=verse_out(verse) if verse else None))
    return ChallengeDetailOut(**_challenge_out(db, c, user, today), days=days)


@router.post("/challenges/{ident}/join", response_model=ChallengeProgressOut, status_code=201)
def join_challenge(ident: str, user: User = Depends(current_user), db: Session = Depends(get_db)):
    c = _get_challenge(db, ident)
    if db.get(UserChallenge, (user.id, c.id)) is None:
        db.add(UserChallenge(user_id=user.id, challenge_id=c.id)); db.commit()
    return _progress(db, c, user)


@router.post("/challenges/{ident}/days/{day_number}/complete", response_model=CompleteDayOut)
def complete_day(ident: str, day_number: int, data: CompleteDayInput | None = None,
                 client_date: str | None = Query(default=None), user: User = Depends(current_user), db: Session = Depends(get_db)):
    c = _get_challenge(db, ident)
    if day_number < 1 or day_number > c.total_days:
        raise HTTPException(status_code=404, detail="That day does not exist in this challenge")
    today = _effective_today(client_date)
    existing = db.scalar(select(ChallengeProgress).where(ChallengeProgress.user_id == user.id, ChallengeProgress.challenge_id == c.id,
                                                         ChallengeProgress.day_number == day_number))
    enrol = db.get(UserChallenge, (user.id, c.id))
    if existing is not None:  # idempotent
        return CompleteDayOut(challenge_id=c.id, day_number=day_number, progress=_progress(db, c, user), badge_earned_now=False)
    if day_number > 1:
        prev = db.scalar(select(ChallengeProgress).where(ChallengeProgress.user_id == user.id, ChallengeProgress.challenge_id == c.id,
                                                         ChallengeProgress.day_number == day_number - 1))
        if prev is None:
            raise HTTPException(status_code=409, detail=f"Complete day {day_number - 1} first")
        if settings.challenge_one_per_day and prev.completed_on >= today:
            raise HTTPException(status_code=409, detail="Great work today! Come back tomorrow to unlock the next day")
    if enrol is None:
        enrol = UserChallenge(user_id=user.id, challenge_id=c.id)
        db.add(enrol)
    db.add(ChallengeProgress(user_id=user.id, challenge_id=c.id, day_number=day_number, completed_on=today,
                             note=(data.note if data else None)))
    db.flush()
    finished = db.scalar(select(func.count()).select_from(ChallengeProgress).where(
        ChallengeProgress.user_id == user.id, ChallengeProgress.challenge_id == c.id)) >= c.total_days
    badge_now = False
    if finished and enrol.completed_at is None:
        enrol.completed_at = datetime.now(timezone.utc)
        badge_now = True
    db.commit()
    return CompleteDayOut(challenge_id=c.id, day_number=day_number, progress=_progress(db, c, user), badge_earned_now=badge_now,
                          badge_name=c.badge_name if badge_now else None, badge_icon=c.badge_icon if badge_now else None)


@router.delete("/challenges/{ident}/progress", status_code=204)
def restart_challenge(ident: str, user: User = Depends(current_user), db: Session = Depends(get_db)):
    """Start over. Any badge that was already earned is kept."""
    c = _get_challenge(db, ident)
    for row in db.scalars(select(ChallengeProgress).where(ChallengeProgress.user_id == user.id, ChallengeProgress.challenge_id == c.id)).all():
        db.delete(row)
    db.commit()


@router.get("/users/badges", response_model=list[BadgeOut])
def my_badges(user: User = Depends(current_user), db: Session = Depends(get_db)):
    rows = db.scalars(select(UserChallenge).where(UserChallenge.user_id == user.id, UserChallenge.completed_at.is_not(None))
                      .order_by(UserChallenge.completed_at.desc())).all()
    return [BadgeOut(challenge_id=r.challenge_id, challenge_code=r.challenge.code, challenge_title=r.challenge.title,
                     badge_name=r.challenge.badge_name, badge_icon=r.challenge.badge_icon, color=r.challenge.color,
                     earned_at=r.completed_at.isoformat()) for r in rows]


# ── Stories ──────────────────────────────────────────────────────────────────
def _story_texts(db: Session, story: Story, language_id: int | None) -> tuple[StoryTranslation | None, bool, dict[int, str]]:
    english_id = _english_id(db)
    for lang, fallback in ((language_id, False), (english_id, True)):
        if lang is None:
            continue
        tr = db.scalar(select(StoryTranslation).where(StoryTranslation.story_id == story.id, StoryTranslation.language_id == lang))
        if tr is not None:
            scenes = {s.scene_number: db.scalar(select(StorySceneTranslation.text).where(
                StorySceneTranslation.scene_id == s.id, StorySceneTranslation.language_id == lang)) or "" for s in story.scenes}
            return tr, (fallback and lang != language_id), scenes
    return None, False, {}


def _story_verse(db: Session, story: Story, language_id: int | None) -> Verse | None:
    for lang in (language_id, _english_id(db)):
        if lang is None:
            continue
        v = db.scalar(verse_query(db).where(Verse.scripture_id == story.scripture_id, Verse.book == story.book,
                                            Verse.chapter == story.chapter, Verse.verse_number == story.verse_number, Verse.language_id == lang))
        if v is not None:
            return v
    return None


def _story_out(db: Session, story: Story, language_id: int | None) -> dict | None:
    tr, fallback, _scenes = _story_texts(db, story, language_id)
    if tr is None:
        return None
    lang = db.get(Language, tr.language_id)
    verse = _story_verse(db, story, language_id)
    return dict(id=story.id, code=story.code, emoji=story.emoji, palette=story.palette,
                values=[x for x in story.moral_values.split(",") if x], title=tr.title, lesson=tr.lesson, language=lang.code,
                language_fallback=fallback, scripture=story.scripture.name,
                reference=f"{story.book or ''} {story.chapter}:{story.verse_number}".strip(), verse_id=verse.id if verse else None,
                scene_count=len(story.scenes))


@router.get("/stories", response_model=list[StoryOut])
def list_stories(language_id: int | None = None, scripture_id: int | None = None, db: Session = Depends(get_db)):
    _language_or_404(db, language_id)
    q = select(Story).order_by(Story.sort_order)
    if scripture_id is not None:
        q = q.where(Story.scripture_id == scripture_id)
    out = [_story_out(db, s, language_id) for s in db.scalars(q).all()]
    return [o for o in out if o]


def _story_detail(db: Session, story: Story, language_id: int | None) -> StoryDetailOut:
    base = _story_out(db, story, language_id)
    if base is None:
        raise HTTPException(status_code=404, detail="This story is not available in that language yet")
    _tr, _fb, scenes = _story_texts(db, story, language_id)
    verse = _story_verse(db, story, language_id)
    return StoryDetailOut(**base, scenes=[StorySceneOut(scene_number=s.scene_number, art=s.art, text=scenes.get(s.scene_number, "")) for s in story.scenes],
                          verse=verse_out(verse) if verse else None)


@router.get("/stories/{code}", response_model=StoryDetailOut)
def story_detail(code: str, language_id: int | None = None, db: Session = Depends(get_db)):
    _language_or_404(db, language_id)
    story = db.scalar(select(Story).where(Story.code == code))
    if story is None:
        raise HTTPException(status_code=404, detail="Story not found")
    return _story_detail(db, story, language_id)


@router.get("/verses/{verse_id}/story", response_model=StoryDetailOut)
def story_for_verse(verse_id: int, language_id: int | None = None, db: Session = Depends(get_db)):
    """The story behind a verse, if one exists (matched by scripture + book + chapter + verse)."""
    v = db.get(Verse, verse_id)
    if v is None:
        raise HTTPException(status_code=404, detail="Verse not found")
    story = db.scalar(select(Story).where(Story.scripture_id == v.scripture_id, Story.book == v.book,
                                          Story.chapter == v.chapter, Story.verse_number == v.verse_number))
    if story is None:
        raise HTTPException(status_code=404, detail="There is no story for this verse yet")
    return _story_detail(db, story, language_id if language_id is not None else v.language_id)
