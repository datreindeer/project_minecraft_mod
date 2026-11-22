"""16x16 item and block textures for the jungle and mangrove fauna (part of gen_fauna_jungle_textures.py).

- meats recoloured from house shapes (gen_wildlife_item_textures.recolour): tiger (leopard meat shape), python (snake
  meat), orangutan (mutton), monitor (rabbit), crocodile (chicken shape, pale), monkey (porkchop); the mudskipper and its
  cooked form from the pike, crab meat from the sturgeon fillet.
- the tiger pelt, raw pelt, rug and mantle: the warm leopard painters (gen_fauna_warm_tex_items) with tiger colours and
  stripes in place of rosettes; Python Skin, Monitor Skin and the crocodile hides on the house hide shape.
- Crocodile Hide armour recoloured from the Lindworm Scale pieces; Blowpipe, Darts, Frog Poison, Orangutan Hair drawn;
  spawn eggs (gen_wildlife_item_textures.egg). Block: tiger_rug.
Deterministic (hash noise). Run: python scripts/gen_fauna_jungle_tex_items.py"""
import importlib.util
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from wildlife_paint import fur, h, lerp, shade  # noqa: E402
from gen_wildlife_item_textures import egg, hexc, recolour  # noqa: E402

TEX = HERE.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
CLEAR = (0, 0, 0, 0)


