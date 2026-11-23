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
Second pass (client/vanilla/WildVanillaRenderers): rabbit, fox, wolf, cat/ocelot (one feline model) and camel keep the
vanilla skeleton exactly (names, pivots, rotations) so the vanilla model classes and keyframe animations drive them; a
part vanilla lays rotated by pi/2 gets an upright "shape" child for its boxes. Skins for every vanilla adult texture
(wolf wild/tame/angry per variant, sleeping fox, all cat variants) plus collar overlays; babies stay vanilla.

Same idea as gen_savanna_models.py (its Model class): every box is packed into the skin and painted from the 3D point
each texel covers. Part names are the ones the vanilla models look up (head, body, legs; wings for the chicken), so
vanilla animation, the livestock poses and the carcass poses keep working.
Usage: python scripts/gen_vanilla_models.py. Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import CLEAR, Model, rel, rounded  # noqa: E402
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
    # deep shoulders over a narrower brisket, a wide sagging barrel, a rounded ham tucked in below, an arched loin
    m.box("body", "shoulders", -4.5 - g, -4.5 - g, -8, 9 + 2 * g, 8 + 2 * g, 5)
    m.box("body", "brisket", -3.5, 3.5 + g, -7.5, 7, 1, 4)
    m.box("body", "chest", -3.5 - g, -3.5 - g, -9 - g, 7 + 2 * g, 6 + 2 * g, 1)
    m.box("body", "barrel", -5 - g, -3.5 - g, -3, 10 + 2 * g, 8 + 2 * g, 7)
    m.box("body", "belly", -4, 4.5 + g, -3.5, 8, 1, 8)
    m.box("body", "ham", -4.5 - g, -4 - g, 4, 9 + 2 * g, 7 + 2 * g, 4)
    m.box("body", "ham_low", -3.5, 3 + g, 4.5, 7, 1, 3)
    m.box("body", "rump", -3.5 - g, -3.5 - g, 8, 7 + 2 * g, 6 + 2 * g, 1 + g)
    m.box("body", "back", -3.5 - g, -5 - g, -7, 7 + 2 * g, 1, 14)
    m.box("body", "loin", -2.5, -5.5 - g, -4, 5, 1, 8)
    m.box("body", "crest", -1, -6 - g, -7.5, 2, 1, 7)                           # the bristly ridge over the shoulders
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


# ============================================================================ second pass: shared
# These keep the vanilla skeleton exactly (part names, pivots and rotations), so the vanilla model classes (and their
# keyframe animations and poses) drive them unchanged. A vanilla body part lies rotated by pi/2; a "shape" child turned
# back by -pi/2 holds the boxes upright, so they are laid out (and painted) in plain model space.
HALF_PI = math.pi / 2


def upright(m, part):
    m.part(part + "_shape", part, (0, 0, 0), (-HALF_PI, 0, 0), name="shape")
    return part + "_shape"


def pointed(face, p, b, tip_w=1):
    """A pointed ear: the top corners of the front/back faces cut away (the thin side faces keep a narrow tip)."""
    if face in ("front", "back"):
        k = (p[1] - b["y"]) / b["h"]   # 0 at the top
        half = b["w"] / 2
        return abs(p[0] - (b["x"] + half)) > tip_w / 2 + k * (half - tip_w / 2) + 0.01
    if face == "top":
        return abs(p[0] - (b["x"] + b["w"] / 2)) > tip_w / 2
    return False


def ticked(c, P, salt, amount=0.18):
    """Agouti fur: dark-tipped and pale-banded hairs, a fine salt-and-pepper over the colour."""
    k = h(int(P[0] * 3 + P[2] * 5), int(P[1] * 3), salt)
    return tuple(max(0, min(255, round(v * (1 - amount + 2 * amount * k)))) for v in c)


