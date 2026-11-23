"""The dry-country warm fauna's models and skins, each from one table (wildlife/warm, docs/fauna/warm.md):

- src/main/java/com/mechanical/client/wildlife/warm/WarmDryGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the skinned carcass skins in textures/entity/skinned/wildlife/

Tortoise (TortoiseModel: a high carapace stepped up from a flared marginal rim to a crown of raised vertebral scutes,
every scute with a dark seam, olive-brown growth rings and a pale centre; a pale plastron with dark seams; a wrinkled
scaly head with a horn beak on a leathery neck, elephantine scaly forelegs with claws, a stub tail), Desert Lizard
(DesertLizardModel: low flat body, wedge head with a jaw, ringed eye and ear opening, sprawling legs with long-toed
feet, a long tapering tail in four segments; sandy with dark crossbands, pale spots and a cream belly) and Leopard
(LeopardModel: rounder head with a short muzzle, whisker pads, small round black-backed ears with the white spot,
green-amber eyes; a golden-tawny coat fading to a cream belly with real rosettes, solid spots on head, legs and belly,
a long tail whose spots turn to rings before the dark tip).

Same idea as gen_savanna_models.py (whose Model this uses): every box is packed into the skin and painted from the 3D
point each texel covers. Part names and the part tree are the ones the three models' setupAnim look up.
Usage: python scripts/gen_owarm_reptiles_models.py (after gen_fauna_warm_textures.py, whose gen_fauna_warm_tex_dry.py
still paints these files first, and before gen_aged_textures.py). Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import CLEAR, Model, rel, rounded  # noqa: E402
from wildlife_paint import h, lerp, save, shade  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/warm/WarmDryGeometry.java"
SIDES = ("left", "right")


class M(Model):
    """Model that also takes flat planes (one size 0, both faces drawn): the lizard's toes."""

    def box(self, part, mat, x, y, z, w, hh, d):
        sizes = (w, hh, d)
        assert part in self.parts and all(float(v).is_integer() and v >= 0 for v in sizes) and sizes.count(0) <= 1, (part, mat)
        self.boxes.append(dict(part=part, mat=mat, x=x, y=y, z=z, w=int(w), h=int(hh), d=int(d)))


def plate_scales(u, v, cell, salt, edge=0.7):
    """0 inside a scale, 1 on its rim: rounded scales on a brick grid (every other row offset), in face coordinates."""
    row = math.floor(v / cell)
    fu = (u / cell + 0.5 * (row % 2)) % 1
    fv = (v / cell) % 1
    r = math.hypot(fu - 0.5, (fv - 0.5) * 1.2)
    return 1.0 if r > edge * 0.5 + 0.1 * h(math.floor(u / cell + 0.5 * (row % 2)), row, salt) else 0.0


def face_uv(face, P):
    """Two model-space coordinates across a face (so patterns line up on neighbouring boxes)."""
    if face in SIDES:
        return P[2], P[1]
    if face in ("front", "back"):
        return P[0], P[1]
    return P[0], P[2]


