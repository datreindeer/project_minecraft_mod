"""Data for the worn trails between structures (com.mechanical.worldgen.Trails). Run: python scripts/gen_trails.py
Structures listed in the tag mechanical:trail_connected get trails to their nearest neighbours (random_spread sets only)."""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data/mechanical"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


CONNECTED = ["#minecraft:village", "mechanical:ancient_oak", "mechanical:tribe_village"]

w(D / "tags/worldgen/structure/trail_connected.json", {"replace": False, "values": CONNECTED})
w(D / "worldgen/feature/trail.json", {"type": "mechanical:trail"})
# no placement modifiers: runs once per chunk at its corner, the feature finds its own trail columns
w(D / "worldgen/placed_feature/trail.json", {"feature": "mechanical:trail", "placement": []})
w(D / "neoforge/biome_modifier/add_trails.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
    "features": "mechanical:trail", "step": "top_layer_modification"})
print("trails ok")
