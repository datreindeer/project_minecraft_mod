"""Paint the wild horse textures for WildHorseModel (128x64) and WildHorseFoalModel (64x32), plus the skinned (flesh)
version of the adult for butchered carcasses.

Usage: python scripts/gen_wild_horse_texture.py
Deterministic. BOXES / FOAL_BOXES must match the cubes in the two models (texOffs, origin, size).
Look (Przewalski / tarpan dun): yellow-dun body with a dark dorsal stripe from mane to tail, pale cream belly
("pangare"), mealy pale muzzle and eye rings, dark erect mane with paler sides, dark legs with faint zebra bars above
the knees, dark tail. The foal is paler and creamier with a short dark brush mane.
"""
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth

PALETTES = {
    "adult": dict(coat=(190, 146, 92), coat_dark=(160, 118, 72), coat_light=(212, 172, 118), belly=(232, 214, 180),
                  stripe=(70, 50, 34), mane=(42, 32, 26), mane_light=(176, 146, 106), leg=(46, 36, 30),
                  bar=(120, 90, 60), mealy=(228, 210, 176), nose=(64, 52, 46), face=(176, 132, 84)),
    "foal": dict(coat=(210, 180, 132), coat_dark=(190, 158, 112), coat_light=(226, 200, 156), belly=(238, 226, 200),
                 stripe=(120, 90, 62), mane=(80, 60, 44), mane_light=(196, 170, 130), leg=(150, 120, 90),
                 bar=(170, 138, 100), mealy=(236, 224, 198), nose=(110, 92, 80), face=(198, 166, 120)),
}
EYE, EYE_BROWN, GLINT = (16, 12, 11), (58, 38, 26), (196, 200, 208)
HOOF, HOOF_LIGHT = (52, 48, 46), (78, 72, 66)
INNER_EAR = (200, 170, 140)


def P(v):
    return PALETTES[v]


def dorsal(p, c, x, width):
    return lerp(c, p["stripe"], 0.85) if abs(x) < width else c


def body(top, bottom, stripe_w):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)
        if face == "bottom":
            return fur(p["belly"], i, j, 11, 0.04)
        if face == "top":
            c = lerp(p["coat_dark"], p["coat"], abs(x) / 4)
            return fur(dorsal(p, c, x, stripe_w), i, j, 12, 0.05)
        # darker top line, bright flank, pale belly from ~70% down
        if t < 0.2:
            c = lerp(p["coat_dark"], p["coat"], t / 0.2)
        elif t < 0.65:
            c = lerp(p["coat"], p["coat_light"], (t - 0.2) / 0.45 * 0.6)
        else:
            c = lerp(lerp(p["coat"], p["coat_light"], 0.6), p["belly"], smooth((t - 0.65) / 0.3))
        if face == "back" and abs(x) < stripe_w and t < 0.3:
            c = lerp(c, p["stripe"], 0.8)                      # stripe runs down to the dock
        if face in ("left", "right"):
            if z < -6 and t < 0.5:
                c = lerp(c, p["coat_dark"], 0.25)              # faint shoulder shading
            if abs(z - 5.5) < 3 and t < 0.55:
                c = lerp(c, p["coat_light"], 0.2 * (1 - abs(z - 5.5) / 3))   # rounded quarters
        return fur(c, i, j, 13, 0.05)
    return mat


def neck(top, bottom, z_front, z_back):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)                         # 0 at the poll .. 1 at the withers
        if face == "front":                                    # throat, paler
            return fur(lerp(p["coat"], p["belly"], 0.35), i, j, 21, 0.05)
        if face == "back":                                     # crest under the mane
            return fur(p["mane"], i, j, 22, 0.08)
        if face in ("top", "bottom"):
            return fur(p["coat_dark"], i, j, 23, 0.05)
        crest = (z - z_front) / (z_back - z_front)             # 0 throat .. 1 crest
        c = lerp(p["coat_light"], p["coat_dark"], crest * 0.8)
        if crest > 0.85:
            c = lerp(c, p["mane"], 0.6)                        # mane roots
        c = lerp(c, p["face"], (1 - t) * 0.3)
        return fur(c, i, j, 24, 0.05)
    return mat