# ============================================================================ rabbit
def rabbit_model():
    m = Model("Rabbit")
    t = 0.3927
    m.part("body", "root", (0, 23, 4), (-t, 0, 0))
    m.box("body", "rump", -3.5, -6, -4.5, 7, 6, 5)
    m.box("body", "chest", -3, -5.5, -9, 6, 5, 5)
    m.box("body", "back", -2.5, -6.8, -8, 5, 1, 7)
    m.box("body", "rump_round", -2.5, -5, 0.5, 5, 4, 1)
    m.box("body", "thigh", 3.2, -4.5, -4, 1, 4, 4)
    m.box("body", "thigh", -4.2, -4.5, -4, 1, 4, 4)
    m.part("tail", "body", (0, -4.9916, 0.0125))
    m.box("tail", "scut", -1.5, -2.5, -0.5, 3, 3, 2)
    m.part("head", "body", (0, -5.2929, -8.1213), (t, 0, 0))
    m.box("head", "skull", -2, -2.5, -3.5, 4, 4, 4)
    m.box("head", "cheeks", -2.5, -0.5, -3, 5, 2, 3)
    m.box("head", "muzzle", -1.5, -1, -5, 3, 3, 2)
    for side, s in ((1, "left"), (-1, "right")):
        m.part(f"{s}_ear", "head", (side * 1.5, -3.7071, -0.8787), (-0.2, 0, side * 0.12))
        m.box(f"{s}_ear", "ear", -1, -5.5, -0.5, 2, 6, 1)
    m.part("frontlegs", "body", (0, -1.5349, -6.3108))
    m.part("right_front_leg", "frontlegs", (-2, 1.9239, 0.3827), (t, 0, 0))
    m.box("right_front_leg", "foreleg", -0.9, -1, -0.9, 2, 3, 2)
    m.box("right_front_leg", "forepaw", -0.9, 2, -1.6, 2, 1, 2)
    m.part("left_front_leg", "frontlegs", (2, 1.9239, 0.4827), (t, 0, 0))
    m.box("left_front_leg", "foreleg", -1, -1, -1, 2, 3, 2)
    m.box("left_front_leg", "forepaw", -1, 2, -1.7, 2, 1, 2)
    m.part("backlegs", "root", (0, 23, 4))
    for side, s, yaw in ((-1, "right", t), (1, "left", -t)):
        m.part(f"{s}_hind_leg", "backlegs", (side * 3, 0.5, 0))
        m.part(f"{s}_haunch", f"{s}_hind_leg", (0, -0.5, 0), (0, yaw, 0))
        m.box(f"{s}_haunch", "foot", -1, 0, -5, 2, 1, 6)
        m.box(f"{s}_haunch", "heel", -1, -1.5, -0.5, 2, 2, 2)
    return m


RAB = {
    "brown": ((146, 110, 78), (226, 212, 188), None),
    "white": ((240, 238, 232), (246, 244, 240), None),
    "black": ((44, 40, 40), (70, 64, 62), None),
    "white_splotched": ((240, 238, 232), (246, 244, 240), (40, 36, 36)),
    "gold": ((214, 168, 96), (240, 222, 180), None),
    "salt": ((168, 156, 140), (214, 206, 194), (70, 62, 54)),
    "toast": ((42, 38, 38), (244, 242, 236), None),
    "caerbannog": ((242, 240, 234), (248, 246, 242), None),
}


def rabbit_colour(v, mat, face, p, P, b):
    coat, belly, mark = RAB[v]
    x, y, z = p
    red_eyed = v in ("white", "caerbannog")
    c = ticked(coat, P, 3, 0.12 if v in ("brown", "salt", "gold") else 0.06)
    if v == "brown":
        c = lerp(c, (90, 66, 46), 0.35 * (1 - under(face, p, b, P))) if mat in ("back", "rump", "chest") else c
    if mark and v == "white_splotched" and noise3(P, 2.8, 5) > 0.6:
        c = mark
    if v == "salt" and h(int(P[0] * 3 + P[2] * 7), int(P[1] * 3), 9) > 0.75:
        c = mark
    if v == "toast":   # Dutch: white blaze, collar and front, dark rear and cheeks
        white = (mat in ("chest", "foreleg", "forepaw") and (P[2] < -2.5 or face in ("front", "bottom"))) \
            or (mat in ("skull", "muzzle") and abs(x) < 0.6) or mat in ("foot", "heel")
        c = belly if white else coat
    if mat in ("rump", "chest", "back", "rump_round", "thigh") and face == "bottom" or mat == "chest" and face == "front" and y > -2:
        c = lerp(c, belly, 0.75)
    if mat == "scut":
        return belly if face in ("back", "bottom") else lerp(c, belly, 0.4)
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        if face == "front":
            return lerp((226, 170, 160), c, 0.35) if b["x"] + 0.4 < x < b["x"] + b["w"] - 0.4 else c   # pink inner ear
        return lerp(c, (40, 30, 26), 0.35) if y < b["y"] + 1 and v == "brown" else c
    if mat == "skull" and face in ("left", "right") and -1.5 <= y < -0.5 and b["z"] + 0.5 <= z < b["z"] + 1.5:
        return (176, 30, 36) if red_eyed else (24, 16, 12)
    if mat == "skull" and face in ("left", "right") and -2 <= y < 0 and b["z"] <= z < b["z"] + 2 and v == "brown":
        return lerp(belly, c, 0.3)   # the pale eye ring
    if mat == "muzzle":
        if face == "front" and -1 <= y < 0 and abs(x) < 0.6:
            return (200, 140, 140)   # nose
        if face == "front" and y >= 1:
            return lerp(belly, (180, 150, 140), 0.3)
        return lerp(c, belly, 0.5)
    if mat == "cheeks":
        return lerp(c, belly, 0.3)
    if mat in ("foot", "forepaw"):
        return lerp(c, belly, 0.6 if face == "bottom" else 0.1)
    return c


