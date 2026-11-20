"""Texture for the canvas tilt over a Trader Caravan's cart (client/CartModel.Canopy, CartEntity#isCaravan): 128x128,
the same UV boxes as the model. Weathered linen canvas, darker where the bent hoops run under it (every 7 px along the
cart) and along the hems, dusty toward the bottom of the side panels.
Run: python scripts/gen_cart_canopy_textures.py"""
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/cart_canopy.png"
CANVAS = (214, 200, 168)
DUST = (168, 146, 112)
# (u, v, w, h, d) as in CartModel.Canopy: roof 16x1x30, side panels 1x16x30
BOXES = [(0, 0, 16, 1, 30), (0, 32, 1, 16, 30)]


def faces(u, v, w, h, d):
    """face -> (x0, y0, fw, fh); 'along' tells which texture axis runs along the cart (model z)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}


def main():
    r = random.Random("caravan-canopy")
    img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    for u, v, w, h, d in BOXES:
        for face, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    # position along the cart, and across it (0 = upper edge of a side panel)
                    along = y if face in ("top", "bottom") else x if face in ("east", "west") else None
                    f = (1.05 if (x + y) % 2 == 0 else 0.95) * (1 + r.uniform(-0.04, 0.04))  # weave
                    c = CANVAS
                    if face in ("east", "west") and h > 1:
                        t = y / (fh - 1)
                        c = tuple(a + (b - a) * t * t * 0.6 for a, b in zip(CANVAS, DUST))  # road dust
                    if along is not None and along % 7 == 1:
                        f *= 0.78  # a hoop under the canvas
                    if along is not None and along in (0, d - 1):
                        f *= 0.7   # hemmed ends
                    if face in ("north", "south"):
                        f *= 0.8
                    img.putpixel((x0 + x, y0 + y), tuple(max(0, min(255, round(ch * f))) for ch in c) + (255,))
    img.save(OUT)
    print("cart canopy texture written")


if __name__ == "__main__":
    main()
