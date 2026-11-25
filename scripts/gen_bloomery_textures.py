"""Bloomery textures (Ironworking.java). Run: python scripts/gen_bloomery_textures.py

- Fire Bricks (the bloomery's walls): pale cream-grey firebrick in offset courses with a dark clay daub, each brick its
  own tone, black grog specks; the stack's walls darken with soot toward the top. The hearth's front is the wall with a
  low fired-clay slag arch (dark, slag drips at the sill); while lit the animated glow fills the arch. The back has a
  fired-clay tuyere ring round a dark hole. The stack's top is the rim of the shaft; inside soot-black walls and the
  charge (charcoal lumps with rusty ore), glowing and animated while lit.
- Items: Grog (crushed brick: the Crushed Copper Ore heap in brick red), Fire Clay (vanilla's clay ball in pale fireclay
  with grog flecks), Fire Brick (vanilla's brick shape, pale cream with dark specks; unlike the buff Furnace Brick),
  Iron Bloom (the user's library iron_bloom: cold dark spongy iron with rust and slag pits; hot: animated glowing orange
  to yellow-white), Wrought Iron Ingot (the library steel_ingot in dull grey iron), Slag (glassy black-green lumps
  with bubbles), Bronze Smithing Hammer (the library basic_smithing_hammer with its head in bronze) and Bronze Hammer
  Head (just the head), rough casts and molds as gen_bronzetools_textures.py (helpers copied).
- Bronze Anvil: cast bronze, the face polished brighter.
- GUI: bloomery.png, the kiln's screen with its thermometer re-marked for a 0..1500 deg tube at 1100 (forging) and
  1350 (smelting iron).
"""
import json
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR_NAME = "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
JAR = ROOT / JAR_NAME
if not JAR.exists():  # a git worktree without its own build: use the main checkout's
    JAR = Path(r"C:\Users\Yngve\Documents\project_minecraft_mod") / JAR_NAME
LIB = Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item")
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
jar = zipfile.ZipFile(JAR)

BRONZE = [(52, 32, 12), (92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]
ROUGH = [(40, 30, 16), (74, 56, 30), (108, 84, 46), (134, 106, 60), (158, 128, 78)]
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]
FIREBRICK = [(96, 90, 80), (150, 142, 126), (196, 188, 166), (222, 214, 192), (240, 234, 216)]  # pale cream-grey
DAUB = [(70, 44, 32), (100, 66, 48), (124, 86, 64)]
BRICK = [(70, 26, 16), (122, 50, 32), (164, 78, 52), (196, 112, 80)]
IRON = [(30, 30, 34), (58, 58, 64), (90, 90, 96), (124, 124, 130), (160, 160, 166), (196, 196, 200)]
RUST = (118, 58, 30)
SLAG = [(14, 16, 14), (30, 36, 32), (50, 62, 54), (78, 96, 84), (120, 140, 126)]
SOOT = (30, 24, 20)


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


def mix(a, b, f):
    return tuple(int(a[c] * (1 - f) + b[c] * f) for c in range(3))


def save(img, path, frames=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if frames:
        mc.write_text(json.dumps({"animation": {"frametime": frames}}) + "\n")
    elif mc.exists():
        mc.unlink()


def recolor(img, pal, mask=None):
    out = img.copy()
    pts = mask if mask is not None else px(img)
    ls = [lum(img.getpixel(p)) for p in pts]
    lo, hi = min(ls), max(ls)
    for p in pts:
        out.putpixel(p, ramp(pal, (lum(img.getpixel(p)) - lo) / max(1, hi - lo)) + (img.getpixel(p)[3],))
    return out


def rough(img, seed):
    rng = random.Random(seed)
    out = Image.new("RGBA", (16, 16))
    pts = px(img)
    lo, hi = min(lum(img.getpixel(p)) for p in pts), max(lum(img.getpixel(p)) for p in pts)
    for p in pts:
        t = (lum(img.getpixel(p)) - lo) / max(1, hi - lo) * 0.85 + rng.uniform(-0.18, 0.12)
        out.putpixel(p, ramp(ROUGH, t) + (255,))
    for x in range(16):  # casting flash where the mold halves met
        for y in range(16):
            if img.getpixel((x, y))[3]:
                continue
            n = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3])
            if n and rng.random() < 0.22:
                out.putpixel((x, y), ROUGH[1] + (255,))
    return out


