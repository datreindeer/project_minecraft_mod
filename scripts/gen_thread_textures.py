# Thread textures (gen_thread.py): Stinging Nettle (grown and seedling, cross), the Flax crop (stage0..4: shoots, leafy
# stems, sky-blue flowers, golden ripe stems with seed bolls), and the items: Nettle / Flax Seeds, Nettle / Flax Stalks,
# Nettle / Flax Fiber, Nettle / Linen / Sinew Thread and the Drop Spindle. Plants and stalks are painted along stems from
# fixed seeds, seeds are hand-drawn grids, the threads are the mod's Wool Thread recoloured so every thread reads alike.
# usage: python scripts/gen_thread_textures.py [preview.png]
import math
import random
import sys
from pathlib import Path
from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
RNG = random.Random(11)


def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)


def grid(rows, pal):
    img = new()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                put(img, x, y, RNG.choice(c) if isinstance(c, tuple) else c)
    return img


# ============================== Stinging Nettle ==============================
NET = dict(D="34522a", M="4b6c30", L="618a3c", E="78a24a", V="8cb45a", s="44622a", h="b4c49a", c="a7b878", C="8a9c5c")
# serrated, heart-shaped leaves pointing right (first column touches the stem); mirrored for the left side
LEAF = {
    2: ["DL.", "DME"],
    3: [".ML.", "DMVE", ".DM."],
    4: [".MLE.", "DMVLE", "DMMVL", ".DM.."],
}


def leaf(img, x, y, size, side, rng):
    for dy, row in enumerate(LEAF[size]):
        for dx, ch in enumerate(row):
            if ch != ".":
                c = NET[ch]
                if ch in "LE" and rng.random() < 0.25:  # the toothed edge catches the light unevenly
                    c = NET["M"]
                put(img, x + side * (dx + 1), y + dy - 1, c)


