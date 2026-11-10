# WisdomOne – App Enhancement & Journal Reflection (implementation notes)

Implements `design/WisdomOne_Enhancement_and_Journal_Reflection_Requirements.html`. Section numbers below match that document.

## Requirement coverage

| § | Requirement | Where | Status |
|---|---|---|---|
| 1 | Home: Save + one primary share button ("Spread the Light") → Share Studio with **Share** and **WhatsApp Status**; Studio button and Set-as-Wallpaper removed | `MainActivity.kt` (HomeScreen), `features/studio/StudioScreen.kt` | Done. WhatsApp has no public "post to Status" intent: the image is sent to WhatsApp (chat picker lists *My status* first), the user reviews and publishes. |
| 2 | Wisdom Tree: 5 monthly levels (150 days), unique activities per level, box hides when a level has no pending activity, Settings switch, re-enable restarts | `backend/app/content/tree_activities.py`, `services/tree_activities.py`, `api/v1/studio.py`, `features/tree/*`, Settings → Reading Preferences | Done. 30 unique activities (6/level). Disabling hides the tree; re-enabling sets `started_on = today` so earlier days/events stop counting (history rows are kept). |
| 3 | Settings redesign to the supplied references | `SettingsExperience.kt` | Done (home list, Tradition & Language, Verse Frequency, Lock Screen & Wallpaper, themed sub-pages). Widget Studio screen was not restyled beyond the shared palette. |
| 4 | Lock screen & wallpaper: 20 images per tradition, gallery of 4-per-page horizontal pager, two exclusive toggles, preview, Save & Apply bottom sheet | `lockscreen/WallpaperPacks.kt`, `LockScreenVerse.kt`, `SettingsExperience.kt`, `assets/wallpapers/*` | Done. See "Wallpaper artwork" below. |
| 5 | Consistent icons | `res/drawable/tradition_*.xml` redrawn as one outline set; Settings uses one gold Material-outlined tile style | Done for onboarding + Settings; other screens already use one icon family or emoji by design (see limitations). |
| 6 | Dark mode contrast | `ui/WTheme.kt` (theme-aware `W` palette), hard-coded colours replaced, status-bar style follows the in-app theme | Done and checked on-device for Home, Discover, Journal, Settings. |
| 7 | AdMob | `android/.../ads/*`, backend `ads_*` config keys | Done: banner (anchored/inline/fixed), native (+video), interstitial, rewarded, rewarded-interstitial, app-open. |
| 8 | Centralised key-value config, startup + foreground refresh | `GET /api/v1/config`, `config/AppConfigManager.kt` | Done. |
| 9 | Journal Reflection from Explain sheet | `features/explain/ExplainSheet.kt`, `features/journal/ReflectionSheet.kt` | Done. |
| 10 | Discover → Journal (Mood / Reflections) with list, view, edit, delete + confirm, empty state | `features/journal/JournalScreen.kt` | Done. |
| 11 | Verse-based questions | `backend/app/content/reflection_questions.py`, `services/reflections.py` | Done: every one of the 1,633 verses has a question in its own language (7 languages); famous verses have hand-written questions; generic fallback if a row is missing. |
| 12 | Backend: tables + APIs | `models/entities.py`, `api/v1/journal.py`, migration `0008` | Done (see API below). |
| 13 | End-to-end testing | `backend/tests`, Android unit tests, on-device run | See `docs/TEST_REPORT_V2.md`. |
| 14 | Posters | `design/posters/` (10 concepts × 4 sizes), `tools/generate_posters.py` | Done; verses need editorial verification before publishing. |

## New / changed APIs (all under `/api/v1`)

