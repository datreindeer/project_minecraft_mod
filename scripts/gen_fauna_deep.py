"""Data for the deep fauna (wildlife/deep/DeepFauna.java): Cave Salamander (olm), Blind Cave Fish, Bat Swarm, Cavern Spider
and its Cave Web.

- item models (flat items; the Cave Web item shows the block texture), lang (entities, items, spawn eggs, the web)
- the Cave Web block: blockstate, cross model, loot (whole with shears or a knife, otherwise String), sword-efficient tag
- loot: the olm drops itself, the fish itself, the swarm Bat Wings and Guano (VanillaGame's; none leave a carcass); the spider is butchered
- recipes: Dark-Sight Broth (olm + Glow Lichen + Bowl), cave fish and spider legs on the Campfire / in the smoker, Silk
  Gland + knife -> 3 String, 8 Arrows round a Cavern Spider Fang -> 8 Arrows of Slowness
- tags: spoilage / smoking
- fishing: Blind Cave Fish join the fish pool of gameplay/fishing/fish (gen_data.py writes it) where the air over the hook is
  out of the sky below y 50 (a cave lake); there it is the commonest catch
No biome modifiers: the olm, fish and swarm come from DeepFauna's own spawners, the spider from Underground's. The Mammoth
Steppe's spawns are in gen_world_biomes.py.
Textures: scripts/gen_fauna_deep_textures.py. Docs: docs/fauna/deep.md.
Generator order: ... gen_roots.py, gen_fauna_*.py (alphabetical), gen_block_items.py, gen_unlocks.py, gen_advancements.py,
gen_registry.py.
Run: python scripts/gen_fauna_deep.py"""
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
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")  # the lang file is kept ASCII-escaped


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def item_tag(name, *values):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    tag_add(D / f"{ns}/tags/item/{path}.json", *values)


def flat_item(item, texture=None):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": texture or f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


def cook(name, ingredient, result, ticks=400):
    for kind in ("campfire_cooking", "smoking"):
        recipe(f"{name}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food",
               "cookingtime": ticks if kind == "campfire_cooking" else ticks // 4, "experience": 0.1,
               "ingredient": ingredient, "result": {"id": result, "count": 1}})


def loot(name, *pools):
    w(D / f"mechanical/loot_table/entities/{name}.json", {"type": "minecraft:entity", "pools": list(pools),
                                                          "random_sequence": f"mechanical:entities/{name}"})


def count(lo, hi):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


ANIMALS = {"cave_salamander": "Cave Salamander", "blind_cave_fish": "Blind Cave Fish", "bat_swarm": "Bat Swarm",
           "cavern_spider": "Cavern Spider"}
ITEMS = {"cave_salamander": "Cave Salamander", "dark_sight_broth": "Dark-Sight Broth", "blind_cave_fish": "Blind Cave Fish",
         "cooked_cave_fish": "Cooked Cave Fish", "spider_leg": "Spider Leg",
         "roasted_spider_leg": "Roasted Spider Leg", "silk_gland": "Silk Gland", "cavern_spider_fang": "Cavern Spider Fang"}

lang = {f"entity.mechanical.{k}": v for k, v in ANIMALS.items()}
lang.update({f"item.mechanical.{k}_spawn_egg": f"{v} Spawn Egg" for k, v in ANIMALS.items()})
lang.update({f"item.mechanical.{k}": v for k, v in ITEMS.items()})
lang["block.mechanical.cave_web"] = "Cave Web"

for k in ITEMS:
    flat_item(k)
for k in ANIMALS:
    flat_item(f"{k}_spawn_egg")

# ---- Cave Web: a cross like the cobweb; whole with shears or a knife, String otherwise ----
w(A / "models/block/cave_web.json", {"parent": "minecraft:block/cross", "textures": {"cross": "mechanical:block/cave_web"}})
w(A / "blockstates/cave_web.json", {"variants": {"": {"model": "mechanical:block/cave_web"}}})
flat_item("cave_web", "mechanical:block/cave_web")
w(D / "mechanical/loot_table/blocks/cave_web.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": M + "cave_web", "condition": {"type": "minecraft:any_of", "terms": [
            "minecraft:tool/can_shear", "minecraft:tool/can_silk_touch",
            {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:knives"}}]}},
        {"type": "minecraft:item", "name": MC + "string", "condition": {"type": "minecraft:survives_explosion"}}]}]}],
    "random_sequence": "mechanical:blocks/cave_web"})
tag_add(D / "minecraft/tags/block/sword_efficient.json", M + "cave_web")

# ---- loot of the ones without a carcass ----
loot("cave_salamander", {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "cave_salamander"}]})
loot("blind_cave_fish", {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "blind_cave_fish"}]},
     {"rolls": 1, "condition": {"type": "minecraft:random_chance", "chance": 0.05},
      "entries": [{"type": "minecraft:item", "name": MC + "bone_meal"}]})
loot("bat_swarm", {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "bat_wing", "modifier": [count(1, 3)]}]},
     {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "guano", "modifier": [count(1, 2)]}]})

# ---- recipes ----
cook("cooked_cave_fish", M + "blind_cave_fish", M + "cooked_cave_fish", 300)
cook("roasted_spider_leg", M + "spider_leg", M + "roasted_spider_leg", 300)
shapeless("dark_sight_broth", [M + "cave_salamander", MC + "glow_lichen", MC + "bowl"], M + "dark_sight_broth", category="misc")
shapeless("string_from_silk_gland", [M + "silk_gland", "#mechanical:knives"], MC + "string", 3)
recipe("slowness_arrows_from_cavern_spider_fang", {"type": "minecraft:crafting_shaped", "category": "equipment",
       "pattern": ["AAA", "AFA", "AAA"], "key": {"A": MC + "arrow", "F": M + "cavern_spider_fang"},
       "result": {"id": MC + "tipped_arrow", "count": 8, "components": {"minecraft:potion_contents": {"potion": "minecraft:slowness"}}}})

# ---- tags ----
RAW = [M + "blind_cave_fish", M + "spider_leg", M + "cave_salamander"]
COOKED = [M + "cooked_cave_fish", M + "roasted_spider_leg"]
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_fish", M + "blind_cave_fish")
item_tag("smokable_meat", M + "spider_leg")

# ---- fishing: in a cave lake the blind fish bite most. The block over the hook (offsetY 1: the air above the water;
# the water itself dims the sky light, so it never "sees the sky") out of the sky, below y 50 (Temperature.CAVE_TOP) ----
fish_path = D / "minecraft/loot_table/gameplay/fishing/fish.json"
fish = json.loads(fish_path.read_text(encoding="utf-8"))
entries = [e for e in fish["pools"][0]["entries"] if e["name"] != M + "blind_cave_fish"]
entries.append({"type": "minecraft:item", "name": M + "blind_cave_fish", "weight": 150, "condition": {
    "type": "minecraft:location_check", "offsetY": 1, "predicate": {"can_see_sky": False, "position": {"y": {"max": 50}}}}})
fish["pools"][0]["entries"] = entries
w(fish_path, fish)

merge(A / "lang/en_us.json", lang)
print("fauna deep ok")
