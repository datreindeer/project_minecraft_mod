"""Paint the night predator textures: grey wolf (64x64, GreyWolfModel), its skinned (flesh) version for butchered
carcasses, and the 16x16 items: Wolf Pelt and both spawn eggs. The cave lion's model and skin are made by
gen_savanna_models.py.

Usage: python scripts/gen_predator_textures.py
Deterministic. WOLF must match the cubes in the model (texOffs, origin, size); same 3D painting approach as
the other wildlife (wildlife_paint.py), so markings line up across faces.
Grey wolf look: grizzled grey-black saddle over the back and the top of the tail, tawny-buff flanks and legs, cream
belly, throat, cheeks and lips, dark muzzle bridge, black nose and tail tip, amber eyes, a dark stripe down the front of
the forelegs.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import OUT, egg, hexc  # noqa: E402

# ---------------- grey wolf ----------------
W_SADDLE = (58, 56, 54)
W_GRIZZLE = (128, 122, 112)
W_TAWNY = (150, 122, 88)
W_BUFF = (186, 162, 124)
W_CREAM = (226, 216, 196)
W_DARK = (30, 28, 27)
NOSE = (24, 22, 22)
EYE, EYE_AMBER = (14, 12, 10), (196, 140, 40)
TONGUE, TOOTH = (170, 76, 80), (230, 224, 206)
PAD = (40, 34, 32)
TRANSPARENT = (0, 0, 0, 0)


def agouti(base, i, j, salt, dark=W_DARK, light=W_CREAM, amt=0.35):
    """Banded guard hairs: each texel is a hair tip that is dark, pale-banded or base coloured."""
    r = h(i, j, salt)
    if r < amt * 0.45:
        return fur(lerp(base, dark, 0.65), i, j, salt + 1, 0.08)
    if r > 1 - amt * 0.4:
        return fur(lerp(base, light, 0.5), i, j, salt + 2, 0.05)
    return fur(base, i, j, salt + 3, 0.08)


def wolf_coat(t, i, j, salt):
    """Down the side, t 0 (spine) .. 1 (belly): grizzled saddle, tawny flank, buff, cream belly."""
    if t < 0.32:
        return agouti(lerp(W_SADDLE, W_GRIZZLE, t / 0.32 * 0.8), i, j, salt, amt=0.55)
    if t < 0.62:
        return agouti(lerp(W_GRIZZLE, W_TAWNY, smooth((t - 0.32) / 0.3)), i, j, salt + 10, amt=0.3)
    if t < 0.8:
        return fur(lerp(W_TAWNY, W_BUFF, (t - 0.62) / 0.18), i, j, salt + 20)
    return fur(lerp(W_BUFF, W_CREAM, smooth((t - 0.8) / 0.2)), i, j, salt + 30, 0.04)


def wolf_body(top, bottom, pale_front=False):
    def mat(v, face, x, y, z, i, j):
        if face == "top":
            return agouti(lerp(W_SADDLE, W_GRIZZLE, abs(x) / 4), i, j, 11, amt=0.6)
        if face == "bottom":
            return fur(W_CREAM, i, j, 12, 0.04)
        if face == "front":
            t = (y - top) / (bottom - top)
            return fur(lerp(W_BUFF, W_CREAM, t), i, j, 13, 0.05) if pale_front else wolf_coat(t, i, j, 14)
        if face == "back":
            t = (y - top) / (bottom - top)
            return wolf_coat(max(t, abs(x) / 3 * 0.3), i, j, 15) if t < 0.5 else fur(W_CREAM, i, j, 16, 0.04)
        return wolf_coat((y - top) / (bottom - top), i, j, 17)
    return mat


def wolf_mane(v, face, x, y, z, i, j):
    """Thick shoulder ruff: longer, streakier hair; the throat (front, lower) cream."""
    t = (y + 4) / 7
    if face == "front":
        return fur(lerp(W_GRIZZLE, W_CREAM, smooth(t * 1.6)), i, j, 21, 0.09)
    if face == "bottom":
        return fur(W_CREAM, i, j, 22, 0.05)
    if face == "top":
        return agouti(W_SADDLE, i, j, 23, amt=0.7)
    return wolf_coat(t * 0.85, i, j // 2 * 2 + (i % 2), 24)  # long hanging hairs


def wolf_tail(v, face, x, y, z, i, j):
    """Hangs from the root (y 0) to the tip (y 9): black tip, dark top with the gland spot, buff underside."""
    if y > 7 or face == "bottom":
        return fur(W_DARK, i, j, 31, 0.08)
    if face == "back" and 1 < y < 3:
        return fur(W_DARK, i, j, 32, 0.06)  # dark spot on top near the root (the tail hangs, so "back" is up)
    if face in ("back", "top"):
        return agouti(W_SADDLE, i, j, 33, amt=0.6)
    if face == "front":
        return fur(W_BUFF, i, j, 34, 0.06)
    return agouti(lerp(W_GRIZZLE, W_TAWNY, y / 9), i, j, 35, amt=0.4)


def wolf_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 1.5) < 0.1:
            return EYE_AMBER if x < 0 else EYE_AMBER
        if abs(abs(x) - 2.5) < 0.1 and abs(y + 1.5) < 0.1:
            return EYE  # dark outer eye line
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 2.5) < 0.1:
            return fur(W_CREAM, i, j, 41, 0.04)  # pale brow spots
        if abs(x) < 1 and y < -1:
            return agouti(W_SADDLE, i, j, 42, amt=0.5)  # dark forehead between the eyes
        return fur(lerp(W_TAWNY, W_CREAM, 0.5 if y > 0 else 0.0), i, j, 43)
    if face == "top":
        return agouti(lerp(W_SADDLE, W_GRIZZLE, 0.4), i, j, 44, amt=0.5)
    if face == "bottom":
        return fur(W_CREAM, i, j, 45, 0.04)
    if face in ("left", "right"):
        return fur(W_CREAM, i, j, 46, 0.04) if y > 0 else agouti(W_TAWNY, i, j, 47)
    return agouti(W_GRIZZLE, i, j, 48)


def wolf_ruff(v, face, x, y, z, i, j):
    """Cheek ruffs: cream below, grizzled tips above."""
    t = (y + 1) / 4
    if face == "top":
        return agouti(W_GRIZZLE, i, j, 51, amt=0.5)
    return fur(lerp(W_GRIZZLE, W_CREAM, smooth(t * 1.4)), i, j, 52, 0.08)


def wolf_snout(v, face, x, y, z, i, j):
    if face == "front":
        return NOSE if y < 0 else shade(W_DARK, 1.2)
    if face == "top":
        f = (-z - 4) / 4  # 0 at the eyes .. 1 at the nose
        return NOSE if f > 0.85 else agouti(lerp(W_TAWNY, W_SADDLE, f * 0.6), i, j, 61, amt=0.3)
    if face == "bottom":
        return shade(TONGUE, 0.8)  # palate, seen when the jaw drops
    if y > 0:
        return W_DARK if z < -6 else fur(W_CREAM, i, j, 62, 0.04)  # lip line towards the front
    return fur(W_TAWNY if z > -6.5 else lerp(W_TAWNY, W_DARK, 0.4), i, j, 63)


def wolf_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -3 or abs(x) > 1 else TONGUE
    if face in ("left", "right", "front"):
        return W_DARK if face != "front" and z < -3 else fur(W_CREAM, i, j, 71, 0.04)
    return fur(W_CREAM, i, j, 72, 0.04)


def wolf_ear(v, face, x, y, z, i, j):
    """Pointed: the outer top corner is cut away; pale inner ear, dark-rimmed back."""
    if y < -2 and x < 0:
        return TRANSPARENT
    if face == "front":
        return fur(W_CREAM, i, j, 81, 0.05) if x > 0 and y > -2 else fur(W_DARK, i, j, 82, 0.06)
    if face == "back":
        return agouti(W_TAWNY, i, j, 83, amt=0.5)
    return fur(W_DARK, i, j, 84, 0.06)


def wolf_leg(top, bottom, fore):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            return W_TAWNY
        if fore and face == "front" and t > 0.15:
            return fur(lerp(W_TAWNY, W_DARK, 0.6), i, j, 91, 0.07)  # dark stripe down the foreleg
        if face == "left" or (not fore and face == "back"):  # inner side (mirrored onto the left leg)
            return fur(W_CREAM, i, j, 92, 0.05)  # inner side / back of the thigh
        return fur(lerp(W_TAWNY, W_BUFF, t), i, j, 93 + fore)
    return mat


def wolf_lower(v, face, x, y, z, i, j):
    if y >= 4:  # paw
        if face == "bottom":
            return PAD
        if face == "front":
            return W_DARK if abs(x) < 0.4 else fur(W_BUFF, i, j, 101, 0.05)  # gap between the toes
        return fur(W_BUFF, i, j, 102, 0.05)
    if face == "front":
        return fur(lerp(W_BUFF, W_TAWNY, 0.4), i, j, 103, 0.06)
    return fur(W_BUFF, i, j, 104, 0.05)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors GreyWolfModel.createBodyLayer
WOLF = {
    "chest": (0, 0, -3, -3, -7, 6, 6, 8, wolf_body(-3, 3)),
    "loin": (0, 14, -2.5, -2.5, 1, 5, 5, 7, wolf_body(-2.5, 2.5)),
    "mane": (28, 0, -3.5, -4, -8, 7, 7, 4, wolf_mane),
    "tail": (52, 15, -1.5, 0, -1.5, 3, 9, 3, wolf_tail),
    "skull": (28, 11, -3, -3, -4, 6, 5, 4, wolf_skull),
    "ruff": (24, 20, -3.5, -1, -1.5, 7, 4, 2, wolf_ruff),
    "snout": (50, 0, -1.5, -1, -8, 3, 2, 4, wolf_snout),
    "jaw": (50, 6, -1.5, 0, -4, 3, 1, 4, wolf_jaw),
    "ear": (48, 11, -1, -3, -0.5, 2, 3, 1, wolf_ear),
    "fore_upper": (0, 26, -1, -1, -1.5, 2, 5, 3, wolf_leg(-1, 4, True)),
    "lower": (10, 26, -1, 0, -1, 2, 4, 2, wolf_lower),
    "paw": (18, 26, -1, 4, -2, 2, 1, 3, wolf_lower),
    "thigh": (28, 26, -1.5, -1, -2, 3, 6, 4, wolf_leg(-1, 5, False)),
}

# ---------------- items ----------------
# Wolf pelt seen from above: pointed ears and muzzle at the top, legs splayed, a long tail hanging down.
PELT_MASK = [
    "....o.....o.....",
    "....oo...oo.....",
    "....xxxxxxx.....",
    ".....xxxxx......",
    "..oo.xxxxx.oo...",
    ".oxxxxxxxxxxxo..",
    "..oxxxxxxxxxo...",
    "....xxxxxxx.....",
    "....xxxxxxx.....",
    "....xxxxxxx.....",
    "..oxxxxxxxxxo...",
    ".oxxxxxxxxxxxo..",
    "..oo.xxxxx.oo...",
    ".......xx.......",
    ".......xx.......",
    ".......oo.......",
]


def wolf_pelt():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mask = {(x, y) for y, row in enumerate(PELT_MASK) for x, c in enumerate(row) if c != "."}
    ragged = {(x, y) for y, row in enumerate(PELT_MASK) for x, c in enumerate(row) if c == "o"}
    for x, y in mask:
        edge = any((x + dx, y + dy) not in mask for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if (x, y) in ragged or edge:
            c = (40, 36, 34) if (x, y) not in ragged or h(x, y, 5) < 0.5 else W_TAWNY
        else:
            d = abs(x - 7.5)  # dark grizzled saddle down the middle, tawny then cream towards the edges
            c = agouti(W_SADDLE, x, y, 201, amt=0.6) if d < 1.6 else agouti(W_TAWNY, x, y, 202, amt=0.35) if d < 3 else fur(W_BUFF, x, y, 203, 0.06)
        if y >= 13:
            c = (28, 26, 25) if y == 15 or (x, y) in ragged else agouti(W_SADDLE, x, y, 204, amt=0.5)  # tail, black tip
        im.putpixel((x, y), (*c[:3], 255))
    for p in ((6, 3), (8, 3)):
        im.putpixel(p, (*EYE_AMBER, 255))
    return im


def grey_wolf_egg():
    D, C, K = "#2c2a28", "#e2d8c4", "#151313"
    ears = [(4, 0), (11, 0), (4, 1), (11, 1)]
    paint = [(4, 0, D), (11, 0, D), (4, 1, D), (11, 1, D), (5, 2, D), (10, 2, D)]
    paint += [(x, y, "#3e3b38") for x in range(6, 10) for y in (2, 3, 4)]  # dark saddle down from the crown
    paint += [(6, 7, "#c48c28"), (9, 7, "#c48c28")]  # amber eyes
    paint += [(x, y, C) for x in range(5, 11) for y in (9, 10, 11)] + [(7, 9, K), (8, 9, K), (7, 12, C), (8, 12, C)]
    spots = [(3, 6, D), (12, 7, D), (3, 11, D), (12, 11, D)]
    return egg(["#b4aa9a", "#9a8f80", "#837866", "#6a604f", "#4a4338", "#2e2a24"], spots, ears, paint)


def cave_lion_egg():
    T, P = "#2e241c", "#ece2ca"
    ears = [(4, 1), (11, 1)]
    paint = [(4, 1, T), (11, 1, T), (6, 6, "#c48c28"), (9, 6, "#c48c28")]
    paint += [(x, y, P) for x in range(5, 11) for y in (9, 10, 11)] + [(7, 9, "#96625a"), (8, 9, "#96625a"), (7, 11, T), (8, 11, T)]
    spots = [(3, 8, "#a08460"), (12, 8, "#a08460"), (4, 12, "#a08460"), (11, 12, "#a08460")]
    return egg(["#e2cfa6", "#cfb88a", "#b89e72", "#9a8058", "#6e5a3c", "#4a3a26"], spots, ears, paint)


def main():
    wolf = paint(WOLF, "wolf", (64, 64))
    save(wolf, "wildlife/grey_wolf.png")
    save(skinned(wolf, WOLF), "skinned/wildlife/grey_wolf.png")
    for name, im in {"wolf_pelt": wolf_pelt(), "grey_wolf_spawn_egg": grey_wolf_egg(), "cave_lion_spawn_egg": cave_lion_egg()}.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(OUT / f"{name}.png")
        print("wrote", name)


if __name__ == "__main__":
    main()