def load(name, alias):
    spec = importlib.util.spec_from_file_location(alias, HERE / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


W = load("gen_fauna_warm_tex_items", "warm_items_jungle")         # the plain warm painters
T = load("gen_fauna_warm_tex_items", "warm_items_tiger")          # a copy re-coloured for the tiger
T.L_DEEP, T.L_GOLD, T.L_CREAM, T.L_SPOT, T.L_AMBER = (176, 78, 24), (222, 122, 40), (242, 230, 212), (24, 18, 14), (214, 190, 70)


def _stripe(x, y, salt, px=4, py=3):
    """Tiger stripes in place of the leopard's rosettes: wavy bars across the coat."""
    return "ring" if (y + int(1.4 * math.sin(x * 0.9 + h(x // 4, 0, salt) * 2))) % 3 == 0 and h(x, y, salt + 2) < 0.85 else None


T.rosette = _stripe

R = lambda *cs: list(cs)  # noqa: E731
TIGER_RAW = R("#1e0607", "#300b0c", "#451012", "#581717", "#691e1c", "#782622", "#86322b", "#9a4a3e")
TIGER_COOKED = R("#150b08", "#22130c", "#2e1a11", "#3a2215", "#46291a", "#52311f", "#5e3b26", "#744e34")
PYTHON_RAW = R("#7a5a5a", "#90706c", "#a4847e", "#b69690", "#c4a6a0", "#d2b6ae", "#dec6be", "#eadad0")
PYTHON_COOKED = R("#4c3a2a", "#644e38", "#7a6246", "#8e7454", "#a08664", "#b09874", "#c0aa88", "#d4c2a2")
APE_RAW = R("#2c0808", "#440d0d", "#5c1414", "#701b19", "#82231f", "#902d26", "#9e3a30", "#b2564a")
APE_COOKED = R("#1e100a", "#2e1a10", "#3f2516", "#4e2f1c", "#5c3923", "#6a432a", "#7a4f33", "#9a6c4a")
MONITOR_RAW = R("#6c4c4e", "#845e5e", "#987070", "#aa8282", "#b89292", "#c6a2a0", "#d4b4b0", "#e2c8c2")
MONITOR_COOKED = R("#463626", "#5e4834", "#745a42", "#886c50", "#9a7e60", "#aa9070", "#baa284", "#d0bca0")
CROC_RAW = R("#8a7068", "#a0847a", "#b4988c", "#c4aa9e", "#d2bcb0", "#dcccc0", "#e6dad0", "#f0e8e0")
CROC_COOKED = R("#5a4632", "#725c44", "#887054", "#9c8466", "#ae9878", "#beaa8a", "#ccbc9e", "#e0d4bc")
MONKEY_RAW = R("#4c1418", "#682024", "#842e30", "#9c3e3e", "#b0504e", "#c06460", "#cc7c76", "#da9890")
MONKEY_COOKED = R("#341e10", "#4e2e16", "#68401d", "#7e5025", "#92622f", "#a4743d", "#b48a50", "#cca872")
MUD_FISH = R("#2e2a20", "#423c2e", "#565040", "#6a6250", "#7e7662", "#928a74", "#a8a08a", "#c4bea6")
MUD_COOKED = R("#2a1a0e", "#3e2816", "#54381e", "#684828", "#7c5a34", "#906c42", "#a48054", "#c09c70")
CRAB_RAW = R("#b07c6c", "#c08c7a", "#d09c88", "#dcae98", "#e6bea8", "#eecdba", "#f4dccc", "#faece2")
CRAB_COOKED = R("#a4381c", "#b84a26", "#c85e32", "#d87240", "#e48a56", "#eca270", "#f2bc90", "#f8d8b8")


def put(im, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16:
        im.putpixel((x, y), (*c[:3], 255))


def meats():
    return {
        "tiger_meat": W.meat("leopard_meat", TIGER_RAW, fibre=0.86, salt=1001),
        "cooked_tiger_meat": W.meat("cooked_leopard_meat", TIGER_COOKED, fibre=0.88, salt=1002),
        "python_meat": W.meat("raw_snake", PYTHON_RAW, salt=1003),
        "cooked_python_meat": W.meat("cooked_snake", PYTHON_COOKED, salt=1004),
        "orangutan_meat": W.meat("mutton_scraps", APE_RAW, fat=(226, 200, 180), salt=1005),
        "cooked_orangutan_meat": W.meat("mutton_scraps", APE_COOKED, salt=1006),
        "monitor_meat": W.meat("rabbit_scraps", MONITOR_RAW, salt=1007),
        "cooked_monitor_meat": W.meat("rabbit_scraps", MONITOR_COOKED, salt=1008),
        "crocodile_meat": W.meat("chicken_scraps", CROC_RAW, fibre=0.94, salt=1009),
        "cooked_crocodile_meat": W.meat("chicken_scraps", CROC_COOKED, salt=1010),
        "monkey_meat": W.meat("porkchop_scraps", MONKEY_RAW, salt=1011),
        "cooked_monkey_meat": W.meat("porkchop_scraps", MONKEY_COOKED, salt=1012),
        "mudskipper": mudskipper_item(),
        "cooked_mudskipper": recolour(ITEM / "cooked_pike.png", MUD_COOKED),
        "raw_crab_meat": recolour(ITEM / "raw_sturgeon.png", CRAB_RAW),
        "cooked_crab_meat": recolour(ITEM / "cooked_sturgeon.png", CRAB_COOKED),
    }


def mudskipper_item():
    im = recolour(ITEM / "whole_pike.png", MUD_FISH)
    px = im.load()
    for x in range(16):
        for y in range(16):
            if px[x, y][3] and h(x, y, 1020) < 0.12:
                px[x, y] = (206, 214, 236, 255)  # pale-blue spangles
    return im


def hide_fill(base, dark, salt, scale=2):
    """A scaly hide surface: plates of `scale` px with dark grout and per-plate tint."""
    def at(x, y):
        c = shade(base, 1 + (h(x // scale, y // scale, salt) - 0.5) * 0.25)
        return dark if x % scale == 0 or y % scale == 0 else c
    return at


def python_net(x, y):
    c = (196, 168, 102) if (x // 3 + y // 3) % 2 else (156, 132, 84)
    return (38, 30, 22) if x % 3 == 0 and y % 2 == 0 or y % 3 == 0 and x % 2 == 1 else c


def hides():
    out = {
        "python_skin": W.hide_item(python_net, 1031, raw_edge=0.2),
        "monitor_skin": W.hide_item(lambda x, y: (214, 196, 104) if h(x, y, 1032) < 0.18 else hide_fill((80, 80, 62), (46, 46, 36), 1033, 1)(x, y),
                                    1034, raw_edge=0.2),
        "raw_crocodile_hide": W.hide_item(hide_fill((88, 94, 62), (52, 56, 38), 1035), 1036, fold=21, lip=True),
        "crocodile_hide": W.hide_item(hide_fill((104, 110, 72), (60, 64, 42), 1037), 1038, raw_edge=0.0),
    }
    pelt = T.leopard_pelt()
    out["tiger_pelt"] = pelt
    out["raw_tiger_pelt"] = T.raw_item(pelt, 1039)
    return out


def recolour_item(src, dark, light, pattern=None):
    im = Image.open(ITEM / src).convert("RGBA")
    px = im.load()
    for x in range(16):
        for y in range(16):
            r, g, b, a = px[x, y]
            if not a:
                continue
            t = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            c = lerp(dark, light, min(1.0, t * 1.25))
            if pattern:
                c = pattern(x, y, c)
            px[x, y] = (*c, a)
    return im


def croc_pattern(x, y, c):
    return shade(c, 0.78) if (x + y) % 3 == 0 else c


def armour():
    d, l = (40, 46, 30), (140, 144, 96)
    return {
        "crocodile_hide_helmet": recolour_item("lindworm_scale_helmet.png", d, l, croc_pattern),
        "crocodile_hide_cuirass": recolour_item("lindworm_scale_chestplate.png", d, l, croc_pattern),
        "crocodile_hide_leggings": recolour_item("lindworm_scale_leggings.png", d, l, croc_pattern),
        "crocodile_hide_boots": recolour_item("lindworm_scale_boots.png", d, l, croc_pattern),
        "tiger_pelt_mantle": recolour_item("otter_fur_cape.png", (150, 64, 20), (238, 150, 70),
                                           lambda x, y, c: (26, 20, 16) if (x + y // 2) % 4 == 0 and c[0] > 120 else c),
    }


def line(im, pts, col_at, width=1):
    """Thick polyline from (x, y) points; col_at(t) colour along it."""
    for (ax, ay), (bx, by) in zip(pts, pts[1:]):
        n = int(max(abs(bx - ax), abs(by - ay)) * 3) + 1
        for k in range(n + 1):
            t = k / n
            x, y = ax + (bx - ax) * t, ay + (by - ay) * t
            for dx in range(width):
                put(im, round(x) + dx, round(y), col_at(t))


def blowpipe():
    """A long bamboo tube from lower left to upper right, nodes ringed darker, the mouthpiece bound with twine."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for i in range(14):
        x, y = 1 + i, 14 - i
        node = i % 4 == 2
        put(im, x, y, (110, 130, 52) if node else (172, 186, 88))
        put(im, x + 1, y, (88, 104, 40) if node else (128, 146, 60))
        put(im, x, y - 1, (204, 214, 128) if not node else (130, 150, 60))
    for i in range(2):
        put(im, 1 + i, 14 - i, (170, 130, 80))  # twine at the mouth end
        put(im, 2 + i, 14 - i, (125, 92, 55))
    put(im, 15, 1, (40, 34, 20))  # the bore
    return im


def dart(poison):
    """A thin bamboo splinter with a fluff of feather down at the back; a poison dart's point is dark blue."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for i in range(10):
        put(im, 3 + i, 12 - i, (196, 180, 120) if i % 3 else (170, 150, 96))
    for (x, y) in ((2, 13), (3, 13), (2, 12), (1, 13), (2, 14), (3, 14), (1, 12)):
        put(im, x, y, (236, 232, 222) if (x + y) % 2 else (206, 200, 188))  # the down plug
    tip = (40, 70, 170) if poison else (120, 110, 80)
    for (x, y) in ((12, 3), (13, 2), (14, 1)):
        put(im, x, y, tip)
    if poison:
        put(im, 11, 4, (70, 110, 210))
    return im


def frog_poison():
    """A smear of shiny poison on a leaf: a green leaf with a blue-black glistening blob."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 6.5, (y - 8.5) / 4.0
            u = (dx + dy) / 1.414
            v = (dy - dx) / 1.414
            if u * u / 1.2 + v * v / 0.35 <= 1.0:
                c = (64, 130, 48) if abs(u - v) > 0.08 else (40, 96, 34)
                put(im, x, y, fur(c, x, y, 1051, 0.08, streak=False))
    for (x, y) in ((7, 7), (8, 7), (6, 8), (7, 8), (8, 8), (9, 8), (7, 9), (8, 9), (6, 9)):
        put(im, x, y, (40, 70, 190) if (x + y) % 3 else (20, 26, 60))
    put(im, 7, 7, (180, 210, 255))  # the shine
    put(im, 9, 9, (250, 220, 40))   # a fleck of yellow
    return im


def orangutan_hair():
    """A hank of long red hair, tied in the middle."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for k in range(7):
        x0 = 4 + k
        line(im, [(x0, 1), (x0 - 1 + (k % 2), 7), (x0 + 1 - (k % 3) * 0.5, 14)],
             lambda t, k=k: lerp((210, 110, 50), (140, 52, 20), (k % 3) / 2 * 0.7 + 0.3 * t))
    for x in range(4, 12):
        put(im, x, 7, (110, 80, 50))  # the tie
    return im


def tiger_rug():
    return T.leopard_rug()


def eggs():
    def e(tones, spots=()):
        return egg(tones, spots)
    K = "#1a1410"
    tiger = [(x, y, K) for x, y in ((4, 4), (5, 5), (9, 4), (10, 5), (4, 8), (5, 9), (10, 8), (11, 9), (6, 12), (9, 12))]
    py = [(x, y, "#2a2016") for x, y in ((5, 4), (6, 5), (9, 6), (10, 7), (4, 9), (7, 9), (8, 10), (11, 11), (6, 12))]
    ape = [(6, 7, "#4e3c36"), (9, 7, "#4e3c36"), (7, 9, "#4e3c36"), (8, 9, "#4e3c36")]
    mon = [(x, y, "#d6c468") for x, y in ((5, 5), (9, 4), (7, 7), (4, 9), (10, 9), (6, 11), (9, 12))]
    frog = [(x, y, "#141418") for x, y in ((5, 4), (6, 4), (9, 6), (10, 6), (5, 9), (6, 10), (10, 10), (8, 12))]
    croc = [(x, y, "#2e3420") for x, y in ((5, 5), (7, 5), (9, 5), (5, 8), (7, 8), (9, 8), (11, 8), (6, 11), (8, 11), (10, 11))]
    mud = [(x, y, "#cfd6ec") for x, y in ((5, 6), (9, 5), (7, 9), (10, 10), (5, 11))]
    crab = [(x, y, "#ec8028") for x, y in ((3, 8), (4, 8), (4, 9), (3, 9), (5, 7))]
    monkey = [(7, 8, "#d68a6a"), (8, 8, "#d68a6a"), (7, 9, "#d68a6a"), (8, 9, "#c47458"), (6, 12, "#ece8e0"), (9, 12, "#ece8e0")]
    return {
        "tiger_spawn_egg": e(["#f0a050", "#de7a28", "#c06220", "#8e4414", "#c87830", "#5a2a0c"], tiger),
        "python_spawn_egg": e(["#d4bc80", "#bca068", "#9c8452", "#76603a", "#a88c58", "#3e3020"], py),
        "orangutan_spawn_egg": e(["#e08848", "#c86a2e", "#a8501e", "#7c3812", "#b05a24", "#4c200a"], ape),
        "monitor_lizard_spawn_egg": e(["#8a8a6c", "#6e6e56", "#5a5a46", "#424234", "#6a6a52", "#24241c"], mon),
        "poison_dart_frog_spawn_egg": e(["#7aa8f0", "#4a80e0", "#2c60c8", "#1c4096", "#3c6cd0", "#0e1c46"], frog),
        "saltwater_crocodile_spawn_egg": e(["#8e9666", "#727a50", "#5c6440", "#444a30", "#6a7048", "#22261a"], croc),
        "mudskipper_spawn_egg": e(["#a89c80", "#8c8066", "#726850", "#58503c", "#80745a", "#2e2a20"], mud),
        "fiddler_crab_spawn_egg": e(["#8a9cc8", "#6c7ca8", "#525e86", "#3a4262", "#5e6c96", "#1c2032"], crab),
        "proboscis_monkey_spawn_egg": e(["#e8b47c", "#d08e50", "#b4723a", "#8a5428", "#c07e44", "#4a2c14"], monkey),
    }


def items():
    out = {}
    out.update(meats())
    out.update(hides())
    out.update(armour())
    out.update({"blowpipe": blowpipe(), "dart": dart(False), "poison_dart": dart(True), "frog_poison": frog_poison(),
                "orangutan_hair": orangutan_hair(), "tiger_rug": tiger_rug()})
    out.update(eggs())
    return out


def main():
    for folder, made in ((ITEM, items()), (BLOCK, {"tiger_rug": tiger_rug()})):
        for name, im in made.items():
            assert im.size == (16, 16) and im.getbbox(), name
            im.save(folder / f"{name}.png")
    print("fauna jungle item textures ok")


if __name__ == "__main__":
    main()
