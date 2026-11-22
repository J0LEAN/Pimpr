#!/usr/bin/env python3
"""Prépare les thèmes à images de Pimpr.

Entrée : un dossier par thème (hero.*, w1.* à w4.*, manifest.json) et tools/themes_spec.py.
Sortie : les images recadrées en WebP dans app/src/main/res/raw, le catalogue Kotlin
(ImageThemeData.kt) et la liste des crédits (docs/CREDITS-IMAGES.md).

    python3 -I tools/build_themes.py <dossier des sources>
"""
import colorsys, glob, json, os, sys
import numpy as np
from PIL import Image, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from themes_spec import THEMES  # noqa: E402

SRC = sys.argv[1]
RAW = os.path.join(ROOT, "app/src/main/res/raw")
KT = os.path.join(ROOT, "app/src/main/java/com/pimpr/app/data/catalog/ImageThemeData.kt")
CREDITS = os.path.join(ROOT, "docs/CREDITS-IMAGES.md")
LOCK = (1080, 2340)
Image.MAX_IMAGE_PIXELS = 400_000_000


def find(d, stem):
    for ext in ("jpg", "jpeg", "png", "webp"):
        p = os.path.join(d, f"{stem}.{ext}")
        if os.path.exists(p):
            return p
    return None


def cover(im, size, focus=(0.5, 0.5), pixel=False):
    w, h = size
    s = max(w / im.width, h / im.height)
    if pixel:
        s = max(1, int(np.ceil(s)))
        im = im.resize((im.width * s, im.height * s), Image.NEAREST)
    else:
        im = im.resize((max(w, round(im.width * s)), max(h, round(im.height * s))), Image.LANCZOS)
    x = round((im.width - w) * focus[0])
    y = round((im.height - h) * focus[1])
    return im.crop((x, y, x + w, y + h))


def hls(rgb):
    return colorsys.rgb_to_hls(*(c / 255 for c in rgb))


def from_hls(h, l, s):
    return tuple(round(c * 255) for c in colorsys.hls_to_rgb(h % 1, min(max(l, 0), 1), min(max(s, 0), 1)))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def lum(rgb):
    return (0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2]) / 255


def hexi(rgb):
    return "0x%02X%02X%02X" % tuple(rgb)


def parse(v):
    return ((v >> 16) & 255, (v >> 8) & 255, v & 255) if isinstance(v, int) else tuple(v)


def clusters(im, n=7):
    q = im.convert("RGB").resize((72, 156), Image.LANCZOS).quantize(n, kmeans=4)
    pal = q.getpalette()[: n * 3]
    counts = sorted(q.getcolors(), reverse=True)
    total = sum(c for c, _ in counts)
    return [(tuple(pal[i * 3: i * 3 + 3]), c / total) for c, i in counts]


def palette(lock, spec):
    cl = clusters(lock)
    mean_l = sum(lum(c) * w for c, w in cl)
    dark = spec.get("dark", mean_l < 0.42)

    def score(c, w):
        h, l, s = hls(c)
        return 0 if l < 0.1 or l > 0.9 else w * (0.25 + s)

    main = parse(spec["main"]) if "main" in spec else max(cl, key=lambda cw: score(*cw))[0]
    mh, ml, ms = hls(main)
    ms = min(ms, 0.62)

    def far(c):
        d = abs(hls(c)[0] - mh)
        return min(d, 1 - d) > 0.1

    others = [(c, w) for c, w in cl if far(c) and 0.15 < hls(c)[1] < 0.9 and hls(c)[2] > 0.2]
    if others:
        a = max(others, key=lambda cw: cw[1] * (0.2 + hls(cw[0])[2]))[0]
        ah, _, a_s = hls(a)
        accent = from_hls(ah, 0.62 if dark else 0.5, min(max(a_s, 0.45), 0.75))
    else:
        accent = from_hls(mh, 0.78 if dark else 0.3, ms)

    if dark:
        card = from_hls(mh, min(max(ml, 0.2), 0.3), min(ms, 0.5))
        on = from_hls(mh, 0.92, min(ms, 0.4))
        card2 = from_hls(mh, 0.86, min(ms, 0.32))
        on2 = from_hls(mh, 0.16, min(ms, 0.5))
        tiles = [mix(card, (255, 255, 255), 0.1), card, mix(card, (255, 255, 255), 0.2)]
        base = from_hls(mh, 0.11, min(ms, 0.4))
    else:
        card = from_hls(mh, min(max(ml, 0.4), 0.52), ms)
        on = from_hls(mh, 0.96, min(ms, 0.5))
        card2 = from_hls(mh, 0.94, min(ms, 0.4))
        on2 = from_hls(mh, 0.24, min(ms + 0.05, 0.6))
        tiles = [card, mix(card, (255, 255, 255), 0.28), mix(card, (0, 0, 0), 0.18), card2]
        base = from_hls(mh, 0.9, min(ms, 0.3))
    p = dict(dark=dark, card=card, onCard=on, card2=card2, onCard2=on2, accent=accent, tiles=tiles, base=base)
    for k in ("card", "onCard", "card2", "onCard2", "accent", "base"):
        if k in spec:
            p[k] = parse(spec[k])
    if "tiles" in spec:
        p["tiles"] = [parse(t) for t in spec["tiles"]]
    return p


