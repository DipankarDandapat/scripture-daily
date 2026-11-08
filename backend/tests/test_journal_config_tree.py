"""Journal Reflection, centralised config/ads and Wisdom Tree activities (restart-on-enable)."""
import os
import tempfile
import uuid
os.environ.setdefault("SD_DATABASE_URL", f"sqlite:///{tempfile.gettempdir()}/scripture_test_{uuid.uuid4().hex}.db")
os.environ.setdefault("SD_JWT_SECRET", "test-secret-with-at-least-32-characters")
from datetime import datetime, timedelta, timezone

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import func, select

from app.core.config import settings
from app.core.database import SessionLocal
from app.main import app
from app.models.entities import JournalReflection, ReadingDay, ReflectionQuestion, Verse

TODAY = datetime.now(timezone.utc).date()


def register(client: TestClient, email: str) -> dict:
    token = client.post("/api/v1/auth/register", json={"email": email, "password": "correct-horse-8"}).json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture(scope="module")
def client():
    with TestClient(app) as c:
        yield c


def _prefs(client, headers, religion=1, language=1, scripture="1"):
    r = client.put("/api/v1/users/preferences", headers=headers, json={"religion_id": religion, "language_id": language, "scripture_ids": scripture, "frequency": "24h", "notifications_enabled": False})
    assert r.status_code == 200, r.text


# ── Reflection questions ─────────────────────────────────────────────────────
def test_every_active_verse_has_one_question_in_its_own_language(client):
    with SessionLocal() as db:
        verses = db.scalar(select(func.count(Verse.id)).where(Verse.is_active.is_(True)))
        questions = db.scalar(select(func.count(ReflectionQuestion.id)).where(ReflectionQuestion.is_active.is_(True)))
        assert verses == questions
        assert not db.scalar(select(func.count(ReflectionQuestion.id)).where(ReflectionQuestion.reflection_question == ""))
        # question rows carry the verse's scripture + language
        mismatched = db.scalar(select(func.count(ReflectionQuestion.id)).join(Verse, Verse.id == ReflectionQuestion.verse_id)
                               .where((Verse.language_id != ReflectionQuestion.language_id) | (Verse.scripture_id != ReflectionQuestion.scripture_id)))
        assert mismatched == 0


def test_questions_follow_language_and_are_not_one_generic_prompt(client):
    en = client.get("/api/v1/verses/today", params={"religion_id": 1, "language_id": 1}).json()
    hi = client.get("/api/v1/verses/today", params={"religion_id": 1, "language_id": 2}).json()
    q_en = client.get(f"/api/v1/verses/{en['id']}/reflection-question").json()
    q_hi = client.get(f"/api/v1/verses/{hi['id']}/reflection-question").json()
    assert q_en["language"] == "en" and q_hi["language"] == "hi" and not q_en["is_fallback"]
    assert any("ऀ" <= ch <= "ॿ" for ch in q_hi["question"])
    with SessionLocal() as db:
        distinct = db.scalar(select(func.count(func.distinct(ReflectionQuestion.reflection_question))).where(ReflectionQuestion.language_id == 1))
        assert distinct >= 60
    assert client.get("/api/v1/verses/99999999/reflection-question").status_code == 404


def test_curated_question_for_a_famous_verse(client):
    with SessionLocal() as db:
        v = db.scalar(select(Verse).where(Verse.scripture_id == 1, Verse.language_id == 1, Verse.chapter == 3, Verse.verse_number == 19))
        assert v is not None
    q = client.get(f"/api/v1/verses/{v.id}/reflection-question").json()
    assert q["question"] == "What is one thing you can do today without expecting a reward?"


