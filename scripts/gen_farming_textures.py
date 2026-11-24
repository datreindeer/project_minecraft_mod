# Farming textures: grain/pea crops (cross model, stage0..4), Wild Pea (wild grains and oats: gen_wildgrain_textures.py), withered crop, farm blocks
# (quern, baking stone, compost heap, scarecrow) and farming items. Crops and block surfaces are painted
# procedurally from fixed seeds; items are hand-drawn pixel grids. Colours baked in, like vanilla crops.
# usage: python scripts/gen_farming_textures.py [preview.png]
import random
import sys
from pathlib import Path
from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)


def grid(rows, pal, img=None, ox=0, oy=0):
    """Hand-drawn grid; '.' is transparent. Palette values may be tuples (random pick, seeded by caller)."""
    img = img or new()
    assert all(len(r) == len(rows[0]) for r in rows), rows
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                assert ch in pal, f"unknown colour {ch!r}"
                c = pal[ch]
                put(img, ox + x, oy + y, RNG.choice(c) if isinstance(c, tuple) else c)
    return img


RNG = random.Random(0)

# ============================== crops ==============================
GREEN = dict(l="62c23c", m="44a02c", d="2e7c22", D="1f5a19")
GREEN_OLD = dict(l="9cb046", m="7f9a36", d="5f7a2a", D="485c22")   # yellowing lower leaves at stage 3
GRAIN = {
    # head l/m/d, awn w, ripe stalk l/m/d, green head l/m/d, green awn
    "barley": dict(h=("f2e6ae", "dccb84", "b39e5c"), w="eadcaa", s=("d9c37c", "b9a15a", "8c773c"),
                   gh=("a8d872", "80b44c", "5c8c38"), gw="c4e49a"),
    "einkorn": dict(h=("c4804c", "9c5834", "6c3824"), w="b6845a", s=("bc9c5c", "99783e", "6c522e"),
                    gh=("98b452", "74923a", "566c2c"), gw="aac078"),
    "emmer": dict(h=("e0b056", "ba863a", "8a5e26"), w="cc9c52", s=("d4b262", "b08e44", "7e6432"),
                  gh=("90c85c", "6ca442", "4c7c30"), gw="b4d888"),
}


def head(img, g, x, y, ripe, rng):
    """Grain head with its top at (x, y)."""
    p = GRAIN[g]
    hl, hm, hd = p["h"] if ripe else p["gh"]
    w = p["w"] if ripe else p["gw"]
    if g == "barley":      # plump head, kernels in rows, long awns fanning out like a brush
        for i in range(4):
            put(img, x, y + i, hl if i % 2 == 0 else hm)
            put(img, x + 1, y + i, hm if i % 2 == 0 else hd)
        for i in range(1, 4):
            put(img, x - i, y - i, w)
            put(img, x + 1 + i, y - i, w)
            put(img, x + (i % 2), y - i, w)
    elif g == "einkorn":   # thin flat zigzag head, short awns
        for i in range(4):
            put(img, x, y + i, hm if i % 2 else hl)
            put(img, x + (1 if i % 2 else -1), y + i, hd)
        put(img, x, y - 1, w)
        put(img, x + 1, y - 2, w)
    else:                  # emmer: long dense flat 2-wide head, bearded along its sides
        for i in range(5):
            put(img, x, y + i, hl if i % 2 == 0 else hm)
            put(img, x + 1, y + i, hm if i % 2 == 0 else hd)
        for dx, dy in ((-1, -1), (2, -1), (0, -2), (1, -1), (-1, 1), (2, 2), (-1, 3)):
            put(img, x + dx, y + dy, w)


def stalk(img, x, h, lean, cols, leaves, rng, leaf_cols=None):
    """Stalk from the bottom edge, h px tall, bending by lean above mid height; returns its top (x, y)."""
    l, m, d = cols
    lc = leaf_cols or cols
    top = (x, 16 - h)
    for i in range(h):
        y = 15 - i
        xx = x + (lean if i >= h * 0.6 else 0)
        put(img, xx, y, d if i < h * 0.3 else (m if i < h * 0.7 else l))
        top = (xx, y)
    for (ly, side) in leaves:        # blades off the stalk, curving out and drooping
        if ly >= h - 2:
            continue
        y = 15 - ly
        xx = x + (lean if ly >= h * 0.6 else 0)
        put(img, xx + side, y - 1, lc[1])
        put(img, xx + 2 * side, y - 2, lc[0])
        if ly > 3:
            put(img, xx + 3 * side, y - 2, lc[1])
            put(img, xx + 4 * side, y - 1, lc[2])
    return top


