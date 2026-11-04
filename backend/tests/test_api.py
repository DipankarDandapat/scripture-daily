import os
import tempfile
import uuid
os.environ["SD_DATABASE_URL"] = f"sqlite:///{tempfile.gettempdir()}/scripture_test_{uuid.uuid4().hex}.db"
os.environ["SD_JWT_SECRET"] = "test-secret-with-at-least-32-characters"
import pytest
from fastapi.testclient import TestClient
from sqlalchemy import func, select
from app.core.config import settings
from app.core.database import SessionLocal
from app.main import app
from app.models.entities import Language, Scripture, Verse, VerseMood

MIN_PER_COMBO = 30


def register(client: TestClient, email: str) -> dict:
    token = client.post("/api/v1/auth/register", json={"email": email, "password": "correct-horse-8"}).json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def test_core_flow(client):
    assert client.get("/api/v1/health").json()["status"] == "ok"
    religions = client.get("/api/v1/religions").json()
    langs = client.get("/api/v1/languages").json()
    assert len(religions) == 5 and len(langs) == 7
    scriptures = client.get("/api/v1/scriptures", params={"religion_id": 1}).json()
    assert scriptures and scriptures[0]["name"] == "Bhagavad Gita"
    verse = client.get("/api/v1/verses/today", params={"scripture_id": 1, "language_id": 1}).json()
    assert verse["religion"] == "Hinduism" and verse["is_demo"] is False
    assert "topics" in verse and isinstance(verse["explanation_available"], bool)
    assert client.get("/api/v1/verses/search", params={"q": "patient"}).status_code == 200
    headers = register(client, "reader@example.com")
    assert client.get("/api/v1/users/me", headers=headers).json()["email"] == "reader@example.com"
    assert client.put("/api/v1/users/preferences", headers=headers, json={"religion_id": 1, "language_id": 2, "scripture_ids": "1", "frequency": "6h", "notifications_enabled": True}).status_code == 200
    preferred = client.get("/api/v1/verses/today", headers=headers).json()
    assert preferred["language"] == "hi" and preferred["scripture"] == "Bhagavad Gita"
    assert client.post("/api/v1/favorites", headers=headers, json={"verse_id": verse["id"]}).status_code == 201
    assert len(client.get("/api/v1/favorites", headers=headers).json()) == 1
    assert client.delete(f"/api/v1/favorites/{verse['id']}", headers=headers).status_code == 204
    assert client.get("/api/v1/favorites", headers=headers).json() == []
    assert client.get("/api/v1/favorites").status_code == 401


def test_invalid_preference_combination_is_rejected(client):
    headers = register(client, "reader2@example.com")
    response = client.put("/api/v1/users/preferences", headers=headers, json={"religion_id": 2, "language_id": 1, "scripture_ids": "1", "frequency": "daily", "notifications_enabled": False})
    assert response.status_code == 422


def test_every_scripture_language_combo_has_at_least_30_verses(client):
    with SessionLocal() as db:
        rows = db.execute(select(Scripture.code, Language.code, func.count(Verse.id)).select_from(Verse)
                          .join(Scripture, Scripture.id == Verse.scripture_id).join(Language, Language.id == Verse.language_id)
                          .where(Verse.is_active.is_(True)).group_by(Scripture.code, Language.code)).all()
        assert len(rows) == 22
        for scripture, language, count in rows:
            assert count >= MIN_PER_COMBO, (scripture, language, count)
        assert db.scalar(select(func.count(Verse.id)).where(Verse.is_demo.is_(True))) == 0
        assert db.scalar(select(func.count(Verse.id)).where(Verse.simple_explanation.is_(None))) == 0


def test_random_exclude(client):
    first = client.get("/api/v1/verses/random", params={"scripture_id": 1, "language_id": 1}).json()
    for _ in range(5):
        other = client.get("/api/v1/verses/random", params={"scripture_id": 1, "language_id": 1, "exclude_id": first["id"]}).json()
        assert other["id"] != first["id"]


