# Textures for the mod's own tree species (aspen, alder, beech, linden, willow, ash, hazel, pine, yew): logs, stripped logs, planks,
# leaves (pre-coloured, no biome tint), saplings, doors and trapdoors.
# Planks and ring ends reuse vanilla's shared plank/ring layout recoloured (vanilla does the same per wood);
# bark, leaves, saplings, doors and trapdoors are drawn per species. Also Beechnuts, Roasted Beechnuts and Pine Resin.
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

# ------------------------------------------------------------------ bark (log side)
# Rough barks take a vanilla bark's structure and repaint it with the species ramp by brightness rank, so they sit
# next to vanilla logs without hand-stamped patterns repeating in a grid. Smooth barks get soft noise plus a few
# lenticels/scars scattered at random (never on a grid).
def lum(c): return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
def desat(c, s):
    g = lum(c); return tuple(max(0, min(255, int(round(g + (v - g) * s)))) for v in c[:3])

def remap(pattern, ramp, lo, hi, sat=1.0):
    """pattern's k-th darkest opaque pixel -> ramp at quantile lo..hi (transparent pixels stay transparent)"""
    px = sorted(((i, c) for i, c in enumerate(pattern.get_flattened_data()) if c[3] > 0), key=lambda t: lum(t[1]))
    img = new(); n = len(px)
    for k, (i, _) in enumerate(px):
        put(img, i % 16, i // 16, desat(ramp_at(ramp, (lo + (hi - lo) * k / max(1, n - 1)) * (len(ramp) - 1)), sat))
    return img

def scatter(img, seed, n, cols, shapes, min_d, opaque_only=False):
    """n marks at random spots at least min_d apart (measured across the tile seam)"""
    r = random.Random(seed); pts = []
    def d2(a, b): return min((a[0] - b[0]) % 16, (b[0] - a[0]) % 16) ** 2 + min((a[1] - b[1]) % 16, (b[1] - a[1]) % 16) ** 2
    for _ in range(2000):
        if len(pts) == n: break
        q = (r.randrange(16), r.randrange(16))
        if all(d2(q, o) >= min_d * min_d for o in pts): pts.append(q)
    for x, y in pts:
        for dx, dy, k in r.choice(shapes):
            if not opaque_only or img.getpixel(((x + dx) % 16, (y + dy) % 16))[3]: put(img, x + dx, y + dy, cols[k])
    return img

def smooth_bark(seed, b, lo, hi):
    n1, n2 = noise(seed, 4), noise(seed + "f", 8); r = random.Random(seed); img = new()
    for y in range(16):
        for x in range(16):
            v = 0.7 * n1[y][x] + 0.18 * n2[y][x] + 0.12 * r.random()
            put(img, x, y, ramp_at(b, (lo + (hi - lo) * v) * (len(b) - 1)))
    return img

def bark_aspen(p):
    b = p["bark"]; img = smooth_bark("aspen", b, 0.45, 0.95)
    dark, mid = (40, 42, 38), (92, 96, 86)
    scatter(img, "aspen_l", 5, {1: dark, 2: mid}, [[(0, 0, 1), (1, 0, 1), (-1, 0, 2), (2, 0, 2), (0, 1, 2)],
            [(0, 0, 1), (1, 0, 2)], [(0, 0, 1), (1, 0, 1), (0, -1, 2), (1, 1, 2)]], 6)
    # a few big dark diamond lenticels, aspen's signature
    return scatter(img, "aspen_d", 3, {1: dark, 2: (70, 72, 66)},
                   [[(0, 0, 1), (1, 0, 1), (-1, 0, 2), (2, 0, 2), (0, 1, 2), (1, 1, 2), (0, -1, 2), (1, -1, 2)]], 7)

def bark_beech(p):
    b = p["bark"]; img = smooth_bark("beech", b, 0.3, 0.85)
    return scatter(img, "beech_l", 4, {1: shade(b[0], 1.1), 2: b[1]},
                   [[(0, 0, 1), (1, 0, 1), (2, 0, 2)], [(-1, 0, 2), (0, 0, 1), (1, 0, 2)]], 6)

# species: (vanilla bark to borrow, ramp quantile lo, hi, saturation)
ROUGH = {"alder": ("dark_oak_log", 0.05, 0.9, 0.85), "linden": ("oak_log", 0.05, 0.95, 0.9),
         "willow": ("dark_oak_log", 0.05, 0.95, 0.85), "ash": ("spruce_log", 0.15, 0.95, 0.8),
         "hazel": ("oak_log", 0.1, 0.95, 0.8), "pine": ("dark_oak_log", 0.05, 0.95, 0.7),
         "yew": ("spruce_log", 0.0, 0.9, 0.7)}
BARK = {"aspen": bark_aspen, "beech": bark_beech}
BARK.update({k: (lambda p, v=v: remap(van(v[0]), p["bark"], v[1], v[2], v[3])) for k, v in ROUGH.items()})


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

# vanilla's per-pixel leaf noise in the species greens, ~45% of the holes filled with the darkest tone for a denser
# crown; berries/flowers/catkins scattered sparsely at random spots
LEAF_BASE = {"beech": "birch_leaves", "willow": "birch_leaves", "pine": "spruce_leaves", "yew": "spruce_leaves"}
LEAF_ACCENTS = {  # (count, shapes using the species' leaf colour keys, min distance)
    "alder": (3, [[(0, 0, "y")], [(0, 0, "x"), (0, 1, "x")]], 7),                  # last year's cones
    "linden": (3, [[(0, 0, "f"), (1, 0, "F")], [(0, 0, "f"), (0, 1, "F")]], 7),    # pale flowers
    "hazel": (3, [[(0, 0, "C"), (0, 1, "c")]], 7),                                 # hanging catkins
    "yew": (4, [[(0, 0, "r"), (1, 0, "R")], [(0, 0, "r"), (0, 1, "R")]], 6),       # red arils
}

def leaves(name, p):
    c = p["leaf"]
    img = remap(van(LEAF_BASE.get(name, "oak_leaves")), [c[k] for k in "DSMLH"], 0.0, 1.0)
    # keep vanilla's holes: filling them made our trees look solid next to vanilla ones
    if name in LEAF_ACCENTS:
        n, shapes, d = LEAF_ACCENTS[name]
        scatter(img, name + "acc", n, c, shapes, d, opaque_only=True)
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

# ------------------------------------------------------------------ willow, ash, hazel, pine, yew (gen_trees.py)
SPECIES.update({
    "willow": dict(   # pale creamy wood, grey furrowed bark, narrow light silvery-green leaves
        wood=[(170, 156, 120), (183, 169, 132), (195, 181, 144), (206, 193, 156), (216, 204, 168), (225, 214, 180), (234, 224, 192)],
        stripped_base="stripped_birch_log", stripped_range=(1.4, 6.2),
        leaf=dict(D=(62, 92, 40), S=(88, 122, 52), M=(118, 152, 68), L=(152, 182, 92), H=(196, 216, 144)),
        bark=[(48, 48, 44), (70, 70, 64), (92, 91, 84), (112, 111, 102), (132, 130, 120), (152, 150, 140)],
        metal=((54, 54, 54), (146, 146, 146))),
    "ash": dict(      # very pale, almost white strong wood, light grey diamond-ridged bark, mid-green leaflets
        wood=[(196, 186, 160), (207, 198, 173), (217, 209, 185), (226, 219, 196), (234, 228, 206), (241, 236, 216), (248, 244, 227)],
        stripped_base="stripped_birch_log", stripped_range=(1.8, 6.5),
        leaf=dict(D=(36, 76, 28), S=(52, 100, 36), M=(72, 128, 46), L=(98, 154, 60), H=(140, 186, 88)),
        bark=[(84, 86, 82), (108, 110, 105), (130, 132, 126), (150, 152, 145), (168, 170, 162), (186, 188, 180)],
        metal=((40, 40, 42), (120, 120, 124))),
    "hazel": dict(    # pinkish-pale wood, smooth glossy copper-brown bark with pale lenticels, round leaves and catkins
        wood=[(170, 128, 106), (183, 141, 118), (195, 153, 129), (205, 164, 139), (214, 175, 150), (222, 186, 161), (230, 197, 173)],
        stripped_base="stripped_cherry_log", stripped_range=(1.6, 6.3),
        leaf=dict(D=(40, 80, 26), S=(56, 106, 34), M=(78, 134, 44), L=(106, 160, 58), H=(146, 192, 86),
                  c=(214, 196, 98), C=(160, 138, 62)),
        bark=[(56, 40, 32), (78, 56, 42), (100, 72, 52), (122, 88, 62), (142, 104, 74), (164, 124, 92)],
        metal=((48, 48, 50), (136, 136, 140))),
    "pine": dict(     # yellowish resinous wood, orange flaky bark plates, blue-green needles
        wood=[(166, 120, 70), (180, 133, 80), (193, 146, 90), (204, 158, 100), (214, 170, 110), (223, 181, 121), (231, 192, 132)],
        stripped_base="stripped_spruce_log", stripped_range=(1.6, 6.4),
        leaf=dict(D=(24, 52, 40), S=(34, 70, 52), M=(48, 92, 66), L=(68, 116, 84), H=(104, 148, 112)),
        bark=[(58, 38, 30), (90, 56, 38), (136, 76, 44), (170, 98, 54), (196, 122, 68), (220, 150, 90)],
        metal=((44, 44, 46), (128, 128, 132))),
    "yew": dict(      # red-orange heartwood, reddish peeling bark, very dark flat needles with red arils
        wood=[(132, 54, 32), (150, 66, 38), (166, 78, 44), (180, 90, 52), (193, 102, 60), (205, 115, 70), (216, 128, 82)],
        stripped_base="stripped_acacia_log", stripped_range=(1.2, 6.2),
        leaf=dict(D=(12, 34, 20), S=(18, 46, 26), M=(26, 60, 34), L=(38, 78, 44), H=(58, 100, 60),
                  r=(204, 44, 40), R=(140, 22, 26)),
        bark=[(44, 24, 24), (68, 36, 32), (94, 50, 40), (120, 64, 48), (150, 84, 60), (184, 106, 74)],
        metal=((150, 110, 40), (214, 174, 76))),
})

LEAF_SPRITES.update({
    "willow": [["L.", "LH", "MM", ".S"], [".L", "LM", "MS", "S."], ["L", "M", "M", "S"]],   # narrow, drooping
    "ash": [["L.L", "MLM", ".S."], ["LL.", ".MS"], [".L", "LM", "MS"]],                   # paired leaflets
    "hazel": [[".LL.", "LHMM", "LMMS", ".MS."], ["LL.", "LMS", ".S."]],                    # round, toothed
    "pine": [["L.L", ".M.", "S.S"], ["L.L.L", ".MMM.", "..S.."], ["L..", ".M.", "..S"]],   # needle tufts
    "yew": [["L.L", "MMM", "S.S"], [".L.", "LMS", ".S."], ["LMS"]],                       # flat needles along twigs
})

SAPLINGS.update({
    "willow": ([(7, 9, 4, 7, 2), (8, 7, 11, 5, 2)],
               [(2, 3, 5), (0, 2, 7), (1, 10, 4), (2, 12, 6), (0, 6, 1), (1, 8, 1), (2, 4, 10), (1, 10, 9)], 2, 12),
    "ash": ([(7, 9, 4, 6, 3), (8, 7, 11, 4, 3)],
            [(0, 2, 3), (0, 10, 1), (1, 5, 0), (2, 3, 7), (2, 11, 6), (1, 8, 3)], 1, 12),
    "hazel": ([(7, 12, 4, 6, 2), (8, 12, 11, 5, 2), (7, 9, 6, 3, 2)],
              [(0, 2, 3), (0, 9, 2), (1, 4, 0), (1, 11, 6), (1, 1, 7)], 5, 13),
    "pine": ([(7, 6, 4, 8, 1), (8, 6, 11, 8, 1), (7, 10, 3, 12, 1), (8, 10, 12, 12, 1)],
             [(0, 6, 0), (1, 5, 3), (1, 1, 6), (1, 9, 6), (2, 2, 9), (2, 10, 9), (0, 6, 7)], 1, 12),
    "yew": ([(7, 10, 3, 9, 1), (8, 9, 12, 8, 1), (7, 6, 4, 5, 1), (8, 5, 11, 4, 1)],
            [(0, 2, 7), (0, 10, 6), (0, 3, 3), (0, 9, 2), (1, 6, 0), (2, 5, 9), (2, 1, 5), (2, 11, 4)], 2, 12),
})

def _door(top_inner, bottom_head, bottom_inner):
    """inner 12x12 designs framed like the doors above; bottom_head = the two 11-wide rows beside the handle"""
    top = ["2222222222222222", "2555555555555532"] + ["25" + r + "32" for r in top_inner] + ["2544444444444432", "2533333333333332"]
    bottom = ["25" + bottom_head[0] + "kK2", "25" + bottom_head[1] + "k32"] + ["25" + r + "32" for r in bottom_inner] \
        + ["2544444444444432", "2222222222222222"]
    return top, bottom

def _item_door(top6, bottom5):
    return ["5555555552"] + ["54" + r + "32" for r in top6] + ["544444kK32"] + ["54" + r + "32" for r in bottom5] + ["3222222222"]

P12 = "pppppppppppp"
CROSS = ["pp...44...pp"] * 3 + ["pp44444444pp"] * 2 + ["pp...44...pp"] * 3
ARCH = ["pppp....pppp", "ppp......ppp", "pp........pp", "pp........pp", "pp........pp", "pp44444444pp"]
DOORS.update({
    "willow": _door([P12] + ["pp..pppp..pp"] * 7 + [P12] * 4, ("ppppppppppp", "pppdppppdpp"), ["pppdppppdppp"] * 12),
    "ash": _door([P12] + CROSS + [P12] * 3, ("ppppppppppp", "p553p553p55"), ["p5l2p5l2p5l2"] * 11 + ["p322p322p322"]),
    "hazel": _door([P12] + ["pp..p..p..pp", "pp..p..p..pp", P12] * 3 + [P12] * 2, ("qqqqqqqqqqq", "ccccccccccc"),
                   ["qqqqqqqqqqqq", "qqqqqqqqqqqq", "cccccccccccc"] * 4),
    "pine": _door([P12] + ARCH + [P12] * 5, ("ppppppppppp", "44444444444"),
                  ["pppdppppdppp"] * 4 + ["444444444444", "333333333333"] + ["pppdppppdppp"] * 4 + ["444444444444", "333333333333"]),
    "yew": _door([P12, "ppp..44..ppp", "pp...44...pp", "pp...44...pp", "pp44444444pp", "pp...44...pp", "pp...44...pp", "ppp..44..ppp"] + [P12] * 4,
                 ("ppppppppppp", "p5553pp5553"), ["p5dd2pp5dd2p"] * 11 + ["p3222pp3222p"]),
})
TRAPDOORS.update({
    "willow": [P12] + ["pp..pppp..pp"] * 8 + [P12] * 3,
    "ash": [P12, P12] + ["pp...44...pp"] * 3 + ["pp44444444pp"] * 2 + ["pp...44...pp"] * 3 + [P12, P12],
    "hazel": ["qqqqqqqqqqqq", "qqqqqqqqqqqq", "cccccccccccc"] * 4,
    "pine": [P12] + ARCH[:5] + ["pp44444444pp", "pp........pp", "pp........pp", "ppp......ppp", "pppp....pppp", P12],
    "yew": [P12, "ppp..44..ppp"] + ["pp...44...pp"] * 3 + ["pp44444444pp"] * 2 + ["pp...44...pp"] * 3 + ["ppp..44..ppp", P12],
})
ITEM_DOORS.update({
    "willow": _item_door(["pppppp", "p.pp.p", "p.pp.p", "p.pp.p", "p.pp.p", "pppppp"], ["pdppdp"] * 5),
    "ash": _item_door(["pppppp", "p.44.p", "p4444p", "p.44.p", "p.44.p", "pppppp"], ["5l25l2"] * 4 + ["322322"]),
    "hazel": _item_door(["pppppp", "p.pp.p", "pppppp", "p.pp.p", "pppppp", "pppppp"], ["qqqqqq", "cccccc", "qqqqqq", "cccccc", "qqqqqq"]),
    "pine": _item_door(["pp..pp", "p....p", "p....p", "p4444p", "pppppp", "pppppp"], ["pdppdp", "444444", "pdppdp", "pdppdp", "444444"]),
    "yew": _item_door(["pp44pp", "p.44.p", "444444", "p.44.p", "pp44pp", "pppppp"], ["5d25d2"] * 4 + ["322322"]),
})

# ------------------------------------------------------------------ fruit trees: rowan, bird cherry, wild apple, plum, olive, date palm
# Same set as above plus three fruit-stage leaves (<w>_leaves_blossom/_unripe/_ripe): one sprite per tree, stamped on the
# same spots in every stage with its letters recoloured (flower -> green fruit -> ripe fruit), so the fruit grows where
# the flowers were.
SPECIES.update({
    "rowan": dict(        # pale pinkish-brown wood, smooth silver-grey bark with lenticel dashes, airy pinnate leaves
        wood=[(160, 118, 100), (174, 131, 111), (187, 143, 122), (198, 154, 132), (208, 165, 143), (217, 176, 154), (226, 188, 166)],
        stripped_base="stripped_cherry_log", stripped_range=(1.6, 6.3),
        leaf=dict(D=(30, 66, 24), S=(46, 92, 32), M=(68, 122, 42), L=(98, 152, 58), H=(142, 186, 86)),
        bark=[(66, 66, 64), (96, 96, 92), (122, 121, 116), (144, 143, 137), (165, 164, 157), (186, 185, 178)],
        metal=((46, 46, 48), (132, 132, 136))),
    "bird_cherry": dict(  # light yellowish-brown wood, dark rough bark with pale lenticels, dark elliptic leaves
        wood=[(170, 136, 86), (183, 149, 97), (195, 161, 108), (205, 172, 118), (214, 183, 129), (222, 193, 140), (230, 203, 152)],
        stripped_base="stripped_oak_log", stripped_range=(1.4, 6.3),
        leaf=dict(D=(20, 50, 22), S=(30, 70, 30), M=(44, 92, 40), L=(62, 118, 52), H=(92, 146, 70)),
        bark=[(34, 30, 28), (50, 45, 41), (66, 59, 54), (82, 74, 67), (98, 89, 80), (116, 106, 96)],
        metal=((44, 44, 46), (128, 128, 132))),
    "wild_apple": dict(   # pinkish-tan wood with a darker heart, grey-brown scaly bark, dull oval leaves
        wood=[(146, 100, 80), (162, 114, 92), (177, 128, 103), (190, 141, 114), (201, 153, 125), (211, 165, 137), (220, 177, 149)],
        stripped_base="stripped_cherry_log", stripped_range=(1.2, 6.2),
        leaf=dict(D=(38, 64, 30), S=(54, 84, 38), M=(74, 108, 50), L=(98, 132, 64), H=(132, 160, 88)),
        bark=[(44, 38, 34), (64, 56, 49), (86, 76, 66), (106, 95, 83), (126, 114, 100), (148, 136, 120)],
        metal=((48, 48, 50), (136, 136, 140))),
    "plum": dict(         # warm reddish-brown wood, glossy dark purplish bark with lenticels, small fresh green leaves
        wood=[(128, 62, 44), (144, 74, 52), (158, 86, 60), (171, 97, 68), (183, 108, 77), (194, 120, 87), (205, 132, 98)],
        stripped_base="stripped_acacia_log", stripped_range=(1.3, 6.2),
        leaf=dict(D=(34, 80, 24), S=(50, 106, 32), M=(72, 136, 42), L=(100, 164, 56), H=(144, 198, 86)),
        bark=[(34, 22, 26), (52, 34, 38), (70, 46, 50), (88, 60, 62), (108, 76, 76), (134, 98, 96)],
        metal=((150, 110, 40), (214, 174, 76))),
    "olive": dict(        # cream wood with a dark wavy figure, pale grey gnarled bark, silvery grey-green lance leaves
        wood=[(176, 150, 100), (189, 163, 111), (201, 175, 122), (211, 186, 133), (220, 196, 144), (228, 206, 156), (236, 216, 168)],
        stripped_base="stripped_birch_log", stripped_range=(1.4, 6.4),
        leaf=dict(D=(62, 76, 62), S=(94, 110, 90), M=(136, 150, 128), L=(172, 184, 164), H=(212, 218, 204)),
        bark=[(60, 58, 52), (84, 82, 74), (108, 106, 96), (132, 129, 118), (154, 151, 139), (176, 173, 160)],
        metal=((44, 44, 46), (120, 120, 124))),
    "date_palm": dict(    # light tan fibrous wood, brown trunk of old leaf bases, grey-green fronds
        wood=[(168, 138, 94), (180, 150, 104), (192, 162, 114), (202, 173, 124), (211, 183, 134), (219, 193, 145), (227, 203, 156)],
        stripped_base="stripped_birch_log", stripped_range=(1.4, 6.4),
        leaf=dict(D=(38, 60, 42), S=(58, 84, 56), M=(82, 110, 72), L=(110, 138, 92), H=(150, 170, 120), r=(128, 126, 74)),
        bark=[(52, 36, 24), (76, 54, 34), (100, 72, 46), (124, 92, 60), (148, 114, 76), (172, 138, 96)],
        metal=((44, 44, 46), (128, 128, 132))),
})

def dashes(img, seed, n, cols, min_d=5):
    """horizontal lenticel dashes (light top, dark under) scattered at random"""
    return scatter(img, seed, n, cols, [[(0, 0, 1), (1, 0, 1), (2, 0, 1), (0, 1, 2), (1, 1, 2), (2, 1, 2)],
                                         [(0, 0, 1), (1, 0, 1), (0, 1, 2), (1, 1, 2)],
                                         [(0, 0, 1), (1, 0, 1), (2, 0, 1), (3, 0, 1), (1, 1, 2), (2, 1, 2)]], min_d)

def bark_rowan(p):
    b = p["bark"]; img = smooth_bark("rowan", b, 0.35, 0.9)
    return dashes(img, "rowan_l", 7, {1: (64, 62, 58), 2: b[1]})

def bark_bird_cherry(p):
    img = remap(van("dark_oak_log"), p["bark"], 0.05, 0.9, 0.85)
    return dashes(img, "bcherry_l", 6, {1: (156, 146, 128), 2: p["bark"][1]}, 6)

def bark_plum(p):
    b = p["bark"]; img = smooth_bark("plum", b, 0.2, 0.75)
    for y in range(16):  # glossy sheen running down the stem
        for x in (3, 4, 11):
            put(img, x, y, mix(img.getpixel((x, y)), b[5], 0.55 if x == 4 else 0.35))
    return dashes(img, "plum_l", 7, {1: (168, 136, 120), 2: b[0]})

def bark_wild_apple(p):
    """scaly bark: tall plates (wrapped Voronoi cells), each lit at the top, shaded at the bottom, dark cracks between"""
    b = p["bark"]; r = random.Random("apple_plates")
    pts = [(r.uniform(0, 16), r.uniform(0, 16), r.uniform(0.3, 0.9)) for _ in range(15)]
    img = new()
    for y in range(16):
        for x in range(16):
            ds = []
            for px, py, t in pts:
                dx = min(abs(x - px), 16 - abs(x - px)); dy = min(abs(y - py), 16 - abs(y - py)) * 0.45
                ds.append((dx * dx + dy * dy, py, t))
            ds.sort(); (d0, py, t), d1 = ds[0], ds[1][0]
            gap = math.sqrt(d1) - math.sqrt(d0)
            if gap < 0.42: put(img, x, y, b[0] if gap < 0.2 else b[1]); continue
            up = (py - y + 8) % 16 - 8  # >0 above the plate centre
            put(img, x, y, ramp_at(b, 1.6 + 2.6 * t + 0.3 * up))
    # a few plates flaking off: a light freshly exposed patch
    return scatter(img, "apple_flake", 3, {1: (150, 120, 94), 2: (126, 98, 76)},
                   [[(0, 0, 1), (1, 0, 1), (0, 1, 2)], [(0, 0, 1), (0, 1, 2), (1, 1, 2)]], 7, opaque_only=True)

def bark_olive(p):
    """gnarled: pale smooth bark split by deep fissures that wind round the trunk, a lit ridge left of each"""
    b = p["bark"]; img = smooth_bark("olive", b, 0.45, 0.95)
    for x0, amp, ph, step in ((2, 1.6, 0.0, 1), (7, 2.2, 2.0, 1), (12, 1.4, 4.0, 1)):
        for y in range(16):
            x = x0 + round(amp * math.sin(2 * math.pi * y / 16 + ph))
            put(img, x, y, b[0]); put(img, x + 1, y, b[1]); put(img, x - 1, y, b[5])
    # a knot where a branch once grew
    return scatter(img, "olive_knot", 1, {1: b[0], 2: b[2], 3: b[4]},
                   [[(0, 0, 1), (1, 0, 1), (-1, 0, 2), (2, 0, 2), (0, -1, 3), (1, -1, 3), (0, 1, 2), (1, 1, 2)]], 8)

def bark_date_palm(p):
    """old leaf bases: stacked crescents, each lit along its top edge and shading down to a dark curved lower rim;
    every row shifted half a base so the rims interlock into a diamond net"""
    b = p["bark"]; img = new()
    for y in range(16):
        for x in range(16):
            row = y // 4; u = (x + (4 if row % 2 else 0)) % 8; v = y % 4
            rim = (1, 2, 3, 3, 3, 3, 2, 1)[u]                      # the lower rim curves up at the sides
            if v > rim: c = b[1] if (x + y) % 2 else b[0]          # fibre showing between neighbouring bases
            elif v == rim: c = b[0]
            elif v == 0: c = b[5] if 1 <= u <= 6 else b[3]
            else: c = b[4 - v] if 1 <= u <= 6 else b[2 - min(v, 1)]
            put(img, x, y, c)
    return img

BARK.update({"rowan": bark_rowan, "bird_cherry": bark_bird_cherry, "wild_apple": bark_wild_apple,
             "plum": bark_plum, "olive": bark_olive, "date_palm": bark_date_palm})

def palm_top(p):
    """palm trunks have no growth rings: scattered fibre bundles in a soft pith, a ring of leaf-base bark round it"""
    w, b = p["wood"], p["bark"]; img = new(); r = random.Random("palmtop")
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5))
            if d > 7: c = b[2] if (x + y) % 2 else b[1]
            elif d > 6: c = w[0]
            else: c = w[4] if r.random() < 0.7 else w[5]
            put(img, x, y, c)
    for _ in range(26):
        x, y = r.randrange(2, 14), r.randrange(2, 14)
        put(img, x, y, w[1]); put(img, x + 1, y, w[2])
    return img

