"""Late Bronze Age Smelting Furnace: Furnace Bricks, the Furnace Tap and Clay Channels.
Writes block models (the tap: brick cube, clay spout, plug in / pulled out, glow planes for the fire and the metal
running down the spout; the channel: trough middle, arm and end wall for the multipart), blockstates, item model
definitions, loot tables, crafting recipes, the mineable tag and lang. The kiln firing (Unfired Furnace Brick ->
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
w(A / "blockstates/furnace_bricks.json", {"variants": {"": {"model": M + "block/furnace_bricks"}}})
w(A / "items/furnace_bricks.json", {"model": {"type": "minecraft:model", "model": M + "block/furnace_bricks"}})

# ---------------- Furnace Tap (built facing north: the spout sticks out of the north face) ----------------
# The front texture has the tap hole (x 6..10, y 4..7) in a breast of darker clay and a peephole (x 7..9, y 12..14).
TAP_TEX = {"particle": M + "block/furnace_bricks", "bricks": M + "block/furnace_bricks", "front": M + "block/furnace_tap_front",
           "clay": M + "block/clay_channel", "inside": M + "block/clay_channel_inside", "plug": M + "block/furnace_tap_plug",
           "stick": "minecraft:block/stripped_oak_log", "glow": M + "block/furnace_tap_glow", "molten": M + "block/furnace_tap_molten"}
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
# lit: fire seen through the peephole and the (open) tap hole; the plug covers the hole glow when it is in
GLOW = {"light_emission": 15, "shade": False}
model("furnace_tap_lit", [
    {"from": [7, 12, -0.01], "to": [9, 14, -0.01], **GLOW, "faces": {"north": {"uv": [7, 2, 9, 4], "texture": "#glow"}}},
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
parts = [{"apply": {"model": M + "block/clay_channel_middle"}}]
for side, y in ROT.items():
    rot = {"y": y, "uvlock": True} if y else {}
    parts += [{"when": {side: "true"}, "apply": {"model": M + "block/clay_channel_arm", **rot}},
              {"when": {side: "false"}, "apply": {"model": M + "block/clay_channel_end", **rot}}]
w(A / "blockstates/clay_channel.json", {"multipart": parts})
# held: a straight north-south length
model("clay_channel_inventory", MIDDLE + ARM + [turn180(e) for e in ARM], CH_TEX)
w(A / "items/clay_channel.json", {"model": {"type": "minecraft:model", "model": M + "block/clay_channel_inventory"}})

# ---------------- flat items ----------------
for item in ("unfired_furnace_brick", "furnace_brick", "unfired_clay_channel"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})

# ---------------- loot, tags ----------------
for block in ("furnace_bricks", "furnace_tap", "clay_channel"):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
        "random_sequence": f"mechanical:blocks/{block}"})
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "furnace_bricks", M + "furnace_tap", M + "clay_channel")

# ---------------- recipes ----------------
def shaped(name, pattern, key, result, count=1, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": key, "result": {"id": M + result, **({"count": count} if count > 1 else {})}})


# fireclay: clay tempered with sand, so the bricks stand the heat
shaped("unfired_furnace_brick", ["CS", "SC"], {"C": "minecraft:clay_ball", "S": "#minecraft:sand"}, "unfired_furnace_brick", 4)
shaped("furnace_bricks", ["BB", "BB"], {"B": M + "furnace_brick"}, "furnace_bricks", 1, "building")
shaped("unfired_clay_channel", ["C C", "C C"], {"C": "minecraft:clay_ball"}, "unfired_clay_channel", 4)
w(D / "mechanical/recipe/furnace_tap.json", {"type": "minecraft:crafting_shapeless", "category": "building",
    "ingredients": [M + "furnace_bricks", M + "clay_channel", "minecraft:clay_ball", "minecraft:stick"], "result": {"id": M + "furnace_tap"}})

merge(A / "lang/en_us.json", {
    "item.mechanical.unfired_furnace_brick": "Unfired Furnace Brick",
    "item.mechanical.furnace_brick": "Furnace Brick",
    "block.mechanical.furnace_bricks": "Furnace Bricks",
    "block.mechanical.furnace_tap": "Furnace Tap",
    "item.mechanical.unfired_clay_channel": "Unfired Clay Channel",
    "block.mechanical.clay_channel": "Clay Channel",
    "container.mechanical.smelting_furnace": "Smelting Furnace",
    "message.mechanical.furnace.checking": "Checking the furnace...",
    "message.mechanical.furnace.missing": "Not a furnace yet: %s Furnace Bricks missing (one at %s %s %s). 3x3x3 around an open shaft, the tap mid-bottom front",
    "message.mechanical.furnace.blocked": "The shaft must be open from top to bottom: clear %s %s %s",
    "message.mechanical.furnace.tap_open": "Tap open: molten metal runs out into the molds",
    "message.mechanical.furnace.tap_closed": "Tap plugged",
    "message.mechanical.furnace.unlit": "The furnace is not lit",
    "message.mechanical.furnace.needs_fuel": "Drop Charcoal down the shaft first",
    "gui.mechanical.furnace.molten": "%s: %s units",
    "gui.mechanical.furnace.charge": "Ore melting: %s units",
    "gui.mechanical.furnace.mix": "Mix: %s%% tin = %s",
    "gui.mechanical.furnace.empty": "Empty: drop ore down the shaft",
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