# ── Mood ─────────────────────────────────────────────────────────────────────
def test_moods(client):
    assert {m["code"] for m in client.get("/api/v1/moods").json()} == {"anxious", "happy", "sad", "angry"}
    for mood in ("anxious", "happy", "sad", "angry"):
        body = client.get("/api/v1/mood/verse", params={"mood": mood, "religion_id": 1, "language_id": 1}).json()
        assert body["mood"] == mood and body["verse"]["language"] == "en" and body["message"]
    seen = {client.get("/api/v1/mood/verse", params={"mood": "sad", "language_id": 1, "religion_id": 3, "shuffle": n}).json()["verse"]["id"] for n in range(8)}
    assert len(seen) > 1  # shuffle produces different verses
    assert client.get("/api/v1/mood/verse", params={"mood": "bored"}).status_code == 422
    assert client.get("/api/v1/mood/verse", params={"mood": "happy", "language_id": 99, "religion_id": 1}).status_code == 404


def test_mood_uses_preferences_and_checkins(client):
    headers = register(client, "mood@example.com")
    client.put("/api/v1/users/preferences", headers=headers, json={"religion_id": 3, "language_id": 6, "scripture_ids": "3", "frequency": "daily", "notifications_enabled": False})
    verse = client.get("/api/v1/mood/verse", params={"mood": "anxious"}, headers=headers).json()["verse"]
    assert verse["scripture"] == "Quran" and verse["language"] == "ur"
    assert client.post("/api/v1/mood/checkins", json={"mood": "anxious"}).status_code == 401
    assert client.post("/api/v1/mood/checkins", headers=headers, json={"mood": "anxious", "verse_id": verse["id"]}).status_code == 201
    assert client.post("/api/v1/mood/checkins", headers=headers, json={"mood": "sad"}).status_code == 201
    assert client.post("/api/v1/mood/checkins", headers=headers, json={"mood": "nope"}).status_code == 422
    assert client.post("/api/v1/mood/checkins", headers=headers, json={"mood": "sad", "verse_id": 9999999}).status_code == 404
    summary = client.get("/api/v1/mood/summary", headers=headers).json()
    assert summary["total"] == 2 and summary["counts"]["anxious"] == 1 and len(summary["recent"]) == 2


def test_every_mood_available_for_every_combo(client):
    with SessionLocal() as db:
        rows = db.execute(select(Verse.scripture_id, Verse.language_id, VerseMood.mood, func.count()).join(VerseMood, VerseMood.verse_id == Verse.id)
                          .group_by(Verse.scripture_id, Verse.language_id, VerseMood.mood)).all()
        per_combo: dict = {}
        for sid, lid, mood, n in rows:
            per_combo.setdefault((sid, lid), {})[mood] = n
        assert len(per_combo) == 22
        for combo, moods in per_combo.items():
            assert set(moods) == {"anxious", "happy", "sad", "angry"}, combo
            assert min(moods.values()) >= 3, (combo, moods)


# ── Explain ──────────────────────────────────────────────────────────────────
def test_explain(client):
    verse = client.get("/api/v1/verses/random", params={"scripture_id": 1, "language_id": 3}).json()
    body = client.get(f"/api/v1/verses/{verse['id']}/explain")
    assert body.status_code == 200 and body.json()["language"] == "bn" and len(body.json()["text"]) > 5
    assert client.get("/api/v1/verses/9999999/explain").status_code == 404


