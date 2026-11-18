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

# Pools (amount 2-4): one wet puddle that grows with the amount, ragged edge, darker where it is deepest.
import math
# deepest (centre) to thinnest (rim): the rim is lighter and lets the ground show through a little
RAMP = [(52, 3, 5, 255), (68, 5, 6, 255), (86, 8, 8, 255), (104, 11, 10, 255), (122, 15, 13, 255),
        (140, 20, 17, 255), (156, 27, 22, 235), (168, 36, 30, 200)]


def pool_colour(t, dx, dy, rnd):
    """t = 0 at the centre, 1 at the edge. Dithered so the shades blend; the side facing the light (top left) is a bit lighter."""
    t = t * 0.85 + 0.15 * max(0.0, min(1.0, t + (-dx - dy) * 0.04)) + rnd.uniform(-0.09, 0.09)
    return RAMP[max(0, min(len(RAMP) - 1, int(max(0.0, t) ** 1.3 * len(RAMP))))]


for level, radius in ((2, 3.5), (3, 5.2), (4, 7.4)):
    rnd = random.Random(2000 + level)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5 + rnd.uniform(-0.6, 0.6), 7.5 + rnd.uniform(-0.6, 0.6)
    lobes = [(rnd.uniform(0, math.tau), rnd.uniform(0.6, 1.4)) for _ in range(5)]  # lumpy outline
    for x in range(16):
        for y in range(16):
            dx, dy = x - cx, y - cy
            ang = math.atan2(dy, dx)
            edge = radius * (1 + 0.18 * sum(a * math.cos(ang * (i + 2) + ph) for i, (ph, a) in enumerate(lobes)) / len(lobes))
            d = math.hypot(dx, dy) + rnd.uniform(-0.35, 0.35)
            if d <= edge:
                img.putpixel((x, y), pool_colour(d / edge, dx, dy, rnd))
    for _ in range(int(radius * 1.5)):  # splashes and drops around the rim
        ang, d = rnd.uniform(0, math.tau), radius + rnd.uniform(0.5, 2.5)
        x, y = int(cx + math.cos(ang) * d), int(cy + math.sin(ang) * d)
        if 0 <= x < 16 and 0 <= y < 16 and img.getpixel((x, y))[3] == 0: img.putpixel((x, y), RAMP[-2][:3] + (190,))
    for _ in range(level):  # glossy highlights on the surface
        x, y = int(cx + rnd.uniform(-radius, radius) * 0.4), int(cy + rnd.uniform(-radius, radius) * 0.4)
        img.putpixel((x, y), WET)
        if x + 1 < 16: img.putpixel((x + 1, y), WET)
    assert img.getbbox() is not None
    img.save(f"{OUT}/blood_pool_{level}.png")
print("ok")
