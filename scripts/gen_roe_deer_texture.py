"""Paint the roe deer texture (and its skinned twin) for RoeDeerModel.

Usage: python scripts/gen_roe_deer_texture.py
Every texel is painted from its position on the model cube it belongs to, so the CUBES table below must match
the texOffs/addBox calls in RoeDeerModel.java exactly. Deterministic (hash noise, no random).
"""
from pathlib import Path

from PIL import Image

from gen_skinned_textures import skin

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity/wildlife/roe_deer.png"
SKINNED = ROOT / "src/main/resources/assets/mechanical/textures/entity/skinned/wildlife/roe_deer.png"
W, H = 64, 32

# name: (u, v, x0, y0, z0, w, h, d) -- same numbers as RoeDeerModel.createBodyLayer()
CUBES = {
    "body": (0, 0, -3.0, -3.0, -7.0, 6, 6, 13),
    "head": (38, 0, -2.0, -3.0, -3.0, 4, 4, 5),
    "ear": (56, 0, -1.5, -4.0, -0.5, 3, 4, 1),
    "tail": (56, 5, -1.0, 0.0, 0.0, 2, 2, 1),
    "snout": (38, 9, -1.5, -1.5, -6.0, 3, 3, 3),
    "neck": (50, 9, -1.5, -6.0, -2.0, 3, 6, 4),
    "front_upper": (0, 19, -1.5, -1.0, -1.5, 3, 5, 3),
    "front_lower": (12, 19, -1.0, 4.0, -1.0, 2, 6, 2),
    "hind_upper": (20, 19, -1.5, -1.0, -2.0, 3, 6, 4),
    "hind_lower": (34, 19, -1.0, 5.0, -0.5, 2, 6, 2),
}

