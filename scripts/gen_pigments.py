"""Pigments and cave paintings (Pigments.java, CavePaintingBlock, PaintItem, PaintbrushItem): every texture and data file.
Loose Red Ochre / Yellow Ochre / Chalk finds (rock models, loot, worldgen in badlands, savanna, rivers, shores, meadows),
grinding (Flint Pickaxe in the grid; the Stone Anvil is Java), paint and paintbrush recipes, pigment -> dye, recipe
unlocks, the cave painting block (15 motifs x 4 colours, hand-drawn rock-art pixel motifs), item textures, lang.
Generator order: ... gen_soot.py, gen_phantom.py, gen_pigments.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_pigments.py"""
import json
import math
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

from gen_crude_ranged_textures import JAR

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
(A / "textures/block").mkdir(parents=True, exist_ok=True)
(A / "textures/item").mkdir(parents=True, exist_ok=True)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def vanilla(path):
    return Image.open(BytesIO(zipfile.ZipFile(JAR).read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def lum(r, g, b):
    return (r * 299 + g * 587 + b * 114) / 1000 / 255


def ramp(stops, t):
    """Colour at t (0..1) along a list of RGB stops."""
    t = max(0.0, min(1.0, t)) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    f = t - i
    return tuple(int(stops[i][k] + (stops[i + 1][k] - stops[i][k]) * f) for k in range(3))


def recolour(img, stops, gamma=1.0, keep=lambda x, y, px: True):
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            px = img.getpixel((x, y))
            if px[3] and keep(x, y, px):
                out.putpixel((x, y), ramp(stops, lum(*px[:3]) ** gamma) + (px[3],))
    return out


# ---- colours (dark -> light) ----
PAL = {
    "black": [(18, 15, 14), (38, 33, 30), (62, 55, 50)],         # soot, charcoal
    "red": [(84, 26, 18), (138, 46, 28), (172, 72, 44)],         # red ochre (haematite earth)
    "yellow": [(140, 92, 30), (190, 136, 48), (218, 172, 82)],   # yellow ochre (goethite earth)
    "white": [(180, 174, 160), (216, 210, 196), (236, 232, 222)],  # chalk
}
COLOURS = list(PAL)

# ---- motifs: hand-drawn grids, '#' thick paint, '+' thin wash (body fill), '.' bare rock ----
SPRITES = {
    "aurochs": [  # Lascaux bull: lyre horns forward, humped shoulders, heavy chest, thin legs
        "..#.#...........",
        "..#..#..........",
        "...##...........",
        "..#####.##......",
        ".##++##########.",
        "##+++++++++++###",
        "#.#++++++++++++#",
        "..#+++++++++++##",
        "..##+++++++++#.#",
        "...#+##++##++#..",
        "...#.#.....#.#..",
        "...#.#.....#.#..",
        "..#..#....#..#..",
    ],
    "horse": [  # the round-bellied "Chinese horse": small head, brush mane, short legs
        ".###............",
        "####............",
        "#.###...........",
        "...####.........",
        "....##########..",
        "...#++++++++++#.",
        "...#+++++++++++#",
        "..#++++++++++#.#",
        "..#+++++++++#..#",
        "...##+++++##....",
        "...#.#...#.#....",
        "...#..#..#..#...",
        "..#...#..#...#..",
    ],
    "deer": [  # stag with branching antlers
        "#.#.#...........",
        ".###.#..........",
        "..#.#...........",
        "...##...........",
        "..###...........",
        "..##...........#",
        "...############.",
        "...#++++++++++#.",
        "...#+++++++++#..",
        "....##+++++##...",
        "....#.#...#.#...",
        "...#..#...#..#..",
        "...#...#..#...#.",
    ],
    "bear": [  # cave bear: low head, heavy hump
        "................",
        ".....#####......",
        "....#+++++###...",
        "...#+++++++++#..",
        ".##++++++++++##.",
        "###+++++++++++#.",
        "#+#++++++++++++#",
        ".##++++++++++++#",
        "..#+++++++++++#.",
        "..##.##+++.##.#.",
        "..##.##...##.##.",
        "..##.##...##.##.",
    ],
    "mammoth": [  # domed head, sloping back, curled tusk, trunk
        "...####.........",
        "..#++++###......",
        ".#++++++++##....",
        ".#++++++++++##..",
        "#+++++++++++++#.",
        "#+#+++++++++++#.",
        "#.#++++++++++++#",
        "#.##+++++++++++#",
        "#...#++++++++++#",
        "#.##.#+##+++##..",
        ".#...#.##..#.#..",
        ".....#.##..#.#..",
        "....##.##.##.##.",
    ],
    "ibex": [  # long horns sweeping back over the body
        "....####........",
        "...#....##......",
        "..##......#.....",
        ".###............",
        "####............",
        ".#.#............",
        "...#########....",
        "...#+++++++##...",
        "...#+++++++#.#..",
        "....##+++##.....",
        "....#.#.#.#.....",
        "...#..#.#..#....",
    ],
    "hunter": [  # stick man with a raised spear
        "............#...",
        "...##......#....",
        "...##.....#.....",
        "....#....#......",
        "..######.#......",
        ".#..#..##.......",
        "#...#...#.......",
        "....#...........",
        "....#...........",
        "...#.#..........",
        "..#...#.........",
        "..#....#........",
        ".#.....#........",
        "#.......#.......",
    ],
    "hand": [  # hand pressed on the rock: palm, four fingers, thumb
        "................",
        "......#.........",
        "...#..#..#......",
        "...#..#..#......",
        "...##.##.#..#...",
        "...##.##.##.#...",
        "...##.##.##.##..",
        "...##########...",
        "#..##########...",
        "##.##########...",
        ".############...",
        "..###########...",
        "...#########....",
        "...#########....",
        "....#######.....",
        "....#######.....",
    ],
}
RUNNER = [  # running hunter for the hunt scene
    "..##..",
    "..##..",
    "...#..",
    ".####.",
    "#..#.#",
    "...#..",
    "..#.#.",
    ".#...#",
    "#....#",
    "#.....",
]
SMALL_HORSE = [  # little galloping horse for the herd scene
    "##........",
    "###.......",
    "..#######.",
    "..#+++++##",
    "..#+++++#.",
    "..#.##.#..",
    ".#..#...#.",
    "#...#....#",
]


class Canvas:
    def __init__(self, width, height=16):
        self.w, self.h = width, height
        self.v = [[0.0] * width for _ in range(height)]

    def put(self, x, y, value):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.v[y][x] = max(self.v[y][x], value)

    def stamp(self, rows, ox, oy):
        for y, row in enumerate(rows):
            for x, c in enumerate(row):
                if c != ".":
                    self.put(ox + x, oy + y, 1.0 if c == "#" else 0.55)

    def line(self, x0, y0, x1, y1, value=1.0):
        n = max(abs(x1 - x0), abs(y1 - y0))
        for i in range(n + 1):
            self.put(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), value)


def centred(rows):
    c = Canvas(16)
    width = max(map(len, rows))
    assert width <= 16 and len(rows) <= 16, rows
    c.stamp(rows, (16 - width) // 2, (16 - len(rows)) // 2)
    return c


motifs = {name: centred(rows) for name, rows in SPRITES.items()}

sun = Canvas(16)
for y in range(16):
    for x in range(16):
        d = math.hypot(x - 7.5, y - 7.5)
        if 2.2 < d < 3.4:
            sun.put(x, y, 1.0)
        elif d <= 2.2:
            sun.put(x, y, 0.55)
for k in range(8):  # eight rays, short gap from the disc
    a = k * math.pi / 4 + 0.15
    sun.line(round(7.5 + 4.6 * math.cos(a)), round(7.5 + 4.6 * math.sin(a)), round(7.5 + 7.2 * math.cos(a)), round(7.5 + 7.2 * math.sin(a)))
motifs["sun"] = sun

spiral = Canvas(16)
t = 0.0
while t < 5.2 * math.pi:  # Archimedean spiral, drawn outward from the centre in one stroke
    r = 0.4 + t / (5.2 * math.pi) * 6.8
    spiral.put(round(7.5 + r * math.cos(t)), round(7.5 + r * math.sin(t)), 1.0)
    t += 0.05
motifs["spiral"] = spiral

dots = Canvas(16)
drnd = random.Random(31)
for row in range(4):  # rows of fingertip dots, like the dot panels of Chauvet
    for col in range(4):
        x, y = 1 + col * 4 + drnd.randint(0, 1), 1 + row * 4 + drnd.randint(0, 1)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
            if drnd.random() < 0.85:
                dots.put(x + dx, y + dy, 1.0)
motifs["dots"] = dots

# scenes, 32 wide, split into a left and a right block
hunt = Canvas(32)
for ox, oy in ((1, 5), (8, 4)):  # two hunters running right, spears levelled at the bull
    hunt.stamp(RUNNER, ox, oy)
    hunt.line(ox + 5, oy + 4, ox + 12, oy - 1)
hunt.stamp(SPRITES["aurochs"], 15, 2)  # the bull faces them, two spears in its back
hunt.stamp(SPRITES["aurochs"], 15, 2)
hunt.line(21, 0, 24, 6)
hunt.line(26, 1, 27, 6)
herd = Canvas(32)
for ox, oy in ((0, 1), (7, 6), (12, 0), (19, 5), (23, 1)):
    herd.stamp(SMALL_HORSE, ox, oy)


def half(canvas, right):
    c = Canvas(16)
    c.v = [row[16:] if right else row[:16] for row in canvas.v]
    return c


motifs["hunt_left"], motifs["hunt_right"] = half(hunt, False), half(hunt, True)
motifs["herd_left"], motifs["herd_right"] = half(herd, False), half(herd, True)
# must match CavePaintingBlock.Motif
MOTIFS = ["aurochs", "horse", "deer", "bear", "mammoth", "ibex", "hunter", "sun", "spiral", "dots", "hand",
          "hunt_left", "hunt_right", "herd_left", "herd_right"]
assert set(MOTIFS) == set(motifs)


def value_noise(rnd, cells, width=16):
    grid = [[rnd.random() for _ in range(cells * width // 16 + 1)] for _ in range(cells + 1)]
    out = [[0.0] * width for _ in range(16)]
    for y in range(16):
        for x in range(width):
            fx, fy = x * cells / 16, y * cells / 16
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            g = lambda i, j: grid[y0 + j][x0 + i]
            out[y][x] = (g(0, 0) * (1 - tx) + g(1, 0) * tx) * (1 - ty) + (g(0, 1) * (1 - tx) + g(1, 1) * tx) * ty
    return out


def paint(canvas, colour, seed):
    """Pigment rubbed on rock: uneven, semi-transparent (stone shows through), worn patches and flecks, never crisp."""
    rnd = random.Random(seed)
    wear = value_noise(rnd, 4)  # weathered patches (same field for all colours of a motif: the rock wears the same)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    crnd = random.Random(seed * 7 + COLOURS.index(colour))
    for y in range(16):
        for x in range(16):
            v = canvas.v[y][x]
            if not v:
                continue
            if rnd.random() < 0.06:  # flaked off
                continue
            edge = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))
                       if not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not canvas.v[y + dy][x + dx])
            alpha = (205 if v >= 1 else 120) * (0.72 + 0.4 * wear[y][x]) - edge * 10 + rnd.randint(-14, 14)
            shade = 0.25 + 0.6 * wear[y][x] + crnd.uniform(-0.2, 0.2)
            img.putpixel((x, y), ramp(PAL[colour], shade) + (int(max(70, min(235, alpha))),))
    return img


for i, m in enumerate(MOTIFS):
    for colour in COLOURS:
        img = paint(motifs[m], colour, 1000 + i)
        assert img.getbbox() is not None, m
        img.save(A / f"textures/block/cave_painting_{m}_{colour}.png")

# ---- cave painting: thin layer models (like soot), one per motif and colour, rotated per facing ----
for m in MOTIFS:
    for colour in COLOURS:
        tex = f"mechanical:block/cave_painting_{m}_{colour}"
        # against the north side of the block, seen from the south: facing=south unrotated
        w(A / f"models/block/cave_painting_{m}_{colour}.json", {"ambientocclusion": False, "textures": {"particle": tex, "paint": tex},
            "elements": [{"from": [0, 0, 0.1], "to": [16, 16, 0.1], "faces": {"south": {"uv": [0, 0, 16, 16], "texture": "#paint"}}}]})
ROT = {"south": {}, "west": {"y": 90}, "north": {"y": 180}, "east": {"y": 270}, "down": {"x": 270}, "up": {"x": 90}}
w(A / "blockstates/cave_painting.json", {"variants": {
    f"colour={colour},facing={facing},motif={m}": {"model": f"mechanical:block/cave_painting_{m}_{colour}", **rot}
    for facing, rot in ROT.items() for m in MOTIFS for colour in COLOURS}})

# ---- loose finds: rock models (gen_data.py rock_size1..3) with earthy textures drawn from vanilla calcite ----
calcite = vanilla("block/calcite")
FIND_TEX = {
    "red_ochre": recolour(calcite, [(96, 34, 22), (146, 58, 34), (178, 86, 52), (196, 112, 74)], 1.6),
    "yellow_ochre": recolour(calcite, [(132, 88, 32), (180, 130, 50), (206, 162, 78), (222, 190, 112)], 1.6),
    "chalk": recolour(calcite, [(178, 176, 166), (214, 212, 202), (236, 234, 226), (246, 245, 240)], 1.2),
}
FINDS = {"red_ochre": "Red Ochre", "yellow_ochre": "Yellow Ochre", "chalk": "Chalk"}
for v in FINDS:
    FIND_TEX[v].save(A / f"textures/block/{v}_rock.png")
    block = f"mechanical:loose_{v}"
    variants = {}
    for size in (1, 2, 3):
        w(A / f"models/block/loose_{v}_size{size}.json", {"parent": f"mechanical:block/rock_size{size}", "textures": {"all": f"mechanical:block/{v}_rock"}})
        variants[f"size={size}"] = [{"model": f"mechanical:block/loose_{v}_size{size}", "y": y} for y in (0, 90, 180, 270)]
    w(A / f"blockstates/loose_{v}.json", {"variants": variants})
    w(D / f"mechanical/loot_table/blocks/loose_{v}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"mechanical:{v}", "modifier": [
            {"type": "minecraft:set_count", "count": s, "condition": {"type": "minecraft:match_block", "blocks": block, "state": {"size": str(s)}}}
            for s in (2, 3)]}]}], "random_sequence": f"mechanical:blocks/loose_{v}"})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", *[f"mechanical:loose_{v}" for v in FINDS])

