# WisdomOne — Tracking Strategy

This document defines what to measure, where to read it, and what the app's Meta integration does today. The current
state was taken from the code; items that cannot be confirmed from the repository are marked **Verify**.

---

## 1. Metrics and formulas

| Metric | Definition / formula | Where to read it |
|---|---|---|
| Impressions | Times an ad was shown | Ads Manager |
| Reach | Unique accounts that saw an ad | Ads Manager |
| Frequency | Impressions ÷ Reach | Ads Manager |
| CPM | Amount spent ÷ Impressions × 1,000 | Ads Manager |
| CTR (link) | Link clicks ÷ Impressions × 100 | Ads Manager ("CTR (link click-through rate)") |
| CPC (link) | Amount spent ÷ Link clicks | Ads Manager |
| Play Store clicks | Link / outbound clicks that open the Play listing | Ads Manager (link clicks / outbound clicks on an App Promotion ad) |
| App installs | Installs attributed to the ads under the chosen attribution setting | Ads Manager (needs the SDK install signal) |
| Cost per install (CPI) | Amount spent ÷ App installs | Ads Manager |
| Install conversion rate | App installs ÷ Play Store clicks × 100 | Calculated from Ads Manager columns; compare with Play Console store-listing conversion (all sources) |
| Activated user | A new user who fires `onboarding_completed` **or** `first_verse_read` (count each user once) | Ads Manager app-event columns (Meta-attributed); Firebase for all users |
| Activation rate | Activated users ÷ App installs × 100 | Calculated |
| Cost per activated user | Amount spent ÷ Activated users | Calculated (Ads Manager "cost per app event" for the activation event) |
| Day 1 retention | Users active on day 1 after install ÷ Users who installed on day 0 × 100 | Firebase Analytics (retention / cohort exploration), Play Console acquisition reports |
| Day 7 retention | Users active on day 7 after install ÷ Users who installed on day 0 × 100 | As above |

Notes:

- **Today only `onboarding_completed` can mark activation** — `first_verse_read` is not fired anywhere in the code.
  Until it is wired, "activated user" = `onboarding_completed`.
- **Retention:** Facebook Analytics, which offered cohort retention, was retired in 2021. Events Manager shows event
  counts, not cohort retention. Use **Firebase Analytics** (the app includes `firebase-analytics`, which collects
  `first_open`, `session_start` and `user_engagement` automatically) and **Play Console** for Day 1 / Day 7 retention.
  These cover all users, not only Meta-attributed ones; to isolate the campaign, compare cohorts from launch days or
  use the Meta campaign source if it reaches Firebase (see §4).
- Meta counts installs by its attribution setting (click / view windows); Play Console and Firebase count installs
  differently. Expect the numbers to disagree and compare **trends**, not absolute totals.

---

## 2. Current integration (from the code)

