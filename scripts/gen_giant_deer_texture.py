"""Paint the giant deer (Megaloceros) texture for GiantDeerModel (128x128) and its skinned (flesh) version, plus the
giant antler item and the spawn egg.

Usage: python scripts/gen_giant_deer_texture.py [--java]
The part tree mirrors GiantDeerModel (pivots, rotations, boxes, UVs from critter_paint.pack()); --java prints the cube
lines to check the Java model against. Deterministic.
Look, after the cave paintings: light fawn body with a dark chocolate shoulder hump, a dark diagonal stripe behind the
shoulder, a dark shaggy throat collar, pale belly and rump patch, dark lower legs; grey-brown head with a dark muzzle;
huge pale bone-coloured palmate antlers with darker grooved bases and ivory tine tips.
"""
import math
import sys

from critter_paint import cube, fur, h, java_cubes, lerp, pack, paint, part, save, shade, skinned, smooth

GD = dict(fawn=(178, 134, 88), back=(140, 100, 62), pale=(222, 204, 170), rump=(232, 218, 190), hump=(84, 56, 36),
          hump_hi=(112, 78, 50), stripe=(66, 44, 30), collar=(72, 50, 34), collar_hi=(104, 76, 52), neck=(150, 116, 82),
          head=(146, 114, 84), head_dark=(104, 80, 60), muzzle=(92, 72, 56), nose=(24, 20, 20), eye=(14, 10, 8),
          eye_gold=(120, 86, 40), leg=(118, 86, 58), leg_dark=(62, 46, 34), hoof=(30, 26, 24), ear_in=(206, 176, 150),
          bone=(214, 198, 162), bone_dark=(150, 128, 96), ivory=(240, 232, 210), burr=(106, 88, 66))