# ---- worldgen: surface finds in their biomes (natural ground, sand, gravel, stone, terracotta; never in water) ----
BADLANDS = ["minecraft:badlands", "minecraft:eroded_badlands", "minecraft:wooded_badlands"]
SAVANNA = ["minecraft:savanna", "minecraft:savanna_plateau", "minecraft:windswept_savanna"]
WHERE = {  # find: (biomes, rarity 1 in N chunks tries, tries)
    "red_ochre": (BADLANDS + SAVANNA, 2, 5),
    "yellow_ochre": (SAVANNA + ["minecraft:desert", "minecraft:river"], 2, 4),  # badlands stay red
    "chalk": (["minecraft:stony_shore", "minecraft:beach", "minecraft:meadow", "minecraft:windswept_hills",
               "minecraft:windswept_gravelly_hills", "minecraft:stony_peaks"], 2, 4),
}
GROUND = ["minecraft:sand", "minecraft:red_sand", "minecraft:gravel", "minecraft:stone", "minecraft:calcite", "minecraft:clay",
          "minecraft:terracotta"] + [f"minecraft:{c}_terracotta" for c in ("white", "orange", "yellow", "brown", "red", "light_gray")]
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
for v, (biomes, rarity, tries) in WHERE.items():
    w(D / f"mechanical/worldgen/feature/loose_{v}.json", {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted", "entries": [
        {"data": {"id": f"mechanical:loose_{v}", "properties": {"size": "1"}}, "weight": 3},
        {"data": {"id": f"mechanical:loose_{v}", "properties": {"size": "2"}}, "weight": 1}]}})
    w(D / f"mechanical/worldgen/placed_feature/loose_{v}.json", {"feature": f"mechanical:loose_{v}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:count", "count": tries},  # a small cluster: spread sideways first, then down onto the ground
        {"type": "minecraft:offset", "x": tri(5), "y": 0, "z": tri(5)},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"},
            {"type": "minecraft:not", "predicate": {"type": "minecraft:matching_fluids", "fluids": "minecraft:water"}},
            {"type": "minecraft:any_of", "predicates": [
                {"type": "minecraft:matching_block_tag", "offset": [0, -1, 0], "tag": "minecraft:substrate_overworld"},
                {"type": "minecraft:matching_blocks", "offset": [0, -1, 0], "blocks": GROUND}]},
            {"type": "minecraft:would_survive", "state": f"mechanical:loose_{v}"}]}}]})
    w(D / f"mechanical/neoforge/biome_modifier/add_loose_{v}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:loose_{v}", "step": "vegetal_decoration"})

