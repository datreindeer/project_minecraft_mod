"""Waterfowl (Mallard, Mute Swan, Grey Heron, Common Crane): model geometry, entity textures, plucked carcass textures
and the 16x16 item / nest-egg textures.

Usage: python scripts/gen_fauna_waterfowl_textures.py
- Mallard and Mute Swan use the flock-bird part tree of gen_bird_textures.py (BirdModel); their createXxxLayer methods are
  written into client/wildlife/waterfowl/WaterfowlBirdModel.java between the GENERATED markers.
- Grey Heron and Common Crane are long-legged waders (WaderModel): a two-piece S-neck (neck, neck2), the head with a long
  bill (heron: and its black crest plume), folded wings with their primaries, a short tail (crane: the drooping "bustle"
  of tertials) and stilt legs in two pieces (leg = the bare tibia, shin = the tarsus with the long toes) bending at the
  ankle. Written into client/wildlife/waterfowl/WaderModel.java.
- Every texel is painted from where it sits on its cube (gen_bird_textures.paint); plucked versions with its
  plucked_painters (textures/entity/skinned/wildlife/, see CarcassRenderer).
Deterministic (hashes only).
"""
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_bird_textures as gb  # noqa: E402
import gen_birds2_textures as b2  # noqa: E402
import gen_wildlife_item_textures as wit  # noqa: E402
from critter_paint import pack, save  # noqa: E402
from wildlife_paint import h, lerp, shade  # noqa: E402
from PIL import Image  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/waterfowl"
HALF_PI = math.pi / 2
fe, side, is_eye, feathers_along = gb.fe, gb.side, gb.is_eye, gb.feathers_along

# ======================================================================= geometry
FLOCK = {  # gen_bird_textures.SPECIES format; texture size
    "mallard": (dict(body=(4, 4, 7), leg=2, leg_x=1, foot=(2, 2), neck=(2, 2), head=(3, 3, 4), beak=(2, 1, 3), beak_y=2,
                     wing=(3, 6, 4, 2), wing_z=1, tail=(3, 2), tail_rot=0.2, eye=(1, 2)), (64, 32)),
    "mute_swan": (dict(body=(6, 6, 11), leg=3, leg_x=1.5, foot=(3, 3), neck=(2, 10), head=(3, 3, 4), beak=(2, 2, 3), beak_y=1,
                       wing=(5, 9, 7, 4), wing_z=1, tail=(4, 3), tail_rot=0.15, eye=(1, 1)), (64, 64)),
}

# waders: body (w, h, d) and its rest pitch; neck pieces (length, rest pitch); head (w, h, d); bill (w, h, d) and its row;
# plume (crest, d) or None; wing (inner span, chord, outer span, outer chord); tail (w, h, length, tilt); legs: x,
# tibia and tarsus length, toes (w, d)
WADERS = {
    "grey_heron": dict(body=(4, 5, 9), body_rot=-0.3, neck=((5, 0.55), (5, -0.9)), head=(2, 2, 3), beak=(1, 1, 5), beak_y=1,
                       plume=3, wing=(4, 8, 6, 3), wing_z=1, tail=(3, 1, 3, -0.3), leg_x=1, tibia=5, tarsus=6, foot=(3, 4),
                       eye=(0, 1)),
    "common_crane": dict(body=(5, 5, 10), body_rot=-0.1, neck=((6, 0.15), (5, 0.05)), head=(2, 3, 3), beak=(1, 1, 3), beak_y=1,
                         plume=None, wing=(4, 9, 7, 3), wing_z=1, tail=(4, 2, 5, -0.7), leg_x=1, tibia=6, tarsus=7, foot=(3, 3),
                         eye=(1, 1)),
}
WADER_SIZE = (64, 64)


