#!/usr/bin/env python3
"""Generates the tradition wallpaper packs (20 images each) bundled in android/app/src/main/assets/wallpapers/.

Original, procedurally drawn artwork (no third-party images): every pixel is produced by this script, so the packs have no
licensing encumbrance and can be regenerated or replaced. Hinduism's pack is the existing illustrated set and is not touched
unless --hinduism is passed (it only copies/renames it).

    pip install pillow numpy
    python tools/generate_wallpapers.py --out android/app/src/main/assets/wallpapers [--preview /tmp/sheet.jpg]

Output: <out>/<slug>/NN.webp (720x1280, WebP q82) for christianity, islam, buddhism, general, plus wallpaper_asset_manifest.json.
Religious figures are shown only as symbols / architecture / silhouettes (cross, mosque, geometric art, lotus, dharma wheel,
meditating silhouette) — never a depiction of a prophet or deity — and always in a respectful style.
"""
import argparse, json, math, os, random
import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

W, H = 720, 1280
SS = 2  # supersampling for vector shapes


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Scene:
    def __init__(self, seed):
        self.rng = random.Random(seed)
        self.np = np.random.default_rng(seed)
        self.img = Image.new("RGB", (W, H), (0, 0, 0))

    # ── backgrounds and light ─────────────────────────────────────────────
    def sky(self, stops, y0=0, y1=H):
        """Vertical gradient through [(pos 0..1, '#hex'), ...] between y0 and y1."""
        arr = np.zeros((y1 - y0, W, 3), dtype=np.float32)
        pos = [s[0] for s in stops]
        cols = np.array([hexc(s[1]) for s in stops], dtype=np.float32)
        t = np.linspace(0, 1, y1 - y0)
        for c in range(3):
            arr[:, :, c] = np.interp(t, pos, cols[:, c])[:, None]
        self.img.paste(Image.fromarray(arr.astype(np.uint8)), (0, y0))

    def glow(self, cx, cy, r, col, a=0.8, power=2.0):
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / max(r, 1)
        m = np.clip(1 - d, 0, 1) ** power * a
        base = np.asarray(self.img, dtype=np.float32)
        c = np.array(hexc(col) if isinstance(col, str) else col, dtype=np.float32)
        base = base * (1 - m[..., None]) + c * m[..., None]
        self.img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8))

    def add_glow(self, cx, cy, r, col, a=0.6):
        """Additive light (brighter, for suns and lanterns)."""
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / max(r, 1)
        m = np.clip(1 - d, 0, 1) ** 2 * a
        base = np.asarray(self.img, dtype=np.float32)
        c = np.array(hexc(col) if isinstance(col, str) else col, dtype=np.float32)
        self.img = Image.fromarray(np.clip(base + c * m[..., None], 0, 255).astype(np.uint8))

    def stars(self, n, y_max=0.6, col="#FFFFFF", size=1.4):
        def f(d, k):
            for _ in range(n):
                x, y = self.rng.random() * W, self.rng.random() * H * y_max
                r = size * (0.4 + self.rng.random() * 0.9)
                a = int(90 + self.rng.random() * 165)
                c = hexc(col) + (a,)
                d.ellipse([(x - r) * k, (y - r) * k, (x + r) * k, (y + r) * k], fill=c)
        self.layer(f)

    # ── drawing layer with anti-aliasing ─────────────────────────────────
    def layer(self, fn, alpha=1.0, blur=0.0):
        lay = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
        fn(ImageDraw.Draw(lay), SS)
        lay = lay.resize((W, H), Image.LANCZOS)
        if blur:
            lay = lay.filter(ImageFilter.GaussianBlur(blur))
        if alpha < 1.0:
            lay.putalpha(lay.getchannel("A").point(lambda v: int(v * alpha)))
        base = self.img.convert("RGBA")
        base.alpha_composite(lay)
        self.img = base.convert("RGB")

    # ── landscape pieces ─────────────────────────────────────────────────
    def ridge(self, base_y, amp, col, rough=1.0, seed=0, alpha=1.0, fade_to=None):
        r = random.Random(seed or self.rng.random())
        ph = [r.random() * 6.28 for _ in range(5)]
        pts = [(0, H)]
        for x in range(0, W + 8, 8):
            t = x / W
            y = base_y - amp * (0.55 * math.sin(t * 2.3 + ph[0]) + 0.3 * math.sin(t * 5.1 + ph[1]) * rough
                                + 0.18 * math.sin(t * 11 + ph[2]) * rough + 0.08 * math.sin(t * 23 + ph[3]) * rough)
            pts.append((x, y))
        pts.append((W, H))

        def f(d, k):
            d.polygon([(x * k, y * k) for x, y in pts], fill=hexc(col) + (255,))
        self.layer(f, alpha=alpha)

    def haze(self, y, h, col, a=0.5):
        arr = np.zeros((H, W), dtype=np.float32)
        t = np.clip(1 - np.abs(np.arange(H) - y) / h, 0, 1) ** 1.5 * a
        arr[:] = t[:, None]
        base = np.asarray(self.img, dtype=np.float32)
        c = np.array(hexc(col), dtype=np.float32)
        self.img = Image.fromarray(np.clip(base * (1 - arr[..., None]) + c * arr[..., None], 0, 255).astype(np.uint8))

    def water(self, y0, tint="#0B1F3A", a=0.35):
        top = self.img.crop((0, max(0, 2 * y0 - H), W, y0)).transpose(Image.FLIP_TOP_BOTTOM)
        top = top.resize((W, H - y0)) if top.size[1] != H - y0 else top
        top = top.filter(ImageFilter.GaussianBlur(2.2))
        arr = np.asarray(top, dtype=np.float32)
        yy = np.arange(arr.shape[0])[:, None, None]
        wob = np.sin(yy * 0.35) * 6
        for i in range(arr.shape[0]):
            arr[i] = np.roll(arr[i], int(wob[i, 0, 0]), axis=0)
        arr = arr * (1 - a) + np.array(hexc(tint), dtype=np.float32) * a
        self.img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)), (0, y0))

    def rays(self, cx, cy, col, n=11, a=0.16, spread=1.2):
        def f(d, k):
            for i in range(n):
                ang = -math.pi / 2 + (i - (n - 1) / 2) * spread / n
                w = 0.035
                p = [(cx, cy), (cx + math.cos(ang - w) * 1800, cy + math.sin(ang - w) * 1800), (cx + math.cos(ang + w) * 1800, cy + math.sin(ang + w) * 1800)]
                d.polygon([(x * k, y * k) for x, y in p], fill=hexc(col) + (int(255 * a),))
        self.layer(f, blur=3)

    def vignette(self, strength=0.45, bottom=0.25):
        yy, xx = np.mgrid[0:H, 0:W]
        d = np.sqrt(((xx - W / 2) / (W * 0.75)) ** 2 + ((yy - H * 0.45) / (H * 0.75)) ** 2)
        m = np.clip(d, 0, 1) ** 2 * strength + np.clip((yy - H * 0.72) / (H * 0.28), 0, 1) * bottom
        base = np.asarray(self.img, dtype=np.float32) * (1 - m[..., None])
        self.img = Image.fromarray(np.clip(base, 0, 255).astype(np.uint8))

    def grain(self, amt=5):
        n = self.np.normal(0, amt, (H, W, 1)).astype(np.float32)
        self.img = Image.fromarray(np.clip(np.asarray(self.img, dtype=np.float32) + n, 0, 255).astype(np.uint8))

    # ── motifs ────────────────────────────────────────────────────────────
    def sun(self, cx, cy, r, col="#FFE9A8", halo="#FFB86B"):
        self.add_glow(cx, cy, r * 6, halo, 0.55)
        self.add_glow(cx, cy, r * 2.2, col, 0.7)

        def f(d, k):
            d.ellipse([(cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k], fill=hexc(col) + (255,))
        self.layer(f)

    def moon(self, cx, cy, r, col="#F4EFD8"):
        self.add_glow(cx, cy, r * 5, col, 0.28)

        def f(d, k):
            d.ellipse([(cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k], fill=hexc(col) + (255,))
        self.layer(f)

    def crescent(self, cx, cy, r, col="#F6E7B4", glow=True):
        if glow:
            self.add_glow(cx, cy, r * 5, col, 0.3)

        def f(d, k):
            d.ellipse([(cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k], fill=hexc(col) + (255,))
            d.ellipse([(cx - r * 0.62) * k, (cy - r * 1.05) * k, (cx + r * 1.2) * k, (cy + r * 0.95) * k], fill=(0, 0, 0, 0))
        lay = Image.new("RGBA", (W * SS, H * SS), (0, 0, 0, 0))
        dd = ImageDraw.Draw(lay)
        dd.ellipse([(cx - r) * SS, (cy - r) * SS, (cx + r) * SS, (cy + r) * SS], fill=hexc(col) + (255,))
        mask = Image.new("L", lay.size, 0)
        ImageDraw.Draw(mask).ellipse([(cx - r * 0.62 + r * 0.38) * SS, (cy - r * 1.05 + 0.04 * r) * SS, (cx + r * 1.2 + r * 0.38) * SS, (cy + r * 0.95 + 0.04 * r) * SS], fill=255)
        lay.putalpha(ImageChops.subtract(lay.getchannel("A"), mask))
        lay = lay.resize((W, H), Image.LANCZOS)
        base = self.img.convert("RGBA"); base.alpha_composite(lay); self.img = base.convert("RGB")

    def cross(self, cx, cy, h, col="#FFF4D6", glow="#FFD27A"):
        self.add_glow(cx, cy, h * 1.6, glow, 0.5)
        w = h * 0.1

        def f(d, k):
            d.rectangle([(cx - w) * k, (cy - h / 2) * k, (cx + w) * k, (cy + h / 2) * k], fill=hexc(col) + (255,))
            d.rectangle([(cx - h * 0.3) * k, (cy - h * 0.22) * k, (cx + h * 0.3) * k, (cy - h * 0.22 + w * 1.7) * k], fill=hexc(col) + (255,))
        self.layer(f)

    def mosque(self, cx, base, s, col="#101B33", crescent_col="#F6E7B4", lit=None):
        def f(d, k):
            c = hexc(col) + (255,)
            def R(x0, y0, x1, y1): d.rectangle([x0 * k, y0 * k, x1 * k, y1 * k], fill=c)
            def E(x0, y0, x1, y1): d.ellipse([x0 * k, y0 * k, x1 * k, y1 * k], fill=c)
            R(cx - 140 * s, base - 120 * s, cx + 140 * s, base)
            E(cx - 105 * s, base - 235 * s, cx + 105 * s, base - 30 * s)           # main dome
            R(cx - 105 * s, base - 130 * s, cx + 105 * s, base - 110 * s)
            d.polygon([(cx * k, (base - 300 * s) * k), ((cx - 6 * s) * k, (base - 232 * s) * k), ((cx + 6 * s) * k, (base - 232 * s) * k)], fill=c)
            for sx in (-190, 190):
                x = cx + sx * s
                R(x - 17 * s, base - 330 * s, x + 17 * s, base)
                R(x - 25 * s, base - 345 * s, x + 25 * s, base - 325 * s)
                d.polygon([(x * k, (base - 410 * s) * k), ((x - 22 * s) * k, (base - 345 * s) * k), ((x + 22 * s) * k, (base - 345 * s) * k)], fill=c)
            for sx in (-85, 0, 85):                                              # glowing arched windows
                if lit:
                    x = cx + sx * s
                    d.ellipse([(x - 13 * s) * k, (base - 95 * s) * k, (x + 13 * s) * k, (base - 60 * s) * k], fill=hexc(lit) + (255,))
                    d.rectangle([(x - 13 * s) * k, (base - 78 * s) * k, (x + 13 * s) * k, (base - 22 * s) * k], fill=hexc(lit) + (255,))
        self.layer(f)
        if crescent_col:
            self.crescent(cx, base - 318 * s, 15 * s, crescent_col, glow=False)

    def geometric(self, cx, cy, size, col="#E9C46A", line=3.0, alpha=0.95, rings=3):
        """Islamic-style eight-point star lattice centred at (cx, cy)."""
        def f(d, k):
            c = hexc(col) + (int(255 * alpha),)
            step = size
            for gy in range(-8, 9):
                for gx in range(-6, 7):
                    x, y = cx + gx * step, cy + gy * step
                    if not (-step < x < W + step and -step < y < H + step):
                        continue
                    for rot in (0, math.pi / 4):
                        p = [(x + step * 0.5 * math.cos(rot + i * math.pi / 2 + math.pi / 4), y + step * 0.5 * math.sin(rot + i * math.pi / 2 + math.pi / 4)) for i in range(4)]
                        d.polygon([(a * k, b * k) for a, b in p], outline=c, width=int(line * k))
                    d.ellipse([(x - step * 0.2) * k, (y - step * 0.2) * k, (x + step * 0.2) * k, (y + step * 0.2) * k], outline=c, width=int(line * k * 0.7))
        self.layer(f)

    def medallion(self, cx, cy, r, col="#F6D98B", petals=16, line=3.0):
        def f(d, k):
            c = hexc(col) + (255,)
            for ring in range(1, 5):
                rr = r * ring / 4
                d.ellipse([(cx - rr) * k, (cy - rr) * k, (cx + rr) * k, (cy + rr) * k], outline=c, width=int(line * k))
            for i in range(petals):
                a = i * 2 * math.pi / petals
                x0, y0 = cx + math.cos(a) * r * 0.25, cy + math.sin(a) * r * 0.25
                x1, y1 = cx + math.cos(a) * r, cy + math.sin(a) * r
                d.line([x0 * k, y0 * k, x1 * k, y1 * k], fill=c, width=int(line * k * 0.8))
                m = (a + math.pi / petals)
                xm, ym = cx + math.cos(m) * r * 0.62, cy + math.sin(m) * r * 0.62
                d.polygon([(x0 * k, y0 * k), (xm * k, ym * k), (x1 * k, y1 * k)], outline=c, width=int(line * k * 0.7))
        self.layer(f)

    def arch(self, cx, base, w, h, col, inner=None, thick=14, pointed=True):
        def f(d, k):
            c = hexc(col) + (255,)
            if pointed:
                pts = [(cx - w / 2, base), (cx - w / 2, base - h * 0.55)]
                for i in range(0, 21):
                    t = i / 20
                    pts.append((cx - w / 2 + t * w / 2, base - h * 0.55 - math.sin(t * math.pi / 2) * h * 0.45))
                for i in range(20, -1, -1):
                    t = i / 20
                    pts.append((cx + w / 2 - t * w / 2, base - h * 0.55 - math.sin(t * math.pi / 2) * h * 0.45))
                pts.append((cx + w / 2, base))
            else:
                pts = [(cx - w / 2, base), (cx - w / 2, base - h + w / 2)]
                for i in range(0, 31):
                    a = math.pi + i * math.pi / 30
                    pts.append((cx + math.cos(a) * w / 2, base - h + w / 2 + math.sin(a) * w / 2))
                pts.append((cx + w / 2, base))
            d.polygon([(x * k, y * k) for x, y in pts], outline=c, width=int(thick * k))
            if inner:
                d.polygon([(x * k, y * k) for x, y in pts], fill=hexc(inner) + (210,))
        self.layer(f)

    def tree(self, cx, base, h, col, canopy=True, branches=0, seed=1):
        r = random.Random(seed)
        def f(d, k):
            c = hexc(col) + (255,)
            tw = h * 0.05
            d.polygon([((cx - tw) * k, base * k), ((cx - tw * 0.4) * k, (base - h * 0.55) * k), ((cx + tw * 0.4) * k, (base - h * 0.55) * k), ((cx + tw) * k, base * k)], fill=c)
            if canopy:
                for _ in range(46):
                    a = r.random() * 6.28; rr = r.random() ** 0.6 * h * 0.34
                    x, y = cx + math.cos(a) * rr * 1.25, base - h * 0.68 + math.sin(a) * rr * 0.8
                    s = h * (0.07 + r.random() * 0.07)
                    d.ellipse([(x - s) * k, (y - s) * k, (x + s) * k, (y + s) * k], fill=c)
            def branch(x, y, ang, ln, depth):
                if depth == 0 or ln < 6:
                    return
                x2, y2 = x + math.cos(ang) * ln, y + math.sin(ang) * ln
                d.line([x * k, y * k, x2 * k, y2 * k], fill=c, width=max(1, int(depth * 1.6 * k)))
                branch(x2, y2, ang - 0.45 - r.random() * 0.2, ln * 0.74, depth - 1)
                branch(x2, y2, ang + 0.45 + r.random() * 0.2, ln * 0.74, depth - 1)
            if branches:
                branch(cx, base - h * 0.5, -math.pi / 2, h * 0.22, branches)
        self.layer(f)

    def lotus(self, cx, cy, r, petal="#FFC1D6", edge="#FF8FB5", layers=3):
        def f(d, k):
            for layer in range(layers):
                n = 8 - layer
                rr = r * (1 - layer * 0.22)
                for i in range(n):
                    a = (i - (n - 1) / 2) * (math.pi * 0.95 / max(n - 1, 1))
                    # petal polygon (pointed ellipse) rotated by a
                    pts = []
                    for t in range(0, 41):
                        u = t / 40
                        px = math.sin(u * math.pi) * rr * 0.28 * (1 if t <= 20 else 1)
                        py = -u * rr
                        pts.append((px, py))
                    left = [(-x, y) for x, y in reversed(pts)]
                    poly = pts + left
                    ca, sa = math.cos(a), math.sin(a)
                    pp = [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in poly]
                    col = mix(hexc(petal), hexc("#FFFFFF"), layer * 0.12) + (240,)
                    d.polygon([(x * k, y * k) for x, y in pp], fill=col, outline=hexc(edge) + (255,))
        self.layer(f)

    def dharma_wheel(self, cx, cy, r, col="#F4D58D", line=5, glow="#FFD27A"):
        self.add_glow(cx, cy, r * 2.4, glow, 0.35)
        def f(d, k):
            c = hexc(col) + (255,)
            d.ellipse([(cx - r) * k, (cy - r) * k, (cx + r) * k, (cy + r) * k], outline=c, width=int(line * k))
            d.ellipse([(cx - r * 0.78) * k, (cy - r * 0.78) * k, (cx + r * 0.78) * k, (cy + r * 0.78) * k], outline=c, width=int(line * 0.6 * k))
            d.ellipse([(cx - r * 0.16) * k, (cy - r * 0.16) * k, (cx + r * 0.16) * k, (cy + r * 0.16) * k], fill=c)
            for i in range(8):
                a = i * math.pi / 4
                d.line([(cx + math.cos(a) * r * 0.16) * k, (cy + math.sin(a) * r * 0.16) * k, (cx + math.cos(a) * r * 0.9) * k, (cy + math.sin(a) * r * 0.9) * k], fill=c, width=int(line * 0.8 * k))
                d.ellipse([(cx + math.cos(a) * r * 0.9 - r * 0.05) * k, (cy + math.sin(a) * r * 0.9 - r * 0.05) * k, (cx + math.cos(a) * r * 0.9 + r * 0.05) * k, (cy + math.sin(a) * r * 0.9 + r * 0.05) * k], fill=c)
        self.layer(f)

    def meditator(self, cx, base, s, col="#150E22", halo="#FFD27A", halo_r=1.0):
        """Seated meditating silhouette with a halo (symbolic, abstract)."""
        self.add_glow(cx, base - 210 * s, 260 * s * halo_r, halo, 0.55)
        def f(d, k):
            c = hexc(col) + (255,)
            d.ellipse([(cx - 120 * s) * k, (base - 120 * s) * k, (cx + 120 * s) * k, (base - 5 * s) * k], fill=c)               # lap
            d.polygon([((cx - 85 * s) * k, (base - 60 * s) * k), ((cx - 52 * s) * k, (base - 215 * s) * k), ((cx + 52 * s) * k, (base - 215 * s) * k), ((cx + 85 * s) * k, (base - 60 * s) * k)], fill=c)  # torso
            d.ellipse([(cx - 48 * s) * k, (base - 205 * s) * k, (cx - 12 * s) * k, (base - 150 * s) * k], fill=c)
            d.ellipse([(cx - 40 * s) * k, (base - 300 * s) * k, (cx + 40 * s) * k, (base - 215 * s) * k], fill=c)               # head
            d.ellipse([(cx - 14 * s) * k, (base - 322 * s) * k, (cx + 14 * s) * k, (base - 296 * s) * k], fill=c)               # ushnisha
            d.ellipse([(cx - 175 * s) * k, (base - 385 * s) * k, (cx + 175 * s) * k, (base - 35 * s) * k], outline=hexc(halo) + (200,), width=int(4 * k))
        self.layer(f)

    def pagoda(self, cx, base, s, col="#150E22", tiers=5):
        def f(d, k):
            c = hexc(col) + (255,)
            y = base
            w = 150 * s
            d.rectangle([(cx - w * 0.7) * k, (y - 40 * s) * k, (cx + w * 0.7) * k, y * k], fill=c)
            y -= 40 * s
            for i in range(tiers):
                th = 42 * s
                d.polygon([((cx - w) * k, y * k), ((cx - w * 0.55) * k, (y - th) * k), ((cx + w * 0.55) * k, (y - th) * k), ((cx + w) * k, y * k)], fill=c)
                d.rectangle([(cx - w * 0.5) * k, (y - th - 22 * s) * k, (cx + w * 0.5) * k, (y - th) * k], fill=c)
                y -= th + 22 * s
                w *= 0.8
            d.polygon([((cx - 6 * s) * k, y * k), (cx * k, (y - 80 * s) * k), ((cx + 6 * s) * k, y * k)], fill=c)
        self.layer(f)

    def stupa(self, cx, base, s, col="#150E22"):
        def f(d, k):
            c = hexc(col) + (255,)
            d.rectangle([(cx - 130 * s) * k, (base - 28 * s) * k, (cx + 130 * s) * k, base * k], fill=c)
            d.rectangle([(cx - 100 * s) * k, (base - 60 * s) * k, (cx + 100 * s) * k, (base - 28 * s) * k], fill=c)
            d.pieslice([(cx - 90 * s) * k, (base - 210 * s) * k, (cx + 90 * s) * k, (base - 30 * s) * k], 180, 360, fill=c)
            d.rectangle([(cx - 22 * s) * k, (base - 250 * s) * k, (cx + 22 * s) * k, (base - 200 * s) * k], fill=c)
            for i in range(5):
                yy = base - 255 * s - i * 14 * s
                ww = (40 - i * 6) * s
                d.rectangle([(cx - ww) * k, (yy - 8 * s) * k, (cx + ww) * k, yy * k], fill=c)
            d.polygon([((cx - 5 * s) * k, (base - 330 * s) * k), (cx * k, (base - 380 * s) * k), ((cx + 5 * s) * k, (base - 330 * s) * k)], fill=c)
        self.layer(f)

    def church(self, cx, base, s, col="#0F1424", lit="#FFD27A"):
        def f(d, k):
            c = hexc(col) + (255,)
            d.rectangle([(cx - 150 * s) * k, (base - 130 * s) * k, (cx + 60 * s) * k, base * k], fill=c)
            d.polygon([((cx - 165 * s) * k, (base - 128 * s) * k), ((cx - 45 * s) * k, (base - 215 * s) * k), ((cx + 75 * s) * k, (base - 128 * s) * k)], fill=c)
            d.rectangle([(cx + 40 * s) * k, (base - 300 * s) * k, (cx + 120 * s) * k, base * k], fill=c)
            d.polygon([((cx + 30 * s) * k, (base - 296 * s) * k), ((cx + 80 * s) * k, (base - 420 * s) * k), ((cx + 130 * s) * k, (base - 296 * s) * k)], fill=c)
            d.rectangle([(cx + 78 * s) * k, (base - 470 * s) * k, (cx + 82 * s) * k, (base - 420 * s) * k], fill=c)
            d.rectangle([(cx + 66 * s) * k, (base - 456 * s) * k, (cx + 94 * s) * k, (base - 452 * s) * k], fill=c)
            for x in (-110, -60, -10):
                d.ellipse([(cx + x * s - 11 * s) * k, (base - 96 * s) * k, (cx + x * s + 11 * s) * k, (base - 60 * s) * k], fill=hexc(lit) + (255,))
                d.rectangle([(cx + x * s - 11 * s) * k, (base - 78 * s) * k, (cx + x * s + 11 * s) * k, (base - 30 * s) * k], fill=hexc(lit) + (255,))
            d.ellipse([(cx + 66 * s) * k, (base - 240 * s) * k, (cx + 94 * s) * k, (base - 200 * s) * k], fill=hexc(lit) + (255,))
        self.layer(f)

    def dove(self, cx, cy, s, col="#FFFFFF"):
        self.add_glow(cx, cy, 200 * s, "#FFF1C2", 0.4)
        def f(d, k):
            c = hexc(col) + (255,)
            d.ellipse([(cx - 46 * s) * k, (cy - 18 * s) * k, (cx + 46 * s) * k, (cy + 22 * s) * k], fill=c)
            d.ellipse([(cx + 30 * s) * k, (cy - 34 * s) * k, (cx + 60 * s) * k, (cy - 6 * s) * k], fill=c)
            d.polygon([((cx + 58 * s) * k, (cy - 22 * s) * k), ((cx + 76 * s) * k, (cy - 18 * s) * k), ((cx + 58 * s) * k, (cy - 14 * s) * k)], fill=hexc("#E9B872") + (255,))
            d.polygon([((cx - 40 * s) * k, (cy - 5 * s) * k), ((cx - 100 * s) * k, (cy - 60 * s) * k), ((cx - 10 * s) * k, (cy - 30 * s) * k)], fill=c)
            d.polygon([((cx - 5 * s) * k, (cy - 8 * s) * k), ((cx - 50 * s) * k, (cy - 85 * s) * k), ((cx + 25 * s) * k, (cy - 25 * s) * k)], fill=c)
            d.polygon([((cx - 44 * s) * k, cy * k), ((cx - 98 * s) * k, (cy + 20 * s) * k), ((cx - 40 * s) * k, (cy + 14 * s) * k)], fill=c)
        self.layer(f)

    def flame(self, cx, cy, s, col="#FFD27A"):
        self.add_glow(cx, cy, 130 * s, col, 0.75)
        def f(d, k):
            d.polygon([((cx - 9 * s) * k, (cy + 12 * s) * k), (cx * k, (cy - 34 * s) * k), ((cx + 9 * s) * k, (cy + 12 * s) * k)], fill=hexc("#FFF6D8") + (255,))
            d.ellipse([(cx - 9 * s) * k, (cy + 2 * s) * k, (cx + 9 * s) * k, (cy + 20 * s) * k], fill=hexc("#FFF6D8") + (255,))
        self.layer(f)

    def lantern(self, cx, cy, s, col="#F2A93B", frame="#2A1B10"):
        self.add_glow(cx, cy, 190 * s, col, 0.65)
        def f(d, k):
            d.line([cx * k, (cy - 160 * s) * k, cx * k, (cy - 55 * s) * k], fill=hexc(frame) + (255,), width=int(3 * k))
            d.polygon([((cx - 24 * s) * k, (cy - 50 * s) * k), (cx * k, (cy - 76 * s) * k), ((cx + 24 * s) * k, (cy - 50 * s) * k)], fill=hexc(frame) + (255,))
            d.polygon([((cx - 28 * s) * k, (cy - 50 * s) * k), ((cx + 28 * s) * k, (cy - 50 * s) * k), ((cx + 20 * s) * k, (cy + 40 * s) * k), ((cx - 20 * s) * k, (cy + 40 * s) * k)], fill=hexc(col) + (230,), outline=hexc(frame) + (255,))
            d.polygon([((cx - 20 * s) * k, (cy + 40 * s) * k), ((cx + 20 * s) * k, (cy + 40 * s) * k), (cx * k, (cy + 66 * s) * k)], fill=hexc(frame) + (255,))
            for x in (-10, 0, 10):
                d.line([(cx + x * s) * k, (cy - 48 * s) * k, (cx + x * 0.7 * s) * k, (cy + 38 * s) * k], fill=hexc(frame) + (190,), width=int(2 * k))
        self.layer(f)

    def flower(self, cx, cy, r, petal="#FF9EB5", center="#FFD166", n=8):
        def f(d, k):
            for i in range(n):
                a = i * 2 * math.pi / n
                x, y = cx + math.cos(a) * r * 0.55, cy + math.sin(a) * r * 0.55
                d.ellipse([(x - r * 0.45) * k, (y - r * 0.45) * k, (x + r * 0.45) * k, (y + r * 0.45) * k], fill=hexc(petal) + (235,))
            d.ellipse([(cx - r * 0.28) * k, (cy - r * 0.28) * k, (cx + r * 0.28) * k, (cy + r * 0.28) * k], fill=hexc(center) + (255,))
        self.layer(f)

    def aurora(self, cols, y=0.25):
        for i, c in enumerate(cols):
            def f(d, k, c=c, i=i):
                pts = []
                for x in range(0, W + 10, 10):
                    t = x / W
                    yy = H * (y + i * 0.05) + math.sin(t * 5 + i) * 70 + math.sin(t * 11 + i * 2) * 25
                    pts.append((x, yy))
                poly = pts + [(x, yy + 260 + i * 40) for x, yy in reversed(pts)]
                d.polygon([(a * k, b * k) for a, b in poly], fill=hexc(c) + (90,))
            self.layer(f, blur=26)

    def waves(self, base, cols, amp=26):
        for i, c in enumerate(cols):
            def f(d, k, c=c, i=i):
                pts = [(0, H)]
                for x in range(0, W + 6, 6):
                    pts.append((x, base + i * 70 + math.sin(x / 70 + i * 1.7) * amp + math.sin(x / 31 + i) * amp * 0.35))
                pts.append((W, H))
                d.polygon([(a * k, b * k) for a, b in pts], fill=hexc(c) + (255,))
            self.layer(f)

    def waterfall(self, cx, top, bottom, w, col="#EAF6FF"):
        def f(d, k):
            for _ in range(60):
                x = cx + (self.rng.random() - 0.5) * w
                a = int(60 + self.rng.random() * 140)
                d.line([x * k, top * k, (x + (self.rng.random() - 0.5) * 8) * k, bottom * k], fill=hexc(col) + (a,), width=int((1 + self.rng.random() * 3) * k))
        self.layer(f, blur=1.2)

    def save(self, path):
        self.vignette(); self.grain(3.5)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        self.img.save(path, "WEBP", quality=82, method=6)


# ══════════════════════════════════════ Christianity ══════════════════════════════════════
def christianity(i):
    s = Scene(100 + i)
    P = [
        ("#1B2A5C", "#7B5EA7", "#F3A683", "#FFD9A0"), ("#12203F", "#3C4D8A", "#E58E7A", "#F9C98B"), ("#0B1530", "#2B3A67", "#8E6AA0", "#F0A27C"),
        ("#26335E", "#8B6BA8", "#F7B8A0", "#FFE2B0"), ("#0A1B2E", "#1F4E6B", "#62A9A8", "#F6D9A0"), ("#2A1738", "#7A3E6B", "#E8796B", "#FFC78B"),
    ]
    p = P[i % len(P)]
    if i == 0:   # cross on hill at sunrise
        s.sky([(0, p[0]), (0.5, p[1]), (0.8, p[2]), (1, p[3])]); s.sun(W * 0.5, H * 0.52, 46); s.rays(W * 0.5, H * 0.52, "#FFE8B0", 13, 0.12, 1.8)
        s.ridge(H * 0.72, 90, "#2A2140", seed=1); s.cross(W * 0.5, H * 0.6, 270, "#FFF1CF"); s.ridge(H * 0.84, 60, "#150F26", seed=2)
    elif i == 1:  # three crosses dusk
        s.sky([(0, "#0F1B3A"), (0.55, "#6B4F8F"), (0.85, "#F08F6E"), (1, "#FFC57A")]); s.sun(W * 0.5, H * 0.7, 70, "#FFD9A0", "#FF9B6A")
        s.ridge(H * 0.74, 60, "#1B1330", seed=5)
        for dx, hh in ((-150, 200), (0, 260), (150, 200)):
            s.cross(W * 0.5 + dx, H * 0.66 - (0 if dx == 0 else -10), hh, "#140C22", "#FF9B6A")
        s.ridge(H * 0.86, 40, "#0B0716", seed=6)
    elif i == 2:  # church at night with stars
        s.sky([(0, "#050B1E"), (0.6, "#16264F"), (1, "#3B3A6E")]); s.stars(160); s.moon(W * 0.78, H * 0.17, 42)
        s.ridge(H * 0.75, 80, "#0C1226", seed=3); s.church(W * 0.45, H * 0.86, 1.15); s.ridge(H * 0.9, 30, "#070A16", seed=4)
        s.add_glow(W * 0.4, H * 0.8, 260, "#FFB454", 0.25)
    elif i == 3:  # stained glass rose window
        s.sky([(0, "#1B1033"), (1, "#0B0820")]); cx, cy = W * 0.5, H * 0.38
        cols = ["#E63946", "#F4A261", "#E9C46A", "#2A9D8F", "#457B9D", "#8E5BD0"]
        def f(d, k):
            for ring, rr in enumerate((250, 190, 130)):
                n = 12 - ring * 2
                for j in range(n):
                    a0 = j * 2 * math.pi / n; a1 = (j + 1) * 2 * math.pi / n
                    c = hexc(cols[(j + ring) % len(cols)]) + (205,)
                    d.pieslice([(cx - rr) * k, (cy - rr) * k, (cx + rr) * k, (cy + rr) * k], math.degrees(a0), math.degrees(a1), fill=c, outline=(20, 12, 36, 255), width=int(5 * k))
            d.ellipse([(cx - 262) * k, (cy - 262) * k, (cx + 262) * k, (cy + 262) * k], outline=(20, 12, 36, 255), width=int(14 * k))
            d.ellipse([(cx - 52) * k, (cy - 52) * k, (cx + 52) * k, (cy + 52) * k], fill=(255, 232, 170, 255), outline=(20, 12, 36, 255), width=int(6 * k))
        s.add_glow(cx, cy, 460, "#FF9F6B", 0.35); s.layer(f); s.cross(cx, cy, 80, "#7A4A12", "#FFD27A")
    elif i == 4:  # dove with rays
        s.sky([(0, "#1D3A70"), (0.5, "#86A8D6"), (1, "#F7E3C2")]); s.rays(W * 0.5, H * 0.1, "#FFFFFF", 15, 0.2, 2.2); s.add_glow(W * 0.5, H * 0.12, 420, "#FFFFFF", 0.7)
        s.dove(W * 0.5, H * 0.34, 2.0); s.ridge(H * 0.84, 70, "#6D8CB5", seed=7, alpha=0.8); s.ridge(H * 0.9, 45, "#3C5C8C", seed=8)
    elif i == 5:  # candlelit open book
        s.sky([(0, "#0D0A14"), (1, "#2A1B1F")]); s.add_glow(W * 0.5, H * 0.5, 420, "#FFB454", 0.45)
        def f(d, k):
            d.polygon([(W * 0.5 * k, H * 0.62 * k), (W * 0.1 * k, H * 0.55 * k), (W * 0.1 * k, H * 0.4 * k), (W * 0.5 * k, H * 0.47 * k)], fill=(246, 232, 200, 255), outline=(120, 80, 40, 255))
            d.polygon([(W * 0.5 * k, H * 0.62 * k), (W * 0.9 * k, H * 0.55 * k), (W * 0.9 * k, H * 0.4 * k), (W * 0.5 * k, H * 0.47 * k)], fill=(240, 224, 190, 255), outline=(120, 80, 40, 255))
            for j in range(9):
                y = 0.435 + j * 0.016
                d.line([W * 0.15 * k, H * (y + 0.01) * k, W * 0.46 * k, H * (y + 0.045) * k], fill=(150, 120, 90, 150), width=int(2 * k))
                d.line([W * 0.54 * k, H * (y + 0.045) * k, W * 0.85 * k, H * (y + 0.01) * k], fill=(150, 120, 90, 150), width=int(2 * k))
        s.layer(f); s.flame(W * 0.5, H * 0.33, 1.6); s.cross(W * 0.5, H * 0.2, 90, "#FFE9B8", "#FFB454")
    elif i == 6:  # shepherd hills at dawn
        s.sky([(0, "#2B2F6B"), (0.5, "#C58EB3"), (0.78, "#FFC2A1"), (1, "#FFE8B8")]); s.sun(W * 0.3, H * 0.62, 38)
        s.ridge(H * 0.62, 70, "#8E6BA0", seed=9, alpha=0.8); s.ridge(H * 0.72, 80, "#5A4580", seed=10); s.ridge(H * 0.82, 70, "#2D2150", seed=11)
        s.tree(W * 0.75, H * 0.8, 260, "#150F2A", canopy=True, seed=3)
    elif i == 7:  # starry night over a cross
        s.sky([(0, "#02040F"), (0.5, "#0B1A3D"), (1, "#27386B")]); s.stars(240); s.glow(W * 0.5, H * 0.45, 380, "#5F7BD6", 0.28)
        s.cross(W * 0.5, H * 0.36, 300, "#EAF1FF", "#7FA4FF"); s.ridge(H * 0.8, 70, "#070B1A", seed=12)
    elif i == 8:  # gothic arches light
        s.sky([(0, "#150F26"), (1, "#3A2347")]); s.add_glow(W * 0.5, H * 0.55, 480, "#FFC97A", 0.5)
        for j, (w, h, th) in enumerate(((560, 1020, 22), (430, 800, 18), (310, 600, 14))):
            s.arch(W * 0.5, H * 0.96, w, h, "#0B0716" if j == 0 else "#2A1B3E", inner="#FFD79A" if j == 2 else None, thick=th)
        s.rays(W * 0.5, H * 0.5, "#FFE8B8", 9, 0.12, 1.4)
    elif i == 9:  # sunrise over water with cross reflection
        s.sky([(0, "#1B2457"), (0.45, "#9A6FA8"), (0.62, "#F5A97F"), (0.68, "#FFE0A3")]); s.sun(W * 0.5, H * 0.6, 44)
        s.ridge(H * 0.66, 30, "#241A3F", seed=13); s.water(int(H * 0.66)); s.cross(W * 0.5, H * 0.5, 230, "#FFF0CF", "#FFC27A")
    elif i == 10:  # lilies
        s.sky([(0, "#22305E"), (0.6, "#7C85C0"), (1, "#F4D6C8")]); s.sun(W * 0.7, H * 0.2, 40, "#FFFFFF", "#FFE8C8")
        for j in range(6):
            s.lotus(W * (0.2 + 0.12 * j), H * (0.72 + 0.03 * (j % 2)), 90 + 14 * (j % 3), "#FFFFFF", "#D9C5E6", layers=2)
        s.ridge(H * 0.9, 40, "#2E4C5E", seed=14)
    elif i == 11:  # fish symbol + waves (ichthys)
        s.sky([(0, "#0B3558"), (0.6, "#2F7BA6"), (1, "#9AD7E0")]); s.sun(W * 0.5, H * 0.18, 40, "#FFFFFF", "#CFEFFF")
        def f(d, k):
            cx, cy = W * 0.5, H * 0.45
            c = (255, 255, 255, 235)
            d.arc([(cx - 220) * k, (cy - 120) * k, (cx + 100) * k, (cy + 120) * k], 205, 335, fill=c, width=int(14 * k))
            d.arc([(cx - 220) * k, (cy - 120) * k, (cx + 100) * k, (cy + 120) * k], 25, 155, fill=c, width=int(14 * k))
            d.arc([(cx - 100) * k, (cy - 120) * k, (cx + 220) * k, (cy + 120) * k], 205, 335, fill=c, width=int(14 * k))
            d.arc([(cx - 100) * k, (cy - 120) * k, (cx + 220) * k, (cy + 120) * k], 25, 155, fill=c, width=int(14 * k))
        s.layer(f); s.waves(H * 0.68, ["#2A8CB2", "#1F6F98", "#145A82", "#0C436A"])
    elif i == 12:  # crown of light, pastel sky
        s.sky([(0, "#2C2A5E"), (0.4, "#B28CC0"), (0.75, "#FFC7B5"), (1, "#FFE9C9")]); s.rays(W * 0.5, H * 0.55, "#FFFFFF", 17, 0.16, 2.4)
        s.cross(W * 0.5, H * 0.34, 220, "#FFFFFF", "#FFE0A8"); s.ridge(H * 0.8, 50, "#B58FB9", seed=15, alpha=0.9); s.ridge(H * 0.88, 40, "#6E4F8D", seed=16)
    elif i == 13:  # olive branch at dusk
        s.sky([(0, "#241A4A"), (0.6, "#B26D9A"), (1, "#FFC48B")]); s.sun(W * 0.25, H * 0.7, 50)
        s.tree(W * 0.62, H * 0.9, 380, "#1A1330", canopy=False, branches=7, seed=21); s.ridge(H * 0.92, 25, "#0E0A1E", seed=17)
    elif i == 14:  # candle row
        s.sky([(0, "#0A0712"), (1, "#23141D")])
        for j in range(7):
            x = W * (0.1 + 0.13 * j); y = H * (0.5 + 0.03 * math.sin(j * 1.2)); s.flame(x, y, 1.1 + 0.15 * (j % 3), "#FFC067")
        s.add_glow(W * 0.5, H * 0.5, 520, "#FF9F4A", 0.25); s.cross(W * 0.5, H * 0.22, 160, "#FFE3A8", "#FFB454")
    elif i == 15:  # mountain with cross at blue hour
        s.sky([(0, "#0D1B3F"), (0.5, "#4A5E9E"), (0.85, "#E5A6B5"), (1, "#FFD9B0")]); s.moon(W * 0.25, H * 0.2, 30)
        s.ridge(H * 0.6, 160, "#3C4A7E", seed=22, rough=1.4, alpha=0.9); s.ridge(H * 0.72, 120, "#26305C", seed=23, rough=1.2); s.cross(W * 0.55, H * 0.5, 130, "#FFF0CF", "#FFC27A")
        s.ridge(H * 0.86, 60, "#0F1530", seed=24)
    elif i == 16:  # golden wheat field
        s.sky([(0, "#2F3B7C"), (0.5, "#F2A97A"), (1, "#FFE08A")]); s.sun(W * 0.5, H * 0.52, 55, "#FFF3C4", "#FFC86B")
        s.ridge(H * 0.62, 40, "#A6733A", seed=25); s.ridge(H * 0.72, 40, "#7C531F", seed=26)
        def f(d, k):
            for _ in range(220):
                x = self_rng() * W; y = H * (0.7 + self_rng() * 0.28); h = 40 + self_rng() * 60
                d.line([x * k, y * k, (x + 6) * k, (y - h) * k], fill=(110, 70, 20, 230), width=int(2 * k))
                d.ellipse([(x + 3) * k, (y - h - 10) * k, (x + 11) * k, (y - h + 8) * k], fill=(230, 180, 70, 255))
        self_rng = s.rng.random; s.layer(f)
    elif i == 17:  # arched doorway to light
        s.sky([(0, "#111A3A"), (1, "#2A1F46")]); s.add_glow(W * 0.5, H * 0.52, 520, "#FFE2A8", 0.7); s.arch(W * 0.5, H * 0.95, 420, 860, "#07050F", inner="#FFE8B8", thick=34, pointed=False)
        s.rays(W * 0.5, H * 0.5, "#FFFFFF", 11, 0.13, 1.6)
    elif i == 18:  # sunset over chapel silhouette
        s.sky([(0, "#2B1B54"), (0.5, "#CF5C78"), (0.78, "#FFA05E"), (1, "#FFD27A")]); s.sun(W * 0.38, H * 0.64, 70, "#FFE9A8", "#FF8E5A")
        s.ridge(H * 0.8, 50, "#2A1230", seed=31); s.church(W * 0.55, H * 0.83, 0.9, col="#1B0B26", lit="#FFB454"); s.ridge(H * 0.9, 25, "#12061A", seed=32)
    else:         # light through clouds
        s.sky([(0, "#27407A"), (0.45, "#8EA6D8"), (1, "#F6E8D2")]); s.rays(W * 0.5, H * 0.18, "#FFFFFF", 19, 0.22, 2.0); s.add_glow(W * 0.5, H * 0.18, 380, "#FFFFFF", 0.65)
        s.dove(W * 0.5, H * 0.4, 1.4); s.cross(W * 0.5, H * 0.62, 180, "#FFFFFF", "#FFE9B0"); s.ridge(H * 0.9, 50, "#7E96C4", seed=33)
    return s


# ══════════════════════════════════════ Islam ══════════════════════════════════════
def islam(i):
    s = Scene(200 + i)
    if i == 0:   # mosque, crescent, night
        s.sky([(0, "#04102B"), (0.6, "#133B6E"), (0.9, "#2F7E93"), (1, "#F0B77A")]); s.stars(150, 0.5); s.crescent(W * 0.72, H * 0.17, 38)
        s.ridge(H * 0.8, 40, "#0B1B34", seed=40); s.mosque(W * 0.5, H * 0.86, 1.25, "#06101F", None, lit="#FFC76B")
    elif i == 1:  # geometric gold on teal
        s.sky([(0, "#06384A"), (1, "#0B6B73")]); s.geometric(W * 0.5, H * 0.5, 120, "#F2D27A", 2.6, 0.75); s.glow(W * 0.5, H * 0.45, 420, "#0B6B73", 0.5); s.medallion(W * 0.5, H * 0.44, 190, "#FFE29A", 20, 3.4)
    elif i == 2:  # mosque at sunset
        s.sky([(0, "#2A1850"), (0.5, "#C4506F"), (0.78, "#FF9A5C"), (1, "#FFD27A")]); s.sun(W * 0.3, H * 0.66, 62, "#FFF0C0", "#FF7F4D")
        s.ridge(H * 0.8, 40, "#401B3E", seed=41, alpha=0.8); s.mosque(W * 0.58, H * 0.88, 1.3, "#1B0A2A", "#FFE29A")
    elif i == 3:  # mihrab arch gold
        s.sky([(0, "#0A1F3D"), (1, "#123B5E")]); s.add_glow(W * 0.5, H * 0.5, 520, "#F4C95D", 0.35)
        for j, (w, h, th) in enumerate(((600, 1060, 26), (470, 860, 20), (340, 660, 14))):
            s.arch(W * 0.5, H * 0.96, w, h, "#E8C468" if j != 1 else "#C79A34", thick=th, pointed=True, inner="#0C2B4F" if j == 2 else None)
        s.geometric(W * 0.5, H * 0.5, 90, "#E8C468", 1.6, 0.35)
    elif i == 4:  # crescent + stars midnight
        s.sky([(0, "#030818"), (0.7, "#0C1F4A"), (1, "#1D4E7A")]); s.stars(260, 0.85); s.crescent(W * 0.5, H * 0.34, 120, "#FFF1C0"); s.ridge(H * 0.9, 50, "#050B1A", seed=42)
    elif i == 5:  # lanterns
        s.sky([(0, "#1A0D2E"), (1, "#42204A")])
        for j, (x, y, sc) in enumerate(((0.22, 0.34, 1.2), (0.5, 0.2, 1.5), (0.78, 0.38, 1.1), (0.36, 0.58, 0.9), (0.68, 0.62, 1.0))):
            s.lantern(W * x, H * y, sc, "#F2A93B" if j % 2 == 0 else "#E56B6F")
        s.stars(80, 0.9, "#FFE9B0", 1.1)
    elif i == 6:  # geometric blue with emerald
        s.sky([(0, "#0B1E5B"), (1, "#0F5E6A")]); s.geometric(W * 0.5, H * 0.5, 100, "#9FE0D4", 2.2, 0.55); s.medallion(W * 0.5, H * 0.5, 280, "#F6D98B", 24, 3.0)
    elif i == 7:  # dome + minaret silhouette, blue hour
        s.sky([(0, "#101E4A"), (0.55, "#4B5FA3"), (0.85, "#F0A98A"), (1, "#FFE0A8")]); s.crescent(W * 0.22, H * 0.2, 30, "#FFF1C0")
        s.ridge(H * 0.84, 30, "#1B2250", seed=43, alpha=0.7); s.mosque(W * 0.44, H * 0.9, 1.35, "#0E1431", "#FFF1C0")
    elif i == 8:  # mosque reflection on water
        s.sky([(0, "#07163A"), (0.5, "#1D4E8F"), (0.64, "#F7B77E")]); s.stars(90, 0.4); s.moon(W * 0.8, H * 0.16, 30)
        s.ridge(H * 0.6, 20, "#0B1B34", seed=44); s.mosque(W * 0.5, H * 0.62, 1.1, "#06101F", "#F6E7B4", lit="#FFC76B"); s.water(int(H * 0.62), "#06163A", 0.45)
    elif i == 9:  # rub el hizb star
        s.sky([(0, "#1B0F3A"), (1, "#3B1F5E")])
        def f(d, k):
            cx, cy = W * 0.5, H * 0.42
            for rot in (0, math.pi / 4):
                p = [(cx + 270 * math.cos(rot + j * math.pi / 2 + math.pi / 4), cy + 270 * math.sin(rot + j * math.pi / 2 + math.pi / 4)) for j in range(4)]
                d.polygon([(a * k, b * k) for a, b in p], outline=(246, 217, 139, 255), width=int(7 * k), fill=(70, 35, 110, 120))
            d.ellipse([(cx - 105) * k, (cy - 105) * k, (cx + 105) * k, (cy + 105) * k], outline=(246, 217, 139, 255), width=int(5 * k))
        s.add_glow(W * 0.5, H * 0.42, 460, "#C79BFF", 0.35); s.layer(f); s.geometric(W * 0.5, H * 0.42, 70, "#E8C468", 1.3, 0.25); s.stars(80, 0.9)
    elif i == 10:  # desert dunes and crescent
        s.sky([(0, "#14224F"), (0.5, "#6B5B95"), (0.8, "#F4A582"), (1, "#FFD59A")]); s.crescent(W * 0.7, H * 0.2, 42); s.stars(70, 0.4)
        s.ridge(H * 0.7, 60, "#B96F56", seed=45, rough=0.5); s.ridge(H * 0.8, 70, "#8A4A48", seed=46, rough=0.5); s.ridge(H * 0.9, 50, "#4D2744", seed=47, rough=0.4)
    elif i == 11:  # tessellation emerald and gold
        s.sky([(0, "#033D2F"), (1, "#0A6B4D")]); s.geometric(W * 0.5, H * 0.5, 80, "#E6C86A", 2.0, 0.6); s.glow(W * 0.5, H * 0.5, 380, "#033D2F", 0.55); s.medallion(W * 0.5, H * 0.5, 150, "#FFE29A", 16, 3)
    elif i == 12:  # twin minarets at dawn
        s.sky([(0, "#18275E"), (0.5, "#9B7FB6"), (0.78, "#FFB68F"), (1, "#FFE5B0")]); s.sun(W * 0.5, H * 0.66, 50)
        s.ridge(H * 0.78, 30, "#4A3A6D", seed=48, alpha=0.8); s.mosque(W * 0.5, H * 0.9, 1.4, "#241A45", "#FFE9B0")
    elif i == 13:  # archway to garden
        s.sky([(0, "#0B2A44"), (1, "#16465C")]); s.add_glow(W * 0.5, H * 0.55, 420, "#9FE8C6", 0.4)
        s.arch(W * 0.5, H * 0.97, 520, 980, "#F0D28A", thick=22, inner="#133A55"); s.arch(W * 0.5, H * 0.97, 380, 780, "#C9A24B", thick=14, inner="#1E5E72")
        s.flower(W * 0.38, H * 0.8, 40, "#FFB3C6"); s.flower(W * 0.62, H * 0.82, 46, "#FFD6A5"); s.flower(W * 0.5, H * 0.86, 34, "#FFFFFF")
    elif i == 14:  # crescent over minaret
        s.sky([(0, "#050B1F"), (1, "#26356B")]); s.stars(180, 0.8); s.crescent(W * 0.5, H * 0.22, 80)
        s.ridge(H * 0.9, 40, "#060A18", seed=49); s.mosque(W * 0.5, H * 0.94, 1.0, "#05080F", None, lit="#FFC76B")
    elif i == 15:  # gold on navy medallion
        s.sky([(0, "#0B1633"), (1, "#18305E")]); s.medallion(W * 0.5, H * 0.4, 300, "#F4D58D", 12, 4); s.medallion(W * 0.5, H * 0.4, 180, "#F4D58D", 8, 3); s.stars(60, 0.9, "#F4D58D", 1.0)
    elif i == 16:  # oasis night
        s.sky([(0, "#041029"), (0.7, "#173A6B"), (1, "#2F6F8E")]); s.stars(160, 0.6); s.moon(W * 0.3, H * 0.2, 36)
        s.ridge(H * 0.66, 50, "#0A1B33", seed=50); s.water(int(H * 0.7), "#06163A", 0.5); s.tree(W * 0.78, H * 0.7, 330, "#050C1A", seed=8)
    elif i == 17:  # lantern + geometric
        s.sky([(0, "#2B0F3F"), (1, "#5B1F52")]); s.geometric(W * 0.5, H * 0.5, 130, "#E8B96B", 2.0, 0.3); s.lantern(W * 0.5, H * 0.38, 2.0, "#F2A93B")
    elif i == 18:  # blue mosque at dusk
        s.sky([(0, "#0D1B4D"), (0.55, "#3D5FA8"), (0.85, "#F2A2A0"), (1, "#FFD9A0")]); s.ridge(H * 0.82, 25, "#233671", seed=51, alpha=0.8)
        s.mosque(W * 0.5, H * 0.9, 1.45, "#101A44", "#FFE9B0", lit="#FFD27A"); s.stars(60, 0.4)
    else:        # stars and arch
        s.sky([(0, "#02071A"), (1, "#162E5F")]); s.stars(220, 0.9); s.arch(W * 0.5, H * 0.95, 520, 1000, "#E6C86A", thick=16, inner=None); s.crescent(W * 0.5, H * 0.3, 60)
    return s


# ══════════════════════════════════════ Buddhism ══════════════════════════════════════
def buddhism(i):
    s = Scene(300 + i)
    if i == 0:   # meditator at sunset with halo
        s.sky([(0, "#2B1648"), (0.5, "#C4506F"), (0.8, "#FF9A5C"), (1, "#FFD27A")]); s.sun(W * 0.5, H * 0.46, 120, "#FFE9A8", "#FF8E5A"); s.ridge(H * 0.8, 40, "#2A1030", seed=60)
        s.meditator(W * 0.5, H * 0.84, 1.55, "#1A0B26")
    elif i == 1:  # dharma wheel gold on deep red
        s.sky([(0, "#3B0D1E"), (1, "#7A1F2E")]); s.dharma_wheel(W * 0.5, H * 0.4, 230, "#F6D98B", 9)
    elif i == 2:  # lotus pond dawn
        s.sky([(0, "#2B2B6E"), (0.5, "#B58AC4"), (0.7, "#FFC8B5")]); s.sun(W * 0.5, H * 0.52, 45); s.ridge(H * 0.62, 50, "#5E4B8E", seed=61); s.water(int(H * 0.64), "#1D2A5E", 0.4)
        for x, y, r in ((0.3, 0.78, 100), (0.68, 0.74, 120), (0.5, 0.88, 140)):
            s.lotus(W * x, H * y, r, "#FFC1D6", "#F08FB5")
    elif i == 3:  # stupa at dusk
        s.sky([(0, "#1B1B4B"), (0.55, "#8B5E9E"), (0.85, "#F4A27C"), (1, "#FFD59A")]); s.sun(W * 0.5, H * 0.56, 40); s.ridge(H * 0.78, 40, "#2A1E4A", seed=62); s.stupa(W * 0.5, H * 0.88, 1.5, "#150C26")
    elif i == 4:  # pagoda in mist mountains
        s.sky([(0, "#2C3A6B"), (0.6, "#9FB1D9"), (1, "#F2E4D4")]); s.ridge(H * 0.55, 170, "#8FA3CF", seed=63, alpha=0.7); s.haze(H * 0.62, 160, "#E7EEF8", 0.7)
        s.ridge(H * 0.72, 120, "#5D6EA3", seed=64); s.haze(H * 0.76, 120, "#E7EEF8", 0.5); s.pagoda(W * 0.62, H * 0.86, 1.1, "#1B2447")
    elif i == 5:  # bodhi tree + moon
        s.sky([(0, "#050B1F"), (0.6, "#1B2E62"), (1, "#3B4F8A")]); s.stars(120, 0.6); s.moon(W * 0.5, H * 0.3, 80, "#FFF6D8"); s.tree(W * 0.5, H * 0.9, 700, "#05081A", canopy=True, seed=9)
        s.meditator(W * 0.5, H * 0.9, 0.55, "#02030B", "#FFF1C0", 0.7)
    elif i == 6:  # single lotus bloom, deep blue
        s.sky([(0, "#0B1B3D"), (1, "#1E4E7A")]); s.add_glow(W * 0.5, H * 0.46, 420, "#7FD3E8", 0.35); s.lotus(W * 0.5, H * 0.58, 250, "#FFD1E3", "#F08FB5", 3)
        s.water(int(H * 0.7), "#0B1B3D", 0.5)
    elif i == 7:  # enso circle on cream with ink
        s.sky([(0, "#F3EAD7"), (1, "#E3D3B4")])
        def f(d, k):
            cx, cy, r = W * 0.5, H * 0.4, 230
            for t in range(0, 320):
                a = math.radians(30 + t)
                w = (22 - t / 320 * 18) * k * (0.8 + 0.3 * math.sin(t * 0.3))
                x, y = cx + math.cos(a) * r, cy + math.sin(a) * r
                d.ellipse([x * k - w, y * k - w, x * k + w, y * k + w], fill=(24, 18, 28, 235))
        s.layer(f, blur=0.8); s.sun(W * 0.78, H * 0.16, 26, "#C0392B", "#E8B9A0")
    elif i == 8:  # sunrise temple silhouettes
        s.sky([(0, "#201848"), (0.5, "#B05C86"), (0.78, "#FF9E6A"), (1, "#FFD88A")]); s.sun(W * 0.5, H * 0.62, 70); s.ridge(H * 0.74, 60, "#3A1F4A", seed=65)
        s.pagoda(W * 0.3, H * 0.84, 0.9, "#1B0C26"); s.stupa(W * 0.72, H * 0.85, 0.9, "#150A20"); s.ridge(H * 0.9, 25, "#0E0618", seed=66)
    elif i == 9:  # dharma wheel + clouds (blue)
        s.sky([(0, "#14306B"), (0.6, "#5E8AC9"), (1, "#E8F1FA")]); s.dharma_wheel(W * 0.5, H * 0.36, 200, "#FFE9A8", 7, "#FFFFFF"); s.haze(H * 0.8, 200, "#FFFFFF", 0.7)
    elif i == 10:  # stacked stones + water ripples
        s.sky([(0, "#2A3B5E"), (0.7, "#8AA0C0"), (1, "#E7EBF2")]); s.water(int(H * 0.62), "#2A3B5E", 0.25)
        def f(d, k):
            cx, y = W * 0.5, H * 0.62
            for j, (w, h) in enumerate(((190, 62), (140, 52), (100, 44), (64, 36), (36, 28))):
                d.ellipse([(cx - w / 2) * k, (y - h) * k, (cx + w / 2) * k, y * k], fill=(60 + j * 14, 62 + j * 14, 74 + j * 14, 255))
                y -= h * 0.72
            for r in (90, 150, 220):
                d.ellipse([(cx - r * 1.4) * k, (H * 0.64 - r * 0.22) * k, (cx + r * 1.4) * k, (H * 0.64 + r * 0.22) * k], outline=(255, 255, 255, 110), width=int(2 * k))
        s.layer(f)
    elif i == 11:  # candle + lotus lamp
        s.sky([(0, "#0A0A18"), (1, "#241636")]); s.add_glow(W * 0.5, H * 0.5, 480, "#FFB454", 0.4); s.lotus(W * 0.5, H * 0.58, 200, "#FFD9B8", "#F4A261", 3); s.flame(W * 0.5, H * 0.42, 2.0)
    elif i == 12:  # golden Buddha silhouette and bokeh
        s.sky([(0, "#3B1F0A"), (1, "#8B5A1F")]); s.add_glow(W * 0.5, H * 0.44, 520, "#FFC04D", 0.6); s.meditator(W * 0.5, H * 0.8, 1.7, "#2A1405", "#FFE08A", 1.4)
        def f(d, k):
            for _ in range(40):
                x, y, r = s.rng.random() * W, s.rng.random() * H, 6 + s.rng.random() * 26
                d.ellipse([(x - r) * k, (y - r) * k, (x + r) * k, (y + r) * k], fill=(255, 220, 140, int(20 + s.rng.random() * 40)))
        s.layer(f, blur=2)
    elif i == 13:  # misty forest path
        s.sky([(0, "#1C3A3E"), (0.6, "#6FA3A0"), (1, "#E1F0E3")]); s.rays(W * 0.6, 0, "#FFFFFF", 9, 0.15, 1.1)
        for j, x in enumerate((0.1, 0.28, 0.7, 0.9)):
            s.tree(W * x, H * (0.88 - 0.02 * j), 520 - j * 40, "#0F2A2A" if j % 2 else "#143736", canopy=True, seed=10 + j)
        s.haze(H * 0.7, 220, "#E1F0E3", 0.6)
    elif i == 14:  # sunrise over bell tower
        s.sky([(0, "#1D1E52"), (0.5, "#8C5FA3"), (0.8, "#FFAA7A"), (1, "#FFE0A0")]); s.sun(W * 0.5, H * 0.66, 55); s.ridge(H * 0.78, 40, "#3A2552", seed=67); s.pagoda(W * 0.5, H * 0.88, 1.35, "#190C28", tiers=6)
    elif i == 15:  # lotus field pink dusk
        s.sky([(0, "#3A2B6A"), (0.5, "#E1799F"), (1, "#FFD1A0")]); s.water(int(H * 0.5), "#3A2B6A", 0.3)
        for k in range(9):
            s.lotus(W * (0.1 + 0.1 * k), H * (0.62 + 0.04 * (k % 3) + 0.04 * (k // 3)), 70 + (k % 3) * 25, "#FFC1D6", "#F08FB5", 2)
    elif i == 16:  # moon over stupa
        s.sky([(0, "#030818"), (1, "#26356B")]); s.stars(200, 0.8); s.moon(W * 0.5, H * 0.26, 90, "#FFF6D8"); s.ridge(H * 0.84, 40, "#050A18", seed=68); s.stupa(W * 0.5, H * 0.9, 1.45, "#04070F")
    elif i == 17:  # wheel on orange
        s.sky([(0, "#C2410C"), (1, "#F59E0B")]); s.dharma_wheel(W * 0.5, H * 0.42, 260, "#FFF3C4", 10, "#FFE9A8")
    elif i == 18:  # mountain monastery
        s.sky([(0, "#1B2F63"), (0.55, "#7F9BD0"), (0.85, "#F0C6B0"), (1, "#FFE6B8")]); s.ridge(H * 0.5, 190, "#6E86BD", seed=69, rough=1.4, alpha=0.8); s.ridge(H * 0.66, 130, "#3D4F8A", seed=70)
        s.pagoda(W * 0.34, H * 0.76, 0.7, "#1A2350", 4); s.haze(H * 0.7, 140, "#F0E4DA", 0.5); s.ridge(H * 0.86, 60, "#1A2350", seed=71)
    else:        # tranquil lotus night
        s.sky([(0, "#050B22"), (1, "#1A3A66")]); s.stars(120, 0.5); s.moon(W * 0.72, H * 0.18, 34); s.water(int(H * 0.6), "#050B22", 0.4)
        s.lotus(W * 0.5, H * 0.74, 160, "#FFD6E8", "#F08FB5", 3); s.lotus(W * 0.2, H * 0.84, 90, "#FFC1D6", "#F08FB5", 2); s.lotus(W * 0.82, H * 0.82, 100, "#FFC1D6", "#F08FB5", 2)
    return s


# ══════════════════════════════════════ General wisdom (nature) ══════════════════════════════════════
def general(i):
    s = Scene(400 + i)
    if i == 0:   # alpine lake sunrise
        s.sky([(0, "#274C8F"), (0.45, "#F2A987"), (0.62, "#FFE4A8")]); s.sun(W * 0.5, H * 0.58, 50); s.ridge(H * 0.56, 150, "#5C6FA8", seed=80, rough=1.5, alpha=0.8); s.ridge(H * 0.64, 100, "#2F3E73", seed=81)
        s.water(int(H * 0.66), "#16254F", 0.3)
    elif i == 1:  # ocean sunset
        s.sky([(0, "#2B1A5C"), (0.5, "#D15C7A"), (0.72, "#FFA05E"), (0.8, "#FFE08A")]); s.sun(W * 0.5, H * 0.62, 80, "#FFF0B8", "#FF8E5A")
        s.waves(H * 0.68, ["#C65A6E", "#8E3D6E", "#5A2A68", "#2D1B52", "#180F3A"])
    elif i == 2:  # forest light rays
        s.sky([(0, "#0F2E2A"), (0.6, "#2E6B55"), (1, "#8FC9A2")]); s.rays(W * 0.7, -40, "#FFF6C8", 11, 0.22, 1.3)
        for j, x in enumerate((0.05, 0.22, 0.4, 0.62, 0.8, 0.95)):
            s.tree(W * x, H * (0.9 - 0.02 * (j % 3)), 560 - (j % 3) * 70, "#06201B" if j % 2 else "#0B2D25", seed=20 + j)
        s.haze(H * 0.75, 200, "#CFE9D6", 0.4)
    elif i == 3:  # waterfall
        s.sky([(0, "#1C3D5A"), (1, "#9CC7D6")]); s.ridge(H * 0.4, 100, "#2A5A4F", seed=82, rough=1.3); s.ridge(H * 0.48, 80, "#1E4A40", seed=83)
        s.waterfall(W * 0.5, H * 0.36, H * 0.74, 120); s.water(int(H * 0.74), "#16384A", 0.4); s.haze(H * 0.74, 120, "#FFFFFF", 0.4)
    elif i == 4:  # aurora
        s.sky([(0, "#020B1B"), (0.6, "#0B2A3E"), (1, "#123B4B")]); s.stars(180, 0.7); s.aurora(["#3CFFB4", "#4AA8FF", "#B26BFF"], 0.18); s.ridge(H * 0.8, 70, "#030A12", seed=84, rough=1.4); s.ridge(H * 0.9, 30, "#02060C", seed=85)
    elif i == 5:  # desert dunes
        s.sky([(0, "#3A4C93"), (0.5, "#F2A47E"), (1, "#FFE0A6")]); s.sun(W * 0.7, H * 0.4, 48)
        s.ridge(H * 0.62, 60, "#D98A5B", seed=86, rough=0.5); s.ridge(H * 0.72, 70, "#B3623F", seed=87, rough=0.5); s.ridge(H * 0.84, 60, "#7E3F34", seed=88, rough=0.4)
    elif i == 6:  # misty valley
        s.sky([(0, "#3A4C7A"), (0.6, "#B3BEDA"), (1, "#F2EEE9")]); s.ridge(H * 0.5, 120, "#8E9CC4", seed=89, alpha=0.7); s.haze(H * 0.55, 130, "#F2EEE9", 0.7)
        s.ridge(H * 0.64, 100, "#6F7EA8", seed=90, alpha=0.9); s.haze(H * 0.68, 130, "#F2EEE9", 0.6); s.ridge(H * 0.8, 80, "#46557F", seed=91)
    elif i == 7:  # flower meadow
        s.sky([(0, "#4B79C9"), (0.55, "#BFD8F2"), (1, "#FFF2D0")]); s.sun(W * 0.2, H * 0.2, 40, "#FFFFFF", "#FFE8A8"); s.ridge(H * 0.6, 60, "#7FB77E", seed=92); s.ridge(H * 0.7, 60, "#4E9A63", seed=93)
        for j in range(26):
            s.flower(W * s.rng.random(), H * (0.72 + 0.26 * s.rng.random()), 18 + s.rng.random() * 26, s.rng.choice(["#FF8FA3", "#FFD166", "#FFFFFF", "#C9A0FF", "#FF9F68"]))
    elif i == 8:  # moonlit lake
        s.sky([(0, "#05102B"), (0.55, "#1C3C70"), (1, "#315F8F")]); s.stars(140, 0.5); s.moon(W * 0.5, H * 0.22, 60); s.ridge(H * 0.55, 90, "#0A1A38", seed=94)
        s.water(int(H * 0.58), "#05102B", 0.35)
    elif i == 9:  # golden sunrise clouds
        s.sky([(0, "#38457F"), (0.4, "#D98FA6"), (0.7, "#FFC27A"), (1, "#FFE9A8")]); s.sun(W * 0.5, H * 0.66, 60, "#FFF6D0", "#FFB45E"); s.rays(W * 0.5, H * 0.66, "#FFE2A8", 15, 0.12, 2.2)
        s.haze(H * 0.5, 120, "#FFD7C2", 0.35); s.ridge(H * 0.8, 50, "#6B4F7E", seed=95); s.ridge(H * 0.9, 30, "#3A2A55", seed=96)
    elif i == 10:  # lone tree sunrise
        s.sky([(0, "#2B2B6E"), (0.5, "#E08FA0"), (0.8, "#FFC27A"), (1, "#FFE6A8")]); s.sun(W * 0.5, H * 0.58, 70); s.ridge(H * 0.74, 40, "#3A2552", seed=97); s.tree(W * 0.5, H * 0.78, 360, "#150A24", seed=2)
        s.ridge(H * 0.86, 25, "#0E0618", seed=98)
    elif i == 11:  # snowy peaks pink hour
        s.sky([(0, "#27407A"), (0.5, "#C8A0C8"), (0.9, "#FFD9C2")]); s.ridge(H * 0.5, 230, "#F5E9F2", seed=99, rough=1.6, alpha=0.95); s.ridge(H * 0.62, 170, "#9DA8D6", seed=100, rough=1.4)
        s.ridge(H * 0.78, 90, "#5668A0", seed=101); s.ridge(H * 0.88, 50, "#2E3A6E", seed=102)
    elif i == 12:  # sea of stars
        s.sky([(0, "#02030D"), (0.7, "#0B1C3F"), (1, "#173B67")]); s.stars(320, 0.9); s.glow(W * 0.5, H * 0.4, 500, "#6C7CD6", 0.2); s.ridge(H * 0.88, 60, "#02050C", seed=103, rough=1.3)
    elif i == 13:  # tropical beach
        s.sky([(0, "#2A5CA8"), (0.5, "#FFB38A"), (0.66, "#FFE3A5")]); s.sun(W * 0.5, H * 0.6, 52)
        s.waves(H * 0.66, ["#7FD6D2", "#46B5C0", "#2A8BA8", "#1E6A94"], 14); s.ridge(H * 0.9, 25, "#F2D6A0", seed=104, rough=0.4)
    elif i == 14:  # rain-forest canopy green
        s.sky([(0, "#062A1F"), (1, "#2F8F5A")]); s.rays(W * 0.3, -30, "#E7FFCF", 9, 0.2, 1.2)
        for j in range(5):
            s.tree(W * (0.1 + 0.2 * j), H * 0.94, 640 - 50 * (j % 2), "#041A12" if j % 2 else "#082A1D", seed=40 + j)
        s.haze(H * 0.8, 160, "#B9F0C6", 0.3)
    elif i == 15:  # lavender fields
        s.sky([(0, "#3C3C80"), (0.55, "#C58AC0"), (1, "#FFD9C0")]); s.sun(W * 0.5, H * 0.5, 44); s.ridge(H * 0.6, 40, "#7E5BA8", seed=105)
        def f(d, k):
            for row in range(18):
                y = H * (0.62 + row * 0.022)
                for x in range(-20, W + 40, 18 + row):
                    d.line([x * k, y * k, (x + 3) * k, (y - 40 - row * 3) * k], fill=(130 + row * 4, 80 + row * 3, 190, 255), width=int((3 + row * 0.25) * k))
        s.layer(f)
    elif i == 16:  # river bend
        s.sky([(0, "#254A7C"), (0.5, "#9CC0E0"), (1, "#F4EAD6")]); s.ridge(H * 0.5, 120, "#4C7C6A", seed=106); s.ridge(H * 0.62, 90, "#2F5F4F", seed=107)
        def f(d, k):
            d.polygon([(W * 0.2 * k, H * k), (W * 0.45 * k, H * 0.64 * k), (W * 0.55 * k, H * 0.64 * k), (W * 0.9 * k, H * k)], fill=(120, 175, 205, 230))
        s.layer(f, blur=1.5); s.ridge(H * 0.95, 30, "#1D4036", seed=108)
    elif i == 17:  # sunflowers at golden hour
        s.sky([(0, "#3A4F9A"), (0.5, "#F1A678"), (1, "#FFE08A")]); s.sun(W * 0.5, H * 0.5, 50, "#FFF0B8", "#FFC86B")
        for j in range(8):
            x = W * (0.08 + 0.12 * j); y = H * (0.7 + 0.04 * math.sin(j * 2))
            s.flower(x, y, 52 + 6 * (j % 3), "#FFC93C", "#5C3A12", 12)
        s.ridge(H * 0.94, 25, "#2D4A22", seed=109)
    elif i == 18:  # cliffs + sea
        s.sky([(0, "#1C3C7A"), (0.55, "#8DB2E0"), (1, "#F4E9D4")]); s.ridge(H * 0.45, 130, "#6A7BA0", seed=110, rough=1.1); s.waves(H * 0.7, ["#5FA8D3", "#3C86B8", "#256A9C", "#154E7E"], 18)
        s.ridge(H * 0.9, 60, "#2B3A55", seed=111, rough=1.4)
    else:        # fireflies at dusk
        s.sky([(0, "#0A1230"), (0.6, "#1C2F5E"), (1, "#2F5F6E")]); s.ridge(H * 0.7, 70, "#06101C", seed=112); s.tree(W * 0.2, H * 0.86, 420, "#04090F", seed=5); s.tree(W * 0.82, H * 0.88, 360, "#04090F", seed=6)
        for _ in range(46):
            s.add_glow(s.rng.random() * W, H * (0.4 + 0.5 * s.rng.random()), 26, "#E7FF7A", 0.9)
    return s


PACKS = {"christianity": christianity, "islam": islam, "buddhism": buddhism, "general": general}


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", required=True)
    ap.add_argument("--only", default=None, help="comma-separated pack slugs")
    ap.add_argument("--preview", default=None, help="write a contact sheet JPEG")
    a = ap.parse_args()
    only = a.only.split(",") if a.only else list(PACKS)
    sheet = Image.new("RGB", (20 * 144, len(only) * 256), (255, 255, 255)) if a.preview else None
    for row, slug in enumerate(only):
        for i in range(20):
            sc = PACKS[slug](i)
            sc.save(os.path.join(a.out, slug, f"{i + 1:02d}.webp"))
            if sheet is not None:
                sheet.paste(sc.img.resize((144, 256)), (i * 144, row * 256))
        print("generated", slug)
    if sheet is not None:
        sheet.save(a.preview, quality=88)


if __name__ == "__main__":
    main()
