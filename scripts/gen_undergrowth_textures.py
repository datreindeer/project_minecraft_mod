# Forest undergrowth textures: hazel shrub (2 halves, with/without nuts), juniper (with/without berries), bramble,
# undergrowth, sapling thicket (2 halves), and the hazelnut / roasted hazelnut / juniper berry items.
# Hand-drawn letter grids like gen_berry_textures.py; colours baked in (no biome tint).
# usage: python scripts/gen_undergrowth_textures.py [preview_dir]
import random
import sys
from pathlib import Path
from PIL import Image

RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"

HAZEL = {"L": "86b752", "g": ("62983d", "578b37"), "d": "477d31", "D": "2f5a24", "s": "7d6248", "S": "564231",
         "h": "d4d07a", "H": "a8a850", "n": "b47840", "N": "6e4222", "o": "4a2e1a", "l": "d09a62"}
JUNIPER = {"L": "6f9a74", "g": ("426f55", "3b654c"), "d": "2b5141", "D": "1b3a2f", "w": "8db29a", "s": "6a4a34",
           "S": "4a3426", "b": "93a3cc", "B": "4f5d92", "K": "2d3668", "k": "1b1f40"}
BRAMBLE = {"L": "6f9e4a", "g": ("4b7c37", "447230"), "d": "31572a", "D": "213e1e", "c": "8a4040", "C": "552630",
           "t": "d6c0a0", "f": "f2e2e4"}
UNDER = {"L": "86bd5c", "g": ("5c9c3f", "529039"), "d": "3d7231", "D": "295223", "s": "62843a"}
THICKET = {"L": "8ec65c", "g": ("66a742", "5c9a3c"), "d": "437a30", "s": "8a7058", "S": "5e4a3a", "w": "a39a86"}
NUT = {"o": "3e2414", "N": "7a4826", "n": "a46a38", "l": "cf9a5e", "p": "e0d2aa", "h": "93ab52", "H": "617d30"}
ROAST = {"o": "2a160c", "N": "4e2a14", "n": "74421e", "l": "a06a38", "p": "b89c70", "h": "8a6a3a", "H": "5a4024"}

