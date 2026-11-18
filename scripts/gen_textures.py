# WARNING: one-off script from the first stone age pass. Re-running it OVERWRITES the hand-made flint tool,
# plant_fibre and twine textures. Only use it for the stone head placeholders.
# Generates stone-age item textures by recoloring vanilla textures.
# usage: python -I gen_textures.py <vanilla item texture dir> <output dir>
import colorsys, sys
from pathlib import Path
from PIL import Image

src, out = Path(sys.argv[1]), Path(sys.argv[2])
out.mkdir(parents=True, exist_ok=True)

def is_handle(r, g, b):
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    return s > 0.25 and 0.02 < h < 0.15  # brown/orange wood

def recolor(name, dst, head=None, handle=None, drop_handle=False):
    img = Image.open(src / f"{name}.png").convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            if is_handle(r, g, b):
                if drop_handle:
                    px[x, y] = (0, 0, 0, 0)
                elif handle:
                    px[x, y] = tint((r, g, b), handle) + (a,)
            elif head:
                px[x, y] = tint((r, g, b), head) + (a,)
    img.save(out / f"{dst}.png")

def tint(rgb, color):
    lum = (0.3 * rgb[0] + 0.59 * rgb[1] + 0.11 * rgb[2]) / 255
    k = 0.35 + lum * 1.1
    return tuple(max(0, min(255, int(c * k))) for c in color)

FLINT = (72, 74, 86)
for tool in ["pickaxe", "axe", "shovel", "hoe"]:
    recolor(f"stone_{tool}", f"flint_{tool}", head=FLINT)
    recolor(f"stone_{tool}", f"stone_{tool}_head", drop_handle=True)
recolor("stone_sword", "flint_knife", head=FLINT)
recolor("stone_sword", "stone_sword_blade", drop_handle=True)
recolor("wheat", "plant_fiber", head=(70, 140, 50), handle=(70, 140, 50))
recolor("string", "twine", head=(170, 130, 80))
print("ok")

# Diagonal crop along the sword: d = distance from the pommel (bottom-left corner).
def crop(dst, keep):
    img = Image.open(out / f"{dst}.png")
    px = img.load()
    for y in range(16):
        for x in range(16):
            if not keep(x + (15 - y)):
                px[x, y] = (0, 0, 0, 0)
    img.save(out / f"{dst}.png")

crop("stone_sword_blade", lambda d: d >= 14)
crop("flint_knife", lambda d: d <= 18)
