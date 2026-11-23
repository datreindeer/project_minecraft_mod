"""The warm-country birds' models and skins, each from one table (wildlife/warm, docs/fauna/warm.md):

- src/main/java/com/mechanical/client/wildlife/warm/WarmBirdGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<bird>.png and the plucked carcass skins in textures/entity/skinned/wildlife/

Great Hornbill (BirdModel's part tree: black and white body, shaggy cream neck, long down-curved yellow bill under the
big upturned casque with its black edges, red eye, white wing bars and trailing edges, white tail with a black band),
Red Jungle Fowl rooster / hen (JungleFowlModel: comb and wattles, golden hackle cape over the shoulders, red-brown
saddle, green-black breast, folded tail fan with the arched sickles, grey spurred legs; the hen mottled brown with a
small comb) and Indian Peafowl peacock / peahen (PeafowlModel: blue neck and breast, crest of tufted wires, white face
stripes, scaled green back, barred wings with chestnut primaries, and the train of seven long eyed panels that rises
into the fan; the peahen brown with a green neck and white belly).

Same idea as gen_savanna_models.py (whose Model class this uses): every box is packed into the skin and painted from the
3D point each texel covers. Part names are the ones the animation code looks up. The jungle fowl chick keeps
PheasantChickModel and the texture gen_fauna_warm_tex_birds.py paints.
Usage: python scripts/gen_owarm_birds_models.py (after gen_fauna_warm_textures.py, which still paints these files
first). Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import CLEAR, Model, rel  # noqa: E402
from wildlife_paint import h, lerp, save, shade  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/warm/WarmBirdGeometry.java"
HALF_PI = math.pi / 2
SIDES = ("left", "right")


def eye_at(face, p, y0, z0, iris, pupil=(14, 10, 10), glint=(250, 246, 236)):
    """A 2x2 eye on the side faces with its top-left corner at (y0, z0): glint over the pupil, iris behind and below."""
    if face not in SIDES or not (y0 <= p[1] < y0 + 2 and z0 <= p[2] < z0 + 2):
        return None
    top, front = p[1] < y0 + 1, p[2] < z0 + 1
    return glint if top and front else pupil if front or top else iris


def feather_rows(P, face, salt, cell=2.0):
    """0..1: the paler fringe of overlapping feather rows (scallops, offset every other row), in model space."""
    a = P[2] if face in SIDES else P[0]
    b = P[1] if face != "top" and face != "bottom" else P[2]
    row = math.floor(b / cell + 40)
    col = math.floor(a / cell + 40 + 0.5 * (row % 2))
    fy = (b / cell + 40) % 1
    fx = (a / cell + 40 + 0.5 * (row % 2)) % 1
    edge = fy > 0.6 and abs(fx - 0.5) < 0.45
    return (0.7 if edge else 0.0) + 0.3 * h(col, row, salt)


def ragged(face, p, b, salt, chance=0.5):
    """Ragged lower edge of a feather cape: some texels of the bottom row (and the bottom face) clear."""
    if face == "bottom":
        return h(int(p[0] * 2 + 9), int(p[2] * 2 + 9), salt) < chance
    return face != "top" and p[1] > b["y"] + b["h"] - 1 and h(int(p[0] * 3 + p[2] * 2 + 20), 1, salt) < chance


def plucked(colour, keep, quills=(), gone=(), eye=lambda mat, face, p, b: False, light=(238, 204, 172), dark=(198, 148, 118)):
    """The plucked carcass: bare goose-bumped skin, keep (bill, comb, legs) and the eyes as they are, quills (tail and
    flight feathers) as rows of stubs, gone (feather-only parts: train, crest, sickles) clear."""
    def paint(v, mat, face, p, P, b):
        if mat in keep or eye(mat, face, p, b):
            return colour(v, mat, face, p, P, b)
        if mat in gone:
            return CLEAR
        if mat in quills:
            stub = int(p[0] * 2 + p[2] * 2 + 40) % 2 == 0 and int(p[1] * 2 + 40) % 2 == 0
            return (214, 186, 150) if stub else (176, 132, 104)
        c = lerp(light, dark, 0.3 * h(int(P[0] * 2 + 40), int(P[1] * 2 + P[2] * 3 + 90), 40)
                 + (0.35 if face == "bottom" else 0.1 if face == "back" else 0.0))
        return shade(c, 0.86) if h(int(P[0] * 4 + 80), int(P[1] * 4 + P[2] * 4 + 160), 41) < 0.16 else c
    return paint


# ============================================================================ great hornbill
def hornbill_model():
    """BirdModel's part tree; the body underside one pixel below the leg pivot (it roosts on y 23)."""
    m = Model("Great hornbill")
    m.part("body", "root", (0, 18, 0))
    m.box("body", "core", -3, -3, -5, 6, 6, 10)
    m.box("body", "breast", -2.5, -2.5, -6, 5, 5, 1)
    m.box("body", "back_round", -2.5, -3.5, -4, 5, 1, 8)
    m.box("body", "belly_round", -2.5, 2.5, -3.5, 5, 1, 7)
    m.box("body", "rump", -2, -2.5, 5, 4, 4, 1)
    m.part("neck", "body", (0, -2, -4.5), (0.15, 0, 0))
    m.box("neck", "ruff", -2, -2, -2, 4, 3, 4)               # the shaggy cream collar
    m.box("neck", "neck", -1.5, -3.5, -1.5, 3, 3, 3)
    m.part("head", "neck", (0, -3.5, 0), (-0.15, 0, 0))
    m.box("head", "skull", -1.5, -3, -2.5, 3, 3, 4)
    m.box("head", "nape", -1, -2.5, 1.5, 2, 2, 1)            # short crest of loose feathers behind
    m.box("head", "bill_base", -1, -2.2, -5.5, 2, 2, 3)      # the bill curves down in three steps
    m.box("head", "bill_mid", -1, -1.7, -8, 2, 2, 3)
    m.box("head", "bill_tip", -0.5, -1, -10, 1, 2, 2)
    m.box("head", "casque", -1.5, -4.2, -6.5, 3, 2, 4)       # the casque rides on the upper bill
    m.box("head", "casque_horn", -1.5, -5.2, -7, 3, 1, 2)    # its front corners curl up
    m.box("head", "casque_back", -1.5, -4.7, -3, 3, 1, 1)    # and the back rises a little: the U
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "body", (side * 3, -2.5, -3.5))
        m.box(f"{s}_wing", "wing", -1 if side < 0 else 0, 0, 0, 1, 5, 8)
        m.part(f"{s}_tip", f"{s}_wing", (side * 0.3, 5, 0), (HALF_PI, 0, 0), name=f"{s}_wing_tip")
        m.box(f"{s}_tip", "primaries", -1 if side < 0 else 0, 0, 0, 1, 9, 3)  # folded: the lower 3 rows of the wing
    m.part("tail", "body", (0, -2, 5), (-0.2, 0, 0))
    m.box("tail", "coverts", -2.5, -1, 0, 5, 1, 3)
    m.box("tail", "tail", -2, 0, 0, 4, 2, 11)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_leg", "root", (side * 1.5, 20, 0.5))
        m.box(f"{s}_leg", "thigh", -1, -1, -1, 2, 2, 2)
        m.box(f"{s}_leg", "shank", -0.5, 1, -0.5, 1, 3, 1)
        m.box(f"{s}_leg", "toes", -1, 3, -2.5, 2, 1, 3)
        m.box(f"{s}_leg", "hind_toe", -0.5, 3, 0.5, 1, 1, 1)
    return m