# ── Universal Truths ─────────────────────────────────────────────────────────
def test_universal_truths(client):
    topics = client.get("/api/v1/universal-truths/topics").json()
    codes = {t["code"] for t in topics}
    assert {"love", "patience", "forgiveness", "gratitude", "peace"} <= codes
    assert all(t["religion_count"] >= 2 for t in topics)
    for code in ("love", "patience"):
        body = client.get(f"/api/v1/universal-truths/{code}", params={"language_id": 1}).json()
        religions = {c["religion_code"] for c in body["cards"]}
        assert {"hinduism", "christianity", "islam", "buddhism"} <= religions
        assert all(c["verse"]["language"] == "en" and not c["language_fallback"] for c in body["cards"])
    # Bengali has no Christian/Islamic/Buddhist verses: they fall back to English and say so.
    bn = client.get("/api/v1/universal-truths/love", params={"language_id": 3}).json()
    fallback = {c["religion_code"]: c["language_fallback"] for c in bn["cards"]}
    assert fallback["hinduism"] is False and fallback["christianity"] is True
    reshuffled = [c["verse"]["id"] for c in client.get("/api/v1/universal-truths/love", params={"language_id": 1, "seed": 1}).json()["cards"]]
    base = [c["verse"]["id"] for c in client.get("/api/v1/universal-truths/love", params={"language_id": 1}).json()["cards"]]
    assert reshuffled != base
    assert client.get("/api/v1/universal-truths/unknown").status_code == 404
    assert client.get("/api/v1/universal-truths/love", params={"language_id": 99}).status_code == 404


# ── Challenges ───────────────────────────────────────────────────────────────
def test_challenge_flow_and_badge(client, monkeypatch):
    monkeypatch.setattr(settings, "challenge_one_per_day", False)
    headers = register(client, "challenger@example.com")
    listing = client.get("/api/v1/challenges").json()
    assert {"gratitude-30", "anxiety-relief-7", "compassion-21", "calm-anger-10"} <= {c["code"] for c in listing} and len(listing) == 14
    seven = next(c for c in listing if c["code"] == "anxiety-relief-7")
    assert seven["total_days"] == 7 and seven["progress"]["joined"] is False
    detail = client.get(f"/api/v1/challenges/{seven['id']}", params={"language_id": 1, "religion_id": 2}, headers=headers).json()
    assert len(detail["days"]) == 7 and detail["days"][0]["status"] == "available" and detail["days"][1]["status"] == "locked"
    assert detail["days"][0]["verse"]["language"] == "en"
    assert client.post(f"/api/v1/challenges/{seven['id']}/join").status_code == 401
    assert client.post(f"/api/v1/challenges/{seven['id']}/join", headers=headers).status_code == 201
    assert client.post(f"/api/v1/challenges/{seven['id']}/days/3/complete", headers=headers).status_code == 409  # not unlocked
    assert client.post(f"/api/v1/challenges/{seven['id']}/days/9/complete", headers=headers).status_code == 404
    for day in range(1, 8):
        r = client.post(f"/api/v1/challenges/{seven['code']}/days/{day}/complete", headers=headers, json={"note": f"day {day}"})
        assert r.status_code == 200, r.text
        assert r.json()["badge_earned_now"] is (day == 7)
    final = r.json()
    assert final["badge_name"] == "Calm Heart" and final["progress"]["completed"] and final["progress"]["badge_earned"]
    again = client.post(f"/api/v1/challenges/{seven['id']}/days/7/complete", headers=headers).json()
    assert again["badge_earned_now"] is False  # idempotent
    badges = client.get("/api/v1/users/badges", headers=headers).json()
    assert [b["badge_name"] for b in badges] == ["Calm Heart"]
    assert client.delete(f"/api/v1/challenges/{seven['id']}/progress", headers=headers).status_code == 204
    after = client.get(f"/api/v1/challenges/{seven['id']}", headers=headers).json()["progress"]
    assert after["completed_days"] == [] and after["badge_earned"] is True  # restart keeps the earned badge


