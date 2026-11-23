"""Folklore textures (gen_folklore.py, com.mechanical.folklore). Deterministic: hashes only, no randomness.

- entity/wildlife/mountain_watcher.png: the golden eagle's 64x64 skin (same model, client.folklore.FolkloreClient) turned
  pale: luminance mapped onto bone-white to silver-grey, with a faint pearly shimmer (scattered pale cyan/violet texels).
- item/mountain_watcher_spawn_egg.png, item/watcher_feather.png: the golden eagle egg and Eagle Feather recoloured the same way.
- item/boiled_fly_agaric.png: the Fly Agaric item, the red cooked down to a dull orange-brown, the spots greyed.
- block/glimmering_stump_side/_top.png: vanilla oak log bark and rings (from the merged jar) overgrown with moss.
- mob_effect/second_sight.png: an 18x18 violet eye.
- The folklore beings (Beings): their models and skins come from gen_folklore_models.py; here only
  the spawn eggs (the zombie egg recoloured), the gifts and trophies (pixel art or vanilla items recoloured), Frost Heart
  and Mossy Troll Stone blocks, and the Lost, Mountain Guest and Fiddler's Blessing effect icons.
Run: python scripts/gen_folklore_textures.py (needs a gradle build for the merged jar).
"""
import glob
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))
assert JAR, "no merged minecraft jar (run a gradle build first)"


def hsh(x, y, salt):
    n = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    n = (n ^ (n >> 13)) * 1274126177
    return ((n ^ (n >> 16)) & 0xFFFF) / 65535.0


def lerp(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def luma(c):
    return (0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]) / 255


def pale(src, salt):
    """Golden-brown plumage to pale: dark parts silver-grey, light parts bone-white, a pearly glint here and there."""
    im = src.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = min(1.0, luma((r, g, b)) * 1.6 + 0.15)
            c = lerp((118, 124, 138), (246, 243, 232), l)
            k = hsh(x, y, salt)
            if k > 0.93:
                c = lerp(c, (200, 236, 250) if k > 0.965 else (224, 206, 250), 0.6)  # the shimmer
            px[x, y] = (*c, a)
    return im


def boiled(src):
    im = src.copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = luma((r, g, b))
            if r > g + 40:  # the red cap: cooked down to dull orange-brown
                c = lerp((92, 52, 30), (176, 110, 58), min(1, l * 2.2))
            elif l > 0.7:  # white spots and stem: greyed, soaked
                c = lerp((r, g, b), (190, 176, 150), 0.55)
            else:
                c = lerp((r, g, b), (110, 90, 70), 0.3)
            px[x, y] = (*c, a)
    return im


def mossy(src, salt, amount):
    im = src.convert("RGBA")
    px = im.load()
    mosses = [(62, 92, 34), (78, 112, 40), (96, 132, 50), (54, 80, 30)]
    for y in range(im.height):
        for x in range(im.width):
            # thicker towards the foot of the bark (and the rim of the top), clumped by a coarse hash
            lean = (y / (im.height - 1)) if salt == 1 else min(x, y, 15 - x, 15 - y) < 2
            clump = hsh(x // 2, y // 2, salt + 7)
            if clump * 0.6 + hsh(x, y, salt) * 0.4 < amount * (0.5 + float(lean)):
                px[x, y] = (*mosses[int(hsh(x, y, salt + 3) * 4) % 4], 255)
    return im


def eye():
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    px = im.load()
    for y in range(18):
        for x in range(18):
            dx, dy = (x - 8.5) / 8, (y - 8.5) / 4.6
            d = dx * dx + dy * dy
            if d > 1:
                continue
            r = ((x - 8.5) ** 2 + (y - 8.5) ** 2) ** 0.5
            if d > 0.72:
                px[x, y] = (88, 40, 140, 255)  # lid line
            elif r < 1.6:
                px[x, y] = (24, 10, 40, 255)  # pupil
            elif r < 3.6:
                px[x, y] = lerp((150, 90, 230), (210, 170, 255), hsh(x, y, 9))  # iris
            else:
                px[x, y] = (236, 230, 246, 255)
    px[7, 7] = (255, 255, 255, 255)  # glint
    return im


# ---------------------------------------------------------------- the folklore beings (folklore.Beings, client.folklore.BeingsClient)
# Skins are 64x64 in the zombie layer's layout (only the classic top half is used): Steve's, the zombie's or the husk's,
# recoloured by what each texel is (skin, hair, shirt, trousers) and painted over where a being needs it.

def ramp(dark, light, l):
    return lerp(dark, light, max(0.0, min(1.0, l)))


def egg(src, base, spot, dark):
    """A mob-shaped spawn egg (the zombie's) recoloured: its skin, shirt and trousers become three tones."""
    im = src.convert("RGBA").copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            l = luma((r, g, b))
            if l < 0.14:
                continue  # outline and eyes
            if g > r + 40 and b > r + 40:
                c = ramp(base[0], base[1], l * 1.7)
            elif b > g + 30:
                c = ramp(dark[0], dark[1], l * 2.2)
            else:
                c = ramp(spot[0], spot[1], l * 1.7)
            px[x, y] = (*c, a)
    return im


def art(rows, pal, size=16):
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), (*pal[ch], 255))
    return im


