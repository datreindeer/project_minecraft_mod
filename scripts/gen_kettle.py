"""Bronze Kettle and Tripod data (Kettle.java, TripodBlock.java, TripodBlockEntity.java, client/KettleRenderer.java).

- Tripod: three poles lashed at the top with cordage, their ends crossing above the lashing, legs leaning out 28 degrees
  down to the ground round the fire (the tripod block stands on the campfire / fire pit, so the legs reach 16 px below
  it; euler-rotated with rescale, so each leg still ends at the ground). tripod_campfire is centred on the block,
  tripod_pit on its south-east corner (the middle of a 2x2 Fire Pit seen from part 0) and turned by the blockstate for
  parts 1-3. A short bronze hook hangs under the lashing.
- Kettle: an open pot (floor, four walls with chamfered corners, rolled rim, a narrower base), two lugs, a bail handle
  (two tilted uprights and a bar) and a bronze chain of alternating links up to the hook. kettle_campfire hangs with its
  base in the flames; kettle_pit 4 px higher (above the Fire Pit's spits, TripodBlockEntity.PIT_LIFT). The liquid
  surface and floating chunks are drawn by client/KettleRenderer (inner square +-4 px, surface y -3.5..1.5).
- multipart blockstate over pit (0 campfire, 1-4 pit part + 1) and kettle; items: flat pictures
- Clay Kettle Mold (bronze only, 8 units): slab with the kettle's cavity, molten / solid bronze looks, blockstate, loot
  (keeps uses), item; Unfired Clay Kettle Mold shaped at the Mold Bench (28 Clay Balls); fired in the kiln (KilnBlockEntity)
- items: Bronze Kettle, Rough Cast Bronze Kettle (ground on the Whetstone), Stew, Broth, Hide Glue
- recipes: Tripod (3 Tool Rods + Cordage), Bellows with Hide Glue in place of the Twine; tags: stew and broth perish in a
  day, the tripod is mineable with an axe; lang
Textures: scripts/gen_kettle_textures.py.
Generator order: ... gen_firepit.py, gen_birds2.py, gen_kettle.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_kettle.py"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
B = "mechanical:block/"
LEAN = 28        # degrees the legs lean out from the vertical
PIT_LIFT = 4     # TripodBlockEntity.PIT_LIFT


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def patch_tag(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    for v in values:
        if v not in tag["values"]:
            tag["values"].append(v)
    w(path, tag)


def r2(v):
    return round(v, 3)


def box(frm, to, tex, rot=None, skip=(), uv=None):
    """An element with uvs taken from the face's own size (wrapped into the 16 px sprite), so nothing stretches."""
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    sizes = {"north": (frm[0], dx, dy), "south": (frm[0], dx, dy), "east": (frm[2], dz, dy), "west": (frm[2], dz, dy),
             "up": (frm[0], dx, dz), "down": (frm[0], dx, dz)}
    faces = {}
    for f, (start, su, sv) in sizes.items():
        if f in skip or su <= 0 or sv <= 0:
            continue
        t = tex[f] if isinstance(tex, dict) else tex
        if uv and f in uv:
            u = uv[f]
        else:
            u0 = start % 16 if start % 16 + min(su, 16) <= 16 else 0
            v0 = (16 - min(sv, 16)) / 2 if f not in ("up", "down") else (frm[2] % 16 if frm[2] % 16 + min(sv, 16) <= 16 else 0)
            u = [u0, v0, u0 + min(su, 16), v0 + min(sv, 16)]
        faces[f] = {"uv": [r2(x) for x in u], "texture": t}
    e = {"from": [r2(v) for v in frm], "to": [r2(v) for v in to], "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


# ---------------------------------------------------------------- tripod

def tripod(cx, cz):
    els = []
    apex = 16
    for i, az in enumerate((0, 120, 240)):
        # a pole from the ground (y -16 after the rescaled tilt) through the lashing to 8 px above it
        pole = box([cx - 0.9, -16, cz - 0.9], [cx + 0.9, 24, cz + 0.9], "#pole",
                   rot={"origin": [cx, apex, cz], "x": LEAN, "y": az, "rescale": True},
                   uv={"up": [7, 7, 9, 9], "down": [7, 7, 9, 9], **{f: [4 + 3 * i, 0, 6 + 3 * i, 16] for f in ("north", "south", "east", "west")}})
        pole["faces"]["up"]["texture"] = pole["faces"]["down"]["texture"] = "#pole_end"
        els.append(pole)
    els.append(box([cx - 1.6, apex - 1.5, cz - 1.6], [cx + 1.6, apex + 1.5, cz + 1.6], "#lashing",
                   rot={"origin": [cx, apex, cz], "axis": "y", "angle": 22.5}))
    els.append(box([cx - 1.4, apex - 0.4, cz - 1.4], [cx + 1.4, apex + 0.4, cz + 1.4], "#lashing",
                   rot={"origin": [cx, apex, cz], "axis": "y", "angle": -22.5}))
    return els


LINK = {f: [6, 4, 10, 12] for f in ("north", "south", "east", "west")}  # the one oval link drawn in kettle_chain.png


def kettle(cx, cz, lift):
    """The pot, bail and chain, its middle at (cx, cz), lifted by lift."""
    ox, oy, oz = cx - 8, lift, cz - 8
    T = lambda x, y, z: [x + ox, y + oy, z + oz]
    els = []
    out, ins = "#bronze", "#inside"
    floor_y, rim_y = -4, 2
    # base and floor
    els.append(box(T(5, -6, 5), T(11, -5, 11), out))
    els.append(box(T(4, -5, 4), T(12, floor_y, 12), {"up": ins, **{f: out for f in ("down", "north", "south", "east", "west")}}))
    # walls: the inner face of each shows the dark inside
    walls = [(T(4, floor_y, 3), T(12, rim_y, 4), "south"), (T(4, floor_y, 12), T(12, rim_y, 13), "north"),
             (T(3, floor_y, 4), T(4, rim_y, 12), "east"), (T(12, floor_y, 4), T(13, rim_y, 12), "west")]
    for f, t, inner in walls:
        els.append(box(f, t, {inner: ins, **{d: out for d in ("down", "up", "north", "south", "east", "west") if d != inner}}))
    # a belly band, a little proud of the walls low down
    for f, t in ((T(4.5, -3.5, 2.6), T(11.5, -1.5, 3)), (T(4.5, -3.5, 13), T(11.5, -1.5, 13.4)),
                 (T(2.6, -3.5, 4.5), T(3, -1.5, 11.5)), (T(13, -3.5, 4.5), T(13.4, -1.5, 11.5))):
        els.append(box(f, t, out))
    # chamfered corners
    for x, z in ((3.9, 3.9), (12.1, 3.9), (3.9, 12.1), (12.1, 12.1)):
        els.append(box(T(x - 0.7, floor_y, z - 0.7), T(x + 0.7, rim_y + 0.5, z + 0.7), out,
                       rot={"origin": T(x, 0, z), "axis": "y", "angle": 45}))
    # rolled rim
    for f, t in ((T(3, rim_y, 2.5), T(13, rim_y + 1, 3.5)), (T(3, rim_y, 12.5), T(13, rim_y + 1, 13.5)),
                 (T(2.5, rim_y, 3), T(3.5, rim_y + 1, 13)), (T(12.5, rim_y, 3), T(13.5, rim_y + 1, 13))):
        els.append(box(f, t, out))
    # lugs and the bail: two uprights leaning in, a bar across
    els.append(box(T(1.5, 0.5, 7.2), T(2.6, 3.5, 8.8), out))
    els.append(box(T(13.4, 0.5, 7.2), T(14.5, 3.5, 8.8), out))
    els.append(box(T(1.7, 3, 7.6), T(2.5, 8.2, 8.4), out, rot={"origin": T(2.1, 3, 8), "axis": "z", "angle": -22.5}))
    els.append(box(T(13.5, 3, 7.6), T(14.3, 8.2, 8.4), out, rot={"origin": T(13.9, 3, 8), "axis": "z", "angle": 22.5}))
    bar_y = 3 + 5.2 * math.cos(math.radians(22.5))
    els.append(box(T(3.6, bar_y - 0.7, 7.6), T(12.4, bar_y + 0.1, 8.4), out))
    # chain: alternating links from the bar up to the hook under the lashing
    y, top, n = bar_y - 0.3, 15 - oy, 0
    while y < top - 0.5:
        h = min(2.4, top - y)
        if n % 2 == 0:
            els.append(box(T(7.4, y, 7.95), T(8.6, y + h, 8.05), "#chain", skip=("up", "down", "east", "west"), uv=LINK))
        else:
            els.append(box(T(7.95, y, 7.4), T(8.05, y + h, 8.6), "#chain", skip=("up", "down", "north", "south"), uv=LINK))
        y += 1.8
        n += 1
    return els


TRIPOD_TEX = {"particle": B + "tripod_pole", "pole": B + "tripod_pole", "pole_end": B + "tripod_pole_end", "lashing": B + "tripod_lashing"}
KETTLE_TEX = {"particle": B + "kettle_bronze", "bronze": B + "kettle_bronze", "inside": B + "kettle_inside", "chain": B + "kettle_chain"}
w(A / "models/block/tripod_campfire.json", {"textures": TRIPOD_TEX, "elements": tripod(8, 8)})
w(A / "models/block/tripod_pit.json", {"textures": TRIPOD_TEX, "elements": tripod(16, 16)})
hook = lambda cx, cz: box([cx - 0.35, 13.6, cz - 0.35], [cx + 0.35, 15.2, cz + 0.35], "#bronze")
w(A / "models/block/kettle_campfire.json", {"textures": KETTLE_TEX, "elements": kettle(8, 8, 0) + [hook(8, 8)]})
w(A / "models/block/kettle_pit.json", {"textures": KETTLE_TEX, "elements": kettle(16, 16, PIT_LIFT) + [hook(16, 16)]})

# part 0's pit middle is its south-east corner; turned clockwise: 90 -> south-west (part 1), 180 -> north-west (part 3), 270 -> north-east (part 2)
PIT_Y = {1: 0, 2: 90, 3: 270, 4: 180}
parts = [{"when": {"pit": "0"}, "apply": {"model": B + "tripod_campfire"}},
         {"when": {"pit": "0", "kettle": "true"}, "apply": {"model": B + "kettle_campfire"}}]
for pit, y in PIT_Y.items():
    rot = {"y": y} if y else {}
    parts.append({"when": {"pit": str(pit)}, "apply": {"model": B + "tripod_pit", **rot}})
    parts.append({"when": {"pit": str(pit), "kettle": "true"}, "apply": {"model": B + "kettle_pit", **rot}})
w(A / "blockstates/tripod.json", {"multipart": parts})

w(D / "mechanical/loot_table/blocks/tripod.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + "tripod"}]}],
    "random_sequence": "mechanical:blocks/tripod"})

