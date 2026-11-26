"""The quest screen's sprites (client/quests/QuestScreen), in the GUI style of gen_gui_textures.py:
gui/sprites/quest/{task,goal,challenge}_{done,ready,locked}.png (26x26 nodes), a halo per shape, the tick and padlock
badges and the tiled board behind the graph.

Stands alone (only borrows gen_gui_textures' palette and helpers); run after gen_gui_textures.py.
Run: python scripts/gen_quest_textures.py
"""
import json
import math

from gen_gui_textures import MOD, GRAIN, OUTLINE, noise, new, rgb, shade, pattern

OUT = MOD / "sprites/quest"
S = 26


def inside(shape, x, y, grow=0.0):
    """Whether pixel (x, y) of a 26x26 node is inside its shape, grown by `grow` pixels (negative: shrunk)."""
    dx, dy = abs(x + 0.5 - S / 2), abs(y + 0.5 - S / 2)
    if shape == "task":  # a square with rounded corners
        half, r = 12 + grow, max(0.0, 4 + grow)
        if dx > half or dy > half:
            return False
        return math.hypot(max(0.0, dx - (half - r)), max(0.0, dy - (half - r))) <= r + 0.15
    if shape == "goal":  # a roundel
        return math.hypot(dx, dy) <= 12.9 + grow
    # challenge: an eight-pointed medal, a square and a diamond laid over each other
    square = max(dx, dy) <= 10.6 + grow
    diamond = dx + dy <= 14.6 + grow * 1.4 and max(dx, dy) <= 13 + grow
    return square or diamond


# outline is always the dark; rim light and dark, face, the face's pressed centre
STATES = {
    "done": (rgb("b9e08a"), rgb("5f8a3c"), rgb("3c4f26"), rgb("2c3b1c")),
    "ready": (rgb("ffe08a"), rgb("a87a2c"), rgb("6b4b28"), rgb("523a1f")),
    "locked": (rgb("66584a"), rgb("3b3128"), rgb("2a2119"), rgb("1f1812")),
}


def node(shape, state):
    light, dark, face, centre = STATES[state]
    im = new(S, S)
    for y in range(S):
        for x in range(S):
            if not inside(shape, x, y):
                continue
            if not inside(shape, x, y, -1.1):
                c = OUTLINE
            elif not inside(shape, x, y, -3.0):  # the rim, lit from the top left
                t = max(0.0, min(1.0, (x + y - 8) / 34))
                c = tuple(round(a + (b - a) * t) for a, b in zip(light, dark))
            elif not inside(shape, x, y, -4.0):
                c = OUTLINE
            else:
                base = centre if inside(shape, x, y, -6.0) else face
                c = shade(base, round(GRAIN[x + len(shape) * 31, y + len(state) * 17] * 6))
                if not inside(shape, x, y - 1, -4.0):  # the shadow the rim casts into the face
                    c = shade(c, -18)
            im.putpixel((x, y), c)
    return im


def halo(shape):
    """A soft white ring around a node (34x34, the node at 4,4); the screen tints it."""
    im = new(S + 8, S + 8)
    for y in range(S + 8):
        for x in range(S + 8):
            for grow, alpha in ((1.0, 235), (2.0, 150), (3.0, 75), (4.0, 28)):
                if inside(shape, x - 4, y - 4, grow):
                    if not inside(shape, x - 4, y - 4, 0):
                        im.putpixel((x, y), (255, 255, 255, alpha))
                    break
    return im


def check_badge():
    im = new(11, 11)
    pattern(im, 0, 0, ["...ooooo...",
                       ".ooGGGGGoo.",
                       ".oGGGGGGwo.",
                       "oGGGGGGwwGo",
                       "oGGGGGwwGGo",
                       "oGwGGwwGGGo",
                       "oGwwwwGGGGo",
                       "oGGwwGGGGGo",
                       ".oGGGGGGGo.",
                       ".ooGGGGGoo.",
                       "...ooooo..."], {"o": OUTLINE, "G": rgb("4f8a2e"), "w": rgb("f4ffe0")})
    return im


def lock_badge():
    im = new(9, 11)
    pattern(im, 0, 0, ["..ooooo..",
                       ".oommmoo.",
                       ".om.o.mo.",
                       ".omo.omo.",
                       "ooooooooo",
                       "oLLLLLLLo",
                       "oLLLoLLLo",
                       "oLLoooLLo",
                       "oLLLoLLLo",
                       "oDDDDDDDo",
                       "ooooooooo"], {"o": OUTLINE, "m": rgb("a89a86"), "L": rgb("8a7658"), "D": rgb("67563e")})
    return im


def board():
    """The board behind the graph: dark, quiet hide (tiles every 64 px) with a faint peg every 32 px."""
    grain = noise(64, 64, 64, 64, seed=23)
    im = new(64, 64)
    base = rgb("1b130d")
    for y in range(64):
        for x in range(64):
            im.putpixel((x, y), shade(base, round(grain[x, y] * 3.2)))
    for px in (16, 48):
        for py in (16, 48):
            im.putpixel((px, py), rgb("3a2c1e"))
            im.putpixel((px, py + 1), rgb("0e0a06"))
    return im


def save(name, im, scaling=None):
    OUT.mkdir(parents=True, exist_ok=True)
    im.save(OUT / f"{name}.png")
    if scaling:
        (OUT / f"{name}.png.mcmeta").write_text(json.dumps({"gui": {"scaling": scaling}}, indent=2) + "\n")


if __name__ == "__main__":
    for shape in ("task", "goal", "challenge"):
        for state in STATES:
            save(f"{shape}_{state}", node(shape, state))
        save(f"{shape}_halo", halo(shape))
    save("check", check_badge())
    save("lock", lock_badge())
    save("board", board(), {"type": "tile", "width": 64, "height": 64})
    print("quest sprites:", len(list(OUT.glob("*.png"))))
