"""The vanilla farm animals rebuilt in the mod's style, each from one table (client/vanilla/FarmAnimalRenderers):

- src/main/java/com/mechanical/client/vanilla/VanillaGeometry.java: every part and box (generated, do not edit)
- textures/entity/vanilla/<animal>/<vanilla texture name>.png (one per vanilla variant texture) and the skinned
  carcass skins in textures/entity/skinned/vanilla/ (plucked skin for the chicken)

Cow: a landrace cow, smaller and finer than her wild ancestor the aurochs, with a deep barrel, hip bones, udder, neck
and dewlap, a long face with a broad wet muzzle, sideways ears under short horns, and a long tail with a switch.
  temperate = dark brown pied with white (vanilla's brown-and-white cow), warm = a red zebu with a shoulder hump, big
  dewlap, drooping ears and lyre horns, cold = a shaggy ginger highland cow with a fringe over the eyes and wide horns.
Pig: an old-breed pig, longer in the leg and snout than a modern one, with a bristly crest, heavy jowls, a snout disc,
  forward-flopping ears and a curly tail; temperate pink, warm red-brown with a dark saddle, cold a curly-coated
  woolly pig (its own model, the coat stands out).
Sheep: a hornless primitive sheep (thin legs, roman nose, sideways ears) with a separate fleece model the wool colour
  tints (grey-white locks, so dyed wool stays clean); sheared it shows its stubbly skin.
Chicken: a landrace hen: egg-shaped body, upturned tail fan, hackles, red comb and wattles, yellow scaled legs with
  toes; temperate white, warm a red-gold partridge hen, cold a fluffier barred grey hen with feathered legs. Chicks are
  balls of down with a head.
Every baby has its own model (calf, piglet, lamb with its own fleece, chick) and the vanilla variant's colours.

Same idea as gen_savanna_models.py (its Model class): every box is packed into the skin and painted from the 3D point
each texel covers. Part names are the ones the vanilla models look up (head, body, legs; wings for the chicken), so
vanilla animation, the livestock poses and the carcass poses keep working.
Usage: python scripts/gen_vanilla_models.py. Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import CLEAR, Model, rel  # noqa: E402
from wildlife_paint import h, lerp, save  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/vanilla/VanillaGeometry.java"


def noise3(P, cell, salt):
    """Smooth value noise in model space (0..1): patches that run on across faces and boxes."""
    q = [P[a] / cell for a in range(3)]
    b = [math.floor(v) for v in q]
    f = [q[a] - b[a] for a in range(3)]
    f = [t * t * (3 - 2 * t) for t in f]

    def v(i, j, k):
        return h((b[0] + i) * 73 + (b[2] + k) * 19, b[1] + j, salt)

    x00 = v(0, 0, 0) + (v(1, 0, 0) - v(0, 0, 0)) * f[0]
    x10 = v(0, 1, 0) + (v(1, 1, 0) - v(0, 1, 0)) * f[0]
    x01 = v(0, 0, 1) + (v(1, 0, 1) - v(0, 0, 1)) * f[0]
    x11 = v(0, 1, 1) + (v(1, 1, 1) - v(0, 1, 1)) * f[0]
    y0 = x00 + (x10 - x00) * f[1]
    y1 = x01 + (x11 - x01) * f[1]
    return y0 + (y1 - y0) * f[2]


def strands(c, p, face, amount=0.16, period=3):
    """Long hair: light and dark streaks running down the sides (along y), across the top (along z)."""
    x, y, z = p
    if face in ("left", "right"):
        k = h(int(z * 2), int(y // period), 17)
    elif face in ("front", "back"):
        k = h(int(x * 2), int(y // period), 23)
    else:
        k = h(int(x * 2), int(z // period), 29)
    return tuple(max(0, min(255, round(v * (1 - amount + 2 * amount * k)))) for v in c)


def ragged_bottom(face, p, b, depth=1.5, salt=0):
    """Transparent, uneven lower edge of a hanging fringe (sides only)."""
    if face == "bottom":
        return True
    if face == "top":
        return False
    x, y, z = p
    edge = b["y"] + b["h"] - y
    k = h(int(x * 3 + z * 5), salt, 31)
    return edge < depth * k


def eye(face, p, b, y0, z0, w=2):
    """A side-facing eye: True for the dark eye texels on the left/right face (z0 = its front edge, from the front)."""
    if face not in ("left", "right"):
        return False
    return y0 <= p[1] < y0 + 1 and z0 <= p[2] < z0 + w


def under(face, p, b, P0):
    """0 on the back, 1 on the belly: the side shading position for a body box."""
    if face == "top":
        return 0.0
    if face == "bottom":
        return 1.0
    return rel(p[1], b["y"], b["y"] + b["h"])


def inner_leg(face, P):
    return (face == "left" and P[0] < 0) or (face == "right" and P[0] > 0)


# ============================================================================ cow
def cattle_legs(m, front, hind, fl=11, hl=12, w=3):
    """Front legs: forearm, cannon, dewclaw and a cloven hoof; hind: a broad thigh, the hock set back, cannon, hoof."""
    for side, s in ((-1, "right"), (1, "left")):
        fx, fy, fz = front
        m.part(f"{s}_front_leg", "root", (side * fx, fy, fz))
        m.box(f"{s}_front_leg", "forearm", -w / 2, -1, -2, w, 6, 4)
        m.box(f"{s}_front_leg", "cannon", -1, 5, -1, 2, fl - 7, 2)
        m.box(f"{s}_front_leg", "dewclaw", -0.5, fl - 3, 1, 1, 1, 1)
        m.box(f"{s}_front_leg", "hoof", -w / 2, fl - 2, -1.5 - (w - 3) / 2, w, 2, w)
        hx, hy, hz = hind
        m.part(f"{s}_hind_leg", "root", (side * hx, hy, hz))
        m.box(f"{s}_hind_leg", "thigh", -w / 2 - 0.5, -2, -2.5, w + 1, 7, 5)
        m.box(f"{s}_hind_leg", "gaskin", -w / 2, 5, -1, w, 3, 3)
        m.box(f"{s}_hind_leg", "cannon", -1, 8, -0.5, 2, hl - 10, 2)
        m.box(f"{s}_hind_leg", "dewclaw", -0.5, hl - 3, 1.5, 1, 1, 1)
        m.box(f"{s}_hind_leg", "hoof", -w / 2, hl - 2, -1.5 - (w - 3) / 2, w, 2, w)


def cow_model(kind):
    m = Model({"temperate": "Cow", "warm": "Warm cow", "cold": "Cold cow"}[kind])
    m.part("body", "root", (0, 10, 0))
    m.box("body", "chest", -5.5, -6, -10, 11, 10, 8)
    m.box("body", "withers", -4, -7.5, -9.5, 8, 2, 6)
    m.box("body", "barrel", -6, -6, -3, 12, 11, 9)
    m.box("body", "hips", -5.5, -6.5, 5, 11, 9, 6)
    m.box("body", "rump", -4, -7.5, 5.5, 8, 1, 5)           # the flat sacrum between the hip bones
    m.box("body", "hook", 4.5, -7, 5.5, 2, 2, 2)            # hip bones standing out of the flank
    m.box("body", "hook", -6.5, -7, 5.5, 2, 2, 2)
    m.box("body", "brisket", -2.5, 2, -11, 5, 4, 3)
    m.box("body", "belly", -5, 5, -2, 10, 1, 8)             # the rounded underline of the barrel
    m.box("body", "udder", -2.5, 4.5, 3, 5, 3, 5)
    for tx in (-1.5, 0.5):
        for tz in (3.5, 6.5):
            m.box("body", "teat", tx, 7.5, tz, 1, 1, 1)
    if kind == "warm":
        m.box("body", "hump", -2.5, -10, -9, 5, 3, 5)       # the zebu's fatty shoulder hump
        m.box("body", "dewlap_low", -1, 4, -9.5, 2, 4, 7)   # the dewlap runs on under the chest
    if kind == "cold":
        m.box("body", "skirt", -6.5, 3, -9, 13, 4, 19)      # the shaggy coat hanging below the belly line
        m.box("body", "mane", -5, -8.5, -10, 10, 3, 9)      # thick hair over the shoulders
    m.part("tail", "body", (0, -7, 11), (0.12, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 12, 1)
    m.box("tail", "switch", -1, 11, -1, 2, 4, 2)

    m.part("head", "root", (0, 5.5, -9.5))
    m.part("neck", "head", (0, 0, 0), (-0.1, 0, 0))
    m.box("neck", "neck", -3.5, -4, -7, 7, 8, 8)
    m.box("neck", "crest", -2.5, -5, -6, 5, 1, 6)
    m.box("neck", "dewlap", -1, 4, -6.5, 2, 3 if kind != "warm" else 5, 6)
    if kind == "cold":
        m.box("neck", "neck_shag", -4, -4.5, -7.5, 8, 10, 7)
    m.part("skull", "neck", (0, -0.5, -6.5), (0.8, 0, 0))
    m.box("skull", "poll", -3.5, -4, -4, 7, 6, 4)
    m.box("skull", "face", -3, -3.5, -8, 6, 5, 4)
    m.box("skull", "muzzle", -3, -3, -10, 6, 4, 2)
    m.box("skull", "jaw", -2.5, 1, -9.5, 5, 2, 6)
    if kind == "cold":
        m.box("skull", "fringe", -4, -5, -5.5, 8, 5, 3)       # the forelock over the eyes
    ear_l, ear_drop = (2, 0.9) if kind == "warm" else (3, 0.35)
    for side, s in ((-1, "right"), (1, "left")):
        if kind == "warm":   # long drooping ears
            m.part(f"{s}_ear", "skull", (side * 3.5, -2, -2), (0.2, 0, side * 1.2))
            m.box(f"{s}_ear", "ear", 0 if side > 0 else -4, -0.5, -1, 4, 1, 2)
        else:
            m.part(f"{s}_ear", "skull", (side * 3.5, -2.5, -2.5), (0, 0, side * ear_drop))
            m.box(f"{s}_ear", "ear", 0 if side > 0 else -ear_l, -0.5, -1, ear_l, 1, 2)
        if kind == "temperate":   # short horns: out, then curving forward and up
            m.part(f"{s}_horn", "skull", (side * 3, -3.8, -3), (0, 0, side * 1.25))
            m.box(f"{s}_horn", "horn", -0.5, -3, -0.5, 1, 3, 1)
            m.part(f"{s}_horn_tip", f"{s}_horn", (0, -3, 0), (0.5, 0, -side * 1.0), name="tip")
            m.box(f"{s}_horn_tip", "horn_tip", -0.5, -2, -0.5, 1, 2, 1)
        elif kind == "warm":      # lyre horns: up and out, the tips turning in
            m.part(f"{s}_horn", "skull", (side * 3, -3.5, -3), (-0.2, 0, side * 1.0))
            m.box(f"{s}_horn", "horn", -1, -2, -1, 2, 2, 2)
            m.part(f"{s}_horn_mid", f"{s}_horn", (0, -2, 0), (-0.25, 0, -side * 0.6), name="mid")
            m.box(f"{s}_horn_mid", "horn", -0.5, -2, -0.5, 1, 2, 1)
            m.part(f"{s}_horn_tip", f"{s}_horn_mid", (0, -2, 0), (0.2, 0, -side * 0.5), name="tip")
            m.box(f"{s}_horn_tip", "horn_tip", -0.5, -2, -0.5, 1, 2, 1)
        else:                     # wide highland horns: out level, sweeping up at the ends
            m.part(f"{s}_horn", "skull", (side * 3.5, -3.8, -3), (0.25, 0, side * 1.45))
            m.box(f"{s}_horn", "horn", -1, -4, -1, 2, 4, 2)
            m.part(f"{s}_horn_mid", f"{s}_horn", (0, -4, 0), (0, 0, -side * 0.4), name="mid")
            m.box(f"{s}_horn_mid", "horn", -0.5, -3, -0.5, 1, 3, 1)
            m.part(f"{s}_horn_tip", f"{s}_horn_mid", (0, -3, 0), (0.2, 0, -side * 0.7), name="tip")
            m.box(f"{s}_horn_tip", "horn_tip", -0.5, -3, -0.5, 1, 3, 1)
    cattle_legs(m, (3.8, 13, -6.5), (3.8, 12, 7.5))
    if kind == "cold":
        for side, s in ((-1, "right"), (1, "left")):
            m.box(f"{s}_front_leg", "leg_shag", -2, -1, -2.5, 4, 7, 5)
            m.box(f"{s}_hind_leg", "leg_shag", -2.5, -2, -3, 5, 8, 6)
    return m


def calf_model():
    m = Model("Calf")
    m.part("body", "root", (0, 15, 0))
    m.box("body", "chest", -3.5, -3.5, -6, 7, 6, 5)
    m.box("body", "barrel", -3.5, -3.5, -1.5, 7, 6, 6)
    m.box("body", "hips", -3.5, -4, 4, 7, 6, 4)
    m.part("tail", "body", (0, -3.5, 8), (0.15, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 6, 1)
    m.box("tail", "switch", -0.5, 5, -0.5, 1, 2, 1)
    m.part("head", "root", (0, 12.5, -5.5))
    m.part("neck", "head", (0, 0, 0), (-0.1, 0, 0))
    m.box("neck", "neck", -2, -2.5, -4, 4, 5, 5)
    m.part("skull", "neck", (0, -0.5, -3.5), (0.55, 0, 0))
    m.box("skull", "poll", -2.5, -3, -4, 5, 5, 4)
    m.box("skull", "face", -2, -2.5, -6.5, 4, 4, 3)
    m.box("skull", "muzzle", -2, -2, -8.5, 4, 3, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "skull", (side * 2.5, -2, -1.5), (0, 0, side * 0.45))
        m.box(f"{s}_ear", "ear", 0 if side > 0 else -3, -0.5, -1, 3, 1, 2)
        m.box("skull", "horn_bud", side * 1.6 - 0.5, -3.6, -2.5, 1, 1, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 2.2, 16, -4))
        m.box(f"{s}_front_leg", "forearm", -1, -1, -1, 2, 5, 2)
        m.box(f"{s}_front_leg", "cannon", -1, 4, -1, 2, 3, 2)
        m.box(f"{s}_front_leg", "hoof", -1, 7, -1.5, 2, 1, 2)
        m.part(f"{s}_hind_leg", "root", (side * 2.2, 15.5, 4.5))
        m.box(f"{s}_hind_leg", "thigh", -1.5, -1.5, -1.5, 3, 5, 3)
        m.box(f"{s}_hind_leg", "cannon", -1, 3.5, -0.5, 2, 4, 2)
        m.box(f"{s}_hind_leg", "hoof", -1, 7.5, -1, 2, 1, 2)
    return m


COW = {
    "temperate": dict(coat=(70, 46, 34), coat2=(52, 34, 26), white=(234, 228, 216), muzzle=(176, 140, 130), nose=(120, 86, 80),
                      horn=(222, 210, 180), horn_tip=(70, 60, 50), hoof=(44, 36, 32), switch=(40, 30, 24)),
    "warm": dict(coat=(158, 80, 44), coat2=(126, 58, 32), white=(214, 160, 110), muzzle=(54, 40, 36), nose=(36, 28, 26),
                 horn=(200, 190, 168), horn_tip=(64, 56, 48), hoof=(40, 32, 28), switch=(36, 26, 22)),
    "cold": dict(coat=(184, 104, 50), coat2=(140, 70, 32), white=(222, 170, 110), muzzle=(150, 104, 90), nose=(70, 48, 44),
                 horn=(230, 216, 186), horn_tip=(74, 62, 50), hoof=(48, 38, 32), switch=(110, 56, 26)),
}
EYE, PINK, TEAT = (22, 16, 14), (220, 164, 152), (196, 128, 122)


def cow_colour(v, mat, face, p, P, b):
    kind, baby = v.split(":")[0], v.endswith(":baby")
    C = COW[kind]
    x, y, z = p
    coat = lerp(C["coat"], C["coat2"], noise3(P, 6, 3) * 0.6)
    if kind == "temperate":   # pied: big white patches, white belly and socks
        if noise3(P, 4.5, 11) > 0.64 or (face == "bottom" and mat in ("chest", "barrel", "hips")):
            coat = lerp(C["white"], (210, 204, 192), noise3(P, 2, 5) * 0.4)
    if kind == "warm":        # fawn underside and legs, darker hump and neck
        t = under(face, p, b, P) if mat in ("chest", "barrel", "hips", "neck") else 0
        coat = lerp(coat, C["white"], smoothstep(0.55, 1.0, t))
        if mat in ("hump", "crest", "withers"):
            coat = lerp(coat, (90, 44, 26), 0.35)
    if kind == "cold":
        coat = strands(coat, p, face, 0.2, 2)
    if mat in ("skirt", "neck_shag", "leg_shag", "mane", "fringe"):
        if mat in ("skirt", "leg_shag", "neck_shag", "fringe") and ragged_bottom(face, p, b, 2.5 if mat == "skirt" else 2, len(mat)):
            return CLEAR
        if mat == "skirt" and face == "top":
            return CLEAR
        if mat == "fringe" and face == "back":
            return CLEAR
        return strands(lerp(coat, C["coat2"], 0.2 * rel(y, b["y"], b["y"] + b["h"])), p, face, 0.24, 2)
    if mat in ("horn", "horn_tip", "horn_bud"):
        if mat == "horn_tip":
            return lerp(C["horn"], C["horn_tip"], rel(-y, 0, b["h"]) * 0.9 + 0.1)
        return lerp(C["horn"], (180, 168, 140), 0.3 * h(int(y * 2), 0, 3)) if mat == "horn" else C["horn"]
    if mat in ("hoof", "dewclaw"):
        if mat == "hoof" and face == "front" and abs(x) < 0.5:
            return (24, 20, 18)   # the cleft between the claws
        return C["hoof"]
    if mat in ("udder", "teat"):
        return TEAT if mat == "teat" else lerp(PINK, coat, 0.25 if face == "top" else 0.0)
    if mat == "switch":
        return C["switch"]
    if mat == "tail":
        return coat
    if mat == "muzzle":
        if face == "front":
            if -2 <= y < -1 and (b["x"] + 0.5 < x < b["x"] + 1.5 or b["x"] + b["w"] - 1.5 < x < b["x"] + b["w"] - 0.5):
                return (40, 28, 26)   # nostrils
            return lerp(C["nose"], C["muzzle"], 0.3 * h(int(x * 3), int(y * 3), 9))
        if face == "top" and z < b["z"] + 1.5:
            return C["muzzle"]
        if y > 0:
            return lerp(C["muzzle"], (90, 70, 66), 0.3)
        return lerp(C["muzzle"], coat, 0.4)
    if mat == "jaw":
        return lerp(C["muzzle"], coat, 0.5) if z < b["z"] + 2 else coat
    if mat in ("poll", "face"):
        if mat == "poll" and eye(face, p, b, -2, b["z"] + (0.5 if not baby else 0), 2):
            return EYE
        if mat == "poll" and face in ("left", "right") and -3 <= y < -2 and z < b["z"] + 2.5:
            return lerp(coat, (20, 14, 12), 0.45)   # the lid line over the eye
        if kind == "temperate" and (face in ("front", "top") or mat == "face" and face == "bottom"):
            if abs(x) < 2.2 or face == "front":
                return lerp(C["white"], (200, 192, 180), 0.2 * h(int(x * 3), int(y * 3), 7))   # the white face
        if kind == "warm" and face in ("left", "right") and mat == "poll":
            return lerp(coat, (70, 34, 20), 0.3)
        return coat
    if mat == "ear":
        if face == "bottom" or face == "front" and kind == "warm":
            return lerp(PINK, coat, 0.35)
        if face in ("left", "right") and abs(x) > abs(b["x"] + (b["w"] if b["x"] >= 0 else 0)) - 0.6:
            return lerp(coat, (30, 22, 18), 0.3)    # ear tip
        return coat
    if mat in ("forearm", "thigh", "gaskin", "cannon"):
        low = rel(P[1], 15, 23)
        c = coat
        if kind == "temperate" and noise3(P, 4, 13) > 0.5 and P[1] > 18:
            c = C["white"]
        if kind == "warm":
            c = lerp(coat, C["white"], 0.5 * low)
        if inner_leg(face, P):
            c = lerp(c, C["coat2"], 0.3)
        return lerp(c, C["coat2"], 0.25 * low)
    if mat in ("chest", "barrel", "hips", "rump", "withers", "hook", "brisket", "neck", "crest", "dewlap", "dewlap_low", "hump"):
        if mat == "hook" and face != "bottom":
            return lerp(coat, (255, 240, 220), 0.06)
        return coat
    return coat


def smoothstep(a, b, t):
    t = max(0.0, min(1.0, (t - a) / (b - a)))
    return t * t * (3 - 2 * t)


# ============================================================================ pig
def pig_model(cold):
    """An old-breed pig; the cold one wears a curly coat that stands 1 px out from the body."""
    m = Model("Cold pig" if cold else "Pig")
    g = 1 if cold else 0
    m.part("body", "root", (0, 13.5, 0))
    m.box("body", "shoulders", -4.5 - g, -4 - g, -8, 9 + 2 * g, 8 + 2 * g, 6)
    m.box("body", "chest", -3.5 - g, -3 - g, -9 - g, 7 + 2 * g, 6 + 2 * g, 1)   # rounds off the front
    m.box("body", "barrel", -5 - g, -4 - g, -2.5, 10 + 2 * g, 8 + 2 * g, 6)
    m.box("body", "ham", -4.5 - g, -4 - g, 3.5, 9 + 2 * g, 8 + 2 * g, 5)
    m.box("body", "rump", -3.5 - g, -3 - g, 8.5, 7 + 2 * g, 6 + 2 * g, 1 + g)  # rounds off the back
    m.box("body", "back", -3.5 - g, -5 - g, -7.5, 7 + 2 * g, 1, 15)            # the arched topline
    m.box("body", "crest", -1, -6 - g, -7.5, 2, 1, 10)                          # the bristly ridge along the spine
    m.box("body", "belly", -4, 4 + g, -5, 8, 1, 10)                             # the sagging belly line with the teats
    m.part("tail", "body", (0, -3, 8.5 + 2 * g), (0.5, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 2, 1)
    m.part("tail_curl", "tail", (0, 2, 0), (-1.6, 0, 0.6), name="curl")
    m.box("tail_curl", "tail", -0.5, 0, -0.5, 1, 2, 1)
    m.part("tail_tip", "tail_curl", (0, 2, 0), (-1.6, 0, 0), name="tip")
    m.box("tail_tip", "tail_tip", -0.5, 0, -0.5, 1, 1, 1)
    m.part("head", "root", (0, 12.5, -7.5))
    m.part("skull", "head", (0, 0, 0), (0.25, 0, 0))
    m.box("skull", "jowl", -4 - g, -3.5 - g, -4, 8 + 2 * g, 8 + 2 * g, 5)
    m.box("skull", "brow", -3, -3.5 - g, -8, 6, 6, 4)
    m.box("skull", "snout", -2, -1.5, -12, 4, 3, 4)
    m.box("skull", "disc", -2, -2, -13, 4, 4, 1)
    m.box("skull", "jaw", -2, 1.5, -11, 4, 2, 7)
    for side, s in ((-1, "right"), (1, "left")):   # lop ears hanging forward over the eyes
        m.part(f"{s}_ear", "skull", (side * 2.2, -3.5 - g, -5), (1.6, side * -0.2, side * 0.5))
        m.box(f"{s}_ear", "ear", -1.5, -4, -0.5, 3, 4, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 2.8, 17, -5.5))
        m.box(f"{s}_front_leg", "forearm", -1.5 - g, -1, -1.5 - g, 3 + 2 * g, 4, 3 + 2 * g)
        m.box(f"{s}_front_leg", "shank", -1, 3, -1, 2, 3, 2)
        m.box(f"{s}_front_leg", "trotter", -1, 6, -1.5, 2, 1, 2)
        m.box(f"{s}_front_leg", "dewclaw", -1, 5, 1, 2, 1, 1)
        m.part(f"{s}_hind_leg", "root", (side * 2.8, 16.5, 6))
        m.box(f"{s}_hind_leg", "thigh", -2 - g, -1.5, -2 - g, 4 + 2 * g, 5, 4 + 2 * g)
        m.box(f"{s}_hind_leg", "shank", -1, 3.5, -1, 2, 3, 2)
        m.box(f"{s}_hind_leg", "trotter", -1, 6.5, -1.5, 2, 1, 2)
        m.box(f"{s}_hind_leg", "dewclaw", -1, 5.5, 1, 2, 1, 1)
    return m


def piglet_model():
    m = Model("Piglet")
    m.part("body", "root", (0, 19, 0))
    m.box("body", "barrel", -2.5, -2.5, -4.5, 5, 5, 9)
    m.box("body", "crest", -1, -3, -4, 2, 1, 7)
    m.part("tail", "body", (0, -2, 4.5), (0.6, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 2, 1)
    m.part("head", "root", (0, 18, -4))
    m.part("skull", "head", (0, 0, 0), (0.15, 0, 0))
    m.box("skull", "jowl", -2.5, -2.5, -4, 5, 5, 4)
    m.box("skull", "snout", -1.5, -1, -6, 3, 2, 2)
    m.box("skull", "disc", -1.5, -1.5, -7, 3, 3, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "skull", (side * 1.6, -2.5, -2.5), (1.3, side * -0.2, side * 0.5))
        m.box(f"{s}_ear", "ear", -1, -2, -0.5, 2, 2, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 1.5, 21, -2.5))
        m.box(f"{s}_front_leg", "shank", -1, -1, -1, 2, 3, 2)
        m.box(f"{s}_front_leg", "trotter", -1, 2, -1.5, 2, 1, 2)
        m.part(f"{s}_hind_leg", "root", (side * 1.5, 21, 3))
        m.box(f"{s}_hind_leg", "thigh", -1, -1, -1, 2, 3, 2)
        m.box(f"{s}_hind_leg", "trotter", -1, 2, -1.5, 2, 1, 2)
    return m


PIG = {
    "temperate": dict(skin=(232, 170, 160), skin2=(214, 146, 140), hair=(246, 220, 206), disc=(214, 124, 126), mud=(110, 86, 64),
                      hoof=(150, 120, 100), spot=(120, 104, 104)),
    "warm": dict(skin=(150, 82, 52), skin2=(118, 60, 38), hair=(186, 112, 70), disc=(70, 46, 42), mud=(84, 62, 44),
                 hoof=(52, 40, 34), spot=(52, 34, 28)),
    "cold": dict(skin=(220, 186, 140), skin2=(190, 150, 104), hair=(240, 214, 170), disc=(120, 80, 74), mud=(96, 76, 56),
                 hoof=(58, 46, 40), spot=(150, 110, 70)),
}


def curls(c, p, face, P, amount=0.22):
    """Curly wool: small round locks with darker hollows between them (model space, so they line up)."""
    k = noise3((P[0] * 1.6, P[1] * 1.6, P[2] * 1.6), 1.2, 41)
    k2 = h(int(P[0] * 2 + P[2] * 3), int(P[1] * 2), 43)
    return tuple(max(0, min(255, round(v * (1 - amount + 2 * amount * (0.7 * k + 0.3 * k2))))) for v in c)


def pig_colour(v, mat, face, p, P, b):
    kind = v.split(":")[0]
    C = PIG[kind]
    x, y, z = p
    base = lerp(C["skin"], C["skin2"], noise3(P, 6, 7) * 0.45)
    if kind == "warm":   # the dark saddle over the back and shoulders
        if noise3(P, 5, 17) * 0.7 + (0.5 if P[1] < 11 and mat in ("shoulders", "barrel", "crest") else 0) > 0.75:
            base = lerp(C["spot"], base, 0.25)
    if kind == "temperate" and noise3(P, 5, 19) > 0.74 and mat in ("barrel", "ham", "shoulders", "back"):
        base = lerp(base, C["spot"], 0.3)    # a grey skin patch here and there
    if kind == "cold":
        base = curls(lerp(C["hair"], C["skin2"], 0.35 * under(face, p, b, P) + 0.25 * noise3(P, 5, 9)), p, face, P, 0.3)
    elif face != "bottom" and h(int(P[0] * 3 + P[2] * 7), int(P[1] * 3), 13) > 0.86:
        base = lerp(base, C["hair"], 0.5)    # sparse bristles
    if mat == "crest":
        return strands(lerp(base, C["skin2"] if kind != "warm" else C["spot"], 0.35), p, face, 0.3, 1)
    if mat == "belly":
        if face == "bottom" and int(z) % 2 == 0 and abs(x) > 1.5:
            return C["disc"]   # teats
        return lerp(base, C["skin2"], 0.3)
    if mat == "disc":
        if face == "front":
            if -1 <= y < 0 and (-1.5 < x < -0.5 or 0.5 < x < 1.5):
                return lerp(C["disc"], (40, 20, 20), 0.6)   # nostrils
            return lerp(C["disc"], (255, 210, 200), 0.15 * h(int(x * 3), int(y * 3), 3))
        return C["disc"]
    if mat == "snout":
        if face in ("left", "right") and y > 0:
            return lerp(C["disc"], base, 0.5)   # the mouth line
        return lerp(base, C["disc"], 0.2)
    if mat == "jaw":
        return lerp(base, C["skin2"], 0.4)
    if mat in ("brow", "jowl"):
        if mat == "brow" and face in ("left", "right") and -2 <= y < -1 and b["z"] + 1 <= z < b["z"] + 2.5:
            return (24, 16, 14)   # small eye
        if mat == "brow" and face in ("left", "right") and -3 <= y < -2 and b["z"] + 0.5 <= z < b["z"] + 3:
            return lerp(base, (60, 40, 36), 0.35)   # brow wrinkle over the eye
        if mat == "jowl" and b["w"] == 5 and face in ("left", "right") and -1.5 <= y < -0.5 and b["z"] <= z < b["z"] + 1:
            return (24, 16, 14)   # the piglet's eye
        return base
    if mat == "ear":
        if face == "back" or face == "bottom":
            return lerp(C["disc"], base, 0.4)   # inside of the flopped-forward ear
        return lerp(base, C["skin2"], 0.2 * rel(-y, 0, 4))
    if mat in ("trotter", "dewclaw"):
        if face == "front" and abs(x) < 0.5:
            return (30, 24, 22)
        return C["hoof"]
    if mat in ("shank", "forearm", "thigh"):
        low = rel(P[1], 18, 23)
        if kind != "cold" and P[1] > 20.5 and noise3(P, 2, 31) > 0.35:
            return lerp(base, C["mud"], 0.7)   # mud on the lower legs
        return lerp(base, C["skin2"], 0.3 * low + (0.15 if inner_leg(face, P) else 0))
    if mat in ("tail", "tail_tip"):
        return lerp(base, C["skin2"], 0.3)
    return lerp(base, C["skin2"], 0.25 * under(face, p, b, P))


# ============================================================================ sheep
def sheep_parts(m, lamb):
    """The parts shared by the sheep's body and its fleece (same names, pivots and rotations, so they move as one)."""
    if lamb:
        m.part("body", "root", (0, 16, 0))
        m.part("tail", "body", (0, -2, 4.5), (0.3, 0, 0))
        m.part("head", "root", (0, 14, -4.5))
        m.part("neck", "head", (0, 0, 0), (-0.5, 0, 0))
        m.part("skull", "neck", (0, -1, -2.5), (0.9, 0, 0))
        for side, s in ((-1, "right"), (1, "left")):
            m.part(f"{s}_front_leg", "root", (side * 1.5, 18, -3))
            m.part(f"{s}_hind_leg", "root", (side * 1.5, 17, 3.5))
    else:
        m.part("body", "root", (0, 11, 0))
        m.part("tail", "body", (0, -3.5, 9), (0.25, 0, 0))
        m.part("head", "root", (0, 8, -7.5))
        m.part("neck", "head", (0, 0, 0), (-0.6, 0, 0))
        m.part("skull", "neck", (0, -1, -4.5), (1.0, 0, 0))
        for side, s in ((-1, "right"), (1, "left")):
            m.part(f"{s}_front_leg", "root", (side * 2, 13, -5.5))
            m.part(f"{s}_hind_leg", "root", (side * 2, 12, 6.5))