# ---- item textures ----
raw = vanilla("item/raw_iron")  # a knobbly lump
recolour(raw, [(78, 26, 16), (130, 46, 28), (168, 72, 44), (198, 110, 72)], 1.3).save(A / "textures/item/red_ochre.png")
recolour(raw, [(110, 72, 24), (170, 120, 44), (206, 160, 74), (228, 196, 116)], 1.3).save(A / "textures/item/yellow_ochre.png")
recolour(vanilla("item/clay_ball"), [(160, 158, 148), (210, 208, 198), (234, 232, 224), (248, 247, 242)], 1.4).save(A / "textures/item/chalk.png")
gunpowder = vanilla("item/gunpowder")  # a heap of powder, like Soot
POWDER = {"black": [(14, 12, 12), (40, 36, 34), (70, 64, 60)], "red": [(90, 26, 16), (150, 52, 32), (196, 98, 64)],
          "yellow": [(136, 88, 26), (196, 142, 50), (232, 196, 110)], "white": [(170, 168, 160), (222, 220, 212), (250, 249, 246)]}
for colour, stops in POWDER.items():
    recolour(gunpowder, stops, 1.2).save(A / f"textures/item/{colour}_pigment.png")
# paint: the vanilla bowl with the stew swapped for a thick paste of the colour
bowl, stew = vanilla("item/bowl"), vanilla("item/mushroom_stew")
for colour, stops in POWDER.items():
    img = stew.copy()
    for y in range(16):
        for x in range(16):
            px = stew.getpixel((x, y))
            if px[3] and px != bowl.getpixel((x, y)):
                img.putpixel((x, y), ramp(stops, lum(*px[:3]) ** 0.8 + 0.1) + (255,))
    img.save(A / f"textures/item/{colour}_paint.png")
