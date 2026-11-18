# Textures for the mod's own tree species (aspen, alder, beech, linden): logs, stripped logs, planks,
# leaves (pre-coloured, no biome tint), saplings, doors and trapdoors.
# Planks and ring ends reuse vanilla's shared plank/ring layout recoloured (vanilla does the same per wood);
# bark, leaves, saplings, doors and trapdoors are drawn per species.
# usage: python gen_wood_textures.py <vanilla block texture dir> [preview png]
import math, os, random, sys
from PIL import Image

src = sys.argv[1]
prev = sys.argv[2] if len(sys.argv) > 2 else None
out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/mechanical/textures")

def van(name): return Image.open(f"{src}/{name}.png").convert("RGBA").crop((0, 0, 16, 16))
def shade(c, f): return tuple(max(0, min(255, int(round(v * f)))) for v in c[:3])
def mix(a, b, t): return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))
def ramp_at(ramp, t):
    """t in [0, len-1], linear between ramp colours"""
    t = max(0.0, min(len(ramp) - 1.0, t)); i = min(int(t), len(ramp) - 2)
    return mix(ramp[i], ramp[i + 1], t - i)

def noise(seed, px, py=None):
    """tileable 16x16 value noise with px x py random lattice, smooth-interpolated, 0..1"""
    py = py or px
    r = random.Random(seed); g = [[r.random() for _ in range(px)] for _ in range(py)]
    def sm(t): return t * t * (3 - 2 * t)
    res = [[0.0] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            fx, fy = x * px / 16, y * py / 16
            x0, y0 = int(fx), int(fy); tx, ty = sm(fx - x0), sm(fy - y0)
            a = g[y0 % py][x0 % px]; b = g[y0 % py][(x0 + 1) % px]
            c = g[(y0 + 1) % py][x0 % px]; d = g[(y0 + 1) % py][(x0 + 1) % px]
            res[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return res

def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))
def put(img, x, y, c): img.putpixel((x % 16, y % 16), tuple(c[:3]) + (255,))

def stamp(img, sprite, x0, y0, cols, wrap=True):
    for j, row in enumerate(sprite):
        for i, ch in enumerate(row):
            if ch == "." or ch == " ": continue
            x, y = x0 + i, y0 + j
            if not wrap and not (0 <= x < img.width and 0 <= y < img.height): continue
            img.putpixel((x % img.width, y % img.height), cols[ch][:3] + (255,))

# ------------------------------------------------------------------ species palettes
# planks: 7 tones dark -> light, slotted into vanilla's plank layout
SPECIES = {
    "aspen": dict(
        wood=[(158, 148, 120), (172, 162, 133), (186, 176, 146), (198, 189, 158), (210, 202, 171), (220, 213, 183), (230, 224, 196)],
        stripped_base="stripped_birch_log", stripped_range=(1.6, 6.3),
        leaf=dict(D=(66, 98, 26), S=(92, 130, 32), M=(122, 162, 42), L=(156, 190, 58), H=(194, 218, 96)),
        bark=[(112, 122, 104), (148, 158, 136), (172, 182, 160), (192, 200, 180), (208, 214, 196), (224, 228, 212)],
        metal=((58, 60, 58), (150, 154, 150))),
    "alder": dict(
        wood=[(132, 74, 42), (150, 87, 50), (165, 99, 57), (178, 111, 65), (191, 123, 73), (202, 134, 82), (212, 146, 93)],
        stripped_base="stripped_acacia_log", stripped_range=(1.2, 6.2),
        leaf=dict(D=(20, 50, 22), S=(30, 66, 28), M=(42, 86, 36), L=(58, 108, 46), H=(82, 132, 60),
                  x=(62, 40, 26), y=(98, 66, 40)),
        bark=[(36, 31, 28), (52, 45, 40), (67, 59, 52), (82, 73, 64), (98, 88, 77), (114, 104, 91)],
        metal=((44, 44, 46), (128, 128, 132))),
    "beech": dict(
        wood=[(152, 112, 92), (167, 126, 104), (181, 140, 115), (193, 152, 125), (204, 164, 135), (214, 175, 145), (223, 187, 157)],
        stripped_base="stripped_cherry_log", stripped_range=(1.4, 6.2),
        leaf=dict(D=(34, 88, 24), S=(50, 116, 32), M=(70, 146, 42), L=(98, 174, 56), H=(160, 216, 108)),
        bark=[(100, 103, 101), (124, 127, 124), (143, 146, 142), (159, 162, 157), (174, 176, 170), (189, 190, 185)],
        metal=((150, 110, 40), (214, 174, 76))),
    "linden": dict(
        wood=[(194, 178, 128), (205, 190, 140), (216, 202, 152), (225, 212, 163), (233, 222, 174), (240, 231, 185), (247, 240, 198)],
        stripped_base="stripped_pale_oak_log", stripped_range=(1.8, 6.4),
        leaf=dict(D=(34, 72, 30), S=(48, 94, 38), M=(66, 118, 48), L=(88, 142, 60), H=(118, 166, 76),
                  f=(232, 220, 132), F=(194, 180, 92)),
        bark=[(70, 64, 56), (90, 83, 73), (108, 100, 88), (124, 116, 102), (140, 131, 116), (157, 148, 131)],
        metal=((50, 50, 52), (140, 140, 144))),
}