# ============================================================================ fox
def fox_model():
    m = Model("Fox")
    m.part("head", "root", (-1, 16.5, -3))
    m.box("head", "skull", -2, -2.5, -4, 6, 5, 5)
    m.box("head", "cheeks", -2.5, 0.5, -3, 7, 3, 3)
    m.box("head", "muzzle", -0.5, 1, -8, 3, 2, 4)
    m.box("head", "jaw", -0.5, 3, -7, 3, 1, 3)
    m.part("right_ear", "head", (0, 0, 0))
    m.box("right_ear", "ear", -2, -5.5, -2.5, 3, 3, 1)
    m.part("left_ear", "head", (0, 0, 0))
    m.box("left_ear", "ear", 1, -5.5, -2.5, 3, 3, 1)
    m.part("body", "root", (0, 16, -6), (HALF_PI, 0, 0))
    sh = upright(m, "body")
    m.box(sh, "throat", -2, -2, 2, 4, 5, 2)
    m.box(sh, "chest", -2.5, -2.5, 3.5, 5, 5, 4)
    m.box(sh, "barrel", -3, -2.5, 7, 6, 5, 4)
    m.box(sh, "hips", -2.5, -2.5, 11, 5, 5, 4)
    m.part("tail", "body", (-4, 15, -1), (-0.05236, 0, 0))
    m.box("tail", "tail_base", 2.5, 0, -0.5, 3, 3, 3)
    m.box("tail", "brush", 2, 3, -1, 4, 5, 4)
    m.box("tail", "tail_tip", 2.5, 8, -0.5, 3, 2, 3)
    for s, x in (("right", -5), ("left", -1)):
        m.part(f"{s}_hind_leg", "root", (x, 17.5, 7))
        m.box(f"{s}_hind_leg", "thigh", 1.5, 0, -1.5, 3, 3, 3)
        m.box(f"{s}_hind_leg", "shin", 2, 3, -1, 2, 3, 2)
        m.box(f"{s}_hind_leg", "paw", 2, 5.5, -1.5, 2, 1, 2)
        m.part(f"{s}_front_leg", "root", (x, 17.5, 0))
        m.box(f"{s}_front_leg", "foreleg", 2, 0.5, -1, 2, 5, 2)
        m.box(f"{s}_front_leg", "paw", 2, 5.5, -1.5, 2, 1, 2)
    return m


FOX = {"red": dict(back=(200, 98, 40), side=(222, 128, 58), white=(242, 238, 228), leg=(46, 32, 28), tail=(188, 92, 40)),
       "snow": dict(back=(232, 232, 228), side=(240, 240, 236), white=(250, 250, 248), leg=(206, 206, 204), tail=(236, 236, 232))}


