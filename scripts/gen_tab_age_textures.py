"""Age icons for the Tab list (client/clan/TribeTabList): 9x9 GUI sprites, one per age.
Stone: a knapped flint hand axe. Bronze: a bronze axe head. Iron: an iron axe head. Steam: a cogwheel. Electric: a lightning bolt.
Standalone texture script. Run: python scripts/gen_tab_age_textures.py"""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/gui/sprites/tab"

# '.' empty, letters index the palette: o outline, d dark, m mid, l light, h highlight, w wood
ICONS = {
    "stone": ({"o": (40, 36, 34), "d": (88, 84, 82), "m": (122, 118, 112), "l": (158, 154, 146), "h": (196, 192, 184)}, [
        "...ooo...",
        "..odmlo..",
        ".odmmlho.",
        ".odmdmlo.",
        "odmmdmlho",
        "odmdmmmlo",
        ".odmmdmo.",
        "..oddmo..",
        "...ooo...",
    ]),
    "bronze": ({"o": (52, 30, 14), "d": (128, 72, 30), "m": (184, 112, 50), "l": (222, 156, 80), "h": (248, 206, 130), "w": (110, 78, 46)}, [
        ".ooo.....",
        "ohlmo....",
        "olmmdo...",
        "ommddwo..",
        ".oddowwo.",
        "..oo.owwo",
        "......owo",
        ".......o.",
        ".........",
    ]),
    "iron": ({"o": (30, 32, 36), "d": (98, 104, 112), "m": (150, 158, 166), "l": (196, 204, 210), "h": (238, 242, 246), "w": (110, 78, 46)}, [
        ".ooo.....",
        "ohlmo....",
        "olmmdo...",
        "ommddwo..",
        ".oddowwo.",
        "..oo.owwo",
        "......owo",
        ".......o.",
        ".........",
    ]),
    "steam": ({"o": (40, 30, 22), "d": (110, 82, 50), "m": (150, 116, 70), "l": (196, 160, 104), "h": (232, 206, 150)}, [
        "...o.o...",
        ".oolmloo.",
        ".ohmmmdo.",
        "olmo.odlo",
        ".mm...mm.",
        "olmo.odlo",
        ".odmmmdo.",
        ".oodmdoo.",
        "...o.o...",
    ]),
    "electric": ({"o": (40, 40, 60), "d": (70, 130, 200), "m": (110, 180, 240), "l": (170, 220, 255), "h": (240, 250, 255)}, [
        ".....ooo.",
        "....ohlo.",
        "...ohlo..",
        "..ohlmoo.",
        ".ohhhlmo.",
        ".ooolmo..",
        "...omo...",
        "..omo....",
        "..oo.....",
    ]),
}


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    for name, (palette, rows) in ICONS.items():
        assert len(rows) == 9 and all(len(r) == 9 for r in rows), name
        img = Image.new("RGBA", (9, 9))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                if ch != ".":
                    img.putpixel((x, y), palette[ch] + (255,))
        img.save(OUT / f"age_{name}.png")
    print(f"wrote {len(ICONS)} age icons to {OUT}")


if __name__ == "__main__":
    main()
