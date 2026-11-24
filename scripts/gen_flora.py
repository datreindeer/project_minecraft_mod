"""Flora phase 1 data (docs/flora-and-food-plan.md; Java: Flora, WildFlowerBlock, TallWildFlowerBlock, ThicketBushBlock,
worldgen/ThicketFeature): wild flowers (bloom / leaves models, item pictures, loot, worldgen patches), the magic Fern
Flower and Frost Lily, and the thorny thicket bushes (Blackthorn, Sea Buckthorn, Jujube: multipart models that join
to their neighbours and grow a top block, berries, loot, worldgen clumps), Cooked Elderberries, tags and lang.

Climate windows: each plant lists a range of biome base temperature (vanilla scale) and downfall; the biomes are worked
out here from every overworld land biome (vanilla's read from the patched jar, the mod's from their json), so a plant
lands only where its climate is. Specialists (ground or coast) list their biomes instead. The chosen biomes are printed.
Textures: the standalone scripts/gen_flora_textures.py.

Generator order: ... gen_writing.py, gen_flora.py, gen_winter_plants.py (tints the flowers' models) ...
Run: python scripts/gen_flora.py"""
import glob
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M, S = "minecraft:", "mechanical:"


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def tag(path, *values):
    t = read(path, {"values": []})
    for v in values:
        if v not in t["values"]:
            t["values"].append(v)
    w(path, t)


# ---------------------------------------------------------------- climate of every land biome
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
NOT_LAND = ("ocean", "river", "caves", "deep_dark", "mushroom", "peaks", "ice_spikes", "beach")
CLIMATE = {}
for b in json.loads(jar.read("data/minecraft/tags/worldgen/biome/is_overworld.json"))["values"]:
    if any(n in b for n in NOT_LAND):
        continue
    j = json.loads(jar.read(f"data/minecraft/worldgen/biome/{b.split(':')[1]}.json"))
    CLIMATE[b] = (j["temperature"], j["downfall"])
MOD_SURFACE = ["deciduous_forest", "hazel_meadow", "willow_fen", "pine_heath", "chalk_downs", "peat_bog", "slate_highlands",
               "soapstone_valley", "limestone_karst", "marble_gorge", "ochre_badlands", "basalt_coast", "shingle_beach", "reed_delta",
               "glacial_moraine", "gypsum_flats", "mammoth_steppe"]  # not the Scorched Barrens: burnt ground, its own plants
for b in MOD_SURFACE:
    j = read(D / f"mechanical/worldgen/biome/{b}.json", None)
    CLIMATE[S + b] = (j["temperature"], j["downfall"])


def climate(tmin, tmax, dmin=0.0, dmax=1.0, minus=()):
    return sorted(b for b, (t, d) in CLIMATE.items() if tmin <= t <= tmax and dmin <= d <= dmax and b.split(":")[1] not in minus)


# ---------------------------------------------------------------- placement
def ground(dry):
    tags = ["minecraft:substrate_overworld"] + (["minecraft:sand", "minecraft:supports_dry_vegetation"] if dry else [])
    preds = [{"type": "minecraft:matching_block_tag", "tag": t, "offset": [0, -1, 0]} for t in tags]
    if dry:  # Flora.dryGround: gravel and shingle too
        preds.append({"type": "minecraft:matching_blocks", "blocks": [M + "gravel", S + "beach_pebbles"], "offset": [0, -1, 0]})
    return {"type": "minecraft:any_of", "predicates": preds}


tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}


def patch(name, state, biomes, rarity, count, spread, dry):
    """A patch of a plant: as the forage patches in gen_data.py, on natural ground (sand and terracotta too for dry kinds)."""
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": state})
    placement = [{"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
                 {"type": "minecraft:count", "count": count}]
    if spread:
        placement.append({"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)})
    placement += [{"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
                  {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                      {"type": "minecraft:replaceable"}, ground(dry)]}}]
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": S + name, "placement": placement})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
                                                                   "features": S + name, "step": "vegetal_decoration"})


lang = read(A / "lang/en_us.json", {})
TAGS_B, TAGS_I = D / "minecraft/tags/block", D / "minecraft/tags/item"

# ---------------------------------------------------------------- flowers
FORESTS = [M + b for b in ("forest", "birch_forest", "old_growth_birch_forest", "dark_forest", "flower_forest", "old_growth_pine_taiga",
                           "old_growth_spruce_taiga", "dappled_forest")] + [S + "deciduous_forest"]
