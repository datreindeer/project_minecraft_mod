"""Paint the river fauna (data: scripts/gen_fauna_river.py; Java: wildlife/river/, client/wildlife/river/).

- Entities (64x64, painted from 3D like gen_forest_critter_textures.py): Beaver (rich brown, darker back, flat black
  scaly tail, orange incisors, webbed hind feet, a stick for its mouth) and Otter (sleek chocolate, pale throat,
  whiskers, a fish for its mouth), plus their skinned carcass textures; the Mosquito Swarm's specks (16x16).
- Items (16x16): beaver/otter meat raw and cooked, Beaver Tail raw and roasted, raw and cured Beaver/Otter Pelts,
  Castoreum, Beaver Tooth, Castoreum Salve, Beaver Tooth Knife, Beaver Fur Hat and Otter Fur Cape (vanilla leather
  silhouettes recoloured, like the wolf fur), three spawn eggs.
- Blocks: Beaver Dam (sticks in mud), the Stretching Hoop's willow and the pelts laced into it (skin and fur side,
  raw and cured).
- Equipment layers: beaver_fur (the hat) and otter_fur (the cape), on the vanilla leather UVs.

Usage: python scripts/gen_fauna_river_textures.py [--java]   (--java prints the model cube lines; deterministic)
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from critter_paint import cube, fur, h, java_cubes, lerp, pack, paint, part, save, shade, skinned, smooth  # noqa: E402
from gen_bronze_prep_textures import lum, vanilla  # noqa: E402
import gen_wildlife_item_textures as wit  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK, EQUIP = TEX / "item", TEX / "block", TEX / "entity/equipment"
CLEAR = (0, 0, 0, 0)


def hexc(s):
    return wit.hexc(s)[:3]


# ======================================================================= beaver
BV = dict(back=(78, 48, 28), coat=(108, 70, 40), light=(138, 94, 58), belly=(150, 112, 76), cheek=(124, 86, 54),
          tail=(50, 45, 44), tail_hi=(78, 71, 67), tail_lo=(32, 29, 29), foot=(44, 34, 28), web=(66, 52, 44),
          claw=(24, 18, 14), tooth=(214, 118, 38), tooth_hi=(242, 162, 72), nose=(30, 24, 22), eye=(10, 8, 6),
          glint=(218, 218, 224), ear=(64, 40, 24), bark=(112, 92, 66), bark_dark=(78, 62, 44), wood=(222, 196, 146))


def bv_coat(face, X, Y, Z, n, tx, ty):
    """Glossy brown: darker along the back, paler on the flanks, buff belly; dark guard hairs flecked in."""
    if face == "bottom":
        c = BV["belly"]
    else:
        c = lerp(BV["back"], BV["light"], smooth((Y - 15.0) / 6.5))
    if h(tx, ty, 11) < 0.12:
        c = shade(c, 0.78)
    elif h(tx, ty, 13) > 0.93:
        c = shade(c, 1.18)  # a sheen on the guard hairs
    return fur(c, tx, ty, 12, 0.07)


def bv_skull(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right") and abs(Y - 16.8) < 0.6 and abs(Z - (-7.5)) < 0.6:
        return BV["eye"]
    if face in ("left", "right") and abs(Y - 16.2) < 0.5 and abs(Z - (-7.5)) < 0.5:
        return BV["glint"] if face == "left" else BV["eye"]
    if face == "bottom":
        return fur(BV["belly"], tx, ty, 14, 0.05)
    c = lerp(BV["back"], BV["cheek"], smooth((Y - 15.5) / 4.0))
    return fur(c, tx, ty, 15, 0.07)


def bv_muzzle(face, X, Y, Z, n, tx, ty):
    if face == "front" and Y > 19.2:
        return BV["nose"]  # the split lip under the nose
    c = BV["cheek"] if face != "top" else lerp(BV["back"], BV["cheek"], 0.5)
    if face in ("left", "right", "front") and h(tx, ty, 16) > 0.8:
        return shade(c, 0.75)  # whisker pores
    return fur(c, tx, ty, 17, 0.05)


def bv_nose(face, X, Y, Z, n, tx, ty):
    return shade(BV["nose"], 1.6) if face == "top" else BV["nose"]


def bv_teeth(face, X, Y, Z, n, tx, ty):
    return BV["tooth_hi"] if X < 0.05 and X > -0.95 and tx % 2 == 0 else BV["tooth"]


def bv_ear(face, X, Y, Z, n, tx, ty):
    return BV["ear"] if face != "front" else shade(BV["ear"], 0.7)


def bv_tail(face, X, Y, Z, n, tx, ty):
    """Flat, leathery, black-grey with a raised net of scales."""
    if face in ("top", "bottom"):
        # scales: a diamond net in texture space
        i, j = tx - 0.5 * (ty % 2), ty
        edge = (i % 2 < 0.6) or (j % 2 == 0 and h(tx, ty, 18) < 0.4)
        c = BV["tail_lo"] if edge else lerp(BV["tail"], BV["tail_hi"], h(tx // 2, ty, 19))
        return c if face == "top" else shade(c, 0.9)
    return BV["tail_lo"]


def bv_tailroot(face, X, Y, Z, n, tx, ty):
    return fur(lerp(BV["back"], BV["coat"], 0.4), tx, ty, 20, 0.06)


def bv_leg(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return BV["foot"]
    if Y > 22.5:
        return fur(BV["foot"], tx, ty, 21, 0.05)
    return fur(lerp(BV["coat"], BV["back"], 0.3), tx, ty, 22, 0.06)


def bv_fpaw(face, X, Y, Z, n, tx, ty):
    if face in ("front", "top") and tx % 2 == 0:
        return BV["claw"]
    return BV["foot"]


def bv_hfoot(face, X, Y, Z, n, tx, ty):
    """Big webbed hind feet: dark toes with greyer webs between them."""
    if face == "top":
        return BV["foot"] if tx % 2 == 0 else BV["web"]
    if face == "front":
        return BV["claw"] if tx % 2 == 0 else BV["web"]
    return BV["foot"]


def bv_stick(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right"):
        return BV["wood"]  # the gnawed pale ends
    return BV["bark"] if h(tx, ty, 23) > 0.3 else BV["bark_dark"]


BEAVER = [
    part("body", None, (0, 18.5, 0), (0, 0, 0), [
        cube("bv_fore", -3, -2.5, -5, 6, 5, 6, bv_coat),
        cube("bv_rump", -4, -3.5, 0, 8, 7, 6, bv_coat),
    ]),
    part("tail", "body", (0, 2.0, 6), (-0.35, 0, 0), [
        cube("bv_tailroot", -1.5, -1.5, -1, 3, 2, 2, bv_tailroot),
        cube("bv_tail", -2, -0.5, 0, 4, 1, 7, bv_tail),
    ]),
    part("head", "body", (0, -0.5, -5), (0, 0, 0), [
        cube("bv_skull", -2.5, -2.5, -4, 5, 4, 4, bv_skull),
        cube("bv_muzzle", -2, -1, -6, 4, 3, 2, bv_muzzle),
        cube("bv_nose", -1, -1.2, -6.4, 2, 1, 1, bv_nose),
        cube("bv_teeth", -1, 1.5, -5.9, 2, 2, 0, bv_teeth),
        cube("bv_ear", -2.8, -2.8, -1.5, 1, 1, 1, bv_ear),
        cube("bv_ear", 1.8, -2.8, -1.5, 1, 1, 1, bv_ear, True),
    ]),
    part("stick", "head", (0, 0, 0), (0, 0, 0), [cube("bv_stick", -5, 1.5, -7, 10, 1, 1, bv_stick)]),
    part("right_front_leg", None, (-2, 21, -3), (0, 0, 0), [cube("bv_fleg", -1, 0, -1, 2, 3, 2, bv_leg),
                                                         cube("bv_fpaw", -1, 2, -2, 2, 1, 1, bv_fpaw)]),
    part("left_front_leg", None, (2, 21, -3), (0, 0, 0), [cube("bv_fleg", -1, 0, -1, 2, 3, 2, bv_leg, True),
                                                       cube("bv_fpaw", -1, 2, -2, 2, 1, 1, bv_fpaw, True)]),
    part("right_hind_leg", None, (-3, 20, 3.5), (0, 0, 0), [cube("bv_hleg", -1.5, -1, -1.5, 3, 4, 3, bv_leg),
                                                         cube("bv_hfoot", -1.5, 3, -3.5, 3, 1, 3, bv_hfoot)]),
    part("left_hind_leg", None, (3, 20, 3.5), (0, 0, 0), [cube("bv_hleg", -1.5, -1, -1.5, 3, 4, 3, bv_leg, True),
                                                       cube("bv_hfoot", -1.5, 3, -3.5, 3, 1, 3, bv_hfoot, True)]),
]

# ======================================================================= otter
OT = dict(back=(56, 36, 24), coat=(80, 54, 34), side=(96, 66, 42), sheen=(128, 96, 64), throat=(196, 176, 146),
          throat_lo=(170, 148, 118), belly=(112, 84, 58), nose=(22, 18, 16), eye=(8, 6, 6), glint=(220, 220, 226),
          whisk=(226, 220, 206), foot=(46, 32, 22), claw=(22, 16, 12), fish=(168, 176, 178), fish_back=(84, 98, 104),
          fish_belly=(224, 226, 220), fish_fin=(120, 110, 96))


def ot_sleek(c, tx, ty, salt):
    """Short dense fur with a wet-looking sheen: fine speckle, a light streak now and then."""
    if h(tx, ty // 2, salt) > 0.9:
        c = lerp(c, OT["sheen"], 0.6)
    return fur(c, tx, ty, salt + 1, 0.05, streak=False)


def ot_coat(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        # pale throat and chest fading into the brown belly
        return ot_sleek(lerp(OT["throat_lo"], OT["belly"], smooth((Z + 6) / 6)), tx, ty, 30)
    return ot_sleek(lerp(OT["back"], OT["side"], smooth((Y - 17.0) / 5.0)), tx, ty, 32)


def ot_neck(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or (face in ("left", "right", "front") and Y > 19.6):
        return ot_sleek(OT["throat"], tx, ty, 34)
    return ot_sleek(lerp(OT["back"], OT["side"], smooth((Y - 17.5) / 3)), tx, ty, 36)


def ot_skull(face, X, Y, Z, n, tx, ty):
    if face in ("left", "right") and abs(Y - 17.6) < 0.6 and abs(Z - (-10.4)) < 0.6:
        return OT["eye"]
    if face in ("left", "right") and abs(Y - 17.0) < 0.5 and abs(Z - (-10.4)) < 0.5:
        return OT["glint"] if face == "left" else OT["eye"]
    if face == "bottom" or (face in ("left", "right") and Y > 19.4):
        return ot_sleek(OT["throat"], tx, ty, 38)
    return ot_sleek(lerp(OT["back"], OT["coat"], smooth((Y - 16.5) / 3.5)), tx, ty, 40)


def ot_muzzle(face, X, Y, Z, n, tx, ty):
    if face == "bottom" or (face != "top" and Y > 19.2):
        return ot_sleek(OT["throat"], tx, ty, 42)  # pale chin and lips
    if face in ("left", "right", "front") and h(tx, ty, 43) > 0.75:
        return shade(OT["coat"], 0.7)
    return ot_sleek(OT["coat"], tx, ty, 44)


def ot_nose(face, X, Y, Z, n, tx, ty):
    return shade(OT["nose"], 1.7) if face == "top" else OT["nose"]


def ot_whisk(face, X, Y, Z, n, tx, ty):
    if abs(X) < 1.6:
        return CLEAR  # inside the muzzle
    return OT["whisk"] if (tx + ty) % 2 == 0 or abs(X) > 2.6 else CLEAR


def ot_ear(face, X, Y, Z, n, tx, ty):
    return shade(OT["back"], 0.9)


def ot_tail(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return ot_sleek(OT["belly"], tx, ty, 46)
    return ot_sleek(lerp(OT["back"], OT["coat"], 0.4), tx, ty, 47)


def ot_leg(face, X, Y, Z, n, tx, ty):
    if face == "bottom":
        return OT["foot"]
    return ot_sleek(lerp(OT["back"], OT["foot"], smooth((Y - 21) / 3)), tx, ty, 48)


def ot_foot(face, X, Y, Z, n, tx, ty):
    if face == "front":
        return OT["claw"] if tx % 2 == 0 else OT["foot"]
    return OT["foot"] if (tx + ty) % 3 else shade(OT["foot"], 1.3)  # webbed toes


def ot_fish(face, X, Y, Z, n, tx, ty):
    """A small brown trout across the mouth: dark back, speckled silver flank, pale belly, tail fin at +x."""
    if X > 3.0:
        return OT["fish_fin"] if face != "top" else shade(OT["fish_fin"], 0.85)
    if face == "top":
        return OT["fish_back"]
    if face == "bottom":
        return OT["fish_belly"]
    if Y < 20.2:
        return OT["fish_back"] if h(tx, ty, 49) > 0.25 else shade(OT["fish_back"], 0.7)
    if X < -3.2 and face in ("front", "back") and Y < 20.8:
        return OT["eye"]
    c = OT["fish"] if Y < 21.0 else OT["fish_belly"]
    return shade(c, 0.6) if h(tx, ty, 50) > 0.85 else c


OTTER = [
    part("body", None, (0, 19.5, 0), (0, 0, 0), [
        cube("ot_body", -2.5, -2, -6, 5, 4, 12, ot_coat),
        cube("ot_hips", -3, -2.5, 1, 6, 5, 5, ot_coat),
    ]),
    part("tail", "body", (0, -0.5, 6), (-0.25, 0, 0), [cube("ot_tail1", -1.5, -1.5, 0, 3, 3, 5, ot_tail)]),
    part("tail_tip", "tail", (0, 0, 5), (-0.1, 0, 0), [cube("ot_tail2", -1, -1, 0, 2, 2, 5, ot_tail)]),
    part("head", "body", (0, -0.5, -6), (0, 0, 0), [
        cube("ot_neck", -2, -1.5, -2, 4, 3, 2, ot_neck),
        cube("ot_skull", -2.5, -2.5, -5, 5, 4, 3, ot_skull),
        cube("ot_muzzle", -1.5, -1, -7, 3, 2, 2, ot_muzzle),
        cube("ot_nose", -1, -1.2, -7.4, 2, 1, 1, ot_nose),
        cube("ot_whisk", -3.5, 0, -6.5, 7, 1, 0, ot_whisk),
        cube("ot_ear", -2.7, -2.7, -2.5, 1, 1, 1, ot_ear),
        cube("ot_ear", 1.7, -2.7, -2.5, 1, 1, 1, ot_ear, True),
    ]),
    part("fish", "head", (0, 0, 0), (0, 0, 0), [cube("ot_fish", -4, 0.5, -6.8, 8, 2, 1, ot_fish)]),
    part("right_front_leg", None, (-1.8, 21, -4), (0, 0, 0), [cube("ot_fleg", -1, 0, -1, 2, 3, 2, ot_leg)]),
    part("left_front_leg", None, (1.8, 21, -4), (0, 0, 0), [cube("ot_fleg", -1, 0, -1, 2, 3, 2, ot_leg, True)]),
    part("right_hind_leg", None, (-2, 21, 4), (0, 0, 0), [cube("ot_hleg", -1, -1, -1, 2, 3, 2, ot_leg),
                                                       cube("ot_hfoot", -1, 2, -2, 2, 1, 3, ot_foot)]),
    part("left_hind_leg", None, (2, 21, 4), (0, 0, 0), [cube("ot_hleg", -1, -1, -1, 2, 3, 2, ot_leg, True),
                                                     cube("ot_hfoot", -1, 2, -2, 2, 1, 3, ot_foot, True)]),
]


def mosquito():
    """The swarm's specks share one 1x1x1 cube UV (0,0): a dark body with a faint grey glint of wings on top."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    body, wing = (34, 30, 28, 255), (150, 150, 146, 255)
    for face, tx, ty, *_ in wit_face_texels(0, 0, 1, 1, 1):
        im.putpixel((tx, ty), wing if face == "top" else body)
    return im


