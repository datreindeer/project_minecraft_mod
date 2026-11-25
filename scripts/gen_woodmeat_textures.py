"""Textures for Firewood Piles and the unique dried meats (data: gen_woodmeat.py).

- block/<wood>_firewood_pile_end: the cut ends of a stack of split billets, a 4x4 grid of 4 px ends (one per billet of a
  full pile): quartered and halved logs with the pith in a corner or on an edge, growth rings and a strip of bark round
  the outside, now and then a radial crack, all in that wood's own colours (rings from <wood>_log_top, bark from <wood>_log)
- block/<wood>_firewood_pile_side: the long sides and tops of the billets, four 4 px bands with the grain running down:
  bark (columns of the wood's log texture), split heartwood (its stripped log), bark with a split edge, split sapwood;
  a darker line where two billets meet
- item/: Venison, Beef, Game and Small Game Jerky, Dried Pork, Dried Mutton, Dried Fowl, Dried Seal Meat, Stockfish,
  Raw Meat Strips, Smoked Haunch / Ribs / Loin
- block/drying_rack_fowl, drying_rack_dried_fowl (+ _narrow_): spatchcocked birds hung by the legs from the twine, wings
  spread; pale skin raw, golden brown dried
Vanilla textures come from the merged Minecraft jar (run a gradle build first); mod woods from their own log textures.
Deterministic (hashes only). Run: python scripts/gen_woodmeat_textures.py
"""
import glob
import io
import math
import zipfile
from pathlib import Path

from PIL import Image
from orchard_table import ORCHARD

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar"))
             + glob.glob(str(Path.home() / "Documents/project_minecraft_mod/build/moddev/artifacts/minecraft-patched-*-merged.jar")))
assert JAR, "no merged minecraft jar (run a gradle build first)"
_jar = zipfile.ZipFile(JAR[0])
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar",
         "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew", "rowan", "bird_cherry", "wild_apple", "plum", "olive", "date_palm", "bitter_orange"] + ORCHARD


def h(x, y, salt):
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def block_tex(name):
    """A block texture: the mod's own if it has one, else vanilla's (16x16, first animation frame)."""
    own = TEX / "block" / f"{name}.png"
    img = Image.open(own) if own.exists() else Image.open(io.BytesIO(_jar.read(f"assets/minecraft/textures/block/{name}.png")))
    return img.convert("RGBA").crop((0, 0, 16, 16))


