"""The older "hunt" creatures' models and skins from one table each (the gen_cold_models.py pattern, whose Model,
painter and helpers this reuses):

- src/main/java/com/mechanical/client/wildlife/coldhunters/HuntGeometry.java: Wolverine, Lynx, Ermine, Pine Marten,
  Lemming (generated, do not edit). The hand-written models keep the animation (PredatorModel for the four hunters,
  LemmingModel), so the part trees keep their names and pivots. Ermine, Pine Marten and Lemming are built at twice their
  size and drawn at half (ColdhuntersClient), for finer boxes and texels.
- src/main/java/com/mechanical/client/wildlife/night/MushroomGeometry.java: the Mushroom Man.
- src/main/java/com/mechanical/client/wildlife/underground/UndergroundGeometry.java: Giant Centipede, Vittra, Troll,
  Lindworm (GiantCentipedeModel, FolkModel and LindwormModel animate them by part name).
- textures/entity/wildlife/<name>.png (+ lynx_kitten, ermine_winter), the skinned carcass skins
  (textures/entity/skinned/wildlife/) and textures/entity/night/mushroom_man.png.

Every texel is painted from the model-space point it covers at rest (y down, ground at 24, -z the front, +x the
animal's left), so markings run on across boxes: the wolverine's pale side band, the lynx's spots and barred ruff.

Usage: python scripts/gen_hunt_models.py (also run by gen_fauna_coldhunters_textures.py,
gen_fauna_night_textures.py and gen_fauna_underground_textures.py, whose old box tables it replaces). Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_cold_models import Model, belly_shade, cells, h, h3, lerp, ragged, save, shade, skinned, smooth, strands  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA_DIR = ROOT / "src/main/java/com/mechanical/client/wildlife"
LOWER = {f"{s}_{l}_lower": "lower" for s in ("right", "left") for l in ("front", "hind")}


def near(T, x, y, z, r=0.6):
    return abs(T.p[0] - x) < r and abs(T.p[1] - y) < r and abs(T.p[2] - z) < r


def round_ear(T):
    """Rounded ear tips: the top row of the narrow sides, and the top corners of the broad faces, see-through."""
    if T.face in ("left", "right"):
        return T.j == 0
    if T.face in ("top", "bottom"):
        return T.fw >= 3 and T.i in (0, T.fw - 1)
    return T.j == 0 and T.fw >= 3 and T.i in (0, T.fw - 1)


def legs(m, lx, front, hind):
    """Four legs, each with a "lower" child. front/hind: (pivot y, z, upper boxes, lower pivot (y, z), lower boxes)."""
    for s, side in ((-1, "right"), (1, "left")):
        for name, (py, pz, upper, (ly, lz), lower) in (("front", front), ("hind", hind)):
            leg = m.part(f"{side}_{name}_leg", at=(s * lx, py, pz))
            for b in upper:
                m.box(leg, *b)
            lo = m.part(f"{side}_{name}_lower", leg, (0, ly, lz))
            for b in lower:
                m.box(lo, *b)


# ======================================================================= Wolverine (PredatorModel)
def wolverine():
    m = Model("Wolverine", "wolverine", 128, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 15, 0))
    A("body", "chest", -3.5, 12, -7, 7, 7, 6)
    A("body", "barrel", -3.5, 11.5, -2, 7, 7, 5)
    A("body", "rump", -3.5, 10.5, 3, 7, 8, 5)
    A("body", "back", -3, 10, 1, 6, 1, 7)          # the high humped loins
    A("body", "back", -3, 11, -6, 6, 1, 7)
    A("body", "fringe", -3.5, 18.5, -6, 7, 1, 11)   # shaggy hair under the belly
    A("body", "rump", -3, 11.5, 8, 6, 6, 1)
    A("body", "neck", -2.5, 12, -10, 5, 5, 3)
    m.part("tail", "body", (0, -3.5, 8.5), (1.0, 0, 0))
    B("tail", "tail", -1.5, 0, -1.5, 3, 3, 3)
    m.part("tail_mid", "tail", (0, 2.5, 0), (-0.25, 0, 0))
    B("tail_mid", "tail", -2, 0, -2, 4, 4, 4)
    m.part("tail_tip", "tail_mid", (0, 3.5, 0), (-0.2, 0, 0))
    B("tail_tip", "tail", -1.5, 0, -1.5, 3, 3, 3)
    m.part("head", "body", (0, -1.5, -8.5))
    B("head", "skull", -3, -2.5, -4, 6, 5, 4)
    B("head", "brow", -2.5, -3, -3.5, 5, 1, 3)
    B("head", "cheek", -3.5, -0.5, -3.5, 7, 3, 3)
    B("head", "muzzle", -1.5, 0, -6.5, 3, 2, 3)
    B("head", "nose", -1, -0.2, -7, 2, 1, 1)
    m.part("jaw", "head", (0, 2, -4))
    B("jaw", "jaw", -1.5, 0, -2.5, 3, 1, 3)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.6, -2.4, -1.2), (0, 0, s * 0.3))
        B(n, "ear", -1, -1.5, -0.5, 2, 2, 1)
    legs(m, 2.6,
         (17, -4.5, [("upper", -1.5, -2, -2, 3, 5, 4)], (3, 0),
          [("shank", -1.5, 0, -1.5, 3, 3, 3), ("paw", -2, 3, -2.5, 4, 1, 4), ("claws", -1.5, 3, -3.5, 3, 1, 1)]),
         (16, 5, [("thigh", -2, -2.5, -2.5, 4, 6, 5)], (4, 0.5),
          [("shank", -1.5, 0, -1.5, 3, 3, 3), ("paw", -2, 3, -3, 4, 1, 5), ("claws", -1.5, 3, -4, 3, 1, 1)]))
    return m


WV = dict(fur=(62, 42, 30), dark=(34, 24, 19), light=(92, 64, 44), band=(176, 134, 86), band_dk=(132, 96, 60),
          brow=(150, 136, 116), mask=(30, 22, 18), black=(18, 14, 12), claw=(222, 212, 190), pad=(36, 30, 28),
          throat=(196, 160, 112), eye=(20, 14, 12), shine=(120, 110, 100), ear=(120, 100, 80))


def wolverine_colour(T):
    P, mat, (x, y, z) = WV, T.mat, T.p
    if mat == "claws":
        if T.face in ("front", "top", "bottom") and T.i % 2 == 1:
            return P["pad"]  # the gaps between the five long claws
        return P["claw"] if T.face in ("front", "top") else shade(P["claw"], 0.75)
    if mat == "nose":
        return P["black"]
    if mat == "fringe" and ragged(T, 0.5):
        return None
    if mat == "paw" and T.face == "bottom":
        return P["pad"]
    c = strands(P["fur"], P["dark"], P["light"], T, 1.0)
    c = belly_shade(c, T, 10, 22, P["dark"], 0.55)
    # legs and feet nearly black
    if mat in ("upper", "thigh", "shank", "paw"):
        c = lerp(c, P["dark"], 0.35 + 0.4 * smooth((y - 17) / 5))
    # the pale band: from the shoulder along each flank, rising over the hips and meeting across the rump at the tail
    if mat in ("chest", "barrel", "rump", "back", "thigh", "upper") and z > -6:
        top = 13.2 - 1.6 * smooth((z + 4) / 10)
        width = 2.2 + 0.5 * math.sin(z * 0.7)
        side = T.face in ("left", "right") or (T.face == "back" and abs(x) > 1.5)
        if side and top < y < top + width or (mat == "rump" and T.face in ("top", "back") and z > 6 and abs(x) < 3.5 and y < 13):
            k = 1 - abs((y - top - width / 2) / width) * 1.4 if side else 0.7
            c = lerp(c, lerp(P["band_dk"], P["band"], 0.4 + 0.6 * T.noise(3)), 0.55 + 0.4 * max(0, k))
    if mat == "tail" and T.lp[1] < 2 and T.face != "bottom":
        c = lerp(c, P["band_dk"], 0.35)
    # a pale throat blaze, broken into blotches
    if mat in ("chest", "neck") and (T.face == "bottom" or (T.face == "front" and y > 16)) and h3(x * 1.5, y, z * 1.5, 7) > 0.45:
        c = lerp(c, P["throat"], 0.75)
    # the head: dark mask, silver-grey brow from ear to ear
    if mat in ("skull", "brow", "cheek", "muzzle", "jaw", "ear"):
        c = lerp(c, P["mask"], 0.25)
        if mat == "brow" or (mat == "skull" and (T.face == "top" or (T.side and y < 12.0 and z > -12.3))):
            c = lerp(c, P["brow"], 0.65 + 0.2 * T.noise(5))
        if mat == "cheek" and T.side and T.face != "front" and y < 14.2:
            c = lerp(c, P["brow"], 0.45)
        if mat == "ear":
            return lerp(P["brow"], P["ear"], 0.4) if T.face == "front" and T.j == 1 else lerp(c, P["brow"], 0.5)
        if mat == "muzzle" and T.face == "front" and T.j == 1:
            return lerp(P["mask"], P["black"], 0.5)
        if mat == "jaw" and T.side and T.j == 0:
            return P["black"]
        # small dark eyes with a glint, low on the face
        if mat == "skull" and T.face == "front" and abs(y - 12.5) < 0.6 and abs(abs(x) - 1.5) < 0.6:
            return P["shine"] if x < 0 else P["eye"]
        if mat == "skull" and T.face == "front" and abs(y - 12.5) < 0.6 and abs(abs(x) - 2.5) < 0.6:
            return P["eye"]
    return c


# ======================================================================= Lynx (PredatorModel)
def lynx():
    m = Model("Lynx", "lynx", 128, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 11, 0))
    A("body", "chest", -3, 7.5, -7.5, 6, 7, 6)
    A("body", "belly", -3, 8, -2, 6, 6, 5)
    A("body", "rump", -3, 7, 3, 6, 7, 5)
    A("body", "back", -2.5, 6.5, 0, 5, 1, 7)
    A("body", "back", -2.5, 7, -7, 5, 1, 7)
    A("body", "skinfold", -2.5, 14, -6, 5, 1, 10)
    A("body", "neck", -2.5, 7, -10, 5, 6, 3)
    m.part("tail", "body", (0, -3, 7.5), (1.1, 0, 0))
    B("tail", "tail", -1.5, 0, -1.5, 3, 4, 3)
    m.part("head", "body", (0, -3.5, -9.5))
    B("head", "skull", -3, -3, -4, 6, 5, 4)
    B("head", "brow", -2.5, -3.5, -3.5, 5, 1, 3)
    B("head", "ruff", -4.5, -0.5, -3, 9, 4, 2)      # the hanging cheek ruffs
    B("head", "ruff_tip", -4.5, 3.5, -3, 2, 1, 2)
    B("head", "ruff_tip", 2.5, 3.5, -3, 2, 1, 2)
    B("head", "muzzle", -1.5, -0.5, -6, 3, 2, 2)
    B("head", "nose", -1, -0.8, -6.5, 2, 1, 1)
    m.part("jaw", "head", (0, 1.5, -4))
    B("jaw", "jaw", -1.5, 0, -2, 3, 1, 2)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.1, -2.8, -1.5), (-0.1, 0, s * 0.15))
        B(n, "ear", -1, -3, -0.5, 2, 3, 1)
        B(n, "tuft", -0.5, -5, -0.5, 1, 2, 1)
    legs(m, 2.2,
         (13, -4.5, [("upper", -1.5, -1, -2, 3, 6, 4)], (5, 0),
          [("shank", -1, 0, -1, 2, 5, 2), ("paw", -2, 5, -2.5, 4, 1, 4)]),
         (12, 5.5, [("thigh", -1.5, -1.5, -2.5, 3, 7, 5)], (6, 0.5),
          [("shank", -1, 0, -1, 2, 5, 2), ("paw", -2, 5, -3, 4, 1, 4)]))
    return m


LX = dict(fur=(168, 150, 126), warm=(184, 146, 102), dark=(124, 108, 90), light=(212, 196, 172), white=(238, 232, 220),
          spot=(62, 46, 34), black=(26, 22, 20), eye=(196, 176, 72), pupil=(30, 26, 20), nose=(176, 120, 112), pad=(64, 54, 50))
LX_KIT = dict(LX, fur=(190, 166, 132), warm=(196, 162, 118), dark=(150, 128, 104), spot=(104, 82, 62))


def lynx_colour(P, kit):
    def colour(T):
        mat, (x, y, z) = T.mat, T.p
        if mat == "paw" and T.face == "bottom":
            return P["pad"]
        if mat == "nose":
            return P["nose"] if T.face in ("front", "top") else shade(P["nose"], 0.7)
        if mat == "tuft":
            return P["black"]
        if mat in ("skinfold", "ruff_tip") and ragged(T, 0.5):
            return None
        base = lerp(P["fur"], P["warm"], 0.5 * smooth((10 - y) / 4))
        c = strands(base, P["dark"], P["light"], T, 0.8 if not kit else 1.1)
        white = 0.0
        if mat in ("chest", "belly", "rump", "neck", "skinfold"):
            white = smooth((y - 12.5) / 2.0)
        if mat in ("upper", "thigh", "shank") and T.face in ("left", "right") and x * (1 if T.face == "left" else -1) < 0:
            white = 0.7
        if mat in ("muzzle", "jaw"):
            white = 0.85 if T.face != "top" else 0.3
        if mat == "paw":
            white = 0.35
        c = lerp(c, P["white"], white)
        # dark spots: bold on the legs and lower flanks, small and faded on the back
        if mat in ("chest", "belly", "rump", "back", "neck", "upper", "thigh", "shank") and white < 0.6:
            legish = mat in ("upper", "thigh", "shank")
            d, v = cells(T.p, 1.9 if legish else 2.3, 21)
            strong = 0.8 if y > 9.5 or legish else 0.4
            if d < (0.6 + 0.3 * v) * (1.2 if kit else 1.0) and v > 0.15:
                c = lerp(c, P["spot"], strong)
        if mat == "tail":
            c = lerp(c, P["black"], 0.9) if T.lp[1] > 2.5 else c
            if T.face == "bottom" or (T.face == "back" and T.lp[1] < 2.5):
                c = lerp(c, P["white"], 0.6)
        if mat in ("ruff", "ruff_tip"):
            # pale cheek ruffs, barred with black
            c = lerp(c, P["white"], 0.55)
            if T.face in ("left", "right") and T.j == 1 and h(T.i, T.k, 8) > 0.35:
                c = lerp(c, P["black"], 0.8)
            if mat == "ruff_tip" and T.face != "top":
                c = lerp(c, P["black"], 0.7)
        if mat in ("skull", "brow"):
            # dark lines from the eyes up over the forehead
            if T.face == "front" and abs(abs(x) - 0.5) < 0.6 and y < 0.5 + 5:
                c = lerp(c, P["spot"], 0.5)
            if T.face == "top" and int(abs(x) * 2) % 3 == 0 and h(T.i, T.j, 3) > 0.4:
                c = lerp(c, P["spot"], 0.55)
        if mat == "ear":
            if T.face == "back":
                return lerp(P["light"], P["white"], 0.4) if T.j == 1 else P["black"]  # black, a pale band across the middle
            if T.j == 0 and T.side and T.face in ("front", "back"):
                return P["black"]
            if T.face == "front":
                return lerp(P["white"], P["fur"], 0.35)
            return P["black"] if T.j == 0 else c
        # the eyes: yellow-green under the brow at the front of the skull, a black rim and pupil
        if mat == "skull":
            ey = 5.5  # eye row (model space, the head pivot is at y 4.0)
            if T.face == "front" and abs(y - ey) < 0.6:
                if abs(abs(x) - 1.5) < 0.6:
                    return P["eye"]
                if abs(abs(x) - 2.5) < 0.6:
                    return P["pupil"]
            if T.face == "front" and abs(y - ey - 1) < 0.6 and abs(abs(x) - 1.5) < 0.6:
                return lerp(P["white"], c, 0.3)
            if T.face == "front" and abs(y - ey - 1) < 0.6 and abs(abs(x) - 0.5) < 0.6:
                return lerp(P["spot"], c, 0.4)  # tear lines down to the nose
        if mat == "muzzle" and T.face == "front" and T.j == 1 and T.i == 1:
            return lerp(c, P["black"], 0.55)
        if mat == "muzzle" and T.side and T.face in ("left", "right") and T.j == 1 and h(T.i, T.j, T.k) > 0.3:
            return lerp(c, P["black"], 0.6)  # whisker spots
        if mat == "jaw" and T.side and T.j == 0:
            return lerp(c, P["black"], 0.6)
        return c
    return colour


# ======================================================================= Ermine (PredatorModel, built at 2x)
def ermine():
    m = Model("Ermine", "ermine", 64, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 19, 0))
    A("body", "chest", -2, 16.5, -7, 4, 4, 5)
    A("body", "belly", -2, 16, -2, 4, 4, 5)
    A("body", "haunch", -2, 16.5, 3, 4, 4, 4)
    A("body", "back", -1.5, 15.5, -3, 3, 1, 7)
    A("body", "neck", -1.5, 16, -9.5, 3, 3, 3)
    m.part("tail", "body", (0, -2.5, 7), (1.3, 0, 0))
    B("tail", "tail", -1, 0, -1, 2, 4, 2)
    m.part("tail_tip", "tail", (0, 4, 0), (-0.1, 0, 0))
    B("tail_tip", "tail_tip", -1, 0, -1, 2, 3, 2)
    m.part("head", "body", (0, -2.5, -9))
    B("head", "skull", -2, -2, -3.5, 4, 3, 4)
    B("head", "cheek", -2.5, -0.5, -3, 5, 2, 2)
    B("head", "snout", -1, -1, -5, 2, 2, 2)
    B("head", "nose", -0.5, -1.2, -5.5, 1, 1, 1)
    m.part("jaw", "head", (0, 1, -3.5))
    B("jaw", "jaw", -1, 0, -1.5, 2, 1, 2)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 1.6, -2, -0.8), (0, 0, s * 0.25))
        B(n, "ear", -1, -0.5, -0.5, 2, 1, 1)
    legs(m, 1.3,
         (20, -5, [("upper", -1, -1, -1, 2, 2, 2)], (1, 0),
          [("shank", -1, 0, -1, 2, 2, 2), ("paw", -1, 2, -1.5, 2, 1, 3)]),
         (19.5, 4.5, [("thigh", -1.5, -1.5, -1.5, 3, 3, 3)], (1.5, 0.5),
          [("shank", -1, 0, -1, 2, 2, 2), ("paw", -1, 2, -2, 2, 1, 3)]))
    return m


ER_S = dict(fur=(140, 92, 54), dark=(104, 66, 38), light=(170, 120, 76), under=(242, 234, 212))
ER_W = dict(fur=(246, 244, 236), dark=(220, 216, 204), light=(255, 255, 250), under=(250, 248, 240))
ER_TIP, ER_NOSE, ER_EYE = (22, 18, 16), (40, 30, 28), (14, 10, 10)


def ermine_colour(P, winter):
    def colour(T):
        mat, (x, y, z) = T.mat, T.p
        if mat == "tail_tip":
            return strands(ER_TIP, (10, 8, 8), (50, 44, 40), T, 0.6)
        if mat == "nose":
            return ER_NOSE if not winter else (60, 44, 40)
        c = strands(P["fur"], P["dark"], P["light"], T, 0.7 if not winter else 0.35)
        if not winter:
            # brown above, cream below: a clean wavy line down the flanks, the legs' insides and the feet pale
            line = 18.6 + 0.4 * math.sin(z * 0.8)
            pale = y > line and mat in ("chest", "belly", "haunch", "neck", "cheek", "jaw", "skull", "snout") \
                or (mat in ("upper", "thigh", "shank") and T.face in ("left", "right") and x * (1 if T.face == "left" else -1) < 0) \
                or mat == "paw" or (mat == "upper" and T.face == "front")
            if pale or T.face == "bottom":
                c = lerp(P["under"], (226, 214, 188), 0.3 * T.noise(2))
            if mat == "snout" and T.face in ("front", "left", "right") and y > 13.5 - 0.2:
                c = lerp(P["under"], c, 0.2)  # the white upper lip
        else:
            c = belly_shade(c, T, 15, 24, P["dark"], 0.3)
        if mat == "ear" and round_ear(T):
            return None
        if mat == "ear":
            if T.face == "front":
                return lerp(c, (196, 150, 140) if winter else (226, 200, 180), 0.5)
            if not winter and T.j == 0:
                return lerp(c, P["under"], 0.6)  # pale rims
        # bright black eyes on the sides of the head at the front
        if mat == "skull" and T.face == "front" and abs(y - 15.5) < 0.6 and abs(abs(x) - 1.5) < 0.6:
            return ER_EYE
        if mat == "skull" and T.side and T.face in ("left", "right") and abs(y - 15.5) < 0.6 and z < -11.5:
            return (230, 230, 230) if T.face == "left" else ER_EYE
        if mat == "jaw" and T.side and T.j == 0 and T.face != "back":
            return lerp(c, ER_NOSE, 0.4)
        return c
    return colour


# ======================================================================= Pine Marten (PredatorModel, built at 2x)
def pine_marten():
    m = Model("Pine Marten", "pineMarten", 128, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 17, 0))
    A("body", "chest", -2.5, 13, -8, 5, 5, 6)
    A("body", "belly", -2.5, 12.5, -2, 5, 5, 5)
    A("body", "haunch", -2.5, 12.5, 3, 5, 5, 5)
    A("body", "back", -2, 12, -6, 4, 1, 12)
    A("body", "neck", -2, 12.5, -10.5, 4, 4, 3)
    m.part("tail", "body", (0, -3.5, 8), (1.25, 0, 0))
    B("tail", "tail", -1.5, 0, -1.5, 3, 5, 3)
    m.part("tail_mid", "tail", (0, 4.5, 0), (-0.15, 0, 0))
    B("tail_mid", "tail", -2, 0, -2, 4, 5, 4)
    m.part("tail_tip", "tail_mid", (0, 4.5, 0), (-0.1, 0, 0))
    B("tail_tip", "tail", -1.5, 0, -1.5, 3, 3, 3)
    m.part("head", "body", (0, -4, -10))
    B("head", "skull", -2.5, -2.5, -4, 5, 4, 4)
    B("head", "cheek", -3, -0.5, -3.5, 6, 2, 3)
    B("head", "snout", -1.5, -1, -6.5, 3, 2, 3)
    B("head", "nose", -0.5, -1.2, -7, 1, 1, 1)
    m.part("jaw", "head", (0, 1, -4))
    B("jaw", "jaw", -1, 0, -2.5, 2, 1, 3)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.2, -2.3, -1.5), (0, 0, s * 0.2))
        B(n, "ear", -1.5, -2, -0.5, 3, 2, 1)
    legs(m, 1.8,
         (18.5, -5.5, [("upper", -1.5, -1.5, -1.5, 3, 3, 3)], (1.5, 0),
          [("shank", -1, 0, -1, 2, 3, 2), ("paw", -1.5, 3, -2, 3, 1, 3)]),
         (17.5, 5, [("thigh", -1.5, -2, -2, 3, 5, 4)], (2.5, 0.5),
          [("shank", -1, 0, -1, 2, 3, 2), ("paw", -1.5, 3, -2.5, 3, 1, 4)]))
    return m


PM = dict(fur=(96, 60, 36), dark=(52, 34, 22), light=(130, 88, 56), bib=(232, 182, 92), bib_dk=(206, 146, 62),
          face=(120, 92, 70), cream=(228, 210, 170), eye=(14, 10, 10), nose=(30, 22, 20))


def marten_colour(T):
    P, mat, (x, y, z) = PM, T.mat, T.p
    if mat == "nose":
        return P["nose"]
    c = strands(P["fur"], P["dark"], P["light"], T, 0.9)
    c = belly_shade(c, T, 12, 24, P["dark"], 0.35)
    if mat in ("upper", "thigh", "shank", "paw") or mat == "tail":
        c = lerp(c, P["dark"], 0.45 if mat != "tail" else 0.3 + 0.15 * smooth((T.lp[1]) / 5))
    # the creamy-orange bib from the chin down the throat to between the forelegs
    if mat in ("jaw", "neck", "chest", "cheek") and (T.face in ("bottom", "front") or (T.side and y > 14.0 + (z + 10) * 0.2)):
        if mat != "chest" or (z < -4.5 and abs(x) < 2):
            if not (mat == "cheek" and y < 13.2):
                c = lerp(P["bib_dk"], P["bib"], 0.5 + 0.5 * T.noise(4))
    if mat in ("skull", "snout") and T.face != "bottom":
        c = lerp(c, P["face"], 0.3)
    if mat == "ear" and round_ear(T):
        return None
    if mat == "ear":
        if T.j == 0 and T.face in ("front", "back", "top"):
            return lerp(P["cream"], c, 0.45)
        return lerp(c, (60, 42, 32), 0.45) if T.face == "front" else c
    if mat == "skull" and T.face == "front" and abs(y - 11.5) < 0.6 and abs(abs(x) - 1.5) < 0.6:
        return P["eye"]
    if mat == "skull" and T.side and T.face in ("left", "right") and abs(y - 11.5) < 0.6 and z < -13.4:
        return (210, 200, 190) if T.face == "left" else P["eye"]
    if mat == "snout" and T.face == "front" and T.j == 1:
        return lerp(c, P["nose"], 0.4)
    return c


# ======================================================================= Lemming (LemmingModel, built at 2x)
def lemming():
    m = Model("Lemming", "lemming", 64, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 20, 0))
    A("body", "body", -3.5, 15, -3.5, 7, 7, 9)
    A("body", "back", -2.5, 14.3, -2.5, 5, 1, 7)
    A("body", "rump", -2.5, 15.8, 5.5, 5, 5, 1)
    A("body", "flank", -4.2, 16.5, -2.5, 8, 4, 7)
    A("body", "chest", -2.5, 16.2, -4.5, 5, 5, 1)
    m.part("tail", "body", (0, -1, 6))
    B("tail", "tail", -1, -1, 0, 2, 2, 1)
    m.part("head", at=(0, 18.5, -3.5))
    B("head", "skull", -2.5, -2.8, -4, 5, 4, 4)
    B("head", "cheek", -3, -0.8, -3.5, 6, 3, 3)
    B("head", "snout", -1.5, -1.2, -5, 3, 2, 1)
    B("head", "nose", -1, -1.2, -5.5, 2, 1, 1)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.0, -2.5, -1))
        B(n, "ear", -1, -1, -0.5, 2, 1, 1)
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_front_leg", at=(s * 2, 22, -2.5))
        B(l, "foot", -1, 0, -1.5, 2, 2, 2)
        l = m.part(f"{side}_hind_leg", at=(s * 2.5, 22, 3))
        B(l, "foot", -1.5, 0, -1.5, 3, 2, 3)
    return m


LM = dict(black=(28, 24, 22), black_lt=(58, 50, 44), tawny=(176, 124, 66), tawny_dk=(122, 82, 44), buff=(214, 178, 124),
          belly=(226, 204, 160), eye=(10, 8, 8), nose=(94, 66, 60), foot=(150, 128, 104), stripe=(222, 196, 144))


def lemming_colour(T):
    P, mat, (x, y, z) = LM, T.mat, T.p
    if mat == "nose":
        return P["nose"]
    if mat == "foot":
        return P["foot"] if T.face != "top" else P["buff"]
    # black head and shoulders, a tawny back with a dark saddle, buff cheeks and flanks, a pale belly
    tawny = strands(P["tawny"], P["tawny_dk"], P["buff"], T, 1.0)
    black = strands(P["black"], (14, 12, 12), P["black_lt"], T, 1.0)
    blackness = smooth((-1.5 - z) / 2.5) if mat not in ("skull", "snout", "ear") else 1.0
    c = lerp(tawny, black, blackness)
    if mat in ("body", "back") and abs(z - 1.5) < 2.5 and y < 16 and h3(x, y, z, 5) > 0.35:
        c = lerp(c, P["tawny_dk"], 0.5)  # the dark saddle
    low = smooth((y - 17.5) / 2.5)
    if mat in ("body", "flank", "rump", "chest", "cheek"):
        c = lerp(c, P["buff"], 0.75 * low)
    if mat == "cheek" and T.face != "top":
        c = lerp(c, P["buff"], 0.7)
    if T.face == "bottom":
        c = P["belly"]
    if mat == "snout" or (mat == "cheek" and T.face == "front"):
        c = lerp(lerp(c, P["tawny"], 0.6), P["tawny_dk"], 0.25)
        if T.face == "front" and T.j == 2:
            c = lerp(c, P["buff"], 0.5)
    if mat == "ear":
        return lerp(P["tawny"], P["tawny_dk"], 0.4)
    # small black beady eyes with a pale line back to the ear
    if mat == "skull" and T.face in ("left", "right") and abs(y - 17.2) < 0.6:
        front = (T.fw - 1 - T.i) if T.face == "right" else T.i
        if front == 1:
            return P["eye"]
        if front == 0:
            return (190, 180, 170) if T.face == "left" else P["eye"]
        if front >= 2:
            return lerp(c, P["stripe"], 0.5)
    if mat == "skull" and T.face == "front" and abs(y - 17.2) < 0.6 and abs(x) > 1.9:
        return P["eye"]
    return c


# ======================================================================= Mushroom Man (MushroomManModel)
def mushroom_man():
    m = Model("Mushroom Man", "mushroomMan", 128, 64)
    A, B = m.abox, m.box
    m.part("cap", at=(0, 9, 0))
    B("cap", "brim", -7, -2, -7, 14, 2, 14)        # the margin, its underside the gills
    B("cap", "dome", -6, -4, -6, 12, 2, 12)
    B("cap", "crown", -4, -6, -4, 8, 2, 8)
    B("cap", "umbo", -2, -7, -2, 4, 1, 4)
    B("cap", "veil", -6.5, 0, -6.5, 13, 1, 13)     # torn veil hanging from the rim
    m.part("stem", at=(0, 9, 0))
    B("stem", "stem", -2.5, 0, -2, 5, 5, 4)
    B("stem", "ring", -3.5, 4, -3, 7, 1, 6)         # the skirt (annulus)
    B("stem", "stem", -3, 5, -2.5, 6, 5, 5)
    B("stem", "volva", -3.5, 8, -3, 7, 2, 6)        # the bulb at the foot
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_leg", at=(s * 1.6, 19, 0))
        B(l, "leg", -1.5, 0, -1.5, 3, 4, 3)
        B(l, "root", -2, 4, -2.5, 4, 1, 4)
        a = m.part(f"{side}_arm", at=(s * 4, 10, 0), rot=(0, 0, -s * 0.15))
        B(a, "arm", -1, 0, -1, 2, 7, 2)
        B(a, "hand", -1.5, 7, -1.5, 3, 2, 3)
    return m


MM = dict(cap=(132, 70, 40), cap_dk=(86, 44, 26), cap_lt=(176, 108, 62), wart=(236, 226, 200), gill=(222, 208, 176),
          gill_dk=(170, 150, 118), stem=(226, 218, 196), stem_dk=(176, 164, 140), dirt=(96, 76, 54), hollow=(30, 22, 18),
          veil=(238, 232, 216))


def mushroom_colour(T):
    P, mat, (x, y, z) = MM, T.mat, T.p
    r = math.hypot(x, z)
    if mat in ("brim", "dome", "crown", "umbo"):
        if T.face == "bottom":
            # gills: fine radial lines round the stem
            ang = math.atan2(z, x)
            line = (ang * 18 / math.pi) % 1.0 < 0.45
            return lerp(P["gill"], P["gill_dk"], 0.6 if line else 0.1 + 0.2 * smooth((2.5 - r) / 2))
        t = smooth((7.5 - r) / 6)  # darker towards the middle
        c = lerp(P["cap_lt"], P["cap_dk"], 0.25 + 0.6 * t)
        c = lerp(c, P["cap"], 0.3 * h(T.i, T.j, T.k))
        if T.side and mat == "brim":
            c = lerp(c, P["cap_lt"], 0.25) if (T.i % 2) else shade(c, 0.9)  # striate margin
        # the warts: pale flakes scattered over the cap, thicker on top
        if (T.face == "top" or mat != "brim") and h3(math.floor(x * 0.8), math.floor(y), math.floor(z * 0.8), 9) > 0.8 \
                and h(T.i, T.j, T.k + 2) > 0.3:
            c = lerp(P["wart"], P["gill"], 0.3 * h(T.i, T.j, 4))
        return c
    if mat == "veil":
        if T.face in ("top", "bottom") and r < 5.5:
            return None
        if T.face in ("top", "bottom"):
            return None if (r < 6.2) else lerp(P["veil"], P["gill"], 0.3)
        if ragged(T, 0.6, 3) or h(T.i, T.k, 4) < 0.35:
            return None
        return lerp(P["veil"], P["gill_dk"], 0.2 * h(T.i, T.j, 5))
    if mat == "ring":
        if T.face == "top" and r < 2.5:
            return P["stem_dk"]
        if T.side and T.j == 0 and h(T.i, 0, T.k) < 0.4:
            return lerp(P["veil"], P["stem_dk"], 0.4)
        return lerp(P["veil"], P["gill"], 0.4 * h(T.i, T.j, 6))
    if mat in ("stem", "volva", "leg", "root", "arm", "hand"):
        c = lerp(P["stem"], P["stem_dk"], 0.15 + 0.15 * h(T.i, T.j, T.k))
        # snakeskin fibrils: broken zigzag bands
        if T.side and ((T.j + (T.i // 2) % 2) % 3 == 0) and h(T.i // 2, T.j, T.k + 1) > 0.35:
            c = lerp(c, P["stem_dk"], 0.5)
        c = lerp(c, P["dirt"], 0.75 * smooth((y - 19) / 5))  # earth on the foot
        if mat in ("volva", "root") and h(T.i, T.j, T.k + 3) > 0.6:
            c = lerp(c, P["dirt"], 0.6)
        if mat == "hand" and T.face == "bottom":
            c = lerp(c, P["dirt"], 0.5)
        # the face: two dark hollows and a gaping mouth on the upper stem
        if mat == "stem" and T.face == "front":
            if abs(abs(x) - 1.0) < 0.6 and abs(y - 10.5) < 1.0 and y < 11.6:
                return P["hollow"] if y > 10.0 else lerp(P["hollow"], c, 0.5)
            if abs(x) < 1.1 and abs(y - 12.5) < 0.6:
                return P["hollow"]
            if abs(x) < 1.6 and abs(y - 11.5) < 0.6:
                return lerp(P["hollow"], c, 0.6)
        return c
    return (255, 0, 255)


# ======================================================================= Giant Centipede (GiantCentipedeModel)
SEGMENTS = 8


def centipede():
    m = Model("Giant Centipede", "giantCentipede", 64, 64)
    B = m.box
    parent = "root"
    for i in range(SEGMENTS):
        seg = m.part(f"seg{i}", parent, (0, 20, -8) if i == 0 else (0, 0, 2))
        B(seg, "plate", -2.5, -1.6, 0, 5, 1, 2)       # the glossy back plate (tergite), wider than the body
        B(seg, "body", -2, -0.6, 0, 4, 2, 2)
        for s, side in ((-1, "right"), (1, "left")):
            leg = m.part(f"{side}_leg{i}", seg, (s * 2, 0.5, 1), (0, 0, s * 0.85))
            B(leg, "femur", -2 if s < 0 else 0, -0.5, -0.5, 2, 1, 1)
            spread = 0.55 * (3.5 - i) / 3.5
            foot = m.part(f"{side}_foot{i}", leg, (s * 2, 0, 0), (0, s * spread, s * 0.55))
            B(foot, "tarsus", -2 if s < 0 else 0, -0.5, -0.5, 2, 1, 1)
        parent = seg
    m.part("tail", parent, (0, 0, 2))
    B("tail", "plate", -1.5, -1.3, 0, 3, 1, 2)
    B("tail", "body", -1.5, -0.3, 0, 3, 1, 2)
    for s, side in ((-1, "right"), (1, "left")):
        m.part(f"{side}_cercus", "tail", (s * 1, 0, 1.5), (0.1, s * 0.3, 0))
        B(f"{side}_cercus", "cercus", -0.5, -0.5, 0, 1, 1, 4)
    m.part("head", "seg0", (0, 0, 0))
    B("head", "head", -2.5, -1.4, -3, 5, 2, 3)
    B("head", "clypeus", -1.5, -1.2, -4, 3, 1, 1)
    for s, side in ((-1, "right"), (1, "left")):
        m.part(f"{side}_antenna", "head", (s * 1.2, -0.8, -3.5), (0.3, -s * 0.45, 0))
        B(f"{side}_antenna", "antenna", -0.5, -0.5, -6, 1, 1, 6)
        m.part(f"{side}_claw", "head", (s * 1.3, 0.4, -2.5), (0, -s * 0.5, 0))
        B(f"{side}_claw", "claw", -0.5, -0.5, -2.5, 1, 1, 3)
    return m


CP = dict(plate=(70, 26, 18), plate_lt=(150, 62, 34), rim=(24, 12, 10), side=(110, 44, 26), belly=(196, 150, 90),
          head=(156, 52, 26), head_dk=(96, 30, 18), leg=(206, 140, 48), leg_dk=(128, 66, 28), tip=(40, 22, 14),
          fang=(30, 16, 12))


def centipede_colour(T):
    P, mat, (x, y, z) = CP, T.mat, T.p
    if mat in ("plate", "body"):
        if T.face == "bottom":
            return lerp(P["belly"], P["side"], 0.25 * T.noise())
        if T.face in ("front", "back"):
            return P["rim"]
        if T.face in ("left", "right"):
            return lerp(P["side"], P["rim"], 0.4) if mat == "plate" else lerp(P["side"], P["belly"], 0.3 * T.j)
        c = lerp(P["plate"], P["plate_lt"], 0.45 * (1 - abs(x) / 2.5))
        if abs(x) < 0.6:
            c = lerp(c, P["plate_lt"], 0.35)  # the shine down the middle
        if T.j == 0:
            c = P["rim"]  # the hind edge of each plate, dark
        return c
    if mat in ("femur", "tarsus"):
        if mat == "tarsus" and abs(x) > 5.0:
            return P["tip"]
        c = lerp(P["leg_dk"], P["leg"], 0.25 + 0.3 * T.noise() if mat == "femur" else 0.7 + 0.2 * T.noise())
        return shade(c, 0.8) if T.face == "bottom" else c
    if mat in ("head", "clypeus"):
        c = lerp(P["head"], P["head_dk"], 0.2 * T.noise() + (0.3 if T.face != "top" else 0))
        if T.face == "top" and abs(x) < 0.6:
            c = lerp(c, P["plate_lt"], 0.4)
        if mat == "head" and T.face == "top" and abs(abs(x) - 1.5) < 0.6 and T.j == T.fh - 1:
            return (14, 10, 10)  # the clustered simple eyes
        return c
    if mat == "antenna":
        return lerp(P["leg"], P["leg_dk"], 0.7) if int(abs(T.lp[2])) % 2 else lerp(P["head"], P["leg"], 0.5)
    if mat == "claw":
        return P["fang"] if T.lp[2] < -1.5 else lerp(P["head"], P["head_dk"], 0.4)
    if mat == "cercus":
        return lerp(P["leg"], P["leg_dk"], 0.5) if int(T.lp[2]) % 2 else P["leg_dk"]
    return (255, 0, 255)


# ======================================================================= Vittra and Troll (FolkModel)
def vittra():
    m = Model("Vittra", "vittra", 64, 64)
    B = m.box
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_leg", at=(s * 1.4, 12, 0))
        B(l, "thigh", -1, 0, -1, 2, 7, 2)
        B(l, "shin", -1, 7, -1, 2, 4, 2)
        B(l, "foot", -1, 11, -2.5, 2, 1, 3)
    m.part("body", at=(0, 12, 0))
    B("body", "tunic", -3, -13, -1.5, 6, 6, 3)
    B("body", "tunic", -2.5, -7, -1.5, 5, 4, 3)
    B("body", "hem", -3, -3, -2, 6, 5, 4)            # the ragged hem over the thighs
    B("body", "hood", -3.5, -13.5, -1, 7, 2, 3)      # the hood fallen back on the shoulders
    B("body", "skin", -1, -14.5, -1, 2, 2, 2)        # neck
    m.part("head", "body", (0, -14, 0))
    B("head", "face", -3.5, -7, -3.5, 7, 6, 7)
    B("head", "chin", -2.5, -1, -3, 5, 1, 4)
    B("head", "nose", -0.5, -3.5, -4.5, 1, 2, 1)
    B("head", "hair", -4, -7.5, -4, 8, 2, 8)
    B("head", "hair", -4, -5.5, 1, 8, 9, 3)           # long hair down the back
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 3.5, -3.5, 0), (-0.3, 0, s * 0.7))
        B(n, "ear", -1 if s < 0 else 0, -5, -1, 1, 5, 2)
    for s, side in ((-1, "right"), (1, "left")):
        a = m.part(f"{side}_arm", "body", (s * 3.8, -12, 0))
        x0 = -1.5 if s < 0 else -0.5
        B(a, "sleeve", x0, -1, -1, 2, 6, 2)
        B(a, "skin", x0, 5, -1, 2, 6, 2)
        B(a, "hand", x0, 11, -1, 2, 3, 2)
    return m


VT = dict(skin=(150, 160, 160), skin_dk=(104, 114, 118), skin_lt=(184, 192, 190), eye=(12, 14, 16), eye_lt=(170, 210, 200),
          hair=(30, 34, 30), hair_lt=(70, 78, 66), tunic=(86, 90, 78), tunic_dk=(54, 58, 50), tunic_lt=(118, 120, 104),
          band=(140, 112, 64), cord=(120, 96, 64), lip=(90, 84, 92), nail=(60, 58, 54))


def vittra_colour(T):
    P, mat, (x, y, z) = VT, T.mat, T.p
    n = T.noise()
    if mat in ("skin", "face", "chin", "nose", "ear", "thigh", "shin", "foot", "hand"):
        c = lerp(P["skin"], P["skin_dk"], 0.15 + 0.25 * n)
        if mat == "ear":
            if T.side and T.face in ("front", "back") and T.j == 0 and T.i == 1:
                return None  # the pointed tip
            return lerp(c, (150, 120, 130), 0.4) if T.face == "front" else c
        if mat == "foot" and T.face == "front":
            return lerp(c, P["nail"], 0.5) if T.i % 2 == 0 else c
        if mat == "hand" and T.j == T.fh - 1 and T.side:
            return P["nail"]  # long dark nails
        if mat == "face" and T.face == "front":
            # head pivot y -2 (model space): face box rows y -9..-3
            if -6.0 < y < -4.0 and 0.5 < abs(x) < 2.6:
                glint = x < 0 and y < -5.0 and abs(x + 2.0) < 0.6
                return P["eye_lt"] if glint else P["eye"]  # the huge dark eyes, 2x2 texels, a pale glint
            if -7.0 < y < -6.0 and 0.5 < abs(x) < 2.6:
                return lerp(c, P["skin_dk"], 0.6)  # the brow
            if -4.0 < y < -3.0 and abs(x) < 1.6:
                return P["lip"] if abs(x) < 1.0 else c
        return c
    if mat == "hair":
        col = h(T.i + T.k * 3, 0, 9)
        c = lerp(P["hair"], P["hair_lt"], 0.15 + 0.35 * col)
        if T.face in ("back", "left", "right") and ragged(T, 0.55, 2):
            return None
        return c
    if mat in ("tunic", "hem", "sleeve", "hood"):
        c = lerp(P["tunic"], P["tunic_dk"], 0.3 * n + (0.12 if (T.i + T.j) % 2 else 0))
        if mat == "hem" and T.side and ragged(T, 0.5, 4):
            return None
        if mat == "hem" and T.side and T.j == T.fh - 2 and T.i % 3:
            c = lerp(c, P["band"], 0.5)  # the woven border
        if mat == "tunic" and T.side and abs(y - 8.5) < 0.6:
            return lerp(P["cord"], P["tunic_dk"], 0.3 * (T.i % 2))  # the cord belt
        if mat == "hood":
            c = lerp(c, P["tunic_lt"], 0.25)
        if mat == "sleeve" and T.side and T.j == T.fh - 1:
            c = lerp(c, P["band"], 0.4)
        return c
    return (255, 0, 255)


def troll():
    m = Model("Troll", "troll", 128, 64)
    B = m.box
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_leg", at=(s * 2.8, 12, 0))
        B(l, "hide", -2.5, 0, -2.5, 5, 6, 5)
        B(l, "skin", -2, 6, -2, 4, 5, 4)
        B(l, "foot", -2.5, 11, -3.5, 5, 1, 6)
    m.part("body", at=(0, 12, 0))
    B("body", "belly", -5, -7, -3.5, 10, 7, 7)
    B("body", "chest", -5.5, -14, -3, 11, 7, 6)
    B("body", "moss", -4, -15.5, -0.5, 8, 4, 4)      # the moss-grown hump
    B("body", "kilt", -5.5, -1, -4, 11, 4, 8)
    m.part("head", "body", (0, -13, -2))
    B("head", "skin", -3.5, -6, -4, 7, 6, 6)
    B("head", "brow", -4, -6.5, -4.6, 8, 2, 2)
    B("head", "jaw", -4, -1, -4.5, 8, 3, 6)
    B("head", "hair", -4, -7.5, -2, 8, 3, 6)
    B("head", "hair", -4.5, -5, 1, 9, 6, 3)
    for s, side in ((-1, "right"), (1, "left")):
        e = m.part(f"{side}_eartip", "head", (s * 3.5, -3.5, -1), (0, 0, s * 0.5))
        B(e, "ear", -1 if s < 0 else 0, -1, -1, 1, 4, 2)
    m.part("nose", "head", (0, -3.5, -4.5), (0.25, 0, 0))
    B("nose", "nose", -1.5, -1, -4, 3, 5, 4)
    for s, n in ((-1, "right_tusk"), (1, "left_tusk")):
        m.part(n, "head", (s * 2.8, 0.5, -5))
        B(n, "tusk", -0.5, -2, -0.5, 1, 2, 1)
    for s, side in ((-1, "right"), (1, "left")):
        a = m.part(f"{side}_arm", "body", (s * 6.5, -12.5, 0))
        x0 = -2.5 if s < 0 else -1.5
        B(a, "moss", x0 - 0.5, -2.5, -2.5, 5, 2, 5)    # moss on the shoulder
        B(a, "skin", x0, -1.5, -2, 4, 8, 4)
        B(a, "skin", x0 - 0.5, 6.5, -2.5, 5, 7, 5)
        B(a, "hand", x0 - 1, 13.5, -3, 6, 4, 6)
    m.part("tail", "body", (0, -1, 3.5), (0.5, 0, 0))
    B("tail", "skin", -0.5, 0, -0.5, 1, 9, 1)
    B("tail", "tuft", -1.5, 9, -1.5, 3, 4, 3)
    return m


TR = dict(skin=(116, 118, 106), skin_dk=(60, 63, 56), skin_lt=(156, 156, 142), lichen=(178, 176, 140),
          moss=(70, 104, 38), moss_lt=(124, 156, 62), moss_dk=(38, 60, 24), hair=(52, 50, 38), hair_lt=(86, 84, 62),
          hide=(116, 86, 56), hide_dk=(76, 54, 36), eye=(226, 186, 52), pupil=(20, 16, 10), tusk=(226, 214, 180),
          nail=(36, 34, 30), mouth=(40, 26, 22))


def troll_moss(T, x, y, z):
    """Big patches of moss: over everything facing up, and creeping a little way down the sides from the top edges."""
    if T.mat not in ("skin", "chest", "belly", "brow", "hand"):
        return 0.0
    blob = h3(math.floor(x / 3), math.floor(y / 3), math.floor(z / 3), 41)
    from_top = T.lp[1] - T.b["y"]
    if T.face == "top" and y < 4:
        return 1.0 if blob > 0.2 else 0.0
    if T.side and y < 2 and from_top < 1.0 + 1.5 * blob and blob > 0.35:
        return 1.0
    return 0.0


def troll_colour(T):
    P, mat, (x, y, z) = TR, T.mat, T.p
    if mat in ("skin", "belly", "chest", "jaw", "nose", "ear", "foot", "hand", "brow"):
        if troll_moss(T, x, y, z):
            c = lerp(P["moss_dk"], P["moss_lt"], 0.25 + 0.6 * h3(math.floor(x * 1.5), math.floor(y * 1.5), math.floor(z * 1.5), 42))
            return c if h(T.i, T.j, T.k + 7) < 0.96 else (222, 204, 96)  # a tiny flower now and then
        # weathered stone: each box lit from above and shadowed below, a darker underside, broad slow variation
        r = T.rel()
        c = lerp(P["skin_lt"], P["skin"], 0.35 + 0.65 * r[1]) if T.side else (P["skin_lt"] if T.face == "top" else P["skin_dk"])
        c = belly_shade(c, T, -6, 24, P["skin_dk"], 0.5)
        c = lerp(c, P["skin_dk"], 0.14 * h3(math.floor(x / 4), math.floor(y / 4), math.floor(z / 4), 30) + 0.04 * T.noise())
        if T.side and (T.i == 0 or T.i == T.fw - 1):
            c = shade(c, 0.9)  # the edges turning away
        if h3(math.floor(x / 2), math.floor(y / 2), math.floor(z / 2), 32) > 0.95:
            c = lerp(c, P["lichen"], 0.45)  # a rare crust of lichen, as on old stone
        if mat == "chest" and T.face == "front" and T.j in (1, 2, 3) and 0.5 < abs(x) < 4 and h(T.i, T.j, 4) > 0.5:
            c = lerp(c, P["hair"], 0.55)  # chest hair
        if mat == "hand" and T.side and T.j == T.fh - 1:
            return P["nail"] if T.i % 2 == 0 else c
        if mat == "foot" and T.face == "front" and T.i % 2 == 0:
            return P["nail"]
        if mat == "brow":
            c = lerp(c, P["hair"], 0.3)
        if mat == "nose":
            c = lerp(c, P["skin_lt"], 0.15)
            if T.face == "bottom" and T.i in (0, 2):
                return P["mouth"]  # nostrils
        if mat == "jaw" and T.face == "front" and T.j == 0:
            return P["mouth"] if T.i not in (0, T.fw - 1) else c
        if mat == "skin" and T.face == "front" and z < -5.5:
            # small yellow eyes deep under the brow (head pivot y -1, z -2: the face front at z -6)
            if -5.0 < y < -4.0 and 1.5 < abs(x) < 3.5:
                return P["eye"] if abs(x) < 2.5 else P["pupil"]
            if -4.0 < y < -3.0 and 1.5 < abs(x) < 3.5:
                return shade(c, 0.75)  # the bags under them
        return c
    if mat == "moss":
        c = lerp(P["moss_dk"], P["moss_lt"], 0.2 + 0.7 * h3(math.floor(x * 1.5), math.floor(y * 1.5), math.floor(z * 1.5), 33))
        if T.side and ragged(T, 0.5, 6):
            return None
        if h(T.i, T.j, T.k + 7) > 0.95:
            c = (222, 204, 96)
        return c
    if mat in ("tuft", "hair"):
        col = h(T.i + T.k * 5, 0, 12)
        c = lerp(P["hair"], P["hair_lt"], 0.2 + 0.5 * col)
        if T.side and ragged(T, 0.55, 7):
            return None
        return c
    if mat in ("hide", "kilt"):
        c = lerp(P["hide"], P["hide_dk"], 0.3 * T.noise() + 0.2 * smooth((y - 12) / 4))
        if mat == "kilt" and T.side and ragged(T, 0.55, 8):
            return None
        if mat == "kilt" and T.side and T.j == 0:
            return lerp(P["hide_dk"], P["nail"], 0.4)  # the thong round the waist
        if mat == "kilt" and T.side and h(T.i // 2, T.j, 9) > 0.8:
            c = lerp(c, (150, 130, 100), 0.4)  # pale fur tufts left on the hide
        return c
    if mat == "tusk":
        return P["tusk"] if T.lp[1] < -0.5 else lerp(P["tusk"], P["hide_dk"], 0.3)
    return (255, 0, 255)


# ======================================================================= Lindworm (LindwormModel)
BODY, TAIL = 7, 4


def lindworm():
    m = Model("Lindworm", "lindworm", 128, 64)
    B = m.box
    parent = "root"
    for i in range(BODY + TAIL + 1):
        if i < BODY:
            seg = m.part(f"seg{i}", parent, (0, 21.5, -12) if i == 0 else (0, 0, 6))
            B(seg, "scale", -3, -2.5, 0, 6, 5, 6)
            B(seg, "belly", -2.5, 2.4, 0.5, 5, 1, 5)
            B(seg, "fin", -0.5, -4.5, 1, 1, 2, 4)
            B(seg, "ridge", -1.5, -3, 0.5, 3, 1, 5)
        elif i < BODY + TAIL:
            seg = m.part(f"seg{i}", parent, (0, 0.5 if i == BODY else 0, 6))
            B(seg, "scale", -2, -2, 0, 4, 4, 6)
            if i < BODY + 2:
                B(seg, "fin", -0.5, -3.5, 1, 1, 2, 4)
            else:
                B(seg, "ridge", -1, -2.5, 0.5, 2, 1, 5)
        else:
            seg = m.part(f"seg{i}", parent, (0, 1, 6))
            B(seg, "scale", -1, -1, 0, 2, 2, 6)
            B(seg, "spade", -2, -0.5, 4, 4, 1, 3)     # the tail ends in a flat spade
        parent = seg
    m.part("head", "seg0", (0, -0.5, 0))
    B("head", "skull", -3, -2.5, -5, 6, 4, 5)
    B("head", "snout", -2.5, -2, -9, 5, 3, 4)
    B("head", "brow", -3.5, -3, -5.5, 2, 1, 3)
    B("head", "brow", 1.5, -3, -5.5, 2, 1, 3)
    B("head", "teeth", -2.5, 1, -9, 5, 1, 4)
    m.part("jaw", "head", (0, 1.5, -1))
    B("jaw", "jawbone", -2.5, 0, -7, 5, 2, 7)
    B("jaw", "teeth", -2, -1, -7.5, 4, 1, 4)
    for s, n in ((-1, "right_horn"), (1, "left_horn")):
        m.part(n, "head", (s * 2, -2.5, -2), (0.45, s * -0.25, 0))
        B(n, "horn", -0.5, -0.5, 0, 1, 1, 5)
        B(n, "horn", -1, -1, -0.5, 2, 2, 2)
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_leg", "seg1", (s * 3, -1, 2), (0, 0, -s * 0.6))
        x0 = -2 if s < 0 else 0
        B(l, "scale", x0, 0, -1, 2, 4, 2)
        B(l, "claw", x0, 4, -2, 2, 1, 3)
    return m


LW = dict(scale=(46, 92, 52), scale_dk=(24, 54, 32), scale_lt=(88, 140, 78), belly=(196, 176, 104), belly_dk=(150, 128, 70),
          fin=(120, 36, 30), fin_dk=(70, 20, 18), horn=(222, 210, 176), horn_dk=(150, 136, 110), eye=(236, 196, 40),
          pupil=(16, 12, 8), mouth=(150, 40, 40), tooth=(240, 236, 220), claw=(40, 36, 30))


def lindworm_colour(T):
    P, mat, (x, y, z) = LW, T.mat, T.p
    if mat in ("scale", "skull", "snout", "brow", "jawbone"):
        if T.face == "bottom":
            return lerp(P["belly"], P["belly_dk"], 0.4 * ((T.i + T.j) % 2))
        # overlapping scales: rows staggered along the body, each lit at its front and dark at its rim
        row = math.floor(z / 2.0)
        u = z % 2.0
        v = ((y if T.face in ("left", "right") else x) + (row % 2)) % 2.0
        c = lerp(P["scale"], P["scale_lt"], 0.75 * (1 - u / 2) * (0.5 + 0.5 * (v < 1)))
        if u > 1.5 or (v > 1.5 and T.face in ("left", "right", "top")):
            c = lerp(c, P["scale_dk"], 0.85)
        c = belly_shade(c, T, 17, 24, P["belly_dk"], 0.4 if mat != "jawbone" else 0.6)
        if mat == "jawbone" and T.side and T.face != "back":
            c = lerp(c, P["belly"], 0.35)
        if mat == "snout" and T.face == "front" and T.j == 0 and T.i in (1, 3):
            return P["pupil"]  # nostrils
        if mat == "snout" and T.side and T.j == T.fh - 1:
            return P["mouth"]
        if mat == "skull" and T.face in ("left", "right") and T.j == 1:
            front = (T.fw - 1 - T.i) if T.face == "right" else T.i
            if front == 0:
                return P["eye"]
            if front == 1:
                return P["pupil"]
        if mat == "brow":
            c = lerp(c, P["scale_dk"], 0.3)
        return c
    if mat == "belly":
        if T.face == "bottom":
            return lerp(P["belly"], P["belly_dk"], 0.5 if T.j % 2 == 0 else 0.1)  # broad belly plates
        return lerp(P["belly"], P["belly_dk"], 0.3)
    if mat in ("fin", "ridge"):
        c = lerp(P["fin"], P["fin_dk"], 0.4 * T.noise() + (0.3 if T.face in ("left", "right") and T.j > 0 else 0))
        return lerp(c, P["scale_dk"], 0.5) if mat == "ridge" else c
    if mat == "spade":
        return lerp(P["fin"], P["fin_dk"], 0.3 + 0.3 * T.noise())
    if mat == "teeth":
        if T.face in ("left", "right", "front"):
            return P["tooth"] if T.i % 2 == 0 else None
        return P["mouth"]
    if mat == "horn":
        return lerp(P["horn"], P["horn_dk"], 0.5 if int(T.lp[2] + 1) % 2 else 0.1)
    if mat == "claw":
        return P["claw"] if T.face in ("front", "bottom") or T.i % 2 == 0 else lerp(P["scale"], P["claw"], 0.5)
    return (255, 0, 255)


# ======================================================================= output
def write(path, package, models, renames=None):
    body = "\n".join(m.java() for m in models)
    for old, new in (renames or {}).items():
        body = body.replace(f'"{old}"', f'"{new}"')
    path.write_text(f"""package {package};

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_hunt_models.py (with the skins it paints for them). Do not edit. */
public final class {path.stem} {{
{body}
    private {path.stem}() {{}}
}}
""", encoding="utf-8")


def main():
    wv, lx, er, pm, lm, mm = wolverine(), lynx(), ermine(), pine_marten(), lemming(), mushroom_man()
    for m in (wv, lx, er, pm, lm, mm):
        m.pack()
    keep = ("claws", "nose", "tuft")
    for m, colour, name, skin in (
            (wv, wolverine_colour, "wolverine", True),
            (lx, lynx_colour(LX, False), "lynx", True),
            (lx, lynx_colour(LX_KIT, True), "lynx_kitten", False),
            (er, ermine_colour(ER_S, False), "ermine", True),
            (er, ermine_colour(ER_W, True), "ermine_winter", True),
            (pm, marten_colour, "pine_marten", True),
            (lm, lemming_colour, "lemming", True)):
        im = m.paint(colour)
        save(im, f"wildlife/{name}.png")
        if skin:
            save(skinned(m, im, keep), f"skinned/wildlife/{name}.png")
    save(mm.paint(mushroom_colour), "night/mushroom_man.png")
    write(JAVA_DIR / "coldhunters/HuntGeometry.java", "com.mechanical.client.wildlife.coldhunters", (wv, lx, er, pm, lm), LOWER)
    write(JAVA_DIR / "night/MushroomGeometry.java", "com.mechanical.client.wildlife.night", (mm,))
    cp, vt, tr, lw = centipede(), vittra(), troll(), lindworm()
    for m in (cp, vt, tr, lw):
        m.pack()
    for m, colour, name in ((cp, centipede_colour, "giant_centipede"), (vt, vittra_colour, "vittra"), (tr, troll_colour, "troll"),
                            (lw, lindworm_colour, "lindworm")):
        im = m.paint(colour)
        save(im, f"wildlife/{name}.png")
        save(skinned(m, im, ("claw", "fang", "tusk", "horn", "teeth", "tarsus")), f"skinned/wildlife/{name}.png")
    # the centipede's legs: one pair per segment, every pair named alike under its own segment
    legs = {f"{side}_{n}{i}": f"{side}_{n}" for side in ("right", "left") for n in ("leg", "foot") for i in range(SEGMENTS)}
    write(JAVA_DIR / "underground/UndergroundGeometry.java", "com.mechanical.client.wildlife.underground", (cp, vt, tr, lw), legs)
    for m in (wv, lx, er, pm, lm, mm, cp, vt, tr, lw):
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes")


if __name__ == "__main__":
    main()