def fox_colour(v, mat, face, p, P, b):
    kind, sleep = v.split(":")[0], v.endswith(":sleep")
    C = FOX[kind]
    x, y, z = p
    coat = ticked(lerp(C["back"], C["side"], under(face, p, b, P)), P, 7, 0.1)
    if mat in ("throat", "chest", "barrel", "hips"):
        if face == "bottom" or mat == "throat" or (mat == "chest" and face == "front"):
            return ticked(C["white"], P, 9, 0.04)
        return coat
    if mat in ("tail_base", "brush"):
        return strands(lerp(C["tail"], (70, 40, 26) if kind == "red" else C["tail"], 0.25 * h(int(x * 3), int(z * 3), 11)), p, face, 0.15, 1)
    if mat == "tail_tip":
        return C["white"]
    if mat == "ear":
        if pointed(face, p, b):
            return CLEAR
        if face == "front":
            return (232, 206, 188) if abs(x - (b["x"] + 1.5)) < 0.9 and y > b["y"] + 1.2 else C["leg"] if kind == "red" else C["back"]
        return C["leg"] if kind == "red" else lerp(C["back"], (180, 180, 178), 0.3)   # black-backed ears
    if mat in ("thigh",):
        return coat if face != "front" or kind != "red" else lerp(coat, C["leg"], 0.4)
    if mat in ("shin", "foreleg", "paw"):
        return C["leg"] if kind == "red" else lerp(C["leg"], C["white"], 0.4)
    if mat == "jaw":
        return C["white"]
    if mat == "muzzle":
        if face == "front" and y < b["y"] + 1 and abs(x - 1) < 0.6:
            return (26, 20, 20)   # nose
        if face == "front" or face in ("left", "right") and y >= b["y"] + 1:
            return C["white"] if y >= b["y"] + 1 else coat
        return coat
    if mat == "cheeks":
        return C["white"] if y >= 2 else coat
    if mat == "skull":
        if face in ("left", "right") and -1 <= y < 0 and b["z"] <= z < b["z"] + 1.5:
            return lerp(coat, (40, 26, 20), 0.6) if sleep else (40, 26, 18) if z < b["z"] + 0.7 else (214, 150, 40)   # amber eye
        if face == "front" and -1 <= y < 0 and (abs(x - (-0.5)) < 0.6 or abs(x - 2.5) < 0.6):
            return lerp(coat, (40, 26, 20), 0.7) if sleep else (214, 150, 40)
        if face == "front" and -1 <= y < 0.5 and (abs(x + 1.5) < 0.6 or abs(x - 3.5) < 0.6):
            return (40, 26, 20)   # the dark tear line
        return coat
    return coat


# ============================================================================ wolf
def wolf_model():
    m = Model("Wolf")
    m.part("head", "root", (-1, 13.5, -7))
    m.part("real_head", "head", (0, 0, 0))
    m.box("real_head", "skull", -1.5, -3, -2.5, 5, 5, 4)
    m.box("real_head", "ruff", -2, -1.5, -1, 6, 5, 3)
    m.box("real_head", "muzzle", -0.5, -0.5, -6.5, 3, 3, 4)
    m.box("real_head", "jaw", -0.5, 2, -6, 3, 1, 3)
    m.box("real_head", "ear", -1.5, -5.5, -0.5, 2, 3, 1)
    m.box("real_head", "ear", 1.5, -5.5, -0.5, 2, 3, 1)
    m.part("upper_body", "root", (-1, 14, -3), (HALF_PI, 0, 0))
    ub = upright(m, "upper_body")
    m.box(ub, "chest", -3, -4.5, -3.5, 8, 7, 6)
    m.box(ub, "hackles", -2, -5.5, -3, 6, 1, 5)
    m.box(ub, "neck", -2, -5, -5.5, 6, 6, 3)
    m.part("body", "root", (0, 14, 2), (HALF_PI, 0, 0))
    bd = upright(m, "body")
    m.box(bd, "barrel", -2.5, -3, -4.5, 5, 5, 9)
    m.box(bd, "hips", -3, -3, 3.5, 6, 5, 4)
    for s, x in (("right", -2.5), ("left", 0.5)):
        m.part(f"{s}_hind_leg", "root", (x, 16, 7))
        m.box(f"{s}_hind_leg", "thigh", -0.5, -1.5, -1.5, 3, 4, 3)
        m.box(f"{s}_hind_leg", "shin", 0, 2.5, -0.5, 2, 5, 2)
        m.box(f"{s}_hind_leg", "paw", 0, 7, -1.5, 2, 1, 2)
        m.part(f"{s}_front_leg", "root", (x, 16, -4))
        m.box(f"{s}_front_leg", "foreleg", 0, 0, -1, 2, 7, 2)
        m.box(f"{s}_front_leg", "paw", 0, 7, -1.5, 2, 1, 2)
    m.part("tail", "root", (-1, 12, 8), (math.pi / 5, 0, 0))
    m.part("real_tail", "tail", (0, 0, 0))
    m.box("real_tail", "tail_base", 0, 0, -1, 2, 3, 2)
    m.box("real_tail", "brush", -0.5, 3, -1.5, 3, 4, 3)
    m.box("real_tail", "tail_tip", 0, 7, -1, 2, 2, 2)
    return m


