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
        ("bark", "flint_axe", "bark", T, "Bark Peeler",
         "Hold right-click with an axe on a Fallen Log to peel off Bark (tinder, bast, baskets)", has("bark")),
        ("bark_basket", "bark", "bark_basket", T, "Gatherer's Basket",
         "Craft a Bark Basket for berries, mushrooms and hides. Food keeps longer inside", has("bark_basket")),
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
        ("campfire", "firewood", "minecraft:campfire", T, "Hearth",
         "Build a Campfire from Sticks and 3 Firewood. It is placed unlit", has("minecraft:campfire")),
        ("light_fire", "campfire", "fire_drill", G, "Let There Be Fire",
         "Right-click an unlit Campfire with the Fire Drill. Leaf Litter or Bark as tinder first helps it catch",
         used_on("fire_drill", "minecraft:campfire", {"lit": "true"})),
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
        ("crucible", "charcoal", "crucible", G, "Crucible",
         "Fire an Unfired Crucible (7 Clay Balls) in the kiln at 800°: needs Charcoal or Bellows. The key to bronze",
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
         "Sew Leather and Wool Thread (or String) into Leather armor at the Armor Station",
         has("minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings",
             "minecraft:leather_boots")),
        ("leather_satchel", "armor_station", "leather_satchel", T, "Satchel",
         "Sew 4 Leather into a Leather Satchel: 9 slots that go with you", has("leather_satchel")),
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
         "Hunt the rare Giant Deer for its Giant Antler", has("giant_antler")),
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
        ("nest_egg", "root", "goose_egg", T, "Nest Egg",
         "Take an egg from a wild Bird Nest (by rivers, in forests, on snow or bare rock). Leave one, or the birds may abandon it",
         has("#bird_eggs")),
        ("smoking_rack", "cooked", "smoking_rack", T, "Smokehouse",
         "Build a Smoking Rack (Sticks, Cordage) over or beside a burning Campfire", has("smoking_rack")),
        ("smoked", "smoking_rack", "smoked_meat", G, "Smoked",
         "Hang raw meat or fish on the Smoking Rack for 4 minutes. Keeps 30 days", has("smoked_meat", "smoked_fish")),
        ("dried", "root", "jerky", G, "Sun-Dried",
         "Put raw meat, fish, berries or mushrooms on a Drying Rack outdoors by day (faster in full sun). Keeps 30 days",
         has("jerky", "dried_fish", "dried_berries", "dried_mushrooms")),
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
        # fauna: coast
        ("sea_meat", "cooked", "cooked_seal_meat", T, "Sea Meat", "Cook Seal or Walrus meat on a Campfire",
         has("cooked_seal_meat", "cooked_walrus_meat")),
        ("caviar", "root", "sturgeon_roe", T, "Caviar", "Take the roe from a Sturgeon: little food, but very filling",
         has("sturgeon_roe")),
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
         "Shape Clay Balls into a mold and fire it in the kiln (600°)",
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
        ("bronze_armor", "bronze", "bronze_helmet", G, "Bronze Plate",
         "Cast plates, sharpen them and sew a Bronze Helmet or Greaves at the Armor Station",
         has("bronze_helmet", "bronze_greaves")),
        ("furnace_bricks", "bronze", "furnace_bricks", T, "Firebrick",
         "Shape Clay Balls and Sand into Unfired Furnace Bricks, fire them in the kiln (600°) and put 4 together",
         has("furnace_brick", "furnace_bricks")),
        ("smelting_furnace", "furnace_bricks", "furnace_tap", G, "Smelting Furnace",
         "Build 3x3x3 Furnace Bricks around an open shaft, a Furnace Tap mid-bottom front. Drop ore and Charcoal in, light it",
         by_code()),
        ("tap_metal", "smelting_furnace", "clay_channel", C, "Tapped",
         "Sneak-use the tap to open it: molten metal runs along Clay Channels into a Clay Mold. A quarter more metal per ore",
         by_code()),
        ("kettle", "bronze", "bronze_kettle", G, "Kettle",
         "Fire a Clay Kettle Mold (5 Clay), pour 8 units of bronze into it and grind the casting on the Whetstone",
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
        ("wooden_wheel", "bronze_saw", "wooden_wheel", T, "Wheelwright",
         "Craft a Wooden Wheel: Planks, Tool Rods, a Bronze Ingot hub and the Bronze Saw", has("wooden_wheel")),
        ("cart", "wooden_wheel", "cart", G, "On Wheels", "Build a Wheeled Cart from 2 Wooden Wheels, Planks and Tool Rods",
         has("cart")),
        ("rope_halter", "bronze", "rope_halter", T, "Rope Halter",
         "Craft 3 Cordage into a Rope Halter. Sneak up to a Wild Horse and feed it grain to earn its trust",
         has("rope_halter")),
        ("tame_horse", "rope_halter", "minecraft:saddle", G, "Horse Whisperer",
         "Halter a Wild Horse that trusts you and keep feeding it until it is tame", tamed("tame_horse")),
        ("saddle_blanket", "tame_horse", "hide_saddle_blanket", T, "Bareback No More",
         "Craft a Hide Saddle Blanket (3 Leather, 2 Cordage) for full speed and jumps", has("hide_saddle_blanket")),
    ]),
}


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
            adv |= {"criteria": crit, "requirements": [list(crit)], "display": display}
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
