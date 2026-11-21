"""Paint the underground fauna (scripts/gen_fauna_underground.py, com.mechanical.wildlife.underground): the Giant Centipede's
entity texture and its skinned (butchered) version, the Chitin Vest equipment layer and the 16x16 item textures; then the
Vittra, Troll and Lindworm (entity + skinned textures painted on FolkModel / LindwormModel boxes, armour layers, items
and the Troll Stone block recoloured from vanilla and mod textures; needs the merged Minecraft jar in build/moddev/artifacts).

Usage: python scripts/gen_fauna_underground_textures.py
The UV table below mirrors the texOffs and box sizes in GiantCentipedeModel (client/wildlife/underground). Deterministic:
hashes only, no randomness.
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
# skinned: the plates off, pale cream-grey flesh with the segment lines showing
S = dict(C, plate=(206, 194, 160), rim=(150, 132, 104), shine=(226, 218, 190), side=(196, 180, 146), belly=(214, 204, 172),
         head=(196, 150, 120))

# part -> (u, v, w, h, d), as GiantCentipedeModel's texOffs/addBox
UV = {"head": (0, 0, 4, 2, 3), "seg": (0, 6, 4, 2, 2), "tail": (0, 11, 3, 1, 2), "leg": (16, 0, 3, 1, 1),
      "claw": (16, 3, 1, 1, 2), "antenna": (28, 0, 1, 1, 5), "cerci": (40, 0, 1, 1, 3)}


def faces(u, v, w, hh, d):
    """Vanilla box UV: face -> (x0, y0, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
            "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh)}


def colour(P, part, face, i, j, fw, fh):
    if part in ("seg", "tail", "head"):
        base = P["head"] if part == "head" else P["plate"]
        if face == "bottom":
            return P["belly"]
        if face == "top":
            # the plate: a shine down the middle, a dark rim at the back edge (plates overlap front to back)
            c = lerp(base, P["shine"], 0.5) if part != "head" and i in (1, 2) and j == 0 else base
            if part != "head" and j == fh - 1:
                c = P["rim"]
            return c
        if face in ("left", "right"):
            return P["rim"] if part != "head" and i == fh - 1 else lerp(base, P["side"], 0.6)
        return shade(base, 0.85)
    if part == "leg":
        return P["leg_dark"] if face == "bottom" or (face in ("left", "right") and fw == 1) else P["leg"]
    if part == "claw":
        return P["tip"] if face in ("front",) or (face in ("top", "bottom", "left", "right") and j == 0 and fh > 1) else P["claw"]
    return P["leg"] if (i + j) % 3 else P["leg_dark"]  # antennae, cerci: ringed


def entity(P, salt):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    for part, box in UV.items():
        for face, (x0, y0, fw, fh) in faces(*box).items():
            for j in range(fh):
                for i in range(fw):
                    c = colour(P, part, face, i, j, fw, fh)
                    n = 0.92 + 0.16 * h(x0 + i, y0 + j, salt)
                    px[x0 + i, y0 + j] = (*shade(c, n), 255)
    return im


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
# Entity textures paint the boxes of FolkModel / LindwormModel (UV tables below mirror their texOffs); items, armour layers
# and the Troll Stone block are recoloured vanilla or mod textures.
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


def paint(im, uv, colour, salt):
    """Paints each part's box faces: colour(part, face, i, j, fw, fh) -> rgb, with a little per-pixel noise."""
    px = im.load()
    for part, box in uv.items():
        for face, (x0, y0, fw, fh) in faces(*box).items():
            for j in range(fh):
                for i in range(fw):
                    c = colour(part, face, i, j, fw, fh)
                    if c is None:
                        continue
                    n = 0.9 + 0.2 * h(x0 + i, y0 + j, salt)
                    px[x0 + i, y0 + j] = (*shade(c, n), 255)
    return im


# part -> (u, v, w, h, d) as FolkModel.createVittraLayer / createTrollLayer, LindwormModel.createBodyLayer
VITTRA_UV = {"head": (0, 0, 8, 8, 8), "ear": (32, 0, 1, 4, 2), "body": (16, 16, 6, 12, 3), "arm": (40, 16, 3, 12, 3),
             "leg": (0, 16, 3, 12, 3)}
TROLL_UV = {"head": (0, 0, 8, 8, 8), "nose": (32, 0, 2, 4, 3), "tusk": (44, 0, 1, 2, 1), "body": (0, 16, 10, 14, 6),
            "arm": (32, 16, 4, 16, 4), "tail": (48, 16, 1, 10, 1), "tuft": (52, 16, 2, 3, 2), "leg": (0, 36, 5, 12, 5)}
LINDWORM_UV = {"head": (0, 0, 6, 4, 8), "jaw": (30, 0, 5, 2, 7), "horn": (56, 0, 1, 1, 5), "leg": (72, 0, 2, 4, 2),
               "seg": (0, 20, 6, 5, 6), "spine": (28, 20, 1, 2, 4), "tail": (0, 34, 4, 4, 6), "tip": (0, 46, 2, 2, 6)}

# vittra: ash-grey skin, a ragged mane of black hair, huge black eyes with a pale glint, a moss-brown loincloth
VC = dict(skin=(124, 128, 136), dark=(88, 92, 102), light=(156, 160, 166), hair=(30, 30, 34), eye=(12, 12, 16),
          glint=(220, 230, 240), ear_in=(150, 120, 126), cloth=(84, 72, 46), cloth_dark=(60, 50, 32))
VS = dict(VC, skin=(196, 140, 128), dark=(160, 100, 92), light=(214, 168, 156), hair=(170, 110, 100), cloth=(176, 120, 108),
          cloth_dark=(150, 96, 86), ear_in=(186, 118, 110))


def vittra_colour(P):
    def c(part, face, i, j, fw, fh):
        if part == "head":
            if face == "top" or face == "back" or (face in ("left", "right") and j < 3):
                return P["hair"]
            if face == "front":
                if j < 2:
                    return P["hair"]
                if j in (3, 4) and i in (1, 2, 5, 6):  # the great eyes, two by two
                    return P["glint"] if j == 3 and i in (1, 5) else P["eye"]
                if j == 6 and i in (3, 4):
                    return P["dark"]  # mouth
                return P["skin"]
            return P["dark"] if face == "bottom" else P["skin"]
        if part == "ear":
            return P["ear_in"] if face == "front" else P["skin"]
        if part == "body":
            if j >= fh - 4 and face not in ("top",):
                return P["cloth_dark"] if j == fh - 1 else P["cloth"]
            return P["light"] if face == "front" and j < 6 else P["skin"]
        if part == "leg":
            return P["dark"] if j >= fh - 1 else P["skin"]
        return P["dark"] if j >= fh - 1 else P["skin"]  # arms: dark long fingers at the end
    return c


# troll: mossy grey-green warty skin, dark brown hair and tail tuft, small yellow eyes, ivory tusks, a hide loincloth
TC = dict(skin=(98, 106, 80), dark=(66, 72, 54), wart=(128, 132, 100), hair=(58, 42, 28), eye=(214, 170, 40),
          tusk=(222, 212, 172), nose=(116, 108, 84), cloth=(110, 82, 52), cloth_dark=(80, 58, 36), belly=(122, 124, 96))
TS = dict(TC, skin=(152, 72, 62), dark=(118, 50, 44), wart=(176, 100, 88), hair=(130, 60, 52), nose=(160, 80, 70),
          cloth=(150, 70, 60), cloth_dark=(120, 52, 46), belly=(176, 108, 96))


def troll_colour(P):
    def warty(face, i, j, salt):
        return P["wart"] if h(i * 7 + j, j * 3 + i, salt) > 0.86 else (P["belly"] if face == "front" else P["skin"])

    def c(part, face, i, j, fw, fh):
        if part == "head":
            if face == "top" or face == "back" or (face in ("left", "right") and j < 2):
                return P["hair"]
            if face == "front":
                if j == 0:
                    return P["hair"]
                if j == 3 and i in (1, 2, 5, 6):
                    return P["eye"] if i in (2, 5) else P["dark"]
                if j == 2 and 1 <= i <= 6:
                    return P["dark"]  # heavy brow
                if j == 7 and 2 <= i <= 5:
                    return P["dark"]  # wide mouth
            return warty(face, i, j, 31)
        if part == "nose":
            return P["nose"] if face != "bottom" else P["dark"]
        if part == "tusk":
            return P["tusk"]
        if part in ("tail",):
            return P["skin"]
        if part == "tuft":
            return P["hair"]
        if part == "body":
            if face == "back" and j < 4 or face == "top":
                return P["hair"]  # a shaggy back
            if j >= fh - 4 and face != "top":
                return P["cloth_dark"] if j == fh - 1 or (i + j) % 5 == 0 else P["cloth"]
            return warty(face, i, j, 37)
        if part == "arm":
            return P["dark"] if j >= fh - 2 else warty(face, i, j, 41)
        return P["dark"] if j >= fh - 1 else warty(face, i, j, 43)  # legs, broad dark feet
    return c


# lindworm: dark green scales with a paler scale edge, a golden belly, a black spine ridge, yellow eyes, ivory horns and teeth
LC = dict(scale=(46, 82, 54), edge=(70, 112, 70), dark=(26, 44, 30), belly=(196, 164, 82), belly_dark=(160, 128, 58),
          eye=(240, 200, 40), pupil=(20, 14, 6), horn=(224, 214, 176), tooth=(240, 236, 220), mouth=(120, 40, 40), claw=(24, 20, 18))
LS = dict(LC, scale=(176, 88, 78), edge=(196, 112, 100), dark=(130, 56, 50), belly=(212, 160, 140), belly_dark=(188, 132, 116),
          mouth=(150, 60, 56))


def lindworm_colour(P):
    def scales(i, j):
        # offset rows of scales: a pale edge on every scale's lower rim
        return P["edge"] if (j % 2 == 1 and (i + j // 2) % 2 == 0) else P["scale"]

    def c(part, face, i, j, fw, fh):
        if part in ("seg", "tail", "tip"):
            if face == "bottom":
                return P["belly_dark"] if i in (0, fw - 1) or j % 3 == 2 else P["belly"]
            if face in ("left", "right") and j == fh - 1:
                return P["belly_dark"]
            return scales(i, j)
        if part == "spine":
            return P["dark"]
        if part == "head":
            if face == "front":
                return P["dark"] if j == 0 else (P["tooth"] if j == fh - 1 and i % 2 == 0 else scales(i, j))
            if face in ("left", "right"):
                if j == 1 and i in (fw - 3, fw - 2):  # the eye, back along the head
                    return P["pupil"] if i == fw - 2 else P["eye"]
                if j == fh - 1:
                    return P["tooth"] if i % 2 == 0 and i < fw - 2 else P["mouth"]
            if face == "bottom":
                return P["mouth"]
            return scales(i, j)
        if part == "jaw":
            if face == "top":
                return P["mouth"]
            if face in ("left", "right") and j == 0:
                return P["tooth"] if i % 2 == 0 else P["mouth"]
            return P["belly"] if face == "bottom" else scales(i, j)
        if part == "horn":
            return P["horn"]
        if part == "leg":
            return P["claw"] if j == fh - 1 else scales(i, j)
        return P["scale"]
    return c


def folk_entities():
    for name, size, uv, colour, pal, skinned, salt in (
            ("vittra", (64, 64), VITTRA_UV, vittra_colour, VC, VS, 21),
            ("troll", (64, 64), TROLL_UV, troll_colour, TC, TS, 22),
            ("lindworm", (128, 64), LINDWORM_UV, lindworm_colour, LC, LS, 23)):
        for rel, P in ((f"entity/wildlife/{name}.png", pal), (f"entity/skinned/wildlife/{name}.png", skinned)):
            out = RES / rel
            out.parent.mkdir(parents=True, exist_ok=True)
            paint(Image.new("RGBA", size, (0, 0, 0, 0)), uv, colour(P), salt).save(out)


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
    folk_entities()
    folk_armour()
    folk_items()


def main():
    for rel, P, salt in (("entity/wildlife/giant_centipede.png", C, 11), ("entity/skinned/wildlife/giant_centipede.png", S, 12)):
        out = RES / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        entity(P, salt).save(out)
    chitin_vest()
    items()
    folk()
    print("underground textures ok")


if __name__ == "__main__":
    main()