WOLF = {   # back, saddle, belly
    "pale": ((196, 192, 186), (140, 136, 130), (234, 230, 222)),
    "woods": ((128, 102, 76), (74, 60, 46), (200, 182, 156)),
    "ashen": ((150, 150, 156), (92, 92, 98), (204, 204, 208)),
    "black": ((44, 42, 44), (26, 24, 26), (74, 70, 70)),
    "chestnut": ((140, 86, 54), (90, 54, 36), (212, 172, 132)),
    "rusty": ((178, 100, 54), (112, 62, 38), (228, 198, 162)),
    "snowy": ((236, 236, 232), (210, 210, 206), (250, 250, 248)),
    "spotted": ((198, 152, 98), (120, 88, 56), (232, 214, 182)),
    "striped": ((208, 178, 122), (130, 104, 66), (236, 222, 190)),
}


def wolf_colour(v, mat, face, p, P, b):
    x, y, z = p
    if v == "collar":   # a band round the neck (tinted with the collar colour), a metal ring at the throat
        if mat != "neck" or face in ("front", "back"):
            return CLEAR
        if not (b["z"] + 0.5 <= z < b["z"] + 2.5):
            return CLEAR
        return (180, 180, 180) if face == "bottom" and abs(x - 1) < 0.6 else (236, 236, 236)
    kind, state = v.split(":")
    back, saddle, belly = WOLF[kind]
    t = under(face, p, b, P)
    c = lerp(back, belly, smoothstep(0.45, 1.0, t))
    if mat in ("chest", "hackles", "barrel", "hips", "neck") and t < 0.4 and abs(P[0]) < 3:
        c = lerp(c, saddle, 0.55 * noise3(P, 3, 13) + 0.25)   # the dark saddle over the back and shoulders
    if kind == "spotted" and noise3(P, 2.2, 17) > 0.62:
        c = lerp(c, saddle, 0.8)
    if kind == "striped" and face in ("left", "right", "top") and math.sin(P[2] * 1.3 + noise3(P, 3, 19) * 2) > 0.55:
        c = lerp(c, saddle, 0.75)
    c = strands(ticked(c, P, 21, 0.1), p, face, 0.12, 1)
    angry = state == "angry"
    if mat == "ear":
        if pointed(face, p, b):
            return CLEAR
        return lerp(c, (40, 30, 28), 0.3) if face == "front" and abs(x - (b["x"] + 1)) < 0.6 else lerp(c, saddle, 0.3)
    if mat in ("tail_base", "brush"):
        return lerp(c, saddle, 0.3 if face == "top" else 0.0)
    if mat == "tail_tip":
        return lerp(saddle, (30, 26, 24), 0.4)
    if mat == "muzzle":
        if face == "front" and y < b["y"] + 1 and abs(x - 1) < 0.6:
            return (28, 22, 22)   # nose
        if face == "top" and z < b["z"] + 1 and abs(x - 1) < 0.6:
            return (28, 22, 22)
        if angry and face in ("left", "right", "front") and y >= b["y"] + 2:
            return (238, 232, 216) if int(z * 2) % 2 == 0 else (120, 40, 44)   # bared teeth and gum
        return lerp(c, belly, 0.4 if y > b["y"] + 1 else 0.0)
    if mat == "jaw":
        if angry and face != "bottom":
            return (238, 232, 216) if int(z * 2) % 2 == 0 else (120, 40, 44)
        return belly
    if mat == "skull":
        if face == "front" and -1.5 <= y < -0.5 and (abs(x - 0) < 0.6 or abs(x - 2) < 0.6):
            return (200, 40, 30) if angry else (214, 170, 70) if state == "wild" else (120, 84, 50)   # eyes
        if face == "front" and -2.5 <= y < -1.5 and (abs(x - 0) < 0.6 or abs(x - 2) < 0.6):
            return lerp(c, (30, 24, 22), 0.6 if angry else 0.2)   # brows, knitted when angry
        return c
    if mat == "ruff":
        return lerp(c, belly, 0.5 if y > 0 else 0.15)
    if mat in ("foreleg", "shin", "paw", "thigh"):
        c2 = lerp(c, belly, 0.35 if inner_leg(face, P) or face == "back" else 0.15)
        return lerp(c2, saddle, 0.3) if mat == "paw" and face == "top" else c2
    return c


