"""Dirt Slab, Grass Slab and Dirt Path Slab (living-world plan F11; EarthBlocks, DirtSlabBlock), and the ground slabs and
stairs of the terrain plan phase 2 (worldgen/SmoothGround lays them on 1-block steps).

Blockstates and models on vanilla textures (the Grass Slab on grassy_slab like Turf, tinted by season, and a snowy look
with snow on top and grass_block_snow sides), loot (all three drop Dirt Slabs, 2 for a double), the recipe 3 Dirt -> 6,
the shovel's block transformer (vanilla's dirt path rule plus Dirt/Grass Slab -> Dirt Path Slab, type kept), tags, lang.
Bottom slabs show the top half of the side texture (ground_slab / grassy_ground_slab), so grass, podzol and snow keep
their fringe along the slab's top edge.

Phase 2: Dirt/Grass Stairs (grass creeps on and off), Coarse Dirt Slab/Stairs, Podzol Slab, Sand and Red Sand Slab/Stairs
(not falling), Gravel, Moss, Mud, Peat, Loess and Wood Ash Slabs: models, loot (itself; Grass Stairs -> Dirt Stairs,
Podzol Slab -> Dirt Slab unless silk touch), recipes (3 in a row -> 6, stairs -> 4), shovel tag, slabs/stairs tags, lang.

Generator order: after gen_earth.py (grassy_slab models), before gen_block_items.py, gen_unlocks.py, gen_advancements.py
and gen_loot_format.py. Run: python scripts/gen_dirt_slab.py
"""
import glob
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
A, D = ROOT / "src/main/resources/assets/mechanical", ROOT / "src/main/resources/data"
M, V = "mechanical:", "minecraft:block/"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def ground_slab(name, grassy):
    """Vanilla's bottom slab with the sides' uv on the texture's top half (the grass/snow fringe on the slab's top edge)."""
    m = json.loads(jar.read("assets/minecraft/models/block/slab.json"))
    face = lambda f, tex, tint: {**f, "uv": [0, 0, 16, 8], "texture": tex, **({"tintindex": 0} if tint else {})}
    els = []
    for e in m["elements"]:
        faces = {d: face(f, f["texture"], False) if d not in ("up", "down") else {**f, **({"tintindex": 0} if grassy and d == "up" else {})}
                 for d, f in e["faces"].items()}
        els.append({**e, "faces": faces})
        if grassy:
            els.append({**e, "faces": {d: face(f, "#overlay", True) for d, f in e["faces"].items() if d not in ("up", "down")}})
    w(A / f"models/block/{name}.json", {**m, "elements": els})


ground_slab("ground_slab", False)
ground_slab("grassy_ground_slab", True)


def fringe_stairs(name, grassy):
    """Vanilla's straight stairs with the lower step split: its front half shows the side texture's top half (the fringe on
    the front edge), the back half under the upper step keeps the lower half."""
    m = json.loads(jar.read("assets/minecraft/models/block/stairs.json"))
    side = lambda uv, cull=None: {"uv": uv, "texture": "#side", **({"cullface": cull} if cull else {})}
    front = {"from": [0, 0, 0], "to": [8, 8, 16], "faces": {
        "down": {"uv": [0, 0, 8, 16], "texture": "#bottom", "cullface": "down"}, "up": {"uv": [0, 0, 8, 16], "texture": "#top"},
        "north": side([8, 0, 16, 8], "north"), "south": side([0, 0, 8, 8], "south"), "west": side([0, 0, 16, 8], "west")}}
    back = {"from": [8, 0, 0], "to": [16, 8, 16], "faces": {
        "down": {"uv": [8, 0, 16, 16], "texture": "#bottom", "cullface": "down"},
        "north": side([0, 8, 8, 16], "north"), "south": side([8, 8, 16, 16], "south"), "east": side([0, 8, 16, 16], "east")}}
    els = []
    for e in (front, back, m["elements"][1]):
        e = json.loads(json.dumps(e))
        if grassy and "up" in e["faces"]:
            e["faces"]["up"]["tintindex"] = 0
        els.append(e)
        if grassy:
            els.append({**{k: v for k, v in e.items() if k != "faces"}, "faces": {
                d: {**f, "texture": "#overlay", "tintindex": 0} for d, f in e["faces"].items() if d in ("north", "south", "west", "east")}})
    w(A / f"models/block/{name}.json", {**m, "elements": els})


fringe_stairs("fringe_stairs", False)
fringe_stairs("grassy_fringe_stairs", True)


def slab_models(name, textures, bottom=M + "block/ground_slab", top="minecraft:block/slab_top"):
    w(A / f"models/block/{name}.json", {"parent": bottom, "textures": textures})
    w(A / f"models/block/{name}_top.json", {"parent": top, "textures": textures})


def slab_variants(name, double, extra=""):
    return {f"type=bottom{extra}": {"model": M + f"block/{name}"}, f"type=top{extra}": {"model": M + f"block/{name}_top"},
            f"type=double{extra}": {"model": double}}


