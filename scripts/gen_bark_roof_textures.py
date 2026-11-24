# Bark Roof textures: overlapping rows of bark shingles cut from each wood's own log bark (BarkRoofBlock, gen_bark_roof.py).
# Four rows of shingles 4 px high, each row shifted against the one above; every shingle is a piece of the log's side
# texture (grain running down the roof), a little lighter or darker than its neighbours, shaded under the row above,
# lit along its lower edge, with dark gaps between shingles. Tiles in both directions.
# usage: python gen_bark_roof_textures.py <vanilla block texture dir> <out block texture dir> [preview png]
import os
import sys
from PIL import Image

src, out = sys.argv[1:3]
prev = sys.argv[3] if len(sys.argv) > 3 else None
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar",
         "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew", "rowan", "bird_cherry", "wild_apple", "plum", "olive", "date_palm"]
ROW_WIDTHS = [[5, 6, 5], [6, 5, 5], [5, 5, 6], [6, 5, 5]]  # shingle widths per row, each row sums to 16
ROW_SHIFT = [0, 3, 1, 4]


def tex(name):
    """A vanilla texture, or (mod woods) the one painted by gen_wood_textures.py in the output folder."""
    p = os.path.join(src, f"{name}.png")
    return p if os.path.exists(p) else os.path.join(out, f"{name}.png")


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3]) + (255,)


def roof(bark):
    img = Image.new("RGBA", (16, 16))
    for row in range(4):
        # which shingle each column belongs to (with the row's shift), and where the gaps are
        owner, edge = [0] * 16, [False] * 16
        x = ROW_SHIFT[row]
        for i, wdt in enumerate(ROW_WIDTHS[row]):
            for k in range(wdt):
                owner[(x + k) % 16] = i
            edge[(x + wdt - 1) % 16] = True  # the last column of a shingle is the gap to the next
            x += wdt
        for y in range(4):
            for col in range(16):
                i = owner[col]
                # a piece of bark for this shingle: its own spot on the log, grain running down the roof
                sx, sy = (col + 5 * i + 7 * row) % 16, (y + 4 * ((row + i) % 4)) % 16
                c = bark.getpixel((sx, sy))
                f = (0.92, 1.06, 0.98)[(i + row) % 3]  # neighbouring shingles differ a little
                if y == 0:
                    f *= 0.68  # in the shadow of the row above
                elif y == 3:
                    f *= 1.12  # the lower edge catches the light
                if edge[col]:
                    f *= 0.55 if y < 3 else 0.7
                img.putpixel((col, 4 * row + y), shade(c, f))
    return img


sheet = Image.new("RGBA", (16 * len(WOODS), 32), (0, 0, 0, 0)) if prev else None
for n, wood in enumerate(WOODS):
    bark = Image.open(tex(f"{wood}_log")).convert("RGBA")
    img = roof(bark)
    img.save(os.path.join(out, f"{wood}_bark_roof.png"))
    if sheet:
        sheet.paste(bark, (16 * n, 0))
        sheet.paste(img, (16 * n, 16))
if sheet:
    sheet.resize((sheet.width * 8, sheet.height * 8), Image.NEAREST).save(prev)
print(f"bark roof textures: {len(WOODS)}")
