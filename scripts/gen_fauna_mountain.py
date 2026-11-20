"""Data for the mountain fauna (wildlife/mountain/MountainFauna.java): Ibex, Chamois, Marmot, Saiga, Cave Bear, Cave Hyena.

- item models (flat items, the Horn Bow with its pulling stages, block items), lang, sounds (marmot whistle, hyena giggle)
- recipes: every meat on the Campfire / in the smoker; raw pelts fat-tanned in the grid (knife + fat), pelts cut into
  Leather, rugs, Marmot Fat -> Tallow, Marmot Salve, Horn Window, horns into Bone Needles and Fish Hooks
- blocks: rugs (Bear Rug model), the Cave Bear Skull (model, loot), the Horn Window; the Cave Bear pelt on the tall
  Drying Rack (hide 22/23, both blocks: added to the rack's blockstate and loot that gen_preserving.py writes)
- tags: spoilage / smoking for the meats, mechanical:tanning_fats, Chamois Boots armour tags, the bow enchant tags,
  the has_cave_hyenas biome tag (also the night spawner in MountainFauna)
- world spawns (biome modifiers) and the vanilla goat removed where the ibex lives
Textures: scripts/gen_fauna_mountain_textures.py. Docs: docs/fauna/mountain.md.
Generator order: ... gen_roots.py, gen_fauna_*.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_fauna_mountain.py"""
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


def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, result, count=1, category="misc"):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": category, "ingredients": ingredients,
                  "result": {"id": result, "count": count}})


ANIMALS = {"ibex": "Ibex", "chamois": "Chamois", "marmot": "Marmot", "saiga": "Saiga", "cave_bear": "Cave Bear", "cave_hyena": "Cave Hyena"}
MEATS = {"ibex": "Ibex", "chamois": "Chamois", "marmot": "Marmot", "saiga": "Saiga", "cave_bear": "Cave Bear", "hyena": "Hyena"}
ITEMS = {
    "raw_ibex_pelt": "Raw Ibex Pelt", "ibex_pelt": "Ibex Pelt", "raw_chamois_hide": "Raw Chamois Hide", "chamois_leather": "Chamois Leather",
    "marmot_pelt": "Marmot Pelt", "raw_saiga_pelt": "Raw Saiga Pelt", "saiga_pelt": "Saiga Pelt", "raw_hyena_pelt": "Raw Hyena Pelt",
    "hyena_pelt": "Hyena Pelt", "raw_cave_bear_pelt": "Raw Cave Bear Pelt", "cave_bear_pelt": "Cave Bear Pelt",
    "ibex_horn": "Ibex Horn", "chamois_horn": "Chamois Horn", "marmot_fat": "Marmot Fat", "saiga_horn": "Saiga Horn",
    "hyena_fang": "Hyena Fang", "marmot_salve": "Marmot Salve", "chamois_boots": "Chamois Boots",
}
BLOCKS = {"ibex_rug": "Ibex Rug", "saiga_rug": "Saiga Rug", "hyena_rug": "Hyena Rug", "cave_bear_rug": "Cave Bear Rug",
          "cave_bear_skull": "Cave Bear Skull", "horn_window": "Horn Window"}

lang = {f"entity.mechanical.{k}": v for k, v in ANIMALS.items()}
lang.update({f"item.mechanical.{k}_spawn_egg": f"{v} Spawn Egg" for k, v in ANIMALS.items()})
lang.update({f"item.mechanical.{k}": v for k, v in ITEMS.items()})
lang.update({f"block.mechanical.{k}": v for k, v in BLOCKS.items()})
lang["item.mechanical.horn_bow"] = "Horn Bow"
lang["subtitles.mechanical.entity.marmot.whistle"] = "Marmot whistles"
lang["subtitles.mechanical.entity.cave_hyena.laugh"] = "Hyena giggles"

# ---- items ----
for k in ITEMS:
    flat_item(k)
for k in ANIMALS:
    flat_item(f"{k}_spawn_egg")
for meat, name in MEATS.items():
    raw, cooked = f"{meat}_meat", f"cooked_{meat}_meat"
    flat_item(raw)
    flat_item(cooked)
    lang[f"item.mechanical.{raw}"] = f"{name} Meat"
    lang[f"item.mechanical.{cooked}"] = f"Cooked {name} Meat"
    # only the campfire (and the smoker) cook food, like the scraps
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        recipe(f"{cooked}_from_{kind}", {"type": f"minecraft:{kind}", "category": "food", "cookingtime": ticks, "experience": 0.1,
                                         "ingredient": M + raw, "result": {"id": M + cooked}})
