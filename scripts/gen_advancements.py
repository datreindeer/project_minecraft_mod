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


def ate(*items):
    """Ate any one of the items."""
    return {f"ate_{n}": {"trigger": "minecraft:consume_item", "conditions": {"item": {"items": check_item(i)}}}
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


def in_biome(biome):
    """The player inside the biome (checked about once a second)."""
    return {"in_biome": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"biomes": mid(biome)}}}}}}


def tamed(entity):
    return {"tamed": {"trigger": "minecraft:tame_animal", "conditions": {"entity": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:entity_type": mid(entity)}}}}}


def stood_on(block, state=None):
    """The player standing on the block (checked about once a second)."""
    pred = {"blocks": mid(block), **({"state": state} if state else {})}
    return {"stood_on": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:stepping_on": {"block": pred}}}}}}


def below_y(y):
    """The player at or below height y (checked about once a second)."""
    return {"below": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this",
        "predicate": {"minecraft:location": {"position": {"y": {"max": y}}}}}}}}


def in_biome(biome):
    """The player inside the biome (checked about once a second)."""
    return {"in_biome": {"trigger": "minecraft:location", "conditions": {"player": {
        "type": "minecraft:entity_properties", "entity": "this", "predicate": {"minecraft:location": {"biomes": mid(biome)}}}}}}


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
        ("walking_staff", "tool_rod", "walking_staff", T, "Long Road",
         "Lash two Tool Rods together with Twine into a Walking Staff, like the caravan traders carry", has("walking_staff")),
        ("elders_staff", "walking_staff", "elders_staff", T, "Wisdom of Elders",
         "Crown a Walking Staff with a Bone and two Feathers: an Elder's Staff", has("elders_staff")),
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
         "Right-click the top of a solid block with Firewood to stack a pile, up to 3 blocks high. Any wood stacks on, mixed; an empty hand takes a piece back",
         by_code()),
        ("log_pile", "woodpile", "oak_rough_log", T, "Log Pile",
         "Right-click the ground with a Rough Log to start a log pile, any woods mixed. Round logs roll off a tall, narrow pile: "
         "build it wider to build it higher, or between walls", by_code()),
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
        ("five_kinds_dried", "drying_rack", "dried_peat", G, "Sun and Wind",
         "Take five different dried things off a Drying Rack: leather, jerky, dried berries, Adobe Bricks, Dried Peat, Dried Reed, "
         "Dried Kelp...", by_code()),
        ("bone_needle", "root", "bone_needle", T, "Needlework",
         "Carve Fish Bones, a Bone or a Horn into a Bone Needle with a knife", has("bone_needle")),
        ("leatherworking_bench", "leather", "leatherworking_bench", T, "Leatherworker",
         "Craft a Leatherworking Bench from Leather, a Bone Needle, Planks and a Crafting Table", has("leatherworking_bench")),
        ("leather_armor", "leatherworking_bench", "minecraft:leather_chestplate", T, "Hide Clothes",
         "Sew Leather and thread (wool, nettle, linen or sinew) into Leather armor at the Leatherworking Bench",
         has("minecraft:leather_helmet", "minecraft:leather_chestplate", "minecraft:leather_leggings",
             "minecraft:leather_boots")),
        ("leather_satchel", "leatherworking_bench", "leather_satchel", T, "Satchel",
         "Sew 4 Leather into a Leather Satchel: 9 slots that go with you", has("leather_satchel")),
        ("leather_pouch", "leather_satchel", "leather_pouch", T, "Pouch",
         "Tie a Leather Pouch: Cordage over Leather. 4 slots, worn at the hip in place of a quiver (B opens it)", has("leather_pouch")),
        ("worn_satchel", "leather_satchel", "leather_satchel", T, "Shoulder Bag",
         "Put the Leather Satchel in the Back slot next to your player in the inventory, then press B to open it anywhere",
         by_code()),
        ("worn_cloak", "leather_armor", "otter_fur_cape", T, "Cloaked",
         "Wear a cloak (Otter Fur Cape, Ermine Mantle or Glider Cloak) in the Cloak slot: warmth and armor over your armor",
         by_code()),
        ("travois", "scraped_hide", "travois", T, "Travois",
         "Lash 4 Tool Rods, 2 Cordage and a Scraped Hide into a Travois to drag big carcasses home", has("travois")),
        ("many_hands", "butcher", "cordage", T, "Many Hands",
         "Drag a big carcass together with a friend: both sneak + right-click it with an empty hand. Two pull faster than one",
         by_code()),
        ("moving_a_mountain", "many_hands", "mammoth_ivory", C, "Moving a Mountain",
         "Drag a Woolly Mammoth carcass home with four hunters. Fewer can only strain at it", by_code()),
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
         "Sew a Quiver at the Leatherworking Bench (2 Leather, thread) and wear it in the Hip slot: bows shoot from it, picked-up arrows go in",
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
        ("deep_cold", "fur_armor", "minecraft:deepslate", G, "The Cold Deep",
         "Go below Y 0. Caves get colder the deeper you go and freeze you near the bottom: wear furs and light a fire",
         below_y(0)),
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
         "Bring down a Woolly Mammoth and butcher it (Tusk Dagger or better) for its Ivory. The herd's matriarch warns once, then charges",
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
        ("crop_stakes", "sheaf", "crop_stakes", T, "Staked Field",
         "Craft Crop Stakes (Sticks, Twine) and put them on farmland. Right-click a ripe crop there to harvest and replant it",
         has("crop_stakes")),
        ("flail", "sheaf", "flail", T, "Threshing",
         "Hold sheaves in the off hand and right-click a hard surface with a Flail for grain and Straw",
         has("flail")),
        ("straw_bed", "sheaf", "minecraft:straw_bed", G, "Rest Easy",
         "Craft a Straw Bed (3 Straw, 3 Planks) and right-click it to rest: sets spawn, keeps Phantoms away",
         has("minecraft:straw_bed")),
        ("slept_in_straw", "straw_bed", "minecraft:straw_bed", G, "Night Spins By",
         "Lie down in a Straw Bed at night. Once every player is lying down, the night runs fast till morning",
         by_code()),
        ("porridge", "wild_grain", "porridge", G, "Porridge",
         "Mix 2 Grain, a Clay Water Bucket and a Bowl, then cook it on a Campfire", has("porridge")),
        ("square_meal", "porridge", "minecraft:bread", T, "Square Meal",
         "Eat Porridge, Bread, Broth or a Stew. Prepared meals keep you full far longer than raw food",
         ate("porridge", "minecraft:bread", "broth", "stew")),
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
        ("written_word", "bronze_tools", "minecraft:writable_book", G, "The Written Word",
         "Cut Sugar Cane into Paper with a Bronze Knife, bind a Book with Leather, and make a Book and Quill with Soot Ink "
         "(2 Soot + Tallow or Lard) and a Feather", has("minecraft:writable_book")),
        ("bronze_weapons", "bronze", "bronze_sword", G, "Bronze Blade",
         "Fire spear or sword molds, cast and sharpen, then assemble a Bronze Spear or Bronze Sword",
         has("bronze_spear", "bronze_sword")),
        ("bronze_arrows", "bronze", "bronze_arrow", T, "Bronze Tips",
         "Shape a Clay Arrowheads Mold (3 Clay Balls), cast bronze in it, grind the sprue into 4 Bronze Arrowheads and fletch Bronze Arrows: 3 damage, seldom break",
         has("bronze_arrow")),
        ("armor_station", "bronze", "armor_station", T, "Armor Station",
         "Cast and sharpen a Bronze Plate, then build an Armor Station from 2 Bronze Plates, a Bronze Ingot, Planks and a Crafting Table",
         has("armor_station")),
        ("bronze_armor", "armor_station", "bronze_helmet", G, "Bronze Plate",
         "Rivet Bronze Plates onto Leather with a pickaxe or hammer at the Armor Station: a Bronze Helmet or Greaves",
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
        ("yew_longbow", "bronze_saw", "yew_longbow", G, "Longbowman",
         "Saw a Yew Rough Log into a Yew Stave with the Bronze Saw, then string it at the Tool Station with Sinew and Leather. Slow to draw, hits hard",
         has("yew_longbow")),
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
        ("saddlebags", "tame_horse", "leather_satchel", T, "Saddlebags",
         "Right-click a tamed horse with a Leather Satchel. Two fit, one on each side; sneak + right-click to pack them, shears take them off",
         by_code()),
        ("leather_barding", "tame_horse", "minecraft:leather_horse_armor", T, "Leather Barding",
         "Sew Leather Horse Armor at the Leatherworking Bench (7 Leather, 4 Thread) and dye it as you like", has("minecraft:leather_horse_armor")),
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


# fauna: underground (Cave Hyena and Cave Bear are the mountain fauna's; underground they come only in total darkness)
TABS["wilderness"][1].extend([
    ("giant_centipede", "deep_cold", "centipede_fang", T, "Look Up",
     "Kill a Giant Centipede in the dark below Y 0 - it clings to cave ceilings and drops on you - and butcher it for chitin, fangs and venom",
     has("centipede_chitin", "centipede_fang")),
    ("chitin_vest", "giant_centipede", "chitin_vest", T, "Plated",
     "Sew 7 Centipede Chitin and Sinew into a Chitin Vest: a chestplate long before bronze", has("chitin_vest")),
    ("centipede_venom", "giant_centipede", "centipede_venom_sac", T, "Venom of the Deep",
     "Ring a Centipede Venom Sac with 8 Arrows for 8 Arrows of Poison, or bind it with Lard and Plant Fibre into Snakebite Poultice",
     has("centipede_venom_sac")),
])
TABS["food"][1].extend([
    ("cooked_centipede", "cooked", "cooked_centipede", T, "Crunchy", "Cook a Raw Centipede on a Campfire", has("cooked_centipede")),
    ("cooked_folk", "cooked_centipede", "cooked_troll_meat", T, "Strange Meat",
     "Cook the meat of a Vittra, a Troll or a Lindworm on a Campfire", has("cooked_vittra_meat", "cooked_troll_meat", "cooked_lindworm_meat")),
])
# the folk of the dark (Nordic folklore): Vittra, Troll, Lindworm - light is the defence
TABS["wilderness"][1].extend([
    ("vittra", "deep_cold", "vittra_eye", T, "Hush",
     "Kill a Vittra below Y 0 - it hunts by sound: mining and sprinting call it, torchlight keeps it off - and butcher it for hide and eyes",
     has("vittra_hide", "vittra_eye")),
    ("vittra_hood", "vittra", "vittra_hood", T, "Unheard", "Sew 5 Vittra Hide into a Vittra Hood: no vittra hears you through it",
     has("vittra_hood")),
    ("vittra_eyes", "vittra", "minecraft:glass_bottle", T, "Eyes of the Dark",
     "Put a Vittra Eye in a Glass Bottle: a Potion of Night Vision",
     {"crafted": {"trigger": "minecraft:recipe_crafted", "conditions": {"recipes": "mechanical:night_vision_potion_from_vittra_eye"}}}),
    ("troll", "deep_cold", "troll_tusk", G, "Troll Slayer",
     "Kill a Troll below Y -30 - it shuns torchlight - and butcher it with a tusk-grade knife for its hide, hair and tusks",
     has("troll_hide", "troll_tusk")),
    ("troll_stone", "troll", "troll_stone", C, "Sunrise", "Lure a Troll out under the open sky by day: it turns to stone. Mine a Troll Stone",
     has("troll_stone")),
    ("trollhide_coat", "troll", "trollhide_coat", T, "Thick Skinned", "Sew 7 Troll Hide and Sinew into a Trollhide Coat", has("trollhide_coat")),
    ("troll_tusk_knife", "troll", "troll_tusk_knife", T, "Tusk and Twine",
     "Bind a Troll Tusk to a stick with Twine: a skinning knife that cuts the thickest hides", has("troll_tusk_knife")),
    ("lindworm", "troll", "lindworm_scale", C, "Wyrmslayer",
     "Slay a Lindworm in the deepest dark below Y -45 - fast, armoured, venomous - and claim the hoard it lay on",
     killed({"minecraft:entity_type": "mechanical:lindworm"})),
    ("lindworm_scale_armor", "lindworm", "lindworm_scale_chestplate", G, "Dragon Scale",
     "Make armour of Lindworm Scales", has("lindworm_scale_helmet", "lindworm_scale_chestplate", "lindworm_scale_leggings", "lindworm_scale_boots")),
    ("lindworm_fang_blade", "lindworm", "lindworm_fang_blade", T, "Fang Blade", "Bind two Lindworm Fangs to a stick: the Lindworm Fang Blade",
     has("lindworm_fang_blade")),
    ("lindworm_heart", "lindworm", "lindworm_heart", T, "Heart of the Wyrm", "Eat the Lindworm Heart: its strength becomes yours",
     ate("lindworm_heart")),
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
    # Trader Caravans (people/Caravans.java), in the Wandering Trader's place
    ("caravan", "root", "minecraft:chest", T, "The Caravan Has Come",
     "Talk to the old trader of a Trader Caravan: horses, covered carts and armed guards, now and then near you as the Wandering Trader once was",
     by_code()),
    ("caravan_gem", "caravan", "minecraft:diamond", G, "Treasures for Coin",
     "Sell a gemstone to a caravan's trader: they love gems and pay in silver and gold", by_code()),
    # the Miner under ground (people/Miners.java)
    ("miner_trade", "deep_cold", "minecraft:torch", T, "Deep Trade",
     "Trade with a Miner, a rare wanderer of the caves below Y 30, hunched under his ore sack. He sells the ores missing nearby",
     by_code()),
    # granted per climate from Java (people/TribeVisits.java, ClimateZone), criteria named after the climates
    ("tribe_climates", "tribe_village", "sharpened_bamboo_block", G, "Peoples of the World",
     "Walk into a Tribe Village in each climate: turf and bone huts in the snow, bark-roofed halls in the woods, "
     "mud brick on the savanna and in the desert, stilt houses in the jungle and swamps",
     AllOf({z: {"trigger": "minecraft:impossible"} for z in ("cold", "temperate", "warm", "jungle")})),
])
# cave biomes (docs/caves.md; gen_world_biomes.py, gen_caves.py): one per biome, the deep ones after The Cold Deep
TABS["wilderness"][1].extend([
    ("cave_limestone_caverns", "root", "limestone", T, "Limestone Caverns",
     "Find Limestone Caverns at middle depth under temperate land: limestone walls, travertine pools and thin stalactites", in_biome("limestone_caverns")),
    ("cave_salt_caves", "cave_limestone_caverns", "rock_salt", T, "Salt Caves",
     "Find the Salt Caves under hot, dry land: white gypsum walls hung with Rock Salt", in_biome("salt_caves")),
    ("cave_ochre_caves", "cave_limestone_caverns", "red_ochre_block", T, "Ochre Caves",
     "Find the Ochre Caves just below warm, dry land: red and yellow ochre in chalk, and paintings older than any tribe", in_biome("ochre_caves")),
    ("cave_soapstone_grottos", "cave_limestone_caverns", "soapstone", T, "Soapstone Grottos",
     "Find the Soapstone Grottos under cold land. Someone hid tools here once: look for their chests", in_biome("soapstone_grottos")),
    ("cave_glowworm_caves", "cave_limestone_caverns", "glowworm", T, "Glowworm Caves",
     "Find the Glowworm Caves under humid woods, lit faint blue by Glowworms on the ceiling", in_biome("glowworm_caves")),
    ("cave_ice_caves", "cave_limestone_caverns", "minecraft:packed_ice", T, "Ice Caves",
     "Find the Ice Caves under the frozen lands: packed and blue ice in slate", in_biome("ice_caves")),
    ("cave_peat_hollows", "cave_limestone_caverns", "peat", T, "Peat Hollows",
     "Find the Peat Hollows just below cool, wet land, where Bog Oak roots hang from the peat", in_biome("peat_hollows")),
    ("cave_bone_caves", "cave_limestone_caverns", "bone_pile", G, "Bone Caves",
     "Find the Bone Caves, the dens of cave bears and hyenas, littered with bones. Bring a spear", in_biome("bone_caves")),
    ("cave_flooded_caves", "cave_limestone_caverns", "minecraft:clay_ball", T, "Flooded Caves",
     "Find the Flooded Caves under the coasts: clay, gravel and water everywhere", in_biome("flooded_caves")),
    ("cave_galena_seams", "deep_cold", "raw_galena", T, "Galena Seams",
     "Find the Galena Seams deep down: shale and black alum shale, rich in galena", in_biome("galena_seams")),
    ("cave_magma_vents", "deep_cold", "scoria", G, "Magma Vents",
     "Find the Magma Vents near the bottom of the world under hot lands: basalt, scoria, magma and lava", in_biome("magma_vents")),
    ("cave_crystal_geodes", "deep_cold", "rough_rose_quartz", C, "Crystal Geodes",
     "Find the rare Crystal Geodes deep in the rock: quartzite caves full of gem crystals", in_biome("crystal_geodes")),
])
# cave environment (phase 9C; Temperature.java heat, Preserving.coldRate, SmokingRackBlock.peatSmoke): granted from Java
TABS["wilderness"][1].extend([
    ("cold_store", "cave_ice_caves", "minecraft:blue_ice", G, "Cold Store",
     "Leave food in a chest or barrel in the Ice Caves (or with ice right beside it) and open it again: in near-freezing air food spoils at a quarter of the speed, in cool air at half",
     by_code()),
    ("magma_vents_cool_off", "cave_magma_vents", "minecraft:water_bucket", G, "Hot Spring",
     "The Magma Vents' air overheats you: you tire, and above 19 on the thermometer you burn. Cool off in water there",
     by_code()),
])
TABS["food"][1].extend([
    ("peat_smoke", "smoked", "dried_peat", T, "Peat Smoke",
     "Feed a burning Campfire Peat or Dried Peat and smoke meat or fish over it: the thick peat smoke cures twice as fast as wood",
     by_code()),
])
# NPC villages rebuilt as their tribe goes up an age (people/AgeRebuild.java; granted from Java)
TABS["bronze_age"][1].extend([
    ("tribe_village_bronze", "root", "kiln", T, "Bronze Age Neighbours",
     "Come to a tribe's village or camp rebuilt for the Bronze Age: timber halls on stone footings, thatch, pens and a clay kiln",
     by_code()),
    ("tribe_village_iron", "tribe_village_bronze", "bronze_anvil", G, "Iron Age Neighbours",
     "Come to a tribe's village rebuilt for the Iron Age: taller halls and walls, and a smith at work", by_code()),
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
    ("soot_fertiliser", "manure", "soot", T, "Black Gold",
     "Scrape Soot off a sooty ceiling and right-click farmland with it: fertility +1, like Compost",
     used_on("soot", "minecraft:farmland")),
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
# seasons (Seasons.java): granted from Java
TABS["wilderness"][1].extend([
    ("first_frost", "root", "minecraft:snowball", T, "First Frost",
     "Live to see winter. Lakes freeze, snow falls where it rained, and the cold bites: wear fur and keep a fire burning",
     by_code()),
    ("through_winter", "first_frost", "minecraft:campfire", G, "Through the Winter",
     "Be there when winter turns to spring. Stock food before the cold comes: little grows and less is found in winter",
     by_code()),
    ("full_year", "through_winter", "minecraft:clock", C, "A Full Year",
     "Be there when a new year begins (1 Thaw)", by_code()),
    ("whiteout", "first_frost", "minecraft:powder_snow_bucket", G, "Whiteout",
     "Stand out in a snowstorm: a thunderstorm in winter snows hard and drifts snow deep, even under the trees",
     by_code()),
    # animals in winter (WinterAnimals.java)
    ("rude_awakening", "first_frost", "bear_claw", G, "Rude Awakening",
     "Wake a bear from its winter sleep. Bears hibernate in a den all winter, and a woken bear is an angry one",
     by_code()),
    ("cranes_return", "through_winter", "minecraft:feather", T, "The Cranes Return",
     "See the migratory birds back in spring. Cranes, Grey Herons, Geese and Mute Swans fly south in late autumn",
     by_code()),
])
# seasons for crops and berries (Farming.Climate seasons, WildBerryBushBlock.FROZEN): granted from Java
TABS["food"][1].extend([
    ("winter_berries", "root", "lingonberries", G, "Winter Larder",
     "Pick Lingonberries or Juniper Berries in winter. Other berry bushes freeze and give nothing until spring", by_code()),
    ("fallow_season", "root", "minecraft:wheat_seeds", T, "Out of Season",
     "Sneak-click a crop with an empty hand in winter: nothing grows in the fields until spring, so harvest and store before",
     by_code()),
])
# player tribes (clan/Clans.java): granted from Java
TABS["stone_age"][1].extend([
    ("found_tribe", "fire_pit", "fire_pit", T, "Hearth of the Tribe",
     "Press G by a Fire Pit and found a tribe: the pit becomes its centre and can't be broken. Members share their advancements",
     by_code()),
    ("grow_tribe", "found_tribe", "fire_pit", G, "Kin",
     "Invite another player into your tribe from the tribe page (G)", by_code()),
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
     has("bronze_bucket", "bronze_water_bucket", "bronze_milk_bucket")),
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
    ("bog_iron", "raw_iron", "raw_bog_iron", T, "Bog Iron",
     "Dig rusty Bog Iron Ore with a shovel from the floors of fen and swamp pools (sometimes river beds)",
     has("raw_bog_iron")),
    ("iron_bloom", "bloomery", "hot_iron_bloom", G, "Iron Bloom",
     "Smelt 4 Raw Iron or 6 Raw Bog Iron with Charcoal in the Bloomery. It needs 1350°: pump the bellows into the hearth",
     has("hot_iron_bloom", "iron_bloom")),
    ("bronze_anvil", "bronze", "bronze_anvil", G, "Bronze Anvil",
     "Fire a Clay Anvil Mold (32 Clay Balls), pour a full crucible of bronze (10 units) into it and grind the casting",
     has("bronze_anvil")),
    ("copper_coin", "bronze_anvil", "copper_coin", T, "Mint",
     "Lay a Copper Ingot on a Bronze Anvil and strike it 4 times with the Bronze Smithing Hammer: 4 Copper Coins, the money villagers take",
     has("copper_coin")),
    ("fire_grate", "bronze_anvil", "bronze_fire_grate", T, "Clean Burn",
     "Strike 3 Bronze Ingots 6 times on a Bronze Anvil and lay the grate in a Fire Pit or Clay Kiln: longer burn, far less smoke, Charcoal in the pit",
     has("bronze_fire_grate")),
    ("smithing_hammer", "bronze", "bronze_smithing_hammer", T, "Smith",
     "Cast a hammer head, grind it and assemble a Bronze Smithing Hammer at the Tool Station", has("bronze_smithing_hammer")),
    ("quarry_hammer", "smithing_hammer", "bronze_quarry_hammer", T, "Quarryman",
     "Assemble a Bronze Quarry Hammer (Bronze Hammer Head, Bronze Plate, Tool Rod): it breaks stone 3x3, ores crumble to stone",
     has("bronze_quarry_hammer")),
    ("wrought_iron", "iron_bloom", "wrought_iron_ingot", C, "Wrought Iron",
     "Hammer a hot Iron Bloom 8 times on a Bronze Anvil before it cools: Wrought Iron Ingots and Slag",
     has("wrought_iron_ingot")),
])

# ---------------------------------------------------------------- Iron Age tab (docs/ages/03-iron-age.md): starts where the
# Bronze Age's Wrought Iron leaves off. Every Iron Age feature adds its rows here.
TABS["iron_age"] = ("minecraft:block/iron_block", [
    ("root", None, "wrought_iron_ingot", T, "Iron Age",
     "Work a hot Iron Bloom into Wrought Iron on a Bronze Anvil: the Iron Age begins", has("wrought_iron_ingot")),
    ("roasted_ore", "root", "roasted_iron_ore", T, "Roasted",
     "Roast Raw Iron or Raw Bog Iron on a Campfire or Fire Pit: roasted ore blooms twice as rich (2 Wrought Iron instead of 1)",
     has("roasted_iron_ore", "roasted_bog_iron")),
    ("forged", "root", "iron_plate", G, "Forged",
     "Heat a Wrought Iron Ingot to 1100° in a Bloomery or Clay Kiln, choose a pattern (sneak-use the anvil with the hammer) "
     "and hammer it on a Bronze Anvil before it cools: Iron Plate, Tool Rod or a tool head", by_code()),
    ("iron_hammer", "forged", "iron_smithing_hammer", T, "Iron Hammer",
     "Forge an Iron Hammer Head and assemble an Iron Smithing Hammer (Leather, Tool Rod): it wears half as fast on hot metal",
     has("iron_smithing_hammer")),
    ("iron_tools", "forged", "minecraft:iron_pickaxe", G, "Ironsmith",
     "Assemble an iron tool at the Tool Station: forged head + Leather + Tool Rod. Iron Pickaxes mine gold, redstone and diamond",
     has("minecraft:iron_pickaxe", "minecraft:iron_axe", "minecraft:iron_shovel", "minecraft:iron_hoe", "minecraft:iron_sword",
         "minecraft:iron_spear", "iron_knife")),
    ("iron_armor", "forged", "minecraft:iron_chestplate", G, "Iron Clad",
     "Rivet Iron Plates onto leather at the Armor Station: the full iron armour, chestplate included",
     has("minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings", "minecraft:iron_boots")),
    ("charcoal_clamp", "root", "minecraft:charcoal", G, "Charcoal Burner",
     "Stack wood piles, cover them with earth on every side and on top, leave one side open and light it with the Fire Drill. "
     "After 5 minutes every piece of firewood is a Charcoal (a rough log two)", by_code()),
    ("cementation", "forged", "cemented_pot", G, "Cementation",
     "Shape a Cementation Pot from Fire Clay and fire it, pack it with 3 Wrought Iron, 2 Charcoal and Soot, and hold it at 1000° "
     "in a Bloomery for 6 minutes. Break it open: 3 Blister Steel", has("cemented_pot", "blister_steel")),
    ("steel", "cementation", "steel_ingot", C, "Steel",
     "Heat Blister Steel to forging heat and fold it with 6 blows of an Iron Smithing Hammer into a Steel Ingot: the end of the Iron Age",
     has("steel_ingot")),
    ("steel_tools", "steel", "steel_pickaxe", G, "Steelsmith",
     "Forge a steel head from hot Steel Ingots and fit it on a Tool Rod, wedged with an Iron Tool Rod. Steel mines obsidian",
     has("steel_pickaxe", "steel_axe", "steel_shovel", "steel_hoe", "steel_sword", "steel_spear", "steel_knife")),
    ("flint_and_steel", "steel", "minecraft:flint_and_steel", T, "Strike a Light",
     "Flint + Steel Plate: a Flint and Steel lights a fire every time", has("minecraft:flint_and_steel")),
    ("iron_bellows", "forged", "iron_bellows", T, "Iron Lungs",
     "Plate a Bellows with 2 Iron Plates and an Iron Tool Rod: it pumps twice as hard and the heat holds for 60 s after each pump",
     has("iron_bellows")),
    ("smithing_hearth", "forged", "smithing_hearth", G, "Smithing Hearth",
     "Build a Smithing Hearth (7 Fire Brick, an Iron Plate), stock it with Charcoal and light it: iron and steel in its coals heat up "
     "and stay hot as long as it burns", by_code()),
    ("steel_quarry_hammer", "steel", "steel_quarry_hammer", T, "Mountain Breaker",
     "Forge a Steel Hammer Head and assemble a Steel Quarry Hammer (Steel Plate, Iron Tool Rod): every ore a steel pickaxe mines crumbles in its 3x3 face",
     has("steel_quarry_hammer")),
    ("steel_armor", "steel", "steel_chestplate", G, "Steel Clad",
     "Rivet Steel Plates onto leather at the Armor Station: steel armour, between iron and diamond",
     has("steel_helmet", "steel_chestplate", "steel_leggings", "steel_boots")),
])

# stone families (StoneFamilies.java; gen_stone_families.py): one per Stone Age stone
TABS["stone_age"][1].extend([
    ("slate", "flint_pickaxe", "slate_tiles", T, "Slate Roof",
     "Mine Slate with a Flint Pickaxe, mostly in cold windswept hills and peaks. Polish it, then craft Slate Bricks into Slate Tiles: a roof that doesn't burn",
     has("slate", "slate_tiles")),
    ("gneiss", "flint_pickaxe", "gneiss_bricks", T, "Old Rock",
     "Mine banded Gneiss with a Bronze Pickaxe (flint only scratches it), mostly under mountains: Smooth Gneiss, Gneiss Bricks, mossy and cracked",
     has("gneiss", "gneiss_bricks")),
    ("chalk_block", "ochre", "chalk_block", T, "White Cliffs",
     "Press 4 Chalk into a Chalk Block, or find chalk beds near the surface by beaches and plains. Broken, it crumbles back into 4 Chalk",
     has("chalk_block", "cut_chalk")),
    ("soapstone", "flint_pickaxe", "polished_soapstone", T, "Soapstone",
     "Mine soft grey-green Soapstone with a Flint Pickaxe, mostly under mountains, and craft it into Polished Soapstone and Soapstone Bricks",
     has("soapstone", "polished_soapstone")),
    ("shale", "flint_pickaxe", "shale_bricks", T, "Shale",
     "Mine dark Shale with a Flint Pickaxe, low down under plains, swamps and rivers, and craft it into Polished Shale and Shale Bricks",
     has("shale", "shale_bricks")),
    ("conglomerate", "flint_pickaxe", "conglomerate_bricks", T, "Pudding Stone",
     "Mine pebbly Conglomerate with a Flint Pickaxe in river banks, and craft it into Polished Conglomerate and Conglomerate Bricks",
     has("conglomerate", "conglomerate_bricks")),
])

# stone families, phase 6: soft stones in the Stone Age tab, hard ones (Bronze Pickaxe) in the Bronze Age tab
TABS["stone_age"][1].extend([
    ("pumice", "flint_pickaxe", "pumice", T, "Pumice",
     "Mine light, porous Pumice with a Flint Pickaxe: deep down near lava (y -30 to 20), and higher up under the badlands",
     has("pumice", "pumice_bricks")),
    ("scoria", "flint_pickaxe", "scoria", T, "Scoria",
     "Mine red-black Scoria with a Flint Pickaxe: deep down near lava (y -30 to 20), and higher up under the badlands",
     has("scoria", "polished_scoria")),
    ("jasper_stone", "flint_pickaxe", "jasper_bricks", T, "Red Rock",
     "Mine red-brown Jasper Stone with a Flint Pickaxe, mostly under badlands and deserts (y 50 to 100), and craft Jasper Bricks",
     has("jasper_stone", "jasper_bricks")),
    ("gypsum", "flint_pickaxe", "cut_gypsum", T, "Gypsum",
     "Mine chalky Gypsum with a Flint Pickaxe under deserts and badlands (y 40 to 90), and cut it into Cut Gypsum",
     has("gypsum", "cut_gypsum")),
    ("alum_shale", "flint_pickaxe", "alum_shale_bricks", T, "Black Layers",
     "Mine near-black Alum Shale with a Flint Pickaxe, deep under cold taiga and snowy lands (y -30 to 40)",
     has("alum_shale", "alum_shale_bricks")),
])
TABS["bronze_age"][1].extend([
    ("marble", "bronze_pickaxe", "marble_pillar", T, "Marble",
     "Mine white Marble with a Bronze Pickaxe, deep under mountains and warm plateaus (y -20 to 40). The Bronze Chisel carves it into Chiseled Marble and Marble Pillars",
     has("marble", "marble_pillar", "chiseled_marble")),
    ("quartzite", "bronze_pickaxe", "quartzite_bricks", T, "Quartzite",
     "Mine pale, hard Quartzite with a Bronze Pickaxe under mountains (y 0 to 120)",
     has("quartzite", "quartzite_bricks")),
    ("porphyry", "bronze_pickaxe", "polished_red_porphyry", T, "Porphyry",
     "Mine Red or Green Porphyry with a Bronze Pickaxe: rare small blobs everywhere, red more often under badlands, green under windswept hills",
     has("red_porphyry", "green_porphyry")),
    ("rapakivi_granite", "bronze_pickaxe", "rapakivi_granite_bricks", T, "Rapakivi",
     "Mine spotted Rapakivi Granite with a Bronze Pickaxe, mostly under taiga, birch forests and snowy plains (y 0 to 80)",
     has("rapakivi_granite", "rapakivi_granite_bricks")),
    ("coastal_basalt", "bronze_pickaxe", "polished_coastal_basalt", T, "Basalt Columns",
     "Mine Coastal Basalt with a Bronze Pickaxe: dark columns stand on Stony Shores, and blobs lie under beaches",
     has("coastal_basalt", "polished_coastal_basalt")),
    ("serpentine", "bronze_pickaxe", "serpentine_bricks", T, "Serpentine",
     "Mine rare, mottled green Serpentine with a Bronze Pickaxe, mostly under windswept hills and stony peaks",
     has("serpentine", "serpentine_bricks")),
    ("alabaster", "bronze_pickaxe", "alabaster_pillar", T, "Alabaster",
     "Mine creamy Alabaster with a Bronze Pickaxe: small blobs in the gypsum under deserts and badlands (y 40 to 90)",
     has("alabaster", "alabaster_bricks", "alabaster_pillar")),
])

# stone crafts (StoneCrafts.java; gen_stone_crafts.py, world-materials plan phase 9A): each stone's real use
TABS["stone_age"][1].extend([
    ("pumice_polish", "pumice", "pumice_chunk", T, "Rubbed Smooth",
     "Crush Pumice on the Stone Anvil into a Pumice Chunk and right-click rough stone with it: Soapstone, Andesite, Tuff ... come out "
     "polished, no bronze needed. A chunk polishes 16 blocks", by_code()),
])
TABS["bronze_age"][1].extend([
    ("soapstone_molds", "molds", "soapstone_ingot_mold", T, "Stone That Remembers",
     "Carve a mold from Polished Soapstone at the Mold Bench: no firing, and it never cracks however often you cast",
     has(*[f"soapstone_{s}_mold" for s in ["ingot", "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "saw_blade", "plate", "spear_head", "sword_blade", "kettle", "arrowheads", "bucket", "sickle_blade", "shears", "fish_hooks", "needles", "tongs", "chisel_blade", "hammer_head", "anvil"]])),
    ("rubble", "stone_anvil", "minecraft:gravel", T, "Rubble",
     "Crush Conglomerate on the Stone Anvil into 2 Gravel and a Flint, or Shale into 2 Clay Balls", by_code()),
    ("basalt_anvil", "coastal_basalt", "basalt_anvil", T, "Heavy Stone",
     "Set a Stone Anvil on 3 Coastal Basalt: the heavy Basalt Anvil crushes in 2 blows instead of 4", has("basalt_anvil")),
    ("quartzite_whetstone", "quartzite", "quartzite_whetstone", T, "Hard Grit",
     "Build a Whetstone with Quartzite instead of Sandstone: it grinds a Rough Cast head in 2 strokes instead of 4",
     has("quartzite_whetstone")),
    ("rapakivi_quern", "rapakivi_granite", "rapakivi_quern", T, "Millstone",
     "Build a Quern from Rapakivi Granite: it grinds a Grain into Flour in 2 clicks instead of 4", has("rapakivi_quern")),
    ("alabaster_lamp", "alabaster", "alabaster_lamp", T, "Glowing Stone",
     "Carve an Alabaster Lamp (5 Alabaster, Plant Fibre), fill it with Tallow or Blubber and light it: the flame shines through the "
     "stone, light 15", placed("alabaster_lamp")),
])
TABS["iron_age"][1].extend([
    ("jasper_striker", "flint_and_steel", "rough_jasper", T, "Red Spark",
     "Jasper strikes sparks off steel like flint: Rough or Polished Jasper + Steel Plate make a Flint and Steel",
     {"crafted": {"trigger": "minecraft:recipe_crafted", "conditions": {"recipes": "mechanical:flint_and_steel_from_jasper"}}}),
])

# surface biomes (world-materials plan phases 4 and 7; gen_world_biomes.py): one exploration step per biome, saying what is there
TABS["wilderness"][1].extend([
    ("chalk_downs", "root", "chalk_block", T, "White Downs",
     "Find the Chalk Downs: open grass on white Chalk near the coast, flint lying in the turf, hares and sheep", in_biome("chalk_downs")),
    ("peat_bog", "chalk_downs", "peat", T, "Into the Bog",
     "Find a Peat Bog in the cool taiga: Peat under the moss, dark pools, Bog Iron in the pool floors, buried Bog Oak, Reed and cranes", in_biome("peat_bog")),
    ("slate_highlands", "chalk_downs", "slate_tiles", T, "Slate Highlands",
     "Find the Slate Highlands: cold windswept hills with Slate on every slope, Gneiss beneath, heather, mouflon and eagles", in_biome("slate_highlands")),
    ("soapstone_valley", "chalk_downs", "soapstone", T, "Soapstone Valley",
     "Find the rare Soapstone Valley in the temperate windswept forests: grey-green Soapstone lies in the open, soft enough for flint", in_biome("soapstone_valley")),
    ("limestone_karst", "chalk_downs", "limestone", T, "Karst",
     "Find a Limestone Karst: bare limestone pavement with thin grass and flowers, dry sinkholes, Travertine in the pools", in_biome("limestone_karst")),
    ("marble_gorge", "chalk_downs", "marble", T, "Marble Gorge",
     "Find a Marble Gorge on the warm plateaus: white Marble cliffs and Limestone. Marble needs a Bronze Pickaxe", in_biome("marble_gorge")),
    ("ochre_badlands", "chalk_downs", "red_ochre_block", T, "Painted Cliffs",
     "Find the Ochre Badlands: cliffs striped with Red and Yellow Ochre, Gypsum and Mudstone. Loose ochre lies on the red sand", in_biome("ochre_badlands")),
    ("basalt_coast", "chalk_downs", "coastal_basalt", T, "Basalt Coast",
     "Find a Basalt Coast: black Coastal Basalt columns standing in the sea, Beach Pebbles between them, seals on the rocks", in_biome("basalt_coast")),
    ("shingle_beach", "chalk_downs", "beach_pebbles", T, "Shingle",
     "Find a Shingle Beach on a cool coast: Beach Pebbles over gravel, loose Fieldstone and great glacial boulders", in_biome("shingle_beach")),
    ("reed_delta", "chalk_downs", "reed", T, "Reed Delta",
     "Find a Reed Delta where swamp meets the coast: dense Reed beds, mud banks, Clay, Loess and Mudstone, ducks and swans", in_biome("reed_delta")),
    ("glacial_moraine", "chalk_downs", "fieldstone", T, "Moraine",
     "Find a Glacial Moraine in the cold taiga: gravel ridges, Conglomerate, Fieldstone and boulders of Gneiss and Granite left by the ice",
     in_biome("glacial_moraine")),
    ("gypsum_flats", "chalk_downs", "gypsum", T, "White Flats",
     "Find the Gypsum Flats in the driest desert: a white crust of Gypsum and Rock Salt, Alabaster below", in_biome("gypsum_flats")),
])
TABS["stone_age"][1].extend([
    ("beach_pebbles", "root", "beach_pebbles", T, "Shingle Floor",
     "Dig Beach Pebbles with a shovel on stony shores, shingle beaches and basalt coasts, or mix 2 Gravel and 2 Loose Stones. Slabs make a pebble floor",
     has("beach_pebbles", "beach_pebble_slab")),
    ("fieldstone", "flint_pickaxe", "dry_stone_wall", T, "Dry Stone Wall",
     "Pick up Fieldstone lying on moraines and shingle (any pickaxe), or put 4 Loose Stones together. Six make 6 Dry Stone Wall: no mortar needed",
     has("fieldstone", "dry_stone_wall")),
])

# bog and earth (EarthBlocks.java; gen_earth.py, gen_stone_families.py): one per material family
TABS["stone_age"][1].extend([
    ("peat", "light_fire", "peat", T, "Peat Cutter",
     "Dig Peat with a shovel at the bottom of ponds and streams in swamps and taigas. It burns twice as long as firewood on a campfire "
     "and three minutes in the Clay Kiln; 4 Peat make 4 Peat Bricks", has("peat", "peat_bricks")),
    ("dried_peat", "peat", "dried_peat", T, "Turf Stack",
     "Cut a Peat block into 4 Cut Peat and dry them on a Drying Rack outdoors by day. Each Dried Peat burns as long as a firewood "
     "and as hot, so a block of peat gives twice the fire once dried", has("dried_peat")),
    ("bog_oak", "peat", "bog_oak_log", T, "Bog Oak",
     "Dig out a black Bog Oak trunk buried in the ground of swamps and taigas (y 55-62): a whole wood set without a tree", has("#bog_oak_logs")),
    ("turf", "root", "turf", T, "Sod Roof",
     "Bind Dirt with Plant Fibre into Turf, and cut it into slabs and stairs for a turf roof", has("turf", "turf_slab", "turf_stairs")),
    ("reed_thatch", "root", "reed_thatch_roof", T, "Thatcher",
     "Cut Reed by rivers and swamps (it grows like sugar cane), dry it on a Drying Rack and lay the Dried Reed as Reed Thatch "
     "or a sloped Reed Thatch Roof. It burns easily",
     has("reed_thatch", "reed_thatch_roof", "reed_thatch_slab", "reed_thatch_stairs")),
    ("adobe", "root", "adobe_bricks", T, "Adobe",
     "Find Mudstone on river banks in deserts, savannas and badlands and craft it into Adobe Bricks, or mix Clay, Sand and Straw "
     "into Wet Adobe Bricks and dry them on a Drying Rack: 4 Adobe Bricks make 4 blocks",
     has("mudstone", "adobe_bricks", "adobe_brick")),
    ("loess", "root", "loess", T, "Yellow Earth",
     "Dig pale Loess under the grass of plains and savannas. Tilled with a hoe it is the richest farmland; 4 make Loess Bricks",
     has("loess", "loess_bricks")),
    ("rammed_earth", "root", "rammed_earth", T, "Rammed Earth",
     "Ram Dirt or Loess, Gravel and a Clay Ball into 2 Rammed Earth: cheap, thick walls", has("rammed_earth")),
    ("wattle_and_daub", "rammed_earth", "framed_wattle_and_daub", T, "Wattle and Daub",
     "Weave sticks into Wattle, then daub it with 2 Clay Balls, Straw or Plant Fibre and Manure. Two sticks frame it as timber",
     has("wattle_and_daub", "framed_wattle_and_daub")),
    ("ochre_block", "ochre", "red_ochre_block", T, "Red Earth, Yellow Earth",
     "Press 4 Red or Yellow Ochre into a block, or dig ochre beds in clay banks, rivers, savannas and badlands. Broken, it gives the 4 back",
     has("red_ochre_block", "yellow_ochre_block")),
])
TABS["bronze_age"][1].extend([
    ("kaolin_tiles", "bellows", "kaolin_tiles", T, "White Tiles",
     "Dig rare white Kaolin with a shovel in forests, plains and jungles (it crumbles into 4 Kaolin Clay) and fire the block in the Clay Kiln "
     "at 1000°: Charcoal and a bellows. Kaolin Tiles", has("kaolin_tiles")),
])

# the Iron Age's new ores (IronOres.java; gen_iron_ores.py)
TABS["iron_age"][1].extend([
    ("galena", "root", "raw_galena", T, "Galena",
     "Mine Galena Ore with a Bronze Pickaxe: heavy grey lead ore, with a little silver in it", has("raw_galena")),
    ("lead", "galena", "lead_ingot", T, "Lead",
     "Smelt Raw Galena into a Lead Ingot in the Clay Kiln at 800°: Charcoal gets there without bellows", has("lead_ingot")),
    ("cupellation", "lead", "silver_nugget", G, "Cupellation",
     "Press a Lead Ingot into Bone Meal for a Leaded Cupel and fire it at 1000°: the lead soaks away and a Silver Nugget is left",
     has("silver_nugget")),
    ("silver_coin", "cupellation", "silver_coin", T, "Sterling",
     "Strike a Silver Ingot (9 Silver Nuggets) 4 times on a Bronze Anvil: 4 Silver Coins, each worth 10 Copper", has("silver_coin")),
    ("gold", "root", "minecraft:gold_ingot", T, "Gold",
     "Melt Raw Gold, or 9 Gold Nuggets panned from gravel and sand, into a Gold Ingot in the Clay Kiln at 1100°",
     has("minecraft:gold_ingot")),
    ("gold_coin", "gold", "gold_coin", G, "Treasury",
     "Strike a Gold Ingot 4 times on a Bronze Anvil: 4 Gold Coins, each worth 10 Silver", has("gold_coin")),
    ("flux", "root", "flux", T, "Flux",
     "Crush Limestone on the Stone Anvil into 2 Flux. Right-click the Bloomery Hearth with it: the next bloom gives one more Wrought Iron", has("flux")),
    ("limestone_bricks", "flux", "limestone_bricks", T, "Limestone Mason",
     "Carve Limestone with the Bronze Chisel, or craft 4 Polished Limestone into Limestone Bricks: slabs, stairs and walls like any stone",
     has("polished_limestone", "limestone_bricks")),
    ("salt", "root", "salt", T, "Salt of the Earth",
     "Mine Rock Salt with a Bronze Pickaxe, deep under deserts, badlands and savannas", has("salt")),
    ("salted", "salt", "salted_meat", T, "Salt Cured",
     "Salt meat or fish: it never spoils. A Scraped Hide and Salt make Leather without the drying rack",
     has("salted_meat", "salted_fish")),
])

# lime and mortar (world-materials-plan phase 2; IronOres.java, gen_lime.py, the lime stones in gen_stone_families.py)
TABS["iron_age"][1].extend([
    ("dolomite", "limestone_bricks", "dolomite_bricks", T, "Dolomite",
     "Mine Dolomite, found with Limestone and most in its cliffs, and craft it into Polished Dolomite and Dolomite Bricks. It crushes into Flux too",
     has("dolomite", "dolomite_bricks")),
    ("travertine", "limestone_bricks", "cut_travertine", T, "Travertine",
     "Mine honey-coloured Travertine under rivers, swamps and plains, and cut it into Cut Travertine",
     has("travertine", "cut_travertine")),
    ("shell_bank", "limestone_bricks", "cut_shell_bank", T, "Shell Bank",
     "Mine Shell Bank, stone of old seashells, under beaches and shores, and cut it into Cut Shell Bank",
     has("shell_bank", "cut_shell_bank")),
    ("quicklime", "limestone_bricks", "quicklime", T, "Quicklime",
     "Burn Limestone, Dolomite, a Chalk Block, Travertine or Shell Bank in the Clay Kiln at 900° (bellows): Quicklime",
     has("quicklime")),
    ("lime_mortar", "quicklime", "lime_mortar", G, "Lime Mortar",
     "Slake Quicklime with a bucket of water, mix it with Sand and Gravel: 4 Lime Mortar. 4 bricks' worth of stone and a Mortar lay 8 bricks",
     has("lime_mortar")),
    ("lime_plaster", "quicklime", "lime_plaster", T, "Lime Plaster",
     "Mix Slaked Lime, 2 Sand and Straw into 4 Lime Plaster. 8 around a pigment dye it white, black, red or yellow",
     has("lime_plaster")),
])

# fauna: night (the Gloson and the Mushroom Man, in the creeper's place)
TABS["wilderness"][1].extend([
    ("gloson", "butcher", "razor_ridge", C, "Razorback",
     "Kill a Gloson - the grey sow that circles you in the dark with glowing eyes and charges - and cut the Razor Ridge off its back",
     has("razor_ridge")),
    ("razor_knife", "gloson", "razor_knife", T, "Razor's Edge", "Lash a Razor Ridge to a Stick with Sinew: a Razor Knife",
     has("razor_knife")),
    ("bristle_brush", "gloson", "gloson_bristles", T, "Bristle Brush", "Bind Gloson Bristles to a Stick: a Paintbrush",
     {"crafted": {"trigger": "minecraft:recipe_crafted", "conditions": {"recipes": "mechanical:paintbrush_from_gloson_bristles"}}}),
    ("spore_sac", "butcher", "spore_sac", T, "Before It Bursts",
     "Kill a Mushroom Man before it swells and bursts into spores, and take its Spore Sac", has("spore_sac")),
    ("spore_bomb", "spore_sac", "spore_bomb", T, "Spore Bomb", "Wrap a Spore Sac in a Puffball: a Spore Bomb to throw", has("spore_bomb")),
    ("fungal_tinder", "spore_sac", "mushroom_flesh", T, "Fungal Tinder",
     "Lay Mushroom Flesh as tinder in an unlit fire: it catches like dry bark", has("mushroom_flesh")),
])
TABS["food"][1].extend([
    ("cooked_gloson", "cooked", "cooked_gloson", T, "Ghost Pork", "Cook Raw Gloson on a Campfire", has("cooked_gloson")),
])

# stone chemistry, writing and plaster (world-materials-plan phase 9B; Chemistry.java, gen_chemistry.py)
def crafted(*recipes):
    return {"crafted": {"trigger": "minecraft:recipe_crafted", "conditions": {"recipes": [f"mechanical:{r}" for r in recipes]}}}


TABS["iron_age"][1].extend([
    ("alum", "root", "alum", T, "Alum",
     "Roast Alum Shale in the Clay Kiln at 800° and leach it with a bucket of water: 2 Alum", has("alum")),
    ("mordant", "alum", "minecraft:red_dye", T, "Mordant",
     "Mix 2 pigment with an Alum: 4 dye, twice what the pigment gives alone",
     crafted(*(f"{c}_dye_from_{c}_pigment_with_alum" for c in ("black", "red", "yellow", "white")))),
    ("tawed", "alum", "minecraft:leather", T, "Tawed",
     "Taw 2 Scraped Hides with one Alum: 2 Leather", crafted("leather_from_alum")),
])
TABS["bronze_age"][1].extend([
    ("plaster_of_paris", "stone_anvil", "plaster_of_paris", T, "Plaster of Paris",
     "Crush Gypsum on the Stone Anvil and burn the powder in the Clay Kiln at 300°. With 2 Sand and Straw it makes 4 Lime Plaster, no bellows needed",
     has("plaster_of_paris")),
    ("slate_tablet", "bronze_tools", "slate_tablet", T, "Slate and Chalk",
     "Frame Polished Slate with 2 Sticks into a Slate Tablet. Write on it with Chalk in your other hand; sneak-use wipes it clean",
     has("slate_tablet")),
])

# weathering, mosaics and roof tiles (world-materials-plan phase 10; Masonry.java, gen_masonry.py)
TABS["bronze_age"][1].extend([
    ("weathered", "rubble", "mossy_limestone_bricks", T, "Weathered",
     "Bricks left out in the weather age: wet with rain or water they grow Mossy, through frosty winters they turn Cracked. "
     "Scrub the moss off with a Pumice Chunk", by_code()),
    ("mosaic", "marble", "warm_mosaic_tiles", T, "Mosaic",
     "Set four different polished stones together: 4 Mosaic Tiles. Warm (Jasper, Quartzite, Travertine, Yellow Ochre), cool "
     "(Slate, Serpentine, Marble, Soapstone) or checkered (Marble, Alabaster, Alum Shale, Coastal Basalt)",
     has("warm_mosaic_tiles", "cool_mosaic_tiles", "checkered_mosaic_tiles")),
    ("tiled_roof", "kaolin_tiles", "tiled_roof", T, "Tiled Roof",
     "Shape 2 Clay Balls into an Unfired Roof Tile and fire it in the Clay Kiln at 800°. 3 Fired Roof Tiles lay 4 Tiled Roof, "
     "a roof that never burns; pigment colours it", has(*[f"{c}tiled_roof" for c in ("", "red_", "black_", "yellow_", "white_")])),
])
TABS["iron_age"][1].extend([
    ("repointed", "lime_mortar", "cracked_limestone_bricks", T, "Repointed",
     "Right-click cracked bricks with Lime Mortar: the cracks are filled and the bricks are whole again", by_code()),
])

# the Wagon (WagonEntity.java, gen_wagon.py; docs/wagon-plan.md)
TABS["iron_age"][1].extend([
    ("iron_rimmed_wheel", "forged", "iron_rimmed_wheel", T, "Iron Shod",
     "Craft a Wooden Wheel with 2 Iron Plates: an Iron-Rimmed Wheel", has("iron_rimmed_wheel")),
    ("wagon", "iron_rimmed_wheel", "wagon", G, "Wagoner",
     "Build a Wagon: 4 Iron-Rimmed Wheels, 3 Planks, an Iron Plate and an Iron Tool Rod for the pole", has("wagon")),
    ("wagon_team", "wagon", "minecraft:lead", T, "A Pair in the Yoke",
     "Ride one tamed horse and lead another, then right-click the Wagon with an empty hand: both go in the yoke. One horse alone barely moves it",
     by_code()),
    ("full_wagon", "wagon_team", "raw_haunch", G, "Heavy Haul",
     "Load 12 room of carcasses on a Wagon with all four places empty: four large bodies, six medium or twelve light", by_code()),
    ("wagon_chests", "wagon", "minecraft:chest", T, "Freight",
     "Right-click each of a Wagon's four places with a Chest: 108 slots on wheels. Hay Bales feed the team, wooden slabs make seats",
     by_code()),
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
    "predators": ["grey_wolf", "cave_lion", "brown_bear", "minecraft:phantom", "wolverine", "lynx", "gloson", "cave_bear", "cave_hyena", "leopard",
                   "minecraft:polar_bear", "minecraft:panda", "minecraft:wolf"],
    "waters": ["seal", "walrus", "pike", "sturgeon", "beaver", "otter", "minecraft:turtle"],
    "underground": ["giant_centipede", "vittra", "troll", "lindworm"],
}
GROUP_INFO = {  # id -> (icon, title, description)
    "small_game": ("rabbit_scraps", "Small Game", "Butcher hares, squirrels and other small animals"),
    "deer": ("venison_scraps", "Deer and Elk", "Butcher the deer of the woods and the tundra"),
    "big_game": ("beef_scraps", "Big Game", "Butcher aurochs, wild horses, boars and wild sheep"),
    "predators": ("bear_claw", "Predators", "Butcher the hunters that hunt you"),
    "ice_age": ("mammoth_ivory", "Ice Age Giants", "Bring down the mammoth and the woolly rhinoceros"),
    "waters": ("blubber", "Shore and River", "Butcher seals, walruses and the great river fish"),
    "underground": ("centipede_chitin", "The Deep", "Butcher the creatures that breed in the dark under the earth"),
    "birds": ("minecraft:feather", "Fowl", "Pluck and butcher game birds"),
    "more_game": ("game_meat_scraps", "More Game", "Butcher the rest of the wild"),
}
CHALLENGE = {"giant_deer", "cave_lion", "brown_bear", "woolly_mammoth", "woolly_rhino", "walrus", "cave_bear", "troll", "lindworm"}


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
            # products are shared between animals: Butchery.awardButchered grants this one when this very animal is cut up
            crit["butchered"] = {"trigger": "minecraft:impossible"}
            rows.append((path, g, icon, C if path in CHALLENGE else T, animal, f"Butcher a {animal}: {names}", crit))
    return rows


TABS["hunting"] = ("minecraft:gui/advancements/backgrounds/husbandry", hunting_rows())


def gemstone_rows():
    """Gemstones tab from gem_table.py: a group per kind, one advancement per gem telling where its rough stone is found,
    then the Lapidary Wheel and its abrasives, hardest last."""
    from gem_table import GEMS, HARDNESS, VANILLA
    groups = {"ore": ("ore_gems", "rough_garnet", "Veins of Colour", "Mine a gem ore: each gem has its own depth and biome"),
              "crystal": ("crystal_gems", "rough_clear_quartz", "Crystal Caves", "Break a crystal growing on a cave floor or ceiling"),
              "pebble": ("pebble_gems", "rough_agate", "Lucky Pebbles", "Pick up a loose gem pebble from a cave floor")}
    rows = [("root", None, "rough_garnet", T, "Gemstones",
             "Find any Rough gem underground: gem ores, crystals in caves, or loose pebbles", has("#rough_gemstones"))]
    for kind, (gid, icon, title, desc) in groups.items():
        gems = [g for g in GEMS if g["kind"] == kind]
        rows.append((gid, "root", icon, T, title, desc, has(*[f"rough_{g['name']}" for g in gems])))
        rows += [(g["name"], gid, f"rough_{g['name']}", G if g.get("tier") or g["name"] == "moonstone" else T, g["title"], g["en"],
                  has(f"rough_{g['name']}")) for g in gems]
    rows.append(("old_favourites", "root", "rough_diamond", T, "Old Favourites",
                 "Diamond Ore, Emerald Ore and Amethyst Clusters now give Rough gems: cut them on a Lapidary Wheel",
                 has(*[f"rough_{v}" for v in VANILLA])))
    names = {"diamond": "Diamond Dust", "emerald": "Garnet Grit", "amethyst": "Flint Grit"}
    rows += [(f"cut_{v}", "old_favourites", item, G if v == "diamond" else T, f"Cut {t}", f"Cut a Rough {t} on the Lapidary Wheel with {names[v]}",
              has(item)) for v, (t, item, _) in VANILLA.items()]
    rows += [
        ("lapidary_wheel", "root", "lapidary_wheel", T, "Lapidary",
         "Build a Lapidary Wheel (Sandstone, Twine, a Tool Rod and planks). Right-click it with Sand, then 4 times with a soft Rough gem",
         has("lapidary_wheel")),
        ("first_cut", "lapidary_wheel", "amber", T, "First Facet", "Cut a gem: an abrasive at least as hard as the stone (Mohs) is needed",
         has("#gemstones")),
        ("flawed", "first_cut", "flawed_garnet", T, "Hairline Crack",
         "An abrasive only just hard enough can crack the stone into a Flawed gem. A harder one never does",
         has(*[f"flawed_{n}" for n in [g["name"] for g in GEMS] + list(VANILLA)])),
        ("flawless", "first_cut", "flawless_garnet", C, "Flawless",
         "Cut a Flawless gem: only a harder abrasive than the stone needs can, and fine rough stones from Large Crystals double the odds",
         has(*[f"flawless_{n}" for n in [g["name"] for g in GEMS] + list(VANILLA)])),
        ("flint_grit", "first_cut", "flint_grit", T, "Harder Than Sand",
         "Crush Flint on the Stone Anvil into Flint Grit: it cuts quartz, jade, peridot and the pebble gems", has("flint_grit")),
        ("garnet_grit", "flint_grit", "garnet_grit", T, "Garnet Paper",
         "Crush Rough Garnet on the Stone Anvil into Garnet Grit: it cuts garnet, topaz, tourmaline, aquamarine and emerald", has("garnet_grit")),
        ("corundum_grit", "garnet_grit", "corundum_grit", G, "Corundum",
         "Crush Rough Ruby or Sapphire into Corundum Grit: the only grit hard enough for ruby and sapphire", has("corundum_grit")),
        ("diamond_dust", "corundum_grit", "diamond_dust", G, "Diamond Cuts Diamond",
         "Crush a Rough Diamond into Diamond Dust: nothing else is hard enough to cut a diamond", has("diamond_dust")),
    ]
    rows.append(("all_gems", "first_cut", "opal", C, "Treasure Hoard", "Cut every kind of gemstone",
                 has_all(*[g["name"] for g in GEMS], *[item for _, item, _ in VANILLA.values()])))
    assert all(HARDNESS[g["name"]] for g in GEMS)
    return rows


TABS["gemstones"] = ("minecraft:gui/advancements/backgrounds/stone", gemstone_rows())


# ---------------------------------------------------------------- Folklore tab (docs/folklore-plan.md): Faith, Second Sight and
# the beings only believers see. Small Folk and Above the Clouds are granted from Java (folklore.Faith).
TABS["folklore"] = ("minecraft:block/moss_block", [
    ("root", None, "fly_agaric", T, "Folklore",
     "Eat a Fly Agaric, raw or boiled on a Campfire (Boiled Fly Agaric, no poison): Second Sight shows what is hidden",
     ate("fly_agaric", "boiled_fly_agaric")),
    ("small_folk", "root", "glimmering_stump", T, "Small Folk",
     "Find a Glimmering Stump in an old or dark forest and right-click it with berries or nuts. Come back the next morning", by_code()),
    ("above_the_clouds", "root", "mountain_watcher_spawn_egg", G, "Above the Clouds",
     "See a Mountain Watcher circling the peaks above height 150 on a clear day. Only believers (or Second Sight) see it", by_code()),
    ("gift_of_the_peaks", "above_the_clouds", "watcher_feather", C, "Gift of the Peaks",
     "Come close to a Mountain Watcher you can see: once a year it lets a Watcher Feather fall", has("watcher_feather")),
])


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
