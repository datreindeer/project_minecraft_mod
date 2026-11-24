"""Seasonal looks of deciduous leaves (client.SeasonalFoliage): per leaves type a bare-branches texture (twigs in that
tree's bark colours on a transparent tile), the same with buds (pink blossom for cherry), and for untinted leaves a grey
copy of the leaves that the game tints in autumn. Writes textures/block/seasonal/*.png and models/block/seasonal/*.json.
Standalone (no other generator depends on it). Run: python scripts/gen_seasonal_leaves_textures.py"""
import io
import json
import math
import random
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
ASSETS = ROOT / "src/main/resources/assets/mechanical"
TEX = ASSETS / "textures/block"
OUT_TEX = TEX / "seasonal"
OUT_MODEL = ASSETS / "models/block/seasonal"
# must match SeasonalFoliage.DECIDUOUS / AUTUMN_MODEL
VANILLA = ["oak", "dark_oak", "birch", "cherry", "red_poplar", "orange_poplar", "yellow_poplar"]
MOD = ["aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "rowan", "bird_cherry", "wild_apple", "plum"]
AUTUMN_MODEL = ["cherry", "aspen", "alder", "beech", "linden", "willow", "ash", "hazel"]  # fruit trees: bare + buds only
LOG = {"red_poplar": "poplar", "orange_poplar": "poplar", "yellow_poplar": "poplar"}
TWIG_BROWN = (74, 54, 38)
BUDS = {"cherry": [(236, 160, 192, 255), (250, 204, 220, 255)], "wild_apple": [(214, 110, 146, 255), (246, 206, 218, 255)],
        "plum": [(232, 232, 220, 255), (250, 250, 244, 255)]}
GREEN_BUDS = [(124, 170, 58, 255), (162, 204, 84, 255)]


def texture(jar, leaves, name):
    """A block texture: ours for our woods, vanilla's from the game jar otherwise."""
    if leaves in MOD:
        return Image.open(TEX / f"{name}.png").convert("RGBA")
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def bark(jar, leaves):
    """Dark, mid and light twig colours: the log's bark tones, pulled toward a twig brown (birch twigs are not white)."""
    log = LOG.get(leaves, leaves)
    img = texture(jar, leaves, f"{log}_log")
    px = sorted((p for p in img.getdata() if p[3] > 0), key=lambda p: 0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2])
    pull = 0.75 if leaves == "birch" else 0.4
    tones = []
    for q in (0.2, 0.5, 0.8):
        p = px[int(q * (len(px) - 1))]
        tones.append(tuple(int(p[i] * (1 - pull) + TWIG_BROWN[i] * pull) for i in range(3)) + (255,))
    return tones


def grow(img, x, y, angle, length, rnd, tones, depth, tips):
    """A crooked twig, forking now and then; light top pixel, dark lower ones."""
    dark, mid, light = tones
    for i in range(length):
        px, py = int(x), int(y)
        if not (0 <= px < 16 and 0 <= py < 16):
            return
        img.putpixel((px, py), light if depth == 0 and i % 3 == 0 else (mid if (i + depth) % 2 else dark))
        if depth < 2 and i > 1 and rnd.random() < 0.22:
            grow(img, x, y, angle + rnd.choice((-1, 1)) * rnd.uniform(0.6, 1.1), max(2, length - i - rnd.randint(1, 3)), rnd, tones, depth + 1, tips)
        angle += rnd.uniform(-0.25, 0.25)
        x += math.cos(angle)
        y += math.sin(angle)
    tips.append((int(x - math.cos(angle)), int(y - math.sin(angle))))


def bare(name, tones):
    rnd = random.Random(f"bare_{name}")
    img = Image.new("RGBA", (16, 16), tones[0][:3] + (0,))  # transparent, but dark if drawn opaque (Cutout Leaves off)
    tips = []
    for edge in range(4):  # twigs come in from every side, so neighbouring blocks read as one crown
        t = rnd.uniform(2, 13)
        x, y, a = [(t, 15.5, -math.pi / 2), (t, 0.0, math.pi / 2), (0.0, t, 0.0), (15.5, t, math.pi)][edge]
        grow(img, x, y, a + rnd.uniform(-0.5, 0.5), rnd.randint(10, 15), rnd, tones, 0, tips)
    return img, tips


def budding(name, img, tips):
    rnd = random.Random(f"buds_{name}")
    colours = BUDS.get(name, GREEN_BUDS)
    out = img.copy()
    twig = [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3] > 0]
    spots = tips + rnd.sample(twig, len(twig) // 5)
    for x, y in spots:
        for dx, dy in ((0, 0), rnd.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16:
                out.putpixel((x + dx, y + dy), rnd.choice(colours))
    return out


def grey(img):
    """The leaves in grey, bright enough that a tint shows its colour (like vanilla's tinted leaf textures)."""
    lum = [0.299 * r + 0.587 * g + 0.114 * b for r, g, b, a in img.getdata() if a > 0]
    lo, hi = min(lum), max(lum)
    out = Image.new("RGBA", img.size)
    for i, (r, g, b, a) in enumerate(img.getdata()):
        v = int(80 + (0.299 * r + 0.587 * g + 0.114 * b - lo) / max(1, hi - lo) * 160)
        out.putpixel((i % img.width, i // img.width), (v, v, v, a))
    return out


def save(img, name, mipmap, parent):
    img.save(OUT_TEX / f"{name}.png")
    (OUT_TEX / f"{name}.png.mcmeta").write_text(json.dumps({"texture": {"mipmap_strategy": mipmap}}, indent=2) + "\n")
    model = {"parent": parent, "textures": {"all": f"mechanical:block/seasonal/{name}"}}
    (OUT_MODEL / f"{name}.json").write_text(json.dumps(model, indent=2) + "\n")


def main():
    OUT_TEX.mkdir(parents=True, exist_ok=True)
    OUT_MODEL.mkdir(parents=True, exist_ok=True)
    jar = zipfile.ZipFile(JAR)
    for leaves in VANILLA + MOD:
        tones = bark(jar, leaves)
        img, tips = bare(leaves, tones)
        assert sum(1 for p in img.getdata() if p[3]) > 20, f"{leaves}: too few twigs"
        save(img, f"{leaves}_bare", "cutout", "minecraft:block/cube_all")
        save(budding(leaves, img, tips), f"{leaves}_buds", "cutout", "minecraft:block/cube_all")
        if leaves in AUTUMN_MODEL:  # minecraft:block/leaves has tintindex 0: the autumn colour comes from the tint
            save(grey(texture(jar, leaves, f"{leaves}_leaves")), f"{leaves}_autumn", "dark_cutout", "minecraft:block/leaves")
    print("seasonal leaves ok")


if __name__ == "__main__":
    main()
