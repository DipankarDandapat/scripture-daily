# Google AdMob Integration — WisdomOne

## Quick facts

| Item | Value |
|---|---|
| AdMob App ID | `ca-app-pub-7872455938809561~3577314080` |
| SDK | `com.google.android.gms:play-services-ads:23.6.0` |
| Consent | Google UMP (`com.google.android.ump:user-messaging-platform:3.1.0`) |
| Global kill switch | `ads_enabled` in backend DB |
| Test / prod toggle | `ads_use_test_ids` in backend DB |
| Debug builds | **Always** use Google test units — hardcoded, cannot be overridden by server |
| Admin token | `WisdomOne@2024` (from `backend/.env` → `SD_ADMIN_TOKEN`) |

---

## How the system works

```
Backend DB (app_config table)
        │
        ▼  GET /api/v1/config  (ETag-cached, refreshed on startup + foreground)
AppConfigManager  ──►  AdConfig.from(snapshot)
        │
        ▼
AdManager  ──►  UMP consent  ──►  MobileAds.initialize()
        │
        ▼
AdSlot (Compose)           ──►  Banner / Native  (automatic, inline)
AdManager.showInterstitial ──►  Interstitial      (preload then show)
AdManager.loadAndShowRewarded            ──►  Rewarded
AdManager.loadAndShowRewardedInterstitial──►  Rewarded Interstitial
AppOpenAdManager.onForeground            ──►  App Open
```

---

## Google test unit IDs (used in all debug builds)

| Format | Test unit ID |
|---|---|
| `APP_OPEN` | `ca-app-pub-3940256099942544/9257395921` |
| `BANNER_ADAPTIVE_ANCHORED` | `ca-app-pub-3940256099942544/9214589741` |
| `BANNER_ADAPTIVE_INLINE` | `ca-app-pub-3940256099942544/9214589741` |
| `BANNER_FIXED` | `ca-app-pub-3940256099942544/6300978111` |
| `INTERSTITIAL` | `ca-app-pub-3940256099942544/1033173712` |
| `REWARDED` | `ca-app-pub-3940256099942544/5224354917` |
| `REWARDED_INTERSTITIAL` | `ca-app-pub-3940256099942544/5354046379` |
| `NATIVE_ADVANCED` | `ca-app-pub-3940256099942544/2247696110` |
| `NATIVE_VIDEO` | `ca-app-pub-3940256099942544/1044960115` |

Debug builds always use these regardless of `ads_use_test_ids` or `ad_unit_id` in the placement.

---

## Production ad unit IDs — fill before launch

Set via `PUT /api/v1/admin/config/ads_production_units`. All blank by default.

| Format key | Production unit ID | Status |
|---|---|---|
| `APP_OPEN` | _(empty)_ | ⚠️ Not configured |
| `BANNER_ADAPTIVE_ANCHORED` | _(empty)_ | ⚠️ Not configured |
| `BANNER_ADAPTIVE_INLINE` | _(empty)_ | ⚠️ Not configured |
| `BANNER_FIXED` | _(empty)_ | ⚠️ Not configured |
| `INTERSTITIAL` | _(empty)_ | ⚠️ Not configured |
| `REWARDED` | _(empty)_ | ⚠️ Not configured |
| `REWARDED_INTERSTITIAL` | _(empty)_ | ⚠️ Not configured |
| `NATIVE_ADVANCED` | _(empty)_ | ⚠️ Not configured |
| `NATIVE_VIDEO` | _(empty)_ | ⚠️ Not configured |

---

## All placements

### Banner & Native — rendered automatically by `AdSlot` Compose component

