# Generates the mod's JSON resources (recipes, models, loot, tags, lang). usage: python scripts/gen_data.py src/main/resources
import json, sys
from pathlib import Path

R = Path(sys.argv[1])
A, D = R / "assets/mechanical", R / "data"

WRITTEN = set()  # files this run wrote, so later passes can build on them instead of the vanilla original
def w(path, obj):
    WRITTEN.add(Path(path).resolve())
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")

# ---- item models ----
TOOLS = ["crude_stone_pickaxe", "crude_stone_axe", "flint_pickaxe", "flint_axe", "flint_shovel", "flint_hoe", "flint_knife"]
FLAT = ["double_stone_axe_head", "cordage", "tool_rod", "double_stone_pickaxe_head", "plant_fibre", "twine", "stone_chunk", "granite_chunk", "diorite_chunk", "andesite_chunk",
        "stone_pickaxe_head", "stone_axe_head"]
for item in TOOLS + FLAT:
    parent = "minecraft:item/handheld" if item in TOOLS else "minecraft:item/generated"
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# ---- loose rock blocks ----
def box(f, t):
    return {"from": f, "to": t, "faces": {d: {"texture": "#all"} for d in ["down", "up", "north", "south", "east", "west"]}}

ROCK_SHAPES = {
    1: [box([5, 0, 6], [10, 1.5, 11]), box([7, 0, 5], [11, 2, 9])],
    2: [box([4, 0, 4], [12, 3, 11]), box([6, 0, 6], [11, 4, 12])],
    3: [box([2, 0, 2], [14, 5, 13]), box([4, 0, 4], [12, 7, 14])],
}
for size, elements in ROCK_SHAPES.items():
    w(A / f"models/block/rock_size{size}.json", {"textures": {"particle": "#all"}, "elements": elements})

# variant -> (texture, drop item)
ROCKS = {
    "stone": ("minecraft:block/stone", "mechanical:stone_chunk"),
    "granite": ("minecraft:block/granite", "mechanical:granite_chunk"),
    "diorite": ("minecraft:block/diorite", "mechanical:diorite_chunk"),
    "andesite": ("minecraft:block/andesite", "mechanical:andesite_chunk"),
    "flint": ("minecraft:block/blackstone", "minecraft:flint"),
}
for v, (tex, drop) in ROCKS.items():
    block = f"mechanical:loose_{v}"
    variants = {}
    for size in ROCK_SHAPES:
        model = f"mechanical:block/loose_{v}_size{size}"
        w(A / f"models/block/loose_{v}_size{size}.json", {"parent": f"mechanical:block/rock_size{size}", "textures": {"all": tex}})
        variants[f"size={size}"] = [{"model": model, "y": y} for y in (0, 90, 180, 270)]
    w(A / f"blockstates/loose_{v}.json", {"variants": variants})

    def counted(size, count):
        return {"type": "minecraft:set_count", "count": count,
                "condition": {"type": "minecraft:match_block", "blocks": block, "state": {"size": str(size)}}}
    w(D / f"mechanical/loot_table/blocks/loose_{v}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": drop, "modifier": [
            counted(2, 2), counted(3, {"type": "minecraft:uniform", "min": 3, "max": 4})]}]}],
        "random_sequence": f"mechanical:blocks/loose_{v}",
    })

# ---- worldgen: scatter rocks on the surface ----
for v in ROCKS:
    w(D / f"mechanical/worldgen/feature/loose_{v}.json", {
        "type": "minecraft:simple_block",
        "to_place": {"type": "minecraft:weighted", "entries": [
            {"data": {"id": f"mechanical:loose_{v}", "properties": {"size": "1"}}, "weight": 6},
            {"data": {"id": f"mechanical:loose_{v}", "properties": {"size": "2"}}, "weight": 3},
            {"data": {"id": f"mechanical:loose_{v}", "properties": {"size": "3"}}, "weight": 1}]},
    })
    tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
    w(D / f"mechanical/worldgen/placed_feature/loose_{v}.json", {
        "feature": f"mechanical:loose_{v}",
        "placement": [
            {"type": "minecraft:rarity_filter", "chance": 2 if v == "stone" else 4},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
            {"type": "minecraft:biome"},
            {"type": "minecraft:count", "count": 6},
            {"type": "minecraft:offset", "x": tri(6), "y": tri(2), "z": tri(6)},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:replaceable"},  # air, grass, ferns ...
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0],
                 "blocks": ["minecraft:grass_block", "minecraft:stone"] + ([] if v in ("stone", "flint") else [f"minecraft:{v}"])},
                {"type": "minecraft:would_survive", "state": f"mechanical:loose_{v}"}]}},
        ],
    })
w(D / "mechanical/neoforge/biome_modifier/add_loose_rocks.json", {
    "type": "neoforge:add_features",
    "biomes": "#minecraft:is_overworld",
    "features": [f"mechanical:loose_{v}" for v in ROCKS],
    "step": "vegetal_decoration",
})

# ---- tags ----
w(D / "mechanical/tags/item/flint_tool_materials.json", {"values": ["minecraft:flint"]})
w(D / "mechanical/tags/item/rock_chunks.json", {"values": [f"mechanical:{v}_chunk" for v in ["stone", "granite", "diorite", "andesite"]]})
for tag, item in [("pickaxes", ["flint_pickaxe", "crude_stone_pickaxe"]), ("axes", ["flint_axe", "crude_stone_axe", "thunderstone"]), ("shovels", "flint_shovel"), ("hoes", "flint_hoe"), ("swords", ["flint_knife", "iron_knife"])]:
    w(D / f"minecraft/tags/item/{tag}.json", {"values": [f"mechanical:{i}" for i in (item if isinstance(item, list) else [item])]})
w(D / "minecraft/tags/block/mineable/pickaxe.json", {"values": ["mechanical:tin_ore", "mechanical:tin_block"] + [f"mechanical:loose_{v}" for v in ROCKS]})

# ---- recipes ----
def shaped(name, pattern, key, result, ns="mechanical"):
    w(D / f"{ns}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
                                        "pattern": pattern, "key": key, "result": {"id": result}})

def shapeless(name, ingredients, result, count=1, ns="mechanical", category="misc"):
    w(D / f"{ns}/recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": category,
                                        "ingredients": ingredients, "result": {"id": result, "count": count}})

shapeless("twine", ["mechanical:plant_fibre"] * 3, "mechanical:twine")
shapeless("cordage", ["mechanical:twine"] * 3, "mechanical:cordage")
# Carving a tool rod out of a log with the flint knife (the knife survives, losing durability).
shaped("tool_rod", ["K", "L"], {"K": "#mechanical:knives", "L": "#minecraft:logs"}, "mechanical:tool_rod")

FTS = {"F": "minecraft:flint", "S": "minecraft:stick", "T": "mechanical:twine"}
shaped("flint_pickaxe", ["FFF", "TS ", " S "], FTS, "mechanical:flint_pickaxe")
shaped("flint_axe", ["FF", "TS"], FTS, "mechanical:flint_axe")
shaped("flint_shovel", ["F", "T", "S"], FTS, "mechanical:flint_shovel")
shaped("flint_hoe", ["FF", "TS", " S"], FTS, "mechanical:flint_hoe")
shaped("flint_knife", ["F ", "TS"], FTS, "mechanical:flint_knife")

# Knapping: rock chunks shaped with a flint knife (the knife survives, losing durability).
RK = {"R": "#mechanical:rock_chunks", "K": "#mechanical:knives"}
shaped("stone_pickaxe_head", ["RRR", " K "], RK, "mechanical:stone_pickaxe_head")
shaped("stone_axe_head", ["RR", "RK"], RK, "mechanical:stone_axe_head")

# Stone tools are assembled in the Tool Station (ToolStationRecipes.java), not the crafting table.
# Two single-sided pick heads knapped together make the double head for the full stone pickaxe.
shaped("double_stone_axe_head", ["HKH"], {"H": "mechanical:stone_axe_head", "K": "#mechanical:knives"}, "mechanical:double_stone_axe_head")
shaped("double_stone_pickaxe_head", ["HKH"], {"H": "mechanical:stone_pickaxe_head", "K": "#mechanical:knives"}, "mechanical:double_stone_pickaxe_head")
shaped("tool_station", ["FTF", "PCP"], {"F": "minecraft:flint", "T": "mechanical:twine", "P": "#minecraft:planks", "C": "minecraft:crafting_table"}, "mechanical:tool_station")

# Our own tree species (Trees.java): full wood sets in the mechanical namespace; also in WOODS (stumps, fallen logs, firewood).
NEW_WOODS = ["aspen", "alder", "beech", "linden"]
wns = lambda wood: "mechanical" if wood in NEW_WOODS else "minecraft"  # namespace of a wood's vanilla-style blocks and textures

# Planks: 2 per log until there is a saw.
for wood, logs in [("oak", "oak_logs"), ("spruce", "spruce_logs"), ("birch", "birch_logs"), ("jungle", "jungle_logs"),
                   ("acacia", "acacia_logs"), ("dark_oak", "dark_oak_logs"), ("mangrove", "mangrove_logs"),
                   ("cherry", "cherry_logs"), ("pale_oak", "pale_oak_logs"), ("poplar", "poplar_logs"),
                   ("crimson", "crimson_stems"), ("warped", "warped_stems")] + [(x, f"{x}_logs") for x in NEW_WOODS]:
    n = "mechanical" if wood in NEW_WOODS else "minecraft"
    w(D / f"{n}/recipe/{wood}_planks.json", {"type": "minecraft:crafting_shapeless", "category": "building", "group": "planks",
                                             "ingredients": [f"#{n}:{logs}"], "result": {"id": f"{n}:{wood}_planks", "count": 2}})

# Disabled vanilla recipes: these come back in later ages.
DISABLED = ["furnace"] + [f"{m}_{t}" for m in ["wooden", "copper", "iron", "diamond"]
                          for t in ["pickaxe", "axe", "shovel", "hoe", "sword", "spear"]] + [f"stone_{t}" for t in ["pickaxe", "axe", "shovel", "hoe", "sword"]]
for name in DISABLED:
    w(D / f"minecraft/recipe/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = {k: v for k, v in json.loads(lang_path.read_text()).items() if "example" not in k and "configuration" not in k}
title = lambda s: s.replace("_", " ").title()
for item in TOOLS + FLAT:
    lang[f"item.mechanical.{item}"] = title(item)
for v in ROCKS:
    lang[f"block.mechanical.loose_{v}"] = f"Loose {title(v)}"
w(lang_path, lang)
print("ok")

# Each vanilla recipe has an unlock advancement pointing at it; disable those too or registry loading fails.
for name in DISABLED:
    cat = "decorations" if name == "furnace" else "combat" if name.endswith(("sword", "spear")) else "tools"
    w(D / f"minecraft/advancement/recipes/{cat}/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
print("advancements ok")

# ---- tool station block ----
w(A / "blockstates/tool_station.json", {"variants": {"": {"model": "mechanical:block/tool_station"}}})
w(A / "models/block/tool_station.json", {"parent": "minecraft:block/cube", "textures": {
    "particle": "minecraft:block/crafting_table_front",
    "up": "minecraft:block/smithing_table_top", "down": "minecraft:block/oak_planks",
    "north": "minecraft:block/crafting_table_front", "south": "minecraft:block/crafting_table_front",
    "east": "minecraft:block/fletching_table_side", "west": "minecraft:block/fletching_table_side"}})
w(A / "items/tool_station.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/tool_station"}})
w(D / "mechanical/loot_table/blocks/tool_station.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "mechanical:tool_station"}]}],
    "random_sequence": "mechanical:blocks/tool_station"})
