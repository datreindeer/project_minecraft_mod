"""Paint the cold fauna's items and blocks (data: scripts/gen_fauna_cold.py). The animals' models and skins and the
Pika Hay Pile come from scripts/gen_cold_models.py, which this runs at the end.

- Items (16x16): meat, Polar Bear Liver, raw and cured pelts (the bear's and the leopard's pelts recoloured, the fox's
  drawn as a cased pelt), Polar Bear Cloak, Arctic Fox Hat, Snow Leopard Boots, the two rugs, spawn eggs.
- Equipment layers (hat, boots, the cloak's shoulders) and the cloak's cape (entity/cloak/polar_bear_cloak.png).
- Blocks: Polar Bear Rug and Snow Leopard Rug (the Bear Rug's and Leopard Rug's textures recoloured).
- The Siberian tiger's meat, pelt, rug and mantle are the jungle tiger's (gen_fauna_jungle*.py).
- The birds' feathers and meat, Bone Marrow, Pika Hay.
Run: python scripts/gen_fauna_cold_textures.py   (deterministic; after the brown bear, leopard and coldhunters textures)
"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, shade  # noqa: E402
from gen_wildlife_item_textures import OUT as ITEM, egg, recolour as recolour_item  # noqa: E402
from gen_bronze_prep_textures import lum, vanilla  # noqa: E402
from gen_fauna_coldhunters_textures import (COOKED, LONG_PELT, RAW_MEAT, RAW_PALE, SMALL_ROUND, grid, pelt_shape, recolour_layer, rgba,  # noqa: E402
                                             small_meat)
import gen_cold_models  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
BLOCK, EQUIP, ENTITY = TEX / "block", TEX / "entity/equipment", TEX / "entity"
CLEAR = (0, 0, 0, 0)
NOSE = (24, 20, 20)


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


# =============================================================== arctic fox (its pelt and hat)
WHITE, WHITE_SHADE = (240, 239, 234), (220, 220, 216)


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


# =============================================================== feathers (recoloured)
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


def lammergeier_tone(c, l, x, y):
    if c[0] > 150 and c[1] > 100 and c[2] < 70 and l < 190:
        return (214, 120, 40)  # the eye ring
    return ramp([(36, 36, 40), (64, 64, 70), (98, 96, 100), (206, 138, 82), (234, 176, 118)], l / 220)


# =============================================================== pika hay (the item)
HAY = [(170, 160, 88), (190, 178, 100), (206, 192, 116), (150, 148, 76), (128, 136, 70)]

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
    for name, im in items2().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
        print("wrote item", name)
    gen_cold_models.main()  # the animals' models and skins, the hay pile


if __name__ == "__main__":
    build()