# name: English, tall, dry ground, blooms in spring (worldgen state), biomes, (rarity, tries, spread)
FLOWERS = {
    "pasque_flower": ("Pasque Flower", False, False, True, climate(0.25, 0.8, 0.0, 0.5, minus=("shingle_beach",)), (3, 12, 4)),
    "harebell": ("Harebell", False, False, False, climate(0.1, 0.75, 0.3, 0.7), (3, 12, 4)),
    "lady_slipper": ("Lady's Slipper", False, False, True, [S + "limestone_karst", S + "chalk_downs"], (5, 8, 3)),
    "sea_thrift": ("Sea Thrift", False, True, True, [S + "shingle_beach", S + "basalt_coast", M + "stony_shore", M + "beach"], (2, 14, 4)),
    "mountain_avens": ("Mountain Avens", False, False, False, climate(-0.5, 0.4, 0.2, 0.7), (3, 12, 4)),
    "edelweiss": ("Edelweiss", False, False, False, [S + "slate_highlands", S + "mammoth_steppe", M + "windswept_hills",
                                                     M + "windswept_gravelly_hills"], (5, 8, 3)),
    "sand_verbena": ("Sand Verbena", False, True, True, [M + "desert", S + "gypsum_flats", M + "badlands", M + "eroded_badlands",
                                                         S + "ochre_badlands"], (4, 10, 4)),
    "desert_marigold": ("Desert Marigold", False, True, True, climate(1.0, 2.0, 0.0, 0.2), (3, 10, 4)),
    "great_mullein": ("Great Mullein", True, True, False, climate(0.3, 1.2, 0.0, 0.5, minus=("shingle_beach",)), (5, 6, 5)),
    "desert_candle": ("Desert Candle", True, True, True, climate(0.3, 2.0, 0.0, 0.1), (5, 6, 4)),
    # phase 2
    "foxglove": ("Foxglove", True, False, False, climate(0.5, 0.8, 0.5, 0.9, minus=("pale_garden", "cherry_grove", "willow_fen", "reed_delta", "swamp", "mangrove_swamp")),
                 (5, 8, 4)),
    "yellow_adonis": ("Yellow Adonis", False, False, True, climate(0.3, 0.9, 0.0, 0.45, minus=("shingle_beach",)), (4, 10, 4)),
    "bog_asphodel": ("Bog Asphodel", False, False, False, [S + "peat_bog"], (2, 16, 5)),
    "marsh_marigold": ("Marsh Marigold", False, False, True, [S + "willow_fen", S + "reed_delta", M + "swamp"], (3, 12, 4)),
    "wood_anemone": ("Wood Anemone", False, False, True, [S + "deciduous_forest", M + "forest", M + "birch_forest", M + "old_growth_birch_forest",
                                                          M + "flower_forest", M + "dappled_forest"], (4, 28, 5)),  # spring carpets
    "bluebells": ("Bluebells", False, False, True, [S + "deciduous_forest", M + "forest", M + "dark_forest"], (5, 28, 5)),
    "purple_saxifrage": ("Purple Saxifrage", False, False, True, [S + "slate_highlands", S + "glacial_moraine", M + "windswept_hills",
                                                                  M + "windswept_gravelly_hills", M + "stony_shore"], (3, 10, 3)),
    "purple_loosestrife": ("Purple Loosestrife", True, False, False, [S + "reed_delta", S + "willow_fen", M + "swamp", M + "river"], (3, 10, 4)),
    "spiny_thrift": ("Spiny Thrift", False, True, True, [S + "gypsum_flats", S + "ochre_badlands", S + "marble_gorge"], (4, 8, 4)),
    "cats_foot": ("Cat's Foot", False, False, False, [S + "pine_heath", S + "slate_highlands", M + "meadow"], (3, 10, 3)),
}
MAGIC = {
    "fern_flower": ("Fern Flower", FORESTS, (400, 1, 0)),
    "frost_lily": ("Frost Lily", [S + "slate_highlands", S + "glacial_moraine", M + "snowy_plains", M + "snowy_taiga", M + "grove",
                                  M + "snowy_slopes"], (250, 2, 2)),
    # phase 3: glow only at night (the lily) or on full-moon nights (moonwort)
    "will_o_wisp_lily": ("Will-o'-Wisp Lily", [S + "peat_bog"], (60, 2, 3)),
    "moonwort": ("Moonwort", [S + "limestone_karst"], (90, 2, 2)),
}


