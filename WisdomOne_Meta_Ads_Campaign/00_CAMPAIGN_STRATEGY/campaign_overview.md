# WisdomOne — Meta App Promotion Campaign Overview

| Item | Value |
|---|---|
| Campaign name | `WisdomOne \| App Promotion \| Creative Diversification` |
| Channels | Facebook and Instagram (Meta Audience Network is optional — see `campaign_structure.md`) |
| App | WisdomOne — Android, package `com.scripturedaily` (version 1.4.2, versionCode 22 in `android/app/build.gradle.kts`) |
| Store destination | https://play.google.com/store/apps/details?id=com.scripturedaily |
| Price | Free (no in-app purchase or subscription exists in the code) |
| Structure | 1 campaign · 5 ad sets · 25 ads · 75 images (4:5, 1:1, 9:16) · 5 videos |
| Source of truth | `tools/marketing/campaign_spec.py` — names, copy, CTAs and filenames in every document come from this file |

> The Play listing must be **live, public in every target country and verified** (package name, title, screenshots and
> privacy policy) before any ad is published. Ads that send people to an unavailable or mismatched listing waste spend
> and can be rejected.

---

## 1. Goal

Acquire Android users who are likely to adopt a **daily-verse habit**, measured in this order:

1. **Installs** at an acceptable cost per install (CPI).
2. **Activation** — the new user finishes onboarding and reads a verse (`onboarding_completed` / `first_verse_read`).
3. **Retention** — the user comes back on Day 1 and Day 7.

The campaign is also a structured creative test: five angles, five executions each.

| Ad set | Angle (from the spec) | Lead feature |
|---|---|---|
| AS01 \| Daily Wisdom | Daily inspiration and Bible verses | Daily Bible verse |
| AS02 \| Lock Screen Verse | Make wisdom part of your everyday phone experience | Lock-screen verse wallpaper |
| AS03 \| Wallpaper Experience | Beautiful Bible verse wallpapers | Artwork / colour-theme wallpapers, live wallpaper |
| AS04 \| Daily Spiritual Routine | Build a consistent daily spiritual routine | Refresh schedule, reminders, Wisdom Tree |
| AS05 \| Premium WisdomOne | All-in-one premium experience | Whole-app overview and design quality |

"Premium" in AS05 describes **design quality**, not a paid tier. The app is free; no ad may imply a subscription,
upgrade or paid version.

---

## 2. Objective recommendation

**Use the Meta App Promotion objective.** It is the only objective that optimises for app installs and in-app events
reported by the Meta SDK and sends people straight to the Play listing.

Inside App Promotion there are two set-ups. Ads Manager labels and defaults change frequently, so confirm what the
account shows at build time.

| | Advantage+ app campaign | Manual app campaign |
|---|---|---|
| Who decides audience / placements / creative mix | Meta's automation, with few inputs | You, per ad set (Advantage+ audience and placements can still be switched on) |
| Ad-set structure | Minimal or none — creatives are pooled | Multiple ad sets, each with its own budget and creatives |
| Readability of the five angles | Low — results are reported mainly per ad, with spend concentrated where Meta chooses | High — each angle has its own ad set, budget and learning |
| Data needed to work well | Higher — relies on verified SDK events and volume | Works from day one with install optimisation |
| Best use here | Scaling proven creatives (Phase 3+) | Launch and learning (Phase 1–2) |

**Recommendation**

1. **Phase 1–2: manual app campaign** with the five ad sets in the spec. Within each ad set use broad delivery or
   Advantage+ audience and Advantage+ placements, so the only deliberate variable between ad sets is the creative angle.
   This keeps the "Creative Diversification" test readable.
2. **Phase 3: Advantage+ app campaign** built from the winning ads (and fresh iterations of the winning angles), once
   SDK events are verified in Events Manager and install volume is steady. It can run alongside the manual campaign
   while results are compared, with care to avoid the two competing for the same budget without a plan.

---