# ============================================================================ cat and ocelot
def feline_model():
    m = Model("Feline")
    m.part("head", "root", (0, 15, -9))
    m.box("head", "skull", -2.5, -2, -3, 5, 4, 4)
    m.box("head", "cheeks", -3, 0, -2.5, 6, 2, 3)
    m.box("head", "muzzle", -1.5, 0, -4, 3, 2, 1)
    m.box("head", "ear", -2.5, -3.5, -1, 2, 2, 1)
    m.box("head", "ear", 0.5, -3.5, -1, 2, 2, 1)
    m.part("body", "root", (0, 12, -10), (HALF_PI, 0, 0))
    bd = upright(m, "body")
    m.box(bd, "neck", -2, 1.5, 1.5, 4, 4, 2)
    m.box(bd, "chest", -2, 2.5, 3, 4, 5, 5)
    m.box(bd, "barrel", -2.5, 2.5, 7.5, 5, 5, 6)
    m.box(bd, "hips", -2, 2, 13, 4, 5, 6)
    m.box(bd, "pouch", -2, 7, 9, 4, 1, 5)
    m.part("tail1", "root", (0, 15, 8), (0.9, 0, 0))
    m.box("tail1", "tail", -0.5, 0, 0, 1, 8, 1)
    m.part("tail2", "root", (0, 20, 14))
    m.box("tail2", "tail_end", -0.5, 0, 0, 1, 8, 1)
    for side, s in ((1, "left"), (-1, "right")):
        m.part(f"{s}_hind_leg", "root", (side * 1.1, 18, 5))
        m.box(f"{s}_hind_leg", "haunch", -1, -1.5, 0, 2, 4, 3)
        m.box(f"{s}_hind_leg", "shin", -1, 2.5, 1, 2, 3, 2)
        m.box(f"{s}_hind_leg", "paw", -1, 5, 0.5, 2, 1, 2)
        m.part(f"{s}_front_leg", "root", (side * 1.2, 14.1, -5))
        m.box(f"{s}_front_leg", "foreleg", -1, 0, 0, 2, 9, 2)
        m.box(f"{s}_front_leg", "paw", -1, 9, -0.5, 2, 1, 2)
    return m


CAT = {   # coat, belly, mark, eye, pattern
    "tabby": ((150, 128, 98), (210, 196, 170), (62, 50, 40), (150, 170, 60), "tabby"),
    "black": ((34, 32, 34), (240, 238, 234), None, (190, 170, 50), "tuxedo"),
    "red": ((218, 134, 64), (240, 214, 170), (166, 86, 38), (200, 150, 40), "tabby"),
    "siamese": ((232, 220, 196), (240, 232, 214), (74, 56, 46), (90, 140, 210), "points"),
    "british_shorthair": ((132, 138, 148), (150, 156, 166), None, (210, 140, 50), "solid"),
    "calico": ((242, 240, 234), (246, 244, 240), (210, 120, 50), (180, 160, 50), "calico"),
    "persian": ((226, 192, 142), (240, 220, 186), (196, 156, 104), (200, 140, 50), "solid"),
    "ragdoll": ((236, 226, 206), (246, 242, 234), (120, 98, 82), (80, 130, 200), "points"),
    "white": ((242, 240, 236), (248, 246, 242), None, (90, 150, 210), "solid"),
    "jellie": ((120, 118, 124), (238, 236, 232), None, (170, 170, 60), "tuxedo"),
    "all_black": ((28, 26, 30), (36, 34, 38), None, (210, 180, 40), "solid"),
    "ocelot": ((214, 170, 92), (240, 228, 200), (54, 38, 26), (190, 150, 60), "rosettes"),
}