def figure(img, p, seed, rows):
    """olive's dark wavy figure: a few meandering streaks along the grain"""
    r = random.Random(seed)
    for y0 in rows:
        ph = r.uniform(0, 6.28)
        for x in range(16):
            y = y0 + round(1.2 * math.sin(ph + x * 0.7))
            put(img, x, y, mix(img.getpixel((x % 16, y % 16)), (96, 72, 44), 0.6))

def fibres(img, seed, vertical):
    """long fibre streaks along the grain"""
    n = noise(seed, 2, 8) if vertical else noise(seed, 8, 2)
    for y in range(16):
        for x in range(16):
            put(img, x, y, shade(img.getpixel((x, y)), 0.9 + 0.16 * n[y][x]))

# fruit-tree leaves: a vanilla leaf structure remapped like the others
LEAF_BASE.update({"rowan": "acacia_leaves", "bird_cherry": "oak_leaves", "wild_apple": "azalea_leaves",
                  "plum": "oak_leaves", "olive": "pale_oak_leaves"})

# per tree: one small fruit sprite (same letters in every stage) and the stage palettes (1 blossom, 2 unripe, 3 ripe).
# Each stage comes in three variants (<w>_leaves_ripe, _ripe_2, _ripe_3; the blockstate picks one per block and turns
# it) with only one or two small clusters, so a crown shows scattered fruit among plenty of leaf instead of a grid.
# A variant keeps its spots in every stage, so the fruit grows where the flowers were. Apple and plum bloom with their
# own blossom sprite (bloom). s = stalk, always twig brown. Every stamped sprite gets a dark leaf shadow under its
# right/bottom edge so it stands off the foliage.
FRUIT = {
    # a small flat-topped corymb: single-pixel berries (H lit, A plain) in a dome, darker gaps d, thin stalks
    "rowan": dict(sprite=[
        "..s..",
        ".s.s.",
        "HdAdH",
        ".AdH."],
        stages={1: dict(H=(248, 242, 216), A=(232, 226, 194), d=(196, 190, 150)),
                2: dict(H=(214, 204, 92), A=(206, 150, 56), d=(124, 120, 40)),
                3: dict(H=(255, 150, 70), A=(230, 72, 28), d=(140, 34, 20))}),
    # one drooping raceme of small flowers / cherries, a glossy highlight h near the top
    "bird_cherry": dict(sprite=[
        "s.",
        "hB",
        "aA",
        "Ba",
        ".A"],
        stages={1: dict(h=(255, 255, 248), A=(240, 242, 230), B=(222, 226, 208), a=(180, 188, 168)),
                2: dict(h=(208, 98, 64), A=(172, 198, 80), B=(146, 176, 62), a=(100, 128, 42)),
                3: dict(h=(180, 160, 190), A=(70, 46, 68), B=(48, 30, 46), a=(22, 12, 20))}),
    # one small apple on its stalk: highlight h, blush A, yellow-green Y, shade a
    "wild_apple": dict(sprite=[
        "s.",
        "hA",
        "Ya"],
        bloom=[".W.", "WyP", ".P."],
        stages={1: dict(W=(252, 244, 246), P=(238, 186, 204), y=(216, 196, 88)),
                2: dict(h=(210, 234, 132), A=(160, 202, 74), Y=(142, 188, 64), a=(96, 136, 44)),
                3: dict(h=(252, 242, 150), A=(208, 66, 44), Y=(206, 200, 76), a=(150, 74, 36))}),
    # one plum: waxy bloom b, body P/p, shade d
    "plum": dict(sprite=[
        "s.",
        "bP",
        "pd"],
        bloom=[".W.", "WyP", ".P."],
        stages={1: dict(W=(252, 252, 246), P=(220, 226, 212), y=(206, 204, 100)),
                2: dict(b=(234, 242, 172), P=(204, 224, 98), p=(166, 198, 72), d=(78, 110, 36)),
                3: dict(b=(170, 162, 210), P=(100, 74, 150), p=(68, 46, 110), d=(40, 26, 66))}),
    # two small oval olives on stalks
    "olive": dict(sprite=[
        "s.s",
        "h.s",
        "O.h",
        "..O"],
        stages={1: dict(h=(250, 244, 206), O=(226, 216, 164)),
                2: dict(h=(196, 210, 116), O=(126, 146, 52)),
                3: dict(h=(150, 118, 162), O=(52, 30, 58))}),
}
TWIG = (86, 62, 44)
# per variant: clusters (x, y, sprites in the cluster); a cluster's second sprite sits beside and a bit below the first
FRUIT_VARIANTS = [[(2, 2, 2), (10, 9, 1)], [(9, 3, 1), (3, 11, 2)], [(6, 7, 1)]]

