# Gemstones data from scripts/gem_table.py: models, loot, worldgen, biome tags, block/item tags, lang, registry notes.
# usage: python scripts/gen_gemstones.py   (textures: gen_gemstone_textures.py; then gen_block_items.py, gen_advancements.py)
import json
from pathlib import Path

from gem_table import GEMS, SIZES, blocks_of, ore_block

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / "src/main/resources"
A, D = R / "assets/mechanical", R / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def patch_tag(path, values):
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    w(path, tag)


def drop(name, *modifiers):
    return {"type": "minecraft:item", "name": name, **({"modifier": list(modifiers)} if modifiers else {})}


def silk_or(block, entry):
    return {"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": M + block}, entry]}


def loot(block, entry):
    w(D / f"mechanical/loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [entry]}],
                                                        "random_sequence": f"mechanical:blocks/{block}"})


def count(lo, hi):
    return {"type": "minecraft:set_count", "count": lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}}


FORTUNE = {"type": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"}
DECAY = {"type": "minecraft:explosion_decay"}
HOST_TEXTURE = {"stone": "minecraft:block/stone", "deepslate": "minecraft:block/deepslate", "sandstone": "minecraft:block/sandstone"}
HOST_TARGET = {"stone": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
               "deepslate": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
               "sandstone": {"predicate_type": "minecraft:block_match", "block": "minecraft:sandstone"}}
FACING_ROT = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90},
              "west": {"x": 90, "y": 270}}
AIR = {"type": "minecraft:matching_block_tag", "tag": "minecraft:air"}


def height(lo, hi):
    return {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": lo},
                                                          "max_inclusive": {"absolute": hi}}}


def cave_surface(n, lo, hi, up):
    """Placement on a cave floor (up=False: scan down, place above) or ceiling, never near the open surface."""
    return [{"type": "minecraft:count", "count": n}, {"type": "minecraft:in_square"}, height(lo, hi),
            {"type": "minecraft:environment_scan", "direction_of_search": "up" if up else "down", "max_steps": 12,
             "target_condition": {"type": "minecraft:solid"}, "allowed_search_condition": AIR},
            {"type": "minecraft:offset", "x": 0, "y": -1 if up else 1, "z": 0},
            {"type": "minecraft:surface_relative_threshold_filter", "heightmap": "OCEAN_FLOOR_WG", "max_inclusive": -8},
            {"type": "minecraft:biome"}]


def weighted(entries):
    return {"type": "minecraft:simple_block", "to_place": {"type": "minecraft:weighted", "entries": [
        {"data": {"id": i, "properties": p}, "weight": wt} for i, p, wt in entries]}}


lang = {}
sv_notes, en_notes = {}, {}
pickaxe, bronze, features = [], [], []