def grain(im, amount, seed):
    a = np.asarray(im).astype(np.int16)
    rng = np.random.default_rng(seed)
    a += rng.normal(0, amount, a.shape[:2] + (1,)).astype(np.int16)
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))


def make_home(lock, mode, base, seed, pixel):
    if mode == "same":
        return lock, LOCK
    if mode == "dim":      # la même image, assombrie et à peine floutée
        im = lock if pixel else lock.filter(ImageFilter.GaussianBlur(5))
        return Image.blend(im, Image.new("RGB", im.size, base), 0.45), LOCK
    if mode == "veil":     # la même image sous un voile de la couleur de base
        im = lock if pixel else lock.filter(ImageFilter.GaussianBlur(3))
        return Image.blend(im, Image.new("RGB", im.size, base), 0.6), LOCK
    small = lock.resize((270, 585), Image.LANCZOS).filter(ImageFilter.GaussianBlur(26))
    im = Image.blend(small, Image.new("RGB", small.size, base), 0.5).resize((720, 1560), Image.BICUBIC)
    return grain(im, 3.5, seed), (720, 1560)


def kstr(s):
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def main():
    os.makedirs(RAW, exist_ok=True)
    for old in glob.glob(os.path.join(RAW, "th_*")):
        os.remove(old)
    rows, credits, total = [], [], 0
    for n, spec in enumerate(THEMES):
        d = os.path.join(SRC, spec.get("dir", spec["id"]))
        hero = find(d, spec.get("hero", "hero"))
        if not hero or not os.path.exists(os.path.join(d, "manifest.json")):
            print("!! incomplet :", spec["id"]); continue
        pixel = spec.get("pixel", False)
        rid = "th_" + spec["id"].replace("-", "_")
        lock = cover(Image.open(hero).convert("RGB"), LOCK, spec.get("focus", (0.5, 0.5)), pixel)
        pal = palette(lock, spec)
        home, hsize = make_home(lock, spec.get("home", "soft"), pal["base"], n, pixel)
        if "homeImage" in spec:
            home = cover(Image.open(find(d, spec["homeImage"])).convert("RGB"), LOCK, spec.get("homeFocus", (0.5, 0.5)), pixel)
            if spec.get("home") == "veil":
                home = Image.blend(home, Image.new("RGB", home.size, pal["base"]), 0.55)

        def save(im, name, q=80):
            nonlocal total
            p = os.path.join(RAW, f"{rid}_{name}.webp")
            if pixel:
                im.save(p, "WEBP", lossless=True, method=6)
            else:
                im.save(p, "WEBP", quality=q, method=6)
            total += os.path.getsize(p)

        save(lock, "lock", 80)
        save(home, "home", 72)
        order = spec.get("widgets", ["w1", "w2", "w3", "w4"])
        for i, stem in enumerate(order, 1):
            p = find(d, stem) or hero
            im = Image.open(p).convert("RGB")
            wide = i in (2, 4)
            focus = spec.get("wfocus", {}).get(stem, (0.5, 0.5))
            save(cover(im, (960, 600) if wide else (640, 640), focus, pixel), f"w{i}", 80)

        top = lock.crop((160, 200, 920, 640)).resize((8, 5), Image.BOX)
        lock_light = spec.get("lockLight", float(np.mean([lum(px) for px in top.get_flattened_data()])) < 0.62)
        home_l = float(np.mean([lum(px) for px in home.resize((8, 16), Image.BOX).get_flattened_data()]))
        dark_home = spec.get("homeDark", home_l < 0.5)

        man = []
        mp = os.path.join(d, "manifest.json")
        if os.path.exists(mp):
            man = json.load(open(mp))
        used = {os.path.splitext(os.path.basename(hero))[0]} | set(order)
        man = [m for m in man if os.path.splitext(m.get("file", ""))[0] in used]
        authors = []
        for m in man:
            a = (m.get("author") or m.get("holder") or "").strip()
            if a and a not in authors:
                authors.append(a)
        credit = spec.get("credit") or ", ".join(authors[:3])
        credits.append((spec["name"], man))

        opt = ""
        for k in ("caption", "ornament"):
            if spec.get(k):
                opt += f", {k} = {kstr(spec[k])}"
        if pixel:
            opt += ", pixel = true"
        if spec.get("featured"):
            opt += ", featured = true"
        if spec.get("tile"):
            opt += f", tile = TileShape.{spec['tile']}"
        if spec.get("shape"):
            opt += f", shape = ShapeKind.{spec['shape']}"
        if "corner" in spec:
            opt += f", corner = {spec['corner']}f"
        rows.append(
            f"    ImageThemeRow(\n"
            f"        {kstr(spec['id'])}, {kstr(spec['name'])}, {kstr(spec['tagline'])}, ThemeCategory.{spec['cat']},\n"
            f"        {kstr(spec['tags'])},\n"
            f"        dark = {str(dark_home).lower()}, lockLight = {str(bool(lock_light)).lower()},\n"
            f"        card = {hexi(pal['card'])}, onCard = {hexi(pal['onCard'])}, card2 = {hexi(pal['card2'])}, onCard2 = {hexi(pal['onCard2'])}, accent = {hexi(pal['accent'])},\n"
            f"        tiles = listOf({', '.join(hexi(t) for t in pal['tiles'])}),\n"
            f"        font = {kstr(spec['font'])}, quoteFont = {kstr(spec.get('quoteFont', 'playfair'))}, recipe = {spec.get('recipe', n)},\n"
            f"        quote = {kstr(spec['quote'])},\n"
            f"        credit = {kstr(credit)}{opt},\n"
            f"    ),\n"
        )
        print(f"{spec['id']:18s} dark={dark_home!s:5} lockLight={bool(lock_light)!s:5} card={hexi(pal['card'])} accent={hexi(pal['accent'])}")

    with open(KT, "w") as f:
        f.write("package com.pimpr.app.data.catalog\n\nimport com.pimpr.app.model.ShapeKind\nimport com.pimpr.app.model.ThemeCategory\nimport com.pimpr.app.model.TileShape\n\n")
        f.write("// Fichier généré par tools/build_themes.py à partir de tools/themes_spec.py : ne pas modifier à la main.\n\n")
        f.write("@Suppress(\"unused\")\nprivate val keepImports = listOf<Any>(ShapeKind.ROUNDED, TileShape.SQUIRCLE)\n\n")
        f.write("internal val ImageThemeRows: List<ImageThemeRow> = listOf(\n" + "".join(rows) + ")\n")
    with open(CREDITS, "w") as f:
        f.write("# Images des thèmes\n\nToutes les images embarquées sont dans le domaine public ou sous licence CC0. "
                "Elles ont été recadrées, et parfois floutées ou teintées pour servir de fond d'accueil.\n")
        for name, man in credits:
            f.write(f"\n## {name}\n\n")
            for m in man:
                bits = [m.get("title", "").strip(), m.get("author", "").strip(), str(m.get("year", "")).strip(), m.get("holder", "").strip(), m.get("license", "").strip()]
                f.write("- " + ", ".join(b for b in bits if b) + (f" — <{m['source']}>" if m.get("source") else "") + "\n")
    print(f"{len(rows)} thèmes, {total / 1e6:.1f} Mo d'images")


main()
