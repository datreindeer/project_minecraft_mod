"""Item models for every mod block that has no item model yet (Mechanical.registerMissingBlockItems gives them items).
Plants drawn as a cross, and flat ground decals, become flat item pictures (like vanilla flowers); everything else
shows its block model. Run last, after the other generators: python scripts/gen_block_items.py"""
import json
from pathlib import Path

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"
NO_ITEM = {"blood"}
FLAT_PARENTS = ("block/cross", "block/tinted_cross", "block/flower_pot_cross", "mechanical:block/shrub")


def first_model(node):
    if isinstance(node, dict):
        if "model" in node:
            return node["model"]
        for v in node.values():
            m = first_model(v)
            if m:
                return m
    if isinstance(node, list):
        for v in node:
            m = first_model(v)
            if m:
                return m
    return None


def read_model(ref):
    ns, path = ref.split(":", 1) if ":" in ref else ("minecraft", ref)
    f = A.parent / ns / "models" / f"{path}.json"
    return json.loads(f.read_text()) if f.exists() else None


def item_model(block):
    bs = json.loads((A / "blockstates" / f"{block}.json").read_text())
    ref = first_model(bs.get("variants") or bs.get("multipart"))
    if not ref:
        return None
    model = read_model(ref) or {}
    parent = model.get("parent", "")
    textures = model.get("textures", {})
    flat_decal = any(e.get("to", [0, 2, 0])[1] - e.get("from", [0, 0, 0])[1] < 0.5 for e in model.get("elements", [])) and model.get("elements")
    tex = textures.get("cross") or textures.get("texture") or (next(iter(v for k, v in textures.items() if k != "particle"), None))
    if tex and (parent.endswith(FLAT_PARENTS) or parent in FLAT_PARENTS or flat_decal):
        # a flat picture of the plant / decal, like vanilla flowers
        name = f"mechanical:item/{block}"
        (A / "models/item").mkdir(parents=True, exist_ok=True)
        (A / "models/item" / f"{block}.json").write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": tex}}, indent=2) + "\n")
        return {"model": {"type": "minecraft:model", "model": name}}
    return {"model": {"type": "minecraft:model", "model": ref}}


def main():
    made = []
    for f in sorted((A / "blockstates").glob("*.json")):
        block = f.stem
        if block in NO_ITEM or block.startswith("potted_") or (A / "items" / f"{block}.json").exists():
            continue
        definition = item_model(block)
        if definition is None:
            print(f"  no model for {block}")
            continue
        (A / "items" / f"{block}.json").write_text(json.dumps(definition, indent=2) + "\n")
        made.append(block)
    print(f"block items ok ({len(made)} new): " + ", ".join(made))


if __name__ == "__main__":
    main()
