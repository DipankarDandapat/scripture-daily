# WisdomOne — Creative Diversification Strategy

The 25 ads are built to be **genuinely different**, not 25 resizes of one idea. Each ad changes several of: hook,
headline, composition, background, phone position, typography scale, palette, emotion, featured feature and CTA.
All values in the matrices come from `tools/marketing/campaign_spec.py`.

---

## 1. Why diversification matters on Meta now

- **Creative is the targeting.** With broad or Advantage+ audiences, Meta's ad-retrieval and ranking system (which
  Meta has described under the name *Andromeda*) decides who sees which ad largely from the ad itself and early
  engagement. Different hooks and visuals reach different pockets of people; near-identical ads compete for the same
  pocket.
- **Near-duplicates are wasted slots.** Meta has indicated that very similar creatives can be treated as one for
  delivery. Five truly distinct ads per ad set give the system five real options.
- **Fatigue spreads slower.** When one concept tires, others with a different look and message can carry delivery.
- **Learning is clearer.** Distinct concepts produce a clearer read of *which idea* works, not which crop.

---

## 2. Message matrix (from the spec)

| Ad | Hook | Headline | Feature | Emotion | Description | CTA | Video |
|---|---|---|---|---|---|---|---|
| `AS01_AD01_start_your_day` | Morning ritual | Start Your Day With Wisdom | Daily Bible verse | Hopeful, fresh | A daily Bible verse | Install Now | Video |
| `AS01_AD02_little_wisdom` | Tiny daily habit | A Little Wisdom Every Day | Daily verse habit | Calm, simple | One verse. Every day. | Install Now | — |
| `AS01_AD03_moment_of_peace` | Pause and breathe | Find a Moment of Peace | Daily verse + calm design | Peaceful, emotional | Your daily moment of calm | Install Now | — |
| `AS01_AD04_words_that_inspire` | Meaningful words | Words That Inspire Your Day | Verse card + Explain like I'm 5 | Inspired | Verses that stay with you | Install Now | — |
| `AS01_AD05_daily_dose` | Direct install | Your Daily Dose of Wisdom | Daily verse + Wisdom Tree | Confident | Free daily Bible verses | Install Now | — |
| `AS02_AD01_lockscreen_wisdom` | Feature reveal | Wisdom on Your Lock Screen | Lock-screen verse wallpaper | Wonder | Verses on your lock screen | Install Now | — |
| `AS02_AD02_every_unlock` | Unlock moment | Inspiration Every Time You Unlock | Automatic verse refresh | Uplifting | New verse, automatically | Install Now | Video |
| `AS02_AD03_favourite_verses` | Personal & close | Your Verses, Always Close | Personalised lock screen (artwork, theme, text) | Warm, personal | Personal. Beautiful. Yours. | Install Now | — |
| `AS02_AD04_beautiful_lockscreen` | Visual showcase | A Beautiful Lock Screen | Artwork + verse lock screens | Delight | 20 artworks + colour themes | Install Now | — |
| `AS02_AD05_part_of_everyday` | Before / after | Let Wisdom Into Your Everyday | Lock-screen verse (before vs after) | Reflective | See the difference | Install Now | — |
| `AS03_AD01_wallpapers_with_meaning` | Gallery | Wallpapers With Meaning | Verse wallpapers | Admiration | Art + scripture | Download | — |
| `AS03_AD02_screen_inspiration` | Transformation | Turn Your Screen Into Inspiration | Live wallpaper (time-of-day themes) | Inspired | Live verse wallpaper | Install Now | Video |
| `AS03_AD03_visual_inspiration` | Personalisation | New Visual Inspiration | Wallpaper gallery + colour themes | Creative | Design your wallpaper | Install Now | — |
| `AS03_AD04_faith_meets_design` | Premium design | Faith Meets Beautiful Design | Premium wallpaper artwork | Awe | Designed for calm | Install Now | — |
| `AS03_AD05_personalize_everyday` | Options | Make Every Screen Yours | Colour themes | Playful, personal | Themes for every mood | Install Now | — |
| `AS04_AD01_make_time` | Morning & evening | Make Time for Daily Wisdom | Verse refresh schedule (1–24 h) | Grounded | On your schedule | Install Now | — |
| `AS04_AD02_gentle_reminder` | Gentle nudge | A Gentle Reminder to Pause | Verse reminders | Cared for | Gentle verse reminders | Install Now | — |
| `AS04_AD03_one_verse_one_moment` | Rhythm | One Verse. One Moment. Every Day. | Daily verse | Simple, sincere | Simple daily habit | Install Now | — |
| `AS04_AD04_stay_connected` | Stay connected | Stay Connected Every Day | Reminder + lock-screen verse | Reassuring | Daily verse, delivered | Install Now | Video |
| `AS04_AD05_small_moments` | Progress | Small Moments, Meaningful Days | Wisdom Tree (reading habit) | Encouraged | Grow your Wisdom Tree | Install Now | — |
| `AS05_AD01_meet_wisdomone` | Introduction | Meet WisdomOne | App overview | Curious | Your daily verse app | Install Now | Video |
| `AS05_AD02_more_than_a_verse` | Feature depth | More Than Just a Bible Verse | Feature list | Impressed | Everything in one app | Install Now | — |
| `AS05_AD03_beautiful_space` | Premium UI | A Beautiful Space for Wisdom | Dark theme / premium UI | Serene | Calm, crafted design | Install Now | — |
| `AS05_AD04_travels_with_you` | Everywhere | Wisdom That Travels With You | Lock screen + live wallpaper | Companionship | Lock + home screen verse | Install Now | — |
| `AS05_AD05_better_experience` | Strong install | Discover WisdomOne | Complete experience | Motivated | Install free today | Install Now | — |

