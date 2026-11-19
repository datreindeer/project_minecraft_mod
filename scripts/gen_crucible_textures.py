"""Crucible item textures, drawn as a round, thick-walled clay pot seen from a little above (like the buckets): an oval
rim with the hollow inside, a bulging body lit from the upper left, grog specks and a dark outline.
Variants: unfired (raw grey clay), crucible (fired, empty), molten_crucible (glowing copper, 6-frame animation) and
solid_crucible (set copper). Run: python scripts/gen_crucible_textures.py
"""
import json
import random
from pathlib import Path

from PIL import Image

ITEM = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"
CX = 7.5
# half width of the body per row (rows 4..13): bulges a little in the middle, narrows to the foot
HALF = {5: 6.0, 6: 6.3, 7: 6.5, 8: 6.5, 9: 6.3, 10: 5.9, 11: 5.3, 12: 4.6, 13: 3.7}
RIM_A, RIM_B, RIM_Y = 6.3, 2.5, 4.4     # outer rim oval
IN_A, IN_B, IN_Y = 4.6, 1.7, 4.3       # the hollow
FIRED = [(74, 34, 20), (112, 52, 30), (146, 72, 40), (176, 94, 54), (204, 124, 76), (226, 156, 104)]
RAW = [(70, 76, 90), (104, 112, 128), (134, 142, 158), (160, 167, 182), (184, 190, 203), (206, 211, 222)]


def ramp(pal, t):
    t = min(max(t, 0.0), 1.0) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(round(pal[i][k] * (1 - f) + pal[i + 1][k] * f) for k in range(3))


def oval(x, y, a, b, cy):
    return ((x - CX) / a) ** 2 + ((y - cy) / b) ** 2


def pot(pal, seed):
    """The empty pot; returns the image and the set of hollow pixels."""
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    hollow = set()
    for y in range(16):
        for x in range(16):
            px, inside_body = x + 0.5, False
            if y in HALF and abs(px - CX) <= HALF[y]:
                inside_body = True
            on_rim = oval(px, y + 0.5, RIM_A, RIM_B, RIM_Y) <= 1
            if not (inside_body or on_rim):
                continue
            if on_rim and oval(px, y + 0.5, IN_A, IN_B, IN_Y) <= 1:
                hollow.add((x, y))
                continue
            if on_rim:
                # top of the rim, facing up: the brightest part, lit from the upper left; the front lip a touch darker
                t = 0.86 - (px - CX) / RIM_A * 0.16 + (0.04 if y + 0.5 < IN_Y else -0.06)
            else:
                n = (px - CX) / HALF.get(y, 5.6)          # -1 left .. 1 right
                t = 0.55 - n * 0.32 - (y - 4) * 0.025     # lit from the left, darker towards the foot
                if -0.75 < n < -0.4 and 5 <= y <= 11:
                    t += 0.16                             # glaze-like highlight down the left side
                if n > 0.7:
                    t -= 0.12                             # turning away on the right
            t += rnd.uniform(-0.05, 0.05)
            if rnd.random() < 0.06:
                t -= 0.18                                 # grog specks
            img.putpixel((x, y), ramp(pal, t) + (255,))
    # rim's lower lip casts a line where it meets the body
    for x in range(16):
        y = round(RIM_Y + RIM_B * (1 - ((x + 0.5 - CX) / RIM_A) ** 2) ** 0.5 if abs(x + 0.5 - CX) < RIM_A else -1)
        if 4 <= y < 16 and img.getpixel((x, y))[3]:
            r, g, b, a = img.getpixel((x, y))
            img.putpixel((x, y), (round(r * 0.78), round(g * 0.78), round(b * 0.78), a))
    # dark outline around everything
    alpha = {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3] or (x, y) in hollow}
    edge = pal[0]
    for (x, y) in list(alpha):
        if any((x + dx, y + dy) not in alpha for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
            if (x, y) not in hollow:
                img.putpixel((x, y), edge + (255,))
    return img, hollow


def fill_hollow(img, hollow, colour_at):
    for (x, y) in hollow:
        img.putpixel((x, y), colour_at(x, y) + (255,))


def save(img, path, frames=None):
    img.save(path)
    meta = path.with_suffix(".png.mcmeta")
    if frames:
        meta.write_text(json.dumps({"animation": {"frametime": 6}}) + "\n")
    elif meta.exists():
        meta.unlink()


def empty_inside(x, y):
    # looking in from above the front: the far inner wall catches light, the near side is in the lip's shadow
    d = (y + 0.5 - (IN_Y - IN_B)) / (2 * IN_B)
    return ramp([(28, 12, 8), (46, 22, 13), (78, 38, 22), (112, 56, 32)], 0.95 - d - (x + 0.5 - CX) / 14)


raw, raw_hollow = pot(RAW, 3)
fill_hollow(raw, raw_hollow, lambda x, y: ramp([(40, 44, 54), (58, 63, 76), (84, 90, 104)], (y + 0.5 - 2.4) / 2.4))
save(raw, ITEM / "unfired_crucible.png")

fired, hollow = pot(FIRED, 7)
empty = fired.copy()
fill_hollow(empty, hollow, empty_inside)
save(empty, ITEM / "crucible.png")

# molten copper: bright, a hot white-yellow core drifting across, bubbles popping; a warm glow on the rim's inner edge
MOLTEN = [(150, 40, 8), (214, 82, 14), (246, 140, 30), (255, 196, 70), (255, 238, 160)]
anim = Image.new("RGBA", (16, 16 * 6), (0, 0, 0, 0))
for f in range(6):
    frame = fired.copy()
    rnd = random.Random(100 + f)
    hot_x = CX - 1.8 + f * 0.7
    bubbles = {(rnd.choice(sorted(hollow))) for _ in range(2)}

    def molten(x, y, hot_x=hot_x, bubbles=bubbles):
        t = 0.95 - abs(x + 0.5 - hot_x) / 6.5 - abs(y + 0.5 - IN_Y) * 0.12
        if (x, y) in bubbles:
            t += 0.25
        return ramp(MOLTEN, t)
    fill_hollow(frame, hollow, molten)
    for (x, y) in hollow:  # glow on the rim just around the metal
        for dx, dy in ((0, -1), (-1, 0), (1, 0)):
            p = (x + dx, y + dy)
            if p not in hollow and 0 <= p[0] < 16 and 0 <= p[1] < 16 and frame.getpixel(p)[3]:
                r, g, b, a = frame.getpixel(p)
                frame.putpixel(p, (min(255, r + 40), min(255, g + 18), b, a))
    anim.paste(frame, (0, f * 16))
save(anim, ITEM / "molten_crucible.png", frames=6)

# set copper: dull metal with a sheen across it
solid = fired.copy()
fill_hollow(solid, hollow, lambda x, y: ramp([(92, 46, 30), (150, 80, 52), (196, 116, 76), (224, 160, 112)],
                                             0.45 + (0.35 if (x + y) in (9, 10) else 0) - (x + 0.5 - CX) / 12))
save(solid, ITEM / "solid_crucible.png")
print("crucible textures ok")
