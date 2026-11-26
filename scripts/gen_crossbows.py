"""Dynamic crossbows (docs/superpowers/specs/2026-11-26-crossbows-design.md): textures, models, recipes, tags and lang for
the Iron and Steel Crossbow Prods, the Steel Crossbow (all six frames, as vanilla's crossbow), Bronze and Steel Bolts and
their heads, and the Clay Bolt Heads Mold the bronze heads are cast in (drawn as gen_tool_rods.py draws its mold).
The crossbows' stock and string layers: gen_tool_parts.py (RANGED). The soapstone mold twin: gen_stone_crafts.py.

- Steel Crossbow: vanilla's frames with the wooden prod reforged in blued steel, a heavier steel stirrup, and a windlass
  (drum and crank) on the stock behind the trigger. Stock and string keep vanilla's colours so gen_tool_parts splits
  them the same way.
- Prods: vanilla's prod arc and stirrup on their own, in iron or steel, with the Iron Tool Rod trigger stub.
- Recipes: the prods (2 plates either side of an Iron Tool Rod); vanilla's crossbow recipe off (the Tool Station builds it).
Chain: right after gen_iron_age.py (it draws the Iron Bolt this recolours), before gen_tool_parts.py, gen_unlocks.py and
gen_advancements.py. gen_stone_crafts.py and gen_mold_chest.py (earlier in the chain) read the committed mold files; rerun them
after a change to the mold here. Run: python scripts/gen_crossbows.py [--sheet <png>]
"""
import io
import json
import os
import random
import sys
import zipfile
from pathlib import Path

from PIL import Image

