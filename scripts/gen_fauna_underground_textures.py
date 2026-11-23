"""Paint the underground fauna (scripts/gen_fauna_underground.py, com.mechanical.wildlife.underground): the Chitin Vest
equipment layer and the 16x16 item textures; the Vittra's, Troll's and Lindworm's armour layers, items and the Troll Stone
block recoloured from vanilla and mod textures (needs the merged Minecraft jar in build/moddev/artifacts). The four beings'
entity and skinned textures come with their models from gen_hunt_models.py, which main() runs.

Usage: python scripts/gen_fauna_underground_textures.py
Deterministic: hashes only, no randomness.
"""
import importlib.util
from pathlib import Path

from PIL import Image

_spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
wit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wit)
RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def h(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    v = (v ^ (v >> 13)) * 1274126177 & 0xFFFFFFFF
    return (v & 0xFFFF) / 0xFFFF


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


# Scolopendra: glossy dark red-brown back plates with a black rear rim, amber-orange legs, a red-orange head, amber antennae,
# black-tipped venom claws, a tan underside.
C = dict(plate=(104, 40, 26), rim=(38, 18, 14), shine=(156, 70, 40), side=(128, 58, 32), belly=(186, 150, 96),
         head=(170, 64, 30), leg=(214, 150, 52), leg_dark=(160, 100, 34), claw=(120, 34, 22), tip=(30, 20, 16))


def faces(u, v, w, hh, d):
    """Vanilla box UV: face -> (x0, y0, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
            "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh)}


def chitin_vest():
    """Humanoid chest layer: overlapping red-brown chitin plates in rows on the body box (u 16, v 16, 8x12x4) and the
    shoulders of the arm box (u 40, v 16, 4x12x4), laced with sinew at the sides."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()

    def plate(x, y, row_y):
        rim = (y - row_y) % 3 == 2
        c = C["rim"] if rim else lerp(C["plate"], C["shine"], 0.35) if (y - row_y) % 3 == 0 else C["plate"]
        return (*shade(c, 0.9 + 0.2 * h(x, y, 77)), 255)

    for face, (x0, y0, fw, fh) in faces(16, 16, 8, 12, 4).items():
        if face in ("top", "bottom"):
            continue
        for j in range(fh - 1):
            for i in range(fw):
                px[x0 + i, y0 + j] = plate(x0 + i, y0 + j, y0)
        if face in ("left", "right"):
            for j in range(1, fh - 1, 2):
                px[x0 + fw // 2, y0 + j] = (222, 206, 164, 255)  # sinew lacing
    for face, (x0, y0, fw, fh) in faces(40, 16, 4, 12, 4).items():
        for j in range(fh if face in ("top",) else 4):
            for i in range(fw):
                px[x0 + i, y0 + j] = plate(x0 + i, y0 + j, y0)
    out = RES / "entity/equipment/humanoid/chitin_vest.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)


CHITIN = [  # a curved plate of chitin, glossy, the dark rim along its edge
    "................",
    "................",
    "................",
    ".....OOOOOO.....",
    "...OOHHSSSSOO...",
    "..OHHSSPPPPPRO..",
    "..OHSPPPPPPPRO..",
    ".OHSPPPPPPPPRRO.",
    ".OSPPPPPPPPPRRO.",
    ".OPPPPPPPPPRRRO.",
    ".ORRPPPPPPRRRO..",
    "..ORRRRRRRRRO...",
    "...OOOOOOOOO....",
    "................",
    "................",
    "................",
]
CHITIN_PAL = {"O": "#1e100c", "H": "#c8794a", "S": "#9c4a2c", "P": "#6a281a", "R": "#3a1610"}

FANG = [  # a hooked venom claw, black at the tip
    "................",
    "................",
    "..........OO....",
    ".........OTTO...",
    "........OTTO....",
    ".......ODDO.....",
    "......ORRDO.....",
    ".....ORRRO......",
    "....ORLRRO......",
    "...ORLLRO.......",
    "...OLLRRO.......",
    "...ORRRO........",
    "....OOO.........",
    "................",
    "................",
    "................",
]
FANG_PAL = {"O": "#1a0e0a", "T": "#16100c", "D": "#4a1c12", "R": "#8a2c1c", "L": "#c25a34"}

VENOM_SAC = [  # a translucent sac of yellow-green venom, the duct trailing off
    "................",
    "................",
    "......OOOO......",
    "....OOWLLLOO....",
    "...OWLLLMMMMO...",
    "...OLLLMMMMMO...",
    "..OLLMMMMMMDDO..",
    "..OLMMMMMMDDDO..",
    "..OMMMMMMDDDDO..",
    "...OMMMMDDDDO...",
    "....OMMDDDDO....",
    ".....OODDOO.....",
    ".......ORO......",
    "........ORO.....",
    ".........OO.....",
    "................",
]
VENOM_PAL = {"O": "#2c3018", "W": "#f6fadc", "L": "#d8e698", "M": "#a8c050", "D": "#70862c", "R": "#8a3c24"}

MEAT = [  # a length of centipede: plated segments, legs along the sides
    "................",
    "................",
    "............OO..",
    "..........OOAO..",
    "........OOAAO.L.",
    ".......OAABBO...",
    "....L.OBBAAO.L..",
    ".....OAABBO.....",
    "...LOBBAAO.L....",
    "....OAABBO......",
    "..LOBBAAO.L.....",
    "...OAABO........",
    "..OBBAO..L......",
    "..OOOO..........",
    "................",
    "................",
]

VEST = [  # the vest: rows of overlapping plates, laced up the front
    "................",
    "...OOO....OOO...",
    "..OPPPO..OPPPO..",
    "..OPRRPOOPRRPO..",
    "..OOPPPPPPPPOO..",
    "...OPPPWWPPPO...",
    "...ORRRWWRRRO...",
    "...OPPPWWPPPO...",
    "...OPPPWWPPPO...",
    "...ORRRWWRRRO...",
    "...OPPPWWPPPO...",
    "...OPPPWWPPPO...",
    "...ORRRWWRRRO...",
    "...OOOOOOOOOO...",
    "................",
    "................",
]
VEST_PAL = {"O": "#1e100c", "P": "#7a3020", "R": "#3a1610", "W": "#ded0a4"}


def items():
    out = {
        "centipede_chitin": wit.from_grid(CHITIN, CHITIN_PAL),
        "centipede_fang": wit.from_grid(FANG, FANG_PAL),
        "centipede_venom_sac": wit.from_grid(VENOM_SAC, VENOM_PAL),
        "raw_centipede": wit.from_grid(MEAT, {"O": "#3a1a12", "A": "#c9a088", "B": "#a66a52", "L": "#d89a48"}),
        "cooked_centipede": wit.from_grid(MEAT, {"O": "#2a1408", "A": "#d0884a", "B": "#9a5428", "L": "#8a5a2a"}),
        "chitin_vest": wit.from_grid(VEST, VEST_PAL),
        # egg: the dark red-brown of the plates, dark rims between the segments, amber legs out at the sides
        "giant_centipede_spawn_egg": wit.egg(["#b8603a", "#8c3c24", "#6a281a", "#4a1a10", "#3a140c", "#240c08"],
                                             [(x, y, "#2a0e08") for y in (5, 8, 11) for x in range(4, 12)],
                                             [(x, y) for y in (4, 7, 10, 13) for x in (1, 14)],
                                             [(x, y, "#e0a040") for y in (4, 7, 10, 13) for x in (1, 14)]),
    }
    for name, im in out.items():
        im.save(wit.OUT / f"{name}.png")


# ================================================================ the folk of the dark: Vittra, Troll, Lindworm
# Items, armour layers and the Troll Stone block are recoloured vanilla or mod textures.
import zipfile
from io import BytesIO

_ROOT = Path(__file__).resolve().parent.parent
_JAR = next(iter(sorted((_ROOT / "build/moddev/artifacts").glob("minecraft-patched-*-merged.jar"))
                 + sorted((_ROOT.parent / "project_minecraft_mod/build/moddev/artifacts").glob("minecraft-patched-*-merged.jar"))), None)


def vanilla(rel):
    assert _JAR, "no merged minecraft jar (run a gradle build first)"
    with zipfile.ZipFile(_JAR) as z:
        return Image.open(BytesIO(z.read(f"assets/minecraft/textures/{rel}.png"))).convert("RGBA")


def mod(rel):
    return Image.open(RES / f"{rel}.png").convert("RGBA")


def lum(c):
    return (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255


def ramp(stops, t):
    t = max(0.0, min(1.0, t))
    n = len(stops) - 1
    k = min(int(t * n), n - 1)
    return lerp(stops[k], stops[k + 1], t * n - k)


def recolour(img, stops, lo=0.05, hi=0.9, salt=0):
    """Every pixel's lightness (stretched lo..hi) onto the colour ramp, alpha kept."""
    out = img.copy()
    px = out.load()
    for x in range(img.width):
        for y in range(img.height):
            c = px[x, y]
            if c[3] == 0:
                continue
            r = ramp(stops, (lum(c) - lo) / (hi - lo))
            if salt:
                r = shade(r, 1 + (h(x, y, salt) - 0.5) * 0.1)
            px[x, y] = (*r, c[3])
    return out


def scale_pattern(img, salt):
    """Rows of overlapping scales over a recoloured armour layer: every other pixel row a little darker, offset."""
    px = img.load()
    for x in range(img.width):
        for y in range(img.height):
            c = px[x, y]
            if c[3] and (y % 2 == 1 and (x + y // 2) % 2 == 0):
                px[x, y] = (*shade(c[:3], 0.78), c[3])
    return img


VITTRA_GREY = [(40, 42, 48), (84, 88, 98), (124, 128, 136), (162, 166, 172), (200, 204, 208)]
TROLL_GREEN = [(34, 38, 26), (64, 70, 52), (98, 106, 80), (130, 134, 102), (168, 168, 132)]
LINDWORM_GREEN = [(14, 28, 18), (34, 64, 42), (58, 100, 62), (120, 150, 70), (214, 196, 110)]
IVORY = [(70, 60, 40), (150, 136, 100), (206, 194, 156), (232, 224, 196), (250, 246, 232)]


def folk_armour():
    eq = RES / "entity/equipment"
    recolour(vanilla("entity/equipment/humanoid/leather"), VITTRA_GREY, lo=0.3, hi=1.0, salt=51).save(eq / "humanoid/vittra_hood.png")
    recolour(vanilla("entity/equipment/humanoid/leather"), TROLL_GREEN, salt=52).save(eq / "humanoid/trollhide_coat.png")
    scale_pattern(recolour(vanilla("entity/equipment/humanoid/iron"), LINDWORM_GREEN, salt=53), 53).save(eq / "humanoid/lindworm_scale.png")
    scale_pattern(recolour(vanilla("entity/equipment/humanoid_leggings/iron"), LINDWORM_GREEN, salt=54), 54).save(
        eq / "humanoid_leggings/lindworm_scale.png")


HEART = [  # a dark red heart, green-black vessels at the top
    "................",
    "................",
    "......GG..G.....",
    ".....OGGO.GO....",
    "...OOOGGOOGOO...",
    "..ORRRRRRRRRRO..",
    ".ORLLRRRRRRRRRO.",
    ".ORLRRRRRRRRRDO.",
    ".ORRRRRRRRRRDDO.",
    "..ORRRRRRRRDDO..",
    "...ORRRRRRDDO...",
    "....ORRRRDDO....",
    ".....ORRDDO.....",
    "......ODDO......",
    ".......OO.......",
    "................",
]


def folk_items():
    it = wit.OUT
    meat = [(50, 22, 22), (120, 50, 50), (170, 90, 86), (204, 150, 140), (236, 210, 200)]
    out = {
        "vittra_meat": recolour(vanilla("item/mutton"), [(40, 30, 34), (120, 84, 92), (176, 136, 140), (214, 186, 186), (240, 224, 224)]),
        "cooked_vittra_meat": recolour(vanilla("item/cooked_mutton"), [(40, 26, 20), (100, 66, 48), (150, 104, 74), (196, 150, 108), (230, 200, 160)]),
        "troll_meat": recolour(vanilla("item/beef"), [(30, 14, 14), (90, 34, 34), (140, 60, 54), (180, 104, 90), (214, 168, 150)]),
        "cooked_troll_meat": recolour(vanilla("item/cooked_beef"), [(26, 16, 10), (70, 42, 26), (112, 70, 44), (150, 104, 70), (190, 150, 110)]),
        "lindworm_meat": recolour(vanilla("item/porkchop"), meat),
        "cooked_lindworm_meat": recolour(vanilla("item/cooked_porkchop"), [(40, 24, 10), (110, 70, 30), (170, 120, 60), (214, 170, 100), (240, 214, 160)]),
        "vittra_hide": recolour(mod("item/raw_hide"), VITTRA_GREY, salt=61),
        "troll_hide": recolour(mod("item/raw_hide"), TROLL_GREEN, salt=62),
        "vittra_eye": recolour(vanilla("item/spider_eye"), [(8, 8, 12), (30, 32, 40), (70, 76, 90), (170, 180, 196), (236, 240, 248)]),
        "troll_hair": recolour(vanilla("item/string"), [(30, 20, 12), (58, 42, 28), (90, 66, 44), (120, 92, 64), (150, 120, 90)], lo=0.2),
        "troll_tusk": recolour(mod("item/tusk"), IVORY),
        "lindworm_scale": recolour(vanilla("item/turtle_scute"), LINDWORM_GREEN),
        "lindworm_fang": recolour(mod("item/centipede_fang"), IVORY),
        "lindworm_venom_gland": recolour(mod("item/centipede_venom_sac"), [(30, 16, 40), (70, 40, 90), (110, 80, 150), (150, 190, 90), (220, 240, 180)]),
        "lindworm_heart": wit.from_grid(HEART, {"O": "#2a0808", "R": "#8c1a1a", "L": "#d05050", "D": "#5a0e0e", "G": "#2c3a24"}),
        "vittra_hood": recolour(vanilla("item/leather_helmet"), VITTRA_GREY, lo=0.3, hi=1.0),
        "trollhide_coat": recolour(vanilla("item/leather_chestplate"), TROLL_GREEN),
        "troll_tusk_knife": recolour(mod("item/beaver_tooth_knife"), [(30, 22, 14), (90, 66, 40), (150, 120, 80), (214, 204, 166), (246, 240, 220)]),
        "lindworm_fang_blade": recolour(vanilla("item/iron_sword"), [(20, 18, 14), (70, 56, 36), (150, 136, 100), (222, 212, 180), (250, 246, 230)]),
        # eggs: ash grey with black eyes; mossy green with brown hair; dark green scales with a gold belly
        "vittra_spawn_egg": wit.egg(["#c8ccd2", "#a4a8b0", "#7c8088", "#5c6068", "#44484e", "#2c2e34"],
                                    [(x, y, "#101014") for x, y in ((5, 6), (6, 6), (9, 6), (10, 6))]),
        "troll_spawn_egg": wit.egg(["#a6aa84", "#868c68", "#62684e", "#4a5038", "#3a3e2c", "#26281c"],
                                   [(x, y, "#3a2a1a") for y in (3, 4) for x in range(5, 11)] + [(7, 9, "#e0d8b0"), (9, 9, "#e0d8b0")]),
        "lindworm_spawn_egg": wit.egg(["#5a8a5c", "#3e6a46", "#2e5236", "#203c28", "#16301e", "#0e2014"],
                                      [(x, y, "#c4a452") for y in (11, 12, 13) for x in range(6, 10)] + [(x, 4, "#101810") for x in range(7, 9)]),
    }
    for piece in ("helmet", "chestplate", "leggings", "boots"):
        out[f"lindworm_scale_{piece}"] = scale_pattern(recolour(vanilla(f"item/iron_{piece}"), LINDWORM_GREEN, salt=63), 63)
    for name, im in out.items():
        im.save(it / f"{name}.png")
    block = RES / "block/troll_stone.png"
    block.parent.mkdir(parents=True, exist_ok=True)
    recolour(vanilla("block/mossy_cobblestone"), TROLL_GREEN, salt=64).save(block)


def folk():
    folk_armour()
    folk_items()


def main():
    import sys
    sys.path.insert(0, str(Path(__file__).resolve().parent))
    import gen_hunt_models  # the four beings' models and skins (UndergroundGeometry) come from one table each there
    gen_hunt_models.main()
    chitin_vest()
    items()
    folk()
    print("underground textures ok")


if __name__ == "__main__":
    main()
