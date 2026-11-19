"""Leather Satchel item texture: a sewn leather bag with a shoulder strap, a flap with stitching and a bone toggle.
Run: python scripts/gen_satchel_texture.py"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item/leather_satchel.png"

# O outline, D dark leather, M mid leather, L light leather, H highlight, s stitch, S strap, s2 strap dark,
# B bone toggle, b bone shadow
PAL = {
    "O": "3b1f12", "D": "6e3a1f", "M": "8f4f2b", "L": "a8653a", "H": "c47f4c",
    "s": "e0c9a0", "S": "7a4424", "T": "53291a", "B": "eee6cf", "b": "b8ab88",
}
GRID = [
    "....OOOOOOOO....",
    "...OSTOOOOTSO...",
    "..OS..O..O..SO..",
    "..OT........TO..",
    "..OS........SO..",
    ".OOOOOOOOOOOOOO.",
    ".OHHLLLLLLLLLMO.",
    ".OHsLsLsLsLsLMO.",
    ".OLLLLLBbLLLLMO.",
    ".ODLLLLBbLLLMDO.",
    ".OMDDLLOOLLDDMO.",
    ".OLMMDDDDDDMMDO.",
    ".OLMMMMMMMMMMDO.",
    ".OLsMsMsMsMsMDO.",
    "..ODDDDDDDDDDO..",
    "...OOOOOOOOOO...",
]
assert len(GRID) == 16 and all(len(r) == 16 for r in GRID)

img = Image.new("RGBA", (16, 16))
for y, row in enumerate(GRID):
    for x, c in enumerate(row):
        if c != ".":
            h = PAL[c]
            img.putpixel((x, y), (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255))
img.save(OUT)
print("satchel texture ok")
