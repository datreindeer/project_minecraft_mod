"""Paint the Wolf Pelt goods (data: scripts/gen_wolf_pelt.py), in the grey wolf's colours (gen_predator_textures.py):
grizzled grey-black saddle, tawny flanks, buff and cream below, amber eyes.

- Wolf Hood: item icon, and the 64x64 equipment layer for client/WolfHoodModel.java (HOOD repeats its boxes: hood,
  upper jaw, fangs, ears, shoulder mantle, back pelt, tail), painted from 3D like the wildlife (wildlife_paint.paint).
- Wolf Fur Coat / Leggings / Boots: icons from the vanilla leather silhouettes, and equipment layers on the vanilla
  leather UVs (back and shoulders grizzled dark, fronts paler).
- Wolf Rug: flat wolfskin seen from above (item + block), head to the north like the Bear Rug.
- Furred Straw Bed: the pelt spread over the bed and the wolf's head on the pillow (UVs in gen_wolf_pelt.py).

Run: python scripts/gen_wolf_pelt_textures.py   (deterministic)
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, shade, smooth  # noqa: E402
from gen_predator_textures import (EYE, EYE_AMBER, NOSE, TONGUE, TOOTH, W_BUFF, W_CREAM, W_DARK, W_GRIZZLE,  # noqa: E402
                                   W_SADDLE, W_TAWNY, agouti)
from gen_bronze_prep_textures import lum, vanilla  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK, EQUIP = TEX / "item", TEX / "block", TEX / "entity/equipment"
CLEAR = (0, 0, 0, 0)
HIDE = (176, 138, 98)  # the tanned skin side of the pelt
OUTLINE = (26, 24, 23)


def coat(t, i, j, salt):
    """Brightness t 0..1 -> grizzled saddle (dark) .. tawny .. buff (light), with banded guard hairs."""
    if t < 0.35:
        return agouti(lerp(W_SADDLE, W_GRIZZLE, t / 0.35), i, j, salt, amt=0.55)
    if t < 0.7:
        return agouti(lerp(W_GRIZZLE, W_TAWNY, smooth((t - 0.35) / 0.35)), i, j, salt + 5, amt=0.3)
    return fur(lerp(W_TAWNY, W_BUFF, (t - 0.7) / 0.3), i, j, salt + 9, 0.06)


# ---------------- Wolf Hood: worn model (64x64) ----------------

def hood(v, face, x, y, z, i, j):
    if face == "bottom":
        return CLEAR  # open underneath
    if face == "front":
        if -3 < x < 3 and y > -6:
            return CLEAR  # the face looks out here
        if abs(abs(x) - 2.5) < 0.1 and abs(y + 6.5) < 0.1:
            return EYE_AMBER  # the wolf's eyes, either side of the jaw's root
        if abs(abs(x) - 3.5) < 0.1 and abs(y + 6.5) < 0.1:
            return EYE
        if y > -6.5:
            return fur(lerp(W_BUFF, W_CREAM, 0.6), i, j, 301, 0.05)  # pale ruff round the face
        return agouti(W_SADDLE if abs(x) < 2 else W_GRIZZLE, i, j, 302, amt=0.5)
    if face == "top":
        return agouti(lerp(W_SADDLE, W_GRIZZLE, abs(x) / 4), i, j, 303, amt=0.6)
    if face == "back":
        return coat(0.15 + 0.1 * abs(x) / 4, i, j, 304)
    return coat(0.25 + 0.5 * (y + 8) / 8, i, j, 305) if y < -1 else fur(W_CREAM, i, j, 306, 0.05)  # cheek ruff


def jaw(v, face, x, y, z, i, j):
    """Upper jaw, nose forward (z = -9); the palate and teeth show from below."""
    if face == "front":
        return NOSE if y < -8.5 else shade(W_DARK, 1.2)
    if face == "top":
        f = (-z - 5) / 4  # 0 at the hood .. 1 at the nose
        return NOSE if f > 0.8 else agouti(lerp(W_TAWNY, W_SADDLE, 0.3 + f * 0.4), i, j, 311, amt=0.3)
    if face == "bottom":
        return TOOTH if abs(x) > 1.4 or z < -8.4 else shade(TONGUE, 0.75)
    if face == "back":
        return W_TAWNY
    if y > -7.6:
        return W_DARK if z < -6.5 else fur(W_CREAM, i, j, 312, 0.04)  # lip line
    return fur(W_TAWNY if z > -7 else lerp(W_TAWNY, W_DARK, 0.4), i, j, 313)


def fang(v, face, x, y, z, i, j):
    return shade(TOOTH, 0.8) if y > -5.6 else shade(TOOTH, 1.0 if face in ("front", "top") else 0.9)


def ear(v, face, x, y, z, i, j):
    """Pointed: the outer top corner is cut away; pale inside, dark rim."""
    if y < -11 and x < -3:
        return CLEAR
    if face == "front":
        return fur(W_CREAM, i, j, 321, 0.05) if x > -3 and y > -11 else fur(W_DARK, i, j, 322, 0.06)
    if face == "back":
        return agouti(W_TAWNY, i, j, 323, amt=0.5)
    return fur(W_DARK, i, j, 324, 0.06)


def mantle(v, face, x, y, z, i, j):
    if face == "bottom":
        return fur(HIDE, i, j, 331, 0.05)
    if face == "top":
        return coat(0.1 + 0.6 * smooth(abs(x) / 8), i, j, 332)
    if face == "front":
        return fur(lerp(W_BUFF, W_CREAM, 0.5), i, j, 333, 0.05) if abs(x) < 4 else coat(0.6, i, j, 334)
    if face == "back":
        return coat(0.15 + 0.4 * abs(x) / 8, i, j, 335)
    return coat(0.6, i, j, 336)


def back_pelt(v, face, x, y, z, i, j):
    if face == "front":
        return fur(HIDE, i, j, 341, 0.05)  # skin side against the wearer
    if face == "back":
        return coat(min(0.9, 0.08 + 0.75 * abs(x) / 5 - 0.02 * (y - 3)), i, j, 342)  # saddle down the middle
    return coat(0.55, i, j, 343)


def tail(v, face, x, y, z, i, j):
    if y > 15:
        return fur(W_DARK, i, j, 351, 0.06)  # black tip
    return agouti(W_SADDLE if face in ("back", "top") else W_GRIZZLE, i, j, 352, amt=0.5)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors WolfHoodModel.createLayer (fangs and ears share one UV each)
HOOD = {
    "hood": (0, 0, -4, -8, -4, 8, 8, 8, hood),
    "jaw": (32, 0, -2, -10, -9, 4, 3, 4, jaw),
    "fang": (48, 0, -1.75, -7, -8.5, 1, 2, 1, fang),
    "ear": (56, 0, -4, -12, -3, 2, 3, 1, ear),
    "mantle": (0, 16, -8, 0, -3, 16, 2, 6, mantle),
    "back": (0, 24, -5, 2.5, 3, 10, 9, 1, back_pelt),
    "tail": (24, 24, -1, 11.5, 3.25, 2, 5, 2, tail),
}

# ---------------- icons ----------------

HOOD_ICON = [
    "...ko......ok...",
    "..kIo......oIk..",
    "..kIgo....ogIk..",
    "..oggggggggggo..",
    ".ogGGgAssAgGGgo.",
    ".ogGGgsSSsgGGgo.",
    ".ogggsSSSSsgggo.",
    ".otttsSNNSsttto.",
    ".otttFkkkkFttto.",
    ".otcxxxxxxxxcto.",
    ".otcxxxxxxxxcto.",
    ".otcxxxxxxxxcto.",
    "otttcxxxxxxctttо".replace("о", "o"),
    "ottttcccccctttto",
    "obbbbbbbbbbbbbbo",
    ".oooooooooooooo.",
]


def hood_icon():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(HOOD_ICON):
        assert len(row) == 16, row
        for x, c in enumerate(row):
            if c == ".":
                continue
            col = {"o": OUTLINE, "k": W_DARK, "I": W_CREAM, "A": EYE_AMBER, "N": NOSE, "F": TOOTH, "x": (34, 26, 22),
                   "S": W_TAWNY, "s": lerp(W_TAWNY, W_SADDLE, 0.4)}.get(c)
            if col is None:
                if c == "G":
                    col = agouti(W_SADDLE, x, y, 361, amt=0.5)
                elif c == "g":
                    col = agouti(W_GRIZZLE, x, y, 362, amt=0.5)
                elif c == "t":
                    col = coat(0.55, x, y, 363)
                elif c == "c":
                    col = fur(W_CREAM, x, y, 364, 0.05)
                else:  # b: pelt over the shoulders
                    col = coat(0.2 + 0.5 * abs(x - 7.5) / 7.5, x, y, 365)
            im.putpixel((x, y), (*col[:3], 255))
    return im


def fur_icon(name, salt, tufts=(), lining=()):
    """Vanilla leather icon silhouette and shading, recoloured as wolf fur (grizzled down the middle, tawny at the sides)."""
    src = vanilla(f"item/leather_{name}")
    px = src.load()
    cols = sorted({px[x, y] for y in range(16) for x in range(16) if px[x, y][3]}, key=lum)
    out = Image.new("RGBA", (16, 16), CLEAR)
    o = out.load()
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            rank = cols.index(px[x, y])  # 0 darkest outline .. highlight
            if rank == 0:
                col = OUTLINE
            elif rank == 1:
                col = shade(W_SADDLE, 0.8)
            else:
                t = 0.2 + 0.13 * (rank - 1) + 0.25 * abs(x - 7.5) / 7.5 + 0.2 * (h(x, y // 2 + x % 2, salt) - 0.5)
                col = coat(t, x, y, salt + 1)
            o[x, y] = (*col[:3], 255)
    for p in lining:  # tanned skin showing at the openings
        o[p] = (*shade(HIDE, 0.85 if sum(p) % 2 else 1.0), 255)
    for p in tufts:  # ragged fur past the hem
        o[p] = (*coat(0.5 if h(*p, salt + 3) < 0.5 else 0.7, *p, salt + 4)[:3], 255)
    return out


# ---------------- equipment layers for the coat, leggings and boots ----------------

def fur_layer(path, salt):
    src = vanilla(path)
    px = src.load()
    out = Image.new("RGBA", src.size, CLEAR)
    o = out.load()
    for y in range(src.height):
        for x in range(src.width):
            if not px[x, y][3]:
                continue
            s = (lum(px[x, y]) - 153) / (214 - 153)  # vanilla leather greys span ~153..214
            t = 0.25 + 0.45 * s + 0.25 * (h(x, y // 2 + x % 2, salt) - 0.5)
            if 32 <= x < 40 and 20 <= y < 32:  # back of the body: the saddle
                t -= 0.3
            elif 20 <= x < 28 and 20 <= y < 32:  # front of the body: paler chest
                t += 0.2
            elif y < 20 and (44 <= x < 52 or 20 <= x < 28):  # tops of the shoulders
                t -= 0.2
            o[x, y] = (*coat(max(0.0, min(1.0, t)), x, y, salt + 1)[:3], 255)
    return out


# ---------------- Wolf Rug ----------------

# Head north: N nose, s/S muzzle, E eyes, e ears; o darker edge, x fur, w claws, t tail, T tail tip.
RUG_MASK = [
    ".......NN.......",
    "......sNNs......",
    "......sSSs......",
    "..e..oxSSxo..e..",
    "..eeoxExxExoee..",
    "...eoxxxxxxoe...",
    "w.o.oxxxxxxo.o.w",
    "oxxooxxxxxxooxxo",
    ".oxxxxxxxxxxxxo.",
    "..oxxxxxxxxxxo..",
    "..oxxxxxxxxxxo..",
    ".oxxxxxxxxxxxxo.",
    "oxxooxxxxxxooxxo",
    "w.o.oxxxxxxo.o.w",
    ".......tt.......",
    ".......TT.......",
]


def wolf_rug():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(RUG_MASK):
        assert len(row) == 16, row
        for x, c in enumerate(row):
            if c == ".":
                continue
            if c == "N":
                col = NOSE
            elif c == "E":
                col = EYE_AMBER
            elif c == "s":
                col = lerp(W_TAWNY, W_DARK, 0.3)
            elif c == "S":
                col = W_TAWNY
            elif c == "e":
                col = W_DARK
            elif c == "w":
                col = (70, 64, 58) if h(x, y, 371) < 0.6 else (110, 102, 92)
            elif c == "o":
                col = OUTLINE if h(x, y, 372) < 0.35 else shade(W_SADDLE, 0.9)
            elif c == "t":
                col = agouti(W_SADDLE, x, y, 373, amt=0.5)
            elif c == "T":
                col = W_DARK
            else:  # saddle down the back, tawny then buff towards the edges
                d = abs(x - 7.5)
                col = coat(0.08 + 0.85 * smooth(d / 6.5), x, y, 374)
            im.putpixel((x, y), (*col[:3], 255))
    return im


# ---------------- Furred Straw Bed ----------------

def bed_pelt():
    """The pelt spread on the bed, seen from above: tail end north, saddle down the middle, ragged tawny edges."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            d = abs(x - 7.5)
            col = coat(0.08 + 0.8 * smooth(d / 7.5), x, y, 381)
            if (x in (0, 15)) and h(x, y, 382) < 0.4:
                col = coat(0.85, x, y, 383)  # ragged pale hair at the edge
            im.putpixel((x, y), (*col[:3], 255))
    return im


