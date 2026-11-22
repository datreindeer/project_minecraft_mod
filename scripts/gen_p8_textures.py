"""Textures for the steel and jewellery phase.

Items (transparent, outlined, lit from the top left):
- steel_hearth_ring: a riveted blue-grey steel hoop seen at an angle
- tool_loops: a leather strap with three sewn loops and a small steel buckle
- steel_frame_pack: a big leather backpack on a steel frame, two straps and a flap
- gold_ring_blank / gold_chain: matte cast gold
- gold_ring / gold_necklace: polished gold with an EMPTY socket; gold_ring_gem / gold_necklace_gem hold only the
  greyscale gem (tinted in game) and line up exactly with that socket
Blocks (opaque): steel_casting_table_top/_side, jewelers_bench_top/_side
Run: python scripts/gen_p8_textures.py
"""
import math
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"

# O outline, K..H dark -> light, r rivet head; like vanilla iron but cooler blue-grey
STEEL = {"O": "1c2027", "K": "2b3038", "D": "4a525e", "M": "6d7887", "L": "94a0b0", "H": "c3ccd8", "r": "e4eaf2"}
# leather from gen_satchel_texture.py; T strap shadow, s stitch
LEATHER = {"O": "3b1f12", "T": "53291a", "D": "6e3a1f", "M": "8f4f2b", "L": "a8653a", "H": "c47f4c", "s": "e0c9a0"}
# steel under other letters, for mixing with leather
STEEL_MIX = {"k": "2b3038", "c": "6d7887", "b": "94a0b0", "a": "c3ccd8"}
# like vanilla gold ingot; W is the polish highlight
GOLD = {"O": "4a2c06", "D": "7a4a0a", "M": "b07a12", "L": "e0a91c", "H": "f8d248", "W": "fff3a8"}
# greyscale gem (tinted in game): w highlight -> z shade
GEM = {"w": "ffffff", "x": "dcdcdc", "y": "b4b4b4", "z": "8c8c8c"}


def rgb(hx):
    return int(hx[0:2], 16), int(hx[2:4], 16), int(hx[4:6], 16)


