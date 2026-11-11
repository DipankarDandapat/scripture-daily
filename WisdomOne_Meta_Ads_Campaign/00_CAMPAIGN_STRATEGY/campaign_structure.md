# WisdomOne — Campaign Structure

All names, headlines, styles, CTAs and filenames below are taken from `tools/marketing/campaign_spec.py`
(`ad_name()`, `image_files()`, `video_file()`). If the spec changes, regenerate the assets and update this table.

```
WisdomOne | App Promotion | Creative Diversification          (1 campaign · App Promotion objective)
├── AS01 | Daily Wisdom                 5 ads · 1 video
├── AS02 | Lock Screen Verse            5 ads · 1 video
├── AS03 | Wallpaper Experience         5 ads · 1 video
├── AS04 | Daily Spiritual Routine      5 ads · 1 video
└── AS05 | Premium WisdomOne            5 ads · 1 video
                                        = 25 ads · 75 images · 5 videos
```

## 1. Ad sets

| ID | Ad set name (Ads Manager) | Asset folder | Angle |
|---|---|---|---|
| AS01 | `AS01 \| Daily Wisdom` | `02_AD_SET_01_DAILY_WISDOM` | Daily inspiration and Bible verses |
| AS02 | `AS02 \| Lock Screen Verse` | `03_AD_SET_02_LOCKSCREEN_VERSE` | Make wisdom part of your everyday phone experience |
| AS03 | `AS03 \| Wallpaper Experience` | `04_AD_SET_03_WALLPAPER_EXPERIENCE` | Beautiful Bible verse wallpapers |
| AS04 | `AS04 \| Daily Spiritual Routine` | `05_AD_SET_04_DAILY_SPIRITUAL_ROUTINE` | Build a consistent daily spiritual routine |
| AS05 | `AS05 \| Premium WisdomOne` | `06_AD_SET_05_PREMIUM_WISDOMONE` | All-in-one premium experience |

Inside each ad-set folder, `images/` holds the 15 JPGs (5 ads × 3 formats) and `videos/` holds that ad set's MP4 plus
a storyboard contact sheet (`*_video_storyboard.jpg`, for review only — not for upload).

## 2. Ads (25)

Every ad has three images: 4:5 (1080 × 1350), 1:1 (1080 × 1080) and 9:16 (1080 × 1920). Headline length is shown
in brackets (Meta truncates long headlines in some placements; all are ≤ 33 characters).

