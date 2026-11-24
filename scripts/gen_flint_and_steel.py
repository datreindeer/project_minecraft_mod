"""Flint and Steel (mechanical:flint_and_steel): item sprite, item definition and model.
  item/flint_and_steel.png   16x16: a steel striker (the arched bar) with a knapped flint chunk above it and a spark
  particle/flint_spark.png   4x4: a round glowing dot, white (FlintSparkParticle tints it as it cools)
  particles/flint_spark.json
  data minecraft:creeper_igniters + mechanical:flint_and_steel (vanilla's Flint and Steel is no longer made)
"""
import json
from pathlib import Path

from PIL import Image

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"
PAL = {"O": "24262b", "a": "8d939a", "b": "6b7078", "c": "c3c8ce", "d": "4e535a",
       "F": "3d3a40", "f": "5d5964", "g": "857f8c", "y": "f2c94c", "w": "fff4b0"}
GRID = [
    "................",
    "..........y.....",
    ".........ywy.y..",
    "..........y.....",
    "........OOOOO...",
    ".......OgggfFO..",
    "......OggffFFFO.",
    "......OgfFFFFO..",
    ".......OOFFFO...",
    "....OOOOOOOOOO..",
    "...OcaaaaaaabO..",
    "..OcaOOOOOOOabO.",
    "..OcO.......ObO.",
    "..OaO.......ObO.",
    "..ObdbbbbbbbdbO.",
    "...OOOOOOOOOOO..",
]
assert all(len(r) == 16 for r in GRID) and len(GRID) == 16
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
for y, row in enumerate(GRID):
    for x, c in enumerate(row):
        if c != ".":
            h = PAL[c]
            img.putpixel((x, y), (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255))
(A / "textures/item").mkdir(parents=True, exist_ok=True)
img.save(A / "textures/item/flint_and_steel.png")
(A / "models/item/flint_and_steel.json").write_text(json.dumps(
    {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/flint_and_steel"}}, indent=2) + "\n")
(A / "items/flint_and_steel.json").write_text(json.dumps(
    {"model": {"type": "minecraft:model", "model": "mechanical:item/flint_and_steel"}}, indent=2) + "\n")
spark = Image.new("RGBA", (4, 4), (0, 0, 0, 0))
for (x, y), a in {(1, 0): 150, (2, 0): 150, (0, 1): 150, (3, 1): 150, (0, 2): 150, (3, 2): 150, (1, 3): 150, (2, 3): 150,
                  (1, 1): 255, (2, 1): 255, (1, 2): 255, (2, 2): 255}.items():
    v = 255 if a == 255 else 225
    spark.putpixel((x, y), (v, v, v, 255))  # opaque layer: the rim is a shade darker instead of see-through
(A / "textures/particle").mkdir(parents=True, exist_ok=True)
spark.save(A / "textures/particle/flint_spark.png")
(A / "particles/flint_spark.json").write_text(json.dumps({"textures": ["mechanical:flint_spark"]}, indent=2) + "\n")
D = Path(__file__).resolve().parent.parent / "src/main/resources/data"
(D / "minecraft/tags/item").mkdir(parents=True, exist_ok=True)
(D / "minecraft/tags/item/creeper_igniters.json").write_text(json.dumps({"replace": False, "values": ["mechanical:flint_and_steel"]}, indent=2) + "\n")
print("flint_and_steel ok")
