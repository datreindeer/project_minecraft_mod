"""Textures for the canvas tilts over Trader Caravans' carts and wagons (client/TiltModel): the cart's 128x128, the wagon's
256x128, with the same UV boxes as the model. Woven canvas in two stripes, every other arch panel in the second (an awning's
bands), darker where the bent hoops run under it (every 7 px along) and at the hemmed ends, a band along the top of each
side panel and road dust toward its bottom; the end hoops are bent wood. One plain linen pair (cart_canopy, wagon_canopy)
and one pair dyed per caravan kind (people/TraderKinds, scripts/gen_traders.py): <cart|wagon>_canopy_<kind>.png. A kind
without a row in KINDS keeps the plain linen tilt (client/CargoRender.tilt).
Run: python scripts/gen_cart_canopy_textures.py"""
import math
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity"
LINEN = (214, 200, 168)
DUST = (168, 146, 112)
WOOD = (122, 88, 54)
# kind: (stripe A, stripe B, side band, patches?)
KINDS = {
    "woodsman": ((92, 110, 62), (188, 170, 128), (70, 52, 32), False),   # moss green and undyed, a bark-brown band
    "mining": ((92, 88, 82), (150, 96, 64), (52, 48, 44), False),         # soot grey and rust
    "jewelry": ((44, 58, 138), (204, 162, 62), (112, 36, 52), False),     # deep blue and gold, a crimson band
    "hunting": ((124, 86, 54), (84, 60, 40), (40, 30, 22), False),        # hide brown and dark brown
    "herbs": ((126, 146, 96), (196, 152, 70), (88, 104, 60), False),      # sage and ochre
    "foraging": ((198, 182, 142), (156, 124, 84), (110, 84, 52), True),   # oatmeal and brown, patched
}
SEGMENTS = 6


def seg_width(radius):
    return math.ceil(2 * radius * math.sin(math.pi / SEGMENTS / 2) + 0.3)


# as TiltModel.cart() / wagon(): radius, rise, length, UV corners of stripe A, stripe B, sides, hoops, texture size
LAYOUTS = {
    "cart": (11, 5, 30, (0, 0), (0, 32), (0, 64), (64, 64), (128, 128)),
    "wagon": (13, 7, 48, (0, 0), (128, 0), (0, 50), (128, 60), (256, 128)),
}


def faces(u, v, w, h, d):
    """face -> (x0, y0, fw, fh), the usual box layout."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}


def shade(c, f):
    return tuple(max(0, min(255, round(ch * f))) for ch in c)


def mix(a, b, t):
    return tuple(x + (y - x) * t for x, y in zip(a, b))


def paint(img, r, box, base, band=None, patches=False, side=False):
    u, v, w, h, d = box
    for face, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
        for y in range(fh):
            for x in range(fw):
                along = y if face in ("top", "bottom") else x if face in ("east", "west") else None
                f = (1.06 if (x + y) % 2 == 0 else 0.94) * (1 + r.uniform(-0.035, 0.035))  # the weave
                c = base
                if patches and along is not None and (along // 6 + x // 3) % 5 == 0 and face in ("top", "east", "west"):
                    c = mix(base, (120, 96, 62), 0.45)  # a sewn-on patch
                    if along % 6 in (0, 5):
                        f *= 0.8  # its stitched edge
                if side and face in ("east", "west"):
                    t = y / max(1, fh - 1)
                    if band is not None and y <= 1:
                        c = band  # a band along the top of the side panel
                    else:
                        c = mix(c, DUST, t * t * 0.55)  # road dust toward the bottom
                if along is not None and along % 7 == 1:
                    f *= 0.8  # a hoop under the canvas
                if along is not None and along in (0, d - 1):
                    f *= 0.72  # hemmed ends
                if face in ("north", "south"):
                    f *= 0.82
                img.putpixel((x0 + x, y0 + y), shade(c, f) + (255,))


def wood(img, corner, r):
    x0, y0 = corner
    for y in range(6):
        for x in range(24):
            grain = 0.9 + 0.12 * math.sin(x * 0.9 + y * 2.1) + r.uniform(-0.04, 0.04)
            img.putpixel((x0 + x, y0 + y), shade(WOOD, grain) + (255,))


def build(layout, a, b, band, patches):
    radius, rise, length, ua, ub, us, uh, size = LAYOUTS[layout]
    r = random.Random(f"tilt-{layout}-{a}")
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    w = seg_width(radius)
    paint(img, r, (*ua, w, 1, length), a, patches=patches)
    paint(img, r, (*ub, w, 1, length), b, patches=patches)
    paint(img, r, (*us, 1, rise, length), a, band=band, patches=patches, side=True)
    wood(img, uh, r)
    return img


def main():
    for layout in LAYOUTS:
        build(layout, LINEN, shade(LINEN, 0.92), None, False).save(OUT / f"{layout}_canopy.png")
        for kind, (a, b, band, patches) in KINDS.items():
            build(layout, a, b, band, patches).save(OUT / f"{layout}_canopy_{kind}.png")
    print(f"tilt textures written: plain + {len(KINDS)} kinds, cart and wagon")


if __name__ == "__main__":
    main()