RAW_MEATS = [M + f"{m}_meat" for m in MEATS]
COOKED_MEATS = [M + f"cooked_{m}_meat" for m in MEATS]
item_tag("perishes_1_day", *RAW_MEATS)
item_tag("perishes_3_days", *COOKED_MEATS)
item_tag("rots_to_flesh", *RAW_MEATS, *COOKED_MEATS)
item_tag("smokable_meat", *RAW_MEATS)

# Horn Bow: the vanilla bow's pulling stages
for n in ("", "_pulling_0", "_pulling_1", "_pulling_2"):
    w(A / f"models/item/horn_bow{n}.json", {"parent": "minecraft:item/bow", "textures": {"layer0": f"mechanical:item/horn_bow{n}"}})
pull = lambda n: {"type": "minecraft:model", "model": f"mechanical:item/horn_bow_pulling_{n}"}
w(A / "items/horn_bow.json", {"model": {"type": "minecraft:condition", "property": "minecraft:using_item",
    "on_false": {"type": "minecraft:model", "model": "mechanical:item/horn_bow"},
    "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05, "fallback": pull(0),
                "entries": [{"threshold": 0.65, "model": pull(1)}, {"threshold": 0.9, "model": pull(2)}]}}})
for tag in ("enchantable/bow", "enchantable/durability"):
    tag_add(D / f"minecraft/tags/item/{tag}.json", M + "horn_bow")

# Chamois Boots: foot armour of fat-tanned chamois leather
tag_add(D / "minecraft/tags/item/foot_armor.json", M + "chamois_boots")
w(D / "mechanical/tags/item/repairs_chamois_leather_armor.json", {"values": [M + "chamois_leather"]})
w(A / "equipment/chamois_leather.json", {"layers": {"humanoid": [{"texture": "mechanical:chamois_leather"}]}})

# ---- recipes ----
# fat tanning: a raw pelt worked with a knife and fat (the knife stays in the grid)
item_tag("tanning_fats", M + "tallow", M + "marmot_fat", M + "lard", M + "bear_fat")
for raw, done in (("raw_ibex_pelt", "ibex_pelt"), ("raw_saiga_pelt", "saiga_pelt"), ("raw_hyena_pelt", "hyena_pelt"),
                  ("raw_chamois_hide", "chamois_leather")):
    shapeless(done, [M + raw, "#mechanical:knives", "#mechanical:tanning_fats"], M + done)
for pelt in ("ibex_pelt", "saiga_pelt", "hyena_pelt"):
    shapeless(f"leather_from_{pelt}", [M + pelt, "#mechanical:knives"], "minecraft:leather", 2)
    rug = pelt.replace("_pelt", "_rug")
    shapeless(rug, [M + pelt, M + pelt, "minecraft:bone"], M + rug, category="building")
shapeless("cave_bear_rug", [M + "cave_bear_pelt", "mechanical:bear_claw"], M + "cave_bear_rug", category="building")
recipe("tallow_from_marmot_fat", {"type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 300, "experience": 0.1,
                                  "ingredient": M + "marmot_fat", "result": {"id": M + "tallow"}})
shapeless("marmot_salve", [M + "marmot_fat", M + "marmot_fat", M + "juniper_berries"], M + "marmot_salve", 2)
recipe("horn_window", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["SHS", "HSH", "SHS"],
                       "key": {"S": "minecraft:stick", "H": M + "saiga_horn"}, "result": {"id": M + "horn_window", "count": 4}})
for horn in ("ibex_horn", "saiga_horn"):
    shapeless(f"bone_needle_from_{horn}", [M + horn, "#mechanical:knives"], M + "bone_needle", category="equipment")
shapeless("bone_fish_hook_from_chamois_horn", [M + "chamois_horn", "#mechanical:knives"], M + "bone_fish_hook", 2, category="equipment")

# ---- blocks ----
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
rot = lambda y: {"y": y} if y else {}


def simple_loot(block):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + block}], "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"mechanical:blocks/{block}"})


for rug in ("ibex_rug", "saiga_rug", "hyena_rug", "cave_bear_rug"):
    w(A / f"models/block/{rug}.json", {"parent": "mechanical:block/bear_rug", "textures": {"particle": f"mechanical:block/{rug}", "top": f"mechanical:block/{rug}"}})
    w(A / f"blockstates/{rug}.json", {"variants": {f"facing={f}": {"model": f"mechanical:block/{rug}", **rot(y)} for f, y in ROT}})
    flat_item(rug)
    simple_loot(rug)

