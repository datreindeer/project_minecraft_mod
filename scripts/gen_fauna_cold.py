"""Data for the cold fauna (wildlife/cold/ColdFauna.java): Polar Bear (in place of vanilla's), Arctic Fox, Snow Leopard.

- items: raw/cooked meat of each, Polar Bear Liver, raw pelts and cured pelts/fur, Polar Bear Cloak, Arctic Fox Hat,
  Snow Leopard Boots, Polar Bear Rug, Snow Leopard Rug, spawn eggs (models, lang)
- recipes: raw meat on the Campfire (20 s) or in the smoker; the goods in the crafting grid
- tags: raw meat perishes in 1 day and cooked in 3 (on top of gen_preserving.py's), all rot to flesh, raw meat is smokable;
  armour slot tags, #mechanical:fur_insulation (warm like Fur, Temperature.java), repair tags
- equipment assets for the three worn pieces; the two rugs (the Bear Rug's model, own texture), loot and recipes
- spawns: one biome modifier per animal, and vanilla's polar bear removed from every overworld biome
Textures: scripts/gen_fauna_cold_textures.py. Butchering yields are registered in ColdFauna.init().
Generator order: in the gen_fauna_* step (alphabetical: after gen_fauna_coast.py, before gen_fauna_coldhunters.py).
Run: python scripts/gen_fauna_cold.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M, MC = "mechanical:", "minecraft:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    tag_add(D / f"{ns}/tags/item/{path}.json", *values)


def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def shapeless(name, ingredients, result, count=1, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
        "ingredients": ingredients, "result": {"id": result, "count": count}})


ANIMALS = {"polar_bear": "Polar Bear", "arctic_fox": "Arctic Fox", "snow_leopard": "Snow Leopard"}
MEATS = {"polar_bear": "Polar Bear", "arctic_fox": "Arctic Fox", "snow_leopard": "Snow Leopard"}  # raw_<x> / cooked_<x>
ITEMS = {  # id -> English name
    "polar_bear_liver": "Polar Bear Liver",
    "raw_polar_bear_pelt": "Raw Polar Bear Pelt", "polar_bear_pelt": "Polar Bear Pelt",
    "raw_arctic_fox_pelt": "Raw Arctic Fox Pelt", "arctic_fox_fur": "Arctic Fox Fur",
    "raw_snow_leopard_pelt": "Raw Snow Leopard Pelt", "snow_leopard_pelt": "Snow Leopard Pelt",
    "polar_bear_cloak": "Polar Bear Cloak", "arctic_fox_hat": "Arctic Fox Hat", "snow_leopard_boots": "Snow Leopard Boots",
}
ARMOR = {"polar_bear_cloak": ("chest_armor", "polar_bear_pelt"), "arctic_fox_hat": ("head_armor", "arctic_fox_fur"),
         "snow_leopard_boots": ("foot_armor", "snow_leopard_pelt")}
RUGS = {"polar_bear_rug": ("Polar Bear Rug", "polar_bear_pelt", 1), "snow_leopard_rug": ("Snow Leopard Rug", "snow_leopard_pelt", 2)}

lang = {}

# ---- entities and spawn eggs ----
for a, name in ANIMALS.items():
    lang[f"entity.mechanical.{a}"] = name
    lang[f"item.mechanical.{a}_spawn_egg"] = f"{name} Spawn Egg"
    flat_item(f"{a}_spawn_egg")

# ---- meat: campfire and smoker, spoilage, smoking racks ----
RAW, COOKED = [], []
for m, name in MEATS.items():
    raw, cooked = f"raw_{m}", f"cooked_{m}"
    for item, label in ((raw, f"Raw {name}"), (cooked, f"Cooked {name}")):
        flat_item(item)
        lang[f"item.mechanical.{item}"] = label
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        w(D / f"mechanical/recipe/{cooked}_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
            "cookingtime": ticks, "experience": 0.1, "ingredient": M + raw, "result": {"id": M + cooked}})
    RAW.append(M + raw)
    COOKED.append(M + cooked)
item_tag("perishes_1_day", *RAW, M + "polar_bear_liver")
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED, M + "polar_bear_liver")
item_tag("smokable_meat", *RAW)

# ---- pelts and goods ----
for item, name in ITEMS.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
for item, (slot, repair) in ARMOR.items():
    item_tag(f"minecraft:{slot}", M + item)
    item_tag("fur_insulation", M + item)
    w(D / f"mechanical/tags/item/repairs_{repair}.json", {"values": [M + repair]})
    w(A / f"equipment/{item}.json", {"layers": {"humanoid": [{"texture": f"mechanical:{item}"}]}})

shapeless("polar_bear_cloak", [M + "polar_bear_pelt", M + "polar_bear_pelt", MC + "leather", M + "sinew"], M + "polar_bear_cloak",
          category="equipment")
shapeless("arctic_fox_hat", [M + "arctic_fox_fur"] * 3 + [M + "sinew"], M + "arctic_fox_hat", category="equipment")
shapeless("snow_leopard_boots", [MC + "leather_boots", M + "snow_leopard_pelt", M + "snow_leopard_pelt", M + "sinew"],
          M + "snow_leopard_boots", category="equipment")

# ---- rugs: flat like the Bear Rug (same block class and model shape), head away from whoever places it ----
for rug, (name, pelt, n) in RUGS.items():
    lang[f"block.mechanical.{rug}"] = name
    w(A / f"models/block/{rug}.json", {"parent": "mechanical:block/bear_rug",
        "textures": {"particle": f"mechanical:block/{rug}", "top": f"mechanical:block/{rug}"}})
    w(A / f"blockstates/{rug}.json", {"variants": {f"facing={f}": {"model": f"mechanical:block/{rug}", **({"y": y} if y else {})}
        for f, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}})
    flat_item(rug)
    w(D / f"mechanical/loot_table/blocks/{rug}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + rug}]}], "random_sequence": f"mechanical:blocks/{rug}"})
    shapeless(rug, [M + pelt] * n + [MC + "bone"], M + rug, category="building")

# ---- spawns ----
ICE = [MC + "snowy_plains", MC + "ice_spikes", MC + "snowy_beach", M + "glacial_moraine"]
PEAKS = [MC + "frozen_peaks", MC + "jagged_peaks", MC + "stony_peaks"]
SPAWNS = {  # animal -> (biomes, weight, min, max)
    "polar_bear": (ICE + [MC + "frozen_ocean", MC + "deep_frozen_ocean"], 1, 1, 2),
    "arctic_fox": (ICE, 4, 1, 2),
    "snow_leopard": (PEAKS, 1, 1, 1),
}
for a, (biomes, weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{a}.json", {"type": "neoforge:add_spawns", "biomes": biomes, "spawners": [
        {"type": M + a, "weight": weight, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})
# ours takes the place of vanilla's everywhere (ColdFauna.onJoin swaps any that still turn up)
w(D / "mechanical/neoforge/biome_modifier/remove_vanilla_polar_bears.json", {"type": "neoforge:remove_spawns",
    "biomes": "#minecraft:is_overworld", "entity_types": MC + "polar_bear"})

merge(A / "lang/en_us.json", lang)
print("fauna cold ok")
