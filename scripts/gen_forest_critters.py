"""Data and item textures for the small forest animals (squirrel, hedgehog, badger) and the giant deer: item models,
spawn biome modifiers, the squirrel pelt recipe, lang, and the 16x16 item textures (spawn eggs, squirrel pelt, giant
antler). Entity textures: gen_forest_critter_textures.py and gen_giant_deer_texture.py.

Usage: python scripts/gen_forest_critters.py
"""
import importlib.util
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"

_spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
wit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wit)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


ANIMALS = ["squirrel", "hedgehog", "badger", "giant_deer"]
ITEMS = ["squirrel_pelt", "giant_antler"] + [f"{a}_spawn_egg" for a in ANIMALS]

# ---- spawns: (weight, min, max), biomes ----
FORESTS = ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:dark_forest"]
TAIGAS = ["minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"]
SPAWNS = {
    "squirrel": ((8, 1, 2), FORESTS + TAIGAS + ["minecraft:snowy_taiga", "minecraft:windswept_forest"]),
    "hedgehog": ((5, 1, 1), ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest"]),
    # hedgehogs out in the open only now and then (plains, meadow)
    "hedgehog_open": ((2, 1, 1), ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow"]),
    # also needs a slope nearby (Badger.onSlope, spawn placement)
    "badger": ((5, 1, 2), ["minecraft:forest", "minecraft:dark_forest", "minecraft:birch_forest", "minecraft:windswept_forest"] + TAIGAS),
    # rare mini-boss: always alone
    "giant_deer": ((1, 1, 1), ["minecraft:forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga",
                               "minecraft:plains", "minecraft:meadow"]),
}

# ---- item textures ----
PELT = [
    "................",
    ".....OOOOO......",
    "....OrRRRrO.....",
    "...OrRRLRRrO....",
    "..OOrRLCLRrOO...",
    ".OrOrRCCCRrOrO..",
    ".OO.ORLCLRO.OO..",
    "....ORRCRRO.....",
    "....OrRCRrO.....",
    ".OO.ORLCLRO.OO..",
    ".OrOrRRCRRrOrO..",
    "..OOrRRRRRrOOO..",
    "....OrRRRrOTTO..",
    ".....OOrOOTLTTO.",
    "......OOOTTLTO..",
    ".........OOOO...",
]
PELT_PAL = {"O": "#3a1d0f", "r": "#7e3a1a", "R": "#a8532a", "L": "#c87a44", "C": "#ead8bc", "T": "#9c5428"}

GIANT_ANTLER = [
    ".......O.O.O.O..",
    "......OTOTOTOTO.",
    ".....OOLLLLLLLTO",
    "..O.OLLMLLMLLLO.",
    ".OTOLMLLMLLMLLTO",
    "..OLLLMMLLMLLO..",
    "...OLMLLMMLLLTO.",
    "...OMLMLLMLLLO..",
    "..OMLMMLLLMLO...",
    "..OMDMLMMMOO....",
    ".OTODMMDOO......",
    "..OMDDMO........",
    "..OMDMO.........",
    ".ORRDO..........",
    "ORrRO...........",
    ".OOO............",
]
ANTLER_PAL = {"O": "#3e2c1c", "T": "#f6f0dc", "L": "#e0ceA2", "M": "#c2a77a", "D": "#8e7250", "R": "#6e5236", "r": "#4c3824"}


def eggs():
    W, K = "#f0e6d2", "#141110"
    squirrel = wit.egg(["#d9824a", "#c06a34", "#a45528", "#83411d", "#5c2c12", "#3e1d0b"],
                       [(6, 9, W), (7, 10, W), (8, 10, W), (9, 9, W), (7, 11, W), (8, 11, W)],
                       [(5, 0), (10, 0)], [(5, 0, "#4e2410"), (10, 0, "#4e2410"), (6, 5, K), (9, 5, K)])
    hog_spots = [(x, y, "#e2d2ae") for x, y in ((4, 4), (7, 3), (10, 5), (5, 7), (9, 7), (12, 8), (3, 10), (7, 10), (11, 11), (5, 13), (9, 13))]
    hedgehog = wit.egg(["#8a7058", "#735a45", "#5e4836", "#4a3829", "#33261c", "#221912"], hog_spots, (),
                       [(7, 14, "#b49a7a"), (8, 14, "#b49a7a"), (7, 15, K), (8, 15, K)])
    stripes = ([(x, y, K) for y in range(3, 11) for x in (5, 6, 9, 10)] + [(x, y, W) for y in range(2, 12) for x in (7, 8)]
               + [(x, y, W) for y in range(8, 11) for x in (4, 11)])
    badger = wit.egg(["#b4b0a8", "#9a968e", "#827e77", "#69665f", "#46443f", "#2c2a27"], [], (), stripes + [(7, 10, K), (8, 10, K)])
    A1, A2 = "#efe4c4", "#c4ab80"
    left = [(0, 0, A1), (2, 0, A1), (4, 0, A1), (0, 1, A1), (1, 1, A1), (2, 1, A1), (3, 1, A1), (4, 1, A1),
            (1, 2, A2), (2, 2, A2), (3, 2, A2), (4, 2, A2), (5, 2, A2), (5, 3, A2)]
    palms = left + [(15 - x, y, c) for x, y, c in left]
    deer = wit.egg(["#d6aa74", "#bf9058", "#a47646", "#835b34", "#5c3f24", "#3e2a18"],
                   [(5, 7, "#5a3c26"), (6, 6, "#5a3c26"), (7, 6, "#5a3c26"), (8, 6, "#5a3c26"), (9, 6, "#5a3c26"), (10, 7, "#5a3c26"),
                    (7, 14, "#efe2c6"), (8, 14, "#efe2c6")],
                   [(x, y) for x, y, _ in palms], [], shift=1)
    for x, y, c in palms:
        deer.putpixel((x, y), wit.hexc(c))
    return {"squirrel_spawn_egg": squirrel, "hedgehog_spawn_egg": hedgehog, "badger_spawn_egg": badger, "giant_deer_spawn_egg": deer}


def main():
    for item in ITEMS:
        w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
        w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    for name, ((wt, lo, hi), biomes) in SPAWNS.items():
        mob = name.removesuffix("_open")
        w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": f"mechanical:{mob}", "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})
    # four small pelts stitched together with twine make one hide (then scraped and dried like any other)
    w(D / "mechanical/recipe/raw_hide_from_squirrel_pelts.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": ["mechanical:squirrel_pelt"] * 4 + ["mechanical:twine"], "result": {"id": "mechanical:raw_hide", "count": 1}})
    w(D / "mechanical/advancement/recipes/raw_hide_from_squirrel_pelts.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "mechanical:squirrel_pelt"}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": ["mechanical:raw_hide_from_squirrel_pelts"]}})

    lang_path = A / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for a in ANIMALS:
        name = a.replace("_", " ").title()
        lang[f"entity.mechanical.{a}"] = name
        lang[f"item.mechanical.{a}_spawn_egg"] = f"{name} Spawn Egg"
    lang["item.mechanical.squirrel_pelt"] = "Squirrel Pelt"
    lang["item.mechanical.giant_antler"] = "Giant Antler"
    lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

    textures = {"squirrel_pelt": wit.from_grid(PELT, PELT_PAL), "giant_antler": wit.from_grid(GIANT_ANTLER, ANTLER_PAL), **eggs()}
    for name, im in textures.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(wit.OUT / f"{name}.png")
    print("forest critters ok:", ", ".join(textures))


if __name__ == "__main__":
    main()
