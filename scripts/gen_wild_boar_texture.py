"""Wild boar entity texture (64x64) for WildBoarModel, plus its skinned (flesh) version.

Usage: python gen_wild_boar_texture.py
Box UVs below must match WildBoarModel.createBodyLayer() (texOffs, w, h, d).
Face-local coords are as seen from outside the box, top row up; on the right (west, -x) side column 0 is the
back, on the left (east, +x) side column 0 is the front; on top/bottom row 0 is the back.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_skinned_textures import skin  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity"
OUT = ROOT / "wildlife/wild_boar.png"
OUT_SKINNED = ROOT / "skinned/wildlife/wild_boar.png"
SIZE = 64

# name: (u, v, w, h, d)
BOXES = {
    "chest": (0, 0, 10, 8, 9),
    "rump": (0, 17, 9, 6, 10),
    "hump": (0, 56, 8, 1, 6),
    "crest_front": (0, 41, 0, 3, 8),
    "crest_back": (16, 44, 0, 3, 5),
    "rump_crest": (0, 46, 0, 3, 7),
    "skull": (0, 33, 7, 7, 5),
    "bridge": (40, 25, 5, 5, 2),
    "snout": (40, 16, 4, 4, 4),
    "disc": (40, 37, 5, 4, 1),
    "jaw": (40, 32, 3, 1, 4),
    "tusk": (56, 4, 1, 2, 1),
    "ear": (56, 0, 2, 3, 1),
    "tail": (60, 4, 1, 6, 1),
    "tuft": (52, 9, 2, 2, 2),
    "leg_upper": (40, 0, 4, 5, 4),
    "leg_lower": (40, 9, 3, 4, 3),
}

# coat: near-black brown to grey-brown, grizzled straw tips
C = [(34, 28, 23), (49, 41, 34), (64, 54, 44), (80, 67, 54), (97, 82, 65)]
TIP = [(114, 100, 80), (136, 120, 96), (158, 142, 116)]
CREST = [(16, 13, 12), (26, 21, 18), (38, 31, 26)]
SKIN = [(44, 38, 36), (58, 50, 47), (72, 62, 57)]
DISC = [(92, 72, 70), (116, 92, 88), (136, 110, 104)]
NOSTRIL = (38, 24, 24)
HOOF = [(24, 22, 21), (40, 37, 35), (62, 58, 54)]
IVORY = [(196, 184, 154), (226, 216, 190), (244, 238, 220)]
EYE, EYE_GLINT, EYE_RIM = (14, 10, 9), (96, 70, 48), (30, 24, 21)


def h(x, y, salt):
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000 / 1000


def pick(pal, t):
    return pal[max(0, min(len(pal) - 1, int(t * len(pal))))]


def faces(u, v, w, hh, d):
    """face -> (x0, y0, width, height) in texture space."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
        "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh),
    }


def bristle(gx, gy, salt, tone=0.0, tips=0.12, pal=C):
    """Coarse bristly coat: each column is a run of vertical bristle strokes 2-4 px long. A stroke is lighter at its
    tip (top pixel, sometimes a grizzled straw tip) and darkens towards the root."""
    tips *= 2.2  # tips are per stroke, not per pixel
    off = int(h(gx, 1, salt) * 4)
    length = 2 + int(h(gx, 0, salt + 1) * 3)
    k, j = divmod(gy + off, length)
    stroke = h(gx, k, salt)
    if j == 0 and h(gx, k, salt + 3) < tips:
        return pick(TIP, h(gx, k, salt + 4) * 0.8 + max(0.0, tone))
    t = 0.25 + 0.5 * stroke + tone - 0.22 * j / (length - 1) + 0.08 * (h(gx, gy, salt + 2) - 0.5)
    return pick(pal, t)


def paint(img, name, fn, mirror_ok=True):
    """fn(face, x, y, fw, fh, gx, gy) -> rgb or rgba or None (transparent)."""
    u, v, w, hh, d = BOXES[name]
    px = img.load()
    for face, (x0, y0, fw, fh) in faces(u, v, w, hh, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh, x0 + x, y0 + y)
                px[x0 + x, y0 + y] = (0, 0, 0, 0) if c is None else (c if len(c) == 4 else (*c, 255))


def front_frac(face, x, fw):
    """0 at the back, 1 at the front of the box, for side faces."""
    if face == "right":
        return x / max(1, fw - 1)
    if face == "left":
        return 1 - x / max(1, fw - 1)
    return 0.5


# --- parts -----------------------------------------------------------------------------------------------------