# ============================================================================ tortoise
def tortoise_model():
    m = Model("Tortoise")
    m.part("body", "root", (0, 21, 0))
    m.box("body", "plastron", -4.5, -0.5, -6, 9, 1, 12)
    # the carapace: every tier is a cross of two boxes, so the shell is oval from above, stepping up from the flared
    # marginal rim to a high dome with the raised vertebral scutes along the ridge
    for mat, y, hh, (w, d) in (("lip", -1.5, 1, (12, 15)), ("rim", -2.5, 2, (11, 14)), ("costal", -4.5, 2, (10, 13)),
                               ("upper", -5.5, 1, (8, 12)), ("dome", -6.5, 1, (6, 10))):
        m.box("body", mat, -w / 2, y, -(d - 2) / 2, w, hh, d - 2)
        m.box("body", mat, -(w - 2) / 2, y, -d / 2, w - 2, hh, d)
    m.box("body", "crown", -2, -7.5, -3.5, 4, 1, 7)
    for z in (-3.5, -1, 1.5):
        m.box("body", "vert", -1, -7.75, z, 2, 1, 2)
    m.part("neck", "body", (0, -1.5, -6.5), (-0.25, 0, 0))
    m.box("neck", "neck", -1.5, -1.5, -4, 3, 3, 4)
    m.part("head", "neck", (0, -0.5, -4), (0.25, 0, 0))
    m.box("head", "skull", -2, -2, -3, 4, 3, 3)
    m.box("head", "brow", -1.5, -2.5, -2.5, 3, 1, 2)          # the domed crown of the head
    m.box("head", "cheek", -1.5, 1, -3, 3, 1, 2)              # the jaw, under the skull
    m.box("head", "beak", -1.5, -1.5, -4, 3, 2, 1)            # the horny beak, hooked over the jaw
    m.part("tail", "body", (0, -1, 6.5), (-0.5, 0, 0))
    m.box("tail", "tail", -1, -1, 0, 2, 2, 2)
    m.box("tail", "tail_tip", -0.5, -0.5, 2, 1, 1, 1)
    for side, s in ((-1, "right"), (1, "left")):
        # forelegs: flat elephantine columns armoured with big scales, splayed a little; hind legs like posts
        m.part(f"{s}_front_leg", "root", (side * 4, 20.5, -4.5), (0, 0, -side * 0.12))
        m.box(f"{s}_front_leg", "foreleg", -1.5, -0.5, -1.5, 3, 3, 3)
        m.box(f"{s}_front_leg", "forefoot", -2, 2.5, -2.5, 4, 1, 4)
        m.part(f"{s}_hind_leg", "root", (side * 3.8, 20.5, 4.5))
        m.box(f"{s}_hind_leg", "hindleg", -1.5, -0.5, -1.5, 3, 3, 3)
        m.box(f"{s}_hind_leg", "hindfoot", -2, 2.5, -2, 4, 1, 4)
    return m


TT = dict(seam=(62, 44, 24), ring=(116, 92, 50), ring2=(150, 118, 66), tan=(184, 150, 88), pale=(232, 206, 134),
          olive=(132, 118, 64), plas=(220, 200, 142), plas_d=(190, 166, 108), plas_seam=(104, 80, 46),
          skin=(198, 172, 114), skin_d=(128, 104, 66), skin_l=(220, 198, 146), scale=(212, 188, 126), horn=(74, 62, 44),
          horn_l=(126, 108, 78), claw=(62, 50, 36), eye=(18, 14, 12), glint=(232, 226, 210), lid=(150, 124, 80),
          neck=(172, 152, 110))
TORTOISE_SHELL = ("plastron", "lip", "rim", "costal", "upper", "dome", "crown", "vert")

# scute centres in the body's space: 4 vertebrals on the ridge, 3 costals a side, the marginal ring round the rim
_SCUTES = [(0, -7.0, z) for z in (-4.5, -1.5, 1.5, 4.5)]
_SCUTES += [(s * 3.9, -4.4, z) for s in (-1, 1) for z in (-4.0, 0.0, 4.0)]
_SCUTES += [(s * 5.7, -1.5, z) for s in (-1, 1) for z in (-4.5, -1.5, 1.5, 4.5)]
_SCUTES += [(x, -1.5, s * 7.2) for s in (-1, 1) for x in (-3.0, 0.0, 3.0)]
_SCUTES += [(sx * 4.8, -1.5, sz * 6.4) for sx in (-1, 1) for sz in (-1, 1)]


def scute(P):
    """(index of the nearest scute, distance to it, distance to the seam) for a point on the shell."""
    q = (P[0], (P[1] - 21) * 1.5, P[2])
    ds = sorted((math.dist(q, (c[0], c[1] * 1.5, c[2])), i) for i, c in enumerate(_SCUTES))
    return ds[0][1], ds[0][0], (ds[1][0] - ds[0][0]) * 0.5