w(D / "minecraft/tags/block/mineable/axe.json", {"values": ["mechanical:tool_station"]})
lang.update({
    "block.mechanical.tool_station": "Tool Station",
    "entity.mechanical.falling_tree": "Falling Tree",
    "container.mechanical.tool_station": "Tool Station",
    "gui.mechanical.tool_station.head": "Head",
    "gui.mechanical.tool_station.binding": "Binding",
    "gui.mechanical.tool_station.rod": "Rod",
})
w(lang_path, lang)
print("tool station ok")

# ---- stumps left by felled trees ----
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar"] + NEW_WOODS
side = lambda uv: {"texture": "#side", "uv": uv}
w(A / "models/block/stump.json", {"textures": {"particle": "#side"}, "elements": [{"from": [2, 0, 2], "to": [14, 7, 14], "faces": {
    "up": {"texture": "#top", "uv": [2, 2, 14, 14]}, "down": {"texture": "#top", "uv": [2, 2, 14, 14], "cullface": "down"},
    "north": side([2, 9, 14, 16]), "south": side([2, 9, 14, 16]), "east": side([2, 9, 14, 16]), "west": side([2, 9, 14, 16])}}]})
for wood in WOODS:
    w(A / f"models/block/{wood}_stump.json", {"parent": "mechanical:block/stump",
        "textures": {"side": f"{wns(wood)}:block/{wood}_log", "top": f"{wns(wood)}:block/{wood}_log_top"}})
    w(A / f"blockstates/{wood}_stump.json", {"variants": {"": [{"model": f"mechanical:block/{wood}_stump", "y": y} for y in (0, 90, 180, 270)]}})
    w(D / f"mechanical/loot_table/blocks/{wood}_stump.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "minecraft:stick",
        "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
        "random_sequence": f"mechanical:blocks/{wood}_stump"})
    lang[f"block.mechanical.{wood}_stump"] = f"{title(wood)} Stump"
w(D / "minecraft/tags/block/mineable/axe.json", {"values": ["mechanical:tool_station"] + [f"mechanical:{x}_stump" for x in WOODS]})
w(lang_path, lang)
# ---- stump as chopping block: a rough log stood on it (log = 1 + index in WOODS) ----
w(A / "models/block/upright_log.json", {"textures": {"particle": "#side"}, "elements": [{"from": [3, 7, 3], "to": [13, 21, 13], "faces": {
    "up": {"texture": "#top", "uv": [3, 3, 13, 13]}, "down": {"texture": "#top", "uv": [3, 3, 13, 13]},
    "north": side([3, 2, 13, 16]), "south": side([3, 2, 13, 16]), "east": side([3, 2, 13, 16]), "west": side([3, 2, 13, 16])}}]})
for wood in WOODS:
    w(A / f"models/block/{wood}_upright_log.json", {"parent": "mechanical:block/upright_log",
        "textures": {"side": f"{wns(wood)}:block/{wood}_log", "top": f"{wns(wood)}:block/{wood}_log_top"}})
for wood in WOODS:
    w(A / f"blockstates/{wood}_stump.json", {"multipart": [{"apply": [{"model": f"mechanical:block/{wood}_stump", "y": y} for y in (0, 90, 180, 270)]}]
        + [{"when": {"log": str(i + 1)}, "apply": {"model": f"mechanical:block/{lw}_upright_log"}} for i, lw in enumerate(WOODS)]})

# ---- fallen logs (felled trees lie where they land): stage 0 bark, 1 debarked, 2-3 hewn thinner; built along z ----
def fallen_model(name, side_tex, end_tex, x0, x1, h):
    # z runs along the log; grain of the side texture must run along z, so east/west faces are rotated
    s = lambda: {"texture": "#side", "uv": [x0, 16 - h, x1, 16]}
    w(A / f"models/block/{name}.json", {"textures": {"particle": side_tex, "side": side_tex, "end": end_tex}, "elements": [{
        "from": [x0, 0, 0], "to": [x1, h, 16], "faces": {
            "north": {"texture": "#end", "uv": [x0, 16 - h, x1, 16], "cullface": "north"},
            "south": {"texture": "#end", "uv": [x0, 16 - h, x1, 16], "cullface": "south"},
            "up": {"texture": "#side", "uv": [x0, 0, x1, 16]}, "down": {"texture": "#side", "uv": [x0, 0, x1, 16], "cullface": "down"},
            "east": {"texture": "#side", "uv": [0, x0, 16, x1], "rotation": 90}, "west": {"texture": "#side", "uv": [0, x0, 16, x1], "rotation": 90}}}]})
FALLEN_STAGES = [(0, 16, 16, False), (0, 16, 16, True), (2, 14, 12, True), (4, 12, 8, True)]
for wood in WOODS:
    variants = {}
    for stage, (x0, x1, h, stripped) in enumerate(FALLEN_STAGES):
        n = wns(wood)
        tex = f"{n}:block/stripped_{wood}_log" if stripped else f"{n}:block/{wood}_log"
        fallen_model(f"{wood}_fallen_log_{stage}", tex, f"{n}:block/stripped_{wood}_log_top" if stripped else f"{n}:block/{wood}_log_top", x0, x1, h)
        variants[f"axis=z,stage={stage}"] = {"model": f"mechanical:block/{wood}_fallen_log_{stage}"}
        variants[f"axis=x,stage={stage}"] = {"model": f"mechanical:block/{wood}_fallen_log_{stage}", "y": 90}
    w(A / f"blockstates/{wood}_fallen_log.json", {"variants": variants})
    w(D / f"mechanical/loot_table/blocks/{wood}_fallen_log.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
        "type": "minecraft:item", "name": "minecraft:stick",
        "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
        "random_sequence": f"mechanical:blocks/{wood}_fallen_log"})
    lang[f"block.mechanical.{wood}_fallen_log"] = f"Fallen {title(wood)} Log"
    item = f"{wood}_rough_log"
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    lang[f"item.mechanical.{item}"] = f"Rough {title(wood)} Log"
w(A / "models/item/bark.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/bark"}})
w(A / "items/bark.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/bark"}})
lang["item.mechanical.bark"] = "Bark"
# bast: the inner bark gives fibre
shapeless("plant_fibre_from_bark", ["mechanical:bark", "#mechanical:knives"], "mechanical:plant_fibre", count=2)
# bark basket: a small 3x3 store for fruit, berries and hides
shaped("bark_basket", [" C ", "B B", "BBB"], {"C": "mechanical:cordage", "B": "mechanical:bark"}, "mechanical:bark_basket")
w(A / "models/item/bark_basket.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/bark_basket"}})
w(A / "items/bark_basket.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/bark_basket"}})
lang["item.mechanical.bark_basket"] = "Bark Basket"
BUSHES = {"raspberry": "raspberries", "blueberry": "blueberries", "lingonberry": "lingonberries",
          "wild_strawberry": "wild_strawberries", "elderberry": "elderberries", "cloudberry": "cloudberries"}
MUSHROOMS = ["chanterelle", "porcini", "morel", "oyster_mushroom", "fly_agaric", "puffball"]
w(D / "mechanical/tags/item/basket_storable.json", {"values": ["minecraft:apple", "minecraft:golden_apple", "minecraft:sweet_berries",
    "minecraft:glow_berries", "minecraft:melon_slice", "minecraft:chorus_fruit", "minecraft:leather", "minecraft:rabbit_hide",
    "mechanical:raw_hide", "mechanical:scraped_hide", "minecraft:brown_mushroom", "minecraft:red_mushroom", "mechanical:roasted_mushrooms",
    "mechanical:hazelnuts", "mechanical:roasted_hazelnuts", "mechanical:juniper_berries"]
    + [f"mechanical:{b}" for b in BUSHES.values()] + [f"mechanical:{m}" for m in MUSHROOMS]})

# ---- foraging: wild berry bushes (rare, regrow after picking) and mushrooms (spread slowly in shade) ----
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
def forage_feature(name, state, biomes, rarity, count, spread):
    w(D / f"mechanical/worldgen/feature/{name}.json", {"type": "minecraft:simple_block", "to_place": state})
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:offset", "x": tri(spread), "y": 0, "z": tri(spread)},
        # each spot finds the ground itself (a random height above/below hit the ground only a third of the time)
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"},  # air, grass, ferns ...
            {"type": "minecraft:matching_block_tag", "tag": "minecraft:substrate_overworld", "offset": [0, -1, 0]}]}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})
BUSH_BIOMES = {
    "raspberry": ["minecraft:forest", "minecraft:birch_forest", "minecraft:old_growth_birch_forest", "minecraft:flower_forest"],
    "blueberry": ["minecraft:forest", "minecraft:dark_forest", "minecraft:taiga", "minecraft:old_growth_pine_taiga"],
    "lingonberry": ["minecraft:taiga", "minecraft:snowy_taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga"],
    "wild_strawberry": ["minecraft:plains", "minecraft:meadow", "minecraft:flower_forest", "minecraft:sunflower_plains"],
    "elderberry": ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest", "minecraft:river"],
    "cloudberry": ["minecraft:swamp", "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove"],
}
for bush, berries in BUSHES.items():
    block = f"{bush}_bush"
    for stage in range(4):
        w(A / f"models/block/{block}_stage{stage}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{block}_stage{stage}"}})
    w(A / f"blockstates/{block}.json", {"variants": {f"age={a}": {"model": f"mechanical:block/{block}_stage{a}"} for a in range(4)}})
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:stick",
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]},
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mechanical:plant_fibre",
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
        "random_sequence": f"mechanical:blocks/{block}"})
    lang[f"block.mechanical.{block}"] = title(block)
    w(A / f"models/item/{berries}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{berries}"}})
    w(A / f"items/{berries}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{berries}"}})
    lang[f"item.mechanical.{berries}"] = title(berries)
    forage_feature(block, {"id": f"mechanical:{block}", "properties": {"age": "3"}},
                   BUSH_BIOMES[bush], 8, 7, 4)  # a patch of a few bushes
w(D / "mechanical/tags/block/wild_berry_bushes.json", {"values": [f"mechanical:{b}_bush" for b in BUSHES]})
def mushroom_loot(item, seq):
    """Cut cleanly with a knife: always. Plucked by hand: half the time it falls apart."""
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": item, "condition": {"type": "minecraft:match_tool", "predicate": {"items": "#mechanical:knives"}}},
        {"type": "minecraft:item", "name": item, "condition": {"type": "minecraft:random_chance", "chance": 0.5}}]}]}],
        "random_sequence": seq}
for vm in ("brown_mushroom", "red_mushroom"):
    w(D / f"minecraft/loot_table/blocks/{vm}.json", mushroom_loot(f"minecraft:{vm}", f"minecraft:blocks/{vm}"))
MUSHROOM_BIOMES = {
    "chanterelle": ["minecraft:forest", "minecraft:birch_forest", "minecraft:taiga", "minecraft:old_growth_birch_forest", "minecraft:old_growth_pine_taiga"],
    "porcini": ["minecraft:forest", "minecraft:dark_forest", "minecraft:taiga", "minecraft:old_growth_spruce_taiga"],
    "morel": ["minecraft:forest", "minecraft:flower_forest", "minecraft:birch_forest"],
    "oyster_mushroom": ["minecraft:dark_forest", "minecraft:swamp", "minecraft:old_growth_spruce_taiga"],
    "fly_agaric": ["minecraft:birch_forest", "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:dark_forest", "minecraft:old_growth_birch_forest"],
    "puffball": ["minecraft:plains", "minecraft:meadow", "minecraft:sunflower_plains"],
}
for m in MUSHROOMS:
    w(A / f"models/block/{m}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{m}"}})
    w(A / f"blockstates/{m}.json", {"variants": {"": {"model": f"mechanical:block/{m}"}}})
    w(D / f"mechanical/loot_table/blocks/{m}.json", mushroom_loot(f"mechanical:{m}", f"mechanical:blocks/{m}"))
    w(A / f"models/item/{m}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{m}"}})
    w(A / f"items/{m}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{m}"}})
    lang[f"block.mechanical.{m}"] = title(m)
    lang[f"item.mechanical.{m}"] = title(m)
    forage_feature(m, {"id": f"mechanical:{m}"}, MUSHROOM_BIOMES[m], 10, 14, 6)  # a mushroom field
