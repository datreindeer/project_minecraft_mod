"""Field tools (FarmTools.java; docs/farming.md "Fältredskap"): Seed Bag (Bronze Age, sows 3x3), Linen Seed Bag (Iron
Age, sows 5x5) and Bronze Ard (Tool Station: Bronze Hoe Head + Bronze Plate + Tool Rod, tills 3x3; ToolStationRecipes).
Item models/definitions, the two bag recipes (recipe book unlocks come from gen_unlocks.py), hoe tags for the ard, lang.
Textures: scripts/gen_farm_tools_textures.py.
Generator order: ... gen_bronzetools.py, gen_farm_tools.py, gen_pots.py ... gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_farm_tools.py"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def patch_tag(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


for name, parent in (("seed_bag", "generated"), ("linen_seed_bag", "generated"), ("bronze_ard", "handheld")):
    w(A / f"models/item/{name}.json", {"parent": f"minecraft:item/{parent}", "textures": {"layer0": M + f"item/{name}"}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{name}"}})

INGOT = ["minecraft:copper_ingot", M + "bronze_ingot"]  # as gen_farming3.py: the metal waits for casting
RECIPES = {
    # a metal ring holds the leather bag's mouth open, so seed pours out in a sweep
    "seed_bag": ([" C ", "LIL", " L "], {"C": M + "cordage", "L": "minecraft:leather", "I": INGOT}),
    # linen sewn round an iron rim, a leather bottom
    "linen_seed_bag": (["TPT", "T T", "TLT"], {"T": M + "linen_thread", "P": M + "iron_plate", "L": "minecraft:leather"}),
}
for name, (pattern, key) in RECIPES.items():
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern,
                                              "key": key, "result": {"id": M + name}})

patch_tag(D / "minecraft/tags/item/hoes.json", [M + "bronze_ard"])  # enchantable like a hoe

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({
    "item.mechanical.seed_bag": "Seed Bag", "item.mechanical.linen_seed_bag": "Linen Seed Bag", "item.mechanical.bronze_ard": "Bronze Ard",
    "message.mechanical.seed_bag.nothing": "Nothing to sow: no empty farmland here, or no seed in the bag that grows on it",
})
w(lang_path, lang)
print(f"farm tools ok ({len(RECIPES)} recipes)")
