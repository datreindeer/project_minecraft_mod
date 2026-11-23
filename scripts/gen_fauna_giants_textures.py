"""Paint the textures of the Ice Age giants (GiantsFauna): the 16x16 items, the pegged raw hides, the rugs and the Ivory
Figurine. The animals' own models and skins come from gen_pred_models.py.

Usage: python scripts/gen_fauna_giants_textures.py
Deterministic.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, shade, smooth  # noqa: E402
from gen_wildlife_item_textures import egg, from_grid, hexc  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
CLEAR = (0, 0, 0, 0)

# ---------------------------------------------------------------- 16x16 items

ITEM = TEX / "item"
JAR = Path(__file__).resolve().parent.parent / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"


def vanilla_item(name):
    import io
    import zipfile
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/item/{name}.png"))).convert("RGBA")


def recolour(im, ramp):
    """Map the picture's colours, ranked by brightness, onto a dark->light ramp (keeps the shape and shading)."""
    im = im.copy()
    px = im.load()
    xy = [(x, y) for y in range(16) for x in range(16) if px[x, y][3]]
    cols = sorted({px[p] for p in xy}, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    mapping = {c: hexc(ramp[round(i * (len(ramp) - 1) / max(len(cols) - 1, 1))]) for i, c in enumerate(cols)}
    for p in xy:
        px[p] = mapping[px[p]]
    return im


MEATS = {
    # raw / cooked from a vanilla shape, recoloured: fatty mammoth (yellow fat), lean dark rhino, deep red bison, musk ox
    "mammoth": ("beef", ["#2a0606", "#4a0c0c", "#6a1414", "#8a1e1c", "#a62e28", "#c45a44", "#e2c48a", "#f2e2b4"],
                ["#1e1009", "#2e1a0e", "#422614", "#58341c", "#6e4426", "#8a5a34", "#c8a064", "#e4c88c"]),
    "rhino": ("porkchop", ["#200606", "#3a0b0b", "#521110", "#681816", "#7c201c", "#92302a", "#b65a4c", "#d8a090"],
              ["#1a0e08", "#2a170d", "#3a2112", "#4a2a18", "#5a341e", "#6c4028", "#906042", "#b88a64"]),
    "bison": ("mutton", ["#240404", "#3e0808", "#580e0e", "#701414", "#861c1a", "#9c2a24", "#c25248", "#e6b4a0"],
              ["#1c0f08", "#2c180d", "#3e2414", "#4e2e1a", "#603a20", "#74482a", "#9a6a44", "#c49a6c"]),
    "musk_ox": ("rabbit", ["#260707", "#400c0c", "#5a1312", "#701a18", "#86241f", "#9c3329", "#be5c4c", "#e0aa96"],
                ["#1c1009", "#2c190f", "#3c2415", "#4c2e1b", "#5e3a22", "#70472b", "#946848", "#bc9470"]),
}

IVORY_GRID = [
    "................",
    "....OO..........",
    "...OWLO.........",
    "...OLMO.........",
    "...OLMDO........",
    "....OLMDO.......",
    "....OLLMDO......",
    ".....OLLMDOO....",
    "......OLLMMDOO..",
    ".......OOLLMMDO.",
    ".........OOLLMDO",
    "...........OYYDO",
    "..........ORYYO.",
    "..........ORRO..",
    "...........OO...",
    "................",
]
IVORY_PAL = {"O": "#4a3c26", "W": "#fffbee", "L": "#f2e8cc", "M": "#dccca2", "D": "#b4a078", "Y": "#d6bc7e", "R": "#9a7a48"}

HORN_GRID = [
    "................",
    "..OO............",
    "..OPO...........",
    "..OPLO..........",
    "...OPLO.........",
    "...OPLMO........",
    "...OPLMMO.......",
    "....OPLMMO......",
    "....OPLMMDO.....",
    ".....OPLMMDO....",
    ".....OPLMMDDO...",
    "......OPLMMDDO..",
    "......ORRRRRRRO.",
    ".......OOOOOOOO.",
    "................",
    "................",
]
HORN_PAL = {"O": "#1c1612", "P": "#a89678", "L": "#7a6a54", "M": "#5c4e40", "D": "#40362e", "R": "#2e2620"}

TONGUE_GRID = [
    "................",
    "................",
    "................",
    "................",
    "...........OOO..",
    ".........OOPPLO.",
    ".......OOPPPPLO.",
    ".....OOPPMPPPLO.",
    "....OPPMPPPPPO..",
    "...OPPPPMPPOO...",
    "...ODPPPPOO.....",
    "...ODDDOO.......",
    "....OOO.........",
    "................",
    "................",
    "................",
]
TONGUE_PAL = {"O": "#4a2228", "P": "#b47478", "L": "#d49aa0", "M": "#8e545c", "D": "#6e3a42"}
TONGUE_COOKED = ["#2a140c", "#4a2a18", "#6a4026", "#8a5a36", "#a8784e"]

FAT_GRID = [
    "................",
    "................",
    "................",
    ".....OOOOO......",
    "...OOWWWLLOO....",
    "..OLWWLLLLLMO...",
    ".OLWLLLLLLLMMO..",
    ".OMLLLLLLMMMMO..",
    ".OYMMMMMMMMMDO..",
    "OYYYMYYYYYYDDO..",
    "OYYYYYYYYDYDDO..",
    "ODYYDYYYYYDDdO..",
    "ODDDDDDDDDDdOO..",
    ".OOOOOOOOOOOO...",
    "................",
    "................",
]
FAT_PAL = {"O": "#8a6a3a", "W": "#fff8de", "L": "#f8eabc", "M": "#efd896", "Y": "#e2c470", "D": "#c8a454", "d": "#a8863e"}

KNIFE_GRID = [
    "................",
    "............OO..",
    "...........OWO..",
    "..........OWLO..",
    ".........OWLO...",
    "........OWLDO...",
    ".......OWLDO....",
    "......OWLDO.....",
    ".....OYLDO......",
    "....OOYDO.......",
    "...OcCOO........",
    "..OCcCO.........",
    ".OcCcO..........",
    ".OCcO...........",
    "..OO............",
    "................",
]
KNIFE_PAL = {"O": "#3c3020", "W": "#fffbee", "L": "#ece0c0", "D": "#c4b088", "Y": "#a88e5c", "C": "#8a6a44", "c": "#6a4e30"}

CLUB_GRID = [
    "..........OO....",
    ".........OPO....",
    "........OPLO....",
    "........OPMO....",
    ".......OWWWWO...",
    "......OWwccwWO..",
    "......OwWccWwO..",
    ".......OwWWwO...",
    "......OwwO......",
    ".....OwWO.......",
    "....OwWO........",
    "...OwWO.........",
    "..OcCO..........",
    ".OCcO...........",
    ".OcO............",
    "..O.............",
]
CLUB_PAL = {"O": "#1e1610", "P": "#a89678", "L": "#7a6a54", "M": "#5c4e40", "W": "#8a6a44", "w": "#6a4e30",
            "c": "#c4a878", "C": "#9c8058"}

# spread hide seen from above: neck at the top, legs to the corners, tail at the bottom
HIDE_MASK = [
    "......xxxx......",
    ".....xxxxxx.....",
    ".xx.xxxxxxxx.xx.",
    "xxxxxxxxxxxxxxxx",
    ".xxxxxxxxxxxxxx.",
    "...xxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "...xxxxxxxxxx...",
    ".xxxxxxxxxxxxxx.",
    "xxxxxxxxxxxxxxxx",
    ".xx.xxxxxxxx.xx.",
    ".....xxxxxx.....",
    ".......xx.......",
]


def hide_fur(kind):
    """Fur colour at (x, y) of a spread hide, fur side up."""
    def c(x, y):
        strand = h(x, y // 2 + (x % 3), 300)
        if kind == "mammoth":
            base = lerp((60, 36, 24), (150, 100, 60), 0.3 + 0.6 * strand)
            return lerp(base, (186, 130, 76), 0.6) if h(x, y, 301) < 0.1 else base
        if kind == "rhino":
            return lerp((84, 58, 36), (160, 116, 76), 0.35 + 0.5 * strand)
        if kind == "bison":
            cape = max(0.0, 1 - y / 9)  # the woolly cape at the neck end
            base = lerp((40, 28, 20), (98, 66, 42), 0.3 + 0.5 * strand)
            return lerp(base, (150, 106, 60), cape * (0.6 + 0.4 * strand))
        # musk ox: near-black with the pale saddle down the middle
        base = lerp((26, 20, 16), (80, 62, 48), 0.3 + 0.5 * strand)
        if 6 <= x <= 9 and 4 <= y <= 9:
            base = lerp(base, (178, 158, 126), 0.7)
        return base
    return c


def blob(rows):
    return {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch != "."}


def edge_of(cells, x, y):
    return any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def spread_hide(kind, mask=HIDE_MASK, outline=(24, 16, 12)):
    im = Image.new("RGBA", (16, 16), CLEAR)
    cells = blob(mask)
    fur_at = hide_fur(kind)
    if kind == "mammoth":  # long hair: a ragged, shaggy rim
        cells = {(x, y) for x, y in cells if not (edge_of(cells, x, y) and h(x, y, 303) < 0.3)}
    for x, y in cells:
        c = fur_at(x, y)
        if edge_of(cells, x, y):
            c = outline if h(x, y, 302) < 0.65 else shade(c, 0.6)
        else:
            c = shade(c, 1.0 + 0.08 * (7.5 - x) / 7.5 + 0.06 * (7 - y) / 7)  # light from the top left
        im.putpixel((x, y), (*c, 255))
    return im


RAW_FLESH = [(132, 44, 42), (168, 70, 64), (194, 104, 92)]
FAT = (232, 214, 188)


def raw_hide_item(img, salt):
    """The cured hide picture made wet and dark, a raw red rim and the bottom right corner folded back (flesh side)."""
    out = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    cells = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
    fold = 20
    for x, y in cells:
        c = px[x, y][:3]
        if x + y > fold:
            col = lerp(RAW_FLESH[0], RAW_FLESH[2], 0.35 + 0.5 * h(x, y, salt))
            if h(x // 2, y, salt + 1) < 0.3:
                col = lerp(col, FAT, 0.7)
            if x + y == fold + 1:
                col = FAT
        elif edge_of(cells, x, y) and h(x, y, salt + 2) < 0.45:
            col = lerp(RAW_FLESH[0], RAW_FLESH[2], h(x, y, salt + 3))
        else:
            col = shade(lerp(c, (60, 40, 34), 0.1), 0.85)
        out.putpixel((x, y), (*col, 255))
    return out


def fluff(base, light, dark, salt, cells):
    im = Image.new("RGBA", (16, 16), CLEAR)
    for x, y in cells:
        t = 0.5 - 0.03 * (x - 7.5) - 0.03 * (y - 7.5) + 0.35 * (h(x // 2 + y % 2, y // 2, salt) - 0.5)
        c = shade(dark, 0.75) if edge_of(cells, x, y) else lerp(dark, light, t)
        if not edge_of(cells, x, y) and h(x, y, salt + 1) < 0.12:
            c = lerp(c, base, 0.5)
        im.putpixel((x, y), (*c, 255))
    return im


QIVIUT_SHAPE = [
    "................",
    "................",
    "......xxxx......",
    "....xxxxxxxx....",
    "...xxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "...xxxxxxxxxx...",
    "....xxx..xxx....",
    "................",
    "................",
    "................",
]


def mammoth_wool():
    """A hank of long ginger-brown hair, tied at the top left and splaying out to the bottom right."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            a, b = x + y - 15, x - y  # a along the hank, b across it
            width = 1.5 + max(0, a + 6) * 0.16 + 0.6 * (h(a, 0, 311) - 0.5)
            if not -11 <= a <= 13 or abs(b) > width:
                continue
            if a > 9 and (b + a) % 2:
                continue  # loose ends
            t = 0.5 + 0.4 * (h(b + 20, a // 3, 310) - 0.5) - 0.1 * b / max(width, 1)
            c = lerp((70, 40, 24), (196, 136, 78), t)
            if -9 <= a <= -7:
                c = (58, 42, 30) if (a + b) % 2 else (96, 70, 46)  # a twist of cord round the end
            elif abs(b) > width - 1:
                c = shade(c, 0.7)
            im.putpixel((x, y), (*c, 255))
    return im


def spawn_eggs():
    eggs = {}
    I1, I2 = "#f4ead0", "#cbb88c"
    tusks = [(3, 12, I1), (2, 13, I1), (2, 14, I2), (12, 12, I1), (13, 13, I1), (13, 14, I2)]
    trunk = [(7, 10, "#3a2c24"), (8, 10, "#4a3a30"), (7, 11, "#3a2c24"), (8, 12, "#3a2c24"), (8, 13, "#2a201a")]
    eggs["woolly_mammoth_spawn_egg"] = egg(["#a86a40", "#8a5232", "#6c3e26", "#52301e", "#3a2216", "#26160e"],
                                            [(5, 4, "#c48a56"), (10, 5, "#c48a56"), (4, 8, "#4a2a18"), (11, 9, "#4a2a18")],
                                            [(x, y) for x, y, _ in tusks], tusks + trunk)
    H1, H2 = "#9a8a70", "#5c4e40"
    horn = [(7, 0, H1), (7, 1, H1), (8, 1, H2), (7, 2, H2)]
    eggs["woolly_rhino_spawn_egg"] = egg(["#b48a5a", "#9a7046", "#7e5836", "#62442a", "#463020", "#2e2016"],
                                          [(5, 6, "#c8a070"), (10, 7, "#c8a070"), (6, 11, "#5a3e28"), (10, 12, "#5a3e28"),
                                           (6, 3, "#4a4038"), (7, 3, "#4a4038"), (8, 3, "#4a4038")],
                                          [(x, y) for x, y, _ in horn], horn)
    K = "#1a1410"
    bhorns = [(2, 1, K), (13, 1, K), (3, 2, K), (12, 2, K)]
    cape = [(x, y, "#b88a50" if (x + y) % 3 else "#d0a466") for y in range(3, 7) for x in range(4, 12)]
    eggs["steppe_bison_spawn_egg"] = egg(["#6e4a30", "#583a26", "#46301f", "#362418", "#261a12", "#18100b"],
                                          [(7, 12, K), (8, 12, K), (7, 13, K)],
                                          [(x, y) for x, y, _ in bhorns], cape + bhorns)
    B1, B2 = "#e2d6b8", "#a8987a"
    boss = [(x, 2, B1) for x in range(4, 12)] + [(3, 3, B1), (12, 3, B1), (3, 4, B2), (12, 4, B2), (4, 5, B2), (11, 5, B2)]
    saddle = [(x, 7, "#b8a07c") for x in range(5, 11)] + [(x, 8, "#a08a68") for x in range(6, 10)]
    socks = [(x, 14, "#e0d8c6") for x in range(6, 10)] + [(4, 13, "#d0c8b4"), (11, 13, "#d0c8b4")]
    eggs["musk_ox_spawn_egg"] = egg(["#4a3a2e", "#3a2c22", "#2c2018", "#201712", "#16100c", "#0e0a08"],
                                     [], [], boss + saddle + socks)
    return eggs


def items():
    out = {}
    cooked_of = {"beef": "cooked_beef", "porkchop": "cooked_porkchop", "mutton": "cooked_mutton", "rabbit": "cooked_rabbit"}
    for animal, (shape, raw, cooked) in MEATS.items():
        out[f"raw_{animal}"] = recolour(vanilla_item(shape), raw)
        out[f"cooked_{animal}"] = recolour(vanilla_item(cooked_of[shape]), cooked)
    out["bison_tongue"] = from_grid(TONGUE_GRID, TONGUE_PAL)
    out["cooked_bison_tongue"] = recolour(out["bison_tongue"], TONGUE_COOKED)
    out["mammoth_ivory"] = from_grid(IVORY_GRID, IVORY_PAL)
    out["rhino_horn"] = from_grid(HORN_GRID, HORN_PAL)
    out["mammoth_fat"] = from_grid(FAT_GRID, FAT_PAL)
    out["mammoth_wool"] = mammoth_wool()
    out["qiviut"] = fluff((150, 130, 108), (188, 170, 146), (96, 80, 66), 320, blob(QIVIUT_SHAPE))
    out["rhino_horn_club"] = from_grid(CLUB_GRID, CLUB_PAL)
    for kind, salt in (("mammoth", 400), ("rhino", 410), ("bison", 420), ("musk_ox", 430)):
        cured = spread_hide(kind)
        out[f"{kind}_hide"] = cured
        out[f"raw_{kind}_hide"] = raw_hide_item(cured, salt)
    for kind in ("mammoth", "bison", "musk_ox"):
        out[f"{kind}_rug"] = spread_hide(kind)
    out.update(spawn_eggs())
    for name, im in out.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
    print(f"wrote {len(out)} item textures")
    return out


# ---------------------------------------------------------------- blocks: pegged hides, rugs, figurine

BLOCK = TEX / "block"
# a hide pegged out flat, flesh side up, stretched to the four corner pegs
PEG_MASK = {
    "mammoth": [
        ".xxx........xxx.",
        ".xxxxxxxxxxxxxx.",
        ".xxxxxxxxxxxxxx.",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxxx.",
        ".xxxxxxxxxxxxxx.",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        ".xxxxxxxxxxxxxx.",
        ".xxxxxxxxxxxxxx.",
        ".xxx........xxx.",
    ],
    "small": [
        ".xx..........xx.",
        ".xxx..xxxx..xxx.",
        "..xxxxxxxxxxxx..",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "..xxxxxxxxxxxx..",
        ".xxx..xxxx..xxx.",
        ".xx....xx....xx.",
        ".......xx.......",
    ],
}
RAWHIDE = [(150, 116, 74), (186, 150, 104), (214, 186, 140)]


def pegged(kind, cured, salt):
    cells = blob(PEG_MASK["mammoth" if kind == "mammoth" else "small"])
    fur_at = hide_fur(kind)
    im = Image.new("RGBA", (16, 16), CLEAR)
    for x, y in cells:
        if edge_of(cells, x, y) and h(x, y, salt) < 0.7:
            c = shade(fur_at(x, y), 0.85)  # the fur shows round the rim (fur side down)
        elif cured:
            k = 0.45 + 0.35 * (h(x // 2, y, salt + 1) - 0.5) + 0.1 * smooth(1 - abs(x - 7.5) / 8)
            c = lerp(RAWHIDE[0], RAWHIDE[2], k)
            if h(x, y // 2, salt + 2) < 0.08:
                c = shade(c, 0.85)  # dried veins
        else:
            c = lerp(RAW_FLESH[0], RAW_FLESH[2], 0.3 + 0.55 * h(x // 2, y, salt + 3))
            if h(x // 3, y, salt + 4) < 0.22:
                c = lerp(c, FAT, 0.65)  # fat and membrane still on it
        im.putpixel((x, y), (*c, 255))
    return im


def figurine():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            t = 0.55 + 0.3 * (h(x, y // 2, 500) - 0.5) - 0.012 * (x + y)
            c = lerp((196, 176, 130), (248, 240, 218), t)
            if (x * 3 + y * 5) % 11 == 0 and h(x, y, 501) < 0.6:
                c = shade(c, 0.8)  # carved hatching
            im.putpixel((x, y), (*c, 255))
    return im


def blocks(item_pics):
    out = {}
    for kind, salt in (("mammoth", 600), ("rhino", 610), ("bison", 620), ("musk_ox", 630)):
        out[f"raw_{kind}_hide"] = pegged(kind, False, salt)
        out[f"raw_{kind}_hide_cured"] = pegged(kind, True, salt)
    for kind in ("mammoth", "bison", "musk_ox"):
        out[f"{kind}_rug"] = item_pics[f"{kind}_rug"]
    out["ivory_figurine"] = figurine()
    for name, im in out.items():
        im.save(BLOCK / f"{name}.png")
    print(f"wrote {len(out)} block textures")


def main():
    blocks(items())


if __name__ == "__main__":
    main()
