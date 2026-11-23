# Flora phase 1 textures (docs/flora-and-food-plan.md; data in gen_flora.py).
# Flowers: hand-drawn 16x16 grids (cross models like vanilla's flowers), each with a leaves look for out of season (the
# flower letters mapped to seed heads, buds or nothing). Magic flowers also get a _glow layer: only their blossom, drawn
# full-bright by the model. Thicket bushes: painted leaf masses (one leaf layout per kind, shared by every stage, so the
# fruit sits on the same twigs from flower to ripe), bare winter twigs, and the branch planes inside. Berry items: grids.
# usage: python scripts/gen_flora_textures.py [preview_dir]
import random
import sys
from pathlib import Path

from PIL import Image

RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def hexc(h, a=255):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def grid(rows, pal, remap=None):
    """rows of letters -> image; remap maps letters to other letters ('.' = clear) for the out-of-season look."""
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new("RGBA", (16, 16))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if remap and ch in remap:
                ch = remap[ch]
            if ch != ".":
                c = pal[ch]
                img.putpixel((x, y), c if isinstance(c, tuple) else hexc(c))
    return img


# ---------------------------------------------------------------- flowers
# each: pal, bloom grid, and remap (letters -> letters) giving the leaves look, or an explicit leaves grid
FLOWERS = {
    "pasque_flower": dict(  # nodding violet cups, yellow boss, silky stems; seed heads turn to silver feathers
        pal={"A": "b79ae6", "B": "8459c4", "C": "583192", "y": "f2d24a", "s": "8fa27a", "h": "d4dccb", "H": "aebaa4",
             "g": "6f9455", "d": "4a6c3a"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "....BAB.........",
            "...BAyAB...BAB..",
            "...CByBC..BAyAB.",
            "....CBC...CByBC.",
            ".....h.....CBC..",
            ".....s......h...",
            "..d.gsg..d..s.g.",
            ".gdg.sdg.gdgsgdg",
            "gd.dgsgdgd.dsdg.",
        ],
        remap={"A": "h", "B": "H", "C": ".", "y": "h"},
    ),
    "harebell": dict(  # pale blue bells nodding on thread stems
        pal={"A": "c4d2f6", "B": "8ea4ea", "C": "5d70c2", "s": "6f9a4a", "g": "80ae56", "d": "557f38"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            ".......s........",
            "......s.ss......",
            ".....s....s.....",
            "....AB.....s....",
            "...ABBC...AB....",
            "...BCBC..ABBC...",
            "....C.s..BCBC...",
            "......s...C.s...",
            "..s...s.....s...",
            "...s..s....s....",
            ".g.s.gs..g.s.g..",
            "gdg.sgdgdgsgdgd.",
        ],
        remap={"A": ".", "B": "g", "C": "d"},
    ),
    "lady_slipper": dict(  # yellow pouch, maroon twisted petals, broad ribbed leaves
        pal={"P": "f6d83e", "Y": "c99e1e", "R": "8a3028", "r": "5a1c1a", "s": "6e9a48", "g": "7fb055", "d": "4e7a34", "L": "a0cc70"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            ".....R....R.....",
            "......RrrR......",
            ".....R.rr.R.....",
            "....r..PP..r....",
            "......PPPY......",
            "......YPYY......",
            ".......YY.......",
            "..L....s....L...",
            "..gL...s...Lg...",
            "...gd.Ls.Ldg....",
            "....dgdsgdgd....",
        ],
        remap={"R": ".", "r": ".", "P": ".", "Y": "."},
    ),
    "sea_thrift": dict(  # pink pompoms on a dense cushion; papery straw heads after flowering
        pal={"A": "f8c0d8", "B": "e27eaa", "C": "b04e80", "s": "7a9a5a", "g": "6f9c52", "d": "4c7a3a", "L": "92be6a",
             "e": "d8c8a0", "f": "a89868"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "...ABA....AB....",
            "..ABBBA..ABBA...",
            "..BBCBB..BBCB...",
            "...CBC....CB.ABA",
            "....s......s.BCB",
            ".....s....s...s.",
            "..L.Ls.L.Ls.Ls..",
            ".LgLgdgLgdgLgdL.",
            "LgdgdgdgdgdgdgdL",
        ],
        remap={"A": "e", "B": "e", "C": "f"},
    ),
    "mountain_avens": dict(  # low mat of small dark leaves, white eight-petalled flowers with a golden boss
        pal={"W": "f8f8f0", "w": "d6dacd", "y": "e8c838", "s": "6c8a4c", "g": "5f8a48", "d": "3e6630", "L": "7ea65c"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "...W.W..........",
            "..WWwWW....W.W..",
            "..wWyWw...WWwWW.",
            "..WWwWW...wWyWw.",
            "...WsW.L..WWwWW.",
            ".L.gsLgdg..WsW..",
            "LgdLgdgdLgdLsdgL",
            "gdgdgdLgdgdgdgdg",
        ],
        remap={"W": ".", "w": ".", "y": "s"},
    ),
    "edelweiss": dict(  # woolly white star bracts round a cluster of buttons, grey felted leaves
        pal={"W": "f4f4ee", "w": "cdd2c6", "y": "d8d098", "s": "8a9878", "g": "9aaa88", "d": "6f8065"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "....W.W.........",
            "...WwywW...W.W..",
            "..WWyyWW..WwyW..",
            "...WwwW..WWyyWW.",
            "....WsW...WwwW..",
            ".....s.....sW...",
            "..g..s..g..s....",
            ".gdg.sg.dg.sgd..",
            "gdgdgsdgdgdsdgdg",
        ],
        remap={"W": ".", "w": "g", "y": "d"},
    ),
    "sand_verbena": dict(  # domes of small pink trumpets on sprawling stems
        pal={"A": "f8b0dc", "B": "da6cba", "C": "a84890", "y": "fff0f8", "s": "8a9a5a", "g": "8fae62", "d": "627f45"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "...ABA......ABA.",
            "..AByBA....AByBA",
            "..BBABB....BBABB",
            "...CBC..g...CBC.",
            "....s..gdg...s..",
            ".g..s.g..dg.s...",
            "gdgs.sgd..sss.g.",
            ".dssgd.dgss..sgd",
        ],
        remap={"A": ".", "B": "g", "C": "d", "y": "."},
    ),
    "desert_marigold": dict(  # bright yellow daisies over grey woolly leaves
        pal={"Y": "f8d830", "y": "dcac1e", "O": "c87818", "s": "98a488", "g": "a8b498", "d": "7e8a70"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "....Y.Y.........",
            "...YYyYY........",
            "..YyOOyY...Y.Y..",
            "...YYyYY..YYyYY.",
            "....YsY..YyOOyY.",
            ".....s....YYyYY.",
            ".....s.....YsY..",
            "..g..s.....s....",
            ".gd..s..g.s..g..",
            "gdgd.sgdgdsgdgd.",
            ".gdgdsdgdgsdgdg.",
        ],
        remap={"Y": ".", "y": ".", "O": "d"},
    ),
    "great_mullein_bottom": dict(  # felted grey rosette, a thick stalk rising
        pal={"g": "8a9a72", "G": "a8b590", "d": "627150", "s": "7c8a5e"},
        bloom=[
            ".......ss.......",
            "......gssg......",
            ".......ss.......",
            "......Gssg......",
            ".....Ggss.......",
            ".......ssgG.....",
            ".......ss.......",
            "....GGgssgG.....",
            "...GgggssggGG...",
            ".......ss.......",
            "..GGgg.ss.ggGG..",
            ".Gggd..ss..dggG.",
            "GGgdd.gssg.ddgGG",
            "Ggdd.GgssgG.ddgG",
            ".dd.GggssggG.dd.",
            "...Gggdssdggg...",
        ],
        remap={},
    ),
    "great_mullein_top": dict(  # a dense spike of yellow flowers, buds at the tip
        pal={"Y": "f6d83c", "y": "d6ac22", "o": "b8861a", "s": "7c8a5e", "g": "8a9a72", "G": "a8b590", "d": "627150"},
        bloom=[
            "................",
            ".......g........",
            "......gGg.......",
            "......gYsg......",
            ".....YyssyY.....",
            "......YssYy.....",
            ".....yYssyY.....",
            ".....YossYy.....",
            "....YyYssoyY....",
            ".....yYssYy.....",
            "....YyossyYY....",
            ".....YyssyY.....",
            "....gYsssYg.....",
            ".....gGssg......",
            ".......ss.......",
            "......Gssg......",
        ],
        remap={"Y": "g", "y": "d", "o": "d"},
    ),
    "desert_candle_bottom": dict(  # the open lower flowers of the spike, a slim stalk, strap leaves
        pal={"A": "ffd6a8", "B": "f4a868", "C": "cf7838", "s": "8a9a60", "g": "8aa268", "d": "5f7a45"},
        bloom=[
            ".......AB.......",
            "......BACB......",
            ".......BA.......",
            "......CAB.......",
            ".......sB.......",
            ".......s........",
            ".......s........",
            ".......s........",
            ".......s........",
            ".......s........",
            "...g...s....g...",
            "....g..s...g....",
            ".g..dg.s..gd..g.",
            "..gd.dgs.gd.dg..",
            "...gd.dsgd.dg...",
            "....gdgsdgdg....",
        ],
        remap={"A": ".", "B": ".", "C": "."},
    ),
    "desert_candle_top": dict(  # a tall candle of apricot stars opening from the bottom, pale buds at the tip
        pal={"A": "ffd6a8", "B": "f4a868", "C": "cf7838", "b": "ead2a4", "s": "8a9a60", "g": "8aa268", "d": "5f7a45"},
        bloom=[
            "................",
            ".......b........",
            ".......b........",
            "......bbb.......",
            ".......bb.......",
            "......bbb.......",
            "......Abb.......",
            "......bAB.......",
            ".....ABAb.......",
            "......BACA......",
            ".....AACBA......",
            "......CABB......",
            ".....BACAB......",
            "......ABCA......",
            ".....ACBAB......",
            "......BACB......",
        ],
        remap={"A": "g", "B": "g", "C": "d", "b": "g"},
    ),
    "fern_flower": dict(  # the flat picture (item): a rosette of fronds round a stalk with one small glowing flower
        pal={"W": "ffffff", "w": "fff6c8", "Y": "ffe46e", "O": "ffb43c", "R": "f07a2c", "s": "4f7a30", "g": "5f9a3f", "G": "86c456",
             "d": "3f6e2c", "b": "6ea848"},
        bloom=[
            "................",
            "................",
            ".......R........",
            "....RO.Y.OR.....",
            ".....OYWYO......",
            "....OYWWWYO.....",
            ".....OYWYO......",
            "....R.OsO.R.....",
            "...G...s...G....",
            "..Gg...s...gG...",
            ".G.gG..s..Gg.G..",
            "G..sgG.s.Ggs..G.",
            ".G..sgGsGgs..G..",
            "..gG.sdsds.Gg...",
            "....gG.s.Gg.....",
            "......dsd.......",
        ],
        leaves=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "......bb........",
            ".....b..b.......",
            "..G...bsb...G...",
            ".Gg....s....gG..",
            "..gG.G.s.G.Gg...",
            "G..gGg.s.gGg..G.",
            "gG..gd.s.dg..Gg.",
            ".gGg.dgsgd.gGg..",
            "..dgG.dsd.Ggd...",
            "....dg.s.gd.....",
            "......dsd.......",
        ],
        glow="WwYOR",
    ),
    "frost_lily": dict(  # icy, half-clear petals tipped white over a deep blue throat; dark bluish leaves
        pal={"I": ("e2f4ff", 225), "J": ("bce4f8", 230), "K": ("8cc6ec", 240), "T": "ffffff", "P": "5e9ed4", "s": "3f6e5a",
             "g": "4f7f5a", "d": "2f5a44"},
        bloom=[
            "................",
            "................",
            "................",
            "....T.....T.....",
            "....IT...TI.....",
            ".....IJ.JI......",
            "...TIJKPKJIT....",
            "....IJKPKJI.....",
            ".....JKPKJ......",
            "......KsK.......",
            ".......s........",
            "..g....s....g...",
            "...g...s...g....",
            "...dg..s..gd....",
            "....dg.s.gd.....",
            ".....dgsgd......",
        ],
        leaves=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "..g.........g...",
            "...g...s...g....",
            "...dg..s..gd....",
            "....dg.s.gd.....",
            ".....dgsgd......",
        ],
        glow="IJKTP",
    ),
}
for v in FLOWERS.values():
    v["pal"] = {k: (hexc(*c) if isinstance(c, tuple) else c) for k, c in v["pal"].items()}