| Ad set | Ad name | Headline | Style | Image formats | Video | CTA |
|---|---|---|---|---|---|---|
| AS01 | `AS01_AD01_start_your_day` | Start Your Day With Wisdom (26) | `hero_dawn` | 4:5, 1:1, 9:16 | Yes — `AD01_start_your_day_video.mp4` (18 s) | Install Now |
| AS01 | `AS01_AD02_little_wisdom` | A Little Wisdom Every Day (25) | `typography` | 4:5, 1:1, 9:16 | No | Install Now |
| AS01 | `AS01_AD03_moment_of_peace` | Find a Moment of Peace (22) | `cinematic` | 4:5, 1:1, 9:16 | No | Install Now |
| AS01 | `AS01_AD04_words_that_inspire` | Words That Inspire Your Day (27) | `verse_card` | 4:5, 1:1, 9:16 | No | Install Now |
| AS01 | `AS01_AD05_daily_dose` | Your Daily Dose of Wisdom (25) | `minimal_cta` | 4:5, 1:1, 9:16 | No | Install Now |
| AS02 | `AS02_AD01_lockscreen_wisdom` | Wisdom on Your Lock Screen (26) | `lock_fullbleed` | 4:5, 1:1, 9:16 | No | Install Now |
| AS02 | `AS02_AD02_every_unlock` | Inspiration Every Time You Unlock (33) | `unlock_hero` | 4:5, 1:1, 9:16 | Yes — `AD02_every_unlock_video.mp4` (17 s) | Install Now |
| AS02 | `AS02_AD03_favourite_verses` | Your Verses, Always Close (25) | `duo_personal` | 4:5, 1:1, 9:16 | No | Install Now |
| AS02 | `AS02_AD04_beautiful_lockscreen` | A Beautiful Lock Screen (23) | `fan3` | 4:5, 1:1, 9:16 | No | Install Now |
| AS02 | `AS02_AD05_part_of_everyday` | Let Wisdom Into Your Everyday (29) | `before_after` | 4:5, 1:1, 9:16 | No | Install Now |
| AS03 | `AS03_AD01_wallpapers_with_meaning` | Wallpapers With Meaning (23) | `wall_grid` | 4:5, 1:1, 9:16 | No | Download |
| AS03 | `AS03_AD02_screen_inspiration` | Turn Your Screen Into Inspiration (33) | `day_night` | 4:5, 1:1, 9:16 | Yes — `AD02_screen_inspiration_video.mp4` (23 s) | Install Now |
| AS03 | `AS03_AD03_visual_inspiration` | New Visual Inspiration (22) | `split_gallery` | 4:5, 1:1, 9:16 | No | Install Now |
| AS03 | `AS03_AD04_faith_meets_design` | Faith Meets Beautiful Design (28) | `cinematic` | 4:5, 1:1, 9:16 | No | Install Now |
| AS03 | `AS03_AD05_personalize_everyday` | Make Every Screen Yours (23) | `fan3` | 4:5, 1:1, 9:16 | No | Install Now |
| AS04 | `AS04_AD01_make_time` | Make Time for Daily Wisdom (26) | `day_night_routine` | 4:5, 1:1, 9:16 | No | Install Now |
| AS04 | `AS04_AD02_gentle_reminder` | A Gentle Reminder to Pause (26) | `notifications` | 4:5, 1:1, 9:16 | No | Install Now |
| AS04 | `AS04_AD03_one_verse_one_moment` | One Verse. One Moment. Every Day. (33) | `typography_night` | 4:5, 1:1, 9:16 | No | Install Now |
| AS04 | `AS04_AD04_stay_connected` | Stay Connected Every Day (24) | `duo_reminder` | 4:5, 1:1, 9:16 | Yes — `AD04_stay_connected_video.mp4` (18 s) | Install Now |
| AS04 | `AS04_AD05_small_moments` | Small Moments, Meaningful Days (30) | `tree_focus` | 4:5, 1:1, 9:16 | No | Install Now |
| AS05 | `AS05_AD01_meet_wisdomone` | Meet WisdomOne (14) | `product_grid` | 4:5, 1:1, 9:16 | Yes — `AD01_meet_wisdomone_video.mp4` (28 s) | Install Now |
| AS05 | `AS05_AD02_more_than_a_verse` | More Than Just a Bible Verse (28) | `feature_list` | 4:5, 1:1, 9:16 | No | Install Now |
| AS05 | `AS05_AD03_beautiful_space` | A Beautiful Space for Wisdom (28) | `split_dark` | 4:5, 1:1, 9:16 | No | Install Now |
| AS05 | `AS05_AD04_travels_with_you` | Wisdom That Travels With You (28) | `duo_ambient` | 4:5, 1:1, 9:16 | No | Install Now |
| AS05 | `AS05_AD05_better_experience` | Discover WisdomOne (18) | `install_hero` | 4:5, 1:1, 9:16 | No | Install Now |

### Videos

| Video ID (spec) | Attached to ad | File | Length |
|---|---|---|---|
| `V01_daily_wisdom` | `AS01_AD01_start_your_day` | `AD01_start_your_day_video.mp4` | 18 s |
| `V02_lockscreen_verse` | `AS02_AD02_every_unlock` | `AD02_every_unlock_video.mp4` | 17 s |
| `V03_wallpaper_experience` | `AS03_AD02_screen_inspiration` | `AD02_screen_inspiration_video.mp4` | 23 s |
| `V04_daily_routine` | `AS04_AD04_stay_connected` | `AD04_stay_connected_video.mp4` | 18 s |
| `V05_complete_experience` | `AS05_AD01_meet_wisdomone` | `AD01_meet_wisdomone_video.mp4` | 28 s |

All five videos are rendered by `tools/marketing/make_meta_videos.py` at **1080 × 1920 (9:16), H.264 / AAC, 30 fps**,
with original music generated by the script (no licence needed). Being vertical, they belong in Stories and Reels; in
Feeds, let the 4:5 image serve, or check Meta's crop preview before allowing the video there.

