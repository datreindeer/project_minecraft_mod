# Coin item textures: Copper, Silver and Gold Coin (Coins.java). Each is a small stack of coins with one lying beside it,
# seen from slightly above: dark outline, rim, face with a highlight and a stamped mark (a dot, a cross, a ring).
# usage: python scripts/gen_coin_textures.py
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"

# outline, rim shadow, rim, face, face highlight, stamp
METALS = {
    "copper_coin": ("3b1d10", "6e3a1f", "9a5530", "c87a48", "eaa676", "8a4726"),
    "silver_coin": ("2c3034", "5d6469", "8d959b", "c3cacf", "eef2f4", "7c858b"),
    "gold_coin":   ("3f2a05", "8a6212", "b88a1c", "e6bb3a", "fbe58a", "a67812"),
}
STAMPS = {  # offsets from the face centre
    "copper_coin": [(0, 0)],
    "silver_coin": [(0, 0), (-1, 0), (1, 0), (0, -1), (0, 1)],
    "gold_coin": [(-1, 0), (1, 0), (0, -1), (0, 1)],
}
RX, RY, THICK = 4.0, 2.3, 1  # face half-width / half-height, rim thickness in pixels


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def inside(x, y, cx, cy):
    return ((x - cx) / RX) ** 2 + ((y - cy) / RY) ** 2 <= 1.0


def coin(px, cx, cy, pal, stamp):
    outline, shadow, rim, face, hi, mark = (hexc(c) for c in pal)
    body = {(x, y) for x in range(16) for y in range(16) if any(inside(x, y - d, cx, cy) for d in range(THICK + 1))}
    top = {(x, y) for x in range(16) for y in range(16) if inside(x, y, cx, cy)}
    for x, y in body - top:
        px[(x, y)] = shadow if x > cx + 1 else rim
    for x, y in top:
        light = (x - cx) / RX + (y - cy) / RY < -0.75
        px[(x, y)] = hi if light else face
    for dx, dy in stamp:
        px[(round(cx) + dx, round(cy) + dy)] = mark
    for x, y in body:  # outline: every empty neighbour of the coin, drawn over what lies behind it
        for nx, ny in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if (nx, ny) not in body and 0 <= nx < 16 and 0 <= ny < 16:
                px[(nx, ny)] = outline


def build(name):
    pal, stamp = METALS[name], STAMPS[name]
    px = {}
    coin(px, 10.5, 11.5, pal, stamp)         # the loose one, behind and to the right
    for i, cy in enumerate((11.0, 9.0, 7.0)):   # the stack, bottom up: only the top coin shows its stamp
        coin(px, 5.0, cy, pal, stamp if i == 2 else [])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        img.putpixel((x, y), c)
    return img


if __name__ == "__main__":
    for name in METALS:
        img = build(name)
        assert img.getbbox() is not None and img.size == (16, 16), name
        img.save(OUT / f"{name}.png")
        print(OUT / f"{name}.png")
