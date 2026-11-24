"""The High Furnace (docs/steel-furnace-plan.md, Steel Age; code HighFurnace.java and the classes it names), phases 0-2:
Silica Bricks, Banded Furnace Casing (with four heat glows), Firebox, Taphole, High Furnace Front (sight hatch, glowing
by heat), Great Bellows (static frame + the part models client/GreatBellowsRenderer works), the Clay Tuyere Mold, the items
(Crushed Quartzite, Unfired Silica Brick, Silica Brick, Steel Hoop, Bronze Tuyere Nozzle), recipes, loot, tags, lang.
The kiln firing (Unfired Silica Brick 1250°, the tuyere mold), the Stone Anvil (Quartzite -> 2 Crushed Quartzite) and the
anvil's Hoop pattern are in Java. Textures: gen_high_furnace_textures.py.
Generator order: right after gen_wildgrain.py (anything after it that rewrites block items / held models runs later anyway).
Run: python scripts/gen_high_furnace.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M, B = "mechanical:", "mechanical:block/"
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
FACES = ("down", "up", "north", "south", "east", "west")
SIDES = ("north", "south", "east", "west")


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
    (x0, y0, z0), (x1, y1, z1) = frm, to
    u, v = {"north": ((16 - x1, 16 - x0), (16 - y1, 16 - y0)), "south": ((x0, x1), (16 - y1, 16 - y0)),
            "east": ((16 - z1, 16 - z0), (16 - y1, 16 - y0)), "west": ((z0, z1), (16 - y1, 16 - y0)),
            "up": ((x0, x1), (z0, z1)), "down": ((x0, x1), (16 - z1, 16 - z0))}[face]

    def fix(a, b):
        lo, hi = min(a, b), max(a, b)
        d = 16 if hi <= 0 else -16 if lo >= 16 else -lo if lo < 0 else 16 - hi if hi > 16 else 0
        return a + d, b + d
    (u0, u1), (v0, v1) = fix(*u), fix(*v)
    return [u0, v0, u1, v1]


def box(frm, to, tex, faces=FACES, **extra):
    texs = tex if isinstance(tex, dict) else {f: tex for f in faces}
    return {"from": frm, "to": to, **extra, "faces": {f: {"uv": face_uv(f, frm, to), "texture": t} for f, t in texs.items()}}


def cube(tex, cull=True):
    """A full cube; tex maps face -> texture ref."""
    return {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: {"texture": tex[f], **({"cullface": f} if cull else {})} for f in FACES}}


def glow_planes(faces, tex, light, inset=0.01):
    """Fullbright planes a hair outside the cube's faces (the glowing joints / grate), culled with the face."""
    planes = {"north": ([0, 0, -inset], [16, 16, -inset]), "south": ([0, 0, 16 + inset], [16, 16, 16 + inset]),
              "west": ([-inset, 0, 0], [-inset, 16, 16]), "east": ([16 + inset, 0, 0], [16 + inset, 16, 16])}
    return [{"from": planes[f][0], "to": planes[f][1], "light_emission": light, "shade": False,
             "faces": {f: {"uv": [0, 0, 16, 16], "texture": tex, "cullface": f}}} for f in faces]


def model(name, elements, textures, parent="minecraft:block/block"):
    w(A / f"models/block/{name}.json", {"parent": parent, "textures": textures, "elements": elements})


def item_def(name, mdl):
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": mdl}})


def loot(name):
    w(D / f"mechanical/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + name}]}],
        "random_sequence": f"mechanical:blocks/{name}"})


LIGHT = {1: 9, 2: 12, 3: 15}
CASING = {"particle": B + "banded_furnace_casing", "side": B + "banded_furnace_casing", "end": B + "banded_furnace_casing_top"}
CASE_FACES = {"north": "#side", "south": "#side", "east": "#side", "west": "#side", "up": "#end", "down": "#end"}

# ---------------- Silica Bricks: the floor
w(A / "models/block/silica_bricks.json", {"parent": "minecraft:block/cube_all", "textures": {"all": B + "silica_bricks"}})
w(A / "blockstates/silica_bricks.json", {"variants": {"": {"model": B + "silica_bricks"}}})
item_def("silica_bricks", B + "silica_bricks")
loot("silica_bricks")

