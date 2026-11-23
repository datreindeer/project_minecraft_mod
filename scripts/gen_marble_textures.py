"""Marble textures (the marble stone family, gen_stone_families.py): painted here instead of recoloured from quartz.

Raw Marble is natural rock: white-grey, cloudy, with soft grey veins that wander and fade out inside the tile (never
crossing it, so neighbours never cut a line off). Four variants (marble_side, _2, _3, _4; marble_top is for slabs and
stairs); gen_stone_families.py (TURNED) writes a blockstate that picks one at random and turns it every way, so a cliff
never shows the same tile in a lattice. Every texture tiles with itself.
Smooth Marble (marble_bottom) is clean and nearly white with faint veins; Bricks are polished ashlar with crisp mortar;
Pillar and Chiseled Marble are carved in the same stone. Cracked and Mossy Marble Bricks are made from marble_bricks by
gen_stone_families.py, so run this first.

Standalone texture script. Run: python scripts/gen_marble_textures.py, then gen_stone_families.py and gen_blend_edges.py
(writes marble_preview.png in the repo root, not committed).
"""
import math
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = Path(sys.argv[1]) if len(sys.argv) > 1 else ROOT / "src/main/resources/assets/mechanical/textures/block"  # arg: try-out folder
N = 16
Y, X = np.mgrid[0:N, 0:N].astype(float)

# one ramp, dark (vein) -> light (polished white), slightly cool in the greys and warm in the whites
RAMP = [(146, 150, 158), (166, 169, 176), (184, 187, 192), (199, 201, 205), (212, 213, 215), (223, 223, 223), (232, 231, 229),
        (240, 239, 236), (247, 246, 243)]


def noise(rng, cell):
    """Tileable value noise, a lattice point every `cell` px, smoothly interpolated (0..1). Fine grain only: a lattice
    of 2x2 or less per tile is a visible egg-crate, so the large shapes come from waves() instead."""
    g = N // cell
    grid = rng.random((g, g))
    u, v = (X + 0.5) / cell, (Y + 0.5) / cell
    i0, j0 = np.floor(u).astype(int) % g, np.floor(v).astype(int) % g
    fu, fv = u - np.floor(u), v - np.floor(v)
    fu, fv = fu * fu * (3 - 2 * fu), fv * fv * (3 - 2 * fv)
    a, b = grid[j0, i0], grid[j0, (i0 + 1) % g]
    c, d = grid[(j0 + 1) % g, i0], grid[(j0 + 1) % g, (i0 + 1) % g]
    return (a * (1 - fu) + b * fu) * (1 - fv) + (c * (1 - fu) + d * fu) * fv


def waves(rng, kmax=3, power=1.4):
    """Tileable irregular field (0..1): random waves with whole-number frequencies up to kmax per tile, weaker the finer."""
    n = np.zeros((N, N))
    for kx in range(-kmax, kmax + 1):
        for ky in range(0, kmax + 1):
            if ky == 0 and kx <= 0:
                continue
            k = math.hypot(kx, ky)
            n += rng.normal() * k ** -power * np.cos(2 * math.pi * (kx * X + ky * Y) / N + rng.uniform(0, 2 * math.pi))
    return (n - n.min()) / (n.max() - n.min() + 1e-9)


def fbm(rng, kmax=4):
    return waves(rng, kmax)


def vein(rng, fx, fy, width, wander):
    """A tileable vein: the zero lines of sin(pi (fx x + fy y) / 16 + turbulence), max(|fx|, |fy|) lines per tile.
    Returns (core 0..1, halo 0..1)."""
    t = waves(rng, 3, 1.2) - 0.5
    s = np.abs(np.sin(math.pi * (fx * X + fy * Y) / N + wander * t))
    return np.clip(1 - s / width, 0, 1) ** 1.3, np.clip(1 - s / (width * 3.2), 0, 1)


