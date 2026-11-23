"""The coast fauna's models and skins, each from one table (wildlife/coast/CoastFauna, docs/fauna/coast.md):

- src/main/java/com/mechanical/client/wildlife/coast/CoastGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the skinned carcass skins in textures/entity/skinned/wildlife/

Seal (harbour seal: a sleek spindle body tapering to the hips, a round head with big dark eyes, a whiskered muzzle with
V nostrils, clawed fore flippers and the fanned hind flippers; spotted grey coat with pale rings on the back and a pale
belly; the pup in a white coat), Walrus (a huge wrinkled sack of a body on thick neck folds, a small head with a broad
bristly moustache pad and long ivory tusks, small eyes, big bare flippers; warty pinkish-brown skin, darker on top; the
calf grey-brown and tuskless), Pike (a long torpedo with a duck-bill snout and protruding lower jaw, dorsal and anal fins
set far back by the forked tail; olive with rows of pale bean spots, a white belly and mottled amber fins) and Sturgeon
(a pointed shovel snout with four barbels and a tube mouth underneath, rows of bony scutes along the back, flanks and
belly edge, a shark-like tail with the long upper lobe; grey-brown above, pale below).

Same idea as gen_savanna_models.py (whose Model it uses): every box is packed into the skin and painted from the 3D
point each texel covers. Part names are the ones SealModel, WalrusModel, PikeModel and SturgeonModel look up.
Usage: python scripts/gen_owarm_coast_models.py (after gen_fauna_coast_textures.py, which still paints the items, hides
and armour, and before gen_aged_textures.py). Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import Model, rel, side_of, spot  # noqa: E402
from wildlife_paint import h, lerp, save, shade  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/coast/CoastGeometry.java"
CLEAR = (0, 0, 0, 0)


class M(Model):
    """Model that also takes flat fins and flippers (one size 0: a plane with both faces)."""

    def box(self, part, mat, x, y, z, w, hh, d):
        sizes = (w, hh, d)
        assert part in self.parts and all(float(v).is_integer() and v >= 0 for v in sizes) and sizes.count(0) <= 1, (part, mat)
        self.boxes.append(dict(part=part, mat=mat, x=x, y=y, z=z, w=int(w), h=int(hh), d=int(d)))


def gradient(t, top, mid, low, split=0.55, band=0.3):
    return lerp(top, mid, t / split) if t < split else lerp(mid, low, (t - split) / band)


def fin_edge(face, p, b, along):
    """0..1 how far a texel is across a fin (for pale rims and ragged edges); along = the axis index of the fin's span."""
    lo, hi = (b["x"], b["x"] + b["w"]) if along == 0 else (b["y"], b["y"] + b["h"]) if along == 1 else (b["z"], b["z"] + b["d"])
    return rel(p[along], lo, hi)


# ============================================================================ seal
FLIPPER_X, FLIPPER_Z = 0.3, 0.5