HB = dict(black=(26, 25, 30), sheen=(48, 52, 70), white=(240, 238, 230), cream=(238, 226, 176), stain=(232, 204, 110),
          yellow=(240, 182, 28), ylight=(248, 212, 84), orange=(232, 136, 40), ivory=(244, 232, 176), edge=(24, 20, 20),
          leg=(74, 76, 84), skin=(52, 40, 44), iris=(216, 36, 28), gape=(150, 100, 40))
HB_BODY = ("core", "breast", "back_round", "belly_round", "rump")


def hornbill_eye(mat, face, p, b):
    return mat == "skull" and eye_at(face, p, -2.2, -1.6, HB["iris"]) is not None


def hornbill_colour(v, mat, face, p, P, b):
    c = HB
    x, y, z = p
    black = lerp(c["black"], c["sheen"], 0.6 * feather_rows(P, face, 3))
    if mat in HB_BODY:
        # black back, wings' shoulders and upper breast; white belly, vent and thighs
        if face == "bottom" or P[1] > 19.5 + 0.3 * (P[2] + 2) or (mat == "rump" and face != "top" and y > -0.5):
            return lerp(c["white"], (210, 206, 196), 0.4 * feather_rows(P, face, 4))
        return black
    if mat == "ruff":
        if ragged(face, p, b, 5, 0.45):
            return CLEAR
        return lerp(c["cream"], c["stain"], 0.25 + 0.5 * h(int(x * 3 + z * 2 + 20), int(y * 2 + 9), 6))
    if mat == "neck":
        # cream, stained yellow by the preen oil; the face and chin black
        if face == "front" and y < b["y"] + 1.5:
            return black
        return lerp(c["cream"], c["stain"], 0.2 + 0.45 * h(int(x * 2 + z * 3 + 20), int(y * 2 + 9), 7))
    if mat in ("skull", "nape"):
        e = eye_at(face, p, -2.2, -1.6, c["iris"]) if mat == "skull" else None
        if e:
            return e
        if mat == "skull" and face in SIDES and -2.6 <= y < -0.4 and -2.2 <= z < 0.8:
            return c["skin"]  # the bare dark skin round the eye
        if mat == "nape":
            return lerp(c["cream"], c["stain"], 0.4) if y > b["y"] + 1 else black
        return black
    if mat in ("bill_base", "bill_mid", "bill_tip"):
        if face in SIDES and mat != "bill_tip" and b["y"] + 1 <= y < b["y"] + 1.35 + (0.3 if mat == "bill_mid" else 0):
            return c["gape"]  # the gape line between the mandibles
        if mat == "bill_base" and z > b["z"] + b["d"] - 0.8:
            return lerp(c["edge"], c["yellow"], 0.3)  # dark at the base
        if mat == "bill_tip":
            return lerp(c["ylight"], c["ivory"], rel(-z, 8.5, 10)) if face != "bottom" else c["yellow"]
        return lerp(c["yellow"], c["ylight"], rel(-z, 4, 8) * 0.6) if face != "bottom" else lerp(c["yellow"], c["orange"], 0.3)
    if mat in ("casque", "casque_horn", "casque_back"):
        if face == "front" or (mat == "casque_back" and face in ("back", "top")):
            return c["edge"]  # the black front and back edges of the casque
        if mat == "casque" and face in SIDES and (z < b["z"] + 1 or y > b["y"] + b["h"] - 0.5):
            return c["edge"] if z < b["z"] + 1 else lerp(c["edge"], c["yellow"], 0.4)
        if face == "top" or y < b["y"] + 0.8:
            return lerp(c["orange"], c["yellow"], 0.35 * h(int(x * 2 + 9), int(z * 2 + 20), 8))
        return lerp(c["yellow"], c["orange"], 0.25 * rel(-y, 4, 5) + 0.15 * h(int(z * 2 + 20), int(y * 2 + 20), 9))
    if mat == "wing":
        # black with the broad cream bar across the coverts and white-tipped secondaries along the lower edge
        if 0.7 <= y < 2 and z < 7:
            return lerp(c["cream"], c["stain"], 0.35 * h(int(z * 2), int(y * 2), 10))
        if y >= 4 and int(z + 40) % 2 == 0:
            return c["white"]
        if face == "bottom":
            return c["white"]
        return lerp(black, c["edge"], 0.4) if int(z + 40) % 2 and y > 3 else black
    if mat == "primaries":
        # long black flight feathers (along y), white tips, the feather edges dark
        if y > b["h"] - 2:
            return c["white"] if y > b["h"] - 1.3 or int(z * 2) % 2 == 0 else lerp(c["white"], c["black"], 0.4)
        return lerp(c["black"], c["edge"], 0.6) if int(z + 40) % 2 else lerp(c["black"], c["sheen"], 0.3 * h(int(z), int(y), 11))
    if mat in ("tail", "coverts"):
        if mat == "coverts":
            return lerp(c["black"], c["sheen"], 0.4 * h(int(x * 2 + 9), int(z * 2), 12))
        # white tail with a broad black band; the feather shafts faint
        band = 3.5 <= z < 7
        base = c["black"] if band else c["white"]
        if face == "top" and int(x + 40) % 2 == 0:
            base = shade(base, 0.9 if not band else 1.25)
        return base
    if mat == "thigh":
        return lerp(c["white"], (206, 200, 190), 0.4 * h(int(x * 2 + 9), int(y * 2 + 9), 13))
    if mat in ("shank", "toes", "hind_toe"):
        if mat == "toes" and face in ("front", "top") and z < b["z"] + 0.8:
            return (34, 32, 36)  # claws
        return lerp(c["leg"], (52, 52, 58), 0.4 if int(y * 2 + z * 2 + 40) % 2 else 0.0)
    raise KeyError(mat)