for g in GEMS:
    gem, title, kind = g["name"], g["title"], g["kind"]
    lang[f"item.mechanical.{gem}"] = title
    w(A / f"models/item/{gem}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{gem}"}})
    w(A / f"items/{gem}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{gem}"}})
    w(D / f"c/tags/item/gems/{gem}.json", {"values": [M + gem]})
    w(D / f"mechanical/tags/worldgen/biome/has_gem/{gem}.json", {"values": g["biomes"]})
    blocks = blocks_of(g)
    pickaxe += [M + b for b in blocks]
    links = ", ".join(f"[[{b}]]" for b in blocks)
    modifier_step = "underground_ores" if kind == "ore" else "underground_decoration"
    placed = []

    if kind == "ore":
        for host in g["hosts"]:
            b = ore_block(gem, host)
            lang[f"block.mechanical.{b}"] = f"{'Deepslate ' if host == 'deepslate' else ''}{title} Ore"
            w(A / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"mechanical:block/{b}"}})
            w(A / f"blockstates/{b}.json", {"variants": {"": {"model": f"mechanical:block/{b}"}}})
            loot(b, silk_or(b, drop(M + gem, FORTUNE, DECAY)))
            if g.get("tier") == "bronze":
                bronze.append(M + b)
        w(D / f"mechanical/worldgen/feature/gem_{gem}.json", {
            "type": "minecraft:ore", "discard_chance_on_air_exposure": 0.0, "size": g["size"],
            "targets": [{"state": {"id": M + ore_block(gem, h)}, "target": HOST_TARGET[h]} for h in g["hosts"]]})
        w(D / f"mechanical/worldgen/placed_feature/gem_{gem}.json", {"feature": f"mechanical:gem_{gem}", "placement": [
            {"type": "minecraft:count", "count": g["count"]}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:trapezoid", "min_inclusive": {"absolute": g["y"][0]},
                                                          "max_inclusive": {"absolute": g["y"][1]}}},
            {"type": "minecraft:biome"}]})
        placed.append(f"gem_{gem}")
        tool = "**Bronze Pickaxe** eller bättre" if g.get("tier") == "bronze" else "valfri pickaxe"
        tool_en = "a **Bronze Pickaxe** or better" if g.get("tier") == "bronze" else "any pickaxe"
        sv_notes[gem] = {"text": f"Ädelsten ur malm. {g['sv']}. Bryts ur {links} med {tool} (Fortune ger fler).", "category": "materials"}
        en_notes[gem] = {"text": f"Gemstone from ore. {g['en']}. Mined from {links} with {tool_en} (Fortune gives more).", "category": "materials"}
        for b in blocks:
            sv_notes[b] = {"text": f"Malm med [[{gem}]]. {g['sv']}. Kräver {tool}; ger 1 [[{gem}]] (Fortune hjälper), sig själv med Silk Touch.",
                           "category": "blocks"}
            en_notes[b] = {"text": f"Ore holding [[{gem}]]. {g['en']}. Needs {tool_en}; drops 1 [[{gem}]] (Fortune helps), itself with Silk Touch.",
                           "category": "blocks"}

    elif kind == "crystal":
        drops = {"small": [count(1, 1)], "medium": [count(1, 2)], "large": [count(2, 4), FORTUNE]}
        sv_size = {"small": "liten", "medium": "mellanstor", "large": "stor"}
        for s in SIZES:
            b = f"{s}_{gem}_crystal"
            lang[f"block.mechanical.{b}"] = f"{s.title()} {title} Crystal"
            w(A / f"models/block/{b}.json", {"parent": "minecraft:block/cross", "textures": {"cross": f"mechanical:block/{b}"}})
            w(A / f"blockstates/{b}.json", {"variants": {f"facing={f}": {"model": f"mechanical:block/{b}", **r} for f, r in FACING_ROT.items()}})
            loot(b, silk_or(b, drop(M + gem, *drops[s], DECAY)))
            sv_notes[b] = {"text": f"En {sv_size[s]} kristall av [[{gem}]] som växer på grottgolv och grottak. {g['sv']}. "
                                   f"Bryts med pickaxe → {['1', '1–2', '2–4 (Fortune hjälper)'][SIZES.index(s)]} [[{gem}]]; "
                                   f"sig själv med Silk Touch. Lyser svagt.", "category": "blocks"}
            en_notes[b] = {"text": f"A {s} [[{gem}]] crystal growing on cave floors and ceilings. {g['en']}. "
                                   f"Mined with a pickaxe → {['1', '1–2', '2–4 (Fortune helps)'][SIZES.index(s)]} [[{gem}]]; "
                                   f"itself with Silk Touch. Glows faintly.", "category": "blocks"}
        crystals = [(M + f"{s}_{gem}_crystal", wt) for s, wt in zip(SIZES, (6, 3, 1))]
        for side, facing, up, n in (("floor", "up", False, g["count"]), ("ceiling", "down", True, max(1, g["count"] // 2))):
            w(D / f"mechanical/worldgen/feature/gem_{gem}_{side}.json",
              weighted([(i, {"facing": facing, "waterlogged": "false"}, wt) for i, wt in crystals]))
            w(D / f"mechanical/worldgen/placed_feature/gem_{gem}_{side}.json",
              {"feature": f"mechanical:gem_{gem}_{side}", "placement": cave_surface(n, *g["y"], up)})
            placed.append(f"gem_{gem}_{side}")
        sv_notes[gem] = {"text": f"Kristall-ädelsten. {g['sv']}. Växer som små, mellanstora och stora kristaller ({links}).",
                         "category": "materials"}
        en_notes[gem] = {"text": f"Crystal gemstone. {g['en']}. Grows as small, medium and large crystals ({links}).",
                         "category": "materials"}

    else:  # pebble
        b = f"loose_{gem}"
        lang[f"block.mechanical.{b}"] = f"Loose {title}"
        variants = {}
        for size in (1, 2, 3):
            w(A / f"models/block/{b}_size{size}.json", {"parent": f"mechanical:block/rock_size{size}",
                                                       "textures": {"all": f"mechanical:block/{gem}_pebble"}})
            variants[f"size={size}"] = [{"model": f"mechanical:block/{b}_size{size}", "y": y} for y in (0, 90, 180, 270)]
        w(A / f"blockstates/{b}.json", {"variants": variants})
        loot(b, drop(M + gem, *[{"type": "minecraft:set_count", "count": s,
                                 "condition": {"type": "minecraft:match_block", "blocks": M + b, "state": {"size": str(s)}}}
                                for s in (2, 3)]))
        w(D / f"mechanical/worldgen/feature/gem_{gem}.json", weighted([(M + b, {"size": str(s)}, wt) for s, wt in ((1, 6), (2, 3), (3, 1))]))
        w(D / f"mechanical/worldgen/placed_feature/gem_{gem}.json", {"feature": f"mechanical:gem_{gem}",
                                                                    "placement": cave_surface(g["count"], *g["y"], False)})
        placed.append(f"gem_{gem}")
        sv_notes[gem] = {"text": f"Ädelsten som lösa stenar. {g['sv']}. Plockas från [[{b}]].", "category": "materials"}
        en_notes[gem] = {"text": f"Gemstone found as loose pebbles. {g['en']}. Picked up from [[{b}]].", "category": "materials"}
        sv_notes[b] = {"text": f"Lösa stenar av [[{gem}]] på grottgolv. {g['sv']}. Storlek 1–2 plockas för hand, storlek 3 kräver pickaxe. "
                               f"Ger 1–3 [[{gem}]] efter storlek.", "category": "blocks"}
        en_notes[b] = {"text": f"Loose [[{gem}]] pebbles on cave floors. {g['en']}. Sizes 1–2 are picked up by hand, size 3 needs a pickaxe. "
                               f"Drops 1–3 [[{gem}]] by size.", "category": "blocks"}

    w(D / f"mechanical/neoforge/biome_modifier/add_gem_{gem}.json", {
        "type": "neoforge:add_features", "biomes": f"#mechanical:has_gem/{gem}",
        "features": [M + p for p in placed], "step": modifier_step})
    features += placed

# ---- tags ----
w(D / "c/tags/item/gems.json", {"values": [f"#c:gems/{g['name']}" for g in GEMS]})
w(D / "mechanical/tags/item/gemstones.json", {"values": [M + g["name"] for g in GEMS]})
patch_tag(D / "minecraft/tags/block/mineable/pickaxe.json", pickaxe)
patch_tag(D / "mechanical/tags/block/needs_bronze_tool.json", bronze)

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang_path.write_text(json.dumps(json.loads(lang_path.read_text(encoding="utf-8")) | lang, indent=2) + "\n", encoding="utf-8")

# ---- registry notes (sv + en), same layout as the hand-kept files: one entry per line ----
for name, notes in (("registry_notes.json", sv_notes), ("registry_notes_en.json", en_notes)):
    p = ROOT / "docs" / name
    merged = json.loads(p.read_text(encoding="utf-8")) | notes
    p.write_text("{\n" + ",\n".join(f"  {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in merged.items()) + "\n}\n",
                 encoding="utf-8")

print(f"gemstones ok: {len(GEMS)} gems, {len(pickaxe)} blocks, placed features: {' '.join(features)}")
