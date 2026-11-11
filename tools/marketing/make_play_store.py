#!/usr/bin/env python3
"""Task 1 — Google Play Store creatives for WisdomOne.

    .venv/bin/python tools/marketing/make_play_store.py            # everything
    .venv/bin/python tools/marketing/make_play_store.py 3 5         # only creatives 3 and 5

Writes into play_store_creatives/ (see its README). Every creative uses real app screens and shows the lock-screen verse.
"""
from __future__ import annotations

import os
import shutil
import sys

from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wo_kit as K  # noqa: E402

OUT = os.path.join(K.ROOT, "play_store_creatives")
FORMATS = {"9x16": (1080, 1920), "4x5": (1080, 1350), "1x1": (1080, 1080)}
LOCK_NOTE = ("Today’s Bible verse", "But the fruit of the Spirit is love, joy, peace, longsuffering…")


def screen(name):
    return K.src(name)


def lock(render, notification=False):
    return K.lockscreen(render, notification=LOCK_NOTE if notification else None)


# ── Layout engine ────────────────────────────────────────────────────────────
def layout(W, H, *, bg, headline, sub, phones, eyebrow=None, chips=(), pal="dawn", headline_family=("serif", "bolditalic"),
           cta=True, text_align=None, bleed=0.10, phone_scale=1.0, lockup=True, top_frac=None, cta_text="Get it on Google Play", force_cta=False):
    """phones: list of dicts {img, w (relative width), x (offset as fraction of phone-area width), y (fraction of main height), tilt, z}."""
    top, bottom, accent, tc, sc = K.PALETTES[pal]
    canvas = bg.convert("RGBA")
    wide = W / H >= 0.95
    m = int(W * 0.075)
    # ── text ──
    if wide:
        tx, ty, tw = m, int(H * 0.12), int(W * 0.415)
        align = text_align or "left"
    else:
        tx, ty, tw = m, int(H * (top_frac if top_frac is not None else (0.065 if H / W > 1.5 else 0.06))), W - 2 * m
        align = text_align or "center"
    y = ty
    if lockup:
        if align == "center":
            K.brand_lockup(canvas, (W // 2, y), size=int(W * 0.052), color=tc, anchor="mt")
        else:
            K.brand_lockup(canvas, (tx, y), size=int(W * 0.05), color=tc)
        y += int(W * 0.052 * 1.55)
    if eyebrow:
        f = int(W * 0.026)
        b = K.pill(canvas, eyebrow.upper(), (W // 2 if align == "center" else tx, y), size=f, bg=accent, fg=K.C["white"] if sum(accent) < 400 else K.C["ink"],
                   anchor="mt" if align == "center" else "lt", style="bold")
        y = b[3] + int(H * 0.018)
    hsize = int(W * (0.088 if not wide else 0.075))
    y, _ = K.text_block(canvas, headline, (tx, y, tw, int(H * (0.20 if not wide else 0.36))), headline_family[0], headline_family[1], hsize,
                        fill=tc, align=align, line=1.06, max_lines=3)
    y += int(H * 0.012)
    y, _ = K.text_block(canvas, sub, (tx, y, tw, int(H * 0.14)), "sans", "medium", int(W * 0.036), fill=sc, align=align, line=1.32, max_lines=3)
    if chips and wide:
        chips = chips[:3]   # keep room for the call to action
    if chips:
        y += int(H * 0.014)
        cx = tx
        rows = []
        cur, cur_w = [], 0
        tmp = ImageDraw.Draw(canvas)
        fs = int(W * 0.026)
        for c in chips:
            cw = tmp.textlength(c, font=K.font("sans", "demi", fs)) + fs * 2.2
            if cur and cur_w + cw > tw:
                rows.append((cur, cur_w)); cur, cur_w = [], 0
            cur.append((c, cw)); cur_w += cw + fs * 0.5
        if cur: rows.append((cur, cur_w))
        for row, rw in rows:
            cx = tx + ((tw - rw) / 2 if align == "center" else 0)
            for c, cw in row:
                light = sum(tc) < 600
                b = K.pill(canvas, c, (cx, y), size=fs, bg=(255, 255, 255), fg=K.C["plum"] if light else K.C["cream"],
                           alpha=215 if light else 38, style="demi")
                cx = b[2] + fs * 0.5
            y += int(fs * 2.15)
    text_bottom = y
    if force_cta and not wide:
        b = K.play_cta(canvas, (W // 2 if align == "center" else tx, y + int(H * 0.012)), height=int(W * 0.07), dark=sum(tc) <= 600, anchor="mt" if align == "center" else "lt", text=cta_text)
        y = text_bottom = b[3]
    if cta and wide:
        light_text = sum(tc) > 600   # light headline = dark background → white CTA
        K.play_cta(canvas, (tx, min(H - m - int(W * 0.075), text_bottom + int(H * 0.04))), height=int(W * 0.075), dark=not light_text, text=cta_text)
    # ── phones ──
    if wide:
        area = (int(W * 0.52), int(H * 0.06), W - int(W * 0.52) - int(m * 0.4), int(H * 0.94))
    else:
        area = (0, text_bottom + int(H * 0.035), W, H - text_bottom - int(H * 0.035))
    ax, ay, aw, ah = area
    main_h = int(ah * (1 + bleed) * phone_scale) if not wide else int(ah * 0.98 * phone_scale)
    main_w = K.width_for_height(main_h)
    main_w = min(main_w, int(aw * (0.62 if not wide else 0.80)))
    main_h = K.phone_height(main_w)
    placed = []
    for p in sorted(phones, key=lambda q: q.get("z", 0)):
        pw = int(main_w * p.get("w", 1.0))
        ph_img = K.phone(p["img"], pw, tilt=p.get("tilt", 0))
        cx = ax + aw // 2 + int(aw * p.get("x", 0) * (0.8 if wide else 1.0))
        cy = ay + int(main_h * p.get("y", 0))
        placed.append([ph_img, cx - ph_img.width // 2, cy, pw])
    if wide:  # never let a phone enter the text column; bleed off the right edge instead
        shift = max(0, ax - min(q[1] for q in placed))
        for q in placed:
            q[1] += shift
    for ph_img, x, y0, pw in placed:
        K.paste(canvas, ph_img, (x, y0), shadow_blur=int(pw * 0.06), shadow_opacity=0.42, shadow_offset=(0, int(pw * 0.05)))
    canvas.info["text_bottom"] = text_bottom
    canvas.info["phones_top"] = ay
    return canvas


# ── Backgrounds ──────────────────────────────────────────────────────────────
def bg_dawn(W, H):
    im = K.gradient((W, H), (255, 242, 222), (238, 210, 236))
    land = K.landscape_background((W, int(H * 0.55)), fade_to=None, align=(0.62, 1.0))
    mask = Image.linear_gradient("L").resize((W, int(H * 0.55)))
    im.paste(land, (0, H - land.height), mask)
    im = K.glow(im, (int(W * 0.78), int(H * 0.30)), W * 0.55, (255, 236, 196), 0.5)
    return K.grain(K.bokeh(im, 22, (255, 255, 255), seed=4), 3)


def bg_night(W, H, seed=1):
    im = K.gradient((W, H), (36, 22, 74), (8, 8, 24))
    im = K.glow(im, (int(W * 0.5), int(H * 0.62)), W * 0.75, (120, 80, 170), 0.45)
    K.stars(im, int(160 * W * H / (1080 * 1920)), seed=seed, top_frac=0.75)
    return K.grain(K.bokeh(im, 18, (255, 220, 170), seed=seed, alpha=(10, 40)), 3)


def bg_lavender(W, H):
    im = K.gradient((W, H), (250, 244, 255), (214, 196, 240))
    im = K.glow(im, (int(W * 0.15), int(H * 0.15)), W * 0.7, (255, 236, 214), 0.6)
    return K.grain(K.bokeh(im, 26, (255, 255, 255), seed=8, alpha=(30, 80)), 3)


def bg_art(W, H, n, darken=0.35, blur=0):
    im = K.art_background((W, H), n, blur=blur, darken=darken)
    # readable text: a deep plum veil over the upper part (portrait) or the left part (square / landscape)
    import numpy as np
    if W / H >= 0.95:
        t = np.clip(1 - np.linspace(0, 1, W) / 0.62, 0, 1)[None, :].repeat(H, 0)
    else:
        t = np.clip(1 - np.linspace(0, 1, H) / 0.55, 0, 1)[:, None].repeat(W, 1)
    mask = Image.fromarray((np.clip(t * 1.25, 0, 1) ** 0.7 * 238).astype("uint8"), "L")
    im = Image.composite(Image.new("RGB", (W, H), (22, 12, 40)), im, mask)
    return K.grain(im, 3)


def bg_plum(W, H):
    im = K.gradient((W, H), (66, 36, 110), (16, 9, 30))
    im = K.glow(im, (int(W * 0.85), int(H * 0.1)), W * 0.8, (212, 165, 116), 0.28)
    return K.grain(K.bokeh(im, 20, (238, 196, 120), seed=12, alpha=(10, 36)), 3)


# ── Creatives ────────────────────────────────────────────────────────────────
def c1_daily_wisdom(W, H):
    return layout(W, H, bg=bg_dawn(W, H), pal="dawn", eyebrow="Daily Bible verse", headline="Your Daily Wisdom",
                  sub="A meaningful Bible verse every day — and on your lock screen, too.",
                  phones=[dict(img=lock("render_lock_img08"), w=0.78, x=-0.25, y=0.10, tilt=8, z=0),
                          dict(img=screen("app_home_no_tree_verse"), w=1.0, x=0.10, y=0.0, z=1)])


def c2_lockscreen(W, H):
    return layout(W, H, bg=bg_night(W, H, 2), pal="night", eyebrow="Lock Screen Verse", headline="Wisdom on Your Lock Screen",
                  sub="A new verse appears automatically — every 1, 6, 12 or 24 hours.",
                  chips=("Every 1 h", "6 h", "12 h", "24 h"),
                  phones=[dict(img=lock("render_lock_img03"), w=1.0, x=0.0, y=0.0, z=1)], phone_scale=1.0)


def c3_wallpapers(W, H):
    return layout(W, H, bg=bg_lavender(W, H), pal="lavender", eyebrow="Verse wallpapers", headline="Beautiful Verse Wallpapers",
                  sub="20 artworks, colour themes and a live wallpaper — make every screen yours.",
                  phones=[dict(img=lock("render_lock_img14"), w=0.80, x=-0.30, y=0.09, tilt=7, z=0),
                          dict(img=lock("render_lock_color_royal_purple"), w=0.80, x=0.30, y=0.09, tilt=-7, z=0),
                          dict(img=lock("render_lock_img06"), w=0.95, x=0.0, y=0.0, z=1)])


def c4_companion(W, H):
    return layout(W, H, bg=bg_art(W, H, 4, darken=0.30), pal="plum", eyebrow="Your spiritual companion", headline="Your Daily Spiritual Companion",
                  sub="Daily verse, a mood check-in and a Wisdom Tree that grows with every day you read.",
                  phones=[dict(img=lock("render_lock_img12"), w=0.74, x=0.27, y=0.12, tilt=-8, z=0),
                          dict(img=screen("app_home_with_tree_top"), w=1.0, x=-0.08, y=0.0, z=1)])


def c5_reminders(W, H):
    canvas = layout(W, H, bg=bg_dawn(W, H), pal="dawn", eyebrow="Gentle reminders", headline="Personalized Daily Reminders",
                    sub="A gentle nudge with today’s verse, at the rhythm you choose.",
                    phones=[dict(img=lock("render_lock_img10", notification=True), w=0.76, x=0.27, y=0.10, tilt=-7, z=0),
                            dict(img=screen("app_reminder_notification"), w=1.0, x=-0.10, y=0.0, z=1)])
    return canvas


def c6_premium(W, H):
    return layout(W, H, bg=bg_plum(W, H), pal="plum", eyebrow="Crafted for calm", headline="A Beautiful Experience for Your Faith",
                  sub="Elegant typography, light & dark themes and artwork made for quiet moments.",
                  phones=[dict(img=lock("render_lock_color_midnight"), w=0.78, x=-0.27, y=0.10, tilt=7, z=0),
                          dict(img=screen("app_tradition_dark"), w=1.0, x=0.10, y=0.0, z=1)])


def c7_always(W, H):
    return layout(W, H, bg=bg_art(W, H, 3, darken=0.25), pal="sunset", eyebrow="Ambient Wisdom", headline="Your Faith, Always With You",
                  sub="Your verse on the lock screen and as a calm live wallpaper on your home screen.",
                  phones=[dict(img=lock("render_lock_img01"), w=0.92, x=-0.22, y=0.05, tilt=4, z=1),
                          dict(img=K.homescreen_live("render_live_evening"), w=0.92, x=0.22, y=0.0, tilt=-4, z=0)])


def c8_all(W, H):
    return layout(W, H, bg=bg_lavender(W, H), pal="lavender", eyebrow="All in one app", headline="One App, Daily Inspiration",
                  sub="Everything you need for a daily moment with God’s word.",
                  chips=("Daily verse", "Lock-screen verse", "Live wallpaper", "Reminders", "Wisdom Tree", "Share cards"),
                  phones=[dict(img=lock("render_lock_img16"), w=0.80, x=-0.31, y=0.10, tilt=8, z=0),
                          dict(img=screen("app_ambient_wisdom_live_preview"), w=0.80, x=0.31, y=0.10, tilt=-8, z=0),
                          dict(img=screen("app_home_with_tree_verse"), w=0.95, x=0.0, y=0.0, z=1)])


CREATIVES = [
    ("creative_01_daily_wisdom", c1_daily_wisdom),
    ("creative_02_lockscreen_verse", c2_lockscreen),
    ("creative_03_verse_wallpaper", c3_wallpapers),
    ("creative_04_daily_companion", c4_companion),
    ("creative_05_smart_reminders", c5_reminders),
    ("creative_06_premium_experience", c6_premium),
    ("creative_07_faith_always", c7_always),
    ("creative_08_all_features", c8_all),
]


# ── Feature graphic 1024 x 500 ───────────────────────────────────────────────
def feature_graphic():
    W, H = 1024, 500
    im = K.landscape_background((W, H), align=(0.5, 0.75))
    veil = Image.linear_gradient("L").rotate(90, expand=True).resize((W, H))  # dark left → transparent right
    im = Image.composite(Image.new("RGB", (W, H), (255, 246, 236)), im, veil.point(lambda v: int(v * 0.92)))
    canvas = im.convert("RGBA")
    K.brand_lockup(canvas, (56, 58), size=54, color=K.C["plum_deep"])
    K.text_block(canvas, "Daily Bible verses on your lock screen", (56, 140, 470, 170), "serif", "bolditalic", 52, fill=K.C["plum"], line=1.08, max_lines=3)
    K.text_block(canvas, "Daily verse  ·  Lock-screen verse\nLive wallpaper  ·  Gentle reminders", (56, 318, 450, 80), "sans", "medium", 21, fill=(92, 70, 110), line=1.3)
    # two phones on the right (lock screen in front), cropped by the bottom edge like a Play hero
    back = K.phone(screen("app_home_no_tree_verse"), 190, tilt=-6)
    front = K.phone(lock("render_lock_img08"), 215, tilt=5)
    K.paste(canvas, back, (785, 70), shadow_blur=14, shadow_opacity=0.35, shadow_offset=(0, 10))
    K.paste(canvas, front, (610, 52), shadow_blur=16, shadow_opacity=0.4, shadow_offset=(0, 12))
    return canvas


# ── Lock-screen verse showcase (05) ──────────────────────────────────────────
def lockscreen_showcases():
    out = []
    picks = [("render_lock_img03", "night", "Every unlock, a moment of peace"), ("render_lock_img14", "lavender", "Your verse, beautifully framed"),
             ("render_lock_color_golden", "dawn", "Colour themes or artwork — your choice"), ("render_lock_img18", "plum", "Changes automatically, on your schedule")]
    for i, (render, pal, line) in enumerate(picks, 1):
        W, H = 1080, 1920
        bg = {"night": bg_night, "lavender": bg_lavender, "dawn": bg_dawn, "plum": bg_plum}[pal](W, H) if pal != "night" else bg_night(W, H, i)
        im = layout(W, H, bg=bg, pal=pal, eyebrow="Lock Screen Verse", headline=line,
                    sub="WisdomOne draws today’s verse onto your lock-screen wallpaper.", phones=[dict(img=lock(render), w=1.0, x=0, y=0, z=1)], bleed=0.04)
        out.append((f"lockscreen_verse_{i:02d}", im))
    # the three lock styles side by side (artwork, colour theme, notification)
    W, H = 1080, 1350
    im = layout(W, H, bg=bg_lavender(W, H), pal="lavender", eyebrow="Three ways to see it", headline="Artwork, colour or a quiet reminder",
                sub="Pick the lock-screen style that suits you.",
                phones=[dict(img=lock("render_lock_img06"), w=0.86, x=-0.32, y=0.06, z=0), dict(img=lock("render_lock_color_ocean_blue"), w=0.86, x=0.32, y=0.06, z=0),
                        dict(img=lock("render_lock_img11", notification=True), w=0.9, x=0, y=0, z=1)])
    out.append(("lockscreen_verse_styles_4x5", im))
    return out


def main(only=None):
    for sub in ("01_feature_screenshots", "02_aso_creatives", "03_feature_graphic", "04_promotional_mockups", "05_lockscreen_verse_creatives", "06_final_exports"):
        os.makedirs(os.path.join(OUT, sub), exist_ok=True)
    made = []
    for i, (name, fn) in enumerate(CREATIVES, 1):
        if only and i not in only:
            continue
        for fmt, (W, H) in FORMATS.items():
            im = fn(W, H)
            if fmt == "9x16":
                p = K.save(im, os.path.join(OUT, "02_aso_creatives", f"{name}.jpg"), quality=95)
            else:
                p = K.save(im, os.path.join(OUT, "04_promotional_mockups", f"{name}_{fmt}.jpg"), quality=93)
            made.append(p); print(p)
    if not only:
        fg = feature_graphic()
        made.append(K.save(fg, os.path.join(OUT, "03_feature_graphic", "feature_graphic_1024x500.png")))
        K.save(fg, os.path.join(OUT, "03_feature_graphic", "feature_graphic_1024x500.jpg"), quality=95)
        for name, im in lockscreen_showcases():
            made.append(K.save(im, os.path.join(OUT, "05_lockscreen_verse_creatives", f"{name}.jpg"), quality=93))
        # 06: upload-ready set (Play Console: phone screenshots 9:16 JPEG/PNG ≤ 8 MB, feature graphic 1024x500)
        fin = os.path.join(OUT, "06_final_exports")
        for d in ("play_store_phone_screenshots_1080x1920", "feature_graphic_1024x500", "social_4x5_1080x1350", "social_1x1_1080x1080"):
            os.makedirs(os.path.join(fin, d), exist_ok=True)
        for name, _ in CREATIVES:
            shutil.copy(os.path.join(OUT, "02_aso_creatives", f"{name}.jpg"), os.path.join(fin, "play_store_phone_screenshots_1080x1920"))
            shutil.copy(os.path.join(OUT, "04_promotional_mockups", f"{name}_4x5.jpg"), os.path.join(fin, "social_4x5_1080x1350"))
            shutil.copy(os.path.join(OUT, "04_promotional_mockups", f"{name}_1x1.jpg"), os.path.join(fin, "social_1x1_1080x1080"))
        shutil.copy(os.path.join(OUT, "03_feature_graphic", "feature_graphic_1024x500.png"), os.path.join(fin, "feature_graphic_1024x500"))
    return made


if __name__ == "__main__":
    only = {int(a) for a in sys.argv[1:]} or None
    main(only)