w(D / "mechanical/tags/item/edible_mushrooms.json", {"values": [f"mechanical:{m}" for m in MUSHROOMS if m != "fly_agaric"] + ["minecraft:brown_mushroom"]})
w(A / "models/item/roasted_mushrooms.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/roasted_mushrooms"}})
w(A / "items/roasted_mushrooms.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/roasted_mushrooms"}})
lang["item.mechanical.roasted_mushrooms"] = "Roasted Mushrooms"
w(D / "mechanical/recipe/roasted_mushrooms.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 400,
    "experience": 0.1, "ingredient": "#mechanical:edible_mushrooms", "result": {"id": "mechanical:roasted_mushrooms"}})
print("stumps ok")

# ---- fire drill, campfire, kiln, crucible ----
for item in ["fire_drill", "unfired_crucible", "crucible"]:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
# Beehive-shaped clay kiln built facing north: walls with an arched front opening around the campfire below,
# a stepped dome and a chimney. Blockstate y-rotations turn it to face the player.
def kbox(f, t, faces=("down", "up", "north", "south", "east", "west"), tex="#clay"):
    return {"from": f, "to": t, "faces": {d: {"texture": tex} for d in faces}}
stone = lambda f, t: kbox(f, t, tex="#stone")
soot = lambda f, t: kbox(f, t, tex="#soot")
def log_z(f, t, tex):  # log lying along z, faces copied from vanilla's campfire
    return {"from": f, "to": t, "faces": {"north": {"uv": [0, 4, 4, 8], "texture": "#log"}, "south": {"uv": [0, 4, 4, 8], "texture": "#log"},
            "east": {"uv": [0, 1, 16, 5], "texture": tex}, "west": {"uv": [16, 0, 0, 4], "texture": tex},
            "up": {"uv": [0, 0, 16, 4], "rotation": 90, "texture": tex}, "down": {"uv": [0, 0, 16, 4], "rotation": 90, "texture": "#log"}}}
def log_x(f, t, tex):  # log lying along x
    return {"from": f, "to": t, "faces": {"east": {"uv": [0, 4, 4, 8], "texture": "#log"}, "west": {"uv": [0, 4, 4, 8], "texture": "#log"},
            "north": {"uv": [16, 0, 0, 4], "texture": tex}, "south": {"uv": [0, 1, 16, 5], "texture": tex},
            "up": {"uv": [0, 0, 16, 4], "texture": tex}, "down": {"uv": [0, 0, 16, 4], "texture": "#log"}}}
def flame(f, t, faces):
    return {"from": f, "to": t, "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45}, "shade_direction_override": "up",
            "faces": {d: {"uv": [0, 0, 16, 16], "texture": "#fire"} for d in faces}}

# Two-block clay kiln, built facing north, tapering upwards. LOWER = firebox (replaces the campfire): a ring of stones,
# clay walls with an arched, sooty fire opening, campfire logs inside and flames when lit.
# UPPER = baking chamber with a sooty loading opening, a stepped dome and a chimney blackened at the top.
KILN_TEX = {"particle": "minecraft:block/clay", "clay": "minecraft:block/clay", "stone": "minecraft:block/cobblestone", "soot": "mechanical:block/kiln_soot"}
for lit in (False, True):
    tex = "#lit_log" if lit else "#log"
    elements = [
        # foundation stones, open at the front
        stone([0.5, 0, 12.5], [15.5, 3, 15.5]), stone([0.5, 0, 3.5], [3.5, 3, 12.5]), stone([12.5, 0, 3.5], [15.5, 3, 12.5]),
        stone([0.5, 0, 0.5], [5, 3, 3.5]), stone([11, 0, 0.5], [15.5, 3, 3.5]),
        # clay walls, lower course
        kbox([1, 3, 13], [15, 10, 15]), kbox([1, 3, 3], [3, 10, 13]), kbox([13, 3, 3], [15, 10, 13]),
        kbox([1, 3, 1], [5, 10, 3]), kbox([11, 3, 1], [15, 10, 3]),
        # arch over the fire opening
        soot([5, 7, 1], [6, 8, 3]), soot([10, 7, 1], [11, 8, 3]), soot([5, 8, 1], [11, 10, 3]),
        # clay walls, upper course (slightly narrower)
        kbox([1.5, 10, 12.5], [14.5, 16, 14.5]), kbox([1.5, 10, 3.5], [3.5, 16, 12.5]), kbox([12.5, 10, 3.5], [14.5, 16, 12.5]),
        kbox([1.5, 10, 1.5], [14.5, 16, 3.5]),
        kbox([3.5, 14, 3.5], [12.5, 16, 12.5], ("down", "up")),          # chamber floor
        log_z([3.5, 0, 3.25], [7.5, 4, 12.75], tex), log_z([8.5, 0, 3.25], [12.5, 4, 12.75], tex),
        log_x([3.25, 3, 3.5], [12.75, 7, 7.5], tex), log_x([3.25, 3, 8.5], [12.75, 7, 12.5], tex),
    ]
    if lit:
        elements += [flame([3, 1, 8], [13, 13, 8], ("north", "south")), flame([8, 1, 3], [8, 13, 13], ("east", "west"))]
    w(A / f"models/block/kiln_lower{'_lit' if lit else ''}.json", {"parent": "minecraft:block/block", "textures": {
        **KILN_TEX, "log": "minecraft:block/campfire_log", "lit_log": "minecraft:block/campfire_log_lit", "fire": "minecraft:block/campfire_fire"},
        "elements": elements})
w(A / "models/block/kiln_upper.json", {"parent": "minecraft:block/block", "textures": KILN_TEX,
    "elements": [
        kbox([1.5, 0, 12.5], [14.5, 6, 14.5]), kbox([1.5, 0, 3.5], [3.5, 6, 12.5]), kbox([12.5, 0, 3.5], [14.5, 6, 12.5]),
        kbox([1.5, 0, 1.5], [6, 6, 3.5]), kbox([10, 0, 1.5], [14.5, 6, 3.5]),   # front around the loading opening
        kbox([6, 0, 1.5], [10, 1, 3.5]), soot([6, 5, 1.5], [10, 6, 3.5]),       # sill and sooty lintel
        kbox([3.5, 5, 3.5], [12.5, 6, 12.5], ("down", "up")),                   # chamber ceiling
        kbox([2.5, 6, 2.5], [13.5, 8, 13.5]), kbox([3.5, 8, 3.5], [12.5, 10, 12.5]), kbox([5, 10, 5], [11, 12, 11]),  # dome
        kbox([6.5, 12, 6.5], [9.5, 14.5, 9.5]), soot([6.5, 14.5, 6.5], [9.5, 16, 9.5]),  # chimney, blackened top
    ]})
rot = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for f, y in rot.items():
    for half in ("lower", "upper"):
        for lit in ("false", "true"):
            for active in ("false", "true"):
                model = "mechanical:block/kiln_upper" if half == "upper" else f"mechanical:block/kiln_lower{'_lit' if lit == 'true' else ''}"
                variants[f"active={active},facing={f},half={half},lit={lit}"] = {"model": model, **({"y": y} if y else {})}
w(A / "blockstates/kiln.json", {"variants": variants})
w(A / "items/kiln.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/kiln_upper"}})
STICKS = {"type": "minecraft:item", "name": "minecraft:stick",
    "functions": [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}
half_drop = lambda half, item: {"type": "minecraft:item", "name": item,
    "condition": {"type": "minecraft:match_block", "blocks": "mechanical:kiln", "state": {"half": half}}}
w(D / "mechanical/loot_table/blocks/kiln.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [half_drop("upper", "mechanical:kiln"), {**STICKS, "condition": half_drop("lower", "")["condition"]}]}],
    "random_sequence": "mechanical:blocks/kiln"})
w(D / "minecraft/tags/block/mineable/pickaxe.json", {"values": ["mechanical:tin_ore", "mechanical:tin_block", "mechanical:kiln"] + [f"mechanical:loose_{v}" for v in ROCKS]})

shaped("fire_drill", ["TS", "S "], {"T": "mechanical:twine", "S": "minecraft:stick"}, "mechanical:fire_drill")
shaped("unfired_crucible", ["C C", "C C", "CCC"], {"C": "minecraft:clay_ball"}, "mechanical:unfired_crucible")
shaped("kiln", ["CRC", "C C", "RRR"], {"C": "minecraft:clay_ball", "R": "#mechanical:rock_chunks"}, "mechanical:kiln")
# Campfire without coal (it is placed unlit and lit with the fire drill).
w(D / "minecraft/recipe/campfire.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "key": {"L": "#minecraft:logs", "S": "minecraft:stick"}, "pattern": [" S ", "S S", "LLL"], "result": {"id": "minecraft:campfire"}})

lang.update({
    "item.mechanical.fire_drill": "Fire Drill",
    "item.mechanical.unfired_crucible": "Unfired Crucible",
    "item.mechanical.crucible": "Crucible",
    "block.mechanical.kiln": "Clay Kiln",
    "container.mechanical.kiln": "Clay Kiln",
    "gui.mechanical.kiln.lit": "Burning",
    "gui.mechanical.kiln.unlit": "Not lit: add fuel and light it with a Fire Drill",
    "message.mechanical.kiln.needs_campfire": "The kiln must be placed on a campfire",
    "message.mechanical.kiln.needs_fuel": "Put fuel in the kiln first",
})
w(lang_path, lang)
print("kiln ok")

# Campfire falls apart into 1-3 sticks (vanilla drops charcoal, which would skip the kiln; itself would be too easy).
w(D / "minecraft/loot_table/blocks/campfire.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [STICKS]}],
    "random_sequence": "minecraft:blocks/campfire"})
print("campfire loot ok")

# ---- firewood: logs drop 2, four (2x2) craft back into that log; campfires are built from it ----
for wood in WOODS:
    fw = f"{wood}_firewood"
    for n in (1, 2, 3):
        w(A / f"models/item/{fw}_{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{fw}_{n}"}})
    model = lambda n: {"type": "minecraft:model", "model": f"mechanical:item/{fw}_{n}"}
    # 1 shows one piece, 2-15 two, 16+ three
    w(A / f"items/{fw}.json", {"model": {"type": "minecraft:range_dispatch", "property": "minecraft:count", "normalize": False,
        "entries": [{"threshold": 2, "model": model(2)}, {"threshold": 16, "model": model(3)}], "fallback": model(1)}})
    shaped(f"{wood}_log_from_firewood", ["FF", "FF"], {"F": f"mechanical:{fw}"}, f"{wns(wood)}:{wood}_log")
    lang[f"item.mechanical.{fw}"] = f"{title(wood)} Firewood"
