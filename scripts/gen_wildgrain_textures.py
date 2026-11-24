"""Textures for the wild grain stands (gen_wildgrain.py; WildGrainBlock stages young, heading, ripe, shattered; two drawn
variants a/b each), the Oats crop (stage0..4) and its items, and Oatcakes. Wild plants are thin and short beside the
crops, with few stems and nodding ears, each kind its own: Einkorn a slim zigzag ear with sparse awns, reddish brown;
Emmer a broad dense ear with long near-black awns; Barley a narrow ear with a long brush of awns, pale straw, hanging
when ripe; Oats an open panicle of dangling spikelets with dark twisted awns; Rice tall (32 px, it stands in water) with
a drooping panicle and long red awns. Young plants come up among last year's grey straw; shattered ears are empty
stubs on bleached, broken stalks. Standalone: python scripts/gen_wildgrain_textures.py [preview.png]"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


class Canvas:
    def __init__(self, w=16, h=16):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.w, self.h = w, h

    def put(self, x, y, c, over=True):
        x, y = int(round(x)), int(round(y))
        if 0 <= x < self.w and 0 <= y < self.h and (over or self.img.getpixel((x, y))[3] == 0):
            self.img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)

    def get(self, x, y):
        return self.img.getpixel((x, y))[3] if 0 <= x < self.w and 0 <= y < self.h else 0


# ----------------------------------------------------------------- palettes
YOUNG = ("9fd166", "7bb54a", "5c9638", "416f2a")          # fresh blades, light to dark
DEAD = ("d8ceb0", "b8ab88", "958a6a", "736a52")            # last year's grey straw
HEAD_STALK = ("8cbc52", "6c9e3e", "4f7c30")
HEAD_EAR = ("b4d47a", "8fb658", "678e3c")
BASE_RIPE = ("b9c068", "98a24c", "77803a", "5d6430")       # drying lower leaves
BLEACH = ("d6ccae", "bbae8e", "9a8e70", "7b7058")          # shattered straw
KINDS = {
    #          stalk ripe                  ear ripe light..dark          awn ripe   awn heading  height, stems
    "einkorn": dict(stalk=("d8c27e", "b49856", "886c3a"), ear=("d8b46c", "b98e4a", "876234"), awn="c9a46e", gawn="b8d488", h=(5, 9), n=5),
    "emmer": dict(stalk=("dcbf6c", "b89a4a", "8a7034"), ear=("ecc46a", "c9963f", "94662a"), awn="5e4228", gawn="7e9a50", h=(5, 8), n=4),
    "barley": dict(stalk=("e0c97e", "c2a65c", "94793e"), ear=("f2dc94", "d8bb6a", "a88a4a"), awn="ead39a", gawn="cfe6a4", h=(5, 9), n=5),
    "oats": dict(stalk=("dccb8c", "b8a466", "8c7a46"), ear=("efe6c4", "d2c496", "a89a6c"), awn="5a4630", gawn="6a8a3c", h=(6, 9), n=4),
    "rice": dict(stalk=("c8b46a", "a08c48", "766634"), ear=("dcbc6c", "b8924a", "866434"), awn="b04a2a", gawn="9cbc5c", h=(19, 24), n=5),
}


def curve(x0, h, lean, bottom=15):
    """Pixels of a stalk from (x0, bottom) up h px, bending by lean (px at the top) more and more towards the tip."""
    pts = []
    for i in range(h):
        t = i / max(1, h - 1)
        pts.append((x0 + lean * t * t, bottom - i))
    return pts


def stalk(c, pts, cols, broken_at=None, droop=1):
    """Draws a stalk (dark at the foot, light near the top); broken_at: the part above that index flops over sideways."""
    out = []
    n = len(pts)
    for i, (x, y) in enumerate(pts):
        if broken_at is not None and i > broken_at:
            j = i - broken_at
            bx, by = pts[broken_at]
            x, y = bx + droop * j, by - (1 if j < 2 else 0) + max(0, j - 3) // 2
        col = cols[2] if i < n * 0.3 else cols[1] if i < n * 0.75 else cols[0]
        c.put(x, y, col)
        out.append((round(x), round(y)))
    return out


def blade(c, x, y, n, d, cols, arch=0.0):
    """A leaf blade from (x, y) up n px, drifting sideways (d px per 2 rows), the tip bowing over by arch."""
    for i in range(n):
        t = i / max(1, n - 1)
        xx = x + d * (i / 2) + arch * t * t * (1 if d >= 0 else -1)
        yy = y - i + (arch * 0.6 * t * t if arch else 0)
        c.put(xx, yy, cols[3] if i == 0 else cols[2] if i < n * 0.35 else cols[1] if i < n * 0.7 else cols[0])


def leaf_flag(c, x, y, side, cols, length=4):
    """A leaf leaving the stalk at (x, y): up and out, then arching over and down at the tip."""
    path = [(1, -1), (2, -1), (3, -1), (4, 0), (5, 1), (6, 2)][:length]
    for i, (dx, dy) in enumerate(path):
        c.put(x + side * dx, y + dy, cols[2] if i == 0 else cols[1] if i < length - 1 else cols[0])


# ----------------------------------------------------------------- ears: hand-drawn, nodding to the right (mirrored for the
# left). The last row marks with '+' the pixel the ear sits on (the stalk top); it is not drawn. l/m/d: ear light, mid, dark;
# w: awn; s: stalk; g: glume (papery, after shattering)
EARS = {
    ("einkorn", "heading"): [
        "w...w",
        ".w.w.",
        "..l..",
        ".dm..",
        "..ml.",
        ".dm..",
        "..+..",
    ],
    ("einkorn", "ripe"): [
        "...w..w",
        "....ww.",
        "...lm..",
        "..dml..",
        "..lm...",
        ".dmd...",
        ".lm....",
        ".+.....",
    ],
    ("emmer", "heading"): [
        "w.w.w",
        ".www.",
        ".lmd.",
        ".lmd.",
        ".lmd.",
        "..md.",
        "..+..",
    ],
    ("emmer", "ripe"): [
        ".w..w.w",
        "..w.ww.",
        "w..www.",
        ".w.lmd.",
        "..lmd..",
        "..lmd..",
        ".lmd...",
        ".+.....",
    ],
    ("barley", "heading"): [
        "w.w.w",
        "w.w.w",
        ".wlw.",
        "..md.",
        "..lm.",
        "..md.",
        "..+..",
    ],
    ("barley", "ripe"): [
        "..ss...",
        ".s..l..",
        ".s..ml.",
        ".s..dmw",
        ".s..wdw",
        ".+..w.w",
    ],
    ("oats", "heading"): [
        "..l..",
        ".lml.",
        "l.s.l",
        "m.s.m",
        ".lsl.",
        ".m.m.",
        "..+..",
    ],
    ("oats", "ripe"): [
        "...s...",
        ".l.s...",
        ".m.s.l.",
        ".w.sssm",
        "..ss..w",
        "lss.l..",
        "m...m..",
        "w..s.w.",
        "...+...",
    ],
    ("rice", "heading"): [
        "..mlm..",
        ".s...l.",
        "s.....m",
        "s......",
        "+......",
    ],
    ("rice", "ripe"): [
        "..mlml.",
        ".s....m",
        "s.....d",
        "s....wl",
        "s....wd",
        "+.....w",
    ],
    # after shattering: bare grey stubs, a last awn or an empty glume
    ("einkorn", "shattered"): [
        "..w",
        ".g.",
        ".g.",
        ".+.",
    ],
    ("emmer", "shattered"): [
        "..k.",
        ".k..",
        ".g..",
        ".g..",
        ".+..",
    ],
    ("barley", "shattered"): [
        "g..",
        ".g.",
        ".+.",
    ],
    ("oats", "shattered"): [
        "..s..",
        "g.s..",
        "g.s.g",
        ".ss.g",
        "..s..",
        "..+..",
    ],
    ("rice", "shattered"): [
        "..gg..",
        ".s..g.",
        "s....g",
        "+.....",
    ],
}


def ear(c, kind, x, y, d, stage, rng):
    k = KINDS[kind]
    ripe = stage == "ripe"
    rows = EARS[(kind, stage)]
    ax, ay = rows[-1].index("+"), len(rows) - 1
    if stage == "shattered":
        pal = dict(g=BLEACH[1], s=BLEACH[2], w=BLEACH[0], k="5e4836")
    else:
        l, m, dk = k["ear"] if ripe else HEAD_EAR
        pal = dict(l=l, m=m, d=dk, w=k["awn"] if ripe else k["gawn"], s=(k["stalk"] if ripe else HEAD_STALK)[0])
    for ry, row in enumerate(rows):
        for rx, ch in enumerate(row):
            if ch in ".+":
                continue
            if stage == "shattered" and ch in "wgk" and rng.random() < 0.25:   # some of what is left has gone too
                continue
            c.put(x + d * (rx - ax), y + (ry - ay), pal[ch])


# ----------------------------------------------------------------- one plant
def wild(kind, stage, variant):
    k = KINDS[kind]
    rng = random.Random(f"{kind}/{variant}")    # the same stems and spots through every stage of a variant
    tall = kind == "rice"
    W, H = 16, (32 if tall else 16)
    c = Canvas(W, H)
    bottom = H - 1
    stems = []
    xs = [2, 4, 7, 9, 12, 13][:k["n"] + 1]
    rng.shuffle(xs)
    for i in range(k["n"]):
        x0 = min(13, xs[i] + rng.choice((0, 1)))
        lean = rng.choice((-3, -2, -1, 1, 2, 3)) * (1.4 if tall else 1)
        if x0 < 5 and lean < 0 or x0 > 10 and lean > 0:
            lean = -lean
        stems.append(dict(x=x0, h=rng.randint(*k["h"]), lean=lean, d=1 if lean > 0 else -1,
                          leaf=rng.randint(1, 3) * (3 if tall else 1), side=rng.choice((-1, 1)),
                          broken=rng.random() < 0.4, short=rng.randint(0, 2), leafy=tall or rng.random() < 0.5))
    blades = [(rng.randint(1, 14), rng.randint(4, 8) * (2 if tall else 1), rng.choice((-1, 0, 0, 1)), rng.random()) for _ in range(8 if tall else 7)]
    deads = [(rng.randint(2, 13), rng.randint(7, 11), rng.choice((-1, 1))) for _ in range(2)]

    if tall:  # rice: long arching leaves from low on the culms (the water line comes about half way up)
        leaf = YOUNG if stage in ("young", "heading") else BASE_RIPE if stage == "ripe" else BLEACH
        for bx, n, d, r in blades:
            blade(c, bx, bottom, n + (6 if stage != "young" else 0), d or (1 if bx < 8 else -1), leaf, arch=1.5 + r * 2.5)
        if stage == "young":
            return c.img

    if stage == "young":
        for dx, h, side in deads:      # last year's straw still standing, bent over at the top, an empty ear on one
            got = stalk(c, curve(dx, h, side, bottom), DEAD[:3], broken_at=h - 3, droop=side)
            if dx % 2:
                tx, ty = got[-1]
                c.put(tx + side, ty + 1, DEAD[0])
        for bx, n, d, r in blades:     # a tuft of fresh blades in front, the stems to come already showing as shoots
            blade(c, bx, bottom, n, d, YOUNG, arch=r * 1.5)
            blade(c, bx + (1 if bx < 14 else -1), bottom, n - 2 - int(r * 2), 1 if d <= 0 else -1, YOUNG, arch=r)
        for s in stems:
            blade(c, s["x"], bottom, s["h"] - 1, 1 if s["lean"] > 0 else -1 if s["lean"] < -1.5 else 0, YOUNG, arch=0.5)
        return c.img

    # grass at the foot: fresh with green ears, yellowing once ripe, bleached after
    base = YOUNG if stage == "heading" else BASE_RIPE if stage == "ripe" else BLEACH
    if not tall:
        for bx, n, d, r in blades[:5]:
            blade(c, bx, bottom, max(2, n - 3), d, base, arch=r)

    for s in stems:
        h = s["h"] - (1 if stage == "heading" else 0) - s["short"]
        up = len(EARS[(kind, stage)]) - 1   # room for the ear above the stalk
        h = min(h, bottom - up)
        cols = HEAD_STALK if stage == "heading" else k["stalk"] if stage == "ripe" else BLEACH[:3]
        pts = curve(s["x"], h, s["lean"], bottom)
        broken = s["broken"] and stage == "shattered"
        got = stalk(c, pts, cols, broken_at=(h * 2) // 3 if broken else None, droop=s["d"])
        if s["leafy"]:
            lx, ly = got[min(len(got) - 1, s["leaf"])]
            leaf_flag(c, lx, ly, s["side"], HEAD_STALK if stage == "heading" else base, 3 if not tall else 6)
        if broken:   # the top lies over and its ear is gone
            continue
        tx, ty = got[-1]
        ear(c, kind, tx, ty, s["d"], stage, rng)
    assert not any(c.img.getpixel((x, 0))[3] for x in range(W)), f"{kind} {stage} {variant} reaches the top edge"
    return c.img


# ----------------------------------------------------------------- the Oats crop (cross, 5 drawn stages) and items
def oat_crop(stage):
    rng = random.Random("oat_crop")
    c = Canvas()
    if stage == 0:
        for x in (3, 8, 12):
            blade(c, x, 15, 3, rng.choice((-1, 1)), YOUNG)
        return c.img
    if stage <= 2:
        n = (4, 5) if stage == 1 else (7, 9)
        for x in (1, 4, 7, 10, 13):
            blade(c, x, 15, n[1] - rng.randint(0, 2), 0, YOUNG)
            blade(c, x - 1, 15, n[0], -1, YOUNG)
            blade(c, x + 1, 15, n[0] - 1, 1, YOUNG)
        return c.img
    ripe = stage == 4
    k = KINDS["oats"]
    for x in (1, 4, 7, 10, 13):   # a fuller, taller plant than the wild one: 5 stems, heavy panicles
        blade(c, x, 15, 4, rng.choice((-1, 1)), BASE_RIPE if ripe else YOUNG)
    for i, x in enumerate((2, 5, 8, 11, 14)):
        h = 14 - (i % 2) - (0 if ripe else 1)
        pts = curve(x, h, (-1 if i % 2 else 1), 15)
        got = stalk(c, pts, k["stalk"] if ripe else HEAD_STALK)
        tx, ty = got[-1]
        ear(c, "oats", tx, ty, -1 if i % 2 else 1, "ripe" if ripe else "heading", rng)
    return c.img


def grid(rows, pal):
    c = Canvas()
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c.put(x, y, pal[ch])
    return c.img


OAT_GRAIN = [  # husked oat kernels: long, slim, pale cream with a dark crease
    "................",
    "................",
    "....ab..........",
    "...abc.....ab...",
    "...abc....abc...",
    "....bcd...abc...",
    ".....d.....bcd..",
    ".......ab...d...",
    "......abc.......",
    "..ab..abc....ab.",
    ".abc...bcd..abc.",
    ".abc....d...abc.",
    "..bcd........bcd",
    "...d..........d.",
    "................",
    "................",
]
OAT_SHEAF = [
    "...e..o...e.o...",
    "..a.o.a..o.a..o.",
    "...b.e.b.e..b...",
    ".o.a..a.ao.a.e..",
    "..e.b.ab.b.b.o..",
    "....a.sab.a.....",
    ".....bsSsb......",
    "......sSs.......",
    ".....sSsS.......",
    "....rRsS........",
    "...sRrS.........",
    "..sSsR..........",
    ".sStS...........",
    ".tSt............",
    "..t.............",
    "................",
]
OATCAKES = [  # three round griddle cakes, stacked and leaning, cracked edges, toasted spots
    "................",
    "................",
    "................",
    ".......aaaa.....",
    ".....aabbbbaa...",
    "....abbcbbbcba..",
    "....abbbbcbbba..",
    "...aabcbbbbbcaa.",
    "..abbbbbbcbbbbd.",
    ".abcbbbcbbbbcbd.",
    ".abbbbbbbbcbbbd.",
    ".dabbcbbbbbbbdd.",
    "..ddabbbbcbadd..",
    "....ddddddddd...",
    "................",
    "................",
]


def build():
    out = {}
    for kind in KINDS:
        for st in ("young", "heading", "ripe", "shattered"):
            if kind == "rice":   # 32x32: variant a on the left half, b on the right
                sheet = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
                sheet.alpha_composite(wild(kind, st, "a"), (0, 0))
                sheet.alpha_composite(wild(kind, st, "b"), (16, 0))
                out[f"block/wild_rice_{st}"] = sheet
            else:
                for v in "ab":
                    out[f"block/wild_{kind}_{st}_{v}"] = wild(kind, st, v)
    for s in range(5):
        out[f"block/oat_stage{s}"] = oat_crop(s)
    out["item/oat_grain"] = grid(OAT_GRAIN, dict(a="f2ead0", b="d8c89a", c="b4a274", d="7c6c48"))
    out["item/oat_sheaf"] = grid(OAT_SHEAF, dict(a="efe6c4", b="d2c496", e="a89a6c", o="5a4630", s="dccb8c", S="b8a466",
                                                 t="8c7a46", r="9a7a58", R="6a5038"))
    out["item/oatcakes"] = grid(OATCAKES, dict(a="e8cf98", b="d6b276", c="9c6e3c", d="8c6638"))
    return out


def preview(images, dst):
    s, pad, cols = 6, 6, 8
    keys = list(images)
    cell = 32 * s + pad
    sheet = Image.new("RGBA", (cols * cell + pad, ((len(keys) + cols - 1) // cols) * cell + pad), (128, 160, 120, 255))
    for i, key in enumerate(keys):
        im = images[key]
        sheet.alpha_composite(im.resize((im.width * s, im.height * s), Image.NEAREST), (pad + (i % cols) * cell, pad + (i // cols) * cell))
    sheet.save(dst)


if __name__ == "__main__":
    imgs = build()
    for rel, img in imgs.items():
        img.save(TEX / f"{rel}.png")
    print(f"wrote {len(imgs)} textures")
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
