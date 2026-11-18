# Bark basket item texture: woven bark strips, open top, cordage handle. Colours from bark.png / cordage.png.
# usage: python scripts/gen_bark_basket_texture.py
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item/bark_basket.png"

# k/d/c/C cordage, O outline, z/g/G back rim, i/j inside, R/a/m front rim, w woven body (filled below)
GRID = [
    ".....kkkkkk.....",
    "....kCdCdCdk....",
    "...kdk....kCk...",
    "...Ck......dk...",
    "..kd........Ck..",
    "..Ck........dk..",
    ".OdzgGgGgGgzCkO.",
    ".OgiijiiiijiigO.",
    ".OmiiiijiiiiimO.",
    "OaRmRaRRmRaRRmaO",
    ".Owwwwwwwwwwwwo.",
    ".Owwwwwwwwwwwwo.",
    ".Owwwwwwwwwwwwo.",
    "..Owwwwwwwwwwo..",
    "..Owwwwwwwwwwo..",
    "...OOOOOOOOOO...",
]
PAL = {
    "k": "3d2b1e", "d": "5c4319", "c": "795632", "C": "b2977c",
    "O": "2e2419", "o": "3a2e20",
    "z": "443b2f", "g": "665844", "G": "87755b",
    "i": "241b12", "j": "33281b",
    "R": "be9d5f", "a": "ae8e52", "m": "987849",
}
STAKES = (3, 6, 9, 12)
WEAVER = [("987849", "745a36"), ("87755b", "665844")]  # (top row, bottom row) per band
NUDGE = {(7, 12), (11, 14)}  # hand-made: a couple of strips sit a pixel off

def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)

def weave(x, y):
    if y == 10: return "87755b" if x in STAKES else "4c3d26"  # shadow under the rim, stake tops peeking out
    band, top = divmod(y - 11, 2)[0], (y - 11) % 2 == 0
    if (x, y) in NUDGE: top = not top
    for si, sx in enumerate(STAKES):
        if x == sx:
            if (band + si) % 2 == 0:  # stake passes over the weaver: inner-bark highlight
                return "be9d5f" if top else "ae8e52"
            return "4c3d26"            # weaver over the stake, stake edge in shadow
    near = min(abs(x - sx) for sx in STAKES)
    hi, lo = WEAVER[band]
    c = hi if top else lo
    if near == 1 and not top: c = "4c3d26" if band != 1 else "443b2f"  # dip where it tucks under
    return c

def build():
    assert len(GRID) == 16 and all(len(r) == 16 for r in GRID)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(GRID):
        for x, ch in enumerate(row):
            if ch == ".": continue
            img.putpixel((x, y), hexc(weave(x, y) if ch == "w" else PAL[ch]))
    return img

if __name__ == "__main__":
    build().save(OUT)
    print(OUT)
