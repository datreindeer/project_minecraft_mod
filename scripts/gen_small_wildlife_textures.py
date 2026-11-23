"""Generate hare and pheasant entity textures (+ skinned/plucked versions).

Usage: python gen_small_wildlife_textures.py
The part trees below mirror HareModel.java / PheasantModel.java (pivot, rotation, cubes as
(texU, texV, x0, y0, z0, w, h, d)); keep them in sync. Deterministic: no randomness, only hashes.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_skinned_textures import h, lerp, skin  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity"
TEX_W, TEX_H = 64, 32


def part(name, pivot, rot, cubes, children=()):
    return {"name": name, "pivot": pivot, "rot": rot, "cubes": cubes, "children": list(children)}


HARE = [
    part("body", (0, 16.5, 3.5), (0, 0, 0), [(0, 0, -2, -2.5, -8, 4, 5, 8)], [
        part("head", (0, -1.5, -7), (0, 0, 0), [(24, 0, -2, -3, -3.5, 4, 3, 4)], [
            part("nose", (0, -1, -3.5), (0, 0, 0), [(40, 0, -1, -1, -1, 2, 2, 1)]),
            part("left_ear", (1, -3, -0.5), (-0.3, 0, 0.12), [(54, 0, -1, -6, -0.5, 2, 6, 1)]),
            part("right_ear", (-1, -3, -0.5), (-0.3, 0, -0.12), [(48, 0, -1, -6, -0.5, 2, 6, 1)]),
        ]),
        part("tail", (0, -1.5, 0), (-0.5, 0, 0), [(40, 3, -1, -1, -0.5, 2, 2, 2)]),
        part("right_front_leg", (-1.1, 2.5, -7), (0, 0, 0), [(52, 7, -1, 0, -1, 2, 5, 2)]),
        part("left_front_leg", (1.1, 2.5, -7), (0, 0, 0), [(52, 14, -1, 0, -1, 2, 5, 2)]),
        part("right_haunch", (-2, 0.5, -2), (0, 0, 0), [(38, 7, -1, -2, -2.5, 2, 4, 5)], [
            part("right_hind_foot", (0, 2, 2), (0, 0, 0), [(0, 13, -1, 0, -1, 2, 4, 2), (0, 19, -1, 4, -5, 2, 1, 6)]),
        ]),
        part("left_haunch", (2, 0.5, -2), (0, 0, 0), [(24, 7, -1, -2, -2.5, 2, 4, 5)], [
            part("left_hind_foot", (0, 2, 2), (0, 0, 0), [(8, 13, -1, 0, -1, 2, 4, 2), (16, 19, -1, 4, -5, 2, 1, 6)]),
        ]),
    ]),
]

PHEASANT = [
    part("body", (0, 18, 0), (-0.15, 0, 0), [(0, 0, -2, -2, -4, 4, 4, 8)], [
        part("head", (0, -1.5, -3.5), (0.15, 0, 0),
             [(24, 0, -1, -4, -1, 2, 4, 2), (32, 0, -1.5, -6.5, -2.5, 3, 3, 3), (44, 0, -0.5, -5.5, -3.5, 1, 1, 1)]),
        part("left_wing", (2, -1.5, -3), (0, 0, 0), [(0, 12, 0, 0, 0, 1, 4, 7)]),
        part("right_wing", (-2, -1.5, -3), (0, 0, 0), [(16, 12, -1, 0, 0, 1, 4, 7)]),
        part("tail", (0, -1, 4), (0.45, 0, 0), [(32, 6, -1.5, -0.5, 0, 3, 1, 6), (32, 13, -1, -0.5, 6, 2, 1, 7)]),
    ]),
    part("left_leg", (1, 20, 0.5), (0, 0, 0), [(50, 0, -0.5, 0, -0.5, 1, 3, 1), (50, 4, -1, 3, -2.5, 2, 1, 3)]),
    part("right_leg", (-1, 20, 0.5), (0, 0, 0), [(54, 0, -0.5, 0, -0.5, 1, 3, 1), (50, 8, -1, 3, -2.5, 2, 1, 3)]),
]


def walk(parts):
    for p in parts:
        yield p
        yield from walk(p["children"])


def faces(u, v, w, hh, d):
    """Box-UV net, matching ModelPart.Cube. Yields (face, tu, tv, x, y, z): the texel and the cube-local point
    (pixels from the min corner) it covers. 'right' is the -x side (the animal's right), 'front' is -z."""
    for j in range(d):
        for i in range(w):
            yield "top", u + d + i, v + j, i + .5, 0, d - j - .5
            yield "bottom", u + d + w + i, v + j, i + .5, hh, d - j - .5
    for j in range(hh):
        for i in range(d):
            yield "right", u + i, v + d + j, 0, j + .5, d - i - .5
            yield "left", u + d + w + i, v + d + j, w, j + .5, i + .5
        for i in range(w):
            yield "front", u + d + i, v + d + j, i + .5, j + .5, 0
            yield "back", u + 2 * d + w + i, v + d + j, w - i - .5, j + .5, d


def paint(parts, painters):
    img = Image.new("RGBA", (TEX_W, TEX_H), (0, 0, 0, 0))
    px = img.load()
    regions = {}
    for p in walk(parts):
        for k, (u, v, _, _, _, w, hh, d) in enumerate(p["cubes"]):
            fn = painters[(p["name"], k)]
            regions[(p["name"], k)] = (u, v, 2 * (w + d), d + hh)
            for face, tu, tv, x, y, z in faces(u, v, w, hh, d):
                assert px[tu, tv][3] == 0, f"UV overlap at {tu},{tv} ({p['name']})"
                c = fn(face, x, y, z, w, hh, d, tu, tv)
                if c is not None:
                    px[tu, tv] = (*c, 255)
    return img, regions


def tick(base, tu, tv, salt=0, dark=(40, 30, 24), light=(214, 196, 160), pd=110, pl=200):
    """Agouti fur: each hair banded, so a fur patch reads as base colour flecked with black and cream."""
    n = h(tu, tv, salt)
    if n < pd:
        return lerp(base, dark, 0.4)
    if n < pl:
        return lerp(base, light, 0.35)
    return lerp(base, (0, 0, 0) if n % 2 else (255, 240, 210), 0.06)


# ---------------------------------------------------------------- hare
H_DARK, H_BROWN, H_OCHRE = (86, 66, 46), (124, 94, 62), (164, 120, 72)
H_TAN, H_CREAM, H_WHITE = (190, 154, 106), (226, 212, 184), (240, 236, 226)
H_BLACK, H_GREY, H_SOLE = (30, 25, 22), (126, 110, 92), (60, 46, 36)


def hare_body(f, x, y, z, w, hh, d, tu, tv):
    if f == "top":
        return tick(H_DARK if 1 <= x <= 3 else H_BROWN, tu, tv, 1)
    if f == "bottom":
        return lerp(H_CREAM, H_WHITE, 0.5) if h(tu, tv, 2) > 200 else H_CREAM
    if f == "front":  # chest: rufous
        return tick(H_OCHRE if y < 4 else H_TAN, tu, tv, 3, pd=60, pl=120)
    if f == "back":  # rump: greyer
        return tick(H_GREY if y < 4 else H_CREAM, tu, tv, 4)
    # flanks: dark grizzled back, warm ochre sides, creamy belly line
    row = int(y)
    if row == 0:
        base = H_DARK
    elif row <= 2:
        base = H_OCHRE if z < 3 else H_BROWN
    elif row == 3:
        base = H_TAN
    else:
        return H_CREAM if (tu + tv) % 3 else H_WHITE
    if z > 6 and row <= 2:
        base = lerp(base, H_GREY, 0.5)
    return tick(base, tu, tv, 5)


def hare_head(f, x, y, z, w, hh, d, tu, tv):
    row = int(y)
    if f == "top":
        return tick(H_DARK if 1 < x < 3 else H_BROWN, tu, tv, 6, pd=90)
    if f == "bottom":
        return H_CREAM
    if f == "back":
        return tick(H_BROWN, tu, tv, 7)
    if f == "front":  # forehead above the snout; the cheeks either side of it
        return tick(H_BROWN, tu, tv, 8, pd=60) if row == 0 else (H_TAN if row == 1 else H_CREAM)
    # sides: big amber eye with a pale ring, cream cheek below
    col = int(z)
    if row == 1 and col == 1:
        return (214, 150, 46)  # amber iris (front half of the eye)
    if row == 1 and col == 2:
        return (26, 18, 14)  # pupil
    if row == 0 and col in (1, 2):
        return (206, 180, 136)  # pale eye ring
    if row == 2:
        return H_CREAM if col < 2 else H_TAN
    if col == 0:
        return H_TAN
    return tick(H_BROWN, tu, tv, 9, pd=80)


def hare_nose(f, x, y, z, w, hh, d, tu, tv):
    row = int(y)
    if f == "front":
        return (136, 92, 84) if row == 0 else ((204, 186, 160) if int(x) == 0 else (190, 172, 146))
    if f == "top":
        return H_TAN
    if f == "bottom":
        return H_CREAM
    return (176, 150, 112) if row == 0 else (H_WHITE if f in ("left", "right") else H_CREAM)


def hare_ear(f, x, y, z, w, hh, d, tu, tv):
    if f in ("top",) or y < 1.5:
        return H_BLACK  # black tips
    if y < 2:
        return lerp(H_BLACK, H_GREY, 0.4)
    if f == "front":  # inner ear: pale pinkish grey, cream fringe
        return (196, 176, 166) if y < hh - 1 else (176, 150, 130)
    if f in ("left", "right"):
        return (176, 160, 136) if y < 3 else tick(H_GREY, tu, tv, 17, pd=60)
    if f == "bottom":
        return H_BROWN
    return tick(H_GREY, tu, tv, 10, pd=70)


def hare_tail(f, x, y, z, w, hh, d, tu, tv):
    if f == "top" or (f == "back" and y < 1) or (f in ("left", "right") and y < 1 and z > 1):
        return H_BLACK if (tu + tv) % 4 else (52, 44, 38)
    if f == "front":
        return H_BROWN
    return H_WHITE if (tu * 3 + tv) % 5 else H_CREAM


def hare_haunch(f, x, y, z, w, hh, d, tu, tv):
    if f == "bottom":
        return H_CREAM
    if f == "top":
        return tick(H_DARK, tu, tv, 11)
    if f == "front":
        return tick(H_OCHRE, tu, tv, 12)
    if f == "back":
        return tick(H_GREY, tu, tv, 13)
    if y > 3:
        return H_TAN if z < 4 else H_CREAM
    # thigh: rounded muscle, a darker rim at the top edge, brighter in the middle
    base = H_DARK if y < 1 else (H_OCHRE if 1 <= z <= 3 and y < 3 else H_BROWN)
    return tick(base, tu, tv, 14)


def hare_front_leg(f, x, y, z, w, hh, d, tu, tv):
    row = int(y)
    if f == "bottom":
        return H_SOLE
    if f == "top":
        return H_OCHRE
    if row == hh - 1:  # paw
        if f == "front":
            return (70, 56, 44) if (tu % 2 == 0) else (220, 200, 162)  # claws between buff toes
        return (214, 192, 152)
    if f == "back":
        return H_CREAM
    if row == 0:
        return tick(H_OCHRE, tu, tv, 15)
    return H_TAN if (f == "front" or (tu + tv) % 3) else H_OCHRE


def hare_shin(f, x, y, z, w, hh, d, tu, tv):
    if f == "back":
        return (92, 72, 54) if y > 1 else H_BROWN  # dark hock
    if f == "front":
        return H_TAN
    if f in ("top", "bottom"):
        return H_BROWN
    return tick(H_BROWN if y < 2 else H_TAN, tu, tv, 16, pd=80)


def hare_foot(f, x, y, z, w, hh, d, tu, tv):
    if f == "bottom":
        return H_SOLE if (tu + tv) % 4 else (80, 62, 48)
    if f == "front":
        return (66, 52, 40) if tu % 2 else (222, 204, 168)
    if f == "top":
        if z < 1:
            return (222, 204, 168)
        return lerp((206, 180, 136), H_BROWN, min(1, z / d * 1.3))
    if f == "back":
        return H_BROWN
    return (200, 174, 132) if z < 2 else (H_TAN if z < 4 else H_BROWN)


HARE_PAINTERS = {
    ("body", 0): hare_body, ("head", 0): hare_head, ("nose", 0): hare_nose,
    ("left_ear", 0): hare_ear, ("right_ear", 0): hare_ear, ("tail", 0): hare_tail,
    ("left_front_leg", 0): hare_front_leg, ("right_front_leg", 0): hare_front_leg,
    ("left_haunch", 0): hare_haunch, ("right_haunch", 0): hare_haunch,
    ("left_hind_foot", 0): hare_shin, ("right_hind_foot", 0): hare_shin,
    ("left_hind_foot", 1): hare_foot, ("right_hind_foot", 1): hare_foot,
}

# ---------------------------------------------------------------- pheasant
P_COPPER, P_COPPER_D, P_GOLD = (184, 92, 40), (128, 54, 26), (214, 140, 60)
P_MARK, P_CHEST, P_PURPLE = (44, 24, 20), (150, 56, 30), (70, 36, 72)
P_GREEN, P_GREEN_L, P_GREEN_D = (30, 78, 50), (58, 128, 80), (20, 48, 36)
P_RED, P_RED_L, P_WHITE = (186, 28, 30), (226, 62, 48), (236, 234, 226)
P_RUMP, P_BUFF, P_BAR = (126, 128, 98), (192, 162, 106), (48, 36, 28)
P_HORN, P_LEG = (214, 200, 150), (146, 132, 114)


def scallop(tu, tv, base, edge, mark):
    """Feathers in 2x2 cells, offset every other row of cells: a dark crescent at each feather tip."""
    cx = (tu + (tv // 2) % 2) % 2
    cy = tv % 2
    if cy == 1 and cx == 0:
        return mark
    if cy == 0 and cx == 1:
        return edge
    return base


def pheasant_body(f, x, y, z, w, hh, d, tu, tv):
    if f == "bottom":  # dark belly
        return (62, 40, 30) if h(tu, tv, 20) > 250 else (92, 50, 30)
    if f == "front":  # breast: deep copper, purple-black scallops
        return P_PURPLE if (tu + tv // 2) % 2 == 0 and tv % 2 else (P_CHEST if (tu + tv) % 3 else (172, 70, 36))
    if f == "back":
        return (78, 50, 34)
    if f == "top":
        if z > 5.5:  # slate-olive rump
            return P_RUMP if h(tu, tv, 21) > 200 else (104, 108, 84)
        return scallop(tu, tv, P_COPPER_D, (172, 110, 64), P_MARK)  # mantle: dark centres, buff edges
    if y > 3:
        return (106, 58, 32)
    if z > 6.5:
        return P_RUMP
    # flanks: golden copper with black spade marks
    return scallop(tu, tv, P_GOLD if z > 2 else P_COPPER, lerp(P_GOLD, (240, 190, 110), 0.4), P_MARK)


def iridescent(tu, tv, y, salt):
    n = h(tu, tv, salt)
    if y < 1.5:
        return P_GREEN_L if n < 300 else P_GREEN
    return P_PURPLE if n < 350 else P_GREEN_D


def pheasant_neck(f, x, y, z, w, hh, d, tu, tv):
    row = int(y)
    if f == "bottom" or row == 3:
        return scallop(tu, tv, P_COPPER, P_GOLD, P_MARK) if f != "bottom" else P_COPPER
    if row == 2:
        return P_WHITE  # the white collar
    return iridescent(tu, tv, y + 1, 22)


def pheasant_head(f, x, y, z, w, hh, d, tu, tv):
    row, col = int(y), int(z)
    if f == "top":
        return P_GREEN_L if (tu + tv) % 3 == 0 else (P_GREEN if z < 2 else P_GREEN_D)  # ear tufts at the back
    if f == "bottom":
        return P_PURPLE
    if f == "back":
        return iridescent(tu, tv, y, 23)
    if f == "front":
        if row == 1 and int(x) != 1:
            return P_RED
        return P_GREEN if row == 0 else P_PURPLE
    # sides: red wattle around a pale yellow eye
    if col <= 1 and row <= 1 or (col == 0 and row == 2):
        if row == 1 and col == 1:
            return (236, 200, 90)
        return P_RED_L if (row == 0 and col == 1) else P_RED
    if row == 0:
        return P_GREEN_L if col == 2 else P_GREEN
    return iridescent(tu, tv, y, 24)


def pheasant_beak(f, x, y, z, w, hh, d, tu, tv):
    return (170, 154, 110) if f == "bottom" else P_HORN


def pheasant_wing(f, x, y, z, w, hh, d, tu, tv):
    outer = f in ("left", "right")
    if f == "top":
        return (184, 150, 100)
    if f == "bottom":
        return (96, 70, 48)
    if f in ("front", "back"):
        return (130, 98, 64)
    row, col = int(y), int(z)
    # tell the outer face from the inner one: the left wing's outer side is +x
    is_outer = outer and ((f == "left") == (tu < 16))
    if not is_outer:
        return (178, 160, 132) if (tu + tv) % 3 else (160, 140, 112)
    if col >= 5:  # primaries at the tip: dark brown with buff spots
        return (178, 148, 102) if (col + 2 * row) % 3 == 0 else (92, 68, 46)
    if row == 0:  # lesser coverts: sandy, faint dark centres
        return (132, 98, 62) if h(tu, tv, 25) < 250 else (180, 140, 90)
    if row == 1:  # greater coverts: pale-fringed feathers
        return scallop(tu, tv, (166, 126, 80), (204, 176, 126), (112, 82, 54))
    if row == 3:  # secondaries' buff fringe
        return (196, 166, 116) if col % 2 else (150, 114, 72)
    return (150, 112, 70) if col % 2 == 0 else (122, 90, 58)


def tail_colour(f, x, z, w, tu, tv, zoff):
    zz = int(z) + zoff
    if f == "back":
        return P_BAR
    if zz % 2 == 1:
        return P_BAR  # crossbars
    if f in ("top", "bottom"):
        edge = x < 1 or x > w - 1
        c = (166, 96, 52) if edge else P_BUFF  # chestnut fringes, buff centre
        return lerp(c, (40, 30, 24), 0.25) if f == "bottom" else c
    return (166, 96, 52)


def pheasant_tail_base(f, x, y, z, w, hh, d, tu, tv):
    return tail_colour(f, x, z, w, tu, tv, 0) if f != "front" else P_RUMP


def pheasant_tail_tip(f, x, y, z, w, hh, d, tu, tv):
    if f == "front":
        return P_BUFF
    return tail_colour(f, x, z, w, tu, tv, 6)


def pheasant_leg(f, x, y, z, w, hh, d, tu, tv):
    if f == "back" and int(y) == 2:
        return (70, 60, 52)  # spur
    return P_LEG if (tu + tv) % 2 else (128, 114, 98)


def pheasant_foot(f, x, y, z, w, hh, d, tu, tv):
    if f == "front":
        return (60, 52, 46)  # claws
    if f == "top":
        return (128, 114, 98) if int(x) == int(z) % 2 else P_LEG
    return (116, 104, 90)


PHEASANT_PAINTERS = {
    ("body", 0): pheasant_body, ("head", 0): pheasant_neck, ("head", 1): pheasant_head, ("head", 2): pheasant_beak,
    ("left_wing", 0): pheasant_wing, ("right_wing", 0): pheasant_wing,
    ("tail", 0): pheasant_tail_base, ("tail", 1): pheasant_tail_tip,
    ("left_leg", 0): pheasant_leg, ("right_leg", 0): pheasant_leg,
    ("left_leg", 1): pheasant_foot, ("right_leg", 1): pheasant_foot,
}

# ---------------------------------------------------------------- plucked pheasant
SKIN_PINK, SKIN_YELLOW, FOLLICLE = (238, 198, 168), (236, 212, 160), (206, 160, 130)


def pluck(img, regions):
    """Pale poultry skin with goose-bump follicles; legs, feet, beak and eyes stay; tail becomes pale quill stubs."""
    out = img.copy()
    src, px = img.load(), out.load()
    keep = {("left_leg", 0), ("right_leg", 0), ("left_leg", 1), ("right_leg", 1), ("head", 2)}
    quills = {("tail", 0), ("tail", 1)}
    for key, (u, v, rw, rh) in regions.items():
        for ty in range(v, v + rh):
            for tx in range(u, u + rw):
                r, g, b, a = src[tx, ty]
                if not a or key in keep:
                    continue
                lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
                if key in quills:
                    c = (234, 224, 198) if tx % 2 else (210, 194, 160)  # shafts side by side
                elif key == ("head", 1) and r > 150 and g < 90:
                    c = (200, 70, 70)  # wattle stays red
                elif key == ("head", 1) and r > 220 and g > 180:
                    c = (40, 30, 26)  # eye
                else:
                    c = lerp(SKIN_PINK, SKIN_YELLOW, h(tx // 2, ty, 30) / 1000)
                    c = lerp(c, (180, 130, 104), max(0.0, 0.35 - lum) * 0.8)  # keep a hint of the feather shading
                    if h(tx, ty, 31) < 160:
                        c = lerp(c, FOLLICLE, 0.6)
                px[tx, ty] = (*c, a)
    return out


def main():
    for name, parts, painters in (("hare", HARE, HARE_PAINTERS), ("pheasant", PHEASANT, PHEASANT_PAINTERS)):
        img, regions = paint(parts, painters)
        skinned = skin(img.copy()) if name == "hare" else pluck(img, regions)
        assert skinned.size == img.size
        assert skinned.getchannel("A").tobytes() == img.getchannel("A").tobytes()
        for sub, im in (("wildlife", img), ("skinned/wildlife", skinned)):
            dst = ROOT / sub / f"{name}.png"
            dst.parent.mkdir(parents=True, exist_ok=True)
            im.save(dst)
            print("wrote", dst.relative_to(ROOT))


if __name__ == "__main__":
    main()
    import gen_small_models  # these animals now have their own models and skins, which this would overwrite
    gen_small_models.main()