def wader_geometry(S):
    part, cube = gb.part, gb.cube
    bw, bh, bd = S["body"]
    lt, ls = S["tibia"], S["tarsus"]
    leg_py = 23 - lt - ls
    by = leg_py + 1 - bh / 2
    (n1, r1), (n2, r2) = S["neck"]
    hw, hh, hd = S["head"]
    kw, kh, kd = S["beak"]
    wh, wd, th, td = S["wing"]
    tw, tt, tl, trot = S["tail"]
    fw, fd = S["foot"]
    head_rot = -(S["body_rot"] + r1 + r2)  # head held level at rest
    head_z0 = 1.5 - hd
    head_cubes = [cube("head", -hw / 2, -hh + 0.5, head_z0, hw, hh, hd),
                  cube("beak", -kw / 2, -hh + 0.5 + S["beak_y"], head_z0 - kd, kw, kh, kd)]
    if S["plume"]:
        head_cubes.append(cube("plume", -0.5, -hh + 0.5, head_z0 + hd, 1, 1, S["plume"]))
    parts = [
        part("body", None, (0, by, 0), (S["body_rot"], 0, 0), [cube("body", -bw / 2, -bh / 2, -bd / 2, bw, bh, bd)]),
        part("neck", "body", (0, -bh / 2 + 1, -bd / 2 + 1), (r1, 0, 0), [cube("neck", -1, -n1, -1, 2, n1 + 1, 2)]),
        part("neck2", "neck", (0, -n1, 0), (r2, 0, 0), [cube("neck2", -1, -n2, -1, 2, n2 + 1, 2)]),
        part("head", "neck2", (0, -n2, 0), (head_rot, 0, 0), head_cubes),
        part("right_wing", "body", (-bw / 2, -bh / 2 + 0.5, -bd / 2 + S["wing_z"]), (0, 0, 0), [cube("wing", -1, 0, 0, 1, wh, wd)]),
        part("left_wing", "body", (bw / 2, -bh / 2 + 0.5, -bd / 2 + S["wing_z"]), (0, 0, 0), [cube("wing", 0, 0, 0, 1, wh, wd, True)]),
        part("right_wing_tip", "right_wing", (-0.5, wh, 0), (HALF_PI, 0, 0), [cube("tip", -1, 0, 0, 1, th, td)]),
        part("left_wing_tip", "left_wing", (0.5, wh, 0), (HALF_PI, 0, 0), [cube("tip", 0, 0, 0, 1, th, td, True)]),
        part("tail", "body", (0, -bh / 2 + 1, bd / 2 - 1.5), (trot, 0, 0), [cube("tail", -tw / 2, 0, 0, tw, tt, tl)]),
    ]
    for sd, x in (("right", -S["leg_x"]), ("left", S["leg_x"])):
        m = sd == "left"
        parts.append(part(f"{sd}_leg", None, (x, leg_py, 0.5), (0.2, 0, 0), [cube("leg", -0.5, 0, -0.5, 1, lt, 1, m)]))
        parts.append(part(f"{sd}_shin", f"{sd}_leg", (0, lt, 0), (-0.2, 0, 0),
                          [cube("shin", -0.5, 0, -0.5, 1, ls, 1, m), cube("foot", -fw / 2, ls, 0.5 - fd, fw, 1, fd, m)]))
    return parts


# ======================================================================= painters
def mallard_painters(S, drake):
    GREEN, GLOSS, PURPLE = (30, 92, 52), (52, 140, 84), (60, 54, 110)
    RING, CHEST, CHEST_D = (240, 240, 236), (112, 58, 40), (86, 42, 30)
    FLANK, FLANK_D, BACK, BACK_D = (196, 196, 194), (168, 168, 168), (110, 98, 84), (80, 70, 60)
    BLACK, TAILW = (24, 24, 28), (228, 226, 220)
    BUFF, BROWN, DARK = (190, 150, 104), (140, 100, 64), (70, 48, 32)  # hen
    BILL_D, BILL_H, NAIL = (214, 196, 66), (210, 120, 40), (30, 26, 22)
    SPEC, SPEC_L, WHITE, WING, WING_D = (44, 64, 170), (80, 104, 206), (238, 238, 234), (120, 114, 108), (90, 86, 82)
    LEG = (232, 128, 42)

    def mottled(p, salt):
        r = h(p.tx, p.ty, salt)
        c = BUFF if r > 0.55 else BROWN if r > 0.2 else DARK
        if int(p.Z) % 2 == 0 and r > 0.4:
            c = lerp(c, BUFF, 0.4)  # pale feather fringes in rows
        return fe(c, p, salt + 1, 0.05)

    def vermic(c, p, salt):
        """The drake's fine grey wavy barring."""
        return fe(shade(c, 0.9) if (int(p.y * 2) + int(p.z)) % 3 == 0 else c, p, salt, 0.03)

    def body(p):
        if not drake:
            if p.face == "bottom":
                return fe(lerp(BUFF, (214, 190, 150), 0.5), p, 1, 0.05)
            return mottled(p, 2)
        if p.face == "front" or side(p) and p.fz < 0.28 and p.fy > 0.2 or p.face == "bottom" and p.fz < 0.3:
            return fe(CHEST if h(p.tx, p.ty, 3) > 0.25 else CHEST_D, p, 4)  # chestnut breast
        if p.face == "back" or p.fz > 0.82 and p.face in ("top", "bottom") or side(p) and p.fz > 0.86:
            return fe(BLACK, p, 5) if p.fy < 0.75 or p.face != "back" else fe(TAILW, p, 6)  # black stern
        if p.face == "top" or side(p) and p.fy < 0.25:
            return fe(BACK if h(p.tx, p.ty, 7) > 0.3 else BACK_D, p, 8)
        if p.face == "bottom":
            return fe(lerp(FLANK, WHITE, 0.4), p, 9, 0.03)
        return vermic(FLANK if p.fy < 0.7 else FLANK_D, p, 10)

    def neck(p):
        if not drake:
            return mottled(p, 11)
        if p.fy > 0.6:
            return fe(RING, p, 12, 0.02)  # white collar
        return fe(lerp(GREEN, GLOSS, h(p.tx, p.ty, 13)), p, 14, 0.06)

    def head(p):
        if is_eye(p, S):
            return (20, 14, 10)
        if not drake:
            ey, ez = S["eye"]
            if side(p) and p.iy == ey and p.iz != ez:
                return fe(DARK, p, 15)  # dark eye stripe
            if p.face == "top":
                return fe(DARK, p, 16)  # dark crown
            return fe(lerp(BUFF, (206, 176, 132), 0.5), p, 17, 0.05)
        if p.face == "top":
            return fe(lerp(GLOSS, PURPLE, 0.3 * h(p.tx, p.ty, 18)), p, 19, 0.06)  # sheen on the crown
        return fe(lerp(GREEN, GLOSS, 0.6 * h(p.tx, p.ty, 20) if side(p) else 0.1), p, 21, 0.06)

    def beak(p):
        if p.face == "front" or p.iz == 0 and p.face == "top":
            return NAIL
        if drake:
            return shade(BILL_D, 0.85) if p.face == "bottom" else BILL_D
        return (60, 52, 40) if p.face == "top" and p.fz > 0.3 else BILL_H  # orange bill, dark saddle

    def wing(p):
        if not p.upper:
            return fe((206, 204, 200), p, 22, 0.03)
        if p.s < 0.5 and 0.52 <= p.c < 0.86:
            if p.c < 0.57 or p.c > 0.8:
                return WHITE  # white bars either side of the speculum
            return fe(SPEC_L if int(p.y) % 2 else SPEC, p, 23, 0.06)  # blue speculum
        if p.s > 0.55:
            return feathers_along(p, WING_D, 24)
        if not drake:
            return mottled(p, 25) if p.c < 0.5 else feathers_along(p, WING, 26)
        return feathers_along(p, WING, 27)

    def tail(p):
        if drake and p.face == "top" and p.fz < 0.4:
            return fe(BLACK, p, 28)  # the curled black coverts
        return fe(TAILW if drake else BUFF, p, 29, 0.04)

    def leg(p):
        return fe(LEG, p, 30, 0.05)

    return dict(body=body, neck=neck, head=head, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)


