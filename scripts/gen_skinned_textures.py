"""Generate "skinned" (raw flesh) versions of vanilla animal body textures.

Usage: python gen_skinned_textures.py [SRC_ENTITY_DIR]
Output mirrors the source relative path under assets/mechanical/textures/entity/skinned/.
"""
import colorsys
import math
import re
import sys
from pathlib import Path

from PIL import Image

SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else Path(
    r"C:\Users\Yngve\AppData\Local\Temp\claude\43d83a9a335e657f\9ee37f73-4613-4afd-a1ce-400b3a021c61\scratchpad\src\assets\minecraft\textures\entity")
OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/skinned"

# chicken: plucked skin instead of flesh, in gen_birds2_textures.py
FOLDERS = ["cow", "pig", "sheep", "rabbit", "goat", "horse", "llama", "camel", "fox",
           "wolf", "cat", "parrot", "bear", "panda", "turtle", "frog", "armadillo", "sniffer",
           "axolotl", "bee", "strider"]
# Overlays / non-body layers and undead variants.
EXCLUDE = re.compile(r"(wool|markings|skeleton|zombie|spit|husk|angry|tame|collar|armor|stinger)")

DARK, MID, LIGHT = (110, 20, 25), (175, 45, 45), (215, 110, 105)
FAT, BONE, BLOOD = (235, 215, 190), (225, 215, 195), (90, 10, 15)


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def h(x, y, salt):
    # Deterministic per-pixel hash, avoids the diagonal lines a linear (x*a+y*b)%n pattern gives.
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000


def lum_of(r, g, b):
    return (0.299 * r + 0.587 * g + 0.114 * b) / 255


def is_bone(r, g, b):
    _, s, _ = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s < 0.15 and max(r, g, b) > 225


def flesh(x, y, detail, bone):
    if bone:
        return BONE
    # Procedural meat, independent of the fur pattern: horizontal muscle fibres + slow wave.
    t = 0.45 + 0.20 * (h(x // 4, y, 3) / 1000 - 0.5) + 0.08 * math.sin(y * 0.9 + x * 0.15)
    # Only small-scale detail from the original (eyes, nostrils, mouth edges), faint.
    t += max(-0.18, min(0.18, detail * 0.8))
    t = max(0.0, min(1.0, t))
    c = lerp(DARK, MID, t / 0.5) if t < 0.5 else lerp(MID, LIGHT, (t - 0.5) / 0.5)
    if h(x // 3, y, 1) < 55:  # fat/sinew streaks
        return lerp(c, FAT, 0.7)
    if h(x, y, 2) < 45:
        return lerp(c, BLOOD, 0.55)
    return c


def local_mean(lum, mask, W, H, r=3):
    """Mean luminance over opaque pixels in a (2r+1)^2 box, via integral images."""
    S = [[0.0] * (W + 1) for _ in range(H + 1)]
    C = [[0] * (W + 1) for _ in range(H + 1)]
    for y in range(H):
        for x in range(W):
            S[y + 1][x + 1] = lum[y][x] * mask[y][x] + S[y][x + 1] + S[y + 1][x] - S[y][x]
            C[y + 1][x + 1] = mask[y][x] + C[y][x + 1] + C[y + 1][x] - C[y][x]
    def m(x, y):
        x0, y0, x1, y1 = max(0, x - r), max(0, y - r), min(W, x + r + 1), min(H, y + r + 1)
        n = C[y1][x1] - C[y0][x1] - C[y1][x0] + C[y0][x0]
        return (S[y1][x1] - S[y0][x1] - S[y1][x0] + S[y0][x0]) / n
    return m


def skin(img):
    img = img.convert("RGBA")
    px = img.load()
    W, H = img.size
    opaque = [(x, y) for y in range(H) for x in range(W) if px[x, y][3]]
    lum = [[lum_of(*px[x, y][:3]) for x in range(W)] for y in range(H)]
    mask = [[1 if px[x, y][3] else 0 for x in range(W)] for y in range(H)]
    mean = local_mean(lum, mask, W, H)
    cand = {(x, y) for x, y in opaque if is_bone(*px[x, y][:3])}
    det = {(x, y): lum[y][x] - mean(x, y) for x, y in opaque}
    # Bone only for small white spots that stand out (eyes, teeth, horns), not white fur texture.
    bones = {(x, y) for (x, y) in cand if det[(x, y)] > 0.2 and sum(
        (x + dx, y + dy) in cand for dx in (-1, 0, 1) for dy in (-1, 0, 1)) <= 5}
    if len(bones) > 0.02 * len(opaque):  # that many white flecks = fur pattern (e.g. sheep)
        bones = set()
    for x, y in opaque:
        px[x, y] = (*flesh(x, y, det[(x, y)], (x, y) in bones), px[x, y][3])
    return img


def main():
    written = []
    for folder in FOLDERS:
        for src in sorted((SRC / folder).rglob("*.png")):
            if EXCLUDE.search(src.stem):
                continue
            rel = src.relative_to(SRC)
            dst = OUT / rel
            dst.parent.mkdir(parents=True, exist_ok=True)
            orig = Image.open(src)
            out = skin(orig)
            assert out.size == orig.size
            assert out.getchannel("A").tobytes() == orig.convert("RGBA").getchannel("A").tobytes()
            out.save(dst)
            written.append(rel.as_posix())
    print(f"wrote {len(written)} files")
    for w in written:
        print(" ", w)


if __name__ == "__main__":
    main()
