"""Water power and the Sawmill data (WaterPower.java, Sawmill.java; docs/flora-and-food-plan.md section 7, phases 5a/5b).

- Water Wheel: the hub block (an invisible block model, the wheel is drawn by client/WaterPowerRenderers) and the part
  models the renderer puts the 5-block wheel together from (hub, spoke, rim board, inner ring board, paddle), each with an
  item model definition (items/<part>.json) so the renderer can draw it on a stand-in stack. Flat item picture.
- Wooden Shaft (turning shaft model) and Gearbox (timber frame block model + the bevel gear part)
- Log Flume (dry / still / running models, the running water's texture turned by the blockstate to run the right way)
  and Saw Bench (trough, guide posts, top beam; the sash with its blade, the flywheel and the pitman rod are parts)
- Sawdust (fuel 50 ticks, compost), Sawdust Briquette (4 Sawdust + Clay Ball -> 2; a Clay Kiln chars it to Charcoal)
- recipes, unlocks (wheel, shaft, gearbox, flume by an Iron Plate; the Saw Bench by a Steel Plate), loot, axe tags, lang
Textures: scripts/gen_waterpower_textures.py. Advancements: gen_advancements.py (iron_age: water_wheel, full_power, sawmill).
Generator order: ... gen_flora.py, gen_waterpower.py, gen_winter_plants.py ... (before gen_block_items / gen_unlocks).
Run: python scripts/gen_waterpower.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
B = "mechanical:block/"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def tag_add(path, *values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


FACE_AXES = {"up": ("x", "z"), "down": ("x", "z"), "north": ("x", "y"), "south": ("x", "y"), "east": ("z", "y"), "west": ("z", "y")}
AX = {"x": 0, "y": 1, "z": 2}


def span(a, b):
    """A uv range for the box edge a..b (pixels): kept as is inside one texture, else wrapped, else stretched to 16."""
    lo, hi = min(a, b), max(a, b)
    if hi - lo >= 16:
        return 0, 16
    off = (lo // 16) * 16
    lo, hi = lo - off, hi - off
    if hi > 16:
        lo, hi = 16 - (hi - lo), 16
    return lo, hi


def box(frm, to, tex, grain=None, faces=None, rot=None, uv=None):
    """An element. tex: a texture ref, or {"v", "h", "end"} picked per face by the grain axis (the board's length)."""
    out = {}
    for f in faces or FACE_AXES:
        u_ax, v_ax = FACE_AXES[f]
        u0, u1 = span(frm[AX[u_ax]], to[AX[u_ax]])
        v0, v1 = span(16 - to[AX[v_ax]], 16 - frm[AX[v_ax]]) if v_ax == "y" else span(frm[AX[v_ax]], to[AX[v_ax]])
        if isinstance(tex, dict):
            t = tex["v"] if grain == v_ax else tex["h"] if grain == u_ax else tex["end"]
        else:
            t = tex
        out[f] = {"uv": (uv or {}).get(f, [u0, v0, u1, v1]), "texture": t}
    e = {"from": frm, "to": to, "faces": out}
    if rot:
        e["rotation"] = {"origin": [8, 8, 8], "axis": rot[0], "angle": rot[1]}
    return e


def octagon(frm, to, tex, grain="z", axis="z", faces=None):
    """A box and the same box turned 45 degrees about its axis: an eight-sided prism."""
    return [box(frm, to, tex, grain, faces), box(frm, to, tex, grain, faces, rot=(axis, 45))]


def model(name, textures, elements, extra=None):
    w(A / f"models/block/{name}.json", {"textures": textures, "elements": elements, **(extra or {})})


def part(name):
    """Item model definition so the renderer can draw block model `name` on a stand-in stack."""
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": B + name}})


WOOD = {"v": "#wood_v", "h": "#wood_h", "end": "#end"}
WHEEL_TEX = {"wood_v": B + "water_wheel_wood_v", "wood_h": B + "water_wheel_wood_h", "end": B + "water_wheel_hub_end",
             "iron": B + "water_wheel_iron", "particle": B + "water_wheel_wood_v",
             "shaft_v": B + "wooden_shaft_v", "shaft_h": B + "wooden_shaft_h", "shaft_end": B + "wooden_shaft_end"}
SHAFT = {"v": "#shaft_v", "h": "#shaft_h", "end": "#shaft_end"}

# ---------------------------------------------------------------- the wheel (centre of every part model = the axle)
# hub: an iron-banded eight-sided drum on the axle (the axle is a shaft's size, so shafts line up with it)
model("water_wheel_hub", WHEEL_TEX,
      octagon([5, 5, 0], [11, 11, 16], SHAFT)
      + octagon([2.5, 2.5, 1.5], [13.5, 13.5, 14.5], WOOD)
      + octagon([2, 2, 3], [14, 14, 4.5], "#iron") + octagon([2, 2, 11.5], [14, 14, 13], "#iron"))
# spoke pair (drawn one block out): radius 5..33 px -> model y -3..25, in line with the rims (z 2..4, 12..14), a tie between
model("water_wheel_spoke", WHEEL_TEX, [
    box([6.5, -3, 2], [9.5, 25, 4], WOOD, "y"), box([6.5, -3, 12], [9.5, 25, 14], WOOD, "y"),
    box([7, 11, 4], [9, 13, 12], WOOD, "z"),
    box([6, 6, 1.6], [10, 7.5, 4.4], "#iron"), box([6, 6, 11.6], [10, 7.5, 14.4], "#iron")])  # straps where the inner ring crosses
# rim board pair (two blocks out): radius 30..36 -> y 6..12, a straight chord 15 long, front and back
model("water_wheel_rim", WHEEL_TEX, [box([0.5, 6, 2], [15.5, 12, 4], WOOD, "x"), box([0.5, 6, 12], [15.5, 12, 14], WOOD, "x")])
# inner ring between two spokes (one block out): radius 18..21 -> y 10..13, chord 16
model("water_wheel_ring", WHEEL_TEX, [box([0, 10, 2.2], [16, 13, 3.8], WOOD, "x"), box([0, 10, 12.2], [16, 13, 13.8], WOOD, "x")])
# paddle (two blocks out) between the rims: radius 27..38 -> y 3..14, a sole board on the back, iron cleats through the rims
model("water_wheel_paddle", WHEEL_TEX, [
    box([7, 3, 4], [9, 14, 12], WOOD, "y"),
    box([9, 3, 4], [12, 4.5, 12], WOOD, "x"),
    box([6.5, 8.5, 1.4], [9.5, 10.5, 2], "#iron"), box([6.5, 8.5, 14], [9.5, 10.5, 14.6], "#iron")])
for p in ("water_wheel_hub", "water_wheel_spoke", "water_wheel_rim", "water_wheel_ring", "water_wheel_paddle"):
    part(p)
# the hub block itself draws nothing (the renderer does); particles from the wet wood
model("water_wheel", {"particle": B + "water_wheel_wood_v"}, [])
w(A / "blockstates/water_wheel.json", {"variants": {"axis=x": {"model": B + "water_wheel"}, "axis=z": {"model": B + "water_wheel"}}})
w(A / "models/item/water_wheel.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/water_wheel"}})
w(A / "items/water_wheel.json", {"model": {"type": "minecraft:model", "model": M + "item/water_wheel"}})

