# WisdomOne — Audience Strategy

> **Everything in this document is a hypothesis to test, not a guaranteed targeting option.** Meta adds, merges and
> removes detailed-targeting options regularly, and availability differs by country and account. Check every option in
> Ads Manager at build time, and drop any option that is unavailable or touches a sensitive category.

---

## 1. Compliance first — religion is a sensitive category

WisdomOne is a daily Bible-verse app, so its ads sit next to one of Meta's most sensitive areas. These rules apply to
every ad set, every ad and every future iteration.

### 1.1 Targeting

- **Do not target, or try to infer, anyone's religious beliefs.** Meta removed detailed-targeting options that refer
  to religious practices and groups (along with other sensitive topics such as health, ethnicity, political
  affiliation and sexual orientation) in early 2022. Do not try to rebuild those audiences through proxies (for example
  stacking interests that stand in for a religion, or uploading lists segmented by religion).
- **Do not build custom or lookalike audiences from religion-revealing data.** The app currently sends `tradition`
  and `scripture` parameters with some Meta events (see `tracking_strategy.md`). Those parameters must be removed
  **before** any app-event audience or lookalike is created, and audiences must never be filtered on them.
- Religious content is **not** a Special Ad Category by itself (those cover credit, employment, housing and social /
  political issues). Do not select one unless an ad discusses a social issue, which none of the 25 ads do.
- **Age:** consider 18+ for all ad sets. The app is "not directed at children under 13" (privacy policy) and contains
  children's stories meant for parents to share; an adult audience avoids any ambiguity.

### 1.2 Copy and creative — Meta's "personal attributes" standard

Ads may describe the **product**; they must not assert or imply knowledge of the **reader's** personal attributes —
religion, beliefs, health (including mental health), financial status, and similar.

