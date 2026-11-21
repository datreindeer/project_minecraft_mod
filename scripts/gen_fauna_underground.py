"""Data for the underground fauna (wildlife/underground/UndergroundFauna.java): the Giant Centipede and the creatures of Nordic
folklore (Vittra, Troll, Lindworm: second half of the file). Cave Hyena and Cave Bear are MountainFauna's (scripts/gen_fauna_mountain.py); underground they spawn by the darkness rule in Underground.java, which
needs no data.

- items: Raw/Cooked Centipede, Centipede Chitin, Centipede Fang, Centipede Venom Sac, Chitin Vest, the spawn egg (models, lang)
- recipes: raw meat on the Campfire (20 s) or in the smoker; Chitin Vest (7 chitin + sinew); 8 Arrows round a venom sac ->
  8 Arrows of Poison; venom sac + lard + plant fibre -> 2 Snakebite Poultice. (Centipede Fang -> 4 Barbed Arrow is a
  Fletching Table recipe: FletchingRecipes.java.)
- tags: raw meat perishes in 1 day and cooked in 3 (on top of gen_preserving.py's), both rot to flesh, raw is smokable and
  dryable; the vest is chest armour, repaired with chitin; its equipment asset
- no biome modifiers: the centipede only comes from Underground's spawner
Textures: scripts/gen_fauna_underground_textures.py. Butchering yield: UndergroundFauna.init().
Generator order: ... gen_fauna_river.py, gen_fauna_underground.py, gen_fauna_warm.py ... (gen_fauna_* run alphabetically
after gen_roots.py, before gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py).
Run: python scripts/gen_fauna_underground.py"""
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
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def item_tag(name, *values):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    p = D / f"{ns}/tags/item/{path}.json"
    old = json.loads(p.read_text(encoding="utf-8")) if p.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(p, old)


def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


ITEMS = {"raw_centipede": "Raw Centipede", "cooked_centipede": "Cooked Centipede", "centipede_chitin": "Centipede Chitin",
         "centipede_fang": "Centipede Fang", "centipede_venom_sac": "Centipede Venom Sac", "chitin_vest": "Chitin Vest",
         "giant_centipede_spawn_egg": "Giant Centipede Spawn Egg"}
lang = {"entity.mechanical.giant_centipede": "Giant Centipede"}
for item, name in ITEMS.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name

# ---- meat ----
for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
    w(D / f"mechanical/recipe/cooked_centipede_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
        "cookingtime": ticks, "experience": 0.1, "ingredient": M + "raw_centipede", "result": {"id": M + "cooked_centipede"}})
item_tag("perishes_1_day", M + "raw_centipede")
item_tag("perishes_3_days", M + "cooked_centipede")
item_tag("rots_to_flesh", M + "raw_centipede", M + "cooked_centipede")
item_tag("smokable_meat", M + "raw_centipede")

# ---- Chitin Vest ----
item_tag("minecraft:chest_armor", M + "chitin_vest")
w(D / "mechanical/tags/item/repairs_centipede_chitin.json", {"values": [M + "centipede_chitin"]})
w(A / "equipment/chitin_vest.json", {"layers": {"humanoid": [{"texture": "mechanical:chitin_vest"}]}})
w(D / "mechanical/recipe/chitin_vest.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["C C", "CSC", "CCC"], "key": {"C": M + "centipede_chitin", "S": M + "sinew"},
    "result": {"id": M + "chitin_vest", "count": 1}})

# ---- venom: poisoned arrows, and the antidote ----
w(D / "mechanical/recipe/poison_arrows_from_centipede_venom_sac.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["AAA", "AVA", "AAA"], "key": {"A": MC + "arrow", "V": M + "centipede_venom_sac"},
    "result": {"id": MC + "tipped_arrow", "count": 8, "components": {"minecraft:potion_contents": {"potion": "minecraft:poison"}}}})