# ============================================================================ red jungle fowl
def fowl_model():
    """JungleFowlModel's parts; the body's underside on y 20, so the roost (body.y + 4) sits it on the ground."""
    m = Model("Red jungle fowl")
    m.part("body", "root", (0, 18, 0), (-0.25, 0, 0))
    m.box("body", "core", -2.5, -2, -3.5, 5, 4, 7)
    m.box("body", "breast", -2, -1.5, -4.5, 4, 3, 1)
    m.box("body", "back_round", -2, -2.8, -3, 4, 1, 6)
    m.box("body", "saddle", -3, -2.4, 0.5, 6, 3, 3)          # the saddle hackles hanging over the rump
    m.part("head", "body", (0, -1.5, -3), (0.25, 0, 0))
    m.box("head", "neck", -1.5, -3.5, -1.5, 3, 4, 3)
    m.box("head", "hackle", -2, -2, -1.5, 4, 3, 4)            # the hackle cape flowing over the shoulders
    m.box("head", "skull", -1.5, -6, -2, 3, 3, 3)
    m.box("head", "beak", -0.5, -5, -3.6, 1, 1, 2)
    m.box("head", "hen_comb", -0.5, -6.8, -1.6, 1, 1, 2)      # the hen's small comb (covered by the rooster's)
    m.part("comb", "head")
    m.box("comb", "comb", -0.5, -8.5, -2.6, 1, 3, 4)
    m.box("comb", "wattle", -1, -4, -2.8, 2, 2, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "body", (side * 2.5, -1.8, -2.5))
        m.box(f"{s}_wing", "wing", -1 if side < 0 else 0, 0, 0, 1, 4, 6)
    m.part("tail", "body", (0, -1.5, 3), (-0.6, 0, 0))
    m.box("tail", "tail", -1, -5, 0, 2, 5, 3)                 # the folded fan, tall and narrow
    m.part("sickle", "tail", (0, -4.5, 1), (1.25, 0, 0))
    m.box("sickle", "sickle", -0.5, -1, 0, 1, 2, 6)
    m.box("sickle", "sickle_covert", -1, -0.5, -1, 2, 2, 3)
    m.part("sickle_tip", "sickle", (0, 0, 5.5), (-1.35, 0, 0))
    m.box("sickle_tip", "sickle_end", -0.5, -1, 0, 1, 2, 7)
    m.box("sickle_tip", "sickle_low", -0.5, 1, -0.5, 1, 1, 5)  # a second, shorter sickle below
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_leg", "root", (side * 1.1, 20, 0.5))
        m.box(f"{s}_leg", "thigh", -1, -1.5, -1, 2, 2, 2)
        m.box(f"{s}_leg", "shank", -0.5, 0.5, -0.5, 1, 3, 1)
        m.box(f"{s}_leg", "spur", -0.5, 2, 0.5, 1, 1, 1)
        m.box(f"{s}_leg", "toes", -1, 3, -2.5, 2, 1, 3)
    return m


