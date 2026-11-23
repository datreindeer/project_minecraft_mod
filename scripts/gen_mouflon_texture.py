"""Paint the mouflon textures for MouflonModel (ram, ewe; 64x64) and MouflonLambModel (lamb; 64x32), plus the skinned
(flesh) versions of the adults for butchered carcasses.

Usage: python scripts/gen_mouflon_texture.py
Deterministic. BOXES / LAMB_BOXES must match the cubes in the two models (texOffs, origin, size).
Look: ram rich reddish-brown with a pale saddle patch on each flank, a dark band low on the flank above the white
belly, white rump, white lower legs, white muzzle and eye rings, dark throat ruff, ridged grey-brown horns. Ewe
plainer and greyer-fawn without the saddle. Lamb soft fawn with a white belly.
"""
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth

PALETTES = {
    "ram": dict(coat=(142, 78, 44), coat_dark=(104, 56, 32), coat_light=(168, 100, 60), band=(46, 32, 24),
                saddle=(222, 210, 186), white=(236, 232, 222), head=(118, 92, 72), head_dark=(78, 60, 48),
                ruff=(52, 38, 30), leg_stripe=(60, 44, 34), nose=(70, 58, 52)),
    "ewe": dict(coat=(150, 108, 74), coat_dark=(120, 86, 60), coat_light=(170, 128, 90), band=(104, 76, 56),
                saddle=None, white=(234, 228, 216), head=(132, 104, 82), head_dark=(98, 76, 60),
                ruff=(112, 84, 62), leg_stripe=(96, 72, 54), nose=(82, 68, 60)),
    "lamb": dict(coat=(164, 124, 88), coat_dark=(140, 104, 74), coat_light=(184, 146, 108), band=(126, 94, 68),
                 saddle=None, white=(238, 232, 220), head=(150, 118, 90), head_dark=(118, 92, 72),
                 ruff=(140, 104, 76), leg_stripe=(120, 92, 70), nose=(110, 92, 82)),
}
EYE, EYE_GOLD, GLINT = (16, 12, 10), (150, 112, 46), (200, 204, 210)
INNER_EAR = (206, 178, 160)
HOOF = (40, 36, 34)
HORN_BASE, HORN_MID, HORN_TIP = (112, 96, 76), (146, 128, 100), (182, 166, 136)


def P(v):
    return PALETTES[v]


