"""Paint the coast fauna (wildlife/coast): Seal (+ white pup), Walrus (+ calf), Pike and Sturgeon entity textures, the
skinned (flesh) versions for the carcasses (seal, walrus, sturgeon), the sealskin and walrus hide on the tall Drying Rack,
the Sealskin and Walrus Hide armour layers, the Oil Lamp block, and every 16x16 item texture of the group.

Usage: python scripts/gen_fauna_coast_textures.py [--java]
The part trees mirror SealModel / WalrusModel / PikeModel / SturgeonModel (client/wildlife/coast); --java prints the cube
lines (texOffs from pack()) to keep the Java models in step. Deterministic: hashes only, no randomness.
Looks: grey seal-ish harbour seal, dark grey back with black blotches and pale rings, cream-grey belly, big black eyes;
pup in a white lanugo coat. Walrus: cinnamon-pink wrinkled skin with sparse hair, a bristly whisker pad and ivory tusks;
calf darker grey-brown. Pike: olive back, green flanks with rows of pale bean-shaped spots, white belly, rust fins with
dark mottles, a golden eye, duck-bill jaws. Sturgeon: olive-grey back, pale belly, rows of white bony scutes (back, flanks
and belly edge), a long pointed snout with four barbels and a shark-like tail.
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from critter_paint import cube, fur, h, java_cubes, lerp, pack, paint, part, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import OUT, egg, from_grid, hexc  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEXROOT = ROOT / "src/main/resources/assets/mechanical/textures"
CLEAR = (0, 0, 0, 0)


def blot(X, Z, salt, scale=2.2, thresh=0.72):
    """Irregular dark blotch field in model space (value noise on a coarse grid, smoothed)."""
    gx, gz = X / scale, Z / scale
    x0, z0 = math.floor(gx), math.floor(gz)
    fx, fz = smooth(gx - x0), smooth(gz - z0)
    v = lambda a, b: h(a, b, salt)
    top = v(x0, z0) * (1 - fx) + v(x0 + 1, z0) * fx
    bot = v(x0, z0 + 1) * (1 - fx) + v(x0 + 1, z0 + 1) * fx
    return top * (1 - fz) + bot * fz > thresh


# ======================================================================= seal
SE = dict(back=(78, 78, 76), flank=(122, 120, 112), belly=(198, 192, 176), spot=(40, 40, 42), ring=(168, 164, 152),
          eye=(10, 9, 10), glint=(220, 224, 230), nose=(30, 28, 30), whisker=(232, 226, 210), lip=(150, 144, 134),
          claw=(36, 32, 30), flipper=(66, 64, 62))
PUP = dict(back=(226, 222, 210), flank=(236, 232, 222), belly=(242, 240, 232), spot=(214, 208, 196), ring=(230, 226, 216),
           eye=(10, 9, 10), glint=(220, 224, 230), nose=(24, 22, 24), whisker=(250, 248, 240), lip=(196, 190, 180),
           claw=(60, 56, 54), flipper=(206, 200, 190))


def se_coat(P, belly_y):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "bottom" or Y > belly_y + 1.5:
            return fur(P["belly"], tx, ty, 11, 0.04)
        t = smooth((Y - (belly_y - 5.0)) / 6.5)
        c = lerp(P["back"], P["flank"], t)
        if Y > belly_y:
            c = lerp(c, P["belly"], smooth((Y - belly_y) / 1.5))
        # dark blotches with pale rims (ringed harbour-seal look), thinning out down the flanks
        if blot(X + Y * 0.6, Z, 12, 1.7, 0.76 + 0.1 * t):
            c = P["spot"]
        elif blot(X + Y * 0.6, Z, 12, 1.7, 0.70 + 0.1 * t):
            c = lerp(c, P["ring"], 0.55)
        return fur(c, tx, ty, 13, 0.05, streak=False)
    return mat


def se_head(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face in ("left", "right"):
            if abs(Y - 16.5) <= 1.0 and -14.2 <= Z <= -12.5:            # big dark eye (2x2)
                return P["glint"] if (Y < 16.0 and Z < -13.3) else P["eye"]
            if Y > 19.0:
                return fur(lerp(P["flank"], P["belly"], 0.6), tx, ty, 14, 0.04)
            return se_coat(P, 19.5)(face, X, Y, Z, n, tx, ty)
        if face == "bottom":
            return fur(P["belly"], tx, ty, 15, 0.04)
        if face == "front":                                                # brow over the muzzle
            return fur(lerp(P["back"], P["flank"], 0.5), tx, ty, 16, 0.05)
        return se_coat(P, 19.5)(face, X, Y, Z, n, tx, ty)
    return mat


def se_muzzle(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "front":
            if Y < 18.6 and abs(X) < 1.0:
                return P["nose"]                                           # V-shaped nostrils
            if Y > 19.6:
                return P["lip"]
            return fur(P["flank"], tx, ty, 17, 0.04)
        if face in ("left", "right"):
            if Y > 18.0 and (tx + ty) % 2 == 0 and Z < -16.5:
                return P["whisker"]                                        # whisker pads
            return fur(lerp(P["flank"], P["belly"], 0.35), tx, ty, 18, 0.04)
        if face == "bottom":
            return fur(P["belly"], tx, ty, 19, 0.04)
        return fur(lerp(P["back"], P["flank"], 0.6), tx, ty, 20, 0.05)
    return mat


def se_flipper(P, nails):
    def mat(face, X, Y, Z, n, tx, ty):
        c = fur(P["flipper"], tx, ty, 21, 0.06, streak=False)
        if nails(face, X, Y, Z, tx):
            return P["claw"]
        return c
    return mat


SEAL = [
    part("body", None, (0, 19, 0), (0, 0, 0), [
        cube("se_torso", -5, -4, -5, 10, 9, 10, None),
        cube("se_chest", -4, -3.5, -11, 8, 8, 6, None),
    ]),
    part("head", "body", (0, -1, -11), (0, 0, 0), [
        cube("se_skull", -3, -3.5, -5, 6, 6, 5, None),
        cube("se_muzzle", -2, -0.5, -8, 4, 3, 3, None),
    ]),
    part("hips", "body", (0, 0.5, 5), (0, 0, 0), [cube("se_hips", -3.5, -3, 0, 7, 7, 6, None)]),
    part("tail", "hips", (0, 2, 6), (0, 0, 0), [
        cube("se_hflip", -3.5, -0.5, 0, 3, 1, 5, None),
        cube("se_hflip", 0.5, -0.5, 0, 3, 1, 5, None, True),
    ]),
    part("right_flipper", None, (-4.5, 21, -8), (0.3, 0, 0.5), [cube("se_fflip", -1, 0, -1.5, 2, 3, 3, None)]),
    part("left_flipper", None, (4.5, 21, -8), (0.3, 0, -0.5), [cube("se_fflip", -1, 0, -1.5, 2, 3, 3, None, True)]),
]
SEAL_SIZE = (64, 64)


def seal_parts(P):
    mats = {"se_torso": se_coat(P, 21.5), "se_chest": se_coat(P, 21.0), "se_hips": se_coat(P, 21.5),
            "se_skull": se_head(P), "se_muzzle": se_muzzle(P),
            "se_hflip": se_flipper(P, lambda f, X, Y, Z, tx: f == "back" and tx % 2 == 0),
            "se_fflip": se_flipper(P, lambda f, X, Y, Z, tx: f == "bottom" or (f == "front" and Y > 23.0))}
    return with_mats(SEAL, mats)


def with_mats(parts, mats):
    out = []
    for p in parts:
        q = dict(p)
        q["cubes"] = [dict(c, mat=mats[c["key"]]) for c in p["cubes"]]
        out.append(q)
    return out


# ======================================================================= walrus
WA = dict(skin=(160, 104, 82), dark=(118, 72, 56), light=(186, 132, 106), pink=(196, 128, 112), hair=(204, 160, 120),
          pad=(214, 184, 150), bristle=(244, 236, 214), eye=(40, 18, 14), tusk=(236, 228, 204), tusk_dark=(196, 182, 150),
          flipper=(132, 86, 68), nail=(70, 50, 40))
CALF = dict(skin=(104, 86, 76), dark=(74, 60, 54), light=(132, 112, 98), pink=(140, 106, 96), hair=(150, 128, 110),
            pad=(170, 150, 128), bristle=(220, 212, 196), eye=(24, 14, 12), tusk=(236, 228, 204), tusk_dark=(196, 182, 150),
            flipper=(88, 72, 64), nail=(50, 40, 36))


def wa_skin(P, belly_y):
    def mat(face, X, Y, Z, n, tx, ty):
        base = P["light"] if (face == "bottom" or Y > belly_y) else P["skin"]
        c = base
        # deep folds: wavy horizontal wrinkles round the neck and shoulders, mottled pink patches
        wave = math.sin(Z * 0.9 + math.sin(X * 0.5) * 1.5 + Y * 0.3)
        if wave > 0.82 and face != "bottom":
            c = lerp(c, P["dark"], 0.6)
        if blot(X, Z + Y, 31, 3.0, 0.66):
            c = lerp(c, P["pink"], 0.45)
        if h(tx, ty, 32) < 0.06:
            c = P["hair"]                                                 # sparse coarse hairs
        return fur(c, tx, ty, 33, 0.05, streak=False)
    return mat


def wa_head(P):
    skin = wa_skin(P, 18.0)

    def mat(face, X, Y, Z, n, tx, ty):
        if face in ("left", "right") and abs(Y - 13.0) < 0.6 and abs(Z - (-15.5)) < 0.6:
            return P["eye"]                                               # small, deep-set eye
        return skin(face, X, Y, Z, n, tx, ty)
    return mat


def wa_muzzle(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return fur(lerp(P["skin"], P["pad"], 0.4), tx, ty, 34, 0.05)
        if face == "front" or face in ("left", "right"):
            if Y > 14.8 and (tx * 3 + ty) % 3 == 0:
                return P["bristle"]                                        # stiff whisker quills
            if face == "front" and Y < 14.6 and abs(X) < 2.5 and abs(X) > 0.6:
                return P["dark"]                                           # nostril slits
            return fur(P["pad"], tx, ty, 35, 0.05, streak=False)
        return fur(lerp(P["pad"], P["light"], 0.5), tx, ty, 36, 0.04)
    return mat


def wa_tusk(P):
    def mat(face, X, Y, Z, n, tx, ty):
        t = smooth((Y - 18.5) / 6.0)                                       # dark root to bright tip
        c = lerp(P["tusk_dark"], P["tusk"], t)
        return shade(c, 0.9) if face in ("back", "right") else c
    return mat


def wa_flipper(P):
    def mat(face, X, Y, Z, n, tx, ty):
        if face in ("front", "bottom") and Y > 22.5 and tx % 2 == 0:
            return P["nail"]
        return fur(P["flipper"], tx, ty, 37, 0.06, streak=False)
    return mat


WALRUS = [
    part("body", None, (0, 17, 0), (0, 0, 0), [
        cube("wa_torso", -8, -7, -6, 16, 14, 13, None),
        cube("wa_chest", -7, -6, -12, 14, 12, 6, None),
    ]),
    part("head", "body", (0, -3, -12), (0, 0, 0), [
        cube("wa_skull", -4.5, -4, -6, 9, 8, 6, None),
        cube("wa_muzzle", -4, -0.5, -9, 8, 5, 3, None),
    ]),
    part("right_tusk", "head", (-2.5, 4.5, -8), (0.15, 0, 0.06), [cube("wa_tusk", -0.5, 0, -0.5, 1, 6, 1, None)]),
    part("left_tusk", "head", (2.5, 4.5, -8), (0.15, 0, -0.06), [cube("wa_tusk", -0.5, 0, -0.5, 1, 6, 1, None, True)]),
    part("hips", "body", (0, 1, 7), (0, 0, 0), [cube("wa_hips", -6, -5, 0, 12, 11, 7, None)]),
    part("right_hind", "hips", (-3.5, 5, 7), (0, 0, 0), [cube("wa_hflip", -2, -0.5, 0, 4, 1, 6, None)]),
    part("left_hind", "hips", (3.5, 5, 7), (0, 0, 0), [cube("wa_hflip", -2, -0.5, 0, 4, 1, 6, None, True)]),
    part("right_front", None, (-6.5, 18, -9), (0, 0, 0.2), [cube("wa_fflip", -1.5, 0, -2, 3, 6, 4, None)]),
    part("left_front", None, (6.5, 18, -9), (0, 0, -0.2), [cube("wa_fflip", -1.5, 0, -2, 3, 6, 4, None, True)]),
]
WALRUS_SIZE = (128, 64)


def walrus_parts(P):
    mats = {"wa_torso": wa_skin(P, 21.0), "wa_chest": wa_skin(P, 20.5), "wa_hips": wa_skin(P, 21.5), "wa_skull": wa_head(P),
            "wa_muzzle": wa_muzzle(P), "wa_tusk": wa_tusk(P), "wa_hflip": wa_flipper(P), "wa_fflip": wa_flipper(P)}
    return with_mats(WALRUS, mats)


# ======================================================================= pike
PK = dict(back=(48, 62, 34), green=(90, 110, 52), spot=(206, 196, 128), belly=(232, 228, 206), fin=(170, 96, 48),
          fin_dark=(86, 56, 30), eye=(214, 170, 50), pupil=(12, 10, 8), jaw=(196, 190, 160))


def pk_side(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or Y > 22.0:
        return fur(PK["belly"], tx, ty, 41, 0.03, streak=False)
    if face == "top" or Y < 19.6:
        c = PK["back"]
    else:
        c = lerp(PK["back"], PK["green"], smooth((Y - 19.4) / 1.4))
    # rows of pale bean-shaped spots along the flanks (offset every other row)
    row = math.floor((Y - 19.0) / 1.0)
    if face in ("left", "right") and 19.5 < Y < 22.2 and (math.floor(Z + row * 1.5) % 3 == 0) and h(math.floor(Z), row, 42) < 0.8:
        c = PK["spot"]
    return fur(c, tx, ty, 43, 0.05, streak=False)


def pk_head(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right") and abs(Y - 20.5) <= 0.5 and abs(Z - (-6.5)) <= 0.5:
        return PK["pupil"] if Z > -6.5 else PK["eye"]
    if face in ("left", "right") and abs(Z - (-4.5)) < 0.5 and Y > 20.0:
        return shade(PK["green"], 0.7)                                     # gill cover edge
    return pk_side(face, X, Y, Z, n, tx, ty)


def pk_snout(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right") and abs(Y - 21.5) < 0.5:
        return shade(PK["jaw"], 0.6)                                       # the long mouth line
    if face == "bottom" or Y > 21.5:
        return PK["jaw"]
    return fur(PK["back"], tx, ty, 44, 0.05, streak=False)


def pk_fin(face, X, Y, Z, n, tx, ty):
    c = PK["fin"] if h(tx // 2, ty, 45) > 0.3 else PK["fin_dark"]
    return fur(c, tx, ty, 46, 0.06, streak=False)


def pk_caudal(face, X, Y, Z, n, tx, ty):
    # forked tail: notch out the middle of the trailing edge
    if Z > 12.5 and abs(Y - 21.0) < (Z - 12.5) * 1.2:
        return CLEAR
    return pk_fin(face, X, Y, Z, n, tx, ty)


def pk_dorsal(face, X, Y, Z, n, tx, ty):
    # rounded fin: clear the top front corner
    if Y < 18.3 and Z < 6.8:
        return CLEAR
    return pk_fin(face, X, Y, Z, n, tx, ty)


PIKE = [
    part("body", None, (0, 21, 0), (0, 0, 0), [cube("pk_body", -1.5, -2, -4, 3, 4, 9, pk_side)]),
    part("head", "body", (0, 0, -4), (0, 0, 0), [
        cube("pk_head", -1.5, -1.5, -4, 3, 3, 4, pk_head),
        cube("pk_snout", -1, -0.5, -7, 2, 2, 3, pk_snout),
    ]),
    part("tail", "body", (0, 0, 5), (0, 0, 0), [
        cube("pk_tail", -1, -1.5, 0, 2, 3, 5, pk_side),
        cube("pk_dorsal", 0, -3.5, 1, 0, 2, 4, pk_dorsal),
        cube("pk_anal", 0, 1.5, 1, 0, 2, 3, pk_fin),
    ]),
    part("caudal", "tail", (0, 0, 5), (0, 0, 0), [cube("pk_caudal", 0, -3, 0, 0, 6, 4, pk_caudal)]),
    part("right_fin", "body", (-1.5, 1.5, -3), (0, 0, 0.5), [cube("pk_pec", -2, 0, 0, 2, 0, 2, pk_fin)]),
    part("left_fin", "body", (1.5, 1.5, -3), (0, 0, -0.5), [cube("pk_pec", 0, 0, 0, 2, 0, 2, pk_fin, True)]),
]
PIKE_SIZE = (64, 32)

# ======================================================================= sturgeon
ST = dict(back=(84, 84, 66), side=(132, 128, 104), belly=(220, 212, 188), scute=(232, 226, 204), scute_dark=(170, 162, 136),
          speck=(56, 56, 46), snout=(168, 160, 132), barbel=(214, 184, 160), eye=(20, 18, 14), fin=(100, 96, 78))


def st_scutes(Y, Z, face):
    """Rows of bony diamond scutes: one along each flank (lateral line) and one along each belly edge."""
    if face not in ("left", "right"):
        return False
    for row in (19.0, 22.4):
        if abs(Y - row) < 0.7 and math.floor(Z) % 2 == 0:
            return True
    return False


def st_side(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or Y > 22.8:
        c = ST["belly"]
    elif face == "top" or Y < 18.0:
        c = ST["back"]
    else:
        c = lerp(ST["back"], ST["side"], smooth((Y - 17.5) / 3.0))
        if Y > 21.8:
            c = lerp(c, ST["belly"], smooth((Y - 21.8) / 1.0))
    if st_scutes(Y, Z, face):
        return ST["scute"] if (math.floor(Y * 2) + math.floor(Z)) % 2 == 0 else ST["scute_dark"]
    if h(tx, ty, 51) < 0.08 and Y < 21.5:
        c = ST["speck"]
    return fur(c, tx, ty, 52, 0.04, streak=False)


def st_ridge(face, X, Y, Z, n, tx, ty):
    return ST["scute"] if math.floor(Z) % 2 == 0 else ST["scute_dark"]  # the row of dorsal scutes


def st_head(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right") and abs(Y - 19.0) <= 0.5 and abs(Z - (-11.5)) <= 0.5:
        return ST["eye"]
    if face in ("top",) and h(tx, ty, 53) < 0.25:
        return ST["scute_dark"]                                            # bony head plates
    return st_side(face, X, Y, Z, n, tx, ty) if face != "top" else fur(ST["back"], tx, ty, 54, 0.06, streak=False)


def st_snout(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return ST["belly"]
    return fur(lerp(ST["snout"], ST["back"], 0.3 if face == "top" else 0.0), tx, ty, 55, 0.05, streak=False)


def st_barbels(face, X, Y, Z, n, tx, ty):
    # four whiskers hanging under the snout
    xi = math.floor(X + 1.5)
    return ST["barbel"] if face in ("front", "back") and xi in (0, 2) and Y < 23.0 else CLEAR


def st_fin(face, X, Y, Z, n, tx, ty):
    return fur(ST["fin"], tx, ty, 56, 0.07, streak=False)


def st_upper_lobe(face, X, Y, Z, n, tx, ty):
    # heterocercal tail: the upper lobe sweeps up and back to a point
    lz = Z - 18.0                                                         # 0..6 along the lobe
    top = 13.0 + (6.0 - lz) * 0.9                                         # the leading edge rises towards the back
    if Y < top - 1.0 or Y > 13.0 + 6.0 - lz * 0.25:
        return CLEAR
    return st_fin(face, X, Y, Z, n, tx, ty)


STURGEON = [
    part("body", None, (0, 20, 0), (0, 0, 0), [
        cube("st_body", -3, -3.5, -8, 6, 7, 16, st_side),
        cube("st_ridge", -0.5, -4.5, -7, 1, 1, 14, st_ridge),
    ]),
    part("head", "body", (0, 0.5, -8), (0, 0, 0), [
        cube("st_head", -2.5, -3, -5, 5, 5, 5, st_head),
        cube("st_snout", -1.5, -1, -10, 3, 2, 5, st_snout),
        cube("st_barbels", -1.5, 1, -9, 3, 2, 0, st_barbels),
    ]),
    part("tail", "body", (0, -0.5, 8), (0, 0, 0), [
        cube("st_tail", -2, -2.5, 0, 4, 5, 8, st_side),
        cube("st_dorsal", 0, -4.5, 3, 0, 2, 4, st_fin),
        cube("st_anal", 0, 2.5, 3, 0, 2, 3, st_fin),
    ]),
    part("tail_tip", "tail", (0, -0.5, 8), (0, 0, 0), [
        cube("st_tip", -1, -1.5, 0, 2, 3, 4, st_side),
        cube("st_upper", 0, -6, 2, 0, 5, 6, st_upper_lobe),
        cube("st_lower", 0, 1.5, 2, 0, 3, 4, st_fin),
    ]),
    part("right_fin", "body", (-3, 2.5, -5), (0, 0, 0.3), [cube("st_pec", -4, 0, 0, 4, 0, 4, st_fin)]),
    part("left_fin", "body", (3, 2.5, -5), (0, 0, -0.3), [cube("st_pec", 0, 0, 0, 4, 0, 4, st_fin, True)]),
]
STURGEON_SIZE = (64, 64)


def build(parts, size):
    uv = pack(parts, *size)
    return paint(parts, uv, size), uv


def entities():
    models = {"seal": (seal_parts(SE), SEAL_SIZE), "seal_pup": (seal_parts(PUP), SEAL_SIZE),
              "walrus": (walrus_parts(WA), WALRUS_SIZE), "walrus_calf": (walrus_parts(CALF), WALRUS_SIZE),
              "pike": (PIKE, PIKE_SIZE), "sturgeon": (STURGEON, STURGEON_SIZE)}
    keep = {"walrus": {"wa_tusk"}}
    for name, (parts, size) in models.items():
        img, uv = build(parts, size)
        if "--java" in sys.argv and name in ("seal", "walrus", "pike", "sturgeon"):
            print(name)
            java_cubes(parts, uv)
        save(img, f"wildlife/{name}.png")
        if name in ("seal", "walrus", "sturgeon"):
            sk = skinned(img, {}, ())
            for k in keep.get(name, ()):  # tusks stay ivory on the carcass
                copy_key(img, sk, parts, uv, k)
            save(sk, f"skinned/wildlife/{name}.png")


def copy_key(src, dst, parts, uv, key):
    for p in parts:
        for c in p["cubes"]:
            if c["key"] != key:
                continue
            u, v = uv[key]
            _, _, _, w, hh, d = c["box"]
            for x in range(u, u + 2 * (math.ceil(w) + math.ceil(d))):
                for y in range(v, v + math.ceil(d) + math.ceil(hh)):
                    dst.putpixel((x, y), src.getpixel((x, y)))
            return


# ======================================================================= items (16x16)
GRIDS = {
    "raw_seal_meat": ([
        "................",
        "................",
        "....oooooo......",
        "...oFFFFFFoo....",
        "..oFfmmmmmFFo...",
        "..ofmmlllmmmfo..",
        ".ofmmlhhllmmmdo.",
        ".ofmlhhllmmmmdo.",
        ".ofmllllmmmmddo.",
        ".ofmmllmmmmmddo.",
        "..ofmmmmmmmddo..",
        "..ofdmmmmmddo...",
        "...oddddddoo....",
        "....oooooo......",
        "................",
        "................"],
        dict(o="#2a0e14", d="#4e1622", m="#6e2232", l="#8c3444", h="#b05a66", F="#e8dcc4", f="#c8b8a0")),
    "raw_walrus_meat": ([
        "................",
        "................",
        "..ssssssssss....",
        ".osFFFFFFFFFso..",
        ".oFFHFFFFFFFFo..",
        ".offfffffffffo..",
        ".ommllhhlllmmdo.",
        ".omlhhllmmmmmdo.",
        ".omllllmmmmmddo.",
        ".ommmlmmmmmdddo.",
        ".odmmmmmmmdddo..",
        "..oddddddddoo...",
        "...ooooooooo....",
        "................",
        "................",
        "................"],
        dict(o="#2e1010", s="#7a4a3a", F="#eadcbc", H="#faf2dc", f="#c8b494", d="#5a1a1e", m="#7c2a2c", l="#9c403e", h="#bc6660")),
    "blubber": ([
        "................",
        "................",
        "................",
        "....ooooooo.....",
        "...osssssssoo...",
        "..oFFFFFFFFFso..",
        ".oFHHFFFFFFFFo..",
        ".oFHFFFFFFFFfo..",
        ".oFFFFFFFFFffo..",
        ".offFFFFFFfffo..",
        "..offffffffoo...",
        "...oooooooo.....",
        "................",
        "................",
        "................",
        "................"],
        dict(o="#6e5a40", s="#55494a", F="#efe2b8", H="#fbf4dc", f="#d8c48e")),
    "sealskin": ([
        "................",
        "......oooo......",
        ".....oGGgGo.....",
        "..oo.oGSGGo.oo..",
        "..oGooGgGSooGo..",
        "...oGGSGGgGGo...",
        "....oGgGSGGo....",
        "....oGSGGgGo....",
        "....ogGGGSGo....",
        "....oGGSGgGo....",
        "....oSGgGGGo....",
        "....oGGGSGgo....",
        ".....oGgGGo.....",
        "....ooGooGoo....",
        "...oGGo..oGGo...",
        "....oo....oo...."],
        dict(o="#8c7a5c", G="#7c7c78", g="#9c9a92", S="#3c3c3e")),
    "raw_sealskin": ([
        "................",
        "......oooo......",
        ".....oGGgGo.....",
        "..oo.oGSGGo.oo..",
        "..oGooGgGSooGo..",
        "...oGGSGGgGGo...",
        "....oGgGSGGo....",
        "....oGSGGgGo....",
        "....ogGGGSGo....",
        "....oGGSGgpp....",
        "....oSGgGpPPp...",
        "....oGGGSpPfPp..",
        ".....oGgGGpPPp..",
        "....ooGoooppp...",
        "...oGGo..oGGo...",
        "....oo....oo...."],
        dict(o="#8a2e2a", G="#5e5e5c", g="#74726c", S="#2a2a2c", p="#a04a44", P="#d08878", f="#ecd2b8")),
    "walrus_hide": ([
        "................",
        "................",
        "..oooooooooooo..",
        ".oHHHHwHHHHHHHo.",
        ".oHwwHHHHHwHHHo.",
        ".oHHHHHwwHHHHwo.",
        ".owHHHHHHHHwHHo.",
        ".oHHHwwHHHHHHHo.",
        ".oHHHHHHHwwHHHo.",
        ".ottttttttttttO.",
        ".oTTTTTTTTTTTTO.",
        "..oOOOOOOOOOOO..",
        "................",
        "................",
        "................",
        "................"],
        dict(o="#4a2c1e", H="#b07a5a", w="#86553c", t="#c89a74", T="#9a6a4c", O="#3a2216")),
    "raw_walrus_hide": ([
        "................",
        "................",
        "..oooooooooooo..",
        ".oHHHHwHHHHHHHo.",
        ".oHwwHHHHHwHHHo.",
        ".oHHHHHwwHHHHwo.",
        ".owHHHHHHHHwHHo.",
        ".oHHHwwHHHHHHHo.",
        ".oHHHHHHHwwHHHo.",
        ".ottttftttttftO.",
        ".oTTTTTTTTTTTTO.",
        "..oOOOOOOOOOOO..",
        "................",
        "................",
        "................",
        "................"],
        dict(o="#5a1e1a", H="#a4664e", w="#7a4434", t="#d07c70", f="#ecd2b8", T="#b0544c", O="#3e1210")),
    "sealskin_parka": ([
        "................",
        "..ooo......ooo..",
        ".oWWWo.oo.oWWWo.",
        ".oGGGWoWWoWGGGo.",
        ".oGSGGWWWWGGSGo.",
        ".oGGGGGgGGGGGGo.",
        "..ooGGGSGGgGoo..",
        "....oGgGGGGo....",
        "....oGGGGSGo....",
        "....oGSGgGGo....",
        "....oGGGGGGo....",
        "....ogGGSGGo....",
        "....oWWWWWWo....",
        ".....oooooo.....",
        "................",
        "................"],
        dict(o="#2a2a2c", W="#e8e2d0", G="#7a7874", g="#949288", S="#3e3e40")),
    "sealskin_boots": ([
        "................",
        "................",
        "..oooo...oooo...",
        "..oWWo...oWWo...",
        "..oGGo...oGGo...",
        "..oGSo...oSGo...",
        "..ogGo...oGgo...",
        "..oSGo...oGSo...",
        "..oGGo...oGGo...",
        "..oGGGo..oGGGo..",
        "..oGSGGo.oGGSGo.",
        "..oGGgGo.ogGGGo.",
        "..otttto.otttto.",
        "..oooooo.oooooo.",
        "................",
        "................"],
        dict(o="#2a2a2c", W="#e8e2d0", G="#7a7874", g="#949288", S="#3e3e40", t="#5a4a3a")),
    "walrus_hide_cuirass": ([
        "................",
        "..ooo......ooo..",
        ".oHHHo.oo.oHHHo.",
        ".oHHHHoTToHHHHo.",
        ".oTTTTTTTTTTTTo.",
        ".oHHlHHHHHHlHHo.",
        "..oTTlTTTTlTTo..",
        "...oHHlHHlHHo...",
        "...oTTTllTTTo...",
        "...oHHHllHHHo...",
        "...oTTTllTTTo...",
        "...oHHHllHHHo...",
        "...oTTTTTTTTo...",
        "....oooooooo....",
        "................",
        "................"],
        dict(o="#3a2216", H="#a8704e", T="#7e4c34", l="#e0d0aa")),
    "walrus_ivory": ([
        "................",
        "..oo............",
        ".oRRo...........",
        ".oRrRo..........",
        "..oRIIo.........",
        "...oIIHo........",
        "....oIIHo.......",
        ".....oIIHo......",
        "......oIIHo.....",
        "......oIIIHo....",
        ".......oIIIHo...",
        "........oIIHo...",
        ".........oIHHo..",
        "..........oIHo..",
        "...........oHo..",
        "............oo.."],
        dict(o="#5a4c36", R="#9a8462", r="#6e5a40", I="#e2d6b8", H="#f8f2e2")),
    "ivory_knife": ([
        "................",
        "............oo..",
        "...........oHo..",
        "..........oHIo..",
        ".........oHIo...",
        "........oHIo....",
        ".......oHIo.....",
        "......oHIo......",
        ".....oIIo.......",
        "....oTTo........",
        "...oTtTo........",
        "..oSTo..........",
        ".oSso...........",
        ".oSo............",
        "..o.............",
        "................"],
        dict(o="#3a2c1c", H="#faf4e4", I="#d8ccac", T="#b08c58", t="#8a6a40", S="#7a5a34", s="#5c4224")),
    "whole_pike": ([
        "................",
        "...........oo...",
        "..........oFFo..",
        ".........oFFFo..",
        "........oGGoo...",
        ".......oGsGGo...",
        "......oGGGsGo...",
        ".....oGsGGGGo...",
        "....oGGGsGGo....",
        "...oGsGGGsGo....",
        "..oGGGGsGGo.....",
        ".oEGGsGGGo......",
        "oBBGGGGFo.......",
        "oBBBoooo........",
        ".ooo............",
        "................"],
        dict(o="#1e2a16", G="#5a7034", s="#d2c47e", E="#d6aa32", B="#d8d2b0", F="#a8602e")),
    "raw_pike": ([
        "................",
        "................",
        "................",
        "..........ooo...",
        "........ooPPPo..",
        "......ooPPLLPo..",
        "....ooPPLLLPPo..",
        "...oPPLLLLPPo...",
        "..oPPLLLPPPo....",
        "..oPLLLPPPo.....",
        "..oPPPPPoo......",
        "...oPPoo........",
        "....oo..........",
        "................",
        "................",
        "................"],
        dict(o="#9a6a5c", P="#e8c4b4", L="#f6e0d4")),
    "whole_sturgeon": ([
        "................",
        "............o...",
        "...........oFo..",
        "..........oFFo..",
        ".........oGGo...",
        "........oGwGo...",
        ".......oGGGwo...",
        "......oGwGGGo...",
        ".....oGGGwGo....",
        "....oGwGGGo.....",
        "...oGGGwGo......",
        "..oEGwGGo.......",
        ".oSGGGGo........",
        "oSSbooo.........",
        "obo.............",
        "................"],
        dict(o="#24241c", G="#74725c", w="#ece6d0", E="#141410", S="#a8a284", b="#d8b8a0", F="#5c5a48")),
    "raw_sturgeon": ([
        "................",
        "................",
        ".....oooooo.....",
        "...ooSSSSSSoo...",
        "..oSPPPPPPPPSo..",
        "..oSPLLPPPPPSo..",
        ".oSPLLPPPPPPPSo.",
        ".oSPPPPPWPPPPSo.",
        ".oSPPPPWWWPPPSo.",
        ".oSPPPPPWPPPPSo.",
        "..oSPPPPPPPPSo..",
        "..oSPPPPPPPPSo..",
        "...ooSSSSSSoo...",
        ".....oooooo.....",
        "................",
        "................"],
        dict(o="#3e3a2e", S="#6e6c5a", P="#e2a898", L="#f2c8bc", W="#f4ece0")),
    "sturgeon_roe": ([
        "................",
        "................",
        "................",
        "................",
        "......oooo......",
        "....ooRrRRoo....",
        "...oRRrHRrRRo...",
        "..oRrHRRrRHrRo..",
        "..oRRrRHRrRRro..",
        ".oRHRrRRrHRrRRo.",
        ".orRRHrRRrRRHro.",
        "..oorrrrrrrroo..",
        "....oooooooo....",
        "................",
        "................",
        "................"],
        dict(o="#141518", R="#3c3e44", r="#24262a", H="#8a909a")),
    "isinglass": ([
        "................",
        "................",
        "....oooooo......",
        "...oYYYYYYoo....",
        "..oYHYYyYYYYo...",
        "..oYYyYYHYYYyo..",
        "..oYyYYYYyYYYo..",
        "...oYYHYyYYYYo..",
        "...oYYYYYYHYyo..",
        "..oYyYYYyYYYo...",
        "..oYYHYYYYyo....",
        "...ooYYyYYo.....",
        ".....ooooo......",
        "................",
        "................",
        "................"],
        dict(o="#a8966a", Y="#e8deb0", y="#c8b880", H="#fffbe8")),
}
GRIDS["cooked_seal_meat"] = (GRIDS["raw_seal_meat"][0],
                             dict(o="#24120a", d="#4a2614", m="#6a3a1e", l="#8a5230", h="#ac7448", F="#d8b47c", f="#b08c58"))
GRIDS["cooked_walrus_meat"] = (GRIDS["raw_walrus_meat"][0],
                               dict(o="#28140a", s="#5a3622", F="#e0bc80", H="#f0d8a4", f="#b8925c", d="#4c2a14", m="#6e3e20", l="#8e5630", h="#b07a4c"))
GRIDS["cooked_pike"] = (GRIDS["raw_pike"][0], dict(o="#6a4420", P="#d4a060", L="#ecc88c"))
GRIDS["cooked_sturgeon"] = (GRIDS["raw_sturgeon"][0], dict(o="#3a2410", S="#5a4a30", P="#c8945a", L="#e0b47c", W="#efe0c0"))


def eggs():
    K, W = "#141414", "#efe8d8"
    seal_spots = [(x, y, "#3a3a3c") for x, y in ((5, 4), (9, 3), (11, 6), (4, 8), (8, 7), (12, 10), (6, 11), (10, 13))]
    seal = egg(["#a8a49a", "#8e8a82", "#76736c", "#5e5c58", "#44423f", "#2c2b2a"], seal_spots, (), [(6, 14, K), (9, 14, K)])
    tusks = [(6, 12, "#efe6cc"), (6, 13, "#efe6cc"), (6, 14, "#e2d6b4"), (9, 12, "#efe6cc"), (9, 13, "#efe6cc"), (9, 14, "#e2d6b4")]
    walrus = egg(["#d49a7e", "#bc8268", "#a06a54", "#845642", "#5e3c2e", "#3e271e"],
                 [(x, y, "#e8d2b0") for x, y in ((5, 11), (7, 11), (8, 11), (10, 11))], (), tusks)
    pike = egg(["#a4b46a", "#86984e", "#6c803c", "#56682e", "#3c4a20", "#283214"],
               [(x, y, "#e2d68e") for x, y in ((5, 5), (8, 4), (11, 6), (4, 9), (7, 8), (10, 9), (6, 12), (9, 12), (12, 11))], (), [])
    sturgeon = egg(["#b4b098", "#98947c", "#7e7a64", "#66634f", "#4a4838", "#302f24"],
                   [(x, y, W) for x, y in ((7, 2), (8, 4), (7, 6), (8, 8), (7, 10), (8, 12), (4, 7), (11, 7), (4, 11), (11, 11))], (), [])
    return {"seal_spawn_egg": seal, "walrus_spawn_egg": walrus, "pike_spawn_egg": pike, "sturgeon_spawn_egg": sturgeon}


def items():
    out = {name: from_grid(rows, pal) for name, (rows, pal) in GRIDS.items()}
    out.update(eggs())
    for name, im in out.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(OUT / f"{name}.png")
    print("items:", len(out))


# ======================================================================= blocks: oil lamp, rack pelts
def block_tex(name, fn):
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            c = fn(x, y)
            im.putpixel((x, y), c if len(c) == 4 else (*c, 255))
    im.save(TEXROOT / "block" / f"{name}.png")


def blocks():
    # soapstone: grey-green, soft speckles and tool marks
    block_tex("oil_lamp", lambda x, y: fur(lerp((122, 128, 116), (92, 98, 90), h(x // 2, y, 61)), x, y, 62, 0.08, streak=False))
    block_tex("oil_lamp_oil", lambda x, y: (250, 232, 170) if h(x, y, 63) < 0.12 else lerp((214, 178, 96), (186, 144, 64), h(x, y // 2, 64)))
    block_tex("oil_lamp_wick", lambda x, y: lerp((92, 70, 44), (58, 44, 30), h(x, y, 65)))

    # sealskin: one block of the tall rack (rows 3-13, cols 3-12): an oval skin with fore and hind flippers, lashed at the corners
    seal_pelt = ["T.oSSSSo.T",
                 "TooSGGSooT",
                 ".oGGgGSGo.",
                 "fGSGGGgGGf",
                 ".oGgSGGSo.",
                 ".oGGGgGGo.",
                 ".oSGGSGgo.",
                 ".oGgGGGSo.",
                 "..oGSGgo..",
                 ".ffoGGoff.",
                 "Tff.oo.ffT"]
    for stage, pal in (("raw", dict(o=(140, 52, 46), G=(86, 86, 84), g=(106, 104, 98), S=(40, 40, 42), f=(70, 70, 68), T=(170, 130, 80))),
                       ("dry", dict(o=(150, 128, 92), G=(124, 124, 118), g=(150, 148, 140), S=(56, 56, 58), f=(110, 108, 102), T=(170, 130, 80)))):
        rack_tex(f"drying_rack_seal_{stage}", seal_pelt, pal, 3, stage)
    # walrus hide: the whole tall rack (one hide 10 x 25), a thick wrinkled slab lashed down both sides
    rows = []
    for r in range(25):
        row = ""
        for c in range(10):
            if c in (0, 9) and r % 4 == 1:
                row += "T"
            elif c in (0, 9) or r in (0, 24):
                row += "o"
            else:
                row += "w" if (r * 3 + c * 5) % 11 == 0 or (r + c * 2) % 13 == 0 else "H"
        rows.append(row)
    for stage, pal in (("raw", dict(o=(150, 66, 58), H=(164, 102, 78), w=(122, 68, 52), T=(170, 130, 80))),
                       ("dry", dict(o=(120, 84, 58), H=(176, 122, 90), w=(134, 85, 60), T=(170, 130, 80)))):
        rack_tex(f"drying_rack_walrus_{stage}_upper", rows[:13], pal, 3, stage)
        rack_tex(f"drying_rack_walrus_{stage}_lower", rows[13:], pal, 0, stage)


RAW_FLESH = [(132, 44, 42), (168, 70, 64), (194, 104, 92)]
CURED = [(128, 96, 62), (160, 124, 86), (184, 150, 108)]


def rack_tex(name, rows, pal, row0, stage):
    """Front (hair side) from the grid; the _back is the flesh side: raw red with fat streaks, or cured tan; twine stays."""
    front = Image.new("RGBA", (16, 16), CLEAR)
    back = Image.new("RGBA", (16, 16), CLEAR)
    flesh = RAW_FLESH if stage == "raw" else CURED
    for r, row in enumerate(rows):
        for c, ch in enumerate(row):
            if ch == ".":
                continue
            x, y = 3 + c, row0 + r
            front.putpixel((x, y), (*fur(pal[ch], x, y + row0 * 7, 66, 0.06, streak=False), 255))
            if ch == "T":
                bcol = pal[ch]
            else:
                bcol = flesh[min(2, int(h(x, y + row0 * 7, 67) * 3))]
                if stage == "raw" and h(x // 2, y, 68) < 0.12:
                    bcol = (232, 214, 188)
            back.putpixel((x, y), (*bcol, 255))
    front.save(TEXROOT / "block" / f"{name}.png")
    back.save(TEXROOT / "block" / f"{name}_back.png")


# ======================================================================= armour layers (recoloured from the fur layer)
def armour():
    src = Image.open(TEXROOT / "entity/equipment/humanoid/fur.png").convert("RGBA")
    for name, fn in (("sealskin", seal_layer), ("walrus_hide", walrus_layer)):
        out = Image.new("RGBA", src.size, CLEAR)
        for y in range(src.height):
            for x in range(src.width):
                r, g, b, a = src.getpixel((x, y))
                if a:
                    out.putpixel((x, y), (*fn(x, y, (0.299 * r + 0.587 * g + 0.114 * b) / 255), a))
        out.save(TEXROOT / f"entity/equipment/humanoid/{name}.png")


def seal_layer(x, y, lum):
    if lum > 0.78:                                             # the fur's pale trim stays a cream ruff
        return fur((228, 222, 206), x, y, 71, 0.05)
    c = lerp((70, 70, 68), (132, 130, 122), lum * 1.3)
    if blot(x, y, 72, 1.6, 0.74):
        c = (44, 44, 46)
    return fur(c, x, y, 73, 0.05, streak=False)


def walrus_layer(x, y, lum):
    c = lerp((96, 58, 40), (176, 122, 88), lum * 1.2)
    if y % 4 == 3:
        c = shade(c, 0.72)                                     # overlapping strips of hide
    if x % 6 == 2 and y % 4 in (1, 2):
        c = (224, 208, 170)                                    # sinew lacing
    return fur(c, x, y, 74, 0.04, streak=False)


if __name__ == "__main__":
    entities()
    items()
    blocks()
    armour()
