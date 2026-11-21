"""Bronze Age phase 3b (bronze) textures. Run: python scripts/gen_bronze_textures.py (after gen_copper_textures.py)

Bronze is golden brown, kept apart from copper's salmon pink: one palette for every bronze piece.
- Bronze Ingot and Bronze Plate: the user's library shapes (bronze_ingot.png, bronze_plate.png) recoloured to bronze.
- Heads: vanilla iron tool shapes (not copper's, so the tiers look different) without the stick; the knife blade is the
  user's steel knife (tools/steel_knife.png) like the copper one. Tools: the iron tools with the head recoloured.
- Spear Head / Bronze Spear and Sword Blade / Bronze Sword: vanilla iron spear (icon and in-hand) and iron sword with
  every grey (iron) pixel in bronze; the sword's grip is rewrapped in leather. The blade piece keeps the cross-guard.
- Copper / Tin Concentrate: the library's dust piles washed clean (no grey grit), a little darker and wet-looking, with
  bright metal glints; tin concentrate is the black "tin sand" cassiterite washes down to.
- Saw Blade / Bronze Saw: drawn here, a toothed blade along the diagonal, the saw on a wooden D-handle.
- Rough Cast pieces: the same shapes, dull and mottled with casting flash (as copper's).
- Bronze Arrowhead (drawn here), Rough Cast Bronze Arrowheads (four heads on a runner, the gang mold's cavity), Bronze Arrow
  (vanilla's arrow with a bronze head).
- Clay Saw Blade / Plate / Spear Head / Sword Blade / Arrowheads Mold tops and unfired items; molten and solid bronze overlays for every mold shape (the cavity
  masks are the same as gen_copper_textures.py uses, so the metal fills the cavity drawn there).
- Bronze Helmet / Bronze Greaves items (iron's shapes, bronze) and worn layers: the helmet from iron's, the greaves are
  only the shins of iron's leggings with a leather strap at the knee.
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
ITEM, BLOCK, EQUIP = TEX / "item", TEX / "block", TEX / "entity/equipment"
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


BRONZE = [(52, 32, 12), (92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]
ROUGH = [(40, 30, 16), (74, 56, 30), (108, 84, 46), (134, 106, 60), (158, 128, 78)]       # dull casting skin
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]   # a touch yellower than copper
WOOD = [(52, 34, 18), (88, 58, 30), (122, 84, 46), (150, 108, 62)]
LEATHER = [(56, 32, 18), (84, 50, 28), (110, 70, 42)]


def recolor(img, pal, lo=None, hi=None, mask=None):
    """Luminance -> palette over the pixels in mask (default: all opaque ones), keeping alpha."""
    out = img.copy()
    pts = mask if mask is not None else px(img)
    ls = [lum(img.getpixel(p)) for p in pts]
    lo = min(ls) if lo is None else lo
    hi = max(ls) if hi is None else hi
    for p in pts:
        a = img.getpixel(p)[3]
        out.putpixel(p, ramp(pal, (lum(img.getpixel(p)) - lo) / max(1, hi - lo)) + (a,))
    return out


STICK = {c[:3] for c in (vanilla("item/stick").getpixel(p) for p in px(vanilla("item/stick")))}


def head_pixels(img):
    head = [p for p in px(img) if img.getpixel(p)[:3] not in STICK]
    # the iron hoe outlines its haft in the head's own dark colour: keep an outline pixel only if it borders the metal
    dark = min(img.getpixel(p)[:3] for p in head)
    metal = {p for p in head if img.getpixel(p)[:3] != dark}
    return [p for p in head if p in metal or any((p[0] + dx, p[1] + dy) in metal for dx in (-1, 0, 1) for dy in (-1, 0, 1))]


def only(img, pts):
    out = Image.new("RGBA", img.size)
    for p in pts:
        out.putpixel(p, img.getpixel(p))
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


# ---- tools and heads: iron shapes in bronze ----
pieces, tools = {}, {}
for t in ("pickaxe", "axe", "shovel", "hoe"):
    src = vanilla(f"item/iron_{t}")
    hp = head_pixels(src)
    tool = recolor(src, BRONZE, mask=hp)
    tools[t] = tool
    pieces[f"{t}_head"] = only(tool, hp)

knife_src = Image.open(LIB / "tools/steel_knife.png").convert("RGBA")
blade_px = [p for p in px(knife_src) if len(set(knife_src.getpixel(p)[:3])) == 1]  # the grey blade (the handle is brown)
tools["knife"] = recolor(knife_src, BRONZE, lo=0x18, hi=0x5A, mask=blade_px)
pieces["knife_blade"] = only(tools["knife"], blade_px)


def saw(with_handle):
    """A broad blade from the lower left to the upper right, teeth on its lower-right edge; the saw adds a D-handle."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            along, across = x - y, x + y - 15  # along: -15 lower left .. 15 upper right; across: + = lower right
            if -6 <= along <= 13 and -2 <= across <= 1:  # body: bright back edge, darker towards the teeth
                t = {-2: 0.88, -1: 0.6, 0: 0.42, 1: 0.26}[across] - (0.1 if along >= 12 else 0)
                img.putpixel((x, y), ramp(BRONZE, t) + (255,))
            elif -5 <= along <= 12 and ((across == 2 and along % 4 == 3) or (across == 3 and along % 4 == 0)):  # sawteeth
                img.putpixel((x, y), (BRONZE[2] if across == 2 else BRONZE[1]) + (255,))
            elif with_handle and -13 <= along <= -7 and -4 <= across <= 3:
                if -11 <= along <= -9 and -1 <= across <= 0:
                    continue  # the hand hole
                edge = along in (-13, -12, -7) or across in (-4, 3)
                img.putpixel((x, y), ramp(WOOD, 0.2 if edge else 0.75 - 0.06 * across) + (255,))
    return img