def swan_painters(S):
    WHITE, SHADE, DEEP = (246, 246, 242), (224, 226, 226), (204, 206, 208)
    BILL, KNOB, LEG = (230, 112, 40), (22, 20, 22), (40, 38, 40)

    def white(p, salt):
        c = SHADE if p.face == "bottom" else DEEP if p.face == "back" and p.fy > 0.6 else WHITE
        return fe(c, p, salt, 0.025)

    def body(p):
        if p.face == "top" and int(p.Z) % 2 == 0 and h(p.tx, p.ty, 1) < 0.5:
            return fe(SHADE, p, 2, 0.02)  # soft feather rows on the back
        return white(p, 3)

    def head(p):
        if is_eye(p, S):
            return (24, 18, 16)
        ey, ez = S["eye"]
        if side(p) and p.iz < ez and p.iy >= ey:
            return KNOB  # bare black skin between the eye and the bill
        return white(p, 4)

    def beak(p):
        if p.iz == int(S["beak"][2]) - 1 and p.face in ("top", "left", "right") and p.iy == 0 or p.face == "back":
            return KNOB  # the black knob at the base
        if p.face == "front":
            return (40, 34, 30)  # black nail
        return shade(BILL, 0.88) if p.face == "bottom" else BILL

    def wing(p):
        if not p.upper:
            return fe(SHADE, p, 5, 0.02)
        return feathers_along(p, lerp(WHITE, DEEP, 0.4 if p.c > 0.8 else 0.0), 6)

    def leg(p):
        return fe(LEG, p, 7, 0.08)

    return dict(body=body, neck=lambda p: white(p, 8), head=head, beak=beak, wing=wing, tip=wing, tail=lambda p: white(p, 9),
                leg=leg, foot=leg)


