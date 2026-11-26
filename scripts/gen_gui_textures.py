"""The hide-and-bark GUI style for every screen: our own drawings over vanilla's gui textures and sprites
(assets/minecraft/textures/gui/), the HideUi sprites the mod's own screens use, and a re-skin of the mod's container
backgrounds. Colours match com.mechanical.client.ui.HideUi.

Run LAST in the gui part of the chain, after gen_copper_textures, gen_bloomery_textures, gen_smelter_textures,
gen_station_textures, gen_moldbench_textures and gen_equipment_textures: those draw the mod's backgrounds in vanilla
greys, which retheme_mod() turns into leather (running it twice changes nothing).

Vanilla layouts (slot positions and sizes) are written out below; nothing is read from Mojang's textures.
"""
import json
import random
import re
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets"
MC = RES / "minecraft/textures/gui"
MOD = RES / "mechanical/textures/gui"


def rgb(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# ---- palette (HideUi.java has the same numbers) ----
OUTLINE = rgb("140d07")
EDGE = rgb("8a6a44")
EDGE_HI = rgb("a58255")
EDGE_LO = rgb("6a4f31")
INNER = rgb("1e150d")
HIDE = rgb("37281b")
STITCH = rgb("8f714a")
STITCH_HOLE = rgb("1c130b")
SLOT = rgb("55422f")
SLOT_DARK = rgb("160f09")
SLOT_MID = rgb("352719")
SLOT_LIGHT = rgb("7c5f3f")
INSET = rgb("221810")
ARROW = rgb("1a120b")
ARROW_LIP = rgb("6e5438")
GHOST = rgb("35281b")
GOLD = rgb("e8c060")
NAME = rgb("f0d8a8")
TAN_DIM = rgb("9a8266")
LEATHER = rgb("5c4129")
LEATHER_HOT = rgb("7d5832")


def noise(w, h, period_x=None, period_y=None, seed=7):
    """Leather grain: soft blotches plus pores, values about -1..1; repeats every period (for tiled sprites)."""
    px, py = period_x or 4096, period_y or 4096

    def lattice(ix, iy, cell, s):
        return random.Random((ix % max(1, px // cell)) * 7349 + (iy % max(1, py // cell)) * 9151 + s).uniform(-1, 1)

    def value(x, y, cell, s):
        ix, iy, fx, fy = x // cell, y // cell, (x % cell) / cell, (y % cell) / cell
        fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
        top = lattice(ix, iy, cell, s) * (1 - fx) + lattice(ix + 1, iy, cell, s) * fx
        bottom = lattice(ix, iy + 1, cell, s) * (1 - fx) + lattice(ix + 1, iy + 1, cell, s) * fx
        return top * (1 - fy) + bottom * fy

    out = {}
    for y in range(h):
        for x in range(w):
            v = value(x, y, 12, seed) * 0.55 + value(x, y, 4, seed + 1) * 0.30
            pore = random.Random((x % px) * 131 + (y % py) * 977 + seed).random()
            v += -0.55 if pore < 0.055 else 0.4 if pore > 0.965 else (pore - 0.5) * 0.22
            out[x, y] = v
    return out


GRAIN = noise(512, 512)


def shade(c, d):
    return (max(0, min(255, c[0] + d)), max(0, min(255, c[1] + d)), max(0, min(255, c[2] + int(d * 0.8))), c[3])


def new(w, h, c=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), c)


def fill(im, x, y, w, h, c):
    for j in range(y, y + h):
        for i in range(x, x + w):
            if 0 <= i < im.width and 0 <= j < im.height:
                im.putpixel((i, j), c)


def leather(im, x, y, w, h, base=HIDE, amp=7, grain=GRAIN):
    for j in range(y, y + h):
        for i in range(x, x + w):
            if 0 <= i < im.width and 0 <= j < im.height:
                im.putpixel((i, j), shade(base, round(grain[i % 512, j % 512] * amp)))


def frame(im, x, y, w, h, top_left, bottom_right=None):
    bottom_right = bottom_right or top_left
    fill(im, x, y, w, 1, top_left)
    fill(im, x, y, 1, h, top_left)
    fill(im, x, y + h - 1, w, 1, bottom_right)
    fill(im, x + w - 1, y, 1, h, bottom_right)


def cut_corners(im, x, y, w, h, c):
    """Rounds a rectangle's outline: clears the corner pixels and pulls the outline one pixel in."""
    for cx, cy, dx, dy in ((x, y, 1, 1), (x + w - 1, y, -1, 1), (x, y + h - 1, 1, -1), (x + w - 1, y + h - 1, -1, -1)):
        for p in ((cx, cy), (cx + dx, cy), (cx, cy + dy)):
            im.putpixel(p, (0, 0, 0, 0))
        im.putpixel((cx + dx, cy + dy), c)


def stitches(im, x, y, length, horizontal=True, thread=STITCH, hole=STITCH_HOLE):
    """A running stitch: 3 px of thread, 3 px gap, a dark shadow under each thread."""
    for k in range(length):
        if k % 6 < 3:
            i, j = (x + k, y) if horizontal else (x, y + k)
            im.putpixel((i, j), thread)
            si, sj = (i, j + 1) if horizontal else (i + 1, j)
            if hole and 0 <= si < im.width and 0 <= sj < im.height:
                im.putpixel((si, sj), hole)


def panel(im, x, y, w, h, base=HIDE, stitched=True, grain=GRAIN, amp=7, rounded=True):
    """The hide panel: dark outline, tan leather edge, dark seam line, grained hide, a running stitch inside."""
    leather(im, x, y, w, h, base, amp, grain)
    frame(im, x, y, w, h, OUTLINE)
    frame(im, x + 1, y + 1, w - 2, h - 2, EDGE_HI, EDGE_LO)
    im.putpixel((x + w - 2, y + 1), EDGE)
    im.putpixel((x + 1, y + h - 2), EDGE)
    frame(im, x + 2, y + 2, w - 4, h - 4, INNER)
    if rounded:  # clears the corners, so only for a panel with nothing under it
        cut_corners(im, x, y, w, h, OUTLINE)
    if stitched and w >= 24 and h >= 24:
        stitches(im, x + 6, y + 3, w - 12)
        stitches(im, x + 6, y + h - 5, w - 12)
        stitches(im, x + 3, y + 6, h - 12, False)
        stitches(im, x + w - 5, y + 6, h - 12, False)


def slot(im, x, y, w=18, h=18, base=SLOT):
    """A pressed pocket: dark top and left, light lip at the bottom and right."""
    leather(im, x + 1, y + 1, w - 2, h - 2, base, 3)
    fill(im, x, y, w - 1, 1, SLOT_DARK)
    fill(im, x, y, 1, h - 1, SLOT_DARK)
    fill(im, x + 1, y + h - 1, w - 1, 1, SLOT_LIGHT)
    fill(im, x + w - 1, y + 1, 1, h - 1, SLOT_LIGHT)
    im.putpixel((x + w - 1, y), SLOT_MID)
    im.putpixel((x, y + h - 1), SLOT_MID)
    fill(im, x + 1, y + 1, w - 2, 1, shade(base, -14))
    fill(im, x + 1, y + 1, 1, h - 2, shade(base, -14))


def grid(im, x, y, cols, rows, base=SLOT):
    for j in range(rows):
        for i in range(cols):
            slot(im, x + i * 18, y + j * 18, base=base)


def inset(im, x, y, w, h, base=INSET):
    slot(im, x, y, w, h, base)


def inventory_slots(im, x=7, y=83):
    grid(im, x, y, 9, 3)
    grid(im, x, y + 58, 9, 1)


def window(h=166, w=176, size=(256, 256), inv=True):
    im = new(*size)
    panel(im, 0, 0, w, h)
    if inv:
        inventory_slots(im, y=h - 83)
    return im


def pattern(im, x, y, rows, pal):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in pal and 0 <= x + i < im.width and 0 <= y + j < im.height:
                im.putpixel((x + i, y + j), pal[ch])


def stamp(im, x, y, mask, c=ARROW, lip=ARROW_LIP):
    """A shape pressed into the leather: dark, with a light lip along its lower edges."""
    for i, j in mask:
        if (i, j + 1) not in mask:
            im.putpixel((x + i, y + j + 1), lip)
    for i, j in mask:
        im.putpixel((x + i, y + j), c)


def arrow_mask(w=22, h=15):
    """A flint-headed arrow pointing right: a 3 px shaft and a barbed head."""
    mid, head = h // 2, h // 2 + 1
    out = set()
    for i in range(w - head):
        out |= {(i, mid - 1), (i, mid), (i, mid + 1)}
    for k in range(head):
        for j in range(mid - (head - 1 - k), mid + (head - 1 - k) + 1):
            out.add((w - head + k, j))
    return out


ARROW_BIG = arrow_mask(22, 15)

FLAME_ROWS = ["......#.......",
              ".....##.......",
              ".....###......",
              "....####..#...",
              "..#.#####.#...",
              "..#.#####.##..",
              ".##.##########",
              ".#############",
              "##############",
              "######.#######",
              "#####...######",
              ".###.....####.",
              ".###.....###..",
              "...###..###..."]
FLAME_MASK = {(i, j) for j, r in enumerate(FLAME_ROWS) for i, ch in enumerate(r) if ch == "#"}


def plus(im, x, y, s=13):
    m = s // 2
    mask = {(i, j) for i in range(s) for j in range(s) if abs(i - m) <= 1 or abs(j - m) <= 1}
    stamp(im, x, y, mask)


def flame_sprite():
    """The lit fire: dark red at the edge, orange, a yellow-white heart; drawn over the stamped flame."""
    im = new(14, 14)
    for i, j in FLAME_MASK:
        edge = sum((i + dx, j + dy) not in FLAME_MASK for dx, dy in ((1, 0), (-1, 0), (0, -1)))
        heart = abs(i - 6.5) < 3 and 7 <= j <= 11
        im.putpixel((i, j), rgb("fff0a0") if heart and not edge else rgb("c0421a") if edge >= 2 or j < 2
                    else rgb("ee7a1e") if edge or j < 6 else rgb("f8b030"))
    return im


def progress_sprite():
    """24x16 over the stamped 22x15 arrow at (0, 0): a warm amber arrow."""
    im = new(24, 16)
    for i, j in ARROW_BIG:
        top = (i, j - 1) not in ARROW_BIG
        low = (i, j + 1) not in ARROW_BIG
        im.putpixel((i, j), rgb("fbe39a") if top else rgb("b07e2c") if low else GOLD)
    return im


HAMMER = ["..............",
          "......ooo.....",
          ".....ohhho....",
          "......ohhho...",
          ".......ohhho..",
          "......wohhhho.",
          ".....wwo.ohho.",
          "....wwd...oo..",
          "...wwd........",
          "..wwd.........",
          ".wwd..........",
          "wwd...........",
          "wd............"]
HAMMER_PAL = {"o": OUTLINE, "h": rgb("a9a296"), "w": rgb("a07444"), "d": rgb("5e4024")}


def big_icon(im, x, y, rows, pal, k=2):
    for j, row in enumerate(rows):
        for i, ch in enumerate(row):
            if ch in pal:
                fill(im, x + i * k, y + j * k, k, k, pal[ch])


def save(im, path, meta=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    if meta:
        (path.parent / (path.name + ".mcmeta")).write_text(json.dumps({"gui": {"scaling": meta}}, indent=2) + "\n")


def nine(w, h, border, **extra):
    return {"type": "nine_slice", "width": w, "height": h, "border": border, **extra}


# ================= containers =================

def furnace_like():
    im = window()
    slot(im, 55, 16)
    slot(im, 55, 52)
    slot(im, 111, 30, 26, 26)
    stamp(im, 56, 36, FLAME_MASK)
    stamp(im, 79, 34, ARROW_BIG)
    return im


def containers():
    out = {}
    for name in ("furnace", "blast_furnace", "smoker"):
        out[name] = furnace_like()

    im = window()  # crafting table
    grid(im, 29, 16, 3, 3)
    slot(im, 119, 30, 26, 26)
    stamp(im, 90, 35, ARROW_BIG)
    out["crafting_table"] = im

    im = window()
    grid(im, 61, 16, 3, 3)
    out["dispenser"] = im

    im = window(h=133)
    grid(im, 43, 19, 5, 1)
    out["hopper"] = im

    im = window()
    grid(im, 7, 17, 9, 3)
    out["shulker_box"] = im

    # chests: the screen blits rows*18+17 px from the top, then the 96 px from y=126
    im = new(256, 256)
    panel(im, 0, 0, 176, 222)
    grid(im, 7, 17, 9, 6)
    inventory_slots(im, y=139)
    out["generic_54"] = im

    im = window()  # anvil: the hammer, the name field is a sprite at 59,20
    big_icon(im, 17, 7, HAMMER, HAMMER_PAL)
    for x in (26, 75, 133):
        slot(im, x, 46)
    plus(im, 53, 49)
    stamp(im, 102, 48, arrow_mask(22, 13))
    out["anvil"] = im

    im = window()  # smithing
    big_icon(im, 8, 8, HAMMER, HAMMER_PAL)
    grid(im, 7, 47, 3, 1)
    slot(im, 97, 47)
    stamp(im, 68, 49, arrow_mask(22, 13))
    out["smithing"] = im

    im = window()  # brewing stand
    for x, y in ((16, 16), (78, 16), (55, 50), (78, 57), (101, 50)):
        slot(im, x, y)
    def tube(points):
        """A copper pipe, 3 px thick: lit along the top (left), shadowed underneath."""
        cells = set()
        for (x0, y0), (x1, y1) in zip(points, points[1:]):
            for i in range(min(x0, x1), max(x0, x1) + 3):
                for j in range(min(y0, y1), max(y0, y1) + 3):
                    cells.add((i, j))
        for i, j in cells:
            c = rgb("d99a5c") if (i - 1, j) not in cells or (i, j - 1) not in cells else                 rgb("6e4122") if (i + 1, j) not in cells or (i, j + 1) not in cells else rgb("a8683a")
            im.putpixel((i, j), c)

    tube([(86, 34), (86, 55)])
    tube([(86, 40), (63, 40), (63, 48)])
    tube([(86, 40), (109, 40), (109, 48)])
    tube([(34, 23), (46, 23), (46, 45), (56, 45)])
    inset(im, 59, 43, 20, 6)  # the fuel bar
    tube([(34, 24), (44, 24), (44, 44), (58, 44)])
    stamp(im, 99, 18, {(i, j) for j in range(26) for i in range(5)
                       if 1 <= i <= 3 and j < 22 or j >= 22 and abs(i - 2) <= 25 - j}, c=SLOT_MID, lip=SLOT_LIGHT)
    out["brewing_stand"] = im

    im = window()  # cartography table: the map preview is a sprite at 67,13
    slot(im, 14, 14)
    slot(im, 14, 51)
    plus(im, 16, 35)
    stamp(im, 37, 37, arrow_mask(22, 13))
    slot(im, 140, 34, 26, 26)
    out["cartography_table"] = im

    im = window()  # crafter
    grid(im, 25, 16, 3, 3)
    slot(im, 129, 30, 26, 26, base=INSET)
    out["crafter"] = im

    im = window()  # enchanting table: the three offers are sprites at 60,14
    slot(im, 14, 46)
    slot(im, 34, 46)
    inset(im, 59, 13, 110, 59)
    out["enchanting_table"] = im

    im = window()  # grindstone: a stone wheel in a wooden frame behind its two inputs
    wood = rgb("6e4b29")
    for x in (30, 76):
        panel(im, x, 26, 12, 47, base=wood, stitched=False, rounded=False)
        for y in range(31, 69, 3):
            fill(im, x + 4 + y % 4, y, 1, 2, shade(wood, -22))
    fill(im, 41, 41, 36, 5, rgb("4a321c"))  # the axle
    fill(im, 41, 41, 36, 1, rgb("a07444"))
    grit = noise(512, 512, seed=77)
    for j in range(59):  # the wheel seen edge on: rounded ends, shaded toward both sides, grain in the stone
        trim = max(0, 6 - j, j - 52)
        for i in range(trim, 34 - trim):
            side = (i - 14) / 19
            c = shade(rgb("8c857a"), round(grit[i, j] * 16 - side * side * 42))
            if i in (trim, 33 - trim) or j in (0, 58):
                c = OUTLINE
            im.putpixel((42 + i, 14 + j), c)
    slot(im, 48, 18)
    slot(im, 48, 39)
    slot(im, 128, 33)
    stamp(im, 94, 35, ARROW_BIG)
    out["grindstone"] = im

    im = window()  # loom: threads on a frame behind the inputs, the pattern list, the banner preview, the result
    inset(im, 8, 20, 46, 47, base=rgb("2c2014"))
    for x in range(11, 52, 3):
        fill(im, x, 22, 1, 43, rgb("6f5a3c"))
    fill(im, 7, 18, 48, 3, rgb("7a5430"))
    fill(im, 7, 66, 48, 3, rgb("7a5430"))
    for x, y in ((12, 25), (32, 25), (22, 44)):
        slot(im, x, y)
    inset(im, 59, 12, 58, 58)
    inset(im, 118, 12, 14, 58)
    inset(im, 140, 7, 24, 44)
    slot(im, 138, 52, 26, 26)
    out["loom"] = im

    im = window()  # stonecutter
    slot(im, 19, 32)
    inset(im, 51, 14, 66, 56)
    inset(im, 118, 14, 14, 56)
    slot(im, 138, 28, 26, 26)
    out["stonecutter"] = im

    im = window()  # horse: saddle/armour slots and the chest grid are sprites
    inset(im, 25, 17, 54, 54, base=rgb("120c07"))
    inset(im, 79, 17, 90, 54, base=rgb("2c2015"))
    out["horse"] = im

    im = window()
    inset(im, 25, 17, 54, 54, base=rgb("120c07"))
    out["nautilus"] = im

    im = new(256, 256)  # beacon: dark wells for the two power columns, payment row
    panel(im, 0, 0, 230, 219)
    inset(im, 8, 10, 103, 93, base=rgb("160f09"))
    inset(im, 112, 10, 106, 93, base=rgb("160f09"))

    def pyramid(x, y, levels):
        fill(im, x + 9, y - 8, 1, 7, rgb("3a2c1e"))
        for k in range(4):
            lit = k >= 4 - levels
            fill(im, x + 9 - 3 * k - 2, y + 3 * k, 6 * k + 5, 3, rgb("c99a3c") if lit else rgb("3c2d1e"))
            fill(im, x + 9 - 3 * k - 2, y + 3 * k, 6 * k + 5, 1, rgb("f0d27c") if lit else rgb("4c3a27"))
        fill(im, x + 8, y - 1, 3, 2, rgb("f8e8a8"))

    for n, y in enumerate((35, 60, 85)):
        pyramid(17, y, n + 1)
    pyramid(154, 29, 4)
    for x in (42, 64, 86, 108):  # the dividers between the payment items
        for k in range(18):
            im.putpixel((x + 19 - (0 if k < 9 else 2), 109 + k), ARROW_LIP)
    slot(im, 135, 109)
    inventory_slots(im, x=35, y=136)
    out["beacon"] = im

    out["inventory"] = inventory(tall=False)

    # creative tabs (195x136)
    for name in ("tab_items", "tab_item_search", "tab_inventory"):
        im = new(256, 256)
        panel(im, 0, 0, 195, 136)
        if name == "tab_inventory":
            for x, y in ((53, 5), (107, 5), (34, 19), (53, 32), (107, 32)):
                slot(im, x, y)
            inset(im, 72, 5, 34, 45, base=rgb("120c07"))
            grid(im, 8, 53, 9, 3)
            grid(im, 8, 111, 9, 1)
            slot(im, 172, 111, base=rgb("5a2a1e"))
            for k in range(8):  # destroy: a red cross
                for dx in (0, 1):
                    im.putpixel((177 + k + dx, 116 + k), rgb("e0715a"))
                    im.putpixel((177 + k + dx, 123 - k), rgb("e0715a"))
        else:
            grid(im, 8, 17, 9, 5)
            grid(im, 8, 111, 9, 1)
            inset(im, 174, 17, 14, 112)
            if name == "tab_item_search":
                inset(im, 80, 4, 90, 12)
        out["creative_inventory/" + name] = im

    for name, im in out.items():
        save(im, MC / "container" / f"{name}.png")


def accessory_slots():
    """The accessory slots of equipment.Accessory, read from the Java so the picture can't drift from the menu."""
    src = (ROOT / "src/main/java/com/mechanical/equipment/Accessory.java").read_text(encoding="utf-8")
    found = [(int(x), int(y)) for x, y in re.findall(r'^\s+[A-Z_0-9]+\("\w+", (\d+), (\d+),', src, re.M)]
    assert len(found) == 8, f"expected 8 accessory slots, found {found}"
    return found


def inventory(tall):
    """The survival inventory; tall = ours (mixin InventoryScreenMixin): 22 px higher for the accessory slots."""
    extra = 22 if tall else 0
    im = new(256, 256)
    panel(im, 0, 0, 176, 166 + extra)
    grid(im, 7, 7, 1, 4)
    inset(im, 25, 7, 54 if tall else 51, 72, base=rgb("120c07"))
    slot(im, 79 if tall else 76, 61)
    shift = 2 if tall else 0
    grid(im, 97 + shift, 17, 2, 2)
    slot(im, 153 + (1 if tall else 0), 27)
    stamp(im, 135 + (1 if tall else 0), 29, arrow_mask(16, 13))
    if tall:
        for x, y in accessory_slots():
            slot(im, x - 1, y - 1)
        stitches(im, 100, 90, 68)  # a seam beside the belt row
    inventory_slots(im, y=83 + extra)
    return im


# ================= other full textures =================

def misc_textures():
    # recipe book (147x166 at 1,1): a hide with a dark well for the recipes
    im = new(256, 256)
    panel(im, 1, 1, 147, 166)
    inset(im, 9, 9, 131, 150, base=rgb("241a11"))
    pattern(im, 13, 14, ["..ooo...", ".o...o..", "o.ll..o.", "o.l...o.", "o.....o.", ".o...o..", "..ooowo.", "......ww", ".......w"],
            {"o": TAN_DIM, "l": NAME, "w": rgb("a07444")})
    save(im, MC / "recipe_book.png")

    # book: hide cover, parchment page (vanilla writes black on it)
    im = new(256, 256)
    panel(im, 20, 1, 146, 180, base=rgb("5a3520"), stitched=False)
    page = rgb("ecdcb8")
    leather(im, 27, 7, 132, 167, page, 4)
    frame(im, 27, 7, 132, 167, rgb("fbf0d4"), rgb("a8916a"))
    for k in range(4):
        fill(im, 159 + k, 9 + k, 1, 164 - k, rgb("cdb98e") if k % 2 == 0 else rgb("a8916a"))
    stitches(im, 31, 12, 156, False, rgb("8e3b22"), rgb("c9b088"))
    stitches(im, 23, 8, 164, False)
    stitches(im, 24, 177, 138)
    save(im, MC / "book.png")

    # game mode switcher (125x75 of 128x128)
    im = new(128, 128)
    panel(im, 0, 0, 125, 75)
    save(im, MC / "container/gamemode_switcher.png")

    # menu backgrounds: upright bark ridges, dark and see-through (tiled every 32 gui px)
    def bark(alpha, lift=0):
        rnd = random.Random(31)
        tile = new(32, 32)
        columns, x = [], 0
        while x < 32:
            w = rnd.randint(3, 7)
            columns.append((x, min(w, 32 - x), rnd.randint(-4, 4)))
            x += w
        g = noise(32, 32, 32, 32, seed=44)
        for x0, w, tone in columns:
            breaks = {rnd.randrange(32) for _ in range(2)}
            for x in range(x0, x0 + w):
                for y in range(32):
                    d = tone + lift + round(g[x, y] * 5)
                    if x == x0 or y in breaks:
                        d -= 9
                    elif x == x0 + 1:
                        d += 5
                    tile.putpixel((x, y), shade(rgb("21170e", alpha), d))
        return tile

    save(bark(150), MC / "menu_background.png")
    save(bark(120), MC / "inworld_menu_background.png")
    save(bark(150, -8), MC / "menu_list_background.png")
    save(bark(135, -8), MC / "inworld_menu_list_background.png")
    save(bark(235, 4), MC / "tab_header_background.png")
    for name, order in (("header_separator", (EDGE, OUTLINE)), ("footer_separator", (OUTLINE, EDGE))):
        for prefix in ("", "inworld_"):
            im = new(32, 2)
            for x in range(32):
                for y in range(2):
                    c = order[y]
                    im.putpixel((x, y), c if c != EDGE or x % 8 < 5 else EDGE_LO)
            save(im, MC / f"{prefix}{name}.png")


# ================= sprites =================

SP = MC / "sprites"


def button(w, h, face, outline, thread, hi=24, grain=None):
    """A rawhide button; the stitch period (6) divides the 192 px middle slice, so it tiles without a break."""
    grain = grain or noise(w, h, 192, None, seed=13)
    im = new(w, h)
    leather(im, 0, 0, w, h, face, 6, grain)
    frame(im, 0, 0, w, h, outline)
    fill(im, 1, 1, w - 2, 1, shade(face, hi))
    fill(im, 1, 2, 1, h - 4, shade(face, hi // 2))
    fill(im, 1, h - 3, w - 2, 2, shade(face, -22))
    fill(im, w - 2, 2, 1, h - 4, shade(face, -16))
    if thread:
        for x in range(4, w - 4):
            if (x - 4) % 6 < 3:
                for y in (2, h - 4):
                    im.putpixel((x, y), thread)
    return im


def small_button(w, h, state, glyph=None, pal=None):
    face, outline = {"normal": (LEATHER, OUTLINE), "hot": (LEATHER_HOT, GOLD), "off": (rgb("30271e"), OUTLINE),
                     "on": (rgb("4d6a34"), OUTLINE)}[state]
    im = button(w, h, face, outline, None)
    if glyph:
        pattern(im, (w - len(glyph[0])) // 2, (h - len(glyph)) // 2, glyph, pal or {"#": NAME, "s": OUTLINE})
    return im


def boxed(w, h, face, top, bottom, amp=4):
    im = new(w, h)
    leather(im, 0, 0, w, h, face, amp)
    frame(im, 0, 0, w, h, top, bottom)
    return im


def widgets():
    w = SP / "widget"
    for prefix in ("button", "realms_button"):
        save(button(200, 20, LEATHER, OUTLINE, rgb("a98a60")), w / f"{prefix}.png", nine(200, 20, 4))
        save(button(200, 20, LEATHER_HOT, GOLD, rgb("f2d98e")), w / f"{prefix}_highlighted.png", nine(200, 20, 4))
        save(button(200, 20, rgb("332a21"), OUTLINE, None, hi=10), w / f"{prefix}_disabled.png", nine(200, 20, 4))

    track = boxed(200, 20, rgb("22180f"), SLOT_DARK, EDGE_LO)
    save(track, w / "slider.png", nine(200, 20, 1))
    save(boxed(200, 20, rgb("22180f"), GOLD, GOLD), w / "slider_highlighted.png", nine(200, 20, 1))
    for name, face, line in (("slider_handle", rgb("a5855a"), OUTLINE), ("slider_handle_highlighted", rgb("d0ab6c"), rgb("fff0c0"))):
        save(button(8, 20, face, line, None, hi=40), w / f"{name}.png", nine(8, 20, {"left": 2, "top": 2, "right": 2, "bottom": 3}))

    for name, line in (("text_field", EDGE_LO), ("text_field_highlighted", GOLD)):
        im = new(200, 20, rgb("0e0905"))
        frame(im, 0, 0, 200, 20, line)
        save(im, w / f"{name}.png", nine(200, 20, 1))
    im = new(200, 20, rgb("f4e6c4"))
    frame(im, 0, 0, 200, 20, GOLD)
    save(im, w / "preedit.png", nine(200, 20, 1))

    check = ["............##", "...........##s", "..........##s.", ".........##s..", "##......##s...", "s##....##s....",
             ".s##..##s.....", "..s####s......", "...s##s.......", "....ss........"]
    for hot in (False, True):
        suffix = "_highlighted" if hot else ""
        line = GOLD if hot else EDGE
        for selected in (False, True):
            im = boxed(20, 20, rgb("20160d"), OUTLINE, OUTLINE)
            frame(im, 1, 1, 18, 18, line)
            if selected:
                pattern(im, 3, 5, check, {"#": GOLD, "s": OUTLINE})
            save(im, w / f"checkbox{'_selected' if selected else ''}{suffix}.png")

    cross = ["##....##", "###..###", ".######.", "..####..", "..####..", ".######.", "###..###", "##....##"]
    save(small_button(14, 14, "normal", cross), w / "cross_button.png")
    save(small_button(14, 14, "hot", cross), w / "cross_button_highlighted.png")

    locked = [".####.", "#....#", "#....#", "######", "##ss##", "##ss##", "######"]
    unlocked = ["....####..", "...#....#.", "...#....#.", "######....", "##ss##....", "##ss##....", "######...."]
    for name, glyph in (("locked_button", locked), ("unlocked_button", unlocked)):
        for suffix, state in (("", "normal"), ("_highlighted", "hot"), ("_disabled", "off")):
            pal = {"#": TAN_DIM if state == "off" else NAME, "s": OUTLINE}
            save(small_button(20, 20, state, glyph, pal), w / f"{name}{suffix}.png")

    turn = [".....##................", "...####................", ".##########............", "#############..........",
            ".#################.....", "...###..########ssss...", ".....##.....ssssssss..."]
    right = [".....##....", "..####.....", "#######....", "########...", "#########..", "########s..", "#######s...",
             "..###ss....", ".....s....."]
    for hot in (False, True):
        pal = {"#": GOLD if hot else NAME, "s": OUTLINE}
        for name, flip in (("page_backward", False), ("page_forward", True)):
            im = new(23, 13)
            pattern(im, 0, 2, turn, pal)
            if flip:
                im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            save(im, w / f"{name}{'_highlighted' if hot else ''}.png")

    # tabs of the option screens (nine slice, open at the bottom)
    for name, face, top, line in (("tab", rgb("241a11", 230), EDGE_LO, OUTLINE), ("tab_highlighted", rgb("33251a", 235), GOLD, OUTLINE),
                                  ("tab_selected", rgb("5c4129", 245), EDGE_HI, OUTLINE),
                                  ("tab_selected_highlighted", rgb("6d4e30", 245), GOLD, OUTLINE)):
        im = new(130, 24, face)
        fill(im, 0, 0, 130, 1, line)
        fill(im, 0, 0, 1, 24, line)
        fill(im, 129, 0, 1, 24, line)
        fill(im, 1, 1, 128, 1, top)
        fill(im, 1, 1, 1, 23, shade(top, -20))
        fill(im, 128, 1, 1, 23, shade(top, -30))
        save(im, w / f"{name}.png", nine(130, 24, {"left": 2, "top": 2, "right": 2, "bottom": 0}))

    save(boxed(6, 32, rgb("a5855a"), rgb("d6b57c"), rgb("5a4228")), w / "scroller.png", nine(6, 32, 1))
    save(new(6, 32, rgb("100a06", 230)), w / "scroller_background.png", nine(6, 32, 1))
    im = new(80, 80)
    frame(im, 0, 0, 80, 80, EDGE)
    frame(im, 1, 1, 78, 78, OUTLINE)
    save(im, w / "slot_frame.png")


def tooltip_and_popups():
    # vanilla's geometry: the box is drawn 9 px outside the text's padding in a 100x100 sprite with a clear margin
    im = new(100, 100)
    leather(im, 8, 8, 84, 84, rgb("20160d", 246), 3, noise(100, 100, 82, 82, seed=3))
    cut = (0, 0, 0, 0)
    for p in ((8, 8), (91, 8), (8, 91), (91, 91)):
        im.putpixel(p, cut)
    save(im, SP / "tooltip/background.png", nine(100, 100, 9))
    im = new(100, 100)
    frame(im, 9, 9, 82, 82, EDGE_HI, EDGE_LO)
    save(im, SP / "tooltip/frame.png", nine(100, 100, 10, stretch_inner=True))

    for name, w, h, border in (("popup/background", 236, 34, 6), ("social_interactions/background", 236, 34, 8)):
        im = new(w, h)
        panel(im, 0, 0, w, h, stitched=False)
        save(im, SP / f"{name}.png", nine(w, h, border))

    for name, base in (("advancement", rgb("2c2015")), ("system", rgb("2c2015")), ("now_playing", rgb("2c2015"))):
        h = 64 if name == "system" else 32
        im = new(160, h)
        panel(im, 0, 0, 160, h, base=base, stitched=False)
        meta = nine(160, 64, {"left": 17, "top": 30, "right": 4, "bottom": 4}) if name == "system" else nine(160, 32, 4)
        if name == "system":
            fill(im, 6, 9, 2, 8, GOLD)
            fill(im, 6, 19, 2, 2, GOLD)
        save(im, SP / f"toast/{name}.png", meta)


def container_sprites():
    c = SP / "container"
    save((lambda im: (slot(im, 0, 0), im)[1])(new(18, 18)), c / "slot.png")
    for name, a, line in (("slot_highlight_back", 70, None), ("slot_highlight_front", 34, GOLD)):
        im = new(24, 24)
        fill(im, 4, 4, 16, 16, (255, 226, 160, a))
        if line:
            frame(im, 4, 4, 16, 16, (line[0], line[1], line[2], 170))
        save(im, c / f"{name}.png", nine(24, 24, 4))

    for folder in ("furnace", "blast_furnace", "smoker"):
        save(progress_sprite(), c / folder / "burn_progress.png")
        save(flame_sprite(), c / folder / "lit_progress.png")

    # brewing: the arrow fills downwards, bubbles rise, the fuel bar is amber
    im = new(9, 28)
    for j in range(26):
        for i in range(5):
            if 1 <= i <= 3 and j < 22 or j >= 22 and abs(i - 2) <= 25 - j:
                im.putpixel((i + 2, j + 1), GOLD if i != 1 else rgb("fbe39a"))
    save(im, c / "brewing_stand/brew_progress.png")
    im = new(12, 29)
    rnd = random.Random(5)
    for y in range(26, 0, -4):
        x = rnd.randint(1, 8)
        r = 2 if y > 14 else 1
        for i in range(-r, r + 1):
            for j in range(-r, r + 1):
                if i * i + j * j <= r * r + 1 and 0 <= x + i < 12 and 0 <= y + j < 29:
                    im.putpixel((x + i, y + j), rgb("f4e4bc") if i < 0 and j < 0 else rgb("c9b288", 210))
    save(im, c / "brewing_stand/bubbles.png")
    save(boxed(18, 4, GOLD, rgb("fbe39a"), rgb("a06c20"), 10), c / "brewing_stand/fuel_length.png")

    # anvil name field and the crossed-out arrows
    for name, face, line in (("text_field", rgb("120c07"), EDGE), ("text_field_disabled", rgb("2a2017"), SLOT_DARK)):
        im = new(110, 16, face)
        frame(im, 0, 0, 110, 16, line)
        save(im, c / f"anvil/{name}.png")
    for folder in ("anvil", "grindstone", "smithing", "cartography_table"):
        im = new(28, 21)
        for k in range(13):
            for dx in range(3):
                im.putpixel((7 + k + dx, 4 + k), rgb("c8452e") if dx < 2 else OUTLINE)
                im.putpixel((19 - k + dx, 4 + k), rgb("c8452e") if dx < 2 else OUTLINE)
        save(im, c / f"{folder}/error.png")
    im = new(26, 26)
    slot(im, 0, 0, 26, 26, base=rgb("5a2a1e"))
    save(im, c / "loom/error.png")

    # beacon buttons
    for name, state in (("button", "normal"), ("button_highlighted", "hot"), ("button_disabled", "off"), ("button_selected", "on")):
        save(small_button(22, 22, state), c / f"beacon/{name}.png")

    # crafter
    im = new(18, 18)
    slot(im, 0, 0, base=rgb("251b12"))
    for k in range(4, 14):
        im.putpixel((k, k), rgb("8a5a40"))
        im.putpixel((17 - k, k), rgb("8a5a40"))
    save(im, c / "crafter/disabled_slot.png")

    # lists: enchantment offers, stonecutter and loom buttons. Enchanting writes dark brown and green on these,
    # so the offers stay light tanned hide
    for name, face in (("enchantment_slot", rgb("a8906a")), ("enchantment_slot_highlighted", rgb("c4a474")),
                       ("enchantment_slot_disabled", rgb("82694a"))):
        save(boxed(108, 19, face, shade(face, 28), shade(face, -45), 6), c / f"enchanting_table/{name}.png")
    for folder, key, w, h in (("stonecutter", "recipe", 16, 18), ("loom", "pattern", 14, 14)):
        for suffix, face, line in (("", rgb("82643f"), OUTLINE), ("_highlighted", rgb("a87e4a"), GOLD), ("_selected", rgb("4e3a24"), GOLD)):
            save(boxed(w, h, face, line if suffix else shade(face, 26), line if suffix else shade(face, -30)), c / f"{folder}/{key}{suffix}.png")
    for folder in ("stonecutter", "loom", "creative_inventory"):
        for suffix, face in (("", rgb("a5855a")), ("_disabled", rgb("4a3a29"))):
            im = button(12, 15, face, OUTLINE, None, hi=36)
            for y in (4, 6, 8, 10):
                fill(im, 3, y, 6, 1, shade(face, -45))
            save(im, c / f"{folder}/scroller{suffix}.png")

    im = new(90, 54)
    grid(im, 0, 0, 5, 3)
    save(im, c / "horse/chest_slots.png")

    # status effects beside the inventory
    for name, line in (("effect_background", EDGE), ("effect_background_ambient", rgb("5aa8a0"))):
        im = new(32, 32)
        panel(im, 0, 0, 32, 32, base=rgb("2a1e13"), stitched=False, amp=3)
        frame(im, 1, 1, 30, 30, line)
        save(im, c / f"inventory/{name}.png", nine(32, 32, 4))

    # bundle tooltips
    save(boxed(24, 24, rgb("4a3827"), SLOT_DARK, SLOT_LIGHT), c / "bundle/slot_background.png", nine(24, 24, 4))
    for name, a in (("slot_highlight_back", 80), ("slot_highlight_front", 40)):
        im = new(24, 24)
        fill(im, 2, 2, 20, 20, (255, 226, 160, a))
        save(im, c / f"bundle/{name}.png", nine(24, 24, 4))
    im = new(12, 12)
    frame(im, 0, 0, 12, 12, EDGE)
    frame(im, 1, 1, 10, 10, OUTLINE)
    save(im, c / "bundle/bundle_progressbar_border.png", nine(12, 12, 2))
    for name, face in (("bundle_progressbar_fill", rgb("b98a3c")), ("bundle_progressbar_full", rgb("c8452e"))):
        save(boxed(6, 6, face, shade(face, 25), shade(face, -30)), c / f"bundle/{name}.png", nine(6, 6, 2))

    creative_tabs()


def creative_tabs():
    """26x32 tabs, drawn 28 px above (4 rows over the panel's edge) or below it. The selected one grows out of the
    panel: same hide, no line between them; the first and last carry on the panel's side edges."""
    for row in ("top", "bottom"):
        for col in range(1, 8):
            for selected in (False, True):
                im = new(26, 40)  # drawn taller than the tab, then cropped: no bottom edge
                base = HIDE if selected else rgb("251b12")
                if selected:
                    panel(im, 0, 0, 26, 40, base=base, stitched=False)
                    im = im.crop((0, 0, 26, 32))
                    stitches(im, 5, 3, 16)
                    for x in range(3, 23):  # open into the panel: clear the side edges' lower rows into plain hide
                        for y in range(28, 32):
                            im.putpixel((x, y), shade(HIDE, round(GRAIN[x, y] * 7)))
                    for y in range(28, 32):
                        if col != 1:  # elsewhere the panel's own top edge carries on beside the tab
                            for x, c in ((0, OUTLINE), (1, EDGE), (2, INNER)):
                                im.putpixel((x, y), c if y == 28 else shade(HIDE, round(GRAIN[x, y] * 7)))
                        if col != 7:
                            for x, c in ((25, OUTLINE), (24, EDGE), (23, INNER)):
                                im.putpixel((x, y), c if y == 28 else shade(HIDE, round(GRAIN[x, y] * 7)))
                    if col != 1:
                        for k, c in enumerate((OUTLINE, EDGE_HI, INNER)):
                            im.putpixel((0, 29 + k), c)
                    if col != 7:
                        for k, c in enumerate((OUTLINE, EDGE_HI, INNER)):
                            im.putpixel((25, 29 + k), c)
                else:
                    panel(im, 0, 2, 26, 38, base=base, stitched=False)
                    im = im.crop((0, 0, 26, 32))
                    fill(im, 0, 28, 26, 4, (0, 0, 0, 0))
                if row == "bottom":
                    im = im.transpose(Image.Transpose.FLIP_TOP_BOTTOM)
                save(im, SP / f"container/creative_inventory/tab_{row}_{'selected' if selected else 'unselected'}_{col}.png")


def recipe_book_sprites():
    r = SP / "recipe_book"
    for name, selected in (("tab", False), ("tab_selected", True)):
        w = 35 if selected else 30
        im = new(41, 27)
        panel(im, 0, 0, w + 6, 26, base=HIDE if selected else rgb("251b12"), stitched=False)
        im = im.crop((0, 0, 35, 27))
        fill(im, w, 0, 35 - w, 27, (0, 0, 0, 0))
        if selected:
            stitches(im, 5, 3, 28)
        save(im, r / f"{name}.png")

    faces = {"slot_craftable": (rgb("6a5034"), GOLD), "slot_many_craftable": (rgb("6a5034"), GOLD),
             "slot_uncraftable": (rgb("3a251c"), rgb("8c3c2c")), "slot_many_uncraftable": (rgb("3a251c"), rgb("8c3c2c"))}
    for name, (face, line) in faces.items():
        im = new(25, 25)
        slot(im, 0, 0, 25, 25, base=face)
        frame(im, 1, 1, 23, 23, line)
        if "many" in name:
            fill(im, 21, 3, 2, 19, shade(face, 30))
        save(im, r / f"{name}.png")

    im = new(32, 32)
    panel(im, 0, 0, 32, 32, stitched=False)
    save(im, r / "overlay_recipe.png", nine(32, 32, 4))
    for kind in ("crafting_overlay", "furnace_overlay"):
        for suffix, face, line in (("", rgb("55422f"), EDGE_LO), ("_highlighted", rgb("7a5a36"), GOLD),
                                   ("_disabled", rgb("3a251c"), rgb("8c3c2c")), ("_disabled_highlighted", rgb("4c2e22"), rgb("d0664c"))):
            im = boxed(24, 24, face, line, line)
            frame(im, 1, 1, 22, 22, OUTLINE)
            if kind == "crafting_overlay":
                for k in (8, 15):
                    fill(im, k, 3, 1, 18, shade(face, -24))
                    fill(im, 3, k, 18, 1, shade(face, -24))
            else:
                frame(im, 8, 8, 8, 8, shade(face, -30))
            save(im, r / f"{kind}{suffix}.png")

    chest = [".oooooooo.", "owwwwwwwwo", "owwwwwwwwo", "oooommoooo", "oddddddddo", "oddddddddo", ".oooooooo."]
    stone = [".oooooooo.", "oggggggggo", "oggooooggo", "ogoffffogo", "ogoooooogo", "oggggggggo", ".oooooooo."]
    for prefix, icon in (("filter", chest), ("furnace_filter", stone)):
        for on in (False, True):
            for hot in (False, True):
                state = "hot" if hot else "on" if on else "normal"
                im = small_button(26, 16, state)
                pattern(im, 12, 4, icon, {"o": OUTLINE, "w": rgb("c08c50"), "d": rgb("8a5e30"), "m": GOLD,
                                          "g": rgb("9c968a"), "f": rgb("e08028")})
                mark = ["....##", "...##.", "##.#..", ".###..", "..#..."] if on else ["#...#", ".#.#.", "..#..", ".#.#.", "#...#"]
                pattern(im, 4, 5, mark, {"#": rgb("b6e48c") if on else rgb("e88a6a")})
                save(im, r / f"{prefix}_{'enabled' if on else 'disabled'}{'_highlighted' if hot else ''}.png")

    tri = ["#.......", "###.....", "#####...", "#######.", "########", "#######.", "#####...", "###.....", "#......."]
    for hot in (False, True):
        for name, flip in (("page_forward", False), ("page_backward", True)):
            im = new(12, 17)
            pattern(im, 2, 4, tri, {"#": GOLD if hot else NAME})
            if flip:
                im = im.transpose(Image.Transpose.FLIP_LEFT_RIGHT)
            save(im, r / f"{name}{'_highlighted' if hot else ''}.png")

    book = ["..oooooooo", ".obbbbbbpo", "obbbbbbbpo", "obbggbbbpo", "obbggbbbpo", "obbbbbbbpo", "obbbbbbbpo", "oppppppppo", ".ooooooooo"]
    for hot in (False, True):
        im = small_button(20, 18, "hot" if hot else "normal")
        pattern(im, 5, 4, book, {"o": OUTLINE, "b": rgb("9c5a30"), "p": rgb("e8d8b0"), "g": GOLD})
        save(im, r / f"button{'_highlighted' if hot else ''}.png")


def screen_sprites():
    save(boxed(18, 18, LEATHER, EDGE_HI, OUTLINE), SP / "statistics/header.png")


# ================= HideUi (the mod's own screens) =================

def hide_ui():
    """gui/sprites/hide/*: the same leather for screens drawn in code (client/ui/HideUi)."""
    # nine slice with tiled middle: the grain and the stitches repeat every 120 px
    im = new(132, 132)
    panel(im, 0, 0, 132, 132, grain=noise(512, 512, 120, 120))
    save(im, MOD / "sprites/hide/panel.png", nine(132, 132, 6))
    for name, face, line in (("row", rgb("2a1e13"), rgb("4a3826")), ("row_hover", rgb("5a4127"), rgb("c9a05c")),
                             ("row_off", rgb("241a12"), rgb("372a1d"))):
        im = new(64, 24)
        leather(im, 0, 0, 64, 24, face, 3, noise(64, 24, 56, 16, seed=21))
        frame(im, 0, 0, 64, 24, line, shade(line, -22))
        save(im, MOD / f"sprites/hide/{name}.png", nine(64, 24, 4))
    im = new(64, 24)
    slot(im, 0, 0, 64, 24, base=INSET)
    save(im, MOD / "sprites/hide/inset.png", nine(64, 24, 4))


# ================= the mod's container backgrounds =================

GREY = {"outline": (0, 0, 0), "panel": (198, 198, 198), "light": (255, 255, 255), "dark": (85, 85, 85),
        "slot": (139, 139, 139), "slot_dark": (55, 55, 55), "list": (110, 110, 110), "ghost": (125, 125, 125)}


def retheme_mod():
    """The mod's backgrounds are drawn in vanilla's greys by their own generators: swap each grey for its leather."""
    names = {v: k for k, v in GREY.items()}
    for path in sorted((MOD / "container").glob("*.png")):
        if path.name == "inventory_tall.png":
            continue
        src = Image.open(path).convert("RGBA")
        px = src.load()
        if not any(px[x, y][3] == 255 and px[x, y][:3] == GREY["panel"] for x in range(0, src.width, 3) for y in range(0, 60, 3)):
            continue  # already leather
        x0, y0, x1, y1 = src.getbbox()
        out = new(*src.size)
        panel(out, x0, y0, x1 - x0, y1 - y0)
        # slot interiors: grey enclosed by a slot's dark top-left corner (any other grey 139 is an arrow or an icon)
        pockets = set()
        for y in range(y0, y1):
            for x in range(x0, x1):
                if px[x, y][:3] != GREY["slot_dark"] or px[x + 1, y][:3] != GREY["slot_dark"] or px[x, y + 1][:3] != GREY["slot_dark"]:
                    continue
                if px[x - 1, y][:3] == GREY["slot_dark"] or px[x, y - 1][:3] == GREY["slot_dark"]:
                    continue
                w = h = 0
                while px[x + w, y][:3] == GREY["slot_dark"]:
                    w += 1
                while px[x, y + h][:3] == GREY["slot_dark"]:
                    h += 1
                pockets |= {(i, j) for i in range(x, x + w + 1) for j in range(y, y + h + 1)}
        for y in range(y0 + 3, y1 - 3):
            for x in range(x0 + 3, x1 - 3):
                r, g, b, a = px[x, y]
                kind = names.get((r, g, b)) if a == 255 else None
                if kind == "panel" or a == 0:
                    continue
                d = round(GRAIN[x, y] * 3)
                if kind == "light":
                    c = SLOT_LIGHT
                elif kind == "dark":
                    c = SLOT_MID
                elif kind == "slot_dark":
                    c = SLOT_DARK
                elif kind == "slot":
                    c = shade(SLOT, d) if (x, y) in pockets else ARROW
                elif kind == "list":
                    c = shade(INSET, d)
                elif kind == "ghost":
                    c = GHOST
                elif kind == "outline":
                    c = rgb("120c07")
                else:
                    c = (r, g, b, a)
                out.putpixel((x, y), c)
        # furnaces: the vanilla-shaped fire and arrow become ours, so the lit sprites sit exactly on them
        spots = {"kiln": ((56, 36), (79, 34)), "bloomery": ((56, 36), (79, 34)), "smelting_furnace": ((22, 70), None)}
        if path.stem in spots:
            flame, arrow = spots[path.stem]
            leather(out, flame[0], flame[1], 14, 15)
            stamp(out, flame[0], flame[1], FLAME_MASK)
            if arrow:
                leather(out, arrow[0], arrow[1], 24, 17)
                stamp(out, arrow[0], arrow[1], ARROW_BIG)
        out.save(path)


if __name__ == "__main__":
    containers()
    save(inventory(tall=True), MOD / "container/inventory_tall.png")
    misc_textures()
    widgets()
    tooltip_and_popups()
    container_sprites()
    recipe_book_sprites()
    screen_sprites()
    hide_ui()
    retheme_mod()
    print("gui textures written")
