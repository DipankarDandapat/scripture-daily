# WisdomOne – Settings redesign and active-verse implementation

This document records the implementation delivered from the approved Settings design and `Feature.txt`.

## Android Settings experience

- Replaced the legacy Settings composable with a dedicated Settings experience while retaining the existing five-item bottom navigation.
- Settings home contains: Tradition & Language, Verse Frequency, Lock Screen & Wallpaper, Widget Studio, Reading Preferences, App Theme, Notifications, Battery & Performance, Help & Support, and Rate WisdomOne.
- No My Profile or Downloads entries are included.
- Subpages have in-app back navigation and Android system-back handling.
- The visual system uses the existing WisdomOne logo, warm cream/light theme, deep purple primary actions, gold accents, rounded cards and a dark-mode-aware Material theme.

## Tradition-aware lock-screen wallpapers

The wallpaper categories are filtered by the selected tradition:

- Hinduism: Krishna, Shiva, Ganesha, Radha Krishna, Bhagavad Gita.
- Christianity: Jesus, Cross, Bible, Church, Biblical Scenes.
- Islam: Islamic Geometric Patterns, Mosques, Quran Calligraphy, Arabic Artwork.
- Buddhism: Buddha, Dharma, Meditation, Lotus.
- General Wisdom: Nature, Mountains, Sunrise, Ocean, Flowers, Universal Inspiration.

The gallery and on-device renderer now use bundled image assets for the Hinduism pack (20 distinct generated devotional/nature artworks, 600×900 WebP). The renderer draws the selected image at the device display dimensions, overlays a contrast panel, then renders the active verse and reference. Other traditions still use the existing tradition-filtered vector scene fallback in this source package; their 20-image photographic/illustrated packs are not included yet. General Wisdom uses only neutral landscape/flower/wave artwork and no religious symbol fallback.

Wallpaper options:

- Multicolor (Automatic), Golden, Royal Purple, Forest Green, Ocean Blue, Soft Sand, Midnight.
- Multicolor rotates the full background palette and decorative treatment whenever the wallpaper is generated. Fixed themes remain fixed between verse updates.
- The Settings preview calls the same wallpaper bitmap renderer used by the lock-screen pipeline.
- The existing exact-device-size bitmap and Android lock-screen wallpaper pipeline is retained.
- Verse text and scripture reference can be independently hidden on the wallpaper.

## Wallpaper content modes

The app stores one canonical mode locally and in the backend:

- `change_verse`: a due scheduled refresh can select a new active verse; Home, lock screen and widget are updated from that verse.
- `keep_same_verse`: scheduled work refreshes wallpaper artwork only. The Home/widget verse is retained. The backend `GET /verses/active` also returns the existing verse after its due time while this mode is active, so Home refresh/app restart cannot accidentally rotate it.

Changing tradition resets the wallpaper category to the first category belonging to that tradition, preventing old-tradition artwork from leaking into the new selection.

## Centralized verse frequency

The only frequency selector is Settings > Verse Frequency:

- Every hour (`1h`)
- Every 6 hours (`6h`)
- Every 12 hours (`12h`)
- Every 24 hours (`24h`)

The independent palette-refresh worker has been retired and old scheduled palette jobs are cancelled. `VerseSyncWorker` is the single cadence owner. Widget Studio contains no frequency selector. Widget colours/layouts are stored independently from wallpaper colours.

## Widget Studio

- Widget theme choices: Day, Dusk, Sage, Ocean, Sand and Midnight. The old AUTO theme is migrated to Day and is not shown as a selectable theme.
- Existing Standard, Compact and Large Text layouts remain.
- Added a text-size scale (80–120%) with an Apply action and wired it to the Glance widget renderer and preview.
- Widget refresh/tap redraws the currently active verse; it no longer selects a random verse. The UI label now says Refresh.

## Reading preferences, app theme and notifications

- Reading preferences control scripture label, chapter/verse reference, translation label, WisdomOne branding and font size on the Home verse card. Wallpaper scripture/reference/branding preferences are also applied by the wallpaper renderer.
- Light, Dark and System theme choices are persisted and applied by the app-level Material theme.
- Daily reminders, verse update notifications and wallpaper update notifications have separate preferences. Wallpaper-update notifications are sent only after an actual wallpaper refresh and only when opted in.
- Battery & Performance links to Android battery/app settings and explains WorkManager/Doze behaviour.

## Active verse persistence and APIs

New authenticated API operations:

- `GET /api/v1/verses/active`: returns the persisted active verse; it selects one only when none exists, preferences no longer match, or the schedule is due (except when `keep_same_verse` is active).
- `POST /api/v1/verses/discover`: explicit Discover action; selects a different eligible verse, persists it and resets the update deadline.
- `POST /api/v1/verses/active/refresh`: scheduler operation; idempotent until due.

`GET /verses/active` and explicit Discover use a per-installation lock and a database row lock where supported. Verse responses now include `scripture_id` and `language_id` so the Room cache records the actual scripture/language of the selected verse.

The Home page now calls the active endpoint for normal refresh/reopen and calls Discover only for the explicit Discover button. Pull-to-refresh is idempotent. The widget refresh callback no longer rotates the verse. The foreground app listens for the persisted active verse ID so a WorkManager update can update Home while the app is open.