def heron_painters(S):
    GREY, GREY_L, GREY_D = (150, 156, 164), (176, 182, 188), (118, 124, 132)
    WHITE, PALE, BLACK = (238, 238, 236), (214, 216, 218), (28, 28, 32)
    BILL, BILL_D, LEG, LEG_D = (226, 180, 70), (196, 140, 50), (176, 150, 96), (140, 118, 74)
    PRIM = (52, 54, 62)

    def body(p):
        if p.face == "bottom" or p.face == "front" and p.fy > 0.4:
            return fe(PALE, p, 1, 0.03)
        if side(p) and p.fz < 0.3 and p.fy < 0.55:
            return fe(BLACK, p, 2, 0.05)  # the black shoulder patch at the bend of the wing
        if p.face == "front":
            return fe(WHITE, p, 3, 0.03)
        if side(p) and p.fy > 0.6:
            return fe(lerp(PALE, GREY, 0.5), p, 4, 0.04)
        return fe(GREY if h(p.tx, p.ty, 5) > 0.2 else GREY_L, p, 6, 0.04)

    def neck(p):
        if p.face == "front" and int(p.y) % 2 == 0 and h(p.tx, p.ty, 7) < 0.7:
            return fe(BLACK, p, 8)  # dark streaks down the foreneck
        return fe(WHITE if p.face == "front" else lerp(WHITE, GREY_L, 0.5), p, 9, 0.03)

    def neck2(p):
        if p.face == "front" and p.iy % 2 == 1:
            return fe(lerp(BLACK, GREY_D, 0.4), p, 10)
        return fe(WHITE, p, 11, 0.03)

    def head(p):
        if is_eye(p, S):
            return (230, 200, 60)  # yellow eye
        ey, ez = S["eye"]
        if side(p) and p.iy == ey and p.iz >= ez or p.face == "back" and p.iy == 0:
            return BLACK  # black stripe from the eye back into the crest
        if p.face == "top":
            return fe(WHITE, p, 12, 0.02)
        return fe(WHITE, p, 13, 0.03)

    def beak(p):
        return BILL_D if p.face == "bottom" else BILL

    def plume(p):
        return fe(BLACK, p, 14, 0.06)

    def wing(p):
        if not p.upper:
            return fe(GREY_D, p, 15, 0.04)
        if p.s > 0.5:
            return feathers_along(p, PRIM, 16)  # dark flight feathers
        if p.c > 0.7:
            return feathers_along(p, GREY_D, 17)
        return feathers_along(p, GREY_L if p.c < 0.25 else GREY, 18)

    def tail(p):
        return fe(GREY_D, p, 19, 0.04)

    def leg(p):
        return fe(LEG if p.face != "back" else LEG_D, p, 20, 0.06)

    return dict(body=body, neck=neck, neck2=neck2, head=head, beak=beak, plume=plume, wing=wing, tip=wing, tail=tail,
                leg=leg, shin=leg, foot=leg)


def crane_painters(S):
    GREY, GREY_L, GREY_D = (140, 144, 150), (164, 168, 172), (108, 110, 116)
    BLACK, WHITE, RED = (26, 26, 30), (236, 236, 232), (200, 36, 36)
    BILL, LEG = (156, 150, 116), (36, 34, 36)

    def body(p):
        if p.face == "top" and int(p.Z) % 2 == 0 and h(p.tx, p.ty, 1) < 0.4:
            return fe(GREY_D, p, 2, 0.04)
        if p.face == "bottom":
            return fe(GREY_L, p, 3, 0.03)
        return fe(GREY if h(p.tx, p.ty, 4) > 0.25 else GREY_L, p, 5, 0.04)

    def neck(p):
        # black neck with the white stripe running down its back and sides from behind the eye
        if p.face == "back" or side(p) and p.fz > 0.5 and p.fy < 0.6:
            return fe(WHITE, p, 6, 0.03)
        if p.fy > 0.75:
            return fe(lerp(BLACK, GREY_D, 0.5), p, 7)
        return fe(BLACK, p, 8, 0.05)

    def neck2(p):
        if p.face == "back" or side(p) and p.fz > 0.5:
            return fe(WHITE, p, 9, 0.03)
        return fe(BLACK, p, 10, 0.05)

    def head(p):
        if is_eye(p, S):
            return (180, 40, 30)  # reddish eye
        if p.face == "top" and p.fz < 0.7 or side(p) and p.iy == 0 and p.iz in (1, 2):
            return fe(RED, p, 11, 0.05)  # bare red crown
        if side(p) and p.iz == 2 and p.iy >= 1:
            return fe(WHITE, p, 12, 0.03)  # white stripe starting behind the eye
        return fe(BLACK, p, 13, 0.05)

    def beak(p):
        return shade(BILL, 0.85) if p.face == "bottom" else BILL

    def wing(p):
        if not p.upper:
            return fe(GREY_D, p, 14, 0.04)
        if p.s > 0.5 or p.c > 0.82:
            return feathers_along(p, BLACK, 15)  # black primaries and secondaries
        return feathers_along(p, GREY, 16)

    def tail(p):
        # the bustle: long grey tertials, black-tipped, curling over the tail
        if p.fz > 0.6 or p.face == "bottom":
            return fe(lerp(BLACK, GREY_D, 0.35), p, 17, 0.06)
        return fe(GREY_D if (p.ix + int(p.Z)) % 2 else GREY, p, 18, 0.05)

    def leg(p):
        return fe(LEG, p, 19, 0.08)

    return dict(body=body, neck=neck, neck2=neck2, head=head, beak=beak, wing=wing, tip=wing, tail=tail, leg=leg, shin=leg, foot=leg)


