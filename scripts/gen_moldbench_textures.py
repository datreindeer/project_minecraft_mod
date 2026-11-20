"""Mold Bench textures (MoldBench.java, model: scripts/gen_moldbench.py).

- block/mold_bench_top: vanilla oak planks smeared with wet clay (thick smears, dried pale edges, scraped streaks)
- block/mold_bench_parts: a sheet of the small parts, uv-mapped by the model:
    0-7 x 0-5   the clay slab's top with an ingot-shaped cavity half pressed in and thumb marks
    0-7 x 6-7   the slab's long side (layers of wet clay), 8-13 x 6-7 its short side
    0-2 x 8-10  the clay lump's top, 0-2 x 11 its side
    0-3 x 12-13 flint blade of the knife, 4-7 x 12-13 its handle (lashed with twine)
    0-5 x 14-15 the wooden spatula (handle), 8-11 x 14-15 its paddle
- gui/container/mold_bench.png: the station GUI (tool_station.png) with only the middle input slot and a clay icon
Clay greys are the unfired-mold palette (gen_copper_textures.RAW). Run: python scripts/gen_moldbench_textures.py
"""
from pathlib import Path

from PIL import Image

from gen_station_textures import TEX, ICON_POS, from_grid, vanilla

CLAY = {"D": "#60647a", "d": "#7e8494", "c": "#9ca2b2", "l": "#b0b6c4", "w": "#c4c9d4"}
WOOD = {"s": "#674f2c", "h": "#7e6237", "o": "#967441", "p": "#af8f55"}
FLINT = {"k": "#2c2c2e", "f": "#48484c", "g": "#66666b", "G": "#8a8a90"}
TWINE = {"t": "#a08a5a", "T": "#c4ac74"}
PAL = {**CLAY, **WOOD, **FLINT, **TWINE, "O": "#373737"}

# clay smeared over the planks: the slab hides the middle, so most of it sits round the edges where hands wiped off
TOP_DECOR = [
    ".lc.........s...",
    "lccd.....lw..s..",
    ".cdd....lccd....",
    "..d......cd...lw",
    ".s............cd",
    "..s.............",
    "lw..............",
    "cd............s.",
    "d..............s",
    "...............l",
    ".l............lc",
    "lcd............d",
    ".d...s..........",
    "......s.....lw..",
    "..lwc..s...lccd.",
    ".lccccd.....dd..",
]
# 16x16 parts sheet, see the docstring for the regions
PARTS = [
    "wllllllc........",
    "lcDDDDlc........",
    "lDdddDcc........",
    "cDdddDlc........",
    "ccDDDDcd........",
    "clccccdd........",
    "lllclllc..lclll.",
    "ddcddcdd..dcddd.",
    "wlc.............",
    "lcc.............",
    "cdd.............",
    "ddD.............",
    "Ggff.TtT........",
    "fffk.hoh........",
    "pophop..pppo....",
    "hshhsh..ohhs....",
]
GUI_ICON = [
    "............",
    "....OOOO....",
    "...OwllcO...",
    "..OwllccdO..",
    "..OlcccddO..",
    "..OccddDdO..",
    "...OddDDO...",
    "..OOOOOOOO..",
    ".OppoooohsO.",
    "OpoohhhhhssO",
    "OOOOOOOOOOOO",
    "............",
]


def main():
    from_grid(TOP_DECOR, PAL, vanilla("block/oak_planks")).save(TEX / "block/mold_bench_top.png")
    from_grid(PARTS, PAL).save(TEX / "block/mold_bench_parts.png")
    gui = Image.open(TEX / "gui/container/tool_station.png").convert("RGBA")
    panel = gui.getpixel((70, 10))
    for y0 in (16, 52):  # only the middle input slot stays
        for y in range(y0, y0 + 18):
            for x in range(79, 97):
                gui.putpixel((x, y), panel)
    gui.alpha_composite(from_grid(GUI_ICON, PAL), ICON_POS)
    gui.save(TEX / "gui/container/mold_bench.png")


if __name__ == "__main__":
    main()