def test_missing_question_uses_a_fallback_instead_of_a_blank(client):
    headers = register(client, "fallback@example.com")
    with SessionLocal() as db:
        v = db.scalar(select(Verse).where(Verse.language_id == 1).order_by(Verse.id.desc()))
        row = db.scalar(select(ReflectionQuestion).where(ReflectionQuestion.verse_id == v.id))
        row.is_active = False
        db.commit()
        vid, rid = v.id, row.id
    q = client.get(f"/api/v1/verses/{vid}/reflection-question").json()
    assert q["is_fallback"] and q["question_id"] is None and q["question"].strip()
    with SessionLocal() as db:
        db.get(ReflectionQuestion, rid).is_active = True
        db.commit()


# ── Journal reflections CRUD ─────────────────────────────────────────────────
def test_today_question_save_edit_delete_flow(client):
    h = register(client, "journal1@example.com")
    _prefs(client, h)
    today = client.get("/api/v1/reflections/today", headers=h, params={"client_date": TODAY.isoformat()}).json()
    assert today["question"] and today["verse_reference"] and today["existing"] is None and today["max_length"] == 500
    saved = client.post("/api/v1/reflections", headers=h, json={"verse_id": today["verse_id"], "answer": "  I will help someone today.  "})
    assert saved.status_code == 201, saved.text
    r = saved.json()
    assert r["answer"] == "I will help someone today." and r["question"] == today["question"] and r["verse_reference"] == today["verse_reference"]
    assert r["reflection_date"] == TODAY.isoformat() and r["question_id"] == today["question_id"]
    # same verse + same day updates the existing entry (no duplicates)
    again = client.post("/api/v1/reflections", headers=h, json={"verse_id": today["verse_id"], "answer": "Updated thought"})
    assert again.status_code == 200 and again.json()["id"] == r["id"] and again.json()["answer"] == "Updated thought"
    assert client.get("/api/v1/reflections", headers=h).json()["total"] == 1
    assert client.get("/api/v1/reflections/today", headers=h).json()["existing"]["id"] == r["id"]
    # view / edit / delete
    assert client.get(f"/api/v1/reflections/{r['id']}", headers=h).json()["answer"] == "Updated thought"
    edited = client.put(f"/api/v1/reflections/{r['id']}", headers=h, json={"answer": "A better answer"})
    assert edited.status_code == 200 and edited.json()["answer"] == "A better answer"
    assert client.delete(f"/api/v1/reflections/{r['id']}", headers=h).status_code == 204
    assert client.get(f"/api/v1/reflections/{r['id']}", headers=h).status_code == 404
    assert client.delete(f"/api/v1/reflections/{r['id']}", headers=h).status_code == 404
    assert client.get("/api/v1/reflections", headers=h).json() == {"items": [], "total": 0, "limit": 20, "offset": 0, "has_more": False}


def test_reflection_validation(client):
    h = register(client, "journal2@example.com")
    vid = client.get("/api/v1/verses/today", params={"religion_id": 1, "language_id": 1}).json()["id"]
    assert client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": "   "}).status_code == 422
    assert client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": "x" * 501}).status_code == 422
    assert client.post("/api/v1/reflections", headers=h, json={"verse_id": 99999999, "answer": "ok"}).status_code == 404
    assert client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": "ok", "reflection_date": "bad"}).status_code == 422
    assert client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": "x" * 500}).status_code == 201
    assert client.post("/api/v1/reflections", json={"verse_id": vid, "answer": "ok"}).status_code in (401, 403)