def sheep_model(lamb):
    """The bare sheep: what a shorn sheep shows, and the head and legs below the fleece."""
    m = Model("Lamb" if lamb else "Sheep")
    sheep_parts(m, lamb)
    if lamb:
        m.box("body", "barrel", -2.5, -2.5, -4.5, 5, 5, 9)
        m.box("tail", "tail", -0.5, 0, -0.5, 1, 3, 1)
        m.box("neck", "neck", -1.5, -2, -3, 3, 4, 4)
        m.box("skull", "cranium", -2, -2.5, -2.5, 4, 4, 3)
        m.box("skull", "face", -1.5, -2, -5, 3, 3, 3)
        for side, s in ((-1, "right"), (1, "left")):
            m.part(f"{s}_ear", "skull", (side * 2, -1, -0.5), (0, 0, side * 0.5))
            m.box(f"{s}_ear", "ear", 0 if side > 0 else -2, -0.5, -0.5, 2, 1, 1)
            m.box(f"{s}_front_leg", "forearm", -1, -1, -1, 2, 3, 2)
            m.box(f"{s}_front_leg", "cannon", -1, 2, -1, 2, 3, 2)
            m.box(f"{s}_front_leg", "hoof", -1, 5, -1, 2, 1, 2)
            m.box(f"{s}_hind_leg", "thigh", -1, -1, -1.5, 2, 4, 3)
            m.box(f"{s}_hind_leg", "cannon", -1, 3, -1, 2, 3, 2)
            m.box(f"{s}_hind_leg", "hoof", -1, 6, -1, 2, 1, 2)
        return m
    m.box("body", "chest", -3.5, -4, -8, 7, 7, 6)
    m.box("body", "barrel", -4, -4, -2.5, 8, 8, 8)
    m.box("body", "hips", -3.5, -4.5, 5, 7, 7, 4)
    m.box("tail", "tail", -1, 0, -0.5, 2, 5, 1)
    m.box("neck", "neck", -2, -2.5, -5, 4, 5, 6)
    m.box("skull", "cranium", -2.5, -3, -3, 5, 5, 4)
    m.box("skull", "face", -2, -2.5, -7, 4, 4, 4)
    m.box("skull", "nose", -1.5, -2, -8, 3, 3, 1)
    m.box("skull", "jaw", -1.5, 1.5, -6.5, 3, 1, 4)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "skull", (side * 2.5, -1.5, -0.5), (0, 0, side * 0.5))
        m.box(f"{s}_ear", "ear", 0 if side > 0 else -3, -0.5, -1, 3, 1, 2)
        m.box(f"{s}_front_leg", "forearm", -1, -1, -1.5, 2, 5, 3)
        m.box(f"{s}_front_leg", "cannon", -1, 4, -1, 2, 6, 2)
        m.box(f"{s}_front_leg", "hoof", -1, 10, -1.5, 2, 1, 2)
        m.box(f"{s}_hind_leg", "thigh", -1.5, -1, -2, 3, 6, 4)
        m.box(f"{s}_hind_leg", "cannon", -1, 5, -0.5, 2, 6, 2)
        m.box(f"{s}_hind_leg", "hoof", -1, 11, -1, 2, 1, 2)
    return m


