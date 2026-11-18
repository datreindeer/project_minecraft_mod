# Wild mushroom block (cross model) + item textures, and roasted_mushrooms. Hand-drawn pixel grids.
# Sized after vanilla brown/red_mushroom (8x8, standing on the bottom edge); items fill the frame more.
# usage: python scripts/gen_mushroom_textures.py [preview.png]
import sys
from pathlib import Path
from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def draw(layers):
    """layers: (rows, x, y, palette) drawn in order; '.' is transparent."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for rows, ox, oy, pal in layers:
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    assert ch in pal, f"unknown colour {ch!r}"
                    img.putpixel((ox + x, oy + y), hexc(pal[ch]))
    return img


def grid(rows, pal):
    """Full-width grid, padded at the top so it stands on the bottom edge."""
    assert len(rows) <= 16 and all(len(r) == 16 for r in rows), [r for r in rows if len(r) != 16]
    return draw([(rows, 0, 16 - len(rows), pal)])


# ---------------- chanterelle: golden trumpet, wavy rim, false gills down the stem ----------------
CHANT = {"l": "ffd65c", "Y": "f0b32a", "y": "d9961c", "o": "b8781a", "q": "9a5f12", "O": "7d4a0e",
         "g": "eaa82a", "G": "b27014", "s": "f2c25a", "S": "c48f2a"}
CHANTERELLE_BLOCK = [
    "...l.lYl.Yl.....",
    "..lYYooooYYl....",
    "..yYYYYYYYYyO...",
    "...yggGgGggO....",
    "....ygGgGgOl.lYl",
    ".....gGgGOlYooYY",
    "......sSO.yYYyO.",
    "......sSO..gGO..",
    "......sSO..sS...",
]
CHANTERELLE_ITEM = [
    "................",
    "...l.ll.l.......",
    "..lYlYYlYl.l....",
    ".lYYoooooYlY....",
    ".YYoqqqqooYYl...",
    ".yYYoooooYYYyO..",
    "..yYYYYYYYYyO...",
    "...yggGgGgGO....",
    "....ygGgGgOl.lYl",
    ".....gGgGOlYooYY",
    ".....ygGgO.yYYyO",
    "......sSO..gGgO.",
    "......sSO...sS..",
    "......sSO...sS..",
    ".....sSSO...sS..",
    "................",
]

# ---------------- porcini: fat pale netted stem, big brown bun cap ----------------
PORC = {"h": "c99459", "c": "a8723c", "C": "8a5a2e", "D": "6b4220", "K": "4a2d16",
        "u": "d9c47e", "U": "b8a25e", "W": "f0e6cc", "w": "dccfae", "n": "c4b48e", "v": "b3a27c"}
PORCINI_BLOCK = [
    ".....hhccD......",
    "....hccCCCDD....",
    "...hcCCCCCCDD...",
    "...cCCCCCCDDDK..",
    "...KDDDDDDDDK...",
    "....uUuUuUuU....",
    ".....WWwwwv.....",
    "....WWwnwwvv....",
    "....WWwwnwvv....",
    ".....Wwwwvv.....",
]
PORCINI_ITEM = [
    "................",
    ".....hhhccC.....",
    "...hhhccccCCD...",
    "..hhccCCCCCCDD..",
    ".hhcCCCCCCCCDDD.",
    ".hcCCCCCCCCCDDK.",
    ".cCCCCCCCCCDDDK.",
    ".KDCCCCCCCDDDKK.",
    "..KKDDDDDDDDKK..",
    "...uUuUuuUuUu...",
    "....WWwwwwvv....",
    "....WWwnwwvv....",
    "...WWwwwnwwvv...",
    "...WWwnwwwnvv...",
    "....Wwwwwvvv....",
    ".....vvvvvv.....",
]

# ---------------- morel: cone of honeycomb pits on a pale stem ----------------
MOREL = {"r": "b8935f", "R": "8c6a40", "p": "4e3620", "P": "33231a", "s": "ece2c8", "S": "cdbf9c", "T": "a89a78"}
MOREL_BLOCK = [
    "......rR........",
    ".....rprR.......",
    ".....prpR.......",
    "....rrrrRR......",
    "....rprpRP......",
    "....pRpRpR..rR..",
    "....rrrRRR.rpRP.",
    "....prpRpP.prpR.",
    "....RRRRRP.rRRP.",
    ".....sSST..RpRP.",
    ".....sSST...sST.",
    ".....sSST...sST.",
    "....ssSSTT..sST.",
]
MOREL_ITEM = [
    "................",
    ".....rR.........",
    "....rprR........",
    "....prpRP.......",
    "...rrrrRRR......",
    "...rpPrpRP......",
    "...pRprPRP..rR..",
    "...rrrrRRR.rprR.",
    "...prPrpRP.prpRP",
    "...pRpRpRP.rrRRP",
    "...rrrRRRP.pRpRP",
    "...RpRpRPP.rRRPP",
    "....sSSTT...sST.",
    "....sSSTT...sST.",
    "....sSSTT...sST.",
    "...ssSSTTT.ssSTT",
]

# ---------------- oyster mushroom: overlapping grey-beige fan shelves ----------------
OYST = {"a": "d6ccb6", "b": "b5aa94", "c": "948a76", "d": "6f6758", "g": "ebe4d2", "G": "c8bfac",
        "s": "ddd4c0", "S": "b8ae98"}
OYSTER_BLOCK = [  # shelves joined by short side stalks to one base
    ".....aaab.......",
    "...aabbbbc......",
    "..abbbbbbcc.....",
    "..dgGgGgGdd.....",
    "...sddddd.aab...",
    "...s....abbbbc..",
    "...sS..abbbbbcc.",
    "...sSssdgGgGgdd.",
    "...sS...ddddd...",
    "..ssSS..........",
]
OYSTER_ITEM = [
    "................",
    "......aaaab.....",
    "....aabbbbbbc...",
    "...abbbbbbbbcc..",
    "..aabbbbbbbbccd.",
    "..dgGgGgGgGgGdd.",
    "...sdddddddddd..",
    "...s.aaab.......",
    ".aaabbbbbbc.....",
    "abbbbbbbbbcc....",
    "dgGgGgGgGgGd....",
    ".sddddddddd.....",
    ".sS...aaaabbbbc.",
    ".sSssabbbbbbbbcc",
    ".sS..dgGgGgGgGgd",
    "ssSS...dddddddd.",
]

# ---------------- fly agaric: red cap, white warts, white stem with ring ----------------
FLY = {"L": "fe2a2a", "M": "e21212", "N": "c41d26", "Q": "a9121a", "w": "f6f2e4", "x": "d8cfba",
       "g": "f0ead6", "G": "d2cab0", "s": "f2eedc", "S": "d4ceb4", "T": "b5ae92", "W": "ffffff"}
FLY_AGARIC_BLOCK = [
    "......wLLM......",
    "....LLLLwMMM....",
    "...LwLLLMMMwN...",
    "...LLLMwMMMNN...",
    "..wLMMMMMNNwNQ..",
    "..QNNNNNNNNQQQ..",
    "....gGgGgGgG....",
    "......sST.......",
    ".....WsssT......",
    "......sST.......",
    "......sST.......",
    ".....ssSTT......",
]
FLY_AGARIC_ITEM = [
    "................",
    "......LLLLM.....",
    "....LwwLLLMMM...",
    "...LLwwLLMwwMN..",
    "..LLLLLMMMwxMNN.",
    "..LwwLMMMMMMNNN.",
    ".LLwxMMMwwMNNQQ.",
    ".MMMMMwxMNNNwxQ.",
    ".QNNNNNNNNNQQQQ.",
    "..gGgGgGgGgGgG..",
    "......sST.......",
    ".....WssssT.....",
    "......WssT......",
    "......sST.......",
    ".....ssSTT......",
    ".....sSSTT......",
]

# ---------------- puffball: round white-cream ball, tiny bumps, pinched base ----------------
PUFF = {"a": "fffbea", "b": "f1ead2", "c": "ddd4b8", "d": "c3b99a", "e": "a39878", "k": "e0d6bb",
        "f": "9c8c6c", "F": "7e6f52"}
PUFFBALL_BLOCK = [
    "......abbc......",
    ".....aabkbc.....",
    "....abbbbcck....",
    "....abkbbccd....",
    "....bbbbckcd....",
    "....bcbcccdd....",
    "....cckccdde.bc.",
    ".....cdcdde.abkd",
    "......ddee..bcde",
    ".....fFFFf...ff.",
]
PUFFBALL_ITEM = [
    "................",
    "................",
    "...aabbbcc......",
    "..aabkbbbcc.....",
    ".aabbbbkbccd....",
    ".abkbbbbcckd....",
    ".abbbbkbcccd....",
    ".bbkbbbccckd....",
    ".bbbbcbcckdd....",
    ".cbcbcccdcde.bc.",
    "..cckccddde.abkd",
    "...cdcddde..bckd",
    "....dddee...cdde",
    "....fFFFf....fF.",
    "................",
    "................",
]

# ---------------- roasted mushrooms: mixed charred pieces on a stick ----------------
ROAST = {"h": "e8b25a", "b": "c8873a", "B": "a0662a", "x": "3b2414", "X": "6a4220",
         "y": "e2a636", "Y": "c07e1c", "p": "e0c48a", "P": "b8955a",
         "k": "493615", "K": "896727", "j": "281e0b"}
STICK = [  # vanilla stick.png, rows 2..14
    "............kj", "..........kKj.", ".........kKj..", "........kKj...", ".......kKj....",
    "......kKj.....", ".....kKj......", "....kKj.......", "...kKj........", "..kKj.........",
    ".kKj..........", "kKj...........", "jj............",
]
ROASTED_LAYERS = [  # pieces spaced so the stick shows between them
    (STICK, 2, 2, ROAST),
    ([".hp.", "hpXP", "pPPX", ".PP."], 11, 2, ROAST),                   # stem chunk
    (["y.y.y", "yhyXY", ".YyY.", "..Y.."], 7, 5, ROAST),                 # chanterelle piece
    ([".hbB.", "hbXbB", "pPPPX", ".PxP."], 3, 9, ROAST),                 # brown cap, gills below
]

TEXTURES = {
    "block/chanterelle": grid(CHANTERELLE_BLOCK, CHANT), "item/chanterelle": grid(CHANTERELLE_ITEM, CHANT),
    "block/porcini": grid(PORCINI_BLOCK, PORC), "item/porcini": grid(PORCINI_ITEM, PORC),
    "block/morel": grid(MOREL_BLOCK, MOREL), "item/morel": grid(MOREL_ITEM, MOREL),
    "block/oyster_mushroom": grid(OYSTER_BLOCK, OYST), "item/oyster_mushroom": grid(OYSTER_ITEM, OYST),
    "block/fly_agaric": grid(FLY_AGARIC_BLOCK, FLY), "item/fly_agaric": grid(FLY_AGARIC_ITEM, FLY),
    "block/puffball": grid(PUFFBALL_BLOCK, PUFF), "item/puffball": grid(PUFFBALL_ITEM, PUFF),
    "item/roasted_mushrooms": draw(ROASTED_LAYERS),
}


def preview(path, scale=12):
    """Each texture over a checker and over grass-green, plus a 1x strip."""
    names = list(TEXTURES)
    cell = 16 * scale + 8
    sheet = Image.new("RGBA", (cell * len(names), cell * 2 + 24), (40, 40, 40, 255))
    for i, n in enumerate(names):
        big = TEXTURES[n].resize((16 * scale,) * 2, Image.NEAREST)
        for row, bg in enumerate(("checker", (94, 140, 60, 255))):
            if bg == "checker":
                base = Image.new("RGBA", big.size)
                for y in range(16):
                    for x in range(16):
                        base.paste((150, 150, 150, 255) if (x + y) % 2 else (190, 190, 190, 255),
                                   (x * scale, y * scale, (x + 1) * scale, (y + 1) * scale))
            else:
                base = Image.new("RGBA", big.size, bg)
            base.alpha_composite(big)
            sheet.paste(base, (i * cell + 4, row * cell + 4))
        sheet.alpha_composite(TEXTURES[n], (i * cell + 4, cell * 2 + 4))
    sheet.save(path)


if __name__ == "__main__":
    for name, img in TEXTURES.items():
        out = TEX / f"{name}.png"
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out)
        print(out)
    if len(sys.argv) > 1:
        preview(sys.argv[1])