def test_challenge_one_day_per_calendar_day(client):
    headers = register(client, "slow@example.com")
    ten = next(c for c in client.get("/api/v1/challenges").json() if c["code"] == "calm-anger-10")
    assert settings.challenge_one_per_day is True
    assert client.post(f"/api/v1/challenges/{ten['id']}/days/1/complete", headers=headers).status_code == 200
    assert client.get(f"/api/v1/challenges/{ten['id']}", headers=headers).json()["progress"]["available_tomorrow"] is True
    blocked = client.post(f"/api/v1/challenges/{ten['id']}/days/2/complete", headers=headers)
    assert blocked.status_code == 409 and "tomorrow" in blocked.json()["detail"].lower()
    assert client.post(f"/api/v1/challenges/{ten['id']}/days/2/complete", params={"client_date": "nonsense"}, headers=headers).status_code == 422


def test_challenge_lengths(client):
    by_code = {c["code"]: c["total_days"] for c in client.get("/api/v1/challenges").json()}
    assert {k: by_code[k] for k in ("gratitude-30", "anxiety-relief-7", "compassion-21", "calm-anger-10")} == {"gratitude-30": 30, "anxiety-relief-7": 7, "compassion-21": 21, "calm-anger-10": 10}
    assert {k: v for k, v in by_code.items() if k not in ("gratitude-30", "anxiety-relief-7", "compassion-21", "calm-anger-10")} == {
        "inner-peace-7": 7, "gratitude-21": 21, "patience-7": 7, "courage-14": 14, "forgiveness-7": 7, "self-discipline-21": 21,
        "kindness-7": 7, "hope-14": 14, "mindfulness-7": 7, "everyday-wisdom-21": 21}
    detail = client.get("/api/v1/challenges/gratitude-30").json()
    assert [d["day_number"] for d in detail["days"]] == list(range(1, 31))


# ── Stories ──────────────────────────────────────────────────────────────────
def test_stories(client):
    stories = client.get("/api/v1/stories", params={"language_id": 2}).json()
    assert len(stories) == 50 and all(s["language"] == "hi" and not s["language_fallback"] for s in stories)
    detail = client.get("/api/v1/stories/arjuna-and-the-birds-eye", params={"language_id": 6}).json()
    assert detail["language"] == "ur" and len(detail["scenes"]) == 3 and all(s["text"] and s["art"] for s in detail["scenes"])
    assert detail["lesson"] and detail["verse"] is not None
    matches = 0
    for v in client.get("/api/v1/scriptures/1/chapters/2/verses", params={"language_id": 1}).json():
        r = client.get(f"/api/v1/verses/{v['id']}/story")
        if r.status_code == 200:
            matches += 1
            assert r.json()["code"] == "arjuna-and-the-birds-eye" and r.json()["language"] == "en"
    assert matches == 1
    assert client.get("/api/v1/stories/missing").status_code == 404
    assert client.get("/api/v1/stories", params={"language_id": 99}).status_code == 404


MIN_STORIES = 10


def test_stories_never_mix_traditions_and_each_has_at_least_ten(client):
    """Fixes 'Arjuna and the Bird's Eye appears for an Islam user': listings are scoped to one tradition."""
    religions = client.get("/api/v1/religions").json()
    all_codes = {s["code"]: s["religion_code"] for s in client.get("/api/v1/stories").json()}
    for religion in religions:
        langs = client.get("/api/v1/languages", params={"religion_id": religion["id"]}).json()
        expected = {c for c, r in all_codes.items() if r == religion["code"]}
        assert len(expected) >= MIN_STORIES, (religion["code"], len(expected))
        for lang in langs:
            listed = client.get("/api/v1/stories", params={"religion_id": religion["id"], "language_id": lang["id"]}).json()
            assert {s["code"] for s in listed} == expected, (religion["code"], lang["code"])
            assert all(s["religion_code"] == religion["code"] for s in listed)
            for s in listed:
                assert s["title"] and s["lesson"] and s["summary"] and s["scene_count"] >= 3
    islam = client.get("/api/v1/stories", params={"religion_id": 3, "language_id": 1}).json()
    assert "arjuna-and-the-birds-eye" not in {s["code"] for s in islam}
    # a signed-in user is scoped by their saved tradition even without sending religion_id
    headers = register(client, "story-scope@example.com")
    client.put("/api/v1/users/preferences", headers=headers, json={"religion_id": 3, "language_id": 1, "scripture_ids": "3", "frequency": "daily", "notifications_enabled": False})
    own = client.get("/api/v1/stories", params={"language_id": 1}, headers=headers).json()
    assert {s["code"] for s in own} == {c for c, r in all_codes.items() if r == "islam"}