# ------------------------------------------------------------------ bark (log side), all tile in both directions
def bark_aspen(p):
    b = p["bark"]; img = new()
    n1, n2 = noise("aspen1", 4, 8), noise("aspen2", 8, 16)
    for y in range(16):
        for x in range(16):
            v = 0.6 * n1[y][x] + 0.4 * n2[y][x]
            put(img, x, y, ramp_at(b, 2.0 + v * 3.6))
    r = random.Random("aspen")
    # faint horizontal lenticel dashes
    for x, y, l in [(1, 1, 2), (10, 3, 3), (5, 7, 2), (13, 11, 2), (0, 13, 3), (7, 14, 2), (12, 6, 2), (4, 10, 2),
                    (14, 0, 2), (8, 1, 2), (1, 8, 2), (6, 12, 2), (11, 15, 2), (15, 4, 2)]:
        for i in range(l): put(img, x + i, y, b[2] if i % 2 == 0 else b[3])
    dk, md, bl = (44, 44, 38), (86, 88, 76), (26, 26, 22)
    big = ["...m...", ".mmkdm.", "mkkkkkm", ".mdkmm.", "...m..."]
    small = [".m.", "mkd", ".m."]
    cols = {"m": md, "d": dk, "k": bl}
    for sx, sy in [(1, 2), (8, 9)]:
        stamp(img, big, sx, sy, cols)
        put(img, sx + 3, sy + 5, b[5]); put(img, sx + 2, sy - 1, b[2]); put(img, sx + 4, sy - 1, b[2])
    stamp(img, small, 12, 3, cols)
    for x, y in [(3, 12), (4, 12), (4, 13), (5, 13)]: put(img, x, y, bl)  # black scar
    put(img, 3, 13, md); put(img, 5, 12, md)
    return img

def wobble_line(seed, x0, amp=1):
    """x position per row for a vertical line that wanders but meets itself across the tile seam"""
    r = random.Random(seed); xs = [0.0]
    for _ in range(15): xs.append(xs[-1] + r.choice([-1, 0, 0, 0, 1]) * amp)
    drift = xs[-1] / 16
    return [x0 + round(xs[y] - drift * y) for y in range(16)]

def bark_alder(p):
    b = p["bark"]; img = new()
    n1, n2 = noise("alder1", 4, 4), noise("alder2", 8, 8)
    for y in range(16):
        for x in range(16):
            put(img, x, y, ramp_at(b, 2.2 + (0.6 * n1[y][x] + 0.4 * n2[y][x]) * 2.4))
    r = random.Random("alder")
    lines = [wobble_line(f"alder{i}", x0) for i, x0 in enumerate((1, 5, 9, 12))]
    # horizontal breaks between plates: (plate index, y)
    breaks = [(0, 4), (0, 11), (1, 7), (1, 14), (2, 2), (2, 10), (3, 6), (3, 13)]
    for k, (pi, y) in enumerate(breaks):
        a, c = lines[pi][y], lines[(pi + 1) % 4][y] + (16 if pi == 3 else 0)
        for x in range(a + 1, c):
            put(img, x, y, b[1]); put(img, x, y + 1, b[4] if (x + k) % 3 else b[5])
    for li, xs in enumerate(lines):
        for y in range(16):
            x = xs[y]
            deep = r.random() > 0.15
            put(img, x, y, b[0] if deep else b[1])
            put(img, x + 1, y, b[1] if deep else b[2])   # shadowed wall
            put(img, x - 1, y, b[2])
            if r.random() < 0.45: put(img, x + 2, y, b[4])  # lit plate edge
    for x, y in [(3, 1), (7, 9), (14, 3), (10, 13), (2, 8)]:  # pale warty lenticels
        put(img, x, y, b[5]); put(img, x + 1, y, b[4])
    return img