def seal_model():
    m = M("Seal")
    m.part("body", "root", (0, 19, 0))
    m.box("body", "barrel", -5, -4, -6, 10, 9, 10)        # the thick middle, lying on its belly
    m.box("body", "back", -4, -5, -5, 8, 1, 8)             # rounds the back
    m.box("body", "flank", -5.5, -2, -5, 11, 6, 8)         # lying, it bulges out at the sides
    m.box("body", "chest", -4.5, -3, -11, 9, 8, 5)
    m.box("body", "chest_top", -3.5, -4, -10, 7, 1, 4)
    m.box("body", "neck", -3.5, -2.5, -13, 7, 7, 2)
    m.part("head", "body", (0, -1, -13))
    m.box("head", "skull", -3.5, -3.5, -5, 7, 6, 5)        # round dog-like head, no ears
    m.box("head", "crown", -2.5, -4.5, -4, 5, 1, 3)
    m.box("head", "muzzle", -2.5, -1, -8, 5, 3, 3)         # short muzzle with the whisker pads and V nostrils
    m.box("head", "chin", -2, 2, -7.5, 4, 1, 3)
    m.box("head", "whiskers", -5.5, 0.5, -7.5, 3, 0, 2)
    m.box("head", "whiskers", 2.5, 0.5, -7.5, 3, 0, 2)
    m.part("hips", "body", (0, 0.5, 4))
    m.box("hips", "hips", -4, -3.5, 0, 8, 8, 5)            # the body tapers to the hips
    m.box("hips", "hip_top", -3, -4.5, 0.5, 6, 1, 4)
    m.box("hips", "rump", -3, -1.5, 5, 6, 6, 3)
    m.part("tail", "hips", (0, 2, 8))
    m.box("tail", "heel", -2, -1.5, -0.5, 4, 3, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_hind", "tail", (side * 1, 0, 1.5), (0, side * 0.25, 0))  # the paired hind flippers, fanned
        m.box(f"{s}_hind", "hind", -1, -0.5, 0, 2, 1, 2)
        m.box(f"{s}_hind", "hind_fan", -2, -0.5, 2, 4, 1, 4)
        m.part(f"{s}_flipper", "root", (side * 4.5, 19.5, -8), (FLIPPER_X, 0, -side * FLIPPER_Z))
        m.box(f"{s}_flipper", "flipper", -0.5, 0, -1.5, 1, 3, 3)
        m.box(f"{s}_flipper", "flipper_tip", -0.5, 3, -2, 1, 2, 3)
    return m


SE = dict(back=(74, 76, 78), flank=(134, 132, 124), belly=(204, 198, 182), spot=(38, 38, 42), ring=(176, 172, 160),
          head=(96, 96, 96), lip=(158, 152, 142), eye=(12, 10, 12), glint=(226, 232, 238), nose=(28, 26, 28),
          whisker=(236, 230, 214), flipper=(70, 70, 72), claw=(26, 22, 20))
PUP = dict(back=(222, 218, 206), flank=(234, 230, 220), belly=(244, 242, 234), spot=(206, 200, 188), ring=(240, 236, 228),
           head=(226, 222, 212), lip=(214, 208, 198), eye=(12, 10, 12), glint=(226, 232, 238), nose=(30, 28, 30),
           whisker=(250, 248, 242), flipper=(200, 194, 184), claw=(70, 64, 60))
SEAL_COAT = ("barrel", "back", "flank", "chest", "chest_top", "neck", "hips", "hip_top", "rump", "heel", "skull", "crown", "cheeks")


def seal_coat(C, face, P):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 14.5, 23.5)
    c = gradient(t, C["back"], C["flank"], C["belly"], 0.5, 0.35)
    if t < 0.45:
        # the dark back with pale rings around dark centres (the harbour seal's ringed spots)
        if spot(P, 2.6, 0.6, 41, 0.7):
            return C["spot"]
        if spot(P, 2.6, 1.2, 41, 0.7):
            return C["ring"]
        return c
    if t < 0.88:
        if spot(P, 2.1, 0.8 - 0.3 * t, 43, 0.75, (1, 1, 0.8)):
            return lerp(C["spot"], c, 0.1 + 0.3 * t)
        return c
    return lerp(C["spot"], c, 0.55) if spot(P, 2.6, 0.45, 45, 0.3) else c


