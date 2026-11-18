"""Generate 9x9 temperature HUD sprites (sun = warm, snowflake = cold, frosted snowflake = freezing) (vanilla HUD style) + optional preview.

Like vanilla hearts/food: background.png holds the dark outline; *_full/*_half are drawn
on top of it and only fill the interior. Half = left half incl. centre column (as heart/half).

Usage: python gen_temperature_hud.py [preview_out.png [vanilla_food_full.png]]
"""
import os
import sys
from PIL import Image

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets",
                   "mechanical", "textures", "gui", "sprites", "hud", "temperature")

# Round slot: X = outline, i = interior. Sun and snowflakes fill the interior; uncovered interior stays dark.
SHAPE = [
    "..XXXXX..",
    ".XiiiiiX.",
    "XiiiiiiiX",
    "XiiiiiiiX",
    "XiiiiiiiX",
    "XiiiiiiiX",
    "XiiiiiiiX",
    ".XiiiiiX.",
    "..XXXXX..",
]
assert len(SHAPE) == 9 and all(len(r) == 9 for r in SHAPE)


def hexc(h):
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


# Sun: W hot centre, o disc, O shaded disc edge, y rays.
SUN = [
    ".........",
    "....y....",
    ".y.ooo.y.",
    "..oWWoo..",
    ".yoWooOy.",
    "..ooooO..",
    ".y.oOO.y.",
    "....y....",
    ".........",
]
# Snowflake: W centre, C arms, c fork tips and diagonal sparks.
FLAKE = [
    ".........",
    "....C....",
    "..c.C.c..",
    "...cCc...",
    ".CCCWCCC.",
    "...cCc...",
    "..c.C.c..",
    "....C....",
    ".........",
]

ICONS = {
    "warm": (SUN, dict(W="fff6b0", o="ffcc33", O="ef8f1c", y="ffaa22")),
    "cold": (FLAKE, dict(W="e4f6ff", C="6cc0f4", c="a8dcff")),
    # freezing: white flake on an ice-blue frosted disc ("." = every uncovered interior pixel)
    "freezing": (FLAKE, {"W": "ffffff", "C": "ffffff", "c": "dff6ff", ".": "2f6a9a"}),
}
for grid, _ in ICONS.values():
    assert len(grid) == 9 and all(len(r) == 9 for r in grid)


def icon(name, half=False):
    grid, pal = ICONS[name]
    im = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    for y in range(9):
        for x in range(9):
            r = grid[y][x]
            if r in pal and SHAPE[y][x] == "i" and not (half and x > 4):
                im.putpixel((x, y), hexc(pal[r]))
    return im


def background():
    im = Image.new("RGBA", (9, 9), (0, 0, 0, 0))
    for y, row in enumerate(SHAPE):
        for x, c in enumerate(row):
            if c == "X":
                im.putpixel((x, y), (0, 0, 0, 255))
            elif c == "i":
                im.putpixel((x, y), (0x28, 0x28, 0x28, 255))
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    sprites = {"background": background()}
    for n in ICONS:
        sprites[f"{n}_full"] = icon(n)
        sprites[f"{n}_half"] = icon(n, half=True)
    for n, im in sprites.items():
        # interior pixels must stay inside the background shape
        if n != "background":
            assert all(im.getpixel((x, y))[3] == 0 or SHAPE[y][x] == "i"
                       for y in range(9) for x in range(9)), n
        im.save(os.path.join(OUT, n + ".png"))
        print("wrote", n + ".png")
    if len(sys.argv) > 1:
        preview(sprites, sys.argv[1], sys.argv[2] if len(sys.argv) > 2 else None)


def preview(sp, out, food_path):
    bg = sp["background"]

    def slot(name):
        im = bg.copy()
        if name:
            im.alpha_composite(sp[name])
        return im

    singles = [slot(None)] + [slot(n) for n in sp if n != "background"]
    if food_path:
        singles.append(Image.open(food_path).convert("RGBA"))
    # rows of 10, right-to-left like food: (cold count, half?, kind)
    rows = [
        ["warm_full"] * 10,
        ["warm_full"] * 6 + ["warm_half"] + [None] * 3,
        ["cold_full"] * 3 + ["cold_half"] + [None] * 6,
        ["cold_full"] * 10,
        ["freezing_full"] * 7 + ["freezing_half"] + [None] * 2,
    ]
    W = 10 * 8 + 4
    canvas = Image.new("RGBA", (max(W, len(singles) * 10 + 2), 12 + len(rows) * 10 + 2), (0x6f, 0x6f, 0x6f, 255))
    for i, im in enumerate(singles):
        canvas.alpha_composite(im, (2 + i * 10, 2))
    for r, row in enumerate(rows):
        for i, n in enumerate(row):  # index 0 is rightmost, like vanilla food
            canvas.alpha_composite(slot(n), (2 + (9 - i) * 8, 14 + r * 10))
    canvas.resize((canvas.width * 8, canvas.height * 8), Image.NEAREST).save(out)
    print("preview", out)


if __name__ == "__main__":
    main()