**How sizes work:**
- Width is always `fillMaxWidth` — measured from the screen at runtime (`BoxWithConstraints`, minimum 320 dp)
- Height is set by the SDK after the ad loads; collapses to 0 dp on failure so no blank gap is left
- `BANNER_ADAPTIVE_ANCHORED` — `AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)` → typically **~50–60 dp tall**, full screen width, anchored to top or bottom
- `BANNER_ADAPTIVE_INLINE` — `AdSize.getInlineAdaptiveBannerAdSize(widthDp, 250)` → **up to 250 dp tall**, expands to fit content, used inline inside scroll lists
- `NATIVE_ADVANCED` — custom card built in code: full width, **~170 dp media view** + headline + body + CTA button, follows app theme (rounded corners, gold accent)

**Format is fully interchangeable via DB — no build required.**
The `format` field in each placement row in `ads_placements` is read at runtime by `AdConfig.resolve()`. `AdSlot` then checks `format.isBanner` or `format.isNative` and renders accordingly. There is no hardcoding of format per placement name anywhere in the Android code.

Example — switch `home_top_banner` from `BANNER_ADAPTIVE_ANCHORED` to `NATIVE_ADVANCED`:
```bash
# PATCH — merge a single placement, all others untouched
curl -X PATCH http://localhost:8000/api/v1/admin/config/ads_placements \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": {"home_top_banner": {"enabled": true, "screen": "home", "format": "NATIVE_ADVANCED", "ad_unit_id": ""}}}'

# PUT also merges for ads_placements — same result, same safety
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_placements \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": {"home_top_banner": {"enabled": true, "screen": "home", "format": "NATIVE_ADVANCED", "ad_unit_id": ""}}}'
```
Both send only the keys you want to change. All other placements are preserved. The app picks it up on the next foreground config refresh — no build, no release needed.

**Supported format values for any placement:**
| Value | Renders as | Size |
|---|---|---|
| `BANNER_ADAPTIVE_ANCHORED` | Banner | ~50–60 dp tall · full width |
| `BANNER_ADAPTIVE_INLINE` | Banner | up to 250 dp · full width |
| `BANNER_FIXED` | Banner | 320×50 dp fixed |
| `BANNER_STANDARD_320_50` | Banner | 320×50 dp |
| `BANNER_STANDARD_300_50` | Banner | 300×50 dp |
| `BANNER_STANDARD_320_100` | Banner | 320×100 dp |
| `BANNER_MEDIUM_RECTANGLE` | Banner | 300×250 dp |
| `NATIVE_ADVANCED` | Native card | ~300 dp · full width |

> Full-screen formats (`INTERSTITIAL`, `REWARDED`, `REWARDED_INTERSTITIAL`, `APP_OPEN`) are **filtered out** by `AdSlot` — they will never render inline even if set on a banner placement. Those must be triggered via `AdManager` methods.

| Placement ID | Screen | Format | Size | DB enabled | Notes |
|---|---|---|---|---|---|
| `home_top_banner` | Home | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `home_after_mood` | Home | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `home_after_daily_verse` | Home | NATIVE_ADVANCED | ~300 dp · full width | ✅ | Default on |
| `home_bottom_banner` | Home | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `home_inline_banner` | Home | BANNER_ADAPTIVE_INLINE | up to 250 dp · full width | ❌ | |
| `saved_first_verse_native` | Saved Verses | NATIVE_ADVANCED | ~300 dp · full width | ✅ | Default on |
| `saved_every_two_banner` | Saved Verses | BANNER_ADAPTIVE_INLINE | up to 250 dp · full width | ✅ | Default on |
| `saved_bottom_banner` | Saved Verses | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `saved_empty_state_ad` | Saved Verses | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `scripture_list_top` | Explore | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `scripture_list_inline` | Explore | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `scripture_list_bottom` | Explore | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `scripture_detail_inline` | Explore | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `verse_detail_after_content` | Verse Detail | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `verse_detail_bottom` | Verse Detail | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `verse_detail_between_sections` | Verse Detail | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `discover_top_banner` | Discover | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `discover_inline_native` | Discover | NATIVE_ADVANCED | ~300 dp · full width | ✅ | Default on |
| `discover_bottom_banner` | Discover | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `tree_screen_bottom` | Wisdom Tree | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `history_top_banner` | History | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `history_inline_native` | History | NATIVE_ADVANCED | ~300 dp · full width | ✅ | Default on |
| `history_bottom_banner` | History | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `wallpaper_gallery_banner` | Wallpaper Gallery | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ✅ | Default on |
| `wallpaper_gallery_inline` | Wallpaper Gallery | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |
| `wallpaper_preview_bottom` | Wallpaper Preview | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `settings_bottom_banner` | Settings | BANNER_ADAPTIVE_ANCHORED | ~50–60 dp tall · full width | ❌ | |
| `journal_list_inline` | Journal | NATIVE_ADVANCED | ~300 dp · full width | ❌ | |

