"""Fresh leaf litter, one per leaf type: vanilla's leaf litter shape and shading, coloured like that tree's leaves.
Grey (biome-tinted) vanilla leaves get their usual default foliage colour first.
Run after the leaf textures exist: python scripts/gen_leaf_litter_textures.py"""
import colorsys
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
OUT = ROOT / "src/main/resources/assets/mechanical/textures/block"
VANILLA = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "yellow_poplar",
           "orange_poplar", "red_poplar", "azalea", "flowering_azalea"]
MOD = ["aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew", "rowan", "bird_cherry", "wild_apple", "plum", "olive", "date_palm", "bitter_orange"]
# vanilla's fixed/default foliage tints for the grey leaf textures
TINT = {"oak": (0x48, 0xB5, 0x18), "jungle": (0x30, 0xBB, 0x0B), "acacia": (0xAE, 0xA4, 0x2A), "dark_oak": (0x59, 0xAE, 0x30),
        "mangrove": (0x8D, 0xB1, 0x27), "spruce": (0x61, 0x99, 0x61), "birch": (0x80, 0xA7, 0x55)}


def leaf_colour(img):
    """Average colour of the opaque pixels, after the biome tint for grey textures."""
    px = [p for p in img.convert("RGBA").getdata() if p[3] > 128]
    r, g, b = (sum(p[i] for p in px) / len(px) for i in range(3))
    return r, g, b


def main():
    jar = zipfile.ZipFile(JAR)
    base = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/leaf_litter.png"))).convert("RGBA")
    base_lum = [0.299 * r + 0.587 * g + 0.114 * b for r, g, b, a in base.getdata() if a > 0]
    mean_lum = sum(base_lum) / len(base_lum)
    for leaf in VANILLA + MOD:
        if leaf in MOD:
            img = Image.open(OUT / f"{leaf}_leaves.png")
        else:
            img = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{leaf}_leaves.png")))
        r, g, b = leaf_colour(img)
        if leaf in TINT:  # grey texture: multiply by the tint, like the game does
            t = TINT[leaf]
            r, g, b = r * t[0] / 255, g * t[1] / 255, b * t[2] / 255
        # fallen leaves are a touch duller and darker than living ones
        h, l, s = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
        r, g, b = (c * 255 for c in colorsys.hls_to_rgb(h, l * 0.92, s * 0.85))
        out = Image.new("RGBA", base.size, (0, 0, 0, 0))
        for i, (pr, pg, pb, pa) in enumerate(base.getdata()):
            if pa == 0:
                continue
            k = (0.299 * pr + 0.587 * pg + 0.114 * pb) / mean_lum  # keep vanilla's shading
            out.putpixel((i % base.width, i // base.width), tuple(min(255, int(c * k)) for c in (r, g, b)) + (pa,))
        assert out.getbbox() is not None, leaf
        out.save(OUT / f"{leaf}_leaf_litter.png")
    print("leaf litter textures ok")


if __name__ == "__main__":
    main()