def bark_beech(p):
    b = p["bark"]; img = new()
    n1, n2, n3 = noise("beech1", 4, 4), noise("beech2", 8, 8), noise("beech3", 4, 4)
    lichen = (150, 160, 138)
    for y in range(16):
        for x in range(16):
            v = 0.7 * n1[y][x] + 0.3 * n2[y][x]
            c = ramp_at(b, 2.4 + v * 2.4)
            if n3[y][x] > 0.68: c = mix(c, lichen, 0.7)        # faint green-grey lichen patches
            elif n3[y][x] < 0.26: c = mix(c, b[5], 0.8)         # pale mottling
            put(img, x, y, c)
    for x, y, l in [(2, 3, 4), (11, 5, 3), (6, 10, 5), (13, 14, 3), (0, 15, 2)]:  # fine horizontal wrinkles
        for i in range(l): put(img, x + i, y, b[1] if i not in (0, l - 1) else b[2])
    # branch scar with its drooping 'moustache'
    stamp(img, ["..k..", ".m.m.", "m...m"], 9, 8, {"k": b[0], "m": b[1]})
    put(img, 11, 7, b[5])
    stamp(img, [".k.", "m.m"], 2, 12, {"k": b[0], "m": b[2]})
    return img

def bark_linden(p):
    b = p["bark"]; img = new()
    n1 = noise("linden1", 2, 8)
    for y in range(16):
        for x in range(16): put(img, x, y, ramp_at(b, 2.2 + n1[y][x] * 1.6))
    r = random.Random("linden")
    furrows = [[x0 + round(0.8 * math.sin(2 * math.pi * y / 16 + ph)) for y in range(16)]
               for x0, ph in ((0, 0.0), (3, 2.1), (7, 4.0), (10, 1.2), (13, 3.3))]
    for fi, xs in enumerate(furrows):
        nxt = furrows[(fi + 1) % 5]
        for y in range(16):
            x = xs[y]; w = (nxt[y] - x) % 16
            put(img, x + 1, y, b[4] if r.random() > 0.1 else b[5])      # lit left face of the ridge
            for i in range(2, w - 1): put(img, x + i, y, b[3] if r.random() > 0.3 else b[2])
            put(img, x + w - 1, y, b[2] if r.random() > 0.3 else b[1])  # shaded right face
            put(img, x, y, b[0] if r.random() > 0.2 else b[1])          # shallow furrow
    for x, y, l in [(4, 5, 2), (11, 12, 2), (1, 9, 2)]:                  # rare breaks in the ridges
        for i in range(l): put(img, x + i, y, b[1])
    return img

BARK = {"aspen": bark_aspen, "alder": bark_alder, "beech": bark_beech, "linden": bark_linden}

# ------------------------------------------------------------------ planks, ends, stripped
OAK_PLANKS = sorted({c[:3] for c in van("oak_planks").get_flattened_data()}, key=sum)

def recolour_planks(name, base, p):
    img = new(); v = van(base)
    for y in range(16):
        for x in range(16):
            c = v.getpixel((x, y))[:3]
            if c in OAK_PLANKS: put(img, x, y, p["wood"][OAK_PLANKS.index(c)])
            else: put(img, x, y, rim(name, x, y))
    return img

def flecks(img, p, seed, n):
    r = random.Random(seed)
    for _ in range(n):
        x, y = r.randrange(16), r.randrange(16)
        put(img, x, y, shade(img.getpixel((x, y)), 0.86))

