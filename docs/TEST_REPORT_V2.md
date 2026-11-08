# Test report – Enhancements & Journal Reflection

## Automated
| Suite | Result |
|---|---|
| Backend `pytest` (`backend/tests`) | **43 passed** (27 existing + 16 new in `test_journal_config_tree.py`) |
| Android JVM unit tests (`testDebugUnitTest`) | **38 passed** (ad config resolution / global switch / blocked screens / saved-verse ad positions, wallpaper modes + rotation, tree logic, templates) |
| `alembic upgrade head` on an empty DB and on a copy of the bundled DB | OK (migration `0007` made idempotent first) |
| `gradlew :app:assembleDebug` | OK |

Pre-existing failures fixed on the way: two time-dependent backend tests (hard-coded 2026-11-04 / missing import) and one Android test whose expectation
contradicted its own title (155 reading days are *all counted*).

New backend coverage: every active verse has one question in its own language; language-correct and varied questions; curated question for Gita 3.19;
fallback when a question is disabled; save → update-same-day (no duplicates) → edit → delete; validation (blank, >500, unknown verse, bad date, no auth);
pagination / month / date filters; owner isolation; snapshot survives question + verse edits; `/config` defaults use the official Google test unit ids,
production ids stay blank, ETag/304; admin writes apply without a release; malformed ad config is rejected (422); tree activities are unique per level,
progress/completion persists after un-saving, level box hides when nothing is pending, disable hides, re-enable restarts and cannot be undone by a device
re-uploading old days.

## Manual, on a Pixel 7 emulator (Android API 37), backend on localhost
Verified by screenshots: onboarding for Hinduism (consistent outline tradition icons) → startup config gate → Home (Spread the Light, Wisdom Tree + level
activity box, native test ad rendered under the actions) → Share Studio (Share / WhatsApp Status, no wallpaper action) → Explain sheet → Today's Reflection
sheet (question, 500-char counter, tappable thoughts) → Journal (Mood / Reflections tabs, week strip, entry card, Edit sheet, Delete confirmation and backend
deletion) → Settings (list, Tradition & Language, Verse Frequency, Lock Screen & Wallpaper with exclusive toggles, 4-per-page gallery, Christianity pack after
switching tradition, live lock-screen preview, full-screen preview, Save & Apply bottom sheet → "applied to home & lock screen") → Reading Preferences Wisdom Tree
switch (hides the tree) → Dark mode on Home/Discover/Journal/Settings. With `ads_enabled=false` set on the server the ads disappeared on the next foreground
(second `GET /config` in the server log).

## Not covered / needs a human or other hardware
* Other Android versions and OEM wallpaper/permission behaviour (Samsung, Xiaomi, …); only the apply-success path was exercised.
* Real WhatsApp (not installed on the emulator); the share chooser fallback was not exercised.
* Production ad units, UMP consent in an EEA region, rewarded/interstitial/app-open on a device (they are off by default; the debug Ads test lab exists for them).
* Onboarding for every tradition × language combination on-device (verse-content combinations are covered by the existing backend tests).
* Poster wording needs editorial verification (see `design/posters/README.md`).
