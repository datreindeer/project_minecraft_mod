"""Mod icon: crossed flint spear + bronze sword over a stone-age sunset (pixel art, 256x256)."""
from PIL import Image, ImageOps
T = "src/main/resources/assets/mechanical/textures/item/"
N, S = 32, 8  # background grid 32x32, scaled 8x -> 256

bg = Image.new("RGBA", (N, N))
sky = [(40, 26, 58), (92, 44, 70), (170, 70, 60), (224, 128, 64), (246, 196, 104)]
for y in range(N):
    for x in range(N):
        c = sky[min(4, y * 5 // 22)]
        if (x - 16) ** 2 + (y - 15) ** 2 <= 64: c = (255, 226, 150)       # sun
        if y >= 22 + (2 if x < 9 else 0) - (1 if 12 < x < 22 else 0): c = (54, 40, 34)  # hills
        if y >= 27: c = (36, 28, 24)                                       # ground
        corner = min(x, N - 1 - x) + min(y, N - 1 - y) < 3                 # rounded corners
        bg.putpixel((x, y), (0, 0, 0, 0) if corner else c + (255,))
icon = bg.resize((N * S,) * 2, Image.NEAREST)

for name, flip in (("flint_spear", False), ("bronze_sword", True)):
    tex = Image.open(T + name + ".png").convert("RGBA")
    if flip: tex = ImageOps.mirror(tex)
    out_l = Image.new("RGBA", (18, 18)); a = tex.getchannel("A")  # 1px dark outline so blades read against the sky
    for dx, dy in ((0, 1), (2, 1), (1, 0), (1, 2)): out_l.paste((28, 18, 20, 255), (dx, dy), a)
    out_l.alpha_composite(tex, (1, 1)); tex = out_l.resize((198, 198), Image.NEAREST)
    shadow = Image.new("RGBA", tex.size, (0, 0, 0, 140)); shadow.putalpha(tex.getchannel("A").point(lambda a: 110 if a else 0))
    icon.alpha_composite(shadow, (35, 35)); icon.alpha_composite(tex, (29, 29))

out = "src/main/resources/jackt.png"
icon.save(out); print("wrote", out)
