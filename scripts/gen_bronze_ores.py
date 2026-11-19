# Bronze Age phase 1 (ores) data: item/block models, loose ore finds, worldgen, Tin Ore drops, panning loot,
# Stone Anvil, recipes, tags, lang. usage: python scripts/gen_bronze_ores.py   (then gen_block_items.py)
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


# ---- items ----
ITEMS = ["malachite", "native_copper", "cassiterite", "crushed_copper_ore", "crushed_tin_ore", "pan", "pan_with_gravel", "pan_with_sand"]
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# ---- loose finds over ore veins: rock models (gen_data.py rock_size1..3) with mineral textures ----
FINDS = {"malachite": "mechanical:block/malachite", "native_copper": "minecraft:block/raw_copper_block", "cassiterite": "mechanical:block/cassiterite"}
for v, tex in FINDS.items():
    block = f"mechanical:loose_{v}"
    variants = {}
    for size in (1, 2, 3):
        w(A / f"models/block/loose_{v}_size{size}.json", {"parent": f"mechanical:block/rock_size{size}", "textures": {"all": tex}})
        variants[f"size={size}"] = [{"model": f"mechanical:block/loose_{v}_size{size}", "y": y} for y in (0, 90, 180, 270)]
    w(A / f"blockstates/loose_{v}.json", {"variants": variants})
    w(D / f"mechanical/loot_table/blocks/loose_{v}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"mechanical:{v}", "modifier": [
            {"type": "minecraft:set_count", "count": s, "condition": {"type": "minecraft:match_block", "blocks": block, "state": {"size": str(s)}}}
            for s in (2, 3)]}]}],
        "random_sequence": f"mechanical:blocks/loose_{v}",
    })

# ---- worldgen: OreIndicators (Java) probes its own chunk; vegetal step runs after underground_ores ----
w(D / "mechanical/worldgen/feature/ore_indicators.json", {"type": "mechanical:ore_indicators"})
w(D / "mechanical/worldgen/placed_feature/ore_indicators.json", {"feature": "mechanical:ore_indicators", "placement": []})
w(D / "mechanical/neoforge/biome_modifier/add_ore_indicators.json", {
    "type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
    "features": "mechanical:ore_indicators", "step": "vegetal_decoration"})

# ---- Tin Ore: 1-2 Cassiterite (fortune), itself with silk touch ----
w(D / "mechanical/loot_table/blocks/tin_ore.json", {
    "type": "minecraft:block",
    "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "mechanical:tin_ore"},
        {"type": "minecraft:item", "name": "mechanical:cassiterite", "modifier": [
            {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}},
            {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
            {"type": "minecraft:explosion_decay"}]}]}]}],
    "random_sequence": "mechanical:blocks/tin_ore"})

# ---- panning: one roll from the base table, plus an extra Cassiterite chance by rivers, beaches and stony shores ----
WATERSIDE = ["minecraft:river", "minecraft:frozen_river", "minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore"]


def item(name, weight):
    return {"type": "minecraft:item", "name": name, "weight": weight}


PANNING = {
    "gravel": [{"type": "minecraft:empty", "weight": 40}, item("minecraft:flint", 22), item("mechanical:stone_chunk", 16),
               item("mechanical:cassiterite", 8), item("mechanical:native_copper", 2)],
    "sand": [{"type": "minecraft:empty", "weight": 55}, item("minecraft:flint", 6), item("mechanical:stone_chunk", 8),
             item("mechanical:cassiterite", 10), item("mechanical:native_copper", 1)],
}
for fill, entries in PANNING.items():
    w(D / f"mechanical/loot_table/gameplay/panning/{fill}.json", {
        "type": "minecraft:archaeology",
        "pools": [
            {"rolls": 1, "entries": entries},
            {"rolls": 1, "condition": {"type": "minecraft:location_check", "predicate": {"biomes": WATERSIDE}},
             "entries": [item("mechanical:cassiterite", 1), {"type": "minecraft:empty", "weight": 3}]}],
        "random_sequence": f"mechanical:gameplay/panning/{fill}"})

