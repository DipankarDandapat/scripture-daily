#!/usr/bin/env python3
"""Task 5 — the 25 Meta ad images (each in 4:5, 1:1 and 9:16) for WisdomOne_Meta_Ads_Campaign/.

    .venv/bin/python tools/marketing/make_meta_ads.py              # all 25 ads x 3 formats
    .venv/bin/python tools/marketing/make_meta_ads.py AS02_AD01      # one ad

Specs come from campaign_spec.py; every composition style is a different function below (creative diversification).
9:16 versions keep headlines inside the Stories/Reels safe area (top 13 % and bottom 22 % stay free of key text).
"""
from __future__ import annotations

import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wo_kit as K  # noqa: E402
import make_play_store as P  # noqa: E402
import campaign_spec as SPEC  # noqa: E402

OUT = os.path.join(K.ROOT, "WisdomOne_Meta_Ads_Campaign")
NOTE = ("Today’s Bible verse", "They are new every morning: great is thy faithfulness.")
VERSES = {
    "psalm46": ("Be still, and know that I am God.", "Psalms 46:10"),
    "lam3": ("They are new every morning: great is thy faithfulness.", "Lamentations 3:23"),
    "psalm119": ("Thy word is a lamp unto my feet, and a light unto my path.", "Psalms 119:105"),
    "psalm30": ("Weeping may endure for a night, but joy cometh in the morning.", "Psalms 30:5"),
}


def top_for(W, H):
    return 0.13 if H / W > 1.6 else None   # Stories / Reels safe area


def lock(r, note=False):
    return K.lockscreen(r, notification=NOTE if note else None)


def scr(n):
    return K.src(n)


def base(W, H, ad, bg, headline_family=("serif", "bolditalic"), **kw):
    return P.layout(W, H, bg=bg, pal=ad["pal"], eyebrow=kw.pop("eyebrow", ad["hook"]), headline=ad["headline"], sub=kw.pop("sub"),
                    top_frac=top_for(W, H), headline_family=headline_family, **kw)


def veil(im, frac=0.55, strength=230, color=(20, 10, 36), side=False):
    W, H = im.size
    if side:
        t = np.clip(1 - np.linspace(0, 1, W) / frac, 0, 1)[None, :].repeat(H, 0)
    else:
        t = np.clip(1 - np.linspace(0, 1, H) / frac, 0, 1)[:, None].repeat(W, 1)
    mask = Image.fromarray((np.clip(t * 1.25, 0, 1) ** 0.7 * strength).astype("uint8"), "L")
    return Image.composite(Image.new("RGB", (W, H), color), im.convert("RGB"), mask)


# ── Compositions ─────────────────────────────────────────────────────────────
def hero_dawn(W, H, ad):
    return base(W, H, ad, P.bg_dawn(W, H), sub="A new Bible verse every morning — on your phone and your lock screen.",
                phones=[dict(img=lock("render_lock_img12"), w=0.76, x=0.27, y=0.11, tilt=-7, z=0),
                        dict(img=scr("app_home_no_tree_top"), w=1.0, x=-0.09, y=0, z=1)])