def recolour(src, dark, light, gain=1.6):
    im = src.convert("RGBA").copy()
    px = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (*ramp(dark, light, luma((r, g, b)) * gain), a)
    return im


COMB = ["................",
        "................",
        "................",
        "...oooooooooo...",
        "..oLLLLLLLLLLo..",
        "..oLHHLLHHLLLo..",
        "..oLLLLLLLLLLo..",
        "..oooooooooooo..",
        "..oLoLoLoLoLoL..",
        "..oLoLoLoLoLoL..",
        "..oLoLoLoLoLoL..",
        "..oLoLoLoLoLoL..",
        "...o.o.o.o.o.o..",
        "................",
        "................",
        "................"]
MASK = ["................",
        "....oooooooo....",
        "...oRRYYYYRRo...",
        "..oWWWWWWWWWWo..",
        "..oWKKWWWWKKWo..",
        "..oWKYWWWWYKWo..",
        "..oWWWWRRWWWWo..",
        "..oRWWWRRWWWRo..",
        "..oWWWWRRWWWWo..",
        "...oWTTTTTTWo...",
        "...oWKKKKKKWo...",
        "....oWWWWWWo....",
        ".....oRWWRo.....",
        "......oooo......",
        "................",
        "................"]
GOAT = ["................",
        "..r.............",
        ".rYr............",
        "..YYo...........",
        "..YKYo..........",
        "...YYYYYYYYYY...",
        "...YRRRRYYYYYo..",
        "...YYYYYYYYYYY..",
        "....YYYYYYYYY...",
        "....Y.Y...Y.Y...",
        "....Y.Y...Y.Y...",
        "....Y.Y...Y.Y...",
        "...oo.oo.oo.oo..",
        "................",
        "................",
        "................"]
TORC = ["................",
        "................",
        ".....oooooo.....",
        "...ooLHLHLHoo...",
        "..oLHo....oHLo..",
        "..oHo......oHo..",
        ".oLo........oLo.",
        ".oHo........oHo.",
        ".oLo........oLo.",
        "..oHo......oHo..",
        "..oLHo....oHLo..",
        "...oKo....oKo...",
        "..oKKo....oKKo..",
        "...oo......oo...",
        "................",
        "................"]


def icon(rows, pal):
    im = Image.new("RGBA", (18, 18), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x + 1, y + 1), (*pal[ch], 255))
    return im


LOST = ["................",
        ".....oooooo.....",
        "...oo......oo...",
        "..o...gggg...o..",
        ".o...g....g...o.",
        ".o..g..gg..g..o.",
        "o..g..g..g..g..o",
        "o..g..g.og..g..o",
        "o..g..g..g..g..o",
        "o..g...gg...g..o",
        ".o..g......g..o.",
        ".o...gggggg...o.",
        "..o..........o..",
        "...oo......oo...",
        ".....oooooo.....",
        "................"]
GUEST = ["................",
         "......KKKK......",
         ".....KGGGGK.....",
         "....KGGGGGGK....",
         "...KGGkGGkGGK...",
         "...KGGGGGGGGK...",
         "....KGGGGGGK....",
         ".....KGGGGK.....",
         "....KKGGGGKK....",
         "...KGGGGGGGGK...",
         "..KGGGGGGGGGGK..",
         "..KGGGGGGGGGGK..",
         "..KGGGGGGGGGGK..",
         "..KKKKKKKKKKKK..",
         "................",
         "................"]
FIDDLE = ["............w...",
          "...........w....",
          "..........w.....",
          ".........w......",
          "......BBw.......",
          ".....BBwB.......",
          "....BBwBB.......",
          "...BBBwBBB......",
          "..BBBwBBBB......",
          "..BbwbBBB.......",
          "...BBBBBB.......",
          "..BBBBBBB.......",
          ".BBBbbBBB.......",
          ".BBBBBBB........",
          "..BBBBB.........",
          "................"]


