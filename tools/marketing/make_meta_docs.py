#!/usr/bin/env python3
"""Task 5 — per-ad copy files, ad-set READMEs, master CSV, brand logo exports, 08_FINAL_EXPORTS and the validation report.

Run after make_meta_ads.py and make_meta_videos.py:
    .venv/bin/python tools/marketing/make_meta_docs.py
Every row of the master CSV is validated against the real file (exists, pixel size, video duration / codecs). A row is only
marked "Validated" when the file passes; otherwise it says what failed.
"""
from __future__ import annotations

import csv
import json
import os
import shutil
import subprocess
import sys

from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import wo_kit as K  # noqa: E402
import campaign_spec as SPEC  # noqa: E402

ROOT = os.path.join(K.ROOT, "WisdomOne_Meta_Ads_Campaign")
RATIO = {"4x5": "4:5", "1x1": "1:1", "9x16": "9:16"}
PLACEMENT = {"4x5": "Facebook & Instagram Feed", "1x1": "Facebook & Instagram Feed, Marketplace, right column", "9x16": "Stories & Reels"}


def rel(p):
    return os.path.relpath(p, ROOT)


def img_path(ad, fmt):
    return os.path.join(ROOT, SPEC.ad_set(ad["set"])["folder"], "images", SPEC.image_files(ad)[fmt])


def vid_path(ad):
    return os.path.join(ROOT, SPEC.ad_set(ad["set"])["folder"], "videos", SPEC.video_file(ad)) if ad.get("video") else ""


def check_image(path, size):
    if not os.path.exists(path):
        return "Missing"
    with Image.open(path) as im:
        if im.size != size:
            return f"Wrong size {im.size[0]}x{im.size[1]}"
    return "Validated"


def probe(path):
    out = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration:stream=codec_type,codec_name,width,height,r_frame_rate,pix_fmt",
                          "-of", "json", path], capture_output=True, text=True, check=True).stdout
    return json.loads(out)


def check_video(path, expected):
    if not path or not os.path.exists(path):
        return "Missing", {}
    info = probe(path)
    v = next(s for s in info["streams"] if s["codec_type"] == "video")
    a = next((s for s in info["streams"] if s["codec_type"] == "audio"), None)
    dur = float(info["format"]["duration"])
    problems = []
    if v["codec_name"] != "h264": problems.append("not H.264")
    if (v["width"], v["height"]) != (1080, 1920): problems.append("not 1080x1920")
    if v.get("pix_fmt") != "yuv420p": problems.append("not yuv420p")
    if not 15 <= dur <= 30: problems.append(f"duration {dur:.1f}s outside 15-30 s")
    if abs(dur - expected) > 0.5: problems.append(f"duration {dur:.1f}s != planned {expected}s")
    if not a or a["codec_name"] != "aac": problems.append("no AAC audio")
    facts = {"duration": round(dur, 2), "video": f"{v['codec_name']} {v['width']}x{v['height']} {v['r_frame_rate']} {v.get('pix_fmt')}",
             "audio": f"{a['codec_name']}" if a else "none", "size_mb": round(os.path.getsize(path) / 1e6, 1)}
    return ("Validated" if not problems else "; ".join(problems)), facts


