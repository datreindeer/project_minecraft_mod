"""The desert fauna's models and skins, each from one table (com.mechanical.wildlife.desert, docs/fauna/desert.md):

- src/main/java/com/mechanical/client/wildlife/desert/DesertGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the skinned (butchered) textures/entity/skinned/wildlife/<animal>.png

Each animal is a part tree (model space: y down, ground at 24, -z the front, +x the animal's left). Every texel is painted
from the point it covers, given both in model space at the rest pose (X, Y, Z, so markings line up across boxes) and in
its part's own space (x, y, z: eyes, nostrils, toes), then lit from above (tops brighter, undersides and lower flanks
darker). Part names are what the Java models animate: CamelModel, ScorpionModel, FennecModel, BighornModel
(BovidModel), PumaModel (LeopardModel), DesertSnakeModel (SnakeModel) and the condor on ForestBirdModel (BirdModel).
A part named "leg.lower" is a part "lower" in Java (the prefix only keeps the names unique here).

Usage: python scripts/gen_desert_models.py (gen_fauna_desert_textures.py calls it). Deterministic: hashes only.
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from critter_paint import NORMALS, mv, pack, save, world  # noqa: E402
from wildlife_paint import face_texels, fur, h, lerp, shade, skinned  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/desert/DesertGeometry.java"

# ---------------------------------------------------------------- building
PARTS = []
BUILT = {}  # name -> (parts, uv, texture): for previews


def cube(key, x0, y0, z0, w, hh, d, mat, mirror=False):
    return dict(key=key, box=(x0, y0, z0, w, hh, d), mat=mat, mirror=mirror)


def part(name, parent, pivot, rot=(0, 0, 0), cubes=()):
    PARTS.append(dict(name=name, parent=parent, pivot=pivot, rot=rot, cubes=list(cubes)))
    return name


def sb(side, x0, y0, z0, w, hh, d):
    """A box given for the left side (+x); the right side's is mirrored across x."""
    return (x0 if side > 0 else -x0 - w, y0, z0, w, hh, d)


class Q:
    """One texel: face, model point X Y Z and normal n, part-local point x y z, texel tx ty, u v across the face (0..1)."""
    __slots__ = ("face", "X", "Y", "Z", "n", "x", "y", "z", "tx", "ty", "u", "v", "box")


def paint(parts, uv, size):
    W, H = size
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    seen, owner = set(), {}
    for p in parts:
        R, t = world(parts, p["name"])
        for c in p["cubes"]:
            if c["key"] in seen:
                continue
            seen.add(c["key"])
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            w, hh, d = int(w), int(hh), int(d)
            for face, tx, ty, x, y, z in face_texels(u, v, x0, y0, z0, w, hh, d):
                assert 0 <= tx < W and 0 <= ty < H, (c["key"], tx, ty)
                assert owner.setdefault((tx, ty), c["key"]) == c["key"], f"UV overlap {c['key']} / {owner[(tx, ty)]}"
                q = Q()
                q.face, q.tx, q.ty, q.x, q.y, q.z, q.box = face, tx, ty, x, y, z, c["box"]
                q.X, q.Y, q.Z = (a + b for a, b in zip(mv(R, (x, y, z)), t))
                q.n = mv(R, NORMALS[face])
                fw = {"top": w, "bottom": w, "front": w, "back": w}.get(face, d)
                fh = hh if face not in ("top", "bottom") else d
                q.u = ((x - x0) / max(1, w)) if face in ("top", "bottom", "front", "back") else ((z - z0) / max(1, d))
                q.v = ((y - y0) / max(1, hh)) if face not in ("top", "bottom") else ((z - z0) / max(1, d))
                col = c["mat"](q)
                if col is not None:
                    px[tx, ty] = col if len(col) == 4 else (*col, 255)
    return img


def lit(mat, top, bottom, k=1.0):
    """Light from above: up-facing texels brighter, down-facing darker, a gradient from the back down to the belly."""
    def m(q):
        c = mat(q)
        if c is None or (len(c) == 4 and c[3] == 0):
            return c
        t = max(0.0, min(1.0, (q.Y - top) / max(1e-6, bottom - top)))
        f = 1.0 + k * (0.05 - 0.13 * q.n[1] - 0.15 * t)
        return shade(c, f) if len(c) == 3 else (*shade(c, f), c[3])
    return m


def near(a, b, r=0.5):
    return abs(a - b) < r


def finish(name, size, keep=(), skin=True):
    """Pack, paint, save the texture (and its skinned version, boxes in keep copied over unchanged); returns the parts."""
    parts = list(PARTS)
    PARTS.clear()
    uv = pack(parts, *size)
    img = paint(parts, uv, size)
    BUILT[name] = (parts, uv, img)
    save(img, f"wildlife/{name}.png")
    if skin:
        sk = skinned(img, {}, ())
        src, dst = img.load(), sk.load()
        for p in parts:
            for c in p["cubes"]:
                if c["key"] in keep:
                    u, v = uv[c["key"]]
                    _, _, _, w, hh, d = c["box"]
                    for tx in range(u, u + 2 * (math.ceil(w) + math.ceil(d))):
                        for ty in range(v, v + math.ceil(d) + math.ceil(hh)):
                            dst[tx, ty] = src[tx, ty]
        save(sk, f"skinned/wildlife/{name}.png")
    return parts, uv, size


# ======================================================================= camel (dromedary, 128x128)
CM = dict(coat=(198, 156, 104), light=(224, 194, 146), dark=(150, 110, 68), hump=(172, 126, 80), shag=(128, 90, 56),
          belly=(230, 210, 172), callus=(108, 90, 72), pad=(120, 106, 92), nail=(52, 44, 38), muzzle=(218, 190, 148),
          nose=(46, 34, 26), eye=(18, 14, 12), lash=(56, 40, 28), ear_in=(140, 104, 76), strap=(112, 66, 38),
          strap_dark=(70, 40, 22), weave=(200, 164, 100), weave_dark=(138, 102, 56), blanket=(156, 50, 40),
          blanket_b=(224, 184, 96), blanket_d=(60, 40, 70))