def nettle(young):
    rng = random.Random("nettle_young" if young else "nettle")
    img = new()
    stems = [(7, 6, 0.0), (3, 4, -0.1), (11, 5, 0.1)] if young else [(5, 15, -0.05), (11, 12, 0.08)]
    for x0, h, lean in stems:
        top = 15 - h
        for t in range(h):
            x, y = x0 + round(lean * t), 15 - t
            put(img, x, y, NET["s"])
            if not young and t % 3 == 1 and rng.random() < 0.7:  # the stinging hairs
                put(img, x + rng.choice((-1, 1)), y, NET["h"])
        # opposite leaf pairs every 3 px, smaller towards the top, the pairs turned a little each node
        for i, t in enumerate(range(2, h, 3 if young else 4)):
            x, y = x0 + round(lean * t), 15 - t
            left = h - t
            size = 4 if left > 9 else 3 if left > 4 else 2
            if young:
                size = min(size, 3)
            leaf(img, x, y, size, -1, rng)
            leaf(img, x, y, size, 1, rng)
            # grown plants hang green seed catkins from the upper leaf axils
            if not young and 3 < left <= 9:
                for side in (-1, 1):
                    if rng.random() < 0.7:
                        for k in range(rng.randint(2, 3)):
                            put(img, x + side * (2 + k // 2), y + 2 + k, NET["c" if k % 2 == 0 else "C"])
        put(img, x0 + round(lean * h), top, NET["E"])
        put(img, x0 + round(lean * (h - 1)) - 1, top + 1, NET["L"])
        put(img, x0 + round(lean * (h - 1)) + 1, top + 1, NET["L"])
    return img


# ============================== Flax ==============================
FLAX = dict(g="5f8a3c", G="476e2c", l="7aa64e", B="6c8ee0", b="a2bcf4", w="dfe8fb", y="e8d36c",
            Y="cdb25e", T="b0924a", t="8e7238", o="c49a52", O="8c6830", k="6a4c22")


def flax(stage):
    rng = random.Random(f"flax{stage}")
    img = new()
    if stage == 0:
        for x in (2, 6, 9, 13):
            put(img, x, 15, FLAX["G"])
            put(img, x, 14, FLAX["g"])
            put(img, x + rng.choice((-1, 1)), 13, FLAX["l"])
        return img
    ripe = stage == 4
    h = {1: 5, 2: 9, 3: 12, 4: 13}[stage]
    xs = [1, 3, 5, 7, 9, 11, 13, 15] if stage >= 2 else [2, 5, 8, 11, 14]
    for x0 in xs:
        hh = h - rng.randint(0, 2)
        lean = rng.choice((-0.12, 0, 0.1))
        for t in range(hh):
            x, y = x0 + round(lean * t), 15 - t
            if ripe:
                put(img, x, y, FLAX["T"] if t < 3 else FLAX["Y"] if rng.random() < 0.7 else FLAX["T"])
            else:
                put(img, x, y, FLAX["G"] if t < 2 else FLAX["g"])
                # narrow alternate leaves hugging the stem
                if t >= 2 and t % 2 == 0 and t < hh - 1:
                    put(img, x + (1 if (t // 2) % 2 else -1), y, rng.choice((FLAX["l"], FLAX["g"])))
        tx, ty = x0 + round(lean * (hh - 1)), 16 - hh
        if stage == 3 and rng.random() < 0.8:  # five sky-blue petals round a yellow eye
            for dx, dy, c in ((0, -1, "b"), (-1, 0, "B"), (1, 0, "B"), (0, 0, "y"), (-1, -1, "w"), (1, -1, "b")):
                put(img, tx + dx, ty + dy - 1, FLAX[c])
        elif ripe:  # round seed bolls on short branches
            for dx, dy in ((0, -1), (-1, 1), (1, 2)):
                put(img, tx + dx, ty + dy, FLAX["o"])
                put(img, tx + dx, ty + dy + 1 if dy < 0 else ty + dy, FLAX["O"])
            put(img, tx, ty, FLAX["k"])
    return img


# ============================== stalks, fibre ==============================


def bundle(stem_cols, tip, rng, leaf_col=None, twine=("c8b48a", "9c8a64")):
    """Three stems from the lower left to the upper right, tied with a twine band."""
    img = new()
    for (ox, oy), col, n in zip(((1, 0), (0, 0), (-1, 0)), stem_cols, (11, 12, 10)):
        for t in range(n):
            x, y = 2 + t + ox, 13 - t + oy
            put(img, x, y, col)
            if leaf_col and t % 3 == 2 and rng.random() < 0.6:  # stubs where the leaves were stripped
                put(img, x + ox + (ox == 0), y - (ox >= 0) + (ox < 0) * 2, leaf_col)
        tip(img, 2 + n - 1 + ox, 13 - n + 1 + oy)
    put(img, 1, 14, stem_cols[2])  # cut ends
    put(img, 2, 14, stem_cols[1])
    for t in (4, 5):  # the twine band
        x, y = 2 + t, 13 - t
        for dx, dy in ((1, 0), (0, 0), (0, 1), (-1, 0), (1, 1)):
            put(img, x + dx, y + dy, twine[(t + dx) % 2])
    return img


def nettle_stalks():
    rng = random.Random("nettle_stalks")

    def tip(img, x, y):
        put(img, x, y - 1, NET["L"])
        put(img, x + 1, y, NET["E"])
    return bundle((NET["L"], NET["M"], NET["D"]), tip, rng, NET["E"])


def flax_stalks():
    rng = random.Random("flax_stalks")

    def tip(img, x, y):  # a seed boll on every stem
        put(img, x, y - 1, FLAX["o"])
        put(img, x + 1, y - 1, FLAX["O"])
        put(img, x + 1, y, FLAX["k"])
    return bundle((FLAX["Y"], FLAX["T"], FLAX["t"]), tip, rng)


def hank(cols, specks, seed):
    """A hank of fibre: a band of fine strands lying diagonally, the ends fanning out loose."""
    rng = random.Random(seed)
    img = new()
    for k, col in zip((-2, -1, 0, 1, 2), cols):
        start, end = rng.randint(0, 2), 12 - rng.randint(0, 2)
        for t in range(start, end + 1):
            fan = k * (max(0, 2 - t) + max(0, t - 10))  # loose ends spread
            c = rng.choice(specks) if rng.random() < 0.12 else col
            put(img, 2 + t + k + fan, 13 - t, c)
    return img


# ============================== seeds ==============================
FLAX_SEEDS = [
    "................",
    "................",
    "......oO........",
    ".....hOOd...oO..",
    ".....oOOd..hOOd.",
    "......dd...oOOd.",
    "...oO.......dd..",
    "..hOOd..oO......",
    "..oOOd.hOOd.....",
    "...dd..oOOd..oO.",
    "........dd..hOOd",
    "..oO........oOOd",
    ".hOOd...oO...dd.",
    ".oOOd..hOOd.....",
    "..dd...oOOd.....",
    "........dd......",
]
FLAX_SEED_PAL = dict(h="e2b878", o="b47838", O="935a26", d="5c3414")

NETTLE_SEEDS = [
    "................",
    "................",
    "........sS......",
    "....sS...d..sS..",
    ".....d..sS...d..",
    "..sS.....d......",
    "...d..sS....sS..",
    "......Sd.....d..",
    "..sS......sS....",
    "...d.sS....d.sS.",
    ".....Sd.......d.",
    "...sS...sS......",
    "....d....d..sS..",
    "........sS...d..",
    ".........d......",
    "................",
]
NETTLE_SEED_PAL = dict(s="aaa274", S="7e764a", d="4e4a2c")

# ============================== Drop Spindle ==============================
WOOD, WOOD_D, WOOD_L = "8a6440", "64462a", "a8825a"
STONE, STONE_D, STONE_L = "8c8c88", "5e5e5a", "b2b2ac"
YARN, YARN_D = "e2dcc4", "b8b096"


def drop_spindle():
    img = new()
    for t in range(13):  # the shaft, lower left to upper right
        x, y = 1 + t, 14 - t
        put(img, x, y, WOOD if t % 4 else WOOD_D)
        if 2 < t < 12:
            put(img, x + 1, y, WOOD_L if t % 3 else WOOD)
    put(img, 14, 0, WOOD_D)  # the notch the yarn hooks into
    put(img, 15, 1, WOOD_D)
    # the cop of spun yarn wound on the shaft: fat in the middle, tapering to both ends
    for x in range(16):
        for y in range(16):
            u, v = (x - 8) - (y - 7), (x - 8) + (y - 7)
            if abs(u) <= 5 and abs(v) <= (2 if abs(u) <= 2 else 1):
                put(img, x, y, YARN_D if v >= 2 or (u + v) % 4 == 0 else YARN)
    # the whorl: a stone disc across the shaft near the bottom, seen at a slant
    for x in range(16):
        for y in range(16):
            u, v = (x - 4) - (y - 11), (x - 4) + (y - 11)
            if abs(u) <= 1 and abs(v) <= 5:
                put(img, x, y, STONE_D if abs(v) >= 5 or u == 1 else STONE_L if v < -1 else STONE)
    put(img, 4, 11, WOOD_D)  # the shaft through the hole
    # the yarn leading off the top
    for x, y in ((15, 2), (15, 3), (14, 4)):
        put(img, x, y, YARN)
    return img


# ============================== threads: the mod's Wool Thread recoloured ==============================


def recolour(src, base):
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    px = [src.getpixel((x, y)) for y in range(16) for x in range(16)]
    mx = max(lum(c) for c in px if c[3])
    out = new()
    for y in range(16):
        for x in range(16):
            c = src.getpixel((x, y))
            if c[3]:
                f = 0.6 + 0.4 * lum(c) / mx
                out.putpixel((x, y), tuple(min(255, round(v * f)) for v in base) + (c[3],))
    return out


def build():
    wool = Image.open(TEX / "item/wool_thread.png").convert("RGBA")
    out = {
        "block/stinging_nettle": nettle(False),
        "block/stinging_nettle_young": nettle(True),
        "item/nettle_seeds": grid(NETTLE_SEEDS, NETTLE_SEED_PAL),
        "item/flax_seeds": grid(FLAX_SEEDS, FLAX_SEED_PAL),
        "item/nettle_stalks": nettle_stalks(),
        "item/flax_stalks": flax_stalks(),
        "item/nettle_fiber": hank(("6e7452", "9aa07a", "b6bc96", "a4aa84", "868c64"), ("5a5e3e", "c4c8a8"), "nettle_fiber"),
        "item/flax_fiber": hank(("a8987a", "d6c8a4", "ece2c4", "e0d4b2", "c0b08c"), ("8c7c5c", "f6f0dc"), "flax_fiber"),
        "item/nettle_thread": recolour(wool, (168, 172, 128)),
        "item/linen_thread": recolour(wool, (240, 230, 196)),
        "item/sinew_thread": recolour(wool, (222, 176, 118)),
        "item/drop_spindle": drop_spindle(),
    }
    for s in range(5):
        out[f"block/flax_stage{s}"] = flax(s)
    for name, img in out.items():
        path = TEX / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    return out


def preview(images, dst):
    sheet = Image.new("RGBA", (len(images) * 136, 136), (120, 160, 210, 255))
    for i, img in enumerate(images.values()):
        sheet.paste(img.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4), img.resize((128, 128), Image.NEAREST))
    sheet.save(dst)


if __name__ == "__main__":
    imgs = build()
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
    print(f"thread textures ok ({len(imgs)})")