def stamp_shadowed(img, sprite, x0, y0, cols, shadow):
    stamp(img, sprite, x0, y0, cols)
    mask = {((x0 + i) % 16, (y0 + j) % 16) for j, row in enumerate(sprite) for i, ch in enumerate(row) if ch not in ". "}
    for x, y in mask:
        for q in (((x + 1) % 16, y), (x, (y + 1) % 16)):
            if q not in mask and img.getpixel(q)[3]: put(img, q[0], q[1], shadow)

def rowan_leaves(p):
    """pinnate leaves: a rachis with paired leaflets (lit above, shaded below) laid over a dark under-crown"""
    c = p["leaf"]; img = remap(van("oak_leaves"), [c["D"], c["D"], c["S"], c["S"], c["M"]], 0.0, 1.0)
    r = random.Random("rowan_pinnate"); pts = []
    for _ in range(4000):
        q = (r.randrange(16), r.randrange(16))
        if all(min((q[0] - o[0]) % 16, (o[0] - q[0]) % 16) ** 2 + min((q[1] - o[1]) % 16, (o[1] - q[1]) % 16) ** 2 >= 26 for o in pts):
            pts.append(q)
        if len(pts) == 9: break
    for x0, y0 in pts:
        dx, dy = r.choice(((1, 1), (-1, 1), (1, 0), (-1, 0), (1, 1), (-1, 1)))
        px, py = (-dy, dx) if dx > 0 else (dy, -dx)        # perpendicular pointing up/left (the lit side)
        if py > 0 or (py == 0 and px > 0): px, py = -px, -py
        n = r.randint(4, 6)
        for i in range(n):
            x, y = x0 + dx * i, y0 + dy * i
            put(img, x, y, c["M"])
            put(img, x + px, y + py, c["H"] if i % 2 else c["L"])
            put(img, x - px, y - py, c["S"] if i % 2 else c["M"])
        put(img, x0 + dx * n, y0 + dy * n, c["L"])          # terminal leaflet
    return img