def to_image(light, rng, grain):
    """Lightness 0..1 -> the ramp, a little random dither so the steps break up like real grain."""
    idx = np.clip(np.rint(light * (len(RAMP) - 1) + rng.normal(0, grain, light.shape)), 0, len(RAMP) - 1).astype(int)
    img = Image.new("RGBA", (N, N))
    for y in range(N):
        for x in range(N):
            img.putpixel((x, y), RAMP[idx[y, x]] + (255,))
    return img


def wisps(rng, n, length, angle, spread=0.9, wobble=0.2, branch=0.25):
    """n soft veins that start and end inside the tile (tapered ends, wrapping round its edges), roughly along `angle`
    (radians), some with a short branch. Never crossing the whole tile, so random neighbours never cut a line off.
    Returns (core, halo) 0..1."""
    core, halo = np.zeros((N, N)), np.zeros((N, N))

    def walk(x, y, a, steps):
        pts = []
        for _ in range(steps):
            pts.append((x, y))
            a += rng.normal(0, wobble)  # gently wandering
            x, y = x + math.cos(a), y + math.sin(a)
        for i, (px, py) in enumerate(pts):
            t = min(1.0, (min(i, len(pts) - 1 - i) + 1) / 3.0)
            ix, iy = int(round(px)) % N, int(round(py)) % N
            core[iy, ix] = max(core[iy, ix], t)
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                halo[(iy + dy) % N, (ix + dx) % N] = max(halo[(iy + dy) % N, (ix + dx) % N], t)
        return pts

    for _ in range(n):
        a = angle + rng.uniform(-spread, spread) / 2
        pts = walk(rng.uniform(0, N), rng.uniform(0, N), a, int(rng.integers(length[0], length[1] + 1)))
        if rng.random() < branch:  # a short branch off the middle
            x, y = pts[len(pts) // 2]
            walk(x, y, a + rng.choice([-1, 1]) * rng.uniform(0.6, 1.1), int(rng.integers(3, 6)))
    return core, np.maximum(halo - core, 0)


def raw(seed, angle):
    """Natural marble: cloudy white-grey, a few soft grey veins that wander, a pit or two, a glint of crystal."""
    rng = np.random.default_rng(seed)
    light = 0.71 + 0.22 * (waves(rng, 2, 1.2) - 0.5) + 0.06 * (noise(rng, 2) - 0.5)
    core, halo = wisps(rng, 2, (10, 15), angle)
    drift, drift_halo = wisps(rng, 1, (12, 16), angle + 0.3, wobble=0.15, branch=0)  # a broad, faint grey drift
    light = light - 0.30 * core - 0.10 * halo - 0.09 * drift - 0.07 * drift_halo
    light -= 0.15 * (noise(rng, 1) > 0.965)  # pits in the weathered face
    light += 0.12 * (noise(rng, 1) > 0.975)  # calcite crystals catching the light
    return to_image(light, rng, 0.16)


def smooth(seed):
    """Polished: brighter, finer, the veins only a breath of grey."""
    rng = np.random.default_rng(seed)
    light = 0.86 + 0.07 * (waves(rng, 2, 1.2) - 0.5)
    core, halo = wisps(rng, 2, (10, 15), 0.9, wobble=0.18, branch=0)
    light = light - 0.10 * core - 0.03 * halo
    return to_image(light, rng, 0.10)


def bricks(seed):
    """Two courses of ashlar, joints offset by half a block; 1 px mortar, each stone bevelled and veined on its own."""
    rng = np.random.default_rng(seed)
    light = np.zeros((N, N))
    stones = [((0, 15), (0, 7), 0), ((0, 7), (8, 15), 1), ((8, 15), (8, 15), 2)]  # (x0, x1), (y0, y1)
    for (x0, x1), (y0, y1), k in stones:
        part = 0.84 + 0.05 * (fbm(rng) - 0.5) + rng.uniform(-0.025, 0.025)
        core, halo = wisps(rng, 1, (6, 9), rng.uniform(0, 3))
        part = part - 0.15 * core - 0.04 * halo
        light[y0:y1 + 1, x0:x1 + 1] = part[y0:y1 + 1, x0:x1 + 1]
    # the stones of row 2 wrap round the tile edge: x 8..15 then 0..6 of the next tile is one stone
    light[8:16, 0:7] = light[8:16, 8:15].mean() + (light[8:16, 0:7] - light[8:16, 0:7].mean()) * 0.6
    for (x0, x1), (y0, y1), _ in [((0, 14), (0, 6), 0), ((8, 15), (8, 14), 0), ((0, 6), (8, 14), 0)]:
        light[y0, x0:x1 + 1] += 0.07  # top edge catches the light
        light[y1, x0:x1 + 1] -= 0.06  # under edge in shade
    light[0:7, 0] += 0.03
    light[8:15, 8] += 0.03
    light[0:7, 14] -= 0.03
    light[8:15, 6] -= 0.03
    mortar = 0.22
    light[7, :] = light[15, :] = mortar
    light[0:7, 15] = mortar
    light[8:15, 7] = mortar
    return to_image(light, rng, 0.12)


# carved stone, as ramp positions (0 darkest vein grey .. 1 polished white): light from the top left
HI, FACE, PANEL, LIP, MID, SHADE, DARK = 1.0, 0.8, 0.66, 0.92, 0.56, 0.36, 0.26


def pillar_side(seed):
    """Fluted column: four flutes lit from the left, a darker arris at each edge, a faint vein running up."""
    rng = np.random.default_rng(seed)
    g1, g2 = 0.42, 0.62  # each flute: shaded left wall, half-lit right wall
    cols = [DARK, 0.95, g1, g2, 0.95, g1, g2, 0.95, 0.9, g1, g2, 0.95, g1, g2, 0.9, 0.32]
    light = np.tile(np.array(cols), (N, 1)) + 0.04 * (fbm(rng) - 0.5)
    core, _ = wisps(rng, 1, (8, 12), math.pi / 2, 0.4)
    light -= 0.08 * core
    return to_image(light, rng, 0.08)


def pillar_top(seed):
    """End of the column: a dark arris, a bright fillet, a groove ring, then the polished face with a faint ring."""
    rng = np.random.default_rng(seed)
    inset = np.minimum(np.minimum(X, N - 1 - X), np.minimum(Y, N - 1 - Y))
    r = np.hypot(X - 7.5, Y - 7.5)
    light = 0.84 + 0.04 * (fbm(rng) - 0.5)
    light = np.where(inset == 0, DARK, np.where(inset == 1, 0.95, np.where(inset == 2, 0.5, light)))
    light = np.where((inset > 2) & (np.abs(r - 3.6) < 0.55), 0.64, light)
    light = np.where(r < 1.2, 0.95, light)
    return to_image(light, rng, 0.08)


# chiseled faces drawn by hand: W lit edge, L half-lit lip, F face, P sunk panel, M half-shaded edge, S shadow, D dark rim
SHADES = {"W": HI, "L": LIP, "F": FACE, "P": PANEL, "M": MID, "S": SHADE, "D": DARK}
FRAME = ["WWWWWWWWWWWWWWWD",
         "WFFFFFFFFFFFFFFD",
         "WFSSSSSSSSSSSSLD"] + ["WFS..........LFD"] * 10 + [
         "WFSLLLLLLLLLLLFD",
         "WFFFFFFFFFFFFFFD",
         "DDDDDDDDDDDDDDDD"]
LOZENGE = ["PPPPPPPPPP",  # a raised lozenge with a small sunk boss
           "PPPPWLPPPP",
           "PPPWFFLPPP",
           "PPWFFFFLPP",
           "PWFFSMFFLP",
           "PMFFMWFFSP",
           "PPMFFFFSPP",
           "PPPMFFSPPP",
           "PPPPMSPPPP",
           "PPPPPPPPPP"]
MEDALLION = ["PPPPPPPPPP",  # a raised disc in a sunk ring, a boss in the middle
             "PPPSSSSPPP",
             "PPSFFFFLPP",
             "PSFFFFFFLP",
             "PSFFWMFFLP",
             "PSFFMSFFLP",
             "PSFFFFFFLP",
             "PPSFFFFLPP",
             "PPPLLLLPPP",
             "PPPPPPPPPP"]


def carved(seed, motif):
    """The frame (bevelled rim, flat band, sunk groove) round a sunk panel carrying the motif; a faint grain over all."""
    rng = np.random.default_rng(seed)
    rows = [r[:3] + motif[i - 3] + r[13:] if 3 <= i < 13 else r for i, r in enumerate(FRAME)]
    light = np.array([[SHADES[c] for c in r] for r in rows]) + 0.03 * (fbm(rng) - 0.5)
    return to_image(light, rng, 0.05)


# natural variants: (main vein direction, faint vein direction); mirrored and turned by the blockstate as well
RAW = {"": 1.0, "_2": 0.4, "_3": 2.2, "_4": 1.6}  # main vein direction (radians) of each variant
out = {}
for i, (suffix, dirs) in enumerate(RAW.items()):
    out["marble_side" + suffix] = raw(100 + i, dirs)
out["marble_top"] = raw(200, 2.3)  # slabs and stairs
out["marble_bottom"] = smooth(300)
out["marble_bricks"] = bricks(400)
out["marble_pillar_side"] = pillar_side(500)
out["marble_pillar_top"] = pillar_top(600)
out["chiseled_marble"] = carved(700, LOZENGE)
out["chiseled_marble_top"] = carved(800, MEDALLION)
for name, img in out.items():
    img.save(OUT / f"{name}.png")

# preview: every texture 16x, and a 8x8 wall of random raw variants (mirrored and turned like the blockstate) at 6x
rng = np.random.default_rng(1)
sheet = Image.new("RGBA", (6 * 16, 48))
for i, img in enumerate(out.values()):
    sheet.paste(img, ((i % 6) * 16, (i // 6) * 16))
sheet = sheet.resize((sheet.width * 16, sheet.height * 16), Image.NEAREST)
wall = Image.new("RGBA", (128, 128))
for y in range(8):
    for x in range(8):
        t = out["marble_side" + list(RAW)[rng.integers(len(RAW))]]
        wall.paste(t.rotate(90 * int(rng.integers(4))), (x * 16, y * 16))
built = Image.new("RGBA", (128, 128))
for y in range(8):
    for x in range(8):
        t = out["marble_bricks" if y < 3 else "marble_pillar_side" if x in (1, 6) else "marble_bottom" if y < 7 else "chiseled_marble"]
        built.paste(t.rotate(90 * int(rng.integers(4))) if t is out["marble_bottom"] else t, (x * 16, y * 16))
far = Image.new("RGBA", (256, 256))  # 16x16 blocks at 1 px per texel: how a cliff reads from afar
for y in range(16):
    for x in range(16):
        t = out["marble_side" + list(RAW)[rng.integers(len(RAW))]]
        far.paste(t.rotate(90 * int(rng.integers(4))), (x * 16, y * 16))
far.save((OUT if len(sys.argv) > 1 else ROOT) / "marble_far.png")
prev = Image.new("RGBA", (1536, 768 + 768), (60, 60, 60, 255))
prev.paste(sheet, (0, 0))
prev.paste(wall.resize((768, 768), Image.NEAREST), (0, 768))
prev.paste(built.resize((768, 768), Image.NEAREST), (768, 768))
prev.save((OUT if len(sys.argv) > 1 else ROOT) / "marble_preview.png")
print(f"{len(out)} marble textures ok")
