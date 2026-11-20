"""Textures for the extra bronze tools (BronzeTools.java). Run: python scripts/gen_bronzetools_textures.py

Same bronze palette, rough casting skin and mold drawing as gen_bronze_textures.py (helpers copied, so that script's
output and seeds stay untouched).
- Bronze Bucket: vanilla's bucket recoloured to bronze, one pixel lower, with a cordage bail over the rim; the Water /
  Lava / Milk Bucket keep vanilla's fluid pixels. The Bronze Bucket Body is the unshifted bucket without the bail.
- Bronze Sickle / Sickle Blade: the Flint Sickle with its grey flint blade in bronze.
- Bronze Shears: vanilla's shears all in bronze (one-piece spring shears).
- Bronze Fish Hook / Bronze Needle: the Bone Fish Hook and Bone Needle with the bone in bronze (the needle keeps its thread);
  the Bronze Hook Fishing Rod is the Bone Hook rod with the hook in bronze (cast: as the bone rod's, the hook is in the water).
- Bronze Tongs / Tong Arms: drawn here, two arms crossing at a rivet, short jaws upper right; the tongs wrap the handles in
  leather. Bronze Chisel / Chisel Blade: drawn here, a flat-edged blade on a wooden handle with a bronze ferrule.
- Bronze Quarry Hammer: the head of the user's library quarry_hammer (25x25, box-scaled to 16x16) in bronze on the Bronze
  Smithing Hammer's haft (gen_bloomery_textures.py's output).
- Rough Cast pieces: the same shapes, dull and mottled with casting flash; the fish hooks and needles are gang castings
  (four on a runner), also the cavities of their molds.
- Clay molds: fired tops, unfired item icons, molten and solid bronze in the cavity (as gen_bronze_textures.py).
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
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
jar = zipfile.ZipFile(JAR)

BRONZE = [(52, 32, 12), (92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]
ROUGH = [(40, 30, 16), (74, 56, 30), (108, 84, 46), (134, 106, 60), (158, 128, 78)]
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]
WOOD = [(52, 34, 18), (88, 58, 30), (122, 84, 46), (150, 108, 62)]
LEATHER = [(56, 32, 18), (84, 50, 28), (110, 70, 42), (140, 96, 60)]
CORD = [(88, 70, 40), (140, 116, 72), (186, 160, 110)]
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]


def vanilla(path):
    return Image.open(BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def ours(name):
    return Image.open(ITEM / f"{name}.png").convert("RGBA")


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


def recolor(img, pal, lo=None, hi=None, mask=None):
    """Luminance -> palette over the pixels in mask (default: all opaque ones), keeping alpha."""
    out = img.copy()
    pts = mask if mask is not None else px(img)
    ls = [lum(img.getpixel(p)) for p in pts]
    lo = min(ls) if lo is None else lo
    hi = max(ls) if hi is None else hi
    for p in pts:
        out.putpixel(p, ramp(pal, (lum(img.getpixel(p)) - lo) / max(1, hi - lo)) + (img.getpixel(p)[3],))
    return out


def only(img, pts):
    out = Image.new("RGBA", img.size)
    for p in pts:
        out.putpixel(p, img.getpixel(p))
    return out


def grey(c, tol=6):
    return c[3] and max(c[:3]) - min(c[:3]) <= tol


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
    """A dark edge on the shaded (lower right) side of the shape."""
    out = img.copy()
    for x in range(16):
        for y in range(16):
            if not img.getpixel((x, y))[3] and any(0 <= x - dx < 16 and 0 <= y - dy < 16 and img.getpixel((x - dx, y - dy))[3]
                                                   for dx, dy in ((1, 0), (0, 1))):
                out.putpixel((x, y), color + (255,))
    return out


def line(x0, y0, x1, y1):
    """Pixels of a straight line (Bresenham)."""
    pts, dx, dy = [], abs(x1 - x0), -abs(y1 - y0)
    sx, sy, err = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1), abs(x1 - x0) - abs(y1 - y0)
    while True:
        pts.append((x0, y0))
        if (x0, y0) == (x1, y1):
            return pts
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy


def from_rows(rows, shade):
    """Pixel-art rows ('#' metal) shaded lit on the upper left as the arrowhead sprue."""
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "#":
                up = y == 0 or rows[y - 1][x] != "#"
                left = x == 0 or row[x - 1] != "#"
                right = x + 1 < 16 and row[x + 1] == "#"
                img.putpixel((x, y), ramp(shade, 0.8 if up or left else 0.45 if right else 0.25) + (255,))
    return img


items, rough_src = {}, {}

# ---- Bronze Bucket ----
bucket = vanilla("item/bucket")
body = recolor(bucket, BRONZE, lo=30, hi=260)
items["bronze_bucket_body"] = rough_src["bucket"] = body


def lowered(img):
    out = Image.new("RGBA", (16, 16))
    out.alpha_composite(img, (0, 1))
    return out


BAIL = [(2, 3), (2, 2), (3, 1), (4, 1), (5, 0), (6, 0), (7, 0), (8, 0), (9, 0), (10, 0), (11, 1), (12, 1), (13, 2), (13, 3)]


def with_bail(img):
    out = img.copy()
    for i, p in enumerate(BAIL):
        if not out.getpixel(p)[3] or p[1] < 2:
            out.putpixel(p, CORD[2 if i % 2 else 1] + (255,))
    return out


items["bronze_bucket"] = with_bail(lowered(body))
for fluid in ("water", "lava", "milk"):
    full = vanilla(f"item/{fluid}_bucket")
    mixed = full.copy()
    for p in px(full):  # where the filled bucket is the plain bucket, use bronze; elsewhere it's the fluid
        if full.getpixel(p) == bucket.getpixel(p):
            mixed.putpixel(p, body.getpixel(p))
    items[f"bronze_{fluid}_bucket"] = with_bail(lowered(mixed))

# ---- Bronze Sickle ----
flint_sickle = ours("flint_sickle")
blade = [p for p in px(flint_sickle) if grey(flint_sickle.getpixel(p))]
items["bronze_sickle"] = recolor(flint_sickle, BRONZE, mask=blade)
items["bronze_sickle_blade"] = rough_src["sickle_blade"] = only(items["bronze_sickle"], blade)

# ---- Bronze Shears ----
items["bronze_shears"] = rough_src["shears"] = recolor(vanilla("item/shears"), BRONZE, lo=20, hi=230)

# ---- Fish hook, needle, rod ----
items["bronze_fish_hook"] = recolor(ours("bone_fish_hook"), BRONZE)
needle = ours("bone_needle")
items["bronze_needle"] = recolor(needle, BRONZE, mask=[p for p in px(needle) if needle.getpixel(p)[0] - needle.getpixel(p)[1] <= 20])  # not the brown thread


def hooked(rod):
    """The bone hook's pixels (unsaturated, at the bottom right where the line ends) in bronze."""
    hook = [(x, y) for x, y in px(rod) if x >= 8 and y >= 13 and grey(rod.getpixel((x, y)), 30)]
    return recolor(rod, BRONZE, lo=40, hi=280, mask=hook) if hook else rod.copy()


