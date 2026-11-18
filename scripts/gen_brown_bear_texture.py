"""Brown bear entity textures: adult (128x64) for BrownBearModel, cub (64x32) for BearCubModel, plus skinned versions.

Usage: python gen_brown_bear_texture.py
Box UVs below must match the models' createBodyLayer() (texOffs, w, h, d).
Face-local coords are as seen from outside the box, top row up; on the right (west, -x) side column 0 is the
back, on the left (east, +x) side column 0 is the front; on top/bottom row 0 is the back.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_skinned_textures import skin  # noqa: E402
from gen_wild_boar_texture import faces, front_frac, h, pick  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity"

# name: (u, v, w, h, d)
ADULT = {
    "chest": (0, 0, 12, 13, 12),
    "hump": (48, 0, 9, 3, 7),
    "hump2": (48, 10, 6, 1, 4),
    "ruff": (48, 15, 10, 6, 3),
    "rump": (0, 25, 11, 11, 13),
    "fringe": (48, 25, 0, 4, 10),
    "skull": (80, 0, 8, 7, 6),
    "brow": (108, 0, 6, 2, 1),
    "muzzle": (80, 13, 5, 4, 4),
    "jaw": (98, 13, 4, 2, 4),
    "nose": (114, 13, 3, 2, 1),
    "ear": (114, 16, 3, 3, 1),
    "tail": (114, 20, 3, 3, 2),
    "front_leg": (0, 49, 5, 8, 5),
    "front_paw": (20, 49, 5, 3, 6),
    "hind_leg": (42, 49, 6, 8, 7),
    "hind_paw": (68, 49, 5, 4, 6),
    "claws": (68, 59, 4, 1, 1),
}
CUB = {
    "body": (0, 0, 7, 6, 10),
    "skull": (34, 0, 6, 5, 4),
    "muzzle": (34, 9, 3, 2, 2),
    "nose": (44, 9, 2, 1, 1),
    "ear": (54, 0, 2, 2, 1),
    "tail": (54, 3, 2, 2, 1),
    "leg": (0, 16, 3, 4, 3),
}
TRANSPARENT_OK = {"fringe", "ear"}

# coat: deep chocolate to warm mid brown; grizzled tips straw/silver-brown; legs and paws near black-brown
C = [(40, 26, 17), (55, 36, 23), (71, 47, 30), (88, 59, 37), (106, 72, 45), (124, 86, 54), (143, 101, 64)]
TIP = [(150, 118, 82), (172, 140, 100), (194, 164, 124), (214, 190, 152)]
DARK = [(22, 15, 11), (31, 21, 15), (41, 28, 19), (52, 36, 24), (64, 45, 30)]
MUZ = [(78, 56, 38), (98, 72, 50), (118, 90, 64), (136, 106, 78)]
NOSE = [(20, 17, 17), (34, 29, 29), (52, 45, 44), (78, 70, 68)]
NOSTRIL = (8, 6, 6)
EYE, EYE_GLINT, EYE_RIM = (10, 7, 6), (92, 58, 30), (28, 18, 12)
PALATE = [(92, 40, 44), (122, 58, 60), (150, 82, 82)]
TOOTH = [(208, 198, 172), (232, 224, 204)]
PAD = [(26, 22, 21), (38, 33, 31), (50, 44, 41)]
CLAW = [(96, 86, 70), (140, 128, 106), (176, 164, 140), (204, 194, 170)]
INNER_EAR = [(46, 32, 24), (60, 43, 32)]
COLLAR = [(176, 158, 128), (204, 188, 158), (226, 214, 190)]


def fur(gx, gy, salt, tone=0.0, tips=0.1, pal=C, tip_pal=TIP, short=False):
    """Long shaggy coat: each column is a run of hanging hair strands 3-5 px long (2-3 for a cub's fluff). A strand
    darkens at the root (top), lightens towards its tip (bottom), sometimes ends in a grizzled tip, and casts a dark
    pixel on the strand below. Blocky low-frequency clumps break it into tufts."""
    base = 2 if short else 3
    off = int(h(gx, 1, salt) * 5)
    length = base + int(h(gx, 0, salt + 1) * 3)
    k, j = divmod(gy + off, length)
    strand = h(gx, k, salt)
    clump = h(gx // 3, (gy + off) // 4, salt + 7) - 0.5
    if j == length - 1 and h(gx, k, salt + 3) < tips * 1.5:
        return pick(tip_pal, h(gx, k, salt + 4) * 0.7 + max(0.0, tone))
    t = 0.32 + 0.32 * strand + 0.22 * clump + tone + 0.16 * j / (length - 1) + 0.06 * (h(gx, gy, salt + 2) - 0.5)
    if j == 0:
        t -= 0.14  # shadow under the strand above
    return pick(pal, t)


def paint(img, boxes, name, fn):
    """fn(face, x, y, fw, fh, gx, gy) -> rgb or None (transparent)."""
    u, v, w, hh, d = boxes[name]
    px = img.load()
    for face, (x0, y0, fw, fh) in faces(u, v, w, hh, d).items():
        for y in range(fh):
            for x in range(fw):
                c = fn(face, x, y, fw, fh, x0 + x, y0 + y)
                px[x0 + x, y0 + y] = (0, 0, 0, 0) if c is None else (*c, 255)


def face_pixels(boxes, name):
    u, v, w, hh, d = boxes[name]
    return [(fx + x, fy + y) for fx, fy, fw, fh in faces(u, v, w, hh, d).values() for y in range(fh) for x in range(fw)]


def check(img, boxes):
    """Every box face pixel is painted (fringes and ears may have holes), nothing outside the boxes, no UV overlaps."""
    used = set()
    for name in boxes:
        px = face_pixels(boxes, name)
        assert not (used & set(px)), f"{name} overlaps another box"
        used |= set(px)
        if name not in TRANSPARENT_OK:
            assert all(img.getpixel(p)[3] == 255 for p in px), f"{name} has holes"
    w, hh = img.size
    assert all(img.getpixel((x, y))[3] == 0 for x in range(w) for y in range(hh) if (x, y) not in used)


# --- adult -----------------------------------------------------------------------------------------------------

def chest(face, x, y, fw, fh, gx, gy):
    if face == "top":  # withers under the hump: grizzled, more so towards the front
        f = y / (fh - 1)
        return fur(gx, gy, 11, tone=0.08 + 0.08 * f, tips=0.12 + 0.18 * f)
    if face == "bottom":
        return fur(gx, gy, 12, tone=-0.35, tips=0.03, pal=DARK)
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        tone = 0.1 - 0.4 * (y / (fh - 1)) - 0.08 * f  # sunlit back, dark belly, shoulders darken into the forelegs
        return fur(gx, gy, 13, tone=tone, tips=0.14 * (1 - y / fh) + 0.04)
    if face == "front":  # breast below the head, dark
        return fur(gx, gy, 14, tone=-0.18 - 0.1 * (y > fh / 2), tips=0.05)
    return fur(gx, gy, 15, tone=-0.1)


def rump(face, x, y, fw, fh, gx, gy):
    if face == "top":
        return fur(gx, gy, 21, tone=0.05, tips=0.12)
    if face == "bottom":
        return fur(gx, gy, 22, tone=-0.35, tips=0.03, pal=DARK)
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        tone = 0.06 - 0.38 * (y / (fh - 1)) - 0.08 * (1 - f) * (y > fh / 2)  # dark haunch at the back
        return fur(gx, gy, 23, tone=tone, tips=0.12 * (1 - y / fh) + 0.03)
    if face == "back":
        return fur(gx, gy, 24, tone=-0.08 - 0.22 * (y / (fh - 1)), tips=0.06)
    return fur(gx, gy, 25, tone=-0.1)


def hump(face, x, y, fw, fh, gx, gy):
    """The grizzled hump: pale tips over a brown base, strongest on top."""
    if face == "top":
        return fur(gx, gy, 31, tone=0.18, tips=0.45)
    if face == "bottom":
        return fur(gx, gy, 32, tone=0.0)
    return fur(gx, gy, 33, tone=0.12, tips=0.3)


def hump2(face, x, y, fw, fh, gx, gy):
    return fur(gx, gy, 35, tone=0.25, tips=0.6)


def fringe_fn(salt):
    """Hanging belly fur: row 0 lies against the chest side; each column hangs down 1-3 more px to a ragged edge."""
    def fn(face, x, y, fw, fh, gx, gy):
        if face not in ("right", "left"):
            return C[1]  # zero-area faces
        hang = 1 + int(h(x, 0, salt) * 3)
        if y > hang:
            return None
        if y == hang and y > 0:
            return pick(DARK, 0.6 + 0.4 * h(x, y, salt + 1)) if h(x, 1, salt) < 0.7 else pick(TIP, 0.1)
        return fur(gx, gy, salt + 2, tone=-0.3 - 0.06 * y, tips=0.05)
    return fn


def ruff(face, x, y, fw, fh, gx, gy):
    """Fluffy cheeks and throat behind the skull."""
    if face == "front":
        return fur(gx, gy, 41, tone=0.02 - 0.12 * (y >= fh - 1), tips=0.22)
    if face == "bottom":
        return fur(gx, gy, 42, tone=-0.2, tips=0.06)
    if face in ("right", "left"):
        return fur(gx, gy, 43, tone=0.05 - 0.15 * (y / (fh - 1)), tips=0.24)
    return fur(gx, gy, 44, tone=0.0, tips=0.15)


def skull(face, x, y, fw, fh, gx, gy):
    if face == "top":  # broad forehead, short fur
        return fur(gx, gy, 51, tone=0.05, tips=0.1, short=True)
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        return fur(gx, gy, 52, tone=0.02 - 0.12 * (y >= fh - 2) - 0.05 * f, tips=0.12, short=True)
    if face == "front":  # mostly behind the brow and muzzle; edges show as cheeks
        return fur(gx, gy, 53, tone=-0.05, tips=0.08, short=True)
    if face == "bottom":
        return fur(gx, gy, 54, tone=-0.22, tips=0.04)
    return fur(gx, gy, 55, tone=-0.05)


def brow(face, x, y, fw, fh, gx, gy):
    """Forehead stop with the small dark eyes set at its foot, looking forward."""
    if face == "front":
        if y == 1 and x in (0, fw - 1):
            return EYE
        if y == 0 and x in (0, fw - 1):
            return EYE_RIM
        if y == 1 and x in (1, fw - 2):
            return EYE_GLINT  # brown inner corner of the small eye
        return fur(gx, gy, 62, tone=-0.08 - 0.06 * y, tips=0.06, short=True)
    if face in ("right", "left"):
        return EYE if y == 1 else EYE_RIM
    if face == "top":
        return fur(gx, gy, 63, tone=0.02, tips=0.08, short=True)
    return pick(MUZ, 0.3)


def muzzle(face, x, y, fw, fh, gx, gy):
    """Lighter, shorter-haired muzzle; nose leather behind the pad; dark lip line."""
    if face == "top":  # bridge, lighter towards the nose (last row = front)
        return pick(MUZ, 0.25 + 0.5 * y / (fh - 1) + 0.25 * (h(gx, gy, 71) - 0.5))
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        if y == fh - 1:
            return NOSE[1] if f > 0.5 else pick(MUZ, 0.05)  # lip line
        return pick(MUZ, 0.35 + 0.3 * f - 0.15 * (y / fh) + 0.2 * (h(gx, gy, 72) - 0.5))
    if face == "front":
        if y <= 1 and 1 <= x <= fw - 2:
            return NOSE[2] if y == 1 else NOSE[1]  # leather around the pad
        if y == fh - 1:
            return NOSE[0] if 1 <= x <= fw - 2 else NOSE[1]  # mouth
        return pick(MUZ, 0.55 + 0.2 * (h(gx, gy, 73) - 0.5))
    if face == "bottom":  # palate, seen with the jaw open; teeth along the front (last row)
        if y == fh - 1:
            return TOOTH[1] if x in (0, fw - 1) else TOOTH[0]
        return pick(PALATE, 0.3 + 0.5 * h(gx, gy, 74)) if y % 2 else PALATE[0]
    return pick(MUZ, 0.2)


def nose(face, x, y, fw, fh, gx, gy):
    if face == "front":
        if y == 1 and x in (0, fw - 1):
            return NOSTRIL
        return NOSE[3] if (y == 0 and x == 1) else NOSE[1]
    if face == "top":
        return NOSE[3] if x == 1 else NOSE[2]
    return NOSE[0]


def jaw(face, x, y, fw, fh, gx, gy):
    if face == "top":  # tongue and gums, lower teeth at the front
        if y == fh - 1:
            return TOOTH[1] if x in (0, fw - 1) else TOOTH[0]
        return PALATE[2] if 1 <= x <= fw - 2 else PALATE[1]
    if face == "front":
        return NOSE[1] if y == 0 else pick(MUZ, 0.25)
    if face == "bottom":
        return pick(MUZ, 0.1 + 0.3 * h(gx, gy, 81))
    if face in ("right", "left"):
        return NOSE[1] if y == 0 else pick(MUZ, 0.15 + 0.2 * h(gx, gy, 82))
    return pick(MUZ, 0.1)


def ear(face, x, y, fw, fh, gx, gy):
    """Small round ears: square corners cut away; dark inner ear framed by fur."""
    if face in ("front", "back"):
        if y == 0 and x in (0, fw - 1):
            return None
        if face == "front" and x == 1 and y >= 1:
            return INNER_EAR[0] if y == 1 else INNER_EAR[1]
        return fur(gx, gy, 91, tone=0.08, tips=0.25, short=True)
    if face in ("right", "left"):
        return None if y == 0 else fur(gx, gy, 92, tone=0.05, tips=0.2, short=True)
    if face == "top":
        return fur(gx, gy, 93, tone=0.1, tips=0.3) if x == 1 else None
    return C[2]


def tail(face, x, y, fw, fh, gx, gy):
    return fur(gx, gy, 101, tone=-0.15, tips=0.1, short=True)


def front_leg(face, x, y, fw, fh, gx, gy):
    """Body colour at the top (hidden in the chest) darkening to near black at the wrist."""
    if face == "top":
        return C[2]
    if face == "bottom":
        return DARK[1]
    t = y / (fh - 1)
    if y <= 2:
        return fur(gx, gy, 111, tone=-0.15 - 0.1 * y, tips=0.06)
    # back of the foreleg: longer hanging "feathering" with pale tips
    tips = 0.14 if face == "back" else 0.05
    return fur(gx, gy, 112, tone=0.15 - 0.4 * t, tips=tips, pal=DARK)


def front_paw(face, x, y, fw, fh, gx, gy):
    if face == "bottom":  # sole: row of toe pads at the front (last row), big palm pad behind
        if y == fh - 1:
            return PAD[0] if x % 2 else DARK[0]
        if 1 <= y <= fh - 3 and 1 <= x <= fw - 2:
            return PAD[1] if (x + y) % 3 else PAD[2]
        return DARK[1]
    if face == "front":  # toes: dark gaps between them
        return DARK[0] if x in (1, 3) and y >= 1 else fur(gx, gy, 121, tone=-0.1, tips=0.03, pal=DARK, short=True)
    if face == "top":
        return fur(gx, gy, 122, tone=-0.05, tips=0.03, pal=DARK, short=True)
    return fur(gx, gy, 123, tone=-0.08 - 0.1 * (y == fh - 1), tips=0.04, pal=DARK, short=True)


def claws(face, x, y, fw, fh, gx, gy):
    """Long pale horn claws: alternating shades so the four read separately."""
    if face == "top":
        return CLAW[3] if x % 2 == 0 else CLAW[2]
    if face == "bottom":
        return CLAW[0]
    if face == "front":
        return CLAW[2] if x % 2 == 0 else CLAW[1]
    return CLAW[1]


def hind_leg(face, x, y, fw, fh, gx, gy):
    """Heavy haunch: body coloured at the top, shading to the dark lower leg."""
    if face == "top":
        return C[2]
    if face == "bottom":
        return DARK[1]
    t = y / (fh - 1)
    if t < 0.45:
        return fur(gx, gy, 131, tone=0.0 - 0.35 * t, tips=0.1 * (1 - t))
    return fur(gx, gy, 132, tone=0.25 - 0.45 * t, tips=0.05, pal=DARK)


def hind_paw(face, x, y, fw, fh, gx, gy):
    if face == "bottom":  # long plantigrade sole
        if y == fh - 1:
            return PAD[0] if x % 2 else DARK[0]
        if 1 <= x <= fw - 2 and y >= 1:
            return PAD[1] if (x * 3 + y) % 4 else PAD[2]
        return DARK[1]
    if face == "front":
        if y == fh - 1:
            return CLAW[1] if x % 2 == 0 else DARK[0]  # short hind claws
        return DARK[0] if x in (1, 3) and y >= 2 else fur(gx, gy, 141, tone=-0.1, tips=0.03, pal=DARK, short=True)
    if face == "top":
        return fur(gx, gy, 142, tone=-0.05, tips=0.03, pal=DARK, short=True)
    return fur(gx, gy, 143, tone=-0.06 - 0.1 * (y == fh - 1), tips=0.04, pal=DARK, short=True)


def build_adult():
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    for name, fn in [("chest", chest), ("rump", rump), ("hump", hump), ("hump2", hump2), ("fringe", fringe_fn(151)),
                     ("ruff", ruff), ("skull", skull), ("brow", brow), ("muzzle", muzzle), ("jaw", jaw),
                     ("nose", nose), ("ear", ear), ("tail", tail), ("front_leg", front_leg),
                     ("front_paw", front_paw), ("claws", claws), ("hind_leg", hind_leg), ("hind_paw", hind_paw)]:
        paint(img, ADULT, name, fn)
    return img


# --- cub -------------------------------------------------------------------------------------------------------

CUB_C = [(36, 24, 16), (50, 33, 22), (64, 43, 28), (80, 54, 35), (97, 66, 42), (114, 79, 51)]


def cfur(gx, gy, salt, tone=0.0, tips=0.12):
    return fur(gx, gy, salt, tone=tone, tips=tips, pal=CUB_C, short=True)


def cub_body(face, x, y, fw, fh, gx, gy):
    """Fluffy, darker than the adult, with the pale neck ring many Eurasian cubs have (broken, at the shoulders)."""
    if face in ("right", "left"):
        f = front_frac(face, x, fw)
        if f > 0.82 and h(gx, gy, 201) < 0.75 and y < fh - 1:
            return pick(COLLAR, h(gx, gy, 202))
        return cfur(gx, gy, 203, tone=0.08 - 0.35 * y / (fh - 1), tips=0.14 * (1 - y / fh))
    if face == "top":
        if y >= fh - 2 and h(gx, gy, 204) < 0.6:  # collar crosses the nape (front rows)
            return pick(COLLAR, h(gx, gy, 205))
        return cfur(gx, gy, 206, tone=0.08, tips=0.15)
    if face == "front":  # chest: collar band at the top
        if y <= 1 and h(gx, gy, 207) < 0.8:
            return pick(COLLAR, h(gx, gy, 208))
        return cfur(gx, gy, 209, tone=-0.15)
    if face == "bottom":
        return cfur(gx, gy, 210, tone=-0.35, tips=0.0)
    return cfur(gx, gy, 211, tone=-0.1)


def cub_skull(face, x, y, fw, fh, gx, gy):
    if face == "front":
        if y == 1 and x in (1, fw - 2):
            return EYE
        if y == 1 and x in (0, fw - 1):
            return EYE_RIM
        return cfur(gx, gy, 221, tone=0.0)
    if face == "bottom":
        return cfur(gx, gy, 222, tone=-0.25, tips=0.0)
    if face in ("right", "left"):
        return cfur(gx, gy, 223, tone=0.05 - 0.15 * (y >= fh - 1), tips=0.15)
    return cfur(gx, gy, 224, tone=0.06, tips=0.14)


def cub_muzzle(face, x, y, fw, fh, gx, gy):
    if face == "front":
        return NOSE[0] if y == fh - 1 and x == 1 else pick(MUZ, 0.4 + 0.3 * h(gx, gy, 231))
    if face in ("right", "left") and y == fh - 1:
        return NOSE[1]
    return pick(MUZ, 0.3 + 0.4 * h(gx, gy, 232))


def cub_ear(face, x, y, fw, fh, gx, gy):
    if face == "front":
        return INNER_EAR[1] if y == 1 else cfur(gx, gy, 241, tone=0.1, tips=0.2)
    return cfur(gx, gy, 242, tone=0.08, tips=0.2)


def cub_leg(face, x, y, fw, fh, gx, gy):
    if face == "bottom":
        return PAD[1] if x == 1 else DARK[1]
    if face == "front" and y == fh - 1:
        return CLAW[1] if x != 1 else DARK[0]
    return pick(DARK, 0.25 + 0.5 * (1 - y / fh) * h(gx, gy, 251) + 0.2 * (y == 0))


def build_cub():
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    for name, fn in [("body", cub_body), ("skull", cub_skull), ("muzzle", cub_muzzle), ("nose", nose),
                     ("ear", cub_ear), ("tail", lambda *a: cfur(a[5], a[6], 261, tone=-0.12)), ("leg", cub_leg)]:
        paint(img, CUB, name, fn)
    return img


def save_with_skinned(img, boxes, name, keep):
    check(img, boxes)
    out = ROOT / f"wildlife/{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    flesh = skin(img.copy())
    px, src = flesh.load(), img.load()
    for part in keep:  # claws stay horn
        for p in face_pixels(boxes, part):
            px[p] = src[p]
    assert flesh.size == img.size and flesh.getchannel("A").tobytes() == img.getchannel("A").tobytes()
    out_skinned = ROOT / f"skinned/wildlife/{name}.png"
    out_skinned.parent.mkdir(parents=True, exist_ok=True)
    flesh.save(out_skinned)
    print("wrote", out, "and", out_skinned)


def main():
    save_with_skinned(build_adult(), ADULT, "brown_bear", ["claws"])
    save_with_skinned(build_cub(), CUB, "brown_bear_cub", [])


if __name__ == "__main__":
    main()
