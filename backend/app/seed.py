"""Idempotent catalog seeding.

The verse catalog lives in ``app/content/*.py`` (authentic scripture text, per-verse topics and
mood tags, simple explanations, stories and challenges). ``seed_database`` is safe to run on every
start: the heavy verse/story/challenge sync only runs when ``CONTENT_VERSION`` changes.
"""
from sqlalchemy import delete, select, update
from sqlalchemy.orm import Session

from app.content import bible, challenges as challenge_content, dhammapada, gita, mahabharata, quran, ramayana, stories, wisdom
from app.models.entities import (
    AppMeta, Challenge, ChallengeDay, Favorite, Language, MoodCheckin, Religion, ReligionLanguage, Scripture, Story,
    StoryScene, StorySceneTranslation, StoryTranslation, Topic, Verse, VerseMood, VerseTopic,
)

CONTENT_VERSION = "2026.11.03-1"

RELIGIONS = [
    ("Hinduism", "hinduism"),
    ("Christianity", "christianity"),
    ("Islam", "islam"),
    ("Buddhism", "buddhism"),
    ("General Wisdom", "general wisdom"),
]

LANGUAGES = [
    ("English", "en", "English"),
    ("Hindi", "hi", "हिन्दी"),
    ("Bengali", "bn", "বাংলা"),
    ("Spanish", "es", "Español"),
    ("Arabic", "ar", "عربي"),
    ("Urdu", "ur", "اردو"),
    ("Thai", "th", "ภาษาไทย"),
]

# religion code -> language codes in display order (first 3 shown in the UI)
RELIGION_LANGUAGES = {
    "hinduism":       ["hi", "en", "bn"],
    "christianity":   ["en", "hi", "es"],
    "islam":          ["ar", "en", "ur", "hi"],
    "buddhism":       ["en", "hi", "th"],
    "general wisdom": ["en", "hi", "bn"],
}

# scripture code -> (display name, religion code, content module)
SCRIPTURES = {
    "gita":        ("Bhagavad Gita", "hinduism", gita),
    "bible":       ("Bible", "christianity", bible),
    "quran":       ("Quran", "islam", quran),
    "dhammapada":  ("Dhammapada", "buddhism", dhammapada),
    "wisdom":      ("Inspirational Verses", "general wisdom", wisdom),
    "ramayana":    ("Ramayana", "hinduism", ramayana),
    "mahabharata": ("Mahabharata", "hinduism", mahabharata),
}

SOURCE_BY_SCRIPTURE = {
    "gita": "Bhagavad Gita (Mahabharata, Bhishma Parva) — Sanskrit original",
    "ramayana": "Valmiki Ramayana — Sanskrit original",
    "mahabharata": "Mahabharata — Sanskrit original",
    "dhammapada": "Dhammapada (Khuddaka Nikaya) — Pali original",
    "wisdom": "Classical wisdom texts — Vedas, Upanishads, Tao Te Ching, Analects, Tirukkural",
    "quran": "Quran — Arabic original",
    "bible": "Bible",
}
# (scripture, language) -> (translation label, licence)
TRANSLATION = {
    ("bible", "en"): ("King James Version", "Public domain"),
    ("bible", "hi"): ("Hindi Bible (Indian Old Version tradition)", "Public domain"),
    ("bible", "es"): ("Reina-Valera (1909 tradition)", "Public domain"),
    ("quran", "ar"): ("Arabic original", "Public domain"),
}
DEFAULT_TRANSLATION = ("WisdomOne translation of the original", "Public-domain original · WisdomOne translation")