**Native card layout (from `buildNativeView` in `AdSlot.kt`):**
```
┌─────────────────────────────────────────┐
│ [Ad]  [Icon 36×36]  Headline (bold)     │  ← top row
│ Body text (up to 3 lines, 13 sp)        │
│ ┌─────────────────────────────────────┐ │
│ │        Media view  (170 dp)         │ │
│ └─────────────────────────────────────┘ │
│ [ Call-to-action button  (46 dp tall) ] │
└─────────────────────────────────────────┘
  Rounded corners 18 dp · Gold border · App theme colours
```

---

### Full-screen ads — Rewarded, Rewarded Interstitial, Interstitial, App Open

| Placement ID | Screen | Format | DB enabled | Test unit ID | Trigger | UI wired? |
|---|---|---|---|---|---|---|
| `studio_share_rewarded` | Share Studio | REWARDED | ✅ | `5224354917` | Tap **Share** button | ✅ |
| `studio_whatsapp_rewarded` | Share Studio | REWARDED | ✅ | `5224354917` | Tap **WhatsApp Status** button | ✅ |
| `studio_save_image_rewarded` | Share Studio | REWARDED | ✅ | `5224354917` | Tap **Save image to gallery** | ✅ |
| `studio_generate_video_rewarded` | Video Studio | REWARDED_INTERSTITIAL | ✅ | `5354046379` | Tap **Generate video** | ✅ |
| `studio_share_video_rewarded` | Video Studio | REWARDED | ✅ | `5224354917` | Tap **Share / Reels** on result screen | ✅ |
| `studio_whatsapp_video_rewarded` | Video Studio | REWARDED | ✅ | `5224354917` | Tap **WhatsApp Status** on result screen | ✅ |
| `studio_save_video_rewarded` | Video Studio | REWARDED | ✅ | `5224354917` | Tap **Save to gallery** on result screen | ✅ |
| `studio_make_another_interstitial` | Video Studio | INTERSTITIAL | ✅ | `1033173712` | Tap **Make another** on result screen | ✅ |
| `tree_rewarded_bonus` | Wisdom Tree | REWARDED | ❌ | `5224354917` | Not yet wired to a UI button | ❌ |
| `wallpaper_download_rewarded` | Wallpaper | REWARDED | ❌ | `5224354917` | Not yet wired to a UI button | ❌ |
| `tree_completion_interstitial` | Wisdom Tree | INTERSTITIAL | ❌ | `1033173712` | After all 5 tree stages complete | ✅ |
| `app_open_ad` | App launch | APP_OPEN | ❌ | `9257395921` | Every foreground return (4 h cooldown) | ✅ |

---

## Rewarded ads — implementation detail

### ✅ Share Studio (image card)