# Cave Bear Skull: muzzle north (to -z) for facing=north. Bone faces use the 0-12 area of the texture, the dark eye
# sockets and nasal opening u 12-16 v 0-4, teeth u 12-16 v 12-16 (gen_fauna_mountain_textures.skull_block).
FACES = ("north", "south", "east", "west", "up", "down")


def el(f, t, kind="bone"):
    def uv(face):
        dx, dy, dz = (t[i] - f[i] for i in range(3))
        wd, ht = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}[face]
        u0, v0 = {"bone": (0, 0), "dark": (12, 0), "teeth": (12, 12)}[kind]
        lim = 12 if kind == "bone" else 4
        return [u0, v0, u0 + min(wd, lim), v0 + min(ht, lim)]
    return {"from": f, "to": t, "faces": {face: {"uv": uv(face), "texture": "#skull"} for face in FACES}}


skull = [
    el([4, 1, 7], [12, 7, 15]),               # braincase
    el([4.5, 5, 4], [11.5, 8.5, 9]),          # the domed forehead, rising steeply above the muzzle
    el([3, 1.5, 5.5], [13, 4.5, 9]),          # cheekbones (zygomatic arches)
    el([5, 1.5, 1], [11, 5, 6]),              # muzzle
    el([5.5, 0, 2.5], [10.5, 1, 12]),         # lower jaw
    el([5.5, 0, 1.2], [6.5, 2.5, 2.2], "teeth"), el([9.5, 0, 1.2], [10.5, 2.5, 2.2], "teeth"),  # canines
    el([6.5, 2.5, 0.9], [9.5, 4.5, 1], "dark"),  # nasal opening
    el([4.6, 5.5, 3.9], [6.6, 7, 4], "dark"), el([9.4, 5.5, 3.9], [11.4, 7, 4], "dark"),  # eye sockets
]
w(A / "models/block/cave_bear_skull.json", {"parent": "minecraft:block/block", "textures": {"particle": "mechanical:block/cave_bear_skull",
    "skull": "mechanical:block/cave_bear_skull"}, "elements": skull,
    "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [0.8, 0.8, 0.8]},
                "fixed": {"rotation": [0, 180, 0], "scale": [0.8, 0.8, 0.8]}}})
w(A / "blockstates/cave_bear_skull.json", {"variants": {f"facing={f}": {"model": "mechanical:block/cave_bear_skull", **rot(y)} for f, y in ROT}})
w(A / "items/cave_bear_skull.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/cave_bear_skull"}})
simple_loot("cave_bear_skull")

w(A / "models/block/horn_window.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "mechanical:block/horn_window"}})
w(A / "blockstates/horn_window.json", {"variants": {"": {"model": "mechanical:block/horn_window"}}})
w(A / "items/horn_window.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/horn_window"}})
simple_loot("horn_window")
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "horn_window")


# ---- the cave bear pelt on the tall Drying Rack: hide 22 raw / 23 dry, over both blocks like the bear's ----
def pelt_plane(tex, y0, y1):
    return {"textures": {"particle": "mechanical:block/drying_rack", "front": f"mechanical:block/{tex}", "back": f"mechanical:block/{tex}_back"},
            "elements": [{"from": [3, y0, 7], "to": [13, y1, 7], "faces": {
                "north": {"uv": [3, 16 - y1, 13, 16 - y0], "texture": "#front"}, "south": {"uv": [13, 16 - y1, 3, 16 - y0], "texture": "#back"}}}]}


for stage in ("raw", "dry"):
    w(A / f"models/block/drying_rack_cave_bear_{stage}_lower.json", pelt_plane(f"drying_rack_cave_bear_{stage}_lower", 4, 16))
    w(A / f"models/block/drying_rack_cave_bear_{stage}_upper.json", pelt_plane(f"drying_rack_cave_bear_{stage}_upper", 0, 13))
bs_path = A / "blockstates/drying_rack.json"
bs = json.loads(bs_path.read_text(encoding="utf-8"))
parts = [p for p in bs["multipart"] if not p["apply"]["model"].removeprefix("mechanical:block/").startswith("drying_rack_cave_bear_")
         and p.get("when", {}).get("hide") != "22|23"]
parts += [{"when": {"facing": f, "part": "single", "hide": "22|23"}, "apply": {"model": "mechanical:block/drying_rack_0", **rot(y)}}
          for f, y in ROT]  # never hung there; just not invisible if set by command
parts += [{"when": {"facing": f, "part": half, "hide": str(h)}, "apply": {"model": f"mechanical:block/drying_rack_cave_bear_{st}_{half}", **rot(y)}}
          for f, y in ROT for half in ("lower", "upper") for h, st in ((22, "raw"), (23, "dry"))]
