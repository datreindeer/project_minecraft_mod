"""Big pelts on the tall Drying Rack, and the raw pelt items (data: gen_preserving.py, DryingRackBlock.java).

- Rack planes, painted in model space (texture row = 15 - model y, column = model x; the pelt fills x 3-12):
  - bear (both blocks of a tall rack, one hide 10 x 25): head up under the top crossbar, forelegs splayed and lashed with
    twine up to the bar, hind legs lashed down to the bottom bar; split into drying_rack_bear_{raw,dry}_upper (rows 3-15)
    and _lower (rows 0-11). Fur in the Bear Pelt item's palette (FUR / TIPS, gen_bear_item_textures.py).
  - wolf (one block, 10 x 11): drying_rack_wolf_{raw,dry} (rows 3-13), the grey wolf coat (saddle down the back, tawny
    then buff to the edges, gen_wolf_pelt_textures.py), tail with a black tip.
  - each has a _back texture for the south face (same layout; the uv mirrors it): the flesh side, raw red-pink with fat,
    or cured to tan skin; the fur fringe shows round the edge.
  - raw: the fur darker and matted, a raw red rim; dried: fluffier with pale guard-hair tips, a stiff leathery rim.
- Items raw_bear_pelt / raw_wolf_pelt: the finished pelt items with the fur wet and dark, a red raw rim and the bottom
  right corner folded back to show the pink flesh side.
Run: python scripts/gen_pelt_rack_textures.py   (deterministic)"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, shade, smooth  # noqa: E402
from gen_predator_textures import (EYE, NOSE, W_BUFF, W_DARK, W_GRIZZLE, W_SADDLE, W_TAWNY, agouti)  # noqa: E402
from gen_bear_item_textures import FUR, TIPS  # noqa: E402
from gen_wildlife_item_textures import hexc  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
TWINE, TWINE_DK = (170, 130, 80), (125, 92, 55)
RAW_FLESH = [(132, 44, 42), (168, 70, 64), (194, 104, 92)]
FAT = (232, 214, 188)
SKIN = [(128, 96, 62), (160, 124, 86), (184, 150, 108)]  # cured flesh side
BEAR_FUR = [hexc(c)[:3] for c in FUR]
BEAR_TIPS = [hexc(c)[:3] for c in TIPS]

# T twine, N nose, s/S muzzle, E eye hole, e ear, o ragged edge, x fur, w paw (claws), t tail, k tail tip
BEAR = ["T...NN...T",
        "T..sNNs..T",
        "T..ssss..T",
        "T.eExxEe.T",
        "T.eexxee.T",
        "T..oxxo..T",
        "wwoxxxxoww",
        "oxxxxxxxxo",
        ".ooxxxxoo.",
        "..oxxxxo..",
        ".oxxxxxxo.",
        ".xxxxxxxx.",
        ".xxxxxxxx.",
        ".xxxxxxxx.",
        ".xxxxxxxx.",
        ".xxxxxxxx.",
        ".xxxxxxxx.",
        ".oxxxxxxo.",
        "..oxxxxo..",
        ".ooxxxxoo.",
        "oxxxxxxxxo",
        "wwoxxxxoww",
        "T..oxxo..T",
        "T...tt...T",
        "T........T"]
WOLF = ["T...NN...T",
        "T..sSSs..T",
        "T.eExxEe.T",
        "wwoexxeoww",
        "oxxxxxxxxo",
        ".oxxxxxxo.",
        ".oxxxxxxo.",
        "oxxxxxxxxo",
        "wwoxxxxoww",
        "T...tt...T",
        "T...kk...T"]
for _m in (BEAR, WOLF):
    assert all(len(r) == 10 and r == r[::-1] for r in _m), "masks are 10 wide and symmetric"


def coat(t, i, j, salt):
    """gen_wolf_pelt_textures.coat (copied: importing that script needs the built Minecraft jar)."""
    if t < 0.35:
        return agouti(lerp(W_SADDLE, W_GRIZZLE, t / 0.35), i, j, salt, amt=0.55)
    if t < 0.7:
        return agouti(lerp(W_GRIZZLE, W_TAWNY, smooth((t - 0.35) / 0.35)), i, j, salt + 5, amt=0.3)
    return fur(lerp(W_TAWNY, W_BUFF, (t - 0.7) / 0.3), i, j, salt + 9, 0.06)


def ramp(cols, t):
    t = max(0.0, min(0.999, t)) * (len(cols) - 1)
    i = int(t)
    return lerp(cols[i], cols[i + 1], t - i)


def bear_fur(x, y, n, dry, salt):
    """The Bear Pelt item's look at rack size: light from the top left, a grizzled stripe down the spine."""
    light = -0.04 * (x - 4.5) - 0.012 * (y - n / 2)
    strand = h(x, y // 2 + (x % 3), salt)
    t = 0.48 + light + 0.35 * (strand - 0.5) - 0.1 * (y % 2 == 0 and h(x, y, salt + 1) < 0.4)
    c = BEAR_FUR[max(0, min(len(BEAR_FUR) - 1, int(t * len(BEAR_FUR))))]
    spine = x in (4, 5) and 6 <= y <= n - 5
    if dry and spine and h(x, y, salt + 2) < 0.45 or dry and h(x, y, salt + 3) < 0.1:
        c = BEAR_TIPS[int(h(x, y, salt + 4) * 2)]
    return c


def wolf_fur(x, y, n, dry, salt):
    d = abs(x - 4.5)
    return coat(0.08 + 0.85 * smooth(d / 4.5) + (0.05 if dry and h(x, y, salt) < 0.15 else 0), x, y, salt + 1)


def pelt_front(mask, kind, dry, salt):
    """Rows of (x, row, colour) for the fur side."""
    n, out = len(mask), []
    for y, row in enumerate(mask):
        for x, c in enumerate(row):
            if c == ".":
                continue
            if c == "T":
                col = TWINE if h(x, y, salt) < 0.7 else TWINE_DK
            elif c == "N":
                col = NOSE
            elif c == "E":
                col = EYE  # the eye holes
            elif c == "s":
                col = lerp(BEAR_FUR[3], BEAR_TIPS[0], 0.3) if kind == "bear" else lerp(W_TAWNY, W_DARK, 0.3)
            elif c == "S":
                col = W_TAWNY
            elif c == "e":
                col = BEAR_FUR[1] if kind == "bear" else W_DARK
            elif c == "w":  # paw pad with pale claw tips at the outer edge
                outer = x in (0, 9)
                col = (196, 184, 160) if outer and h(x, y, salt + 1) < 0.5 else (52, 40, 32)
            elif c == "t":
                col = BEAR_FUR[2] if kind == "bear" else agouti(W_SADDLE, x, y, salt + 2, amt=0.5)
            elif c == "k":
                col = W_DARK
            elif c == "o":  # the rim: raw red flesh showing, or a stiff dark edge once cured
                if dry:
                    col = lerp(SKIN[0], (40, 30, 24), 0.5) if h(x, y, salt + 3) < 0.5 else (bear_fur if kind == "bear" else wolf_fur)(x, y, n, dry, salt + 4)
                else:
                    col = ramp(RAW_FLESH, h(x, y, salt + 5)) if h(x, y, salt + 6) < 0.8 else FAT
            else:
                col = (bear_fur if kind == "bear" else wolf_fur)(x, y, n, dry, salt + 7)
            if not dry and c in "xstoe" and c != "o":
                col = shade(lerp(col, (60, 40, 34), 0.12), 0.8)  # wet, matted, darker
                if h(x, y, salt + 8) < 0.06:
                    col = shade(col, 1.35)  # a wet glint
            out.append((x, y, col[:3]))
    return out


def pelt_back(mask, dry, salt):
    """The flesh side, seen from behind (same layout; the model's south uv mirrors it)."""
    out = []
    for y, row in enumerate(mask):
        for x, c in enumerate(row):
            if c == ".":
                continue
            if c == "T":
                col = TWINE if h(x, y, salt) < 0.7 else TWINE_DK
            elif c == "E":
                col = EYE
            elif c in "ow" or (c == "x" and (row[x - 1:x] in ("", ".", "T") or row[x + 1:x + 2] in ("", ".", "T"))):
                col = shade(BEAR_FUR[3] if c == "w" else BEAR_FUR[2], 0.9) if dry else (58, 40, 30)  # fur fringe round the edge
                if not dry and c == "o":
                    col = ramp(RAW_FLESH, 0.2 + 0.5 * h(x, y, salt + 1))
            elif dry:
                col = ramp(SKIN, 0.35 + 0.5 * h(x, y // 2, salt + 2))
                if h(x, y, salt + 3) < 0.08:
                    col = shade(col, 0.82)  # stretch marks
            else:
                col = ramp(RAW_FLESH, 0.3 + 0.6 * h(x, y // 2, salt + 4))
                if h(x // 2, y // 2, salt + 5) < 0.3:
                    col = lerp(col, FAT, 0.75)  # patches of fat still on the skin
            out.append((x, y, col[:3]))
    return out


def place(pixels, first_row, tex_row, count):
    img = Image.new("RGBA", (16, 16))
    for x, y, col in pixels:
        if first_row <= y < first_row + count:
            img.putpixel((3 + x, tex_row + y - first_row), (*col, 255))
    return img


def save(img, rel):
    p = TEX / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


for dry in (False, True):
    stage = "dry" if dry else "raw"
    for side, pix in (("", pelt_front(BEAR, "bear", dry, 600)), ("_back", pelt_back(BEAR, dry, 640))):
        # bear rows 0-12: upper block y 12..0 (texture rows 3-15); rows 13-24: lower block y 15..4 (rows 0-11)
        save(place(pix, 0, 3, 13), f"block/drying_rack_bear_{stage}_upper{side}")
        save(place(pix, 13, 0, 12), f"block/drying_rack_bear_{stage}_lower{side}")
    for side, pix in (("", pelt_front(WOLF, "wolf", dry, 700)), ("_back", pelt_back(WOLF, dry, 740))):
        save(place(pix, 0, 3, 11), f"block/drying_rack_wolf_{stage}{side}")  # y 12..2


# ---- items: the finished pelt, wet and dark, a raw rim and the bottom right corner folded back ----
def raw_item(src, salt):
    im = Image.open(TEX / f"item/{src}.png").convert("RGBA")
    out = Image.new("RGBA", (16, 16))
    px = im.load()
    solid = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
    edge = {(x, y) for x, y in solid if any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))}
    fold = 21  # pixels with x + y > fold are folded over: the flesh side shows there
    for x, y in solid:
        c = px[x, y][:3]
        if x + y > fold:
            col = ramp(RAW_FLESH, 0.35 + 0.5 * h(x, y, salt))
            if h(x // 2, y, salt + 1) < 0.3:
                col = lerp(col, FAT, 0.7)
            if x + y == fold + 1:
                col = FAT  # the fold's pale edge
        elif (x, y) in edge and c not in (EYE, NOSE) and h(x, y, salt + 2) < 0.45:
            col = ramp(RAW_FLESH, h(x, y, salt + 3))
        else:
            col = shade(lerp(c, (60, 40, 34), 0.1), 0.85)
            if h(x, y, salt + 4) < 0.05:
                col = shade(col, 1.3)
        out.putpixel((x, y), (*col[:3], 255))
    for x, y in solid:  # a drop of blood under the fold
        if x + y == fold and h(x, y, salt + 5) < 0.35:
            out.putpixel((x, y), (110, 20, 22, 255))
    return out


save(raw_item("bear_pelt", 800), "item/raw_bear_pelt")
save(raw_item("wolf_pelt", 820), "item/raw_wolf_pelt")
print("pelt rack textures ok")