def cross(name, tex):
    w(A / f"models/block/{name}.json", {"parent": M + "block/cross", "textures": {"cross": S + f"block/{tex}"}})


def flat_item(name, tex):
    w(A / f"models/item/{name}.json", {"parent": M + "item/generated", "textures": {"layer0": S + f"block/{tex}"}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": S + f"item/{name}"}})


for name, (en, tall, dry, spring, biomes, (rarity, tries, spread)) in FLOWERS.items():
    if tall:
        for half in ("bottom", "top"):
            cross(f"{name}_{half}", f"{name}_{half}")
            cross(f"{name}_{half}_leaves", f"{name}_{half}_leaves")
        w(A / f"blockstates/{name}.json", {"variants": {
            f"blooming={b},half={h}": {"model": S + f"block/{name}_{'bottom' if h == 'lower' else 'top'}{'' if b == 'true' else '_leaves'}"}
            for b in ("true", "false") for h in ("lower", "upper")}})
        flat_item(name, f"{name}_top")
        loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": S + name,
            "condition": {"type": "minecraft:match_block", "blocks": S + name, "state": {"half": "lower"}}}],
            "condition": {"type": "minecraft:survives_explosion"}}], "random_sequence": f"mechanical:blocks/{name}"}
    else:
        cross(name, name)
        cross(f"{name}_leaves", f"{name}_leaves")
        w(A / f"blockstates/{name}.json", {"variants": {"blooming=true": {"model": S + f"block/{name}"},
                                                         "blooming=false": {"model": S + f"block/{name}_leaves"}}})
        flat_item(name, name)
        loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                                                      "entries": [{"type": "minecraft:item", "name": S + name}]}],
                "random_sequence": f"mechanical:blocks/{name}"}
    w(D / f"mechanical/loot_table/blocks/{name}.json", loot)
    lang[f"block.mechanical.{name}"] = en
    patch(name, {"id": S + name, "properties": {"blooming": str(spring).lower()}}, biomes, rarity, tries, spread, dry)
    tag(TAGS_B / "flowers.json", S + name)
    tag(TAGS_I / "flowers.json", S + name)
    tag(TAGS_B / "replaceable_by_trees.json", S + name)
    tag(D / "mechanical/tags/item/compostable.json", S + name)
    print(f"{name}: {', '.join(b.split(':')[1] for b in biomes)}")

def glow_cross(name):
    """The plant as a cross, and its blossom again on top, drawn full-bright (light_emission) so it shines in the dark."""
    def planes(tex, light):
        out = []
        for f, t, faces in (([0.8, 0, 8], [15.2, 16, 8], ("north", "south")), ([8, 0, 0.8], [8, 16, 15.2], ("east", "west"))):
            e = {"from": f, "to": t, "shade": False, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
                 "faces": {d: {"uv": [0, 0, 16, 16], "texture": tex} for d in faces}}
            if light:
                e["light_emission"] = 15
            out.append(e)
        return out
    w(A / f"models/block/{name}.json", {"ambientocclusion": False, "textures": {"particle": S + f"block/{name}", "cross": S + f"block/{name}",
                                                                              "glow": S + f"block/{name}_glow"},
                                        "elements": planes("#cross", False) + planes("#glow", True)})


