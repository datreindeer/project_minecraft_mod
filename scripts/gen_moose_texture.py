"""Paint the moose textures (bull, cow for MooseModel; calf for MooseCalfModel) and the skinned (flesh) adult versions.

Usage: python scripts/gen_moose_texture.py
Deterministic. BOXES / CALF_BOXES must match the cubes in MooseModel / MooseCalfModel (texOffs, origin, size).
Every texel is painted from the 3D point it covers on its cube (same approach and helpers as gen_red_deer_texture.py).
"""
import importlib.util
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"


def _load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


rd = _load("gen_red_deer_texture")
h, lerp, shade, fur, face_texels = rd.h, rd.lerp, rd.shade, rd.fur, rd.face_texels

# --- palettes ----------------------------------------------------------------
SHARED = dict(EYE=(14, 10, 9), GLINT=(190, 196, 204), HOOF=(30, 26, 24), NOSTRIL=(16, 12, 11),
              EAR_IN=(150, 136, 124), ANTLER_BASE=(92, 74, 54), ANTLER_MID=(182, 162, 126), ANTLER_TIP=(234, 224, 198))
# bulls are near black-brown with a paler greyish saddle; cows browner with a paler face
BULL = dict(SHARED, COAT=(58, 42, 33), COAT_DARK=(34, 25, 20), COAT_LIGHT=(88, 66, 50), SADDLE=(104, 86, 68),
            BELLY=(28, 21, 17), RIDGE=(22, 17, 14), FACE=(62, 46, 35), MUZZLE=(92, 78, 66), LEG=(184, 176, 164),
            LEG_SHADE=(140, 132, 122))
COW = dict(SHARED, COAT=(82, 60, 44), COAT_DARK=(52, 38, 29), COAT_LIGHT=(114, 88, 66), SADDLE=(128, 106, 82),
           BELLY=(40, 30, 24), RIDGE=(34, 25, 20), FACE=(112, 84, 62), MUZZLE=(124, 104, 88), LEG=(192, 184, 172),
           LEG_SHADE=(148, 140, 130))
# calves are a plain warm red-brown, unspotted, with brown (not yet pale) legs
CALF = dict(SHARED, COAT=(148, 92, 56), COAT_DARK=(108, 64, 40), COAT_LIGHT=(176, 116, 74), SADDLE=(170, 116, 78),
            BELLY=(116, 76, 50), RIDGE=(92, 56, 36), FACE=(126, 80, 52), MUZZLE=(70, 48, 36), LEG=(150, 108, 78),
            LEG_SHADE=(120, 84, 60))


def side_grad(p, y, top, bottom):
    """Back -> flank -> belly gradient for a body side; y in [top, bottom]."""
    t = (y - top) / (bottom - top)
    if t < 0.15:
        return lerp(p["SADDLE"], p["COAT"], t / 0.15)
    if t < 0.5:
        return lerp(p["COAT"], p["COAT_LIGHT"], (t - 0.15) / 0.35 * 0.4)
    if t < 0.85:
        return lerp(lerp(p["COAT"], p["COAT_LIGHT"], 0.4), p["COAT_DARK"], (t - 0.5) / 0.35)
    return lerp(p["COAT_DARK"], p["BELLY"], (t - 0.85) / 0.15)