def tortoise_colour(v, mat, face, p, P, b):
    c = TT
    x, y, z = p
    if mat == "plastron":
        if face != "bottom":
            return c["plas_d"]
        # pale yellow plates with dark seams down the middle and across, dark blotches on the inner corners
        if abs(x) < 0.6 or any(abs(z - s) < 0.6 for s in (-3.5, -0.5, 2.5)):
            return c["plas_seam"]
        return lerp(c["plas"], c["plas_d"], 0.3 * h(int(x * 2 + 9), int(z * 2 + 20), 3) + (0.4 if abs(x) < 2 and h(int(z + 9), 1, 4) < 0.4 else 0))
    if mat in TORTOISE_SHELL:
        if face == "bottom":
            return shade(c["ring"], 0.7)
        i, d, seam = scute(P)
        if seam < 0.26:
            return c["seam"]
        # growth rings round each scute's pale centre (the areola): pale, a tan band, an olive ring, tan to the seam
        if d < 1.25:
            col = c["pale"]
        elif d < 1.9:
            col = lerp(c["tan"], c["pale"], 0.4)
        elif d < 2.5 and seam > 0.6:
            col = lerp(c["ring2"], c["olive"], 0.4)
        else:
            col = c["tan"]
        if seam < 0.5:
            col = lerp(col, c["ring"], 0.45)  # the darker edge band inside the seam
        if mat == "vert" and face == "top":
            col = lerp(col, c["pale"], 0.35)  # the raised knobs catch the light
        return lerp(col, c["olive"], 0.18 * h(i, 7, 5))
    if mat == "neck":
        # leathery skin in loose folds round the neck, paler underneath
        fold = (z + 0.3 * math.sin(x * 2 + y)) % 1.3 < 0.45
        base = lerp(c["neck"], c["skin_l"], 0.5) if face == "bottom" else c["neck"]
        return lerp(base, c["skin_d"], 0.5 if fold else 0.15 * h(int(x * 3 + 9), int(z * 3 + 30), 6))
    if mat in ("skull", "brow", "cheek"):
        if mat == "skull" and face in SIDES and y < 0 and z < -1:
            # the dark eye high on the head, in its heavy wrinkled lids
            if y < -1 and z < -2:
                return c["eye"]
            return c["lid"]
        u, w = face_uv(face, P)
        rim = plate_scales(u * 1.3, w * 1.3, 2.0, 7)
        if face == "top":
            # the big horny plates of the crown, browner, each with a dark rim
            return lerp(lerp(c["scale"], c["horn_l"], 0.35 * h(int(u * 2 + 9), int(w * 2 + 9), 8)), c["skin_d"], 0.75 * rim)
        base = lerp(c["skin"], c["skin_l"], 0.4 if mat == "cheek" else 0)
        if face == "bottom":
            base = c["skin_l"]
        return lerp(base, c["skin_d"], 0.5 * rim)
    if mat == "beak":
        if face == "bottom" or (face != "top" and y > 0):
            return c["horn"]  # the cutting edge, closed over the jaw
        return lerp(c["horn_l"], c["horn"], 0.3 if face in SIDES else 0.0)
    if mat in ("foreleg", "forefoot", "hindleg", "hindfoot"):
        fore = mat.startswith("fore")
        if mat.endswith("foot"):
            if face == "bottom":
                return c["skin_d"]
            if face == "front" or (face in SIDES and z < b["z"] + 1):
                # blunt horn claws along the front of the foot
                return c["claw"] if int(x - b["x"]) % 2 == 0 else c["skin_d"]
            return lerp(c["skin"], c["skin_d"], 0.3)
        u, w = face_uv(face, P)
        big = fore and face in ("front", "left", "right")
        rim = plate_scales(u, w, 1.5 if big else 1.0, 11 if fore else 12)
        base = lerp(c["scale"], c["skin_l"], 0.35) if big else c["skin"]
        return lerp(base, c["skin_d"], 0.6 * rim)
    if mat in ("tail", "tail_tip"):
        rim = plate_scales(P[0] + P[1], P[2], 1.0, 13)
        return lerp(c["skin"], c["skin_d"], 0.5 * rim)
    raise KeyError(mat)


# ============================================================================ desert lizard
LZ_TAIL_REST = (-0.45, 0.25, 0.15, 0.05)
SPRAWL = 1.2