def draw(grid, pal):
    """Character grid -> image; characters missing from `pal` stay transparent."""
    assert len(grid) == 16 and all(len(r) == 16 for r in grid), grid
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(grid):
        for x, c in enumerate(row):
            if c in pal:
                img.putpixel((x, y), rgb(pal[c]) + (255,))
    return img


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def h(x, y, salt):
    """Deterministic per-pixel hash 0..1."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 10000) / 10000


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    a, b, f = cols[i], cols[i + 1], t - i
    return tuple(round(a[k] + (b[k] - a[k]) * f) for k in range(3)) + (255,)


# ---------------------------------------------------------------- items

HEARTH_RING = [
    "................",
    "................",
    "....OOOOOOOO....",
    "..OOLHLLHLLLOO..",
    ".OLHrLOOOOLrLMO.",
    ".OLLOKKKKKKOLMO.",
    "OLLOK......KOMDO",
    "OLrO........OrDO",
    "OLMDO......ODMDO",
    "ODMMDOOOOOODMDDO",
    "ODLLHLLHLLHLLMDO",
    ".OKDrDMMMDrDDKO.",
    "..OOKKDDDDKKOO..",
    "....OOOOOOOO....",
    "................",
    "................",
]

TOOL_LOOPS = [
    "................",
    "................",
    "................",
    "OOOOOOOOOOOO....",
    "OHLOOHLOOHLO....",
    "OLTOOLTOOLTO....",
    "OLTOOLTOOLTO....",
    "OMTOOMTOOMTOOOOO",
    "OOOOOOOOOOOOaabO",
    "OHLsLHLsLHLOaTcO",
    "OMMDMMDMMDMOaTcO",
    "OOOOOOOOOOOObckO",
    "...........OOOOO",
    "................",
    "................",
    "................",
]

FRAME_PACK = [
    ".kkkkkkkkkkkkkk.",
    ".kaabbbbbbbbbck.",
    ".kakOOOOOOOOkck.",
    ".kaOHHLLLLLMOck.",
    ".kaOHLTLLTLMOck.",
    ".kaOLLTLLTLMOck.",
    ".kbOLLaLLaLDOck.",
    ".kbOOOTOOTOOOck.",
    ".kbOMMTMMTMDOck.",
    ".kbOMLTMMTMDOck.",
    ".kbOMMTMMTMDOck.",
    ".kbOMsTssTsDOck.",
    ".kbODMTMMTDDOck.",
    ".kcODDTDDTDDOkk.",
    ".kkOOOOOOOOOOk..",
    "..k..........k..",
]

RING_BLANK = [  # matte: no W, a few rough dark pixels
    "................",
    "................",
    "................",
    ".....OOOOOO.....",
    "...OOLHLDMLOO...",
    "..OLHOOOOOOMDO..",
    ".OLDO......OMDO.",
    ".OLO........OMO.",
    ".OMO........ODO.",
    ".OMDO......ODDO.",
    "..OMDOOOOOODDO..",
    "...OODMDLDDOO...",
    ".....OOOOOO.....",
    "................",
    "................",
    "................",
]

RING = [  # w/x/y/z = gem socket (transparent here, drawn in gold_ring_gem)
    "....OO....OO....",
    "....OHwwxyMO....",
    "....OLwxyyDO....",
    "....OLxyyzDO....",
    ".....OMzzDO.....",
    "...OOLHMMDOOO...",
    "..OWLOOOOOOMDO..",
    ".OWLO......OMDO.",
    ".OHO........ODO.",
    ".OLO........ODO.",
    ".OLMO......ODDO.",
    "..OLMOOOOOODDO..",
    "...OOMWLLMDOO...",
    ".....OOOOOO.....",
    "................",
    "................",
]

NECKLACE = [
    ".O............O.",
    "OHO..........ODO",
    "OMO..........OMO",
    ".OHO........ODO.",
    ".OMO........OMO.",
    "..OHO......ODO..",
    "..OMO......OMO..",
    "...OHO....ODO...",
    "...OMO....OMO...",
    "....OHOOOODO....",
    "....OLwwxyMO....",
    "....OHwxyyDO....",
    "....OLxyyzDO....",
    ".....OMzzDO.....",
    "......OMDO......",
    ".......OO.......",
]


def gold_chain():
    """A coil of links round an oval plus a trailing end: face-on links (rim with a hole) alternate with edge-on
    links (a short bar)."""
    shades = [rgb(GOLD[k]) for k in "DMLH"]
    col, holes = {}, set()
    centres = [(7.5 + 5 * math.cos(a), 6.5 + 3.4 * math.sin(a))
               for a in (2 * math.pi * i / 10 + 0.3 for i in range(10))] + [(12.6, 11.6), (13, 13)]
    for i, (fx, fy) in enumerate(centres):
        cx, cy = round(fx), round(fy)
        if i % 2 == 0:
            holes.add((cx, cy))
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if dx or dy:
                        col[(cx + dx, cy + dy)] = ramp(shades, 0.55 - 0.3 * (dx + dy) / 2)
        else:
            for dx in (0, 1):
                col[(cx + dx, cy)] = ramp(shades, 0.75 - 0.4 * dx)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for (x, y), c in col.items():
        img.putpixel((x, y), c)
    out = rgb(GOLD["O"]) + (255,)
    for y in range(16):
        for x in range(16):
            if (x, y) not in col and (x, y) not in holes and any(
                    (x + dx, y + dy) in col for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                img.putpixel((x, y), out)
    return img


# ---------------------------------------------------------------- blocks

STEEL_RAMP = [rgb(STEEL[k]) for k in "KDML"]
WOOD_RAMP = [(77, 51, 25), (107, 74, 38), (138, 100, 54), (168, 126, 72), (196, 154, 94)]


def recess(px, mask, inner, shadow, lit):
    """Pressed-in cavity: dark floor, shadowed top/left wall, lit bottom/right wall."""
    for (x, y) in mask:
        if (x, y - 1) not in mask or (x - 1, y) not in mask:
            px[x, y] = shadow
        elif (x, y + 1) not in mask or (x + 1, y) not in mask:
            px[x, y] = lit
        else:
            px[x, y] = inner


def rivet(px, x, y):
    px[x, y] = rgb(STEEL["r"]) + (255,)
    px[x + 1, y + 1] = rgb(STEEL["K"]) + (255,)


def casting_top():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = ramp(STEEL_RAMP, 0.25 + 0.25 * h(x, y, 1))
            if x in (0, 15) or y in (0, 15):
                px[x, y] = ramp(STEEL_RAMP, 0.6 if x == 0 or y == 0 else 0.05)
    ring = {(x, y) for x in range(16) for y in range(16) if 1.6 <= math.hypot(x - 5.5, y - 7.5) <= 3.6}
    bar = {(x, y) for x in range(10, 13) for y in range(3, 13)}
    for m in (ring, bar):
        recess(px, m, rgb(STEEL["K"]) + (255,), rgb(STEEL["O"]) + (255,), rgb(STEEL["M"]) + (255,))
    for x, y in ((1, 1), (13, 1), (1, 13), (13, 13)):
        rivet(px, x, y)
    return img


def casting_side():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            if y == 0:
                c = ramp(STEEL_RAMP, 0.9)                      # lit top edge
            elif y < 10:
                c = ramp(STEEL_RAMP, 0.35 + 0.2 * h(x, y, 2))  # plate
            elif y == 10:
                c = ramp(STEEL_RAMP, 0.1)                      # rail
            elif x in (1, 2, 3, 12, 13, 14):
                c = ramp(STEEL_RAMP, 0.3 + 0.15 * h(x, y, 3) - (0.15 if x in (3, 14) else 0))  # legs
            else:
                c = rgb(STEEL["O"]) + (255,)                   # dark gap under the table
            px[x, y] = c
    for x, y in ((1, 2), (13, 2), (1, 7), (13, 7)):
        rivet(px, x, y)
    return img


def wood_planks(px, salt):
    """Horizontal oak-ish planks, 4 px high, with seams and grain streaks."""
    for y in range(16):
        for x in range(16):
            t = 0.45 + 0.15 * math.sin((x + 3 * (y // 4)) * 0.9 + y % 4) * 0.5 + 0.2 * h(x, y, salt)
            if y % 4 == 3:
                t = 0.12                                       # seam
            elif y % 4 == 0:
                t += 0.12                                      # lit plank top
            px[x, y] = ramp(WOOD_RAMP, t)


def bench_top():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    wood_planks(px, 4)
    for x, y in ((2, 5), (3, 6), (4, 6), (6, 1), (7, 2), (3, 9), (4, 9)):  # scratches
        px[x, y] = ramp(WOOD_RAMP, 0.95)
    lea = {k: rgb(v) + (255,) for k, v in LEATHER.items()}
    for y in range(9, 15):                                    # leather pad, bottom right
        for x in range(9, 15):
            edge = x in (9, 14) or y in (9, 14)
            px[x, y] = lea["O"] if edge else lea["H" if x == 10 or y == 10 else "M" if (x + y) % 3 else "L"]
    for x in (11, 13):
        px[x, 12] = lea["s"]
    return img


def bench_side():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    wood_planks(px, 5)
    gap = (40, 26, 12, 255)
    for y in range(5, 12):                                    # drawer: dark gap round a lighter front
        for x in range(3, 13):
            if x in (3, 12) or y in (5, 11):
                px[x, y] = gap
            else:
                px[x, y] = ramp(WOOD_RAMP, 0.6 + 0.15 * h(x, y, 6) + (0.15 if y == 6 else 0))
    px[7, 8], px[8, 8] = rgb(GOLD["H"]) + (255,), rgb(GOLD["M"]) + (255,)  # knob
    px[7, 9], px[8, 9] = rgb(GOLD["D"]) + (255,), gap
    for x in range(16):
        px[x, 15] = ramp(WOOD_RAMP, 0.05)
    return img


if __name__ == "__main__":
    out = {
        "item/steel_hearth_ring": draw(HEARTH_RING, STEEL),
        "item/tool_loops": draw(TOOL_LOOPS, {**LEATHER, **STEEL_MIX}),
        "item/steel_frame_pack": draw(FRAME_PACK, {**LEATHER, **STEEL_MIX}),
        "item/gold_ring_blank": draw(RING_BLANK, GOLD),
        "item/gold_chain": gold_chain(),
        "item/gold_ring": draw(RING, GOLD),
        "item/gold_ring_gem": draw(RING, GEM),
        "item/gold_necklace": draw(NECKLACE, GOLD),
        "item/gold_necklace_gem": draw(NECKLACE, GEM),
        "block/steel_casting_table_top": casting_top(),
        "block/steel_casting_table_side": casting_side(),
        "block/jewelers_bench_top": bench_top(),
        "block/jewelers_bench_side": bench_side(),
    }
    for rel, img in out.items():
        save(img, rel)

    # self-check on the written files
    loaded = {rel: Image.open(TEX / f"{rel}.png").convert("RGBA") for rel in out}
    for rel, img in loaded.items():
        assert img.size == (16, 16), rel
        a = [img.getpixel((x, y))[3] for x in range(16) for y in range(16)]
        assert all(v == 255 for v in a) if rel.startswith("block/") else (0 in a and 255 in a), rel
    for base in ("gold_ring", "gold_necklace"):
        b, g = loaded[f"item/{base}"], loaded[f"item/{base}_gem"]
        gem = [(x, y) for x in range(16) for y in range(16) if g.getpixel((x, y))[3]]
        assert gem and all(b.getpixel(p)[3] == 0 for p in gem), base
        assert all(len(set(g.getpixel(p)[:3])) == 1 for p in gem), base  # greyscale
        print(f"{base}: gem {len(gem)} px, all inside the transparent socket")
    print(f"wrote {len(out)} textures, all 16x16")