def stripped_side(p):
    v = van(p["stripped_base"]).transpose(Image.FLIP_LEFT_RIGHT)
    ls = [sum(v.getpixel((x, y))[:3]) for y in range(16) for x in range(16)]
    lo, hi = min(ls), max(ls); a, b = p["stripped_range"]
    img = new()
    for y in range(16):
        for x in range(16):
            t = (sum(v.getpixel((x, y))[:3]) - lo) / (hi - lo)
            put(img, x, y, ramp_at(p["wood"], a + t * (b - a)))
    return img

# ------------------------------------------------------------------ leaves
LEAF_SPRITES = {
    "aspen": [[".LL.", "LHMS", "LMMS", ".SS."], ["LH.", "MMS", ".S."], [".L.", "LMS", ".S."]],
    "alder": [[".L.L.", "LHMMS", "LMMMS", ".MMS.", "..S.."], [".LM.", "LHMS", "MMMS", ".SS."]],
    "beech": [[".L.", "LHM", "LHM", "MMS", ".S."], [".LLM.", "LHHMS", ".MSS."], [".L.", "LHM", "MMS", ".S."]],
    "linden": [["LL.LM", "LHMMS", "LMMMS", ".MMS.", "..S.."], [".LM.", "LHMS", "MMMS", ".SS."]],
}
LEAF_HOLES = {"aspen": 0.34, "alder": 0.30, "beech": 0.18, "linden": 0.26}

def leaves(name, p):
    c = p["leaf"]; r = random.Random(name + "leaves")
    n = noise(name + "h", 3, 3); n2 = noise(name + "h2", 7, 7)  # lattices off the 4px leaf grid
    hv = sorted(0.6 * n[y][x] + 0.4 * n2[y][x] for y in range(16) for x in range(16))
    thr = hv[int(len(hv) * (1 - LEAF_HOLES[name]))]
    img = new()
    for y in range(16):
        for x in range(16):
            v = 0.6 * n[y][x] + 0.4 * n2[y][x]
            if v < thr: put(img, x, y, c["D"] if (x * 7 + y * 3) % 5 else c["S"])
    sprites = LEAF_SPRITES[name]
    for gy in range(4):                      # one leaf per 4x4 cell, jittered; wraps across the tile seam
        for gx in range(4):
            s = sprites[r.randrange(len(sprites))]
            stamp(img, s, gx * 4 + 2 * (gy % 2) + r.randint(-1, 1), gy * 4 + r.randint(-1, 1), c)  # staggered rows
    core = hv[int(len(hv) * (1 - 0.8 * LEAF_HOLES[name]))]
    for y in range(16):                      # re-open the hole cores the leaves were stamped over
        for x in range(16):
            if 0.6 * n[y][x] + 0.4 * n2[y][x] >= core: img.putpixel((x, y), (0, 0, 0, 0))
    if name == "alder":                      # last year's cones
        for x, y in [(3, 9), (12, 2)]: stamp(img, [".y", "yx", "xx", ".x"], x, y, c)
    if name == "linden":                     # pale flower clusters
        for x, y in [(9, 10), (2, 2)]: stamp(img, [".f.", "fFf", ".f."], x, y, c)
    return img

# ------------------------------------------------------------------ saplings: twigs + leaf sprites placed by hand
def line(img, x0, y0, x1, y1, col):
    n = max(abs(x1 - x0), abs(y1 - y0)) or 1
    for i in range(n + 1):
        put(img, round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), col)

SAPLINGS = {
    # (twigs [(x0,y0,x1,y1,tone)], leaves [(sprite idx, x, y)], trunk top y, base 2px from y)
    "aspen": ([(7, 9, 4, 6, 2), (7, 7, 10, 4, 2), (7, 11, 10, 9, 2)],
              [(2, 6, 0), (0, 5, 2), (1, 2, 5), (0, 9, 2), (1, 9, 7), (2, 3, 7), (1, 6, 10)], 2, 12),
    "alder": ([(7, 10, 3, 7, 1), (8, 8, 12, 5, 1), (7, 6, 5, 3, 1)],
              [(0, 5, 0), (1, 0, 4), (0, 10, 2), (1, 2, 8), (1, 10, 7), (0, 6, 4)], 3, 11),
    "beech": ([(7, 10, 3, 8, 2), (8, 8, 12, 6, 2), (7, 6, 4, 3, 2), (7, 12, 10, 10, 2)],
              [(1, 5, 0), (0, 1, 2), (1, 0, 7), (0, 11, 3), (1, 10, 6), (2, 7, 3), (2, 3, 9), (2, 9, 8), (1, 2, 11)], 2, 12),
    "linden": ([(7, 10, 3, 7, 1), (8, 9, 12, 7, 1), (7, 6, 10, 3, 1)],
               [(0, 5, 0), (0, 0, 3), (0, 10, 2), (1, 1, 7), (0, 10, 7), (1, 6, 4)], 3, 11),
}

