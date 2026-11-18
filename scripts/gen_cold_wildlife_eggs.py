"""Spawn egg item textures for the cold-biome wildlife (moose, reindeer), in the style of gen_wildlife_item_textures.py.

Run: python scripts/gen_cold_wildlife_eggs.py
"""
import importlib.util
from pathlib import Path

_spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
wit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wit)


def over(im, pixels):
    """Antler pixels drawn as given (egg() would shift them down and outline them)."""
    for x, y, c in pixels:
        im.putpixel((x, y), wit.hexc(c))
    return im


def moose():
    # broad pale palms either side of the top, a dark ridge, pale grey stockings at the bottom, dark muzzle in the middle
    T, M, D = "#e6dcc0", "#bfa77e", "#7a6248"
    left = [(0, 1, T), (1, 0, T), (2, 1, M), (1, 1, M), (0, 2, M), (1, 2, D), (2, 2, M), (3, 2, D), (3, 3, D)]
    paint = left + [(15 - x, y, c) for x, y, c in left]
    palms = [(x, y) for x, y, _ in paint]
    R = "#1a1310"
    paint += [(7, 2, R), (8, 2, R), (7, 3, R), (8, 3, R), (8, 4, R)]
    paint += [(6, 9, "#5c4c40"), (7, 9, "#6e5c4e"), (8, 9, "#6e5c4e"), (9, 9, "#5c4c40"),
              (6, 10, "#2a201b"), (7, 10, "#5c4c40"), (8, 10, "#5c4c40"), (9, 10, "#2a201b")]
    spots = [(4, 13, "#cfc7ba"), (5, 13, "#cfc7ba"), (10, 13, "#cfc7ba"), (11, 13, "#cfc7ba"), (5, 14, "#aaa296"), (10, 14, "#aaa296")]
    im = wit.egg(["#6e5442", "#5a4334", "#47342a", "#352720", "#261c16", "#18110d"], spots, palms, paint[len(left) * 2:], shift=1)
    return over(im, paint[:len(left) * 2])


def reindeer():
    # tall branched antlers, a white throat band and rump spots on grey-brown
    A, B = "#e8dcc0", "#a88e68"
    left = [(3, 0, A), (2, 1, B), (4, 1, A), (3, 2, B), (5, 2, B)]
    paint = left + [(15 - x, y, c) for x, y, c in left]
    antlers = [(x, y) for x, y, _ in paint]
    W = "#f2efe6"
    paint += [(x, 7, W) for x in range(4, 12)] + [(x, 8, "#d8d2c4") for x in range(5, 11)]
    spots = [(5, 12, W), (10, 12, W), (7, 13, W), (8, 13, W), (6, 4, "#5a4a3e"), (9, 5, "#5a4a3e")]
    im = wit.egg(["#a8988a", "#8e7e70", "#76685c", "#5e5248", "#433a33", "#2e2823"], spots, antlers, paint[len(left) * 2:], shift=1)
    return over(im, paint[:len(left) * 2])


if __name__ == "__main__":
    for name, im in (("moose_spawn_egg", moose()), ("reindeer_spawn_egg", reindeer())):
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(wit.OUT / f"{name}.png")
        print("wrote", name)
