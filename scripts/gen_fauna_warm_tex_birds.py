"""Warm-country jungle birds: model geometry and painted entity textures (part of gen_fauna_warm_textures.py).

- hornbill (great hornbill, a FlockBird drawn by BirdModel): BirdModel's part tree from gen_bird_textures.geometry(), the
  beak swapped for the huge down-curved bill and the yellow casque on top; written into HornbillLayer.java.
- jungle fowl (rooster and hen in JungleFowlModel.java; the chick uses PheasantChickModel's geometry with its own colours).
- peafowl (peacock and peahen in PeafowlModel.java): the peacock's train is seven feather panels hinged at the rump that
  lie trailing behind him and swing up into the fan; all seven share one UV patch (front = the eyed upper side).
The cube tables below are the single source: main() writes them into the Java classes between the GENERATED markers and
paints every texel from where it sits on its cube (gen_bird_textures.paint), so model and texture can't drift apart.
Plucked carcass textures (textures/entity/skinned/wildlife/): bare goose-bumped skin, eyes, bill, casque, comb and legs
kept, the tail and wing edges as quill stubs, the peacock's train gone (transparent).
Deterministic (hashes only). Run alone: python scripts/gen_fauna_warm_tex_birds.py
"""
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_baby_small_wildlife_textures as chick_gen  # noqa: E402
import gen_bird_textures as B  # noqa: E402
from critter_paint import pack, save  # noqa: E402
from wildlife_paint import h, lerp, shade  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/warm"
HALF_PI = math.pi / 2
part, cube, fe, side = B.part, B.cube, B.fe, B.side

# ======================================================================= geometry
HORNBILL_S = dict(body=(5, 5, 9), leg=2, leg_x=1.5, foot=(2, 3), neck=(3, 3), head=(3, 3, 4), beak=(2, 2, 5), beak_y=1,
                  wing=(4, 8, 6, 3), wing_z=1, tail=(4, 9), tail_rot=-0.1, eye=(1, 1))


def hornbill_geometry():
    parts = B.geometry(HORNBILL_S)
    head = next(p for p in parts if p["name"] == "head")
    # the bill droops at the tip; the casque rides on the rear of the bill, a pixel above the crown
    head["cubes"] += [cube("bill_tip", -0.5, 0.0, -9.0, 1, 1, 2), cube("casque", -1, -3.5, -6.5, 2, 2, 5)]
    return parts


def fowl_geometry():
    return [
        part("body", None, (0, 18, 0), (-0.25, 0, 0), [cube("body", -2, -2, -3.5, 4, 4, 7)]),
        # the hackle cape, the small head, beak; pitch cancels the body's so the neck stands upright
        part("head", "body", (0, -1.5, -3), (0.25, 0, 0), [cube("neck", -1.5, -3, -1.5, 3, 4, 3), cube("head", -1, -4.5, -2.5, 2, 2, 3),
                                                         cube("beak", -0.5, -4, -3.5, 1, 1, 1)]),
        part("comb", "head", (0, 0, 0), (0, 0, 0), [cube("comb", -0.5, -6.5, -2.5, 1, 2, 3), cube("wattle", -0.5, -3, -2.5, 1, 2, 1)]),
        part("left_wing", "body", (2, -1.5, -2.5), (0, 0, 0), [cube("wing", 0, 0, 0, 1, 3, 6, True)]),
        part("right_wing", "body", (-2, -1.5, -2.5), (0, 0, 0), [cube("wing", -1, 0, 0, 1, 3, 6)]),
        # the upright tail fan (the hen's whole tail), the rooster's sickles arching up and back from its top
        part("tail", "body", (0, -1.5, 3), (-0.6, 0, 0), [cube("tail", -1, -3, 0, 2, 3, 2)]),
        part("sickle", "tail", (0, -3, 1), (1.25, 0, 0), [cube("sickle_a", -0.5, -1, 0, 1, 2, 6)]),
        part("sickle_tip", "sickle", (0, 0, 5.5), (-1.35, 0, 0), [cube("sickle_b", -0.5, -1, 0, 1, 2, 6)]),
        part("left_leg", None, (1, 20, 0.5), (0, 0, 0), [cube("leg", -0.5, 0, -0.5, 1, 3, 1, True), cube("foot", -1, 3, -2.5, 2, 1, 3, True)]),
        part("right_leg", None, (-1, 20, 0.5), (0, 0, 0), [cube("leg", -0.5, 0, -0.5, 1, 3, 1), cube("foot", -1, 3, -2.5, 2, 1, 3)]),
    ]


