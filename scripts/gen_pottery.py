"""Bronze Age ceramics: Potter's Wheel, Storage Jar, Water Jug and their greenware.
Writes the round block models (each round tier is two crossed boxes, a chamfered square), the wheel's turning parts
(wheel head and the clay stages, drawn by client/PottersWheelRenderer through item model definitions without items),
blockstates, item model definitions, loot tables, the Potter's Wheel recipe, the mineable tag and lang. The kiln
firing is in Java (KilnBlockEntity.RECIPES). Code: Pottery.java and the classes it names. Textures: gen_pottery_textures.py.
Generator order: ... gen_wolf_pelt.py, gen_pottery.py, gen_block_items.py, gen_unlocks.py, gen_registry.py.
Run: python scripts/gen_pottery.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
MAX_WATER = 6  # WaterJugBlock.MAX


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


def tier(y0, y1, width, chamfer, side, top, bottom=None, cx=8.0, cz=8.0):
    """A round slice from y0 to y1: two crossed boxes (a square with its corners cut by `chamfer`). The side texture is
    a wrap of the pot's profile (texture row 0 = model top), so each slice takes the rows of its own height. The second
    box's top and bottom sit a hair inside the first's so the two lids don't z-fight."""
    h = width / 2
    out = []
    for i, (dx, dz) in enumerate([(h, h - chamfer), (h - chamfer, h)] if chamfer else [(h, h)]):
        e = 0.01 * i
        x0, x1, z0, z1 = cx - dx, cx + dx, cz - dz, cz + dz
        faces = {
            "north": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": side},
            "south": {"uv": [x0, 16 - y1, x1, 16 - y0], "texture": side},
            "west": {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": side},
            "east": {"uv": [z0, 16 - y1, z1, 16 - y0], "texture": side},
            "up": {"uv": [x0, z0, x1, z1], "texture": top},
        }
        if bottom:
            faces["down"] = {"uv": [x0, z0, x1, z1], "texture": bottom}
        out.append({"from": [x0, y0 + e, z0], "to": [x1, y1 - e, z1], "faces": faces})
    return out


def box(frm, to, tex):
    """A small plain box (spout, handle)."""
    faces = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        faces[f] = {"texture": tex}
    return {"from": frm, "to": to, "faces": faces}


def model(name, elements, textures, parent="minecraft:block/block"):
    w(A / f"models/{name}.json", {"parent": parent, "textures": textures, "elements": elements})


# ---------------- Storage Jar: wide belly, narrow neck, rim; a lid with a knob when sealed ----------------
JAR_BODY = (tier(0, 1, 8, 1, "#side", "#top", "#top") + tier(1, 3, 10, 2, "#side", "#top") + tier(3, 8, 12, 2, "#side", "#top")
            + tier(8, 10, 10, 2, "#side", "#top") + tier(10, 12, 6, 1, "#side", "#top"))
JAR_TEX = {"particle": M + "block/storage_jar_side", "side": M + "block/storage_jar_side", "top": M + "block/storage_jar_top",
           "lid": M + "block/storage_jar_lid", "mouth": M + "block/storage_jar_mouth"}
model("block/storage_jar", JAR_BODY + tier(12, 13, 8, 1, "#side", "#top") + tier(13, 14, 7, 1.5, "#side", "#lid") + tier(14, 15, 2, 0, "#side", "#lid"), JAR_TEX)
model("block/storage_jar_open", JAR_BODY + tier(12, 13, 8, 1, "#side", "#mouth"), JAR_TEX)
ROT = [0, 90, 180, 270]
w(A / "blockstates/storage_jar.json", {"variants": {
    f"open={o}": [{"model": M + ("block/storage_jar_open" if o == "true" else "block/storage_jar"), **({"y": y} if y else {})} for y in ROT]
    for o in ("false", "true")}})

# ---------------- Water Jug: round belly, tall neck, spout and handle; the mouth shows the water ----------------
def jug(mouth):
    els = (tier(0, 1, 6, 1, "#side", "#top", "#top") + tier(1, 6, 10, 2, "#side", "#top") + tier(6, 8, 8, 2, "#side", "#top")
           + tier(8, 12, 4, 1, "#side", "#top") + tier(12, 13, 6, 1, "#side", mouth))
    els.append(box([7, 11.5, 3.5], [9, 12.75, 5], "#side"))          # spout (north)
    els.append(box([7.5, 10, 10], [8.5, 11, 13], "#side"))           # handle: top bar out from the neck
    els.append(box([7.5, 5.5, 12], [8.5, 10, 13], "#side"))          # handle: down to the shoulder
    return els


JUG_TEX = {"particle": M + "block/water_jug_side", "side": M + "block/water_jug_side", "top": M + "block/water_jug_top",
           "mouth": M + "block/water_jug_mouth", "water_low": M + "block/water_jug_mouth_low", "water_full": M + "block/water_jug_mouth_full"}
MOUTH = lambda n: "#mouth" if n == 0 else "#water_low" if n <= MAX_WATER // 2 else "#water_full"
for name, mouth in [("water_jug", "#mouth"), ("water_jug_low", "#water_low"), ("water_jug_full", "#water_full")]:
    model(f"block/{name}", jug(mouth), JUG_TEX)
jug_model = lambda n: M + ("block/water_jug" if n == 0 else "block/water_jug_low" if n <= MAX_WATER // 2 else "block/water_jug_full")
w(A / "blockstates/water_jug.json", {"variants": {f"water={n}": [{"model": jug_model(n), **({"y": y} if y else {})} for y in ROT]
                                                  for n in range(MAX_WATER + 1)}})
# held: the jug shows its water, read from the block_state component
w(A / "items/water_jug.json", {"model": {"type": "minecraft:select", "property": "minecraft:block_state", "block_state_property": "water",
    "cases": [{"when": [str(n) for n in range(1, MAX_WATER + 1) if jug_model(n) == m], "model": {"type": "minecraft:model", "model": m}}
              for m in (M + "block/water_jug_low", M + "block/water_jug_full")],
    "fallback": {"type": "minecraft:model", "model": M + "block/water_jug"}}})
w(A / "items/storage_jar.json", {"model": {"type": "minecraft:model", "model": M + "block/storage_jar"}})

# ---------------- greenware: the same shapes in raw grey clay ----------------
for item, parent, tex in [("unfired_storage_jar", "block/storage_jar", ["side", "top", "lid", "mouth"]),
                          ("unfired_water_jug", "block/water_jug", ["side", "top", "mouth"])]:
    base = parent.split("/")[1]
    w(A / f"models/item/{item}.json", {"parent": M + parent, "textures": {
        "particle": M + f"block/unfired_{base}_side", **{t: M + f"block/unfired_{base}_{t}" for t in tex}}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})

# ---------------- Potter's Wheel: stone flywheel, axle; the wheel head and clay turn (renderer) ----------------
STONE = {"stone_side": "minecraft:block/smooth_stone_slab_side", "stone_top": "minecraft:block/smooth_stone"}
WHEEL_BASE = (tier(0, 3, 14, 3, "#stone_side", "#stone_top", "#stone_top") + tier(3, 4, 5, 1, "#axle", "#axle_top")
              + tier(4, 11, 2, 0, "#axle", "#axle_top"))
WHEEL_HEAD = tier(10.25, 11, 4, 1, "#head_side", "#head_bottom", "#head_bottom") + tier(11, 12, 12, 3, "#head_side", "#head_top", "#head_bottom")
WHEEL_TEX = {"particle": "minecraft:block/oak_planks", **STONE, "axle": "minecraft:block/stripped_oak_log",
             "axle_top": "minecraft:block/stripped_oak_log_top", "head_side": M + "block/potters_wheel_head_side",
             "head_top": M + "block/potters_wheel_head_top", "head_bottom": "minecraft:block/oak_planks"}
model("block/potters_wheel", WHEEL_BASE, WHEEL_TEX)
w(A / "models/block/wheel_head.json", {"textures": WHEEL_TEX, "elements": WHEEL_HEAD})
model("item/potters_wheel", WHEEL_BASE + WHEEL_HEAD, WHEEL_TEX)
w(A / "blockstates/potters_wheel.json", {"variants": {"": {"model": M + "block/potters_wheel"}}})
w(A / "items/potters_wheel.json", {"model": {"type": "minecraft:model", "model": M + "item/potters_wheel"}})

# clay stages on the wheel, modelled standing at y 0 (the renderer lifts them onto the head)
CLAY_TEX = {"particle": M + "block/wheel_clay", "clay": M + "block/wheel_clay", "clay_top": M + "block/wheel_clay_top",
            "clay_mouth": M + "block/wheel_clay_mouth"}
STAGES = {
    # a slapped-down lump, a little off centre and lopsided
    "wheel_clay_lump": tier(0, 2, 7, 2, "#clay", "#clay_top", cx=8.5, cz=7.5) + tier(2, 3.5, 5, 1.5, "#clay", "#clay_top", cx=8.0, cz=8.0)
    + tier(3.5, 4.25, 2.5, 0.5, "#clay", "#clay_top", cx=7.5, cz=8.5),
    # centred: a smooth low dome
    "wheel_clay_centred": tier(0, 2, 8, 2, "#clay", "#clay_top") + tier(2, 3, 6, 2, "#clay", "#clay_top") + tier(3, 3.5, 3, 1, "#clay", "#clay_top"),
    # raised: coned up into a tall column
    "wheel_clay_raised": tier(0, 1, 6, 1.5, "#clay", "#clay_top") + tier(1, 6, 5, 1, "#clay", "#clay_top") + tier(6, 6.75, 4, 1, "#clay", "#clay_top"),
    # opened: a hollow, widening at the lip
    "wheel_clay_opened": tier(0, 4, 6, 1.5, "#clay", "#clay_top") + tier(4, 6, 7, 2, "#clay", "#clay_top") + tier(6, 7, 8, 2, "#clay", "#clay_mouth"),
}
for name, els in STAGES.items():
    w(A / f"models/block/{name}.json", {"textures": CLAY_TEX, "elements": els})
for name in ["wheel_head", *STAGES]:
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"block/{name}"}})