# ---------------------------------------------------------------- items

for item in ("tripod", "bronze_kettle", "rough_cast_bronze_kettle", "stew", "broth", "hide_glue", "unfired_clay_kettle_mold"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})

# ---------------------------------------------------------------- Clay Kettle Mold (as gen_copper.py's molds; bronze only)

s = "kettle"
base = {"particle": B + "kiln_clay_fired", "side": B + "kiln_clay_fired", "top": B + f"clay_{s}_mold_top"}
slab = {"from": [1, 0, 1], "to": [15, 3, 15], "faces": {
    "up": {"texture": "#top", "uv": [1, 1, 15, 15]}, "down": {"texture": "#side", "cullface": "down"},
    **{d: {"texture": "#side", "uv": [1, 13, 15, 16]} for d in ("north", "south", "east", "west")}}}
w(A / f"models/block/clay_{s}_mold.json", {"parent": "minecraft:block/block", "textures": base, "elements": [slab]})
for fill in ("molten", "solid"):
    metal = {"from": [1, 3.01, 1], "to": [15, 3.01, 15], "shade": fill == "solid", **({"light_emission": 12} if fill == "molten" else {}),
             "faces": {"up": {"texture": "#metal", "uv": [1, 1, 15, 15]}}}
    w(A / f"models/block/clay_{s}_mold_{fill}_bronze.json", {"parent": "minecraft:block/block",
        "textures": {**base, "metal": B + f"clay_{s}_mold_bronze_{fill}"}, "elements": [slab, metal]})