def fern_flower_models():
    """Fern Flower: a rosette of eight arching fronds (four leaning out, four upright on the diagonals), a slim stalk, and in
    bloom one five-petalled flower on top (seen from above, its cup from the side), drawn full-bright; out of bloom the
    stalk ends in a curled bud."""
    def plane(f, t, axis_faces, uv, tex, rot=None, light=False):
        e = {"from": f, "to": t, "shade": False, "faces": {d: {"uv": uv, "texture": tex} for d in axis_faces}}
        if rot:
            e["rotation"] = rot
        if light:
            e["light_emission"] = 15
        return e
    NS, EW = ("north", "south"), ("east", "west")
    fronds = [
        plane([8, 0, 8], [18, 13, 8], NS, [0, 0, 16, 16], "#frond", {"origin": [8, 0, 8], "axis": "z", "angle": -22.5}),
        plane([-2, 0, 8], [8, 13, 8], NS, [16, 0, 0, 16], "#frond", {"origin": [8, 0, 8], "axis": "z", "angle": 22.5}),
        plane([8, 0, 8], [8, 13, 18], EW, [16, 0, 0, 16], "#frond", {"origin": [8, 0, 8], "axis": "x", "angle": 22.5}),
        plane([8, 0, -2], [8, 13, 8], EW, [0, 0, 16, 16], "#frond", {"origin": [8, 0, 8], "axis": "x", "angle": -22.5}),
        plane([8, 0, 8], [15, 9, 8], NS, [0, 0, 16, 16], "#frond", {"origin": [8, 0, 8], "axis": "y", "angle": 45}),
        plane([1, 0, 8], [8, 9, 8], NS, [16, 0, 0, 16], "#frond", {"origin": [8, 0, 8], "axis": "y", "angle": 45}),
        plane([8, 0, 8], [15, 9, 8], NS, [0, 0, 16, 16], "#frond", {"origin": [8, 0, 8], "axis": "y", "angle": -45}),
        plane([1, 0, 8], [8, 9, 8], NS, [16, 0, 0, 16], "#frond", {"origin": [8, 0, 8], "axis": "y", "angle": -45}),
    ]
    stalk = [plane([4, 0, 8], [12, 10, 8], NS, [4, 6, 12, 16], "#stalk", {"origin": [8, 8, 8], "axis": "y", "angle": a}) for a in (45, -45)]
    flower = [{"from": [4, 10.5, 4], "to": [12, 10.5, 12], "shade": False, "light_emission": 15,
               "faces": {"up": {"uv": [4, 4, 12, 12], "texture": "#top"}, "down": {"uv": [4, 4, 12, 12], "texture": "#top"}}}]
    flower += [plane([4, 6, 8], [12, 12, 8], NS, [4, 10, 12, 16], "#side", {"origin": [8, 8, 8], "axis": "y", "angle": a}, True)
               for a in (45, -45)]
    bud = [plane([4, 0, 8], [12, 10, 8], NS, [4, 6, 12, 16], "#bud", {"origin": [8, 8, 8], "axis": "y", "angle": a}) for a in (45, -45)]
    T = {"particle": S + "block/fern_flower_frond", "frond": S + "block/fern_flower_frond", "stalk": S + "block/fern_flower_stalk",
         "top": S + "block/fern_flower_top", "side": S + "block/fern_flower_side", "bud": S + "block/fern_flower_bud"}
    w(A / "models/block/fern_flower.json", {"ambientocclusion": False, "textures": T, "elements": fronds + stalk + flower})
    w(A / "models/block/fern_flower_leaves.json", {"ambientocclusion": False, "textures": T, "elements": fronds + bud})


for name, (en, biomes, (rarity, tries, spread)) in MAGIC.items():
    if name == "fern_flower":
        fern_flower_models()
    else:
        glow_cross(name)
        cross(f"{name}_leaves", f"{name}_leaves")
    w(A / f"blockstates/{name}.json", {"variants": {"blooming=true": {"model": S + f"block/{name}"},
                                                     "blooming=false": {"model": S + f"block/{name}_leaves"}}})
    flat_item(name, name)
    # only picked in bloom does it give itself; out of bloom the fern gives a little fibre, the lily nothing
    entries = [{"type": "minecraft:item", "name": S + name, "condition": {"type": "minecraft:match_block", "blocks": S + name,
                                                                          "state": {"blooming": "true"}}}]
    if name == "fern_flower":
        entries.append({"type": "minecraft:item", "name": S + "plant_fibre"})
    w(D / f"mechanical/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:alternatives", "children": entries}]}], "random_sequence": f"mechanical:blocks/{name}"})
    lang[f"block.mechanical.{name}"] = en
    patch(name, {"id": S + name, "properties": {"blooming": "false"}}, biomes, rarity, tries, spread, False)
    print(f"{name}: {', '.join(b.split(':')[1] for b in biomes)}")

