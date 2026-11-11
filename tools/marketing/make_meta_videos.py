#!/usr/bin/env python3
"""Task 5 — five 1080x1920 promotional videos (H.264 / AAC, 30 fps, 15–30 s) with original music.

    .venv/bin/python tools/marketing/make_meta_videos.py            # all five
    .venv/bin/python tools/marketing/make_meta_videos.py V02          # one

Frames are composed in Python from real app screens / app-rendered wallpapers (see wo_kit) and piped into ffmpeg. The music
is generated here (soft pad chords + bell arpeggio), so it is original and needs no licence. Also writes a storyboard
contact sheet next to each video.
"""
from __future__ import annotations

import math
import os
import subprocess
import sys
import wave
from functools import lru_cache

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wo_kit as K  # noqa: E402
import make_play_store as P  # noqa: E402
import campaign_spec as SPEC  # noqa: E402

W, H, FPS = 1080, 1920, 30
OUT = os.path.join(K.ROOT, "WisdomOne_Meta_Ads_Campaign")
NOTE = ("Today’s Bible verse", "They are new every morning: great is thy faithfulness.")
SAFE_TOP, SAFE_BOTTOM = int(H * 0.13), int(H * 0.72)   # captions stay inside (Reels / Stories UI)


# ── easing / helpers ─────────────────────────────────────────────────────────
def ease(x):  # easeInOutCubic
    x = max(0.0, min(1.0, x))
    return 4 * x ** 3 if x < 0.5 else 1 - (-2 * x + 2) ** 3 / 2


def ramp(t, a, b):
    return ease((t - a) / max(1e-6, b - a))


@lru_cache(maxsize=64)
def phone_img(key: str, width: int, tilt: float = 0.0):
    kind, name = key.split(":", 1)
    if kind == "lock":
        scr = K.lockscreen(name)
    elif kind == "locknote":
        scr = K.lockscreen(name, notification=NOTE)
    elif kind == "live":
        scr = K.homescreen_live(name)
    else:
        scr = K.src(name)
    return K.phone(scr, width, tilt=tilt)


@lru_cache(maxsize=8)
def anim_frames(theme: str):
    """Decodes play_store_creatives/01_feature_screenshots/render_live_<theme>_anim.mp4 (72 frames @ 12 fps, app renderer)."""
    path = os.path.join(K.SRC, f"render_live_{theme}_anim.mp4")
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", path, "-f", "rawvideo", "-pix_fmt", "rgb24", "-"], capture_output=True, check=True).stdout
    n = len(raw) // (720 * 1600 * 3)
    arr = np.frombuffer(raw, np.uint8).reshape(n, 1600, 720, 3)
    return [Image.fromarray(a).resize((1080, 2400), Image.LANCZOS) for a in arr]


@lru_cache(maxsize=512)
def live_phone(theme: str, idx: int, width: int, tilt: float = 0.0):
    fr = anim_frames(theme)[idx % 72]
    im = fr.convert("RGBA"); d = ImageDraw.Draw(im)
    d.text((76, 43), "9:41", font=K.font("sans", "demi", 39), fill=(255, 255, 255, 230))
    K._status_icons(d, 1080, 2400, (255, 255, 255, 230))
    return K.phone(im.convert("RGB"), width, tilt=tilt)


@lru_cache(maxsize=64)
def caption_img(text: str, size: int, color=(255, 255, 255), family=("serif", "bolditalic"), width=int(W * 0.84), align="center", glow=True):
    layer = Image.new("RGBA", (width, int(size * 4.6)), (0, 0, 0, 0))
    y, _ = K.text_block(layer, text, (0, 0, width, layer.height), family[0], family[1], size, fill=color + (255,), align=align, line=1.1, max_lines=4)
    layer = layer.crop((0, 0, width, max(10, y + int(size * 0.25))))
    if glow:
        sh = layer.split()[-1].filter(ImageFilter.GaussianBlur(size * 0.18)).point(lambda v: int(v * 0.55))
        shadow = Image.new("RGBA", layer.size, (10, 6, 20, 0)); shadow.putalpha(sh)
        out = Image.new("RGBA", layer.size, (0, 0, 0, 0)); out.alpha_composite(shadow); out.alpha_composite(layer)
        return out
    return layer


