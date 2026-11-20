"""Small-batch textures (scripts/gen_smallbatch.py): the Broom item.

Broom: the Tool Rod's own pixels for the handle (lower left), a band of Twine-coloured lashing, and a fan of straw
(the Straw item's palette) spreading toward the upper right, each strand its own shade with ragged, uneven tips.
Run: python scripts/gen_smallbatch_textures.py (not part of the data chain; the PNGs are committed)."""
import random
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
ITEM = ROOT / "src/main/resources/assets/mechanical/textures/item"
STRAW = [(106, 80, 56), (156, 130, 58), (208, 180, 94), (240, 220, 138)]  # Straw item: shadow .. sunlit
TWINE = [(83, 66, 55), (99, 78, 66), (130, 102, 85)]


def broom():
    rng = random.Random(1812)
    rod = Image.open(ITEM / "tool_rod.png").convert("RGBA")
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            a, c = x - y, x + y - 15  # a: along the handle (lower left -> upper right), c: across (- = upper left)
            if a <= -5 and rod.getpixel((x, y))[3]:
                img.putpixel((x, y), rod.getpixel((x, y)))  # the handle
    # strands lie along the handle's line (constant c); three shades repeating across them read as straw
    tips = {c: 12 - rng.choice((0, 0, 1)) - abs(c) // 2 for c in range(-5, 6)}  # ragged ends
    for x in range(16):
        for y in range(16):
            a, c = x - y, x + y - 15
            if -4 <= a <= -2 and abs(c) <= 2:  # twine lashing, two turns
                img.putpixel((x, y), TWINE[2 if c < 0 else 0 if c > 0 else 1] + (255,))
            elif abs(c) <= 5 and -1 <= a <= tips[c] and abs(c) <= min(5.6, 2.2 + (a + 1) * 0.6):  # the straw flares out
                t = (3, 2, 3, 1)[(c + 20) // 2 % 4] - (c >= 3) + (c <= -3)
                if a >= tips[c] - 1 and rng.random() < 0.4:
                    t += 1  # sun-bleached tips
                img.putpixel((x, y), STRAW[max(0, min(3, t))] + (255,))
    # a dark edge on the shaded (lower right) side of the straw, as the item's other outlines
    out = img.copy()
    for x in range(16):
        for y in range(16):
            if not img.getpixel((x, y))[3] and x + y - 15 > 0 and x - y >= -1 and any(
                    0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3] for dx, dy in ((-1, 0), (0, 1))):
                out.putpixel((x, y), STRAW[0][:3] + (255,))
    return out


broom().save(ITEM / "broom.png")
print("smallbatch textures ok")
