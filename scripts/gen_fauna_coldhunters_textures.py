"""Paint the cold hunters (data: scripts/gen_fauna_coldhunters.py; models: client/wildlife/coldhunters/*Model.java).

- Entities: the five animals' models and skins (with the skinned carcass skins) come from one table each in
  gen_hunt_models.py, which build() runs; the colours below are for the items and layers.
- Items (16x16): raw and cooked meat of each, raw pelts (flesh side up, fur at the edges) and cured furs, Lemming Pelt,
  Wolverine Claw, Claw Flesher, Wolverine-Trimmed Hood, Ermine Mantle, Marten Fur Cap, Lynx Rug, the five spawn eggs.
- Equipment layers: the hood (the Fur Hood's layer with a frosty wolverine ruff round the face), the mantle and the cap.
- Block: the Lynx Rug (head north, like the Wolf Rug).
Run: python scripts/gen_fauna_coldhunters_textures.py   (deterministic)
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import OUT as ITEM, egg, recolour  # noqa: E402
from gen_predator_textures import PELT_MASK  # noqa: E402
from gen_wolf_pelt_textures import RUG_MASK  # noqa: E402
from gen_bronze_prep_textures import lum, vanilla  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
BLOCK, EQUIP = TEX / "block", TEX / "entity/equipment"
CLEAR = (0, 0, 0, 0)
EYE, NOSE_BLACK, PINK_NOSE = (16, 12, 10), (26, 20, 20), (186, 120, 118)
CLAW, PAD, TOOTH, TONGUE = (214, 204, 178), (40, 32, 30), (232, 226, 210), (170, 84, 86)
OUTLINE = (28, 22, 18)
HIDE_SIDE = (222, 196, 168)  # the flesh side of a fresh pelt
HIDE_DRY = (196, 162, 118)


def rgba(c):
    return (*c[:3], 255)


def spot(x, y, z, salt, size=2.0, density=0.3):
    """A spot pattern in 3D (so it runs on across faces): staggered cells, some filled with a rounded blot."""
    cy = math.floor(y / size)
    sx = x / size + (0.5 if cy % 2 else 0.0)
    cx, cz = math.floor(sx), math.floor(z / size)
    if h(cx * 7 + cz * 131, cy, salt) >= density:
        return False
    fx, fy, fz = sx - cx - 0.5, y / size - cy - 0.5, z / size - cz - 0.5
    return fx * fx + fy * fy + fz * fz < 0.32 + 0.1 * h(cx, cy + cz, salt + 1)


# =============================================================== LYNX
L_BACK, L_SIDE, L_PALE, L_BELLY = (150, 116, 84), (186, 148, 106), (212, 190, 154), (234, 226, 210)
L_SPOT, L_BLACK = (66, 46, 32), (30, 26, 24)
EYE_LYNX = (190, 160, 70)


# =============================================================== WOLVERINE
V_DARK, V_BROWN, V_BAND, V_BROW = (40, 28, 21), (70, 48, 33), (176, 138, 92), (148, 130, 108)
V_TIP = (92, 66, 44)


# =============================================================== ERMINE
E_BROWN, E_DARKB, E_CREAM, E_WHITE, E_SNOW = (134, 88, 54), (104, 66, 40), (238, 226, 194), (246, 244, 238), (236, 234, 222)


# =============================================================== LEMMING
M_BLACK, M_TAWNY, M_SADDLE, M_BUFF, M_PALE = (32, 26, 22), (176, 120, 62), (96, 64, 38), (210, 176, 116), (226, 206, 160)


# =============================================================== PINE MARTEN
P_BROWN, P_DARK, P_FACE, P_BIB, P_CREAM = (96, 60, 36), (56, 36, 24), (118, 82, 54), (230, 190, 112), (226, 208, 170)


def marten_coat(c, i, j, salt):
    if h(i, j, salt + 9) < 0.15:
        c = lerp(c, (150, 110, 70), 0.4)  # glossy, paler-tipped guard hairs
    return fur(c, i, j, salt, 0.07)


# =============================================================== items
def grid(rows, colour):
    """rows of 16 chars; colour(ch, x, y) -> rgb or None for clear."""
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = colour(ch, x, y)
                if c is not None:
                    im.putpixel((x, y), rgba(c))
    return im


RAW_MEAT = ["#3a0a0b", "#561112", "#701a19", "#862420", "#9a3027", "#ac3f31", "#bf5a46", "#d6826c"]
RAW_PALE = ["#4a1a18", "#6a2826", "#843632", "#9c463e", "#b45a4e", "#c66e60", "#d68a7a", "#e8ae9e"]  # cat meat is pale
COOKED = ["#1c0f09", "#2c190f", "#3d2416", "#4c2e1c", "#5a3722", "#684129", "#774c31", "#a07650"]

# whole small skinned bodies: s flesh, d dark flesh, f fat/sinew, b bone (the legs' ends), o outline
SMALL_LONG = [  # ermine and marten: long and slim, laid out diagonally
    "................",
    "................",
    "............oo..",
    "...........osso.",
    "..........ossdso",
    "........ooossso.",
    ".......osssfsdo.",
    "......ossdssso..",
    ".....ossssfso...",
    "....osdsssso....",
    "...ossssdso.....",
    "..obsssfso......",
    ".obo.osso.......",
    "..o..obo........",
    ".....o..........",
    "................",
]
SMALL_ROUND = [  # lemming: a round little body, head and stubby legs
    "................",
    "................",
    "................",
    "................",
    ".......oooo.....",
    ".....oossssoo...",
    "....ossdssssso..",
    "...osssssfsssso.",
    "...ossfsssdssso.",
    "...osssssssssso.",
    "....osssdsssoo..",
    ".....oobooboo...",
    "......o..o......",
    "................",
    "................",
    "................",
]


def small_meat(rows, ramp, salt):
    def col(ch, x, y):
        if ch == "o":
            return tuple(int(ramp[0][i:i + 2], 16) for i in (1, 3, 5))
        if ch == "b":
            return (226, 214, 190)
        if ch == "f":
            return (234, 210, 180) if ramp is not COOKED else (180, 132, 84)
        k = 3 if ch == "d" else 5 + (h(x, y, salt) < 0.35) - (h(x, y, salt + 1) < 0.2)
        return tuple(int(ramp[k][i:i + 2], 16) for i in (1, 3, 5))
    return grid(rows, col)


def pelt_shape(mask, fur_col, raw, salt):
    """A pelt seen from the fur side (cured) or the flesh side (raw: pale skin, a fringe of fur at the edge)."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    cells = {(x, y) for y, row in enumerate(mask) for x, c in enumerate(row) if c != "."}
    for (x, y) in cells:
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if raw and not edge:
            c = lerp(HIDE_SIDE, (196, 120, 110), 0.25 * h(x, y, salt) + (0.25 if h(x // 2, y, salt + 1) < 0.2 else 0))
        else:
            c = fur_col(x, y)
            if edge:
                c = shade(c, 0.72)
        im.putpixel((x, y), rgba(c))
    return im


LONG_PELT = [  # a cased small-fur pelt: head at the top, forelegs, body, hind legs, long tail
    "......o..o......",
    "......xxxx......",
    ".......xx.......",
    "....o.xxxx.o....",
    ".....xxxxxx.....",
    "......xxxx......",
    "......xxxx......",
    "......xxxx......",
    "......xxxx......",
    "......xxxx......",
    "....o.xxxx.o....",
    ".....xxxxxx.....",
    ".......xx.......",
    ".......xx.......",
    ".......xx.......",
    ".......tt.......",
]
ROUND_PELT = [
    "................",
    "................",
    "................",
    "................",
    "......xxxx......",
    "....xxxxxxxx....",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "....xxxxxxxx....",
    "......xxxx......",
    "................",
    "................",
    "................",
    "................",
]


def wolverine_pelt_col(x, y):
    d = abs(x - 7.5)
    c = V_BAND if 2.5 < d < 4.5 and 4 < y < 13 else V_BROWN if d < 2.5 else V_DARK
    if y >= 13:
        c = V_DARK  # the bushy tail
    return fur(c, x, y, 601, 0.08)


def lynx_pelt_col(x, y):
    d = abs(x - 7.5)
    c = L_BACK if d < 1.5 else L_SIDE if d < 4 else L_PALE
    if spot(x * 1.0, y * 1.0, 0.0, 611, 2.0, 0.35) and d > 0.5:
        c = lerp(c, L_SPOT, 0.75)
    if y >= 14:
        c = L_BLACK if y == 15 else L_SIDE
    if y <= 1:
        c = L_BLACK  # ear tufts
    return fur(c, x, y, 612, 0.07)


def ermine_pelt_col(x, y):
    return L_BLACK if y >= 14 else fur(lerp(E_WHITE, E_SNOW, h(x, y, 621) * 0.6), x, y, 622, 0.03)


def marten_pelt_col(x, y):
    if 1 <= y <= 3 and abs(x - 7.5) < 1.6:
        return fur(P_BIB, x, y, 631, 0.05)  # the bib under the chin
    return marten_coat(P_DARK if y >= 12 else P_BROWN, x, y, 632)


def lemming_pelt_col(x, y):
    return fur(M_BLACK if y <= 6 else M_SADDLE if abs(x - 7.5) < 1.5 else M_TAWNY, x, y, 641, 0.08)


CLAW_ROWS = [
    "................",
    "................",
    "....oo..........",
    "...oWWo.....oo..",
    "...oWLWo...oWWo.",
    "....oLMWo..oWLWo",
    ".....oMLo...oLMo",
    ".....oMMo...oMLo",
    "....oMMDo..oMMo.",
    "...oDDDo..oMMDo.",
    "...oKKo...oDDo..",
    "....oo....oKKo..",
    "...........oo...",
    "................",
    "................",
    "................",
]
CLAW_PAL = {"o": (52, 40, 30), "W": (250, 244, 226), "L": (232, 222, 196), "M": (206, 192, 160), "D": (150, 130, 100), "K": (70, 50, 38)}

FLESHER_ROWS = [  # a hooked claw blade lashed to a stick, handle bottom-left
    "................",
    "...........oo...",
    "..........oWWo..",
    "..........oLWo..",
    "...........oLMo.",
    "..........oMMMo.",
    ".........oSSDo..",
    "........oSSo....",
    ".......oTTo.....",
    "......oTTo......",
    ".....oSSo.......",
    "....oSSo........",
    "...oSSo.........",
    "..oSSo..........",
    "..ooo...........",
    "................",
]
FLESHER_PAL = {**CLAW_PAL, "S": (128, 92, 56), "T": (196, 170, 120)}

HOOD_TRIM = [(4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (11, 4), (11, 5), (11, 6), (11, 7), (11, 8), (11, 9), (11, 10),
             (5, 3), (6, 3), (9, 3), (10, 3), (5, 11), (10, 11)]

MANTLE_ROWS = [
    "................",
    "................",
    "....oooooooo....",
    "...oWWWWWWWWo...",
    "..oWWKWWWWKWWo..",
    "..oWWWWLLWWWWo..",
    ".oWWWWLooLWWWWo.",
    ".oWKWWLo.oLWWKWo",
    ".oWWWWo...oWWWWo",
    "oWWWWWo...oWWWWo",
    "oWWKWWo...oWKWWo",
    "oWWWWWo...oWWWWo",
    "oWWWWo.....oWWWo",
    "ooooo.......oooo",
    "................",
    "................",
]

CAP_ROWS = [
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "....oBBbBBBo....",
    "...oBbBBBBbBo...",
    "...oBBBBbBBBo...",
    "..oBBBbBBBBBBo..",
    "..oBBBBBBBbBBo..",
    "..oCCCCCCCCCCo..",
    "..oCcCCCcCCCCo..",
    "...oooooooooo...",
    "................",
    "................",
    "................",
    "................",
]


def item_textures():
    out = {}
    venison = ITEM / "venison_scraps.png"
    out["raw_wolverine"] = recolour(venison, RAW_MEAT)
    out["cooked_wolverine"] = recolour(venison, COOKED)
    out["raw_lynx"] = recolour(venison, RAW_PALE)
    out["cooked_lynx"] = recolour(venison, ["#2a170d", "#3c2214", "#4f2e1b", "#5f3a23", "#6e452b", "#7c5034", "#8b5c3d", "#b48a60"])
    out["raw_ermine"] = small_meat(SMALL_LONG, RAW_PALE, 701)
    out["cooked_ermine"] = small_meat(SMALL_LONG, COOKED, 702)
    out["raw_marten"] = small_meat(SMALL_LONG, RAW_MEAT, 703)
    out["cooked_marten"] = small_meat(SMALL_LONG, COOKED, 704)
    out["raw_lemming"] = small_meat(SMALL_ROUND, RAW_PALE, 705)
    out["cooked_lemming"] = small_meat(SMALL_ROUND, COOKED, 706)
    out["raw_wolverine_pelt"] = pelt_shape(PELT_MASK, wolverine_pelt_col, True, 711)
    out["wolverine_fur"] = pelt_shape(PELT_MASK, wolverine_pelt_col, False, 712)
    out["raw_lynx_pelt"] = pelt_shape(PELT_MASK, lynx_pelt_col, True, 713)
    out["lynx_pelt"] = pelt_shape(PELT_MASK, lynx_pelt_col, False, 714)
    out["raw_ermine_pelt"] = pelt_shape(LONG_PELT, ermine_pelt_col, True, 715)
    out["ermine_fur"] = pelt_shape(LONG_PELT, ermine_pelt_col, False, 716)
    out["raw_marten_pelt"] = pelt_shape(LONG_PELT, marten_pelt_col, True, 717)
    out["marten_fur"] = pelt_shape(LONG_PELT, marten_pelt_col, False, 718)
    out["lemming_pelt"] = pelt_shape(ROUND_PELT, lemming_pelt_col, False, 719)
    out["wolverine_claw"] = grid(CLAW_ROWS, lambda ch, x, y: CLAW_PAL[ch])
    out["claw_flesher"] = grid(FLESHER_ROWS, lambda ch, x, y: FLESHER_PAL[ch])
    hood = Image.open(ITEM / "fur_hood.png").convert("RGBA")
    for p in HOOD_TRIM:  # the frosty ruff round the face opening
        hood.putpixel(p, rgba(lerp(V_BAND, (232, 222, 200), 0.5 * h(*p, 721)) if h(*p, 722) < 0.75 else V_TIP))
    out["wolverine_trimmed_hood"] = hood
    out["ermine_mantle"] = grid(MANTLE_ROWS, lambda ch, x, y: {"o": (70, 64, 58), "K": L_BLACK, "L": HIDE_DRY}.get(ch)
                                or fur(lerp(E_WHITE, E_SNOW, h(x, y, 731)), x, y, 732, 0.03))
    out["marten_fur_cap"] = grid(CAP_ROWS, lambda ch, x, y: {"o": OUTLINE, "b": (150, 110, 70), "c": shade(P_BIB, 0.85)}.get(ch)
                                 or (marten_coat(P_BROWN, x, y, 741) if ch == "B" else fur(P_BIB, x, y, 742, 0.05)))
    out["lynx_rug"] = lynx_rug()
    out.update(eggs())
    return out


def lynx_rug():
    def col(ch, x, y):
        if ch == "N":
            return PINK_NOSE
        if ch == "E":
            return EYE_LYNX
        if ch in "sS":
            return L_PALE
        if ch == "e":
            return L_BLACK  # ears with their tufts
        if ch == "w":
            return CLAW if h(x, y, 751) < 0.6 else shade(CLAW, 0.7)
        if ch == "o":
            return shade(L_BACK, 0.7)
        if ch == "t":
            return L_SIDE
        if ch == "T":
            return L_BLACK
        return lynx_pelt_col(x, min(13, max(2, y)))
    return grid(RUG_MASK, col)


def eggs():
    K = "#1e1a18"
    lynx = egg(["#d8c2a0", "#c2a882", "#a88e6a", "#8a7254", "#5e4c38", "#3e3226"],
               [(4, 6, "#5a4232"), (11, 7, "#5a4232"), (5, 11, "#5a4232"), (10, 12, "#5a4232"), (8, 9, "#5a4232")],
               [(4, 0), (11, 0), (4, 1), (11, 1)],
               [(4, 0, K), (11, 0, K), (4, 1, K), (11, 1, K), (6, 6, "#c0a040"), (9, 6, "#c0a040"),
                (3, 9, "#ece4d4"), (12, 9, "#ece4d4"), (3, 10, K), (12, 10, K)])
    wolverine = egg(["#6a4c36", "#56402e", "#463224", "#36261c", "#241912", "#160f0b"],
                    [(x, 7 + (x - 3) // 4, "#b08a5c") for x in range(3, 13)],
                    [(5, 1), (10, 1)],
                    [(5, 1, "#46342a"), (10, 1, "#46342a")] + [(x, 3, "#948470") for x in range(6, 10)]
                    + [(6, 5, "#100c0a"), (9, 5, "#100c0a")])
    ermine = egg(["#fbfaf6", "#efede6", "#e0ddd2", "#cbc6b8", "#9a958a", "#6a665e"],
                 [], [(7, 15), (8, 15)], [(7, 14, K), (8, 14, K), (7, 15, K), (8, 15, K), (6, 5, K), (9, 5, K), (7, 7, K)])
    lemming = egg(["#d8a462", "#c08848", "#a87036", "#8a5a2a", "#5e3c1c", "#3c2612"],
                  [(7, 9, "#5c3e24"), (8, 10, "#5c3e24"), (7, 11, "#5c3e24")], [],
                  [(x, y, "#262018") for x in range(4, 12) for y in (2, 3, 4)] + [(6, 4, "#e2cfa0"), (9, 4, "#e2cfa0")])
    marten = egg(["#8a5c3a", "#74492c", "#603b22", "#4c2e1a", "#321e10", "#1e120a"],
                 [], [(4, 1), (11, 1)],
                 [(4, 1, "#e2d0aa"), (11, 1, "#e2d0aa"), (6, 5, "#100c0a"), (9, 5, "#100c0a")]
                 + [(x, y, "#e6be70") for x in range(6, 10) for y in (8, 9, 10)])
    return {"lynx_spawn_egg": lynx, "wolverine_spawn_egg": wolverine, "ermine_spawn_egg": ermine,
            "lemming_spawn_egg": lemming, "pine_marten_spawn_egg": marten}


# =============================================================== equipment layers
def hood_layer():
    """The Fur Hood's layer with a thick, frosty wolverine ruff round the face opening: the brow band and the edges of the
    front face of the head, and the front columns of both sides."""
    im = Image.open(EQUIP / "humanoid/fur.png").convert("RGBA")
    px = im.load()
    out = im.copy()
    o = out.load()

    def ruff(x, y):
        o[x, y] = rgba(lerp(V_BAND, (236, 228, 210), 0.6 * h(x, y, 801)) if h(x, y, 802) < 0.72 else V_TIP)

    for y in range(8, 16):
        for x in range(8, 16):  # front face: everything round the opening
            if px[x, y][3] and (y >= 9 or x in (8, 15)):
                ruff(x, y)
        for x in (6, 7, 16, 17):  # the sides, next to the face
            if px[x, y][3]:
                ruff(x, y)
    return out


def recolour_layer(path, colour, region):
    src = vanilla(path)
    px = src.load()
    out = Image.new("RGBA", src.size, CLEAR)
    o = out.load()
    for y in range(src.height):
        for x in range(src.width):
            if px[x, y][3] and region(x, y):
                s = (lum(px[x, y]) - 153) / (214 - 153)
                o[x, y] = rgba(colour(x, y, max(0.0, min(1.0, s))))
    return out


def mantle_colour(x, y, s):
    if h(x // 2, y // 2, 811) < 0.08 and h(x, y, 812) < 0.7:
        return L_BLACK  # the black tail-tips sewn on as spots
    if y in (31, 20) and 40 <= x < 56:
        return HIDE_DRY  # the tanned hem at the cuffs
    return fur(lerp(E_SNOW, E_WHITE, s), x, y, 813, 0.03)


def cap_colour(x, y, s):
    if (y == 15 or y == 14) and x < 32:
        return fur(P_BIB, x, y, 821, 0.05)  # a cream band round the brim
    return marten_coat(lerp(P_DARK, P_BROWN, 0.4 + 0.6 * s), x, y, 822)


def build():
    import gen_hunt_models  # the five animals' models and skins (HuntGeometry) come from one table each there
    gen_hunt_models.main()
    for name, im in item_textures().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
        print("wrote item", name)
    lynx_rug().save(BLOCK / "lynx_rug.png")
    layers = {
        "wolverine_trimmed_hood": hood_layer(),
        "ermine_mantle": recolour_layer("entity/equipment/humanoid/leather", mantle_colour, lambda x, y: y >= 16),
        "marten_fur_cap": recolour_layer("entity/equipment/humanoid/leather", cap_colour, lambda x, y: y < 16),
    }
    for name, im in layers.items():
        assert im.getbbox(), name
        im.save(EQUIP / f"humanoid/{name}.png")
        print("wrote equipment", name)


if __name__ == "__main__":
    build()
