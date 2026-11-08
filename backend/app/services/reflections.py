"""Reflection-question seeding and helpers (Journal Reflection)."""
from collections import defaultdict

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.content.reflection_questions import GENERIC, OVERRIDES, POOLS
from app.models.entities import Language, ReflectionQuestion, Scripture, Topic, Verse

_FALLBACK_TOPIC = "wisdom"


def verse_reference(verse: Verse) -> str:
    """Human-readable reference used as the journal snapshot, e.g. 'Bhagavad Gita · Chapter 3 · Verse 19'."""
    name = verse.scripture.name
    if verse.book and verse.book != name:
        return f"{name} · {verse.book} {verse.chapter}:{verse.verse_number}"
    return f"{name} · Chapter {verse.chapter} · Verse {verse.verse_number}"


def generic_question(language_code: str) -> str:
    return GENERIC.get(language_code, GENERIC["en"])


def ensure_reflection_questions(db: Session) -> int:
    """Create a question for every active verse that has none. Existing rows (possibly edited by an editor) are never changed."""
    have = set(db.scalars(select(ReflectionQuestion.verse_id)).all())
    topic_order = {t.code: t.sort_order for t in db.scalars(select(Topic)).all()}
    langs = {l.id: l.code for l in db.scalars(select(Language)).all()}
    codes = {s.id: s.code for s in db.scalars(select(Scripture)).all()}
    verses = [v for v in db.scalars(select(Verse).where(Verse.is_active.is_(True)).order_by(Verse.id)).all() if v.id not in have]
    if not verses:
        return 0
    # Group translations of the same verse so every language of one verse gets the same question idea (same pool index).
    groups: dict[tuple, list[Verse]] = defaultdict(list)
    for v in verses:
        groups[(v.scripture_id, v.book, v.chapter, v.verse_number)].append(v)
    usage: dict[tuple[str, int], int] = defaultdict(int)
    created = 0
    for key in sorted(groups, key=lambda k: (k[0], k[1] or "", k[2], k[3])):
        rows = groups[key]
        topics = [t for t in sorted((t.code for t in rows[0].topics), key=lambda c: topic_order.get(c, 99)) if t in POOLS]
        pair = tuple(dict.fromkeys(topics[:2] or [_FALLBACK_TOPIC]))
        # Candidates = the verse's (up to two) topic pools, interleaved; choose the least-used one so questions spread evenly.
        size = len(POOLS[pair[0]]["en"])
        candidates = [(t, i) for i in range(size) for t in pair]
        topic, qi = min(candidates, key=lambda c: (usage[c], candidates.index(c)))
        usage[(topic, qi)] += 1
        for v in rows:
            lang = langs.get(v.language_id, "en")
            pool = POOLS[topic].get(lang) or POOLS[topic]["en"]
            text = pool[qi % len(pool)]
            override = OVERRIDES.get((codes.get(v.scripture_id, ""), v.chapter, v.verse_number))
            if override and lang == "en":
                text = override
            db.add(ReflectionQuestion(verse_id=v.id, scripture_id=v.scripture_id, language_id=v.language_id, reflection_question=text, is_active=True))
            created += 1
    db.commit()
    return created