# paintbrush: the vanilla stick, its top end bound with a thick tuft of fibre whose tip is stained with red paint
brush = vanilla("item/stick").copy()
stick = [(x, y) for y in range(16) for x in range(16) if brush.getpixel((x, y))[3]]
ty = min(y for _, y in stick)
FIBRE = [(96, 80, 44), (138, 118, 66), (176, 156, 96)]
for x, y in stick:
    d = y - ty
    if d == 4:
        brush.putpixel((x, y), (70, 52, 30, 255))  # the binding
    elif d < 4:
        for px, py in ((x, y), (x - 1, y), (x, y + 1), (x - 1, y + 1) if d < 2 else (x, y)):
            if 0 <= px < 16 and 0 <= py < 16:
                c = (150, 52, 32) if d == 0 else (118, 40, 24) if d == 1 else FIBRE[(px + py) % 3]
                brush.putpixel((px, py), c + (255,))
brush.save(A / "textures/item/paintbrush.png")

ITEMS = {"red_ochre": "Red Ochre", "yellow_ochre": "Yellow Ochre", "chalk": "Chalk", "black_pigment": "Black Pigment",
         "red_pigment": "Red Pigment", "yellow_pigment": "Yellow Pigment", "white_pigment": "White Pigment",
         "black_paint": "Black Paint", "red_paint": "Red Paint", "yellow_paint": "Yellow Paint", "white_paint": "White Paint",
         "paintbrush": "Paintbrush"}
