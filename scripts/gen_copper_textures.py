"""Bronze Age phases 2-3 (copper) textures. Run: python scripts/gen_copper_textures.py

- Copper tool heads: the vanilla copper tools with the stick pixels taken out; the knife blade is the user's steel
  knife (workshop library, tools/steel_knife.png) recoloured onto the vanilla copper tool palette, and the Copper
  Knife is that blade on the knife's own handle.
- Rough Cast heads: the same shapes, dull and mottled like a fresh casting, with bits of casting flash on the edges.
- Clay Tuyere (raw / fired), unfired mold items and the mold block tops: the cavity is each casting's own silhouette.
- Mold metal overlays (molten: animated glow, solid: copper), kiln overheating cracks (3 stages), the kiln's ember
  glow (animated), bellows leather, and the thermometer drawn into the kiln GUI.
"""
import json
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item")
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def px(img):
    return [(x, y) for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3]]


def lum(c):
    return (c[0] * 3 + c[1] * 6 + c[2]) / 10


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def save(img, path, frames=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if frames:
        mc.write_text(json.dumps({"animation": {"frametime": frames}}) + "\n")
    elif mc.exists():
        mc.unlink()


STICK = {c[:3] for c in (vanilla("item/stick").getpixel(p) for p in px(vanilla("item/stick")))}
COPPER = [(66, 28, 17), (95, 44, 28), (214, 109, 72), (227, 130, 108), (252, 153, 130)]  # vanilla copper tool palette
ROUGH = [(46, 24, 16), (84, 46, 30), (124, 72, 48), (150, 92, 62), (176, 116, 82)]      # dull, oxidised casting skin
MOLTEN = [(120, 20, 4), (200, 60, 8), (246, 128, 20), (255, 196, 64), (255, 244, 190)]


def without_stick(img):
    out = img.copy()
    for p in px(img):
        if img.getpixel(p)[:3] in STICK:
            out.putpixel(p, (0, 0, 0, 0))
    return out


# ---- copper heads and knife ----
heads = {t: without_stick(vanilla(f"item/copper_{t}")) for t in ("pickaxe", "axe", "shovel", "hoe")}
knife_src = Image.open(LIB / "tools/steel_knife.png").convert("RGBA")
GREYS = {0x26: COPPER[0], 0x2E: COPPER[1], 0x36: COPPER[2], 0x53: COPPER[4]}
knife, blade = knife_src.copy(), Image.new("RGBA", (16, 16))
for p in px(knife_src):
    r, g, b, a = knife_src.getpixel(p)
    if r == g == b:  # the steel blade (the handle is brown)
        c = GREYS.get(r, ramp(COPPER, r / 0x53))
        knife.putpixel(p, c + (a,))
        blade.putpixel(p, c + (a,))
heads["knife"] = blade
for t, img in heads.items():
    save(img, ITEM / (f"copper_{t}_head.png" if t != "knife" else "copper_knife_blade.png"))
save(knife, ITEM / "copper_knife.png")


def rough(img, seed):
    rng = random.Random(seed)
    out = Image.new("RGBA", (16, 16))
    pts = px(img)
    lo, hi = min(lum(img.getpixel(p)) for p in pts), max(lum(img.getpixel(p)) for p in pts)
    for p in pts:
        t = (lum(img.getpixel(p)) - lo) / max(1, hi - lo) * 0.85 + rng.uniform(-0.18, 0.12)
        out.putpixel(p, ramp(ROUGH, t) + (255,))
    # casting flash: thin burrs where the mold halves met
    for x in range(16):
        for y in range(16):
            if img.getpixel((x, y))[3]:
                continue
            n = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3])
            if n and rng.random() < 0.22:
                out.putpixel((x, y), ROUGH[1] + (255,))
    return out


for i, (t, img) in enumerate(heads.items()):
    save(rough(img, 40 + i), ITEM / (f"rough_cast_copper_{t}_head.png" if t != "knife" else "rough_cast_copper_knife_blade.png"))