def typography(W, H, ad, verse="lam3", night=False):
    """Typography-first: a catalogue verse set large; a small lock-screen phone anchors the product."""
    text, ref = VERSES[verse]
    if night:
        bg = P.bg_night(W, H, 7); tc, sc, acc = K.C["cream"], (200, 196, 230), K.C["gold_bright"]
    else:
        bg = K.grain(K.glow(K.gradient((W, H), (252, 246, 236), (238, 222, 200)), (int(W * 0.2), int(H * 0.1)), W * 0.8, (255, 255, 255), 0.6), 3)
        tc, sc, acc = K.C["plum_deep"], (110, 88, 70), K.C["purple"]
    im = bg.convert("RGBA")
    m = int(W * 0.08)
    wide = W / H >= 0.95
    y = int(H * (top_for(W, H) or 0.07))
    K.brand_lockup(im, (m, y), size=int(W * 0.048), color=tc)
    y += int(W * 0.1)
    d = ImageDraw.Draw(im)
    d.text((m - int(W * 0.008), y - int(W * 0.03)), "“", font=K.font("didot", "bold", int(W * 0.2)), fill=acc + (110,) if im.mode == "RGBA" else acc)
    y += int(W * 0.11)
    col_w = int(W * (0.52 if wide else 0.84))
    y, _ = K.text_block(im, ad["headline"] if night else text, (m, y, col_w, int(H * (0.42 if wide else 0.30))), "didot", "regular" if not night else "bold",
                        int(W * (0.085 if not night else 0.09)), fill=tc, line=1.12, max_lines=6)
    if not night:
        d.text((m, y + int(H * 0.012)), "— " + ref, font=K.font("sans", "demi", int(W * 0.03)), fill=acc)
        y += int(H * 0.06)
        y, _ = K.text_block(im, ad["headline"], (m, y + int(H * 0.02), col_w, int(H * 0.12)), "serif", "bolditalic", int(W * 0.06), fill=tc, line=1.1, max_lines=2)
    else:
        y, _ = K.text_block(im, "Today’s verse, on your lock screen and in the app.", (m, y + int(H * 0.02), col_w, int(H * 0.1)), "sans", "medium", int(W * 0.036), fill=sc, line=1.3)
    # small lock phone, bottom-right, bleeding off the edge
    pw = int(W * (0.44 if H / W > 1.6 else (0.36 if not wide else 0.30)))
    ph = K.phone(lock("render_lock_img04" if not night else "render_lock_img03"), pw, tilt=-8)
    py = H - int(K.phone_height(pw) * (0.80 if wide else 0.72))
    K.paste(im, ph, (W - int(pw * 0.95), max(py, y + int(H * 0.03)) if not wide else py), shadow_blur=int(pw * 0.07), shadow_opacity=0.4)
    return im


def typography_night(W, H, ad):
    return typography(W, H, ad, verse="psalm46", night=True)


