"""Stone-age watercraft data (Watercraft.java, DugoutBlankBlock.java): Log Raft, Dugout Canoe, Paddle and the Dugout Blank.

- Log Raft: crafting table, 6 logs (Rough Logs or any log that burns) lashed with 3 Cordage.
- Paddle: a Rough Log carved with a knife (the knife stays, losing 1).
- Dugout Canoe: no recipe. A knife scores a log lying on its side (#mechanical:dugout_logs, or a fallen log with or without
  bark) into a Dugout Blank, which is hewn / burnt / hewn / burnt / hewn / smoothed in the world. Its block models here
  mirror DugoutBlankBlock.DEPTH.
- Vanilla boats, chest boats and bamboo rafts are switched off in gen_age_recipes.py.
Textures: scripts/gen_watercraft_textures.py.
Generator order: ... gen_soot.py, gen_phantom.py, gen_watercraft.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_watercraft.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"


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


# ---- items ----
ITEMS = {"log_raft": "Log Raft", "dugout_canoe": "Dugout Canoe", "paddle": "Paddle"}
for item in ITEMS:
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/handheld" if item == "paddle" else "minecraft:item/generated",
                                       "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# every wood's Rough Log (gen_data.py writes their item files, so new woods are picked up here)
rough_logs = sorted(f"mechanical:{p.stem}" for p in (A / "items").glob("*_rough_log.json"))
assert rough_logs, "run gen_data.py first"
w(D / "mechanical/tags/item/rough_logs.json", {"values": rough_logs})
w(D / "mechanical/tags/item/raft_logs.json", {"values": ["#mechanical:rough_logs", "#minecraft:logs_that_burn"]})
w(D / "mechanical/tags/block/dugout_logs.json", {"values": ["#minecraft:logs_that_burn"]})
w(D / "mechanical/tags/item/dugout_fire_starters.json", {"values": ["minecraft:torch", "minecraft:soul_torch", "minecraft:flint_and_steel",
                                                                    "mechanical:fire_drill"]})

w(D / "mechanical/recipe/log_raft.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["LCL", "LCL", "LCL"], "key": {"L": "#mechanical:raft_logs", "C": "mechanical:cordage"}, "result": {"id": "mechanical:log_raft"}})
w(D / "mechanical/recipe/paddle.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["#mechanical:rough_logs", "#mechanical:knives"], "result": {"id": "mechanical:paddle", "count": 1}})
unlock("log_raft", "mechanical:cordage", ["log_raft"])
unlock("paddle", "#mechanical:rough_logs", ["paddle"])

# ---- Dugout Blank: built along z; the hollow is DEPTH px deep, walls 3 px (2 when carved), solid ends 2 px ----
DEPTH = [0, 3, 3, 7, 7, 11]
INSIDE = ["hollow", "hollow", "charred", "hollow", "charred", "hollow"]
T = lambda n: f"mechanical:block/dugout_blank_{n}"


def faces(**kw):
    """Faces from texture keys; north/south are the log's ends and cull against neighbours, as does the bottom."""
    out = {}
    for f, tex in kw.items():
        out[f] = {"texture": f"#{tex}"}
        if f in ("down",):
            out[f]["cullface"] = "down"
    return out


for stage, d in enumerate(DEPTH):
    textures = {"particle": T("side"), "side": T("side"), "top": T("top"), "end": T("end"), "inside": T(INSIDE[stage])}
    if d == 0:
        textures["top"] = T("scored")
        elements = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": faces(down="side", up="top", north="end", south="end", east="side", west="side")}]
    else:
        wall, floor = (2 if stage == 5 else 3), 16 - d
        elements = [
            {"from": [0, 0, 0], "to": [16, floor, 16], "faces": faces(down="side", up="inside", north="end", south="end", east="side", west="side")},
            {"from": [0, floor, 0], "to": [wall, 16, 16], "faces": faces(up="top", north="end", south="end", east="inside", west="side")},
            {"from": [16 - wall, floor, 0], "to": [16, 16, 16], "faces": faces(up="top", north="end", south="end", east="side", west="inside")},
            {"from": [wall, floor, 0], "to": [16 - wall, 16, 2], "faces": faces(up="top", north="end", south="inside")},
            {"from": [wall, floor, 14], "to": [16 - wall, 16, 16], "faces": faces(up="top", north="inside", south="end")},
        ]
    w(A / f"models/block/dugout_blank_{stage}.json", {"textures": textures, "elements": elements})
w(A / "blockstates/dugout_blank.json", {"variants": {
    f"axis={axis},burning={b},stage={s}": ({"model": f"mechanical:block/dugout_blank_{s}"} | ({"y": 90} if axis == "x" else {}))
    for axis in ("x", "z") for b in ("false", "true") for s in range(6)}})
w(D / "mechanical/loot_table/blocks/dugout_blank.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{
    "type": "minecraft:item", "name": "minecraft:stick",
    "modifier": [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}}]}]}],
    "random_sequence": "mechanical:blocks/dugout_blank"})
tag_add(D / "minecraft/tags/block/mineable/axe.json", "mechanical:dugout_blank")

merge(A / "lang/en_us.json", {f"item.mechanical.{k}": v for k, v in ITEMS.items()} | {
    "block.mechanical.dugout_blank": "Dugout Blank",
    "entity.mechanical.log_raft": "Log Raft",
    "entity.mechanical.dugout_canoe": "Dugout Canoe",
    "message.mechanical.dugout.burning": "Let the fire burn out first",
    "message.mechanical.dugout.needs_fire": "Burn the hewn wood first (torch or fire drill)",
    "message.mechanical.dugout.needs_axe": "Hew it out with an axe first",
    "message.mechanical.dugout.needs_knife": "Smooth the hollow with a knife",
})
print("watercraft ok")