def test_reflections_are_listed_newest_first_with_pagination_and_filters(client):
    h = register(client, "journal3@example.com")
    with SessionLocal() as db:
        vids = [v.id for v in db.scalars(select(Verse).where(Verse.language_id == 1, Verse.scripture_id == 1).order_by(Verse.id).limit(4)).all()]
    days = [TODAY, TODAY - timedelta(days=1), TODAY - timedelta(days=1), TODAY]  # two verses on two different days
    # client_date is clamped to +-1 day of UTC today, so only today/yesterday are creatable through the API
    for vid, d in zip(vids, days):
        assert client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": f"answer {vid}", "reflection_date": d.isoformat()}).status_code == 201
    page = client.get("/api/v1/reflections", headers=h, params={"limit": 3}).json()
    assert page["total"] == 4 and len(page["items"]) == 3 and page["has_more"]
    dates = [i["reflection_date"] for i in page["items"]]
    assert dates == sorted(dates, reverse=True)
    rest = client.get("/api/v1/reflections", headers=h, params={"limit": 3, "offset": 3}).json()
    assert len(rest["items"]) == 1 and not rest["has_more"]
    assert client.get("/api/v1/reflections", headers=h, params={"date": TODAY.isoformat()}).json()["total"] == 2
    assert client.get("/api/v1/reflections", headers=h, params={"month": TODAY.strftime("%Y-%m")}).json()["total"] >= 2
    assert client.get("/api/v1/reflections", headers=h, params={"month": "2020-13"}).status_code == 422
    assert set(client.get("/api/v1/reflections/dates", headers=h).json()) >= {TODAY.isoformat(), (TODAY - timedelta(days=1)).isoformat()}


def test_reflections_are_private_to_their_owner(client):
    a, b = register(client, "journal-a@example.com"), register(client, "journal-b@example.com")
    vid = client.get("/api/v1/verses/today", params={"religion_id": 1, "language_id": 1}).json()["id"]
    rid = client.post("/api/v1/reflections", headers=a, json={"verse_id": vid, "answer": "private"}).json()["id"]
    assert client.get(f"/api/v1/reflections/{rid}", headers=b).status_code == 404
    assert client.put(f"/api/v1/reflections/{rid}", headers=b, json={"answer": "hack"}).status_code == 404
    assert client.delete(f"/api/v1/reflections/{rid}", headers=b).status_code == 404
    assert client.get("/api/v1/reflections", headers=b).json()["total"] == 0
    assert client.get(f"/api/v1/reflections/{rid}", headers=a).json()["answer"] == "private"
    # B can save a reflection on the same verse and day without colliding with A's
    assert client.post("/api/v1/reflections", headers=b, json={"verse_id": vid, "answer": "mine"}).status_code == 201


def test_history_is_preserved_when_the_question_or_verse_changes(client):
    h = register(client, "journal-history@example.com")
    with SessionLocal() as db:
        v = db.scalar(select(Verse).where(Verse.language_id == 1, Verse.scripture_id == 2).order_by(Verse.id))
        vid = v.id
    created = client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": "kept"}).json()
    with SessionLocal() as db:
        q = db.scalar(select(ReflectionQuestion).where(ReflectionQuestion.verse_id == vid))
        original = q.reflection_question
        q.reflection_question = "A totally different edited question?"
        verse = db.get(Verse, vid)
        old_text = verse.text
        verse.text = "Edited verse text"
        db.commit()
    got = client.get(f"/api/v1/reflections/{created['id']}", headers=h).json()
    assert got["question"] == original and got["verse_text"] == old_text and got["verse_reference"]
    with SessionLocal() as db:  # restore catalog + deleting the verse keeps the entry (SET NULL), snapshot intact
        db.get(Verse, vid).text = old_text
        db.scalar(select(ReflectionQuestion).where(ReflectionQuestion.verse_id == vid)).reflection_question = original
        db.commit()
        assert db.scalar(select(func.count(JournalReflection.id)).where(JournalReflection.verse_id == vid)) >= 1


# ── Centralised config + ads ─────────────────────────────────────────────────
ADMIN = {"X-Admin-Token": settings.admin_token}
OFFICIAL_TEST_UNITS = {
    "APP_OPEN": "ca-app-pub-3940256099942544/9257395921", "BANNER_ADAPTIVE_ANCHORED": "ca-app-pub-3940256099942544/9214589741",
    "BANNER_ADAPTIVE_INLINE": "ca-app-pub-3940256099942544/9214589741", "BANNER_FIXED": "ca-app-pub-3940256099942544/6300978111",
    "INTERSTITIAL": "ca-app-pub-3940256099942544/1033173712", "REWARDED": "ca-app-pub-3940256099942544/5224354917",
    "REWARDED_INTERSTITIAL": "ca-app-pub-3940256099942544/5354046379", "NATIVE_ADVANCED": "ca-app-pub-3940256099942544/2247696110",
    "NATIVE_VIDEO": "ca-app-pub-3940256099942544/1044960115",
}