def sapling(name, p):
    img = new(); b = p["bark"]; c = p["leaf"]
    twigs, lv, top, base = SAPLINGS[name]
    for x0, y0, x1, y1, t in twigs: line(img, x0, y0, x1, y1, b[t])
    for y in range(top, 16):
        put(img, 7, y, b[4] if name == "aspen" else b[3])
        if y >= base: put(img, 8, y, b[2] if name == "aspen" else b[1])
    if name == "aspen":
        for y in (6, 10, 13): put(img, 7, y, (60, 60, 52))     # dark lenticels on the white stem
    sp = LEAF_SPRITES[name]
    for i, x, y in lv: stamp(img, sp[i], x, y, c, wrap=False)
    if name == "alder": stamp(img, ["y", "x", "x"], 12, 10, c, wrap=False); stamp(img, ["y", "x"], 3, 12, c, wrap=False)
    return img

# ------------------------------------------------------------------ doors and trapdoors
# 0-6 wood tones, p/q vertical/horizontal plank grain, l lighter horizontal grain (raised panels),
# d darker grain, '.' hole (rim auto-shaded: top/left edge dark, bottom/right light), k/K handle metal
DOORS = {
    "aspen": ([
        "2222222222222222", "2555555555555532", "25pppppppppppp32", "25pppppppppppp32",
        "25ppppp..ppppp32", "25pppp....pppp32", "25ppp......ppp32", "25pp........pp32",
        "25pp........pp32", "25ppp......ppp32", "25pppp....pppp32", "25ppppp..ppppp32",
        "25pppppppppppp32", "25pppppppppppp32", "2544444444444432", "2533333333333332"], [
        "25pppppppppppkK2", "25pppppppppppk32", "25ppppppppp15332", "25pppppppp153p32",
        "25ppppppp153pp32", "25pppppp153ppp32", "25ppppp153pppp32", "25pppp153ppppp32",
        "25ppp153pppppp32", "25pp153ppppppp32", "25p153pppppppp32", "25153ppppppppp32",
        "2553pppppppppp32", "25pppppppppppp32", "2544444444444432", "2222222222222222"]),
    "alder": ([
        "2222222222222222", "2555555555555532", "25pppppppppppp32", "25pppppppppppp32",
        "25pppp.43.pppp32", "25ppp..43..ppp32", "25pp...43...pp32", "25pp44444444pp32",
        "25pp33343333pp32", "25pp...43...pp32", "25ppp..43..ppp32", "25pppp.43.pppp32",
        "25pppppppppppp32", "25pppppppppppp32", "2544444444444432", "2533333333333332"], [
        "25pppppppppppkK2", "25p5553pp5553k32", "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32",
        "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32",
        "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32", "25p5ll2pp5ll2p32",
        "25p3222pp3222p32", "25pppppppppppp32", "2544444444444432", "2222222222222222"]),
    "beech": ([
        "2222222222222222", "2555555555555532", "25pppppppppppp32", "25pppp....pppp32",
        "25pppp....pppp32", "25pppp....pppp32", "25pppp4444pppp32", "25pppp....pppp32",
        "25pppp....pppp32", "25pppp4444pppp32", "25pppp....pppp32", "25pppp....pppp32",
        "25pppppppppppp32", "25pppppppppppp32", "2544444444444432", "2533333333333332"], [
        "25pppppppppppkK2", "25p5555555553k32", "25p5hhhhhhhh2p32", "25p5hhhhhhhh2p32",
        "25p3222222222p32", "25pppppppppppp32", "25p5555555553p32", "25p5hhhhhhhh2p32",
        "25p5hhhhhhhh2p32", "25p3222222222p32", "25pppppppppppp32", "25p5555555553p32",
        "25p5hhhhhhhh2p32", "25p3222222222p32", "25pppppppppppp32", "2222222222222222"]),
    "linden": ([
        "2222222222222222", "2555555555555532", "25pppppppppppp32", "25pppppppppppp32",
        "25ppp..pp..ppp32", "25pp........pp32", "25pp........pp32", "25pp........pp32",
        "25ppp......ppp32", "25pppp....pppp32", "25ppppp..ppppp32", "25pppppppppppp32",
        "25pppppppppppp32", "25pppppppppppp32", "2544444444444432", "2533333333333332"], [
        "25pppppppppppkK2", "25p5555555553k32", "25p5hhhhhhhh2p32", "25p5hcchhcch2p32",
        "25p5chhcchhc2p32", "25p5chhhhhhc2p32", "25p5hchhhhch2p32", "25p5hhchhchh2p32",
        "25p5hhhcchhh2p32", "25p5hhhhhhhh2p32", "25p5hhhhhhhh2p32", "25p5hhhhhhhh2p32",
        "25p3222222222p32", "25pppppppppppp32", "2544444444444432", "2222222222222222"]),
}

