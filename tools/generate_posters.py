#!/usr/bin/env python3
"""WisdomOne marketing posters (design/posters/<NN>-<slug>/<size>.png): 10 concepts x 4 export sizes.

    pip install pillow numpy
    python tools/generate_posters.py --out design/posters

Backgrounds are the project's own artwork (android/app/src/main/assets/wallpapers + generate_wallpapers.py scenes).
Verses are short, widely-cited passages with their standard references - a content owner must still verify wording
against the licensed edition before publishing (see design/posters/README.md). The Google Play call-to-action is a
text pill: swap in Google's official badge artwork from the Play brand guidelines before running paid campaigns.
"""
import argparse, os, sys
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_wallpapers as gw  # noqa: E402

ASSETS = os.path.join(ROOT, "android/app/src/main/assets/wallpapers")
LOGO = os.path.join(ROOT, "android/app/src/main/res/drawable/ic_wisdomone_ui_logo.png")
SCREEN = os.path.join(ROOT, "design/posters/source/screen_home.png")
SIZES = {"4x5_1080x1350": (1080, 1350), "square_1080x1080": (1080, 1080), "story_1080x1920": (1080, 1920), "ads_1200x628": (1200, 628)}

F = {
    "latin": ("/System/Library/Fonts/Avenir Next.ttc", {"regular": 7, "bold": 0, "med": 5}),
    "serif": ("/System/Library/Fonts/Palatino.ttc", {"regular": 0, "italic": 2, "bold": 1}),
    "deva": ("/System/Library/Fonts/Kohinoor.ttc", {"regular": 0, "bold": 2}),
    "arab": ("/System/Library/Fonts/GeezaPro.ttc", {"regular": 0, "bold": 1}),
}


def font(kind, style, size):
    path, idx = F[kind]
    try:
        return ImageFont.truetype(path, size, index=idx[style])
    except Exception:
        return ImageFont.truetype(path, size)


def wrap(d, text, fnt, max_w, direction=None):
    words, lines, cur = text.split(" "), [], ""
    for w in words:
        t = (cur + " " + w).strip()
        if d.textlength(t, font=fnt) <= max_w or not cur:
            cur = t
        else:
            lines.append(cur); cur = w
    if cur:
        lines.append(cur)
    return lines


def draw_block(d, lines, fnt, x, y, fill, align="left", max_w=0, lh=1.25, shadow=True, direction=None):
    for ln in lines:
        w = d.textlength(ln, font=fnt)
        px = x if align == "left" else (x + (max_w - w) / 2 if align == "center" else x + max_w - w)
        if shadow:
            d.text((px + 2, y + 2), ln, font=fnt, fill=(0, 0, 0, 150), direction=direction)
        d.text((px, y), ln, font=fnt, fill=fill, direction=direction)
        y += int(fnt.size * lh)
    return y


def cover(img, size):
    w, h = size
    s = max(w / img.width, h / img.height)
    img = img.resize((int(img.width * s) + 1, int(img.height * s) + 1), Image.LANCZOS)
    left, top = (img.width - w) // 2, (img.height - h) // 2
    return img.crop((left, top, left + w, top + h))


def phone(screen, height):
    """Phone mockup around a screenshot."""
    sw = int(height * 0.47)
    scr = screen.resize((sw, int(sw * screen.height / screen.width))).crop((0, 0, sw, height - 36))
    out = Image.new("RGBA", (sw + 36, height), (0, 0, 0, 0))
    d = ImageDraw.Draw(out)
    d.rounded_rectangle([0, 0, sw + 35, height - 1], radius=54, fill=(24, 20, 34, 255), outline=(90, 80, 110, 255), width=3)
    mask = Image.new("L", scr.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, scr.width - 1, scr.height - 1], radius=40, fill=255)
    out.paste(scr, (18, 18), mask)
    shadow = Image.new("RGBA", (out.width + 60, out.height + 60), (0, 0, 0, 0))
    shadow.paste((0, 0, 0, 140), (30, 40, 30 + out.width, 40 + out.height), out.getchannel("A"))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    shadow.alpha_composite(out, (30, 30))
    return shadow