from gen_metals import CLAY_METALS  # the Clay Mold "metal" states

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / "src/main/resources/assets/mechanical"
D = ROOT / "src/main/resources/data"
TEX, ITEM, B, M = R / "textures", R / "textures/item", "mechanical:block/", "mechanical:"
jar = zipfile.ZipFile(os.environ.get("MECHANICAL_MC_JAR", ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"))


def vanilla(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/item/{name}.png"))).convert("RGBA")


def mod(name):
    return Image.open(ITEM / f"{name}.png").convert("RGBA")


def hexes(*cs):
    return [tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) for c in cs]


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def by_rank(colours, pal):
    """Each colour -> the palette shade of the same luminance rank (the drawn shading survives)."""
    s = sorted(set(colours), key=lum)
    return {c: pal[round(i * (len(pal) - 1) / max(1, len(s) - 1))] for i, c in enumerate(s)}


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def flat_item(name, parent="minecraft:item/generated"):
    w(R / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": M + "item/" + name}})
    w(R / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + "item/" + name}})


# vanilla crossbow colours (crossbow_standby): the wood, the string and stirrup greys
WOOD = set(hexes("4d2e11", "2a1a09", "6d4218", "90581f", "b5732f", "3b240d", "1f1206", "482b10", "36200b"))
GREY = set(hexes("444444", "6d6d6d", "969696", "979797"))
IRON = hexes("1e2023", "3f4247", "666a70", "8f949a", "b9bdc2")
# blued steel: darker and colder than iron, a bright polished edge
STEEL = hexes("15191f", "2b333d", "45525f", "6b7b8b", "93a3b2", "b9c5d0")
STIRRUP = hexes("1b2027", "394450", "61707f", "8d9cab")


def stock(x, y):
    """The stock's band in vanilla's crossbow frames: down the diagonal from the prod to the butt (gen_tool_parts.crossbow_zones)."""
    return x + y >= 11 and abs(x - y) <= 3


def prod_pixels(im):
    """The prod arc (wood off the stock) and the stirrup (grey in the top-left corner)."""
    px = im.load()
    arc = {(x, y) for y in range(16) for x in range(16) if px[x, y][3] and px[x, y][:3] in WOOD and not stock(x, y)}
    stirrup = {(x, y) for y in range(6) for x in range(6) if px[x, y][3] and px[x, y][:3] in GREY}
    return arc, stirrup


def reforge(im, arc_pal, stirrup_pal):
    """The prod arc and stirrup recoloured into metal, by luminance rank, the stock, string and arrow left alone."""
    out = im.copy()
    px = out.load()
    arc, stirrup = prod_pixels(im)
    for pixels, pal in ((arc, arc_pal), (stirrup, stirrup_pal)):
        m = by_rank([px[p][:3] for p in pixels], pal)
        for p in pixels:
            out.putpixel(p, m[px[p][:3]] + (255,))
    return out, arc


def heavier(img, arc):
    """The steel prod is thicker: an edge line along the arc's outside (up and left of it), lit along the top arm."""
    px = img.load()
    add = {}
    for x, y in arc:
        for dx, dy in ((0, -1), (-1, 0)):
            q = (x + dx, y + dy)
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and not px[q][3] and q not in add and not (q[0] <= 5 and q[1] <= 5):
                add[q] = STEEL[1]
    for q, c in add.items():
        img.putpixel(q, c + (255,))
    return set(add)


# The windlass at the butt: a steel drum across the stock (two rows on the anti-diagonal, lit on its upper-left row), its
# end cap standing out on the right and a crank arm up to a turned wooden knob. (x, y) -> steel shade index, or a knob shade.
WINDLASS = {(11, 14): 1, (12, 13): 3, (13, 12): 4, (14, 11): 3,           # the drum, lit row
            (12, 14): 0, (13, 13): 1, (14, 12): 1, (11, 15): 0,           # its shaded row
            (15, 10): 5, (15, 11): 2,                                     # the end cap
            (14, 9): 3, (15, 9): 1, (13, 8): 2,                           # the crank arm, rising to the knob
            (12, 7): "k", (13, 7): "k2"}                                  # the knob
KNOB = {"k": (144, 88, 31), "k2": (77, 46, 17)}


def windlass(img):
    for p, c in WINDLASS.items():
        img.putpixel(p, (KNOB[c] if isinstance(c, str) else STEEL[c]) + (255,))


FRAMES = {"standby": "crossbow_standby", "pulling_0": "crossbow_pulling_0", "pulling_1": "crossbow_pulling_1",
          "pulling_2": "crossbow_pulling_2", "arrow": "crossbow_arrow", "firework": "crossbow_firework"}
steel_frames = {}
for f, src in FRAMES.items():
    im, arc = reforge(vanilla(src), STEEL, STIRRUP)
    heavier(im, arc)
    windlass(im)
    assert not any(stock(*p) and isinstance(c, str) for p, c in WINDLASS.items()), "a wooden knob pixel would be read as stock by gen_tool_parts"
    steel_frames[f] = im
    im.save(ITEM / f"steel_{src}.png")
# models as vanilla's: the standby frame carries the display transforms the others inherit
display = json.loads(jar.read("assets/minecraft/models/item/crossbow.json"))["display"]
w(R / "models/item/steel_crossbow.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/steel_crossbow_standby"}, "display": display})
for f in ("pulling_0", "pulling_1", "pulling_2", "arrow", "firework"):
    w(R / f"models/item/steel_crossbow_{f}.json", {"parent": M + "item/steel_crossbow", "textures": {"layer0": M + f"item/steel_crossbow_{f}"}})
item_def = json.loads(jar.read("assets/minecraft/items/crossbow.json"))


def rename(node):
    if isinstance(node, dict):
        return {k: (v.replace("minecraft:item/crossbow", M + "item/steel_crossbow") if k == "model" and isinstance(v, str) else rename(v)) for k, v in node.items()}
    return [rename(v) for v in node] if isinstance(node, list) else node


steel_items = R / "items/steel_crossbow.json"
if not steel_items.exists() or "minecraft:composite" not in steel_items.read_text(encoding="utf-8"):  # gen_tool_parts layers it afterwards
    w(steel_items, rename(item_def))

# ---- prods: the arc and stirrup alone, with the Iron Tool Rod stub of the trigger where the stock would sit
TRIGGER = {(5, 5): 3, (6, 6): 2, (7, 7): 1, (6, 5): 4, (7, 6): 3, (8, 7): 2, (5, 6): 0, (6, 7): 0, (7, 8): 0, (8, 8): 0}