w(D / "mechanical/tags/item/firewood.json", {"values": [f"mechanical:{x}_firewood" for x in WOODS]})
w(D / "minecraft/recipe/campfire.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "key": {"F": "#mechanical:firewood", "S": "minecraft:stick"}, "pattern": [" S ", "S S", "FFF"], "result": {"id": "minecraft:campfire"}})
w(lang_path, lang)
print("firewood ok")

# ---- hunting gear, fur, tallow, fletching table ----
def flat_item(item, parent="minecraft:item/generated"):
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
for item, name in [("tusk_dagger", "Tusk Dagger")]:
    flat_item(item, "minecraft:item/handheld")
    lang[f"item.mechanical.{item}"] = name
for item, name in [("bone_fish_hook", "Bone Fish Hook"), ("tallow", "Tallow"), ("bone_arrow", "Bone Arrow"),
                   ("barbed_arrow", "Barbed Arrow"), ("fire_arrow", "Fire Arrow"), ("fur_hood", "Fur Hood"), ("fur_coat", "Fur Coat"),
                   ("fur_leggings", "Fur Leggings"), ("fur_boots", "Fur Boots")]:
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
# bone hook rod: like the crude rod
for suffix in ["", "_cast"]:
    w(A / f"models/item/bone_hook_fishing_rod{suffix}.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": f"mechanical:item/bone_hook_fishing_rod{suffix}"}})
w(A / "items/bone_hook_fishing_rod.json", {"model": {"type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
    "on_false": {"type": "minecraft:model", "model": "mechanical:item/bone_hook_fishing_rod"},
    "on_true": {"type": "minecraft:model", "model": "mechanical:item/bone_hook_fishing_rod_cast"}}})
lang["item.mechanical.bone_hook_fishing_rod"] = "Bone Hook Fishing Rod"
# sinew bow: vanilla bow models with our textures
for suffix in ["", "_pulling_0", "_pulling_1", "_pulling_2"]:
    w(A / f"models/item/sinew_bow{suffix}.json", {"parent": "minecraft:item/bow", "textures": {"layer0": f"mechanical:item/sinew_bow{suffix}"}})
sbow = lambda x: {"type": "minecraft:model", "model": f"mechanical:item/sinew_bow{x}"}
w(A / "items/sinew_bow.json", {"model": {"type": "minecraft:condition", "property": "minecraft:using_item", "on_false": sbow(""),
    "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05, "fallback": sbow("_pulling_0"),
                "entries": [{"threshold": 0.65, "model": sbow("_pulling_1")}, {"threshold": 0.9, "model": sbow("_pulling_2")}]}}})
lang["item.mechanical.sinew_bow"] = "Sinew Bow"
lang["container.mechanical.fletching_table"] = "Fletching Table"
w(D / "minecraft/tags/item/enchantable/bow.json", {"values": ["mechanical:crude_bow", "mechanical:sinew_bow"]})
w(D / "minecraft/tags/item/enchantable/fishing.json", {"values": ["mechanical:crude_fishing_rod", "mechanical:bone_hook_fishing_rod"]})
w(D / "minecraft/tags/item/enchantable/durability.json", {"values": ["mechanical:crude_bow", "mechanical:crude_fishing_rod", "mechanical:sinew_bow",
    "mechanical:bone_hook_fishing_rod", "mechanical:fur_hood", "mechanical:fur_coat", "mechanical:fur_leggings", "mechanical:fur_boots"]})
w(D / "minecraft/tags/item/arrows.json", {"values": ["mechanical:bone_arrow", "mechanical:barbed_arrow", "mechanical:fire_arrow"]})
w(D / "mechanical/tags/item/knives.json", {"values": ["mechanical:flint_knife", "mechanical:iron_knife", "mechanical:tusk_dagger"]})
for tag, piece in [("head_armor", "fur_hood"), ("chest_armor", "fur_coat"), ("leg_armor", "fur_leggings"), ("foot_armor", "fur_boots")]:
    w(D / f"minecraft/tags/item/{tag}.json", {"values": [f"mechanical:{piece}"] + (["mechanical:antler_crown"] if tag == "head_armor" else [])})
w(D / "mechanical/tags/item/repairs_fur_armor.json", {"values": ["mechanical:bear_pelt"]})
# fur armour: the leather piece lined with bear pelt
w(A / "equipment/fur.json", {"layers": {"humanoid": [{"texture": "mechanical:fur"}], "humanoid_leggings": [{"texture": "mechanical:fur"}]}})
# (fur and leather armour are sewn at the Armor Station: ArmorStationRecipes.java)
# bear rug: flat, facing
w(A / "models/block/bear_rug.json", {"textures": {"particle": "mechanical:block/bear_rug", "top": "mechanical:block/bear_rug"}, "elements": [{
    "from": [0, 0, 0], "to": [16, 1, 16], "faces": {
        "up": {"texture": "#top", "uv": [0, 0, 16, 16]}, "down": {"texture": "#top", "uv": [0, 0, 16, 16], "cullface": "down"},
        "north": {"texture": "#top", "uv": [0, 0, 16, 1]}, "south": {"texture": "#top", "uv": [0, 15, 16, 16]},
        "east": {"texture": "#top", "uv": [15, 0, 16, 16]}, "west": {"texture": "#top", "uv": [0, 0, 1, 16]}}}]})
w(A / "blockstates/bear_rug.json", {"variants": {f"facing={f}": {"model": "mechanical:block/bear_rug", **({"y": y} if y else {})}
    for f, y in {"north": 0, "east": 90, "south": 180, "west": 270}.items()}})
flat_item("bear_rug")
w(D / "mechanical/loot_table/blocks/bear_rug.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "mechanical:bear_rug"}]}], "random_sequence": "mechanical:blocks/bear_rug"})
lang["block.mechanical.bear_rug"] = "Bear Rug"
shapeless("bear_rug", ["mechanical:bear_pelt", "mechanical:bear_claw"], "mechanical:bear_rug", category="building")
# bone fish hook; tallow rendered from fat over a campfire; candles from tallow
shapeless("bone_fish_hook", ["mechanical:fish_bones", "#mechanical:knives"], "mechanical:bone_fish_hook", category="equipment")
for fat, n in (("lard", 1), ("bear_fat", 2)):
    w(D / f"mechanical/recipe/tallow_from_{fat}.json", {"type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 400,
        "experience": 0.1, "ingredient": f"mechanical:{fat}", "result": {"id": "mechanical:tallow", "count": n}})
w(D / "mechanical/recipe/candle_from_tallow.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["T", "S", "S"],
    "key": {"T": "mechanical:twine", "S": "mechanical:tallow"}, "result": {"id": "minecraft:candle", "count": 2}})
# boiled eggs: any egg on a campfire; peeled in the grid (the shell comes back via StoneAgeEvents)
w(D / "mechanical/recipe/boiled_egg.json", {"type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 300,
    "experience": 0.1, "ingredient": "#minecraft:eggs", "result": {"id": "mechanical:boiled_egg"}})
shapeless("peeled_egg", ["mechanical:boiled_egg"], "mechanical:peeled_egg")
for item, name in [("boiled_egg", "Boiled Egg"), ("peeled_egg", "Peeled Egg"), ("eggshell", "Eggshell")]:
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name
# ---- armor station + bone needle ----
w(A / "blockstates/armor_station.json", {"variants": {"": {"model": "mechanical:block/armor_station"}}})
w(A / "models/block/armor_station.json", {"parent": "minecraft:block/cube", "textures": {
    "particle": "mechanical:block/armor_station_front", "up": "mechanical:block/armor_station_top", "down": "minecraft:block/oak_planks",
    "north": "mechanical:block/armor_station_front", "south": "mechanical:block/armor_station_front",
    "east": "mechanical:block/armor_station_side", "west": "mechanical:block/armor_station_side"}})
w(A / "items/armor_station.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/armor_station"}})
w(D / "mechanical/loot_table/blocks/armor_station.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "mechanical:armor_station"}]}],
    "random_sequence": "mechanical:blocks/armor_station"})
lang["block.mechanical.armor_station"] = "Armor Station"
lang["container.mechanical.armor_station"] = "Armor Station"
w(D / "mechanical/recipe/armor_station.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["LNL", "PCP"],
    "key": {"L": "minecraft:leather", "N": "mechanical:bone_needle", "P": "#minecraft:planks", "C": "minecraft:crafting_table"},
    "result": {"id": "mechanical:armor_station"}})
flat_item("bone_needle")
lang["item.mechanical.bone_needle"] = "Bone Needle"
for src in ("fish_bones", "bone", "horn"):
    ns = "minecraft" if src == "bone" else "mechanical"
    shapeless(f"bone_needle_from_{src}", [f"{ns}:{src}", "#mechanical:knives"], "mechanical:bone_needle", category="equipment")
# leather armour: sewn at the armor station, not crafted
for name in ("leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots"):
    w(D / f"minecraft/recipe/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
    w(D / f"minecraft/advancement/recipes/combat/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
# rock chunks back into cobblestone
shaped("cobblestone_from_chunks", ["RR", "RR"], {"R": "#mechanical:rock_chunks"}, "minecraft:cobblestone")
# Removed: arrows (fletching table now), honeycomb candles (tallow now), smoker and blast furnace (later ages)
for cat, name in (("combat", "arrow"), ("decorations", "candle"), ("decorations", "smoker"), ("decorations", "blast_furnace")):
    w(D / f"minecraft/recipe/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
    w(D / f"minecraft/advancement/recipes/{cat}/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
# stone breaks into rock chunks (silk touch keeps the block)
for rock in ("stone", "granite", "diorite", "andesite"):
    w(D / f"minecraft/loot_table/blocks/{rock}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": f"minecraft:{rock}"},
        {"type": "minecraft:item", "name": f"mechanical:{rock}_chunk", "condition": {"type": "minecraft:survives_explosion"},
         "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 2, "max": 4}}]}]}]}],
        "random_sequence": f"minecraft:blocks/{rock}"})

# ---- recipe book unlocks: picking up an item reveals the recipes it leads to ----
UNLOCKS = {
    "flint": ("minecraft:flint", ["flint_pickaxe", "flint_axe", "flint_shovel", "flint_hoe", "flint_knife", "tool_station"]),
    "plant_fibre": ("mechanical:plant_fibre", ["twine"]),
    "twine": ("mechanical:twine", ["cordage", "fire_drill"]),
    "whole_fish": ("#mechanical:whole_fish", ["gut_cod", "gut_salmon", "gut_tropical_fish", "gut_pufferfish"]),
    "fish_bones": ("mechanical:fish_bones", ["bone_meal_from_fish_bones", "bone_needle_from_fish_bones", "bone_fish_hook"]),
    "bark": ("mechanical:bark", ["plant_fibre_from_bark", "bark_basket"]),
    "bear_pelt": ("mechanical:bear_pelt", ["bear_rug"]),
    "leather": ("minecraft:leather", ["armor_station"]),
    "bone": ("minecraft:bone", ["bone_needle_from_bone"]),
    "horn": ("mechanical:horn", ["bone_needle_from_horn"]),
    "tallow": ("mechanical:tallow", ["candle_from_tallow"]),
    "boiled_egg": ("mechanical:boiled_egg", ["peeled_egg"]),
    "rock_chunks_cobble": ("#mechanical:rock_chunks", ["cobblestone_from_chunks"]),
    "firewood": ("#mechanical:firewood", ["tool_rod", "minecraft:campfire"] + [f"{x}_log_from_firewood" for x in WOODS]),
    "rock_chunks": ("#mechanical:rock_chunks", ["stone_pickaxe_head", "stone_axe_head", "kiln"]),
    "clay_ball": ("minecraft:clay_ball", ["kiln", "unfired_crucible"]),
    "stone_pickaxe_head": ("mechanical:stone_pickaxe_head", ["double_stone_pickaxe_head"]),
    "stone_axe_head": ("mechanical:stone_axe_head", ["double_stone_axe_head"]),
}
for name, (item, recipes) in UNLOCKS.items():
    w(D / f"mechanical/advancement/recipes/{name}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]],
        "rewards": {"recipes": [r if ":" in r else f"mechanical:{r}" for r in recipes]},
    })
print("unlocks ok")

# ---- flint spear, armor, beds, bow ----
# gui/ground: flat item; in hand: vanilla's spear display transforms (held like a spear, braced when charging)
w(A / "models/item/flint_spear.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/flint_spear"}})
w(A / "models/item/flint_spear_in_hand.json", {"parent": "minecraft:item/spear_in_hand", "textures": {"layer0": "mechanical:item/flint_spear_in_hand"}})
w(A / "items/flint_spear.json", {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
    "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": "mechanical:item/flint_spear"}}],
    "fallback": {"type": "minecraft:model", "model": "mechanical:item/flint_spear_in_hand"}}, "swap_animation_scale": 1.95})