def feline_colour(v, mat, face, p, P, b):
    x, y, z = p
    if v == "collar":
        if mat != "neck" or face in ("front", "back") or not (b["z"] + 0.5 <= z < b["z"] + 1.5):
            return CLEAR
        return (236, 236, 236) if not (face == "bottom" and abs(x) < 0.6) else (230, 200, 60)   # little bell
    coat, belly, mark, eye_c, pat = CAT[v]
    t = under(face, p, b, P)
    c = lerp(coat, belly, smoothstep(0.55, 1.0, t))
    tail = mat in ("tail", "tail_end")
    leg = mat in ("foreleg", "shin", "paw", "haunch")
    if pat == "tabby":
        band = math.sin(P[2] * 1.4 + noise3(P, 3, 23) * 2.5) > 0.4 if not leg else math.sin(P[1] * 1.6) > 0.5
        if (band and t < 0.75) or (tail and int(y) % 3 == 0):
            c = lerp(c, mark, 0.7)
    elif pat == "tuxedo":
        white = t > 0.6 or (mat == "chest" and face == "front") or mat == "paw" or (mat == "muzzle") or (mat == "cheeks" and y > 0.5) \
            or (v == "jellie" and noise3(P, 3, 29) > 0.66)
        c = belly if white else coat
    elif pat == "points":
        point = mat in ("muzzle", "ear") or tail or (mat in ("foreleg", "shin") and P[1] > 18) or (mat == "skull" and face == "front") \
            or (mat == "cheeks" and face == "front")
        c = lerp(c, mark, 0.85) if point else c
        if v == "ragdoll" and mat == "paw":
            c = belly
    elif pat == "calico":
        k = noise3(P, 2.6, 31)
        if k > 0.62:
            c = mark
        elif k < 0.3 and t < 0.6:
            c = (38, 34, 36)
    elif pat == "rosettes":
        k = noise3((P[0] * 1.6, P[1] * 1.6, P[2] * 1.6), 1.2, 37)
        if t < 0.8 and 0.6 < k < 0.78:
            c = mark
        elif t < 0.8 and k >= 0.78:
            c = lerp(coat, (176, 120, 56), 0.6)   # the darker centre of a rosette
        if tail and int(y) % 3 == 0:
            c = mark
    c = ticked(c, P, 41, 0.06)
    if mat == "ear":
        if pointed(face, p, b):
            return CLEAR
        if face == "front" and pat not in ("points",):
            return lerp((220, 170, 160), c, 0.4)
        return c
    if mat == "skull" and face == "front":
        if -1 <= y < 0 and abs(abs(x) - 1) < 0.3:
            return eye_c
        if pat == "tabby" and y < -1 and abs(x) < 1.6 and int(x * 2) % 2 == 0:
            return lerp(c, mark, 0.6)   # the M on the forehead
    if mat == "muzzle" and face == "front" and y < 1 and abs(x) < 0.6:
        return (200, 130, 130) if pat not in ("points", "solid") or v in ("white", "persian") else (70, 54, 50)   # nose
    if pat == "rosettes" and mat == "cheeks" and face in ("left", "right") and abs(y - 0.5) < 0.5:
        return mark   # the dark cheek stripe
    return c