TRAIN_PANELS = 7
TRAIN_REST = -HALF_PI - 0.3  # trailing back and down to the ground
TRAIN_SPREAD = 0.05  # yaw between neighbouring panels when trailing
TRAIN_ROLL = 0.2  # and roll: the outer panels lie lower, so the train is a low dome
PANEL = (7, 16)  # width at the tip, length


def peafowl_geometry():
    pw, pl = PANEL
    parts = [
        part("body", None, (0, 17.5, 0), (-0.15, 0, 0), [cube("body", -2.5, -2.5, -4.5, 5, 5, 9)]),
        part("head", "body", (0, -2, -4), (0.15, 0, 0), [cube("neck", -1, -6, -1, 2, 7, 2), cube("head", -1, -8, -2, 2, 2, 3),
                                                       cube("beak", -0.5, -7.5, -3, 1, 1, 1)]),
        part("crest", "head", (0, -8, -0.5), (-0.2, 0, 0), [cube("crest", -0.5, -3, -1.5, 1, 3, 3)]),
        part("left_wing", "body", (2.5, -2, -3.5), (0, 0, 0), [cube("wing", 0, 0, 0, 1, 4, 8, True)]),
        part("right_wing", "body", (-2.5, -2, -3.5), (0, 0, 0), [cube("wing", -1, 0, 0, 1, 4, 8)]),
        part("tail", "body", (0, -2, 4.5), (0.25, 0, 0), [cube("tail", -1.5, -0.5, 0, 3, 1, 6)]),
        part("train", "body", (0, -1.5, 4), (0, 0, 0), []),
    ]
    for i in range(TRAIN_PANELS):
        k = i - TRAIN_PANELS // 2
        # each panel a hair further back (its own normal) the further out it sits: the middle one on top, no z-fighting
        parts.append(part(f"train_{i}", "train", (0, 0, 0), (TRAIN_REST, k * TRAIN_SPREAD, k * TRAIN_ROLL),
                          [cube("panel", -pw / 2, -pl, -0.5 + abs(k) * 0.15, pw, pl, 1)]))
    parts += [
        part("left_leg", None, (1.2, 19, 0.5), (0, 0, 0), [cube("leg", -0.5, 0, -0.5, 1, 4, 1, True), cube("foot", -1, 4, -2.5, 2, 1, 3, True)]),
        part("right_leg", None, (-1.2, 19, 0.5), (0, 0, 0), [cube("leg", -0.5, 0, -0.5, 1, 4, 1), cube("foot", -1, 4, -2.5, 2, 1, 3)]),
    ]
    return parts


# ======================================================================= hornbill
H_BLACK, H_GLOSS, H_SOOT = (24, 24, 28), (40, 44, 58), (34, 34, 38)
H_WHITE, H_CREAM, H_STAIN = (236, 234, 224), (232, 222, 180), (226, 200, 112)
H_YELLOW, H_ORANGE, H_IVORY, H_RED = (246, 204, 52), (232, 132, 40), (240, 226, 160), (206, 36, 30)
H_LEG = (112, 116, 108)