def plucked(S, base, light, dark):
    out = gb.plucked_painters(S, base, light, dark)
    out["neck2"] = out["neck"]
    out["shin"] = out["leg"]
    out["plume"] = lambda p: (222, 206, 176)  # quill stubs of the crest
    return out


# texture -> species, painters, plucked skin (light, dark); mallard dark pink game meat, swan pale, heron and crane dark red
TEXTURES = {
    "mallard_drake": ("mallard", lambda S: mallard_painters(S, True), (232, 176, 160), (186, 118, 106)),
    "mallard_hen": ("mallard", lambda S: mallard_painters(S, False), (232, 176, 160), (186, 118, 106)),
    "mute_swan": ("mute_swan", swan_painters, (244, 220, 190), (206, 166, 132)),
    "grey_heron": ("grey_heron", heron_painters, (222, 160, 150), (172, 102, 96)),
    "common_crane": ("common_crane", crane_painters, (226, 168, 152), (176, 110, 100)),
}


# ======================================================================= Java
def write_layers(path, marker, layers):
    src = path.read_text(encoding="utf-8")
    begin, end = f"    // BEGIN GENERATED by scripts/{marker}\n", "    // END GENERATED\n"
    assert begin in src and end in src, f"markers missing in {path.name}"
    new = re.sub(re.escape(begin) + ".*?" + re.escape(end), lambda m: begin + "\n\n".join(layers) + "\n" + end, src, flags=re.S)
    path.write_text(new, encoding="utf-8")
    print("wrote", path.relative_to(ROOT))


def entities():
    built = {}
    flock_layers, wader_layers = [], []
    for species, (S, size) in FLOCK.items():
        gb.SIZE = size
        parts = gb.geometry(S)
        uv = pack(parts, *size)
        built[species] = (parts, uv, S, size)
        flock_layers.append(gb.java_layer(species, parts, uv))
    for species, S in WADERS.items():
        gb.SIZE = WADER_SIZE
        parts = wader_geometry(S)
        uv = pack(parts, *WADER_SIZE)
        built[species] = (parts, uv, S, WADER_SIZE)
        wader_layers.append(gb.java_layer(species, parts, uv))
    for tex, (species, painters, light, dark) in TEXTURES.items():
        parts, uv, S, size = built[species]
        gb.SIZE = size
        base = painters(S)
        save(gb.paint(parts, uv, S, base), f"wildlife/{tex}.png")
        save(gb.paint(parts, uv, S, plucked(S, base, light, dark)), f"skinned/wildlife/{tex}.png")
    gb.SIZE = (64, 32)
    me = Path(__file__).name
    write_layers(JAVA / "WaterfowlBirdModel.java", me, flock_layers)
    write_layers(JAVA / "WaderModel.java", me, wader_layers)