w(D / "minecraft/tags/item/spears.json", {"values": ["mechanical:flint_spear"]})
lang["item.mechanical.flint_spear"] = "Flint Spear"

# Disabled until later: every armor except leather (an Armor Station comes later), beds (need shears later), stone spear.
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]
LATER = {"combat": [f"{m}_{p}" for m in ["iron", "golden", "diamond", "copper"] for p in ["helmet", "chestplate", "leggings", "boots"]]
                   + ["turtle_helmet", "stone_spear"],
         "decorations": [f"{c}_bed" for c in COLORS] + [f"dye_{c}_bed" for c in COLORS] + ["straw_bed"]}
for cat, names in LATER.items():
    for name in names:
        w(D / f"minecraft/recipe/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
        w(D / f"minecraft/advancement/recipes/{cat}/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
# Bow and fishing rod: no crafting recipe; the stone age has crude ones from the Tool Station (real ones come later).
for cat, name in (("combat", "bow"), ("tools", "fishing_rod")):
    w(D / f"minecraft/recipe/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
    w(D / f"minecraft/advancement/recipes/{cat}/{name}.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
# crude bow: vanilla bow models (display transforms) with our textures
for suffix in ["", "_pulling_0", "_pulling_1", "_pulling_2"]:
    w(A / f"models/item/crude_bow{suffix}.json", {"parent": "minecraft:item/bow", "textures": {"layer0": f"mechanical:item/crude_bow{suffix}"}})
bow_model = lambda s: {"type": "minecraft:model", "model": f"mechanical:item/crude_bow{s}"}
w(A / "items/crude_bow.json", {"model": {"type": "minecraft:condition", "property": "minecraft:using_item", "on_false": bow_model(""),
    "on_true": {"type": "minecraft:range_dispatch", "property": "minecraft:use_duration", "scale": 0.05, "fallback": bow_model("_pulling_0"),
                "entries": [{"threshold": 0.65, "model": bow_model("_pulling_1")}, {"threshold": 0.9, "model": bow_model("_pulling_2")}]}}})
for suffix in ["", "_cast"]:
    w(A / f"models/item/crude_fishing_rod{suffix}.json", {"parent": "minecraft:item/handheld_rod", "textures": {"layer0": f"mechanical:item/crude_fishing_rod{suffix}"}})
w(A / "items/crude_fishing_rod.json", {"model": {"type": "minecraft:condition", "property": "minecraft:fishing_rod/cast",
    "on_false": {"type": "minecraft:model", "model": "mechanical:item/crude_fishing_rod"},
    "on_true": {"type": "minecraft:model", "model": "mechanical:item/crude_fishing_rod_cast"}}})
lang.update({"item.mechanical.crude_bow": "Crude Bow", "item.mechanical.crude_fishing_rod": "Crude Fishing Rod"})
w(D / "minecraft/tags/item/enchantable/bow.json", {"values": ["mechanical:crude_bow"]})
w(D / "minecraft/tags/item/enchantable/fishing.json", {"values": ["mechanical:crude_fishing_rod"]})
w(D / "minecraft/tags/item/enchantable/durability.json", {"values": ["mechanical:crude_bow", "mechanical:crude_fishing_rod"]})

# ---- fish: dead fish (and catches) are whole fish that must be gutted with the flint knife -> fillet (vanilla raw fish) ----
import zipfile
JAR = Path(__file__).resolve().parent.parent / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
FISH = {"cod": "cod", "salmon": "salmon", "tropical_fish": "tropical_fish", "pufferfish": "pufferfish"}
with zipfile.ZipFile(JAR) as jar:
    def whole_fish(path):
        text = jar.read(f"data/minecraft/loot_table/{path}.json").decode()
        for f in FISH:
            text = text.replace(f'"minecraft:{f}"', f'"mechanical:whole_{f}"')
        w(D / f"minecraft/loot_table/{path}.json", json.loads(text))
    whole_fish("gameplay/fishing/fish")
    for f in FISH:
        whole_fish(f"entities/{f}")
for f in FISH:
    item = f"whole_{f}"
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    lang[f"item.mechanical.{item}"] = title(item)
    shapeless(f"gut_{f}", [f"mechanical:{item}", "#mechanical:knives"], f"minecraft:{f}", category="misc")
w(A / "models/item/fish_bones.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/fish_bones"}})
w(A / "items/fish_bones.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/fish_bones"}})
lang["item.mechanical.fish_bones"] = "Fish Bones"
shapeless("bone_meal_from_fish_bones", ["mechanical:fish_bones"], "minecraft:bone_meal")
w(D / "mechanical/tags/item/whole_fish.json", {"values": [f"mechanical:whole_{f}" for f in FISH]})
w(lang_path, lang)
print("armor/beds/bow ok")

# ---- food: meat scraps, hides, wool thread, drying rack ----
MEATS = ["beef", "porkchop", "mutton", "chicken", "rabbit", "game_meat"]
for m in MEATS:
    for item in (f"{m}_scraps", f"cooked_{m}_scraps"):
        w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
        w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
        lang[f"item.mechanical.{item}"] = title(item).replace("Porkchop", "Pork")
    # only campfire (and the smoker, later) cook food: there is no furnace
    for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
        w(D / f"mechanical/recipe/cooked_{m}_scraps_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
            "cookingtime": ticks, "experience": 0.1, "ingredient": f"mechanical:{m}_scraps", "result": {"id": f"mechanical:cooked_{m}_scraps"}})
for item in ("raw_hide", "scraped_hide", "wool_thread"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    lang[f"item.mechanical.{item}"] = title(item)
shapeless("scraped_hide", ["mechanical:raw_hide", "#mechanical:knives"], "mechanical:scraped_hide")
shaped("white_wool_from_thread", ["TTT", "TTT", "TTT"], {"T": "mechanical:wool_thread"}, "minecraft:white_wool")
shaped("drying_rack", ["STS", "S S"], {"S": "minecraft:stick", "T": "mechanical:twine"}, "mechanical:drying_rack")

# Drying rack (built facing north): two posts, top and bottom bars, a hide stretched in the middle when loaded.
def rack_box(f, t):
    return {"from": f, "to": t, "faces": {d: {"texture": "#wood"} for d in ["down", "up", "north", "south", "east", "west"]}}
frame = [rack_box([1, 0, 7], [3, 16, 9]), rack_box([13, 0, 7], [15, 16, 9]), rack_box([3, 13, 7.25], [13, 15, 8.75]), rack_box([3, 2, 7.25], [13, 4, 8.75])]
hide_plane = {"from": [3, 4, 8], "to": [13, 13, 8], "faces": {"north": {"uv": [3, 3, 13, 12], "texture": "#hide"}, "south": {"uv": [3, 3, 13, 12], "texture": "#hide"}}}
for stage, tex in ((0, None), (1, "drying_rack_hide_wet"), (2, "drying_rack_hide_dry")):
    textures = {"particle": "mechanical:block/drying_rack", "wood": "mechanical:block/drying_rack"}
    if tex: textures["hide"] = f"mechanical:block/{tex}"
    w(A / f"models/block/drying_rack_{stage}.json", {"parent": "minecraft:block/block", "textures": textures,
        "elements": frame + ([hide_plane] if tex else [])})
w(A / "blockstates/drying_rack.json", {"variants": {f"facing={f},hide={h}": {"model": f"mechanical:block/drying_rack_{h}", **({"y": y} if y else {})}
                                                    for f, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)] for h in (0, 1, 2)}})
w(A / "items/drying_rack.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/drying_rack_0"}})
hide_drop = lambda h, item: {"type": "minecraft:item", "name": item,
    "condition": {"type": "minecraft:match_block", "blocks": "mechanical:drying_rack", "state": {"hide": str(h)}}}
w(D / "mechanical/loot_table/blocks/drying_rack.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "mechanical:drying_rack"}]},
    {"rolls": 1, "entries": [hide_drop(1, "mechanical:scraped_hide"), hide_drop(2, "minecraft:leather")]}],
    "random_sequence": "mechanical:blocks/drying_rack"})
w(D / "minecraft/tags/block/mineable/axe.json", {"values": ["mechanical:tool_station", "mechanical:drying_rack"] + [f"mechanical:{x}_stump" for x in WOODS]
    + [f"mechanical:{x}_fallen_log" for x in WOODS]})
