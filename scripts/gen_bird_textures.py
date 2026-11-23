"""Flocking birds (crow, goose, rock dove, ptarmigan, seagull): model geometry and painted entity textures.

Usage: python scripts/gen_bird_textures.py
Also the plucked carcass textures (textures/entity/skinned/wildlife/, see CarcassRenderer) of the edible ones.
One part tree (geometry()) per species from a few dimensions; it is written into BirdModel.java between the
GENERATED markers (createXxxLayer methods) so the Java model and the textures can't drift apart, and every texel is
painted from where it sits on its cube (cube-local coordinates, plus model space at the rest pose).

Wings: an inner wing hangs folded along the body side; the outer wing ("tip", the primaries) is its child, folded back
along the lower edge of the wing and past the tail at rest, swung out in line with the inner wing in flight. Wing
painters get the position along the whole span (s: 0 at the shoulder, 1 at the tip) and along the chord (c: 0 leading,
1 trailing edge); the outer face ("right" on the right wing, mirrored on the left) is the upper side in flight.
Deterministic: hashes only, no randomness.
"""
import math
import re
from pathlib import Path

from critter_paint import NORMALS, mv, pack, save, world
from wildlife_paint import face_texels, h, lerp, shade
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
MODEL_JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/BirdModel.java"
SIZE = (64, 32)
HALF_PI = math.pi / 2

# body (w, h, d); leg cube length (+1 for the foot); neck (w, length); head (w, h, d); beak (w, h, d) and its row from
# the top of the head; wing (inner span, chord, outer span, outer chord); tail (w, length) and its tilt
SPECIES = {
    "crow": dict(body=(4, 4, 7), leg=3, leg_x=1, foot=(2, 3), neck=(2, 1), head=(3, 3, 3), beak=(1, 1, 2), beak_y=1,
                 wing=(3, 6, 4, 2), wing_z=1, tail=(3, 4), tail_rot=-0.15, eye=(1, 1)),
    "goose": dict(body=(5, 5, 9), leg=3, leg_x=1.5, foot=(3, 3), neck=(2, 6), head=(3, 3, 4), beak=(2, 1, 2), beak_y=1,
                  wing=(4, 8, 5, 3), wing_z=1, tail=(3, 3), tail_rot=-0.1, eye=(1, 1)),
    "rock_dove": dict(body=(3, 3, 6), leg=2, leg_x=0.5, foot=(1, 2), neck=(2, 2), head=(2, 2, 3), beak=(1, 1, 1), beak_y=1,
                      wing=(2, 5, 4, 2), wing_z=1, tail=(2, 4), tail_rot=-0.1, eye=(0, 1)),
    "ptarmigan": dict(body=(4, 4, 6), leg=2, leg_x=1, foot=(2, 2), neck=(2, 1), head=(3, 3, 3), beak=(1, 1, 1), beak_y=1,
                      wing=(3, 5, 3, 2), wing_z=1, tail=(3, 3), tail_rot=-0.2, eye=(1, 1), leg_w=2),
    "seagull": dict(body=(4, 4, 7), leg=3, leg_x=1, foot=(2, 3), neck=(2, 1), head=(3, 3, 3), beak=(1, 1, 2), beak_y=1,
                    wing=(3, 6, 7, 3), wing_z=1.5, tail=(3, 3), tail_rot=-0.1, eye=(1, 1)),
}


def part(name, parent, pivot, rot, cubes):
    return dict(name=name, parent=parent, pivot=pivot, rot=rot, cubes=cubes)


def cube(key, x0, y0, z0, w, hh, d, mirror=False):
    return dict(key=key, box=(x0, y0, z0, w, hh, d), mirror=mirror)


