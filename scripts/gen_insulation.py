"""Shelter warmth of blocks (Temperature.shelter): block tags mechanical:insulation/high, /medium, /poor.
Any other solid block counts as stone (Temperature.STONE_WARMTH). Bark roofs are found from their blockstate files.
Run after gen_earth.py and gen_bark_roof.py: python scripts/gen_insulation.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
OUT = ROOT / "data/mechanical/tags/block/insulation"
M, V = "mechanical:", "minecraft:"


def family(base, *parts):
    return [M + base] + [M + base.removesuffix("s") + p for p in parts]


bark_roofs = sorted(M + p.stem for p in (ROOT / "assets/mechanical/blockstates").glob("*_roof.json")
                    if not p.stem.endswith("tiled_roof"))  # fired roof tiles (gen_masonry.py) count as stone
HIGH = (
    # earth, turf and thatch: the old ways of keeping a house warm
    [M + b for b in ("turf", "turf_slab", "turf_stairs", "peat", "mudstone", "wattle_and_daub", "framed_wattle_and_daub",
                     "reed_thatch", "reed_thatch_slab", "reed_thatch_stairs")]
    + family("peat_bricks", "_slab", "_stairs", "_wall") + family("adobe_bricks", "_slab", "_stairs", "_wall")
    + [M + "rammed_earth", M + "rammed_earth_slab", M + "rammed_earth_stairs", M + "rammed_earth_wall"]
    + bark_roofs
    + [V + "snow_block", V + "packed_mud", V + "mud_bricks", V + "mud_brick_slab", V + "mud_brick_stairs", V + "mud_brick_wall",
       V + "hay_block", "#minecraft:wool"])
MEDIUM = ["#minecraft:logs", "#minecraft:planks", "#minecraft:wooden_slabs", "#minecraft:wooden_stairs", "#minecraft:wooden_doors",
          "#minecraft:wooden_trapdoors", M + "loess"] + family("loess_bricks", "_slab", "_stairs", "_wall")
POOR = ["#minecraft:impermeable", "#minecraft:leaves", V + "ice", V + "packed_ice", V + "blue_ice", V + "iron_bars", M + "wattle"]

lang = json.loads((ROOT / "assets/mechanical/lang/en_us.json").read_text(encoding="utf-8"))
for name, values in (("high", HIGH), ("medium", MEDIUM), ("poor", POOR)):
    for v in values:  # every mod block must exist (catches renamed blocks)
        assert not v.startswith(M) or f"block.{v.replace(':', '.')}" in lang, v
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / f"{name}.json").write_text(json.dumps({"values": values}, indent=2) + "\n", encoding="utf-8")
print("insulation ok:", len(HIGH), len(MEDIUM), len(POOR))
