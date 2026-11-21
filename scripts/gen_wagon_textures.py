"""Textures for the Wagon (Iron Age upgrade of the Wheeled Cart):
- entity/wagon.png (256x128) for client/WagonModel.java: BOXES mirrors its cubes (texOffs, origin, size)
- items: wagon, iron_rimmed_wheel (16x16)
Same art style as scripts/gen_cart_textures.py (oak planks, hewn oak), but ironed: dark forged iron tyres, hub caps,
corner straps, nails and the pole-tip ring, colours sampled from item/iron_plate.png. Deterministic, overwrites its outputs.
Run: python scripts/gen_wagon_textures.py"""
import math

from PIL import Image

from gen_cart_textures import PLANK, T, grain, item, palette
from wildlife_paint import h, paint, shade

IRON = palette("item/iron_plate.png")                     # dark to light
FORGED = [shade(IRON[0], 0.7), IRON[0], IRON[1], IRON[2]]  # blacker forged iron for tyres and caps


def rivet(c, i, j):
    return IRON[3] if (i + j) % 2 == 0 else c


def iron(i, j, salt, base=1):
    """Hammered forged iron: a dark base with a few lighter hammer marks."""
    return FORGED[base + (h(i, j, salt) > 0.8)]


def plank_mat(seam_axis, width, nail_every=0, strap_z=None):
    """Planks running along z (as the cart's), iron nails; strap_z: z bands where an iron strap wraps the board."""
    def mat(v, face, x, y, z, i, j):
        across = x if seam_axis == "x" else y
        if strap_z and face != "bottom" and any(abs(z - s) < 1.0 for s in strap_z):
            return rivet(iron(i, j, 3), i, j) if abs(across % width - width / 2) < 0.6 else iron(i, j, 3)
        if face in ("front", "back"):                     # end grain
            return shade(PLANK[1], 0.92 + (h(int(x), int(y), 31) - 0.5) * 0.1)
        if face in ("left", "right") and seam_axis == "x":
            across = y
        c = grain(PLANK[2], z, across * 2, 11)
        if (across % width) < 1.0 and face not in ("left", "right") or seam_axis == "y" and (across % width) < 1.0:
            c = shade(PLANK[0], 1.05)
        if nail_every and abs(((z + 24) % nail_every) - nail_every / 2) < 0.6 and (across % width) > width - 1.5:
            c = IRON[2]
        return c
    return mat