# ---------------- Banded Furnace Casing: the walls; heat 1-3 glow in the joints on every side
model("banded_furnace_casing", [cube(CASE_FACES)], CASING)
for h in (1, 2, 3):
    model(f"banded_furnace_casing_heat_{h}", [cube(CASE_FACES)] + glow_planes(SIDES, "#glow", LIGHT[h]),
          {**CASING, "glow": B + f"banded_furnace_casing_glow_{h}"})
w(A / "blockstates/banded_furnace_casing.json", {"variants": {
    f"heat={h}": {"model": B + "banded_furnace_casing" + (f"_heat_{h}" if h else "")} for h in range(4)}})
item_def("banded_furnace_casing", B + "banded_furnace_casing")
loot("banded_furnace_casing")

# ---------------- Firebox and Taphole: a casing cube with their face to the north (turned by facing)
def facing_block(name, front, glow_front, side_glow):
    tex = {**CASING, "front": B + front}
    faces = {**CASE_FACES, "north": "#front"}
    model(name, [cube(faces)], tex)
    for h in (1, 2, 3):
        els = [cube(faces)]
        t = dict(tex)
        if glow_front:
            t["fglow"] = B + f"{glow_front}_{h}" if glow_front.endswith("glow") else B + glow_front
            els += glow_planes(["north"], "#fglow", LIGHT[h])
        if side_glow:
            t["glow"] = B + f"banded_furnace_casing_glow_{h}"
            els += glow_planes(["south", "east", "west"], "#glow", LIGHT[h])
        model(f"{name}_heat_{h}", els, t)
    w(A / f"blockstates/{name}.json", {"variants": {
        f"facing={f},heat={h}": {"model": B + name + (f"_heat_{h}" if h else ""), **({"y": y} if y else {})}
        for f, y in ROT.items() for h in range(4)}})
    item_def(name, B + name)
    loot(name)


facing_block("firebox", "firebox_front", "firebox_front_glow", True)
facing_block("taphole", "taphole_front", None, True)

# ---------------- High Furnace Front: the steel door; the round sight hatch (texture x 5..10, y 3..8 = model x 5..11,
# y 7..13 on the north face) glows with the heat; its iron cover hangs swung open on its hinge beside it, and a handle
# bar stands off the door
FRONT_TEX = {**CASING, "front": B + "high_furnace_front", "hatch": B + "high_furnace_hatch", "glow": B + "high_furnace_front_glow",
             "iron": B + "great_bellows_cam"}
FRONT_FACES = {**CASE_FACES, "north": "#front"}
HATCH = [  # the cover (hatch texture's circle, uv x 5..11, y 3..9), hinged at the hole's left edge (model x 11), swung out
    {"from": [11, 7, -0.6], "to": [17, 13, -0.1], "rotation": {"angle": -45, "axis": "y", "origin": [11, 10, -0.35]},
     "faces": {"north": {"uv": [5, 3, 11, 9], "texture": "#hatch"}, "south": {"uv": [5, 3, 11, 9], "texture": "#hatch"},
               "east": {"uv": [5, 3, 6, 9], "texture": "#hatch"}, "west": {"uv": [10, 3, 11, 9], "texture": "#hatch"},
               "up": {"uv": [5, 3, 11, 4], "texture": "#hatch"}, "down": {"uv": [5, 8, 11, 9], "texture": "#hatch"}}},
    box([10.6, 9, -0.9], [11.4, 11, 0], "#iron"),  # the hinge knuckle
]
HANDLE = [box([2.2, 4, -1.6], [3.2, 12, -0.8], "#iron"), box([2.2, 4.5, -0.8], [3.2, 5.5, 0], "#iron"), box([2.2, 10.5, -0.8], [3.2, 11.5, 0], "#iron")]
for h in range(4):
    els = [cube(FRONT_FACES)] + HATCH + HANDLE
    if h:  # the melt glowing through the open hatch, fullbright, brighter the hotter
        els.append({"from": [5, 7, -0.02], "to": [11, 13, -0.02], "light_emission": LIGHT[h], "shade": False,
                    "faces": {"north": {"uv": [5, 3, 11, 9], "texture": "#glow", "cullface": "north"}}})
    model("high_furnace_front" + (f"_heat_{h}" if h else ""), els, FRONT_TEX)