def fleece_model(lamb):
    """The fleece: a deep rounded coat over the body, a ruff on the neck, a topknot, and wool down the upper legs."""
    m = Model("Lamb fleece" if lamb else "Fleece")
    sheep_parts(m, lamb)
    if lamb:
        m.box("body", "fleece", -3.5, -3.5, -5.5, 7, 7, 11)
        m.box("body", "fleece_top", -2.5, -4.5, -4.5, 5, 1, 9)
        m.box("neck", "ruff", -2, -2.5, -3, 4, 5, 4)
        m.box("skull", "topknot", -2, -3.5, -1.5, 4, 1, 3)
        for side, s in ((-1, "right"), (1, "left")):
            m.box(f"{s}_front_leg", "leg_wool", -1.5, -1, -1.5, 3, 3, 3)
            m.box(f"{s}_hind_leg", "leg_wool", -1.5, -1, -2, 3, 4, 4)
        return m
    m.box("body", "fleece", -5.5, -6, -9.5, 11, 10, 20)
    m.box("body", "fleece_top", -4.5, -7, -8.5, 9, 1, 18)
    m.box("body", "fleece_low", -4.5, 4, -8.5, 9, 1, 18)
    m.box("body", "fleece_front", -4.5, -5, -10.5, 9, 8, 1)
    m.box("body", "fleece_back", -4.5, -5, 10.5, 9, 8, 1)
    m.box("tail", "tail_wool", -1.5, -0.5, -1, 3, 4, 2)
    m.box("neck", "ruff", -2.5, -3.5, -5, 5, 6, 6)
    m.box("skull", "topknot", -2.5, -4, -2.5, 5, 2, 4)
    for side, s in ((-1, "right"), (1, "left")):
        m.box(f"{s}_front_leg", "leg_wool", -1.5, -1, -2, 3, 4, 4)
        m.box(f"{s}_hind_leg", "leg_wool", -2, -1, -2.5, 4, 5, 5)
    return m


