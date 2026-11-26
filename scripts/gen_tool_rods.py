"""Tool rod sprites for the dynamic tools: the Iron Tool Rod's drawing (same diagonal, edge / two highlights / shadow)
recoloured per material, plus item definitions, models and lang.
Copper and Bronze Tool Rods are cast (Casting.CLAY_TOOL_ROD_MOLD, 2 units) and ground on the Whetstone like the copper
heads: this also writes the Clay Tool Rod Mold (block models, blockstate, item, loot), the unfired mold and the rough
casts, drawn as gen_copper_textures.py / gen_bronzetools_textures.py draw theirs. The soapstone twin: gen_stone_crafts.py.
Run: python scripts/gen_tool_rods.py"""
import json
import random
from pathlib import Path
from PIL import Image

from gen_metals import CLAY_METALS  # the Clay Mold "metal" states

R = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"
IRON = {"444444": 0, "d8d8d8": 1, "c1c1c1": 2, "181818": 3}  # edge, highlight, mid, shadow
# edge, highlight, mid, shadow
PALETTES = {
    "bone": ("a69a7c", "f4f1e2", "d9d2b6", "5e5440"),
    "copper": ("9c4e31", "fc9982", "d66d48", "421c11"),
    "bronze": ("7e5721", "e2bf71", "c59949", "34200c"),
    "steel": ("3a3f46", "9aa3ae", "747d89", "14171b"),
}
NAMES = {"bone": "Bone", "copper": "Copper", "bronze": "Bronze", "steel": "Steel"}