# ---- Stone Anvil ----
face = lambda tex, uv, cull=None: {"texture": tex, "uv": uv, **({"cullface": cull} if cull else {})}
def slab(f, t):
    sides = [f[0], 16 - t[1], t[0], 16 - f[1]]
    return {"from": f, "to": t, "faces": {
        "up": face("#top", [f[0], f[2], t[0], t[2]]), "down": face("#bottom", [f[0], f[2], t[0], t[2]], "down"),
        "north": face("#side", sides), "south": face("#side", sides),
        "east": face("#side", [f[2], 16 - t[1], t[2], 16 - f[1]]), "west": face("#side", [f[2], 16 - t[1], t[2], 16 - f[1]])}}
w(A / "models/block/stone_anvil.json", {
    "textures": {"particle": "minecraft:block/smooth_stone", "top": "minecraft:block/smooth_stone",
                 "side": "minecraft:block/stone", "bottom": "minecraft:block/cobblestone"},
    "elements": [slab([1, 0, 2], [15, 5, 14]), slab([2, 0, 1], [14, 4, 15]), slab([3, 5, 3], [12, 5.5, 12])]})
w(A / "blockstates/stone_anvil.json", {"variants": {"": [{"model": "mechanical:block/stone_anvil", "y": y} for y in (0, 90, 180, 270)]}})
w(A / "items/stone_anvil.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/stone_anvil"}})
w(D / "mechanical/loot_table/blocks/stone_anvil.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "mechanical:stone_anvil"}]}],
    "random_sequence": "mechanical:blocks/stone_anvil"})

# ---- tags ----
w(D / "mechanical/tags/item/hammerstones.json", {"values": ["mechanical:flint_pickaxe", "mechanical:crude_stone_pickaxe", "minecraft:stone_pickaxe"]})
# the pickaxe tag is written by gen_data.py (which lists these too); patch the file so both stay in step
pick = D / "minecraft/tags/block/mineable/pickaxe.json"
tag = json.loads(pick.read_text())
for b in ["mechanical:stone_anvil"] + [f"mechanical:loose_{v}" for v in FINDS]:
    if b not in tag["values"]:
        tag["values"].append(b)
w(pick, tag)

# ---- recipes ----
w(D / "mechanical/recipe/pan.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["P P", "TPT"], "key": {"P": "#minecraft:planks", "T": "mechanical:twine"}, "result": {"id": "mechanical:pan"}})
# the Flint Pickaxe works as a hammerstone and stays in the grid (FlintPickaxeItem)
w(D / "mechanical/recipe/stone_anvil.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["RKR", "CCC"], "key": {"R": "#mechanical:rock_chunks", "K": "mechanical:flint_pickaxe", "C": "minecraft:cobblestone"},
    "result": {"id": "mechanical:stone_anvil"}})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text())
lang.update({
    "item.mechanical.malachite": "Malachite", "item.mechanical.native_copper": "Native Copper", "item.mechanical.cassiterite": "Cassiterite",
    "item.mechanical.crushed_copper_ore": "Crushed Copper Ore", "item.mechanical.crushed_tin_ore": "Crushed Tin Ore",
    "item.mechanical.pan": "Pan", "item.mechanical.pan_with_gravel": "Pan with Gravel", "item.mechanical.pan_with_sand": "Pan with Sand",
    "block.mechanical.loose_malachite": "Loose Malachite", "block.mechanical.loose_native_copper": "Loose Native Copper",
    "block.mechanical.loose_cassiterite": "Loose Cassiterite", "block.mechanical.stone_anvil": "Stone Anvil",
    "message.mechanical.pan.need_water": "Pan in water: stand in it or aim at it",
})
w(lang_path, lang)
print("bronze ores ok")