def cinematic(W, H, ad):
    """Cinematic: full-bleed app artwork, letterbox bars, centred serif line, lock phone rising from the bottom."""
    bg = K.art_background((W, H), ad.get("art", 3), align=(0.5, 0.45))
    bg = veil(bg, 0.5, 180) if H / W > 1 else veil(bg, 0.62, 200, side=True)
    im = K.grain(bg, 4).convert("RGBA")
    bar = int(H * 0.035)
    d = ImageDraw.Draw(im)
    d.rectangle([0, 0, W, bar], fill=(0, 0, 0, 255)); d.rectangle([0, H - bar, W, H], fill=(0, 0, 0, 255))
    wide = W / H >= 0.95
    m = int(W * 0.08)
    y = int(H * (top_for(W, H) or 0.09))
    K.brand_lockup(im, (m if wide else W // 2, y), size=int(W * 0.046), color=K.C["cream"], anchor="lt" if wide else "mt")
    y += int(W * 0.1)
    bw = int(W * (0.42 if wide else 0.84))
    y, _ = K.text_block(im, ad["headline"], (m, y, bw, int(H * 0.26)), "bask", "italic", int(W * 0.085), fill=K.C["cream"], align="left" if wide else "center", line=1.1, max_lines=3, shadow_px=3)
    y, _ = K.text_block(im, ad["primary"].split(". ")[0] + ".", (m, y + int(H * 0.015), bw, int(H * 0.12)), "sans", "medium", int(W * 0.034), fill=(236, 226, 246),
                        align="left" if wide else "center", line=1.32, max_lines=3)
    pw = int(W * (0.36 if wide else (0.50 if H / W > 1.6 else 0.42)))
    ph = K.phone(lock(ad.get("lock", "render_lock_img10")), pw)
    if wide:
        x, yy = int(W * 0.77) - ph.width // 2, int(H * 0.08)
    else:
        x, yy = W // 2 - ph.width // 2, max(y + int(H * 0.035), H - int(K.phone_height(pw) * 0.86))
    K.paste(im, ph, (x, yy), shadow_blur=int(pw * 0.08), shadow_opacity=0.55)
    return im


def verse_card(W, H, ad):
    """The app's real verse card, cropped from Home and enlarged, beside a lock screen."""
    bg = P.bg_plum(W, H).convert("RGBA")
    card = scr("app_home_no_tree_verse").crop((40, 330, 1040, 1310))
    cw = int(W * (0.80 if H / W > 1 else 0.52))
    card = card.resize((cw, int(cw * card.height / card.width)), Image.LANCZOS).convert("RGBA")
    card.putalpha(K.rounded_mask(card.size, int(cw * 0.07)))
    wide = W / H >= 0.95
    im = P.layout(W, H, bg=bg, pal="plum", eyebrow=ad["hook"], headline=ad["headline"], sub="Read it, save it, share it — or tap “Explain this to me like I’m 5”.",
                  phones=[dict(img=lock("render_lock_img16"), w=0.8, x=0.30 if not wide else 0.15, y=0.04 if wide else 0.18, tilt=-6, z=0)],
                  top_frac=top_for(W, H), phone_scale=0.92)
    cw = int(W * (0.66 if not wide else 0.40))
    card = card.resize((cw, int(cw * card.height / card.width)), Image.LANCZOS)
    if wide:
        K.paste(im, card, (int(W * 0.50), H - card.height - int(H * 0.06)), shadow_blur=24, shadow_opacity=0.5)
    else:
        K.paste(im, card, (int(W * 0.06), im.info["phones_top"] + int(H * 0.06)), shadow_blur=28, shadow_opacity=0.5)
    return im


def minimal_cta(W, H, ad):
    return base(W, H, ad, P.bg_lavender(W, H), sub="Free on Google Play. A fresh Bible verse every day.", force_cta=True, cta_text="Install free on Google Play",
                phones=[dict(img=lock("render_lock_img08"), w=0.74, x=-0.27, y=0.12, tilt=7, z=0),
                        dict(img=scr("app_home_with_tree_card"), w=0.95, x=0.08, y=0, z=1)])


def lock_fullbleed(W, H, ad):
    """Frameless: the complete real lock screen as a glowing card over a blurred copy of itself."""
    ls = lock(ad["lock"])
    bg = K.cover(ls, (W, H), (0.5, 0.5)).filter(ImageFilter.GaussianBlur(W * 0.035))
    im = K.grain(Image.blend(bg, Image.new("RGB", (W, H), (6, 8, 26)), 0.55), 3).convert("RGBA")
    wide = W / H >= 0.95
    m = int(W * 0.08)
    y = int(H * (top_for(W, H) or 0.07))
    if wide:
        K.brand_lockup(im, (m, int(H * 0.12)), size=int(W * 0.046), color=K.C["cream"])
        y, _ = K.text_block(im, ad["headline"], (m, int(H * 0.22), int(W * 0.44), int(H * 0.36)), "serif", "bolditalic", int(W * 0.08), fill=K.C["cream"], line=1.06, max_lines=3)
        y, _ = K.text_block(im, "Today’s Bible verse, drawn right onto your lock-screen wallpaper.", (m, y + 12, int(W * 0.42), int(H * 0.2)), "sans", "medium",
                            int(W * 0.034), fill=(214, 210, 238), line=1.3)
        K.pill(im, "Real WisdomOne lock screen", (m, y + int(H * 0.03)), size=int(W * 0.024), bg=(255, 255, 255), fg=K.C["cream"], alpha=40)
        area = (int(W * 0.55), int(H * 0.06), int(W * 0.40), int(H * 0.88))
    else:
        K.brand_lockup(im, (W // 2, y), size=int(W * 0.046), color=K.C["cream"], anchor="mt")
        y += int(W * 0.09)
        y, _ = K.text_block(im, ad["headline"], (m, y, W - 2 * m, int(H * 0.16)), "serif", "bolditalic", int(W * 0.088), fill=K.C["cream"], align="center", line=1.06, max_lines=2)
        b = K.pill(im, "Real WisdomOne lock screen", (W // 2, y + int(H * 0.008)), size=int(W * 0.024), bg=(255, 255, 255), fg=K.C["cream"], alpha=40, anchor="mt")
        area = (m, b[3] + int(H * 0.03), W - 2 * m, H - b[3] - int(H * (0.05 if H / W < 1.6 else 0.07)))
    ax, ay, aw, ah = area
    ch = ah; cw = int(ch * 1080 / 2400)
    if cw > aw:
        cw = aw; ch = int(cw * 2400 / 1080)
    card = ls.resize((cw, ch), Image.LANCZOS).convert("RGBA")
    card.putalpha(K.rounded_mask(card.size, int(cw * 0.09)))
    halo = Image.new("RGBA", (cw + 8, ch + 8), (0, 0, 0, 0))
    ImageDraw.Draw(halo).rounded_rectangle([0, 0, cw + 7, ch + 7], radius=int(cw * 0.09) + 4, outline=(238, 196, 120, 150), width=3)
    x = ax + (aw - cw) // 2
    K.paste(im, card, (x, ay), shadow_blur=int(cw * 0.08), shadow_opacity=0.6)
    im.alpha_composite(halo, (x - 4, ay - 4))
    return im


def unlock_hero(W, H, ad):
    im = base(W, H, ad, P.bg_night(W, H, 9), sub="A new verse appears automatically — every 1, 6, 12 or 24 hours.",
              chips=("No need to open the app", "Refreshes automatically"),
              phones=[dict(img=lock(ad["lock"]), w=1.0, x=0, y=0, z=1)], bleed=0.06)
    return im


def duo_personal(W, H, ad):
    return base(W, H, ad, P.bg_dawn(W, H), sub="Choose artwork, colour theme, text size and placement.",
                phones=[dict(img=scr("app_wallpaper_gallery"), w=0.86, x=-0.25, y=0.08, tilt=6, z=0),
                        dict(img=lock("render_lock_img05"), w=0.98, x=0.17, y=0, z=1)])


def fan3(W, H, ad):
    a, b, c = ad["locks"]
    bg = P.bg_lavender(W, H) if ad["pal"] == "lavender" else K.grain(K.glow(K.gradient((W, H), (252, 244, 230), (226, 204, 170)), (W // 2, int(H * 0.7)), W * 0.8, (255, 236, 200), 0.5), 3)
    return base(W, H, ad, bg, sub=ad["description"] + " — your verse changes with every refresh.",
                phones=[dict(img=lock(a), w=0.82, x=-0.31, y=0.09, tilt=9, z=0), dict(img=lock(c), w=0.82, x=0.31, y=0.09, tilt=-9, z=0),
                        dict(img=lock(b), w=0.96, x=0, y=0, z=1)])


def before_after(W, H, ad):
    """Factual before/after: a plain default lock screen vs. the same lock screen with WisdomOne."""
    plain = K.gradient((1080, 2400), (74, 84, 110), (28, 32, 46))
    before = K.lockscreen_from(plain)
    after = lock(ad["lock"])
    bg = K.grain(K.glow(K.gradient((W, H), (238, 246, 236), (186, 214, 192)), (int(W * 0.7), int(H * 0.2)), W * 0.8, (255, 255, 240), 0.5), 3)
    im = base(W, H, ad, bg, sub="Same phone. A lock screen that greets you with God’s word.",
              phones=[dict(img=before, w=0.86, x=-0.24, y=0.05, tilt=0, z=0), dict(img=after, w=0.86, x=0.24, y=0.05, tilt=0, z=1)], bleed=0.04, phone_scale=0.88)
    # labels sit on top of each phone (outside the Reels bottom UI zone)
    wide = W / H >= 0.95
    fs = int(W * 0.028)
    if wide:
        K.pill(im, "Before", (int(W * 0.545), int(H * 0.035)), size=fs, bg=(255, 255, 255), fg=K.C["ink_mid"], alpha=235)
        K.pill(im, "With WisdomOne", (W - int(W * 0.04), int(H * 0.035)), size=fs, bg=K.C["purple"], fg=K.C["white"], anchor="rt")
    else:
        yb = im.info["phones_top"] - int(fs * 1.2)
        K.pill(im, "Before", (int(W * 0.26), yb), size=fs, bg=(255, 255, 255), fg=K.C["ink_mid"], anchor="mt", alpha=235)
        K.pill(im, "With WisdomOne", (int(W * 0.74), yb), size=fs, bg=K.C["purple"], fg=K.C["white"], anchor="mt")
    return im


def wall_grid(W, H, ad):
    """Gallery wall: four real verse wallpapers as framed tiles."""
    bg = P.bg_plum(W, H).convert("RGBA")
    m = int(W * 0.07)
    wide = W / H >= 0.95
    y = int(H * (top_for(W, H) or 0.06))
    K.brand_lockup(bg, (W // 2, y), size=int(W * 0.046), color=K.C["cream"], anchor="mt")
    y += int(W * 0.09)
    y, _ = K.text_block(bg, ad["headline"], (m, y, W - 2 * m, int(H * 0.14)), "serif", "bolditalic", int(W * 0.085), fill=K.C["cream"], align="center", line=1.05, max_lines=2)
    y, _ = K.text_block(bg, "Calm artwork + a Bible verse, refreshed with every new verse.", (m, y + 6, W - 2 * m, int(H * 0.08)), "sans", "medium", int(W * 0.034),
                        fill=(214, 200, 236), align="center", line=1.3)
    cols = 4 if wide else 2
    gap = int(W * 0.03)
    tile_w = (W - 2 * m - gap * (cols - 1)) // cols
    tile_h = int(tile_w * 2400 / 1080)
    avail = H - y - int(H * 0.04)
    rows = 1 if wide else 2
    if tile_h * rows + gap * (rows - 1) > avail:
        tile_h = (avail - gap * (rows - 1)) // rows
        tile_w = int(tile_h * 1080 / 2400)
    total_w = cols * tile_w + (cols - 1) * gap
    x0 = (W - total_w) // 2
    y0 = y + int(H * 0.03)
    for i, r in enumerate(ad["locks"][: cols * rows]):
        t = K.src(r).resize((tile_w, tile_h), Image.LANCZOS).convert("RGBA")
        t.putalpha(K.rounded_mask(t.size, int(tile_w * 0.09)))
        cx = x0 + (i % cols) * (tile_w + gap); cy = y0 + (i // cols) * (tile_h + gap)
        K.paste(bg, t, (cx, cy), shadow_blur=int(tile_w * 0.06), shadow_opacity=0.5)
    return bg


def day_night(W, H, ad, routine=False):
    """Two home screens with the live wallpaper — morning and night (Ambient Wisdom live wallpaper themes)."""
    if routine:
        bg = P.bg_dawn(W, H); sub = "Morning or evening — choose how often your verse refreshes: every 1, 6, 12 or 24 h."
        imgs = (K.homescreen_live("render_live_morning"), lock("render_lock_img11"))
        labels = ("Morning", "Evening")
    else:
        bg = K.grain(K.gradient((W, H), (206, 226, 246), (54, 66, 128)), 3); sub = "A live wallpaper that follows your day, with today’s verse on your home screen."
        imgs = (K.homescreen_live("render_live_morning"), K.homescreen_live("render_live_night"))
        labels = ("Morning", "Night")
    im = base(W, H, ad, bg, sub=sub, phones=[dict(img=imgs[0], w=0.9, x=-0.23, y=0.03, tilt=4, z=1), dict(img=imgs[1], w=0.9, x=0.23, y=0.0, tilt=-4, z=0)], bleed=0.06)
    fs = int(W * 0.028)
    wide = W / H >= 0.95
    yl = im.info["phones_top"] - int(fs * 1.2)
    if not wide:
        K.pill(im, labels[0], (int(W * 0.27), yl), size=fs, bg=(255, 255, 255), fg=K.C["plum"], anchor="mt", alpha=235)
        K.pill(im, labels[1], (int(W * 0.73), yl), size=fs, bg=K.C["plum"], fg=K.C["white"], anchor="mt")
    return im


def day_night_routine(W, H, ad):
    return day_night(W, H, ad, routine=True)


def split_gallery(W, H, ad):
    return base(W, H, ad, P.bg_lavender(W, H), sub="20 artworks · 6 colour themes · text size & placement.",
                chips=("Artwork", "Colour themes", "Live wallpaper"),
                phones=[dict(img=scr("app_lockscreen_designer"), w=1.0, x=-0.13, y=0, z=1), dict(img=lock("render_lock_color_golden"), w=0.72, x=0.30, y=0.13, tilt=-8, z=0)])


def notifications(W, H, ad):
    """Floating notification stack over calm artwork, with the lock screen that shows it."""
    bg = veil(K.art_background((W, H), ad.get("art", 10), blur=int(W * 0.012)), 0.6, 170, (16, 30, 24))
    im = K.grain(bg, 3).convert("RGBA")
    wide = W / H >= 0.95
    m = int(W * 0.08)
    y = int(H * (top_for(W, H) or 0.07))
    K.brand_lockup(im, (m, y), size=int(W * 0.046), color=K.C["cream"])
    y += int(W * 0.1)
    col = int(W * (0.46 if wide else 0.84))
    y, _ = K.text_block(im, ad["headline"], (m, y, col, int(H * 0.2)), "serif", "bolditalic", int(W * 0.088), fill=K.C["cream"], line=1.06, max_lines=3, shadow_px=2)
    y, _ = K.text_block(im, "A quiet notification with today’s verse. No noise, no pressure.", (m, y + 8, col, int(H * 0.1)), "sans", "medium", int(W * 0.035), fill=(226, 236, 228), line=1.3)
    nw = int(W * (0.44 if wide else (0.80 if H / W > 1.6 else 0.60)))
    cards = [K.notification_card("Today’s Bible verse", "They are new every morning: great is thy faithfulness.", nw),
             K.notification_card("Today’s Bible verse", "Be still, and know that I am God.", int(nw * 0.94))]
    pw = int(W * (0.34 if wide else 0.44))
    ph = K.phone(lock("render_lock_img13", note=True), pw)
    if wide:
        K.paste(im, ph, (int(W * 0.60), int(H * 0.10)), shadow_blur=22, shadow_opacity=0.5)
        cy = int(H * 0.62)
        for i, c in enumerate(cards):
            K.paste(im, c, (m + i * int(W * 0.03), cy + i * int(c.height * 1.1)), shadow_blur=18, shadow_opacity=0.35)
    else:
        cy = y + int(H * 0.03)
        for i, c in enumerate(cards):
            K.paste(im, c, (m + i * int(W * 0.04), cy + i * int(c.height * 1.12)), shadow_blur=18, shadow_opacity=0.35)
        K.paste(im, ph, (W - int(pw * 0.95), H - int(K.phone_height(pw) * (0.55 if H / W > 1.6 else 0.48))), shadow_blur=22, shadow_opacity=0.5)
    return im


def duo_reminder(W, H, ad):
    return base(W, H, ad, K.grain(K.gradient((W, H), (222, 236, 248), (132, 170, 214)), 3), sub="Today’s verse as a gentle notification — and on your lock screen.",
                phones=[dict(img=scr("app_reminder_notification"), w=0.95, x=-0.15, y=0, z=1), dict(img=lock("render_lock_img09", note=True), w=0.8, x=0.27, y=0.1, tilt=-7, z=0)])


def tree_focus(W, H, ad):
    """Wisdom Tree: the real tree card magnified, with the home screen that holds it."""
    im = base(W, H, ad, P.bg_dawn(W, H), sub="Read today’s verse and watch your Wisdom Tree grow — sprout, leaves, branches, flowers, fruit.",
              phones=[dict(img=scr("app_home_with_tree_top"), w=0.92, x=-0.18, y=0.02, z=0), dict(img=lock("render_lock_img20"), w=0.7, x=0.32, y=0.16, tilt=-6, z=1)],
              phone_scale=0.96)
    card = scr("app_home_with_tree_card").crop((50, 360, 1030, 1430))
    wide = W / H >= 0.95
    cw = int(W * (0.40 if wide else 0.56))
    card = card.resize((cw, int(cw * card.height / card.width)), Image.LANCZOS).convert("RGBA")
    card.putalpha(K.rounded_mask(card.size, int(cw * 0.07)))
    pos = (int(W * 0.06), int(H * 0.58)) if wide else (int(W * 0.40), int(H * (0.62 if H / W > 1.6 else 0.58)))
    K.paste(im, card, pos, shadow_blur=26, shadow_opacity=0.4)
    return im


def product_grid(W, H, ad):
    """Product collage: four real screens (home, tree, lock screen, ambient settings)."""
    bg = P.bg_lavender(W, H).convert("RGBA")
    wide = W / H >= 0.95
    m = int(W * 0.07)
    y = int(H * (top_for(W, H) or 0.06))
    K.brand_lockup(bg, (W // 2, y), size=int(W * 0.05), color=K.C["plum_deep"], anchor="mt")
    y += int(W * 0.095)
    y, _ = K.text_block(bg, ad["headline"], (m, y, W - 2 * m, int(H * 0.1)), "serif", "bolditalic", int(W * 0.1), fill=K.C["plum_deep"], align="center", line=1.0, max_lines=1)
    y, _ = K.text_block(bg, "Daily verse · Lock screen · Live wallpaper · Wisdom Tree", (m, y + 4, W - 2 * m, int(H * 0.06)), "sans", "demi", int(W * 0.032),
                        fill=(90, 78, 118), align="center", line=1.3, max_lines=2)
    shots = [scr("app_home_no_tree_verse"), lock("render_lock_img08"), scr("app_home_with_tree_top"), K.homescreen_live("render_live_evening")]
    n = 4
    gap = int(W * 0.025)
    avail_h = H - y - int(H * 0.05)
    pw = (W - 2 * m - gap * (n - 1)) // n
    if K.phone_height(pw) > avail_h:
        pw = K.width_for_height(avail_h)
    total = n * pw + (n - 1) * gap
    x = (W - total) // 2
    for i, s in enumerate(shots):
        ph = K.phone(s, pw)
        off = int(H * 0.025) if i % 2 else 0
        K.paste(bg, ph, (x + i * (pw + gap), y + int(H * 0.03) + off), shadow_blur=int(pw * 0.07), shadow_opacity=0.35)
    return bg


def feature_list(W, H, ad):
    """Phone + a list of the real features with drawn icons."""
    bg = P.bg_plum(W, H).convert("RGBA")
    wide = W / H >= 0.95
    m = int(W * 0.075)
    y = int(H * (top_for(W, H) or 0.06))
    K.brand_lockup(bg, (m, y), size=int(W * 0.046), color=K.C["cream"])
    y += int(W * 0.095)
    col = int(W * (0.50 if wide else 0.84))
    y, _ = K.text_block(bg, ad["headline"], (m, y, col, int(H * 0.18)), "serif", "bolditalic", int(W * 0.085), fill=K.C["cream"], line=1.05, max_lines=3)
    feats = [("Daily Bible verse", "Fresh every day"), ("Lock-screen verse", "Artwork or colour themes"), ("Live wallpaper", "Sky follows your day"),
             ("Gentle reminders", "1, 6, 12 or 24 hours"), ("Wisdom Tree", "Grows as you read"), ("Explain like I’m 5", "Simple meaning of a verse")]
    if not wide and H / W < 1.4:
        feats = feats[:4]
    if wide:
        feats = feats[:5]
    rw = col if wide else int(W * 0.54)
    d = ImageDraw.Draw(bg)
    fs = int(W * (0.034 if not wide else 0.028)); ss = int(fs * 0.8)
    y += int(H * 0.02)
    for t, s in feats:
        r = int(fs * 0.9)
        d.ellipse([m, y, m + 2 * r, y + 2 * r], fill=K.C["gold"])
        d.line([(m + r * 0.55, y + r * 1.0), (m + r * 0.9, y + r * 1.35), (m + r * 1.5, y + r * 0.65)], fill=K.C["plum_deep"], width=max(3, r // 4))
        d.text((m + 2 * r + int(fs * 0.6), y - 2), t, font=K.font("sans", "demi", fs), fill=K.C["cream"])
        d.text((m + 2 * r + int(fs * 0.6), y + int(fs * 1.25)), s, font=K.font("sans", "regular", ss), fill=(200, 188, 226))
        y += int(fs * 2.9)
    pw = int(W * (0.34 if wide else 0.42))
    ph = K.phone(lock("render_lock_img17") if not wide else scr("app_home_with_tree_verse"), pw)
    pos = (int(W * 0.62), int(H * 0.10)) if wide else (W - int(pw * 0.98), H - int(K.phone_height(pw) * (0.72 if H / W > 1.6 else 0.80)))
    K.paste(bg, ph, pos, shadow_blur=22, shadow_opacity=0.5)
    if wide:
        small = K.phone(lock("render_lock_img17"), int(pw * 0.7), tilt=-8)
        K.paste(bg, small, (int(W * 0.80), int(H * 0.40)), shadow_blur=18, shadow_opacity=0.5)
    return bg


def split_dark(W, H, ad):
    return base(W, H, ad, P.bg_night(W, H, 13), sub="Light or dark — every screen is crafted for calm and focus.", headline_family=("didot", "italic"),
                phones=[dict(img=scr("app_settings_dark"), w=1.0, x=0.12, y=0, z=1), dict(img=lock("render_lock_color_midnight"), w=0.74, x=-0.30, y=0.12, tilt=7, z=0)])


def duo_ambient(W, H, ad):
    bg = veil(K.art_background((W, H), ad.get("art", 1)), 0.55, 200)
    return base(W, H, ad, K.grain(bg, 3), sub="Lock screen when you check the time, live wallpaper when you open an app.",
                phones=[dict(img=lock("render_lock_img07"), w=0.92, x=-0.21, y=0.04, tilt=4, z=1), dict(img=K.homescreen_live("render_live_afternoon"), w=0.9, x=0.22, y=0, tilt=-4, z=0)])


def install_hero(W, H, ad):
    bg = K.landscape_background((W, H), fade_to=(255, 246, 236), align=(0.6, 1.0))
    return base(W, H, ad, K.grain(bg, 3), sub="Your first verse arrives in seconds. Free on Google Play.", force_cta=True, cta_text="Install WisdomOne free",
                eyebrow="Free to install",
                phones=[dict(img=lock("render_lock_img12"), w=0.78, x=0.27, y=0.10, tilt=-7, z=0), dict(img=scr("app_home_with_tree_verse"), w=0.98, x=-0.08, y=0, z=1)])


STYLES = {f.__name__: f for f in (hero_dawn, typography, typography_night, cinematic, verse_card, minimal_cta, lock_fullbleed, unlock_hero, duo_personal, fan3,
                                  before_after, wall_grid, day_night, day_night_routine, split_gallery, notifications, duo_reminder, tree_focus, product_grid,
                                  feature_list, split_dark, duo_ambient, install_hero)}


def render(ad):
    s = SPEC.ad_set(ad["set"])
    folder = os.path.join(OUT, s["folder"], "images")
    files = SPEC.image_files(ad)
    out = []
    for fmt, (W, H) in SPEC.FORMATS.items():
        im = STYLES[ad["style"]](W, H, ad)
        out.append(K.save(im, os.path.join(folder, files[fmt]), quality=92))
    return out


if __name__ == "__main__":
    want = set(sys.argv[1:])
    for ad in SPEC.ADS:
        name = f"{ad['set']}_AD{ad['no']:02d}"
        if want and name not in want:
            continue
        for p in render(ad):
            print(p.replace(K.ROOT + "/", ""))