def wit_face_texels(u, v, w, hh, d):
    from wildlife_paint import face_texels
    return face_texels(u, v, 0, 0, 0, w, hh, d)


# ======================================================================= items
def shaded(rows, pal, ramp=None, salt=0):
    """Grid -> image. Letters in pal are flat colours; letters in ramp ({letter: [dark..light]}) shade from light at the
    top-left to dark at the bottom-right, with a little per-texel noise."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        assert len(row) == 16, (row, len(row))
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ramp and ch in ramp:
                tones = ramp[ch]
                s = 1 - (x / 15 * 0.45 + y / 15 * 0.55) + (h(x, y, salt) - 0.5) * 0.35
                c = tones[max(0, min(len(tones) - 1, int(s * len(tones))))]
                im.putpixel((x, y), (*hexc(c), 255))
            else:
                im.putpixel((x, y), (*hexc(pal[ch]), 255))
    return im


JOINT = [
    "................",
    "................",
    "......oooo......",
    "....ooRRRRoo....",
    "...oRRRrRRRRo...",
    "..oRRrRRRRrRRo..",
    "..oRRRRRrRRRRo..",
    ".oRRrRRRRRRrRRo.",
    ".oRRRRrRRRRRRRo.",
    ".oRrRRRRRrRRRRo.",
    "..oRRRRRRRRRro..",
    "..oFFRRRrRRRFo..",
    "...oFFFFFFFFoBB.",
    "....ooooooooBWB.",
    ".............BB.",
    "................",
]
FILLET = [
    "................",
    "................",
    "................",
    "..........ooo...",
    "........ooRRRo..",
    "......ooRRrRRo..",
    "....ooRRRRRRo...",
    "...oRRrRRFRRo...",
    "..oRRRRRRrRo....",
    "..oRrRRFRRo.....",
    ".oRRRRRrRo......",
    ".oRRFRRRo.......",
    ".oFRRRoo........",
    "..oooo..........",
    "................",
    "................",
]
TAIL = [
    "................",
    "......oooo......",
    ".....oSSSSo.....",
    "....oSSSSSSo....",
    "...oSSSSSSSSo...",
    "...oSSSSSSSSo...",
    "...oSSSSSSSSo...",
    "...oSSSSSSSSo...",
    "...oSSSSSSSSo...",
    "...oSSSSSSSSo...",
    "....oSSSSSSo....",
    "....oSSSSSSo....",
    ".....oSSSSo.....",
    "......oKKo......",
    "......oKKo......",
    ".......oo.......",
]


def scaly(rows, scale_dark, scale_mid, scale_hi, outline, root, char=None, salt=0):
    """The beaver tail icon: a diamond net of scales, lit from the top-left; optional char marks."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "o":
                c = outline
            elif ch == "K":
                c = root[(x + y) % 2]
            elif ch == "S":
                net = (x + y) % 3 == 0 or (x - y) % 3 == 0
                lit = 1 - (x / 15 * 0.5 + y / 15 * 0.5)
                c = scale_dark if net else (scale_hi if lit > 0.55 and h(x, y, salt) > 0.4 else scale_mid)
                if char and h(x, y, salt + 1) < char:
                    c = "#1c120c"
            else:
                continue
            im.putpixel((x, y), (*hexc(c), 255))
    return im