def lizard_model():
    m = M("Desert lizard")
    m.part("body", "root", (0, 21, 0))
    m.box("body", "torso", -2, -1, -3.5, 4, 2, 7)
    m.box("body", "back", -1.5, -1.4, -3, 3, 1, 6)           # the low ridge down the spine
    m.box("body", "neck", -1.5, -1.1, -4.5, 3, 2, 1)
    m.box("body", "hips", -1.5, -0.9, 3.5, 3, 2, 1)
    m.part("head", "body", (0, -0.3, -4.5))
    m.box("head", "skull", -2, -1.5, -3, 4, 2, 3)            # broad at the jowls...
    m.box("head", "face", -1.5, -1.2, -4, 3, 2, 1)
    m.box("head", "snout", -1, -0.8, -5, 2, 1, 1)            # ...to a pointed wedge
    m.box("head", "jaw", -1.5, 0.5, -4.5, 3, 1, 4)
    m.part("throat", "head", (0, 0.6, -1.5))
    m.box("throat", "throat", -1, 0, -1, 2, 1, 2)
    m.part("tail0", "body", (0, -0.2, 4.5), (LZ_TAIL_REST[0], 0, 0))
    m.box("tail0", "tail0", -1.5, -0.8, 0, 3, 2, 4)
    m.part("tail1", "tail0", (0, 0.2, 4), (LZ_TAIL_REST[1], 0, 0))
    m.box("tail1", "tail1", -1, -0.8, 0, 2, 2, 4)
    m.part("tail2", "tail1", (0, 0.2, 4), (LZ_TAIL_REST[2], 0, 0))
    m.box("tail2", "tail2", -1, -0.5, 0, 2, 1, 4)
    m.part("tail3", "tail2", (0, 0, 4), (LZ_TAIL_REST[3], 0, 0))
    m.box("tail3", "tail3", -0.5, -0.5, 0, 1, 1, 4)
    for side, s in ((-1, "right"), (1, "left")):
        for hind in (False, True):
            leg = f"{s}_{'hind' if hind else 'front'}_leg"
            low = f"{s[0]}{'h' if hind else 'f'}_lower"
            # the upper leg rolls out sideways (elbow/knee out), the lower leg rolls back down to the foot
            m.part(leg, "body", (side * 1.6, 0.3, 2.5 if hind else -2.3), (0, 0, -side * SPRAWL))
            m.box(leg, "thigh" if hind else "arm", -0.5, 0, -1 if hind else -0.5, 1, 2, 2 if hind else 1)
            m.part(low, leg, (0, 2, 0), (0, 0, side * SPRAWL), name="lower")
            m.box(low, "shin", -0.5, 0, -0.5, 1, 2, 1)
            m.box(low, "hind_toes" if hind else "toes", -1.5, 1.95, -3 if hind else -2.5, 3, 0, 4 if hind else 3)
    return m


TOES_FRONT = ("c.c", "x.x", "xxx")
TOES_HIND = ("..c", "c.x", "x.x", "xxx")
LZ = dict(sand=(208, 168, 106), ochre=(178, 130, 74), band=(96, 64, 36), band_l=(146, 108, 66), spot=(236, 208, 150),
          belly=(238, 228, 200), belly_d=(214, 200, 166), dot=(92, 66, 40), eye=(16, 12, 10), ring=(236, 210, 120),
          ear=(118, 86, 54), mouth=(96, 70, 46), claw=(60, 46, 34))


def lizard_back(face, P, b):
    """The dorsal pattern in model space: dark crossbands with pale spots between them, sandy flanks, cream belly."""
    if face == "bottom":
        return lerp(LZ["belly"], LZ["belly_d"], 0.3 * h(int(P[0] * 2 + 9), int(P[2] * 2 + 40), 21))
    top = face == "top" or P[1] < 20.6
    low = face in SIDES and P[1] > 21.6
    base = lerp(LZ["sand"], LZ["ochre"], 0.25 + 0.25 * h(int(P[0] * 2 + 9), int(P[2] * 2 + 40), 22))
    if low:
        return lerp(base, LZ["belly"], 0.6)
    band = (P[2] + 40 + 0.4 * math.sin(P[0] * 2)) % 3.2
    if band < 1.0 and (top or face in ("front", "back")):
        return LZ["band"] if abs(P[0]) < 1.3 or face != "top" else LZ["band_l"]
    if band < 1.0:
        return lerp(base, LZ["band"], 0.55)
    if 1.6 < band < 2.6 and h(int(P[0] * 2 + 9), int(P[2] + 40), 23) < 0.35:
        return LZ["spot"]  # pale spots between the bands
    if h(int(P[0] * 3 + 9), int(P[2] * 3 + 40), 24) < 0.1:
        return LZ["dot"]
    return base


