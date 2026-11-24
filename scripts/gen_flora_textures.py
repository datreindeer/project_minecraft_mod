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

# phase 2 flowers (tall ones as _bottom/_top pairs) and the phase 3 magic flowers
FLOWERS.update({
    "foxglove_top": dict(  # a spire of nodding pink bells, spotted mouths, mostly on one side; green buds at the tip
        pal={"A": "eeaadc", "B": "c86cb6", "C": "8e3f8a", "D": "4e1a56", "W": "f8eaf4", "s": "5d7f3e", "b": "a8c070",
             "g": "6f9a4a", "G": "8fbc62", "d": "476b32"},
        bloom=[
            "................",
            ".......b........",
            "......bsb.......",
            ".......sb.......",
            "......bsA.......",
            ".......sAB......",
            "......bsBB......",
            ".......sABC.....",
            "......AsBWD.....",
            ".....ABs.D......",
            ".....BCsAAB.....",
            "......DsABBC....",
            ".......sBWDC....",
            ".....AAs..D.....",
            "....ABBsAAB.....",
            "....BWDsABBC....",
        ],
        remap={"A": "b", "B": "g", "C": ".", "D": ".", "W": "."},
    ),
    "foxglove_bottom": dict(  # the lowest bells, the bare stalk, big soft basal leaves
        pal={"A": "eeaadc", "B": "c86cb6", "C": "8e3f8a", "D": "4e1a56", "W": "f8eaf4", "s": "5d7f3e", "b": "a8c070",
             "g": "6f9a4a", "G": "8fbc62", "d": "476b32", "L": "a6cf7a"},
        bloom=[
            ".....D.sBWDC....",
            ".......s..D.....",
            ".....AAsAAB.....",
            "....ABBsABBC....",
            "....BWDsBWDC....",
            ".....D.s..D.....",
            ".......s........",
            ".......s........",
            "...LG..s........",
            "..LGgG.s..LGG...",
            ".LGgggGsLGgggG..",
            "LGggddgsggggddG.",
            "ggdd..dsgdd..dgg",
            ".d..LGgsgLG...d.",
            "...LGgdsdggGG...",
            "..LGgd.s.ddggG..",
        ],
        remap={"A": "b", "B": "g", "C": ".", "D": ".", "W": "."},
    ),
    "purple_loosestrife_top": dict(  # a long spike of magenta whorls, tapering to a bud tip
        pal={"A": "f094d8", "B": "c84eae", "C": "86287e", "s": "7a6a44", "g": "6e9a48", "G": "8cbc60", "d": "4a7032"},
        bloom=[
            "................",
            "................",
            ".......C........",
            "......BAB.......",
            ".......sB.......",
            "......ABAC......",
            ".....BAsBC......",
            "......CsC.......",
            ".....ABAAB......",
            "....BAAsBAC.....",
            ".....CBsBCC.....",
            "......CsC.......",
            ".....AABAB......",
            "....BAAsBBC.....",
            ".....CBsBCC.....",
            "....gC.s.Cg.....",
        ],
        remap={"A": "G", "B": "g", "C": "d"},
    ),
    "purple_loosestrife_bottom": dict(  # the lowest whorl, then pairs of narrow willow leaves up the stem
        pal={"A": "f094d8", "B": "c84eae", "C": "86287e", "s": "7a6a44", "g": "6e9a48", "G": "8cbc60", "d": "4a7032"},
        bloom=[
            "...gd..s..dg....",
            ".......s........",
            ".....ABAAB......",
            "....BAAsBAC.....",
            ".....CBsBCC.....",
            "...Gg..s..gG....",
            ".GGgd..s..dgGG..",
            "G.gd...s...dg.G.",
            ".......s........",
            "..G....s....G...",
            "...GGg.s.gGG....",
            ".Ggggd.s.dgggG..",
            "G..dd..s..dd..G.",
            "...G...s...G....",
            "..Ggd..s..dgG...",
            ".Ggd..dsd..dgG..",
        ],
        remap={"A": "G", "B": "g", "C": "d"},
    ),
    "yellow_adonis": dict(  # big glossy yellow suns of narrow petals over feathery dill-fine leaves
        pal={"Y": "fbe046", "H": "fff4a8", "y": "e0b020", "O": "a87410", "c": "c0a82a", "s": "6e9446", "g": "6e9e44",
             "G": "92c060", "d": "4a7430"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "...Y.Y.Y........",
            "..YHYHYYy.......",
            ".YHYcccyyY.Y.Y..",
            "..YYcOcyy.YHYyY.",
            ".YyycccyO.YcOy..",
            "..yyyyyOO.yyyO..",
            "...O.s.O...OsO..",
            ".g.g.s.g....s.g.",
            "..GgGs.gG.gGsg..",
            ".gdgGsgdgGgds.g.",
            "g.Gdgs.GdgGsgGdg",
            ".gd.gsdg.gdsdg..",
        ],
        remap={"Y": ".", "H": ".", "y": ".", "O": ".", "c": "."},
    ),
    "bog_asphodel": dict(  # little spikes of yellow stars with orange anthers over stiff sword leaves; orange pods after
        pal={"Y": "f8d23a", "y": "d29a1c", "o": "e8661a", "s": "8a9a48", "G": "8cb058", "g": "6a9440", "d": "466a2c",
             "r": "c8642a"},
        bloom=[
            "................",
            "................",
            "............Y...",
            "...Y.......YoY..",
            "..YoY.......Yy..",
            "...YY.Y....yoY..",
            "..yoY.YY....Ys..",
            "...Ys.YoY..yoY..",
            "..yoY.yYY...ys..",
            "...ys.yoY.G..s..",
            ".G..s..ys.G..s.G",
            ".G..s..ys.g..sG.",
            "..g.sG..sGg..sg.",
            "G.g.sG.gs.g.Gs.g",
            ".GgdsgGgsdgGgsd.",
            "..gdsdgdsdgdgsd.",
        ],
        remap={"Y": ".", "y": "r", "o": "r"},
    ),
    "marsh_marigold": dict(  # glossy golden cups over round kidney-shaped dark glossy leaves
        pal={"H": "fff2a0", "Y": "f8cc28", "y": "d89c10", "O": "a06808", "L": "8ec66a", "g": "4f8a3a", "d": "336628",
             "D": "22481c", "s": "5a8a3a"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "..HY.YY.........",
            ".HYYOYYy....HY.Y",
            "..YyOOyy...HYYOY",
            "...yyyy.....YOyy",
            "....s....LL..yy.",
            ".LL.s..LLggs.s..",
            "LLggsgLggggdgs..",
            "LgggdgLgdggdLLg.",
            "gggddggdddgLggdg",
            ".gddDdgddDdggddd",
            "..dDD.dDD.Ddddd.",
        ],
        remap={"H": ".", "Y": ".", "y": ".", "O": "."},
    ),
    "wood_anemone": dict(  # a carpet of white six-petalled stars, pink-backed, over deeply cut three-lobed leaves
        pal={"W": "fcfcf6", "w": "dcdcd4", "p": "e6bccc", "y": "eccc3c", "S": "8a6a5a", "s": "6a8a4a", "G": "8cc464",
             "g": "62a040", "d": "3f7a2c"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "..W.W...........",
            ".WWyWw.....W.W..",
            "..wWWp....WWyWw.",
            "...pS......wpWp.",
            "....S..W.W..S...",
            "..G.S.WWyWw.S.G.",
            ".GgGS..pWp..SGgG",
            "GgdgGgG.S.GgGgdg",
            ".dGgdgGgsGgdgGd.",
            "Ggdg.dgdsdg.dgdG",
            ".dgdgdgdsdgdgdg.",
        ],
        remap={"W": ".", "w": ".", "p": ".", "y": ".", "S": "."},
    ),
    "bluebells": dict(  # arching one-sided spikes of narrow violet bells curling at the mouth, strap leaves
        pal={"A": "b0b0f4", "B": "7470dc", "C": "4c42ae", "D": "2e2878", "s": "5c8a48", "g": "5f9a42", "G": "80b85a",
             "d": "3c6e2c"},
        bloom=[
            "................",
            "................",
            "......sss.......",
            ".....s...s......",
            "....s.....s.....",
            "....s.A..A.s....",
            "...s.AB.AB.B....",
            "...sAB.ABC.C....",
            "...sBC.BCD.ss...",
            "...sC.D.D.s..s..",
            "...s......s.A.A.",
            ".g.s.G...sABAB..",
            ".gGsGg..GsBCBC..",
            "..gsgd.GgsC.C.g.",
            ".GgsgGgdgsgGgdgG",
            "GgdsdgdgdsdgdgdG",
        ],
        leaves=["................"] * 11 + [
            ".g.g.G......gG..",
            ".gGgGg..G..ggG..",
            "..gggd.Gg.GgGgd.",
            ".GgggGgdggGgGdgG",
            "GgdgddgdgddgdgdG"],
    ),
    "purple_saxifrage": dict(  # a low dark cushion starred all over with pink-purple flowers
        pal={"P": "f4a8dc", "p": "d466bc", "c": "fbe6a0", "G": "6e9656", "g": "4f7a3e", "d": "355a2c", "D": "22401e"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "....P......P....",
            "...PcP.P..PcP...",
            "..gGpgPcP.GpgP..",
            ".gGdgGgpGgdgPcP.",
            "gGdPgdGdgPgGdpg.",
            "GdPcPdgdPcPdgdgd",
            "dgdpdgdDdpdgdgDd",
            ".DdDdDdDdDdDdDd.",
        ],
        remap={"P": "G", "p": "g", "c": "G"},
    ),
    "spiny_thrift": dict(  # a spiny grey-green needle cushion dotted with small papery pink flowers; straw after
        pal={"G": "b4c49c", "g": "8e9e7a", "d": "6c7a5e", "D": "4e5a44", "P": "f8cad8", "p": "e08cb0", "q": "aa5a7e",
             "e": "d8c8a0", "f": "a89868"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "......G.G.......",
            "...G...G.G..G...",
            "..G.G.gG.gG.g.g.",
            "...gGg.g.g.gGg..",
            ".G..gggPpggg..g.",
            "..GggdgpqgdggG..",
            "G..gdPpdddgdgg.g",
            ".GggdpqDDdPpgGg.",
            "GggdDdDdDdpqdggg",
            ".gddDdDDdDDdddg.",
            "gdDdDDdDDdDDdDdg",
            ".dDDDDDDDDDDDDd.",
        ],
        remap={"P": "e", "p": "f", "q": "f"},
    ),
    "cats_foot": dict(  # silvery woolly rosettes, short stalks with fluffy pink-white clusters (the cat's paws)
        pal={"W": "dfe6d6", "w": "b4c2aa", "v": "8a9a80", "u": "62705a", "S": "a8b0a0", "F": "fcf2f6", "f": "f2c0d4",
             "k": "d08aa8"},
        bloom=[
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
            "...fFf..........",
            "..FfkFf....fFf..",
            "..kfFkf...FfkFf.",
            "...kSk....kfFfk.",
            "....S......kSk..",
            "....S.......S...",
            "..wWwS.Ww..wSWw.",
            ".wWvWwWwvWwWvwWv",
            "WvwuvwvuwvWuwvuw",
            ".uvuvuvuvuuvuvu.",
        ],
        remap={"F": ".", "f": ".", "k": ".", "S": "."},
    ),
    "will_o_wisp_lily": dict(  # night bloom: a nodding ghost-pale lily, petals curled back, glowing blue-green; two wisps
        pal={"T": "f2fffc", "I": ("d4fff2", 205), "J": ("9cf0dc", 215), "K": ("5ed4c4", 230), "P": "2fa8a0",
             "o": ("eafff8", 210), "w": ("c8fff0", 150), "s": "4e6e52", "g": "5a7e56", "d": "3a5a3c", "b": "d0e8dc",
             "B": "a8c8b8"},
        bloom=[
            "................",
            "............w...",
            "......sss.......",
            ".....s...s......",
            ".....s....s.....",
            ".....s..T.JK.T..",
            ".....s..IJKPKJI.",
            ".....s...IJPJI..",
            ".....s....KPK...",
            ".g...s.....o....",
            "..g..s.....o....",
            "..g..s...g......",
            "...g.s..g.......",
            ".g.gds.gd..w....",
            "..gdgsgd........",
            "...dgsdg........",
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
            "........bb......",
            "........Bs......",
            ".g......s...g...",
            "..g.g..s..g.g...",
            "..gdg.dsd.gdg...",
            "...dgdgsgdgd....",
        ],
        glow="TIJKPow",
    ),
    "moonwort": dict(  # a tiny fern: one frond of half-moon leaflets, a stalk with a grape-like spike of silver beads
        pal={"G": "a0d07c", "g": "6ea24e", "d": "4a7a36", "s": "5e8a42", "B": "ffffff", "b": ("e4ecf8", 240),
             "c": ("b4c4d8", 240), "e": "8a8c52", "f": "6a6a3a"},
        bloom=[
            "................",
            ".......Bb.......",
            "......B.sB......",
            ".......Bsc......",
            "......Bcs.B.....",
            ".....c..sBc.....",
            "......Bbs.b.....",
            ".......cs.......",
            "....GG..s..GG...",
            "...Ggd..s.dgG...",
            "....dGGsGGd.....",
            "...GG.ds.gGG....",
            "..Ggd..s..dgG...",
            "...d.GgsgG.d....",
            "......dsd.......",
            ".......s........",
        ],
        remap={"B": "e", "b": "e", "c": "f"},
        glow="Bbc",
    ),
})
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
        clump=(2.4, 3.4), stretch=(1.0, 1.0), coverage=0.76, fruit="drupe", bunches=5, seed=37, blossom_heavy=False, soft=True),
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