def mane_mat(v, face, x, y, z, i, j):
    """Erect mane: dark core, paler fawn hair on the sides, ragged tips."""
    p = P(v)
    if face in ("left", "right"):
        outer = z > 3.3 if v == "adult" else z > 1.5
        c = p["mane"] if not outer or h(i, j, 31) < 0.45 else lerp(p["mane"], p["mane_light"], 0.7)
        return fur(c, i, j, 32, 0.12)
    if face == "back" and h(i, j, 33) < 0.3:
        return fur(p["mane_light"], i, j, 34, 0.1)
    return fur(p["mane"], i, j, 35, 0.12)


def skull(top, bottom, z_front, eye_y, eye_z):
    """front = face (the head hangs nose-down), top = poll, eyes on the sides with pale rings."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "front":
            return fur(lerp(p["face"], p["coat_dark"], 0.2), i, j, 41, 0.05)
        if face == "top":
            return fur(lerp(p["coat_dark"], p["mane"], 0.3), i, j, 42, 0.06)
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return EYE
            if abs(y - eye_y) < 0.6 and abs(z - eye_z - 1) < 0.6:
                return GLINT if face == "left" else EYE_BROWN
            c = p["face"]
            if abs(y - eye_y) < 1.6 and eye_z - 1.6 < z < eye_z + 2.6:
                c = lerp(c, p["mealy"], 0.55)                  # pale eye ring
            if y > eye_y + 1.5:
                c = lerp(c, p["coat_light"], 0.3)              # cheek
            return fur(c, i, j, 43, 0.05)
        if face == "bottom":
            return fur(lerp(p["face"], p["belly"], 0.4), i, j, 44, 0.04)
        return fur(p["coat_dark"], i, j, 45)
    return mat


def muzzle(x_half, top, bottom, z_front, z_back):
    """Mealy pale muzzle, darker skin around the nostrils and lips."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        k = (z_back - z) / (z_back - z_front)                  # 0 at the skull .. 1 at the nose
        if face == "front":
            if y > bottom - 1.0:
                return fur(lerp(p["nose"], p["mealy"], 0.4), i, j, 50, 0.05)   # lower lip
            if y > bottom - 2.0:
                return shade(p["nose"], 0.7)                   # mouth
            if abs(abs(x) - x_half * 0.6) < 0.6 and y < top + 1.6:
                return shade(p["nose"], 0.55)                  # nostrils
            return fur(lerp(p["mealy"], p["nose"], 0.35), i, j, 51, 0.05)
        if face == "top":
            return fur(lerp(p["face"], p["mealy"], smooth((k - 0.4) / 0.5)), i, j, 52, 0.05)
        if face == "bottom":
            return fur(lerp(p["mealy"], p["nose"], 0.25 * k), i, j, 53, 0.04)
        if face in ("left", "right"):
            if k > 0.75 and abs(y - (top + 1)) < 0.6:
                return shade(p["nose"], 0.65)                  # nostril wrap
            if abs(y - (bottom - 1.5)) < 0.6 and k > 0.5:
                return shade(p["nose"], 0.8)                   # mouth line
            c = lerp(p["face"], p["mealy"], smooth((k - 0.3) / 0.5))
            return fur(c, i, j, 54, 0.05)
        return fur(p["face"], i, j, 55)
    return mat


def ear_mat(v, face, x, y, z, i, j):
    p = P(v)
    if face == "front":
        rim = abs(x) > 0.6 or y < -2.4
        return p["mane"] if rim else fur(INNER_EAR, i, j, 60, 0.05)
    if y < -2.0:
        return fur(p["mane"], i, j, 61, 0.05)                  # dark tips
    return fur(lerp(p["coat_dark"], p["mane"], 0.3), i, j, 62)


def tail_dock(v, face, x, y, z, i, j):
    p = P(v)
    if face in ("top", "back") or abs(x) < 0.6 and face == "back":
        return fur(p["stripe"], i, j, 70, 0.06)
    return fur(lerp(p["coat_dark"], p["mane"], 0.5 + y / 8), i, j, 71, 0.08)