def lizard_colour(v, mat, face, p, P, b):
    c = LZ
    x, y, z = p
    if mat in ("torso", "back", "neck", "hips"):
        return lizard_back(face, P, b)
    if mat in ("tail0", "tail1", "tail2", "tail3"):
        if face == "bottom":
            return c["belly_d"]
        # sandy tail in clean dark rings, the rings closing up towards the tip
        ring = (P[2] + 40) % (3.0 if mat in ("tail0", "tail1") else 2.4)
        if ring < 0.9:
            return c["band"]
        return lerp(c["sand"], c["ochre"], 0.3 + 0.2 * h(int(P[0] * 2 + 9), int(P[2] * 2 + 40), 25))
    if mat in ("skull", "face", "snout"):
        if face == "bottom":
            return c["belly"]
        if mat == "skull" and face in SIDES:
            # the eye in its pale ring, high on the head, and the dark ear opening behind it
            if -1.5 <= y < -0.5 and -2 <= z < -1:
                return c["eye"]
            if -0.5 <= y < 0.5 and -2 <= z < -1:
                return c["ring"]
            if -0.5 <= y < 0.5 and z >= -1:
                return c["ear"]
        if mat == "skull" and face == "top" and -2 <= z < -1 and abs(x) > 1:
            return c["ring"]  # the ring rounds over the top of the eye
        base = lerp(c["sand"], c["ochre"], 0.3)
        if face == "top" and h(int(x * 2 + 9), int(z * 2 + 40), 26) < 0.15:
            return c["dot"]  # small dark scales on the crown
        if face in SIDES and y > 0.2:
            return lerp(base, c["belly"], 0.4)  # pale upper lip
        return base
    if mat == "jaw":
        if face == "top":
            return c["mouth"]
        return lerp(c["sand"], c["ochre"], 0.2) if face != "bottom" else c["belly"]
    if mat == "throat":
        return lerp(c["belly"], c["ring"], 0.35) if face == "bottom" else c["belly_d"]
    if mat in ("arm", "thigh", "shin"):
        if h(int(P[0] * 2 + P[2] * 2 + 40), int(P[1] * 2 + 40), 27) < 0.25:
            return c["band"]
        return lerp(c["sand"], c["ochre"], 0.5)
    if mat in ("toes", "hind_toes"):
        # long splayed toes from a small palm, everything between them cut out; dark claws on the tips
        mask = TOES_HIND if mat == "hind_toes" else TOES_FRONT
        col, row = int(x - b["x"]), int(z - b["z"])  # row 0 at the toe tips
        if mask[row][col] == ".":
            return CLEAR
        return c["claw"] if mask[row][col] == "c" else lerp(c["ochre"], c["band"], 0.25 + 0.1 * row)
    raise KeyError(mat)


# ============================================================================ leopard
LP_TAIL_REST = (0.5, 0.5, 1.0)


