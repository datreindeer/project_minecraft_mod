"""Wild grain stands (docs/farming.md "Vilda sädesbestånd"; Java: WildGrainBlock, worldgen.WildGrainFeature, Farming.WILD_GRAINS):
block models and states (4 stages x 2 drawn variants, mirrored), stage loot, worldgen (rare noise-shaped stands, fewer
scattered single plants), lang, the Oatcakes dish item, oats as horse feed. Textures: gen_farming_textures.py.
Generator order: after gen_herbs (it adds Rice and the dishes this extends), before gen_winter_plants and gen_block_items.
Run: python scripts/gen_wildgrain.py"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    t = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    t["values"] += [v for v in values if v not in t["values"]]
    w(path, t)


def mc(*names):
    return [n if ":" in n else "minecraft:" + n for n in names]


# kind: (name, grain it gives)
KINDS = {"einkorn": ("Wild Einkorn", M + "einkorn_grain"), "emmer": ("Wild Emmer", M + "emmer_grain"),
         "barley": ("Wild Barley", M + "barley_grain"), "oats": ("Wild Oats", M + "oat_grain"), "rice": ("Wild Rice", M + "rice")}
STAGES = ["young", "heading", "ripe", "shattered"]
lang = {}

# ---------------------------------------------------------------- models: a cross, Wild Rice a tall one (32 px, 1.5 blocks of plant
# standing in the water); its texture is 32x32 with variant a on the left half and b on the right
def tall_cross(tex, half):
    u0 = 0 if half == "a" else 8
    face = {"uv": [u0, 0, u0 + 8, 16], "texture": "#cross"}
    rot = {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True}
    return {"ambientocclusion": False, "textures": {"particle": tex, "cross": tex}, "elements": [
        {"from": [0.8, 0, 8], "to": [15.2, 32, 8], "rotation": rot, "shade": False, "faces": {"north": face, "south": face}},
        {"from": [8, 0, 0.8], "to": [8, 32, 15.2], "rotation": rot, "shade": False, "faces": {"west": face, "east": face}}]}


for kind, (name, grain) in KINDS.items():
    block = f"wild_{kind}"
    lang[f"block.mechanical.{block}"] = name
    variants = {}
    for s, st in enumerate(STAGES):
        models = []
        for v in "ab":
            model = f"{block}_{st}_{v}"
            if kind == "rice":
                w(A / f"models/block/{model}.json", tall_cross(f"mechanical:block/{block}_{st}", v))
            else:
                w(A / f"models/block/{model}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{model}"}})
            models += [{"model": M + "block/" + model}, {"model": M + "block/" + model, "y": 90}]  # turned a quarter: mirrored
        if kind == "rice":
            for wl in ("false", "true"):
                variants[f"stage={s},waterlogged={wl}"] = models
        else:
            variants[f"stage={s}"] = models
    w(A / f"blockstates/{block}.json", {"variants": variants})
    # the item (creative) shows the ripe plant
    w(A / f"models/item/{block}.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": f"mechanical:block/{block}_ripe" if kind == "rice" else f"mechanical:block/{block}_ripe_a"}})
    w(A / f"items/{block}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{block}"}})

    # loot by stage: young, a few of last year's ears still hang on (15% cut, 8% plucked); green ears 25% / 12%;
    # ripe: 1 grain cut, half the time plucked, a sickle adds Straw (and its bonus grain: Farming.onWildGrainDrops);
    # shattered: the seed is gone, Straw and a 20% (10%) chance of a grain left in an ear
    stage = lambda n: {"type": "minecraft:match_block", "blocks": M + block, "state": {"stage": str(n)}}
    cut = {"type": "minecraft:any_of", "terms": [{"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:knives"}},
                                                 {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:sickles"}}]}
    sickle = {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:sickles"}}
    chance = lambda c: {"type": "minecraft:random_chance", "chance": c}
    every = lambda *t: {"type": "minecraft:all_of", "terms": list(t)}
    item = lambda n, cond: {"type": "minecraft:item", "name": n, "condition": cond}
    # cut: c_cut; otherwise (not cut, so a failed cut roll doesn't fall through to the hand roll) c_hand
    pick = lambda n, c_cut, c_hand: {"type": "minecraft:alternatives", "children": [
        item(n, every(cut, chance(c_cut))), item(n, every({"type": "minecraft:inverted", "term": cut}, chance(c_hand)))]}
    pools = [{"rolls": 1, "condition": stage(0), "entries": [pick(grain, 0.15, 0.08)]},
             {"rolls": 1, "condition": stage(1), "entries": [pick(grain, 0.25, 0.12)]},
             {"rolls": 1, "condition": stage(2), "entries": [pick(grain, 1.0, 0.5)]},
             {"rolls": 1, "condition": every(stage(2), sickle), "entries": [{"type": "minecraft:item", "name": M + "straw"}]},
             {"rolls": 1, "condition": stage(3), "entries": [pick(M + "straw", 1.0, 0.5)]},
             {"rolls": 1, "condition": stage(3), "entries": [pick(grain, 0.2, 0.1)]}]
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": pools, "random_sequence": f"mechanical:blocks/{block}"})

# ---------------------------------------------------------------- worldgen
# stands: (biomes, one in N chunks). Rice tries more often: most spots in a marsh have no shallow edge.
STANDS = {
    "einkorn": (mc("plains", "sunflower_plains", "meadow", "mechanical:chalk_downs", "mechanical:hazel_meadow", "mechanical:limestone_karst"), 22),
    "emmer": (mc("plains", "sunflower_plains", "savanna", "mechanical:hazel_meadow"), 30),
    "barley": (mc("savanna", "savanna_plateau", "windswept_savanna", "sunflower_plains"), 24),
    "oats": (mc("meadow", "plains", "mechanical:hazel_meadow", "mechanical:chalk_downs"), 24),
    "rice": (mc("swamp", "mangrove_swamp", "mechanical:reed_delta", "mechanical:willow_fen"), 2),
}
for kind, (biomes, rarity) in STANDS.items():
    name = f"wild_{kind}_stand"
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": f"mechanical:{name}"})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})

# scattered single plants, fewer than before the stands (placement as gen_data.forage_feature); the old names stay (Retrogen)
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
SCATTER = {
    "wild_einkorn": ("einkorn", mc("plains", "sunflower_plains", "meadow", "savanna", "mechanical:hazel_meadow"), 16, 4),
    "wild_emmer": ("emmer", mc("plains", "sunflower_plains", "mechanical:hazel_meadow"), 18, 4),
    "wild_emmer_forest": ("emmer", mc("forest", "flower_forest", "mechanical:deciduous_forest"), 32, 3),
    "wild_barley": ("barley", mc("savanna", "savanna_plateau", "windswept_savanna"), 16, 4),
    "wild_oats": ("oats", mc("meadow", "plains", "mechanical:hazel_meadow"), 18, 4),
}
for name, (kind, biomes, rarity, n) in SCATTER.items():
    # one configured feature per kind (the unit codec decodes to one object; two configured features of it clash)
    w(D / f"mechanical/worldgen/feature/wild_{kind}.json", {"type": f"mechanical:wild_{kind}_scatter"})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:wild_{kind}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"}, {"type": "minecraft:count", "count": n},
        {"type": "minecraft:offset", "x": tri(3), "y": 0, "z": tri(3)},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"},
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:substrate_overworld", "offset": [0, -1, 0]}]}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})

# ---------------------------------------------------------------- tags, the oat dish, lang
tag_add(D / "mechanical/tags/block/mineable/sickle.json", *[f"mechanical:wild_{k}" for k in KINDS])
tag_add(D / "minecraft/tags/item/horse_food.json", M + "oat_grain")  # oats: the horse's grain
tag_add(D / "mechanical/tags/item/perishes_3_days.json", M + "oatcakes")
w(A / "models/item/oatcakes.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/oatcakes"}})
w(A / "items/oatcakes.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/oatcakes"}})
lang.update({
    "item.mechanical.oatcakes": "Oatcakes",
    "message.mechanical.wild_grain.0": "%s: green shoots among last year's straw | ears in about %s days",
    "message.mechanical.wild_grain.1": "%s: green ears | ripe in about %s days",
    "message.mechanical.wild_grain.2": "%s: ripe, cut it now! | the ears shatter in about %s days",
    "message.mechanical.wild_grain.3": "%s: the ears have shattered, the seed lies on the ground | ripe again in about %s days",
    "tooltip.mechanical.wild_grain_window": "%s ripens around %s %s; cut it within %s days, before the ears shatter",
})
lang_path = A / "lang/en_us.json"
full = json.loads(lang_path.read_text(encoding="utf-8"))
full.update(lang)
w(lang_path, full)
print(f"wild grain ok: {len(KINDS)} kinds, {len(STANDS)} stands, {len(SCATTER)} scattered")
