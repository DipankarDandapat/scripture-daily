"""
Run once to merge the new studio ad placements into an existing scripture_daily.db.

    cd backend
    python -m scripts.migrate_studio_placements

Safe to run multiple times — skips placements that already exist.
"""
import json, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from app.core.database import SessionLocal
from app.services.app_config import _merge_new_placements, _remove_blocked_screen, _NEW_PLACEMENTS

def main():
    db = SessionLocal()
    try:
        _merge_new_placements(db)
        _remove_blocked_screen(db, "share_studio")

        # Verify
        from app.models.entities import AppConfigEntry
        row = db.get(AppConfigEntry, "ads_placements")
        placements = json.loads(row.value)
        found = [k for k in _NEW_PLACEMENTS if k in placements]
        missing = [k for k in _NEW_PLACEMENTS if k not in placements]

        print(f"\n✓ Merged {len(found)} studio placements into ads_placements:")
        for k in found:
            p = placements[k]
            print(f"  {k:45s}  format={p['format']:25s}  enabled={p['enabled']}")

        blocked_row = db.get(AppConfigEntry, "ads_blocked_screens")
        blocked = json.loads(blocked_row.value) if blocked_row else []
        print(f"\n✓ ads_blocked_screens: {blocked}")

        if missing:
            print(f"\n✗ Still missing: {missing}")
        else:
            print("\n✓ All studio placements present in DB.")
    finally:
        db.close()

if __name__ == "__main__":
    main()
