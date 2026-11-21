"""Loot tables in the 26.x format: rewrites any written the old way, so their conditions and functions take effect.

26.x reads a condition as {"type": ..., ...} under "condition" and a function as {"type": ...} under "modifier"; the old
keys ("conditions": [{"condition": ...}], "functions": [{"function": ...}]) are silently ignored, so a table written the
old way drops its items with none of its conditions (e.g. a Hanging Rack dropped one rack per part). Old
block_state_property ({"block", "properties"}) becomes match_block ({"blocks", "state"}); several conditions go under
all_of. Safe to run any number of times; fails on keys it doesn't know how to convert.

Run near the end of the chain (after every generator that writes loot tables, before gen_registry.py):
python scripts/gen_loot_format.py
"""
import json
from pathlib import Path

DATA = Path(__file__).resolve().parent.parent / "src/main/resources/data"


def condition(c):
    if isinstance(c, str):  # a predicate reference, e.g. "minecraft:tool/can_shear"
        return c
    c = dict(c)
    kind = c.pop("condition", None) or c.pop("type")
    out = {"type": kind}
    if kind == "minecraft:block_state_property":
        out = {"type": "minecraft:match_block", "blocks": c.pop("block")}
        if "properties" in c:
            out["state"] = c.pop("properties")
    if kind == "minecraft:alternative":
        out["type"] = "minecraft:any_of"
    if "term" in c:
        out["term"] = condition(c.pop("term"))
    if "terms" in c:
        out["terms"] = [condition(t) for t in c.pop("terms")]
    out.update(c)
    return out


def conditions(old):
    terms = [condition(c) for c in old]
    return terms[0] if len(terms) == 1 else {"type": "minecraft:all_of", "terms": terms}


def function(f):
    f = dict(f)
    out = {"type": f.pop("function", None) or f.pop("type")}
    if "conditions" in f:
        out["condition"] = conditions(f.pop("conditions"))
    out.update(f)
    return out


def walk(node):
    if isinstance(node, list):
        return [walk(n) for n in node]
    if not isinstance(node, dict):
        return node
    out = {}
    for key, value in node.items():
        if key == "conditions":
            assert "condition" not in node, node
            out["condition"] = conditions(value)
        elif key == "functions":
            assert "modifier" not in node, node
            fs = [function(f) for f in value]
            out["modifier"] = fs[0] if len(fs) == 1 else fs
        elif key == "condition" and isinstance(value, dict):
            out[key] = condition(value)
        else:
            out[key] = walk(value)
    return out


changed = 0
for path in sorted(DATA.glob("*/loot_table/**/*.json")):
    old = json.loads(path.read_text(encoding="utf-8"))
    new = walk(old)
    if new != old:
        path.write_text(json.dumps(new, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        changed += 1
text = "\n".join(p.read_text(encoding="utf-8") for p in DATA.glob("*/loot_table/**/*.json"))
assert '"conditions"' not in text and '"functions"' not in text and '"condition": "minecraft:survives' not in text, "old loot format left"
print(f"loot format ok ({changed} rewritten)")