def is_iron(c):
    return c[3] and max(c[:3]) - min(c[:3]) <= 6


def bronzed(src, grip=False):
    """Every grey pixel in bronze (luminance kept); grip=True rewraps the brown hilt in leather."""
    metal = [p for p in px(src) if is_iron(src.getpixel(p))]
    out = recolor(src, BRONZE, lo=24, hi=285, mask=metal)  # hi past white: iron's white edge becomes gold, not cream
    if grip:
        wood = [p for p in px(src) if p not in metal]
        out = recolor(out, LEATHER + [(140, 96, 60)], mask=wood)
    return out, metal


tools["sword"], sword_metal = bronzed(vanilla("item/iron_sword"), grip=True)


def connected(pts, start):
    """The pixels of pts joined to start (the blade without the separate pommel)."""
    pts, seen, todo = set(pts), {start}, [start]
    while todo:
        x, y = todo.pop()
        for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if q in pts and q not in seen:
                seen.add(q)
                todo.append(q)
    return sorted(seen)


spear_icon, spear_metal = bronzed(vanilla("item/iron_spear"))
save(spear_icon, ITEM / "bronze_spear.png")
save(bronzed(vanilla("item/iron_spear_in_hand"))[0], ITEM / "bronze_spear_in_hand.png")

tools["saw"] = saw(True)
pieces["saw_blade"] = saw(False)
pieces["plate"] = recolor(Image.open(LIB / "bronze_plate.png").convert("RGBA"), BRONZE)
# (added after the older pieces so their rough-cast seeds stay the same)
pieces["sword_blade"] = only(tools["sword"], connected(sword_metal, (14, 0)))
pieces["spear_head"] = only(spear_icon, spear_metal)
ingot = recolor(Image.open(LIB / "bronze_ingot.png").convert("RGBA"), BRONZE)
save(ingot, ITEM / "bronze_ingot.png")

for i, (name, img) in enumerate(pieces.items()):
    save(img, ITEM / f"bronze_{name}.png")
    save(rough(img, 70 + i), ITEM / f"rough_cast_bronze_{name}.png")
for t, img in tools.items():
    save(img, ITEM / f"bronze_{t}.png")


