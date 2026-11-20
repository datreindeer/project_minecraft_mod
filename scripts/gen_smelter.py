"""Late Bronze Age Smelting Furnace: Furnace Bricks (plain, or a tuyere while the furnace is formed), the Furnace
Chute, the Furnace Tap and Clay Channels.
Writes block models (the tap: brick cube, clay spout, plug in / pulled out, glow planes for the fire mouth, the tap
hole and the metal running down the spout; the chute: a funnel down to a hole; the channel: trough middle, arm and end
wall for the multipart, and the 45-degree slope), blockstates, item model definitions, loot tables, crafting recipes,
the mineable tag and lang. The kiln firing (Unfired Furnace Brick ->
Furnace Brick, Unfired Clay Channel -> Clay Channel) is in Java (KilnBlockEntity.RECIPES). Code: Smelting.java and the
classes it names. Textures: gen_smelter_textures.py.
Generator order: ... gen_pottery.py, gen_cart.py, gen_smelter.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_smelter.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ("down", "up", "north", "south", "east", "west")


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def face_uv(face, frm, to):
    """The block-model default UV for a face, shifted back inside 0..16 where the box sticks out of the block (the
    spout, the plug), so it never samples outside its own texture."""
    (x0, y0, z0), (x1, y1, z1) = frm, to
    u, v = {"north": ((x0, x1), (16 - y1, 16 - y0)), "south": ((x0, x1), (16 - y1, 16 - y0)),
            "east": ((z0, z1), (16 - y1, 16 - y0)), "west": ((z0, z1), (16 - y1, 16 - y0)),
            "up": ((x0, x1), (z0, z1)), "down": ((x0, x1), (z0, z1))}[face]
    def fix(a, b):  # shift a span that pokes out of 0..16 back inside (by 16 when wholly out, else just enough)
        lo, hi = min(a, b), max(a, b)
        d = 16 if hi <= 0 else -16 if lo >= 16 else -lo if lo < 0 else 16 - hi if hi > 16 else 0
        return a + d, b + d
    (u0, u1), (v0, v1) = fix(*u), fix(*v)
    return [u0, v0, u1, v1]


def box(frm, to, tex, faces=FACES, **extra):
    """A box; tex is one texture for every face or a {face: texture} map."""
    texs = tex if isinstance(tex, dict) else {f: tex for f in faces}
    return {"from": frm, "to": to, **extra, "faces": {f: {"uv": face_uv(f, frm, to), "texture": t} for f, t in texs.items()}}


def model(name, elements, textures, parent="minecraft:block/block"):
    w(A / f"models/block/{name}.json", {"parent": parent, "textures": textures, "elements": elements})


def turn180(el):
    """The same element turned half round the block's vertical axis (for the item models)."""
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    swap = {"north": "south", "south": "north", "east": "west", "west": "east", "up": "up", "down": "down"}
    frm, to = [16 - x1, y0, 16 - z1], [16 - x0, y1, 16 - z0]
    return {**el, "from": frm, "to": to, "faces": {swap[f]: {**v, "uv": face_uv(swap[f], frm, to)} for f, v in el["faces"].items()}}


# ---------------- Furnace Bricks: a full cube of fireclay bricks ----------------
w(A / "models/block/furnace_bricks.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/furnace_bricks"}})
# tuyere (set by the tap on the side and back walls' bottom middles while formed): a clay blowpipe hole on the north face
TUY_TEX = {"particle": M + "block/furnace_bricks", "bricks": M + "block/furnace_bricks", "front": M + "block/furnace_bricks_tuyere",
           "glow": M + "block/furnace_tap_glow"}
model("furnace_bricks_tuyere", [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
    f: {"texture": "#front" if f == "north" else "#bricks", "cullface": f} for f in FACES}}], TUY_TEX)
# lit: the fire glows in the hole (texture px x 6..9, y 7..10 = model x 6..10, y 6..9)
model("furnace_bricks_tuyere_lit", [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
    f: {"texture": "#front" if f == "north" else "#bricks", "cullface": f} for f in FACES}},
    {"from": [6, 6, -0.01], "to": [10, 10, -0.01], "light_emission": 15, "shade": False,
     "faces": {"north": {"uv": [6, 6, 10, 10], "texture": "#glow", "cullface": "north"}}}], TUY_TEX)
