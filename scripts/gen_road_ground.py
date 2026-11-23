"""Trodden road ground (people.RoadBuilding): Packed Sand, Packed Red Sand, Packed Snow and their slabs.

Models (full block on the textures from gen_road_ground_textures.py; slabs on the ground_slab models of gen_dirt_slab),
blockstates, item models, loot (itself; the slab 2 when double), shovel and slab tags, lang. Where they lie naturally:
- Packed Sand: wind-packed flats in deserts and on beaches, small patches in the surface sand;
- Packed Red Sand: the same in badlands;
- Packed Snow: wind-scoured patches on snowy slopes, groves and peaks, in the surface snow.
Small disks (vanilla's disk feature) in the top block only, rare.

Generator order: after gen_dirt_slab.py (the ground_slab models), before gen_blend_edges.py, gen_block_items.py and
gen_loot_format.py. Run: python scripts/gen_road_ground.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A = RES / "assets/mechanical"
D = RES / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    p = D / rel
    have = json.loads(p.read_text(encoding="utf-8")) if p.exists() else {"values": []}
    for v in values:
        if v not in have["values"]:
            have["values"].append(v)
    w(p, have)


BLOCKS = ["packed_sand", "packed_red_sand", "packed_snow"]
for b in BLOCKS:
    tex = M + "block/" + b
    w(A / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": tex}})
    w(A / f"blockstates/{b}.json", {"variants": {"": {"model": M + "block/" + b}}})
    w(A / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": M + "block/" + b}})
    w(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                                                     "entries": [{"type": "minecraft:item", "name": M + b}]}], "random_sequence": f"mechanical:blocks/{b}"})
    s = b + "_slab"
    cube = {"top": tex, "side": tex, "bottom": tex, "particle": tex}
    w(A / f"models/block/{s}.json", {"parent": M + "block/ground_slab", "textures": cube})
    w(A / f"models/block/{s}_top.json", {"parent": "minecraft:block/slab_top", "textures": cube})
    w(A / f"blockstates/{s}.json", {"variants": {"type=bottom": {"model": M + f"block/{s}"}, "type=top": {"model": M + f"block/{s}_top"},
                                                 "type=double": {"model": M + "block/" + b}}})
    w(A / f"items/{s}.json", {"model": {"type": "minecraft:model", "model": M + f"block/{s}"}})
    w(D / f"mechanical/loot_table/blocks/{s}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + s, "modifier": [
            {"type": "minecraft:set_count", "count": 2, "condition": {"type": "minecraft:match_block", "blocks": M + s, "state": {"type": "double"}}},
            {"type": "minecraft:explosion_decay"}]}]}], "random_sequence": f"mechanical:blocks/{s}"})
    w(D / f"mechanical/recipe/{s}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["###"],
                                          "key": {"#": M + b}, "result": {"id": M + s, "count": 6}})

ids = [M + b for b in BLOCKS]
slabs = [M + b + "_slab" for b in BLOCKS]
tag_add("minecraft/tags/block/mineable/shovel.json", ids + slabs)
tag_add("minecraft/tags/block/slabs.json", slabs)
tag_add("minecraft/tags/item/slabs.json", slabs)

# where they lie naturally: small disks in the top block (the origin is the air just above the ground)
PATCHES = {
    "packed_sand_flats": ("packed_sand", ["minecraft:sand"], ["minecraft:desert", "minecraft:beach"], 5, (2, 4)),
    "packed_red_sand_flats": ("packed_red_sand", ["minecraft:red_sand"], "#minecraft:is_badlands", 6, (2, 3)),
    "packed_snow_scours": ("packed_snow", ["minecraft:snow_block"], ["minecraft:snowy_slopes", "minecraft:grove", "minecraft:frozen_peaks",
                                                                    "minecraft:jagged_peaks"], 6, (2, 3)),
}
for name, (block, target, biomes, chance, (rmin, rmax)) in PATCHES.items():
    w(D / f"mechanical/worldgen/feature/{name}.json", {
        "type": "minecraft:disk", "half_height": 1, "radius": {"type": "minecraft:uniform", "min_inclusive": rmin, "max_inclusive": rmax},
        "state_provider": {"type": "minecraft:rule_based", "fallback": {"id": M + block}, "rules": []},
        "target": {"type": "minecraft:matching_blocks", "blocks": target}})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + name, "placement": [
        {"type": "minecraft:rarity_filter", "chance": chance}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
                                                                    "features": [M + name], "step": "local_modifications"})

lang_path = A / "lang/en_us.json"
text = lang_path.read_text(encoding="utf-8")
have = json.loads(text)
names = {"packed_sand": "Packed Sand", "packed_red_sand": "Packed Red Sand", "packed_snow": "Packed Snow"}
LANG = {}
for b, n in names.items():
    LANG[f"block.mechanical.{b}"] = n
    LANG[f"block.mechanical.{b}_slab"] = n + " Slab"
add = "".join(f",\n  {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in LANG.items() if k not in have)
if add:
    end = text.rstrip().rfind("}")
    text = text[:end].rstrip() + add + "\n}\n"
    lang_path.write_text(text, encoding="utf-8")
print("ok")