def outline(img, color=BRONZE[0]):
    out = img.copy()
    for x in range(16):
        for y in range(16):
            if not img.getpixel((x, y))[3] and any(0 <= x - dx < 16 and 0 <= y - dy < 16 and img.getpixel((x - dx, y - dy))[3]
                                                   for dx, dy in ((1, 0), (0, 1))):
                out.putpixel((x, y), color + (255,))
    return out


# ================================================================ items
items = {}

# Grog: the crushed ore heap in brick red
items["grog"] = recolor(Image.open(ITEM / "crushed_copper_ore.png").convert("RGBA"), BRICK)

# Fire Clay: the clay ball, pale fireclay with grog flecks
rng = random.Random(501)
fc = recolor(vanilla("item/clay_ball"), [(120, 112, 98), (176, 168, 148), (214, 206, 186), (236, 230, 212)])
for p in px(fc):
    if rng.random() < 0.12 and fc.getpixel(p)[:3] != (120, 112, 98):
        fc.putpixel(p, ramp(BRICK, rng.uniform(0.4, 0.9)) + (255,))
items["fire_clay"] = fc

# Fire Brick: vanilla's brick shape, pale cream, dark specks
rng = random.Random(502)
fb = recolor(vanilla("item/brick"), FIREBRICK)
for p in px(fb):
    if rng.random() < 0.08 and lum(fb.getpixel(p)) > 150:
        fb.putpixel(p, mix(fb.getpixel(p)[:3], (40, 34, 30), 0.6) + (255,))
items["fire_brick"] = fb

# Iron Bloom (cold): the library bloom in dark spongy iron, rust and slag pits
bloom_src = Image.open(LIB / "iron_bloom.png").convert("RGBA")
rng = random.Random(503)
cold = recolor(bloom_src, IRON[:5])
for p in px(cold):
    r = rng.random()
    if r < 0.12:
        cold.putpixel(p, mix(cold.getpixel(p)[:3], RUST, 0.55) + (255,))
    elif r < 0.2:
        cold.putpixel(p, SLAG[1] + (255,))
items["iron_bloom"] = cold

# Hot Iron Bloom: the same sponge glowing, 4 flickering frames (the pits stay darker red)
hot = Image.new("RGBA", (16, 64))
pits = {p for p in px(cold) if cold.getpixel(p)[:3] == SLAG[1]}
for f in range(4):
    frng = random.Random(510 + f)
    base = recolor(bloom_src, MOLTEN[1:])
    for x, y in px(base):
        c = base.getpixel((x, y))[:3]
        c = mix(c, MOLTEN[0], 0.55) if (x, y) in pits else mix(c, MOLTEN[4], frng.uniform(0, 0.25))
        hot.putpixel((x, y + 16 * f), c + (255,))
save(hot, ITEM / "hot_iron_bloom.png", frames=6)

# Wrought Iron Ingot (and Block): scripts/gen_metal_textures.py, from the metal table

# Slag: glassy black-green lumps with bubbles and a glint
rng = random.Random(505)
slag = Image.new("RGBA", (16, 16))
for cx, cy, r in ((6, 9, 4.2), (10.5, 7, 3.4), (8, 12, 3.0), (4, 5.5, 2.2)):
    for x in range(16):
        for y in range(16):
            d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
            if d <= r:
                t = 0.55 - (x - cx + y - cy) / (r * 3) + rng.uniform(-0.08, 0.08)
                slag.putpixel((x, y), ramp(SLAG, t) + (255,))
for p in px(slag):
    if rng.random() < 0.07:
        slag.putpixel(p, SLAG[0] + (255,))  # bubbles
for x, y in ((5, 7), (9, 5), (7, 11)):
    slag.putpixel((x, y), SLAG[4] + (255,))
items["slag"] = outline(slag, (10, 10, 10))