w(D / "mechanical/recipe/snakebite_poultice_from_centipede_venom_sac.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "centipede_venom_sac", M + "lard", M + "plant_fibre"], "result": {"id": M + "snakebite_poultice", "count": 2}})

# ================================================================ the folk of the dark: Vittra, Troll, Lindworm
# - items: their meats, hides, parts and what is made of them; Troll Stone (the block daylight leaves of a troll); spawn eggs
# - recipes: meats on the Campfire / in the smoker; Vittra Hood; Vittra Eye + Glass Bottle -> Potion of Night Vision;
#   Trollhide Coat; Troll Tusk Knife; 2 Troll Hair -> 3 Twine; the four Lindworm Scale armour pieces; Lindworm Fang Blade;
#   8 Arrows round a Lindworm Venom Gland -> 8 Arrows of Strong Poison
# - tags: perishing/rotting/smoking meats, armour slots, repair materials, knives/swords
# - loot: mechanical:entities/lindworm_hoard (rolled by Lindworm#die), Troll Stone drops itself
FOLK = {"vittra_meat": "Vittra Meat", "cooked_vittra_meat": "Cooked Vittra Meat", "vittra_hide": "Vittra Hide",
        "vittra_eye": "Vittra Eye", "vittra_hood": "Vittra Hood",
        "troll_meat": "Troll Meat", "cooked_troll_meat": "Cooked Troll Meat", "troll_hide": "Troll Hide", "troll_hair": "Troll Hair",
        "troll_tusk": "Troll Tusk", "trollhide_coat": "Trollhide Coat", "troll_tusk_knife": "Troll Tusk Knife",
        "lindworm_meat": "Lindworm Meat", "cooked_lindworm_meat": "Cooked Lindworm Meat", "lindworm_scale": "Lindworm Scale",
        "lindworm_fang": "Lindworm Fang", "lindworm_venom_gland": "Lindworm Venom Gland", "lindworm_heart": "Lindworm Heart",
        "lindworm_scale_helmet": "Lindworm Scale Helmet", "lindworm_scale_chestplate": "Lindworm Scale Chestplate",
        "lindworm_scale_leggings": "Lindworm Scale Leggings", "lindworm_scale_boots": "Lindworm Scale Boots",
        "lindworm_fang_blade": "Lindworm Fang Blade",
        "vittra_spawn_egg": "Vittra Spawn Egg", "troll_spawn_egg": "Troll Spawn Egg", "lindworm_spawn_egg": "Lindworm Spawn Egg"}
HANDHELD = {"troll_tusk_knife", "lindworm_fang_blade"}
lang.update({"entity.mechanical.vittra": "Vittra", "entity.mechanical.troll": "Troll", "entity.mechanical.lindworm": "Lindworm",
             "block.mechanical.troll_stone": "Troll Stone"})
for item, name in FOLK.items():
    flat_item(item, "minecraft:item/handheld" if item in HANDHELD else "minecraft:item/generated")
    lang[f"item.mechanical.{item}"] = name


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shaped(name, pattern, key, result, count=1, category="equipment", components=None):
    res = {"id": result, "count": count}
    if components:
        res["components"] = components
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key, "result": res})


# ---- meats ----
for raw, cooked in (("vittra_meat", "cooked_vittra_meat"), ("troll_meat", "cooked_troll_meat"), ("lindworm_meat", "cooked_lindworm_meat")):
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food", "cookingtime": ticks, "experience": 0.2,
                                         "ingredient": M + raw, "result": {"id": M + cooked}})
    item_tag("perishes_1_day", M + raw)
    item_tag("perishes_3_days", M + cooked)
    item_tag("rots_to_flesh", M + raw, M + cooked)
    item_tag("smokable_meat", M + raw)

# ---- vittra ----
shaped("vittra_hood", ["HHH", "H H"], {"H": M + "vittra_hide"}, M + "vittra_hood")
recipe("night_vision_potion_from_vittra_eye", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "vittra_eye", MC + "glass_bottle"],
    "result": {"id": MC + "potion", "count": 1, "components": {"minecraft:potion_contents": {"potion": "minecraft:night_vision"}}}})
item_tag("minecraft:head_armor", M + "vittra_hood")
item_tag("repairs_vittra_hide", M + "vittra_hide")
w(A / "equipment/vittra_hood.json", {"layers": {"humanoid": [{"texture": "mechanical:vittra_hood"}]}})

