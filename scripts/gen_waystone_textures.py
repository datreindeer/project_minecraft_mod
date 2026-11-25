"""Textures for the Waystones (worldgen/Waystones.java, data: gen_waystone.py): seven looks of carved standing stone, one per
kind of land (mossy granite, wind-worn sandstone, rimed frost stone, banded red rock, sooty blackstone, pale chalk, lichened
grey granite). Per look:
- waystone_<look>_side / _top / _base: the dressed stone (tool marks, grain, weathering of its land) and the rough footing
- waystone_<look>_front_lower / _middle / _upper: the rune faces: a carved frame and runes cut as grooves (dark stroke, a lit
  lower lip): Ehwaz (the horse) low, Raidho and Gebo (the road, the gift) in the middle, Algiz and a ring (the warding, the
  way round) at the top
- waystone_<look>_glow_lower / _middle / _upper: just the rune strokes in the look's colour, drawn full bright by the model
Face coordinates: the lower face is v 0..13 (block y 16..3), the middle v 0..16, the upper shaft v 8..16, taper v 4..8, head v 2..4.
Standalone. Run: python scripts/gen_waystone_textures.py"""
import math
import random
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/block"

# base stone, dark, light, accent (weathering), accent 2, glow
LOOKS = {
    "mossy": ((112, 117, 108), (70, 76, 70), (150, 155, 144), (78, 112, 50), (112, 146, 64), (124, 255, 176)),
    "sand": ((212, 186, 130), (168, 138, 88), (236, 216, 168), (190, 158, 100), (226, 204, 150), (255, 200, 96)),
    "frost": ((168, 180, 192), (112, 124, 140), (206, 216, 226), (242, 248, 255), (178, 216, 238), (158, 232, 255)),
    "red": ((172, 90, 56), (120, 58, 36), (206, 128, 82), (92, 48, 34), (214, 150, 96), (255, 138, 74)),
    "dark": ((54, 50, 60), (30, 28, 34), (84, 80, 92), (104, 100, 104), (70, 60, 84), (196, 146, 255)),
    "chalk": ((210, 207, 194), (166, 162, 150), (232, 230, 220), (176, 182, 108), (150, 156, 150), (143, 184, 255)),
    "grey": ((126, 126, 124), (84, 84, 84), (164, 164, 160), (198, 148, 66), (122, 140, 82), (127, 224, 224)),
}


def clamp(c):
    return tuple(max(0, min(255, int(round(v)))) for v in c)


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def noise(seed, cells, w=16, h=16):
    """Smooth value noise 0..1 on a cells x cells lattice."""
    rng = random.Random(seed)
    g = [[rng.random() for _ in range(cells + 2)] for _ in range(cells + 2)]
    out = [[0.0] * w for _ in range(h)]
    for y in range(h):
        for x in range(w):
            fx, fy = x * cells / w, y * cells / h
            ix, iy = int(fx), int(fy)
            tx, ty = fx - ix, fy - iy
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a, b, c, d = g[iy][ix], g[iy][ix + 1], g[iy + 1][ix], g[iy + 1][ix + 1]
            out[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


def stone(look, seed, kind):
    """The dressed stone of a look. kind: side (a face, v 0 at the top), top, base (rough footing)."""
    base, dark, light, acc, acc2, _ = LOOKS[look]
    rng = random.Random(seed)
    n1, n2 = noise(seed, 3), noise(seed + 7, 6)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = n1[y][x] * 0.6 + n2[y][x] * 0.4
            c = mix(dark, light, 0.2 + v * 0.65)
            c = mix(c, base, 0.45)
            if kind == "side" and x % 3 == 0:  # faint vertical tool marks of the dressing
                c = mix(c, dark, 0.12)
            if kind == "base":
                c = mix(c, dark, 0.25)
            r = rng.random()
            if r < 0.07:
                c = mix(c, dark, 0.35)  # grain
            elif r < 0.12:
                c = mix(c, light, 0.35)
            img.putpixel((x, y), clamp(c) + (255,))

    def put(x, y, c, t=1.0):
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), clamp(mix(img.getpixel((x, y))[:3], c, t))[:3] + (255,))

    n3 = noise(seed + 13, 4)
    if look == "mossy":  # moss creeping up from the ground and settling on the top
        for y in range(16):
            for x in range(16):
                reach = (y / 15) if kind == "side" else (0.9 if kind == "base" else 0.55)
                if n3[y][x] * 0.8 + reach * 0.6 > 0.82:
                    put(x, y, acc2 if (x * 7 + y * 3) % 5 == 0 else acc, 0.85)
    elif look == "sand":  # wind-scoured bands and drifted sand on the footing
        for y in range(16):
            band = math.sin(y * 1.3 + n3[y][0] * 2) * 0.5 + 0.5
            for x in range(16):
                put(x, y, acc2 if band > 0.75 else acc, 0.18 if kind != "top" else 0.1)
        if kind in ("base", "top"):
            for y in range(16):
                for x in range(16):
                    if n3[y][x] > 0.55:
                        put(x, y, acc2, 0.6)
    elif look == "frost":  # rime on top, frost crystals in the grain, snow on the footing
        for y in range(16):
            for x in range(16):
                if kind == "top" and n3[y][x] > 0.3 or kind == "base" and n3[y][x] > 0.45:
                    put(x, y, acc, 0.9)
                elif kind == "side" and y < 3 and n3[y][x] > 0.4 - y * 0.1:
                    put(x, y, acc, 0.75)
                elif rng.random() < 0.06:
                    put(x, y, acc2, 0.6)
    elif look == "red":  # strata and dark desert varnish running down
        for y in range(16):
            if y % 5 in (1, 3):
                for x in range(16):
                    put(x, y, acc2 if y % 5 == 1 else dark, 0.25)
        if kind == "side":
            for x in rng.sample(range(16), 3):
                for y in range(rng.randrange(0, 4), rng.randrange(8, 16)):
                    put(x, y, acc, 0.4)
    elif look == "dark":  # pale ash in the cracks, a purple sheen
        for _ in range(2):
            x, y = rng.randrange(16), rng.randrange(16)
            for _ in range(7):
                put(x, y, acc, 0.6)
                x, y = x + rng.choice((-1, 0, 1)), y + 1
        for y in range(16):
            for x in range(16):
                if n3[y][x] > 0.7:
                    put(x, y, acc2, 0.35)
    else:  # chalk and grey: lichen rosettes
        for _ in range(3 if kind != "base" else 5):
            cx, cy, col = rng.randrange(16), rng.randrange(16), acc if rng.random() < 0.6 else acc2
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    if abs(dx) + abs(dy) < 2 or rng.random() < 0.4:
                        put(cx + dx, cy + dy, col, 0.75 if dx == dy == 0 else 0.5)
    if kind == "base":  # a few cracks in the rough footing
        for _ in range(2):
            x, y = rng.randrange(16), rng.randrange(16)
            for _ in range(5):
                put(x, y, dark, 0.6)
                x, y = x + rng.choice((-1, 1)), y + rng.choice((0, 1))
    return img