variants = {}
for facing, y in ROT.items():
    rot = {"y": y} if y else {}
    variants[f"facing={facing},tuyere=none"] = {"model": M + "block/furnace_bricks"}
    variants[f"facing={facing},tuyere=cold"] = {"model": M + "block/furnace_bricks_tuyere", **rot}
    variants[f"facing={facing},tuyere=lit"] = {"model": M + "block/furnace_bricks_tuyere_lit", **rot}
w(A / "blockstates/furnace_bricks.json", {"variants": variants})
w(A / "items/furnace_bricks.json", {"model": {"type": "minecraft:model", "model": M + "block/furnace_bricks"}})

# ---------------- Furnace Chute: a basin (x/z 3..13, y 6..16) narrowing to a hole (5..11) down into the chamber ----------------
def cull(el):
    """Culls the faces of an element that lie on the block's boundary."""
    (x0, y0, z0), (x1, y1, z1) = el["from"], el["to"]
    edge = {"down": y0 == 0, "up": y1 == 16, "north": z0 == 0, "south": z1 == 16, "west": x0 == 0, "east": x1 == 16}
    for f, face in el["faces"].items():
        if edge[f]:
            face["cullface"] = f
    return el


CHUTE_TEX = {"particle": M + "block/furnace_bricks", "bricks": M + "block/furnace_bricks", "top": M + "block/furnace_chute_top",
             "inside": M + "block/furnace_chute_inside", "glow": M + "block/furnace_tap_glow"}
OUT = {"north": "#bricks", "south": "#bricks", "east": "#bricks", "west": "#bricks", "down": "#bricks"}
CHUTE = [cull(e) for e in [
    # the ring around the hole, its top the basin's floor
    box([0, 0, 0], [16, 6, 5], {**OUT, "up": "#inside", "south": "#inside"}),
    box([0, 0, 11], [16, 6, 16], {**OUT, "up": "#inside", "north": "#inside"}),
    box([0, 0, 5], [5, 6, 11], {**OUT, "up": "#inside", "east": "#inside", "north": "#inside", "south": "#inside"}),
    box([11, 0, 5], [16, 6, 11], {**OUT, "up": "#inside", "west": "#inside", "north": "#inside", "south": "#inside"}),
    # the basin's walls
    box([0, 6, 0], [16, 16, 3], {**OUT, "up": "#top", "south": "#inside", "down": "#inside"}),
    box([0, 6, 13], [16, 16, 16], {**OUT, "up": "#top", "north": "#inside", "down": "#inside"}),
    box([0, 6, 3], [3, 16, 13], {**OUT, "up": "#top", "east": "#inside", "down": "#inside"}),
    box([13, 6, 3], [16, 16, 13], {**OUT, "up": "#top", "west": "#inside", "down": "#inside"}),
]]
for e in CHUTE:  # the inner faces of the ring and walls face the opening: never culled
    for f in e["faces"].values():
        if f["texture"] == "#inside":
            f.pop("cullface", None)
model("furnace_chute", CHUTE, CHUTE_TEX)
# lit: the fire glows up the hole's four walls
GLOW_PLANE = {"light_emission": 15, "shade": False}
model("furnace_chute_lit", [
    {"from": [5.01, 0, 5], "to": [5.01, 6, 11], **GLOW_PLANE, "faces": {"east": {"uv": [5, 10, 11, 16], "texture": "#glow"}}},
    {"from": [10.99, 0, 5], "to": [10.99, 6, 11], **GLOW_PLANE, "faces": {"west": {"uv": [5, 10, 11, 16], "texture": "#glow"}}},
    {"from": [5, 0, 5.01], "to": [11, 6, 5.01], **GLOW_PLANE, "faces": {"south": {"uv": [5, 10, 11, 16], "texture": "#glow"}}},
    {"from": [5, 0, 10.99], "to": [11, 6, 10.99], **GLOW_PLANE, "faces": {"north": {"uv": [5, 10, 11, 16], "texture": "#glow"}}},
], CHUTE_TEX)
w(A / "blockstates/furnace_chute.json", {"multipart": [{"apply": {"model": M + "block/furnace_chute"}},
                                                       {"when": {"lit": "true"}, "apply": {"model": M + "block/furnace_chute_lit"}}]})
