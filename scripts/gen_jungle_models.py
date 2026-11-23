"""The jungle fauna's models and skins from one table each (com.mechanical.wildlife.jungle, docs/fauna/jungle.md):

- src/main/java/com/mechanical/client/wildlife/jungle/JungleGeometry.java: every model's parts and boxes (generated, do
  not edit); the hand-written models (TigerModel, PythonModel ...) animate them by part name.
- textures/entity/wildlife/<name>.png and the skinned carcass skin textures/entity/skinned/wildlife/<name>.png.

Each texel is painted from the model-space point it covers at the rest pose (critter_paint: pixels, y down with the ground
at 24, -z the front, +x the animal's left), so stripes, bands and markings run on across boxes. Small creatures are
built at twice their size in pixels and drawn at half scale (JungleFaunaClient), for finer boxes and texels.

Usage: python scripts/gen_jungle_models.py (also run by gen_fauna_jungle_textures.py). Deterministic: hashes only.
"""
import math
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from critter_paint import cube, h, lerp, pack, paint, part, save, shade, skinned, smooth, world  # noqa: E402

JAVA = HERE.parent / "src/main/java/com/mechanical/client/wildlife/jungle/JungleGeometry.java"


def local(parts, name):
    """Model space -> the part's own space at rest (for markings placed on a tilted head, a hanging nose ...)."""
    R, t = world(parts, name)

    def f(X, Y, Z):
        d = (X - t[0], Y - t[1], Z - t[2])
        return tuple(sum(R[k][i] * d[k] for k in range(3)) for i in range(3))
    return f


def clamp01(t):
    return max(0.0, min(1.0, t))