def seal_colour(v, mat, face, p, P, b):
    C = PUP if v == "pup" else SE
    x, y, z = p
    if mat in ("skull", "crown"):
        c = seal_coat(C, face, P)
        if mat == "skull" and face in ("front", "left", "right"):
            # big dark eyes on the front corners of the head, wrapping round onto the sides, a small glint high up
            if face == "front" and -2.5 <= y < -0.5 and 1 <= abs(x) < 3:
                return lerp(C["glint"], C["eye"], 0.35) if abs(x) >= 2 and y < -1.5 else C["eye"]
            if face != "front" and -2.5 <= y < -0.5 and z < b["z"] + 1:
                return C["eye"]
            if face == "front":
                return lerp(C["head"], C["lip"], 0.25 if abs(x) < 1 else 0.0)
        return lerp(c, C["head"], 0.35)
    if mat in SEAL_COAT:
        return seal_coat(C, face, P)
    if mat == "muzzle":
        if face == "front":
            if -1 <= y < 0 and 0.4 <= abs(x) < 1.6:
                return C["nose"]  # the V nostrils
            if -1 <= y < 0:
                return lerp(C["head"], C["lip"], 0.3)
            if y >= 1.2 and abs(x) < 1.2:
                return lerp(C["nose"], C["lip"], 0.35)  # mouth
            return lerp(C["lip"], C["nose"], 0.45) if h(int(x * 2 + 9), int(y * 2 + 9), 51) < 0.3 else C["lip"]
        if face in ("left", "right") and y >= -0.2:
            return lerp(C["lip"], C["nose"], 0.5) if (int(z * 2) + int(y * 2)) % 3 == 0 else C["lip"]  # whisker pores
        return lerp(C["head"], C["lip"], 0.5)
    if mat == "chin":
        return C["lip"] if face != "bottom" else lerp(C["lip"], C["belly"], 0.5)
    if mat == "whiskers":
        i, j = int(x - b["x"]), int(z - b["z"])
        return C["whisker"] if (i + j) % 2 == 0 else CLEAR
    if mat in ("flipper", "flipper_tip"):
        if mat == "flipper_tip" and y >= b["y"] + 1 and face in ("bottom", "front", "left", "right") and int(z - b["z"]) % 2 == 0:
            return C["claw"]  # claws at the flipper tips
        return lerp(C["flipper"], seal_coat(C, face, P), 0.4 if mat == "flipper" and y < 1 else 0.0)
    if mat in ("hind", "hind_fan"):
        i, last = int(x - b["x"]), z > b["z"] + b["d"] - 1
        if mat == "hind_fan" and (last or face == "back") and i % 2 == 1:
            return CLEAR  # webbing notched between the toes
        dark = mat == "hind_fan" and face in ("top", "bottom") and i % 2 == 0
        return shade(C["flipper"], 0.8 if dark else 1.0)
    return C["flank"]


# ============================================================================ walrus
TUSK_X, TUSK_Z, FRONT_Z = 0.15, 0.06, 0.2


