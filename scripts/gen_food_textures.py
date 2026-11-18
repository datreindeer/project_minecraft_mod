"""Placeholder textures for meat scraps, hides, wool thread, flint spear, tool station and drying rack.
Never overwrites existing files."""
import colorsys
import os
import random
from PIL import Image, ImageDraw

V = r"C:\Users\Yngve\AppData\Local\Temp\claude\43d83a9a335e657f\9ee37f73-4613-4afd-a1ce-400b3a021c61\scratchpad\src\assets\minecraft\textures"
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "mechanical", "textures")
PREVIEW = r"C:\Users\Yngve\AppData\Local\Temp\claude\43d83a9a335e657f\9ee37f73-4613-4afd-a1ce-400b3a021c61\scratchpad\textures_preview.png"

created, skipped, preview = [], [], []


def van(path):
    return Image.open(os.path.join(V, path)).convert("RGBA")


def save(img, rel):
    p = os.path.normpath(os.path.join(OUT, rel))
    preview.append((rel, img if not os.path.exists(p) else Image.open(p).convert("RGBA")))
    if os.path.exists(p):
        skipped.append(p)
        return
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    created.append(p)


def map_px(img, fn):
    img = img.copy()
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            if px[x, y][3]:
                px[x, y] = fn(px[x, y])
    return img


def pixels(img):
    return [img.getpixel((x, y)) for y in range(img.height) for x in range(img.width)]


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c[:3]) + (255,)


# --- 1. meat scraps: two shrunken copies offset diagonally ---
def scraps(src):
    piece = src.crop(src.getbbox())
    w, h = piece.size
    big = piece.resize((max(1, round(w * 0.6)), max(1, round(h * 0.6))), Image.NEAREST)
    small = piece.resize((max(1, round(w * 0.5)), max(1, round(h * 0.5))), Image.NEAREST).transpose(Image.FLIP_LEFT_RIGHT)
    out = Image.new("RGBA", (16, 16))
    out.alpha_composite(small, (16 - small.width - 2, 2))
    out.alpha_composite(big, (2, 16 - big.height - 2))
    return out


def recolor_hsv(img, dh, ds, dv):
    def f(c):
        h, s, v = colorsys.rgb_to_hsv(*(x / 255 for x in c[:3]))
        r, g, b = colorsys.hsv_to_rgb((h + dh) % 1, min(1, s * ds), v * dv)
        return (int(r * 255), int(g * 255), int(b * 255), c[3])
    return map_px(img, f)


for raw, cooked in [("beef", "cooked_beef"), ("porkchop", "cooked_porkchop"), ("mutton", "cooked_mutton"),
                    ("chicken", "cooked_chicken"), ("rabbit", "cooked_rabbit")]:
    save(scraps(van(f"item/{raw}.png")), f"item/{raw}_scraps.png")
    save(scraps(van(f"item/{cooked}.png")), f"item/{cooked}_scraps.png")
save(recolor_hsv(scraps(van("item/beef.png")), -0.03, 0.85, 0.7), "item/game_meat_scraps.png")
save(recolor_hsv(scraps(van("item/cooked_beef.png")), 0.0, 0.9, 0.7), "item/cooked_game_meat_scraps.png")

# --- 2/3. hides: irregular silhouette, darker rim, specks ---
HIDE = ["..XXX....XXX....",
        "..XXXXXXXXXXX...",
        "...XXXXXXXXXX...",
        ".XXXXXXXXXXXXX..",
        "XXXXXXXXXXXXXXX.",
        ".XXXXXXXXXXXXXX.",
        "..XXXXXXXXXXXX..",
        "..XXXXXXXXXXX...",
        "..XXXXXXXXXXXX..",
        ".XXXXXXXXXXXXXX.",
        "XXXXXXXXXXXXXXX.",
        ".XXXXXXXXXXXXX..",
        "..XXXXXXXXXXX...",
        "..XXX.....XXX...",
        "................",
        "................"]
HIDE = [r[:15].rjust(16, ".") for r in HIDE]  # centre-ish
HIDE = HIDE[-1:] + HIDE[:-1]                 # shift down one row