def speck(c, tx, ty, salt, amt=0.06):
    """Per-texel noise plus a faint run along the texture rows, so flat colours don't read as plastic."""
    n = (h(tx, ty, salt) - 0.5) * 2 * amt + (h(tx // 2, ty, salt + 3) - 0.5) * amt
    return shade(c, 1 + n)


def hair(c, tx, ty, salt, amt=0.08):
    """Fur: speckle plus short vertical strands (texture columns), darker roots every few texels."""
    n = (h(tx, ty, salt) - 0.5) * 2 * amt + (h(tx, ty // 2, salt + 7) - 0.5) * amt * 1.5
    return shade(c, 1 + n)


def lit(c, face, t):
    """Light from above: tops a touch brighter, undersides darker, sides darkening by t (0 at the top, 1 at the bottom)."""
    if face == "top":
        return shade(c, 1.06)
    if face == "bottom":
        return shade(c, 0.78)
    return shade(c, 1.0 - 0.16 * clamp01(t))


# =========================================================================================================== tiger
# A heavy great cat: a deep chest under jutting shoulder blades, long loins and rump, a short thick neck, a broad round
# head with white sideburns flaring out, a short wide muzzle, small round ears (black behind with a white spot), thick
# legs on big paws, a long tail ringed black to the tip. Orange above, cream lower down, white belly, inner legs and face;
# narrow black stripes, doubled and forked, down the flanks to a point, chevrons over the spine, rings on legs and tail.

TG = dict(back=(196, 96, 26), orange=(222, 122, 40), cream=(236, 170, 96), white=(242, 236, 224), black=(22, 16, 14),
          brown=(70, 36, 18), nose=(196, 122, 112), eye=(222, 186, 64), pupil=(16, 12, 10), lip=(40, 26, 22),
          claw=(222, 214, 196), pad=(58, 42, 38), mouth=(150, 70, 70), tooth=(240, 236, 220))


def tiger_stripe(u, v, salt, period=3.0, width=1.25, reach=(6.0, 11.0)):
    """Stripes across a surface: u runs along the stripes' spacing (px), v along their length (px from their root). Each
    stripe wavers, is a little over a texel wide, thins to a point at a length of its own; some fork near the end."""
    k = math.floor(u / period)
    off = math.sin(v * 0.55 + k * 1.9) * 0.5 + (h(k, 2, salt) - 0.5) * 0.8
    du = (u + off) - k * period
    length = reach[0] + (reach[1] - reach[0]) * h(k, 3, salt)
    taper = clamp01(1.25 - v / length)
    w = width * (0.8 + 0.4 * h(k, 1, salt)) * min(1.0, taper)
    if 0 <= du < w:
        return True
    return h(k, 4, salt) < 0.45 and v > length * 0.45 and abs(du - 1.9) < 0.55 * taper  # a fork


def tiger(skin=False):
    P = TG

    def coat(face, X, Y, Z, n, tx, ty, top=8.0, bottom=17.0, salt=1100):
        """The body coat: orange darkening up to the spine, cream low on the flanks, white belly; stripes from the spine."""
        t = clamp01((Y - top) / (bottom - top))
        if face == "bottom" or t > 0.82:
            return lit(hair(P["white"], tx, ty, salt), face, 0.4)
        if t > 0.62:
            c = lerp(P["cream"], P["white"], (t - 0.62) / 0.2)
        else:
            c = lerp(P["back"], P["orange"], t / 0.35) if t < 0.35 else lerp(P["orange"], P["cream"], (t - 0.35) / 0.27)
        if face in ("front", "back"):
            return lit(hair(lerp(c, P["cream"], 0.5) if face == "front" else c, tx, ty, salt + 3), face, t)
        # stripes hang down from the spine, chevrons over the back: v = distance from the spine over the back and down
        v = abs(X) if face == "top" else 4.5 + (Y - top)
        u = Z + v * 0.25 + (1.4 if X < 0 else 0)
        if tiger_stripe(u, v, salt) and t < 0.78:
            return lit(hair(P["black"], tx, ty, salt + 1, 0.12), face, t)
        return lit(hair(c, tx, ty, salt + 2), face, t)

    def body(face, X, Y, Z, n, tx, ty):
        return coat(face, X, Y, Z, n, tx, ty)

    def neck(face, X, Y, Z, n, tx, ty):
        if Y > 13.0 or face == "bottom":
            return lit(hair(P["white"], tx, ty, 1110), face, 0.3)       # the white throat
        return coat(face, X, Y, Z, n, tx, ty, salt=1111)

    def limb(face, X, Y, Z, n, tx, ty, salt=1120):
        """Legs: orange outside fading to white on the inner side and the back, black rings across the outer face."""
        inner = (face == "left" and X < 0) or (face == "right" and X > 0) or face == "back"
        t = clamp01((Y - 11) / 12)
        if inner or face == "bottom":
            return lit(hair(P["white"], tx, ty, salt), face, t)
        c = lerp(P["orange"], P["cream"], t)
        if Y < 21.0 and tiger_stripe(Y + Z * 0.2, 2.0, salt, period=2.6, width=1.0):
            return lit(hair(P["black"], tx, ty, salt + 1, 0.1), face, t)
        return lit(hair(c, tx, ty, salt + 2), face, t)

    def paw(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["pad"]
        if face == "front" and Y > 23.0 and (tx % 2 == 0):
            return P["claw"]                                            # claw tips peeping out of the fur
        if face == "front" and tx % 2 == 0 and Y > 22.5:
            return shade(P["cream"], 0.75)                              # the toe partings
        return lit(hair(P["cream"] if face != "top" else P["orange"], tx, ty, 1130), face, 0.5)

    def tail(face, X, Y, Z, n, tx, ty):
        # rings round the tail by distance from its root (in the rest pose the tail hangs down and back)
        d = math.hypot(Y - 9.0, Z - 12.0)
        if d > 17.5:
            return lit(hair(P["black"], tx, ty, 1140), face, 0.3)       # the black tip
        if (d % 3.0) < 0.95:
            return lit(hair(P["black"], tx, ty, 1141), face, 0.3)
        c = P["white"] if face == "front" or (face == "bottom") else P["orange"]
        return lit(hair(c, tx, ty, 1142), face, 0.3)

    def skull(face, X, Y, Z, n, tx, ty):
        ax = abs(X)
        if face == "front":
            # eyes: amber with a black pupil, a white patch over each, black rims; the forehead's stripes
            if 1.0 <= ax < 3.0 and 8.0 <= Y < 9.0:
                return P["pupil"] if ax < 1.5 else P["eye"]             # amber, the pupil to the inside
            if 1.0 <= ax < 2.0 and 9.0 <= Y < 10.0:
                return P["black"]                                       # the dark streak under the eye
            if 1.0 <= ax < 3.0 and 7.0 <= Y < 8.0:
                return lit(hair(P["white"], tx, ty, 1150), face, 0)      # the white brows
            if Y < 7.0 and (ax < 0.5 or (1.0 < ax < 1.7 and Y < 6.6)):
                return P["black"]                                       # forehead stripes
            if ax >= 3.0 and 8.0 <= Y < 10.0:
                return P["black"] if ax < 3.6 and Y >= 9.0 else lit(hair(P["white"], tx, ty, 1151), face, 0)
            if Y >= 10.0:
                return lit(hair(P["white"] if ax > 1.0 else P["cream"], tx, ty, 1152), face, 0.3)
            return lit(hair(P["orange"], tx, ty, 1153), face, 0)
        if face in ("left", "right"):
            if Y > 10.5:
                return lit(hair(P["white"], tx, ty, 1154), face, 0.2)
            if (Z + Y * 0.6) % 2.5 < 0.8 and Y > 6.5:
                return P["black"]                                       # stripes back from the eye
            return lit(hair(P["orange"], tx, ty, 1155), face, (Y - 6) / 8)
        if face == "top":
            if (abs(X) < 0.6 and Z > -19) or ((abs(X) + Z * 0.5) % 2.4 < 0.7 and Z > -18.5):
                return P["black"]
            return lit(hair(P["back"], tx, ty, 1156), face, 0)
        if face == "bottom":
            return lit(hair(P["white"], tx, ty, 1157), face, 0)
        return lit(hair(P["orange"], tx, ty, 1158), face, 0)

    def ruff(face, X, Y, Z, n, tx, ty):
        # the white sideburns flaring off the cheeks, with two black stripes swept back
        if face in ("left", "right", "back") and (abs(Y - 9.5 - (Z + 19) * 0.3) < 0.5 or abs(Y - 11.5 - (Z + 19) * 0.3) < 0.5):
            return P["black"]                                           # two stripes swept back across the cheek
        if face == "top" or (Y < 11.0 and face != "front"):
            return lit(hair(P["orange"], tx, ty, 1160), face, 0)
        return lit(hair(P["white"], tx, ty, 1161, 0.1), face, 0.2)

    def muzzle(face, X, Y, Z, n, tx, ty):
        if face == "front" and 11.0 <= Y < 12.0 and abs(X) < 0.5:
            return P["lip"]                                             # the philtrum
        if face == "front" and 12.0 <= Y < 13.0 and 0.5 < abs(X) < 2.0:
            return P["lip"]                                             # the mouth line out to the corners
        if face == "front" and Y >= 10.0:
            return lit(hair(P["white"], tx, ty, 1170), face, 0)
        if face in ("left", "right") and Y > 10.5 and (tx + ty) % 2 == 0 and h(tx, ty, 1171) < 0.5:
            return P["black"]                                           # whisker spots
        if face == "top" or (Y < (10.0 if face == "front" else 11.0) and face != "bottom"):
            return lit(hair(P["orange"], tx, ty, 1172), face, 0)        # the bridge of the nose
        return lit(hair(P["white"], tx, ty, 1173), face, 0.2)

    def nose(face, X, Y, Z, n, tx, ty):
        return shade(P["nose"], 0.8) if face == "front" and Y > 9.6 and abs(X) < 0.5 else P["nose"]

    def jaw(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return P["mouth"] if abs(X) < 1.0 else P["tooth"]
        return lit(hair(P["white"], tx, ty, 1180), face, 0.2)       # the white chin

    def fang(face, X, Y, Z, n, tx, ty):
        return P["tooth"]

    def ear(face, X, Y, Z, n, tx, ty):
        if face == "back":
            return (238, 238, 232) if 0.0 < Y - 4.6 < 1.0 else P["black"]  # the white "false eye" spot
        if face == "front":
            return (234, 200, 180) if Y > 4.5 else lit(hair(P["white"], tx, ty, 1190), face, 0)
        return P["black"] if Y < 4.6 else lit(hair(P["orange"], tx, ty, 1191), face, 0)

    parts = [
        part("body", None, (0, 12, 0), (0, 0, 0), [
            cube("tg_chest", -4.5, -4, -11, 9, 9, 11, body),
            cube("tg_withers", -5, -4.6, -10, 10, 4, 7, body),           # the shoulder blades jutting up
            cube("tg_loin", -4, -3.6, 0, 8, 8, 9, body),
            cube("tg_rump", -4.5, -4, 7.5, 9, 8, 4, body),
            cube("tg_neck", -3.5, -4.3, -15, 7, 8, 5, neck)]),
        part("head", "body", (0, -2, -14), (0, 0, 0), [
            cube("tg_skull", -4, -4, -6, 8, 7, 6, skull),
            cube("tg_ruff", -5, -1.5, -4.5, 10, 4, 4, ruff),
            cube("tg_muzzle", -2.5, -1, -9.5, 5, 4, 4, muzzle),
            cube("tg_nose", -1.5, -1.6, -9.8, 3, 1, 1, nose)]),
        part("jaw", "head", (0, 3, -5.5), (0, 0, 0), [
            cube("tg_jaw", -2, 0, -3.8, 4, 1, 4, jaw),
            cube("tg_fang", -1.5, -0.6, -3.6, 1, 1, 1, fang),
            cube("tg_fang", 0.5, -0.6, -3.6, 1, 1, 1, fang, True)]),
        part("right_ear", "head", (-2.8, -3.8, -2.5), (0, 0, -0.3), [cube("tg_ear", -1, -2, -0.5, 2, 2, 1, ear)]),
        part("left_ear", "head", (2.8, -3.8, -2.5), (0, 0, 0.3), [cube("tg_ear", -1, -2, -0.5, 2, 2, 1, ear, True)]),
        part("tail0", "body", (0, -3, 11), (0.55, 0, 0), [cube("tg_tail0", -1.5, 0, -1.5, 3, 7, 3, tail)]),
        part("tail1", "tail0", (0, 6.8, 0), (0.45, 0, 0), [cube("tg_tail1", -1, 0, -1, 2, 7, 2, tail)]),
        part("tail2", "tail1", (0, 6.8, 0), (0.9, 0, 0), [cube("tg_tail2", -1, 0, -1, 2, 6, 2, tail)]),
    ]
    for side in (-1, 1):
        m = side > 0
        s = "left" if m else "right"
        parts += [
            part(f"{s}_front_leg", None, (side * 2.7, 13, -7), (0, 0, 0), [cube("tg_arm", -2, -2, -2.5, 4, 7, 5, lambda *a: limb(*a, salt=1121), m)]),
            part(f"{s}_front_lower", f"{s}_front_leg", (0, 5, 0), (0, 0, 0), [
                cube("tg_forearm", -1.5, 0, -1.5, 3, 5, 3, lambda *a: limb(*a, salt=1122), m),
                cube("tg_fpaw", -2, 4, -3, 4, 2, 4, paw, m)]),
            part(f"{s}_hind_leg", None, (side * 2.7, 12, 8.5), (0, 0, 0), [cube("tg_thigh", -2, -2, -3, 4, 8, 6, lambda *a: limb(*a, salt=1123), m)]),
            part(f"{s}_hind_lower", f"{s}_hind_leg", (0, 6, 1.0), (0, 0, 0), [
                cube("tg_shank", -1.5, 0, -1.5, 3, 4, 3, lambda *a: limb(*a, salt=1124), m),
                cube("tg_hpaw", -2, 4, -3, 4, 2, 4, paw, m)]),
        ]
    return parts


# ========================================================================================================== python
# A heavy constrictor about four blocks long: a broad head with a squared snout over a lower jaw, heat pits along the lip,
# a short neck and ten body segments thickest in the middle, tapering to a thin tail. Reticulated: a gold ground netted
# with black lines into saddles over the back and pale-centred eyes down the flanks; a black line down the crown, a dark
# streak back from each eye, a cream belly of broad plates.

PY = dict(gold=(190, 152, 82), tan=(128, 100, 58), net=(30, 24, 18), pale=(222, 212, 176), belly=(228, 220, 188),
          head=(170, 136, 76), eye=(206, 160, 54), pupil=(20, 16, 12), tongue=(36, 26, 34), lip=(222, 210, 168), pit=(66, 52, 34))
PY_SEGS = [(4, 3), (5, 4), (5, 4), (6, 4), (6, 5), (6, 5), (6, 4), (5, 4), (4, 4), (4, 3)]
PY_NECK_Z = -22.0


def python_snake():
    P = PY

    def scales(c, tx, ty, salt):
        # a fine scale grain: every other texel in a staggered grid a shade darker
        c = speck(c, tx, ty, salt, 0.05)
        return shade(c, 0.9) if (tx + (ty // 1) % 2) % 2 == 0 and h(tx, ty, salt + 5) < 0.6 else c

    def body(face, X, Y, Z, n, tx, ty, hh=4.0):
        if face == "bottom":
            return shade(P["belly"], 0.82) if int(Z * 1.0) % 2 == 0 else P["belly"]   # broad belly plates
        u = Z - PY_NECK_Z
        if face == "top" or face in ("front", "back"):
            q = (u % 7.0) - 3.5
            r = abs(q) * 0.85 + abs(X) * 0.9
            if 1.9 < r < 2.7:
                return scales(P["net"], tx, ty, 1201)
            return lit(scales(P["gold"] if r <= 1.9 else P["tan"], tx, ty, 1202), "top", 0)
        t = clamp01((24.0 - Y) / hh)                                            # 1 at the top of the flank
        if t < 0.2:
            return lit(scales(P["belly"], tx, ty, 1203), face, 0.8)
        q = ((u + 3.5) % 7.0) - 3.5
        r = abs(q) * 0.8 + abs(t - 0.55) * hh * 1.0
        if r < 0.45:
            return scales(P["net"], tx, ty, 1204)                               # the dark pupil of each eye-spot
        if r < 1.3:
            return lit(scales(P["pale"], tx, ty, 1205), face, 1 - t)
        if r < 2.0:
            return scales(P["net"], tx, ty, 1206)
        return lit(scales(P["gold"] if t > 0.7 else P["tan"], tx, ty, 1207), face, 1 - t)

    def head(face, X, Y, Z, n, tx, ty):
        z = Z - PY_NECK_Z                                                       # negative ahead of the neck
        if face == "bottom":
            return P["lip"]
        if face == "top":
            if abs(X) < 0.5 and z > -6:
                return P["net"]                                                 # the crown line
            return lit(scales(P["head"], tx, ty, 1210), face, 0)
        if face in ("left", "right"):
            if -3.4 < z < -2.3 and 21.6 < Y < 22.5:
                return P["pupil"] if -3.0 < z < -2.6 else P["eye"]
            if Y > 22.5:
                return P["pit"] if int(z * 1.0) % 2 == 0 and z < -2 else P["lip"]   # heat pits along the upper lip
            if 22.0 < Y < 23.0 and z > -2.3:
                return P["net"]                                                 # the dark streak back from the eye
            return lit(scales(P["head"], tx, ty, 1211), face, 0.3)
        if face == "front":
            if Y > 22.6:
                return P["lip"]
            if z < -6.5 and 0.6 < abs(X) < 1.4 and Y < 22.3:
                return P["net"]                                                 # nostrils on the snout
            return lit(scales(P["head"], tx, ty, 1212), face, 0)
        return lit(scales(P["head"], tx, ty, 1213), face, 0.3)

    def jaw(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return (176, 120, 120)
        if face in ("left", "right") and h(tx, ty, 1220) < 0.25:
            return P["pit"]
        return P["lip"]

    def tongue(face, X, Y, Z, n, tx, ty):
        z = Z - PY_NECK_Z
        if z < -9.5 and abs(X) < 0.5:
            return (0, 0, 0, 0)                                                 # the fork
        return P["tongue"]

    parts = [part("neck", None, (0, 24, PY_NECK_Z), (0, 0, 0), [cube("py_neck", -1.5, -3, 0, 3, 3, 4, lambda *a: body(*a, hh=3.0))]),
             part("head", "neck", (0, 0, 0), (0, 0, 0), [
                 cube("py_skull", -2.5, -3.5, -4, 5, 3, 4, head),
                 cube("py_snout", -2, -3.2, -7, 4, 2, 3, head)]),
             part("jaw", "head", (0, -1.0, -0.5), (0, 0, 0), [cube("py_jaw", -2, 0, -6, 4, 1, 6, jaw)]),
             part("tongue", "head", (0, -1, -6), (0, 0, 0), [cube("py_tongue", -1, 0, -4, 2, 0, 4, tongue)])]
    parent = "neck"
    for i, (w, hh) in enumerate(PY_SEGS):
        name = f"seg{i}"
        parts.append(part(name, parent, (0, 0, 4), (0, 0, 0), [cube(f"py_seg{i}", -w / 2, -hh, 0, w, hh, 4, lambda *a, hh=hh: body(*a, hh=float(hh)))]))
        parent = name
    parts.append(part("tail0", parent, (0, 0, 4), (0, 0, 0), [cube("py_tail0", -1, -2, 0, 2, 2, 4, lambda *a: body(*a, hh=2.0))]))
    parts.append(part("tail1", "tail0", (0, 0, 4), (0, 0, 0), [cube("py_tail1", -0.5, -1, 0, 1, 1, 4, lambda *a: body(*a, hh=1.0))]))
    return parts


# ================================================================================================== monitor lizard
# A water monitor: a long low body on sprawled legs with curved claws, a long neck, a narrow head with a long snout and
# nostrils near its tip, a pink forked tongue, a long tail flattened from side to side. Near-black above with bands of
# yellow eye-spots, the throat and lips cream barred black, the belly pale yellow with faint dark bands.

MN = dict(back=(58, 60, 46), back2=(78, 78, 60), spot=(196, 182, 112), spot_dark=(132, 122, 72), belly=(212, 198, 140),
          throat=(222, 210, 160), bar=(40, 38, 32), eye=(170, 120, 40), claw=(36, 30, 26), tongue=(196, 110, 140))


def monitor():
    P = MN

    def grain(c, tx, ty, salt):
        c = speck(c, tx, ty, salt, 0.07)
        return shade(c, 0.88) if (tx + ty) % 2 == 0 and h(tx, ty, salt + 2) < 0.5 else c

    def ocelli(face, X, Y, Z, n, tx, ty, salt, band=4.5):
        """Bands of yellow eye-spots across the back (rings round a dark centre), single spots down the flanks."""
        if face == "bottom" or Y > 21.4:
            c = P["belly"] if int((Z + 40) / 2.2) % 3 else shade(P["belly"], 0.8)
            return lit(grain(c, tx, ty, salt), face, 0.6)
        k = math.floor((Z + 40) / band)
        q = (Z + 40) - k * band - band / 2
        across = X if face in ("top", "front", "back") else (Y - 17) * (1 if X > 0 else -1) + 3.5 * (1 if X > 0 else -1)
        cell = across - math.floor(across / 2.2) * 2.2 - 1.1
        r = math.hypot(q * 0.9, cell)
        if abs(q) < 1.4 and h(k, math.floor(across / 2.2), salt) < 0.75:
            if r < 0.45:
                return grain(P["back"], tx, ty, salt + 1)
            if r < 1.05:
                return lit(grain(P["spot"], tx, ty, salt + 2), face, 0.2)
        return lit(grain(P["back2"] if h(tx, ty, salt + 3) < 0.3 else P["back"], tx, ty, salt + 4), face, 0.4)

    def body(face, X, Y, Z, n, tx, ty):
        return ocelli(face, X, Y, Z, n, tx, ty, 1300)

    def neck(face, X, Y, Z, n, tx, ty):
        if face == "bottom" or Y > 20.0:
            return P["bar"] if int(Z * 1.0) % 3 == 0 else grain(P["throat"], tx, ty, 1310)
        return ocelli(face, X, Y, Z, n, tx, ty, 1311, band=3.0)

    def tail(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return lit(grain(P["belly"], tx, ty, 1320), face, 0)
        k = math.floor((Z - 9) / 3.5)
        if k % 2 == 1 and Z < 26:
            return lit(grain(P["spot_dark"] if face != "top" else P["spot"], tx, ty, 1321), face, 0.3)  # yellowish bands
        return lit(grain(P["back"], tx, ty, 1322), face, 0.3)

    def head(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return grain(P["throat"], tx, ty, 1330)
        if face in ("left", "right"):
            if -18.6 < Z < -17.6 and 16.0 < Y < 17.0:
                return P["eye"] if Z > -18.2 else (20, 16, 12)
            if Y > 17.8:
                return P["bar"] if int(Z * 1.0) % 2 == 0 else P["throat"]     # barred lips
            if -23.6 < Z < -22.6 and Y < 17.0:
                return (20, 16, 12)                                             # the nostril near the snout tip
        if face == "front" and Y > 18.6:
            return P["throat"]
        if h(tx, ty, 1331) < 0.18:
            return P["spot"]                                                    # yellow speckle over the head
        return lit(grain(P["back"], tx, ty, 1332), face, 0.2)

    def jaw(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return (180, 100, 110)
        return P["bar"] if face in ("left", "right") and int(Z * 1.0) % 2 == 0 else grain(P["throat"], tx, ty, 1340)

    def tongue(face, X, Y, Z, n, tx, ty):
        return (0, 0, 0, 0) if Z < -26.0 and abs(X) < 0.5 else P["tongue"]

    def leg(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["belly"]
        return lit(grain(P["spot"] if h(tx, ty, 1350) < 0.2 else P["back"], tx, ty, 1351), face, 0.3)

    def foot(face, X, Y, Z, n, tx, ty):
        if face == "front":
            return P["claw"] if tx % 2 == 0 else (0, 0, 0, 0)                   # the curved claws
        if face == "top":
            return P["spot"] if h(tx, ty, 1361) < 0.25 else (shade(P["back2"], 0.85) if tx % 2 == 0 else P["back2"])  # toes
        return lit(grain(P["back2"], tx, ty, 1360), face, 0.3)

    parts = [
        part("body", None, (0, 19, 0), (0, 0, 0), [
            cube("mn_torso", -3.5, -2.5, -5, 7, 5, 10, body),
            cube("mn_chest", -3, -2.3, -7, 6, 4, 2, body),
            cube("mn_hips", -3, -2.3, 5, 6, 4, 2, body)]),
        part("neck", "body", (0, -0.5, -7), (0, 0, 0), [cube("mn_neck", -1.5, -1.5, -4, 3, 3, 4, neck)]),
        part("head", "neck", (0, -0.3, -4), (0, 0, 0), [
            cube("mn_skull", -2, -2, -4, 4, 3, 4, head),
            cube("mn_snout", -1.5, -1.6, -8, 3, 2, 4, head)]),
        part("jaw", "head", (0, 1, -0.5), (0, 0, 0), [cube("mn_jaw", -1.5, 0, -7, 3, 1, 7, jaw)]),
        part("tongue", "head", (0, 0.6, -8), (0, 0, 0), [cube("mn_tongue", -0.5, 0, -4, 1, 0, 4, tongue)]),
        part("tail0", "body", (0, -0.5, 7), (-0.08, 0, 0), [cube("mn_tail0", -1.5, -2, 0, 3, 4, 6, tail)]),
        part("tail1", "tail0", (0, 0.3, 6), (0.05, 0, 0), [cube("mn_tail1", -1, -1.7, 0, 2, 3, 6, tail)]),
        part("tail2", "tail1", (0, 0.2, 6), (0.03, 0, 0), [cube("mn_tail2", -0.5, -1.2, 0, 1, 2, 6, tail)]),
        part("tail3", "tail2", (0, 0.1, 6), (0.02, 0, 0), [cube("mn_tail3", -0.5, -0.6, 0, 1, 1, 5, tail)]),
    ]
    for side in (-1, 1):
        m = side > 0
        s = "left" if m else "right"
        for hind, z, key in ((False, -4.0, "f"), (True, 4.0, "h")):
            n = f"{s}_{'hind' if hind else 'front'}_leg"
            parts.append(part(n, "body", (side * 3.2, 1.0, z), (0, 0, -side * 1.25), [
                cube(f"mn_{key}upper", -1, 0, -1, 2, 4, 2, leg, m)]))
            parts.append(part(f"{s}_{'hind' if hind else 'front'}_lower", n, (0, 3.7, 0), (0, 0, side * 1.25), [
                cube(f"mn_{key}shin", -1, 0, -1, 2, 2, 2, leg, m),
                cube(f"mn_{key}foot", -1.5, 2, -3 if not hind else -3.5, 3, 1, 4 if not hind else 5, foot, m)]))
    return parts


# ======================================================================================================= crocodile
# A saltwater crocodile some five blocks long: a broad flat body armoured with rows of raised scutes, a wide skull table
# with raised eye bosses, a long heavy snout with a nostril knob, ragged teeth showing above and below the closed mouth,
# a lower jaw that gapes, sprawled legs with webbed clawed feet, and a long tail with a double crest of scutes merging
# into one. Dark olive above banded with black on the tail, yellow-grey flanks, a cream belly of square plates.

CR = dict(back=(70, 78, 46), back_dark=(38, 44, 28), scute=(96, 104, 64), side=(132, 132, 80), flank=(176, 168, 110),
          belly=(214, 204, 156), eye=(206, 176, 54), pupil=(20, 18, 10), tooth=(236, 230, 206), mouth=(196, 120, 104),
          gum=(220, 160, 140), claw=(40, 36, 30))


def crocodile():
    P = CR

    def plates(c, tx, ty, salt, cell=2):
        c = speck(c, tx, ty, salt, 0.06)
        return shade(c, 0.8) if tx % cell == 0 or ty % cell == 0 else c

    def armour(face, X, Y, Z, n, tx, ty, salt, top=15.5, bottom=21.5):
        t = clamp01((Y - top) / (bottom - top))
        if face == "bottom":
            return plates(P["belly"], tx, ty, salt)
        if face == "top":
            return lit(plates(P["back"] if h(tx // 2, ty // 2, salt) < 0.7 else P["back_dark"], tx, ty, salt), face, 0)
        c = lerp(P["back"], P["side"], t / 0.45) if t < 0.45 else lerp(P["side"], P["flank"], (t - 0.45) / 0.4)
        if t > 0.88:
            c = P["belly"]                                                      # the pale belly edge
        elif h(math.floor(Z / 2), math.floor(Y / 1.5), salt + 4) < 0.28:
            c = lerp(c, P["back_dark"], 0.7)                                    # dark blotches on the flanks
        return lit(plates(c, tx, ty, salt + 1), face, t)

    def body(face, X, Y, Z, n, tx, ty):
        return armour(face, X, Y, Z, n, tx, ty, 1400)

    def scute(face, X, Y, Z, n, tx, ty):
        return lit(speck(P["scute"] if face == "top" else P["back_dark"], tx, ty, 1401, 0.08), face, 0.2)

    def tail(face, X, Y, Z, n, tx, ty):
        c = armour(face, X, Y, Z, n, tx, ty, 1410, 15.5, 21.0)
        if face != "bottom" and (Z - 11) % 6 < 2:
            return shade(c, 0.55)                                               # black bands down the tail
        return c

    def head(face, X, Y, Z, n, tx, ty):
        return armour(face, X, Y, Z, n, tx, ty, 1420, 15.5, 19.5)

    def boss(face, X, Y, Z, n, tx, ty):
        if face in ("front", "left", "right") and Y > 14.6:
            if face == "front":
                return P["pupil"] if abs(abs(X) - 2.5) < 0.5 else P["eye"]
            return P["eye"] if (face == "left") == (X > 0) else P["back_dark"]
        return lit(plates(P["back"], tx, ty, 1430), face, 0)

    def snout(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["gum"]
        if face in ("left", "right") and Y > 18.5:
            return P["flank"]
        return armour(face, X, Y, Z, n, tx, ty, 1440, 16.0, 19.0)

    def knob(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return P["back_dark"] if abs(X) < 0.5 or abs(Z + 33.5) > 0.5 else (20, 18, 14)  # the nostrils
        return lit(plates(P["back"], tx, ty, 1450), face, 0)

    def teeth(face, X, Y, Z, n, tx, ty, up=False):
        # a ragged row: two-texel teeth with gaps, alternate ones longer
        k = int((Z + 50) * 1.0)
        row = (Y - math.floor(Y))
        if k % 2:
            return (0, 0, 0, 0)
        return P["tooth"] if (k // 2) % 2 == 0 or (row < 0.5) != up else (0, 0, 0, 0)

    def jaw(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return P["mouth"] if abs(X) < 2.2 else P["gum"]
        if face == "bottom":
            return plates(P["belly"], tx, ty, 1460)
        if face in ("left", "right") and Y < 20.0:
            return P["flank"]
        return lit(plates(P["belly"], tx, ty, 1461), face, 0.3)

    def leg(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return plates(P["belly"], tx, ty, 1470)
        return armour("left", X, 17.5, Z, n, tx, ty, 1471)

    def foot(face, X, Y, Z, n, tx, ty):
        if face == "front":
            return P["claw"] if tx % 2 == 0 else shade(P["side"], 0.7)
        if face == "bottom":
            return P["flank"]
        return lit(plates(P["side"] if face != "top" else P["back"], tx, ty, 1480), face, 0.3)

    parts = [part("body", None, (0, 18.5, 0), (0, 0, 0), [
        cube("cr_torso", -6, -3.5, -11, 12, 6, 22, body),
        cube("cr_belly", -6.5, -1, -8, 13, 3, 16, body),
        cube("cr_neck", -4.5, -3, -15, 9, 5, 4, body)])]
    body = parts[0]["cubes"]
    for z in (-14.5, -12.5):                                                    # the nuchal shield
        body += [cube("cr_scute", -2.5, -4, z, 2, 1, 2, scute), cube("cr_scute", 0.5, -4, z, 2, 1, 2, scute, True)]
    for z in range(-9, 11, 3):                                                  # dorsal rows
        for x in (-4.5, -2.2, 0.2, 2.5):
            body.append(cube("cr_scute", x, -4.4, z, 2, 1, 2, scute, x > 0))
    parts += [
        part("head", "body", (0, -1, -15), (0, 0, 0), [
            cube("cr_skull", -4, -2, -6, 8, 3, 6, head),
            cube("cr_cheek", -4.5, 0, -6, 9, 2, 5, head),
            cube("cr_boss", -3.5, -3, -4.5, 2, 1, 2, boss),
            cube("cr_boss", 1.5, -3, -4.5, 2, 1, 2, boss, True),
            cube("cr_snout", -3.5, -1.5, -13, 7, 3, 7, snout),
            cube("cr_tip", -3, -1.2, -19, 6, 2, 6, snout),
            cube("cr_knob", -1.5, -2, -19.2, 3, 1, 2, knob),
            cube("cr_uteeth", -3.1, 0.8, -19, 0, 1, 13, teeth),
            cube("cr_uteeth", 3.1, 0.8, -19, 0, 1, 13, teeth, True)]),
        part("jaw", "head", (0, 0.8, -5), (0, 0, 0), [
            cube("cr_jaw", -3, 0, -14, 6, 2, 14, jaw),
            cube("cr_lteeth", -3.05, -1, -14, 0, 1, 13, lambda *a: teeth(*a, up=True)),
            cube("cr_lteeth", 3.05, -1, -14, 0, 1, 13, lambda *a: teeth(*a, up=True), True)]),
    ]
    # the tail: five tapering segments, a double crest becoming single
    dims = [(10, 5), (8, 4), (6, 3), (4, 2), (2, 2)]
    parent, piv = "body", (0, -0.5, 11)
    for i, (w, hh) in enumerate(dims):
        cs = [cube(f"cr_tail{i}", -w / 2, -hh / 2 - 0.5, 0, w, hh, 8, tail)]
        for z in (1, 4.5):
            if i < 3:
                cs += [cube("cr_crest", -w / 2 + 0.5, -hh / 2 - 1.5, z, 1, 1, 2, scute), cube("cr_crest", w / 2 - 1.5, -hh / 2 - 1.5, z, 1, 1, 2, scute, True)]
            else:
                cs.append(cube("cr_crest", -0.5, -hh / 2 - 1.5, z, 1, 1, 2, scute))
        parts.append(part(f"tail{i}", parent, piv, (0, 0, 0), cs))
        parent, piv = f"tail{i}", (0, 0.5 if i < 2 else 0.3, 8)
    for side in (-1, 1):
        m = side > 0
        s = "left" if m else "right"
        for hind, z, key in ((False, -7.0, "f"), (True, 7.0, "h")):
            n = f"{s}_{'hind' if hind else 'front'}_leg"
            parts.append(part(n, "body", (side * 5.5, 2.3, z), (0, 0, -side * 1.25), [
                cube(f"cr_{key}upper", -1.5 if not hind else -2, 0, -1.5 if not hind else -2, 3 if not hind else 4, 4, 3 if not hind else 4, leg, m)]))
            parts.append(part(f"{s}_{'hind' if hind else 'front'}_lower", n, (0, 3.8, 0), (0, 0, side * 1.25), [
                cube(f"cr_{key}shin", -1, 0, -1, 2, 1, 2, leg, m),
                cube(f"cr_{key}foot", -2, 1, -4 if hind else -3, 4, 1, 6 if hind else 5, foot, m)]))
    return parts


# ======================================================================================================= orangutan
# A flanged Bornean male on all fours: a great hairy barrel of a body leaning forward on immensely long arms, fists to the
# ground, short bowed legs. A small high-browed face with close-set eyes and a broad lipped mouth inside wide cheek
# flanges, a hanging throat sac. Long rust-red hair hangs in a ragged fringe from the arms, flanks and belly; the face,
# flanges, palms and soles bare dark grey-brown skin.

OR = dict(hair=(172, 70, 28), hair_dark=(112, 40, 16), hair_light=(210, 108, 48), skin=(70, 56, 50), skin_light=(104, 84, 74),
          skin_dark=(44, 34, 30), eye=(36, 22, 14), white=(200, 186, 160), lip=(92, 72, 64))
OR_LEAN = 1.1


def orangutan():
    P = OR

    def shag(face, X, Y, Z, n, tx, ty, salt, ragged=True, fh=0):
        """Long coarse hair: strands down each texture column, dark roots, coppery tips, a ragged hanging lower edge."""
        col = h(tx, 0, salt)
        c = lerp(P["hair_dark"], P["hair"], 0.45 + 0.4 * col)
        if (ty + int(col * 5)) % 5 == 0:
            c = lerp(c, P["hair_light"], 0.5)
        if face == "bottom":
            c = lerp(c, P["hair_dark"], 0.35)
        return speck(c, tx, ty, salt, 0.06)

    def body(face, X, Y, Z, n, tx, ty):
        return lit(shag(face, X, Y, Z, n, tx, ty, 1510), face, 0.2)

    def head(face, X, Y, Z, n, tx, ty):
        if face == "front":
            return lit(speck(P["skin"], tx, ty, 1520), face, 0)
        return lit(shag(face, X, Y, Z, n, tx, ty, 1521), face, 0.1)

    def face_mat(face, X, Y, Z, n, tx, ty):
        x, y, z = L["head"](X, Y, Z)                                            # the face box spans y -6..-2
        ax = abs(x)
        if face == "front":
            if 0.4 < ax < 1.6 and -5.0 < y < -4.0:
                return (150, 120, 100) if 0.4 < ax < 0.8 else P["eye"]          # dark eyes, a glint
            if -6.0 < y < -5.0 and ax < 2.5:
                return P["skin_dark"]                                           # the heavy brow
            if -3.0 < y < -2.0 and 0.3 < ax < 1.2:
                return P["skin_dark"]                                           # nostrils
            return speck(P["skin_light"] if y > -4 else P["skin"], tx, ty, 1530, 0.05)
        return speck(P["skin"], tx, ty, 1531, 0.05)

    def mouth(face, X, Y, Z, n, tx, ty):
        x, y, z = L["head"](X, Y, Z)
        if face == "front" and -1.5 < y < -0.5:
            return P["skin_dark"]                                               # the wide mouth line
        return speck(P["lip"], tx, ty, 1540, 0.05)

    def flange(face, X, Y, Z, n, tx, ty):
        x, y, z = L["head"](X, Y, Z)
        # rounded: the corners cut away
        if x ** 2 / 6.3 ** 2 + (y + 4.0) ** 2 / 3.8 ** 2 > 1.0:
            return (0, 0, 0, 0)
        if face in ("front", "back"):
            return lit(speck(lerp(P["skin"], P["skin_light"], 0.4 + 0.3 * h(tx // 2, ty // 2, 1550)), tx, ty, 1551, 0.06), face, 0)
        return lit(shag(face, X, Y, Z, n, tx, ty, 1552), face, 0)

    def sac(face, X, Y, Z, n, tx, ty):
        return lit(speck(P["skin_light"] if face == "front" else P["skin"], tx, ty, 1560, 0.05), face, 0.3)

    def limb(face, X, Y, Z, n, tx, ty):
        return lit(shag(face, X, Y, Z, n, tx, ty, 1570), face, 0.2)

    def hand(face, X, Y, Z, n, tx, ty):
        if face == "front" and tx % 2 == 0:
            return P["skin_dark"]                                               # knuckles
        return speck(P["skin"], tx, ty, 1580, 0.05)

    def skirt(face, X, Y, Z, n, tx, ty):
        """The long hair hanging off the flanks and belly: a curtain, transparent below each strand's ragged end."""
        x, y, z = L["skirt"](X, Y, Z)
        if y > 4.0 - 2.5 * h(tx, 1, 1590):
            return (0, 0, 0, 0)
        return lit(shag(face, X, Y, Z, n, tx, ty, 1591), face, 0.3)

    L = {}
    parts = [part("body", None, (0, 14, 3), (OR_LEAN, 0, 0), [
        cube("or_belly", -5, -7, -5, 10, 7, 9, body),
        cube("or_chest", -5.5, -13, -4.5, 11, 7, 9, body),
        cube("or_mantle", -6.5, -14, -2, 13, 5, 7, body)])]
    # the hair skirt hanging straight down from under the belly, and the head counter-rotated upright
    parts.append(part("skirt", "body", (0, -5, 4.5), (-OR_LEAN, 0, 0), [
        cube("or_skirt", -5.5, 0, -6, 0, 4, 8, skirt),
        cube("or_skirt", 5.5, 0, -6, 0, 4, 8, skirt, True),
        cube("or_skirtb", -5.5, 0, 2, 11, 4, 0, skirt)]))
    parts.append(part("head", "body", (0, -13, -1.5), (-OR_LEAN + 0.15, 0, 0), [
        cube("or_skull", -3.5, -7, -2.5, 7, 7, 6, head),
        cube("or_crown", -3, -8, -1.5, 6, 1, 5, head),
        cube("or_flange", -6, -7.5, -3.5, 12, 7, 1, flange),
        cube("or_face", -2.5, -6, -4.5, 5, 4, 1, face_mat),
        cube("or_mouth", -2.5, -2.5, -5.5, 5, 3, 2, mouth),
        cube("or_sac", -3.5, -0.5, -4, 7, 3, 5, sac)]))
    for side in (-1, 1):
        m = side > 0
        s = "left" if m else "right"
        parts += [
            part(f"{s}_arm", "body", (side * 6.5, -11.5, 0), (-OR_LEAN - 0.15, 0, -side * 0.3), [
                cube("or_uarm", -2, -1, -2, 4, 9, 4, limb, m)]),
            part(f"{s}_forearm", f"{s}_arm", (0, 7.5, 0), (0.3, 0, side * 0.22), [
                cube("or_farm", -1.5, 0, -1.5, 3, 7, 3, limb, m),
                cube("or_cuff", -2, 0, -2, 4, 5, 4, limb, m),
                cube("or_fist", -1.5, 7, -2, 3, 2, 3, hand, m)]),
            part(f"{s}_leg", None, (side * 3.5, 15, 4.5), (0, 0, -side * 0.1), [
                cube("or_thigh", -2, -1, -2, 4, 5, 4, limb, m)]),
            part(f"{s}_shin", f"{s}_leg", (0, 4, 0), (0, 0, side * 0.1), [
                cube("or_shin", -1.5, 0, -1.5, 3, 4, 3, limb, m),
                cube("or_foot", -1.5, 4, -3, 3, 1, 5, hand, m)]),
        ]
    L["head"], L["skirt"] = local(parts, "head"), local(parts, "skirt")
    return parts


# ================================================================================================ proboscis monkey
# A pot-bellied leaf monkey on all fours: a big round belly, a reddish-brown cap and back fading to orange, cream-grey
# arms and legs, a white rump and a long thick white tail. A small pink face with close-set eyes; the male's great
# drooping nose hangs over his mouth (part "nose"), the female and young have a small upturned one ("snub").

PB = dict(cap=(150, 66, 34), back=(196, 112, 58), orange=(222, 150, 84), cream=(226, 214, 190), grey=(196, 190, 178),
          white=(240, 236, 226), face=(222, 150, 120), face_dark=(176, 100, 80), eye=(34, 22, 16), nose=(214, 120, 96))
PB_LEAN = 1.2


def proboscis():
    P = PB

    def fur(c, tx, ty, salt):
        return hair(c, tx, ty, salt, 0.07)

    def body(face, X, Y, Z, n, tx, ty):
        x, y, z = L["body"](X, Y, Z)
        if y > -2.5:
            return lit(fur(P["white"], tx, ty, 1600), face, 0.2)            # the pale rump
        if z < -3.5 or (face == "front" and z < 0):
            return lit(fur(P["cream"], tx, ty, 1601), face, 0.3)             # the pot belly
        c = lerp(P["back"], P["orange"], clamp01((2.5 - z) / 5.5))
        return lit(fur(c, tx, ty, 1602), face, clamp01((Y - 9) / 8))

    def collar(face, X, Y, Z, n, tx, ty):
        return lit(fur(lerp(P["orange"], P["cream"], 0.35), tx, ty, 1610), face, 0.1)  # the paler ruff round the neck

    def head(face, X, Y, Z, n, tx, ty):
        if face == "top" or Y < 4.4:
            return lit(fur(P["cap"], tx, ty, 1620), face, 0)
        return lit(fur(P["orange"], tx, ty, 1621), face, 0.2)

    def face_mat(face, X, Y, Z, n, tx, ty):
        if face == "front":
            ax = abs(X)
            y = L["head"](X, Y, Z)[1]
            if 0.3 < ax < 1.5 and -3.0 < y < -2.0:
                return P["eye"] if ax > 0.9 else P["face_dark"]
            if -2.0 < y < -1.0 and ax < 0.6:
                return P["face_dark"]                                           # the brow between the eyes
            return speck(P["face"], tx, ty, 1630, 0.05)
        return speck(P["face_dark"], tx, ty, 1631, 0.05)

    def nose(face, X, Y, Z, n, tx, ty):
        if face == "bottom" or (face == "front" and L["nose"](X, Y, Z)[1] > 3.0):
            return P["face_dark"] if abs(X) > 0.4 else P["nose"]
        return lit(speck(P["nose"], tx, ty, 1640, 0.05), face, 0.2)

    def limb(face, X, Y, Z, n, tx, ty):
        c = P["grey"] if Y > 13 else P["orange"]
        return lit(fur(c, tx, ty, 1650), face, 0.3)

    def hand(face, X, Y, Z, n, tx, ty):
        return speck((70, 56, 50), tx, ty, 1660, 0.05)

    def tail(face, X, Y, Z, n, tx, ty):
        return lit(fur(P["white"], tx, ty, 1670), face, 0.2)

    L = {}
    parts = [part("body", None, (0, 17, 3), (PB_LEAN, 0, 0), [
        cube("pb_belly", -4.5, -7, -6, 9, 7, 9, body),
        cube("pb_chest", -3.5, -11, -3, 7, 5, 6, body),
        cube("pb_collar", -4, -12, -2.5, 8, 2, 6, collar)]),
        part("head", "body", (0, -11.5, -0.5), (-PB_LEAN + 0.1, 0, 0), [
            cube("pb_skull", -2.5, -5, -2.5, 5, 5, 5, head),
            cube("pb_face", -2, -4, -3.5, 4, 4, 1, face_mat),
            cube("pb_jaw", -1.5, -1, -3.2, 3, 2, 1, face_mat)]),
        part("nose", "head", (0, -2.5, -3.5), (0.15, 0, 0), [
            cube("pb_nose", -1, 0, -1.5, 2, 4, 1, nose),
            cube("pb_nosetip", -1.5, 2, -2, 3, 2, 2, nose)]),
        part("snub", "head", (0, -2.5, -3.5), (0, 0, 0), [cube("pb_snub", -0.5, 0, -1, 1, 1, 1, nose)]),
        part("tail", "body", (0, 0.5, 1.5), (-2.15, 0, 0), [cube("pb_tail0", -1, -1, 0, 2, 2, 8, tail)]),
        part("tail_tip", "tail", (0, 0, 7.5), (-0.35, 0, 0), [cube("pb_tail1", -1, -1, 0, 2, 2, 8, tail)]),
    ]
    for side in (-1, 1):
        m = side > 0
        s = "left" if m else "right"
        parts += [
            part(f"{s}_arm", "body", (side * 4.3, -10, -0.5), (-PB_LEAN + 0.05, 0, -side * 0.12), [cube("pb_uarm", -1.5, -1, -1.5, 3, 5, 3, limb, m)]),
            part(f"{s}_forearm", f"{s}_arm", (0, 4, 0), (0.05, 0, side * 0.1), [
                cube("pb_farm", -1, 0, -1, 2, 5, 2, limb, m),
                cube("pb_hand", -1, 5, -1.5, 2, 1, 2, hand, m)]),
            part(f"{s}_leg", None, (side * 2.8, 17.5, 4.5), (0, 0, 0), [cube("pb_thigh", -1.5, -1, -2, 3, 4, 4, limb, m)]),
            part(f"{s}_shin", f"{s}_leg", (0, 3.5, 0), (0, 0, 0), [
                cube("pb_shin", -1, 0, -1, 2, 2, 2, limb, m),
                cube("pb_foot", -1, 2, -2, 2, 1, 3, hand, m)]),
        ]
    L["nose"], L["head"], L["body"] = local(parts, "nose"), local(parts, "head"), local(parts, "body")
    return parts


# ====================================================================================== the little ones (built x2)
# Built at twice their size in pixels (JungleFaunaClient draws them at half scale): ground at 24, as usual.

FROG_BLUE = dict(main=(52, 112, 222), light=(110, 170, 244), dark=(28, 64, 160), mark=(14, 14, 22), belly=(40, 80, 180),
                 limb=(30, 70, 190), pad=(150, 190, 250), eye=(12, 12, 16), shine=(200, 220, 255))
FROG_YELLOW = dict(main=(244, 206, 32), light=(252, 232, 110), dark=(200, 156, 20), mark=(14, 14, 18), belly=(30, 28, 30),
                   limb=(236, 196, 30), pad=(250, 236, 150), eye=(12, 12, 16), shine=(220, 220, 220))


def dart_frog(P, banded=False):
    """A dart frog sitting up: a squat body with a hunched back, a broad head with big black bulging eyes, thin front
    legs braced straight, hind legs folded in a Z, round toe pads. Blue: azure with black spots. Yellow: the
    yellow-banded kind, black with broad yellow bands round head, body and legs."""

    def blotch(X, Y, Z):
        # black spots placed by model position, so they run across boxes
        cx, cz = math.floor(X / 1.6), math.floor(Z / 1.6)
        return h(cx + 20, cz + 20 + math.floor(Y / 2) * 7, 1700) < 0.32

    def band(X, Y, Z):
        # the black bands: a mask round the eyes, a saddle across the middle of the back, the rump
        return (-6.8 < Z < -5.2 and Y < 19.5) or (-1.6 < Z < 0.2) or Z > 3.4

    def skin(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["belly"]
        if banded:
            c = P["mark"] if band(X, Y, Z) or (blotch(X, Y, Z) and h(math.floor(X), math.floor(Z), 1701) < 0.5) else P["main"]
            return lit(speck(c, tx, ty, 1702, 0.05), face, 0.2)
        if blotch(X, Y, Z):
            return P["mark"]
        c = P["light"] if face == "top" else P["main"]
        return lit(speck(c, tx, ty, 1703, 0.05), face, clamp01((Y - 16) / 8))

    def eye(face, X, Y, Z, n, tx, ty):
        if face == "top" and (tx + ty) % 3 == 0:
            return P["shine"]                                                  # the wet glint
        if face == "bottom":
            return P["main"]
        return P["eye"]

    def limb(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["belly"] if not banded else P["mark"]
        if banded:
            return P["mark"] if int((Y + Z) * 0.6) % 3 == 0 else P["main"]
        if h(math.floor(X), math.floor(Z) + math.floor(Y) * 5, 1720) < 0.3:
            return P["mark"]
        return lit(speck(P["limb"], tx, ty, 1721, 0.05), face, 0.3)

    def pad(face, X, Y, Z, n, tx, ty):
        # the toes splayed with round pads at their tips; the gaps between them cut away
        if face == "top":
            return P["pad"] if tx % 2 == 0 else (0, 0, 0, 0)
        if face in ("front", "back", "bottom"):
            return P["limb"] if tx % 2 == 0 else (0, 0, 0, 0)
        return P["limb"]

    def mouth(face, X, Y, Z, n, tx, ty):
        y = L["head"](X, Y, Z)[1]
        if face in ("front", "left", "right") and 0.5 < y < 1.5:
            return P["mark"]                                                   # the mouth line
        if face == "front" and -1.0 < y < 0.0 and 0.4 < abs(X) < 1.5:
            return P["mark"]                                                   # nostrils
        return skin(face, X, Y, Z, n, tx, ty)

    L = {}
    parts = [
        part("body", None, (0, 20.7, 0), (-0.35, 0, 0), [
            cube("df_body", -3, -3, -4, 6, 5, 8, skin),
            cube("df_hump", -2.5, -4, -0.5, 5, 1, 4, skin)]),
        part("head", "body", (0, -1, -4), (0.35, 0, 0), [
            cube("df_head", -3.5, -2.5, -5, 7, 4, 5, mouth),
            cube("df_snout", -2.5, -1.5, -6, 5, 3, 1, mouth),
            cube("df_eye", -4.3, -3.2, -4.2, 2, 2, 3, eye),
            cube("df_eye", 2.3, -3.2, -4.2, 2, 2, 3, eye, True)]),
        part("throat", "head", (0, 1.5, -3), (0, 0, 0), [cube("df_throat", -2, 0, -2, 4, 1, 3, skin)]),
    ]
    for side in (-1, 1):
        m = side > 0
        s = "left" if m else "right"
        parts += [
            part(f"{s}_front_leg", None, (side * 2.6, 20.6, -5), (-0.25, 0, -side * 0.2), [
                cube("df_arm", -0.5, 0, -0.5, 1, 3, 1, limb, m),
                cube("df_hand", -1, 2.5, -1.5, 2, 1, 2, pad, m)]),
            part(f"{s}_hind_leg", None, (side * 2.8, 21.0, 2.5), (0, -side * 0.35, 0), [
                cube("df_thigh", -1 if not m else -1, -1, -3, 2, 2, 4, limb, m),
                cube("df_shin", (-1.8 if not m else -0.2), 0.5, -4, 2, 2, 4, limb, m),
                cube("df_foot", (-2.5 if not m else -0.5), 2, -6, 3, 1, 3, pad, m)]),
        ]
    L["head"] = local(parts, "head")
    return parts


MS = dict(back=(150, 140, 104), side=(196, 184, 146), band=(104, 92, 64), spot=(160, 220, 248), belly=(236, 230, 210),
          eye=(26, 22, 18), iris=(196, 176, 120), fin=(150, 136, 110), fin_edge=(60, 150, 220), fin_spot=(236, 240, 246))


def mudskipper():
    """A mudskipper propped on its fins: a blunt head with a wide mouth, goggle eyes close together on top, a long body
    narrowing to the tail, a tall rounded first dorsal fin (blue-edged, spotted) and a long second one, a rounded tail fin,
    muscular pectoral fins it crutches on. Olive-brown with dark saddles and pale blue spangles, a pale belly."""
    P = MS

    def skin(face, X, Y, Z, n, tx, ty):
        if face == "bottom" or Y > 22.6:
            return speck(P["belly"], tx, ty, 1800, 0.04)
        if (Z + 20) % 4.5 < 1.4 and face != "front" and Y < 20.5:
            c = lerp(P["band"], P["back"], 0.35)                                # dark saddles over the back
        elif h(tx, ty, 1801) < 0.16:
            c = P["spot"]                                                       # blue spangles
        else:
            c = lerp(P["back"], P["side"], clamp01((Y - 18) / 4))
        return lit(speck(c, tx, ty, 1802, 0.06), face, clamp01((Y - 18) / 5))

    def head(face, X, Y, Z, n, tx, ty):
        if face == "front" and Y > 21.2 and Y < 22.2:
            return P["band"]                                                    # the wide mouth
        if face == "front" and Y > 22.2:
            return P["belly"]
        return skin(face, X, Y, Z, n, tx, ty)

    def eye(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return P["iris"] if (tx + ty) % 2 else (240, 236, 220)
        if face == "bottom":
            return P["side"]
        return P["eye"] if Y < 18.0 else P["iris"]                             # the black pupil over a golden ring

    def fin(face, X, Y, Z, n, tx, ty, top, z0, length, tall):
        # a rounded fin: transparent outside an arc rising from the back
        t = clamp01((Z - z0) / length)
        height = tall * math.sin(math.pi * min(1.0, t * 1.1 + 0.08)) ** 0.6
        up = top - Y
        if up > height:
            return (0, 0, 0, 0)
        if up > height - 1.0:
            return P["fin_edge"]                                                # the blue rim
        if h(tx, ty, 1810) < 0.3:
            return P["fin_spot"]                                                # white spots
        return shade(P["fin"], 0.75 if int(Z * 1.0) % 2 == 0 else 1.0)          # the fin rays

    def tailfin(face, X, Y, Z, n, tx, ty):
        r = math.hypot((Y - 20.5) * 1.0, (Z - 7.0) * 0.7)
        if r > 3.4:
            return (0, 0, 0, 0)
        return P["fin_edge"] if r > 2.7 else shade(P["fin"], 0.85 + 0.15 * (int(Y) % 2))

    def pect(face, X, Y, Z, n, tx, ty):
        return lit(speck(P["side"], tx, ty, 1820, 0.06), face, 0.3)

    def pectfin(face, X, Y, Z, n, tx, ty):
        return P["fin"] if (tx + ty) % 3 else shade(P["fin"], 0.8)

    return [
        part("body", None, (0, 21, 0), (0, 0, 0), [
            cube("ms_body", -2, -2, -2, 4, 4, 6, skin),
            cube("ms_dorsal", 0, -8, -1.5, 0, 6, 5, lambda *a: fin(*a, top=19.0, z0=-1.5, length=5.0, tall=6.0)),
            cube("ms_dorsal2", 0, -4, 3, 0, 2, 5, lambda *a: fin(*a, top=19.0, z0=3.0, length=5.0, tall=2.0))]),
        part("head", "body", (0, 0, -2), (0, 0, 0), [
            cube("ms_head", -2.5, -2, -4, 5, 4, 4, head),
            cube("ms_snout", -2, -1, -5, 4, 3, 1, head),
            cube("ms_eye", -2.3, -4.5, -3.5, 2, 3, 2, eye),
            cube("ms_eye", 0.3, -4.5, -3.5, 2, 3, 2, eye, True),
            cube("ms_cheek", -3, -0.5, -3.5, 6, 2, 3, head)]),
        part("tail", "body", (0, 0, 4), (0, 0, 0), [
            cube("ms_tail", -1.5, -1.5, 0, 3, 3, 4, skin),
            cube("ms_tailfin", 0, -3.5, 3, 0, 7, 4, tailfin)]),
        part("right_fin", "body", (-2, 1, -1), (0, 0, 0.5), [
            cube("ms_pect", -1, 0, -1, 1, 2, 2, pect),
            cube("ms_pectfin", -1, 2, -1.5, 1, 1, 3, pectfin)]),
        part("left_fin", "body", (2, 1, -1), (0, 0, -0.5), [
            cube("ms_pect", 0, 0, -1, 1, 2, 2, pect, True),
            cube("ms_pectfin", 0, 2, -1.5, 1, 1, 3, pectfin, True)]),
    ]


FC = dict(shell=(46, 120, 150), shell_light=(96, 186, 206), shell_dark=(20, 60, 80), rim=(230, 236, 230),
          claw=(236, 108, 30), claw_light=(252, 186, 90), claw_tip=(250, 244, 226), small=(206, 160, 120),
          leg=(196, 124, 80), leg_dark=(130, 70, 44), eye=(18, 16, 14), stalk=(210, 176, 130), mouth=(150, 90, 70))


def fiddler_crab():
    """A fiddler crab: a flat squarish carapace, wider at the front, teal with pale mottling; eye stalks raised from the
    front corners; four pairs of jointed walking legs; one small claw and, on the male (part "big_claw"), one huge orange
    claw - a heavy palm with a fixed finger and a hinged one. The female has a second small claw (part "female_claw")."""
    P = FC

    def shell(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["small"]
        if face == "top":
            if abs(Z + 0.5) < 0.5 and abs(X) < 3.5 and abs(X) > 0.6:
                return P["shell_dark"]                                          # the H-shaped grooves
            if abs(abs(X) - 1.2) < 0.5 and -1.5 < Z < 2.0:
                return P["shell_dark"]
            c = P["shell_light"] if h(tx // 1, ty // 1, 1900) < 0.3 else P["shell"]
            return lit(speck(c, tx, ty, 1901, 0.06), face, 0)
        if face == "front":
            return P["mouth"] if abs(X) < 1.5 and Y > 21 else lit(speck(P["shell"], tx, ty, 1902, 0.05), face, 0.5)
        return lit(speck(P["shell_dark"] if Y > 21.4 else P["shell"], tx, ty, 1903, 0.05), face, 0.5)

    def rim(face, X, Y, Z, n, tx, ty):
        return P["rim"] if face in ("top", "front") else P["shell_dark"]

    def stalk(face, X, Y, Z, n, tx, ty):
        return P["stalk"]

    def eye(face, X, Y, Z, n, tx, ty):
        return (200, 200, 190) if face == "top" and (tx + ty) % 2 == 0 else P["eye"]

    def claw(face, X, Y, Z, n, tx, ty):
        if face == "top" or face == "left" or face == "right":
            c = P["claw_light"] if Y < 19.5 else P["claw"]
            if h(tx, ty, 1910) < 0.2:
                c = shade(c, 0.85)                                              # tubercles
            return lit(speck(c, tx, ty, 1911, 0.05), face, 0.2)
        return lit(speck(P["claw"], tx, ty, 1912, 0.05), face, 0.3)

    def finger(face, X, Y, Z, n, tx, ty):
        if L["big_claw"](X, Y, Z)[2] < -11.5 or (L["dactyl"](X, Y, Z)[2] < -3.5):
            return P["claw_tip"]                                                # white tips
        if face in ("bottom", "top") and tx % 2 == 0:
            return P["claw_tip"]                                                # teeth on the biting edge
        return lit(speck(P["claw"], tx, ty, 1920, 0.05), face, 0.2)

    def small(face, X, Y, Z, n, tx, ty):
        return P["claw_tip"] if Z < -6.5 else P["small"]

    def leg(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["leg_dark"]
        return P["leg"] if (tx + ty) % 3 else P["leg_dark"]

    L = {}
    parts = [part("body", None, (0, 21.8, 0), (0, 0, 0), [
        cube("fc_shell", -5, -2, -3.5, 10, 3, 7, shell),
        cube("fc_front", -5.5, -2.2, -4.5, 11, 2, 1, shell),
        cube("fc_under", -4, 1, -3, 8, 1, 6, shell),
        cube("fc_stalk", -4.5, -5, -4.3, 1, 3, 1, stalk),
        cube("fc_stalk", 3.5, -5, -4.3, 1, 3, 1, stalk, True),
        cube("fc_eye", -5, -6.5, -4.8, 2, 2, 2, eye),
        cube("fc_eye", 3, -6.5, -4.8, 2, 2, 2, eye, True)]),
        # the male's great claw on his right: arm, a heavy palm, the fixed finger below and the hinged one above
        part("big_claw", "body", (-4.5, 0, -4), (-0.2, -0.6, 0), [
            cube("fc_arm", -1.5, -1, -3, 3, 2, 3, claw),
            cube("fc_palm", -1.5, -3, -8, 3, 4, 5, claw),
            cube("fc_fixed", -1, 0, -13, 2, 1, 5, finger)]),
        part("dactyl", "big_claw", (0, -2.5, -8), (0.1, 0, 0), [cube("fc_dactyl", -1, 0, -5, 2, 1, 5, finger)]),
        part("small_claw", "body", (4, 0.5, -4), (0, 0.9, 0), [
            cube("fc_small", -0.5, -0.5, -3, 1, 1, 3, small, True),
            cube("fc_smallp", -0.5, -1, -5, 1, 2, 2, small, True)]),
        part("female_claw", "body", (-4, 0.5, -4), (0, -0.9, 0), [
            cube("fc_small", -0.5, -0.5, -3, 1, 1, 3, small),
            cube("fc_smallp", -0.5, -1, -5, 1, 2, 2, small)]),
    ]
    for i, z in enumerate((-2.0, -0.4, 1.2, 2.8)):
        for side in (-1, 1):
            m = side > 0
            s = "left" if m else "right"
            ln = 4 if i in (1, 2) else 3
            parts.append(part(f"{s}_leg{i}", "body", (side * 4.8, 0, z), (0, side * (0.55 - i * 0.36), -side * 0.6), [
                cube(f"fc_femur{ln}", 0 if m else -ln, -0.5, -0.5, ln, 1, 1, leg, m)]))
            parts.append(part(f"{s}_foot{i}", f"{s}_leg{i}", (side * ln, 0, 0), (0, 0, side * 1.6), [
                cube("fc_dactylus", 0 if m else -5, -0.5, -0.5, 5, 1, 1, leg, m)]))
    L["big_claw"], L["dactyl"] = local(parts, "big_claw"), local(parts, "dactyl")
    return parts


# ====================================================================================================== registry

MODELS = {  # texture name -> (parts maker, texture size, the Java method)
    "tiger": (tiger, (128, 64), "tiger"),
    "python": (python_snake, (64, 64), "python"),
    "monitor_lizard": (monitor, (64, 64), "monitor"),
    "saltwater_crocodile": (crocodile, (128, 128), "crocodile"),
    "orangutan": (orangutan, (128, 64), "orangutan"),
    "proboscis_monkey": (proboscis, (64, 64), "proboscis"),
    "poison_dart_frog": (lambda: dart_frog(FROG_BLUE), (64, 32), "dartFrog"),
    "mudskipper": (mudskipper, (64, 32), "mudskipper"),
    "fiddler_crab": (fiddler_crab, (64, 32), "fiddlerCrab"),
}
VARIANTS = {  # texture name -> (base texture name, parts maker): same boxes and UVs, other paint
    "poison_dart_frog_yellow": ("poison_dart_frog", lambda: dart_frog(FROG_YELLOW, banded=True)),
}


def f(v):
    v = round(float(v), 4)
    return f"{v}F"


def java_method(method, parts, uv, size):
    lines = []
    for p in parts:
        cl = "CubeListBuilder.create()"
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            plane = ", new CubeDeformation(0.001F)" if 0 in (w, hh, d) else ""
            cl += (f"\n                .texOffs({u}, {v})" + (".mirror()" if c["mirror"] else ".mirror(false)")
                   + f".addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(hh)}, {f(d)}{plane})")
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        parent = "root" if p["parent"] is None else p["parent"]
        lines.append(f"        PartDefinition {p['name']} = {parent}.addOrReplaceChild(\"{p['name']}\", {cl},\n"
                     f"                PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)}));")
    body = "\n".join(lines)
    return f"""    static LayerDefinition {method}() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{body}
        return LayerDefinition.create(mesh, {size[0]}, {size[1]});
    }}
"""


def main():
    methods, uvs = [], {}
    for name, (make, size, method) in MODELS.items():
        parts = make()
        uv = pack(parts, *size)
        uvs[name] = (uv, size)
        methods.append(java_method(method, parts, uv, size))
        save(paint(parts, uv, size), f"wildlife/{name}.png")
        save(skinned(paint(parts, uv, size), {}, ()), f"skinned/wildlife/{name}.png")
        print(f"{name}: {len(parts)} parts, {sum(len(p['cubes']) for p in parts)} boxes")
    for name, (base, make) in VARIANTS.items():
        uv, size = uvs[base]
        img = paint(make(), uv, size)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")
    src = f"""package com.mechanical.client.wildlife.jungle;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_jungle_models.py (with the skins it paints for them): the jungle fauna's parts and boxes. Do not edit. */
@SuppressWarnings("unused")
final class JungleGeometry {{
{chr(10).join(methods)}
    private JungleGeometry() {{}}
}}
"""
    JAVA.write_text(src, encoding="utf-8")
    print("wrote", JAVA.name)


if __name__ == "__main__":
    main()