SH = dict(skin=(214, 196, 176), stubble=(232, 224, 206), face=(222, 206, 182), face2=(196, 176, 150), nose=(150, 112, 104),
          hoof=(64, 54, 46), inner_ear=(220, 168, 156), wool=(255, 255, 252), wool_dark=(228, 226, 218))


def sheep_colour(v, mat, face, p, P, b):
    x, y, z = p
    if mat.startswith(("fleece", "ruff", "topknot", "leg_wool", "tail_wool")):
        # grey-white locks: round tufts with shadowed partings (kept pale so the dye tint reads true)
        k = noise3((P[0] * 1.3, P[1] * 1.3, P[2] * 1.3), 1.6, 61)
        k2 = h(int(P[0] * 2 + 37 * P[2]), int(P[1] * 2), 67)
        c = lerp(SH["wool"], SH["wool_dark"], smoothstep(0.5, 0.85, k) * 0.85 + 0.15 * k2)
        if face == "bottom" or (face in ("left", "right", "front", "back") and y > b["y"] + b["h"] - 1):
            c = lerp(c, SH["wool_dark"], 0.35)   # the dirtier underside and tips
        return c
    if mat in ("chest", "barrel", "hips", "neck", "tail"):
        # shorn: a short creamy stubble with the pink-grey skin showing through
        return lerp(SH["stubble"], SH["skin"], 0.35 + 0.4 * h(int(P[0] * 3 + P[2] * 5), int(P[1] * 3), 71) + 0.2 * under(face, p, b, P))
    if mat in ("cranium", "face", "nose", "jaw"):
        c = lerp(SH["face"], SH["face2"], noise3(P, 2.5, 73) * 0.6)
        if mat == "cranium" and eye(face, p, b, -1.5, b["z"] + 0.5, 1):
            return (40, 30, 22)
        if mat == "cranium" and face in ("left", "right") and -1.5 <= y < -0.5 and b["z"] + 1.5 <= z < b["z"] + 2.5:
            return (164, 132, 70)   # the amber eye with its slot pupil beside
        if mat == "nose" and face == "front":
            if -1 <= y < 0 and abs(x) > 0.5:
                return (70, 46, 42)   # nostrils
            return SH["nose"]
        if mat == "face" and face == "front":
            return lerp(c, SH["nose"], 0.3)
        return c
    if mat == "ear":
        return SH["inner_ear"] if face in ("bottom", "front") else lerp(SH["face"], SH["face2"], 0.4)
    if mat == "hoof":
        if face == "front" and abs(x) < 0.5:
            return (34, 28, 24)
        return SH["hoof"]
    if mat in ("forearm", "cannon", "thigh"):
        c = lerp(SH["face"], SH["face2"], 0.3 + 0.3 * rel(P[1], 14, 23))
        if mat == "cannon" and face == "front" and y > b["y"] + b["h"] - 1.5:
            return lerp(c, (90, 74, 60), 0.5)   # the dark knee/pastern
        return c
    return SH["skin"]


