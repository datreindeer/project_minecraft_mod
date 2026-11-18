# Blood drop decals for the blood trail block (3 variants). usage: python scripts/gen_blood_textures.py
import random
from PIL import Image
OUT = "src/main/resources/assets/mechanical/textures/block"
DARK, MID, WET = (90, 8, 8, 255), (130, 14, 14, 255), (170, 30, 28, 255)
for v in range(3):
    rnd = random.Random(1000 + v)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for _ in range(rnd.randint(3, 5)):  # a few drops of different sizes
        cx, cy, r = rnd.randint(2, 13), rnd.randint(2, 13), rnd.choice([0, 1, 1, 2])
        for x in range(cx - r - 1, cx + r + 2):
            for y in range(cy - r - 1, cy + r + 2):
                if 0 <= x < 16 and 0 <= y < 16 and (x - cx) ** 2 + (y - cy) ** 2 <= r * r + rnd.random():
                    img.putpixel((x, y), DARK if (x - cx) + (y - cy) > 0 else MID)
        if r > 0: img.putpixel((cx - 1 if cx > 0 else cx, cy - 1 if cy > 0 else cy), WET)  # glossy highlight
    for _ in range(rnd.randint(3, 6)):  # tiny splatter
        img.putpixel((rnd.randint(0, 15), rnd.randint(0, 15)), MID)
    img.save(f"{OUT}/blood_{v}.png")
print("ok")
