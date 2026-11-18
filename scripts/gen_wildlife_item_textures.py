"""Generate 16x16 wildlife item textures (venison, sinew, antler, lard, tusk, spawn eggs).

Run: python scripts/gen_wildlife_item_textures.py
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


# ---------- scraps: reuse the house "beef scraps" shape, recolour by luminance rank ----------

def recolour(src, ramp):
    im = Image.open(src).convert("RGBA")
    px = im.load()
    xy = [(x, y) for y in range(16) for x in range(16) if px[x, y][3]]
    cols = sorted({px[p] for p in xy}, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    mapping = {c: hexc(ramp[round(i * (len(ramp) - 1) / max(len(cols) - 1, 1))]) for i, c in enumerate(cols)}
    for p in xy:
        px[p] = mapping[px[p]]
    return im


VENISON_RAW = ["#240405", "#3c0809", "#550d0e", "#6a1313", "#7c1a18", "#8a211e", "#962a25", "#a8443a"]
VENISON_COOKED = ["#1c0f09", "#2c190f", "#3d2416", "#4c2e1c", "#5a3722", "#684129", "#774c31", "#a07650"]


# ---------- hand-drawn items ----------

ANTLER = [
    "................",
    "...........O..O.",
    "..........OTOOTO",
    "..........OLOOLO",
    "...OO.....OMOLMO",
    "..OTO.....OMLMDO",
    "..OLO....OMLMDO.",
    "..OMO...OMLMDO..",
    "..OMDO.OMLMDO...",
    "...OMDOMLMDO....",
    "...OMMLLMDO.....",
    "...OMLMMDO......",
    "..ORMLMDO.......",
    ".ORrRDDO........",
    ".OrRrRO.........",
    "..OOOO..........",
]
ANTLER_PAL = {"O": "#3e2e1f", "T": "#f2ead6", "L": "#dccaa2", "M": "#bfa57c", "D": "#8f7451",
              "R": "#6a5034", "r": "#4c3824"}

TUSK = [
    "................",
    "............OO..",
    "...........OWO..",
    "...........ODWO.",
    "...........ODLO.",
    "..........ODLLO.",
    ".........ODLLMO.",
    "........ODLLLMO.",
    ".......ODLLLMO..",
    ".....OODLLLMMO..",
    "...OODDLLLLMO...",
    "..ORYYLLLMMO....",
    "..ORYYYMMOO.....",
    "..OORYMOO.......",
    "....OOO.........",
    "................",
]
TUSK_PAL = {"O": "#4a3f2c", "W": "#fffdf2", "L": "#f1ead2", "M": "#d8cca7", "D": "#a8987a",
            "Y": "#e3cf8e", "R": "#b8975a"}

LARD = [
    "................",
    "................",
    "................",
    "......OOOO......",
    "...OOOWWWLOO....",
    "..OLWWWLLLLMO...",
    "..OLWLLLLLLMMO..",
    "..OMLLLLLMMMMO..",
    "..OYMMMMMMMMDO..",
    ".OYYYMYYYYYDDO..",
    ".OYYYYYYYDYDDO..",
    ".ODYYDYYYYDDdO..",
    "gODDDDDDDDDdOGg.",
    "gGOOOOOOOOOOGGg.",
    ".gGGgggggggGg...",
    "................",
]
LARD_PAL = {"O": "#8f7f52", "W": "#fffef6", "L": "#f6f1dc", "M": "#ebe2c0", "Y": "#e4d59c",
            "D": "#cdbd8a", "d": "#b3a271", "g": "#6aa040", "G": "#3f6e26"}


def sinew():
    pal = {k: hexc(v) for k, v in {"O": "#7c6640", "1": "#fbf4d8", "2": "#ecdcaa", "3": "#d6c087",
                                    "4": "#b09662", "t": "#6b4a2b", "u": "#9a7448"}.items()}
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    # bundle runs bottom-left -> top-right; a = position along it, b = across it
    for y in range(16):
        for x in range(16):
            a, b = x - y, x + y - 15
            if not -12 <= a <= 12 or not -3 <= b <= 2:
                continue
            tie = 5 <= a <= 6
            fray = a <= -8 or a >= 9
            if fray and (b % 2 or abs(a) - 8 > 2 - abs(b + 0.5)):  # splayed loose fibres
                continue
            if tie:
                im.putpixel((x, y), pal["t"] if (a + b) % 2 else pal["u"])
            elif b in (-3, 2) and not fray:
                im.putpixel((x, y), pal["O"] if b == 2 else pal["4"])
            else:
                stripe = (a + b) % 4  # twisted strands
                tone = "1243" if b < 0 else "2434"
                im.putpixel((x, y), pal[tone[stripe]])
    return im


# ---------- spawn eggs (26.3 style: egg silhouette with animal features) ----------

EGG_ROWS = {1: (6, 9), 2: (4, 11), 3: (3, 12), 4: (3, 12), 5: (2, 13), 6: (2, 13), 7: (2, 13), 8: (2, 13),
            9: (2, 13), 10: (2, 13), 11: (2, 13), 12: (3, 12), 13: (3, 12), 14: (4, 11), 15: (6, 9)}


def egg(tones, spots=(), extra_mask=(), paint=(), shift=0):
    """tones: hi, light, mid, dark, outline_light, outline_dark. spots/paint: [(x, y, colour)]."""
    mask = {(x, y + shift) for y, (a, b) in EGG_ROWS.items() for x in range(a, b + 1) if 0 <= y + shift < 16}
    mask |= set(extra_mask)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    hi, li, mi, da, ol, od = (hexc(t) for t in tones)
    for (x, y) in mask:
        nb = [(x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)]
        if any(n not in mask for n in nb):
            im.putpixel((x, y), od if (x >= 8 or y >= 10 + shift) else ol)
            continue
        if (x + 1, y) not in mask or (x, y + 1) not in mask or (x + 1, y + 1) not in mask:
            im.putpixel((x, y), da)
            continue
        s = (x - 7.5) / 6 * 0.6 + (y - shift - 8) / 7 * 0.8
        im.putpixel((x, y), hi if s < -0.55 else li if s < -0.1 else mi if s < 0.45 else da)
    for x, y, c in list(spots) + list(paint):
        im.putpixel((x, y + (shift if (x, y + shift) in mask else 0)), hexc(c))
    return im


def roe_deer():
    W = "#f4ede0"
    spots = [(5, 5, W), (9, 4, W), (11, 7, W), (4, 9, W), (7, 7, W), (10, 11, W), (6, 12, W)]
    nubs = [(6, 0), (9, 0)]  # short dark spike antlers
    paint = [(6, 0, "#3a2414"), (9, 0, "#3a2414"),
             (7, 13, W), (8, 13, W), (7, 14, "#1d1210"), (8, 14, "#1d1210")]
    return egg(["#e39a62", "#cf7a42", "#b9622f", "#954a22", "#6e3517", "#4f240f"],
               spots, nubs, paint)


def red_deer():
    B = "#e2c48c"
    left = [(1, 0, "#efe4c8"), (4, 0, "#efe4c8"), (2, 1, "#c8b088"), (4, 1, "#c8b088"),
            (3, 2, "#8a6e4c"), (4, 2, "#8a6e4c"), (5, 2, "#8a6e4c")]
    paint = left + [(15 - x, y, c) for x, y, c in left]  # branched antlers, mirrored
    antlers = [(x, y) for x, y, _ in paint]
    spots = [(5, 6, B), (10, 6, B), (7, 9, B), (4, 11, B), (11, 10, B), (9, 13, B)]
    paint += [(7, 14, "#d9bf92"), (8, 14, "#d9bf92"), (7, 15, "#2a1810"), (8, 15, "#2a1810")]
    return egg(["#b9673c", "#9c4e2a", "#843f22", "#68301a", "#4a2112", "#33160b"],
               spots, antlers, paint, shift=1)


def wild_boar():
    K = "#1c1714"
    spots = [(5, 5, K), (10, 4, K), (4, 8, K), (11, 8, K), (8, 7, K), (5, 11, K), (10, 12, K), (6, 9, K)]
    mane = [(7, 1, K), (8, 1, K), (7, 2, K), (8, 2, "#2c2420"), (7, 3, K), (8, 3, K), (8, 4, K)]
    snout = [(6, 12, "#a07f74"), (7, 12, "#b8968a"), (8, 12, "#b8968a"), (9, 12, "#a07f74"),
             (6, 13, "#8a6a60"), (7, 13, "#3a2a26"), (8, 13, "#8a6a60"), (9, 13, "#3a2a26"),
             (5, 11, "#fbf6e6"), (10, 11, "#fbf6e6"), (5, 12, "#e6dcc0"), (10, 12, "#e6dcc0")]
    return egg(["#7b6c5e", "#655749", "#54473b", "#41372e", "#2f2721", "#1f1a16"],
               spots, (), mane + snout)


def hare():
    K, W = "#1e1a17", "#f2efe8"
    E1, E2, P = "#9b8368", "#6a5644", "#d6a99a"
    paint = [(4, 0, K), (5, 0, K), (10, 0, K), (11, 0, K),  # long ears with black tips
             (4, 1, E1), (5, 1, P), (10, 1, P), (11, 1, E2),
             (4, 2, E1), (5, 2, P), (10, 2, P), (11, 2, E2)]
    ears = [(x, y) for x, y, _ in paint]
    spots = [(6, 4, K), (9, 5, W), (4, 7, W), (11, 6, K), (7, 8, K), (10, 9, W), (5, 10, K),
             (6, 12, W), (7, 12, W), (8, 12, W), (9, 12, W), (7, 13, W), (8, 13, W)]
    return egg(["#b59e83", "#9b8368", "#846d55", "#6a5644", "#4b3c2f", "#33281f"],
               spots, ears, paint, shift=1)


def pheasant():
    G1, G2, G3 = "#2f6b4a", "#1f4d35", "#14301f"
    W, R = "#f4f1e8", "#c8322a"
    paint = []
    for (x, y) in [(x, y) for y in range(1, 6) for x in range(EGG_ROWS[y][0] + 1, EGG_ROWS[y][1])]:
        paint.append((x, y, G1 if x + y < 9 else G2))
    paint += [(4, 4, R), (5, 4, R), (10, 4, R), (11, 4, R), (5, 3, R), (10, 3, R)]
    paint += [(x, 6, W) for x in range(3, 13)]
    spots = [(5, 8, "#5a2a14"), (9, 9, "#5a2a14"), (11, 11, "#5a2a14"), (4, 11, "#5a2a14"),
             (7, 12, "#5a2a14"), (8, 10, "#f0b070"), (6, 9, "#f0b070")]
    im = egg(["#e3964f", "#cc7a35", "#b06226", "#8c4a1a", "#5e2f10", "#40200a"], spots, (), paint)
    for y in range(1, 6):  # green head outline
        a, b = EGG_ROWS[y]
        im.putpixel((a, y), hexc(G3))
        im.putpixel((b, y), hexc(G3))
    for x in range(6, 10):
        im.putpixel((x, 1), hexc(G3))
    return im


def build():
    src = OUT / "beef_scraps.png"
    items = {
        "venison_scraps": recolour(src, VENISON_RAW),
        "cooked_venison_scraps": recolour(OUT / "cooked_beef_scraps.png", VENISON_COOKED),
        "sinew": sinew(),
        "antler": from_grid(ANTLER, ANTLER_PAL),
        "lard": from_grid(LARD, LARD_PAL),
        "tusk": from_grid(TUSK, TUSK_PAL),
        "roe_deer_spawn_egg": roe_deer(),
        "red_deer_spawn_egg": red_deer(),
        "wild_boar_spawn_egg": wild_boar(),
        "hare_spawn_egg": hare(),
        "pheasant_spawn_egg": pheasant(),
    }
    for name, im in items.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(OUT / f"{name}.png")
    return items


if __name__ == "__main__":
    for n in build():
        print("wrote", n)
