"""Paint the aurochs textures for AurochsModel (bull, cow; 128x64) and AurochsCalfModel (calf; 64x32), plus the skinned
(flesh) versions of the adults for butchered carcasses.

Usage: python scripts/gen_aurochs_texture.py
Deterministic. BOXES / CALF_BOXES must match the cubes in the two models (texOffs, origin, size).
Look: bull near-black with a pale eel stripe along the spine, a pale curly forelock between the horns and the pale
muzzle ring; cow red-brown with a darker head and legs; calf bright chestnut. Horns cream with black tips.
"""
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth

PALETTES = {
    "bull": dict(coat=(44, 33, 28), coat_dark=(24, 19, 17), coat_light=(72, 54, 42), belly=(32, 25, 22),
                 head=(36, 28, 24), head_dark=(20, 16, 14), stripe=(156, 124, 86), forelock=(132, 100, 70),
                 leg=(28, 22, 20), muzzle=(198, 186, 162), nose=(34, 30, 30), tuft=(16, 13, 12)),
    "cow": dict(coat=(122, 68, 38), coat_dark=(86, 46, 27), coat_light=(152, 92, 54), belly=(100, 60, 37),
                head=(82, 47, 30), head_dark=(56, 33, 22), stripe=(172, 124, 78), forelock=(122, 76, 46),
                leg=(62, 40, 28), muzzle=(202, 188, 162), nose=(44, 36, 34), tuft=(30, 22, 18)),
    "calf": dict(coat=(156, 88, 46), coat_dark=(124, 68, 36), coat_light=(184, 114, 66), belly=(168, 116, 78),
                 head=(138, 78, 42), head_dark=(108, 60, 33), stripe=(188, 128, 76), forelock=(160, 98, 56),
                 leg=(126, 74, 42), muzzle=(216, 198, 172), nose=(76, 56, 52), tuft=(92, 56, 34)),
}
EYE, EYE_BROWN, GLINT = (14, 11, 10), (52, 34, 24), (190, 196, 204)
INNER_EAR = (176, 140, 118)
HOOF = (30, 27, 26)
HORN_BASE, HORN_MID, HORN_TIP = (150, 140, 122), (228, 218, 194), (28, 25, 24)


def P(v):
    return PALETTES[v]


def coat_side(p, y, top, bottom):
    """Back -> flank -> belly for a body side: darker along the back, a sheen on the upper flank, darker low down."""
    t = (y - top) / (bottom - top)
    if t < 0.15:
        return lerp(p["coat_dark"], p["coat"], t / 0.15)
    if t < 0.5:
        return lerp(p["coat"], p["coat_light"], (t - 0.15) / 0.35 * 0.55)
    if t < 0.85:
        return lerp(lerp(p["coat"], p["coat_light"], 0.55), p["coat_dark"], (t - 0.5) / 0.35)
    return lerp(p["coat_dark"], p["belly"], (t - 0.85) / 0.15)


def eel(p, c, x, width, amount=0.75):
    """Pale stripe along the spine (strongest on the bull)."""
    if abs(x) < width:
        return lerp(c, p["stripe"], amount * (1 - abs(x) / (width * 1.6)))
    return c


def barrel(top, bottom, z_front, z_back, stripe_w):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "bottom":
            return fur(p["belly"], i, j, 11, 0.06)
        if face == "top":
            c = lerp(p["coat_dark"], p["coat"], abs(x) / 6 + 0.1)
            return fur(eel(p, c, x, stripe_w), i, j, 12)
        c = coat_side(p, y, top, bottom)
        if face in ("left", "right"):
            hz = z_back - (z_back - z_front) * 0.25        # rounded haunch catches the light
            if abs(z - hz) < 3.5 and y < (top + bottom) / 2:
                c = lerp(c, p["coat_light"], 0.2 * (1 - abs(z - hz) / 3.5))
            if z > z_back - 1.5:                            # shadow at the hip edge
                c = shade(c, 0.9)
        if face == "back" and abs(x) < 1.5 and y < top + 2:
            c = eel(p, c, x, stripe_w)                      # stripe runs down to the tail root
        return fur(c, i, j, 13)
    return mat


