# Rough log textures (a whole chopped log section, lying diagonally) and bark strips,
# colours taken from each wood's vanilla log textures.
# usage: python gen_rough_log_textures.py <vanilla block texture dir> <out item texture dir> <preview png>
import math, os, random, sys
from PIL import Image
src, out, prev = sys.argv[1:4]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar", "aspen", "alder", "beech", "linden"]
# mod woods have no vanilla texture: read the log textures painted by gen_wood_textures.py
def tex(name): p = f"{src}/{name}.png"; return p if os.path.exists(p) else os.path.join(os.path.dirname(os.path.abspath(out)), "block", f"{name}.png")

def palette(img):
    px = sorted({img.getpixel((x, y))[:3] for x in range(img.width) for y in range(img.height)}, key=lambda c: sum(c))
    return px[len(px) // 6], px[len(px) // 2], px[5 * len(px) // 6]  # dark, mid, light

def shade(c, f): return tuple(max(0, min(255, int(v * f))) for v in c)

AX, AY = 4.4, 11.6          # centre of the cut end (lower-left)
DX, DY = 1 / math.sqrt(2), -1 / math.sqrt(2)  # log axis, running up-right
R, LEN, FACE = 3.9, 9.0, 0.8   # radius, length, end ellipse squash along the axis

def rough_log(wood, bark, end):
    dark, mid, light = bark
    edark, emid, elight = end
    rnd = random.Random(wood)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # bark furrows: (offset across the log, start, end along it)
    furrows = [(o, a, a + rnd.uniform(3, 7)) for o in (-2.2, -0.6, 1.0, 2.6) for a in [rnd.uniform(0, 4)]]
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5 - AX, y + 0.5 - AY
            t = px * DX + py * DY      # along the axis
            s = px * -DY + py * DX     # across: negative = upper-left (lit) side
            face = (t / (R * FACE)) ** 2 + (s / R) ** 2
            back = ((t - LEN) / (R * FACE)) ** 2 + (s / R) ** 2
            if face <= 1:              # cut end: rings in the plank colour, bark rim
                rho = math.sqrt(face)
                if rho > 0.8: c = dark
                elif rho < 0.2: c = edark
                elif 0.42 < rho < 0.6: c = shade(edark, 1.05)
                else: c = elight
                if t + 0.35 * s > 0.7 and c != dark: c = shade(c, 0.82)  # second chop facet
            elif (0 <= t <= LEN and abs(s) <= R) or back <= 1:  # bark body
                u = s / R
                c = light if u < -0.5 else dark if u > 0.45 else mid
                if any(abs(s - o) < 0.5 and a <= t <= b for o, a, b in furrows): c = shade(c, 0.75)
                elif rnd.random() < 0.1: c = shade(c, 1.12)
            else:
                continue
            img.putpixel((x, y), c + (255,))
    a = img.copy()
    # outline: darken pixels on the silhouette edge (vanilla item look), less on the lit side
    for y in range(16):
        for x in range(16):
            if not a.getpixel((x, y))[3]: continue
            open_ = [not (0 <= x + dx < 16 and 0 <= y + dy < 16) or not a.getpixel((x + dx, y + dy))[3]
                     for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1))]
            if open_[0] or open_[1]: img.putpixel((x, y), shade(a.getpixel((x, y))[:3], 0.68) + (255,))
            elif open_[2] or open_[3]: img.putpixel((x, y), shade(a.getpixel((x, y))[:3], 0.85) + (255,))
    # axe marks: chips bitten out of the rim and a dark chop line across the face
    for x, y in AXE_CHIPS: img.putpixel((x, y), (0, 0, 0, 0))
    for x, y in AXE_LINE: img.putpixel((x, y), shade(edark, 0.7) + (255,))
    for x, y in AXE_LIT: img.putpixel((x, y), shade(elight, 1.15) + (255,))
    return img

AXE_CHIPS = [(1, 9), (6, 13)]
AXE_LINE = [(2, 10), (3, 11), (4, 12)]   # chop ridge across the end grain
AXE_LIT = [(3, 10), (4, 11), (5, 12)]    # lit edge of the ridge

# o/m/l = bark dark/mid/light, i/j = inner side light/mid; upper case = grey-brown bark
BARK = """
................
......oMMO......
....oMLMMjO.....
...oMjiijiMO....
...Oi....jiO....
...i.....iO.....
..........ommo..
......oommllmo..
...oommlmmjiio..
.oommjjiii..io..
omjii.......ji..
oi..............
.i..OMMMMO......
...OMLMMMMMO....
..OMjiiijjMMO...
..ji.......jiO..
"""

def bark_strips():
    brown = palette(Image.open(f"{src}/oak_log.png").convert("RGBA"))
    grey = tuple(tuple(int(v * 0.55 + sum(c) / 3 * 0.45) for v in c) for c in brown)  # desaturated oak bark
    inner = palette(Image.open(f"{src}/stripped_oak_log.png").convert("RGBA"))
    cols = {"o": brown[0], "m": brown[1], "l": brown[2], "O": grey[0], "M": grey[1], "L": grey[2],
            "i": inner[2], "j": inner[1], "I": inner[2], "J": inner[1]}
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(BARK.strip().splitlines()):
        assert len(row) == 16, row
        for x, ch in enumerate(row):
            if ch != ".": img.putpixel((x, y), cols[ch] + (255,))
    return img

sheet = Image.new("RGBA", (16 * 4 * 6, 16 * 4 * 3), (200, 200, 200, 255))
for i, wood in enumerate(WOODS):
    bark = palette(Image.open(tex(f"{wood}_log")).convert("RGBA"))
    end = palette(Image.open(tex(f"{wood}_log_top")).convert("RGBA").crop((4, 4, 12, 12)).resize((16, 16)))
    img = rough_log(wood, bark, end)
    img.save(f"{out}/{wood}_rough_log.png")
    big = img.resize((64, 64), Image.NEAREST); sheet.paste(big, ((i % 6) * 64, (i // 6) * 64), big)
img = bark_strips()
img.save(f"{out}/bark.png")
big = img.resize((64, 64), Image.NEAREST); sheet.paste(big, (2 * 64, 2 * 64), big)
sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST).save(prev)
print("ok")
