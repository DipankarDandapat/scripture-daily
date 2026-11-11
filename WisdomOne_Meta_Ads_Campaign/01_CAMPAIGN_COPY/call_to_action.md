# WisdomOne — Call-to-Action Buttons

Meta App Promotion ads use a fixed list of CTA buttons; the exact list depends on the campaign set-up and changes over
time, so confirm in Ads Manager. The button always opens the Play listing for `com.scripturedaily` (install campaigns)
or the app (re-engagement campaigns).

## Recommended buttons

| CTA button | Role | When to use | Notes |
|---|---|---|---|
| **Install Now** | Primary | Default for every app-install ad | Clearest, most literal instruction for an install objective. Used by 24 of 25 launch ads. |
| **Download** | Secondary | Where the creative is about getting something (wallpapers, verses) rather than "an app" | Used by `AS03_AD01_wallpapers_with_meaning`, whose message is a wallpaper gallery. Useful as a soft variant when refreshing a fatigued Install Now ad. |
| **Use App** | Later — re-engagement | App Promotion campaigns aimed at people who already installed (e.g. installed but not activated) | Not part of this campaign. The app has no deep links today, so "Use App" can only open the Home screen; add app links first (see `tracking_strategy.md`). Needs an app-activity custom audience, which needs clean SDK events. |
| Learn More | Not recommended | — | Softer intent; for an install objective it tends to add a step without adding information. |
| Play Game, Shop Now, Book Now, etc. | Do not use | — | Do not describe WisdomOne; mismatched CTAs can hurt trust and may be rejected. |

## Launch assignment (from the spec)

| Ad set | Ads with **Install Now** | Ads with **Download** |
|---|---|---|
| AS01 \| Daily Wisdom | `AS01_AD01_start_your_day`, `AS01_AD02_little_wisdom`, `AS01_AD03_moment_of_peace`, `AS01_AD04_words_that_inspire`, `AS01_AD05_daily_dose` | — |
| AS02 \| Lock Screen Verse | `AS02_AD01_lockscreen_wisdom`, `AS02_AD02_every_unlock`, `AS02_AD03_favourite_verses`, `AS02_AD04_beautiful_lockscreen`, `AS02_AD05_part_of_everyday` | — |
| AS03 \| Wallpaper Experience | `AS03_AD02_screen_inspiration`, `AS03_AD03_visual_inspiration`, `AS03_AD04_faith_meets_design`, `AS03_AD05_personalize_everyday` | `AS03_AD01_wallpapers_with_meaning` |
| AS04 \| Daily Spiritual Routine | `AS04_AD01_make_time`, `AS04_AD02_gentle_reminder`, `AS04_AD03_one_verse_one_moment`, `AS04_AD04_stay_connected`, `AS04_AD05_small_moments` | — |
| AS05 \| Premium WisdomOne | `AS05_AD01_meet_wisdomone`, `AS05_AD02_more_than_a_verse`, `AS05_AD03_beautiful_space`, `AS05_AD04_travels_with_you`, `AS05_AD05_better_experience` | — |

## Consistency rules

- **In-image buttons must match the action.** Two images carry a drawn button: `AS01_AD05_daily_dose`
  ("Install free on Google Play") and `AS05_AD05_better_experience` ("Install WisdomOne free"). Keep the Meta button on
  these ads as **Install Now**.
- **Copy and button should agree.** Primary text that ends "Install WisdomOne free…" pairs with Install Now; copy about
  wallpapers pairs naturally with Download.
- **"Free" is accurate** — the app has no purchases. Never add price, discount or "limited time" language.
- **Google Play branding:** if a "Get it on Google Play" badge appears in any creative, use Google's official badge and
  follow Google Play's brand guidelines; do not imitate it with a home-made pill.
- **CTA tests:** to learn whether Download beats Install Now, duplicate one strong ad and change **only** the button
  (Meta A/B test). Comparing `AS03_AD01` with other ads is not a clean CTA test, because the creative differs too.
