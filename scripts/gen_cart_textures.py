"""Textures for the Wheeled Cart and horse taming (Carting.java):
- entity/cart.png (128x128) for client/CartModel.java: BOXES mirrors its cubes (texOffs, origin, size)
- entity/wildlife/hide_saddle_blanket.png (128x64): the blanket over the tamed horse's back, drawn on an inflated
  WildHorseModel (client/wildlife/TameHorseRenderer); only the body box is painted, everything else stays transparent
- items: cart, wooden_wheel, rope_halter, hide_saddle_blanket (16x16)
Wood is planed oak planks, the hub a cast bronze cap, the felloes pinned with bronze nails; colours for cordage, tool rods,
bronze and hide are sampled from the mod's own item textures so everything matches. Deterministic, overwrites its outputs.
Run: python scripts/gen_cart_textures.py"""
import math
from pathlib import Path

from PIL import Image

from wildlife_paint import fur, h, lerp, paint, shade

T = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def palette(name, n=4):
    """The n most common opaque colours of a mod texture, dark to light."""
    img = Image.open(T / name).convert("RGBA")
    counts = {}
    for c in (img.get_flattened_data() if hasattr(img, "get_flattened_data") else img.getdata()):
        if c[3] > 200:
            counts[c[:3]] = counts.get(c[:3], 0) + 1
    common = sorted(counts, key=counts.get, reverse=True)[:n]
    return sorted(common, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])


CORD = palette("item/cordage.png")
ROD = palette("item/tool_rod.png")
BRONZE = palette("item/bronze_ingot.png")
HIDE = palette("item/scraped_hide.png")
PLANK = [(104, 78, 44), (138, 106, 62), (164, 130, 80), (186, 152, 98)]   # oak planks, dark to light
LEATHER = [(98, 56, 32), (128, 74, 42), (150, 92, 54), (172, 112, 68)]


