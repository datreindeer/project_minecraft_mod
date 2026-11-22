"""Paint the cold fauna (data: scripts/gen_fauna_cold.py; models: client/wildlife/cold/, BrownBearModel, LeopardModel,
ForestBirdModel, LemmingModel).

- Polar Bear (128x64) and its cub: the brown bear's textures (gen_brown_bear_texture.py) turned creamy white by lightness,
  with a black nose, eyes and lips and dark claws; skinned version for the carcass.
- Snow Leopard (128x64): the leopard's texture (gen_fauna_warm_tex_dry.py) turned smoky grey with charcoal rosettes.
- Arctic Fox (64x32), painted from 3D like the other wildlife (wildlife_paint.py): snow white in winter, grey-brown above
  and buff below in summer; skinned versions of both.
- Items (16x16): meat, Polar Bear Liver, raw and cured pelts (the bear's and the leopard's pelts recoloured, the fox's
  drawn as a cased pelt), Polar Bear Cloak, Arctic Fox Hat, Snow Leopard Boots, the two rugs, spawn eggs.
- Equipment layers (hat, boots, the cloak's shoulders) and the cloak's cape (entity/cloak/polar_bear_cloak.png).
- Blocks: Polar Bear Rug and Snow Leopard Rug (the Bear Rug's and Leopard Rug's textures recoloured).
- Siberian Tiger (128x64): painted from 3D on the leopard's box table (LEOPARD in gen_fauna_warm_tex_dry.py): deep
  orange, white belly, cheeks and brows, black stripes running round the body; Tiger Pelt, Tiger Rug, Tiger Pelt Cloak.
- Snowy Owl, Black Grouse (cock and hen), Lammergeier: the eagle-owl's, capercaillie's and golden eagle's textures
  recoloured by lightness; Pika: the lemming's, recoloured. Their feathers, meat, Bone Marrow, Pika Hay and its pile.
The FOX table mirrors the cubes in ArcticFoxModel: keep them in sync.
Run: python scripts/gen_fauna_cold_textures.py   (deterministic; after the brown bear, leopard and coldhunters textures)
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth, _skinner  # noqa: E402
from gen_wildlife_item_textures import OUT as ITEM, egg, recolour as recolour_item  # noqa: E402
from gen_bronze_prep_textures import lum, vanilla  # noqa: E402
from gen_fauna_coldhunters_textures import (COOKED, LONG_PELT, RAW_MEAT, RAW_PALE, SMALL_ROUND, grid, pelt_shape, recolour_layer, rgba,  # noqa: E402
                                             small_meat)
from gen_fauna_warm_tex_dry import LEOPARD  # noqa: E402
from gen_predator_textures import PELT_MASK  # noqa: E402
from gen_wolf_pelt_textures import RUG_MASK  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
BLOCK, EQUIP, ENTITY = TEX / "block", TEX / "entity/equipment", TEX / "entity"
CLEAR = (0, 0, 0, 0)
EYE, NOSE, PAD, TOOTH, TONGUE = (16, 12, 10), (24, 20, 20), (40, 34, 32), (232, 226, 210), (170, 84, 86)


def load(rel):
    return Image.open(TEX / f"{rel}.png").convert("RGBA")


def ramp(stops, t):
    t = max(0.0, min(1.0, t))
    n = len(stops) - 1
    k = min(int(t * n), n - 1)
    return lerp(stops[k], stops[k + 1], t * n - k)


def by_lightness(img, colour):
    """colour(rgba, lum 0..255, x, y) -> rgb or None (keep) for every opaque pixel."""
    out = img.copy()
    px = out.load()
    for x in range(img.width):
        for y in range(img.height):
            c = px[x, y]
            if c[3]:
                r = colour(c, lum(c), x, y)
                if r is not None:
                    px[x, y] = (*r, c[3])
    return out


def reddish(c):
    """Tongue, nose leather, raw flesh: red with blue about as strong as green (brown fur has much less blue)."""
    return c[0] > c[1] * 1.45 and c[0] > 90 and c[2] >= c[1] * 0.8


# =============================================================== POLAR BEAR
PB_STOPS = [(176, 168, 150), (210, 204, 186), (230, 226, 210), (244, 242, 232)]


def polar(c, l, x, y, salt=0):
    if reddish(c):
        return None
    if l < 12:
        return NOSE  # nose, eyes, lips
    if 68 <= x < 78 and 59 <= y < 61 and salt == 0:
        return (58, 52, 48)  # the claw row (BrownBearModel texOffs 68, 59) goes dark
    if l > 215:
        return None  # teeth
    if l > 135:
        return PB_STOPS[-1]  # the brown bear's grizzled tips: just white
    return shade(ramp(PB_STOPS, (l - 16) / (95 - 16)), 1 + (h(x, y, 901 + salt) - 0.5) * 0.05)


# =============================================================== SNOW LEOPARD
SL_STOPS = [(54, 54, 56), (82, 80, 78), (150, 146, 140), (174, 170, 162), (196, 192, 184), (224, 222, 216)]
SL_EYE = (176, 190, 170)  # pale grey-green


def snow_leopard_tone(c, l, x, y):
    if reddish(c):
        return None  # nose and tongue keep their pink
    if max(abs(c[0] - 206), abs(c[1] - 184), abs(c[2] - 70)) < 14:
        return SL_EYE  # the leopard's gold eye
    return ramp(SL_STOPS, (l - 20) / (250 - 20))


# =============================================================== ARCTIC FOX
WHITE, WHITE_SHADE, WHITE_BELLY = (240, 239, 234), (220, 220, 216), (232, 230, 224)
S_BACK, S_SIDE, S_BELLY, S_DARK = (88, 74, 62), (120, 102, 84), (200, 186, 160), (66, 56, 48)


def coat(v, t):
    """t 0 (spine) .. 1 (belly)."""
    if v == "winter":
        return lerp(WHITE, WHITE_SHADE, 0.3 * t) if t < 0.8 else WHITE_BELLY
    return lerp(S_BACK, S_SIDE, t / 0.6) if t < 0.6 else lerp(S_SIDE, S_BELLY, smooth((t - 0.6) / 0.3))


def fox_body(v, face, x, y, z, i, j):
    t = (y + 2.5) / 5
    if face == "top":
        t = abs(x) / 2.5 * 0.2
    elif face == "bottom":
        t = 1.0
    elif face == "front":
        t = min(1.0, t + 0.4)  # the deep fluffy chest
    return fur(coat(v, t), i, j, 1001, 0.06 if v == "winter" else 0.08)


def fox_tail(v, face, x, y, z, i, j):
    c = coat(v, 0.3 if face != "front" else 0.6)
    if v == "summer" and y > 6:
        c = S_DARK  # a darker tip in summer
    return fur(c, i, j, 1011, 0.07)


def fox_head(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1) < 0.1 and abs(y + 1) < 0.1:
            return EYE
        return fur(coat(v, 0.5 if y > 0 else 0.2), i, j, 1021, 0.05)
    if face == "bottom":
        return fur(coat(v, 1.0), i, j, 1022, 0.04)
    if face == "top":
        return fur(coat(v, 0.0), i, j, 1023, 0.06)
    return fur(coat(v, 0.6 if y > 0 else 0.25), i, j, 1024, 0.06)


def fox_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        return NOSE if y < 0.5 else fur(coat(v, 0.8), i, j, 1031, 0.04)
    if face == "top":
        return fur(coat(v, 0.3), i, j, 1032, 0.05)
    if face == "bottom":
        return shade(TONGUE, 0.8)
    return fur(coat(v, 0.7), i, j, 1033, 0.04)


def fox_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -1.5 else TONGUE
    return fur(coat(v, 0.9), i, j, 1041, 0.04)


def fox_ear(v, face, x, y, z, i, j):
    if y < -1.5 and abs(x) > 0.5:
        return CLEAR  # rounded tips
    if face == "front":
        return fur(lerp(coat(v, 0.9), (200, 170, 160), 0.3), i, j, 1051, 0.04)
    return fur(coat(v, 0.1) if v == "winter" else S_DARK, i, j, 1052, 0.05)


def fox_leg(v, face, x, y, z, i, j):
    return fur(coat(v, 0.5 if face != "left" else 0.85), i, j, 1061, 0.06)


def fox_paw(v, face, x, y, z, i, j):
    if face == "bottom":
        return PAD if v == "summer" else fur(WHITE_SHADE, i, j, 1071, 0.05)  # furred soles in winter
    return fur(coat(v, 0.7), i, j, 1072, 0.06)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors ArcticFoxModel.createBodyLayer
FOX = {
    "body": (0, 0, -2.5, -2.5, -5, 5, 5, 10, fox_body),
    "tail": (30, 0, -1.5, 0, -1.5, 3, 8, 3, fox_tail),
    "ear": (42, 0, -1, -2, -0.5, 2, 2, 1, fox_ear),
    "head": (0, 15, -2.5, -2.5, -3.5, 5, 4, 4, fox_head),
    "muzzle": (18, 15, -1, -0.5, -5.5, 2, 2, 2, fox_muzzle),
    "jaw": (26, 15, -1, 0, -2, 2, 1, 2, fox_jaw),
    "fore_upper": (0, 23, -1, -1, -1, 2, 3, 2, fox_leg),
    "hind_upper": (8, 23, -1, -1, -1.5, 2, 3, 3, fox_leg),
    "lower": (18, 23, -1, 0, -1, 2, 2, 2, fox_leg),
    "paw": (26, 23, -1, 2, -1.5, 2, 1, 2, fox_paw),
}


# =============================================================== items
LIVER = [
    "................",
    "................",
    "................",
    "....oooo........",
    "...oLLLLoo......",
    "..oLHLLLLLoo....",
    "..oLLLLLLLLLoo..",
    "..oLLLLDLLLLLLo.",
    "...oLLLDDLLLLLo.",
    "....oLLLDDLLLo..",
    ".....ooLLLLLo...",
    ".......ooLLo....",
    ".........oo.....",
    "................",
    "................",
    "................",
]
LIVER_PAL = {"o": (60, 18, 22), "L": (118, 36, 44), "H": (168, 74, 80), "D": (88, 26, 32)}

HAT = [
    "................",
    "................",
    "................",
    "......oooo......",
    "....ooWWWWoo....",
    "...oWWwWWWWWo...",
    "..oWWWWWWwWWWo..",
    "..oWwWWWWWWWWo..",
    "..oWWWWWwWWWWo..",
    ".oWWWWWWWWWWWWo.",
    ".oWWwWWWWWWwWWo.",
    "..oooooooooooo..",
    "................",
    "................",
    "................",
    "................",
]

BOOTS = [
    "................",
    "................",
    "...oooo..oooo...",
    "...oGGo..oGGo...",
    "...oGSo..oSGo...",
    "...oGGo..oGGo...",
    "...oSGo..oGSo...",
    "...oGGo..oGGo...",
    "...oGGoo.oGGoo..",
    "..oGSGGo.oGGSGo.",
    "..oGGGGo.oGGGGo.",
    "..oLLLLo.oLLLLo.",
    "...oooo...oooo..",
    "................",
    "................",
    "................",
]

CLOAK = [
    "................",
    "................",
    "....oooooooo....",
    "...oWWWWWWWWo...",
    "..oWWWWWWWWWWo..",
    "..oWWWLLLLWWWo..",
    ".oWWWWLooLWWWWo.",
    ".oWWWWLo.oLWWWWo",
    ".oWWWWo...oWWWWo",
    "oWWWWWo...oWWWWo",
    "oWWWWWo...oWWWWo",
    "oWWWWWo...oWWWWo",
    "oWWWWo.....oWWWo",
    "ooooo.......oooo",
    "................",
    "................",
]


def fox_pelt_col(x, y):
    if y >= 13:
        return fur(WHITE_SHADE, x, y, 1101, 0.04)  # the bushy tail
    return fur(lerp(WHITE, WHITE_SHADE, 0.4 * h(x, y, 1102)), x, y, 1103, 0.03)


def items():
    out = {}
    out["raw_polar_bear"] = recolour_item(ITEM / "beef_scraps.png", RAW_MEAT)
    out["cooked_polar_bear"] = recolour_item(ITEM / "beef_scraps.png", COOKED)
    out["raw_snow_leopard"] = by_lightness(load("item/leopard_meat"), lambda c, l, x, y: shade(c[:3], 1.08))
    out["cooked_snow_leopard"] = load("item/cooked_leopard_meat")
    out["raw_arctic_fox"] = recolour_item(ITEM / "rabbit_scraps.png", RAW_MEAT)
    out["cooked_arctic_fox"] = recolour_item(ITEM / "cooked_rabbit_scraps.png", COOKED)
    out["polar_bear_liver"] = grid(LIVER, lambda ch, x, y: LIVER_PAL[ch])
    out["raw_polar_bear_pelt"] = by_lightness(load("item/raw_bear_pelt"), lambda c, l, x, y: None if reddish(c) or l > 150 else polar(c, l + 20, x, y, 1))
    out["polar_bear_pelt"] = by_lightness(load("item/bear_pelt"), lambda c, l, x, y: polar(c, l + 20, x, y, 2))
    out["raw_snow_leopard_pelt"] = by_lightness(load("item/raw_leopard_pelt"), lambda c, l, x, y: None if reddish(c) or l > 200 and c[0] - c[2] < 40
                                                else snow_leopard_tone(c, l, x, y))
    out["snow_leopard_pelt"] = by_lightness(load("item/leopard_pelt"), snow_leopard_tone)
    out["raw_arctic_fox_pelt"] = pelt_shape(LONG_PELT, fox_pelt_col, True, 1111)
    out["arctic_fox_fur"] = pelt_shape(LONG_PELT, fox_pelt_col, False, 1112)
    out["arctic_fox_hat"] = grid(HAT, lambda ch, x, y: (70, 66, 60) if ch == "o" else fur(WHITE_SHADE if ch == "w" else WHITE, x, y, 1121, 0.03))
    out["snow_leopard_boots"] = grid(BOOTS, lambda ch, x, y: {"o": (52, 50, 48), "S": (60, 58, 58), "L": (120, 92, 62)}.get(ch)
                                     or fur((180, 176, 168), x, y, 1131, 0.05))
    out["polar_bear_cloak"] = grid(CLOAK, lambda ch, x, y: {"o": (96, 92, 84), "L": (196, 162, 118)}.get(ch)
                                   or fur(lerp(WHITE, (226, 222, 204), h(x, y, 1141)), x, y, 1142, 0.03))
    out["polar_bear_rug"] = by_lightness(load("item/bear_rug"), lambda c, l, x, y: polar(c, l + 10, x, y, 3))
    out["snow_leopard_rug"] = by_lightness(load("item/leopard_rug"), snow_leopard_tone)
    K = "#141010"
    out["polar_bear_spawn_egg"] = egg(["#fbfaf2", "#efece0", "#e0dccc", "#ccc6b2", "#9a9484", "#6a665a"], [], [],
                                      [(7, 9, K), (8, 9, K), (6, 6, K), (9, 6, K)])
    out["arctic_fox_spawn_egg"] = egg(["#ffffff", "#f2f2ee", "#e4e4de", "#d0d0ca", "#9c9c96", "#6c6c66"], [], [(4, 1), (11, 1)],
                                      [(6, 5, K), (9, 5, K), (7, 8, K), (8, 8, K), (4, 1, "#d8d4cc"), (11, 1, "#d8d4cc")])
    out["snow_leopard_spawn_egg"] = egg(["#e8e6e0", "#d4d0c8", "#bcb8b0", "#a29e96", "#6c6a66", "#44423f"],
                                        [(4, 6, "#3c3a38"), (11, 7, "#3c3a38"), (5, 11, "#3c3a38"), (10, 12, "#3c3a38"), (8, 9, "#3c3a38"),
                                         (6, 13, "#3c3a38"), (9, 4, "#3c3a38")], [], [(6, 5, "#b0beaa"), (9, 5, "#b0beaa")])
    return out


# =============================================================== worn pieces
CAPE_FACES = {"top": (1, 0, 10, 1), "bottom": (11, 0, 10, 1), "right": (0, 1, 1, 16), "front": (1, 1, 10, 16),
              "left": (11, 1, 1, 16), "back": (12, 1, 10, 16)}  # vanilla player-cape UV (box 10x16x1 at 0,0)


def cape():
    """The bear hide hung from the shoulders: thick white fur outside with a few darker guard hairs, tanned hide inside."""
    rng = random.Random(1201)
    img = Image.new("RGBA", (64, 32), CLEAR)
    for face, (x0, y0, w, hh) in CAPE_FACES.items():
        for y in range(y0, y0 + hh):
            for x in range(x0, x0 + w):
                if face == "back":
                    c = lerp((196, 162, 118), (176, 140, 98), rng.random() * 0.6)
                else:
                    c = lerp(WHITE, (222, 216, 196), rng.random() * 0.7)
                    if face != "top" and y >= y0 + hh - 2:
                        c = lerp(c, (204, 196, 172), 0.6)  # the ragged hem
                img.putpixel((x, y), rgba(c))
    for face in ("right", "front", "left"):  # vertical hair strands
        x0, y0, w, hh = CAPE_FACES[face]
        for _ in range(w * hh // 5):
            x, y = x0 + rng.randrange(w), y0 + rng.randrange(hh)
            for k in range(rng.randint(2, 3)):
                if y + k < y0 + hh:
                    img.putpixel((x, y + k), rgba(rng.choice([(206, 202, 186), (250, 250, 244)])))
    return img


def layers():
    return {
        "arctic_fox_hat": recolour_layer("entity/equipment/humanoid/leather",
                                         lambda x, y, s: fur(lerp(WHITE_SHADE, WHITE, s), x, y, 1301, 0.03), lambda x, y: y < 16),
        "snow_leopard_boots": recolour_layer("entity/equipment/humanoid/leather",
                                             lambda x, y, s: (60, 58, 58) if h(x // 2, y // 2, 1311) < 0.2 else fur(ramp(SL_STOPS, 0.55 + 0.3 * s), x, y, 1312, 0.05),
                                             lambda x, y: y >= 16 and x < 16),
        "polar_bear_cloak": recolour_layer("entity/equipment/humanoid/leather",
                                           lambda x, y, s: fur(lerp((222, 216, 196), WHITE, s), x, y, 1321, 0.03), lambda x, y: y >= 16 and x >= 16),
    }


# =============================================================== SIBERIAN TIGER
T_BACK, T_ORANGE, T_PALE, T_WHITE, T_STRIPE = (196, 98, 30), (222, 128, 46), (236, 186, 120), (240, 236, 224), (26, 20, 18)
T_EYE, T_NOSE = (210, 180, 70), (170, 104, 96)


def t_coat(t):
    return lerp(T_BACK, T_ORANGE, t / 0.45) if t < 0.45 else lerp(T_ORANGE, T_PALE, (t - 0.45) / 0.25) if t < 0.7 else lerp(T_PALE, T_WHITE, smooth((t - 0.7) / 0.2))


def stripe(along, round_, salt, period=3.4, width=0.95):
    """Dark bands across the body: along = position down the animal, round_ = how far round it (bends the band)."""
    wob = 0.9 * __import__("math").sin(round_ * 0.7 + h(int(along // period), 0, salt) * 6) + h(int(along // period), 1, salt) * 0.8
    return (along + wob) % period < width and h(int((along + wob) // period), int(round_ // 3), salt + 1) < 0.85


def t_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return fur(T_WHITE, i, j, 1501, 0.04)
        t = abs(x) / 4 * 0.2 if face == "top" else (y - top) / (bottom - top)
        if t < 0.75 and stripe(z, (y if face != "top" else 0) + abs(x), 1502):
            return fur(T_STRIPE, i, j, 1503, 0.08)
        return fur(t_coat(t), i, j, 1504, 0.06)
    return mat


def t_tail(start):
    def mat(v, face, x, y, z, i, j):
        d = start + y
        if d > 17.5:
            return fur(T_STRIPE, i, j, 1511, 0.08)
        if (d % 2.6) < 0.9:
            return fur(T_STRIPE, i, j, 1512, 0.08)
        return fur(T_PALE if face == "front" else T_ORANGE, i, j, 1513, 0.06)
    return mat


def t_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 2) < 0.1:
            return T_EYE
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 3) < 0.1 or y > 0.5 or abs(abs(x) - 2.5) < 0.1 and y > -1.5:
            return fur(T_WHITE, i, j, 1521, 0.03)  # white brows, cheeks and muzzle sides
        if y < -2.5 and abs(x) < 1.2 and (i % 2 == 0):
            return T_STRIPE  # the forehead marks
        return fur(T_ORANGE, i, j, 1522, 0.05)
    if face == "bottom":
        return fur(T_WHITE, i, j, 1523, 0.03)
    if face in ("left", "right"):
        if y > 0:
            return fur(T_WHITE, i, j, 1524, 0.03)
        return fur(T_STRIPE, i, j, 1525, 0.08) if stripe(z * 1.6, y, 1526, 2.2, 0.7) else fur(T_ORANGE, i, j, 1527, 0.05)
    return fur(T_STRIPE, i, j, 1528, 0.08) if stripe(z * 1.6 + x, 0, 1529, 2.4, 0.7) else fur(T_BACK, i, j, 1530, 0.05)


def t_muzzle(v, face, x, y, z, i, j):
    if face == "front" and y < -0.5:
        return T_NOSE if abs(x) < 1 else fur(T_WHITE, i, j, 1541, 0.03)
    if face == "bottom":
        return shade(TONGUE, 0.8)
    if face == "top":
        return T_NOSE if z < -7 and abs(x) < 1 else fur(T_ORANGE, i, j, 1542, 0.05)
    return fur(T_WHITE, i, j, 1543, 0.03)


def t_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -2 or (abs(x) > 1 and z < -1) else TONGUE
    return fur(T_WHITE, i, j, 1551, 0.03)


def t_ear(v, face, x, y, z, i, j):
    if y < -1 and x < 0 and face in ("front", "back", "top", "right"):
        return CLEAR
    if face == "front":
        return fur(T_WHITE, i, j, 1561, 0.05)
    if face == "back" and y > -1 and x < 0.5:
        return fur(T_WHITE, i, j, 1562, 0.03)  # the white spot on the black ear back
    return fur(T_STRIPE, i, j, 1563, 0.1)


def t_leg(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "left":
            return fur(T_WHITE, i, j, 1571, 0.03)
        if face != "back" and stripe(y * 1.3, x + z, 1572, 2.6, 0.8) and t < 0.8:
            return fur(T_STRIPE, i, j, 1573, 0.08)
        return fur(lerp(T_ORANGE, T_PALE, t * 0.5), i, j, 1574, 0.05)
    return mat


def t_lower(v, face, x, y, z, i, j):
    if y >= 5:
        return PAD if face == "bottom" else fur(T_PALE, i, j, 1581, 0.04)
    return fur(T_WHITE if face == "left" else T_PALE, i, j, 1582, 0.05)


TIGER_MATS = {"chest": t_body(-4, 4), "loin": t_body(-3.5, 3.5), "shoulders": t_body(-3, 3), "neck": t_body(-3.5, 2.5),
              "tail0": t_tail(0), "tail1": t_tail(8), "tail2": t_tail(15), "skull": t_skull, "muzzle": t_muzzle, "jaw": t_jaw,
              "ear": t_ear, "fore_upper": t_leg(-2, 4), "lower": t_lower, "paw": t_lower, "thigh": t_leg(-2, 5)}
TIGER = {name: (*box[:8], TIGER_MATS[name]) for name, box in LEOPARD.items()}


def tiger_pelt_col(x, y):
    d = abs(x - 7.5)
    if y >= 14:
        return T_STRIPE if y == 15 or (y % 2 == 0) else T_ORANGE
    if d > 4.5:
        return fur(T_WHITE, x, y, 1591, 0.03)
    if stripe(y * 1.0, d, 1592, 2.6, 0.9) and d > 0.6:
        return T_STRIPE
    return fur(t_coat(d / 6), x, y, 1593, 0.06)


def tiger_rug():
    def col(ch, x, y):
        if ch == "N":
            return T_NOSE
        if ch == "E":
            return T_EYE
        if ch in "sS":
            return T_WHITE
        if ch == "e":
            return T_STRIPE
        if ch == "w":
            return (214, 204, 178)
        if ch == "o":
            return shade(T_BACK, 0.7)
        if ch in "tT":
            return T_STRIPE if ch == "T" else T_ORANGE
        return tiger_pelt_col(x, min(13, max(2, y)))
    return grid(RUG_MASK, col)


# =============================================================== birds and pika (recoloured)
def snowy_owl_tone(c, l, x, y):
    if c[0] > 220 and c[2] < 60:
        return (226, 196, 52)  # the eagle-owl's orange eye: lemon yellow
    if l < 70:
        return (52, 50, 54) if h(x, y, 1601) < 0.6 else (236, 236, 232)  # sparse dark bars
    return shade(lerp((214, 214, 210), (250, 250, 248), (l - 70) / 150), 1 + (h(x, y, 1602) - 0.5) * 0.04)


def grouse_tone(c, l, x, y):
    if c[0] > 150 and c[1] < 90 and c[2] < 90:
        return None  # the red brow
    if l > 200:
        return (240, 240, 236)  # white wing bar and undertail
    return ramp([(14, 14, 22), (28, 30, 44), (44, 48, 70), (70, 76, 98)], l / 200)  # glossy blue-black


def grouse_hen_tone(c, l, x, y):
    return shade(lerp(c[:3], (l, l * 0.9, l * 0.78), 0.35), 1.0)


def lammergeier_tone(c, l, x, y):
    if c[0] > 150 and c[1] > 100 and c[2] < 70 and l < 190:
        return (214, 120, 40)  # the eye ring
    return ramp([(36, 36, 40), (64, 64, 70), (98, 96, 100), (206, 138, 82), (234, 176, 118)], l / 220)


def pika_tone(c, l, x, y):
    if l < 18:
        return None  # eyes
    return shade(ramp([(98, 86, 74), (126, 112, 96), (154, 140, 122), (196, 186, 166)], l / 255), 1 + (h(x, y, 1611) - 0.5) * 0.06)


HAY = [(170, 160, 88), (190, 178, 100), (206, 192, 116), (150, 148, 76), (128, 136, 70)]


def hay_texture():
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = int(h(x, y // 3 + (x % 3), 1621) * len(HAY))  # short vertical strands
            im.putpixel((x, y), rgba(HAY[k]))
    return im


MARROW = [
    "................",
    "................",
    "..........ooo...",
    ".........oWWWo..",
    "........oWWYWo..",
    ".......oWYYYo...",
    "......oWYRYo....",
    ".....oWYYYo.....",
    "....oWYRYo......",
    "...oWYYYo.......",
    "..oWWYWo........",
    "..oWWWo.........",
    "...ooo..........",
    "................",
    "................",
    "................",
]
TUFT = [
    "................",
    "................",
    "....g....g......",
    ".....g..g...g...",
    "..g..G..G..g....",
    "...g.GgGG.g.....",
    "....GGgGGgG.....",
    "...gGGGgGGGg....",
    "..gGGgGGGgGGg...",
    "..GGGGGgGGGGG...",
    "..oGGGGGGGGGo...",
    "...ooooooooo....",
    "................",
    "................",
    "................",
    "................",
]


def items2():
    out = {}
    out["raw_tiger"] = recolour_item(ITEM / "beef_scraps.png", RAW_MEAT)
    out["cooked_tiger"] = recolour_item(ITEM / "cooked_beef_scraps.png", COOKED)
    out["raw_pika"] = small_meat(SMALL_ROUND, RAW_PALE, 1631)
    out["cooked_pika"] = small_meat(SMALL_ROUND, COOKED, 1632)
    out["raw_black_grouse"] = by_lightness(load("item/raw_capercaillie"), lambda c, l, x, y: shade(c[:3], 0.95))
    out["cooked_black_grouse"] = by_lightness(load("item/cooked_capercaillie"), lambda c, l, x, y: shade(c[:3], 1.04))
    out["plucked_black_grouse"] = by_lightness(load("item/plucked_capercaillie"), lambda c, l, x, y: shade(c[:3], 0.97))
    out["bone_marrow"] = grid(MARROW, lambda ch, x, y: {"o": (110, 98, 80), "W": (236, 228, 206), "Y": (226, 186, 96), "R": (190, 96, 70)}[ch])
    out["snowy_owl_feather"] = by_lightness(load("item/owl_feather"), snowy_owl_tone)
    out["black_grouse_tail_feather"] = by_lightness(load("item/capercaillie_tail_feather"), grouse_tone)
    out["lammergeier_feather"] = by_lightness(load("item/eagle_feather"), lammergeier_tone)
    out["pika_hay"] = grid(TUFT, lambda ch, x, y: {"o": (110, 104, 60), "g": HAY[3], "G": HAY[int(h(x, y, 1641) * 3)]}[ch])
    out["raw_tiger_pelt"] = pelt_shape(PELT_MASK, tiger_pelt_col, True, 1651)
    out["tiger_pelt"] = pelt_shape(PELT_MASK, tiger_pelt_col, False, 1652)
    out["tiger_pelt_cloak"] = grid(CLOAK, lambda ch, x, y: {"o": (70, 40, 22), "L": (196, 162, 118)}.get(ch)
                                   or (T_STRIPE if (x + y // 2) % 4 == 0 else fur(T_ORANGE, x, y, 1661, 0.05)))
    out["tiger_rug"] = tiger_rug()
    K = "#141010"
    out["siberian_tiger_spawn_egg"] = egg(["#f0a050", "#de8a34", "#c47024", "#9a5418", "#5e3410", "#3a200a"],
                                          [(x, y, K) for x, y in ((4, 5), (4, 6), (11, 6), (11, 7), (5, 10), (5, 11), (10, 11), (10, 12), (7, 13), (8, 13))],
                                          [], [(6, 5, "#f0e8d8"), (9, 5, "#f0e8d8"), (7, 8, "#f0e8d8"), (8, 8, "#f0e8d8")])
    out["snowy_owl_spawn_egg"] = egg(["#ffffff", "#f4f4f2", "#e6e6e2", "#d2d2cc", "#a0a09a", "#6c6c66"],
                                     [(5, 9, "#3a3a3e"), (9, 10, "#3a3a3e"), (7, 12, "#3a3a3e"), (10, 7, "#3a3a3e")], [],
                                     [(6, 5, "#e2c434"), (9, 5, "#e2c434")])
    out["black_grouse_spawn_egg"] = egg(["#4a4e66", "#363a50", "#26283a", "#1a1c28", "#101018", "#08080c"],
                                        [(x, 10, "#f0f0ec") for x in range(4, 12)], [], [(6, 4, "#c02a24"), (9, 4, "#c02a24")])
    out["lammergeier_spawn_egg"] = egg(["#eab276", "#d8985c", "#c07c44", "#6a6a70", "#46464c", "#2a2a2e"],
                                       [(x, y, "#4a4a50") for x in range(3, 13) for y in (10, 11)], [], [(6, 5, K), (9, 5, K), (7, 7, K)])
    out["pika_spawn_egg"] = egg(["#c4b8a0", "#a8987e", "#8c7c66", "#6e604e", "#4a4034", "#2e2820"], [], [(4, 2), (11, 2)],
                                [(6, 6, K), (9, 6, K), (4, 2, "#8c7c66"), (11, 2, "#8c7c66")])
    return out


def build():
    bear = by_lightness(load("entity/wildlife/brown_bear"), polar)
    save(bear, "wildlife/polar_bear.png")
    save(_skinner().skin(bear.copy()), "skinned/wildlife/polar_bear.png")
    save(by_lightness(load("entity/wildlife/brown_bear_cub"), lambda c, l, x, y: polar(c, l, x, y, 4)), "wildlife/polar_bear_cub.png")
    leopard = by_lightness(load("entity/wildlife/leopard"), snow_leopard_tone)
    save(leopard, "wildlife/snow_leopard.png")
    save(_skinner().skin(leopard.copy()), "skinned/wildlife/snow_leopard.png")
    for v, name in (("winter", "arctic_fox"), ("summer", "arctic_fox_summer")):
        img = paint(FOX, v, (64, 32))
        save(img, f"wildlife/{name}.png")
        save(skinned(img, FOX), f"skinned/wildlife/{name}.png")
    for name, im in items().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
        print("wrote item", name)
    for rug in ("polar_bear_rug", "snow_leopard_rug"):
        src = load("block/bear_rug" if rug == "polar_bear_rug" else "block/leopard_rug")
        by_lightness(src, (lambda c, l, x, y: polar(c, l + 10, x, y, 5)) if rug == "polar_bear_rug" else snow_leopard_tone).save(BLOCK / f"{rug}.png")
    for name, im in layers().items():
        assert im.getbbox(), name
        im.save(EQUIP / f"humanoid/{name}.png")
        print("wrote equipment", name)
    cape().save(ENTITY / "cloak/polar_bear_cloak.png")
    # ---- the rest of the cold: tiger, birds, pika ----
    tiger = paint(TIGER, "tiger", (128, 64))
    save(tiger, "wildlife/siberian_tiger.png")
    save(skinned(tiger, TIGER, keep=("paw",)), "skinned/wildlife/siberian_tiger.png")
    save(by_lightness(load("entity/wildlife/owl"), snowy_owl_tone), "wildlife/snowy_owl.png")
    save(by_lightness(load("entity/wildlife/capercaillie"), grouse_tone), "wildlife/black_grouse.png")
    save(by_lightness(load("entity/wildlife/capercaillie_hen"), grouse_hen_tone), "wildlife/black_grouse_hen.png")
    save(by_lightness(load("entity/wildlife/golden_eagle"), lammergeier_tone), "wildlife/lammergeier.png")
    pika = by_lightness(load("entity/wildlife/lemming"), pika_tone)
    save(pika, "wildlife/pika.png")
    save(_skinner().skin(pika.copy()), "skinned/wildlife/pika.png")
    for src in ("capercaillie", "capercaillie_hen"):  # the grouse's carcass, if its kind has a skinned texture
        f = ENTITY / f"skinned/wildlife/{src}.png"
        if f.exists():
            save(Image.open(f).convert("RGBA"), f"skinned/wildlife/{src.replace('capercaillie', 'black_grouse')}.png")
    for name, im in items2().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
        print("wrote item", name)
    tiger_rug().save(BLOCK / "tiger_rug.png")
    hay_texture().save(BLOCK / "pika_hay_pile.png")
    recolour_layer("entity/equipment/humanoid/leather", lambda x, y, s: T_STRIPE if (x + y) % 5 == 0 else fur(lerp(T_BACK, T_ORANGE, s), x, y, 1671, 0.05),
                   lambda x, y: y >= 16 and x >= 16).save(EQUIP / "humanoid/tiger_pelt_cloak.png")
    tcape = cape()
    px = tcape.load()
    for x in range(tcape.width):
        for y in range(tcape.height):
            c = px[x, y]
            if c[3] and not (x >= 12 and y >= 1 and x < 22):  # the fur faces (the lining stays tanned hide)
                px[x, y] = rgba(T_STRIPE if (y + h(x // 3, 0, 1681) * 4) % 4.2 < 1.1 else shade(T_ORANGE, 0.9 + 0.2 * h(x, y, 1682)))
    tcape.save(ENTITY / "cloak/tiger_pelt_cloak.png")


if __name__ == "__main__":
    build()
