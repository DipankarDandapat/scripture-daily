"""WisdomOne | App Promotion | Creative Diversification — the single source of truth for Task 5.

Images (make_meta_ads.py), videos (make_meta_videos.py), per-ad copy files, the master CSV and the contact sheet are all
generated from this file, so filenames and copy can never drift apart.

Copy rules applied: never ask about or imply the reader's religion or other sensitive traits (Meta policy) — copy describes
the app ("daily Bible verse"), not the person; no feature that is not in the app; no price / ranking claims.
"""

CAMPAIGN = "WisdomOne | App Promotion | Creative Diversification"
PLAY_URL = "https://play.google.com/store/apps/details?id=com.scripturedaily"
FORMATS = {"4x5": (1080, 1350), "1x1": (1080, 1080), "9x16": (1080, 1920)}

AD_SETS = [
    {"id": "AS01", "folder": "02_AD_SET_01_DAILY_WISDOM", "name": "AS01 | Daily Wisdom", "angle": "Daily inspiration and Bible verses"},
    {"id": "AS02", "folder": "03_AD_SET_02_LOCKSCREEN_VERSE", "name": "AS02 | Lock Screen Verse", "angle": "Make wisdom part of your everyday phone experience"},
    {"id": "AS03", "folder": "04_AD_SET_03_WALLPAPER_EXPERIENCE", "name": "AS03 | Wallpaper Experience", "angle": "Beautiful Bible verse wallpapers"},
    {"id": "AS04", "folder": "05_AD_SET_04_DAILY_SPIRITUAL_ROUTINE", "name": "AS04 | Daily Spiritual Routine", "angle": "Build a consistent daily spiritual routine"},
    {"id": "AS05", "folder": "06_AD_SET_05_PREMIUM_WISDOMONE", "name": "AS05 | Premium WisdomOne", "angle": "All-in-one premium experience"},
]