def prod(arc_pal, stirrup_pal, steel):
    im, arc = reforge(vanilla("crossbow_standby"), arc_pal, stirrup_pal)
    keep = arc | prod_pixels(vanilla("crossbow_standby"))[1] | (heavier(im, arc) if steel else set())
    out = Image.new("RGBA", (16, 16))
    px = im.load()
    for p in keep:
        out.putpixel(p, px[p])
    rod = hexes("181818", "444444", "7a7a7a", "a0a0a0", "c1c1c1")  # the Iron Tool Rod's own shades, a step darker
    for p, i in TRIGGER.items():
        out.putpixel(p, rod[i] + (255,))
    shifted = Image.new("RGBA", (16, 16))
    shifted.paste(out, (1, 1))  # the arc sits in the top-left of the crossbow; a step in, it centres in a slot
    return shifted


prod(IRON, hexes("3a3d42", "6b6f75", "989ca2", "c4c8cd"), False).save(ITEM / "iron_crossbow_prod.png")
prod(STEEL, STIRRUP, True).save(ITEM / "steel_crossbow_prod.png")

# ---- bolts: the Iron Bolt and its head recoloured, metal pixels only
IRON_HEAD = hexes("323439", "70757c", "aaafb6", "dee2e6")
BOLT_METAL = {"bronze": hexes("34200c", "7e5721", "c59949", "ecd08a"), "steel": hexes("161a20", "4a5866", "8fa0b0", "d6dee6")}
for metal, pal in BOLT_METAL.items():
    for src, dst in (("iron_bolt_head", f"{metal}_bolt_head"), ("iron_bolt", f"{metal}_bolt")):
        im = mod(src)
        px = im.load()
        for y in range(16):
            for x in range(16):
                if px[x, y][3] and px[x, y][:3] in IRON_HEAD:
                    im.putpixel((x, y), pal[IRON_HEAD.index(px[x, y][:3])] + (255,))
        if metal == "steel" and dst.endswith("bolt"):  # dark-dyed fletching marks the steel bolts in the quiver
            for y in range(16):
                for x in range(16):
                    r, g, b, a = px[x, y]
                    if a and r == g == b and r > 150:
                        im.putpixel((x, y), (int(r * 0.55), int(g * 0.58), int(b * 0.62), 255))
        im.save(ITEM / f"{dst}.png")

# ---- the Clay Bolt Heads Mold: four squat bodkins on a runner (also the cavity), ground apart into Bronze Bolt Heads
BOLT_SPRUE = [
    "................",
    "...##.....##....",
    "..####...####...",
    "..####...####...",
    "...##.....##....",
    "...##.....##....",
    "...#########....",
    ".......#........",
    "...##..#..##....",
    "..####.#.####...",
    "..####.#.####...",
    "...##..#..##....",
    "...##..#..##....",
    "...#########....",
    "................",
    "................",
]
BRONZE = [(92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]
sprue = Image.new("RGBA", (16, 16))
for y, row in enumerate(BOLT_SPRUE):
    for x, ch in enumerate(row):
        if ch == "#":
            up, left = BOLT_SPRUE[y - 1][x] != "#", row[x - 1] != "#"
            sprue.putpixel((x, y), ramp(BRONZE, 0.8 if up or left else 0.45 if row[x + 1] == "#" else 0.25) + (255,))
ROUGH = [(40, 30, 16), (74, 56, 30), (108, 84, 46), (134, 106, 60), (158, 128, 78)]  # gen_bronzetools_textures.py
SOLID = [(92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]


def px_of(img):
    return [(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]]


def rough(img, pal, seed):
    """Dull and mottled like a fresh casting, with burrs of flash where the mold halves met (gen_tool_rods.rough)."""
    rng = random.Random(seed)
    out = Image.new("RGBA", (16, 16))
    pts = px_of(img)
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
    img = Image.new("RGBA", (16, 16))
    for x in range(1, 15):
        for y in range(2, 14):
            img.putpixel((x, y), top.getpixel((x, y)))
        for y in (14, 15):
            r, g, b, _ = top.getpixel((x, 13))
            img.putpixel((x, y), (int(r * 0.7), int(g * 0.7), int(b * 0.7), 255))
    return img


def save(img, path, frames=None):
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if frames:
        mc.write_text(json.dumps({"animation": {"frametime": frames}}) + "\n")
    elif mc.exists():
        mc.unlink()


save(mold_top(sprue, Image.open(TEX / "block/kiln_clay_fired.png").convert("RGBA"), FIRED, 710), TEX / "block/clay_bolt_heads_mold_top.png")
save(item_icon(mold_top(sprue, None, RAW, 720)), ITEM / "unfired_clay_bolt_heads_mold.png")
save(rough(sprue, ROUGH, 730), ITEM / "rough_cast_bronze_bolt_heads.png")
solid = Image.new("RGBA", (16, 16))
for p in px_of(sprue):
    t = 0.5 + (0.25 if not cavity(sprue, p[0] - 1, p[1] - 1) else 0) + random.Random(p[0] * 31 + p[1]).uniform(-0.1, 0.1)
    solid.putpixel(p, ramp(SOLID, t) + (255,))
save(solid, TEX / "block/clay_bolt_heads_mold_bronze_solid.png")
molten = Image.new("RGBA", (16, 64))
for f in range(4):
    rng = random.Random(f * 13 + 740)
    for x, y in px_of(sprue):
        molten.putpixel((x, y + f * 16), ramp(MOLTEN, 0.55 + rng.uniform(-0.15, 0.35)) + (255,))
save(molten, TEX / "block/clay_bolt_heads_mold_bronze_molten.png", frames=8)

base = {"particle": B + "kiln_clay_fired", "side": B + "kiln_clay_fired", "top": B + "clay_bolt_heads_mold_top"}
slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
    "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
    **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in ("north", "south", "east", "west")}}}
