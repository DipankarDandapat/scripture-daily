# Meta (Facebook) SDK — Event Tracking Reference

All events are sent to **Meta Events Manager** and used for Facebook / Instagram
app-install campaign attribution and optimisation.

---

## Enable / disable

Meta event tracking is controlled by a single backend config flag:

| Config key | Type | Default | Effect |
|---|---|---|---|
| `meta_ads_enabled` | boolean | `false` | `true` → SDK initialised, install/activation auto-logging on, events flow to Meta. `false` → SDK stays uninitialised, nothing is sent (not even install/activation), and all `MetaAnalytics.track()` calls are no-ops. |

Set the flag in the backend `GET /api/v1/app/config` response
(`backend/app/content/app_config.py`, read by `AppConfigManager`). No app update
is required to turn tracking on or off.

How the gate works:

- `AndroidManifest.xml` sets `com.facebook.sdk.AutoInitEnabled` and
  `com.facebook.sdk.AutoLogAppEventsEnabled` to **`false`**, so the SDK never
  initialises or logs on its own.
- `WisdomOneApplication` waits for the startup config, then calls
  `MetaAnalytics.init()` and calls it again every time `meta_ads_enabled`
  changes on a later config refresh.
- `MetaAnalytics.init()` with the flag **on**: `setAutoInitEnabled(true)`,
  `fullyInitialize()`, `setAutoLogAppEventsEnabled(true)`, `activateApp()`.
  With the flag **off**: sets both SDK settings back to `false`. The SDK
  persists these settings, so after the flag is turned off at most one cold
  start (the one before the new config is applied) can still auto-initialise.

---

## Credentials (fill in before launch)

| String resource | Where to get it |
|---|---|
| `facebook_app_id` | Meta Developer dashboard → your app → Settings → Basic |
| `facebook_client_token` | Meta Developer dashboard → your app → Settings → Advanced → Client token |

> **Never** embed the Meta App Secret in the Android app.

---

## Events

| Event name | Kotlin constant | Trigger | Parameters | Priority |
|---|---|---|---|---|
| `fb_mobile_activate_app` / install | automatic | App install and app open — logged by the SDK only after `MetaAnalytics.init()` has enabled it | — | Critical |
| `onboarding_completed` | `Event.ONBOARDING_COMPLETED` | User selects tradition + language and taps **Start reading** | `language_id` (app display language id) | High |
| `first_verse_read` | `Event.FIRST_VERSE_READ` | First time a verse is on screen on Home on this install (`MetaAnalytics.trackFirstVerseRead`). Fires at most once per install; the moment is claimed even while tracking is off, so enabling Meta later never reports an old user's "first" verse | — | Medium |
| `daily_reminder_enabled` | `Event.DAILY_REMINDER_ENABLED` | User grants notification permission or enables reminders in the setup guide or Settings | `source` (`"reminders"`) | Medium |
| `verse_saved` | `Event.VERSE_SAVED` | User saves (bookmarks) a verse | — | Low |
| `mood_checked_in` | `Event.MOOD_CHECKED_IN` | User submits a mood check-in (first selection only, not shuffle) | — | Medium |
| `challenge_joined` | `Event.CHALLENGE_JOINED` | Reserved — wire to the join-challenge call in `ChallengesScreen` | — | Medium |
| `challenge_day_completed` | `Event.CHALLENGE_DAY_DONE` | Reserved — wire to the complete-day call in `ChallengesScreen` | — | Low |
| `story_opened` | `Event.STORY_OPENED` | User opens a children's story from the Home screen | — | Low |
| `share_studio_opened` | `Event.SHARE_STUDIO_OPENED` | User taps **Share with the World** on the Home screen | — | Low |
| `explain_opened` | `Event.EXPLAIN_OPENED` | User taps **Explain this to me like I'm 5** | — | Low |

### Sensitive data — never send

Meta's terms prohibit sharing religion, beliefs, health or mental state and
other sensitive attributes, and ads must not be targeted on them. So no event
may carry the tradition/religion, scripture name, verse text or id, story id,
mood, or anything the user types.

`MetaAnalytics` enforces this: any parameter key containing one of the stems in
`MetaAnalytics.SENSITIVE_KEY_STEMS` (e.g. `religion`, `tradition`, `faith`,
`scripture`, `verse`, `story`, `mood`, `emotion`, `health`, …) is dropped, both
in `MetaAnalytics.params()` and again in `track()` for any hand-built `Bundle`.
`MetaAnalyticsTest` covers the blocklist. If you need a new parameter, use a
neutral key and a value that reveals nothing sensitive.

> `language_id` is the app display language (English, Hindi, Arabic, …). It is
> kept for campaign localisation but correlates loosely with tradition; drop it
> if Meta review flags it.

---

## Implementation files

| File | Role |
|---|---|
| `android/app/src/main/java/com/scripturedaily/analytics/MetaAnalytics.kt` | Wrapper — reads config flag, initialises the SDK, filters sensitive keys, calls `AppEventsLogger` |
| `android/app/src/main/java/com/scripturedaily/WisdomOneApplication.kt` | Calls `MetaAnalytics.init()` after startup config and whenever `meta_ads_enabled` changes |
| `android/app/src/test/java/com/scripturedaily/analytics/MetaAnalyticsTest.kt` | Unit tests: sensitive-key blocklist, once-per-install `first_verse_read` |
| `android/app/src/main/java/com/scripturedaily/MainActivity.kt` | Call sites for all wired events |
| `android/app/src/main/res/values/strings.xml` | `facebook_app_id` and `facebook_client_token` placeholders |
| `android/app/src/main/AndroidManifest.xml` | `<meta-data>` entries; auto-init and auto-logging set to `false` |
| `android/app/build.gradle.kts` | `com.facebook.android:facebook-core:19.0.0` dependency |
| `android/app/src/main/java/com/scripturedaily/config/AppConfigManager.kt` | `meta_ads_enabled` added to `SAFE_DEFAULTS` (default `false`) |

---

## Wiring remaining reserved events

```kotlin
// ChallengesScreen — join
MetaAnalytics.track(context, MetaAnalytics.Event.CHALLENGE_JOINED)

// ChallengesScreen — complete a day
MetaAnalytics.track(context, MetaAnalytics.Event.CHALLENGE_DAY_DONE)
```

---

## Testing

1. Set `meta_ads_enabled: true` in the backend config (or temporarily hard-code
   `return true` in `MetaAnalytics.isEnabled()` for a local test build).
2. Check logcat for the `MetaAnalytics` tag: `init() — SDK activated` and
   `track(<event>) params=[…] — sent`. With the flag off you should see
   `tracking disabled` and no Meta network traffic, even on first install.
3. Use the **Meta Events Manager → Test Events** tab with your device's
   advertising ID to verify events arrive in real time.
4. Revert the hard-code before merging.