def ad_copy_md(ad):
    s = SPEC.ad_set(ad["set"])
    files = SPEC.image_files(ad)
    vf = SPEC.video_file(ad)
    lines = [
        f"# {SPEC.ad_name(ad)} — {ad['title']}", "",
        f"**Campaign:** {SPEC.CAMPAIGN}  ", f"**Ad set:** {s['name']} — {s['angle']}  ", f"**Ad name (Ads Manager):** `{SPEC.ad_name(ad)}`", "",
        "| Field | Copy | Characters |", "|---|---|---|",
        f"| Primary text | {ad['primary']} | {len(ad['primary'])} |",
        f"| Headline | {ad['headline']} | {len(ad['headline'])} |",
        f"| Description | {ad['description']} | {len(ad['description'])} |",
        f"| Call to action | {ad['cta']} | — |",
        f"| Destination | {SPEC.PLAY_URL} | — |", "",
        "## Creative", "",
        f"- **Marketing angle / hook:** {ad['hook']} — {s['angle'].lower()}",
        f"- **Feature highlighted:** {ad['feature']}",
        f"- **Emotional tone:** {ad['emotion']}",
        f"- **Visual composition:** `{ad['style']}` · palette `{ad['pal']}` (see `tools/marketing/make_meta_ads.py`)", "",
        "| Placement | File | Size |", "|---|---|---|",
    ]
    for fmt, (w, h) in SPEC.FORMATS.items():
        lines.append(f"| {PLACEMENT[fmt]} ({RATIO[fmt]}) | `../images/{files[fmt]}` | {w} × {h} |")
    if vf:
        lines.append(f"| Reels / Stories video ({SPEC.VIDEOS[ad['video']]['seconds']} s) | `../videos/{vf}` | 1080 × 1920 |")
    lines += ["", "## Notes", "",
              "- Copy describes the app, never the reader (no assumptions about faith or other personal attributes — Meta policy).",
              "- Every app screen in the creative is a real WisdomOne screen; verses are quoted from the app catalogue (KJV).", ""]
    return "\n".join(lines)


def set_readme(s):
    ads = [a for a in SPEC.ADS if a["set"] == s["id"]]
    lines = [f"# {s['name']}", "", f"**Marketing angle:** {s['angle']}", "",
             "| Ad | Title | Headline | Composition | Images | Video |", "|---|---|---|---|---|---|"]
    for a in ads:
        files = SPEC.image_files(a)
        lines.append(f"| `{SPEC.ad_name(a)}` | {a['title']} | {a['headline']} | `{a['style']}` | " +
                     " · ".join(f"`images/{files[f]}`" for f in SPEC.FORMATS) + f" | {('`videos/' + SPEC.video_file(a) + '`') if a.get('video') else '—'} |")
    lines += ["", "Per-ad copy (primary text, headline, description, CTA, filenames, angle) is in `ad_copy/adNN_copy.md`.",
              "Audience hypothesis and testing plan: `../00_CAMPAIGN_STRATEGY/audience_strategy.md`.", ""]
    return "\n".join(lines)