def leopard_model():
    m = Model("Leopard")
    m.part("body", "root", (0, 13, 0))
    m.box("body", "chest", -3.5, -4, -9, 7, 8, 10)            # deep chest
    m.box("body", "withers", -3, -4.6, -9, 6, 1, 6)           # the shoulder blades rounding the back
    m.box("body", "loins", -3, -3.5, 1, 6, 7, 9)              # the waist tucks up to the hips
    m.box("body", "rump", -2.5, -4.1, 2, 5, 1, 7)
    m.box("body", "shoulders", -4, -3, -8, 8, 6, 5)
    m.box("body", "neck", -2.5, -3.5, -12, 5, 6, 4)
    m.part("tail0", "body", (0, -2.5, 9.5), (LP_TAIL_REST[0], 0, 0))
    m.box("tail0", "tail0", -1, 0, -1, 2, 8, 2)
    m.part("tail1", "tail0", (0, 7.8, 0), (LP_TAIL_REST[1], 0, 0))
    m.box("tail1", "tail1", -1, 0, -1, 2, 7, 2)
    m.part("tail2", "tail1", (0, 6.8, 0), (LP_TAIL_REST[2], 0, 0))
    m.box("tail2", "tail2", -1, 0, -1, 2, 5, 2)
    m.part("head", "body", (0, -2.5, -11.5))
    m.box("head", "skull", -3.5, -3.5, -4.5, 7, 4, 5)         # a round cat skull
    m.box("head", "crown", -2.5, -4.3, -4, 5, 1, 4)
    m.box("head", "jowls", -3.5, 0.5, -3.5, 7, 1, 3)
    m.box("head", "muzzle", -2, -0.5, -6.5, 4, 2, 2)          # short, with the whisker pads
    m.box("head", "bridge", -1, -1.2, -6, 2, 1, 2)
    m.box("head", "nose", -1, -0.8, -6.8, 2, 1, 1)
    m.part("jaw", "head", (0, 1.5, -4.5))
    m.box("jaw", "jaw", -1.5, 0, -2, 3, 1, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "head", (side * 2.4, -3.8, -1.5), (0, 0, side * 0.25))
        m.box(f"{s}_ear", "ear", -1.5, -2, -0.5, 3, 2, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 2.2, 14, -6))
        m.box(f"{s}_front_leg", "upper", -1.5, -2, -2, 3, 6, 4)
        m.part(f"{s[0]}f_lower", f"{s}_front_leg", (0, 4, 0), name="lower")
        m.box(f"{s[0]}f_lower", "lower", -1, 0, -1, 2, 4, 2)
        m.box(f"{s[0]}f_lower", "paw", -1.5, 4, -2, 3, 2, 3)  # big round forepaws
        m.part(f"{s}_hind_leg", "root", (side * 2.2, 13, 7.5))
        m.box(f"{s}_hind_leg", "thigh", -1.5, -2, -2.5, 3, 7, 5)
        m.part(f"{s[0]}h_lower", f"{s}_hind_leg", (0, 5, 1), name="lower")
        m.box(f"{s[0]}h_lower", "hock", -1, 0, -1, 2, 4, 2)
        m.box(f"{s[0]}h_lower", "paw", -1.5, 4, -2, 3, 2, 3)
    return m


LP = dict(back=(190, 134, 58), gold=(214, 162, 82), pale=(232, 200, 140), cream=(242, 232, 210), centre=(158, 96, 36),
          spot=(32, 24, 18), nose=(176, 102, 92), iris=(164, 168, 64), iris_d=(120, 128, 46), pupil=(16, 14, 10),
          glint=(240, 240, 220), pad=(46, 36, 32), lip=(58, 44, 36), tongue=(190, 98, 96), tooth=(236, 230, 214))
LP_BODY = ("chest", "withers", "loins", "rump", "shoulders", "neck")


# the ring texels round a rosette's centre, in order round the clock: 8 round a 2x2 centre, 12 round a 3x3 one
_RING_SMALL = [(0, -2), (1, -1), (1, 0), (0, 1), (-1, 1), (-2, 0), (-2, -1), (-1, -2)]
_RING_BIG = sorted([(0, -2), (1, -2), (2, -1), (2, 0), (2, 1), (1, 2), (0, 2), (-1, 2), (-2, 1), (-2, 0), (-2, -1), (-1, -2)],
                   key=lambda o: math.atan2(o[1], o[0]))
# which ring texels are dark: 3-5 separate spots of one or two texels with the ground colour showing between them
_BREAKS_SMALL = ("10101010", "11011010", "11010110", "10110110")
_BREAKS_BIG = ("110110101100", "101101101010", "110101101010")


def rosette(u, w, cell, salt, big):
    """None, 'ring' or 'centre': rosettes on a jittered grid in face coordinates, worked out per texel (integer cells)."""
    u, w = math.floor(u), math.floor(w)
    row = math.floor(w / cell)
    best = None
    size = 5 if big else 4
    for dr in (-1, 0, 1):
        r = row + dr
        off = 0.5 * (r % 2)
        col = math.floor(u / cell - off)
        for dc in (-1, 0, 1):
            k = col + dc
            if h(k * 17 + 3, r, salt + 1) < 0.08:
                continue  # a few gaps in the pattern
            cu = math.floor((k + off) * cell + h(k, r, salt + 2) * (cell - size + 0.99)) + 2
            cw = math.floor(r * cell + h(k, r, salt + 3) * (cell - size + 0.99)) + 2
            d = max(abs(u - cu + (0 if big else 0.5)), abs(w - cw + (0 if big else 0.5)))
            if best is None or d < best[0]:
                best = (d, u - cu, w - cw, k, r)
    if best is None:
        return None
    _, du, dw, k, r = best
    if (max(abs(du), abs(dw)) <= 1) if big else (du in (-1, 0) and dw in (-1, 0)):
        return "centre"
    ring = _RING_BIG if big else _RING_SMALL
    breaks = _BREAKS_BIG if big else _BREAKS_SMALL
    key = (du, dw)
    if key not in ring:
        return None
    pattern = breaks[int(h(k, r, salt + 4) * len(breaks))]
    turn = int(h(k, r, salt + 5) * len(ring))
    return "ring" if pattern[(ring.index(key) + turn) % len(ring)] == "1" else None


