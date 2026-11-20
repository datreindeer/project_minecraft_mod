"""Advancement tabs that show the way forward, and hide the vanilla tabs.

- data/mechanical/advancement/<tab>/<id>.json for every row in TABS: four tabs (Stone Age, Wilderness & Hunting,
  Food & Farming, Bronze Age). Each row: id, parent, icon, frame, title, description, criteria. The parent is the step
  you need first, so the tree reads as the progression. Lang keys advancements.mechanical.<tab>.<id>.title/.description.
- Every vanilla advancement in data/minecraft/advancement/{story,nether,end,adventure,husbandry}/ (listed from the merged
  jar) is overridden with a neoforge:never condition, so those tabs are gone. recipes/** (recipe-book unlocks) are kept.
- Every item id used must have an item model definition (assets/<ns>/items/<id>.json in the mod or the jar) and every
  tag a tag file, so a typo fails here. Parents must exist in the same tab.

EVERY NEW FEATURE ADDS ITS ROWS TO TABS (item -> where it sits in the tree and a description that says how to get it).

Generator order: ... gen_cart.py, gen_smelter.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_advancements.py
"""
import glob
import json
import shutil
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
D = RES / "data"
LANG = RES / "assets/mechanical/lang/en_us.json"
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar"))
             + glob.glob(str(ROOT.parent / "project_minecraft_mod/build/moddev/artifacts/minecraft-patched-*-merged.jar"))
             + glob.glob(str(Path.home() / "Documents/project_minecraft_mod/build/moddev/artifacts/minecraft-patched-*-merged.jar")))
assert JAR, "no merged minecraft jar (run a gradle build first)"
jar = zipfile.ZipFile(JAR[0])
JAR_NAMES = set(jar.namelist())
VANILLA_TABS = ("story", "nether", "end", "adventure", "husbandry")
M = "mechanical:"


def mid(i):
    """'x' -> 'mechanical:x', '#x' -> '#mechanical:x', namespaced ids unchanged."""
    tag, s = i.startswith("#"), i.lstrip("#")
    return ("#" if tag else "") + (s if ":" in s else M + s)


def check_item(i):
    i = mid(i)
    ns, path = i.lstrip("#").split(":")
    if i.startswith("#"):
        rel = f"data/{ns}/tags/item/{path}.json"
    else:
        rel = f"assets/{ns}/items/{path}.json"
    assert (RES / rel).exists() or rel in JAR_NAMES, f"unknown item/tag {i}"
    return i


# ---------------------------------------------------------------- criteria helpers (each returns {name: criterion})

class AllOf(dict):
    """Criteria that must ALL be met (plain dicts mean any one)."""


def has_all(*items):
    """Every one of the items in the inventory at some time (each criterion stays done once met)."""
    return AllOf({f"has_{n}": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": check_item(i)}]}}
                  for n, i in enumerate(items)})


def has(*items):
    """Any one of the items (or #tags) in the inventory."""
    return {f"has_{n}": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": check_item(i)}]}}
            for n, i in enumerate(items)}


def placed(*blocks):
    return {f"placed_{n}": {"trigger": "minecraft:placed_block",
                            "conditions": {"location": {"type": "minecraft:match_block", "blocks": mid(b)}}}
            for n, b in enumerate(blocks)}


def used_on(tool, block=None, state=None):
    terms = [{"type": "minecraft:match_tool", "predicate": {"items": check_item(tool)}}]
    if block:
        pred = {"blocks": mid(block)}
        if state:
            pred["state"] = state
        terms.insert(0, {"type": "minecraft:location_check", "predicate": {"block": pred}})
    return {"used": {"trigger": "minecraft:item_used_on_block",
                     "conditions": {"location": {"type": "minecraft:all_of", "terms": terms}}}}


def by_code():
    """Granted from Java (PlayerAdvancements.award with criterion "done"), e.g. when a Smelting Furnace is complete."""
    return {"done": {"trigger": "minecraft:impossible"}}


class AllOf(dict):
    """Criteria that must ALL be met (plain dicts mean any one)."""


def visit(structure):
    return {"visit": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this",
        "predicate": {"minecraft:location": {"structures": mid(structure)}}}}}}