# ---------------------------------------------------------------- thicket bushes
# name: English, berries id, berries English, (temp min, max, downfall min, max) = Flora.THICKET_KINDS, worldgen rarity
THICKETS = {
    "blackthorn": ("Blackthorn", "sloes", "Sloes", (0.5, 0.9, 0.3, 0.8), 9),
    "sea_buckthorn": ("Sea Buckthorn", "sea_buckthorn_berries", "Sea Buckthorn Berries", (0.0, 0.5, 0.0, 0.6), 6),
    "jujube": ("Jujube", "jujubes", "Jujubes", (1.0, 2.0, 0.0, 0.5), 6),
    # phase 2 (Flora.THICKET_KINDS; low ones are mats/mounds)
    "rosehip": ("Dog Rose", "rosehips", "Rosehips", (0.4, 0.8, 0.3, 0.6), 7),
    "crowberry": ("Crowberry", "crowberries", "Crowberries", (-0.5, 0.4, 0.3, 0.9), 5),
    "bearberry": ("Bearberry", "bearberries", "Bearberries", (0.15, 0.45, 0.1, 0.6), 5),
    "barberry": ("Barberry", "barberries", "Barberries", (0.6, 1.3, 0.0, 0.5), 7),
    "caper": ("Caper Bush", "capers", "Capers", (1.0, 1.6, 0.0, 0.3), 3),
    "red_currant": ("Red Currant", "red_currants", "Red Currants", (0.6, 0.8, 0.8, 1.0), 8),
    "black_currant": ("Black Currant", "black_currants", "Black Currants", (0.25, 0.6, 0.8, 1.0), 7),
}
LOW = {"crowberry", "bearberry", "caper"}
LOOKS = ["stage0", "stage1", "stage2", "stage3", "bare", "bare_ripe"]
LOOK_WHEN = {"stage0": {"leafless": "false", "age": "0"}, "stage1": {"leafless": "false", "age": "1"},
             "stage2": {"leafless": "false", "age": "2"}, "stage3": {"leafless": "false", "age": "3"},
             "bare": {"leafless": "true", "age": "0|1|2"}, "bare_ripe": {"leafless": "true", "age": "3"}}


def face(u0, v0, u1, v1, tex="#leaves"):
    return {"uv": [u0, v0, u1, v1], "texture": tex, "tintindex": 0}


def leaf_box(f, t, faces=("north", "south", "east", "west", "up"), tex="#leaves"):
    """A box of leaves; each face shows the part of the texture under it, so neighbouring boxes run on."""
    (x0, y0, z0), (x1, y1, z1) = f, t
    v0, v1 = max(0, 16 - y1), 16 - y0
    uv = {"north": (16 - x1, v0, 16 - x0, v1), "south": (x0, v0, x1, v1), "west": (z0, v0, z1, v1), "east": (16 - z1, v0, 16 - z0, v1),
          "up": (x0, z0, x1, z1), "down": (x0, 16 - z1, x1, 16 - z0)}
    return {"from": list(f), "to": list(t), "faces": {d: face(*uv[d], tex) for d in faces}}


def stems(x0, x1, top):
    """Two crossed planes of branches inside the leaves (as vanilla's cross)."""
    return [{"from": [x0, 0, 8], "to": [x1, top, 8], "shade": False,
             "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": a, "rescale": True},
             "faces": {d: {"uv": [x0, max(0, 16 - top), x1, 16], "texture": "#stems", "tintindex": 0} for d in ("north", "south")}}
            for a in (45, -45)]


def sprigs(y0, y1, reach=1.5):
    """Two upright planes of leafy shoots (#sprig: a few shoots on clear ground) through the middle, wider than the bush:
    their ends stick out of the sides as shoots, so the outline is ragged, not a box."""
    v = [16 - y1, 16 - y0]
    return [{"from": [-reach, y0, 8], "to": [16 + reach, y1, 8], "shade": False,
             "faces": {d: {"uv": [0, v[0], 16, v[1]], "texture": "#sprig", "tintindex": 0} for d in ("north", "south")}},
            {"from": [8, y0, -reach], "to": [8, y1, 16 + reach], "shade": False,
             "faces": {d: {"uv": [0, v[0], 16, v[1]], "texture": "#sprig", "tintindex": 0} for d in ("east", "west")}}]