HAZEL_TOP = [
    "................",
    "................",
    "......LgL.......",
    "....LggdgL.Lg...",
    "...LgdDgLggdgL..",
    "..LgdLgdgdgDgdL.",
    ".LgdgdDgdLgDdgd.",
    ".gdLgdgDdgdgLgdL",
    "LgdgD.gdgLgDgdgd",
    "gdD.LgdDgdgLgdD.",
    ".dLgdgD.Dg.dLgdL",
    "LgdgdLgLsLgD.gdg",
    "gdD.gdgdsgdsLgdD",
    ".dLgd.sDs.sgdgd.",
    "LgdgdLs.sLgsDgdL",
    "gdD.gds..sSdgdD.",
]
HAZEL_BOTTOM = [
    "Lgd.LgdgL.gdLgdg",
    "gdD.gdsDgLgsgdD.",
    ".dLgd.sgdgds.dLg",
    "LgdgLs.LgdsLgdgd",
    "gdD.gsgdD.sgdD..",
    ".dLgds.d.s.dLgL.",
    "..gd.s.Lgs..gdgd",
    "Lg...sgdgs.Lg.d.",
    "gdL..s.d.sgdD...",
    ".dgL.s..s.dg....",
    "..gdLs..s.LgL...",
    "....ds.s..gdg...",
    ".....s.s.s.d....",
    "......ss.s......",
    "......SsSs......",
    ".....SS.SS......",
]
# hazelnut clusters (top-left of a 2x2: husk over nut) shown when the shrub has nuts
NUT_SPOTS_TOP = [(3, 7), (11, 5), (6, 11), (12, 12), (1, 13)]
NUT_SPOTS_BOTTOM = [(2, 1), (12, 3), (7, 5)]
JUNIPER_GRID = [
    "................",
    "................",
    "................",
    ".......L........",
    "......wgd.......",
    ".....LgdgL......",
    "....wgdDgwg.....",
    "...LgdgLdgDgL...",
    "..wgDgwgdLgdgd..",
    "..LgdLgDgwgDgdL.",
    ".wgwgdgLdgdLgwg.",
    ".LgdDgwgdDgwgdgd",
    "wgdLgdgLgdgLgDdL",
    "LgDgwgDgdwgdgdgd",
    ".dgdgDgdgDgdgDd.",
    "...SsS..sSs.S...",
]
JUNIPER_BERRY_SPOTS = [(5, 8), (9, 6), (3, 11), (11, 10), (7, 12), (13, 13), (8, 9)]
BRAMBLE_GRID = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......Lg...gL...",
    "...LgdgCc.cdgLg.",
    "..gdD.c.tcc.Dgd.",
    ".Lgc.cfLg.tcLgdL",
    ".gtcLgdgdc.cgdgd",
    "c.cgdDtc.LgdcDt.",
    "Lgcd.cLgdgdtc.Lg",
    "gdtcgdfdDc.cLgdg",
    ".cLgdtc.cLgdcgd.",
    "c.gdc.Ccdg.tc.c.",
    "..C..C..C...C...",
]
UNDER_GRID = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......L........",
    "..Lg...gL....g..",
    "...gL.Lg.d..gL..",
    "L...gdgd.gLgd...",
    ".gL.Dgd.LgdgL.Lg",
    "..gdLgDgdgDgdgd.",
    "Lg.gdLgdgdLgDg.L",
    ".gdDgdgDgLgdgdg.",
    "gdLgdDgdgdDgdLgd",
]
THICKET_TOP = [
    "......Lg........",
    "...Lg.gdL...gL..",
    "..Lgd..w...Lgd..",
    "...ds..wLg..s.Lg",
    "...s..gwd...sgd.",
    "..Lgs..w...Lgs..",
    "..gds..wL...gs..",
    "....s.Lwgd..s...",
    "....s..w...Ls...",
    "..Lgs..w...gdsLg",
    "...ds..wLg...sd.",
    "....s..wd....s..",
    "..s.s..w.....s..",
    "..s..s.w....s...",
    "..s..s.w....s..S",
    "..s..s.w...s...S",
]
THICKET_BOTTOM = [
    "..s..s.w...s...S",
    "..s..s.w...s..S.",
    "..s.Lg.w...s..S.",
    "..s.gds.w..sL.S.",
    "..Ls..s.w..sgdS.",
    ".Lgds.s.w.s..S..",
    "..d.s.s.w.s..S..",
    "....s.s.wLs.S...",
    "...Lgs.s.wgdS...",
    "...gds.s.w.S....",
    ".....s.s.wS.Lg..",
    ".....s.s.wS.gd..",
    "..Lg.s.s.wS.....",
    "..gds.s.w.S.....",
    "......s.w.S.....",
    ".....SsSwS.S....",
]
NUTS_ITEM = [
    "................",
    "................",
    "................",
    "................",
    "........oo......",
    ".......onlo.....",
    "......onlnNo....",
    "......onnnNo....",
    "..oo..oNnNNo....",
    ".onlo.onNNNo....",
    "onlnNo.pppp.oo..",
    "onnnNo.....onlo.",
    "oNnNNo....onlnNo",
    "oNNNNo....onnnNo",
    ".pppp.....oNnNNo",
    "...........pppp.",
]
BERRIES_ITEM = [
    "................",
    "..........d.....",
    "........d.gd.d..",
    ".........gwgd...",
    "......d.dgDg....",
    "....bBb..gd.d...",
    "...bBBBk.d......",
    "...BBBKk.bBb....",
    "...kBKKkbBBBk...",
    "....kkk.BBBKk...",
    "...bBb..kBKKk...",
    "..bBBBk..kkk....",
    "..BBBKk.........",
    "..kBKKk.........",
    "...kkk..........",
    "................",
]


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def paint(grid, pal, seed, stamps=(), stamp=None):
    assert len(grid) == 16 and all(len(r) == 16 for r in grid), seed
    rng = random.Random(seed)  # fixed speckle so reruns give identical files
    rows = [list(r) for r in grid]
    for x, y in stamps:  # fruit drawn over the foliage
        for dy, srow in enumerate(stamp):
            for dx, ch in enumerate(srow):
                if ch != "." and 0 <= x + dx < 16 and 0 <= y + dy < 16:
                    rows[y + dy][x + dx] = ch
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                img.putpixel((x, y), hexc(rng.choice(c) if isinstance(c, tuple) else c))
    return img