def test_config_serves_ads_defaults_with_official_test_ids_and_etag(client):
    r = client.get("/api/v1/config")
    assert r.status_code == 200
    body = r.json()
    cfg = body["config"]
    assert cfg["ads_enabled"] is True and cfg["ads_use_test_ids"] is True
    assert cfg["ads_app_id"] == "ca-app-pub-7872455938809561~3577314080"
    assert cfg["ads_test_units"] == OFFICIAL_TEST_UNITS
    assert all(v == "" for v in cfg["ads_production_units"].values())  # production IDs are kept separate and unset
    assert cfg["ads_saved_verses"] == {"first_ad_after_items": 1, "repeat_interval_items": 2, "max_inline_ads": 3, "repeat_format": "BANNER_ADAPTIVE_INLINE"}
    assert cfg["ads_placements"]["home_after_daily_verse"]["enabled"] and not cfg["ads_placements"]["app_open_ad"]["enabled"]
    assert not cfg["ads_placements"]["tree_completion_interstitial"]["enabled"]
    assert "reflection_editor" in cfg["ads_blocked_screens"]
    etag = r.headers["etag"]
    assert client.get("/api/v1/config", headers={"If-None-Match": etag}).status_code == 304


def test_admin_config_changes_apply_without_an_app_release(client):
    assert client.put("/api/v1/admin/config/ads_enabled", json={"value": False}).status_code == 403
    assert client.put("/api/v1/admin/config/ads_enabled", headers={"X-Admin-Token": "wrong"}, json={"value": False}).status_code == 403
    before = client.get("/api/v1/config").json()
    assert client.put("/api/v1/admin/config/ads_enabled", headers=ADMIN, json={"value": False}).status_code == 200
    after = client.get("/api/v1/config").json()
    assert after["config"]["ads_enabled"] is False and after["version"] != before["version"]
    assert client.get("/api/v1/config", headers={"If-None-Match": f'"{before["version"]}"'}).status_code == 200
    placements = before["config"]["ads_placements"]
    placements["app_open_ad"]["enabled"] = True
    assert client.put("/api/v1/admin/config/ads_placements", headers=ADMIN, json={"value": placements}).status_code == 200
    assert client.get("/api/v1/config").json()["config"]["ads_placements"]["app_open_ad"]["enabled"] is True
    # restore
    placements["app_open_ad"]["enabled"] = False
    client.put("/api/v1/admin/config/ads_placements", headers=ADMIN, json={"value": placements})
    client.put("/api/v1/admin/config/ads_enabled", headers=ADMIN, json={"value": True})
    # brand new keys can be added for future settings; private keys are not served
    assert client.put("/api/v1/admin/config/some_future_flag", headers=ADMIN, json={"value": {"x": 1}}).status_code == 200
    assert client.put("/api/v1/admin/config/secret_thing", headers=ADMIN, json={"value": 1, "is_public": False}).status_code == 200
    cfg = client.get("/api/v1/config").json()["config"]
    assert cfg["some_future_flag"] == {"x": 1} and "secret_thing" not in cfg
    assert client.delete("/api/v1/admin/config/some_future_flag", headers=ADMIN).status_code == 204