| Not allowed (implies the reader's attribute) | Allowed (describes the app) |
|---|---|
| "Are you Christian?" | "A daily Bible verse, beautifully presented." |
| "Meet other believers like you" | "WisdomOne brings you one verse every day." |
| "Struggling with your faith?" | "Find a moment of peace." |
| "Feeling anxious today? Read this verse." | "Busy day? Take thirty seconds." (`AS01_AD03`) |
| "Your faith deserves a better app" | "Faith Meets Beautiful Design" (`AS03_AD04` — about the app's design, not the reader) |

The spec's copy follows this rule ("copy describes the app, not the person"). Any new copy must be checked against the
same test: *would this sentence still be true if the reader held no religious belief?* If the sentence only works by
presuming the reader's belief or state of mind, rewrite it.

Also avoid:

- Before/after framing about **people** (the `before_after` style compares two lock screens, not a person — keep it that
  way).
- Claims of results ("transform your life"), rankings ("#1 Bible app") or price claims other than "free".
- Any implication of a paid or "premium" tier — AS05's "premium" means design quality only.

---

## 2. Shared settings (all ad sets)

| Setting | Hypothesis / recommendation |
|---|---|
| Locations | Countries where the Play listing is live and English creative fits. The English Bible catalogue (KJV) is what the ads show. Hindi and Spanish Bible catalogues exist in the app, but no localised creative exists yet. |
| Language | English (or leave open where Meta's language targeting would shrink reach too much — test). |
| Age | 18+ (see §1.1). No gender targeting. |
| Device | Android only (App Promotion with a Play Store app does this automatically). Consider an OS-version floor of Android 8.0 (`minSdk 26`). |
| Audience mode | Broad or Advantage+ audience by default. Detailed targeting, where used, as *suggestions* only. |
| Exclusions | Existing app users once an app-activity custom audience is available (Meta usually avoids showing install ads to people who already have the app — verify in delivery). |
| Placements | Advantage+ placements (see `campaign_structure.md` §4). |

---

## 3. Ad-set hypotheses

### AS01 | Daily Wisdom — broad delivery

| | |
|---|---|
| Hypothesis | With no audience inputs, Meta's delivery system can find people who respond to a simple "one verse a day" message more efficiently than manual targeting. AS01 acts as the **control** for the whole test. |
| Audience | Broad (country, language, age only) or Advantage+ audience with no suggestions. |
| Creative angle | Daily inspiration: morning ritual, tiny habit, moment of peace, meaningful words, direct install. |
| Suggested placements | Feeds (4:5) and Reels (V01 video on `AS01_AD01_start_your_day`). |
| Budget considerations | Equal share in Phase 1. As the control it should keep enough budget to stay out of learning even if other ad sets win. |
| Testing strategy | Five executions of one angle. Compare text-led (`typography`, `minimal_cta`) vs image-led (`hero_dawn`, `cinematic`) vs feature-led (`verse_card`). |
| Optimisation approach | Installs first; this ad set is the first candidate for an activation-event test because broad audiences usually build volume fastest. |

### AS02 | Lock Screen Verse — faith / inspirational interests (only where Meta allows)

| | |
|---|---|
| Hypothesis | People who already follow inspirational-quote or devotional-style content respond best to the most distinctive feature — a verse on the lock screen. |
| Audience | Broad with optional Advantage+ audience **suggestions** around general inspirational content (for example inspirational quotes, daily devotional-style publications, Bible-reading apps as a product category) **only if** Ads Manager offers them and they are not religion-identity options. If no such option exists, run AS02 broad — the creative itself does the selecting. |
| Creative angle | Lock-screen verse: feature reveal, unlock moment, personal lock screen, artwork showcase, before/after lock screen. |
| Suggested placements | Stories and Reels first (9:16 lock screens); V02 video on `AS02_AD02_every_unlock`. |
| Budget considerations | Equal share in Phase 1. If interest suggestions shrink the potential audience sharply, expect higher CPM; judge on cost per activated user, not CPI alone. |
| Testing strategy | Option: duplicate AS02 into a broad twin (same ads) for a short A/B test to learn whether the interest suggestions add anything. Use Meta's A/B test tool so audiences do not overlap. |
| Optimisation approach | Installs; then compare lock-screen ads' activation rate — this feature requires the user to complete onboarding and turn the lock-screen verse on, so activation is the real signal. |

### AS03 | Wallpaper Experience — wallpaper / personalisation interests

| | |
|---|---|
| Hypothesis | People interested in phone personalisation (wallpapers, home-screen customisation, aesthetic design, art and photography) are drawn by beautiful verse wallpapers even before they look for a verse app. |
| Audience | Advantage+ audience with suggestions around wallpapers, phone customisation, graphic design, art — if available. Non-religious interests, so lower policy risk. |
| Creative angle | Gallery of wallpapers, live wallpaper by time of day, design-your-own, premium artwork, colour themes. |
| Suggested placements | Instagram Feed, Explore and Reels; V03 video on `AS03_AD02_screen_inspiration`. |
| Budget considerations | Equal share in Phase 1. Expect possibly cheaper installs but weaker activation (some people want wallpapers, not a verse) — budget decisions must wait for activation data. |
| Testing strategy | `AS03_AD01_wallpapers_with_meaning` is the only `Download` CTA in the campaign — compare it informally with `Install Now` ads (it is not a clean CTA test because the creative differs too). |
| Optimisation approach | Installs; move to an activation event early if activation rate lags other ad sets. |

### AS04 | Daily Spiritual Routine — habit / self-improvement positioning

| | |
|---|---|
| Hypothesis | People interested in habits, routines, journaling, mindfulness or personal development respond to a routine framing (schedule, gentle reminders, Wisdom Tree progress). |
| Audience | Advantage+ audience with suggestions around habit building, productivity, journaling, mindfulness, self-improvement — if available. Do **not** use mental-health conditions as targeting or copy hooks. |
| Creative angle | Morning & evening rhythm, gentle reminder, one-verse ritual, staying connected, Wisdom Tree progress. |
| Suggested placements | Feeds and Stories; V04 video on `AS04_AD04_stay_connected`. |
| Budget considerations | Equal share in Phase 1. This angle is most likely to show its value in Day 7 retention rather than CPI, so give it time before cutting. |
| Testing strategy | Compare reminder-led (`notifications`, `duo_reminder`) vs progress-led (`tree_focus`) vs rhythm-led (`day_night_routine`, `typography_night`). |
| Optimisation approach | Installs; strongest candidate for optimising on `first_verse_read` / a retention-related event once that event is wired and volume allows. |

### AS05 | Premium WisdomOne — broad, premium positioning

| | |
|---|---|
| Hypothesis | A broad audience shown the **whole product** (lock screen, live wallpaper, reminders, Wisdom Tree, design quality) converts better than single-feature messages, because it answers "why this app?" in one ad. |
| Audience | Broad / Advantage+ audience with no suggestions (a second broad ad set, so AS01 vs AS05 compares single-message vs all-in-one creative on the same audience). |
| Creative angle | Introduction, feature depth, premium UI, wisdom everywhere, strong install. |
| Suggested placements | Reels (28 s V05 overview on `AS05_AD01_meet_wisdomone`) and Feeds. |
| Budget considerations | Equal share in Phase 1. If AS05 wins, its ads are the natural seed for a Phase 3 Advantage+ app campaign. |
| Testing strategy | Overview (`product_grid`, `feature_list`) vs mood (`split_dark`, `duo_ambient`) vs direct response (`install_hero`). |
| Optimisation approach | Installs, then activation. Watch overlap with AS01: both are broad; if they compete heavily, consider merging them into one broad ad set with ten ads in Phase 2. |

---

## 4. Testing rules

1. **One deliberate variable per comparison.** Ad sets differ by angle (and, for AS02–AS04, by optional audience
   suggestions). When an audience question matters, test it with Meta's A/B test tool rather than by eye.
2. **Read at the right level.** Angle → ad set metrics. Execution → ad metrics *within* an ad set.
3. **Judge on quality, not just CPI.** Rank ad sets by cost per activated user and Day 7 retention once the data exists.
4. **Expect overlap.** Broad ad sets in one campaign compete for similar people. Some overlap is normal; persistent
   high CPM across all ad sets is a sign to consolidate.
5. **Document every change** (date, what, why) so results can be interpreted later.

## 5. Later audiences (after tracking is fixed)

| Audience | Use | Condition |
|---|---|---|
| App-activity custom audience: installed, not activated | Re-engagement (`Use App`) | Needs deep links / re-engagement setup (none exist yet) and clean events |
| Lookalike / Advantage+ seed from activated users | Prospecting | Events must contain **no** religion-revealing parameters; check Meta's audience-creation restrictions for the account |
| Exclusion: all app users | Avoid paying for existing users | Available once the SDK reports app activity |