def end_board(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        return shade(PLANK[1], 0.92 + (h(i, j, 33) - 0.5) * 0.1)
    if abs(x) > 10.0 and face != "bottom":                # iron corner straps
        return rivet(iron(i, j, 5), i, j) if abs(y + 16.5) < 0.6 and face in ("front", "back") else iron(i, j, 5)
    c = grain(PLANK[2], x, y * 2, 13)
    if abs(y + 16.5) < 0.6:
        c = shade(PLANK[0], 1.05)                         # seam between the two boards
    return c


def stake(v, face, x, y, z, i, j):
    if face in ("top", "bottom"):
        return shade(PLANK[1], 0.85)
    if y < -21.0:
        return iron(i, j, 7)                              # iron cap band on the stake head
    return grain(shade(PLANK[2], 0.9), y, x + z, 41, 0.06)


def axle(v, face, x, y, z, i, j):
    if face in ("left", "right") or abs(x) > 15.0:
        return iron(i, j, 9, 2)                           # iron axle ends and linchpins
    return grain(shade(PLANK[1], 0.8), x, y * 3 + z, 17, 0.05)


def bolster(v, face, x, y, z, i, j):
    if face == "top" and abs(x) < 2.0:
        return IRON[3] if abs(x) < 0.6 and abs(z) < 0.6 else iron(i, j, 11)   # kingpin plate
    if face in ("front", "back") and abs(abs(x) - 7.5) < 0.6:
        return IRON[2]                                    # bolts into the axle
    if face in ("left", "right"):
        return shade(PLANK[1], 0.9)
    return grain(shade(PLANK[2], 0.85), x, y * 3, 19, 0.06)


def pole(v, face, x, y, z, i, j):
    if face == "front":
        return iron(i, j, 13, 2)                          # iron cap on the tip
    if face == "back":
        return shade(PLANK[1], 0.9)
    if -39.0 < z < -37.0 or -34.0 < z < -33.0:
        return FORGED[2] if z < -37.0 and (i + j) % 3 else FORGED[1]   # the tip ring and a band behind it
    return grain(shade(PLANK[2], 0.9), z, x + y * 3, 21, 0.06)


def yoke(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        return shade(PLANK[1], 0.9)
    if abs(abs(x) - 8.0) < 1.0 or abs(x) < 1.0:
        return iron(i, j, 15, 2)                          # iron collar rings and the clamp on the pole
    return grain(PLANK[2], x, y * 3 + z, 23, 0.06)


def hub(v, face, x, y, z, i, j):
    """Dark forged iron cap with a lighter boss and the linchpin hole."""
    if face in ("left", "right") and abs(y) < 0.6 and abs(z) < 0.6:
        return FORGED[0]
    edge = max(abs(y), abs(z)) > 1.5 if face in ("left", "right") else max(abs(x), abs(z if face in ("top", "bottom") else y)) > 1.5
    return FORGED[0] if edge else FORGED[1 + (h(i, j, 51) > 0.7)]


def spoke(v, face, x, y, z, i, j):
    return grain(PLANK[3], y, x + z, 25, 0.05)


def felloe(v, face, x, y, z, i, j):
    if face == "top":
        return shade(PLANK[1], 0.75)                      # under the tyre
    if face in ("front", "back"):
        return shade(PLANK[1], 0.9)
    return grain(PLANK[2], z, y * 2, 29, 0.05)


def tyre(v, face, x, y, z, i, j):
    """Forged iron band: hammered running surface, a rivet through the middle of each segment's sides."""
    if face in ("left", "right") and abs(z) < 0.75:
        return IRON[2]
    return iron(i, j, 53, 0 if face in ("front", "back", "bottom") else 1)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors WagonModel.create (local cube coordinates)
BOXES = {
    "floor": (0, 0, -12, -14, -24, 24, 2, 48, plank_mat("x", 4)),
    "board": (0, 50, 12, -20, -24, 2, 8, 48, plank_mat("y", 4, 8, strap_z=(-23, 23))),   # left; right is mirrored
    "end_board": (100, 50, -12, -19, -24, 24, 5, 2, end_board),                          # front and back
    "stake": (228, 0, -1, -23, -1, 2, 10, 2, stake),
    "axle": (100, 60, -17, -1, -1, 34, 2, 2, axle),                                      # rear and front
    "bolster": (100, 66, -10, -4, -1.5, 20, 3, 3, bolster),
    "pole": (144, 0, -1, -1, -40, 2, 2, 40, pole),
    "yoke": (100, 72, -14, -1, -1, 28, 2, 2, yoke),
    "hub": (172, 50, -2, -2, -2, 4, 4, 4, hub),
    "spoke": (188, 50, -0.5, -7, -0.5, 1, 14, 1, spoke),
    "felloe": (192, 50, -1, -7.5, -2, 2, 2, 4, felloe),
    "tyre": (208, 50, -1.25, -8.5, -2.25, 2.5, 1, 4.5, tyre),
}


def float_faces(u, v, x0, y0, z0, w, hh, d):
    """face_texels for float sizes: the UV edges are the float values; every pixel a face's rectangle touches gets
    painted (a pixel shared by two faces of the same box takes the later one), sampled at its clamped centre."""
    x1, y1, z1 = x0 + w, y0 + hh, z0 + d
    rects = (("top", u + d, v, w, d), ("bottom", u + d + w, v, w, d), ("right", u, v + d, d, hh),
             ("left", u + d + w, v + d, d, hh), ("front", u + d, v + d, w, hh), ("back", u + 2 * d + w, v + d, w, hh))
    for face, fu, fv, fw, fh in rects:
        for tx in range(math.floor(fu), math.ceil(fu + fw)):
            for ty in range(math.floor(fv), math.ceil(fv + fh)):
                s = min(1.0, max(0.0, (tx + 0.5 - fu) / fw))
                t = min(1.0, max(0.0, (ty + 0.5 - fv) / fh))
                yield face, tx, ty, *{
                    "top": (x0 + s * w, y0, z1 - t * d), "bottom": (x0 + s * w, y1, z1 - t * d),
                    "right": (x0, y0 + t * hh, z1 - s * d), "left": (x1, y0 + t * hh, z0 + s * d),
                    "front": (x0 + s * w, y0 + t * hh, z0), "back": (x1 - s * w, y0 + t * hh, z1)}[face]


def paint_f(boxes, size):
    W, H = size
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    owner = {}
    for name, (u, v, x0, y0, z0, w, hh, d, mat) in boxes.items():
        for face, tx, ty, x, y, z in float_faces(u, v, x0, y0, z0, w, hh, d):
            assert 0 <= tx < W and 0 <= ty < H, (name, tx, ty)
            assert owner.setdefault((tx, ty), name) == name, f"UV overlap {name} / {owner[(tx, ty)]} at {tx},{ty}"
            c = mat(None, face, x, y, z, tx, ty)
            px[tx, ty] = c if len(c) == 4 else (*c, 255)
    return img


# ---------------------------------------------------------------- items
def iron_wheel_px(fx, fy, x, y, cx=8, cy=8, r=7.0):
    """wooden_wheel.png's wheel, with a dark iron tyre round the felloes and an iron hub."""
    d = math.hypot(fx - cx, fy - cy)
    if d > r:
        return None
    if d > r - 0.9:
        return FORGED[2] if abs(math.atan2(fy - cy, fx - cx) * 6 / math.pi % 2 - 1) < 0.15 else FORGED[1]
    if d > r - 2.0:
        return PLANK[1 + (d > r - 1.4)]
    if d < 1.6:
        return IRON[2] if d < 1.0 else FORGED[1]
    a = math.atan2(fy - cy, fx - cx)
    for k in range(6):
        da = (a - k * math.pi / 3 + math.pi) % (2 * math.pi) - math.pi
        if abs(da) * d < 0.75:
            return PLANK[3] if k < 3 else PLANK[2]
    return None


def wagon_px(fx, fy, x, y):
    if 5 <= y <= 8 and 2 <= x <= 15:                       # side boards: two planks, iron corner straps
        if x in (2, 15) or x == 9 and y in (5, 7):
            return FORGED[1 + (y == 6)]
        return PLANK[3] if y == 5 else PLANK[2] if y in (6, 7) else PLANK[1]
    if y in (3, 4) and x in (3, 9, 14):                    # stakes with iron caps
        return FORGED[2] if y == 3 else PLANK[2]
    for cx in (5.0, 12.5):                                 # near wheels under the bed
        w = iron_wheel_px(fx, fy, x, y, cx, 12.0, 3.6)
        if w:
            return w
    for cx in (6.0, 13.5):                                 # the far pair peeking out behind
        w = iron_wheel_px(fx, fy, x, y, cx, 11.0, 3.6)
        if w:
            return shade(w, 0.7)
    if 0 <= x <= 1 and y == 9 or x == 0 and y == 10:
        return FORGED[2] if x == 0 else PLANK[2]           # the pole, iron ring at its tip
    return None


def main():
    # self-check: the float painter agrees with wildlife_paint's on whole-pixel boxes
    ints = {k: (*b[:5], *(int(n) for n in b[5:8]), b[8]) for k, b in BOXES.items() if all(float(n).is_integer() for n in b[5:8])}
    assert len(ints) == len(BOXES) - 1
    assert paint_f(ints, (256, 128)).tobytes() == paint(ints, None, (256, 128)).tobytes()
    paint_f(BOXES, (256, 128)).save(T / "entity/wagon.png")
    item(wagon_px).save(T / "item/wagon.png")
    item(iron_wheel_px).save(T / "item/iron_rimmed_wheel.png")
    print("wagon textures ok")


if __name__ == "__main__":
    main()