def pill(d, xy, text, fnt, fill, fg):
    x, y = xy
    w = d.textlength(text, font=fnt) + fnt.size * 1.8
    h = fnt.size * 2.1
    d.rounded_rectangle([x, y, x + w, y + h], radius=h / 2, fill=fill)
    d.text((x + fnt.size * 0.9, y + h / 2), text, font=fnt, fill=fg, anchor="lm")
    return w, h


def play_cta(d, x, y, scale):
    """Text-only 'Get it on Google Play' badge stand-in."""
    h = int(64 * scale); w = int(300 * scale)
    d.rounded_rectangle([x, y, x + w, y + h], radius=int(14 * scale), fill=(0, 0, 0, 235), outline=(190, 190, 200, 255), width=2)
    # play triangle
    tx, ty = x + int(22 * scale), y + int(16 * scale)
    d.polygon([(tx, ty), (tx, ty + int(32 * scale)), (tx + int(28 * scale), ty + int(16 * scale))], fill=(52, 199, 123, 255))
    d.text((x + int(66 * scale), y + int(21 * scale)), "GET IT ON", font=font("latin", "regular", int(13 * scale)), fill=(255, 255, 255, 255), anchor="lm")
    d.text((x + int(66 * scale), y + int(43 * scale)), "Google Play", font=font("latin", "bold", int(24 * scale)), fill=(255, 255, 255, 255), anchor="lm")
    return w, h


def load_bg(spec):
    kind, ref = spec["bg"]
    if kind == "asset":
        return Image.open(os.path.join(ASSETS, ref)).convert("RGB")
    return gw.PACKS[ref[0]](ref[1]).img


POSTERS = [
    dict(slug="01-bhagavad-gita-daily-wisdom", tag="BHAGAVAD GITA · DAILY WISDOM", bg=("asset", "hinduism/02.webp"), script="deva",
         verse="कर्मण्येवाधिकारस्ते मा फलेषु कदाचन।", text="You have a right to your actions, but never to their fruits.", ref="Bhagavad Gita 2.47", head="Do your duty with a steady mind."),
    dict(slug="02-bhagavad-gita-life-lessons", tag="BHAGAVAD GITA · LIFE LESSONS", bg=("asset", "hinduism/08.webp"), script="deva", phone=True,
         verse="उद्धरेदात्मनात्मानं नात्मानमवसादयेत्।", text="Lift yourself by your own effort; never let yourself down.", text2="अपना उद्धार स्वयं करो, स्वयं को गिरने मत दो।", ref="Bhagavad Gita 6.5", head="Life lessons, one verse at a time."),
    dict(slug="03-ramayana-values-for-life", tag="RAMAYANA · VALUES FOR LIFE", bg=("asset", "hinduism/10.webp"), script="deva",
         verse="सत्यमेवेश्वरो लोके सत्ये धर्मः सदाश्रितः।", text="Truth alone is supreme in the world; dharma always rests on truth.", ref="Valmiki Ramayana, Ayodhya Kanda 109.13", head="Values that light a lifetime."),
    dict(slug="04-mahabharata-timeless-lessons", tag="MAHABHARATA · TIMELESS LESSONS", bg=("asset", "hinduism/09.webp"), script="deva",
         verse="अहिंसा परमो धर्मः।", text="Non-violence is the highest dharma.", ref="Mahabharata, Anushasana Parva", head="Wisdom for every battle within."),
    dict(slug="05-bible-daily-inspiration", tag="BIBLE · DAILY INSPIRATION", bg=("gen", ("christianity", 5)), script=None,
         verse="", text="The LORD is my shepherd; I shall not want.", ref="Psalm 23:1 · King James Version", head="A verse to begin your day."),
    dict(slug="06-bible-hope-and-strength", tag="BIBLE · HOPE & STRENGTH", bg=("gen", ("christianity", 0)), script=None, phone=True,
         verse="", text="Fear thou not; for I am with thee: be not dismayed; for I am thy God: I will strengthen thee.", ref="Isaiah 41:10 · King James Version", head="Hope and strength, daily."),
    dict(slug="07-quran-daily-guidance", tag="QURAN · DAILY GUIDANCE", bg=("gen", ("islam", 3)), script="arab",
         verse="فَإِنَّ مَعَ الْعُسْرِ يُسْرًا", text="For indeed, with hardship comes ease.", ref="Quran 94:5 (Ash-Sharh) · Sahih International", head="Guidance for today."),
    dict(slug="08-quran-daily-reflection", tag="QURAN · DAILY REFLECTION", bg=("gen", ("islam", 9)), script="arab",
         verse="إِنَّ اللَّهَ مَعَ الصَّابِرِينَ", text="Indeed, Allah is with the patient.", ref="Quran 2:153 (Al-Baqarah) · Sahih International", head="Patience and gratitude, every day."),
    dict(slug="09-dhammapada-inner-peace", tag="DHAMMAPADA · INNER PEACE", bg=("gen", ("buddhism", 0)), script=None,
         verse="", text="Better than a thousand hollow words is one word that brings peace.", ref="Dhammapada 100", head="Find stillness within."),
    dict(slug="10-one-app-multiple-traditions", tag="WISDOMONE", bg=("gen", ("general", 9)), script=None, phone=True, traditions=True,
         verse="", text="Hinduism · Christianity · Islam · Buddhism · General Wisdom", ref="Verses in your language, tradition-aware wallpapers, journal reflections.", head="One App. Multiple Traditions."),
]


