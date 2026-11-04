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


def stage_for(total_days: int) -> tuple[int, str, str]:
    """(level 1..5, stage code, label). Zero days -> level 1 / sprout (not yet unlocked)."""
    for level, (code, label, first, last) in enumerate(STAGES, start=1):
        if total_days <= last or level == len(STAGES):
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
    level, code, label = stage_for(total)
    current, longest = streaks(days, today)
    last = max(days) if days else None
    read_today = today in days
    if total == 0:
        status = "Ready to sprout"
    elif total > STAGES[-1][3]:
        status = "Flourishing"
    elif read_today or (last is not None and (today - last).days <= 1):
        status = "Growing"
    else:
        status = "Resting"  # growth is paused, nothing is lost
    nxt = None
    for idx, (c, lab, first, _last) in enumerate(STAGES, start=1):
        if total < first:
            nxt = {"stage": c, "label": lab, "level": idx, "at_days": first, "days_to_go": first - total}
            break
    milestones = [{"stage": c, "label": lab, "level": i, "first_day": first, "last_day": last_day, "unlocked": total >= first}
                  for i, (c, lab, first, last_day) in enumerate(STAGES, start=1)]
    return {"total_days": total, "level": level, "stage": code, "stage_label": label, "status": status,
            "current_streak": current, "longest_streak": longest, "read_today": read_today,
            "last_read": last.isoformat() if last else None, "next_milestone": nxt, "milestones": milestones,
            "history": [d.isoformat() for d in sorted(days)[-90:]]}
