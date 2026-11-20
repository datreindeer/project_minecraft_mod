"""Warm-country mammals: onager (+ foal, the tamed one's Pack Baskets), water buffalo (+ calf), tapir (+ striped calf),
plus the skinned (flesh) versions of the adults for butchered carcasses. Called by gen_fauna_warm_textures.py.

Usage: python scripts/gen_fauna_warm_tex_mammals.py
Deterministic. Each *_BOXES table must match the cubes in its model in client/wildlife/warm/ (texOffs, origin, size).
Looks:
- onager: sandy fawn with a sharp white belly, white legs, white rump patch and a white wedge behind the shoulder, a dark
  dorsal stripe from the short dark mane to the tail tuft, pale muzzle with a dark nose, long dark-tipped ears.
  The panniers are woven bark on a hide strap. Foal paler and fluffier.
- water buffalo: slate-grey to blackish, sparse-haired skin, pinkish-grey stockings, pale chevrons on the throat, dark
  ridged horns. Calf brownish-grey and hairier.
- tapir (Malayan): black with an off-white saddle over the back half and belly; white-rimmed ears. Calf chestnut with
  cream stripes and spots.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402

EYE, EYE_BROWN, GLINT = (14, 11, 10), (56, 36, 24), (196, 200, 208)


def eye_at(face, y, z, ey, ez, glint_left=True):
    """Two-texel eye on the head's side faces with a glint; None elsewhere."""
    if face not in ("left", "right") or abs(y - ey) >= 0.6:
        return None
    if abs(z - ez) < 0.6:
        return EYE
    if abs(z - ez - 1) < 0.6:
        return GLINT if (face == "left") == glint_left else EYE_BROWN
    return None


def hoof_px(base, face, x, z, i, j, cleft=True):
    if face == "bottom":
        return shade(base, 0.7) if cleft and abs(x) < 0.3 else base
    return shade(base, 1.0 + 0.25 * (h(i, j, 9) - 0.5))


# --------------------------------------------------------------------------------------------------------------- onager
ON = {
    "adult": dict(coat=(198, 160, 110), coat_dark=(172, 134, 88), coat_light=(216, 182, 134), white=(238, 232, 218),
                  stripe=(92, 62, 40), mane=(64, 46, 34), mane_light=(150, 118, 84), muzzle=(232, 224, 206),
                  nose=(56, 44, 38), ear_tip=(58, 42, 32), inner=(226, 210, 190), hoof=(72, 64, 58), fluff=0.06),
    "foal": dict(coat=(214, 186, 144), coat_dark=(196, 166, 124), coat_light=(228, 204, 166), white=(242, 238, 228),
                 stripe=(150, 112, 78), mane=(120, 90, 64), mane_light=(196, 168, 128), muzzle=(238, 232, 218),
                 nose=(104, 86, 74), ear_tip=(110, 82, 60), inner=(232, 220, 204), hoof=(132, 118, 104), fluff=0.11),
}


def on_body(top, bottom, z_front, z_back, stripe_w):
    """Sandy back and flanks, sharp white belly line that rises behind the shoulder and into the white rump patch."""
    length = z_back - z_front

    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        f = p["fluff"]
        t = (y - top) / (bottom - top)
        k = (z - z_front) / length                         # 0 chest .. 1 rump
        if face == "bottom":
            return fur(p["white"], i, j, 11, 0.04)
        if face == "top":
            c = lerp(p["coat_dark"], p["coat"], abs(x) / 3.5)
            if abs(x) < stripe_w:
                c = p["stripe"]
            elif k > 0.9:
                c = lerp(c, p["white"], 0.5)               # rump patch reaches over the croup
            return fur(c, i, j, 12, f)
        if face == "front":                                # chest: sandy above, white below
            c = p["coat_dark"] if t < 0.45 else lerp(p["coat_dark"], p["white"], smooth((t - 0.45) / 0.25))
            return fur(c, i, j, 13, f)
        if face == "back":                                 # rump: white, stripe running down to the tail
            c = p["white"]
            if abs(x) < stripe_w and t < 0.3:
                c = p["stripe"]
            elif t < 0.15:
                c = lerp(p["coat"], p["white"], 0.5)
            return fur(c, i, j, 14, f * 0.7)
        # sides
        line = 0.62
        line -= 0.22 * max(0.0, 1 - abs(k - 0.3) / 0.1)     # white wedge behind the shoulder
        line -= 0.3 * smooth((k - 0.72) / 0.2)              # belly white sweeps up into the rump patch
        if t < 0.18:
            c = lerp(p["coat_dark"], p["coat"], t / 0.18)
        else:
            c = lerp(p["coat"], p["coat_light"], (t - 0.18) / 0.5)
        if k < 0.2 and t < 0.5:
            c = lerp(c, p["coat_dark"], 0.25)               # shoulder shading
        if t > line:
            c = lerp(c, p["white"], smooth((t - line) / 0.12))
        if k > 0.88 and t > 0.12:
            c = lerp(c, p["white"], smooth((k - 0.88) / 0.08))  # rump patch wraps round the quarters
        return fur(c, i, j, 15, f)
    return mat


