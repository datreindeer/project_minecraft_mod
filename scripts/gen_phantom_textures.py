"""Generate the phantom hide chain and Glider Cloak textures (data: gen_phantom.py).

- items: raw / scraped phantom hide (recoloured from the mod's raw_hide / scraped_hide shapes), phantom leather (vanilla
  leather shape), glider frame (drawn: lashed rods and bones), glider cloak (vanilla elytra icon shape as a ribbed membrane)
- drying rack hide planes: phantom hide wet / dried / rotted (from the mod's drying_rack_hide_wet/dry layouts)
- equipment: wings layer (the elytra layout as a folded bat-wing membrane with bone struts and a scalloped hem) and the
  humanoid layer (membrane mantle: collar with a bone clasp, shoulder capes, back panel)
- the flayed phantom for the skinned carcass: grey undead flesh, the wing membranes torn to tatters

Palette from the vanilla phantom texture. Run: python scripts/gen_phantom_textures.py
"""
import math
import sys
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
_jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(_jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def ours(path):
    return Image.open(TEX / f"{path}.png").convert("RGBA")


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def h(x, y, salt):
    """Deterministic per-pixel hash 0..999."""
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3)) + (255,)


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def ramp(colors, t):
    """Colour at t (0..1) along a list of hex colours."""
    cs = [hexc(c) for c in colors]
    t = max(0.0, min(0.999, t)) * (len(cs) - 1)
    i = int(t)
    return lerp(cs[i], cs[i + 1], t - i)


# vanilla phantom colours, dark -> light
MEMBRANE = ["#1f253f", "#292f4a", "#374377", "#43518a", "#5061a4"]
BONE = ["#796e60", "#958c79", "#a99b87", "#c3b9a1", "#dcd9c0"]
FLESH = ["#624d43", "#7e627b", "#a68179", "#c09c85"]
SCRAPED = ["#4f5672", "#6e7594", "#8e93ad", "#aeb0c2", "#c9c8d3"]
PLEATHER = ["#1b2232", "#26324a", "#33445f", "#435877", "#5a7393"]  # phantom leather: dark slate with a blue sheen
ROTTEN = ["#2c2a17", "#45421f", "#5d5a2c", "#74703c", "#857b4e"]
CORD = ["#765636", "#a68870", "#b5977d"]
WOOD = ["#4a3826", "#6b5034", "#8a6a45"]
OUTLINE = hexc("#161a2c")


def recolour(img, colors, lo=None, hi=None, salt=0, noise=0.08):
    """Map each opaque pixel's brightness (stretched to the image's own range) onto a colour ramp, with a little noise."""
    px = img.load()
    W, H = img.size
    ls = [lum(px[x, y]) for y in range(H) for x in range(W) if px[x, y][3]]
    lo = min(ls) if lo is None else lo
    hi = max(ls) if hi is None else hi
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    for y in range(H):
        for x in range(W):
            if px[x, y][3]:
                t = (lum(px[x, y]) - lo) / max(1e-6, hi - lo) + noise * (h(x, y, salt) / 1000 - 0.5)
                out.putpixel((x, y), ramp(colors, t)[:3] + (px[x, y][3],))
    return out


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)
    print("wrote", rel)


