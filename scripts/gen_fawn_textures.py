"""Paint the roe deer fawn and red deer calf textures for FawnModel (one model, one UV layout).

Usage: python scripts/gen_fawn_textures.py
Every texel is painted from its position on the model cube it belongs to (same approach as gen_roe_deer_texture.py),
so the CUBES table below must match the texOffs/addBox calls in FawnModel.java exactly. Deterministic (hash noise).
"""
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "src/main/resources/assets/mechanical/textures/entity/wildlife"
W, H = 64, 32

# name: (u, v, x0, y0, z0, w, h, d) -- same numbers as FawnModel.createBodyLayer()
CUBES = {
    "body": (0, 0, -2.0, -2.0, -4.0, 4, 4, 8),
    "head": (24, 0, -1.5, -2.0, -3.0, 3, 3, 4),
    "snout": (38, 0, -1.0, -1.5, -5.0, 2, 2, 2),
    "ear": (46, 0, -1.0, -3.0, -0.5, 2, 3, 1),
    "tail": (52, 0, -0.5, 0.0, 0.0, 1, 2, 1),
    "neck": (24, 7, -1.0, -3.0, -1.0, 2, 4, 2),
    "front_upper": (0, 12, -1.0, -1.5, -1.0, 2, 4, 2),
    "front_lower": (8, 12, -0.5, 2.5, -0.5, 1, 4, 1),
    "hind_upper": (12, 12, -1.0, -1.5, -1.5, 2, 4, 3),
    "hind_lower": (32, 7, -0.5, 2.5, -0.2, 1, 4, 1),
}

SHARED = dict(
    BLACK=(28, 22, 21), EYE=(22, 16, 15), SHINE=(236, 236, 230), HOOF=(48, 38, 34),
    EAR_IN=(226, 194, 174), EAR_RIM=(62, 44, 36), WHITE=(238, 232, 218), WHITE_SH=(214, 204, 188),
)
# roe fawn: russet coat, white spots, no real rump patch yet
ROE = dict(SHARED, FUR=(156, 82, 44), FUR_DARK=(118, 58, 32), FUR_LIGHT=(184, 106, 60), BELLY=(214, 174, 128),
           SPOT=(240, 232, 214), FACE=(140, 84, 50), MUZZLE=(88, 60, 44), LEG=(146, 90, 56), EAR_OUT=(132, 88, 60),
           RUMP=None)
# red deer calf: warmer, lighter red-brown, pale buff spots, buff rump patch hint
RED = dict(SHARED, FUR=(186, 94, 46), FUR_DARK=(148, 68, 36), FUR_LIGHT=(210, 124, 70), BELLY=(220, 186, 140),
           SPOT=(232, 208, 160), FACE=(150, 96, 62), MUZZLE=(110, 82, 62), LEG=(150, 100, 66), EAR_OUT=(140, 98, 70),
           RUMP=(218, 188, 140))


def h(x, y, salt):
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def texels(cube):
    """Yield (tx, ty, face, x, y, z): every texel of the box net with the local point at its centre."""
    u, v, x0, y0, z0, w, hh, d = cube
    for i in range(w):
        for j in range(d):
            z = z0 + d - j - 0.5
            yield u + d + i, v + j, "top", x0 + i + 0.5, y0, z
            yield u + d + w + i, v + j, "bottom", x0 + i + 0.5, y0 + hh, z
    for j in range(hh):
        y = y0 + j + 0.5
        for i in range(d):
            yield u + i, v + d + j, "west", x0, y, z0 + d - i - 0.5
            yield u + d + w + i, v + d + j, "east", x0 + w, y, z0 + i + 0.5
        for i in range(w):
            yield u + d + i, v + d + j, "north", x0 + i + 0.5, y, z0
            yield u + 2 * d + w + i, v + d + j, "south", x0 + w - i - 0.5, y, z0 + d


