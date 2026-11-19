"""Data for the cold hunters (wildlife/coldhunters/ColdhuntersFauna.java): Wolverine, Lynx, Ermine, Lemming, Pine Marten.

- items: raw/cooked meat of each, raw pelts and cured furs, Lemming Pelt, Wolverine Claw, Claw Flesher, Wolverine-Trimmed
  Hood, Ermine Mantle, Marten Fur Cap, Lynx Rug, spawn eggs (models, lang)
- recipes: raw meat on the Campfire (20 s) or in the smoker; the goods in the crafting grid; 4 Lemming Pelts + Twine into a
  Raw Hide (like the squirrel's)
- tags: raw meat perishes in 1 day and cooked in 3 (on top of gen_preserving.py's), all rot to flesh, raw meat is smokable
  and dryable; armour slot tags, #mechanical:fur_insulation (warm like Fur, Temperature.java), the hood in
  #minecraft:freeze_immune_wearables (no freezing damage), repair tags; the Claw Flesher is a knife
- equipment assets for the three worn pieces; the Lynx Rug block (the Bear Rug's model, own texture), loot and recipe
- spawns: one biome modifier per animal (vanilla cold/forest biomes and the mod's Pine Heath / Deciduous Forest)
Textures: scripts/gen_fauna_coldhunters_textures.py. Butchering yields are registered in ColdhuntersFauna.init().
Generator order: ... gen_kettle.py, gen_roots.py, gen_fauna_coldhunters.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py (it adds to the perish tags that gen_preserving.py rewrites).
Run: python scripts/gen_fauna_coldhunters.py"""
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


ANIMALS = {"wolverine": "Wolverine", "lynx": "Lynx", "ermine": "Ermine", "lemming": "Lemming", "pine_marten": "Pine Marten"}
# meat: raw_<stem> / cooked_<stem> -> display name
MEATS = {"wolverine": "Wolverine", "lynx": "Lynx", "ermine": "Ermine", "lemming": "Lemming", "marten": "Marten"}
ITEMS = {  # id -> English name
    "raw_wolverine_pelt": "Raw Wolverine Pelt", "wolverine_fur": "Wolverine Fur", "wolverine_claw": "Wolverine Claw",
    "raw_lynx_pelt": "Raw Lynx Pelt", "lynx_pelt": "Lynx Pelt",
    "raw_ermine_pelt": "Raw Ermine Pelt", "ermine_fur": "Ermine Fur",
    "raw_marten_pelt": "Raw Marten Pelt", "marten_fur": "Marten Fur",
    "lemming_pelt": "Lemming Pelt",
    "wolverine_trimmed_hood": "Wolverine-Trimmed Hood", "ermine_mantle": "Ermine Mantle", "marten_fur_cap": "Marten Fur Cap",
}
ARMOR = {"wolverine_trimmed_hood": ("head_armor", "wolverine_fur"), "ermine_mantle": ("chest_armor", "ermine_fur"),
         "marten_fur_cap": ("head_armor", "marten_fur")}

lang = {"block.mechanical.lynx_rug": "Lynx Rug", "item.mechanical.claw_flesher": "Claw Flesher"}

# ---- entities and spawn eggs ----
for a, name in ANIMALS.items():
    lang[f"entity.mechanical.{a}"] = name
    lang[f"item.mechanical.{a}_spawn_egg"] = f"{name} Spawn Egg"
    flat_item(f"{a}_spawn_egg")

# ---- meat: campfire and smoker, spoilage, smoking/drying racks ----
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
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_meat", *RAW)

# ---- pelts, parts and goods ----
for item, name in ITEMS.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
flat_item("claw_flesher", "minecraft:item/handheld")
for item, (slot, repair) in ARMOR.items():
    item_tag(f"minecraft:{slot}", M + item)
    item_tag("fur_insulation", M + item)
    w(D / f"mechanical/tags/item/repairs_{repair}.json", {"values": [M + repair]})
    w(A / f"equipment/{item}.json", {"layers": {"humanoid": [{"texture": f"mechanical:{item}"}]}})
item_tag("minecraft:freeze_immune_wearables", M + "wolverine_trimmed_hood")
w(D / "mechanical/tags/item/repairs_claw_flesher.json", {"values": [M + "wolverine_claw"]})
item_tag("knives", M + "claw_flesher")

shapeless("wolverine_trimmed_hood", [M + "fur_hood", M + "wolverine_fur", M + "wolverine_fur", M + "sinew"], M + "wolverine_trimmed_hood",
          category="equipment")
shapeless("claw_flesher", [M + "wolverine_claw", M + "wolverine_claw", MC + "stick", M + "sinew"], M + "claw_flesher", category="equipment")
w(D / "mechanical/recipe/ermine_mantle.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["E E", "ELE", "ESE"], "key": {"E": M + "ermine_fur", "L": MC + "leather", "S": M + "sinew"},
    "result": {"id": M + "ermine_mantle", "count": 1}})
w(D / "mechanical/recipe/marten_fur_cap.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["MMM", "M M", " S "], "key": {"M": M + "marten_fur", "S": M + "sinew"},
    "result": {"id": M + "marten_fur_cap", "count": 1}})
shapeless("raw_hide_from_lemming_pelts", [M + "lemming_pelt"] * 4 + [M + "twine"], M + "raw_hide")

# ---- Lynx Rug: flat like the Bear Rug (same block class and model shape), head away from whoever places it ----
w(A / "models/block/lynx_rug.json", {"parent": "mechanical:block/bear_rug",
    "textures": {"particle": "mechanical:block/lynx_rug", "top": "mechanical:block/lynx_rug"}})
w(A / "blockstates/lynx_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/lynx_rug", **({"y": y} if y else {})}
    for f, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}})
flat_item("lynx_rug")
w(D / "mechanical/loot_table/blocks/lynx_rug.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "mechanical:lynx_rug"}]}], "random_sequence": "mechanical:blocks/lynx_rug"})
shapeless("lynx_rug", [M + "lynx_pelt", M + "lynx_pelt", MC + "bone"], M + "lynx_rug", category="building")

# ---- spawns ----
SNOW = [MC + "snowy_plains", MC + "snowy_taiga", MC + "grove", MC + "snowy_slopes"]
TAIGA = [MC + "taiga", MC + "old_growth_spruce_taiga", MC + "old_growth_pine_taiga", M + "pine_heath"]
FOREST = [MC + "forest", MC + "birch_forest", MC + "old_growth_birch_forest", MC + "dark_forest", M + "deciduous_forest"]
SPAWNS = {  # animal -> (biomes, weight, min, max)
    "wolverine": (SNOW, 2, 1, 1),
    "lynx": (TAIGA + [MC + "snowy_taiga", MC + "dark_forest"], 2, 1, 2),
    "ermine": (SNOW + TAIGA, 4, 1, 2),
    "lemming": (SNOW + [MC + "ice_spikes"], 8, 3, 6),
    "pine_marten": (FOREST + TAIGA + [MC + "snowy_taiga"], 4, 1, 1),
}
for a, (biomes, weight, lo, hi) in SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{a}.json", {"type": "neoforge:add_spawns", "biomes": biomes, "spawners": [
        {"type": M + a, "weight": weight, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

merge(A / "lang/en_us.json", lang)
print("fauna coldhunters ok")