def test_invalid_ad_config_is_rejected_so_it_cannot_crash_apps(client):
    bad = [
        ("ads_enabled", "yes"), ("ads_app_id", "nope"), ("ads_test_units", {"NATIVE_ADVANCED": "bad"}), ("ads_test_units", {"UNKNOWN": "ca-app-pub-3940256099942544/2247696110"}),
        ("ads_production_units", {"NATIVE_ADVANCED": "ca-app-pub-1/2"}), ("ads_placements", {"p": {"enabled": "true", "format": "NATIVE_ADVANCED"}}),
        ("ads_placements", {"p": {"enabled": True, "format": "POPUP"}}), ("ads_placements", {"p": {"enabled": True, "format": "NATIVE_ADVANCED", "ad_unit_id": "x"}}),
        ("ads_saved_verses", {"first_ad_after_items": -1}), ("ads_global", {"max_ads_per_screen": "3"}), ("ads_privacy", {"max_ad_content_rating": "X"}),
        ("Bad-Key", 1),
    ]
    for key, value in bad:
        assert client.put(f"/api/v1/admin/config/{key}", headers=ADMIN, json={"value": value}).status_code == 422, (key, value)
    assert client.get("/api/v1/config").json()["config"]["ads_enabled"] is True


# ── Wisdom Tree activities ───────────────────────────────────────────────────
def test_tree_levels_have_unique_activities_and_five_levels():
    from app.content.tree_activities import LEVELS
    assert [lv["level"] for lv in LEVELS] == [1, 2, 3, 4, 5]
    codes = [a["code"] for lv in LEVELS for a in lv["activities"]]
    titles = [a["title"] for lv in LEVELS for a in lv["activities"]]
    assert len(codes) == len(set(codes)) and len(titles) == len(set(titles))
    assert all(len(lv["activities"]) >= 5 for lv in LEVELS)


def test_tree_activity_progress_completion_and_hiding(client):
    h = register(client, "tree-act@example.com")
    t = client.get("/api/v1/tree", headers=h, params={"client_date": TODAY.isoformat()}).json()
    assert t["enabled"] and t["level"] == 1 and t["activity_box_visible"] and t["activities"]["total_count"] == 6 and t["activities"]["completed_count"] == 0
    assert [lv["status"] for lv in t["levels"]] == ["active", "locked", "locked", "locked", "locked"]
    # report activities
    assert client.post("/api/v1/tree/activity", headers=h, json={"event": "bogus"}).status_code == 422
    t = client.post("/api/v1/tree/activity", headers=h, json={"event": "share"}).json()
    share = next(i for i in t["activities"]["items"] if i["code"] == "l1_share")
    assert share["completed"] and share["progress"] == 1
    explain = lambda tt: next(i for i in tt["activities"]["items"] if i["code"] == "l1_explain")
    t = client.post("/api/v1/tree/activity", headers=h, json={"event": "explain"}).json()
    assert explain(t)["progress"] == 1 and not explain(t)["completed"]
    t = client.post("/api/v1/tree/activity", headers=h, json={"event": "explain"}).json()
    assert explain(t)["completed"]
    # favorites: completion survives un-saving the verses
    ids = [v["id"] for v in client.get("/api/v1/verses/search", params={"q": "the"}).json()[:3]] if isinstance(client.get("/api/v1/verses/search", params={"q": "the"}).json(), list) else []
    if len(ids) < 3:
        with SessionLocal() as db:
            ids = [v.id for v in db.scalars(select(Verse).where(Verse.language_id == 1).limit(3)).all()]
    for vid in ids:
        assert client.post("/api/v1/favorites", headers=h, json={"verse_id": vid}).status_code == 201
    t = client.get("/api/v1/tree", headers=h).json()
    assert next(i for i in t["activities"]["items"] if i["code"] == "l1_save")["completed"]
    for vid in ids:
        client.delete(f"/api/v1/favorites/{vid}", headers=h)
    t = client.get("/api/v1/tree", headers=h).json()
    assert next(i for i in t["activities"]["items"] if i["code"] == "l1_save")["completed"]
    # a saved reflection counts; the rest of the level is finished through direct data so the box can hide
    vid = client.get("/api/v1/verses/today", params={"religion_id": 1, "language_id": 1}).json()["id"]
    assert client.post("/api/v1/reflections", headers=h, json={"verse_id": vid, "answer": "first"}).status_code == 201
    t = client.get("/api/v1/tree", headers=h).json()
    assert next(i for i in t["activities"]["items"] if i["code"] == "l1_reflect")["completed"]
    for _ in range(3):
        client.post("/api/v1/mood/checkins", headers=h, json={"mood": "happy"})
    days = [(TODAY - timedelta(days=n)).isoformat() for n in range(1, 7)]
    t = client.post("/api/v1/tree/read", headers=h, params={"client_date": TODAY.isoformat()}, json={"dates": days}).json()
    assert t["activities"]["all_completed"] and t["activities"]["completed_count"] == 6
    assert t["activity_box_visible"] is False  # nothing pending -> the level's activity box disappears
    assert t["level"] == 1 and t["levels"][0]["completed"] == 6


