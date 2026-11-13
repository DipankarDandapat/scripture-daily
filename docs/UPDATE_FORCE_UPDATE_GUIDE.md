# App Update & Force Update — Complete Guide

## How it works (end-to-end)

### Source of truth

The `app_update` object lives in the `app_config` table (key = `"app_update"`).  
Defaults come from `backend/app/content/app_config.py` and are inserted once on server start.  
**Existing DB rows are never overwritten** — only admin API calls change them.

Current default value:

```json
{
  "enabled": true,
  "force_update": false,
  "latest_version": "1.4.2",
  "minimum_supported_version": "1.0.0",
  "force_update": false,
  "title": "A new version is available",
  "message": "Update WisdomOne to get the latest improvements and fixes.",
  "mandatory_title": "Update required",
  "mandatory_message": "This version of WisdomOne is no longer supported. Please update to continue.",
  "play_store_url": "https://play.google.com/store/apps/details?id=com.scripturedaily",
  "optional_reminder_hours": 24
}
```

---

## Field reference

| Field | Type | Meaning |
|---|---|---|
| `enabled` | bool | `false` turns every update prompt off globally |
| `latest_version` | `"1.5.0"` | Newest version in the Play Store |
| `minimum_supported_version` | `"1.0.0"` | Installs older than this **must** update (cannot skip) |
| `force_update` | bool | `true` = anyone below `latest_version` must update |
| `title` | string (≤ 80 chars) | Optional prompt title |
| `message` | string (≤ 400 chars) | Optional prompt body |
| `mandatory_title` | string (≤ 80 chars) | Mandatory screen title |
| `mandatory_message` | string (≤ 400 chars) | Mandatory screen body |
| `play_store_url` | `https://…` or `market://…` | Where "Update Now" button goes |
| `optional_reminder_hours` | int (0–720) | Hours before re-showing the optional prompt after "Later" |

---

## Startup flow

```
Android starts
  └─ AppConfigManager fetches GET /api/v1/app/config  (≤ 4 s, ETag/304 cache)
       └─ AppGate.decide(snapshot, installedVersion)
            ├─ installed < minimum_supported_version       → Mandatory Update screen
            ├─ force_update AND installed < latest_version → Mandatory Update screen
            ├─ installed < latest_version                  → Optional prompt (dismissible)
            └─ otherwise                                   → Open app normally
```

The gate wraps the **entire** Compose tree — no flash of the app before the gate fires.  
Back press on the mandatory screen calls `finishAffinity` (closes the app, no force-stop).

---

## Decision logic (AppGate.kt)

```kotlin
fun updateStatus(update: UpdateConfig, installed: String): UpdateStatus = when {
    !update.enabled                                                -> DISABLED
    isOlder(installed, update.minimumSupportedVersion)             -> MANDATORY
    update.forceUpdate && isOlder(installed, update.latestVersion) -> MANDATORY
    isOlder(installed, update.latestVersion)                       -> OPTIONAL
    else                                                           -> UP_TO_DATE
}
```

---

## Scenario cheat-sheet

| Goal | `latest_version` | `minimum_supported_version` | `force_update` | Result for v1.4.1 |
|---|---|---|---|---|
| No prompts (up to date) | `1.4.2` | `1.0.0` | `false` | Open |
| Optional prompt | `1.5.0` | `1.0.0` | `false` | Optional dialog |
| Force everyone to update | `1.5.0` | `1.0.0` | `true` | Mandatory screen |
| Block old versions hard | `1.5.0` | `1.5.0` | `false` | Mandatory screen |
| Disable all update UI | `enabled: false` | — | — | Open |

---

## Admin API — base URL & token

| Environment | Base URL |
|---|---|
| Local dev | `http://localhost:8000/api/v1` |
| Android emulator | `http://10.0.2.2:8000/api/v1` |
| Production | `https://your-domain.com/api/v1` |

```
X-Admin-Token: WisdomOne@2024
```

> ⚠️ Change `SD_ADMIN_TOKEN` in `backend/.env` before any public deployment.

---

## Update commands

### Bump latest version (optional prompt for older installs)
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"latest_version": "1.5.0"}'
```

### Make a version mandatory (blocks installs below minimum)
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"minimum_supported_version": "1.5.0"}'
```

