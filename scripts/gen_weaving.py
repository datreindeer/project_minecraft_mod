"""Iron Age weaving data (Weaving.java, StrawBedRest.java; docs/ages/03-iron-age.md "Vävstol och Cloth Bed").

- items: Silk Thread, Wool Cloth, Linen Cloth, Silk Cloth (models, item definitions, lang)
- Warp-weighted Loom: blockstate (facing), model (frame, warp, clay weights), loot, mineable/axe,
  recipe [Log][Iron Tool Rod][Log] / [Log][ ][Log] / [Clay Ball]x3, unlocked by an Iron Tool Rod
- Cloth Bed: the vanilla straw bed models retextured, loot (the bed itself), recipe [Cloth]x3 / [Straw]x3 / [Planks]x3
- tags: mechanical:cloth (any cloth, for the bed), Silk Thread into mechanical:threads
Textures: scripts/gen_weaving_textures.py. Advancements: gen_advancements.py (iron_age tab, "warp_weighted_loom" ...).
Generator order: ... gen_plough.py, gen_weaving.py, gen_equipment.py ...
Run: python scripts/gen_weaving.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def unlock(name, item, recipes):
    w(D / f"mechanical/advancement/recipes/{name}.json", {"parent": "minecraft:recipes/root",
        "criteria": {"has_item": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": item}]}}},
        "requirements": [["has_item"]], "rewards": {"recipes": [f"mechanical:{r}" for r in recipes]}})


def loot_self(block, head_only=False):
    entry = {"type": "minecraft:item", "name": M + block}
    if head_only:  # a bed is two blocks: only the head drops it
        entry["condition"] = {"type": "minecraft:match_block", "blocks": M + block, "state": {"part": "head"}}
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [entry]}], "random_sequence": f"mechanical:blocks/{block}"})


# ---- items ----
for name in ("silk_thread", "wool_cloth", "linen_cloth", "silk_cloth"):
    w(A / f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{name}"}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{name}"}})
w(D / "mechanical/tags/item/cloth.json", {"values": [M + "wool_cloth", M + "linen_cloth", M + "silk_cloth"]})
tag_add(D / "mechanical/tags/item/threads.json", M + "silk_thread")

# ---- Warp-weighted Loom: two posts and a beam, the warp hanging between, clay weights at the bottom ----
LOG, LOG_TOP = "minecraft:block/stripped_spruce_log", "minecraft:block/stripped_spruce_log_top"


def cube(frm, to, tex="#wood", top="#wood_top"):
    faces = {d: {"texture": tex} for d in ("north", "south", "east", "west")}
    faces |= {"up": {"texture": top}, "down": {"texture": top}}
    return {"from": frm, "to": to, "faces": faces}


loom = [cube([0, 0, 7], [2, 16, 9]), cube([14, 0, 7], [16, 16, 9]),
        cube([2, 13, 6.5], [14, 15, 9.5]),                       # the cloth beam the woven cloth rolls onto
        cube([2, 7, 6], [14, 8, 7]),                             # the heddle rod, in front of the warp
        {"from": [2, 2.5, 8], "to": [14, 13, 8], "faces": {      # the warp itself, seen from both sides
            "north": {"uv": [2, 2.5, 14, 13], "texture": "#warp"}, "south": {"uv": [14, 2.5, 2, 13], "texture": "#warp"}}}]
loom += [cube([x, 0.5, 7.25], [x + 1.5, 2.5, 8.75], "#weight", "#weight") for x in (2.5, 4.5, 6.5, 8.5, 10.5, 12)]
w(A / "models/block/warp_weighted_loom.json", {"parent": "minecraft:block/block",
    "textures": {"wood": LOG, "wood_top": LOG_TOP, "warp": M + "block/warp_weighted_loom_warp",
                 "weight": "minecraft:block/terracotta", "particle": LOG}, "elements": loom})
w(A / "blockstates/warp_weighted_loom.json", {"variants": {
    f"facing={f}": {"model": M + "block/warp_weighted_loom", **({"y": y} if y else {})}
    for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
w(A / "items/warp_weighted_loom.json", {"model": {"type": "minecraft:model", "model": M + "block/warp_weighted_loom"}})
loot_self("warp_weighted_loom")
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "warp_weighted_loom")
w(D / "mechanical/recipe/warp_weighted_loom.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["LRL", "L L", "BBB"],
    "key": {"L": "#minecraft:logs", "R": M + "iron_tool_rod", "B": "minecraft:clay_ball"},
    "result": {"id": M + "warp_weighted_loom"}})
unlock("warp_weighted_loom", M + "iron_tool_rod", ["warp_weighted_loom"])

# ---- Cloth Bed: vanilla's straw bed shape, a linen mattress under a wool blanket ----
for part in ("foot", "head"):
    w(A / f"models/block/cloth_bed_{part}.json", {"parent": f"minecraft:block/straw_bed_{part}",
        "textures": {"all": M + "block/cloth_bed", "particle": M + "block/cloth_bed_particle"}})
w(A / "blockstates/cloth_bed.json", {"variants": {
    f"facing={f},part={p}": {"model": M + f"block/cloth_bed_{p}", **({"y": y} if y else {})}
    for f, y in (("north", 180), ("east", 270), ("south", 0), ("west", 90)) for p in ("foot", "head")}})
# the item: both halves side by side, as vanilla's straw bed item
SPIN = {"left_rotation": [0.0, 1.0, 0.0, -0.0], "right_rotation": [0.0, 0.0, 0.0, 1.0], "scale": [1.0, 1.0, 1.0]}
w(A / "items/cloth_bed.json", {"model": {"type": "minecraft:composite", "models": [
    {"type": "minecraft:model", "model": M + "block/cloth_bed_head", "transformation": SPIN | {"translation": [1.0, 0.0, 1.0]}},
    {"type": "minecraft:model", "model": M + "block/cloth_bed_foot", "transformation": SPIN | {"translation": [1.0, 0.0, 2.0]}}]}})
loot_self("cloth_bed", head_only=True)
w(D / "mechanical/recipe/cloth_bed.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["CCC", "SSS", "PPP"],
    "key": {"C": "#mechanical:cloth", "S": M + "straw", "P": "#minecraft:planks"},
    "result": {"id": M + "cloth_bed"}})
unlock("cloth_bed", "#mechanical:cloth", ["cloth_bed"])

merge(A / "lang/en_us.json", {
    "item.mechanical.silk_thread": "Silk Thread",
    "item.mechanical.wool_cloth": "Wool Cloth",
    "item.mechanical.linen_cloth": "Linen Cloth",
    "item.mechanical.silk_cloth": "Silk Cloth",
    "block.mechanical.warp_weighted_loom": "Warp-weighted Loom",
    "block.mechanical.cloth_bed": "Cloth Bed",
    "tag.item.mechanical.cloth": "Cloth (any kind)",
    "message.mechanical.loom.how": "Right-click with %s Wool, Linen or Silk Thread to weave a Cloth",
    "message.mechanical.loom.more": "Weaving takes %s thread",
    "message.mechanical.cloth_bed_rest": "You rest well under the woollen blanket.",
})
print("weaving ok")