def hornbill_painters(S):
    def black(p, salt):
        return fe(lerp(H_BLACK, H_GLOSS, 0.5 if p.face == "top" else 0.2 if side(p) else 0), p, salt, 0.08)

    def white(p, salt):
        return fe(H_WHITE if p.face != "bottom" else shade(H_WHITE, 0.9), p, salt, 0.03)

    def body(p):
        if p.face == "bottom" or p.face == "back":
            return white(p, 1)
        if p.face == "front":
            return black(p, 2) if p.fy < 0.45 else white(p, 3)  # black breast over the white belly
        if side(p) and p.fy > 0.7 and p.fz > 0.25:
            return white(p, 4)  # white thighs and vent
        return black(p, 5)

    def neck(p):
        # white neck stained yellow with preen oil, the black face reaching down the throat
        if p.face == "front" and p.fy < 0.35:
            return black(p, 6)
        return fe(lerp(H_CREAM, H_STAIN, 0.5 * h(p.tx // 2, p.ty, 7)), p, 8, 0.05)

    def head(p):
        if B.is_eye(p, S):
            return H_RED
        if side(p) and (p.iy, p.iz) in ((0, 1), (2, 1), (1, 0), (1, 2)):
            return (14, 12, 12)  # bare black skin round the eye
        return black(p, 9)

    def bill(p):
        # yellow, orange-stained at the base and along the top, ivory towards the tip
        c = lerp(H_YELLOW, H_ORANGE, 0.55 * (1 - p.fz) if p.face == "top" else 0.0)
        if p.fz < 0.35:
            c = lerp(c, H_IVORY, 0.6 * (0.35 - p.fz) / 0.35)
        if p.face == "bottom":
            c = shade(c, 0.85)
        if side(p) and p.iy == 1 and p.fz < 0.9:
            c = shade(c, 0.78)  # the gape line
        return fe(c, p, 10, 0.04)

    def bill_tip(p):
        return fe(shade(H_IVORY, 0.9 if p.face == "bottom" else 1.0), p, 11, 0.03)

    def casque(p):
        # broad and flat-topped, orange-yellow above, a black rim at the front corners and along the base
        if p.face == "front" and p.iy == 1:
            return (30, 26, 22)
        if p.face == "top":
            return fe(lerp(H_YELLOW, H_ORANGE, 0.45 + 0.25 * (abs(p.x) < 0.5)), p, 12, 0.05)
        return fe(lerp(H_YELLOW, H_ORANGE, 0.25 * (1 - p.fy)), p, 13, 0.05)

    def wing(p):
        if not p.upper:
            return fe(H_WHITE, p, 14, 0.03) if 0.45 <= p.c < 0.62 and p.s < 0.6 else fe(H_SOOT, p, 15)
        if p.c > 0.86 and p.s < 0.6 or p.s > 0.6 and p.c > 0.7:
            return fe(H_WHITE, p, 16, 0.03)  # white tips of the flight feathers: the pale trailing edge
        if 0.45 <= p.c < 0.62 and p.s < 0.6 or 0.4 <= p.s < 0.5 and p.c > 0.3:
            return fe(lerp(H_WHITE, H_CREAM, 0.4), p, 17, 0.03)  # the broad wing bar
        return B.feathers_along(p, lerp(H_BLACK, H_GLOSS, 0.4 - 0.25 * p.s), 18)

    def tip(p):
        # primaries: white bases continue the wing bar (folded, a pale patch at the front of the closed wing), black beyond
        if p.s < 0.52:
            return fe(lerp(H_WHITE, H_CREAM, 0.4), p, 22, 0.03) if p.upper or p.face != "left" else fe(H_WHITE, p, 23, 0.03)
        if p.s > 0.9 and p.iz == 2:
            return fe(H_SOOT, p, 24)
        return B.feathers_along(p, lerp(H_BLACK, H_GLOSS, 0.3), 25) if p.upper else fe(H_SOOT, p, 26)

    def tail(p):
        if 0.38 <= p.fz < 0.62 and p.face != "bottom" or 0.42 <= p.fz < 0.58:
            return black(p, 19)  # the black band across the long white tail
        return white(p, 20)

    def leg(p):
        return fe(H_LEG, p, 21, 0.08)

    return dict(body=body, neck=neck, head=head, beak=bill, bill_tip=bill_tip, casque=casque, wing=wing, tip=tip,
                tail=tail, leg=leg, foot=leg)


# ======================================================================= red jungle fowl
F_EYE, F_BEAK, F_LEG = (176, 72, 30), (190, 170, 128), (110, 112, 112)
F_RED, F_RED_D, F_FACE = (214, 34, 28), (168, 20, 22), (222, 70, 50)
F_GOLD, F_FLAME, F_MAROON, F_BROWN = (238, 168, 44), (224, 104, 30), (126, 36, 22), (150, 64, 30)
F_GREEN, F_BLACK = (30, 62, 48), (18, 22, 22)
FH_BROWN, FH_DARK, FH_LIGHT, FH_GOLD = (128, 90, 56), (84, 56, 34), (168, 128, 82), (196, 146, 66)
F_EYE_POS = (1, 1)  # head side texel (row, z column) of the eye
FOWL_S = dict(wing=(3, 6, 0, 0), eye=F_EYE_POS)


def fowl_painters(rooster):
    def green(p, salt):
        """Glossy green-black: lighter green on the upper faces."""
        return fe(lerp(F_BLACK, F_GREEN, 0.75 if p.face == "top" else 0.45 if side(p) else 0.2), p, salt, 0.1)

    def mottled(c, p, salt):
        """The hen's fine brown vermiculation."""
        r = h(p.tx, p.ty, salt)
        return fe(FH_DARK if r < 0.3 else FH_LIGHT if r > 0.85 else c, p, salt + 1, 0.05)

    def hackle(p, salt):
        """Long pointed neck feathers: gold streaks, flame-orange towards the shoulders (rooster)."""
        if not rooster:
            return fe(lerp(FH_GOLD, FH_DARK, 0.5 * (int(p.x * 2) % 2)), p, salt, 0.05)
        c = lerp(F_GOLD, F_FLAME, p.fy * 0.9)
        return fe(shade(c, 0.86) if int(p.x * 2 + p.z * 2) % 3 == 0 else c, p, salt, 0.05)

    def body(p):
        if not rooster:
            if p.face == "bottom" or p.face == "front" and p.fy > 0.5:
                return fe(lerp(FH_LIGHT, FH_GOLD, 0.3), p, 1, 0.05)
            return mottled(FH_BROWN, p, 2)
        if p.face == "top":
            # the cape of hackles over the shoulders, the dark red saddle behind it
            return hackle(p, 3) if p.fz < 0.3 else fe(lerp(F_MAROON, F_FLAME, 0.35 * h(p.tx, p.ty // 2, 4)), p, 5, 0.06)
        if p.face == "back" and p.fy < 0.4:
            return fe(lerp(F_FLAME, F_GOLD, 0.4), p, 6, 0.06)  # the long golden saddle hackles over the rump
        if side(p) and p.fy < 0.25:
            return fe(F_MAROON, p, 7, 0.06)
        return green(p, 8)  # black-green breast and belly

    def neck(p):
        if not rooster and p.face == "front":
            return fe(FH_GOLD, p, 9, 0.05)
        if rooster and p.face == "front" and p.fy > 0.6:
            return green(p, 10)
        return hackle(p, 11)

    def head(p):
        if B.is_eye(p, FOWL_S):
            return F_EYE
        if side(p) and p.iy == 1 and p.iz == 2 and rooster:
            return (242, 240, 232)  # white ear lobe
        if p.face == "top" or p.face == "back" or side(p) and p.iy == 0 and p.iz > 0:
            return hackle(p, 12) if rooster else fe(FH_GOLD, p, 12, 0.05)
        return fe(F_FACE if rooster else lerp(F_FACE, FH_BROWN, 0.6), p, 13, 0.06)  # bare red face

    def beak(p):
        return shade(F_BEAK, 0.8) if p.face == "bottom" else F_BEAK

    def comb(p):
        # serrated blade: the top row alternates points and gaps
        if p.face == "top" or side(p) and p.iy == 0:
            if int(p.z + 2.5) % 2 == 1:
                return (0, 0, 0, 0)
        return fe(F_RED if p.face != "bottom" else F_RED_D, p, 14, 0.06)

    def wattle(p):
        return fe(F_RED if p.iy == 0 else F_RED_D, p, 15, 0.06)

    def wing(p):
        if not rooster:
            if not p.upper:
                return fe(FH_LIGHT, p, 16, 0.04)
            return B.feathers_along(p, FH_DARK if p.c > 0.7 else FH_BROWN, 17) if h(p.tx, p.ty, 18) > 0.3 else fe(FH_LIGHT, p, 19, 0.05)
        if not p.upper:
            return fe(F_BLACK, p, 20, 0.06)
        if p.c > 0.65:
            return B.feathers_along(p, lerp(F_BROWN, F_GOLD, 0.25 if int(p.y) % 2 else 0.0), 21)  # rust-edged secondaries
        if p.s > 0.6:
            return green(p, 22)  # the glossy green wing bar
        return fe(lerp(F_MAROON, F_BROWN, 0.4 * h(p.tx, p.ty, 23)), p, 24, 0.05)  # dark red shoulder

    def tail(p):
        if not rooster:
            return fe(FH_DARK if int(p.y) % 2 else FH_BROWN, p, 25, 0.06)
        return green(p, 26)

    def sickle(p):
        c = lerp(F_BLACK, F_GREEN, 0.9 if side(p) and int(p.z) % 3 else 0.6)  # metallic green with a sheen in bands
        return fe(c, p, 27, 0.08)

    def leg(p):
        return fe(F_LEG, p, 28, 0.06)

    return dict(body=body, neck=neck, head=head, beak=beak, comb=comb, wattle=wattle, wing=wing, tail=tail,
                sickle_a=sickle, sickle_b=sickle, leg=leg, foot=leg)


# chick: PheasantChickModel's geometry and painters, re-coloured: darker chocolate stripes and a chestnut crown
FOWL_CHICK = dict(C_BUFF=(206, 162, 100), C_TAWNY=(146, 92, 52), C_CREAM=(236, 218, 172), C_STRIPE=(62, 38, 24),
                  C_RUFOUS=(136, 70, 36), C_DARK=(40, 26, 18), C_BEAK=(196, 160, 120), C_LEG=(176, 150, 120))


def fowl_chick():
    saved = {k: getattr(chick_gen, k) for k in FOWL_CHICK}
    try:
        for k, v in FOWL_CHICK.items():
            setattr(chick_gen, k, v)
        chick_gen.G.TEX_W, chick_gen.G.TEX_H = chick_gen.CHICK_W, chick_gen.CHICK_H
        return chick_gen.G.paint(chick_gen.PHEASANT_CHICK, chick_gen.CHICK_PAINTERS)[0]
    finally:
        for k, v in saved.items():
            setattr(chick_gen, k, v)
        chick_gen.G.TEX_W, chick_gen.G.TEX_H = 64, 32


# ======================================================================= peafowl
P_BLUE, P_BLUE_L, P_BLUE_D = (28, 84, 186), (62, 138, 222), (18, 44, 120)
P_TEAL, P_GREEN, P_BRONZE, P_GOLD = (26, 140, 132), (48, 128, 62), (150, 112, 44), (196, 164, 72)
P_NAVY, P_FAN_BACK = (14, 18, 60), (104, 88, 64)
P_BARRED, P_BAR_D, P_CHESTNUT = (196, 176, 136), (60, 48, 36), (166, 92, 42)
PH_BROWN, PH_DARK, PH_GREEN, PH_WHITE = (128, 108, 84), (92, 76, 58), (48, 110, 72), (232, 228, 214)
P_BEAK, P_LEG = (180, 168, 150), (120, 112, 104)
PEA_S = dict(wing=(4, 8, 0, 0), eye=(0, 1))
CLEAR = (0, 0, 0, 0)


def panel_alpha(p):
    """The feather panel's outline: narrow at the hinge, widening to a rounded tip."""
    pw, pl = PANEL
    r = -p.y  # distance from the hinge
    half = 1.0 + 2.5 * min(1.0, r / (pl - 2))
    if r > pl - 1:
        half -= 1.5  # rounded tip
    return abs(p.x) <= half


def ocellus(p):
    """Colour of an eye-spot at this texel, or None: navy pupil, blue, teal ring, bronze ring, the green fringe."""
    for cy, rx, ry in ((-13.0, 3.0, 2.5), (-8.5, 2.0, 1.8), (-4.5, 1.2, 1.2)):
        d = math.hypot(p.x / rx, (p.y - cy) / ry)
        if d <= 1.0:
            return P_NAVY if d < 0.35 else P_BLUE if d < 0.6 else P_TEAL if d < 0.8 else P_BRONZE
    return None


def peafowl_painters(male):
    def blue(p, salt):
        """Iridescent blue: a lighter sheen on upper faces and in scattered scales."""
        c = P_BLUE_L if p.face == "top" or h(p.tx, p.ty, salt) > 0.8 else P_BLUE_D if p.face == "bottom" else P_BLUE
        return fe(c, p, salt + 1, 0.05)

    def brown(p, salt):
        r = h(p.tx, p.ty, salt)
        return fe(PH_DARK if r < 0.3 else PH_BROWN, p, salt + 1, 0.05)

    def body(p):
        if not male:
            if p.face == "bottom" or p.face == "back" and p.fy > 0.4 or side(p) and p.fy > 0.75 and p.fz > 0.2:
                return fe(PH_WHITE, p, 1, 0.03)  # white belly
            if p.face == "front" and p.fy < 0.4:
                return fe(PH_GREEN, p, 2, 0.08)  # the green of the neck down onto the breast
            return brown(p, 3)
        if p.face == "top" and p.fz > 0.35:
            return fe(lerp(P_GREEN, P_BRONZE, 0.3 * h(p.tx, p.ty, 4)), p, 5, 0.08)  # scaly green-bronze back
        if p.face == "bottom" or side(p) and p.fy > 0.75 or p.face == "back":
            return fe((34, 40, 52), p, 6, 0.06)  # dark underparts
        return blue(p, 7)

    def neck(p):
        if not male:
            return fe(PH_GREEN, p, 8, 0.1) if p.fy > 0.3 or p.face != "back" else brown(p, 9)
        return blue(p, 10)

    def head(p):
        if B.is_eye(p, PEA_S):
            return (18, 14, 12)
        if side(p) and p.iy == 1 and p.iz <= 1:
            return PH_WHITE  # the white face stripe below the eye
        if side(p) and p.iy == 0 and p.iz == 2:
            return PH_WHITE
        return blue(p, 11) if male else (fe(PH_GREEN, p, 12, 0.08) if p.face != "top" else brown(p, 13))

    def beak(p):
        return shade(P_BEAK, 0.82) if p.face == "bottom" else P_BEAK

    def crest(p):
        # the fan of bare shafts with tufts on top (the peacock's blue, the peahen's brown-green): seen from the side the
        # three tufts on the top row, the shafts converging to one below
        tuft = P_BLUE_L if male else (96, 116, 78)
        stalk = (30, 54, 116) if male else PH_DARK
        if p.face == "top":
            return tuft
        if p.face in ("bottom", "front", "back"):
            return tuft if p.iy == 0 else CLEAR
        if p.iy == 0:
            return tuft if male or p.iz == 1 else stalk
        return stalk if (p.iy == 1) != (p.iz == 1) else CLEAR

    def wing(p):
        if not p.upper:
            return fe(PH_BROWN if male else lerp(PH_BROWN, PH_WHITE, 0.4), p, 14, 0.04)
        if not male:
            return B.feathers_along(p, PH_DARK if p.c > 0.7 else PH_BROWN, 15)
        if p.c > 0.72:
            return B.feathers_along(p, P_CHESTNUT, 16)  # chestnut primaries folded at the back
        # buff coverts mottled with black bars
        return fe(lerp(P_BARRED, P_BAR_D, 0.75) if h(p.tx, p.ty, 17) < 0.4 else P_BARRED, p, 18, 0.05)

    def tail(p):
        return fe(lerp(PH_DARK, P_BRONZE, 0.3) if male else PH_DARK, p, 18, 0.06)

    def panel(p):
        if p.face not in ("front", "back") or not panel_alpha(p):
            return CLEAR
        if p.face == "back":
            return fe(P_FAN_BACK, p, 19, 0.08)  # the dull underside
        eye = ocellus(p)
        if eye:
            return eye
        # bronze-green barbs, a darker shaft down the middle
        c = lerp(P_GREEN, P_GOLD, 0.35 * h(p.tx, p.ty, 20))
        if abs(p.x) < 0.5:
            c = shade(c, 0.72)
        return fe(c, p, 21, 0.06)

    def leg(p):
        return fe(P_LEG, p, 22, 0.06)

    return dict(body=body, neck=neck, head=head, beak=beak, crest=crest, wing=wing, tail=tail, panel=panel, leg=leg, foot=leg)


# ======================================================================= plucked
QUILL, QUILL_D = (236, 226, 202), (204, 188, 156)


def plucked(base, eye, light, dark, keep=(), quills=(), gone=()):
    """Bare skin with follicles everywhere except: keep (bill, legs, comb) unchanged, eye texels kept, quills (tails) as
    rows of stubs, gone (feather-only parts: the peacock's train, the crest) transparent; the trailing wing edge stubbled."""
    def skin(p, salt=40):
        c = lerp(light, dark, 0.3 * h(p.tx // 2, p.ty, salt) + (0.35 if p.face == "bottom" else 0.12 if p.face == "back" else 0.0))
        return shade(c, 0.86) if h(p.tx, p.ty, salt + 1) < 0.16 else c

    def stubs(p):
        return QUILL if int(p.x * 2 + p.z) % 2 else QUILL_D

    def make(key):
        if key in keep:
            return base[key]
        if key in gone:
            return lambda p: CLEAR
        if key in quills:
            return lambda p: stubs(p) if p.face in ("top", "bottom", "back") or int(p.y) % 2 == 0 else shade(QUILL_D, 0.92)
        if key in ("wing", "tip"):
            return lambda p: stubs(p) if p.c > 0.75 and int(p.y) % 2 == 0 else skin(p, 44)
        if key == "head":
            return lambda p: base["head"](p) if eye(p) else skin(p, 42)
        return lambda p: skin(p, 40 + len(key))

    return {k: make(k) for k in base}


# ======================================================================= Java
def jf(v):
    v = round(v, 4)
    return f"{v}F" if v != int(v) else f"{int(v)}.0F"


def java_layer(method, parts, uv, size):
    out = [f"    public static LayerDefinition {method}() {{",
           "        MeshDefinition mesh = new MeshDefinition();",
           "        PartDefinition root = mesh.getRoot();"]
    for p in parts:
        cubes = "".join(
            f".texOffs({uv[c['key']][0]}, {uv[c['key']][1]}){'.mirror()' if c['mirror'] else ''}.addBox("
            + ", ".join(jf(v) for v in c["box"]) + ")" for c in p["cubes"])
        pose = ", ".join(jf(v) for v in (*p["pivot"], *p["rot"]))
        parent = B.camel(p["parent"]) if p["parent"] else "root"
        out.append(f"        PartDefinition {B.camel(p['name'])} = {parent}.addOrReplaceChild(\"{p['name']}\",")
        out.append(f"            CubeListBuilder.create(){cubes},")
        out.append(f"            PartPose.offsetAndRotation({pose}));")
    out += [f"        return LayerDefinition.create(mesh, {size[0]}, {size[1]});", "    }"]
    return "\n".join(out)


def write_java(cls, layer):
    path = JAVA / f"{cls}.java"
    src = path.read_text(encoding="utf-8")
    begin, end = "    // BEGIN GENERATED by scripts/gen_fauna_warm_tex_birds.py\n", "    // END GENERATED\n"
    if begin not in src:
        return  # the model now comes from gen_owarm_birds_models.py (WarmBirdGeometry), which also repaints these skins
    assert end in src, f"end marker missing in {cls}.java"
    new = re.sub(re.escape(begin) + ".*?" + re.escape(end), lambda m: begin + layer + "\n" + end, src, flags=re.S)
    if new != src:
        path.write_text(new, encoding="utf-8")
        print("wrote", path.relative_to(ROOT))


def build(parts, size, S, textures, cls, method):
    """Pack, write the Java layer, paint each (rel path, painters)."""
    B.SIZE = size  # gen_bird_textures.paint reads the texture size from its module
    uv = pack(parts, *size)
    write_java(cls, java_layer(method, parts, uv, size))
    for rel, painters in textures:
        save(B.paint(parts, uv, S, painters), rel)


# ======================================================================= main
def main():
    S = HORNBILL_S
    hb = hornbill_painters(S)
    build(hornbill_geometry(), (64, 32), S, [
        ("wildlife/hornbill.png", hb),
        ("skinned/wildlife/hornbill.png", plucked(hb, lambda p: B.is_eye(p, S), (238, 200, 170), (196, 146, 118),
                                                 keep=("beak", "bill_tip", "casque", "leg", "foot"), quills=("tail",))),
    ], "HornbillLayer", "create")

    fowl_eye = lambda p: B.is_eye(p, FOWL_S)  # noqa: E731
    rooster, hen = fowl_painters(True), fowl_painters(False)
    fowl_keep = ("beak", "comb", "wattle", "leg", "foot")
    build(fowl_geometry(), (64, 32), FOWL_S, [
        ("wildlife/jungle_fowl_rooster.png", rooster),
        ("wildlife/jungle_fowl_hen.png", hen),
        ("skinned/wildlife/jungle_fowl_rooster.png", plucked(rooster, fowl_eye, (240, 206, 160), (204, 154, 112), keep=fowl_keep,
                                                             quills=("tail", "sickle_a", "sickle_b"))),
        ("skinned/wildlife/jungle_fowl_hen.png", plucked(hen, fowl_eye, (240, 206, 160), (204, 154, 112), keep=fowl_keep,
                                                         quills=("tail", "sickle_a", "sickle_b"))),
    ], "JungleFowlModel", "createBodyLayer")
    save(fowl_chick(), "wildlife/jungle_fowl_chick.png")

    pea_eye = lambda p: B.is_eye(p, PEA_S)  # noqa: E731
    cock, hen = peafowl_painters(True), peafowl_painters(False)
    pea_keep = ("beak", "leg", "foot")
    build(peafowl_geometry(), (64, 64), PEA_S, [
        ("wildlife/peacock.png", cock),
        ("wildlife/peahen.png", hen),
        ("skinned/wildlife/peacock.png", plucked(cock, pea_eye, (236, 196, 168), (192, 140, 112), keep=pea_keep, quills=("tail",),
                                                 gone=("panel", "crest"))),
        ("skinned/wildlife/peahen.png", plucked(hen, pea_eye, (236, 196, 168), (192, 140, 112), keep=pea_keep, quills=("tail",),
                                                gone=("panel", "crest"))),
    ], "PeafowlModel", "createBodyLayer")


if __name__ == "__main__":
    main()
