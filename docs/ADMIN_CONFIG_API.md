# WisdomOne — Admin Config API Reference

## Admin Token

```
WisdomOne@2024
```

> ⚠️ Change this before any public/production deployment (`SD_ADMIN_TOKEN` in `backend/.env`).

---

## Base URL

| Environment | URL |
|---|---|
| Local dev | `http://localhost:8000/api/v1` |
| Emulator (Android) | `http://10.0.2.2:8000/api/v1` |
| Production | `https://your-domain.com/api/v1` |

---

## How ETag / 304 works

1. App fetches `GET /config` → server returns `200` + `ETag: "abc123"` header
2. App saves the ETag
3. Next fetch → app sends `If-None-Match: "abc123"`
4. Config unchanged → server returns `304 Not Modified` (no body, uses cached copy)
5. **Any config value changes** → SHA1 hash changes → new ETag → server returns `200` with fresh data

**304 is not an error.** It means the config is unchanged and the app is using its cache correctly.

---

## View All Config Values

### All keys (admin — includes timestamps)
```bash
curl http://localhost:8000/api/v1/admin/config \
  -H "X-Admin-Token: WisdomOne@2024"
```

### Public config (exactly what the app receives)
```bash
curl http://localhost:8000/api/v1/config
```

### Browser / Swagger UI
- Full docs: `http://localhost:8000/docs`
- Quick view: `http://localhost:8000/api/v1/config`

---

## Update a Config Key

```
PUT /api/v1/admin/config/{key}
```

```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/{key} \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": <new_value>}'
```

---

## Real Examples

### 🔴 Turn ads OFF globally
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_enabled \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": false}'
```

### 🟢 Turn ads ON globally
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_enabled \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": true}'
```

---

### 🧪 Use test ad IDs (development)
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_use_test_ids \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": true}'
```

### 🚀 Switch to production ad IDs (before Play Store release)
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_use_test_ids \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": false}'
```

---

### ⏱ Change config refresh interval (seconds)
```bash
# Check every 60 seconds (aggressive — for testing)
curl -X PUT http://localhost:8000/api/v1/admin/config/config_refresh_min_interval_seconds \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": 60}'

# Default (every 5 minutes)
curl -X PUT http://localhost:8000/api/v1/admin/config/config_refresh_min_interval_seconds \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": 300}'
```

---

### 📢 Enable / disable a specific ad placement
```bash
# Enable home top banner
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_placements \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
      "home_top_banner":        {"enabled": true,  "screen": "home",     "format": "BANNER_ADAPTIVE_ANCHORED", "ad_unit_id": ""},
      "home_after_daily_verse": {"enabled": true,  "screen": "home",     "format": "NATIVE_ADVANCED",          "ad_unit_id": ""},
      "discover_inline_native": {"enabled": true,  "screen": "discover", "format": "NATIVE_ADVANCED",          "ad_unit_id": ""},
      "saved_first_verse_native":{"enabled": true, "screen": "saved",    "format": "NATIVE_ADVANCED",          "ad_unit_id": ""},
      "saved_every_two_banner": {"enabled": true,  "screen": "saved",    "format": "BANNER_ADAPTIVE_INLINE",   "ad_unit_id": ""},
      "wallpaper_gallery_banner":{"enabled": true, "screen": "wallpaper","format": "BANNER_ADAPTIVE_ANCHORED", "ad_unit_id": ""},
      "journal_list_inline":    {"enabled": false, "screen": "journal",  "format": "NATIVE_ADVANCED",          "ad_unit_id": ""}
    }
  }'
```

---

### 🌳 Turn off Wisdom Tree for new installs
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/wisdom_tree_default_enabled \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{"value": false}'
```

---

### 🔢 Update ad frequency caps
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_global \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
      "max_ads_per_screen": 3,
      "min_seconds_between_fullscreen_ads": 180,
      "min_seconds_between_app_open_ads": 14400,
      "respect_user_navigation": true
    }
  }'
```

---

### 🔒 Update privacy / consent settings
```bash
curl -X PUT http://localhost:8000/api/v1/admin/config/ads_privacy \
  -H "X-Admin-Token: WisdomOne@2024" \
  -H "Content-Type: application/json" \
  -d '{
    "value": {
      "require_consent": true,
      "tag_for_child_directed_treatment": false,
      "tag_for_under_age_of_consent": false,
      "max_ad_content_rating": "PG"
    }
  }'
```

