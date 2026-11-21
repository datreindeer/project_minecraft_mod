"""Textures for the field tools (FarmTools.java, scripts/gen_farm_tools.py):
  item/seed_bag.png        the Leather Pouch's shape, open at the neck with grain heaped in it, a bronze ring holding the mouth open
  item/linen_seed_bag.png  the same bag in pale linen with a stitched seam and an iron ring
  item/bronze_ard.png      the Bronze Hoe's palette: a broad bronze share on a long beam, a crosswise grip near the end
Run: python scripts/gen_farm_tools_textures.py"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"

BAG = [
    "................",
    "................",
    "......k..j......",
    ".....jkjkkj.....",
    "....bkjkjjkb....",
    ".....rRRRRr.....",
    "......bccb......",
    ".....bfeedb.....",
    "....bfeeeedgb...",
    "...bfeeeeeedgb..",
    "...bfeseeeedgb..",
    "...bfeeseeedgb..",
    "...bfeeeseedgb..",
    "...beeeeseddgb..",
    "....bdddddggb...",
    ".....bbbbbbb....",
]
GRAIN = {"j": (196, 160, 82), "k": (232, 204, 120)}
LEATHER = {"b": (59, 31, 18), "c": (122, 85, 48), "d": (143, 79, 43), "e": (168, 101, 58), "f": (196, 127, 76), "g": (110, 58, 31),
           "s": (168, 101, 58), "r": (92, 60, 22), "R": (218, 178, 96), **GRAIN}
LINEN = {"b": (92, 82, 64), "c": (176, 162, 128), "d": (190, 176, 142), "e": (218, 206, 172), "f": (236, 228, 200), "g": (160, 146, 114),
         "s": (150, 136, 104), "r": (70, 70, 74), "R": (168, 170, 176), **GRAIN}
ARD = [
    "......aaaa......",
    ".....abbcca.....",
    "......abcceaa...",
    ".......ddeccafg.",
    ".........deeehd.",
    "..........dfced.",
    ".........fgddd..",
    "........fhd.....",
    ".......fgd......",
    "......fhd.......",
    "..f..fgd........",
    "...ffhd.........",
    "...fhgd.........",
    "..fhd.hd........",
    "..dd...hd.......",
    "........d.......",
]
HOE = {"a": (90, 58, 21), "b": (244, 218, 150), "c": (222, 184, 104), "d": (52, 32, 12), "e": (206, 164, 83), "f": (73, 54, 21),
       "g": (104, 78, 30), "h": (137, 103, 39)}


def paint(grid, pal, name):
    assert len(grid) == 16 and all(len(r) == 16 for r in grid), name
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((x, y), pal[c] + (255,))
    img.save(OUT / f"{name}.png")
    print(f"item/{name}.png ok")


paint(BAG, LEATHER, "seed_bag")
paint(BAG, LINEN, "linen_seed_bag")
paint(ARD, HOE, "bronze_ard")