---

## 3. Visual matrix

**Style** and **palette** come from the spec; composition, background, phone set-up and typography are taken from the
composition functions in `tools/marketing/make_meta_ads.py` (one function per style). Every phone screen is a real app
screenshot or a wallpaper rendered by the app's own `LockScreenVerse` / live-wallpaper code. Lock renders are named as
in the spec (`img03` = `render_lock_img03`). In most styles the spec's **hook** appears as a small eyebrow line above
the headline.

Palette tone (from `tools/marketing/wo_kit.py`): **light** = `dawn`, `lavender`, `sand`, `sage`, `ocean`;
**dark** = `plum`, `night`, `midnight`; **warm gradient** = `sunset`.

| Ad | Style | Palette (tone) | Composition | Background | Phones / screens | Headline typography |
|---|---|---|---|---|---|---|
| `AS01_AD01` | `hero_dawn` | dawn (light) | Classic hero: headline over a layered phone pair | Sunrise gradient | Lock screen (`img12`) tilted behind the Home screen | Palatino Bold Italic |
| `AS01_AD02` | `typography` | sand (light) | Type-first: a catalogue verse (Lamentations 3:23) set large with its reference, headline below | Warm parchment gradient | One small tilted lock screen (`img04`) bleeding off the bottom-right edge | Didot verse + Palatino Bold Italic headline |
| `AS01_AD03` | `cinematic` | sunset (warm) | Film still: letterbox bars, centred line, phone rising from the bottom | Full-bleed app artwork #3 with dark veil | One lock screen | Baskerville Italic, centred |
| `AS01_AD04` | `verse_card` | plum (dark) | The real Home verse card, enlarged, in front of a lock screen | Plum gradient | Lock screen (`img16`) + enlarged verse card | Palatino Bold Italic |
| `AS01_AD05` | `minimal_cta` | lavender (light) | Minimal layout with an "Install free on Google Play" button | Lavender gradient | Lock screen (`img08`) behind Home with the Wisdom Tree card | Palatino Bold Italic |
| `AS02_AD01` | `lock_fullbleed` | night (dark) | Frameless: the complete lock screen as a glowing card; "Real WisdomOne lock screen" tag | Blurred, darkened copy of the same lock screen | One frameless lock screen (`img03`) with gold halo | Palatino Bold Italic |
| `AS02_AD02` | `unlock_hero` | midnight (dark) | One full-size phone; chips "No need to open the app" / "Refreshes automatically" | Night sky | One large lock screen (`img18`), bleeding off the frame | Palatino Bold Italic |
| `AS02_AD03` | `duo_personal` | dawn (light) | Personalisation pair | Sunrise gradient | Wallpaper gallery screen behind a lock screen (`img05`) | Palatino Bold Italic |
| `AS02_AD04` | `fan3` | lavender (light) | Three lock screens fanned | Lavender gradient | `img14`, `img06`, royal purple colour theme | Palatino Bold Italic |
| `AS02_AD05` | `before_after` | sage (light) | Two phones labelled "Before" and "With WisdomOne" (a plain lock screen vs the verse lock screen — no people) | Soft green gradient | Plain default lock screen + lock screen (`img10`), upright | Palatino Bold Italic |
| `AS03_AD01` | `wall_grid` | plum (dark) | Gallery wall of framed wallpaper tiles (2 × 2; one row of 4 in 1:1) | Plum gradient | Four wallpaper tiles: `img02`, `img07`, `img15`, `img19` | Palatino Bold Italic, centred |
| `AS03_AD02` | `day_night` | ocean (light) | Live wallpaper "Morning" vs "Night" | Sky-blue to indigo gradient | Two home screens with the live wallpaper | Palatino Bold Italic |
| `AS03_AD03` | `split_gallery` | lavender (light) | Designer screen + result; chips "Artwork / Colour themes / Live wallpaper" | Lavender gradient | Lock-screen designer screen + golden colour-theme lock screen | Palatino Bold Italic |
| `AS03_AD04` | `cinematic` | midnight (dark) | Film still (as AS01_AD03, different art and message) | Full-bleed app artwork #18 with dark veil | One lock screen | Baskerville Italic, centred |
| `AS03_AD05` | `fan3` | sand (light) | Three colour themes fanned | Warm sand gradient | Golden, ocean blue, forest green colour-theme lock screens | Palatino Bold Italic |
| `AS04_AD01` | `day_night_routine` | dawn (light) | "Morning" / "Evening" pair with the 1 / 6 / 12 / 24 h schedule | Sunrise gradient | Live-wallpaper home screen + lock screen (`img11`) | Palatino Bold Italic |
| `AS04_AD02` | `notifications` | sage (light) | Floating stack of verse notifications (Lamentations 3:23, Psalms 46:10) | App artwork #10 with green veil | Notification cards + lock screen showing the notification (`img13`) | Palatino Bold Italic |
| `AS04_AD03` | `typography_night` | night (dark) | Type-first, headline set large | Night sky | One small tilted lock screen (`img03`) | Didot Bold |
| `AS04_AD04` | `duo_reminder` | ocean (light) | Reminder pair | Light-blue gradient | Real reminder-notification screen + lock screen with notification (`img09`) | Palatino Bold Italic |
| `AS04_AD05` | `tree_focus` | dawn (light) | Wisdom Tree card magnified over its Home screen | Sunrise gradient | Home with tree + lock screen (`img20`) + enlarged tree card | Palatino Bold Italic |
| `AS05_AD01` | `product_grid` | lavender (light) | Product collage, staggered row of four phones | Lavender gradient | Home verse, lock screen (`img08`), Home with tree, live wallpaper (evening) | Palatino Bold Italic, centred |
| `AS05_AD02` | `feature_list` | plum (dark) | Six-item feature checklist with gold ticks beside a phone | Plum gradient | Lock screen (`img17`); Home with tree in 1:1 | Palatino Bold Italic |
| `AS05_AD03` | `split_dark` | midnight (dark) | Dark-theme showcase | Night sky | Dark Settings screen + midnight colour-theme lock screen | Didot Italic |
| `AS05_AD04` | `duo_ambient` | sunset (warm) | Lock screen + home screen: "the verse with you all day" | App artwork #1 with veil | Lock screen (`img07`) + live wallpaper (afternoon) home screen | Palatino Bold Italic |
| `AS05_AD05` | `install_hero` | dawn (light) | Direct response: "Free to install" eyebrow, "Install WisdomOne free" button | Approved Settings landscape fading to cream | Lock screen (`img12`) behind Home with tree | Palatino Bold Italic |

