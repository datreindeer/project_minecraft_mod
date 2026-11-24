# Herb & vegetable textures (agent A of the herbs feature): wild vegetable plants (cross), the 22 vegetable/spice crops
# (stage0..3: sprout, young, grown, ripe), the 12 wild herbs (full + cut back), and the produce / eastern-caravan item icons.
# Plants are built from small parametrised painters (shaded leaves along a curved axis, stems, umbels, globes, spikes),
# placed from fixed seeds so every stage keeps the same plants in the same spots. Items are shaded masks (light from the
# upper left) with a dark outline edge like the mod's other produce. Deterministic; colours baked in like vanilla crops.
# usage: python scripts/gen_herbs_textures.py
import math
import random
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


# ============================== canvas helpers ==============================
def hexc(s): return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def new(): return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, c):
    x, y = int(round(x)), int(round(y))
    if 0 <= x < 16 and 0 <= y < 16 and c:
        img.putpixel((x, y), hexc(c) if isinstance(c, str) else c)


def get(img, x, y):
    return img.getpixel((x, y))[3] > 0 if 0 <= x < 16 and 0 <= y < 16 else False


def line(img, x0, y0, x1, y1, c):
    """Bresenham line; c is a colour or f(i, n) -> colour (i = 0 at the start)."""
    x0, y0, x1, y1 = (int(round(v)) for v in (x0, y0, x1, y1))
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    n = max(dx, -dy) + 1
    err, i = dx + dy, 0
    while True:
        put(img, x0, y0, c(i, n) if callable(c) else c)
        if x0 == x1 and y0 == y1:
            return
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
        i += 1


def poly(img, pts, c):
    for (a, b) in zip(pts, pts[1:]):
        line(img, a[0], a[1], b[0], b[1], c)