def camel_mat(kind):
    P = CM

    def mat(q):
        r = h(q.tx, q.ty, 900)
        if kind == "strap":
            return shade(P["strap"] if (q.tx + q.ty) % 5 else P["strap_dark"], 0.92 + 0.12 * r)
        if kind == "basket":
            if q.face == "top":
                return shade(P["weave_dark"], 0.6)
            if q.v < 0.12 or q.v > 0.88:
                return shade(P["strap"], 0.9 + 0.1 * r)                     # rims bound with leather
            c = P["weave"] if (q.tx + q.ty) % 2 else lerp(P["weave"], P["weave_dark"], 0.5)
            return shade(c, 0.9 + 0.14 * r)
        if kind == "blanket":
            band = int(q.Z + 40) % 4
            c = P["blanket"] if band < 2 else P["blanket_b"] if band == 2 else P["blanket_d"]
            if q.face != "top" and q.v > 0.75:
                c = P["blanket_b"] if q.tx % 2 else P["blanket"]            # the tasselled hem
            return shade(c, 0.88 + 0.14 * r)
        if kind == "pad":
            if q.face == "front" and near(q.x, 0, 0.6):
                return P["nail"]                                           # the cleft between the two toes
            if q.face == "front" and q.v < 0.5:
                return lerp(P["nail"], P["pad"], 0.35)                     # the toenails over the toes
            if q.face == "top":
                return fur(lerp(P["pad"], P["belly"], 0.3), q.tx, q.ty, 911, 0.08)
            return shade(P["pad"], 0.86 + 0.16 * r)
        if kind == "callus":
            if q.face == "front":
                return shade(P["callus"], 0.85 + 0.25 * r)                 # the bare knee callus
            return fur(lerp(P["coat"], P["belly"], 0.5), q.tx, q.ty, 912, 0.06)
        if kind == "brisket":
            return shade(P["callus"], 0.85 + 0.2 * r)
        if kind in ("leg", "cannon"):
            c = lerp(P["coat"], P["belly"], 0.3 if kind == "leg" else 0.55)
            if kind == "leg" and q.face in ("back", "left") and q.v > 0.4:
                c = lerp(c, P["belly"], 0.3)
            return fur(c, q.tx, q.ty, 901, 0.06)
        if kind == "tail":
            return fur(lerp(P["dark"], P["coat"], 0.3), q.tx, q.ty, 902, 0.08)
        if kind == "tuft":
            return fur(P["shag"] if q.ty % 2 else lerp(P["shag"], P["nose"], 0.45), q.tx, q.ty, 903, 0.1)
        if kind == "ear":
            return P["ear_in"] if q.face == "front" else fur(P["dark"], q.tx, q.ty, 904, 0.06)
        if kind == "hump":
            # longer, darker hair over the hump and the withers, fading into the coat
            t = max(0.0, min(1.0, (-q.Y - 6) / 7))
            c = lerp(P["coat"], P["hump"], t)
            if q.face == "top" or (q.ty + int(r * 3)) % 3 == 0:
                c = lerp(c, P["shag"], 0.3 * t + 0.1)
            return fur(c, q.tx, q.ty, 905, 0.1)
        if kind in ("neck1", "neck2"):
            throat = q.n[1] > 0.35 if kind == "neck1" else q.n[2] < -0.6
            c = lerp(P["coat"], P["light"], 0.25)
            if throat:  # the shaggy throat hair
                c = P["shag"] if (q.tx + q.ty // 2) % 3 == 0 else lerp(P["coat"], P["shag"], 0.35)
            elif q.face == "top" and kind == "neck1":
                c = lerp(c, P["hump"], 0.3)
            return fur(c, q.tx, q.ty, 906, 0.08)
        if kind == "skull":
            if q.face in ("left", "right"):
                if near(q.y, -2.0) and near(q.z, -3.5):
                    return P["eye"]                                        # the big dark eye
                if near(q.y, -2.0) and near(q.z, -2.5):
                    return lerp(P["eye"], P["coat"], 0.5)
                if near(q.y, -1.0) and near(q.z, -3.5):
                    return lerp(P["coat"], P["light"], 0.6)               # pale rim below the eye
            if q.face == "top":
                return fur(lerp(P["coat"], P["hump"], 0.3), q.tx, q.ty, 913, 0.08)
            return fur(lerp(P["coat"], P["light"], 0.2), q.tx, q.ty, 907, 0.06)
        if kind == "brow":
            if q.face == "bottom" or (q.face in ("left", "right") and q.v > 0.5):
                return P["lash"]                                           # the long lashes over the eye
            return fur(lerp(P["coat"], P["dark"], 0.2), q.tx, q.ty, 914, 0.06)
        if kind in ("muzzle", "lip", "jaw"):
            c = lerp(P["muzzle"], P["coat"], 0.35 * r)
            if kind == "muzzle":
                if q.face == "front" and near(abs(q.x), 1.2, 0.5) and near(q.y, -1.5, 0.6):
                    return P["nose"]                                       # the slit nostrils
                if q.face in ("left", "right") and q.z < -9.5 and near(q.y, -1.5, 0.6):
                    return lerp(P["nose"], P["muzzle"], 0.3)
                if q.face == "top" and q.z > -7:
                    c = lerp(c, P["coat"], 0.4)
            if kind == "lip":
                if q.face == "front" and near(q.x, 0, 0.5):
                    return P["nose"]                                       # the split upper lip
                if q.face == "bottom" or (q.face != "top" and q.v > 0.7):
                    return lerp(P["nose"], P["muzzle"], 0.45)
            if kind == "jaw":
                if q.face == "top":
                    return P["nose"]
                if q.face in ("left", "right", "front") and q.v < 0.3:
                    return lerp(P["nose"], P["muzzle"], 0.5)                # the mouth line
                if q.face == "bottom":
                    c = lerp(c, P["shag"], 0.3)
            return fur(c, q.tx, q.ty, 908, 0.05)
        if kind == "chest":
            c = lerp(P["coat"], P["light"], 0.25)
            if q.face == "front":
                c = P["shag"] if (q.tx + q.ty // 2) % 3 == 0 else lerp(P["coat"], P["shag"], 0.3)
            return fur(c, q.tx, q.ty, 909, 0.07)
        # body: tan, creamier down the flanks to the belly
        t = max(0.0, min(1.0, (q.Y + 6) / 12))
        c = lerp(P["coat"], P["belly"], 0.65 * t * t)
        if q.face == "bottom":
            c = P["belly"]
        if q.face == "top":
            c = lerp(c, P["hump"], 0.2)
        return fur(c, q.tx, q.ty, 910, 0.07)

    return mat


def camel():
    M = lambda k: lit(camel_mat(k), -14, 9)  # noqa: E731
    part("body", None, (0, 0, 0), (0, 0, 0), [
        cube("cm_barrel", -5, -6, -10, 10, 12, 20, M("body")),
        cube("cm_chest", -4.5, -7, -14, 9, 13, 5, M("chest")),
        cube("cm_brisket", -2, 6, -13, 4, 2, 4, M("brisket")),
        cube("cm_withers", -3.5, -9, -13, 7, 3, 6, M("hump")),
        cube("cm_rump", -4.5, -7, 9, 9, 10, 5, M("body")),
        cube("cm_hump1", -4, -9, -7, 8, 3, 15, M("hump")),
        cube("cm_hump2", -3.5, -11, -5, 7, 2, 11, M("hump")),
        cube("cm_hump3", -2.5, -13, -3, 5, 2, 7, M("hump")),
        cube("cm_hump4", -1.5, -14, -1.5, 3, 1, 3, M("hump")),
    ])
    part("tail", "body", (0, -5, 13.5), (0.3, 0, 0), [
        cube("cm_tail", -1, 0, -0.5, 2, 9, 1, M("tail")),
        cube("cm_tuft", -1.5, 8, -1, 3, 4, 2, M("tuft")),
    ])
    part("packs", "body", (0, 0, 0), (0, 0, 0), [
        cube("cm_blanket", -4.5, -12.5, -4.5, 9, 4, 11, M("blanket")),
        cube("cm_girth", -5.5, -9, 0, 11, 15, 2, M("strap")),
    ])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        part(f"{s}_basket", "packs", (side * 5.5, -6, 0.5), (0, 0, side * -0.06), [
            cube("cm_basket", *sb(side, 0, 0, -4.5, 3, 8, 9), M("basket"), side > 0),
        ])
    # the neck: forward and a little down off the chest, deep at the base, then a crook and up to the head
    part("neck", "body", (0, -3, -13), (1.75, 0, 0), [
        cube("cm_neck", -2.5, -10, -3, 5, 11, 7, M("neck1")),
        cube("cm_crook", -2.5, -11.5, -2.5, 5, 3, 6, M("neck1")),
    ])
    part("neck2", "neck", (0, -10, 0.5), (-1.6, 0, 0), [
        cube("cm_neck2", -2, -13, -2.5, 4, 14, 5, M("neck2")),
    ])
    part("head", "neck2", (0, -12.5, 0), (0.15, 0, 0), [
        cube("cm_skull", -2.5, -3.5, -5, 5, 5, 6, M("skull")),
        cube("cm_brow", -3, -3.5, -4.5, 6, 1, 2, M("brow")),
        cube("cm_muzzle", -2, -2.5, -11, 4, 4, 6, M("muzzle")),
        cube("cm_lip", -2, 0.5, -12, 4, 2, 3, M("lip")),
        cube("cm_jaw", -1.5, 1.5, -10, 3, 2, 6, M("jaw")),
    ])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        part(f"{s}_ear", "head", (side * 2, -3.5, -0.5), (0, 0, side * 0.4), [cube("cm_ear", -0.5, -2, -1, 1, 2, 2, M("ear"), side > 0)])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        m = side > 0
        part(f"{s}_front_leg", None, (side * 3, 5, -10.5), (0, 0, 0), [
            cube("cm_forearm_top", -2, -3, -2, 4, 6, 4, M("leg"), m),
            cube("cm_forearm", -1.5, 3, -1.5, 3, 7, 3, M("leg"), m),
        ])
        part(f"{s}_front_leg.lower", f"{s}_front_leg", (0, 10, 0), (0, 0, 0), [
            cube("cm_knee", -1.5, 0, -2, 3, 2, 3, M("callus"), m),
            cube("cm_cannon", -1, 2, -1, 2, 5, 2, M("cannon"), m),
            cube("cm_pad", -2, 7, -3, 4, 2, 5, M("pad"), m),
        ])
        part(f"{s}_hind_leg", None, (side * 3, 4, 9.5), (0, 0, 0), [
            cube("cm_thigh", -2, -4, -3, 4, 9, 6, M("leg"), m),
            cube("cm_gaskin", -1.5, 4, -1.5, 3, 7, 3, M("leg"), m),
        ])
        part(f"{s}_hind_leg.lower", f"{s}_hind_leg", (0, 11, 0.5), (0, 0, 0), [
            cube("cm_hock", -1.5, 0, -1, 3, 2, 3, M("callus"), m),
            cube("cm_cannon", -1, 2, -1, 2, 5, 2, M("cannon"), m),
            cube("cm_pad", -2, 7, -3, 4, 2, 5, M("pad"), m),
        ])
    return finish("camel", (128, 128))


# ======================================================================= desert scorpion (64x64)
SC = dict(plate=(226, 190, 108), plate_dark=(168, 124, 58), seam=(118, 84, 40), belly=(238, 214, 150), leg=(236, 208, 140),
          joint=(178, 140, 74), claw=(214, 168, 84), tip=(70, 44, 24), vesicle=(176, 112, 44), barb=(28, 18, 12),
          eye=(16, 12, 10))
LEG_Z = -0.25
LEG_Y = (0.5, 0.17, -0.16, -0.49)


def scorpion_mat(kind):
    P = SC

    def mat(q):
        r = h(q.tx, q.ty, 700)
        if kind == "barb":
            return shade(P["barb"], 1.0 + 0.4 * (1 - q.v))
        if kind == "vesicle":
            c = lerp(P["vesicle"], P["barb"], 0.4 * max(0.0, q.z - 1.5))
            return shade(c, 0.9 + 0.15 * r)
        if kind in ("femur", "shin"):
            c = P["leg"]
            along = q.u if q.face in ("top", "bottom", "front", "back") else q.v
            if (kind == "femur" and q.x * 0 == 0 and (abs(q.x) < 0.6)) or along > 0.85:
                c = P["joint"]                                             # the darker joints
            if kind == "shin" and abs(q.x) > 2.2:
                c = lerp(P["joint"], P["tip"], 0.4)                        # the claw-tipped feet
            return shade(c, 0.88 + 0.18 * r)
        if kind in ("arm", "hand", "finger"):
            c = P["claw"]
            if kind == "finger":
                c = lerp(P["claw"], P["tip"], max(0.0, min(1.0, (-q.z - 4.0) / 1.5)))
            elif kind == "hand" and q.face in ("top", "left", "right") and (q.tx + q.ty) % 3 == 0:
                c = lerp(c, P["plate_dark"], 0.45)                         # the keeled, granular hand
            elif kind == "arm" and abs(q.z) > 2.5:
                c = P["joint"]
            return shade(c, 0.88 + 0.18 * r)
        if q.face == "bottom":
            return shade(P["belly"], 0.92 + 0.1 * r)
        if kind == "carapace":
            if q.face == "front" and near(abs(q.x), 1.9, 0.6) and q.v < 0.5:
                return P["eye"]                                            # the lateral eyes at the front corners
            if q.face == "top" and q.z < -2.3:
                return lerp(P["plate"], P["plate_dark"], 0.5)
            c = P["plate"] if (q.tx * 3 + q.ty) % 5 else lerp(P["plate"], P["plate_dark"], 0.35)  # granules
            return shade(c, 0.92 + 0.14 * r)
        if kind == "mound":
            return P["eye"] if q.face in ("left", "right", "front") else P["plate_dark"]
        if kind == "chelicera":
            return shade(lerp(P["plate"], P["tip"], 0.5), 0.9 + 0.2 * r)
        if kind == "tergite":
            c = P["plate"]
            if q.face in ("top", "left", "right", "back") and (q.z - q.box[2]) > 1.3:
                c = P["seam"]                                              # the dark hind edge of each plate
            elif q.face == "top" and abs(q.x) < 0.6:
                c = lerp(c, P["plate_dark"], 0.6)                          # the keel
            elif q.face == "top" and near(abs(q.x), 2.0, 0.6):
                c = lerp(c, P["plate_dark"], 0.35)
            return shade(c, 0.9 + 0.15 * r)
        if kind == "segment":
            c = P["plate"]
            if (q.z - q.box[2]) > 2.2:
                c = P["seam"]
            if q.face in ("top", "left", "right") and near(abs(q.x), abs(q.box[0]) - 0.5, 0.5) and q.face == "top":
                c = lerp(c, P["plate_dark"], 0.5)                          # the keels along the tail
            return shade(c, 0.9 + 0.15 * r)
        return shade(lerp(P["plate"], P["plate_dark"], 0.3), 0.9 + 0.15 * r)   # mesosoma under the plates

    return mat


def scorpion():
    M = lambda k: lit(scorpion_mat(k), 18, 24, 0.7)  # noqa: E731
    plates = []
    for k, (z, w) in enumerate(((-3.6, 6), (-1.85, 6), (-0.1, 6), (1.65, 5))):
        plates.append(cube(f"sc_tergite{k}", -w / 2, -1.5 + 0.1 * (k % 2), z, w, 1, 2, M("tergite")))
    part("body", None, (0, 21.5, 0), (0, 0, 0), [cube("sc_meso", -2.5, -1, -3.5, 5, 2, 7, M("meso")), *plates])
    part("head", "body", (0, 0, -3.5), (0, 0, 0), [
        cube("sc_carapace", -2.5, -1.5, -3, 5, 2, 3, M("carapace")),
        cube("sc_mound", -0.5, -2, -2.5, 1, 1, 1, M("mound")),
        cube("sc_chelicera", -1, -0.5, -4, 2, 1, 1, M("chelicera")),
    ])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        m = side > 0
        part(f"{s}_arm", "head", (side * 1.8, 0, -2.5), (0, side * 0.6, 0), [
            cube("sc_arm", -0.5, -0.5, -3, 1, 1, 3, M("arm"), m),
        ])
        part(f"{s}_claw", f"{s}_arm", (0, 0, -3), (0, side * -0.9, 0), [
            cube("sc_hand", -1.5, -1, -3, 3, 2, 3, M("hand"), m),
            cube("sc_finger_a", *sb(side, -1.3, -0.5, -5.5, 1, 1, 3), M("finger"), m),
            cube("sc_finger_b", *sb(side, 0.3, -0.5, -5, 1, 1, 2), M("finger"), m),
        ])
        for k, z in enumerate((-2.6, -1.0, 0.6, 2.2)):
            leg = part(f"{s}_leg{k}", "body", (side * 2.5, 0.2, z), (0, side * LEG_Y[k], side * LEG_Z), [
                cube("sc_femur", *sb(side, 0, -0.5, -0.5, 2, 1, 1), M("femur"), m),
            ])
            part(f"{leg}.shin", leg, (side * 2.0, 0, 0), (0, 0, side * 1.6), [
                cube("sc_shin", *sb(side, 0, -0.5, -0.5, 3, 1, 1), M("shin"), m),
            ])
    prev = "body"
    for k, (w, rot) in enumerate(((3, 0.75), (3, 0.75), (2, 0.75), (2, 0.7))):
        name = f"tail{k + 1}"
        part(name, prev, (0, -0.5 if k == 0 else 0, 3.4 if k == 0 else 2.7), (rot, 0, 0), [
            cube(f"sc_tail{k + 1}", -w / 2, -1, 0, w, 2, 3, M("segment")),
        ])
        prev = name
    part("sting", "tail4", (0, 0, 2.7), (0.9, 0, 0), [cube("sc_vesicle", -1, -1, 0, 2, 2, 2, M("vesicle"))])
    part("sting.barb", "sting", (0, -0.2, 1.7), (1.0, 0, 0), [cube("sc_barb", -0.5, -0.5, 0, 1, 1, 2, M("barb"))])
    return finish("desert_scorpion", (64, 64))


# ======================================================================= fennec fox (64x64)
FX = dict(coat=(226, 194, 142), back=(206, 168, 116), white=(246, 240, 228), cream=(240, 224, 196), ear_in=(226, 186, 166),
          ear_rim=(196, 156, 108), nose=(26, 20, 18), eye=(20, 16, 14), tear=(150, 106, 70), tip=(34, 26, 22),
          gland=(120, 84, 56), paw=(222, 196, 156))
FENNEC_TAIL_X = -0.9


def fennec_mat(kind):
    P = FX

    def mat(q):
        r = h(q.tx, q.ty, 600)
        if kind == "ear":
            if q.face == "front":  # the inside: pink skin, long white hairs along the rims
                rim = q.u < 0.25 or q.u > 0.75
                return fur(P["white"] if rim else P["ear_in"], q.tx, q.ty, 601, 0.05)
            if q.face == "back":
                return fur(lerp(P["back"], P["ear_rim"], 0.4), q.tx, q.ty, 602, 0.06)
            return fur(P["ear_rim"], q.tx, q.ty, 603, 0.06)
        if kind == "nose":
            return P["nose"]
        if kind == "muzzle":
            if q.face in ("left", "right") and near(q.y, -0.5, 0.5) and q.z > -4.5:
                return P["tear"]                                           # the dark tear line down the muzzle
            if q.face == "bottom" or q.v > 0.5:
                return fur(P["white"], q.tx, q.ty, 604, 0.04)
            return fur(P["cream"], q.tx, q.ty, 605, 0.05)
        if kind == "skull":
            if q.face in ("left", "right") and near(q.y, -1.0) and near(q.z, -2.5):
                return P["eye"]                                            # big dark eyes
            if q.face in ("left", "right") and near(q.y, 0.0) and near(q.z, -2.5):
                return P["tear"]
            if q.face == "front" and near(abs(q.x), 1.5) and near(q.y, -1.0):
                return P["eye"]
            if q.face == "front" and near(abs(q.x), 1.5) and near(q.y, 0.0):
                return P["tear"]
            if q.face == "front" and q.v > 0.5:
                return fur(P["white"], q.tx, q.ty, 606, 0.04)
            return fur(P["coat"] if q.face != "top" else P["back"], q.tx, q.ty, 607, 0.06)
        if kind == "cheek":
            return fur(P["white"], q.tx, q.ty, 608, 0.05)
        if kind == "chest":
            return fur(P["white"] if q.face in ("front", "bottom") else P["cream"], q.tx, q.ty, 609, 0.05)
        if kind in ("tail", "tail_tip"):
            t = q.z - q.box[2]
            if kind == "tail_tip" and t > 2.5:
                return fur(P["tip"], q.tx, q.ty, 610, 0.1)                 # the black tip
            if kind == "tail" and q.face == "top" and near(t, 1.5, 0.8) and abs(q.x) < 0.6:
                return P["gland"]                                          # the dark spot over the tail gland
            c = P["back"] if q.face == "top" else P["coat"]
            if q.face == "bottom":
                c = P["cream"]
            return fur(c, q.tx, q.ty, 611, 0.1)
        if kind in ("leg", "thigh"):
            c = P["coat"] if q.face in ("front", "right", "left") else P["cream"]
            if kind == "leg" and q.v > 0.8:
                c = P["paw"]
            return fur(c, q.tx, q.ty, 612, 0.06)
        # body: buff back, cream flanks, white belly
        t = max(0.0, min(1.0, (q.Y - 15) / 4))
        c = lerp(P["back"], P["coat"], min(1.0, t * 2))
        if t > 0.7:
            c = lerp(c, P["cream"], (t - 0.7) / 0.3)
        if q.face == "bottom":
            c = P["white"]
        return fur(c, q.tx, q.ty, 613, 0.07)

    return mat


def fennec():
    M = lambda k: lit(fennec_mat(k), 13, 20, 0.8)  # noqa: E731
    part("body", None, (0, 17, 0), (0, 0, 0), [
        cube("fx_torso", -2, -2, -4, 4, 4, 8, M("body")),
        cube("fx_chest", -1.5, -1.5, -5, 3, 4, 2, M("chest")),
        cube("fx_haunch", -2.5, -1.8, 1.5, 5, 3, 3, M("body")),
    ])
    part("head", "body", (0, -1, -4.5), (0, 0, 0), [
        cube("fx_skull", -2, -2.5, -3, 4, 3, 3, M("skull")),
        cube("fx_cheek", -2.5, -0.5, -2.5, 5, 2, 2, M("cheek")),
        cube("fx_muzzle", -1, -1, -5, 2, 2, 2, M("muzzle")),
        cube("fx_nose", -0.5, -1.2, -5.4, 1, 1, 1, M("nose")),
    ])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        part(f"{s}_ear", "head", (side * 1.3, -2.3, -1.3), (0, 0, side * 0.3), [
            cube("fx_ear", -2, -4, -0.5, 4, 4, 1, M("ear"), side > 0),
            cube("fx_ear_mid", -1.5, -6, -0.5, 3, 2, 1, M("ear"), side > 0),
            cube("fx_ear_tip", -0.5, -7, -0.5, 1, 1, 1, M("ear"), side > 0),
        ])
    part("tail", "body", (0, -1, 4), (FENNEC_TAIL_X, 0, 0), [cube("fx_tail", -1.5, -1.5, 0, 3, 3, 5, M("tail"))])
    part("tail_tip", "tail", (0, 0, 4.5), (0.35, 0, 0), [cube("fx_tail_tip", -1.5, -1.5, 0, 3, 3, 4, M("tail_tip"))])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        m = side > 0
        part(f"{s}_front_leg", None, (side * 1.1, 19, -3.2), (0, 0, 0), [
            cube("fx_arm", -1, -2, -1, 2, 3, 2, M("thigh"), m),
            cube("fx_leg", -0.5, 0, -0.5, 1, 5, 1, M("leg"), m),
        ])
        part(f"{s}_hind_leg", None, (side * 1.1, 19, 3.2), (0, 0, 0), [
            cube("fx_thigh", -1, -2, -1.5, 2, 4, 3, M("thigh"), m),
            cube("fx_leg", -0.5, 0, -0.5, 1, 5, 1, M("leg"), m),
        ])
    return finish("fennec_fox", (64, 64))


# ======================================================================= bighorn sheep (ram / ewe / lamb, 128x64)
BH = dict(back=(108, 78, 52), coat=(132, 98, 66), light=(160, 126, 90), belly=(234, 226, 210), dark=(66, 48, 34),
          face=(122, 92, 64), muzzle=(226, 218, 200), horn=(176, 152, 110), horn_dark=(128, 108, 76), horn_tip=(208, 192, 156),
          hoof=(44, 40, 38), eye=(196, 150, 60), pupil=(18, 14, 12), nose=(48, 40, 36))
BH_VARIANTS = {
    "bighorn_ram": {},
    "bighorn_ewe": dict(back=(124, 92, 62), coat=(148, 112, 78), face=(138, 106, 74), horn=(150, 132, 100), horn_dark=(118, 100, 74)),
    "bighorn_lamb": dict(back=(146, 116, 86), coat=(168, 136, 102), light=(186, 158, 124), face=(160, 128, 96)),
}


def bighorn_mat(kind, P):
    def mat(q):
        r = h(q.tx, q.ty, 820)
        if kind in ("horn", "ewe_horn"):
            # heavy, ridged with growth rings across the horn, paler and broomed at the worn tips
            seg = q.box[4]
            ring = (q.ty + q.tx // 3) % 3 == 0 if q.face in ("front", "back", "top", "bottom") else (q.ty % 3 == 0)
            c = lerp(P["horn"], P["horn_tip"], 0.15 * (1 - q.v))
            if ring:
                c = lerp(c, P["horn_dark"], 0.6)
            return shade(c, 0.88 + 0.16 * r + 0.0 * seg)
        if kind == "horn_tip":
            return shade(lerp(P["horn_tip"], P["horn"], 0.3 * q.v), 0.9 + 0.12 * r)
        if kind == "hoof":
            return shade(P["hoof"], 0.85 + 0.3 * r) if not (q.face == "front" and near(q.x, 0, 0.5)) else (20, 18, 18)
        if kind == "ear":
            return fur(lerp(P["belly"], (190, 150, 130), 0.4) if q.face == "bottom" else P["coat"], q.tx, q.ty, 821, 0.06)
        if kind == "tail":
            return fur(P["dark"] if q.face in ("top", "back") else P["belly"], q.tx, q.ty, 822, 0.06)
        if kind == "skull":
            if q.face in ("left", "right") and near(q.y, -1.5) and near(q.z, -2.5, 0.6):
                return P["pupil"] if near(q.z, -2.5, 0.25) or True and q.tx % 2 == 0 else P["eye"]
            if q.face in ("left", "right") and near(q.y, -1.5) and near(q.z, -1.5):
                return P["eye"]
            if q.face == "front" and q.v > 0.6:
                return fur(P["muzzle"], q.tx, q.ty, 823, 0.05)
            return fur(P["face"], q.tx, q.ty, 824, 0.06)
        if kind == "muzzle":
            if q.face == "front" and near(abs(q.x), 1.0) and near(q.y, -0.5, 0.6):
                return P["nose"]
            if q.face == "front" and near(q.x, 0, 0.5) and q.v > 0.6:
                return P["nose"]
            if q.face == "top" or (q.face in ("left", "right") and q.v < 0.5 and q.box[4] > 1):
                return fur(lerp(P["face"], P["muzzle"], 0.15), q.tx, q.ty, 825, 0.06)
            return fur(P["muzzle"], q.tx, q.ty, 826, 0.05)
        if kind == "neck":
            c = P["coat"] if q.n[2] > -0.5 else lerp(P["coat"], P["dark"], 0.25)
            if q.face == "top":
                c = P["back"]
            return fur(c, q.tx, q.ty, 827, 0.07)
        if kind in ("upper", "thigh", "cannon"):
            # dark brown down the front of the legs, white down the back
            front = q.n[2] < -0.5 or (q.face in ("left", "right") and q.u < 0.4 and kind == "cannon")
            back = q.n[2] > 0.5
            c = P["dark"] if front else P["belly"] if back and kind != "thigh" else P["coat"]
            if kind == "thigh" and (q.n[2] > 0.5 or (q.face in ("left", "right") and q.u > 0.6)):
                c = P["belly"]                                             # the white rump runs down the thighs
            return fur(c, q.tx, q.ty, 828, 0.07)
        # body: brown back, white belly and the big white rump patch round a dark tail
        t = max(0.0, min(1.0, (q.Y - 6) / 10))
        c = lerp(P["back"], P["light"], t)
        if q.Z > 6.0 and (q.face == "back" or (q.face in ("left", "right") and t > 0.2)):
            c = P["belly"]
        if q.face == "bottom" or (t > 0.85 and q.face in ("left", "right") and abs(q.Z) < 6):
            c = P["belly"]
        return fur(c, q.tx, q.ty, 829, 0.07)

    return mat


def bighorn_parts(P):
    M = lambda k: lit(bighorn_mat(k, P), 4, 22)  # noqa: E731
    part("body", None, (0, 11, 0), (0, 0, 0), [
        cube("bh_barrel", -4.5, -4, -7, 9, 9, 14, M("body")),
        cube("bh_chest", -4, -4.5, -10, 8, 10, 4, M("body")),
        cube("bh_shoulder", -4, -5, -9, 8, 2, 8, M("body")),
        cube("bh_rump", -4, -4.5, 6.5, 8, 8, 3, M("body")),
    ])
    part("tail", "body", (0, -3.5, 9.3), (0.3, 0, 0), [cube("bh_tail", -1, 0, -0.5, 2, 3, 1, M("tail"))])
    part("neck", "body", (0, -2.5, -8.5), (0.5, 0, 0), [
        cube("bh_neck", -2.5, -6, -3, 5, 7, 6, M("neck")),
        cube("bh_dewlap", -2, -3, 2.5, 4, 4, 1, M("neck")),
    ])
    part("head", "neck", (0, -5.5, -0.5), (-0.15, 0, 0), [
        cube("bh_skull", -2.5, -3, -4.5, 5, 5, 5, M("skull")),
        cube("bh_muzzle", -2, -2, -8, 4, 3, 4, M("muzzle")),
        cube("bh_jaw", -1.5, 1, -7.5, 3, 1, 4, M("muzzle")),
    ])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        part(f"{s}_ear", "head", (side * 2.5, -2.3, -0.5), (0, side * 0.3, side * -0.3), [
            cube("bh_ear", *sb(side, 0, -0.5, -1, 3, 1, 2), M("ear"), side > 0),
        ])
    part("horns", "head", (0, 0, 0))
    part("small_horns", "head", (0, 0, 0))
    # the ram's curl: up and back off the poll, down behind the ear, forward under it and up past the cheek; thick at
    # the base, thinning, the whole spiral flaring outward so the tips stand clear of the face
    curl = ((4, 3, -1.3), (4, 3, -1.05), (4, 3, -1.05), (3, 3, -1.0), (3, 2, -1.0), (2, 2, -0.9))
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        # the boss at the poll, out to the side; the curl turns about the head's own x axis from its end
        parent = part(f"{s}_horn", "horns", (side * 1.0, -2.6, -0.6), (0, 0, 0), [
            cube("bh_horn_base", *sb(side, 0, -1.5, -1.5, 3, 3, 3), M("horn"), side > 0)])
        for k, (ln, th, rx) in enumerate(curl):
            name = f"{s}_horn_{k + 2}"
            pivot = (side * 2.6, 0, 0) if k == 0 else (0, -(curl[k - 1][0] - 0.7), 0)
            key = "bh_horn_tip" if k == len(curl) - 1 else f"bh_horn{k}"
            part(name, parent, pivot, (rx, side * -0.1, side * 0.22), [cube(key, -th / 2, -ln, -th / 2, th, ln, th, M("horn_tip" if k == len(curl) - 1 else "horn"), side > 0)])
            parent = name
        # the ewe's: short and only a little curved back
        e = part(f"{s}_ewe_horn", "small_horns", (side * 1.3, -2.6, -2.0), (-0.45, 0, side * 0.2), [
            cube("bh_ewe_horn", -1, -3, -1, 2, 3, 2, M("ewe_horn"), side > 0)])
        part(f"{s}_ewe_horn_tip", e, (0, -2.6, 0), (-0.6, 0, 0), [cube("bh_ewe_tip", -0.5, -3, -0.5, 1, 3, 1, M("horn_tip"), side > 0)])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        m = side > 0
        part(f"{s}_front_leg", None, (side * 2.5, 15, -6.5), (0, 0, 0), [
            cube("bh_forearm", -2, -2, -2, 4, 4, 4, M("upper"), m),
            cube("bh_upper", -1.5, 2, -1.5, 3, 3, 3, M("upper"), m),
        ])
        part(f"{s}_front_leg.lower", f"{s}_front_leg", (0, 4.5, 0), (0, 0, 0), [
            cube("bh_cannon", -1, 0, -1, 2, 4, 2, M("cannon"), m),
            cube("bh_hoof", -1.5, 3.5, -1.5, 3, 1, 3, M("hoof"), m),
        ])
        part(f"{s}_hind_leg", None, (side * 2.5, 14, 6), (0, 0, 0), [
            cube("bh_thigh", -2, -3, -2.5, 4, 8, 5, M("thigh"), m),
        ])
        part(f"{s}_hind_leg.lower", f"{s}_hind_leg", (0, 5.5, 0.5), (0, 0, 0), [
            cube("bh_cannon", -1, 0, -1, 2, 4, 2, M("cannon"), m),
            cube("bh_hoof", -1.5, 3.5, -1.5, 3, 1, 3, M("hoof"), m),
        ])


def bighorn():
    out = None
    for v, over in BH_VARIANTS.items():
        bighorn_parts(dict(BH, **over))
        keep = ("bh_hoof", "bh_horn_tip", "bh_horn_base", "bh_ewe_horn", "bh_ewe_tip") + tuple(f"bh_horn{k}" for k in range(6))
        out = finish(v, (128, 64), keep, skin=v != "bighorn_lamb")
    return out


# ======================================================================= puma (LeopardModel's part tree, 128x64)
PM = dict(back=(160, 112, 68), coat=(186, 138, 88), flank=(204, 160, 110), cream=(236, 222, 196), white=(246, 240, 230),
          dark=(58, 44, 36), tip=(46, 34, 28), nose=(170, 104, 92), eye=(176, 142, 62), pupil=(16, 14, 12), ear_in=(210, 180, 160),
          paw=(150, 112, 76), claw=(230, 222, 204))
TAIL_REST = (0.5, 0.5, 1.0)


def puma_mat(kind):
    P = PM

    def mat(q):
        r = h(q.tx, q.ty, 760)
        if kind == "ear":
            if q.face == "back":
                return fur(P["dark"], q.tx, q.ty, 761, 0.06) if q.v < 0.7 else fur(P["coat"], q.tx, q.ty, 762)
            if q.face == "front":
                return fur(P["ear_in"], q.tx, q.ty, 763, 0.05)
            return fur(P["back"], q.tx, q.ty, 764)
        if kind == "nose":  # the bridge of the nose, the pink nose leather at its tip
            if q.face == "front":
                return P["nose"]
            if q.face == "top" and q.z < -5.6:
                return lerp(P["nose"], P["coat"], 0.3)
            return fur(lerp(P["coat"], P["back"], 0.3), q.tx, q.ty, 765)
        if kind == "muzzle":  # white whisker pads, a black spot at each whisker base, the dark lip line
            if q.face in ("left", "right") and near(q.y, -0.5, 0.5) and near(q.z, -5.0, 0.5):
                return P["dark"]
            if q.face != "top" and q.v > 0.75:
                return lerp(P["dark"], P["white"], 0.4)
            if q.face == "top":
                return fur(lerp(P["coat"], P["white"], 0.3), q.tx, q.ty, 766)
            return fur(P["white"], q.tx, q.ty, 767, 0.04)
        if kind == "jaw":
            return fur(P["white"] if q.face != "top" else P["dark"], q.tx, q.ty, 768, 0.04)
        if kind == "skull":
            if q.face in ("left", "right") and near(q.y, -2.5) and near(q.z, -4.0):
                return P["eye"]
            if q.face == "front":
                if near(q.y, -2.5) and near(abs(q.x), 1.5):
                    return P["eye"]                                        # golden eyes
                if near(q.y, -1.5) and near(abs(q.x), 1.5):
                    return P["cream"]                                      # pale patch under each eye
                if near(q.y, -2.5) and near(abs(q.x), 0.5):
                    return lerp(P["dark"], P["coat"], 0.5)                 # the dark inner corners
                if near(q.y, -3.5) and near(abs(q.x), 0.5):
                    return lerp(P["dark"], P["coat"], 0.55)                # the dark spots over the eyes
                if q.v > 0.75:
                    return fur(P["white"], q.tx, q.ty, 779, 0.04)
            if q.face in ("left", "right") and q.v > 0.6 and q.z < -2.5:
                return fur(P["cream"], q.tx, q.ty, 780, 0.04)
            return fur(P["coat"] if q.face != "top" else P["back"], q.tx, q.ty, 772)
        if kind == "throat":
            return fur(P["cream"] if q.face in ("bottom", "front") else P["coat"], q.tx, q.ty, 773)
        if kind.startswith("tail"):
            if kind == "tail2" and q.v > 0.55:
                return fur(P["tip"], q.tx, q.ty, 774, 0.08)                # the dark tail tip
            under = q.face in ("front",)
            return fur(lerp(P["coat"], P["cream"], 0.5) if under else P["coat"], q.tx, q.ty, 775)
        if kind in ("upper", "thigh", "lower"):
            c = P["coat"] if q.face not in ("back", "left") or kind == "thigh" else lerp(P["coat"], P["cream"], 0.4)
            if kind == "thigh" and q.face == "back":
                c = lerp(P["coat"], P["cream"], 0.35)
            return fur(c, q.tx, q.ty, 776)
        if kind == "paw":
            if q.face == "front" and q.tx % 2 == 0:
                return lerp(P["paw"], P["claw"], 0.3)                      # toes
            return fur(lerp(P["coat"], P["paw"], 0.4), q.tx, q.ty, 777)
        # body: tawny, darker along the spine, pale belly
        t = max(0.0, min(1.0, (q.Y - 8.5) / 9))
        c = lerp(P["back"], P["flank"], min(1.0, t * 1.6))
        if t > 0.8 or q.face == "bottom":
            c = P["cream"]
        return fur(c, q.tx, q.ty, 778, 0.06)

    return mat


def puma():
    M = lambda k: lit(puma_mat(k), 8, 24, 0.9)  # noqa: E731
    part("body", None, (0, 13, 0), (0, 0, 0), [
        cube("pm_chest", -3.5, -4, -9, 7, 8, 9, M("body")),
        cube("pm_loin", -3, -3.5, -0.5, 6, 6, 7, M("body")),
        cube("pm_haunch", -3.5, -4.2, 5, 7, 7, 5, M("body")),
        cube("pm_shoulder", -4, -3.5, -8, 8, 6, 5, M("body")),
        cube("pm_neck", -2.5, -3.5, -12, 5, 6, 4, M("body")),
        cube("pm_throat", -2, -0.5, -12.5, 4, 3, 4, M("throat")),
    ])
    part("tail0", "body", (0, -2.5, 9.5), (TAIL_REST[0], 0, 0), [cube("pm_tail0", -1, 0, -1, 2, 8, 2, M("tail0"))])
    part("tail1", "tail0", (0, 7.8, 0), (TAIL_REST[1], 0, 0), [cube("pm_tail1", -1, 0, -1, 2, 7, 2, M("tail1"))])
    part("tail2", "tail1", (0, 6.8, 0), (TAIL_REST[2], 0, 0), [cube("pm_tail2", -1, 0, -1, 2, 6, 2, M("tail2"))])
    part("head", "body", (0, -2.5, -11.5), (0, 0, 0), [
        cube("pm_skull", -3, -4, -4.5, 6, 5, 5, M("skull")),
        cube("pm_muzzle", -2, -1.5, -6.5, 4, 2, 2, M("muzzle")),
        cube("pm_nose", -1, -2, -6.8, 2, 1, 3, M("nose")),
    ])
    part("jaw", "head", (0, 0.5, -4.5), (0, 0, 0), [cube("pm_jaw", -1.5, 0, -2, 3, 1, 2, M("jaw"))])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        part(f"{s}_ear", "head", (side * 2.1, -3.8, -1.5), (0, 0, side * 0.3), [cube("pm_ear", -1, -2, -0.5, 2, 2, 1, M("ear"), side > 0)])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        m = side > 0
        part(f"{s}_front_leg", None, (side * 2.2, 14, -6), (0, 0, 0), [cube("pm_upper", -1.5, -2, -2, 3, 6, 4, M("upper"), m)])
        part(f"{s}_front_leg.lower", f"{s}_front_leg", (0, 4, 0), (0, 0, 0), [
            cube("pm_lower", -1, 0, -1, 2, 5, 2, M("lower"), m),
            cube("pm_paw", -1.5, 4, -2.5, 3, 2, 4, M("paw"), m),
        ])
        part(f"{s}_hind_leg", None, (side * 2.2, 13, 7.5), (0, 0, 0), [cube("pm_thigh", -2, -3, -2.5, 4, 8, 5, M("thigh"), m)])
        part(f"{s}_hind_leg.lower", f"{s}_hind_leg", (0, 5, 1), (0, 0, 0), [
            cube("pm_lower", -1, 0, -1, 2, 5, 2, M("lower"), m),
            cube("pm_paw", -1.5, 4, -2.5, 3, 2, 4, M("paw"), m),
        ])
    return finish("puma", (128, 64))


# ======================================================================= snakes (SnakeModel's chain, 64x64)
SNAKE_PIVOT_Y = 22.5


def snake_parts(name, M, segs, head, extra_tail=()):
    """segs: [(part, width, height, depth, offset)] from the neck back to the tail; the belly of every segment on the ground."""
    first = segs[0]
    part("neck", None, (0, SNAKE_PIVOT_Y, -10), (0, 0, 0), [cube(f"{name}_neck", -first[1] / 2, 1.5 - first[2], 0, first[1], first[2], first[3], M("body"))])
    part("head", "neck", (0, 0, 0), (0, 0, 0), head)
    part("tongue", "head", (0, 0, 0), (0, 0, 0), [cube(f"{name}_tongue", -0.5, 1.0, -6.5, 1, 0, 2, M("tongue"))])
    parent = "neck"
    for k, (pname, w, hh, d, off) in enumerate(segs[1:]):
        cubes = [cube(f"{name}_{pname}", -w / 2, 1.5 - hh, 0, w, hh, d, M("tail" if pname == "tail" else "body"))]
        if pname == "tail":
            cubes += list(extra_tail)
        part(pname, parent, (0, 0, off), (0, 0, 0), cubes)
        parent = pname


def scales(c, q, salt):
    k = (q.tx + q.ty) % 2
    return shade(c, (0.92 if k else 1.04) + 0.06 * (h(q.tx, q.ty, salt) - 0.5))


RS = dict(ground=(156, 140, 112), side=(140, 124, 100), diamond=(66, 52, 40), edge=(226, 214, 178), belly=(218, 206, 174),
          eye=(196, 156, 70), pupil=(20, 16, 14), lip=(220, 208, 182), dark=(30, 26, 24), light=(228, 222, 206),
          rattle=(208, 186, 142), rattle_dark=(150, 128, 92), tongue=(40, 30, 34), mask=(80, 64, 50))
SV = dict(ground=(226, 204, 158), side=(212, 188, 140), saddle=(170, 136, 92), belly=(240, 228, 198), eye=(200, 160, 80),
          pupil=(20, 16, 14), lip=(238, 226, 200), streak=(118, 90, 60), horn=(196, 166, 116), tongue=(40, 30, 34))


def snake_mat(kind, P, rattler, Z0):
    def mat(q):
        if kind == "tongue":
            fork = q.z < -5.6 and q.tx % 2 == 0
            return (0, 0, 0, 0) if fork else P["tongue"]
        if kind == "horn":
            return shade(P["horn"], 0.9 + 0.2 * h(q.tx, q.ty, 790) - 0.15 * q.v * 0)
        if kind == "rattle":
            ring = (q.z - q.box[2]) > 0.6 if q.face in ("left", "right", "top", "bottom") else False
            return shade(P["rattle_dark"] if ring else P["rattle"], 1.0 + 0.1 * (h(q.tx, q.ty, 791) - 0.5))
        s = q.Z - Z0                                                       # distance back from the snout
        if q.face == "bottom":
            return scales(P["belly"], q, 780)
        if kind in ("head", "snout", "brow"):
            side = q.face in ("left", "right")
            if side and kind == "head" and near(q.y, -1.0, 0.5) and near(q.z, -3.5, 0.5):
                return P["eye"]
            if side and kind == "head" and near(q.y, -1.0, 0.5) and q.z > -3.0:
                return P["mask"] if rattler else P["streak"]               # the dark band back from the eye
            if side and q.y > 0.0:
                return scales(P["lip"], q, 781)                            # pale lip scales
            if rattler and side and near(q.y, -0.25, 0.25):
                return P["edge"]                                           # the pale line from the eye to the jaw
            if kind == "brow" and not rattler:
                return scales(lerp(P["ground"], P["saddle"], 0.3), q, 782)
            if q.face == "top" and kind == "head" and rattler and near(q.z, -1.5, 0.6) and abs(q.x) < 0.6:
                return P["diamond"]
            return scales(P["ground"] if q.face == "top" else P["side"], q, 783)
        up = q.face == "top" or q.n[1] < -0.7
        if rattler:
            if kind == "tail" or s > 25.0:                                 # the coon tail: black and white rings
                ring = int(s * 1.0) % 2 == 0
                return P["dark"] if ring else P["light"]
            k = abs(((s / 3.0) % 2) - 1)                                   # diamond half-width along the body
            if up:
                d = abs(q.x) - (1.0 - k) * 2.0
                if d < -0.3:
                    return scales(P["diamond"], q, 784)
                if d < 0.4:
                    return P["edge"]
                return scales(P["ground"], q, 785)
            spot = abs((((s / 3.0) + 1) % 2) - 1) < 0.3 and q.y < 0.8
            return scales(lerp(P["side"], P["diamond"], 0.55) if spot else P["side"], q, 786)
        saddle = (s / 2.6) % 2 < 0.8
        if up:
            return scales(lerp(P["ground"], P["saddle"], 0.55) if saddle else P["ground"], q, 787)
        return scales(lerp(P["side"], P["saddle"], 0.3) if saddle and q.y < 1.0 else P["side"], q, 788)

    return mat


def rattlesnake():
    Z0 = -15.5
    M = lambda k: lit(snake_mat(k, RS, True, Z0), 20.5, 24, 0.7)  # noqa: E731
    # the broad arrow head: wide jowls, narrowing to a blunt snout, a plate over each eye
    head = [cube("rs_jowl", -2.5, -1.5, -2.5, 5, 2, 3, M("head")),
            cube("rs_mid", -2, -1.5, -4.5, 4, 2, 2, M("head")),
            cube("rs_snout", -1.5, -1.2, -5.5, 3, 2, 1, M("snout")),
            cube("rs_brow", -2.2, -1.9, -4, 1, 1, 2, M("brow")),
            cube("rs_brow", 1.2, -1.9, -4, 1, 1, 2, M("brow"), True)]
    rattle = [cube("rs_rattle1", -1, -0.8, 3.5, 2, 2, 1, M("rattle")),
              cube("rs_rattle2", -1, -0.8, 4.5, 2, 2, 1, M("rattle")),
              cube("rs_rattle3", -0.5, -0.5, 5.5, 1, 1, 1, M("rattle"))]
    snake_parts("rs", M, [("neck", 3, 2, 4, 0), ("seg2", 4, 3, 4, 3), ("seg3", 5, 3, 4, 3), ("seg4", 4, 3, 4, 3),
                          ("seg5", 4, 3, 4, 3), ("seg6", 3, 2, 4, 3), ("tail", 2, 2, 4, 3)], head, rattle)
    return finish("rattlesnake", (64, 64))


def sand_viper():
    Z0 = -14.5
    M = lambda k: lit(snake_mat(k, SV, False, Z0), 21, 24, 0.6)  # noqa: E731
    head = [cube("sv_jowl", -2, -1.3, -2, 4, 2, 2, M("head")),
            cube("sv_mid", -1.5, -1.3, -3.5, 3, 2, 2, M("head")),
            cube("sv_snout", -1, -1.1, -4.5, 2, 2, 1, M("snout")),
            cube("sv_horn", -1.5, -3.0, -3.0, 1, 2, 1, M("horn")),
            cube("sv_horn", 0.5, -3.0, -3.0, 1, 2, 1, M("horn"), True)]
    snake_parts("sv", M, [("neck", 3, 2, 3, 0), ("seg2", 3, 2, 3, 2.5), ("seg3", 4, 3, 3, 2.5), ("seg4", 4, 3, 3, 2.5),
                          ("seg5", 3, 2, 3, 2.5), ("seg6", 2, 2, 3, 2.5), ("tail", 1, 1, 3, 2.5)], head)
    return finish("sand_viper", (64, 64))


# ======================================================================= condor (the flock birds' part tree, 128x64)
CD = dict(black=(26, 24, 28), black_l=(48, 46, 52), sheen=(64, 66, 78), white=(252, 252, 250), white_d=(226, 226, 222),
          skin=(168, 96, 86), skin_d=(116, 82, 80), comb=(122, 70, 66), beak=(222, 212, 186), beak_base=(70, 62, 58),
          eye=(150, 40, 30), leg=(130, 126, 122), claw=(40, 36, 34))
CONDOR_TILT = 0.5


def condor_mat(kind):
    P = CD

    def mat(q):
        r = h(q.tx, q.ty, 800)
        if kind == "ruff":
            # the fluffy white collar, downy and uneven at the rim
            c = P["white"] if (q.tx + q.ty * 2) % 5 else P["white_d"]
            if q.face != "top" and q.face != "bottom" and q.v > 0.75 and r < 0.4:
                c = P["black_l"]
            return shade(c, 0.94 + 0.08 * r)
        if kind == "neck":
            return shade(lerp(P["skin"], P["skin_d"], 0.5 + 0.4 * r), 1.0)   # bare, wrinkled, grey-red
        if kind == "head":
            if q.face in ("left", "right") and near(q.y, -1.5) and near(q.z, -1.5):
                return P["eye"]
            if q.face in ("left", "right") and q.v > 0.6 and (q.tx + q.ty) % 3 == 0:
                return P["skin_d"]                                         # folds of bare skin
            return shade(lerp(P["skin"], P["skin_d"], 0.35 * r), 1.0)
        if kind in ("comb", "wattle"):
            return shade(P["comb"], 0.85 + 0.25 * r)
        if kind == "beak":
            return P["beak"] if q.z < -5.5 or q.face == "front" else lerp(P["beak_base"], P["beak"], 0.4)
        if kind == "beak_base":
            return shade(P["beak_base"], 0.9 + 0.2 * r)
        if kind in ("leg", "foot"):
            if kind == "foot" and q.face == "front":
                return P["claw"] if q.tx % 2 == 0 else P["leg"]
            return shade(P["leg"], 0.85 + 0.2 * r) if (q.ty % 2 or kind == "foot") else shade(P["leg"], 0.75)
        if kind == "body":
            if q.face == "top" or (q.face in ("left", "right") and q.v < 0.3):
                c = lerp(P["black"], P["sheen"], 0.25 * r)                 # glossy back
            else:
                c = lerp(P["black"], P["black_l"], 0.4 * r)
            return c
        if kind == "tail":
            return lerp(P["black"], P["black_l"], 0.3 * r) if q.tx % 2 else P["black"]
        if kind == "wing":
            # the upper wing's big white panel (the secondaries' upper sides): on the folded wing it shows on the flank
            outer = q.face in ("left", "right")
            panel = outer and 0.3 < q.u < 0.92 and 0.3 < q.v < 0.85
            if panel:
                c = P["white"] if (q.ty + q.tx // 2) % 4 else P["white_d"]
                return shade(c, 0.95 + 0.06 * r)
            c = lerp(P["black"], P["black_l"], 0.35 * r)
            if outer and (q.tx + q.ty) % 4 == 0:
                c = lerp(c, P["sheen"], 0.4)                               # feather edges
            return c
        if kind in ("tip", "finger"):
            c = lerp(P["black"], P["black_l"], 0.3 * r)
            if kind == "tip" and q.face in ("left", "right") and q.tx % 3 == 0:
                c = lerp(c, P["sheen"], 0.35)                              # the long primaries
            return c
        return P["black"]

    return mat


def condor():
    M = lambda k: lit(condor_mat(k), 0, 24, 0.6)  # noqa: E731
    part("body", None, (0, 13.2, 0), (-CONDOR_TILT, 0, 0), [
        cube("cd_body", -4.5, -4.5, -7, 9, 9, 14, M("body")),
        cube("cd_mantle", -4, -5.5, -5, 8, 1, 9, M("body")),
        cube("cd_breast", -3.5, -3.5, -8, 7, 7, 1, M("body")),
    ])
    part("neck", "body", (0, -3.5, -6), (CONDOR_TILT, 0, 0), [
        cube("cd_ruff", -4, -1.5, -4, 8, 3, 8, M("ruff")),
        cube("cd_neck", -1.5, -6, -1.5, 3, 5, 3, M("neck")),
    ])
    part("head", "neck", (0, -5.5, 0), (0, 0, 0), [
        cube("cd_head", -2, -3, -3, 4, 3, 4, M("head")),
        cube("cd_comb", -0.5, -4, -4, 1, 1, 3, M("comb")),
        cube("cd_beak_base", -1.5, -2.5, -5, 3, 2, 2, M("beak_base")),
        cube("cd_beak", -1, -2.5, -7, 2, 2, 2, M("beak")),
        cube("cd_hook", -0.5, -0.5, -7, 1, 1, 1, M("beak")),
        cube("cd_wattle", -1, 0, -2.5, 2, 1, 2, M("wattle")),
    ])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        m = side > 0
        w = part(f"{s}_wing", "body", (side * 4.5, -4.5, -6.5), (0, 0, 0), [cube("cd_wing", *sb(side, 0, 0, 0, 1, 9, 14), M("wing"), m)])
        fingers = [cube(f"cd_finger{k}", *sb(side, 0, 14, z, 1, ln, 1), M("finger"), m)
                   for k, (z, ln) in enumerate(((0, 4), (2, 5), (4, 5), (6, 4)))]
        t = part(f"{s}_wing_tip", w, (side * 0.5, 9, 0), (1.5708, 0, 0), [cube("cd_tip", *sb(side, -1, 0, 0, 1, 14, 8), M("tip"), m)])
        part(f"{t}.fingers", t, (0, 0, 0), (0, 0, 0), fingers)  # the spread primaries, shown in flight (CondorModel)
    part("tail", "body", (0, -3.5, 6.5), (-0.15, 0, 0), [cube("cd_tail", -3, 0, 0, 6, 1, 7, M("tail"))])
    for side in (-1, 1):
        s = "left" if side > 0 else "right"
        part(f"{s}_leg", None, (side * 2.0, 18, 0.5), (0, 0, 0), [
            cube("cd_leg", -1, 0, -1, 2, 5, 2, M("leg"), side > 0),
            cube("cd_foot", -1.5, 5, -3, 3, 1, 4, M("foot"), side > 0),
        ])
    return finish("condor", (128, 64), skin=False)


# ======================================================================= the Java geometry
def java_layer(method, model):
    parts, uv, size = model
    f = lambda v: f"{float(v):.4f}".rstrip("0").rstrip(".") + ("" if "." in f"{float(v):.4f}".rstrip("0").rstrip(".") else ".0") + "F"  # noqa: E731
    var = {None: "root"}
    lines = []
    for i, p in enumerate(parts):
        var[p["name"]] = f"p{i}"
        jname = p["name"].split(".")[-1]
        cl = "CubeListBuilder.create()"
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            cl += (f"\n                .texOffs({u}, {v})" + (".mirror()" if c["mirror"] else ".mirror(false)")
                   + f".addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(hh)}, {f(d)})")
        px_, py_, pz_ = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = f"PartPose.offsetAndRotation({f(px_)}, {f(py_)}, {f(pz_)}, {f(rx)}, {f(ry)}, {f(rz)})"
        lines.append(f"        PartDefinition p{i} = {var[p['parent']]}.addOrReplaceChild(\"{jname}\", {cl},\n                {pose});")
    return (f"    static LayerDefinition {method}() {{\n        MeshDefinition mesh = new MeshDefinition();\n"
            f"        PartDefinition root = mesh.getRoot();\n" + "\n".join(lines)
            + f"\n        return LayerDefinition.create(mesh, {size[0]}, {size[1]});\n    }}\n")


def main():
    layers = [("camel", camel()), ("scorpion", scorpion()), ("fennec", fennec()), ("bighorn", bighorn()), ("puma", puma()),
              ("rattlesnake", rattlesnake()), ("sandViper", sand_viper()), ("condor", condor())]
    body = "\n".join(java_layer(m, model) for m, model in layers)
    JAVA.write_text(f"""package com.mechanical.client.wildlife.desert;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_desert_models.py (with the skins painted for it): the desert fauna's parts and boxes. Do not edit. */
public final class DesertGeometry {{
{body}
    private DesertGeometry() {{}}
}}
""", encoding="utf-8")
    print("desert models ok")


if __name__ == "__main__":
    main()
