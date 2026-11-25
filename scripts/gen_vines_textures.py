"""Textures for flora 4 (standalone, not in the generator chain): the wild grape vine and its grapes, teosinte and maize, and
their items (Vineyard.java, Maize.java; models in scripts/gen_flora.py).

- grape_vine_stage0..3 / bare: the leaf plane of a vine face (as vanilla's vine): two woody canes winding up with curling
  tendrils and five-lobed grape leaves lit from the top left; stage3's leaves are turning gold and red (autumn, ripe); bare is
  the winter vine, thicker grey-brown canes and dry tendrils. grape_vine_stage1..3_fruit: the plane in front, only the
  flower sprays (1), hard green clusters (2) or ripe dusky purple clusters with a bloom (3) hanging from short stalks.
- maize_stage0..6 (lower half) and maize_stage3..6_top (upper half): one stalk per crop plane, jointed, with long strap leaves
  arching out and drooping; ears in green husks with a silk tuft (5), then the dry tan plant with ears burst open on gold
  kernels and a brown tassel (6). wild_maize_stage0..3 and wild_maize_plant: teosinte, a bushy grass of many thin stems with
  small hard ears in two rows.
- items: grapes, raisins, wine, grape_vine, teosinte, maize_kernels, corn_cob, roasted_corn.
Run: python scripts/gen_vines_textures.py [preview dir]"""
import random
import sys
from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def hexc(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def to_img(px):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)
    return img