# summer coat palette
FUR = (178, 94, 50)
FUR_DARK = (140, 70, 38)
FUR_DEEP = (112, 54, 31)
FUR_LIGHT = (200, 118, 66)
BELLY = (212, 166, 116)
WHITE = (236, 230, 216)
WHITE_SH = (212, 202, 186)
FACE = (146, 90, 56)
MUZZLE = (92, 64, 48)
BLACK = (26, 21, 20)
EYE = (34, 24, 22)
SHINE = (232, 232, 228)
LEG = (150, 92, 56)
HOOF = (44, 36, 32)
EAR_OUT = (138, 96, 66)
EAR_IN = (222, 188, 166)
EAR_RIM = (58, 42, 34)


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
    """Subtle fur noise: per-pixel jitter plus short vertical hair streaks."""
    n = (h(tx, ty, 7) / 1000 - 0.5) * 0.10 + (h(tx, ty // 2, 8) / 1000 - 0.5) * 0.06
    return shade(c, 1 + n * amount)


def paint_body(face, x, y, z, tx, ty):
    side = face in ("west", "east")
    if face == "bottom":
        c = BELLY if z < 3 else WHITE
        if abs(x) > 2:
            c = lerp(c, FUR, 0.45)
        return fur(c, tx, ty, 0.6)
    if face == "top":
        c = lerp(FUR_DARK, FUR, abs(x) / 3)  # darker spine line
        if z > 4.5:
            c = lerp(c, FUR_LIGHT, 0.3)
        return fur(c, tx, ty)
    if face == "south":  # rump: big white patch framed by russet
        if y < -1.5 or abs(x) > 2.5:
            return fur(FUR, tx, ty)
        return fur(WHITE if y < 1.5 else WHITE_SH, tx, ty, 0.5)
    if face == "north":  # chest under the neck
        c = lerp(FUR, FUR_DARK, (y + 3) / 6)
        return fur(c, tx, ty)
    if side:
        t = (y + 3) / 6  # 0 at the back line, 1 at the belly
        c = lerp(FUR_DARK, FUR, t * 3) if t < 0.34 else lerp(FUR, FUR_LIGHT, (t - 0.34) * 2)
        if t > 0.84:
            c = lerp(c, BELLY, 0.6)
        if z > 5 and y > -2:  # rump patch wraps round the haunch
            c = WHITE if y < 2 else WHITE_SH
        elif z > 4 and y > -1 and h(tx, ty, 3) < 500:
            c = lerp(c, WHITE, 0.5)
        if z < -5 and t > 0.6:  # shadow behind the elbow
            c = shade(c, 0.88)
        return fur(c, tx, ty)
    return fur(FUR, tx, ty)


def paint_neck(face, x, y, z, tx, ty):
    t = (y + 6) / 6  # 0 at the head end, 1 at the shoulders
    if face == "north":
        if y < -4:
            return fur(WHITE, tx, ty, 0.4)  # white throat patch
        if y < -3 and abs(x) < 1:
            return fur(WHITE_SH, tx, ty, 0.4)
        return fur(lerp(FUR_LIGHT, FUR, t), tx, ty)
    if face == "south":
        return fur(FUR_DARK, tx, ty)
    if face in ("west", "east"):
        c = lerp(FUR_DARK, FUR, (2 - z) / 3)  # darker along the nape
        if z < -1 and y < -4:
            c = lerp(c, WHITE, 0.6)
        return fur(c, tx, ty)
    return fur(FUR_DARK if face == "top" else FUR, tx, ty)


def paint_head(face, x, y, z, tx, ty):
    if face in ("west", "east"):
        # big dark eye on the side, 2x2 with a highlight towards the front top
        if -3 < z < -1 and -2 < y < 0:
            front = z < -2
            top = y < -1
            if front and top:
                return SHINE
            return EYE
        if -3 < z < -1 and -3 < y < -2:
            return fur(shade(FACE, 0.8), tx, ty)  # brow shadow
        if y > 0:
            return fur(lerp(FACE, WHITE, 0.5) if z < 0 else FACE, tx, ty)  # pale jaw line
        c = lerp(FACE, FUR, (z + 3) / 5)
        return fur(c, tx, ty)
    if face == "top":
        return fur(lerp(shade(FACE, 0.85), FUR_DARK, (z + 3) / 5), tx, ty)
    if face == "bottom":
        return fur(WHITE if z < 0.5 else lerp(WHITE, FUR_LIGHT, 0.4), tx, ty, 0.4)
    if face == "north":  # forehead above the snout
        return fur(shade(FACE, 0.82), tx, ty)
    return fur(FUR_DARK, tx, ty)


def paint_snout(face, x, y, z, tx, ty):
    if face == "north":
        if y < -0.5:
            return fur(MUZZLE, tx, ty)
        if y < 0.5:  # black nose with lighter nostrils
            return (54, 44, 42) if abs(x) > 0.9 else BLACK
        return WHITE if abs(x) > 0.9 else BLACK  # black lip, white chin corners
    if face == "bottom":
        return fur(WHITE, tx, ty, 0.4)
    if face == "top":
        return fur(lerp(MUZZLE, FACE, (z + 6) / 3), tx, ty)
    if face in ("west", "east"):
        if z < -5:
            return BLACK if y > -0.5 else MUZZLE
        if y > 0.5:
            return WHITE
        return fur(lerp(MUZZLE, FACE, (z + 5) / 2), tx, ty)
    return fur(FACE, tx, ty)


def paint_ear(face, x, y, z, tx, ty):
    rim = y < -3 or abs(x) > 0.5 and face in ("north", "south")
    if face == "north":  # inner ear, faces forward
        if y < -3:
            return EAR_RIM
        if abs(x) > 0.5:
            return fur(WHITE, tx, ty, 0.3)  # pale hair along the edges
        return EAR_IN if y < -0.5 else shade(EAR_IN, 0.9)
    if face == "south":
        return EAR_RIM if y < -3 else fur(EAR_OUT, tx, ty)
    if face == "top":
        return EAR_RIM
    return EAR_RIM if rim or y < -2.5 else fur(EAR_OUT, tx, ty)


def paint_tail(face, x, y, z, tx, ty):
    if face == "south" or face == "bottom":
        return fur(WHITE, tx, ty, 0.4)
    return fur(FUR_LIGHT if y < 1 else WHITE_SH, tx, ty)


def paint_front_upper(face, x, y, z, tx, ty):
    t = (y + 1) / 5
    if face == "south" or face == "bottom":
        c = lerp(FUR, BELLY, 0.4)
    else:
        c = lerp(FUR, LEG, t)
    if face == "north" and t > 0.5:
        c = shade(c, 0.9)
    return fur(c, tx, ty)


def leg_lower(face, x, y, z, tx, ty, top):
    if face == "bottom" or y > top + 5:
        return HOOF if face == "bottom" or h(tx, ty, 5) < 850 else shade(HOOF, 1.4)
    t = (y - top) / 5
    c = lerp(LEG, shade(LEG, 0.8), t)
    if face == "north":
        c = shade(c, 0.85)  # darker front of the cannon bone
    if face == "south" and y > top + 4:
        c = (70, 54, 44)  # dew claw / pastern
    if face == "top":
        c = FUR
    return fur(c, tx, ty, 0.7)


def paint_hind_upper(face, x, y, z, tx, ty):
    t = (y + 1) / 6
    if face == "south":
        return fur(WHITE if y < 2.5 else lerp(WHITE_SH, LEG, (y - 2.5) / 2.5), tx, ty, 0.5)
    if face in ("west", "east") and z > 1 and y < 3:
        return fur(lerp(FUR, WHITE, 0.55), tx, ty)  # edge of the rump patch on the ham
    if face == "bottom":
        return fur(LEG, tx, ty)
    c = lerp(FUR, LEG, max(0.0, t - 0.4) / 0.6)
    if t < 0.25:
        c = lerp(c, FUR_LIGHT, 0.4)  # highlight on the round of the haunch
    return fur(c, tx, ty)


def paint_hind_lower(face, x, y, z, tx, ty):
    c = leg_lower(face, x, y, z, tx, ty, 5.0)
    if face in ("west", "east") and 6 < y < 7.0:
        return (74, 52, 40)  # metatarsal gland tuft
    return c


PAINTERS = {
    "body": paint_body, "head": paint_head, "ear": paint_ear, "tail": paint_tail, "snout": paint_snout,
    "neck": paint_neck, "front_upper": paint_front_upper,
    "front_lower": lambda f, x, y, z, tx, ty: leg_lower(f, x, y, z, tx, ty, 4.0),
    "hind_upper": paint_hind_upper, "hind_lower": paint_hind_lower,
}


def build():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    for name, cube in CUBES.items():
        for tx, ty, face, x, y, z in texels(cube):
            assert 0 <= tx < W and 0 <= ty < H, (name, tx, ty)
            assert px[tx, ty][3] == 0, f"UV overlap at {tx},{ty} ({name})"
            px[tx, ty] = (*PAINTERS[name](face, x, y, z, tx, ty), 255)
    return img


def main():
    img = build()
    TEX.parent.mkdir(parents=True, exist_ok=True)
    SKINNED.parent.mkdir(parents=True, exist_ok=True)
    img.save(TEX)
    flesh = skin(img.copy())
    assert flesh.size == img.size and flesh.getchannel("A").tobytes() == img.getchannel("A").tobytes()
    flesh.save(SKINNED)
    print("wrote", TEX.relative_to(ROOT), "and", SKINNED.relative_to(ROOT))


if __name__ == "__main__":
    main()