| Method & path | Purpose |
|---|---|
| `GET /verses/{id}/reflection-question` | Question for a verse (active row, else a generic fallback flagged `is_fallback`) |
| `GET /reflections/today?verse_id=&client_date=` | Today's verse, question, and the user's existing answer |
| `POST /reflections` | Save (201). Same user + verse + day **updates** the entry (200) – no duplicates. 1–500 chars, trimmed |
| `GET /reflections?limit&offset&month=YYYY-MM&date=` | Newest reflection date first, paginated |
| `GET /reflections/dates?days=` | Dates that have an entry (week-strip ticks) |
| `GET/PUT/DELETE /reflections/{id}` | Owner only (404 for anyone else). Entries snapshot the question, verse text and reference, so they survive catalog edits |
| `GET /tree`, `POST /tree/read` | Now include `enabled`, `started_on`, `activities`, `activity_box_visible`, `levels` |
| `PUT /tree/settings {enabled}` | Enable/disable; enabling after a disable restarts the journey |
| `POST /tree/activity {event}` | Client activity (`explain share story studio truths wallpaper widget discover`) |
| `GET /config` (ETag / `If-None-Match` → 304) | Public key-value config |
| `GET /admin/config`, `PUT/DELETE /admin/config/{key}` | `X-Admin-Token`; ad keys are validated so a bad edit can't ship |
| `PUT /users/preferences` | `wallpaper_mode` is now `verse_and_image` (default) \| `keep_image` \| `colors`; legacy `change_verse`/`keep_same_verse` map to the default |

Tables added: `verse_reflection_questions`, `journal_reflections`, `tree_settings`, `tree_activity_done`, `tree_activity_events`, `app_config`
(created by the app's startup `create_all` and by Alembic `0008_journal_tree_config`; `0007` was made idempotent so a fresh `alembic upgrade head` works).

## Centralised config & AdMob

* Keys (all editable at runtime, see `backend/app/content/app_config.py`): `ads_enabled` (**global switch**), `ads_use_test_ids`, `ads_app_id`,
  `ads_test_units`, `ads_production_units`, `ads_global` (max ads/screen, full-screen and app-open cooldowns), `ads_banner`,
  `ads_placements` (per placement: `enabled`, `format`, optional `ad_unit_id`), `ads_saved_verses` (first ad after N verses, every M, max),
  `ads_blocked_screens`, `ads_privacy`, `config_refresh_min_interval_seconds`.
* Startup: the app waits for `GET /config` for at most 4 s, then uses the last good copy or a built-in fallback with **ads off**.
  Foreground: conditional GET on every return from the background, at most once per `config_refresh_min_interval_seconds`.
* `ads_enabled=false` → no consent flow, no SDK init, no ad request, nothing rendered. Verified on-device (ads vanish after the next foreground).
* Placement defaults follow the spec's "Combination B": home (after verse), saved (after verse 1, then every 2 more, max 3), discover, history inline native,
  wallpaper gallery banner. App-open, interstitial and rewarded stay **off**. Reflection editor, Explain sheet and Share Studio are in `ads_blocked_screens`.
* **Test vs production IDs**: debug builds always use Google's test units regardless of config. A release build uses production units only when
  `ads_use_test_ids=false` **and** the unit id for that format/placement is a valid, non-empty id.
  Go-live checklist: (1) create the ad units in AdMob; (2) `PUT /admin/config/ads_production_units` with the real IDs;
  (3) enable one placement at a time; (4) `PUT /admin/config/ads_use_test_ids {"value": false}`; (5) re-check consent (UMP) in a target region.
* Consent: Google UMP before the SDK initialises; `ads_privacy` sets max content rating (default `PG`), child-directed and under-age tags.
* Debug-only "Ads test lab" (Settings → Help & Support) loads every format against the active config.

## Wallpaper artwork

Hinduism keeps its 20 illustrated images (moved to `assets/wallpapers/hinduism`). Christianity, Islam, Buddhism and General Wisdom are
**original procedurally drawn art** (`tools/generate_wallpapers.py`) – symbols, architecture and landscapes only, with no depiction of any person –
so there is no third-party licensing risk. They are good-looking placeholders rather than photography; any pack can be replaced by dropping 20 files
named `01.webp…20.webp` into its `assets/wallpapers/<slug>/` folder (no code change). See `design/wallpaper_asset_manifest.json`.

## Known limitations
* WhatsApp Status: see §1 above. Wallpaper application depends on the OEM; success is only reported when `WallpaperManager.setBitmap` returns a valid id.
* Reflection questions are curated per topic and spread so that no question is reused more than ~7 times across 510 distinct verses; unique per-verse
  questions can be loaded by editing the `verse_reflection_questions` rows.
* Tree activity events are fire-and-forget (lost if the device is offline at that moment); reading days still sync offline.
* Android 8–16 / multi-OEM wallpaper behaviour was only exercised on one emulator (API 37, Pixel 7 image) – see the test report.