def test_every_story_has_complete_text_in_english_and_hindi_and_graceful_fallback(client):
    for lang_id in (1, 2):
        for s in client.get("/api/v1/stories", params={"language_id": lang_id}).json():
            assert not s["language_fallback"], (s["code"], lang_id)
            d = client.get(f"/api/v1/stories/{s['code']}", params={"language_id": lang_id}).json()
            assert all(sc["text"].strip() and sc["art"] for sc in d["scenes"]), s["code"]
    # a language a story was not translated into falls back to English (never to another story or tradition)
    bn = client.get("/api/v1/stories", params={"language_id": 3, "religion_id": 1}).json()
    assert len(bn) == 10 and {s["language"] for s in bn} <= {"bn", "en"}


# ── Studio templates ──────────────────────────────────────────────────────────
def _rgb(h):
    h = h.lstrip("#"); return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _lum(c):
    def ch(v):
        s = v / 255; return s / 12.92 if s <= 0.03928 else ((s + 0.055) / 1.055) ** 2.4
    r, g, b = map(ch, c); return .2126 * r + .7152 * g + .0722 * b


def _contrast(a, b):
    la, lb = _lum(a), _lum(b); return (max(la, lb) + .05) / (min(la, lb) + .05)


def _mix(a, b, t): return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def test_studio_templates_are_tradition_aware(client):
    by_religion = {r["code"]: r["id"] for r in client.get("/api/v1/religions").json()}
    common = {t["code"] for t in client.get("/api/v1/studio/templates").json()}
    assert common and all(t["tradition"] is None for t in client.get("/api/v1/studio/templates").json())  # unknown tradition: common only
    seen: dict[str, set] = {}
    for code, rid in by_religion.items():
        rows = client.get("/api/v1/studio/templates", params={"religion_id": rid}).json()
        assert common <= {t["code"] for t in rows}, code
        own = {t["code"] for t in rows if t["tradition"] is not None}
        assert own and all(t["tradition"] == code for t in rows if t["tradition"] is not None), code
        seen[code] = own
    names = list(seen.values())
    assert all(a.isdisjoint(b) for i, a in enumerate(names) for b in names[i + 1:])  # never shared between traditions
    assert not any(t["motif"] in ("om", "cross", "crescent", "dharma") for t in client.get("/api/v1/studio/templates", params={"religion_id": by_religion["general wisdom"]}).json())
    video = client.get("/api/v1/studio/templates", params={"religion_id": 1, "kind": "video"}).json()
    assert video and client.get("/api/v1/studio/templates", params={"kind": "bogus"}).status_code == 422
    assert client.get("/api/v1/studio/templates", params={"religion_id": 999}).status_code == 404


def test_template_colours_are_readable(client):
    rows = client.get("/api/v1/studio/templates", params={"religion_id": 1}).json()
    rows += [t for r in (2, 3, 4, 5) for t in client.get("/api/v1/studio/templates", params={"religion_id": r}).json() if t["tradition"]]
    for t in rows:
        top, bottom = _rgb(t["color_top"]), _rgb(t["color_bottom"])
        assert min(_contrast(_rgb(t["color_text"]), _mix(top, bottom, x)) for x in (.2, .35, .5, .65, .75)) >= 4.5, t["code"]
        assert _contrast(_rgb(t["color_accent"]), _mix(top, bottom, .8)) >= 3.0, t["code"]
        assert _contrast(_rgb(t["color_muted"]), _mix(top, bottom, .84)) >= 3.0, t["code"]