### Force update (everyone below latest must update)
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"latest_version": "1.5.0", "force_update": true}'
```

### Disable all update prompts
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### Update Play Store URL
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"play_store_url": "https://play.google.com/store/apps/details?id=com.scripturedaily"}'
```

### Update all texts at once
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "WisdomOne 1.5 is here",
    "message": "Ambient Wisdom, new stories and more.",
    "mandatory_title": "Update required",
    "mandatory_message": "This version is no longer supported. Please update to continue."
  }'
```

---

## Testing each scenario

### Preview what a specific installed version would see
```bash
# Optional prompt
curl "http://localhost:8000/api/v1/app/config/evaluate?version=1.4.1"
# → {"screen": "optional_update", "update_status": "optional", ...}

# Mandatory screen
curl "http://localhost:8000/api/v1/app/config/evaluate?version=0.9.0"
# → {"screen": "mandatory_update", "update_status": "mandatory", ...}

# No prompt (up to date)
curl "http://localhost:8000/api/v1/app/config/evaluate?version=1.5.0"
# → {"screen": "app", "update_status": "up_to_date", ...}
```

### Verify the DB was updated
```bash
# Admin view — shows updated_at timestamp
curl http://localhost:8000/api/v1/admin/config \
  -H "X-Admin-Token: WisdomOne@2024" | python -m json.tool | grep -A20 '"app_update"'

# Public view — exactly what the app receives (ETag changes after any update)
curl -i http://localhost:8000/api/v1/app/config
```

### Force the Android app to re-fetch immediately
Kill and reopen the app (startup always fetches fresh), or temporarily set the interval to 0:

```bash
# Set to 0 (re-fetch on every foreground)
curl -X PUT http://localhost:8000/api/v1/admin/config/config_refresh_min_interval_seconds \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": 0}'

# Restore default (5 minutes)
curl -X PUT http://localhost:8000/api/v1/admin/config/config_refresh_min_interval_seconds \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": 300}'
```

---

## How ETag / 304 caching works

1. App fetches `GET /api/v1/app/config` → server returns `200` + `ETag: "abc123"`
2. App saves the ETag locally
3. Next fetch → app sends `If-None-Match: "abc123"`
4. Config unchanged → `304 Not Modified` (no body, app uses its cached copy)
5. Any value changes → SHA1 hash changes → new ETag → `200` with fresh data

**304 is not an error.** It means the config is unchanged.

---

## Offline behaviour

| Situation | Behaviour |
|---|---|
| No network, no config ever fetched | "You're offline" screen with Retry and Continue |
| No network, cached config says update optional | Optional prompt shown from cache |
| No network, cached config says mandatory update | Mandatory screen (user needs network to update anyway) |
| No network, cached config says maintenance on | Maintenance screen for at most `enforce_cached_for_hours`, then app opens |

---

## Related files

| File | Purpose |
|---|---|
| `backend/app/content/app_config.py` | Default values (`APP_UPDATE` dict) |
| `backend/app/services/app_config.py` | `evaluate_gate()`, `_validate_app_update()`, `set_config()` |
| `backend/app/api/v1/features.py` | `PATCH /admin/app-update` and `GET /app/config/evaluate` routes |
| `android/.../gate/AppGate.kt` | Client-side decision logic (mirrors `evaluate_gate`) |
| `android/.../gate/AppGateScreens.kt` | Mandatory update and optional prompt Compose screens |
| `backend/tests/test_app_config_maintenance.py` | Backend tests for every update scenario |
| `android/.../gate/AppGateTest.kt` | Android unit tests (version compare, 4 scenarios, offline rules) |

---

---

# Maintenance Mode — Complete Guide

## What it does

When `maintenance_mode.enabled` is `true`, the app shows a full-screen maintenance page **before any app content is composed**. The user cannot bypass it — they can only tap **Check Again** (if `allow_retry` is true) or wait until you turn it off.

Checked **after** a mandatory update and **before** an optional update prompt:

```
mandatory update? → Mandatory Update screen
  └─ maintenance?   → Maintenance screen
       └─ optional update? → app + dismissible prompt
            └─ app