def put(canvas, layer, x, y, alpha=1.0):
    if alpha <= 0:
        return
    if alpha < 1:
        layer = layer.copy(); layer.putalpha(layer.split()[-1].point(lambda v: int(v * alpha)))
    canvas.alpha_composite(layer, (int(x), int(y)))


def caption(canvas, text, t, t_in, t_out=None, y=SAFE_TOP + 40, size=86, color=(255, 255, 255), family=("serif", "bolditalic")):
    a = ramp(t, t_in, t_in + 0.6) * (1 - (ramp(t, t_out - 0.5, t_out) if t_out else 0))
    lay = caption_img(text, size, color, family)
    put(canvas, lay, (W - lay.width) / 2, y + (1 - a) * 28, a)


def sub(canvas, text, t, t_in, t_out=None, y=SAFE_TOP + 260, color=(240, 232, 250)):
    caption(canvas, text, t, t_in, t_out, y=y, size=40, color=color, family=("sans", "medium"))


@lru_cache(maxsize=32)
def bg_cache(kind: str):
    if kind == "dawn": return P.bg_dawn(W, H).convert("RGBA")
    if kind == "night": return P.bg_night(W, H, 21).convert("RGBA")
    if kind == "plum": return P.bg_plum(W, H).convert("RGBA")
    if kind == "lavender": return P.bg_lavender(W, H).convert("RGBA")
    if kind == "sunrise": return K.landscape_background((W, H), align=(0.55, 0.9)).convert("RGBA")
    if kind.startswith("art"): return P.bg_art(W, H, int(kind[3:]), darken=0.3).convert("RGBA")
    raise KeyError(kind)


