# Firewood textures: 1-3 split logs, colours taken from each wood's vanilla log textures.
# usage: python gen_firewood.py <vanilla block texture dir> <out item texture dir> <preview png>
import os, sys
from PIL import Image
src, out, prev = sys.argv[1:4]
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar", "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew", "rowan", "bird_cherry", "wild_apple", "plum", "olive", "date_palm"]
# mod woods have no vanilla texture: read the log textures painted by gen_wood_textures.py
def tex(name): p = f"{src}/{name}.png"; return p if os.path.exists(p) else os.path.join(os.path.dirname(os.path.abspath(out)), "block", f"{name}.png")

def palette(img):
    px = sorted({img.getpixel((x, y))[:3] for x in range(16) for y in range(16)}, key=lambda c: sum(c))
    return px[len(px) // 6], px[len(px) // 2], px[5 * len(px) // 6]  # dark, mid, light

def piece(canvas, x0, y0, bark, end):
    """A split log lying diagonally: 9px bark body with an end-grain face at the lower-left."""
    dark, mid, light = bark
    edark, emid, elight = end
    for i in range(9):          # body runs up-right
        for j in range(4):      # thickness
            x, y = x0 + 3 + i, y0 + 3 - i // 2 + j
            c = light if j == 0 else dark if j == 3 else mid
            if (i + j) % 4 == 0 and j in (1, 2): c = dark
            canvas.putpixel((x, y), c + (255,))
    for j in range(5):          # end grain cap
        for i in range(3):
            c = elight if (i, j) not in ((0, 0), (0, 4)) else None
            if c is None: continue
            if i == 1 and j == 2: c = edark
            elif i == 2 or j in (0, 4): c = emid
            canvas.putpixel((x0 + i, y0 + 2 + j), c + (255,))

LAYOUT = {1: [(2, 6)], 2: [(1, 8), (4, 3)], 3: [(0, 9), (4, 5), (2, 1)]}
sheet = Image.new("RGBA", (16 * 3 * 4, 16 * len(WOODS) * 4), (200, 200, 200, 255))
for row, wood in enumerate(WOODS):
    bark = palette(Image.open(tex(f"{wood}_log")).convert("RGBA"))
    end = palette(Image.open(tex(f"{wood}_log_top")).convert("RGBA").crop((4, 4, 12, 12)).resize((16, 16)))
    for n, spots in LAYOUT.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        for x, y in spots: piece(img, x, y, bark, end)
        img.save(f"{out}/{wood}_firewood_{n}.png")
        big = img.resize((64, 64), Image.NEAREST); sheet.paste(big, ((n - 1) * 64, row * 64), big)
sheet.save(prev)
print("ok")
