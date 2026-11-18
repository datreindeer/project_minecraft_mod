# Wild berry bush block textures (stage0..3, cross model like vanilla sweet_berry_bush) and berry item textures.
# Each plant: a small stage0 grid, one full-grown grid whose fruit letters are recoloured per stage
# (stage1 flowers, stage2 unripe, stage3 ripe), and an item grid. Colours baked in, like vanilla's bush.
# usage: python scripts/gen_berry_textures.py [preview_dir]
import random
import sys
from pathlib import Path
from PIL import Image

RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"

# Shared letters: L light leaf, g mid leaf (speckled between two mids), d dark leaf, D darkest,
# w leaf underside, s stem, S dark stem, f fruit stalk. Fruit letters (A B C E y) come from FRUIT[stage].
PLANTS = {
    "raspberry": dict(
        leaf={"L": "79ad4f", "g": ("5a923e", "4d8537"), "d": "35682c", "D": "234a22", "w": "aebfa0",
              "s": "8a4a36", "S": "5e2f24", "f": "4d7a2e"},
        fruit=[
            {"A": "f4f2e6", "B": "e2e0d0", "C": "d8c870", "E": "f4f2e6"},       # small white flowers
            {"A": "dfe6a8", "B": "bccb7c", "C": "8f9f52", "E": "eef2c8"},       # hard pale drupelets
            {"A": "ec5a6a", "B": "c42640", "C": "7e1229", "E": "ff9aa4"},       # ripe red
        ],
        stage0=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......Lg........",
            ".....gLgd.......",
            "....LgdsgL......",
            "....dw.sgdg.....",
            ".......s.wLg....",
            "...Lg..s.s.d....",
            "...gdg.s.s......",
            "....w.sS.s......",
            "......S..S......",
        ],
        grown=[
            "......Lg...gL...",
            "...LgLdg.LgdgL..",
            "..gdwgsgLgsdwg..",
            ".Lg..s.wd.s..gL.",
            "..dws..s..s.Lgd.",
            "..f.s.Lsg.sgdw..",
            ".AEBs.gdsf.s.f..",
            ".BBC.s.wsAEBsAEB",
            "..C.Lg.s.BBC.BBC",
            ".LgLgds.s.CLg.C.",
            ".gdw.s..sLgdgL..",
            "..fLgsAEBgdw.gd.",
            "..AEBsBBC.s.f...",
            "..BBC.sC.s.AEB..",
            "...C..s..s.BBC..",
            "......S.S...C...",
        ],
        item=[
            "................",
            "................",
            ".........Lg.....",
            ".......LgLdg....",
            "........dwf.....",
            "....AEB..f......",
            "...ABABA.AEB....",
            "...BABCBABABA...",
            "...CBCBCBABCB...",
            "....CCC.CBCBC...",
            "......AEB.CC....",
            ".....ABABA......",
            ".....BABCB......",
            ".....CBCBC......",
            "......CCC.......",
            "................",
        ],
    ),
    "blueberry": dict(
        leaf={"L": "8cc456", "g": ("63a540", "58983a"), "d": "3f7a30", "D": "2a5726", "w": "6ea24a",
              "s": "5c7a34", "S": "40592a", "f": "6e7f3a"},
        fruit=[
            {"A": "f2c4c8", "B": "d68f9c", "C": "a85a6e", "E": "f2c4c8"},       # pink urn-shaped bells
            {"A": "cfe09a", "B": "a8c46c", "C": "8a8a48", "E": "dceab0"},       # green, blushing
            {"A": "7d8cc0", "B": "3e4683", "C": "23224f", "E": "aab4d8"},       # dusty blue-purple bloom
        ],
        stage0=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......gL........",
            "...Lg..s..Lg....",
            "....ds.s.s.d....",
            ".....s.sLs......",
            "...gL.s.ds.gL...",
            "....d.s.s.sd....",
            ".......SS.......",
        ],
        grown=[
            "................",
            "................",
            ".......Lg.......",
            "...Lg..s..gL....",
            "....ds.sLg.s.Lg.",
            ".Lg..sAEd.s.sd..",
            "..dLgsBCs.sLg...",
            "....dss..s.d.AE.",
            ".gL..sLg.sLgsBC.",
            "..dsAEd.s.ds.s..",
            "...sBC.Lgs.s.Lg.",
            ".Lg.s.s.dsAE.d..",
            "..ds.s.Lg.BCs...",
            ".AE.s.s.ds.s.gL.",
            ".BC..s.s.s.s.d..",
            ".....S.SS.......",
        ],
        item=[
            "................",
            "................",
            "................",
            "......AEB.......",
            ".....ABBBC..L...",
            ".....BBCBCgLd...",
            ".....CBBCC.d....",
            "..AEB.CCCAEB....",
            ".ABBBC..ABBBC...",
            ".BBCBC..BBCBC...",
            ".CBBCCAEBCBBC...",
            "..CCCABBBCCC....",
            ".....BBCBC......",
            ".....CBBCC......",
            "......CCC.......",
            "................",
        ],
    ),
    "lingonberry": dict(
        leaf={"L": "5f9455", "g": ("2f6233", "2a5a2e"), "d": "1d4424", "D": "13301a", "w": "86b67a",
              "s": "6e3c2c", "S": "4a271e", "f": "7a3a2a"},
        fruit=[
            {"A": "fbe4ea", "B": "eab0c0", "C": "c47e92", "E": "fbe4ea"},       # pale pink bells
            {"A": "f2f4dc", "B": "d6e0b4", "C": "aab88a", "E": "fafbea"},       # whitish green
            {"A": "f8483a", "B": "d0201e", "C": "8c1012", "E": "ffb4a0"},       # bright glossy red
        ],
        stage0=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......Lw........",
            "..Lw..gd.Lw.....",
            ".Lgd.s.s.gdLw...",
            "..gdLwsLwsLgd...",
            "...s.gdgds.s....",
        ],
        grown=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "....AE....AE....",
            "...AEBAE.ABAE...",
            "...BCBBC.BCBC...",
            "...LwCff..ffLw..",
            "..LgdLwsLwsLgd..",
            ".Lwgd.gdgdLwLw..",
            "Lgd.Lw.sLw.gdgd.",
            ".gdLgdLwgdLw.Lw.",
            "Lw.gd.gdLwgdLgd.",
            ".gdLw.s.gd.s.gd.",
            "...s..S.S.s.....",
        ],
        item=[
            "................",
            "................",
            "................",
            "..........Lw....",
            ".........Lgd....",
            "......f..gd.....",
            ".....AEf.f......",
            "....AEBCAE......",
            "....BCAEBCAE....",
            ".....CBCAEBC....",
            "....AEBBCBC.....",
            "....BCAEBC......",
            ".....CBC........",
            "................",
            "................",
            "................",
        ],
    ),
    "wild_strawberry": dict(
        leaf={"L": "7fc052", "g": ("58a03c", "4d9236"), "d": "377530", "D": "245524", "w": "93c96a",
              "s": "6a9a40", "S": "4a7030", "f": "8a6a3a"},
        fruit=[
            {"A": "fbfbf2", "B": "e8e8dc", "C": "f2d040", "E": "fbfbf2", "y": "fbfbf2"},  # white, yellow eye
            {"A": "eef4c8", "B": "c8dc94", "C": "a8bc6c", "E": "f6f8e0", "y": "e8e070"},  # pale green
            {"A": "f04a3c", "B": "c8221c", "C": "861412", "E": "ff9a84", "y": "f2d060"},  # red, yellow seeds
        ],
        stage0=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......LgL.......",
            "...LgLgdgLgL....",
            "...gdgds.gdgd...",
            "....d..s.s.d....",
            "......s.s.......",
            "......s.s.......",
            ".......SS.......",
        ],
        grown=[
            "................",
            "................",
            "................",
            "........LgL.....",
            "...LgL.LgdgL....",
            "..LgdgLgdsdgd...",
            "..gdsdgd.s.d....",
            ".....s...s..LgL.",
            ".....s..s..LgdgL",
            ".LgL..s.s.sgdsdg",
            "LgdgL.s.s.s.....",
            "gdsdg.s.ss..f...",
            ".....s.s.s...f..",
            "...f.s.s.s.Ey.f.",
            "..Ey..s.s..yBEy.",
            "..yB...SS...BC..",
        ],
        item=[
            "................",
            "................",
            "................",
            "...f.......f....",
            "....f.....f.....",
            "..LgdL...LgdL...",
            "..EAyB...EAyB...",
            "..AByC...yBBC...",
            "...yC.f...BCy...",
            "....C.f....C....",
            ".....LgdL.......",
            ".....EAyB.......",
            ".....yBBC.......",
            "......BCy.......",
            ".......C........",
            "................",
        ],
    ),
    "elderberry": dict(
        leaf={"L": "6e9f48", "g": ("4f843a", "457834"), "d": "335f2a", "D": "22401e", "w": "86b05e",
              "s": "8c7c62", "S": "5e5040", "f": "6e3048"},
        fruit=[
            {"A": "f8f4dc", "B": "e6e0b8", "C": "c8c08e", "E": "fffbe8", "f": "7f9e52"},       # creamy flat umbels
            {"A": "a8c868", "B": "7ea44a", "C": "5a7c36", "E": "c4dc8c", "f": "6e8a44"},       # green berries
            {"A": "5a3058", "B": "32183a", "C": "1c0c22", "E": "8a6a96"},       # purple-black
        ],
        stage0=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "......LgL.......",
            "....LgdsgdL.....",
            "...gd..s..dg....",
            ".......s.Lg.....",
            "..LgL..sgdgL....",
            "...dgd.s.d......",
            "....d..s........",
            ".......s........",
            ".......S........",
            "......SSS.......",
        ],
        grown=[
            "..LgL......LgL..",
            ".gdgdL....LgdgL.",
            "Lgd.dgf.f.fgd.dg",
            ".d..s.fff.s..d..",
            "..LgsAEBEBAsLg..",
            ".gdgsBCBCBCsgdgL",
            "LgdLgsC.C.Cs.gdg",
            ".d.gds.f.Lgs.d..",
            "..f.f.sgsdgsf.f.",
            "...ff.sds.s..ff.",
            ".AEBEAs.sLgAEBEA",
            ".BCBCBLgsgdBCBCB",
            "Lg.C.Cgds.s.C.C.",
            ".gdgd..sSs.gdgd.",
            "..d.d..SS...d.d.",
            "......SSS.......",
        ],
        item=[
            "................",
            "................",
            ".......ff.......",
            "......f..f......",
            ".....f.ff.f.....",
            "....ff.f.f.ff...",
            "...AEBABEBAEBA..",
            "..ABCBEBCBCBEB..",
            "..BCBCBCBEBCBC..",
            "...CBEBCBCBCB...",
            "....CBCBEBCB....",
            ".....CBCBCB.....",
            "......CBCB......",
            ".......CC.......",
            "................",
            "................",
        ],
    ),
    "cloudberry": dict(
        leaf={"L": "a0c85a", "g": ("7cae46", "71a03f"), "d": "557f33", "D": "3a5c27", "w": "b4d474",
              "s": "6f8f3c", "S": "4a6a2c", "f": "7a9a40"},
        fruit=[
            {"A": "fcfcf4", "B": "ecece0", "C": "dcdccc", "E": "f0d860", "y": "fcfcf4"},  # big white flower
            {"A": "f2b49a", "B": "d8705a", "C": "a84a3a", "E": "f8d0b8", "y": "c8d890"},  # hard, red-blushed
            {"A": "ffc456", "B": "ec9628", "C": "b4601a", "E": "fff0b0", "y": "d88a24"},  # soft amber
        ],
        stage0=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "..LgL.Lg........",
            ".LgdgLgdgL......",
            ".gdgdgdLgL.Lg...",
            "..dDd.dgdgLgdg..",
            "......s.ds.gd...",
            ".......SS.......",
        ],
        grown=[
            "................",
            "................",
            "................",
            "................",
            "......yAy.......",
            ".....ABEBA......",
            ".....BAEAB......",
            ".....CBABC......",
            "......CwC.LgL.Lg",
            ".LgL.Lg.fLgdgLgd",
            "LgdgLgdgfgdgdgdg",
            "gdgdgdLgfdLgdgd.",
            ".dDdgdgdf.dDdD..",
            "..s.d.d.f..s....",
            "...s....f.s.....",
            "....s..SfS......",
        ],
        item=[
            "................",
            "................",
            "................",
            "...LwgL.........",
            "..yAyAEy........",
            "..ABEBEBA.......",
            "..BAEABAB.LwgL..",
            "..CBABCBC.yAyAy.",
            "...CBCBC.ABEBEBA",
            "....CCC..BAEABAB",
            ".........CBABCBC",
            "..........CBCBC.",
            "...........CCC..",
            "................",
            "................",
            "................",
        ],
    ),
}


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def paint(grid, plant, fruit, seed):
    assert len(grid) == 16 and all(len(r) == 16 for r in grid), plant
    rng = random.Random(seed)  # fixed speckle so reruns give identical files
    pal = {**PLANTS[plant]["leaf"], **fruit}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            c = pal[ch]
            img.putpixel((x, y), hexc(rng.choice(c) if isinstance(c, tuple) else c))
    return img