SHAPES = {  # part: core elements (a darker heart, a narrow foot, a bulging middle, a rounded top, sprigs and twigs poking out), north side lobe
    "lower": ([leaf_box((3, 0.5, 3), (13, 12, 13), tex="#inner"), leaf_box((2.5, 0, 2.5), (13.5, 3, 13.5)),
               leaf_box((0.5, 3, 0.5), (15.5, 11, 15.5)), leaf_box((2, 11, 2), (14, 14, 14)), leaf_box((4, 14, 4), (12, 16, 12))]
              + sprigs(2, 13) + stems(0, 16, 20),
              [leaf_box((1.5, 2, 0), (14.5, 12, 0.5), ("up", "down", "east", "west"))]),
    "upper": ([leaf_box((3.5, 0, 3.5), (12.5, 8, 12.5), tex="#inner"), leaf_box((1, 0, 1), (15, 6, 15)),
               leaf_box((2.5, 6, 2.5), (13.5, 10, 13.5)), leaf_box((4.5, 10, 4.5), (11.5, 12.5, 11.5))]
              + sprigs(0, 9, 1.0) + stems(1, 15, 16),
              [leaf_box((2, 0, 0), (14, 7, 1), ("up", "east", "west"))]),
    "young": ([leaf_box((4.5, 0, 4.5), (11.5, 6, 11.5)), leaf_box((5.5, 6, 5.5), (10.5, 8.5, 10.5))] + stems(3, 13, 11), None),
}

LOW_SHAPES = {  # a mat / mound about 6 px high: a darker heart, a wide foot, a rounded top, leafy sprigs breaking the outline
    "lower": ([leaf_box((3, 0.5, 3), (13, 5, 13), tex="#inner"), leaf_box((0.5, 0, 0.5), (15.5, 4, 15.5)),
               leaf_box((2.5, 4, 2.5), (13.5, 6, 13.5)), leaf_box((5, 6, 5), (11, 7, 11))] + sprigs(0, 5, 1.0),
              [leaf_box((1.5, 0, 0), (14.5, 3.5, 0.5), ("up", "east", "west"))]),
    "upper": SHAPES["upper"],  # never grown (ThicketBushBlock: low kinds grow no top), kept so the blockstate resolves
    "young": ([leaf_box((4.5, 0, 4.5), (11.5, 3, 11.5)), leaf_box((6, 3, 6), (10, 4, 10))], None),
}

for name, (en, berries, berries_en, (tmin, tmax, dmin, dmax), rarity) in THICKETS.items():
    shapes = LOW_SHAPES if name in LOW else SHAPES
    def tex(look):
        bare = look.startswith("bare")  # winter: the heart and the branch tips are bare twigs too
        return {"particle": S + f"block/{name}_{look}", "leaves": S + f"block/{name}_{look}",
                "inner": S + f"block/{name}_{'bare' if bare else 'inner'}", "stems": S + f"block/{name}_stems{'_bare' if bare else ''}",
                "sprig": S + f"block/{name}_sprig{'_bare' if bare else ''}"}
    for part, (core, lobe) in shapes.items():
        for look in (["stage0", "bare"] if part == "young" else LOOKS):
            suffix = "" if look == "stage0" and part == "young" else f"_{look}"
            w(A / f"models/block/{name}_{part}{suffix}.json", {"parent": M + "block/block", "ambientocclusion": False,
                                                               "textures": tex(look), "elements": core})
            if lobe:
                w(A / f"models/block/{name}_{part}_side_{look}.json", {"ambientocclusion": False, "textures": tex(look), "elements": lobe})
    parts = [{"when": {"young": "true", "leafless": "false"}, "apply": {"model": S + f"block/{name}_young"}},
             {"when": {"young": "true", "leafless": "true"}, "apply": {"model": S + f"block/{name}_young_bare"}}]
    for half in ("lower", "upper"):
        for look in LOOKS:
            when = {"young": "false", "half": half, **LOOK_WHEN[look]}
            parts.append({"when": when, "apply": {"model": S + f"block/{name}_{half}_{look}"}})
            for side, rot in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
                parts.append({"when": {**when, side: "true"}, "apply": {"model": S + f"block/{name}_{half}_side_{look}", "y": rot}})
    w(A / f"blockstates/{name}.json", {"multipart": parts})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": S + f"block/{name}_lower_stage3"}})
    w(D / f"mechanical/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": M + "stick",
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": S + "plant_fibre",
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
        "random_sequence": f"mechanical:blocks/{name}"})
    lang[f"block.mechanical.{name}"] = en
    flat = {"parent": M + "item/generated", "textures": {"layer0": S + f"item/{berries}"}}
    w(A / f"models/item/{berries}.json", flat)
    w(A / f"items/{berries}.json", {"model": {"type": "minecraft:model", "model": S + f"item/{berries}"}})
    lang[f"item.mechanical.{berries}"] = berries_en
    biomes = climate(tmin, tmax, dmin, dmax, minus=("pale_garden", "mangrove_swamp"))
    w(D / f"mechanical/worldgen/feature/{name}_thicket.json", {"type": S + f"{name}_thicket"})
    w(D / f"mechanical/worldgen/placed_feature/{name}_thicket.json", {"feature": S + f"{name}_thicket", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}_thicket.json", {"type": "neoforge:add_features", "biomes": biomes,
                                                                           "features": S + f"{name}_thicket", "step": "vegetal_decoration"})
    tag(D / "mechanical/tags/block/wild_berry_bushes.json", S + name)
    for t in ("basket_storable", "dryable_berries", "perishes_2_days", "snare_bait"):
        tag(D / f"mechanical/tags/item/{t}.json", S + berries)
    print(f"{name}_thicket: {', '.join(b.split(':')[1] for b in biomes)}")