# ============================================================================ chicken
def chicken_model(cold):
    """A landrace hen; the cold one is fluffier, with feathered legs and a muffed face."""
    m = Model("Cold chicken" if cold else "Chicken")
    g = 1 if cold else 0
    m.part("body", "root", (0, 17, 0))
    m.box("body", "body", -2.5 - g, -3 - g, -3.5, 5 + 2 * g, 5 + 2 * g, 7)
    m.box("body", "breast", -2 - g, -2 - g, -4.5, 4 + 2 * g, 4 + 2 * g, 1)
    m.box("body", "saddle", -2, -4 - g, 0, 4, 1, 4)
    m.box("body", "fluff", -2 - g, 2 + g, -2, 4 + 2 * g, 1, 5)
    m.part("tail", "body", (0, -3 - g, 3), (-0.6, 0, 0))
    m.box("tail", "tail", -1, -4, -1, 2, 4, 3)
    m.box("tail", "tail_tip", -0.5, -5, 0, 1, 1, 2)
    m.part("head", "root", (0, 14.5 - g, -3))
    m.box("head", "neck", -1.5 - g, -3, -1.5 - g, 3 + 2 * g, 4, 3 + g)
    m.box("head", "head", -1.5, -5.5, -2.5, 3, 3, 3)
    m.box("head", "beak", -0.5, -4.5, -4, 1, 1, 2)
    m.box("head", "comb", -0.5, -7, -2.5, 1, 2, 3)
    m.box("head", "wattle", -0.5, -3, -2.5, 1, 2, 1)
    if cold:
        m.box("head", "muff", -2, -3.5, -2.5, 4, 1, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "root", (side * (2.5 + g), 14.5 - g, -1))
        x0 = -1 if side < 0 else 0
        m.box(f"{s}_wing", "wing", x0, 0, -2.5, 1, 4, 6)
        m.box(f"{s}_wing", "primaries", x0, 2, 3.5, 1, 2, 2)
        m.part(f"{s}_leg", "root", (side * 1, 19, 0.5))
        m.box(f"{s}_leg", "thigh", -1, -1, -1, 2, 2, 2)
        if cold:
            m.box(f"{s}_leg", "shank_feathered", -1, 1, -1, 2, 3, 2)
        else:
            m.box(f"{s}_leg", "shank", -0.5, 1, -0.5, 1, 3, 1)
        m.box(f"{s}_leg", "toes", -1.5, 4, -2.5, 3, 1, 3)
        m.box(f"{s}_leg", "hind_toe", -0.5, 4, 0.5, 1, 1, 1)
    return m