HAZELNUT = ["hH", "nN"]
BERRY = ["bB", "Bk"]


def build():
    return {
        "block/hazel_shrub_top.png": paint(HAZEL_TOP, HAZEL, "ht"),
        "block/hazel_shrub_top_nuts.png": paint(HAZEL_TOP, HAZEL, "ht", NUT_SPOTS_TOP, HAZELNUT),
        "block/hazel_shrub_bottom.png": paint(HAZEL_BOTTOM, HAZEL, "hb"),
        "block/hazel_shrub_bottom_nuts.png": paint(HAZEL_BOTTOM, HAZEL, "hb", NUT_SPOTS_BOTTOM, HAZELNUT),
        "block/juniper.png": paint(JUNIPER_GRID, JUNIPER, "j"),
        "block/juniper_berries.png": paint(JUNIPER_GRID, JUNIPER, "j", JUNIPER_BERRY_SPOTS, BERRY),
        "block/bramble.png": paint(BRAMBLE_GRID, BRAMBLE, "b"),
        "block/undergrowth.png": paint(UNDER_GRID, UNDER, "u"),
        "block/sapling_thicket_top.png": paint(THICKET_TOP, THICKET, "tt"),
        "block/sapling_thicket_bottom.png": paint(THICKET_BOTTOM, THICKET, "tb"),
        "item/hazelnuts.png": paint(NUTS_ITEM, NUT, "n"),
        "item/roasted_hazelnuts.png": paint(NUTS_ITEM, ROAST, "n"),
        "item/juniper_berries.png": paint(BERRIES_ITEM, {**JUNIPER, "B": "4f5d92"}, "jb"),
    }


def preview(images, dst):
    s, gap = 10, 8
    # tall plants stacked (top over bottom) so the halves can be checked for a seamless join
    cols = [["block/hazel_shrub_top.png", "block/hazel_shrub_bottom.png"],
            ["block/hazel_shrub_top_nuts.png", "block/hazel_shrub_bottom_nuts.png"],
            ["block/sapling_thicket_top.png", "block/sapling_thicket_bottom.png"],
            ["block/juniper.png", "block/juniper_berries.png"], ["block/bramble.png", "block/undergrowth.png"],
            ["item/hazelnuts.png", "item/roasted_hazelnuts.png"], ["item/juniper_berries.png"]]
    sheet = Image.new("RGBA", (len(cols) * (16 * s + gap) + gap, 2 * 16 * s + 2 * gap + 16 * s), (198, 206, 214, 255))
    for c, col in enumerate(cols):
        for r, k in enumerate(col):
            stacked = c < 3
            y = gap + r * (16 * s + (0 if stacked else gap))
            sheet.alpha_composite(images[k].resize((16 * s, 16 * s), Image.NEAREST), (gap + c * (16 * s + gap), y))
    # bottom strip: the blocks at true-ish scale, side by side on grass-coloured ground
    y0 = 2 * 16 * s + 2 * gap
    sheet.paste((96, 140, 60, 255), (0, y0 + 16 * 4, sheet.width, sheet.height))
    x = gap
    for k in ["block/hazel_shrub_bottom_nuts.png", "block/juniper_berries.png", "block/bramble.png", "block/undergrowth.png",
              "block/sapling_thicket_bottom.png", "block/juniper.png", "block/hazel_shrub_bottom.png"]:
        sheet.alpha_composite(images[k].resize((64, 64), Image.NEAREST), (x, y0))
        if k.endswith("_bottom.png") or k.endswith("_bottom_nuts.png"):
            top = images[k.replace("bottom", "top")].resize((64, 64), Image.NEAREST)
            sheet.alpha_composite(top, (x, y0 - 64))
        x += 72
    Path(dst).mkdir(parents=True, exist_ok=True)
    sheet.save(Path(dst) / "undergrowth_textures.png")


if __name__ == "__main__":
    imgs = build()
    for rel, img in imgs.items():
        img.save(RES / rel)
    print(f"{len(imgs)} undergrowth textures ok")
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