# ---------------------------------------------------------------- thicket bushes
THICKETS = {
    # leaf: highlight, light, mid, dark, shadow. Each kind its own look: Blackthorn small dark rounded leaves in tight clumps,
    # Sea Buckthorn silvery narrow leaves in streaks with orange beads packed along the twigs, Jujube big glossy bright clumps.
    "blackthorn": dict(
        leaf=["8fbf63", "62954a", "467536", "305426", "1f3519"], bark=["241c1a", "3a2e2a", "56463e"],
        flower=["fdfcf6", "e4e0d2", "e9d47a"], unripe=["6d8a45", "8ca95a", "b5cf7a"], ripe=["1a1626", "3a3358", "9496c0"],
        clump=(1.8, 2.8), stretch=(1.0, 1.0), coverage=0.74, fruit="sloe", bunches=5, seed=11, blossom_heavy=True),
    "sea_buckthorn": dict(
        leaf=["c2cfba", "93a88d", "72886c", "52654c", "364532"], bark=["43342a", "5c4936", "7a6450"],
        flower=["c6c27a", "a6a65a", "e2de96"], unripe=["98aa52", "b4c466", "d2dc86"], ripe=["c4560a", "f08a1e", "ffc868"],
        clump=(1.4, 2.2), stretch=(1.9, 0.75), coverage=0.6, fruit="beads", bunches=5, seed=23, blossom_heavy=False),
    "jujube": dict(
        leaf=["b8ec84", "6cbc44", "4a9a32", "2f7224", "1d4c17"], bark=["4a2a1e", "6a4030", "8a5a40"],
        flower=["eae672", "c4c24e", "f8f6aa"], unripe=["b6bc58", "d6d474", "f0ecaa"], ripe=["5e1810", "9c3420", "e08a66"],
        clump=(2.4, 3.4), stretch=(1.0, 1.0), coverage=0.76, fruit="drupe", bunches=5, seed=37, blossom_heavy=False),
}


