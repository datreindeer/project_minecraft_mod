"""Folklore textures (gen_folklore.py, com.mechanical.folklore). Deterministic: hashes only, no randomness.

- entity/wildlife/mountain_watcher.png: the golden eagle's 64x64 skin (same model, client.folklore.FolkloreClient) turned
  pale: luminance mapped onto bone-white to silver-grey, with a faint pearly shimmer (scattered pale cyan/violet texels).
- item/mountain_watcher_spawn_egg.png, item/watcher_feather.png: the golden eagle egg and Eagle Feather recoloured the same way.
- item/boiled_fly_agaric.png: the Fly Agaric item, the red cooked down to a dull orange-brown, the spots greyed.
- block/glimmering_stump_side/_top.png: vanilla oak log bark and rings (from the merged jar) overgrown with moss.
- mob_effect/second_sight.png: an 18x18 violet eye.
Run: python scripts/gen_folklore_textures.py (needs a gradle build for the merged jar).
"""
import glob
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))
assert JAR, "no merged minecraft jar (run a gradle build first)"


def hsh(x, y, salt):
    n = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def luma(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def pale(src, salt):
    """Golden-brown plumage to pale: dark parts silver-grey, light parts bone-white, a pearly glint here and there."""
    im = src.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = min(1.0, luma((r, g, b)) * 1.6 + 0.15)
            c = lerp((118, 124, 138), (246, 243, 232), l)
            k = hsh(x, y, salt)
            if k > 0.93:
                c = lerp(c, (200, 236, 250) if k > 0.965 else (224, 206, 250), 0.6)  # the shimmer
            px[x, y] = (*c, a)
    return im


def boiled(src):
    im = src.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = luma((r, g, b))
            if r > g + 40:  # the red cap: cooked down to dull orange-brown
                c = lerp((92, 52, 30), (176, 110, 58), min(1, l * 2.2))
            elif l > 0.7:  # white spots and stem: greyed, soaked
                c = lerp((r, g, b), (190, 176, 150), 0.55)
            else:
                c = lerp((r, g, b), (110, 90, 70), 0.3)
            px[x, y] = (*c, a)
    return im


def mossy(src, salt, amount):
    im = src.convert("RGBA")
    px = im.load()
    mosses = [(62, 92, 34), (78, 112, 40), (96, 132, 50), (54, 80, 30)]
    for y in range(im.height):
        for x in range(im.width):
            # thicker towards the foot of the bark (and the rim of the top), clumped by a coarse hash
            lean = (y / (im.height - 1)) if salt == 1 else min(x, y, 15 - x, 15 - y) < 2
            clump = hsh(x // 2, y // 2, salt + 7)
            if clump * 0.6 + hsh(x, y, salt) * 0.4 < amount * (0.5 + float(lean)):
                px[x, y] = (*mosses[int(hsh(x, y, salt + 3) * 4) % 4], 255)
    return im


def eye():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = im.load()
    for y in range(18):
        for x in range(18):
            dx, dy = (x - 8.5) / 8, (y - 8.5) / 4.6
            d = dx * dx + dy * dy
            if d > 1:
                continue
            r = ((x - 8.5) ** 2 + (y - 8.5) ** 2) ** 0.5
            if d > 0.72:
                px[x, y] = (88, 40, 140, 255)  # lid line
            elif r < 1.6:
                px[x, y] = (24, 10, 40, 255)  # pupil
            elif r < 3.6:
                px[x, y] = lerp((150, 90, 230), (210, 170, 255), hsh(x, y, 9))  # iris
            else:
                px[x, y] = (236, 230, 246, 255)
    px[7, 7] = (255, 255, 255, 255)  # glint
    return im


def main():
    jar = zipfile.ZipFile(JAR[0])
    vanilla = lambda p: Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{p}")))
    out = {
        "entity/wildlife/mountain_watcher.png": pale(Image.open(TEX / "entity/wildlife/golden_eagle.png").convert("RGBA"), 11),
        "item/mountain_watcher_spawn_egg.png": pale(Image.open(TEX / "item/golden_eagle_spawn_egg.png").convert("RGBA"), 12),
        "item/watcher_feather.png": pale(Image.open(TEX / "item/eagle_feather.png").convert("RGBA"), 13),
        "item/boiled_fly_agaric.png": boiled(Image.open(TEX / "item/fly_agaric.png").convert("RGBA")),
        "block/glimmering_stump_side.png": mossy(vanilla("block/oak_log.png"), 1, 0.45),
        "block/glimmering_stump_top.png": mossy(vanilla("block/oak_log_top.png"), 2, 0.4),
        "mob_effect/second_sight.png": eye(),
    }
    for path, im in out.items():
        (TEX / path).parent.mkdir(parents=True, exist_ok=True)
        im.save(TEX / path)
    print(f"folklore textures: {len(out)}")


if __name__ == "__main__":
    main()
