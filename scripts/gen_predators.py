"""Night predators data: Grey Wolf and Cave Lion (wildlife/GreyWolf.java, CaveLion.java, Predators.java).
Writes item models (Wolf Pelt, spawn eggs), lang, the wolf howl sound event (sounds.json: vanilla wolf whines pitched
down into a howl, heard ~96 blocks off), biome tags for where they live (also read by the night spawner in
Predators.java), world-generation spawns (biome modifiers), the item tag of things that scare them
(mechanical:scares_predators: torches and lanterns), and removes vanilla wolves from grey wolf country.
Textures: scripts/gen_predator_textures.py. Butchery yields are in Butchery.java.
Generator order: ... gen_soot.py, gen_phantom.py, gen_predators.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_predators.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


ITEMS = {"wolf_pelt": "Wolf Pelt", "grey_wolf_spawn_egg": "Grey Wolf Spawn Egg", "cave_lion_spawn_egg": "Cave Lion Spawn Egg"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
merge(A / "lang/en_us.json", {**{f"item.mechanical.{k}": v for k, v in ITEMS.items()},
                              "entity.mechanical.grey_wolf": "Grey Wolf", "entity.mechanical.cave_lion": "Cave Lion",
                              "subtitles.mechanical.entity.grey_wolf.howl": "Wolves howl"})

# 26.3 has no wolf howl any more: long whines pitched down (the game clamps pitch at 0.5) carry far like a howl
HOWL = 96
merge(A / "sounds.json", {"entity.grey_wolf.howl": {"subtitle": "subtitles.mechanical.entity.grey_wolf.howl", "sounds": [
    {"name": "minecraft:mob/wolf/big/whine", "pitch": 0.6, "attenuation_distance": HOWL},
    {"name": "minecraft:mob/wolf/sad/whine", "pitch": 0.62, "attenuation_distance": HOWL},
    {"name": "minecraft:mob/wolf/classic/whine", "pitch": 0.58, "attenuation_distance": HOWL},
    {"name": "minecraft:mob/wolf/grumpy/whine", "pitch": 0.6, "attenuation_distance": HOWL}]}})

# where they live (night spawner + world generation)
WOLF_BIOMES = ["#minecraft:is_taiga", "minecraft:forest", "mechanical:deciduous_forest", "mechanical:hazel_meadow", "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
               "minecraft:dark_forest", "minecraft:plains", "minecraft:snowy_plains", "minecraft:grove", "minecraft:snowy_slopes"]
LION_BIOMES = ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna", "minecraft:sunflower_plains",
               "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:stony_peaks", "minecraft:meadow"]
w(D / "mechanical/tags/worldgen/biome/has_grey_wolves.json", {"values": WOLF_BIOMES})
w(D / "mechanical/tags/worldgen/biome/has_cave_lions.json", {"values": LION_BIOMES})
# a few packs and lions placed with the world (they sleep by day); the rest arrive at night (Predators.java)
w(D / "mechanical/neoforge/biome_modifier/add_grey_wolves.json", {"type": "neoforge:add_spawns", "biomes": "#mechanical:has_grey_wolves",
    "spawners": [{"type": "mechanical:grey_wolf", "weight": 2, "count": {"type": "minecraft:uniform", "min_inclusive": 3, "max_inclusive": 6}}]})
w(D / "mechanical/neoforge/biome_modifier/add_cave_lions.json", {"type": "neoforge:add_spawns", "biomes": "#mechanical:has_cave_lions",
    "spawners": [{"type": "mechanical:cave_lion", "weight": 1, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 2}}]})
# the tameable vanilla wolf stays only outside grey wolf country (savanna plateaus, badlands, jungle edges)
w(D / "mechanical/neoforge/biome_modifier/remove_vanilla_wolves.json", {"type": "neoforge:remove_spawns",
    "biomes": "#mechanical:has_grey_wolves", "entity_types": "minecraft:wolf"})

w(D / "mechanical/tags/item/scares_predators.json", {"values": ["minecraft:torch", "minecraft:soul_torch", "minecraft:lantern",
                                                                "minecraft:soul_lantern", "minecraft:copper_torch"]})
print("predators ok")
