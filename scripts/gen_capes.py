"""Capes of the elders, the Trader Caravan leaders and the Oak Keeper (client/people/TribeCapeLayer.java), one 128 x 128
sheet each in textures/entity/tribesperson/cape/<style>.png:
  (0,0) 28 x 44   the cape's outside, 2 px per model unit, with the emblem on the back; see-through pixels shape the hem
  (32,0) 28 x 44  its lining
  (64,0) (64,16) (64,32) (96,0) (96,16) (96,32), 32 x 16 each: the materials of the 3D shoulder pieces and collar
                  (PAD, TOP, TRIM, HARD, JEWEL, FRINGE; any box with 2(w+d) <= 32 and d+h <= 16 can wear one)
  (0,48) five 5 x 6 sprites: leaves / fur tufts / feathers laid over the cloth and standing off the shoulders
The tile and sprite places mirror the constants in client/people/TribeCapeModel.java: keep them in sync.
The elders' cloth is in the tribe's colour: <style>_tint.png marks it (client/people/TintedSkin), as for their skins.
Generator order: any time (imports gen_tribe_skins only for its colours). Run: python scripts/gen_capes.py [preview_dir]"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_tribe_skins as T  # noqa: E402

OUT = T.OUT / "cape"
H = T.hexc
W, HGT = 28, 44
PAD, TOP, TRIM, HARD, JEWEL, FRINGE = (64, 0), (64, 16), (64, 32), (96, 0), (96, 16), (96, 32)
SPRITE_Y = 48


def noise(x, y, salt):
    return T.h_(x * 1619 + y * 31337, salt)


class Sheet:
    def __init__(self):
        self.img = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
        self.mask = Image.new("RGBA", (128, 128), (0, 0, 0, 0))

    def put(self, x, y, c, f=1.0):
        """Pixel in colour c shaded by f; T.DYE also goes into the tint mask."""
        self.img.putpixel((x, y), T.clamp(T.shade(c, f)) + (255,))
        g = max(0, min(255, round(235 * f))) if isinstance(c, T.Dye) else None
        self.mask.putpixel((x, y), (g, g, g, 255) if g is not None else (0, 0, 0, 0))

    def clear(self, x, y):
        self.img.putpixel((x, y), (0, 0, 0, 0))
        self.mask.putpixel((x, y), (0, 0, 0, 0))

    def tile(self, at, paint, w=32, h=16):
        for y in range(h):
            for x in range(w):
                c = paint(x, y)
                if c is not None:
                    col, f = c
                    self.put(at[0] + x, at[1] + y, col, f)


# ---------------- materials: (x, y) -> (colour, shade) ----------------
def fur(col, salt=1):
    def p(x, y):
        lock = noise(x, y // 3 + (x % 2), salt) * 0.3 - 0.15
        tip = 1.14 if (y + x * 3 + int(noise(x, 0, salt) * 4)) % 4 == 0 else 0.97
        return col, (1 + lock) * tip
    return p


def cloth(col, salt=2):
    def p(x, y):
        return col, (1.05 if (x + y) % 2 else 0.95) * (1 + 0.08 * (noise(x, y, salt) - 0.5))
    return p


def braid(col, dark):
    """A twisted cord or braided galloon: diagonal strands."""
    def p(x, y):
        k = (x + y * 2) % 4
        return (dark, 1.0) if k == 3 else (col, [1.22, 1.0, 0.84][k])
    return p


def brocade(base, thread):
    """Embroidered cloth: a diamond lattice in the thread colour."""
    def p(x, y):
        if (x + y) % 6 == 0 or (x - y) % 6 == 0:
            return thread, 0.95 if (x % 2) else 1.1
        if (x % 6, y % 6) == (3, 0):
            return thread, 1.2
        return base, 1.04 if (x + y) % 2 else 0.95
    return p


def bone(col=T.BONE):
    def p(x, y):
        grain = 0.86 if noise(x, y // 4, 9) > 0.78 else 1.0
        return col, grain * (1.07 - 0.02 * (y % 8)) * (1 + 0.06 * (noise(x, y, 11) - 0.5))
    return p


def metal(col):
    def p(x, y):
        k = (x - y) % 6
        return col, [1.35, 1.15, 1.0, 0.82, 0.72, 0.92][k]
    return p


def jewel(col):
    def p(x, y):
        return col, [1.55, 1.05, 0.8, 1.0][(x % 2) + 2 * (y % 2)]
    return p


def bark(col):
    def p(x, y):
        ridge = noise(x, 0, 41)
        crack = noise(x, y // 3 + int(ridge * 3), 43) > 0.72
        return col, (0.62 if crack else 0.88 + ridge * 0.36) * (1 + 0.1 * (noise(x, y, 47) - 0.5))
    return p


def moss(col):
    def p(x, y):
        v = noise(x, y, 53)
        return col, 1.28 if v > 0.82 else 0.72 if v < 0.2 else 0.95 + 0.15 * noise(x // 2, y // 2, 59)
    return p


def feathers(cols):
    def p(x, y):
        col = cols[(x // 3 + (y // 5)) % len(cols)]
        rib = 1.2 if x % 3 == 1 else 0.9
        return col, rib * (0.72 if y % 5 == 4 else 1.0)
    return p


def fringe(strand, tip=None, head=None, every=2):
    """Hanging strands (alpha between them), a solid head row, beads or tassel tips at uneven lengths."""
    def p(x, y):
        if y < 1:
            return (head or strand), 1.1 if x % 2 else 0.9
        if x % every:
            return None
        end = 3 + int(noise(x, 0, 61) * 2.99)
        if y > end:
            return None
        if tip is not None and y >= end - 1:
            return tip, 1.1 if y == end - 1 else 0.85
        return strand, 1.0 - 0.05 * y + (0.1 if x % 4 == 0 else 0)
    return p


# ---------------- sprites (5 x 6): leaf, tuft, feather ----------------
SPRITES = {
    "oak": [".ada.", "aabaa", "daaad", "abaaa", ".aab.", "..a.."],
    "leaf": ["..a..", ".aab.", "aabaa", "aabaa", ".dbd.", "..b.."],
    "tuft": ["aaaaa", "abaaa", "aaaba", "daaad", ".a.a.", "...a."],
    "feather": [".aaa.", ".aba.", "aabaa", "dabad", ".aba.", "..c.."],
}


def sprite(s, n, shape, col, rib=None, salt=0):
    for y, row in enumerate(SPRITES[shape]):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            # the sprite hangs: row 0 is where it is fixed
            f = {"a": 1.0, "b": 1.25, "d": 0.76, "c": 1.0}[ch] * (1.08 - 0.045 * y) * (1 + 0.1 * (noise(x, y, salt + n) - 0.5))
            c = rib if ch in "bc" and rib is not None else col
            s.put(n * 5 + x, SPRITE_Y + y, c, f)


# ---------------- emblems: o outline, a main, b light, c accent; 16 wide ----------------
EMBLEMS = {
    "elder": [  # a stag's skull with great antlers under a sun disc
        "a....o....o....a",
        "aa...a.cc.a...aa",
        ".a..aa.cc.aa..a.",
        ".aa.a..cc..a.aa.",
        "..aaa......aaa..",
        "a..aa......aa..a",
        "aaaaaa....aaaaaa",
        "...baaa..aaab...",
        ".....aaaaaa.....",
        "....bbbbbbaa....",
        "....boobboaa....",
        "....boobboaa....",
        ".....bbbbaa.....",
        ".....bbbaaa.....",
        "......baaa......",
        "......boao......",
        "......baaa......",
        ".......ba.......",
    ],
    "plain": [  # a pair of trading scales over a coin
        ".......bb.......",
        "......baab......",
        ".aaaaaaaaaaaaaa.",
        ".b.....aa.....b.",
        "b.b....aa....b.b",
        "b.b....aa....b.b",
        "b..b...aa...b..b",
        "aaaaa..aa..aaaaa",
        ".aaa...aa...aaa.",
        ".......aa.......",
        ".....accccaa....",
        "....accbbccaa...",
        "....acbccbcaa...",
        "....acbccbcaa...",
        "....accbbccaa...",
        ".....aaccaaa....",
    ],
    "woodsman": [  # a fir over crossed roots
        ".......bb.......",
        "......baaa......",
        ".....baaaaa.....",
        "....baaaaaaa....",
        "......baaa......",
        ".....baaaaa.....",
        "....baaaaaaa....",
        "...baaaaaaaaa...",
        ".....baaaaa.....",
        "....baaaaaaa....",
        "...baaaaaaaaa...",
        "..baaaaaaaaaaa..",
        ".......cc.......",
        ".......cc.......",
        "......cccc......",
        "....cc.cc.cc....",
    ],
    "mining": [  # crossed picks over a lantern spark
        "..aaa......aaa..",
        ".abbaa....aabba.",
        "ab..caa..aac..ba",
        "a....cca.acc...a",
        "......ccacc.....",
        ".......ccc......",
        "......cccca.....",
        ".....ccc.ccc....",
        "....ccc...ccc...",
        "...ccc..b..ccc..",
        "..ccc..bab..ccc.",
        ".ccc..baaab..ccc",
        "cc.....bab....cc",
        "........b.......",
    ],
    "jewelry": [  # a cut gem in a gold setting, with rays
        ".......b........",
        "..b....b....b...",
        "...b.......b....",
        "....aaaaaaa.....",
        "...abbcccbba....",
        "..abbbccccbaa...",
        ".aaaaaaaaaaaaa..",
        ".acbbcbbbccbca..",
        "..acbcbbbccca...",
        "...acccbcccca...",
        "....accbccca....",
        ".....acbcca.....",
        "......acca......",
        ".......aa.......",
    ],
    "hunting": [  # a drawn bow, the arrow on its string pointing up
        ".......bb.......",
        "......bbbb......",
        ".....bbbbbb.....",
        ".......cc.......",
        "....aaaccaaa....",
        "..aaa..cc..aaa..",
        ".aa....cc....aa.",
        "aa.....cc.....aa",
        "ab.....cc.....ba",
        "b.b....cc....b.b",
        "....b..cc..b....",
        "......bccb......",
        ".......cc.......",
        ".....a.cc.a.....",
        ".....aaccaa.....",
        "......a..a......",
    ],
    "herbs": [  # a sprig of three pairs of leaves and a flower
        "......cccc......",
        ".....ccbbcc.....",
        "......cccc......",
        ".aa....aa....aa.",
        "abba...aa...abba",
        ".abba..aa..abba.",
        "..aaaa.aa.aaaa..",
        ".aa....aa....aa.",
        "abba...aa...abba",
        ".abbaa.aa.aabba.",
        "...aaaaaaaaaa...",
        ".......aa.......",
        "......aaaa......",
        ".....aa..aa.....",
    ],
    "foraging": [  # a basket heaped with berries and greens
        ".....a....a.....",
        "....aa.cc.aa....",
        ".....cccbcc.....",
        "...cbccccccbc...",
        "..ccccbcccccccc.",
        ".bbbbbbbbbbbbbb.",
        ".babababababaab.",
        "..aabababababa..",
        "..abababababab..",
        "...aabababaab...",
        "....bbbbbbbb....",
    ],
    "eastern": [  # an eight-petalled sun flower
        ".......aa.......",
        "..aa..abba..aa..",
        "..abaaabbaaaba..",
        "...abbaccabba...",
        "...aabccccbaa...",
        ".aaaaccbbccaaaa.",
        "abbbccbbbbccbbba",
        "abbbccbbbbccbbba",
        ".aaaaccbbccaaaa.",
        "...aabccccbaa...",
        "...abbaccabba...",
        "..abaaabbaaaba..",
        "..aa..abba..aa..",
        ".......aa.......",
    ],
    "oak": [  # the Ancient Oak: a round crown of leaves, a broad trunk and spreading roots
        ".....ab.ab......",
        "...abaabaaab....",
        "..aaabaaabaaab..",
        ".abaaaacaaaabaa.",
        "aaabacaaaacaaaab",
        "abaaaacacaaabaaa",
        ".aacaaacaaacaaa.",
        "..aaacacacaaaa..",
        "....aa.ccc.aa...",
        ".......ccc......",
        ".......ccc......",
        ".......ccc......",
        "......ccccc.....",
        "....ccc.c.ccc...",
        "...c...c.c...c..",
        "......b...b.....",
    ],
}


def emblem(s, name, cols, ring=None, cy=24):
    """The back emblem in the middle of the upper cape, outlined dark so it reads on any colour (dyes too)."""
    rows = EMBLEMS[name]
    x0, y0 = (W - 16) // 2, cy - len(rows) // 2
    marks = {(x0 + x, y0 + y): ch for y, row in enumerate(rows) for x, ch in enumerate(row) if ch != "."}
    dark = H("17100a")
    if ring is not None:  # a stitched ring round the emblem
        cx = 13.5
        for y in range(HGT):
            for x in range(W):
                d = math.hypot((x - cx) / 11.4, (y - cy) / 11.4)
                if 0.93 < d <= 1.07 and (x, y) not in marks:
                    s.put(x, y, ring, 1.1 if (x + y) % 3 else 0.8)
                elif 1.07 < d <= 1.18:
                    s.put(x, y, dark, 1.2)
    for (x, y) in marks:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            q = (x + dx, y + dy)
            if q not in marks and 0 <= q[0] < W:
                s.put(q[0], q[1], dark)
    for (x, y), ch in marks.items():
        col = cols["a" if ch == "o" else ch]
        if ch == "o":
            s.put(x, y, dark)
        else:
            s.put(x, y, col, 1.06 - 0.12 * (y - y0) / len(rows))


# ---------------- the cloth ----------------
def cape_cloth(s, base, hem_rows, border, material=None, stripes=None):
    """Outside and lining: cloth with painted fold light, deeper toward the hem, with side borders and a hem band.
    base: colour; border: (band colour, stitch colour); hem_rows(x, y) -> ("keep"|"cut"|(colour, shade))."""
    band, stitch = border
    for y in range(HGT):
        for x in range(W):
            fold = 1 + 0.10 * math.cos((x - 13.5) / 27.0 * math.pi * 6) * min(1.0, y / 20.0)
            f = fold * (1.07 - 0.20 * y / HGT) * (1 + 0.07 * (noise(x, y, 5) - 0.5)) * (1.03 if (x + y) % 2 else 0.97)
            col = base
            if material is not None:
                col, mf = material(x, y)
                f *= mf
            if stripes is not None:
                st = stripes(x, y)
                if st is not None:
                    col = st
            s.put(x, y, col, f)
            s.put(32 + x, y, base, 0.58 * (1 + 0.1 * (noise(x, y, 7) - 0.5)) * fold)
            # side borders
            edge = min(x, W - 1 - x)
            if edge < 3:
                if edge == 1:
                    s.put(x, y, stitch if y % 3 != 2 else band, (1.1 if y % 3 == 0 else 0.9) * (1.05 - 0.15 * y / HGT))
                else:
                    s.put(x, y, band, (0.72 if edge == 2 else 0.9) * (1.06 - 0.18 * y / HGT))
    for y in range(HGT - 10, HGT):
        for x in range(W):
            h = hem_rows(x, y)
            if h == "cut":
                s.clear(x, y)
                s.clear(32 + x, y)
            elif h != "keep":
                s.put(x, y, h[0], h[1])
                s.put(32 + x, y, h[0], h[1] * 0.6)


def band_hem(band, stitch, accent, tail, tip=None):
    """An embroidered hem band, then tassels (tail colour) with see-through gaps."""
    def p(x, y):
        r = y - (HGT - 10)
        if r == 0:
            return stitch, 1.15 if x % 2 else 0.85
        if r in (1, 5):
            return band, 0.7
        if r in (2, 3, 4):
            zig = abs((x % 6) - 3)  # a zigzag with a bead of the accent in every valley
            if zig == r - 2 or zig == r - 1:
                return stitch, 1.1
            if r == 2 and x % 6 == 0:
                return accent, 1.1
            return band, 1.0
        if r == 6:
            return stitch, 0.9 if x % 2 else 1.1
        length = 9 if x % 3 == 1 else 8
        if x % 3 == 2 or r > length:
            return "cut"
        if tip is not None and r == length:
            return tip, 1.0
        return tail, (1.12 if x % 3 == 0 else 0.86) * (1.0 - 0.05 * (r - 7))
    return p


def fur_hem(col, band, stitch):
    """A leather band stitched in bone, a deep fur edge cut ragged below."""
    def p(x, y):
        r = y - (HGT - 10)
        if r < 2:
            return (stitch if (x + r) % 4 == 0 else band), 1.0
        if r == 2:
            return band, 0.7
        if r >= 7 and (r - 7) >= int(noise(x, 3, 71) * 3.99) + (1 if x % 5 == 0 else 0):
            return "cut"
        c, f = fur(col, 13)(x, r)
        return c, f * (1.08 if r == 3 else 1.0)
    return p


def feather_hem(cols, band, stitch):
    def p(x, y):
        r = y - (HGT - 10)
        if r < 2:
            return (stitch if (x + r) % 4 == 0 else band), 1.0
        k = x % 4  # pointed feathers side by side
        length = [6, 9, 8, 5][k] if (x // 4) % 2 else [5, 8, 9, 6][k]
        if r > length:
            return "cut"
        col = cols[(x // 4) % len(cols)]
        return col, (1.25 if k == 1 else 0.82 if k in (0, 3) else 1.0) * (0.75 if r == 2 else 1.0)
    return p


def leaf_shade(x, r, k, drop, leaf):
    if r == drop:
        return 0.62  # the leaf's rim, in shadow
    if k == 2 and r in (1, 2):
        return 1.22  # a short midrib
    return (0.8 if k in (0, 4) else 1.0) * (0.9 + 0.2 * noise(leaf, 3, 83)) * (1.06 - 0.05 * r)


def leaf_hem(cols):
    """Rows of overlapping leaves, the lowest cut to their pointed tips."""
    def p(x, y):
        r = y - (HGT - 10)
        for row, off in ((2, 0), (1, 2), (0, 0)):  # the upper rows lie over the lower
            top = row * 3
            k = (x + off) % 5
            idx = (x + off) // 5
            drop = [2, 3, 4, 3, 2][k]
            if top <= r <= top + drop:
                col = cols[(idx * 3 + row) % len(cols)]
                return col, leaf_shade(x, r - top, k, drop, idx + row)
        return "cut" if r > 6 else "keep"
    return p


# ---------------- the styles ----------------
def finish(s, name):
    s.img.save(OUT / f"{name}.png")
    if s.mask.getbbox() is not None:
        s.mask.save(OUT / f"{name}_tint.png")
    else:
        (OUT / f"{name}_tint.png").unlink(missing_ok=True)


def elder(zone):
    """The elder: hide in the tribe's colour, a bone-white antlered skull under a sun, fur (or linen, or feathers) at the
    shoulders and hem, carved bone plates, antler tines and amber."""
    s = Sheet()
    pelt = {"temperate": H("8e7c66"), "cold": T.FUR_WHITE, "warm": H("c9b184"), "jungle": H("2f8a4a")}[zone]
    leather = H("4a2e1a")
    if zone == "jungle":
        hem = feather_hem([H("2f8a4a"), H("c43a2a"), H("e0b830"), H("2e5fa8")], leather, T.BONE)
    elif zone == "warm":
        hem = band_hem(leather, T.BONE, T.AMBER, H("d9cba8"), tip=T.BONE)
    else:
        hem = fur_hem(pelt, leather, T.BONE)
    cape_cloth(s, T.DYE, hem, (leather, T.BONE))
    # two rows of painted bone-white teeth marks below the shoulders and above the hem
    for x in range(3, W - 3):
        for y0, up in ((7, False),):
            k = abs((x % 4) - 2)
            for d in range(3 - k):
                s.put(x, y0 + (2 - d if up else d), H("e8dfc8"), 0.92 if d else 1.0)
    emblem(s, "elder", {"a": H("e8dfc8"), "b": H("fff8e6"), "c": T.AMBER}, ring=H("b8863a"), cy=25)  # a row lower: clear of the fur ruff
    if zone == "jungle":
        s.tile(PAD, feathers([H("2f8a4a"), H("3aa05a"), H("2e5fa8"), H("2f8a4a"), H("c43a2a"), H("e0b830")]))
    elif zone == "warm":
        s.tile(PAD, lambda x, y: (H("c9b184") if (x // 1 + y) % 2 else H("a89066"), 1.06 if y % 3 else 0.8))
    else:
        s.tile(PAD, fur(pelt, 3))
    s.tile(TOP, bone(H("ddd0b0")))
    s.tile(TRIM, braid(H("8a5a34"), leather))
    s.tile(HARD, bone(H("cdb88c")))  # antler
    s.tile(JEWEL, jewel(T.AMBER))
    s.tile(FRINGE, fringe(H("7a4e2c"), tip=T.BONE, head=leather))
    for n in range(5):
        if zone == "jungle":
            sprite(s, n, "feather", [H("2f8a4a"), H("c43a2a"), H("2e5fa8"), H("e0b830"), H("3aa05a")][n], H("c8c090"))
        elif zone == "warm":
            sprite(s, n, "tuft", H("d9cba8") if n % 2 else H("b89c6c"))
        else:
            sprite(s, n, "tuft", T.shade(pelt, 1.0 + 0.06 * (n - 2)))
    finish(s, f"elder_{zone}")


GOLD_L = H("e2b440")
# kind: cloth, lining/pad accent, trim thread, band, jewel, material of the cloth
TRADERS = {
    "plain": dict(base=T.DYE, thread=GOLD_L, band=H("4a2e1a"), gem=H("c0283a"), second=H("6a2a30")),
    "woodsman": dict(base=H("3e5a2e"), thread=H("d8a05a"), band=H("5a3a20"), gem=H("c8862a"), second=H("8a3226")),
    "mining": dict(base=H("45444a"), thread=H("d0853e"), band=H("2a2220"), gem=H("ffc24a"), second=H("6a3a22")),
    "jewelry": dict(base=H("2c3c8a"), thread=H("f0c448"), band=H("4a1e52"), gem=H("e0284a"), second=H("5a2a5e")),
    "hunting": dict(base=H("6e3c22"), thread=H("e8dfc8"), band=H("2e1e14"), gem=H("9c3420"), second=H("9a5532")),
    "herbs": dict(base=H("55753a"), thread=H("e0cc7a"), band=H("38461e"), gem=H("b080d0"), second=H("a8842e")),
    "foraging": dict(base=H("8a6a46"), thread=H("dcc694"), band=H("4a3622"), gem=H("a82a40"), second=H("6a7036")),
    "eastern": dict(base=H("9c1c2e"), thread=H("eeac28"), band=H("22285e"), gem=H("1e9a6a"), second=H("22285e")),
}


def trader(kind):
    """A caravan leader: rich travelling cloth in the caravan's colours, braid and embroidery, a tasselled hem, brass
    clasps and a gem; the kind's own sign on the back."""
    s = Sheet()
    c = TRADERS[kind]
    base, thread, band, gem, second = c["base"], c["thread"], c["band"], c["gem"], c["second"]
    material = stripes = None
    if kind == "foraging":  # patchwork
        pal = [H("8a6a46"), H("6a5a3a"), H("a08a5a"), H("5e4a32"), H("7a7046")]

        def material(x, y):
            col = pal[(x // 7 * 3 + (y + (x // 7) * 3) // 8 * 2) % len(pal)]
            seam = x % 7 == 0 or (y + (x // 7) * 3) % 8 == 0
            return col, 0.76 if seam else 1.0
    elif kind == "eastern":  # ikat stripes down both sides
        def stripes(x, y):
            sx = min(x, W - 1 - x) + (y // 2) % 2
            return {4: H("eeac28"), 5: H("22285e"), 6: H("22285e")}.get(sx)
    elif kind == "woodsman":
        def stripes(x, y):
            return H("8a3226") if y in (HGT - 14, HGT - 13) or y in (6, 7) else None
    elif kind == "mining":
        def material(x, y):
            return base, 0.82 if noise(x // 2, y // 2, 77) > 0.8 else 1.0  # soot
    tail = {"herbs": H("d0b865"), "foraging": H("b89a62"), "hunting": H("8a5a34"), "mining": H("a0602c")}.get(kind, thread)
    tip = {"hunting": T.TOOTH, "foraging": H("a82a40"), "herbs": H("8a5aa8"), "eastern": H("1e7a4c")}.get(kind)
    hem = band_hem(band, thread, gem, tail, tip)
    if kind == "hunting":
        hem = fur_hem(T.FUR_RED, band, thread)
    cape_cloth(s, base, hem, (band, thread), material, stripes)
    for x in range(3, W - 3):  # the embroidered yoke under the collar
        k = abs((x % 6) - 3)
        s.put(x, 5, thread, 1.0 if x % 2 else 0.85)
        s.put(x, 7 + k // 2, thread, 1.12 if k == 0 else 0.9)
        if x % 6 == 0:
            s.put(x, 10, gem, 1.1)
    for x in range(3, W - 3):  # a second embroidered line above the hem band
        if x % 4 != 3:
            s.put(x, HGT - 12, thread, 0.95 if x % 4 else 1.1)
    light = T.mix(thread, H("fff6d8"), 0.55)
    accent = {"plain": H("e2b440"), "woodsman": H("9a6a3a"), "mining": H("b0764a"), "jewelry": H("e0284a"),
              "hunting": H("c86a34"), "herbs": H("c8a0e0"), "foraging": H("c83250"), "eastern": H("eeac28")}[kind]
    main = {"woodsman": H("a8c878"), "mining": H("c4c8cc"), "eastern": H("eeac28"), "herbs": H("b8d878"),
            "foraging": H("d8b878")}.get(kind, thread)
    emblem(s, kind, {"a": main, "b": light, "c": accent}, ring=thread)
    if kind == "hunting":
        s.tile(PAD, fur(T.FUR_RED, 17))
    elif kind == "foraging":
        s.tile(PAD, lambda x, y: ([H("a08a5a"), H("6a5a3a"), H("8a6a46"), H("7a7046")][(x // 4 + y // 3 * 2) % 4],
                                  0.75 if x % 4 == 0 or y % 3 == 0 else 1.0))
    else:
        s.tile(PAD, brocade(second, thread))
    s.tile(TOP, cloth(base if not isinstance(base, T.Dye) else T.DYE))
    s.tile(TRIM, braid(thread, band))
    s.tile(HARD, metal({"mining": H("b0764a"), "hunting": H("d8c8a0"), "woodsman": H("a87a3c")}.get(kind, H("c89a36"))))
    s.tile(JEWEL, jewel(gem))
    s.tile(FRINGE, fringe(tail, tip=tip, head=thread))
    for n in range(5):
        if kind == "hunting":
            sprite(s, n, "tuft", T.shade(T.FUR_RED, 1.0 + 0.07 * (n - 2)))
        elif kind == "herbs":
            sprite(s, n, "leaf", [H("6a8a3a"), H("7aa04a"), H("8a9a4a"), H("5a7a3a"), H("94ac5a")][n], H("c8d890"))
        else:
            sprite(s, n, "tuft", tail)
    finish(s, f"trader_{kind}")


def oak():
    """The Oak Keeper: deep moss under a mantle of layered oak leaves, the Ancient Oak in gold on the back, bark
    shoulder pieces, moss and acorns."""
    s = Sheet()
    deep = H("2f4a1e")
    leaves = [H("4f7a2a"), H("5f8a34"), H("3f6422"), H("6e9238"), H("57782a")]
    cape_cloth(s, deep, leaf_hem(leaves + [H("8a7a2a")]), (H("3a2a18"), H("b0902e")), material=moss(deep))
    # shoulders: three rows of painted leaves under the 3D ones
    for row in (2, 1, 0):
        for x in range(W):
            k = (x + row * 3) % 5
            for r in range(row * 3, row * 3 + [2, 3, 4, 3, 2][k] + 1):
                leaf = (x + row * 3) // 5
                s.put(x, r, leaves[(leaf + row) % 5], leaf_shade(x, r - row * 3, k, [2, 3, 4, 3, 2][k], leaf + row))
    emblem(s, "oak", {"a": H("e0bc48"), "b": H("fff0a0"), "c": H("c08c2c")}, ring=H("a8862a"))
    s.tile(PAD, bark(H("5e4228")))
    s.tile(TOP, moss(H("567a2a")))
    s.tile(TRIM, braid(H("6a5a2a"), H("2a3a14")))  # twisted vine
    s.tile(HARD, lambda x, y: (H("7a5426"), (1.2 if (x + y) % 2 == 0 else 0.72)))  # acorn cup scales
    s.tile(JEWEL, lambda x, y: (H("c89a3c"), [1.3, 1.05, 0.85, 1.0][(x % 2) + 2 * (y % 2)]))  # the nut
    s.tile(FRINGE, fringe(H("7a9a4a"), tip=H("a9bd86"), head=H("3f6422")))
    for n, col in enumerate(leaves):
        sprite(s, n, "oak", col, T.mix(col, H("c8d070"), 0.2))
    finish(s, "oak_keeper")


ELDER_ZONES = T.ZONES


def build():
    OUT.mkdir(parents=True, exist_ok=True)
    for zone in ELDER_ZONES:
        elder(zone)
    for kind in ["plain"] + T.CARAVAN_KINDS:
        trader(kind)
    oak()
    for name, rows in EMBLEMS.items():
        assert all(len(r) == 16 for r in rows), name


if __name__ == "__main__":
    build()
    names = sorted(p for p in OUT.glob("*.png") if not p.stem.endswith("_tint"))
    print(f"wrote {len(names)} capes")
    if len(sys.argv) > 1:  # a contact sheet of the backs, the elders' dyed red
        sheet = Image.new("RGBA", (len(names) * 34 * 6, 50 * 6), (90, 110, 90, 255))
        for i, p in enumerate(names):
            img = Image.open(p).convert("RGBA")
            tint = p.with_name(p.stem + "_tint.png")
            if tint.exists():
                img = T.dyed(img.crop((0, 0, 64, 64)), Image.open(tint).convert("RGBA").crop((0, 0, 64, 64)), (0xb0, 0x2e, 0x26))
            sheet.alpha_composite(img.crop((0, 0, 28, 44)).resize((168, 264), Image.NEAREST), (i * 34 * 6 + 12, 12))
        sheet.save(Path(sys.argv[1]) / "capes_sheet.png")
