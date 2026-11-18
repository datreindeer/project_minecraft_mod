"""Generate the baby small-wildlife textures: pheasant chick and leveret (baby hare).

Usage: python gen_baby_small_wildlife_textures.py
PHEASANT_CHICK mirrors PheasantChickModel.java (pivot, rotation, cubes as (texU, texV, x0, y0, z0, w, h, d)); keep
them in sync. The leveret has no model of its own: HareModel is scaled down by the renderer, so it is painted on the
adult HARE part table (imported) and keeps hare.png's UV layout exactly. Deterministic: no randomness, only hashes.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_small_wildlife_textures as G  # noqa: E402
from gen_skinned_textures import h, lerp  # noqa: E402

OUT = G.ROOT / "wildlife"
part = G.part

PHEASANT_CHICK = [
    part("body", (0, 21, 0), (0, 0, 0), [(0, 0, -2, -1.5, -2, 4, 3, 4), (0, 7, -1.5, -2.5, -1, 3, 1, 3)], [
        part("head", (0, -0.5, -1.5), (0, 0, 0), [(16, 0, -1.5, -2.5, -1.5, 3, 3, 3), (28, 0, -0.5, -1.5, -2.5, 1, 1, 1)]),
        part("left_wing", (2, -1, -1), (0, 0, 0), [(12, 7, 0, 0, 0, 1, 2, 3)]),
        part("right_wing", (-2, -1, -1), (0, 0, 0), [(20, 7, -1, 0, 0, 1, 2, 3)]),
        part("tail", (0, -1, 2), (0.6, 0, 0), [(28, 2, -0.5, -0.5, 0, 1, 1, 1)]),
    ]),
    part("left_leg", (1, 22, 0.5), (0, 0, 0), [(28, 4, -0.5, 0, -0.5, 1, 2, 1), (0, 12, -1, 2, -1.5, 2, 0, 2)]),
    part("right_leg", (-1, 22, 0.5), (0, 0, 0), [(28, 7, -0.5, 0, -0.5, 1, 2, 1), (8, 12, -1, 2, -1.5, 2, 0, 2)]),
]
CHICK_W, CHICK_H = 32, 16


# ---------------------------------------------------------------- pheasant chick
C_BUFF, C_TAWNY, C_CREAM = (214, 172, 108), (190, 136, 72), (238, 222, 180)
C_STRIPE, C_RUFOUS, C_DARK = (92, 60, 36), (150, 92, 48), (54, 36, 26)
C_EYE, C_BEAK, C_LEG = (22, 16, 14), (204, 160, 126), (232, 176, 124)


def down(base, tu, tv, salt, amt=0.12):
    """Fluffy down: soft light/dark flecks, no hard ticking."""
    n = h(tu, tv, salt)
    if n < 180:
        return lerp(base, C_DARK, amt)
    if n > 820:
        return lerp(base, (255, 246, 220), amt * 1.4)
    return base


def chick_body(f, x, y, z, w, hh, d, tu, tv):
    if f == "bottom":
        return down(C_CREAM, tu, tv, 40, 0.06)
    if f == "front":  # breast: buff fading to the pale belly
        return down(C_BUFF if y < 1 else C_CREAM, tu, tv, 41)
    if f == "top":  # two dark side stripes framing a tawny back (the cap adds the middle one)
        col = int(x)
        return down(C_STRIPE if col in (0, 3) else C_TAWNY, tu, tv, 42)
    row = int(y)
    if f == "back":
        return down(C_STRIPE if row == 0 and int(x) in (0, 3) else (C_TAWNY if row == 0 else (C_BUFF if row == 1 else C_CREAM)), tu, tv, 43)
    # flanks: dark stripe along the top edge, buff side, cream belly line
    if row == 0:
        return down(C_STRIPE if z > 0.6 else C_TAWNY, tu, tv, 44)
    return down(C_BUFF if row == 1 else C_CREAM, tu, tv, 45)


def chick_cap(f, x, y, z, w, hh, d, tu, tv):
    if f == "top":  # dark central stripe with rufous edging
        col = int(x)
        return down(C_STRIPE if col == 1 else C_RUFOUS, tu, tv, 46)
    if f == "bottom":
        return C_TAWNY
    return down(C_TAWNY if f in ("left", "right") else C_RUFOUS, tu, tv, 47)


def chick_head(f, x, y, z, w, hh, d, tu, tv):
    row, col = int(y), int(z)
    if f == "top":  # chestnut crown cap edged dark
        return down(C_STRIPE if int(x) == 1 and z > 1 else C_RUFOUS, tu, tv, 48)
    if f == "bottom":
        return C_CREAM
    if f == "back":
        return down(C_STRIPE if int(x) == 1 and row < 2 else (C_RUFOUS if row == 0 else C_BUFF), tu, tv, 49)
    if f == "front":  # pale face below a rufous forehead
        return down(C_RUFOUS if row == 0 else (C_BUFF if row == 1 else C_CREAM), tu, tv, 50, 0.06)
    # sides: crown edge, dark stripe running back through the eye, pale cheek
    if row == 0:
        return down(C_RUFOUS if col else C_BUFF, tu, tv, 51)
    if row == 1:
        if col == 1:
            return C_EYE
        return C_STRIPE if col == 2 else lerp(C_STRIPE, C_BUFF, 0.5)
    return down(C_CREAM if col < 2 else C_BUFF, tu, tv, 52, 0.05)


def chick_beak(f, x, y, z, w, hh, d, tu, tv):
    if f == "front":
        return (238, 214, 190)  # paler tip (egg tooth)
    return lerp(C_BEAK, (150, 112, 90), 0.3) if f == "bottom" else C_BEAK


def chick_wing(f, x, y, z, w, hh, d, tu, tv):
    if f == "top":
        return C_STRIPE
    if f in ("bottom", "front", "back"):
        return C_BUFF
    is_outer = (f == "left") == (tu < 20)  # left wing's outer side is +x
    if not is_outer:
        return C_CREAM
    row, col = int(y), int(z)
    if row == 0:  # tawny shoulder with one dark streak
        return down(lerp(C_STRIPE, C_TAWNY, 0.35) if col == 1 else C_TAWNY, tu, tv, 53)
    return down(C_BUFF if col < 2 else C_CREAM, tu, tv, 54)


def chick_tail(f, x, y, z, w, hh, d, tu, tv):
    return C_STRIPE if f == "top" else C_TAWNY


def chick_leg(f, x, y, z, w, hh, d, tu, tv):
    return C_LEG if (f in ("front", "left", "right") or int(y) == 0) else lerp(C_LEG, (170, 120, 90), 0.4)


def chick_foot(f, x, y, z, w, hh, d, tu, tv):
    if f == "bottom":
        return lerp(C_LEG, (170, 120, 90), 0.4)
    return C_LEG if int(x) == (int(z) % 2) or z < 1 else (210, 156, 108)  # two toes, gap shading


CHICK_PAINTERS = {
    ("body", 0): chick_body, ("body", 1): chick_cap, ("head", 0): chick_head, ("head", 1): chick_beak,
    ("left_wing", 0): chick_wing, ("right_wing", 0): chick_wing, ("tail", 0): chick_tail,
    ("left_leg", 0): chick_leg, ("right_leg", 0): chick_leg, ("left_leg", 1): chick_foot, ("right_leg", 1): chick_foot,
}


# ---------------------------------------------------------------- leveret
def soft_tick(base, tu, tv, salt=0, dark=(40, 30, 24), light=(214, 196, 160), pd=110, pl=200):
    """Baby fur: same agouti idea as the adult's tick(), at a third of the contrast, so it reads soft and fluffy."""
    n = h(tu, tv, salt)
    if n < pd * 0.7:
        return lerp(base, dark, 0.15)
    if n < pl:
        return lerp(base, light, 0.2)
    return base


