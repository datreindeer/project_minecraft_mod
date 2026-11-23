"""All textures for the savanna fauna (wildlife/savanna/SavannaFauna.java).

The animals' own skins (and their models) are painted by gen_savanna_models.py.
Items (16x16): meats, hides, the cheetah pelt and cloak, Gazelle Horn, Signal Horn, Ostrich Feather and Egg, Aardvark
Claw, Termites, Chitin Paste, spawn eggs. Blocks: termite_mound (side) and termite_mound_top. Equipment: the Cheetah
Cloak's humanoid layer and cape (entity/cloak/cheetah_cloak.png).
Deterministic. Run: python scripts/gen_fauna_savanna_textures.py"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import TEX, fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import egg  # noqa: E402
import gen_fauna_mountain_textures as mt  # noqa: E402
from gen_fauna_warm_tex_items import hide_item, meat, raw_item, tube, put, CLEAR  # noqa: E402
from gen_fauna_warm_tex_items import LEOPARD_RAW, LEOPARD_COOKED, ONAGER_RAW, ONAGER_COOKED, BUFFALO_RAW, BUFFALO_COOKED  # noqa: E402
from gen_fauna_warm_tex_items import TAPIR_RAW, TAPIR_COOKED  # noqa: E402
from gen_fauna_coldhunters_textures import recolour_layer  # noqa: E402

ITEM, BLOCK = TEX.parent / "item", TEX.parent / "block"
TRANSPARENT = (0, 0, 0, 0)
EYE = (16, 12, 10)
BLACK = (28, 24, 22)


def spot(p, cell, radius, salt, chance=0.8):
    """Solid round spots on a jittered 3D grid (so they line up across faces)."""
    base = [math.floor(v / cell) for v in p]
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                k = (base[0] + dx, base[1] + dy, base[2] + dz)
                if h(k[0] * 31 + k[2], k[1], salt + 9) > chance:
                    continue
                c = [(k[a] + 0.2 + 0.6 * h(k[0] * 7 + a, k[1] * 13 + k[2] * 3, salt + a)) * cell for a in range(3)]
                if sum((p[a] - c[a]) ** 2 for a in range(3)) < radius * radius:
                    return True
    return False


# the cheetah's colours, for its pelt, cloak and cape (the animals' own skins: gen_savanna_models.py)
C_BACK, C_GOLD, C_PALE, C_WHITE = (196, 150, 78), (216, 172, 96), (232, 206, 156), (240, 234, 220)
C_SPOT, C_EYE, C_NOSE = (26, 22, 18), (196, 150, 52), (40, 30, 28)
TONGUE, TOOTH = (176, 82, 86), (232, 226, 208)


# ====================================================================== items
CHEETAH_STOPS = [(30, 24, 20), (150, 108, 52), (196, 150, 78), (216, 172, 96), (236, 214, 170)]


def cheetah_pelt():
    return mt.pelt("wolf_pelt", CHEETAH_STOPS, spots=741, salt=742)


def hyena_fur(x, y):
    return (52, 36, 26) if h(x // 2, y // 2, 751) < 0.3 and h(x, y, 752) < 0.6 else fur((184, 150, 98), x, y, 753, 0.07)


def gazelle_fur(x, y):
    if y in (9, 10):
        return fur((40, 30, 24), x, y, 761, 0.06)  # the flank band across the hide
    return fur((192, 138, 80) if y < 9 else (236, 228, 214), x, y, 762, 0.06)


def ostrich_skin(x, y):
    c = fur((206, 176, 150), x, y, 771, 0.04)
    return shade(c, 0.75) if (x + 2 * y) % 4 == 0 and h(x, y, 772) < 0.7 else c  # the quill bumps of ostrich leather


def aardvark_skin(x, y):
    c = fur((160, 128, 112), x, y, 781, 0.05, streak=False)
    return shade(c, 0.78) if h(x, y, 782) < 0.12 else c


def horn_icon(base, light, ring=True, band=None):
    """A curved horn from lower left to upper right, ringed, the tip curling forward."""
    pts = [(3.0, 13.5), (5.5, 10.0), (7.0, 6.5), (8.5, 3.5), (11.0, 2.0), (12.5, 2.5)]
    width = lambda t: 3.6 - 2.6 * t

    def colour(t, s, x, y):
        c = lerp(light, base, 0.3 + 0.5 * t) if s < 0 else lerp(base, shade(base, 0.7), t)
        if band and 0.08 < t < 0.2:
            return band
        if ring and int(t * 14) % 2 == 0 and t < 0.85:
            c = shade(c, 0.78)
        return c

    return tube(pts, width, colour, lambda t, x, y: shade(base, 0.55))


def feather_icon():
    """A soft white ostrich plume curving up and right on a dark quill."""
    pts = [(2.5, 14.0), (5.0, 10.0), (8.0, 6.5), (11.0, 4.0), (13.5, 2.5)]
    width = lambda t: 1.0 + 6.0 * math.sin(min(1.0, t * 1.15) * math.pi) ** 0.7

    def colour(t, s, x, y):
        if abs(s) < 0.2:
            return (90, 84, 78)  # quill
        c = lerp((248, 246, 240), (206, 200, 190), abs(s))
        return shade(c, 0.9) if (x + y) % 3 == 0 else c

    return tube(pts, width, colour, lambda t, x, y: (176, 170, 160))


def claw_icon():
    pts = [(4.0, 12.5), (6.0, 8.5), (8.5, 5.0), (11.5, 3.5), (13.0, 4.5)]
    return tube(pts, lambda t: 4.0 - 3.0 * t, lambda t, s, x, y: lerp((96, 82, 66), (60, 50, 42), t) if s > 0 else lerp((150, 132, 108), (96, 82, 66), t),
                lambda t, x, y: (40, 32, 26))


def ostrich_egg(cooked):
    if cooked:  # roasted in its shell: browned, the top cracked open on the yellow
        im = egg(["#c8a070", "#b08858", "#966f44", "#7a5634", "#5a4028", "#3e2c1c"])
        for x, y in ((6, 3), (7, 2), (8, 3), (9, 2), (7, 3), (8, 2)):
            put(im, x, y, (236, 196, 80))
        for x in range(5, 11):
            put(im, x, 4, (90, 64, 40))
        return im
    im = egg(["#fbf7ea", "#f1ead4", "#e2d8bc", "#cbbf9f", "#a99c7c", "#857858"])
    px = im.load()
    for x in range(16):
        for y in range(16):
            if px[x, y][3] and h(x, y, 791) < 0.08:
                put(im, x, y, (214, 204, 176))  # pores in the thick shell
    return im


def termites():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for k, (x, y) in enumerate(((3, 4), (9, 3), (6, 8), (11, 9), (4, 12), (10, 13))):
        for dx in range(3):
            put(im, x + dx, y, (236, 226, 200) if dx else (196, 112, 48))  # pale body, orange head
        put(im, x + 1, y + 1, (208, 196, 168))
    return im


def chitin_paste():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(16):
        for y in range(16):
            d = math.hypot((x - 7.5) / 6.0, (y - 9.0) / 4.5)
            if d < 1:
                c = lerp((206, 150, 70), (130, 84, 40), d)
                if h(x, y, 801) < 0.15:
                    c = shade(c, 1.2)
                put(im, x, y, c if d < 0.85 else (96, 62, 30))
    for x, y in ((5, 7), (6, 6), (9, 7)):
        put(im, x, y, (250, 222, 160))  # glossy highlights
    return im


def signal_horn():
    im = horn_icon((50, 44, 38), (96, 86, 74), band=(186, 150, 104))
    put(im, 3, 14, (186, 150, 104))  # the sinew-bound mouthpiece
    put(im, 2, 14, (150, 116, 76))
    return im


def cheetah_cloak():
    im = cheetah_pelt()
    for x in range(2, 14):
        put(im, x, 1, (120, 80, 46))  # a leather tie across the shoulders
    put(im, 1, 1, (86, 56, 30))
    put(im, 14, 1, (86, 56, 30))
    return im


def items():
    return {
        "cheetah_meat": meat("chicken_scraps", LEOPARD_RAW, fibre=0.8, salt=701),
        "cooked_cheetah_meat": meat("cooked_chicken_scraps", LEOPARD_COOKED, fibre=0.8, salt=702),
        "gazelle_meat": meat("venison_scraps", ONAGER_RAW, salt=703),
        "cooked_gazelle_meat": meat("cooked_venison_scraps", ONAGER_COOKED, salt=704),
        "ostrich_meat": meat("beef_scraps", BUFFALO_RAW, salt=705),
        "cooked_ostrich_meat": meat("cooked_beef_scraps", BUFFALO_COOKED, salt=706),
        "aardvark_meat": meat("porkchop_scraps", TAPIR_RAW, fat=(226, 200, 186), salt=707),
        "cooked_aardvark_meat": meat("cooked_porkchop_scraps", TAPIR_COOKED, fat=(170, 130, 92), salt=708),
        "hyena_hide": hide_item(hyena_fur, 711, lip=True, raw_edge=0.4),
        "raw_gazelle_hide": hide_item(gazelle_fur, 712),
        "raw_ostrich_hide": hide_item(ostrich_skin, 713, fold=21),
        "raw_aardvark_hide": hide_item(aardvark_skin, 714, lip=True, raw_edge=0.3),
        "cheetah_pelt": cheetah_pelt(),
        "raw_cheetah_pelt": raw_item(cheetah_pelt(), 715),
        "cheetah_cloak": cheetah_cloak(),
        "gazelle_horn": horn_icon((40, 36, 32), (96, 88, 78)),
        "signal_horn": signal_horn(),
        "ostrich_feather": feather_icon(),
        "aardvark_claw": claw_icon(),
        "ostrich_egg": ostrich_egg(False),
        "cooked_ostrich_egg": ostrich_egg(True),
        "termites": termites(),
        "chitin_paste": chitin_paste(),
        "spotted_hyena_spawn_egg": egg(["#e0c48e", "#c8a46c", "#ac8a56", "#8a6c42", "#5c4630", "#3c2e20"],
                                       spots=[(5, 5, "#3a2a1e"), (9, 7, "#3a2a1e"), (6, 10, "#3a2a1e"), (10, 11, "#3a2a1e")]),
        "cheetah_spawn_egg": egg(["#f4dca6", "#e2bc76", "#cc9e58", "#a87c40", "#7a5a2e", "#4e3a1e"],
                                 spots=[(5, 4, "#1e1a16"), (8, 5, "#1e1a16"), (10, 8, "#1e1a16"), (6, 8, "#1e1a16"), (8, 11, "#1e1a16"),
                                        (5, 12, "#1e1a16")]),
        "gazelle_spawn_egg": egg(["#e6b884", "#cc9a60", "#b07e48", "#8e6236", "#604228", "#40301e"],
                                 paint=[(x, 9, "#2a201a") for x in range(3, 13)] + [(x, 10, "#2a201a") for x in range(3, 13)]
                                 + [(x, y, "#f0eade") for x in range(4, 12) for y in (11, 12)]),
        "ostrich_spawn_egg": egg(["#5a5452", "#3e3a38", "#2c2826", "#1e1a1a", "#141010", "#0a0808"],
                                 spots=[(6, 9, "#f0eee6"), (7, 9, "#f0eee6"), (9, 10, "#f0eee6"), (10, 10, "#f0eee6"), (8, 4, "#d49a8c")]),
        "aardvark_spawn_egg": egg(["#d8bcae", "#bc9e8e", "#a08272", "#84685a", "#5e4a40", "#40322a"],
                                  spots=[(7, 4, "#e8a89c"), (8, 4, "#e8a89c")]),
    }


# ====================================================================== blocks, equipment
def mound(x, y, salt, top=False):
    """Baked red earth: vertical flutes washed out by the rain (lit ridges, shadowed grooves), a paler crust, grit, and
    tunnel openings with a lit lip above and a shadow inside. The top: rough crust round the central shaft."""
    lo, mid, hi = (120, 58, 32), (168, 92, 50), (206, 136, 84)
    if top:
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 1.7:
            return (34, 18, 12)  # the shaft
        if d < 2.6:
            return (92, 48, 28)
        ring = 0.5 + 0.5 * math.sin(d * 1.9 + h(x, y, salt) * 1.2)
        c = lerp(mid, hi, 0.25 + 0.4 * ring * (1 - d / 12))
    else:
        ridge = 0.5 + 0.5 * math.sin((x + 0.6 * math.sin(y * 0.7 + salt)) * math.pi / 2.5)  # flutes ~5 px apart
        c = lerp(lo, mid, 0.35 + 0.5 * ridge)
        if ridge > 0.85:
            c = lerp(c, hi, 0.45)  # sun on the ridge crest
        if h(x, y // 4, salt + 4) < 0.12:
            c = lerp(c, lo, 0.4)  # a runnel streak
    # tunnel holes: a dark mouth, a lit lip above it
    for hx, hy in ((3, 5), (11, 11)) if not top else ((4, 12), (12, 4)):
        if y == hy and hx <= x <= hx + 1:
            return (30, 16, 10)  # a tunnel mouth, two texels wide
        if y == hy + 1 and x == hx:
            return (66, 34, 20)  # its shadowed lower lip
        if y == hy - 1 and hx <= x <= hx + 1:
            c = lerp(c, hi, 0.5)
    n = h(x, y, salt + 3)
    if n < 0.06:
        return lerp(c, (230, 190, 140), 0.5)  # a grain of pale grit
    if n > 0.95:
        return lerp(c, lo, 0.5)
    return fur(c, x, y, salt + 3, 0.05)


def blocks():
    side, top = Image.new("RGBA", (16, 16)), Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            side.putpixel((x, y), (*mound(x, y, 811), 255))
            top.putpixel((x, y), (*mound(x, y, 821, True), 255))
    return {"termite_mound": side, "termite_mound_top": top}


def cloak_layer():
    def colour(x, y, s):
        if h(x // 2, y // 2, 831) < 0.22 and h(x, y, 832) < 0.75:
            return C_SPOT
        return fur(lerp(C_GOLD, C_PALE, s * 0.6), x, y, 833, 0.05)
    return recolour_layer("entity/equipment/humanoid/leather", colour, lambda x, y: y >= 16)


def cape_mat(v, face, x, y, z, i, j):
    if face == "back":
        return fur((150, 108, 68), i, j, 841, 0.05)  # tanned leather lining
    if y > 13 and face == "front":
        return fur(C_SPOT if (y - 13) % 2 < 1 else C_PALE, i, j, 842, 0.06)  # the ringed tail hanging at the hem
    if spot((x * 1.2, y, z * 3), 1.9, 0.6, 843, 0.85):
        return fur(C_SPOT, i, j, 844, 0.08)
    return fur(lerp(C_GOLD, C_PALE, y / 20), i, j, 845, 0.05)


def main():
    for folder, made in ((ITEM, items()), (BLOCK, blocks())):
        for name, im in made.items():
            assert im.size == (16, 16) and im.getbbox(), name
            im.save(folder / f"{name}.png")
    layer = cloak_layer()
    assert layer.getbbox()
    layer.save(TEX / "equipment/humanoid/cheetah_cloak.png")
    save(paint({"cape": (0, 0, -5, 0, -0.5, 10, 16, 1, cape_mat)}, "cape", (64, 32)), "cloak/cheetah_cloak.png")
    print("fauna savanna textures ok")


if __name__ == "__main__":
    main()