def test_tree_next_level_has_different_activities():
    from app.services.tree import tree_state
    from app.content.tree_activities import level_catalog
    first = {a["code"] for a in level_catalog(1)["activities"]}
    for lvl in range(2, 6):
        assert not first & {a["code"] for a in level_catalog(lvl)["activities"]}
    today = TODAY
    assert tree_state({today - timedelta(days=i) for i in range(31)}, today)["level"] == 2


def test_tree_disable_hides_and_reenable_restarts_the_journey(client):
    h = register(client, "tree-reset@example.com")
    days = [(TODAY - timedelta(days=n)).isoformat() for n in range(1, 6)]
    t = client.post("/api/v1/tree/read", headers=h, params={"client_date": TODAY.isoformat()}, json={"dates": days}).json()
    assert t["total_days"] == 6 and t["enabled"]
    client.post("/api/v1/tree/activity", headers=h, json={"event": "share"})
    off = client.put("/api/v1/tree/settings", headers=h, json={"enabled": False}, params={"client_date": TODAY.isoformat()}).json()
    assert off["enabled"] is False and off["activity_box_visible"] is False and off["total_days"] == 6  # history is kept while disabled
    # reading while disabled does not advance the tree
    assert client.post("/api/v1/tree/read", headers=h, params={"client_date": TODAY.isoformat()}, json={}).json()["total_days"] == 6
    # activity events are ignored while disabled
    ignored = client.post("/api/v1/tree/activity", headers=h, json={"event": "share"}).json()
    assert ignored["enabled"] is False
    # disabling twice is harmless
    assert client.put("/api/v1/tree/settings", headers=h, json={"enabled": False}).json()["reset_count"] == 0
    on = client.put("/api/v1/tree/settings", headers=h, json={"enabled": True}, params={"client_date": TODAY.isoformat()}).json()
    assert on["enabled"] and on["reset_count"] == 1 and on["started_on"] == TODAY.isoformat()
    # the restart is dated today: the 5 earlier days no longer count (today's reading, made before the switch, does)
    assert on["level"] == 1 and on["total_days"] == 1 and on["stage"] == "sprout" and on["journey_day"] == 1
    assert on["activities"]["completed_count"] == 0 and on["activity_box_visible"]
    # a device re-uploading its old local days cannot bring the old progress back
    t = client.post("/api/v1/tree/read", headers=h, params={"client_date": TODAY.isoformat()}, json={"dates": days, "mark_today": False}).json()
    assert t["total_days"] == 1
    t = client.post("/api/v1/tree/read", headers=h, params={"client_date": TODAY.isoformat()}, json={}).json()
    assert t["total_days"] == 1 and t["read_today"]
    # enabling when already enabled does not reset again
    assert client.put("/api/v1/tree/settings", headers=h, json={"enabled": True}).json()["reset_count"] == 1
    assert client.put("/api/v1/tree/settings", headers=h, json={"enabled": True}).json()["total_days"] == 1
    with SessionLocal() as db:  # reading history rows were never deleted
        uid = client.get("/api/v1/users/me", headers=h).json()["id"]
        assert db.scalar(select(func.count(ReadingDay.id)).where(ReadingDay.user_id == uid)) >= 6