w(A / "blockstates/high_furnace_front.json", {"variants": {
    f"facing={f},heat={h}": {"model": B + "high_furnace_front" + (f"_heat_{h}" if h else ""), **({"y": y} if y else {})}
    for f, y in ROT.items() for h in range(4)}})
item_def("high_furnace_front", B + "high_furnace_front")
loot("high_furnace_front")

# ---------------- Great Bellows (nozzle to the north). Static: the timber sill and back posts, the two bottom boards,
# the bronze nozzle, the gear train on both sides (a spur gear on the incoming axle at y 8 drives the cam shaft at y 13).
# Moving (client/GreatBellowsRenderer, part models below): the top boards hinged at the front, the leather pleats between,
# the cam shaft and the lower gears.
BT = {"particle": B + "great_bellows_board", "board": B + "great_bellows_board", "leather": B + "great_bellows_leather",
      "frame": B + "great_bellows_frame", "end": B + "great_bellows_frame_end", "bronze": B + "great_bellows_nozzle", "cam": B + "great_bellows_cam"}
FRAME = {"north": "#frame", "south": "#frame", "east": "#frame", "west": "#frame", "up": "#end", "down": "#end"}
BEAM_Z = {"north": "#end", "south": "#end", "east": "#frame", "west": "#frame", "up": "#frame", "down": "#frame"}
LEFT, RIGHT = (1.5, 7.5), (8.5, 14.5)  # the two bellows across x
static = [
    box([0, 0, 0], [2, 3, 16], BEAM_Z), box([14, 0, 0], [16, 3, 16], BEAM_Z),        # sill beams along the length
    box([2, 0, 13], [14, 2.5, 16], "#frame"), box([2, 0, 0.5], [14, 2.5, 3], "#frame"),  # cross beams
    box([0, 3, 11], [2, 15, 14], FRAME), box([14, 3, 11], [16, 15, 14], FRAME),      # back posts with the cam bearings
    box([0, 15, 10.5], [16, 16.5, 14.5], BEAM_Z | {"up": "#frame"}),                  # the cap beam over the posts
    box([0.2, 6, 6.5], [1.8, 10, 9.5], "#frame"), box([14.2, 6, 6.5], [15.8, 10, 9.5], "#frame"),  # axle bearings
]
for x0, x1 in (LEFT, RIGHT):
    static += [box([x0, 2.5, 2], [x1, 3.5, 15.5], "#board")]                     # bottom board
    static += [box([x0 + 2, 3.5, 2], [x1 - 2, 5.5, 3.2], "#bronze")]               # its throat to the nozzle
# the nozzle: both throats meet in one bronze pipe out of the front, narrowing to the tuyere
static += [box([5.5, 3, 0.8], [10.5, 6, 2.2], "#bronze"), box([6.5, 3.5, -0.4], [9.5, 5.5, 0.8], "#bronze")]
model("great_bellows", static, BT)
w(A / "blockstates/great_bellows.json", {"variants": {
    f"facing={f}": {"model": B + "great_bellows", **({"y": y} if y else {})} for f, y in ROT.items()}})
loot("great_bellows")

# part models, their hinge / axle at the model's centre (8, 8, 8): the renderer puts that point where it belongs
model("great_bellows_lid", [box([8, 8, 8], [14, 9, 21.5], "#board"), box([8.5, 9, 17.5], [13.5, 9.6, 19], "#cam"),
                            box([10, 9, 19.5], [12, 10.5, 21.2], "#frame")], BT)           # board, iron strap, the cam's tappet
