"""16x16 item and block textures for the warm-country fauna (part of gen_fauna_warm_textures.py).

- meats: scrap pieces recoloured from the house *_scraps shapes (like venison in gen_wildlife_item_textures.py), each
  animal on its own base shape: onager (mutton shape, lean dark red), tortoise (chicken, pale pink-grey), buffalo (beef,
  marbled), tapir (porkchop, darker), leopard (rabbit, dark with fibres); the lizard tail is painted along a curve.
- birds: cuts in the gen_birds2_textures.py style (hornbill wing, crossed jungle fowl drumsticks, peafowl breast fillet)
  and plucked birds (hornbill with its casque, jungle fowl with comb and wattle, long-necked peafowl).
- hides on the house raw_hide shape (onager, tapir, buffalo), the leopard pelt like the wolf pelt (gen_predator_textures)
  and its raw version like raw_wolf_pelt (gen_pelt_rack_textures.raw_item), lizard skin.
- parts, made items, spawn eggs (gen_wildlife_item_textures.egg).
- blocks: leopard_rug (top of the Bear Rug slab, head north).
Deterministic (hash noise). Run: python scripts/gen_fauna_warm_tex_items.py"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, shade, smooth  # noqa: E402
from gen_wildlife_item_textures import EGG_ROWS, egg, from_grid, hexc, recolour  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
CLEAR = (0, 0, 0, 0)
N4 = ((1, 0), (-1, 0), (0, 1), (0, -1))

# shared with gen_pelt_rack_textures.py (copied: that script paints on import)
TWINE, TWINE_DK = (170, 130, 80), (125, 92, 55)
RAW_FLESH = [(132, 44, 42), (168, 70, 64), (194, 104, 92)]
FAT = (232, 214, 188)
SKIN = [(128, 96, 62), (160, 124, 86), (184, 150, 108)]
EYE, NOSE = (14, 12, 10), (24, 22, 22)

# leopard
L_GOLD = (212, 160, 78)
L_DEEP = (184, 120, 50)
L_CREAM = (238, 224, 192)
L_SPOT = (34, 26, 20)
L_AMBER = (204, 170, 60)


def rgb(c):
    return hexc(c)[:3] if isinstance(c, str) else tuple(c[:3])


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), (*rgb(c), 255))


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    return lerp(cols[i], cols[i + 1], t - i)


def solid_of(im):
    px = im.load()
    return {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}


def edge_of(solid):
    return {(x, y) for x, y in solid if any((x + dx, y + dy) not in solid for dx, dy in N4)}


def grid(rows, pal):
    """from_grid, but palette values may be rgb tuples or callables (x, y) -> colour."""
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                put(im, x, y, c(x, y) if callable(c) else c)
    return im


# ---------------------------------------------------------------- tube painter: a shape swept along a curve

def tube(points, width, colour, outline):
    """points: polyline (x, y) in pixels; width(t) -> full width; colour(t, s, x, y) with t 0..1 along, s -1..1 across
    (s < 0 = the lit, upper-left side). Pixels on the border get outline(t, x, y)."""
    pts = []
    for (ax, ay), (bx, by) in zip(points, points[1:]):
        for k in range(24):
            f = k / 24
            pts.append((ax + (bx - ax) * f, ay + (by - ay) * f))
    pts.append(points[-1])
    n = len(pts) - 1
    inside = {}
    for y in range(16):
        for x in range(16):
            cx, cy = x + 0.5, y + 0.5
            i = min(range(n + 1), key=lambda k: (pts[k][0] - cx) ** 2 + (pts[k][1] - cy) ** 2)
            t = i / n
            d = math.hypot(pts[i][0] - cx, pts[i][1] - cy)
            if d <= width(t) / 2:
                j0, j1 = max(0, i - 1), min(n, i + 1)
                tx, ty = pts[j1][0] - pts[j0][0], pts[j1][1] - pts[j0][1]
                side = (tx * (cy - pts[i][1]) - ty * (cx - pts[i][0]))
                s = max(-1.0, min(1.0, math.copysign(d / max(width(t) / 2, 0.01), side)))
                # light from the upper left: flip so that s < 0 faces up-left
                nx, ny = -ty, tx
                if nx + ny > 0:
                    s = -s
                inside[(x, y)] = (t, s)
    im = Image.new("RGBA", (16, 16), CLEAR)
    for (x, y), (t, s) in inside.items():
        border = any((x + dx, y + dy) not in inside for dx, dy in N4)
        put(im, x, y, outline(t, x, y) if border else colour(t, s, x, y))
    return im


# ---------------------------------------------------------------- meats

ONAGER_RAW = ["#2a0607", "#430b0c", "#5e1112", "#741816", "#87201c", "#962a24", "#a63a30", "#b8564a"]
ONAGER_COOKED = ["#1e0f08", "#2e1a10", "#402616", "#50301d", "#5e3a24", "#6c442b", "#7c5034", "#9a6c48"]
TORTOISE_RAW = ["#6e5458", "#86686a", "#9c7e7e", "#b09292", "#c0a4a2", "#ceb4b0", "#dcc6c0", "#e8d8d0"]
TORTOISE_COOKED = ["#4a3c30", "#62503e", "#78644e", "#8c765c", "#9c886c", "#ac987c", "#bcaa8e", "#d0c0a4"]
BUFFALO_RAW = ["#200506", "#360809", "#4c0d0e", "#5e1212", "#701816", "#7e1e1b", "#8c2822", "#9c3a30"]
BUFFALO_COOKED = ["#170b07", "#25140c", "#341d11", "#422616", "#4e2e1b", "#5a3620", "#684026", "#80583a"]
TAPIR_RAW = ["#5a1a22", "#78262e", "#94363c", "#ac4a4e", "#c05e60", "#d07672", "#dc8e88", "#e8aaa2"]
TAPIR_COOKED = ["#3a2212", "#583418", "#74481f", "#8c5a28", "#a06c34", "#b28044", "#c29658", "#d8b47a"]
LEOPARD_RAW = ["#1c0607", "#2e0a0c", "#420f11", "#541616", "#651c1b", "#722420", "#80302a", "#94463c"]
LEOPARD_COOKED = ["#140b08", "#20120c", "#2c1910", "#382014", "#432719", "#4e2e1e", "#5a3824", "#704a32"]


def meat(src, ramp_, fat=None, fibre=None, salt=0):
    im = recolour(ITEM / f"{src}.png", ramp_)
    solid = solid_of(im)
    inner = solid - edge_of(solid)
    for x, y in sorted(inner):
        c = im.getpixel((x, y))[:3]
        if fat and h(x, y, salt) < 0.22 and (x + y + int(h(x // 2, y // 3, salt + 1) * 3)) % 3 == 0:
            put(im, x, y, lerp(c, fat, 0.65))  # marbling: short pale streaks
        elif fibre and (x - y) % 2 == 0 and h(x, y, salt + 2) < 0.6:
            put(im, x, y, shade(c, fibre))  # stringy grain
    return im


def lizard_tail(cooked):
    if cooked:  # charred and curled into a hook
        pts = [(3.0, 13.0), (6.5, 12.0), (10.0, 9.5), (12.0, 6.0), (11.0, 3.0), (8.5, 2.5), (7.8, 4.6)]
        band, base, dark = (40, 26, 16), (138, 94, 52), (70, 44, 24)
    else:
        pts = [(3.0, 12.5), (6.0, 10.0), (9.5, 7.0), (12.0, 4.5), (13.8, 2.2)]
        band, base, dark = (84, 56, 30), (220, 192, 138), (156, 124, 82)

    def width(t):
        return 5.6 - 4.2 * t if not cooked else 5.0 - 3.4 * t

    def colour(t, s, x, y):
        if t < 0.05:
            return (214, 120, 112) if s < 0.2 else (170, 80, 76)  # the cut end
        if t < 0.1 and not cooked:
            return (196, 98, 92) if abs(s) < 0.5 else (240, 210, 190)
        c = lerp(base, dark, 0.5 + 0.5 * s)
        if (x + y) % 2 == 0:
            c = shade(c, 0.9)  # scale checker
        if int(t * 8 + 0.3) % 2 == 1:
            c = lerp(c, band, 0.75)  # crossbands
        if cooked and h(x, y, 61) < 0.25:
            c = shade(c, 0.65)  # charred
        if s < -0.55 and h(x, y, 62) < 0.6:
            c = shade(c, 1.2)
        return c

    def outline(t, x, y):
        if t < 0.06:
            return (110, 36, 34) if not cooked else (70, 30, 20)
        return (64, 44, 26) if not cooked else (30, 18, 10)

    return tube(pts, width, colour, outline)


# ---------------------------------------------------------------- bird cuts (o outline, d dark, m mid, l light, h highlight, s skin, b bone, B bone shade)

CUTS = {
    "hornbill": [  # a big wing: the meaty upper joint with its bone knob, the forearm up to the pointed tip
        "................",
        "............oo..",
        "...........oslo.",
        "..........oshmo.",
        "..........ohlmo.",
        ".........oshlmdo",
        ".........ohlmmdo",
        ".........olmmdo.",
        "....ooooolmmmdo.",
        "..oosshhllmmmdo.",
        ".obshhllmmmmddo.",
        "obBshlmmmmmddo..",
        "oBbommmmmdddo...",
        ".oo.oddddddoo...",
        ".....oooooo.....",
        "................",
    ],
    "jungle_fowl": [  # a pair of small drumsticks, crossed
        "................",
        "................",
        "..ooo......ooo..",
        ".oshlo....oshlo.",
        ".ohllmo..ohllmo.",
        ".olmmdo..olmmdo.",
        ".ommddo..ommddo.",
        "..odddo..odddo..",
        "...odo....odo...",
        "....obo..obo....",
        ".....oboobo.....",
        "......obBo......",
        ".....oBoobo.....",
        "....obo..oBo....",
        "....oo....oo....",
        "................",
    ],
    "peafowl": [  # a long breast fillet with its skin
        "................",
        "................",
        "...........oo...",
        ".........oossoo.",
        ".......oosshhso.",
        ".....oosshhllmo.",
        "....oshhhllmmdo.",
        "...oshllllmmmdo.",
        "..oshllmmmmmddo.",
        "..oslmmmmmmddo..",
        ".oslmmmmmmddo...",
        ".osmmmmmddoo....",
        ".odmmmddoo......",
        ".oddddoo........",
        "..oooo..........",
        "................",
    ],
}
RAW = {
    "hornbill": dict(o="#3e1216", d="#6a1e24", m="#8e3034", l="#b0524c", h="#cc7c6e", s="#dcc0a4", b="#eee6d2", B="#c8bca4"),
    "jungle_fowl": dict(o="#5a2226", d="#a8484c", m="#cc6e6a", l="#e4988e", h="#f2c0b2", s="#f0d8bc", b="#eee6d2", B="#c8bca4"),
    "peafowl": dict(o="#4e1a1e", d="#94343a", m="#b84e50", l="#d47a74", h="#eaa89e", s="#ecd0b0", b="#eee6d2", B="#c8bca4"),
}
COOKED = {
    "hornbill": dict(o="#341a0c", d="#6a361a", m="#94522a", l="#b4723a", h="#d29a58", s="#c88c48", b="#e8dcc0", B="#bcae90"),
    "jungle_fowl": dict(o="#44240f", d="#8a4e22", m="#b67234", l="#d4964c", h="#ecbe74", s="#e0a858", b="#e8dcc0", B="#bcae90"),
    "peafowl": dict(o="#3c200e", d="#7a4220", m="#a8622e", l="#c88440", h="#e2ac62", s="#d89a4c", b="#e8dcc0", B="#bcae90"),
}

# plucked: o outline, s skin, l light, d shade, f follicle, q quill stubs, k bill, K bill shade, c casque, e eye, r red, g leg, G leg shade
PSKIN = dict(o="#7e5246", s="#ecc4a6", l="#f6dcc4", d="#c89478", f="#d4a488", q="#e8dcc0", e="#1c1410", g="#5a5650", G="#3e3a36")
PLUCKED = {
    "hornbill": ([  # dark grey-pink skin, the great yellow casque-bill
        "................",
        "................",
        "..oooo..........",
        ".occcCoo........",
        "kkkkkKeso.......",
        ".okkkKoslooooo..",
        "..ooooosllsssso.",
        ".....osllssssfdo",
        ".....oslsfsssfdo",
        ".....osssssfssdo",
        ".....osfsssssddo",
        "......osssssddqq",
        ".......oddddoqqo",
        "........ogoog...",
        "........oGGoGG..",
        "................",
    ], dict(PSKIN, s="#cfa898", l="#e2c0b2", d="#a87e70", f="#b48a7c", o="#5e3e36",
            k="#f2cc4a", K="#c89428", c="#f6dc78", C="#e09a34")),
    "jungle_fowl": ([  # small and trim, red comb and wattle, slate legs
        "................",
        "................",
        "....rr..........",
        "...rrro.........",
        "..oesso.........",
        ".kossro.........",
        "..orro..........",
        "...osloooooo....",
        "..osllssssssoqq.",
        "..oslsfssfsdoq..",
        "..osssssssfdoq..",
        "...osfssssddo...",
        "....oddddddo....",
        ".....oogogo.....",
        "......oGoG......",
        "................",
    ], dict(PSKIN, s="#eec8a4", l="#f8e0c4", d="#c89a76", f="#d6aa86", k="#c8b48a", r="#d02e28",
            g="#7a8088", G="#5a6068")),
    "peafowl": ([  # small head with crest stubs on a long neck, big body, long quill stubs of the train
        "..q.q...........",
        "...qq...........",
        "..ooo...........",
        ".kesso..........",
        "..osso..........",
        "...oso..........",
        "...osdo.........",
        "...osdo.........",
        "...oslooooooo...",
        "..osllssssssdo..",
        "..oslsfsssfssdoo",
        "..osssssfsssddqq",
        "...osfssssddooqq",
        "....oddssddo..oq",
        ".....oogoogo....",
        ".....oGG.oGG....",
    ], dict(PSKIN, k="#9a8e7e", g="#8e8478", G="#6a6258")),
}

EGG_JF = ([  # small, pointed, cream-tan with a few freckles
    "................",
    "................",
    "................",
    "................",
    ".......oo.......",
    "......owwo......",
    ".....owhlwo.....",
    ".....owlwwo.....",
    "....owwwwxwo....",
    "....owxwwwdo....",
    "....owwwwwdo....",
    "....odwwwxdo....",
    ".....odwwdo.....",
    "......oooo......",
    "................",
    "................",
], dict(o="#7a6040", w="#e2c89a", l="#eedab4", h="#faf0d8", d="#c4a676", x="#b08a5a"))


# ---------------------------------------------------------------- hides

def hide_mask():
    """The house raw_hide silhouette (gen_food_textures.HIDE, centred and shifted down one row)."""
    rows = ["..XXX....XXX....", "..XXXXXXXXXXX...", "...XXXXXXXXXX...", ".XXXXXXXXXXXXX..", "XXXXXXXXXXXXXXX.",
            ".XXXXXXXXXXXXXX.", "..XXXXXXXXXXXX..", "..XXXXXXXXXXX...", "..XXXXXXXXXXXX..", ".XXXXXXXXXXXXXX.",
            "XXXXXXXXXXXXXXX.", ".XXXXXXXXXXXXX..", "..XXXXXXXXXXX...", "..XXX.....XXX...", "................",
            "................"]
    rows = [r[:15].rjust(16, ".") for r in rows]
    rows = rows[-1:] + rows[:-1]
    return {(x, y) for y in range(16) for x in range(16) if rows[y][x] == "X"}


def hide_item(fur_at, salt, fold=None, lip=False, raw_edge=0.55):
    """fur_at(x, y) -> colour of the hair side. fold: pixels with x + y > fold show the flesh side folded back.
    lip: a row of raw flesh under the bottom edge shows the hide's thickness."""
    mask = hide_mask()
    edge = edge_of(mask)
    im = Image.new("RGBA", (16, 16), CLEAR)
    if lip:
        for x, y in mask:
            if (x, y + 1) not in mask and y + 1 < 16:
                put(im, x, y + 1, ramp(RAW_FLESH, 0.1 + 0.4 * h(x, y, salt + 9)))
    for x, y in mask:
        if fold is not None and x + y > fold:
            c = ramp(RAW_FLESH, 0.4 + 0.5 * h(x, y, salt))
            if h(x // 2, y, salt + 1) < 0.3:
                c = lerp(c, FAT, 0.7)
            if x + y == fold + 1:
                c = FAT
            if (x, y) in edge:
                c = shade(c, 0.75)
        elif (x, y) in edge:
            c = ramp(RAW_FLESH, h(x, y, salt + 2)) if h(x, y, salt + 3) < raw_edge else shade(fur_at(x, y), 0.72)
        else:
            c = fur_at(x, y)
            if fold is not None and x + y == fold:
                c = shade(c, 0.7)  # the shadow of the fold
        put(im, x, y, c)
    return im


def onager_fur(x, y):
    d = abs(x - 8)
    base = lerp((196, 160, 112), L_CREAM if d > 5 else (210, 178, 130), smooth((d - 2) / 5))
    if d <= 1 and x != 9 and 1 <= y <= 13:
        base = (92, 64, 42)  # the dorsal stripe
    if y in (2, 3) and 6 <= x <= 10:
        base = lerp(base, (92, 64, 42), 0.35)  # darker mane ridge at the neck
    return fur(base, x, y, 501, 0.07)


def tapir_fur(x, y):
    saddle = 5 <= y <= 10 and (2 <= x <= 13)
    c = (232, 226, 212) if saddle else (34, 30, 30)
    if saddle and y in (5, 10):
        c = lerp(c, (34, 30, 30), 0.35)
    return fur(c, x, y, 511, 0.06)


def buffalo_fur(x, y):
    c = lerp((104, 108, 114), (70, 72, 78), (x + y) / 28)
    if h(x, y, 521) < 0.12:
        c = (46, 46, 50)  # sparse coarse hair
    elif h(x, y // 2, 522) < 0.1:
        c = shade(c, 1.18)  # creases in the thick skin
    return c


def lizard_skin():
    """Split and spread flat: the head at the top, splayed toes, the tail tapering down; dark crossbands."""
    rows = [
        "................",
        ".......oo.......",
        "......oxxo......",
        "......oxxo......",
        ".kk..oxxxxo..kk.",
        "..koooxxxxoook..",
        ".....oxxxxo.....",
        ".....oxxxxo.....",
        ".....oxxxxo.....",
        "..koooxxxxoook..",
        ".kk..oxxxxo..kk.",
        "......oxxo......",
        "......oxxo......",
        ".......xxo......",
        ".......oxo......",
        "........o.......",
    ]
    band = (92, 62, 34)

    def xx(x, y):
        c = lerp((228, 202, 150), (178, 146, 98), abs(x - 7.5) / 2.5 + 0.1 * (y > 10))
        if (x + y) % 2:
            c = shade(c, 0.9)  # scales
        if y in (3, 6, 9, 12) or (y == 14):
            c = lerp(c, band, 0.8)
        return c
    return grid(rows, {"x": xx, "o": lambda x, y: (96, 70, 42) if h(x, y, 531) < 0.7 else (130, 100, 64),
                       "k": (110, 82, 50)})


# ---------------------------------------------------------------- leopard coat and pelts

def rosette(x, y, salt, px=4, py=3):
    """'core', 'ring' or None: open leopard rosettes on a staggered lattice."""
    for j in range(y // py - 1, y // py + 2):
        for i in range(x // px - 1, x // px + 2):
            cx = i * px + (j % 2) * (px // 2) + int(h(i, j, salt) * 2)
            cy = j * py + int(h(i, j, salt + 1) * 2)
            dx, dy = x - cx, y - cy
            if dx == dy == 0:
                return "core"
            if abs(dx) + abs(dy) == 1 and h(x, y, salt + 2) < 0.78:
                return "ring"
    return None


def leopard_coat(x, y, d, salt, px=4, py=3):
    """d 0 (spine) .. 1 (belly edge); px, py: rosette spacing."""
    base = lerp(L_DEEP, L_GOLD, smooth(d / 0.5)) if d < 0.75 else lerp(L_GOLD, L_CREAM, (d - 0.75) / 0.25)
    r = rosette(x, y, salt, px, py)
    if r == "ring":
        return fur(L_SPOT, x, y, salt + 3, 0.1)
    if r == "core":
        return lerp(L_DEEP, L_SPOT, 0.25) if d < 0.8 else L_SPOT  # pale belly gets solid spots
    return fur(base, x, y, salt + 4, 0.06)


# wolf_pelt silhouette (gen_predator_textures.PELT_MASK) with round ears and the long tail curled to the side
PELT_MASK = [
    "....oo...oo.....",
    "....oxxxxxo.....",
    "....xExxxEx.....",
    ".....xsNsx......",
    "..oo.xxxxx.oo...",
    ".oxxxxxxxxxxxo..",
    "..oxxxxxxxxxo...",
    "....xxxxxxx.....",
    "....xxxxxxx.....",
    "....xxxxxxx.....",
    "..oxxxxxxxxxo...",
    ".oxxxxxxxxxxxo..",
    "..oo.xxxxx.oo...",
    ".......tt.......",
    "........tt...kk.",
    ".........tttk...",
]


def leopard_pelt():
    im = Image.new("RGBA", (16, 16), CLEAR)
    mask = {(x, y) for y, row in enumerate(PELT_MASK) for x, c in enumerate(row) if c != "."}
    for x, y in mask:
        c = PELT_MASK[y][x]
        edge = any((x + dx, y + dy) not in mask for dx, dy in N4)
        if c == "E":
            col = EYE  # eye holes
        elif c == "N":
            col = (150, 92, 80)
        elif c == "s":
            col = L_CREAM
        elif c == "t":
            col = L_SPOT if (x + y) % 2 else lerp(L_GOLD, L_CREAM, 0.3)  # ringed tail
        elif c == "k":
            col = L_SPOT
        elif y == 0 or (y == 1 and c == "o"):
            col = (40, 30, 22)  # dark backs of the ears
        elif c == "o":
            col = (86, 58, 30) if h(x, y, 541) < 0.5 else lerp(L_GOLD, L_CREAM, 0.5)
        elif edge and y < 13:
            col = (86, 58, 30) if h(x, y, 544) < 0.6 else lerp(L_DEEP, L_GOLD, 0.5)
        elif y <= 3:
            col = L_SPOT if h(x, y, 543) < 0.2 else fur(L_DEEP, x, y, 545, 0.06)  # small spots on the head
        else:
            col = leopard_coat(x, y, min(1.0, abs(x - 7) / 4.5), 542, 3, 3)
        put(im, x, y, col)
    return im


def raw_item(src, salt):
    """gen_pelt_rack_textures.raw_item: fur wet and dark, a raw rim, the bottom right corner folded back."""
    px = src.load()
    out = Image.new("RGBA", (16, 16), CLEAR)
    solid = solid_of(src)
    edge = edge_of(solid)
    fold = 21
    for x, y in solid:
        c = px[x, y][:3]
        if x + y > fold:
            col = ramp(RAW_FLESH, 0.35 + 0.5 * h(x, y, salt))
            if h(x // 2, y, salt + 1) < 0.3:
                col = lerp(col, FAT, 0.7)
            if x + y == fold + 1:
                col = FAT
        elif (x, y) in edge and c != L_AMBER and h(x, y, salt + 2) < 0.45:
            col = ramp(RAW_FLESH, h(x, y, salt + 3))
        else:
            col = shade(lerp(c, (60, 40, 34), 0.12), 0.82)
            if h(x, y, salt + 4) < 0.05:
                col = shade(col, 1.3)
        put(out, x, y, col)
    for x, y in solid:
        if x + y == fold and h(x, y, salt + 5) < 0.35:
            put(out, x, y, (110, 20, 22))
    return out


RUG_MASK = [  # head north: e ears, H head, E eyes, s muzzle, N nose, o edge, x coat, w claws, t tail, k tail tip
    "....ee....ee....",
    "....eHHHHHHe....",
    ".....HEHHEH.....",
    ".....HHssHH.....",
    "......sNNs......",
    "w.oo.oxxxxo.oo.w",
    "oxxxxxxxxxxxxxxo",
    ".ooxxxxxxxxxxoo.",
    "...oxxxxxxxxo...",
    "...oxxxxxxxxo...",
    "..ooxxxxxxxxoo..",
    ".oxxxxxxxxxxxxo.",
    "oxxooxxxxxxooxxo",
    "w.o.ooxxxxoo.o.w",
    ".......tt.......",
    "........ttttkk..",
]


def leopard_rug():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(RUG_MASK):
        assert len(row) == 16, row
        for x, c in enumerate(row):
            if c == ".":
                continue
            if c == "e":
                col = (40, 30, 22)
            elif c == "H":
                col = L_SPOT if h(x, y, 551) < 0.22 else fur(L_GOLD, x, y, 552, 0.06)
            elif c == "E":
                col = L_AMBER
            elif c == "s":
                col = L_CREAM
            elif c == "N":
                col = (150, 92, 80)
            elif c == "w":
                col = (236, 228, 210) if h(x, y, 553) < 0.5 else (90, 70, 50)
            elif c == "o":
                col = (54, 38, 24) if h(x, y, 554) < 0.4 else lerp(L_GOLD, L_CREAM, 0.6)
            elif c == "t":
                col = L_SPOT if (x + y) % 2 else lerp(L_GOLD, L_CREAM, 0.3)
            elif c == "k":
                col = L_SPOT
            else:
                col = leopard_coat(x, y, min(1.0, abs(x - 7.5) / 6.5), 555)
            put(im, x, y, col)
    return im


# the wolf's rack mask (gen_pelt_rack_textures.WOLF), muzzle all pale
RACK = ["T...NN...T",
        "T..ssss..T",
        "T.eExxEe.T",
        "wwoexxeoww",
        "oxxxxxxxxo",
        ".oxxxxxxo.",
        ".oxxxxxxo.",
        "oxxxxxxxxo",
        "wwoxxxxoww",
        "T...tt...T",
        "T...kk...T"]
assert all(len(r) == 10 and r == r[::-1] for r in RACK)


def rack_fur(x, y, dry, salt):
    col = leopard_coat(x, y, min(1.0, abs(x - 4.5) / 4.5), salt)
    if dry and col not in (L_SPOT,) and h(x, y, salt + 9) < 0.12:
        col = lerp(col, L_CREAM, 0.5)  # fluffed pale tips
    return col


# ---------------------------------------------------------------- parts

TORTOISE_SHELL = [  # side view: the domed carapace, scutes with pale areolae, marginal scutes and the plastron rim
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "...ooaAAAacoo...",
    "..osaaAAAacsdo..",
    ".osAasaaacsAcdo.",
    ".oaAasssssscddo.",
    ".oasaaAacsAcsdo.",
    ".oasaaaccsccsdo.",
    ".ossssssssssssо.".replace("о", "o"),
    ".omnmnmnmnmnmno.",
    ".oppppppPPPPPPo.",
    "..oooooooooooo..",
    "................",
    "................",
]
SHELL_PAL = {"o": (40, 30, 16), "s": (62, 46, 22), "a": (180, 150, 72), "A": (232, 206, 126), "c": (146, 118, 52),
             "d": (110, 88, 38), "m": (140, 110, 54), "n": (74, 54, 26), "p": (210, 184, 108), "P": (170, 146, 82)}


def tortoise_shell():
    return grid(TORTOISE_SHELL, SHELL_PAL)


def buffalo_horn():
    """A big crescent of slate horn, ridged across, the open base end pale and fibrous."""
    pts = [(3.0, 13.5), (7.0, 13.0), (11.0, 11.0), (13.2, 7.5), (12.8, 4.0), (10.5, 1.8), (8.0, 1.2)]

    def width(t):
        return 6.0 - 4.8 * t

    def colour(t, s, x, y):
        if t < 0.05:
            return (176, 160, 136) if s < 0 else (138, 122, 100)  # the open end
        c = lerp((132, 130, 132), (40, 38, 40), 0.5 + 0.5 * s)
        if t > 0.8:
            c = lerp(c, (26, 24, 24), (t - 0.8) / 0.2)  # black tip
        if (t * 11) % 1 < 0.3 and t < 0.85:
            c = shade(c, 0.7)  # ridges
        return c

    return tube(pts, width, colour, lambda t, x, y: (60, 50, 40) if t < 0.04 else (20, 18, 18))



CASQUE = [
    "................",
    "................",
    "................",
    ".oo.............",
    ".oCoo...........",
    "..oCCooooo......",
    "..oCYYYYYYoooo..",
    "...oCYYYYYYYARo.",
    "...oSSSSSSSSSRo.",
    "..oBBWBBBBBBbRo.",
    ".oBWBBBBBbbbbRo.",
    "oBBbbbbbbooooo..",
    "obbooooo........",
    "oo..............",
    "................",
    "................",
]
CASQUE_PAL = {"o": "#4a3214", "C": "#fff0a8", "Y": "#f4cc4a", "A": "#e88a2a", "R": "#a63c22", "S": "#c87a26",
              "B": "#f2e2b0", "W": "#fffbe8", "b": "#d0b47c"}

FEATHER = [
    "..........ooo...",
    ".........oGBGo..",
    "........oGbKbGo.",
    "........oBKKKBo.",
    "........oGbKbGo.",
    ".......ogoGBGo..",
    "......oggsooo...",
    ".....oggso......",
    "....oggso.......",
    "...ogsgo........",
    "..ogsgo.........",
    "..ogso..........",
    ".osgo...........",
    ".oso............",
    "os.o............",
    "o...............",
]
FEATHER_PAL = {"o": "#1c2a1c", "G": "#c8a238", "B": "#2a8a6a", "b": "#2a5cb8", "K": "#141a3a",
               "g": "#4a8a3a", "s": "#e2d8b0"}


# ---------------------------------------------------------------- made items

PACK_BASKETS = [
    "................",
    ".....oooooo.....",
    "....ossssssO....",
    "...oso....oSO...",
    "..oso......oSO..",
    "oooooo....oooooo",
    "oLwLwo....oLwLwo",
    "owLwLo....owLwLo",
    "oDDDDo....oDDDDo",
    "oLwLwossssoLwLwo",
    "owLwLoSSSSowLwLo",
    "oLwLwo....oLwLwo",
    "owLwLo....owLwLo",
    "oDDDDo....oDDDDo",
    ".oooo......oooo.",
    "................",
]


def pack_baskets():
    pal = {"o": (52, 34, 18), "L": (176, 130, 72), "w": (134, 94, 48), "D": (98, 66, 34),
           "s": (176, 138, 98), "S": (130, 98, 66), "O": (70, 50, 32)}
    im = grid(PACK_BASKETS, pal)
    for y, row in enumerate(PACK_BASKETS):  # shade the far side of each basket
        for x, ch in enumerate(row):
            if ch in "Lw" and x in (4, 14):
                put(im, x, y, shade(pal[ch], 0.82))
    return im


SHELL_RATTLE = [
    "................",
    "........ooooo...",
    "......oosaAsoo..",
    ".....osAsaasAdo.",
    "....oaasssssddo.",
    "....osaaAsaAsdo.",
    "....oddddsdddoo.",
    "....mnmnmnmnmo..",
    ".....ppPPppPo...",
    "......oTtTo.....",
    ".....oTtTo......",
    "....owwo........",
    "...owwo.........",
    "..owwo..........",
    ".oWwo...........",
    "..oo............",
]


def shell_rattle():
    pal = {"o": (40, 30, 16), "s": (58, 42, 22), "a": (176, 146, 70), "A": (230, 204, 124), "d": (124, 100, 44),
           "m": (140, 110, 54), "n": (58, 42, 22), "p": (206, 180, 104), "P": (168, 144, 80),
           "T": TWINE, "t": TWINE_DK, "w": (138, 98, 56), "W": (98, 68, 38)}
    return grid(SHELL_RATTLE, pal)


# ---------------------------------------------------------------- spawn eggs

def over(im, pixels):
    for x, y, c in pixels:
        put(im, x, y, c)
    return im


def ears_mirror(left):
    return left + [(15 - x, y, c) for x, y, c in left]


def onager_egg():
    D, C = "#5a3e28", "#efe2c8"
    ears = ears_mirror([(4, 0, D), (4, 1, "#c8a676"), (5, 1, "#e6cfa6"), (4, 2, "#c8a676")])
    stripe = [(7, y, D) for y in range(3, 12)] + [(8, y, D) for y in range(3, 6)]
    belly = [(4, 11, C), (5, 12, C), (10, 12, C), (11, 11, C)]
    muzzle = [(6, 12, C), (7, 12, C), (8, 12, C), (9, 12, C), (7, 13, "#3a2a1e"), (8, 13, "#3a2a1e")]
    im = egg(["#ead2a6", "#d8b98a", "#c4a070", "#a48258", "#76583a", "#523c26"], (), [(x, y) for x, y, _ in ears],
             stripe + belly + muzzle, shift=1)
    return over(im, ears)


def tortoise_egg():
    S = "#4a3418"
    seams = [(x, 6, S) for x in range(3, 13)] + [(x, 10, S) for x in range(3, 13)]
    seams += [(5, y, S) for y in range(3, 6)] + [(10, y, S) for y in range(3, 6)] + [(7, y, S) for y in range(7, 10)]
    seams += [(4, y, S) for y in range(11, 13)] + [(11, y, S) for y in range(11, 13)]
    areolae = [(7, 4, "#e6cc7a"), (3, 8, "#e6cc7a"), (12, 8, "#d4b866"), (9, 8, "#e6cc7a"), (7, 12, "#d4b866")]
    return egg(["#c8b060", "#ae9646", "#947c36", "#76602a", "#4e3e1c", "#342812"], (), (), seams + areolae)


def lizard_egg():
    B = "#5a3e22"
    bands = [(x, y, B) for y in (4, 8, 12) for x in range(EGG_ROWS[y][0] + 1, EGG_ROWS[y][1])]
    bands += [(x, 5, "#7a5a36") for x in range(4, 12, 3)] + [(x, 9, "#7a5a36") for x in range(3, 13, 3)]
    eyes = [(5, 2, "#1a1410"), (10, 2, "#1a1410")]
    return egg(["#f0dca8", "#e0c68a", "#c8aa6e", "#a88a54", "#7a6038", "#544024"], (), (), bands + eyes)


def buffalo_egg():
    H, Hd = "#d0c8bc", "#8a8278"
    horns = ears_mirror([(0, 3, Hd), (1, 2, H), (2, 1, H), (3, 1, H), (4, 2, Hd)])
    paint = [(6, 12, "#7a7a80"), (7, 12, "#8e8e94"), (8, 12, "#8e8e94"), (9, 12, "#7a7a80"),
             (7, 13, "#1a1a1c"), (8, 13, "#1a1a1c"), (5, 7, "#141416"), (10, 7, "#141416")]
    spots = [(4, 10, "#2a2a2e"), (11, 10, "#2a2a2e"), (6, 4, "#2a2a2e"), (9, 4, "#2a2a2e")]
    im = egg(["#8a8e96", "#70747c", "#5a5e66", "#464950", "#2e3036", "#1c1e22"], spots, (), paint, shift=1)
    return over(im, horns)


def tapir_egg():
    W = "#ece6d8"
    saddle = [(x, y, W if y > 6 else "#d4cebe") for y in range(6, 11) for x in range(EGG_ROWS[y][0] + 1, EGG_ROWS[y][1])]
    snout = [(7, 14, "#2a2626"), (8, 14, "#2a2626"), (7, 15, "#3a3434"), (6, 13, "#2a2626"), (9, 13, "#2a2626")]
    ears = [(4, 1, W), (11, 1, W)]
    im = egg(["#4a4646", "#363232", "#2a2626", "#1e1b1b", "#141212", "#0a0909"], (), [(7, 15), (8, 15)],
             saddle + ears, shift=0)
    return over(im, snout[:2] + snout[3:])


def leopard_egg():
    K = "#1e1610"
    rosettes = []
    for cx, cy in ((5, 4), (10, 5), (4, 9), (8, 8), (11, 10), (6, 12)):
        rosettes += [(cx - 1, cy, K), (cx + 1, cy, K), (cx, cy - 1, K), (cx, cy, "#a8742c")]
    ears = [(4, 1, K), (11, 1, K)]
    face = [(6, 7, "#d0b040"), (9, 7, "#d0b040")]
    muzzle = [(7, 13, "#f2e6cc"), (8, 13, "#f2e6cc"), (7, 14, "#8a5a4a"), (8, 14, "#8a5a4a")]
    return egg(["#f0c878", "#e0ae5c", "#c8944a", "#a87638", "#7a5428", "#52381a"], rosettes, [(x, y) for x, y, _ in ears],
               ears + face + muzzle)


def hornbill_egg():
    Y, O, W = "#f6d24a", "#e08a2a", "#f4eee0"
    casque = [(6, 0, Y), (7, 0, Y), (8, 0, Y), (9, 0, O), (5, 1, Y), (6, 1, Y), (7, 1, Y), (8, 1, Y), (9, 1, Y), (10, 1, O)]
    bill = [(7, 2, Y), (8, 2, Y), (6, 2, "#c89428"), (9, 2, "#c89428")]
    paint = casque + bill + [(5, 4, "#c83a2a"), (10, 4, "#c83a2a")]
    paint += [(x, 13, W) for x in range(5, 11)] + [(x, 12, W) for x in range(4, 12, 2)]
    spots = [(4, 8, "#3a3a3e"), (11, 9, "#3a3a3e"), (7, 10, "#3a3a3e")]
    return egg(["#3e3c40", "#2e2c30", "#222024", "#18171a", "#0e0d10", "#060508"], spots,
               [(x, y) for x, y, _ in casque], paint)


def jungle_fowl_egg_spawn():
    R, G, Gd = "#d02e28", "#2e6a3a", "#1a3a22"
    comb = [(6, 0, R), (7, 0, R), (9, 0, R), (6, 1, R), (7, 1, R), (8, 1, R), (9, 1, R)]
    paint = comb + [(7, 4, "#f0a020"), (8, 4, "#f0a020"), (6, 5, "#1a1410"), (7, 6, R), (8, 6, R)]
    paint += [(x, y, G if (x + y) % 2 else Gd) for y in (11, 12, 13) for x in range(EGG_ROWS[y][0] + 1, EGG_ROWS[y][1])]
    spots = [(4, 7, "#f0a020"), (11, 8, "#f0a020"), (5, 9, "#8a2a14"), (10, 10, "#8a2a14")]
    return egg(["#f08a3a", "#dc6c28", "#c0541e", "#9a4016", "#6a2a0e", "#461a08"], spots,
               [(x, y) for x, y, _ in comb], paint)


def peafowl_egg():
    crest = [(6, 0, "#2a5cb8"), (8, 0, "#2a5cb8"), (10, 0, "#2a5cb8"), (7, 1, "#1a3a7a"), (9, 1, "#1a3a7a"), (8, 1, "#1a3a7a")]
    eyes = []
    for cx, cy in ((5, 8), (10, 9), (7, 12), (11, 13 - 1)):
        eyes += [(cx, cy, "#141a3a"), (cx + 1, cy, "#c8a238"), (cx, cy + 1, "#2a8a6a")]
    paint = crest + eyes + [(5, 3, "#f4f0e0"), (6, 4, "#141410"), (10, 3, "#f4f0e0")]
    return egg(["#4a8ae0", "#2e6ac8", "#2054a8", "#18408a", "#10306a", "#0a2048"], (),
               [(x, y) for x, y, _ in crest], paint)


# ---------------------------------------------------------------- build

def items():
    out = {
        "onager_meat": meat("mutton_scraps", ONAGER_RAW, salt=401),
        "cooked_onager_meat": meat("cooked_mutton_scraps", ONAGER_COOKED, salt=402),
        "tortoise_meat": meat("chicken_scraps", TORTOISE_RAW, salt=403),
        "cooked_tortoise_meat": meat("cooked_chicken_scraps", TORTOISE_COOKED, salt=404),
        "buffalo_meat": meat("beef_scraps", BUFFALO_RAW, fat=(226, 196, 186), salt=405),
        "cooked_buffalo_meat": meat("cooked_beef_scraps", BUFFALO_COOKED, fat=(168, 128, 88), salt=406),
        "tapir_meat": meat("porkchop_scraps", TAPIR_RAW, salt=407),
        "cooked_tapir_meat": meat("cooked_porkchop_scraps", TAPIR_COOKED, salt=408),
        "leopard_meat": meat("rabbit_scraps", LEOPARD_RAW, fibre=0.78, salt=409),
        "cooked_leopard_meat": meat("cooked_rabbit_scraps", LEOPARD_COOKED, fibre=0.78, salt=410),
        "lizard_tail": lizard_tail(False),
        "cooked_lizard_tail": lizard_tail(True),
    }
    for bird in CUTS:
        out[f"raw_{bird}"] = from_grid(CUTS[bird], RAW[bird])
        out[f"cooked_{bird}"] = from_grid(CUTS[bird], COOKED[bird])
        rows, pal = PLUCKED[bird]
        out[f"plucked_{bird}"] = from_grid(rows, pal)
    pelt = leopard_pelt()
    out.update({
        "raw_onager_hide": hide_item(onager_fur, 571),
        "raw_tapir_hide": hide_item(tapir_fur, 572, lip=True, raw_edge=0.35),
        "raw_buffalo_hide": hide_item(buffalo_fur, 573, fold=20, lip=True, raw_edge=0.3),
        "raw_leopard_pelt": raw_item(pelt, 574),
        "leopard_pelt": pelt,
        "lizard_skin": lizard_skin(),
        "tortoise_shell": tortoise_shell(),
        "buffalo_horn": buffalo_horn(),
        "hornbill_casque": from_grid(CASQUE, CASQUE_PAL),
        "peacock_feather": from_grid(FEATHER, FEATHER_PAL),
        "jungle_fowl_egg": from_grid(*EGG_JF),
        "pack_baskets": pack_baskets(),
        "shell_rattle": shell_rattle(),
        "leopard_rug": leopard_rug(),
        "onager_spawn_egg": onager_egg(),
        "tortoise_spawn_egg": tortoise_egg(),
        "desert_lizard_spawn_egg": lizard_egg(),
        "water_buffalo_spawn_egg": buffalo_egg(),
        "tapir_spawn_egg": tapir_egg(),
        "leopard_spawn_egg": leopard_egg(),
        "hornbill_spawn_egg": hornbill_egg(),
        "jungle_fowl_spawn_egg": jungle_fowl_egg_spawn(),
        "peafowl_spawn_egg": peafowl_egg(),
    })
    return out


def blocks():
    return {"leopard_rug": leopard_rug()}


def main():
    for folder, made in ((ITEM, items()), (BLOCK, blocks())):
        for name, im in made.items():
            assert im.size == (16, 16) and im.getbbox(), name
            im.save(folder / f"{name}.png")
    print("fauna warm item textures ok")


if __name__ == "__main__":
    main()