## SQLite and migrations

- Added `active_verse_states` (one row per authenticated installation identity) and `verse_selection_history`.
- Added `wallpaper_mode` to `user_preferences`.
- Added Alembic revisions `0005_active_verse_state` and `0006_wallpaper_content_mode`.
- Both migrations are idempotent for databases where app startup has already created the new objects.
- Backend startup's existing schema check adds the `wallpaper_mode` column to an existing SQLite `user_preferences` table; `Base.metadata.create_all` creates the new state/history tables without dropping existing records.
- The bundled `backend/scripture_daily.db` was updated with the new tables and column.

For a current existing database whose schema is already at the previous 0004 application level but has no Alembic version row, the tested migration sequence is:

```bash
cd backend
alembic stamp 0004_wisdomone_v2_enhancements
alembic upgrade head
```

Do not stamp an older database at 0004 unless its existing schema is actually at that application level. Back up the SQLite database before running migrations.

## Security and release signing

- Removed the uploaded `backend/.env` and bundled PKCS#12 keystore from the deliverable.
- Added `backend/.env.example`; create a private `.env` locally and set strong secrets.
- Release signing now reads `WISDOMONE_STORE_FILE`, `WISDOMONE_STORE_PASSWORD`, `WISDOMONE_KEY_ALIAS` and `WISDOMONE_KEY_PASSWORD` from Gradle properties or environment variables. Do not commit signing secrets or keystores.
- `google-services.json` is not included; add your own Firebase configuration if your build requires it.

## Verification results

- Backend Python compilation: passed.
- Backend pytest suite: **26 passed**. The environment did not include `pwdlib`; tests used a temporary test-only PBKDF2 password-hasher shim on `PYTHONPATH`. The shim is not included in the deliverable.
- Alembic migration from a clean database: passed through head (`0006`).
- Alembic migration against a copy of the bundled existing SQLite database: passed through head (`0006`). The migration chain was made idempotent to accommodate the app's existing `Base.metadata.create_all` bootstrap.
- Bundled SQLite database was restored from the uploaded project and upgraded through head without dropping verse/catalog data.
- Kotlin source parsing produced no Kotlin syntax diagnostics; full type checking was not possible because Android/Compose SDK dependencies are not installed here.
- Android Gradle assemble and physical-device tests: **not completed**. Android SDK is not installed and Gradle 8.10.2 could not be downloaded because outbound DNS/network access is unavailable. Do not treat lock-screen permission behaviour, device-specific wallpaper application, or Glance widget rendering as device-verified.

## Local verification

1. Open `android/` in Android Studio with JDK 17 and Android SDK 36.
2. Sync Gradle; add your Firebase `google-services.json` if required by the app configuration.
3. Set `SD_API_BASE_URL` in `android/gradle.properties` to your backend URL.
4. Build and run the debug app on an Android device.
5. In `backend/`, install `requirements.txt`, create a private `.env` from `.env.example`, then run `uvicorn app.main:app --host 0.0.0.0 --port 8000` and `pytest -q`.
6. Verify the acceptance scenarios in `Feature.txt`, especially refresh idempotency, Discover rotation, tradition switching, the two wallpaper modes, device restart and widget synchronisation.

## Approved visual reference

The approved six-screen concept is included at `design/approved_settings_ui_reference.png`. The shipped Android screens are implemented in Compose using the existing app architecture. The wallpaper gallery and lock-screen artwork are generated locally with tradition-specific vector scenes and palette treatment; this avoids introducing remote image dependencies or unlicensed artwork. A future curated photographic/illustrated asset pack can replace these vector scenes without changing the preference, scheduling or active-verse APIs.

The bundled SQLite database has also been sanitised: the anonymous test/install user and device rows were removed while retaining the scripture catalog and verse content.


## Additional work in this continuation

- Added 20 generated Hindu wallpaper images under `android/app/src/main/res/drawable-nodpi/` and a visual contact sheet at `design/hindu_20_wallpaper_asset_preview.jpg`.
- Added a 20-image Hindu wallpaper gallery with selectable image state and persistence.
- Added preferred wallpaper verse text sizes (Small, Medium, Large) and positions (Top, Middle, Bottom).
- Updated the bitmap renderer to use image-based Hindu wallpapers, overlay a contrast panel, and adaptively reduce text size for long verses.
- Added a manual `applyWallpaperNow` path so Save & Apply still applies the chosen wallpaper when automatic updates are disabled.
- Fixed initial Settings loading so the previously selected image index is not reset on first composition.
- Made the Wisdom Tree API test date-relative rather than tied to 2026-11-04.
- Fixed Alembic revisions 0002–0004 so a fresh schema created by the existing metadata bootstrap does not fail with duplicate columns/tables.

## Wallpaper pack status

- Hinduism: 20 generated images included.
- Christianity: image pack not included.
- Islam: image pack not included.
- Buddhism: image pack not included.
- General Wisdom: image pack not included.

The remaining four packs must be added before claiming the requested 100-image release is complete. The UI continues to filter their categories correctly and uses the existing safe tradition-specific vector artwork fallback until the corresponding image packs are supplied.