# phase 2 bushes. style: the leaf surface painted over the lit clumps (see leaf_mass); low: a mat about 6 px tall whose
# sides show only the bottom rows, so the fruit is spread over the whole tile; evergreen: winter keeps the leaves (a duller
# winter palette) instead of bare twigs. Fruit colours: [shadow, body, highlight]; flower colours: [petal, petal shade, centre].
THICKETS.update({
    "rosehip": dict(  # dog rose: small mid-green leaves, thorny arching canes, pink roses, scarlet hips that stay into winter
        leaf=["a2cc72", "72a64e", "548a3c", "3a6a2c", "26481e"], bark=["3e2a1e", "5a4426", "8a3a2a"],
        flower=["fcd2e0", "ec8cb0", "f4d23c"], unripe=["7a7a1e", "c08a2a", "e8c060"], ripe=["8a1008", "d8301c", "ffb090"],
        clump=(1.3, 2.0), stretch=(1.0, 1.0), coverage=0.72, fruit="hip", bunches=5, seed=51, thorny=True, canes=2),
    "crowberry": dict(  # heath mat: tiny dark needle leaves crowded along reddish stems, shiny black berries
        leaf=["7c9c50", "56783a", "3e5e2c", "2c4622", "1e3018"], bark=["4a2018", "7e3826", "a04a32"],
        flower=["a82848", "6e1a30", "e070a0"], unripe=["4a6a2a", "7a9a40", "b8d080"], ripe=["140a1c", "3a2248", "6a7290"],
        winter=["6e7a4a", "52603a", "3e4a2c", "2c3420", "1c2216"],
        clump=(1.6, 2.6), stretch=(1.0, 1.0), coverage=0.86, style="needle", stamps=22, soft=True,
        low=True, evergreen=True, fruit="heath", bunches=6, nmax=2, seed=63),
    "bearberry": dict(  # trailing mat: small glossy leathery leaves on red-brown runners, bright red berries
        leaf=["a8da84", "62a246", "448034", "2e6026", "1c3e18"], bark=["4e2418", "7e3a24", "a65a38"],
        flower=["fbf4f2", "f0b0c0", "c86a84"], unripe=["6a9a3a", "9cc65a", "d8eca0"], ripe=["900e10", "e0261e", "ffc0b0"],
        winter=["a8945a", "84683a", "68502e", "4a3622", "2e2216"],
        clump=(1.1, 1.7), stretch=(1.0, 1.0), coverage=0.8, style="oval", trail=2, soft=True,
        low=True, evergreen=True, fruit="bear", bunches=6, nmax=2, seed=71),
    "barberry": dict(  # thorny: tufts of small blue-green oval leaves, hanging yellow flowers, then strings of red berries
        leaf=["b0d0ac", "80a886", "5e8870", "426652", "2a4438"], bark=["4a3424", "6e5034", "9a7a50"],
        flower=["f8e050", "d8b020", "fff4a0"], unripe=["5a8a3a", "8ab64e", "c4e08a"], ripe=["900c14", "dc1e24", "ff9a90"],
        clump=(1.0, 1.7), stretch=(1.0, 1.0), coverage=0.7, style="tuft", fruit="barb", bunches=6, seed=83, thorny=True, soft=True),
    "caper": dict(  # low sprawling mound: round fleshy blue-green leaves, white flowers with long violet stamens, olive buds
        leaf=["b8ccb0", "90ac8e", "6e8e76", "52705e", "3a5246"], bark=["4e4a30", "6a6640", "8a8656"],
        flower=["fcf6f4", "e8c8d4", "8a3aa0"], unripe=["5e7a24", "9ab83e", "d0e07a"], ripe=["34400e", "5e7420", "9cac50"],
        clump=(2.0, 3.0), stretch=(1.0, 1.0), coverage=0.8, style="round", soft=True,
        low=True, fruit="caper", bunches=5, nmax=2, seed=91),
    "red_currant": dict(  # lobed bright green leaves, hanging strings of glossy see-through red currants
        leaf=["aee27e", "78bc4c", "56983a", "3c742c", "264e1e"], bark=["4a3828", "6a523a", "8c7250"],
        flower=["d8dc7a", "a8b050", "f0f0b0"], unripe=["5a8a2e", "8ac04a", "d4f0a0"], ripe=["8a0a12", "dc2a2a", "ff9088"],
        clump=(2.4, 3.4), stretch=(1.0, 1.0), coverage=0.76, style="maple", soft=True, fruit="currant", bunches=5, seed=101),
    "black_currant": dict(  # the same lobed leaves, darker; dull pink flower strings, glossy black currants
        leaf=["8ec468", "5c9a40", "427c32", "2e5c26", "1e3c18"], bark=["40302a", "5e4636", "7e624a"],
        flower=["d8a8b0", "a87884", "e8e0a0"], unripe=["4a7a2a", "78aa44", "c0e090"], ripe=["08080c", "1e1c28", "4c5470"],
        clump=(2.4, 3.4), stretch=(1.0, 1.0), coverage=0.76, style="maple", soft=True, fruit="currant", bunches=5, seed=113,
        dim_glint=True),
})