def chest(top, bottom, stripe_w):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "bottom":
            return fur(p["belly"], i, j, 21, 0.06)
        if face == "top":
            return fur(eel(p, lerp(p["coat_dark"], p["coat"], abs(x) / 7), x, stripe_w), i, j, 22)
        if face == "front":                                 # brisket: darkest part of the forehand
            c = lerp(p["coat_dark"], p["belly"], (y - top) / (bottom - top) * 0.5)
            return fur(c, i, j, 23, 0.08)
        c = coat_side(p, y, top, bottom)
        if face in ("left", "right"):
            c = lerp(c, p["coat_dark"], 0.35 if v == "bull" else 0.15)   # heavy, dark forehand
            if abs(z + 6.5) < 1.0 and y > 0:
                c = shade(c, 0.9)                           # elbow crease
        return fur(c, i, j, 24)
    return mat


def hump_mat(v, face, x, y, z, i, j):
    p = P(v)
    c = lerp(p["coat_dark"], p["coat"], 0.3)
    if face == "top":
        c = eel(p, c, x, 1.2)
    return fur(c, i, j, 25, 0.09)


def neck(top, bottom, z_front, stripe_w):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "top":                                   # crest
            return fur(eel(p, p["coat_dark"], x, stripe_w, 0.6), i, j, 31)
        if face == "bottom":
            return fur(shade(p["coat_dark"], 0.9), i, j, 32)
        if face == "front":
            return fur(p["head_dark"], i, j, 33)
        c = coat_side(p, y, top, bottom)
        c = lerp(c, p["head"], max(0.0, (z_front + 3 - z) / 3))   # darker towards the head
        if h(i, j // 3, 34) < 0.25:
            c = shade(c, 0.88)                              # skin folds
        return fur(c, i, j, 35, 0.08)
    return mat


def dewlap_mat(v, face, x, y, z, i, j):
    p = P(v)
    return fur(lerp(p["coat_dark"], p["belly"], 0.3), i, j, 36, 0.08)


def skull(top, bottom, z_front, eye_y, eye_z):
    """Box faces: front = forehead (the head hangs nose-down), top = poll, sides carry the eyes."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        curl = h(i // 2 + (j % 2), j // 2, 41)
        if face == "front":
            if y < top + (bottom - top) * 0.4:              # curly forelock above the muzzle
                c = lerp(p["forelock"], p["head"], 0.35 if curl < 0.5 else 0.0)
                return fur(c, i, j, 42, 0.1)
            return fur(p["head"], i, j, 43)
        if face == "top":
            c = p["head_dark"]
            if z < z_front + 2:
                c = lerp(c, p["forelock"], 0.6 if curl < 0.6 else 0.3)
            return fur(c, i, j, 44, 0.09)
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return EYE
            if abs(y - eye_y) < 0.6 and abs(z - eye_z - 1) < 0.6:
                return EYE_BROWN if face == "left" else EYE
            if abs(y - eye_y + 1) < 0.6 and abs(z - eye_z) < 0.6:
                return GLINT if face == "right" else shade(p["head_dark"], 0.9)
            c = p["head"]
            if abs(y - eye_y + 1) < 0.6 and abs(z - eye_z - 0.5) < 1.6:
                c = p["head_dark"]                          # brow
            if y > eye_y + 1.5:
                c = lerp(c, p["coat"], 0.25)                # jowl
            return fur(c, i, j, 45, 0.06)
        if face == "bottom":
            return fur(lerp(p["head"], p["belly"], 0.4), i, j, 46)
        return fur(p["head_dark"], i, j, 47)
    return mat


def muzzle(x_half, top, bottom, z_front, z_back):
    """Pale ring around a dark, moist nose plate; dark mouth line."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        ring = p["muzzle"]
        if face == "front":
            edge = abs(x) > x_half - 0.6 or y < top + 0.6
            if y > bottom - 1.0:
                return fur(ring, i, j, 50, 0.04)            # lower lip
            if y > bottom - 2.0:
                return shade(p["nose"], 1.3)                # mouth
            if edge:
                return fur(ring, i, j, 51, 0.04)
            if abs(abs(x) - x_half * 0.5) < 0.6 and y < top + 2:
                return shade(p["nose"], 0.6)                # nostrils
            return fur(p["nose"], i, j, 52, 0.1)
        front_t = (z_back - z) / (z_back - z_front)         # 0 at the skull .. 1 at the nose
        if face == "top":
            c = lerp(p["head"], ring, smooth((front_t - 0.55) / 0.3))
            return fur(c, i, j, 53, 0.05)
        if face == "bottom":
            return fur(lerp(ring, p["head"], 0.2 * (1 - front_t)), i, j, 54, 0.04)
        if face in ("left", "right"):
            if abs(y - (bottom - 1.5)) < 0.6 and front_t > 0.3:
                return shade(p["nose"], 1.4)                # mouth line
            c = lerp(p["head"], ring, smooth((front_t - 0.5) / 0.35))
            if y > bottom - 1.0:
                c = lerp(c, ring, 0.6)                      # pale lips
            return fur(c, i, j, 55, 0.05)
        return fur(p["head"], i, j, 56)
    return mat


def ear_mat(v, face, x, y, z, i, j):
    """Ear sticks out along x; -x is the outer tip (mirrored onto the left ear)."""
    p = P(v)
    if face == "front":
        rim = x < -1.0 or abs(y) > 0.6
        return fur(p["head"] if rim else INNER_EAR, i, j, 60, 0.06)
    if face == "right" or x < -1.0:
        return fur(p["head_dark"], i, j, 61)
    return fur(p["head"], i, j, 62)


def horn(t0, t1, length):
    """Gradient along the horn (t = 0 at the skull .. 1 at the tip): grey-cream base, cream middle, black tip."""
    def mat(v, face, x, y, z, i, j):
        t = t0 if face == "bottom" else t1 if face == "top" else t0 + (t1 - t0) * (-y / length)
        c = lerp(HORN_BASE, HORN_MID, t / 0.6) if t < 0.6 else lerp(HORN_MID, HORN_TIP, (t - 0.6) / 0.4)
        if t < 0.5 and h(i, j, 70) < 0.3:
            c = shade(c, 0.88)                              # growth rings near the base
        if face in ("front", "left") and t < 0.85:
            c = shade(c, 1.06)                              # sheen on one side
        return c
    return mat


def tail_mat(v, face, x, y, z, i, j):
    p = P(v)
    return fur(lerp(p["coat_dark"], p["tuft"], y / 10), i, j, 80)


def tuft_mat(v, face, x, y, z, i, j):
    p = P(v)
    c = p["tuft"] if h(i, j // 2, 81) > 0.3 else shade(p["tuft"], 1.6)
    return fur(c, i, j, 82, 0.1)


def leg_upper(top, bottom, hind):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)
        c = lerp(p["coat"] if hind else p["coat_dark"], p["leg"], t * 1.1)
        if face == "front":
            c = shade(c, 0.92)
        if face == "back" and hind and t < 0.4:
            c = lerp(c, p["coat_light"], 0.15)
        return fur(c, i, j, 90 + int(hind))
    return mat


def leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "bottom":
            return HOOF if abs(x) > 0.2 else shade(HOOF, 0.6)   # cloven hoof
        if face == "top":
            return p["leg"]
        if y > length - 1:
            return shade(HOOF, 1.0 + 0.2 * (h(i, j, 95) - 0.5))
        if face == "front" and y > length - 1 and abs(x) < 0.2:
            return shade(HOOF, 0.6)
        if face == "back" and abs(y - (length - 1.5)) < 0.6:
            return HOOF                                     # dewclaws
        return fur(lerp(p["leg"], p["head_dark"], y / length * 0.5), i, j, 96)
    return mat


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors AurochsModel.createBodyLayer
BOXES = {
    "body": (0, 0, -6, -5, -10, 12, 10, 20, barrel(-5, 5, -10, 10, 1.2)),
    "chest": (64, 0, -6.5, -7, -12, 13, 13, 9, chest(-7, 6, 1.2)),
    "hump": (64, 22, -4, -9, -11.5, 8, 3, 7, hump_mat),
    "tail": (60, 46, -0.5, 0, -0.5, 1, 10, 1, tail_mat),
    "tuft": (14, 46, -1, 9, -1, 2, 3, 2, tuft_mat),
    "neck": (0, 30, -3, -4, -7, 6, 8, 8, neck(-4, 4, -7, 1.0)),
    "dewlap": (0, 46, -1, 4, -6, 2, 3, 5, dewlap_mat),
    "skull": (28, 30, -3.5, -3.5, -5.5, 7, 7, 6, skull(-3.5, 3.5, -5.5, -1, -4)),
    "muzzle": (100, 32, -2.5, -1.5, -9.5, 5, 5, 4, muzzle(2.5, -1.5, 3.5, -9.5, -5.5)),
    "ear": (50, 46, -1.5, -0.5, -1, 3, 1, 2, ear_mat),
    "horn": (22, 46, -1, -4, -1, 2, 4, 2, horn(0.0, 0.4, 4)),
    "horn_mid": (30, 46, -1, -4, -1, 2, 4, 2, horn(0.4, 0.75, 4)),
    "cow_horn": (38, 46, -1, -3, -1, 2, 3, 2, horn(0.0, 0.55, 3)),
    "horn_tip": (46, 46, -0.5, -3, -0.5, 1, 3, 1, horn(0.72, 1.0, 3)),
    "front_upper": (54, 32, -2, -1, -2, 4, 7, 4, leg_upper(-1, 6, False)),
    "hind_upper": (70, 32, -2, -1, -2.5, 4, 7, 5, leg_upper(-1, 6, True)),
    "lower": (88, 32, -1.5, 0, -1.5, 3, 5, 3, leg_lower(5)),
}

# mirrors AurochsCalfModel.createBodyLayer
CALF_BOXES = {
    "body": (0, 0, -3, -3, -5.5, 6, 6, 11, barrel(-3, 3, -5.5, 5.5, 0.6)),
    "neck": (34, 0, -1.5, -2, -4, 3, 4, 5, neck(-2, 2, -4, 0.5)),
    "skull": (34, 9, -2.5, -2.5, -4, 5, 5, 4, skull(-2.5, 2.5, -4, -0.5, -3)),
    "muzzle": (50, 0, -1.5, -1, -6.5, 3, 3, 3, muzzle(1.5, -1, 2, -6.5, -3.5)),
    "ear": (52, 6, -1.5, -0.5, -1, 3, 1, 2, ear_mat),
    "tail": (52, 9, -0.5, 0, -0.5, 1, 6, 1, tuft_mat),
    "front_upper": (0, 17, -1.5, -1, -1.5, 3, 4, 3, leg_upper(-1, 3, False)),
    "lower": (12, 17, -1, 0, -1, 2, 4, 2, leg_lower(4)),
    "hind_upper": (20, 17, -1.5, -1, -2, 3, 4, 4, leg_upper(-1, 3, True)),
}
HORNS = ("horn", "horn_mid", "cow_horn", "horn_tip")


def main():
    for variant in ("bull", "cow"):
        img = paint(BOXES, variant, (128, 64))
        save(img, f"wildlife/aurochs_{variant}.png")
        save(skinned(img, BOXES, HORNS), f"skinned/wildlife/aurochs_{variant}.png")
    # the young's skin now comes with its model from gen_herd_models.py (the *_BOXES table above is unused)


if __name__ == "__main__":
    main()
