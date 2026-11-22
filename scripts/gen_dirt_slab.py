"""Dirt Slab, Grass Slab and Dirt Path Slab (living-world plan F11; EarthBlocks, DirtSlabBlock).

Blockstates and models on vanilla textures (the Grass Slab on grassy_slab like Turf, tinted by season, and a snowy look
with snow on top and grass_block_snow sides), loot (all three drop Dirt Slabs, 2 for a double), the recipe 3 Dirt -> 6,
the shovel's block transformer (vanilla's dirt path rule plus Dirt/Grass Slab -> Dirt Path Slab, type kept), tags, lang.

Generator order: after gen_earth.py (grassy_slab models), before gen_block_items.py, gen_unlocks.py, gen_advancements.py
and gen_loot_format.py. Run: python scripts/gen_dirt_slab.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
A, D = ROOT / "src/main/resources/assets/mechanical", ROOT / "src/main/resources/data"
M, V = "mechanical:", "minecraft:block/"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def slab_models(name, textures, parent="minecraft:block/"):
    w(A / f"models/block/{name}.json", {"parent": parent + "slab", "textures": textures})
    w(A / f"models/block/{name}_top.json", {"parent": parent + "slab_top", "textures": textures})


def slab_variants(name, double, extra=""):
    return {f"type=bottom{extra}": {"model": M + f"block/{name}"}, f"type=top{extra}": {"model": M + f"block/{name}_top"},
            f"type=double{extra}": {"model": double}}


dirt = {"bottom": V + "dirt", "top": V + "dirt", "side": V + "dirt", "particle": V + "dirt"}
slab_models("dirt_slab", dirt)
w(A / "blockstates/dirt_slab.json", {"variants": slab_variants("dirt_slab", V + "dirt")})

grass = {"top": V + "grass_block_top", "side": V + "dirt", "bottom": V + "dirt", "overlay": V + "grass_block_side_overlay", "particle": V + "dirt"}
slab_models("grass_slab", grass, M + "block/grassy_")
snowy = {"top": V + "snow", "side": V + "grass_block_snow", "bottom": V + "dirt", "particle": V + "dirt"}
slab_models("grass_slab_snowy", snowy)
w(A / "blockstates/grass_slab.json", {"variants": {**slab_variants("grass_slab", V + "grass_block", ",snowy=false"),
                                                   **slab_variants("grass_slab_snowy", V + "grass_block_snow", ",snowy=true")}})
w(A / "items/grass_slab.json", {"model": {"type": "minecraft:model", "model": M + "block/grass_slab",
                                          "tints": [{"type": "minecraft:grass", "downfall": 1.0, "temperature": 0.5}]}})

path = {"top": V + "dirt_path_top", "side": V + "dirt_path_side", "bottom": V + "dirt", "particle": V + "dirt"}
slab_models("dirt_path_slab", path)
w(A / "blockstates/dirt_path_slab.json", {"variants": slab_variants("dirt_path_slab", V + "dirt_path")})

for b in ("dirt_slab", "grass_slab", "dirt_path_slab"):
    w(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + "dirt_slab", "modifier": [
            {"type": "minecraft:set_count", "count": 2, "condition": {"type": "minecraft:match_block", "blocks": M + b, "state": {"type": "double"}}},
            {"type": "minecraft:explosion_decay"}]}]}], "random_sequence": f"mechanical:blocks/{b}"})

w(D / "mechanical/recipe/dirt_slab.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["###"],
                                           "key": {"#": "minecraft:dirt"}, "result": {"id": M + "dirt_slab", "count": 6}})

# the shovel: vanilla's rule (turns_into_dirt_path with air above) after ours
air_above = {"type": "minecraft:matching_block_tag", "offset": [0, 1, 0], "tag": "minecraft:air"}
w(D / "minecraft/block_transformer/shovel.json", [{"block_state_provider": {"type": "minecraft:rule_based", "rules": [
    {"if_true": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_blocks", "blocks": [M + "dirt_slab", M + "grass_slab"]}, air_above]},
     "then": {"type": "minecraft:copy_properties", "source": {"id": M + "dirt_path_slab"}}},
    {"if_true": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:turns_into_dirt_path"}, air_above]},
     "then": {"id": "minecraft:dirt_path"}}]},
    "disallowed_faces": ["down"], "item_damage_per_use": 1, "sound": "minecraft:item.shovel.flatten"}])

blocks = [M + b for b in ("dirt_slab", "grass_slab", "dirt_path_slab")]
tag_add("minecraft/tags/block/mineable/shovel.json", blocks)
tag_add("minecraft/tags/block/slabs.json", blocks)
tag_add("minecraft/tags/item/slabs.json", blocks)

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({"block.mechanical.dirt_slab": "Dirt Slab", "block.mechanical.grass_slab": "Grass Slab", "block.mechanical.dirt_path_slab": "Dirt Path Slab"})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