w(A / "items/furnace_chute.json", {"model": {"type": "minecraft:model", "model": M + "block/furnace_chute"}})

# ---------------- Furnace Tap (built facing north: the spout sticks out of the north face) ----------------
# The front texture has the tap hole (x 6..10, y 4..7) in a breast of darker clay and, above it, an arched fire mouth
# (texture x 4..11, rows 1..6 = model y 10..15) where the fire shows while lit.
TAP_TEX = {"particle": M + "block/furnace_bricks", "bricks": M + "block/furnace_bricks", "front": M + "block/furnace_tap_front",
           "clay": M + "block/clay_channel", "inside": M + "block/clay_channel_inside", "plug": M + "block/furnace_tap_plug",
           "stick": "minecraft:block/stripped_oak_log", "glow": M + "block/furnace_tap_glow", "molten": M + "block/furnace_tap_molten",
           "fire": M + "block/furnace_tap_fire"}
cube = {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: {"texture": "#front" if f == "north" else "#bricks", "cullface": f} for f in FACES}}
cube["faces"]["north"].pop("cullface")  # the spout sits in front of it: never culled
SPOUT = [
    box([6, 3, -5], [10, 4, 0], {"up": "#inside", "down": "#clay", "north": "#clay", "east": "#clay", "west": "#clay"}),   # floor
    box([5, 3, -5], [6, 6, 0], {"up": "#clay", "down": "#clay", "north": "#clay", "east": "#inside", "west": "#clay"}),    # west lip
    box([10, 3, -5], [11, 6, 0], {"up": "#clay", "down": "#clay", "north": "#clay", "east": "#clay", "west": "#inside"}),  # east lip
    box([5.5, 1.5, -1.5], [10.5, 3, 0], {f: "#clay" for f in FACES}),                                                    # bracket under it
]
model("furnace_tap", [cube] + SPOUT, TAP_TEX)
# plugged: a clay bung in the hole with a stick through it to pull it by
PLUG_IN = [box([6, 4, -1], [10, 7, 0.01], "#plug"), box([7.5, 5, -3], [8.5, 6, -1], "#stick")]
# pulled: the bung lies on the ground beside the spout
PLUG_OUT = [box([11.5, 0, -2.5], [14.5, 2, 0.5], "#plug"), box([12.5, 0.5, -4.5], [13.5, 1.5, -2.5], "#stick")]
model("furnace_tap_plug_in", PLUG_IN, TAP_TEX)
model("furnace_tap_plug_out", PLUG_OUT, TAP_TEX)
# lit: flames in the fire mouth (cut out to its arch) and the glow of the (open) tap hole; the plug covers the hole glow when it is in
GLOW = {"light_emission": 15, "shade": False}
model("furnace_tap_lit", [
    {"from": [4, 10, -0.01], "to": [12, 15, -0.01], **GLOW, "faces": {"north": {"uv": [4, 1, 12, 6], "texture": "#fire"}}},
    {"from": [6, 4, -0.01], "to": [10, 7, -0.01], **GLOW, "faces": {"north": {"uv": [6, 9, 10, 12], "texture": "#glow"}}},
], TAP_TEX)
# pouring: molten metal running down the spout and dropping off its lip into the channel
model("furnace_tap_pouring", [
    {"from": [6.5, 4.01, -5], "to": [9.5, 4.5, 0], **GLOW, "faces": {"up": {"uv": [6.5, 0, 9.5, 5], "texture": "#molten"}}},
    {"from": [6.75, 3.25, -5.3], "to": [9.25, 4.5, -5.3], **GLOW, "faces": {"north": {"uv": [6.75, 8, 9.25, 9.25], "texture": "#molten"},
                                                                         "south": {"uv": [6.75, 8, 9.25, 9.25], "texture": "#molten"}}},
], TAP_TEX)
parts = []
for facing, y in ROT.items():
    rot = {"y": y} if y else {}
    parts += [{"when": {"facing": facing}, "apply": {"model": M + "block/furnace_tap", **rot}},
              {"when": {"facing": facing, "open": "false"}, "apply": {"model": M + "block/furnace_tap_plug_in", **rot}},
              {"when": {"facing": facing, "open": "true"}, "apply": {"model": M + "block/furnace_tap_plug_out", **rot}},
              {"when": {"facing": facing, "lit": "true"}, "apply": {"model": M + "block/furnace_tap_lit", **rot}},
              {"when": {"facing": facing, "pouring": "true"}, "apply": {"model": M + "block/furnace_tap_pouring", **rot}}]