def geometry(S):
    bw, bh, bd = S["body"]
    legc = S["leg"]
    leg_py = 23 - legc
    by = leg_py + 1 - bh / 2
    nw, nl = S["neck"]
    hw, hh, hd = S["head"]
    kw, kh, kd = S["beak"]
    wh, wd, th, td = S["wing"]
    tw, tl = S["tail"]
    lw = S.get("leg_w", 1)
    fw, fd = S["foot"]
    head_z0 = 1.5 - hd
    return [
        part("body", None, (0, by, 0), (0, 0, 0), [cube("body", -bw / 2, -bh / 2, -bd / 2, bw, bh, bd)]),
        part("neck", "body", (0, -bh / 2 + 1, -bd / 2 + 1), (0, 0, 0), [cube("neck", -nw / 2, -nl, -nw / 2, nw, nl + 1, nw)]),
        part("head", "neck", (0, -nl, 0), (0, 0, 0), [cube("head", -hw / 2, -hh + 0.5, head_z0, hw, hh, hd),
                                                      cube("beak", -kw / 2, -hh + 0.5 + S["beak_y"], head_z0 - kd, kw, kh, kd)]),
        part("right_wing", "body", (-bw / 2, -bh / 2 + 0.5, -bd / 2 + S["wing_z"]), (0, 0, 0), [cube("wing", -1, 0, 0, 1, wh, wd)]),
        part("left_wing", "body", (bw / 2, -bh / 2 + 0.5, -bd / 2 + S["wing_z"]), (0, 0, 0), [cube("wing", 0, 0, 0, 1, wh, wd, True)]),
        part("right_wing_tip", "right_wing", (-0.5, wh, 0), (HALF_PI, 0, 0), [cube("tip", -1, 0, 0, 1, th, td)]),
        part("left_wing_tip", "left_wing", (0.5, wh, 0), (HALF_PI, 0, 0), [cube("tip", 0, 0, 0, 1, th, td, True)]),
        part("tail", "body", (0, -bh / 2 + 1, bd / 2 - 0.5), (S["tail_rot"], 0, 0), [cube("tail", -tw / 2, 0, 0, tw, 1, tl)]),
        part("right_leg", None, (-S["leg_x"], leg_py, 0.5), (0, 0, 0), [cube("leg", -lw / 2, 0, -lw / 2, lw, legc, lw),
                                                                    cube("foot", -fw / 2, legc, 0.5 - fd, fw, 1, fd)]),
        part("left_leg", None, (S["leg_x"], leg_py, 0.5), (0, 0, 0), [cube("leg", -lw / 2, 0, -lw / 2, lw, legc, lw, True),
                                                                  cube("foot", -fw / 2, legc, 0.5 - fd, fw, 1, fd, True)]),
    ]


class P:
    """What a painter sees for one texel."""
    __slots__ = ("face", "x", "y", "z", "fx", "fy", "fz", "ix", "iy", "iz", "X", "Y", "Z", "n", "tx", "ty", "s", "c", "upper")


def paint(parts, uv, S, painters):
    W, H = SIZE
    img = Image.new("RGBA", SIZE, (0, 0, 0, 0))
    px = img.load()
    done, owner = set(), {}
    wh, wd, th, td = S["wing"]
    for prt in parts:
        R, t = world(parts, prt["name"])
        for c in prt["cubes"]:
            if c["key"] in done:
                continue
            done.add(c["key"])
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            for face, tx, ty, x, y, z in face_texels(u, v, x0, y0, z0, int(w), int(hh), int(d)):
                assert 0 <= tx < W and 0 <= ty < H, (c["key"], tx, ty)
                assert owner.setdefault((tx, ty), c["key"]) == c["key"], f"UV overlap {c['key']} / {owner[(tx, ty)]}"
                p = P()
                p.face, p.x, p.y, p.z, p.tx, p.ty = face, x, y, z, tx, ty
                p.fx, p.fy, p.fz = (x - x0) / w, (y - y0) / hh, (z - z0) / d
                p.ix, p.iy, p.iz = int(x - x0), int(y - y0), int(z - z0)
                p.X, p.Y, p.Z = (a + b for a, b in zip(mv(R, (x, y, z)), t))
                p.n = mv(R, NORMALS[face])
                p.s = p.c = 0.0
                p.upper = face == "right"
                if c["key"] == "wing":
                    p.s, p.c = (y - y0) / (wh + th), (z - z0) / wd
                elif c["key"] == "tip":
                    p.s, p.c = (wh + y - y0) / (wh + th), (z - z0) / wd
                col = painters[c["key"]](p)
                px[tx, ty] = col if len(col) == 4 else (*col, 255)
    return img