## 3. Optimisation event

| Stage | Optimise for | Condition to move on |
|---|---|---|
| Launch | **App installs** | Default. Needs only a working SDK install signal and the live listing. |
| Later | **An in-app activation event** — `onboarding_completed`, or `first_verse_read` once it is wired | All of the following are true (below) |

Move to an in-app event only when:

- The event arrives reliably in **Events Manager → Test Events** and then in live data for the app.
- The event is **offered as an optimisation event** for the app in Ads Manager (custom-event availability varies; see
  `tracking_strategy.md` for logging a standard event alongside the custom one).
- Volume is high enough. Meta's general learning-phase guidance is roughly **50 optimisation events per ad set per
  week**; below that, delivery tends to stay unstable. If one ad set cannot reach that, optimise the strongest ad set(s)
  only or consolidate.
- Sensitive parameters have been removed from the events (see `tracking_strategy.md` — `tradition` and `scripture`
  parameters are currently sent).

Current code state: `onboarding_completed` is wired; **`first_verse_read` is defined but never fired**. Value
optimisation (ROAS) does not apply — the app has no purchase event.

---

## 4. Prerequisites checklist

Status legend: **In code** = found in the repository · **To do** · **Verify** = cannot be confirmed from the repository.

| # | Prerequisite | Status | Notes |
|---|---|---|---|
| 1 | Play listing for `com.scripturedaily` live and public in target countries | Verify | Must be confirmed before launch. |
| 2 | Meta app registered in Meta for Developers with the Android platform: package `com.scripturedaily`, default class `com.scripturedaily.MainActivity`, release + Play App Signing key hashes, Play Store selected as store | Verify | Done in the Meta App Dashboard, not in code. |
| 3 | Meta app in **Live** mode and linked to the ad account / Business portfolio | Verify | |
| 4 | `facebook_app_id` and `facebook_client_token` in `android/app/src/main/res/values/strings.xml` | In code | Values are present. The App **Secret** must never be put in the app. |
| 5 | `com.facebook.android:facebook-core:19.0.0` dependency | In code | `android/app/build.gradle.kts`. |
| 6 | Manifest meta-data: `ApplicationId`, `ClientToken`, `AutoInitEnabled=true`, `AutoLogAppEventsEnabled=true` | In code | Auto-logging is **not** gated by the backend flag — see `tracking_strategy.md`. |
| 7 | `meta_ads_enabled` switched to `true` in the backend config | To do | Defaults to `false` in `backend/app/content/app_config.py`; until it is `true` every `MetaAnalytics.track()` call is dropped. Change via `PUT /api/v1/admin/config/meta_ads_enabled` (`docs/ADMIN_CONFIG_API.md`). |
| 8 | Remove `tradition` / `scripture` parameters from Meta events | To do | They reveal religion; Meta's terms prohibit sending sensitive data. Must be fixed before the flag is turned on. |
| 9 | Wire `first_verse_read` (if it will be used for activation / optimisation) | To do | Constant exists, no call site. |
| 10 | Events verified in **Events Manager → Test Events** on a release build | To do | Install, app activation, `onboarding_completed` at minimum. |
| 11 | Privacy policy hosted at a public URL and set in the Play listing and Meta App Dashboard | Verify | `docs/privacy-policy.html` exists. |
| 12 | Privacy policy updated to disclose the Meta SDK (and AdMob) | To do | It currently lists only Firebase, Google Play and hosting, and states the app does not use data for targeted advertising. |
| 13 | Play Console **Data safety** form updated for the Meta SDK (device / advertising ID, app interactions, data shared for analytics and advertising) | To do | |
| 14 | Consent approach for the Meta SDK decided (EEA/UK consent, US state "Limited Data Use") | To do | UMP consent exists for AdMob only. |
| 15 | Advertising ID permission (`com.google.android.gms.permission.AD_ID`) in the **release** merged manifest | Verify | Present in the debug merged manifest via dependencies. |
| 16 | Facebook Page and Instagram account connected as the ad identity; ad account payment method; Business verification if requested | Verify | |
| 17 | Creatives and copy checked against Meta Advertising Standards (personal attributes, no before/after claims about people) | To do | See `audience_strategy.md` §1. |
| 18 | Official "Get it on Google Play" badge used if a Play badge appears in creatives | Verify | The Play creatives README notes a text stand-in. |
| — | SKAdNetwork, Aggregated Event Measurement for iOS, App Tracking Transparency prompt | Not applicable | iOS-only. WisdomOne is Android-only. |