def despeckle(px):
    """Soft clumps: a pixel no neighbour shares, sitting among 3+ neighbours of one colour, takes that colour; leaf pixels
    with at most one leaf neighbour (stray single pixels at the rim) go."""
    out = dict(px)
    for (x, y), c in px.items():
        nb = [px.get(((x + ox) % 16, (y + oy) % 16)) for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
        filled = [n for n in nb if n]
        if len(filled) <= 1:
            del out[(x, y)]
        elif c not in filled:
            best = max(set(filled), key=filled.count)
            if filled.count(best) >= 3:
                out[(x, y)] = best
    return out


def leaf_mass(p, seed, coverage=None, darken=0, leaf=None):
    """The leaves of a kind: lit clumps (the phase 1 look), then the kind's own surface: needle sprigs (heath), a glossy
    rim of light on the top left of every leaf (leathery and fleshy leaves), notched lobes (currants). Trailing kinds get
    their runners showing in the gaps between the leaves."""
    p = dict(p, leaf=leaf or p["leaf"])
    px = clumps(p, seed, coverage, darken)
    style = p.get("style")
    rng = random.Random(seed + 2)
    L = p["leaf"]
    if style == "needle":  # short upright needle tufts, lit at the tips
        for _ in range(p["stamps"]):
            x, y = rng.randrange(16), rng.randrange(16)
            put(px, x, y, L[min(4, 2 + darken)])
            put(px, x, y - 1, L[min(4, 1 + darken)])
            put(px, x + rng.choice((-1, 1)), y - 2, L[min(4, darken)])
    if style in ("oval", "round"):  # glossy: light catches every leaf edge facing the top left
        for (x, y), c in list(px.items()):
            if ((x - 1) % 16, y) not in px and (x, (y - 1) % 16) not in px:
                px[(x, y)] = L[min(4, darken)]
    if style == "maple":  # a few small notches in the upper rims of the broad clumps, so they read as lobed leaves
        for (x, y) in list(px):
            if ((x, (y - 1) % 16) not in px and ((x - 1) % 16, y) in px and ((x + 1) % 16, y) in px
                    and rng.random() < 0.18):
                px.pop((x, y), None)
    for _ in range(p.get("trail", 0)):  # runners, seen only in the gaps
        x, y, dx = rng.randrange(16), rng.randrange(16), rng.choice((-1, 1))
        for i in range(16):
            if (x % 16, y % 16) not in px:
                px[(x % 16, y % 16)] = p["bark"][1 if i % 4 else 2]
            x += dx
            if rng.random() < 0.35:
                y += rng.choice((-1, 1))
    return despeckle(px) if p.get("soft") else px


def put(px, x, y, c):
    px[(x % 16, y % 16)] = c if isinstance(c, str) else c


def berry2(px, x, y, col):
    """a round 2x2 berry: highlight top left, shadow bottom right"""
    put(px, x, y, col[2])
    put(px, x + 1, y, col[1])
    put(px, x, y + 1, col[1])
    put(px, x + 1, y + 1, col[0])


def draw_fruit(px, p, spots, stage):
    """stage 1 flowers, 2 unripe, 3 ripe, on the same twig tips. Low kinds sit on the mat, no twig."""
    col = {1: p["flower"], 2: p["unripe"], 3: p["ripe"]}[stage]
    rng = random.Random(p["seed"] + 77)
    f = p["fruit"]
    for si, (x, y, dx, n, k) in enumerate(spots):
        n = min(n, p.get("nmax", n))
        if p.get("low"):
            tx, ty = x, y - 2
        elif f in ("barb", "currant"):  # a short twig reaching out sideways, the string hangs from its end
            tx, ty = max(4, min(11, x)), y - 4  # off the tile edge, where the box corners would cut the string
            for i in range(3):
                put(px, tx, ty, p["bark"][1])
                tx, ty = tx + dx, ty - (i % 2)
        else:
            tx, ty = twig_line(px, p, x, y, 3 + int(k * 2), dx, rng)
        if f == "hip":
            if stage == 1:  # a pink five-petalled rose round a yellow heart
                for ox, oy, c in ((0, -1, 0), (-1, 0, 0), (1, 0, 1), (-1, 1, 1), (1, 1, 1), (0, 1, 1), (-1, -1, 0)):
                    put(px, tx + ox, ty + oy, col[c])
                put(px, tx, ty, col[2])
            else:  # an oval hip 2x3 hanging from the tip, glossy spot, dark withered sepals at its end
                for hx, hy in ((tx, ty), (tx + 2 * dx, ty + 1))[: 1 + (n > 2)]:
                    for ox, oy, c in ((0, 0, 2), (1, 0, 1), (0, 1, 1), (1, 1, 1), (0, 2, 1), (1, 2, 0)):
                        put(px, hx + ox, hy + oy, col[c])
                    put(px, hx + 1, hy + 3, p["bark"][0])
        elif f in ("heath", "bear"):
            pts = [(tx, ty), (tx + 2, ty + 1), (tx - 1, ty + 2), (tx + 1, ty + 3)][:n + (f == "heath")]
            for bx, by in pts:
                if stage == 1 and f == "heath":  # tiny purple-red flowers, one pixel each
                    put(px, bx, by, col[0])
                elif stage == 1:  # little white urns, pink at the mouth, hanging
                    put(px, bx, by, col[0])
                    put(px, bx, by + 1, col[1])
                elif f == "heath":  # small beads, 1-2 px, a dim highlight only on a few of the ripe ones
                    rb = random.Random(bx * 31 + by)
                    put(px, bx, by, col[1])
                    if rb.random() < 0.5:
                        put(px, bx, by + 1, col[0])
                    if stage == 3 and rb.random() < 0.3:
                        put(px, bx, by, col[2])
                else:
                    berry2(px, bx, by, col)
        elif f == "barb":  # a stalk hanging from the tip with berries (or flowers) left and right down it
            for i in range(1, 4):
                put(px, tx, ty + i, p["bark"][2])
            for i, (ox, oy) in enumerate(((-1, 1), (1, 2), (-1, 3), (1, 4), (0, 5))[: n + 1]):
                if stage == 1:
                    put(px, tx + ox, ty + oy, col[0 if i % 2 else 2])
                    put(px, tx + ox, ty + oy + 1, col[1])
                else:  # elongated berries 1x2
                    put(px, tx + ox, ty + oy, col[2] if ox <= 0 else col[1])
                    put(px, tx + ox, ty + oy + 1, col[1] if ox <= 0 else col[0])
        elif f == "caper":
            flower = (stage == 2 and si < 2) or (stage == 3 and si == 0)  # only a few flowers open at once
            if flower:  # a round white flower, the long violet stamens bursting out past the petals
                for j, row in enumerate((".WW..", "WWWw.", "WWVww", ".www.", "...V.")):
                    for i, ch in enumerate(row):
                        if ch != ".":
                            put(px, tx + i - 2, ty + j - 2, p["flower"][{"W": 0, "w": 1}.get(ch, 2)])
            elif stage in (1, 2):  # small green buds
                for ox, oy in ((0, 0), (2, 1), (-1, 2))[:n]:
                    put(px, tx + ox, ty + oy, p["unripe"][2])
                    put(px, tx + ox, ty + oy + 1, p["unripe"][0])
            else:  # plump olive buds, the capers
                for ox, oy in ((0, 0), (3, 1), (-3, 1), (0, 3))[:n]:
                    for kx, ky, c in ((1, 0, 2), (0, 1, 2), (1, 1, 1), (2, 1, 0), (1, 2, 0)):
                        put(px, tx + ox + kx - 1, ty + oy + ky - 1, col[c])
        elif f == "currant":  # a string hanging from the tip: 3 beads down a thin stalk
            for i in range(1, 4):
                put(px, tx, ty + i, p["bark"][2])
            for i, (ox, oy) in enumerate(((0, 1), (-1, 3), (0, 5))[: n + 1]):
                if stage == 1:
                    put(px, tx + ox, ty + oy, col[2])
                    put(px, tx + ox + 1, ty + oy + 1, col[1])
                else:
                    berry2(px, tx + ox, ty + oy, col)
                    if p.get("dim_glint") and stage == 3 and (si + i) % 3:  # most black currants without a glint
                        put(px, tx + ox, ty + oy, col[1])


def thorny_twigs(p, seed, count=6):
    px = bare_twigs(p, seed, count)
    if p.get("thorny"):
        rng = random.Random(seed + 1)
        for (x, y), c in list(px.items()):
            if rng.random() < 0.1:
                put(px, x + rng.choice((-1, 1)), y - 1, p["bark"][2])
    return px


def blossom_twigs(p, bare, seed):
    """Blackthorn in spring: dark bare twigs carrying clusters of small five-petalled blossoms (a yellow-green eye, white
    petals, the lower ones half hidden behind the upper), a few closed buds at the tips, a couple of young leaves."""
    rng = random.Random(seed)
    px = dict(bare)
    px.update(clumps(p, seed, 0.12))  # the first young leaves
    for pos, c in bare.items():  # the twigs stay on top of the young leaves
        px[pos] = c
    twig = sorted(bare)
    rng.shuffle(twig)
    centres = []
    for x, y in twig:
        if all(abs(x - a) + abs(y - b) >= 4 for a, b in centres):
            centres.append((x, y))
        if len(centres) >= 11:
            break
    white, shade, eye = p["flower"]
    eye = "b4bc4c"  # a yellow-green eye
    for cx, cy in centres:
        blossoms = [(cx, cy)] + [(cx + rng.choice((-3, 3)), cy + rng.choice((-1, 1, 2)))
                                 for _ in range(rng.randint(0, 1))]
        for bx, by in sorted(blossoms, key=lambda b: b[1]):  # top ones first, lower ones overlap
            for ox, oy, c in ((0, -1, white), (-1, 0, white), (1, 0, shade), (0, 1, shade)):
                put(px, bx + ox, by + oy, c)
            if rng.random() < 0.5:  # the fifth petal peeking out at a corner
                put(px, bx + rng.choice((-1, 1)), by + 1, shade)
            put(px, bx, by, eye)
    for x, y in twig[:6]:  # closed buds sitting on the twigs
        if all(abs(x - a) + abs(y - b) >= 3 for a, b in centres):
            put(px, x, y, shade)
    return px


SPRIG_LEAF = {"blackthorn": "small", "sea_buckthorn": "narrow", "jujube": "large", "rosehip": "medium", "crowberry": "needle",
              "bearberry": "small", "barberry": "small", "caper": "medium", "red_currant": "large", "black_currant": "large"}


def sprig_images(name, p):
    """The two upright planes sticking out of the bush: 3-5 tidy leafy shoots, a thin stem growing up and out to the left
    and right edges with paired leaves along it and a leafy tip, clear between them. _sprig_bare: the same shoots as bare
    twigs (evergreen mats: the shoots in their winter leaves). Visible rows: bush lower part 3-14, upper 7-16, mats 11-16."""
    rng = random.Random(p["seed"] + 21)
    kind = SPRIG_LEAF[name]
    low = p.get("low")
    shoots = []  # (start x, start y, end x, end y)
    if low:
        for ex, ey in ((0, 11), (15, 11), (2, 12), (13, 12)):
            shoots.append((rng.randint(6, 9), 15, ex, ey, 1.0))
    else:
        # separate shoots fanning out: the tall ones rise straight and bend late, the low ones lean out early
        for ex, ey, sx, bend in ((0, 3, 6, 2.2), (15, 4, 9, 2.2), (0, 9, 5, 1.0), (15, 8, 10, 1.0)):
            shoots.append((sx + rng.choice((0, 1)) * (1 if sx > 8 else -1), 15, ex, ey, bend))

    def paint(leafy, leaf):
        px = {}
        for sx, sy, ex, ey, bend in shoots:
            pts = []
            for i in range(41):  # rises first, then bends out toward the edge
                t = i / 40
                q = (round(sx + (ex - sx) * t ** bend), round(sy + (ey - sy) * (t ** 0.6 if low else t)))
                if not pts or pts[-1] != q:
                    pts.append(q)
            for j, (x, y) in enumerate(pts):
                px[(x % 16, y)] = p["bark"][0 if j < len(pts) * 0.3 else 1]
            for j in range(int(len(pts) * 0.4), len(pts) - 1, 3 if kind != "needle" else 1):  # bare stem low down
                (x, y), (x2, y2) = pts[j], pts[min(len(pts) - 1, j + 1)]
                dx, dy = x2 - x, y2 - y
                nx, ny = -dy, dx  # the two sides of the stem
                if not leafy:
                    if p.get("thorny") and j % 2:
                        put(px, x + nx, y + ny, p["bark"][2])
                    elif j % 3 == 2:
                        put(px, x + nx, y + ny, p["bark"][2])  # a bud
                    continue
                for side in (1, -1):
                    sxn, syn = nx * side, ny * side
                    lit = sxn + syn <= 0  # the side facing the top left
                    c0, c1 = (leaf[0], leaf[1]) if lit else (leaf[2], leaf[3])
                    if kind == "needle":
                        if (j + side) % 2:
                            put(px, x + sxn, y + syn, c1)
                        continue
                    put(px, x + sxn, y + syn, c1)
                    if kind in ("medium", "large", "narrow"):
                        put(px, x + 2 * sxn + dx, y + 2 * syn + dy, c0)
                    if kind == "large":
                        put(px, x + sxn + dx, y + syn + dy, c0)
                        put(px, x + 2 * sxn, y + 2 * syn, c1)
                    if kind == "small":
                        put(px, x + sxn + dx, y + syn + dy, c0)
                    if kind == "medium":
                        put(px, x + sxn + dx, y + syn + dy, c1)
                if p.get("thorny") and j % 2:
                    put(px, x - nx, y - ny, p["bark"][2])
            if leafy:  # the leafy tip
                x, y = pts[-1]
                put(px, x, y, leaf[1])
                put(px, x, y - 1, leaf[0])
        return {k: v for k, v in px.items() if 0 <= k[1] < 16}

    leaf = p["leaf"]
    return {f"block/{name}_sprig.png": to_img(paint(True, leaf)),
            f"block/{name}_sprig_bare.png": to_img(paint(True, p["winter"]) if p.get("evergreen") else paint(False, leaf))}


def thicket_images(name, p):
    out = {}
    leaves = leaf_mass(p, p["seed"])
    spots = bunches(p, p["seed"] + 1)
    bare = thorny_twigs(p, p["seed"] + 5)
    new = "style" in p or p["fruit"] == "hip"

    def fruit(px, stage):
        if new:
            draw_fruit(px, p, spots, stage)
        else:
            draw_bunches(px, p, spots, {1: p["flower"], 2: p["unripe"], 3: p["ripe"]}[stage], flowers=stage == 1)
        return px

    def canes(px):  # thorny arching canes showing through the leaves (dog rose)
        rng = random.Random(p["seed"] + 4)
        for _ in range(p.get("canes", 0)):
            x, y, dx = rng.randrange(16), 15, rng.choice((-1, 1))
            for i in range(12):
                put(px, x, y, p["bark"][1])
                if i % 3 == 1:
                    put(px, x - dx, y, p["bark"][2])  # a thorn
                y -= 1
                x += dx if i % 2 else 0
        return px

    leaves = canes(leaves)
    out[f"block/{name}_stage0.png"] = to_img(leaves)
    if p.get("blossom_heavy"):  # blackthorn flowers before its leaves
        out[f"block/{name}_stage1.png"] = to_img(blossom_twigs(p, bare, p["seed"] + 9))
    else:
        out[f"block/{name}_stage1.png"] = to_img(fruit(dict(leaves), 1))
    for stage in (2, 3):
        out[f"block/{name}_stage{stage}.png"] = to_img(fruit(dict(leaves), stage))
    if p.get("evergreen"):  # the mat keeps its leaves through winter, duller
        bare = leaf_mass(p, p["seed"], leaf=p["winter"])
    out[f"block/{name}_bare.png"] = to_img(bare)
    out[f"block/{name}_bare_ripe.png"] = to_img(fruit(dict(bare), 3))
    # inner layer: the shaded heart of the bush, seen through the gaps (darker, denser)
    out[f"block/{name}_inner.png"] = to_img(clumps(dict(p, clump=p.get("clump", (1.4, 2.2)), stretch=p.get("stretch", (1, 1)),
                                                         coverage=0.85), p["seed"] + 13, 0.85, darken=2))
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
    out.update(sprig_images(name, p))
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


_COOKED = [  # a soft mash in a leaf, the cooked_elderberries layout
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
]
_CURRANTS = [  # two strings of currants hanging from a leafy twig
    "................",
    "..g.............",
    ".gGs............",
    "...s............",
    "....s...........",
    "....sABC........",
    "....ABBCD.......",
    "...ABBCCD.......",
    "....CBCDsABC....",
    ".....DD.ABBCD...",
    "....ABC.BBCCD...",
    "...ABBCD.CDDABC.",
    "...BBCCD...ABBCD",
    "....CDD....BBCCD",
    "............CDD.",
    "................",
]
ITEMS.update({
    "rosehips": dict(pal={"A": "ffa088", "B": "e03a20", "C": "a81a10", "D": "6a0c08", "k": "3a2418", "s": "5a7a34", "g": "4f7a38"},
                     rows=[
                         "................",
                         "........g.......",
                         ".......gsg......",
                         "......s...s.....",
                         ".....s.....s....",
                         "....ABB....s....",
                         "...ABBBC..ABB...",
                         "...ABBBC.ABBBC..",
                         "...BBBBC.ABBBC..",
                         "...BBBCC.BBBBC..",
                         "...CBBCD.BBBCC..",
                         "....CCD..CBBCD..",
                         "....kDk..CBCCD..",
                         ".....k....CCD...",
                         "..........kDk...",
                         "...........k....",
                     ]),
    "crowberries": dict(pal={"A": "dce6f6", "B": "3a3a50", "C": "1e1e2c", "D": "0c0c14", "g": "5a7c3e", "G": "86a65a",
                             "d": "3a5a2a", "s": "6a3424"},
                        rows=[
                            "................",
                            "..........G.g...",
                            ".........gGgd...",
                            "........sGgd....",
                            ".......sgGd.....",
                            "......s.gd......",
                            "...BBC.sBBC.....",
                            "..BABCCBABCC....",
                            "..BBBCCBBBCC....",
                            "..CBCCDCBCCD....",
                            "...CCD.BBC......",
                            ".....BBABCC.....",
                            ".....BBBBCC.....",
                            ".....CBCCCD.....",
                            "......CCDD......",
                            "................",
                        ]),
    "bearberries": dict(pal={"A": "ffc0b0", "B": "e0261e", "C": "a01410", "D": "600a0a", "g": "5e9e44", "G": "a8da84",
                             "d": "2e6026", "s": "7e3a24"},
                        rows=[
                            "................",
                            "................",
                            "..........GGg...",
                            ".........GGggd..",
                            "........Gggdd...",
                            ".....GGg.gdd....",
                            "....GGggd.s.....",
                            "....Ggdd.s......",
                            ".....dd.s.......",
                            "...ABB.sABB.....",
                            "..ABBBCABBBC....",
                            "..BBBBCBBBBC....",
                            "..CBBCDCBBCD....",
                            "...CDABBCDD.....",
                            ".....BBBBC......",
                            ".....CCCDD......",
                        ]),
    "barberries": dict(pal={"A": "ff9a90", "B": "dc1e24", "C": "9a0c14", "s": "7a5a34", "g": "80a886", "G": "b0d0ac"},
                       rows=[
                           "................",
                           "...gG...........",
                           "..gGg..s........",
                           "...g..s.........",
                           ".....s.s........",
                           "....AB.s........",
                           "....BCsAB.......",
                           "....BC.BC.s.....",
                           ".....AB.BCs.....",
                           "....sBC..AB.....",
                           "...AB.C..BC.....",
                           "...BC.AB..C.AB..",
                           "...BC.BC...sBC..",
                           "....C.BC..AB.C..",
                           "......C...BC....",
                           "...........C....",
                       ]),
    "capers": dict(pal={"A": "c8d488", "B": "7e8e34", "C": "56641e", "D": "3a4412"},
                   rows=[
                       "................",
                       "................",
                       "................",
                       "................",
                       "................",
                       "......ABC.......",
                       ".....ABBCD......",
                       "..ABCBBCCDABC...",
                       ".ABBCDCDDABBCD..",
                       ".BBCCDABCBBCCD..",
                       "..CDDABBCDCDD...",
                       "....ABBCCDABC...",
                       "....BBCCDABBCD..",
                       ".....CDD.BBCCD..",
                       "..........CDD...",
                       "................",
                   ]),
    "red_currants": dict(pal={"A": "ffb0a8", "B": "f04040", "C": "c0181c", "D": "7a0a10", "s": "6a523a", "g": "78bc4c",
                              "G": "aee27e"}, rows=_CURRANTS),
    "black_currants": dict(pal={"A": "b4bcd8", "B": "3a3850", "C": "1e1c2a", "D": "08080e", "s": "5e4636", "g": "5c9a40",
                                "G": "8ec468"}, rows=_CURRANTS),
    "rowan_berries": dict(pal={"A": "ffb070", "B": "f05a18", "C": "c03a10", "D": "5a1808", "s": "6a5a3a", "g": "6a9448", "G": "9cc46a"},
                          rows=[
                              "................",
                              "...........gG...",
                              "..........gGg...",
                              ".........s......",
                              "........s.......",
                              ".....s.s.s......",
                              "....s.s...s.s...",
                              "...s.AB.sAB..s..",
                              ".AB..BBCsBBC.AB.",
                              ".BBC.sCD..CD.BBC",
                              "..CD...AB.....CD",
                              "...AB..BBC.AB...",
                              "...BBC..CD.BBC..",
                              "....CDAB..ABCD..",
                              "......BBC.BBC...",
                              ".......CD..CD...",
                          ]),
    "cooked_rowan_berries": dict(pal={"A": "f08060", "B": "a82814", "C": "701810", "D": "4a0c08", "S": "ffb090",
                                      "g": "4f7a38", "G": "6e9a48", "d": "355a26"}, rows=_COOKED),
    "yew_arils": dict(pal={"A": "ff8a8a", "B": "e02838", "C": "a8142a", "D": "6a0a1a", "k": "26261a", "K": "4e4e34",
                           "s": "5a4a2a", "g": "3e6a30", "G": "5a8a44"},
                      rows=[
                          "................",
                          "..........G.G...",
                          ".........GgGg...",
                          "........sGg.G...",
                          ".......s.g......",
                          "......s.........",
                          "...ABBs.........",
                          "..ABKkC..ABB....",
                          "..BkkkC.ABKkC...",
                          "..CBBCD.BkkkC...",
                          "...CDD..CBBCD...",
                          ".....ABB.CDD....",
                          "....ABKkC.......",
                          "....BkkkC.......",
                          "....CBBCD.......",
                          ".....CDD........",
                      ]),
    "bird_cherries": dict(pal={"A": "8a90b8", "B": "2a2838", "C": "16141e", "D": "08080c", "s": "5a7a34"},
                          rows=[
                              "................",
                              "........s.......",
                              ".......s.s......",
                              "......s...s.....",
                              ".....s.s...s....",
                              "....s..s....s...",
                              "....s...s...s...",
                              "...s....s....s..",
                              "..ABB...s...ABB.",
                              ".ABBBC..s..ABBBC",
                              ".BBBBC.ABB.BBBBC",
                              ".CBBCDABBBCCBBCD",
                              "..CDD.BBBBC.CDD.",
                              "......CBBCD.....",
                              ".......CDD......",
                              "................",
                          ]),
    "crab_apple": dict(pal={"A": "f0f0a0", "B": "c8d050", "C": "98a838", "D": "6a7a28", "R": "d85030", "r": "a83828",
                            "s": "6a4a2a", "g": "5a9a3a", "G": "8cc860"},
                       rows=[
                           "................",
                           "................",
                           "................",
                           ".......s.Gg.....",
                           ".......sgg......",
                           ".....BBsBB......",
                           "....BABBBBC.....",
                           "...BAABBRRC.....",
                           "...BABBRRrC.....",
                           "...BBBRRrrD.....",
                           "...CBBRrrrD.....",
                           "....CCrrrD......",
                           ".....DDDD.......",
                           "................",
                           "................",
                           "................",
                       ]),
    "wild_plums": dict(pal={"A": "c4c0e0", "a": "8a86b8", "B": "4e3e7a", "C": "34285a", "D": "1e1838", "s": "5a4a2a",
                            "g": "5a8a3a"},
                       rows=[
                           "................",
                           "................",
                           "......gg........",
                           ".....gs.........",
                           "....aBsB........",
                           "...aABBBC.......",
                           "..aABBBBCC......",
                           "..aBBBaBCC.s....",
                           "..BBBBBCCD.s....",
                           "..CBBBCCDaaBB...",
                           "...CCCCDaABBBC..",
                           "....DDDaABBBBCC.",
                           "........aBBBaCC.",
                           "........BBBBCCD.",
                           ".........CCCDD..",
                           "................",
                       ]),
    "olives": dict(pal={"A": "c8d078", "B": "8a9a3a", "C": "5e6e22", "K": "6a5a7a", "L": "3a2a40", "M": "1e1622",
                        "l": "cfd8c8", "u": "aebca0", "v": "62745a", "s": "6a5a3a"},
                   rows=[
                       "................",
                       "..........vu....",
                       ".........vuu....",
                       "........vul.....",
                       ".......vul......",
                       "......sul.......",
                       ".....s.l........",
                       "...ABB.s..KLL...",
                       "..ABBBC.sKLLLM..",
                       "..BBBBC..LLLLM..",
                       "..CBBCC..MLLMM..",
                       "...CCC.ABB.MM...",
                       "......ABBBC.....",
                       "......BBBBC.....",
                       "......CBBCC.....",
                       ".......CCC......",
                   ]),
    "dates": dict(pal={"A": "eaa458", "B": "a85a1c", "C": "6e3410", "D": "4a200a", "s": "c8a060"},
                  rows=[
                      "................",
                      "......s.........",
                      ".....s.s........",
                      "....s..s.s......",
                      "...AB..s..s.....",
                      "..ABC.AB...s....",
                      "..BBC.BBC.AB....",
                      "..BCC.BBC.BBC...",
                      "..CCD.BCC.BBC...",
                      "...D..CCD.BCC...",
                      "....AB.D..CCD...",
                      "...ABBC....D....",
                      "...BBBC.AB......",
                      "...BCCD.BBC.....",
                      "....CD..BCC.....",
                      "........CD......",
                  ]),
})


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


def ball(img, cx, cy, r, pal, seed, dimples=True):
    """A round fruit lit from the upper left: pal = highlight, light, body, shade, dark rim; a pitted peel (dimples)."""
    import math
    import random
    rnd = random.Random(seed)
    for y in range(16):
        for x in range(16):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy) / r
            if d > 1:
                continue
            t = 0.55 * d + 0.35 * (dx + dy) / (2 * r) + 0.25
            k = 0 if t < 0.16 else 1 if t < 0.36 else 2 if t < 0.66 else 3 if d < 0.88 else 4
            if dimples and 1 < k < 4 and rnd.random() < 0.08:
                k += 1  # the pits of the peel
            img.putpixel((x, y), hexc(pal[k]))