def test_every_challenge_day_has_a_verse_in_the_users_own_tradition_and_language(client):
    """Tradition + language integrity for challenges: a relevant verse on every day, never from another tradition."""
    religions = {r["id"]: r["code"] for r in client.get("/api/v1/religions").json()}
    challenges = client.get("/api/v1/challenges").json()
    for rid, rcode in religions.items():
        for lang in client.get("/api/v1/languages", params={"religion_id": rid}).json():
            for ch in challenges:
                detail = client.get(f"/api/v1/challenges/{ch['id']}", params={"religion_id": rid, "language_id": lang["id"]}).json()
                for day in detail["days"]:
                    if day["day_number"] != 1:
                        continue  # later days are locked until reached; day verses are validated below through the DB check
                    assert day["verse"] is not None, (rcode, lang["code"], ch["code"])
                    if not ch["is_universal"]:
                        assert day["verse"]["religion"].lower().startswith(rcode.split()[0]), (rcode, ch["code"], day["verse"]["religion"])


def test_all_challenge_days_resolve_a_verse_for_every_combo(client):
    from app.api.v1.features import _day_verse
    from app.models.entities import Challenge, Religion
    with SessionLocal() as db:
        for religion in db.scalars(select(Religion)).all():
            from app.api.v1.routes import _religion_language_ids
            for lang_id in _religion_language_ids(db, religion.id):
                for challenge in db.scalars(select(Challenge)).all():
                    for day in challenge.days:
                        v = _day_verse(db, challenge, day, lang_id, religion.id)
                        assert v is not None, (religion.code, lang_id, challenge.code, day.day_number)
                        if not challenge.is_universal:
                            assert v.scripture.religion_id == religion.id and v.language_id == lang_id


# ── Catalogue size + integrity ───────────────────────────────────────────────────
def test_every_tradition_language_has_at_least_100_unique_valid_verses(client):
    from scripts.validate_catalog import validate
    problems, report = validate(100)
    assert problems == []
    assert len(report["matrix"]) == 16  # 16 tradition x language combinations
    assert all(m["count"] >= 100 for m in report["matrix"].values())
    assert report["stories"] == {"hinduism": 10, "christianity": 10, "islam": 10, "buddhism": 10, "general wisdom": 10}


def test_verses_never_leak_across_traditions_or_languages(client):
    """Every verse surface (today, random, history, mood, search by tradition) stays inside the selected tradition + language."""
    religions = client.get("/api/v1/religions").json()
    for r in religions:
        for lang in client.get("/api/v1/languages", params={"religion_id": r["id"]}).json():
            today = client.get("/api/v1/verses/today", params={"religion_id": r["id"], "language_id": lang["id"], "client_date": "2026-11-04"}).json()
            assert today["religion"].lower().startswith(r["code"].split()[0]) and today["language"] == lang["code"], (r["code"], lang["code"])
            seen = set()
            for _ in range(12):
                v = client.get("/api/v1/verses/random", params={"religion_id": r["id"], "language_id": lang["id"]}).json()
                assert v["religion"].lower().startswith(r["code"].split()[0]) and v["language"] == lang["code"]
                seen.add(v["id"])
            assert len(seen) > 3  # genuinely random, not a fixed pick
            history = client.get("/api/v1/verses/today/history", params={"religion_id": r["id"], "language_id": lang["id"], "count": 7}).json()
            assert len(history) == 7 and all(h["language"] == lang["code"] and h["religion"].lower().startswith(r["code"].split()[0]) for h in history)
            for mood in ("anxious", "happy", "sad", "angry"):
                m = client.get("/api/v1/mood/verse", params={"mood": mood, "religion_id": r["id"], "language_id": lang["id"]}).json()["verse"]
                assert m["language"] == lang["code"] and m["religion"].lower().startswith(r["code"].split()[0])
    # daily verse changes with the (client) calendar day, and is stable within a day
    a = client.get("/api/v1/verses/today", params={"religion_id": 3, "language_id": 1, "client_date": "2026-11-04"}).json()["id"]
    assert a == client.get("/api/v1/verses/today", params={"religion_id": 3, "language_id": 1, "client_date": "2026-11-04"}).json()["id"]
    assert a != client.get("/api/v1/verses/today", params={"religion_id": 3, "language_id": 1, "client_date": "2026-11-05"}).json()["id"]
    # invalid combinations are rejected rather than leaking
    assert client.get("/api/v1/verses/random", params={"religion_id": 3, "scripture_id": 1, "language_id": 1}).status_code == 422
    assert client.get("/api/v1/verses/today", params={"client_date": "nope"}).status_code == 422


