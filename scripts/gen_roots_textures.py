# Root crop textures (gen_roots.py): Wild Carrot Plant and Sea Beet (cross), the Wild Carrot crop (stage0..3), Wild Carrot
# and Sea Beet Leaves items, and the Root Cellar (plank bin, lid, straw with roots inside). Plants and items are hand-drawn
# pixel grids; the crop fronds and the planks are painted from fixed seeds. Colours baked in, like vanilla crops.
# usage: python scripts/gen_roots_textures.py [preview.png]
import random
import sys
from pathlib import Path
from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
RNG = random.Random(7)


def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), hexc(c))


def grid(rows, pal, img=None):
    """Hand-drawn 16x16 grid; '.' is transparent; a palette tuple is a random pick (seeded)."""
    img = img or new()
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                put(img, x, y, RNG.choice(c) if isinstance(c, tuple) else c)
    return img


# ============================== wild plants ==============================
LEAF = dict(l="94b450", L="6e9438", D="4c6c28", g="7a9a44", G="56762e")
UMBEL = dict(W="f6f4ea", w="dedac6", s="b6b29c", p="4e2242")

WILD_CARROT_PLANT = [  # Queen Anne's lace: two flat white umbels (one with the dark centre floret) over fine feathery leaves
    "................",
    "..wWWWw.........",
    ".WWsWWsW........",
    "..sWpWs...wWWw..",
    "...g.g...wWsWWW.",
    "....g.....sWWWs.",
    "....g......g.g..",
    "....g.......g...",
    "..l.g..l....g...",
    ".lLlg.lLl..lgl..",
    "..LDg.LD..lLgLl.",
    ".l.Lg.L.l.LDgD.l",
    "lLl.gL.lLlD.gL.L",
    ".DLlgDLl.LlLg.Ll",
    "..DLGLD.LD.DG.D.",
    "...DGD....D.G...",
]

SEA_BEET = [  # glossy leathery leaves in a low rosette, red-tinged stalks, two spikes of small green flowers
    "......f.........",
    ".....fFf........",
    "......f..f......",
    ".....fF.fFf.....",
    "......r..f......",
    "......r..r......",
    "..LH..r..r..HL..",
    ".LHML.r.r..LMHL.",
    ".MLMDLr.r.LDMLM.",
    "..MDMDrLrLDMDM..",
    "LHMDKMrMrMKDMHL.",
    ".LMMDKrDrKDMML..",
    "..DMLDRrRDLMD...",
    ".DMDMLRrRLMDMD..",
    "..KDMDRRRDMDK...",
    "....KDRRRDK.....",
]
BEET = dict(K="2a4620", D="3b622e", M="4e7c38", L="6c9c4a", H="a2c87e", r="9a3444", R="6a2230", f="c0c862", F="8c9a3a")

# ============================== the wild carrot crop ==============================