def leopard_coat(face, P, salt=0):
    """Golden-tawny back fading to the cream belly; rosettes on back and flanks, solid spots low down and on the belly."""
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 9.0, 17.0)
    if t < 0.55:
        base = lerp(LP["back"], LP["gold"], t / 0.55)
    elif t < 0.85:
        base = lerp(LP["gold"], LP["pale"], (t - 0.55) / 0.3)
    else:
        base = lerp(LP["pale"], LP["cream"], (t - 0.85) / 0.15)
    u, w = face_uv(face, P)
    if t < 0.8:
        big = t > 0.3 and face in SIDES
        kind = rosette(u, w, 6.0 if big else 4.6, 300 + salt + (50 if face == "top" else 0), big)
        if kind == "ring":
            return LP["spot"]
        if kind == "centre":
            return lerp(base, LP["centre"], 0.8)
        return base
    if leo_spot(P, 2.6, 0.75, 310 + salt):
        return LP["spot"]
    return base


def leo_spot(P, cell, r, salt):
    """Solid spots (2-3 texels) on a jittered 3D grid."""
    base = [math.floor(P[a] / cell) for a in range(3)]
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                k = (base[0] + dx, base[1] + dy, base[2] + dz)
                if h(k[0] * 31 + k[2], k[1], salt) > 0.6:
                    continue
                c = [(k[a] + 0.25 + 0.5 * h(k[0] * 7 + a, k[1] * 13 + k[2] * 3, salt + a + 1)) * cell for a in range(3)]
                if sum((P[a] - c[a]) ** 2 for a in range(3)) < r * r:
                    return True
    return False