def body(top, bottom, z_front, z_back):
    """Back -> red flank (with the ram's saddle patch) -> dark band -> white belly; white rump around the tail."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)
        if face == "bottom":
            return fur(p["white"], i, j, 11, 0.03)
        if face == "top":
            c = lerp(p["coat_dark"], p["coat"], abs(x) / (top * -1) * 0.8 + 0.2)
            if z > z_back - 1.5:
                c = lerp(c, p["white"], 0.4)                    # rump patch creeps over the croup
            return fur(c, i, j, 12)
        if face == "back":                                      # white rump patch, ringed darker
            d = (x / (abs(top) * 0.85)) ** 2 + ((y - 0.5) / (abs(top) * 0.9)) ** 2
            if d < 0.75 or t > 0.8:
                return fur(p["white"], i, j, 13, 0.03)
            if d < 1.0:
                return fur(p["band"], i, j, 14, 0.06)
            return fur(p["coat_dark"], i, j, 15)
        if face == "front":
            return fur(lerp(p["coat_dark"], p["ruff"], 0.5), i, j, 16)
        # sides
        if t > 0.82:
            return fur(p["white"], i, j, 17, 0.03)
        if t > 0.66:
            return fur(p["band"], i, j, 18, 0.06)              # dark flank band
        c = lerp(p["coat_dark"], p["coat"], t / 0.25) if t < 0.25 else lerp(p["coat"], p["coat_light"], (t - 0.25) / 0.4 * 0.5)
        mid = (z_front + z_back) / 2 + (z_back - z_front) * 0.08
        if p["saddle"]:                                         # pale oval saddle patch mid-flank
            d = ((z - mid) / ((z_back - z_front) * 0.26)) ** 2 + ((t - 0.4) / 0.27) ** 2
            if d < 1.0:
                c = lerp(c, p["saddle"], smooth((1.0 - d) / 0.35))
        if z > z_back - 1.5 and t > 0.3:
            c = lerp(c, p["white"], 0.6)                        # rump patch wraps the hip
        if z < z_front + 2 and t > 0.4:
            c = lerp(c, p["band"], 0.35)                        # dark shoulder line
        return fur(c, i, j, 19, 0.07)
    return mat


def neck(top, bottom, z_front, z_back):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)                          # 0 at the head .. 1 at the shoulders
        if face == "front":                                     # throat: the ram's dark ruff, ragged
            c = lerp(p["head"], p["ruff"], smooth(t * 1.5))
            if v == "ram" and h(i, j // 2, 21) < 0.3:
                c = shade(c, 0.8)
            return fur(c, i, j, 22, 0.09)
        if face in ("top", "bottom"):
            return fur(p["head"], i, j, 23)
        crest = (z - z_front) / (z_back - z_front)
        c = lerp(lerp(p["head"], p["coat"], t), p["coat_dark"], crest * 0.4)
        if crest < 0.3:
            c = lerp(c, p["ruff"], (0.3 - crest) * 2 * t)       # ruff wraps the sides of the throat
        return fur(c, i, j, 24)
    return mat


def skull(top, bottom, z_front, eye_y, eye_z):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "front":                                     # face: darker brow, lighter lower
            c = p["head_dark"] if y < top + 1.5 else p["head"]
            return fur(c, i, j, 41, 0.05)
        if face == "top":
            return fur(p["head_dark"], i, j, 42, 0.06)
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return EYE_GOLD if face == "left" else EYE      # sheep eyes: golden with a dark pupil
            if abs(y - eye_y) < 0.6 and abs(z - eye_z - 1) < 0.6:
                return EYE if face == "left" else EYE_GOLD
            c = p["head"]
            if abs(y - eye_y) < 1.6 and eye_z - 1.6 < z < eye_z + 2.6:
                c = lerp(c, p["white"], 0.6)                    # pale eye ring
            if y > eye_y + 1.5:
                c = lerp(c, p["white"], 0.25)
            return fur(c, i, j, 43, 0.05)
        if face == "bottom":
            return fur(lerp(p["head"], p["white"], 0.5), i, j, 44, 0.04)
        return fur(p["head_dark"], i, j, 45)
    return mat


def muzzle(x_half, top, bottom):
    """White muzzle, dark nose and split lip."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "front":
            if y < top + 1.0:
                return shade(p["nose"], 0.8) if abs(x) > 0.4 else p["nose"]
            if abs(x) < 0.6 and y < bottom - 0.5:
                return p["nose"]                                # philtrum
            return fur(p["white"], i, j, 50, 0.03)
        if face == "top":
            return fur(lerp(p["head"], p["white"], 0.35), i, j, 51, 0.05)
        if face in ("left", "right"):
            if abs(y - (bottom - 1)) < 0.6 and z < -5:
                return shade(p["nose"], 1.2)                    # mouth line
            return fur(lerp(p["head"], p["white"], 0.7 if y > top + 0.5 else 0.45), i, j, 52, 0.04)
        return fur(p["white"], i, j, 53, 0.03)
    return mat


def ear_mat(v, face, x, y, z, i, j):
    """Ear sticks out along x; -x is the outer tip (mirrored onto the left ear)."""
    p = P(v)
    if face == "front":
        return fur(INNER_EAR if x > -1.0 else p["head_dark"], i, j, 60, 0.05)
    return fur(p["head_dark"] if x < -0.8 else p["head"], i, j, 61)


def horn(t0, t1, length):
    """Ridged horn: transverse ridges every texel row, darker growth rings, lighter towards the tip."""
    def mat(v, face, x, y, z, i, j):
        t = t0 if face == "bottom" else t1 if face == "top" else t0 + (t1 - t0) * (-y / length)
        c = lerp(HORN_BASE, HORN_MID, t / 0.5) if t < 0.5 else lerp(HORN_MID, HORN_TIP, (t - 0.5) / 0.5)
        if face not in ("top", "bottom"):
            c = shade(c, 0.86 if j % 2 else 1.04)               # ridges
            if h(0, int(t * 9), 70) < 0.25:
                c = shade(c, 0.8)                               # annual ring
        if face == "right":
            c = shade(c, 0.92)                                  # inner side, against the head
        return c
    return mat


def tail_mat(v, face, x, y, z, i, j):
    p = P(v)
    if face in ("front", "bottom"):
        return fur(p["white"], i, j, 80, 0.03)
    return fur(lerp(p["coat_dark"], p["band"], y / 3), i, j, 81)


