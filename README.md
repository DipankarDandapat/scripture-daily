# Scripture Daily

A native Android starter project based on the supplied PRD: **Kotlin + Jetpack Compose + Room + Glance** backed by a **FastAPI + SQLAlchemy** API. The repository is intentionally content-driven so another scripture or language can be added through catalog data rather than app-specific screen branches.

## WisdomOne v2 features

| # | Feature | Where |
|---|---|---|
| 1 | **Mood-based verse** — "How are you feeling today?" (Anxious / Happy / Sad / Angry) swaps the daily verse for one tagged for that mood; "Another verse" shuffles; every check-in is logged | Home · `GET /mood/verse`, `POST /mood/checkins` |
| 2 | **Universal Truths** — one verse per tradition on the same topic (14 topics), shown side by side; shuffle, share as text or as an image | Discover · `GET /universal-truths/*` |
| 3 | **Share Studio** — 6 templates, 6 backgrounds, 3 fonts + size slider, WhatsApp Status / Instagram Story / square / 4:5 formats, optional WisdomOne mark (reference + source are always drawn) | Home → Studio |
| 4 | **Challenges** — 30 Days of Gratitude, 7 Days of Anxiety Relief, 21 Days of Compassion, 10 Days of Calm; one new day per calendar day; badge on completion | Discover · `GET/POST /challenges/*`, `GET /users/badges` |
| 5 | **Explain like I'm 5** — simple explanation of every verse, in the verse's own language | Home · `GET /verses/{id}/explain` |
| 6 | **Interactive widgets** — tap ↻ *New verse* on the widget itself; Standard, Compact, Large text, Reference-first layouts; Dusk / Sage / Ocean / Sand / Midnight / time-of-day themes (home + lock screen) | Discover → Widget Studio |
| 7 | **Verse → Video & Wallpaper** — vertical H.264/AAC MP4 (slow zoom, drifting light, text fade-in) with on-device generated calm music; wallpapers sized to the screen | Home → Video |
| 8 | **Stories for children** — 8 illustrated stories × 3 swipeable scenes × 7 languages, read-aloud player (device TTS), verse + "What we learn" | Discover → Stories, Home → Story · `GET /stories/*` |
| 9 | **Added by the implementer:** Mood Journal (weekly mood summary), "available tomorrow" challenge state, Hindi Quran reachable from Settings, share card for Universal Truths | — |

### Verse catalog (SQLite, `backend/scripture_daily.db`)

663 active verses; every scripture × language combination has **30+** records (Bible 31). Each verse carries topics, mood tags and a simple explanation. Authentic scripture text only (the old demo paraphrases were removed); English Bible is the public-domain KJV, other renderings are WisdomOne's plain-language translations of the public-domain originals.

| Scripture | en | hi | bn | es | ar | ur | th |
|---|---|---|---|---|---|---|---|
| Bhagavad Gita | 30 | 30 | 30 | | | | |
| Ramayana | 30 | 30 | 30 | | | | |
| Mahabharata | 30 | 30 | 30 | | | | |
| Bible | 31 | 31 | | 31 | | | |
| Quran | 30 | 30 | | | 30 | 30 | |
| Dhammapada | 30 | 30 | | | | | 30 |
| Inspirational Verses | 30 | 30 | 30 | | | | |

> **Review before a public release.** Ramayana and Mahabharata sarga/adhyaya/shloka numbers (and the Yaksha Prashna serial numbers) follow commonly cited numbering and should be verified against a critical edition; the Hindi/Bengali/Urdu/Thai/Spanish renderings should be proofread by native speakers. Content lives in `backend/app/content/*.py` and is synced idempotently by `app/seed.py` whenever `CONTENT_VERSION` changes (verse IDs — and therefore saved favourites — are preserved where the reference matches).

### Database changes

New tables: `app_meta`, `topics`, `verse_topics`, `verse_moods`, `mood_checkins`, `challenges`, `challenge_days`, `user_challenges`, `challenge_progress`, `stories`, `story_translations`, `story_scenes`, `story_scene_translations`. New column: `verses.simple_explanation`. SQLite upgrades happen automatically at start-up (`app/main.py`); other databases use Alembic revision `0003_wisdomone_features`.