def walrus_model():
    m = M("Walrus")
    m.part("body", "root", (0, 17, 0))
    m.box("body", "barrel", -8, -6, -5, 16, 13, 11)        # the huge sack of a body
    m.box("body", "back", -7, -7, -4, 14, 1, 9)
    m.box("body", "bulge", -9, -2, -4, 18, 8, 9)           # the sack sags out at the sides
    m.box("body", "fold", -9.5, -1, -2, 19, 5, 1)          # skin folds standing out on the flanks
    m.box("body", "fold", -9.5, 0, 2, 19, 4, 1)
    m.box("body", "chest", -7.5, -7, -10, 15, 12, 5)       # deep chest propped on the fore flippers
    m.box("body", "chest_top", -6.5, -8, -9.5, 13, 1, 4)
    m.box("body", "neck", -6.5, -7.5, -12, 13, 10, 2)      # the thick wrinkled neck in two folds
    m.box("body", "neck", -5.5, -7.5, -14, 11, 9, 2)
    m.part("head", "body", (0, -5, -14))
    m.box("head", "skull", -4.5, -4, -5, 9, 7, 5)          # small round head
    m.box("head", "crown", -3.5, -5, -4, 7, 1, 3)
    m.box("head", "muzzle", -5, -1, -8, 10, 6, 4)          # the broad bristly moustache pad
    m.box("head", "pad", -5.5, 0, -7.5, 5, 4, 3)           # the two swollen lobes of the moustache
    m.box("head", "pad", 0.5, 0, -7.5, 5, 4, 3)
    m.box("head", "chin", -3, 5, -7, 6, 2, 3)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_tusk", "head", (side * 2.5, 5, -7.5), (TUSK_X, 0, -side * TUSK_Z))
        m.box(f"{s}_tusk", "tusk_root", -1, -1, -1, 2, 2, 2)
        m.box(f"{s}_tusk", "tusk", -0.5, 1, -0.5, 1, 6, 1)
    m.part("hips", "body", (0, 1, 6))
    m.box("hips", "hips", -6.5, -5, 0, 13, 11, 6)
    m.box("hips", "hip_top", -5.5, -6, 0.5, 11, 1, 5)
    m.box("hips", "rump", -5, -2, 6, 10, 8, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_hind", "hips", (side * 3, 5, 7.5))
        m.box(f"{s}_hind", "hind_base", -1.5, -1, -1, 3, 2, 2)
        m.box(f"{s}_hind", "hind", -2, -0.5, 1, 4, 1, 4)
        m.part(f"{s}_front", "root", (side * 7.5, 18, -8.5), (0, 0, -side * FRONT_Z))
        m.box(f"{s}_front", "arm", -1.5, 0, -2, 3, 5, 4)
        m.box(f"{s}_front", "flipper", -2, 5, -5, 4, 1, 6)
    return m


WA = dict(top=(122, 80, 64), skin=(176, 118, 94), low=(196, 142, 118), belly=(170, 120, 100), wrinkle=(108, 68, 54),
          wart=(204, 156, 130), hair=(150, 104, 80), pad=(208, 158, 128), bristle=(244, 232, 200), pore=(128, 84, 66),
          eye=(40, 18, 14), lid=(120, 70, 60), tusk=(234, 224, 196), tusk_root=(196, 176, 130), flipper=(126, 82, 70),
          nail=(64, 46, 40), mouth=(80, 46, 40))
CALF = dict(top=(84, 68, 60), skin=(118, 98, 86), low=(140, 118, 104), belly=(124, 104, 92), wrinkle=(70, 56, 50),
            wart=(140, 120, 106), hair=(96, 78, 66), pad=(150, 128, 110), bristle=(214, 204, 184), pore=(86, 70, 60),
            eye=(24, 16, 14), lid=(90, 70, 62), tusk=(234, 224, 196), tusk_root=(196, 176, 130), flipper=(82, 66, 60),
            nail=(46, 38, 34), mouth=(60, 42, 38))


def walrus_skin(C, face, P, folds=True):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 9, 23.5)
    c = gradient(t, C["top"], C["skin"], C["low"], 0.5, 0.3)
    if t > 0.85:
        c = lerp(c, C["belly"], 0.5)
    # deep wrinkles wandering round the body: a dark crease with a lit ridge above it
    if folds and face != "bottom":
        k = (P[1] + 1.1 * math.sin(P[2] * 0.4 + P[0] * 0.25) + 40) % 3.5
        if k < 0.7:
            return lerp(C["wrinkle"], c, 0.25)
        if k > 2.9:
            return lerp(c, C["wart"], 0.3)
    if h(int(P[0] * 2 + 40), int(P[1] * 2 + P[2] * 5 + 90), 61) < 0.05:
        return C["wart"]  # warty knobs
    if h(int(P[0] * 3 + 40), int(P[2] * 3 + P[1] + 90), 62) < 0.03:
        return C["hair"]  # sparse coarse hair
    return c


