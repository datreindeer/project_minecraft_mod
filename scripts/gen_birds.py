"""Data and item textures for the flocking birds (crow, goose, rock dove, ptarmigan, seagull) and the desert hare: item
models, spawn biome modifiers, loot tables (feathers, a little meat), lang and the 16x16 spawn eggs; also the desert
hare's entity texture (the hare painter of gen_small_wildlife_textures.py with a sandy palette).
Bird entity textures and model geometry: gen_bird_textures.py.

Usage: python scripts/gen_birds.py
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


NAMES = {"crow": "Hooded Crow", "goose": "Greylag Goose", "rock_dove": "Rock Dove", "ptarmigan": "Willow Ptarmigan", "seagull": "Seagull",
         "desert_hare": "Desert Hare"}

# ---- spawns: (weight, min, max), biomes; flocks spawn together ----
SPAWNS = {
    "crow": ((6, 3, 6), ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest", "minecraft:birch_forest",
                         "minecraft:flower_forest", "minecraft:dark_forest", "minecraft:savanna", "minecraft:windswept_forest"]),
    "goose": ((10, 4, 7), ["minecraft:river", "minecraft:swamp", "minecraft:frozen_river"]),
    "rock_dove": ((8, 4, 7), ["minecraft:meadow", "minecraft:windswept_hills", "minecraft:windswept_gravelly_hills",
                              "minecraft:stony_peaks", "minecraft:stony_shore", "minecraft:savanna_plateau"]),
    "ptarmigan": ((7, 3, 5), ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes",
                              "minecraft:taiga", "minecraft:old_growth_spruce_taiga"]),
    # gulls: beaches and rocky shores, rivers now and then, and a few out over the (shallow) sea
    "seagull": ((10, 2, 4), ["minecraft:beach", "minecraft:snowy_beach", "minecraft:stony_shore"]),
    "desert_hare": ((8, 1, 2), ["minecraft:desert", "minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"]),
    "seagull_water": ((3, 2, 3), ["minecraft:river", "minecraft:ocean", "minecraft:cold_ocean", "minecraft:lukewarm_ocean"]),
}

# ---- drops: feathers (min, max), meat item and (min, max) ----
LOOT = {
    "crow": ((1, 2), "mechanical:game_meat_scraps", (0, 1)),
    "goose": ((2, 4), "mechanical:chicken_scraps", (1, 2)),
    "rock_dove": ((1, 2), "mechanical:chicken_scraps", (0, 1)),
    "ptarmigan": ((1, 3), "mechanical:chicken_scraps", (1, 1)),
    "seagull": ((1, 3), None, None),
}


def count(lo, hi):
    return {"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


def loot(bird):
    (flo, fhi), meat, mc = LOOT[bird]
    pools = [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:feather", "modifier": [count(flo, fhi)]}]}]
    if meat:
        pools.append({"rolls": 1, "entries": [{"type": "minecraft:item", "name": meat, "modifier": [count(*mc)]}]})
    return {"type": "minecraft:entity", "pools": pools, "random_sequence": f"mechanical:entities/{bird}"}


def eggs():
    K = "#151316"
    crow = wit.egg(["#b4b2b0", "#9c9a98", "#868482", "#6c6a69", "#2a2a30", "#18181c"],
                   [(x, y, K) for y in range(1, 6) for x in range(2, 14)] + [(x, 6, "#2e2e36") for x in range(5, 11)],
                   (), [(9, 3, "#5a5a66"), (11, 9, K), (4, 10, K), (12, 11, K)])
    goose = wit.egg(["#b8ae9e", "#9e9482", "#867c6c", "#6e6556", "#4c4538", "#352f26"],
                    [(x, y, "#c9bfae") for y in (6, 8, 10, 12) for x in range(3, 13, 2)],
                    (), [(7, 2, "#e8863a"), (8, 2, "#e8863a"), (7, 14, "#f0eee8"), (8, 14, "#f0eee8")])
    dove = wit.egg(["#c8ccd6", "#b0b4c0", "#9aa0ae", "#828898", "#545a68", "#3a3f4a"],
                   [(x, 9, "#24242a") for x in range(4, 12)] + [(x, 11, "#24242a") for x in range(4, 12)],
                   (), [(5, 4, "#4a8a68"), (6, 5, "#8a5c92"), (9, 4, "#8a5c92"), (10, 5, "#4a8a68")])
    ptarmigan = wit.egg(["#a8704a", "#8e5a38", "#764a2e", "#5e3a24", "#3e2616", "#2a1a10"],
                        [(4, 5, "#3c2414"), (9, 4, "#3c2414"), (11, 7, "#c89c68"), (6, 8, "#3c2414")]
                        + [(x, y, "#f2f3f6") for y in range(10, 15) for x in range(3, 13)],
                        (), [(6, 3, "#d02a24"), (7, 3, "#d02a24")])
    gull = wit.egg(["#ffffff", "#f2f2ee", "#e2e4e6", "#cfd3d8", "#9aa4b0", "#6e7884"],
                   [(x, y, "#98a4b4") for y in range(6, 10) for x in range(2, 14)] + [(x, 10, "#1e1e22") for x in range(3, 13)]
                   + [(5, 10, "#ffffff"), (10, 10, "#ffffff")],
                   (), [(7, 3, "#d6cc54"), (8, 3, "#d6cc54")])
    hare = wit.egg(["#ecdcb8", "#dcc496", "#c8aa78", "#ad8e5e", "#7c6442", "#56442c"],
                   [(5, 6, "#8e7450"), (9, 5, "#8e7450"), (11, 9, "#f4ecda"), (6, 10, "#f4ecda"), (8, 12, "#f4ecda")],
                   [(5, 0), (5, 1), (10, 0), (10, 1)], [(5, 0, "#3a2c1e"), (10, 0, "#3a2c1e"), (5, 1, "#c8aa78"), (10, 1, "#c8aa78")])
    return {"desert_hare_spawn_egg": hare, "crow_spawn_egg": crow, "goose_spawn_egg": goose, "rock_dove_spawn_egg": dove,
            "ptarmigan_spawn_egg": ptarmigan, "seagull_spawn_egg": gull}


def desert_hare_texture():
    """The hare painter with a pale sandy (cape hare) palette, plus the skinned version for the carcass."""
    spec = importlib.util.spec_from_file_location("swt", Path(__file__).resolve().parent / "gen_small_wildlife_textures.py")
    swt = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(swt)
    swt.H_DARK, swt.H_BROWN, swt.H_OCHRE = (150, 118, 82), (184, 150, 106), (206, 170, 116)
    swt.H_TAN, swt.H_CREAM, swt.H_WHITE, swt.H_GREY = (222, 194, 146), (238, 228, 206), (246, 242, 232), (178, 162, 138)
    img, _ = swt.paint(swt.HARE, swt.HARE_PAINTERS)
    for sub, im in (("wildlife", img), ("skinned/wildlife", swt.skin(img.copy()))):
        dst = swt.ROOT / sub / "desert_hare.png"
        im.save(dst)
        print("wrote", dst.relative_to(ROOT))


def main():
    desert_hare_texture()
    for bird in NAMES:
        item = f"{bird}_spawn_egg"
        w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
        w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
        if bird in LOOT:
            w(D / f"mechanical/loot_table/entities/{bird}.json", loot(bird))
    for name, ((wt, lo, hi), biomes) in SPAWNS.items():
        mob = name.removesuffix("_water")
        w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
            "spawners": [{"type": f"mechanical:{mob}", "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

    lang_path = A / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for bird, name in NAMES.items():
        lang[f"entity.mechanical.{bird}"] = name
        lang[f"item.mechanical.{bird}_spawn_egg"] = f"{name} Spawn Egg"
    lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

    for name, im in eggs().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(wit.OUT / f"{name}.png")
    print("birds ok:", ", ".join(NAMES))


if __name__ == "__main__":
    main()