def leg_upper(top, bottom, hind):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)
        if face == "top":
            return p["coat"]
        if hind:
            c = lerp(p["coat"], p["white"], smooth((t - 0.3) / 0.5))
            if face == "back":
                c = p["white"]                                  # rump patch runs down the back of the thigh
        else:
            c = lerp(p["band"] if t < 0.3 else p["coat_dark"], p["white"], smooth((t - 0.4) / 0.5))
        if face == "front" and t > 0.2:
            c = lerp(c, p["leg_stripe"], 0.7)                   # dark stripe down the front of the leg
        return fur(c, i, j, 90 + int(hind), 0.05)
    return mat


def leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "bottom":
            return HOOF if abs(x) > 0.2 else shade(HOOF, 0.6)
        if face == "top":
            return p["white"]
        if y > length - 1:
            return shade(HOOF, 1.0 + 0.2 * (h(i, j, 95) - 0.5))
        if face == "front" and y < length * 0.5:
            return fur(p["leg_stripe"], i, j, 96, 0.06)         # stripe fades out at the fetlock
        return fur(p["white"], i, j, 97, 0.03)
    return mat


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors MouflonModel.createBodyLayer
BOXES = {
    "body": (0, 0, -3.5, -3.5, -7, 7, 7, 14, body(-3.5, 3.5, -7, 7)),
    "tail": (56, 2, -1, 0, -0.5, 2, 3, 1, tail_mat),
    "neck": (42, 0, -1.5, -6, -2, 3, 7, 4, neck(-6, 1, -2, 2)),
    "skull": (42, 11, -2, -2, -4, 4, 4, 5, skull(-2, 2, -4, -0.5, -2.5)),
    "muzzle": (0, 21, -1.5, -1, -7, 3, 3, 3, muzzle(1.5, -1, 2)),
    "ear": (56, 0, -1.5, -0.5, -0.5, 3, 1, 1, ear_mat),
    "horn_big": (12, 21, -1.5, -3, -1.5, 3, 3, 3, horn(0.0, 0.45, 3)),
    "horn_mid": (24, 21, -1, -3, -1, 2, 3, 2, horn(0.45, 0.75, 3)),
    "horn_tip": (32, 21, -0.5, -3, -0.5, 1, 3, 1, horn(0.75, 1.0, 3)),
    "ewe_horn": (36, 21, -0.5, -2, -0.5, 1, 2, 1, horn(0.2, 0.9, 2)),
    "front_upper": (0, 27, -1.5, -1, -1.5, 3, 5, 3, leg_upper(-1, 4, False)),
    "hind_upper": (12, 27, -1.5, -1, -2, 3, 5, 4, leg_upper(-1, 4, True)),
    "lower": (26, 27, -1, 0, -1, 2, 6, 2, leg_lower(6)),
}

# mirrors MouflonLambModel.createBodyLayer
LAMB_BOXES = {
    "body": (0, 0, -2, -2, -4, 4, 4, 8, body(-2, 2, -4, 4)),
    "neck": (24, 0, -1, -3, -1, 2, 4, 2, neck(-3, 1, -1, 1)),
    "skull": (32, 0, -1.5, -1.5, -3, 3, 3, 4, skull(-1.5, 1.5, -3, -0.5, -1.5)),
    "muzzle": (46, 0, -1, -0.5, -5, 2, 2, 2, muzzle(1, -0.5, 1.5)),
    "ear": (54, 0, -1, -0.5, -0.5, 2, 1, 1, ear_mat),
    "tail": (54, 2, -0.5, 0, -0.5, 1, 2, 1, tail_mat),
    "front_upper": (0, 12, -1, -1, -1, 2, 3, 2, leg_upper(-1, 2, False)),
    "lower": (8, 12, -0.5, 0, -0.5, 1, 5, 1, leg_lower(5)),
    "hind_upper": (12, 12, -1, -1, -1.5, 2, 3, 3, leg_upper(-1, 2, True)),
}
HORNS = ("horn_big", "horn_mid", "horn_tip", "ewe_horn")


def main():
    for variant in ("ram", "ewe"):
        img = paint(BOXES, variant, (64, 64))
        save(img, f"wildlife/mouflon_{variant}.png")
        save(skinned(img, BOXES, HORNS), f"skinned/wildlife/mouflon_{variant}.png")
    # the young's skin now comes with its model from gen_herd_models.py (the *_BOXES table above is unused)


if __name__ == "__main__":
    main()