# ---------------------------------------------------------------- shaft and gearbox
SHAFT_TEX = {k: WHEEL_TEX[k] for k in ("shaft_v", "shaft_h", "shaft_end", "iron")} | {"particle": B + "wooden_shaft_v"}
model("wooden_shaft_rotor", SHAFT_TEX, octagon([5, 5, 0], [11, 11, 16], SHAFT)
      + octagon([4.5, 4.5, 7], [11.5, 11.5, 9], "#iron"),
      {"display": {"gui": {"rotation": [30, 45, 0], "scale": [0.625] * 3}, "fixed": {"scale": [0.5] * 3}}})
part("wooden_shaft_rotor")
model("wooden_shaft", {"particle": B + "wooden_shaft_v"}, [])
w(A / "blockstates/wooden_shaft.json", {"variants": {"axis=x": {"model": B + "wooden_shaft"}, "axis=z": {"model": B + "wooden_shaft"}}})
w(A / "items/wooden_shaft.json", {"model": {"type": "minecraft:model", "model": B + "wooden_shaft_rotor"}})

GEAR_TEX = {"gear": B + "gear_wood", "frame_v": B + "gearbox_frame_v", "frame_h": B + "gearbox_frame_h", "iron": B + "water_wheel_iron",
            "shaft_v": B + "wooden_shaft_v", "shaft_h": B + "wooden_shaft_h", "shaft_end": B + "wooden_shaft_end", "particle": B + "gearbox_frame_v"}