w(A / f"blockstates/clay_{s}_mold.json", {"variants": {
    f"fill={fill},metal={m},uses={u}": {"model": B + f"clay_{s}_mold" + ("" if fill == "empty" else f"_{fill}_bronze")}
    for fill in ("empty", "molten", "solid") for m in ("copper", "bronze", "poor_bronze") for u in range(5)}})
w(A / f"items/clay_{s}_mold.json", {"model": {"type": "minecraft:model", "model": B + f"clay_{s}_mold"}})
w(D / f"mechanical/loot_table/blocks/clay_{s}_mold.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": M + f"clay_{s}_mold",
                 "modifier": [{"type": "minecraft:copy_state", "block": M + f"clay_{s}_mold", "properties": ["uses"]}]}]}],
    "random_sequence": f"mechanical:blocks/clay_{s}_mold"})

# ---------------------------------------------------------------- recipes and tags


def shaped(name, pattern, key, result, category="misc"):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
        "pattern": pattern, "key": key, "result": {"id": result}})


shaped("tripod", [" C ", " R ", "R R"], {"C": M + "cordage", "R": M + "tool_rod"}, M + "tripod")
# the Unfired Clay Kettle Mold is shaped at the Mold Bench from 28 Clay Balls (MoldBench.ALL)
# Hide Glue seals the bag as well as Twine lashes it
shaped("bellows_with_hide_glue", ["PHP", "PHP", "GUG"], {"P": "#minecraft:planks", "H": M + "scraped_hide", "G": M + "hide_glue",
       "U": M + "clay_tuyere"}, M + "bellows", "equipment")

