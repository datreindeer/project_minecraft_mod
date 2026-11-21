"""Textures for the Mouldboard Plough (Iron Age, PloughEntity):
- entity/mouldboard_plough.png (128x64) for client/PloughModel.java: BOXES mirrors its cubes (texOffs, origin, size)
- item: mouldboard_plough (16x16)
Same art style as scripts/gen_wagon_textures.py: hewn oak and dark forged iron, the share, coulter edge and mouldboard
face polished bright by the soil. Deterministic, overwrites its outputs.
Run: python scripts/gen_plough_textures.py"""
from gen_cart_textures import PLANK, T, grain, item
from gen_wagon_textures import FORGED, IRON, iron, paint_f
from wildlife_paint import h, shade

SOIL = [(72, 52, 34), (94, 68, 44)]


def beam(v, face, x, y, z, i, j):
    """Hewn oak: adze marks across the grain, iron bands where the standard and the fork are bolted through."""
    if face in ("front", "back"):
        return shade(PLANK[1], 0.85 + (h(i, j, 61) - 0.5) * 0.1)
    if abs(z + 22) < 1.2 or abs(z) < 1.2:
        return IRON[2] if abs(x) < 0.6 and face == "top" else iron(i, j, 63)
    c = grain(shade(PLANK[2], 0.88), z, x + y * 3, 65, 0.08)
    return shade(c, 0.92) if int(z) % 5 == 0 else c


def handle(v, face, x, y, z, i, j):
    """Smooth oak, worn darker where the hands hold it at the top end."""
    if face in ("front", "back"):
        return shade(PLANK[1], 0.9)
    c = grain(PLANK[3], z, x + y * 3, 67, 0.05)
    return shade(c, 0.72) if z > 20 else c


def wood_bar(v, face, x, y, z, i, j):
    """Swingletree and rung: oak, iron hooks at the swingletree's ends."""
    if abs(x) > 5.5:
        return iron(i, j, 69, 2)
    if face in ("left", "right"):
        return shade(PLANK[1], 0.9)
    return grain(PLANK[2], x, y * 3 + z, 71, 0.06)


def forged(v, face, x, y, z, i, j):
    return iron(i, j, 73)


def coulter(v, face, x, y, z, i, j):
    """Knife: dark iron, the front edge and lower blade scoured bright."""
    if face == "front" or y > 8 and face in ("left", "right"):
        return IRON[3] if h(i, j, 75) > 0.3 else IRON[2]
    return iron(i, j, 77)


def standard(v, face, x, y, z, i, j):
    return grain(shade(PLANK[2], 0.82), y, x + z, 79, 0.06)


def share(v, face, x, y, z, i, j):
    """The share: polished bright underneath and at the point, forged on top, a little soil clinging at the back."""
    if face == "bottom" or z < -4.5:
        return IRON[3] if h(i, j, 81) > 0.25 else IRON[2]
    if face == "top" and z > 0.5 and h(i, j, 83) > 0.6:
        return SOIL[h(i, j, 85) > 0.5]
    return iron(i, j, 87, 2)


def landside(v, face, x, y, z, i, j):
    return IRON[2 + (face == "right" and h(i, j, 89) > 0.5)] if face in ("left", "right") else iron(i, j, 91)


def mouldboard(v, face, x, y, z, i, j):
    """The turning face (towards the share, local -x) is polished by the furrow; the back is forged and rusty-dark."""
    if face == "right":
        return IRON[3] if h(i, j, 93) > 0.35 else IRON[2]
    if face == "left" and h(i, j, 95) > 0.85:
        return SOIL[0]
    return iron(i, j, 97, 0 if face == "left" else 1)


def fork(v, face, x, y, z, i, j):
    return iron(i, j, 99, 1)


def hub(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        return FORGED[0] if abs(y) < 0.6 and abs(z) < 0.6 else FORGED[2]
    return FORGED[1]


def spoke(v, face, x, y, z, i, j):
    return grain(PLANK[3], y, x + z, 101, 0.05)


def rim(v, face, x, y, z, i, j):
    """Felloe with an iron tyre on its running face (the box's top, outermost)."""
    if face == "top":
        return iron(i, j, 103, 0)
    if face in ("left", "right") and y < -4.6:
        return FORGED[1]
    return grain(PLANK[2], z, y * 2, 105, 0.05)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors PloughModel.create (local cube coordinates)
BOXES = {
    "beam": (0, 0, -1.5, -1.5, -26, 3, 3, 30, beam),
    "handle": (0, 33, -1, -1, 0, 2, 2, 26, handle),          # left and right
    "share": (66, 0, -2, -2, -5, 4, 1.5, 7, share),
    "share_tip": (88, 0, -1, -1.5, -7, 2, 1, 2, share),
    "landside": (66, 9, -2.5, -3, -4, 1, 3, 9, landside),
    "mould_low": (86, 9, 0, -3, 0, 1, 3, 7, mouldboard),
    "mould_high": (102, 9, 0, -3, 0, 1, 3, 6, mouldboard),
    "standard": (116, 0, -1, -11.5, -1.5, 2, 9.5, 3, standard),
    "coulter": (56, 33, -0.5, 0, -1, 1, 12, 2, coulter),
    "clevis": (62, 33, -1, -1, -2, 2, 2, 2, forged),
    "rung": (62, 43, -4, -0.5, -0.5, 8, 1, 1, wood_bar),
    "swingletree": (62, 47, -7, -1, -1, 14, 2, 2, wood_bar),
    "fork": (70, 33, -0.5, -8, -0.5, 1, 8, 1, fork),
    "axle": (74, 33, -3, -0.5, -0.5, 6, 1, 1, forged),
    "hub": (88, 33, -1, -1.5, -1.5, 2, 3, 3, hub),
    "spoke": (98, 33, -0.5, -4.5, -0.5, 1, 9, 1, spoke),
    "rim": (102, 33, -1, -5, -2.1, 2, 1, 4.2, rim),
}

# 16x16 sprite, side view, horse to the left: beam, handles rising right, share and mouldboard under, little wheel front
SPRITE = [
    "................",
    "..............hh",
    ".............hh.",
    "............hh..",
    "...........hh...",
    "..........hh.r..",
    ".........hh.r...",
    "fbbbbbbbbbbr....",
    "fBBBBBBBBBBs....",
    "f.F...c...ms....",
    "..F...c..mms....",
    ".www..c.mmms....",
    "w.k.w.c.MMMSS...",
    "wkokw..ePPPPP...",
    "w.k.w...eeee....",
    ".www............",
]


def sprite_px(fx, fy, x, y):
    k = SPRITE[y][x]
    return {
        "h": PLANK[3] if x < 13 else shade(PLANK[2], 0.75),   # handles, worn grip
        "r": PLANK[2],                                         # rung / brace
        "b": PLANK[2], "B": PLANK[1],                          # beam, lit top and shaded side
        "s": shade(PLANK[2], 0.85),                            # standard
        "f": FORGED[2],                                        # clevis and hook
        "c": IRON[2],                                          # coulter
        "m": IRON[3], "M": IRON[2],                            # mouldboard, polished
        "S": IRON[1], "P": FORGED[1], "e": IRON[3],            # share, edge
        "w": FORGED[1], "k": PLANK[3], "o": FORGED[2], "F": FORGED[2],   # wheel: iron tyre, spokes, hub; the fork
    }.get(k)


def main():
    paint_f(BOXES, (128, 64)).save(T / "entity/mouldboard_plough.png")
    item(sprite_px).save(T / "item/mouldboard_plough.png")
    print("plough textures ok")


if __name__ == "__main__":
    main()