def contact_sheet(fmt, out):
    w, h = SPEC.FORMATS[fmt]
    tw = 300; th = int(tw * h / w)
    pad, lab = 18, 54
    sheet = Image.new("RGB", (5 * (tw + pad) + pad, 5 * (th + lab + pad) + pad + 70), (247, 243, 250))
    d = ImageDraw.Draw(sheet)
    d.text((pad, 20), f"{SPEC.CAMPAIGN} — 25 ads · {RATIO[fmt]}", font=K.font("sans", "bold", 30), fill=(45, 27, 78))
    for i, ad in enumerate(SPEC.ADS):
        x = pad + (i % 5) * (tw + pad); y = 70 + pad + (i // 5) * (th + lab + pad)
        with Image.open(img_path(ad, fmt)) as im:
            sheet.paste(im.resize((tw, th), Image.LANCZOS), (x, y))
        d.text((x, y + th + 6), SPEC.ad_name(ad)[:32], font=K.font("sans", "demi", 16), fill=(45, 27, 78))
        d.text((x, y + th + 28), ad["headline"][:36], font=K.font("sans", "regular", 15), fill=(92, 82, 112))
    sheet.save(out, quality=88)


def main():
    rows, report = [], []
    # per-ad copy + set READMEs
    for s in SPEC.AD_SETS:
        base = os.path.join(ROOT, s["folder"])
        os.makedirs(os.path.join(base, "ad_copy"), exist_ok=True)
        with open(os.path.join(base, "README.md"), "w") as f:
            f.write(set_readme(s))
        for a in [a for a in SPEC.ADS if a["set"] == s["id"]]:
            with open(os.path.join(base, "ad_copy", f"ad{a['no']:02d}_copy.md"), "w") as f:
                f.write(ad_copy_md(a))
    # master CSV with validation
    video_facts = {}
    for a in SPEC.ADS:
        vstatus, facts = ("", {})
        if a.get("video"):
            vstatus, facts = check_video(vid_path(a), SPEC.VIDEOS[a["video"]]["seconds"])
            video_facts[SPEC.video_file(a)] = (vstatus, facts)
        for fmt, size in SPEC.FORMATS.items():
            st = check_image(img_path(a, fmt), size)
            vf = SPEC.video_file(a) if (fmt == "9x16" and a.get("video")) else ""
            status = st if not vf else (st if st != "Validated" else ("Validated" if vstatus == "Validated" else f"Image validated; video: {vstatus}"))
            rows.append({
                "Campaign Name": SPEC.CAMPAIGN, "Ad Set Name": SPEC.ad_set(a["set"])["name"], "Ad Name": SPEC.ad_name(a),
                "Creative Type": "Static image + video" if vf else "Static image", "Creative Angle": f"{a['hook']} — {a['feature']}",
                "Primary Text": a["primary"], "Headline": a["headline"], "Description": a["description"], "CTA": a["cta"],
                "Image Filename": rel(img_path(a, fmt)), "Video Filename": rel(vid_path(a)) if vf else "", "Aspect Ratio": RATIO[fmt], "Export Status": status,
            })
    csv_path = os.path.join(ROOT, "01_CAMPAIGN_COPY", "ad_copy_master_sheet.csv")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        wr = csv.DictWriter(f, fieldnames=list(rows[0].keys())); wr.writeheader(); wr.writerows(rows)
    # brand logos (master: the 512 px Play Store icon)
    logo_dir = os.path.join(ROOT, "07_BRAND_ASSETS", "logo"); os.makedirs(logo_dir, exist_ok=True)
    master = Image.open(os.path.join(K.ROOT, "android/app/src/main/res/ic_launcher-playstore.png")).convert("RGBA")
    master.save(os.path.join(logo_dir, "wisdomone_logo_512.png"))
    master.resize((256, 256), Image.LANCZOS).save(os.path.join(logo_dir, "wisdomone_logo_256.png"))
    round_ = master.copy(); round_.putalpha(K.rounded_mask((512, 512), 256)); round_.save(os.path.join(logo_dir, "wisdomone_logo_round_512.png"))
    # 08 final exports (byte-identical copies → stored once by git)
    fin = os.path.join(ROOT, "08_FINAL_EXPORTS")
    for d in ("facebook_feed", "instagram_feed", "stories", "reels", "preview"):
        os.makedirs(os.path.join(fin, d), exist_ok=True)
    for a in SPEC.ADS:
        n = SPEC.ad_name(a)
        shutil.copy(img_path(a, "4x5"), os.path.join(fin, "facebook_feed", f"{n}_4x5.jpg"))
        shutil.copy(img_path(a, "1x1"), os.path.join(fin, "facebook_feed", f"{n}_1x1.jpg"))
        shutil.copy(img_path(a, "4x5"), os.path.join(fin, "instagram_feed", f"{n}_4x5.jpg"))
        shutil.copy(img_path(a, "1x1"), os.path.join(fin, "instagram_feed", f"{n}_1x1.jpg"))
        shutil.copy(img_path(a, "9x16"), os.path.join(fin, "stories", f"{n}_9x16.jpg"))
        if a.get("video"):
            shutil.copy(vid_path(a), os.path.join(fin, "reels", f"{n}_video.mp4"))
            sb = vid_path(a).replace(".mp4", "_storyboard.jpg")
            if os.path.exists(sb):
                shutil.copy(sb, os.path.join(fin, "preview", f"{n}_video_storyboard.jpg"))
    for fmt in SPEC.FORMATS:
        contact_sheet(fmt, os.path.join(fin, "preview", f"contact_sheet_25_ads_{fmt}.jpg"))
    # validation report
    n_img = sum(1 for r in rows); ok_img = sum(1 for a in SPEC.ADS for fmt, sz in SPEC.FORMATS.items() if check_image(img_path(a, fmt), sz) == "Validated")
    ok_vid = sum(1 for v, _ in video_facts.values() if v == "Validated")
    copies = {}
    for a in SPEC.ADS:
        copies.setdefault(a["primary"], []).append(SPEC.ad_name(a))
    dup_primary = [v for v in copies.values() if len(v) > 1]
    sets_ok = all(sum(1 for a in SPEC.ADS if a["set"] == s["id"]) == 5 for s in SPEC.AD_SETS)
    lines = ["# Validation report", "", "Generated by `tools/marketing/make_meta_docs.py` from the files on disk.", "",
             "| Check | Result |", "|---|---|",
             f"| 5 ad sets, 5 ads each | {'Pass' if sets_ok and len(SPEC.AD_SETS) == 5 else 'Fail'} |",
             f"| Unique ad names | {'Pass' if len({SPEC.ad_name(a) for a in SPEC.ADS}) == 25 else 'Fail'} |",
             f"| Unique primary texts (25) | {'Pass' if not dup_primary else 'Fail: ' + str(dup_primary)} |",
             f"| Unique compositions per ad set | {'Pass' if all(len({a['style'] for a in SPEC.ADS if a['set'] == s['id']}) >= 4 for s in SPEC.AD_SETS) else 'Check'} |",
             f"| Image files present with exact pixel size | {ok_img} / {len(SPEC.ADS) * 3} |",
             f"| Videos: H.264 + AAC, 1080×1920, yuv420p, 15–30 s, planned length | {ok_vid} / {len(SPEC.VIDEOS)} |",
             f"| Headlines ≤ 40 characters | {sum(len(a['headline']) <= 40 for a in SPEC.ADS)} / 25 |",
             f"| Descriptions ≤ 30 characters | {sum(len(a['description']) <= 30 for a in SPEC.ADS)} / 25 |",
             f"| CSV rows (25 ads × 3 ratios) all filenames resolve | {sum(r['Export Status'] == 'Validated' for r in rows)} / {n_img} validated |",
             "", "## Videos", "", "| File | Status | Duration | Video | Audio | Size |", "|---|---|---|---|---|---|"]
    for name, (st, fa) in video_facts.items():
        lines.append(f"| `{name}` | {st} | {fa.get('duration', '—')} s | {fa.get('video', '—')} | {fa.get('audio', '—')} | {fa.get('size_mb', '—')} MB |")
    lines += ["", "## Manual checks (done by reviewing every image and video storyboard)", "",
              "- Spelling and readability of all headlines, captions and copy.",
              "- App screens match the real app (they are real screenshots / app-renderer output — see `play_store_creatives/README.md`).",
              "- Brand consistency: palette, Palatino italic headlines, Avenir Next body, WisdomOne lock-up on every creative.",
              "- A real lock screen with a verse appears in 23 of 25 ads and in all 5 videos. The two exceptions are deliberate: `AS03_AD01` shows the",
              "  lock-screen verse wallpapers as a gallery (without the clock) and `AS03_AD02` shows the home-screen live wallpaper.",
              "- 9:16 images keep headlines below the top 13 % and above the bottom Reels/Stories UI zone.", ""]
    with open(os.path.join(fin, "preview", "validation_report.md"), "w") as f:
        f.write("\n".join(lines))
    print(f"images ok {ok_img}/75, videos ok {ok_vid}/5, csv rows {len(rows)}, dup primary {len(dup_primary)}")


if __name__ == "__main__":
    main()