All 9:16 versions keep key text out of the Stories / Reels interface zones (top 13 % and bottom 22 % of the frame),
as set in the generator.

### Diversity checks

| Dimension | Coverage |
|---|---|
| Styles | 23 distinct styles across 25 ads. Only `cinematic` (AS01_AD03, AS03_AD04) and `fan3` (AS02_AD04, AS03_AD05) repeat, each time in a different ad set, palette and content. |
| Palettes | 9 palettes. Each ad set uses 5 different palettes, except AS04, which uses `dawn` twice (AD01, AD05) with unrelated compositions. |
| Light vs dark | 15 light, 8 dark, 2 warm-gradient — every ad set has at least one dark and one light execution. |
| Phone set-ups | One phone, frameless card, two phones, three fanned, four-up grid / row, and type-first layouts all appear. |
| Hooks | 25 different hooks; no two ads open on the same idea. |
| Headline faces | Palatino Bold Italic (most), Baskerville Italic (cinematic), Didot (type-first and dark showcase). |
| Features | Daily verse, Explain like I'm 5, Wisdom Tree, lock-screen verse, auto refresh, personalisation, artworks, colour themes, live wallpaper, refresh schedule, reminders, dark theme, mood check-in (AS05_AD02 copy). |
| Emotions | 24 distinct emotion labels across 25 ads (only "Inspired" repeats: AS01_AD04, AS03_AD02), from calm to motivated. |
| CTA | 24 × Install Now, 1 × Download (`AS03_AD01`). |
| Motion | 5 videos (one per ad set) + 20 static ads. |