```kotlin
// StudioScreen.kt — ShareTab
fun withReward(placementId: String, action: suspend () -> Unit) {
    if (!AdManager.canRequest() || AdManager.config.value.resolve(placementId) == null) {
        scope.launch { action() }   // ad off → proceed directly
        return
    }
    AdManager.loadAndShowRewarded(
        activity, placementId,
        onReward = { scope.launch { action() } },
        onUnavailable = { scope.launch { action() } }  // always let user proceed
    )
}

// Share button
onClick = { withReward("studio_share_rewarded") { ... ShareActions.shareImage(...) } }

// WhatsApp Status button
onClick = { withReward("studio_whatsapp_rewarded") { ... ShareActions.shareToWhatsApp(...) } }

// Save image to gallery
onClick = { withReward("studio_save_image_rewarded") { ... ShareActions.saveImageToGallery(...) } }
```

### ✅ Video Studio — Generate video (Rewarded Interstitial)

```kotlin
// StudioScreen.kt — VideoTab
AdManager.loadAndShowRewardedInterstitial(
    activity, "studio_generate_video_rewarded",
    onReward = { startGeneration() },
    onUnavailable = { startGeneration() }
)
```

### ✅ Video Studio — result screen

```kotlin
// Share / Reels
AdManager.loadAndShowRewarded(activity, "studio_share_video_rewarded",
    onReward = { ShareActions.shareVideo(...) },
    onUnavailable = { ShareActions.shareVideo(...) })

// WhatsApp Status
AdManager.loadAndShowRewarded(activity, "studio_whatsapp_video_rewarded", ...)

// Save to gallery
AdManager.loadAndShowRewarded(activity, "studio_save_video_rewarded", ...)

// Make another (Interstitial)
AdManager.preloadInterstitial("studio_make_another_interstitial")
AdManager.showInterstitial(activity, "studio_make_another_interstitial") { result = null }
```

### ❌ tree_rewarded_bonus — not yet wired

```kotlin
// Wire to a "Claim bonus" button in TreeJourneyScreen or WisdomTreeCard:
AdManager.loadAndShowRewarded(
    activity, "tree_rewarded_bonus",
    onReward = { /* grant bonus XP or streak protection */ },
    onUnavailable = { snackbarHostState.showSnackbar("Not available right now.") }
)
```

### ❌ wallpaper_download_rewarded — not yet wired

```kotlin
// Wire to a "Download free wallpaper" button in the wallpaper screen:
AdManager.loadAndShowRewarded(
    activity, "wallpaper_download_rewarded",
    onReward = { /* unlock the wallpaper download */ },
    onUnavailable = { snackbarHostState.showSnackbar("Not available right now.") }
)
```

---

## Interstitial ads — implementation detail

### ✅ studio_make_another_interstitial

Shown when user taps **Make another** after a video is generated. Preloaded on the same tap, shown immediately if ready.

### ✅ tree_completion_interstitial

```kotlin
// MainActivity.kt
milestoneStage?.let { st ->
    LaunchedEffect(st) {
        if (st == TreeStage.FRUITS) AdManager.preloadInterstitial("tree_completion_interstitial")
    }
    MilestoneDialog(st) {
        milestoneStage = null
        if (st == TreeStage.FRUITS) AdManager.showInterstitial(activity, "tree_completion_interstitial")
    }
}
```

Cooldown: 180 s between any two full-screen ads (`ads_global.min_seconds_between_fullscreen_ads`).

---

## Blocked screens — ads never show here

| Screen key | Reason |
|---|---|
| `reflection_editor` | Writing space — no interruption |
| `explain_sheet` | Reading context — no interruption |

> `share_studio` was removed from blocked screens to allow the studio rewarded placements.

---

## Saved Verses ad insertion rules (`ads_saved_verses`)

| Key | Default | Meaning |
|---|---|---|
| `first_ad_after_items` | 1 | First ad after the 1st saved verse |
| `repeat_interval_items` | 2 | Then every 2 verses |
| `max_inline_ads` | 3 | Never more than 3 ads in the list |
| `repeat_format` | `BANNER_ADAPTIVE_INLINE` | Format for repeat ads |

---

## Frequency / safety rules (`ads_global`)