# ======================================================================= items (16x16)
grid, hexc = b2.grid, b2.hexc
RAW = {
    "mallard": dict(o="#3e1216", d="#702228", m="#983438", l="#ba5650", h="#d88272", s="#e6c8a6", b="#eee6d2", B="#c8bca4"),
    "swan": dict(o="#4a1a1c", d="#842e32", m="#ac4848", l="#cc7470", h="#e6a29a", s="#f0d8b8", b="#eee6d2", B="#c8bca4"),
    "heron": dict(o="#36100f", d="#5e1a1c", m="#842a2c", l="#a44644", h="#c47064", s="#d8bca6", b="#eee6d2", B="#c8bca4"),
    "crane": dict(o="#3a1214", d="#661e22", m="#8e3034", l="#b0504c", h="#cc7c70", s="#e0c4a8", b="#eee6d2", B="#c8bca4"),
}
COOKED = {
    "mallard": dict(o="#30180a", d="#663418", m="#8e5226", l="#b07038", h="#cc9452", s="#c4843e", b="#e8dcc0", B="#bcae90"),
    "swan": dict(o="#3c200e", d="#7c4420", m="#aa642e", l="#ca8842", h="#e4b066", s="#dca050", b="#e8dcc0", B="#bcae90"),
    "heron": dict(o="#2c1408", d="#5a2c14", m="#7e4220", l="#a06030", h="#c0844a", s="#b4783c", b="#e8dcc0", B="#bcae90"),
    "crane": dict(o="#30160a", d="#623218", m="#884c24", l="#aa6a34", h="#c88e4e", s="#bc8040", b="#e8dcc0", B="#bcae90"),
}
CUTS = {
    "mallard": [  # a duck breast: thick fat-capped skin on one side, scored
        "................",
        "................",
        "................",
        "......oooooo....",
        "....oosssssso...",
        "...osshsshsso...",
        "..oshllllllmso..",
        "..ohllmmmmmmdo..",
        ".ohllmmmmmmmddo.",
        ".olmmmmmmmmmddo.",
        ".ommmmmmmmmdddo.",
        "..ommmmmmmddddo.",
        "...oodmmmddddo..",
        ".....oodddooo...",
        ".......ooo......",
        "................",
    ],
    "swan": b2.CUTS["goose"],  # the big leg quarter
    "heron": [  # a long thin thigh and drumstick
        "................",
        "...........ooo..",
        "..........osso..",
        ".........oshlmo.",
        "........oshlmdo.",
        ".......oshlmmdo.",
        "......oshlmmdo..",
        ".....oslmmmdo...",
        "....oslmmmdo....",
        "...oommmddo.....",
        "...obodddo......",
        "..obbboo........",
        ".obbBo..........",
        "obBBo...........",
        ".oo.............",
        "................",
    ],
    "crane": b2.CUTS["pheasant"],  # a big drumstick
}
SKIN = b2.SKIN
PLUCKED = {
    "mallard": ([  # compact duck, flat bill, short neck, orange feet
        "................",
        "................",
        "................",
        "...ooo..........",
        "kkkeso..........",
        ".kooso..........",
        "...osloooooo....",
        "..oslllsssssoo..",
        "..oslsfssssfsdo.",
        "..osssssfssssddo",
        "...osfssssssddqo",
        "....oddsssdddoq.",
        ".....oooggooo...",
        "......oGGGo.....",
        "................",
        "................",
    ], dict(SKIN, s="#e2b0a0", l="#f0cab8", d="#bc8274", f="#c89282", k="#d8c442", e="#1c1410", g="#e88a3a", G="#bc6a26")),
    "swan": ([  # long curved neck, big body, orange bill with the black knob
        "................",
        "kkKo............",
        ".ooeso..........",
        "...oso..........",
        "...osdo.........",
        "....osdo........",
        "....osdo........",
        "...oslo.........",
        "...osloooooooo..",
        "..oslllssssssdo.",
        "..oslsfssssfsddo",
        "..ossssssfssssdo",
        "...osfssssssddqo",
        "....oddssssdddo.",
        ".....oooGooGoo..",
        ".......GG..GG...",
    ], dict(SKIN, s="#f0d4b4", l="#f8e6cc", d="#ccaa86", f="#dab896", k="#e8742a", K="#1c1a1a", e="#1c1410", G="#2c2a2a")),
    "heron": ([  # dagger bill, long S-neck, thin body and long legs
        "kkkko...........",
        "...oeso.........",
        "....oso.........",
        "....osdo........",
        ".....osdo.......",
        ".....osdo.......",
        "....oslo........",
        "....osloooooo...",
        "...oslllssssoo..",
        "...oslsfsssfsdo.",
        "....osssssssqqo.",
        ".....oddssddoq..",
        "......ooggoo....",
        ".......og.g.....",
        ".......og.g.....",
        "......GGo.GG....",
    ], dict(SKIN, s="#d8a49a", l="#e8c0b4", d="#b07a70", f="#bc887e", k="#e2b446", e="#e6c83c", g="#b0965e", G="#8c764a")),
    "crane": ([  # straight neck, red crown, bustle stubs, long dark legs
        "................",
        ".rr.............",
        "kkeso...........",
        "..oso...........",
        "..oso...........",
        "..osdo..........",
        "...osdo.........",
        "...oslo.........",
        "...osloooooooo..",
        "..oslllssssssdo.",
        "..oslsfssssfsqqo",
        "..ossssssfssqqqo",
        "...osfssssddqqo.",
        "....oddsssddoo..",
        "......ogo.og....",
        ".....GGo.GGo....",
    ], dict(SKIN, s="#dca898", l="#ecc4b6", d="#b47c70", f="#c08a7e", k="#a49c74", e="#b42a20", r="#c8302c", g="#2e2c2e", G="#1c1a1c")),
}
EGGS = {  # shape from gen_birds2_textures (o outline, w shell, l light, h shine, d shade, x/y markings)
    "mallard": (b2.EGGS["pheasant"][0], dict(o="#6a7458", w="#c8d0b0", l="#d8dec4", h="#eef0e0", d="#a6ae8e")),  # greenish buff
    "swan": (b2.EGGS["goose"][0], dict(o="#7a7c74", w="#dfe2d6", l="#ecefe4", h="#fafbf6", d="#bcc0b2")),  # very big, chalky grey-white
    "heron": (b2.EGGS["pheasant"][0], dict(o="#4a7470", w="#8cc4be", l="#a8d6d0", h="#c8eae4", d="#6aa49e")),  # pale blue-green
    "crane": ([  # olive-brown, blotched dark
        "................",
        "................",
        ".......oo.......",
        "......owwo......",
        ".....owlxwo.....",
        "....owhwwwxo....",
        "....owlwyxwo....",
        "...owxwwwwwyo...",
        "...owwwyxwwdo...",
        "...oxwwwwwxdo...",
        "...owwwwwwwdo...",
        "...odwyxwwddo...",
        "....odwwwxdo....",
        ".....odddddo....",
        "......ooooo.....",
        "................",
    ], dict(o="#4a3a22", w="#a48c5c", l="#b8a272", h="#d0bc90", d="#806c44", x="#4a2c18", y="#6e4626")),
}
SHELLS = {"mallard": ("#c8d0b0", "#b0b896"), "mute_swan": ("#dfe2d6", "#c8ccbe"), "grey_heron": ("#8cc4be", "#74aca6"),
          "common_crane": ("#a48c5c", "#4a2c18")}