def stair_models(name, textures, parents="minecraft:block/"):
    for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
        w(A / f"models/block/{name}{suffix}.json", {"parent": parents + parent, "textures": textures})


def stair_variants(name, extra=""):
    """Vanilla sandstone_stairs' variants on our models; extra is appended to each key (",snowy=false")."""
    state = json.loads(jar.read("assets/minecraft/blockstates/sandstone_stairs.json"))["variants"]
    return {k + extra: {**v, "model": v["model"].replace("minecraft:block/sandstone_stairs", M + "block/" + name)} for k, v in state.items()}


dirt = {"bottom": V + "dirt", "top": V + "dirt", "side": V + "dirt", "particle": V + "dirt"}
slab_models("dirt_slab", dirt)
w(A / "blockstates/dirt_slab.json", {"variants": slab_variants("dirt_slab", V + "dirt")})

grass = {"top": V + "grass_block_top", "side": V + "dirt", "bottom": V + "dirt", "overlay": V + "grass_block_side_overlay", "particle": V + "dirt"}
slab_models("grass_slab", grass, M + "block/grassy_ground_slab", M + "block/grassy_slab_top")
snowy = {"top": V + "snow", "side": V + "grass_block_snow", "bottom": V + "dirt", "particle": V + "dirt"}
slab_models("grass_slab_snowy", snowy)
w(A / "blockstates/grass_slab.json", {"variants": {**slab_variants("grass_slab", V + "grass_block", ",snowy=false"),
                                                   **slab_variants("grass_slab_snowy", V + "grass_block_snow", ",snowy=true")}})
GRASS_TINT = [{"type": "minecraft:grass", "downfall": 1.0, "temperature": 0.5}]
w(A / "items/grass_slab.json", {"model": {"type": "minecraft:model", "model": M + "block/grass_slab", "tints": GRASS_TINT}})

path = {"top": V + "dirt_path_top", "side": V + "dirt_path_side", "bottom": V + "dirt", "particle": V + "dirt"}
slab_models("dirt_path_slab", path, "minecraft:block/slab")  # the path's side texture is already cut to 15/16
w(A / "blockstates/dirt_path_slab.json", {"variants": slab_variants("dirt_path_slab", V + "dirt_path")})


def slab_loot(b, drop):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": M + drop, "modifier": [
            {"type": "minecraft:set_count", "count": 2, "condition": {"type": "minecraft:match_block", "blocks": M + b, "state": {"type": "double"}}},
            {"type": "minecraft:explosion_decay"}]}]}], "random_sequence": f"mechanical:blocks/{b}"}


def self_loot(b, drop):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                                                  "entries": [{"type": "minecraft:item", "name": M + drop}]}], "random_sequence": f"mechanical:blocks/{b}"}


for b in ("dirt_slab", "grass_slab", "dirt_path_slab"):
    w(D / f"mechanical/loot_table/blocks/{b}.json", slab_loot(b, "dirt_slab"))


def recipe(name, pattern, base, count):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": pattern,
                                              "key": {"#": base}, "result": {"id": M + name, "count": count}})


recipe("dirt_slab", ["###"], "minecraft:dirt", 6)

# ---- phase 2: ground slabs and stairs. name stem -> (base item for the recipe, textures, double-slab model)
def cube(tex):
    return {"top": tex, "side": tex, "bottom": tex, "particle": tex}


GROUND = {
    "coarse_dirt": ("minecraft:coarse_dirt", cube(V + "coarse_dirt"), V + "coarse_dirt"),
    "podzol": ("minecraft:podzol", {"top": V + "podzol_top", "side": V + "podzol_side", "bottom": V + "dirt", "particle": V + "dirt"}, V + "podzol"),
    "sand": ("minecraft:sand", cube(V + "sand"), V + "sand"),
    "red_sand": ("minecraft:red_sand", cube(V + "red_sand"), V + "red_sand"),
    "gravel": ("minecraft:gravel", cube(V + "gravel"), V + "gravel"),
    "moss": ("minecraft:moss_block", cube(V + "moss_block"), V + "moss_block"),
    "mud": ("minecraft:mud", cube(V + "mud"), V + "mud"),
    "peat": (M + "peat", cube(M + "block/peat"), M + "block/peat"),
    "loess": (M + "loess", cube(M + "block/loess"), M + "block/loess"),
    "wood_ash": (M + "wood_ash", cube(M + "block/wood_ash"), M + "block/wood_ash"),
}
STAIRS = ("coarse_dirt", "sand", "red_sand")
new_slabs, new_stairs = [], ["dirt_stairs", "grass_stairs"]
lang_new = {}
for stem, (base, tex, double) in GROUND.items():
    b = stem + "_slab"
    new_slabs.append(b)
    slab_models(b, tex)
    w(A / f"blockstates/{b}.json", {"variants": slab_variants(b, double)})
    recipe(b, ["###"], base, 6)
    if stem == "podzol":  # like vanilla podzol: dirt unless silk touch
        loot = slab_loot(b, "dirt_slab")
        entry = loot["pools"][0]["entries"][0]
        loot["pools"][0]["entries"] = [{"type": "minecraft:alternatives", "children": [
            {**entry, "name": M + b, "condition": "minecraft:tool/can_silk_touch"}, entry]}]
        w(D / f"mechanical/loot_table/blocks/{b}.json", loot)
    else:
        w(D / f"mechanical/loot_table/blocks/{b}.json", slab_loot(b, b))
    if stem in STAIRS:
        s = stem + "_stairs"
        new_stairs.append(s)
        stair_models(s, tex)
        w(A / f"blockstates/{s}.json", {"variants": stair_variants(s)})
        recipe(s, ["#  ", "## ", "###"], base, 4)
        w(D / f"mechanical/loot_table/blocks/{s}.json", self_loot(s, s))

