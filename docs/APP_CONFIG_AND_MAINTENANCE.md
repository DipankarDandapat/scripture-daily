# App configuration, update prompts & maintenance mode

WisdomOne no longer uses **Firebase Remote Config**. Everything the app needs at startup comes from one backend document:

```
GET /api/v1/app/config          (ETag / If-None-Match → 304 when unchanged)
```

`GET /api/v1/config` returns the same document and is kept only for app versions released before this change.

## 1. What was migrated

| Before (Firebase Remote Config) | After (`/api/v1/app/config`) |
|---|---|
| `latestAppVersion` — any older install saw a blocking "Update Required" screen | `app_update` object (below) with separate *latest* and *minimum supported* versions, optional vs. mandatory, texts and Play Store URL |
| `ForceUpdateManager` fetched Remote Config inside the Home composable, so users briefly entered the app before the update screen appeared | `AppGateHost` (android `gate/`) evaluates the config **before** any app screen is composed |
| `com.google.firebase:firebase-config` dependency | removed. Analytics, Crashlytics and Messaging are still used and unchanged |

Audit result: `ForceUpdateManager.kt` was the only Remote Config consumer (ads, Meta SDK switch, Wisdom Tree default were already served by the backend config through `AppConfigManager`).

## 2. Configuration keys

### `app_update`

| Field | Type | Meaning |
|---|---|---|
| `enabled` | bool | `false` turns every update prompt off |
| `latest_version` | `"1.4.2"` | newest version in the Play Store |
| `minimum_supported_version` | `"1.0.0"` | older installs **must** update (cannot be skipped) |
| `force_update` | bool | `true` = anything older than `latest_version` must update |
| `title`, `message` | text | optional prompt texts |
| `mandatory_title`, `mandatory_message` | text | mandatory screen texts |
| `play_store_url` | `https://…` or `market://…` | where "Update Now" goes (falls back to the Play Store page of the package) |
| `optional_reminder_hours` | int | after "Later", ask again after this many hours (or immediately for a newer version) |

Scenarios (installed version `I`):

| # | Condition | Result |
|---|---|---|
| 1 | `I ≥ minimum` and `I < latest` is false (supported) | no update UI |
| 2 | `I < latest` | optional prompt — **Update Now** / **Later** |
| 3 | `I < minimum_supported_version`, or `force_update` and `I < latest` | full-screen mandatory update, back button closes the app |
| 4 | `I ≥ latest` | continue normally |

### `maintenance_mode`

| Field | Type | Meaning |
|---|---|---|
| `enabled` | bool | `true` blocks the whole app |
| `title`, `message` | text | default *We'll Be Back Soon!* / upgrade message |
| `estimated_end_time` | ISO-8601 with zone or `null` | shown as "We'll be back around 12:00 PM today" in the user's local time |
| `allow_retry` | bool | show the **Check Again** button |
| `enforce_cached_for_hours` | int (0–168) | how long a *cached* "enabled" may keep blocking while the API is unreachable |

### `ambient_wisdom`
Server kill switches for the Ambient Wisdom Layer (`live_wallpaper_available`, `activity_aware_available`, `ambient_notification_available`).

Every value is validated on write (`422` for bad versions, URLs, texts, times), so a typo can never crash installed apps.

## 3. Startup priority

```
fetch config (≤ 4 s, cached copy / safe defaults after that)
 └─ mandatory update?  → Mandatory Update screen
     └─ maintenance?   → Maintenance screen
         └─ optional update? → app + dismissible prompt
             └─ app
```

Implemented identically in Android (`gate/AppGate.kt`) and the backend (`app.services.app_config.evaluate_gate`, exposed as
`GET /api/v1/app/config/evaluate?version=1.4.2` so admins can check a change before users see it).

The gate wraps the **entire** Compose tree in `MainActivity`, so while it blocks:
- Home, Daily Verse, Journal, Settings, Discover etc. are never composed (no flash of the app before the gate);
- back press closes the app (`finishAffinity`, no force-stop);
- deep links, launcher shortcuts and notification taps are dropped (`onBlocked` clears them) — they cannot open an app screen;
- `LockScreenVerseActivity` redirects to `MainActivity` (the gate) instead of showing content;
- app-open ads are suppressed.

When the config changes while the app is open (foreground refresh, **Check Again**, or the automatic re-check every 2 minutes on the maintenance screen), the gate recomposes immediately — users enter the app as soon as maintenance is switched off.

## 4. Offline behaviour & trade-offs

| Situation | Behaviour |
|---|---|
| API unreachable, **no** config ever fetched (fresh install) | "You're offline" screen with **Try Again** and **Continue**. Maintenance is never *assumed*. |
| API unreachable, cached config says maintenance **off** | app opens on its offline cache |
| API unreachable, cached config says maintenance **on** | maintenance screen (with a "last status we received" note) for at most `enforce_cached_for_hours` after the fetch and never past `estimated_end_time`; afterwards the app opens |
| API unreachable, cached config says mandatory update | mandatory screen (the user needs a connection to update anyway) |

**Trade-off.** During maintenance the backend may be partly down. Keep `/api/v1/app/config` reachable (it only reads one small table) so devices get an authoritative answer. If the whole server is down, the cache rule above decides: a longer `enforce_cached_for_hours` enforces maintenance more strictly; a shorter one guarantees users are never locked out for long by a network problem. 12 h is the default.

## 5. Admin operations

All admin calls need `X-Admin-Token: <SD_ADMIN_TOKEN>`.

```bash
# Optional update to 1.5.0
curl -X PATCH $API/admin/app-update -H "X-Admin-Token: $TOKEN" -H "Content-Type: application/json" \
     -d '{"latest_version": "1.5.0", "title": "New in 1.5", "message": "Ambient Wisdom is here."}'

# Make 1.5.0 mandatory
curl -X PATCH $API/admin/app-update -H "X-Admin-Token: $TOKEN" -H "Content-Type: application/json" \
     -d '{"minimum_supported_version": "1.5.0"}'

# Maintenance on (texts and ETA optional)
curl -X POST $API/admin/maintenance/enable -H "X-Admin-Token: $TOKEN" -H "Content-Type: application/json" \
     -d '{"estimated_end_time": "2026-11-12T06:30:00Z", "message": "Database upgrade in progress."}'

# Edit without toggling (hide retry, clear the ETA)
curl -X PATCH $API/admin/maintenance -H "X-Admin-Token: $TOKEN" -H "Content-Type: application/json" \
     -d '{"allow_retry": false, "clear_estimated_end_time": true}'

# Maintenance off
curl -X POST $API/admin/maintenance/disable -H "X-Admin-Token: $TOKEN"

# Preview the maintenance screen (HTML, light/dark, any time zone)
curl "$API/admin/maintenance/preview?theme=dark&tz=Asia/Kolkata" -H "X-Admin-Token: $TOKEN" > preview.html && open preview.html

# What would version 1.4.2 see right now?
curl "$API/app/config/evaluate?version=1.4.2"
```

The generic `PUT/PATCH /api/v1/admin/config/{key}` endpoints work for these keys too (object values are merged).

## 6. Tests

- Backend: `backend/tests/test_app_config_maintenance.py` — endpoint + ETag, every update scenario, priority, validation, admin flows, preview.
- Android: `app/src/test/.../gate/AppGateTest.kt` — version compare, four scenarios, priority, offline/cached rules, malformed config, "Later" reminder.