for n, it in (("raw_hide", ("mechanical:raw_hide", ["scraped_hide", "drying_rack"])), ("wool_thread", ("mechanical:wool_thread", ["white_wool_from_thread"]))):
    item, recipes = it
    w(D / f"mechanical/advancement/recipes/{n}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})
lang.update({"block.mechanical.drying_rack": "Drying Rack", "entity.mechanical.carcass": "Carcass"})

# Tool Station: its own textures
w(A / "models/block/tool_station.json", {"parent": "minecraft:block/cube", "textures": {
    "particle": "mechanical:block/tool_station_side", "up": "mechanical:block/tool_station_top", "down": "minecraft:block/oak_planks",
    "north": "mechanical:block/tool_station_front", "south": "mechanical:block/tool_station_front",
    "east": "mechanical:block/tool_station_side", "west": "mechanical:block/tool_station_side"}})
w(lang_path, lang)
print("food ok")

# ---- bone pile: left where an animal was fully butchered; never despawns ----
# the bone pile model and its textures are hand-made (scripts/gen_bone_pile_textures.py); not generated here
w(A / "blockstates/bone_pile.json", {"variants": {"": [{"model": "mechanical:block/bone_pile", "y": y} for y in (0, 90, 180, 270)]}})
w(D / "mechanical/loot_table/blocks/bone_pile.json", {"type": "minecraft:block", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone", "condition": {"type": "minecraft:random_chance", "chance": 0.15}}]},
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:bone_meal", "condition": {"type": "minecraft:random_chance", "chance": 0.25},
        "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}],
    "random_sequence": "mechanical:blocks/bone_pile"})
lang["block.mechanical.bone_pile"] = "Bone Pile"
w(lang_path, lang)
print("bone pile ok")

# ---- blood trail decal ----
for v in range(3):
    w(A / f"models/block/blood_{v}.json", {"textures": {"particle": f"mechanical:block/blood_{v}", "blood": f"mechanical:block/blood_{v}"},
        "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "shade": False,
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#blood"}, "down": {"uv": [0, 16, 16, 0], "texture": "#blood"}}}]})
for lvl in (2, 3, 4):  # bigger pools when blood lands on blood
    w(A / f"models/block/blood_pool_{lvl}.json", {"textures": {"particle": f"mechanical:block/blood_pool_{lvl}", "blood": f"mechanical:block/blood_pool_{lvl}"},
        "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "shade": False,
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#blood"}, "down": {"uv": [0, 16, 16, 0], "texture": "#blood"}}}]})
w(A / "blockstates/blood.json", {"variants": {
    "amount=1": [{"model": f"mechanical:block/blood_{v}", "y": y} for v in range(3) for y in (0, 90, 180, 270)],
    **{f"amount={lvl}": [{"model": f"mechanical:block/blood_pool_{lvl}", "y": y} for y in (0, 90, 180, 270)] for lvl in (2, 3, 4)}}})
lang["block.mechanical.blood"] = "Blood"
w(lang_path, lang)
print("blood ok")

# ---- campfire embers + burnt food ----
# vanilla campfire gets an "embers" state (mixin): glowing logs, no flame (model: campfire_embers, made from vanilla's template)
CAMPFIRE_Y = {"north": 180, "east": 270, "south": 0, "west": 90}
w(Path(sys.argv[1]) / "assets/minecraft/blockstates/campfire.json", {"variants": {
    **{f"facing={f},lit=true": {"model": "minecraft:block/campfire", **({"y": y} if y else {})} for f, y in CAMPFIRE_Y.items()},
    **{f"embers=false,facing={f},lit=false": {"model": "minecraft:block/campfire_off", **({"y": y} if y else {})} for f, y in CAMPFIRE_Y.items()},
    **{f"embers=true,facing={f},lit=false": {"model": "mechanical:block/campfire_embers", **({"y": y} if y else {})} for f, y in CAMPFIRE_Y.items()},
}})
for item in ["burnt_food"]:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
lang["item.mechanical.burnt_food"] = "Burnt Food"

# ---- forest wildlife ----
WILD = ["roe_deer", "red_deer", "wild_boar", "hare", "pheasant"]
for item in ["venison_scraps", "cooked_venison_scraps", "sinew", "antler", "lard", "tusk"] + [f"{x}_spawn_egg" for x in WILD]:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    lang[f"item.mechanical.{item}"] = title(item)
for kind, ticks in (("campfire_cooking", 400), ("smoking", 100)):
    w(D / f"mechanical/recipe/cooked_venison_scraps_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "food",
        "cookingtime": ticks, "experience": 0.1, "ingredient": "mechanical:venison_scraps", "result": {"id": "mechanical:cooked_venison_scraps"}})
for x in WILD:
    lang[f"entity.mechanical.{x}"] = title(x)
# spawn in forests: weight, group size
SPAWNS = {"roe_deer": (10, 3, 5), "red_deer": (6, 2, 4), "wild_boar": (6, 2, 4), "hare": (8, 1, 2), "pheasant": (8, 1, 3)}
w(D / "mechanical/neoforge/biome_modifier/add_forest_wildlife.json", {"type": "neoforge:add_spawns", "biomes": "#minecraft:is_forest",
    "spawners": [{"type": f"mechanical:{x}", "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}
                 for x, (wt, lo, hi) in SPAWNS.items()]})
# ---- open-country and cold wildlife ----
MORE_WILD = ["aurochs", "wild_horse", "mouflon", "moose", "reindeer", "brown_bear"]
for item in ["horn", "bear_pelt", "bear_fat", "bear_claw"] + [f"{x}_spawn_egg" for x in MORE_WILD]:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    lang[f"item.mechanical.{item}"] = title(item)
for x in MORE_WILD:
    lang[f"entity.mechanical.{x}"] = title(x)
MORE_SPAWNS = {  # (weight, min, max), biomes
    "aurochs": ((8, 3, 6), ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna"]),
    "wild_horse": ((6, 3, 6), ["minecraft:plains", "minecraft:savanna"]),
    "mouflon": ((8, 2, 5), ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_savanna",
                            "minecraft:savanna_plateau", "minecraft:meadow", "minecraft:stony_peaks"]),
    "moose": ((5, 1, 3), ["minecraft:taiga", "minecraft:old_growth_pine_taiga", "minecraft:old_growth_spruce_taiga", "minecraft:swamp"]),
    "reindeer": ((8, 4, 8), ["minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:snowy_slopes", "minecraft:ice_spikes"]),
}
# bears: forests and all taigas, rare, alone or a mother with cubs
w(D / "mechanical/neoforge/biome_modifier/add_brown_bears.json", {"type": "neoforge:add_spawns",
    "biomes": {"type": "neoforge:or", "values": ["#minecraft:is_forest", "#minecraft:is_taiga"]},
    "spawners": [{"type": "mechanical:brown_bear", "weight": 2, "count": {"type": "minecraft:uniform", "min_inclusive": 1, "max_inclusive": 3}}]})
for x, ((wt, lo, hi), biomes) in MORE_SPAWNS.items():
    w(D / f"mechanical/neoforge/biome_modifier/add_{x}.json", {"type": "neoforge:add_spawns", "biomes": biomes,
        "spawners": [{"type": f"mechanical:{x}", "weight": wt, "count": {"type": "minecraft:uniform", "min_inclusive": lo, "max_inclusive": hi}}]})

# Farm animals no longer spawn in the wild; they live in villages (VillageAnimals.java). Goats, rabbits, mooshrooms stay.
w(D / "mechanical/neoforge/biome_modifier/remove_farm_animals.json", {"type": "neoforge:remove_spawns", "biomes": "#minecraft:is_overworld",
    "entity_types": [f"minecraft:{x}" for x in ["cow", "sheep", "pig", "chicken", "horse", "donkey", "llama"]]})
# Iron Knife: iron age, no recipe yet; needed for the bear. Every knife recipe takes any knife.
w(D / "mechanical/tags/item/knives.json", {"values": ["mechanical:flint_knife", "mechanical:iron_knife"]})
w(A / "models/item/iron_knife.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "mechanical:item/iron_knife"}})
w(A / "items/iron_knife.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/iron_knife"}})
lang["item.mechanical.iron_knife"] = "Iron Knife"
lang["message.mechanical.knife_too_weak"] = "This hide is too tough for your knife"
w(lang_path, lang)
print("wildlife data ok")

# ---- names: the rock chunks are just rocks ----
lang.update({"item.mechanical.stone_chunk": "Rock", "item.mechanical.granite_chunk": "Granite Rock",
             "item.mechanical.diorite_chunk": "Diorite Rock", "item.mechanical.andesite_chunk": "Andesite Rock"})
w(lang_path, lang)


# ---- villages: stone age nerf (trades, chests, dying fields, no golem iron) ----
import zipfile as _zf, json as _json
_jar = _zf.ZipFile(JAR)
def _vanilla(path):
    return _json.loads(_jar.read(path).decode())

# Trades: each profession's level 1-2 get stone age trades; levels 3-5 are empty until later ages unlock them.
# (wants, wants_count, gives, gives_count, max_uses, xp)
M = "mechanical:"
E = "minecraft:emerald"
TRADES = {
    "armorer":       [[("minecraft:leather", 10, E, 1, 16, 2), (E, 3, "minecraft:leather_helmet", 1, 8, 1)],
                      [(M + "sinew", 6, E, 1, 16, 10), (E, 5, "minecraft:leather_chestplate", 1, 6, 5)]],
    "butcher":       [[(M + "game_meat_scraps", 10, E, 1, 16, 2), (E, 1, M + "cooked_venison_scraps", 4, 16, 1)],
                      [(M + "lard", 6, E, 1, 16, 10), (E, 1, M + "cooked_beef_scraps", 4, 16, 5)]],
    "cartographer":  [[("minecraft:paper", 24, E, 1, 16, 2), (E, 3, "minecraft:map", 1, 8, 1)],
                      [(M + "bark", 16, E, 1, 16, 10), (E, 1, M + "cordage", 6, 12, 5)]],
    "cleric":        [[("minecraft:rotten_flesh", 24, E, 1, 16, 2), (E, 1, "minecraft:candle", 3, 12, 1)],
                      [(M + "eggshell", 8, E, 1, 16, 10), (E, 1, "minecraft:bone_meal", 6, 12, 5)]],
    "farmer":        [[(M + "raspberries", 16, E, 1, 16, 2), (E, 1, "minecraft:wheat_seeds", 8, 12, 1)],
                      [(M + "blueberries", 16, E, 1, 16, 10), (E, 2, "minecraft:carrot", 3, 8, 5)]],
    "fisherman":     [[(M + "whole_cod", 6, E, 1, 16, 2), (E, 1, M + "bone_fish_hook", 1, 12, 1)],
                      [(M + "whole_salmon", 5, E, 1, 16, 10), (E, 3, M + "crude_fishing_rod", 1, 6, 5)]],
    "fletcher":      [[("minecraft:stick", 32, E, 1, 16, 2), (E, 1, "minecraft:arrow", 8, 12, 1)],
                      [("minecraft:flint", 24, E, 1, 16, 10), (E, 3, M + "crude_bow", 1, 6, 5)]],
    "leatherworker": [[(M + "raw_hide", 6, E, 1, 16, 2), (E, 1, "minecraft:leather", 2, 12, 1)],
                      [("minecraft:rabbit_hide", 6, E, 1, 16, 10), (E, 3, M + "bark_basket", 1, 6, 5)]],
    "librarian":     [[("minecraft:paper", 24, E, 1, 16, 2), (E, 2, "minecraft:book", 1, 12, 1)],
                      [("minecraft:ink_sac", 6, E, 1, 16, 10), (E, 1, "minecraft:candle", 3, 12, 5)]],
    "mason":         [[("minecraft:clay_ball", 10, E, 1, 16, 2), (E, 1, M + "stone_chunk", 12, 16, 1)],
                      [(M + "granite_chunk", 20, E, 1, 16, 10), (E, 1, "minecraft:packed_mud", 8, 12, 5)]],
    "shepherd":      [[("minecraft:white_wool", 18, E, 1, 16, 2), (E, 1, M + "wool_thread", 6, 12, 1)],
                      [(M + "plant_fibre", 24, E, 1, 16, 10), (E, 1, "minecraft:white_carpet", 4, 12, 5)]],
    "toolsmith":     [[("minecraft:flint", 16, E, 1, 16, 2), (E, 1, M + "flint_axe", 1, 12, 1)],
                      [(M + "stone_chunk", 24, E, 1, 16, 10), (E, 3, M + "crude_stone_axe", 1, 6, 5)]],
    "weaponsmith":   [[(M + "cordage", 12, E, 1, 16, 2), (E, 2, M + "flint_spear", 1, 12, 1)],
                      [(M + "tusk", 4, E, 1, 16, 10), (E, 4, M + "tusk_dagger", 1, 6, 5)]],
}
def _stack(i, n):
    return {"id": i, "count": n} if n > 1 else {"id": i}
for prof, levels in TRADES.items():
    for lvl in range(1, 6):
        ids = []
        for i, (wi, wn, gi, gn, uses, xp) in enumerate(levels[lvl - 1] if lvl <= len(levels) else []):
            name = f"{prof}/{lvl}/stone_{i}"
            w(D / f"mechanical/villager_trade/{name}.json", {"wants": _stack(wi, wn), "gives": _stack(gi, gn),
                "max_uses": uses, "xp": xp, "reputation_discount": 0.05})
            ids.append(f"mechanical:{name}")
        w(D / f"minecraft/tags/villager_trade/{prof}/level_{lvl}.json", {"replace": True, "values": ids})

# wandering trader: drop anything made of metal (iron pickaxe, buckets, golden dandelion)
for tag in ("common", "uncommon", "buying"):
    vals = _vanilla(f"data/minecraft/tags/villager_trade/wandering_trader/{tag}.json")["values"]
    w(D / f"minecraft/tags/villager_trade/wandering_trader/{tag}.json", {"replace": True,
        "values": [v for v in vals if not any(x in v for x in ("iron", "diamond", "bucket", "gold"))]})

# Chests: primitive loot only
def _e(item, lo, hi, weight):
    return {"type": "minecraft:item", "name": item, "weight": weight,
            "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}
BASE = [_e("minecraft:stick", 2, 6, 10), _e("minecraft:flint", 1, 4, 8), _e(M + "plant_fibre", 2, 6, 8),
        _e(M + "cordage", 1, 3, 5), _e(M + "bark", 1, 3, 5), _e("minecraft:wheat_seeds", 1, 4, 6),
        _e(M + "twine", 1, 3, 4), _e("minecraft:bone", 1, 3, 4)]
EXTRA = {
    "weaponsmith": [_e(M + "flint_spear", 1, 1, 2), _e("minecraft:arrow", 2, 6, 4), _e(M + "flint_knife", 1, 1, 2)],
    "toolsmith": [_e(M + "flint_axe", 1, 1, 2), _e(M + "stone_axe_head", 1, 1, 3), _e(M + "tool_rod", 1, 2, 4)],
    "armorer": [_e("minecraft:leather", 1, 3, 4), _e(M + "wool_thread", 1, 3, 4)],
    "tannery": [_e("minecraft:leather", 1, 3, 5), _e(M + "raw_hide", 1, 2, 5), _e(M + "scraped_hide", 1, 1, 3)],
    "butcher": [_e(M + "cooked_beef_scraps", 1, 3, 5), _e(M + "lard", 1, 2, 3), _e(M + "sinew", 1, 2, 3)],
    "fisher": [_e(M + "whole_cod", 1, 2, 5), _e(M + "fish_bones", 1, 3, 4), _e(M + "bone_fish_hook", 1, 1, 2)],
    "fletcher": [_e("minecraft:feather", 2, 6, 6), _e("minecraft:arrow", 2, 6, 4)],
    "shepherd": [_e("minecraft:white_wool", 1, 3, 5), _e(M + "wool_thread", 1, 4, 5)],
    "mason": [_e("minecraft:clay_ball", 2, 6, 6), _e(M + "stone_chunk", 2, 6, 6)],
    "cartographer": [_e("minecraft:paper", 1, 4, 5)],
    "temple": [_e("minecraft:candle", 1, 2, 4), _e(M + "tallow", 1, 2, 4), _e("minecraft:bone_meal", 1, 3, 3)],
}
HOUSE = [_e(M + "raspberries", 1, 4, 4), _e(M + "blueberries", 1, 4, 4), _e(M + "cooked_mutton_scraps", 1, 3, 4),
         _e("minecraft:carrot", 1, 2, 2), _e("minecraft:potato", 1, 2, 2)]
for path in [n for n in _jar.namelist() if n.startswith("data/minecraft/loot_table/chests/village/") and n.endswith(".json")]:
    name = path.rsplit("/", 1)[1][:-5]
    kind = name.removeprefix("village_")
    pool = BASE + EXTRA.get(kind, HOUSE)
    w(D / f"minecraft/loot_table/chests/village/{name}.json", {"type": "minecraft:chest", "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 3, "max": 6}, "entries": pool}],
        "random_sequence": f"minecraft:chests/village/{name}"})

# Fields: most crops died, a few young ones left (young crops only give seeds / one carrot or potato)
def _rule(block, prob, out):
    return {"input_predicate": {"predicate_type": "minecraft:random_block_match", "block": block, "probability": prob},
            "location_predicate": {"predicate_type": "minecraft:always_true"}, "output_state": out}
DEAD = {"id": "minecraft:air"}
for farm in ("plains", "desert", "savanna", "snowy", "taiga"):
    rules = []
    for crop in ("wheat", "carrots", "potatoes", "beetroots", "melon_stem", "pumpkin_stem"):
        rules.append(_rule(f"minecraft:{crop}", 0.75, DEAD))
    rules += [_rule("minecraft:wheat", 0.2, {"id": "minecraft:carrots", "properties": {"age": "1"}}),
              _rule("minecraft:wheat", 0.15, {"id": "minecraft:potatoes", "properties": {"age": "1"}}),
              _rule("minecraft:wheat", 1.0, {"id": "minecraft:wheat", "properties": {"age": "2"}})]
    for crop in ("carrots", "potatoes", "beetroots"):
        rules.append(_rule(f"minecraft:{crop}", 1.0, {"id": f"minecraft:{crop}", "properties": {"age": "1"}}))
    for stem in ("melon_stem", "pumpkin_stem"):
        rules.append(_rule(f"minecraft:{stem}", 1.0, {"id": f"minecraft:{stem}", "properties": {"age": "1"}}))
    rules.append(_rule("minecraft:farmland", 0.6, {"id": "minecraft:farmland", "properties": {"moisture": "0"}}))
    w(D / f"minecraft/worldgen/processor_list/farm_{farm}.json", {"processors": [{"processor_type": "minecraft:rule", "rules": rules}]})

# Iron golem: no iron
w(D / "minecraft/loot_table/entities/iron_golem.json", {"type": "minecraft:entity", "pools": [
    {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "minecraft:poppy",
        "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 2}}]}]}],
    "random_sequence": "minecraft:entities/iron_golem"})


# ---- twigs: small sticks on the forest floor, break for a stick ----
for v in range(3):
    w(A / f"models/block/twigs_{v}.json", {"textures": {"particle": f"mechanical:block/twigs_{v}", "twigs": f"mechanical:block/twigs_{v}"},
        "elements": [{"from": [0, 0.1, 0], "to": [16, 0.1, 16], "shade": False,
                      "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#twigs"}, "down": {"uv": [0, 16, 16, 0], "texture": "#twigs"}}}]})
w(A / "blockstates/twigs.json", {"variants": {"": [{"model": f"mechanical:block/twigs_{v}", "y": y} for v in range(3) for y in (0, 90, 180, 270)]}})
w(D / "mechanical/loot_table/blocks/twigs.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:item", "name": "minecraft:stick"}]}], "random_sequence": "mechanical:blocks/twigs"})
lang["block.mechanical.twigs"] = "Twigs"
w(lang_path, lang)
forage_feature("twigs", {"id": "mechanical:twigs"}, {"type": "neoforge:or", "values": ["#minecraft:is_forest", "#minecraft:is_taiga"]}, 1, 3, 7)