src = Image.open(R / "textures/item/iron_tool_rod.png").convert("RGBA")
for mat, pal in PALETTES.items():
    img = Image.new("RGBA", src.size)
    for x in range(16):
        for y in range(16):
            r, g, b, a = src.getpixel((x, y))
            if a:
                img.putpixel((x, y), tuple(int(pal[IRON["%02x%02x%02x" % (r, g, b)]][i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    if mat == "bone":  # a knuckle at each end where the bone was cut
        for x, y, c in ((13, 2, 1), (14, 3, 1), (2, 13, 2), (3, 13, 2), (2, 14, 3), (3, 14, 3)):
            img.putpixel((x, y), tuple(int(pal[c][i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    name = f"{mat}_tool_rod"
    img.save(R / f"textures/item/{name}.png")
    (R / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"mechanical:item/{name}"}}, indent=2) + "\n")
    (R / f"models/item/{name}.json").write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{name}"}}, indent=2) + "\n")

# recipes: bone from a knife over a bone (the knife wears as in every knife recipe). Steel is forged on the anvil
# (Smithing.Pattern.TOOL_ROD) like the iron one; copper and bronze are cast (below).
D = R.parent.parent / "data/mechanical/recipe"
def shaped(name, pattern, key, count=1):
    (D / f"{name}.json").write_text(json.dumps({"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": key,
                                                "result": {"id": f"mechanical:{name}", "count": count}}, indent=2) + "\n")
# shaped, knife above two bones: one Bone + knife in any layout is already the Bone Needle
shaped("bone_tool_rod", ["K", "B", "B"], {"K": "#mechanical:knives", "B": "minecraft:bone"})
for old in ("copper_tool_rod", "bronze_tool_rod"):  # the 2-ingot recipes these rods had before they were cast
    (D / f"{old}.json").unlink(missing_ok=True)

# ---- the cast rods: Clay Tool Rod Mold (copper and bronze), unfired mold, rough casts ----
TEX, B = R / "textures", "mechanical:block/"
ROUGH = {"copper": [(46, 24, 16), (84, 46, 30), (124, 72, 48), (150, 92, 62), (176, 116, 82)],    # gen_copper_textures.py
         "bronze": [(40, 30, 16), (74, 56, 30), (108, 84, 46), (134, 106, 60), (158, 128, 78)]}   # gen_bronzetools_textures.py
SOLID = {"copper": [(95, 44, 28), (214, 109, 72), (227, 130, 108), (252, 153, 130)],
         "bronze": [(92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]}
MOLTEN = {"copper": [(120, 20, 4), (200, 60, 8), (246, 128, 20), (255, 196, 64), (255, 244, 190)],
          "bronze": [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]}
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def px(img):
    return [(x, y) for y in range(img.height) for x in range(img.width) if img.getpixel((x, y))[3]]


def lum(c):
    return (c[0] * 3 + c[1] * 6 + c[2]) / 10


def save(img, path, frames=None):
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if frames:
        mc.write_text(json.dumps({"animation": {"frametime": frames}}) + "\n")
    elif mc.exists():
        mc.unlink()


def rough(img, pal, seed):
    """Dull and mottled like a fresh casting, with burrs of flash where the mold halves met."""
    rng = random.Random(seed)
    out = Image.new("RGBA", (16, 16))
    pts = px(img)
    lo, hi = min(lum(img.getpixel(p)) for p in pts), max(lum(img.getpixel(p)) for p in pts)
    for p in pts:
        out.putpixel(p, ramp(pal, (lum(img.getpixel(p)) - lo) / max(1, hi - lo) * 0.85 + rng.uniform(-0.18, 0.12)) + (255,))
    for x in range(16):
        for y in range(16):
            if img.getpixel((x, y))[3]:
                continue
            n = sum(1 for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if 0 <= x + dx < 16 and 0 <= y + dy < 16 and img.getpixel((x + dx, y + dy))[3])
            if n and rng.random() < 0.22:
                out.putpixel((x, y), pal[1] + (255,))
    return out


def cavity(m, x, y):
    return 0 <= x < 16 and 0 <= y < 16 and m.getpixel((x, y))[3] > 0


def mold_top(m, base, pal, seed):
    """Clay slab top with the cavity sunk in: dark floor, shadow on the upper-left lip, light on the lower-right."""
    rng = random.Random(seed)
    img = base.copy() if base else Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            if base is None:
                img.putpixel((x, y), ramp(pal, 0.55 + rng.uniform(-0.12, 0.12)) + (255,))
            if cavity(m, x, y):
                img.putpixel((x, y), ramp(pal, (0.12 if not cavity(m, x - 1, y - 1) else 0.28) + rng.uniform(0, 0.08)) + (255,))
            elif cavity(m, x - 1, y - 1) or cavity(m, x - 1, y) or cavity(m, x, y - 1):
                img.putpixel((x, y), ramp(pal, 0.95) + (255,))
    return img


def item_icon(top):
    """Unfired mold item: the slab seen from above with a darker front edge."""
    img = Image.new("RGBA", (16, 16))
    for x in range(1, 15):
        for y in range(2, 14):
            img.putpixel((x, y), top.getpixel((x, y)))
        for y in (14, 15):
            r, g, b, _ = top.getpixel((x, 13))
            img.putpixel((x, y), (int(r * 0.7), int(g * 0.7), int(b * 0.7), 255))
    return img


rod = Image.open(TEX / "item/copper_tool_rod.png").convert("RGBA")
save(mold_top(rod, Image.open(TEX / "block/kiln_clay_fired.png").convert("RGBA"), FIRED, 610), TEX / "block/clay_tool_rod_mold_top.png")
save(item_icon(mold_top(rod, None, RAW, 620)), TEX / "item/unfired_clay_tool_rod_mold.png")
for i, metal in enumerate(("copper", "bronze")):
    save(rough(Image.open(TEX / f"item/{metal}_tool_rod.png").convert("RGBA"), ROUGH[metal], 630 + i), TEX / f"item/rough_cast_{metal}_tool_rod.png")
    solid = Image.new("RGBA", (16, 16))
    for p in px(rod):
        t = 0.5 + (0.25 if not cavity(rod, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
        solid.putpixel(p, ramp(SOLID[metal], t) + (255,))
    save(solid, TEX / f"block/clay_tool_rod_mold_{metal}_solid.png")
    molten = Image.new("RGBA", (16, 64))
    for f in range(4):
        rng = random.Random(f * 13 + i + 640)
        for x, y in px(rod):
            molten.putpixel((x, y + f * 16), ramp(MOLTEN[metal], 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
    save(molten, TEX / f"block/clay_tool_rod_mold_{metal}_molten.png", frames=8)


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


# the mold block, as gen_copper.py writes the copper/bronze molds
base = {"particle": B + "kiln_clay_fired", "side": B + "kiln_clay_fired", "top": B + "clay_tool_rod_mold_top"}
slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
    "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
    **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in ("north", "south", "east", "west")}}}
w(R / "models/block/clay_tool_rod_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
for look in ("copper", "bronze"):
    for fill in ("molten", "solid"):
        top = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
               "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
        w(R / f"models/block/clay_tool_rod_mold_{fill}_{look}.json", {"parent": "minecraft:block/block",
            "textures": {**base, "metal": B + f"clay_tool_rod_mold_{look}_{fill}"}, "elements": [slab, top]})
w(R / "blockstates/clay_tool_rod_mold.json", {"variants": {  # poor bronze looks like bronze
    f"fill={fill},metal={m},uses={u}": {"model": B + "clay_tool_rod_mold" + ("" if fill == "empty" else f"_{fill}_{'copper' if m == 'copper' else 'bronze'}")}
    for fill in ("empty", "molten", "solid") for m in CLAY_METALS for u in range(5)}})
w(R / "items/clay_tool_rod_mold.json", {"model": {"type": "minecraft:model", "model": B + "clay_tool_rod_mold"}})
# picked up, a mold keeps how many casts it has made (cracks at 5)
w(R.parent.parent / "data/mechanical/loot_table/blocks/clay_tool_rod_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": "mechanical:clay_tool_rod_mold",
                 "modifier": [{"type": "minecraft:copy_state", "block": "mechanical:clay_tool_rod_mold", "properties": ["uses"]}]}]}],
    "random_sequence": "mechanical:blocks/clay_tool_rod_mold"})
for name in ("unfired_clay_tool_rod_mold", "rough_cast_copper_tool_rod", "rough_cast_bronze_tool_rod"):
    w(R / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{name}"}})
    w(R / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{name}"}})

# lang: only our keys, inserted after the Iron Tool Rod's line
lang = R / "lang/en_us.json"
text = lang.read_bytes().decode("utf-8")
nl = "\r\n" if "\r\n" in text else "\n"
anchor = '  "item.mechanical.iron_tool_rod": "Iron Tool Rod",' + nl
if '"item.mechanical.bone_tool_rod"' not in text:
    text = text.replace(anchor, anchor + "".join(f'  "item.mechanical.{m}_tool_rod": "{n} Tool Rod",' + nl for m, n in NAMES.items()), 1)
CAST = {"item.mechanical.unfired_clay_tool_rod_mold": "Unfired Clay Tool Rod Mold", "block.mechanical.clay_tool_rod_mold": "Clay Tool Rod Mold",
        "block.mechanical.soapstone_tool_rod_mold": "Soapstone Tool Rod Mold",
        "item.mechanical.rough_cast_copper_tool_rod": "Rough Cast Copper Tool Rod", "item.mechanical.rough_cast_bronze_tool_rod": "Rough Cast Bronze Tool Rod"}
anchor = '  "item.mechanical.steel_tool_rod": "Steel Tool Rod",' + nl
if '"item.mechanical.rough_cast_copper_tool_rod"' not in text:
    text = text.replace(anchor, anchor + "".join(f'  "{k}": "{v}",' + nl for k, v in CAST.items()), 1)
lang.write_bytes(text.encode("utf-8"))
