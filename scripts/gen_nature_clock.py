"""Data for the nature clock (docs/terrain-plan.md phases 5-7, worldgen/NatureClock).

In 26.x the live heightmaps (MOTION_BLOCKING, MOTION_BLOCKING_NO_LEAVES, OCEAN_FLOOR) count only blocks in the tag
#minecraft:blocks_motion_in_heightmap(_no_leaves), which vanilla fills through #blocks_motion_no_leaves (logs, dirt,
sand...). Mod blocks outside those tags are invisible to the heightmaps: the clock (and rain, snow, spawning) would read
the ground under them. This adds the full blocks the clock lays down or reads as ground: Burnt Soil, Wood Ash, Charred
Log, Deadwood, Broken Deadwood and the Grub Log.

Generator order: anywhere after gen_deadwood.py and gen_elkwight.py; put it just before gen_advancements.py.
Run: python scripts/gen_nature_clock.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TAG = ROOT / "src/main/resources/data/minecraft/tags/block/blocks_motion_no_leaves.json"
BLOCKS = ["burnt_soil", "wood_ash", "charred_log", "deadwood", "broken_deadwood", "grub_log"]

TAG.parent.mkdir(parents=True, exist_ok=True)
TAG.write_text(json.dumps({"replace": False, "values": [f"mechanical:{b}" for b in BLOCKS]}, indent=2) + "\n", encoding="utf-8")
print(f"{TAG.relative_to(ROOT)}: {len(BLOCKS)} blocks")
