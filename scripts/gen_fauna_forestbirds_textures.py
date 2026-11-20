"""Forest birds (ForestBirdsFauna): model geometry, painted entity textures, plucked carcass textures and 16x16 items.

Usage: python scripts/gen_fauna_forestbirds_textures.py

Entities: the flock-bird part tree of gen_bird_textures.py (body, neck, head, wings with folded outer wings, tail, legs;
geometry() from a few dimensions), with on top per species
- tilt: the upright sitting angle (owl, eagle, vulture, raven): the body's rest pose is turned back by it (and raised so
  its lowest corner sits on the legs), the neck turned forward again so the head stays level (ForestBirdModel);
- extras: extra cubes on the shared parts (the capercaillie's beard, the bustard's whiskers, the owl's ear tufts, the
  eagle's hooked bill, the vulture's ruff, the raven's shaggy throat).
The layers are written into ForestBirdModel.java between the GENERATED markers. Every texel is painted from where it sits
on its cube (cube-local coordinates and model space, see gen_bird_textures.P). Textures are 64x64.
Plucked carcasses (textures/entity/skinned/wildlife/): gen_bird_textures.plucked_painters with each bird's skin.
Items: hand-placed 16x16 grids. Deterministic: hashes only, no randomness.
"""
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_bird_textures as gbt  # noqa: E402
from critter_paint import pack, save  # noqa: E402
from gen_bird_textures import cube, fe, feathers_along, is_eye, side  # noqa: E402
from PIL import Image  # noqa: E402
from wildlife_paint import h, lerp, shade  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
MODEL_JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/forestbirds/ForestBirdModel.java"
ITEM_TEX = ROOT / "src/main/resources/assets/mechanical/textures"
SIZE = (64, 64)
gbt.SIZE = SIZE  # gbt.paint() and gbt.java_layer() read the module's SIZE

# body (w, h, d); leg cube length; neck (w, length); head (w, h, d); beak (w, h, d) and its row from the head top; wing
# (inner span, chord, outer span, outer chord); tail (w, length) and its droop; eye (row, column from the front)
SPECIES = {
    "capercaillie": dict(body=(6, 6, 10), leg=3, leg_x=1.5, foot=(3, 3), neck=(3, 2), head=(4, 4, 4), beak=(2, 2, 2), beak_y=2,
                         wing=(5, 8, 4, 3), wing_z=1, tail=(6, 7), tail_rot=-0.3, eye=(1, 1), leg_w=2, tilt=0.0,
                         extras=[("head", "beard", (-1, 0.5, -2.5, 2, 2, 2), False)]),
    "great_bustard": dict(body=(6, 6, 10), leg=5, leg_x=1.5, foot=(2, 2), neck=(2, 5), head=(3, 3, 4), beak=(1, 1, 2), beak_y=1,
                          wing=(5, 9, 5, 3), wing_z=1, tail=(5, 4), tail_rot=-0.15, eye=(1, 1), tilt=0.0,
                          extras=[("head", "whisker", (-2.5, -0.5, -1.5, 1, 1, 3), False), ("head", "whisker", (1.5, -0.5, -1.5, 1, 1, 3), True)]),
    "owl": dict(body=(6, 5, 7), leg=2, leg_x=1.5, foot=(2, 2), neck=(3, 1), head=(6, 5, 5), beak=(1, 2, 1), beak_y=2,
                wing=(4, 6, 4, 3), wing_z=1, tail=(4, 4), tail_rot=-0.2, eye=(2, 1), leg_w=2, tilt=0.9,
                extras=[("head", "tuft", (-3, -6.5, -2.5, 1, 2, 1), False), ("head", "tuft", (2, -6.5, -2.5, 1, 2, 1), True)]),
    "raven": dict(body=(5, 5, 9), leg=3, leg_x=1, foot=(2, 3), neck=(2, 1), head=(4, 4, 4), beak=(2, 2, 3), beak_y=1,
                  wing=(4, 7, 6, 3), wing_z=1, tail=(4, 6), tail_rot=-0.2, eye=(1, 1), tilt=0.25,
                  extras=[("head", "hackle", (-1, 0.5, -2.5, 2, 1, 2), False)]),
    "golden_eagle": dict(body=(6, 6, 10), leg=3, leg_x=1.5, foot=(3, 3), neck=(3, 2), head=(4, 4, 5), beak=(2, 2, 2), beak_y=1,
                         wing=(5, 9, 9, 4), wing_z=1, tail=(5, 7), tail_rot=-0.15, eye=(1, 1), leg_w=2, tilt=0.45,
                         extras=[("head", "hook", (-1, -0.5, -5.5, 2, 1, 1), False)]),
    "vulture": dict(body=(7, 7, 11), leg=3, leg_x=1.5, foot=(3, 3), neck=(2, 4), head=(3, 3, 4), beak=(2, 2, 2), beak_y=1,
                    wing=(6, 10, 10, 5), wing_z=1, tail=(5, 4), tail_rot=-0.15, eye=(1, 1), leg_w=2, tilt=0.5,
                    extras=[("body", "ruff", (-2.5, -4.5, -5.5, 5, 2, 3), False)]),
}