# ---- Bronze Arrowhead, the arrowhead sprue, Bronze Arrow ----
def arrowhead():
    """One cast arrowhead along the diagonal, point to the upper right: a leaf blade (lit edge upper left), two barbs
    trailing back past the shoulders, a tang for the shaft; outlined dark on the shaded side."""
    img = Image.new("RGBA", (16, 16))
    shape = {}
    for x in range(16):
        for y in range(16):
            a, c = x - y, x + y - 15  # a: along (-15 lower left .. 15 upper right), c: across (- = upper left, lit)
            if 0 <= a <= 13:
                half = 4.2 if a <= 4 else (13 - a) * 4.2 / 9  # square shoulders, tapering to the point
                if abs(c) <= half:
                    edge = abs(c) > half - 1.2 and abs(c) >= 2  # the ground edge
                    shape[x, y] = (0.92 if edge else 0.7) if c < 0 else (0.25 if edge else 0.5) if c > 0 else 0.98
            elif -4 <= a <= -1 and 4 <= abs(c) <= 5:  # barbs
                shape[x, y] = 0.75 if c < 0 else 0.3
            elif -9 <= a <= -1 and abs(c) <= 1:  # tang
                shape[x, y] = 0.45 if c < 0 else 0.28
    for (x, y), t in shape.items():
        img.putpixel((x, y), ramp(BRONZE, t) + (255,))
    for x in range(16):
        for y in range(16):
            if (x, y) not in shape and x + y - 15 > 0 and any((x + dx, y + dy) in shape for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), BRONZE[0] + (255,))
    return img


ARROW_SPRUE = [  # four heads on a runner (# metal), as cast in the gang mold; also the mold's cavity
    "................",
    "...#.......#....",
    "..###.....###...",
    "..###.....###...",
    ".#####...#####..",
    ".#.#.#...#.#.#..",
    "...#.......#....",
    "...#########....",
    "...#...#...#....",
    "..###..#..###...",
    "..###..#..###...",
    ".#####.#.#####..",
    ".#.#.#.#.#.#.#..",
    "...#...#...#....",
    "...#########....",
    "................",
]
sprue = Image.new("RGBA", (16, 16))
for y, row in enumerate(ARROW_SPRUE):
    for x, ch in enumerate(row):
        if ch == "#":
            up = y == 0 or ARROW_SPRUE[y - 1][x] != "#"
            left = x == 0 or row[x - 1] != "#"
            sprue.putpixel((x, y), ramp(BRONZE, 0.8 if up or left else 0.45 if row[x + 1] == "#" else 0.25) + (255,))
save(arrowhead(), ITEM / "bronze_arrowhead.png")
save(rough(sprue, 90), ITEM / "rough_cast_bronze_arrowheads.png")

# the vanilla arrow with its flint head swapped for bronze (head pixels: top right quarter, the greys)
arrow = vanilla("item/arrow")
head_px = [(x, y) for x in range(8, 16) for y in range(8) if is_iron(arrow.getpixel((x, y)))]
save(recolor(arrow, BRONZE, lo=40, hi=270, mask=head_px), ITEM / "bronze_arrow.png")

# ---- armour: items and worn layers ----
save(recolor(vanilla("item/iron_helmet"), BRONZE), ITEM / "bronze_helmet.png")
save(recolor(vanilla("item/iron_leggings"), BRONZE), ITEM / "bronze_greaves.png")
layer = vanilla("entity/equipment/humanoid/iron")
helmet = Image.new("RGBA", layer.size)
for x, y in px(layer):
    if y < 16:  # head and hat only: there is no bronze chestplate or boots
        helmet.putpixel((x, y), layer.getpixel((x, y)))
save(recolor(helmet, BRONZE), EQUIP / "humanoid/bronze.png")
legs = vanilla("entity/equipment/humanoid_leggings/iron")
greaves = Image.new("RGBA", legs.size)
for x, y in px(legs):
    shin = x < 16 and y >= 24          # lower 8 of the leg's 12 side pixels (leg box at 0,16: 4x12x4)
    sole = 8 <= x < 12 and 16 <= y < 20  # underside of the leg box
    if shin or sole:
        greaves.putpixel((x, y), legs.getpixel((x, y)))
