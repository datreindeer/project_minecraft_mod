"""Wolf Pelt uses: Wolf Hood, Wolf Fur Coat / Leggings / Boots, Wolf Rug and the Furred Straw Bed.
Writes item models, lang, armour tags, the repair tag, the equipment assets (wolf_fur: the coat, leggings and boots;
wolf_hood: the hood's own 3D model, client/WolfHoodModel.java), the rug's block model / blockstate / loot / recipe, and
the Furred Straw Bed's blockstate (vanilla straw bed models with a pelt model on top), pelt models and loot.
The armour is sewn at the Armor Station (ArmorStationRecipes.java); warmth in Temperature.java; the hood's effect on
Grey Wolves in wildlife/GreyWolf.java; the bed in StrawBedRest.java. Textures: scripts/gen_wolf_pelt_textures.py.
Generator order: ... gen_trees.py, gen_biomes.py, gen_wolf_pelt.py, gen_block_items.py, gen_unlocks.py, gen_registry.py.
Run: python scripts/gen_wolf_pelt.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


ARMOR = {"wolf_hood": ("Wolf Hood", "head_armor"), "wolf_fur_coat": ("Wolf Fur Coat", "chest_armor"),
         "wolf_fur_leggings": ("Wolf Fur Leggings", "leg_armor"), "wolf_fur_boots": ("Wolf Fur Boots", "foot_armor")}
for item, (_name, tag) in ARMOR.items():
    flat_item(item)
    tag_add(D / f"minecraft/tags/item/{tag}.json", f"mechanical:{item}")
w(D / "mechanical/tags/item/repairs_wolf_fur_armor.json", {"values": ["mechanical:wolf_pelt"]})
w(A / "equipment/wolf_fur.json", {"layers": {"humanoid": [{"texture": "mechanical:wolf_fur"}],
                                             "humanoid_leggings": [{"texture": "mechanical:wolf_fur"}]}})
w(A / "equipment/wolf_hood.json", {"layers": {"humanoid": [{"texture": "mechanical:wolf_hood"}]}})

# ---- Wolf Rug: flat like the Bear Rug (same block class and model shape), head away from whoever places it ----
w(A / "models/block/wolf_rug.json", {"parent": "mechanical:block/bear_rug",
    "textures": {"particle": "mechanical:block/wolf_rug", "top": "mechanical:block/wolf_rug"}})
w(A / "blockstates/wolf_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/wolf_rug", **({"y": y} if y else {})}
    for f, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}})
flat_item("wolf_rug")
w(D / "mechanical/loot_table/blocks/wolf_rug.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "mechanical:wolf_rug"}]}], "random_sequence": "mechanical:blocks/wolf_rug"})
w(D / "mechanical/recipe/wolf_rug.json", {"type": "minecraft:crafting_shapeless", "category": "building",
    "ingredients": ["mechanical:wolf_pelt", "mechanical:wolf_pelt", "minecraft:bone"], "result": {"id": "mechanical:wolf_rug", "count": 1}})

# ---- Furred Straw Bed: the vanilla straw bed models, with a wolf pelt spread over them ----
# Model space as the vanilla models with facing=south (no rotation): the foot at z 0..16 with its fringe out at z < 0,
# the head's base z 0..8 (top at y 4) and its pillow z 8..16 (top at y 5).
P, H = "#pelt", "#head"


def box(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def slab(z0, z1, v0, v1):
    """The pelt lying on the straw (top at y 4) with its edges hanging down both long sides."""
    side = {"uv": [0, v0, 3, v1], "texture": P, "rotation": 90}
    return [
        box([0.5, 4, z0], [15.5, 4.75, z1], {"up": {"uv": [0, v0, 16, v1], "texture": P}, "north": {"uv": [0, v0, 15, v0 + 1], "texture": P},
                                             "south": {"uv": [0, v1 - 1, 15, v1], "texture": P}}),
        box([15.5, 1.5, z0], [16.25, 4.75, z1], {"up": {"uv": [15, v0, 16, v1], "texture": P}, "east": side, "west": side,
                                                 "north": {"uv": [15, v0, 16, v0 + 3], "texture": P}, "south": {"uv": [15, v0, 16, v0 + 3], "texture": P}}),
        box([-0.25, 1.5, z0], [0.5, 4.75, z1], {"up": {"uv": [0, v0, 1, v1], "texture": P}, "east": side, "west": side,
                                                "north": {"uv": [0, v0, 1, v0 + 3], "texture": P}, "south": {"uv": [0, v0, 1, v0 + 3], "texture": P}}),
    ]


def all_faces(uv_up, uv_side, tex):
    return {"up": {"uv": uv_up, "texture": tex}, **{f: {"uv": uv_side, "texture": tex} for f in ("north", "south", "east", "west")}}


FOOT = slab(0, 16, 0, 16) + [  # the tail hangs over the foot end, black tip at the bottom
    box([6.75, 0.5, -1], [9.25, 4.75, 0], {"north": {"uv": [13, 10, 15.5, 14.25], "texture": H}, "east": {"uv": [13, 10, 14, 14.25], "texture": H},
                                         "west": {"uv": [13, 10, 14, 14.25], "texture": H}, "up": {"uv": [13, 10, 15.5, 11], "texture": H}}),
]
HEAD = slab(0, 8, 8, 16) + [
    box([2.5, 5, 8], [13.5, 5.5, 10.5], all_faces([0, 10, 11, 12.5], [0, 10, 11, 10.5], H)),  # the scruff over the pillow
    box([5, 5, 9], [11, 6.75, 14], {"up": {"uv": [0, 0, 6, 5], "texture": H}, "north": {"uv": [0, 6, 6, 7.75], "texture": H},
                                    "east": {"uv": [0, 6, 5, 7.75], "texture": H}, "west": {"uv": [0, 6, 5, 7.75], "texture": H},
                                    "south": {"uv": [0, 8, 6, 9.75], "texture": H}}),  # skull, eyes towards the snout
    box([6.5, 5, 14], [9.5, 6.25, 16], {"up": {"uv": [6, 0, 9, 2], "texture": H}, "south": {"uv": [6, 2, 9, 3.25], "texture": H},
                                        "east": {"uv": [6, 4, 8, 5.25], "texture": H}, "west": {"uv": [6, 4, 8, 5.25], "texture": H}}),  # snout
    box([5.25, 6.75, 9.5], [6.75, 8.5, 10.5], all_faces([10, 0, 11.5, 1], [10, 0, 11.5, 1.75], H)),  # ears
    box([9.25, 6.75, 9.5], [10.75, 8.5, 10.5], all_faces([10, 0, 11.5, 1], [10, 0, 11.5, 1.75], H)),
]
TEX = {"pelt": "mechanical:block/furred_straw_bed_pelt", "head": "mechanical:block/furred_straw_bed_head",
       "particle": "mechanical:block/furred_straw_bed_pelt"}
w(A / "models/block/furred_straw_bed_foot_pelt.json", {"textures": TEX, "elements": FOOT})
w(A / "models/block/furred_straw_bed_head_pelt.json", {"textures": TEX, "elements": HEAD})
w(A / "blockstates/furred_straw_bed.json", {"multipart": [
    {"when": {"facing": f, "part": part}, "apply": {"model": model, **({"y": y} if y else {})}}
    for f, y in {"south": 0, "west": 90, "north": 180, "east": 270}.items() for part in ("foot", "head")
    for model in (f"minecraft:block/straw_bed_{part}", f"mechanical:block/furred_straw_bed_{part}_pelt")]})
# broken: the straw bed's own drop (loose fibre) and the pelt back
w(D / "mechanical/loot_table/blocks/furred_straw_bed.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": name, "condition": {"type": "minecraft:match_block", "blocks": "mechanical:furred_straw_bed", "state": {"part": "head"}},
     "modifier": [{"type": "minecraft:set_count", "count": count}]}]}
    for name, count in (("mechanical:plant_fibre", {"type": "minecraft:uniform", "min": 2, "max": 4}), ("mechanical:wolf_pelt", 1))],
    "random_sequence": "mechanical:blocks/furred_straw_bed"})

merge(A / "lang/en_us.json", {**{f"item.mechanical.{k}": v[0] for k, v in ARMOR.items()},
                              "block.mechanical.wolf_rug": "Wolf Rug", "block.mechanical.furred_straw_bed": "Furred Straw Bed",
                              "message.mechanical.furred_straw_bed_rest": "You rest warm under the wolf pelt."})
print("wolf pelt ok")