def fe(c, p, salt, amt=0.06):
    """Feather speckle."""
    return shade(c, 1 + (h(p.tx, p.ty, salt) - 0.5) * 2 * amt)


def side(p):
    return p.face in ("left", "right")


def is_eye(p, S):
    return side(p) and (p.iy, p.iz) == S["eye"]


def feathers_along(p, c, salt):
    """Flight feathers: alternating light/dark strips across the span, darker tips at the trailing edge."""
    k = 0.94 if int(p.y) % 2 else 1.0
    if p.c > 0.85:
        k *= 0.9
    return fe(shade(c, k), p, salt, 0.04)


# ======================================================================= hooded crow
def crow_painters(S):
    BLACK, GLOSS, SOOT = (30, 30, 36), (46, 50, 70), (42, 42, 48)
    GREY, GREY_D, GREY_L = (150, 148, 146), (122, 120, 119), (166, 164, 162)
    BEAK, LEG = (38, 36, 38), (26, 24, 26)

    def black(p, salt):
        return fe(lerp(BLACK, GLOSS, 0.6 if p.face == "top" else 0.25 if side(p) else 0), p, salt, 0.08)

    def body(p):
        if p.face == "front":
            return black(p, 1) if p.fy < 0.5 else fe(GREY_L, p, 2)  # black bib on the upper breast
        if p.face == "top":
            return fe(GREY_D, p, 3)
        if p.face == "bottom":
            return fe(GREY_L, p, 4)
        if side(p) and p.fz < 0.15 and p.fy < 0.5:
            return black(p, 5)
        return fe(lerp(GREY_D, GREY, p.fy), p, 6)

    def neck(p):
        if p.face == "front" or side(p) and p.fz < 0.5:
            return black(p, 7)
        return fe(GREY_D, p, 8)

    def head(p):
        if is_eye(p, S):
            return (10, 8, 8)
        return black(p, 9)

    def beak(p):
        return shade(BEAK, 1.5) if p.face == "top" else BEAK

    def wing(p):
        if not p.upper:
            return fe(SOOT, p, 10)
        return feathers_along(p, lerp(BLACK, GLOSS, 0.45 - 0.3 * p.s), 11)

    def tail(p):
        return black(p, 12) if p.face != "bottom" else fe(SOOT, p, 13)

    def leg(p):
        return fe(LEG, p, 14, 0.1)

    return dict(body=body, neck=neck, head=head, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# ======================================================================= greylag goose
def goose_painters(S):
    BACK, EDGE, FLANK = (118, 108, 96), (178, 168, 152), (132, 124, 114)
    BELLY, BREAST, WHITE = (198, 192, 182), (170, 160, 146), (240, 238, 232)
    NECK, HEAD = (120, 112, 100), (130, 120, 106)
    BEAK, NAIL, LEG = (236, 134, 58), (240, 222, 194), (232, 160, 150)
    FORE, PRIM, SEC, UNDER = (168, 174, 176), (72, 72, 76), (92, 88, 82), (184, 186, 186)

    def barred(c, p, along, salt):
        """Pale feather edges in bars (scalloped back and flanks)."""
        if int(along) % 2 == 0 and h(p.tx, p.ty, salt) < 0.85:
            c = lerp(c, EDGE, 0.45)
        return fe(c, p, salt + 1, 0.05)

    def body(p):
        if p.face == "top":
            return barred(BACK, p, p.Z, 1)
        if p.face == "bottom":
            return fe(WHITE if p.fz > 0.75 else lerp(BELLY, FLANK, 0.25 * (h(p.tx, p.ty, 3) > 0.8)), p, 4)
        if p.face == "back":
            return fe(BACK if p.fy < 0.4 else WHITE, p, 5)
        if p.face == "front":
            return fe(BREAST if p.fy < 0.6 else BELLY, p, 6)
        if p.fy > 0.65:
            return fe(BELLY if p.fz < 0.8 else WHITE, p, 7)
        return barred(lerp(FLANK, BACK, 0.6 - p.fy), p, p.Z, 8)

    def neck(p):
        c = NECK
        if side(p) and int(p.y * 2) % 3 == 0:
            c = shade(c, 0.88)  # the furrowed neck feathers
        return fe(c, p, 9, 0.05)

    def head(p):
        if is_eye(p, S):
            return (22, 16, 14)
        if side(p) and (p.iy, p.iz) == (S["eye"][0], S["eye"][1] - 1):
            return (214, 128, 70)  # orange eye ring in front of the eye
        return fe(HEAD, p, 10, 0.05)

    def beak(p):
        if p.face == "front":
            return NAIL
        return shade(BEAK, 0.9) if p.face == "bottom" else BEAK

    def wing(p):
        if not p.upper:
            return fe(lerp(UNDER, (128, 128, 128), max(0.0, p.c - 0.6) * 2), p, 11)
        if p.s > 0.5:
            c = lerp(FORE, PRIM, (p.s - 0.45) / 0.3)
        elif p.c < 0.5:
            c = lerp(FORE, EDGE, 0.4) if int(p.z) % 2 == 0 else FORE  # pale grey forewing in rows
        else:
            c = SEC if p.c < 0.88 else EDGE
        return feathers_along(p, c, 12)

    def tail(p):
        if p.face == "bottom" or side(p) or p.fz > 0.7:
            return fe(WHITE, p, 13, 0.03)
        return fe(BACK, p, 14)

    def leg(p):
        return fe(LEG, p, 15, 0.05)

    return dict(body=body, neck=neck, head=head, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# ======================================================================= rock dove
def dove_painters(S):
    BODY, MANTLE, PALE = (150, 156, 170), (180, 184, 194), (204, 208, 216)
    HEAD, BREAST = (110, 116, 134), (162, 150, 166)
    GREEN, PURPLE = (74, 138, 104), (136, 92, 146)
    BAR, TIP, BAND = (34, 34, 42), (72, 74, 84), (40, 40, 48)
    EYE, BEAK, CERE, FEET = (232, 118, 40), (52, 48, 48), (238, 238, 234), (204, 88, 88)

    def sheen(p, salt):
        return fe(GREEN if h(p.tx, p.ty, salt) < 0.55 else PURPLE, p, salt + 1, 0.08)

    def body(p):
        if p.face == "top":
            return fe(MANTLE, p, 1)
        if p.face == "front":
            return sheen(p, 2) if p.fy < 0.34 else fe(BREAST, p, 3)
        if p.face == "back":
            return fe(PALE if p.fy < 0.5 else BODY, p, 4)  # pale rump
        if p.face == "bottom":
            return fe(PALE, p, 5)
        return fe(BODY, p, 6)

    def neck(p):
        return fe(HEAD, p, 7) if p.face == "back" else sheen(p, 8)

    def head(p):
        if is_eye(p, S):
            return EYE
        return fe(HEAD, p, 9)

    def beak(p):
        return CERE if p.face == "top" else BEAK

    def wing(p):
        if not p.upper:
            return fe(PALE, p, 10, 0.03)
        if p.s > 0.6:
            c = lerp(MANTLE, TIP, (p.s - 0.55) / 0.35)
        elif p.s < 0.5 and (0.5 <= p.c < 0.64 or 0.78 <= p.c < 0.92):
            c = BAR  # the two black wing bars
        else:
            c = MANTLE
        return feathers_along(p, c, 11)

    def tail(p):
        return fe(BAND if p.fz > 0.7 else BODY, p, 12)

    def leg(p):
        return fe(FEET, p, 13, 0.05)

    return dict(body=body, neck=neck, head=head, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# ======================================================================= common gull ("fiskmås")
def gull_painters(S):
    WHITE, SHADE, MANTLE = (244, 244, 240), (222, 226, 230), (152, 164, 180)
    BLACK, BEAK, TIP, LEG = (26, 26, 30), (214, 204, 84), (232, 214, 96), (198, 196, 98)

    def body(p):
        if p.face == "top":
            return fe(MANTLE, p, 1, 0.04)
        if p.face == "bottom":
            return fe(SHADE, p, 2, 0.03)
        if side(p) and p.fy < 0.3:
            return fe(MANTLE, p, 3, 0.04)
        return fe(WHITE, p, 4, 0.03)

    def white(p):
        return fe(SHADE if p.face == "bottom" else WHITE, p, 5, 0.03)

    def head(p):
        if is_eye(p, S):
            return (44, 38, 30)
        return white(p)

    def beak(p):
        return TIP if p.face == "front" or p.iz == 0 else BEAK  # greenish-yellow bill, brighter at the tip

    def wing(p):
        if not p.upper:
            return fe((96, 96, 102) if p.s > 0.76 else SHADE, p, 6, 0.03)
        if p.s > 0.7:
            mirror = p.s > 0.9 and (int(p.y) + int(p.z)) % 3 == 0 or p.s > 0.97
            return WHITE if mirror else feathers_along(p, BLACK, 7)  # black tip with white "mirrors"
        if p.c > 0.86 or p.face == "front":
            return fe(WHITE, p, 8, 0.03)  # white trailing edge
        return feathers_along(p, MANTLE, 9)

    def leg(p):
        return fe(LEG, p, 10, 0.05)

    return dict(body=body, neck=white, head=head, beak=beak, wing=wing, tip=wing, tail=white, leg=leg, foot=leg)


# ======================================================================= willow ptarmigan
def ptarmigan_painters(S, winter):
    W, WS, BLACK, COMB = (242, 243, 246), (214, 218, 228), (28, 26, 26), (210, 40, 36)
    BROWN, DARK, BUFF, CHEST = (132, 82, 48), (70, 44, 28), (186, 140, 90), (150, 72, 42)

    def mottled(c, p, salt):
        r = h(p.tx, p.ty, salt)
        if r < 0.28 or int(p.Z) % 2 == 0 and r < 0.45:
            c = DARK
        elif r > 0.86:
            c = BUFF
        return fe(c, p, salt + 1, 0.06)

    def white(p, salt=1):
        return fe(WS if p.face in ("bottom", "back") else W, p, salt, 0.03)

    def body(p):
        if winter or p.face == "bottom" or side(p) and p.fy > 0.6:
            return white(p, 2)
        return mottled(CHEST if p.face == "front" else BROWN, p, 3)

    def neck(p):
        return white(p, 4) if winter else mottled(CHEST, p, 5)

    def head(p):
        if is_eye(p, S):
            return BLACK
        ey, ez = S["eye"]
        if side(p) and p.iy == ey - 1 and (p.iz == ez or not winter and p.iz == ez - 1):
            return COMB  # red comb over the eye
        return white(p, 6) if winter else mottled(CHEST, p, 7)

    def beak(p):
        return BLACK

    def wing(p):
        if not winter and p.upper and p.s < 0.3 and p.c < 0.4:
            return mottled(BROWN, p, 8)
        return fe(lerp(W, WS, 0.5 if int(p.y) % 2 else 0) if p.upper else W, p, 9, 0.03)

    def tail(p):
        if p.face == "top" and abs(p.x) < 0.6:
            return white(p, 10)  # white coverts hide the middle of the black tail
        return fe(BLACK, p, 11)

    def leg(p):
        return white(p, 12)  # feathered feet

    return dict(body=body, neck=neck, head=head, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# ======================================================================= plucked (carcass after the first knife pass)
def plucked_painters(S, base, light, dark):
    """Bare poultry skin with goose-bump follicles, darker underneath and in the folds; eyes, beak and (bare) legs keep the
    bird's own colours, the tail and the trailing edge of the wing become rows of pale quill stubs. Feathered feet
    (ptarmigan) turn to skin like the rest."""
    QUILL, QUILL_D = (236, 226, 202), (204, 188, 156)

    def skin(p, salt=40):
        c = lerp(light, dark, 0.3 * h(p.tx // 2, p.ty, salt) + (0.35 if p.face == "bottom" else 0.12 if p.face == "back" else 0.0))
        if h(p.tx, p.ty, salt + 1) < 0.16:
            c = shade(c, 0.86)  # follicle
        return c

    def head(p):
        return base["head"](p) if is_eye(p, S) else skin(p, 42)

    def quills(p):
        return QUILL if int(p.x * 2) % 2 else QUILL_D

    def wing(p):
        if p.c > 0.75 and int(p.y) % 2 == 0:
            return quills(p)
        return skin(p, 44)

    def tail(p):
        return quills(p) if p.face in ("top", "bottom", "back") else shade(QUILL_D, 0.92)

    def leg(p):
        col = base["leg"](p)
        return skin(p, 46) if min(col[:3]) > 200 else col  # white feathered feet: plucked bare

    return dict(body=skin, neck=lambda p: skin(p, 48), head=head, beak=base["beak"], wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


# texture -> (species, its painters, light and dark skin): goose pale yellow, the pigeon and the grouse dark pink
PLUCKED = {
    "goose": ("goose", goose_painters, (244, 216, 180), (208, 160, 124)),
    "rock_dove": ("rock_dove", dove_painters, (226, 160, 150), (178, 104, 100)),
    "ptarmigan": ("ptarmigan", lambda S: ptarmigan_painters(S, False), (234, 180, 162), (188, 120, 108)),
    "ptarmigan_winter": ("ptarmigan", lambda S: ptarmigan_painters(S, True), (234, 180, 162), (188, 120, 108)),
}


TEXTURES = {
    "crow": ("crow", crow_painters),
    "goose": ("goose", goose_painters),
    "rock_dove": ("rock_dove", dove_painters),
    "seagull": ("seagull", gull_painters),
    "ptarmigan": ("ptarmigan", lambda S: ptarmigan_painters(S, False)),
    "ptarmigan_winter": ("ptarmigan", lambda S: ptarmigan_painters(S, True)),
}


# ---------------------------------------------------------------- Java
def jf(v):
    v = round(v, 4)
    return f"{v}F" if v != int(v) else f"{int(v)}.0F"


def camel(name):
    a, *rest = name.split("_")
    return a + "".join(r.title() for r in rest)


def java_layer(species, parts, uv):
    out = [f"    public static LayerDefinition create{''.join(w.title() for w in species.split('_'))}Layer() {{",
           "        MeshDefinition mesh = new MeshDefinition();",
           "        PartDefinition root = mesh.getRoot();"]
    for p in parts:
        cubes = "".join(
            f".texOffs({uv[c['key']][0]}, {uv[c['key']][1]}){'.mirror()' if c['mirror'] else ''}.addBox("
            + ", ".join(jf(v) for v in c["box"]) + ")" for c in p["cubes"])
        pose = ", ".join(jf(v) for v in (*p["pivot"], *p["rot"]))
        out.append(f"        PartDefinition {camel(p['name'])} = {camel(p['parent']) if p['parent'] else 'root'}.addOrReplaceChild(\"{p['name']}\",")
        out.append(f"            CubeListBuilder.create(){cubes},")
        out.append(f"            PartPose.offsetAndRotation({pose}));")
    out += [f"        return LayerDefinition.create(mesh, {SIZE[0]}, {SIZE[1]});", "    }"]
    return "\n".join(out)


def main():
    layers = []
    built = {}
    for species, S in SPECIES.items():
        parts = geometry(S)
        uv = pack(parts, *SIZE)
        built[species] = (parts, uv)
        layers.append(java_layer(species, parts, uv))
    for tex, (species, painters) in TEXTURES.items():
        parts, uv = built[species]
        S = SPECIES[species]
        save(paint(parts, uv, S, painters(S)), f"wildlife/{tex}.png")
    for tex, (species, painters, light, dark) in PLUCKED.items():
        parts, uv = built[species]
        S = SPECIES[species]
        save(paint(parts, uv, S, plucked_painters(S, painters(S), light, dark)), f"skinned/wildlife/{tex}.png")
    src = MODEL_JAVA.read_text(encoding="utf-8")
    begin, end = "    // BEGIN GENERATED by scripts/gen_bird_textures.py\n", "    // END GENERATED\n"
    assert begin in src and end in src, "markers missing in BirdModel.java"
    new = re.sub(re.escape(begin) + ".*?" + re.escape(end), lambda m: begin + "\n\n".join(layers) + "\n" + end, src, flags=re.S)
    MODEL_JAVA.write_text(new, encoding="utf-8")
    print("wrote", MODEL_JAVA.relative_to(ROOT))


if __name__ == "__main__":
    main()
    import gen_small_models  # these animals now have their own models and skins, which this would overwrite
    gen_small_models.main()
