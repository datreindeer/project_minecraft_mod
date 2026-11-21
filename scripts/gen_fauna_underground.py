"""Data for the underground fauna (wildlife/underground/UndergroundFauna.java): the Giant Centipede. Cave Hyena and Cave Bear
are MountainFauna's (scripts/gen_fauna_mountain.py); underground they spawn by the darkness rule in Underground.java, which
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

merge(A / "lang/en_us.json", lang)
print("fauna underground ok")
