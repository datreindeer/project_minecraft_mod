"""Generate 16x16 whole (ungutted) fish item textures and fish bones.

The vanilla raw fish items now stand for the cleaned fillet, so the whole fish share a
bigger, glossier silhouette with eye, gills, fins and a wet sheen, coloured per species.

Run: python scripts/gen_fish_textures.py
"""
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/item"


def hexc(h):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), 255)


def from_grid(rows, pal):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), hexc(pal[ch]))
    return im


# Head bottom-left, tail top-right, like the vanilla fish items.
# o outline, d back, m flank, b belly, h wet sheen, s species marking, e eye, w eye glint,
# g gill, f fin, F fin shadow, t tail, T tail ray
FISH = [
    "...........oo...",
    "..........otto..",
    "..........ottTo.",
    "........oo.otTTo",
    ".......offooTTto",
    "......offoddmoo.",
    ".....ofoddhdmo..",
    "....ofodhdsmbo..",
    "....oddhdmmbbo..",
    "...oddhsmmbbo...",
    "..odwgdmmbbo....",
    "..oedgmmbbo.....",
    "..oddmgmbbo.....",
    "..ombbgbbofo....",
    "...obbbbofFo....",
    "....oooo.oo.....",
]

# Vanilla-derived ramps: whole fish show skin and scales, not the fillet's flesh colours.
SPECIES = {
    "whole_cod": dict(o="#4a3524", d="#7d6a45", m="#b18953", b="#e3d9c0", h="#f4efdf", s="#5c4a2c",
                      e="#000000", w="#ffffff", g="#5c4a2c", f="#986d4e", F="#6b442b", t="#986d4e", T="#6b442b"),
    "whole_salmon": dict(o="#1b2a26", d="#3e4e43", m="#9aa8a2", b="#e2d4d2", h="#f4f7f5", s="#171717",
                         e="#000000", w="#ffffff", g="#3e4e43", f="#99736d", F="#58403c", t="#5d7764", T="#3e4e43"),
    "whole_tropical_fish": dict(o="#692e09", d="#df590a", m="#f46f20", b="#f58a48", h="#fff3ea", s="#fbd8c2",
                                e="#000000", w="#ffffff", g="#fbd8c2", f="#bb502c", F="#692e09", t="#f46f20", T="#bb502c"),
    "whole_pufferfish": dict(o="#83500e", d="#d8951a", m="#ffc908", b="#fce5bc", h="#fffbe8", s="#915b15",
                             e="#000000", w="#ffffff", g="#b67826", f="#429bba", F="#358aa8", t="#7ea8bd", T="#429bba"),
}


def band(*ts):
    """Body pixels across the fish where y - x is in ts (stripes run perpendicular to the spine)."""
    return [(x, y, "s") for y, row in enumerate(FISH) for x, ch in enumerate(row) if ch in "dmbh" and y - x in ts]


# Extra per-species pixels on top of FISH: (x, y, palette char).
MARKS = {
    "whole_cod": [(11, 5, "s"), (8, 6, "s"), (6, 8, "s"), (12, 6, "h")],
    "whole_salmon": [(11, 5, "s"), (8, 6, "s"), (6, 8, "s")],
    "whole_tropical_fish": band(0, 1) + band(-5),
    "whole_pufferfish": [(11, 5, "s"), (8, 6, "s"), (6, 8, "s"), (9, 8, "s"),
                         (3, 7, "o"), (15, 8, "o"), (13, 10, "o")],
}

BONES = [
    "............c..c",
    "............b.b.",
    "............bb..",
    "........c...bbbc",
    ".....c...b.b....",
    "......b...b.....",
    ".......b.b.b....",
    "....c...b...c...",
    ".....b.b.b......",
    "......b...b.....",
    "...cbb.b...c....",
    "..cbabc.b.......",
    ".cbbbbd..c......",
    ".bbbdd..........",
    "..dd............",
    "................",
]
BONE_PAL = {"a": "#3a3a36", "b": "#e8e5d2", "c": "#a9a89a", "d": "#7b7e6b"}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, pal in SPECIES.items():
        rows = [list(r) for r in FISH]
        for x, y, ch in MARKS[name]:
            rows[y][x] = ch
        from_grid(["".join(r) for r in rows], pal).save(OUT / f"{name}.png")
    from_grid(BONES, BONE_PAL).save(OUT / "fish_bones.png")
    print("wrote", ", ".join([*SPECIES, "fish_bones"]), "to", OUT)


if __name__ == "__main__":
    main()
