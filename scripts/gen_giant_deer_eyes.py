"""Glowing red eyes overlay for the angry Giant Deer: the eye pixels of giant_deer.png, everything else transparent.
Run after gen_giant_deer_texture.py: python scripts/gen_giant_deer_eyes.py"""
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/wildlife"
EYE, EYE_GOLD = (14, 10, 8), (120, 86, 40)  # GD["eye"], GD["eye_gold"] in gen_giant_deer_texture.py

src = Image.open(TEX / "giant_deer.png").convert("RGBA")
out = Image.new("RGBA", src.size, (0, 0, 0, 0))
n = 0
for y in range(src.height):
    for x in range(src.width):
        rgb = src.getpixel((x, y))[:3]
        if rgb == EYE:
            out.putpixel((x, y), (255, 36, 24, 255)); n += 1  # glowing pupil
        elif rgb == EYE_GOLD:
            out.putpixel((x, y), (255, 140, 110, 255)); n += 1  # hot rim
assert n > 0, "no eye pixels found"
out.save(TEX / "giant_deer_eyes.png")
print(f"giant deer eyes ok ({n} px)")
