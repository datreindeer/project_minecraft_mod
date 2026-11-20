# Gemstones: one table shared by gen_gemstones.py, gen_gemstone_textures.py and gen_advancements.py.
# Keep the names in step with Gemstones.java (it registers the blocks and items).
#
# kind  ore:     full ore blocks in veins; "hosts" = the rock the ore sits in (stone / deepslate / sandstone)
#       crystal: small / medium / large crystals growing on cave floors and ceilings
#       pebble:  loose gem pebbles (sizes 1-3) lying on cave floors
# biomes: biome ids and #tags (become the biome tag mechanical:has_gem/<gem>); y: height range of the feature
# count: tries per chunk; size: ore vein size; tier "bronze": needs a Bronze Pickaxe; pal: dark -> light colours

OVERWORLD = ["#minecraft:is_overworld"]
SNOWY = ["minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:snowy_taiga", "minecraft:snowy_slopes",
         "minecraft:grove", "minecraft:frozen_peaks", "minecraft:jagged_peaks", "minecraft:snowy_beach"]
WINDSWEPT = ["minecraft:windswept_hills", "minecraft:windswept_gravelly_hills", "minecraft:windswept_forest",
             "minecraft:windswept_savanna", "minecraft:stony_peaks"]

GEMS = [
    # ---- ore gems ----
    dict(name="garnet", title="Garnet", kind="ore", hosts=["stone", "deepslate"], y=(-32, 64), biomes=OVERWORLD, count=6, size=6,
         pal=[(52, 6, 14), (96, 14, 26), (146, 26, 40), (196, 52, 62), (240, 132, 136)],
         sv="Mörkröd granat. Vanlig i hela övervärlden, y −32 till 64", en="Dark red garnet. Common all over the Overworld, y −32 to 64"),
    dict(name="ruby", title="Ruby", kind="ore", hosts=["deepslate"], y=(-64, -16), biomes=OVERWORLD, count=4, size=4, tier="bronze",
         pal=[(70, 0, 20), (140, 6, 40), (208, 20, 60), (246, 70, 104), (255, 186, 204)],
         sv="Sällsynt, bara djupt i deepslate (y −64 till −16)", en="Rare, only deep in deepslate (y −64 to −16)"),
    dict(name="sapphire", title="Sapphire", kind="ore", hosts=["stone", "deepslate"], y=(-48, 32), biomes=["#minecraft:is_mountain"],
         count=7, size=5, tier="bronze", pal=[(8, 16, 70), (16, 38, 140), (34, 72, 204), (84, 130, 240), (186, 214, 255)],
         sv="Bara under berg (bergsbiomer), y −48 till 32", en="Only under mountains (mountain biomes), y −48 to 32"),
    dict(name="topaz", title="Topaz", kind="ore", hosts=["stone"], y=(0, 80), biomes=["#minecraft:is_badlands"], count=8, size=6,
         pal=[(110, 52, 6), (176, 96, 14), (226, 148, 34), (250, 196, 86), (255, 238, 176)],
         sv="Gyllene topas under badlands, y 0 till 80", en="Golden topaz under the badlands, y 0 to 80"),
    dict(name="jade", title="Jade", kind="ore", hosts=["stone"], y=(16, 100), biomes=["#minecraft:is_jungle"], count=7, size=7,
         pal=[(10, 52, 30), (24, 96, 56), (52, 146, 88), (110, 194, 132), (196, 236, 204)],
         sv="Grön jade under djungeln, y 16 till 100", en="Green jade under the jungle, y 16 to 100"),
    dict(name="opal", title="Opal", kind="ore", hosts=["sandstone"], y=(30, 100), biomes=["minecraft:desert"], count=12, size=6,
         pal=[(150, 176, 196), (190, 210, 222), (226, 236, 242), (248, 228, 236), (255, 255, 255)],
         sv="Sitter i sandsten under öknen, y 30 till 100", en="Sits in sandstone under the desert, y 30 to 100"),
    dict(name="turquoise", title="Turquoise", kind="ore", hosts=["stone"], y=(32, 120), biomes=["#minecraft:is_savanna"], count=7, size=6,
         pal=[(10, 76, 80), (20, 128, 130), (44, 182, 176), (110, 220, 206), (200, 248, 238)],
         sv="Turkos under savannen, y 32 till 120", en="Turquoise under the savanna, y 32 to 120"),
    dict(name="peridot", title="Peridot", kind="ore", hosts=["deepslate"], y=(-64, -16), biomes=["#minecraft:is_deep_ocean"],
         count=7, size=5, tier="bronze", pal=[(40, 62, 6), (80, 116, 12), (126, 170, 24), (176, 214, 60), (226, 248, 150)],
         sv="Djupt i deepslate under djuphavet, y −64 till −16", en="Deep in deepslate under the deep ocean, y −64 to −16"),
    dict(name="amber", title="Amber", kind="ore", hosts=["stone"], y=(32, 80), biomes=["#minecraft:is_forest"], count=6, size=5,
         pal=[(80, 34, 4), (140, 66, 8), (198, 108, 16), (236, 160, 40), (255, 214, 120)],
         sv="Fossil kåda under skogar, y 32 till 80", en="Fossil resin under forests, y 32 to 80"),
    # ---- crystal gems ----
    dict(name="clear_quartz", title="Clear Quartz", kind="crystal", y=(-16, 48), biomes=OVERWORLD, count=10,
         pal=[(150, 160, 170), (190, 198, 206), (222, 228, 234), (240, 244, 248), (255, 255, 255)],
         sv="Bergkristall. Växer i grottor i hela övervärlden, y −16 till 48", en="Rock crystal. Grows in caves all over the Overworld, y −16 to 48"),
    dict(name="rose_quartz", title="Rose Quartz", kind="crystal", y=(0, 56),
         biomes=["minecraft:cherry_grove", "minecraft:meadow", "minecraft:flower_forest"], count=16,
         pal=[(150, 80, 100), (200, 120, 142), (232, 160, 180), (248, 200, 214), (255, 236, 242)],
         sv="I grottor under Cherry Grove, Meadow och Flower Forest, y 0 till 56",
         en="In caves under Cherry Grove, Meadow and Flower Forest, y 0 to 56"),
    dict(name="citrine", title="Citrine", kind="crystal", y=(-16, 56), biomes=["minecraft:dripstone_caves"], count=24,
         pal=[(140, 80, 6), (200, 130, 14), (238, 180, 36), (252, 216, 90), (255, 244, 180)],
         sv="Bara i Dripstone Caves", en="Only in Dripstone Caves"),
    dict(name="smoky_quartz", title="Smoky Quartz", kind="crystal", y=(-32, 56), biomes=SNOWY, count=14,
         pal=[(36, 28, 24), (66, 54, 46), (102, 86, 74), (146, 128, 112), (204, 190, 176)],
         sv="I grottor under snöiga biomer, y −32 till 56", en="In caves under snowy biomes, y −32 to 56"),
    dict(name="aquamarine", title="Aquamarine", kind="crystal", y=(-16, 40),
         biomes=["#minecraft:is_ocean", "#minecraft:is_river", "#minecraft:is_beach"], count=12,
         pal=[(30, 100, 120), (60, 150, 170), (104, 196, 210), (160, 228, 236), (220, 250, 252)],
         sv="I grottor under hav, floder och stränder, y −16 till 40", en="In caves under oceans, rivers and beaches, y −16 to 40"),
    dict(name="fluorite", title="Fluorite", kind="crystal", y=(-48, 16), biomes=OVERWORLD, count=7,
         pal=[(50, 20, 90), (90, 40, 150), (110, 120, 200), (90, 200, 170), (200, 255, 230)],
         sv="Lila-grön fluorit, djupt i hela övervärlden, y −48 till 16", en="Purple-green fluorite, deep all over the Overworld, y −48 to 16"),
    dict(name="celestine", title="Celestine", kind="crystal", y=(-16, 56), biomes=["minecraft:lush_caves"], count=24,
         pal=[(60, 90, 150), (100, 136, 196), (144, 178, 228), (190, 214, 246), (236, 244, 255)],
         sv="Bara i Lush Caves", en="Only in Lush Caves"),
    dict(name="moonstone", title="Moonstone", kind="crystal", y=(-64, 0), biomes=["minecraft:deep_dark"], count=22, light=(3, 5, 7),
         pal=[(110, 120, 150), (160, 170, 200), (204, 212, 236), (232, 238, 252), (255, 255, 255)],
         sv="Lyser svagt. Bara i Deep Dark", en="Glows faintly. Only in the Deep Dark"),
    dict(name="tourmaline", title="Tourmaline", kind="crystal", y=(-48, 32), biomes=WINDSWEPT, count=12,
         pal=[(20, 60, 30), (40, 110, 60), (200, 70, 110), (240, 120, 150), (255, 200, 210)],
         sv="Melon-turmalin, i grottor under Windswept-biomer och Stony Peaks, y −48 till 32",
         en="Watermelon tourmaline, in caves under windswept biomes and Stony Peaks, y −48 to 32"),
    # ---- pebble gems ----
    dict(name="agate", title="Agate", kind="pebble", y=(0, 64), biomes=OVERWORLD, count=5,
         pal=[(90, 50, 30), (150, 90, 60), (200, 150, 110), (230, 210, 190), (250, 246, 240)],
         sv="Randig agat, ligger på grottgolv i hela övervärlden, y 0 till 64", en="Banded agate, lies on cave floors all over the Overworld, y 0 to 64"),
    dict(name="jasper", title="Jasper", kind="pebble", y=(-16, 80), biomes=["#minecraft:is_badlands", "minecraft:desert"], count=10,
         pal=[(70, 16, 10), (120, 30, 18), (166, 52, 30), (200, 90, 56), (232, 150, 110)],
         sv="Röd jaspis på grottgolv under badlands och öken, y −16 till 80", en="Red jasper on cave floors under badlands and desert, y −16 to 80"),
    dict(name="carnelian", title="Carnelian", kind="pebble", y=(0, 64), biomes=["#minecraft:is_savanna"], count=10,
         pal=[(110, 30, 6), (176, 60, 12), (220, 100, 30), (244, 150, 70), (255, 206, 150)],
         sv="Orange karneol på grottgolv under savannen, y 0 till 64", en="Orange carnelian on cave floors under the savanna, y 0 to 64"),
    dict(name="chalcedony", title="Chalcedony", kind="pebble", y=(-32, 48), biomes=["#minecraft:is_taiga"], count=9,
         pal=[(80, 96, 120), (120, 138, 160), (162, 178, 196), (200, 212, 224), (236, 242, 248)],
         sv="Blågrå kalcedon på grottgolv under taigan, y −32 till 48", en="Blue-grey chalcedony on cave floors under the taiga, y −32 to 48"),
    dict(name="bloodstone", title="Bloodstone", kind="pebble", y=(-64, -16), biomes=OVERWORLD, count=6,
         pal=[(10, 30, 20), (20, 56, 36), (36, 84, 54), (60, 110, 76), (190, 30, 30)],
         sv="Mörkgrön med röda prickar, djupt nere i hela övervärlden, y −64 till −16",
         en="Dark green with red specks, deep down all over the Overworld, y −64 to −16"),
]

SIZES = ["small", "medium", "large"]


def ore_block(gem, host):
    return f"{gem}_ore" if host == "stone" else f"{host}_{gem}_ore"


def blocks_of(g):
    """Every block id a gem has."""
    if g["kind"] == "ore":
        return [ore_block(g["name"], h) for h in g["hosts"]]
    if g["kind"] == "crystal":
        return [f"{s}_{g['name']}_crystal" for s in SIZES]
    return [f"loose_{g['name']}"]