def geometry(S):
    """gbt.geometry plus the upright tilt and the extra cubes."""
    parts = gbt.geometry(S)
    by_name = {p["name"]: p for p in parts}
    t = S.get("tilt", 0.0)
    if t:
        bw, bh, bd = S["body"]
        leg_py = 23 - S["leg"]
        lowest = bh / 2 * math.cos(t) + bd / 2 * math.sin(t)  # the lowest body corner (rear underside) after turning back
        body = by_name["body"]
        body["pivot"] = (0, leg_py + 1 - lowest, 0)
        body["rot"] = (-t, 0, 0)
        by_name["neck"]["rot"] = (t, 0, 0)
    for part, key, box, mirror in S.get("extras", []):
        by_name[part]["cubes"].append(cube(key, *box, mirror=mirror))
    return parts


def mottle(c, dark, p, salt, amount=0.3):
    return fe(dark if h(p.tx, p.ty, salt) < amount else c, p, salt + 1, 0.06)


def barred(c, bar, p, along, salt, width=2):
    """Dark bars across the feathers (every `width` px along `along`), slightly ragged."""
    if int(along) % width == 0 and h(p.tx, p.ty, salt) < 0.85:
        c = bar
    return fe(c, p, salt + 1, 0.05)


# ======================================================================= capercaillie: the cock and the hen
def capercaillie_painters(S):
    SLATE, SLATE_D, VERM = (58, 60, 66), (40, 40, 46), (86, 88, 92)
    GREEN, GREEN_L = (34, 74, 58), (52, 104, 80)
    BROWN, BROWN_D = (96, 66, 40), (66, 44, 28)
    BLACK, WHITE, RED = (22, 22, 24), (232, 230, 224), (200, 36, 32)
    BEAK, LEG = (226, 218, 196), (92, 88, 84)

    def slate(p, salt):
        """Dark grey finely vermiculated with paler grey."""
        return fe(VERM if h(p.tx, p.ty, salt) < 0.18 else SLATE, p, salt + 1, 0.05)

    def body(p):
        if p.face == "front":
            return fe(lerp(GREEN, GREEN_L, h(p.tx, p.ty, 1) * 0.6), p, 2, 0.05) if p.fy < 0.65 else fe(BLACK, p, 3)  # green breast shield
        if p.face == "top":
            return mottle(BROWN, BROWN_D, p, 4, 0.3) if p.fz < 0.55 else slate(p, 5)  # brown mantle, grey rump
        if p.face == "bottom":
            return fe(WHITE if p.fz > 0.8 else BLACK, p, 6) if h(p.tx, p.ty, 7) > 0.18 else WHITE  # black belly, white flecks
        if p.face == "back":
            return slate(p, 8)
        if p.fy > 0.7:
            return fe(BLACK, p, 9) if h(p.tx, p.ty, 10) > 0.15 else WHITE
        return slate(p, 11)

    def neck(p):
        return fe(SLATE_D, p, 12) if p.face != "back" else slate(p, 13)

    def head(p):
        if is_eye(p, S):
            return (30, 22, 18)
        ey, ez = S["eye"]
        if side(p) and p.iy == ey - 1 and ez - 1 <= p.iz <= ez + 1:
            return RED  # the red brow over the eye
        return slate(p, 14)

    def beak(p):
        return shade(BEAK, 0.85) if p.face == "bottom" else BEAK

    def beard(p):
        return fe(BLACK, p, 15, 0.1)

    def wing(p):
        if not p.upper:
            return fe((206, 204, 198) if p.s < 0.5 else (120, 116, 110), p, 16)
        if p.s < 0.18 and p.c < 0.2:
            return WHITE  # the white shoulder spot
        if p.s > 0.55:
            return feathers_along(p, lerp(BROWN_D, (52, 46, 40), (p.s - 0.55) * 2), 17)
        return mottle(BROWN, BROWN_D, p, 18, 0.35)

    def tail(p):
        if p.face == "bottom":
            return fe(WHITE if h(p.tx, p.ty, 19) < 0.4 else BLACK, p, 20)
        if p.fz < 0.35 and h(p.tx, p.ty, 21) < 0.22:
            return WHITE  # white flecks near the base of the fan
        return fe(BLACK, p, 22, 0.08)

    def leg(p):
        return fe(LEG, p, 23, 0.08) if p.iy < S["leg"] else fe((60, 56, 52), p, 24)  # feathered shanks, dark toes

    return dict(body=body, neck=neck, head=head, beak=beak, beard=beard, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


def capercaillie_hen_painters(S):
    RUST, BUFF, BAR, BREAST = (164, 112, 64), (206, 166, 112), (54, 36, 24), (204, 128, 62)
    PALE, RED, BEAK, LEG = (226, 204, 168), (196, 52, 40), (70, 62, 56), (150, 128, 104)

    def body(p):
        if p.face == "front":
            return fe(BREAST, p, 1) if p.fy < 0.5 else barred(PALE, BAR, p, p.Y, 2)  # plain orange breast
        if p.face == "bottom":
            return barred(PALE, BAR, p, p.Z, 3, 3)
        if p.face == "back":
            return barred(RUST, BAR, p, p.Y, 4, 3)
        if side(p) and p.fy > 0.7:
            return barred(PALE, BAR, p, p.Z, 5, 3)
        return barred(lerp(RUST, BUFF, h(p.tx, p.ty, 6) * 0.5), BAR, p, p.Z, 7, 3)

    def neck(p):
        return barred(BREAST if p.face == "front" else RUST, BAR, p, p.Y, 8, 3)

    def head(p):
        if is_eye(p, S):
            return (28, 20, 16)
        ey, ez = S["eye"]
        if side(p) and p.iy == ey - 1 and p.iz == ez:
            return RED
        return barred(RUST, BAR, p, p.y + p.z, 9, 3)

    def beak(p):
        return BEAK

    def beard(p):
        return barred(BUFF, BAR, p, p.y, 10, 3)

    def wing(p):
        if not p.upper:
            return fe(PALE, p, 11)
        return barred(lerp(RUST, BUFF, 0.3) if p.s < 0.55 else (128, 92, 56), BAR, p, p.z, 12, 3)

    def tail(p):
        if p.fz > 0.85:
            return fe(PALE, p, 13)
        return barred((176, 98, 50), BAR, p, p.z, 14, 3)  # rufous tail with black bars

    def leg(p):
        return fe(LEG, p, 15, 0.08)

    return dict(body=body, neck=neck, head=head, beak=beak, beard=beard, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# ======================================================================= great bustard (cock)
def bustard_painters(S):
    GINGER, GINGER_L, BAR = (190, 128, 66), (214, 156, 90), (48, 32, 22)
    GREY, GREY_D, WHITE, CHESTNUT = (168, 172, 180), (140, 144, 152), (240, 238, 232), (170, 96, 52)
    DARK, BEAK, LEG = (52, 44, 40), (150, 140, 120), (130, 112, 86)

    def body(p):
        if p.face == "top" or p.face == "back" and p.fy < 0.4:
            return barred(lerp(GINGER, GINGER_L, h(p.tx, p.ty, 1) * 0.6), BAR, p, p.Z, 2, 3)
        if p.face == "front":
            return fe(CHESTNUT, p, 3) if p.fy < 0.35 else fe(WHITE, p, 4, 0.03)  # chestnut breast band
        if p.face == "bottom" or p.face == "back":
            return fe(WHITE, p, 5, 0.03)
        if p.fy < 0.6:
            return barred(GINGER, BAR, p, p.Z, 6, 3)
        return fe(WHITE, p, 7, 0.03)

    def neck(p):
        if p.face == "front" and p.fy > 0.65:
            return fe(CHESTNUT, p, 8)
        return fe(lerp(GREY, GREY_D, 0.4 if p.face == "back" else 0), p, 9, 0.04)

    def head(p):
        if is_eye(p, S):
            return (36, 28, 22)
        if p.face == "top" and abs(p.x) < 0.6:
            return fe(DARK, p, 10)  # dark stripe over the crown
        return fe(GREY, p, 11, 0.04)

    def whisker(p):
        return fe(WHITE, p, 12, 0.02)

    def beak(p):
        return BEAK if p.face != "bottom" else shade(BEAK, 0.85)

    def wing(p):
        if not p.upper:
            return fe(WHITE, p, 13, 0.03)
        if p.s > 0.6:
            return feathers_along(p, DARK, 14)  # dark primaries
        if p.c > 0.65:
            return fe(WHITE, p, 15, 0.03)  # the big white wing panel (a white edge on the folded wing)
        return barred(GINGER, BAR, p, p.z, 16, 3) if p.c < 0.5 else fe(lerp(GREY, WHITE, 0.5), p, 17, 0.04)  # ginger coverts, grey greater coverts

    def tail(p):
        if p.fz > 0.8 or p.face == "bottom":
            return fe(WHITE, p, 18, 0.03)
        return barred((196, 112, 56), BAR, p, p.z, 19, 3)

    def leg(p):
        return fe(LEG, p, 20, 0.06)

    return dict(body=body, neck=neck, head=head, beak=beak, whisker=whisker, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# ======================================================================= eagle-owl
def owl_painters(S):
    TAWNY, TAWNY_L, STREAK, DARK = (176, 124, 66), (206, 160, 98), (58, 38, 22), (96, 66, 40)
    DISC, RIM, WHITE, EYE, PUPIL = (196, 160, 112), (66, 44, 28), (232, 222, 200), (240, 132, 26), (18, 12, 10)
    BEAK, CLAW = (34, 30, 30), (30, 26, 26)

    def body(p):
        if p.face == "front":
            if p.fy < 0.2 and abs(p.x) < 2:
                return fe(WHITE, p, 1, 0.03)  # pale throat
            c = TAWNY_L if p.fy > 0.3 else TAWNY
            if int(p.x * 2 + 7) % 3 == 0 and h(p.tx, p.ty, 2) < 0.8:
                c = STREAK  # bold dark streaks down the breast
            return fe(c, p, 3)
        if p.face == "bottom":
            return barred(TAWNY_L, DARK, p, p.Z, 4, 3)
        return mottle(lerp(TAWNY, DARK, 0.45), STREAK, p, 5, 0.35)  # dark mottled back

    def neck(p):
        return mottle(TAWNY, STREAK, p, 6, 0.3)

    def head(p):
        if p.face == "front":
            ey, ez = S["eye"]
            # the big forward-facing orange eyes in the facial disc (ix: 0..5 across the face)
            if p.iy == ey and p.ix in (1, 4):
                return EYE
            if p.iy == ey and p.ix in (2, 3):
                return RIM
            if p.iy == ey + 1 and p.ix in (1, 4):
                return PUPIL
            if p.iy == 0 or p.ix in (0, 5):
                return fe(RIM, p, 7)  # dark rim round the disc
            return fe(DISC, p, 8, 0.05)
        if p.face == "top":
            return mottle(TAWNY, STREAK, p, 9, 0.4)
        return mottle(lerp(TAWNY, DARK, 0.3), STREAK, p, 10, 0.35)

    def tuft(p):
        return fe(STREAK if p.iy == 0 else DARK, p, 11)

    def beak(p):
        return BEAK

    def wing(p):
        if not p.upper:
            return barred(TAWNY_L, DARK, p, p.y, 12, 3)
        if p.s > 0.45:
            return barred(lerp(TAWNY, DARK, 0.3), STREAK, p, p.y, 13, 2)  # barred flight feathers
        return mottle(lerp(TAWNY, DARK, 0.4), STREAK, p, 14, 0.4)

    def tail(p):
        return barred(lerp(TAWNY, DARK, 0.3), STREAK, p, p.z, 15, 2)

    def leg(p):
        return fe(TAWNY_L, p, 16, 0.05)  # feathered to the toes

    def foot(p):
        return CLAW if p.face == "front" else fe(TAWNY_L, p, 17, 0.05)

    return dict(body=body, neck=neck, head=head, tuft=tuft, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, foot=foot)


# ======================================================================= common raven
def raven_painters(S):
    BLACK, BLUE, PURPLE, SOOT = (20, 20, 26), (40, 50, 86), (56, 40, 76), (34, 34, 40)

    def gloss(p, salt, k=1.0):
        sheen = 0.5 if p.face == "top" else 0.28 if side(p) else 0.08
        tone = BLUE if h(p.tx // 2, p.ty, salt) < 0.6 else PURPLE
        return fe(lerp(BLACK, tone, sheen * k), p, salt + 1, 0.08)

    def body(p):
        return gloss(p, 1) if p.face != "bottom" else fe(SOOT, p, 2)

    def head(p):
        if is_eye(p, S):
            return (6, 6, 8)
        return gloss(p, 3)

    def beak(p):
        return (30, 30, 34) if p.face != "top" else (46, 46, 52)

    def hackle(p):
        return fe(BLACK, p, 4, 0.12)  # the shaggy throat

    def wing(p):
        if not p.upper:
            return fe(SOOT, p, 5)
        return feathers_along(p, lerp(BLACK, BLUE, 0.42 - 0.3 * p.s), 6)

    def tail(p):
        return gloss(p, 7, 0.8)

    def leg(p):
        return fe((22, 22, 24), p, 8, 0.1)

    return dict(body=body, neck=lambda p: gloss(p, 9), head=head, beak=beak, hackle=hackle, wing=wing, tip=wing, tail=tail,
                leg=leg, foot=leg)


# ======================================================================= golden eagle
def eagle_painters(S):
    BROWN, BROWN_D, GOLD, GOLD_L = (82, 56, 36), (58, 40, 26), (196, 146, 70), (222, 176, 98)
    COVERT, GREYBAR = (132, 98, 62), (110, 100, 92)
    CERE, HOOK, FOOT, TALON = (232, 196, 70), (44, 40, 40), (226, 190, 70), (30, 26, 26)

    def brown(p, salt, k=0.0):
        return fe(lerp(BROWN, BROWN_D, k + 0.25 * h(p.tx, p.ty, salt)), p, salt + 1, 0.06)

    def body(p):
        if p.face == "top" and p.fz < 0.3:
            return fe(lerp(GOLD, BROWN, 0.4), p, 1)  # the gold runs down onto the mantle
        if p.face == "bottom":
            return brown(p, 2, 0.4)
        return brown(p, 3, 0.15)

    def neck(p):
        return fe(GOLD if p.face != "front" else lerp(GOLD, BROWN, 0.5), p, 4, 0.06)  # golden nape

    def head(p):
        if is_eye(p, S):
            return (120, 70, 24)
        if side(p) and p.iy == S["eye"][0] - 1 and p.iz <= S["eye"][1]:
            return BROWN_D  # the heavy brow
        if p.face in ("top", "back") or side(p) and p.fz > 0.4:
            return fe(lerp(GOLD, GOLD_L, h(p.tx, p.ty, 5)), p, 6, 0.05)  # golden crown and nape
        return brown(p, 7)

    def beak(p):
        return CERE if p.fz > 0.5 else (70, 66, 64)  # yellow cere at the base, dark horn ahead

    def hook(p):
        return HOOK

    def wing(p):
        if not p.upper:
            return brown(p, 8, 0.35)
        if p.s < 0.45 and p.c < 0.45:
            return mottle(COVERT, BROWN, p, 9, 0.3)  # the pale band across the upper wing
        return feathers_along(p, lerp(BROWN, BROWN_D, 0.3 + 0.4 * p.s), 10)

    def tail(p):
        return barred(BROWN, GREYBAR, p, p.z, 11, 3) if p.fz < 0.75 else brown(p, 12, 0.5)

    def leg(p):
        return brown(p, 13, 0.0)  # feathered "trousers"

    def foot(p):
        return TALON if p.face == "front" else fe(FOOT, p, 14, 0.05)

    return dict(body=body, neck=neck, head=head, beak=beak, hook=hook, wing=wing, tip=wing, tail=tail, leg=leg, foot=foot)


# ======================================================================= griffon vulture
def vulture_painters(S):
    SAND, SAND_L, SAND_D = (176, 142, 100), (204, 172, 128), (140, 108, 74)
    DARK, BLACK, DOWN, RUFF = (70, 54, 40), (34, 28, 26), (226, 222, 214), (244, 240, 230)
    BEAK, LEG = (186, 176, 150), (138, 140, 140)

    def body(p):
        if p.face == "bottom":
            return mottle(SAND_D, DARK, p, 1, 0.2)
        if p.face == "front":
            return mottle(SAND_L, SAND, p, 2, 0.4)
        return mottle(SAND, SAND_D, p, 3, 0.3)

    def neck(p):
        return fe(lerp(DOWN, (196, 180, 170), h(p.tx, p.ty, 4) * 0.5), p, 5, 0.04)  # bare, pinkish-grey down

    def head(p):
        if is_eye(p, S):
            return (190, 140, 50)
        return fe(DOWN, p, 6, 0.04)

    def beak(p):
        return BEAK if p.fz > 0.4 else (60, 56, 52)

    def ruff(p):
        return fe(RUFF, p, 7, 0.06)

    def wing(p):
        if not p.upper:
            return fe(lerp(SAND, (230, 220, 200), 0.4) if p.s < 0.5 else BLACK, p, 8)
        if p.s > 0.5 or p.c > 0.6:
            return feathers_along(p, lerp(DARK, BLACK, (p.s - 0.4)), 9)  # dark flight feathers
        return mottle(SAND, SAND_L, p, 10, 0.3)  # pale buff coverts

    def tail(p):
        return fe(BLACK, p, 11)

    def leg(p):
        return fe(LEG, p, 12, 0.05) if p.iy < S["leg"] - 1 else fe(lerp(LEG, SAND, 0.2), p, 13)

    return dict(body=body, neck=neck, head=head, beak=beak, ruff=ruff, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


TEXTURES = {
    "capercaillie": ("capercaillie", capercaillie_painters),
    "capercaillie_hen": ("capercaillie", capercaillie_hen_painters),
    "great_bustard": ("great_bustard", bustard_painters),
    "owl": ("owl", owl_painters),
    "raven": ("raven", raven_painters),
    "golden_eagle": ("golden_eagle", eagle_painters),
    "vulture": ("vulture", vulture_painters),
}
# plucked: the dark game skin of a grouse, the paler skin of the bustard
PLUCKED = {
    "capercaillie": ("capercaillie", capercaillie_painters, (226, 164, 146), (172, 104, 92)),
    "capercaillie_hen": ("capercaillie", capercaillie_hen_painters, (226, 164, 146), (172, 104, 92)),
    "great_bustard": ("great_bustard", bustard_painters, (238, 200, 168), (196, 144, 112)),
}


def plucked(S, painters, light, dark):
    d = gbt.plucked_painters(S, painters, light, dark)
    for key in ("beard", "whisker"):
        d[key] = d["neck"]  # plucked bare like the rest
    return d


def entities():
    built, layers = {}, []
    for species, S in SPECIES.items():
        parts = geometry(S)
        uv = pack(parts, *SIZE)
        built[species] = (parts, uv)
        layers.append(gbt.java_layer(species, parts, uv))
    for tex, (species, painters) in TEXTURES.items():
        parts, uv = built[species]
        S = SPECIES[species]
        save(gbt.paint(parts, uv, S, painters(S)), f"wildlife/{tex}.png")
    for tex, (species, painters, light, dark) in PLUCKED.items():
        parts, uv = built[species]
        S = SPECIES[species]
        save(gbt.paint(parts, uv, S, plucked(S, painters(S), light, dark)), f"skinned/wildlife/{tex}.png")
    src = MODEL_JAVA.read_text(encoding="utf-8")
    begin, end = "    // BEGIN GENERATED by scripts/gen_fauna_forestbirds_textures.py\n", "    // END GENERATED\n"
    assert begin in src and end in src, "markers missing in ForestBirdModel.java"
    new = re.sub(re.escape(begin) + ".*?" + re.escape(end), lambda m: begin + "\n\n".join(layers) + "\n" + end, src, flags=re.S)
    MODEL_JAVA.write_text(new, encoding="utf-8")
    print("wrote", MODEL_JAVA.relative_to(ROOT))


# ======================================================================= items
def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def grid(rows, pal):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), (*hexc(pal[ch]), 255))
    return im


def save_item(im, rel):
    out = ITEM_TEX / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    print("wrote", out.relative_to(ROOT))


# meat: o outline, d dark, m mid, l light, h highlight, s skin, b bone, B bone shade
RAW = {
    "capercaillie": dict(o="#3a1014", d="#681c22", m="#8e2c32", l="#b04e4a", h="#cc7a6c", s="#dcbca0", b="#eee6d2", B="#c8bca4"),
    "bustard": dict(o="#4a1a1c", d="#86302e", m="#ac4a44", l="#cc7466", h="#e4a090", s="#ecd0b0", b="#eee6d2", B="#c8bca4"),
}
COOKED = {
    "capercaillie": dict(o="#30180a", d="#643218", m="#8c4e26", l="#ae6e36", h="#cc9652", s="#c08844", b="#e8dcc0", B="#bcae90"),
    "bustard": dict(o="#3a1e0c", d="#784020", m="#a2602e", l="#c4823e", h="#e0aa5e", s="#d49a4a", b="#e8dcc0", B="#bcae90"),
}
CUTS = {
    "capercaillie": [  # the dark breast fillet, a thick wedge with its skin along the top
        "................",
        "................",
        "................",
        ".....ooooo......",
        "...oosssssoo....",
        "..osshhllmmoo...",
        "..oshhllmmmmdo..",
        ".oshllmmmmmmddo.",
        ".oslmmmmmmmmddo.",
        ".oslmmmmmmmdddo.",
        "..olmmmmmmddddo.",
        "..oommmmmddddo..",
        "....ooddddddoo..",
        "......oooooo....",
        "................",
        "................",
    ],
    "bustard": [  # a great long drumstick and thigh
        "................",
        ".......ooooo....",
        ".....oosssssoo..",
        "....osshhllmmdo.",
        "...oshhlllmmmdo.",
        "...oshllmmmmmddo",
        "...osllmmmmmmddo",
        "...oslmmmmmmddo.",
        "....ommmmmmddo..",
        ".....oommmddo...",
        "......ommddo....",
        ".....obodoo.....",
        "....obbbo.......",
        "...obbBo........",
        "..obBBo.........",
        "...ooo..........",
    ],
}
SKIN = dict(o="#7e5246", s="#ecc4a6", l="#f6dcc4", d="#c89478", f="#d4a488", q="#e8dcc0")
PLUCKED_ITEMS = {
    "capercaillie": ([  # big and dark-skinned, the pale hooked bill, red brow, stubs of the fan
        "................",
        "................",
        "...ooo..........",
        "..orroo.........",
        ".kesslo.........",
        "..oslloooooo....",
        "..osllsssssooo..",
        ".osllsfsssssdqq.",
        ".oslsssfssssdqqq",
        ".ossfssssfssdqq.",
        "..ossssssssddo..",
        "...oddsssdddo...",
        "....ooogoggoo...",
        "......oGGoGG....",
        "................",
        "................",
    ], dict(SKIN, s="#dca088", l="#ecbca4", d="#b47664", f="#c4887a", o="#683a30", k="#e2dac4", e="#1c1410", r="#c8242a",
            g="#8a7e70", G="#625a50")),
    "bustard": ([  # long neck and legs, big pale body
        "................",
        ".ooo............",
        "okeso...........",
        ".ooso...........",
        "...osd..........",
        "...osd..........",
        "...oslo.........",
        "...osloooooooo..",
        "..oslllssssssdo.",
        "..oslsfssssfsddo",
        "..ossssssfssssqo",
        "...osfssssssddo.",
        "....odddddddoo..",
        ".......ogogo....",
        ".......og.og....",
        "......oGG.GGo...",
    ], dict(SKIN, k="#9a8e78", e="#241c16", g="#8a7656", G="#665638")),
}
EGGS = {
    "capercaillie": ([  # buff-yellow, finely spotted red-brown
        "................",
        "................",
        "................",
        "......oooo......",
        ".....owwxwo.....",
        "....owlhwwyo....",
        "....owhwxwwo....",
        "...owlwwwywxo...",
        "...oxwwwwwwdo...",
        "...owwywxwwdo...",
        "...owwwwwwydo...",
        "...odxwwwwddo...",
        "....odwwywdo....",
        ".....odddddo....",
        "......ooooo.....",
        "................",
    ], dict(o="#5e4426", w="#d8c08a", l="#e6d2a2", h="#f4e6c4", d="#b89e6a", x="#7a3a1c", y="#9a5a30")),
    "bustard": ([  # big, olive-green blotched brown
        "................",
        "......oooo......",
        ".....owwwwo.....",
        "....owlhwxwo....",
        "...owlhwwyxdo...",
        "...owlwwwwwdo...",
        "..owlwxxwwwwdo..",
        "..owwwywwwxwdo..",
        "..owwwwwwyxwdo..",
        "..owxwwwwwwddo..",
        "..owyxwwwwwddo..",
        "...owwwwxwddo...",
        "...odwwwyxddo...",
        "....oddddddo....",
        ".....oooooo.....",
        "................",
    ], dict(o="#3e4224", w="#8e9460", l="#a6ac78", h="#bec494", d="#6e7446", x="#5a4024", y="#74583a")),
}
# nest egg shells: (base, speckle)
SHELLS = {"capercaillie": ("#d8c08a", "#7a3a1c"), "bustard": ("#8e9460", "#5a4024")}

FEATHERS = {
    "capercaillie_tail_feather": ([  # broad, black with a white fleck, the quill pale
        "................",
        "............oo..",
        "...........obbo.",
        "..........obbbo.",
        ".........obwbbo.",
        "........obbbbo..",
        ".......obbbbqo..",
        "......obbbbqo...",
        ".....obbwbqo....",
        "....obbbbqo.....",
        "...obbbbqo......",
        "...obbbqo.......",
        "....ooqo........",
        "....qqo.........",
        "...qq...........",
        "................",
    ], dict(o="#0e0e10", b="#26262a", w="#e8e6e0", q="#d8d0c0")),
    "owl_feather": ([  # soft, tawny with dark bars and a frayed (silent) fringe
        "................",
        "...........o.o..",
        "..........otato.",
        ".........otdtto.",
        "........ottttdo.",
        ".......odtttqo..",
        "......otttdqo...",
        ".....ottdtqo....",
        "....odtttqo.....",
        "...otttdqo......",
        "..otdttqo.......",
        "..ottqoo........",
        "...ooq..........",
        "....q...........",
        "...q............",
        "................",
    ], dict(o="#6a4a2c", t="#c89a5e", d="#5a3c22", a="#e6c896", q="#ece0c8")),
    "raven_feather": ([  # long and glossy black with a blue sheen
        "................",
        ".............o..",
        "............obo.",
        "...........obbo.",
        "..........obsbo.",
        ".........obsbo..",
        "........obsbqo..",
        ".......obsbqo...",
        "......obsbqo....",
        ".....obbbqo.....",
        "....obbbqo......",
        "...obbbqo.......",
        "...obbqo........",
        "....ooq.........",
        "....q...........",
        "...q............",
    ], dict(o="#08080c", b="#1c1c26", s="#3a4874", q="#4a4a56")),
    "eagle_feather": ([  # big stiff flight feather, dark brown banded grey, a pale tip
        "..............o.",
        ".............olo",
        "............obbo",
        "...........obgbo",
        "..........obbbo.",
        ".........obgbqo.",
        "........obbbqo..",
        ".......obgbqo...",
        "......obbbqo....",
        ".....obgbqo.....",
        "....obbbqo......",
        "...obbbqo.......",
        "...obbqo........",
        "....ooq.........",
        "...qq...........",
        "..q.............",
    ], dict(o="#2a1c12", b="#5a3e28", g="#8a7c6c", l="#d8c8a8", q="#e2d6bc")),
}

MISC = {
    "owl_pellet": ([  # a grey felt of fur with little bones poking out
        "................",
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "...ooffgffwoo...",
        "..offgffbffgfo..",
        ".offwfgfffgffdo.",
        ".ofgffffbbffgdo.",
        ".offfbffgffffdo.",
        "..odfgffffwfddo.",
        "...oodfffffddo..",
        ".....oodddoo....",
        "................",
        "................",
        "................",
    ], dict(o="#3a3632", f="#7a726a", g="#958c82", d="#5a544e", b="#e8e2d2", w="#d0c8b6")),
    "raven_talisman": ([  # three raven feathers bound with twine to a bone bead on a cord
        "...c........c...",
        "....c......c....",
        ".....c....c.....",
        "......cccc......",
        "......owwo......",
        "......oWWo......",
        ".....oyyyyo.....",
        "....okbkbkbo....",
        "....okbskbko....",
        "....okbskbko....",
        "....okbskbko....",
        ".....kbskbk.....",
        ".....kbk.bk.....",
        "......k...k.....",
        "................",
        "................",
    ], dict(c="#8a6a40", o="#3a2c1c", w="#eae2cc", W="#c8bea6", y="#b48a52", k="#08080c", b="#1e1e28", s="#3e4c78")),
    "eagle_talon": ([  # a great curved black claw on its yellow scaly toe
        "................",
        "................",
        "................",
        "...oooo.........",
        "..oyyyyo........",
        "..oyYyyyoo......",
        "...oyyYyyyo.....",
        "....ooyyyyko....",
        "......oykkkko...",
        ".......okkkKko..",
        "........okkKko..",
        "........okkko...",
        ".........okko...",
        "..........oko...",
        "...........o....",
        "................",
    ], dict(o="#2a2216", y="#e0b84a", Y="#b88e2c", k="#26221e", K="#5a5450")),
    "vulture_wing_bone": ([  # a long, light hollow wing bone (ulna), knobbed ends
        "................",
        ".............oo.",
        "............obbo",
        "...........obbBo",
        "..........obbBo.",
        ".........obbBo..",
        "........obbBo...",
        ".......obbBo....",
        "......obbBo.....",
        ".....obbBo......",
        "....obbBo.......",
        "...obbBo........",
        "..obbBBo........",
        ".obbBBo.........",
        ".oBBoo..........",
        "..oo............",
    ], dict(o="#6a6050", b="#f0e8d4", B="#c8bca0")),
    "eagle_fletched_arrow": ([  # flint tip, shaft, broad brown-and-grey eagle fletching
        "................",
        ".............ooo",
        "............ofFo",
        "...........ofFFo",
        "..........osfFo.",
        ".........osso...",
        "........osso....",
        ".......osso.....",
        "......osso......",
        "..bb.osso.......",
        ".bgbosso........",
        ".bbgsso.........",
        "..bgbso.........",
        ".bgbgb..........",
        "bgb.bb..........",
        "bb..............",
    ], dict(o="#2a2218", f="#4a4a52", F="#6e6e78", s="#8a6a42", b="#5a3e28", g="#9a8c7c")),
    "silent_arrow": ([  # flint tip, shaft, soft barred tawny owl fletching
        "................",
        ".............ooo",
        "............ofFo",
        "...........ofFFo",
        "..........osfFo.",
        ".........osso...",
        "........osso....",
        ".......osso.....",
        "......osso......",
        "..tt.osso.......",
        ".tdtosso........",
        ".ttdsso.........",
        "..tdtso.........",
        ".tdtdt..........",
        "tdt.tt..........",
        "tt..............",
    ], dict(o="#2a2218", f="#4a4a52", F="#6e6e78", s="#8a6a42", t="#d0a466", d="#6a4a2c")),
}


def shell(base, spot):
    im = Image.new("RGBA", (16, 16))
    b, s = hexc(base), hexc(spot)
    for y in range(16):
        for x in range(16):
            r = h(x, y, 81)
            c = lerp(b, (255, 255, 255), 0.25) if y < 4 else b
            if r < 0.14 or h(x // 2, y // 2, 82) < 0.08:
                c = lerp(c, s, 0.75 if r < 0.08 else 0.5)
            im.putpixel((x, y), (*c, 255))
    return im


def spawn_eggs():
    import importlib.util
    spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
    wit = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(wit)
    K = "#141416"
    return {
        # slate-grey cock: green breast band, red brow, pale bill
        "capercaillie_spawn_egg": wit.egg(["#6a6c72", "#55575e", "#43454c", "#34363c", "#26272c", "#18181c"],
                                          [(x, 8, "#2e5e48") for x in range(3, 13)] + [(x, 9, "#3c7458") for x in range(4, 12)]
                                          + [(5, 11, "#e8e6e0"), (10, 12, "#e8e6e0")],
                                          (), [(6, 3, "#c8242a"), (9, 3, "#c8242a"), (7, 4, "#e2dac4"), (8, 4, "#e2dac4")]),
        # ginger barred back, grey head, white belly
        "great_bustard_spawn_egg": wit.egg(["#d8a466", "#c08446", "#a46c36", "#86562a", "#5e3c1e", "#3e2814"],
                                           [(x, y, "#3a2618") for y in (5, 7, 9) for x in range(3, 13, 3)]
                                           + [(x, y, "#f0eee8") for y in range(11, 15) for x in range(4, 12)],
                                           (), [(x, 2, "#a8acb4") for x in range(6, 10)] + [(x, 3, "#a8acb4") for x in range(5, 11)]),
        # tawny, dark streaks, two orange eyes
        "owl_spawn_egg": wit.egg(["#d8aa6c", "#c0904e", "#a4763c", "#865e2e", "#5e3e1e", "#3e2814"],
                                 [(x, y, "#3a2614") for x in (4, 7, 10) for y in (8, 10, 12)],
                                 [(5, 0), (10, 0)], [(5, 0, "#3a2614"), (10, 0, "#3a2614"), (6, 4, "#f08420"), (9, 4, "#f08420"),
                                                     (6, 5, K), (9, 5, K), (7, 6, "#222020"), (8, 6, "#222020")]),
        # glossy black with blue
        "raven_spawn_egg": wit.egg(["#3a4468", "#262c44", "#1c1e2c", "#141620", "#0c0c12", "#060608"],
                                   [(5, 6, "#4a5888"), (9, 9, "#4a5888"), (6, 12, "#2e3654")], (), [(7, 4, "#30303a"), (8, 4, "#30303a")]),
        # dark brown, golden head
        "golden_eagle_spawn_egg": wit.egg(["#7a5638", "#62442a", "#4e3622", "#3c2a1a", "#281c12", "#1a120c"],
                                          [(x, y, "#d4a456") for y in range(1, 5) for x in range(4, 12)],
                                          (), [(7, 5, "#e8c446"), (8, 5, "#e8c446"), (8, 6, "#2c2828")]),
        # sandy buff, white down head and ruff, dark flight feathers
        "vulture_spawn_egg": wit.egg(["#d4b080", "#bc9666", "#a07c50", "#84623e", "#5c4228", "#3e2c1a"],
                                     [(x, y, "#ece8de") for y in range(1, 4) for x in range(5, 11)]
                                     + [(x, 5, "#f6f2ea") for x in range(3, 13)]
                                     + [(x, y, "#3a2c22") for y in range(11, 15) for x in (3, 4, 11, 12)],
                                     (), [(7, 2, "#c08c34")]),
    }


def items():
    for bird, rows in CUTS.items():
        save_item(grid(rows, RAW[bird]), f"item/raw_{bird}.png")
        save_item(grid(rows, COOKED[bird]), f"item/cooked_{bird}.png")
    for bird, (rows, pal) in PLUCKED_ITEMS.items():
        save_item(grid(rows, pal), f"item/plucked_{bird}.png")
    for bird, (rows, pal) in EGGS.items():
        save_item(grid(rows, pal), f"item/{bird}_egg.png")
    for bird, (base, spot) in SHELLS.items():
        save_item(shell(base, spot), f"block/bird_egg_{bird}.png")
    for name, (rows, pal) in {**FEATHERS, **MISC}.items():
        save_item(grid(rows, pal), f"item/{name}.png")
    for name, im in spawn_eggs().items():
        save_item(im, f"item/{name}.png")


if __name__ == "__main__":
    entities()
    items()