def render(spec, size, screen, logo):
    W, H = size
    land = W > H * 1.3
    bg = cover(load_bg(spec), size).convert("RGBA")
    ov = Image.new("RGBA", size, (0, 0, 0, 0))
    od = ImageDraw.Draw(ov)
    for y in range(H):  # legibility gradient
        a = int(40 + 175 * (y / H) ** 1.3) if not land else 130
        od.line([(0, y), (W, y)], fill=(10, 6, 24, a))
    bg.alpha_composite(ov)
    d = ImageDraw.Draw(bg)
    m = int(W * 0.07) if not land else 56
    ls = 1.0 if not land else 0.74
    # brand
    lg = logo.resize((int(86 * ls * 1.2), int(86 * ls * 1.2)))
    bg.alpha_composite(lg, (m, m))
    d.text((m + lg.width + 16, m + lg.height / 2), "WisdomOne", font=font("latin", "bold", int(44 * ls)), fill=(255, 255, 255, 255), anchor="lm")
    area_w = W - 2 * m if not (land and spec.get("phone")) else int(W * 0.55)
    if spec.get("phone") and not land:
        ph = phone(screen, int(H * (0.42 if H > 1500 else 0.36)))
        bg.alpha_composite(ph, (W - ph.width - m // 2, int(H * 0.5) if H > 1500 else int(H * 0.46)))
        area_w = int(W * 0.5) if H <= 1500 else W - 2 * m
    if spec.get("phone") and land:
        ph = phone(screen, H - 70)
        bg.alpha_composite(ph, (W - ph.width - 30, 25))
    # content block anchored low (portrait) or middle (landscape)
    tag_f = font("latin", "bold", int(26 * ls * 1.15))
    head_f = font("serif", "bold", int(64 * ls * (1.0 if not land else 0.85)))
    verse_f = font(spec["script"] or "serif", "regular", int(68 * ls)) if spec["script"] else None
    text_f = font("serif", "italic", int(44 * ls * (1.0 if not land else 0.82)))
    ref_f = font("latin", "med", int(27 * ls * 1.1))
    lines_head = wrap(d, spec["head"], head_f, area_w)
    lines_text = wrap(d, "“" + spec["text"] + "”", text_f, area_w)
    text2_f = font("deva", "regular", int(36 * ls))
    lines_text2 = wrap(d, spec["text2"], text2_f, area_w) if spec.get("text2") else []
    verse_lines = wrap(d, spec["verse"], verse_f, area_w) if spec["verse"] else []
    total = (len(lines_head) * head_f.size * 1.2 + 24 + (len(verse_lines) * verse_f.size * 1.4 + 18 if verse_lines else 0)
             + len(lines_text) * text_f.size * 1.3 + len(lines_text2) * text2_f.size * 1.4 + 30 + ref_f.size * 2 + 150)
    y = (H - total - m) if not land else max(m + 110, (H - total) / 2 + 40)
    if spec.get("phone") and not land and H <= 1500:
        y = H * 0.12 + 120
    if spec.get("phone") and not land and H > 1500:
        y = H * 0.17
    pill(d, (m, int(y)), spec["tag"], tag_f, (20, 10, 40, 185), (255, 226, 160, 255)); y += tag_f.size * 2.1 + 22
    y = draw_block(d, lines_head, head_f, m, y, (255, 255, 255, 255), lh=1.15); y += 14
    if verse_lines:
        rtl = "rtl" if spec["script"] == "arab" else None
        y = draw_block(d, verse_lines, verse_f, m, y, (255, 226, 160, 255), align="right" if rtl else "left", max_w=area_w, lh=1.45, direction=rtl); y += 14
    y = draw_block(d, lines_text, text_f, m, y, (246, 240, 255, 255), lh=1.3); y += 8
    if lines_text2: y = draw_block(d, lines_text2, text2_f, m, y, (255, 226, 160, 255), lh=1.4)
    y += 10
    y = draw_block(d, wrap(d, spec["ref"], ref_f, area_w), ref_f, m, y, (255, 214, 140, 255), lh=1.3) + 22
    if spec.get("traditions"):
        names = [("hinduism", "Hinduism"), ("christianity", "Christianity"), ("islam", "Islam"), ("buddhism", "Buddhism"), ("general", "General Wisdom")]
        x = m
        tk = 1.3 * (0.62 if (H <= 1100 or land) else 1.0)
        for slug, label in names:
            th = Image.open(os.path.join(ASSETS, slug, "01.webp")).convert("RGB").resize((int(72 * ls * tk), int(128 * ls * tk)))
            mask = Image.new("L", th.size, 0); ImageDraw.Draw(mask).rounded_rectangle([0, 0, th.width - 1, th.height - 1], radius=14, fill=255)
            if x + th.width < m + area_w:
                bg.paste(th, (int(x), int(y)), mask)
                d.text((x + th.width / 2, y + th.height + 14), label.split()[0], font=font("latin", "med", int(17 * ls * 1.2)), fill=(255, 255, 255, 255), anchor="mm")
                x += th.width + 14
        y += int(128 * ls * tk) + 40
    play_cta(d, m, int(min(y + 6, H - 64 * 1.2 * ls - m * 0.6)) if not land else int(H - m - 64 * ls * 1.1), 1.2 * ls if not land else 0.9)
    return bg.convert("RGB")


def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--out", required=True)
    a = ap.parse_args()
    logo = Image.open(LOGO).convert("RGBA")
    mask = Image.new("L", logo.size, 0); ImageDraw.Draw(mask).ellipse([0, 0, logo.width - 1, logo.height - 1], fill=255)
    logo.putalpha(mask)
    screen = Image.open(SCREEN).convert("RGB")
    for spec in POSTERS:
        for name, size in SIZES.items():
            path = os.path.join(a.out, spec["slug"], f"{name}.jpg")
            os.makedirs(os.path.dirname(path), exist_ok=True)
            render(spec, size, screen, logo).save(path, quality=92, subsampling=0, optimize=True)
        print("poster", spec["slug"])


if __name__ == "__main__":
    main()
