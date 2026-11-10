"""Wisdom Tree progression rules (pure functions, shared by the API and unit tests).

Growth is lifetime: every distinct day with a completed daily reading adds one day of growth. Missing a day
never removes anything — it only pauses growth (status "Resting"). The current streak is tracked separately.
"""
from datetime import date, timedelta

# (stage code, label, first day, last day) — the last stage has no upper bound: the tree keeps flourishing.
STAGES = [
    ("sprout", "Sprout", 1, 3),
    ("leaves", "Leaves", 4, 7),
    ("branches", "Branches", 8, 14),
    ("flowers", "Flowers", 15, 21),
    ("fruits", "Fruits", 22, 30),
]


DAYS_PER_LEVEL = 30
MAX_LEVELS = 5
TOTAL_JOURNEY_DAYS = DAYS_PER_LEVEL * MAX_LEVELS


def stage_for(total_days: int) -> tuple[int, str, str]:
    """Return the growth milestone within the active 30-reading-day level."""
    if total_days >= TOTAL_JOURNEY_DAYS:
        return len(STAGES), STAGES[-1][0], STAGES[-1][1]
    day_in_level = 0 if total_days <= 0 else ((total_days - 1) % DAYS_PER_LEVEL) + 1
    for level, (code, label, first, last) in enumerate(STAGES, start=1):
        if day_in_level <= last:
            return level, code, label
    return len(STAGES), STAGES[-1][0], STAGES[-1][1]


def streaks(days: set[date], today: date) -> tuple[int, int]:
    """(current streak, longest streak). The current streak survives until the end of tomorrow's grace day."""
    if not days:
        return 0, 0
    ordered = sorted(days)
    longest = run = 1
    for prev, cur in zip(ordered, ordered[1:]):
        run = run + 1 if (cur - prev).days == 1 else 1
        longest = max(longest, run)
    last = ordered[-1]
    current = 0
    if (today - last).days <= 1:
        current, cursor = 1, last
        while cursor - timedelta(days=1) in days:
            cursor -= timedelta(days=1)
            current += 1
    return current, max(longest, current)


def tree_state(days: set[date], today: date) -> dict:
    total = len(days)
    _stage_index, code, label = stage_for(total)
    journey_complete = total >= TOTAL_JOURNEY_DAYS
    journey_level = 1 if total <= 0 else min(MAX_LEVELS, ((total - 1) // DAYS_PER_LEVEL) + 1)
    journey_day = 0 if total <= 0 else ((total - 1) % DAYS_PER_LEVEL) + 1
    current, longest = streaks(days, today)
    last = max(days) if days else None
    read_today = today in days
    if total == 0:
        status = "Ready to sprout"
    elif journey_complete:
        status = "Journey complete"
    elif read_today or (last is not None and (today - last).days <= 1):
        status = "Growing"
    else:
        status = "Resting"  # growth is paused, nothing is lost
    nxt = None
    for idx, (c, lab, first, _last) in enumerate(STAGES, start=1):
        if not journey_complete and journey_day < first:
            nxt = {"stage": c, "label": lab, "level": idx, "at_days": first, "days_to_go": first - journey_day}
            break
    milestones = [{"stage": c, "label": lab, "level": i, "first_day": first, "last_day": last_day, "unlocked": journey_day >= first}
                  for i, (c, lab, first, last_day) in enumerate(STAGES, start=1)]
    return {"total_days": total, "level": journey_level, "journey_day": journey_day,
            "journey_complete": journey_complete, "days_to_journey_complete": max(0, TOTAL_JOURNEY_DAYS - total),
            "stage": code, "stage_label": label, "status": status,
            "current_streak": current, "longest_streak": longest, "read_today": read_today,
            "last_read": last.isoformat() if last else None, "next_milestone": nxt, "milestones": milestones,
            "history": [d.isoformat() for d in sorted(days)[-90:]]}