# style = visual composition (see make_meta_ads.py); pal = brand palette variant; video = video file attached to this ad.
ADS = [
    # ── AD SET 01 — DAILY WISDOM ──
    dict(set="AS01", no=1, slug="start_your_day", title="Start Your Day With Wisdom", style="hero_dawn", pal="dawn",
         hook="Morning ritual", emotion="Hopeful, fresh", feature="Daily Bible verse",
         headline="Start Your Day With Wisdom",
         primary="Before the emails and the noise — one verse to set the tone for your day. WisdomOne brings you a daily Bible verse, beautifully presented, every morning.",
         description="A daily Bible verse", cta="Install Now", video="V01_daily_wisdom"),
    dict(set="AS01", no=2, slug="little_wisdom", title="A Little Wisdom Every Day", style="typography", pal="sand",
         hook="Tiny daily habit", emotion="Calm, simple", feature="Daily verse habit",
         headline="A Little Wisdom Every Day",
         primary="Big changes start small. Read one verse a day and watch a habit grow — WisdomOne keeps it simple, quiet and beautiful.",
         description="One verse. Every day.", cta="Install Now"),
    dict(set="AS01", no=3, slug="moment_of_peace", title="Find a Moment of Peace", style="cinematic", pal="sunset", art=3,
         hook="Pause and breathe", emotion="Peaceful, emotional", feature="Daily verse + calm design",
         headline="Find a Moment of Peace",
         primary="Busy day? Take thirty seconds. Open WisdomOne, read today’s verse, breathe — and carry a little peace with you.",
         description="Your daily moment of calm", cta="Install Now"),
    dict(set="AS01", no=4, slug="words_that_inspire", title="Words That Inspire Your Day", style="verse_card", pal="plum",
         hook="Meaningful words", emotion="Inspired", feature="Verse card + Explain like I'm 5",
         headline="Words That Inspire Your Day",
         primary="Some words stay with you. WisdomOne shows a meaningful Bible verse each day — and can explain it in simple words when you want to go deeper.",
         description="Verses that stay with you", cta="Install Now"),
    dict(set="AS01", no=5, slug="daily_dose", title="Your Daily Dose of Wisdom", style="minimal_cta", pal="lavender",
         hook="Direct install", emotion="Confident", feature="Daily verse + Wisdom Tree",
         headline="Your Daily Dose of Wisdom",
         primary="Today’s verse is waiting. Install WisdomOne and get a fresh Bible verse every day — plus a Wisdom Tree that grows as you read.",
         description="Free daily Bible verses", cta="Install Now"),
    # ── AD SET 02 — LOCK SCREEN VERSE ──
    dict(set="AS02", no=1, slug="lockscreen_wisdom", title="Wisdom on Your Lock Screen", style="lock_fullbleed", pal="night", lock="render_lock_img03",
         hook="Feature reveal", emotion="Wonder", feature="Lock-screen verse wallpaper",
         headline="Wisdom on Your Lock Screen",
         primary="Your lock screen, but meaningful. WisdomOne places today’s Bible verse right on your lock-screen wallpaper — no need to open an app.",
         description="Verses on your lock screen", cta="Install Now"),
    dict(set="AS02", no=2, slug="every_unlock", title="Every Time You Unlock, Find Inspiration", style="unlock_hero", pal="midnight", lock="render_lock_img18",
         hook="Unlock moment", emotion="Uplifting", feature="Automatic verse refresh",
         headline="Inspiration Every Time You Unlock",
         primary="You unlock your phone dozens of times a day. Let some of those moments be inspiring — WisdomOne refreshes the verse on your lock screen automatically.",
         description="New verse, automatically", cta="Install Now", video="V02_lockscreen_verse"),
    dict(set="AS02", no=3, slug="favourite_verses", title="Your Favourite Verses, Always Close", style="duo_personal", pal="dawn",
         hook="Personal & close", emotion="Warm, personal", feature="Personalised lock screen (artwork, theme, text)",
         headline="Your Verses, Always Close",
         primary="Pick the artwork, the colours and the text size — WisdomOne turns your lock screen into a personal space for God’s word.",
         description="Personal. Beautiful. Yours.", cta="Install Now"),
    dict(set="AS02", no=4, slug="beautiful_lockscreen", title="A Beautiful Lock Screen Experience", style="fan3", pal="lavender",
         locks=("render_lock_img14", "render_lock_img06", "render_lock_color_royal_purple"),
         hook="Visual showcase", emotion="Delight", feature="Artwork + verse lock screens",
         headline="A Beautiful Lock Screen",
         primary="Artwork and scripture, together. Choose from 20 artworks or calm colour themes and see a new verse on your lock screen every day.",
         description="20 artworks + colour themes", cta="Install Now"),
    dict(set="AS02", no=5, slug="part_of_everyday", title="Let Wisdom Be Part of Your Everyday", style="before_after", pal="sage", lock="render_lock_img10",
         hook="Before / after", emotion="Reflective", feature="Lock-screen verse (before vs after)",
         headline="Let Wisdom Into Your Everyday",
         primary="Same phone, different feeling. Swap an ordinary lock screen for one that greets you with a Bible verse — every single day.",
         description="See the difference", cta="Install Now"),
    # ── AD SET 03 — WALLPAPER EXPERIENCE ──
    dict(set="AS03", no=1, slug="wallpapers_with_meaning", title="Beautiful Wallpapers With Meaning", style="wall_grid", pal="plum",
         locks=("render_lock_img02", "render_lock_img07", "render_lock_img15", "render_lock_img19"),
         hook="Gallery", emotion="Admiration", feature="Verse wallpapers",
         headline="Wallpapers With Meaning",
         primary="Wallpapers that say something. Each WisdomOne wallpaper pairs calm artwork with a Bible verse — and it changes with your daily verse.",
         description="Art + scripture", cta="Download"),
    dict(set="AS03", no=2, slug="screen_inspiration", title="Turn Your Screen Into a Source of Inspiration", style="day_night", pal="ocean",
         hook="Transformation", emotion="Inspired", feature="Live wallpaper (time-of-day themes)",
         headline="Turn Your Screen Into Inspiration",
         primary="A calm live wallpaper that follows your day — golden sunrise in the morning, deep blue at night — with today’s verse on your home screen.",
         description="Live verse wallpaper", cta="Install Now", video="V03_wallpaper_experience"),
    dict(set="AS03", no=3, slug="visual_inspiration", title="New Visual Inspiration", style="split_gallery", pal="lavender",
         hook="Personalisation", emotion="Creative", feature="Wallpaper gallery + colour themes",
         headline="New Visual Inspiration",
         primary="20 artworks, 6 colour themes, your choice of text size and placement. Design a verse wallpaper that feels like you.",
         description="Design your wallpaper", cta="Install Now"),
    dict(set="AS03", no=4, slug="faith_meets_design", title="Faith Meets Beautiful Design", style="cinematic", pal="midnight", art=18,
         hook="Premium design", emotion="Awe", feature="Premium wallpaper artwork",
         headline="Faith Meets Beautiful Design",
         primary="Thoughtful typography. Gentle light. Scripture at the centre. WisdomOne is designed to feel as calm as the words it shares.",
         description="Designed for calm", cta="Install Now"),
    dict(set="AS03", no=5, slug="personalize_everyday", title="Personalize Your Everyday Experience", style="fan3", pal="sand",
         locks=("render_lock_color_golden", "render_lock_color_ocean_blue", "render_lock_color_forest_green"),
         hook="Options", emotion="Playful, personal", feature="Colour themes",
         headline="Make Every Screen Yours",
         primary="Golden, ocean, forest, midnight… Pick a colour theme or artwork and WisdomOne keeps your verse wallpaper fresh with every new verse.",
         description="Themes for every mood", cta="Install Now"),
    # ── AD SET 04 — DAILY SPIRITUAL ROUTINE ──
    dict(set="AS04", no=1, slug="make_time", title="Make Time for Daily Wisdom", style="day_night_routine", pal="dawn",
         hook="Morning & evening", emotion="Grounded", feature="Verse refresh schedule (1–24 h)",
         headline="Make Time for Daily Wisdom",
         primary="Morning coffee or evening wind-down — WisdomOne fits your rhythm. Choose how often your verse refreshes: every 1, 6, 12 or 24 hours.",
         description="On your schedule", cta="Install Now"),
    dict(set="AS04", no=2, slug="gentle_reminder", title="A Gentle Reminder to Pause", style="notifications", pal="sage", art=10,
         hook="Gentle nudge", emotion="Cared for", feature="Verse reminders",
         headline="A Gentle Reminder to Pause",
         primary="A quiet notification with today’s verse — no noise, no pressure. Just a gentle reminder to pause and reflect.",
         description="Gentle verse reminders", cta="Install Now"),
    dict(set="AS04", no=3, slug="one_verse_one_moment", title="One Verse. One Moment. Every Day.", style="typography_night", pal="night",
         hook="Rhythm", emotion="Simple, sincere", feature="Daily verse",
         headline="One Verse. One Moment. Every Day.",
         primary="One verse. One moment. Every day. That’s all it takes to build a habit that stays with you — WisdomOne makes it easy.",
         description="Simple daily habit", cta="Install Now"),
    dict(set="AS04", no=4, slug="stay_connected", title="Stay Connected With Daily Inspiration", style="duo_reminder", pal="ocean",
         hook="Stay connected", emotion="Reassuring", feature="Reminder + lock-screen verse",
         headline="Stay Connected Every Day",
         primary="Get today’s verse as a gentle notification and see it on your lock screen. Staying connected to daily inspiration has never been easier.",
         description="Daily verse, delivered", cta="Install Now", video="V04_daily_routine"),
    dict(set="AS04", no=5, slug="small_moments", title="Small Moments, Meaningful Days", style="tree_focus", pal="dawn",
         hook="Progress", emotion="Encouraged", feature="Wisdom Tree (reading habit)",
         headline="Small Moments, Meaningful Days",
         primary="Every day you read, your Wisdom Tree grows — from a sprout to leaves, branches, flowers and fruit. Small moments add up.",
         description="Grow your Wisdom Tree", cta="Install Now"),
    # ── AD SET 05 — PREMIUM WISDOMONE ──
    dict(set="AS05", no=1, slug="meet_wisdomone", title="Meet WisdomOne", style="product_grid", pal="lavender",
         hook="Introduction", emotion="Curious", feature="App overview",
         headline="Meet WisdomOne",
         primary="Meet WisdomOne: a daily Bible verse, lock-screen and live wallpapers, gentle reminders and a Wisdom Tree that grows with you — all in one calm app.",
         description="Your daily verse app", cta="Install Now", video="V05_complete_experience"),
    dict(set="AS05", no=2, slug="more_than_a_verse", title="More Than Just a Bible Verse", style="feature_list", pal="plum",
         hook="Feature depth", emotion="Impressed", feature="Feature list",
         headline="More Than Just a Bible Verse",
         primary="Daily verse, lock-screen wallpaper, live wallpaper, reminders, a mood check-in and simple explanations — WisdomOne is more than just a verse.",
         description="Everything in one app", cta="Install Now"),
    dict(set="AS05", no=3, slug="beautiful_space", title="A Beautiful Space for Daily Wisdom", style="split_dark", pal="midnight",
         hook="Premium UI", emotion="Serene", feature="Dark theme / premium UI",
         headline="A Beautiful Space for Wisdom",
         primary="Light or dark, every screen of WisdomOne is crafted for calm — a beautiful, distraction-free space for your daily verse.",
         description="Calm, crafted design", cta="Install Now"),
    dict(set="AS05", no=4, slug="travels_with_you", title="Wisdom That Travels With You", style="duo_ambient", pal="sunset", art=1,
         hook="Everywhere", emotion="Companionship", feature="Lock screen + live wallpaper",
         headline="Wisdom That Travels With You",
         primary="On your lock screen when you check the time, on your home screen when you open an app — WisdomOne keeps today’s verse with you all day.",
         description="Lock + home screen verse", cta="Install Now"),
    dict(set="AS05", no=5, slug="better_experience", title="Discover a Better Daily Wisdom Experience", style="install_hero", pal="dawn",
         hook="Strong install", emotion="Motivated", feature="Complete experience",
         headline="Discover WisdomOne",
         primary="Ready for a better daily wisdom experience? Install WisdomOne free and get your first verse in seconds.",
         description="Install free today", cta="Install Now"),
]

VIDEOS = {
    "V01_daily_wisdom": dict(set="AS01", seconds=18, title="Daily Wisdom"),
    "V02_lockscreen_verse": dict(set="AS02", seconds=17, title="Lock Screen Verse"),
    "V03_wallpaper_experience": dict(set="AS03", seconds=23, title="Wallpaper Experience"),
    "V04_daily_routine": dict(set="AS04", seconds=18, title="Daily Spiritual Routine"),
    "V05_complete_experience": dict(set="AS05", seconds=28, title="Complete WisdomOne Experience"),
}


def ad_set(sid):
    return next(s for s in AD_SETS if s["id"] == sid)


def ad_name(ad):
    return f"{ad['set']}_AD{ad['no']:02d}_{ad['slug']}"


def image_files(ad):
    return {fmt: f"AD{ad['no']:02d}_{ad['slug']}_{fmt}.jpg" for fmt in FORMATS}


def video_file(ad):
    return f"AD{ad['no']:02d}_{ad['slug']}_video.mp4" if ad.get("video") else ""