---

## 4. Testing approach

| Level | Question | How to read it |
|---|---|---|
| Ad set (angle) | Which promise attracts the best users? | Compare ad sets on CPI, then cost per activated user and Day 7 retention. |
| Ad (execution) | Which execution of an angle works? | Compare ads within the same ad set only. |
| Asset / placement | Which format works where? | Ads Manager breakdowns by placement (and by image / video where reported). |
| Motion vs static | Do videos add value? | In each ad set, compare the video ad with the static ads — directionally only, since the creative also differs. |

**Rules**

- Launch all five ads per ad set together so none starts with an unfair head start.
- Do not judge an ad on a handful of installs. Meta concentrates spend on early favourites; an ad with little spend is
  *unproven*, not a loser. Give under-delivered ads a fair test later (for example in a fresh ad set) if their concept
  matters.
- Pause an ad only when it has spent meaningfully and is clearly worse on CPI **and** shows no activation advantage.
- Keep at least three live ads per ad set.

---

## 5. Creative refresh cadence

Refresh on **signals**, not on a fixed calendar. Check weekly; act when two or more of these appear on an ad that had
been performing:

- Frequency rising while CTR falls.
- CPI rising for several consecutive days without a matching CPM rise (CPM rises point to auction pressure instead).
- Spend shifting away from the ad without a replacement improving results.

**Refresh options, in order of effort**

1. New hook / headline on the same visual (copy from `01_CAMPAIGN_COPY/`).
2. Same composition, different lock-screen artwork, colour theme or verse (verses only from the app catalogue).
3. New cut of an existing video (different first two seconds).
4. New concept on a winning angle (add to the spec as a new ad number; regenerate assets).

---

## 6. Reading results — checklist

- [ ] Compare like with like: same date range, same optimisation event, attribution setting noted.
- [ ] Look past CPI: activation rate and Day 7 retention per ad set.
- [ ] Check the placement breakdown before concluding a creative is weak — it may only be weak in one placement.
- [ ] Record the winner **and why** (hook? lock-screen visual? motion?) so the next iteration changes one thing.
- [ ] Feed learnings back into `campaign_spec.py` — the spec stays the single source of truth.
