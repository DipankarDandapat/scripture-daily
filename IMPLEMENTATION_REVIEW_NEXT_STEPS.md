# WisdomOne implementation review — 6 November 2026

## Existing implementation confirmed in the supplied ZIP

- Settings home includes the requested ten sections and omits My Profile and Downloads.
- Verse Frequency is centralized in Settings with 1h, 6h, 12h and 24h choices.
- Wallpaper settings include auto wallpaper, both verse/wallpaper modes, tradition-specific category lists, seven colour themes, text sizing/position and a live preview.
- Widget Studio is retained and uses the shared verse cache; its cadence is not separately scheduled.
- Android uses one unique WorkManager job (`scripture-daily-sync`). The legacy palette worker is cancelled/retired.
- FastAPI has persisted active-verse GET, explicit discover POST, and a due-refresh operation. User preference changes invalidate the active verse only when tradition/language changes and reset the deadline when frequency changes.
- The app stores the active verse locally and falls back to a cached verse when the API is unavailable.

## Confirmed gaps — do not mark these complete yet

1. **Wallpaper gallery assets:** the ZIP contains a 20-image Hindu artwork pack. Christianity, Islam, Buddhism and General Wisdom currently render category-themed backgrounds, not 20 separately bundled curated image assets each. This is the largest visual deliverable still open.
2. **Backend verification environment:** the supplied runtime is missing `pwdlib`, and outbound network access is unavailable, so the declared backend requirements cannot be installed here. A temporary test-only password-hash shim was used solely to execute the active-verse tests; it is not included in the project.
3. **Android build verification:** Gradle wrapper needs Gradle 8.10.2, but the distribution could not be downloaded because network access is unavailable. No APK build result is claimed from this run.
4. **Device validation:** no Android device/emulator is attached, so wallpaper permissions, actual lock-screen application, OEM restrictions, and widget appearance remain unverified on hardware.
5. **Production content:** catalog source/license review and replacement of any illustrative content must be completed before release; the project’s own status notes call this out.

## Regression test strengthened in this pass

`backend/tests/test_api.py::test_active_verse_is_persisted_and_only_discover_rotates` now checks not only that repeated active-verse GET requests retain the same verse ID, but also that `selected_at` and `next_update_at` remain unchanged. This guards against accidental timer resets on read.

## Validation actually performed

- Targeted backend tests: **2 passed** (`test_active_verse_is_persisted_and_only_discover_rotates`, `test_keep_same_verse_wallpaper_mode_prevents_due_rotation`).
- Full backend suite: attempted, but did not finish within the available execution window.
- Android Gradle build: attempted offline; blocked because Gradle 8.10.2 was not cached and network access is unavailable.

## Next implementation sequence

1. Add and review the four missing tradition-specific wallpaper image packs (20 images per tradition), with General Wisdom strictly non-religious.
2. Connect each gallery selection to its own image resource and verify rotation remains inside the selected tradition/category.
3. Run the full backend suite in an environment with the declared dependencies installed.
4. Build debug and release variants with the project’s pinned Gradle distribution, then run instrumentation/UI tests on at least one Pixel/AOSP device and one Samsung device.
5. Verify acceptance cases 1–17 from `Feature.txt`, including manual discover, due refresh, offline fallback, device restart and wallpaper-only mode.