def tamed(entity):
    return {"tamed": {"trigger": "minecraft:tame_animal", "conditions": {"entity": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": mid(entity)}}}}}


def stood_on(block, state=None):
    """The player standing on the block (checked about once a second)."""
    pred = {"blocks": mid(block), **({"state": state} if state else {})}
    return {"stood_on": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:stepping_on": {"block": pred}}}}}}


def killed(predicate):
    """The player kills an entity matching the predicate."""
    return {"killed": {"trigger": "minecraft:player_killed_entity", "conditions": {"entity": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": predicate}}}}


SCRAPS = [f"{a}_scraps" for a in ("beef", "porkchop", "mutton", "chicken", "rabbit", "game_meat", "venison")]
COOKED = [f"cooked_{s}" for s in SCRAPS] + ["cooked_haunch", "cooked_ribs", "cooked_loin"]
BERRIES = ["raspberries", "blueberries", "lingonberries", "wild_strawberries", "elderberries", "cloudberries",
           "blackberries", "chanterelle", "porcini", "morel", "oyster_mushroom", "puffball", "hazelnuts", "beechnuts",
           "juniper_berries", "minecraft:sweet_berries"]
T, G, C = "task", "goal", "challenge"
BARK_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar",
              "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew"]  # Mechanical.WOODS

# (id, parent, icon, frame, title, description, criteria). The first row of a tab is its root.
TABS = {
    "stone_age": ("minecraft:gui/advancements/backgrounds/stone", [
        ("root", None, "minecraft:flint", T, "Stone Age",
         "Bare hands. Pick up Loose Rocks and Loose Flint lying on the ground", has("minecraft:flint", "#rock_chunks")),
        ("plant_fibre", "root", "plant_fibre", T, "Grass Roots",
         "Break grass and ferns for Plant Fibre (a knife triples the yield)", has("plant_fibre")),
        ("twine", "plant_fibre", "twine", T, "Twisted", "Craft 3 Plant Fibre into Twine", has("twine")),
        ("flint_knife", "twine", "flint_knife", T, "Sharp Edge",
         "Craft a Flint Knife from Flint, Twine and a Stick. It stays in the grid as a tool", has("flint_knife")),
        ("flint_axe", "twine", "flint_axe", T, "First Axe",
         "Craft a Flint Axe (2x2, fits the inventory grid). Logs need an axe", has("flint_axe")),
        ("flint_pickaxe", "twine", "flint_pickaxe", T, "Hammerstone",
         "Craft a Flint Pickaxe. It also knaps rocks into stone tool heads", has("flint_pickaxe")),
        ("flint_tools", "twine", "flint_shovel", T, "Diggers", "Craft a Flint Shovel or a Flint Hoe",
         has("flint_shovel", "flint_hoe")),
        ("cordage", "twine", "cordage", T, "Strong Rope", "Craft 3 Twine into Cordage", has("cordage")),
        ("firewood", "flint_axe", "oak_firewood", T, "Firewood",
         "Chop a tree with an axe: its logs split into Firewood", has("#firewood")),
        ("bark", "flint_axe", "oak_bark", T, "Bark Peeler",
         "Hold right-click with an axe on a Fallen Log to peel off its Bark, one kind per wood (tinder, bast, baskets, roofs)", has("#barks")),
        ("bark_collector", "bark", "birch_bark", C, "Bark Collector",
         f"Collect the Bark of all {len(BARK_WOODS)} woods, from Oak to Yew: each wood's Fallen Log gives its own",
         has_all(*[f"{w}_bark" for w in BARK_WOODS])),
        ("bark_basket", "bark", "bark_basket", T, "Gatherer's Basket",
         "Craft a Bark Basket for berries, mushrooms and hides. Food keeps longer inside", has("bark_basket")),
        ("bark_roof", "bark", "spruce_bark_roof", T, "Shingled",
         "Craft a Bark Roof from 3 Bark of one wood. Against a wall it lies as a slope, on top of a block it lies flat", has("#bark_roofs")),
        ("tool_rod", "flint_knife", "tool_rod", T, "Straight Shaft",
         "Put a knife above any Log in the crafting grid to carve a Tool Rod", has("tool_rod")),
        ("pine_resin", "flint_knife", "pine_resin", T, "Resin Torch",
         "Right-click a Pine Log with a knife for Pine Resin. Resin over a Stick makes 2 Torches", has("pine_resin")),
        ("stone_heads", "flint_pickaxe", "stone_pickaxe_head", T, "Knapper",
         "Knap 3 rock chunks with the Flint Pickaxe into a Stone Pickaxe Head or Stone Axe Head",
         has("stone_pickaxe_head", "stone_axe_head")),
        ("double_heads", "stone_heads", "double_stone_pickaxe_head", T, "Double-Sided",
         "Knap two heads together with the Flint Pickaxe between them",
         has("double_stone_pickaxe_head", "double_stone_axe_head")),
        ("tool_station", "flint_pickaxe", "tool_station", T, "Tool Station",
         "Build a Tool Station from a Flint Axe and a Flint Pickaxe. It joins head, binding and shaft", has("tool_station")),
        ("crude_stone", "tool_station", "crude_stone_pickaxe", T, "Crude but Stone",
         "Tool Station: stone head + Twine + Stick makes a Crude Stone Pickaxe or Axe",
         has("crude_stone_pickaxe", "crude_stone_axe")),
        ("stone_pickaxe", "crude_stone", "minecraft:stone_pickaxe", G, "Proper Stone Pickaxe",
         "Tool Station: Double Stone Pickaxe Head + Cordage + Tool Rod", has("minecraft:stone_pickaxe")),
        ("stone_axe", "crude_stone", "minecraft:stone_axe", T, "Proper Stone Axe",
         "Tool Station: Double Stone Axe Head + Cordage + Tool Rod", has("minecraft:stone_axe")),
        ("fire_drill", "twine", "fire_drill", T, "Friction", "Craft a Fire Drill from Twine and Sticks",
         has("fire_drill")),
        ("woodpile", "firewood", "oak_firewood", T, "Woodpile",
         "Right-click the top of a solid block with Firewood to stack a pile. More of the same wood stacks higher; an empty hand takes a piece back",
         by_code()),
        ("campfire", "firewood", "minecraft:campfire", T, "Hearth",
         "Build a Campfire from Sticks and 3 Firewood. It is placed unlit", has("minecraft:campfire")),
        ("light_fire", "campfire", "fire_drill", G, "Let There Be Fire",
         "Right-click an unlit Campfire with the Fire Drill. Leaf Litter or Bark as tinder first helps it catch",
         used_on("fire_drill", "minecraft:campfire", {"lit": "true"})),
        ("broom", "light_fire", "broom", T, "Clean Sweep",
         "Fires leave soot. Tool Station: Straw + Twine + Tool Rod makes a Broom; each sweep thins soot in a 3x3 by one stage",
         by_code()),
        ("fire_pit", "light_fire", "fire_pit", G, "Bonfire",
         "Ring a Firewood with 8 rock chunks into a Fire Pit, set it on a flat 2x2 of ground and light it with the Fire Drill. "
         "Big light, big smoke, bigger raids", used_on("fire_drill", "fire_pit", {"lit": "true"})),
        ("kiln", "light_fire", "kiln", G, "Clay Kiln",
         "Craft a Clay Kiln (Clay Balls, rock chunks) and place it on a Campfire. Burn it 2 min at 500° before use",
         placed("kiln")),
        ("brick", "kiln", "minecraft:brick", T, "Fired Clay", "Bake a Clay Ball into a Brick in the kiln (600°)",
         has("minecraft:brick")),
        ("charcoal", "kiln", "minecraft:charcoal", T, "Charcoal Burner",
         "Bake a Log or 2 Firewood into Charcoal in the kiln. The hottest fuel", has("minecraft:charcoal")),
        ("clay_bucket", "kiln", "clay_bucket", T, "Clay Bucket",
         "Shape 3 Clay Balls into an Unfired Clay Bucket and fire it in the kiln. It only carries water",
         has("clay_bucket", "clay_water_bucket")),
        ("mold_bench", "kiln", "mold_bench", T, "Mold Bench",
         "Craft a Mold Bench from Flint, a Clay block and Planks. Crucibles and clay molds are shaped there from Clay Balls",
         has("mold_bench")),
        ("crucible", "mold_bench", "crucible", G, "Crucible",
         "Shape an Unfired Crucible (7 Clay Balls) at the Mold Bench, fire it in the kiln at 800°: needs Charcoal or Bellows. The key to bronze",
         has("crucible")),
        ("log_raft", "cordage", "log_raft", T, "Raft", "Lash 6 logs together with 3 Cordage into a Log Raft",
         has("log_raft")),
        ("paddle", "log_raft", "paddle", T, "Paddle", "Carve a Rough Log into a Paddle with a knife", has("paddle")),
        ("dugout_canoe", "paddle", "dugout_canoe", G, "Dugout Canoe",
         "Mark a Fallen Log with a knife, then work it: axe, fire, axe, fire, axe, knife", has("dugout_canoe")),
        ("ochre", "root", "red_ochre", T, "Earth Colours",
         "Pick up Loose Red Ochre (badlands, savanna), Yellow Ochre (desert, rivers) or Chalk (shores, hills)",
         has("red_ochre", "yellow_ochre", "chalk")),
        ("pigment", "ochre", "red_pigment", T, "Ground Colour",
         "Grind ochre, chalk or Charcoal with the Flint Pickaxe in the grid, or on a Stone Anvil",
         has("red_pigment", "yellow_pigment", "white_pigment", "black_pigment")),
        ("paint", "pigment", "red_paint", T, "Paint Pot", "Mix 2 pigment with Tallow or water in a Bowl",
         has("red_paint", "yellow_paint", "white_paint", "black_paint")),
        ("cave_painting", "paint", "paintbrush", G, "Cave Painter",
         "Paint in the off hand, right-click a wall with a Paintbrush (Stick + 2 Plant Fibre). Sneak to pick a motif",
         used_on("paintbrush")),
        ("beaten_path", "root", "trodden_path", T, "Beaten Path",
         "Walk the same way over grass a few dozen times until it wears into a Trodden Path. Paths are faster to walk on",
         stood_on("trodden_path", {"persistent": "false"})),
    ]),
    "wilderness": ("minecraft:gui/advancements/backgrounds/adventure", [
        ("root", None, "flint_knife", T, "Wilderness & Hunting",
         "Make a knife. Kill an animal and right-click its Carcass with the knife to butcher it", has("#knives")),
        ("butcher", "root", "venison_scraps", T, "Butcher",
         "Butcher a Carcass with a knife: hides and feathers first, then meat scraps",
         has("raw_hide", *SCRAPS)),
        ("pluck_goose", "butcher", "plucked_goose", T, "Goose Down",
         "Pluck a Greylag Goose carcass with a knife, then take the Plucked Goose with an empty hand or cut it into Raw Goose",
         has("plucked_goose", "raw_goose")),
        ("bird_nest", "pluck_goose", "bird_nest", T, "Home to Roost",
         "Build a Bird Nest from Sticks and Straw near geese, pheasants, ptarmigans, rock doves or chickens: they claim it and lay in it",
         placed("bird_nest")),
        ("scraped_hide", "butcher", "scraped_hide", T, "Hide Scraper",
         "Scrape a Raw Hide with a knife in the crafting grid", has("scraped_hide")),
        ("drying_rack", "scraped_hide", "drying_rack", T, "Drying Rack",
         "Build a Drying Rack and right-click it with a Scraped Hide", has("drying_rack")),
        ("leather", "drying_rack", "minecraft:leather", G, "Leather",
         "Take the dry Leather off the Drying Rack after about 3 minutes", has("minecraft:leather")),
        ("bone_needle", "root", "bone_needle", T, "Needlework",
         "Carve Fish Bones, a Bone or a Horn into a Bone Needle with a knife", has("bone_needle")),
        ("armor_station", "leather", "armor_station", T, "Armor Station",
         "Craft an Armor Station from Leather, a Bone Needle, Planks and a Crafting Table", has("armor_station")),
        ("leather_armor", "armor_station", "minecraft:leather_chestplate", T, "Hide Clothes",
         "Sew Leather and thread (wool, nettle, linen or sinew) into Leather armor at the Armor Station",
         has("minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings",
             "minecraft:leather_boots")),
        ("leather_satchel", "armor_station", "leather_satchel", T, "Satchel",
         "Sew 4 Leather into a Leather Satchel: 9 slots that go with you", has("leather_satchel")),
        ("worn_satchel", "leather_satchel", "leather_satchel", T, "Shoulder Bag",
         "Put the Leather Satchel in the Back slot next to your player in the inventory, then press B to open it anywhere",
         by_code()),
        ("worn_cloak", "leather_armor", "otter_fur_cape", T, "Cloaked",
         "Wear a cloak (Otter Fur Cape, Ermine Mantle or Glider Cloak) in the Cloak slot: warmth and armor over your armor",
         by_code()),
        ("travois", "scraped_hide", "travois", T, "Travois",
         "Lash 4 Tool Rods, 2 Cordage and a Scraped Hide into a Travois to drag big carcasses home", has("travois")),
        ("hanging_rack", "butcher", "hanging_rack", T, "Hanging Rack",
         "Build a Hanging Rack (5 Tool Rods, Cordage) and hang a carried carcass on it", has("hanging_rack")),
        ("prime_cuts", "hanging_rack", "raw_haunch", T, "Prime Cuts",
         "Butcher a carcass on the Hanging Rack: double meat as Haunch, Ribs and Loin",
         has("raw_haunch", "raw_ribs", "raw_loin")),
        ("snare", "root", "snare", T, "Snare",
         "Set a Snare (2 Sticks, Cordage) where small game walks. Bait it with berries or seeds", has("snare")),
        ("fishing", "root", "whole_cod", T, "Gone Fishing",
         "Fish with a Crude Fishing Rod (Tool Station: Flint + Twine + Tool Rod) or a Fish Trap", has("#whole_fish")),
        ("fish_trap", "fishing", "fish_trap", T, "Fish Trap",
         "Craft a Fish Trap (6 Sticks, 2 Twine) and place it in open water, best in rivers", has("fish_trap")),
        ("fish_bones", "fishing", "fish_bones", T, "Clean the Catch",
         "Gut a Whole fish with a knife in the crafting grid for fish and Fish Bones", has("fish_bones")),
        ("bone_hook_rod", "fish_bones", "bone_hook_fishing_rod", T, "Bone Hook",
         "Carve a Bone Fish Hook, then Tool Station: hook + Twine + Tool Rod", has("bone_hook_fishing_rod")),
        ("flint_spear", "root", "flint_spear", T, "Spear",
         "Tool Station: Flint + Cordage + Tool Rod. Sprint and hold right-click to charge", has("flint_spear")),
        ("crude_bow", "root", "crude_bow", T, "Bowyer", "Tool Station: Cordage + Twine + Tool Rod makes a Crude Bow",
         has("crude_bow")),
        ("arrows", "crude_bow", "minecraft:arrow", T, "Fletcher",
         "Right-click a Fletching Table: Flint or bone + Stick + Feather makes 4 arrows",
         has("minecraft:arrow", "bone_arrow")),
        ("sinew_bow", "arrows", "sinew_bow", G, "Sinew Bow",
         "Fletching Table: Sinew + Crude Bow + Cordage. Full arrow damage", has("sinew_bow")),
        ("tallow", "butcher", "tallow", T, "Rendered Fat",
         "Cook Lard (Wild Boar) or Bear Fat on a Campfire into Tallow", has("tallow")),
        ("candle", "tallow", "minecraft:candle", T, "Candlelight",
         "Craft Twine and 2 Tallow into Candles, light them with the Fire Drill", has("minecraft:candle")),
        ("fire_arrow", "arrows", "fire_arrow", T, "Fire Arrow", "Fletching Table: Tallow + Arrow + Twine",
         has("fire_arrow")),
        ("quiver", "arrows", "quiver", T, "Quiver",
         "Sew 3 Leather into a Quiver at the Armor Station and wear it in the Hip slot: bows shoot from it, picked-up arrows go in",
         by_code()),
        ("tusk_dagger", "leather", "tusk_dagger", T, "Hunter's Knife",
         "Butcher a Wild Boar for a Tusk. Tool Station: Tusk + Cordage + Leather makes a Tusk Dagger",
         has("tusk_dagger")),
        ("bear", "tusk_dagger", "raw_bear_pelt", C, "Bear Hunter",
         "Kill a Brown Bear and butcher it with a Tusk Dagger (or bronze knife) for its pelt, fat and claws",
         has("raw_bear_pelt", "bear_claw", "bear_fat")),
        ("bear_pelt", "bear", "bear_pelt", T, "Bear Skin",
         "Dry the Raw Bear Pelt on a tall Drying Rack outdoors by day (it takes the whole rack)", has("bear_pelt")),
        ("fur_armor", "bear_pelt", "fur_coat", G, "Winter Furs",
         "Sew Bear Pelt and Sinew into Fur armor at the Armor Station. Each piece blocks 25 % of the cold",
         has("fur_hood", "fur_coat", "fur_leggings", "fur_boots")),
        ("bear_rug", "bear_pelt", "bear_rug", T, "Bear Rug", "Craft a Bear Pelt and a Bear Claw into a Bear Rug",
         has("bear_rug")),
        ("barbed_arrow", "bear", "barbed_arrow", T, "Barbed Arrow", "Fletching Table: Bear Claw + Stick + Feather",
         has("barbed_arrow")),
        ("raw_wolf_pelt", "butcher", "raw_wolf_pelt", T, "Wolf Pack",
         "Kill a Grey Wolf (they hunt at night and fear fire) and butcher it for a Raw Wolf Pelt",
         has("raw_wolf_pelt")),
        ("wolf_pelt", "raw_wolf_pelt", "wolf_pelt", T, "Wolf Pelt",
         "Dry the Raw Wolf Pelt on a tall Drying Rack outdoors by day", has("wolf_pelt")),
        ("wolf_hood", "wolf_pelt", "wolf_hood", G, "Wolf Hood",
         "Sew 2 Wolf Pelts and Sinew into a Wolf Hood at the Armor Station", has("wolf_hood")),
        ("wolf_fur", "wolf_pelt", "wolf_fur_coat", T, "Wolf Fur",
         "Sew Wolf Pelts and Sinew into a Wolf Fur Coat, Leggings or Boots",
         has("wolf_fur_coat", "wolf_fur_leggings", "wolf_fur_boots")),
        ("wolf_rug", "wolf_pelt", "wolf_rug", T, "Wolf Rug",
         "Craft 2 Wolf Pelts and a Bone into a Wolf Rug. A pelt on a Straw Bed makes a warm Furred Straw Bed",
         has("wolf_rug")),
        ("phantom_hide", "butcher", "raw_phantom_hide", T, "Phantom Skinner",
         "Kill a Phantom yourself and butcher it for Raw Phantom Hide", has("raw_phantom_hide")),
        ("phantom_leather", "phantom_hide", "phantom_leather", T, "Sunlit Leather",
         "Scrape the hide and dry it on a Drying Rack, in daylight only", has("phantom_leather")),
        ("glider_frame", "phantom_leather", "glider_frame", T, "Glider Frame",
         "Craft 4 Tool Rods, 2 Bones, Sinew and Cordage into a Glider Frame", has("glider_frame")),
        ("glider_cloak", "glider_frame", "glider_cloak", C, "Taking Wing",
         "Sew 6 Phantom Leather and the Glider Frame into a Glider Cloak at the Armor Station", has("glider_cloak")),
        ("ancient_oak", "root", "ancient_oak_rune_log", G, "The Ancient Oak",
         "Find the great hollow oak. Trails lead to it from the villages", visit("ancient_oak")),
        ("giant_antler", "ancient_oak", "giant_antler", C, "Giant Slayer",
         "Bring down the rare, keen-eyed Giant Deer for its Giant Antler. It charges anyone who hurts it and rams down the tree you climb",
         has("giant_antler")),
        ("mythic", "giant_antler", "thunderstone", C, "Gift of the Oak",
         "Offer the Giant Antler on the Offering Altar inside the Ancient Oak",
         has("thunderstone", "mistletoe_charm", "ancient_amber", "heart_of_the_oak", "antler_crown")),
        ("undead_brute", "root", "minecraft:rotten_flesh", G, "The Bigger They Are",
         "Kill a Brute: the rare huge zombie in a bearskin cape, slow but tough and hard to knock back",
         killed({"minecraft:entity_type": "minecraft:zombie", "minecraft:entity_tags": {"any_of": ["mechanical.zombie_brute"]}})),
        ("spear_skeleton", "flint_spear", "minecraft:skeleton_skull", T, "Broken Spear",
         "Kill a skeleton charging with a Flint Spear. Now and then it drops the spear",
         killed({"minecraft:entity_type": "minecraft:skeleton",
                 "minecraft:equipment": {"mainhand": {"items": check_item("flint_spear")}}})),
        ("tribe_village", "root", "sharpened_oak_log", T, "Palisade",
         "Find a Tribe Village. Stay on good terms: camps that burn for days draw raiders", visit("tribe_village")),
        # fauna: river
        ("beaver", "butcher", "raw_beaver_pelt", T, "Busy Beaver",
         "Hunt a Beaver by a forest stream or lake and butcher it for its pelt, tail, castoreum and teeth",
         has("raw_beaver_pelt", "castoreum", "beaver_tooth")),
        ("beaver_dam", "beaver", "beaver_dam", T, "Dam Builders",
         "Find a Beaver Dam: beavers fell small trees, gnaw the logs and dam narrow streams", has("beaver_dam")),
        ("pelt_hoop", "beaver", "pelt_hoop", T, "Stretched Thin",
         "Lace a Raw Beaver or Otter Pelt into a Stretching Hoop (4 Sticks, Twine) and let it cure outdoors by day",
         has("beaver_pelt", "otter_pelt")),
        ("waterproof_fur", "pelt_hoop", "beaver_fur_hat", G, "Water off a Beaver's Back",
         "Make a Beaver Fur Hat or an Otter Fur Cape: warm, and they keep off the chill of rain and water",
         has("beaver_fur_hat", "otter_fur_cape")),
        ("beaver_tooth_knife", "beaver", "beaver_tooth_knife", T, "Chisel Tooth",
         "Set a Beaver Tooth in a stick with Twine: a sharp little knife", has("beaver_tooth_knife")),
        ("castoreum_salve", "beaver", "castoreum_salve", T, "Old Remedy",
         "Mix Castoreum, Tallow and Bark into Castoreum Salve: heals and draws out poison. Castoreum also baits snares",
         has("castoreum_salve")),
        ("otter", "butcher", "raw_otter_pelt", T, "Sleek Swimmer",
         "Hunt an Otter along a river or rocky shore. Watch your drying fish: otters are thieves", has("raw_otter_pelt")),
        ("mosquito_swarm", "root", "mosquito_swarm_spawn_egg", T, "Bloodsuckers",
         "Swat a Mosquito Swarm. On warm evenings in the fens they come for you; smoke and fire drive them off",
         killed({"minecraft:entity_type": "mechanical:mosquito_swarm"})),
        # fauna: giants
        ("bison_tongue", "butcher", "bison_tongue", T, "Hunter's Share",
         "Butcher a Steppe Bison for its hide and the hunters' prize, the tongue. A panicked herd stampedes", has("bison_tongue")),
        ("pegged_hide", "butcher", "raw_bison_hide", T, "Pegged Out",
         "A big raw hide (Bison, Musk Ox, Rhino, Mammoth) is too big for a rack: place it on the ground to peg it out, let it cure in the sun",
         has("bison_hide", "musk_ox_hide", "rhino_hide", "mammoth_hide")),
        ("giant_rug", "pegged_hide", "bison_rug", T, "Hearthside Hide", "Make a Bison, Musk Ox or Mammoth Rug from its cured hide",
         has("bison_rug", "musk_ox_rug", "mammoth_rug")),
        ("qiviut", "butcher", "qiviut", T, "Ring of Horns",
         "Butcher a Musk Ox for Qiviut, its fine underwool. Threatened, the herd closes into a ring around its calves", has("qiviut")),
        ("rhino_horn", "butcher", "rhino_horn", G, "Charging Horn",
         "Hunt a Woolly Rhinoceros for its horn. Hurt it and it charges: step aside, and it may stun itself on a tree",
         has("rhino_horn")),
        ("hide_shield", "rhino_horn", "minecraft:shield", T, "Hide Shield",
         "Stretch a cured Rhino Hide over planks into a Shield", has("minecraft:shield")),
        ("rhino_horn_club", "rhino_horn", "rhino_horn_club", T, "Horn Club",
         "Lash a Rhino Horn onto a Tool Rod with Cordage: a slow, heavy club", has("rhino_horn_club")),
        ("mammoth_ivory", "butcher", "mammoth_ivory", C, "Mammoth Hunter",
         "Bring down a Woolly Mammoth and butcher it for its Ivory. The herd's matriarch warns once, then charges",
         has("mammoth_ivory")),
        ("ivory_edge", "mammoth_ivory", "ivory_knife", T, "Ivory Edge",
         "Craft an Ivory Knife from Mammoth Ivory (with Cordage and Leather) or Walrus Ivory: a skinning knife that can cut bears", has("ivory_knife")),
        ("ivory_figurine", "mammoth_ivory", "ivory_figurine", T, "Ivory Carver",
         "Carve Mammoth Ivory into an Ivory Figurine with a knife", has("ivory_figurine")),
        ("mammoth_tallow", "mammoth_ivory", "mammoth_fat", T, "Fat of the Land",
         "Render Mammoth Fat into Tallow on a Campfire (3 per piece)", has("mammoth_fat")),
        # end fauna: giants
        # fauna: forestbirds
        ("lek_hunter", "butcher", "capercaillie_tail_feather", T, "Lek Hunter",
         "Stalk a singing Capercaillie cock at dawn (step in while he grinds, he hears nothing then) and pluck him for his Tail Feathers",
         has("capercaillie_tail_feather")),
        ("great_bustard", "butcher", "plucked_bustard", T, "Wariest of All",
         "Bag a Great Bustard on the open plains: it runs from you long before you are close. Pluck it for a big bird's worth of meat",
         has("plucked_bustard", "raw_bustard")),
        ("owl_feather", "root", "owl_feather", T, "Silent Wings",
         "Bring down an Eagle-Owl as it hunts the forest at night, for its soft Owl Feathers",
         has("owl_feather")),
        ("silent_arrow", "owl_feather", "silent_arrow", G, "Whisper",
         "Fletch Silent Arrows with Owl Feathers at the Fletching Table: a miss lands without a sound and startles nothing",
         has("silent_arrow")),
        ("eagle_feather", "root", "eagle_feather", T, "King of the Mountains",
         "Bring down a Golden Eagle (it may steal your small game!) for Eagle Feathers and Talons",
         has("eagle_feather", "eagle_talon")),
        ("eagle_arrow", "eagle_feather", "eagle_fletched_arrow", T, "Flies True",
         "Fletch Eagle-Fletched Arrows: the hardest-hitting flint arrows",
         has("eagle_fletched_arrow")),
        ("raven_talisman", "root", "raven_talisman", G, "Ravens' Watch",
         "Bind three Raven Feathers to a Bone with Twine. Carried, the dark wears on your mind more slowly",
         has("raven_talisman")),
        # fauna: coast
        ("seal", "butcher", "raw_sealskin", T, "Seal Hunter",
         "Sneak up on a Seal on a cold shore before it slips into the water, and butcher it for Raw Sealskin and Blubber",
         has("raw_sealskin", "blubber")),
        ("sealskin", "seal", "sealskin", T, "Sealskin", "Dry the Raw Sealskin on a tall Drying Rack outdoors by day", has("sealskin")),
        ("sealskin_parka", "sealskin", "sealskin_parka", G, "Dry and Warm",
         "Sew Sealskin and Sinew into a Sealskin Parka or Boots at the Armor Station: warm, and they keep out the chill of the water",
         has("sealskin_parka", "sealskin_boots")),
        ("oil_lamp", "seal", "oil_lamp", T, "Lamp Oil",
         "Craft an Oil Lamp (rock chunks, Plant Fibre), fill it with Blubber and light it with the Fire Drill", placed("oil_lamp")),
        ("walrus", "tusk_dagger", "walrus_ivory", C, "Tusks and Blubber",
         "Kill a Walrus on the frozen sea (it fights back) and butcher it with a Tusk Dagger or better for Ivory, Blubber and hide",
         has("walrus_ivory", "raw_walrus_hide")),
        ("ivory_knife", "walrus", "ivory_knife", T, "Ivory Blade", "Craft Walrus Ivory, Twine and a Stick into an Ivory Knife",
         has("ivory_knife")),
        ("walrus_hide", "walrus", "walrus_hide", T, "Walrus Hide",
         "Dry the Raw Walrus Hide on a tall Drying Rack (it takes the whole rack). A knife cuts it into 6 Cordage", has("walrus_hide")),
        ("walrus_cuirass", "walrus_hide", "walrus_hide_cuirass", G, "Hide Armour",
         "Sew 2 Walrus Hide and Sinew into a Walrus Hide Cuirass at the Armor Station", has("walrus_hide_cuirass")),
        ("pike", "fishing", "whole_pike", T, "Water Wolf",
         "Catch a Pike in a river, fen or swamp: on the hook, or sneak up and spear it", has("whole_pike")),
        ("sturgeon", "pike", "whole_sturgeon", C, "King of the River",
         "Spear one of the rare giant Sturgeons in a river and butcher it, or hook a young one, for its roe and Isinglass",
         has("whole_sturgeon", "isinglass")),
        # fauna: coldhunters
        ("lemming", "snare", "lemming_pelt", T, "Lemming Year",
         "Catch a Lemming on the snowy plains, in a Snare or by hand. Every eighth day they swarm",
         has("raw_lemming", "lemming_pelt")),
        ("ermine_fur", "drying_rack", "ermine_fur", T, "Winter White",
         "Skin an Ermine and cure its Raw Ermine Pelt on a Drying Rack", has("ermine_fur")),
        ("ermine_mantle", "ermine_fur", "ermine_mantle", G, "Chieftain's Mantle",
         "Sew 6 Ermine Fur, Leather and Sinew into an Ermine Mantle: warm as fur and easy to enchant", has("ermine_mantle")),
        ("marten_fur", "drying_rack", "marten_fur", T, "Tree Weasel",
         "Hunt a Pine Marten (it runs up trees and robs bird nests) and cure its pelt on a Drying Rack", has("marten_fur")),
        ("marten_fur_cap", "marten_fur", "marten_fur_cap", T, "Marten Fur Cap",
         "Sew 5 Marten Fur and Sinew into a warm Marten Fur Cap", has("marten_fur_cap")),
        ("wolverine", "butcher", "raw_wolverine_pelt", C, "Glutton",
         "Kill a Wolverine (it stands its ground and steals carcasses and snare bait) and skin it",
         has("raw_wolverine_pelt", "wolverine_claw")),
        ("wolverine_hood", "wolverine", "wolverine_trimmed_hood", G, "Frost-Proof",
         "Trim a Fur Hood with 2 cured Wolverine Fur and Sinew: frost never clings to it, no more freezing",
         has("wolverine_trimmed_hood")),
        ("claw_flesher", "wolverine", "claw_flesher", T, "Claw Flesher",
         "Lash 2 Wolverine Claws, a Stick and Sinew into a skinning knife: more pelt and hide off every carcass",
         has("claw_flesher")),
        ("lynx", "butcher", "raw_lynx_pelt", C, "Night Stalker",
         "Kill a Lynx. It stalks lone wanderers at night: company, fire and torches keep it off", has("raw_lynx_pelt")),
        ("lynx_rug", "lynx", "lynx_rug", T, "Spotted Rug",
         "Cure 2 Lynx Pelts on a Drying Rack and craft them with a Bone into a Lynx Rug", has("lynx_rug")),
        # fauna: meadow
        ("wild_cat", "butcher", "raw_wildcat_pelt", T, "Wildcat",
         "Hunt the shy, night-prowling Wild Cat of forest edges and meadows and skin it for its pelt", has("raw_wildcat_pelt")),
        ("wildcat_fur_cap", "wild_cat", "wildcat_fur_cap", T, "Tabby Cap",
         "Flesh the pelt with a knife, then craft 2 Wildcat Pelts and Sinew into a Wildcat Fur Cap", has("wildcat_fur_cap")),
        ("moleskin", "snare", "moleskin", T, "Velvet",
         "Catch a Mole: snare its runs, or dig up a fresh molehill to flush it out. Flesh its pelt into Moleskin",
         has("moleskin")),
        ("field_vole", "snare", "raw_vole", T, "Pest Control",
         "Catch a Field Vole, the pest that nibbles crops and raids chests by the fields at night", has("raw_vole")),
        ("snakeskin", "butcher", "snakeskin", T, "Shed Skin", "Butcher an Adder or a Grass Snake for Snakeskin", has("snakeskin")),
        ("snakeskin_boots", "snakeskin", "snakeskin_boots", G, "Watch Your Step",
         "Craft Leather Boots, 2 Snakeskin and Sinew into Snakeskin Boots: an adder can't bite through them",
         has("snakeskin_boots")),
        ("venom_gland", "snakeskin", "venom_gland", T, "Fangs",
         "Kill an Adder, the viper of heaths and sunny meadows, and butcher it for its Venom Gland", has("venom_gland")),
        ("poison_arrow", "venom_gland", "minecraft:tipped_arrow", T, "Venom Tips",
         "Craft 8 arrows around a Venom Gland into Arrows of Poison", has("minecraft:tipped_arrow")),
        ("snakebite_poultice", "venom_gland", "snakebite_poultice", T, "Antidote",
         "Bind a Venom Gland, Lard and Plant Fibre into Snakebite Poultices. Eating one cures poison", has("snakebite_poultice")),
    ]),
    "food": ("minecraft:gui/advancements/backgrounds/husbandry", [
        ("root", None, "blueberries", T, "Food & Farming",
         "Pick wild berries, mushrooms or nuts. Fresh food spoils: raw meat in 1 day, berries in 2", has(*BERRIES)),
        ("cooked", "root", "cooked_venison_scraps", T, "Cooked Meal",
         "Cook meat on a lit Campfire. Take it off with an empty hand before it burns", has(*COOKED)),
        ("burnt", "cooked", "burnt_food", T, "Overdone", "Leave food on the Campfire too long", has("burnt_food")),
        ("roasted", "root", "roasted_hazelnuts", T, "Roasted",
         "Roast mushrooms, hazelnuts or beechnuts on a Campfire",
         has("roasted_mushrooms", "roasted_hazelnuts", "roasted_beechnuts")),
        ("peeled_egg", "cooked", "peeled_egg", T, "Shell Game",
         "Boil an egg on the Campfire, then peel it in the crafting grid", has("peeled_egg")),
        ("cooked_bird", "cooked", "cooked_goose", T, "Game Bird",
         "Cook Raw Goose, Pheasant, Ptarmigan or Pigeon on a Campfire, or roast a whole Plucked bird",
         has("cooked_goose", "cooked_pheasant", "cooked_ptarmigan", "cooked_pigeon")),
        # fauna: river
        ("beaver_tail", "cooked", "roasted_beaver_tail", T, "Trapper's Treat",
         "Roast a Beaver Tail on the Campfire: fatty, rich and filling", has("roasted_beaver_tail")),
        ("nest_egg", "root", "goose_egg", T, "Nest Egg",
         "Take an egg from a wild Bird Nest (by rivers, in forests, on snow or bare rock). Leave one, or the birds may abandon it",
         has("#bird_eggs")),
        ("smoking_rack", "cooked", "smoking_rack", T, "Smokehouse",
         "Build a Smoking Rack (Sticks, Cordage) over or beside a burning Campfire", has("smoking_rack")),
        ("smoked", "smoking_rack", "smoked_meat", G, "Smoked",
         "Hang raw meat or fish on the Smoking Rack for 4 minutes. Keeps 30 days",
         has("smoked_meat", "smoked_fish", "smoked_haunch", "smoked_ribs", "smoked_loin")),
        ("dried", "root", "jerky", G, "Sun-Dried",
         "Put raw meat, fish, berries or mushrooms on a Drying Rack outdoors by day (faster in full sun). Keeps 30 days",
         has("#dried_meat", "dried_berries", "dried_mushrooms")),
        # woodmeat: each meat dries into its own thing (Preserving.DRYING, granted from RackFood)
        ("three_kinds_dried", "dried", "venison_jerky", G, "Larder of the North",
         "Take three different dried meats or fish off a Drying Rack: venison, beef, fowl, stockfish... Big cuts must be sliced into strips first",
         by_code()),
        ("spoiled", "root", "spoiled_food", T, "Gone Off",
         "Let food spoil. A Bark Basket or a sealed Storage Jar slows it down", has("spoiled_food")),
        ("wild_grain", "root", "einkorn_grain", T, "Wild Grains",
         "Cut Wild Einkorn or Wild Emmer with a knife, or pick Wild Peas", has("#grains", "peas")),
        ("flint_sickle", "wild_grain", "flint_sickle", T, "Sickle", "Tool Station: Flint + Twine + Stick",
         has("flint_sickle")),
        ("sow", "wild_grain", "minecraft:wooden_hoe", T, "Sower",
         "Till soil with a hoe and sow grain or peas on the farmland",
         placed("barley_crop", "einkorn_crop", "emmer_crop", "pea_crop", "minecraft:wheat")),
        ("sheaf", "sow", "einkorn_sheaf", T, "Harvest",
         "Harvest a ripe crop for a Sheaf. A sickle gives bonus sheaves and Straw", has("#sheaves")),
        ("flail", "sheaf", "flail", T, "Threshing",
         "Hold sheaves in the off hand and right-click a hard surface with a Flail for grain and Straw",
         has("flail")),
        ("straw_bed", "sheaf", "minecraft:straw_bed", G, "Rest Easy",
         "Craft a Straw Bed (3 Straw, 3 Planks) and right-click it to rest: sets spawn, keeps Phantoms away",
         has("minecraft:straw_bed")),
        ("porridge", "wild_grain", "porridge", G, "Porridge",
         "Mix 2 Grain, a Clay Water Bucket and a Bowl, then cook it on a Campfire", has("porridge")),
        ("wild_carrot", "root", "wild_carrot", T, "Wild Root",
         "Dig up a Wild Carrot (white flower umbels on meadows and plains). A knife or sickle gets the whole root",
         has("wild_carrot")),
        ("carrot", "wild_carrot", "minecraft:carrot", G, "Domesticated",
         "Sow Wild Carrots on farmland. Now and then a ripe harvest gives a real Carrot", has("minecraft:carrot")),
        ("sea_beet", "root", "minecraft:beetroot_seeds", T, "Shore Beet",
         "Cut Sea Beet on a beach or stony shore: Sea Beet Leaves and sometimes Beetroot Seeds",
         has("sea_beet_leaves", "minecraft:beetroot_seeds")),
        ("potato", "root", "minecraft:potato", G, "From Far Away",
         "Potatoes don't grow wild here. A few tribe elders trade seed potatoes for Leather and an Antler", has("minecraft:potato")),
        ("root_cellar", "spoiled", "root_cellar", T, "Root Cellar",
         "Build a Root Cellar (Planks, Straw) and dig it into the ground: roots keep 4x longer, other food 2x",
         has("root_cellar")),
        ("manure", "sow", "manure", T, "Manure",
         "Pick up Manure that livestock drop. Right-click farmland with it: fertility +2", has("manure")),
        ("compost", "manure", "compost", T, "Compost",
         "Build a Compost Heap (Sticks, Leaf Litter, a copper ingot) and fill it with plant waste", has("compost")),
        ("scarecrow", "sheaf", "scarecrow", T, "Scarecrow",
         "Build a Scarecrow (copper ingot bell, Carved Pumpkin, Straw, Raw Hide) to keep crows off the crops",
         has("scarecrow")),
        ("flour", "wild_grain", "flour", T, "Grind",
         "Build a Quern (Cobblestone, Stick, copper ingot axle) and grind Grain into Flour", has("flour")),
        ("dough", "flour", "dough", T, "Dough", "Mix 2 Flour and a Clay Water Bucket into Dough", has("dough")),
        ("bread", "dough", "minecraft:bread", G, "Daily Bread",
         "Bake Dough on a Baking Stone beside a Campfire, or in the kiln at 200°", has("minecraft:bread")),
        ("hearty_stew", "cooked", "stew", G, "Hearty Stew",
         "Simmer 4 or more ingredients in a Bronze Kettle of water over a fire, then ladle the stew out with a Bowl", by_code()),
        # fauna: giants
        ("giant_meat", "cooked", "cooked_mammoth", T, "Ice Age Feast",
         "Cook Mammoth, Rhino, Bison or Musk Ox meat on a Campfire", has("cooked_mammoth", "cooked_rhino", "cooked_bison", "cooked_musk_ox")),
        ("bison_tongue_roast", "giant_meat", "cooked_bison_tongue", T, "Delicacy",
         "Roast a Bison Tongue on a Campfire: the most filling cut of all", has("cooked_bison_tongue")),
        # end fauna: giants
        # fauna: forestbirds
        ("forest_fowl", "cooked_bird", "cooked_capercaillie", T, "Feast of the Forest",
         "Cook Raw Capercaillie or Raw Bustard on a Campfire, or roast a whole Plucked one",
         has("cooked_capercaillie", "cooked_bustard")),
        ("ground_nest", "nest_egg", "bustard_egg", T, "Ground Nest",
         "Take an egg from a Capercaillie nest on the pine-forest floor or a Great Bustard nest out in the grass",
         has("capercaillie_egg", "bustard_egg")),
        # fauna: coast
        ("sea_meat", "cooked", "cooked_seal_meat", T, "Sea Meat", "Cook Seal or Walrus meat on a Campfire",
         has("cooked_seal_meat", "cooked_walrus_meat")),
        ("caviar", "root", "sturgeon_roe", T, "Caviar", "Take the roe from a Sturgeon: little food, but very filling",
         has("sturgeon_roe")),
        # fauna: coldhunters
        ("taiga_fare", "cooked", "cooked_lynx", T, "Taiga Fare",
         "Cook the meat of a Lynx, Wolverine, Pine Marten, Ermine or Lemming on a Campfire",
         has("cooked_lynx", "cooked_wolverine", "cooked_marten", "cooked_ermine", "cooked_lemming")),
        # fauna: meadow
        ("small_game", "cooked", "cooked_wildcat", T, "Small Game",
         "Roast Wildcat Meat, a Mole, a Vole or Snake Meat on the Campfire",
         has("cooked_wildcat", "cooked_mole", "cooked_vole", "cooked_snake")),
    ]),
    "bronze_age": ("minecraft:block/cut_copper", [
        ("root", None, "malachite", T, "Bronze Age",
         "Look for green Malachite or Native Copper among loose rocks: copper ore lies below",
         has("malachite", "native_copper", "cassiterite", "minecraft:raw_copper")),
        ("raw_copper", "root", "minecraft:raw_copper", T, "Copper Ore",
         "Mine Copper Ore with a stone pickaxe for Raw Copper", has("minecraft:raw_copper")),
        ("pan", "root", "pan", T, "Prospector", "Craft a Pan from Planks and Twine", has("pan")),
        ("panning", "pan", "pan_with_gravel", T, "Panning",
         "Right-click Gravel or Sand with the Pan, then hold right-click in water for Cassiterite and copper",
         has("pan_with_gravel", "pan_with_sand")),
        ("stone_anvil", "root", "stone_anvil", T, "Stone Anvil",
         "Craft a Stone Anvil from rock chunks and Cobblestone, with the Flint Pickaxe", has("stone_anvil")),
        ("crushed", "stone_anvil", "crushed_copper_ore", T, "Crushed Ore",
         "Put ore on the Stone Anvil and strike it 4 times with a pickaxe as hammerstone",
         has("crushed_copper_ore", "crushed_tin_ore")),
        ("concentrate", "crushed", "copper_concentrate", T, "Concentrate",
         "Hold an empty Pan and crushed ore in the other hand, use it in water. Worth double in the crucible",
         has("copper_concentrate", "tin_concentrate")),
        ("tuyere", "root", "clay_tuyere", T, "Tuyere",
         "Shape 3 Clay Balls into an Unfired Clay Tuyere and fire it in the kiln (600°)", has("clay_tuyere")),
        ("bellows", "tuyere", "bellows", G, "Bellows",
         "Craft Bellows, set it facing the kiln's firebox and pump it: +40° per click", has("bellows")),
        ("molds", "root", "clay_ingot_mold", T, "Mold Maker",
         "Shape a mold from Clay Balls at the Mold Bench and fire it in the kiln (600°)",
         has("clay_ingot_mold", "clay_pickaxe_head_mold", "clay_axe_head_mold", "clay_shovel_head_mold",
             "clay_hoe_head_mold", "clay_knife_blade_mold")),
        ("molten", "bellows", "molten_crucible", G, "Molten Copper",
         "Click copper ore onto a Crucible and melt it in the kiln: copper needs 1085°", has("molten_crucible")),
        ("copper_ingot", "molten", "minecraft:copper_ingot", T, "Pour",
         "Pour the molten crucible into a Clay Ingot Mold within 60 s", has("minecraft:copper_ingot")),
        ("rough_cast", "molten", "rough_cast_copper_pickaxe_head", T, "Cast",
         "Pour molten copper into a tool head mold and take out the Rough Cast head",
         has(*[f"rough_cast_copper_{p}" for p in ("pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade")])),
        ("whetstone", "rough_cast", "whetstone", T, "Whetstone",
         "Build a Whetstone from Sandstone, Sticks and Planks", has("whetstone")),
        ("sharpen", "whetstone", "copper_pickaxe_head", T, "Sharpen",
         "Right-click the Whetstone 4 times with a Rough Cast head",
         has(*[f"copper_{p}" for p in ("pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade")])),
        ("copper_pickaxe", "sharpen", "minecraft:copper_pickaxe", G, "Copper Pickaxe",
         "Tool Station: Copper Pickaxe Head + Cordage + Tool Rod. It mines Tin Ore", has("minecraft:copper_pickaxe")),
        ("copper_tools", "sharpen", "minecraft:copper_axe", T, "Coppersmith",
         "Assemble a Copper Axe, Shovel, Hoe or Knife at the Tool Station",
         has("minecraft:copper_axe", "minecraft:copper_shovel", "minecraft:copper_hoe", "copper_knife")),
        ("tin", "copper_pickaxe", "cassiterite", T, "Tin",
         "Mine Tin Ore with the Copper Pickaxe and crush the Cassiterite on the Stone Anvil", has("crushed_tin_ore")),
        ("bronze", "tin", "bronze_ingot", C, "Bronze!",
         "Melt about 9 parts copper and 1 part tin in a Crucible at 950° and cast it",
         has("bronze_ingot", *[f"rough_cast_bronze_{p}" for p in ("pickaxe_head", "axe_head", "shovel_head", "hoe_head",
                                                                   "knife_blade", "saw_blade", "plate")])),
        ("bronze_pickaxe", "bronze", "bronze_pickaxe", G, "Bronze Pickaxe",
         "Cast, sharpen and assemble a Bronze Pickaxe. It mines Iron Ore", has("bronze_pickaxe")),
        ("raw_iron", "bronze_pickaxe", "minecraft:raw_iron", C, "Toward Iron",
         "Mine Iron Ore with a Bronze Pickaxe. The Iron Age awaits", has("minecraft:raw_iron")),
        ("bronze_tools", "bronze", "bronze_axe", T, "Bronzesmith",
         "Assemble a Bronze Axe, Shovel, Hoe or Knife (the knife can butcher bears)",
         has("bronze_axe", "bronze_shovel", "bronze_hoe", "bronze_knife")),
        ("bronze_weapons", "bronze", "bronze_sword", G, "Bronze Blade",
         "Fire spear or sword molds, cast and sharpen, then assemble a Bronze Spear or Bronze Sword",
         has("bronze_spear", "bronze_sword")),
        ("bronze_arrows", "bronze", "bronze_arrow", T, "Bronze Tips",
         "Shape a Clay Arrowheads Mold (3 Clay Balls), cast bronze in it, grind the sprue into 4 Bronze Arrowheads and fletch Bronze Arrows: 3 damage, seldom break",
         has("bronze_arrow")),
        ("bronze_armor", "bronze", "bronze_helmet", G, "Bronze Plate",
         "Cast plates, sharpen them and sew a Bronze Helmet or Greaves at the Armor Station",
         has("bronze_helmet", "bronze_greaves")),
        ("furnace_bricks", "bronze", "furnace_bricks", T, "Firebrick",
         "Shape Clay Balls and Sand into Unfired Furnace Bricks, fire them in the kiln (600°) and put 4 together",
         has("furnace_brick", "furnace_bricks")),
        ("smelting_furnace", "furnace_bricks", "furnace_tap", G, "Smelting Furnace",
         "Build 3x3x3 Furnace Bricks with only the middle empty, a Furnace Chute on top, a Furnace Tap mid-bottom front. Throw ore and Charcoal onto the chute, light it",
         by_code()),
        ("tap_metal", "smelting_furnace", "clay_channel", C, "Tapped",
         "Sneak-use the tap to open it: molten metal fills Clay Channels downhill and runs into a Clay Mold. A quarter more metal per ore",
         by_code()),
        ("scoop_channel", "tap_metal", "crucible", T, "Skimming",
         "Use an empty Crucible on a Clay Channel with metal in it to scoop it out. Channels laid down a one-block step slope like rails",
         by_code()),
        ("kettle", "bronze", "bronze_kettle", G, "Kettle",
         "Shape a Clay Kettle Mold (28 Clay Balls) at the Mold Bench and fire it, pour 8 units of bronze into it and grind the casting on the Whetstone",
         has("bronze_kettle")),
        ("hide_glue", "kettle", "hide_glue", T, "Sticky Business",
         "Boil Raw Hide, Rabbit Hide or Fish Bones in the kettle. Hide Glue hafts tools in place of Cordage", has("hide_glue")),
        ("tanned", "kettle", "minecraft:leather", T, "Tanner",
         "Boil Bark with Scraped Hides in the kettle: 3 Leather from every 2 hides, rain or shine", by_code()),
        ("bronze_saw", "bronze", "bronze_saw", T, "Saw",
         "Cast a saw blade and assemble a Bronze Saw. Log + Saw makes 4 Planks", has("bronze_saw")),
        ("potters_wheel", "bronze_saw", "potters_wheel", T, "Potter's Wheel",
         "Build a Potter's Wheel (Planks, Bronze Saw, Bronze Ingot, Cobblestone)", has("potters_wheel")),
        ("storage_jar", "potters_wheel", "storage_jar", G, "Storage Jar",
         "Throw 4 Clay Balls on the wheel and fire the jar. Sealed, food spoils 4x slower", has("storage_jar")),
        ("water_jug", "potters_wheel", "water_jug", T, "Water Jug",
         "Throw 3 Clay Balls on the wheel and fire the jug. Holds 6 pours", has("water_jug")),
        ("flower_pot", "potters_wheel", "minecraft:flower_pot", T, "Potted",
         "Throw 2 Clay Balls on the wheel into an Unfired Flower Pot and fire it in the kiln (600°)", has("minecraft:flower_pot")),
        ("decorated_pot", "potters_wheel", "minecraft:decorated_pot", T, "Potter's Pride",
         "Throw 4 Clay Balls on the wheel into an Unfired Decorated Pot and fire it in the kiln (600°)", has("minecraft:decorated_pot")),
        ("sherd_pot", "decorated_pot", "minecraft:arms_up_pottery_sherd", G, "Storyteller",
         "Press a Pottery Sherd into a finished Unfired Decorated Pot while it is still on the wheel. Fired, the pot keeps the picture",
         by_code()),
        ("wooden_wheel", "bronze_saw", "wooden_wheel", T, "Wheelwright",
         "Craft a Wooden Wheel: Planks, Tool Rods, a Bronze Ingot hub and the Bronze Saw", has("wooden_wheel")),
        ("cart", "wooden_wheel", "cart", G, "On Wheels", "Build a Wheeled Cart from 2 Wooden Wheels, Planks and Tool Rods",
         has("cart")),
        ("full_cart", "cart", "raw_haunch", T, "Two for the Road",
         "Load two carcasses onto a Wheeled Cart. It holds 6 room: a light body takes 1, a medium 2, a large 3", by_code()),
        ("storage_cart", "cart", "minecraft:chest", T, "Storage Cart",
         "Right-click an empty Wheeled Cart with a Chest to mount it: 54 slots on wheels (sneak + right-click)", by_code()),
        ("rope_halter", "bronze", "rope_halter", T, "Rope Halter",
         "Craft 3 Cordage into a Rope Halter. Sneak up to a Wild Horse and feed it grain to earn its trust",
         has("rope_halter")),
        ("tame_horse", "rope_halter", "minecraft:saddle", G, "Horse Whisperer",
         "Halter a Wild Horse that trusts you and keep feeding it until it is tame", tamed("tame_horse")),
        ("saddle_blanket", "tame_horse", "hide_saddle_blanket", T, "Bareback No More",
         "Craft a Hide Saddle Blanket (3 Leather, 2 Cordage) for full speed and jumps", has("hide_saddle_blanket")),
    ]),
}

# fauna: waterfowl
TABS["wilderness"][1].extend([
    ("pluck_swan", "pluck_goose", "plucked_swan", T, "Swan Song",
     "Bring down a Mute Swan and pluck it: Down, a Swan Quill and a big Plucked Swan. Keep clear of its nest", has("plucked_swan")),
    ("down_tinder", "pluck_goose", "down", T, "Light as Down",
     "Pluck Down from a Mallard or Mute Swan. Laid in an unlit campfire, Fire Pit or kiln it is the best tinder", has("down")),
    ("heron_bill", "butcher", "heron_bill", T, "Patient Fisher",
     "Butcher a Grey Heron for its Heron Bill: a knife carves it into 2 Bone Fish Hooks", has("heron_bill")),
    ("plume_headdress", "heron_bill", "plume_headdress", T, "Among the Reeds",
     "Make a Plume Headdress from Heron Plumes, Down and Leather: herons and cranes let you come close", has("plume_headdress")),
    ("bone_flute", "butcher", "bone_flute", G, "First Music",
     "Carve a Hollow Bird Bone (swan, crane) or a Vulture Wing Bone into a Bone Flute. Playing it lifts the spirits around you", has("bone_flute")),
])
TABS["food"][1].extend([
    ("cooked_waterfowl", "cooked_bird", "cooked_swan", T, "Swan Feast",
     "Cook Raw Mallard, Swan, Heron or Crane on a Campfire, or roast the whole Plucked bird",
     has("cooked_mallard", "cooked_swan", "cooked_heron", "cooked_crane")),
    ("swan_egg", "nest_egg", "swan_egg", T, "Swan's Egg", "Take an egg from a Mute Swan's nest without getting chased off",
     has("swan_egg")),
])


# fauna: mountain
TABS["wilderness"][1].extend([
    ("ibex", "butcher", "ibex_horn", T, "Lord of the Peaks",
     "Get close to an Ibex on the peaks (it flees uphill, up the rock face) and butcher it for its Horns and pelt",
     has("ibex_horn", "raw_ibex_pelt")),
    ("horn_bow", "sinew_bow", "horn_bow", G, "Composite Bow",
     "Fletching Table: 2 Sinew + Sinew Bow + 2 Ibex Horns makes the Horn Bow: its arrows fly faster and hit harder", has("horn_bow")),
    ("fat_tanning", "scraped_hide", "chamois_leather", T, "Fat Tanning",
     "Work a raw Ibex, Saiga or Hyena pelt or a Raw Chamois Hide with a knife and fat (Tallow, Lard, Bear or Marmot Fat) in the grid",
     has("ibex_pelt", "saiga_pelt", "hyena_pelt", "chamois_leather")),
    ("chamois_boots", "fat_tanning", "chamois_boots", T, "Sure-Footed",
     "Sew 3 Chamois Leather and Sinew into Chamois Boots at the Armor Station: long falls hurt less", has("chamois_boots")),
    ("marmot", "snare", "marmot_pelt", T, "Sentinel",
     "Catch a Marmot before its whistle sends the colony - and every animal around - running", has("marmot_pelt", "marmot_fat")),
    ("saiga", "arrows", "saiga_horn", T, "Steppe Runner",
     "Bring down a Saiga, the most skittish game of the steppe (take a bow), and take its amber horns", has("saiga_horn", "raw_saiga_pelt")),
    ("horn_window", "saiga", "horn_window", T, "Amber Light",
     "Set Saiga Horns in a lattice of sticks: Horn Windows let the light in", has("horn_window")),
    ("cave_bear", "bear", "cave_bear_skull", C, "Cave Bear",
     "Wake a Cave Bear in its den under the mountains, survive, and butcher it with a Tusk Dagger or better: its skull is your trophy",
     has("cave_bear_skull", "raw_cave_bear_pelt")),
    ("cave_bear_pelt", "cave_bear", "cave_bear_pelt", T, "Ice Age Fur",
     "Dry the Raw Cave Bear Pelt on a tall Drying Rack outdoors by day (it takes the whole rack)", has("cave_bear_pelt")),
    ("cave_hyena", "raw_wolf_pelt", "hyena_fang", T, "Laughing Thieves",
     "Kill a Cave Hyena - they steal carcasses and harass you at night in packs - for its fangs and spotted pelt",
     has("hyena_fang", "raw_hyena_pelt")),
])
TABS["food"][1].extend([
    ("mountain_meat", "cooked", "cooked_ibex_meat", T, "Mountain Fare",
     "Cook Ibex, Chamois, Marmot, Saiga, Cave Bear or Hyena meat on a Campfire",
     has(*[f"cooked_{m}_meat" for m in ("ibex", "chamois", "marmot", "saiga", "cave_bear", "hyena")])),
    ("marmot_salve", "root", "marmot_salve", T, "Marmot Grease",
     "Mix 2 Marmot Fat with Juniper Berries into Marmot Salve: rub it on to heal", has("marmot_salve")),
])


# fauna: warm
TABS["bronze_age"][1].extend([
    ("tame_onager", "rope_halter", "pack_baskets", G, "Beast of Burden",
     "Win over a skittish Onager with feed, halter it and keep feeding it until it is tame", tamed("tame_onager")),
    ("pack_baskets", "tame_onager", "pack_baskets", T, "Pack Animal",
     "Sling Pack Baskets (2 Bark Baskets, Scraped Hide, Cordage) on a tame Onager: it carries your load", has("pack_baskets")),
])
TABS["wilderness"][1].extend([
    ("tortoise_shell", "butcher", "tortoise_shell", T, "Slow and Steady",
     "Butcher a Tortoise. Pulled into its shell it shrugs off most blows", has("tortoise_shell")),
    ("shell_rattle", "tortoise_shell", "shell_rattle", T, "Rattle",
     "Bind a Tortoise Shell with gravel inside onto a stick: held in hand it keeps predators off like a torch", has("shell_rattle")),
    ("lizard_tail", "root", "lizard_tail", T, "Left Holding the Tail",
     "Grab at a Desert Lizard: it sheds its tail and escapes", has("lizard_tail")),
    ("water_buffalo", "butcher", "buffalo_horn", C, "Herd Bull",
     "Bring down a Water Buffalo (its whole herd will charge you) and butcher it with a Tusk Dagger or better",
     has("buffalo_horn", "raw_buffalo_hide")),
    ("leopard", "butcher", "raw_leopard_pelt", C, "Tree Cat",
     "Kill a Leopard. It sleeps up in the trees by day and drops on its prey at dusk", has("raw_leopard_pelt")),
    ("leopard_rug", "leopard", "leopard_rug", T, "Rosettes",
     "Fat-tan 2 Raw Leopard Pelts with a knife and Tallow or Lard, then craft them with a Bone into a Leopard Rug",
     has("leopard_rug")),
    ("jungle_plumage", "butcher", "peacock_feather", T, "Jungle Plumage",
     "Pluck a Peacock for its eyed feathers, or a Hornbill for its casque", has("peacock_feather", "hornbill_casque")),
])
TABS["food"][1].extend([
    ("warm_fare", "cooked", "cooked_buffalo_meat", T, "Far Country Fare",
     "Cook the meat of an Onager, Tortoise, Water Buffalo, Tapir or Leopard, or a Lizard Tail, on a Campfire",
     has(*[f"cooked_{m}" for m in ("onager_meat", "tortoise_meat", "buffalo_meat", "tapir_meat", "leopard_meat", "lizard_tail")])),
    ("jungle_fowl_egg", "root", "jungle_fowl_egg", T, "First Chicken",
     "Pick up an egg a Jungle Fowl hen laid. Throw it: it may hatch a chick", has("jungle_fowl_egg")),
])
# end fauna: warm
# tribes that visit and settle (people/TribeVisits.java, Settlements.java; granted from Java)
TABS["wilderness"][1].extend([
    ("tribe_visit", "root", "minecraft:white_banner", T, "Guests at the Fire",
     "Keep your camp fire burning for 3 days and a tribe comes to see who you are. Right-click their leader to talk and trade",
     by_code()),
    ("tribe_guests", "tribe_visit", "minecraft:campfire", T, "A Night by the Fire",
     "When the leader asks at dusk, let the band stay the night. At dawn they leave you a gift", by_code()),
    ("tribe_neighbours", "tribe_guests", "hide_tent_slope", G, "Neighbours",
     "A tribe you hosted settles nearby and builds a camp of hide tents. Its traders walk over to you", by_code()),
    ("tribe_palisade", "tribe_neighbours", "sharpened_spruce_log", G, "Walls of Their Own",
     "A neighbouring camp gathers enough wood, stone and hides to grow, and raises a palisade", by_code()),
    ("tribe_friends", "tribe_visit", "hide_canvas", C, "Best of Friends",
     "Reach a standing of 80 with a tribe: trade with them, bring them food, let them stay by your fire", by_code()),
    # granted per climate from Java (people/TribeVisits.java, ClimateZone), criteria named after the climates
    ("tribe_climates", "tribe_village", "sharpened_bamboo_block", G, "Peoples of the World",
     "Walk into a Tribe Village in each climate: turf and bone huts in the snow, bark-roofed halls in the woods, "
     "mud brick on the savanna and in the desert, stilt houses in the jungle and swamps",
     AllOf({z: {"trigger": "minecraft:impossible"} for z in ("cold", "temperate", "warm", "jungle")})),
])

# bats and the Giant Deer's charge (wildlife/VanillaGame.java, GiantDeer.java; gen_wildtweaks.py)
TABS["wilderness"][1].extend([
    ("night_eye_salve", "butcher", "night_eye_salve", T, "Eyes of the Bat",
     "Grind 2 Bat Wings with Soot and Lard into Night-Eye Salve. Rub it under your eyes to see in the dark",
     {"used": {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": check_item("night_eye_salve")}}}}),
    ("giant_deer_charge", "ancient_oak", "minecraft:goat_horn", G, "Taking the Antlers",
     "Live through a Giant Deer's charge. When it lowers its head and stomps, jump aside",
     {"hurt": {"trigger": "minecraft:entity_hurt_player", "conditions": {"damage": {"source_entity": {"minecraft:entity_type": mid("giant_deer")}}}}}),
])
TABS["food"][1].extend([
    ("guano", "manure", "guano", T, "Bat Droppings",
     "Scrape Guano from under a bat roost in a cave. Right-click farmland with it: poor soil turns rich at once", has("guano")),
])

# thread sources (Spinning.java; gen_thread.py)
TABS["stone_age"][1].extend([
    ("nettle", "plant_fibre", "nettle_stalks", T, "Stinging Nettle",
     "Cut Stinging Nettle in forests and on riverbanks for Nettle Stalks. It stings bare legs: wear leggings or sneak through",
     has("nettle_stalks")),
    ("drop_spindle", "nettle", "drop_spindle", T, "Drop Spindle",
     "Craft a Drop Spindle: a Stick over a whorl of rock, Bone or Clay. It stays in the grid as a tool", has("drop_spindle")),
    ("spun_thread", "drop_spindle", "nettle_thread", G, "Spun Yarn",
     "Strip Nettle Stalks into Nettle Fiber with a knife, then spin it with the Drop Spindle into Nettle Thread", has("nettle_thread")),
    ("sinew_thread", "flint_knife", "sinew_thread", T, "Sinew Thread",
     "Split Sinew from a butchered animal with a knife into Sinew Thread: it sews Leather like any other thread", has("sinew_thread")),
])
TABS["bronze_age"][1].extend([
    ("flax", "root", "flax_seeds", T, "Flax",
     "Buy Flax Seeds from a village Farmer (trade level 3) and sow them on farmland", has("flax_seeds")),
    ("linen", "flax", "linen_thread", G, "Linen",
     "Ret 4 Flax Stalks in a Clay Water Bucket with a knife into Flax Fiber, then spin it on a Drop Spindle into Linen Thread",
     has("linen_thread")),
])

# more bronze tools (BronzeTools.java; gen_bronzetools.py)
TABS["bronze_age"][1].extend([
    ("bronze_bucket", "bronze", "bronze_bucket", T, "Bronze Bucket",
     "Cast a bucket in a Clay Bucket Mold, grind it and fit a Cordage bail at the Tool Station. It never cracks",
     has("bronze_bucket", "bronze_water_bucket", "bronze_lava_bucket", "bronze_milk_bucket")),
    ("bronze_lava", "bronze_bucket", "bronze_lava_bucket", G, "Hot Stuff",
     "Scoop up lava with a Bronze Bucket (Poor Bronze is too brittle for it)", has("bronze_lava_bucket")),
    ("bronze_sickle", "bronze", "bronze_sickle", T, "Reaper",
     "Assemble a Bronze Sickle: it cuts a 3x3 swath of ripe crops or grass in one stroke (sneak to cut one)", has("bronze_sickle")),
    ("bronze_shears", "bronze", "bronze_shears", T, "Shearer",
     "Cast Bronze Shears and grind them: shear sheep and keep leaves", has("bronze_shears")),
    ("bronze_hook", "bronze", "bronze_hook_fishing_rod", T, "Bronze Hook",
     "Cast 4 Bronze Fish Hooks in one mold, grind them apart and rig a Bronze Hook Fishing Rod: faster bites, better catches",
     has("bronze_hook_fishing_rod")),
    ("bronze_needle", "bronze", "bronze_needle", T, "Fine Needlework",
     "Cast 4 Bronze Needles in one mold. They sew at the Armor Station 8 times as long as a Bone Needle", has("bronze_needle")),
    ("bronze_tongs", "bronze", "bronze_tongs", G, "Tongs",
     "Assemble Bronze Tongs. Held in either hand, molten crucibles and fresh castings don't burn you", has("bronze_tongs")),
    ("bronze_chisel", "bronze", "bronze_chisel", T, "Stonemason",
     "Assemble a Bronze Chisel and right-click Stone, Stone Bricks or Sandstone with it to carve it one step further",
     has("bronze_chisel")),
])

# the Bloomery and wrought iron (Ironworking.java; gen_bloomery.py)
TABS["bronze_age"][1].extend([
    ("fire_brick", "raw_iron", "fire_brick", T, "Fire Brick",
     "Crush Brick on the Stone Anvil into Grog, mix Fire Clay from Clay Balls and Grog, and fire it in the kiln at 1100°",
     has("fire_brick")),
    ("bloomery", "fire_brick", "bloomery_hearth", G, "Bloomery",
     "Build a Bloomery: a Bloomery Hearth with two Bloomery Stacks on it and the top left open", by_code()),
    ("iron_bloom", "bloomery", "hot_iron_bloom", G, "Iron Bloom",
     "Smelt 4 Raw Iron with Charcoal in the Bloomery. It needs 1350°: pump the bellows into the hearth",
     has("hot_iron_bloom", "iron_bloom")),
    ("bronze_anvil", "bronze", "bronze_anvil", G, "Bronze Anvil",
     "Fire a Clay Anvil Mold (32 Clay Balls), pour a full crucible of bronze (10 units) into it and grind the casting",
     has("bronze_anvil")),
    ("smithing_hammer", "bronze", "bronze_smithing_hammer", T, "Smith",
     "Cast a hammer head, grind it and assemble a Bronze Smithing Hammer at the Tool Station", has("bronze_smithing_hammer")),
    ("wrought_iron", "iron_bloom", "wrought_iron_ingot", C, "Wrought Iron",
     "Hammer a hot Iron Bloom 8 times on a Bronze Anvil before it cools: Wrought Iron Ingots and Slag",
     has("wrought_iron_ingot")),
])

# ---------------------------------------------------------------- Hunting tab: built from the butchering yields in the Java
# code (Butchery's table and every Butchery.register(...) call), so new animals show up by themselves. One advancement per
# animal that needs every product its carcass gives (meat, hide/pelt/feathers, special parts), grouped under a few nodes.

JAVA = ROOT / "src/main/java"
VANILLA_LANG = json.loads(jar.read("assets/minecraft/lang/en_us.json").decode("utf-8")) \
    if "assets/minecraft/lang/en_us.json" in JAR_NAMES else {}
HELPERS = {"hide(": "raw_hide", "feathers(": "minecraft:feather", "FEATHERS": "minecraft:feather"}
HUNT_GROUPS = {  # entity id -> group; animals not listed land in "more_game" (birds by their feathers)
    "small_game": ["hare", "desert_hare", "squirrel", "hedgehog", "badger", "minecraft:rabbit", "ermine", "lemming",
                   "pine_marten", "marmot", "wild_cat", "mole", "field_vole", "adder", "grass_snake", "desert_lizard", "tortoise",
                   "minecraft:bat", "minecraft:fox", "minecraft:ocelot", "minecraft:armadillo", "minecraft:frog"],
    "deer": ["roe_deer", "red_deer", "reindeer", "moose", "giant_deer"],
    "big_game": ["aurochs", "wild_horse", "mouflon", "wild_boar", "minecraft:cow", "minecraft:mooshroom", "minecraft:pig",
                 "minecraft:sheep", "steppe_bison", "musk_ox", "ibex", "chamois", "saiga", "onager", "water_buffalo", "tapir",
                 "minecraft:camel", "minecraft:llama", "minecraft:trader_llama", "minecraft:goat", "minecraft:hoglin"],
    "ice_age": ["woolly_mammoth", "woolly_rhino"],
    "predators": ["grey_wolf", "cave_lion", "brown_bear", "minecraft:phantom", "wolverine", "lynx", "cave_bear", "cave_hyena", "leopard",
                   "minecraft:polar_bear", "minecraft:panda", "minecraft:wolf"],
    "waters": ["seal", "walrus", "pike", "sturgeon", "beaver", "otter", "minecraft:turtle"],
}
GROUP_INFO = {  # id -> (icon, title, description)
    "small_game": ("rabbit_scraps", "Small Game", "Butcher hares, squirrels and other small animals"),
    "deer": ("venison_scraps", "Deer and Elk", "Butcher the deer of the woods and the tundra"),
    "big_game": ("beef_scraps", "Big Game", "Butcher aurochs, wild horses, boars and wild sheep"),
    "predators": ("bear_claw", "Predators", "Butcher the hunters that hunt you"),
    "ice_age": ("mammoth_ivory", "Ice Age Giants", "Bring down the mammoth and the woolly rhinoceros"),
    "waters": ("blubber", "Shore and River", "Butcher seals, walruses and the great river fish"),
    "birds": ("minecraft:feather", "Fowl", "Pluck and butcher game birds"),
    "more_game": ("game_meat_scraps", "More Game", "Butcher the rest of the wild"),
}
CHALLENGE = {"giant_deer", "cave_lion", "brown_bear", "woolly_mammoth", "woolly_rhino", "walrus", "cave_bear"}


def _args(src, start):
    """The text between the parenthesis at src[start] and its match."""
    depth = 0
    for i in range(start, len(src)):
        if src[i] == "(":
            depth += 1
        elif src[i] == ")":
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
    return ""


FIELD_IDS = {}  # (ClassName, FIELD) and FIELD -> registered id (from `NAME = X.register...("id"` lines)


def _field_ids():
    import re
    if not FIELD_IDS:
        for f in sorted(JAVA.rglob("*.java")):
            for m in re.finditer(r'\b([A-Z][A-Z0-9_]+)\s*=\s*[\w.]+\.register\w*\(\s*"([a-z0-9_]+)"', f.read_text(encoding="utf-8")):
                FIELD_IDS[(f.stem, m.group(1))] = m.group(2)
                FIELD_IDS.setdefault(m.group(1), m.group(2))
    return FIELD_IDS


def _item(ref, here=""):
    """'Wildlife.SINEW' / 'SINEW' (a field of class `here`) / 'Items.BONE' -> item id if it exists, else None."""
    owner, _, field = ref.rpartition(".")
    if owner == "Items":
        cands = ["minecraft:" + field.lower()]
    else:
        ids = _field_ids()
        cands = [M + i for i in (ids.get((owner or here, field)), ids.get(field), field.lower()) if i]
    for i in cands:
        try:
            return check_item(i)
        except AssertionError:
            pass
    return None


def hunting_yields():
    """{entity id: [item ids]} from every Yield in the Java sources."""
    import re
    out = {}
    for f in sorted(JAVA.rglob("*.java")):
        src = f.read_text(encoding="utf-8")
        for m in re.finditer(r"(?:Map\.entry|Butchery\.register)\(", src):
            body = _args(src, m.end() - 1)
            if "Yield(" not in body:
                continue
            head, _, rest = body.partition("Yield(")
            ent = re.search(r"EntityTypes\.([A-Z_]+)|(?:(\w+)\.)?([A-Z][A-Z0-9_]+)(?:\.get\(\)|::get)", head)
            if not ent:
                continue
            if ent.group(1):
                eid = "minecraft:" + ent.group(1).lower()
            else:
                ids = _field_ids()
                eid = ids.get((ent.group(2) or f.stem, ent.group(3))) or ids.get(ent.group(3)) or ent.group(3).lower()
            items = []
            for h, i in HELPERS.items():
                if h in rest:
                    items.append(mid(i))
            for ref in re.findall(r"(?<![\w.])((?:[A-Z][A-Za-z]+\.)?[A-Z][A-Z0-9_]+)\b(?!\s*\()", rest):
                i = _item(ref, f.stem)
                if i and i not in items:
                    items.append(i)
            if items:
                out[eid] = items  # Butchery.register (later files) overrides the table, like in the game
    return out


def name_of(i, kind):
    ns, path = mid(i).split(":")
    key = f"{kind}.{ns}.{path}"
    old = json.loads(LANG.read_text(encoding="utf-8"))
    return old.get(key) or VANILLA_LANG.get(key) or path.replace("_", " ").title()


def hunting_rows():
    rows = [("root", None, "flint_spear", T, "Hunting",
             "Every animal gives its own meat, hide and parts: butcher one of each kind and collect them all",
             has(*[s for s in SCRAPS if s != "chicken_scraps"]))]
    yields = hunting_yields()
    group_of = {e: g for g, es in HUNT_GROUPS.items() for e in es}
    animals = {}
    for eid, items in sorted(yields.items()):
        g = group_of.get(eid) or ("birds" if mid("minecraft:feather") in items else "more_game")
        animals.setdefault(g, []).append((eid, items))
    for g in GROUP_INFO:
        if g not in animals:
            continue
        icon, title, desc = GROUP_INFO[g]
        rows.append((g, "root", icon, T, title, desc, has(*sorted({i for _, its in animals[g] for i in its[:1]}))))
        for eid, items in animals[g]:
            ns, path = mid(eid).split(":")
            egg = f"{ns}:{path}_spawn_egg"
            try:
                icon = check_item(egg)
            except AssertionError:
                icon = items[0]
            animal = name_of(eid, "entity")
            names = ", ".join(name_of(i, "item") for i in items)
            crit = AllOf({f"has_{n}": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": i}]}}
                          for n, i in enumerate(items)})
            rows.append((path, g, icon, C if path in CHALLENGE else T, animal, f"Butcher a {animal}: {names}", crit))
    return rows


TABS["hunting"] = ("minecraft:gui/advancements/backgrounds/husbandry", hunting_rows())


def build():
    out_root = D / "mechanical/advancement"
    lang = {}
    count = {}
    for tab, (background, rows) in TABS.items():
        out = out_root / tab
        if out.exists():
            shutil.rmtree(out)
        out.mkdir(parents=True)
        ids = set()
        for n, (aid, parent, icon, frame, title, desc, crit) in enumerate(rows):
            assert aid not in ids, f"duplicate {tab}/{aid}"
            assert (parent is None) == (n == 0), f"{tab}/{aid}: only the first row is the root"
            assert parent is None or parent in ids, f"{tab}/{aid}: parent {parent} must come earlier in the tab"
            assert frame in (T, G, C) and crit
            ids.add(aid)
            key = f"advancements.mechanical.{tab}.{aid}"
            lang[key + ".title"], lang[key + ".description"] = title, desc
            display = {"icon": {"id": check_item(icon)}, "title": {"translate": key + ".title"},
                       "description": {"translate": key + ".description"}, "frame": frame,
                       "show_toast": parent is not None, "announce_to_chat": False, "hidden": False}
            if parent is None:
                display["background"] = background
            adv = {"parent": f"mechanical:{tab}/{parent}"} if parent else {}
            adv |= {"criteria": dict(crit), "requirements": [[c] for c in crit] if isinstance(crit, AllOf) else [list(crit)],
                    "display": display}
            (out / f"{aid}.json").write_text(json.dumps(adv, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        count[tab] = len(rows)

    # hide the vanilla tabs (keep recipes/**)
    hidden = 0
    for name in sorted(JAR_NAMES):
        parts = name.split("/")
        if len(parts) == 5 and parts[:3] == ["data", "minecraft", "advancement"] and parts[3] in VANILLA_TABS \
                and name.endswith(".json"):
            p = D / "minecraft/advancement" / parts[3] / parts[4]
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(json.dumps({"neoforge:conditions": [{"type": "neoforge:never"}]}, indent=2) + "\n")
            hidden += 1
    assert hidden > 100, hidden

    old = json.loads(LANG.read_text(encoding="utf-8"))
    old = {k: v for k, v in old.items() if not k.startswith("advancements.mechanical.")} | lang
    LANG.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")
    print(f"advancements: {count} ({sum(count.values())}), vanilla hidden: {hidden}")


if __name__ == "__main__":
    build()
