"""Paint the red deer textures (stag + hind) for RedDeerModel, and their skinned (flesh) versions.

Usage: python scripts/gen_red_deer_texture.py
Deterministic. The BOXES table must match the cubes in RedDeerModel.createBodyLayer (texOffs, origin, size).
Every texel is painted from the 3D point it covers on its cube, so features (eyes, rump patch, hooves) line up
across faces.
"""
import importlib.util
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"
W, H = 128, 64

# --- palette ---------------------------------------------------------------
COAT = (148, 80, 45)
COAT_DARK = (112, 57, 33)
COAT_LIGHT = (176, 106, 62)
GREY = (112, 90, 72)          # greyer neck / legs
GREY_DARK = (82, 64, 52)
BELLY = (92, 70, 55)
MANE = (66, 46, 34)
MANE_LIGHT = (100, 72, 50)
BUFF = (218, 190, 146)
BUFF_EDGE = (176, 140, 98)
FACE = (128, 92, 66)
FACE_LIGHT = (156, 124, 98)
MUZZLE = (150, 134, 118)
NOSE = (38, 30, 28)
CHIN = (196, 180, 160)
EYE = (18, 13, 12)
GLINT = (196, 202, 210)
GLAND = (58, 40, 30)
INNER_EAR = (210, 180, 160)
EAR_RIM = (70, 54, 44)
HOOF = (44, 37, 33)
ANTLER_BASE = (86, 64, 42)
ANTLER_MID = (166, 136, 96)
ANTLER_TIP = (238, 228, 204)