TRAPDOORS = {  # inner 12x12, framed automatically
    "aspen": ["pppppppppppp", "pppppppppppp", "ppppp..ppppp", "pppp....pppp", "ppp......ppp", "pp........pp",
              "pp........pp", "ppp......ppp", "pppp....pppp", "ppppp..ppppp", "pppppppppppp", "pppppppppppp"],
    "alder": ["qqqqqqqqqqqq", "qqqq.43.qqqq", "qqq..43..qqq", "qq...43...qq", "qq...43...qq", "qq44444444qq",
              "qq33343333qq", "qq...43...qq", "qq...43...qq", "qqq..43..qqq", "qqqq.43.qqqq", "qqqqqqqqqqqq"],
    "beech": ["pppppppppppp", "pppppppppppp", "pp..p..p..pp", "pp..p..p..pp", "pppppppppppp", "pp..p..p..pp",
              "pp..p..p..pp", "pppppppppppp", "pp..p..p..pp", "pp..p..p..pp", "pppppppppppp", "pppppppppppp"],
    "linden": ["pppppppppppp", "pppppppppppp", "ppp..pp..ppp", "pp........pp", "pp........pp", "pp........pp",
               "ppp......ppp", "pppp....pppp", "ppppp..ppppp", "pppppppppppp", "pppppppppppp", "pppppppppppp"],
}

ITEM_DOORS = {  # 10x14, placed at (3, 2) like vanilla
    "aspen": ["5555555552", "54pppppp32", "54pp..pp32", "54p....p32", "54p....p32", "54pp..pp32", "54pppppp32",
              "544444kK32", "54ppp15332", "54pp153p32", "54p153pp32", "54153ppp32", "5453pppp32", "3222222222"],
    "alder": ["5555555552", "54pppppp32", "54p.43.p32", "54p4444p32", "54p.43.p32", "54pppppp32", "5444444432",
              "54pppppk32", "5455355332", "545l25l232", "545l25l232", "545l25l232", "5432232232", "3222222222"],
    "beech": ["5555555552", "54pppppp32", "54pp..pp32", "54pp44pp32", "54pp..pp32", "54pp44pp32", "54pp..pp32",
              "544444kK32", "5455555332", "545hhhh232", "5432222232", "5455555332", "5432222232", "3222222222"],
    "linden": ["5555555552", "54pppppp32", "54p.pp.p32", "54......32", "54p....p32", "54pp..pp32", "54pppppp32",
               "544444kK32", "5455555332", "545hhhh232", "545hcch232", "545hhhh232", "5432222232", "3222222222"],
}