GEAR = {"v": "#frame_v", "h": "#frame_h", "end": "#gear"}
# bevel gear on +z: body radius 3.5 at z 12..14, eight teeth to radius 5.25 (four straight, four turned 45 degrees)
teeth = []
for frm, to in (([7.25, 11.5, 11.5], [8.75, 13.25, 14]), ([7.25, 2.75, 11.5], [8.75, 4.5, 14]),
                ([2.75, 7.25, 11.5], [4.5, 8.75, 14]), ([11.5, 7.25, 11.5], [13.25, 8.75, 14])):
    teeth += [box(frm, to, "#iron"), box(frm, to, "#iron", rot=("z", 45))]
model("gearbox_gear", GEAR_TEX, octagon([6, 6, 0], [10, 10, 16], SHAFT) + octagon([4.5, 4.5, 11.5], [11.5, 11.5, 14], GEAR) + teeth)
part("gearbox_gear")
frame = [box([0, 0, 0], [16, 2, 16], GEAR, "x"), box([0, 14, 0], [16, 16, 16], GEAR, "x")]
for x, z in ((0, 0), (14, 0), (0, 14), (14, 14)):
    frame.append(box([x, 2, z], [x + 2, 14, z + 2], GEAR, "y"))
    frame.append(box([x - 0.2 if x == 0 else x + 1.2, 1, z - 0.2 if z == 0 else z + 1.2],
                     [x + 0.8 if x == 0 else x + 2.2, 15, z + 0.8 if z == 0 else z + 2.2], "#iron"))  # iron corner angles
for f in ("north", "south", "east", "west"):  # bearing blocks where the axles leave the frame
    lip = {"north": ([5, 4, -0.5], [11, 12, 1]), "south": ([5, 4, 15], [11, 12, 16.5]),
           "west": ([-0.5, 4, 5], [1, 12, 11]), "east": ([15, 4, 5], [16.5, 12, 11])}[f]
    frame += [box(lip[0], [lip[1][0], 5.2, lip[1][2]], "#iron"), box([lip[0][0], 10.8, lip[0][2]], lip[1], "#iron")]
model("gearbox", GEAR_TEX, frame)
w(A / "blockstates/gearbox.json", {"variants": {"": {"model": B + "gearbox"}}})
w(A / "items/gearbox.json", {"model": {"type": "minecraft:model", "model": B + "gearbox"}})

# ---------------------------------------------------------------- flume (along z) and saw bench (in side north, flow to +z)
FLUME_TEX = {"side": B + "log_flume_side", "inside": B + "log_flume_inside", "top": B + "log_flume_top",
             "iron": B + "water_wheel_iron", "particle": B + "log_flume_side"}
TROUGH_FACES = {"up": "#inside", "down": "#side", "north": "#top", "south": "#top", "east": "#side", "west": "#side"}


def trough(floor_gaps=(), battens=True):
    els = []
    zs = [0] + [z for g in floor_gaps for z in g] + [16]
    for z0, z1 in zip(zs[::2], zs[1::2]):
        els.append(box([1, 0, z0], [15, 3, z1], "#side") | {})
        els[-1]["faces"]["up"]["texture"] = "#inside"
    for x0 in (1, 13):
        e = box([x0, 3, 0], [x0 + 2, 12, 16], "#side")
        e["faces"]["up"]["texture"] = "#top"
        e["faces"]["east" if x0 == 1 else "west"]["texture"] = "#inside"
        els.append(e)
    for x0 in ((0, 15) if battens else ()):  # outside battens holding the walls, iron bands over them
        els.append(box([x0, 0, 6.5], [x0 + 1, 12.5, 9.5], "#top"))
        els.append(box([x0 - 0.1, 9, 6.3], [x0 + 1.1, 10.5, 9.7], "#iron"))
    return els


