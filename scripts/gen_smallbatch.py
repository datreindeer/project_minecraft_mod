"""Small batch: Bronze Arrowheads / Bronze Arrow, the Broom, path covers.

- Item models: Bronze Arrowhead, Rough Cast Bronze Arrowheads, Bronze Arrow, Broom (the Clay Arrowheads Mold block and
  its unfired item are written with the other molds in gen_copper.py).
- Tags: minecraft:arrows (bows shoot the Bronze Arrow); mechanical:path_covers (TrampledPaths: thin covers the path
  wears in under and that stay on it: soot, carpets, the rugs, leaf litter).
- Lang.
Textures: gen_bronze_textures.py (arrowheads, arrow, mold), gen_smallbatch_textures.py (broom).
Generator order: ... gen_kettle.py, gen_moldbench.py, gen_smallbatch.py, gen_roots.py, gen_fauna_*, gen_block_items.py,
gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_smallbatch.py"""
import json
from pathlib import Path

R = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def tag_add(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    for v in values:
        if v not in tag["values"]:
            tag["values"].append(v)
    w(path, tag)


# ---- item models ----
for item, parent in (("bronze_arrowhead", "generated"), ("rough_cast_bronze_arrowheads", "generated"),
                     ("bronze_arrow", "generated"), ("broom", "handheld")):
    w(A / f"models/item/{item}.json", {"parent": f"minecraft:item/{parent}", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})

# ---- tags ----
tag_add(D / "minecraft/tags/item/arrows.json", [M + "bronze_arrow"])
RUGS = ["bear_rug", "wolf_rug", "lynx_rug", "leopard_rug", "mammoth_rug", "bison_rug", "musk_ox_rug", "ibex_rug", "saiga_rug",
        "hyena_rug", "cave_bear_rug"]
LITTER = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "yellow_poplar", "orange_poplar",
          "red_poplar", "azalea", "flowering_azalea", "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew", "rowan", "bird_cherry", "wild_apple", "plum", "olive", "date_palm"]  # Mechanical.LITTER_LEAVES
for r in RUGS + [f"{l}_leaf_litter" for l in LITTER]:
    assert (A / f"blockstates/{r}.json").exists(), r
w(D / "mechanical/tags/block/path_covers.json", {"values": [
    M + "soot", "#minecraft:wool_carpets", "minecraft:moss_carpet", "minecraft:pale_moss_carpet", "minecraft:leaf_litter",
    *[M + r for r in RUGS], *[f"{M}{l}_leaf_litter" for l in LITTER]]})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({
    "item.mechanical.bronze_arrowhead": "Bronze Arrowhead",
    "item.mechanical.rough_cast_bronze_arrowheads": "Rough Cast Bronze Arrowheads",
    "item.mechanical.bronze_arrow": "Bronze Arrow",
    "item.mechanical.broom": "Broom",
    "tooltip.mechanical.poor_bronze_arrow": "Poor Bronze: half a point less damage",
})
w(lang_path, lang)
print("smallbatch ok")