def _sync_catalog(db: Session) -> tuple[dict[str, Religion], dict[str, Language], dict[str, Scripture]]:
    for name, code in RELIGIONS:
        if not db.scalar(select(Religion).where(Religion.code == code)):
            db.add(Religion(name=name, code=code))
    for name, code, native in LANGUAGES:
        row = db.scalar(select(Language).where(Language.code == code))
        if row is None:
            db.add(Language(name=name, code=code, native_script=native))
        elif not row.native_script:
            row.native_script = native
    db.flush()
    religions = {r.code: r for r in db.scalars(select(Religion))}
    languages = {l.code: l for l in db.scalars(select(Language))}
    for rcode, lcodes in RELIGION_LANGUAGES.items():
        for order, lcode in enumerate(lcodes):
            exists = db.scalar(select(ReligionLanguage).where(
                ReligionLanguage.religion_id == religions[rcode].id, ReligionLanguage.language_id == languages[lcode].id))
            if exists is None:
                db.add(ReligionLanguage(religion_id=religions[rcode].id, language_id=languages[lcode].id, sort_order=order))
    scriptures: dict[str, Scripture] = {}
    for code, (name, rcode, _mod) in SCRIPTURES.items():
        row = db.scalar(select(Scripture).where(Scripture.code == code))
        if row is None:
            row = Scripture(name=name, code=code, religion_id=religions[rcode].id)
            db.add(row)
            db.flush()
        scriptures[code] = row
    db.flush()
    return religions, languages, scriptures


def _sync_topics(db: Session) -> dict[str, Topic]:
    existing = {t.code: t for t in db.scalars(select(Topic))}
    for order, (code, name, icon, desc) in enumerate(challenge_content.TOPICS):
        row = existing.get(code)
        if row is None:
            row = Topic(code=code)
            db.add(row)
            existing[code] = row
        row.name, row.icon, row.description, row.sort_order = name, icon, desc, order
    db.flush()
    return existing


def _sync_verses(db: Session, languages: dict[str, Language], scriptures: dict[str, Scripture], topics: dict[str, Topic]) -> None:
    existing: dict[tuple, Verse] = {}
    legacy: dict[tuple, Verse] = {}  # demo rows without a book, matched by chapter/verse so IDs (and favourites) survive
    for v in db.scalars(select(Verse)):
        existing[(v.scripture_id, v.language_id, v.book, v.chapter, v.verse_number)] = v
        if v.book is None:
            legacy[(v.scripture_id, v.language_id, v.chapter, v.verse_number)] = v

    kept_ids: set[int] = set()
    tags: list[tuple[Verse, str, str]] = []
    for scode, (_name, _rcode, mod) in SCRIPTURES.items():
        scripture = scriptures[scode]
        for book, chapter, number, topic_str, mood_str, by_lang in mod.VERSES:
            for lcode, (text, simple) in by_lang.items():
                lang = languages[lcode]
                label, licence = TRANSLATION.get((scode, lcode), DEFAULT_TRANSLATION)
                sep = ":" if scode in ("bible", "quran") else "."
                source = f"{SOURCE_BY_SCRIPTURE[scode]} · {book} {chapter}{sep}{number}"
                key = (scripture.id, lang.id, book, chapter, number)
                row = existing.get(key) or legacy.get((scripture.id, lang.id, chapter, number))
                if row is not None and row.id in kept_ids:
                    row = None
                if row is None:
                    row = Verse(scripture_id=scripture.id, language_id=lang.id)
                    db.add(row)
                row.book, row.chapter, row.verse_number = book, chapter, number
                row.text, row.translation, row.source, row.license = text, label, source, licence
                row.simple_explanation = simple
                row.is_demo, row.is_active = False, True
                db.flush()
                kept_ids.add(row.id)
                tags.append((row, topic_str, mood_str))

    # Retire the old illustrative demo paraphrases that have no authentic replacement.
    stale = sorted({v.id for v in list(existing.values()) if v.is_demo and v.id not in kept_ids})
    if stale:
        # SQLite does not enforce ON DELETE CASCADE unless PRAGMA foreign_keys is on, so clean up explicitly.
        db.execute(delete(Favorite).where(Favorite.verse_id.in_(stale)))
        db.execute(delete(VerseTopic).where(VerseTopic.verse_id.in_(stale)))
        db.execute(delete(VerseMood).where(VerseMood.verse_id.in_(stale)))
        db.execute(update(MoodCheckin).where(MoodCheckin.verse_id.in_(stale)).values(verse_id=None))
        db.execute(delete(Verse).where(Verse.id.in_(stale)))
        db.expire_all()

    ids = sorted(kept_ids)
    for i in range(0, len(ids), 500):
        chunk = ids[i:i + 500]
        db.execute(delete(VerseTopic).where(VerseTopic.verse_id.in_(chunk)))
        db.execute(delete(VerseMood).where(VerseMood.verse_id.in_(chunk)))
    for row, topic_str, mood_str in tags:
        for code in dict.fromkeys(t.strip() for t in topic_str.split(",") if t.strip()):
            db.add(VerseTopic(verse_id=row.id, topic_id=topics[code].id))
        for mood in dict.fromkeys(m.strip() for m in mood_str.split(",") if m.strip()):
            db.add(VerseMood(verse_id=row.id, mood=mood))
    db.flush()


