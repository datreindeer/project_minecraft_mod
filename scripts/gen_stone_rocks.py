"""Loose rocks of the mod's natural stones (living-world plan F6; StoneFamilies.ROCK_STONES has the same list).

Each stone gets what Loose Granite has (gen_data.py): loose_<stone> (RockBlock, sizes 1-3) on the stone's own texture,
dropping <stone>_chunk (1 / 2 / 3-4 by size; Pumice drops 1 Pumice Chunk), the chunk in #mechanical:rock_chunks (knapped
stone heads, Fire Pit, Kiln ...), 4 chunks -> the stone block, and surface scatter where the stone comes up: on the stone
itself, or on grass/dirt/sand/gravel with the stone 1-3 blocks under it. The chunk texture is Stone Rock's shape recoloured
with the stone's own block texture (its brightness ranks mapped onto the stone's colours).

Generator order: after gen_stone_families.py (needs its textures and lang), before gen_block_items.py, gen_unlocks.py,
gen_advancements.py and gen_loot_format.py. Run: python scripts/gen_stone_rocks.py
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
A, D = ROOT / "src/main/resources/assets/mechanical", ROOT / "src/main/resources/data"
M = "mechanical:"

STONES = ["limestone", "slate", "gneiss", "soapstone", "shale", "conglomerate", "dolomite", "travertine", "shell_bank",
                         "marble", "quartzite", "red_porphyry", "green_porphyry", "rapakivi_granite", "coastal_basalt", "pumice",
                         "scoria", "serpentine", "gypsum", "alabaster", "alum_shale"]
# the block texture where it isn't the stone's name (a side texture), and the block 4 chunks make when not the stone
TEXTURE = {"marble": "marble_side", "coastal_basalt": "coastal_basalt_side", "scoria": "scoria_side", "alabaster": "alabaster_side"}
MAKES = {"slate": "cobbled_slate"}  # like the stone's own drop
SOIL = ["minecraft:grass_block", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:podzol", "minecraft:sand", "minecraft:red_sand",
        "minecraft:gravel", "minecraft:snow_block"]


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def r(path):
    return json.loads(path.read_text(encoding="utf-8"))


def tag_add(rel, values):
    path = D / rel
    tag = r(path) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def chunk_texture(stone):
    """Stone Rock's pixels, each brightness rank replaced by the colour at the same rank in the stone's texture."""
    shape = Image.open(A / "textures/item/stone_chunk.png").convert("RGBA")
    src = Image.open(A / f"textures/block/{TEXTURE.get(stone, stone)}.png").convert("RGBA")
    lum = lambda c: c[0] * 299 + c[1] * 587 + c[2] * 114
    colours = sorted((c for c in src.get_flattened_data() if c[3] > 0), key=lum)
    pixels = [(x, y) for y in range(shape.height) for x in range(shape.width) if shape.getpixel((x, y))[3] > 0]
    ranked = sorted(set(lum(shape.getpixel(p)) for p in pixels))
    out = Image.new("RGBA", shape.size)
    for p in pixels:
        c = shape.getpixel(p)
        q = ranked.index(lum(c)) / max(1, len(ranked) - 1)
        out.putpixel(p, colours[round(q * (len(colours) - 1))][:3] + (c[3],))
    return out


lang_path = A / "lang/en_us.json"
lang = r(lang_path)
tri = lambda m: {"type": "minecraft:trapezoid", "min": -m, "max": m, "plateau": 0}
for stone in STONES:
    block, tex = f"loose_{stone}", f"{M}block/{TEXTURE.get(stone, stone)}"
    drop = M + ("pumice_chunk" if stone == "pumice" else f"{stone}_chunk")
    variants = {}
    for size in (1, 2, 3):
        w(A / f"models/block/{block}_size{size}.json", {"parent": f"{M}block/rock_size{size}", "textures": {"all": tex}})
        variants[f"size={size}"] = [{"model": f"{M}block/{block}_size{size}", "y": y} for y in (0, 90, 180, 270)]
    w(A / f"blockstates/{block}.json", {"variants": variants})

    def counted(size, count):
        return {"type": "minecraft:set_count", "count": count,
                "condition": {"type": "minecraft:match_block", "blocks": M + block, "state": {"size": str(size)}}}
    entry = {"type": "minecraft:item", "name": drop}
    if stone != "pumice":  # one abrasive Pumice Chunk whatever the size, as crushing a Pumice block gives
        entry["modifier"] = [counted(2, 2), counted(3, {"type": "minecraft:uniform", "min": 3, "max": 4})]
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}],
                                                       "random_sequence": f"mechanical:blocks/{block}"})

    w(D / f"mechanical/worldgen/feature/{block}.json", {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted", "entries": [
        {"data": {"id": M + block, "properties": {"size": str(s)}}, "weight": wt} for s, wt in ((1, 6), (2, 3), (3, 1))]}})
    on = lambda dy, blocks: {"type": "minecraft:matching_blocks", "offset": [0, dy, 0], "blocks": blocks}
    w(D / f"mechanical/worldgen/placed_feature/{block}.json", {"feature": M + block, "placement": [
        {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
        {"type": "minecraft:biome"},
        {"type": "minecraft:count", "count": 6},
        {"type": "minecraft:offset", "x": tri(6), "y": tri(2), "z": tri(6)},
        {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
            {"type": "minecraft:replaceable"},
            {"type": "minecraft:any_of", "predicates": [on(-1, [M + stone]),
                {"type": "minecraft:all_of", "predicates": [on(-1, SOIL), {"type": "minecraft:any_of", "predicates": [
                    on(-2, [M + stone]), on(-3, [M + stone]), on(-4, [M + stone])]}]}]},
            {"type": "minecraft:would_survive", "state": M + block}]}}]})

    if stone != "pumice":
        item = f"{stone}_chunk"
        chunk_texture(stone).save(A / f"textures/item/{item}.png")
        w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{M}item/{item}"}})
        w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"{M}item/{item}"}})
        w(D / f"mechanical/recipe/{stone}_from_chunks.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
            "pattern": ["RR", "RR"], "key": {"R": M + item}, "result": {"id": M + MAKES.get(stone, stone)}})
    name = lang[f"block.mechanical.{stone}"]
    lang[f"block.mechanical.{block}"] = f"Loose {name}"
    if stone != "pumice":
        lang[f"item.mechanical.{stone}_chunk"] = f"{name} Rock"

w(D / "mechanical/neoforge/biome_modifier/add_stone_rocks.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                                  "features": [f"{M}loose_{s}" for s in STONES], "step": "vegetal_decoration"})
tag_add("mechanical/tags/item/rock_chunks.json", [f"{M}{s}_chunk" for s in STONES if s != "pumice"])
tag_add("minecraft/tags/block/mineable/pickaxe.json", [f"{M}loose_{s}" for s in STONES])
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")
print(f"{len(STONES)} loose rocks")
