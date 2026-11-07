# Scripture Daily — PRD Coverage Audit

**Audit date:** 2026-10-04  
**Source reviewed:** supplied Scripture Daily PRD, sections 1–35 (all 1,335 lines)  
**Purpose:** Distinguish implemented behavior from the remaining V1 work; this is not a claim that every PRD item is production-complete.

## Summary

The earlier delivery was a **working starter, not a complete implementation of the PRD**. The main user-visible problems reported—favorites being difficult to verify and no verse on the lock screen—have been addressed in this revision, but some requirements remain partial or unconfigured. In particular, the current verse catalog is demo content, Android account/server sync and FCM are not wired, and administrative catalog management is incomplete.

## Fixes in this revision

- Favorites are now observed as a Room `Flow`, so the Favorites list updates from the database rather than depending on a one-time screen-entry query. Saving/removing gives an on-screen confirmation. An Android instrumentation regression test covers save → list → remove.
- The Android instrumentation test source compiles, but it could not be executed here because no Android device or emulator is attached.
- Verse reminders now post/update a lock-screen-visible notification immediately after a verse is fetched (and on scheduled sync) when the user enables reminders. The notification channel and notification request public lock-screen visibility, include a tap action to open the app, and can be dismissed by the user.
- The Glance widget is declared for both `home_screen` and `keyguard` widget hosts. It can be added to the lock screen only where the device's Android/OEM widget host exposes that feature; the notification is the broader fallback.
- In-app help and the README now explain the reminder permission, lock-screen privacy setting, and device-dependent lock-screen widget setup.

## Coverage against V1 requirements

| PRD area | Current implementation | Status / remaining work |
|---|---|---|
| Religion/language/scripture | Android onboarding selects religion and language; scripture ID is resolved from the API catalog. Initial UI labels/catalog are limited to Hinduism/Gita, Christianity/Bible, and Islam/Quran. | **Partial.** PRD's separate welcome, language, scripture, frequency, notification, and widget setup steps are combined or moved to Settings. No UI for multiple scriptures per religion yet. |
| Daily and random verses | FastAPI has today/random verse routes; Android displays both and caches fetched verses. | **Implemented for the included sample catalog.** Verse strings are illustrative demo paraphrases. |
| Favorites | Android saves locally in Room, observes changes, and supports removal; backend has user-scoped favorites endpoints. | **Local flow implemented.** Android sign-in and server-synced favorites are not wired, so favorites do not sync across devices. |
| Lock-screen visibility | Public-visibility notification when Verse reminders are enabled; keyguard widget category added; same verse is refreshed through the worker. | **Implemented with OS/OEM constraints.** The user must grant notification permission and allow lock-screen content. Direct lock-screen widgets require a compatible host and user placement. |
| Home-screen widget | Glance widget reads Room cache and updates from app/worker. | **Implemented.** User must add it from the launcher's widget picker. Small/medium/large layouts have not been separately designed/device-tested. |
| Notifications/frequency | WorkManager local reminders, with daily or random updates at the selected cadence. | **Partial.** Firebase Cloud Messaging, backend push sender, token lifecycle, and server notification scheduling are not configured. WorkManager timing is approximate and Android may defer it. |
| Offline support | Cached verse fallback is restricted to selected scripture/language; favorites are local. | **Partial.** No full library/chapter download or sync conflict strategy; first use needs a network connection to fetch the catalog/verse. |
| Dark mode/settings | App follows Android system light/dark appearance; tradition, language, frequency, and reminder settings are present. | **Implemented in basic form.** No in-app theme override or full localization of UI strings. |
| Explore/chapter/detail | Search API and chapter API exist. Android Explore supports search. | **Partial.** Chapter browser, verse detail, and About Scripture screens from the PRD are not present in the app. |
| Authentication | Backend register/login/JWT and current-user routes exist. | **Partial.** No sign-in/register UI or token storage in Android. |
| User preferences | Backend preference read/write API; Android persists preferences locally and uses the selected catalog IDs. | **Partial.** Android preferences are not uploaded to the account API. |
| Backend schema | PostgreSQL-ready SQLAlchemy models include users, religions, languages, scriptures, verses, preferences, favorites, and devices. | **Partial.** Books/chapters/translations/notification settings are simplified into verse fields; they are not separate normalized tables as in the conceptual PRD schema. |
| Admin/content publishing | Admin-token-protected verse publishing endpoint exists. | **Partial.** No full CRUD for religions/languages/scriptures/books/chapters/translations, no translation-management flow, and no separate web admin UI. |
| Architecture | Backend is modular by API/core/model/schema; Android uses Compose, Retrofit, Room, Glance, and WorkManager. | **Partial.** Android screens/data calls are still concentrated in `MainActivity`; the PRD's MVVM/repository/use-case separation is not fully implemented. |
| Testing | Backend integration tests cover API/auth/preferences/favorites and pass. Room save/remove instrumentation test source compiles. | **Partial.** Android test was not run without an attached emulator/device; no Compose UI tests or Pixel/Samsung/OEM matrix has been run. |
| Deployment/security | Docker Compose provides FastAPI + PostgreSQL; Alembic initial schema is present. | **Development setup only.** No production HTTPS/Nginx/Redis/FCM, backup/monitoring/crash reporting, privacy policy, release signing, or store submission. Replace development secrets and disable cleartext traffic before release. |

## Notes on lock-screen behavior

The PRD explicitly promises an **Android application, home-screen widget, and notification**; it does not require a special lock-screen-only UI. This build now supports two paths:

1. **Verse notification:** Enable **Settings → Verse reminders**, grant Android notification permission, and save. Android must also permit Scripture Daily notifications and sensitive notification content on the lock screen. The app requests public visibility, but Android and the user retain final control over whether content appears there.
2. **Lock-screen widget:** On devices whose lock-screen widget host supports third-party widgets, add Scripture Daily from the device's lock-screen customization flow. Availability varies by Android release and OEM; use the notification path on devices without that host.

Official references: [Android notification lock-screen visibility](https://developer.android.com/develop/ui/compose/notifications/create-notification) and [Android lock-screen widget FAQ](https://android-developers.googleblog.com/2025/03/widgets-on-lock-screen-faq.html).

## Demo verse content

The few preloaded verses are seeded so the project can be exercised immediately. They are **original illustrative paraphrases**, not authoritative scripture translations. Replace them with reviewed/licensed content and accurate source/license metadata before public use. A small predefined set is expected in this development build; it is not a production content library.

## Validation in this workspace

- Backend: `pytest -q` — **2 passed**.
- Android: debug APK build — **successful**.
- Android instrumentation sources: `compileDebugAndroidTestKotlin` — **successful**.
- Android device/emulator: **none attached**, so instrumentation/UI behavior and OEM-specific lock-screen placement were not run here.

## Recommended remaining V1 work

1. Run the new APK on the user's phone; confirm Save → Snackbar → Favorites → Remove, then verify a reminder after Android notification privacy settings are enabled.
2. Complete onboarding as discrete steps, including frequency/notification choices and widget placement guidance.
3. Wire Android authentication, preference sync, and Favorites API synchronization.
4. Configure Firebase/FCM credentials and server-side notification delivery if remote push is required.
5. Normalize content administration (scripture/book/chapter/translation CRUD and publishing) and add a separate admin web console if needed.
6. Add chapter/detail screens and device-level Compose/widget tests; test supported Android/OEM versions.
7. Replace demo paraphrases with approved content, then complete production HTTPS, signing, privacy, backup, and monitoring work.