| Area | Current state | Source |
|---|---|---|
| Meta SDK | `com.facebook.android:facebook-core:19.0.0` (core only — no login / share) | `android/app/build.gradle.kts` |
| Credentials | `facebook_app_id` and `facebook_client_token` string resources are filled in | `android/app/src/main/res/values/strings.xml` |
| SDK start-up | `AutoInitEnabled=true` and `AutoLogAppEventsEnabled=true` in the manifest; the merged manifest registers `FacebookInitProvider`, so the SDK initialises at process start | `AndroidManifest.xml`, build intermediates |
| App wrapper | `MetaAnalytics.init()` runs after the backend config loads; if `meta_ads_enabled` is true it calls `AppEventsLogger.activateApp()` and creates a logger | `analytics/MetaAnalytics.kt`, `WisdomOneApplication.kt` |
| Server switch | `meta_ads_enabled` defaults to **false** (backend and the app's safe defaults). While false, every `MetaAnalytics.track()` call is dropped | `backend/app/content/app_config.py`, `config/AppConfigManager.kt` |
| Install Referrer (Google Play) | Not declared directly, but `facebook-core` pulls in `com.android.installreferrer:installreferrer:2.2`, and the merged manifest contains `BIND_GET_INSTALL_REFERRER_SERVICE` | Gradle cache POM, merged manifest |
| Advertising ID | `com.google.android.gms.permission.AD_ID` is present in the **debug** merged manifest via dependencies | Merged manifest — **Verify** for release |
| Firebase | `firebase-analytics`, `firebase-crashlytics`, `firebase-messaging` (BoM 34.19.0). **No custom Firebase Analytics events** are logged in app code | `build.gradle.kts`, code search |
| Consent | Google UMP consent is used for **AdMob only**. No Meta-specific consent, Advertiser ID or data-processing controls are set | `ads/AdManager.kt`, code search |
| Purchases | None. The app is free; no billing library, no purchase event | Code search |
| Deep links | No `VIEW` intent filters — no deep links / app links for re-engagement ads | `AndroidManifest.xml` |

### Events

| Event | Type | Wired? | Parameters sent | Call site |
|---|---|---|---|---|
| App install / app activation (`fb_mobile_activate_app`) | Automatic (SDK) | Yes — via auto-logging and `activateApp()` | — | SDK |
| `onboarding_completed` | Custom | Yes | `tradition` (e.g. "Hinduism"), `language_id` | `MainActivity.kt` (onboarding finish) |
| `first_verse_read` | Custom | **No** — constant only | — | — |
| `daily_reminder_enabled` | Custom | Yes (2 places) | `source` | `MainActivity.kt` |
| `verse_saved` | Custom | Yes | `scripture` (e.g. "Bhagavad Gita") | `MainActivity.kt` |
| `mood_checked_in` | Custom | Yes | `mood` (e.g. "anxious") | `MainActivity.kt` |
| `explain_opened` | Custom | Yes | `scripture` | `MainActivity.kt` |
| `story_opened` | Custom | Yes | `scripture` | `MainActivity.kt` |
| `share_studio_opened` | Custom | Yes | `scripture` | `MainActivity.kt` |
| `challenge_joined` | Custom | **No** — constant only | — | — |
| `challenge_day_completed` | Custom | **No** — constant only | — | — |

No Meta **standard** events (for example Completed Tutorial / Complete Registration) are logged; all in-app events
are custom names.

---

## 3. Gaps found

| # | Gap | Impact | Recommendation |
|---|---|---|---|
| 1 | **Religion-revealing parameters are sent to Meta.** `onboarding_completed` sends `tradition`; `verse_saved`, `explain_opened`, `story_opened` and `share_studio_opened` send `scripture`. `mood_checked_in` sends a mood such as "anxious" (mental-health adjacent). This contradicts the privacy note in `docs/META_EVENTS.md`. | Meta's Business Tools Terms prohibit sending sensitive information such as religious beliefs or health. Meta may restrict or block the app's events, which would break optimisation and audiences. | Remove `tradition`, `scripture` and `mood` from Meta events (keep them in first-party analytics if needed) **before** `meta_ads_enabled` is switched on. |
| 2 | **The server flag does not gate everything.** With `AutoInitEnabled` and `AutoLogAppEventsEnabled` set to true in the manifest, the SDK is expected to initialise at start-up and auto-log install / activation events regardless of `meta_ads_enabled`. | Data may reach Meta before the flag is on and before any consent; `META_EVENTS.md` describes the flag as a full off switch. | Verify with Test Events while the flag is false. If events arrive, set `AutoLogAppEventsEnabled` (and optionally `AdvertiserIDCollectionEnabled`) to false in the manifest and enable them at runtime (`FacebookSdk.setAutoLogAppEventsEnabled(true)`, `setAdvertiserIDCollectionEnabled(true)`) inside `MetaAnalytics.init()` once the flag — and consent, where required — allow it. |
| 3 | **`first_verse_read` is never fired.** | The recommended activation / optimisation event does not exist yet. | Fire once per install after the first `markReading()` (guard with a persisted flag), as `docs/META_EVENTS.md` describes. |
| 4 | **No Meta consent / privacy controls.** UMP consent covers AdMob only; no `setDataProcessingOptions` (Limited Data Use) or consent-gated collection for the Meta SDK. | Compliance risk in the EEA / UK and US states with privacy laws. | Decide a consent model with legal review; gate Meta collection on it; set Limited Data Use where required. |
| 5 | **Privacy policy and Data safety form do not mention Meta.** The policy lists Firebase, Google Play and hosting only, and states the app does not use information for targeted advertising (AdMob is also not listed). | Play policy and Meta platform terms require accurate disclosure. | Update `docs/privacy-policy.html` and the Play Console Data safety form before launch. |
| 6 | **No standard events.** Only custom event names are logged. | Fewer optimisation choices; custom events may not be offered for optimisation. | Also log a standard event at onboarding completion (for example `AppEventsConstants.EVENT_NAME_COMPLETED_TUTORIAL`) and check which events Ads Manager offers. |
| 7 | **Meta Install Referrer not configured.** Google Play Install Referrer is available (transitively), but Meta's own Install Referrer (campaign data for installs from the Facebook / Instagram apps) needs set-up in the App Dashboard and, per Meta's documentation, package-visibility `<queries>` entries for the Facebook and Instagram apps. The merged manifest only queries WhatsApp packages. | Weaker attribution detail for installs that start in the Facebook / Instagram apps. | Follow Meta's Install Referrer guide for SDK 19; add the `<queries>` entries; verify in Events Manager. |
| 8 | **Firebase has no custom events and no link to Meta campaigns.** | Retention by campaign cannot be read in Firebase; activation cannot be cross-checked. | Log `onboarding_completed` / `first_verse_read` to Firebase too (same names, no sensitive parameters). |
| 9 | **Release build not verified.** AD_ID permission and Meta meta-data were checked in the debug merged manifest only. | Release could differ (manifest merging, shrinking — minify is currently off). | Check the release merged manifest and run Test Events on a release build installed from an internal-testing track. |
| 10 | **No deep links.** | A later re-engagement campaign (`Use App` CTA) can only open the home screen. | Add app links before any re-engagement campaign. |

Not applicable on Android: App Tracking Transparency, SKAdNetwork and iOS Aggregated Event Measurement.

---

## 4. Recommended next steps (in order)

1. **Strip sensitive parameters** from all Meta events (gap 1).
2. **Make the flag a real switch:** confirm auto-logging behaviour; move auto-log / Advertiser ID collection behind
   `meta_ads_enabled` and consent if needed (gaps 2 and 4).
3. **Wire `first_verse_read`** and add a standard-event twin for onboarding (gaps 3 and 6).
4. **Update the privacy policy and Data safety form** (gap 5).
5. **Configure Meta Install Referrer** and add the `<queries>` entries (gap 7).
6. **Mirror activation events to Firebase** for retention and cross-checking (gap 8). Keep the same event names in both
   systems. They are separate pipelines, so there is no double counting inside either tool — but never add Meta and
   Firebase counts together.
7. **Verify end to end on a release build:** Events Manager → Test Events (install, activation, `onboarding_completed`,
   `first_verse_read`), then switch `meta_ads_enabled` on through `PUT /api/v1/admin/config/meta_ads_enabled`.
8. **Use a Play `referrer` for non-ad links.** For organic Meta posts, bio links and other channels, append a
   `referrer` to the store URL so Firebase / Play can attribute them, e.g.
   `https://play.google.com/store/apps/details?id=com.scripturedaily&referrer=utm_source%3Dinstagram%26utm_medium%3Dsocial%26utm_campaign%3Dwisdomone_bio`.
   App-install ads take the store destination from the Meta app settings, so a custom referrer may not pass through
   them — verify before relying on it for paid traffic.

---

## 5. Reporting cadence

| When | What |
|---|---|
| Daily (first two weeks) | Delivery health: spend, CPM, CTR, installs, any disapprovals; event volume in Events Manager |
| Weekly | CPI and install conversion rate per ad set and ad; activation rate; frequency and fatigue signals |
| Weekly, from the second week | Day 1 and Day 7 retention cohorts (Firebase, Play Console) for launch-week installers |
| Phase change | Full review: angle ranking by cost per activated user and retention; decisions logged |