def edge(px, x, y, W, H):
    """Opaque pixel next to a transparent one (or the border)."""
    return any(not (0 <= x + dx < W and 0 <= y + dy < H) or px[x + dx, y + dy][3] == 0
               for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


# ---------- items ----------

def raw_phantom_hide():
    src = ours("item/raw_hide")
    img = recolour(src, MEMBRANE[1:], salt=1)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if not px[x, y][3]:
                continue
            if edge(px, x, y, 16, 16):
                px[x, y] = OUTLINE
            elif (x + 2 * y) % 7 == 0 and h(x, y, 2) < 600:  # membrane veins
                px[x, y] = hexc(MEMBRANE[1])
            elif h(x, y, 3) < 90:  # pale flesh left on the underside
                px[x, y] = hexc(FLESH[1 + h(x, y, 4) % 2])
    return img


def scraped_phantom_hide():
    src = ours("item/scraped_hide")
    img = recolour(src, SCRAPED, salt=5)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and edge(px, x, y, 16, 16):
                px[x, y] = hexc(MEMBRANE[1])  # the dark outer skin shows at the rim
            elif px[x, y][3] and (x + 2 * y) % 7 == 0 and h(x, y, 6) < 500:
                px[x, y] = hexc(SCRAPED[1])
    return img


def phantom_leather():
    src = vanilla("item/leather")
    img = recolour(src, PLEATHER, salt=7, noise=0.05)
    px = img.load()
    for y in range(16):
        for x in range(16):
            # a faint oily sheen in diagonal bands
            if px[x, y][3] and not edge(px, x, y, 16, 16) and (x - y) % 6 == 0 and h(x, y, 8) < 700:
                px[x, y] = lerp(px[x, y], hexc("#7f9cc0"), 0.45)
    return img


def line(img, x0, y0, x1, y1, col):
    steps = max(abs(x1 - x0), abs(y1 - y0))
    pts = []
    for i in range(steps + 1):
        x = round(x0 + (x1 - x0) * i / steps)
        y = round(y0 + (y1 - y0) * i / steps)
        img.putpixel((x, y), col(x, y))
        pts.append((x, y))
    return pts


def glider_frame():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    wood = lambda x, y: ramp(WOOD, 0.35 + 0.4 * ((x + y) % 3 == 0))
    bone = lambda x, y: ramp(BONE, 0.55 + 0.3 * (h(x, y, 9) / 1000))
    # a wing skeleton laid diagonally like a tool: the Tool Rod spar, three bone ribs fanning off it to the hem
    line(img, 1, 14, 13, 2, wood)
    line(img, 2, 14, 14, 2, wood)
    ribs = [line(img, 5, 10, 8, 14, bone), line(img, 8, 7, 13, 12, bone), line(img, 11, 4, 15, 7, bone)]
    px = img.load()
    # dark outline below / right of every drawn pixel so the thin rods read on any background
    drawn = [(x, y) for y in range(16) for x in range(16) if px[x, y][3]]
    for x, y in drawn:
        for dx, dy in ((1, 0), (0, 1)):
            if 0 <= x + dx < 16 and 0 <= y + dy < 16 and not px[x + dx, y + dy][3]:
                px[x + dx, y + dy] = hexc("#2a2016")
    # cordage lashing where each rib meets the spar, a sinew line tying the rib tips together
    for x, y in ((5, 10), (6, 10), (8, 7), (9, 7), (11, 4), (12, 4)):
        px[x, y] = ramp(CORD, h(x, y, 10) / 1000)
    for x, y in ((9, 13), (10, 13), (11, 12), (12, 11), (13, 9), (14, 8)):
        if not px[x, y][3] or px[x, y] == hexc("#2a2016"):
            px[x, y] = hexc("#d6c087")
    return img


def glider_cloak_icon():
    src = vanilla("item/elytra")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    spx, px = src.load(), img.load()
    for y in range(16):
        for x in range(16):
            if not spx[x, y][3]:
                continue
            side = x if x < 8 else 15 - x  # 0 at the outer edge, 7 at the spine
            if y >= 12 and side in (2, 3) or y == 13 and side == 4:
                continue  # scalloped hem between the ribs
            t = 0.75 - (y - 2) / 14 * 0.55 + 0.1 * (h(x, y, 11) / 1000 - 0.5)
            c = ramp(MEMBRANE, t)
            if side in (1, 5) and y >= 4:  # bone ribs
                c = ramp(BONE, 0.45 + 0.35 * (y % 2))
            px[x, y] = c
    for y in range(16):
        for x in range(16):
            if px[x, y][3] and edge(px, x, y, 16, 16) and not (y == 2):
                px[x, y] = OUTLINE
    for x in range(4, 12):  # a cord across the top, bone clasp in the middle
        if px[x, 2][3]:
            px[x, 2] = ramp(CORD, 0.3 + 0.5 * (x % 2))
    px[7, 2] = px[8, 2] = hexc(BONE[4])
    return img


# ---------- drying rack hide planes ----------

def rack(src, colors, salt, extra=None):
    img = recolour(ours(f"block/{src}"), colors, salt=salt)
    if extra:
        px = img.load()
        for y in range(16):
            for x in range(16):
                if px[x, y][3]:
                    extra(px, x, y)
    return img


def wet_shine(px, x, y):
    if 4 <= x <= 12 and 4 <= y <= 11 and h(x, y, 12) < 70:
        px[x, y] = hexc("#8a9ad0")  # glistening wet membrane


def dry_sheen(px, x, y):
    if 4 <= x <= 12 and 4 <= y <= 11 and (x - y) % 6 == 0 and h(x, y, 13) < 650:
        px[x, y] = lerp(px[x, y], hexc("#7f9cc0"), 0.4)


def rot_spots(px, x, y):
    if 3 <= x <= 12 and 3 <= y <= 12:
        n = h(x // 2, y // 2, 14)
        if n < 180:
            px[x, y] = hexc("#1d1b0e")  # black rot
        elif n < 260:
            px[x, y] = hexc("#6f7d3a")  # mould


# ---------- equipment ----------

def wings():
    """Elytra layout (64x32), wing box at texOffs 22,0 (10x20x2): south face x 36..46 is the outer, visible side."""
    src = vanilla("entity/equipment/wings/elytra")
    spx = src.load()
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()
    rows = {}
    for y in range(2, 22):
        xs = [x for x in range(34, 48) if spx[x, y][3]]
        if xs:
            rows[y] = (xs[0], xs[-1])
    targets = (0.12, 0.5, 0.86)  # where the three bone ribs end at the hem
    for y, (x0, x1) in rows.items():
        k = (y - 2) / 19
        ribs = [0.08 + (t - 0.08) * k for t in targets]
        for x in range(x0, x1 + 1):
            f = (x - x0) / max(1, x1 - x0)
            d = min(abs(f - r) for r in ribs) * (x1 - x0 + 1)
            # scalloped hem: the membrane is cut back in an arc between the ribs over the last rows
            gaps = sorted(ribs + [0.0, 1.0])
            for a, b in zip(gaps, gaps[1:]):
                if a <= f <= b and b > a:
                    depth = 3.2 * math.sin(math.pi * (f - a) / (b - a))
                    if 21 - y < depth and d > 0.6:
                        break
            else:
                if d < 0.6:
                    c = ramp(BONE, 0.35 + 0.45 * (h(x, y, 15) / 1000))
                else:
                    t = 0.15 + 0.6 * k + 0.12 * (h(x // 2, y, 16) / 1000 - 0.5)
                    c = ramp(MEMBRANE, t)
                    if (x + y) % 5 == 0 and h(x, y, 17) < 450:
                        c = lerp(c, hexc(MEMBRANE[0]), 0.5)  # faint veins
                px[x, y] = c
    # dark rim along the hem and the outer edge
    for y in range(2, 22):
        for x in range(34, 48):
            if px[x, y][3] and edge(px, x, y, 64, 32) and lum(px[x, y]) < 0.35:
                px[x, y] = lerp(px[x, y], OUTLINE, 0.6)
    # top / bottom faces and the thin outer edge strip: dark membrane fold
    for y in range(32):
        for x in range(64):
            if spx[x, y][3] and not (34 <= x < 48 and 2 <= y < 22):
                px[x, y] = ramp(MEMBRANE, 0.1 + 0.15 * (h(x, y, 18) / 1000))
    return img


def humanoid():
    """Chest armour layer (64x32): body box at 16,16 (8x12x4), arm box at 40,16 (4x12x4). A mantle, not a coat."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = img.load()

    def mem(x, y, t):
        px[x, y] = ramp(MEMBRANE, t + 0.12 * (h(x, y, 19) / 1000 - 0.5))

    # body top: the membrane over the shoulders
    for x in range(20, 28):
        for y in range(16, 20):
            mem(x, y, 0.25)
    # collar: front rows 20..22 with a ragged lower edge, sides 20..23
    for x in range(20, 28):
        for y in range(20, 23 + (h(x, 0, 20) < 500)):
            mem(x, y, 0.4 - 0.08 * (y - 20))
    for x in list(range(16, 20)) + list(range(28, 32)):
        for y in range(20, 24 + (h(x, 1, 21) < 400)):
            mem(x, y, 0.3)
    # back panel down to the waist (the folded wings hang over it), ribbed with two bone struts
    for x in range(32, 40):
        bottom = 30 - (2 if x in (32, 35, 36, 39) else 0)
        for y in range(20, bottom):
            mem(x, y, 0.2 + 0.5 * (y - 20) / 10)
        if x in (33, 38):
            for y in range(20, bottom):
                px[x, y] = ramp(BONE, 0.4 + 0.3 * (y % 2))
    # bone clasp at the throat, cord running to it
    for x in (22, 25):
        px[x, 22] = ramp(CORD, 0.6)
    px[23, 22] = px[24, 22] = hexc(BONE[4])
    px[23, 23] = px[24, 23] = hexc(BONE[2])
    # arms: shoulder capes, rows 20..23 and a ragged row 24
    for x in range(44, 48):
        for y in range(16, 20):
            mem(x, y, 0.25)
    for x in range(40, 56):
        for y in range(20, 24 + (h(x, 2, 22) < 500)):
            mem(x, y, 0.3 + 0.1 * (y - 20))
    for x in (45, 46):  # a bone stud on each shoulder
        px[x, 21] = hexc(BONE[3])
    # dark seam along every lower edge
    for y in range(17, 32):
        for x in range(16, 56):
            if px[x, y][3] and (y + 1 >= 32 or px[x, y + 1][3] == 0):
                px[x, y] = lerp(px[x, y], OUTLINE, 0.55)
    return img


# ---------- flayed phantom ----------

UNDEAD = ["#2f3329", "#464b3c", "#5f6450", "#7a7c64", "#93927a"]


def skinned_phantom():
    src = vanilla("entity/phantom/phantom")
    spx = src.load()
    img = src.copy()
    px = img.load()
    for y in range(64):
        for x in range(64):
            r, g, b, a = spx[x, y]
            if not a or (r, g, b) == (0, 0, 0):
                continue
            t = lum((r, g, b))
            wing_tip = 16 <= x < 62 and 24 <= y < 35
            if g > 150 and r < 50:  # eyes: dull and clouded
                px[x, y] = hexc("#5b5c50")
            elif b > r + 12:  # blue hide -> grey undead flesh, or torn membrane on the wing tips
                if wing_tip and h(x, y, 23) < 330 + 12 * abs(x - 39):
                    px[x, y] = (0, 0, 0, 0)
                elif wing_tip:
                    px[x, y] = ramp(UNDEAD, 0.55 + 0.3 * (h(x, y, 24) / 1000))
                else:
                    c = ramp(UNDEAD, t * 2.2 + 0.15 * (h(x // 3, y, 25) / 1000 - 0.5))
                    if h(x, y, 26) < 60:
                        c = hexc(FLESH[0])  # dark rotten streak
                    px[x, y] = c
            elif r > b + 20 and r - g < 60 and t < 0.6:  # the pink underside: dried, darker meat
                px[x, y] = lerp((r, g, b), hexc("#4a3a33"), 0.45)
            else:  # bone stays, a little paler
                px[x, y] = lerp((r, g, b), hexc(BONE[4]), 0.2)
    return img


if __name__ == "__main__":
    save(raw_phantom_hide(), "item/raw_phantom_hide")
    save(scraped_phantom_hide(), "item/scraped_phantom_hide")
    save(phantom_leather(), "item/phantom_leather")
    save(glider_frame(), "item/glider_frame")
    save(glider_cloak_icon(), "item/glider_cloak")
    save(rack("drying_rack_hide_wet", MEMBRANE, 27, wet_shine), "block/drying_rack_phantom_wet")
    save(rack("drying_rack_hide_dry", PLEATHER, 28, dry_sheen), "block/drying_rack_phantom_dry")
    save(rack("drying_rack_hide_wet", ROTTEN, 29, rot_spots), "block/drying_rack_phantom_rotten")
    save(wings(), "entity/equipment/wings/glider_cloak")
    save(humanoid(), "entity/equipment/humanoid/glider_cloak")
    save(skinned_phantom(), "entity/skinned/phantom/phantom")
