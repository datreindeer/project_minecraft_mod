"""Twigs ground cover: 3 variants of a few small forked sticks on a transparent 16x16 tile.
Run: python scripts/gen_twig_textures.py"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/block"
DARK, MID, LIGHT, TIP = (40, 28, 16, 120), (96, 68, 41, 255), (128, 94, 58, 255), (150, 118, 76, 255)


def line(img, x0, y0, angle, length, rnd):
    """A crooked stick: light top pixel, darker shadow pixel beside it, paler broken tip."""
    dx, dy = math.cos(angle), math.sin(angle)
    # unit perpendicular pointing down-right on screen = the shadow side
    sx, sy = (1 if -dy >= 0 else -1), (1 if dx >= 0 else -1)
    x, y = x0, y0
    pts = []
    for i in range(length):
        px, py = int(round(x)), int(round(y))
        if 0 <= px < 16 and 0 <= py < 16:
            pts.append((px, py))
        angle += rnd.uniform(-0.12, 0.12)
        x += math.cos(angle)
        y += math.sin(angle)
    for i, (px, py) in enumerate(pts):
        shadow = (px + (sx if abs(dy) > abs(dx) else 0), py + (sy if abs(dx) >= abs(dy) else 0))
        if 0 <= shadow[0] < 16 and 0 <= shadow[1] < 16 and img.getpixel(shadow)[3] == 0 and i % 2:
            img.putpixel(shadow, DARK)
        img.putpixel((px, py), TIP if i in (0, len(pts) - 1) else (LIGHT if i % 3 else MID))
    return pts


def variant(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for _ in range(rnd.randint(3, 4)):
        x0, y0 = rnd.uniform(2, 13), rnd.uniform(2, 13)
        pts = line(img, x0, y0, rnd.uniform(0, math.tau), rnd.randint(6, 10), rnd)
        if len(pts) > 4 and rnd.random() < 0.8:  # a little side fork
            fx, fy = pts[len(pts) // 2]
            line(img, fx, fy, rnd.uniform(0, math.tau), rnd.randint(2, 3), rnd)
    return img


if __name__ == "__main__":
    for v in range(3):
        img = variant(1000 + v * 17)
        assert img.getbbox() is not None, "empty twig texture"
        img.save(OUT / f"twigs_{v}.png")
    print("twigs ok")
