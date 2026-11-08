"""Wisdom Tree activities: progress per level, completion bookkeeping and the enable/disable (restart) rule.

Reading history is never deleted. Disabling the tree hides it; re-enabling restarts the journey: the
``started_on`` date moves to today so earlier reading days / events no longer count and Level 1's first
activity is the next one to do. Completed-activity rows and reported events are cleared on restart.
"""
from datetime import date, datetime, time, timedelta

from sqlalchemy import delete, distinct, func, select
from sqlalchemy.orm import Session

from app.content.tree_activities import ACTIVITY_EVENTS, LEVELS, level_catalog
from app.models.entities import (
    ChallengeProgress, Favorite, JournalReflection, MoodCheckin, ReadingDay, TreeActivityDone, TreeActivityEvent, TreeSetting, User,
)
from app.services.tree import DAYS_PER_LEVEL, MAX_LEVELS, tree_state


def get_setting(db: Session, user: User) -> TreeSetting:
    row = db.get(TreeSetting, user.id)
    if row is None:
        row = TreeSetting(user_id=user.id, enabled=True, started_on=None, reset_count=0)
        db.add(row)
        db.commit()
    return row


def set_enabled(db: Session, user: User, enabled: bool, today: date) -> TreeSetting:
    row = get_setting(db, user)
    if enabled and not row.enabled:
        # Re-enable => restart from the first activity of Level 1.
        row.started_on = today
        row.reset_count = (row.reset_count or 0) + 1
        db.execute(delete(TreeActivityDone).where(TreeActivityDone.user_id == user.id))
        db.execute(delete(TreeActivityEvent).where(TreeActivityEvent.user_id == user.id))
    row.enabled = enabled
    db.commit()
    return row


def reading_days(db: Session, user: User, started_on: date | None) -> set[date]:
    q = select(ReadingDay.day).where(ReadingDay.user_id == user.id)
    if started_on is not None:
        q = q.where(ReadingDay.day >= started_on)
    return set(db.scalars(q).all())


def record_event(db: Session, user: User, event: str, day: date) -> None:
    if event not in ACTIVITY_EVENTS:
        raise ValueError(event)
    db.add(TreeActivityEvent(user_id=user.id, event=event, day=day))
    db.commit()


def _level_window_start(days: set[date], level: int, started_on: date | None, today: date) -> date:
    ordered = sorted(days)
    idx = (level - 1) * DAYS_PER_LEVEL
    if idx < len(ordered):
        return ordered[idx]
    return started_on or (ordered[0] if ordered else today)


def _metric_progress(db: Session, user: User, spec: dict, since: date, days: set[date]) -> int:
    metric, since_dt = spec["metric"], datetime.combine(since, time.min)
    if metric == "reading_days":
        return len([d for d in days if d >= since])
    if metric == "favorites":
        return db.scalar(select(func.count(Favorite.id)).where(Favorite.user_id == user.id, Favorite.created_at >= since_dt)) or 0
    if metric == "mood_checkins":
        return db.scalar(select(func.count(MoodCheckin.id)).where(MoodCheckin.user_id == user.id, MoodCheckin.created_at >= since_dt)) or 0
    if metric == "reflections":
        return db.scalar(select(func.count(JournalReflection.id)).where(JournalReflection.user_id == user.id, JournalReflection.created_at >= since_dt)) or 0
    if metric == "challenge_days":
        return db.scalar(select(func.count(ChallengeProgress.id)).where(ChallengeProgress.user_id == user.id, ChallengeProgress.completed_on >= since)) or 0
    col = distinct(TreeActivityEvent.day) if spec.get("mode") == "days" else TreeActivityEvent.id
    return db.scalar(select(func.count(col)).where(TreeActivityEvent.user_id == user.id, TreeActivityEvent.event == metric, TreeActivityEvent.day >= since)) or 0


def build_tree(db: Session, user: User, today: date) -> dict:
    setting = get_setting(db, user)
    days = reading_days(db, user, setting.started_on)
    state = tree_state(days, today)
    level = state["level"]
    done_rows = {r.code: r for r in db.scalars(select(TreeActivityDone).where(TreeActivityDone.user_id == user.id)).all()}
    since = _level_window_start(days, level, setting.started_on, today)
    catalog = level_catalog(level)
    items, newly = [], False
    for spec in catalog["activities"]:
        progress = _metric_progress(db, user, spec, since, days)
        completed = spec["code"] in done_rows or progress >= spec["target"]
        if completed and spec["code"] not in done_rows and setting.enabled:
            db.add(TreeActivityDone(user_id=user.id, code=spec["code"], completed_on=today))
            newly = True
        items.append({"code": spec["code"], "title": spec["title"], "description": spec["description"], "metric": spec["metric"],
                      "target": spec["target"], "progress": min(progress, spec["target"]) if not completed else spec["target"], "completed": completed})
    if newly:
        db.commit()
    done_codes = set(done_rows) | {i["code"] for i in items if i["completed"]}
    all_done = all(i["completed"] for i in items)
    levels = []
    for lv in LEVELS:
        codes = [a["code"] for a in lv["activities"]]
        n_done = sum(1 for c in codes if c in done_codes)
        status = "done" if (state["journey_complete"] or lv["level"] < level) else ("active" if lv["level"] == level else "locked")
        levels.append({"level": lv["level"], "title": lv["title"], "days": lv["days"], "completed": n_done, "total": len(codes), "status": status})
    state.update({
        "enabled": setting.enabled,
        "started_on": setting.started_on.isoformat() if setting.started_on else None,
        "reset_count": setting.reset_count or 0,
        "activities": {"level": level, "title": catalog["title"], "subtitle": catalog["subtitle"], "days": catalog["days"], "items": items,
                       "completed_count": sum(1 for i in items if i["completed"]), "total_count": len(items), "all_completed": all_done},
        # The level's activity box disappears from the active UI once every activity in it is done (or the tree is off / finished).
        "activity_box_visible": bool(setting.enabled and not state["journey_complete"] and not all_done),
        "levels": levels,
    })
    return state