JF = {
    "rooster": dict(gold=(240, 158, 42), straw=(250, 206, 104), rust=(196, 84, 30), saddle=(160, 50, 26), maroon=(112, 30, 22),
                    green=(26, 38, 32), gsheen=(46, 96, 70), bsheen=(40, 60, 110), chestnut=(156, 92, 40), brown=(84, 52, 30),
                    red=(214, 32, 30), red_d=(150, 20, 22), face=(214, 60, 52), lobe=(238, 232, 220)),
    "hen": dict(gold=(206, 150, 70), straw=(220, 182, 110), rust=(150, 96, 52), saddle=(128, 90, 56), maroon=(92, 62, 40),
                green=(70, 50, 34), gsheen=(96, 72, 48), bsheen=(96, 72, 48), chestnut=(140, 98, 58), brown=(76, 52, 34),
                red=(178, 60, 50), red_d=(130, 40, 36), face=(186, 102, 84), lobe=(186, 102, 84)),
}
JF_LEG, JF_BEAK = (122, 126, 134), (160, 140, 108)


def fowl_eye(mat, face, p, b):
    return mat == "skull" and face in SIDES and -5 <= p[1] < -4 and -1.4 <= p[2] < 0.6


def fowl_colour(v, mat, face, p, P, b):
    c = JF[v]
    x, y, z = p
    rooster = v == "rooster"

    def mottle(base, salt):
        """The hen's vermiculated brown: dark bars and pale shaft streaks in model space."""
        n = h(int(P[0] * 2 + 40), int(P[1] * 2 + P[2] * 2 + 90), salt)
        bar = int(P[1] * 2 + P[2] * 1.3 + 90) % 3 == 0
        return lerp(base, c["brown"], 0.55) if bar and n < 0.7 else lerp(base, c["straw"], 0.35) if n > 0.85 else base

    def hackles(salt):
        """Long pointed hackle feathers: gold edges, darker shaft streaks, paler towards the top."""
        a = (P[2] if face in SIDES else P[0]) * 1.5
        stripe = int(a + 40) % 2 == 0 and h(int(a + 40), 3, salt) < 0.8
        base = lerp(c["gold"], c["straw"], rel(-P[1], 11, 17))
        if not rooster:
            return lerp(c["brown"], c["gold"], 0.7) if not stripe else c["brown"]
        return lerp(base, c["rust"], 0.55) if stripe else base

    if mat in ("core", "breast", "back_round"):
        if rooster:
            if face == "top" or (face in SIDES and y < -0.8 and z > -2.5) or mat == "back_round":
                return lerp(c["saddle"], c["maroon"], 0.5 * feather_rows(P, face, 20))  # red-brown back
            return lerp(c["green"], c["gsheen"], 0.55 * feather_rows(P, face, 21))  # green-black breast and belly
        if face == "front" or face == "bottom" or mat == "breast" or (face in SIDES and y > 1):
            return mottle(lerp(c["chestnut"], c["straw"], 0.25), 22)  # paler rufous breast
        return mottle(c["saddle"], 23)
    if mat == "saddle":
        if ragged(face, p, b, 24, 0.55) or face == "front":
            return CLEAR
        if not rooster:
            return mottle(c["saddle"], 25)
        a = (z if face in SIDES else x) * 1.5
        return lerp(c["rust"], c["gold"], 0.5 * rel(-y, 0, 2.4)) if int(a + 40) % 2 else lerp(c["saddle"], c["rust"], 0.5)
    if mat == "hackle":
        if ragged(face, p, b, 26, 0.5) or face == "top":
            return CLEAR
        return hackles(27)
    if mat == "neck":
        if face == "front" and not rooster:
            return lerp(c["chestnut"], c["straw"], 0.4)
        return hackles(28)
    if mat == "skull":
        if fowl_eye(mat, face, p, b):
            return (24, 14, 10) if p[2] < -0.4 else (220, 120, 40)  # dark pupil, orange iris behind it
        if face in SIDES and y >= -4 and z < 0.4:
            return c["face"]  # the bare red face below and behind the eye
        if face in SIDES and y >= -4.2 and z >= 0.4:
            return c["lobe"]  # the white ear lobe
        if face in SIDES and y < -5:
            return lerp(c["gold"], c["rust"], 0.4)
        if face == "front":
            return c["face"]
        return lerp(c["rust"], c["gold"], 0.4 * h(int(x * 2 + 9), int(z * 2 + 9), 29)) if rooster else mottle(c["chestnut"], 30)
    if mat == "beak":
        return lerp(JF_BEAK, (90, 74, 60), 0.5) if face == "top" or z < b["z"] + 0.6 else JF_BEAK
    if mat in ("comb", "hen_comb", "wattle"):
        if mat == "comb" and face != "bottom" and y < b["y"] + 1 and int(z + 40) % 2 == 1:
            return CLEAR  # the serrated points of the comb
        base = lerp(c["red"], c["red_d"], 0.35 * rel(y, b["y"], b["y"] + b["h"]) + 0.2 * h(int(z * 2 + 9), int(y * 2 + 20), 31))
        return shade(base, 1.1) if face == "top" else base
    if mat == "wing":
        if not rooster:
            if y >= 3 and int(z + 40) % 2 == 0:
                return lerp(c["brown"], c["straw"], 0.3)
            return mottle(c["saddle"], 32)
        if z < 3 and y < 2:
            return lerp(c["maroon"], c["saddle"], 0.4 * h(int(z * 2), int(y * 2), 33))  # the dark red shoulder
        if 2 <= y < 3 and z < 5:
            return lerp(c["green"], c["gsheen"], 0.5 + 0.3 * h(int(z * 2), 2, 37))  # the glossy green bar across the wing
        if z > 4.5:
            return lerp(c["brown"], (36, 28, 22), 0.4) if int(y + 40) % 2 else c["brown"]  # dark primaries
        return lerp(c["chestnut"], c["brown"], 0.6) if int(z + 40) % 2 else c["chestnut"]  # chestnut secondaries
    if mat == "tail":
        if not rooster:
            return lerp(c["brown"], c["green"], 0.5) if int(z * 2 + 40) % 2 else c["brown"]
        return lerp(c["green"], c["gsheen"], 0.6 * h(int(z * 2 + 9), int(y * 2 + 20), 34) + (0.3 if face == "top" else 0))
    if mat in ("sickle", "sickle_covert", "sickle_end", "sickle_low"):
        if mat == "sickle_end" and z > b["z"] + b["d"] - 1.5 and (face == "top" or y < b["y"] + 1):
            return CLEAR  # the pointed tip
        sheen = 0.8 if face == "top" or y < b["y"] + 1 else 0.3
        return lerp(c["green"], c["bsheen"] if int(z + 40) % 3 == 0 else c["gsheen"], sheen * (0.5 + 0.5 * h(int(z * 2), int(y), 35)))
    if mat == "thigh":
        return lerp(c["green"], c["gsheen"], 0.3) if rooster else mottle(c["saddle"], 36)
    if mat == "spur":
        return lerp(JF_LEG, (210, 200, 180), 0.4) if rooster else CLEAR
    if mat in ("shank", "toes"):
        if mat == "toes" and z < b["z"] + 0.8 and face in ("front", "top"):
            return (60, 56, 52)
        return lerp(JF_LEG, (88, 92, 100), 0.45 if int(y * 2 + z * 2 + 40) % 2 else 0.0)
    raise KeyError(mat)


