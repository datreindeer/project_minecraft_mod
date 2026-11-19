"""The Ancient Oak (AncientOak.java, worldgen/AncientOakFeature.java): blockstates, models, tags, worldgen placement, lang,
and the hand-painted 16x16 textures of the tree, the Offering Altar and the five mythic items (plus the Antler Crown's
worn layer). Run: python scripts/gen_ancient_oak.py [contact_sheet.png]"""
import json
import math
import sys
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
TEX = A / "textures"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def h(x, y, salt=0):
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def hexc(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def grid(rows, pal, size=None):
    """Pixel art from a character grid; '.' is transparent."""
    hgt, wid = len(rows), max(len(r) for r in rows)
    img = Image.new("RGBA", size or (wid, hgt), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                img.putpixel((x, y), c if len(c) == 4 else c + (255,))
    return img


# ---------------------------------------------------------------- block textures

BARK = [hexc(c) for c in ("2b2117", "3b2d1e", "4d3b27", "5e4a32", "70593c")]
MOSS = [hexc(c) for c in ("3f5a22", "557530", "6d8f3a")]
RUNE = hexc("9ff0c8")


FURROW, FURROW_EDGE = hexc("231c16"), hexc("332a22")
PLATE = [hexc(c) for c in ("40362d", "4b4036", "554a3f", "60544a", "6b5f53")]
MOSS_TINT = hexc("4f6a2e")


def furrows(y):
    """x of the three furrow lines at row y. Periodic in y (period 16) and shared by every variant, so the ridges run on
    unbroken across the tops, bottoms and sides of all the trunk's blocks."""
    t = 2 * math.pi * y / 16
    return [(k * 16 / 3 + 0.5 + 0.85 * math.sin(t + k * 2.1) + 0.35 * math.sin(2 * t + k * 1.3)) % 16 for k in range(3)]


def circ(a, b):
    d = abs(a - b) % 16
    return min(d, 16 - d)


def bark_side(variant=0):
    """Old oak bark that tiles seamlessly: deep wandering vertical furrows between broad plates (lit from the left), a
    faint continuous moss streak in one furrow, and per-variant details (cracks, a knot, lichen) kept off the edges."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        fs = furrows(y)
        for x in range(16):
            cx = x + 0.5
            d = min(circ(cx, f) for f in fs)
            k = min(range(3), key=lambda i: circ(cx, fs[i]))
            if d < 0.75:
                c = FURROW
            elif d < 1.35:
                c = FURROW_EDGE
            else:
                # which side of the nearest furrow: the plate's left edge catches the light
                right_of = ((cx - fs[k]) % 16) < 8
                v = 3 if right_of and d < 2.2 else 1 if not right_of and d < 2.2 else 2
                grain = h(x, y // 3, 40) - 0.5  # faint vertical grain, small so no blotches
                c = shade(PLATE[v], 1 + grain * 0.06)
            if k == 1 and d < 1.35:  # the mossy furrow
                c = mix(c, MOSS_TINT, 0.25 + 0.15 * math.sin(2 * math.pi * y / 16))
            img.putpixel((x, y), c + (255,))

    def px(x, y, c):
        img.putpixel((x % 16, y), c + (255,))

    # variant details, away from the tile edges (rows 3..12) so neighbours of any variant still line up
    if variant == 1:
        for x0, y0 in [(3, 5), (10, 9)]:  # short horizontal cracks across a plate
            for i in range(3):
                px(x0 + i, y0, FURROW_EDGE)
            px(x0 + 1, y0 + 1, PLATE[0])
    elif variant == 2:
        for dx, dy in [(0, 0), (1, 0), (0, 1), (1, 1), (-1, 0), (2, 1)]:  # an old knot with a dark eye
            px(8 + dx, 7 + dy, FURROW_EDGE)
        px(8, 7, FURROW)
        px(9, 8, FURROW)
        for x, y in [(4, 4), (5, 4), (4, 5), (12, 11), (13, 11)]:  # grey-green lichen dots
            img.putpixel((x, y), mix(img.getpixel((x, y))[:3], hexc("7f8f62"), 0.55) + (255,))
    return img


def rune_bark():
    """Bark variant 0 with a glyph carved deep into a plate, glowing green-gold from inside (the block gives light 6)."""
    img = bark_side(0)
    core, glow, halo = hexc("c9ffd6"), hexc("6fe0a0"), hexc("3f8a5f")
    glyph = [(8, 2), (8, 3), (8, 4), (8, 5), (8, 6), (8, 7), (8, 8), (8, 9), (8, 10), (8, 11), (8, 12), (8, 13),
             (7, 4), (6, 3), (9, 6), (10, 5), (7, 9), (6, 10), (9, 11), (10, 12), (10, 13)]
    gs = set(glyph)
    for x, y in glyph:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                q = (x + dx, y + dy)
                if q not in gs and 0 <= q[1] < 16:
                    img.putpixel((q[0] % 16, q[1]), mix(img.getpixel((q[0] % 16, q[1]))[:3], halo, 0.45) + (255,))
    for x, y in glyph:
        img.putpixel((x, y), (core if y in (4, 5, 8, 9, 12) else glow) + (255,))
    return img


def glowcap():
    """A little cluster of glowing mushrooms (cross model): pale stems, softly lit green-gold caps."""
    rows = [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "....ccc.........",
        "...cGggc........",
        "...gggGg..ccc...",
        "....ss...cgGgc..",
        "....ss...ggggg..",
        "....ss....ss.cc.",
        "..cc.ss...ss.gg.",
        "..gg.ss...ss.s..",
        "...s.ss...ss.s..",
        "...s.ss...ss.s..",
    ]
    return grid(rows, {"c": hexc("d8f5a0"), "G": hexc("f6ffd0"), "g": hexc("9ccf5a"), "s": hexc("e6dcc4")})


def log_top():
    img = Image.new("RGBA", (16, 16))
    rings = [hexc(c) for c in ("6b4a2a", "8a6438", "a07a48", "7a5631", "94703f")]
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            d = math.hypot(dx, dy) + math.sin(math.atan2(dy, dx) * 3) * 0.35
            if max(abs(dx), abs(dy)) > 6.6:
                c = BARK[1 + (1 if h(x, y, 5) > 0.5 else 0)]
            elif d < 1.6:
                c = hexc("4e3420")
            else:
                c = rings[int(d * 1.15) % len(rings)]
                c = shade(c, 0.94 + h(x, y, 6) * 0.1)
            img.putpixel((x, y), c + (255,))
    # a glowing heart ring and a radial crack
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if 2.3 < d < 3.0 and h(x, y, 7) > 0.35:
                img.putpixel((x, y), mix(img.getpixel((x, y))[:3], RUNE, 0.35) + (255,))
    for i in range(2, 6):
        img.putpixel((8 + i, 8 + i - (i > 3)), hexc("4a301c") + (255,))
    return img


def leaves():
    """Old dark oak leaves (pre-coloured, no biome tint): lobed clumps with holes and the odd golden-lit leaf."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    greens = [hexc(c) for c in ("1f3d14", "2c5219", "3a6a22", "4c802b", "6b9a35")]
    for x in range(16):
        for y in range(16):
            n = h(x, y, 11)
            lobe = (h(x // 3, y // 3, 12) * 0.6 + h((x + 1) // 2, (y + 1) // 2, 13) * 0.4)
            if n < 0.1 or lobe < 0.2:
                continue  # holes
            v = int(lobe * 3.8 + (n - 0.5) * 1.4)
            if (x + y) % 5 == 0 and n > 0.6:
                v += 1  # leaf highlights
            c = greens[max(0, min(4, v))]
            img.putpixel((x, y), c + (255,))
    for x, y in [(3, 4), (12, 10), (7, 13)]:
        img.putpixel((x, y), hexc("b9c95a") + (255,))
    return img


STONE = [hexc(c) for c in ("3d3f3a", "54574f", "6a6d63", "80837a", "999b90")]


def altar_side():
    """Rough grey stone, moss in the cracks, a root wrapping round it, and a carved spiral."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            v = 2 + (1 if h(x // 2, y // 2, 21) > 0.55 else 0) - (1 if h(x, y, 22) > 0.8 else 0)
            if y in (3, 11) or (x in (5, 12) and y < 11 and y > 3):
                v = 0 if h(x, y, 23) > 0.3 else 1  # joints between the stones
            img.putpixel((x, y), STONE[max(0, min(4, v))] + (255,))
            if v <= 1 and h(x, y, 24) > 0.5:
                img.putpixel((x, y), MOSS[1] + (255,))
    # top rim lighter
    for x in range(16):
        img.putpixel((x, 0), STONE[4] + (255,))
        img.putpixel((x, 1), STONE[3] + (255,))
    # a root snaking across the lower stones
    for x in range(16):
        y = 13 + round(math.sin(x * 0.7) * 1.3)
        img.putpixel((x, y), BARK[3] + (255,))
        img.putpixel((x, y + 1), BARK[1] + (255,))
        if x % 5 == 2:
            img.putpixel((x, y - 1), BARK[4] + (255,))
    # carved spiral, faintly lit
    for x, y in [(8, 5), (9, 5), (10, 6), (10, 7), (9, 8), (8, 8), (7, 7), (7, 6), (8, 6), (9, 7)]:
        img.putpixel((x, y), mix(STONE[0], RUNE, 0.45) + (255,))
    return img


def altar_top():
    """The top: stone slab with a worn round bowl in the middle and moss round the edge."""
    img = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            v = 3 + (1 if h(x, y, 31) > 0.7 else 0) - (1 if h(x // 2, y // 2, 32) > 0.75 else 0)
            if d < 4.2:
                v = 1 if d > 3.3 else 0 if d > 2.4 else 1  # bowl: dark rim falling to a lighter worn floor
                c = STONE[v] if d > 2.4 else mix(STONE[1], hexc("6b5a3a"), 0.4)
            else:
                c = STONE[max(0, min(4, v))]
            if d > 6.6 and h(x, y, 33) > 0.55:
                c = MOSS[1 if h(x, y, 34) > 0.5 else 0]
            img.putpixel((x, y), c + (255,))
    # rune ticks round the bowl
    for a in range(0, 360, 45):
        x, y = round(7.5 + math.cos(math.radians(a)) * 5.2), round(7.5 + math.sin(math.radians(a)) * 5.2)
        img.putpixel((x, y), mix(STONE[1], RUNE, 0.5) + (255,))
    return img


# ---------------------------------------------------------------- item textures

THUNDERSTONE = [
    "................",
    ".......aSS......",
    ".....aSssSS.....",
    "....aSsttssS....",
    "...aSstwbtssS...",
    "...Sstttwtssc...",
    "..aStttwbtscCh..",
    "..Sstwbttt.cHl..",
    "..SstbwttS.Hlh..",
    "...SsstwS.Hlh...",
    "....SSSS.Hlh....",
    ".......Hlh......",
    "......Hlh.......",
    ".....Hlh........",
    "....Hlh.........",
    "...Hh...........",
]
THUNDER_PAL = {"S": hexc("2e333b"), "s": hexc("4a535e"), "t": hexc("6d7a89"), "a": hexc("8b9aab"),
               "w": hexc("e8f6ff"), "b": hexc("7cc4f5"), "h": hexc("7a5530"), "H": hexc("4a321b"), "l": hexc("a57a46"),
               "c": hexc("cdb98f"), "C": hexc("8e7952"), " ": hexc("2e333b")}

MISTLETOE = [
    "................",
    "......cc........",
    ".....c..c.......",
    ".....c..c.......",
    "......cc........",
    ".......C........",
    "......gCg.......",
    "...LLgsCsgLL....",
    "..LllLgsgLllL...",
    "..LlllLsLlllL...",
    "...LllLsLllL....",
    "....LLwsWLL.....",
    "......wsw.......",
    ".....WwsWw......",
    "......w.W.......",
    "................",
]
MISTLETOE_PAL = {"c": hexc("8a3b2a"), "C": hexc("b5553c"), "g": hexc("6f8a3c"), "s": hexc("8c9a52"),
                 "L": hexc("5e7a32"), "l": hexc("a3bd62"), "w": hexc("f2f0dc"), "W": hexc("cfd0b4")}

AMBER = [
    "................",
    "................",
    "......dddd......",
    "....ddooood.....",
    "...doyyoooodd...",
    "..doyhyooooood..",
    "..doyyoowowood..",
    "..dooooowkwood..",
    "..doooookkkood..",
    "..doooookkkood..",
    "..doooooqkqood..",
    "...doooqokoqd...",
    "...ddooooooodd..",
    ".....ddddddd....",
    "................",
    "................",
]
AMBER_PAL = {"d": hexc("8a4a0c"), "o": hexc("d98a1e"), "y": hexc("f5c04a"), "h": hexc("fff1b8"),
             "k": hexc("4a2a10"), "w": hexc("f0b562"), "q": hexc("8a5418")}

HEART = [
    "................",
    "................",
    "...bbb....bbb...",
    "..bwwwb..bwwwb..",
    ".bwrrrwbbwrrrwb.",
    ".bwrRRrwwrRRrwb.",
    ".bwrRggRRggRrwb.",
    ".bwrRgGGGGgRrwb.",
    "..bwrRgGGgRrwb..",
    "...bwrRggRrwb...",
    "....bwrRRrwb....",
    ".....bwrrwb.....",
    "......bwwb......",
    ".......bb.......",
    "................",
    "................",
]
HEART_PAL = {"b": hexc("3b2a18"), "w": hexc("6e4a28"), "r": hexc("9a6a3a"), "R": hexc("b9874c"),
             "g": hexc("c8d65e"), "G": hexc("f2f5a8")}

CROWN = [
    "................",
    ".a..........a...",
    ".a.a......a.a...",
    "..aa.a..a.aa....",
    "..Aa.a..a.aA....",
    "...Aaa..aaA.....",
    "....AA..AA......",
    "....A....A......",
    "...bbbbbbbbb....",
    "..bBBlBBBlBBb...",
    "..bBgBBBBBgBb...",
    "...bbbbbbbbb....",
    "................",
    "................",
    "................",
    "................",
]
CROWN_PAL = {"a": hexc("e3d6b4"), "A": hexc("b9a57c"), "b": hexc("4e3520"), "B": hexc("7a5532"),
             "l": hexc("5f8a34"), "g": hexc("d9b84a")}


def shifted(img, dy):
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, dy))
    return out


def crown_layer():
    """Worn layer (64x32 humanoid): a woven bark-and-leather band round the brow with antlers painted rising from the
    temples up the sides and over the top, on both the head and the slightly larger hat layer."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    band, band_d, leaf, gold = hexc("7a5532"), hexc("4e3520"), hexc("5f8a34"), hexc("d9b84a")
    ant, ant_d = hexc("e3d6b4"), hexc("b9a57c")
    # head side faces: u 0..32, v 8..16 (right, front, left, back); band on rows 11..12 of each face (brow height)
    for u in range(0, 32):
        for v in (11, 12):
            c = band if (u + v) % 3 else band_d
            img.putpixel((u, v), c + (255,))
        img.putpixel((u, 10), band_d + (255,))
    for u in (10, 13, 26, 29):  # leaves and a gold bead on the front and back of the band
        img.putpixel((u, 11), leaf + (255,))
    img.putpixel((12, 12), gold + (255,))
    img.putpixel((11, 12), gold + (255,))
    # antlers on the hat layer side faces (u 32..64, v 8..16): right face 32..40, left face 48..56
    for base in (32, 48):
        tines = [(3, 15), (3, 14), (4, 13), (4, 12), (5, 11), (5, 10), (6, 9), (6, 8), (3, 10), (2, 9), (2, 8), (5, 12), (6, 12), (7, 11)]
        for x, y in tines:
            img.putpixel((base + x, y), (ant if y % 2 else ant_d) + (255,))
    # and on the hat top (u 40..48, v 0..8): two beams curving back
    for x, y in [(1, 6), (1, 5), (2, 4), (2, 3), (6, 6), (6, 5), (5, 4), (5, 3), (2, 2), (5, 2), (1, 1), (6, 1)]:
        img.putpixel((40 + x, y), ant + (255,))
    return img


def spirit_bloom():
    """Spirit Bloom (cross model): a slender stem with two leaves and a nodding, star-like flower of pale cyan-white
    petals round a bright heart, plus a closed bud."""
    rows = [
        "................",
        "....p.p.........",
        "...pPpPp........",
        "..pPwHwPp.......",
        "...PwHwP........",
        "..pPpwpPp.......",
        "....pSp.....b...",
        ".....s.....bBb..",
        ".....s......b...",
        "..ll.s......s...",
        ".lLlls.....s....",
        "..l.ls....s.....",
        ".....s...s.lL...",
        ".....sl.s.lLl...",
        ".....sLls.......",
        "......ss........",
    ]
    return grid(rows, {"p": hexc("8fd9e8"), "P": hexc("c6f3fb"), "w": hexc("f4fdff"), "H": hexc("fffbe0"), "S": hexc("6fb7a0"),
                       "s": hexc("4f8a5a"), "l": hexc("3f7a45"), "L": hexc("67a865"), "b": hexc("9fdfe6"), "B": hexc("e4fbff")})


def glow_moss():
    """Glow Moss (carpet top): dense dark moss in soft clumps, sprinkled with luminous cyan-green specks. Tiles seamlessly."""
    img = Image.new("RGBA", (16, 16))
    base = [hexc(c) for c in ("2d4a22", "38592a", "456b31", "527a38")]
    for x in range(16):
        for y in range(16):
            clump = (math.sin(2 * math.pi * x / 8 + 0.7) + math.sin(2 * math.pi * y / 8 + 1.9) + math.sin(2 * math.pi * (x + y) / 16)) / 3
            v = int((clump + 1) * 1.6 + (h(x, y, 50) - 0.5) * 0.9)
            img.putpixel((x, y), base[max(0, min(3, v))] + (255,))
    for x, y in [(2, 3), (7, 1), (12, 4), (5, 8), (10, 10), (14, 13), (1, 12), (8, 14), (13, 7)]:
        img.putpixel((x, y), hexc("b8ffd8") + (255,))
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = ((x + dx) % 16, (y + dy) % 16)
            img.putpixel(q, mix(img.getpixel(q)[:3], hexc("6fdc9a"), 0.5) + (255,))
    return img


def textures():
    out = {}
    out["block/ancient_oak_log"] = bark_side(0)
    out["block/ancient_oak_log_2"] = bark_side(1)
    out["block/ancient_oak_log_3"] = bark_side(2)
    out["block/ancient_oak_rune_log"] = rune_bark()
    out["block/glowcap"] = glowcap()
    out["block/spirit_bloom"] = spirit_bloom()
    out["block/glow_moss"] = glow_moss()
    out["block/ancient_oak_log_top"] = log_top()
    out["block/ancient_oak_leaves"] = leaves()
    out["block/offering_altar_side"] = altar_side()
    out["block/offering_altar_top"] = altar_top()
    out["item/thunderstone"] = grid(THUNDERSTONE, THUNDER_PAL)
    out["item/mistletoe_charm"] = grid(MISTLETOE, MISTLETOE_PAL)
    out["item/ancient_amber"] = grid(AMBER, AMBER_PAL)
    out["item/heart_of_the_oak"] = grid(HEART, HEART_PAL)
    out["item/antler_crown"] = shifted(grid(CROWN, CROWN_PAL), 2)
    out["entity/equipment/humanoid/antler_crown"] = crown_layer()
    return out


# ---------------------------------------------------------------- models and data

def cube(f, t, tex, faces=("down", "up", "north", "south", "east", "west"), uv=None):
    return {"from": f, "to": t, "faces": {d: dict({"texture": tex}, **({"uv": uv} if uv else {})) for d in faces}}


def data():
    # logs: axis variants, each picking one of three seamless bark textures at random; wood = bark on every face
    bark = {"": "ancient_oak_log", "_2": "ancient_oak_log_2", "_3": "ancient_oak_log_3"}
    for suffix, tex in bark.items():
        side = f"mechanical:block/{tex}"
        w(A / f"models/block/ancient_oak_log{suffix}.json", {"parent": "minecraft:block/cube_column",
            "textures": {"end": "mechanical:block/ancient_oak_log_top", "side": side}})
        w(A / f"models/block/ancient_oak_log{suffix}_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
            "textures": {"end": "mechanical:block/ancient_oak_log_top", "side": side}})
        w(A / f"models/block/ancient_oak_wood{suffix}.json", {"parent": "minecraft:block/cube_column", "textures": {"end": side, "side": side}})

    def axes(model, horizontal):
        return {"axis=y": [{"model": f"mechanical:block/{model}{s}", "weight": wt} for s, wt in (("", 3), ("_2", 2), ("_3", 2))],
                "axis=z": [{"model": f"mechanical:block/{model}{s}{horizontal}", "x": 90, "weight": wt} for s, wt in (("", 3), ("_2", 2), ("_3", 2))],
                "axis=x": [{"model": f"mechanical:block/{model}{s}{horizontal}", "x": 90, "y": 90, "weight": wt} for s, wt in (("", 3), ("_2", 2), ("_3", 2))]}
    w(A / "blockstates/ancient_oak_log.json", {"variants": axes("ancient_oak_log", "_horizontal")})
    w(A / "blockstates/ancient_oak_wood.json", {"variants": axes("ancient_oak_wood", "")})
    w(A / "models/block/ancient_oak_rune_log.json", {"parent": "minecraft:block/cube_column",
        "textures": {"end": "mechanical:block/ancient_oak_log_top", "side": "mechanical:block/ancient_oak_rune_log"}})
    w(A / "models/block/ancient_oak_rune_log_horizontal.json", {"parent": "minecraft:block/cube_column_horizontal",
        "textures": {"end": "mechanical:block/ancient_oak_log_top", "side": "mechanical:block/ancient_oak_rune_log"}})
    w(A / "blockstates/ancient_oak_rune_log.json", {"variants": {
        "axis=y": {"model": "mechanical:block/ancient_oak_rune_log"},
        "axis=z": {"model": "mechanical:block/ancient_oak_rune_log_horizontal", "x": 90},
        "axis=x": {"model": "mechanical:block/ancient_oak_rune_log_horizontal", "x": 90, "y": 90}}})
    # glowcap: cross model, item uses the block texture, drops itself
    w(A / "models/block/glowcap.json", {"parent": "minecraft:block/cross", "textures": {"cross": "mechanical:block/glowcap"}})
    w(A / "blockstates/glowcap.json", {"variants": {"": {"model": "mechanical:block/glowcap"}}})
    w(A / "models/item/glowcap.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:block/glowcap"}})
    w(A / "items/glowcap.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/glowcap"}})
    w(A / "models/block/spirit_bloom.json", {"parent": "minecraft:block/cross", "textures": {"cross": "mechanical:block/spirit_bloom"}})
    w(A / "blockstates/spirit_bloom.json", {"variants": {"": {"model": "mechanical:block/spirit_bloom"}}})
    w(A / "models/block/glow_moss.json", {"parent": "minecraft:block/carpet", "textures": {"wool": "mechanical:block/glow_moss"}})
    w(A / "blockstates/glow_moss.json", {"variants": {"": {"model": "mechanical:block/glow_moss"}}})
    for b in ("spirit_bloom", "glow_moss"):
        w(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": f"mechanical:{b}"}], "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"mechanical:blocks/{b}"})
    w(D / "mechanical/loot_table/blocks/glowcap.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": "mechanical:glowcap"}], "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": "mechanical:blocks/glowcap"})
    w(A / "models/block/ancient_oak_leaves.json", {"parent": "minecraft:block/cube_all",
        "textures": {"all": "mechanical:block/ancient_oak_leaves"}})
    w(A / "blockstates/ancient_oak_leaves.json", {"variants": {"": {"model": "mechanical:block/ancient_oak_leaves"}}})

    # branches (8 px) and twigs (4 px): a core plus an arm per connected side, like chorus plants
    for name, size in (("ancient_oak_branch", 8), ("ancient_oak_twig", 4)):
        lo, hi = 8 - size / 2, 8 + size / 2
        tex = {"bark": "mechanical:block/ancient_oak_log", "particle": "mechanical:block/ancient_oak_log"}
        w(A / f"models/block/{name}_core.json", {"textures": tex, "elements": [cube([lo, lo, lo], [hi, hi, hi], "#bark", uv=[lo, lo, hi, hi])]})
        # arm pointing north (towards -z); the blockstate rotates it to the other sides
        arm = {"from": [lo, lo, 0], "to": [hi, hi, lo], "faces": {
            "north": {"texture": "#bark", "uv": [lo, lo, hi, hi], "cullface": "north"},
            "east": {"texture": "#bark", "uv": [0, lo, lo, hi]}, "west": {"texture": "#bark", "uv": [0, lo, lo, hi]},
            "up": {"texture": "#bark", "uv": [lo, 0, hi, lo]}, "down": {"texture": "#bark", "uv": [lo, 0, hi, lo]}}}
        w(A / f"models/block/{name}_side.json", {"textures": tex, "elements": [arm]})
        side = f"mechanical:block/{name}_side"
        rot = {"north": {}, "east": {"y": 90}, "south": {"y": 180}, "west": {"y": 270}, "up": {"x": 270}, "down": {"x": 90}}
        parts = [{"apply": {"model": f"mechanical:block/{name}_core"}}]
        parts += [{"when": {d: "true"}, "apply": dict({"model": side}, **r)} for d, r in rot.items()]
        w(A / f"blockstates/{name}.json", {"multipart": parts})

    # altar: foot, pillar, slab top (matches the Java shape)
    st = {"side": "mechanical:block/offering_altar_side", "top": "mechanical:block/offering_altar_top",
          "particle": "mechanical:block/offering_altar_side"}

    def slab(f, t):
        return {"from": f, "to": t, "faces": {
            "down": {"texture": "#side", "uv": [f[0], f[2], t[0], t[2]]}, "up": {"texture": "#top", "uv": [f[0], f[2], t[0], t[2]]},
            **{d: {"texture": "#side", "uv": [f[0], 16 - t[1], t[0], 16 - f[1]]} for d in ("north", "south", "east", "west")}}}
    w(A / "models/block/offering_altar.json", {"parent": "minecraft:block/block", "textures": st,
        "elements": [slab([1, 0, 1], [15, 3, 15]), slab([3, 3, 3], [13, 9, 13]), slab([1, 9, 1], [15, 12, 15])]})
    w(A / "blockstates/offering_altar.json", {"variants": {"active=false": {"model": "mechanical:block/offering_altar"},
                                                            "active=true": {"model": "mechanical:block/offering_altar"}}})

    # the mythic items
    for item in ("thunderstone", "mistletoe_charm", "ancient_amber", "heart_of_the_oak", "antler_crown"):
        parent = "minecraft:item/handheld" if item == "thunderstone" else "minecraft:item/generated"
        w(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": f"mechanical:item/{item}"}})
        w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    w(A / "equipment/antler_crown.json", {"layers": {"humanoid": [{"texture": "mechanical:antler_crown"}]}})

    # tags: the tree's own tag (never logs/leaves, so felling and leaf decay leave it alone); immune to withers, dragons
    # and other features; altar offerings (the Giant Antler comes from another part of the mod, so it is optional)
    tree = [f"mechanical:{b}" for b in ("ancient_oak_log", "ancient_oak_wood", "ancient_oak_branch", "ancient_oak_twig",
                                         "ancient_oak_leaves", "offering_altar", "ancient_oak_rune_log")]
    w(D / "mechanical/tags/block/ancient_oak.json", {"values": tree})
    for tag in ("wither_immune", "dragon_immune", "features_cannot_replace"):
        w(D / f"minecraft/tags/block/{tag}.json", {"replace": False, "values": ["#mechanical:ancient_oak"]})
    w(D / "mechanical/tags/item/altar_offerings.json", {"values": [{"id": "mechanical:giant_antler", "required": False}]})

    # worldgen: a structure (AncientOakStructure), one chance per 24x24-chunk cell, forest and dark forest only;
    # placed after the vegetation (it clears the biome's trees under itself)
    w(D / "mechanical/tags/worldgen/biome/has_structure/ancient_oak.json", {"values": ["minecraft:forest", "minecraft:dark_forest"]})
    w(D / "mechanical/worldgen/structure/ancient_oak.json", {"type": "mechanical:ancient_oak",
        "biomes": "#mechanical:has_structure/ancient_oak", "spawn_overrides": {}, "step": "top_layer_modification"})
    w(D / "mechanical/worldgen/structure_set/ancient_oak.json", {"structures": [{"structure": "mechanical:ancient_oak", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "salt": 717450131, "spacing": 24, "separation": 12}})

    lang_path = A / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    lang.update({
        "block.mechanical.ancient_oak_log": "Ancient Oak Log",
        "block.mechanical.ancient_oak_wood": "Ancient Oak Wood",
        "block.mechanical.ancient_oak_branch": "Ancient Oak Branch",
        "block.mechanical.ancient_oak_twig": "Ancient Oak Twig",
        "block.mechanical.ancient_oak_leaves": "Ancient Oak Leaves",
        "block.mechanical.offering_altar": "Offering Altar",
        "block.mechanical.ancient_oak_rune_log": "Ancient Oak Rune Bark",
        "block.mechanical.glowcap": "Glowcap",
        "block.mechanical.spirit_bloom": "Spirit Bloom",
        "block.mechanical.glow_moss": "Glow Moss",
        "message.mechanical.offering_altar.waiting": "The altar waits for a worthy offering.",
        "message.mechanical.offering_altar.busy": "The old forest is listening...",
        "item.mechanical.thunderstone": "Thunderstone",
        "item.mechanical.thunderstone.lore": "Struck from the sky, it calls the sky back down.",
        "item.mechanical.mistletoe_charm": "Mistletoe Charm",
        "item.mechanical.mistletoe_charm.lore": "Carried close, it mends wounds and turns poison.",
        "item.mechanical.ancient_amber": "Ancient Amber",
        "item.mechanical.ancient_amber.lore": "A sleeping insect; beasts take you for a quiet thing.",
        "item.mechanical.heart_of_the_oak": "Heart of the Oak",
        "item.mechanical.heart_of_the_oak.lore": "It still beats. Saplings wake at its touch.",
        "item.mechanical.antler_crown": "Antler Crown",
        "item.mechanical.antler_crown.lore": "See as the deer see; walk among them unnoticed.",
    })
    lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def sheet(path, texs):
    """Contact sheet: every texture at 8x, leaves/bark also tiled 3x3, for eyeballing."""
    scale, pad = 8, 8
    items = list(texs.items())
    cell = 64 * scale // 2
    img = Image.new("RGBA", (pad + (cell + pad) * 4, pad + (cell + pad) * ((len(items) + 3) // 4 + 2)), (120, 140, 120, 255))
    for i, (name, t) in enumerate(items):
        x, y = pad + (i % 4) * (cell + pad), pad + (i // 4) * (cell + pad)
        if t.size == (16, 16):
            tile = Image.new("RGBA", (48, 48))
            for a in range(3):
                for b in range(3):
                    tile.paste(t, (a * 16, b * 16))
            big = (t if "item" in name else tile).resize((cell, cell), Image.NEAREST)
        else:
            big = t.resize((cell, cell // 2), Image.NEAREST)
        img.alpha_composite(big, (x, y))
    trunk = Image.new("RGBA", (64, 64))
    names = ["block/ancient_oak_log", "block/ancient_oak_log_2", "block/ancient_oak_log_3"]
    for a in range(4):
        for b in range(4):
            name = "block/ancient_oak_rune_log" if (a, b) == (2, 1) else names[int(h(a, b, 77) * 3)]
            trunk.paste(texs[name], (a * 16, b * 16))
    img.alpha_composite(trunk.resize((cell * 2, cell * 2), Image.NEAREST), (pad, pad + ((len(items) + 3) // 4) * (cell + pad)))
    img.save(path)


if __name__ == "__main__":
    texs = textures()
    for name, t in texs.items():
        p = TEX / f"{name}.png"
        p.parent.mkdir(parents=True, exist_ok=True)
        t.save(p)
    data()
    if len(sys.argv) > 1:
        sheet(sys.argv[1], texs)
    print(f"ancient oak ok ({len(texs)} textures)")