def walrus_colour(v, mat, face, p, P, b):
    C = CALF if v == "calf" else WA
    x, y, z = p
    if mat == "neck":
        c = walrus_skin(C, face, P, folds=False)
        if face in ("left", "right", "top") and int((z - b["z"]) * 2) % 4 == 0:
            return lerp(C["wrinkle"], c, 0.3)  # the ring folds round the neck
        return c
    if mat == "fold":
        c = walrus_skin(C, face, P, folds=False)
        return lerp(C["wrinkle"], c, 0.3) if face in ("front", "back", "bottom") else lerp(c, C["wart"], 0.2)
    if mat in ("bulge", "barrel", "back", "chest", "chest_top", "hips", "hip_top", "rump"):
        return walrus_skin(C, face, P)
    if mat in ("skull", "crown"):
        c = walrus_skin(C, face, P, folds=False)
        if mat == "skull" and face in ("left", "right") and -2.5 <= y < -1.5 and b["z"] + 1 <= z < b["z"] + 2:
            return C["eye"]  # small eyes high on the sides of the head
        if mat == "skull" and face in ("left", "right") and -3 <= y < -1 and b["z"] + 0.5 <= z < b["z"] + 2.5:
            return C["lid"]
        return c
    if mat in ("muzzle", "pad"):
        if face == "top" and mat == "muzzle":
            if z < b["z"] + 1.2 and 1 <= abs(x) < 3:
                return C["mouth"]  # nostrils on top of the snout
            return lerp(C["skin"], C["pad"], 0.4)
        if face == "bottom":
            return C["mouth"] if abs(x) < 2.5 else C["pad"]
        # the moustache: a fleshy pad split down the middle, stiff pale quills sprouting from its lower part
        if face in ("front", "back") and mat == "muzzle" and abs(x) < 1 and y > 0:
            return lerp(C["pad"], C["pore"], 0.6)
        row, col = int(y + 20), int((x if face in ("front", "back") else z) * 1 + 20)
        if y > 0.5 and h(col, row, 63) < 0.45:
            return C["bristle"]
        if y < 0.5 and mat == "muzzle":
            return lerp(walrus_skin(C, face, P, folds=False), C["pad"], 0.5)
        return lerp(C["pad"], C["pore"], 0.3 * h(col, row, 64))
    if mat == "chin":
        return lerp(C["low"], C["mouth"], 0.3) if face != "bottom" else C["belly"]
    if mat == "tusk_root":
        return lerp(C["pad"], C["tusk_root"], 0.4)
    if mat == "tusk":
        return lerp(C["tusk"], C["tusk_root"], rel(y, 7, 1) * 0.6) if face not in ("back", "right") else shade(C["tusk"], 0.9)
    if mat in ("arm", "hind_base"):
        return lerp(walrus_skin(C, face, P), C["flipper"], rel(y, b["y"], b["y"] + b["h"]))
    if mat == "flipper":
        if face in ("front", "top") and z < b["z"] + 1.3 and int(x - b["x"]) % 2 == 0:
            return C["nail"]  # stubby nails on the fore flipper's toes
        return lerp(C["flipper"], C["skin"], 0.25 * h(int(x * 2 + 9), int(z * 2 + 9), 65))
    if mat == "hind":
        i, last = int(x - b["x"]), z > b["z"] + b["d"] - 1
        if (last or face == "back") and i in (1, 2):
            return CLEAR  # the flipper splits into two lobes
        return shade(C["flipper"], 0.85 if face in ("top", "bottom") and i % 3 == 0 else 1.0)
    return C["skin"]


# ============================================================================ pike
PIKE_FIN_Z = -0.4


def pike_model():
    m = M("Pike")
    m.part("body", "root", (0, 21, 0))
    m.box("body", "trunk", -1.5, -2, -4, 3, 4, 8)
    m.box("body", "ridge", -1, -2.5, -3, 2, 1, 6)
    m.box("body", "belly", -1, 1.5, -3, 2, 1, 6)
    m.part("head", "body", (0, 0, -4))
    m.box("head", "head", -1.5, -1.5, -3, 3, 3, 3)
    m.box("head", "snout", -1, -1, -7, 2, 1, 4)            # the flat duck bill
    m.box("head", "jaw", -1, 0.25, -7.5, 2, 1, 4)          # the lower jaw juts out past it
    m.part("tail", "body", (0, 0, 4))
    m.box("tail", "tail", -1, -1.5, 0, 2, 3, 4)
    m.box("tail", "dorsal", 0, -4.5, 0.5, 0, 3, 4)         # dorsal and anal fins set far back
    m.box("tail", "anal", 0, 1.5, 1, 0, 3, 3)
    m.part("caudal", "tail", (0, 0, 4))
    m.box("caudal", "stock", -0.5, -1, -0.5, 1, 2, 2)
    m.box("caudal", "caudal", 0, -3.5, 1, 0, 7, 4)         # forked tail fin
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_fin", "body", (side * 1.5, 1.5, -3), (0, 0, -side * PIKE_FIN_Z))
        m.box(f"{s}_fin", "pectoral", -3 if side < 0 else 0, 0, 0, 3, 0, 3)
        m.part(f"{s}_pelvic", "body", (side * 1, 2, 2), (0, 0, side * 0.7))
        m.box(f"{s}_pelvic", "pelvic", -2 if side < 0 else 0, 0, 0, 2, 0, 3)
    return m


