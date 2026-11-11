"""Default values for the centralised key-value configuration served by ``GET /api/v1/config``.

Defaults are inserted once (``ensure_app_config``); afterwards the database rows are the source of truth and can be
changed through ``PUT /api/v1/admin/config/{key}`` without shipping a new app build. Keep **test** ad-unit IDs and
**production** ad-unit IDs in separate keys; the app only uses production IDs when ``ads_use_test_ids`` is false *and*
it is a release build.
"""

ADMOB_APP_ID = "ca-app-pub-7872455938809561~3577314080"

# Google's official sample ad units (https://developers.google.com/admob/android/test-ads) — safe to click in development.
TEST_UNITS = {
    "APP_OPEN": "ca-app-pub-3940256099942544/9257395921",
    "BANNER_ADAPTIVE_ANCHORED": "ca-app-pub-3940256099942544/9214589741",
    "BANNER_ADAPTIVE_INLINE": "ca-app-pub-3940256099942544/9214589741",
    "BANNER_FIXED": "ca-app-pub-3940256099942544/6300978111",
    "INTERSTITIAL": "ca-app-pub-3940256099942544/1033173712",
    "REWARDED": "ca-app-pub-3940256099942544/5224354917",
    "REWARDED_INTERSTITIAL": "ca-app-pub-3940256099942544/5354046379",
    "NATIVE_ADVANCED": "ca-app-pub-3940256099942544/2247696110",
    "NATIVE_VIDEO": "ca-app-pub-3940256099942544/1044960115",
}

# Fill these in the database (admin API) before a production release. Empty = placement stays off in production.
PRODUCTION_UNITS = {fmt: "" for fmt in TEST_UNITS}

FORMATS = sorted(set(TEST_UNITS) | {
    "BANNER_STANDARD_320_50", "BANNER_STANDARD_300_50", "BANNER_STANDARD_320_100", "BANNER_MEDIUM_RECTANGLE", "BANNER_LARGE_ADAPTIVE",
})
# Standard banner sizes borrow the fixed-size banner unit.
FORMAT_UNIT_ALIAS = {
    "BANNER_STANDARD_320_50": "BANNER_FIXED", "BANNER_STANDARD_300_50": "BANNER_FIXED", "BANNER_STANDARD_320_100": "BANNER_FIXED",
    "BANNER_MEDIUM_RECTANGLE": "BANNER_FIXED", "BANNER_LARGE_ADAPTIVE": "BANNER_ADAPTIVE_ANCHORED",
}


def _p(screen: str, fmt: str, enabled: bool = False, **extra) -> dict:
    return {"enabled": enabled, "screen": screen, "format": fmt, "ad_unit_id": "", **extra}


# Placement inventory from design/AdMob_Placement_Firebase_Remote_Config_Specification.txt. Combination B ("balanced") is the
# starting point: full-screen formats (app open, interstitial, rewarded) stay off until measured.
PLACEMENTS = {
    "home_top_banner": _p("home", "BANNER_ADAPTIVE_ANCHORED"),
    "home_after_mood": _p("home", "NATIVE_ADVANCED"),
    "home_after_daily_verse": _p("home", "NATIVE_ADVANCED", True),
    "home_bottom_banner": _p("home", "BANNER_ADAPTIVE_ANCHORED"),
    "home_inline_banner": _p("home", "BANNER_ADAPTIVE_INLINE"),
    "saved_first_verse_native": _p("saved", "NATIVE_ADVANCED", True),
    "saved_every_two_banner": _p("saved", "BANNER_ADAPTIVE_INLINE", True),
    "saved_bottom_banner": _p("saved", "BANNER_ADAPTIVE_ANCHORED"),
    "saved_empty_state_ad": _p("saved", "NATIVE_ADVANCED"),
    "scripture_list_top": _p("explore", "BANNER_ADAPTIVE_ANCHORED"),
    "scripture_list_inline": _p("explore", "NATIVE_ADVANCED"),
    "scripture_list_bottom": _p("explore", "BANNER_ADAPTIVE_ANCHORED"),
    "scripture_detail_inline": _p("explore", "NATIVE_ADVANCED"),
    "verse_detail_after_content": _p("verse_detail", "NATIVE_ADVANCED"),
    "verse_detail_bottom": _p("verse_detail", "BANNER_ADAPTIVE_ANCHORED"),
    "verse_detail_between_sections": _p("verse_detail", "NATIVE_ADVANCED"),
    "discover_top_banner": _p("discover", "BANNER_ADAPTIVE_ANCHORED"),
    "discover_inline_native": _p("discover", "NATIVE_ADVANCED", True),
    "discover_bottom_banner": _p("discover", "BANNER_ADAPTIVE_ANCHORED"),
    "tree_screen_bottom": _p("tree", "BANNER_ADAPTIVE_ANCHORED"),
    "tree_completion_interstitial": _p("tree", "INTERSTITIAL"),
    "tree_rewarded_bonus": _p("tree", "REWARDED"),
    "history_top_banner": _p("history", "BANNER_ADAPTIVE_ANCHORED"),
    "history_inline_native": _p("history", "NATIVE_ADVANCED", True),
    "history_bottom_banner": _p("history", "BANNER_ADAPTIVE_ANCHORED"),
    "wallpaper_gallery_banner": _p("wallpaper", "BANNER_ADAPTIVE_ANCHORED", True),
    "wallpaper_gallery_inline": _p("wallpaper", "NATIVE_ADVANCED"),
    "wallpaper_preview_bottom": _p("wallpaper", "BANNER_ADAPTIVE_ANCHORED"),
    "wallpaper_download_rewarded": _p("wallpaper", "REWARDED"),
    "settings_bottom_banner": _p("settings", "BANNER_ADAPTIVE_ANCHORED"),
    "journal_list_inline": _p("journal", "NATIVE_ADVANCED"),
    "app_open_ad": _p("app", "APP_OPEN"),
    # Share Studio
    "studio_share_rewarded": _p("share_studio", "REWARDED"),
    "studio_whatsapp_rewarded": _p("share_studio", "REWARDED"),
    "studio_save_image_rewarded": _p("share_studio", "REWARDED"),
    # Video Studio
    "studio_generate_video_rewarded": _p("share_studio", "REWARDED_INTERSTITIAL"),
    "studio_share_video_rewarded": _p("share_studio", "REWARDED"),
    "studio_whatsapp_video_rewarded": _p("share_studio", "REWARDED"),
    "studio_save_video_rewarded": _p("share_studio", "REWARDED"),
    "studio_make_another_interstitial": _p("share_studio", "INTERSTITIAL"),
}
# Screens where an ad must never appear however the placements are configured (scripture reading + reflection writing).
BLOCKED_SCREENS = ["reflection_editor", "explain_sheet"]

