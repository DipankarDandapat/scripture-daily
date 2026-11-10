# Wisdom Tree five-level journey implementation

## Product decision
After completing all five levels, the large Wisdom Tree card is removed from Home automatically. The user does **not** restart from Level 1 automatically. All reading days and completion status remain preserved in local history and backend reading history. This avoids an endless repeat loop and keeps Home uncluttered.

## Progression
- Five levels, each requiring 30 distinct completed reading days (150 reading days total).
- Each level reuses the existing five visual milestones: Sprout (days 1–3), Leaves (4–7), Branches (8–14), Flowers (15–21), Fruits (22–30).
- The next level starts on the next completed reading day after the prior level's 30th day.
- Missed calendar days pause progress; they do not erase it.
- Multiple readings on one date count once.
- After 150 unique reading days, Level 5 remains completed and the Home tree card is hidden.
- Subsequent reading days continue to be recorded, but do not restart or mutate the completed tree journey.

## User experience
- While the journey is active, Home displays the existing Wisdom Tree card.
- The journey detail shows the current level, day within the level, milestones, reading history and completion message.
- Once all five levels are complete, the Home tree card is removed; no Settings switch is added because the lifecycle is automatic and consistent for everyone.
- No existing reading history is deleted.

## Files changed
- `android/app/src/main/java/com/scripturedaily/features/tree/TreeLogic.kt`
- `android/app/src/main/java/com/scripturedaily/features/tree/WisdomTreeUi.kt`
- `android/app/src/main/java/com/scripturedaily/MainActivity.kt`
- `android/app/src/test/java/com/scripturedaily/features/WisdomOneFeaturesTest.kt`
- `backend/app/services/tree.py`

## Validation
Python syntax compilation is run during packaging. Android Gradle/emulator validation depends on a local Android SDK and Gradle distribution and was not available in this environment.
