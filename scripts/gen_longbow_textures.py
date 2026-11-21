"""Yew Longbow and Yew Stave textures (BronzeTools.YEW_LONGBOW / YEW_STAVE).

The longbow is the Sinew Bow sprite (each pulling stage) recoloured into yew: the limbs in reddish-orange heartwood with
the pale sapwood showing on the belly side, a dark leather grip in the middle, the string kept. The stave is a straight
yew blank, the same two-tone wood, tapered to the tips.
Standalone: python scripts/gen_longbow_textures.py
"""
from pathlib import Path

from PIL import Image

ITEM = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"
HEART = [(92, 42, 24), (132, 62, 34), (172, 86, 48), (206, 108, 62)]  # from yew_rough_log.png
SAP = (226, 196, 150)
GRIP = [(150, 112, 70), (96, 66, 40)]  # a tan leather wrap, banded


def ramp(t):
    t = max(0.0, min(0.999, t)) * (len(HEART) - 1)
    i, f = int(t), t - int(t)
    return tuple(round(a + (b - a) * f) for a, b in zip(HEART[i], HEART[i + 1]))


def lum(c):
    return (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255


def is_string(c):
    return c[0] > 170 and c[1] > 160 and abs(c[0] - c[2]) < 40


def longbow(base):
    im = Image.open(ITEM / f"{base}.png").convert("RGBA")
    px = im.load()
    wood = {(x, y) for x in range(16) for y in range(16) if px[x, y][3] and not is_string(px[x, y])}
    for x, y in wood:
        c = px[x, y]
        n = ramp((lum(c) - 0.1) / 0.6)
        # the belly (the side facing the string, down-right) shows the pale sapwood
        if (x + 1, y + 1) not in wood and (x + 1, y) not in wood and lum(c) > 0.25:
            n = SAP
        if 8 <= x + y <= 10 and x < 8 and y < 8:  # the grip in the middle of the stave
            n = GRIP[(x + y) % 2]
        px[x, y] = (*n, c[3])
    return im


def stave():
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for i in range(1, 15):  # diagonal from the bottom-left to the top-right
        x, y = i, 15 - i
        px[x, y] = (*ramp(0.35 + 0.25 * ((i % 3) == 0)), 255)
        if 2 < i < 13:  # thicker in the middle: the sapwood on one side, darker heartwood on the other
            px[x, y - 1] = (*SAP, 255)
            if 4 < i < 11:
                px[x + 1, y] = (*HEART[1], 255)
    px[1, 14] = px[14, 1] = (*HEART[0], 255)  # tips
    return im


if __name__ == "__main__":
    longbow("sinew_bow").save(ITEM / "yew_longbow.png")
    for n in range(3):
        longbow(f"sinew_bow_pulling_{n}").save(ITEM / f"yew_longbow_pulling_{n}.png")
    stave().save(ITEM / "yew_stave.png")
    print("longbow textures ok")