for item in ITEMS:
    parent = "minecraft:item/handheld" if item == "paintbrush" else "minecraft:item/generated"
    w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# ---- recipes ----
R = D / "mechanical/recipe"
GRIND = {"red_ochre": "red_pigment", "yellow_ochre": "yellow_pigment", "chalk": "white_pigment", "minecraft:charcoal": "black_pigment"}
for src, out in GRIND.items():  # by hand: one pigment; the Stone Anvil gives two (Pigments.ground)
    w(R / f"{out}_from_{src.split(':')[-1]}.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "group": out,
        "ingredients": [src if ":" in src else f"mechanical:{src}", "mechanical:flint_pickaxe"], "result": {"id": f"mechanical:{out}", "count": 1}})
BINDER = ["mechanical:tallow", "mechanical:clay_water_bucket", "minecraft:water_bucket"]  # fat or water; buckets come back
PIGMENT = {"black": ["mechanical:black_pigment", "mechanical:soot"], "red": "mechanical:red_pigment",
           "yellow": "mechanical:yellow_pigment", "white": "mechanical:white_pigment"}
for colour, pigment in PIGMENT.items():
    w(R / f"{colour}_paint.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "group": "paint",
        "ingredients": [pigment, pigment, BINDER, "minecraft:bowl"], "result": {"id": f"mechanical:{colour}_paint", "count": 1}})
    w(R / f"{colour}_dye_from_{colour}_pigment.json", {"type": "minecraft:crafting_shapeless", "category": "misc", "group": f"{colour}_dye",
        "ingredients": [f"mechanical:{colour}_pigment"], "result": {"id": f"minecraft:{colour}_dye", "count": 1}})
