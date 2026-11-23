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
    "fern_flower": dict(  # fronds round a stalk that opens into a glowing golden flower on summer nights
        pal={"W": "ffffff", "w": "fff6c8", "Y": "ffe46e", "O": "ffb43c", "R": "f07a2c", "s": "4f7a30", "g": "5f9a3f", "G": "86c456",
             "d": "3f6e2c", "b": "6ea848"},
        bloom=[
            ".....R...R......",
            "....ROR.ROR.....",
            "...ROYOYOYOR....",
            "....OYwWwYO.....",
            "..RRYwWWWwYRR...",
            "....OYwWwYO.....",
            "...ROYOYOYOR....",
            "..G.ROR.RORG....",
            ".Gg..R.s.R.gG...",
            "..gG.G.s.G.Gg...",
            "G..gGg.s.gGg..G.",
            "gG..gd.s.dg..Gg.",
            ".gGg.dgsgd.gGg..",
            "..dgG.dsd.Ggd...",
            "....dg.s.gd.....",
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
    "blackthorn": dict(
        leaf=["36572b", "47703a", "5a8a44", "74a556", "93c06c"], bark=["2a2220", "3c302b", "54453c"],
        flower=["fbfaf4", "e6e2d6", "e8d888"], unripe=["6f8c46", "8eaa58", "aec870"], ripe=["1c1828", "37314f", "8486ad"],
        leaf_shape="small", fruit="sloe", clusters=9, coverage=0.80, seed=11, blossom_heavy=True),
    "sea_buckthorn": dict(
        leaf=["5d7059", "73886d", "8ea088", "a7b8a2", "c6d2c0"], bark=["4a3a2c", "5e4a38", "7a6450"],
        flower=["c8c47a", "a8a85c", "e0dc90"], unripe=["93a650", "aebf62", "c8d27a"], ripe=["c85a08", "f08a1c", "ffc060"],
        leaf_shape="narrow", fruit="dense", clusters=8, coverage=0.66, seed=23, blossom_heavy=False),
    "jujube": dict(
        leaf=["2f6a22", "3f822c", "55a034", "72bc46", "9ad66a"], bark=["4e2c20", "6a4030", "86563e"],
        flower=["e6e070", "c8c450", "f6f2a0"], unripe=["b8bc58", "d4d070", "ece898"], ripe=["6e1e14", "a23a24", "d6785a"],
        leaf_shape="oval", fruit="drupe", clusters=7, coverage=0.72, seed=37, blossom_heavy=False),
}

# leaf shapes: (role, dx, dy); h = lit edge, m = blade, d = shaded edge
SHAPES = {
    "small": [[("h", 0, 0), ("m", 1, 0), ("m", 0, 1), ("d", 1, 1)], [("h", 1, 0), ("m", 0, 1), ("m", 1, 1), ("d", 0, 2)],
              [("h", 0, 0), ("m", 1, 0), ("d", 2, 1)], [("h", 0, 0), ("m", 0, 1), ("d", 1, 2)]],
    "narrow": [[("h", 0, 0), ("m", 1, 1), ("m", 2, 2), ("d", 3, 3)], [("h", 3, 0), ("m", 2, 1), ("m", 1, 2), ("d", 0, 3)],
               [("h", 0, 0), ("m", 1, 0), ("m", 2, 1), ("d", 3, 1)], [("h", 0, 1), ("m", 1, 1), ("m", 2, 0), ("d", 3, 0)]],
    "oval": [[("h", 0, 0), ("h", 1, 0), ("m", 0, 1), ("m", 1, 1), ("m", 2, 1), ("d", 1, 2), ("d", 2, 2)],
             [("h", 1, 0), ("m", 0, 1), ("m", 1, 1), ("m", 0, 2), ("m", 1, 2), ("d", 1, 3)],
             [("h", 0, 0), ("m", 1, 0), ("m", 1, 1), ("m", 2, 1), ("d", 2, 2), ("d", 3, 2)]],
}


def leaf_mass(p):
    """Whole leaves laid back to front until the coverage is met: the first ones deep in the bush (dark), the last ones on
    top (light). Each leaf has a lit edge and a shaded edge, and drops a dark pixel of shadow under it, so the mass reads
    as leaves, not noise. The holes between them stay clear."""
    rng = random.Random(p["seed"])
    px, order = {}, []
    shapes = SHAPES[p["leaf_shape"]]
    target = 256 * p["coverage"]
    n = 0
    while len(px) < target and n < 2000:
        n += 1
        x, y = rng.randrange(16), rng.randrange(16)
        depth = min(1.0, len(px) / target)  # 0 = deep inside, 1 = outermost
        mid = 1 + round(depth * 2 - (y / 16) * 0.8)
        shade = {"h": min(4, mid + 1), "m": max(0, min(3, mid)), "d": max(0, mid - 1)}
        sh = rng.choice(shapes)
        for role, dx, dy in sh:
            px[((x + dx) % 16, (y + dy) % 16)] = p["leaf"][shade[role]]
        under = ((x + sh[-1][1]) % 16, (y + sh[-1][2] + 1) % 16)
        if under not in px:
            px[under] = p["leaf"][0]
    return px


def twigs(p, seed, count=7):
    """Bare winter twigs: forking branches climbing across the face."""
    rng = random.Random(seed)
    px = {}
    for _ in range(count):
        x, y = rng.randrange(16), 15
        dx = rng.choice((-1, 1))
        for step in range(rng.randint(9, 16)):
            px[(x % 16, y)] = p["bark"][0 if step < 4 else 1 if rng.random() < 0.7 else 2]
            y -= 1
            if y < 0:
                break
            if rng.random() < 0.45:
                x += dx
            if rng.random() < 0.18:  # a side shoot
                sx, sy = x, y
                for _ in range(rng.randint(2, 4)):
                    sx -= dx
                    sy -= rng.randint(0, 1)
                    if 0 <= sy < 16:
                        px[(sx % 16, sy)] = p["bark"][2]
    return px


def fruit_spots(p):
    """Where the fruit hangs (the same in every stage): cluster centres and their berries."""
    rng = random.Random(p["seed"] + 1)
    spots = []
    for _ in range(p["clusters"]):
        cx, cy = rng.randrange(1, 15), rng.randrange(1, 15)
        if p["fruit"] == "sloe":  # round 2x2 sloes, a pair or a single
            spots.append([(cx, cy, "big")] + ([(cx + 2, cy + 1, "big")] if rng.random() < 0.5 else []))
        elif p["fruit"] == "dense":  # orange beads packed along a short twig
            d = rng.choice((-1, 1))
            spots.append([(cx + i * d // 2 + rng.randint(0, 1), cy + i, "bead") for i in range(rng.randint(4, 7))])
        else:  # jujube: an oval 2x3 drupe
            spots.append([(cx, cy, "drupe")])
    return spots


def put_fruit(px, spots, col, kind):
    """col: dark, mid, light. kind: 'fruit' or 'flower'."""
    for cluster in spots:
        for x, y, f in cluster:
            if kind == "flower" and f == "bead":  # sea buckthorn: tiny catkin flowers along the twig
                px[(x % 16, y % 16)] = col[2] if (x + y) % 2 else col[1]
            elif kind == "flower":
                for dx, dy in ((0, -1), (-1, 0), (1, 0), (0, 1)):
                    px[((x + dx) % 16, (y + dy) % 16)] = col[0] if f == "bead" else col[1] if (dx + dy) % 2 else col[0]
                px[(x % 16, y % 16)] = col[2]
            elif f == "big":
                for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
                    px[((x + dx) % 16, (y + dy) % 16)] = col[1]
                px[((x + 1) % 16, (y + 1) % 16)] = col[0]
                px[(x % 16, y % 16)] = col[2]
            elif f == "bead":
                px[(x % 16, y % 16)] = col[1] if (x + y) % 3 else col[2]
                px[(x % 16, (y + 1) % 16)] = col[0] if (x + y) % 2 else col[1]
            else:
                for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (0, 2), (1, 2)):
                    px[((x + dx) % 16, (y + dy) % 16)] = col[1]
                px[((x + 1) % 16, (y + 2) % 16)] = col[0]
                px[((x + 1) % 16, (y + 1) % 16)] = col[0]
                px[(x % 16, y % 16)] = col[2]


def to_img(px):
    img = Image.new("RGBA", (16, 16))
    for (x, y), c in px.items():
        img.putpixel((x, y), hexc(c))
    return img


def thicket_images(name, p):
    out = {}
    leaves = leaf_mass(p)
    spots = fruit_spots(p)
    bare = twigs(p, p["seed"] + 5)
    out[f"block/{name}_stage0.png"] = to_img(leaves)
    if p["blossom_heavy"]:  # blackthorn: a cloud of white blossom on dark twigs before the leaves come
        px = dict(bare)
        px.update({k: v for k, v in leaves.items() if hash(k) % 4 == 0})
        rng = random.Random(p["seed"] + 9)
        extra = [[(rng.randrange(16), rng.randrange(16), "big")] for _ in range(6)]
        put_fruit(px, spots + extra, p["flower"], "flower")
    else:
        px = dict(leaves)
        put_fruit(px, spots, p["flower"], "flower")
    out[f"block/{name}_stage1.png"] = to_img(px)
    for stage, col in ((2, p["unripe"]), (3, p["ripe"])):
        px = dict(leaves)
        put_fruit(px, spots, col, "fruit")
        out[f"block/{name}_stage{stage}.png"] = to_img(px)
    out[f"block/{name}_bare.png"] = to_img(bare)
    px = dict(bare)
    put_fruit(px, spots, p["ripe"], "fruit")
    out[f"block/{name}_bare_ripe.png"] = to_img(px)
    # the branch planes inside: a few stems forking up from the root
    rng = random.Random(p["seed"] + 3)
    st = {}
    for x0 in (5, 8, 11):
        x = x0
        for y in range(15, 1, -1):
            st[(x, y)] = p["bark"][0 if y > 11 else 1]
            if rng.random() < 0.3:
                x += rng.choice((-1, 1))
                x = max(1, min(14, x))
            if y < 11 and rng.random() < 0.25:
                st[(max(0, min(15, x + rng.choice((-2, 2)))), y - 1)] = p["bark"][2]
    for (x, y), c in list(leaves.items()):  # a few leaves on the inner twigs too
        if y < 13 and hash((x, y)) % 3 == 0:
            st[(x, y)] = c
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


def build():
    out = {}
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