OVAL = [
    "................",
    ".....ooooo......",
    "...ooxXXXxoo....",
    "..oxXXXXXXXxo...",
    "..oXXXXXXXXXo...",
    ".oxXXXXXXXXXxo..",
    ".oXXXXXXXXXXXo..",
    ".oXXXXXXXXXXXo..",
    ".oXXXXXXXXXXXo..",
    ".oxXXXXXXXXXxo..",
    "..oXXXXXXXXXo...",
    "..oxXXXXXXXxo...",
    "...ooxXXXxoo....",
    ".....ooooo......",
    "................",
    "................",
]
LONG = [
    "......oooo......",
    ".....oXXXXo.....",
    ".....oXEXEo.....",
    "....oXXXXXXo....",
    "....oxXXXXxo....",
    "....oXXXXXXo....",
    "...oxXXXXXXxo...",
    "...oXXXXXXXXo...",
    "...oXXXXXXXXo...",
    "...oxXXXXXXxo...",
    "....oXXXXXXo....",
    "....oXXXXXXo....",
    ".....oXXXXo.....",
    "......oXXo......",
    "......oXXo......",
    ".......oo.......",
]
FLESH = ["#e2a7a0", "#c76f6a", "#a64a48"]
LEATHER = ["#e6cfa2", "#cfae7c", "#b58e5e"]