bs["multipart"] = parts
w(bs_path, bs)
loot_path = D / "mechanical/loot_table/blocks/drying_rack.json"
loot = json.loads(loot_path.read_text(encoding="utf-8"))
hide_pool = next(p for p in loot["pools"] if any(e.get("condition", {}).get("state", {}).get("hide") == "16" for e in p["entries"]))
hide_pool["entries"] = [e for e in hide_pool["entries"] if e.get("condition", {}).get("state", {}).get("hide") not in ("22", "23")]
hide_pool["entries"] += [{"type": "minecraft:item", "name": M + item, "condition": {"type": "minecraft:match_block", "blocks": M + "drying_rack",
                          "state": {"hide": h, "part": "lower"}}} for h, item in (("22", "raw_cave_bear_pelt"), ("23", "cave_bear_pelt"))]
w(loot_path, loot)

# ---- sounds: vanilla sounds pitched ----
merge(A / "sounds.json", {
    "entity.marmot.whistle": {"subtitle": "subtitles.mechanical.entity.marmot.whistle", "sounds": [
        {"name": f"minecraft:mob/fox/screech{n}", "pitch": 1.9, "attenuation_distance": 48} for n in range(1, 5)]},
    "entity.cave_hyena.laugh": {"subtitle": "subtitles.mechanical.entity.cave_hyena.laugh", "sounds": [
        {"name": "minecraft:entity/witch/celebrate", "pitch": 1.35, "attenuation_distance": 48},
        {"name": "minecraft:entity/witch/celebrate", "pitch": 1.5, "attenuation_distance": 48},
        {"name": "minecraft:mob/fox/screech2", "pitch": 0.75, "attenuation_distance": 48}]}})

# ---- world spawns ----
U = lambda lo, hi: {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}
SPAWNS = {  # entity -> [(biomes, weight, min, max)]
    "ibex": [(["minecraft:stony_peaks"], 8, 2, 5), (["minecraft:jagged_peaks", "minecraft:snowy_slopes"], 6, 2, 4),
             (["minecraft:frozen_peaks", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills"], 4, 2, 4)],
    "chamois": [(["minecraft:meadow", "minecraft:cherry_grove", "minecraft:grove", "minecraft:windswept_forest"], 4, 2, 4),
                (["minecraft:windswept_hills", "minecraft:snowy_slopes", "minecraft:stony_peaks"], 3, 2, 3)],
    "marmot": [(["minecraft:meadow"], 8, 3, 6), (["minecraft:cherry_grove"], 5, 3, 5),
               (["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills"], 3, 2, 4)],
    "saiga": [(["minecraft:savanna", "minecraft:savanna_plateau"], 5, 4, 8), (["minecraft:plains", "minecraft:sunflower_plains"], 3, 4, 7),
              (["minecraft:snowy_plains", "mechanical:pine_heath"], 3, 3, 6)],
    # cave bears only take where it is dark under rock (MountainFauna.isDen): the dens under the mountains
    "cave_bear": [(["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest", "minecraft:stony_peaks",
                    "minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:snowy_slopes", "minecraft:grove", "minecraft:meadow",
                    "minecraft:dripstone_caves"], 2, 1, 2)],
    "cave_hyena": [(["#mechanical:has_cave_hyenas"], 2, 3, 5)],
}
HYENA_BIOMES = ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:plains", "minecraft:sunflower_plains",
                "minecraft:badlands", "minecraft:wooded_badlands", "minecraft:eroded_badlands", "mechanical:hazel_meadow"]
w(D / "mechanical/tags/worldgen/biome/has_cave_hyenas.json", {"values": HYENA_BIOMES})
for ent, entries in SPAWNS.items():
    for n, (biomes, weight, lo, hi) in enumerate(entries):
        w(D / f"mechanical/neoforge/biome_modifier/add_{ent}{'' if n == 0 else f'_{n + 1}'}.json", {
            "type": "neoforge:add_spawns", "biomes": biomes[0] if len(biomes) == 1 and biomes[0].startswith("#") else biomes,
            "spawners": [{"type": M + ent, "weight": weight, "count": U(lo, hi)}]})
# the ibex takes the vanilla goat's place in the high mountains
w(D / "mechanical/neoforge/biome_modifier/remove_vanilla_goats.json", {"type": "neoforge:remove_spawns",
    "biomes": ["minecraft:snowy_slopes", "minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks"],
    "entity_types": "minecraft:goat"})

merge(A / "lang/en_us.json", lang)
print("fauna mountain ok")
