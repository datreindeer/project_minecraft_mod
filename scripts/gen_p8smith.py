"""Smithing and fire pit phase data (docs/living-world-plan.md F9, F10; F12's own data is in gen_firepit.py / gen_kettle.py).

- F9 Satchels.java: Tool Loops and Steel-Frame Pack items (flat pictures; recipes are Leatherworking Bench recipes in code),
  item tag mechanical:satchel_tools (what the tool loops take). The equipment/back tag is written by gen_equipment.py.
- F10 Jewelry.java: Gold Ring Blank, Gold Chain; Gold Ring and Gold Necklace (two layers, the gem layer tinted by the
  custom_model_data colour Jewelry.make sets); Steel Casting Table and Jeweler's Bench (block models turned by facing,
  loot, recipes, mineable tags); lang. equipment/ring and equipment/neck tags: gen_equipment.py.
Textures: scripts/gen_p8_textures.py (items, block faces), scripts/gen_equipment_textures.py (the pack screens).
Generator order: ... gen_firepit.py, gen_kettle.py, gen_equipment.py, gen_p8smith.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py.
Run: python scripts/gen_p8smith.py"""
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


def flat(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + f"item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}"}})


for item in ("tool_loops", "steel_frame_pack", "gold_ring_blank", "gold_chain"):
    flat(item)

# the set pieces: gold layer as is, gem layer tinted with the gem's colour (custom_model_data colour 0; white if unset)
for item in ("gold_ring", "gold_necklace"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated",
                                       "textures": {"layer0": M + f"item/{item}", "layer1": M + f"item/{item}_gem"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{item}", "tints": [
        {"type": "minecraft:constant", "value": -1}, {"type": "minecraft:custom_model_data", "index": 0, "default": -1}]}})

# tools the satchel's loops hold
w(D / "mechanical/tags/item/satchel_tools.json", {"values": [
    "#mechanical:knives", "#mechanical:saws", "#mechanical:tongs", "#mechanical:needles",
    M + "bronze_chisel", M + "iron_chisel", M + "steel_chisel",
    M + "flint_sickle", M + "bronze_sickle", M + "iron_sickle", M + "steel_sickle",
    M + "bronze_smithing_hammer", M + "iron_smithing_hammer", M + "steel_smithing_hammer",
    M + "bronze_quarry_hammer", M + "iron_quarry_hammer", M + "steel_quarry_hammer",
    M + "bronze_shears", "minecraft:shears", M + "fire_drill", "minecraft:flint_and_steel"]})


# ---------------------------------------------------------------- the two work tables
def box(frm, to, tex, faces=("north", "south", "east", "west", "up", "down")):
    out = {}
    for f in faces:
        if f in ("up", "down"):
            uv = [frm[0], frm[2], to[0], to[2]]
        elif f in ("north", "south"):
            uv = [frm[0], 16 - to[1], to[0], 16 - frm[1]]
        else:
            uv = [frm[2], 16 - to[1], to[2], 16 - frm[1]]
        out[f] = {"uv": uv, "texture": tex[f] if isinstance(tex, dict) else tex}
    return {"from": frm, "to": to, "faces": out}


def table(name, top, side, legs, extra=()):
    """A work top 14 px high on four legs (or a closed front), the top face its own texture."""
    t = {f: "#side" for f in ("north", "south", "east", "west")} | {"up": "#top", "down": "#side"}
    els = [box([0, 11, 0], [16, 14, 16], t)]
    if legs:
        for x, z in ((0, 0), (13, 0), (0, 13), (13, 13)):
            els.append(box([x, 0, z], [x + 3, 11, z + 3], "#side"))
        els.append(box([1, 3, 1], [15, 4, 15], "#side", faces=("up", "down", "north", "south", "east", "west")))  # a shelf
    else:
        els.append(box([1, 0, 1], [15, 11, 15], "#side"))
    els += list(extra)
    w(A / f"models/block/{name}.json", {"parent": "minecraft:block/block", "textures": {"particle": B + side, "top": B + top, "side": B + side},
                                        "elements": els})
    w(A / f"blockstates/{name}.json", {"variants": {f"facing={f}": {"model": B + name, **({"y": y} if y else {})}
                                                    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": B + name}})
    w(D / f"mechanical/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{
        "rolls": 1, "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + name}]}],
        "random_sequence": f"mechanical:blocks/{name}"})


# casting table: steel legs and a crucible lip at the back (towards the kiln) to pour from
table("steel_casting_table", "steel_casting_table_top", "steel_casting_table_side", True,
      [box([5, 14, 12], [11, 16, 15], "#side")])
# jeweler's bench: closed front with a drawer, a little bench vise at the back corner
table("jewelers_bench", "jewelers_bench_top", "jewelers_bench_side", False,
      [box([11, 14, 11], [14, 16, 14], "#side")])
tag_add(D / "minecraft/tags/block/mineable/pickaxe.json", M + "steel_casting_table")
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "jewelers_bench")


def shaped(name, pattern, key, result):
    w(D / f"mechanical/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": pattern,
                                             "key": key, "result": {"id": result}})


shaped("steel_casting_table", ["PPP", "BCB", "B B"], {"P": M + "steel_plate", "B": M + "fire_brick", "C": M + "cementation_pot"},
       M + "steel_casting_table")
shaped("jewelers_bench", ["SC ", "WWW", "W W"], {"S": M + "steel_plate", "C": "#mechanical:needles", "W": "#minecraft:planks"},
       M + "jewelers_bench")

merge(A / "lang/en_us.json", {
    "item.mechanical.tool_loops": "Tool Loops",
    "item.mechanical.steel_frame_pack": "Steel-Frame Pack",
    "item.mechanical.gold_ring_blank": "Gold Ring Blank",
    "item.mechanical.gold_chain": "Gold Chain",
    "item.mechanical.gold_ring": "Gold Ring",
    "item.mechanical.gold_ring.with": "Gold Ring with %s",
    "item.mechanical.gold_necklace": "Gold Necklace",
    "item.mechanical.gold_necklace.with": "Gold Necklace with %s",
    "block.mechanical.steel_casting_table": "Steel Casting Table",
    "block.mechanical.jewelers_bench": "Jeweler's Bench",
    "message.mechanical.casting.full": "The casting table holds %s Gold Ingots",
    "message.mechanical.casting.needs_kiln": "The gold needs a Clay Kiln at %s° beside the table to melt",
    "message.mechanical.casting.mode": "Mould: %2$s × %1$s",
    "message.mechanical.jewelers_bench.needs": "Lay a Gold Ring Blank or Gold Chain and a cut gem on the bench, then set it with a chisel",
    "message.mechanical.jewelers_bench.occupied": "There is already one on the bench",
})
print("p8smith ok")
