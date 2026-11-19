"""Mold Bench data (MoldBench.java, MoldBenchBlock.java): every unfired clay mold and the Unfired Crucible are shaped
there from Clay Balls (MoldBench.ALL), so gen_data.py / gen_copper.py / gen_kettle.py no longer write crafting recipes
for them.

- model (worker on the north side): four oak legs with stretchers, a plank top smeared with clay, a clay slab with an
  ingot cavity half pressed in, a clay lump, a flint knife along the front edge and a wooden spatula on the right
- blockstate (facing), item model, loot, axe tag, lang
- recipe: Flint, Clay, Flint over 3 Planks (unlocked with Clay Balls by gen_data.py's clay_ball recipe advancement)
Textures: scripts/gen_moldbench_textures.py.
Generator order: ... gen_kettle.py, gen_moldbench.py, gen_roots.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_moldbench.py"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
SIDES = ("north", "south", "east", "west")


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def patch_tag(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def el(f, t, faces):
    """faces: {direction: (texture, uv)}; a None uv lets Minecraft take it from the element's position."""
    return {"from": f, "to": t, "faces": {d: {"texture": tex, **({"uv": uv} if uv else {})} for d, (tex, uv) in faces.items()}}


def cube(f, t, tex, uv_top=None, uv_side=None, uv_end=None, down=True):
    """A box: top/bottom take uv_top, north/south uv_side, east/west uv_end (all None = positional)."""
    faces = {"up": (tex, uv_top), "north": (tex, uv_side), "south": (tex, uv_side), "east": (tex, uv_end), "west": (tex, uv_end)}
    if down:
        faces["down"] = (tex, uv_top)
    return el(f, t, faces)


legs = [cube([x, 0, z], [x + 2, 9, z + 2], "#log", [0, 0, 2, 2], [x, 7, x + 2, 16], [z, 7, z + 2, 16])
        for x in (1, 13) for z in (2, 12)]
rails = [cube([x, 3, 4], [x + 1, 4.5, 12], "#log", [4, 0, 5, 8], [0, 4, 1, 5.5], [4, 4, 12, 5.5]) for x in (1.5, 13.5)]
rails.append(cube([3, 3, 12.5], [13, 4.5, 13.5], "#log", [3, 0, 13, 1], [3, 4, 13, 5.5], [0, 4, 1, 5.5]))
top = el([0, 9, 1], [16, 11, 15], {"up": ("#top", [0, 1, 16, 15]), "down": ("#planks", [0, 1, 16, 15]),
         "north": ("#planks", [0, 5, 16, 7]), "south": ("#planks", [0, 5, 16, 7]),
         "east": ("#planks", [1, 9, 15, 11]), "west": ("#planks", [1, 9, 15, 11])})
slab = cube([4, 11, 5], [12, 13, 11], "#parts", [0, 0, 8, 6], [0, 6, 8, 8], [8, 6, 14, 8], down=False)
lump = cube([12, 11, 11], [15, 12, 14], "#parts", [0, 8, 3, 11], [0, 11, 3, 12], [0, 11, 3, 12], down=False)
knife = [cube([3, 11, 1.5], [7, 11.5, 2.5], "#parts", [0, 12, 4, 13], [0, 13, 4, 13.5], [0, 13, 1, 13.5], down=False),
         cube([7, 11, 1.5], [10, 11.75, 2.5], "#parts", [4, 12, 7, 13], [4, 13, 7, 13.75], [4, 13, 5, 13.75], down=False)]
spatula = [cube([13, 11, 2], [14, 11.5, 8], "#parts", [0, 14, 6, 15], [0, 15, 1, 15.5], [0, 15, 6, 15.5], down=False),
           cube([12.5, 11, 8], [14.5, 11.5, 10.5], "#parts", [8, 14, 12, 16], [8, 15, 10, 15.5], [8, 15, 10.5, 15.5], down=False)]
for e in spatula:  # the sheet draws the spatula along x; it lies along z
    e["faces"]["up"]["rotation"] = 90

w(A / "models/block/mold_bench.json", {"parent": "minecraft:block/block", "textures": {
    "particle": "minecraft:block/oak_planks", "planks": "minecraft:block/oak_planks", "log": "minecraft:block/oak_log",
    "top": M + "block/mold_bench_top", "parts": M + "block/mold_bench_parts"},
    "elements": legs + rails + [top, slab, lump] + knife + spatula})
w(A / "blockstates/mold_bench.json", {"variants": {f"facing={f}": {"model": M + "block/mold_bench", **({"y": y} if y else {})}
                                                   for f, y in ROT.items()}})
w(A / "items/mold_bench.json", {"model": {"type": "minecraft:model", "model": M + "block/mold_bench"}})
w(D / "mechanical/loot_table/blocks/mold_bench.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + "mold_bench"}]}],
    "random_sequence": "mechanical:blocks/mold_bench"})
patch_tag(D / "minecraft/tags/block/mineable/axe.json", [M + "mold_bench"])

# flint edges to cut the clay, a clay block to work on, a plank bench (nothing else uses flint with clay)
w(D / "mechanical/recipe/mold_bench.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["FCF", "PPP"],
    "key": {"F": "minecraft:flint", "C": "minecraft:clay", "P": "#minecraft:planks"}, "result": {"id": M + "mold_bench"}})

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({"block.mechanical.mold_bench": "Mold Bench", "container.mechanical.mold_bench": "Mold Bench"})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
print("mold bench data written")