def water(kind):
    """The water face at y 10 between the walls: still, or running towards +z (water_flow scrolls down its texture)."""
    if kind == "dry":
        return []
    tex, uv = ("#still", [3, 0, 13, 16]) if kind == "still" else ("#flow", [0, 0, 5, 8])
    return [{"from": [3, 10, 0], "to": [13, 10, 16], "faces": {"up": {"uv": uv, "texture": tex, "tintindex": 0}}}]


WATER_TEX = {"still": "minecraft:block/water_still", "flow": "minecraft:block/water_flow"}
for kind in ("dry", "still", "flow"):
    model(f"log_flume_{kind}", FLUME_TEX | WATER_TEX, trough() + water(kind), {"render_type": "minecraft:translucent"})
ROT = {("z", "dry"): 0, ("x", "dry"): 90, ("z", "still"): 0, ("x", "still"): 90,
       ("z", "positive"): 0, ("z", "negative"): 180, ("x", "positive"): 270, ("x", "negative"): 90}
w(A / "blockstates/log_flume.json", {"variants": {
    f"axis={a},water={wt}": {"model": B + "log_flume_" + ("flow" if wt in ("positive", "negative") else wt), **({"y": ROT[(a, wt)]} if ROT[(a, wt)] else {})}
    for a in ("x", "z") for wt in ("dry", "still", "positive", "negative")}})
w(A / "items/log_flume.json", {"model": {"type": "minecraft:model", "model": B + "log_flume_dry"}})

SAW_TEX = FLUME_TEX | WATER_TEX | {"frame_v": B + "gearbox_frame_v", "frame_h": B + "gearbox_frame_h", "frame_end": B + "gear_wood",
                                    "blade": B + "saw_blade"}
FRAME = {"v": "#frame_v", "h": "#frame_h", "end": "#frame_end"}


def bench(kind):
    els = trough(floor_gaps=[(7, 9)], battens=False) + [box([1, 0, 7], [15, 1, 9], "#inside")]  # the slot the sash runs in
    for x0 in (1, 13):  # guide posts on the walls, a groove on the inside face for the sash
        els.append(box([x0, 3, 6], [x0 + 2, 30, 10], FRAME, "y"))
        els.append(box([x0 - 0.3 if x0 == 1 else x0 + 1.3, 12, 5.7], [x0 + 0.7 if x0 == 1 else x0 + 2.3, 13.5, 10.3], "#iron"))
        els.append(box([x0 - 0.3 if x0 == 1 else x0 + 1.3, 27, 5.7], [x0 + 0.7 if x0 == 1 else x0 + 2.3, 28.5, 10.3], "#iron"))
        # knee braces from the post down onto the trough wall, both ways along it
        els.append(box([x0, 4, 2.5], [x0 + 2, 12, 4.5], FRAME, "y", rot=("x", 22.5)))
        els.append(box([x0, 4, 11.5], [x0 + 2, 12, 13.5], FRAME, "y", rot=("x", -22.5)))
    els.append(box([0, 30, 5.5], [16, 32, 10.5], FRAME, "x"))       # the head beam
    els.append(box([7, 29.2, 6.5], [9, 30, 9.5], "#iron"))          # top blade guide
    return els + water(kind)


for kind in ("dry", "still", "flow"):
    model(f"saw_bench_{kind}", SAW_TEX, bench(kind), {"render_type": "minecraft:translucent"})
FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for f, y in FACING_Y.items():
    axis = "z" if f in ("north", "south") else "x"
    for wt in ("dry", "still", "positive", "negative"):
        if wt in ("dry", "still"):
            m, ry = "saw_bench_" + wt, y
        else:  # the flow model runs towards local +z; the bench is symmetric end to end, so turn it to the flow
            m = "saw_bench_flow"
            want = {("z", "positive"): "south", ("z", "negative"): "north", ("x", "positive"): "east", ("x", "negative"): "west"}[(axis, wt)]
            ry = {"south": 0, "west": 90, "north": 180, "east": 270}[want]  # model +z (south) turned by ry points at `want`
        variants[f"facing={f},water={wt}"] = {"model": B + m, **({"y": ry} if ry else {})}
w(A / "blockstates/saw_bench.json", {"variants": variants})
w(A / "items/saw_bench.json", {"model": {"type": "minecraft:model", "model": B + "saw_bench_dry"}})

# moving parts of the saw (block coordinates of a bench facing north; the renderer moves and turns them)
model("saw_sash", SAW_TEX, [
    box([3, 4, 7], [4, 26.5, 9], FRAME, "y"), box([12, 4, 7], [13, 26.5, 9], FRAME, "y"),        # stiles in the grooves
    box([-3.6, 25, 7], [19.6, 26.5, 9], FRAME, "x"),                                           # top crossbar, out to the pitmen
    box([3, 4, 7.5], [13, 5, 8.5], "#iron"),                                                       # bottom bar
    box([7.75, 5, 5.5], [8.25, 25, 10.5], "#blade", uv={"west": [0, 0, 8, 16], "east": [8, 0, 0, 16], "north": [0, 0, 0.5, 16], "south": [15.5, 0, 16, 16]}),
    box([7, 23.5, 6.3], [9, 25, 9.7], "#iron"), box([7, 5, 6.3], [9, 6.5, 9.7], "#iron"),     # blade buckles
    box([-3.7, 24.6, 6.7], [-2.1, 26.9, 9.3], "#iron"), box([18.1, 24.6, 6.7], [19.7, 26.9, 9.3], "#iron")])  # pitman eyes
spokes = []
for frm, to in (([7.5, 3, 7.5], [8.5, 13, 8.5]), ([3, 7.5, 7.5], [13, 8.5, 8.5])):
    spokes += [box(frm, to, FRAME, "y" if frm[1] == 3 else "x"), box(frm, to, FRAME, "y" if frm[1] == 3 else "x", rot=("z", 45))]
rim = []
for frm, to in (([5.7, 12.3, 7.2], [10.3, 13.6, 8.8]), ([5.7, 2.4, 7.2], [10.3, 3.7, 8.8]),
                ([2.4, 5.7, 7.2], [3.7, 10.3, 8.8]), ([12.3, 5.7, 7.2], [13.6, 10.3, 8.8])):
    rim += [box(frm, to, "#iron"), box(frm, to, "#iron", rot=("z", 45))]
model("saw_flywheel", SAW_TEX, spokes + rim + [
    box([6.5, 6.5, 7], [9.5, 9.5, 9.5], "#iron"), box([7, 7, 5], [9, 9, 7], FRAME, "z"),
    box([7.4, 10.4, 8.6], [8.6, 11.6, 10.4], "#iron")])                                           # crank pin (radius 3)
model("saw_pitman", SAW_TEX, [box([7.25, 8, 7.25], [8.75, 25.75, 8.75], FRAME, "y"),
                              box([7, 7.2, 7], [9, 9.2, 9], "#iron"), box([7, 24.6, 7], [9, 26.6, 9], "#iron")])
for p in ("saw_sash", "saw_flywheel", "saw_pitman"):
    part(p)

# ---------------------------------------------------------------- items, recipes, loot, tags
for item in ("sawdust", "sawdust_briquette"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})
for block in ("water_wheel", "wooden_shaft", "gearbox", "log_flume", "saw_bench"):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + block}]}],
        "random_sequence": f"mechanical:blocks/{block}"})
tag_add(D / "minecraft/tags/block/mineable/axe.json", *(M + b for b in ("water_wheel", "wooden_shaft", "gearbox", "log_flume", "saw_bench")))


def shaped(name, pattern, key, result, count=1):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                             "key": key, "result": {"id": result, **({"count": count} if count > 1 else {})}})


