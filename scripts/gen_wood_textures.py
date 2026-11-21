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
    c = p["leaf"]; r = random.Random(name + "leaves")
    img = remap(van(LEAF_BASE.get(name, "oak_leaves")), [c[k] for k in "DSMLH"], 0.0, 1.0)
    for y in range(16):
        for x in range(16):
            if img.getpixel((x, y))[3] == 0 and r.random() < 0.45: put(img, x, y, c["D"])
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
extras()

if prev:
    S, per = 8, 11
    sheet = Image.new("RGBA", (per * (16 * S + 8) + 8, -(-len(written) // per) * (16 * S + 8) + 8), (118, 118, 118, 255))
    for i, (rel, img) in enumerate(written):
        big = img.resize((16 * S, 16 * S), Image.NEAREST)
        sheet.paste(big, (8 + (i % per) * (16 * S + 8), 8 + (i // per) * (16 * S + 8)), big)
    sheet.save(prev)
print(f"ok, {len(written)} textures")