---

## Delete a Config Key

```bash
curl -X DELETE http://localhost:8000/api/v1/admin/config/{key} \
  -H "X-Admin-Token: WisdomOne@2024"
```

> The key is removed from the DB. The app falls back to its built-in default on the next fetch.

---

## All Config Keys

| Key | Type | Default | What it controls |
|---|---|---|---|
| `ads_enabled` | bool | `true` | Global ads on/off — overrides everything |
| `ads_use_test_ids` | bool | `true` | Test vs production ad units |
| `ads_app_id` | string | AdMob ID | AdMob application ID |
| `ads_test_units` | object | Google test IDs | Test ad unit IDs per format |
| `ads_production_units` | object | all empty | Production ad unit IDs per format |
| `ads_placements` | object | see below | Per-placement enabled + format + unit |
| `ads_global` | object | see below | Frequency caps across all placements |
| `ads_banner` | object | adaptive | Banner size and collapse behaviour |
| `ads_saved_verses` | object | see below | Ad spacing in the Saved tab |
| `ads_blocked_screens` | array | `["reflection_editor","explain_sheet","share_studio"]` | Screens that never show ads |
| `ads_privacy` | object | PG / consent on | UMP consent + age-appropriate settings |
| `config_refresh_min_interval_seconds` | int | `300` | Min seconds between foreground config fetches |
| `wisdom_tree_default_enabled` | bool | `true` | Whether new installs start with Wisdom Tree on |
| `config_schema_version` | int | `1` | Bump when config shape changes incompatibly |

---

## Ad Placement IDs

| Placement ID | Screen | Format | On by default |
|---|---|---|---|
| `home_top_banner` | Home | BANNER_ADAPTIVE_ANCHORED | ❌ |
| `home_after_mood` | Home | NATIVE_ADVANCED | ❌ |
| `home_after_daily_verse` | Home | NATIVE_ADVANCED | ✅ |
| `home_bottom_banner` | Home | BANNER_ADAPTIVE_ANCHORED | ❌ |
| `home_inline_banner` | Home | BANNER_ADAPTIVE_INLINE | ❌ |
| `saved_first_verse_native` | Saved | NATIVE_ADVANCED | ✅ |
| `saved_every_two_banner` | Saved | BANNER_ADAPTIVE_INLINE | ✅ |
| `saved_bottom_banner` | Saved | BANNER_ADAPTIVE_ANCHORED | ❌ |
| `discover_top_banner` | Discover | BANNER_ADAPTIVE_ANCHORED | ❌ |
| `discover_inline_native` | Discover | NATIVE_ADVANCED | ✅ |
| `discover_bottom_banner` | Discover | BANNER_ADAPTIVE_ANCHORED | ❌ |
| `wallpaper_gallery_banner` | Wallpaper | BANNER_ADAPTIVE_ANCHORED | ✅ |
| `journal_list_inline` | Journal | NATIVE_ADVANCED | ❌ |
| `tree_screen_bottom` | Tree | BANNER_ADAPTIVE_ANCHORED | ❌ |
| `tree_completion_interstitial` | Tree | INTERSTITIAL | ❌ |
| `app_open_ad` | App | APP_OPEN | ❌ |

---

## Verify a Change — 3 Ways

After any `PUT`, confirm the update took effect:

```bash
# 1. Admin view — shows updated_at timestamp
curl http://localhost:8000/api/v1/admin/config \
  -H "X-Admin-Token: WisdomOne@2024" | python -m json.tool

# 2. Public view — exactly what the app receives (ETag will be different now)
curl -i http://localhost:8000/api/v1/config

# 3. Single key only
curl http://localhost:8000/api/v1/admin/config \
  -H "X-Admin-Token: WisdomOne@2024" | python -m json.tool | grep -A3 "ads_enabled"
```

---

## Force Android App to Re-fetch

After a config change the app will pick it up automatically on the next foreground refresh (within `config_refresh_min_interval_seconds`). To force it immediately:

- **Kill and reopen the app** — the startup fetch always runs
- **Or** set `config_refresh_min_interval_seconds` to `0` temporarily, then restore it

---

*Last updated: see `updated_at` field in `GET /api/v1/admin/config`*