w(R / "paintbrush.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
    "ingredients": ["minecraft:stick", "mechanical:plant_fibre", "mechanical:plant_fibre"], "result": {"id": "mechanical:paintbrush"}})
UNLOCK = ["red_pigment_from_red_ochre", "yellow_pigment_from_yellow_ochre", "white_pigment_from_chalk", "black_pigment_from_charcoal",
          "black_paint", "red_paint", "yellow_paint", "white_paint", "paintbrush",
          "black_dye_from_black_pigment", "red_dye_from_red_pigment", "yellow_dye_from_yellow_pigment", "white_dye_from_white_pigment"]
w(D / "mechanical/advancement/recipes/pigments.json", {"parent": "minecraft:recipes/root",
    "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": [
        "mechanical:red_ochre", "mechanical:yellow_ochre", "mechanical:chalk", "mechanical:soot",
        "mechanical:red_pigment", "mechanical:yellow_pigment", "mechanical:white_pigment", "mechanical:black_pigment"]}]}}},
    "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in UNLOCK]}})

# ---- lang ----
lang = {f"item.mechanical.{k}": v for k, v in ITEMS.items()}
lang.update({f"block.mechanical.loose_{v}": f"Loose {name}" for v, name in FINDS.items()})
lang["block.mechanical.cave_painting"] = "Cave Painting"
MOTIF_NAMES = {"aurochs": "Aurochs", "horse": "Horse", "deer": "Stag", "bear": "Cave Bear", "mammoth": "Mammoth", "ibex": "Ibex",
               "hunter": "Hunter", "sun": "Sun", "spiral": "Spiral", "dots": "Dots", "hand": "Hand Print",
               "hunt_left": "The Hunt (2 wide)", "hunt_right": "The Hunt", "herd_left": "Running Herd (2 wide)", "herd_right": "Running Herd"}
lang.update({f"motif.mechanical.{k}": v for k, v in MOTIF_NAMES.items()})
lang.update({
    "message.mechanical.paintbrush.motif": "Motif: %s",
    "message.mechanical.paintbrush.no_paint": "Hold a bowl of paint in your other hand",
    "message.mechanical.paintbrush.no_room": "Paint needs a solid face with space in front",
    "message.mechanical.paintbrush.no_room_scene": "A scene needs two solid wall faces side by side",
    "tooltip.mechanical.paintbrush.motif": "Motif: %s (sneak-use to change)",
})
merge(A / "lang/en_us.json", lang)
print(f"pigments ok ({len(MOTIFS)} motifs x {len(COLOURS)} colours)")