ITEMS = {
    "down": ([  # a fluffy tuft of white down
        "................",
        "................",
        "......w..w......",
        "....w.wwlw.w....",
        "...wwlwwwwlww...",
        "..w.wwwhhwwwlw..",
        "...wlwhhhhwww...",
        "..wwwwhhhwwlww..",
        "...wlwwhwwwwd...",
        "..w.wwwwwlwdw...",
        "....wdwwlwddw...",
        "...w.wdwwdw.w...",
        ".....w.dd.w.....",
        "........q.......",
        "................",
        "................",
    ], dict(w="#ece8e0", l="#f8f6f2", h="#ffffff", d="#cfc8bc", q="#b8ae9a")),
    "swan_quill": ([  # a long white flight feather with a stiff pale shaft
        "..............wo",
        ".............wwo",
        "............wwlo",
        "...........wwlo.",
        "..........wwlwo.",
        ".........wwlwo..",
        "........wwlwwo..",
        ".......wwlwwo...",
        "......wwlwwo....",
        ".....wwlwwdo....",
        "....dwlwwdo.....",
        "...ddlwddo......",
        "...dlddoo.......",
        "..sloo..........",
        ".ss.............",
        "s...............",
    ], dict(w="#f4f4f0", l="#d8d4c8", d="#c6c8cc", o="#9ca0a8", s="#e0d6c0")),
    "heron_plume": ([  # long slender grey-and-black plumes, curving
        "................",
        "...........kk...",
        "..........kgk...",
        ".........kgk..k.",
        "........kgk..kk.",
        ".......kgk..kgk.",
        "......kgk..kgk..",
        ".....kgk..kgk...",
        "....ggk..kgk....",
        "...ggk..ggk.....",
        "...gk..ggk......",
        "..ggk.ggk.......",
        "..gkkggk........",
        "...sgk..........",
        "..ss............",
        ".s..............",
    ], dict(k="#1c1c22", g="#9aa0a8", s="#d8d2c4")),
    "heron_bill": ([  # the long dagger bill, yellow with a darker ridge
        "................",
        "................",
        "...............o",
        ".............ooy",
        "...........ooyyo",
        ".........ooyyydo",
        ".......ooyyyddo.",
        ".....ooyyyddoo..",
        "...ooyyyddoo....",
        "..oyyyddoo......",
        ".oyhyddo........",
        ".ohyddo.........",
        ".ooddo..........",
        "...oo...........",
        "................",
        "................",
    ], dict(o="#6a5428", y="#e2b446", h="#f4d47a", d="#b88a30")),
    "hollow_bird_bone": ([  # a long hollow wing bone, knobbed ends, the open hollow showing at one end
        "................",
        "............oo..",
        "...........obho.",
        "..........obbko.",
        ".........obbBo..",
        "........obbBo...",
        ".......obbBo....",
        "......obbBo.....",
        ".....obbBo......",
        "....obbBo.......",
        "...obbBo........",
        "..obbBo.........",
        ".obhBo..........",
        ".obbo...........",
        "..oo............",
        "................",
    ], dict(o="#7c7058", b="#ece4cc", h="#fbf7ea", B="#c8bc9e", k="#4a4030")),
    "bone_flute": ([  # the bone with finger holes and a notched mouthpiece, bound with sinew
        "................",
        "............oo..",
        "...........okko.",
        "..........obbBo.",
        ".........osssBo.",
        "........obbBo...",
        ".......obkBo....",
        "......obbBo.....",
        ".....obkBo......",
        "....obbBo.......",
        "...obkBo........",
        "..osssBo........",
        ".obbBo..........",
        ".obbo...........",
        "..oo............",
        "................",
    ], dict(o="#6e6248", b="#e6dcc0", B="#c0b292", k="#3a3024", s="#a4744a")),
    "plume_headdress": ([  # a leather band with heron plumes and a white swan quill standing up
        "................",
        "......k...w.....",
        ".....kg..ww..k..",
        ".....kg..wl.kg..",
        "....kg..wl..kg..",
        "....kg..wl.kg...",
        "...kg..wl..kg...",
        "...kg..wl.kg....",
        "....kg.wlkg.....",
        "....oooooooooo..",
        "...oLLLlLLLlLLo.",
        "...oLDLLDLLDLLo.",
        "....oooooooooo..",
        "................",
        "................",
        "................",
    ], dict(k="#1c1c22", g="#9aa0a8", w="#f4f4f0", l="#cfcac0", o="#4a2e18", L="#9a6a3c", D="#6e4626")),
}