# ---- troll ----
shaped("trollhide_coat", ["H H", "HSH", "HHH"], {"H": M + "troll_hide", "S": M + "sinew"}, M + "trollhide_coat")
shaped("troll_tusk_knife", ["T ", "WS"], {"T": M + "troll_tusk", "W": M + "twine", "S": MC + "stick"}, M + "troll_tusk_knife")
recipe("twine_from_troll_hair", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "troll_hair", M + "troll_hair"], "result": {"id": M + "twine", "count": 3}})
item_tag("minecraft:chest_armor", M + "trollhide_coat")
item_tag("repairs_troll_hide", M + "troll_hide")
item_tag("troll_tusk_tool_materials", M + "troll_tusk")
item_tag("knives", M + "troll_tusk_knife")
item_tag("minecraft:swords", M + "troll_tusk_knife")
w(A / "equipment/trollhide_coat.json", {"layers": {"humanoid": [{"texture": "mechanical:trollhide_coat"}]}})
# Troll Stone: a plain stone block that drops itself
w(A / "blockstates/troll_stone.json", {"variants": {"": {"model": "mechanical:block/troll_stone"}}})
w(A / "models/block/troll_stone.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "mechanical:block/troll_stone"}})
w(A / "items/troll_stone.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/troll_stone"}})
w(D / "mechanical/loot_table/blocks/troll_stone.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + "troll_stone"}]}],
    "random_sequence": "mechanical:blocks/troll_stone"})
_bt = D / "minecraft/tags/block/mineable/pickaxe.json"
_old = json.loads(_bt.read_text(encoding="utf-8"))
if M + "troll_stone" not in _old["values"]:
    _old["values"].append(M + "troll_stone")
    w(_bt, _old)

# ---- lindworm ----
SC = {"S": M + "lindworm_scale"}
shaped("lindworm_scale_helmet", ["SSS", "S S"], SC, M + "lindworm_scale_helmet")
shaped("lindworm_scale_chestplate", ["S S", "SLS", "SSS"], dict(SC, L=MC + "leather"), M + "lindworm_scale_chestplate")
shaped("lindworm_scale_leggings", ["SSS", "S S", "S S"], SC, M + "lindworm_scale_leggings")
shaped("lindworm_scale_boots", ["S S", "S S"], SC, M + "lindworm_scale_boots")
shaped("lindworm_fang_blade", ["F", "F", "S"], {"F": M + "lindworm_fang", "S": MC + "stick"}, M + "lindworm_fang_blade")
shaped("strong_poison_arrows_from_lindworm_venom_gland", ["AAA", "AVA", "AAA"], {"A": MC + "arrow", "V": M + "lindworm_venom_gland"},
       MC + "tipped_arrow", 8, components={"minecraft:potion_contents": {"potion": "minecraft:strong_poison"}})
for slot, piece in (("head", "helmet"), ("chest", "chestplate"), ("leg", "leggings"), ("foot", "boots")):
    item_tag(f"minecraft:{slot}_armor", M + f"lindworm_scale_{piece}")
item_tag("repairs_lindworm_scale", M + "lindworm_scale")
item_tag("repairs_lindworm_fang", M + "lindworm_fang")
item_tag("minecraft:swords", M + "lindworm_fang_blade")
w(A / "equipment/lindworm_scale.json", {"layers": {"humanoid": [{"texture": "mechanical:lindworm_scale"}],
                                                    "humanoid_leggings": [{"texture": "mechanical:lindworm_scale"}]}})


def entry(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


# the hoard it lay on: gold for sure, a handful of gems, sometimes something rarer (26.x keys: condition/type/modifier)
w(D / "mechanical/loot_table/entities/lindworm_hoard.json", {"type": "minecraft:chest", "pools": [
    {"rolls": 1, "entries": [entry(MC + "gold_ingot", 1, 3, 8)]},
    {"rolls": 1, "entries": [entry(M + "gold_coin", 1, 4, 12)]},
    {"rolls": 1, "entries": [entry(MC + "gold_nugget", 1, 6, 18)]},
    {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 5}, "entries": [
        entry(M + "ruby", 6, 1, 2), entry(M + "sapphire", 6, 1, 2), entry(M + "garnet", 8, 1, 3), entry(M + "topaz", 5, 1, 2),
        entry(M + "amber", 4, 1, 2), entry(MC + "emerald", 4, 1, 2), entry(MC + "diamond", 3, 1, 2), entry(M + "moonstone", 3),
        entry(M + "flawless_ruby", 1), entry(M + "flawless_sapphire", 1), entry(M + "flawless_diamond", 1), entry(M + "flawless_emerald", 1)]},
    {"rolls": 1, "entries": [
        entry(MC + "golden_apple", 6), entry(MC + "gold_block", 3), entry(MC + "enchanted_golden_apple", 1),
        entry(MC + "golden_horse_armor", 3), entry(MC + "name_tag", 3), entry(MC + "music_disc_otherside", 1),
        {"type": "minecraft:empty", "weight": 6}]},
], "random_sequence": "mechanical:entities/lindworm_hoard"})

merge(A / "lang/en_us.json", lang)
print("fauna underground ok")
