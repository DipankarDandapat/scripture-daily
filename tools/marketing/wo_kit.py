"""WisdomOne marketing toolkit: brand, phone mock-ups, lock-screen composites, backgrounds and typography.

Shared by tools/marketing/make_play_store.py (Task 1) and tools/marketing/make_meta_ads.py (Task 5).

Inputs are *real* app material only:
  * play_store_creatives/01_feature_screenshots/app_*.png       — screenshots from the running app (Android emulator)
  * play_store_creatives/01_feature_screenshots/render_*.jpg    — frames drawn by the app's own LockScreenVerse /
                                                                  LiveWallpaperRenderer code at 1080 x 2400
  * android/app/src/main/assets/wallpapers/christianity/*.webp  — the app's bundled artwork
  * design/settings_header_landscape.jpg                        — approved design art
Requires: pip install pillow numpy   (fonts: macOS system fonts Avenir Next / Palatino / Didot / Baskerville)
"""
from __future__ import annotations

import math
import os
import random
from functools import lru_cache

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SRC = os.path.join(ROOT, "play_store_creatives", "01_feature_screenshots")
ART = os.path.join(ROOT, "android", "app", "src", "main", "assets", "wallpapers", "christianity")
LOGO = os.path.join(ROOT, "docs", "assets", "logo.png")
DESIGN = os.path.join(ROOT, "design")
PLAY_URL = "https://play.google.com/store/apps/details?id=com.scripturedaily"

# ── Brand ────────────────────────────────────────────────────────────────────
C = {
    "plum": (45, 27, 78), "plum_deep": (26, 15, 46), "purple": (75, 46, 131), "lavender": (196, 176, 232),
    "gold": (212, 165, 116), "gold_bright": (238, 196, 120), "parchment": (248, 242, 233), "cream": (255, 248, 236),
    "ink": (30, 21, 48), "ink_mid": (92, 82, 112), "white": (255, 255, 255), "rose": (232, 170, 160),
    "sage": (78, 122, 98), "ocean": (30, 70, 110), "night": (10, 16, 40),
}

PALETTES = {  # (top, bottom, accent, text, subtext)
    "dawn":     ((255, 236, 210), (236, 196, 220), C["purple"], C["plum_deep"], (92, 70, 110)),
    "lavender": ((246, 238, 252), (214, 196, 238), C["gold"], C["plum_deep"], (90, 78, 118)),
    "plum":     ((58, 32, 98), (18, 10, 34), C["gold_bright"], C["cream"], (214, 200, 236)),
    "night":    ((14, 22, 56), (4, 6, 18), C["gold_bright"], C["cream"], (196, 204, 236)),
    "sunset":   ((250, 196, 150), (110, 52, 104), C["cream"], C["white"], (255, 236, 220)),
    "sage":     ((232, 242, 230), (176, 206, 182), C["plum"], (24, 44, 34), (62, 92, 74)),
    "ocean":    ((214, 232, 246), (120, 160, 206), C["plum"], (16, 34, 62), (52, 80, 112)),
    "sand":     ((252, 244, 230), (232, 212, 182), C["purple"], (52, 36, 28), (110, 88, 70)),
    "midnight": ((36, 30, 60), (8, 6, 18), C["lavender"], C["white"], (200, 196, 222)),
}

_FONTS = {
    "sans": ("/System/Library/Fonts/Avenir Next.ttc", {"bold": 0, "demi": 2, "medium": 5, "regular": 7, "heavy": 8, "light": 10, "italic": 4}),
    "serif": ("/System/Library/Fonts/Palatino.ttc", {"regular": 0, "italic": 1, "bold": 2, "bolditalic": 3}),
    "didot": ("/System/Library/Fonts/Supplemental/Didot.ttc", {"regular": 0, "italic": 1, "bold": 2}),
    "bask": ("/System/Library/Fonts/Supplemental/Baskerville.ttc", {"regular": 0, "bold": 1, "italic": 2, "bolditalic": 3, "semibold": 4, "semibolditalic": 5}),
}