```

---

## Current default value (DB)

```json
{
  "enabled": false,
  "title": "We'll Be Back Soon!",
  "message": "We're currently upgrading WisdomOne to bring you a better experience. Our team is working to restore the service as quickly as possible.",
  "estimated_end_time": null,
  "allow_retry": true,
  "enforce_cached_for_hours": 12
}
```

---

## Field reference

| Field | Type | Meaning |
|---|---|---|
| `enabled` | bool | `true` blocks the whole app immediately |
| `title` | string (≤ 80 chars) | Maintenance screen heading |
| `message` | string (≤ 600 chars) | Body text shown to the user |
| `estimated_end_time` | ISO-8601 with timezone or `null` | Shown as "We'll be back around 12:00 PM today" in the user's local time |
| `allow_retry` | bool | Show the **Check Again** button |
| `enforce_cached_for_hours` | int (0–168) | How long a cached `"enabled": true` keeps blocking while the API is unreachable |

---

## Admin API commands

### Enable maintenance (minimal — uses default texts)
```bash
curl -X POST http://localhost:8000/api/v1/admin/maintenance/enable \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{}'
```

### Enable with custom message and estimated end time
```bash
curl -X POST http://localhost:8000/api/v1/admin/maintenance/enable \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Scheduled Maintenance",
    "message": "We are upgrading our servers. Back shortly.",
    "estimated_end_time": "2026-11-12T06:30:00Z",
    "allow_retry": true
  }'
```

### Disable maintenance (app opens normally again)
```bash
curl -X POST http://localhost:8000/api/v1/admin/maintenance/disable \
  -H "X-Admin-Token: WisdomOne@2024"
```

### Edit without toggling (patch individual fields)
```bash
# Hide the retry button
curl -X PATCH http://localhost:8000/api/v1/admin/maintenance \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"allow_retry": false}'

# Clear the estimated end time
curl -X PATCH http://localhost:8000/api/v1/admin/maintenance \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"clear_estimated_end_time": true}'

# Update message only
curl -X PATCH http://localhost:8000/api/v1/admin/maintenance \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"message": "Almost done — back in 10 minutes."}'
```

### Using the generic config key (alternative)
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/maintenance_mode \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
      "enabled": true,
      "title": "We will Be Back Soon!",
      "message": "Database upgrade in progress.",
      "estimated_end_time": "2026-11-12T06:30:00Z",
      "allow_retry": true,
      "enforce_cached_for_hours": 12
    }
  }'
```

---

## Testing maintenance mode

### Preview the maintenance screen (HTML)
```bash
# Light theme, UTC
curl "http://localhost:8000/api/v1/admin/maintenance/preview" \
  -H "X-Admin-Token: WisdomOne@2024" > preview.html

# Dark theme, India time
curl "http://localhost:8000/api/v1/admin/maintenance/preview?theme=dark&tz=Asia/Kolkata" \
  -H "X-Admin-Token: WisdomOne@2024" > preview_dark.html
```

### Check what the app would see right now
```bash
curl "http://localhost:8000/api/v1/app/config/evaluate?version=1.4.2"
# When maintenance is on  → {"screen": "maintenance", ...}
# When maintenance is off → {"screen": "app", ...}
```

### Verify the DB value was saved
```bash
curl http://localhost:8000/api/v1/admin/config \
  -H "X-Admin-Token: WisdomOne@2024" | python -m json.tool | grep -A10 '"maintenance_mode"'
```

### Force the app to re-check immediately
The maintenance screen auto-polls every **2 minutes** while visible. To force it faster:
```bash
# Set refresh interval to 0 temporarily
curl -X PUT http://localhost:8000/api/v1/admin/config/config_refresh_min_interval_seconds \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": 0}'
```
Then kill and reopen the app, or tap **Check Again** on the maintenance screen.

---

## Offline behaviour

| Situation | Behaviour |
|---|---|
| API unreachable, maintenance was **off** in cache | App opens normally on its offline cache |
| API unreachable, maintenance was **on** in cache | Maintenance screen shown for at most `enforce_cached_for_hours` after last fetch, and not past `estimated_end_time`; after that the app opens |
| API unreachable, no config ever fetched | "You're offline" screen with Retry and Continue — maintenance is **never assumed** |

