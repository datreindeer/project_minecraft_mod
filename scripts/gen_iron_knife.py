"""Iron Knife texture: the flint knife's layout with the blade recoloured to iron (handle and outline kept)."""
from pathlib import Path
from PIL import Image

T = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"
IRON = {(34, 32, 32): (110, 110, 112), (46, 45, 45): (140, 140, 142), (61, 60, 60): (172, 172, 175),
        (168, 168, 168): (226, 226, 228), (179, 177, 175): (255, 255, 255), (14, 14, 14): (40, 40, 42)}
im = Image.open(T / "flint_knife.png").convert("RGBA")
for y in range(im.height):
    for x in range(im.width):
        r, g, b, a = im.getpixel((x, y))
        if a and (r, g, b) in IRON:
            im.putpixel((x, y), IRON[(r, g, b)] + (a,))
im.save(T / "iron_knife.png")
print("iron knife ok")
