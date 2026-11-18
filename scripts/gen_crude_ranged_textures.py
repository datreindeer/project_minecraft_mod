"""Generate crude (stone age) bow and fishing rod item textures.

The bow frames recolour the vanilla bow / bow_pulling_0..2 pixel by pixel so the layout (and the
vanilla bow model display transforms) stay identical. The fishing rod is drawn from a grid that
follows the vanilla fishing_rod layout, with a crook in the stick.

Run: python scripts/gen_crude_ranged_textures.py
"""
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/mechanical/textures/item"
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def from_grid(rows, pal):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), hexc(pal[ch]))
    return im


PAL = {
    # branch wood: bark on the back of the limb, whittled pale wood on the belly
    "B": "#3e2f1f",  # dark bark
    "b": "#5a4630",  # bark
    "w": "#7d6038",  # wood
    "l": "#a3834f",  # pale whittled wood
    "d": "#2a1e10",  # shadow edge
    "k": "#2f2216",  # knot
    # cordage grip (from cordage.png)
    "C": "#b5977d",
    "c": "#a68870",
    "n": "#765636",
    # twine (from twine.png)
    "T": "#826655",
    "t": "#634e42",
    "u": "#534237",
    # flint (from flint_*.png)
    "F": "#8a8ea5",
    "f": "#727588",
    "g": "#535664",
}

# ---------- bow: recolour vanilla by colour class ----------

V_OUTER, V_LIGHT, V_MID, V_DARK = (73, 54, 21), (137, 103, 39), (104, 78, 30), (40, 30, 11)
V_GRIP, V_GRIP_HI, V_STRING = (107, 107, 107), (150, 150, 150), (68, 68, 68)
# Knots per frame (on the bark edge) and the bump pixel that sticks out of the limb beside each.
KNOTS = {
    "bow": [((9, 2), (9, 1)), ((2, 10), (1, 10))],
    "bow_pulling_0": [((10, 2), (10, 1)), ((1, 12), (0, 12))],
    "bow_pulling_1": [((10, 2), (10, 1)), ((2, 10), (1, 10))],
    "bow_pulling_2": [((10, 2), (10, 1)), ((2, 11), (1, 11))],
}
# Pixels cut off the outer edge so the limb looks hand-carved and uneven.
NICKS = {
    "bow": [(13, 1)],
    "bow_pulling_0": [(14, 1)],
    "bow_pulling_1": [(13, 1)],
    "bow_pulling_2": [(14, 2)],
}


def speck(x, y, salt=0):
    return (x * 7 + y * 13 + x * y * 3 + salt) % 5 == 0


def crude_bow(src, name):
    im = src.convert("RGBA")
    px = im.load()
    pulling = name != "bow"
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    o = out.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            rgb = (r, g, b)
            if pulling and rgb in (V_LIGHT, V_MID, V_DARK) and x - y in (0, 1):
                o[x, y] = px[x, y]  # vanilla arrow shaft, keep it
            elif rgb == V_OUTER:
                o[x, y] = hexc(PAL["B" if speck(x, y) else "b"])
            elif rgb == V_LIGHT:
                o[x, y] = hexc(PAL["b" if speck(x, y, 2) else "l"])
            elif rgb == V_MID:
                o[x, y] = hexc(PAL["w"])
            elif rgb == V_DARK:
                o[x, y] = hexc(PAL["d"])
            elif rgb == V_GRIP_HI:
                o[x, y] = hexc(PAL["C"])
            elif rgb == V_GRIP:
                o[x, y] = hexc(PAL["c" if (x + y) % 2 else "n"])  # wrap turns
            elif rgb == V_STRING:
                o[x, y] = hexc(PAL["T" if (x + y) % 2 else "t"])  # twisted twine
            else:
                o[x, y] = px[x, y]  # arrow head / fletching
    for knot, bump in KNOTS[name]:
        o[knot] = hexc(PAL["k"])
        o[bump] = hexc(PAL["w"])
    for p in NICKS[name]:
        o[p] = (0, 0, 0, 0)
    return out


# ---------- fishing rod: vanilla layout, stick kinked one pixel at row 7 ----------

ROD = [
    "................",
    ".............Tt.",
    "............tbd.",
    "...........twld.",
    "..........tbldS.",
    ".........twld.s.",
    "........twkd..s.",
    "......twbd....S.",
    ".....twld.....s.",
    "....tbld......S.",
    "....wld......s..",
    "...wlTTu.....S..",
    "..wldtTu....s...",
    ".bld.uu...F.f...",
    ".dd.......gF....",
    "................",
]
LINE = {"s": "#634e42", "S": "#826655"}
CAST_REMOVED = set(LINE) | {"F", "f", "g"}


def main():
    with zipfile.ZipFile(JAR) as jar:
        for name in ["bow", "bow_pulling_0", "bow_pulling_1", "bow_pulling_2"]:
            src = Image.open(BytesIO(jar.read(f"assets/minecraft/textures/item/{name}.png")))
            crude_bow(src, name).save(OUT / f"crude_{name}.png")
    pal = PAL | LINE
    from_grid(ROD, pal).save(OUT / "crude_fishing_rod.png")
    cast = ["".join("." if ch in CAST_REMOVED else ch for ch in row) for row in ROD]
    from_grid(cast, pal).save(OUT / "crude_fishing_rod_cast.png")
    print("ok")


if __name__ == "__main__":
    main()
