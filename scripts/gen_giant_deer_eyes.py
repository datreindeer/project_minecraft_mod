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

# Glowing runes in the antlers (always on, faint): growth grooves on the palms light up green-gold, the tine tips glow.
# Antler boxes of GiantDeerModel: (u, v, w, h, d) -> standard cube UV layout (top face at u+d.., v..v+d).
PALM = (0, 47, 12, 1, 11)
BEAM = (54, 59, 8, 2, 2)
TINES = [(100, 47, 1, 1, 6), (28, 59, 1, 1, 4), (82, 59, 1, 1, 3), (38, 59, 1, 1, 4), (90, 59, 1, 1, 3),
         (108, 59, 3, 1, 1), (98, 59, 4, 1, 1), (116, 59, 3, 1, 1), (74, 59, 1, 1, 3)]
VEIN, TIP, PEARL = (70, 200, 120, 255), (150, 235, 130, 255), (40, 120, 80, 255)
runes = Image.new("RGBA", src.size, (0, 0, 0, 0))
u, v, w, h, d = PALM
for face_u in (u + d, u + d + w):  # top and bottom faces
    for tx in range(w):
        for ty in range(d):
            if (tx + ty // 2) % 4 == 0 and (tx * 7 + ty * 3) % 5 != 0:  # the grooves, broken up a little like runes
                runes.putpixel((face_u + tx, v + ty), VEIN)
u, v, w, h, d = BEAM
for x in range(u, u + 2 * (d + w)):
    for y in range(v, v + d + h):
        if (x + y) % 3 == 0:
            runes.putpixel((x, y), PEARL)
for u, v, w, h, d in TINES:
    for x in range(u, u + 2 * (d + w)):
        for y in range(v, v + d + h):
            runes.putpixel((x, y), TIP if y < v + d else PEARL)
assert runes.getbbox() is not None
runes.save(TEX / "giant_deer_runes.png")
print("giant deer runes ok")