def leopard_colour(v, mat, face, p, P, b):
    c = LP
    x, y, z = p
    if mat in LP_BODY:
        if mat == "neck" and face == "bottom":
            return c["cream"]
        return leopard_coat(face, P)
    if mat in ("tail0", "tail1", "tail2"):
        # spots along the top of the tail, closing into rings, then the dark tip; pale underneath
        s = {"tail0": 0, "tail1": 8, "tail2": 15}[mat] + y  # distance along the tail
        under = face == "front"  # the tail hangs, so its 'front' faces down/forward: pale underside
        if s > 18:
            return c["spot"]
        if s > 10:
            if (s + 40) % 2.5 < 0.9:
                return c["spot"]
            return lerp(c["gold"], c["pale"], 0.6 if under else 0.1)
        base = lerp(c["gold"], c["cream"], 0.7) if under else c["gold"]
        if not under and leo_spot((P[0] * 2, s * 1.0, P[2] * 2), 2.4, 0.8, 330):
            return c["spot"]
        return base
    if mat in ("skull", "crown"):
        if face == "bottom":
            return c["cream"]
        if mat == "skull" and face == "front":
            col = int(x + 3.5)  # 0..6 across the face
            if y < -1.5 and y >= -2.5 and col in (1, 2, 4, 5):
                inner = col in (2, 4)
                return c["pupil"] if inner else c["iris"]
            if -1.5 <= y < -0.5 and col in (1, 2, 4, 5):
                return c["cream"] if col in (1, 5) else lerp(c["gold"], c["pale"], 0.3)  # pale under the eye
            if -3.5 <= y < -2.5 and col in (1, 2, 4, 5):
                return lerp(c["back"], c["spot"], 0.4)  # the brow
            return c["gold"] if col != 3 or y < -2.5 else lerp(c["gold"], c["pale"], 0.4)
        if mat == "skull" and face in SIDES:
            if -2.5 <= y < -1.5 and z < -3.5:
                return c["iris"]
            if y > -0.5 and z < -2.5:
                return c["cream"]
        if leo_spot(P, 1.8, 0.55, 340):
            return c["spot"]  # small solid spots all over the head
        return lerp(c["back"], c["gold"], 0.5) if face == "top" else c["gold"]
    if mat == "jowls":
        if face == "bottom":
            return c["cream"]
        if face == "front":
            return lerp(c["gold"], c["cream"], 0.5)
        if leo_spot(P, 1.8, 0.5, 341):
            return c["spot"]
        return lerp(c["gold"], c["pale"], 0.6)
    if mat in ("muzzle", "bridge"):
        if mat == "bridge":
            return lerp(c["gold"], c["back"], 0.3) if face == "top" else c["gold"]
        if face == "bottom":
            return c["cream"]
        if face == "top":
            return lerp(c["gold"], c["back"], 0.2)  # the tawny bridge of the nose runs on to the nose leather
        if face == "front" or (face in SIDES and z < -5.5):
            if y > 0.5:
                return c["cream"]  # the white upper lip
            # white whisker pads dotted with the dark whisker spots
            if face == "front" and abs(x) < 1:
                return lerp(c["gold"], c["pale"], 0.4)
            return c["spot"] if face in SIDES or abs(x) > 1.5 else c["cream"]
        return lerp(c["gold"], c["pale"], 0.5) if y < 0.5 else c["cream"]
    if mat == "nose":
        return c["nose"] if face in ("front", "top") else shade(c["nose"], 0.7)
    if mat == "jaw":
        if face == "top":
            return c["tooth"] if z < -1.2 else c["tongue"]
        return c["cream"]
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        if face == "front":
            # furred pale inside, a tawny rim round it
            return lerp(c["cream"], c["pale"], 0.3) if abs(x) < 0.6 and y > b["y"] + 0.6 else c["gold"]
        if face == "back":
            return c["cream"] if y > -1 and abs(x) < 0.6 else c["spot"]  # black back with the white spot
        return lerp(c["spot"], c["back"], 0.4)
    if mat in ("upper", "thigh", "lower", "hock"):
        inner = (face == "left" and P[0] < 0) or (face == "right" and P[0] > 0)
        t = rel(P[1], 12, 23)
        base = lerp(c["gold"], c["pale"], 0.2 + 0.5 * t)
        if inner or face == "back" and mat in ("upper", "lower"):
            base = lerp(c["pale"], c["cream"], 0.5)
        if mat in ("upper", "thigh") and face in SIDES and not inner and t < 0.35:
            kind = rosette(P[2], P[1], 5.0, 360, False)
            if kind == "ring":
                return c["spot"]
            if kind == "centre":
                return lerp(base, c["centre"], 0.5)
            return base
        if leo_spot(P, 2.0, 0.6, 361):
            return c["spot"]
        return base
    if mat == "paw":
        if face == "bottom":
            return c["pad"]
        if face == "front" and y > b["y"] + 1 and int(x - b["x"]) == 1:
            return lerp(c["pale"], c["back"], 0.5)  # the clefts between the toes
        if face == "front" and int(x - b["x"]) % 2 == 0 and y > b["y"] + 1:
            return lerp(c["cream"], c["pale"], 0.3)
        return lerp(c["pale"], c["cream"], 0.3) if face != "top" else c["pale"]
    raise KeyError(mat)


# ============================================================================ main
def main():
    models = [
        (tortoise_model(), "tortoise", "tortoise", tortoise_colour, TORTOISE_SHELL + ("beak", "forefoot", "hindfoot")),
        (lizard_model(), "desertLizard", "desert_lizard", lizard_colour, ("toes", "hind_toes")),
        (leopard_model(), "leopard", "leopard", leopard_colour, ("paw", "nose")),
    ]
    methods = []
    for m, method, name, colour, keep in models:
        m.pack()
        methods.append(m.java(method))
        img = m.paint(colour, None)
        save(img, f"wildlife/{name}.png")
        save(m.skinned(img, keep=keep), f"skinned/wildlife/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.warm;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_owarm_reptiles_models.py (with the skins it paints): tortoise, desert lizard and leopard parts and boxes. Do not edit. */
public final class WarmDryGeometry {{
{chr(10).join(methods)}
    private WarmDryGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
