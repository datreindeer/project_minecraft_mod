"""Generate the bronze-prep textures: tusk dagger, bear-fur armour (icons + equipment layers), bear rug
(item + block), bone fish hook and bone-hook fishing rod, tallow, bone / fire / barbed arrows, the sinew bow
the fletching table GUI and the boiled egg / peeled egg / eggshell items.

Layout-sensitive textures (arrows, bow frames, armour icons, equipment layers, GUI) are built from the vanilla
textures in the merged jar so their pixel layout stays identical; the rest are drawn from grids.

Run: python scripts/gen_bronze_prep_textures.py
"""
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR, PAL as ROD_PAL, ROD, V_DARK, V_GRIP, V_GRIP_HI, V_LIGHT, V_MID, V_OUTER, V_STRING  # noqa: E402
from gen_wild_boar_texture import h  # noqa: E402
from gen_wildlife_item_textures import from_grid, hexc  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
EQUIP, GUI = TEX / "entity/equipment", TEX / "gui"

_jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(_jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


# ---------- shared palettes (taken from the mod's own materials) ----------

FUR = ["#22160e", "#2f1f14", "#3e291b", "#4f3523", "#61432c", "#755334", "#8a6640"]  # bear_pelt, dark->light
FUR_TIP = "#a8814f"
LEATHER = {"G": "#9a6b43", "g": "#7c5434", "k": "#5a3a22"}
TUSK = {"O": "#4a3f2c", "W": "#fffdf2", "L": "#f1ead2", "M": "#ddd1ac", "D": "#bfae86"}
BONE = {"W": "#f4f1e2", "L": "#e8e5d2", "M": "#a9a89a", "D": "#7b7e6b", "O": "#4a4a40"}
CORD = {"C": "#b5977d", "c": "#a68870", "n": "#765636"}
SINEW = {"1": "#fbf4d8", "2": "#ecdcaa", "3": "#d6c087", "4": "#b09662", "5": "#7c6640"}


def fur(t):
    """Fur colour for brightness t in 0..1."""
    return hexc(FUR[max(0, min(len(FUR) - 1, int(t * len(FUR))))])


# ---------- tusk dagger: diagonal like the vanilla swords, the blade curving up at the tip ----------

TUSK_DAGGER = [
    "..............O.",
    ".............OWO",
    ".............OLO",
    "............OWLO",
    "...........OWLMO",
    "..........OWLMDO",
    "........OOWLMDO.",
    "...nC..OWLLMDO..",
    "...cnCOWLMMDOO..",
    "....cnCMMDDO....",
    ".....CcnOO......",
    "....kgGcn.......",
    "...kGgk.c.......",
    "..kGgk..........",
    ".OGgk...........",
    ".OO.............",
]


def tusk_dagger():
    return from_grid(TUSK_DAGGER, TUSK | CORD | LEATHER)


# ---------- fur armour icons: vanilla leather icon silhouettes, recoloured as shaggy fur ----------

def fur_icon(name, salt, ears=(), tufts=(), leather=()):
    src = vanilla(f"item/leather_{name}")
    px = src.load()
    cols = sorted({px[x, y] for y in range(16) for x in range(16) if px[x, y][3]}, key=lum)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    o = out.load()
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            rank = cols.index(px[x, y])  # 0 darkest outline .. 5 highlight
            if rank == 0:
                o[x, y] = hexc("#160e08")
            elif rank == 1:
                o[x, y] = hexc("#24170e") if h(x, y, salt) < 0.7 else fur(0.15)
            else:
                # shaggy: brightness from the vanilla shading plus streaks that run down the fur
                t = 0.25 + 0.16 * (rank - 1) + 0.3 * (h(x, y // 2 + x % 2, salt + 1) - 0.5)
                o[x, y] = fur(t) if h(x, y, salt + 2) > 0.05 else hexc(FUR_TIP)
    for p in leather:  # leather lining showing at the openings
        o[p] = hexc(LEATHER["g"] if (p[0] + p[1]) % 2 else LEATHER["k"])
    for p in tufts:  # ragged fur hanging past the hem
        o[p] = fur(0.25) if h(*p, salt + 3) < 0.5 else fur(0.4)
    for p in ears:
        o[p] = hexc("#1c120b")
    return out


def fur_hood():
    # round bear ears on the crown, tufts at the cheeks
    return fur_icon("helmet", 21, ears=[(4, 2), (5, 2), (10, 2), (11, 2), (4, 3), (11, 3)],
                    tufts=[(3, 11), (12, 11)], leather=[(5, 4), (10, 4)])


def fur_coat():
    return fur_icon("chestplate", 31, tufts=[(4, 15), (6, 15), (9, 15), (11, 15), (0, 8), (15, 8)],
                    leather=[(6, 3), (9, 3)])


def fur_leggings():
    return fur_icon("leggings", 41, tufts=[(3, 14), (5, 14), (10, 14), (12, 14)])


def fur_boots():
    return fur_icon("boots", 51, tufts=[(2, 9), (13, 9), (2, 6), (13, 6)], leather=[(4, 4), (10, 4)])


# ---------- bear rug: flat bearskin seen from above, head pointing north ----------

# Snout points north (top): N nose, s/S muzzle, E eyes, e ears; o darker edge fur, x fur, w claws.
RUG_MASK = [
    "......oooo......",
    "......sNNs......",
    ".....osSSso.....",
    "....oxExxExo....",
    "...eexxxxxxee...",
    ".o..oxxxxxxo..o.",
    "wxo.oxxxxxxo.oxw",
    "oxxooxxxxxxooxxo",
    ".oxxxxxxxxxxxxo.",
    "..oxxxxxxxxxxo..",
    "..oxxxxxxxxxxo..",
    "..oxxxxxxxxxxo..",
    ".oxxxxxxxxxxxxo.",
    "oxxooxxxxxxooxxo",
    "wxo.oxxxxxxo.oxw",
    ".o...ooxxoo...o.",
]
RUG_FIXED = {"N": "#120d0b", "E": "#0d0907", "s": "#8a6640", "S": "#a8814f", "e": "#2a1b11"}


def bear_rug():
    mask = {(x, y): c for y, r in enumerate(RUG_MASK) for x, c in enumerate(r) if c != "."}
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in mask.items():
        if c in RUG_FIXED:
            col = hexc(RUG_FIXED[c])
        elif c == "w":
            col = hexc("#e8dcc0") if h(x, y, 61) < 0.6 else hexc("#b8a888")
        elif c == "o":
            col = hexc("#1a110b") if h(x, y, 62) < 0.35 else fur(0.1)
        else:
            dist = min(abs(x - 7.5), 6) / 6  # paler down the middle of the back
            t = 0.62 - 0.35 * dist + 0.3 * (h(x, y // 2 + x % 3, 63) - 0.5)
            col = fur(t) if h(x, y, 64) > 0.07 else hexc(FUR_TIP)
        im.putpixel((x, y), col)
    return im


# ---------- bone fish hook ----------

BONE_HOOK = [
    "................",
    "................",
    "........OO......",
    ".......OWLO.....",
    ".......OLDO.....",
    "........OO......",
    "........OLO.....",
    "........OLO.....",
    "........OLMO....",
    "....OO..OLMO....",
    "...OWO..OLMO....",
    "...OLMO.OLDO....",
    "...OLMDOOMDO....",
    "....OMDDMMDO....",
    ".....OODDOO.....",
    ".......OO.......",
]


def bone_fish_hook():
    return from_grid(BONE_HOOK, BONE)


# ---------- bone-hook fishing rod: the crude rod with a bone hook and a heavily twisted line ----------

# extra-twisted line: three twine tones cycling plus a pale fibre glint
TWIST = ["#826655", "#634e42", "#a08470", "#534237"]


def bone_rod(cast):
    rows = [list(r) for r in ROD]
    line = []
    for y, r in enumerate(rows):
        for x, ch in enumerate(r):
            if ch in "Ffg":
                r[x] = "."
            if ch in "sS":
                line.append((x, y))
                r[x] = "."
    im = from_grid(["".join(r) for r in rows], ROD_PAL)
    if not cast:
        for i, p in enumerate(sorted(line, key=lambda p: p[1])):
            im.putpixel(p, hexc(TWIST[i % 4]))
        # J-shaped bone hook hanging from the line end at (12, 12)
        for p, k in {(12, 13): "L", (12, 14): "M", (11, 14): "D", (10, 14): "M", (10, 13): "W"}.items():
            im.putpixel(p, hexc(BONE[k]))
    return im


# ---------- tallow: a pale cream lump of rendered fat ----------

TALLOW = [  # a cut block of tallow: pale top face, shaded side faces, soft rounded corners
    "................",
    "................",
    "................",
    "......OOOOOO....",
    "....OOWWWWLLOO..",
    "..OOWWLLLLLLLMO.",
    ".OWLLLLLLLLLMMO.",
    ".OLLLLLLLLLMMDO.",
    ".OMLLLLLLMMMdDO.",
    ".OMMMMMMMMDDdDO.",
    ".OMMMMMMMMDdDDO.",
    ".OMMMMMMMMDDDdO.",
    ".OdMMMMMMMDDdO..",
    "..OOdddddddOO...",
    "....OOOOOOO.....",
    "................",
]
TALLOW_PAL = {"O": "#8f7c52", "W": "#fffcf0", "L": "#f8f0d8", "M": "#e6d4a4", "D": "#d2bb84", "d": "#b9a26a"}


# ---------- arrows: the vanilla arrow with a new tip or wrap ----------

V_HEAD = {(150, 150, 150): "M", (255, 255, 255): "W", (68, 68, 68): "O", (216, 216, 216): "L"}
V_SHAFT = {(137, 103, 39), (40, 30, 11)}


def bone_arrow():
    im = vanilla("item/arrow")
    px = im.load()
    pal = BONE | {"O": "#5c5848"}
    for y in range(8):  # the head is in the top right, the fletching bottom left
        for x in range(8, 16):
            k = V_HEAD.get(px[x, y][:3])
            if px[x, y][3] and k:
                px[x, y] = hexc(pal[k])
    return im


def fire_arrow():
    im = vanilla("item/arrow")
    wrap = {(9, 6): "#9a7422", (10, 6): "#6e5016", (8, 7): "#b08a34", (9, 7): "#7a5a1a",
            (7, 8): "#9a7422", (8, 8): "#5a4012", (8, 6): "#6e5016", (10, 7): "#4e3810",
            (7, 7): "#7a5a1a", (9, 8): "#4e3810", (9, 5): "#d0a850"}  # last one: a glossy drip of tallow
    for p, c in wrap.items():
        im.putpixel(p, hexc(c))
    return im


BARB_HEAD = [  # rows 0..7, columns 8..15: a hooked claw instead of the flint head
    "......O.",
    ".....OHO",
    "..O.OHLO",
    "..OOLMDO",
    "..OeMDO.",
    "...fOO..",
    "........",
    "........",
]


def barbed_arrow():
    im = vanilla("item/arrow")
    pal = {"O": "#211a12", "H": "#efe6cf", "L": "#c9bb98", "M": "#9c8b6b", "D": "#6c5c44",
           "e": "#896727", "f": "#281e0b"}
    for y, row in enumerate(BARB_HEAD):
        for x8, ch in enumerate(row):
            p = (8 + x8, y)
            if ch == ".":
                if im.getpixel(p)[:3] not in V_SHAFT:
                    im.putpixel(p, (0, 0, 0, 0))
            else:
                im.putpixel(p, hexc(pal[ch]))
    return im


# ---------- sinew bow: recolour the vanilla bow frames by colour class ----------

def sinew_bow(name):
    src = vanilla(f"item/{name}")
    px = src.load()
    pulling = name != "bow"
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    o = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if not a:
                continue
            rgb = (r, g, b)
            if pulling and rgb in (V_LIGHT, V_MID, V_DARK) and x - y in (0, 1):
                o[x, y] = px[x, y]  # vanilla arrow shaft
            elif rgb == V_OUTER:  # back of the limb: sinew backing
                o[x, y] = hexc(SINEW["2"] if (x + y) % 3 else SINEW["3"])
            elif rgb == V_LIGHT:
                o[x, y] = hexc("#9a6a36")  # smooth, oiled wood
            elif rgb == V_MID:
                o[x, y] = hexc("#744c24")
            elif rgb == V_DARK:
                o[x, y] = hexc("#3a2410")
            elif rgb == V_GRIP_HI:
                o[x, y] = hexc(CORD["C"])
            elif rgb == V_GRIP:
                o[x, y] = hexc(CORD["c" if (x + y) % 2 else "n"])
            elif rgb == V_STRING:
                o[x, y] = hexc(SINEW["3"] if (x + y) % 2 else SINEW["4"])
            else:
                o[x, y] = px[x, y]  # arrow head / fletching
    return out


# ---------- equipment layers: vanilla leather UVs, shaggy fur drawn directly ----------

def fur_layer(path, salt, bear_head=False):
    src = vanilla(path)
    px = src.load()
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    o = out.load()
    for y in range(src.height):
        for x in range(src.width):
            if not px[x, y][3]:
                continue
            shade = (lum(px[x, y]) - 153) / (214 - 153)  # vanilla leather greys span ~153..214
            t = 0.2 + 0.45 * shade + 0.35 * (h(x, y // 2 + x % 2, salt) - 0.5)
            col = fur(t)
            if h(x, y, salt + 1) < 0.035:
                col = hexc(FUR_TIP)
            elif h(x, y, salt + 2) < 0.05:
                col = hexc(FUR[0])
            o[x, y] = col
    if bear_head:
        # head top (8..15, 0..7), front at the bottom edge: round ears near the front corners, a pale crown
        for p in [(8, 5), (9, 5), (8, 6), (9, 6), (14, 5), (15, 5), (14, 6), (15, 6)]:
            o[p] = hexc("#1c120b")
        for p in [(9, 6), (14, 6)]:
            o[p] = hexc("#4f3523")
        # front face (8..15, 8..15): two small eyes on the brow of the hood
        for p in [(10, 9), (13, 9)]:
            o[p] = hexc("#0d0907")
    return out


# ---------- fletching table GUI: vanilla smithing screen without the hammer, a feather in its place ----------

def fletching_gui():
    im = vanilla("gui/container/smithing")
    bg = im.getpixel((100, 10))
    for y in range(7, 38):
        for x in range(7, 37):
            im.putpixel((x, y), bg)
    # feather at 2x like the hammer was, with a dark outline so it reads on the grey panel
    feather = vanilla("item/feather")
    outlined = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            if not feather.getpixel((x, y))[3] and any(
                    0 <= x + dx < 16 and 0 <= y + dy < 16 and feather.getpixel((x + dx, y + dy))[3]
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                outlined.putpixel((x, y), (55, 55, 55, 255))
    outlined.alpha_composite(feather)
    im.alpha_composite(outlined.resize((32, 32), Image.NEAREST), (6, 6))
    return im


# ---------- eggs ----------

def boiled_egg():
    # vanilla egg, matte (no bright highlight), a warmer cooked tint and a hairline crack
    im = vanilla("item/egg")
    px = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a:
                if (r, g, b) == (240, 230, 198):
                    r, g, b = 223, 206, 155
                px[x, y] = (int(r * 0.93), int(g * 0.88), int(b * 0.78), a)
    for p in [(6, 7), (7, 8), (8, 8), (9, 9), (8, 10)]:
        px[p] = hexc("#7a6a48")
    return im


PEELED_EGG = [  # halved lengthwise: glossy white with the yolk showing
    "................",
    "................",
    "................",
    "......OOOO......",
    ".....OHWWWO.....",
    "....OHWWWWLO....",
    "....OWWyyWLO....",
    "...OWWyYYyWLO...",
    "...OWyYYHYyLO...",
    "...OWyYYYYyLO...",
    "...OWWyYYydLO...",
    "...OWWWyydLLO...",
    "....OWWWLLLO....",
    ".....OOOOOO.....",
    "................",
    "................",
]
PEELED_PAL = {"O": "#8a8270", "H": "#ffffff", "W": "#f6f3ea", "L": "#d6d0c0",
              "Y": "#f4c43a", "y": "#e0a426", "d": "#b88020"}

EGGSHELL = [  # crushed shell fragments: pale outside, darker inner curve, brown speckles
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......OO........",
    ".....OWLO..OO...",
    "..OO.OWsiOOWLO..",
    ".OWLOOLiiOWsiiO.",
    ".OsWiOOOOWLiiOO.",
    "..OLiOWWOOLsOWLO",
    "...OOOLsiOOOWiO.",
    ".......OOO...OO.",
    "................",
    "................",
]
EGGSHELL_PAL = {"O": "#7a6e58", "W": "#f4eee0", "L": "#ded2ba", "i": "#bfae8e", "s": "#9a7a52"}


# ---------- main ----------

def build():
    outs = {
        ITEM / "tusk_dagger.png": tusk_dagger(),
        ITEM / "fur_hood.png": fur_hood(),
        ITEM / "fur_coat.png": fur_coat(),
        ITEM / "fur_leggings.png": fur_leggings(),
        ITEM / "fur_boots.png": fur_boots(),
        ITEM / "bear_rug.png": bear_rug(),
        BLOCK / "bear_rug.png": bear_rug(),
        ITEM / "bone_fish_hook.png": bone_fish_hook(),
        ITEM / "bone_hook_fishing_rod.png": bone_rod(False),
        ITEM / "bone_hook_fishing_rod_cast.png": bone_rod(True),
        ITEM / "tallow.png": from_grid(TALLOW, TALLOW_PAL),
        ITEM / "bone_arrow.png": bone_arrow(),
        ITEM / "fire_arrow.png": fire_arrow(),
        ITEM / "barbed_arrow.png": barbed_arrow(),
        EQUIP / "humanoid/fur.png": fur_layer("entity/equipment/humanoid/leather", 71, bear_head=True),
        EQUIP / "humanoid_leggings/fur.png": fur_layer("entity/equipment/humanoid_leggings/leather", 81),
        GUI / "fletching_table.png": fletching_gui(),
        ITEM / "boiled_egg.png": boiled_egg(),
        ITEM / "peeled_egg.png": from_grid(PEELED_EGG, PEELED_PAL),
        ITEM / "eggshell.png": from_grid(EGGSHELL, EGGSHELL_PAL),
    }
    for name in ["bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"]:
        outs[ITEM / f"sinew_{name}.png"] = sinew_bow(name)
    for path, im in outs.items():
        assert im.getbbox(), path
        path.parent.mkdir(parents=True, exist_ok=True)
        im.save(path)
    return outs


if __name__ == "__main__":
    for p in build():
        print("wrote", p.relative_to(TEX))