def olive_leaves(p):
    """open silvery crown: the pale oak structure in sage/silver with its darkest fifth cut away as holes"""
    img = leaves("olive", p)
    px = sorted((lum(img.getpixel((x, y))), x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3])
    for _, x, y in px[:len(px) // 5]: img.putpixel((x, y), (0, 0, 0, 0))
    return img

def fruit_leaves(name, p):
    """plain leaves plus three variants of each fruit stage"""
    base = {"rowan": rowan_leaves, "olive": olive_leaves}.get(name, lambda p: leaves(name, p))(p)
    f = FRUIT[name]; out = {"": base}; dark = p["leaf"]["S" if name == "olive" else "D"]  # olive: soft shadow on the silver
    for st, key in ((1, "_blossom"), (2, "_unripe"), (3, "_ripe")):
        cols = dict(f["stages"][st], s=(112, 116, 96) if name == "olive" else TWIG)
        sprite = f["bloom"] if st == 1 and "bloom" in f else f["sprite"]
        w = len(sprite[0])
        for v, clusters in enumerate(FRUIT_VARIANTS):
            img = base.copy()
            for x, y, n in clusters:
                for k in range(n): stamp_shadowed(img, sprite, x + k * w, y + k * 2, cols, dark)
            out[key + ("" if v == 0 else f"_{v + 1}")] = img
    return out

# ------------------------------------------------------------------ date palm crown (frond model, not leaf cubes)
# _top: a frond seen from above on the flat plane at the top of the block; _hang: drooping leaflet tips on the crossed
# planes under it (only rows 4-15 show); _blossom/_unripe/_ripe: the same with flower strands / a date bunch.
def palm_frond_top(p):
    """one frond corner to corner: a pale midrib, long thin leaflets every other pixel on both sides, swept toward the
    base (top left), lit on the upper side"""
    c = p["leaf"]; img = new()
    for i in range(1, 16, 2):
        for sx, sy, f in ((1, -1, 1.0), (-1, 1, 0.82)):
            ex, ey = i + sx * 5 - 2, i + sy * 5 - 2           # leaflet tip
            for k in range(1, 8):
                x, y = round(i + (ex - i) * k / 7), round(i + (ey - i) * k / 7)
                if 0 <= x < 16 and 0 <= y < 16: put(img, x, y, shade([c["H"], c["L"], c["L"], c["M"], c["M"], c["S"], c["S"]][k - 1], f))
    for i in range(16): put(img, i, i, c["r"])
    return img

def palm_hang(p, seed="hang"):
    c = p["leaf"]; img = new(); r = random.Random(seed)
    tones = [c["L"], c["M"], c["M"], c["S"], c["S"], c["D"], c["D"], c["D"]]
    for x in range(0, 16, 2):
        x0 = x + r.choice((0, 1)); n = r.randint(3, 8); lean = r.choice((-1, 1))
        for i in range(n): put(img, x0 + (lean if i > n * 0.6 else 0), 4 + i, tones[i])
    return img

BUNCH = [3, 5, 7, 8, 8, 7, 6, 4, 2]   # widths of the date bunch, row by row from y=6
def palm_bunch(img, st):
    stalk = {1: (206, 190, 120), 2: (200, 150, 54), 3: (170, 110, 46)}[st]
    for y in range(4, 7): put(img, 8, y, stalk)
    if st == 1:  # cream flower strands hanging from a short cross stalk
        for x in range(5, 12): put(img, x, 5, stalk)
        for x0, n in ((5, 6), (7, 9), (9, 8), (11, 5)):
            for i in range(n): put(img, x0, 6 + i, (244, 234, 178) if (i + x0) % 2 else (214, 200, 132))
        return img
    lit, mid, dark, hi = {2: ((246, 196, 70), (226, 158, 40), (160, 98, 24), (255, 236, 150)),
                          3: ((164, 70, 40), (126, 46, 28), (64, 20, 14), (224, 150, 116))}[st]
    for j, w in enumerate(BUNCH):
        y = 6 + j; x0 = 8 - w // 2
        for x in range(x0, x0 + w):
            edge = x in (x0, x0 + w - 1)
            put(img, x, y, dark if (x + y) % 2 else (mid if edge or j > 5 else lit))
    for x, y in ((6, 8), (9, 7), (7, 10), (10, 10), (8, 12)): put(img, x, y, hi)   # glossy highlights on single dates
    return img

def palm_crown(p):
    hang = palm_hang(p)
    out = {"_top": palm_frond_top(p), "_hang": hang}
    for st, key in ((1, "_blossom"), (2, "_unripe"), (3, "_ripe")): out[key] = palm_bunch(hang.copy(), st)
    return out

def palm_frond(p):
    """three arching fronds across the tile: a midrib with long drooping leaflets, alternately left and right"""
    c = p["leaf"]; img = new()
    for k, (y0, ph) in enumerate(((1, 0.0), (6, 2.1), (11, 4.2))):
        for x in range(16):
            y = y0 + round(1.5 * math.sin(2 * math.pi * x / 16 + ph))
            lit = (x // 2 + k) % 2
            if x % 2 == 0:
                for i in range(1, 5): put(img, x - i // 2, y + i, [c["H"] if lit else c["L"], c["L"], c["M"], c["S"]][i - 1])
            else:
                for i in range(1, 4): put(img, x + i // 2, y + i, [c["L"] if lit else c["M"], c["M"], c["S"]][i - 1])
        for x in range(16): put(img, x, y0 + round(1.5 * math.sin(2 * math.pi * x / 16 + ph)), c["r"])
    return img

LEAF_SPRITES.update({
    "rowan": [["L.L.L", "MHMHM", ".S.S."], ["L.L", "HMH", "S.S"]],                     # rows of paired leaflets
    "bird_cherry": [[".L.", "LHM", "LMS", ".S."], ["LL.", "MMS", ".S."]],
    "wild_apple": [[".LL.", "LHMS", "LMMS", ".SS."], ["LL.", "LMS", ".S."]],
    "plum": [[".L.", "LHS", ".S."], ["LM", "MS"]],
    "olive": [["HL.", ".MS"], [".L", "HM", "S."], ["LL", ".S"]],                   # narrow silvery lances
})
SAPLINGS.update({
    "rowan": ([(7, 10, 4, 7, 2), (8, 8, 11, 5, 2)], [(0, 1, 5), (0, 9, 3), (1, 5, 0), (1, 2, 1), (1, 10, 0), (0, 5, 3)], 2, 12),
    "bird_cherry": ([(7, 10, 3, 7, 1), (8, 8, 12, 5, 1), (7, 6, 5, 3, 1)],
                    [(0, 1, 4), (0, 10, 2), (1, 4, 0), (0, 6, 4), (1, 10, 6)], 3, 12),
    "wild_apple": ([(7, 9, 3, 7, 1), (8, 8, 12, 6, 1), (7, 6, 6, 3, 1)],
                   [(0, 0, 4), (0, 10, 3), (1, 5, 0), (1, 2, 8), (1, 11, 7)], 3, 12),
    "plum": ([(7, 10, 4, 7, 1), (8, 8, 11, 5, 1), (7, 6, 6, 3, 1)],
             [(0, 2, 5), (0, 10, 3), (1, 5, 1), (0, 7, 2), (1, 11, 6), (1, 3, 8)], 3, 12),
    "olive": ([(7, 11, 4, 8, 3), (4, 8, 3, 5, 3), (8, 9, 11, 6, 3), (11, 6, 12, 3, 3)],
              [(0, 1, 4), (1, 2, 6), (2, 3, 2), (0, 11, 2), (1, 12, 4), (2, 10, 1), (0, 6, 4), (1, 8, 4), (2, 5, 1)], 6, 13),
})

def palm_sapling(p):
    """a little fan of fronds out of a fibrous stump: each frond a pale midrib with leaflets hanging off it"""
    c, b = p["leaf"], p["bark"]; img = new()
    fronds = ((7, 10, 1, 8), (8, 10, 14, 8), (7, 9, 2, 3), (8, 9, 13, 3), (7, 9, 6, 1), (8, 9, 10, 1))
    for x0, y0, x1, y1 in fronds:
        n = max(abs(x1 - x0), abs(y1 - y0)); sx = 1 if x1 > x0 else -1
        for i in range(1, n + 1):
            x = round(x0 + (x1 - x0) * i / n); y = round(y0 + (y1 - y0) * i / n + 0.06 * i * i)  # tips droop
            if 0 <= y < 16:
                if i > 1 and 0 <= y + 1 < 16: put(img, x, y + 1, c["S"] if i % 2 else c["M"])
                if i > 2 and 0 <= y - 1 < 16 and i % 2 == 0: put(img, x - sx, y - 1, c["L"])
                put(img, x, y, c["r"] if i < n else c["L"])
    for y in range(10, 16):
        put(img, 7, y, b[4] if y % 2 else b[2]); put(img, 8, y, b[2] if y % 2 else b[1])
    return img

SAPLING_FN = {"date_palm": lambda name, p: palm_sapling(p)}

WEAVE = ["llcc", "llcc", "ccll", "ccll"]
def weave(w, h): return ["".join(WEAVE[y % 4][x % 4] for x in range(w)) for y in range(h)]
DOORS.update({
    # rowan: two arched windows, boards with a Z brace below
    "rowan": _door([P12, "ppp.pppp.ppp", "pp..pppp..pp", "pp..pppp..pp", "pp..pppp..pp", "pp..pppp..pp", "pp..pppp..pp",
                    "pp44pppp44pp", P12, P12, P12, P12],
                   ("ppppppppppp", "44444444444"),
                   ["pppdppppdppp"] * 4 + ["444444444444", "333333333333"] + ["pppdppppdppp"] * 4
                   + ["444444444444", "333333333333"]),
    # bird cherry: a round window with a cross, two raised panels below
    "bird_cherry": _door([P12, "pppp....pppp", "ppp..44..ppp", "pp...44...pp", "pp44444444pp", "pp...44...pp", "ppp..44..ppp",
                          "pppp....pppp", P12, P12, P12, P12],
                         ("ppppppppppp", "p5553pp5553"), ["p5ll2pp5ll2p"] * 4 + ["p3222pp3222p", P12, "p5553pp5553p"]
                         + ["p5ll2pp5ll2p"] * 4 + ["p3222pp3222p"]),
    # wild apple: cottage door, four-pane window, horizontal boards below
    "wild_apple": _door([P12, "pp...44...pp", "pp...44...pp", "pp...44...pp", "pp44444444pp", "pp...44...pp", "pp...44...pp",
                         "pp...44...pp", P12, P12, "qqqqqqqqqqqq", "cccccccccccc"],
                        ("qqqqqqqqqqq", "ccccccccccc"), ["qqqqqqqqqqqq", "qqqqqqqqqqqq", "cccccccccccc"] * 4),
    # plum: three slit windows, one big raised panel
    "plum": _door([P12, "pp.pp..pp.pp", "pp.pp..pp.pp", "pp.pp..pp.pp", "pp.pp..pp.pp", "pp.pp..pp.pp", P12,
                   "444444444444", P12, P12, P12, P12],
                  ("ppppppppppp", "p5555555553"), ["p5llllllll2p"] * 11 + ["p3222222222p"]),
    # olive: rustic uneven boards with dark gaps, iron straps, a small peep window
    "olive": _door([P12, "ppdpp..ppdpp", "ppdpp..ppdpp", "ppdppppppdpp", "kkkkkkkkkkkk", "ppdpppdppdpp", "ppdpppdppdpp",
                    "ppdpppdppdpp", "ppdpppdppdpp", "ppdpppdppdpp", "ppdpppdppdpp", "ppdpppdppdpp"],
                   ("ppdpppdppdp", "ppdpppdppdp"),
                   ["ppdpppdppdpp"] * 3 + ["kkkkkkkkkkkk"] + ["ppdpppdppdpp"] * 6 + ["kkkkkkkkkkkk", "ppdpppdppdpp"]),
    # date palm: frame of palm wood with woven fibre panels
    "date_palm": _door([P12, "pp........pp", "pp........pp", "pp........pp", "pp44444444pp"] + ["pp" + r + "pp" for r in weave(8, 6)]
                       + [P12],
                       ("ppppppppppp", "p5555555553"), ["p5" + r + "2p" for r in weave(8, 10)] + ["p3222222222p"]),
})
TRAPDOORS.update({
    "rowan": ["pppdppppdppp"] * 3 + ["444444444444", "333333333333"] + ["pppdppppdppp"] * 2
             + ["444444444444", "333333333333"] + ["pppdppppdppp"] * 3,
    "bird_cherry": [P12, "pppp....pppp", "ppp..44..ppp", "pp...44...pp", "pp...44...pp", "pp44444444pp", "pp44444444pp",
                    "pp...44...pp", "pp...44...pp", "ppp..44..ppp", "pppp....pppp", P12],
    "wild_apple": ["qqqqqqqqqqqq", "qqqqqqqqqqqq", "cccccccccccc"] * 4,
    "plum": [P12] + ["pp.pp..pp.pp"] * 10 + [P12],
    "olive": ["ppdpppdppdpp"] * 2 + ["kkkkkkkkkkkk"] + ["ppdpppdppdpp"] * 6 + ["kkkkkkkkkkkk"] + ["ppdpppdppdpp"] * 2,
    "date_palm": ["p" + r + "p" for r in weave(10, 12)],
})
ITEM_DOORS.update({
    "rowan": _item_door(["pppppp", "p.pp.p", "p.pp.p", "p.pp.p", "p4pp4p", "pppppp"], ["pdppdp", "444444", "pdppdp", "pdppdp", "444444"]),
    "bird_cherry": _item_door(["pp..pp", "p.44.p", "p4444p", "p.44.p", "pp..pp", "pppppp"], ["5l25l2"] * 4 + ["322322"]),
    "wild_apple": _item_door(["pppppp", "p.4..p", "p4444p", "p.4..p", "pppppp", "cccccc"], ["qqqqqq", "cccccc", "qqqqqq", "cccccc", "qqqqqq"]),
    "plum": _item_door(["pppppp", "p.p.p.", "p.p.p.", "p.p.p.", "pppppp", "444444"], ["5llll2"] * 4 + ["322222"]),
    "olive": _item_door(["pdp..p", "pdpppp", "kkkkkk", "pdpdpd", "pdpdpd", "pdpdpd"], ["pdpdpd", "kkkkkk", "pdpdpd", "pdpdpd", "pdpdpd"]),
    "date_palm": _item_door(["p....p", "p....p", "p4444p", "pllccp", "pccllp", "pllccp"], ["5" + r + "2" for r in weave(4, 4)] + ["322222"]),
})

# ------------------------------------------------------------------ tree extras: beechnuts, roasted beechnuts, pine resin
def item_sprite(rows, cols):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    img = new()
    stamp(img, rows, 0, 0, cols, wrap=False)
    return img

NUTS = ["................", "................", "................", "......d.........", ".....dld........",
        "....dlhmd.......", "....dlmmd...d...", "...dlmmmmd.dld..", "...dmmmmmddlhmd.", "...dmmmmd.dlmmd.",
        "....dddd.dlmmmmd", "..........dmmmmd", "..........dmmmd.", "...........ddd..", "................", "................"]
RESIN = ["................", "................", "......oo........", ".....oaao.......", "....oabbao......",
         "....oabyba......", "...oabyybao.....", "...oabbbbbao....", "..oaabbbbbao....", "..oaabbbbbbao...",
         "..oaaabbbbbao...", "...oaaaabbao....", "....oaaaaao.....", ".....ooooo......", "................", "................"]

def extras():
    raw = dict(d=(70, 40, 22), m=(132, 84, 44), l=(178, 124, 70), h=(214, 168, 112))
    roast = dict(d=(42, 24, 14), m=(98, 58, 28), l=(140, 90, 46), h=(186, 136, 80))
    save(item_sprite(NUTS, raw), "item/beechnuts.png")
    save(item_sprite(NUTS, roast), "item/roasted_beechnuts.png")
    save(item_sprite(RESIN, dict(o=(116, 58, 12), a=(186, 112, 22), b=(228, 162, 44), y=(255, 228, 140))), "item/pine_resin.png")

# ------------------------------------------------------------------ assemble
_rims = {}
def rim(name, x, y): return _rims[name].getpixel((x, y))[:3]

written = []
def save(img, rel):
    path = os.path.join(out, rel); img.save(path); written.append((rel, img))

for name, p in SPECIES.items():
    bark = BARK[name](p); _rims[name] = bark
    save(bark, f"block/{name}_log.png")
    save(palm_top(p) if name == "date_palm" else recolour_planks(name, "oak_log_top", p), f"block/{name}_log_top.png")
    strip = stripped_side(p)
    if name == "beech": flecks(strip, p, "beechs", 14)
    if name == "olive": figure(strip, p, "olives", (3, 10))
    if name == "date_palm": fibres(strip, "palms", True)
    save(strip, f"block/stripped_{name}_log.png")
    _rims[name] = strip
    save(recolour_planks(name, "stripped_oak_log_top", p), f"block/stripped_{name}_log_top.png")
    planks = recolour_planks(name, "oak_planks", p)
    if name == "beech": flecks(planks, p, "beechp", 10)
    if name == "olive": figure(planks, p, "olivep", (2, 6, 13))
    if name == "date_palm": fibres(planks, "palmp", False)
    save(planks, f"block/{name}_planks.png")
    if name in FRUIT:
        for key, img in fruit_leaves(name, p).items(): save(img, f"block/{name}_leaves{key}.png")
    elif name == "date_palm":
        save(palm_frond(p), f"block/{name}_leaves.png")
        for key, img in palm_crown(p).items(): save(img, f"block/{name}_leaves{key}.png")
    else: save(leaves(name, p), f"block/{name}_leaves.png")
    save(SAPLING_FN.get(name, sapling)(name, p), f"block/{name}_sapling.png")
    top_rows, bottom_rows = DOORS[name]
    save(paint(top_rows, p, planks), f"block/{name}_door_top.png")
    save(paint(bottom_rows, p, planks), f"block/{name}_door_bottom.png")
    save(trapdoor(name, p, planks), f"block/{name}_trapdoor.png")
    save(item_door(name, p, planks), f"item/{name}_door.png")
extras()

if prev:
    S, per = 8, 11
    sheet = Image.new("RGBA", (per * (16 * S + 8) + 8, -(-len(written) // per) * (16 * S + 8) + 8), (118, 118, 118, 255))
    for i, (rel, img) in enumerate(written):
        big = img.resize((16 * S, 16 * S), Image.NEAREST)
        sheet.paste(big, (8 + (i % per) * (16 * S + 8), 8 + (i // per) * (16 * S + 8)), big)
    sheet.save(prev)
print(f"ok, {len(written)} textures")
