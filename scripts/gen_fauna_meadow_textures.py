"""Paint the meadow fauna (scripts/gen_fauna_meadow.py, com.mechanical.wildlife.meadow): Wild Cat (+ kitten), Mole, Field
Vole, Adder (male grey, female brown) and Grass Snake - entity textures and their skinned (butchered) versions - plus the
Molehill block, the two equipment layers (Wildcat Fur Cap, Snakeskin Boots) and every 16x16 item texture.

Usage: python scripts/gen_fauna_meadow_textures.py [--java]
The part trees below mirror WildCatModel / MoleModel / FieldVoleModel / SnakeModel (client/wildlife/meadow): pivots,
rotations, boxes and the UVs from pack(). --java prints each model's createBodyLayer() parts ready to paste, so the Java
stays in step with the paint. Deterministic: hashes only, no randomness.
"""
import importlib.util
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from critter_paint import cube, fur, h, lerp, pack, paint, part, save, shade, skinned, smooth  # noqa: E402

_spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
wit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wit)
ITEM = wit.OUT
RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"

# ======================================================================= wild cat
# European wild cat: yellowish grey-brown tabby, a dark line down the spine, broken vertical stripes on the flanks, four
# lines over the forehead running back to the neck, two cheek stripes, a cream throat and belly, pink nose, amber-green
# eyes; the thick blunt tail with 3 black rings and a black tip; dark soles running up the hind feet.
CAT = dict(ground=(152, 134, 104), back=(118, 100, 78), stripe=(58, 46, 36), belly=(210, 194, 164), throat=(228, 218, 196),
           ring=(34, 28, 24), nose=(198, 128, 118), eye=(184, 176, 64), pupil=(16, 14, 10), ear_in=(200, 166, 148),
           ear_back=(128, 102, 80), sole=(48, 40, 34), stripe_amt=0.75, fluff=0.07)
# kitten: paler, fluffier, the stripes bolder and bluish-grey eyes
KITTEN = dict(CAT, ground=(166, 150, 124), back=(130, 114, 92), stripe=(70, 56, 46), belly=(222, 210, 186),
              eye=(126, 146, 160), stripe_amt=0.85, fluff=0.11)

BODY_Y, BODY_Z = 15, 0                       # body pivot