greaves = recolor(greaves, BRONZE)
for x in range(16):  # leather strap holding the greave below the knee
    greaves.putpixel((x, 24), ramp(LEATHER, 0.3 + 0.4 * (x % 4 == 1)) + (255,))
save(greaves, EQUIP / "humanoid_leggings/bronze.png")

# ---- molds ----
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]
fired_tex = Image.open(BLOCK / "kiln_clay_fired.png").convert("RGBA")


def without_stick(img):
    return only(img, head_pixels(img))


copper_knife_blade = only(knife_src, blade_px)
masks = {"ingot": vanilla("item/copper_ingot"), "pickaxe_head": without_stick(vanilla("item/copper_pickaxe")),
         "axe_head": without_stick(vanilla("item/copper_axe")), "shovel_head": without_stick(vanilla("item/copper_shovel")),
         "hoe_head": without_stick(vanilla("item/copper_hoe")), "knife_blade": copper_knife_blade,
         "saw_blade": pieces["saw_blade"], "plate": pieces["plate"], "spear_head": pieces["spear_head"], "sword_blade": pieces["sword_blade"],
         "arrowheads": sprue}
NEW = ("saw_blade", "plate", "spear_head", "sword_blade", "arrowheads")


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


for i, (shape, m) in enumerate(masks.items()):
    if shape in NEW:
        save(mold_top(m, fired_tex, FIRED, 30 + i), BLOCK / f"clay_{shape}_mold_top.png")
        save(item_icon(mold_top(m, None, RAW, 40 + i)), ITEM / f"unfired_clay_{shape}_mold.png")
    solid = Image.new("RGBA", (16, 16))
    for p in px(m):
        t = 0.5 + (0.25 if not cavity(m, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
        solid.putpixel(p, ramp(BRONZE[1:], t) + (255,))
    save(solid, BLOCK / f"clay_{shape}_mold_bronze_solid.png")
    molten = Image.new("RGBA", (16, 64))
    for f in range(4):
        rng = random.Random(f * 11 + i + 100)
        for x, y in px(m):
            molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
    save(molten, BLOCK / f"clay_{shape}_mold_bronze_molten.png", frames=8)

# ---- washed concentrate ----
WORKSHOP = LIB / "workshop"
CU_WASHED = [(58, 22, 10), (104, 44, 22), (150, 70, 38), (196, 104, 62), (232, 150, 100)]
SN_WASHED = [(14, 12, 12), (28, 24, 24), (44, 40, 40), (64, 60, 62), (92, 88, 92)]


def concentrate(src, pal, glint, seed):
    """A dust pile recoloured onto pal (cleaner than crushed ore: no grit), damp-dark at the foot, glinting on top."""
    rng = random.Random(seed)
    pts = px(src)
    ls = [lum(src.getpixel(p)) for p in pts]
    lo, hi = min(ls), max(ls)
    top, foot = min(y for _, y in pts), max(y for _, y in pts)
    out = Image.new("RGBA", (16, 16))
    for (x, y), l in zip(pts, ls):
        t = (l - lo) / max(1, hi - lo) - 0.18 * (y - top) / max(1, foot - top) + rng.uniform(-0.06, 0.06)
        out.putpixel((x, y), ramp(pal, t) + (src.getpixel((x, y))[3],))
    inner = [p for p in pts if all((p[0] + dx, p[1] + dy) in set(pts) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))]
    for p in rng.sample(sorted(inner), 6):  # metal glints, more of them on the upper half
        if p[1] < (top + foot) / 2 or rng.random() < 0.4:
            out.putpixel(p, glint + (255,))
    return out


save(concentrate(Image.open(WORKSHOP / "copper/copper_dust.png").convert("RGBA"), CU_WASHED, (255, 206, 160), 5), ITEM / "copper_concentrate.png")
save(concentrate(Image.open(WORKSHOP / "tin/tin_dust2.png").convert("RGBA"), SN_WASHED, (214, 214, 224), 6), ITEM / "tin_concentrate.png")
print("bronze textures ok")
