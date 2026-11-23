"""Textures for the temperate fauna (wildlife/temperate, scripts/gen_fauna_temperate.py).

The animals have their own models; their skins come from scripts/gen_temperate_models.py. Items and blocks: repainted mod textures where a close one exists (meats, pelts, rug, eggs, the
marten cap), else drawn here.
Run: python scripts/gen_fauna_temperate_textures.py (after the textures it reads exist)."""
import random
from pathlib import Path

from PIL import Image

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ENT, ITEM, BLOCK = TEX / "entity/wildlife", TEX / "item", TEX / "block"
OUT = ENT / "temperate"
OUT.mkdir(parents=True, exist_ok=True)
rng = random.Random(1123)


def hexc(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def lum(p):
    return (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255


def ramp(stops, t):
    """stops: list of hex colours dark -> light, spread evenly; t 0..1."""
    cs = [hexc(s) for s in stops]
    t = min(max(t, 0.0), 1.0) * (len(cs) - 1)
    i = min(int(t), len(cs) - 2)
    f = t - i
    return tuple(round(cs[i][k] + (cs[i + 1][k] - cs[i][k]) * f) for k in range(3))


def recolor(src, stops, contrast=1.0, where=None):
    """Repaints every opaque pixel by its brightness (normalised over the image) along the ramp. where(x, y) limits it."""
    im = Image.open(src).convert("RGBA") if not isinstance(src, Image.Image) else src.copy()
    px = im.load()
    ls = [lum(px[x, y]) for x in range(im.width) for y in range(im.height) if px[x, y][3] > 0]
    lo, hi = (min(ls), max(ls)) if ls else (0, 1)
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if a == 0 or (where and not where(x, y)):
                continue
            t = (lum((r, g, b)) - lo) / max(hi - lo, 1e-3)
            t = 0.5 + (t - 0.5) * contrast
            px[x, y] = ramp(stops, t) + (a,)
    return im


def speckle(im, colour, chance, where=None, seed=0):
    r = random.Random(seed)
    px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            if px[x, y][3] and (not where or where(x, y)) and r.random() < chance:
                px[x, y] = hexc(colour) + (px[x, y][3],)
    return im


def save(im, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


# the animals' skins (and their models) are painted by scripts/gen_temperate_models.py


# ---------------------------------------------------------------- items

def from_item(src, stops, name, contrast=1.0, dots=None, seed=0):
    im = recolor(ITEM / f"{src}.png", stops, contrast)
    if dots:
        speckle(im, dots[0], dots[1], seed=seed)
    save(im, ITEM / f"{name}.png")


RAW_RED = ["#3a0d0d", "#6e1c1c", "#a03232", "#c95a50", "#e89a8a"]
COOKED = ["#2a160a", "#4c2a14", "#77461f", "#9c6730", "#c08f52"]
from_item("raw_bison", ["#33090b", "#5f1518", "#8e2a2a", "#b4504a", "#d88a7e"], "raw_wisent")
from_item("cooked_bison", ["#24120a", "#3f2412", "#663c1c", "#8a5a2c", "#ad7f48"], "cooked_wisent")
from_item("tortoise_meat", ["#3a1418", "#6a2a2e", "#965050", "#ba7c72", "#d8a89a"], "pond_turtle_meat")
from_item("cooked_tortoise_meat", COOKED, "cooked_pond_turtle_meat")
from_item("raw_vole", RAW_RED, "raw_water_vole")
from_item("cooked_vole", COOKED, "cooked_water_vole")
for bird, raw_src, plucked_src in (("skylark", "raw_pigeon", "plucked_pigeon"), ("lapwing", "raw_pigeon", "plucked_pigeon"),
                                   ("bittern", "raw_pheasant", "plucked_pheasant")):
    from_item(raw_src, ["#4a1a1a", "#7c3030", "#a85252", "#cc7e70", "#ecb4a4"], f"raw_{bird}")
    from_item("cooked_pigeon" if raw_src == "raw_pigeon" else "cooked_pheasant", COOKED, f"cooked_{bird}")
    from_item(plucked_src, ["#7c4a3c", "#a8705c", "#cf9a82", "#e8c0a8", "#f8e2d0"], f"plucked_{bird}")
from_item("raw_pike", ["#4a2a24", "#7c4a40", "#b07a66", "#d4a690", "#eccab4"], "raw_eel")
from_item("cooked_pike", COOKED, "cooked_eel")
from_item("raw_marten_pelt", ["#3a1608", "#7a3412", "#b25a1e", "#d8823a", "#f0d0a8"], "raw_fox_pelt", 1.1)
from_item("marten_fur", ["#461a08", "#8a3c12", "#c2621e", "#e08a3a", "#f6e0c0"], "fox_fur", 1.1)
from_item("marten_fur_cap", ["#461a08", "#8a3c12", "#c2621e", "#e08a3a", "#f6e0c0"], "fox_fur_hat", 1.1)
from_item("raw_bison_hide", ["#1c120c", "#33221a", "#4e3524", "#6c4c33", "#c9a596"], "raw_wisent_hide")
from_item("tortoise_shell", ["#0e120e", "#1d241b", "#2e3829", "#46523c", "#5c684e"], "pond_turtle_shell", 1.0, ("#d8c24a", 0.08), 13)
from_item("owl_feather", ["#0c0c0c", "#1c1c1c", "#c4231e", "#e0e0dc", "#ffffff"], "woodpecker_feather", 1.3)
from_item("pheasant_egg", ["#6e6a52", "#948e70", "#b4ad8c", "#cfc8a8", "#e6e0c4"], "skylark_egg", 1.0, ("#5a4a32", 0.12), 17)
from_item("pheasant_egg", ["#4a4a2a", "#6c6a3c", "#8c8a52", "#a8a46a", "#c0bc82"], "lapwing_egg", 1.0, ("#141208", 0.16), 19)
from_item("leopard_rug", ["#140c08", "#2a1a10", "#4a2f1b", "#6b4527", "#8a6038"], "wisent_rug", 1.0)
save(recolor(BLOCK / "leopard_rug.png" if (BLOCK / "leopard_rug.png").exists() else ITEM / "leopard_rug.png",
             ["#140c08", "#2a1a10", "#4a2f1b", "#6b4527", "#8a6038"]), BLOCK / "wisent_rug.png")


def sprite(rows, palette):
    """16x16 from a list of 16 strings; '.' transparent, other chars index the palette."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row.ljust(16, ".")):
            if ch != ".":
                im.putpixel((x, y), hexc(palette[ch]) + (255,))
    return im


save(sprite([
    "................",
    "................",
    "....bb..........",
    "...bccb.........",
    "...bccbb........",
    "....bcccbb......",
    "......bcccbb....",
    "........bccdb...",
    ".........bcddb..",
    "..........bcdb..",
    "...........bdb..",
    "..........bdb...",
    ".........bdb....",
    "........bdb.....",
    "........bb......",
    "................"], {"b": "#1a1a0c", "c": "#3c3a1d", "d": "#8d8442"}), ITEM / "whole_eel.png")
CRAY = [
    "................",
    "..a.......a.....",
    "...a.....a......",
    "..bb.....bb.....",
    ".bccb...bccb....",
    "..bcb...bcb.....",
    "...bcbbbcb......",
    "....bcccb.......",
    "....bcdcb.......",
    "....bcccb.......",
    ".....bcb........",
    ".....bcb........",
    ".....bcb........",
    "....bcccb.......",
    "...bc.c.cb......",
    "................"]
save(sprite(CRAY, {"a": "#3a2a1a", "b": "#2a1c10", "c": "#5c4a2c", "d": "#14100a"}), ITEM / "crayfish.png")
save(sprite(CRAY, {"a": "#7a2010", "b": "#6a1a0c", "c": "#d2401e", "d": "#14100a"}), ITEM / "cooked_crayfish.png")
save(sprite([
    "................",
    "................",
    "......bbbb......",
    ".....bccccb.....",
    "....bcdccdcb....",
    "....bccccccb....",
    "....bcdccdcb....",
    "....bccccccb....",
    "....bcdccdcb....",
    "....bccccccb....",
    "....bcccdccb....",
    ".....bccccb.....",
    "......bccb......",
    ".......bb.......",
    "................",
    "................"], {"b": "#1e0c06", "c": "#6e3518", "d": "#a05a2c"}), ITEM / "beetle_shell.png")
save(sprite([
    "................",
    "................",
    "................",
    "......bbb.......",
    ".....bcccb......",
    "....bccdccb.....",
    "...bcd.bccb.....",
    "...bc...bcb.....",
    "...bcb...bcb....",
    "....bcb..bcb....",
    ".....bcbbccb....",
    "......bcccb.....",
    ".......bbb......",
    "................",
    "................",
    "................"], {"b": "#8a7a5a", "c": "#ece2c4", "d": "#c8b48a"}), ITEM / "grub.png")
save(sprite([
    "................",
    "................",
    "................",
    ".......bbb......",
    "......bcccb.....",
    ".....bcdcccb....",
    "....bccccccb....",
    "....bcccdcb.....",
    "...bccccccb.....",
    "...bcdccccb.....",
    "....bccccb......",
    ".....bbbb.......",
    "................",
    "................",
    "................",
    "................"], {"b": "#0e0c0a", "c": "#2a2418", "d": "#5a4a2a"}), ITEM / "leech.png")
save(sprite([
    "................",
    "................",
    "................",
    "....bbbbbbb.....",
    "...bcccdcccb....",
    "..bcdcccccdcb...",
    "..bccceccccb....",
    "..bcdcccdcccb...",
    "..bcccccecccb...",
    "...bccdcccdb....",
    "...bcccccccb....",
    "....bcccdcb.....",
    ".....bbbbb......",
    "................",
    "................",
    "................"], {"b": "#6a4210", "c": "#e2a22a", "d": "#f6d266", "e": "#b8761a"}), ITEM / "wild_honey.png")

# spawn eggs: the onager's egg repainted two-tone per animal (base, spots)
EGGS = {
    "wisent": ("#3a2414", "#1a100a"), "pond_turtle": ("#1d241b", "#d8c24a"), "water_vole": ("#55361f", "#2a1a0e"),
    "stag_beetle": ("#4e2410", "#120a06"), "woodpecker": ("#1d1d22", "#c4231e"), "tawny_owl": ("#7a4421", "#2a160b"),
    "skylark": ("#86683f", "#3a2814"), "lapwing": ("#1c3326", "#f4f4ef"), "bittern": ("#8a6a3c", "#2b1a0c"),
    "sea_eagle": ("#4d3a28", "#f0eee6"), "eel": ("#2c2b14", "#b0a557"),
}
egg_src = Image.open(ITEM / "onager_spawn_egg.png").convert("RGBA")
for name, (base, spots) in EGGS.items():
    b, s = hexc(base), hexc(spots)
    im = egg_src.copy()
    px = im.load()
    for x in range(16):
        for y in range(16):
            r, g, bb, a = px[x, y]
            if not a:
                continue
            l = lum((r, g, bb))
            c = s if l < 0.35 else b
            k = 0.7 + l * 0.6
            px[x, y] = tuple(min(255, round(v * k)) for v in c) + (a,)
    save(im, ITEM / f"{name}_spawn_egg.png")

# fox fur hat worn: the marten cap's layer repainted
save(recolor(TEX / "entity/equipment/humanoid/marten_fur_cap.png", ["#461a08", "#8a3c12", "#c2621e", "#e08a3a", "#f6e0c0"], 1.1),
     TEX / "entity/equipment/humanoid/fox_fur_hat.png")


# ---------------------------------------------------------------- blocks

def noise(w, h, stops, seed, lo=0.2, hi=0.8):
    r = random.Random(seed)
    im = Image.new("RGBA", (w, h))
    for x in range(w):
        for y in range(h):
            im.putpixel((x, y), ramp(stops, lo + r.random() * (hi - lo)) + (255,))
    return im


# Grub Log: a beech log gone grey and punky, bored full of dark holes
log = recolor(BLOCK / "beech_log.png", ["#2a241c", "#4a4034", "#6a5e4c", "#8a7e68", "#a89c84"], 0.9)
r = random.Random(23)
for _ in range(9):
    x, y = r.randrange(1, 15), r.randrange(1, 15)
    log.putpixel((x, y), (18, 12, 8, 255))
    log.putpixel((x, y + 1), (40, 30, 20, 255))
save(log, BLOCK / "grub_log.png")
save(recolor(BLOCK / "beech_log_top.png", ["#3a3024", "#5a4c3a", "#7c6a52", "#9c8a6c", "#b8a888"], 0.9), BLOCK / "grub_log_top.png")

save(noise(16, 16, ["#2e4a1e", "#3f6328", "#557d34", "#6c9441"], 29), BLOCK / "bumblebee_nest.png")
save(noise(16, 16, ["#0c0a06", "#1a140c"], 31), BLOCK / "bumblebee_nest_hole.png")
save(noise(16, 16, ["#b8761a", "#e2a22a", "#f6d266"], 37), BLOCK / "bumblebee_wax.png")
den = noise(16, 16, ["#3a2a1a", "#5a4228", "#7a5a38", "#8e6c46"], 41)
for x, y in ((3, 4), (11, 9), (6, 12)):  # a feather and bits of bone in the spoil
    den.putpixel((x, y), (230, 226, 210, 255))
save(den, BLOCK / "fox_den.png")
save(noise(16, 16, ["#0a0806", "#18120c"], 43), BLOCK / "fox_den_hole.png")

# Fox Tracks: a line of small four-toed prints on transparency
tracks = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
PAW = [(1, 0), (0, 1), (2, 1), (1, 2)]
for i, (cx, cy) in enumerate([(4, 1), (10, 5), (5, 9), (11, 13)]):
    for dx, dy in PAW:
        tracks.putpixel((cx + dx, cy + dy), (52, 38, 24, 200))
    tracks.putpixel((cx + 1, cy + 1), (40, 28, 18, 220))
save(tracks, BLOCK / "fox_tracks.png")

# eggs in the nest (block texture, like bird_egg_bustard)
save(speckle(noise(16, 16, ["#b4ad8c", "#cfc8a8", "#e6e0c4"], 47), "#5a4a32", 0.15, seed=49), BLOCK / "bird_egg_skylark.png")
save(speckle(noise(16, 16, ["#6c6a3c", "#8c8a52", "#a8a46a"], 53), "#141208", 0.2, seed=59), BLOCK / "bird_egg_lapwing.png")
print("temperate textures ok")
