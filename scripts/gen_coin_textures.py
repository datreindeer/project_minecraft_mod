# Coin item textures: Copper, Silver and Gold Coin (Coins.java), five per metal (<name>_1.._5) for growing stack sizes
# like firewood (gen_bronze.py picks them by count): one coin, up to three stacks. Coins seen from slightly above: dark outline, rim, face with a highlight and a stamped mark (a dot, a cross, a ring).
# Also the Coin Purse a Trader Caravan's leader holds (Coins.COIN_PURSE): a drawstring pouch of dark leather, a gold coin at its mouth.
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


# per stage: stacks drawn back to front as (centre x, bottom coin's centre y, coins)
STAGES = [
    [(8.0, 10.0, 1)],
    [(10.5, 11.5, 1), (5.5, 11.0, 2)],
    [(10.5, 11.5, 1), (5.0, 11.0, 3)],
    [(10.5, 9.5, 3), (5.0, 11.5, 3)],
    [(4.5, 8.0, 3), (11.5, 8.0, 3), (8.0, 11.5, 3)],
]


def build(name, stage):
    pal, stamp = METALS[name], STAMPS[name]
    px = {}
    for cx, cy, n in STAGES[stage]:
        for i in range(n):  # bottom up: only the top coin shows its stamp
            coin(px, cx, cy - 2 * i, pal, stamp if i == n - 1 else [])
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in px.items():
        if 0 <= x < 16 and 0 <= y < 16:
            img.putpixel((x, y), c)
    return img


# the Coin Purse, pixel by pixel: o outline, D/M/H/h leather dark to light, s seam, c/C cord, g/G/y gold coin, w/W wrinkle
PURSE = [
    "................",
    "......oooo......",
    ".....oGGgyo.....",
    "....ooGggyoo....",
    "...oMhMHMMDo....",
    "...oMMHMMDDo....",
    "....occcCCo.c...",
    "....oDwMMDo.cC..",
    "...oMHHMWMDo.c..",
    "..oMHhHMMMDDoC..",
    ".oMHhhHMMwMDDo..",
    ".oMHhHMMMsMDDo..",
    ".oMHHMMMMsMDDDo.",
    ".oMMHMMMMsMDDDo.",
    "..oMMMMMMMDDDo..",
    "...ooDDDDDDoo...",
]
PURSE_PAL = {"o": "24160c", "D": "4e2c1a", "M": "6e4127", "H": "8c5734", "h": "a86e44", "s": "3a2214", "w": "5a3420", "W": "7a4a2c",
             "c": "cdb37c", "C": "8e7648", "g": "e6bb3a", "G": "fbe58a", "y": "8a6212"}


def purse():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(PURSE):
        assert len(row) == 16, (y, row)
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), hexc(PURSE_PAL[ch]))
    return img


if __name__ == "__main__":
    purse().save(OUT / "coin_purse.png")
    for name in METALS:
        for stage in range(len(STAGES)):
            img = build(name, stage)
            assert img.getbbox() is not None and img.size == (16, 16), name
            img.save(OUT / f"{name}_{stage + 1}.png")
            print(OUT / f"{name}_{stage + 1}.png")