def blade(img, x, n, d, cols, y=15):
    """Leaf blade growing up from (x, y), drifting sideways by d every 2 px; dark at base, light at tip."""
    l, m, dk = cols
    for i in range(n):
        put(img, x + d * ((i + 1) // 2), y - i, dk if i < n * 0.3 else (m if i < n * 0.7 else l))


def grain_crop(g, stage):
    rng = random.Random(g)               # same plant layout through all stages
    tufts = [(x + rng.choice((0, 1)), rng.randint(0, 2)) for x in (1, 4, 7, 10, 13)]
    stalks = [(x, rng.choice((-1, 0, 0, 1)), rng.choice((0, 1, 2, 3, 4)), (rng.randint(4, 7), rng.choice((-1, 1))))
              for x in (1, 4, 6, 9, 11, 14)]
    img = new()
    full = {"barley": 12, "einkorn": 10, "emmer": 13}[g]
    ripe = stage == 4
    young = (GREEN["l"], GREEN["m"], GREEN["d"])
    if stage == 0:
        for x, v in tufts[::2]:
            put(img, x, 15, GREEN["d"]); put(img, x, 14, GREEN["m"]); put(img, x + (1 if v else -1), 13, GREEN["l"])
            if v == 2:
                put(img, x - 1, 14, GREEN["l"])
        return img
    if stage <= 2:                        # leafy tufts, no stalks yet
        n = (3, 4) if stage == 1 else (6, 8)
        for x, v in tufts:
            blade(img, x, n[1] - v, 0, young)
            blade(img, x - 1, n[0] - (v == 1), -1, young, 14)
            blade(img, x + 1, n[0] - (v == 2), 1, young, 14)
        return img
    base = (GREEN_OLD["l"], GREEN_OLD["m"], GREEN_OLD["d"]) if stage == 3 else GRAIN[g]["s"]
    for x, v in tufts:                    # old lower leaves around the stalk feet
        blade(img, x - 1, 3 - v, -1, base, 14)
        blade(img, x + 1, 2 + (v == 1), 1, base, 14)
    hh = {"barley": 4, "einkorn": 4, "emmer": 5}[g]
    cols = GRAIN[g]["s"] if ripe else young
    for x, lean, dh, leaf in stalks:
        h = full - (1 if stage == 3 else 0) - dh
        x = min(x, 14)
        t = stalk(img, x, h, lean, cols, [leaf], rng, base)
        head(img, g, t[0], t[1] - hh + 2, ripe, rng)
    return img


PEA = dict(l="78c84a", m="55a636", d="3a8228", D="27601e", t="a6dc6a", f="f6f6ee", F="d8dcc8", y="e8d878",
           p="d6f09a", P="aad664", q="3a6a24", k="f0a8c8", K="b0508a")


def pea_vine(stage, wild=False):
    rng = random.Random("wild_pea" if wild else "pea")
    img = new()
    heights = [3, 6, 10, 13, 14]
    h = 9 if wild else heights[stage]
    stems = [3, 8, 12] if not wild else [2, 6, 10, 13]
    if stage == 0 and not wild:
        for x in (4, 11):
            put(img, x, 15, PEA["d"]); put(img, x, 14, PEA["m"])
            put(img, x - 1, 13, PEA["l"]); put(img, x + 1, 13, PEA["l"]); put(img, x - 1, 12, PEA["m"])
        put(img, 8, 15, PEA["d"]); put(img, 8, 14, PEA["l"])
        return img
    tops = []
    for n, x0 in enumerate(stems):
        hh = h - (n % 2) * 2 + rng.randint(-1, 0)
        x = x0
        for i in range(hh):           # zigzag climbing stem
            y = 15 - i
            if i and i % 3 == 0:
                x += 1 if (i // 3 + n) % 2 else -1
            put(img, x, y, PEA["D"] if i < 2 else PEA["d"])
            if i % 3 == 1 and i > 0:  # paired round leaflets at each node
                side = -1 if (i // 3 + n) % 2 else 1
                put(img, x + side, y, PEA["m"]); put(img, x + side * 2, y, PEA["l"])
                put(img, x + side, y - 1, PEA["l"]); put(img, x + side * 2, y + 1, PEA["m"])
        tops.append((x, 16 - hh))
        tx, ty = x, 16 - hh               # curly tendril above the tip
        put(img, tx, ty - 1, PEA["t"]); put(img, tx + 1, ty - 2, PEA["t"]); put(img, tx + 2, ty - 2, PEA["t"])
        put(img, tx + 2, ty - 1, PEA["t"])
    if wild:   # trailing sideways tendrils
        for (x, y) in ((0, 12), (1, 11), (15, 11), (14, 10), (5, 13), (9, 12)):
            put(img, x, y, PEA["t"])
    flower = wild or stage == 3
    pods = wild or stage == 4
    spots = [(1, 9), (13, 7), (6, 5), (10, 11), (4, 12)] if not wild else [(4, 9), (11, 10), (7, 10)]
    for j, (x, y) in enumerate(spots):
        if stage < 3 and not wild:
            break
        if flower and (not wild or j < 2):
            a, b = (PEA["k"], PEA["K"]) if wild else (PEA["f"], PEA["F"])
            put(img, x, y, a); put(img, x + 1, y, a); put(img, x, y + 1, b); put(img, x + 1, y - 1, PEA["y"] if not wild else a)
        if pods and (not wild or j == 2):
            px, py = (x + 1, y) if not wild else (x, y)
            for k in range(4 if not wild else 3):     # fat hanging pod, curving at the tip
                put(img, px + (1 if k == 3 else 0), py + k, PEA["p"])
                put(img, px + 1 + (1 if k == 3 else 0), py + k, PEA["P"])
                put(img, px - 1 + (1 if k == 3 else 0), py + k, PEA["q"])
    return img


def withered():
    img = new()
    c = ("a08c62", "84704a", "665638", "8a8478", "6a665c")
    rng = random.Random("withered")
    for x0, h, dirn in ((1, 6, 1), (3, 9, 1), (5, 11, 1), (7, 6, -1), (9, 8, 1), (11, 10, -1), (13, 7, -1), (14, 5, -1)):
        x, y = x0, 15
        for i in range(h):            # stalk rises then flops over sideways
            put(img, x, y, rng.choice(c))
            if i < h // 2:
                y -= 1
            else:
                x += dirn
                if i % 2:
                    y += 1 if i > h * 0.8 else 0
        put(img, x, y + 1, c[4])
    for (x, y) in ((3, 10), (9, 8), (11, 12), (6, 13)):  # crumpled dead leaves
        put(img, x, y, c[3]); put(img, x + 1, y, c[1])
    return img


# ============================== block surfaces ==============================
def noise_fill(pal, seed, weights=None):
    rng = random.Random(seed)
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, rng.choices(pal, weights or [1] * len(pal))[0])
    return img


def stone(seed, pal):
    """Rough stone: speckled base with darker cracks and lighter flecks, vanilla-stone style blobs."""
    rng = random.Random(seed)
    img = noise_fill(pal[1:3], seed, [3, 2])
    for _ in range(9):
        x, y = rng.randrange(16), rng.randrange(16)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            if rng.random() < 0.8:
                put(img, x + dx, y + dy, pal[3])
    for _ in range(8):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, pal[0]); put(img, x + 1, y, pal[0]) if rng.random() < .5 else None
    for _ in range(3):  # small cracks
        x, y = rng.randrange(16), rng.randrange(16)
        for i in range(3):
            put(img, x + i, y + (i // 2), pal[4])
    return img


def flour_pile():
    """A heap of stone-ground flour: a soft cone lit from the upper left, off-white with bran specks, dust at the foot."""
    rng = random.Random(77)
    pal = ["8d8270", "b3a993", "d2c9b4", "e6dfcd", "f4efe2"]
    img = new()
    for y in range(4, 15):
        half = 1.0 + (y - 4) * 0.62                       # widens towards the base
        for x in range(16):
            dx = x + 0.5 - 7.5
            if abs(dx) > half:
                continue
            t = 0.62 - dx / max(half, 1) * 0.28 - (y - 4) * 0.02
            if y >= 13:
                t -= 0.18 * (y - 12)                      # shadowed foot of the heap
            t += rng.uniform(-0.06, 0.06)
            i = min(4, max(0, int(t * 5)))
            put(img, x, y, pal[i])
            if rng.random() < 0.06 and y < 13:
                put(img, x, y, "9c8a68")                  # bran specks
    for y in range(4, 15):                                # dark rim so it reads against the slot
        row = [x for x in range(16) if img.getpixel((x, y))[3]]
        if row:
            put(img, row[0], y, pal[0]); put(img, row[-1], y, pal[0])
    for x in (2, 13, 14, 1):                              # a little spilled dust
        put(img, x, 15, pal[2])
    return img


QUERN = ("a8a49c", "8e8a82", "7c7870", "66625c", "4a4742")
SOOT = ("6a6460", "524e4a", "45413e", "35322f", "242220")


def quern_side():
    img = stone("quern_side", QUERN)
    for x in range(16):               # seam between upper runner stone and lower bed stone
        put(img, x, 7, QUERN[4]); put(img, x, 8, QUERN[0] if x % 3 else QUERN[1])
    for x in range(16):               # rim highlight / bottom shadow
        put(img, x, 0, QUERN[0] if x % 4 else QUERN[1]); put(img, x, 15, QUERN[4] if x % 3 else QUERN[3])
    for x in (5, 6, 11):              # flour dust trickling from the seam
        put(img, x, 9, "dedad0")
    put(img, 6, 10, "cfcbc0")
    return img


def quern_top():
    img = stone("quern_top", QUERN)
    for r, c in ((7.5, QUERN[3]),):
        pass
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 1.6:
                put(img, x, y, "1e1c1a")           # the eye
            elif d < 2.6:
                put(img, x, y, QUERN[4] if (x + y) % 2 else QUERN[3])
            elif 5.5 < d < 6.4 and (x * 7 + y * 3) % 5 == 0:
                put(img, x, y, QUERN[3])            # faint worn grinding ring
    for (x, y) in ((6, 5), (9, 10), (10, 6)):
        put(img, x, y, "dedad0")                    # grain/flour bits near the eye
    return img


def wood_grain(seed, pal, vertical=True):
    rng = random.Random(seed)
    img = new()
    lines = [rng.choice(pal[1:3]) for _ in range(16)]
    for x in range(16):
        base = lines[x]
        for y in range(16):
            c = base
            if rng.random() < 0.12:
                c = pal[0]
            if rng.random() < 0.08:
                c = pal[3]
            put(img, x if vertical else y, y if vertical else x, c)
    for _ in range(2):                # knots
        x, y = rng.randrange(2, 14), rng.randrange(2, 14)
        put(img, x, y, pal[4]); put(img, x, y + 1, pal[3]); put(img, x - 1, y, pal[3]); put(img, x + 1, y + 1, pal[0])
    return img


WOOD = ("a8824c", "8f6c3c", "7c5c32", "5e4424", "45311a")


def baking_side():
    img = stone("baking_side", SOOT)
    for x in range(16):
        put(img, x, 0, SOOT[0]); put(img, x, 1, SOOT[1] if x % 3 else SOOT[0])
        put(img, x, 14, SOOT[4] if x % 2 else SOOT[3]); put(img, x, 15, SOOT[4])
        if x % 5 == 2:
            put(img, x, 13, SOOT[4])       # soot streaks creeping up from the fire below
            put(img, x + 1, 12, SOOT[3])
    return img


def baking_top():
    img = stone("baking_top", SOOT)
    rng = random.Random("bt")
    for y in range(16):
        for x in range(16):
            if (x in (0, 15) or y in (0, 15)) and rng.random() < .7:
                put(img, x, y, SOOT[4])     # blackened edges
    for (x, y) in ((5, 6), (10, 9), (7, 11), (11, 4)):
        put(img, x, y, "8a7a62")             # flour dust
    return img


LOAF = {
    "dough": dict(l="fbf2dc", m="ecdcb6", d="d4c094", D="b8a274", c="fffaf0"),
    "bread": dict(l="e8b45a", m="c98d34", d="a26a22", D="704616", c="f2d088"),
    "burnt": dict(l="4a3a30", m="2e2420", d="1e1816", D="0e0b0a", c="5e4a3c"),
}
LOAF_GRID = [
    "..lllll.",
    ".lcllmmm",
    "lllmmmmd",
    "lmmmdmdd",
    ".mdddddD",
    "..DDDDD.",
]


def loaf(kind):
    img = grid(LOAF_GRID, LOAF[kind], ox=4, oy=5)
    if kind != "dough":               # scored top
        for (x, y) in ((6, 6), (7, 7), (9, 6), (10, 7)):
            put(img, x, y, LOAF[kind]["d"])
    if kind == "burnt":
        for (x, y) in ((5, 7), (8, 8), (10, 9)):
            put(img, x, y, "5a3a26")
    return img


COMPOST = ("4a3420", "3a281a", "2e2014", "22170e", "5a4228")


def wattle(dark=False):
    img = new()
    k = 0.55 if dark else 1.0
    def sh(c):
        r, g, b = hexc(c)[:3]
        return (int(r * k), int(g * k), int(b * k), 255)
    gap = sh("2a1c10")
    for y in range(16):
        for x in range(16):
            put(img, x, y, gap)
    rod = ("a0804c", "866838", "6a5028", "4e3a1c")
    for row in range(4):              # horizontal withies weaving over/under stakes
        y0 = row * 4
        for x in range(16):
            over = ((x // 5) + row) % 2 == 0
            put(img, x, y0 + 1, sh(rod[0] if over else rod[1]))
            put(img, x, y0 + 2, sh(rod[1] if over else rod[2]))
            put(img, x, y0 + 3, sh(rod[3]))
    for sx in (2, 7, 12):             # upright stakes
        for y in range(16):
            under = ((sx // 5) + y // 4) % 2 == 0
            if not under or y % 4 == 0:
                put(img, sx, y, sh("7a5a30")); put(img, sx + 1, y, sh("5a4222"))
    return img


def litter():
    rng = random.Random("litter")
    img = noise_fill(COMPOST[:3], "litter_base")
    leaf = ("b8762e", "9a5e24", "7c4a1c", "c8963e", "8a7a34", "6e5a2a")
    for _ in range(14):
        x, y = rng.randrange(16), rng.randrange(16)
        c1, c2 = rng.sample(leaf, 2)
        put(img, x, y, c1); put(img, x + 1, y, c1); put(img, x, y + 1, c2)
        if rng.random() < .5:
            put(img, x + 1, y - 1, c2)
    for _ in range(4):                # twigs
        x, y = rng.randrange(14), rng.randrange(16)
        for i in range(3):
            put(img, x + i, y + (i == 2), "5a4024")
    return img


def compost_top():
    rng = random.Random("compost")
    img = noise_fill(COMPOST, "compost_base", [3, 4, 3, 2, 1])
    for _ in range(10):
        x, y = rng.randrange(16), rng.randrange(16)
        put(img, x, y, "1a120a"); put(img, x + 1, y, "1a120a") if rng.random() < .4 else None
    for _ in range(5):
        put(img, rng.randrange(16), rng.randrange(16), rng.choice(("5c6a2c", "6e5a2a", "8a6a3a")))
    return img


def hide_cloth():
    rng = random.Random("cloth")
    img = noise_fill(("8a6440", "7c5838", "704e30", "7c5838"), "cloth_base")
    for _ in range(12):
        put(img, rng.randrange(16), rng.randrange(16), rng.choice(("9a7450", "5e4228")))
    for y in range(16):               # seam with cross stitches
        put(img, 10, y, "4a3420")
        if y % 3 == 0:
            put(img, 9, y, "d0bc8c"); put(img, 11, y + 1, "d0bc8c")
    for (x, y) in ((3, 4), (4, 4), (5, 5), (2, 11), (3, 12)):
        put(img, x, y, "5e4228")       # creases
    for x in range(16):               # frayed hem
        if x % 3 != 1:
            put(img, x, 15, "5e4228")
    return img


STRAW = ("f0dc8a", "dcc26a", "c4a650", "a08638", "7a6428")


def straw_block():
    rng = random.Random("straw")
    img = new()
    for y in range(16):
        for x in range(16):
            put(img, x, y, STRAW[3] if (x + y // 3) % 3 else STRAW[4])
    for _ in range(26):               # loose strands at random slants
        x, y = rng.randrange(16), rng.randrange(16)
        dx = rng.choice((-1, 0, 1))
        for i in range(rng.randint(3, 6)):
            put(img, x + (dx * i) // 2, y + i, STRAW[min(2, i // 2)] if rng.random() < .8 else STRAW[0])
    return img


# ============================== items ==============================
GRAIN_SPRITES = {
    "barley": (["...w", ".ab.", "abc.", "cc.."], dict(a="f2e6ae", b="dccb84", c="a89058", w="e6d8a6")),
    "einkorn": (["ab", "bc"], dict(a="c88a56", b="9c5834", c="6a3622")),
    "emmer": (["...a", "..ab", ".abc", "abc.", "c..."], dict(a="e6b866", b="c0903e", c="8a5e26")),
}
GRAIN_SPOTS = {
    "barley": [(3, 3, 0), (9, 2, 1), (6, 7, 0), (11, 8, 1), (2, 10, 1), (8, 11, 0)],
    "einkorn": [(4, 4), (10, 3), (7, 6), (3, 9), (12, 8), (6, 11), (10, 11), (8, 9)],
    "emmer": [(2, 2, 0), (9, 1, 1), (6, 6, 0), (11, 8, 1), (1, 10, 1), (8, 10, 0)],
}


def grain_item(g):
    rows, pal = GRAIN_SPRITES[g]
    img = new()
    for spot in GRAIN_SPOTS[g]:
        x, y = spot[:2]
        r = [row[::-1] for row in rows] if len(spot) > 2 and spot[2] else rows
        grid(r, pal, img, x, y)
    return img


PEAS = [
    "................",
    "................",
    "..........ttS...",
    ".........tS.....",
    "........qpP.....",
    ".......qpPgP....",
    "......qpoaGgq...",
    ".....qpPGgoaP...",
    "....qpoaGgPgq...",
    "...qpPGgoaPq....",
    "...pPoaGgPq.....",
    "..qpPGgPPq..oa..",
    "..qpPPPqq..oaGg.",
    "...qqqq....oGgg.",
    ".............gg.",
    "................",
]
PEAS_PAL = dict(t="a6dc6a", S="5c8a34", q="2e6020", p="7cc04c", P="5aa236", o="b8ec7c", a="8ad25a", G="5cae38",
                g="3e8a2a")

SHEAVES = {
    "barley": ([
        "..w....w....w...",
        "...w...w...w....",
        "....w..w..w..w..",
        "..w..abab.ab.w..",
        "...w.bcbcabcw...",
        "....abcbcbc..ww.",
        "...w.bcabcabcw..",
        ".....abcbcbcc...",
        "......sbcbcc....",
        ".....sSssc......",
        "....rRsS........",
        "...sRrS.........",
        "..sSsR..........",
        ".sStS...........",
        ".tSt............",
        "..t.............",
    ], dict(w="eadcaa", a="f2e6ae", b="dccb84", c="b39e5c", s="d9c37c", S="b9a15a", t="8c773c",
            r="9a7a58", R="6a5038")),
    "einkorn": ([
        "................",
        "......b...b.....",
        ".....a.b.a..b...",
        "......ab.ba.a..b",
        ".....b.a.ab.b.a.",
        "......cb.c.ab.b.",
        "......sc.bcb.c..",
        ".......sscbc....",
        "......sSssc.....",
        ".....sSsS.......",
        "....rRsS........",
        "...sRrS.........",
        "..sSsR..........",
        ".sStS...........",
        ".tSt............",
        "..t.............",
    ], dict(a="c4804c", b="9c5834", c="6c3824", s="bc9c5c", S="99783e", t="6c522e", r="d0b890",
            R="9a8460")),
    "emmer": ([
        ".....k...k......",
        "....k.k.k.k..k..",
        ".....ab..ab.k.k.",
        "....kbc.kbc..ab.",
        ".....ab..ab.kbck",
        "....kbc.kbc..ab.",
        ".....ab..ab.kbc.",
        "......sc..sc.s..",
        "......s.ss.ss...",
        ".....sSsSss.....",
        "....rRsS........",
        "...sRrS.........",
        "..sSsR..........",
        ".sStS...........",
        ".tSt............",
        "..t.............",
    ], dict(a="e0b056", b="ba863a", c="8a5e26", k="6e4a22", w="d4a656", s="d4b262", S="b08e44",
            t="7e6432", r="9a7a58", R="6a5038")),
}

STRAW_ITEM = [
    "..........l..m..",
    "........l..m.l.l",
    ".........m.l.m..",
    "......l..l.m.l..",
    ".......m.l.ml...",
    "........mlml.d..",
    ".....l..lmld....",
    "......m.lmd.....",
    ".......lmrd.....",
    "......lmRr......",
    ".....lmlrR......",
    "....l.mlmd......",
    "...l.m.l.d......",
    "..l.m..d........",
    "...d.d..........",
    "................",
]
STRAW_PAL = dict(l="f0dc8a", m="d0b45e", d="9c823a", r="9a7a58", R="6a5038")

FLOUR = [
    "................",
    "......t..t......",
    ".......tt.......",
    "......rRRr......",
    ".......ss.......",
    "......sSSs......",
    ".....slllSs.....",
    "....slllllSs....",
    "...sllllllSSs...",
    "...slllllllSs...",
    "...sllllllSSs...",
    "...slllllSSSs...",
    "....sSSSSSSs.fw.",
    ".....sssssswffw.",
    "...........fwwff",
    "................",
]
FLOUR_PAL = dict(t="a08060", r="8a6a48", R="6a5038", s="9a8a6c", S="c8b896", l="e8dcc0", w="fbfaf4",
                 f="e2e0d6")

DOUGH = [
    "................",
    "................",
    "................",
    "................",
    "......hhhh......",
    "....hhlllmmm....",
    "...hllllllmmm...",
    "..hlllllllmmmd..",
    "..llllllmmmmmd..",
    "..mllllmmmmmdd..",
    "..mmmmmmmmmddd..",
    "...dmmmmmmddd...",
    "....ddddddDD....",
    "......DDDD......",
    "................",
    "................",
]
DOUGH_PAL = dict(h="fffcf0", l="f4e8cc", m="e2d2aa", d="c8b484", D="a8946a")

BOWL_PAL = dict(a="3f2b0b", b="533909", c="714f0f", d="43300b", e="1f1502", f="3a2706", g="302109")
RAW_PORRIDGE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....aaaaaa.....",
    "...aaxwvxwvaa...",
    "..avxwvuxvwvxe..",
    "..affvwxvwvffe..",
    "..acbffffffbbe..",
    "...gcccccccbe...",
    "....eebccbee....",
    "......eeee......",
    "................",
    "................",
    "................",
]
RAW_PAL = dict(BOWL_PAL, x="d8c47a", u="b8a05a", w="8ca4b4", v="6e8898")
PORRIDGE = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....aaaaaa.....",
    "...aahllmmhaa...",
    "..almlhllmomme..",
    "..affmlmmlmffe..",
    "..acbffffffbbe..",
    "...gcccccccbe...",
    "....eebccbee....",
    "......eeee......",
    "................",
    "................",
    "................",
]
PORRIDGE_PAL = dict(BOWL_PAL, h="fbf0d4", l="ecdcb2", m="d6c08e", o="c48a3a")

COMPOST_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "........gG......",
    ".....bbaGg......",
    "....baacbbc.....",
    "...baclccbccy...",
    "..bacbcccdccdc..",
    "..acccdcbcddcd..",
    ".baycccdccdcdcd.",
    ".acccdcdcdcdddd.",
    ".cdcccdcddcddDd.",
    "..ddddDdDdDDDD..",
    "...DDDDDDDDDD...",
    "................",
]
COMPOST_PAL = dict(a="7a5a3a", b="5e4228", c="4a3420", d="35251a", D="22170e", l="1a120a", g="8aa040",
                   G="5c7a2c", y="b8762e")

MANURE = [
    "................",
    "................",
    "................",
    "................",
    "......aab.......",
    ".....abbbc......",
    ".....bbcbcd.....",
    "......cccd......",
    "...aabcdd.aab...",
    "..abbbbcdabbbc..",
    "..bbcbbcdbbcbcd.",
    "..cbbccddcbccdd.",
    "...cccddd.cddd..",
    "....dddd..ddd...",
    "................",
    "................",
]
MANURE_PAL = dict(a="7a5a34", b="5c4226", c="45301c", d="2e2012")

FLINT = dict(a="222020", b="757575", c="565656", d="0e0e0e", e="3d3c3c", w="9a9898")
STICK = dict(f="493615", g="684e1e", j="281e0b", n="896727")
TWINE = dict(h="b79c89", i="9b8474", k="836f61", l="a58c7c", m="ac917f")
SICKLE = [
    "......aaaa......",
    "....aabccca.....",
    "...abwaaabca....",
    "..aw.....wbca...",
    "..a.......wbca..",
    "...........wca..",
    "...........wbca.",
    "........fghwbca.",
    ".......fgmldca..",
    "......fnjikda...",
    ".....fgj........",
    "....fnj.........",
    "...fgj..........",
    "..fnj...........",
    "..jj............",
    "................",
]
FLAIL = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "........kh......",
    "......ihlOo.....",
    ".....fmk.POo....",
    "....fgj...POo...",
    "...fnj.....POo..",
    "..fgj.......POo.",
    ".fnj.........PP.",
    ".jj.............",
    "................",
    "................",
    "................",
]


# ============================== build ==============================
def build():
    out = {}
    for g in GRAIN:
        for s in range(5):
            out[f"block/{g}_stage{s}"] = grain_crop(g, s)
        out[f"item/{g}_grain"] = grain_item(g)
        rows, pal = SHEAVES[g]
        out[f"item/{g}_sheaf"] = grid(rows, pal)
    for s in range(5):
        out[f"block/pea_stage{s}"] = pea_vine(s)
    out["block/wild_pea"] = pea_vine(4, wild=True)
    out["block/withered_crop"] = withered()
    out["block/quern_stone_side"] = quern_side()
    out["block/quern_stone_top"] = quern_top()
    out["block/quern_handle"] = wood_grain("quern_handle", WOOD)
    out["block/baking_stone_side"] = baking_side()
    out["block/baking_stone_top"] = baking_top()
    for k in LOAF:
        out[f"block/baking_stone_{k}"] = loaf(k)
    out["block/compost_heap_side"] = wattle()
    out["block/compost_heap_inside"] = wattle(dark=True)
    out["block/compost_heap_litter"] = litter()
    out["block/compost_heap_compost"] = compost_top()
    out["block/scarecrow_post"] = wood_grain("scarecrow_post", WOOD)
    out["block/scarecrow_cloth"] = hide_cloth()
    out["block/scarecrow_straw"] = straw_block()
    out["item/peas"] = grid(PEAS, PEAS_PAL)
    out["item/straw"] = grid(STRAW_ITEM, STRAW_PAL)
    out["item/flour"] = flour_pile()
    out["item/dough"] = grid(DOUGH, DOUGH_PAL)
    out["item/raw_porridge"] = grid(RAW_PORRIDGE, RAW_PAL)
    out["item/porridge"] = grid(PORRIDGE, PORRIDGE_PAL)
    out["item/compost"] = grid(COMPOST_ITEM, COMPOST_PAL)
    out["item/manure"] = grid(MANURE, MANURE_PAL)
    out["item/flint_sickle"] = grid(SICKLE, {**FLINT, **STICK, **TWINE})
    out["item/flail"] = grid(FLAIL, {**STICK, **TWINE, "O": "a8803e", "o": "80602c", "P": "5a3e18"})
    return out


def preview(images, dst):
    s, cols = 8, 10
    keys = list(images)
    rows = (len(keys) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * (16 * s + 6) + 6, rows * (16 * s + 6) + 6), (120, 150, 120, 255))
    for i, k in enumerate(keys):
        sheet.alpha_composite(images[k].resize((16 * s, 16 * s), Image.NEAREST),
                              (6 + (i % cols) * (16 * s + 6), 6 + (i // cols) * (16 * s + 6)))
    sheet.save(dst)


if __name__ == "__main__":
    imgs = build()
    for rel, img in imgs.items():
        assert img.size == (16, 16)
        img.save(TEX / f"{rel}.png")
    print(f"wrote {len(imgs)} textures")
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
