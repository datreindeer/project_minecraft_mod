"""Paint the night monsters (data: scripts/gen_fauna_night.py; code: wildlife/night/, client/wildlife/night/).

- entity/wildlife/gloson.png: the wild boar's 64x64 skin (same model, WildBoarModel) turned ashen: luminance mapped onto a
  cold charcoal-to-ash-grey ramp, the bristle crest paler, the small eyes amber. entity/wildlife/gloson_eyes.png: only
  those eyes (and a dim rim), drawn full-bright by AngryEyesLayer. entity/skinned/wildlife/gloson.png: the boar's skinned
  carcass skin (flesh is flesh).
- entity/night/mushroom_man.png (64x64): a brown, pale-speckled cap with cream gills under it and a knob on top, a pale
  stem of a body with two hollow dark eyes and a mouth slit, stubby feet and thin arms. The box table (BOXES) mirrors the
  cubes in MushroomManModel: keep them in sync.
- items (16x16): Raw/Cooked Gloson (vanilla porkchops, greyed), Gloson Bristles, Razor Ridge, Razor Knife, Mushroom Flesh,
  Spore Sac, Spore Bomb (the Puffball, tied up), the two spawn eggs; mob_effect/bleeding.png (18x18 blood drop).
Run: python scripts/gen_fauna_night_textures.py   (deterministic; needs the merged jar from a gradle build)
"""
import shutil
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import h, lerp  # noqa: E402
from gen_wildlife_item_textures import OUT as ITEM, egg, from_grid, recolour  # noqa: E402
from gen_bronze_prep_textures import vanilla  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ENT = TEX / "entity"