def fur(c, tx, ty, amount=1.0):
    n = (h(tx, ty, 7) / 1000 - 0.5) * 0.10 + (h(tx, ty // 2, 8) / 1000 - 0.5) * 0.06
    return shade(c, 1 + n * amount)


def spotted(z, row):
    """Dapple: spot rows running along the body (z centres -3.5 .. 3.5), every other px, alternate rows staggered,
    with an odd spot left out so it does not read as a checkerboard."""
    k = int(z + 4.5)
    return (k + row) % 2 == 0 and h(k, row, 4) > 90


def paint_body(P, face, x, y, z, tx, ty):
    if face == "bottom":
        c = P["BELLY"] if abs(x) < 1 else lerp(P["BELLY"], P["FUR"], 0.4)
        return fur(c, tx, ty, 0.6)
    if face == "top":
        if abs(x) < 1:  # dark spine stripe
            return fur(P["FUR_DARK"], tx, ty)
        if spotted(z, 0):  # spot row either side of the spine
            return fur(P["SPOT"], tx, ty, 0.3)
        return fur(lerp(P["FUR_DARK"], P["FUR"], 0.6), tx, ty)
    if face == "south":  # rump
        if P["RUMP"] and y > -1 and abs(x) < 1.5:
            return fur(P["RUMP"], tx, ty, 0.5)
        return fur(lerp(P["FUR"], P["BELLY"], 0.5) if y > 1 else P["FUR"], tx, ty)
    if face == "north":  # chest
        return fur(lerp(P["FUR"], P["BELLY"], 0.6) if y > 0 and abs(x) < 1 else P["FUR"], tx, ty)
    # flanks: dark back line, two staggered spot rows, lighter towards the belly
    t = (y + 2) / 4
    c = lerp(P["FUR_DARK"], P["FUR"], t * 2.2)
    if t > 0.75:
        c = lerp(P["FUR_LIGHT"], P["BELLY"], 0.45)
    if y < -1 and spotted(z, 1):  # main row just under the back
        c = P["SPOT"]
    elif 0 < y < 1 and spotted(z, 2) and -3 < z < 3:  # fainter lower row, staggered, with a plain row between
        c = lerp(P["SPOT"], c, 0.3)
    if P["RUMP"] and z > 3 and -1 < y < 1:
        c = lerp(c, P["RUMP"], 0.55)
    return fur(c, tx, ty)


def paint_neck(P, face, x, y, z, tx, ty):
    t = (y + 3) / 4  # 0 at the head end, 1 at the shoulders
    if face == "north":  # throat: white near the jaw
        return fur(P["WHITE"] if y < -1.5 else lerp(P["BELLY"], P["FUR"], t), tx, ty, 0.4)
    if face == "south":
        return fur(P["FUR_DARK"], tx, ty)
    if face in ("west", "east"):
        c = P["FUR_DARK"] if z > 0 else P["FUR"]
        if z < 0 and y < -2:
            c = lerp(c, P["WHITE"], 0.5)
        return fur(c, tx, ty)
    return fur(P["FUR"], tx, ty)


def paint_head(P, face, x, y, z, tx, ty):
    if face in ("west", "east"):
        if -2 < z < 0 and y < 0:  # big 2x2 dark eye, highlight front-top
            return P["SHINE"] if z < -1 and y < -1 else P["EYE"]
        if y > 0:
            return fur(lerp(P["FACE"], P["WHITE"], 0.55), tx, ty, 0.4)  # pale cheek / jaw
        if z < -2:
            return fur(lerp(P["FACE"], P["WHITE"], 0.25), tx, ty)  # pale ring in front of the eye
        return fur(P["FUR"], tx, ty)
    if face == "top":
        return fur(lerp(shade(P["FACE"], 0.85), P["FUR_DARK"], (z + 3) / 4), tx, ty)
    if face == "bottom":
        return fur(P["WHITE"], tx, ty, 0.4)
    if face == "north":  # forehead above the snout
        return fur(shade(P["FACE"], 0.85), tx, ty)
    return fur(P["FUR_DARK"], tx, ty)


def paint_snout(P, face, x, y, z, tx, ty):
    if face == "north":
        return P["BLACK"] if y < 0 else P["WHITE"]  # dark nose over a white chin
    if face == "bottom":
        return fur(P["WHITE"], tx, ty, 0.4)
    if face == "top":
        return fur(P["MUZZLE"] if z < -4 else P["FACE"], tx, ty)
    if face in ("west", "east"):
        if y > 0:
            return P["WHITE"]  # pale lip band
        return P["BLACK"] if z < -4 else fur(P["MUZZLE"], tx, ty)
    return fur(P["FACE"], tx, ty)


def paint_ear(P, face, x, y, z, tx, ty):
    if face == "north":  # inner ear
        if y < -2:
            return P["EAR_RIM"]
        return P["EAR_IN"] if y < -0.5 else shade(P["EAR_IN"], 0.88)
    if face == "south":
        return P["EAR_RIM"] if y < -2 else fur(P["EAR_OUT"], tx, ty)
    if face == "top":
        return P["EAR_RIM"]
    return P["EAR_RIM"] if y < -2 else fur(P["EAR_OUT"], tx, ty)


def paint_tail(P, face, x, y, z, tx, ty):
    if face in ("south", "bottom"):
        return fur(P["WHITE"], tx, ty, 0.4)
    return fur(P["FUR_DARK"] if y < 1 else P["FUR"], tx, ty)


def paint_front_upper(P, face, x, y, z, tx, ty):
    if face == "south":
        return fur(lerp(P["FUR"], P["BELLY"], 0.5), tx, ty)
    return fur(lerp(P["FUR"], P["LEG"], (y + 1.5) / 4), tx, ty)


def paint_hind_upper(P, face, x, y, z, tx, ty):
    if face == "south":
        return fur(P["RUMP"] or lerp(P["FUR"], P["BELLY"], 0.5), tx, ty, 0.5)
    if face == "bottom":
        return fur(P["LEG"], tx, ty)
    c = lerp(P["FUR"], P["LEG"], max(0.0, (y + 1.5) / 4 - 0.4) / 0.6)
    if y < 0 and face in ("west", "east"):
        c = lerp(c, P["FUR_LIGHT"], 0.35)  # round of the haunch
    return fur(c, tx, ty)


def paint_lower(P, face, x, y, z, tx, ty):
    if face == "bottom" or y > 5.5:
        return P["HOOF"]
    if face == "top":
        return P["LEG"]
    c = lerp(P["LEG"], shade(P["LEG"], 0.78), (y - 2.5) / 3)
    if face == "north":
        c = shade(c, 0.88)
    return fur(c, tx, ty, 0.7)


PAINTERS = {
    "body": paint_body, "head": paint_head, "snout": paint_snout, "ear": paint_ear, "tail": paint_tail,
    "neck": paint_neck, "front_upper": paint_front_upper, "front_lower": paint_lower,
    "hind_upper": paint_hind_upper, "hind_lower": paint_lower,
}


def build(P):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    for name, cube in CUBES.items():
        for tx, ty, face, x, y, z in texels(cube):
            assert 0 <= tx < W and 0 <= ty < H, (name, tx, ty)
            assert px[tx, ty][3] == 0, f"UV overlap at {tx},{ty} ({name})"
            px[tx, ty] = (*PAINTERS[name](P, face, x, y, z, tx, ty), 255)
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for fname, P in (("roe_deer_fawn.png", ROE), ("red_deer_calf.png", RED)):
        build(P).save(OUT / fname)
        print("wrote", (OUT / fname).relative_to(ROOT))


if __name__ == "__main__":
    main()