def chest(face, x, y, fw, fh, gx, gy):
    if face == "top":
        spine = abs(x - (fw - 1) / 2) < 1.1  # black dorsal stripe under the crest
        if spine:
            return pick(CREST, h(gx, gy, 9))
        return bristle(gx, gy, 11, tone=-0.1 + 0.08 * abs(x - fw / 2) / (fw / 2), tips=0.14)
    if face == "bottom":
        return bristle(gx, gy, 12, tone=-0.3, tips=0.04)
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        tone = -0.08 * (y == 0) - 0.18 * (y >= fh - 2) + 0.06 * f  # shoulders a touch lighter, belly darker
        return bristle(gx, gy, 13, tone=tone, tips=0.10 + 0.08 * f)
    if face == "front":  # throat/neck behind the head
        return bristle(gx, gy, 14, tone=-0.2, tips=0.08)
    return bristle(gx, gy, 15, tone=-0.1)


def rump(face, x, y, fw, fh, gx, gy):
    if face == "top":
        if abs(x - (fw - 1) / 2) < 0.6:
            return pick(CREST, h(gx, gy, 19) + 0.3)
        return bristle(gx, gy, 21, tone=-0.12, tips=0.12)
    if face == "bottom":
        return bristle(gx, gy, 22, tone=-0.3, tips=0.04)
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        tone = -0.08 * (y == 0) - 0.18 * (y >= fh - 2) - 0.1 * (1 - f) * (y >= fh - 3)  # dark ham
        return bristle(gx, gy, 23, tone=tone, tips=0.11)
    if face == "back":
        if abs(x - (fw - 1) / 2) < 0.6 and y <= 1:
            return CREST[1]
        return bristle(gx, gy, 24, tone=-0.15 - 0.1 * (y >= fh - 2), tips=0.08)
    return bristle(gx, gy, 25, tone=-0.1)


def make_crest(hs, seed):
    """hs: visible bristle height (0..2 px) per z column, back to front."""
    n = len(hs)

    def fn(face, x, y, fw, fh, gx, gy):
        if face not in ("right", "left"):
            return CREST[0]  # zero-area faces
        z = int(round(front_frac(face, x, fw) * (n - 1)))
        top = fh - 1 - hs[z]  # bottom row is buried in the back
        if y < top:
            return None
        if y == top and hs[z] > 0:
            tip = h(z, seed, 5)
            return TIP[0] if tip < 0.25 else (C[2] if tip < 0.6 else CREST[2])
        return pick(CREST, h(gx, gy, seed + 1) * 0.9)
    return fn


def hump(face, x, y, fw, fh, gx, gy):
    if face == "top":
        if abs(x - (fw - 1) / 2) < 1.1:
            return pick(CREST, h(gx, gy, 8))
        return bristle(gx, gy, 16, tone=-0.05, tips=0.14)
    return bristle(gx, gy, 17, tone=-0.08, tips=0.12)


def skull(face, x, y, fw, fh, gx, gy):
    if face in ("right", "left"):
        f = front_frac(face, x, fw)  # 1 = towards the snout
        col = round(f * (fw - 1))
        if y == 2 and col == fw - 2:
            return EYE
        if y == 2 and col == fw - 3:
            return EYE_GLINT  # brown rear corner of the small eye
        if (y == 1 and col == fw - 2) or (y == 3 and col == fw - 2):
            return EYE_RIM
        # lighter grizzled cheek and jowl under the eye, dark brow line above
        if y == 1:
            return bristle(gx, gy, 31, tone=-0.25, tips=0.05)
        cheek = y >= 4
        return bristle(gx, gy, 32, tone=0.1 * cheek - 0.05 + 0.05 * f, tips=0.12 + 0.16 * cheek)
    if face == "top":
        if abs(x - (fw - 1) / 2) < 0.6 and y <= 2:  # crest runs onto the nape
            return CREST[1]
        return bristle(gx, gy, 33, tone=-0.1, tips=0.12)
    if face == "front":  # forehead above the muzzle
        return bristle(gx, gy, 34, tone=-0.15 + 0.05 * (y < 2), tips=0.08)
    if face == "bottom":
        return bristle(gx, gy, 35, tone=-0.1, tips=0.14)
    return bristle(gx, gy, 36, tone=-0.2)


def bridge(face, x, y, fw, fh, gx, gy):
    return bristle(gx, gy, 41, tone=-0.2 + 0.05 * (face == "top"), tips=0.07)


def snout(face, x, y, fw, fh, gx, gy):
    if face == "top":  # wrinkled skin, sparse short hairs
        if y in (1, 3):
            return SKIN[0]
        return pick(SKIN, h(gx, gy, 51) * 0.9 + 0.1)
    if face in ("right", "left"):
        if y == fh - 1:
            return SKIN[0]  # lip line
        return pick(SKIN, h(gx, gy, 52) * 0.8 + 0.1) if h(gx, gy, 53) > 0.15 else C[1]
    return pick(SKIN, h(gx, gy, 54) * 0.6)


