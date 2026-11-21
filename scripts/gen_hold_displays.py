"""Third-person display transforms for items with their own hold pose (client/HoldPoses): buckets hang below the fist,
firewood and rough logs lie across both arms in front of the chest, the kettle is carried in front, the pan lies flat
in both hands, the paddle stands upright at the side. Each transform is solved for its pose's arm angle (see the pose
methods in HoldPoses), so changing one means changing the other. Patches the "display" of models other generators
write, so it runs after them: after gen_block_items.py and gen_held_models.py, before gen_unlocks.py. Items with a 3D
model in hand (gen_held_models.py: "<name>_held", built on the sprite's footprint) get it there, the flat one is GUI-only.
Run: python scripts/gen_hold_displays.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/resources/assets/mechanical/models/item"
WOODS = sorted(p.name[:-len("_rough_log.json")] for p in MODELS.glob("*_rough_log.json"))

# right hand: (rotation, translation, scale). Left hand is mirrored as vanilla writes it (x translation, y/z rotation).
RIGHT = {
    "bucket": ([-90.0, 0.0, -180.0], [0.0, -2.0, -3.5], 0.55),      # arm hanging, swinging with the steps
    "log": ([-141.6, 22.9, 135.0], [-3.53, -1.65, -0.1], 0.65),     # arms -0.9 forward, -0.4 in
    "kettle": ([-147.3, 25.8, -180.0], [-2.24, -3.49, -1.57], 0.6),  # arms -1.0 forward, -0.45 in
    "pan": ([-54.4, 0.0, -22.9], [-2.49, -1.87, -1.36], 0.7),       # arms -0.95 forward, -0.4 in; flat
    "paddle": ([49.9, 78.5, 45.0], [1.17, -4.1, -1.31], 1.2),       # arm -0.7 forward, -0.2 in; blade down at the side
}
ITEMS = {
    "bucket": ["clay_bucket", "clay_water_bucket", "bronze_bucket", "bronze_water_bucket", "bronze_lava_bucket", "bronze_milk_bucket"],
    "log": [f"{w}_rough_log" for w in WOODS] + [f"{w}_firewood_{n}" for w in WOODS for n in (1, 2, 3)],
    "kettle": ["bronze_kettle"],
    "pan": ["pan", "pan_with_gravel", "pan_with_sand"],
    "paddle": ["paddle"],
}


def display(kind):
    rot, tr, s = RIGHT[kind]
    return {"thirdperson_righthand": {"rotation": rot, "translation": tr, "scale": [s, s, s]},
            "thirdperson_lefthand": {"rotation": [rot[0], -rot[1], -rot[2]], "translation": [-tr[0], tr[1], tr[2]], "scale": [s, s, s]}}


patched = 0
for kind, names in ITEMS.items():
    for name in names:
        path = MODELS / f"{name}_held.json"
        if not path.exists():
            path = MODELS / f"{name}.json"
        assert path.exists(), path
        model = json.loads(path.read_text(encoding="utf-8"))
        model["display"] = {**model.get("display", {}), **display(kind)}
        path.write_text(json.dumps(model, indent=2) + "\n", encoding="utf-8")
        patched += 1
assert WOODS and patched == sum(len(n) for n in ITEMS.values())
print(f"{patched} item models: hold displays")