**Recommendation:** keep `enforce_cached_for_hours` at 12 (default). A longer value enforces maintenance more strictly during an outage; a shorter value ensures users are never locked out by a network problem.

---

## Scenario cheat-sheet

| Goal | `enabled` | `estimated_end_time` | `allow_retry` | Result |
|---|---|---|---|---|
| Turn maintenance on (no ETA) | `true` | `null` | `true` | Maintenance screen + Check Again |
| Turn maintenance on with ETA | `true` | `"2026-11-12T06:30:00Z"` | `true` | Screen shows "back around 6:30 AM" |
| Hide retry button | `true` | any | `false` | Maintenance screen, no button |
| Turn maintenance off | `false` | — | — | App opens normally |
| Strict offline lock (24 h) | `true` | `null` | `true` | Cached block lasts 24 h without network |

---

## What is blocked during maintenance

- Home, Daily Verse, Journal, Settings, Discover — never composed (no flash)
- Deep links, launcher shortcuts, notification taps — dropped (`onBlocked` clears them)
- `LockScreenVerseActivity` — redirects to `MainActivity` (the gate) instead of showing content
- App-open ads — suppressed
- Back press — closes the app (`finishAffinity`)

---

## Related files (maintenance)

| File | Purpose |
|---|---|
| `backend/app/content/app_config.py` | `MAINTENANCE_MODE` default dict |
| `backend/app/services/app_config.py` | `_validate_maintenance()`, `evaluate_gate()` |
| `backend/app/api/v1/features.py` | `POST /admin/maintenance/enable`, `POST /admin/maintenance/disable`, `PATCH /admin/maintenance`, `GET /admin/maintenance/preview` |
| `android/.../gate/AppGate.kt` | `maintenanceConfig()`, `decide()` — offline cache rules |
| `android/.../gate/AppGateScreens.kt` | Maintenance Compose screen with auto-poll |
| `backend/tests/test_app_config_maintenance.py` | Backend tests — enable/disable, ETA display, offline cache rules |
| `android/.../gate/AppGateTest.kt` | Android unit tests — cached maintenance window, `estimated_end_time` expiry |

---

---

# Optional Update Popup (Home Screen) — Complete Guide

## What it is

A dismissible `AlertDialog` that floats **on top of the Home screen** — the app is fully usable behind it. The user can tap **Update Now** (opens Play Store) or **Later** (snoozes it). It is not a blocking screen.

---

## When it appears — all conditions must be true

| # | Condition | Where checked |
|---|---|---|
| 1 | `app_update.enabled = true` | `AppGate.updateStatus()` |
| 2 | Installed version < `latest_version` | `AppGate.isOlder(installed, latest)` |
| 3 | Installed version ≥ `minimum_supported_version` | `AppGate.updateStatus()` — otherwise it becomes mandatory |
| 4 | `force_update = false` | `AppGate.updateStatus()` — if true it becomes mandatory |
| 5 | User has not snoozed this exact version recently | `AppGate.shouldPromptOptional()` |

---

## The "Later" snooze logic

```kotlin
fun shouldPromptOptional(update: UpdateConfig, dismissedVersion: String?, dismissedAtMs: Long, nowMs: Long): Boolean {
    if (dismissedVersion != update.latestVersion) return true   // new version → show again immediately
    return nowMs - dismissedAtMs >= update.optionalReminderHours * 3_600_000L
}
```

- Tapping **Later** saves `later_version` + `later_at` to `SharedPreferences` (prefs name: `app_gate`)
- Popup won't show again for `optional_reminder_hours` (default **24 hours**)
- Bumping `latest_version` to a new value shows the popup **immediately** again, ignoring the snooze

---

## How it fits in the startup flow

```
AppGateHost
  └─ AppGate.decide() → Decision.OptionalUpdate
       ├─ content()                  ← Home screen composed normally
       └─ OptionalUpdatePrompt()     ← AlertDialog floats on top
            ├─ shouldPromptOptional() = true  → dialog visible
            └─ shouldPromptOptional() = false → returns early, nothing shown
```