# ---- crucibles: drawn by gen_crucible_textures.py (empty, unfired, molten, solid) ----

# ---- clay palettes ----
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]          # vanilla clay ball greys
fired_tex = Image.open(BLOCK / "kiln_clay_fired.png").convert("RGBA")
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]


def tuyere(pal):
    """A short clay pipe lying diagonally: flared mouth at the upper right, narrowing nozzle at the lower left."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            along, across = x - y, x + y - 15  # along: -9 (nozzle) .. 9 (mouth)
            half = 2 if along >= 6 else 1 if along >= -6 else 0
            if not -9 <= along <= 9 or abs(across) > half + 1:
                continue
            if abs(across) == half + 1:
                c = pal[0]
            else:
                c = pal[3] if across < 0 else pal[2] if across == 0 else pal[1]
            img.putpixel((x, y), c + (255,))
    for x, y in ((12, 3), (11, 3), (12, 4)):  # the bore, seen at the mouth
        img.putpixel((x, y), (40, 28, 24, 255))
    return img


save(tuyere(RAW), ITEM / "unfired_clay_tuyere.png")
save(tuyere(FIRED), ITEM / "clay_tuyere.png")

# ---- molds: cavity masks are the castings themselves ----
masks = {"ingot": vanilla("item/copper_ingot"), "pickaxe_head": heads["pickaxe"], "axe_head": heads["axe"],
         "shovel_head": heads["shovel"], "hoe_head": heads["hoe"], "knife_blade": heads["knife"]}


def cavity(m, x, y):
    return 0 <= x < 16 and 0 <= y < 16 and m.getpixel((x, y))[3] > 0


def mold_top(m, base, pal, seed):
    """Clay slab top with the cavity sunk in: dark floor, shadow on the upper-left lip, light on the lower-right."""
    rng = random.Random(seed)
    img = base.copy() if base else Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if base is None:
                img.putpixel((x, y), ramp(pal, 0.55 + rng.uniform(-0.12, 0.12)) + (255,))
            if cavity(m, x, y):
                shade = 0.12 if not cavity(m, x - 1, y - 1) else 0.28
                img.putpixel((x, y), ramp(pal, shade + rng.uniform(0, 0.08)) + (255,))
            elif cavity(m, x - 1, y - 1) or cavity(m, x - 1, y) or cavity(m, x, y - 1):
                img.putpixel((x, y), ramp(pal, 0.95) + (255,))
    return img


def item_icon(top):
    """Unfired mold item: the slab seen from above with a darker front edge."""
    img = Image.new("RGBA", (16, 16))
    for x in range(1, 15):
        for y in range(2, 14):
            img.putpixel((x, y), top.getpixel((x, y)))
        for y in (14, 15):
            r, g, b, _ = top.getpixel((x, 13))
            img.putpixel((x, y), (int(r * 0.7), int(g * 0.7), int(b * 0.7), 255))
    return img


for i, (shape, m) in enumerate(masks.items()):
    save(mold_top(m, fired_tex, FIRED, 10 + i), BLOCK / f"clay_{shape}_mold_top.png")
    save(item_icon(mold_top(m, None, RAW, 20 + i)), ITEM / f"unfired_clay_{shape}_mold.png")
    # metal overlays: only the cavity, cut out
    solid_m = Image.new("RGBA", (16, 16))
    for p in px(m):
        t = 0.55 + (0.25 if not cavity(m, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
        solid_m.putpixel(p, ramp(COPPER[1:], t) + (255,))
    save(solid_m, BLOCK / f"clay_{shape}_mold_copper_solid.png")
    molten = Image.new("RGBA", (16, 64))
    for f in range(4):
        rng = random.Random(f * 7 + i)
        for x, y in px(m):
            molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
    save(molten, BLOCK / f"clay_{shape}_mold_copper_molten.png", frames=8)

# ---- kiln: overheating cracks (1, 2 strained; 3 cracked through, with flaked-off patches) ----
for stage in (1, 2, 3):
    rng = random.Random(stage * 13)
    img = fired_tex.copy()
    for _ in range(stage * 2 + (2 if stage == 3 else 0)):
        x, y = rng.randrange(16), rng.randrange(16)
        for _ in range(rng.randint(5, 9 + stage * 2)):
            r, g, b, a = img.getpixel((x % 16, y % 16))
            img.putpixel((x % 16, y % 16), (int(r * 0.35), int(g * 0.3), int(b * 0.3), a))
            x += rng.choice((-1, 0, 1))
            y += rng.choice((1, 1, 0, -1))
    if stage == 3:
        for _ in range(3):  # flakes: lighter raw-looking clay where the skin came off
            x0, y0 = rng.randrange(14), rng.randrange(14)
            for dx in range(2):
                for dy in range(2):
                    r, g, b, a = img.getpixel((x0 + dx, y0 + dy))
                    img.putpixel((x0 + dx, y0 + dy), (min(255, r + 40), min(255, g + 30), min(255, b + 25), a))
    save(img, BLOCK / f"kiln_clay_cracks_{stage}.png")

# ---- kiln ember glow (animated) ----
glow = Image.new("RGBA", (16, 64))
for f in range(4):
    rng = random.Random(500 + f)
    for x in range(16):
        for y in range(16):
            glow.putpixel((x, y + f * 16), ramp(MOLTEN, 0.5 + rng.uniform(-0.25, 0.45)) + (255,))
save(glow, BLOCK / "kiln_glow.png", frames=5)

# ---- bellows leather: brown hide with darker folds across ----
rng = random.Random(9)
leather = Image.new("RGBA", (16, 16))
LEATHER = [(58, 34, 20), (86, 52, 30), (112, 70, 42), (134, 88, 56)]
for x in range(16):
    for y in range(16):
        t = 0.6 + rng.uniform(-0.12, 0.12) - (0.35 if y % 4 == 3 else 0) + (0.15 if y % 4 == 0 else 0)
        leather.putpixel((x, y), ramp(LEATHER, t) + (255,))
save(leather, BLOCK / "bellows_leather.png")

# ---- kiln GUI: thermometer tube (inside x 22..27, y 18..62 = 0..1300 deg), bulb, marks at 600 / 950 / 1085 ----
gui_path = TEX / "gui/container/kiln.png"
gui = Image.open(gui_path).convert("RGBA")
BG, DARK, LIGHT, INNER = (198, 198, 198, 255), (55, 55, 55, 255), (255, 255, 255, 255), (30, 22, 20, 255)
for x in range(14, 42):
    for y in range(15, 72):
        gui.putpixel((x, y), BG)
for y in range(17, 63):  # glass frame: dark top/left, light bottom/right, like slot borders
    gui.putpixel((21, y), DARK)
    gui.putpixel((28, y), LIGHT)
for x in range(21, 29):
    gui.putpixel((x, 17), DARK)
for x in range(22, 28):
    for y in range(18, 62):
        gui.putpixel((x, y), INNER)
for x in range(20, 30):  # bulb
    for y in range(62, 69):
        dx, dy = x - 24.5, y - 65.5
        d = (dx * dx) / 25 + (dy * dy) / 12.25
        if d <= 1.0:
            gui.putpixel((x, y), INNER if d < 0.62 else DARK if x < 25 else LIGHT)
for deg, col in ((600, (176, 32, 12, 255)), (950, (240, 122, 16, 255)), (1085, (255, 210, 80, 255))):
    y = 62 - round(deg / 1300 * 44)
    for x in (29, 30, 31):
        gui.putpixel((x, y), col)
    gui.putpixel((29, y + 1), DARK)
gui.save(gui_path)
print("copper textures ok")