def frond(img, x0, h, lean, rng, dark_base=True):
    """A finely divided carrot leaf: a thin stem from the ground with tiny alternate leaflets."""
    for t in range(h):
        x, y = x0 + round(lean * t), 15 - t
        put(img, x, y, LEAF["G"] if (t < 2 and dark_base) else LEAF["g"])
        if t >= 2 and t % 2 == 0:
            side = 1 if (t // 2) % 2 else -1
            put(img, x + side, y, rng.choice((LEAF["L"], LEAF["l"])))
            if t < h - 2 and rng.random() < 0.6:
                put(img, x + 2 * side, y - 1, rng.choice((LEAF["L"], LEAF["D"], LEAF["l"])))
    put(img, x0 + round(lean * (h - 1)), 15 - h, LEAF["l"])


def umbel(img, cx, y):
    for dx, c in ((-2, "w"), (-1, "W"), (0, "W"), (1, "W"), (2, "w")):
        put(img, cx + dx, y, UMBEL[c])
    for dx, c in ((-1, "s"), (0, "W"), (1, "s")):
        put(img, cx + dx, y - 1, UMBEL[c])
    put(img, cx, y + 1, LEAF["g"])


def crop(stage):
    rng = random.Random(f"wild_carrot{stage}")
    img = new()
    if stage == 0:
        for x in (3, 8, 12):
            put(img, x, 15, LEAF["G"])
            put(img, x, 14, LEAF["g"])
            put(img, x - 1, 13, LEAF["l"])
            put(img, x + 1, 13, LEAF["L"])
        return img
    h, n = {1: (5, 5), 2: (8, 6), 3: (10, 7)}[stage]
    xs = [1, 4, 6, 9, 11, 13, 15][:n] if n == 7 else [2, 5, 8, 11, 14, 7][:n]
    for i, x in enumerate(xs):
        frond(img, x, h - rng.randint(0, 2), rng.choice((-0.25, 0, 0.2, 0.3)) * (1 if i % 2 else -1), rng)
    if stage == 3:
        # the pale shoulder of the root shows at the ground, and a few plants have bolted into white umbels
        for x in (4, 10):
            put(img, x, 15, "d8cca0")
            put(img, x + 1, 15, "bfae80")
        for cx, y in ((3, 3), (11, 2)):
            umbel(img, cx, y)
            for yy in range(y + 2, 9):
                put(img, cx, yy, LEAF["g"])
    return img


# ============================== items ==============================
WILD_CARROT_ITEM = [  # thin, pale, a little twisted, with root hairs and a feathery top
    "..........l.l...",
    ".........lLlL.l.",
    "........l.LgLl..",
    ".......lLlgLl.l.",
    "........lLggl...",
    "..........Cc....",
    ".........Ccd....",
    "........hCcd....",
    "........Ccd.....",
    ".......Ccd......",
    "......Ccdh......",
    ".....Ccd........",
    "....hcd.........",
    "....cd..........",
    "...cd...........",
    "..d.............",
]
WILD_CARROT_PAL = dict(l=LEAF["l"], L=LEAF["L"], g=LEAF["G"], c="ece2c0", C="d4c494", d="a8946a", h="bba882")

SEA_BEET_LEAVES = [
    "................",
    ".........DMMD...",
    "........DMLHMD..",
    ".......DMLHLMMD.",
    "..DMMD.DMLrLMMD.",
    ".DMLHMDDMrLMMD..",
    "DMLHLMMDMrMMD...",
    "DMLrLMMDrMMD....",
    "DMMLrMMDrDD.....",
    ".DMMLrMDr.......",
    "..DMMMrDr.......",
    "...DDDrr........",
    "......rR........",
    ".....rR.........",
    "....RR..........",
    "................",
]

# ============================== Root Cellar ==============================
WOOD = ["8a6a42", "7c5e3a", "6e5232", "9a7a4e"]
GAP, NAIL = "3e2c1a", "4a4a46"
STRAW = ["e0c470", "c9aa52", "b08f3e", "eed88a"]


def planks(vertical, seed, boards=4):
    rng = random.Random(seed)
    img = new()
    w = 16 // boards
    for b in range(boards):
        base = rng.randrange(len(WOOD))
        for i in range(w):
            for j in range(16):
                x, y = (b * w + i, j) if vertical else (j, b * w + i)
                c = WOOD[base] if rng.random() > 0.18 else WOOD[(base + rng.choice((1, -1))) % len(WOOD)]
                if i == w - 1:
                    c = GAP
                put(img, x, y, c)
    return img


def cellar_side():
    img = planks(True, "cellar_side")
    for x in range(1, 15):  # straw lining sticks out over the rim (rows 4-5 are the top of the 12-high body)
        if x % 3 != 0:
            put(img, x, 4, random.Random(x).choice(STRAW))
        if x % 4 == 1:
            put(img, x, 5, STRAW[2])
    for x in (2, 13):  # pegs holding the boards to the corner posts
        put(img, x, 7, NAIL)
        put(img, x, 13, NAIL)
    return img


def cellar_lid():
    img = planks(False, "cellar_lid")
    for y in range(16):  # two battens across the boards
        for x in (3, 12):
            put(img, x, y, "5e4428" if y % 4 else "4a3420")
    for x, y in ((6, 7), (7, 6), (8, 6), (9, 7), (6, 8), (9, 8)):  # twine handle loop
        put(img, x, y, "c8b48a")
    put(img, 7, 7, "8a7a5a")
    put(img, 8, 7, "8a7a5a")
    return img


def cellar_inside():
    rng = random.Random("cellar_inside")
    img = new()
    for x in range(16):
        for y in range(16):
            put(img, x, y, STRAW[1] if rng.random() < 0.5 else STRAW[2])
    for _ in range(26):  # loose straw strands lying every which way
        x, y, dx = rng.randrange(16), rng.randrange(16), rng.choice((1, -1))
        for i in range(rng.randint(2, 4)):
            put(img, x + i * dx, y + (i // 2 if rng.random() < 0.5 else 0), rng.choice((STRAW[0], STRAW[3])))
    carrot = ["..gl", ".gg.", "oO..", "Oo.."]
    beet = [".k..", "rRr.", "RrR.", ".R.."]
    potato = [".pP.", "pPPq", "Ppqq", ".qq."]
    pal = dict(g=LEAF["L"], l=LEAF["l"], o="e47c26", O="c0601a", k=LEAF["D"], r="8e2a46", R="62182e", p="d0ae74", P="b89660", q="96784a")
    for rows, ox, oy in ((carrot, 1, 1), (beet, 10, 2), (potato, 5, 9), (potato, 11, 11), (carrot, 1, 11), (beet, 7, 4)):
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    put(img, ox + x, oy + y, pal[ch])
    return img


def build():
    out = {
        "block/wild_carrot_plant": grid(WILD_CARROT_PLANT, {**LEAF, **UMBEL}),
        "block/sea_beet": grid(SEA_BEET, BEET),
        "item/wild_carrot": grid(WILD_CARROT_ITEM, WILD_CARROT_PAL),
        "item/sea_beet_leaves": grid(SEA_BEET_LEAVES, dict(D=BEET["D"], M=BEET["M"], L=BEET["L"], H=BEET["H"], r=BEET["r"], R=BEET["R"])),
        "block/root_cellar_side": cellar_side(),
        "block/root_cellar_bottom": planks(False, "cellar_bottom"),
        "block/root_cellar_lid": cellar_lid(),
        "block/root_cellar_lid_side": planks(False, "cellar_lid_side", boards=8),
        "block/root_cellar_inside": cellar_inside(),
    }
    for s in range(4):
        out[f"block/wild_carrot_stage{s}"] = crop(s)
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
    print(f"roots textures ok ({len(imgs)})")
