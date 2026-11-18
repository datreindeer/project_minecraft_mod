"""Paint the small forest animals: squirrel, hedgehog and badger (entity textures + skinned versions), plus their item
textures (spawn eggs, squirrel pelt).

Usage: python scripts/gen_forest_critter_textures.py [--java]
The part trees below mirror SquirrelModel / HedgehogModel / BadgerModel (pivot, rotation, boxes, UVs from pack());
--java prints the cube lines to check the Java models against. Deterministic: hashes only, no randomness.
"""
import sys

from critter_paint import cube, fur, h, java_cubes, lerp, pack, paint, part, save, shade, skinned, smooth

# ======================================================================= squirrel
# Red squirrel: rufous back and flanks, cream belly and chest, a pale eye ring, dark-tipped ear tufts, a big bushy tail
# with paler hair tips along its edges.
SQ = dict(back=(124, 58, 30), coat=(168, 82, 38), light=(198, 110, 54), cream=(238, 226, 204), white=(248, 242, 230),
          tuft=(84, 38, 22), tail=(146, 66, 32), tail_tip=(206, 132, 72), eye=(16, 10, 8), glint=(232, 232, 236),
          ring=(214, 176, 128), nose=(62, 40, 34), claw=(46, 34, 28), sole=(110, 74, 54))


def sq_body(face, X, Y, Z, n, tx, ty):
    """Back -> rufous flank -> cream belly; the cream starts higher on the chest."""
    if face == "bottom" or n[1] > 0.7:
        return fur(SQ["cream"], tx, ty, 1, 0.04)
    belly_line = 20.6 if Z > -1.5 else 19.8          # model y where the cream begins
    if Y > belly_line:
        return fur(lerp(SQ["cream"], SQ["light"], 0.15 if Y < belly_line + 0.6 else 0), tx, ty, 2, 0.05)
    t = smooth((Y - 18.5) / (belly_line - 18.5))
    c = lerp(SQ["back"], SQ["coat"], t * 1.4)
    if Y > belly_line - 0.7:
        c = lerp(c, SQ["light"], 0.5)
    if abs(X) < 0.6 and face == "top":
        c = shade(c, 0.9)                               # darker line along the spine
    return fur(c, tx, ty, 3, 0.09)