def clumps(p, seed, coverage=None, darken=0):
    """Leaf clumps: soft blobs lit from the top left (highlight and light there, dark and shadow on the bottom right), a
    ragged leafy rim, the clear holes between clumps kept. Back clumps first, so the front ones overlap them. Only the
    kind's five colours, shifted down by darken for the inner layer."""
    rng = random.Random(seed)
    cov = coverage or p["coverage"]
    lo, hi = p["clump"]
    sx, sy = p["stretch"]
    px = {}
    for _ in range(400):
        if len(px) >= 256 * cov:
            break
        cx, cy, r = rng.uniform(0, 16), rng.uniform(0, 16), rng.uniform(lo, hi)
        tilt = rng.choice((-1, 1)) if sx > 1 else 0  # sea buckthorn: streaks slant one way or the other
        for y in range(int(cy - r * 2) - 1, int(cy + r * 2) + 2):
            for x in range(int(cx - r * 2) - 1, int(cx + r * 2) + 2):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                if tilt:
                    dx, dy = (dx + tilt * dy) * 0.7, (dy - tilt * dx) * 0.7
                d = ((dx / sx) ** 2 + (dy / sy) ** 2) ** 0.5 / r
                if d > 1.0 + rng.uniform(-0.12, 0.18):
                    continue
                light = -(dx * 0.65 + dy * 0.75) / (r * max(sx, sy)) - d * 0.35  # top left bright, rim dark
                shade = 0 if light > 0.35 else 1 if light > 0.0 else 2 if light > -0.45 else 3
                if d > 0.85 and (dx + dy) > 0:
                    shade = 4  # the shadowed rim
                px[(x % 16, y % 16)] = p["leaf"][min(4, shade + darken)]
    return px