w(A / "blockstates/furnace_tap.json", {"multipart": parts})
model("furnace_tap_inventory", [cube] + SPOUT + PLUG_IN, TAP_TEX)
w(A / "items/furnace_tap.json", {"model": {"type": "minecraft:model", "model": M + "block/furnace_tap_inventory"}})

# ---------------- Clay Channel: a trough (floor 0..2, walls up to 5, channel 6 wide) ----------------
CH_TEX = {"particle": M + "block/clay_channel", "clay": M + "block/clay_channel", "inside": M + "block/clay_channel_inside"}
CLAY = {f: "#clay" for f in FACES}
MIDDLE = [box([3, 0, 3], [13, 2, 13], {**CLAY, "up": "#inside", "down": "#clay"}),
          box([3, 2, 3], [5, 5, 5], CLAY), box([11, 2, 3], [13, 5, 5], CLAY),
          box([3, 2, 11], [5, 5, 13], CLAY), box([11, 2, 11], [13, 5, 13], CLAY)]
# north arm: floor out to the edge between two walls (their inner faces are the trough's)
ARM = [box([3, 0, 0], [13, 2, 3], {**CLAY, "up": "#inside"}),
       box([3, 2, 0], [5, 5, 3], {**CLAY, "east": "#inside"}),
       box([11, 2, 0], [13, 5, 3], {**CLAY, "west": "#inside"})]
END = [box([5, 2, 3], [11, 5, 5], {**CLAY, "south": "#inside"})]  # north end wall, inner face south
for name, els in (("clay_channel_middle", MIDDLE), ("clay_channel_arm", ARM), ("clay_channel_end", END)):
    model(name, els, CH_TEX)
# slope climbing north (like vanilla's raised rail): a straight trough, floor top at y 8, turned 45 degrees about x through
# the block's middle and stretched to the diagonal, so its floor runs from the south edge's bottom to the north edge's top
TILT = {"origin": [8, 8, 8], "axis": "x", "angle": 45, "rescale": True}
SLOPE = [box([3, 6, 0], [13, 8, 16], {**CLAY, "up": "#inside"}, rotation=TILT),
         box([3, 8, 0], [5, 11, 16], {**CLAY, "east": "#inside"}, rotation=TILT),
         box([11, 8, 0], [13, 11, 16], {**CLAY, "west": "#inside"}, rotation=TILT)]
model("clay_channel_slope", SLOPE, CH_TEX)
parts = [{"when": {"slope": "none"}, "apply": {"model": M + "block/clay_channel_middle"}}]
for side, y in ROT.items():
    rot = {"y": y, "uvlock": True} if y else {}
    parts += [{"when": {"slope": "none", side: "true"}, "apply": {"model": M + "block/clay_channel_arm", **rot}},
              {"when": {"slope": "none", side: "false"}, "apply": {"model": M + "block/clay_channel_end", **rot}},
              {"when": {"slope": side}, "apply": {"model": M + "block/clay_channel_slope", **({"y": y} if y else {})}}]
w(A / "blockstates/clay_channel.json", {"multipart": parts})
# held: a straight north-south length
model("clay_channel_inventory", MIDDLE + ARM + [turn180(e) for e in ARM], CH_TEX)
w(A / "items/clay_channel.json", {"model": {"type": "minecraft:model", "model": M + "block/clay_channel_inventory"}})

# ---------------- flat items ----------------
for item in ("unfired_furnace_brick", "furnace_brick", "unfired_clay_channel"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})

# ---------------- loot, tags ----------------
for block in ("furnace_bricks", "furnace_chute", "furnace_tap", "clay_channel"):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
        "random_sequence": f"mechanical:blocks/{block}"})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "furnace_bricks", M + "furnace_chute", M + "furnace_tap", M + "clay_channel")