# Dirt Stairs and Grass Stairs (grass on the Turf stairs' grassy models, a snowy look like the Grass Slab's)
stair_models("dirt_stairs", dirt)
w(A / "blockstates/dirt_stairs.json", {"variants": stair_variants("dirt_stairs")})
recipe("dirt_stairs", ["#  ", "## ", "###"], "minecraft:dirt", 4)
w(D / "mechanical/loot_table/blocks/dirt_stairs.json", self_loot("dirt_stairs", "dirt_stairs"))
for suffix, parent in (("", "stairs"), ("_inner", "inner_stairs"), ("_outer", "outer_stairs")):
    straight = suffix == ""  # the straight one shows the fringe on the lower step's front too
    w(A / f"models/block/grass_stairs{suffix}.json", {"parent": M + ("block/grassy_fringe_stairs" if straight else "block/grassy_" + parent), "textures": grass})
    w(A / f"models/block/grass_stairs_snowy{suffix}.json", {"parent": M + "block/fringe_stairs" if straight else "minecraft:block/" + parent, "textures": snowy})
w(A / "blockstates/grass_stairs.json", {"variants": {**stair_variants("grass_stairs", ",snowy=false"), **stair_variants("grass_stairs_snowy", ",snowy=true")}})
w(A / "items/grass_stairs.json", {"model": {"type": "minecraft:model", "model": M + "block/grass_stairs", "tints": GRASS_TINT}})
w(D / "mechanical/loot_table/blocks/grass_stairs.json", self_loot("grass_stairs", "dirt_stairs"))

# the shovel: vanilla's rule (turns_into_dirt_path with air above) after ours
air_above = {"type": "minecraft:matching_block_tag", "offset": [0, 1, 0], "tag": "minecraft:air"}
w(D / "minecraft/block_transformer/shovel.json", [{"block_state_provider": {"type": "minecraft:rule_based", "rules": [
    {"if_true": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_blocks", "blocks": [M + "dirt_slab", M + "grass_slab"]}, air_above]},
     "then": {"type": "minecraft:copy_properties", "source": {"id": M + "dirt_path_slab"}}},
    {"if_true": {"type": "minecraft:all_of", "predicates": [
        {"type": "minecraft:matching_block_tag", "tag": "minecraft:turns_into_dirt_path"}, air_above]},
     "then": {"id": "minecraft:dirt_path"}}]},
    "disallowed_faces": ["down"], "item_damage_per_use": 1, "sound": "minecraft:item.shovel.flatten"}])

# the smoothing pass (worldgen/SmoothGround), once per chunk in every overworld biome, last in top_layer_modification:
# after the trails and the snow. The modifier's name sorts after add_trails, and modifiers apply in name order.
w(D / "mechanical/worldgen/feature/smooth_ground.json", {"type": M + "smooth_ground"})
w(D / "mechanical/worldgen/placed_feature/smooth_ground.json", {"feature": M + "smooth_ground", "placement": []})
w(D / "mechanical/neoforge/biome_modifier/smooth_ground.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                                  "features": M + "smooth_ground", "step": "top_layer_modification"})

slabs = [M + b for b in ("dirt_slab", "grass_slab", "dirt_path_slab")] + [M + b for b in new_slabs]
stairs = [M + b for b in new_stairs]
tag_add("minecraft/tags/block/mineable/shovel.json", slabs + stairs)
tag_add("minecraft/tags/block/slabs.json", slabs)
tag_add("minecraft/tags/item/slabs.json", slabs)
tag_add("minecraft/tags/block/stairs.json", stairs)
tag_add("minecraft/tags/item/stairs.json", stairs)


def title(name):
    return " ".join(p.capitalize() for p in name.split("_"))


lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({"block.mechanical.dirt_slab": "Dirt Slab", "block.mechanical.grass_slab": "Grass Slab", "block.mechanical.dirt_path_slab": "Dirt Path Slab"})
lang.update({f"block.mechanical.{b}": title(b) for b in new_slabs + new_stairs})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
