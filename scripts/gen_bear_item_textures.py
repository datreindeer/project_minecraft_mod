"""Generate 16x16 brown bear item textures: bear pelt, bear fat, bear claw and the spawn egg.

Run: python scripts/gen_bear_item_textures.py
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_wildlife_item_textures import OUT, egg, from_grid, hexc  # noqa: E402
from gen_wild_boar_texture import h  # noqa: E402

# Spread pelt seen from above: head with round ears at the top, forelegs and hind legs splayed, ragged fur edge.
PELT_MASK = [
    "....oo....oo....",
    "....oxxxxxxo....",
    ".....xxxxxx.....",
    ".oo..xxxxxx..oo.",
    "oxxxxxxxxxxxxxxo",
    ".oxxxxxxxxxxxxo.",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    ".ooxxxxxxxxxxoo.",
    "oxxxxxxxxxxxxxxo",
    "oxxx.xxxxxx.xxxo",
    ".oo...xxxx...oo.",
    ".......oo.......",
]
FUR = ["#2a1b11", "#3a2618", "#4b3220", "#5d3f28", "#704d31", "#86603d"]
TIPS = ["#a07a4e", "#bc9666"]
OUTLINE = "#1a110b"
EYE = "#0d0907"
NOSE = "#120d0b"


def pelt():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = {(x, y) for y, row in enumerate(PELT_MASK) for x, c in enumerate(row) if c != "."}
    ragged = {(x, y) for y, row in enumerate(PELT_MASK) for x, c in enumerate(row) if c == "o"}
    for x, y in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if (x, y) in ragged or edge:
            im.putpixel((x, y), hexc(OUTLINE) if (x, y) not in ragged or h(x, y, 5) < 0.6 else hexc(FUR[0]))
            continue
        # light from the top left, a darker grizzled stripe down the spine, hanging strands
        light = -0.05 * (x - 7.5) - 0.06 * (y - 7)
        spine = 0.15 if x in (7, 8) and 4 <= y <= 12 else 0.0
        strand = h(x, y // 2 + (x % 3), 11)
        t = 0.45 + light + 0.35 * (strand - 0.5) - 0.12 * (y % 2 == 0 and h(x, y, 12) < 0.4)
        c = FUR[max(0, min(len(FUR) - 1, int(t * len(FUR))))]
        if spine and h(x, y, 13) < 0.45:
            c = TIPS[int(h(x, y, 14) * 2)]
        elif h(x, y, 15) < 0.08:
            c = TIPS[0]
        im.putpixel((x, y), hexc(c))
    for p, c in (((6, 2), EYE), ((9, 2), EYE), ((7, 3), NOSE), ((8, 3), NOSE), ((7, 2), FUR[1]), ((8, 2), FUR[1])):
        im.putpixel(p, hexc(c))
    return im


BEAR_FAT = [
    "................",
    "................",
    "................",
    ".....OOOOO......",
    "...OOWWWWLOO....",
    "..OWWWLLLLLLO...",
    ".OLWLLLLLLLMMO..",
    ".OLLLLLMLLMMMMO.",
    ".OMLLMMMMMMMMDO.",
    ".OMMMMMMMMMDDDO.",
    ".ORRMMMMMMDDDdO.",
    ".OrrRRRMMDDRRrO.",
    "..ODDDDDDDDddO..",
    "...OOOOOOOOOO...",
    "................",
    "................",
]
BEAR_FAT_PAL = {"O": "#8a7448", "W": "#fffbea", "L": "#f8efcf", "M": "#efe0ad", "D": "#d9c486", "d": "#bba560",
                "R": "#b9504a", "r": "#8c2f2c"}

BEAR_CLAW = [
    "................",
    "................",
    ".....OOO........",
    "....OHLLO.......",
    "...OHLLLMO......",
    "...OOOLLMMO.....",
    "......OLMMDO....",
    ".......OLMDO....",
    ".......OLMDDO...",
    "........OMDDO...",
    "........OMDDO...",
    "........OLMDO...",
    ".......ORRRRO...",
    ".......OrRrRO...",
    "........OOOO....",
    "................",
]
BEAR_CLAW_PAL = {"O": "#211a12", "H": "#efe6cf", "L": "#c9bb98", "M": "#9c8b6b", "D": "#6c5c44",
                 "R": "#5a3c26", "r": "#3c271a"}


def spawn_egg():
    D, K, M = "#2a1b11", "#14100c", "#a07a52"
    ears = [(3, 1), (4, 1), (11, 1), (12, 1), (3, 2), (12, 2)]
    paint = [(3, 1, D), (4, 1, D), (11, 1, D), (12, 1, D), (3, 2, D), (12, 2, D),
             (4, 2, "#5a3d27"), (11, 2, "#5a3d27")]
    # grizzled hump across the top of the egg
    paint += [(x, 3, "#b89466") for x in (6, 8, 10)] + [(x, 4, "#c8a676") for x in (5, 7, 9)]
    spots = [(4, 7, D), (11, 6, D), (6, 9, D), (10, 10, D), (3, 10, D), (12, 9, D)]
    # pale muzzle with a black nose, small dark eyes
    paint += [(6, 8, K), (9, 8, K)]
    paint += [(x, y, M) for x in range(6, 10) for y in (10, 11)] + [(7, 10, K), (8, 10, K), (7, 12, D), (8, 12, D)]
    return egg(["#9a6c43", "#7f5634", "#68452a", "#523520", "#3a2617", "#26180e"], spots, ears, paint)


def build():
    items = {
        "bear_pelt": pelt(),
        "bear_fat": from_grid(BEAR_FAT, BEAR_FAT_PAL),
        "bear_claw": from_grid(BEAR_CLAW, BEAR_CLAW_PAL),
        "brown_bear_spawn_egg": spawn_egg(),
    }
    for name, im in items.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(OUT / f"{name}.png")
    return items


if __name__ == "__main__":
    for n in build():
        print("wrote", n)