def body_mat(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or n[1] > 0.7:
        return fur(GD["pale"], tx, ty, 1, 0.04)
    # dark hump over the shoulders, fading back along the spine
    hump = smooth((4.0 - Y) / 4.0) * smooth((2.0 - Z) / 6.0)
    belly = smooth((Y - 9.5) / 2.5)
    c = lerp(GD["back"], GD["fawn"], smooth((Y - 1.0) / 5.0))
    c = lerp(c, GD["pale"], belly)
    c = lerp(c, GD["hump"], hump * 0.9)
    # the cave painters' stripe: a dark band running down and back from the withers behind the shoulder
    s = (Z + 4.0) - (Y - 2.0) * 0.55
    if face in ("left", "right") and abs(s) < 1.1 and 2.0 < Y < 10.5:
        c = lerp(c, GD["stripe"], 0.85 - abs(s) * 0.3)
    # pale rump patch around the tail
    if Z > 8.5 and (face == "back" or (face in ("left", "right", "top") and Y > 3.0)):
        d = math.hypot(X / 4.5, (Y - 6.0) / 5.0) if face == "back" else (11.0 - Z) / 2.5
        if d < 1.0:
            c = lerp(c, GD["rump"], 0.8 * smooth((1.0 - d) / 0.6))
    if face == "front":                                   # chest: dark collar runs down onto it
        c = lerp(c, GD["collar"], smooth((9.0 - Y) / 6.0) * 0.8)
    return fur(c, tx, ty, 2, 0.08)


def hump_mat(face, X, Y, Z, n, tx, ty):
    c = lerp(GD["hump"], GD["hump_hi"], 0.35 * h(tx, ty // 2, 3))
    if face == "back":
        c = lerp(c, GD["back"], 0.4)
    return fur(c, tx, ty, 4, 0.09)


def tail_mat(face, X, Y, Z, n, tx, ty):
    if face == "back" or face == "bottom":
        return fur(GD["rump"], tx, ty, 5, 0.04)
    return fur(GD["hump"], tx, ty, 6)


def neck_mat(face, X, Y, Z, n, tx, ty):
    # the neck leans forward: "front" faces the ground, "back" is the crest
    if face == "front" or face == "bottom":
        return fur(GD["collar"], tx, ty, 7, 0.08)
    c = lerp(GD["neck"], GD["collar"], smooth((Y + 2.0) / 7.0) * 0.6)  # darker towards the shoulders
    if face == "back":
        c = lerp(c, GD["hump"], 0.5)                       # dark mane line on the crest
    return fur(c, tx, ty, 8)


def mane_mat(face, X, Y, Z, n, tx, ty):
    """Shaggy throat collar: long dark hair with lighter tips, ragged."""
    c = GD["collar"] if h(tx, ty, 9) > 0.25 else GD["collar_hi"]
    if h(tx, ty // 2, 10) < 0.3:
        c = shade(c, 0.8)
    return fur(c, tx, ty, 11, 0.06)


def skull_mat(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right"):
        ez, ey = -20.5, -6.2
        if abs(Z - ez) < 0.5 and abs(Y - ey) < 0.5:
            return GD["eye"]
        if abs(Z - (ez - 1)) < 0.5 and abs(Y - ey) < 0.5:
            return GD["eye_gold"]
        if abs(Z - (ez - 0.5)) < 1.6 and abs(Y - ey) < 1.2:
            return fur(GD["head_dark"], tx, ty, 12, 0.04)  # dark eye patch
        if Y > -3.8:
            return fur(lerp(GD["head"], GD["pale"], 0.35), tx, ty, 13, 0.05)  # pale jaw
        return fur(GD["head"], tx, ty, 14)
    if face == "top":
        return fur(GD["head_dark"], tx, ty, 15)
    if face == "bottom":
        return fur(GD["collar"], tx, ty, 16)
    return fur(GD["head_dark"], tx, ty, 17)


def muzzle_mat(face, X, Y, Z, n, tx, ty):
    if face == "front":
        if Y < -3.0:
            return GD["nose"] if (abs(X) > 0.5 or h(tx, ty, 18) < 0.5) else shade(GD["nose"], 1.6)
        return fur(GD["muzzle"], tx, ty, 19, 0.04)
    if face == "top":
        return fur(lerp(GD["head_dark"], GD["muzzle"], 0.5), tx, ty, 20)
    if face == "bottom":
        return fur(GD["pale"], tx, ty, 21, 0.04)
    if Y > -2.4:
        return fur(GD["pale"], tx, ty, 22, 0.04)           # pale chin / lip line
    return fur(lerp(GD["head"], GD["muzzle"], 0.6), tx, ty, 23)


def ear_mat(face, X, Y, Z, n, tx, ty):
    if face == "front":
        return fur(GD["ear_in"], tx, ty, 24, 0.05)
    return fur(GD["head_dark"], tx, ty, 25)


def leg_mat(lower_from):
    def mat(face, X, Y, Z, n, tx, ty):
        if Y > 22.6:
            return GD["hoof"] if face != "top" else shade(GD["hoof"], 1.3)
        if face == "bottom":
            return GD["hoof"]
        t = smooth((Y - lower_from) / 3.0)
        c = lerp(GD["leg"], GD["leg_dark"], t)
        if face == "back" and Y < lower_from:
            c = lerp(c, GD["pale"], 0.35)                  # paler inner/back of the thigh
        return fur(c, tx, ty, 26, 0.07)
    return mat


def antler_mat(kind):
    """Bone: darker and grooved near the skull, paler outwards, ivory at the tine tips; flat palm faces show growth
    grooves running out towards the edge."""
    def mat(face, X, Y, Z, n, tx, ty):
        out = smooth((abs(X) - 3.0) / 14.0)
        c = lerp(GD["bone_dark"], GD["bone"], 0.3 + 0.7 * out)
        if kind == "burr":
            c = GD["burr"] if h(tx, ty, 27) > 0.3 else shade(GD["burr"], 1.25)
        elif kind == "tine":
            c = lerp(c, GD["ivory"], 0.5 + 0.4 * out)
        elif kind == "palm":
            if face in ("top", "bottom"):
                if (tx + ty // 2) % 4 == 0:
                    c = shade(c, 0.86)                     # grooves
                if face == "bottom":
                    c = shade(c, 0.9)
            else:
                c = lerp(c, GD["ivory"], 0.45)             # worn pale rim
        elif kind == "beam" and (ty + tx) % 3 == 0:
            c = shade(c, 0.88)                             # pearling
        n_ = (h(tx, ty, 28) - 0.5) * 0.08
        return shade(c, 1 + n_)
    return mat


def mirror_box(x0, w):
    return -(x0 + w)


def antler_parts(side):
    """side +1 = the deer's left. Boxes for the right side are the left ones mirrored in x (same UVs, mirror flag)."""
    s = "left_" if side > 0 else "right_"
    m = side < 0

    def bx(key, x0, y0, z0, w, hh, d, mat):
        return cube(key, x0 if side > 0 else mirror_box(x0, w), y0, z0, w, hh, d, mat, m)
    return [
        part(s + "beam", "antlers", (2.5 * side, 0, 0), (-0.15, 0, -0.22 * side), [
            bx("gd_burr", -0.5, -1.5, -1.5, 1, 3, 3, antler_mat("burr")),
            bx("gd_beam", 0, -1, -1, 8, 2, 2, antler_mat("beam")),
        ]),
        part(s + "brow_tine", s + "beam", (2 * side, 0, -0.5), (-0.4, 0.25 * side, 0), [bx("gd_brow", -0.5, -0.5, -6, 1, 1, 6, antler_mat("tine"))]),
        part(s + "bez_tine", s + "beam", (5 * side, 0, -0.5), (-0.7, 0.1 * side, 0), [bx("gd_bez", -0.5, -0.5, -4, 1, 1, 4, antler_mat("tine"))]),
        part(s + "palm", s + "beam", (8 * side, 0, 0), (0.45, 0, 0.0), [
            bx("gd_palm", 0, -0.5, -5, 12, 1, 11, antler_mat("palm")),
            bx("gd_tine_f1", 3, -0.5, -8, 1, 1, 3, antler_mat("tine")),
            bx("gd_tine_f2", 6, -0.5, -8.5, 1, 1, 4, antler_mat("tine")),
            bx("gd_tine_f3", 9, -0.5, -8, 1, 1, 3, antler_mat("tine")),
            bx("gd_tine_o1", 12, -0.5, -4, 3, 1, 1, antler_mat("tine")),
            bx("gd_tine_o2", 12, -0.5, -0.5, 4, 1, 1, antler_mat("tine")),
            bx("gd_tine_o3", 12, -0.5, 3, 3, 1, 1, antler_mat("tine")),
            bx("gd_tine_b", 10, -0.5, 6, 1, 1, 3, antler_mat("tine")),
        ]),
    ]


GIANT_DEER = [
    part("body", None, (0, 7, 0), (0, 0, 0), [
        cube("gd_barrel", -5, -5, -11, 10, 10, 22, body_mat),
        cube("gd_chest", -5.5, -5.5, -12, 11, 12, 8, body_mat),
        cube("gd_hump", -4, -8, -11, 8, 3, 9, hump_mat),
    ]),
    part("tail", "body", (0, -4, 11), (0.3, 0, 0), [cube("gd_tail", -1.5, 0, 0, 3, 5, 1, tail_mat)]),
    part("neck", "body", (0, -3, -10), (0.75, 0, 0), [cube("gd_neck", -2.5, -12, -3, 5, 13, 6, neck_mat)]),
    part("mane", "neck", (0, 0, 0), (0, 0, 0), [cube("gd_mane", -3.5, -8, -4.5, 7, 9, 6, mane_mat)]),
    part("head", "neck", (0, -11.5, 0), (-0.65, 0, 0), [
        cube("gd_skull", -3, -4, -5, 6, 6, 7, skull_mat),
        cube("gd_muzzle", -2, -1.5, -11, 4, 4, 6, muzzle_mat),
    ]),
    part("left_ear", "head", (3, -3, 1.5), (0, 0, 1.0), [cube("gd_ear", -1, -4, -0.5, 2, 4, 1, ear_mat)]),
    part("right_ear", "head", (-3, -3, 1.5), (0, 0, -1.0), [cube("gd_ear", -1, -4, -0.5, 2, 4, 1, ear_mat, True)]),
    part("antlers", "head", (0, -4, -1), (0, 0, 0)),
    *antler_parts(1),
    *antler_parts(-1),
    part("left_front_leg", None, (3.2, 12, -8.5), (0, 0, 0), [cube("gd_fleg", -2, -1, -2, 4, 9, 4, leg_mat(17.0))]),
    part("left_front_lower", "left_front_leg", (0, 8, 0), (0, 0, 0), [cube("gd_flow", -1.5, 0, -1.5, 3, 4, 3, leg_mat(17.0))]),
    part("right_front_leg", None, (-3.2, 12, -8.5), (0, 0, 0), [cube("gd_fleg", -2, -1, -2, 4, 9, 4, leg_mat(17.0), True)]),
    part("right_front_lower", "right_front_leg", (0, 8, 0), (0, 0, 0), [cube("gd_flow", -1.5, 0, -1.5, 3, 4, 3, leg_mat(17.0), True)]),
    part("left_hind_leg", None, (3, 12, 8.5), (0, 0, 0), [cube("gd_hleg", -2, -1, -2.5, 4, 10, 5, leg_mat(18.5))]),
    part("left_hind_lower", "left_hind_leg", (0, 9, 0.5), (0, 0, 0), [cube("gd_hlow", -1.5, 0, -1.5, 3, 3, 3, leg_mat(18.5))]),
    part("right_hind_leg", None, (-3, 12, 8.5), (0, 0, 0), [cube("gd_hleg", -2, -1, -2.5, 4, 10, 5, leg_mat(18.5), True)]),
    part("right_hind_lower", "right_hind_leg", (0, 9, 0.5), (0, 0, 0), [cube("gd_hlow", -1.5, 0, -1.5, 3, 3, 3, leg_mat(18.5), True)]),
]
SIZE = (128, 128)


def build():
    uv = pack(GIANT_DEER, *SIZE)
    return paint(GIANT_DEER, uv, SIZE), uv


def main():
    img, uv = build()
    if "--java" in sys.argv:
        java_cubes(GIANT_DEER, uv)
    save(img, "wildlife/giant_deer.png")
    # flesh version; the antlers and hooves stay as they are
    flesh = skinned(img, {}, ())
    src, dst = img.load(), flesh.load()
    keep = {c["key"] for p in GIANT_DEER for c in p["cubes"] if c["key"].startswith(("gd_burr", "gd_beam", "gd_brow", "gd_bez", "gd_palm", "gd_tine"))}
    from wildlife_paint import face_texels
    for p in GIANT_DEER:
        for c in p["cubes"]:
            if c["key"] in keep:
                u, v = uv[c["key"]]
                _, _, _, w, hh, d = c["box"]
                for _f, x, y, *_ in face_texels(u, v, 0, 0, 0, int(w), int(hh), int(d)):
                    dst[x, y] = src[x, y]
    save(flesh, "skinned/wildlife/giant_deer.png")


if __name__ == "__main__":
    main()