# ---------------------------------------------------------------- the old berry bushes: climate windows (phase 2)
# temp min, max, downfall min, max (plan section 2); ripening seasons are Foraging.RIPE_SEASONS. Replaces gen_data.py's fixed lists.
BUSH_CLIMATE = {"raspberry": (0.5, 0.8, 0.5, 0.9), "blueberry": (0.2, 0.7, 0.6, 1.0), "lingonberry": (-0.5, 0.4, 0.4, 0.9),
                "wild_strawberry": (0.5, 0.9, 0.3, 0.8), "elderberry": (0.5, 0.9, 0.5, 1.0), "cloudberry": (-0.5, 0.3, 0.6, 1.0),
                "blackberry": (0.2, 0.9, 0.4, 0.9)}
for bush, (tmin, tmax, dmin, dmax) in BUSH_CLIMATE.items():
    biomes = climate(tmin, tmax, dmin, dmax, minus=("pale_garden", "mangrove_swamp", "jungle", "bamboo_jungle", "sparse_jungle"))
    path = D / f"mechanical/neoforge/biome_modifier/add_{bush}_bush.json"
    mod = read(path, None)
    mod["biomes"] = biomes
    w(path, mod)
    print(f"{bush}_bush: {', '.join(b.split(':')[1] for b in biomes)}")

# ---------------------------------------------------------------- Cooked Rowan Berries (raw ones upset the stomach: Flora.rawRowanBerries)
w(A / "models/item/cooked_rowan_berries.json", {"parent": M + "item/generated", "textures": {"layer0": S + "item/cooked_rowan_berries"}})
w(A / "items/cooked_rowan_berries.json", {"model": {"type": "minecraft:model", "model": S + "item/cooked_rowan_berries"}})
lang["item.mechanical.cooked_rowan_berries"] = "Cooked Rowan Berries"
w(D / "mechanical/recipe/cooked_rowan_berries.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
    "experience": 0.1, "ingredient": S + "rowan_berries", "result": {"id": S + "cooked_rowan_berries"}})
for t in ("basket_storable", "perishes_3_days"):
    tag(D / f"mechanical/tags/item/{t}.json", S + "cooked_rowan_berries")

# ---------------------------------------------------------------- Cooked Elderberries (raw ones are poisonous: Flora.rawElderberries)
w(A / "models/item/cooked_elderberries.json", {"parent": M + "item/generated", "textures": {"layer0": S + "item/cooked_elderberries"}})
w(A / "items/cooked_elderberries.json", {"model": {"type": "minecraft:model", "model": S + "item/cooked_elderberries"}})
lang["item.mechanical.cooked_elderberries"] = "Cooked Elderberries"
w(D / "mechanical/recipe/cooked_elderberries.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
    "experience": 0.1, "ingredient": S + "elderberries", "result": {"id": S + "cooked_elderberries"}})
for t in ("basket_storable", "perishes_3_days"):
    tag(D / f"mechanical/tags/item/{t}.json", S + "cooked_elderberries")

w(A / "lang/en_us.json", lang, ascii=True)
print("flora ok")