def beings(vanilla):
    egg_src = vanilla("item/zombie_spawn_egg.png")
    out = {}
    eggs = {  # base (shirt), spots (skin), trousers
        "huldra": (((46, 72, 34), (106, 138, 62)), ((200, 160, 90), (248, 226, 150)), ((70, 50, 30), (118, 88, 56))),
        "will_o_wisp": (((20, 40, 60), (60, 110, 140)), ((120, 220, 230), (220, 255, 255)), ((30, 50, 40), (60, 90, 70))),
        "jungle_spirit_mask": (((110, 60, 28), (170, 104, 52)), ((180, 40, 30), (230, 190, 60)), ((30, 110, 50), (60, 160, 80))),
        "tomte": (((84, 84, 80), (156, 154, 146)), ((150, 24, 22), (210, 50, 40)), ((200, 200, 196), (250, 250, 246))),
        "nacken": (((30, 58, 40), (66, 98, 66)), ((140, 168, 156), (210, 226, 212)), ((14, 24, 20), (44, 64, 54))),
        "bog_body": (((40, 26, 14), (100, 70, 40)), ((120, 50, 20), (176, 84, 36)), ((24, 16, 10), (60, 42, 24))),
        "frost_giant": (((60, 96, 140), (160, 200, 236)), ((200, 230, 250), (250, 254, 255)), ((30, 50, 90), (80, 120, 170))),
        "dune_wraith": (((150, 120, 70), (230, 206, 150)), ((100, 76, 40), (170, 140, 90)), ((60, 46, 26), (110, 90, 56))),
    }
    for name, (base, spot, dark) in eggs.items():
        out[f"item/{name}_spawn_egg.png"] = egg(egg_src, base, spot, dark)
    bone = {"o": (70, 54, 36), "L": (226, 214, 180), "H": (250, 244, 222)}
    out["item/huldra_comb.png"] = art(COMB, bone)
    out["item/spirit_mask.png"] = art(MASK, {"o": (40, 22, 10), "R": (176, 38, 30), "Y": (226, 184, 52), "W": (162, 100, 50),
                                             "K": (20, 12, 8), "T": (236, 230, 210)})
    out["item/straw_yule_goat.png"] = art(GOAT, {"Y": (222, 186, 96), "o": (150, 112, 50), "K": (60, 40, 20), "r": (196, 40, 34),
                                                 "R": (196, 40, 34)})
    out["item/bog_torc.png"] = art(TORC, {"o": (60, 40, 18), "L": (176, 120, 52), "H": (232, 186, 100), "K": (130, 86, 34)})
    out["item/wisp_ember.png"] = recolour(vanilla("item/magma_cream.png"), (20, 60, 90), (200, 250, 255), 1.4)
    out["item/nacken_string.png"] = recolour(vanilla("item/string.png"), (120, 150, 170), (230, 245, 255))
    out["item/peat_tanned_hide.png"] = recolour(vanilla("item/leather.png"), (30, 20, 12), (112, 78, 44), 1.8)
    out["item/vittra_silver.png"] = recolour(vanilla("item/iron_nugget.png"), (90, 96, 108), (236, 240, 248), 1.5)
    out["item/dune_glass.png"] = recolour(vanilla("item/amethyst_shard.png"), (140, 110, 60), (250, 236, 190), 1.5)
    out["block/mossy_troll_stone.png"] = recolour(vanilla("block/mossy_cobblestone.png"), (44, 56, 36), (150, 158, 132), 1.4)

    heart = recolour(vanilla("block/blue_ice.png"), (60, 110, 170), (210, 236, 255), 1.3)
    hp = heart.load()
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.2:
                hp[x, y] = (*ramp((200, 240, 255), (255, 255, 255), 1 - d / 3.2), 255)  # the cold bright core
    out["block/frost_heart.png"] = heart
    out["mob_effect/lost.png"] = icon(LOST, {"o": (70, 90, 50), "g": (130, 170, 90)})
    out["mob_effect/mountain_guest.png"] = icon(GUEST, {"K": (50, 52, 60), "G": (138, 143, 154), "k": (10, 10, 14)})
    out["mob_effect/fiddlers_blessing.png"] = icon(FIDDLE, {"B": (140, 76, 36), "b": (60, 30, 14), "w": (220, 230, 240)})
    return out


def main():
    jar = zipfile.ZipFile(JAR[0])
    vanilla = lambda p: Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{p}")))
    out = {
        "entity/wildlife/mountain_watcher.png": pale(Image.open(TEX / "entity/wildlife/golden_eagle.png").convert("RGBA"), 11),
        "item/mountain_watcher_spawn_egg.png": pale(Image.open(TEX / "item/golden_eagle_spawn_egg.png").convert("RGBA"), 12),
        "item/watcher_feather.png": pale(Image.open(TEX / "item/eagle_feather.png").convert("RGBA"), 13),
        "item/boiled_fly_agaric.png": boiled(Image.open(TEX / "item/fly_agaric.png").convert("RGBA")),
        "block/glimmering_stump_side.png": mossy(vanilla("block/oak_log.png"), 1, 0.45),
        "block/glimmering_stump_top.png": mossy(vanilla("block/oak_log_top.png"), 2, 0.4),
        "mob_effect/second_sight.png": eye(),
    }
    out.update(beings(vanilla))
    for path, im in out.items():
        (TEX / path).parent.mkdir(parents=True, exist_ok=True)
        im.save(TEX / path)
    print(f"folklore textures: {len(out)}")


if __name__ == "__main__":
    main()