def vanilla(path):
    return Image.open(io.BytesIO(_jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def mod(path):
    return Image.open(TEX / f"{path}.png").convert("RGBA")


def save(img, rel):
    out = TEX / f"{rel}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    a, b, f = cols[i], cols[i + 1], t - i
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + (255,)


def mix(a, b, t):
    return tuple(round(a[k] + (b[k] - a[k]) * t) for k in range(3)) + (255,)


def palette(img, box=None):
    """Dark, mid and light colours of a texture (by brightness)."""
    if box:
        img = img.crop(box)
    px = sorted({img.getpixel((x, y))[:3] for x in range(img.width) for y in range(img.height)}, key=lum)
    return px[len(px) // 8], px[len(px) // 2], px[7 * len(px) // 8]


# ---------------------------------------------------------------- firewood piles
# pith position and bark radius per billet shape: quarters (pith in a corner), halves (pith on an edge), a whole round
SHAPES = [((3.6, 3.6), 3.7), ((0.4, 3.6), 3.7), ((3.6, 0.4), 3.7), ((0.4, 0.4), 3.7),
          ((2.0, 3.7), 2.7), ((2.0, 0.3), 2.7), ((3.7, 2.0), 2.7), ((2.0, 2.0), 1.9)]


def pile_end(wood):
    top, log = block_tex(f"{wood}_log_top"), block_tex(f"{wood}_log")
    rd, rm, rl = palette(top, (4, 4, 12, 12))  # the wood inside the bark
    bd, bm, _ = palette(log)
    img = Image.new("RGBA", (16, 16))
    for ty in range(4):
        for tx in range(4):
            (px, py), bark_r = SHAPES[int(h(tx, ty, 7) * len(SHAPES))]
            ring_at = 1.4 + h(tx, ty, 5) * 1.2  # one growth ring arc across the face
            for y in range(4):
                for x in range(4):
                    d = math.hypot(x + 0.5 - px, y + 0.5 - py)
                    if d > bark_r:
                        c = (bd if h(x + tx * 4, y + ty * 4, 11) < 0.6 else bm) + (255,)
                    elif d < 0.8:
                        c = mix(rm + (255,), rd + (255,), 0.5)  # the pith
                    elif abs(d - ring_at) < 0.45 or d > bark_r - 0.7:
                        c = mix(rl + (255,), rm + (255,), 0.65)  # a ring, and the sapwood edge under the bark
                    else:
                        c = mix(rl + (255,), rm + (255,), 0.15 * h(x + tx * 4, y + ty * 4, 13))
                    if x == 3 or y == 3:
                        c = shade(c, 0.8)  # the shadowed edge of the billet
                    img.putpixel((tx * 4 + x, ty * 4 + y), c)
    return img


def pile_side(wood):
    log, stripped = block_tex(f"{wood}_log"), block_tex(f"stripped_{wood}_log")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            band, bx = divmod(x, 4)
            if band == 0 or (band == 2 and bx < 3):
                c = log.getpixel((x, y))
            else:
                s = stripped.getpixel((x, (y + 5 * band) % 16))
                c = shade(s, 1.06 if band == 1 else 0.94 if band == 3 else 1.0)
                if h(x, y // 3, 21 + band) < 0.12:
                    c = shade(c, 0.86)  # a streak in the grain
            if bx == 3:
                c = shade(c, 0.72)  # where two billets meet
            img.putpixel((x, y), c[:3] + (255,))
    return img


for wood in WOODS:
    save(pile_end(wood), f"block/{wood}_firewood_pile_end")
    save(pile_side(wood), f"block/{wood}_firewood_pile_side")


# ---------------------------------------------------------------- items
def edge(pts, x, y):
    return any((x + dx, y + dy) not in pts for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def recolour(src, cols, rim=1.0, keep=None, salt=1, speck=None):
    """Repaint every opaque pixel along a colour ramp by its brightness; outline darkened by `rim`; `keep(c)` overrides."""
    pts = {(x, y) for y in range(src.height) for x in range(src.width) if src.getpixel((x, y))[3]}
    ls = [lum(src.getpixel(p)) for p in pts]
    lo, hi = min(ls), max(ls)
    out = Image.new("RGBA", src.size)
    for x, y in pts:
        c = src.getpixel((x, y))
        col = (keep(c) if keep else None) or ramp(cols, (lum(c) - lo) / (hi - lo or 1))
        if edge(pts, x, y):
            col = shade(col, rim)
        elif speck and h(x, y, salt) < speck[0]:
            col = speck[1]
        out.putpixel((x, y), col)
    return out


def strips(cols, fleck=None, fleck_rate=0.0, salt=1, lines=((1, 10, 9, 2), (3, 13, 12, 4), (7, 14, 14, 7)), width=1):
    """Thin wavy strips lying diagonally (jerky); top edges catch the light, the odd fat or pepper fleck."""
    img = Image.new("RGBA", (16, 16))
    pts = set()
    for k, (sx, sy, ex, ey) in enumerate(lines):
        for i in range(41):
            t = i / 40
            wob = 0.6 * math.sin(t * 6.3 + k * 2 + salt)
            x, y = sx + (ex - sx) * t + wob * 0.7, sy + (ey - sy) * t + wob * 0.7
            for dx, dy in ((0, 0), (1, 0), (0, 1)) + (((1, 1),) if width > 1 else ()):
                pts.add((min(15, max(0, round(x) + dx)), min(15, max(0, round(y) + dy))))
    for x, y in pts:
        t = 0.35 + 0.45 * h(x, y, salt + 11) - (0.15 if (x, y + 1) not in pts else 0)
        col = ramp(cols, t)
        if edge(pts, x, y) and (x, y - 1) not in pts:
            col = ramp(cols, 0.88)
        elif edge(pts, x, y):
            col = shade(col, 0.7)
        if fleck and h(x, y, salt + 12) < fleck_rate:
            col = fleck
        img.putpixel((x, y), col)
    return img


# jerky by its meat: venison dark and lean, beef redder with fat flecks, game almost black, raw strips bright with fat
save(strips([(46, 16, 14), (82, 30, 24), (112, 46, 34), (140, 70, 52)], salt=3), "item/venison_jerky")
save(strips([(70, 24, 16), (112, 44, 26), (146, 68, 40), (178, 100, 66)], (214, 170, 128, 255), 0.06, salt=5,
            lines=((1, 11, 10, 2), (4, 14, 14, 4)), width=2), "item/beef_jerky")
save(strips([(30, 14, 12), (56, 26, 20), (84, 40, 30), (110, 60, 44)], (26, 20, 18, 255), 0.08, salt=7), "item/game_jerky")
save(strips([(130, 20, 26), (180, 44, 44), (214, 86, 80), (236, 140, 128)], (240, 220, 206, 255), 0.12, salt=9,
            lines=((1, 9, 8, 2), (2, 13, 12, 3), (6, 15, 15, 6)), width=2), "item/raw_meat_strips")
# small game: a whole dried rabbit (vanilla raw rabbit), shrunk to a leathery brown
save(recolour(vanilla("item/rabbit"), [(40, 22, 14), (76, 44, 28), (116, 76, 48), (156, 114, 76)], rim=0.55, salt=13,
              speck=(0.05, (196, 160, 118, 255))), "item/small_game_jerky")
# pork: rosy tan, the fat gone translucent amber
save(recolour(vanilla("item/porkchop"), [(92, 44, 34), (146, 76, 58), (190, 116, 88), (222, 160, 122)], rim=0.65,
              keep=lambda c: (226, 184, 112, 255) if lum(c) > 0.8 else None), "item/dried_pork")
# mutton: deep red-brown, a creamy fat rim
save(recolour(vanilla("item/mutton"), [(58, 22, 18), (98, 40, 28), (134, 64, 42), (164, 94, 66)], rim=0.6,
              keep=lambda c: (220, 198, 160, 255) if lum(c) > 0.78 else None), "item/dried_mutton")
# fowl: golden-tan skin drawn tight, darker where the meat shows
save(recolour(vanilla("item/chicken"), [(104, 62, 26), (156, 104, 46), (198, 148, 74), (232, 196, 120)], rim=0.6, salt=17,
              speck=(0.06, (122, 76, 34, 255))), "item/dried_fowl")
# seal: near-black maroon slabs (blood-rich meat), with a sheen
save(recolour(vanilla("item/cooked_beef"), [(26, 10, 14), (48, 18, 24), (74, 30, 36), (112, 56, 60)], rim=0.55,
              keep=lambda c: (92, 70, 74, 255) if lum(c) > 0.8 else None), "item/dried_seal_meat")
# stockfish: a headless cod dried bone-hard and grey-white, hung by a twine loop through the tail
fish = vanilla("item/cod").transpose(Image.FLIP_LEFT_RIGHT)
stock = recolour(fish, [(108, 104, 96), (156, 152, 140), (200, 196, 184), (232, 230, 220)], rim=0.62, salt=19,
                 speck=(0.05, (176, 168, 150, 255)))
for x in range(16):  # the head is off: clear the head end (the right after flipping) past the gills
    for y in range(16):
        if x >= 13 and stock.getpixel((x, y))[3]:
            stock.putpixel((x, y), (0, 0, 0, 0))
pts = {(x, y) for y in range(16) for x in range(16) if stock.getpixel((x, y))[3]}
for x, y in pts:
    if x == max(px for px, _ in pts):
        stock.putpixel((x, y), (214, 150, 140, 255) if h(x, y, 23) < 0.6 else (176, 104, 98, 255))  # the cut, pink-dry
TWINE = (168, 128, 78, 255)
tail = min(pts)
for dx, dy in ((-1, -1), (-1, -2), (0, -3), (1, -2)):
    x, y = tail[0] + dx, tail[1] + dy
    if 0 <= x < 16 and 0 <= y < 16 and (x, y) not in pts:
        stock.putpixel((x, y), TWINE if dy != -3 else shade(TWINE, 0.8))
save(stock, "item/stockfish")

# whole smoked cuts: the cooked cut recoloured smoky mahogany, the bone (palest pixels) kept pale and smoke-yellowed
SMOKED = [(34, 16, 10), (72, 34, 18), (114, 58, 30), (160, 94, 50)]
for cut in ("haunch", "ribs", "loin"):
    save(recolour(mod(f"item/cooked_{cut}"), SMOKED, rim=0.55,
                  keep=lambda c: (214, 196, 150, 255) if lum(c) > 0.82 and abs(c[0] - c[2]) < 50 else None), f"item/smoked_{cut}")


# ---------------------------------------------------------------- Drying Rack: fowl
RAW_SKIN = [(196, 150, 128), (226, 186, 160), (244, 216, 194)]
DRY_SKIN = [(116, 70, 30), (164, 112, 52), (204, 156, 84)]
BIRD = ["l..l",  # legs tied to the twine (top), body spread flat, wing tips out to the sides, the neck stub at the bottom
        "l..l",
        "bbbb",
        "wbbw",
        "wbbw",
        "bbbb",
        ".bb.",
        ".n.."]


def bird(img, x0, dried, salt):
    skin = DRY_SKIN if dried else RAW_SKIN
    rows = BIRD[:-1] + [".bb."] if not dried else BIRD  # fresh: a fuller neck; dried shrinks to a stub
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch == "l":
                c = (150, 96, 56, 255) if dried else (214, 160, 92, 255)  # bare legs, yellow raw, horny dried
            elif ch == "w":
                c = shade(ramp(skin, 0.4), 0.85)
            elif ch == "n":
                c = shade(ramp(skin, 0.3), 0.8)
            else:
                c = ramp(skin, 0.75 - y * 0.06 + h(x + x0, y, salt) * 0.25)
                if x in (1, 2) and y in (3, 4):
                    c = shade(c, 0.9 if dried else 0.96)  # the split breastbone line
                if h(x + x0, y, salt + 1) < (0.14 if dried else 0.08):
                    c = shade(c, 0.85)  # follicles / dried creases
            img.putpixel((x0 + x, 4 + y), c)


def fowl_plane(dried, narrow=False):
    img = Image.new("RGBA", (16, 16))
    for x in (range(5) if narrow else range(3, 13)):
        img.putpixel((x, 3), TWINE if x % 3 else shade(TWINE, 0.8))
    for i, x0 in enumerate((0,) if narrow else (3, 9)):
        bird(img, x0, dried, 31 + i)
    return img


save(fowl_plane(False), "block/drying_rack_fowl")
save(fowl_plane(True), "block/drying_rack_dried_fowl")
save(fowl_plane(False, True), "block/drying_rack_narrow_fowl")
save(fowl_plane(True, True), "block/drying_rack_narrow_dried_fowl")
print("woodmeat textures ok")
