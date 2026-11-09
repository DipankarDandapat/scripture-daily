# Meta (Facebook) SDK — Event Tracking Reference

All events are sent to **Meta Events Manager** and used for Facebook / Instagram
app-install campaign attribution and optimisation.

---

## Enable / disable

Meta event tracking is controlled by a single backend config flag:

| Config key | Type | Default | Effect |
|---|---|---|---|
| `meta_ads_enabled` | boolean | `false` | `true` → events flow to Meta. `false` → all `MetaAnalytics.track()` calls are no-ops. |

Set the flag in the backend `/config` response (same JSON object used by
`AppConfigManager`). No app update is required to turn tracking on or off.

---

## Credentials (fill in before launch)

| String resource | Where to get it |
|---|---|
| `facebook_app_id` | Meta Developer dashboard → your app → Settings → Basic |
| `facebook_client_token` | Meta Developer dashboard → your app → Settings → Advanced → Client token |

> **Never** embed the Meta App Secret in the Android app.

---

## Events

| Event name (constant) | Kotlin constant | Trigger | Priority |
|---|---|---|---|
| `app_activated` | automatic | First open / re-open after install — sent automatically by the SDK | Critical |
| `onboarding_completed` | `Event.ONBOARDING_COMPLETED` | User selects tradition + language and taps **Start reading** | High |
| `first_verse_read` | `Event.FIRST_VERSE_READ` | Reserved — not yet wired; fire after the first `markReading()` on a fresh install | Medium |
| `daily_reminder_enabled` | `Event.DAILY_REMINDER_ENABLED` | User grants notification permission or enables reminders in the setup guide or Settings | Medium |
| `verse_saved` | `Event.VERSE_SAVED` | User saves (bookmarks) a verse | Low |
| `mood_checked_in` | `Event.MOOD_CHECKED_IN` | User submits a mood check-in (first selection only, not shuffle) | Medium |
| `challenge_joined` | `Event.CHALLENGE_JOINED` | Reserved — wire to the join-challenge call in `ChallengesScreen` | Medium |
| `challenge_day_completed` | `Event.CHALLENGE_DAY_DONE` | Reserved — wire to the complete-day call in `ChallengesScreen` | Low |
| `story_opened` | `Event.STORY_OPENED` | User opens a children's story from the Home screen | Low |
| `share_studio_opened` | `Event.SHARE_STUDIO_OPENED` | User taps **Share with the World** on the Home screen | Low |
| `explain_opened` | `Event.EXPLAIN_OPENED` | User taps **Explain this to me like I'm 5** | Low |

> **Privacy note:** No verse text, religion, scripture name, language, or any
> other user-generated content is included in event names or parameters.
> Only generic engagement action names are sent.

---

## Implementation files

| File | Role |
|---|---|
| `android/app/src/main/java/com/scripturedaily/analytics/MetaAnalytics.kt` | Wrapper — reads config flag, calls `AppEventsLogger` |
| `android/app/src/main/java/com/scripturedaily/MainActivity.kt` | Call sites for all wired events |
| `android/app/src/main/res/values/strings.xml` | `facebook_app_id` and `facebook_client_token` placeholders |
| `android/app/src/main/AndroidManifest.xml` | `<meta-data>` entries for SDK init |
| `android/app/build.gradle.kts` | `com.facebook.android:facebook-core:19.0.0` dependency |
| `android/app/src/main/java/com/scripturedaily/config/AppConfigManager.kt` | `meta_ads_enabled` added to `SAFE_DEFAULTS` (default `false`) |

---

## Wiring remaining reserved events

```kotlin
// ChallengesScreen — join
MetaAnalytics.track(context, MetaAnalytics.Event.CHALLENGE_JOINED)

// ChallengesScreen — complete a day
MetaAnalytics.track(context, MetaAnalytics.Event.CHALLENGE_DAY_DONE)

// After first markReading() on a fresh install
MetaAnalytics.track(context, MetaAnalytics.Event.FIRST_VERSE_READ)
```

---

## Testing

1. Set `meta_ads_enabled: true` in the backend config (or temporarily hard-code
   `return true` in `MetaAnalytics.isEnabled()` for a local test build).
2. Use the **Meta Events Manager → Test Events** tab with your device's
   advertising ID to verify events arrive in real time.
3. Revert the hard-code before merging.


```

Event	                      Parameters
onboarding_completed	       tradition (e.g. "Hinduism"), language_id
daily_reminder_enabled	       source ("reminders")
verse_saved                    scripture  (e.g. "Bhagavad Gita"), saved_count
mood_checked_in	               mood  (e.g. "anxious")
explain_opened	               scripture
story_opened	               scripture
share_studio_opened	           scripture
```