# ============================================================================ peafowl
PANELS, PANEL_W, PANEL_L = 7, 9, 20
TRAIL, TRAIL_SPREAD, TRAIL_ROLL = -HALF_PI - 0.3, 0.05, 0.2


def peafowl_model():
    """PeafowlModel's parts; the body's underside on y 20 (roost: body.y + 4); the train is seven panels hinged at the
    rump, trailing as in the layer (the model fans them by changing pitch, yaw and roll)."""
    m = Model("Peafowl")
    m.part("body", "root", (0, 17.5, 0), (-0.15, 0, 0))
    m.box("body", "core", -2.5, -2.5, -4.5, 5, 5, 9)
    m.box("body", "breast", -2, -2, -5.5, 4, 4, 1)
    m.box("body", "back_round", -2, -3.2, -3.5, 4, 1, 8)
    m.part("head", "body", (0, -2, -4), (0.15, 0, 0))
    m.box("head", "neck_low", -1.5, -3, -1.5, 3, 4, 3)
    m.box("head", "neck_up", -1, -7, -1, 2, 4, 2)
    m.box("head", "skull", -1, -9.5, -2, 2, 3, 3)
    m.box("head", "beak", -0.5, -8.2, -3.5, 1, 1, 2)
    m.part("crest", "head", (0, -9.5, -0.5), (-0.25, 0, 0))
    m.box("crest", "crest", -1.5, -3, 0, 3, 3, 1)             # the fan of bare wires with blue tufts
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "body", (side * 2.5, -2.2, -3.5))
        m.box(f"{s}_wing", "wing", -1 if side < 0 else 0, 0, 0, 1, 4, 8)
        m.box(f"{s}_wing", "primaries", (-1 if side < 0 else 0) + side * 0.1, 1.5, 7, 1, 2, 3)
    m.part("tail", "body", (0, -2, 4.5), (0.1, 0, 0))
    m.box("tail", "tail", -2, -1, 0, 4, 2, 6)
    m.part("train", "body", (0, -1.5, 4))
    for i in range(PANELS):
        k = i - PANELS // 2
        m.part(f"train{i}", "train", rot=(TRAIL, k * TRAIL_SPREAD, k * TRAIL_ROLL), name=f"train_{i}")
        m.box(f"train{i}", f"panel{i}", -PANEL_W / 2, -PANEL_L, -0.5 + 0.15 * abs(k), PANEL_W, PANEL_L, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_leg", "root", (side * 1.2, 19, 0.5))
        m.box(f"{s}_leg", "thigh", -1, -1.5, -1, 2, 2, 2)
        m.box(f"{s}_leg", "shank", -0.5, 0.5, -0.5, 1, 4, 1)
        m.box(f"{s}_leg", "toes", -1, 4, -2.5, 2, 1, 3)
        m.box(f"{s}_leg", "hind_toe", -0.5, 4, 0.5, 1, 1, 1)
    return m


PF = dict(blue=(26, 76, 190), blue_l=(70, 150, 232), blue_d=(20, 44, 120), teal=(44, 150, 128), green=(70, 116, 52),
          bronze=(170, 134, 56), dark=(28, 30, 40), buff=(196, 174, 132), bar=(62, 48, 36), chestnut=(178, 98, 44),
          white=(240, 238, 230), grey=(120, 108, 96), navy=(18, 22, 70), gold=(196, 160, 60), fringe=(86, 128, 52))
PH = dict(brown=(118, 96, 74), dark=(76, 60, 46), pale=(164, 142, 112), green=(52, 128, 76), green_l=(96, 170, 100),
          white=(236, 232, 220))
PF_LEG, PF_BEAK = (140, 126, 112), (176, 160, 138)


def peafowl_eye(mat, face, p, b):
    return mat == "skull" and face in SIDES and -8.5 <= p[1] < -7.5 and -1.2 <= p[2] < -0.2


def panel_shape(p):
    """The panel's outline: narrow at the hinge, widening to a rounded tip."""
    r = -p[1]
    half = 1.0 + (PANEL_W / 2 - 1) * min(1.0, r / (PANEL_L - 3))
    if r > PANEL_L - 1.5:
        half -= 1.5  # rounded tip
    return abs(p[0]) <= half


def ocellus(px, r, i):
    """The eye-spot colour at panel point (px, r) or None: navy heart, blue, teal ring, bronze ring, green fringe.
    Two big eyes side by side near the tip, one in the middle, one small near the hinge; odd panels shifted, so the
    rows interlock in the fan."""
    shift = 1.2 if i % 2 else 0.0
    for cx, cy, rx, ry in ((-2.3, 17.0, 2.0, 2.0), (2.3, 17.0, 2.0, 2.0), (0.0, 12.3 - shift, 2.3, 2.1), (0.0, 7.3 - shift, 1.6, 1.5)):
        d = math.hypot((px - cx) / rx, (r - cy) / ry)
        if d <= 1.0:
            return PF["navy"] if d < 0.4 else PF["blue"] if d < 0.62 else PF["teal"] if d < 0.76 else PF["bronze"]
        if d <= 1.25:
            return PF["fringe"]
    return None


def peafowl_colour(v, mat, face, p, P, b):
    x, y, z = p
    cock = v == "peacock"
    if mat.startswith("panel"):
        if face not in ("front", "back") or not panel_shape(p):
            return CLEAR
        r = -y
        if face == "back":
            return lerp(PF["grey"], (90, 80, 70), 0.4 * h(int(x * 2 + 9), int(r), 50))  # the plain underside
        e = ocellus(x, r, int(mat[5:]))
        if e:
            return e
        # bronze-green barbs, fine and streaky, darker along the shaft
        barb = h(int(x * 2 + 20), int(r * 0.7), 51)
        base = lerp(PF["green"], PF["gold"], 0.2 + 0.5 * barb * rel(r, 2, 14))  # greener at the hinge, bronze out
        return lerp(base, PF["bar"], 0.45) if abs(x) < 0.5 else base
    if mat in ("core", "breast", "back_round"):
        if not cock:
            if face == "bottom" or (face in SIDES and y > 1.2) or (face == "back" and y > 0):
                return PH["white"]  # white belly
            if face == "front" or mat == "breast":
                return lerp(PH["green"], PH["green_l"], 0.5 * feather_rows(P, face, 52)) if y < 0 else lerp(PH["brown"], PH["pale"], 0.4)
            return lerp(PH["brown"], PH["dark"], 0.6 * feather_rows(P, face, 53))
        if face == "top" or mat == "back_round" or (face in SIDES and y < -1.2 and z > -2):
            # the scaled green back: dark-edged bronze-green scales
            s = feather_rows(P, face, 54)
            return lerp(PF["green"], PF["dark"], 0.6) if s > 0.7 else lerp(PF["green"], PF["bronze"], s * 0.6)
        if face == "bottom" or (face in SIDES and y > 1.2) or face == "back":
            return PF["dark"]
        return lerp(PF["blue"], PF["blue_l"], 0.6 * feather_rows(P, face, 55))  # blue breast
    if mat in ("neck_low", "neck_up"):
        if not cock:
            if mat == "neck_up" and face == "back":
                return lerp(PH["brown"], PH["dark"], 0.3)
            return lerp(PH["green"], PH["green_l"], 0.5 * feather_rows(P, face, 56, 1.0))
        return lerp(PF["blue"], PF["blue_l"], 0.7 * feather_rows(P, face, 57, 1.0))
    if mat == "skull":
        if peafowl_eye(mat, face, p, b):
            return (14, 10, 10) if z < -0.7 else (250, 246, 236)
        if face in SIDES and y < -8.5 and z < 0.5:
            return PF["white"]  # the white stripe over the eye
        if face in SIDES and -7.5 <= y < -6.6 and z < 0.4:
            return PF["white"]  # and the white crescent under it
        if not cock:
            return PH["white"] if face in SIDES and y > -8 else lerp(PH["brown"], PH["dark"], 0.3)
        return lerp(PF["blue"], PF["blue_l"], 0.5 * h(int(x * 2 + 9), int(y * 2 + z * 2 + 40), 58))
    if mat == "beak":
        return shade(PF_BEAK, 0.8) if face == "bottom" else PF_BEAK
    if mat == "crest":
        if face not in ("front", "back"):
            return CLEAR
        if y < b["y"] + 1:
            return PF["blue"] if cock else PH["green"]  # the tufted tips
        if y < b["y"] + 2 or abs(x) < 0.5:
            return PF["dark"] if cock else PH["dark"]  # the bare wires, fanning out from the crown
        return CLEAR
    if mat == "wing":
        if not cock:
            if y > 3 and int(z + 40) % 2 == 0:
                return PH["pale"]  # pale edges of the flight feathers
            return lerp(PH["brown"], PH["pale"], 0.5 * feather_rows(P, face, 59)) if int(y * 2 + z + 40) % 3 else PH["dark"]
        if z > 5.5:
            return lerp(PF["chestnut"], PF["bar"], 0.3 if int(y + 40) % 2 else 0.0)  # chestnut flight feathers
        if z > 4 or y > 3:
            return lerp(PF["bar"], (70, 58, 44), 0.5 if int(z + 40) % 2 else 0.0)  # dark secondaries
        return lerp(PF["buff"], PF["bar"], 0.75) if int(z * 1.5 + y * 0.5 + 40) % 2 == 0 else PF["buff"]  # barred shoulder
    if mat == "primaries":
        if not cock:
            return lerp(PH["dark"], PH["brown"], 0.4)
        return lerp(PF["chestnut"], (120, 64, 30), 0.4 if int(z + 40) % 2 else 0.0)
    if mat == "tail":
        if not cock:
            if z > b["z"] + b["d"] - 1:
                return PH["pale"]  # pale tips
            return PH["dark"] if int(z * 1.5 + 40) % 3 == 0 else lerp(PH["brown"], PH["dark"], 0.3)  # barred brown
        return lerp((110, 92, 72), (80, 66, 52), 0.5 if int(x + 40) % 2 else 0.0)
    if mat == "thigh":
        return lerp(PF["dark"], PF["blue_d"], 0.3) if cock else lerp(PH["brown"], PH["pale"], 0.3)
    if mat in ("shank", "toes", "hind_toe"):
        if mat == "toes" and z < b["z"] + 0.8 and face in ("front", "top"):
            return (70, 62, 56)
        return lerp(PF_LEG, (108, 96, 86), 0.45 if int(y * 2 + z * 2 + 40) % 2 else 0.0)
    raise KeyError(mat)


# ============================================================================ main
def main():
    pea_gone = tuple(f"panel{i}" for i in range(PANELS)) + ("crest",)
    models = [
        (hornbill_model(), "hornbill", {"hornbill": None},
         plucked(hornbill_colour, ("bill_base", "bill_mid", "bill_tip", "casque", "casque_horn", "casque_back", "shank", "toes",
                                   "hind_toe"), quills=("tail", "primaries"), eye=hornbill_eye), hornbill_colour),
        (fowl_model(), "jungleFowl", {"jungle_fowl_rooster": "rooster", "jungle_fowl_hen": "hen"},
         plucked(fowl_colour, ("beak", "comb", "hen_comb", "wattle", "spur", "shank", "toes"), quills=("tail",),
                 gone=("sickle", "sickle_covert", "sickle_end", "sickle_low", "saddle", "hackle"), eye=fowl_eye), fowl_colour),
        (peafowl_model(), "peafowl", {"peacock": "peacock", "peahen": "peahen"},
         plucked(peafowl_colour, ("beak", "shank", "toes", "hind_toe"), quills=("tail", "primaries"), gone=pea_gone, eye=peafowl_eye,
                 light=(236, 196, 168), dark=(192, 140, 112)), peafowl_colour),
    ]
    methods = []
    for m, method, skins, bare, colour in models:
        m.pack()
        methods.append(m.java(method))
        for name, variant in skins.items():
            save(m.paint(colour, variant), f"wildlife/{name}.png")
            save(m.paint(bare, variant), f"skinned/wildlife/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.warm;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_owarm_birds_models.py (with the skins it paints): the warm birds' parts and boxes. Do not edit. */
public final class WarmBirdGeometry {{
{chr(10).join(methods)}
    private WarmBirdGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