def unlock(name, item, recipes):
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})


P, IP, SP = "#minecraft:planks", M + "iron_plate", M + "steel_plate"
shaped("water_wheel", ["PPP", "IRI", "PPP"], {"P": P, "I": IP, "R": M + "wooden_shaft"}, M + "water_wheel")
shaped("wooden_shaft", ["L", "T", "L"], {"L": "#mechanical:rough_logs", "T": M + "iron_tool_rod"}, M + "wooden_shaft", 4)
shaped("gearbox", ["PRP", "RIR", "PRP"], {"P": P, "R": M + "wooden_shaft", "I": IP}, M + "gearbox")
shaped("log_flume", ["P P", "PPP"], {"P": P}, M + "log_flume", 4)
shaped("saw_bench", ["S S", "PRP", "LLL"], {"S": SP, "P": P, "R": M + "wooden_shaft", "L": "#minecraft:logs"}, M + "saw_bench")
w(D / "mechanical/recipe/sawdust_briquette.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "sawdust"] * 4 + ["minecraft:clay_ball"], "result": {"id": M + "sawdust_briquette", "count": 2}})
unlock("water_power", IP, ["water_wheel", "wooden_shaft", "gearbox", "log_flume"])
unlock("saw_bench", SP, ["saw_bench"])
unlock("sawdust_briquette", M + "sawdust", ["sawdust_briquette"])

fuel = D / "neoforge/data_maps/item/furnace_fuels.json"
fuels = json.loads(fuel.read_text(encoding="utf-8")) if fuel.exists() else {"values": {}}
fuels["values"][M + "sawdust"] = {"burn_time": 50}
w(fuel, fuels)
tag_add(D / "mechanical/tags/item/compostable.json", M + "sawdust")

merge(A / "lang/en_us.json", {
    "block.mechanical.water_wheel": "Water Wheel",
    "block.mechanical.wooden_shaft": "Wooden Shaft",
    "block.mechanical.gearbox": "Gearbox",
    "block.mechanical.log_flume": "Log Flume",
    "block.mechanical.saw_bench": "Saw Bench",
    "item.mechanical.sawdust": "Sawdust",
    "item.mechanical.sawdust_briquette": "Sawdust Briquette",
    "message.mechanical.water_power.turning": "Turning: %s / %s power",
    "message.mechanical.water_power.still": "Not turning: no Water Wheel turns this line",
    "message.mechanical.water_wheel.power": "Water Wheel: %s / %s power",
    "message.mechanical.water_wheel.no_flow": "The wheel stands still: let water run under it, or fall onto one side of it",
    "message.mechanical.water_wheel.blocked": "%s at %s is in the wheel's way",
    "message.mechanical.water_wheel.no_room": "A Water Wheel is 5 blocks across: %s at %s is in the way",
    "message.mechanical.saw.ready": "Sawing: %s power (needs %s), blade %s / %s",
    "message.mechanical.saw.flumes_in": "The saw needs %s Log Flumes in a line on its in side (where the logs come from)",
    "message.mechanical.saw.flumes_out": "The saw needs %s Log Flumes in a line on its out side",
    "message.mechanical.saw.dry": "The flume is dry: let water into its upstream end",
    "message.mechanical.saw.still": "Water comes in from both ends and stands still: feed the flume from the in side only",
    "message.mechanical.saw.wrong_way": "The water runs away from the saw: the flume needs water flowing toward the saw",
    "message.mechanical.saw.no_blade": "The saw has no blade: fit a Steel Saw Blade",
    "message.mechanical.saw.low_power": "The saw needs %2$s power from a Water Wheel's shaft, it has %1$s",
    "message.mechanical.saw.full": "The flume already holds three logs",
    "message.mechanical.saw.occupied": "A log already lies there",
    "message.mechanical.saw.out_side": "Lay logs in the flume on the saw's in side",
    "message.mechanical.saw.blade_new": "The blade is new",
    "message.mechanical.saw.blade_fitted": "Blade fitted: %s / %s logs",
})
print("waterpower ok")
