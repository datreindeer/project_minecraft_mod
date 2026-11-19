"""Generate "aged" (hung and dried) versions of the skinned carcass textures.

A carcass that has hung 3 minutes on a Hanging Rack has aged (CarcassEntity.isAged). Skinned, its bare meat then renders
with these: dark, dried maroon flesh with a crust along the edges and specks of dried blood, a few glossy streaks of glaze,
yellowed fat and bone. Deterministic: reads assets/mechanical/textures/entity/skinned/ (gen_skinned_textures.py) and
writes the same paths under assets/mechanical/textures/entity/skinned_aged/.

Usage: python gen_aged_textures.py
"""
import colorsys
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_skinned_textures import h, lerp, lum_of  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity"
SRC, OUT = TEX / "skinned", TEX / "skinned_aged"

DRY_DARK, DRY_MID, DRY_LIGHT = (62, 19, 16), (112, 38, 30), (160, 80, 62)
CRUST, GLAZE = (40, 13, 11), (196, 128, 106)
OLD_FAT, OLD_BONE = (196, 164, 104), (204, 188, 148)


def dry(img):
    img = img.convert("RGBA")
    px = img.load()
    W, H = img.size
    src = img.copy().load()

    def edge(x, y):
        # next to a transparent pixel or the texture edge: the outside of a part, dried hardest
        return any(not (0 <= x + dx < W and 0 <= y + dy < H) or src[x + dx, y + dy][3] == 0
                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))

    for y in range(H):
        for x in range(W):
            r, g, b, a = src[x, y]
            if a == 0:
                continue
            _, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.42 and v > 0.75:  # fat, sinew, bone: yellowed and a little darker
                c = lerp((r, g, b), OLD_FAT if s > 0.15 else OLD_BONE, 0.45)
                c = tuple(round(ch * 0.88) for ch in c)
            else:
                t = max(0.0, min(1.0, (lum_of(r, g, b) - 0.12) / 0.45))
                c = lerp(DRY_DARK, DRY_MID, t / 0.5) if t < 0.5 else lerp(DRY_MID, DRY_LIGHT, (t - 0.5) / 0.5)
                if h(x, y, 11) < 90:  # specks of dried blood
                    c = lerp(c, CRUST, 0.6)
                elif h(x // 2, y, 13) < 45:  # short glossy streaks of the glaze
                    c = lerp(c, GLAZE, 0.35)
            if edge(x, y):
                c = lerp(c, CRUST, 0.35)
            px[x, y] = (*c, a)
    return img


def main():
    written = 0
    for src in sorted(SRC.rglob("*.png")):
        dst = OUT / src.relative_to(SRC)
        dst.parent.mkdir(parents=True, exist_ok=True)
        orig = Image.open(src)
        out = dry(orig)
        assert out.size == orig.size
        assert out.getchannel("A").tobytes() == orig.convert("RGBA").getchannel("A").tobytes()
        out.save(dst)
        written += 1
    print(f"wrote {written} files to {OUT}")


if __name__ == "__main__":
    main()