# ---------------- recipes ----------------
def shaped(name, pattern, key, result, count=1, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": key, "result": {"id": M + result, **({"count": count} if count > 1 else {})}})


# fireclay: clay tempered with sand, so the bricks stand the heat
shaped("unfired_furnace_brick", ["CS", "SC"], {"C": "minecraft:clay_ball", "S": "#minecraft:sand"}, "unfired_furnace_brick", 4)
shaped("furnace_bricks", ["BB", "BB"], {"B": M + "furnace_brick"}, "furnace_bricks", 1, "building")
shaped("unfired_clay_channel", ["C C", "C C"], {"C": "minecraft:clay_ball"}, "unfired_clay_channel", 4)
shaped("furnace_chute", ["B B", " B "], {"B": M + "furnace_brick"}, "furnace_chute", 1, "building")  # a funnel of three bricks
w(D / "mechanical/recipe/furnace_tap.json", {"type": "minecraft:crafting_shapeless", "category": "building",
    "ingredients": [M + "furnace_bricks", M + "clay_channel", "minecraft:clay_ball", "minecraft:stick"], "result": {"id": M + "furnace_tap"}})

merge(A / "lang/en_us.json", {
    "item.mechanical.unfired_furnace_brick": "Unfired Furnace Brick",
    "item.mechanical.furnace_brick": "Furnace Brick",
    "block.mechanical.furnace_bricks": "Furnace Bricks",
    "block.mechanical.furnace_tap": "Furnace Tap",
    "block.mechanical.furnace_chute": "Furnace Chute",
    "item.mechanical.unfired_clay_channel": "Unfired Clay Channel",
    "block.mechanical.clay_channel": "Clay Channel",
    "container.mechanical.smelting_furnace": "Smelting Furnace",
    "message.mechanical.furnace.checking": "Checking the furnace...",
    "message.mechanical.furnace.missing": "Not a furnace yet: %s Furnace Bricks missing (one at %s %s %s). 3x3x3, the middle block empty, a Furnace Chute on top, the tap mid-bottom front",
    "message.mechanical.furnace.blocked": "The middle of the furnace must be empty: clear %s %s %s",
    "message.mechanical.furnace.no_chute": "Put a Furnace Chute on top of the furnace's middle: %s %s %s",
    "message.mechanical.furnace.old_shaft": "Rebuild for 0.58: only the middle block stays empty. Furnace Bricks into the bottom of the shaft (%s %s %s), a Furnace Chute on top (%s %s %s)",
    "message.mechanical.channel.nothing_to_scoop": "Not a whole unit of metal in these channels",
    "message.mechanical.furnace.tap_open": "Tap open: molten metal runs out into the channels and molds",
    "message.mechanical.furnace.tap_closed": "Tap plugged",
    "message.mechanical.furnace.unlit": "The furnace is not lit",
    "message.mechanical.furnace.needs_fuel": "Throw Charcoal onto the chute first",
    "gui.mechanical.furnace.molten": "Molten %s",
    "gui.mechanical.furnace.amount": "%s units (%s)",
    "gui.mechanical.furnace.units": "%s units",
    "gui.mechanical.furnace.unmelted": "Unmelted %s",
    "gui.mechanical.furnace.quality.good": "Good",
    "gui.mechanical.furnace.quality.poor": "Poor",
    "gui.mechanical.furnace.quality.pure": "pure",
    "gui.mechanical.furnace.quality.slag": "tin lost as slag",
    "gui.mechanical.furnace.quality.mixing": "still mixing",
    "gui.mechanical.furnace.quality.needs_copper": "needs copper",
    "gui.mechanical.furnace.mix": "Mix: %s%% tin = %s",
    "gui.mechanical.furnace.empty": "Empty: throw ore onto the chute",
    "gui.mechanical.furnace.open": "Tap open",
    "gui.mechanical.furnace.closed": "Tap closed (sneak + use)",
    "gui.mechanical.furnace.pouring": "Tap open: pouring",
    "gui.mechanical.furnace.settling": "Still melting together...",
    "gui.mechanical.furnace.too_cold": "Too cold to flow: %s°",
    "gui.mechanical.furnace.slag": "Wrong tin share: lost as slag",
    "gui.mechanical.furnace.fuel": "Charcoal %s/%s",
    "gui.mechanical.furnace.unlit": "Not lit: Fire Drill on the tap",
    "gui.mechanical.furnace.speed": "Melting speed: %sx (hotter is faster)",
    "gui.mechanical.furnace.tank": "%s / %s units",
})
print("smelter ok")