def chick_model():
    m = Model("Chick")
    m.part("body", "root", (0, 20.5, 0))
    m.box("body", "down", -2, -2.5, -2, 4, 4, 4)
    m.part("head", "root", (0, 18.5, -1.5))
    m.box("head", "chick_head", -1.5, -3, -2, 3, 3, 3)
    m.box("head", "beak", -0.5, -1.5, -3, 1, 1, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "root", (side * 2, 19, 0))
        m.box(f"{s}_wing", "stub", -1 if side < 0 else 0, 0, -1, 1, 2, 2)
        m.part(f"{s}_leg", "root", (side * 0.8, 22, 0.5))
        m.box(f"{s}_leg", "shank", -0.5, 0, -0.5, 1, 1, 1)
        m.box(f"{s}_leg", "toes", -1, 1, -1.5, 2, 1, 2)
    return m


HEN = {
    "temperate": dict(base=(242, 240, 234), edge=(212, 210, 202), dark=(186, 184, 176), breast=(246, 244, 238), tail=(230, 228, 222)),
    "warm": dict(base=(178, 108, 52), edge=(70, 44, 26), dark=(54, 34, 22), breast=(206, 138, 84), tail=(44, 32, 26), hackle=(222, 160, 78)),
    "cold": dict(base=(170, 172, 182), edge=(92, 94, 106), dark=(70, 72, 84), breast=(186, 188, 196), tail=(84, 86, 98)),
}
COMB, BEAK, LEG, SKIN = (204, 44, 40), (222, 186, 96), (226, 184, 76), (242, 204, 172)