# Bronze Smithing Hammer: the library hammer, head in bronze (handle and leather wrap kept)
src = Image.open(LIB / "basic_smithing_hammer.png").convert("RGBA")
HANDLE = {(0x49, 0x36, 0x15), (0x89, 0x67, 0x27), (0x68, 0x4e, 0x1e), (0x28, 0x1e, 0x0b),
          (0x9b, 0x84, 0x74), (0xac, 0x91, 0x7f), (0xb7, 0x9c, 0x89), (0x83, 0x6f, 0x61), (0xa5, 0x8c, 0x7c)}
OUTLINE = {(0x0e, 0x0e, 0x0e), (0x22, 0x20, 0x20)}
head = [p for p in px(src) if src.getpixel(p)[:3] not in HANDLE | OUTLINE]
hammer = recolor(src, BRONZE[1:], head)
for p in px(src):
    if src.getpixel(p)[:3] in OUTLINE:
        hammer.putpixel(p, BRONZE[0] + (255,))
items["bronze_smithing_hammer"] = hammer
head_img = Image.new("RGBA", (16, 16))
for p in head:
    head_img.putpixel(p, hammer.getpixel(p))
head_only = outline(head_img)
items["bronze_hammer_head"] = head_only

# the anvil's silhouette (side view, horn to the right) for the rough cast and the mold
ANVIL_ROWS = [
    "                ",
    "                ",
    "                ",
    "  ##########    ",
    "  #############  ",
    "  ############# ",
    "   ########     ",
    "     ####       ",
    "     ####       ",
    "     ####       ",
    "    ######      ",
    "   ########     ",
    "  ##########    ",
    "  ##########    ",
    "                ",
    "                ",
]
anvil_shape = Image.new("RGBA", (16, 16))
for y, row in enumerate(ANVIL_ROWS):
    for x, ch in enumerate(row[:16]):
        if ch == "#":
            top = y == 0 or ANVIL_ROWS[y - 1][x:x + 1] != "#"
            anvil_shape.putpixel((x, y), ramp(BRONZE[1:], 0.9 if top else 0.55 - x * 0.015) + (255,))
rough_src = {"hammer_head": head_only, "anvil": outline(anvil_shape)}

for name, img in items.items():
    save(img, ITEM / f"{name}.png")
SHAPES = ["hammer_head", "anvil"]
for i, s in enumerate(SHAPES):
    save(rough(rough_src[s], 600 + i), ITEM / f"rough_cast_bronze_{s}.png")

# ---- molds (as gen_bronzetools_textures.py) ----
fired_tex = Image.open(BLOCK / "kiln_clay_fired.png").convert("RGBA")


def cavity(m, x, y):
    return 0 <= x < 16 and 0 <= y < 16 and m.getpixel((x, y))[3] > 0


def mold_top(m, base, pal, seed):
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
    img = Image.new("RGBA", (16, 16))
    for x in range(1, 15):
        for y in range(2, 14):
            img.putpixel((x, y), top.getpixel((x, y)))
        for y in (14, 15):
            r, g, b, _ = top.getpixel((x, 13))
            img.putpixel((x, y), (int(r * 0.7), int(g * 0.7), int(b * 0.7), 255))
    return img


def opaque_only(img):
    out = Image.new("RGBA", img.size)
    for p in px(img):
        if img.getpixel(p)[:3] != BRONZE[0]:
            out.putpixel(p, img.getpixel(p))
    return out


