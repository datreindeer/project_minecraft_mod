"""Data for the warm-country fauna (wildlife/warm/WarmFauna.java): Onager (and the tamed one), Tortoise, Desert Lizard,
Water Buffalo, Tapir, Leopard, Hornbill, Jungle Fowl, Peafowl.

- item models (flat items, the Leopard Rug block item), lang (entities, items, the onager taming messages)
- recipes: every meat on the Campfire / in the smoker; plucked birds roasted whole or cut up; the Jungle Fowl Egg boiled;
  raw hides scraped into Scraped Hides (onager 1, tapir 2, buffalo 3); the leopard pelt fat-tanned in the grid (knife + fat)
  and made into a rug or leather; Lizard Skin into leather; the Tortoise Shell into bowls, a shield and the Shell Rattle;
  Buffalo Horn into Bone Needles and a horn Lantern; the Hornbill Casque into Bone Needles and orange dye; Peacock
  Feathers split for fletching or into cyan dye; Pack Baskets for the tamed onager
- blocks: the Leopard Rug (Bear Rug model)
- tags: spoilage / smoking, mechanical:tanning_fats, mechanical:scares_predators (the rattle), bird eggs, has_leopards
- world spawns (biome modifiers)
Textures: scripts/gen_fauna_warm_textures.py. Docs: docs/fauna/warm.md.
Generator order: ... gen_roots.py, gen_fauna_*.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_warm.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


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
    tag_add(D / f"mechanical/tags/item/{name}.json", *values)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


def shaped(name, pattern, key, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shaped", "category": category, "pattern": pattern, "key": key,
                  "result": {"id": result, "count": count}})


def cook(name, ingredient, result, ticks=400, count=1, kinds=("campfire_cooking", "smoking")):
    for kind in kinds:
        recipe(f"{name}_from_{kind}" if len(kinds) > 1 else name, {"type": f"minecraft:{kind}", "category": "food",
               "cookingtime": ticks if kind == "campfire_cooking" else ticks // 4, "experience": 0.1,
               "ingredient": ingredient, "result": {"id": result, "count": count}})


ANIMALS = {"onager": "Onager", "tame_onager": "Onager", "tortoise": "Tortoise", "desert_lizard": "Desert Lizard",
           "water_buffalo": "Water Buffalo", "tapir": "Tapir", "leopard": "Leopard", "hornbill": "Hornbill",
           "jungle_fowl": "Jungle Fowl", "peafowl": "Peafowl"}
# raw -> (cooked, raw name, cooked name)
MEATS = {
    "onager_meat": ("cooked_onager_meat", "Onager Meat", "Cooked Onager Meat"),
    "tortoise_meat": ("cooked_tortoise_meat", "Tortoise Meat", "Cooked Tortoise Meat"),
    "lizard_tail": ("cooked_lizard_tail", "Lizard Tail", "Cooked Lizard Tail"),
    "buffalo_meat": ("cooked_buffalo_meat", "Buffalo Meat", "Cooked Buffalo Meat"),
    "tapir_meat": ("cooked_tapir_meat", "Tapir Meat", "Cooked Tapir Meat"),
    "leopard_meat": ("cooked_leopard_meat", "Leopard Meat", "Cooked Leopard Meat"),
    "raw_hornbill": ("cooked_hornbill", "Raw Hornbill", "Cooked Hornbill"),
    "raw_jungle_fowl": ("cooked_jungle_fowl", "Raw Jungle Fowl", "Cooked Jungle Fowl"),
    "raw_peafowl": ("cooked_peafowl", "Raw Peafowl", "Cooked Peafowl"),
}
BIRDS = {"hornbill": "Hornbill", "jungle_fowl": "Jungle Fowl", "peafowl": "Peafowl"}
ITEMS = {
    "raw_onager_hide": "Raw Onager Hide", "raw_tapir_hide": "Raw Tapir Hide", "raw_buffalo_hide": "Raw Buffalo Hide",
    "raw_leopard_pelt": "Raw Leopard Pelt", "leopard_pelt": "Leopard Pelt", "lizard_skin": "Lizard Skin",
    "tortoise_shell": "Tortoise Shell", "buffalo_horn": "Buffalo Horn", "hornbill_casque": "Hornbill Casque",
    "peacock_feather": "Peacock Feather", "jungle_fowl_egg": "Jungle Fowl Egg", "pack_baskets": "Pack Baskets",
    "shell_rattle": "Shell Rattle",
}

lang = {f"entity.mechanical.{k}": v for k, v in ANIMALS.items()}
lang.update({f"item.mechanical.{k}_spawn_egg": f"{v} Spawn Egg" for k, v in ANIMALS.items() if k != "tame_onager"})
lang.update({f"item.mechanical.{k}": v for k, v in ITEMS.items()})
lang["block.mechanical.leopard_rug"] = "Leopard Rug"
lang.update({
    "message.mechanical.onager.shies": "The onager bolts. Crouch and hold out feed",
    "message.mechanical.onager.chewing": "The onager is still chewing",
    "message.mechanical.onager.trust": "The onager eats from your hand (trust %s/%s)",
    "message.mechanical.onager.ready_for_halter": "It trusts you now. Put a Rope Halter on it",
    "message.mechanical.onager.needs_halter": "Put a Rope Halter on it before feeding it more",
    "message.mechanical.onager.refuses_halter": "The onager won't take the halter yet. Feed it first",
    "message.mechanical.onager.haltered": "Haltered. Keep feeding it to tame it (trust %s/%s)",
    "message.mechanical.onager.tamed": "The onager is tame. Ride it, give it Pack Baskets, or hitch it to a Cart",
})

# ---- items and food ----
for k in ITEMS:
    flat_item(k)
for k in ANIMALS:
    if k != "tame_onager":
        flat_item(f"{k}_spawn_egg")
for raw, (cooked, raw_name, cooked_name) in MEATS.items():
    flat_item(raw)
    flat_item(cooked)
    lang[f"item.mechanical.{raw}"] = raw_name
    lang[f"item.mechanical.{cooked}"] = cooked_name
    cook(cooked, M + raw, M + cooked)
for bird, name in BIRDS.items():
    plucked = f"plucked_{bird}"
    flat_item(plucked)
    lang[f"item.mechanical.{plucked}"] = f"Plucked {name}"
    # whole on the fire (a longer roast), or cut up with a knife (it stays in the grid), like gen_birds2.py
    cook(f"cooked_{bird}_from_{plucked}", M + plucked, M + f"cooked_{bird}", 600, kinds=("campfire_cooking",))
    shapeless(f"raw_{bird}_from_{plucked}", [M + plucked, "#mechanical:knives"], M + f"raw_{bird}")
cook("boiled_egg_from_jungle_fowl_egg", M + "jungle_fowl_egg", M + "boiled_egg", 300, kinds=("campfire_cooking",))

RAW = [M + r for r in MEATS] + [M + f"plucked_{b}" for b in BIRDS]
COOKED = [M + c for c, _, _ in MEATS.values()]
item_tag("perishes_1_day", *RAW)
item_tag("perishes_3_days", *COOKED)
item_tag("rots_to_flesh", *RAW, *COOKED)
item_tag("smokable_meat", *[M + r for r in MEATS])
item_tag("bird_eggs", M + "jungle_fowl_egg")
item_tag("scares_predators", M + "shell_rattle")
item_tag("tanning_fats", M + "tallow", M + "lard", M + "bear_fat")

# ---- recipes ----
# raw hides scrape into the common hide chain (Drying Rack -> Leather); the knife stays in the grid
for hide, n in (("raw_onager_hide", 1), ("raw_tapir_hide", 2), ("raw_buffalo_hide", 3)):
    shapeless(f"scraped_hide_from_{hide}", [M + hide, "#mechanical:knives"], M + "scraped_hide", n)
# the leopard pelt: fat-tanned in the grid, then a rug or leather
shapeless("leopard_pelt", [M + "raw_leopard_pelt", "#mechanical:knives", "#mechanical:tanning_fats"], M + "leopard_pelt")
shapeless("leopard_rug", [M + "leopard_pelt", M + "leopard_pelt", "minecraft:bone"], M + "leopard_rug", category="building")
shapeless("leather_from_leopard_pelt", [M + "leopard_pelt", "#mechanical:knives"], "minecraft:leather", 2)
shapeless("leather_from_lizard_skin", [M + "lizard_skin", M + "lizard_skin", M + "lizard_skin"], "minecraft:leather")
# tortoise shell: bowls, a shield face, and the rattle that keeps predators off
shapeless("bowl_from_tortoise_shell", [M + "tortoise_shell", "#mechanical:knives"], "minecraft:bowl", 2)
shaped("shield_from_tortoise_shell", ["PSP", "PHP", " P "], {"P": "#minecraft:planks", "S": M + "tortoise_shell", "H": M + "scraped_hide"},
       "minecraft:shield", category="equipment")
shaped("shell_rattle", [" S", "TG", "R "], {"S": M + "tortoise_shell", "T": M + "twine", "G": "minecraft:gravel", "R": "minecraft:stick"},
       M + "shell_rattle", category="equipment")
# buffalo horn: needles, and thin-scraped horn panes round a torch make a horn lantern
shapeless("bone_needle_from_buffalo_horn", [M + "buffalo_horn", "#mechanical:knives"], M + "bone_needle", 2, category="equipment")
shaped("lantern_from_buffalo_horn", [" C ", "HTH", " C "], {"C": M + "cordage", "H": M + "buffalo_horn", "T": "minecraft:torch"},
       "minecraft:lantern")
# hornbill casque ("red ivory"): carved into needles; its yellow-orange colour ground into dye
shapeless("bone_needle_from_hornbill_casque", [M + "hornbill_casque", "#mechanical:knives"], M + "bone_needle", 2, category="equipment")
shapeless("orange_dye_from_hornbill_casque", [M + "hornbill_casque"], "minecraft:orange_dye", 2)
# peacock feather: split into fletching feathers, or its eye-spot blue into dye
shapeless("feather_from_peacock_feather", [M + "peacock_feather", "#mechanical:knives"], "minecraft:feather", 3)
shapeless("cyan_dye_from_peacock_feather", [M + "peacock_feather", M + "peacock_feather"], "minecraft:cyan_dye")
# pack baskets for the tamed onager: two bark baskets slung on a hide strap
shaped("pack_baskets", ["CHC", "B B"], {"C": M + "cordage", "H": M + "scraped_hide", "B": M + "bark_basket"}, M + "pack_baskets",
       category="equipment")

# ---- Leopard Rug: flat like the Bear Rug ----
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
w(A / "models/block/leopard_rug.json", {"parent": "mechanical:block/bear_rug",
    "textures": {"particle": "mechanical:block/leopard_rug", "top": "mechanical:block/leopard_rug"}})
w(A / "blockstates/leopard_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/leopard_rug", **({"y": y} if y else {})}
                                                    for f, y in ROT}})
flat_item("leopard_rug")
w(D / "mechanical/loot_table/blocks/leopard_rug.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": M + "leopard_rug"}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
    "random_sequence": "mechanical:blocks/leopard_rug"})

# ---- world spawns ----
U = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
DESERT = ["minecraft:desert"]
BADLANDS = ["minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"]
SAVANNA = ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"]
JUNGLE = ["minecraft:jungle", "minecraft:sparse_jungle", "minecraft:bamboo_jungle"]
SWAMP = ["minecraft:swamp", "minecraft:mangrove_swamp", "mechanical:willow_fen"]
SPAWNS = {  # entity -> [(biomes, weight, min, max)]
    "onager": [(SAVANNA, 5, 3, 5), (DESERT + BADLANDS, 4, 2, 4), (["minecraft:plains", "minecraft:sunflower_plains"], 1, 2, 3)],
    "tortoise": [(DESERT + BADLANDS + SAVANNA, 5, 1, 2)],
    "desert_lizard": [(DESERT + BADLANDS + SAVANNA, 8, 1, 2), (["mechanical:pine_heath"], 3, 1, 2)],
    "water_buffalo": [(["minecraft:jungle", "minecraft:sparse_jungle"] + SWAMP, 6, 3, 5)],
    "tapir": [(JUNGLE, 6, 1, 2)],
    "leopard": [(["#mechanical:has_leopards"], 2, 1, 1)],
    "hornbill": [(JUNGLE, 6, 2, 4)],
    "jungle_fowl": [(JUNGLE, 8, 2, 4)],
    "peafowl": [(JUNGLE, 4, 2, 3), (SAVANNA, 2, 2, 3)],
}
w(D / "mechanical/tags/worldgen/biome/has_leopards.json", {"values": JUNGLE + SAVANNA})
for ent, entries in SPAWNS.items():
    for n, (biomes, weight, lo, hi) in enumerate(entries):
        w(D / f"mechanical/neoforge/biome_modifier/add_{ent}{'' if n == 0 else f'_{n + 1}'}.json", {
            "type": "neoforge:add_spawns", "biomes": biomes[0] if len(biomes) == 1 and biomes[0].startswith("#") else biomes,
            "spawners": [{"type": M + ent, "weight": weight, "count": U(lo, hi)}]})

merge(A / "lang/en_us.json", lang)
print("fauna warm ok")