GREY = (150, 138, 124)


def babyish(c):
    """Lighter and greyer-brown than the adult."""
    lum = sum(c) / 3
    return lerp(lerp(c, (lum + 8,) * 3, 0.15), lerp(GREY, (232, 224, 210), 0.3), 0.15)


def leveret(adult):
    def fn(f, x, y, z, w, hh, d, tu, tv):
        return babyish(adult(f, x, y, z, w, hh, d, tu, tv))
    return fn


def leveret_head(f, x, y, z, w, hh, d, tu, tv):
    row, col = int(y), int(z)
    if f == "front" and row == 0 and int(x) in (1, 2):
        return (232, 226, 212) if int(x) == 1 else (218, 210, 194)  # faint white blaze
    if f == "top" and z < 1 and int(x) in (1, 2):
        return (214, 204, 186)  # blaze runs onto the forehead
    if f in ("left", "right") and row <= 1 and col in (1, 2):  # big dark eye with a catchlight
        return (214, 206, 192) if row == 0 and col == 1 else ((44, 32, 24) if row == 0 else (24, 18, 14))
    if f in ("left", "right") and row == 2 and col in (1, 2):
        return (228, 216, 196)  # pale ring below the eye
    return babyish(G.hare_head(f, x, y, z, w, hh, d, tu, tv))


def leveret_ear(f, x, y, z, w, hh, d, tu, tv):
    if f == "top" or y < 1:
        return (36, 30, 26)  # black tip, one row only so the ear looks shorter
    if y < 2:
        return lerp((36, 30, 26), (150, 136, 120), 0.6)
    return babyish(G.hare_ear(f, x, y, z, w, hh, d, tu, tv))


def leveret_body(f, x, y, z, w, hh, d, tu, tv):
    if f == "bottom" or (f in ("left", "right") and y >= 4):
        return (244, 240, 230) if h(tu, tv, 60) > 300 else (232, 226, 212)  # fluffy white belly
    return babyish(G.hare_body(f, x, y, z, w, hh, d, tu, tv))


LEVERET_PAINTERS = {k: leveret(fn) for k, fn in G.HARE_PAINTERS.items()}
LEVERET_PAINTERS.update({("head", 0): leveret_head, ("left_ear", 0): leveret_ear, ("right_ear", 0): leveret_ear,
                         ("body", 0): leveret_body})


def paint_leveret():
    orig = G.tick
    G.tick = soft_tick  # the adult painters call G.tick; swap in the soft one while painting
    try:
        return G.paint(G.HARE, LEVERET_PAINTERS)[0]
    finally:
        G.tick = orig


def main():
    G.TEX_W, G.TEX_H = CHICK_W, CHICK_H
    chick, _ = G.paint(PHEASANT_CHICK, CHICK_PAINTERS)  # paint() asserts no UV overlap
    G.TEX_W, G.TEX_H = 64, 32
    lev = paint_leveret()
    adult = G.paint(G.HARE, G.HARE_PAINTERS)[0]
    assert lev.getchannel("A").tobytes() == adult.getchannel("A").tobytes(), "leveret must keep hare.png's UV layout"
    for name, im in (("pheasant_chick", chick), ("hare_leveret", lev)):
        dst = OUT / f"{name}.png"
        im.save(dst)
        print("wrote", dst.relative_to(G.ROOT))


if __name__ == "__main__":
    main()