PK = dict(back=(46, 62, 28), flank=(92, 116, 48), low=(150, 162, 100), belly=(232, 230, 212), spot=(214, 212, 150),
          fin=(172, 128, 62), fin_dark=(88, 62, 34), fin_rim=(210, 176, 106), pec=(196, 144, 78), eye=(214, 170, 56), pupil=(16, 14, 10),
          mouth=(44, 40, 26), gill=(70, 80, 40))


def pike_body(face, P):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 18.5, 23)
    c = gradient(t, PK["back"], PK["flank"], PK["belly"], 0.55, 0.25)
    r, col = int(P[1] + 40), int(P[2] + 40)
    if 0.12 < t < 0.8 and face in ("left", "right") and (col + 2 * r) % 3 == 0 and h(col, r, 71) < 0.85:
        return lerp(PK["spot"], c, 0.1)  # staggered rows of pale bean-shaped spots
    if face == "top":
        return lerp(c, PK["back"], 0.5)
    return c


def pike_fin(face, p, b, rim):
    """Olive-amber membrane, darker rays and a few dark mottles, a pale rim (rim: 0..1 towards the fin's edge)."""
    if h(int(p[1] * 2 + 20), int(p[2] * 2 + 20), 73) < 0.18:
        return PK["fin_dark"]
    c = lerp(PK["fin"], PK["fin_rim"], (rim - 0.6) / 0.4)
    return shade(c, 0.88) if int(p[2] + 20) % 2 == 0 else c


def pike_colour(v, mat, face, p, P, b):
    x, y, z = p
    if mat in ("trunk", "ridge", "belly", "tail", "stock"):
        return pike_body(face, P)
    if mat == "head":
        c = pike_body(face, P)
        if face in ("left", "right"):
            if -1 <= y < 0 and z < b["z"] + 1.2:
                return PK["pupil"] if z < b["z"] + 0.6 else PK["eye"]  # golden eye high on the head
            if -1.5 <= y < 0.5 and z < b["z"] + 1.8:
                return PK["eye"] if y < 0 else lerp(PK["eye"], c, 0.5)
            if z > b["z"] + 2 and y > -1:
                return lerp(PK["gill"], c, 0.4)  # gill cover edge
        return c
    if mat == "snout":
        return lerp(PK["back"], PK["flank"], 0.4) if face != "bottom" else PK["mouth"]
    if mat == "jaw":
        if face == "top":
            return PK["mouth"]
        return PK["belly"] if face == "bottom" else lerp(PK["belly"], PK["low"], 0.4)
    u = rel(z, b["z"], b["z"] + b["d"])  # 0 at the fin's front, 1 at its back
    if mat in ("dorsal", "anal"):
        # low in front, tallest at the back (the pike's fins sit right before the tail)
        v = rel(y, b["y"], b["y"] + b["h"])
        v = v if mat == "dorsal" else 1 - v
        if v < 0.8 - 0.8 * u:
            return CLEAR
        return pike_fin(face, p, b, 1 - v)
    if mat == "caudal":
        # a fan widening back from the stock, the middle of the trailing edge cut into a fork
        if abs(y) > 1 + 2.5 * u or u > 0.5 and abs(y) < (u - 0.5) * 4:
            return CLEAR
        return pike_fin(face, p, b, u)
    if mat in ("pectoral", "pelvic"):
        outer = abs(x) > abs(b["x"] + (b["w"] if b["x"] >= 0 else 0)) - 1
        if outer and u > 0.67:
            return CLEAR  # rounded corner
        return lerp(PK["pec"], PK["fin_rim"], 0.5 * u)
    return PK["flank"]


# ============================================================================ sturgeon
STURGEON_FIN_Z = -0.2


