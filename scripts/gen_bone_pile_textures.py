"""Generates the 16x16 bone pile textures (bone surface + skull with eye sockets).

Run: python scripts/gen_bone_pile_textures.py
Skull UV regions used by models/block/bone_pile.json:
  [0,0,4,5]   cranium top (sockets hinted along the west column)
  [4,0,9,3]   skull front (west face): brow, two eye sockets, cheeks
  [9,0,12,2]  snout front: nasal hole + teeth row
  [0,6,5,9]   plain skull sides / back
"""
import random
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/block"

HIGHLIGHT = (240, 237, 221)
BASE = (227, 223, 201)
MID = (214, 209, 184)
YELLOW = (222, 213, 176)
SHADE = (198, 192, 163)
DEEP = (176, 169, 139)
CRACK = (146, 138, 112)
SOCKET = (52, 45, 38)
SOCKET_IN = (84, 74, 60)


def bone(seed):
    rnd = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            r = rnd.random()
            c = BASE if r < 0.5 else MID if r < 0.72 else YELLOW if r < 0.86 else HIGHLIGHT if r < 0.95 else SHADE
            px[x, y] = c + (255,)
    # a few yellowed blotches
    for _ in range(4):
        cx, cy = rnd.randrange(16), rnd.randrange(16)
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            px[(cx + dx) % 16, (cy + dy) % 16] = YELLOW + (255,)
    # short dark cracks with a lit pixel beside them
    for _ in range(5):
        x, y = rnd.randrange(16), rnd.randrange(16)
        step = rnd.choice(((1, 1), (1, -1), (1, 0), (0, 1)))
        for i in range(rnd.randint(2, 3)):
            px[x % 16, y % 16] = (CRACK if i else DEEP) + (255,)
            px[(x + 1) % 16, (y - 1) % 16] = HIGHLIGHT + (255,)
            x, y = x + step[0], y + step[1]
            if rnd.random() < 0.4:
                y += 1
    return img


def skull():
    img = bone(7)
    px = img.load()
    put = lambda x, y, c: px.__setitem__((x, y), c + (255,))
    # cranium top: lit dome, shaded rim, socket hints on west column (u=0, v=1 and v=3)
    for y in range(5):
        for x in range(4):
            put(x, y, HIGHLIGHT if 1 <= x <= 2 and 1 <= y <= 3 else BASE if (x + y) % 3 else MID)
    put(2, 2, BASE); put(3, 2, CRACK)
    put(0, 1, SOCKET_IN); put(0, 3, SOCKET_IN); put(0, 2, SHADE)
    # skull front (west face, 5 x 3): brow / eyes / cheeks
    for x in range(4, 9):
        put(x, 0, HIGHLIGHT if x in (5, 7) else BASE)
        put(x, 1, SOCKET if x in (5, 7) else MID)
        put(x, 2, SHADE if x in (5, 7) else BASE)
    put(6, 2, MID)
    # snout front (3 x 2): nasal hole, teeth
    put(9, 0, BASE); put(10, 0, SOCKET); put(11, 0, BASE)
    put(9, 1, HIGHLIGHT); put(10, 1, DEEP); put(11, 1, HIGHLIGHT)
    # plain sides: lighter top row, shaded bottom row
    for x in range(5):
        put(x, 6, HIGHLIGHT if x % 2 else BASE)
        put(x, 7, MID if x != 2 else CRACK)
        put(x, 8, SHADE)
    return img


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    bone(3).save(OUT / "bone_pile_bone.png")
    skull().save(OUT / "bone_pile_skull.png")
    assert Image.open(OUT / "bone_pile_skull.png").size == (16, 16)
    print("wrote", OUT / "bone_pile_bone.png", OUT / "bone_pile_skull.png")