def _sync_challenges(db: Session, topics: dict[str, Topic]) -> None:
    existing = {c.code: c for c in db.scalars(select(Challenge))}
    for order, spec in enumerate(challenge_content.CHALLENGES):
        assert len(spec["days"]) == spec["total_days"], spec["code"]
        row = existing.get(spec["code"])
        if row is None:
            row = Challenge(code=spec["code"], title=spec["title"], total_days=spec["total_days"])
            db.add(row)
            db.flush()
        for field in ("title", "subtitle", "description", "total_days", "icon", "color", "badge_name", "badge_icon"):
            setattr(row, field, spec[field])
        row.sort_order, row.is_active = order, True
        days = {d.day_number: d for d in db.scalars(select(ChallengeDay).where(ChallengeDay.challenge_id == row.id))}
        for number, (title, reflection, action, topic_code) in enumerate(spec["days"], start=1):
            day = days.get(number)
            if day is None:
                day = ChallengeDay(challenge_id=row.id, day_number=number)
                db.add(day)
            day.title, day.reflection, day.action, day.topic_id = title, reflection, action, topics[topic_code].id
        for number, day in days.items():
            if number > spec["total_days"]:
                db.delete(day)
    db.flush()


def _sync_stories(db: Session, languages: dict[str, Language], scriptures: dict[str, Scripture]) -> None:
    for order, spec in enumerate(stories.STORIES):
        scode, book, chapter, number = spec["ref"]
        row = db.scalar(select(Story).where(Story.code == spec["code"]))
        if row is None:
            row = Story(code=spec["code"], scripture_id=scriptures[scode].id, chapter=chapter, verse_number=number)
            db.add(row)
            db.flush()
        row.emoji, row.palette, row.moral_values, row.sort_order = spec["emoji"], spec["palette"], spec["values"], order
        row.scripture_id, row.book, row.chapter, row.verse_number = scriptures[scode].id, book, chapter, number
        db.flush()
        # Replace children wholesale (nothing else references them).
        scene_ids = [s.id for s in db.scalars(select(StoryScene).where(StoryScene.story_id == row.id))]
        if scene_ids:
            db.execute(delete(StorySceneTranslation).where(StorySceneTranslation.scene_id.in_(scene_ids)))
        db.execute(delete(StoryScene).where(StoryScene.story_id == row.id))
        db.execute(delete(StoryTranslation).where(StoryTranslation.story_id == row.id))
        db.expire_all()
        scenes = []
        for n, art in enumerate(spec["scenes"], start=1):
            scene = StoryScene(story_id=row.id, scene_number=n, art=art)
            db.add(scene)
            scenes.append(scene)
        db.flush()
        for lcode, (title, lesson, texts) in spec["tr"].items():
            assert len(texts) == len(scenes), (spec["code"], lcode)
            lang = languages[lcode]
            db.add(StoryTranslation(story_id=row.id, language_id=lang.id, title=title, summary=texts[0], lesson=lesson))
            for scene, text in zip(scenes, texts):
                db.add(StorySceneTranslation(scene_id=scene.id, language_id=lang.id, text=text))
    db.flush()


def seed_database(db: Session) -> None:
    religions, languages, scriptures = _sync_catalog(db)
    topics = _sync_topics(db)
    meta = db.get(AppMeta, "content_version")
    if meta is not None and meta.value == CONTENT_VERSION:
        db.commit()
        return
    _sync_verses(db, languages, scriptures, topics)
    _sync_challenges(db, topics)
    _sync_stories(db, languages, scriptures)
    if meta is None:
        db.add(AppMeta(key="content_version", value=CONTENT_VERSION))
    else:
        meta.value = CONTENT_VERSION
    db.commit()
