"""Texture for a Trader Caravan's prepared carcass (client/CarcassRenderer.Lashing, CarcassEntity#isPrepared): 64x64,
the same UV boxes as the model. The wrap (a 16 px cube) is a salted hide, pale and speckled with salt, tied with two dark
cords round it and one along the top where it hangs from the pole; the pole (2x2x16) is barked wood with cut ends.
Run: python scripts/gen_carcass_lashing_textures.py"""
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/carcass_lashing.png"
HIDE = (196, 176, 140)
SALT = (236, 232, 222)
CORD = (92, 70, 44)
BARK = (104, 80, 54)
WOOD = (178, 146, 98)
# (u, v, w, h, d) as in CarcassRenderer.Lashing: wrap 16x16x16, pole 2x2x16
WRAP, POLE = (0, 0, 16, 16, 16), (0, 32, 2, 2, 16)
BANDS = (3, 4, 11, 12)  # cords round the wrap, px along the body (model z)


def faces(u, v, w, h, d):
    """face -> (x0, y0, fw, fh), vanilla cube UV layout."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "east": (u, v + d, d, h),
            "north": (u + d, v + d, w, h), "west": (u + d + w, v + d, d, h), "south": (u + d + w + d, v + d, w, h)}


def clamp(c, f):
    return tuple(max(0, min(255, round(ch * f))) for ch in c) + (255,)


def main():
    r = random.Random("carcass-lashing")
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for face, (x0, y0, fw, fh) in faces(*WRAP).items():
        for y in range(fh):
            for x in range(fw):
                # along the body: rows on top/bottom, columns on the sides; the ends (north/south) show the hide's folds
                along = y if face in ("top", "bottom") else x if face in ("east", "west") else None
                c, f = HIDE, 1 + r.uniform(-0.07, 0.07)
                if r.random() < 0.12:
                    c = SALT
                if face in ("north", "south"):
                    f *= 0.85 if (x + y // 3) % 5 else 0.7  # folded-over hide
                    if y in (3, 4, 11, 12) or x in (7, 8):
                        c, f = CORD, 1 + r.uniform(-0.1, 0.1)  # the cords knotted over the ends
                if along in BANDS or face == "top" and x in (7, 8):
                    c, f = CORD, (1.15 if (along or 0) % 2 == 0 else 0.9) * (1 + r.uniform(-0.08, 0.08))  # twisted cord
                img.putpixel((x0 + x, y0 + y), clamp(c, f))
    for face, (x0, y0, fw, fh) in faces(*POLE).items():
        for y in range(fh):
            for x in range(fw):
                end = face in ("north", "south")
                c = WOOD if end else BARK
                f = 1 + r.uniform(-0.12, 0.12)
                if end and (x, y) == (0, 0):
                    f *= 0.8  # the heart of the cut end
                if not end and r.random() < 0.15:
                    f *= 0.75  # bark furrows
                img.putpixel((x0 + x, y0 + y), clamp(c, f))
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print("carcass lashing texture written")


if __name__ == "__main__":
    main()