# ---------------------------------------------------------------- the carving: F frame (not lit), # rune strokes (lit)
LOWER = [  # rows v 0..12 of the lower face (the panel ends on a bottom frame line above the footing): Ehwaz and a diamond
    "....F......F....",
    "....F......F....",
    "....F......F....",
    "....F#....#F....",
    "....F##..##F....",
    "....F#.##.#F....",
    "....F#....#F....",
    "....F......F....",
    "....F..##..F....",
    "....F.#..#.F....",
    "....F..##..F....",
    "....F......F....",
    "....FFFFFFFF....",
    "................", "................", "................",
]
MIDDLE = [  # rows v 0..15 of the middle face: Raidho above Gebo
    "....F......F....",
    "....F......F....",
    "....F.##...F....",
    "....F.#.#..F....",
    "....F.##...F....",
    "....F.#.#..F....",
    "....F.#..#.F....",
    "....F......F....",
    "....F......F....",
    "....F.#..#.F....",
    "....F..##..F....",
    "....F..##..F....",
    "....F.#..#.F....",
    "....F......F....",
    "....F......F....",
    "....F......F....",
]
UPPER = [  # rows v 0..15 of the upper face: head 2..3 (the eye), taper 4..7 (Algiz), shaft 8..15 (the ring)
    "................",
    "................",
    ".......##.......",
    ".......##.......",
    ".....#.##.#.....",
    "......####......",
    ".......##.......",
    "................",
    "....FFFFFFFF....",
    "....F......F....",
    "....F..##..F....",
    "....F.#..#.F....",
    "....F.#..#.F....",
    "....F..##..F....",
    "....F......F....",
    "....F......F....",
]


def carve(img, mask, look):
    """Grooves: the stroke dark, the pixel under it lit (light from above), a faint tint of the glow round runes."""
    base, dark, light, _, _, glow = LOOKS[look]
    groove = mix(dark, (0, 0, 0), 0.35)
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            img.putpixel((x, y), clamp(groove if ch == "#" else mix(groove, base, 0.25)) + (255,))
            if y + 1 < 16 and mask[y + 1][x] == ".":
                img.putpixel((x, y + 1), clamp(mix(img.getpixel((x, y + 1))[:3], light, 0.55)) + (255,))
            if ch == "#":
                for dx, dy in ((1, 0), (-1, 0), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 16 and 0 <= ny < 16 and mask[ny][nx] == ".":
                        img.putpixel((nx, ny), clamp(mix(img.getpixel((nx, ny))[:3], glow, 0.18)) + (255,))
    return img


def glow_layer(mask, look):
    glow = LOOKS[look][5]
    core = mix(glow, (255, 255, 255), 0.45)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(mask):
        for x, ch in enumerate(row):
            if ch == "#":
                img.putpixel((x, y), clamp(core if (x * 3 + y) % 4 == 0 else glow) + (255,))
    for y, row in enumerate(mask):  # a soft halo in the stone round the strokes
        for x, ch in enumerate(row):
            if ch != "#":
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and mask[ny][nx] != "#" and img.getpixel((nx, ny))[3] == 0:
                    img.putpixel((nx, ny), clamp(mix(glow, (0, 0, 0), 0.3)) + (90,))
    return img


for i, look in enumerate(LOOKS):
    seed = 9100 + i * 31
    stone(look, seed, "side").save(TEX / f"waystone_{look}_side.png")
    stone(look, seed + 1, "top").save(TEX / f"waystone_{look}_top.png")
    stone(look, seed + 2, "base").save(TEX / f"waystone_{look}_base.png")
    carve(stone(look, seed + 3, "side"), LOWER, look).save(TEX / f"waystone_{look}_front_lower.png")
    carve(stone(look, seed + 4, "side"), UPPER, look).save(TEX / f"waystone_{look}_front_upper.png")
    carve(stone(look, seed + 5, "side"), MIDDLE, look).save(TEX / f"waystone_{look}_front_middle.png")
    glow_layer(MIDDLE, look).save(TEX / f"waystone_{look}_glow_middle.png")
    glow_layer(LOWER, look).save(TEX / f"waystone_{look}_glow_lower.png")
    glow_layer(UPPER, look).save(TEX / f"waystone_{look}_glow_upper.png")
print("waystone textures ok")