items["bronze_hook_fishing_rod"] = hooked(ours("bone_hook_fishing_rod"))
items["bronze_hook_fishing_rod_cast"] = hooked(ours("bone_hook_fishing_rod_cast"))

HOOKS = [  # four hooks hanging off a runner (# metal): the gang casting and the mold's cavity
    "................",
    ".##############.",
    ".#...#....#...#.",
    ".#...#....#...#.",
    ".#...#....#...#.",
    ".#...#....#...#.",
    ".#...#....#...#.",
    "##.#.#.#.##.#.#.",
    "#.#..#.#.#.#..#.",
    "#.#..##..#.#..##",
    ".#...#...#..#.#.",
    "................",
    "................",
    "................",
    "................",
    "................",
]
NEEDLES = [  # four needles side by side on a runner, the eyes at the top
    "................",
    "................",
    "..############..",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "..#..#...#..#...",
    "................",
    "................",
    "................",
]
rough_src["fish_hooks"] = from_rows(HOOKS, BRONZE)
rough_src["needles"] = from_rows(NEEDLES, BRONZE)


# ---- Bronze Tongs ----
def tongs(grips):
    img = Image.new("RGBA", (16, 16))
    a = line(1, 12, 13, 3)   # lower arm: handle lower left, jaw upper right
    b = line(4, 15, 11, 1)   # upper arm crossing it
    for i, p in enumerate(a):
        img.putpixel(p, ramp(LEATHER, 0.65) + (255,) if grips and i < 4 else ramp(BRONZE, 0.72) + (255,))
    for i, p in enumerate(b):
        img.putpixel(p, ramp(LEATHER, 0.35) + (255,) if grips and i < 4 else ramp(BRONZE, 0.48) + (255,))
    for p in (a[-1], b[-1]):  # the jaws' gripping tips
        img.putpixel(p, BRONZE[5] + (255,))
    cross = sorted(set(a) & set(b)) or [a[len(a) * 2 // 3]]
    x, y = cross[0]
    img.putpixel((x, y), BRONZE[5] + (255,))  # the rivet
    return outline(img)


items["bronze_tongs"] = tongs(True)
items["bronze_tong_arms"] = rough_src["tongs"] = tongs(False)


# ---- Bronze Chisel ----
def chisel(handle):
    """Along the diagonal like the knife: handle lower left, a bronze ferrule, the blade widening to a flat edge upper right."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            along, across = x - y, x + y - 15  # along: -15 lower left .. 15 upper right; across: + = lower right
            if 0 <= along <= 14 and 0 <= across <= 1 or 11 <= along <= 14 and across in (-1, 2):
                edge = along >= 13
                img.putpixel((x, y), ramp(BRONZE, 0.95 if edge else 0.7 if across <= 0 else 0.42) + (255,))
            elif handle and -2 <= along <= 0 and -1 <= across <= 2:
                img.putpixel((x, y), ramp(BRONZE, 0.55 if across <= 0 else 0.3) + (255,))  # ferrule
            elif handle and -11 <= along <= -3 and -1 <= across <= 2:
                img.putpixel((x, y), ramp(WOOD, 0.85 if across <= 0 else 0.45 if across == 1 else 0.2) + (255,))
    return outline(img)


items["bronze_chisel"] = chisel(True)
items["bronze_chisel_blade"] = rough_src["chisel_blade"] = chisel(False)

# ---- Bronze Quarry Hammer ----
src = Image.open(Path(r"C:\Users\Yngve\Documents\Minecraft Modding\item") / "quarry_hammer.png").convert("RGBA").resize((16, 16), Image.BOX)
head = [p for p in px(src) if src.getpixel(p)[3] >= 128 and src.getpixel(p)[0] - src.getpixel(p)[2] > 20 and p[1] < 10]  # the red head
hammer = recolor(only(src, head), BRONZE[1:], mask=head)
smith = ours("bronze_smithing_hammer")
for p in px(smith):
    if p[1] >= 8 and p[0] < 8 and p not in head:
        hammer.putpixel(p, smith.getpixel(p))  # the haft, lower left
items["bronze_quarry_hammer"] = outline(hammer)

for name, img in items.items():
    save(img, ITEM / f"{name}.png")
SHAPES = ["bucket", "sickle_blade", "shears", "fish_hooks", "needles", "tongs", "chisel_blade"]
for i, s in enumerate(SHAPES):
    save(rough(rough_src[s], 200 + i), ITEM / f"rough_cast_bronze_{s}.png")

# ---- molds ----
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
    """The cavity: the shape without its dark outline pixels (outline() adds BRONZE[0] around it)."""
    return only(img, [p for p in px(img) if img.getpixel(p)[:3] != BRONZE[0]])


for i, s in enumerate(SHAPES):
    m = opaque_only(rough_src[s])
    save(mold_top(m, fired_tex, FIRED, 300 + i), BLOCK / f"clay_{s}_mold_top.png")
    save(item_icon(mold_top(m, None, RAW, 320 + i)), ITEM / f"unfired_clay_{s}_mold.png")
    solid = Image.new("RGBA", (16, 16))
    for p in px(m):
        t = 0.5 + (0.25 if not cavity(m, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
        solid.putpixel(p, ramp(BRONZE[1:], t) + (255,))
    save(solid, BLOCK / f"clay_{s}_mold_bronze_solid.png")
    molten = Image.new("RGBA", (16, 64))
    for f in range(4):
        rng = random.Random(f * 11 + i + 400)
        for x, y in px(m):
            molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
    save(molten, BLOCK / f"clay_{s}_mold_bronze_molten.png", frames=8)
print(f"bronze tools textures ok ({len(items)} items, {len(SHAPES)} molds)")