w(R / "models/block/clay_bolt_heads_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
for fill in ("molten", "solid"):
    top = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
           "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
    w(R / f"models/block/clay_bolt_heads_mold_{fill}_bronze.json", {"parent": "minecraft:block/block",
        "textures": {**base, "metal": B + f"clay_bolt_heads_mold_bronze_{fill}"}, "elements": [slab, top]})
# bronze only (copper has no bolts); every metal state still needs a model, copper and the rest show the bronze fill
w(R / "blockstates/clay_bolt_heads_mold.json", {"variants": {
    f"fill={fill},metal={m},uses={u}": {"model": B + "clay_bolt_heads_mold" + ("" if fill == "empty" else f"_{fill}_bronze")}
    for fill in ("empty", "molten", "solid") for m in CLAY_METALS for u in range(5)}})
w(R / "items/clay_bolt_heads_mold.json", {"model": {"type": "minecraft:model", "model": B + "clay_bolt_heads_mold"}})
w(D / "mechanical/loot_table/blocks/clay_bolt_heads_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": "mechanical:clay_bolt_heads_mold",
                 "modifier": [{"type": "minecraft:copy_state", "block": "mechanical:clay_bolt_heads_mold", "properties": ["uses"]}]}]}],
    "random_sequence": "mechanical:blocks/clay_bolt_heads_mold"})
for name in ("unfired_clay_bolt_heads_mold", "rough_cast_bronze_bolt_heads", "bronze_bolt_head", "steel_bolt_head", "bronze_bolt", "steel_bolt",
             "iron_crossbow_prod", "steel_crossbow_prod"):
    flat_item(name)

# ---- recipes and tags
RECIPE = D / "mechanical/recipe"
for metal in ("iron", "steel"):
    w(RECIPE / f"{metal}_crossbow_prod.json", {"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": ["PRP"],
        "key": {"P": M + f"{metal}_plate", "R": M + "iron_tool_rod"}, "result": {"id": M + f"{metal}_crossbow_prod", "count": 1}})