def h(x, y, salt):
    n = (int(x) * 374761393 + int(y) * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def fur(c, i, j, salt, amt=0.07, streak=True):
    """Per-texel fur speckle plus faint vertical hair streaks (in texture space, so it reads as hair on every face)."""
    n = (h(i, j, salt) - 0.5) * 2 * amt
    if streak:
        n += (h(i, j // 2, salt + 7) - 0.5) * amt
    return shade(c, 1 + n)


# --- materials: (variant, face, x, y, z, i, j) -> rgb or rgba ----------------
# Coordinates are the cube's local model coordinates (pixels; y down, -z = front). i, j = texel position.

def coat_side(y, top, bottom):
    """Back -> flank -> belly gradient for a body side; y in [top, bottom]."""
    t = (y - top) / (bottom - top)
    if t < 0.15:
        return lerp(COAT_DARK, COAT, t / 0.15)      # slightly darker dorsal line
    if t < 0.55:
        return lerp(COAT, COAT_LIGHT, (t - 0.15) / 0.4 * 0.6)
    if t < 0.85:
        return lerp(lerp(COAT, COAT_LIGHT, 0.6), COAT_DARK, (t - 0.55) / 0.3)
    return lerp(COAT_DARK, BELLY, (t - 0.85) / 0.15)


def rump(x, y):
    """0..1 buff amount for the rump patch on the back of the body (oval around/below the tail), plus edge."""
    d = (x / 3.3) ** 2 + ((y - 0.2) / 3.6) ** 2
    return d


def body_mat(v, face, x, y, z, i, j):
    if face == "bottom":
        return fur(BELLY, i, j, 11, 0.06)
    if face == "back":
        d = rump(x, y)
        if d < 0.8:
            return fur(lerp(BUFF, BUFF_EDGE, max(0, d - 0.55) * 2), i, j, 12, 0.04)
        if d < 1.05:
            return fur(BUFF_EDGE if d < 0.92 else COAT_DARK, i, j, 12, 0.05)
        return fur(coat_side(y, -4, 4), i, j, 12)
    if face == "top":
        c = lerp(COAT_DARK, COAT, abs(x) / 4 + 0.15)
        if z > 7.5 and abs(x) < 2.5:            # patch creeps onto the croup around the tail root
            c = lerp(c, BUFF_EDGE, 0.5 if z > 8 else 0.25)
        if z < -6:                               # greyer towards the neck
            c = lerp(c, GREY, (-6 - z) / 4)
        return fur(c, i, j, 13)
    # sides
    c = coat_side(y, -4, 4)
    if abs(z - 5.5) < 3.5 and y < 1:            # rounded haunch catches the light
        c = lerp(c, COAT_LIGHT, 0.25 * (1 - abs(z - 5.5) / 3.5))
    if -3.5 < z < -2 and y > -2:                 # crease behind the shoulder / elbow
        c = shade(c, 0.9)
    if z > 7 and y < 1.5:                        # rump patch wraps the hip edge a little
        c = lerp(c, BUFF_EDGE, 0.45 if z > 8 else 0.2)
    if z < -6:
        c = lerp(c, GREY, (-6 - z) / 5)
    return fur(c, i, j, 14)


def chest_mat(v, face, x, y, z, i, j):
    stag = v == "stag"
    neckc = MANE if stag else GREY_DARK
    if face == "bottom":
        return fur(BELLY, i, j, 21, 0.06)
    if face == "front":
        t = (y + 4.5) / 10
        c = lerp(neckc, BELLY, t * 0.6)
        if stag and h(i, j // 3, 22) < 0.35:
            c = shade(c, 0.85)
        return fur(c, i, j, 23, 0.09 if stag else 0.06)
    if face == "top":
        c = lerp(GREY, COAT, (z + 10) / 7)
        if stag:
            c = lerp(c, MANE_LIGHT, max(0, (-6 - z) / 4))
        return fur(c, i, j, 24)
    if face == "back":
        return fur(coat_side(y, -4.5, 5.5), i, j, 25)
    # sides: grey shoulder blending into the red flank, darker lower chest
    c = coat_side(y, -4.5, 5.5)
    c = lerp(c, GREY, max(0, (-4 - z) / 6) * 0.9)
    if stag:
        c = lerp(c, MANE, max(0, (-7.5 - z) / 2.5) * 0.7)
    return fur(c, i, j, 26)


def neck_mat(v, face, x, y, z, i, j):
    stag = v == "stag"
    if stag:
        base = lerp(MANE_LIGHT, MANE, (y + 9) / 11 * 0.6 + 0.2)
        if face == "front":
            base = shade(base, 0.9)
        c = base if h(i, j // 3, 31) > 0.3 else shade(base, 0.82)   # coarse shaggy strands
        return fur(c, i, j, 32, 0.1)
    c = lerp(GREY, COAT, 0.25)
    if face == "front":                           # paler throat
        c = lerp(c, FACE_LIGHT, 0.35)
    if face == "back":                            # darker nape line
        c = lerp(c, GREY_DARK, 0.4)
    return fur(c, i, j, 33)


def mane_mat(v, face, x, y, z, i, j):
    # Shaggy fringe: ragged transparent bottom edge on the sides (the neck shows through).
    if face in ("left", "right", "front", "back") and y > 0.0:
        cut = 0.5 + (y - 0.0) * 0.35
        if h(i, 0, 41) < cut - 0.35 or (y > 1.0 and h(i, 1, 42) < 0.55):
            return (0, 0, 0, 0)
    if face == "top":
        return (0, 0, 0, 0)                       # inside the neck anyway
    base = MANE if face != "front" else shade(MANE, 0.85)
    if h(i, j // 2, 43) < 0.3:
        base = MANE_LIGHT
    elif h(i, j // 3, 44) < 0.25:
        base = shade(MANE, 0.7)
    if v != "stag":
        base = GREY
    return fur(base, i, j, 45, 0.1)


def head_mat(v, face, x, y, z, i, j):
    c = FACE
    if face == "top":
        c = lerp(GREY_DARK, FACE, 0.5)
        if v == "stag" and abs(abs(x) - 1.5) < 0.6 and abs(z - 0.5) < 0.6:
            return fur(ANTLER_BASE, i, j, 50, 0.05)   # pedicles under the antlers
        return fur(c, i, j, 51)
    if face == "bottom":
        return fur(lerp(FACE_LIGHT, CHIN, 0.4), i, j, 52, 0.05)
    if face == "front":
        c = lerp(GREY_DARK, FACE, 0.6) if y < -1 else FACE   # forehead
        return fur(c, i, j, 53, 0.05)
    if face in ("left", "right"):
        # eye: dark 2x1 with a glint, pale ring, preorbital gland in front of it
        if abs(y + 1.5) < 0.6 and abs(z + 2.5) < 0.6:
            return EYE
        if abs(y + 1.5) < 0.6 and abs(z + 1.5) < 0.6:
            return GLINT if face == "right" else EYE
        if abs(y + 1.5) < 0.6 and abs(z + 0.5) < 0.6:
            return GLINT if face == "left" else FACE_LIGHT
        if abs(y + 0.5) < 0.6 and abs(z + 3.5) < 0.6:
            return GLAND
        if abs(y + 1.5) < 1.6 and -4 < z < 0.5:
            c = lerp(FACE, FACE_LIGHT, 0.45)        # pale eye ring area
        if y > 0.5:
            c = lerp(c, FACE_LIGHT, 0.5)            # lighter cheek / jaw
        return fur(c, i, j, 54, 0.05)
    return fur(lerp(FACE, GREY, 0.3), i, j, 55)


def snout_mat(v, face, x, y, z, i, j):
    c = lerp(FACE, MUZZLE, (-4 - z) / 4 * 0.7)
    if face == "front":
        if y < 0.5:
            return fur(NOSE, i, j, 60, 0.08)        # rhinarium
        if y < 1.0:
            return MUZZLE if abs(x) > 0.6 else shade(NOSE, 1.3)   # upper lip, philtrum
        return fur(CHIN, i, j, 61, 0.04)
    if face == "top":
        if z < -7:
            return fur(NOSE, i, j, 62, 0.08)
        if z < -6:
            return fur(MUZZLE, i, j, 63, 0.04)        # pale band behind the nose
        return fur(lerp(GREY_DARK, FACE, 0.7), i, j, 64)
    if face == "bottom":
        return fur(CHIN, i, j, 65, 0.04)
    if face in ("left", "right"):
        if z < -7 and y < 0.5:
            return NOSE                                # nostril wrap
        if y > 1.0:
            return fur(CHIN if z < -6 else FACE_LIGHT, i, j, 66, 0.04)   # pale lips / chin
        if y > 0.0 and z < -7:
            return shade(NOSE, 1.6)                    # mouth corner
    return fur(c, i, j, 67, 0.05)


def ear_mat(v, face, x, y, z, i, j):
    if face == "front":                         # inner ear: pale with a dark rim
        rim = abs(x) > 0.6 and y < -0.6 or y < -3.4
        return EAR_RIM if rim else fur(INNER_EAR, i, j, 70, 0.05)
    if y < -3.0:
        return EAR_RIM                          # dark tip
    return fur(lerp(GREY, FACE, 0.5), i, j, 71)


def tail_mat(v, face, x, y, z, i, j):
    if face in ("front", "bottom"):
        return fur(BUFF, i, j, 80, 0.04)
    c = lerp(COAT, BUFF_EDGE, y / 3)
    if face == "top":
        c = COAT_DARK
    return fur(c, i, j, 81)


def leg_upper_mat(top, bottom, hind):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if hind:
            c = lerp(COAT, GREY, t * 1.2)       # red ham fading into grey shank
            if face == "back" and t < 0.5:
                c = lerp(BUFF_EDGE, c, t * 2)   # rump patch runs down the back of the thigh
            if face == "front":
                c = shade(c, 0.92)
        else:
            c = lerp(lerp(GREY, COAT, 0.4), GREY_DARK, t)
            if face == "front":
                c = shade(c, 0.9)
            if face == "back":
                c = lerp(c, FACE_LIGHT, 0.15)
        return fur(c, i, j, 90 + int(hind))
    return mat


def leg_lower_mat(length, hind):
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return HOOF if abs(x) > 0.1 else shade(HOOF, 0.7)
        if face == "top":
            return GREY_DARK
        if y > length - 1:                      # hoof, split into two claws on the front
            return shade(HOOF, 1.0 + 0.15 * (h(i, j, 95) - 0.5))
        if y > length - 1.5 - 0.5:              # pale coronet band right above the hoof
            return lerp(GREY_DARK, GREY, 0.6)
        if face == "back" and abs(y - (length - 2.5)) < 0.6:
            return HOOF                          # dewclaws
        if hind and face in ("left", "right") and y < 1:
            return GLAND                         # metatarsal tuft
        return fur(lerp(GREY_DARK, GREY, 0.3), i, j, 96)
    return mat


def antler_mat(y_base_t, y_tip_t, length):
    """Gradient along the antler: t=0 at the base (local y=0) .. 1 at the tip (y=-length), mapped to [y_base_t, y_tip_t]."""
    def mat(v, face, x, y, z, i, j):
        if face == "top":
            t = y_tip_t
        elif face == "bottom":
            t = y_base_t
        else:
            t = y_base_t + (y_tip_t - y_base_t) * (-y / length)
        c = lerp(ANTLER_BASE, ANTLER_MID, t / 0.6) if t < 0.6 else lerp(ANTLER_MID, ANTLER_TIP, (t - 0.6) / 0.4)
        if t < 0.75 and h(i, j, 99) < 0.3:
            c = shade(c, 0.85)                  # pearling / grooves on the beam
        if y_base_t == 0 and y > -1:
            c = shade(ANTLER_BASE, 0.85)        # knobbly burr at the skull
        return c
    return mat


# name: (u, v, x0, y0, z0, w, h, d, material)  -- mirrors RedDeerModel.createBodyLayer
BOXES = {
    "body": (0, 0, -4, -4, -9, 8, 8, 18, body_mat),
    "chest": (52, 0, -4.5, -4.5, -10, 9, 10, 7, chest_mat),
    "neck": (84, 0, -2, -9, -2.5, 4, 11, 5, neck_mat),
    "head": (102, 0, -2.5, -3, -4, 5, 5, 6, head_mat),
    "mane": (0, 26, -3, -6, -3.5, 6, 8, 6, mane_mat),
    "snout": (102, 11, -1.5, -1, -8, 3, 3, 4, snout_mat),
    "ear": (116, 11, -1, -4, -0.5, 2, 4, 1, ear_mat),
    "tail": (122, 11, -1, 0, 0, 2, 3, 1, tail_mat),
    "front_upper": (24, 26, -1.5, -1, -1.5, 3, 7, 3, leg_upper_mat(-1, 6, False)),
    "front_lower": (36, 26, -1, 0, -1, 2, 5, 2, leg_lower_mat(5, False)),
    "hind_upper": (44, 26, -1.5, -1, -2, 3, 8, 4, leg_upper_mat(-1, 7, True)),
    "hind_lower": (58, 26, -1, 0, -1, 2, 4, 2, leg_lower_mat(4, True)),
    "antler_low": (66, 26, -0.5, -6, -0.5, 1, 6, 1, antler_mat(0.0, 0.45, 6)),
    "antler_high": (70, 26, -0.5, -6, -0.5, 1, 6, 1, antler_mat(0.4, 0.8, 6)),
    "tine": (74, 26, -0.5, -4, -0.5, 1, 4, 1, antler_mat(0.35, 1.0, 4)),
}


def face_texels(u, v, x0, y0, z0, w, h, d):
    """Yield (face, tx, ty, x, y, z) for every texel of a box-UV cube (vanilla ModelPart.Cube layout)."""
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    for i in range(w):
        for j in range(d):
            yield "top", u + d + i, v + j, x0 + i + .5, y0, z1 - j - .5
            yield "bottom", u + d + w + i, v + j, x0 + i + .5, y1, z1 - j - .5
    for j in range(h):
        y = y0 + j + .5
        for i in range(d):
            yield "right", u + i, v + d + j, x0, y, z1 - i - .5          # -x side = entity's right
            yield "left", u + d + w + i, v + d + j, x1, y, z0 + i + .5   # +x side = entity's left
        for i in range(w):
            yield "front", u + d + i, v + d + j, x0 + i + .5, y, z0
            yield "back", u + 2 * d + w + i, v + d + j, x1 - i - .5, y, z1


def paint(variant):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    owner = {}
    for name, (u, v, x0, y0, z0, w, hh, d, mat) in BOXES.items():
        for face, tx, ty, x, y, z in face_texels(u, v, x0, y0, z0, w, hh, d):
            assert 0 <= tx < W and 0 <= ty < H, (name, tx, ty)
            assert owner.setdefault((tx, ty), name) == name, f"UV overlap {name} / {owner[(tx, ty)]} at {tx},{ty}"
            c = mat(variant, face, x, y, z, tx, ty)
            px[tx, ty] = c if len(c) == 4 else (*c, 255)
    return img


def load_skinner():
    spec = importlib.util.spec_from_file_location("gen_skinned_textures", ROOT / "scripts/gen_skinned_textures.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def skinned(img, skinner):
    out = skinner.skin(img.copy())
    # Antlers stay bone: copy them back from the furred texture.
    src, dst = img.load(), out.load()
    for name in ("antler_low", "antler_high", "tine"):
        u, v, *_rest, w, hh, d, _m = BOXES[name]
        for tx in range(u, u + 2 * d + 2 * w):
            for ty in range(v, v + d + hh):
                dst[tx, ty] = src[tx, ty]
    return out


def main():
    skinner = load_skinner()
    for variant in ("stag", "hind"):
        img = paint(variant)
        out = TEX / "wildlife" / f"red_deer_{variant}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out)
        sk = skinned(img, skinner)
        assert sk.size == img.size and sk.getchannel("A").tobytes() == img.getchannel("A").tobytes()
        sk_out = TEX / "skinned" / "wildlife" / f"red_deer_{variant}.png"
        sk_out.parent.mkdir(parents=True, exist_ok=True)
        sk.save(sk_out)
        print("wrote", out.relative_to(ROOT), "and", sk_out.relative_to(ROOT))


if __name__ == "__main__":
    main()