def sturgeon_model():
    m = M("Sturgeon")
    m.part("body", "root", (0, 20, 0))
    m.box("body", "fore", -3, -3, -8, 6, 6, 10)
    m.box("body", "aft", -2.5, -2.5, 2, 5, 5, 6)
    m.box("body", "ridge", -2, -3.5, -7, 4, 1, 14)
    # bony scutes: one row down the back, one along each flank, one along each edge of the flat belly
    for z in range(-7, 7, 2):
        m.box("body", "scute", -0.5, -4.5, z, 1, 1, 1)
    for side in (-1, 1):
        for z in range(-7, 8, 2):
            half = 3 if z < 2 else 2.5
            m.box("body", "scute", side * half - (0.6 if side > 0 else 0.4), -0.5, z, 1, 1, 1)
        for z in range(-6, 7, 3):
            half = 3 if z < 2 else 2.5
            m.box("body", "scute", side * (half - 1) - 0.5, 2.6 if z < 2 else 2.1, z, 1, 1, 1)
    m.part("head", "body", (0, 0.5, -8))
    m.box("head", "head", -2.5, -3, -4, 5, 5, 4)
    m.box("head", "snout_base", -2, -2, -7, 4, 3, 3)
    m.box("head", "snout", -2, -1, -10, 4, 2, 3)           # the long flat shovel snout
    m.box("head", "snout_tip", -1.5, -0.5, -12, 3, 1, 2)
    m.box("head", "tip", -0.5, -0.5, -13, 1, 1, 1)
    m.box("head", "mouth", -1.5, 2, -5, 3, 1, 2)            # the toothless tube mouth underneath
    for bx in (-2.1, -1.05, 0.05, 1.1):
        m.box("head", "barbel", bx, 1, -9, 1, 2, 1)         # four barbels in a row in front of the mouth
    m.part("tail", "body", (0, -0.5, 8))
    m.box("tail", "tail", -2, -2, 0, 4, 4, 6)
    for z in (0.5, 2.5, 4.5):
        m.box("tail", "scute", -0.5, -3, z, 1, 1, 1)
        for side in (-1, 1):
            m.box("tail", "scute", side * 2 - (0.6 if side > 0 else 0.4), -0.5, z, 1, 1, 1)
    m.box("tail", "dorsal", 0, -5.5, 2, 0, 3, 4)            # dorsal fin set far back
    m.box("tail", "anal", 0, 2, 2.5, 0, 2, 3)
    m.part("tail_tip", "tail", (0, -0.5, 6))
    m.box("tail_tip", "stock", -1, -1.5, 0, 2, 3, 3)
    m.box("tail_tip", "upper_lobe", 0, -6, 0.5, 0, 6, 7)    # shark-like tail: the long upper lobe sweeps up and back
    m.box("tail_tip", "lower_lobe", 0, 0.5, 1, 0, 4, 4)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_fin", "body", (side * 3, 2.5, -5), (0, 0, -side * STURGEON_FIN_Z))
        m.box(f"{s}_fin", "pectoral", -4 if side < 0 else 0, 0, 0, 4, 0, 4)
        m.part(f"{s}_pelvic", "body", (side * 2, 3, 4), (0, 0, side * 0.5))
        m.box(f"{s}_pelvic", "pelvic", -2 if side < 0 else 0, 0, 0, 2, 0, 3)
    return m


ST = dict(back=(98, 80, 56), flank=(142, 124, 94), low=(176, 170, 152), belly=(222, 216, 198), scute=(214, 206, 184),
          scute_dark=(150, 142, 122), fin=(98, 94, 84), fin_rim=(170, 164, 146), eye=(30, 26, 22), iris=(150, 140, 110),
          mouth=(190, 150, 140), barbel=(210, 196, 176))


def sturgeon_body(face, P):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 16.5, 23)
    c = gradient(t, ST["back"], ST["flank"], ST["belly"], 0.55, 0.3)
    if t < 0.6 and h(int(P[0] * 2 + 30), int(P[2] * 2 + P[1] + 50), 81) < 0.12:
        return shade(c, 0.85)  # the rough, sandpapery skin
    return c


def sturgeon_fin(p, b, span, top_fin=False):
    e = fin_edge(None, p, b, span)
    return lerp(ST["fin"], ST["fin_rim"], (e - 0.65) / 0.35)