patch_tag(D / "mechanical/tags/item/perishes_1_day.json", [M + "stew", M + "broth"])
patch_tag(D / "minecraft/tags/block/mineable/axe.json", [M + "tripod"])

# ---------------------------------------------------------------- lang

merge(A / "lang/en_us.json", {
    "block.mechanical.tripod": "Tripod",
    "item.mechanical.bronze_kettle": "Bronze Kettle", "item.mechanical.rough_cast_bronze_kettle": "Rough Cast Bronze Kettle",
    "item.mechanical.unfired_clay_kettle_mold": "Unfired Clay Kettle Mold", "block.mechanical.clay_kettle_mold": "Clay Kettle Mold",
    "item.mechanical.stew": "Stew", "item.mechanical.broth": "Broth", "item.mechanical.hide_glue": "Hide Glue",
    "tooltip.mechanical.poor_bronze_kettle": "Poor Bronze: holds %s things instead of %s",
    "tooltip.mechanical.stew.food": "Fills %s hunger, %s saturation",
    "tooltip.mechanical.stew.many": "%s ×%s",
    "kettle.mechanical.mode.water": "Water", "kettle.mechanical.mode.food": "Stew",
    "kettle.mechanical.mode.render": "Rendering", "kettle.mechanical.mode.tan": "Tanning liquor",
    "message.mechanical.tripod.no_fire": "A Tripod stands over a Campfire or a Fire Pit",
    "message.mechanical.tripod.one_per_pit": "This Fire Pit already has a Tripod",
    "message.mechanical.tripod.empty": "Hang a Bronze Kettle in the Tripod",
    "message.mechanical.kettle.has_water": "The kettle already has water in it",
    "message.mechanical.kettle.not_done": "It isn't done yet",
    "message.mechanical.kettle.nothing_to_ladle": "Nothing to ladle out: take what's in it with an empty hand",
    "message.mechanical.kettle.cant_go_in": "%s won't make anything in the kettle",
    "message.mechanical.kettle.wrong_mode": "%s doesn't belong in %s: empty it first (sneak, empty hand)",
    "message.mechanical.kettle.full": "The kettle is full (%s)",
    "message.mechanical.kettle.poured_out": "You pour the kettle out",
    "message.mechanical.kettle.status.dry": "Bronze Kettle: empty (holds %s). Fill it with water",
    "message.mechanical.kettle.status.ladle": "Bronze Kettle: %s bowls of %s. Ladle them out with a Bowl",
    "message.mechanical.kettle.status.mode": "%s (%s/%s)",
    "message.mechanical.kettle.status.boiling": "simmering",
    "message.mechanical.kettle.status.heating": "heating up (%s°)",
    "message.mechanical.kettle.status.warm": "only warm over embers (%s°): feed the fire",
    "message.mechanical.kettle.status.cold": "cold (%s°): light a fire under it",
    "message.mechanical.kettle.status.left": "%s:%s left",
})
print("kettle ok")