def lum(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


# =============================================================== GLOSON
ASH = [(22, 24, 28), (52, 55, 60), (92, 95, 100), (140, 143, 146), (190, 192, 194)]
EYES = {(3, 40): (255, 214, 120), (13, 40): (255, 214, 120), (2, 40): (255, 168, 60), (14, 40): (255, 168, 60)}
RIM = [(3, 39), (13, 39), (3, 41), (13, 41)]


def ramp(t):
    t = max(0.0, min(1.0, t)) * (len(ASH) - 1)
    i = min(int(t), len(ASH) - 2)
    return lerp(ASH[i], ASH[i + 1], t - i)


def gloson():
    boar = Image.open(ENT / "wildlife/wild_boar.png").convert("RGBA")
    out = Image.new("RGBA", boar.size, (0, 0, 0, 0))
    eyes = Image.new("RGBA", boar.size, (0, 0, 0, 0))
    src, px, ex = boar.load(), out.load(), eyes.load()
    for y in range(boar.height):
        for x in range(boar.width):
            c = src[x, y]
            if not c[3]:
                continue
            t = 0.22 + lum(c) * 0.9 + (h(x, y, 401) - 0.5) * 0.03  # the dark boar lifted to ash, its bristle noise softened
            px[x, y] = (*ramp(t), c[3])
    for p, c in EYES.items():
        px[p] = (*c, 255)
        ex[p] = (*c, 255)
    for p in RIM:
        ex[p] = (150, 70, 20, 255)
    out.save(ENT / "wildlife/gloson.png")
    eyes.save(ENT / "wildlife/gloson_eyes.png")
    (ENT / "skinned/wildlife").mkdir(parents=True, exist_ok=True)
    shutil.copyfile(ENT / "skinned/wildlife/wild_boar.png", ENT / "skinned/wildlife/gloson.png")


# =============================================================== MUSHROOM MAN
CAP_TOP, CAP_DARK, CAP_SPOT = (128, 84, 50), (88, 54, 32), (214, 196, 160)
GILL, GILL_DARK = (222, 206, 172), (176, 156, 120)
STEM, STEM_SHADE, HOLLOW, MOSS = (226, 216, 192), (184, 170, 140), (36, 26, 20), (110, 122, 70)

# name: (u, v, w, h, d) as in MushroomManModel's texOffs/addBox
BOXES = {"cap": (0, 0, 14, 5, 14), "knob": (0, 19, 8, 2, 8), "stem": (0, 29, 6, 10, 5), "leg": (24, 29, 3, 5, 3), "arm": (36, 29, 2, 8, 2)}


def faces(u, v, w, hh, d):
    """Box UV faces -> (x0, y0, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
            "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh)}


def cap_mat(name, face, i, j, fw, fh, salt):
    n = (h(i, j, salt) - 0.5) * 0.18
    if face == "bottom":
        ci, cj = i - (fw - 1) / 2, j - (fh - 1) / 2
        if name == "cap" and abs(ci) < 2 and abs(cj) < 2:
            return STEM_SHADE  # where the stem meets it
        return GILL_DARK if (i + j) % 2 and abs(ci) != abs(cj) else GILL  # radiating gills
    c = CAP_TOP if face == "top" else lerp(CAP_TOP, CAP_DARK, j / max(fh - 1, 1))
    if face != "top" and name == "cap" and j == fh - 1:
        c = GILL  # the cream rim of the gills shows under the edge
    if h(i * 3 + 1, j * 5 + 2, salt + 7) < 0.12:
        c = CAP_SPOT
    return tuple(max(0, min(255, round(k * (1 + n)))) for k in c)


def stem_mat(name, face, i, j, fw, fh, salt):
    n = (h(i, j, salt) - 0.5) * 0.12
    c = lerp(STEM, STEM_SHADE, 0.6 if face in ("left", "right", "back") else 0.0)
    if face == "front" and name == "stem":
        if j in (2, 3) and i in (1, 4):
            return HOLLOW  # two hollow eyes
        if j == 6 and 2 <= i <= 3:
            return HOLLOW  # a mouth slit
    if face == "front" and name == "stem" and j == 3 and i in (0, 5):
        c = STEM_SHADE
    if j >= fh - 2 and name in ("leg", "stem") and face != "top":
        c = lerp(c, MOSS, 0.45 if name == "leg" else 0.25)  # earth and moss creeping up from the feet
    if h(i * 7, j * 3, salt + 3) < 0.08:
        c = lerp(c, MOSS, 0.5)
    return tuple(max(0, min(255, round(k * (1 + n)))) for k in c)


def mushroom_man():
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for n, (name, box) in enumerate(BOXES.items()):
        mat = cap_mat if name in ("cap", "knob") else stem_mat
        for face, (x0, y0, fw, fh) in faces(*box).items():
            for j in range(fh):
                for i in range(fw):
                    im.putpixel((x0 + i, y0 + j), (*mat(name, face, i, j, fw, fh, 500 + n * 11), 255))
    (ENT / "night").mkdir(parents=True, exist_ok=True)
    im.save(ENT / "night/mushroom_man.png")


# =============================================================== ITEMS
def grey_meat(name, src, amount):
    im = vanilla(src).convert("RGBA")
    px = im.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if a:
                l = round(0.299 * r + 0.587 * g + 0.114 * b)
                px[x, y] = (round(r + (l - r) * amount), round(g + (l - g) * amount), round(b + (l - b) * amount + 6 * amount), a)
    im.save(ITEM / f"{name}.png")


BRISTLES = [
    "................",
    "..O.......O.....",
    "..OL..O..OL..O..",
    "...OL.OL.OL.OL..",
    "...OLMOL.OLMOL..",
    "....OLMOLOLMO...",
    "....OLMOLLMO..O.",
    ".O..OLMLLMMO.OL.",
    ".OL..OLMMLMOOL..",
    "..OL.OLMMLMOL...",
    "..OLLOMMLLMO....",
    "...OOMLMMMDO....",
    "....OTTTTTTO....",
    "....OTWWWWTO....",
    ".....OOOOOO.....",
    "................",
]
BRISTLES_PAL = {"O": "#2a2c30", "L": "#c8cacc", "M": "#8e9196", "D": "#5c5f64", "T": "#8a6e4c", "W": "#b8975a"}

RIDGE = [
    "................",
    "................",
    "..O...O...O.....",
    ".OWO.OWO.OWO..O.",
    ".OLLOOLLOOLLOOWO",
    "OLLMLLLMLLLMLLLO",
    "OMLMMLMMMLMMMLMO",
    "ODMMDMMDMMDMMDO.",
    ".OSSDSSSDSSSDSO.",
    ".ODSSSDSSSSDSO..",
    "..OOODSSSDSOO...",
    ".....OOOOOO.....",
    "................",
    "................",
    "................",
    "................",
]
RIDGE_PAL = {"O": "#26282c", "W": "#f4f4f2", "L": "#d4d6d8", "M": "#9a9ea4", "D": "#62666c", "S": "#7a7f86"}

KNIFE = [
    "................",
    "..............O.",
    ".............OWO",
    "............OWLO",
    "...........OWLMO",
    "..........OWLMO.",
    ".........OWLMO..",
    "........OWLMDO..",
    ".......OWLMDO...",
    "......OSLMDO....",
    ".....OTTSDO.....",
    "....OTWTTO......",
    "...OTTWTO.......",
    "..OBTTTO........",
    ".OBBTO..........",
    "..OO............",
]
KNIFE_PAL = {"O": "#26282c", "W": "#f2f2f0", "L": "#c8cbcf", "M": "#8f949a", "D": "#5a5e64", "S": "#c9b48e",
             "T": "#8a6440", "B": "#5e4228"}

FLESH = [
    "................",
    "................",
    ".....OOOO.......",
    "...OOCCCCOO.....",
    "..OCCWCCCCCO....",
    "..OCWCCCPCCCO...",
    ".OCCCCCCCCPCO...",
    ".OCCPCCCCCCCCO..",
    ".OCCCCCPCCWCCO..",
    "..OCCCCCCCCCCO..",
    "..OBCCPCCCCCBO..",
    "...OBBCCCCBBO...",
    "....OOBBBBOO....",
    "......OOOO......",
    "................",
    "................",
]
FLESH_PAL = {"O": "#5a4632", "C": "#e2d2b0", "W": "#f4ead2", "P": "#c4ac84", "B": "#a88a62"}

SAC = [
    "................",
    "................",
    ".......OO.......",
    "......OTTO......",
    ".....OOTTOO.....",
    "....OSSLLSSO....",
    "...OSLLWLLSSO...",
    "..OSLWLLLLLSSO..",
    "..OSLLLDLLLLSO..",
    "..OSLLLLLDLLSO..",
    "..OSSLDLLLLSSO..",
    "...OSSLLLLSSO...",
    "....OSSSSSSO....",
    ".....OOOOOO.....",
    "................",
    "................",
]
SAC_PAL = {"O": "#4a3e24", "T": "#8a7a4e", "S": "#a8995e", "L": "#cfc28a", "W": "#efe6bc", "D": "#7c6e40"}

DROP = [
    "..................",
    "........OO........",
    "........OO........",
    ".......ORRO.......",
    ".......ORRO.......",
    "......ORRRRO......",
    "......ORWRRO......",
    ".....ORWRRRRO.....",
    ".....ORWRRRRO.....",
    "....ORWRRRRRDO....",
    "....ORRRRRRRDO....",
    "....ORRRRRRDDO....",
    "....ORRRRRRDDO....",
    ".....ORRRRDDO.....",
    ".....ODRRDDDO.....",
    "......ODDDDO......",
    ".......OOOO.......",
    "..................",
]
DROP_PAL = {"O": "#3a0606", "R": "#b01818", "W": "#f08080", "D": "#781010"}


def spore_bomb():
    im = Image.open(ITEM / "puffball.png").convert("RGBA")
    px = im.load()
    rows = [y for y in range(16) if any(px[x, y][3] for x in range(16))]
    mid = (rows[0] + rows[-1]) // 2 if rows else 8
    for x in range(16):  # a twine band round its middle, knotted on top
        if px[x, mid][3]:
            px[x, mid] = (150, 120, 70, 255)
    top = rows[0] if rows else 4
    cx = 8
    for dx, c in ((-1, (150, 120, 70, 255)), (0, (110, 86, 48, 255)), (1, (150, 120, 70, 255))):
        if top > 0:
            px[cx + dx, top - 1] = c
    im.save(ITEM / "spore_bomb.png")


def items():
    grey_meat("raw_gloson", "item/porkchop", 0.45)
    grey_meat("cooked_gloson", "item/cooked_porkchop", 0.3)
    from_grid(BRISTLES, BRISTLES_PAL).save(ITEM / "gloson_bristles.png")
    from_grid(RIDGE, RIDGE_PAL).save(ITEM / "razor_ridge.png")
    from_grid(KNIFE, KNIFE_PAL).save(ITEM / "razor_knife.png")
    from_grid(FLESH, FLESH_PAL).save(ITEM / "mushroom_flesh.png")
    from_grid(SAC, SAC_PAL).save(ITEM / "spore_sac.png")
    spore_bomb()
    G = "#ffcf70"  # glowing eyes
    egg(["#a7aaae", "#8a8d92", "#6c7076", "#50545a", "#3a3d42", "#26282c"],
        [(6, 6, G), (9, 6, G)], [(7, 0), (8, 0)],
        [(7, 0, "#d0d2d4"), (8, 0, "#d0d2d4"), (7, 1, "#d0d2d4"), (8, 1, "#e8e8e8"), (7, 2, "#d0d2d4"), (8, 3, "#d0d2d4"),
         (6, 12, "#9a8a8a"), (7, 12, "#b8a8a8"), (8, 12, "#b8a8a8"), (9, 12, "#9a8a8a")]).save(ITEM / "gloson_spawn_egg.png")
    S = "#e6dab8"
    egg(["#e8dcc0", "#d4c6a4", "#bfae88", "#a08e68", "#7a6a4a", "#544630"],
        [(5, 9, "#2a1e16"), (9, 9, "#2a1e16")], (),
        [(x, y, "#8a5a34" if (x + y) % 3 else S) for y in range(1, 6) for x in range(3, 13) if not (y == 1 and (x < 6 or x > 9))]
        ).save(ITEM / "mushroom_man_spawn_egg.png")
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y, row in enumerate(DROP):
        for x, ch in enumerate(row):
            if ch in DROP_PAL:
                c = DROP_PAL[ch]
                im.putpixel((x, y), (int(c[1:3], 16), int(c[3:5], 16), int(c[5:7], 16), 255))
    im.save(TEX / "mob_effect/bleeding.png")


if __name__ == "__main__":
    gloson()
    mushroom_man()
    items()
    print("night textures ok")