def test_missing_translation_falls_back_inside_the_tradition(client):
    """Spanish is not offered for Islam: the request falls back to English/Arabic Islamic verses, never to another tradition."""
    spanish = next(l for l in client.get("/api/v1/languages").json() if l["code"] == "es")
    v = client.get("/api/v1/verses/today", params={"religion_id": 3, "language_id": spanish["id"]}).json()
    assert v["religion"] == "Islam" and v["language"] in ("en", "ar", "ur", "hi")
    r = client.get("/api/v1/verses/random", params={"religion_id": 3, "language_id": spanish["id"]}).json()
    assert r["religion"] == "Islam"


# ── Wisdom Tree ───────────────────────────────────────────────────────────────────
def test_wisdom_tree_growth_never_resets_and_is_idempotent(client):
    headers = register(client, "tree@example.com")
    assert client.get("/api/v1/tree", headers=headers, params={"client_date": "2026-11-04"}).json()["total_days"] == 0
    assert client.get("/api/v1/tree").status_code == 401
    t = client.post("/api/v1/tree/read", headers=headers, params={"client_date": "2026-11-04"}, json={}).json()
    assert t["total_days"] == 1 and t["stage"] == "sprout" and t["status"] == "Growing" and t["read_today"] is True
    again = client.post("/api/v1/tree/read", headers=headers, params={"client_date": "2026-11-04"}, json={}).json()
    assert again["total_days"] == 1  # repeated reads on one day count once
    # offline history from the device is merged, future dates are ignored, bad dates rejected
    merged = client.post("/api/v1/tree/read", headers=headers, params={"client_date": "2026-11-04"},
                         json={"dates": ["2026-11-01", "2026-11-02", "2026-11-03", "2026-12-25"]}).json()
    assert merged["total_days"] == 4 and merged["stage"] == "leaves" and merged["current_streak"] == 4
    assert client.post("/api/v1/tree/read", headers=headers, json={"dates": ["bad"]}).status_code == 422
    # a long break pauses growth but loses nothing
    from datetime import date
    from app.services.tree import tree_state
    later = tree_state({date(2026, 11, d) for d in (1, 2, 3, 4)}, date(2026, 12, 20))
    assert later["total_days"] == 4 and later["stage"] == "leaves" and later["status"] == "Resting" and later["current_streak"] == 0 and later["longest_streak"] == 4
    # pure sync does not count today
    sync = client.post("/api/v1/tree/read", headers=headers, json={"dates": [], "mark_today": False}).json()
    assert sync["total_days"] == 4
    # users are isolated from each other
    other = register(client, "tree2@example.com")
    assert client.get("/api/v1/tree", headers=other).json()["total_days"] == 0


def test_tree_stage_thresholds_match_the_brief():
    from app.services.tree import stage_for
    expected = {1: "sprout", 3: "sprout", 4: "leaves", 7: "leaves", 8: "branches", 14: "branches", 15: "flowers", 21: "flowers", 22: "fruits", 30: "fruits", 80: "fruits"}
    for days, stage in expected.items():
        assert stage_for(days)[1] == stage, days