model("great_bellows_pleat", [box([8.3, 8, 8.4], [13.7, 8.9, 21.1], "#leather")], BT)
ROD = {"north": "#end", "south": "#end", "east": "#frame", "west": "#frame", "up": "#frame", "down": "#frame"}
model("great_bellows_cam", [box([6.8, 6.8, -0.6], [9.2, 9.2, 16.6], ROD),                  # the cam shaft (along z here)
                            box([6.4, 6.4, 3.5], [9.6, 11.6, 5.5], "#cam"), box([6.4, 4.4, 10.5], [9.6, 9.6, 12.5], "#cam"),  # two lobes
                            box([5.4, 5.4, -0.6], [10.6, 10.6, 0.4], "#bronze"), box([5.4, 5.4, 15.6], [10.6, 10.6, 16.6], "#bronze")], BT)
model("great_bellows_gear", [box([4.6, 4.6, 7.5], [11.4, 11.4, 8.5], "#bronze"), box([7.4, 3.8, 7.4], [8.6, 12.2, 8.6], "#bronze"),
                             box([3.8, 7.4, 7.4], [12.2, 8.6, 8.6], "#bronze")], BT)   # the spur gear on the incoming axle
for p in ("great_bellows_lid", "great_bellows_pleat", "great_bellows_cam", "great_bellows_gear"):
    item_def(p, B + p)
item_def("great_bellows", B + "great_bellows")

# ---------------- the Clay Tuyere Mold (as gen_bloomery.py's molds, bronze only)
s = "tuyere"
base = {"particle": B + "kiln_clay_fired", "side": B + "kiln_clay_fired", "top": B + f"clay_{s}_mold_top"}
slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
    "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
    **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in SIDES}}}
w(A / f"models/block/clay_{s}_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
for fill in ("molten", "solid"):
    metal = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
             "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
    w(A / f"models/block/clay_{s}_mold_{fill}_bronze.json", {"parent": "minecraft:block/block",
        "textures": {**base, "metal": B + f"clay_{s}_mold_bronze_{fill}"}, "elements": [slab, metal]})
w(A / f"blockstates/clay_{s}_mold.json", {"variants": {
    f"fill={fill},metal={m},uses={u}": {"model": B + f"clay_{s}_mold" + ("" if fill == "empty" else f"_{fill}_bronze")}
    for fill in ("empty", "molten", "solid") for m in ("copper", "bronze", "poor_bronze") for u in range(5)}})
item_def(f"clay_{s}_mold", B + f"clay_{s}_mold")
w(D / f"mechanical/loot_table/blocks/clay_{s}_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": M + f"clay_{s}_mold",
                 "modifier": [{"type": "minecraft:copy_state", "block": M + f"clay_{s}_mold", "properties": ["uses"]}]}]}],
    "random_sequence": f"mechanical:blocks/clay_{s}_mold"})

# ---------------- flat items
for name in ("crushed_quartzite", "unfired_silica_brick", "silica_brick", "steel_hoop", "bronze_tuyere_nozzle", "unfired_clay_tuyere_mold"):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{name}"}})
    item_def(name, M + f"item/{name}")

# ---------------- recipes
def recipe(name, obj):
    w(D / f"mechanical/recipe/{name}.json", obj)


def shapeless(name, ingredients, count=1):
    recipe(name, {"type": "minecraft:crafting_shapeless", "category": "building", "ingredients": ingredients,
                  "result": {"id": M + name, "count": count}})


shapeless("unfired_silica_brick", [M + "crushed_quartzite", M + "crushed_quartzite", M + "fire_clay", M + "quicklime"], 4)
recipe("silica_bricks", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["BB", "BB"],
                         "key": {"B": M + "silica_brick"}, "result": {"id": M + "silica_bricks"}})
shapeless("banded_furnace_casing", [M + "silica_brick"] * 4 + [M + "steel_hoop"])
shapeless("firebox", [M + "banded_furnace_casing", M + "bronze_tuyere_nozzle", M + "iron_plate"])
shapeless("high_furnace_front", [M + "banded_furnace_casing", M + "steel_plate", M + "steel_plate", M + "iron_plate"])
shapeless("taphole", [M + "banded_furnace_casing", M + "iron_tool_rod", M + "fire_clay"])
recipe("great_bellows", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PLP", "LGL", "PTP"],
                         "key": {"P": "#minecraft:planks", "L": "minecraft:leather", "G": M + "gearbox", "T": M + "bronze_tuyere_nozzle"},
                         "result": {"id": M + "great_bellows"}})