# ---------------- loot, recipe, tags ----------------
def loot(block, modifier=None):
    entry = {"type": "minecraft:item", "name": M + block}
    if modifier:
        entry["modifier"] = [modifier]
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [
        {"rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}], "random_sequence": f"mechanical:blocks/{block}"})


loot("potters_wheel")
loot("storage_jar")
loot("water_jug", {"type": "minecraft:copy_state", "block": M + "water_jug", "properties": ["water"]})
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "potters_wheel")

# a saw (#mechanical:saws) cuts the plank wheel head (it stays, losing 1), a bronze pivot, a cobblestone flywheel
w(D / "mechanical/recipe/potters_wheel.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["PSP", " B ", "CCC"],
    "key": {"P": "#minecraft:planks", "S": "#mechanical:saws", "B": M + "bronze_ingot", "C": "minecraft:cobblestone"},
    "result": {"id": M + "potters_wheel"}})

merge(A / "lang/en_us.json", {
    "block.mechanical.potters_wheel": "Potter's Wheel",
    "block.mechanical.storage_jar": "Storage Jar",
    "block.mechanical.water_jug": "Water Jug",
    "item.mechanical.unfired_storage_jar": "Unfired Storage Jar",
    "item.mechanical.unfired_water_jug": "Unfired Water Jug",
    "tooltip.mechanical.water_jug": "Water: %s/%s",
    "message.mechanical.wheel.shape": "Throwing a %s (%s Clay Balls)",
    "message.mechanical.wheel.needs_clay": "A %s takes %s Clay Balls (%s on the wheel); sneak with an empty hand to pick another shape",
    "message.mechanical.wheel.enough_clay": "There is enough clay on the wheel: hold right-click with an empty hand to throw it",
})
print("pottery ok")
