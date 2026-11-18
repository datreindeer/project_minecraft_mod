"""Wild boar piglet ("humbug") entity texture (64x32) for PigletModel.

Usage: python gen_piglet_texture.py
Box UVs below must match PigletModel.createBodyLayer() (texOffs, w, h, d).
The coat stripes are painted from 3D positions: every face pixel is mapped back to a point on its box (box-local,
front at z = 0) and coloured by its distance round the cross-section from the spine, so a stripe on the top face
runs on over the edge onto the side and the rump at the same height.
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/wildlife/wild_boar_piglet.png"
W, H = 64, 32

# name: (u, v, w, h, d)
BOXES = {
    "body": (0, 0, 6, 5, 9),
    "head": (0, 14, 5, 5, 4),
    "leg": (18, 14, 2, 3, 2),
    "snout": (32, 0, 3, 2, 2),
    "ear": (32, 4, 2, 2, 1),
    "tail": (38, 4, 1, 3, 1),
}

BASE = [(84, 52, 30), (98, 62, 36), (110, 71, 41), (122, 80, 47)]       # chestnut
DARK = [(58, 37, 23), (68, 44, 27), (78, 51, 31)]                       # dark stripes, spine
LIGHT = [(196, 160, 106), (214, 180, 124), (228, 198, 146)]              # cream/tan stripes
BELLY = [(150, 118, 84), (166, 134, 98), (180, 150, 112)]
LEG = [(46, 31, 22), (58, 39, 27), (70, 48, 33)]
HOOF = [(30, 27, 26), (44, 40, 38)]
SKIN = [(72, 52, 48), (88, 64, 58)]
DISC = [(128, 92, 86), (150, 112, 104), (168, 128, 120)]
NOSTRIL = (46, 28, 28)
EYE, EYE_GLINT = (12, 9, 8), (120, 96, 80)

# floor(distance from spine) -> band, for the body; past the end of the list is belly
BODY_BANDS = "DLBLDLBF"


def h(x, y, salt):
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return (n ^ (n >> 16)) % 1000 / 1000


def pick(pal, t):
    return pal[max(0, min(len(pal) - 1, int(t * len(pal))))]


def faces(u, v, w, hh, d):
    """face -> (x0, y0, width, height) in texture space."""
    return {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
        "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh),
    }


def local_point(face, s, r, w, hh, d):
    """Box-local 3D point (x, y, z) of face coords s, r in [0, 1] (texture x, y). y down, z = 0 at the front."""
    return {
        "top": (s * w, 0, d - r * d), "bottom": (s * w, hh, d - r * d),
        "right": (0, r * hh, d - s * d), "front": (s * w, r * hh, 0),
        "left": (w, r * hh, s * d), "back": (w - s * w, r * hh, d),
    }[face]


def round_dist(x, y, w, edge=1.0):
    """Distance round the cross-section from the spine (top centre), or None for the inside of a front/back face
    deeper than `edge` from the top or sides."""
    cx = w / 2
    side = min(x, w - x)
    if y <= side:
        return abs(x - cx) if y <= edge else None
    return cx + y if side <= edge else None


def band_color(band, z, gx, gy, salt):
    jitter = h(gx, gy, salt)
    if band == "L":
        # the light stripes break up here and there along their length
        if h(int(z), int(gy * 7 + gx * 3), salt + 1) < 0.12:
            return pick(BASE, 0.6 + 0.4 * jitter)
        return pick(LIGHT, 0.2 + 0.8 * jitter)
    if band == "D":
        return pick(DARK, jitter)
    if band == "F":  # lower flank, warming into the belly
        return pick(BELLY, 0.3 * jitter) if jitter < 0.5 else pick(BASE, 0.7 + 0.3 * jitter)
    return pick(BASE, 0.25 + 0.6 * jitter)


def paint(img, name, fn):
    """fn(face, x, y, z, gx, gy) -> rgb, with (x, y, z) the box-local 3D point of the pixel centre."""
    u, v, w, hh, d = BOXES[name]
    px = img.load()
    for face, (x0, y0, fw, fh) in faces(u, v, w, hh, d).items():
        for ty in range(fh):
            for tx in range(fw):
                x, y, z = local_point(face, (tx + 0.5) / fw, (ty + 0.5) / fh, w, hh, d)
                px[x0 + tx, y0 + ty] = (*fn(face, x, y, z, x0 + tx, y0 + ty), 255)


# --- parts -----------------------------------------------------------------------------------------------------

def body(face, x, y, z, gx, gy):
    w, hh, d = BOXES["body"][2:]
    if face == "bottom":
        return pick(BELLY, h(gx, gy, 11))
    dist = round_dist(x, y, w)
    if dist is None:  # chest behind the head / middle of the rump
        return pick(BASE, 0.1 + 0.5 * h(gx, gy, 12))
    i = int(dist)
    band = BODY_BANDS[i] if i < len(BODY_BANDS) else None
    if band is None:
        return pick(BELLY, h(gx, gy, 13))
    if band == "L" and (z < 0.6 or z > d - 0.6) and face in ("top", "left", "right"):
        band = "B"  # stripes fade out at the very shoulder and rump ends
    return band_color(band, z, gx, gy, 14)


def head(face, x, y, z, gx, gy):
    w, hh, d = BOXES["head"][2:]
    col, row = int(x), int(y)
    if face in ("left", "right"):
        zc = int(z)  # 0 = front
        if row == 1 and zc == 1:
            return EYE
        if row == 1 and zc == 2:
            return pick(BASE, 0.1)  # dark eye stripe behind the eye
        if row == 0:
            return band_color("L" if zc >= 1 else "B", z + 20, gx, gy, 21)  # stripe from the brow back over the ear
        if row >= 3:
            return pick(BELLY, 0.2 + 0.6 * h(gx, gy, 22)) if row == 4 or zc == 0 else pick(LIGHT, 0.1 * h(gx, gy, 23))
        return pick(BASE, 0.3 + 0.6 * h(gx, gy, 24))
    if face == "top":
        dist = abs(x - w / 2)
        return band_color("DLB"[min(2, int(dist))] if z > 0.8 else "B", z + 20, gx, gy, 25)
    if face == "front":  # face: forehead stripes, dark round the snout, pale jowls
        if row == 0:
            return band_color("DLB"[min(2, int(abs(x - w / 2)))], 30, gx, gy, 26)
        if row == 1 and col in (0, w - 1):
            return EYE
        if row >= 3 and col in (0, w - 1):
            return pick(BELLY, h(gx, gy, 27))
        if row == 1 and col == w // 2:
            return pick(DARK, h(gx, gy, 31))  # dark blaze down the nose bridge
        return pick(BASE, 0.4 + 0.5 * h(gx, gy, 28))
    if face == "bottom":
        return pick(BELLY, h(gx, gy, 29))
    return pick(BASE, 0.2 + 0.6 * h(gx, gy, 30))  # back of the head, hidden in the body


def snout(face, x, y, z, gx, gy):
    if face == "front":  # disc nose with two nostrils
        col, row = int(x), int(y)
        if col in (0, 2) and row == 1:
            return NOSTRIL
        return DISC[2] if (col, row) == (1, 0) else DISC[1]
    if face == "top":
        return pick(BASE, 0.15 * h(gx, gy, 41)) if z > 1 else SKIN[1]
    if face == "bottom":
        return SKIN[0]
    return SKIN[1] if y < 1 else SKIN[0]


def ear(face, x, y, z, gx, gy):
    if face == "front":
        return (110, 78, 66) if y > 1 else DARK[0]  # inner ear
    return DARK[0] if y < 1 else pick(BASE, 0.1 + 0.3 * h(gx, gy, 51))


def tail(face, x, y, z, gx, gy):
    return DARK[0] if y > 2 else pick(BASE, 0.1 + 0.3 * h(gx, gy, 61))


def leg(face, x, y, z, gx, gy):
    if face == "bottom" or y > 2:
        if face == "front" and int(x) == 1 and y > 2:
            return HOOF[0]  # cleft
        return HOOF[1] if face != "bottom" else HOOF[0]
    if face == "top":
        return LEG[0]
    return pick(LEG, 0.2 + 0.7 * h(gx, gy, 71) + (0.25 if y < 1 else 0))


def build():
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for name, fn in (("body", body), ("head", head), ("snout", snout), ("ear", ear), ("tail", tail), ("leg", leg)):
        paint(img, name, fn)
    return img


def check(img):
    """No two boxes share UV pixels, every face pixel is painted, nothing is painted outside the boxes."""
    used = set()
    for name, (u, v, w, hh, d) in BOXES.items():
        px = {(fx + x, fy + y) for fx, fy, fw, fh in faces(u, v, w, hh, d).values() for y in range(fh) for x in range(fw)}
        assert all(0 <= x < W and 0 <= y < H for x, y in px), f"{name} runs off the texture"
        assert not (used & px), f"{name} overlaps another box"
        used |= px
        assert all(img.getpixel(p)[3] == 255 for p in px), f"{name} has holes"
    assert all(img.getpixel((x, y))[3] == 0 for x in range(W) for y in range(H) if (x, y) not in used)


def main():
    img = build()
    check(img)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