# ---- bigger trees: oaks are big branching trees, birches are tall and slim ----
_tjar = zipfile.ZipFile(JAR)
for path in _tjar.namelist():
    if not path.startswith("data/minecraft/worldgen/feature/") or not path.endswith(".json"):
        continue
    name = path.rsplit("/", 1)[1][:-5]
    if name.startswith(("fallen_", "swamp_", "dark_oak", "pale_oak", "trees_")):
        continue
    tree = json.loads(_tjar.read(path).decode())
    if tree.get("type") != "minecraft:tree":
        continue
    log = tree["trunk_provider"].get("id")
    if log == "minecraft:oak_log":
        tree["trunk_placer"] = {"type": "minecraft:fancy_trunk_placer", "base_height": 9, "height_rand_a": 7, "height_rand_b": 0}
        tree["foliage_placer"] = {"type": "minecraft:fancy_foliage_placer", "height": 4, "offset": 4, "radius": 2}
        tree["minimum_size"] = {"type": "minecraft:two_layers_feature_size", "limit": 0, "min_clipped_height": 6, "upper_size": 0}
    elif log == "minecraft:birch_log":
        tp = tree["trunk_placer"]
        tree["trunk_placer"] = {"type": "minecraft:straight_trunk_placer", "base_height": 9, "height_rand_a": 3,
                                "height_rand_b": tp.get("height_rand_b", 0)}
        tree["foliage_placer"] = {"type": "minecraft:blob_foliage_placer", "height": 4, "offset": 0, "radius": 2}
    else:
        continue
    w(D / f"minecraft/worldgen/feature/{name}.json", tree)
print("forest ok")

# ---- our tree species: aspen, alder, beech, linden (Trees.java) ----
# Blockstates, models, item models, loot and recipes are vanilla birch's with our names (birch leaves drop no apples, like we want).
import re
def from_birch(path, wood):
    text = re.sub(r"minecraft:((?:block/|item/|blocks/)?(?:stripped_|potted_)?)birch_",
                  lambda m: f"mechanical:{m.group(1)}{wood}_", _tjar.read(path).decode())
    return json.loads(text)
TREE_PARTS = ["log", "wood", "planks", "slab", "stairs", "fence", "fence_gate", "door", "trapdoor", "button", "pressure_plate", "leaves", "sapling"]
def tree_blocks(wood):
    return [f"{wood}_{p}" for p in TREE_PARTS] + [f"stripped_{wood}_log", f"stripped_{wood}_wood", f"potted_{wood}_sapling"]
BIRCH_FILE = re.compile(r"(?:stripped_|potted_)?birch_(?!.*(?:sign|boat|shelf))[a-z_]*\.json")
for path in _tjar.namelist():
    folder, _, name = path.rpartition("/")
    if folder not in ("assets/minecraft/blockstates", "assets/minecraft/models/block", "assets/minecraft/models/item", "assets/minecraft/items",
                      "data/minecraft/loot_table/blocks", "data/minecraft/recipe") or not BIRCH_FILE.fullmatch(name):
        continue
    if name == "birch_planks.json" and folder == "data/minecraft/recipe":
        continue  # planks: 2 per log, made above
    for wood in NEW_WOODS:
        obj = from_birch(path, wood)
        if name == "birch_leaves.json" and folder == "assets/minecraft/items":
            obj["model"].pop("tints", None)  # our leaf textures are pre-coloured
        if folder.startswith("assets"):
            out = A / folder.split("/", 2)[2] / name.replace("birch", wood)
        else:
            out = D / "mechanical" / folder.split("/", 2)[2] / name.replace("birch", wood)
        w(out, obj)

# stripping with an axe: our rules go in front of vanilla's
axe = json.loads(_tjar.read("data/minecraft/block_transformer/axe.json").decode())
strip = lambda src, dst: {"if_true": {"type": "minecraft:matching_blocks", "blocks": f"mechanical:{src}"},
                          "then": {"type": "minecraft:copy_properties", "source": {"id": f"mechanical:{dst}", "properties": {"axis": "y"}}}}
axe[0]["block_state_provider"]["rules"][:0] = [strip(f"{x}_{k}", f"stripped_{x}_{k}") for x in NEW_WOODS for k in ("log", "wood")]
w(D / "minecraft/block_transformer/axe.json", axe)

# tags
for x in NEW_WOODS:
    logs = [f"mechanical:{x}_log", f"mechanical:{x}_wood", f"mechanical:stripped_{x}_log", f"mechanical:stripped_{x}_wood"]
    for kind in ("block", "item"):
        w(D / f"mechanical/tags/{kind}/{x}_logs.json", {"values": logs})
TAGS = {"logs_that_burn": "#mechanical:{x}_logs", "planks": "planks", "wooden_slabs": "slab", "wooden_stairs": "stairs",
        "wooden_fences": "fence", "fence_gates": "fence_gate", "wooden_doors": "door", "wooden_trapdoors": "trapdoor",
        "wooden_buttons": "button", "wooden_pressure_plates": "pressure_plate", "leaves": "leaves", "saplings": "sapling"}
for tag, part in TAGS.items():
    vals = [part.format(x=x) if part.startswith("#") else f"mechanical:{x}_{part}" for x in NEW_WOODS]
    for kind in ("block", "item"):
        w(D / f"minecraft/tags/{kind}/{tag}.json", {"values": vals})
w(D / "minecraft/tags/block/overworld_natural_logs.json", {"values": [f"mechanical:{x}_log" for x in NEW_WOODS]})
w(D / "minecraft/tags/block/flower_pots.json", {"values": [f"mechanical:potted_{x}_sapling" for x in NEW_WOODS]})
w(D / "minecraft/tags/block/mineable/hoe.json", {"values": [f"mechanical:{x}_leaves" for x in NEW_WOODS]})
axe_tag = json.loads((D / "minecraft/tags/block/mineable/axe.json").read_text())
axe_tag["values"] += [f"mechanical:{b}" for x in NEW_WOODS for b in tree_blocks(x) if not b.endswith(("leaves", "sapling"))]
w(D / "minecraft/tags/block/mineable/axe.json", axe_tag)