def cat_body(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "bottom" or Y > 16.9:
            c = P["belly"] if h(tx, ty, 300) > 0.12 else shade(P["belly"], 0.82)   # faint belly spots
            return fur(c, tx, ty, 301, 0.04)
        if face == "front":
            return fur(P["throat"] if Y > 14.5 else P["ground"], tx, ty, 302, 0.05)
        c = lerp(P["back"], P["ground"], smooth((Y - 12.5) / 3.2))
        if face == "top" and abs(X) < 0.8:
            return fur(P["stripe"], tx, ty, 303, 0.06)                          # spinal line
        band = math.sin(Z * 1.3 + 0.9 * (h(tx // 2, 0, 304) - 0.5))
        if band > 0.6 and Y < 16.6 and h(tx, ty, 305) > 0.15:
            c = lerp(c, P["stripe"], P["stripe_amt"] * (0.6 if face == "top" else 1.0))
        elif Y > 15.8:
            c = lerp(c, P["belly"], 0.35)
        return fur(c, tx, ty, 306, P["fluff"])
    return mat


def cat_skull(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "front":
            if abs(Y - 12.5) < 0.5 and abs(abs(X) - 1.0) < 0.5:
                return P["pupil"] if h(tx, ty, 310) < 0 else P["eye"]          # amber eye
            if abs(Y - 12.5) < 0.5 and abs(abs(X) - 2.0) < 0.5:
                return lerp(P["eye"], P["stripe"], 0.5)                         # dark eye corner
            if Y < 12.0 and abs(abs(X) - 0.5) < 0.5:
                return fur(P["stripe"], tx, ty, 311, 0.05)                      # forehead lines
            if Y > 13.5:
                return fur(P["throat"], tx, ty, 312, 0.04)
            return fur(P["ground"], tx, ty, 313, P["fluff"])
        if face == "top":
            if abs(abs(X) - 0.5) < 0.5 or abs(abs(X) - 2.0) < 0.5 and Z > -8:
                return fur(P["stripe"], tx, ty, 314, 0.05)
            return fur(P["ground"], tx, ty, 315, P["fluff"])
        if face == "bottom":
            return fur(P["throat"], tx, ty, 316, 0.04)
        if face in ("left", "right"):
            if Y > 14.0:
                return fur(P["throat"], tx, ty, 317, 0.04)
            if abs(Y - 13.0) < 0.5 and Z > -9.0:
                return fur(P["stripe"], tx, ty, 318, 0.05)                      # cheek stripe
            return fur(P["ground"], tx, ty, 319, P["fluff"])
        return fur(P["back"], tx, ty, 320, P["fluff"])
    return mat


def cat_muzzle(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "front":
            if Y < 14.0:
                return P["nose"] if abs(X) < 0.6 else fur(P["throat"], tx, ty, 321, 0.03)
            return shade(P["throat"], 0.75) if abs(X) < 0.6 else fur(P["throat"], tx, ty, 322, 0.03)
        if face == "top":
            return fur(P["ground"], tx, ty, 323, 0.05)
        return fur(P["throat"], tx, ty, 324, 0.03)
    return mat


def cat_ear(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "front":
            return fur(P["ear_in"], tx, ty, 325, 0.05)
        if Y < 9.6 and face in ("back", "top"):
            return fur(P["stripe"], tx, ty, 326, 0.05)                           # dark tip
        return fur(P["ear_back"], tx, ty, 327, 0.06)
    return mat


TAIL_BASE = (13.5, 6.0)                      # tail pivot in model space (y, z)


def cat_tail(P):
    def mat(face, X, Y, Z, n, tx, ty):
        d = math.hypot(Y - TAIL_BASE[0], Z - TAIL_BASE[1])
        if d > 8.2 or any(a < d < a + 1.1 for a in (3.6, 5.4, 7.0)):
            return fur(P["ring"], tx, ty, 330, 0.05)                             # rings and the blunt black tip
        c = P["ground"]
        if face == "top" and d < 3.2 and abs(X) < 0.6:
            c = P["stripe"]
        return fur(c, tx, ty, 331, P["fluff"] * 1.3)
    return mat


def cat_leg(P, hind):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["sole"]
        if hind and face == "back" and Y > 20.0:
            return fur(P["sole"], tx, ty, 332, 0.04)                             # the dark sole up the hind foot
        if Y > 23.0:
            return fur(lerp(P["ground"], P["belly"], 0.3), tx, ty, 333, 0.04)   # paws
        c = P["ground"] if face != "front" or hind else lerp(P["ground"], P["belly"], 0.25)
        if face in ("left", "right") and any(abs(Y - b) < 0.5 for b in ((19.5, 21.5) if not hind else (18.5, 20.5))):
            c = lerp(c, P["stripe"], 0.7)
        return fur(c, tx, ty, 334, P["fluff"])
    return mat


def wild_cat(P):
    return [
        part("body", None, (0, BODY_Y, BODY_Z), (0, 0, 0), [cube("wc_body", -2.5, -2.5, -6, 5, 5, 12, cat_body(P))]),
        part("head", "body", (0, -1.5, -6), (0, 0, 0), [cube("wc_skull", -2.5, -2.5, -4, 5, 4, 4, cat_skull(P)),
                                                        cube("wc_muzzle", -1.5, -0.5, -5, 3, 2, 1, cat_muzzle(P))]),
        part("right_ear", "head", (-1.5, -2.5, -2), (0, 0, -0.25), [cube("wc_ear", -1, -2, -0.5, 2, 2, 1, cat_ear(P))]),
        part("left_ear", "head", (1.5, -2.5, -2), (0, 0, 0.25), [cube("wc_ear", -1, -2, -0.5, 2, 2, 1, cat_ear(P), True)]),
        part("tail", "body", (0, -1.5, 6), (-0.9, 0, 0), [cube("wc_tail1", -1.5, -1.5, 0, 3, 3, 5, cat_tail(P))]),
        part("tail_tip", "tail", (0, 0, 5), (0.35, 0, 0), [cube("wc_tail2", -1.5, -1.5, 0, 3, 3, 4, cat_tail(P))]),
        part("right_front_leg", None, (-1.5, 17, -4.5), (0, 0, 0), [cube("wc_fleg", -1, 0, -1, 2, 7, 2, cat_leg(P, False))]),
        part("left_front_leg", None, (1.5, 17, -4.5), (0, 0, 0), [cube("wc_fleg", -1, 0, -1, 2, 7, 2, cat_leg(P, False), True)]),
        part("right_hind_leg", None, (-1.5, 16, 4.5), (0, 0, 0), [cube("wc_hleg", -1, 0, -1.5, 2, 8, 3, cat_leg(P, True))]),
        part("left_hind_leg", None, (1.5, 16, 4.5), (0, 0, 0), [cube("wc_hleg", -1, 0, -1.5, 2, 8, 3, cat_leg(P, True), True)]),
    ]


# ======================================================================= mole
# Velvet slate-black fur with a soft grey sheen along the back, a bare pink pointed snout, huge pink spade forepaws with
# pale claws, small pink hind feet, a short sparse tail; the eyes are pinpricks lost in the fur.
MO = dict(velvet=(40, 38, 44), sheen=(76, 74, 84), dark=(24, 22, 26), pink=(222, 160, 158), pink_dark=(176, 116, 116),
          claw=(236, 226, 206), eye=(8, 8, 10))


def velvet(c, tx, ty, salt):
    n = h(tx, ty, salt)
    return shade(c, 0.94 + 0.12 * n)                                              # short dense pile: fine, no streaks


def mo_body(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or Y > 22.8:
        return velvet(MO["dark"], tx, ty, 340)
    if face == "top":
        return velvet(lerp(MO["velvet"], MO["sheen"], 0.5 + 0.3 * math.sin(Z * 0.9)), tx, ty, 341)  # the sheen
    return velvet(lerp(MO["sheen"], MO["velvet"], smooth((Y - 19.5) / 2.5)), tx, ty, 342)


def mo_head(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right") and abs(Y - 21.0) < 0.5 and abs(Z - (-4.5)) < 0.5:
        return MO["eye"]
    if face == "top":
        return velvet(MO["sheen"], tx, ty, 343)
    return velvet(MO["velvet"], tx, ty, 344)


def mo_snout(face, X, Y, Z, n, tx, ty):
    if face == "front":
        return MO["pink_dark"]                                                    # the nose disc
    return MO["pink"] if face != "bottom" else MO["pink_dark"]


def mo_hand(face, X, Y, Z, n, tx, ty):
    if face == "right" or face == "bottom":                                       # palm out, digging edge down
        if Y > 22.6 and tx % 2 == 0:
            return MO["claw"]
        return lerp(MO["pink"], MO["pink_dark"], 0.3 * h(tx, ty, 345))
    if face == "front" and Y > 22.6:
        return MO["claw"]
    return MO["pink"] if Y > 22.0 else velvet(MO["velvet"], tx, ty, 346)


def mo_foot(face, X, Y, Z, n, tx, ty):
    return MO["pink"] if face != "bottom" else MO["pink_dark"]


def mo_tail(face, X, Y, Z, n, tx, ty):
    return lerp(MO["pink_dark"], MO["velvet"], 0.5)


MOLE = [
    part("body", None, (0, 21.5, 0), (0, 0, 0), [cube("mo_body", -2, -2, -3.5, 4, 4, 7, mo_body)]),
    part("head", "body", (0, 0.5, -3.5), (0, 0, 0), [cube("mo_head", -1.5, -1.5, -2, 3, 3, 2, mo_head),
                                                     cube("mo_snout", -0.5, -0.5, -4, 1, 1, 2, mo_snout)]),
    part("right_hand", "body", (-2, 1, -2), (0, 0, 0.4), [cube("mo_hand", -1, -1, -1.5, 1, 3, 3, mo_hand)]),
    part("left_hand", "body", (2, 1, -2), (0, 0, -0.4), [cube("mo_hand", 0, -1, -1.5, 1, 3, 3, mo_hand, True)]),
    part("right_foot", None, (-1.5, 23, 2.5), (0, 0, 0), [cube("mo_foot", -0.5, 0, -1, 1, 1, 2, mo_foot)]),
    part("left_foot", None, (1.5, 23, 2.5), (0, 0, 0), [cube("mo_foot", -0.5, 0, -1, 1, 1, 2, mo_foot, True)]),
    part("tail", "body", (0, -1, 3.5), (0.6, 0, 0), [cube("mo_tail", -0.5, -0.5, 0, 1, 1, 2, mo_tail)]),
]

# ======================================================================= field vole
# Grizzled dark grey-brown back (dark hair tips over a warm brown), greyer flanks, a pale grey belly, a blunt face with
# beady black eyes, small rounded ears half buried in the fur, a short two-tone tail and pinkish-grey feet.
VO = dict(back=(98, 78, 56), warm=(132, 104, 72), tip=(52, 42, 32), flank=(122, 108, 90), belly=(170, 164, 152),
          eye=(10, 8, 8), glint=(200, 200, 204), ear=(106, 84, 66), foot=(176, 150, 140), nose=(118, 86, 80))


def grizzle(c, tx, ty, salt):
    n = h(tx, ty, salt)
    if n < 0.25:
        c = lerp(c, VO["tip"], 0.55)
    elif n < 0.42:
        c = lerp(c, VO["warm"], 0.5)
    return fur(c, tx, ty, salt + 1, 0.05)


def vo_body(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or Y > 22.4:
        return fur(VO["belly"], tx, ty, 350, 0.04)
    return grizzle(lerp(VO["back"], VO["flank"], smooth((Y - 20.2) / 2.0)), tx, ty, 351)


def vo_head(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right"):
        if abs(Y - 20.5) < 0.5 and abs(Z - (-3.5)) < 0.5:
            return VO["eye"]
        if Y > 21.8:
            return fur(VO["belly"], tx, ty, 352, 0.04)
    if face == "bottom":
        return fur(VO["belly"], tx, ty, 353, 0.04)
    return grizzle(VO["back"] if face == "top" else VO["flank"], tx, ty, 354)


def vo_snout(face, X, Y, Z, n, tx, ty):
    if face == "front":
        return VO["nose"] if Y < 21.5 else fur(VO["belly"], tx, ty, 355, 0.04)
    if face == "bottom" or Y > 22.0:
        return fur(VO["belly"], tx, ty, 356, 0.04)
    return grizzle(VO["flank"], tx, ty, 357)


def vo_ear(face, X, Y, Z, n, tx, ty):
    return fur(VO["ear"] if face != "front" else lerp(VO["ear"], VO["foot"], 0.4), tx, ty, 358, 0.05)


def vo_leg(face, X, Y, Z, n, tx, ty):
    return VO["foot"] if face != "bottom" else shade(VO["foot"], 0.8)


def vo_tail(face, X, Y, Z, n, tx, ty):
    return fur(VO["tip"] if face == "top" else VO["flank"], tx, ty, 359, 0.05)


VOLE = [
    part("body", None, (0, 21.5, 0), (0, 0, 0), [cube("vo_body", -1.5, -1.5, -2.5, 3, 3, 5, vo_body)]),
    part("head", "body", (0, -0.5, -2.5), (0, 0, 0), [cube("vo_head", -1.5, -1.5, -2, 3, 3, 2, vo_head),
                                                      cube("vo_snout", -1, -0.5, -3, 2, 2, 1, vo_snout)]),
    part("right_ear", "head", (-1, -1.5, -0.5), (0, 0, -0.3), [cube("vo_ear", -0.5, -1, 0, 1, 1, 1, vo_ear)]),
    part("left_ear", "head", (1, -1.5, -0.5), (0, 0, 0.3), [cube("vo_ear", -0.5, -1, 0, 1, 1, 1, vo_ear, True)]),
    part("right_front_leg", None, (-1, 23, -1.5), (0, 0, 0), [cube("vo_leg", -0.5, 0, -0.5, 1, 1, 1, vo_leg)]),
    part("left_front_leg", None, (1, 23, -1.5), (0, 0, 0), [cube("vo_leg", -0.5, 0, -0.5, 1, 1, 1, vo_leg, True)]),
    part("right_hind_leg", None, (-1, 23, 1.5), (0, 0, 0), [cube("vo_leg", -0.5, 0, -0.5, 1, 1, 1, vo_leg)]),
    part("left_hind_leg", None, (1, 23, 1.5), (0, 0, 0), [cube("vo_leg", -0.5, 0, -0.5, 1, 1, 1, vo_leg, True)]),
    part("tail", "body", (0, -0.5, 2.5), (-0.3, 0, 0), [cube("vo_tail", -0.5, -0.5, 0, 1, 1, 2, vo_tail)]),
]

# ======================================================================= snakes
# One chain shared by both snakes: head, neck and five body segments (3 px each, the middle ones 3 wide) and a 1x1 tail;
# the forked tongue is a flat plane under the snout. Model space: the snake lies along z, head at z -14..-10.
SEG = [("neck", 2), ("seg2", 3), ("seg3", 3), ("seg4", 3), ("seg5", 2), ("seg6", 2)]
NECK_Z = -10                                     # neck pivot (model z); each segment is 3 long
SNAKE_Y = 23


def snake_parts(m):
    parts = [part("neck", None, (0, SNAKE_Y, NECK_Z), (0, 0, 0), [cube("sn_neck", -1, -1, 0, 2, 2, 3, m("body"))]),
             part("head", "neck", (0, 0, 0), (0, 0, 0), [cube("sn_head", -1.5, -1, -4, 3, 2, 4, m("head")),
                                                         cube("sn_tongue", -0.5, 0.5, -6, 1, 0, 2, m("tongue"))])]
    parent = "neck"
    for name, wdt in SEG[1:]:
        x0 = -wdt / 2
        parts.append(part(name, parent, (0, 0, 3), (0, 0, 0), [cube(f"sn_{name}", x0, -1, 0, wdt, 2, 3, m("body"))]))
        parent = name
    parts.append(part("tail", parent, (0, 0, 3), (0, 0, 0), [cube("sn_tail", -0.5, -0.5, 0, 1, 1, 3, m("tail"))]))
    return parts


def snake_size(Z):
    """Distance along the body from the snout (model z)."""
    return Z - (NECK_Z - 4)


def scales(c, tx, ty, salt):
    """Overlapping scales: a faint diagonal lattice, darker scale edges."""
    k = (tx + ty) % 2
    return shade(c, (0.92 if k else 1.04) + 0.06 * (h(tx, ty, salt) - 0.5))


AD_M = dict(ground=(178, 172, 156), side=(154, 148, 132), zig=(26, 24, 24), belly=(46, 44, 46), eye=(176, 70, 34),
            lip=(214, 208, 192), tongue=(30, 28, 32))
AD_F = dict(AD_M, ground=(160, 120, 78), side=(138, 102, 66), zig=(60, 34, 22), belly=(52, 40, 34), lip=(204, 176, 132))


def adder(P):
    def m(kind):
        def mat(face, X, Y, Z, n, tx, ty):
            if kind == "tongue":
                fork = Z < -11.0 and abs(X) < 0.5
                return (0, 0, 0, 0) if fork and tx % 2 == 0 else P["tongue"]
            if face == "bottom":
                return scales(P["belly"], tx, ty, 360)
            if kind == "head":
                if face in ("left", "right"):
                    if abs(Y - 22.5) < 0.5 and abs(Z - (-12.5)) < 0.5:
                        return P["eye"]
                    if abs(Y - 22.5) < 0.5 and Z > -12.0:
                        return P["zig"]                                          # dark band behind the eye
                    if Y > 23.0:
                        return P["lip"]                                          # pale lip scales
                    return scales(P["side"], tx, ty, 361)
                if face == "top":
                    s = Z + 14                                                   # 0 at the snout
                    v = s > 1.5 and abs(abs(X) - (s - 1.5) * 0.45) < 0.5       # the dark V (an X on some)
                    return P["zig"] if v else scales(P["ground"], tx, ty, 362)
                if face == "front":
                    return P["lip"] if Y > 22.5 else scales(P["ground"], tx, ty, 363)
                return scales(P["ground"], tx, ty, 364)
            s = snake_size(Z)
            if face == "top" or abs(n[1]) > 0.7:
                zz = abs(((s * 0.75) % 2) - 1)                                   # the zigzag: a triangle wave in x
                edge = 0.25 + zz * 0.9
                if abs(X) < edge:
                    return scales(P["zig"], tx, ty, 365)
                return scales(P["ground"], tx, ty, 366)
            # flanks: a row of dark spots opposite the zigzag's points
            spot = abs(((s * 0.75 + 1) % 2) - 1) < 0.35 and Y < 23.6
            return scales(P["zig"] if spot else P["side"], tx, ty, 367)
        return mat
    return m


GS = dict(olive=(98, 110, 78), olive_dark=(78, 88, 62), bar=(24, 24, 20), collar=(232, 204, 70), collar_dark=(22, 20, 18),
          lip=(222, 220, 196), belly=(214, 212, 196), check=(34, 34, 32), eye=(28, 26, 22), iris=(150, 120, 60),
          tongue=(36, 30, 34))


def grass_snake(face_kind):
    def mat(face, X, Y, Z, n, tx, ty):
        if face_kind == "tongue":
            fork = Z < -11.0 and abs(X) < 0.5
            return (0, 0, 0, 0) if fork and tx % 2 == 0 else GS["tongue"]
        if face == "bottom":
            return GS["check"] if (tx // 1 + ty // 2) % 3 == 0 else scales(GS["belly"], tx, ty, 370)   # chequered belly
        if face_kind == "head":
            if face in ("left", "right"):
                if abs(Y - 22.5) < 0.5 and abs(Z - (-12.5)) < 0.5:
                    return GS["eye"]
                if Y > 22.9 or Z < -13.0:
                    return GS["lip"]
                if Z > -11.0:
                    return GS["collar"]                                          # the yellow collar starts on the head
                return scales(GS["olive"], tx, ty, 371)
            if face == "front":
                return GS["lip"] if Y > 22.5 else scales(GS["olive"], tx, ty, 372)
            return scales(GS["olive"], tx, ty, 373)
        s = snake_size(Z)
        if s < 5.0:                                                              # collar: yellow, then a black band
            if face in ("left", "right"):
                return GS["collar"] if s < 4.0 else GS["collar_dark"]
            return GS["collar_dark"] if s > 3.6 else scales(GS["olive"], tx, ty, 374)
        if face in ("left", "right") and (int(s) % 3 == 0) and h(tx, ty, 375) > 0.2:
            return GS["bar"]                                                     # black bars along the flank
        if face == "top" and abs(X) < 1.0 and int(s * 2) % 7 == 0:
            return GS["olive_dark"]                                              # two faint rows of dark flecks
        return scales(GS["olive"] if face == "top" else lerp(GS["olive"], GS["olive_dark"], 0.3), tx, ty, 376)
    return mat


# ======================================================================= build, Java parts

MODELS = {
    "wild_cat": (lambda: wild_cat(CAT), (64, 32)),
    "mole": (lambda: MOLE, (64, 32)),
    "field_vole": (lambda: VOLE, (32, 32)),
    "adder": (lambda: snake_parts(adder(AD_M)), (64, 32)),
}
VARIANTS = {  # same UVs as their base model, other paint
    "wild_cat_kitten": ("wild_cat", lambda: wild_cat(KITTEN)),
    "adder_female": ("adder", lambda: snake_parts(adder(AD_F))),
    "grass_snake": ("adder", lambda: snake_parts(grass_snake)),
}


def f(v):
    return f"{float(v)}F"


def java_parts(parts, uv):
    """createBodyLayer() lines: every part, its cubes (texOffs from pack()) and pose, in the Java model's shape."""
    out = []
    for p in parts:
        bits = []
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            plane = ", new CubeDeformation(0.001F)" if 0 in (w, hh, d) else ""
            b = f".texOffs({u}, {v})" + (".mirror()" if c["mirror"] else "") + \
                f".addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(hh)}, {f(d)}{plane})" + (".mirror(false)" if c["mirror"] else "")
            bits.append(b)
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = (f"PartPose.offset({f(px)}, {f(py)}, {f(pz)})" if (rx, ry, rz) == (0, 0, 0)
                else f"PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})")
        parent = "root" if p["parent"] is None else p["parent"]
        out.append(f'PartDefinition {p["name"]} = {parent}.addOrReplaceChild("{p["name"]}", CubeListBuilder.create(){"".join(bits)}, {pose});')
    return out


def entities():
    uvs = {}
    for name, (make, size) in MODELS.items():
        parts = make()
        uv = pack(parts, *size)
        uvs[name] = (uv, size)
        img = paint(parts, uv, size)
        if "--java" in sys.argv:
            print(f"// ---- {name} {size}")
            print("\n".join(java_parts(parts, uv)))
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")
    for name, (base, make) in VARIANTS.items():
        uv, size = uvs[base]
        img = paint(make(), uv, size)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")


# ======================================================================= block: molehill (fresh crumbly soil)

def molehill_texture():
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    base = [(74, 52, 36), (94, 68, 46), (112, 82, 56), (60, 42, 30), (128, 98, 68)]
    for y in range(16):
        for x in range(16):
            n = h(x, y, 380)
            c = base[0] if n < 0.3 else base[1] if n < 0.6 else base[2] if n < 0.82 else base[3] if n < 0.93 else base[4]
            if h(x // 2, y // 2, 381) < 0.18:
                c = shade(c, 0.8)                                                # clods and their shadows
            if h(x, y, 382) < 0.03:
                c = (150, 140, 120)                                              # a pebble
            if h(x, y, 383) < 0.025:
                c = (92, 124, 54)                                                # a torn grass blade
            px[x, y] = (*c, 255)
    im.save(RES / "block/molehill.png")


# ======================================================================= equipment layers (humanoid 64x32)

def equipment():
    # Wildcat Fur Cap: the head box (u 0, v 0, 8x8x8): tabby fur over the crown and down the back, an open face, the
    # ringed tail hanging behind on the back face
    cap = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = cap.load()
    P = CAT
    for face, (u0, v0) in {"top": (8, 0), "right": (0, 8), "front": (8, 8), "left": (16, 8), "back": (24, 8)}.items():
        for j in range(8):
            for i in range(8):
                if face == "front" and j > 2:
                    continue                                                     # the face stays open below the brim
                if face in ("left", "right") and j > 5:
                    continue
                c = P["ground"]
                if face == "top" and i in (3, 4):
                    c = P["stripe"]
                elif face in ("left", "right", "back") and (i + j // 2) % 4 == 0:
                    c = lerp(c, P["stripe"], 0.7)
                if face == "back" and i in (3, 4) and j >= 2:
                    c = P["ring"] if j in (3, 5, 7) else P["ground"]            # the tail down the back
                if face == "front" and j == 2:
                    c = shade(P["ground"], 0.75)                                 # rolled brim
                px[u0 + i, v0 + j] = (*fur(c, u0 + i, v0 + j, 390, 0.08), 255)
    save_eq(cap, "wildcat_fur", False)
    # Snakeskin Boots: the leg box (u 0, v 16, 4x12x4), the lower 5 rows wrapped in grey zigzag snakeskin, the sole dark
    boots = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = boots.load()
    for face, (u0, v0, wdt) in {"right": (0, 20, 4), "front": (4, 20, 4), "left": (8, 20, 4), "back": (12, 20, 4)}.items():
        for j in range(7, 12):
            for i in range(wdt):
                zz = abs(((j * 0.75 + i * 0.5) % 2) - 1)
                c = AD_M["zig"] if zz < 0.35 else AD_M["ground"]
                if j == 7:
                    c = (96, 66, 44)                                             # leather cuff
                px[u0 + i, v0 + j] = (*scales(c, u0 + i, v0 + j, 391), 255)
    for i in range(4):
        for j in range(4):
            px[8 + i, 16 + j] = (60, 44, 32, 255)                                # sole
    save_eq(boots, "snakeskin", False)


def save_eq(img, name, leggings):
    out = RES / "entity/equipment" / ("humanoid_leggings" if leggings else "humanoid") / f"{name}.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)


# ======================================================================= items (16x16)

PELT = [  # a stretched pelt, head up, legs out, tail down
    "......O..O......",
    ".....OHOOHO.....",
    ".....OHHHHO.....",
    "...OOOHEHEOOO...",
    "..OLLOHHHHOLLO..",
    "..OLMMRRRRMMLO..",
    "...OMRRRRRRMO...",
    "....ORRSRRRO....",
    "....ORRSRRRO....",
    "....ORRRSRRO....",
    "...OMRRRSRRMO...",
    "..OLMMRRRRMMLO..",
    "..OLLOORROOLLO..",
    "...OO..OTO..OO..",
    ".......OTO......",
    "........OO......",
]


WC_PELT = [  # the wild cat's: flank stripes across the spine line, the ringed tail
    "......O..O......",
    ".....OHOOHO.....",
    ".....OSHHSO.....",
    "...OOOHEHEOOO...",
    "..OLLOHHHHOLLO..",
    "..OLMMRSRRMMLO..",
    "...OSSRSRRSSO...",
    "....ORRSRRRO....",
    "....OSSSSSSO....",
    "....ORRRSRRO....",
    "...OMSSRSRSSO...",
    "..OLMMRRSRMMLO..",
    "..OLLOORROOLLO..",
    "...OO..OTO..OO..",
    ".......OHO......",
    "........OO......",
]


def pelt(pal, grid=PELT):
    return wit.from_grid(grid, pal)


WILDCAT_PELT = {"O": "#3a2d22", "H": "#988468", "E": "#3a2e24", "L": "#7d6a52", "M": "#a28c6c", "R": "#a99272",
                "S": "#3a2e24", "T": "#2a221c"}
WILDCAT_RAW = {"O": "#5a1c16", "H": "#6f5e48", "E": "#2a221a", "L": "#a23c30", "M": "#7a6650", "R": "#7d6a52",
               "S": "#2c241c", "T": "#1e1814"}
MOLESKIN = {"O": "#18161a", "H": "#3e3c46", "E": "#2a282e", "L": "#2c2a30", "M": "#4a4854", "R": "#555261",
            "S": "#3c3a44", "T": "#c49090"}
MOLE_RAW = {"O": "#4a1612", "H": "#2a282e", "E": "#1a181c", "L": "#9a3a30", "M": "#33313a", "R": "#3a3842",
            "S": "#2a282e", "T": "#a06868"}

SNAKESKIN = [
    "................",
    "...........OOO..",
    "..........OGZGO.",
    ".........OGZGZO.",
    "........OGZGZO..",
    ".......OZGZGO...",
    "......OGZGZO....",
    ".....OGZGZO.....",
    "....OZGZGO......",
    "...OGZGZO.......",
    "..OGZGZO........",
    ".OZGZGO.........",
    ".OGZGO..........",
    ".OGGO...........",
    "..OO............",
    "................",
]
SNAKESKIN_PAL = {"O": "#3c3a34", "G": "#c8c2ae", "Z": "#2e2c2a"}

VENOM_GLAND = [
    "................",
    "................",
    ".....OOOO.......",
    "....OWLLLO......",
    "...OWLLMMMO.....",
    "...OLLMMMMDO....",
    "...OLMMMMDDO....",
    "....OMMMDDDO....",
    ".....OMDDDO.....",
    "......ODDO......",
    ".......OYO......",
    "........OYO.....",
    ".........OYO....",
    "..........OO....",
    "................",
    "................",
]
VENOM_PAL = {"O": "#3e3a24", "W": "#fbf6d8", "L": "#e6dc9c", "M": "#c9bc6a", "D": "#9a8c44", "Y": "#d8d0a8"}

POULTICE = [  # herbs bound up in a leaf, tied with twine
    "................",
    "................",
    "......OOOO......",
    "....OOGLLGOO....",
    "...OGLLGGLLGO...",
    "..OGLGGTTGGLGO..",
    "..OLGGTYYTGGLO..",
    "..OGGTYWWYTGGO..",
    "..OLGTYWWYTGLO..",
    "..OGGGTYYTGGGO..",
    "...OGLGTTGLGO...",
    "....OOGLLGOO....",
    "......OOOO......",
    "................",
    "................",
    "................",
]
POULTICE_PAL = {"O": "#2a3a1a", "G": "#557a30", "L": "#7aa046", "T": "#b89a5a", "Y": "#e8dca0", "W": "#f6f0d8"}

SMALL_CRITTER = [  # a whole small skinned body on a skewer
    "................",
    "................",
    "..............O.",
    ".............OS.",
    "....OOOOO...OS..",
    "..OOFFFFFOOOS...",
    ".OFFLLLFFFFOO...",
    "OFLLLLLLFFFFO...",
    "OFLLLLLLLFFFDO..",
    ".OFLLLLLFFFDDO..",
    "..OFFFFFFDDOO...",
    "...OODDDDOO.....",
    "...SO.OO..OO....",
    "..S.............",
    ".S..............",
    "................",
]


def critter(flesh):
    o, d, f_, l = flesh
    return wit.from_grid(SMALL_CRITTER, {"O": o, "F": f_, "L": l, "D": d, "S": "#a88a5c"})


SNAKE_MEAT = [  # chunks of snake on a skewer: skin rim, pale flesh, the spine in the middle
    "................",
    "............OOO.",
    "...........OLFFO",
    "...........OFWFO",
    "..........OOFFDO",
    "........OOO.OOO.",
    ".......OLFFO....",
    ".......OFWFO....",
    "......OOFFDO....",
    "....OOO.OOO.....",
    "...OLFFO........",
    "...OFWFO........",
    "..SOFFDO........",
    ".S..OOO.........",
    "S...............",
    "................",
]

RAW_FLESH = ("#4e1414", "#8a2a26", "#b2443c", "#d26a5e")
COOKED_FLESH = ("#3a2212", "#6a4026", "#94603a", "#b88456")

FUR_CAP = [
    "................",
    "................",
    ".....OOOOOO.....",
    "....OGSGGSGO....",
    "...OGGSGGSGGO...",
    "..OGGGSGGSGGGO..",
    "..OGSGGGGGGSGO..",
    "..OGGGGGGGGGGO..",
    "..ODDDDDDDDDDO..",
    "..OO........OO..",
    "..........ORO...",
    "..........OGO...",
    "..........ORO...",
    "..........OGO...",
    "..........ORO...",
    "...........O....",
]
FUR_CAP_PAL = {"O": "#2a221c", "G": "#a08a6a", "S": "#3e3226", "D": "#6e5c46", "R": "#221c18"}

BOOTS = [
    "................",
    "................",
    "...OOOO.........",
    "...OLLO.........",
    "...OGZO...OOOO..",
    "...OZGO...OLLO..",
    "...OGZO...OGZO..",
    "...OZGO...OZGO..",
    "...OGZOO..OGZO..",
    "...OZGZGO.OZGO..",
    "...OGZGZGOOGZOO.",
    "...OOOOOOOOZGZGO",
    "..........OGZGZO",
    "..........OOOOOO",
    "................",
    "................",
]
BOOTS_PAL = {"O": "#2a2420", "L": "#6e4c32", "G": "#c8c2ae", "Z": "#2e2c2a"}


def recolour_meat(src, ramp):
    return wit.recolour(ITEM / src, ramp)


WILDCAT_RAW_RAMP = ["#2a0a08", "#46120f", "#601a16", "#782420", "#8c2f29", "#9c3a32", "#ae4a40", "#c06a5c"]
WILDCAT_COOKED_RAMP = ["#1e120a", "#2e1c10", "#402818", "#523420", "#62402a", "#724c32", "#82593c", "#a87c56"]


def eggs():
    W, K = "#efe6d4", "#141110"
    cat = wit.egg(["#b4a080", "#9a8666", "#827052", "#6a5a42", "#4a3e2e", "#30281e"],
                  [(x, y, "#3a2e24") for x, y in ((5, 4), (5, 5), (8, 4), (8, 5), (11, 5), (4, 8), (6, 9), (9, 9), (11, 8),
                                                  (5, 12), (8, 12), (10, 12))], (), [(7, 14, K), (8, 14, K), (7, 15, K), (8, 15, K)])
    mole = wit.egg(["#5a5864", "#46444e", "#36343c", "#2a282e", "#1c1a1e", "#121014"], [(x, y, "#4c4852") for x, y in ((5, 5), (9, 7), (6, 10))],
                   (), [(7, 0, "#d8a0a0"), (8, 0, "#d8a0a0"), (3, 9, "#e2a8a6"), (12, 9, "#e2a8a6"), (3, 10, "#e2a8a6"), (12, 10, "#e2a8a6")])
    vole = wit.egg(["#9a8060", "#86684a", "#705638", "#5a442c", "#3e2e1e", "#2a1e14"],
                   [(x, y, "#b4aa98") for x, y in ((6, 12), (7, 13), (8, 13), (9, 12), (7, 12), (8, 12))], (), [(5, 5, K), (10, 5, K)])
    zig = [(7 + (1 if (y // 2) % 2 else -1) * (0 if y % 2 else 1), y, "#1e1c1c") for y in range(1, 15)]
    adder = wit.egg(["#d2ccbc", "#bab4a2", "#a29c8a", "#8a8472", "#5e5a50", "#3c3a34"], zig + [(8 + x - 7, y, c) for x, y, c in zig], ())
    collar = [(x, 4, "#e8cc46") for x in range(3, 13)] + [(x, 5, "#1a1814") for x in range(3, 13)]
    bars = [(x, y, "#1a1814") for x, y in ((3, 8), (12, 8), (3, 11), (12, 11), (4, 13), (11, 13))]
    snake = wit.egg(["#7e8c62", "#6a7852", "#586444", "#465036", "#323a26", "#22281a"], collar + bars, ())
    return {"wild_cat_spawn_egg": cat, "mole_spawn_egg": mole, "field_vole_spawn_egg": vole, "adder_spawn_egg": adder,
            "grass_snake_spawn_egg": snake}


def items():
    tex = {
        "raw_wildcat_pelt": pelt(WILDCAT_RAW, WC_PELT), "wildcat_pelt": pelt(WILDCAT_PELT, WC_PELT),
        "raw_mole_pelt": pelt(MOLE_RAW), "moleskin": pelt(MOLESKIN),
        "snakeskin": wit.from_grid(SNAKESKIN, SNAKESKIN_PAL), "venom_gland": wit.from_grid(VENOM_GLAND, VENOM_PAL),
        "snakebite_poultice": wit.from_grid(POULTICE, POULTICE_PAL),
        "raw_wildcat": recolour_meat("beef_scraps.png", WILDCAT_RAW_RAMP),
        "cooked_wildcat": recolour_meat("beef_scraps.png", WILDCAT_COOKED_RAMP),
        "raw_mole": critter(RAW_FLESH), "cooked_mole": critter(COOKED_FLESH),
        "raw_vole": critter(("#5a1a18", "#962e2a", "#bc4c42", "#da7466")),
        "cooked_vole": critter(("#40261a", "#74482c", "#9c6840", "#c08c5e")),
        "raw_snake": wit.from_grid(SNAKE_MEAT, {**dict(zip("ODFL", RAW_FLESH)), "W": "#efe2d0", "S": "#a88a5c"}),
        "cooked_snake": wit.from_grid(SNAKE_MEAT, {**dict(zip("ODFL", COOKED_FLESH)), "W": "#e6d6b8", "S": "#a88a5c"}),
        "wildcat_fur_cap": wit.from_grid(FUR_CAP, FUR_CAP_PAL), "snakeskin_boots": wit.from_grid(BOOTS, BOOTS_PAL),
        **eggs(),
    }
    # the vole is smaller than the mole: shrink its body by a pixel all round
    tex["raw_vole"] = shrink(tex["raw_vole"])
    tex["cooked_vole"] = shrink(tex["cooked_vole"])
    for name, im in tex.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
    print("items:", ", ".join(tex))


def shrink(im):
    small = im.resize((13, 13), Image.NEAREST)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    out.paste(small, (1, 2))
    return out


def main():
    entities()
    molehill_texture()
    equipment()
    items()


if __name__ == "__main__":
    main()