# ---------------- tags
STONE = ["silica_bricks", "banded_furnace_casing", "firebox", "taphole", "high_furnace_front"]
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", *[M + b for b in STONE])
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "great_bellows")

# ---------------- lang
merge(A / "lang/en_us.json", {
    "item.mechanical.crushed_quartzite": "Crushed Quartzite", "item.mechanical.unfired_silica_brick": "Unfired Silica Brick",
    "item.mechanical.silica_brick": "Silica Brick", "item.mechanical.steel_hoop": "Steel Hoop",
    "item.mechanical.bronze_tuyere_nozzle": "Bronze Tuyere Nozzle", "item.mechanical.unfired_clay_tuyere_mold": "Unfired Clay Tuyere Mold",
    "block.mechanical.clay_tuyere_mold": "Clay Tuyere Mold",
    "block.mechanical.silica_bricks": "Silica Bricks", "block.mechanical.banded_furnace_casing": "Banded Furnace Casing",
    "block.mechanical.firebox": "Firebox", "block.mechanical.taphole": "Taphole", "block.mechanical.high_furnace_front": "High Furnace Front",
    "block.mechanical.great_bellows": "Great Bellows", "container.mechanical.high_furnace": "High Furnace",
    "pattern.mechanical.hoop": "Hoops (8)",
    "message.mechanical.high_furnace.checking": "Checking the furnace...",
    "message.mechanical.high_furnace.blocked": "The inside must be empty: clear %s %s %s",
    "message.mechanical.high_furnace.too_narrow": "Too narrow: the hearth needs at least 2 blocks inside, both ways",
    "message.mechanical.high_furnace.too_wide": "Too wide: a real furnace's bosh is at most 3 blocks inside",
    "message.mechanical.high_furnace.floor": "The floor under the inside must be Silica Bricks: %s %s %s",
    "message.mechanical.high_furnace.missing": "Not a High Furnace yet: %s wall blocks missing (one at %s %s %s)",
    "message.mechanical.high_furnace.too_low": "Too low: at least %s wall layers (6 m with the floor)",
    "message.mechanical.high_furnace.too_tall": "Too tall: at most %s wall layers",
    "message.mechanical.high_furnace.fireboxes": "The furnace needs %s Fireboxes for its size (has %s)",
    "message.mechanical.high_furnace.too_many_fireboxes": "At most %s Fireboxes",
    "message.mechanical.high_furnace.too_many_tapholes": "At most %s Tapholes",
    "message.mechanical.high_furnace.no_furnace": "This Firebox is not part of a High Furnace",
    "message.mechanical.high_furnace.needs_fuel": "No fuel: give the Fireboxes Charcoal or Coal",
    "message.mechanical.high_furnace.firebox_full": "The Firebox is full (%s)",
    "message.mechanical.high_furnace.firebox": "Firebox: %s / %s fuel, blast %s / %s (not lit)",
    "message.mechanical.high_furnace.firebox_lit": "Firebox burning: %s / %s fuel, blast %s / %s",
    "message.mechanical.great_bellows.blowing": "Blast %s / %s",
    "message.mechanical.great_bellows.still": "Still: turn its cam shaft with a Water Wheel's axle from the side",
    "gui.mechanical.high_furnace.heat": "%s° → %s°",
    "gui.mechanical.high_furnace.blast": "Blast %s / %s (%s over %s blocks)",
    "gui.mechanical.high_furnace.drying": "Drying out: %s:%s left (at most %s°)",
    "gui.mechanical.high_furnace.lit": "Burning", "gui.mechanical.high_furnace.out": "Out",
    "gui.mechanical.high_furnace.firebox": "#%s: %s / %s fuel, blast %s",
    "gui.mechanical.high_furnace.pool": "Pool: %s / %s mB (melting: next phase)",
    "gui.mechanical.high_furnace.mark.950": "%s°: bronze", "gui.mechanical.high_furnace.mark.1450": "%s°: iron ore",
    "gui.mechanical.high_furnace.mark.1500": "%s°: steel", "gui.mechanical.high_furnace.mark.1600": "%s°: charcoal's ceiling",
})
print("high furnace: ok")
