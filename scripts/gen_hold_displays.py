"""Third-person display transforms for items with their own hold pose (client/HoldPoses): the pan lies flat in both
hands, the paddle stands upright at the side. Each transform is solved for its pose's arm angle (see the pose methods in
HoldPoses), so changing one means changing the other. Loads carried in both arms (buckets, crucibles, logs, firewood,
the kettle) and hammers on the shoulder are drawn by HoldPoses.LoadLayer, not from the hand, so their models carry no
third-person transform (CLEARED strips the ones earlier versions wrote). Patches the "display" of models other generators
write, so it runs after them: after gen_block_items.py and gen_held_models.py, before gen_unlocks.py.
Run: python scripts/gen_hold_displays.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/resources/assets/mechanical/models/item"
WOODS = sorted(p.name[:-len("_rough_log.json")] for p in MODELS.glob("*_rough_log.json"))

# right hand: (rotation, translation, scale). Left hand is mirrored as vanilla writes it (x translation, y/z rotation).
RIGHT = {
    "pan": ([-54.4, 0.0, -22.9], [-2.49, -1.87, -1.36], 0.7),       # arms -0.95 forward, -0.4 in; flat
    "paddle": ([49.9, 78.5, 45.0], [1.17, -4.1, -1.31], 1.2),       # arm -0.7 forward, -0.2 in; blade down at the side
}
ITEMS = {
    "pan": ["pan", "pan_with_gravel", "pan_with_sand"],
    "paddle": ["paddle"],
}
CLEARED = ["clay_bucket", "clay_water_bucket", "bronze_bucket", "bronze_water_bucket", "bronze_lava_bucket", "bronze_milk_bucket",
           "bronze_kettle"] + [f"{w}_rough_log" for w in WOODS] + [f"{w}_firewood_{n}" for w in WOODS for n in (1, 2, 3)]


def display(kind):
    rot, tr, s = RIGHT[kind]
    return {"thirdperson_righthand": {"rotation": rot, "translation": tr, "scale": [s, s, s]},
            "thirdperson_lefthand": {"rotation": [rot[0], -rot[1], -rot[2]], "translation": [-tr[0], tr[1], tr[2]], "scale": [s, s, s]}}


def patch(name, change):
    path = MODELS / f"{name}.json"
    assert path.exists(), path
    model = json.loads(path.read_text(encoding="utf-8"))
    d = change(dict(model.get("display", {})))
    if d:
        model["display"] = d
    else:
        model.pop("display", None)
    path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")


patched = 0
for kind, names in ITEMS.items():
    for name in names:
        patch(name, lambda d: {**d, **display(kind)})
        patched += 1
for name in CLEARED:
    patch(name, lambda d: {k: v for k, v in d.items() if not k.startswith("thirdperson")})
assert WOODS and patched == sum(len(n) for n in ITEMS.values())
print(f"{patched} item models: hold displays, {len(CLEARED)} cleared")