def disc(face, x, y, fw, fh, gx, gy):
    if face == "front":
        if y in (1, 2) and x in (1, 3):
            return NOSTRIL if y == 2 else DISC[0]
        edge = x in (0, fw - 1) or y in (0, fh - 1)
        if edge:
            return DISC[0] if (x in (0, fw - 1) and y in (0, fh - 1)) else DISC[1]
        return DISC[2] if x == 2 and y == 1 else DISC[1]
    return DISC[0]  # rim seen from the side


def jaw(face, x, y, fw, fh, gx, gy):
    if face == "front":
        return DISC[0]  # pinkish lower lip
    return pick(SKIN, h(gx, gy, 61) * 0.6)


def tusk(face, x, y, fw, fh, gx, gy):
    if face == "top":
        return IVORY[2]
    if face == "bottom":
        return IVORY[0]
    return IVORY[2] if y == 0 else (IVORY[1] if face in ("front", "left", "right") else IVORY[0])


def ear(face, x, y, fw, fh, gx, gy):
    if face == "front":  # inner ear: hairy, brownish pink deep inside
        if y == 0:
            return CREST[0]
        return (88, 64, 58) if y == fh - 1 else (66, 50, 44)
    if y == 0:
        return CREST[0]  # black tufted tips
    return bristle(gx, gy, 71, tone=-0.15, tips=0.1)


def tail(face, x, y, fw, fh, gx, gy):
    return pick(C, 0.15 + 0.3 * h(gx, gy, 81))


def tuft(face, x, y, fw, fh, gx, gy):
    if face == "top":
        return C[1]
    return CREST[0] if h(gx, gy, 82) < 0.6 else (CREST[2] if h(gx, gy, 83) < 0.7 else TIP[0])


def leg_upper(face, x, y, fw, fh, gx, gy):
    if face == "top":
        return C[1]
    return bristle(gx, gy, 91, tone=-0.12 - 0.07 * y, tips=0.08)


def leg_lower(face, x, y, fw, fh, gx, gy):
    if face == "top":
        return C[0]
    if face == "bottom":  # sole: cloven hoof
        return HOOF[0] if x == 1 else HOOF[1]
    if y == fh - 1:
        if face == "front" and x == 1:
            return HOOF[0]  # cleft
        return HOOF[1] if h(gx, gy, 93) < 0.6 else HOOF[2]
    if y == fh - 2:
        return HOOF[2] if face == "front" and x != 1 else HOOF[1]  # hoof top edge catches light
    return pick(C, 0.1 + 0.35 * h(gx, gy, 92))


def build():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    paint(img, "chest", chest)
    paint(img, "rump", rump)
    paint(img, "hump", hump)
    # front: on the withers, onto the nape; back: rear of the chest; rump: fading out towards the tail
    paint(img, "crest_front", make_crest([1, 2, 2, 2, 1, 2, 2, 1], 101))
    paint(img, "crest_back", make_crest([1, 1, 2, 1, 2], 102))
    paint(img, "rump_crest", make_crest([0, 0, 1, 0, 1, 1, 1], 103))
    paint(img, "skull", skull)
    paint(img, "bridge", bridge)
    paint(img, "snout", snout)
    paint(img, "disc", disc)
    paint(img, "jaw", jaw)
    paint(img, "tusk", tusk)
    paint(img, "ear", ear)
    paint(img, "tail", tail)
    paint(img, "tuft", tuft)
    paint(img, "leg_upper", leg_upper)
    paint(img, "leg_lower", leg_lower)
    return img


def face_pixels(name):
    u, v, w, hh, d = BOXES[name]
    return [(fx + x, fy + y) for fx, fy, fw, fh in faces(u, v, w, hh, d).values() for y in range(fh) for x in range(fw)]


def check(img):
    """Every box face pixel is painted (crests may be transparent above the bristles), nothing outside the boxes."""
    used = set()
    for name in BOXES:
        px = face_pixels(name)
        assert not (used & set(px)), f"{name} overlaps another box"
        used |= set(px)
        if "crest" not in name:
            assert all(img.getpixel(p)[3] == 255 for p in px), f"{name} has holes"
    assert all(img.getpixel((x, y))[3] == 0 for x in range(SIZE) for y in range(SIZE) if (x, y) not in used)


def main():
    img = build()
    check(img)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)

    flesh = skin(img.copy())
    px, src = flesh.load(), img.load()
    for p in face_pixels("tusk"):  # tusks stay ivory
        px[p] = src[p]
    assert flesh.size == img.size and flesh.getchannel("A").tobytes() == img.getchannel("A").tobytes()
    OUT_SKINNED.parent.mkdir(parents=True, exist_ok=True)
    flesh.save(OUT_SKINNED)
    print("wrote", OUT, "and", OUT_SKINNED)


if __name__ == "__main__":
    main()