# recipe book: having the logs or planks reveals that wood's recipes
for x in NEW_WOODS:
    recipes = [f"{x}_planks", f"{x}_wood", f"stripped_{x}_wood"] + [f"{x}_{p}" for p in TREE_PARTS[3:11]]
    w(D / f"mechanical/advancement/recipes/{x}_wood.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_logs": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"#mechanical:{x}_logs"}]}},
                     "has_planks": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": f"mechanical:{x}_planks"}]}}},
        "requirements": [["has_logs", "has_planks"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})

for x in NEW_WOODS:
    for b in tree_blocks(x):
        lang[f"block.mechanical.{b}"] = title(b)
w(lang_path, lang)

# worldgen: one tree feature per species (saplings grow the same one), placed in forests and a few other biomes
def tree_feature(x, trunk, foliage, size=None):
    w(D / f"mechanical/worldgen/feature/{x}.json", {"type": "minecraft:tree", "below_trunk_provider": "minecraft:soil_beneath_tree",
        "decorators": [], "foliage_placer": foliage, "ignore_vines": True,
        "foliage_provider": {"id": f"mechanical:{x}_leaves", "properties": {"distance": "7", "persistent": "false", "waterlogged": "false"}},
        "minimum_size": size or {"type": "minecraft:two_layers_feature_size"}, "trunk_placer": trunk,
        "trunk_provider": {"id": f"mechanical:{x}_log", "properties": {"axis": "y"}}})
straight = lambda base, a, b=0: {"type": "minecraft:straight_trunk_placer", "base_height": base, "height_rand_a": a, "height_rand_b": b}
blob = lambda r, h: {"type": "minecraft:blob_foliage_placer", "height": h, "offset": 0, "radius": r}
tree_feature("aspen", straight(9, 2, 2), blob(2, 3))     # tall slim trunk 9-13, small birch-like crown on top
tree_feature("alder", straight(6, 3), blob(2, 3))        # medium straight tree 6-9
tree_feature("beech", straight(8, 4), blob(3, 3))        # tall straight trunk 8-12, wide dense crown (radius 3 stays within leaf distance 6)
tree_feature("linden", {"type": "minecraft:fancy_trunk_placer", "base_height": 8, "height_rand_a": 6, "height_rand_b": 0},
             {"type": "minecraft:fancy_foliage_placer", "height": 4, "offset": 4, "radius": 3},  # broader crown than the (fancy) oaks
             {"type": "minecraft:two_layers_feature_size", "limit": 0, "min_clipped_height": 6, "upper_size": 0})
weighted = lambda *counts: {"type": "minecraft:count", "count": {"type": "minecraft:weighted_list",
                            "distribution": [{"data": n, "weight": wt} for n, wt in counts]}}
rarity = lambda chance: {"type": "minecraft:rarity_filter", "chance": chance}
# placed feature -> (tree, frequency per chunk, biomes)
FOREST = ["minecraft:forest", "minecraft:flower_forest"]
TREE_PLACEMENTS = {
    "aspen_forest": ("aspen", weighted((0, 1), (1, 1)), FOREST),
    "aspen_sparse": ("aspen", rarity(5), ["minecraft:meadow", "minecraft:taiga"]),
    "alder_forest": ("alder", rarity(3), FOREST),
    "alder_waterside": ("alder", weighted((0, 1), (1, 2), (2, 1)), ["minecraft:river", "minecraft:swamp"]),
    "beech_forest": ("beech", weighted((0, 1), (1, 2), (2, 1)), FOREST),
    "linden_forest": ("linden", weighted((0, 1), (1, 1)), FOREST),
    "linden_plains": ("linden", rarity(12), ["minecraft:plains", "minecraft:sunflower_plains"]),
}
for name, (x, freq, biomes) in TREE_PLACEMENTS.items():
    w(D / f"mechanical/worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{x}", "placement": [
        freq, {"type": "minecraft:in_square"}, {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"}, {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:would_survive", "state": f"mechanical:{x}_sapling"}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "vegetal_decoration"})
print("trees ok")

# ---- fresh leaf litter: one per leaf type, leaf-coloured, dries into vanilla leaf litter (textures: gen_leaf_litter_textures.py) ----
LITTER_LEAVES = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "yellow_poplar",
                 "orange_poplar", "red_poplar", "azalea", "flowering_azalea", "aspen", "alder", "beech", "linden"]
_ljar = zipfile.ZipFile(JAR)
_lstate = _ljar.read("assets/minecraft/blockstates/leaf_litter.json").decode()
_lloot = _ljar.read("data/minecraft/loot_table/blocks/leaf_litter.json").decode()
for leaf in LITTER_LEAVES:
    name = f"{leaf}_leaf_litter"
    for n in range(1, 5):
        w(A / f"models/block/{name}_{n}.json", {"parent": f"minecraft:block/template_leaf_litter_{n}", "textures": {"texture": f"mechanical:block/{name}"}})
    w(A / f"blockstates/{name}.json", json.loads(_lstate.replace("minecraft:block/leaf_litter_", f"mechanical:block/{name}_")))
    # drops vanilla Leaf Litter (one per layer), so it is tinder like any other
    w(D / f"mechanical/loot_table/blocks/{name}.json", json.loads(_lloot.replace('"blocks": "minecraft:leaf_litter"', f'"blocks": "mechanical:{name}"')
        .replace('"minecraft:blocks/leaf_litter"', f'"mechanical:blocks/{name}"')))
    lang[f"block.mechanical.{name}"] = title(name)
for tag in ("washed_away_by_fluids", "replaceable_by_trees", "replaceable_by_mushrooms", "replaceable", "inside_step_sound_blocks"):
    w(D / f"minecraft/tags/block/{tag}.json", {"values": [f"mechanical:{l}_leaf_litter" for l in LITTER_LEAVES]})
w(lang_path, lang)
print("leaf litter ok")

# ---- later-age blocks found in villages: locked (TooAdvanced.java) and they break into primitive scraps ----
def _scraps(item, lo, hi):
    return [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": item,
        "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]}]}] if hi else []
TOO_ADVANCED = {  # block: (scrap item, min, max); max 0 = nothing
    "furnace": ("mechanical:stone_chunk", 3, 6), "smoker": ("mechanical:stone_chunk", 3, 6), "blast_furnace": ("mechanical:stone_chunk", 4, 8),
    "stonecutter": ("mechanical:stone_chunk", 2, 4), "brewing_stand": ("mechanical:stone_chunk", 1, 3),
    "anvil": ("", 0, 0), "chipped_anvil": ("", 0, 0), "damaged_anvil": ("", 0, 0),
    "smithing_table": ("minecraft:stick", 2, 4), "grindstone": ("mechanical:stone_chunk", 2, 4),
    "cauldron": ("", 0, 0), "water_cauldron": ("", 0, 0), "lava_cauldron": ("", 0, 0), "powder_snow_cauldron": ("", 0, 0),
    "bell": ("", 0, 0), "lantern": ("", 0, 0), "soul_lantern": ("", 0, 0),
}
for block, (item, lo, hi) in TOO_ADVANCED.items():
    w(D / f"minecraft/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": _scraps(item, lo, hi),
        "random_sequence": f"minecraft:blocks/{block}"})
# using them is locked; cauldrons, bells and lanterns stay usable (water, ringing, light), only their loot changes
w(D / "mechanical/tags/block/too_advanced.json", {"values": [f"minecraft:{b}" for b in
    ("furnace", "smoker", "blast_furnace", "stonecutter", "brewing_stand", "anvil", "chipped_anvil", "damaged_anvil", "smithing_table", "grindstone")]
    + ["#minecraft:beds"]})
# village beds can't be slept in (beds aren't craftable in the stone age); broken, they give back their wool
for c in COLORS:
    w(D / f"minecraft/loot_table/blocks/{c}_bed.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"minecraft:{c}_wool", "condition": {"type": "minecraft:match_block",
            "blocks": f"minecraft:{c}_bed", "state": {"part": "head"}},
         "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]}],
        "random_sequence": f"minecraft:blocks/{c}_bed"})
lang["message.mechanical.too_advanced"] = "You don't know how to use this yet"
w(lang_path, lang)

# ---- structure loot everywhere else (mineshafts, portals, shipwrecks, temples, dungeons ...): no metal, gems or enchanting ----
# Vanilla tables are copied with every entry from a later age filtered out; chests also get a pool of primitive scraps.
LATER_AGE = ("iron", "gold", "diamond", "emerald", "netherite", "copper", "lapis", "redstone", "chainmail", "bucket", "compass",
             "clock", "shears", "rail", "anvil", "tnt", "experience_bottle", "smithing_template", "nugget", "crossbow",
             "flint_and_steel", "spyglass", "brush", "enchanted", "minecraft:bow", "lantern", "chain", "piston", "hopper",
             "map", "quartz", "glistering", "fishing_rod", "trident", "tripwire_hook")
def _later_age(entry):
    text = json.dumps(entry)
    return entry.get("type") == "minecraft:item" and (any(k in entry.get("name", "") for k in LATER_AGE)
        or any(f in text for f in ("enchant_randomly", "enchant_with_levels", "set_enchantments", "enchanted_book")))
def _filter_entries(entries):
    out = []
    for e in entries:
        if "children" in e:
            e = dict(e, children=_filter_entries(e["children"]))
            if not e["children"]:
                continue
        elif e.get("type") == "minecraft:tag" and any(k in e.get("name", "") for k in LATER_AGE):
            continue
        elif _later_age(e):
            continue
        out.append(e)
    return out
SCRAPS = [_e("minecraft:stick", 1, 4, 10), _e("minecraft:flint", 1, 3, 8), _e(M + "plant_fibre", 1, 4, 8), _e(M + "cordage", 1, 2, 5),
          _e("minecraft:bone", 1, 3, 6), _e(M + "twine", 1, 2, 4), _e(M + "stone_chunk", 1, 4, 6), _e("minecraft:wheat_seeds", 1, 3, 4)]
_sjar = zipfile.ZipFile(JAR)
_filtered = 0
for path in _sjar.namelist():
    rel = path.removeprefix("data/minecraft/loot_table/")
    if rel == path or not path.endswith(".json") or rel.startswith("chests/village/"):
        continue
    if not rel.startswith(("chests/", "archaeology/", "barrels/", "dispensers/", "pots/", "spawners/", "trade_rebalance/",
                           "gameplay/hero_of_the_village/", "gameplay/fishing/", "entities/", "equipment/")):
        continue
    out = (D / f"minecraft/loot_table/{rel}").resolve()
    # build on our own override when this run already made one (fish, golem ...), else on vanilla's
    table = json.loads(out.read_text()) if out in WRITTEN else json.loads(_sjar.read(path).decode())
    pools = []
    for pool in table.get("pools", []):
        entries = _filter_entries(pool.get("entries", []))
        if entries:
            pools.append(dict(pool, entries=entries))
    if rel.startswith(("chests/", "trade_rebalance/", "barrels/")):
        pools.append({"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": SCRAPS})
    elif not pools:
        pools.append({"rolls": 1, "entries": SCRAPS})
    table["pools"] = pools
    w(D / f"minecraft/loot_table/{rel}", table)
    _filtered += 1
print(f"structure loot ok ({_filtered} tables)")

# ---- mobs without blood (Blood.java): bones, slime, fire, wind, ghosts, shells, golems ----
w(D / "mechanical/tags/entity_type/bloodless.json", {"values": ["#minecraft:skeletons", "minecraft:slime", "minecraft:magma_cube",
    "minecraft:blaze", "minecraft:breeze", "minecraft:creeper", "minecraft:vex", "minecraft:ghast", "minecraft:shulker",
    "minecraft:wither", "minecraft:iron_golem", "minecraft:snow_golem", "minecraft:creaking", "minecraft:endermite", "minecraft:silverfish"]})
