"""Dynamic tools look like their parts (docs/superpowers/specs/2026-11-26-dynamic-tools-design.md, "Utseende").
Run: python scripts/gen_tool_parts.py [--sheets <dir>]   (in the chain after gen_hold_displays, before gen_block_items)

Every dynamic tool's own 16x16 sprite (vanilla's from the client jar) is split into
- the head: everything that is not haft or binding;
- the haft: the wood palette (vanilla's stick browns, or the tool's own grip / handle colours in TOOLS), plus outline
  pixels that only touch haft (a hoe's dark haft edge);
- the binding zone: the sprite's own wrap pixels (the twine-coloured lashing of the stone tools, sickles, hammers), or
  where it has none, a 2-4 pixel wrap where the main haft enters the head (hand masks in TOOLS where that misreads).
One haft layer per rod and one zone layer per binding are recoloured from those pixels: each original shade, by its
luminance rank, takes the shade of the same rank in the part's ramp (taken from the part's own item texture), so the
drawn shading survives. items/<tool>.json becomes a minecraft:composite of head + select on custom_model_data string 0
(the rod's id path) + select on string 1 (the binding's), written by ToolParts.apply. Fallbacks are the original pixels
(asserted), so a tool never built at the Tool Station looks exactly as before. A sprite without a drawn wrap shows its
binding, the default one too, once the station names it; its fallback zone follows the rod (a select on the rod inside
the binding's fallback), so a bone rod never shows a stray wooden pixel at the head.
--sheets writes 8x sheets per tool (classification, every rod, every binding) to inspect.
"""
import json
import os
import sys
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
JAR = Path(os.environ.get("MECHANICAL_MC_JAR", ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"))
if not JAR.exists():
    sys.exit(f"no Minecraft jar at {JAR}: build once (./gradlew compileJava) or set MECHANICAL_MC_JAR to the merged jar")
ASSETS = ROOT / "src/main/resources/assets"
ITEM_TEX = ASSETS / "mechanical/textures/item"
jar = zipfile.ZipFile(JAR)


def hexset(*cs):
    return {tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) for c in cs}


# the saws' ring handle: the binding is the joint where the blade's tang meets the ring
SAW = {"haft": hexset("493019", "90663a", "8b6237", "865e34", "7b552f", "76512c"), "zone_px": [(3, 10), (3, 11), (4, 11)]}
TONG_RINGS = [(4, 10), (6, 12)]  # the tongs' binding: a ring atop each leather grip
HOE_END = {"haft_px": [(2, 14), (3, 14)]}  # vanilla's hoe haft ends in two outline-coloured pixels
STICK = hexset("281e0b", "493615", "684e1e", "896727")
TWINE_WRAP = hexset("836f61", "9b8474", "a58c7c", "ac917f", "b79c89")
# the mod's leather (gen_bronzetools_textures.LEATHER): vanilla's leather item is orange hide, not a worn wrap
LEATHER = [(56, 32, 18), (84, 50, 28), (110, 70, 42), (140, 96, 60)]

# the Tool Rod is planed: redder and darker than the rough stick, with a light end-grain cap where it pokes through a head
PLANED, END_GRAIN = [(46, 28, 14), (78, 48, 22), (112, 72, 34), (146, 100, 48)], (184, 136, 76)

# part id path -> (tier, ramp source); a ramp is a list of colours dark to light
RODS = {"stick": (0, "minecraft:stick"), "tool_rod": (0, PLANED), "leather": (0, LEATHER),
        "bone_tool_rod": (1, "bone_tool_rod"), "copper_tool_rod": (1, "copper_tool_rod"),
        "bronze_tool_rod": (2, "bronze_tool_rod"), "iron_tool_rod": (3, "iron_tool_rod"), "steel_tool_rod": (4, "steel_tool_rod")}
BINDINGS = {"twine": 0, "cordage": 0, "hide_glue": 0, "leather": 0, "sinew": 0, "bronze_plate": 2, "iron_plate": 3,
            "steel_plate": 4, "iron_tool_rod": 3}  # part id path -> tier

# Binding colours (end line, shade, light, accent), set apart from the wood: every light is above the stick's lightest.
BIND = {"twine": [(96, 90, 78), (150, 144, 128), (225, 218, 196)],
        "cordage": [(96, 72, 44), (140, 108, 68), (206, 174, 122)],
        "leather": [(66, 34, 14), (112, 58, 26), (156, 86, 42), (228, 198, 142)],
        "sinew": [(126, 100, 56), (206, 180, 120), (248, 240, 212)],
        "hide_glue": [(108, 60, 12), (172, 106, 24), (226, 160, 48), (252, 214, 118)],
        "bronze_plate": [(72, 46, 16), (190, 140, 62), (242, 206, 126)],
        "iron_plate": [(46, 46, 52), (166, 168, 176), (226, 227, 231)],
        "steel_plate": [(28, 34, 44), (128, 144, 168), (202, 212, 226)],
        "iron_tool_rod": [(40, 40, 44), (148, 148, 154), (212, 212, 216)]}
WOOD = {"stick", "tool_rod", "leather"}

# (namespace, name, default binding, default rod, overrides) — the dynamic recipes of ToolStationRecipes.ALL.
# Overrides: haft = the haft colours instead of the stick's; wrap = extra wrap colours; haft_px / head_px / zone_px = hand mask
# pixels; wrap_size = length of a drawn-in wrap between its two end lines where the sprite has none; art_rod = the rod the sprite shows when it is
# not the default rod (a wooden haft drawn for an Iron Tool Rod, wooden sword grips drawn for Leather).
TOOLS = [
    ("mechanical", "crude_stone_pickaxe", "twine", "stick", {}),
    ("minecraft", "stone_pickaxe", "cordage", "tool_rod", {}),
    ("mechanical", "crude_stone_axe", "twine", "stick", {}),
    ("minecraft", "stone_axe", "cordage", "tool_rod", {}),
    ("mechanical", "flint_spear", "cordage", "tool_rod", {"spear": True}),
    ("mechanical", "tusk_dagger", "cordage", "leather", {"haft": hexset("5a3a22", "7c5434", "9a6b43"), "wrap": hexset("765636", "b5977d", "a68870")}),
    ("mechanical", "flint_sickle", "twine", "stick", {}),
    ("minecraft", "copper_pickaxe", "cordage", "tool_rod", {}),
    ("minecraft", "copper_axe", "cordage", "tool_rod", {}),
    ("minecraft", "copper_shovel", "cordage", "tool_rod", {}),
    ("minecraft", "copper_hoe", "cordage", "tool_rod", HOE_END),
    ("mechanical", "copper_knife", "twine", "stick", {"wrap_size": 2}),
    ("mechanical", "bronze_pickaxe", "cordage", "tool_rod", {}),
    ("mechanical", "bronze_axe", "cordage", "tool_rod", {}),
    ("mechanical", "bronze_shovel", "cordage", "tool_rod", {}),
    ("mechanical", "bronze_hoe", "cordage", "tool_rod", HOE_END),
    ("mechanical", "bronze_knife", "twine", "stick", {"wrap_size": 2}),
    ("mechanical", "bronze_saw", "twine", "stick", SAW),
    ("mechanical", "bronze_spear", "cordage", "tool_rod", {"spear": True}),
    ("mechanical", "bronze_sword", "cordage", "leather", {"haft": hexset("54321c", "6d4529", "8c603c", "382012"), "wrap_size": 2}),
    ("mechanical", "bronze_sickle", "twine", "stick", {}),
    ("mechanical", "bronze_tongs", "twine", "leather", {"haft": hexset("6c4529", "55321c"), "zone_px": TONG_RINGS}),
    ("mechanical", "bronze_chisel", "twine", "stick", {"haft": hexset("493019", "634323", "744f1e", "896136"), "wrap": hexset("ad8036")}),
    ("mechanical", "bronze_smithing_hammer", "cordage", "tool_rod", {}),
    ("mechanical", "bronze_quarry_hammer", "bronze_plate", "tool_rod", {}),
    ("mechanical", "bronze_ard", "bronze_plate", "tool_rod", {}),
    ("minecraft", "iron_pickaxe", "leather", "tool_rod", {}),
    ("minecraft", "iron_axe", "leather", "tool_rod", {}),
    ("minecraft", "iron_shovel", "leather", "tool_rod", {}),
    ("minecraft", "iron_hoe", "leather", "tool_rod", HOE_END),
    ("mechanical", "iron_knife", "leather", "stick", {"wrap_size": 2}),
    ("minecraft", "iron_sword", "cordage", "leather", {"wrap_size": 2, "art_rod": "stick"}),
    ("minecraft", "iron_spear", "cordage", "tool_rod", {"spear": True}),
    ("mechanical", "iron_smithing_hammer", "leather", "tool_rod", {}),
    ("mechanical", "steel_pickaxe", "iron_tool_rod", "tool_rod", {}),
    ("mechanical", "steel_axe", "iron_tool_rod", "tool_rod", {}),
    ("mechanical", "steel_shovel", "iron_tool_rod", "tool_rod", {}),
    ("mechanical", "steel_hoe", "iron_tool_rod", "tool_rod", HOE_END),
    ("mechanical", "steel_knife", "leather", "stick", {"wrap_size": 2}),
    ("mechanical", "steel_sword", "iron_tool_rod", "leather", {"wrap_size": 2, "art_rod": "stick"}),
    ("mechanical", "steel_spear", "iron_tool_rod", "tool_rod", {"spear": True}),
    ("mechanical", "steel_quarry_hammer", "steel_plate", "iron_tool_rod", {"haft_add": hexset("34200c"), "art_rod": "tool_rod"}),
    ("mechanical", "iron_saw", "leather", "stick", SAW),
    ("mechanical", "steel_saw", "leather", "stick", SAW),
    ("mechanical", "iron_sickle", "leather", "stick", {}),
    ("mechanical", "steel_sickle", "leather", "stick", {}),
    ("mechanical", "iron_chisel", "leather", "stick", {"haft": hexset("493019", "634323", "744f1e", "896136", "34200c"), "wrap": hexset("ad8036")}),
    ("mechanical", "steel_chisel", "leather", "stick", {"haft": hexset("493019", "634323", "744f1e", "896136", "34200c"), "wrap": hexset("ad8036")}),
    ("mechanical", "iron_tongs", "iron_tool_rod", "leather", {"haft": hexset("4c4e55", "393b41"), "zone_px": TONG_RINGS, "art_rod": "iron_tool_rod"}),
    ("mechanical", "iron_quarry_hammer", "iron_plate", "tool_rod", {"haft_add": hexset("34200c")}),
    ("mechanical", "steel_smithing_hammer", "iron_tool_rod", "tool_rod", {}),
]


def tier(name):
    return next((t for p, t in (("steel", 4), ("iron", 3), ("bronze", 2), ("copper", 1)) if name.startswith(p)), 0)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def texture(ref):
    if ref.startswith("minecraft:"):
        return Image.open(jar.open(f"assets/minecraft/textures/item/{ref[10:]}.png")).convert("RGBA")
    return Image.open(ITEM_TEX / f"{ref}.png").convert("RGBA")


def ramp(src):
    """The part's shades dark to light: its item texture's colours, or 5 luminance quantiles of a busy texture."""
    if isinstance(src, list):
        return src
    im = texture(src)
    px = [im.getpixel((x, y))[:3] for y in range(im.height) for x in range(im.width) if im.getpixel((x, y))[3] > 0]
    uniq = sorted(set(px), key=lum)
    if len(uniq) <= 6:
        return uniq
    px.sort(key=lum)
    return [px[int(q * (len(px) - 1))] for q in (0.03, 0.25, 0.5, 0.75, 0.97)]


def recolour(sources, target):
    """Original shade -> the target shade of the same luminance rank."""
    s = sorted(sources, key=lum)
    if len(s) == 1:
        return {s[0]: target[len(target) // 2]}
    return {c: target[round(i * (len(target) - 1) / (len(s) - 1))] for i, c in enumerate(s)}


def recolour_smooth(sources, target, span):
    """Original shade -> the target ramp read at the shade's place in {@code span} (the whole sprite's wood, darkest to lightest), so a
    big stock keeps its grain and its depth, where rank matching would fold its few browns onto a short metal ramp, lightest on top."""
    lo, hi = min(map(lum, span)), max(map(lum, span))
    out = {}
    for c in sources:
        t = (lum(c) - lo) / max(1.0, hi - lo) * (len(target) - 1)
        i = min(int(t), len(target) - 2)
        out[c] = tuple(round(target[i][k] * (1 - (t - i)) + target[i + 1][k] * (t - i)) for k in range(3))
    return out


def split(im, opts):
    px = im.load()
    opaque = {(x, y) for y in range(16) for x in range(16) if px[x, y][3] > 0}
    for p in opaque:
        assert px[p][3] == 255, f"translucent pixel {p}"
    haft_colours = opts.get("haft", STICK) | opts.get("haft_add", set())
    wrap_colours = TWINE_WRAP | opts.get("wrap", set())
    haft = {p for p in opaque if px[p][:3] in haft_colours}
    wrap = {p for p in opaque if px[p][:3] in wrap_colours} - haft
    nb4 = lambda p: [(p[0] + dx, p[1] + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if (p[0] + dx, p[1] + dy) in opaque]
    grown = True
    while grown:  # outline pixels whose every neighbour is haft are the haft's own edge
        grown = False
        for p in opaque - haft - wrap:
            n = nb4(p)
            if n and all(q in haft for q in n):
                haft.add(p)
                grown = True
    haft |= set(opts.get("haft_px", ()))
    haft -= set(opts.get("head_px", ()))
    head = opaque - haft - wrap
    if wrap:
        zone = wrap
    elif "zone_px" in opts:
        zone = set(opts["zone_px"])
        assert zone <= haft
    else:
        zone = junction(haft, head, nb4, opts.get("wrap_size", 3))
    return opaque, head, haft - zone, zone, bool(wrap)


def nb8(p, within):
    return [(p[0] + dx, p[1] + dy) for dx in (-1, 0, 1) for dy in (-1, 0, 1) if (dx or dy) and (p[0] + dx, p[1] + dy) in within]


def groups(pixels):
    left, out = set(pixels), []
    while left:
        todo, g = [left.pop()], set()
        while todo:
            p = todo.pop()
            g.add(p)
            for q in nb8(p, left):
                left.discard(q)
                todo.append(q)
        out.append(g)
    return out


def junction(haft, head, nb4, size=3):
    """The wrap where the haft enters the head: on the main haft (not a stub poking out above the head), from the first
    pixels touching the head's main part (not a sword's pommel) counted from the handle's end, a band {size} + 2 end
    lines long down the haft axis (hafts run bottom-left to top-right, so along the axis x - y falls)."""
    main, body = max(groups(haft), key=len), max(groups(head), key=len)
    cx, cy = sum(p[0] for p in body) / len(body), sum(p[1] for p in body) / len(body)
    end = max(main, key=lambda p: (p[0] - cx) ** 2 + (p[1] - cy) ** 2)
    steps, todo = {end: 0}, [end]
    for p in todo:  # breadth first: steps along the haft from its end
        for q in nb8(p, main):
            if q not in steps:
                steps[q] = steps[p] + 1
                todo.append(q)
    touching = groups({p for p in main if any(q in body for q in nb4(p))})
    top = max(p[0] - p[1] for p in min(touching, key=lambda g: min(steps[p] for p in g)))
    return {p for p in main if top - size - 1 <= p[0] - p[1] <= top}


def paint_wrap(img, zone, b, ends=True):
    """A wrap where the sprite drew none, painted in the binding's own colours: a dark end line at both ends of the band,
    between them each binding's pattern, offset per row so it survives at 16 px. side 0 is the haft's upper-left edge."""
    end, shade, light, *accent = BIND[b]
    axis = [p[0] - p[1] for p in zone]
    top, low, edge = max(axis), min(axis), min(p[0] + p[1] for p in zone)
    for p in zone:
        k, side = top - (p[0] - p[1]), int(p[0] + p[1] > edge)
        if ends and (k == 0 or p[0] - p[1] == low):
            c = end
        elif b == "twine":
            c = light if (k + side) % 2 == 0 else shade
        elif b == "cordage":  # two turns on, one off, on the diagonal
            c = shade if (k + side) % 3 == 2 else light
        elif b == "sinew":  # a thin pale spiral on a darker cream
            c = light if (k + side) % 3 == 0 else shade
        elif b == "leather":
            c = accent[0] if (k == (top - low) // 2 and side == 0) else light if side == 0 else shade
        elif b == "hide_glue":
            c = accent[0] if (k == 1 and side == 0) else light if side == 0 else shade
        else:  # metal: lit edge and shaded edge, a cylinder
            c = light if side == 0 else shade
        img.putpixel(p, (*c, 255))


def layer(im, pixels, colours=None):
    out = Image.new("RGBA", (16, 16))
    src = im.load()
    for p in pixels:
        c = src[p]
        out.putpixel(p, (*colours[c[:3]], 255) if colours else c)
    return out


def rod_layer(im, pixels, colours, rod, head, rp):
    """The haft recoloured; a rod that is not wood gets a dark line where it meets the head, the planed Tool Rod an
    end-grain cap on a tip poking through the head."""
    out = layer(im, pixels, colours)
    touch = lambda p: any((p[0] + dx, p[1] + dy) in head for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
    if rod not in WOOD:
        for p in pixels:
            if touch(p):
                out.putpixel(p, (*rp[0], 255))
    if rod == "tool_rod":
        for g in groups(pixels):
            tip = max(g, key=lambda p: (p[0] - p[1], -p[1]))
            if len(g) <= 3 and touch(tip):
                out.putpixel(tip, (*END_GRAIN, 255))
    return out


def write_png(path, im):
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)


def write_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def template_model(ns, name, spear):
    ref = f"assets/{ns}/models/item/{name}.json"
    m = json.loads(jar.read(ref)) if ns == "minecraft" else json.loads((ROOT / "src/main/resources" / ref).read_text(encoding="utf-8"))
    assert set(m.get("textures", {})) == {"layer0"}, f"{name}: one layer expected, got {m.get('textures')}"
    return m


def ref(path):
    return {"type": "minecraft:model", "model": f"mechanical:item/tool_parts/{path}"}


def select(index, cases, fallback):
    if not cases:
        return fallback
    return {"type": "minecraft:select", "property": "minecraft:custom_model_data", "index": index,
            "cases": [{"when": k, "model": v} for k, v in cases.items()], "fallback": fallback}


def build(ns, name, dbind, drod, opts, sheets):
    im = texture(f"minecraft:{name}" if ns == "minecraft" else name)
    opaque, head, haft, zone, wrapped = split(im, opts)
    assert haft and zone, f"{name}: no haft/zone found"
    t = tier(name)
    rods = [r for r, (rt, _) in RODS.items() if rt <= t and (r != "leather" or drod == "leather")]
    binds = [b for b, bt in BINDINGS.items() if bt <= t]
    art = opts.get("art_rod", drod)  # the rod the sprite shows: the fallback, so it needs no layer of its own
    assert drod in rods and art in rods and dbind in binds, name
    made_rods = [r for r in rods if r != art or r == "tool_rod"]  # the sprites draw a rough stick; the planed Tool Rod gets its own
    src = im.load()
    haft_shades = {src[p][:3] for p in haft | (set() if wrapped else zone)}
    zone_shades = {src[p][:3] for p in zone}
    layers = {"head": layer(im, head), "rod": layer(im, haft), "zone": layer(im, zone)}
    for r in made_rods:
        rp = ramp(RODS[r][1])
        if r.split("_")[0] == name.split("_")[0]:  # the head's own metal: a step darker (its highlight kept), so the haft stays apart
            rp = [tuple(v * 2 // 3 for v in rp[0])] + rp[:-2] + rp[-1:]
        m = recolour(haft_shades, rp)
        layers[f"rod_{r}"] = rod_layer(im, haft, m, r, head, rp)
        if not wrapped:
            layers[f"zone_{r}"] = rod_layer(im, zone, m, r, head, rp)
    for b in binds:
        if b != dbind or not wrapped:
            if wrapped:
                layers[f"binding_{b}"] = layer(im, zone, recolour(zone_shades, BIND[b][:3]))
            else:
                layers[f"binding_{b}"] = Image.new("RGBA", (16, 16))
                paint_wrap(layers[f"binding_{b}"], zone, b, ends="zone_px" not in opts)

    # self-check: the default layers are the original sprite, pixel for pixel, and never overlap
    assert not (head & haft or head & zone or haft & zone) and head | haft | zone == opaque, name
    rebuilt = Image.new("RGBA", (16, 16))
    for k in ("head", "rod", "zone"):
        rebuilt.alpha_composite(layers[k])
    assert rebuilt.tobytes() == im.tobytes(), f"{name}: default layers differ from the sprite"

    model = template_model(ns, name, opts.get("spear"))
    for k, img in layers.items():
        write_png(ITEM_TEX / "tool_parts" / name / f"{k}.png", img)
        write_json(ASSETS / "mechanical/models/item/tool_parts" / name / f"{k}.json",
                   {**model, "textures": {"layer0": f"mechanical:item/tool_parts/{name}/{k}"}})
    rod_cases = {r: ref(f"{name}/rod_{r}") for r in made_rods}
    zone_default = ref(f"{name}/zone") if wrapped else select(0, {r: ref(f"{name}/zone_{r}") for r in made_rods}, ref(f"{name}/zone"))
    bind_cases = {b: ref(f"{name}/binding_{b}") for b in binds if b != dbind or not wrapped}
    composite = {"type": "minecraft:composite", "models": [ref(f"{name}/head"), select(0, rod_cases, ref(f"{name}/rod")),
                                                            select(1, bind_cases, zone_default)]}
    if opts.get("spear"):
        # shortcut: the 3D in-hand spear keeps its fixed model; only the gui / ground sprite follows the parts. Upgrade by splitting *_in_hand too.
        item = {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
                          "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": composite}],
                          "fallback": {"type": "minecraft:model", "model": f"{ns}:item/{name}_in_hand"}},
                "swap_animation_scale": 1.95}
    else:
        item = {"model": composite}
    write_json(ASSETS / ns / "items" / f"{name}.json", item)
    if sheets:
        sheet(sheets, name, im, head, haft, zone, layers, rods, binds, drod, wrapped)
    return len(layers)


def sheet(out, name, im, head, haft, zone, layers, rods, binds, drod, wrapped):
    """Row 1: original, classification (head grey, haft brown, binding zone red); then one cell per rod, one per binding
    on the default rod, all as the Tool Station builds them."""
    cls = Image.new("RGBA", (16, 16))
    for p in head:
        cls.putpixel(p, (150, 150, 160, 255))
    for p in haft:
        cls.putpixel(p, (140, 90, 40, 255))
    for p in zone:
        cls.putpixel(p, (220, 40, 40, 255))
    cells = [("original", im), ("split", cls)]
    zone_for = lambda r: layers["zone"] if wrapped else layers.get(f"zone_{r}", layers["zone"])
    for r in rods:
        c = layers["head"].copy()
        c.alpha_composite(layers.get(f"rod_{r}", layers["rod"]))
        c.alpha_composite(layers.get(f"binding_{BIND_DEFAULT[name]}", zone_for(r)))
        cells.append(("rod " + r, c))
    for b in binds:
        c = layers["head"].copy()
        c.alpha_composite(layers.get(f"rod_{drod}", layers["rod"]))
        c.alpha_composite(layers.get(f"binding_{b}", zone_for(drod)))
        cells.append(("bind " + b, c))
    S, cols = 8, 6
    W = 16 * S + 12
    rows = (len(cells) + cols - 1) // cols
    img = Image.new("RGBA", (cols * W, rows * (W + 12)), (52, 52, 60, 255))
    d = ImageDraw.Draw(img)
    for i, (label, c) in enumerate(cells):
        x, y = (i % cols) * W + 6, (i // cols) * (W + 12) + 14
        d.text((x, y - 12), label, fill=(255, 255, 255, 255))
        bg = Image.new("RGBA", (16, 16), (139, 139, 139, 255))  # the inventory slot grey
        bg.alpha_composite(c)
        img.paste(bg.resize((16 * S, 16 * S), Image.NEAREST), (x, y))
    out.mkdir(parents=True, exist_ok=True)
    img.save(out / f"parts_{name}.png")


def walk(node):
    if isinstance(node, dict):
        for k, v in node.items():
            yield k, v
            yield from walk(v)
    elif isinstance(node, list):
        for v in node:
            yield from walk(v)


BIND_DEFAULT = {n: b for _, n, b, *_ in TOOLS}


# ---- bows and fishing rods (spec addendum): no head on a haft, so zones by the sprite's own colours instead of split()
# A bow is string + grip + stave (custom_model_data strings 2, 1, 0), a fishing rod hook (fixed) + line + rod (strings 1, 0).
# Every frame (pulling, cast) gets its own layers; the arrow and the hook are never recoloured.
POLE = hexset("2a1e10", "2f2216", "3e2f1f", "5a4630", "7d6038", "a3834f")  # the bows' and rods' drawn wood
LINE = hexset("826655", "634e42", "534237", "a08470")  # the crude bow's string, the fishing line and the line wound on the reel
LINE_RAMP = {"cordage": BIND["cordage"], "sinew": BIND["sinew"], "string": [(150, 152, 156), (206, 208, 212), (246, 246, 248)]}
# a taut bowstring has no dark edge (a lone shade takes the light, two shade and light): cordage and sinew as the Crude Bow and the Yew
# Longbow draw them, twine a duller flax, spider silk near white
BOWSTRING = {"cordage": [(99, 78, 66), (130, 102, 85)], "sinew": [(190, 160, 104), (226, 196, 150)],
             "twine": [(150, 144, 128), (186, 178, 154)], "string": [(206, 208, 212), (246, 246, 248)]}
STAVE_RAMP = {"stick": "minecraft:stick", "bamboo": "minecraft:bamboo"}
BOW_FRAMES = ["", "_pulling_0", "_pulling_1", "_pulling_2"]
ROD_FRAMES = ["", "_cast"]
CROSSBOW_FRAMES = ["", "_pulling_0", "_pulling_1", "_pulling_2", "_arrow", "_firework"]  # model names; the first one's texture is *_standby
# crossbows (docs/superpowers/specs/2026-11-26-crossbows-design.md): the stock and string recoloured, the prod, stirrup, windlass and the
# loaded bolt or rocket never. Vanilla draws the string grey, so the Tool Station's Sinew gets a layer of its own ("drawn" is the sprite's).
CROSSBOW_WOOD = hexset("4d2e11", "2a1a09", "6d4218", "90581f", "b5732f", "3b240d", "1f1206", "482b10", "36200b")
CROSSBOW_GREY = hexset("444444", "6d6d6d", "969696", "979797")


def rod_zones(x, y, c):
    """A fishing rod's pixel: the line runs down the right from the tip (x + y >= 18), and its end is wound on the reel low on the pole."""
    if c in LINE and (x >= 9 and x + y >= 18 or 5 <= x <= 7 and 11 <= y <= 13):
        return "grip"  # the line sits in the binding slot
    if c in POLE or c in LINE:
        return "rod"
    return "fixed"


def crossbow_zones(x, y, c):
    """The string is grey (but the stirrup in the top-left corner is not); the stock is the wood on the diagonal band from the prod to the
    butt (gen_crossbows.stock); the wooden prod arc off that band, a steel prod and everything loaded stay fixed."""
    if c in CROSSBOW_GREY:
        return "fixed" if x <= 5 and y <= 5 else "grip"
    return "rod" if c in CROSSBOW_WOOD and x + y >= 11 and abs(x - y) <= 3 else "fixed"


def crude_bow_zones(x, y, c):
    return "string" if c in hexset("826655", "634e42") else "grip" if c in hexset("765636", "b5977d") else "rod" if c in POLE else "fixed"


def yew_zones(x, y, c):
    return "string" if c in hexset("e2c496") else "grip" if c in hexset("604228", "967046") else "fixed"


# name, frames, zone(x, y, colour) -> layer, the parts per slot (rod, binding[, string]) with the default first (its layer is the sprite's own)
RANGED = [
    ("crude_bow", BOW_FRAMES, crude_bow_zones, [["tool_rod", "stick"], ["twine", "cordage", "leather", "sinew"], ["cordage", "twine", "string", "sinew"]]),
    ("yew_longbow", BOW_FRAMES, yew_zones, [["yew_stave"], ["leather", "twine", "cordage", "sinew"], ["sinew", "twine", "cordage", "string"]]),
] + [(n, ROD_FRAMES, rod_zones, [["tool_rod", "stick", "bamboo"], ["twine", "cordage", "string", "sinew"]])
     for n in ("crude_fishing_rod", "bone_hook_fishing_rod", "bronze_hook_fishing_rod", "iron_hook_fishing_rod")] + [
    (n, CROSSBOW_FRAMES, crossbow_zones, [["tool_rod", "stick", "bone_tool_rod", "copper_tool_rod", "bronze_tool_rod", "iron_tool_rod"] + extra,
                                          ["drawn", "sinew", "twine", "cordage", "string"]])
    for n, extra in (("minecraft:crossbow", []), ("steel_crossbow", ["steel_tool_rod"]))]
SLOT_LAYER = ["rod", "grip", "string"]  # custom_model_data string index -> zone (a fishing rod's line is its "grip" slot)


def part_ramp(zone, part):
    if zone == "rod":
        return ramp(STAVE_RAMP[part] if part in STAVE_RAMP else RODS[part][1])
    if zone == "string":
        return BOWSTRING[part]
    return LINE_RAMP[part] if part in LINE_RAMP else BIND[part][:3]


def build_ranged(name, frames, zone_of, slots, sheets):
    """Each frame split into zones by colour, one recoloured layer per non-default part, and its model in the item definition a composite.
    A vanilla item ("minecraft:crossbow") is read from the client jar and its item definition written into the mod's minecraft assets."""
    ns, name = name.split(":") if ":" in name else ("mechanical", name)
    composites, cells, count = {}, [], 0
    for f in frames:
        frame = name + f
        model = template_model(ns, frame, False)
        tex = model["textures"]["layer0"].split(":")[-1].split("item/", 1)[1]  # vanilla's standby frame is the crossbow model's texture
        im = texture(f"minecraft:{tex}" if ns == "minecraft" else tex)
        px = im.load()
        zones = {}
        for y in range(16):
            for x in range(16):
                if px[x, y][3]:
                    assert px[x, y][3] == 255, f"{frame}: translucent pixel {(x, y)}"
                    zones.setdefault(zone_of(x, y, px[x, y][:3]), set()).add((x, y))
        layers = {k: layer(im, zones.get(k, set())) for k in ["fixed"] + SLOT_LAYER[:len(slots)]}
        rebuilt = Image.new("RGBA", (16, 16))
        for img in layers.values():
            rebuilt.alpha_composite(img)
        assert rebuilt.tobytes() == im.tobytes(), f"{frame}: default layers differ from the sprite"
        for i, parts in enumerate(slots):
            zone = SLOT_LAYER[i]
            assert zones.get(zone) or len(parts) == 1, f"{frame}: no {zone} pixels"
            shades = {px[p][:3] for p in zones.get(zone, ())}
            smooth = zone_of is crossbow_zones and zone == "rod"
            for part in parts[1:]:
                pal = part_ramp(zone, part)
                layers[f"{zone}_{part}"] = layer(im, zones[zone], recolour_smooth(shades, pal, CROSSBOW_WOOD) if smooth else recolour(shades, pal))
        for k, img in layers.items():
            write_png(ITEM_TEX / "tool_parts" / frame / f"{k}.png", img)
            write_json(ASSETS / "mechanical/models/item/tool_parts" / frame / f"{k}.json",
                       {**model, "textures": {"layer0": f"mechanical:item/tool_parts/{frame}/{k}"}})
        count += len(layers)
        composites[frame] = {"type": "minecraft:composite", "models": [ref(f"{frame}/fixed")] + [
            select(i, {p: ref(f"{frame}/{SLOT_LAYER[i]}_{p}") for p in parts[1:]}, ref(f"{frame}/{SLOT_LAYER[i]}")) for i, parts in enumerate(slots)]}
        if sheets and f in ("", "_pulling_2", "_cast"):
            for i, parts in enumerate(slots):
                for part in parts:
                    c = layers["fixed"].copy()
                    for j in range(len(slots)):
                        c.alpha_composite(layers[f"{SLOT_LAYER[j]}_{part}" if j == i and part != parts[0] else SLOT_LAYER[j]])
                    cells.append((f"{f[1:] or 'idle'} {SLOT_LAYER[i]} {part}", c))
    if sheets:
        S, cols = 8, 6
        W = 16 * S + 12
        img = Image.new("RGBA", (cols * W, (len(cells) + cols - 1) // cols * (W + 12)), (52, 52, 60, 255))
        d = ImageDraw.Draw(img)
        for i, (label, c) in enumerate(cells):
            x, y = (i % cols) * W + 6, (i // cols) * (W + 12) + 14
            d.text((x, y - 12), label, fill=(255, 255, 255, 255))
            bg = Image.new("RGBA", (16, 16), (139, 139, 139, 255))
            bg.alpha_composite(c)
            img.paste(bg.resize((16 * S, 16 * S), Image.NEAREST), (x, y))
        sheets.mkdir(parents=True, exist_ok=True)
        img.save(sheets / f"parts_{name}.png")

    # the item definition keeps its own frame logic (using_item / use_duration, fishing_rod/cast); each frame's model becomes its composite
    path = ASSETS / ns / "items" / f"{name}.json"
    prefix = f"{ns}:item/"

    def swap(node):
        if isinstance(node, dict):
            m = node.get("model") if node.get("type") == "minecraft:model" else None
            if node.get("type") == "minecraft:composite":  # a second run: back to the frame it was made from
                m = node["models"][0]["model"].replace("tool_parts/", "").rsplit("/", 1)[0]
            if isinstance(m, str) and m.startswith(prefix) and m[len(prefix):] in composites:
                return composites[m[len(prefix):]]
            return {k: swap(v) for k, v in node.items()}
        return [swap(v) for v in node] if isinstance(node, list) else node

    data = swap(json.loads(path.read_text(encoding="utf-8") if path.exists() else jar.read(f"assets/{ns}/items/{name}.json")))
    assert json.dumps(data).count("minecraft:composite") == len(frames), name
    write_json(path, data)
    return count


def main():
    sheets = Path(sys.argv[sys.argv.index("--sheets") + 1]) if "--sheets" in sys.argv else None
    assert len({n for _, n, *_ in TOOLS}) == len(TOOLS)
    total = sum(build(*t, sheets) for t in TOOLS) + sum(build_ranged(*r, sheets) for r in RANGED)
    for ns, name, *_ in TOOLS + [tuple(r[0].split(":")) if ":" in r[0] else ("mechanical", r[0]) for r in RANGED]:  # every model the item definitions name exists
        text = (ASSETS / ns / "items" / f"{name}.json").read_text(encoding="utf-8")
        for m in {v for k, v in walk(json.loads(text)) if k == "model" and isinstance(v, str)}:
            mns, path = m.split(":")
            assert (ASSETS / mns / "models" / f"{path}.json").exists() or f"assets/{mns}/models/{path}.json" in jar.namelist(), f"{name}: no model {m}"
    print(f"{len(TOOLS)} tools, {total} layers")


if __name__ == "__main__":
    main()