def on_neck(top, bottom, z_front, z_back):
    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        f = p["fluff"]
        if face == "front":                                # throat
            return fur(lerp(p["coat_light"], p["white"], 0.65), i, j, 21, f)
        if face == "back":
            return fur(p["mane"], i, j, 22, 0.08)
        if face in ("top", "bottom"):
            return fur(p["coat_dark"], i, j, 23, f)
        crest = (z - z_front) / (z_back - z_front)        # 0 throat .. 1 crest
        c = lerp(p["coat_light"], p["coat_dark"], crest * 0.7)
        if crest < 0.25:
            c = lerp(c, p["white"], 0.45)                  # pale throat edge
        if crest > 0.8:
            c = lerp(c, p["mane"], 0.55)                   # mane roots
        return fur(c, i, j, 24, f)
    return mat


def on_mane(v, face, x, y, z, i, j):
    """Short upright mane: dark core with fawn hair at the sides, ragged dark tips."""
    p = ON[v]
    if face in ("left", "right") and h(i, j, 31) < 0.35:
        return fur(lerp(p["mane"], p["mane_light"], 0.6), i, j, 32, 0.1)
    if face == "top" or face == "back" and h(i, j, 33) < 0.25:
        return fur(shade(p["mane"], 0.8), i, j, 34, 0.1)
    return fur(p["mane"], i, j, 35, 0.12)


def on_skull(top, bottom, z_front, eye_y, eye_z):
    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        if face == "front":                                # forehead / upper face
            return fur(lerp(p["coat"], p["coat_dark"], 0.3), i, j, 41, 0.05)
        if face == "top":
            return fur(lerp(p["coat_dark"], p["mane"], 0.35), i, j, 42, 0.06)
        if face == "bottom":
            return fur(p["white"], i, j, 44, 0.04)
        if face in ("left", "right"):
            e = eye_at(face, y, z, eye_y, eye_z)
            if e:
                return e
            c = p["coat"]
            if abs(y - eye_y) < 1.6 and eye_z - 1.6 < z < eye_z + 2.6:
                c = lerp(c, p["white"], 0.5)               # pale eye ring
            if y > eye_y + 1.2:
                c = lerp(c, p["white"], 0.6)               # white jowl
            return fur(c, i, j, 43, 0.05)
        return fur(p["coat_dark"], i, j, 45)
    return mat


def on_muzzle(x_half, top, bottom, z_front, z_back):
    """Pale mealy muzzle, the nose and lips dark at the very tip."""
    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        k = (z_back - z) / (z_back - z_front)             # 0 at the skull .. 1 at the nose
        if face == "front":
            if y > bottom - 1.0:
                return fur(lerp(p["nose"], p["muzzle"], 0.45), i, j, 50, 0.05)   # lower lip
            if abs(x) > 0.6 and y < top + 1.0:
                return shade(p["nose"], 0.55)              # nostrils
            return fur(lerp(p["nose"], p["muzzle"], 0.25), i, j, 51, 0.06)
        if face == "top":
            return fur(lerp(p["coat"], p["muzzle"], smooth((k - 0.2) / 0.5)), i, j, 52, 0.05)
        if face == "bottom":
            return fur(p["muzzle"], i, j, 53, 0.04)
        if face in ("left", "right"):
            if k > 0.75 and abs(y - (top + 0.5)) < 0.6:
                return shade(p["nose"], 0.8)               # nostril wrap
            if k > 0.5 and abs(y - (bottom - 1.5)) < 0.6:
                return shade(p["nose"], 1.1)               # mouth line
            c = lerp(p["coat"], p["muzzle"], smooth((k - 0.1) / 0.5))
            if k > 0.85:
                c = lerp(c, p["nose"], 0.35)
            return fur(c, i, j, 54, 0.05)
        return fur(p["coat"], i, j, 55)
    return mat


def on_ear(length):
    """Long ear: pale haired inside (front face), dark edges and tip, sandy back."""
    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        tip = y < -length + 1.6
        if face == "front":
            return fur(p["ear_tip"] if tip else p["inner"], i, j, 60, 0.05)
        if face in ("left", "right", "top") or tip:
            return fur(p["ear_tip"], i, j, 61, 0.06)
        return fur(lerp(p["coat"], p["coat_dark"], 0.3), i, j, 62, 0.05)
    return mat


def on_tail(v, face, x, y, z, i, j):
    p = ON[v]
    if face in ("back", "top"):
        return fur(p["stripe"], i, j, 70, 0.06)
    return fur(lerp(p["coat"], p["stripe"], y / 10), i, j, 71, 0.06)


