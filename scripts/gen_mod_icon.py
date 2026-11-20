"""Mod icon: crossed flint spear + bronze sword (pixel art, 256x256). Usage: gen_mod_icon.py [stone|glow|forest|none] [out]"""
import random, sys
from PIL import Image, ImageOps
T = "src/main/resources/assets/mechanical/textures/item/"
N, S = 32, 8  # background grid 32x32, scaled 8x -> 256

def background(kind):
    rnd = random.Random(7)
    bg = Image.new("RGBA", (N, N))
    if kind == "none": return bg
    for y in range(N):
        for x in range(N):
            d = ((x - 15.5) ** 2 + (y - 15.5) ** 2) ** 0.5 / 22  # 0 centre .. ~1 corner
            n = rnd.randint(-6, 6)
            if kind == "stone":   base, edge = (96, 92, 88), (38, 36, 36)
            elif kind == "glow":  base, edge = (200, 110, 50), (22, 16, 24)
            else:                 base, edge = (70, 104, 58), (20, 34, 24)
            c = tuple(max(0, min(255, int(b + (e - b) * min(1, d * 1.15)) + n)) for b, e in zip(base, edge))
            corner = min(x, N - 1 - x) + min(y, N - 1 - y) < 3  # rounded corners
            bg.putpixel((x, y), (0, 0, 0, 0) if corner else c + (255,))
    return bg

def icon(kind):
    img = background(kind).resize((N * S,) * 2, Image.NEAREST)
    for name, flip in (("flint_spear", False), ("bronze_sword", True)):
        tex = Image.open(T + name + ".png").convert("RGBA")
        if flip: tex = ImageOps.mirror(tex)
        out_l = Image.new("RGBA", (18, 18)); a = tex.getchannel("A")  # 1px dark outline so blades read against the background
        for dx, dy in ((0, 1), (2, 1), (1, 0), (1, 2)): out_l.paste((28, 18, 20, 255), (dx, dy), a)
        out_l.alpha_composite(tex, (1, 1)); tex = out_l.resize((198, 198), Image.NEAREST)
        shadow = Image.new("RGBA", tex.size, (0, 0, 0, 0)); shadow.putalpha(tex.getchannel("A").point(lambda v: 110 if v else 0))
        img.alpha_composite(shadow, (35, 35)); img.alpha_composite(tex, (29, 29))
    return img

if __name__ == "__main__":
    kind = sys.argv[1] if len(sys.argv) > 1 else "stone"
    out = sys.argv[2] if len(sys.argv) > 2 else "src/main/resources/jackt.png"
    icon(kind).save(out); print("wrote", out)