def bed_head():
    """Wolf's head, scruff and tail for the furred bed (regions as used by gen_wolf_pelt.py)."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (*coat(0.3, x, y, 391)[:3], 255)
    for y in range(5):  # skull top, snout end (south) at the bottom
        for x in range(6):
            px[x, y] = (*agouti(lerp(W_SADDLE, W_GRIZZLE, abs(x - 2.5) / 3), x, y, 392, amt=0.55)[:3], 255)
    for x in (1, 4):
        px[x, 4] = (*EYE_AMBER, 255)
    for y in range(2):  # snout top, nose at the south end
        for x in range(6, 9):
            px[x, y] = (*(NOSE if y == 1 and x == 7 else agouti(W_TAWNY, x, y, 393, amt=0.3))[:3], 255)
    for x in range(6, 9):  # snout front
        px[x, 2] = (*NOSE, 255)
        px[x, 3] = (*shade(W_DARK, 1.2), 255)
    for x in range(6, 8):  # snout sides with the lip line
        px[x, 4] = (*fur(W_TAWNY, x, 4, 394), 255)
        px[x, 5] = (*W_DARK, 255)
    for y in range(2):  # ears
        for x in range(10, 12):
            px[x, y] = (*fur(W_DARK, x, y, 395, 0.06), 255)
    for y in range(6, 8):  # skull sides: grizzled above, cream cheeks
        for x in range(6):
            px[x, y] = (*(agouti(W_GRIZZLE, x, y, 396, amt=0.5) if y == 6 else fur(W_CREAM, x, y, 397, 0.05))[:3], 255)
    for y in range(8, 10):  # brow facing the snout
        for x in range(6):
            px[x, y] = (*coat(0.5, x, y, 398)[:3], 255)
    for y in range(10, 13):  # scruff
        for x in range(11):
            px[x, y] = (*coat(0.1 + 0.6 * abs(x - 5) / 5, x, y, 399)[:3], 255)
    for y in range(10, 15):  # tail, black tip at the bottom
        for x in range(13, 16):
            px[x, y] = (*(W_DARK if y >= 13 else agouti(W_SADDLE, x, y, 400, amt=0.5))[:3], 255)
    return im


def build():
    outs = {
        EQUIP / "humanoid/wolf_hood.png": paint(HOOD, "hood", (64, 64)),
        EQUIP / "humanoid/wolf_fur.png": fur_layer("entity/equipment/humanoid/leather", 411),
        EQUIP / "humanoid_leggings/wolf_fur.png": fur_layer("entity/equipment/humanoid_leggings/leather", 421),
        ITEM / "wolf_hood.png": hood_icon(),
        ITEM / "wolf_fur_coat.png": fur_icon("chestplate", 431, tufts=[(4, 15), (6, 15), (9, 15), (11, 15), (0, 8), (15, 8)], lining=[(6, 3), (9, 3)]),
        ITEM / "wolf_fur_leggings.png": fur_icon("leggings", 441, tufts=[(3, 14), (5, 14), (10, 14), (12, 14)]),
        ITEM / "wolf_fur_boots.png": fur_icon("boots", 451, tufts=[(2, 9), (13, 9), (2, 6), (13, 6)], lining=[(4, 4), (10, 4)]),
        ITEM / "wolf_rug.png": wolf_rug(),
        BLOCK / "wolf_rug.png": wolf_rug(),
        BLOCK / "furred_straw_bed_pelt.png": bed_pelt(),
        BLOCK / "furred_straw_bed_head.png": bed_head(),
    }
    for path, im in outs.items():
        assert im.getbbox(), path
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path)
    return outs


if __name__ == "__main__":
    for p in build():
        print("wrote", p.relative_to(TEX))