def chicken_colour(v, mat, face, p, P, b):
    plucked, v = v.startswith("plucked:"), v.replace("plucked:", "")
    kind, baby = v.split(":")[0], v.endswith(":baby")
    C = HEN[kind]
    x, y, z = p
    if mat in ("beak",):
        return BEAK if face != "bottom" else lerp(BEAK, (150, 120, 60), 0.4)
    if mat in ("comb", "wattle"):
        return lerp(COMB, (240, 90, 80), 0.25 * h(int(z * 3), int(y * 3), 81))
    if mat in ("shank", "toes", "hind_toe"):
        if mat == "toes":   # three toes forward, the gaps between them open
            if face == "top" and not (abs(x) < 0.5 or (abs(x) > 0.9 and z < b["z"] + 2)) and z < b["z"] + 2:
                return CLEAR
            if face in ("front",) and abs(x) > 0.5 and abs(x) < 1.0:
                return CLEAR
        c = LEG if not baby else lerp(LEG, (250, 210, 120), 0.4)
        return lerp(c, (170, 130, 50), 0.35) if int(y * 2) % 2 == 0 and mat == "shank" else c
    if plucked:   # bare skin with follicles; the eye stays
        if mat == "head" and eye(face, p, b, -4.5, b["z"] + 0.5, 1):
            return (30, 18, 12)
        return lerp(SKIN, (214, 168, 140), 0.5 if h(int(P[0] * 3 + P[2] * 5), int(P[1] * 3), 83) > 0.8 else 0.1 * under(face, p, b, P))
    if baby:
        down = {"temperate": (250, 224, 112), "warm": (206, 160, 100), "cold": (176, 176, 168)}[kind]
        c = lerp(down, (255, 246, 200), 0.25 * h(int(P[0] * 3 + P[2] * 7), int(P[1] * 3), 85))
        if kind == "warm" and face == "top" and (abs(x) < 0.5 or abs(abs(x) - 1.5) < 0.5) and mat == "down":
            c = (110, 70, 40)   # the chipmunk stripes down the back
        if mat == "chick_head" and eye(face, p, b, -2, b["z"] + 0.5, 1):
            return (24, 16, 12)
        return lerp(c, (200, 170, 90) if kind == "temperate" else c, 0.25 * under(face, p, b, P))
    base = C["base"]
    if kind == "cold":   # barred: light and dark bands across every feather
        base = C["base"] if int(P[1] * 1.5 + P[2] * 0.7) % 2 == 0 else lerp(C["base"], C["dark"], 0.55)
    if kind == "warm":   # partridge: fine dark pencilling on a red-gold ground
        if h(int(P[0] * 3 + P[2] * 4), int(P[1] * 3), 87) > 0.62:
            base = lerp(base, C["edge"], 0.55)
    scallop = h(int(P[0] * 2 + P[2] * 2), int(P[1] * 2), 89) > 0.7
    if mat == "head":
        if eye(face, p, b, -4.5, b["z"] + 0.5, 1):
            return (190, 110, 40)   # orange iris
        if face in ("left", "right") and -3.5 <= y < -2.5 and b["z"] + 0.5 <= z < b["z"] + 1.5:
            return (238, 226, 210) if kind != "warm" else COMB   # the earlobe
        if face in ("left", "right") and y > -4.5 and z < b["z"] + 1.5:
            return lerp(COMB, base, 0.4)   # bare red face round the eye
        return base if kind != "warm" else C["hackle"]
    if mat in ("neck", "muff"):
        c = C.get("hackle", base)
        return strands(c, p, face, 0.18, 1) if kind != "cold" else lerp(base, C["base"], 0.3)
    if mat in ("tail", "tail_tip"):
        c = C["tail"]
        return lerp(c, C["dark"], 0.3) if mat == "tail_tip" else strands(c, p, face, 0.15, 1)
    if mat in ("wing", "primaries"):
        c = base
        if mat == "primaries" or y > b["y"] + b["h"] - 1:
            c = lerp(c, C["dark"], 0.45)   # the darker flight feathers
        elif int(z) % 2 == 0 and y > b["y"] + 1:
            c = lerp(c, C["edge"], 0.25)   # rows of coverts
        return c
    if mat in ("breast",):
        return C["breast"]
    if mat in ("thigh", "shank_feathered", "fluff"):
        return lerp(base, C["edge"], 0.2)
    c = lerp(base, C["edge"], 0.3) if scallop else base
    return lerp(c, C["breast"], 0.3 * under(face, p, b, P))