# vanilla's crossbow: built at the Tool Station from the Iron Crossbow Prod now (ToolStationRecipes)
w(D / "minecraft/recipe/crossbow.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})
w(D / "minecraft/advancement/recipes/combat/crossbow.json", {"neoforge:conditions": [{"type": "neoforge:never"}]})  # it names the recipe
w(D / "mechanical/tags/item/bolts.json", {"values": [M + "bronze_bolt", M + "iron_bolt", M + "steel_bolt"]})


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"replace": False, "values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


tag_add(D / "minecraft/tags/item/enchantable/crossbow.json", M + "steel_crossbow")
tag_add(D / "minecraft/tags/item/enchantable/durability.json", M + "steel_crossbow")

# ---- lang: only our keys, each block inserted after an anchor line (never re-escaping the file)
LANG = {
    '  "item.mechanical.iron_bolt": "Iron Bolt",': {
        "item.mechanical.bronze_bolt_head": "Bronze Bolt Head", "item.mechanical.bronze_bolt": "Bronze Bolt",
        "item.mechanical.steel_bolt_head": "Steel Bolt Head", "item.mechanical.steel_bolt": "Steel Bolt",
        "item.mechanical.iron_crossbow_prod": "Iron Crossbow Prod", "item.mechanical.steel_crossbow_prod": "Steel Crossbow Prod",
        "item.mechanical.steel_crossbow": "Steel Crossbow",
        "item.mechanical.unfired_clay_bolt_heads_mold": "Unfired Clay Bolt Heads Mold", "block.mechanical.clay_bolt_heads_mold": "Clay Bolt Heads Mold",
        "block.mechanical.soapstone_bolt_heads_mold": "Soapstone Bolt Heads Mold",
        "item.mechanical.rough_cast_bronze_bolt_heads": "Rough Cast Bronze Bolt Heads"},
    '  "tooltip.mechanical.fishing_rod_parts": "Rod: %1$s  Line: %2$s",': {
        "tooltip.mechanical.crossbow_parts": "Stock: %1$s  String: %2$s",
        "gui.mechanical.tool_station.prod": "Prod", "gui.mechanical.tool_station.stock": "Stock",
        "gui.mechanical.tool_station.wrong_stock": "Not a stock for this crossbow",
        "gui.mechanical.tool_station.would_do.stock": "Stocks that would do:",
        "gui.mechanical.tool_station.lay_parts.crossbow": "Lay in a prod, a string and a stock to see what each part does."},
    '  "tooltip.mechanical.crossbow.bolts_only": "Loads Iron Bolts only",': {
        "tooltip.mechanical.crossbow.all_bolts": "Loads bolts only: Bronze, Iron and Steel"},
}
REPLACE = {'  "tooltip.mechanical.crossbow.bolts_only": "Loads Iron Bolts only",': '  "tooltip.mechanical.crossbow.bolts_only": "Loads Bronze and Iron Bolts only",',
           '  "gui.mechanical.tool_station.type.bow": "Bow",': '  "gui.mechanical.tool_station.type.bow": "Bows & Crossbows",'}  # one ranged pocket
lang = R / "lang/en_us.json"
text = lang.read_bytes().decode("utf-8")
nl = "\r\n" if "\r\n" in text else "\n"
for anchor, keys in LANG.items():
    keys = {k: v for k, v in keys.items() if f'"{k}"' not in text}
    if not keys:
        continue
    at = text.index(anchor + nl) + len(anchor + nl)
    text = text[:at] + "".join(f'  "{k}": "{v}",' + nl for k, v in keys.items()) + text[at:]
for old, new in REPLACE.items():
    text = text.replace(old, new)
lang.write_bytes(text.encode("utf-8"))
json.loads(text)  # still valid

if "--sheet" in sys.argv:  # 8x look at everything drawn here, on the slot grey
    names = [f"steel_{s}" for s in FRAMES.values()] + ["iron_crossbow_prod", "steel_crossbow_prod", "bronze_bolt_head", "steel_bolt_head",
             "bronze_bolt", "iron_bolt", "steel_bolt", "rough_cast_bronze_bolt_heads", "unfired_clay_bolt_heads_mold"]
    S = 8
    sheet = Image.new("RGBA", (7 * (16 * S + 8), 3 * (16 * S + 8)), (60, 60, 68, 255))
    for i, n in enumerate(names + ["~vanilla"]):
        im = vanilla("crossbow_standby") if n == "~vanilla" else mod(n)
        bg = Image.new("RGBA", (16, 16), (139, 139, 139, 255))
        bg.alpha_composite(im)
        sheet.paste(bg.resize((16 * S, 16 * S), Image.NEAREST), ((i % 7) * (16 * S + 8) + 4, (i // 7) * (16 * S + 8) + 4))
    sheet.save(sys.argv[sys.argv.index("--sheet") + 1])
print("crossbows: done")