@lru_cache(maxsize=512)
def font(family: str, style: str, size: int) -> ImageFont.FreeTypeFont:
    path, idx = _FONTS[family]
    return ImageFont.truetype(path, max(6, int(size)), index=idx.get(style, 0))


# ── Sources ──────────────────────────────────────────────────────────────────
@lru_cache(maxsize=64)
def src(name: str) -> Image.Image:
    """A file from 01_feature_screenshots (app_*.png / render_*.jpg)."""
    for ext in ("", ".png", ".jpg"):
        p = os.path.join(SRC, name + ext)
        if os.path.exists(p):
            return Image.open(p).convert("RGB")
    raise FileNotFoundError(name)


@lru_cache(maxsize=32)
def art(n: int) -> Image.Image:
    return Image.open(os.path.join(ART, f"{n:02d}.webp")).convert("RGB")


@lru_cache(maxsize=4)
def logo() -> Image.Image:
    return Image.open(LOGO).convert("RGBA")


@lru_cache(maxsize=8)
def design(name: str) -> Image.Image:
    return Image.open(os.path.join(DESIGN, name)).convert("RGB")


def clean_status_bar(img: Image.Image) -> Image.Image:
    """Removes debug/emulator status icons from a 1080x2400 screenshot, keeping the clock and battery (demo mode 9:41)."""
    im = img.copy()
    w, h = im.size
    bg = im.getpixel((w // 2, int(h * 0.025)))
    ImageDraw.Draw(im).rectangle([int(w * 0.16), 0, int(w * 0.78), int(h * 0.045)], fill=bg)
    return im


# ── Geometry helpers ─────────────────────────────────────────────────────────
def rounded_mask(size, radius) -> Image.Image:
    s = 4  # supersample for smooth corners
    m = Image.new("L", (size[0] * s, size[1] * s), 0)
    ImageDraw.Draw(m).rounded_rectangle([0, 0, size[0] * s - 1, size[1] * s - 1], radius=radius * s, fill=255)
    return m.resize(size, Image.LANCZOS)


def cover(img: Image.Image, size, align=(0.5, 0.5)) -> Image.Image:
    w, h = size
    s = max(w / img.width, h / img.height)
    r = img.resize((max(1, round(img.width * s)), max(1, round(img.height * s))), Image.LANCZOS)
    x = int((r.width - w) * align[0]); y = int((r.height - h) * align[1])
    return r.crop((x, y, x + w, y + h))


def shadow(layer: Image.Image, blur: int, opacity: float = 0.45, color=(10, 6, 20), grow: int = 0) -> Image.Image:
    a = layer.split()[-1]
    pad = blur * 3 + grow
    big = Image.new("L", (layer.width + pad * 2, layer.height + pad * 2), 0)
    big.paste(a, (pad, pad))
    if grow:
        big = big.filter(ImageFilter.MaxFilter(grow * 2 + 1 if grow * 2 + 1 <= 25 else 25))
    big = big.filter(ImageFilter.GaussianBlur(blur)).point(lambda v: int(v * opacity))
    out = Image.new("RGBA", big.size, color + (0,))
    out.putalpha(big)
    return out


def paste(canvas: Image.Image, layer: Image.Image, xy, shadow_blur=0, shadow_opacity=0.45, shadow_offset=(0, 0), anchor="lt"):
    """Pastes an RGBA layer (optionally with a soft drop shadow). anchor: lt | mt | mm | mb."""
    x, y = xy
    if anchor == "mm": x -= layer.width // 2; y -= layer.height // 2
    elif anchor == "mt": x -= layer.width // 2
    elif anchor == "mb": x -= layer.width // 2; y -= layer.height
    if shadow_blur:
        sh = shadow(layer, shadow_blur, shadow_opacity)
        pad = (sh.width - layer.width) // 2
        canvas.alpha_composite(sh, (x - pad + shadow_offset[0], y - pad + shadow_offset[1])) if canvas.mode == "RGBA" else canvas.paste(sh, (x - pad + shadow_offset[0], y - pad + shadow_offset[1]), sh)
    if canvas.mode == "RGBA":
        canvas.alpha_composite(layer, (x, y))
    else:
        canvas.paste(layer, (x, y), layer)
    return (x, y, x + layer.width, y + layer.height)


# ── Phone mock-up ────────────────────────────────────────────────────────────
def phone(screen: Image.Image, width: int, frame=(28, 26, 34), edge=(120, 112, 132), tilt: float = 0.0) -> Image.Image:
    """A modern Android phone (thin bezel, punch-hole camera, side keys, glass glint) showing `screen` (any 9:20 image)."""
    sw = width
    bezel = max(4, round(sw * 0.026))
    screen_w = sw - bezel * 2
    screen_h = round(screen_w * screen.height / screen.width)
    sh = screen_h + bezel * 2
    R = round(sw * 0.125)
    pad = max(3, round(sw * 0.012))  # room for side keys
    body = Image.new("RGBA", (sw + pad * 2, sh), (0, 0, 0, 0))
    d = ImageDraw.Draw(body)
    # side keys (power + volume) on the right edge
    kx = sw + pad - 2
    for (y0, y1) in ((0.20, 0.27), (0.31, 0.42)):
        d.rounded_rectangle([kx - 2, int(sh * y0), kx + pad - 1, int(sh * y1)], radius=pad, fill=edge)
    # frame with metallic edge
    frame_img = Image.new("RGBA", (sw, sh), edge + (255,))
    inner = Image.new("RGBA", (sw - 4, sh - 4), frame + (255,))
    frame_img.paste(inner, (2, 2), rounded_mask(inner.size, R - 2))
    body.paste(frame_img, (pad, 0), rounded_mask((sw, sh), R))
    # screen
    scr = screen.resize((screen_w, screen_h), Image.LANCZOS).convert("RGBA")
    # glass glint: very soft diagonal light
    glint = Image.new("L", scr.size, 0)
    gd = ImageDraw.Draw(glint)
    gd.polygon([(0, 0), (int(screen_w * 0.55), 0), (0, int(screen_h * 0.42))], fill=22)
    glint = glint.filter(ImageFilter.GaussianBlur(screen_w * 0.06))
    scr = Image.alpha_composite(scr, Image.merge("RGBA", (Image.new("L", scr.size, 255),) * 3 + (glint,)))
    body.paste(scr, (pad + bezel, bezel), rounded_mask(scr.size, R - bezel))
    # punch-hole camera
    cr = max(3, round(screen_w * 0.017))
    cx, cy = pad + sw // 2, bezel + round(screen_h * 0.022)
    d.ellipse([cx - cr, cy - cr, cx + cr, cy + cr], fill=(8, 8, 12))
    d.ellipse([cx - cr // 2.4, cy - cr // 2.4, cx + cr // 3, cy + cr // 3], fill=(40, 44, 70))
    if tilt:
        body = body.rotate(tilt, resample=Image.BICUBIC, expand=True)
    return body


def phone_height(width: int, screen_ratio: float = 2400 / 1080) -> int:
    bezel = max(4, round(width * 0.026))
    return round((width - bezel * 2) * screen_ratio) + bezel * 2


def width_for_height(height: int, screen_ratio: float = 2400 / 1080) -> int:
    return int(height / (screen_ratio * 0.948 + 0.052 * 1))  # good approximation of phone_height inverse


# ── Lock screen composite (real app wallpaper + system-style clock) ──────────
def lockscreen(render: str, time_text="9:41", date_text="Wednesday, November 11", notification: tuple | None = None) -> Image.Image:
    """`render` is a 1080x2400 wallpaper drawn by the app's LockScreenVerse renderer. Adds the Android lock clock UI."""
    return lockscreen_from(src(render), time_text, date_text, notification)


def lockscreen_from(wallpaper: Image.Image, time_text="9:41", date_text="Wednesday, November 11", notification: tuple | None = None) -> Image.Image:
    im = wallpaper.copy().convert("RGBA").resize((1080, 2400), Image.LANCZOS)
    w, h = im.size
    d = ImageDraw.Draw(im)
    # status bar
    d.text((int(w * 0.07), int(h * 0.018)), time_text, font=font("sans", "demi", int(w * 0.036)), fill=(255, 255, 255, 230))
    _status_icons(d, w, h, (255, 255, 255, 230))
    # lock glyph
    lx, ly, ls = w // 2, int(h * 0.075), int(w * 0.022)
    d.rounded_rectangle([lx - ls, ly, lx + ls, ly + int(ls * 1.5)], radius=ls // 3, fill=(255, 255, 255, 220))
    d.arc([lx - ls * 0.7, ly - ls * 1.1, lx + ls * 0.7, ly + ls * 0.4], 180, 360, fill=(255, 255, 255, 220), width=max(2, ls // 4))
    # big clock + date
    f = font("sans", "light", int(w * 0.24))
    tw = d.textlength(time_text, font=f)
    for dx, dy, a in ((0, 4, 60),):
        d.text(((w - tw) / 2 + dx, h * 0.105 + dy), time_text, font=f, fill=(0, 0, 0, a))
    d.text(((w - tw) / 2, h * 0.105), time_text, font=f, fill=(255, 255, 255, 245))
    fd = font("sans", "medium", int(w * 0.04))
    dw = d.textlength(date_text, font=fd)
    d.text(((w - dw) / 2, h * 0.235), date_text, font=fd, fill=(255, 255, 255, 235))
    if notification:
        notif = notification_card(notification[0], notification[1], int(w * 0.88), dark=True)
        im.alpha_composite(notif, ((w - notif.width) // 2, int(h * 0.30)))
    # bottom shortcuts
    for x in (w * 0.15, w * 0.85):
        cx, cy, r = int(x), int(h * 0.95), int(w * 0.055)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=(0, 0, 0, 70))
        d.ellipse([cx - r // 3, cy - r // 3, cx + r // 3, cy + r // 3], outline=(255, 255, 255, 220), width=3)
    d.rounded_rectangle([w * 0.38, h * 0.985, w * 0.62, h * 0.985 + 8], radius=4, fill=(255, 255, 255, 200))
    return im.convert("RGB")


def _status_icons(d: ImageDraw.ImageDraw, w, h, fill):
    y = int(h * 0.024); s = int(w * 0.022)
    bx = int(w * 0.93)
    d.rounded_rectangle([bx - s * 1.9, y, bx, y + s * 0.95], radius=s // 4, fill=fill)  # battery
    wx = bx - int(s * 3.4)
    for i, r in enumerate((s * 1.1, s * 0.75, s * 0.4)):  # wifi arcs
        d.pieslice([wx - r, y + s * 1.05 - r, wx + r, y + s * 1.05 + r], 225, 315, fill=fill if i == 2 else None, outline=None if i == 2 else fill, width=max(2, s // 5))


def homescreen_live(render: str, time_text="9:41") -> Image.Image:
    """Live wallpaper frame (drawn by the app's LiveWallpaperRenderer) with a status bar and the WisdomOne dock icon."""
    im = src(render).copy().convert("RGBA")
    w, h = im.size
    d = ImageDraw.Draw(im)
    d.text((int(w * 0.07), int(h * 0.018)), time_text, font=font("sans", "demi", int(w * 0.036)), fill=(255, 255, 255, 230))
    _status_icons(d, w, h, (255, 255, 255, 230))
    # search pill + the WisdomOne icon only (we never invent other apps)
    d.rounded_rectangle([w * 0.06, h * 0.905, w * 0.94, h * 0.905 + w * 0.13], radius=int(w * 0.065), fill=(255, 255, 255, 70))
    ic = logo().resize((int(w * 0.16), int(w * 0.16)), Image.LANCZOS)
    m = Image.new("L", ic.size, 0); ImageDraw.Draw(m).ellipse([0, 0, ic.width - 1, ic.height - 1], fill=255)
    im.paste(ic, (int(w * 0.42), int(h * 0.81)), ImageChops.multiply(m, ic.split()[-1]))
    return im.convert("RGB")


def notification_card(title: str, body: str, width: int, dark=False) -> Image.Image:
    """Android-style notification (the app's real reminder text: "Today's Bible verse")."""
    pad = int(width * 0.05)
    ft = font("sans", "demi", int(width * 0.043)); fb = font("sans", "regular", int(width * 0.04)); fa = font("sans", "medium", int(width * 0.032))
    tmp = ImageDraw.Draw(Image.new("RGB", (10, 10)))
    lines = wrap(tmp, body, fb, width - pad * 3 - int(width * 0.12))[:2]
    h = int(pad * 2.2 + width * 0.05 + ft.size * 1.3 + len(lines) * fb.size * 1.32)
    card = Image.new("RGBA", (width, h), (0, 0, 0, 0))
    bg = (34, 28, 48, 236) if dark else (250, 247, 252, 246)
    fg = (255, 255, 255, 255) if dark else (28, 22, 40, 255)
    sub = (210, 204, 226, 255) if dark else (96, 88, 112, 255)
    ImageDraw.Draw(card).rounded_rectangle([0, 0, width - 1, h - 1], radius=int(width * 0.06), fill=bg)
    d = ImageDraw.Draw(card)
    isz = int(width * 0.1)
    ic = logo().resize((isz, isz), Image.LANCZOS)
    m = Image.new("L", ic.size, 0); ImageDraw.Draw(m).ellipse([0, 0, isz - 1, isz - 1], fill=255)
    card.paste(ic, (pad, pad), ImageChops.multiply(m, ic.split()[-1]))
    x = pad * 2 + isz
    d.text((x, pad - 2), "WisdomOne · now", font=fa, fill=sub)
    y = pad + int(fa.size * 1.35)
    d.text((x, y), title, font=ft, fill=fg); y += int(ft.size * 1.3)
    for ln in lines:
        d.text((x, y), ln, font=fb, fill=sub); y += int(fb.size * 1.32)
    return card


# ── Backgrounds ──────────────────────────────────────────────────────────────
def gradient(size, top, bottom, angle=90) -> Image.Image:
    w, h = size
    if angle == 90:
        t = np.linspace(0, 1, h)[:, None].repeat(w, 1)
    else:
        yy, xx = np.mgrid[0:h, 0:w]
        a = math.radians(angle)
        t = (xx * math.cos(a) + yy * math.sin(a)); t = (t - t.min()) / (t.max() - t.min())
    t = t * t * (3 - 2 * t)  # smoothstep
    arr = np.zeros((h, w, 3))
    for i in range(3):
        arr[..., i] = top[i] + (bottom[i] - top[i]) * t
    return Image.fromarray(arr.astype("uint8"), "RGB")


def glow(canvas: Image.Image, center, radius, color, strength=0.6):
    w, h = canvas.size
    yy, xx = np.mgrid[0:h, 0:w]
    dd = np.sqrt((xx - center[0]) ** 2 + (yy - center[1]) ** 2) / radius
    a = np.clip(1 - dd, 0, 1) ** 2 * strength
    base = np.asarray(canvas.convert("RGB")).astype(float)
    col = np.array(color, dtype=float)
    out = base * (1 - a[..., None]) + col * a[..., None]
    res = Image.fromarray(out.clip(0, 255).astype("uint8"), "RGB")
    return res if canvas.mode == "RGB" else res.convert(canvas.mode)


def bokeh(canvas: Image.Image, n=26, color=(255, 240, 210), seed=3, max_r=0.03, alpha=(18, 60)):
    w, h = canvas.size
    rnd = random.Random(seed)
    layer = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for _ in range(n):
        r = rnd.uniform(0.004, max_r) * w
        x, y = rnd.uniform(0, w), rnd.uniform(0, h)
        d.ellipse([x - r, y - r, x + r, y + r], fill=color + (rnd.randint(*alpha),))
    layer = layer.filter(ImageFilter.GaussianBlur(w * 0.004))
    base = canvas.convert("RGBA"); base.alpha_composite(layer)
    return base.convert(canvas.mode)


def stars(canvas: Image.Image, n=120, seed=5, top_frac=0.7):
    w, h = canvas.size
    rnd = random.Random(seed)
    d = ImageDraw.Draw(canvas)
    for _ in range(n):
        x, y = rnd.uniform(0, w), rnd.uniform(0, h * top_frac)
        r = rnd.choice((1, 1, 1, 2, 2, 3)) * w / 1080
        d.ellipse([x - r, y - r, x + r, y + r], fill=(255, 255, 255) if canvas.mode == "RGB" else (255, 255, 255, rnd.randint(90, 230)))
    return canvas


def grain(canvas: Image.Image, amount=5, seed=1) -> Image.Image:
    rng = np.random.default_rng(seed)
    arr = np.asarray(canvas.convert("RGB")).astype(np.int16)
    arr = arr + rng.integers(-amount, amount + 1, size=arr.shape[:2])[..., None]
    return Image.fromarray(arr.clip(0, 255).astype("uint8"), "RGB")


def art_background(size, n: int, blur=0, darken=0.0, align=(0.5, 0.5)) -> Image.Image:
    im = cover(art(n), size, align)
    if blur:
        im = im.filter(ImageFilter.GaussianBlur(blur))
    if darken:
        im = Image.blend(im, Image.new("RGB", size, (8, 4, 18)), darken)
    return im


def landscape_background(size, fade_to=None, align=(0.6, 1.0)) -> Image.Image:
    """The approved pastel sunrise landscape (design/settings_header_landscape.jpg), optionally fading to a colour at the top."""
    im = cover(design("settings_header_landscape.jpg"), size, align)
    if fade_to:
        w, h = size
        top = Image.new("RGB", size, fade_to)
        mask = Image.fromarray((np.clip(1 - np.linspace(0, 1, h) * 1.6, 0, 1)[:, None].repeat(w, 1) * 255).astype("uint8"), "L")
        im = Image.composite(top, im, mask)
    return im


# ── Typography ───────────────────────────────────────────────────────────────
def wrap(d: ImageDraw.ImageDraw, text: str, f, max_w: int) -> list[str]:
    out = []
    for para in text.split("\n"):
        words, cur = para.split(" "), ""
        for wd in words:
            t = (cur + " " + wd).strip()
            if d.textlength(t, font=f) <= max_w or not cur:
                cur = t
            else:
                out.append(cur); cur = wd
        out.append(cur)
    return out


def text_block(img: Image.Image, text: str, box, family="serif", style="bold", size=80, fill=(255, 255, 255), align="left",
               line=1.12, max_lines=4, min_size=20, shadow_px=0, valign="top", tracking=0) -> tuple[int, int]:
    """Draws wrapped text inside box=(x, y, w, h), shrinking the font until it fits. Returns (bottom_y, used_size)."""
    x, y, bw, bh = box
    d = ImageDraw.Draw(img)
    s = size
    while True:
        f = font(family, style, s)
        lines = wrap(d, text, f, bw)
        th = int(len(lines) * s * line)
        if (len(lines) <= max_lines and th <= bh) or s <= min_size:
            break
        s = int(s * 0.94)
    cy = y + (bh - th) // 2 if valign == "middle" else (y + bh - th if valign == "bottom" else y)
    for ln in lines:
        lw = d.textlength(ln, font=f)
        px = x if align == "left" else (x + (bw - lw) / 2 if align == "center" else x + bw - lw)
        if shadow_px:
            d.text((px, cy + shadow_px), ln, font=f, fill=(0, 0, 0, 90) if img.mode == "RGBA" else (20, 10, 30))
        d.text((px, cy), ln, font=f, fill=fill)
        cy += int(s * line)
    return cy, s


def pill(img: Image.Image, text: str, xy, size=34, bg=(255, 255, 255), fg=(30, 21, 48), family="sans", style="demi", pad=(0.9, 0.42),
         outline=None, anchor="lt", icon: str | None = None, alpha=255) -> tuple:
    d = ImageDraw.Draw(img)
    f = font(family, style, size)
    label = (icon + "  " if icon else "") + text
    tw = d.textlength(label, font=f)
    w = int(tw + size * pad[0] * 2); h = int(size * (1 + pad[1] * 2))
    x, y = xy
    if anchor == "mt": x -= w // 2
    elif anchor == "rt": x -= w
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(layer).rounded_rectangle([0, 0, w - 1, h - 1], radius=h // 2, fill=bg + (alpha,), outline=outline, width=2 if outline else 0)
    ImageDraw.Draw(layer).text((size * pad[0], h / 2), label, font=f, fill=fg, anchor="lm")
    paste(img, layer, (int(x), int(y)))
    return (x, y, x + w, y + h)


def play_cta(img: Image.Image, xy, height=86, dark=True, anchor="lt", text="Get it on Google Play") -> tuple:
    """Call-to-action pill with a simple play triangle. (Text stand-in — swap in Google's official badge before paid use.)"""
    d = ImageDraw.Draw(img)
    f = font("sans", "demi", int(height * 0.36))
    tw = d.textlength(text, font=f)
    w = int(tw + height * 1.45); h = height
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ld = ImageDraw.Draw(layer)
    ld.rounded_rectangle([0, 0, w - 1, h - 1], radius=h // 2, fill=(18, 12, 30, 255) if dark else (255, 255, 255, 255))
    tx = h * 0.42; ty = h * 0.30
    ld.polygon([(tx, ty), (tx, h - ty), (tx + h * 0.36, h / 2)], fill=C["gold_bright"])
    ld.text((h * 0.95, h / 2), text, font=f, fill=(255, 255, 255) if dark else C["ink"], anchor="lm")
    x, y = xy
    if anchor == "mt": x -= w // 2
    paste(img, layer, (int(x), int(y)), shadow_blur=int(h * 0.15), shadow_opacity=0.25, shadow_offset=(0, int(h * 0.08)))
    return (x, y, x + w, y + h)


def brand_lockup(img: Image.Image, xy, size=64, color=(30, 21, 48), anchor="lt", text="WisdomOne"):
    d = ImageDraw.Draw(img)
    f = font("sans", "bold", int(size * 0.62))
    tw = d.textlength(text, font=f)
    w = int(size + size * 0.25 + tw)
    x, y = xy
    if anchor == "mt": x -= w // 2
    elif anchor == "rt": x -= w
    ic = logo().resize((size, size), Image.LANCZOS)
    m = Image.new("L", ic.size, 0); ImageDraw.Draw(m).ellipse([0, 0, size - 1, size - 1], fill=255)
    img.paste(ic, (int(x), int(y)), ImageChops.multiply(m, ic.split()[-1]))
    d.text((x + size * 1.25, y + size / 2), text, font=f, fill=color, anchor="lm")
    return (x, y, x + w, y + size)


def save(img: Image.Image, path: str, quality=92):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im = img.convert("RGB")
    if path.lower().endswith(".png"):
        im.save(path, "PNG", optimize=True)
    else:
        im.save(path, "JPEG", quality=quality, subsampling=0, optimize=True, progressive=True)
    return path