def twig_line(px, p, x, y, length, dx, rng):
    """A thin twig climbing from (x, y), drawn over the leaves; returns where it ends."""
    for i in range(length):
        px[(x % 16, y % 16)] = p["bark"][1 if i < length - 1 else 2]
        y -= 1
        if rng.random() < 0.6:
            x += dx
    return x, y


def bunches(p, seed):
    """Twigs with fruit: each bunch is a short visible twig and 2-4 fruits round its tip (or, Sea Buckthorn, beads packed
    along the twig). Same places in every stage."""
    rng = random.Random(seed)
    out = []
    cells = [(i % 3, i // 3) for i in range(9)]
    rng.shuffle(cells)
    for gx, gy in cells[:p["bunches"]]:  # spread the bunches over a 3x3 grid, not in a pattern
        x, y = gx * 5 + rng.randint(1, 4), gy * 5 + rng.randint(3, 5)
        dx = rng.choice((-1, 1))
        n = 2 + rng.randint(0, 2)
        out.append((x, y, dx, n, rng.random()))
    return out


def draw_bunches(px, p, spots, col, flowers=False):
    rng = random.Random(p["seed"] + 77)
    for x, y, dx, n, k in spots:
        tx, ty = twig_line(px, p, x, y, 3 + int(k * 2), dx, rng)
        if p["fruit"] == "beads":  # berries hugging the twig all the way along it
            for i in range(4 + n):
                bx, by = x + (i * dx) // 2 + (1 if i % 2 else 0), y - i
                px[(bx % 16, by % 16)] = col[2] if i % 3 == 0 else col[1] if i % 2 else col[0]
            continue
        tips = [(tx, ty), (tx - dx, ty + 1), (tx + dx, ty + 1), (tx, ty + 2)][:n]
        for i, (bx, by) in enumerate(tips):
            if flowers:  # five petals round a pale yellow eye
                for ox, oy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                    px[((bx + ox) % 16, (by + oy) % 16)] = col[0] if (ox + oy + i) % 2 else col[1]
                px[(bx % 16, by % 16)] = col[2]
            elif p["fruit"] == "sloe":  # round 2x2 sloe, dark below, 1 px bloom highlight
                for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                    px[((bx + ox) % 16, (by + oy) % 16)] = col[1]
                px[((bx + 1) % 16, (by + 1) % 16)] = col[0]
                px[(bx % 16, by % 16)] = col[2]
            else:  # jujube: an oval drupe hanging 2x3, glossy spot
                for ox, oy in ((0, 0), (1, 0), (0, 1), (1, 1), (0, 2), (1, 2)):
                    px[((bx + ox) % 16, (by + 1 + oy) % 16)] = col[1]
                px[((bx + 1) % 16, (by + 3) % 16)] = col[0]
                px[((bx + 1) % 16, (by + 2) % 16)] = col[0]
                px[(bx % 16, (by + 1) % 16)] = col[2]
                px[(bx % 16, by % 16)] = p["bark"][2]  # its stalk


def bare_twigs(p, seed, count=6):
    """Bare winter twigs: forking branches, thicker at the base, thin tips."""
    rng = random.Random(seed)
    px = {}
    for _ in range(count):
        x, y = rng.randrange(16), 15
        dx = rng.choice((-1, 1))
        for step in range(rng.randint(10, 16)):
            px[(x % 16, y)] = p["bark"][0 if step < 5 else 1]
            y -= 1
            if y < 0:
                break
            if rng.random() < 0.4:
                x += dx
            if rng.random() < 0.2:
                sx, sy = x, y
                for _ in range(rng.randint(2, 4)):
                    sx -= dx
                    sy -= 1
                    if 0 <= sy < 16:
                        px[(sx % 16, sy)] = p["bark"][2]
    return px


def to_img(px):
    img = Image.new("RGBA", (16, 16))
    for (x, y), c in px.items():
        img.putpixel((x, y), hexc(c))
    return img


def thicket_images(name, p):
    out = {}
    leaves = clumps(p, p["seed"])
    spots = bunches(p, p["seed"] + 1)
    bare = bare_twigs(p, p["seed"] + 5)
    out[f"block/{name}_stage0.png"] = to_img(leaves)
    if p["blossom_heavy"]:  # blackthorn flowers before its leaves: white blossom on dark twigs, a few young leaves
        px = dict(bare)
        px.update({k: v for k, v in clumps(p, p["seed"] + 9, 0.25).items()})
        rng = random.Random(p["seed"] + 9)
        extra = [(rng.randrange(1, 15), rng.randrange(4, 15), rng.choice((-1, 1)), 3, 0.2) for _ in range(4)]
        draw_bunches(px, p, spots + extra, p["flower"], flowers=True)
    else:
        px = dict(leaves)
        draw_bunches(px, p, spots, p["flower"], flowers=True)
    out[f"block/{name}_stage1.png"] = to_img(px)
    for stage, col in ((2, p["unripe"]), (3, p["ripe"])):
        px = dict(leaves)
        draw_bunches(px, p, spots, col)
        out[f"block/{name}_stage{stage}.png"] = to_img(px)
    out[f"block/{name}_bare.png"] = to_img(bare)
    px = dict(bare)
    draw_bunches(px, p, spots, p["ripe"])
    out[f"block/{name}_bare_ripe.png"] = to_img(px)
    # inner layer: the shaded heart of the bush, seen through the gaps (darker, denser)
    out[f"block/{name}_inner.png"] = to_img(clumps(p, p["seed"] + 13, 0.85, darken=2))
    # the branch planes: stems forking up from the root, ending in leafy tips that poke out over the crown
    rng = random.Random(p["seed"] + 3)
    st = {}
    tips = []
    for x0 in (4, 8, 12):
        x = x0
        for y in range(15, 2, -1):
            st[(x, y)] = p["bark"][0 if y > 10 else 1]
            if rng.random() < 0.3:
                x = max(1, min(14, x + rng.choice((-1, 1))))
            if y in (9, 6) and rng.random() < 0.7:
                bx = max(0, min(15, x + rng.choice((-2, 2))))
                st[(bx, y - 1)] = p["bark"][2]
                tips.append((bx, y - 2))
        tips.append((x, 2))
    out[f"block/{name}_stems_bare.png"] = to_img(st)
    for tx, ty in tips:  # little leaf sprays at the branch ends
        for ox, oy, c in ((0, 0, 1), (-1, 0, 2), (1, 0, 2), (0, -1, 0), (-1, 1, 3), (1, 1, 3)):
            if 0 <= tx + ox < 16 and 0 <= ty + oy < 16:
                st[(tx + ox, ty + oy)] = p["leaf"][c]
    out[f"block/{name}_stems.png"] = to_img(st)
    return out


# ---------------------------------------------------------------- items
ITEMS = {
    "sloes": dict(pal={"A": "8a98c4", "B": "343a66", "C": "23253f", "D": "16172a", "s": "3c302b", "g": "4f7a38", "G": "66924a"},
                  rows=[
                      "................",
                      "................",
                      "........s.......",
                      ".......s.Gg.....",
                      "......s..gG.....",
                      "....ABBs........",
                      "...ABBBBC.s.....",
                      "...BBBBBC..s....",
                      "...BBBBCC.ABB...",
                      "....CCCC.ABBBC..",
                      "......s..BBBBC..",
                      ".....ABB.BBBCC..",
                      "....ABBBC.CCC...",
                      "....BBBBC.......",
                      "....CBBCC.......",
                      ".....CCC........",
                  ]),
    "sea_buckthorn_berries": dict(pal={"A": "ffc060", "B": "f08a1c", "C": "c85a08", "s": "5e4a38", "g": "8ea088", "G": "c6d2c0"},
                                  rows=[
                                      "................",
                                      "..........gG....",
                                      ".........sg.....",
                                      "....G...s.......",
                                      ".....g.sBA......",
                                      "......sABC......",
                                      "....ABsBAC......",
                                      "....BCsCBA......",
                                      "...ABAsABC......",
                                      "...BCBsBCB......",
                                      "....ABsCAB......",
                                      "....BCsABC......",
                                      ".....AsBC.......",
                                      "......s.........",
                                      ".....s..........",
                                      "................",
                                  ]),
    "jujubes": dict(pal={"A": "d6785a", "B": "a23a24", "C": "6e1e14", "D": "4a120c", "s": "86563e", "g": "55a034", "G": "9ad66a"},
                    rows=[
                        "................",
                        "................",
                        ".........s......",
                        ".......gGsGg....",
                        "........gs......",
                        "....ABB..s.ABB..",
                        "...ABBBC..ABBBC.",
                        "...BBBBC..BBBBC.",
                        "...BBBBC..BBBCC.",
                        "...BBBCC..BBBCC.",
                        "....CCC....CCC..",
                        "......ABB.......",
                        ".....ABBBC......",
                        ".....BBBBC......",
                        ".....BBBCC......",
                        "......CCC.......",
                    ]),
    "cooked_elderberries": dict(pal={"A": "a07ab8", "B": "4a2a5c", "C": "2e1838", "D": "1c0e22", "S": "c890d0",
                                     "g": "4f7a38", "G": "6e9a48", "d": "355a26"},
                                rows=[
                                    "................",
                                    "................",
                                    "................",
                                    "................",
                                    "................",
                                    "......SA.A......",
                                    "....ABSBABBA....",
                                    "...ABCBBSBCBA...",
                                    "..ABCBCBBCBCBA..",
                                    "..BCBDBCBDBCBC..",
                                    ".GgCDCCDCCDCDgG.",
                                    "GgggCCDCCDCCgggG",
                                    ".dggggggggggggd.",
                                    "..ddgggggggggd..",
                                    "....dddddddd....",
                                    "................",
                                ]),
}


def fern_parts():
    """The Fern Flower's model parts: an arching frond (root bottom left, tip top right, leaflets on both sides of the
    stalk, shorter toward the tip), the flower seen from above (five petals, white heart through gold to amber tips), its
    cup from the side, the stalk and the curled bud of the fern out of bloom."""
    import math
    out = {}
    G, g, d, s = hexc("8ccc5c"), hexc("5f9a3f"), hexc("3a6a2a"), hexc("4a7630")
    fr = Image.new("RGBA", (16, 16))
    pts = [(1 + 11 * t + 2 * math.sin(t * 3.0) * (1 - t), 15 - 14 * t ** 0.85) for t in [i / 30 for i in range(31)]]
    for k, (x, y) in enumerate(pts):
        fr.putpixel((int(x), int(y)), s)
    for k in range(3, 29, 4):
        (x, y), (x2, y2) = pts[k], pts[k + 1]
        tx, ty = x2 - x, y2 - y
        ln = math.hypot(tx, ty) or 1
        tx, ty = tx / ln, ty / ln
        n = max(1, round(3.4 * (1 - k / 30) + 0.6))
        for side, col in ((1, G), (-1, g)):  # leaflets slant toward the tip; the upper-left ones lit, the others in shade
            ux, uy = -ty * side * 0.85 + tx * 0.55, tx * side * 0.85 + ty * 0.55
            for m in range(1, n + 1):
                px, py = round(x + ux * m), round(y + uy * m)
                if 0 <= px < 16 and 0 <= py < 16:
                    fr.putpixel((px, py), col if m < n else d)
    out["block/fern_flower_frond.png"] = fr
    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - 8, y + 0.5 - 8
            r, th = math.hypot(dx, dy), math.atan2(dy, dx)
            a = abs(((th + math.pi / 2) % (2 * math.pi / 5)) - math.pi / 5)  # angle off the nearest petal's axis
            a = math.pi / 5 - a
            edge = 1.6 + 3.6 * max(0.0, 1 - (a / 0.52) ** 2) ** 0.5  # rounded petals, a notch between each
            if r > edge:
                continue
            c = "ffffff" if r < 1.1 else "fff4c0" if r < 2.0 else "ffe066" if r < edge * 0.72 else "ffb43c" if r < edge * 0.92 else "f07a2c"
            top.putpixel((x, y), hexc(c))
    out["block/fern_flower_top.png"] = top
    out["block/fern_flower_side.png"] = grid(["................"] * 10 + [
        "....R..O..R.....",
        "....OY.Y.YO.....",
        ".....OYWYO......",
        "......OYO.......",
        ".......s........",
        "................"], {"R": "f07a2c", "O": "ffb43c", "Y": "ffe066", "W": "ffffff", "s": "4a7630"})
    out["block/fern_flower_stalk.png"] = grid(["................"] * 6 + [
        ".......s........",
        ".......s........",
        "......gs........",
        ".......s........",
        ".......sg.......",
        ".......s........",
        ".......s........",
        "......gs........",
        ".......s........",
        ".......s........"], {"s": "4a7630", "g": "6ea848"})
    out["block/fern_flower_bud.png"] = grid(["................"] * 6 + [
        ".......bb.......",
        "......b..b......",
        "......b.bb......",
        ".......bs.......",
        ".......s........",
        ".......s........",
        "......gs........",
        ".......s........",
        ".......s........",
        ".......s........"], {"b": "7cbc50", "s": "4a7630", "g": "6ea848"})
    return out


def build():
    out = fern_parts()
    for name, f in FLOWERS.items():
        out[f"block/{name}.png"] = grid(f["bloom"], f["pal"])
        out[f"block/{name}_leaves.png"] = grid(f["leaves"], f["pal"]) if "leaves" in f else grid(f["bloom"], f["pal"], f["remap"])
        if "glow" in f:
            out[f"block/{name}_glow.png"] = grid(f["bloom"], f["pal"], {ch: "." for ch in f["pal"] if ch not in f["glow"]})
    for name, p in THICKETS.items():
        out.update(thicket_images(name, p))
    for name, it in ITEMS.items():
        out[f"item/{name}.png"] = grid(it["rows"], it["pal"])
    return out


def preview(images, dst):
    s, cols = 10, 9
    keys = list(images)
    rows = [keys[i:i + cols] for i in range(0, len(keys), cols)]
    sheet = Image.new("RGBA", (cols * (16 * s + 8) + 8, len(rows) * (16 * s + 8) + 8), (150, 168, 190, 255))
    for r, row in enumerate(rows):
        for c, k in enumerate(row):
            sheet.alpha_composite(images[k].resize((16 * s, 16 * s), Image.NEAREST), (8 + c * (16 * s + 8), 8 + r * (16 * s + 8)))
    Path(dst).mkdir(parents=True, exist_ok=True)
    sheet.save(Path(dst) / "flora_textures.png")


if __name__ == "__main__":
    imgs = build()
    for rel, img in imgs.items():
        img.save(RES / rel)
    print(f"{len(imgs)} flora textures")
    if len(sys.argv) > 1:
        preview(imgs, sys.argv[1])