for i, s in enumerate(SHAPES):
    m = opaque_only(rough_src[s])
    save(mold_top(m, fired_tex, FIRED, 700 + i), BLOCK / f"clay_{s}_mold_top.png")
    save(item_icon(mold_top(m, None, RAW, 720 + i)), ITEM / f"unfired_clay_{s}_mold.png")
    solid = Image.new("RGBA", (16, 16))
    for p in px(m):
        t = 0.5 + (0.25 if not cavity(m, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
        solid.putpixel(p, ramp(BRONZE[1:], t) + (255,))
    save(solid, BLOCK / f"clay_{s}_mold_bronze_solid.png")
    molten = Image.new("RGBA", (16, 64))
    for f in range(4):
        rng = random.Random(f * 11 + i + 740)
        for x, y in px(m):
            molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
    save(molten, BLOCK / f"clay_{s}_mold_bronze_molten.png", frames=8)

# ================================================================ blocks


def bricks(seed, soot_top=0.0):
    """Courses 4 px (3 brick + 1 daub), bricks 8 wide, offset every other course; soot_top darkens toward the top."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    tones = {}
    for y in range(16):
        course, row = y // 4, y % 4
        off = 4 if course % 2 else 0
        for x in range(16):
            col = (x + off) % 8
            if row == 3 or col == 7:
                c = ramp(DAUB, 0.5 + rng.uniform(-0.2, 0.2))
            else:
                tone = tones.setdefault((course, (x + off) // 8), rng.uniform(-0.12, 0.1))
                t = 0.62 + tone + rng.uniform(-0.06, 0.06) + (0.12 if row == 0 else -0.12 if row == 2 else 0) + (0.05 if col == 0 else -0.07 if col == 6 else 0)
                c = ramp(FIREBRICK, t)
                if rng.random() < 0.07:
                    c = mix(c, (36, 30, 26), 0.65)  # grog specks
            if soot_top:
                c = mix(c, SOOT, max(0.0, soot_top * (1 - y / 10)) * rng.uniform(0.7, 1.0))
            img.putpixel((x, y), c + (255,))
    return img


wall = bricks(801)
save(wall, BLOCK / "bloomery_bricks.png")
save(bricks(802, soot_top=0.75), BLOCK / "bloomery_bricks_sooty.png")

# hearth front: a low arch (x 4..11, rows 9..15) in fired clay, dark inside with slag at the sill
rng = random.Random(803)
front = wall.copy()
ARCH = {(x, y) for y in range(9, 16) for x in range(16)
        if (y == 9 and 6 <= x <= 9) or (y == 10 and 5 <= x <= 10) or (y >= 11 and 4 <= x <= 11)}
for y in range(7, 16):
    for x in range(2, 14):
        if (x, y) not in ARCH and any((x + dx, y + dy) in ARCH for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            t = 0.7 + rng.uniform(-0.06, 0.06) + (0.12 if x < 7 and y < 11 else -0.1 if x > 9 else 0)
            front.putpixel((x, y), ramp(FIRED, t) + (255,))
for (x, y) in ARCH:
    c = mix(SOOT, (10, 8, 6), rng.uniform(0.3, 0.7))
    if y == 15 and rng.random() < 0.7:
        c = ramp(SLAG, rng.uniform(0.2, 0.6))
    front.putpixel((x, y), c + (255,))
for x in range(4, 12):  # soot licked up above the arch
    for y in range(4, 8):
        if rng.random() < 0.35 * (1 - (8 - y) / 5):
            front.putpixel((x, y), mix(front.getpixel((x, y))[:3], SOOT, 0.5) + (255,))
save(front, BLOCK / "bloomery_hearth_front.png")
# lit: the fire in the arch (the model's glow plane over it), 4 frames
glow = Image.new("RGBA", (16, 64))
for f in range(4):
    frng = random.Random(810 + f)
    for (x, y) in ARCH:
        t = 0.35 + (y - 9) / 10 + frng.uniform(-0.2, 0.25)
        glow.putpixel((x, y + 16 * f), ramp(MOLTEN, t) + (255,))
save(glow, BLOCK / "bloomery_hearth_glow.png", frames=4)

# back: the clay tuyere ring round a dark hole (rows 9..13), soot above
tuy = wall.copy()
for x in range(16):
    for y in range(16):
        d = ((x - 7.5) ** 2 + (y - 11.5) ** 2) ** 0.5
        if d <= 1.6:
            tuy.putpixel((x, y), mix(SOOT, (8, 6, 4), 0.5) + (255,))
        elif d <= 3.2:
            tuy.putpixel((x, y), ramp(FIRED, 0.75 - (x + y - 19) * 0.06 + rng.uniform(-0.05, 0.05)) + (255,))
        elif 5 <= x <= 10 and 3 <= y <= 8 and rng.random() < 0.3:
            tuy.putpixel((x, y), mix(tuy.getpixel((x, y))[:3], SOOT, 0.45) + (255,))
save(tuy, BLOCK / "bloomery_tuyere.png")
tuy_glow = Image.new("RGBA", (16, 64))
for f in range(4):
    frng = random.Random(820 + f)
    for x in range(16):
        for y in range(16):
            if ((x - 7.5) ** 2 + (y - 11.5) ** 2) ** 0.5 <= 1.6:
                tuy_glow.putpixel((x, y + 16 * f), ramp(MOLTEN, 0.6 + frng.uniform(-0.2, 0.3)) + (255,))
save(tuy_glow, BLOCK / "bloomery_tuyere_glow.png", frames=5)

# stack top rim (the model's walls are 4 px thick round an 8 px mouth), sooted toward the opening
rim = bricks(830)
for x in range(16):
    for y in range(16):
        inner = 4 <= x <= 11 and 4 <= y <= 11
        edge = min(x, y, 15 - x, 15 - y)
        if not inner:
            rim.putpixel((x, y), mix(rim.getpixel((x, y))[:3], SOOT, max(0, (edge - 1) / 3) * 0.7) + (255,))
save(rim, BLOCK / "bloomery_stack_top.png")
# inside of the shaft: soot-black firebrick, glassy drips
inside = Image.new("RGBA", (16, 16))
rng = random.Random(831)
for x in range(16):
    for y in range(16):
        c = mix(SOOT, (60, 50, 42), rng.uniform(0, 0.35) + (0.15 if y % 4 == 0 else 0))
        if rng.random() < 0.05:
            c = ramp(SLAG, rng.uniform(0.3, 0.8))
        inside.putpixel((x, y), c + (255,))
save(inside, BLOCK / "bloomery_inside.png")
# the charge: charcoal lumps and rusty ore; lit: glowing coals (animated)
charge = Image.new("RGBA", (16, 16))
rng = random.Random(832)
for x in range(16):
    for y in range(16):
        r = rng.random()
        c = ramp([(14, 12, 12), (40, 36, 34), (70, 64, 60)], rng.uniform(0, 1)) if r < 0.75 else mix(RUST, (60, 40, 30), rng.uniform(0, 0.6))
        charge.putpixel((x, y), c + (255,))
save(charge, BLOCK / "bloomery_charge.png")
lit = Image.new("RGBA", (16, 64))
for f in range(4):
    frng = random.Random(840 + f)
    for x in range(16):
        for y in range(16):
            dark = charge.getpixel((x, y))[:3]
            t = frng.uniform(0.2, 1.0)
            lit.putpixel((x, y + 16 * f), (mix(ramp(MOLTEN, t), dark, 0.25) if frng.random() < 0.8 else dark) + (255,))
save(lit, BLOCK / "bloomery_charge_lit.png", frames=5)

# Bronze Anvil: cast bronze, the face polished
rng = random.Random(850)
cast = Image.new("RGBA", (16, 16))
face = Image.new("RGBA", (16, 16))
for x in range(16):
    for y in range(16):
        cast.putpixel((x, y), ramp(BRONZE, 0.36 + rng.uniform(-0.14, 0.14) - (0.14 if (x * 7 + y * 3) % 11 == 0 else 0) + (0.1 if y == 0 else 0)) + (255,))
        face.putpixel((x, y), ramp(BRONZE, 0.72 + rng.uniform(-0.06, 0.06) + (0.1 if (x + y) % 7 == 0 else 0)) + (255,))
save(cast, BLOCK / "bronze_anvil.png")
save(face, BLOCK / "bronze_anvil_top.png")

# ================================================================ GUI: the kiln screen re-marked for 0..1500 deg
gui = Image.open(TEX / "gui/container/kiln.png").convert("RGBA")
BG, DARK = (198, 198, 198, 255), (55, 55, 55, 255)
for x in (29, 30, 31):
    for y in range(15, 72):
        gui.putpixel((x, y), BG)
for deg, col in ((1100, (255, 190, 60, 255)), (1350, (255, 250, 220, 255))):
    y = 62 - round(deg / 1500 * 44)
    for x in (29, 30, 31):
        gui.putpixel((x, y), col)
    gui.putpixel((29, y + 1), DARK)
save(gui, TEX / "gui/container/bloomery.png")
print(f"bloomery textures ok ({len(items) + 1} items)")
