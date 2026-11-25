"""Magma Block uses (Magma.java) and the closed Nether and End (DimensionLock.java): Crushed Magma (item, picture drawn
here), the two refractory recipes it goes into, and the lang keys.
Generator order: ... gen_p8smith.py, gen_magma.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, ...
Run: python scripts/gen_magma.py"""
import json
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data/mechanical"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


# a heap of broken crust: each lump a dark basalt shard (lit from the top left) with a glowing crack through some
OUTLINE, CRUST = (30, 10, 8), [(58, 22, 16), (86, 33, 20), (118, 48, 24)]
GLOW = [(190, 62, 12), (240, 116, 18), (255, 178, 44), (255, 230, 120)]
HEAP = [
    "................",
    "................",
    "................",
    "................",
    "......aaa.......",
    ".....aaaaa......",
    ".....aaaabbb....",
    "...cccaabbbbb...",
    "..cccccdbbbbb...",
    "..ccccddddbbee..",
    ".fcccddddddeeee.",
    ".ffffddddgggeee.",
    ".fffffhhhggggg..",
    "..fffhhhhhggg...",
    "....hhhhh.......",
    "................",
]
# the cracks: pixels of molten rock showing between and through the lumps, hottest in the middle
CRACKS = {(7, 5): 2, (8, 6): 3, (9, 8): 1, (6, 8): 2, (5, 9): 1, (7, 10): 3, (8, 10): 2, (9, 11): 1, (4, 11): 1,
          (11, 10): 2, (12, 11): 1, (6, 12): 2, (7, 13): 1, (10, 12): 2, (3, 12): 0, (13, 11): 0, (8, 12): 3}


def crushed_magma():
    img = Image.new("RGBA", (16, 16))
    rng = random.Random(1125)
    at = lambda x, y: HEAP[y][x] if 0 <= x < 16 and 0 <= y < 16 else "."
    for y in range(16):
        for x in range(16):
            c = at(x, y)
            if c == ".":
                continue
            if (x, y) in CRACKS:
                col = GLOW[CRACKS[(x, y)]]
            elif "." in (at(x + 1, y), at(x, y + 1)):
                col = OUTLINE
            else:
                lit = at(x - 1, y) != c or at(x, y - 1) != c
                shaded = at(x + 1, y) != c or at(x, y + 1) != c
                col = CRUST[2] if lit else OUTLINE if shaded and rng.random() < 0.5 else CRUST[rng.choice((0, 1, 1))]
            img.putpixel((x, y), col + (255,))
    # a lit rim along the top left of the heap, and two loose embers
    for y in range(16):
        for x in range(16):
            if at(x, y) != "." and at(x - 1, y) == "." and at(x, y - 1) == ".":
                img.putpixel((x, y), CRUST[1] + (255,))
    for p, g in (((2, 14), 1), ((13, 14), 2)):
        img.putpixel(p, GLOW[g] + (255,))
    img.save(A / "textures/item/crushed_magma.png")


crushed_magma()
w(A / "models/item/crushed_magma.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/crushed_magma"}})
w(A / "items/crushed_magma.json", {"model": {"type": "minecraft:model", "model": M + "item/crushed_magma"}})


def shapeless(name, ingredients, result, count, category="misc"):
    w(D / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                                  "result": {"id": result, "count": count}})


# refractories: its grit does grog's work in Fire Clay, and quicklime's in the Silica Brick mix
shapeless("fire_clay_from_magma", ["minecraft:clay_ball"] * 2 + [M + "crushed_magma"] * 2, M + "fire_clay", 4)
shapeless("unfired_silica_brick_from_magma", [M + "crushed_quartzite"] * 2 + [M + "fire_clay", M + "crushed_magma"],
          M + "unfired_silica_brick", 4, "building")

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({
    "item.mechanical.crushed_magma": "Crushed Magma",
    "message.mechanical.magma.heats": "The Magma Block under it holds it at %s° without fuel",
    "message.mechanical.dimension.closed": "The way there is closed for now",
    "message.mechanical.dimension.frame": "The frame won't take the eye: the End is closed for now",
})
lang_path.write_text(json.dumps(lang, indent=2) + "\n")
print("magma ok")