def hide(base, specks, speck_col, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    solid = {(x, y) for y in range(16) for x in range(16) if HIDE[y][x] == "X"}
    for x, y in solid:
        edge = any((x + dx, y + dy) not in solid for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        f = rnd.choice((0.72, 0.78)) if edge else rnd.choice((0.92, 0.97, 1.0, 1.0, 1.04))
        px[x, y] = shade(base, f)
    inner = sorted(p for p in solid if all((p[0] + dx, p[1] + dy) in solid for dx in (-1, 1) for dy in (-1, 1)))
    for p in rnd.sample(inner, specks):
        px[p] = speck_col
    return img


save(hide((196, 150, 120), 7, (110, 80, 60, 255), 1), "item/raw_hide.png")
save(hide((225, 200, 160), 2, (175, 150, 115, 255), 1), "item/scraped_hide.png")

# --- 4. wool thread: string recoloured off-white ---
s = van("item/string.png")
mx = max(lum(c) for c in pixels(s) if c[3])
save(map_px(s, lambda c: shade((235, 235, 230), 0.7 + 0.3 * lum(c) / mx)), "item/wool_thread.png")


# --- 5. flint spear: recolour grey head only ---
def is_wood(c):
    h, sat, _ = colorsys.rgb_to_hsv(*(x / 255 for x in c[:3]))
    return 0.02 <= h <= 0.15 and sat > 0.25


def flint(img):
    greys = [lum(c) for c in pixels(img) if c[3] and not is_wood(c)]
    mean = sum(greys) / len(greys)
    return map_px(img, lambda c: c if is_wood(c) else shade((72, 74, 86), lum(c) / mean))


save(flint(van("item/stone_spear.png")), "item/flint_spear.png")
save(flint(van("item/stone_spear_in_hand.png")), "item/flint_spear_in_hand.png")

# --- 6-8. tool station ---
FLINT, FLINT_HI, STONE, STONE_DK, WOOD = (72, 74, 86, 255), (110, 113, 128, 255), (125, 125, 125, 255), (90, 90, 90, 255), (110, 80, 45, 255)


def stamp(img, shape, outline=(45, 32, 20, 255)):
    """shape: {(x, y): colour}. Draws a 1px dark outline around it first so it reads on busy wood."""
    for x, y in shape:
        for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if q not in shape and 0 <= q[0] < 16 and 0 <= q[1] < 16:
                img.putpixel(q, outline)
    for q, c in shape.items():
        img.putpixel(q, c)


def shape(*groups):
    return {p: c for pts, c in groups for p in pts}


top = map_px(van("block/oak_planks.png"), lambda c: shade(c, 0.85))
ImageDraw.Draw(top).rectangle((0, 0, 15, 15), outline=shade((162, 130, 78), 0.5))
# knife, handle bottom-left, 2px-wide blade up-right
stamp(top, shape(([(3, 12), (4, 11)], WOOD),
                 ([(5, 10), (6, 9), (7, 8), (8, 7), (6, 10), (7, 9), (8, 8), (9, 6)], FLINT),
                 ([(6, 9), (7, 8), (8, 7)], FLINT_HI)))
# axe head wedge, right side
stamp(top, shape(([(x, y) for x in range(10, 14) for y in range(8, 12)] + [(13, 7), (13, 12)], STONE),
                 ([(10, 8), (10, 11), (11, 11)], STONE_DK)))
save(top, "block/tool_station_top.png")

side = van("block/crafting_table_side.png")
TWINE, TWINE_DK = (170, 130, 80, 255), (125, 92, 55, 255)
# pickaxe hanging left of the post
stamp(side, shape(([(2, 5), (3, 4), (4, 4), (5, 4), (1, 6), (5, 5)], STONE),
                  ([(3, 5), (3, 6), (3, 7), (3, 8), (3, 9), (3, 10)], WOOD)))
# twine coil right of the post
ring = [(10, 4), (11, 4), (12, 4), (9, 5), (13, 5), (9, 6), (13, 6), (9, 7), (13, 7), (10, 8), (11, 8), (12, 8)]
stamp(side, shape((ring, TWINE), ([(10, 5), (12, 5), (10, 7), (12, 7), (11, 6)], TWINE_DK),
                  ([(10, 6), (11, 5), (12, 6), (11, 7)], TWINE)))
save(side, "block/tool_station_side.png")

fp = os.path.join(V, "block", "fletching_table_front.png")
front = van("block/fletching_table_front.png" if os.path.exists(fp) else "block/crafting_table_front.png")
# small flint spear, diagonal
stamp(front, shape(([(3, 13), (4, 12), (5, 11), (6, 10), (7, 9), (8, 8)], WOOD),
                   ([(9, 7), (10, 6), (9, 6), (10, 7), (11, 5), (10, 5), (11, 6), (12, 4)], FLINT),
                   ([(10, 5), (11, 5)], FLINT_HI)))
save(front, "block/tool_station_front.png")

# --- 9. drying rack frame ---
lp = os.path.join(V, "block", "stripped_oak_log.png")
save(van("block/stripped_oak_log.png" if os.path.exists(lp) else "block/oak_planks.png"), "block/drying_rack.png")


# --- 10. hide on rack: 12x12 inside a 2px transparent margin ---
def rack_hide(pal, blotch, seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    for y in range(2, 14):
        for x in range(2, 14):
            edge = x in (2, 13) or y in (2, 13)
            img.putpixel((x, y), pal[0] if edge else rnd.choice(pal[1:]))
    for _ in range(4):
        cx, cy = rnd.randint(4, 10), rnd.randint(4, 10)
        for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1), (rnd.choice((-1, 2)), rnd.randint(0, 1))):
            img.putpixel((cx + dx, cy + dy), blotch)
    return img


wet_base = (205, 165, 132)
save(rack_hide([shade(wet_base, 0.75), wet_base, wet_base, shade(wet_base, 0.95), (225, 200, 160, 255)],
               shade(wet_base, 0.72), 3), "block/drying_rack_hide_wet.png")
leather = sorted({c for c in pixels(van("item/leather.png")) if c[3]}, key=lum)
save(rack_hide([leather[0]] + leather[len(leather) // 4: 3 * len(leather) // 4 + 1], leather[len(leather) // 6], 4),
     "block/drying_rack_hide_dry.png")

# --- preview sheet ---
S, cols = 8, 6
rows = (len(preview) + cols - 1) // cols
sheet = Image.new("RGBA", (cols * (16 * S + 8) + 8, rows * (16 * S + 8) + 8), (128, 128, 128, 255))
for i, (_, im) in enumerate(preview):
    sheet.alpha_composite(im.resize((16 * S, 16 * S), Image.NEAREST), (8 + (i % cols) * (16 * S + 8), 8 + (i // cols) * (16 * S + 8)))
sheet.save(PREVIEW)

for i, (rel, _) in enumerate(preview):
    print(i, rel)
print("created:", *created, sep="\n  ")
print("skipped:", *skipped, sep="\n  ")
