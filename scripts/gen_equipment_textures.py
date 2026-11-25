"""Equipment textures for the accessory slots, the quiver and worn models.

Makes (under src/main/resources/assets/mechanical/textures/):
  item/quiver.png                         16x16 hide quiver with three fletched arrows
  item/leather_pouch.png                  16x16 leather drawstring pouch, tied with cordage
  entity/worn/leather_pouch.png           32x32 worn pouch (bag, tied neck, belt as the quiver's)
  gui/sprites/container/slot/cloak.png    16x16 empty-slot ghost icons, vanilla style (one grey, rest clear):
  gui/sprites/container/slot/neck.png       cloak, neck, back, ring, hip, charm
  gui/sprites/container/slot/back.png
  gui/sprites/container/slot/ring.png
  gui/sprites/container/slot/hip.png
  gui/sprites/container/slot/charm.png
  entity/cloak/otter_fur_cape.png         64x32 cape textures in the vanilla player-cape UV
  entity/cloak/ermine_mantle.png
  entity/worn/satchel.png                 32x32 worn satchel (bag, flap, strap segment, shoulder pad)
  entity/worn/quiver.png                  32x32 worn quiver (tube, rim, belt, arrow)
  entity/worn/miner_sack.png              64x32 the Miner's ore sack (client/MinerSackModel): sackcloth, ore lumps, straps
  gui/container/pack_both.png             176x166 (on 256x256) backgrounds for equipment.PackMenu, vanilla container
  gui/container/pack_satchel.png            style: the satchel 3x3 and/or the quiver 2x2 over the player inventory
  gui/container/pack_quiver.png             (slot positions as in PackMenu; *_2: both Hip slots)
  gui/container/saddlebags_0/1/2.png       190x166 tamed horse screen (wildlife.SaddlebagMenu) with 0-2 satchels
Run: python scripts/gen_equipment_textures.py"""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def rgba(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def draw(img, grid, pal, ox=0, oy=0):
    """Paint a character grid at (ox, oy); '.' stays transparent."""
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c != ".":
                img.putpixel((ox + x, oy + y), rgba(pal[c]))


def save(img, rel):
    out = ROOT / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print(f"{rel} ok")


def box_faces(u, v, w, h, d):
    """Minecraft box UV: face name -> (x, y, width, height)."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }


def fill(img, rect, pal_chars, pal, rng, weights=None):
    x0, y0, w, h = rect
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            img.putpixel((x, y), rgba(pal[rng.choices(pal_chars, weights)[0]]))


# Leather family shared with leather_satchel.png
LEATHER = {
    "O": "3b1f12", "D": "6e3a1f", "M": "8f4f2b", "L": "a8653a", "H": "c47f4c",
    "s": "e0c9a0", "S": "7a4424", "T": "53291a", "B": "eee6cf", "b": "b8ab88",
    "R": "53291a", "r": "6e3a1f",
    "W": "f2f1ea", "G": "b5b3aa", "n": "7a5530", "c": "a5794a",
}

# ---------------------------------------------------------------- 1. quiver item
QUIVER_ITEM = [
    "......OnO.......",
    "......OWGO......",
    "...OnOOWGOnO....",
    "...OWGOWGOWGO...",
    "...OWGOcOOWGO...",
    "...OWGOcOOWGO...",
    "..OOOOOOOOOOOO..",
    "..OrRRRRRRRRRO..",
    "...OHLLLLsMDOSO.",
    "...OHLLLLDMDOSO.",
    "...OTTTTTTTTOSO.",
    "...OHLLLLsMDOSO.",
    "...OHLLLLDMDOSO.",
    "...ODTTTTTTDOO..",
    "...OMMMMMDDDO...",
    "....OOOOOOOO....",
]
img = Image.new("RGBA", (16, 16))
draw(img, QUIVER_ITEM, LEATHER)
save(img, "item/quiver.png")

# ---------------------------------------------------------------- 1b. leather pouch item (c/n = the cordage drawstring)
POUCH_ITEM = [
    "................",
    "................",
    ".....c....c.....",
    "......c..c......",
    ".......cc.......",
    "......OnnO......",
    ".....OMLLMO.....",
    "......OccO......",
    ".....OHLLMDO....",
    "....OHLLLLMDO...",
    "...OHHLLLLLMDO..",
    "...OHLLLLLLMDO..",
    "...OHLLLLLLMDO..",
    "...OLLLLLLMMDO..",
    "....OMMMMMDDO...",
    ".....OOOOOOO....",
]
img = Image.new("RGBA", (16, 16))
draw(img, POUCH_ITEM, LEATHER)
save(img, "item/leather_pouch.png")

# ---------------------------------------------------------------- 2. slot ghost icons
GHOST = {"#": "555555"}
SLOTS = {
    "cloak": [
        "................",
        "......####......",
        ".....#....#.....",
        "....#.####.#....",
        "....#.#..#.#....",
        "...#..#..#..#...",
        "..#...####...#..",
        "..#..........#..",
        ".#............#.",
        ".#............#.",
        ".#............#.",
        ".#............#.",
        "#..............#",
        "#..............#",
        "################",
        "................",
    ],
    "neck": [
        "................",
        ".#............#.",
        ".#............#.",
        "..#..........#..",
        "..#..........#..",
        "...#........#...",
        "....#......#....",
        ".....##..##.....",
        ".......##.......",
        "......####......",
        "......#..#......",
        "......#..#......",
        "......#.#.......",
        "......##........",
        "......#.........",
        "................",
    ],
    "back": [
        "................",
        "....########....",
        "...#........#...",
        "..#..........#..",
        "..#..........#..",
        ".##############.",
        ".#............#.",
        ".#............#.",
        ".##..........##.",
        ".#.##......##.#.",
        ".#...##..##...#.",
        ".#.....##.....#.",
        ".#....####....#.",
        ".#............#.",
        "..############..",
        "................",
    ],
    "ring": [
        "................",
        "......####......",
        ".....#....#.....",
        ".....#....#.....",
        "....#.####.#....",
        "...#........#...",
        "..#...####...#..",
        "..#..#....#..#..",
        "..#.#......#.#..",
        "..#.#......#.#..",
        "..#..#....#..#..",
        "..#...####...#..",
        "...#........#...",
        "....##....##....",
        "......####......",
        "................",
    ],
    "hip": [
        ".......##.......",
        "....##.##.##....",
        "....##.##.##....",
        ".....#..#..#....",
        "...##########...",
        "...#........#...",
        "...##########...",
        "....#......#....",
        "....#......#....",
        "....#......#....",
        "....########....",
        "....#......#....",
        "....#......#....",
        "....#......#....",
        ".....######.....",
        "................",
    ],
    "charm": [
        "......####......",
        ".....#....#.....",
        "......#..#......",
        ".......##.......",
        "......####......",
        "......#..#......",
        "......#..#......",
        "......#...#.....",
        ".......#..#.....",
        ".......#...#....",
        "........#..#....",
        ".........#.#....",
        "..........##....",
        "...........#....",
        "................",
        "................",
    ],
}
for name, grid in SLOTS.items():
    assert len(grid) == 16 and all(len(r) == 16 for r in grid), name
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw(img, grid, GHOST)
    save(img, f"gui/sprites/container/slot/{name}.png")

# ---------------------------------------------------------------- 3. capes (vanilla cape UV, box 10x16x1 at 0,0)
CAPE = box_faces(0, 0, 10, 16, 1)  # outer face = "front" (x1..10), inner = "back" (x12..21)


def cape(fur, fur_w, gloss, hem, lining, lining_w, seed, spots=()):
    rng = random.Random(seed)
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    pal = {**fur, **hem, **lining}
    for face in ("top", "right", "front", "left"):
        x0, y0, w, h = CAPE[face]
        fill(img, CAPE[face], list(fur), pal, rng, fur_w)
        # vertical strands: a darker or lighter streak 2-3px long
        for _ in range(w * h // 6):
            x, y = x0 + rng.randrange(w), y0 + rng.randrange(h)
            c = rgba(pal[rng.choice(gloss)])
            for k in range(rng.randint(2, 3)):
                if y + k < y0 + h:
                    img.putpixel((x, y + k), c)
    for face in ("right", "front", "left"):  # hem rows at the bottom edge
        x0, y0, w, h = CAPE[face]
        for i, row in enumerate(hem["rows"]):
            for x in range(x0, x0 + w):
                img.putpixel((x, y0 + h - len(hem["rows"]) + i), rgba(pal[rng.choice(row)]))
    fill(img, CAPE["bottom"], [hem["rows"][-1][0]], pal, rng)
    fill(img, CAPE["back"], list(lining), pal, rng, lining_w)
    for x, y, c in spots:
        img.putpixel((x, y), rgba(pal[c]))
    return img


OTTER_FUR = {"a": "4e331f", "b": "3a2516", "c": "664530"}
OTTER_HEM = {"p": "b08a5e", "q": "c9a77c", "d": "8a6a48", "rows": ["pq", "qp", "d"]}
OTTER_LINING = {"l": "6b4a32", "m": "5c3f2a"}
OTTER_FUR_G = {**OTTER_FUR, "x": "82603f"}  # x = glossy highlight, only used in strands
img = cape(OTTER_FUR_G, [5, 3, 2, 0], ["b", "c", "c", "x"], OTTER_HEM, OTTER_LINING, [3, 2], 7)
save(img, "entity/cloak/otter_fur_cape.png")

ERMINE_FUR = {"a": "f0eee3", "b": "e6e3d6", "c": "f7f6ee", "k": "1e1a18"}
ERMINE_HEM = {"e": "dcd8c9", "f": "d0ccbc", "rows": ["ea", "f"]}
ERMINE_LINING = {"l": "c4a276", "m": "b39066", "n": "d0b084"}
# sparse black tail-tip spots on the outer face (x1..10, y1..16): a dot with a tail below
SPOTS = []
for sx, sy in [(2, 2), (7, 3), (4, 6), (9, 7), (2, 10), (6, 11)]:
    SPOTS += [(sx, sy, "k"), (sx, sy + 1, "k")]
img = cape(ERMINE_FUR, [5, 3, 2, 0], ["b", "b", "c"], ERMINE_HEM, ERMINE_LINING, [4, 2, 2], 11, SPOTS)
save(img, "entity/cloak/ermine_mantle.png")

# ---------------------------------------------------------------- 4a. worn satchel (32x32)
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
bag = box_faces(0, 0, 4, 4, 2)
FACE = {
    "top": ["DMMD", "DMMD"],
    "bottom": ["TDDT", "TDDT"],
    "right": ["MD", "sD", "MD", "sT"],
    "front": ["LLLM", "DDDD", "MBbD", "sMsT"],
    "left": ["DM", "Ds", "DM", "Ts"],
    "back": ["MMMD", "sMsD", "MMMD", "TDDT"],
}
for f, (x, y, w, h) in bag.items():
    draw(img, FACE[f], LEATHER, x, y)
flap = box_faces(0, 8, 4, 2, 1)
FACE = {
    "top": ["HLLL"], "bottom": ["MMMM"], "right": ["L", "M"],
    "front": ["HLLL", "sLsL"], "left": ["L", "M"], "back": ["MMMM", "DMMD"],
}
for f, (x, y, w, h) in flap.items():
    draw(img, FACE[f], LEATHER, x, y)
rng = random.Random(3)
for rect in box_faces(0, 12, 2, 1, 1).values():
    fill(img, rect, ["S", "T"], LEATHER, rng, [3, 1])
for rect in box_faces(0, 16, 2, 1, 4).values():
    fill(img, rect, ["S", "T"], LEATHER, rng, [3, 1])
save(img, "entity/worn/satchel.png")

# ---------------------------------------------------------------- 4a2. worn steel-frame pack (64x32)
STEEL = {"a": "8d939a", "b": "6b7078", "c": "a9aeb4", "d": "4e535a"}
img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
rng = random.Random(5)
fill(img, (28, 8, 28, 16), ["a", "b", "c", "d"], STEEL, rng, [4, 3, 2, 1])
for name, (u, v, w, h, d) in {"bag": (0, 0, 9, 11, 5), "flap": (0, 17, 9, 2, 5), "roll": (28, 0, 10, 3, 4)}.items():
    for f, rect in box_faces(u, v, w, h, d).items():
        fill(img, rect, ["M", "D", "L", "S"], LEATHER, rng, [4, 3, 2, 1])
for rect in [box_faces(0, 17, 9, 2, 5)[f] for f in ("front", "back")]:  # flap edge lighter, with stitching
    fill(img, (rect[0], rect[1], rect[2], 1), ["H", "L"], LEATHER, rng, [1, 1])
x, y, w, h = box_faces(0, 0, 9, 11, 5)["back"]  # outward face: a big pocket with a stitched rim and a buckle
for px in range(x + 1, x + w - 1):
    for py in range(y + 3, y + h - 1):
        img.putpixel((px, py), rgba(LEATHER["s" if px in (x + 1, x + w - 2) or py in (y + 3, y + h - 2) else "L"]))
img.putpixel((x + w // 2, y + 3), rgba(STEEL["c"]))
for rect in box_faces(28, 0, 10, 3, 4).values():  # bedroll: a darker tie strap round the middle
    for px in range(rect[0], rect[0] + rect[2]):
        img.putpixel((px, rect[1] + rect[3] // 2), rgba(LEATHER["T"]))
for u, v, w, h, d in [(0, 24, 2, 6, 1), (8, 24, 2, 1, 4), (0, 32, 8, 1, 4)]:
    for rect in box_faces(u, v, w, h, d).values():
        fill(img, rect, ["S", "T"], LEATHER, rng, [3, 1])
save(img, "entity/worn/steel_frame_pack.png")

# ---------------------------------------------------------------- 4b. worn quiver (32x32)
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
tube = box_faces(0, 0, 3, 9, 3)
SIDE = ["HLM", "HLM", "TTT", "HLM", "LLM", "HLM", "TTT", "LMM", "MDD"]
FACE = {
    "top": ["TOT", "OOO", "TOT"],  # open mouth, mostly hidden by the rim
    "bottom": ["DDD", "DTD", "DDD"],
    "right": SIDE, "front": SIDE, "left": SIDE,
    "back": ["LsM", "LDM", "TTT", "LsM", "LDM", "LsM", "TTT", "MsD", "MDD"],  # vertical seam
}
for f, (x, y, w, h) in tube.items():
    draw(img, FACE[f], LEATHER, x, y)
rim = box_faces(0, 14, 4, 1, 4)
FACE = {
    "top": ["RrrR", "rOOr", "rOOr", "RrrR"], "bottom": ["RRRR"] * 4,
    "right": ["rRRR"], "front": ["rrRR"], "left": ["rRRR"], "back": ["RRRR"],
}
for f, (x, y, w, h) in rim.items():
    draw(img, FACE[f], LEATHER, x, y)
belt = box_faces(0, 20, 8, 1, 4)
for f, rect in belt.items():
    fill(img, rect, ["S", "T"], LEATHER, rng, [4, 1])
draw(img, ["SBbS"], LEATHER, belt["front"][0] + 2, belt["front"][1])  # bone buckle
arrow = box_faces(24, 0, 1, 4, 1)
draw(img, ["n"], LEATHER, *arrow["top"][:2])
draw(img, ["c"], LEATHER, *arrow["bottom"][:2])
for f, fe in (("right", "WG"), ("front", "GW"), ("left", "WG"), ("back", "GW")):
    draw(img, [fe[0], fe[1], "c", "c"], LEATHER, *arrow[f][:2])
save(img, "entity/worn/quiver.png")

# ---------------------------------------------------------------- 4c. worn leather pouch (32x32)
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
bag = box_faces(0, 0, 3, 3, 2)
SIDE3 = ["HLM", "LLM", "MDD"]
SIDE2 = ["LM", "LM", "DD"]
FACE = {"top": ["TTT", "TTT"], "bottom": ["DDD", "DDD"], "front": ["HLM", "LsM", "MMD"], "back": SIDE3,
        "right": SIDE2, "left": SIDE2}
for f, (x, y, w, h) in bag.items():
    draw(img, FACE[f], LEATHER, x, y)
neck = box_faces(0, 6, 2, 1, 1)
for f, (x, y, w, h) in neck.items():
    draw(img, [("cn" * w)[:w]] * h, LEATHER, x, y)
belt = box_faces(0, 20, 8, 1, 4)
for f, rect in belt.items():
    fill(img, rect, ["S", "T"], LEATHER, rng, [4, 1])
draw(img, ["SBbS"], LEATHER, belt["front"][0] + 2, belt["front"][1])  # bone buckle
save(img, "entity/worn/leather_pouch.png")


# ---------------------------------------------------------------- 4d. the Miner's ore sack (64x32, client/MinerSackModel)
img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
SACK = {"a": "8a7656", "b": "7a6648", "c": "6a573c", "d": "5a4a32", "e": "9a8664", "s": "4a3c28", "x": "3b1f12"}
rng = random.Random(11)
for f, (x, y, w, h) in box_faces(0, 0, 9, 11, 6).items():
    fill(img, (x, y, w, h), list("abcde"), SACK, rng, [4, 4, 2, 1, 2])
    if f in ("front", "back", "right", "left"):
        for yy in range(y, y + h):  # dirt and wear toward the bottom
            for xx in range(x, x + w):
                if yy - y > h - 3 and rng.random() < 0.6:
                    img.putpixel((xx, yy), rgba(SACK["d" if rng.random() < 0.7 else "s"]))
        for xx in range(x, x + w):  # the rim of the mouth
            img.putpixel((xx, y), rgba(SACK["s"]))
    if f == "back":  # a sewn patch and a seam
        for yy in range(y + 3, y + 7):
            for xx in range(x + 2, x + 5):
                img.putpixel((xx, yy), rgba(SACK["c"] if (xx + yy) % 2 else SACK["d"]))
        for yy in range(y + 1, y + h - 1, 2):
            img.putpixel((x + w // 2 + 1, yy), rgba(SACK["s"]))
ORE = {"g": "7d7d7d", "G": "5f5f5f", "k": "9a9a9a", "t": "2e2a2a", "o": "c4703a", "m": "3c9a6a", "i": "d8af93", "n": "272522"}
for (u, v, w, h, d), chars, weights in (((0, 18, 3, 2, 3), "gGkto", [4, 3, 2, 2, 2]), ((14, 18, 2, 2, 2), "gGkmi", [4, 3, 2, 2, 2]),
                                       ((24, 18, 2, 1, 2), "gGknt", [3, 3, 2, 2, 1])):
    for rect in box_faces(u, v, w, h, d).values():
        fill(img, rect, list(chars), ORE, rng, weights)
for rect in list(box_faces(32, 0, 1, 7, 1).values()) + list(box_faces(40, 0, 1, 1, 5).values()):
    fill(img, rect, ["S", "T"], LEATHER, rng, [3, 1])
save(img, "entity/worn/miner_sack.png")

# ---------------------------------------------------------------- PackMenu backgrounds (vanilla container look)
PANEL, LIGHT, SHADE, OUTLINE, SLOT_DARK, SLOT_FILL = 198, 255, 85, 0, 55, 139


def grey(v):
    return (v, v, v, 255)


def panel(w=176, h=166):
    img = Image.new("RGBA", (256, 256))
    for y in range(h):
        for x in range(w):
            img.putpixel((x, y), grey(PANEL))
    for x in range(2, w - 3):
        img.putpixel((x, 0), grey(OUTLINE))
        img.putpixel((x, h - 1), grey(OUTLINE))
    for y in range(2, h - 3):
        img.putpixel((0, y), grey(OUTLINE))
        img.putpixel((w - 1, y), grey(OUTLINE))
    # rounded outline corners, a 2px highlight along the top/left and a 2px shade along the bottom/right
    for x, y in ((1, 1), (w - 3, 1), (w - 2, 2), (1, h - 3), (2, h - 2), (w - 2, h - 3), (w - 3, h - 2)):
        img.putpixel((x, y), grey(OUTLINE))
    for x in range(1, w - 3):
        for y in (1, 2):
            if (x, y) != (1, 1):
                img.putpixel((x, y), grey(LIGHT))
    for y in range(1, h - 3):
        for x in (1, 2):
            if (x, y) != (1, 1):
                img.putpixel((x, y), grey(LIGHT))
    img.putpixel((3, 3), grey(LIGHT))
    for x in range(3, w - 2):
        for y in (h - 3, h - 2):
            if img.getpixel((x, y))[0] != OUTLINE or x < w - 3:
                img.putpixel((x, y), grey(SHADE))
    for y in range(3, h - 2):
        for x in (w - 3, w - 2):
            img.putpixel((x, y), grey(SHADE))
    img.putpixel((w - 4, h - 4), grey(SHADE))
    for x, y in ((w - 2, h - 3), (w - 3, h - 2)):
        img.putpixel((x, y), grey(OUTLINE))
    img.putpixel((w - 2, h - 2), (0, 0, 0, 0))
    img.putpixel((w - 3, h - 1), (0, 0, 0, 0))
    img.putpixel((w - 1, h - 3), (0, 0, 0, 0))
    for x, y in ((0, 0), (1, 0), (0, 1), (w - 1, 0), (w - 2, 0), (w - 1, 1), (0, h - 1), (1, h - 1), (0, h - 2), (w - 1, h - 1), (w - 2, h - 1), (w - 1, h - 2)):
        img.putpixel((x, y), (0, 0, 0, 0))
    # the rounded bottom-left/top-right/bottom-right corners, pixel for pixel as vanilla's containers
    for x, y in ((0, h - 3), (1, h - 2), (2, h - 1), (w - 3, 0), (w - 2, 1), (w - 1, 2)):
        img.putpixel((x, y), (0, 0, 0, 0))
    for x, y in ((w - 3, h - 2), (w - 2, h - 3)):
        img.putpixel((x, y), grey(SHADE))
    for x, y in ((w - 3, h - 1), (w - 2, h - 2), (w - 1, h - 3)):
        img.putpixel((x, y), grey(OUTLINE))
    return img


def slot(img, x, y):
    """A slot frame for an item drawn at (x, y): 18x18 from (x-1, y-1), dark top/left, light bottom/right."""
    for i in range(18):
        for j in range(18):
            img.putpixel((x - 1 + i, y - 1 + j), grey(SLOT_FILL))
    for i in range(17):
        img.putpixel((x - 1 + i, y - 1), grey(SLOT_DARK))
        img.putpixel((x - 1, y - 1 + i), grey(SLOT_DARK))
        img.putpixel((x + i, y + 16), grey(LIGHT))
        img.putpixel((x + 16, y + i), grey(LIGHT))


def pack(satchel_left, quiver_left, name, cols=3, tools=0, hips=None):
    """cols: the bag's grid (3, or 5 for the Steel-Frame Pack, two rows taller); tools: tool-loop slots right of the grid
    (Satchels.TOOL_SLOTS), each with a faint outline of a hammer so they read as tool slots. As PackMenu lays them out."""
    extra = (cols - 3) * 18
    img = panel(176, 166 + extra)
    if satchel_left is not None:
        for r in range(cols):
            for c in range(cols):
                slot(img, satchel_left + c * 18, 17 + r * 18)
        for i in range(tools):
            x, y = satchel_left + cols * 18 + 4, 17 + i * 18
            slot(img, x, y)
            for px, py in [(5, 3), (6, 3), (7, 3), (8, 3), (9, 3), (10, 3), (5, 4), (6, 4), (7, 4), (8, 4), (9, 4), (10, 4)] + [(7, k) for k in range(5, 13)] + [(8, k) for k in range(5, 13)]:
                img.putpixel((x + px, y + py), grey(SLOT_FILL - 14))  # hammer silhouette
    # hips: the top-left slots of each hip bag's 2x2 (PackMenu.hipPosition); else one at (quiver_left, 26)
    for hx, hy in hips or ([(quiver_left, 26)] if quiver_left is not None else []):
        for r in range(2):
            for c in range(2):
                slot(img, hx + c * 18, hy + r * 18)
    for r in range(3):
        for c in range(9):
            slot(img, 8 + c * 18, 84 + extra + r * 18)
    for c in range(9):
        slot(img, 8 + c * 18, 142 + extra)
    save(img, f"gui/container/{name}.png")


pack(30, 116, "pack_both")
pack(62, None, "pack_satchel")
pack(None, 71, "pack_quiver")
pack(30, 116, "pack_both_tools", tools=3)
pack(62, None, "pack_satchel_tools", tools=3)
pack(8, 134, "pack_pack_both", cols=5, tools=3)
pack((176 - 90 - 22) // 2, None, "pack_pack", cols=5, tools=3)
# both Hip slots worn (two pouches, or a pouch and the quiver): positions as PackMenu.satchelLeft / hipPosition
pack(None, None, "pack_quiver_2", hips=[(47, 26), (93, 26)])
pack(18, None, "pack_both_2", hips=[(82, 26), (124, 26)])
pack(8, None, "pack_both_tools_2", tools=3, hips=[(90, 26), (132, 26)])
pack(8, None, "pack_pack_both_2", cols=5, tools=3, hips=[(134, 17), (134, 63)])


def saddlebags(satchels):
    """wildlife.SaddlebagMenu, 190 wide: saddle + armour slots, left satchel, the horse in a dark box, right satchel."""
    w = 190
    img = panel(w)
    slot(img, 8, 18)
    slot(img, 8, 36)
    for side, left in enumerate((30, 130)):
        if side < satchels:
            for r in range(3):
                for c in range(3):
                    slot(img, left + c * 18, 18 + r * 18)
    for x in range(85, 127):                              # the horse preview box, as vanilla's horse screen
        for y in range(17, 71):
            img.putpixel((x, y), grey(0))
    for r in range(3):
        for c in range(9):
            slot(img, 15 + c * 18, 84 + r * 18)
    for c in range(9):
        slot(img, 15 + c * 18, 142)
    save(img, f"gui/container/saddlebags_{satchels}.png")


for n in range(3):
    saddlebags(n)