def shaggy(c, i, j, salt, k=0.8):
    """Coarse long-hair strands: every few texel columns a darker run."""
    return c if h(i, j // 3, salt) > 0.3 else shade(c, k)


# --- materials: (palette, face, x, y, z, i, j) -> rgb or rgba ------------------

def body_mat(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["BELLY"], i, j, 11, 0.06)
    if face == "back":
        c = side_grad(p, y, -5, 6)
        if abs(x) < 2 and y < 0:
            c = lerp(c, p["COAT_DARK"], 0.4)     # darker around the tail root
        return fur(c, i, j, 12)
    if face == "top":
        c = lerp(p["SADDLE"], p["COAT"], abs(x) / 5)
        if abs(x) < 0.6:
            c = lerp(c, p["RIDGE"], 0.35)         # dark line down the spine
        return fur(c, i, j, 13)
    c = side_grad(p, y, -5, 6)
    if abs(z - 6) < 4 and y < 2:                  # haunch catches the light
        c = lerp(c, p["COAT_LIGHT"], 0.2 * (1 - abs(z - 6) / 4))
    if -4 < z < -2 and y > -1:                    # crease behind the shoulder
        c = shade(c, 0.88)
    return fur(c, i, j, 14)


def chest_mat(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["BELLY"], i, j, 21, 0.06)
    if face == "front":
        c = lerp(p["COAT_DARK"], p["BELLY"], (y + 5.5) / 13)
        return fur(shaggy(c, i, j, 22, 0.85), i, j, 23, 0.08)
    if face == "top":
        return fur(lerp(p["RIDGE"], p["COAT_DARK"], 0.5), i, j, 24)
    if face == "back":
        return fur(side_grad(p, y, -5.5, 7.5), i, j, 25)
    c = side_grad(p, y, -5.5, 7.5)
    c = lerp(c, p["COAT_DARK"], max(0, (-8 - z) / 4) * 0.6)    # darker at the point of the shoulder
    return fur(shaggy(c, i, j, 26, 0.9), i, j, 27)


def hump_mat(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["COAT_DARK"], i, j, 31)
    if face == "top":
        c = p["RIDGE"] if abs(x) < 1.2 else lerp(p["SADDLE"], p["COAT"], (abs(x) - 1.2) / 2.8)
        return fur(shaggy(c, i, j, 32), i, j, 33, 0.09)
    c = lerp(p["SADDLE"], p["COAT"], (y + 4) / 4)
    if face == "front":
        c = lerp(c, p["RIDGE"], 0.5)
    return fur(shaggy(c, i, j, 34, 0.85), i, j, 35, 0.09)


def neck_mat(p, face, x, y, z, i, j):
    # once tipped forward: +z ("back") is the crest, -z ("front") the throat
    if face == "back":
        return fur(shaggy(p["RIDGE"], i, j, 41, 0.8), i, j, 42, 0.1)
    if face == "front":
        return fur(shaggy(lerp(p["COAT_DARK"], p["BELLY"], 0.4), i, j, 43), i, j, 44, 0.08)
    if face in ("top", "bottom"):
        return fur(p["COAT_DARK"], i, j, 45)
    c = lerp(p["COAT"], p["COAT_DARK"], (z + 3.5) / 7 * 0.3)
    c = lerp(c, p["RIDGE"], max(0, (z - 2) / 1.5) * 0.6)     # crest hair spills over the sides
    return fur(shaggy(c, i, j, 46, 0.85), i, j, 47, 0.09)


def ridge_mat(p, face, x, y, z, i, j):
    c = p["RIDGE"] if h(i, j // 2, 51) > 0.35 else lerp(p["RIDGE"], p["COAT"], 0.5)
    return fur(c, i, j, 52, 0.12)


def head_mat(p, face, x, y, z, i, j):
    c = p["FACE"]
    if face == "top":
        return fur(lerp(p["FACE"], p["COAT_DARK"], 0.35), i, j, 61)
    if face == "bottom":
        return fur(p["COAT_DARK"], i, j, 62, 0.05)
    if face == "front":
        return fur(lerp(p["FACE"], p["COAT_DARK"], 0.25 if y < -1 else 0.0), i, j, 63, 0.05)
    if face == "back":
        return fur(lerp(p["FACE"], p["COAT"], 0.5), i, j, 64)
    # small eyes set high, towards the front of the cranium, a darker patch around them
    if abs(y + 0.5) < 0.6 and abs(z + 4.5) < 0.6:
        return p["EYE"]
    if abs(y + 0.5) < 0.6 and abs(z + 3.5) < 0.6:
        return p["GLINT"] if face == "right" else p["EYE"]
    if abs(y + 0.5) < 1.6 and -5 < z < -2:
        c = shade(c, 0.85)
    if y > 1:
        c = lerp(c, p["COAT_DARK"], 0.35)          # jowl
    return fur(c, i, j, 65, 0.05)


def snout_mat(p, face, x, y, z, i, j):
    t = (-5 - z) / 6                                # 0 at the cranium .. 1 at the nose
    c = lerp(p["FACE"], p["MUZZLE"], t)
    if face == "top":
        if t < 0.4:
            c = lerp(c, p["COAT_DARK"], 0.25)       # darker bridge between the eyes
        return fur(c, i, j, 71, 0.05)
    if face == "bottom":
        return fur(lerp(p["COAT_DARK"], p["MUZZLE"], 0.3), i, j, 72, 0.05)
    if face in ("left", "right"):
        if y > 1.5 and t > 0.5:
            return fur(shade(p["MUZZLE"], 0.7), i, j, 73, 0.05)   # lower lip under the overhang
        return fur(c, i, j, 74, 0.05)
    return fur(c, i, j, 75, 0.05)


def nose_mat(p, face, x, y, z, i, j):
    m = p["MUZZLE"]
    if face == "front":
        if abs(abs(x) - 1.5) < 0.6 and 0 < y < 2:
            return p["NOSTRIL"]                     # long nostril slits
        if y > 2.5:
            return fur(shade(m, 0.6), i, j, 81, 0.05)   # drooping upper lip
        return fur(shade(m, 0.85), i, j, 82, 0.05)
    if face in ("left", "right"):
        if z < -13 and 0 < y < 2:
            return shade(p["NOSTRIL"], 1.5)          # nostrils wrap round
        if y > 2.5:
            return fur(shade(m, 0.6), i, j, 83, 0.05)
        return fur(lerp(m, p["FACE"], 0.2), i, j, 84, 0.05)
    if face == "bottom":
        return fur(shade(m, 0.55), i, j, 85, 0.05)
    if face == "back":
        return p["NOSTRIL"] if y > 2.5 else fur(shade(m, 0.7), i, j, 86)   # mouth shadow under the overhang
    return fur(m, i, j, 87, 0.05)


def dewlap_mat(p, face, x, y, z, i, j):
    # the bell: a hairy flap tapering to a tassel (the side faces carry the outline; front/back edges match it)
    if face in ("left", "right") and abs(z) > 1.5 - max(0.0, y - 3.0) * 0.45:
        return (0, 0, 0, 0)
    if face in ("front", "back") and y > 3.5:
        return (0, 0, 0, 0)
    c = lerp(p["COAT_DARK"], p["RIDGE"], y / 6)
    return fur(shaggy(c, i, j, 92, 0.8), i, j, 93, 0.1)


def ear_mat(p, face, x, y, z, i, j):
    if face == "front":
        rim = abs(x) > 0.6 or y < -4.4
        return lerp(p["FACE"], p["COAT_DARK"], 0.5) if rim else fur(p["EAR_IN"], i, j, 101, 0.06)
    if y < -4:
        return p["COAT_DARK"]
    return fur(lerp(p["FACE"], p["COAT"], 0.5), i, j, 102)


def tail_mat(p, face, x, y, z, i, j):
    return fur(p["COAT_DARK"] if face != "front" else p["BELLY"], i, j, 111)


def leg_upper_mat(hind):
    def mat(p, face, x, y, z, i, j):
        t = (y + 1) / 9
        start = 0.35 if hind else 0.5               # the pale stockings reach higher up the hind legs
        if face == "back" and hind:
            start -= 0.15
        if t < start:
            c = p["COAT_DARK"] if hind else lerp(p["COAT"], p["COAT_DARK"], t * 2)
        else:
            c = lerp(p["LEG_SHADE"], p["LEG"], (t - start) / (1 - start))
            if t < start + 0.15:
                c = lerp(p["COAT_DARK"], c, (t - start) / 0.15)
        if face == "front":
            c = shade(c, 0.92)
        return fur(c, i, j, 120 + int(hind))
    return mat


def leg_lower_mat(length):
    def mat(p, face, x, y, z, i, j):
        if face == "bottom":
            return p["HOOF"] if abs(x) > 0.1 else shade(p["HOOF"], 0.7)
        if face == "top":
            return p["LEG_SHADE"]
        if y > length - 1.2:
            if face == "front" and abs(x) < 0.1:
                return shade(p["HOOF"], 0.6)        # cleft between the two claws
            return shade(p["HOOF"], 1.0 + 0.15 * (h(i, j, 131) - 0.5))
        if face == "back" and abs(y - (length - 2.5)) < 0.6:
            return p["HOOF"]                         # dewclaws
        c = p["LEG"] if face != "front" else lerp(p["LEG"], p["LEG_SHADE"], 0.4)
        if y > length - 2.2:
            c = shade(c, 0.85)
        return fur(c, i, j, 132, 0.05)
    return mat


def antler_col(p, t, i, j):
    c = lerp(p["ANTLER_BASE"], p["ANTLER_MID"], t / 0.55) if t < 0.55 else lerp(p["ANTLER_MID"], p["ANTLER_TIP"], (t - 0.55) / 0.45)
    if t < 0.8 and h(i, j, 141) < 0.25:
        c = shade(c, 0.86)
    return c


def beam_mat(p, face, x, y, z, i, j):
    if y > -1.2 or face == "bottom":
        return shade(p["ANTLER_BASE"], 0.85)           # burr at the skull
    return antler_col(p, 0.1 + (-y / 4) * 0.3, i, j)


def palm_mat(length, width, z0):
    """Broad palmate plate: grooved faces darker at the base, a pale worn rim along the edges and far end."""
    def mat(p, face, x, y, z, i, j):
        t = -y / length
        if face in ("front", "back", "top"):
            return antler_col(p, 0.75 + 0.25 * t, i, j)
        if face == "bottom":
            return antler_col(p, 0.35, i, j)
        rim = min(z - z0, z0 + width - z, length + y)
        c = antler_col(p, 0.35 + 0.35 * t + (0.25 if rim < 1 else 0), i, j)
        if h(i // 1, j // 3, 142) < 0.2:
            c = shade(c, 0.88)                           # veins running along the palm
        return c
    return mat


def tine_mat(length):
    def mat(p, face, x, y, z, i, j):
        t = 1.0 if face == "top" else 0.6 if face == "bottom" else 0.6 + 0.4 * (-y / length)
        return antler_col(p, t, i, j)
    return mat


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors MooseModel.createBodyLayer
BOXES = {
    "body": (0, 0, -5, -5, -11, 10, 11, 22, body_mat),
    "chest": (64, 0, -5.5, -5.5, -12, 11, 13, 9, chest_mat),
    "hump": (64, 22, -4, -4, -5, 8, 4, 10, hump_mat),
    "neck": (0, 33, -3, -8, -3.5, 6, 9, 7, neck_mat),
    "ridge": (112, 36, -1, -8.5, 3.5, 2, 9, 1, ridge_mat),
    "head": (104, 0, -3, -3, -5, 6, 6, 6, head_mat),
    "snout": (104, 12, -2.5, -2.5, -11, 5, 5, 6, snout_mat),
    "nose": (100, 23, -3, -2.5, -14, 6, 6, 3, nose_mat),
    "dewlap": (118, 23, -0.5, 0, -1.5, 1, 6, 3, dewlap_mat),
    "ear": (100, 32, -1, -5, -0.5, 2, 5, 1, ear_mat),
    "tail": (106, 32, -1, 0, 0, 2, 4, 1, tail_mat),
    "front_upper": (26, 33, -1.5, -1, -2, 3, 9, 4, leg_upper_mat(False)),
    "front_lower": (56, 33, -1, 0, -1, 2, 8, 2, leg_lower_mat(8)),
    "hind_upper": (40, 33, -1.5, -1, -2.5, 3, 9, 5, leg_upper_mat(True)),
    "hind_lower": (56, 43, -1, 0, -1, 2, 8, 2, leg_lower_mat(8)),
    "beam": (64, 36, -1, -4, -1, 2, 4, 2, beam_mat),
    "palm": (72, 36, -0.5, -8, -3, 1, 8, 7, palm_mat(8, 7, -3)),
    "brow_palm": (88, 36, -0.5, -4, -5, 1, 4, 5, palm_mat(4, 5, -5)),
    "tine": (100, 38, -0.5, -2, -0.5, 1, 2, 1, tine_mat(2)),
    "long_tine": (104, 38, -0.5, -3, -0.5, 1, 3, 1, tine_mat(3)),
}
ANTLER_BOXES = ("beam", "palm", "brow_palm", "tine", "long_tine")


# --- calf ---------------------------------------------------------------------

def calf_body(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["BELLY"], i, j, 151, 0.06)
    if face == "top":
        return fur(lerp(p["COAT_DARK"], p["COAT"], abs(x) / 3 + 0.3), i, j, 152)
    return fur(lerp(p["COAT"], p["BELLY"], max(0, (y - 1) / 2.5)) if face != "back" else p["COAT_DARK"], i, j, 153)


def calf_neck(p, face, x, y, z, i, j):
    c = p["COAT_DARK"] if face == "back" else lerp(p["COAT"], p["BELLY"], 0.3) if face == "front" else p["COAT"]
    return fur(c, i, j, 154)


def calf_head(p, face, x, y, z, i, j):
    if face in ("left", "right"):
        if abs(y + 0.5) < 0.6 and abs(z + 2.5) < 0.6:
            return p["EYE"]
    c = lerp(p["FACE"], p["COAT_DARK"], 0.3) if face == "top" else p["FACE"]
    return fur(c, i, j, 155, 0.05)


def calf_snout(p, face, x, y, z, i, j):
    t = (-3.5 - z) / 4
    c = lerp(p["FACE"], p["MUZZLE"], t)
    if face == "front":
        if abs(abs(x) - 0.75) < 0.4 and -0.5 < y < 1:
            return p["NOSTRIL"]
        return fur(p["MUZZLE"] if y < 1.5 else shade(p["MUZZLE"], 0.75), i, j, 156, 0.05)
    if face == "bottom":
        return fur(shade(p["MUZZLE"], 1.2), i, j, 157, 0.05)
    return fur(c, i, j, 158, 0.05)


def calf_ear(p, face, x, y, z, i, j):
    if face == "front":
        return p["COAT_DARK"] if abs(x) > 0.6 or y < -3.4 else fur(p["EAR_IN"], i, j, 159, 0.05)
    return fur(p["FACE"], i, j, 160)


def calf_leg(length, upper):
    def mat(p, face, x, y, z, i, j):
        if not upper and (face == "bottom" or y > length - 0.8):
            return p["HOOF"]
        c = p["COAT"] if upper and y < 1 else lerp(p["LEG_SHADE"], p["LEG"], 0.5)
        return fur(c, i, j, 161)
    return mat


CALF_BOXES = {
    "body": (0, 0, -3, -3.5, -6, 6, 7, 12, calf_body),
    "neck": (36, 0, -1.5, -5, -2, 3, 6, 4, calf_neck),
    "head": (36, 10, -2, -2, -3.5, 4, 4, 4, calf_head),
    "snout": (0, 19, -1.5, -1.5, -7.5, 3, 4, 4, calf_snout),
    "ear": (52, 0, -1, -4, -0.5, 2, 4, 1, calf_ear),
    "tail": (58, 0, -0.5, 0, 0, 1, 2, 1, tail_mat),
    "front_upper": (0, 27, -1, -1, -1, 2, 6, 2, calf_leg(5, True)),
    "front_lower": (8, 27, -0.5, 0, -0.5, 1, 5, 1, calf_leg(5, False)),
    "hind_upper": (12, 27, -1, -1, -1.5, 2, 6, 3, calf_leg(5, True)),
    "hind_lower": (22, 27, -0.5, 0, -0.5, 1, 5, 1, calf_leg(5, False)),
}


# --- painting -------------------------------------------------------------------

def paint(boxes, palette, size):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    px = img.load()
    owner = {}
    for name, (u, v, x0, y0, z0, w, hh, d, mat) in boxes.items():
        for face, tx, ty, x, y, z in face_texels(u, v, x0, y0, z0, w, hh, d):
            assert 0 <= tx < size[0] and 0 <= ty < size[1], (name, tx, ty)
            assert owner.setdefault((tx, ty), name) == name, f"UV overlap {name} / {owner[(tx, ty)]} at {tx},{ty}"
            c = mat(palette, face, x, y, z, tx, ty)
            px[tx, ty] = c if len(c) == 4 else (*c, 255)
    return img


def skinned(img, skinner, boxes, keep):
    """Flesh version; the boxes in {@code keep} (antlers) stay as they are."""
    out = skinner.skin(img.copy())
    src, dst = img.load(), out.load()
    for name in keep:
        u, v, _x, _y, _z, w, hh, d, _m = boxes[name]
        for tx in range(u, u + 2 * d + 2 * w):
            for ty in range(v, v + d + hh):
                dst[tx, ty] = src[tx, ty]
    assert out.getchannel("A").tobytes() == img.getchannel("A").tobytes()
    return out


def save(img, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    print("wrote", path.relative_to(ROOT))


def main():
    skinner = rd.load_skinner()
    for variant, palette in (("bull", BULL), ("cow", COW)):
        img = paint(BOXES, palette, (128, 64))
        save(img, TEX / "wildlife" / f"moose_{variant}.png")
        save(skinned(img, skinner, BOXES, ANTLER_BOXES), TEX / "skinned" / "wildlife" / f"moose_{variant}.png")
    # the young's skin now comes with its model from gen_herd_models.py (the *_BOXES table above is unused)


if __name__ == "__main__":
    main()
