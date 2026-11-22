"""Paint the cold fauna (data: scripts/gen_fauna_cold.py; models: client/wildlife/cold/, BrownBearModel, LeopardModel).

- Polar Bear (128x64) and its cub: the brown bear's textures (gen_brown_bear_texture.py) turned creamy white by lightness,
  with a black nose, eyes and lips and dark claws; skinned version for the carcass.
- Snow Leopard (128x64): the leopard's texture (gen_fauna_warm_tex_dry.py) turned smoky grey with charcoal rosettes.
- Arctic Fox (64x32), painted from 3D like the other wildlife (wildlife_paint.py): snow white in winter, grey-brown above
  and buff below in summer; skinned versions of both.
- Items (16x16): meat, Polar Bear Liver, raw and cured pelts (the bear's and the leopard's pelts recoloured, the fox's
  drawn as a cased pelt), Polar Bear Cloak, Arctic Fox Hat, Snow Leopard Boots, the two rugs, spawn eggs.
- Equipment layers (hat, boots, the cloak's shoulders) and the cloak's cape (entity/cloak/polar_bear_cloak.png).
- Blocks: Polar Bear Rug and Snow Leopard Rug (the Bear Rug's and Leopard Rug's textures recoloured).
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
from gen_fauna_coldhunters_textures import (COOKED, LONG_PELT, RAW_MEAT, grid, pelt_shape, recolour_layer, rgba)  # noqa: E402

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
SL_STOPS = [(44, 44, 46), (66, 64, 64), (138, 136, 132), (184, 180, 172), (212, 208, 200), (236, 234, 228)]
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
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 1) < 0.1:
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
        return NOSE if z < -5 else fur(coat(v, 0.3), i, j, 1032, 0.05)
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


if __name__ == "__main__":
    build()