# ============================================================================ camel
def camel_model():
    m = Model("Camel")
    m.part("body", "root", (0, 4, 9.5))
    m.box("body", "chest", -7, -11, -23, 14, 12, 9)
    m.box("body", "barrel", -7.5, -11.5, -15, 15, 11, 13)
    m.box("body", "hips", -6.5, -11, -3, 13, 10, 7)
    m.box("body", "rump_round", -5.5, -10, 4, 11, 8, 1)
    m.box("body", "belly", -6, -1, -14, 12, 1, 11)
    m.box("body", "brisket", -2.5, 0.5, -21, 5, 2, 5)          # the chest callus it kneels on
    m.part("hump", "body", (0, -12, -10))
    m.box("hump", "hump", -5, -3, -7, 10, 4, 14)
    m.box("hump", "hump_top", -4, -6, -5.5, 8, 3, 11)
    m.box("hump", "hump_crown", -2.5, -7.5, -3.5, 5, 2, 7)
    m.part("tail", "body", (0, -9, 3.5), (0.15, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 10, 1)
    m.box("tail", "tuft", -1, 9, -1, 2, 4, 2)
    m.part("head", "body", (0, -3, -19.5))
    m.box("head", "neck_base", -3.5, -5, -6, 7, 8, 8)
    m.part("neck", "head", (0, -1, -4), (0.2, 0, 0))
    m.box("neck", "neck", -3, -3.5, -7, 6, 6, 8)
    m.part("upper_neck", "neck", (0, -0.5, -6.5), (-0.1, 0, 0), name="upper")
    m.box("upper_neck", "upper_neck", -2.5, -16, -2.5, 5, 17, 5)
    m.box("upper_neck", "mane", -1, -14, 2, 2, 12, 1)
    m.part("skull", "upper_neck", (0, -15, 0), (-0.1, 0, 0))
    m.box("skull", "cranium", -2.5, -3, -3, 5, 5, 5)
    m.box("skull", "face", -2, -2.5, -8, 4, 4, 5)
    m.box("skull", "lip", -1.5, -1.5, -9, 3, 3, 1)
    m.box("skull", "chin", -1.5, 1.5, -8.5, 3, 1, 4)
    for side, s in ((1, "left"), (-1, "right")):
        m.part(f"{s}_ear", "skull", (side * 2, -2.5, -0.5), (-0.3, 0, side * 0.5))
        m.box(f"{s}_ear", "ear", -0.5, -2, -0.5, 1, 2, 1)
    for side, s in ((1, "left"), (-1, "right")):
        m.part(f"{s}_front_leg", "root", (side * 4.9, 1, -10.5))
        m.box(f"{s}_front_leg", "forearm", -2, 2, -2, 4, 9, 4)
        m.box(f"{s}_front_leg", "knee", -2, 11, -2.5, 4, 2, 4)
        m.box(f"{s}_front_leg", "cannon", -1.5, 13, -1.5, 3, 8, 3)
        m.box(f"{s}_front_leg", "pad", -2, 21, -3, 4, 2, 5)
        m.part(f"{s}_hind_leg", "root", (side * 4.9, 1, 9.5))
        m.box(f"{s}_hind_leg", "thigh", -2.5, 0, -2.5, 5, 10, 5)
        m.box(f"{s}_hind_leg", "hock", -1.5, 10, -1, 3, 3, 3)
        m.box(f"{s}_hind_leg", "cannon", -1.5, 13, -1.5, 3, 8, 3)
        m.box(f"{s}_hind_leg", "pad", -2, 21, -3, 4, 2, 5)
    return m


CAM = dict(coat=(202, 160, 108), dark=(150, 108, 70), pale=(226, 198, 156), callus=(96, 84, 74), pad=(70, 58, 50), lip=(80, 62, 52))


def camel_colour(v, mat, face, p, P, b):
    x, y, z = p
    t = under(face, p, b, P)
    c = ticked(lerp(CAM["coat"], CAM["pale"], smoothstep(0.6, 1.0, t) * 0.7), P, 51, 0.08)
    if mat in ("hump", "hump_top", "hump_crown", "mane"):
        return strands(lerp(c, CAM["dark"], 0.35 if mat != "hump" else 0.15), p, face, 0.18, 1)   # the shaggier hair on the hump
    if mat in ("brisket", "knee"):
        return CAM["callus"] if face in ("bottom", "front") or mat == "brisket" else c
    if mat == "pad":
        return CAM["pad"] if face == "bottom" or y > b["y"] + 1 else lerp(c, CAM["pad"], 0.5)
    if mat in ("lip", "chin"):
        if mat == "lip" and face == "front" and -1 <= y < 0:
            return (40, 30, 26) if abs(x) > 0.5 else CAM["lip"]   # slit nostrils
        return lerp(c, CAM["lip"], 0.45)
    if mat == "cranium" and face in ("left", "right") and -1.5 <= y < -0.5 and b["z"] <= z < b["z"] + 1.5:
        return (30, 22, 18)
    if mat == "cranium" and face in ("left", "right") and -2.5 <= y < -1.5 and b["z"] <= z < b["z"] + 2:
        return (70, 54, 40)   # heavy lashes over the eye
    if mat == "tuft":
        return CAM["dark"]
    if mat == "ear":
        return lerp(c, CAM["dark"], 0.3)
    if mat in ("forearm", "cannon", "thigh", "hock"):
        return lerp(c, CAM["pale"], 0.3 * rel(P[1], 10, 24))
    if mat == "neck" and face == "bottom" or mat == "upper_neck" and face == "front":
        return strands(lerp(c, CAM["dark"], 0.2), p, face, 0.2, 1)   # throat beard
    return c


# ============================================================================ main
MODELS = []
# collars are overlays, and cats never leave a carcass
NOSKIN = ("collar", "cat/cat_")


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
    register(rabbit_model(), "rabbit", rabbit_colour, {f"rabbit/rabbit_{k}": k for k in RAB}, ())
    register(fox_model(), "fox", fox_colour, {"fox/fox": "red", "fox/fox_sleep": "red:sleep", "fox/fox_snow": "snow",
                                              "fox/fox_snow_sleep": "snow:sleep"}, ())
    wolves = {"wolf/wolf_collar": "collar"}
    for k in WOLF:
        base = "wolf/wolf" if k == "pale" else f"wolf/wolf_{k}"
        wolves.update({base: k + ":wild", base + "_tame": k + ":tame", base + "_angry": k + ":angry"})
    register(wolf_model(), "wolf", wolf_colour, wolves, ())
    cats = {f"cat/cat_{k}": k for k in CAT if k != "ocelot"}
    cats.update({"cat/ocelot": "ocelot", "cat/cat_collar": "collar"})
    register(feline_model(), "feline", feline_colour, cats, ())
    register(camel_model(), "camel", camel_colour, {"camel/camel": "adult"}, ("pad",))
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
            if skin is True and not any(n in name for n in NOSKIN):
                save(m.skinned(img, keep), f"skinned/vanilla/{name}.png")
            elif isinstance(skin, str):
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