# ============================================================================ main
MODELS = []


def register(model, method, colour, skins, keep, skinned=True):
    MODELS.append((model, method, colour, skins, keep, skinned))


def build():
    for kind, method in (("temperate", "cow"), ("warm", "warmCow"), ("cold", "coldCow")):
        register(cow_model(kind), method, cow_colour, {f"cow/cow_{kind}": kind + ":adult"}, ("horn", "horn_tip", "hoof", "dewclaw"))
    register(pig_model(False), "pig", pig_colour, {"pig/pig_temperate": "temperate:adult", "pig/pig_warm": "warm:adult"}, ("trotter", "dewclaw"))
    register(pig_model(True), "coldPig", pig_colour, {"pig/pig_cold": "cold:adult"}, ("trotter", "dewclaw"))
    register(piglet_model(), "piglet", pig_colour, {f"pig/pig_{k}_baby": k + ":baby" for k in ("temperate", "warm", "cold")}, ("trotter",))
    register(sheep_model(False), "sheep", sheep_colour, {"sheep/sheep": "adult"}, (), skinned=False)
    register(fleece_model(False), "fleece", sheep_colour, {"sheep/sheep_wool": "adult"}, (), skinned=False)
    register(sheep_model(True), "lamb", sheep_colour, {"sheep/sheep_baby": "baby"}, (), skinned=False)
    register(fleece_model(True), "lambFleece", sheep_colour, {"sheep/sheep_wool_baby": "baby"}, (), skinned=False)
    register(chicken_model(False), "chicken", chicken_colour, {"chicken/chicken_temperate": "temperate:adult", "chicken/chicken_warm": "warm:adult"},
             (), skinned="plucked:")
    register(chicken_model(True), "coldChicken", chicken_colour, {"chicken/chicken_cold": "cold:adult"}, (), skinned="plucked:")
    register(chick_model(), "chick", chicken_colour, {f"chicken/chicken_{k}_baby": k + ":baby" for k in ("temperate", "warm", "cold")},
             (), skinned="plucked:")
    register(calf_model(), "calf", cow_colour, {f"cow/cow_{k}_baby": k + ":baby" for k in ("temperate", "warm", "cold")}, ("hoof", "horn_bud"))


def main():
    build()
    methods = []
    for m, method, colour, skins, keep, skin in MODELS:
        m.pack()
        methods.append(m.java(method))
        for name, variant in skins.items():
            img = m.paint(colour, variant)
            save(img, f"vanilla/{name}.png")
            if skin is True:
                save(m.skinned(img, keep), f"skinned/vanilla/{name}.png")
            elif skin:
                save(m.paint(colour, skin + variant), f"skinned/vanilla/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.parent.mkdir(parents=True, exist_ok=True)
    JAVA.write_text(f"""package com.mechanical.client.vanilla;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_vanilla_models.py (with the skins it paints): the vanilla farm animals' parts and boxes. Do not edit. */
public final class VanillaGeometry {{
{chr(10).join(methods)}
    private VanillaGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