def sturgeon_colour(v, mat, face, p, P, b):
    x, y, z = p
    if mat in ("fore", "aft", "ridge", "tail", "stock"):
        return sturgeon_body(face, P)
    if mat == "scute":
        return ST["scute"] if face in ("top", "front") else ST["scute_dark"] if face == "bottom" else lerp(ST["scute"], ST["scute_dark"], 0.4)
    if mat == "head":
        c = sturgeon_body(face, P)
        if face in ("left", "right") and -1.5 <= y < -0.5 and b["z"] + 0.5 <= z < b["z"] + 1.5:
            return ST["eye"]  # small eyes far back on the snout
        if face in ("left", "right") and -2 <= y < 0 and b["z"] <= z < b["z"] + 2:
            return ST["iris"]
        if face in ("left", "right") and z > b["z"] + 3:
            return shade(c, 0.85)  # gill cover edge
        return lerp(c, ST["scute"], 0.25) if face == "top" else c
    if mat in ("snout_base", "snout", "snout_tip", "tip"):
        c = sturgeon_body(face, P)
        if face == "bottom":
            return lerp(ST["belly"], ST["mouth"], 0.2)
        return lerp(c, ST["low"], 0.25 if mat == "snout_base" else 0.4)
    if mat == "mouth":
        return ST["mouth"] if face != "top" else shade(ST["mouth"], 0.7)
    if mat == "barbel":
        return ST["barbel"] if y < b["y"] + 1.5 else shade(ST["barbel"], 0.85)
    if mat == "dorsal":
        # a swept-back sail: tall at the front, the trailing edge falling away
        u, v = rel(z, b["z"], b["z"] + b["d"]), rel(y, b["y"], b["y"] + b["h"])
        if v < 0.9 * u - 0.1 or v < 0.3 and u < 0.2:
            return CLEAR
        return lerp(ST["fin"], ST["fin_rim"], (0.6 - v) / 0.6)
    if mat == "anal":
        if y > b["y"] + 1 and z < b["z"] + 1:
            return CLEAR
        return sturgeon_fin(p, b, 2)
    if mat == "upper_lobe":
        # leading edge from the stock up to the tip, trailing edge back down: a long swept sickle
        u = rel(z, b["z"], b["z"] + b["d"])          # 0 at the stock, 1 at the tip
        lead = -6 * u                                 # y of the leading (upper) edge
        trail = 0.8 - 3.6 * u ** 2                    # y of the trailing (lower) edge
        if y < lead or y > trail:
            return CLEAR
        return lerp(sturgeon_body("left", P), ST["fin"], 0.4 + 0.5 * u)
    if mat == "lower_lobe":
        u = rel(z, b["z"], b["z"] + b["d"])
        if y > b["y"] + 1 + 3 * u or u > 0.6 and y < b["y"] + 2 * (u - 0.6) / 0.4:
            return CLEAR
        return sturgeon_fin(p, b, 1)
    if mat in ("pectoral", "pelvic"):
        outer = abs(x) > abs(b["x"] + (b["w"] if b["x"] >= 0 else 0)) - 1
        if outer and z > b["z"] + b["d"] - 1.5:
            return CLEAR  # swept-back corner
        return lerp(ST["fin"], ST["fin_rim"], rel(z, b["z"], b["z"] + b["d"]) * 0.6)
    return ST["flank"]


# ============================================================================
def main():
    models = [(seal_model(), "seal", seal_colour, {"seal": None, "seal_pup": "pup"}, ("flipper_tip",), ("seal",)),
              (walrus_model(), "walrus", walrus_colour, {"walrus": None, "walrus_calf": "calf"}, ("tusk", "tusk_root"), ("walrus",)),
              (pike_model(), "pike", pike_colour, {"pike": None}, (), ()),
              (sturgeon_model(), "sturgeon", sturgeon_colour, {"sturgeon": None}, (), ("sturgeon",))]
    methods = []
    for m, method, colour, skins, keep, skinned in models:
        m.pack()
        methods.append(m.java(method))
        for name, variant in skins.items():
            img = m.paint(colour, variant)
            save(img, f"wildlife/{name}.png")
            if name in skinned:
                save(m.skinned(img, keep), f"skinned/wildlife/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.coast;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_owarm_coast_models.py (with the skins it paints): the coast animals' parts and boxes. Do not edit. */
public final class CoastGeometry {{
{chr(10).join(methods)}
    private CoastGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