def kenburns(kind, t, dur, zoom=0.06):
    base = bg_cache(kind)
    z = 1 + zoom * min(1.0, t / max(dur, 0.1))
    if z <= 1.001:
        return base.copy()
    cw, ch = int(W / z), int(H / z)
    return base.crop(((W - cw) // 2, (H - ch) // 2, (W - cw) // 2 + cw, (H - ch) // 2 + ch)).resize((W, H), Image.BILINEAR)


def end_card(canvas, t, line="Discover WisdomOne", cta="Get it on Google Play"):
    a = ramp(t, 0, 0.7)
    ic = K.logo().resize((220, 220), Image.LANCZOS)
    m = Image.new("L", ic.size, 0); ImageDraw.Draw(m).ellipse([0, 0, 219, 219], fill=255)
    from PIL import ImageChops
    ic.putalpha(ImageChops.multiply(m, ic.split()[-1]))
    put(canvas, ic, (W - 220) / 2, SAFE_TOP + 140 - (1 - a) * 30, a)
    caption(canvas, "WisdomOne", t, 0.2, None, y=SAFE_TOP + 400, size=96, color=(255, 255, 255), family=("sans", "bold"))
    caption(canvas, line, t, 0.5, None, y=SAFE_TOP + 540, size=78, color=(238, 196, 120))
    if t > 0.9:
        lay = Image.new("RGBA", (W, 140), (0, 0, 0, 0))
        K.play_cta(lay, (W // 2, 10), height=104, dark=False, anchor="mt", text=cta)
        put(canvas, lay, 0, SAFE_TOP + 720, ramp(t, 0.9, 1.5))
    sub(canvas, "Free on Google Play", t, 1.2, None, y=SAFE_TOP + 880, color=(220, 210, 240))


# ── scene primitives ─────────────────────────────────────────────────────────
PW = 640  # phone width in videos


def phone_at(canvas, img, t, t_in, x=W / 2, y=760, from_dy=900, a_in=0.7):
    k = ramp(t, t_in, t_in + a_in)
    if k <= 0:
        return
    yy = y + (1 - k) * from_dy
    K.paste(canvas, img, (int(x - img.width / 2), int(yy)), shadow_blur=40, shadow_opacity=0.45 * k, shadow_offset=(0, 30))


# ── the five videos ──────────────────────────────────────────────────────────
def v01(t):  # Daily Wisdom, 18 s
    if t < 3:  # sunrise
        c = kenburns("sunrise", t, 3, 0.08)
        c = K.glow(c.convert("RGB"), (int(W * 0.62), int(H * (0.62 - 0.08 * ease(t / 3)))), W * 0.6, (255, 236, 190), 0.35 + 0.2 * ease(t / 3)).convert("RGBA")
        caption(c, "Start your day with wisdom", t, 0.3, None, color=(70, 30, 100))
        return c
    if t < 7:  # app opens
        lt = t - 3
        c = kenburns("dawn", lt, 4)
        caption(c, "Open WisdomOne", lt, 0.0, None, color=(55, 25, 85))
        phone_at(c, phone_img("app:app_home_no_tree_top", PW), lt, 0.2, y=640)
        return c
    if t < 12:  # verse
        lt = t - 7
        c = kenburns("plum", lt, 5)
        caption(c, "A new Bible verse, every day", lt, 0.0, None)
        img = phone_img("app:app_home_no_tree_verse", PW)
        k = ramp(lt, 0, 0.8)
        K.paste(c, img, (int(W / 2 - img.width / 2), int(560 + (1 - k) * 200)), shadow_blur=40, shadow_opacity=0.5)
        return c
    if t < 16:  # lock screen
        lt = t - 12
        c = kenburns("night", lt, 4)
        caption(c, "…and on your lock screen", lt, 0.0, None)
        phone_at(c, phone_img("lock:render_lock_img08", PW), lt, 0.0, y=600)
        return c
    c = bg_cache("plum").copy()
    end_card(c, t - 16)
    return c


@lru_cache(maxsize=1)
def plain_phone():
    """An ordinary lock screen without WisdomOne (plain gradient wallpaper) for the before/after moment."""
    return K.phone(K.lockscreen_from(K.gradient((1080, 2400), (74, 84, 110), (28, 32, 46))), PW)


def v02(t):  # Lock Screen Verse, 17 s
    if t < 3:
        c = kenburns("night", t, 3)
        caption(c, "Your lock screen can say more", t, 0.2, None)
        k = ramp(t, 0.0, 0.8)
        pl = plain_phone()
        K.paste(c, pl, (int(W / 2 - pl.width / 2), int(600 + (1 - k) * 700)), shadow_blur=40, shadow_opacity=0.45)
        return c
    if t < 8:
        lt = t - 3
        c = kenburns("night", lt + 3, 8)
        caption(c, "Today’s Bible verse — right on it", lt, 0.0, None)
        a = ramp(lt, 0, 0.9)
        pl = plain_phone()
        K.paste(c, pl, (int(W / 2 - pl.width / 2), 600), shadow_blur=40, shadow_opacity=0.45)
        img = phone_img("lock:render_lock_img03", PW)
        put(c, img, W / 2 - img.width / 2, 600, a)
        return c
    if t < 13:  # refresh demo: cycle verses
        lt = t - 8
        c = kenburns("plum", lt, 5)
        caption(c, "A new verse, automatically", lt, 0.0, None)
        sub(c, "Every 1, 6, 12 or 24 hours — you choose", lt, 0.4, None)
        seq = ["render_lock_img03", "render_lock_img14", "render_lock_img06", "render_lock_img18"]
        i = min(int(lt / 1.25), len(seq) - 1)
        f = (lt - i * 1.25) / 0.35
        cur = phone_img(f"lock:{seq[i]}", PW)
        K.paste(c, cur, (int(W / 2 - cur.width / 2), 700), shadow_blur=40, shadow_opacity=0.45)
        if i > 0 and f < 1:
            prev = phone_img(f"lock:{seq[i - 1]}", PW)
            put(c, prev, W / 2 - prev.width / 2, 700, 1 - ease(f))
        return c
    if t < 15:
        lt = t - 13
        c = bg_cache("lavender").copy()
        K.brand_lockup(c, (W // 2, int(SAFE_TOP + 300 - ramp(lt, 0, 0.6) * 20)), size=120, color=K.C["plum_deep"], anchor="mt")
        caption(c, "Wisdom on every unlock", lt, 0.3, None, y=SAFE_TOP + 500, color=(60, 30, 100))
        img = phone_img("lock:render_lock_img12", 520)
        phone_at(c, img, lt, 0.2, y=1000)
        return c
    c = bg_cache("night").copy()
    end_card(c, t - 15, "Get WisdomOne")
    return c


def v03(t):  # Wallpaper Experience, 23 s
    if t < 4:
        c = kenburns("art3", t, 4)
        caption(c, "Your home screen, reimagined", t, 0.2, None)
        img = live_phone("morning", int(t * 12), PW)
        phone_at(c, img, t, 0.0, y=620)
        return c
    if t < 10:  # wallpaper moods
        lt = t - 4
        themes = ["morning", "afternoon", "evening", "night"]
        i = min(int(lt / 1.5), 3)
        c = kenburns("plum" if i < 3 else "night", lt, 6)
        caption(c, "A calm live wallpaper that follows your day", lt, 0.0, None, size=72)
        label = ["Morning", "Afternoon", "Evening", "Night"][i]
        img = live_phone(themes[i], int(t * 12), PW)
        K.paste(c, img, (int(W / 2 - img.width / 2), 640), shadow_blur=40, shadow_opacity=0.45)
        f = (lt - i * 1.5) / 0.4
        if i > 0 and f < 1:
            prev = live_phone(themes[i - 1], int(t * 12), PW)
            put(c, prev, W / 2 - prev.width / 2, 640, 1 - ease(f))
        lay = Image.new("RGBA", (W, 100), (0, 0, 0, 0))
        K.pill(lay, label, (W // 2, 10), size=40, bg=(255, 255, 255), fg=K.C["plum"], anchor="mt", alpha=235)
        put(c, lay, 0, 520, 1.0)
        return c
    if t < 16:  # verses on beautiful backgrounds — carousel
        lt = t - 10
        c = kenburns("lavender", lt, 6)
        caption(c, "Verses on beautiful backgrounds", lt, 0.0, None, color=(60, 30, 100))
        seq = ["render_lock_img14", "render_lock_img06", "render_lock_color_golden", "render_lock_img02"]
        off = lt / 6 * (len(seq) - 1)
        for j, r in enumerate(seq):
            img = phone_img(f"lock:{r}", 520)
            x = W / 2 + (j - off) * 600
            if -600 < x < W + 600:
                K.paste(c, img, (int(x - img.width / 2), 720), shadow_blur=34, shadow_opacity=0.4)
        return c
    if t < 21:  # app demo
        lt = t - 16
        c = kenburns("dawn", lt, 5)
        caption(c, "Design it in seconds", lt, 0.0, None, color=(60, 30, 100))
        sub(c, "20 artworks · colour themes · live wallpaper", lt, 0.3, None, color=(92, 70, 110))
        a = phone_img("app:app_wallpaper_gallery", 560, -5)
        b = phone_img("app:app_ambient_wisdom_live_preview", 560, 5)
        phone_at(c, a, lt, 0.1, x=W * 0.30, y=820)
        phone_at(c, b, lt, 0.4, x=W * 0.70, y=760)
        return c
    c = bg_cache("plum").copy()
    end_card(c, t - 21, "Download WisdomOne")
    return c


def v04(t):  # Daily Spiritual Routine, 18 s
    if t < 4:
        c = kenburns("sunrise", t, 4)
        caption(c, "7:00 AM. A gentle reminder.", t, 0.2, None, color=(60, 30, 100))
        img = phone_img("lock:render_lock_img11", PW)
        K.paste(c, img, (int(W / 2 - img.width / 2), 620), shadow_blur=40, shadow_opacity=0.45)
        card = K.notification_card(*NOTE, int(PW * 0.92), dark=True)
        k = ramp(t, 1.0, 1.7)
        put(c, card, W / 2 - card.width / 2, 620 + K.phone_height(PW) * 0.30 - (1 - k) * 80, k)   # below the lock clock
        return c
    if t < 9:
        lt = t - 4
        c = kenburns("dawn", lt, 5)
        caption(c, "Open today’s verse", lt, 0.0, None, color=(60, 30, 100))
        phone_at(c, phone_img("app:app_reminder_notification", PW), lt, 0.0, y=600)
        if lt > 2.2:
            img = phone_img("app:app_home_with_tree_verse", PW)
            put(c, img, W / 2 - img.width / 2, 600, ramp(lt, 2.2, 2.9))
        return c
    if t < 15:
        lt = t - 9
        c = kenburns("night", lt, 6)
        caption(c, "Carry it with you all day", lt, 0.0, None)
        sub(c, "On your lock screen — and as a live wallpaper", lt, 0.4, None)
        a = phone_img("lock:render_lock_img10", 560, 5)
        b = live_phone("evening", int(t * 12), 560, -5)
        phone_at(c, a, lt, 0.2, x=W * 0.30, y=820)
        phone_at(c, b, lt, 0.6, x=W * 0.70, y=760)
        return c
    c = bg_cache("plum").copy()
    end_card(c, t - 15, "Make WisdomOne part of your day")
    return c


def v05(t):  # Complete experience, 28 s
    if t < 5:
        c = kenburns("dawn", t, 5)
        caption(c, "Meet WisdomOne", t, 0.2, None, color=(60, 30, 100), size=100)
        sub(c, "A daily Bible verse, beautifully presented", t, 0.6, None, color=(92, 70, 110))
        phone_at(c, phone_img("app:app_home_no_tree_verse", PW), t, 0.3, y=640)
        return c
    if t < 10:
        lt = t - 5
        c = kenburns("night", lt, 5)
        caption(c, "Your verse on the lock screen", lt, 0.0, None)
        phone_at(c, phone_img("lock:render_lock_img03", PW), lt, 0.0, y=600)
        return c
    if t < 15:
        lt = t - 10
        c = kenburns("art3", lt, 5)
        caption(c, "A live wallpaper that follows your day", lt, 0.0, None, size=74)
        phone_at(c, live_phone("night" if lt > 2.5 else "morning", int(t * 12), PW), lt, 0.0, y=620)
        return c
    if t < 19:
        lt = t - 15
        c = kenburns("art10", lt, 4)
        caption(c, "Gentle daily reminders", lt, 0.0, None)
        img = phone_img("lock:render_lock_img13", PW)
        K.paste(c, img, (int(W / 2 - img.width / 2), 640), shadow_blur=40, shadow_opacity=0.45)
        card = K.notification_card(*NOTE, int(PW * 0.92), dark=True)
        put(c, card, W / 2 - card.width / 2, 640 + K.phone_height(PW) * 0.30 - (1 - ramp(lt, 0.5, 1.1)) * 80, ramp(lt, 0.5, 1.1))
        return c
    if t < 24:
        lt = t - 19
        c = kenburns("lavender", lt, 5)
        caption(c, "A Wisdom Tree that grows with you", lt, 0.0, None, color=(60, 30, 100), size=76)
        sub(c, "Light & dark themes · made for calm", lt, 0.4, None, color=(92, 70, 110))
        phone_at(c, phone_img("app:app_home_with_tree_top", 560, 5), lt, 0.1, x=W * 0.30, y=820)
        phone_at(c, phone_img("app:app_settings_dark", 560, -5), lt, 0.5, x=W * 0.70, y=760)
        return c
    c = bg_cache("plum").copy()
    end_card(c, t - 24, "Install WisdomOne today", cta="Install free on Google Play")
    return c


VIDEOS = {"V01_daily_wisdom": v01, "V02_lockscreen_verse": v02, "V03_wallpaper_experience": v03, "V04_daily_routine": v04, "V05_complete_experience": v05}
CUTS = {"V01_daily_wisdom": [3, 7, 12, 16], "V02_lockscreen_verse": [3, 8, 13, 15], "V03_wallpaper_experience": [4, 10, 16, 21],
        "V04_daily_routine": [4, 9, 15], "V05_complete_experience": [5, 10, 15, 19, 24]}


def frame_at(fn, cuts, t):
    """Scene at time t, cross-faded over 0.45 s into each cut."""
    xf = 0.45
    for cut in cuts:
        if cut - xf <= t < cut:
            a = fn(t).convert("RGB"); b = fn(cut + (t - (cut - xf)) * 0.0 + 0.0001).convert("RGB")
            return Image.blend(a, b, ease((t - (cut - xf)) / xf))
    return fn(t).convert("RGB")


# ── original music ───────────────────────────────────────────────────────────
def music(seconds: float, seed: int, path: str, sr=44100):
    rng = np.random.default_rng(seed)
    n = int(seconds * sr)
    t = np.arange(n) / sr
    # I – vi – IV – V in D major, voiced warm & low; one chord every 4 s
    roots = [[146.83, 220.0, 293.66, 369.99], [123.47, 185.0, 246.94, 293.66], [98.0, 196.0, 246.94, 293.66], [110.0, 220.0, 277.18, 329.63]]
    pad = np.zeros(n)
    seg = 4.0
    for k in range(int(math.ceil(seconds / seg)) + 1):
        start = int(k * seg * sr); end = min(n, int((k + 1) * seg * sr + sr * 1.5))
        if start >= n:
            break
        tt = np.arange(end - start) / sr
        env = np.minimum(1, tt / 1.2) * np.exp(-np.maximum(0, tt - seg) * 1.6)
        chord = roots[k % 4]
        s = sum(np.sin(2 * np.pi * f * tt + rng.uniform(0, 6)) * (0.9 if i == 0 else 0.55) + 0.25 * np.sin(2 * np.pi * f * 2.003 * tt)
                for i, f in enumerate(chord))
        pad[start:end] += s * env * 0.11
    # soft bell arpeggio (eighth notes at 72 bpm), pentatonic
    bells = np.zeros(n)
    notes = [587.33, 659.25, 739.99, 880.0, 987.77, 880.0, 739.99, 659.25]
    step = 60 / 72 / 2
    for i in range(int(seconds / step)):
        if rng.random() < 0.35:
            continue
        st = int(i * step * sr); ln = min(n - st, int(sr * 1.8))
        if ln <= 0:
            break
        tt = np.arange(ln) / sr
        f = notes[(i + int(rng.integers(0, 3))) % len(notes)]
        bells[st:st + ln] += (np.sin(2 * np.pi * f * tt) + 0.3 * np.sin(2 * np.pi * f * 3.01 * tt)) * np.exp(-tt * 2.6) * 0.07
    mix = pad + bells
    # simple stereo reverb (two feedback delays)
    left, right = mix.copy(), mix.copy()
    for d_ms, g in ((97, 0.32), (151, 0.27), (233, 0.22)):
        d = int(sr * d_ms / 1000)
        left[d:] += left[:-d] * g; right[d + 13:] += right[:-(d + 13)] * g
    st = np.stack([left, right], 1)
    fade = np.ones(n); fi = int(sr * 1.2); fo = int(sr * 2.0)
    fade[:fi] = np.linspace(0, 1, fi); fade[-fo:] = np.linspace(1, 0, fo)
    st *= fade[:, None]
    st = st / np.max(np.abs(st)) * 0.6
    with wave.open(path, "wb") as w:
        w.setnchannels(2); w.setsampwidth(2); w.setframerate(sr)
        w.writeframes((st * 32767).astype("<i2").tobytes())


def render(vid: str):
    meta = SPEC.VIDEOS[vid]
    ad = next(a for a in SPEC.ADS if a.get("video") == vid)
    folder = os.path.join(OUT, SPEC.ad_set(meta["set"])["folder"], "videos")
    os.makedirs(folder, exist_ok=True)
    out = os.path.join(folder, SPEC.video_file(ad))
    wav = out.replace(".mp4", ".wav")
    secs = meta["seconds"]
    music(secs, sum(map(ord, vid)), wav)
    fn, cuts = VIDEOS[vid], CUTS[vid]
    cmd = ["ffmpeg", "-y", "-v", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{W}x{H}", "-r", str(FPS), "-i", "-", "-i", wav,
           "-c:v", "libx264", "-preset", "medium", "-crf", "19", "-profile:v", "high", "-pix_fmt", "yuv420p", "-c:a", "aac", "-b:a", "160k",
           "-shortest", "-movflags", "+faststart", out]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE)
    keys = []
    total = int(secs * FPS)
    for i in range(total):
        t = i / FPS
        im = frame_at(fn, cuts, t)
        proc.stdin.write(im.tobytes())
        if i % (FPS * 2) == int(FPS * 1.5):  # storyboard frames (mid-scene)
            keys.append((t, im.resize((270, 480), Image.LANCZOS)))
    proc.stdin.close(); proc.wait()
    os.remove(wav)
    # storyboard sheet
    cols = 6
    rows = math.ceil(len(keys) / cols)
    sheet = Image.new("RGB", (cols * 290 + 20, rows * 520 + 20), (250, 247, 252))
    d = ImageDraw.Draw(sheet)
    for j, (t, im) in enumerate(keys):
        x, y = 20 + (j % cols) * 290, 20 + (j // cols) * 520
        sheet.paste(im, (x, y)); d.text((x, y + 486), f"{t:04.1f} s", font=K.font("sans", "demi", 22), fill=(40, 30, 60))
    sheet.save(out.replace(".mp4", "_storyboard.jpg"), quality=86)
    return out


if __name__ == "__main__":
    want = sys.argv[1:]
    for vid in SPEC.VIDEOS:
        if want and not any(vid.startswith(w) for w in want):
            continue
        print(render(vid).replace(K.ROOT + "/", ""))