## 3. Naming convention

| Level | Pattern | Example |
|---|---|---|
| Campaign | `WisdomOne \| App Promotion \| <test name>` | `WisdomOne \| App Promotion \| Creative Diversification` |
| Ad set | `AS<nn> \| <Angle name>` | `AS02 \| Lock Screen Verse` |
| Ad | `AS<nn>_AD<nn>_<slug>` — `ad_name()` | `AS02_AD04_beautiful_lockscreen` |
| Image file | `<ad-set folder>/images/AD<nn>_<slug>_<4x5\|1x1\|9x16>.jpg` — `image_files()` | `03_AD_SET_02_LOCKSCREEN_VERSE/images/AD04_beautiful_lockscreen_9x16.jpg` |
| Video file | `<ad-set folder>/videos/AD<nn>_<slug>_video.mp4` — `video_file()` | `03_AD_SET_02_LOCKSCREEN_VERSE/videos/AD02_every_unlock_video.mp4` |

Rules:

- Image and video files omit the ad-set prefix; the ad-set folder supplies it. The **ad name in Ads Manager always
  includes it**, so reports can be filtered by `AS0n_`.
- Do not rename ads after launch — renaming breaks the link between reports, the master CSV and the asset files.
- New iterations get a new number and slug (for example `AS02_AD06_<slug>`) after the spec is updated; never reuse a
  number for different creative.
- If the same creative is later re-used in an Advantage+ app campaign, keep the identical ad name so results can be
  compared across campaigns.

## 4. Placements

**Default: Advantage+ placements** with placement asset customisation, uploading all three images to each ad so
Meta can serve the right shape:

| Asset | Intended placements |
|---|---|
| 4:5 image | Facebook Feed, Instagram Feed, Instagram Explore, Facebook Marketplace / video feeds |
| 1:1 image | Facebook right column, search results, in-stream, Audience Network (fallback for any square slot) |
| 9:16 image | Instagram Stories and Reels, Facebook Stories and Reels |
| Video (where assigned, 9:16) | Reels and Stories first; Feeds only if the crop preview looks right |

For the five ads with a video, use a single ad that carries the video and the images (placement customisation or a
flexible-media ad, whichever Ads Manager offers for App Promotion at build time), so the video is used where motion
performs and the images elsewhere.

**Placement emphasis per ad set (hypotheses — keep Advantage+ placements on; review the placement breakdown before
excluding anything):**

| Ad set | Emphasis | Reasoning |
|---|---|---|
| AS01 Daily Wisdom | Feeds (4:5) + Reels (video) | Verse-led, text-forward creative reads well in Feed; the morning video suits Reels. |
| AS02 Lock Screen Verse | Stories and Reels (9:16) | A full-height phone lock screen is closest to how the feature looks in real life. |
| AS03 Wallpaper Experience | Instagram Feed, Explore, Reels | Visual gallery / wallpaper content matches browsing-for-inspiration behaviour. |
| AS04 Daily Spiritual Routine | Feeds + Stories | Routine and reminder messages need a beat of reading; Stories show the notification stack at true scale. |
| AS05 Premium WisdomOne | Reels (28 s overview video) + Feeds | The whole-app overview needs either motion or a large 4:5 canvas. |

**Audience Network:** keep it on initially only if the Phase 1 breakdown shows it delivers activated users, not just
cheap installs. If installs from it show low activation, exclude it.

## 5. Build checklist per ad

- [ ] Ad name exactly as `ad_name()` in the table above.
- [ ] Identity: WisdomOne Facebook Page and Instagram account.
- [ ] Three images uploaded (4:5, 1:1, 9:16) and mapped to placements; video added where the table says **Yes**.
- [ ] Primary text, headline and description copied from the spec (or from the per-ad copy file generated from it).
- [ ] CTA as in the table (`Install Now`, or `Download` for `AS03_AD01_wallpapers_with_meaning`).
- [ ] App: WisdomOne (Google Play, `com.scripturedaily`).
- [ ] Preview checked in every placement: no headline or verse text hidden under Stories / Reels UI.