def paint(rows, p, planks, x0=0, y0=0, img=None):
    """paint an ASCII design; grain chars sample the species planks (rotated for vertical boards)"""
    h, w = len(rows), len(rows[0])
    img = img or Image.new("RGBA", (w, h), (0, 0, 0, 0))
    vert = planks.rotate(90)
    wd = p["wood"]; m0, m1 = p["metal"]
    wood = [shade(wd[0], 0.7), shade(wd[0], 0.84), wd[0], wd[2], wd[3], wd[5], wd[6]]  # frame tones, darker than planks
    for y, row in enumerate(rows):
        assert len(row) == w, (row, len(row))
        for x, ch in enumerate(row):
            X, Y = x0 + x, y0 + y
            if ch == ".": continue
            if ch in "0123456": c = wood[int(ch)]
            # grain is pulled halfway to a flat tone so frames and holes read clearly, like vanilla doors
            elif ch == "p": c = mix(vert.getpixel((X % 16, Y % 16)), wd[3], 0.5)
            elif ch == "q": c = mix(planks.getpixel((X % 16, Y % 16)), wd[3], 0.5)
            elif ch == "l": c = mix(vert.getpixel(((X + 5) % 16, Y % 16)), wd[5], 0.55)
            elif ch == "h": c = mix(planks.getpixel((X % 16, (Y + 5) % 16)), wd[5], 0.55)
            elif ch == "d": c = shade(mix(vert.getpixel((X % 16, Y % 16)), wd[3], 0.5), 0.8)
            elif ch == "c": c = shade(planks.getpixel((X % 16, Y % 16)), 0.72)
            elif ch == "k": c = m0
            elif ch == "K": c = m1
            else: raise ValueError(ch)
            # rim around holes
            if ch in "pqlhd":
                def hole(dx, dy):
                    i, j = x + dx, y + dy
                    return 0 <= j < h and 0 <= i < w and rows[j][i] == "."
                if hole(0, 1) or hole(1, 0): c = wood[1]
                elif hole(0, -1) or hole(-1, 0): c = wood[5]
                elif hole(1, 1): c = wood[2]
            img.putpixel((X, Y), tuple(c[:3]) + (255,))
    return img

def trapdoor(name, p, planks):
    inner = TRAPDOORS[name]
    rows = ["5" * 15 + "2", "5" + "4" * 13 + "32"]
    rows += ["54" + r + "32" for r in inner]
    rows += ["5" + "3" * 14 + "2", "3" + "2" * 15]
    return paint(rows, p, planks)

def item_door(name, p, planks):
    img = new()
    return paint(ITEM_DOORS[name], p, planks, 3, 2, img)

# ------------------------------------------------------------------ assemble
_rims = {}
def rim(name, x, y): return _rims[name].getpixel((x, y))[:3]

written = []
def save(img, rel):
    path = os.path.join(out, rel); img.save(path); written.append((rel, img))

for name, p in SPECIES.items():
    bark = BARK[name](p); _rims[name] = bark
    save(bark, f"block/{name}_log.png")
    save(recolour_planks(name, "oak_log_top", p), f"block/{name}_log_top.png")
    strip = stripped_side(p)
    if name == "beech": flecks(strip, p, "beechs", 14)
    save(strip, f"block/stripped_{name}_log.png")
    _rims[name] = strip
    save(recolour_planks(name, "stripped_oak_log_top", p), f"block/stripped_{name}_log_top.png")
    planks = recolour_planks(name, "oak_planks", p)
    if name == "beech": flecks(planks, p, "beechp", 10)
    save(planks, f"block/{name}_planks.png")
    save(leaves(name, p), f"block/{name}_leaves.png")
    save(sapling(name, p), f"block/{name}_sapling.png")
    top_rows, bottom_rows = DOORS[name]
    save(paint(top_rows, p, planks), f"block/{name}_door_top.png")
    save(paint(bottom_rows, p, planks), f"block/{name}_door_bottom.png")
    save(trapdoor(name, p, planks), f"block/{name}_trapdoor.png")
    save(item_door(name, p, planks), f"item/{name}_door.png")

if prev:
    S, per = 8, 11
    sheet = Image.new("RGBA", (per * (16 * S + 8) + 8, 4 * (16 * S + 8) + 8), (118, 118, 118, 255))
    for i, (rel, img) in enumerate(written):
        big = img.resize((16 * S, 16 * S), Image.NEAREST)
        sheet.paste(big, (8 + (i % per) * (16 * S + 8), 8 + (i // per) * (16 * S + 8)), big)
    sheet.save(prev)
print(f"ok, {len(written)} textures")