## What's implemented (v1 foundation)

- Android first-run tradition/language setup; daily and random verse; local favorites; search; share; settings; system-following light/dark appearance; dark green reading card UI.
- Room offline cache for fetched verses and saved favorites; fallback is restricted to the chosen scripture and language.
- Glance widget reading the local cache, declared for home-screen and keyguard hosts; WorkManager refresh/reminder schedule (hourly, six-hourly, or daily, subject to Android scheduling limits); a public-visibility verse notification for lock-screen fallback when reminders are enabled.
- FastAPI versioned endpoints for registration/login/JWT, user profile, catalog, today/random/specific/search/chapter verses, preferences, favorites, devices, and token-protected verse publishing.
- SQLAlchemy schema suitable for PostgreSQL, SQLite for zero-service local development, demo seed records, Docker Compose for PostgreSQL-backed API.
- Pytest integration coverage for core flows.

## Quick start: API (SQLite)

```bash
cd backend
python -m venv .venv
# Linux/macOS
. .venv/bin/activate
# Windows PowerShell: .venv\\Scripts\\Activate.ps1
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --reload
```

The API creates `scripture_daily.db` in `backend/`, initializes tables and seeds the sample catalog at startup. Open [http://localhost:8000/docs](http://localhost:8000/docs). Health check: `/api/v1/health`.

Run API tests (14 tests: core flows, every scripture×language ≥ 30 verses, moods, explanations, Universal Truths, challenges + badges, stories):

```bash
cd backend
pytest -q
```

The Android project needs a `google-services.json` in `android/app/` (git-ignored). For local builds any Firebase config for package `com.scripturedaily` works.

## Quick start: API (PostgreSQL via Docker)

From the repository root:

```bash
# Replace SD_JWT_SECRET and SD_ADMIN_TOKEN with strong random secrets in backend/.env
docker compose up --build
```

API: [http://localhost:8000/docs](http://localhost:8000/docs). Compose uses PostgreSQL 16 and persists data in the `postgres-data` volume. The backend's `create_all` bootstrap is convenient for development; create and apply formal Alembic revisions before production schema evolution.

## Android app

Open `android/` in Android Studio with JDK 17 and Android SDK 35 installed. Start the API first. The default emulator URL is `http://10.0.2.2:8000/api/v1/`.

For a physical Android device, keep the API bound to `0.0.0.0` and add this to `android/gradle.properties` using the computer's LAN address:

```properties
SD_API_BASE_URL=http://192.168.1.20:8000/api/v1/
```

### Debug build (development)

Clears the Gradle cache and previous outputs, then produces a fresh debug APK:

```bash
cd android
./gradlew clean
./gradlew assembleDebug
```

The signed debug APK is written to:

```
android/app/build/outputs/apk/debug/app-debug.apk
```

Install directly on a connected device or emulator:

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Production / release build (no Chucker)

Chucker (the HTTP inspector overlay) is automatically excluded from release builds — `build.gradle.kts` uses `debugImplementation` for the full library and `releaseImplementation` for the official no-op stub, so **no code changes are needed**.

Before building for release you need a signing keystore. Create one once:

```bash
keytool -genkeypair -v \
  -keystore lumora-release.jks \
  -alias lumora \
  -keyalg RSA -keysize 2048 \
  -validity 10000
```

Add the signing config to `android/gradle.properties` (keep this file out of version control):

```properties
SD_API_BASE_URL=https://scripture-daily.onrender.com/api/v1/
STORE_FILE=/absolute/path/to/lumora-release.jks
STORE_PASSWORD=your_store_password
KEY_ALIAS=lumora
KEY_PASSWORD=your_key_password
```

Add a `signingConfigs` block to `android/app/build.gradle.kts` inside the `android {}` block:

```kotlin
signingConfigs {
    create("release") {
        storeFile = file(providers.gradleProperty("STORE_FILE").get())
        storePassword = providers.gradleProperty("STORE_PASSWORD").get()
        keyAlias = providers.gradleProperty("KEY_ALIAS").get()
        keyPassword = providers.gradleProperty("KEY_PASSWORD").get()
    }
}
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        signingConfig = signingConfigs.getByName("release")
    }
}
```

Then clean and build the release APK:

```bash
cd android
./gradlew clean
./gradlew assembleRelease
```

The release APK (signed, minified, no Chucker) is written to:

```
android/app/build/outputs/apk/release/app-release.apk
```
```commandline
Going forward — if this happens again, run these two commands before building:

./gradlew --stop
./gradlew clean assembleDebug --no-daemon
```
```commandline

cd android
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

For Play Store submission build an AAB instead:

```bash
./gradlew bundleRelease
# output: android/app/build/outputs/bundle/release/app-release.aab
```

```commandline

Build your release
For APK:
.\gradlew assembleRelease

For Google Play AAB:
.\gradlew bundleRelease
```
> Before publishing: set `android:usesCleartextTraffic="false"` in `AndroidManifest.xml`, switch the API URL to HTTPS, add a privacy policy, and configure crash telemetry.

Place the Scripture Daily widget from the Android home-screen picker. On Android devices whose lock-screen host supports widgets, add it through the device's lock-screen customization/widget picker; this widget is declared for the keyguard category. Availability and setup are controlled by Android/OEM and are not present on every device. The cross-device fallback is a verse notification: open in-app Settings, enable **Verse reminders**, grant notification permission, and save. Android must also be allowed to show Scripture Daily notifications and sensitive content on the lock screen; users retain control over that privacy setting. WorkManager delivers updates approximately on the selected cadence; Android may defer execution to protect battery.

## Important content and release notes

- Verse content is authentic public-domain scripture (see *Verse catalog* above). Have it reviewed by scholars/native speakers before public release and keep `source` and `license` metadata accurate.
- The project includes API device registration only as a backend endpoint. No Firebase project credentials or FCM sender are configured, so push delivery is **not active**. The Android reminder is local WorkManager scheduling.
- Android account registration and server-synced favorites/preferences are not wired into the UI yet. Backend authentication and user-scoped APIs are implemented; Android favorites and setup preferences persist locally.
- Admin verse publishing is protected by `SD_ADMIN_TOKEN` (header `X-Admin-Token`). The default is for local development only. There is no separate admin web console in this V1 project.
- No production deployment, release signing, privacy policy, crash telemetry, or store submission is configured. Set `android:usesCleartextTraffic` off and use HTTPS in production.

## Main routes

| Method | Route | Purpose |
|---|---|---|
| GET | `/api/v1/religions`, `/languages`, `/scriptures` | Browse catalog |
| GET | `/api/v1/verses/today`, `/random`, `/{verse_id}` | Verse delivery |
| GET | `/api/v1/verses/search?q=...` | Search verses |
| GET | `/api/v1/scriptures/{id}/chapters/{chapter}/verses` | Chapter contents |
| POST | `/api/v1/auth/register`, `/auth/login` | Create account / sign in |
| GET/PUT | `/api/v1/users/preferences` | Read / update preferences (Bearer token) |
| GET/POST/DELETE | `/api/v1/favorites` | User-scoped saved verses (Bearer token) |
| POST | `/api/v1/devices` | Register a device (Bearer token) |
| POST | `/api/v1/admin/verses` | Publish a verse (`X-Admin-Token`) |
| GET | `/api/v1/moods`, `/mood/verse`, `/mood/summary` · POST `/mood/checkins` | Mood-based verses and journal |
| GET | `/api/v1/verses/{id}/explain`, `/verses/{id}/story` | Simple explanation / story behind a verse |
| GET | `/api/v1/universal-truths/topics`, `/universal-truths/{topic}` | Cross-tradition comparison |
| GET | `/api/v1/challenges`, `/challenges/{id}` · POST `/challenges/{id}/join`, `/challenges/{id}/days/{n}/complete` · DELETE `/challenges/{id}/progress` · GET `/users/badges` | Challenges and badges |
| GET | `/api/v1/stories`, `/stories/{code}` | Children's stories (text per language, scene art tokens) |
| GET | `/api/v1/verses/random?exclude_id=` | Random verse that differs from the current one (widget refresh) |