def ramp(cols):
    """Stem colouring along a line: dark at the start (ground), light at the end."""
    return lambda i, n: cols[min(len(cols) - 1, i * len(cols) // max(n, 1))]


def bez(p0, p1, p2, s):
    return ((1 - s) ** 2 * p0[0] + 2 * (1 - s) * s * p1[0] + s * s * p2[0],
            (1 - s) ** 2 * p0[1] + 2 * (1 - s) * s * p1[1] + s * s * p2[1])


def curve(img, base, tip, bend, c, steps=24):
    """A bent stem from base to tip (bend = sideways offset of the middle, + is to the right of the direction)."""
    (bx, by), (tx, ty) = base, tip
    L = math.hypot(tx - bx, ty - by) or 1
    nx, ny = -(ty - by) / L, (tx - bx) / L
    ctrl = ((bx + tx) / 2 + nx * bend, (by + ty) / 2 + ny * bend)
    pts, last = [], None
    for k in range(steps + 1):
        p = bez(base, ctrl, tip, k / steps)
        q = (int(round(p[0])), int(round(p[1])))
        if q != last:
            pts.append(q)
            last = q
    for i, q in enumerate(pts):
        put(img, q[0], q[1], c(i, len(pts)) if callable(c) else c)
    return pts


# ---- greens (D, d, m, l, h): darkest .. highlight
G_FRESH = ("24561c", "34762a", "4c9634", "6cb444", "96d066")
G_GLOSS = ("173a1c", "21522a", "2f6c34", "468a44", "78b466")
G_BLUE = ("27443f", "3a5f58", "537f74", "729e90", "a0c4b4")
G_GREY = ("3c4838", "52624a", "6c7e62", "8ca080", "b4c4a8")
G_ONION = ("34503c", "4a6c50", "62886a", "82a888", "acc8ae")  # wild onion: brighter grey-green tubes
G_BEAN = ("2e4a34", "466a4a", "5e8a60", "7caa78", "a6c8a0")   # broad bean: glaucous
G_LETT = ("34482e", "4c6440", "668258", "84a272", "aec89a")   # prickly lettuce: dull blue-green
G_YEL = ("3a5a1c", "52781e", "6c982a", "8eb83c", "b8d866")
G_WILD = ("3e5a24", "4c6c28", "6e9438", "94b450", "b4cc78")   # the mod's wild carrot greens
G_OLD = ("5a5422", "7a7230", "9c9440", "bab45a", "d6cc82")    # yellowing old leaves
STEM_G = ("34601e", "46782a", "5c9036")
STEM_W = ("4c3a26", "64503a", "7c6648")                       # woody
STEM_R = ("5a2a24", "7a3c30", "8e5a3a")                       # reddish


def shift(pal, k):
    """Darken (k<0) / lighten a 5-ramp by k steps, clamping at the ends."""
    return tuple(pal[max(0, min(4, i + k))] for i in range(5))


def leaf(img, base, tip, w, pal, bend=0.0, shape="lance", rib=False, dim=0):
    """A shaded leaf along a bent axis from base to tip, max width w px. Light from the upper left: the lit half is light,
    the other half mid, the shadow edge dark; the base darker, the tip lighter."""
    pal = shift(pal, dim)
    (bx, by), (tx, ty) = base, tip
    L = math.hypot(tx - bx, ty - by) or 1
    nx, ny = -(ty - by) / L, (tx - bx) / L
    ctrl = ((bx + tx) / 2 + nx * bend, (by + ty) / 2 + ny * bend)
    samples = [bez(base, ctrl, tip, k / 40) for k in range(41)]
    prof = {
        "lance": lambda s: math.sin(math.pi * s ** 0.8),
        "oval": lambda s: math.sin(math.pi * s) ** 0.6,
        "strap": lambda s: min(1.0, s * 5 + 0.4, (1 - s) * 2.5 + 0.15),
        "arrow": lambda s: min(1.0, (1 - s) ** 0.7 * 1.15, 0.5 + s * 6),
        "round": lambda s: math.sin(math.pi * s) ** 0.45,
        "spoon": lambda s: math.sin(math.pi * s ** 1.6) ** 0.7,
    }[shape]
    for y in range(16):
        for x in range(16):
            best, bi = 1e9, 0
            for i, (sx, sy) in enumerate(samples):
                d2 = (x - sx) ** 2 + (y - sy) ** 2
                if d2 < best:
                    best, bi = d2, i
            s = bi / 40
            if bi in (0, 40) and best > 0.5:
                continue
            i0, i1 = max(0, bi - 1), min(40, bi + 1)
            txx, tyy = samples[i1][0] - samples[i0][0], samples[i1][1] - samples[i0][1]
            tl = math.hypot(txx, tyy) or 1
            d = ((x - samples[bi][0]) * -tyy + (y - samples[bi][1]) * txx) / tl  # signed distance across
            hw = w * prof(s) / 2
            if abs(d) > hw + 0.3 and not (abs(d) < 0.5 and s < 0.97):
                continue
            # which side faces the upper-left light: the normal (-tyy, txx) * sign(d) dotted with (-1, -1)
            lit = ((-tyy) * (1 if d >= 0 else -1) * -1 + txx * (1 if d >= 0 else -1) * -1) > 0
            k = 3 if lit else 2
            if abs(d) < 0.5:
                k = 4 if rib else (3 if hw < 1.2 else 2 + (lit or tyy < 0))
                if rib and hw < 1:
                    k = 3
            elif not lit and abs(d) > hw - 0.7 and hw > 1.0:
                k = 1
            if s < 0.12:
                k -= 1
            elif s > 0.86 and lit:
                k += 1
            put(img, x, y, pal[max(0, min(4, k))])


def blade(img, x, h, drift, pal, y=15, dim=0):
    """A grass-like 1-px blade growing up from (x, y), drifting sideways by drift over its height."""
    pal = shift(pal, dim)
    for i in range(h):
        t = i / max(h - 1, 1)
        put(img, x + drift * t * t, y - i, pal[1] if t < 0.25 else pal[2] if t < 0.6 else pal[3])


def tube(img, x, h, drift, pal, y=15, dim=0):
    """Hollow onion leaf: a 1-px tube with a highlight pixel on its upper-left side here and there."""
    pal = shift(pal, dim)
    for i in range(h):
        t = i / max(h - 1, 1)
        xx = x + drift * t * t
        put(img, xx, y - i, pal[1] if t < 0.2 else pal[2] if t < 0.7 else pal[3])
        if i % 3 == 1 and 0.2 < t < 0.9:
            put(img, xx - 1 if drift >= 0 else xx + 1, y - i, pal[3] if drift >= 0 else pal[2])


# ---- flowers
def globe(img, cx, cy, r, cols, rng, spiky=True):
    """A round flower head (allium umbel, mint whorl): shaded disc with ragged petal tips. cols = (dark, mid, light, hi)."""
    for y in range(int(cy - r - 1), int(cy + r + 2)):
        for x in range(int(cx - r - 1), int(cx + r + 2)):
            d = math.hypot(x - cx, y - cy)
            if d <= r + 0.25:
                lt = (cx - x) + (cy - y)
                k = 2 if lt > 0.6 else 1 if lt > -0.8 else 0
                if d > r - 0.6 and rng.random() < 0.35:
                    k = max(0, k - 1)
                if lt > 1.2 and rng.random() < 0.5:
                    k = 3
                put(img, x, y, cols[k])
            elif spiky and d <= r + 1.1 and rng.random() < 0.3:
                put(img, x, y, cols[1 if (x + y) % 2 else 2])


def starball(img, cx, cy, cols, rng):
    """Ramsons: a loose ball of white six-pointed stars on thin pedicels."""
    for (dx, dy) in ((0, -2), (-2, -1), (2, -1), (-1, 0), (1, 0), (0, 1), (-2, 1), (2, 1), (-1, -1), (1, -1), (0, -1)):
        put(img, cx + dx, cy + dy, cols[3] if (dx < 0 or dy < 0) and rng.random() < 0.7 else cols[2])
    for (dx, dy) in ((-3, 0), (3, 0), (0, -3), (-1, 2), (1, 2)):
        if rng.random() < 0.6:
            put(img, cx + dx, cy + dy, cols[1])
    put(img, cx, cy, cols[0])


def flat_umbel(img, cx, y, w, cols, rng, rays=True, stem=None):
    """Umbellifer head: a flat-topped plate of florets (row y, w wide), dome row above, thin rays down to the stalk."""
    half = w // 2
    for dx in range(-half, half + 1):
        put(img, cx + dx, y, cols[2] if abs(dx) < half else cols[1])
        if rng.random() < 0.3:
            put(img, cx + dx, y, cols[0])
    for dx in range(-half + 1, half):
        put(img, cx + dx, y - 1, cols[3] if dx <= 0 else cols[2])
    if rays and w >= 5:
        put(img, cx - 1, y + 1, stem or STEM_G[1])
        put(img, cx + 1, y + 1, stem or STEM_G[1])
    put(img, cx, y + 1, stem or STEM_G[1])


def spike(img, x, ytop, n, cols, rng, stem=None, pods=0, podc=None):
    """A raceme: little four-petalled flowers alternating up a stalk, buds at the tip, optional seed pods below."""
    stem = stem or STEM_G[2]
    for i in range(n):
        y = ytop + i
        put(img, x, y, stem if i > 0 else cols[1])
        if i < n - pods:
            side = -1 if (i + x) % 2 else 1
            put(img, x + side, y, cols[2] if side < 0 else cols[1])
            if i % 2 == 0 and i > 0:
                put(img, x - side, y, cols[1])
            if i == 1:
                put(img, x, y - 1, cols[2])
        else:
            side = -1 if i % 2 else 1
            put(img, x + side, y - 1, podc or cols[0])
            put(img, x + side, y, podc or cols[0])


def daisy(img, cx, cy, white=("f8f8f0", "d8dacc"), eye=("f2c832", "c89a1c")):
    put(img, cx, cy, eye[0])
    put(img, cx - 1, cy, white[0])
    put(img, cx + 1, cy, white[1])
    put(img, cx, cy - 1, white[0])
    put(img, cx, cy + 1, white[1])


def rand(seed): return random.Random(seed)


def grid(rows, pal, img=None):
    """Hand-drawn 16x16 grid; '.' is transparent."""
    img = img or new()
    assert len(rows) == 16 and all(len(row) == 16 for row in rows), [len(row) for row in rows]
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                put(img, x, y, pal[ch])
    return img


def raceme(img, x, ytop, n, rng, lean=1, podc="8ea838"):
    """A loose branching mustard raceme: an open cluster of yellow florets at the tip, a short side branch with its
    own florets, and slim seed pods angled up and out along the lower part."""
    for i in range(n):
        put(img, x, ytop + i, STEM_G[2])
    for dx, dy, k in ((0, 0, 2), (-1, 1, 3), (1, 0, 1), (1, 2, 1), (-2, 2, 2), (0, -1, 3), (-1, -1, 2)):
        if rng.random() < 0.85 or dy == 0:
            put(img, x + dx, ytop + dy, YEL[k])
    by = ytop + 3   # the side branch
    put(img, x + lean, by - 1, STEM_G[2])
    put(img, x + 2 * lean, by - 2, YEL[2]); put(img, x + 3 * lean, by - 2, YEL[1]); put(img, x + 2 * lean, by - 3, YEL[3])
    for i in range(5, n, 2):   # ascending siliques
        side = -lean if (i // 2) % 2 else lean
        put(img, x + side, ytop + i - 1, podc)
        put(img, x + 2 * side, ytop + i - 3, podc)
        put(img, x + 2 * side, ytop + i - 2, "a8bc50")


# ============================== shared plant parts ==============================
def sprouts(img, kind, xs, pal=G_FRESH):
    """Stage 0: seedlings at the soil line."""
    for i, x in enumerate(xs):
        if kind == "blade":           # allium / grass: a single pale spike, some with a bent tip
            put(img, x, 15, pal[1]); put(img, x, 14, pal[2]); put(img, x, 13, pal[3])
            if i % 2:
                put(img, x + 1, 12, pal[3])
        elif kind == "grass":         # two thin blades
            put(img, x, 15, pal[1]); put(img, x, 14, pal[2]); put(img, x - 1, 13, pal[3]); put(img, x + 1, 14, pal[3])
        elif kind == "coty":          # brassica seed leaves: two round leaves on a short stalk
            put(img, x, 15, STEM_G[1]); put(img, x, 14, STEM_G[2])
            put(img, x - 1, 13, pal[3]); put(img, x - 2, 13, pal[2]); put(img, x + 1, 13, pal[2]); put(img, x + 2, 13, pal[1])
            put(img, x - 1, 12, pal[4])
        elif kind == "umbel":         # two strap cotyledons and a first divided leaf
            put(img, x, 15, STEM_G[1]); put(img, x - 1, 14, pal[3]); put(img, x + 1, 14, pal[2])
            put(img, x, 14, STEM_G[2]); put(img, x, 13, pal[3]); put(img, x + 1, 12, pal[3]); put(img, x - 1, 12, pal[2])
        elif kind == "bean":          # thick hooked shoot opening two leaves
            put(img, x, 15, STEM_G[0]); put(img, x, 14, STEM_G[1]); put(img, x, 13, STEM_G[2])
            put(img, x - 1, 12, pal[3]); put(img, x - 2, 12, pal[2]); put(img, x + 1, 12, pal[2]); put(img, x + 1, 11, pal[3])
            put(img, x - 1, 11, pal[4])
        elif kind == "broad":         # two broad oval first leaves (ramsons, lettuce)
            put(img, x, 15, pal[1]); put(img, x - 1, 14, pal[2]); put(img, x - 1, 13, pal[3]); put(img, x + 1, 14, pal[2])
            put(img, x + 1, 13, pal[3]); put(img, x - 2, 13, pal[3]); put(img, x + 2, 12, pal[2])


def pinnate(img, x0, h, lean, pal, rng, leaflet=2, dim=0, rachis=None):
    """A divided leaf (parsnip, celery, carrot family): a midrib from the ground with paired leaflets."""
    pal = shift(pal, dim)
    rach = rachis or pal[1]
    top = None
    for t in range(h):
        x, y = x0 + lean * t * t / max(h, 1), 15 - t
        put(img, x, y, rach if t < h - 1 else pal[3])
        top = (x, y)
        if t >= 2 and (t - h) % 2 == 0 or t == h - 1:
            for side in (-1, 1):
                for k in range(1, leaflet + 1):
                    c = pal[3] if side < 0 else pal[2]
                    if k == leaflet:
                        c = pal[4] if side < 0 else pal[1]
                    put(img, x + side * k, y - (k - 1) * (0.5 if t > h * 0.5 else 0), c)
    return top


def feather(img, x0, h, lean, pal, rng, dim=0):
    """Dill / fennel: thread-fine forking fronds, mostly air."""
    pal = shift(pal, dim)
    x = x0
    for t in range(h):
        y = 15 - t
        x = x0 + lean * t * t / max(h, 1)
        put(img, x, y, pal[1] if t < 3 else pal[2])
        if t >= 3 and t % 2 == 1:
            side = 1 if (t // 2 + x0) % 2 else -1
            put(img, x + side, y - 1, pal[3])
            put(img, x + 2 * side, y - 2, pal[3] if rng.random() < 0.7 else pal[4])
            if rng.random() < 0.5:
                put(img, x + 2 * side, y - 1, pal[2])
    put(img, x, 15 - h, pal[4])
    return (x, 15 - h + 1)


def lobed(img, base, tip, w, pal, rng, dim=0, bend=0.0):
    """A deeply lobed leaf (turnip, wild lettuce): a leaf with bites taken out along its edge."""
    leaf(img, base, tip, w, pal, bend=bend, shape="spoon", rib=True, dim=dim)
    (bx, by), (tx, ty) = base, tip
    for f in (0.35, 0.6):
        x, y = bx + (tx - bx) * f, by + (ty - by) * f
        nx, ny = -(ty - by), (tx - bx)
        L = math.hypot(nx, ny) or 1
        side = 1 if rng.random() < 0.5 else -1
        img.putpixel((min(15, max(0, int(round(x + nx / L * side * (w / 2))))),
                      min(15, max(0, int(round(y + ny / L * side * (w / 2)))))), (0, 0, 0, 0))


def stubs(img, xs, rng, cols=("5c6a3a", "7a8a4c", "c8cc96"), hmax=4):
    """Cut back with a knife: short stems with pale cut ends."""
    out = []
    for x in xs:
        h = rng.randint(2, hmax)
        lean = rng.choice((-1, 0, 0, 1))
        for i in range(h):
            put(img, x + (lean if i == h - 1 and h > 2 else 0), 15 - i, cols[0] if i < h - 1 else cols[1])
        put(img, x + (lean if h > 2 else 0), 15 - h, cols[2])
        out.append((x, h))
    return out


# ============================== wild & cultivated vegetables ==============================
WHITE = ("b8b8a8", "dcdccc", "f4f4ea", "ffffff")
PINKP = ("7a3a72", "a85696", "cc7cbc", "e8a8dc")
VIOLET = ("6c5a9a", "9484c4", "b8aee0", "dcd6f4")
YEL = ("b08a10", "e0bc1c", "f6dc3a", "fff08a")
PALEY = ("b0a040", "dcd060", "f0e88c", "fcf6c0")
CREAMW = ("9aa070", "c4c89a", "e2e4c4", "f6f6e4")
LILAC = ("8a6aa0", "b496c8", "d0bce0", "ece0f4")


def garlic(stage, wild=False):
    img = new()
    r = rand(("wild_garlic" if wild else "garlic"))
    if wild:   # ramsons: broad glossy leaves from the ground, a white starry ball on a stalk
        xs = (3, 8, 13)
        if stage == 0:
            sprouts(img, "broad", xs, G_GLOSS)
            return img
        h = (0, 6, 9, 10)[stage]
        for i, x in enumerate(xs):
            lean = (-3, 1, 3)[i]
            leaf(img, (x, 15), (x + lean, 15 - h + r.randint(0, 2)), 3, G_GLOSS, bend=lean * 0.6, rib=True, dim=-1)
            if stage >= 2:
                leaf(img, (x, 15), (x - lean // 2 + r.choice((-1, 1)), 16 - h), 3, G_GLOSS, bend=-lean * 0.4, rib=True)
        if stage == 3:
            for x, top in ((6, 3), (11, 5)):
                line(img, x, 15, x, top + 2, ramp(STEM_G))
                starball(img, x, top, WHITE, r)
        return img
    xs = (3, 8, 12)
    if stage == 0:
        sprouts(img, "blade", xs, G_BLUE)
        return img
    h = (0, 5, 9, 10)[stage]
    for i, x in enumerate(xs):   # flat keeled leaves arching out of a stiff neck, alternate sides
        hh = h - r.randint(0, 1)
        line(img, x, 15, x, 15 - hh // 2, ramp(G_BLUE[1:4]))
        for j, (dx, f) in enumerate(((-3, 1.0), (3, 0.9), (-2, 0.7), (2, 0.55))):
            if j >= stage + 1:
                break
            top = (x + dx, 15 - hh * f)
            leaf(img, (x, 15 - hh * 0.35 + j), top, 1.6, G_BLUE, bend=-dx * 0.5, shape="strap",
                 dim=0 if j < 2 else -1)
            if stage == 3 and j == 0:  # tip drying off
                put(img, top[0], top[1], G_OLD[3])
                put(img, top[0] + (1 if dx > 0 else -1) * 0, top[1] + 1, G_OLD[2])
        if stage == 3:   # papery bulb tops showing at the soil
            for dx, c in ((-1, "e6e0d4"), (0, "f6f2ea"), (1, "c8b4c4")):
                put(img, x + dx, 15, c)
            put(img, x, 14, "dcd2cc")
            put(img, x - 1, 14, "eee8de")
    if stage == 3:   # one curling scape
        pts = curve(img, (8, 12), (10, 3), 3, ramp(G_BLUE[2:]))
        put(img, 11, 3, G_BLUE[3]); put(img, 11, 4, "e8e4d8"); put(img, 10, 2, "f6f2ea")
    return img


def onion(stage, wild=False):
    img = new()
    r = rand("wild_onion" if wild else "onion")
    pal = G_BLUE if not wild else G_ONION
    xs = (2, 5, 9, 13) if not wild else (3, 7, 12)
    if stage == 0:
        sprouts(img, "blade", xs[::2] if wild else xs, pal)
        return img
    h = {False: (0, 5, 10, 9), True: (0, 5, 8, 8)}[wild][stage]
    for i, x in enumerate(xs):
        n = (2 if stage == 1 else 3) if wild else (2 if stage == 1 else 3)
        for j in range(n):
            hh = h - j * 2 - r.randint(0, 1)
            dr = (-2, 2, 0, -1)[(i + j) % 4] * (0.6 if wild else 1)
            if stage == 3 and not wild and j == 0:
                # the tops have fallen over: the leaf bends hard and lies out sideways, yellowing at the tip
                side = 1 if i % 2 else -1
                pts = [(x, 15), (x, 12), (x + side * 2, 9), (x + side * 4, 8)]
                poly(img, pts, lambda k, m: pal[2] if k < m * 0.6 else G_OLD[2] if k < m * 0.85 else G_OLD[3])
                continue
            tube(img, x + (j - n // 2), hh, dr, pal, dim=-1 if j else 0)
        if stage == 3 and not wild:   # golden bulb shoulders at the soil
            put(img, x - 1, 15, "a8642a"); put(img, x, 15, "d8984a"); put(img, x + 1, 15, "b87434")
            put(img, x, 14, "e8b468"); put(img, x - 1, 14, "c88440")
    if wild and stage == 3:
        for x, top in ((5, 4), (10, 2)):
            line(img, x, 15, x, top + 2, ramp(STEM_G))
            globe(img, x, top, 1.2, PINKP, r)
    return img


def cabbage(stage, wild=False):
    img = new()
    r = rand("wild_cabbage" if wild else "cabbage")
    pal = G_BLUE
    if stage == 0:
        sprouts(img, "coty", (4, 11), pal)
        return img
    if wild:   # sea-cliff cabbage: loose waxy blue-grey leaves, later a straggly stalk of pale yellow flowers
        sz = (0, 4, 6, 6)[stage]
        for x, dx in ((4, -1), (11, 1)):
            leaf(img, (x, 15), (x + dx * sz * 0.6, 15 - sz), 3 + (stage > 1), pal, bend=-dx, shape="oval", rib=True, dim=-1)
            leaf(img, (x, 15), (x - dx * 2, 16 - sz), 3, pal, bend=dx, shape="oval", rib=True)
        if stage == 3:
            for x, top, lean in ((6, 2, -1), (10, 4, 1)):
                pts = curve(img, (x, 14), (x + lean, top + 3), lean, ramp(G_BLUE[1:4]))
                spike(img, x + lean, top, 5, PALEY, r, stem=G_BLUE[3], pods=2, podc=G_BLUE[3])
        return img
    # cultivated: a rosette of broad veined leaves, which cup and close into a dense round head
    if stage == 1:
        for x in (4, 11):
            leaf(img, (x, 15), (x - 3, 11), 3, pal, bend=-1, shape="oval", rib=True)
            leaf(img, (x, 15), (x + 3, 11), 3, pal, bend=1, shape="oval", rib=True, dim=-1)
            leaf(img, (x, 15), (x, 10), 3, pal, shape="oval", rib=True)
        return img
    # big outer leaves spreading low, flopped out from the centre
    leaf(img, (7, 15), (1, 9), 5, pal, bend=-2, shape="round", rib=True, dim=-1)
    leaf(img, (8, 15), (15, 9), 5, pal, bend=2, shape="round", rib=True, dim=-1)
    if stage == 2:
        leaf(img, (8, 15), (5, 6), 5, pal, bend=-1, shape="round", rib=True)
        leaf(img, (8, 15), (11, 7), 5, pal, bend=1, shape="round", rib=True, dim=-1)
        return img
    # the head: a pale tight ball with the wrapper leaves' veins curving over it
    HEAD = ("4e7a62", "6c9a7c", "8ebc98", "b4d8b4", "d6ecd0")
    cx, cy, rx, ry = 7.5, 11.2, 4.4, 3.9
    for y in range(16):
        for x in range(16):
            v = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if v <= 1.0:
                lt = (cx - x) / rx + (cy - y) / ry
                k = 3 if lt > 0.7 else 2 if lt > -0.3 else 1
                if v > 0.72 and lt < 0:
                    k = 0
                put(img, x, y, HEAD[k])
    for pts in (((5, 8), (4, 10), (4, 13)), ((8, 7), (10, 9), (11, 12)), ((7, 8), (7, 10))):
        poly(img, pts, HEAD[4])
    leaf(img, (5, 15), (2, 11), 3, pal, bend=-1, shape="oval", rib=True)   # wrapper leaves hugging the head's foot
    leaf(img, (10, 15), (13, 11), 3, pal, bend=1, shape="oval", rib=True, dim=-1)
    return img


def leek(stage, wild=False):
    img = new()
    r = rand("wild_leek" if wild else "leek")
    pal = G_BLUE if not wild else G_FRESH
    xs = (3, 8, 13) if not wild else (4, 11)
    if stage == 0:
        sprouts(img, "blade", xs, pal)
        return img
    if wild:   # thin flat strap leaves in a loose fan; ripe: a tall stalk with a pale violet ball
        h = (0, 5, 8, 8)[stage]
        for x in xs:
            for dx, f in ((-3, 0.9), (2, 1.0), (0, 0.8)):
                leaf(img, (x, 15), (x + dx, 15 - h * f), 1.3, pal, bend=-dx * 0.4, shape="strap", dim=0 if dx else -1)
        if stage == 3:
            line(img, 8, 15, 8, 4, ramp(STEM_G))
            globe(img, 8, 3, 1.6, VIOLET, r)
        return img
    h = (0, 6, 10, 12)[stage]
    for i, x in enumerate(xs):
        shaft = (0, 2, 4, 5)[stage]
        # the blanched shaft: white at the soil, green-white higher up
        for k in range(shaft):
            c = ("f2f0e2", "e4e8cc", "c8dca8", "a8c890", "92b880")[min(4, k)]
            put(img, x, 15 - k, c)
            put(img, x + 1, 15 - k, ("cfcdbc", "c4c8a8", "a8bc8c", "8aa874", "789868")[min(4, k)])
        y0 = 15 - shaft
        for j, (dx, f) in enumerate(((-3, 0.95), (3, 0.85), (-1, 1.0), (2, 0.7))):   # flat folded leaves fanned in one plane
            if j >= stage + 1:
                break
            leaf(img, (x + (dx > 0), y0 + 1), (x + dx + (dx > 0), 15 - h * f), 2.2, G_BLUE, bend=-dx * 0.5,
                 shape="strap", dim=-1 if j % 2 else 0)
    return img


def parsnip(stage, wild=False):
    img = new()
    r = rand("wild_parsnip" if wild else "parsnip")
    pal = G_YEL if not wild else G_WILD
    if stage == 0:
        sprouts(img, "umbel", (4, 11) if wild else (3, 8, 13), pal)
        return img
    if wild:
        h = (0, 5, 8, 8)[stage]
        for x, lean in ((3, -1), (7, 1), (12, 1)):
            pinnate(img, x, h - r.randint(0, 2), lean, pal, r, leaflet=1 if stage < 3 else 2)
        if stage == 3:   # branching stalk with flat yellow umbels
            line(img, 8, 15, 8, 5, ramp(STEM_G))
            line(img, 8, 9, 5, 5, STEM_G[2])
            line(img, 8, 8, 12, 4, STEM_G[2])
            flat_umbel(img, 8, 3, 5, YEL, r)
            flat_umbel(img, 4, 4, 3, YEL, r, rays=False)
            flat_umbel(img, 12, 3, 3, YEL, r, rays=False)
        return img
    h = (0, 6, 10, 11)[stage]
    for i, (x, lean) in enumerate(((2, -1), (5, 0), (8, 1), (11, 0), (14, 1))):
        if stage == 1 and i % 2:
            continue
        pinnate(img, x, h - (i % 2) * 2 - r.randint(0, 1), lean * (stage - 0.5) / 2, pal, r, leaflet=2,
                dim=-1 if i % 2 else 0)
    if stage == 3:   # cream root shoulders and leaf stubs
        for x in (4, 10):
            for dx, c in ((-1, "c8b07a"), (0, "efe2b8"), (1, "e0cc98"), (2, "b89c64")):
                put(img, x + dx, 15, c)
            put(img, x, 14, "f6ecc8"); put(img, x + 1, 14, "d8c08c")
    return img


def trefoil(img, x, y, pal, flip=False):
    """A three-lobed glossy leaflet (celery, smallage): top lobe and two side lobes, notched between."""
    f = -1 if flip else 1
    for dx, dy, k in ((0, -1, 4), (0, 0, 3), (-f, 0, 3 if not flip else 2), (f, 0, 2 if not flip else 3), (-f, 1, 2),
                      (f, 1, 1), (0, -2, 3)):
        put(img, x + dx, y + dy, pal[k])


def smallage(img, base, tip, bend, stage):
    """Wild celery leaf: a thin stalk forking into glossy three-lobed leaflets."""
    pts = curve(img, base, tip, bend, ramp(STEM_G))
    t = pts[-1]
    trefoil(img, t[0], t[1], G_GLOSS)
    if stage >= 2 and len(pts) > 4:
        m = pts[len(pts) // 2]
        side = 1 if (t[0] - base[0]) <= 0 else -1
        put(img, m[0] + side, m[1] - 1, STEM_G[2])
        trefoil(img, m[0] + side * 2, m[1] - 2, shift(G_GLOSS, -1), flip=side < 0)


def celery(stage, wild=False):
    img = new()
    r = rand("wild_celery" if wild else "celery")
    if stage == 0:
        sprouts(img, "umbel", (4, 11) if wild else (3, 8, 13), G_GLOSS if wild else G_FRESH)
        return img
    if wild:   # smallage: dark glossy lobed leaflets on thin stalks; ripe: small white-green umbels
        h = (0, 5, 8, 8)[stage]
        for x, lean, dh in ((3, -2, 1), (7, 0, -1), (12, 2, 2)):
            smallage(img, (x, 15), (x + lean, 16 - h + dh), lean * 0.3, stage)
        if stage == 3:
            for x, top in ((5, 4), (10, 3)):
                line(img, x, 13, x, top + 1, STEM_G[2])
                flat_umbel(img, x, top, 3, CREAMW, r, rays=False)
        return img
    # cultivated: an upright bunch of pale ribbed stalks with bright leaflets at the top
    h = (0, 6, 9, 11)[stage]
    for i, (x, lean) in enumerate(((3, -2), (5, -1), (7, 0), (9, 1), (11, 2), (13, 2))):
        if stage == 1 and i % 2:
            continue
        hh = h - (0, 1, 0, 1, 2, 3)[i]
        pale = ("8ab460", "a4cc78", "c4e0a0") if stage == 3 else STEM_G
        pts = curve(img, (8 + (x - 8) // 3, 15), (x + lean, 16 - hh), lean * 0.4, ramp(pale))
        top = pts[-1]
        for dx, dy, c in ((-1, 0, G_FRESH[3]), (1, 0, G_FRESH[2]), (0, -1, G_FRESH[4]), (-1, -1, G_FRESH[3]),
                          (1, 1, G_FRESH[1])):
            put(img, top[0] + dx, top[1] + dy, c)
    if stage == 3:   # thick pale base of the bunch
        for x in range(5, 11):
            put(img, x, 15, ("b4d48c", "dcecb8", "c4dc9c")[x % 3])
            put(img, x, 14, ("a4c47c", "c8e0a4", "b4d08c")[x % 3])
    return img


def turnip(stage, wild=False):
    img = new()
    r = rand("wild_turnip" if wild else "turnip")
    pal = G_FRESH if not wild else G_WILD
    if stage == 0:
        sprouts(img, "coty", (4, 11) if wild else (3, 8, 13), pal)
        return img
    if wild:   # field mustard look: rough lobed leaves, thin branching stems topped with bright yellow flowers
        sz = (0, 4, 6, 6)[stage]
        for x, dx in ((4, -1), (11, 1)):
            lobed(img, (x, 15), (x + dx * 3, 15 - sz), 3, pal, r, bend=-dx)
            lobed(img, (x, 15), (x - dx, 15 - sz - 1), 3, pal, r, dim=-1)
        if stage >= 2:
            for x, top, lean in ((7, 3 if stage == 3 else 6, -1), (9, 2 if stage == 3 else 5, 1)):
                curve(img, (x, 15), (x + lean * 2, top + 1), lean, ramp(STEM_G))
                if stage == 3:
                    tx = x + lean * 2
                    for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (-1, 1), (1, 1)):
                        put(img, tx + dx, top + dy, YEL[2] if dy <= 0 and dx <= 0 else YEL[1])
                    put(img, tx - 1, top - 1, YEL[3])
                    put(img, tx + lean, top + 3, G_WILD[3])
        return img
    h = (0, 5, 8, 9)[stage]
    for i, x in enumerate((4, 11)):
        lobed(img, (x, 15), (x - 3, 15 - h + 1), 3.5, pal, r, bend=-1, dim=-1)
        lobed(img, (x, 15), (x + 3, 15 - h + 1), 3.5, pal, r, bend=1, dim=-1)
        if stage >= 2:
            lobed(img, (x, 15), (x + (i * 2 - 1), 15 - h), 3.5, pal, r)
        if stage == 3:   # white globe with a purple shoulder pushing out of the soil
            for dx, c in ((-2, "6a2a5c"), (-1, "a04a8a"), (0, "b862a0"), (1, "8a3c76"), (2, "5a2050")):
                put(img, x + dx, 14, c)
            for dx, c in ((-2, "d8d2c8"), (-1, "f4f0e8"), (0, "fffcf4"), (1, "e8e2d8"), (2, "c0b8ac")):
                put(img, x + dx, 15, c)
            put(img, x - 1, 13, "8a3c76"); put(img, x, 13, "a04a8a")
    return img


def lettuce_leaf(img, x, y, side, pal):
    """Prickly lettuce leaf: clasping the stem, held steeply up and edge-on, with a lobe notch and a pale midrib."""
    leaf(img, (x, y), (x + side * 3, y - 3), 2.2, pal, bend=side * 0.5, shape="lance", rib=True, dim=-1 if side > 0 else 0)
    if 0 <= x + side * 2 < 16 and 0 <= y - 1 < 16:   # the lobe notch
        img.putpixel((x + side * 2, y - 1), (0, 0, 0, 0))


def prickly_lettuce(img, r, stage):
    """Stage 2 (bolting), 3 (flowering) or 4 (the world plant): a basal rosette of lobed leaves, one stiff, slightly
    branching stem with clasping leaves at different angles and lengths, loose pale-yellow heads at the tips."""
    pal = G_LETT
    stems = [(7, 0)] if stage < 4 else [(6, 0), (12, 3)]
    for sx, drop in stems:   # stems first, so the rosette sits in front of their feet
        top = (5 if stage == 2 else 2) + drop
        kink = (sx + 1, top + 4)
        line(img, sx, 15, sx, top + 5, ramp(STEM_G))
        line(img, sx, top + 5, kink[0], top, STEM_G[2])
        mid = (sx, top + 5)
        tips = [(kink[0], top - 1)]
        if drop == 0:   # side branches off the upper stem
            line(img, sx, top + 4, sx - 3, top + 1, STEM_G[2])
            line(img, kink[0], top + 2, sx + 4, top, STEM_G[2])
            tips += [(sx - 3, top), (sx + 4, top - 1)]
        # clasping stem leaves: alternate sides, each a different length and angle
        for (y, side, ln, rise) in ((11, -1, 4, 2), (9, 1, 3, 3), (7, -1, 2, 2), (13, 1, 3, 1)):
            if y > top + 5 - drop:
                leaf(img, (sx, y), (sx + side * ln, y - rise), 1.9, pal, bend=side * 0.4, shape="lance", rib=True,
                     dim=-1 if side > 0 else 0)
        for (tx, ty) in tips:
            if stage == 2:   # tight green buds
                put(img, tx, ty, "a8b86a"); put(img, tx - 1, ty, "8aa058")
            else:            # loose pale-yellow florets with a green involucre
                put(img, tx, ty + 1, "8aa058")
                put(img, tx, ty, PALEY[2]); put(img, tx - 1, ty - 1, PALEY[3]); put(img, tx + 1, ty, PALEY[1])
                if r.random() < 0.6:
                    put(img, tx + 1, ty - 1, PALEY[2])
    # the basal rosette of lobed leaves, low and spread
    for (b, t, w, dm) in (((7, 15), (1, 12), 3.4, -1), ((8, 15), (14, 12), 3.4, -1), ((7, 15), (3, 11), 3, 0),
                          ((8, 15), (11, 11), 3, 0)):
        lobed(img, b, t, w, pal, r, dim=dm)


def lettuce(stage, wild=False):
    img = new()
    r = rand("wild_lettuce" if wild else "lettuce")
    if stage == 0:
        sprouts(img, "broad", (4, 11) if wild else (3, 8, 13), G_YEL if not wild else G_LETT)
        return img
    if wild:   # prickly lettuce: a tall stiff stem with lobed clasping leaves held edge-on, pale yellow heads
        pal = G_LETT
        if stage == 1:
            for x in (4, 11):
                lobed(img, (x, 15), (x - 2, 11), 2.5, pal, r, bend=-1)
                lobed(img, (x, 15), (x + 2, 11), 2.5, pal, r, bend=1, dim=-1)
            return img
        prickly_lettuce(img, r, stage)
        return img
    # cultivated: a loose head of frilly light-green leaves
    pal = G_YEL
    if stage == 1:
        for x in (4, 11):
            leaf(img, (x, 15), (x - 2, 11), 3, pal, shape="spoon", rib=True)
            leaf(img, (x, 15), (x + 2, 11), 3, pal, shape="spoon", rib=True, dim=-1)
        return img
    s = 0 if stage == 2 else 1
    for (bx, tip, w, dm) in (((7, 15), (1, 10 - s), 4.5, -1), ((8, 15), (15, 10 - s), 4.5, -1),
                             ((7, 15), (4, 7 - s * 2), 4.5, 0), ((8, 15), (11, 7 - s * 2), 4.5, 0),
                             ((7, 15), (8, 8 - s * 2), 4, 1)):
        leaf(img, bx, tip, w + s, pal, shape="spoon", rib=True, dim=dm)
    # frilly leaf margins: lighter crinkles on the top edges
    for x, y in ((3, 8 - s * 2), (5, 6 - s * 2), (10, 6 - s * 2), (12, 8 - s * 2), (8, 6 - s * 2)):
        if get(img, x, y):
            put(img, x, y - 1, pal[4])
    if stage == 3:
        for x, y in ((6, 13), (8, 12), (9, 13)):
            put(img, x, y, "e6f0a8")
    return img


def broad_bean(stage):
    img = new()
    r = rand("broad_bean")
    pal = G_BEAN
    if stage == 0:
        sprouts(img, "bean", (4, 11), pal)
        return img
    h = (0, 6, 12, 13)[stage]
    for n, x in enumerate((3, 8, 12)):
        hh = h - (n % 2) - r.randint(0, 1)
        for i in range(hh):    # thick square stem, darker at the foot
            put(img, x, 15 - i, STEM_G[0] if i < 2 else STEM_G[1] if i < hh * 0.6 else STEM_G[2])
        if stage == 2:   # small white flowers in the leaf axils, a dark blotch on one wing; leaves drawn over them
            for i in (5, 9):
                if i < hh - 2:
                    sd = -1 if (i // 2 + n) % 2 else 1   # opposite the leaflet at this node
                    sd = -sd
                    put(img, x + sd, 15 - i, "f4f2ec"); put(img, x + sd, 14 - i, "e2e0d8")
                    put(img, x + 2 * sd, 15 - i, "d4d0c6"); put(img, x + 2 * sd, 14 - i, "3a2e3e")
        for i in range(2, hh - 1, 2):   # glaucous oval leaflets, alternate, angled up and out
            side = -1 if (i // 2 + n) % 2 else 1
            y = 15 - i
            big = i < hh - 3
            for dx, dy, k in ((1, 0, 2), (2, 0, 1), (1, -1, 3), (2, -1, 2), (3, -1, 1), (2, -2, 4), (3, -2, 2)):
                if big or dx < 3:
                    put(img, x + side * dx, y + dy, pal[k if side < 0 else max(0, k - 1)])
        put(img, x, 15 - hh, pal[4])
        put(img, x, 14 - hh, pal[3])
    if stage == 3:   # fat glossy pale pods hanging down from the nodes, outlined dark so they stand off the leaves
        for (px, top, side) in ((4, 5, 1), (0, 9, 1), (9, 4, 1), (13, 8, -1)):
            m = set()
            for k in range(4):
                xx = px + (1 if k >= 3 else 0) * side
                m |= {(xx, top + k), (xx + 1, top + k)}
            for (mx, my) in m:
                k = my - top
                left = (mx - 1, my) not in m
                put(img, mx, my, ("c4e49a" if k == 1 else "a2cc72") if left else ("80b052" if k < 3 else "6a9a44"))
            put(img, px, top, "c8e698")
            outline_layer(img, m, "263e18")
            put(img, px + (1 if side < 0 else 0), top - 1, STEM_G[1])   # the stalk up to the node
    return img


def dill(stage):
    img = new()
    r = rand("dill")
    pal = G_BLUE
    if stage == 0:
        sprouts(img, "umbel", (3, 8, 13), pal)
        return img
    h = (0, 6, 11, 12)[stage]
    for i, (x, lean) in enumerate(((2, -1), (5, 1), (8, 0), (11, -1), (14, 1))):
        if stage == 1 and i % 2:
            continue
        feather(img, x, h - (i % 2) * 2 - r.randint(0, 1) - (3 if stage == 3 else 0), lean, pal, r, dim=-1 if i % 2 else 0)
    if stage == 3:   # tall hollow stalks with big airy yellow umbels
        for x, top in ((4, 3), (11, 2)):
            line(img, x, 15, x, top + 2, ramp(G_BLUE[1:4]))
            flat_umbel(img, x, top, 5, YEL, r, stem=G_BLUE[2])
            for dx in (-2, 2):
                put(img, x + dx, top + 1, G_BLUE[3])
    return img


def mustard(stage, tall=False):
    img = new()
    r = rand("mustard")
    pal = G_FRESH
    if stage == 0:
        sprouts(img, "coty", (3, 8, 13), pal)
        return img
    if stage == 1:
        for x in (4, 11):
            lobed(img, (x, 15), (x - 3, 11), 3, pal, r, bend=-1)
            lobed(img, (x, 15), (x + 3, 11), 3, pal, r, bend=1, dim=-1)
        return img
    top = 5 if stage == 2 else 2
    for x in (4, 11):
        leaf(img, (x, 15), (x - 3, 12), 3, pal, shape="spoon", rib=True, dim=-1)
        leaf(img, (x, 15), (x + 3, 12), 3, pal, shape="spoon", rib=True, dim=-1)
    for x, lean, dt in ((3, -1, 2), (7, 0, 0), (10, 1, 3), (13, 1, 1)):
        pts = curve(img, (x, 15), (x + lean, top + 2 + dt), lean * 0.5, ramp(STEM_G))
        t = pts[-1]
        if stage == 2:
            for dx, dy in ((0, -1), (-1, 0), (1, 0)):
                put(img, t[0] + dx, t[1] + dy, "b8c43c")   # green flower buds
        else:
            raceme(img, t[0], t[1] - 3, 7, r, lean=1 if lean >= 0 else -1)
    return img


RAPE_Y = ("c89a08", "f0c814", "fce03a", "fff492")   # oilseed rape's hard lemon yellow, warmer than mustard
POD_G = ("4e6a2a", "6e8c3a", "94ae52")                 # green siliques
POD_R = ("6e5a26", "9a8238", "c4ac5a", "e2d08a")       # ripe straw-brown siliques


def rapeseed(stage):
    """Oilseed rape (tamed from the Wild Turnip, Herbs.domesticate): waxy blue-green lobed leaves; tall branching stems
    crowned with dense lemon-yellow heads (the yellow fields of spring); ripe, the stems straw-coloured and bristling with
    slim pods angled up, the last flowers at the very tips."""
    img = new()
    r = rand("rapeseed")
    pal = G_BLUE
    if stage == 0:
        sprouts(img, "coty", (3, 8, 13), pal)
        return img
    if stage == 1:   # a low rosette of broad lobed leaves, three plants
        for x, h in ((4, 6), (11, 7)):
            leaf(img, (x, 15), (x - 3, 15 - h + 2), 3, pal, shape="spoon", rib=True, dim=-1, bend=-0.5)
            leaf(img, (x, 15), (x + 3, 15 - h + 2), 3, pal, shape="spoon", rib=True, dim=-1, bend=0.5)
            lobed(img, (x, 15), (x + (1 if x < 8 else -1), 15 - h), 3, pal, r)
        return img
    ripe = stage == 3
    stem = ("8a8a40", "a8a454", "c4bc6c") if ripe else STEM_G
    for x in (4, 11):   # the old rosette leaves at the foot, yellowing once ripe
        lobed(img, (x, 15), (x - 3, 12), 3, G_OLD if ripe else pal, r, bend=-1, dim=-1)
        lobed(img, (x, 15), (x + 3, 12), 3, G_OLD if ripe else pal, r, bend=1, dim=-1)
    heads = ((2, -1, 4), (6, 0, 1), (9, 1, 2), (13, 1, 5))
    for x, lean, top in heads:
        pts = curve(img, (x + (1 if x < 8 else -1), 15), (x + lean, top + 2), lean * 0.6, ramp(stem))
        for k in (len(pts) // 2, len(pts) * 2 // 3):   # a small leaf clasping the stem
            sx, sy = pts[k]
            side = 1 if (k + x) % 2 else -1
            put(img, sx + side, sy, (G_OLD if ripe else pal)[2]); put(img, sx + side, sy - 1, (G_OLD if ripe else pal)[3])
        tx, ty = pts[-1]
        if not ripe:   # a dense domed head: open flowers round the edge, tight green-yellow buds on top
            for dx, dy, k in ((-1, 0, 2), (0, 0, 2), (1, 0, 1), (-2, 1, 1), (-1, 1, 3), (0, 1, 2), (1, 1, 1), (2, 1, 0),
                              (-1, 2, 1), (0, 2, 2), (1, 2, 0), (0, -1, 3), (-1, -1, 2), (1, -1, 2)):
                if dy < 2 or r.random() < 0.8:
                    put(img, tx + dx, ty + dy, RAPE_Y[k])
            put(img, tx, ty - 2, "c8cc5a")   # the buds still closed at the top
            for i in range(3, 7, 2):   # the first young pods below the head
                side = -1 if (i // 2) % 2 else 1
                put(img, tx + side, ty + i, POD_G[1]); put(img, tx + 2 * side, ty + i - 1, POD_G[2])
        else:   # siliques up the whole upper stem, a last flower or two at the tip
            put(img, tx, ty, RAPE_Y[2]); put(img, tx - 1, ty, RAPE_Y[1])
            if x in (6, 13):
                put(img, tx, ty - 1, RAPE_Y[3])
            for i in range(1, min(9, len(pts) - 2), 2):
                sx, sy = pts[-1 - i]
                side = -1 if (i // 2) % 2 else 1
                put(img, sx + side, sy - 1, POD_R[2 if side < 0 else 1])
                put(img, sx + 2 * side, sy - 2, POD_R[3 if side < 0 else 2])
    return img


def ginger(stage):
    img = new()
    r = rand("ginger")
    pal = G_FRESH
    if stage == 0:
        for x in (4, 11):
            put(img, x, 15, "c88a6a"); put(img, x, 14, G_FRESH[2]); put(img, x, 13, G_FRESH[3]); put(img, x + 1, 12, G_FRESH[3])
        return img
    h = (0, 6, 11, 12)[stage]
    for n, (x, lean) in enumerate(((3, -1), (6, 0), (10, 1), (13, 0))):
        if stage == 1 and n % 2:
            continue
        hh = h - (n % 2) * 2 - r.randint(0, 1)
        for i in range(hh):   # reed-like pseudostem
            put(img, x + (lean if i > hh * 0.6 else 0), 15 - i, ("8a6a3a", STEM_G[1], STEM_G[2])[0 if i < 1 else 1 if i < hh / 2 else 2])
        for i in range(3, hh, 2):   # long narrow leaves held alternately out to the sides, drooping at the tips
            side = 1 if (i // 2 + n) % 2 else -1
            xx = x + (lean if i > hh * 0.6 else 0)
            leaf(img, (xx, 15 - i), (xx + side * 4, 16 - i), 1.6, pal, bend=side * -1.2, shape="lance",
                 dim=-1 if side > 0 else 0)
    if stage == 3:   # pinkish rhizome knuckles at the soil
        for x in (5, 11):
            for dx, dy, c in ((-1, 0, "d8a888"), (0, 0, "f0c8a8"), (1, 0, "e4b494"), (2, 0, "b88466"), (0, -1, "e8a8a0"),
                              (-2, 0, "c49070"), (1, -1, "d07c80")):
                put(img, x + dx, 15 + dy, c)
    return img


def saffron(stage):
    img = new()
    r = rand("saffron")
    pal = G_FRESH
    if stage == 0:
        sprouts(img, "grass", (3, 8, 13), pal)
        return img
    h = (0, 5, 8, 7)[stage]
    for i, x in enumerate((2, 5, 8, 11, 14)):   # narrow grassy leaves with a pale midline stripe
        for dr in (-2, 0, 2):
            hh = h - abs(dr) // 2 - r.randint(0, 1)
            blade(img, x + dr // 2, hh, dr, pal, dim=-1 if dr else 0)
        if stage >= 2:
            put(img, x, 15 - h // 2, "d8eac0")
    if stage == 3:   # low purple crocus cups with three red stigma threads
        for x, y in ((4, 9), (9, 8), (13, 10)):
            put(img, x, y + 2, "d8dcc8"); put(img, x, y + 3, "c8ccb4")
            for dx, dy, c in ((-1, 0, "b48ed8"), (0, 0, "9a6cc8"), (1, 0, "7a4aa6"), (-1, -1, "c8a8e8"),
                              (1, -1, "8a5cb8"), (0, 1, "6a3c94"), (-1, 1, "8a60b4"), (1, 1, "5a2c84")):
                put(img, x + dx, y + dy, c)
            put(img, x, y - 1, "e02818"); put(img, x - 1, y - 2, "c81c10"); put(img, x + 1, y - 2, "f06030")
    return img


def rice(stage):
    img = new()
    r = rand("rice")
    pal = ("2a6a1c", "3c8a26", "52aa32", "74c844", "a0e070")
    if stage == 0:
        sprouts(img, "grass", (2, 6, 10, 14), pal)
        return img
    h = (0, 6, 11, 11)[stage]
    for i, x in enumerate((1, 4, 7, 10, 13)):
        for dr in (-2, -1, 1, 2):
            hh = h - abs(dr) - r.randint(0, 1)
            blade(img, x + (1 if dr > 0 else 0), hh, dr * 1.4, pal if stage < 3 else shift(pal, -1), dim=-1 if abs(dr) == 2 else 0)
    if stage == 3:   # golden panicles nodding over on arching stems
        for x, top, side in ((3, 3, 1), (8, 2, -1), (12, 4, 1)):
            pts = curve(img, (x, 15), (x + side, top), -side, ramp(("8a9a3a", "a8b04c", "c8b860")))
            t = pts[-1]
            for k in range(5):   # the panicle hangs down from the top in a loose arc of grains
                gx, gy = t[0] + side * (1 + k // 2), t[1] + k
                put(img, gx, gy, ("f2d878", "e2bc54", "c89a3a")[k % 3])
                if k % 2 == 0:
                    put(img, gx + side, gy, "e8c860")
    return img


# ============================== wild plants in the world ==============================
def wild_plant(name):
    """The world forms: the wild crop's ripe look, a little fuller, as a standalone flowering plant."""
    r = rand(name + "_plant")
    img = new()
    if name == "wild_garlic":
        for x, lean in ((2, -2), (5, 1), (9, -1), (13, 2)):
            leaf(img, (x, 15), (x + lean, 6 + r.randint(0, 3)), 3.4, G_GLOSS, bend=lean * 0.8, rib=True,
                 dim=-1 if x % 2 else 0)
        for x, top in ((7, 3), (11, 5)):
            line(img, x, 15, x, top + 2, ramp(STEM_G))
            starball(img, x, top, WHITE, r)
    elif name == "wild_onion":
        for x in (2, 5, 9, 13):
            for dr, dh in ((-2, 2), (0, 0), (2, 1)):
                tube(img, x, 8 - dh - r.randint(0, 1), dr * 0.7, G_ONION, dim=-1 if dr else 0)
        for x, top in ((6, 3), (11, 4)):
            line(img, x, 15, x, top + 2, ramp(STEM_G))
            globe(img, x, top, 1.3, PINKP, r)
    elif name == "wild_cabbage":
        for x, dx in ((4, -1), (11, 1)):
            leaf(img, (x, 15), (x + dx * 3, 8), 3.6, G_BLUE, bend=-dx, shape="oval", rib=True, dim=-1)
            leaf(img, (x, 15), (x - dx, 9), 3.4, G_BLUE, bend=dx * 0.6, shape="oval", rib=True)
        for x, top, lean in ((6, 1, -1), (10, 3, 1)):
            curve(img, (x, 15), (x + lean, top + 5), lean, ramp(G_BLUE[1:4]))
            spike(img, x + lean, top, 6, PALEY, r, stem=G_BLUE[3], pods=2, podc=G_BLUE[3])
    elif name == "wild_leek":
        for x in (3, 12):
            for dx, f in ((-3, 0.9), (2, 1.0), (0, 0.8), (-1, 0.6)):
                leaf(img, (x, 15), (x + dx, 15 - 8 * f), 1.5, G_FRESH, bend=-dx * 0.4, shape="strap", dim=0 if dx else -1)
        line(img, 8, 15, 8, 4, ramp(STEM_G))
        line(img, 7, 15, 7, 13, STEM_G[0])
        globe(img, 8, 2.5, 1.7, VIOLET, r)
    elif name == "wild_parsnip":
        for x, lean in ((2, -1), (5, 0), (11, 1), (14, 1)):
            pinnate(img, x, 6 - r.randint(0, 1), lean, G_WILD, r, leaflet=2, dim=-1 if x > 8 else 0)
        line(img, 8, 15, 8, 5, ramp(STEM_G))
        line(img, 8, 10, 4, 5, STEM_G[2])
        line(img, 8, 8, 12, 4, STEM_G[2])
        flat_umbel(img, 8, 3, 5, YEL, r)
        flat_umbel(img, 4, 4, 3, YEL, r, rays=False)
        flat_umbel(img, 12, 3, 3, YEL, r, rays=False)
    elif name == "wild_celery":
        for x, lean in ((2, -1), (5, 0), (11, 1), (14, 0)):
            smallage(img, (x, 15), (x + lean, 9 + r.randint(0, 2)), lean * 0.3, 3)
        for x, top in ((7, 4), (10, 2)):
            line(img, x, 15, x, top + 1, ramp(STEM_G))
            flat_umbel(img, x, top, 3, CREAMW, r, rays=False)
        put(img, 8, 6, STEM_G[2]); put(img, 9, 5, STEM_G[2])
        flat_umbel(img, 5, 6, 3, CREAMW, r, rays=False)
        line(img, 6, 8, 7, 9, STEM_G[2])
    elif name == "wild_turnip":
        for x, dx in ((3, -1), (12, 1)):
            lobed(img, (x, 15), (x + dx * 3, 9), 3.5, G_WILD, r, bend=-dx)
            lobed(img, (x, 15), (x - dx, 8), 3, G_WILD, r, dim=-1)
        for x, top, lean in ((6, 2, -1), (9, 1, 1), (8, 5, 0)):
            curve(img, (x, 15), (x + lean * 2, top + 1), lean, ramp(STEM_G))
            tx = x + lean * 2
            for dx, dy in ((0, 0), (-1, 0), (1, 0), (0, -1), (-1, 1), (1, 1)):
                put(img, tx + dx, top + dy, YEL[2] if dy <= 0 and dx <= 0 else YEL[1])
            put(img, tx - 1, top - 1, YEL[3])
            put(img, tx + lean, top + 3, G_WILD[2]); put(img, tx + lean, top + 4, G_WILD[2])
    elif name == "wild_lettuce":
        prickly_lettuce(img, r, 4)
    return img


# ============================== herbs: full and cut back ==============================
SAGE_PAL = dict(K="26321f", D="3e5034", M="5a6e4a", L="788e62", S="9cb088", w="bccaa8", s="6a6a48", c="c8b890",
                V="8a62c0", v="5e3e9c", P="b494e4")
SAGE = [  # woolly grey-green oblong leaves held up and out, silver upper edges, dark undersides, violet spikes
    ".....P..........",
    "....PV.....P....",
    ".....Vv...PV....",
    "....PV.....Vv...",
    ".....vV...PV....",
    "....PVv....Vv...",
    ".....s.....vs...",
    "..SS.s.S...s.SS.",
    ".SwLMs.SwL.sSLMD",
    ".SLMMDsSLMD.SLMD",
    "..LMDKsLMMDKLMD.",
    "S..MDK.SLMDKMDK.",
    "Sw..SLLMMDK.KD.S",
    "SLMSLMMDK.SwLLMD",
    ".LMDLMDK.SLMMDK.",
    "..KD.KD..LMDK...",
]
SAGE_CUT = [   # woody stubs with pale cut ends, new woolly leaves sprouting round their feet
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "...c.......c....",
    ".S.s...c...s.S..",
    "SwLs.S.s.S.sSLD.",
    "SLMsDSwsSLDsLMD.",
    ".LMDKLMsLMDsMDK.",
    "SLMDsLMDsMDKsLMD",
    ".KDKsKDKsKD.sKD.",
]


def _herb(name, cut=False):
    r = rand(name + ("_cut" if cut else ""))
    img = new()
    if name == "wild_thyme":   # a low dense mat of tiny grey-green leaves with pink-purple flower clusters
        pal = ("3e4a32", "56664a", "72845e", "94a67c", "b8c8a0")
        if cut:
            stubs(img, (3, 6, 9, 12), r, cols=("6a5a40", "7a6a48", "c8b890"), hmax=2)
            for x, y in ((4, 14), (8, 13), (11, 14), (2, 15), (13, 15), (7, 14)):
                put(img, x, y, pal[3]); put(img, x + 1, y, pal[2])
            return img
        for x in range(16):   # woody runners at the soil
            put(img, x, 15, (STEM_W[0], STEM_W[1])[x % 2] if r.random() < 0.6 else pal[1])
        for x in range(0, 16):
            h = 2 + int(2.5 * math.sin((x + 1) / 16 * math.pi)) + r.randint(0, 1)
            for i in range(h):
                y = 14 - i
                k = 1 if i == 0 else (2 if r.random() < 0.6 else 3)
                if i == h - 1 and r.random() < 0.5:
                    k = 4
                put(img, x, y, pal[k] if r.random() < 0.85 else None)
        for cx, cy in ((3, 9), (7, 8), (11, 8), (14, 10), (5, 10)):
            for dx, dy, k in ((0, 0, 2), (-1, 0, 1), (1, 0, 1), (0, -1, 3), (0, 1, 1)):
                put(img, cx + dx, cy + dy, PINKP[k])
        for x in (1, 9, 12):
            put(img, x, 10, PINKP[2])
    elif name == "wild_marjoram":   # bushy upright reddish stems, oval leaves, pink flower clusters at the top
        pal = G_FRESH
        xs = (3, 6, 9, 12)
        if cut:
            stubs(img, xs, r, cols=STEM_R, hmax=4)
            for x in xs:
                leaf(img, (x, 15), (x - 2, 13), 2, pal, shape="oval")
                put(img, x + 1, 14, pal[2])
            return img
        for n, x in enumerate(xs):
            top = 3 + (n % 2) * 2 + r.randint(0, 1)
            lean = (-1, 0, 0, 1)[n]
            pts = curve(img, (x, 15), (x + lean, top + 1), lean * 0.5, ramp(STEM_R))
            for k in range(2, len(pts) - 2, 2):
                px_, py_ = pts[k]
                side = 1 if (k // 2 + n) % 2 else -1
                leaf(img, (px_, py_), (px_ + side * 2, py_ - 1), 1.8, pal, shape="oval", dim=-1 if side > 0 else 0)
            t = pts[-1]
            for dx, dy, k in ((0, 0, 2), (-1, 0, 2), (1, 0, 1), (0, -1, 3), (-1, -1, 2), (1, -1, 1)):
                put(img, t[0] + dx, t[1] + dy, ("8a3a6a", "c0609a", "e08cbc", "f6c0dc")[k])
    elif name == "yarrow":   # feathery fern-like leaves, flat white flower plates on stiff stems
        pal = G_FRESH
        if cut:
            stubs(img, (4, 8, 12), r, cols=("5a6a3a", "6a7a44", "d4d8b0"))
            for x in (2, 6, 10, 14):
                feather(img, x, 3 + r.randint(0, 1), 0, pal, r)
            return img
        for x, lean in ((1, -1), (3, 0), (5, 1), (8, 0), (11, 1), (13, -1), (15, 1)):
            feather(img, x, 5 + r.randint(0, 3), lean, pal, r, dim=-1 if x in (3, 8, 13) else 0)
        for x, top in ((6, 3), (10, 4)):
            line(img, x, 15, x, top + 1, ramp(STEM_G))
            for dx in range(-2, 3):
                put(img, x + dx, top, WHITE[2] if dx < 1 else WHITE[1])
            for dx in (-1, 0, 1):
                put(img, x + dx, top - 1, WHITE[3] if dx < 1 else WHITE[2])
            put(img, x - 1, top, "d8d0b0"); put(img, x + 1, top + 1, STEM_G[2]); put(img, x - 1, top + 1, STEM_G[2])
    elif name == "chamomile":   # fine feathery leaves, small white daisies with yellow domed centres
        pal = G_YEL
        if cut:
            stubs(img, (3, 7, 10, 13), r, cols=("5a7a2a", "6c8c34", "c8d498"), hmax=3)
            for x in (5, 12):
                feather(img, x, 3, 0, pal, r)
            return img
        stems = ((2, 5, -1), (5, 3, 0), (8, 6, 0), (11, 2, 1), (14, 5, 1))
        for x, top, lean in stems:
            pts = curve(img, (x, 15), (x + lean, top + 1), lean * 0.4, ramp(STEM_G))
            for k in range(3, len(pts) - 2, 3):
                put(img, pts[k][0] - 1, pts[k][1], pal[3]); put(img, pts[k][0] + 1, pts[k][1] - 1, pal[2])
        for x, top, lean in stems:
            daisy(img, x + lean, top)
            put(img, x + lean, top - 1, "f6dc5a")   # the yellow centre domes up above the petals
            put(img, x + lean, top, "e0b428")
        for x in (1, 6, 10, 13):
            feather(img, x, 4, 0, pal, r, dim=-1)
    elif name == "water_mint":   # square stems, toothed dark green leaves in pairs, lilac flower globes
        pal = G_GLOSS
        xs = (4, 8, 12)
        if cut:
            stubs(img, xs, r, cols=("4a3a40", "6a5058", "c8c0a0"), hmax=3)
            for x in xs:
                leaf(img, (x, 15), (x - 2, 13), 2.2, pal, shape="oval")
                leaf(img, (x, 15), (x + 2, 13), 2.2, pal, shape="oval", dim=-1)
            return img
        for n, x in enumerate(xs):
            top = 3 + (n % 2) * 2
            line(img, x, 15, x, top + 2, ramp(("4a3040", "5a4048", STEM_G[1])))
            for y in range(13, top + 2, -3):   # opposite pairs of toothed leaves
                leaf(img, (x, y), (x - 3, y - 1), 2.2, pal, shape="oval", dim=0)
                leaf(img, (x, y), (x + 3, y - 1), 2.2, pal, shape="oval", dim=-1)
                put(img, x - 3, y, pal[1]); put(img, x + 2, y + 1, pal[0])
            globe(img, x, top, 1.3, LILAC, r)
    elif name == "sorrel":   # arrow-shaped leaves low down, slender stalks with rusty-red seed spikes
        pal = G_FRESH
        if cut:
            stubs(img, (5, 10), r, cols=("6a3a28", "8a4a30", "c8a080"), hmax=3)
            for x, dx in ((3, -1), (8, 1), (12, 1)):
                leaf(img, (x, 15), (x + dx, 12), 2.4, pal, shape="arrow")
            return img
        for x, dx in ((2, -1), (5, 1), (10, -1), (13, 1)):
            leaf(img, (x, 15), (x + dx * 2, 9 + r.randint(0, 2)), 3, pal, bend=-dx * 0.5, shape="arrow", rib=True,
                 dim=-1 if dx > 0 else 0)
        for x, top in ((7, 2), (11, 4)):
            line(img, x, 15, x, top + 4, ramp(STEM_R))
            for i in range(5):
                y = top + i
                put(img, x, y, ("a8482a", "8a3420", "c05a30")[i % 3])
                if i % 2 == 0:
                    put(img, x - 1, y + 1, "b44e2c"); put(img, x + 1, y, "7a2c1c")
    elif name == "sage":   # hand-drawn: woolly grey-green oblong leaves, violet flower spikes
        return grid(SAGE_CUT if cut else SAGE, SAGE_PAL)
    elif name == "rosemary":   # upright woody shrublet of needle leaves, small pale blue flowers
        pal = ("2c4430", "3e5a40", "56745a", "7a9a7a", "a4c0a0")
        xs = ((3, -1), (6, 0), (9, 0), (12, 1))
        if cut:
            stubs(img, (3, 6, 9, 12), r, cols=STEM_W, hmax=5)
            for x in (4, 10):
                put(img, x, 12, pal[3]); put(img, x + 1, 11, pal[2]); put(img, x - 1, 11, pal[3])
            return img
        for n, (x, lean) in enumerate(xs):
            top = 1 + (n % 2) * 2 + r.randint(0, 1)
            pts = curve(img, (x, 15), (x + lean * 2, top), lean, ramp(STEM_W))
            for k, (px_, py_) in enumerate(pts[2:], 2):   # needles splaying up on both sides
                if k % 1 == 0:
                    put(img, px_ - 1, py_ - (k % 2), pal[3] if k % 2 else pal[2])
                    put(img, px_ + 1, py_ - ((k + 1) % 2), pal[1] if k % 2 else pal[2])
            put(img, pts[-1][0], pts[-1][1] - 1, pal[4])
        for x, y in ((2, 7), (7, 5), (10, 8), (13, 6), (5, 10)):
            put(img, x, y, "b4c8ee"); put(img, x + 1, y, "8ca4d8")
    elif name == "bog_myrtle":   # small woody shrub, narrow grey-green leaves, brown-orange catkins
        pal = ("3e4c38", "56684c", "728666", "92a684", "b4c4a4")
        if cut:
            stubs(img, (4, 8, 12), r, cols=("5a3a2a", "6e4a34", "b89c78"), hmax=5)
            for x in (4, 8, 12):
                put(img, x - 1, 11, pal[3]); put(img, x + 1, 12, pal[2])
            return img
        for n, (x, top, lean) in enumerate(((3, 4, -1), (7, 1, 0), (12, 3, 1))):
            pts = curve(img, (x, 15), (x + lean * 2, top + 2), lean * 0.8, ramp(("4a2e24", "5e3a2a", "74503a")))
            for k in range(3, len(pts) - 1, 3):
                px_, py_ = pts[k]
                side = 1 if (k // 3 + n) % 2 else -1
                leaf(img, (px_, py_), (px_ + side * 3, py_ - 3), 1.5, pal, bend=side * 0.4, shape="lance",
                     dim=-1 if side > 0 else 0)
            t = pts[-1]
            for i in range(3):   # upright catkins clustered at the twig end
                put(img, t[0], t[1] - 1 - i, ("8a4a22", "b4622a", "d88a3a")[i])
                put(img, t[0] + 1, t[1] - i, ("6a3418", "9a5224", "c0702c")[i])
            put(img, t[0] - 1, t[1] - 1, "c0702c")
    elif name == "meadowsweet":   # tall, dark green toothed leaves, frothy cream flower plumes
        pal = G_GLOSS
        if cut:
            stubs(img, (5, 10), r, cols=("5a3a30", "7a4c3a", "d4c8a0"), hmax=4)
            for x, dx in ((3, -1), (8, 1), (13, 1)):
                leaf(img, (x, 15), (x + dx * 2, 12), 2.4, pal, shape="oval", rib=True)
            return img
        for x, top in ((5, 4), (10, 3)):
            line(img, x, 15, x, top + 1, ramp(("6a3a32", "7a4a3a", STEM_G[2])))
        for x, y, side in ((5, 12, -1), (5, 9, 1), (10, 13, 1), (10, 10, -1), (10, 7, 1)):
            leaf(img, (x, y), (x + side * 4, y - 1), 2.4, pal, bend=side * -0.6, shape="lance", rib=True,
                 dim=-1 if side > 0 else 0)
        for cx, cy, w in ((5, 3, 2), (10, 2, 2), (7, 4, 1), (12, 4, 1), (3, 5, 1)):   # frothy cream plumes
            for dy in range(-1, 3):
                for dx in range(-w, w + 1):
                    if abs(dx) + abs(dy) <= w + 1 and r.random() < 0.8:
                        k = 3 if dx + dy < 0 else 2 if dx + dy < 2 else 1
                        put(img, cx + dx, cy + dy, ("b8ac80", "d8cca0", "efe6c4", "fcf8e6")[k])
    elif name == "hops":   # a bine twining up a stick, broad lobed leaves, pale green papery cones
        pal = G_FRESH
        for y in range(0 if not cut else 9, 16):   # the stick
            put(img, 8, y, "8a6a44" if y % 4 else "6e5232")
        if cut:
            put(img, 8, 8, "b49870")
            stubs(img, (6, 10), r, cols=("4a6a2a", "5a7a34", "c4d09c"), hmax=3)
            leaf(img, (6, 14), (3, 12), 2.6, pal, shape="round")
            leaf(img, (10, 14), (13, 12), 2.6, pal, shape="round", dim=-1)
            return img
        for y in range(15, 0, -1):   # the bine spirals: in front left, behind right
            ph = (15 - y) % 4
            x = (7, 7, 9, 9)[ph]
            put(img, x, y, (STEM_G[1], STEM_G[2], STEM_G[0], STEM_G[0])[ph])
        for x, y, side in ((7, 12, -1), (9, 9, 1), (7, 6, -1), (9, 3, 1)):   # broad three-lobed leaves
            leaf(img, (x, y), (x + side * 4, y - 1), 3.6, pal, shape="round", rib=True, dim=-1 if side > 0 else 0)
            put(img, x + side * 3, y - 3, pal[3] if side < 0 else pal[2])   # the side lobe
        for x, y in ((12, 12), (3, 9), (12, 5), (4, 2)):   # papery cones hanging on short stalks
            put(img, x, y - 1, STEM_G[1])
            for dx, dy, c in ((0, 0, "e2f0b4"), (-1, 1, "d0e6a0"), (0, 1, "b8d488"), (1, 1, "8eae5e"), (-1, 2, "b8d488"),
                              (0, 2, "a0c070"), (1, 2, "7a9a4c"), (0, 3, "8eae5e")):
                put(img, x + dx, y + dy, c)
    elif name == "wild_mustard":   # tall stalk, bright yellow flower clusters, thin seed pods
        pal = G_FRESH
        if cut:
            stubs(img, (5, 10), r, hmax=4)
            for x in (4, 11):
                lobed(img, (x, 15), (x - 2, 12), 2.6, pal, r)
            return img
        for x, dx in ((3, -1), (12, 1)):
            lobed(img, (x, 15), (x + dx * 3, 10), 3, pal, r, bend=-dx)
        for x, top, lean in ((6, 2, -1), (9, 1, 1), (11, 4, 1)):
            pts = curve(img, (x, 15), (x + lean, top + 4), lean * 0.4, ramp(STEM_G))
            raceme(img, x + lean, top, 8, r, lean=lean)
    return img


FRESH = {   # herb -> (palette, leaf shape or a special painter) for the new growth after cutting
    "wild_thyme": (G_GREY, "dots"), "wild_marjoram": (G_FRESH, "oval"), "yarrow": (G_FRESH, "feather"),
    "chamomile": (G_YEL, "feather"), "water_mint": (G_GLOSS, "oval"), "sorrel": (G_FRESH, "arrow"),
    "rosemary": (("2c4430", "3e5a40", "56745a", "7a9a7a", "a4c0a0"), "needles"),
    "bog_myrtle": (("3e4c38", "56684c", "728666", "92a684", "b4c4a4"), "lance"), "meadowsweet": (G_GLOSS, "lance"),
    "hops": (G_FRESH, "round"), "wild_mustard": (G_FRESH, "spoon"),
}


def herb(name, cut=False):
    img = _herb(name, cut)
    if not cut or name not in FRESH:
        return img
    pal, shape = FRESH[name]
    pal = shift(pal, 1)   # new growth is brighter
    r = rand(name + "_fresh")
    under = new()
    for x in (1, 5, 10, 14):
        x += r.choice((0, 1))
        hgt = r.randint(2, 4)
        if shape == "feather":
            feather(under, x, hgt + 1, r.choice((-1, 1)), pal, r)
        elif shape == "dots":   # thyme: tiny paired leaves on short new shoots
            for i in range(hgt):
                put(under, x, 15 - i, pal[1 if i == 0 else 2])
                put(under, x + (1 if i % 2 else -1), 14 - i, pal[3 if i % 2 else 4])
        elif shape == "needles":
            for i in range(hgt + 1):
                put(under, x, 15 - i, pal[1])
                put(under, x - 1, 14 - i, pal[3]); put(under, x + 1, 14 - i, pal[2])
        else:
            w = 2.6 if shape in ("oval", "round", "spoon", "arrow") else 1.8
            leaf(under, (x, 15), (x - 2, 15 - hgt), w, pal, shape=shape, rib=w > 2)
            leaf(under, (x, 15), (x + 2, 16 - hgt), w, pal, shape=shape, rib=w > 2, dim=-1)
    return Image.alpha_composite(under, img)


# ============================== items ==============================
def ell(cx, cy, rx, ry):
    return {(x, y) for y in range(16) for x in range(16) if ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2 <= 1.0}


def seg(x0, y0, x1, y1, w=1.0):
    """Mask of a thick segment."""
    out = set()
    L2 = (x1 - x0) ** 2 + (y1 - y0) ** 2 or 1
    for y in range(16):
        for x in range(16):
            t = max(0, min(1, ((x - x0) * (x1 - x0) + (y - y0) * (y1 - y0)) / L2))
            if math.hypot(x - (x0 + t * (x1 - x0)), y - (y0 + t * (y1 - y0))) <= w / 2 + 0.2:
                out.add((x, y))
    return out


def leafmask(base, tip, w, shape="lance", bend=0.0):
    tmp = new()
    leaf(tmp, base, tip, w, ("000001",) * 5, bend=bend, shape=shape)
    return {(x, y) for y in range(16) for x in range(16) if tmp.getpixel((x, y))[3]}


def shade(img, mask, pal, outline=True, grad=0.9, spec=True):
    """Fill a mask with a 5-ramp (D, d, m, l, h): light from the upper left, a dark outline around (pal[0] or darker)."""
    if not mask:
        return
    xs = [p[0] for p in mask]
    ys = [p[1] for p in mask]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    span = max(1, (x1 - x0) + (y1 - y0))
    for (x, y) in mask:
        t = ((x - x0) + (y - y0)) / span          # 0 at the upper left .. 1 at the lower right
        k = 3 - t * 2 * grad
        if (x - 1, y) not in mask or (x, y - 1) not in mask:
            k += 0.8
        if (x + 1, y) not in mask or (x, y + 1) not in mask:
            k -= 0.9
        k = int(round(max(1, min(3.6, k))))
        put(img, x, y, pal[k])
    if spec:   # a highlight speck near the upper-left of the body
        inner = [(x, y) for (x, y) in mask if (x - 1, y) in mask and (x, y - 1) in mask and (x + 1, y) in mask
                 and (x, y + 1) in mask]
        if inner:
            hx, hy = min(inner, key=lambda p: (p[0] - x0) + (p[1] - y0))
            put(img, hx, hy, pal[4])
    if outline:
        oc = pal[0] if outline is True else outline
        for (x, y) in mask:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                q = (x + dx, y + dy)
                if q not in mask:
                    put(img, q[0], q[1], oc)


def outline_layer(img, mask, c):
    for (x, y) in mask:
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in mask:
                put(img, x + dx, y + dy, c)


# item palettes (D outline, d, m, l, h)
P_LEAF = ("1e3a18", "2e5a24", "3e7a30", "5a9c40", "86c060")
P_GLOSS = ("142e16", "214a24", "2e6630", "448a40", "72b060")
P_BLUEG = ("223c38", "3a5e58", "567e72", "76a092", "a6c8b8")
P_GREYG = ("2e3a2a", "4e5e46", "6a7c5e", "8a9e7c", "b0c2a2")
P_YELG = ("2e4a16", "4a7020", "6a9a2c", "8eba40", "bcdc6c")
P_WHITE = ("6a6458", "b8b2a2", "dcd8cc", "f2eee4", "ffffff")
P_CREAM = ("6a5434", "c0a878", "dccaa0", "eee2c0", "fcf6e2")
P_GOLD = ("5a3414", "9a5e24", "c48638", "e0aa58", "f6d48a")
P_PURPLE = ("3a1838", "6a2a5c", "944a84", "b46aa4", "d498c4")
P_BROWN = ("2e1c10", "5a3820", "7a5030", "9a6c44", "c09468")
P_DARK = ("100c0a", "2a2420", "403830", "5a5046", "847868")
P_TAN = ("4a3018", "9a7046", "bc9262", "d8b282", "eed4a8")
P_RED = ("4a0c08", "8a1a10", "b82a18", "dc4828", "f48a5a")
P_PINK = ("6a3828", "b07a5e", "d29c7c", "e8bc9c", "f8dcc4")
P_FIG = ("2a1610", "4e2c1e", "6a3e28", "8a5638", "b07a54")
P_TEA = ("141e10", "2a3420", "3e4a2c", "56603a", "7a8452")


GRAINS = {   # small shaded sprites (rows, letters 0..4 into the palette)
    "round2": ["32", "21"],
    "round3": [".3.", "321", ".1."],
    "corn": ["432", "321", "210"],
    "rib": ["32", "21", "10"],
    "rice": ["432", "221"],
    "sesame": ["3", "2", "1"],
}


def grain_heap(img, cx, base, w, h, pal, kind, rng, loose=(), gap=0):
    """A heap built from single grains: rows from the top down so lower grains overlap, then an outline round it."""
    spr = GRAINS[kind]
    gh, gw = len(spr), len(spr[0])
    mask = set()
    for y in range(base - h, base + 1):
        half = (w / 2) * math.sqrt(max(0.0, (y - (base - h) + 0.5) / (h + 0.5)))
        for x in range(int(cx - half), int(cx + half) + 1):
            mask.add((x, y))
    for (x, y) in mask:   # the shadowed body between the grains
        put(img, x, y, pal[1])
    for y in range(base - h, base + 1, max(1, gh - 1)):
        half = (w / 2) * math.sqrt(max(0.0, (y - (base - h) + 0.5) / (h + 0.5)))
        x = int(cx - half) + (y % 2)
        while x <= cx + half - gw + 1:
            jx, jy = x + rng.choice((0, 0, 1)), y + rng.choice((0, 0, -1))
            for j, row in enumerate(spr):
                for i, ch in enumerate(row):
                    if ch != "." and (jx + i, jy + j) in mask:
                        put(img, jx + i, jy + j, pal[int(ch)])
            x += gw + gap + rng.choice((0, 0, 1))
    for (lx, ly) in loose:   # a few spilled grains
        for j, row in enumerate(spr):
            for i, ch in enumerate(row):
                if ch != ".":
                    put(img, lx + i, ly + j, pal[int(ch)])
                    mask.add((lx + i, ly + j))
    outline_layer(img, mask, pal[0])


def heap(img, cx, base, w, h, pal, rng, grain=("seed", 1), outline=True):
    """A small heap of grains: a mound silhouette shaded, then single grains picked out on it."""
    m = {(x, y) for y in range(16) for x in range(16)
         if y <= base and y >= base - h * (1 - ((x - cx) / (w / 2)) ** 2) and abs(x - cx) <= w / 2}
    shade(img, m, pal, outline=outline, grad=0.6, spec=False)
    for (x, y) in sorted(m):
        if rng.random() < 0.38 and (x, y - 1) in m:
            put(img, x, y, pal[4 if (x + y) % 3 == 0 and y < base - 1 else 1])
    return m


def scatter(img, pts, pal, size=1):
    for (x, y) in pts:
        put(img, x, y, pal[3]); put(img, x + 1, y, pal[2]); put(img, x, y + 1, pal[2]); put(img, x + 1, y + 1, pal[1])
        if size > 1:
            put(img, x, y - 1, pal[1]); put(img, x - 1, y, pal[1])


def item(name):
    img = new()
    r = rand("item_" + name)
    if name == "wild_garlic":   # a bunch of broad ramsons leaves with small white bulbs
        for b, t, dm in (((6, 14), (2, 3), 0), ((7, 14), (9, 1), 0), ((7, 14), (13, 4), -1)):
            shade(img, leafmask(b, t, 4, "lance", bend=0.6), shift(P_GLOSS, dm))
            poly(img, (b, ((b[0] + t[0]) // 2, (b[1] + t[1]) // 2), t), shift(P_GLOSS, dm)[4])
        shade(img, ell(6, 13.5, 1.6, 1.6), P_WHITE)
        shade(img, ell(8.5, 14, 1.4, 1.2), P_WHITE)
    elif name == "garlic":   # a white-purple papery bulb with a dry neck
        shade(img, ell(7.5, 10, 5.3, 4.6), ("4e3a48", "a898a4", "d8ccd4", "f2ece8", "ffffff"))
        for x0, y0, x1, y1 in ((5, 7, 4, 12), (7.5, 6, 7.5, 13), (10, 7, 11, 12)):   # clove seams
            line(img, x0, y0, x1, y1, "b4a0b4")
        for p in ((5, 13), (9, 13), (10, 12), (6, 12)):
            put(img, p[0], p[1], "9a6a96")
        shade(img, seg(7.5, 2, 7.5, 5, 1.6), ("5a4a3a", "a8987a", "c8b894", "dccca8", "eee2c4"))
        for x in (5, 7, 9, 11):
            put(img, x, 15, "9a8a6a")
    elif name == "wild_onion":   # a thin bulb with long green tops
        shade(img, seg(9, 1, 6, 9, 1.2), P_BLUEG)
        shade(img, seg(12, 2, 7, 9, 1.2), shift(P_BLUEG, -1))
        shade(img, ell(6, 11.5, 2.2, 2.6), ("4a3a2a", "b8a08a", "dccab0", "f0e4cc", "ffffff"))
        put(img, 6, 9, "d8b8c8")
        for x, y in ((4, 15), (6, 15), (8, 15)):
            put(img, x, y, "a08a6a")
    elif name == "onion":   # golden-brown bulb, root tuft, dry neck
        shade(img, ell(7.5, 9.5, 5.3, 4.8), P_GOLD)
        for x0, y0, x1, y1 in ((5, 6, 4, 12), (9, 5.5, 11, 12)):
            line(img, x0, y0, x1, y1, "b07030")
        shade(img, seg(7.5, 1, 8.5, 4.5, 1.6), ("4a2c10", "8a5a28", "b07a3a", "c89a58", "e0bc7c"))
        for x, y in ((6, 15), (7, 15), (9, 15), (8, 14), (5, 14)):
            put(img, x, y, "d8c49a")
        put(img, 5, 7, "fbe2a8")
    elif name == "wild_cabbage":   # a few loose blue-grey waxy leaves
        shade(img, leafmask((7, 14), (2, 3), 6, "round", bend=-1), shift(P_BLUEG, -1))
        shade(img, leafmask((8, 14), (13, 4), 6, "round", bend=1), P_BLUEG)
        for pts in (((7, 14), (5, 10), (3, 5)), ((8, 14), (10, 10), (12, 6))):
            poly(img, pts, "c4dcd0")
    elif name == "cabbage":   # a round green head with veined wrapper leaves
        shade(img, ell(7.5, 8.5, 6.4, 6.0), ("1e3a24", "4a7a56", "6e9e76", "94c096", "c8e4c0"))
        for pts in (((7, 3), (5, 6), (4, 11)), ((8, 3), (10, 6), (11, 11)), ((7.5, 4), (7.5, 9)),
                    ((3, 8), (5, 13)), ((12, 7), (11, 13))):
            poly(img, pts, "c6e2bc")
        shade(img, leafmask((4, 15), (1, 8), 4, "round", bend=-1), P_BLUEG, spec=False)
        shade(img, leafmask((11, 15), (14, 8), 4, "round", bend=1), shift(P_BLUEG, -1), spec=False)
    elif name in ("wild_leek", "leek"):   # white shaft, dark blue-green leaves fanned out at the top
        thin = name == "wild_leek"
        w = 1.4 if thin else 2.4
        for b, t, dm in (((8, 7), (3, 1), -1), ((8, 7), (8, 0), 0), ((9, 7), (14, 2), -1)):
            shade(img, leafmask(b, t, w + 0.6, "strap"), shift(P_BLUEG, dm), spec=False)
        shade(img, seg(8.5, 7, 6.5, 13, w), ("5a5a44", "b4c49a", "d8e4c0", "f2f4e4", "ffffff"))
        for x, y in ((5, 15), (6, 15), (7, 14), (7, 15)):
            put(img, x, y, "a89a7a")
    elif name in ("wild_parsnip", "parsnip"):   # a pale tapering root with leaf stubs
        thin = name == "wild_parsnip"
        root = seg(10, 4, 3, 14, 2.2 if thin else 4.2) | (ell(10, 5, 1.8, 1.8) if not thin else set())
        shade(img, root, P_CREAM)
        for t in (0.35, 0.6, 0.8):
            x, y = 10 - 7 * t, 4 + 10 * t
            put(img, x + (1 if not thin else 0), y, "c0a878")
        for b, t in (((10, 4), (12, 0)), ((10, 4), (14, 2)), ((11, 4), (9, 0))):
            line(img, b[0], b[1] - 1, t[0], t[1], P_YELG[3] if not thin else P_YELG[2])
        if thin:
            put(img, 2, 15, "a8946a")
    elif name in ("wild_celery", "celery"):   # ribbed pale-green stalks bunched, leaves at the top
        thin = name == "wild_celery"
        stalks = ((4, 14, 8, 3), (7, 14, 9, 2), (9, 14, 11, 3)) if not thin else ((5, 14, 8, 3), (8, 14, 11, 4))
        pal = ("2e4a1e", "7aa456", "a0c87a", "c4e2a0", "e2f2cc") if not thin else ("1e3a1a", "3e6a2c", "5a8a3c", "7aaa50", "a2c878")
        for x0, y0, x1, y1 in stalks:
            m = seg(x0, y0, x1, y1, 2.0 if not thin else 1.2)
            shade(img, m, pal, spec=False)
        for x0, y0, x1, y1 in stalks:
            for t, k in ((0.0, "0"), (1.0, "1")):
                pass
            leafy = (x1, y1 - 1)
            for dx, dy, c in ((-1, 0, P_GLOSS[3]), (1, 0, P_GLOSS[2]), (0, -1, P_GLOSS[4]), (1, -1, P_GLOSS[3]),
                              (-1, -1, P_GLOSS[2]), (0, 0, P_GLOSS[3])):
                put(img, leafy[0] + dx, leafy[1] + dy, c)
        if not thin:
            for x0, y0, x1, y1 in stalks:
                line(img, x0, y0 - 1, (x0 + x1) // 2, (y0 + y1) // 2, "dff0c4")
    elif name in ("wild_turnip", "turnip"):   # white globe with a purple top and greens
        thin = name == "wild_turnip"
        if thin:
            for b, t in (((8, 8), (5, 1)), ((8, 8), (11, 2))):
                shade(img, leafmask(b, t, 2.4, "lance"), P_LEAF, spec=False)
            shade(img, ell(8, 11, 2.6, 2.4) | seg(8, 12, 9, 15, 1), ("4a3a40", "c4b8bc", "e0d8d4", "f4f0ec", "ffffff"))
            put(img, 7, 9, "a04a8a"); put(img, 8, 9, "b862a0"); put(img, 9, 9, "8a3c76")
        else:
            for b, t, dm in (((7, 6), (3, 0), 0), ((8, 6), (9, 0), 0), ((8, 6), (13, 1), -1)):
                shade(img, leafmask(b, t, 3, "lance"), shift(P_LEAF, dm), spec=False)
            body = ell(7.5, 10, 5, 4.2) | {(7, 15), (8, 15)}
            shade(img, body, ("4a3a44", "c8bcc0", "e4dcd8", "f6f2ee", "ffffff"))
            for (x, y) in body:   # purple flush over the top half, fading down
                if y <= 8 or (y == 9 and (x + y) % 2):
                    put(img, x, y, "b862a0" if (x < 8 and y > 6) else "8a3c76" if x > 9 else "a04a8a")
            put(img, 5, 7, "d890c4")
    elif name in ("wild_lettuce", "lettuce"):
        if name == "wild_lettuce":   # a few lobed grey-green leaves
            for b, t, dm in (((6, 15), (2, 3), 0), ((8, 15), (12, 2), -1)):
                m = leafmask(b, t, 4, "spoon")
                for k in (0.4, 0.65):   # lobe bites
                    m.discard((round(b[0] + (t[0] - b[0]) * k) + (2 if t[0] > b[0] else -2), round(b[1] + (t[1] - b[1]) * k)))
                shade(img, m, shift(P_GREYG, dm), spec=False)
                poly(img, (b, t), "c8d6b4")
        else:   # loose leafy head
            HEADP = ("2a4a16", "5a8a2c", "7aaa3c", "a0cc58", "cce888")
            for b, t, dm in (((7, 14), (1, 6), -1), ((8, 14), (14, 6), -1), ((7, 14), (5, 2), 0), ((8, 14), (11, 2), 0)):
                shade(img, leafmask(b, t, 6, "spoon"), shift(HEADP, dm), spec=False)
            shade(img, ell(7.5, 10, 3.2, 3.6), HEADP)
            for x, y in ((2, 6), (5, 2), (11, 2), (13, 6), (4, 4), (12, 4)):
                put(img, x, y, HEADP[4])
            poly(img, ((7, 14), (5, 8)), HEADP[4]); poly(img, ((8, 14), (11, 7)), HEADP[3])
    elif name == "broad_beans":   # a fat closed pod and one split open showing three pale beans in its white lining
        POD = ("1e3a14", "4e7a2c", "6e9e3c", "92c058", "c0e08a")
        m = seg(2, 9, 9, 2, 3.6)
        shade(img, m, POD)
        for (x, y) in ((4, 7), (6, 5), (8, 3)):   # bean bulges
            put(img, x, y, POD[3])
        put(img, 9, 1, "6a5a3a")
        shell = seg(4, 14, 14, 6, 4.6)
        shade(img, shell, shift(POD, -1), spec=False)
        lining = seg(5, 13, 13, 7, 2.4)
        for (x, y) in lining:
            put(img, x, y, "eef0dc" if (x + y) % 3 else "d8dcc4")
        for cx, cy in ((6.5, 12), (9, 10), (11.5, 8)):   # the beans
            shade(img, ell(cx, cy, 1.3, 1.1), ("5a6a30", "a8bc6a", "c4d68a", "dceaa8", "f2f8d0"), outline=False, spec=False)
            put(img, round(cx) - 1, round(cy) - 1, "f2f8d0")
            put(img, round(cx) + 1, round(cy) + 1, "6a7a38")
    elif name == "dill_seeds":   # a small heap of ribbed brown seeds
        grain_heap(img, 7.5, 14, 13, 7, ("2a1c10", "5a3c20", "8a6638", "b08a52", "d0b078"), "rib", r,
                   loose=((1, 13), (13, 13)))
    elif name == "dill":   # a feathery blue-green sprig
        line(img, 3, 15, 10, 4, ramp(("3a5a40", "567e60", "6a9472")))
        r2 = rand("dillsprig")
        for t in range(2, 11):
            x, y = 3 + t * 7 / 11, 15 - t
            for side in (-1, 1):
                L = 3 if t < 8 else 2
                for k in range(1, L + 1):
                    put(img, x + side * k, y - k * (0.6 if side > 0 else 0.3) - (k > 1), P_BLUEG[2] if side < 0 else P_BLUEG[1])
                    if r2.random() < 0.3:
                        put(img, x + side * k, y - k, P_BLUEG[3])
        flat_umbel(img, 11, 2, 5, YEL, r2, stem="6a9472")
    elif name == "rapeseed":   # heap of tiny round blue-black seeds with a dull sheen
        grain_heap(img, 7.5, 14, 13, 7, ("0c0a0e", "2a2430", "4a4252", "6e6478", "a69cb2"), "round2", r,
                   loose=((2, 14), (12, 13), (14, 15)))
    elif name == "mustard_seeds":   # heap of small yellow-brown round seeds
        grain_heap(img, 7.5, 14, 13, 7, ("3a240c", "7a4c18", "b07c28", "d8a840", "f4d47a"), "round2", r,
                   loose=((1, 14), (13, 13), (14, 15)))
    elif name in ("thyme", "marjoram", "rosemary", "sage_leaves", "mint_leaves", "sorrel_leaves", "bog_myrtle_leaves",
                  "yarrow_leaves"):
        herb_item(img, name, r)
    elif name == "chamomile_flowers":   # a little heap of daisy heads: white petal rings round domed yellow centres
        heads = ((8, 4), (5, 7), (11, 7), (3, 11), (8, 10), (13, 11), (5, 14), (11, 14), (8, 13))
        drawn = set()
        for (x, y) in heads:   # back to front: later heads overlap earlier ones
            for dx, dy, c in ((-1, -2, "fbfbf4"), (0, -2, "f4f4ea"), (-2, -1, "f4f4ea"), (1, -1, "e2e2d6"), (-2, 0, "e8e8dc"),
                              (1, 0, "d4d4c6"), (-1, 1, "c8c8b8"), (0, 1, "b4b4a2"), (-1, -1, "f2c42c"), (0, -1, "e0a820"),
                              (-1, 0, "e8b424"), (0, 0, "c88e18")):
                put(img, x + dx, y + dy, c)
                drawn.add((x + dx, y + dy))
            put(img, x - 1, y - 1, "fae070")
        outline_layer(img, drawn, "6a6a52")
    elif name == "meadowsweet_flowers":   # a frothy cream plume cut on its stem
        line(img, 4, 15, 8, 8, ramp(("5a3428", "7a4a38", "8a5a44")))
        for cx, cy, rr in ((8, 6, 3.4), (11, 4, 2.4), (5, 5, 2.2), (9, 2, 2)):
            m = ell(cx, cy, rr, rr * 0.85)
            for (x, y) in m:
                if r.random() < 0.85:
                    k = 4 if (x - cx) + (y - cy) < -1.5 else 3 if (x - cx) + (y - cy) < 0.5 else 2 if r.random() < 0.7 else 1
                    put(img, x, y, ("8a7a50", "c8b888", "e2d8b0", "f2ecd4", "fffcf0")[k])
        shade(img, leafmask((6, 12), (2, 10), 2.4, "lance"), P_GLOSS, spec=False)
    elif name == "hop_cones":   # two pale green papery cones of overlapping bracts on a stalk, a leaf
        line(img, 9, 3, 12, 0, "4e6e24")
        shade(img, leafmask((11, 2), (15, 1), 2.6, "oval"), P_LEAF, spec=False)
        for cx, cy, dm in ((5.0, 10.0, -1), (9.5, 7.5, 0)):
            pal = shift(("2e4a14", "6a9a34", "8cba4c", "b4d878", "dcf0b0"), dm)
            m = ell(cx, cy, 2.6, 4.0) | {(round(cx), round(cy + 4.4))}
            for (x, y) in m:   # bracts: two-pixel scales in staggered rows, light top edge, dark lower edge
                band = (y - int(cy - 4)) // 2
                top = (y - int(cy - 4)) % 2 == 0
                seam = (x + band) % 3 == 0
                k = 3 if top and not seam else 2 if not seam else 1
                if x > cx + 1:
                    k -= 1
                if x < cx - 1 and top:
                    k += 1
                put(img, x, y, pal[max(1, min(4, k))])
            outline_layer(img, m, pal[0])
    elif name == "black_pepper":   # a little pile of wrinkled black peppercorns
        for cx, cy in ((7, 5), (4, 8), (10, 8), (7, 10), (2, 12), (5, 13), (9, 13), (13, 12), (12, 4)):
            m = ell(cx, cy, 1.6, 1.6)
            shade(img, m, ("080605", "221c18", "38302a", "564c44", "8a7e72"))
            put(img, cx + 1, cy, "1a1410")   # a wrinkle
    elif name == "cinnamon_bark":   # rolled quills, cut ends showing the spiral
        for (x0, y0, x1, y1, dm) in ((2, 11, 12, 4, -1), (4, 14, 14, 7, 0)):
            pal = shift(("3a1a0c", "7a3e1e", "9a5428", "b86e38", "d49058"), dm)
            m = seg(x0, y0, x1, y1, 3.2)
            shade(img, m, pal, spec=False)
            line(img, x0 + 0.5, y0 - 1, x1 - 0.5, y1 - 1, pal[3])   # the rolled edge
            ex, ey = x1, y1   # the cut end: a spiral
            put(img, ex, ey, pal[0]); put(img, ex - 1, ey, pal[3]); put(img, ex, ey + 1, pal[2])
    elif name == "ginger_root":   # knobbly tan rhizome
        m = seg(2, 11, 12, 8, 3.4) | ell(4, 7.5, 1.8, 2.2) | ell(9, 5, 1.6, 2.2) | ell(13, 10.5, 2, 1.8) | ell(7, 12.5, 1.6, 1.5)
        shade(img, m, P_TAN)
        for x, y in ((5, 9), (9, 9), (11, 8), (4, 6), (9, 4)):
            put(img, x, y, "a87e50")
        put(img, 9, 3, "d07c80"); put(img, 4, 6, "e0a090")
    elif name == "saffron_corm":   # round papery corm with a fibrous husk and a shoot tip
        m = ell(8, 10, 5, 4.2)
        shade(img, m, ("3a2a1a", "8a6a44", "a8865a", "c8a87a", "e4cca0"))
        for x0, y0, x1, y1 in ((5, 7, 4, 13), (8, 6, 8, 14), (11, 7, 12, 13), (6, 6, 6, 14)):
            line(img, x0, y0, x1, y1, "9a7a4e")
        for x, y in ((8, 5), (8, 4), (9, 3)):
            put(img, x, y, "d8d0b0")
        put(img, 8, 2, "8ab060")
        for x in (6, 8, 10):
            put(img, x, 15, "c8b08a")
    elif name == "saffron":   # a loose pinch of deep red threads, flared orange tips, pale style ends
        threads = (((3, 13), (10, 3), 1.5), ((5, 14), (13, 5), -1), ((4, 11), (14, 10), -1.5), ((7, 14), (9, 2), 2),
                   ((6, 13), (14, 13), -1))
        drawn = set()
        for k, (b, t, bend) in enumerate(threads):
            pts = curve(new(), b, t, bend, "000000")
            for i, q in enumerate(pts):
                c = ("8a100a", "a8140c", "c81c10", "e0301a")[(i + k) % 4 if i < len(pts) - 2 else 3]
                put(img, q[0], q[1], c)
                drawn.add(q)
            put(img, pts[-1][0], pts[-1][1], "f07a32")
            put(img, pts[0][0], pts[0][1], "f0c860")
        for (x, y) in drawn:   # a dark edge below/right so the threads read on any background
            for dx, dy in ((1, 0), (0, 1)):
                if (x + dx, y + dy) not in drawn:
                    put(img, x + dx, y + dy, "4a0806")
    elif name == "rice":   # a heap of white grains
        grain_heap(img, 7.5, 14, 14, 8, ("6e685c", "a8a294", "d8d4c8", "f2f0e8", "ffffff"), "rice", r,
                   loose=((0, 14), (13, 12)), gap=1)
    elif name == "sesame_seeds":   # small cream teardrop seeds in a low heap, a few spilled
        grain_heap(img, 7.5, 14, 12, 6, ("6a5434", "a88e5e", "dccaa0", "f2e4bc", "fff8e0"), "sesame", r,
                   loose=((1, 13), (14, 13)), gap=1)
    elif name == "dried_dates":   # dark brown wrinkled dates
        for cx, cy, a, dm in ((5, 10, 0.5, 0), (10, 7, -0.5, 0), (10, 12.5, 0.2, -1)):
            m = {(x, y) for y in range(16) for x in range(16)
                 if (((x - cx) * math.cos(a) + (y - cy) * math.sin(a)) / 3.6) ** 2
                 + ((-(x - cx) * math.sin(a) + (y - cy) * math.cos(a)) / 2.0) ** 2 <= 1}
            pal = shift(("1c0e08", "4a2414", "6a3418", "8a4a24", "b4703c"), dm)
            shade(img, m, pal)
            for (x, y) in m:   # wrinkles
                if (x * 3 + y * 2) % 5 == 0 and (x + 1, y) in m and (x - 1, y) in m:
                    put(img, x, y, pal[1])
    elif name == "dried_figs":   # squat brown figs, one split showing seeds
        shade(img, ell(5.5, 10, 4, 3.8) | {(5, 5), (6, 5), (5, 6)}, P_FIG)
        m = ell(10.5, 9.5, 4, 3.8)
        shade(img, m, shift(P_FIG, 0))
        inner = ell(10.5, 10, 2.3, 2.4)
        for (x, y) in inner:   # the split fig: pink-red flesh packed with pale seeds
            put(img, x, y, "a8343a" if (x + y) % 2 else "c8505a")
            if (x * 7 + y * 3) % 4 == 0:
                put(img, x, y, "f0d8a0")
        outline_layer(img, inner, "f2d8b4")
        put(img, 10, 5, "4e2c1e"); put(img, 11, 4, "6a4a2a")
    elif name == "tea_leaves":   # rolled dark green-brown tea leaves, a small heap
        heap(img, 8, 13, 12, 6, P_TEA, r)
        for x, y in ((4, 12), (7, 10), (10, 12), (9, 9), (12, 13), (6, 12)):
            line(img, x, y, x + 1, y - 1, "6a7442")
            put(img, x + 1, y, "1a2212")
        for x0, y0 in ((2, 14), (13, 14)):
            line(img, x0, y0, x0 + 1, y0 - 1, "56603a")
    return img


def herb_item(img, name, r):
    """Cut herb sprigs/leaves as items."""
    if name == "thyme":   # three woody sprigs of tiny grey-green leaves, a pink tip
        for x0, y0, x1, y1 in ((3, 15, 7, 3), (6, 15, 11, 4), (8, 15, 13, 7)):
            line(img, x0, y0, x1, y1, "6a5440")
            for t in range(2, 11):
                x = x0 + (x1 - x0) * t / 12
                y = y0 + (y1 - y0) * t / 12
                put(img, x - 1, y, "8ca07a" if t % 2 else "6a7e5a")
                put(img, x + 1, y - 1, "b0c49c" if t % 3 == 0 else "6a7e5a")
            put(img, x1, y1, "c87cb8"); put(img, x1 - 1, y1, "a8569a"); put(img, x1, y1 - 1, "e0a8d8")
        outline_layer(img, {(x, y) for y in range(16) for x in range(16) if get(img, x, y)}, "2e3424")
    elif name == "marjoram":   # a leafy sprig with oval leaves and a pink flower cluster
        line(img, 4, 15, 10, 3, ramp(("6a3428", "7a3c30", "8e5a3a")))
        for t, side in ((3, -1), (5, 1), (7, -1), (9, 1)):
            x, y = 4 + t * 0.6, 15 - t * 1.2
            shade(img, leafmask((x, y), (x + side * 3.5, y - 1.5), 2.4, "oval"), P_LEAF if side < 0 else shift(P_LEAF, -1),
                  spec=False)
        for dx, dy, c in ((0, 0, "c0609a"), (-1, 0, "e08cbc"), (1, 0, "a04a80"), (0, -1, "f6c0dc"), (1, -1, "c0609a"),
                          (-1, -1, "e08cbc"), (0, -2, "e08cbc")):
            put(img, 10 + dx, 2 + dy, c)
    elif name == "rosemary":   # a woody needled sprig
        line(img, 3, 15, 12, 1, ramp(STEM_W))
        for t in range(1, 15):
            x, y = 3 + t * 9 / 14, 15 - t
            put(img, x - 1, y - (t % 2), "7a9a7a" if t % 2 else "56745a")
            put(img, x + 1, y - ((t + 1) % 2), "3e5a40")
            if t % 3 == 0:
                put(img, x - 2, y - 1, "a4c0a0")
        outline_layer(img, {(x, y) for y in range(16) for x in range(16) if get(img, x, y)}, "1e2c20")
        put(img, 7, 6, "b4c8ee"); put(img, 10, 3, "b4c8ee")
    elif name == "sage_leaves":   # three woolly grey-green oblong leaves
        pal = ("3a4434", "6a7860", "8a987c", "aab89c", "d0dac4")
        for b, t, dm in (((5, 15), (2, 3), 0), ((7, 15), (9, 1), 0), ((8, 15), (14, 5), -1)):
            m = leafmask(b, t, 4.4, "oval")
            shade(img, m, shift(pal, dm))
            poly(img, (b, t), shift(pal, dm)[4])
            for (x, y) in m:   # woolly texture
                if (x * 5 + y * 3) % 7 == 0:
                    put(img, x, y, shift(pal, dm)[4])
    elif name == "mint_leaves":   # toothed dark green leaves on a square stem
        line(img, 8, 15, 8, 9, "5a4048")
        for b, t, dm in (((8, 10), (2, 5), 0), ((8, 10), (14, 5), -1), ((8, 9), (8, 1), 0)):
            m = leafmask(b, t, 5, "oval")
            shade(img, m, shift(P_GLOSS, dm))
            poly(img, (b, t), shift(P_GLOSS, dm)[4])
            for (x, y) in list(m):   # serrated edge: bite out every other edge pixel
                if (x + y) % 3 == 0 and sum((x + dx, y + dy) in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) <= 2:
                    put(img, x, y, shift(P_GLOSS, dm)[0])
    elif name == "sorrel_leaves":   # arrow-shaped leaves with reddish stalks
        for b, t, dm in (((5, 15), (4, 2), 0), ((9, 15), (12, 3), -1)):
            shade(img, leafmask((b[0] + (t[0] - b[0]) * 0.3, b[1] - 4), t, 5, "arrow"), shift(P_LEAF, dm))
            line(img, b[0], b[1], b[0] + (t[0] - b[0]) * 0.3, b[1] - 4, "8a3420")
            poly(img, ((b[0] + (t[0] - b[0]) * 0.3, b[1] - 5), t), shift(P_LEAF, dm)[4])
    elif name == "bog_myrtle_leaves":   # a twig of narrow grey-green leaves with an orange catkin
        line(img, 3, 15, 11, 3, ramp(("4a2e24", "5e3a2a", "74503a")))
        for t, side in ((3, -1), (5, 1), (7, -1), (9, 1), (11, -1)):
            x, y = 3 + t * 8 / 12, 15 - t
            shade(img, leafmask((x, y), (x + side * 4, y - 2), 2, "lance"), P_GREYG if side < 0 else shift(P_GREYG, -1),
                  spec=False)
        for i, c in enumerate(("8a4a22", "c0702c", "d88a3a", "a85a24")):
            put(img, 11 + (i > 1), 3 - i, c)
        put(img, 13, 1, "e8a050")
    elif name == "yarrow_leaves":   # feathery fern-like leaves
        for x0, y0, x1, y1, dm in ((4, 15, 6, 1, 0), (7, 15, 12, 2, -1)):
            line(img, x0, y0, x1, y1, P_LEAF[2 + dm])
            n = abs(y1 - y0)
            for t in range(1, n):
                x, y = x0 + (x1 - x0) * t / n, y0 + (y1 - y0) * t / n
                if t % 2 == 0:
                    put(img, x - 1, y, P_LEAF[3 + dm]); put(img, x - 2, y - 1, P_LEAF[4 + dm])
                    put(img, x + 1, y - 1, P_LEAF[2 + dm]); put(img, x + 2, y - 1, P_LEAF[3 + dm])
        outline_layer(img, {(x, y) for y in range(16) for x in range(16) if get(img, x, y)}, P_LEAF[0])


# ============================== build ==============================
CROPS = {
    "wild_garlic": lambda s: garlic(s, True), "garlic": garlic,
    "wild_onion": lambda s: onion(s, True), "onion": onion,
    "wild_cabbage": lambda s: cabbage(s, True), "cabbage": cabbage,
    "wild_leek": lambda s: leek(s, True), "leek": leek,
    "wild_parsnip": lambda s: parsnip(s, True), "parsnip": parsnip,
    "wild_celery": lambda s: celery(s, True), "celery": celery,
    "wild_turnip": lambda s: turnip(s, True), "turnip": turnip,
    "wild_lettuce": lambda s: lettuce(s, True), "lettuce": lettuce,
    "broad_bean": broad_bean, "dill": dill, "mustard": mustard, "ginger": ginger, "saffron": saffron, "rice": rice,
    "rapeseed": rapeseed,
}
WILD_PLANTS = ["wild_garlic", "wild_onion", "wild_cabbage", "wild_leek", "wild_parsnip", "wild_celery", "wild_turnip",
               "wild_lettuce"]
HERBS = ["wild_thyme", "wild_marjoram", "yarrow", "chamomile", "water_mint", "sorrel", "sage", "rosemary", "bog_myrtle",
         "meadowsweet", "hops", "wild_mustard"]
ITEMS = ["wild_garlic", "garlic", "wild_onion", "onion", "wild_cabbage", "cabbage", "wild_leek", "leek", "wild_parsnip",
         "parsnip", "wild_celery", "celery", "wild_turnip", "turnip", "wild_lettuce", "lettuce", "broad_beans",
         "dill_seeds", "dill", "mustard_seeds", "thyme", "marjoram", "yarrow_leaves", "chamomile_flowers", "mint_leaves",
         "sorrel_leaves", "sage_leaves", "rosemary", "bog_myrtle_leaves", "meadowsweet_flowers", "hop_cones",
         "black_pepper", "cinnamon_bark", "ginger_root", "saffron_corm", "saffron", "rice", "sesame_seeds",
         "dried_dates", "dried_figs", "tea_leaves", "rapeseed"]


def build():
    out = {}
    for name in WILD_PLANTS:
        out[f"block/{name}_plant"] = wild_plant(name)
    for base, fn in CROPS.items():
        for s in range(4):
            out[f"block/{base}_stage{s}"] = fn(s)
    for name in HERBS:
        out[f"block/{name}"] = herb(name)
        out[f"block/{name}_cut"] = herb(name, cut=True)
    for name in ITEMS:
        out[f"item/{'rosemary_sprigs' if name == 'rosemary' else name}"] = item(name)  # the plant block is rosemary
    for name, img in out.items():
        assert img.getbbox(), name   # every texture must have something painted
        path = TEX / f"{name}.png"
        path.parent.mkdir(parents=True, exist_ok=True)
        img.save(path)
    return out


if __name__ == "__main__":
    imgs = build()
    print(f"herbs textures ok ({len(imgs)})")