def sq_head(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right"):
        ey, ez = 18.5, -4.5
        if abs(Y - ey) < 0.5 and abs(Z - ez) < 0.5:
            return SQ["eye"]
        if abs(Y - ey) < 0.5 and abs(Z - (ez + 1)) < 0.5:
            return lerp(SQ["eye"], SQ["ring"], 0.3)       # eye is two texels: dark, with a dark rim behind
        if abs(Y - ey) < 1.0 and -5.6 < Z < -3.0 and h(tx, ty, 40) < 0.8:
            return fur(SQ["ring"], tx, ty, 4, 0.05)      # pale eye ring
        if Y > 19.2:
            return fur(SQ["cream"], tx, ty, 5, 0.04)     # cheeks / throat
        return fur(lerp(SQ["coat"], SQ["light"], 0.3), tx, ty, 6)
    if face == "bottom":
        return fur(SQ["cream"], tx, ty, 7, 0.03)
    if face == "front":
        if Y > 19.3:
            return fur(SQ["cream"], tx, ty, 8, 0.04)
        return fur(SQ["light"], tx, ty, 9)
    if face == "top":
        return fur(lerp(SQ["back"], SQ["coat"], 0.4), tx, ty, 10)
    return fur(SQ["coat"], tx, ty, 11)


def sq_snout(face, X, Y, Z, n, tx, ty):
    if face == "front":
        if Y < 18.9:
            return SQ["nose"] if abs(X) < 0.6 else lerp(SQ["nose"], SQ["light"], 0.5)
        if abs(X) < 0.6:
            return shade(SQ["cream"], 0.8)               # split lip
        return SQ["white"]
    if face == "top":
        return fur(SQ["light"], tx, ty, 12, 0.05)
    if Y > 19.0:
        return fur(SQ["cream"], tx, ty, 13, 0.03)
    return fur(SQ["light"], tx, ty, 14, 0.05)


def sq_ear(face, X, Y, Z, n, tx, ty):
    """Rounded ear with the long winter tuft: dark tip."""
    if Y < 15.9:
        return fur(SQ["tuft"], tx, ty, 15, 0.08)
    if face == "front":
        return fur(lerp(SQ["coat"], SQ["cream"], 0.5), tx, ty, 16, 0.05)
    return fur(SQ["coat"], tx, ty, 17)


def sq_leg(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return SQ["sole"]
    if Y > 23.3 and face == "front":
        return SQ["claw"] if (tx % 2 == 0) else SQ["sole"]
    if face in ("back", "front") and Y < 22.5:
        return fur(SQ["cream"] if face == "back" else SQ["light"], tx, ty, 18, 0.05)
    return fur(lerp(SQ["coat"], SQ["light"], 0.3), tx, ty, 19)


def sq_haunch(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or Y > 21.4:
        return fur(lerp(SQ["light"], SQ["cream"], 0.4), tx, ty, 20, 0.05)
    return fur(lerp(SQ["back"], SQ["coat"], smooth((Y - 19) / 2)), tx, ty, 21)


def sq_foot(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return SQ["sole"]
    if face == "front" and Y > 23.2:
        return SQ["claw"] if tx % 2 == 0 else SQ["sole"]
    return fur(lerp(SQ["coat"], SQ["back"], 0.3), tx, ty, 22)


def sq_tail(core):
    """Bushy tail: darker core, long paler hair tips at the edges (x far from 0 and the outer face)."""
    def mat(face, X, Y, Z, n, tx, ty):
        edge = abs(X) > core or face in ("left", "right")
        c = SQ["tail"]
        if edge:
            c = lerp(c, SQ["tail_tip"], 0.55 + 0.3 * h(tx, ty, 23))
        else:
            c = lerp(c, SQ["back"], 0.3 * h(tx, ty // 2, 24))
        if h(tx, ty, 25) < 0.12:
            c = lerp(c, SQ["tail_tip"], 0.6)              # stray light hairs
        return fur(c, tx, ty, 26, 0.1)
    return mat


SQUIRREL = [
    part("body", None, (0, 20, 2), (0, 0, 0), [cube("sq_body", -1.5, -1.5, -5, 3, 3, 5, sq_body)]),
    part("head", "body", (0, -0.5, -5), (0, 0, 0), [cube("sq_head", -1.5, -2.5, -2.5, 3, 3, 3, sq_head),
                                                   cube("sq_snout", -1, -1.5, -3.5, 2, 2, 1, sq_snout)]),
    part("right_ear", "head", (-1, -2.5, -1), (0, 0, -0.15), [cube("sq_ear", -0.5, -2, -0.5, 1, 2, 1, sq_ear)]),
    part("left_ear", "head", (1, -2.5, -1), (0, 0, 0.15), [cube("sq_ear", -0.5, -2, -0.5, 1, 2, 1, sq_ear, True)]),
    part("right_front_leg", "body", (-1, 1, -4), (0, 0, 0), [cube("sq_leg", -0.5, 0, -0.5, 1, 3, 1, sq_leg)]),
    part("left_front_leg", "body", (1, 1, -4), (0, 0, 0), [cube("sq_leg", -0.5, 0, -0.5, 1, 3, 1, sq_leg, True)]),
    part("right_haunch", "body", (-1.5, 0.5, 0.5), (0, 0, 0), [cube("sq_haunch", -0.5, -1.5, -2, 1, 3, 3, sq_haunch)]),
    part("right_hind_foot", "right_haunch", (0, 1.5, 0), (0, 0, 0), [cube("sq_foot", -0.5, 0, -2.5, 1, 2, 3, sq_foot)]),
    part("left_haunch", "body", (1.5, 0.5, 0.5), (0, 0, 0), [cube("sq_haunch", -0.5, -1.5, -2, 1, 3, 3, sq_haunch, True)]),
    part("left_hind_foot", "left_haunch", (0, 1.5, 0), (0, 0, 0), [cube("sq_foot", -0.5, 0, -2.5, 1, 2, 3, sq_foot, True)]),
    part("tail", "body", (0, -0.5, -0.5), (1.0, 0, 0), [cube("sq_tail1", -1, -1, 0, 2, 2, 4, sq_tail(0.6))]),
    part("tail_mid", "tail", (0, 0, 3.5), (0.9, 0, 0), [cube("sq_tail2", -1.5, -1.5, 0, 3, 3, 5, sq_tail(0.9))]),
    part("tail_tip", "tail_mid", (0, 0, 4.5), (0.9, 0, 0), [cube("sq_tail3", -1.5, -1.5, 0, 3, 3, 3, sq_tail(0.9))]),
]
SQUIRREL_SIZE = (64, 32)


# ======================================================================= hedgehog
# Spines banded dark brown with cream tips (speckled, in rows), a parting over the forehead; face, belly and legs soft
# grey-brown fur, darker mask round the eyes, shiny black nose. Planes of spines stick out past the body for a ragged
# silhouette (sides, crest, back and a crown over the brow).
HH = dict(base=(58, 44, 34), mid=(108, 86, 64), tip=(226, 210, 176), fur=(142, 114, 88), fur_dark=(102, 80, 60),
          mask=(78, 58, 44), light=(186, 164, 134), nose=(22, 18, 18), shine=(120, 110, 108), eye=(10, 8, 8),
          leg=(70, 54, 42), claw=(196, 182, 160))


def spine(tx, ty, salt=0):
    """One spine texel: dark-banded quill, most with a cream tip; tips line up loosely in rows so it reads as layered quills."""
    n = h(tx, ty, 60 + salt)
    k = (tx + 2 * ty + int(n * 2.2)) % 3                 # diagonal rows of quills, jittered
    if k == 0:
        return lerp(HH["tip"], HH["light"], h(tx, ty, 61) * 0.6)
    if k == 1:
        return lerp(HH["mid"], HH["base"], h(tx, ty, 62) * 0.5)
    return lerp(HH["base"], (34, 26, 22), h(tx, ty, 63) * 0.5)


def hh_mantle(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return fur(HH["fur"], tx, ty, 64, 0.06)
    if face == "front" and (Y > 20.4 or (abs(X) < 0.6 and Y > 19.0)):
        return fur(HH["fur_dark"], tx, ty, 65)            # fur below the spine line, the parting over the brow
    if face in ("left", "right") and Y > 21.4:
        return fur(HH["fur"], tx, ty, 66)                  # skirt of fur along the flank
    return spine(tx, ty)


def hh_dome(face, X, Y, Z, n, tx, ty):
    return spine(tx, ty, 1)


def hh_belly(face, X, Y, Z, n, tx, ty):
    return fur(lerp(HH["fur"], HH["light"], 0.25), tx, ty, 67, 0.06)


def plane_spines(inside, salt):
    """Zero-width spine plane: opaque spine texels where inside(X, Y, Z, jitter) says, transparent elsewhere."""
    def mat(face, X, Y, Z, n, tx, ty):
        if not inside(X, Y, Z, h(tx // 1, ty, salt) - 0.5):
            return (0, 0, 0, 0)
        return spine(tx, ty, salt)
    return mat


def side_profile(X, Y, Z, j):
    # dome over the back, a little past the mantle; ragged by j
    top = 16.8 + 2.6 * (Z / 5.0) ** 2 + 0.7 * j
    return top < Y < 21.2 + 0.4 * j and abs(Z) < 4.8 + 0.3 * j


def crest_profile(X, Y, Z, j):
    return Y > 16.4 + 1.6 * (Z / 4.5) ** 2 + 0.8 * abs(j) and abs(Z) < 4.4


def back_profile(X, Y, Z, j):
    return (X / 3.6) ** 2 + ((Y - 19.8) / 3.2) ** 2 < 1.0 + 0.35 * j and Y < 21.6


def crown_profile(X, Y, Z, j):
    return Y > 17.0 + 0.9 * (X / 3.0) ** 2 + 0.8 * abs(j)


def hh_face(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right"):
        if abs(Y - 21.0) < 0.5 and abs(Z - (-5.5)) < 0.5:
            return HH["eye"]
        if abs(Y - 21.0) < 1.0 and -6.2 < Z < -4.2:
            return fur(HH["mask"], tx, ty, 68, 0.05)
        return fur(HH["fur"] if Y < 22.2 else HH["light"], tx, ty, 69)
    if face == "front":
        return fur(HH["mask"] if Y > 21.0 else HH["fur_dark"], tx, ty, 70)
    if face == "top":
        return fur(HH["light"] if abs(X) < 0.6 else HH["fur"], tx, ty, 71)  # pale band up the forehead
    return fur(HH["light"], tx, ty, 72)


def hh_snout(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return fur(HH["light"], tx, ty, 73)
    if face == "top":
        return fur(lerp(HH["fur"], HH["mask"], 0.4), tx, ty, 74)
    return fur(HH["mask"] if Y < 22.2 else HH["fur"], tx, ty, 75, 0.05)


def hh_nose(face, X, Y, Z, n, tx, ty):
    return HH["shine"] if face == "top" else HH["nose"]


def hh_ear(face, X, Y, Z, n, tx, ty):
    return fur(HH["fur_dark"] if face != "front" else HH["light"], tx, ty, 76)


def hh_leg(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return HH["leg"]
    if face == "front" and Y > 23.4:
        return HH["claw"] if tx % 2 == 0 else HH["leg"]
    return fur(HH["leg"], tx, ty, 77)


HEDGEHOG = [
    part("body", None, (0, 21, 0), (0, 0, 0), [
        cube("hh_mantle", -3, -3, -4, 6, 4, 8, hh_mantle),
        cube("hh_dome", -2, -4, -3, 4, 1, 6, hh_dome),
        cube("hh_belly", -2.5, 1, -3.5, 5, 1, 7, hh_belly),
        cube("hh_side", -3.1, -4.5, -5, 0, 5, 10, plane_spines(side_profile, 1)),
        cube("hh_side", 3.1, -4.5, -5, 0, 5, 10, plane_spines(side_profile, 1), True),
        cube("hh_crest", 0, -5, -4.5, 0, 2, 9, plane_spines(crest_profile, 2)),
        cube("hh_back", -3.5, -4.5, 4.1, 7, 5, 0, plane_spines(back_profile, 3)),
        cube("hh_crown", -3, -4.5, -4.1, 6, 2, 0, plane_spines(crown_profile, 4)),
    ]),
    part("head", "body", (0, 0.5, -4), (0, 0, 0), [
        cube("hh_face", -1.5, -1.5, -2, 3, 3, 2, hh_face),
        cube("hh_snout", -1, -0.5, -4, 2, 2, 2, hh_snout),
        cube("hh_nose", -0.5, 0, -5, 1, 1, 1, hh_nose),
        cube("hh_ear", -2, -2, -1, 1, 1, 1, hh_ear),
        cube("hh_ear", 1, -2, -1, 1, 1, 1, hh_ear, True),
    ]),
    part("right_front_leg", None, (-1.8, 22, -2.5), (0, 0, 0), [cube("hh_leg", -0.5, 0, -0.5, 1, 2, 1, hh_leg)]),
    part("left_front_leg", None, (1.8, 22, -2.5), (0, 0, 0), [cube("hh_leg", -0.5, 0, -0.5, 1, 2, 1, hh_leg, True)]),
    part("right_hind_leg", None, (-1.8, 22, 2.5), (0, 0, 0), [cube("hh_leg", -0.5, 0, -0.5, 1, 2, 1, hh_leg)]),
    part("left_hind_leg", None, (1.8, 22, 2.5), (0, 0, 0), [cube("hh_leg", -0.5, 0, -0.5, 1, 2, 1, hh_leg, True)]),
]
HEDGEHOG_SIZE = (64, 32)

# ======================================================================= badger
# Grizzled silver-grey back (each hair dark-banded with a pale tip), black legs, belly and chest; white head with a black
# stripe each side running from the muzzle through the eye over the ear; small white-rimmed ears; black nose.
BG = dict(grey=(140, 136, 130), grey_dark=(98, 94, 90), silver=(196, 192, 184), tip=(54, 50, 48), black=(30, 28, 27),
          black_hi=(52, 48, 46), white=(236, 234, 226), cream=(218, 212, 198), nose=(18, 16, 16), shine=(84, 80, 80),
          eye=(8, 6, 6), claw=(176, 164, 144), pad=(44, 38, 36))


def grizzle(c, tx, ty, salt):
    n = h(tx, ty, salt)
    if n < 0.22:
        c = lerp(c, BG["tip"], 0.55)
    elif n < 0.40:
        c = lerp(c, BG["silver"], 0.5)
    if h(tx, ty // 2, salt + 1) < 0.25:
        c = shade(c, 0.88)                                   # hair streaks
    return fur(c, tx, ty, salt + 2, 0.05)


def bg_coat(belly_y):
    """Back and flanks grey, darkening down the flank into the black belly."""
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "bottom" or Y > belly_y:
            return fur(BG["black"] if h(tx, ty, 80) > 0.2 else BG["black_hi"], tx, ty, 81, 0.05)
        if face == "front" and Y > belly_y - 2.2:
            return fur(BG["black"], tx, ty, 82, 0.05)        # black chest
        t = smooth((Y - (belly_y - 4.5)) / 4.0)
        c = lerp(BG["grey"], BG["grey_dark"], t)
        if face == "top" and abs(X) < 1.2:
            c = lerp(c, BG["silver"], 0.15)                  # paler along the spine
        return grizzle(c, tx, ty, 83)
    return mat


def bg_fringe(face, X, Y, Z, n, tx, ty):
    """Long guard hairs hanging below the flank: grey over black, ragged lower edge."""
    bottom = 21.0 + 0.9 * h(tx, 0, 84)
    if Y > bottom:
        return (0, 0, 0, 0)
    c = lerp(BG["grey_dark"], BG["black_hi"], smooth((Y - 19.4) / 1.6))
    return grizzle(c, tx, ty, 85)


def stripe(X, Y, Z):
    """Black face stripe: the outer band of the forehead and muzzle top, and a band along the side through the eye."""
    return 0.9 < abs(X) < 2.1 and Y < 17.4


def bg_skull(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right"):
        if abs(Y - 16.5) < 0.5 and abs(Z - (-8.0)) < 0.5:
            return BG["eye"]
        if 15.0 < Y < 17.4:
            return fur(BG["black"], tx, ty, 86, 0.04)
        if Y <= 15.0:
            return fur(BG["white"], tx, ty, 87, 0.03)
        return fur(BG["white"] if Z < -7.0 else BG["cream"], tx, ty, 88, 0.03)  # white cheeks, greying to the neck
    if face == "top":
        if 0.9 < abs(X):
            return fur(BG["black"], tx, ty, 89, 0.04)
        return fur(BG["white"], tx, ty, 90, 0.03)
    if face == "bottom":
        return fur(BG["black"], tx, ty, 91, 0.04)
    if face == "back":
        return grizzle(BG["grey"], tx, ty, 92)
    if 0.9 < abs(X) and Y < 17.4:
        return fur(BG["black"], tx, ty, 93, 0.04)
    return fur(BG["white"], tx, ty, 94, 0.03)


def bg_muzzle(face, X, Y, Z, n, tx, ty):
    if face == "top":
        return fur(BG["black"] if abs(X) > 0.6 else BG["white"], tx, ty, 95, 0.03)
    if face in ("left", "right"):
        return fur(BG["black"] if Y < 16.8 else BG["white"], tx, ty, 96, 0.03)
    if face == "bottom":
        return fur(BG["cream"], tx, ty, 97, 0.03)
    if abs(X) > 0.6 and Y < 16.8:
        return fur(BG["black"], tx, ty, 98, 0.03)
    return fur(BG["white"], tx, ty, 99, 0.03)


def bg_nose(face, X, Y, Z, n, tx, ty):
    if face == "front" and Y > 17.4:
        return BG["pad"]
    return BG["shine"] if face == "top" else BG["nose"]


def bg_ear(face, X, Y, Z, n, tx, ty):
    if face == "top" or face == "front":
        return BG["white"]
    return BG["black"]


def bg_leg(claws):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return BG["pad"]
        return fur(BG["black"] if h(tx, ty, 100) > 0.15 else BG["black_hi"], tx, ty, 101, 0.05)
    return mat


def bg_claws(face, X, Y, Z, n, tx, ty):
    if face in ("front", "top") and tx % 2 == 1:
        return BG["claw"]
    return BG["pad"] if face != "top" else shade(BG["claw"], 0.8)


def bg_tail(face, X, Y, Z, n, tx, ty):
    return grizzle(lerp(BG["grey"], BG["silver"], 0.3), tx, ty, 102)


def up(mat, dy=1.5):
    """The painters below were written with the body 1.5 px higher; shift so they keep their marks."""
    return lambda face, X, Y, Z, n, tx, ty: mat(face, X, Y - dy, Z, n, tx, ty)


BADGER = [
    part("body", None, (0, 18.5, 0), (0, 0, 0), [
        cube("bg_fore", -3, -2.5, -6, 6, 5, 8, up(bg_coat(18.6))),
        cube("bg_rump", -3.5, -3, 1, 7, 6, 6, up(bg_coat(19.2))),
        cube("bg_fringe_f", -3.05, 2, -5.5, 0, 2, 7, up(bg_fringe)),
        cube("bg_fringe_f", 3.05, 2, -5.5, 0, 2, 7, up(bg_fringe), True),
        cube("bg_fringe_r", -3.55, 2.5, 1.5, 0, 2, 5, up(bg_fringe)),
        cube("bg_fringe_r", 3.55, 2.5, 1.5, 0, 2, 5, up(bg_fringe), True),
    ]),
    part("tail", "body", (0, -2, 7), (-0.5, 0, 0), [cube("bg_tail", -1, -0.5, 0, 2, 2, 3, up(bg_tail))]),
    part("head", "body", (0, -0.5, -6), (0, 0, 0), [
        cube("bg_skull", -2, -2, -3, 4, 4, 3, up(bg_skull)),
        cube("bg_muzzle", -1.5, -1, -5, 3, 3, 2, up(bg_muzzle)),
        cube("bg_nose", -1, -0.5, -6, 2, 2, 1, up(bg_nose)),
        cube("bg_ear", -2.6, -2.6, -1, 1, 1, 1, up(bg_ear)),
        cube("bg_ear", 1.6, -2.6, -1, 1, 1, 1, up(bg_ear), True),
    ]),
    part("right_front_leg", None, (-2, 20, -4), (0, 0, 0), [cube("bg_fleg", -1.5, -1, -1.5, 3, 5, 3, up(bg_leg(True))),
                                                         cube("bg_claws", -1.5, 3, -2.5, 3, 1, 1, up(bg_claws))]),
    part("left_front_leg", None, (2, 20, -4), (0, 0, 0), [cube("bg_fleg", -1.5, -1, -1.5, 3, 5, 3, up(bg_leg(True)), True),
                                                       cube("bg_claws", -1.5, 3, -2.5, 3, 1, 1, up(bg_claws), True)]),
    part("right_hind_leg", None, (-2.3, 20, 4.5), (0, 0, 0), [cube("bg_hleg", -1.5, -1, -1.5, 3, 5, 3, up(bg_leg(False)))]),
    part("left_hind_leg", None, (2.3, 20, 4.5), (0, 0, 0), [cube("bg_hleg", -1.5, -1, -1.5, 3, 5, 3, up(bg_leg(False)), True)]),
]
BADGER_SIZE = (64, 64)


def build(parts, size):
    uv = pack(parts, *size)
    return paint(parts, uv, size), uv


MODELS = {"squirrel": (SQUIRREL, SQUIRREL_SIZE), "hedgehog": (HEDGEHOG, HEDGEHOG_SIZE), "badger": (BADGER, BADGER_SIZE)}


def main():
    for name, (parts, size) in MODELS.items():
        img, uv = build(parts, size)
        if "--java" in sys.argv:
            print(name)
            java_cubes(parts, uv)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")


if __name__ == "__main__":
    main()
