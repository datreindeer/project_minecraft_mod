"""Paint the night predators' 16x16 items: Wolf Pelt and the grey wolf and cave lion spawn eggs. The grey wolf's model
and skin are made by gen_pred_models.py, the cave lion's by gen_savanna_models.py.

Usage: python scripts/gen_predator_textures.py
Deterministic.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp  # noqa: E402
from gen_wildlife_item_textures import OUT, egg  # noqa: E402

# ---------------- grey wolf ----------------
W_SADDLE = (58, 56, 54)
W_TAWNY = (150, 122, 88)
W_BUFF = (186, 162, 124)
W_CREAM = (226, 216, 196)
W_DARK = (30, 28, 27)
EYE_AMBER = (196, 140, 40)


def agouti(base, i, j, salt, dark=W_DARK, light=W_CREAM, amt=0.35):
    """Banded guard hairs: each texel is a hair tip that is dark, pale-banded or base coloured."""
    r = h(i, j, salt)
    if r < amt * 0.45:
        return fur(lerp(base, dark, 0.65), i, j, salt + 1, 0.08)
    if r > 1 - amt * 0.4:
        return fur(lerp(base, light, 0.5), i, j, salt + 2, 0.05)
    return fur(base, i, j, salt + 3, 0.08)


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
    for name, im in {"wolf_pelt": wolf_pelt(), "grey_wolf_spawn_egg": grey_wolf_egg(), "cave_lion_spawn_egg": cave_lion_egg()}.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(OUT / f"{name}.png")
        print("wrote", name)


if __name__ == "__main__":
    main()
