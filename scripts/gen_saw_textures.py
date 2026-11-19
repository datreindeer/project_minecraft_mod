"""The Bronze Saw's cut in a fallen log (models: gen_data.py, fallen logs):
- saw_kerf: the walls and floor of the kerf, freshly sawn end grain in shadow, with the saw teeth's fine scoring.
- sawdust: pale sawdust spilled at the lips of the cut; dense against the kerf (rows 15-14), thinning out by row 13.
usage: python scripts/gen_saw_textures.py"""
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/block"


def kerf():
    rnd = random.Random(7)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        stroke = rnd.randint(-6, 6)  # each saw stroke scores a slightly different band
        for x in range(16):
            v = 52 + stroke + rnd.randint(-5, 5) - (8 if y in (0, 15) else 0)  # darkest at the edges of the cut
            img.putpixel((x, y), (v + 16, v + 4, v - 10, 255))
    return img


def sawdust():
    rnd = random.Random(11)
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, chance in ((15, 0.85), (14, 0.5), (13, 0.2)):
        for x in range(16):
            if rnd.random() < chance:
                v = rnd.randint(-14, 10)
                img.putpixel((x, y), (214 + v, 186 + v, 140 + v, 255))
    return img


if __name__ == "__main__":
    kerf().save(OUT / "saw_kerf.png")
    sawdust().save(OUT / "sawdust.png")
    print("saw textures ok")