def pelt_icon(rows, fur_tones, edge, cured, salt):
    """A pelt seen from the fur side: speckled fur, the edge raw and bloody or cured tan with lacing holes."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch == "o":
                c = "#24170e" if not cured else "#3a2414"
            elif ch == "E":
                c = "#120c08"
            elif ch == "x":
                c = edge[(x * 7 + y * 3) % len(edge)] if not cured else (edge[0] if (x + y) % 3 else "#5a4028")
            else:
                lit = 1 - (x / 15 * 0.45 + y / 15 * 0.55) + (h(x, y, salt) - 0.5) * 0.5
                c = fur_tones[max(0, min(len(fur_tones) - 1, int(lit * len(fur_tones))))]
                if not cured and h(x, y, salt + 2) > 0.97:
                    c = edge[1]  # bloody patches on a raw pelt
            im.putpixel((x, y), (*hexc(c), 255))
    return im


BV_FUR = ["#3e2716", "#553620", "#6c472a", "#875b36", "#a07045"]
OT_FUR = ["#2a1a10", "#3a2516", "#4e331f", "#664530", "#82603f"]

CASTOREUM = [
    "................",
    "................",
    "...oo......oo...",
    "..oCCo....oCCo..",
    "..oCco....oCco..",
    "...oCo....oCo...",
    "...oCCo..oCCo...",
    "..oCCCCooCCCCo..",
    ".oCCLCCCCCCLCCo.",
    ".oCLCCCCCCCLCCo.",
    ".oCCCCcCCcCCCCo.",
    ".oCCCcCooCcCCCo.",
    "..oCCCo..oCCCo..",
    "...ooo....ooo...",
    "................",
    "................",
]
TOOTH = [
    "................",
    "..........oo....",
    ".........oHTo...",
    "........oTTTo...",
    "........oTTWo...",
    ".......oTTWo....",
    ".......oTTWo....",
    "......oTTWo.....",
    "......oTWWo.....",
    ".....oTWWo......",
    ".....oWWWo......",
    "....oWWWWo......",
    "....oWWWwo......",
    "....owWwo.......",
    ".....ooo........",
    "................",
]
SALVE = [
    "................",
    "................",
    "................",
    "................",
    "....oooooooo....",
    "...oSsSSSSSSo...",
    "..oSsSSSSSsSSo..",
    "..oBSSSSSSSSBo..",
    "..oBbbbbbbbbBo..",
    "..oBBBBBBBBBBo..",
    "..obBkBBkBBBbo..",
    "..oBBBBBBBBBBo..",
    "..obBBBkBBkBbo..",
    "...oBBBBBBBBo...",
    "....oooooooo....",
    "................",
]
KNIFE = [
    "................",
    "............oo..",
    "...........oHTo.",
    "..........oTTWo.",
    ".........oTTWo..",
    "........oTTWo...",
    ".......oTTWo....",
    "......oCcCo.....",
    ".....ocCcCo.....",
    "....oSScoo......",
    "...oSsSo........",
    "..oSsSo.........",
    ".oSsSo..........",
    ".osSo...........",
    "..oo............",
    "................",
]


def fur_icon(name, tones, salt, band=None):
    """Vanilla leather icon silhouette and shading recoloured as fur (tones dark..light); band: rows recoloured."""
    src = vanilla(f"item/leather_{name}")
    px = src.load()
    cols = sorted({px[x, y] for y in range(16) for x in range(16) if px[x, y][3]}, key=lum)
    out = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            rank = cols.index(px[x, y])
            if rank == 0:
                c = "#1c130c"
            else:
                t = rank / (len(cols) - 1) + (h(x, y // 2 + x % 2, salt) - 0.5) * 0.35
                c = tones[max(0, min(len(tones) - 1, int(t * len(tones))))]
                if band and y in band and rank > 1:
                    c = band[y]
            out.putpixel((x, y), (*hexc(c), 255))
    return out


def fur_layer(path, tones, salt, collar=None):
    """Vanilla leather equipment layer recoloured as fur; collar: a paler band round the neck (rows of the body/head)."""
    src = vanilla(path)
    px = src.load()
    out = Image.new("RGBA", src.size, CLEAR)
    for y in range(src.height):
        for x in range(src.width):
            if not px[x, y][3]:
                continue
            s = (lum(px[x, y]) - 153) / (214 - 153)
            t = 0.25 + 0.5 * s + 0.35 * (h(x, y // 2 + x % 2, salt) - 0.5)
            if 32 <= x < 40 and 20 <= y < 32:
                t -= 0.2  # the back
            c = tones[max(0, min(len(tones) - 1, int(t * len(tones))))]
            if collar and collar(x, y):
                c = "#b49a78" if (x + y) % 2 else "#a08664"
            out.putpixel((x, y), (*hexc(c), 255))
    return out


def eggs():
    K = "#140e0a"
    tail = [(x, y, "#2c2826") for y in (13, 14) for x in range(6, 10)] + [(7, 15, "#2c2826"), (8, 15, "#2c2826")]
    beaver = wit.egg(["#a6774a", "#8a5e38", "#714a2a", "#583820", "#3e2616", "#2a190e"],
                     [(6, 5, K), (9, 5, K)], [(x, 15) for x in (7, 8)], tail + [(7, 8, "#d77a26"), (8, 8, "#d77a26"), (7, 7, K), (8, 7, K)])
    C = "#d8c3a0"
    throat = [(x, y, C) for y in range(7, 11) for x in range(6, 10)]
    otter = wit.egg(["#6e4c32", "#5a3c26", "#4a301e", "#3a2416", "#28180e", "#1c1008"],
                    [(6, 5, K), (9, 5, K)], (), throat + [(7, 6, "#1a120c"), (8, 6, "#1a120c"), (5, 6, "#efe8da"), (10, 6, "#efe8da")])
    W = "#c8ccd0"
    specks = [(x, y, K) for x, y in ((4, 4), (7, 3), (10, 5), (5, 8), (9, 7), (12, 9), (3, 11), (7, 11), (11, 12), (6, 14))]
    wings = [(x + 1, y, W) for x, y, _ in specks[::2]]
    mosquito = wit.egg(["#8a8e84", "#73776e", "#5f625a", "#4b4e47", "#33352f", "#22231f"], specks, (), wings)
    return {"beaver_spawn_egg": beaver, "otter_spawn_egg": otter, "mosquito_swarm_spawn_egg": mosquito}


def items():
    raw = {"o": "#4a1414", "R": "#a3302c", "r": "#7d2220", "F": "#ecd0b8", "B": "#6b6255", "W": "#efe8d6"}
    cooked = {"o": "#3a2010", "R": "#9a5a2c", "r": "#6e3c1c", "F": "#dcae68", "B": "#6b6255", "W": "#efe8d6"}
    raw_otter = {"o": "#3c1018", "R": "#8e2a34", "r": "#6a1c26", "F": "#e6c4b6"}
    cooked_otter = {"o": "#301c10", "R": "#86502a", "r": "#5e3418", "F": "#d4a464"}
    out = {
        "raw_beaver": shaded(JOINT, raw, {"R": ["#7d2220", "#a3302c", "#c2463c", "#d8645a"]}, 3),
        "cooked_beaver": shaded(JOINT, cooked, {"R": ["#6e3c1c", "#9a5a2c", "#b8743a", "#d0924e"]}, 4),
        "raw_otter": shaded(FILLET, raw_otter, {"R": ["#6a1c26", "#8e2a34", "#ab3f46", "#c25a5c"]}, 5),
        "cooked_otter": shaded(FILLET, cooked_otter, {"R": ["#5e3418", "#86502a", "#a26a38", "#bc864c"]}, 6),
        "beaver_tail": scaly(TAIL, "#1e1a19", "#3a3432", "#5a524e", "#100c0b", ("#5a3a22", "#6e4a2c"), salt=7),
        "roasted_beaver_tail": scaly(TAIL, "#4a2a14", "#8a5428", "#b47a3c", "#24140a", ("#6a4022", "#7c4e2a"), char=0.12, salt=8),
        "raw_beaver_pelt": pelt_icon(OVAL, BV_FUR, FLESH, False, 9),
        "beaver_pelt": pelt_icon(OVAL, BV_FUR, LEATHER, True, 10),
        "raw_otter_pelt": pelt_icon(LONG, OT_FUR, FLESH, False, 11),
        "otter_pelt": pelt_icon(LONG, OT_FUR, LEATHER, True, 12),
        "castoreum": shaded(CASTOREUM, {"o": "#3e2510", "c": "#7e5326", "L": "#d8ac64"},
                            {"C": ["#86592a", "#a8783c", "#bf8e4c"]}, 13),
        "beaver_tooth": shaded(TOOTH, {"o": "#3a2412", "T": "#d77a26", "H": "#f4ab5a", "W": "#efe4c8", "w": "#c9b996"}),
        "castoreum_salve": shaded(SALVE, {"o": "#3a2c20", "S": "#c49a44", "s": "#e8c870", "B": "#e9e2d0", "b": "#c4baa2", "k": "#3a3430"}),
        "beaver_tooth_knife": shaded(KNIFE, {"o": "#2e1c10", "T": "#d77a26", "H": "#f4ab5a", "W": "#efe4c8",
                                             "C": "#b5977d", "c": "#8a6c52", "S": "#8a6038", "s": "#64421f"}),
        "beaver_fur_hat": fur_icon("helmet", BV_FUR, 14, band={7: "#3a2414"}),
        "otter_fur_cape": fur_icon("chestplate", OT_FUR, 15, band={1: "#c4ab86", 2: "#b49a78"}),
    }
    out.update(eggs())
    return out


# ======================================================================= blocks
def beaver_dam():
    """Peeled and barked sticks laid every which way, packed with dark mud (tiles seamlessly)."""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            c = lerp((52, 40, 28), (78, 62, 44), h(x, y, 60))
            if h(x // 2, y // 2, 61) > 0.85:
                c = (96, 84, 62)  # silt and gravel specks
            px[x, y] = (*c, 255)
    # (x0, y0, dx, dy, length, peeled): every stick wraps round the tile edges
    sticks = [(0, 2, 1, 0, 16, False), (3, 6, 1, 1, 9, True), (9, 9, 1, -1, 7, False), (0, 12, 1, 0, 11, True),
              (12, 0, 0, 1, 7, False), (5, 14, 1, -1, 6, False), (14, 10, 1, 1, 5, True)]
    for i, (x0, y0, dx, dy, n, peeled) in enumerate(sticks):
        for k in range(n):
            x, y = (x0 + dx * k) % 16, (y0 + dy * k) % 16
            if peeled:
                c = (214, 190, 140) if k not in (0, n - 1) else (240, 222, 178)  # gnawed clean
                lo = (168, 140, 96)
            else:
                c = (108, 88, 62) if h(x, y, 62 + i) > 0.35 else (84, 68, 48)
                lo = (60, 48, 34)
            px[x, y] = (*c, 255)
            # a second row of the stick, in shadow, so it reads as round
            if dx and not dy:
                px[x, (y + 1) % 16] = (*lo, 255)
            elif dy and not dx:
                px[(x + 1) % 16, y] = (*lo, 255)
            else:
                px[x, (y + 1) % 16] = (*lo, 255)
    return im


def willow():
    """Thin bent willow rod: grey-green bark with pale scuffs (used along the hoop's bars)."""
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = lerp((92, 96, 66), (124, 122, 84), h(x, y // 3, 63))
            if h(x, y, 64) > 0.9:
                c = (176, 168, 128)
            if y % 4 == 0 and h(x, y, 65) > 0.6:
                c = shade(c, 0.75)
            im.putpixel((x, y), (*c, 255))
    return im


def hoop_pelt(kind, side, cured):
    """The pelt laced into the hoop, 16x16 with alpha: an oval (beaver) or a long oval (otter) inside x/y 2..13,
    twine laces running out to the ring. side 'skin' (flesh, then parchment) or 'fur'."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    rx, ry = (5.4, 5.2) if kind == "beaver" else (3.6, 5.6)
    cx, cy = 7.5, 7.8
    tones = BV_FUR if kind == "beaver" else OT_FUR
    for y in range(16):
        for x in range(16):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 1.0:
                if side == "fur":
                    t = 0.55 + (h(x, y, 66) - 0.5) * 0.6 - (y - cy) / 20
                    c = hexc(tones[max(0, min(len(tones) - 1, int(t * len(tones))))])
                elif cured:
                    c = lerp(hexc(LEATHER[0]), hexc(LEATHER[2]), d * 0.8 + h(x, y, 67) * 0.3)
                else:
                    c = lerp(hexc(FLESH[0]), hexc(FLESH[2]), d * 0.7 + h(x, y, 68) * 0.4)
                    if h(x, y, 69) > 0.88:
                        c = (236, 214, 196)  # fat left on
                if d > 0.78:
                    c = shade(c, 0.8)  # the rolled edge
                im.putpixel((x, y), (*c, 255))
    # laces: from the pelt's rim straight out to the hoop, eight of them
    for k in range(8):
        a = k * math.pi / 4 + math.pi / 8
        ex, ey = cx + math.cos(a) * rx, cy + math.sin(a) * ry
        for step in range(1, 8):
            x, y = round(ex + math.cos(a) * step), round(ey + math.sin(a) * step)
            if not (1 <= x <= 14 and 1 <= y <= 14) or im.getpixel((x, y))[3]:
                if not (1 <= x <= 14 and 1 <= y <= 14):
                    break
                continue
            im.putpixel((x, y), (181, 151, 125, 255))
    return im


def blocks():
    out = {"beaver_dam": beaver_dam(), "pelt_hoop": willow()}
    for kind in ("beaver", "otter"):
        for cured in (False, True):
            state = kind + ("_dry" if cured else "")
            for side in ("skin", "fur"):
                out[f"pelt_hoop_{state}_{side}"] = hoop_pelt(kind, side, cured)
    return out


def main():
    for name, parts, size in (("beaver", BEAVER, (64, 64)), ("otter", OTTER, (64, 64))):
        uv = pack(parts, *size)
        img = paint(parts, uv, size)
        if "--java" in sys.argv:
            print(name)
            java_cubes(parts, uv)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")
    save(mosquito(), "wildlife/mosquito_swarm.png")
    for name, im in items().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
    for name, im in blocks().items():
        im.save(BLOCK / f"{name}.png")
    BV_HAT = ["#3e2716", "#553620", "#6c472a", "#875b36", "#a07045"]
    fur_layer("entity/equipment/humanoid/leather", BV_HAT, 70).save(EQUIP / "humanoid/beaver_fur.png")
    fur_layer("entity/equipment/humanoid/leather", OT_FUR, 71,
              collar=lambda x, y: 16 <= y < 20 and 16 <= x < 40).save(EQUIP / "humanoid/otter_fur.png")
    print("river fauna textures ok")


if __name__ == "__main__":
    main()
