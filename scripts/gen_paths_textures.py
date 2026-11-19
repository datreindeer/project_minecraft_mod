"""Top textures for Worn Grass and Trodden Path (TrampledPaths.java): the bare earth layer of each connection mask.

The block model lays this over vanilla's tinted grass_block_top, so transparent pixels show the biome's own grass and
the path blends into the Grass Blocks around it. Mask bits: north 1, east 2, south 4, west 8 (neighbour is path ground).
Toward a connected side the earth runs out to the edge in a band of fixed width (rows next to the edge carry no noise,
so two connected tiles meet seamlessly); toward the other sides it frays into grass with a noisy, dithered edge.

- trodden_path_top_<mask>: packed earth, path-tan, a darker trodden-in rim, pebbles. Band 12 px wide.
- worn_grass_top_<mask>: a narrow patchy band of bare dirt with grass clumps, specks and dead-grass straw. Band 8 px.

Run: python scripts/gen_paths_textures.py [preview.png]  (the optional argument renders a sample of winding paths)
"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/block"
N, E, S, W = 1, 2, 4, 8
C = 8.0  # tile centre in pixel coordinates (pixel x covers x..x+1)

STAGES = {
    "trodden_path": dict(
        hw=6.0, blob=5.7, amp=2.4, jitter=0.7, holes=0.14, specks=0.16, clumps=0.0, straw=0.0, pebbles=(1, 3), polish=True,
        earth=[(133, 107, 58), (144, 117, 64), (153, 128, 67), (170, 141, 74)], weights=[3, 8, 4, 1.4],
        rim=[(112, 88, 49), (122, 97, 54)], pebble=[(134, 129, 119), (156, 151, 141)], shadow=(98, 90, 80)),
    "worn_grass": dict(
        hw=5.2, blob=4.8, amp=2.2, jitter=0.9, holes=0.30, specks=0.14, clumps=0.6, straw=0.22, pebbles=(0, 1), polish=False,
        earth=[(106, 80, 50), (126, 97, 60), (138, 107, 66), (153, 120, 74)], weights=[2.5, 8, 4, 1.2],
        rim=[(102, 77, 48), (112, 85, 53)], pebble=[(128, 124, 116), (150, 146, 138)], shadow=(92, 86, 78),
        strawc=[(146, 137, 82), (126, 119, 70)],
        # the two rows at a connected edge: fixed and mirror-symmetric, so every neighbour (in any rotation) lines up
        edge=["...###.##.###...", "...#.######.#..."]),
}


def value_noise(rng, cell):
    """Smooth value noise on a lattice of `cell` px, sampled at pixel centres, in [0, 1]."""
    n = int(16 / cell) + 2
    g = [[rng.random() for _ in range(n + 1)] for _ in range(n + 1)]

    def f(x, y):
        gx, gy = (x + 0.5) / cell, (y + 0.5) / cell
        ix, iy = int(gx), int(gy)
        tx, ty = gx - ix, gy - iy
        tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
        a = g[iy][ix] + (g[iy][ix + 1] - g[iy][ix]) * tx
        b = g[iy + 1][ix] + (g[iy + 1][ix + 1] - g[iy + 1][ix]) * tx
        return a + (b - a) * ty
    return f


def edge_dist(x, y, mask):
    """Rows from the nearest connected edge (99 if none): noise fades to nothing there so neighbours line up."""
    d = 99
    if mask & N: d = min(d, y)
    if mask & S: d = min(d, 15 - y)
    if mask & W: d = min(d, x)
    if mask & E: d = min(d, 15 - x)
    return d


def score(x, y, mask, p):
    """Signed earthiness before noise: > 0 inside the path shape (blob in the middle, bands to connected sides)."""
    px, py = x + 0.5, y + 0.5
    s = p["blob"] - math.hypot(px - C, py - C)
    hw = p["hw"]
    if mask & N and py <= C: s = max(s, hw - abs(px - C))
    if mask & S and py >= C: s = max(s, hw - abs(px - C))
    if mask & W and px <= C: s = max(s, hw - abs(py - C))
    if mask & E and px >= C: s = max(s, hw - abs(py - C))
    return max(s, 3.0) if corner(x, y, mask) else s


def corner(x, y, mask):
    """In the quarter between two connected sides: filled in, so wide paths have no grass dots at block corners."""
    px, py = x + 0.5, y + 0.5
    return any(mask & a and mask & b and (px - C) * qx >= 0 and (py - C) * qy >= 0
               for a, b, qx, qy in ((N, E, 1, -1), (E, S, 1, 1), (S, W, -1, 1), (W, N, -1, -1)))


def shape(mask, p, rng):
    """16x16 booleans: True = bare earth."""
    n1, n2, cl = value_noise(rng, 4), value_noise(rng, 2), value_noise(rng, 3)
    earth = [[False] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            taper = min(1.0, max(0.0, (edge_dist(x, y, mask) - 1.5) / 3.0))
            v = score(x, y, mask, p) + taper * (p["amp"] * ((n1(x, y) - 0.5) * 2 + (n2(x, y) - 0.5) * 0.9)
                                                + p["jitter"] * (rng.random() - 0.5) * 2)
            e = v > 0
            if taper > 0 and abs(v) < 0.9:  # dithered fraying: grass holes inside, earth specks outside the line
                if e and rng.random() < p["holes"] * taper: e = False
                elif not e and rng.random() < p["specks"] * taper * 2: e = True
            if e and p["clumps"] and taper > 0.3 and cl(x, y) > p["clumps"] and v < 4.5:
                e = False  # worn grass: clumps of grass still standing in the bare band
            earth[y][x] = e
    for y in range(16):
        for x in range(16):
            for side, row, col in ((N, y, x), (S, 15 - y, x), (W, x, y), (E, 15 - x, y)):
                if "edge" in p and mask & side and row < 2 and not corner(x, y, mask):
                    earth[y][x] = p["edge"][row][col] == "#"
    # no lonely single grass pixel deep inside a trodden path, no stray earth pixel floating far out in the grass
    for y in range(16):
        for x in range(16):
            nb = [earth[y + dy][x + dx] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16]
            if not earth[y][x] and p["polish"] and all(nb) and score(x, y, mask, p) > 2.5:
                earth[y][x] = True
            if earth[y][x] and not any(nb) and score(x, y, mask, p) < -2.5:
                earth[y][x] = False
    return earth


def pick(rng, colours, weights):
    return rng.choices(colours, weights)[0]


def paint(mask, stage, seed):
    p = STAGES[stage]
    rng = random.Random(seed)
    earth = shape(mask, p, rng)
    tone = value_noise(rng, 3)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    def is_earth(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and earth[y][x]

    for y in range(16):
        for x in range(16):
            if not earth[y][x]:
                continue
            # off-tile neighbours count as earth on connected sides, as grass elsewhere
            def nb(dx, dy):
                xx, yy = x + dx, y + dy
                if 0 <= xx < 16 and 0 <= yy < 16:
                    return earth[yy][xx]
                side = N if yy < 0 else S if yy > 15 else W if xx < 0 else E
                return bool(mask & side)
            open_sides = sum(not nb(dx, dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if open_sides:
                # the turf edge: earth pressed down beside it, sometimes a flattened dead grass blade
                if p["straw"] and rng.random() < p["straw"]:
                    c = pick(rng, p["strawc"], [2, 1])
                else:
                    c = p["rim"][0] if open_sides > 1 or rng.random() < 0.5 else p["rim"][1]
            else:
                t = tone(x, y)
                w = list(p["weights"])
                w[0] *= 0.6 + (1 - t)  # darker hollows and lighter, polished humps follow a soft noise
                w[3] *= 0.4 + t * 1.6
                c = pick(rng, p["earth"], w)
            px[x, y] = c + (255,)
    # pebbles: a light stone with its shadow below-right, only well inside the earth
    lo, hi = p["pebbles"]
    for _ in range(rng.randint(lo, hi)):
        for _try in range(20):
            x, y = rng.randint(1, 13), rng.randint(1, 13)
            if all(is_earth(x + dx, y + dy) for dx in (-1, 0, 1, 2) for dy in (-1, 0, 1, 2)):
                px[x, y] = pick(rng, p["pebble"], [2, 1]) + (255,)
                if rng.random() < 0.45:
                    px[x + 1, y] = p["pebble"][0] + (255,)
                    px[x + 1, y + 1] = p["shadow"] + (255,)
                px[x, y + 1] = p["shadow"] + (255,)
                break
    return img


def rot(m):
    """The mask turned 90 degrees clockwise (seen from above): north -> east."""
    return ((m << 1) | (m >> 3)) & 15


def preview(path):
    """A sample of paths on plains grass: each tile picks a random rotation like the blockstate's weighted variants."""
    import zipfile, glob, io
    jars = glob.glob(str(Path.home() / "Documents/project_minecraft_mod/build/moddev/artifacts/minecraft-patched-*-merged.jar"))
    z = zipfile.ZipFile(jars[0])
    g = Image.open(io.BytesIO(z.read("assets/minecraft/textures/block/grass_block_top.png"))).convert("RGBA")
    tint = (145, 189, 89)
    gt = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            v = g.getpixel((x, y))[0]
            gt.putpixel((x, y), tuple(v * t // 255 for t in tint) + (255,))
    W_, H_ = 20, 14
    grid = [[None] * W_ for _ in range(H_)]
    rr = random.Random(5)
    # a winding path, a wider worn crossing and a few lone worn blocks
    for x in range(W_):
        y = int(6 + 3 * math.sin(x / 3.0))
        grid[y][x] = "trodden_path"
        if 0 < x < W_ - 1 and int(6 + 3 * math.sin((x + 1) / 3.0)) != y:
            grid[int(6 + 3 * math.sin((x + 1) / 3.0))][x] = "trodden_path"
    for y in range(H_):
        if grid[y][14] is None: grid[y][14] = "worn_grass"
        if y > 7: grid[y][4] = grid[y][5] = "trodden_path"
    grid[1][2] = grid[2][9] = "worn_grass"
    img = Image.new("RGBA", (W_ * 16, H_ * 16))
    tiles = {(s, m): Image.open(OUT / f"{s}_top_{m}.png") for s in STAGES for m in range(16)}
    for y in range(H_):
        for x in range(W_):
            img.paste(gt.rotate(-90 * rr.randrange(4)), (x * 16, y * 16))
            s = grid[y][x]
            if not s:
                continue
            m = 0
            for bit, dx, dy in ((N, 0, -1), (E, 1, 0), (S, 0, 1), (W, -1, 0)):
                if 0 <= x + dx < W_ and 0 <= y + dy < H_ and grid[y + dy][x + dx]:
                    m |= bit
            r = rr.randrange(4)
            base = m
            for _ in range(r):
                base = ((base >> 1) | (base << 3)) & 15  # turn back: the model drawn for this mask before rotation
            t = tiles[(s, base)].rotate(-90 * r)
            img.alpha_composite(t, (x * 16, y * 16))
    img.resize((W_ * 64, H_ * 64), Image.NEAREST).save(path)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for si, stage in enumerate(STAGES):
        for m in range(16):
            paint(m, stage, 1000 * si + 37 * m + 11).save(OUT / f"{stage}_top_{m}.png")
    print("path textures ok (32)")
    if len(sys.argv) > 1:
        preview(sys.argv[1])


if __name__ == "__main__":
    main()