Priority reminder — optional update is last:
```
mandatory update → maintenance → optional update (popup over app) → app
```

---

## Admin API commands

### Make the popup appear (bump version above what is installed)
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"latest_version": "1.5.0"}'
```

### Make the popup disappear (set latest to match installed)
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"latest_version": "1.4.2"}'
```

### Shorten snooze to 0 hours (re-shows on every app open after "Later")
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"optional_reminder_hours": 0}'
```

### Restore default snooze (24 hours)
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"optional_reminder_hours": 24}'
```

### Disable the popup entirely
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"enabled": false}'
```

### Update the popup texts
```bash
curl -X PATCH http://localhost:8000/api/v1/admin/app-update \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "WisdomOne 1.5 is here",
    "message": "New stories, ambient wisdom and more. Update now for the best experience."
  }'
```

---

## Testing the popup

### Preview what a specific installed version would see
```bash
# Shows optional_update (popup over Home)
curl "http://localhost:8000/api/v1/app/config/evaluate?version=1.4.1"
# → {"screen": "optional_update", "update_status": "optional", ...}

# Shows nothing (up to date)
curl "http://localhost:8000/api/v1/app/config/evaluate?version=1.5.0"
# → {"screen": "app", "update_status": "up_to_date", ...}
```

### Verify the DB value was saved
```bash
curl http://localhost:8000/api/v1/admin/config \
  -H "X-Admin-Token: WisdomOne@2024" | python -m json.tool | grep -A20 '"app_update"'
```

### Force the app to re-fetch and show the popup immediately
```bash
# Set refresh interval to 0
curl -X PUT http://localhost:8000/api/v1/admin/config/config_refresh_min_interval_seconds \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": 0}'
```
Then kill and reopen the app. The popup appears on Home as soon as the config is fetched.

### Clear the "Later" snooze on the device (force popup to show again)
The snooze is stored in `SharedPreferences` on the device (prefs: `app_gate`, keys: `later_version`, `later_at`). To reset it during testing:
- Uninstall and reinstall the app, **or**
- Clear app data from Android Settings → Apps → WisdomOne → Storage → Clear Data, **or**
- Bump `latest_version` to a value the device has never seen — the snooze is ignored for new versions

---

## Scenario cheat-sheet

| Goal | `latest_version` | `minimum_supported_version` | `force_update` | `optional_reminder_hours` | Result for v1.4.1 |
|---|---|---|---|---|---|
| Show optional popup | `1.5.0` | `1.0.0` | `false` | `24` | Popup on Home |
| Show popup again immediately after Later | `1.5.0` | `1.0.0` | `false` | `0` | Popup every open |
| No popup (up to date) | `1.4.2` | `1.0.0` | `false` | `24` | Open normally |
| Escalate to mandatory | `1.5.0` | `1.5.0` | `false` | — | Mandatory screen |
| Force update (mandatory) | `1.5.0` | `1.0.0` | `true` | — | Mandatory screen |
| Disable all update UI | `enabled: false` | — | — | — | Open normally |

---

## Difference: optional popup vs mandatory screen

| | Optional popup | Mandatory screen |
|---|---|---|
| App usable behind it | Yes | No |
| Can be dismissed | Yes — "Later" snoozes it | No — back press closes the app |
| Triggered by | `installed < latest` | `installed < minimum` or `force_update = true` |
| Snooze | `optional_reminder_hours` | N/A |
| New version resets snooze | Yes | N/A |

---

## Related files

| File | Purpose |
|---|---|
| `android/.../gate/AppGate.kt` | `updateStatus()`, `shouldPromptOptional()`, `isOlder()` |
| `android/.../gate/AppGateScreens.kt` | `OptionalUpdatePrompt()` composable (AlertDialog) |
| `backend/app/services/app_config.py` | `evaluate_gate()`, `_validate_app_update()` |
| `backend/app/api/v1/features.py` | `PATCH /admin/app-update`, `GET /app/config/evaluate` |
| `backend/tests/test_app_config_maintenance.py` | Backend tests for optional update scenario |
| `android/.../gate/AppGateTest.kt` | Android unit tests — `shouldPromptOptional`, snooze window, new version resets snooze |
