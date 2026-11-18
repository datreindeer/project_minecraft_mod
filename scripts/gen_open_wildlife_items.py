"""Item textures for the open-country wildlife: horn, and the aurochs / wild horse / mouflon spawn eggs.
Uses the egg painter of gen_wildlife_item_textures.py so the eggs match the forest wildlife ones.

Run: python scripts/gen_open_wildlife_items.py
"""
import importlib.util
from pathlib import Path

_spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
wit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wit)

# Aurochs horn: cream, curving up to a black tip, with a wide open end at the lower left.
HORN = [
    "................",
    "............KK..",
    "...........KTK..",
    "...........KTK..",
    "..........KDLK..",
    "..........KMLK..",
    ".........KMLK...",
    "........KMLLK...",
    ".......KMLLK....",
    "......KMLLHK....",
    "....KKMLLHK.....",
    "...KRMLLHK......",
    "..KRrMLHK.......",
    "..KrRRLK........",
    "...KKKK.........",
    "................",
]
HORN_PAL = {"K": "#2a221c", "T": "#1c1816", "D": "#4a4038", "M": "#b8a888", "L": "#e4d8be", "H": "#f6efe0",
            "R": "#8a7a62", "r": "#5e5244"}


def aurochs():
    C, D = "#e8dcc0", "#2a2420"
    left = [(5, 1, C), (4, 1, C), (3, 1, "#cfc2a4"), (3, 0, D)]  # horns sweep out and up from the poll, black tips
    horns = left + [(15 - x, y, c) for x, y, c in left]
    mask = [(x, y) for x, y, _ in horns]
    stripe = [(7, y, "#9c7c56") for y in range(2, 10)]
    muzzle = [(6, 12, C), (7, 12, C), (8, 12, C), (9, 12, C), (7, 13, D), (8, 13, D)]
    return wit.egg(["#5a4638", "#46362c", "#382a22", "#2a1f19", "#20170f", "#140e0a"], (), mask, horns + stripe + muzzle)


def wild_horse():
    M = "#2e241c"
    mane = [(7, y, M) for y in range(1, 10)] + [(8, y, M) for y in range(1, 6)]
    legs = [(4, 12, M), (5, 13, M), (10, 12, M), (11, 13, M), (6, 14, M), (9, 14, M)]
    muzzle = [(7, 12, "#e6d6b6"), (8, 12, "#e6d6b6"), (7, 13, "#5a4a40"), (8, 13, "#5a4a40")]
    belly = [(4, 10, "#ecdcbc"), (11, 10, "#ecdcbc"), (5, 11, "#ecdcbc"), (10, 11, "#ecdcbc")]
    return wit.egg(["#e2bc84", "#cca06a", "#b48858", "#946c44", "#6e4e30", "#4a3420"], (), (), mane + legs + muzzle + belly)


def mouflon():
    S, W, B = "#eee4cc", "#f6f2ea", "#3a2a20"
    horn = [(1, 4, "#9c8a6c"), (1, 5, "#7c6c54"), (1, 6, "#9c8a6c"), (2, 3, "#b4a282"), (1, 7, "#7c6c54"), (2, 8, "#b4a282")]
    horns = horn + [(15 - x, y, c) for x, y, c in horn]
    saddle = [(4, 7, S), (5, 7, S), (4, 8, S), (10, 7, S), (11, 7, S), (11, 8, S)]
    band = [(3, 10, B), (4, 10, B), (5, 11, B), (12, 10, B), (11, 10, B), (10, 11, B)]
    face = [(6, 12, W), (7, 12, W), (8, 12, W), (9, 12, W), (7, 13, W), (8, 13, W), (7, 14, B), (8, 14, B)]
    return wit.egg(["#c07a48", "#a8643a", "#8e5230", "#724026", "#52301c", "#3a2012"], (), [(x, y) for x, y, _ in horns],
                   horns + saddle + band + face)


def main():
    items = {
        "horn": wit.from_grid(HORN, HORN_PAL),
        "aurochs_spawn_egg": aurochs(),
        "wild_horse_spawn_egg": wild_horse(),
        "mouflon_spawn_egg": mouflon(),
    }
    for name, im in items.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(wit.OUT / f"{name}.png")
        print("wrote", name)


if __name__ == "__main__":
    main()