def put(px, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        px[(x, y)] = c


def grid(rows, pal):
    return to_img({(x, y): pal[ch] for y, row in enumerate(rows) for x, ch in enumerate(row) if ch != "."})


# ---------------------------------------------------------------- grape vine
GREEN = ["a4d46a", "6eaa44", "4e8a34", "346628", "22441a"]   # highlight, light, mid, dark, shadow
AUTUMN = ["f2cc5a", "d89a34", "b8582a", "8a3220", "5a2016"]  # gold to rust
CANE = ["3a2a1e", "5a4230", "7e6248"]
CANE_BARE = ["4a4036", "6e6254", "948876"]
# a five-lobed grape leaf, 5x5: lit from the top left, a dark vein from the stalk notch at the bottom
LEAF = ["L.L.M",
        "LHLMM",
        ".LvM.",
        "MMvMD",
        ".D.D."]


def canes(rng, cols, thick=False):
    """Two canes winding up the tile (they run on over the tile edge into the next block), side shoots, tendril curls."""
    px, path = {}, []
    for x0 in (3 + rng.randint(0, 2), 10 + rng.randint(0, 2)):
        x = x0
        for y in range(15, -1, -1):
            put(px, x, y, cols[0 if y % 5 == 0 else 1])
            if thick:
                put(px, x + 1, y, cols[2] if y % 3 else cols[1])
            path.append((x, y))
            if rng.random() < 0.35:
                x += rng.choice((-1, 1))
                put(px, x, y, cols[1])
        x0 = x
    for _ in range(4):  # tendrils: a short spur ending in a little curl
        x, y = rng.choice(path)
        d = rng.choice((-1, 1))
        for ox, oy in ((d, 0), (2 * d, -1), (3 * d, -1), (3 * d, -2), (2 * d, -2)):
            put(px, x + ox, y + oy, cols[2])
    return px, path


def leaves(px, path, rng, pal, count):
    spots = []
    for _ in range(count):
        x, y = rng.choice(path)
        x += rng.choice((-3, -2, 1, 2))
        y -= 2
        for j, row in enumerate(LEAF):
            for i, ch in enumerate(row):
                if ch != ".":
                    put(px, x + i, y + j, pal[{"H": 0, "L": 1, "M": 2, "D": 3, "v": 4}[ch]])
        spots.append((x + 2, y + 5))
    return spots


def autumn_pal(rng):
    """Turning leaves: some still green, most gold, a few rust red."""
    r = rng.random()
    return GREEN if r < 0.25 else AUTUMN if r < 0.8 else ["e08a4a", "c4502c", "9a3022", "6e2018", "481410"]


CLUSTER = ["...s...",
           "..ss...",
           "ABBABBC",
           "BCBBCBD",
           ".ABBACD",
           ".BCBBD.",
           "..ABCD.",
           "..BCD..",
           "...D..."]
GRAPE_RIPE = {"A": "b8acd4", "B": "5a3a7a", "C": "3e2458", "D": "26143a", "s": "6e5a30"}  # A: the dusty bloom
GRAPE_GREEN = {"A": "dcecaa", "B": "9cbc54", "C": "74983a", "D": "4e6e26", "s": "6e5a30"}
FLOWER = {"A": "f4f2c4", "B": "d6d888", "C": "aab45a", "D": "7e8a3e", "s": "6e7a3a"}


def vine_images():
    out = {}
    rng = random.Random(4041)
    base, path = canes(rng, CANE)
    spots = leaves(dict(base), path, random.Random(7), GREEN, 0)  # the anchor spots only
    green = dict(base)
    lr = random.Random(11)
    spots = leaves(green, path, lr, GREEN, 9)
    out["block/grape_vine_stage0.png"] = to_img(green)
    flowered = dict(green)
    fr = random.Random(13)
    for x, y in spots[:4]:  # tiny cream flower buds peeking from the leaves
        put(flowered, x + fr.choice((-1, 1)), y - 1, FLOWER["A"])
    out["block/grape_vine_stage1.png"] = to_img(flowered)
    out["block/grape_vine_stage2.png"] = to_img(green)
    turning = dict(base)
    ar = random.Random(11)
    for _ in range(9):
        leaves(turning, path, ar, autumn_pal(ar), 1)
    out["block/grape_vine_stage3.png"] = to_img(turning)
    bare, _ = canes(random.Random(4041), CANE_BARE, thick=True)
    out["block/grape_vine_bare.png"] = to_img(bare)

    def fruit(pal, small):
        px = {}
        for cx, cy in ((3, 3), (10, 7), (5, 9)) if not small else ((3, 4), (10, 8)):
            for j, row in enumerate(CLUSTER[: 7 if small else 9]):  # green clusters: shorter, still hard
                for i, ch in enumerate(row):
                    if ch != ".":
                        put(px, cx + i - 3, cy + j, pal[ch])
        return px
    sprays = {}
    for cx, cy in ((4, 4), (11, 8), (6, 10)):  # loose sprays of tiny green-cream flowers
        for ox, oy, c in ((0, 0, "s"), (0, 1, "s"), (-1, 2, "A"), (1, 2, "B"), (0, 3, "A"), (-1, 4, "C"), (1, 4, "A"), (0, 5, "B")):
            put(sprays, cx + ox, cy + oy, FLOWER[c])
    out["block/grape_vine_stage1_fruit.png"] = to_img(sprays)
    out["block/grape_vine_stage2_fruit.png"] = to_img(fruit(GRAPE_GREEN, True))
    out["block/grape_vine_stage3_fruit.png"] = to_img(fruit(GRAPE_RIPE, False))
    return out


# ---------------------------------------------------------------- maize
M_GREEN = ["9ad46a", "62a640", "468a32", "306a26", "1e4a1a"]
M_DRY = ["f0dc9a", "d4b46a", "b08e4a", "866a34", "5a4622"]
STALK, STALK_DRY = ["5e9638", "3e7628", "86b858"], ["c8a860", "a08448", "e2cc8a"]


def leaf(px, x, y, d, length, rise, pal):
    """A strap leaf from the stalk at x,y toward side d: up and out, then arching over and drooping; lit upper edge."""
    pts = []
    for i in range(length):
        t = i / max(1, length - 1)
        lx = x + d * (1 + i)
        ly = y - round(rise * (4 * t * (1 - t)) * 1.0) + round(max(0, t - 0.6) * 4)
        pts.append((lx, ly))
    for i, (lx, ly) in enumerate(pts):
        put(px, lx, ly, pal[1] if i < len(pts) - 2 else pal[2])
        put(px, lx, ly + 1, pal[3] if i < len(pts) - 1 else pal[4])
        if i % 3 == 1:
            put(px, lx, ly, pal[0])


def stalk(px, top, cols, x=7, wide=True):
    for y in range(15, top - 1, -1):
        put(px, x, y, cols[0] if y % 4 else cols[1])  # a darker joint every 4 px
        if wide:
            put(px, x + 1, y, cols[2] if y % 4 else cols[1])


def ear(px, x, y, d, ripe):
    """An ear on the stalk's side: green husk with a silk tuft; ripe, the husk dry and burst open on gold kernels."""
    if ripe:
        for oy, row in enumerate(("hk.", "hKk", "hkK", "hKk", ".h.")):
            for ox, ch in enumerate(row):
                if ch != ".":
                    put(px, x + d * (1 + ox), y + oy, {"h": M_DRY[2], "k": "f2c43a", "K": "c8901e"}[ch])
        put(px, x + d * 2, y - 1, "7a4a22")
    else:
        for oy, row in enumerate(("lm", "lmd", "lmd", ".m")):
            for ox, ch in enumerate(row):
                if ch != ".":
                    put(px, x + d * (1 + ox), y + oy, {"l": "a8d47a", "m": "6eaa48", "d": "4a8a34"}[ch])
        put(px, x + d * 2, y - 1, "d8b45a")
        put(px, x + d * 3, y - 2, "b0703a")  # the silk


def tassel(px, x, y, ripe):
    c = ["8a5a2a", "b4844a"] if ripe else ["e6d88a", "c8b85a"]
    put(px, x, y, c[1])
    for i, (ox, oy) in enumerate(((0, -1), (0, -2), (0, -3), (-1, -2), (-2, -1), (1, -2), (2, -1), (-1, -4), (1, -4), (-3, 0), (3, 0))):
        put(px, x + ox, y + oy, c[i % 2])


def maize_images():
    out = {}
    # lower half: s0 seedling, s1 young, s2 knee high, s3-s6 the full lower stalk (s5 ears, s6 dry and ripe)
    px = {}
    stalk(px, 12, STALK, wide=False)
    leaf(px, 7, 12, -1, 4, 2, M_GREEN)
    leaf(px, 7, 13, 1, 4, 2, M_GREEN)
    out["block/maize_stage0.png"] = to_img(px)
    px = {}
    stalk(px, 7, STALK, wide=False)
    for y, d, n in ((8, -1, 6), (10, 1, 6), (12, -1, 5), (13, 1, 4)):
        leaf(px, 7, y, d, n, 3, M_GREEN)
    out["block/maize_stage1.png"] = to_img(px)
    px = {}
    stalk(px, 1, STALK)
    for y, d, n in ((2, -1, 6), (4, 1, 7), (7, -1, 7), (9, 1, 7), (12, -1, 6), (13, 1, 5)):
        leaf(px, 7 if d < 0 else 8, y, d, n, 3, M_GREEN)
    out["block/maize_stage2.png"] = to_img(px)
    for s in (3, 4, 5, 6):
        ripe = s == 6
        pal, cols = (M_DRY, STALK_DRY) if ripe else (M_GREEN, STALK)
        px = {}
        stalk(px, 0, cols)
        for y, d, n in ((1, 1, 7), (5, -1, 7), (9, 1, 7), (12, -1, 6), (14, 1, 4)):
            leaf(px, 7 if d < 0 else 8, y, d, n, 3, pal)
        if s >= 5:
            ear(px, 8, 4, 1, ripe)
            ear(px, 7, 8, -1, ripe)
        out[f"block/maize_stage{s}.png"] = to_img(px)
        # the top: the stalk going on up, a few leaves, and the tassel once it heads
        px = {}
        top = {3: 9, 4: 5, 5: 3, 6: 3}[s]
        stalk(px, top, cols, wide=s >= 5)
        for y, d, n in ((max(top + 2, 6), -1, 6), (max(top + 5, 10), 1, 7), (13, -1, 6)):
            if y <= 14:
                leaf(px, 7 if d < 0 else 8, y, d, n, 2, pal)
        if s >= 4:
            tassel(px, 7, top - 1 if s > 4 else top, ripe)
        out[f"block/maize_stage{s}_top.png"] = to_img(px)
    # teosinte: a bushy tuft of thin stems; ears tiny, in two rows of hard grey-brown seeds
    def teosinte(height, stems, eared, dry=False):
        px = {}
        rng = random.Random(height * 7 + stems)
        pal = M_DRY if dry else ["a6cc72", "6c9c44", "4e7e34", "36602a", "22401c"]
        for k in range(stems):
            x = 2 + k * 12 // max(1, stems - 1) if stems > 1 else 7
            h = height - rng.randint(0, 3)
            for y in range(15, 15 - h, -1):
                put(px, x, y, pal[2] if y % 3 else pal[3])
                if rng.random() < 0.2 and y < 14:
                    x += rng.choice((-1, 1))
            leaf(px, x, 16 - h + 3, rng.choice((-1, 1)), 4, 2, pal)
            if eared:
                ex, ey = x + 1, 16 - h + 5
                for i in range(3):  # a little zigzag ear of hard seeds
                    put(px, ex + (i % 2), ey + i, "8a7a5a" if not dry else "6a5a40")
                put(px, x, 15 - h, "c8b878" if not dry else "8a6a3a")  # a small tassel tip
                put(px, x - 1, 16 - h, "c8b878" if not dry else "8a6a3a")
        return px
    out["block/wild_maize_stage0.png"] = to_img(teosinte(7, 3, False))
    out["block/wild_maize_stage1.png"] = to_img(teosinte(9, 4, False))
    out["block/wild_maize_stage2.png"] = to_img(teosinte(14, 4, True))
    out["block/wild_maize_stage3.png"] = to_img(teosinte(15, 4, True, dry=True))
    out["block/wild_maize_plant.png"] = to_img(teosinte(15, 5, True))
    return out


# ---------------------------------------------------------------- items
ITEMS = {
    "grapes": ({"A": "c4b8e0", "B": "5e3e80", "C": "40285c", "D": "28163c", "s": "6e5a30", "g": "6eaa44", "G": "a4d46a", "d": "346628"}, [
        "................",
        "........s.gG....",
        ".......s.gGGg...",
        "......s..dgGd...",
        "...ABAsBAB.d....",
        "..ABBBABBBC.....",
        "..BBCBBBCBC.....",
        "..ABBABBABCD....",
        "...BCBBCBCD.....",
        "...ABBABBCD.....",
        "....BCBBCD......",
        "....ABBACD......",
        ".....BCBD.......",
        ".....ABCD.......",
        "......CD........",
        "................"]),
    "raisins": ({"A": "a2725a", "B": "6a3e2e", "C": "4a2820", "D": "2e1814", "w": "8a5a44"}, [
        "................",
        "................",
        "................",
        "................",
        "................",
        "......AB........",
        ".....ABwC.AB....",
        "....ABCBCABwC...",
        "...AwBCDABCBD...",
        "..ABCBDABwCDAB..",
        "..BwCDABCBDABwC.",
        ".ABCDABwCDABCBD.",
        ".BCBDBCBDCBwCD..",
        "..CDDCDD.CDDD...",
        "................",
        "................"]),
    "wine": ({"k": "8a6a4a", "K": "b0905e", "a": "e0a07a", "b": "c07448", "c": "9a5432", "d": "6a3420", "e": "4a2216",
              "W": "6a1a3a", "w": "9a2a50", "g": "5e3e80", "G": "c4b8e0", "l": "6eaa44"}, [
        "................",
        "......kK........",
        "......Kk........",
        ".....abbc.......",
        "......bc........",
        ".....abbc.......",
        "....abbbcd......",
        "...abbbbccd.....",
        "...abWwWWcd.....",
        "...abwgGgWd.....",
        "...abWggglW.....",
        "...bbbWgWcdd....",
        "....bccccdd.....",
        ".....cddde......",
        "................",
        "................"]),
    "grape_vine": ({"H": "a4d46a", "L": "6eaa44", "M": "4e8a34", "D": "346628", "v": "22441a", "s": "5a4230", "S": "3a2a1e",
                    "B": "5e3e80", "A": "c4b8e0", "C": "40285c"}, [
        "................",
        "...........sS...",
        "..L.L.M...s.....",
        "..LHLMM..s......",
        "...LvM..s.......",
        "..MMvMDs........",
        "...D.Ds..L.L.M..",
        ".....s...LHLMM..",
        "....s.....LvM...",
        "....s.ss.MMvMD..",
        "...s.s..s.D.D...",
        "...s.....ABA....",
        "..s......BCBC...",
        "..S.......BC....",
        ".S..............",
        "................"]),
    "teosinte": ({"a": "c8b88a", "b": "9a8a62", "c": "6e6044", "d": "4a3e2c", "g": "86a65a"}, [
        "................",
        "................",
        "..........g.....",
        ".........g......",
        "........ab......",
        ".......abc......",
        "......abcd......",
        ".....abcd.......",
        "....abcd........",
        "...abcd...ab....",
        "..abcd...abc....",
        "..bcd...abcd....",
        "..cd...abcd.....",
        "......abcd......",
        "......bcd.......",
        "................"]),
    "maize_kernels": ({"A": "fff0a0", "B": "f2c43a", "C": "c8901e", "D": "8a5e12"}, [
        "................",
        "................",
        "................",
        "......AB........",
        ".....ABBC...AB..",
        ".....BBCD..ABBC.",
        "......CD...BBCD.",
        "..AB........CD..",
        ".ABBC...AB......",
        ".BBCD..ABBC.....",
        "..CD...BBCD.AB..",
        ".......CDD.ABBC.",
        "....AB.....BBCD.",
        "...ABBC.....CD..",
        "...BBCD.........",
        "....CD.........."]),
    "corn_cob": ({"A": "fff0a0", "B": "f2c43a", "C": "c8901e", "D": "8a5e12", "h": "c8d48a", "H": "9ab060", "j": "6e8a40",
                  "s": "c8a060", "S": "7a4a22"}, [
        "................",
        "............SS..",
        "...........SsS..",
        "..........ABBj..",
        ".........ABCBH..",
        "........ABCBCh..",
        ".......ABCBCBH..",
        "......ABCBCBC...",
        ".....ABCBCBCH...",
        "....ABCBCBCDh...",
        "...hBCBCBCDH....",
        "..hHCBCBCDj.....",
        "..HjjCBCDj......",
        "..jHhjDDj.......",
        "...jHj..........",
        "................"]),
    "roasted_corn": ({"A": "f8d878", "B": "d89a2a", "C": "a46a16", "D": "6a3e0e", "E": "3a2008", "s": "c8a060", "S": "7a4a22"}, [
        "................",
        "............SS..",
        "...........SsS..",
        "..........ABEC..",
        ".........ABCBD..",
        "........AECBCD..",
        ".......ABCECBD..",
        "......ABCBCED...",
        ".....AECBCBCD...",
        "....ABCBECBD....",
        "...ABCBCBCD.....",
        "...BCECBCD......",
        "...CBCBCD.......",
        "....DDDD........",
        "................",
        "................"]),
}


def build():
    out = vine_images()
    out.update(maize_images())
    for name, (pal, rows) in ITEMS.items():
        out[f"item/{name}.png"] = grid(rows, pal)
    return out


if __name__ == "__main__":
    imgs = build()
    for rel, img in imgs.items():
        (RES / rel).parent.mkdir(parents=True, exist_ok=True)
        img.save(RES / rel)
    print(f"{len(imgs)} vine and maize textures")
    if len(sys.argv) > 1:
        s, cols = 8, 8
        keys = list(imgs)
        sheet = Image.new("RGBA", (cols * (16 * s + 6) + 6, ((len(keys) + cols - 1) // cols) * (16 * s + 6) + 6), (120, 140, 160, 255))
        for i, k in enumerate(keys):
            sheet.alpha_composite(imgs[k].resize((16 * s, 16 * s), Image.NEAREST), (6 + (i % cols) * (16 * s + 6), 6 + (i // cols) * (16 * s + 6)))
        Path(sys.argv[1]).mkdir(parents=True, exist_ok=True)
        sheet.save(Path(sys.argv[1]) / "vines_textures.png")