---

## 5. Phases

Durations are indicative. Move phases on **event volume and stability**, not on the calendar.

| Phase | Purpose | Key actions | Exit criteria |
|---|---|---|---|
| 0 · Readiness | Tracking and compliance | Complete the checklist; fix event parameters; switch on `meta_ads_enabled`; verify events | Install + activation events visible in Events Manager from a release build |
| 1 · Launch & learn | Baseline per angle | Manual app campaign, 5 ad sets × 5 ads, optimise for installs; avoid edits during the learning phase | Each ad set has exited learning, or has spent enough to compare CPI with confidence |
| 2 · Read & prune | Find winning angles and executions | Compare ad sets (angle) and ads (execution); pause clear under-performers but keep ≥ 3 ads per ad set; rebalance budget | A ranked view of angles by CPI **and** cost per activated user |
| 3 · Deepen & scale | Optimise for quality | Test optimisation for an activation event; build an Advantage+ app campaign from winners | Stable activation volume; cost per activated user known |
| 4 · Refresh | Fight fatigue | New iterations of winning angles (new hooks, new lock-screen artworks, new video cuts); replace fatigued ads | Ongoing |

---

## 6. KPIs

No targets are set here — Phase 1 establishes the baselines. Definitions and formulas are in `tracking_strategy.md`.

| Tier | KPI | Why it matters |
|---|---|---|
| Primary | App installs, cost per install (CPI) | Launch optimisation goal |
| Primary | Install conversion rate (installs ÷ store clicks) | Shows whether the Play listing continues the ad's promise |
| Quality | Cost per activated user, activation rate | Separates curious installers from real users |
| Quality | Day 1 and Day 7 retention | Habit formation — the product's core promise |
| Diagnostic | Impressions, reach, frequency, CPM | Delivery and fatigue |
| Diagnostic | CTR, CPC, store clicks | Creative pull |

---

## 7. Budget allocation considerations

No budget figures are recommended in this document. When setting budgets, consider:

- **Learning volume.** A useful rule of thumb is that each ad set's weekly budget should cover roughly
  *expected CPI × 50* if it is to exit learning on installs. Estimate CPI from early data, not assumptions.
- **Five ad sets fragment spend.** If the total budget cannot support five ad sets at learning volume, run fewer ad sets
  at a time (for example AS01, AS02, AS05 first) or merge angles, rather than starving all five.
- **Fair angle test.** Ad-set budgets (equal per ad set) give the cleanest angle comparison in Phase 1. Campaign budget
  (Advantage campaign budget) suits Phase 2+, when the goal shifts from learning to efficiency.
- **Change gradually.** Large or frequent budget edits can send an ad set back into learning.
- **Reserve for refresh.** Keep part of the budget for new creative iterations in Phase 4.

---

## 8. Document map

| File | Contents |
|---|---|
| `campaign_structure.md` | Campaign → ad set → ad table, naming, formats, placements |
| `audience_strategy.md` | Policy compliance and audience hypotheses per ad set |
| `creative_diversification_strategy.md` | How the 25 ads differ, testing and refresh cadence |
| `tracking_strategy.md` | Metrics, formulas, current SDK integration and gaps |
| `../01_CAMPAIGN_COPY/` | Campaign-level primary texts, headlines, descriptions and CTAs |
| `../07_BRAND_ASSETS/` | Brand guidelines, logos, fonts |