def grain(c, along, across, salt, amt=0.07):
    """Wood: streaks running along the board (constant along `along`, varying across), plus a little speckle."""
    streak = (h(int(across), 0, salt) - 0.5) * 2 * amt
    knot = h(int(along) // 3, int(across), salt + 5) > 0.985
    c = shade(c, 1 + streak + (h(int(along), int(across), salt + 9) - 0.5) * amt * 0.6)
    return shade(c, 0.75) if knot else c


def plank_mat(seam_axis, width, nail_every=0):
    """Planks running along z, `width` px wide across `seam_axis` ('x' or 'y'); seams darker, bronze nails at the ends."""
    def mat(v, face, x, y, z, i, j):
        across = x if seam_axis == "x" else y
        if face in ("front", "back"):                     # end grain
            ring = (h(int(x), int(y), 31) - 0.5) * 0.1
            return shade(PLANK[1], 0.92 + ring)
        if face in ("left", "right") and seam_axis == "x":
            across = y                                    # the bed's thin edges: one board's side
        c = grain(PLANK[2], z, across * 2, 11)
        if (across % width) < 1.0 and face not in ("left", "right") or seam_axis == "y" and (across % width) < 1.0:
            c = shade(PLANK[0], 1.05)                     # seam between boards
        if nail_every and abs(((z + 14) % nail_every) - nail_every / 2) < 0.6 and (across % width) > width - 1.5:
            c = BRONZE[2]
        return c
    return mat


def end_board(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        return shade(PLANK[1], 0.92 + (h(i, j, 33) - 0.5) * 0.1)
    c = grain(PLANK[2], x, y * 2, 13)
    if y % 2 < 1.0 and abs(y + 13) < 0.6:
        c = shade(PLANK[0], 1.05)
    if abs(abs(x) - 8.5) < 0.6 and face in ("front", "back") and abs(y + 13) < 1.1:
        c = BRONZE[2]                                     # nails into the side boards
    return c


def stake(v, face, x, y, z, i, j):
    if face in ("top", "bottom"):
        return ROD[1]
    return fur(lerp(ROD[1], ROD[2], h(i, 0, 41)), i, j, 42, 0.06)


def axle(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        return BRONZE[1]                                  # the bronze linchpins at the ends
    return grain(shade(PLANK[1], 0.8), x, y * 3 + z, 17, 0.05)


def shaft(v, face, x, y, z, i, j):
    if face in ("front", "back"):
        return ROD[1]
    c = grain(ROD[2], z, x + y * 3, 19, 0.06)
    if abs(z + 26) < 1.0:                                 # a cordage wrap where the harness rope sits
        c = CORD[1 + (int(z * 2 + y) % 2)]
    return c


def hub(v, face, x, y, z, i, j):
    """Cast bronze cap: bright faces with a dark rim and a centre boss on the outer (and inner) face."""
    edge = max(abs(y), abs(z)) > 1.5 if face in ("left", "right") else max(abs(x), abs(y if face != "top" and face != "bottom" else z)) > 1.5
    if face in ("left", "right") and abs(y) < 0.6 and abs(z) < 0.6:
        return BRONZE[0]                                  # axle pin hole
    return BRONZE[1] if edge else BRONZE[2 + (h(i, j, 51) > 0.7)]


def spoke(v, face, x, y, z, i, j):
    return grain(PLANK[3], y, x + z, 23, 0.05)


def felloe(v, face, x, y, z, i, j):
    if face == "top":                                     # running surface, worn, with a bronze nail in the middle
        if abs(z) < 0.6 and abs(x) < 0.6:
            return BRONZE[3]
        return shade(PLANK[1], 0.85 + (h(i, j, 61) - 0.5) * 0.1)
    if face in ("front", "back"):
        return shade(PLANK[1], 0.9)
    return grain(PLANK[2], z, y * 2, 29, 0.05)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors CartModel.create
BOXES = {
    "floor": (0, 0, -10, -11, -14, 20, 2, 28, plank_mat("x", 4)),
    "board": (0, 30, 10, -16, -14, 2, 7, 28, plank_mat("y", 3.5, 7)),
    "end_board": (0, 66, -10, -15, -14, 20, 4, 2, end_board),
    "stake": (44, 66, -1, -19, -1, 2, 10, 2, stake),
    "axle": (52, 66, -14, -1, -1, 28, 2, 2, axle),
    "shaft": (60, 34, -1, -1, -30, 2, 2, 30, shaft),
    "hub": (0, 72, -2, -2, -2, 4, 4, 4, hub),
    "spoke": (16, 72, -0.5, -5, -0.5, 1, 10, 1, spoke),
    "felloe": (20, 72, -1, -7, -2, 2, 2, 4, felloe),
}


# ---------------------------------------------------------------- blanket
def blanket(v, face, x, y, z, i, j):
    """Body box of WildHorseModel (x -4.5..4.5, y -5..5 top to belly, z -10..10 front to back)."""
    clear = (0, 0, 0, 0)
    strap = abs(z + 3.5) < 0.6                            # cordage girth round the barrel
    if face in ("front", "back"):
        return clear
    if face == "bottom":
        return (*CORD[1 + (i % 2)], 255) if strap else clear
    on = -7.5 < z < 2.5 and (face == "top" or y < 1.0)
    if not on:
        return (*CORD[1 + ((i + j) % 2)], 255) if strap else clear
    border = z < -6.5 or z > 1.5 or (face != "top" and y > 0.0)
    if face != "top" and y > 0.0 and h(i, 0, 71) < 0.35:
        return clear                                      # ragged fringe along the lower edge
    if strap:
        return (*CORD[2 - (j % 2)], 255)
    c = lerp(HIDE[1], HIDE[2], h(i, j, 73))
    c = fur(c, i, j, 74, 0.05, streak=False)
    if border:
        c = shade(HIDE[0], 0.85)
        if (i + j) % 3 == 0:
            c = CORD[1]                                   # stitched hem
    elif face == "top" and abs(x) < 1.0:
        c = shade(c, 0.9)                                 # crease over the spine
    return (*c, 255)


BLANKET_BOXES = {"body": (0, 0, -4.5, -5, -10, 9, 10, 20, blanket)}


# ---------------------------------------------------------------- items
def item(draw):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            c = draw(x + 0.5, y + 0.5, x, y)
            if c:
                img.putpixel((x, y), (*c, 255))
    return outline(img)


def outline(img):
    """Darkens the lower/right edge pixels of the shape, as vanilla item art."""
    px = img.load()
    out = img.copy()
    po = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and (x == 15 or y == 15 or not px[min(15, x + 1), y][3] or not px[x, min(15, y + 1)][3]):
                po[x, y] = (*shade(px[x, y][:3], 0.7), 255)
    return out


def wheel_px(fx, fy, x, y, cx=8, cy=8, r=7.0):
    d = math.hypot(fx - cx, fy - cy)
    if d > r:
        return None
    if d > r - 1.6:
        return BRONZE[2] if abs(math.atan2(fy - cy, fx - cx) * 6 / math.pi % 2 - 1) < 0.15 else PLANK[1 + (d > r - 0.8)]
    if d < 1.6:
        return BRONZE[2] if d < 1.0 else BRONZE[1]
    a = math.atan2(fy - cy, fx - cx)
    for k in range(6):
        da = (a - k * math.pi / 3 + math.pi) % (2 * math.pi) - math.pi
        if abs(da) * d < 0.75:
            return PLANK[3] if k < 3 else PLANK[2]
    return None


def cart_px(fx, fy, x, y):
    w = wheel_px(fx, fy, x, y, 9.5, 11.0, 4.5)
    if w:
        return w
    if 6 <= y <= 8 and 2 <= x <= 15:                       # bed side boards
        return PLANK[3] if y == 6 else PLANK[2] if y == 7 else PLANK[1]
    if y == 5 and x in (3, 14):                            # stakes
        return ROD[2]
    if 0 <= x <= 3 and abs(fy - (8.5 + (x - 3) * 0.6)) < 0.6 and y >= 8:
        return ROD[2]                                     # shaft reaching out front
    if x <= 1 and 9 <= y <= 10:
        return ROD[1]
    return None


def halter_px(fx, fy, x, y):
    # noseband loop (an ellipse ring) and the crown piece over it, with a lead rope tailing off
    e = ((fx - 7.5) / 5.2) ** 2 + ((fy - 6.5) / 4.0) ** 2
    if 0.5 < e < 1.12:
        return CORD[2 + ((x + y) % 2 == 0)] if fy < 6.5 else CORD[1 + ((x + y) % 2 == 0)]
    knot = math.hypot(fx - 9.5, fy - 10.3) < 1.3
    if knot:
        return CORD[1]
    if 10 <= y <= 15 and abs(fx - (9.5 + (fy - 10) * 0.55)) < 0.8:
        return CORD[2 - (y % 2)]
    return None


def blanket_px(fx, fy, x, y):
    if not (2 <= x <= 13 and 3 <= y <= 13):
        return None
    if x in (2, 13) or y in (3, 13):
        return CORD[1] if (x + y) % 2 == 0 else shade(HIDE[0], 0.85)
    if 7 <= x <= 8:
        return CORD[2 - (y % 2)]                           # the girth strap across
    c = lerp(HIDE[1], HIDE[2], h(x, y, 81))
    return shade(c, 0.9) if y < 6 else c


def main():
    cart = paint(BOXES, None, (128, 128))
    cart.save(T / "entity/cart.png")
    paint(BLANKET_BOXES, None, (128, 64)).save(T / "entity/wildlife/hide_saddle_blanket.png")
    for name, fn in (("wooden_wheel", wheel_px), ("cart", cart_px), ("rope_halter", halter_px), ("hide_saddle_blanket", blanket_px)):
        item(fn).save(T / f"item/{name}.png")
    print("cart textures ok")


if __name__ == "__main__":
    main()
