"""Generate rock items, the bone needle, the Leatherworking Bench and Armor Station blocks and the station GUIs.

Rocks: stone_chunk / granite_chunk / diorite_chunk / andesite_chunk (shown as "Rock", "Granite Rock", ...),
each a different shape, coloured from its vanilla block texture and lit from the top left.
Leatherworking Bench: an oak leatherworker's bench (top / side / front), built on vanilla oak planks.
Armor Station: the same bench as a smithy, with bronze plates, a helmet and a hammer.
GUIs: copies of tool_station.png (same slots, list and scroll bar) with only a small icon added.

Run: python scripts/gen_station_textures.py
"""
import math
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def vanilla(path):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(BytesIO(z.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def from_grid(rows, pal, im=None):
    assert all(len(r) == len(rows[0]) for r in rows), [len(r) for r in rows]
    im = im or Image.new("RGBA", (len(rows[0]), len(rows)), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), hexc(pal[ch]))
    return im


def speck(x, y, salt=0, mod=5):
    """Scattered (non-diagonal) pseudo-random pixel picker."""
    return ((x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)) % mod == 0


# ---------- rocks ----------

def shaded_rock(mask, ramp, outline, light=(-1.0, -1.0), flat=1.0):
    """Mask '#' = rock. Edge pixels become outline; the rest is lit by an approximate surface normal."""
    h, w = len(mask), len(mask[0])
    inside = {(x, y) for y in range(h) for x in range(w) if mask[y][x] == "#"}
    lx, ly = light[0] / math.hypot(*light), light[1] / math.hypot(*light)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y) in inside:
        if any((x + dx, y + dy) not in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            im.putpixel((x, y), hexc(outline))
            continue
        nx = ny = 0.0
        for dy in range(-3, 4):
            for dx in range(-3, 4):
                if (x + dx, y + dy) not in inside:
                    d2 = dx * dx + dy * dy
                    nx += dx / d2
                    ny += dy / d2
        n = math.hypot(nx, ny) or 1.0
        v = 0.5 + 0.45 * (nx * lx + ny * ly) / n * min(1.0, n / 1.5) * flat
        im.putpixel((x, y), hexc(ramp[max(0, min(len(ramp) - 1, int(v * len(ramp))))]))
    return im, inside


def interior(inside, x, y):
    return all((x + dx, y + dy) in inside for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


# Stone: angular block, hand-drawn faces. T top face, L lit left face, M/D shaded right faces.
STONE = [
    "................",
    "................",
    "................",
    ".....OOOOOOOO...",
    "....OWWWWWWWTO..",
    "...OWTTTTsTTTMO.",
    "..OWTTTTTTTTMMO.",
    "..OLLLLLLLLMMMDO",
    "..OLLLLLLsLLMMDO",
    ".OLLLsLLLLLMMMDO",
    ".OLLLLLLLLLMMsDO",
    ".OLLLLLLsLLMMDO.",
    ".OLLsLLLLLLMMDO.",
    "..OOOLLLLLLMDO..",
    ".....OOOOOOOO...",
    "................",
]
STONE_PAL = {
    "O": "#2f2f2f", "W": "#b0b0b0", "T": "#9a9a9a", "L": "#7f7f7f",
    "M": "#686868", "D": "#525252", "s": "#747474",
}

GRANITE_MASK = [
    "................",
    "................",
    "................",
    ".....######.....",
    "...##########...",
    "..############..",
    "..############..",
    ".##############.",
    ".##############.",
    ".##############.",
    ".##############.",
    "..############..",
    "..############..",
    "...##########...",
    ".....######.....",
    "................",
]
DIORITE_MASK = [
    "................",
    "................",
    "...#####........",
    "..########......",
    ".###########....",
    ".############...",
    ".#############..",
    "..#############.",
    "..##############",
    ".###############",
    ".###############",
    "..#############.",
    "...###########..",
    "....######.##...",
    "................",
    "................",
]
ANDESITE_MASK = [
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....#######....",
    "...###########..",
    "..#############.",
    ".###############",
    ".###############",
    ".###############",
    "..#############.",
    "...###########..",
    ".....#######....",
    "................",
    "................",
]


def rocks():
    out = TEX / "item"
    from_grid(STONE, STONE_PAL).save(out / "stone_chunk.png")

    # granite: round, pink-red, dark flecks and pale crystal glints
    im, inside = shaded_rock(GRANITE_MASK, ["#5f4034", "#7f5646", "#926251", "#9f6b58", "#a97764", "#b48877"], "#3e2a22")
    for (x, y) in inside:
        if not interior(inside, x, y):
            continue
        if speck(x, y, 1, 7):
            im.putpixel((x, y), hexc("#4d352c"))
        elif speck(x, y, 4, 11):
            im.putpixel((x, y), hexc("#e3c0af" if x + y < 15 else "#c99781"))
        elif speck(x, y, 9, 13):
            im.putpixel((x, y), hexc("#b49b91"))
    im.save(out / "granite_chunk.png")

    # diorite: lumpy, white-grey, black and grey speckles
    im, inside = shaded_rock(DIORITE_MASK, ["#7b7b7b", "#8b8b8b", "#a4a2a2", "#bebfc1", "#cececf", "#e9e9e9"], "#454545")
    for (x, y) in inside:
        if not interior(inside, x, y):
            continue
        if speck(x, y, 2, 6):
            im.putpixel((x, y), hexc("#3a3a3a" if speck(x, y, 0, 2) else "#7b7b7b"))
        elif speck(x, y, 5, 9):
            im.putpixel((x, y), hexc("#8b8b8b"))
    im.save(out / "diorite_chunk.png")

    # andesite: flat smooth pebble, few light specks
    im, inside = shaded_rock(ANDESITE_MASK, ["#5a5a5a", "#686868", "#747474", "#7f7f7f", "#8a8a8e", "#9c9c9c"], "#303030", flat=1.2)
    for (x, y) in ((5, 9), (10, 8), (8, 11), (12, 10)):
        im.putpixel((x, y), hexc("#a8aa9a"))
    im.save(out / "andesite_chunk.png")


# ---------- bone needle ----------

NEEDLE = [
    "................",
    "...........OOO..",
    "..........OWWBO.",
    "..........OWoBOt",
    ".........OWBBOOt",
    "........OWBO...t",
    ".......OWBO...tt",
    "......OWBO....t.",
    ".....OWBO....tt.",
    "....OWBO.....t..",
    "...OWBO......t..",
    "..OWBO......u...",
    "..OBO...........",
    "..OO............",
    "................",
    "................",
]
NEEDLE_PAL = {
    "O": "#4e4b3c", "W": "#fcfbed", "B": "#cbc6a5", "o": "#2a2820",
    "t": "#8f6e4e", "u": "#5e4430",
}


# ---------- leatherworking bench / armor station blocks ----------

def plank_base():
    """Oak planks with the crafting table's dark side frame (legs) and top edge."""
    planks = vanilla("block/oak_planks")
    frame = vanilla("block/crafting_table_front")
    for y in range(16):
        for x in (0, 15):
            planks.putpixel((x, y), frame.getpixel((x, y)))
    return planks


LEATHER = {"K": "#3d1c10", "k": "#542716", "m": "#893b25", "n": "#9e492a", "l": "#c65c35", "h": "#d76b43"}
BONE = {"W": "#fcfbed", "B": "#cbc6a5", "O": "#7b7e6b"}
THREAD = {"t": "#e0d4bc", "u": "#4a3a20"}
WOOD = {"p": "#4a3a20", "q": "#67502c"}  # pegs / tool handles
STONE_TOOL = {"s": "#a8a8a8", "S": "#6a6a6a"}

TOP_DECOR = [
    "................",
    ".p............p.",
    "..KkkK...KkkkK..",
    "..khhlkkkllnnk..",
    "..khhllllllWok..",
    "...khlllllWBWk..",
    "...klllllWBntk..",
    "...kllllWBnntk..",
    "..kllllWBnnnmt..",
    "..klllWBnnnmmkt.",
    "..kllWBnnnmmmkt.",
    "..klnBnnnmmmmk..",
    "..KkkkkKkkkkkKt.",
    ".p............tp",
    "................",
    "................",
]
FRONT_DECOR = [
    "................",
    "................",
    "................",
    "...p..p.....p...",
    "..KkkkkK...OqO..",
    "..KhlllK...OqO..",
    "..KllnnK...OqO..",
    "..KllnnmK.OSsSO.",
    "...KlnnmK.OSssO.",
    "...KlnnmK.OSsO..",
    "..KllnmmK.OSsO..",
    "..KnnmmK...OSO..",
    "...KmK.K....O...",
    "................",
    "................",
    "................",
]
SIDE_DECOR = [
    "................",
    "................",
    "................",
    "................",
    "......ppp.......",
    ".......p........",
    "......uuuu......",
    ".....uttttu.....",
    "....uttuuttu....",
    "....utu..utu....",
    "....uttuuttu....",
    ".....uttttu.....",
    "......uuuutu....",
    "...........tu...",
    "............t...",
    "................",
]


def leatherworking_bench():
    pal = {**LEATHER, **BONE, **THREAD, **WOOD, **STONE_TOOL, "o": "#2a2820", "O": "#2e2416"}
    out = TEX / "block"
    top = vanilla("block/oak_planks")
    edge = hexc("#67502c")
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            top.putpixel(p, edge)
    from_grid(TOP_DECOR, pal, top).save(out / "leatherworking_bench_top.png")
    from_grid(FRONT_DECOR, pal, plank_base()).save(out / "leatherworking_bench_front.png")
    from_grid(SIDE_DECOR, pal, plank_base()).save(out / "leatherworking_bench_side.png")


def bench_top():
    top = vanilla("block/oak_planks")
    edge = hexc("#67502c")
    for i in range(16):
        for p in ((i, 0), (i, 15), (0, i), (15, i)):
            top.putpixel(p, edge)
    return top


BRONZE = {"b": "#5e3e18", "c": "#9a6428", "d": "#c88e40", "e": "#f0c878"}
SMITH_TOP = [
    "................",
    ".p............p.",
    "....bbbbbb......",
    "...bcddddcb.....",
    "..bcdeeeddcb....",
    "..bcdedddccb....",
    "..bcddddcccb....",
    "..bbcdddccbb.SS.",
    "...bbccccbb.SssS",
    "....b.bb.b.qSsS.",
    "..........q..S..",
    ".........q......",
    "........q.......",
    ".......q........",
    ".p............p.",
    "................",
]
SMITH_FRONT = [
    "................",
    "................",
    "................",
    "...p...p....p...",
    "..bcb.bcb..SSS..",
    "..bdb.bdb..SsS..",
    "..bdb.bdb...q...",
    "..bdcbbdc...q...",
    "..bdeb.de...q...",
    "..bdc.bdc...q...",
    "..bcb.bcb...q...",
    "..bbb.bbb...q...",
    "................",
    "................",
    "................",
    "................",
]
SMITH_SIDE = [
    "................",
    "................",
    "................",
    "................",
    "......ppp.......",
    ".......p........",
    "......bbbb......",
    ".....bcddcb.....",
    "....bcdeddcb....",
    "....bcddddcb....",
    "....bcddccbb....",
    "....bb.bb.bb....",
    "....b......b....",
    "................",
    "................",
    "................",
]


def armor_station():
    pal = {**BRONZE, **WOOD, **STONE_TOOL}
    out = TEX / "block"
    from_grid(SMITH_TOP, pal, bench_top()).save(out / "armor_station_top.png")
    from_grid(SMITH_FRONT, pal, plank_base()).save(out / "armor_station_front.png")
    from_grid(SMITH_SIDE, pal, plank_base()).save(out / "armor_station_side.png")


# ---------- GUIs ----------

ICON_POS = (136, 60)  # empty panel space under the result slot, clear of every slot and label
FEATHER_ICON = [
    "..........OO",
    "........OOWO",
    ".......OWWGO",
    "......OWWGO.",
    ".....OWWGGO.",
    "....OWWGGO..",
    "...OWWGGO...",
    "..OWWGGO....",
    "..OWGOO.....",
    ".OSOO.......",
    "OSO.........",
    "OO..........",
]
FEATHER_PAL = {"O": "#373737", "W": "#ffffff", "G": "#bebebe", "S": "#8b8b8b"}
NEEDLE_ICON = [
    "........OOO.",
    ".......OWWBO",
    ".......OWoBO",
    "......OWBBO.",
    ".KKKKOWBOO..",
    "KlllOWBOnK..",
    "KlhOWBOnnK..",
    "KlOWBOnnmK..",
    "KOWBOnnnmK..",
    "OWBOnnnmmK..",
    "OBOkkkkkK...",
    "OO..........",
]

HAMMER_ICON = [
    "......OOOO..",
    ".....OSssSO.",
    "....OSssSSO.",
    ".....OSSOO..",
    "....OqO.....",
    "...OqO......",
    "..OqO.......",
    ".OqO........",
    "OqO.........",
    "OO..........",
    "............",
    "............",
]


def gui(name, icon, pal):
    im = Image.open(TEX / "gui/container/tool_station.png").convert("RGBA")
    layer = from_grid(icon, pal)
    im.alpha_composite(layer, ICON_POS)
    im.save(TEX / name)


def main():
    rocks()
    from_grid(NEEDLE, NEEDLE_PAL).save(TEX / "item/bone_needle.png")
    leatherworking_bench()
    armor_station()
    gui("gui/container/fletching_table.png", FEATHER_ICON, FEATHER_PAL)
    gui("gui/container/leatherworking_bench.png", NEEDLE_ICON, {**LEATHER, **BONE, "o": "#2a2820", "O": "#373737"})
    gui("gui/container/armor_station.png", HAMMER_ICON, {**STONE_TOOL, **WOOD, "O": "#373737"})


if __name__ == "__main__":
    main()