| Rule | Default | Config key |
|---|---|---|
| Max ads per screen | 3 | `max_ads_per_screen` |
| Min gap between full-screen ads | 180 s | `min_seconds_between_fullscreen_ads` |
| Min gap between app-open ads | 14400 s (4 h) | `min_seconds_between_app_open_ads` |

---

## Admin API — change any setting without a build

**`PUT /admin/config/{key}`** — for dict keys (`ads_placements`, `ads_global`, etc.) this **merges** the incoming keys into the existing value. Only the keys you send are changed; everything else is preserved.

**`PATCH /admin/config/{key}`** — explicit merge, same behaviour as PUT for dict keys. Use this when you want to be explicit that you're doing a partial update.

**`DELETE /admin/config/{key}`** — removes the key entirely (it will be re-seeded with the default on next server startup).

```bash
# Global kill switch
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_enabled \
  -H "X-Admin-Token: WisdomOne@2024" -H "Content-Type: application/json" \
  -d '{"value": false}'

# Switch to production units
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_use_test_ids \
  -H "X-Admin-Token: WisdomOne@2024" -H "Content-Type: application/json" \
  -d '{"value": false}'

# Enable a single placement (PATCH — only this key changes, all others preserved)
curl -X PATCH http://localhost:8000/api/v1/admin/config/ads_placements \
  -H "X-Admin-Token: WisdomOne@2024" -H "Content-Type: application/json" \
  -d '{"value": {"tree_rewarded_bonus": {"enabled": true, "screen": "tree", "format": "REWARDED", "ad_unit_id": "ca-app-pub-XXXXXXXXXXXXXXXX/YYYYYYYYYY"}}}'

# Disable a single placement without touching others
curl -X PATCH http://localhost:8000/api/v1/admin/config/ads_placements \
  -H "X-Admin-Token: WisdomOne@2024" -H "Content-Type: application/json" \
  -d '{"value": {"home_top_banner": {"enabled": false, "screen": "home", "format": "BANNER_ADAPTIVE_ANCHORED", "ad_unit_id": ""}}}'

# Change format of a placement from banner to native
curl -X PATCH http://localhost:8000/api/v1/admin/config/ads_placements \
  -H "X-Admin-Token: WisdomOne@2024" -H "Content-Type: application/json" \
  -d '{"value": {"home_top_banner": {"enabled": true, "screen": "home", "format": "NATIVE_ADVANCED", "ad_unit_id": ""}}}'

# Increase full-screen cooldown to 10 min (PATCH merges into ads_global)
curl -X PATCH http://localhost:8000/api/v1/admin/config/ads_global \
  -H "X-Admin-Token: WisdomOne@2024" -H "Content-Type: application/json" \
  -d '{"value": {"min_seconds_between_fullscreen_ads": 600}}'
```

---

## Debug / QA — Ads test lab

In a **debug build**: Settings → Ads test lab

- Status dashboard: `ads_enabled`, `SDK ready`, `placements ON` shown as green/red
- Individual buttons for every studio placement, rewarded, rewarded interstitial, interstitial, banner, native
- All buttons use hardcoded Google test unit IDs — work even when placements are disabled in DB
- Live event log shows load / fail / show / reward earned

---

## Pre-launch checklist

- [ ] Add production unit IDs via `PUT /api/v1/admin/config/ads_production_units`
- [ ] Set `ads_use_test_ids` → `false`
- [ ] Set `android:usesCleartextTraffic="false"` in `AndroidManifest.xml`
- [ ] Wire `tree_rewarded_bonus` to a UI button in Wisdom Tree screen
- [ ] Wire `wallpaper_download_rewarded` to a UI button in Wallpaper screen
- [ ] Enable `tree_completion_interstitial` and `app_open_ad` when ready
- [ ] Verify UMP consent form appears on first launch in a GDPR region
- [ ] Test every studio rewarded placement via Ads test lab before release