def headdress_layer():
    """Humanoid armour layer (64x32): a leather band round the head with plumes standing up from the back of it."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    LEATHER, DARK, STITCH = hexc("#8e6034"), hexc("#5e3c1e"), hexc("#c49a5e")
    # helmet box: head cube u 0, v 0 (8x8x8): faces top (8..16, 0..8), sides at v 8..16
    for x in range(0, 32):
        for y in (12, 13):  # band around the brow on all four sides
            c = LEATHER if y == 12 else DARK
            if y == 12 and x % 3 == 1:
                c = STITCH
            im.putpixel((x, y), (*c, 255))
    # plumes: the outer hat layer (u 32, v 0) shows them standing above the head at the back
    BLACK, GREY, WHITE = hexc("#1c1c22"), hexc("#9aa0a8"), hexc("#f4f4f0")
    for x, y, c in [(57, 8, GREY), (57, 9, GREY), (58, 10, BLACK), (58, 11, GREY), (59, 12, GREY),
                    (61, 9, WHITE), (61, 10, WHITE), (61, 11, WHITE), (61, 12, WHITE), (62, 13, WHITE),
                    (52, 10, BLACK), (52, 11, GREY), (53, 12, GREY), (53, 13, BLACK)]:
        im.putpixel((x, y), (*c, 255))
    return im


def spawn_eggs():
    K = "#151316"
    mallard = wit.egg(["#d6d6d4", "#c2c2c0", "#a8a8a6", "#8c8c8a", "#5a5a58", "#3a3a38"],
                      [(x, y, "#1f6a3a") for y in range(1, 5) for x in range(5, 11)] + [(x, 5, "#f0f0ec") for x in range(4, 12)]
                      + [(x, 6, "#7a3e28") for x in range(3, 13)] + [(x, 7, "#7a3e28") for x in range(3, 13)]
                      + [(4, 10, "#2c40aa"), (5, 10, "#2c40aa"), (6, 10, "#2c40aa")],
                      (), [(7, 0, "#d8c442"), (8, 0, "#d8c442"), (12, 12, K), (11, 13, K)])
    swan = wit.egg(["#ffffff", "#f6f6f2", "#e6e8e8", "#d4d6d8", "#a0a4a8", "#70747a"],
                   [(6, 2, "#e8742a"), (7, 2, "#e8742a"), (5, 2, K), (6, 3, K)], (), [(10, 12, "#d8dadc"), (4, 11, "#d8dadc")])
    heron = wit.egg(["#c4cad0", "#a8b0b8", "#949ca4", "#7a828c", "#4e545c", "#34383e"],
                    [(x, 3, K) for x in range(4, 13)] + [(x, y, "#eeeeec") for y in range(5, 9) for x in range(6, 10)]
                    + [(7, 6, K), (7, 8, K), (3, 9, K), (4, 9, K), (12, 9, K)],
                    (), [(2, 6, "#e2b446"), (1, 6, "#e2b446")])
    crane = wit.egg(["#b0b4ba", "#9a9ea6", "#868a92", "#70747c", "#484c52", "#303438"],
                    [(x, y, K) for y in range(2, 6) for x in range(5, 11)] + [(x, 4, "#eeeeea") for x in range(9, 12)]
                    + [(x, 5, "#eeeeea") for x in range(10, 12)] + [(x, y, "#2a2a2e") for y in range(11, 14) for x in range(9, 12)],
                    (), [(7, 1, "#c8302c"), (8, 1, "#c8302c")])
    return {"mallard_spawn_egg": mallard, "mute_swan_spawn_egg": swan, "grey_heron_spawn_egg": heron, "common_crane_spawn_egg": crane}


def items():
    for bird in CUTS:
        b2.save(grid(CUTS[bird], RAW[bird]), f"item/raw_{bird}.png")
        b2.save(grid(CUTS[bird], COOKED[bird]), f"item/cooked_{bird}.png")
    for bird, (rows, pal) in PLUCKED.items():
        b2.save(grid(rows, pal), f"item/plucked_{bird}.png")
    for bird, (rows, pal) in EGGS.items():
        b2.save(grid(rows, pal), f"item/{bird}_egg.png")
    for bird, (base, spot) in SHELLS.items():
        b2.save(b2.shell(base, spot, bird == "common_crane"), f"block/bird_egg_{bird}.png")
    for name, (rows, pal) in ITEMS.items():
        b2.save(grid(rows, pal), f"item/{name}.png")
    b2.save(headdress_layer(), "entity/equipment/humanoid/plume_headdress.png")
    for name, im in spawn_eggs().items():
        assert im.size == (16, 16) and im.getbbox(), name
        b2.save(im, f"item/{name}.png")


if __name__ == "__main__":
    entities()
    items()