def build():
    out = {}
    for name, p in PLANTS.items():
        out[f"block/{name}_bush_stage0.png"] = paint(p["stage0"], name, {}, name)
        for st, fr in enumerate(p["fruit"], start=1):
            out[f"block/{name}_bush_stage{st}.png"] = paint(p["grown"], name, fr, name)
        item = name[:-1] + "ies"  # raspberry -> raspberries, wild_strawberry -> wild_strawberries
        out[f"item/{item}.png"] = paint(p["item"], name, p["fruit"][2], name + "_item")
    return out


def preview(images, dst):
    s, cols = 12, 5
    sheet = Image.new("RGBA", (cols * 16 * s + (cols + 1) * 8, len(PLANTS) * (16 * s + 8) + 8), (198, 206, 214, 255))
    keys = list(images)
    for r, name in enumerate(PLANTS):
        row = [k for k in keys if k.startswith(f"block/{name}_")] + [k for k in keys if k.startswith("item/")][r:r + 1]
        for c, k in enumerate(row):
            big = images[k].resize((16 * s, 16 * s), Image.NEAREST)
            sheet.alpha_composite(big, (8 + c * (16 * s + 8), 8 + r * (16 * s + 8)))
    Path(dst).mkdir(parents=True, exist_ok=True)
    sheet.save(Path(dst) / "berry_textures.png")


if __name__ == "__main__":
    imgs = build()
    for rel, img in imgs.items():
        img.save(RES / rel)
        print(RES / rel)
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