def bitter_oranges():
    """Two bitter oranges, the back one half hidden, a glossy leaf and a stalk on the front one."""
    img = Image.new("RGBA", (16, 16))
    ORANGE = ("ffd070", "f8a028", "e8800c", "b85a06", "6e3004")
    ball(img, 10.8, 7.6, 3.9, ORANGE, 11)
    ball(img, 6.0, 10.6, 4.6, ORANGE, 12)
    for x, y, c in ((5, 6, "5a4426"), (6, 5, "5a4426"), (6, 6, "8a6a3a"),  # the stalk end
                    (7, 4, "2a5a22"), (8, 3, "3e7a2e"), (9, 3, "5a9a3e"), (8, 4, "3e7a2e"), (9, 4, "2a5a22"), (10, 2, "8ac464"),
                    (10, 3, "5a9a3e"), (11, 2, "3e7a2e"), (7, 5, "1e4018"), (4, 8, "fff0b0"), (9, 5, "ffe8a0")):  # leaf, glints
        img.putpixel((x, y), hexc(c))
    return img


def fern_flower_charm():
    """A blooming Fern Flower bound to a loop of twine: the gold five-petalled flower glowing over two curled fronds."""
    import math
    img = Image.new("RGBA", (16, 16))
    C, c = hexc("c4a46c"), hexc("8a6c40")
    for k in range(40):  # the loop of twine, lit at the top left
        a = k / 40 * 2 * math.pi
        x, y = 8 + 3.4 * math.cos(a), 3.6 + 2.6 * math.sin(a)
        img.putpixel((int(round(x)), int(round(y))), C if math.sin(a) < 0.2 and math.cos(a) < 0.6 else c)
    for x, y in ((7, 6), (8, 6), (8, 7)):
        img.putpixel((x, y), c)  # the knot
    img.putpixel((7, 7), C)
    G, g, d = hexc("8ccc5c"), hexc("5f9a3f"), hexc("2e5a22")
    for side in (-1, 1):  # two little fronds curling out under the flower
        for i in range(6):
            x, y = 8 + side * (1 + i * 0.9), 14 - 0.4 * i - (0.4 * (i - 3) ** 2 if i > 3 else 0)  # the tips curl up
            img.putpixel((int(round(x)), int(round(y))), g if i < 4 else d)
            if i % 2 == 1:
                img.putpixel((int(round(x)), int(round(y)) - 1), G)
                img.putpixel((int(round(x)), int(round(y)) + 1), d)
    PET = {"w": "fffbe0", "y": "ffe27a", "o": "f2b830", "a": "c8861a", "k": "8a5a10"}
    flower = ["..y.y..", ".yoyoy.", "yowwwoy", ".ywwwy.", "yowwwoa", ".aoyoa.", "..a.a.."]
    for j, row in enumerate(flower):
        for i, ch in enumerate(row):
            if ch != ".":
                img.putpixel((5 + i, 8 + j), hexc(PET[ch]))
    img.putpixel((8, 10), hexc("ffffff"))  # the heart, brightest
    return img


def build():
    out = fern_parts()
    out["item/bitter_oranges.png"] = bitter_oranges()
    out["item/fern_flower_charm.png"] = fern_flower_charm()
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