PLAY_STORE_URL = "https://play.google.com/store/apps/details?id=com.scripturedaily"

# Replaces the Firebase Remote Config key ``latestAppVersion``. Evaluated on the device (see app.services.app_config.evaluate_gate):
#   installed < minimum_supported_version        -> mandatory update (cannot be skipped)
#   installed < latest_version and force_update  -> mandatory update
#   installed < latest_version                   -> optional update ("Update now" / "Later")
#   otherwise                                    -> continue normally
APP_UPDATE = {
    "enabled": True,
    "latest_version": "1.4.2",
    "minimum_supported_version": "1.0.0",
    "force_update": False,
    "title": "A new version is available",
    "message": "Update WisdomOne to get the latest improvements and fixes.",
    "mandatory_title": "Update required",
    "mandatory_message": "This version of WisdomOne is no longer supported. Please update to continue.",
    "play_store_url": PLAY_STORE_URL,
    "optional_reminder_hours": 24,
}

# Full-screen maintenance page. Checked after a mandatory update and before anything else in the app.
MAINTENANCE_MODE = {
    "enabled": False,
    "title": "We'll Be Back Soon!",
    "message": "We're currently upgrading WisdomOne to bring you a better experience. "
               "Our team is working to restore the service as quickly as possible.",
    "estimated_end_time": None,      # ISO-8601 with offset, e.g. "2026-11-12T06:30:00Z"; shown in the user's local time
    "allow_retry": True,             # show the "Check again" button
    "enforce_cached_for_hours": 12,  # how long a cached "enabled" may block the app while the config API is unreachable
}

# Ambient Wisdom Layer (lock screen, live wallpaper, activity-aware suggestions). Server-side kill switches only — the user's own
# choices stay on the device.
AMBIENT_WISDOM = {
    "live_wallpaper_available": True,
    "activity_aware_available": True,
    "ambient_notification_available": True,
}

DEFAULTS: dict[str, tuple[object, str]] = {
    "app_update": (APP_UPDATE, "App update prompt: latest/minimum supported version, force flag, texts and Play Store URL."),
    "maintenance_mode": (MAINTENANCE_MODE, "Full-screen maintenance page. enabled=true blocks the whole app until it is turned off."),
    "ambient_wisdom": (AMBIENT_WISDOM, "Server kill switches for the Ambient Wisdom Layer features."),
    "config_schema_version": (1, "Bump when the shape of a config value changes in a way old apps cannot read."),
    "config_refresh_min_interval_seconds": (300, "Minimum seconds between two config fetches while the app stays in the foreground."),
    "ads_enabled": (True, "GLOBAL SWITCH. false = no ad is requested or shown anywhere, whatever the placements say."),
    "ads_use_test_ids": (True, "true = Google test ad units. Debug builds always use test units regardless of this value."),
    "ads_app_id": (ADMOB_APP_ID, "AdMob application ID (must also be in the app manifest)."),
    "ads_test_units": (TEST_UNITS, "Official Google test ad-unit IDs per format."),
    "ads_production_units": (PRODUCTION_UNITS, "Production ad-unit IDs per format. Empty = not configured."),
    "ads_global": ({"max_ads_per_screen": 3, "min_seconds_between_fullscreen_ads": 180, "min_seconds_between_app_open_ads": 14400,
                    "respect_user_navigation": True}, "Frequency and display rules shared by every placement."),
    "ads_banner": ({"anchored_adaptive": True, "inline_max_height_dp": 250, "collapse_on_failure": True},
                   "Banner size/layout: adaptive widths are computed from the screen; failed ads collapse to zero height."),
    "ads_placements": (PLACEMENTS, "Per-placement enabled switch, format and optional production ad_unit_id override."),
    "ads_saved_verses": ({"first_ad_after_items": 1, "repeat_interval_items": 2, "max_inline_ads": 3, "repeat_format": "BANNER_ADAPTIVE_INLINE"},
                         "Saved-verses ad insertion: after N saved verses, then every M."),
    "ads_blocked_screens": (BLOCKED_SCREENS, "Screens that never show ads."),
    "ads_privacy": ({"require_consent": True, "tag_for_child_directed_treatment": False, "tag_for_under_age_of_consent": False,
                     "max_ad_content_rating": "PG"}, "Consent (UMP) and age-appropriate ad settings applied before the SDK loads ads."),
    "wisdom_tree_default_enabled": (True, "Whether new installs start with the Wisdom Tree on."),
    "meta_ads_enabled": (False, "Meta (Facebook) SDK event tracking. true = events flow to Meta Events Manager. false = all MetaAnalytics.track() calls are no-ops."),
}
