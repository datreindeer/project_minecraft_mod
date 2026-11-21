"""Iron Age ores (IronOres.java; docs/ages/03-iron-age.md "Galena", "Gold", "Limestone", "Salt").

Writes the Galena Ore / Deepslate Galena Ore, Limestone and Rock Salt blocks (models, blockstates, loot, tool tags), the
items (Raw Galena, Lead Ingot, Leaded Cupel, Silver Nugget, Silver Ingot, Flux, Salt, Salted Meat, Salted Fish), the
worldgen (galena veins, limestone blobs like granite, rock salt lenses under dry biomes), crafting recipes (cupel, silver
nuggets <-> ingot, salting, salt-cured leather), turns off vanilla's gold smelting / blasting and nugget crafting (gold is
melted in the Clay Kiln, Java), adds Gold Nuggets to the Pan's loot and writes lang. Kiln recipes are in Java.
Textures: gen_iron_ores_textures.py.
Generator order: ... gen_bog_iron.py, gen_gemstones.py, gen_iron_ores.py, ... gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_loot_format.py, gen_registry.py. Run: python scripts/gen_iron_ores.py
"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M, V = "mechanical:", "minecraft:"
NEVER = {"neoforge:conditions": [{"type": "neoforge:never"}]}


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


# ---- blocks: plain cubes ----
BLOCKS = ["galena_ore", "deepslate_galena_ore", "limestone", "rock_salt"]
for b in BLOCKS:
    w(A / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"mechanical:block/{b}"}})
    w(A / f"blockstates/{b}.json", {"variants": {"": {"model": f"mechanical:block/{b}"}}})

ITEMS = ["raw_galena", "lead_ingot", "leaded_cupel", "silver_nugget", "silver_ingot", "flux", "salt", "salted_meat", "salted_fish"]
for i in ITEMS:
    w(A / f"models/item/{i}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{i}"}})
    w(A / f"items/{i}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{i}"}})


# ---- loot (26.x format) ----
def ore_loot(block, item, count=None):
    drop = [{"type": "minecraft:set_count", "count": count}] if count else []
    w(D / f"mechanical/loot_table/blocks/{block}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": M + block},
            {"type": "minecraft:item", "name": M + item, "modifier": drop + [
                {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                {"type": "minecraft:explosion_decay"}]}]}]}],
        "random_sequence": f"mechanical:blocks/{block}"})


ore_loot("galena_ore", "raw_galena")
ore_loot("deepslate_galena_ore", "raw_galena")
ore_loot("rock_salt", "salt", {"type": "minecraft:uniform", "min": 2, "max": 3})
w(D / "mechanical/loot_table/blocks/limestone.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + "limestone"}]}],
    "random_sequence": "mechanical:blocks/limestone"})

# ---- tags: galena and rock salt need a Bronze Pickaxe (like iron ore), limestone any pickaxe ----
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", *[M + b for b in BLOCKS])
tag_add(D / "mechanical/tags/block/needs_bronze_tool.json", M + "galena_ore", M + "deepslate_galena_ore", M + "rock_salt")
# limestone is a base stone like granite: caves carve it and ores (galena!) can sit in it
tag_add(D / "minecraft/tags/block/base_stone_overworld.json", M + "limestone")
tag_add(D / "minecraft/tags/block/stone_ore_replaceables.json", M + "limestone")
w(D / "mechanical/tags/item/flux.json", {"values": [M + "flux"]})

# ---- worldgen ----
def ore(name, size, targets, discard=0.0):
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:ore", "size": size, "discard_chance_on_air_exposure": discard,
        "targets": [{"state": state, "target": {"predicate_type": "minecraft:tag_match", "tag": tag}} for tag, state in targets]})


def placed(name, feature, count, lo, hi, shape="uniform", rarity=None):
    head = [{"type": "minecraft:rarity_filter", "chance": rarity}] if rarity else [{"type": "minecraft:count", "count": count}]
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": M + feature, "placement": head + [
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": f"minecraft:{shape}", "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
        {"type": "minecraft:biome"}]})


def modifier(name, biomes, features):
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": [M + f for f in features], "step": "underground_ores"})


# galena: small veins in stone and deepslate, everywhere, mostly round y 16 (a little rarer than iron)
ore("ore_galena", 8, [(V + "stone_ore_replaceables", M + "galena_ore"), (V + "deepslate_ore_replaceables", M + "deepslate_galena_ore")])
placed("ore_galena", "ore_galena", 8, -48, 80, "trapezoid")
modifier("galena_ore", "#minecraft:is_overworld", ["ore_galena"])
# limestone: big blobs like granite/diorite (upper ones show on hillsides), plus extra near the surface on cliffs and shores
ore("ore_limestone", 64, [(V + "base_stone_overworld", M + "limestone")])
placed("ore_limestone_upper", "ore_limestone", 1, 64, 128, rarity=6)
placed("ore_limestone_lower", "ore_limestone", 2, 0, 60)
placed("ore_limestone_cliffs", "ore_limestone", 2, 56, 160)
modifier("limestone", "#minecraft:is_overworld", ["ore_limestone_upper", "ore_limestone_lower"])
w(D / "mechanical/tags/worldgen/biome/has_limestone_cliffs.json", {"values": [V + b for b in (
    "stony_shore", "windswept_hills", "windswept_gravelly_hills", "windswept_forest", "meadow", "savanna_plateau")]})
modifier("limestone_cliffs", "#mechanical:has_limestone_cliffs", ["ore_limestone_cliffs"])
# rock salt: lenses of old dried-up seas under dry land
ore("ore_rock_salt", 24, [(V + "stone_ore_replaceables", M + "rock_salt")])
placed("ore_rock_salt", "ore_rock_salt", 4, 0, 72)
w(D / "mechanical/tags/worldgen/biome/has_rock_salt.json", {"values": [V + b for b in (
    "desert", "badlands", "eroded_badlands", "wooded_badlands", "savanna", "savanna_plateau", "windswept_savanna")]})
modifier("rock_salt", "#mechanical:has_rock_salt", ["ore_rock_salt"])


# ---- recipes ----
def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients, "result": {"id": result, "count": count}})


# a bone-ash cupel: the lead soaks into it in the kiln and leaves a bead of silver
shapeless("leaded_cupel", [M + "lead_ingot", V + "bone_meal"], M + "leaded_cupel")
recipe("silver_ingot", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["NNN", "NNN", "NNN"],
                        "key": {"N": M + "silver_nugget"}, "result": {"id": M + "silver_ingot"}})
shapeless("silver_nugget", [M + "silver_ingot"], M + "silver_nugget", 9)
shapeless("salted_meat", ["#mechanical:smokable_meat", M + "salt"], M + "salted_meat")
shapeless("salted_fish", ["#mechanical:smokable_fish", M + "salt"], M + "salted_fish")
shapeless("leather_from_salt", [M + "scraped_hide", M + "salt"], V + "leather")

# gold is melted in the Clay Kiln (1100°), not in vanilla furnaces, and nuggets don't craft into an ingot
for name in [f"gold_ingot_from_{k}_{o}" for k in ("smelting", "blasting") for o in ("raw_gold", "gold_ore", "deepslate_gold_ore", "nether_gold_ore")] \
        + ["gold_ingot_from_nuggets"]:
    w(D / f"minecraft/recipe/{name}.json", NEVER)
    w(D / f"minecraft/advancement/recipes/misc/{name}.json", NEVER)

# ---- panning: rare Gold Nuggets (base pools), a little more by rivers and shores (gen_bronze_ores.py writes the tables) ----
for fill in ("gravel", "sand"):
    path = D / f"mechanical/loot_table/gameplay/panning/{fill}.json"
    table = json.loads(path.read_text(encoding="utf-8"))
    for pool in table["pools"]:
        if not any(e.get("name") == V + "gold_nugget" for e in pool["entries"]):
            pool["entries"].append({"type": "minecraft:item", "name": V + "gold_nugget", "weight": 1})
    w(path, table)

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({
    "block.mechanical.galena_ore": "Galena Ore", "block.mechanical.deepslate_galena_ore": "Deepslate Galena Ore",
    "block.mechanical.limestone": "Limestone", "block.mechanical.rock_salt": "Rock Salt",
    "item.mechanical.raw_galena": "Raw Galena", "item.mechanical.lead_ingot": "Lead Ingot", "item.mechanical.leaded_cupel": "Leaded Cupel",
    "item.mechanical.silver_nugget": "Silver Nugget", "item.mechanical.silver_ingot": "Silver Ingot", "item.mechanical.flux": "Flux",
    "item.mechanical.salt": "Salt", "item.mechanical.salted_meat": "Salted Meat", "item.mechanical.salted_fish": "Salted Fish",
})
w(lang_path, lang)
print("iron ores ok")