def on_tuft(v, face, x, y, z, i, j):
    p = ON[v]
    c = p["mane"] if h(i, j // 2, 72) > 0.3 else lerp(p["mane"], p["mane_light"], 0.4)
    return fur(c, i, j, 73, 0.12)


def on_leg_upper(top, bottom, hind):
    """Sandy outer forearm / gaskin fading to white; the inside and back of the leg white."""
    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        t = (y - top) / (bottom - top)
        if face == "top":
            return p["coat"]
        if face == "back" or face == "bottom":
            return fur(p["white"], i, j, 90, 0.04)
        fade = 0.45 if face == "front" else 0.35 if not hind else 0.55
        c = lerp(p["coat"], p["white"], smooth((t - fade + 0.2) / 0.35))
        return fur(c, i, j, 91 + int(hind), p["fluff"])
    return mat


def on_leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = ON[v]
        if face == "bottom":
            return hoof_px(p["hoof"], face, x, z, i, j, cleft=False)
        if face == "top":
            return p["white"]
        if y > length - 1:
            return hoof_px(p["hoof"], face, x, z, i, j)
        c = p["white"]
        if y > length - 2 and face == "back":
            c = shade(c, 0.92)                             # fetlock
        return fur(c, i, j, 97, 0.04)
    return mat


BARK, BARK_DARK, BARK_LIGHT = (122, 84, 52), (78, 52, 32), (156, 114, 74)
HIDE, HIDE_DARK = (150, 108, 70), (104, 72, 46)


def basket_mat(v, face, x, y, z, i, j):
    """Woven bark pannier: horizontal strips over upright stakes, a darker rim, the hide strap down the outer face."""
    if face == "top":
        rim = abs(x + 1.5) > 0.9 or abs(z) > 2.9
        return fur(BARK_LIGHT if rim else (52, 36, 24), i, j, 100, 0.08)
    if face == "bottom":
        return fur(BARK_DARK, i, j, 101, 0.08)
    if y < 1.0:
        return fur(shade(BARK_LIGHT, 0.95), i, j, 102, 0.06)    # bound rim
    if face == "right" and abs(z + 0.5) < 0.6:
        return fur(HIDE_DARK if y > 5 else HIDE, i, j, 103, 0.05)  # strap runs down to the bottom
    row = int(y) % 2
    col = int((z if face in ("left", "right") else x) + 10) % 2
    c = BARK_LIGHT if row == col else BARK                  # over-under weave
    if h(i, j, 104) < 0.15:
        c = BARK_DARK
    return fur(shade(c, 1.0 - (y / 6) * 0.15), i, j, 105, 0.06)


def strap_mat(v, face, x, y, z, i, j):
    c = HIDE
    if face in ("front", "back") or abs(z + 0.5) > 1.0:
        c = HIDE_DARK                                       # rolled edges
    if face == "top" and abs(z + 0.5) < 0.6 and int(x + 10) % 2 == 0:
        c = shade(HIDE, 1.15)                               # stitching
    return fur(c, i, j, 106, 0.05)


# mirrors OnagerModel.createBodyLayer
ONAGER_BOXES = {
    "body": (0, 0, -3.5, -3.5, -9, 7, 7, 18, on_body(-3.5, 3.5, -9, 9, 0.6)),
    "tail": (116, 0, -0.5, 0, -0.5, 1, 8, 1, on_tail),
    "tuft": (120, 0, -1, 7, -1, 2, 3, 2, on_tuft),
    "strap": (0, 40, -4, -4, -2, 8, 1, 3, strap_mat),
    "basket": (0, 46, -3, 0, -3.5, 3, 6, 7, basket_mat),
    "neck": (52, 0, -1.5, -8, -2.5, 3, 9, 5, on_neck(-8, 1, -2.5, 2.5)),
    "mane": (68, 0, -0.5, -9, 2, 1, 9, 2, on_mane),
    "skull": (76, 0, -2, -2, -4, 4, 4, 5, on_skull(-2, 2, -4, -0.5, -1.5)),
    "muzzle": (94, 0, -1.5, -1, -9, 3, 3, 5, on_muzzle(1.5, -1, 2, -9, -4)),
    "ear": (110, 0, -1, -5, -0.5, 2, 5, 1, on_ear(5)),
    "front_upper": (0, 27, -1.5, -1, -1.5, 3, 6, 3, on_leg_upper(-1, 5, False)),
    "hind_upper": (12, 27, -1.5, -1, -2, 3, 6, 4, on_leg_upper(-1, 5, True)),
    "lower": (26, 27, -1, 0, -1, 2, 6, 2, on_leg_lower(6)),
}

# mirrors OnagerFoalModel.createBodyLayer
ONAGER_FOAL_BOXES = {
    "body": (0, 0, -2, -2, -4.5, 4, 4, 9, on_body(-2, 2, -4.5, 4.5, 0.6)),
    "neck": (28, 0, -1, -5.5, -1.5, 2, 6, 3, on_neck(-5.5, 0.5, -1.5, 1.5)),
    "mane": (38, 0, -0.5, -6, 1, 1, 5, 1, on_mane),
    "skull": (42, 0, -2, -2, -3.5, 4, 4, 4, on_skull(-2, 2, -3.5, -0.5, -2)),
    "muzzle": (28, 10, -1.5, -1, -7, 3, 3, 4, on_muzzle(1.5, -1, 2, -7, -3)),
    "ear": (58, 0, -1, -4, -0.5, 2, 4, 1, on_ear(4)),
    "tail": (42, 10, -0.5, 0, -0.5, 1, 5, 1, on_tuft),
    "front_upper": (0, 15, -1, -1, -1, 2, 5, 2, on_leg_upper(-1, 4, False)),
    "hind_upper": (12, 15, -1, -1, -1.5, 2, 5, 3, on_leg_upper(-1, 4, True)),
    "lower": (8, 15, -0.5, 0, -0.5, 1, 6, 1, on_leg_lower(6)),
}

# -------------------------------------------------------------------------------------------------------- water buffalo
WB = {
    "adult": dict(skin=(64, 64, 68), dark=(40, 40, 44), light=(90, 90, 96), belly=(74, 72, 74), head=(52, 52, 56),
                  stocking=(150, 134, 130), chevron=(196, 190, 182), muzzle=(46, 46, 50), lip=(150, 144, 140),
                  nose=(26, 26, 30), mud=(98, 84, 66), hair=0.04, tuft=(24, 22, 22)),
    "calf": dict(skin=(118, 100, 86), dark=(94, 80, 70), light=(140, 122, 104), belly=(132, 116, 102), head=(104, 88, 76),
                 stocking=(170, 152, 138), chevron=(214, 204, 188), muzzle=(84, 72, 66), lip=(176, 164, 154),
                 nose=(52, 44, 42), mud=(110, 92, 70), hair=0.1, tuft=(70, 58, 50)),
}
WB_HOOF = (42, 38, 36)
HORN_ROOT, HORN_RIDGE, HORN_TIP = (88, 84, 78), (122, 116, 106), (34, 32, 30)


def wb_skin(p, c, i, j, salt):
    """Sparse-haired hide: little hair streaking, a few pale and dark specks."""
    n = h(i, j, salt + 3)
    if n < 0.06:
        c = shade(c, 1.18)
    elif n > 0.95:
        c = shade(c, 0.85)
    return fur(c, i, j, salt, p["hair"], streak=p["hair"] > 0.05)


def wb_body(top, bottom, z_front, z_back):
    length = z_back - z_front

    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        t = (y - top) / (bottom - top)
        k = (z - z_front) / length
        if face == "bottom":
            c = p["belly"]
            if h(i // 2, j // 2, 10) < 0.25:
                c = lerp(c, p["mud"], 0.35)                 # dried mud from the wallow
            return wb_skin(p, c, i, j, 11)
        if face == "top":
            return wb_skin(p, lerp(p["dark"], p["skin"], abs(x) / 7 * 0.8), i, j, 12)
        if t < 0.15:
            c = lerp(p["dark"], p["skin"], t / 0.15)
        elif t < 0.55:
            c = lerp(p["skin"], p["light"], (t - 0.15) / 0.4 * 0.5)
        else:
            c = lerp(lerp(p["skin"], p["light"], 0.5), p["belly"], (t - 0.55) / 0.45)
        if face in ("left", "right"):
            if abs(k - 0.82) < 0.12 and t < 0.6:
                c = lerp(c, p["light"], 0.2 * (1 - abs(k - 0.82) / 0.12))   # hip bone catches the light
            if abs(k - 0.2) < 0.03 and t > 0.4:
                c = shade(c, 0.88)                          # elbow crease
            if t > 0.8 and h(i // 2, j, 16) < 0.35:
                c = lerp(c, p["mud"], 0.4)                  # mud line low on the flank
        if face == "front":
            c = lerp(c, p["dark"], 0.25)
        return wb_skin(p, c, i, j, 13)
    return mat


def wb_neck(top, bottom, z_front, z_back):
    """Thick neck; the pale chevron necklace across the underside and up the lower sides."""
    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        zb = z_back - z                                     # 0 at the chest .. depth at the head
        if face == "bottom":
            band = abs(zb - 1.8 - abs(x) * 0.55) < 0.75
            c = p["chevron"] if band else lerp(p["head"], p["belly"], 0.3)
            return wb_skin(p, c, i, j, 31)
        if face == "top":
            return wb_skin(p, p["dark"], i, j, 32)
        if face in ("front", "back"):
            return wb_skin(p, p["head"], i, j, 33)
        t = (y - top) / (bottom - top)
        c = lerp(p["skin"], p["head"], 0.4)
        if t > 0.8 and abs(zb - 1.8 - (1 - t) * 3) < 0.7:
            c = p["chevron"]                                # chevron arms run up the sides
        if h(i, j // 3, 34) < 0.22:
            c = shade(c, 0.86)                              # skin folds
        return wb_skin(p, c, i, j, 35)
    return mat


def wb_skull(top, bottom, z_front, eye_y, eye_z):
    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        if face == "front":                                 # broad forehead with a little bristly hair
            c = p["head"] if h(i, j, 41) > 0.2 else shade(p["head"], 1.2)
            return fur(c, i, j, 42, 0.07)
        if face == "top":
            return wb_skin(p, p["dark"], i, j, 43)
        if face == "bottom":                                # the second chevron under the jaw
            c = p["chevron"] if abs(z - z_front - 2) < 0.6 or abs(x) > 2.5 and z < z_front + 2.5 else p["belly"]
            return wb_skin(p, c, i, j, 44)
        if face in ("left", "right"):
            e = eye_at(face, y, z, eye_y, eye_z, glint_left=False)
            if e:
                return e
            c = p["head"]
            if abs(y - eye_y + 1) < 0.6 and abs(z - eye_z - 0.5) < 1.6:
                c = lerp(c, p["chevron"], 0.35)            # pale brow
            if y > eye_y + 2:
                c = lerp(c, p["skin"], 0.3)
            return wb_skin(p, c, i, j, 45)
        return wb_skin(p, p["dark"], i, j, 46)
    return mat


def wb_muzzle(x_half, top, bottom, z_front, z_back):
    """Broad, wet, dark nose plate; pale grey lips and chin."""
    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        k = (z_back - z) / (z_back - z_front)
        if face == "front":
            if y > bottom - 1.0:
                return fur(p["lip"], i, j, 50, 0.05)
            if y > bottom - 2.0:
                return shade(p["nose"], 1.3)                # mouth
            if abs(abs(x) - x_half * 0.55) < 0.6 and y < top + 2:
                return shade(p["nose"], 0.5)                # wide nostrils
            c = p["nose"] if h(i, j, 51) > 0.15 else shade(p["nose"], 1.6)   # moist sheen
            return c
        if face == "top":
            return wb_skin(p, lerp(p["head"], p["muzzle"], k), i, j, 52)
        if face == "bottom":
            return fur(lerp(p["lip"], p["belly"], 1 - k), i, j, 53, 0.05)
        if face in ("left", "right"):
            if y > bottom - 1.0:
                return fur(p["lip"], i, j, 54, 0.05)
            if abs(y - (bottom - 1.5)) < 0.6 and k > 0.4:
                return shade(p["nose"], 1.3)
            return wb_skin(p, lerp(p["head"], p["muzzle"], k), i, j, 55)
        return wb_skin(p, p["head"], i, j, 56)
    return mat


def wb_ear(v, face, x, y, z, i, j):
    """Droopy ear sticking out along x; -x is the outer tip. Pale hairy fringe inside."""
    p = WB[v]
    if face == "bottom":                                    # hangs down: the underside is the open inner ear
        rim = x < -1.0 or abs(z) > 0.6
        return fur(p["head"] if rim else lerp(p["chevron"], p["skin"], 0.5), i, j, 60, 0.1)
    return wb_skin(p, p["head"], i, j, 61)


def wb_horn(t0, t1, length):
    """Dark grey horn with pale transverse ridges near the base, smooth near-black tip (t = 0 skull .. 1 tip)."""
    def mat(v, face, x, y, z, i, j):
        t = t0 if face == "bottom" else t1 if face == "top" else t0 + (t1 - t0) * (-y / length)
        c = lerp(HORN_ROOT, HORN_TIP, smooth(t))
        if t < 0.7 and face not in ("top", "bottom") and int(-y * 1.0) % 2 == 0:
            c = lerp(c, HORN_RIDGE, 0.55 * (1 - t / 0.7))  # ridges, strongest at the base
        if face in ("front", "right") and t < 0.9:
            c = shade(c, 1.08)                              # sheen
        return shade(c, 1 + (h(i, j, 70) - 0.5) * 0.08)
    return mat


def wb_tail(v, face, x, y, z, i, j):
    p = WB[v]
    return wb_skin(p, lerp(p["skin"], p["dark"], y / 11), i, j, 80)


def wb_tuft(v, face, x, y, z, i, j):
    p = WB[v]
    return fur(p["tuft"] if h(i, j // 2, 81) > 0.3 else shade(p["tuft"], 1.7), i, j, 82, 0.1)


def wb_leg_upper(top, bottom, hind):
    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        t = (y - top) / (bottom - top)
        if face == "top":
            return p["skin"]
        c = lerp(p["skin"], p["dark"], t * 0.6)
        if face == "front":
            c = shade(c, 0.94)
        if t > 0.75:
            c = lerp(c, p["stocking"], (t - 0.75) / 0.25 * 0.5)   # stockings start at the knee
        return wb_skin(p, c, i, j, 90 + int(hind))
    return mat


def wb_leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        if face == "top":
            return p["stocking"]
        if face == "bottom":
            return WB_HOOF
        c = p["stocking"]
        if face == "back" and y > length - 1:
            c = WB_HOOF                                     # dewclaws
        if y < 1.0:
            c = lerp(c, p["dark"], 0.4)
        if h(i, j // 2, 95) < 0.18:
            c = lerp(c, p["mud"], 0.5)                      # mud splashes
        return wb_skin(p, c, i, j, 96)
    return mat


def wb_hoof(v, face, x, y, z, i, j):
    if face in ("front", "top") and abs(x) < 0.6 and z < -1.0:
        return shade(WB_HOOF, 0.6)                          # cleft between the splayed toes
    if face == "top" and abs(x) < 1.5 and -1.5 < z < 1.5:
        return WB[v]["stocking"]                            # hidden under the leg
    return hoof_px(WB_HOOF, face, x, z, i, j)


def wb_calf_leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = WB[v]
        if face == "bottom":
            return hoof_px(WB_HOOF, face, x, z, i, j)
        if face == "top":
            return p["stocking"]
        if y > length - 1:
            return hoof_px(WB_HOOF, face, x, z, i, j)
        return wb_skin(p, lerp(p["stocking"], p["skin"], 0.3 * (1 - y / length)), i, j, 97)
    return mat


# mirrors WaterBuffaloModel.createBodyLayer
BUFFALO_BOXES = {
    "body": (0, 0, -7, -6, -11, 14, 12, 22, wb_body(-6, 6, -11, 11)),
    "tail": (122, 16, -0.5, 0, -0.5, 1, 11, 1, wb_tail),
    "tuft": (100, 26, -1, 10, -1, 2, 3, 2, wb_tuft),
    "neck": (72, 0, -3.5, -4.5, -6, 7, 8, 7, wb_neck(-4.5, 3.5, -6, 1)),
    "skull": (100, 0, -3.5, -3.5, -5, 7, 7, 5, wb_skull(-3.5, 3.5, -5, -1, -3.5)),
    "muzzle": (72, 16, -2.5, -1, -9, 5, 5, 4, wb_muzzle(2.5, -1, 4, -9, -5)),
    "ear": (90, 16, -1.5, -0.5, -1, 3, 1, 2, wb_ear),
    "horn": (100, 16, -1, -6, -1.5, 2, 6, 3, wb_horn(0.0, 0.4, 6)),
    "horn_mid": (110, 16, -1, -5, -1, 2, 5, 2, wb_horn(0.38, 0.72, 5)),
    "horn_tip": (118, 16, -0.5, -4, -0.5, 1, 4, 1, wb_horn(0.72, 1.0, 4)),
    "front_upper": (0, 36, -2.5, -1, -2.5, 5, 5, 5, wb_leg_upper(-1, 4, False)),
    "hind_upper": (20, 36, -2.5, -1, -3, 5, 5, 6, wb_leg_upper(-1, 4, True)),
    "lower": (42, 36, -2, 0, -2, 4, 4, 4, wb_leg_lower(4)),
    "hoof": (58, 36, -2.5, 4, -3, 5, 1, 5, wb_hoof),
}

# mirrors WaterBuffaloCalfModel.createBodyLayer
BUFFALO_CALF_BOXES = {
    "body": (0, 0, -3.5, -3.5, -6, 7, 7, 12, wb_body(-3.5, 3.5, -6, 6)),
    "neck": (38, 0, -2, -2.5, -4, 4, 5, 5, wb_neck(-2.5, 2.5, -4, 1)),
    "skull": (38, 10, -2.5, -2.5, -3.5, 5, 5, 4, wb_skull(-2.5, 2.5, -3.5, -0.5, -2.5)),
    "muzzle": (34, 19, -1.5, -1, -6.5, 3, 3, 3, wb_muzzle(1.5, -1, 2, -6.5, -3.5)),
    "ear": (46, 19, -1.5, -0.5, -1, 3, 1, 2, wb_ear),
    "tail": (56, 10, -0.5, 0, -0.5, 1, 7, 1, wb_tuft),
    "front_upper": (0, 19, -1.5, -1, -1.5, 3, 4, 3, wb_leg_upper(-1, 3, False)),
    "hind_upper": (12, 19, -1.5, -1, -2, 3, 4, 4, wb_leg_upper(-1, 3, True)),
    "lower": (26, 19, -1, 0, -1, 2, 5, 2, wb_calf_leg_lower(5)),
}

# ---------------------------------------------------------------------------------------------------------------- tapir
TP = dict(black=(30, 29, 32), black_hi=(52, 52, 58), saddle=(222, 216, 204), saddle_shade=(188, 182, 170),
          rim=(226, 224, 218), skin=(70, 64, 66), lip=(96, 86, 86), nose=(20, 19, 22), toe=(104, 96, 88))
TC = dict(brown=(112, 66, 38), brown_dark=(82, 46, 26), cream=(232, 214, 176), belly=(200, 170, 130),
          nose=(46, 32, 26), toe=(96, 80, 64))


def tp_black(c, i, j, salt):
    """Short sparse black coat with a cool sheen."""
    return fur(c, i, j, salt, 0.06, streak=False)


def tp_saddle_edge(x, y, z, top, bottom):
    """Front edge of the saddle (model z), wobbling a little and slanting back towards the belly."""
    t = (y - top) / (bottom - top)
    return -2.5 + 0.6 * t + (h(int(y * 2), 0, 120) - 0.5) * 0.8


def tp_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            edge = -2.5 + (h(int(x + 10), 1, 121) - 0.5) * 0.8
        else:
            edge = tp_saddle_edge(x, y, z, top, bottom)
        if face == "front":
            white = False
        elif face == "back":
            white = True
        else:
            white = z > edge
        if white:
            c = lerp(TP["saddle"], TP["saddle_shade"], 0.6 * t if face != "top" else 0.0)
            return fur(c, i, j, 130, 0.05)
        c = lerp(TP["black_hi"], TP["black"], min(1.0, t * 1.6)) if face != "bottom" else TP["black"]
        return tp_black(c, i, j, 131)
    return mat


def tp_rump(top, bottom):
    """Rump: white saddle, the thighs black below and behind (they run on into the black hind legs)."""
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        thigh = (face in ("left", "right") and t > 0.68 - (z - 1) * 0.02 and z > 3.5) or (face == "back" and t > 0.75)
        if face == "bottom":
            thigh = z > 6
        if thigh:
            return tp_black(TP["black"], i, j, 132)
        c = lerp(TP["saddle"], TP["saddle_shade"], 0.55 * t if face != "top" else 0.0)
        if face == "back" and abs(x) < 1.0 and t > 0.25:
            c = shade(c, 0.92)                              # under the tail
        return fur(c, i, j, 133, 0.05)
    return mat


def tp_neck(v, face, x, y, z, i, j):
    c = TP["black_hi"] if face == "top" else TP["black"]
    if face in ("left", "right") and h(i, j // 3, 134) < 0.25:
        c = shade(c, 0.85)                                  # thick skin folds
    return tp_black(c, i, j, 135)


def tp_head(eye_y, eye_z):
    def mat(v, face, x, y, z, i, j):
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return (40, 26, 20)                         # small dark-brown eye
            if abs(y - eye_y + 1) < 0.6 and abs(z - eye_z) < 0.6:
                return shade(TP["black_hi"], 1.4) if face == "right" else TP["black"]
            if y > 1.5:
                return tp_black(lerp(TP["black"], TP["lip"], 0.35 if y > 2 else 0.0), i, j, 140)   # lower lip / chin
        if face == "bottom":
            return tp_black(lerp(TP["black"], TP["lip"], 0.3), i, j, 141)
        return tp_black(TP["black_hi"] if face == "top" else TP["black"], i, j, 142)
    return mat


def tp_trunk(v, face, x, y, z, i, j):
    """Wrinkled proboscis: rings of darker skin along it."""
    c = TP["black"] if face != "bottom" else TP["skin"]
    if int(z * 2) % 2 == 0 and face in ("top", "left", "right"):
        c = shade(c, 1.35)
    return tp_black(c, i, j, 143)


def tp_tip(v, face, x, y, z, i, j):
    if face == "front":                                     # nostrils on the flat tip
        return TP["nose"] if abs(x) < 0.6 or y < -0.4 else shade(TP["skin"], 0.8)
    return tp_black(TP["skin"] if face == "bottom" else TP["black"], i, j, 144)


def tp_ear(v, face, x, y, z, i, j):
    """Rounded black ear with the famous white rim (top and edges), pinkish-grey inside."""
    rim = y < -2.0 or face in ("left", "right", "top")
    if face == "front":
        return TP["rim"] if rim or abs(x) > 0.6 and y < -1 else fur(TP["skin"], i, j, 150, 0.05)
    return TP["rim"] if rim else tp_black(TP["black"], i, j, 151)


def tp_tail(v, face, x, y, z, i, j):
    return tp_black(TP["black"], i, j, 152)


def tp_leg_upper(v, face, x, y, z, i, j):
    return tp_black(TP["black_hi"] if face == "front" else TP["black"], i, j, 160)


def tp_foot(toes, length):
    """Lower leg and foot: small hoofed toes, `toes` across the front (the front foot's fourth on the outer side)."""
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return TP["toe"] if z < 0 else shade(TP["skin"], 0.7)   # toes in front, pad behind
        if face == "top":
            return TP["black"]
        if y > length - 1:
            if face == "front":
                return shade(TP["toe"], 1.1 if (i % 2 == 0) == (toes == 3) else 0.85)
            if face == "right" and toes == 4 and z < -0.5:
                return TP["toe"]                            # the small outer fourth toe
            return shade(TP["skin"], 0.7)
        return tp_black(TP["black"], i, j, 161)
    return mat


def tc_coat(c_base, x, y, z, along, across, i, j, salt, spots=False):
    """Watermelon coat: cream stripes running along the body, broken into dashes and spots."""
    if spots:
        cell = h(int((along + 50) // 1.6), int((across + 50) // 1.6), salt + 1)
        if cell < 0.33 and h(i, j, salt + 2) < 0.8:
            return fur(TC["cream"], i, j, salt + 3, 0.05)
        return fur(c_base, i, j, salt + 4, 0.08)
    wob = 0.5 * h(int(along // 2 + 50), 0, salt)
    band = ((across + wob) / 2.0) % 1.0
    if band < 0.38:
        gap = h(int((along + 50) // 2.5), int(across // 2 + 50), salt + 5)
        if gap > 0.2:
            return fur(TC["cream"], i, j, salt + 6, 0.05)
    return fur(c_base, i, j, salt + 7, 0.08)


def tc_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return tc_coat(TC["belly"], x, y, z, z, x, i, j, 170, spots=True)
        if face == "top":
            return tc_coat(TC["brown_dark"], x, y, z, z, x, i, j, 171)
        if face in ("front", "back"):
            return tc_coat(TC["brown"], x, y, z, x, y, i, j, 172, spots=True)
        t = (y - top) / (bottom - top)
        base = lerp(TC["brown_dark"], TC["brown"], t * 2) if t < 0.5 else lerp(TC["brown"], TC["belly"], (t - 0.5) * 1.2)
        return tc_coat(base, x, y, z, z, y, i, j, 173)
    return mat


def tc_spotted(salt, dark=False):
    def mat(v, face, x, y, z, i, j):
        base = TC["brown_dark"] if dark or face == "top" else TC["brown"]
        along = z if face in ("left", "right", "top", "bottom") else x
        return tc_coat(base, x, y, z, along, y if face not in ("top", "bottom") else x, i, j, salt, spots=True)
    return mat


def tc_head(eye_y, eye_z):
    spotted = tc_spotted(180)

    def mat(v, face, x, y, z, i, j):
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return (30, 20, 16)
            if abs(y - eye_y + 1) < 0.6 and abs(z - eye_z) < 0.6 and face == "right":
                return (200, 190, 180)
        if face == "bottom":
            return fur(TC["belly"], i, j, 181, 0.06)
        return spotted(v, face, x, y, z, i, j)
    return mat


def tc_trunk(v, face, x, y, z, i, j):
    if face == "front":
        return TC["nose"] if abs(x) < 0.6 else shade(TC["nose"], 1.5)
    return fur(lerp(TC["brown_dark"], TC["nose"], 0.4), i, j, 182, 0.06)


def tc_ear(v, face, x, y, z, i, j):
    rim = y < -1.0 or face in ("left", "right", "top")
    if face == "front":
        return TC["cream"] if rim else fur(lerp(TC["brown"], TC["belly"], 0.4), i, j, 183, 0.05)
    return TC["cream"] if rim else fur(TC["brown_dark"], i, j, 184, 0.06)


def tc_foot(length):
    leg = tc_spotted(190, dark=True)

    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return TC["toe"] if z < 0 else shade(TC["nose"], 1.2)
        if y > length - 1 and face != "top":
            return shade(TC["toe"], 1.1 if i % 2 == 0 else 0.85)
        return leg(v, face, x, y, z, i, j)
    return mat


# mirrors TapirModel.createBodyLayer
TAPIR_BOXES = {
    "body": (0, 0, -4.5, -4.5, -9, 9, 9, 16, tp_body(-4.5, 4.5)),
    "rump": (0, 25, -5, -5.5, 1, 10, 10, 8, tp_rump(-5.5, 4.5)),
    "tail": (54, 43, -1, 0, -1, 2, 3, 2, tp_tail),
    "neck": (36, 25, -3, -3, -4, 6, 6, 5, tp_neck),
    "head": (0, 43, -2.5, -2.5, -6, 5, 5, 6, tp_head(-0.5, -2.5)),
    "trunk": (22, 43, -1.5, -1.5, -3, 3, 3, 3, tp_trunk),
    "trunk2": (34, 43, -1, -1, -2, 2, 2, 2, tp_trunk),
    "trunk_tip": (42, 43, -1, -1, -1, 2, 2, 1, tp_tip),
    "ear": (48, 43, -1, -3, -0.5, 2, 3, 1, tp_ear),
    "front_upper": (0, 54, -1.5, -1, -1.5, 3, 5, 3, tp_leg_upper),
    "hind_upper": (12, 54, -1.5, -1, -2, 3, 5, 4, tp_leg_upper),
    "front_lower": (26, 54, -1.5, 0, -1.5, 3, 4, 3, tp_foot(4, 4)),
    "hind_lower": (38, 54, -1.5, 0, -1.5, 3, 4, 3, tp_foot(3, 4)),
}

# mirrors TapirCalfModel.createBodyLayer
TAPIR_CALF_BOXES = {
    "body": (0, 0, -3, -3, -5.5, 6, 6, 11, tc_body(-3, 3)),
    "tail": (58, 0, -0.5, 0, -0.5, 1, 2, 1, tc_spotted(185, dark=True)),
    "neck": (34, 0, -2, -2, -3, 4, 4, 4, tc_spotted(186)),
    "head": (34, 8, -2, -2, -4, 4, 4, 4, tc_head(-0.5, -2.5)),
    "trunk": (50, 0, -1, -1, -2, 2, 2, 2, tc_trunk),
    "trunk_tip": (50, 4, -0.5, -0.5, -2, 1, 1, 2, tc_trunk),
    "ear": (56, 4, -1, -2, -0.5, 2, 2, 1, tc_ear),
    "front_upper": (0, 17, -1, -1, -1, 2, 4, 2, tc_spotted(187, dark=True)),
    "hind_upper": (8, 17, -1, -1, -1.5, 2, 4, 3, tc_spotted(188, dark=True)),
    "lower": (18, 17, -1, 0, -1, 2, 4, 2, tc_foot(4)),
}


def main():
    img = paint(ONAGER_BOXES, "adult", (128, 64))
    save(img, "wildlife/onager.png")
    save(skinned(img, ONAGER_BOXES, ("lower", "strap", "basket")), "skinned/wildlife/onager.png")
    save(paint(ONAGER_FOAL_BOXES, "foal", (64, 32)), "wildlife/onager_foal.png")

    img = paint(BUFFALO_BOXES, "adult", (128, 64))
    save(img, "wildlife/water_buffalo.png")
    save(skinned(img, BUFFALO_BOXES, ("horn", "horn_mid", "horn_tip", "hoof")), "skinned/wildlife/water_buffalo.png")
    save(paint(BUFFALO_CALF_BOXES, "calf", (64, 32)), "wildlife/water_buffalo_calf.png")

    img = paint(TAPIR_BOXES, "adult", (64, 64))
    save(img, "wildlife/tapir.png")
    save(skinned(img, TAPIR_BOXES, ()), "skinned/wildlife/tapir.png")
    save(paint(TAPIR_CALF_BOXES, "calf", (64, 32)), "wildlife/tapir_calf.png")


if __name__ == "__main__":
    main()