def tail_hair(v, face, x, y, z, i, j):
    p = P(v)
    strand = h(i, j // 3, 72)
    c = p["mane"] if strand > 0.25 else lerp(p["mane"], p["mane_light"], 0.35)
    return fur(c, i, j, 73, 0.12)


def leg_upper(top, bottom, hind):
    """Dun at the top, dark from the knee / hock down, with faint zebra bars in between (strong dun-factor look)."""
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        t = (y - top) / (bottom - top)
        if face == "top":
            return p["coat"]
        if t > 0.7:
            c = p["leg"]
        else:
            c = lerp(p["coat"], p["bar"], t / 0.7)
            if face in ("left", "right", "front") and int(y * 1.5) % 2 == 0 and t > 0.25:
                c = lerp(c, p["leg"], 0.45)                    # leg barring
        if hind and face == "back" and t < 0.4:
            c = lerp(c, p["belly"], 0.25)
        return fur(c, i, j, 90 + int(hind), 0.05)
    return mat


def leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = P(v)
        if face == "bottom":
            return HOOF if abs(x) + abs(z) > 0.3 else shade(HOOF, 0.7)
        if face == "top":
            return p["leg"]
        if y > length - 1:
            return HOOF_LIGHT if face == "front" and h(i, j, 95) < 0.3 else HOOF
        if y > length - 2 and face == "back":
            return fur(shade(p["leg"], 1.2), i, j, 96, 0.15)  # fetlock tuft
        return fur(p["leg"], i, j, 97, 0.06)
    return mat


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors WildHorseModel.createBodyLayer
BOXES = {
    "body": (0, 0, -4.5, -5, -10, 9, 10, 20, body(-5, 5, 0.6)),
    "tail_dock": (114, 0, -1, 0, -1, 2, 4, 2, tail_dock),
    "tail_hair": (108, 6, -1.5, 3, -1.5, 3, 9, 3, tail_hair),
    "neck": (58, 0, -2, -9, -3, 4, 10, 6, neck(-9, 1, -3, 3)),
    "mane": (78, 0, -1, -10, 2.5, 2, 10, 2, mane_mat),
    "skull": (86, 0, -2.5, -2.5, -5, 5, 5, 6, skull(-2.5, 2.5, -5, -1, -2.5)),
    "muzzle": (86, 11, -2, -1.5, -11, 4, 4, 6, muzzle(2, -1.5, 2.5, -11, -5)),
    "ear": (108, 0, -1, -3, -0.5, 2, 3, 1, ear_mat),
    "front_upper": (0, 30, -1.5, -1, -1.5, 3, 6, 3, leg_upper(-1, 5, False)),
    "hind_upper": (12, 30, -1.5, -1, -2, 3, 6, 4, leg_upper(-1, 5, True)),
    "lower": (26, 30, -1, 0, -1, 2, 5, 2, leg_lower(5)),
}

# mirrors WildHorseFoalModel.createBodyLayer
FOAL_BOXES = {
    "body": (0, 0, -2.5, -2.5, -5, 5, 5, 10, body(-2.5, 2.5, 0.6)),
    "neck": (30, 0, -1.5, -6, -1.5, 3, 7, 3, neck(-6, 1, -1.5, 1.5)),
    "mane": (42, 0, -0.5, -6.5, 1, 1, 6, 1, mane_mat),
    "skull": (46, 0, -2, -2, -3.5, 4, 4, 4, skull(-2, 2, -3.5, -0.5, -2)),
    "muzzle": (30, 10, -1.5, -1, -7.5, 3, 3, 4, muzzle(1.5, -1, 2, -7.5, -3.5)),
    "ear": (46, 8, -1, -3, -0.5, 2, 3, 1, ear_mat),
    "tail": (52, 8, -0.5, 0, -0.5, 1, 5, 1, tail_hair),
    "front_upper": (0, 15, -1, -1, -1, 2, 5, 2, leg_upper(-1, 4, False)),
    "lower": (8, 15, -0.5, 0, -0.5, 1, 6, 1, leg_lower(6)),
    "hind_upper": (12, 15, -1, -1, -1.5, 2, 5, 3, leg_upper(-1, 4, True)),
}


def main():
    img = paint(BOXES, "adult", (128, 64))
    save(img, "wildlife/wild_horse.png")
    save(skinned(img, BOXES, ("lower",)), "skinned/wildlife/wild_horse.png")   # lower legs and hooves keep their hide
    save(paint(FOAL_BOXES, "foal", (64, 32)), "wildlife/wild_horse_foal.png")


if __name__ == "__main__":
    main()
