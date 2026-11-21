"""World-materials biomes (docs/world-materials-plan.md): surface biomes and cave biomes built around the new stones.

Phase 0 (spike): Chalk Downs (surface) and Limestone Caverns (cave). Later phases add one row per biome to BIOMES and
MATERIALS below, plus a climate slot in Java (surface: mixin/OverworldBiomeBuilderMixin.addSurfaceBiome; cave: a row
in ModBiomes.CAVES).

Ground blocks: in 26.3 the surface rules are data (registry worldgen/material_rule; vanilla's overworld.json is the
root). This script writes mechanical:overworld (our rules, one condition per biome) and overrides
minecraft:overworld with vanilla's own sequence plus one reference to ours, right after the ore veins, so our rules
win over vanilla's surface and deepslate rules but only inside our biomes.

Each biome copies a vanilla biome's JSON (minus the dropped features, so per-step feature order stays consistent and
FeatureSorter is happy) and joins every biome tag and every mod biome modifier that lists that vanilla biome.

Generator order: ... gen_biomes.py, gen_world_biomes.py, ... (after gen_limestone_blocks.py), gen_block_items.py ...
Run: python scripts/gen_world_biomes.py"""
import json
import zipfile
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
ROOT = BASE / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
jar = zipfile.ZipFile(BASE / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar")
M = "minecraft:"


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


# ---------------------------------------------------------------- biomes
# name: (vanilla template, English name, drop these template features, overrides of top-level biome fields)
BIOMES = {
    "chalk_downs": ("plains", "Chalk Downs", [M + "trees_plains"],
                    {"temperature": 0.7, "downfall": 0.5, "effects": {"grass_color": "#8fb35a", "water_color": "#3f76e4"}}),
    "limestone_caverns": ("dripstone_caves", "Limestone Caverns", [M + "large_dripstone", M + "dripstone_cluster", M + "pointed_dripstone"],
                          {"temperature": 0.7, "downfall": 0.5}),
}

# ---------------------------------------------------------------- ground blocks
# phase 4: swap for the chalk block
CHALK_STONE = M + "calcite"
LIMESTONE = "mechanical:limestone"
# Every key is optional. Surface biomes: top (open air; under water the floor gets `under`), under (one layer below
# top), stone (the layers below that, about 3-9 deep), slope (top and under on steep slopes). Cave biomes: floor and
# ceiling (the block touching the cave air), wall (all other stone in the biome, so walls and the rock around the cave).
MATERIALS = {
    "chalk_downs": {"top": M + "grass_block", "under": M + "dirt", "stone": CHALK_STONE, "slope": CHALK_STONE},
    "limestone_caverns": {"floor": LIMESTONE, "ceiling": LIMESTONE, "wall": LIMESTONE},
}

block = lambda state: {"type": "minecraft:block", "result_state": state}
when = lambda cond, then: {"type": "minecraft:condition", "if_true": cond, "then_run": then}
seq = lambda *rules: {"type": "minecraft:sequence", "sequence": [r for r in rules if r]}
depth = lambda kind, offset=0, add=False, extra=0: {"type": "minecraft:stone_depth", "surface_type": kind, "offset": offset,
                                                    "add_surface_depth": add, "secondary_depth_range": extra}


def surface_rule(m):
    steep = m.get("slope") and when({"type": "minecraft:steep"}, block(m["slope"]))
    return seq(
        m.get("top") and when(M + "on_floor", seq(steep, when(M + "not_underwater", block(m["top"])))),
        m.get("under") and when(depth("floor", offset=1), seq(steep, block(m["under"]))),
        m.get("stone") and when(M + "deep_under_floor", block(m["stone"])))


def cave_rule(m):
    return seq(m.get("floor") and when(M + "on_floor", block(m["floor"])),
               m.get("ceiling") and when(M + "on_ceiling", block(m["ceiling"])),
               m.get("wall") and block(m["wall"]))


biome_is = lambda name: {"type": "minecraft:biome", "biome_is": f"mechanical:{name}"}
surface = [when(biome_is(n), surface_rule(m)) for n, m in MATERIALS.items() if {"top", "under", "stone", "slope"} & m.keys()]
caves = [when(biome_is(n), cave_rule(m)) for n, m in MATERIALS.items() if {"floor", "ceiling", "wall"} & m.keys()]
RULES = D / "mechanical/worldgen/material_rule"
w(RULES / "overworld.json", seq(when({"type": "minecraft:above_preliminary_surface"}, seq(*surface)), seq(*caves)))

root = json.loads(jar.read("data/minecraft/worldgen/material_rule/overworld.json"))
s = root["sequence"]
assert "mechanical:overworld" not in s and M + "overworld/iron_ore_vein" in s, s
s.insert(s.index(M + "overworld/iron_ore_vein") + 1, "mechanical:overworld")
w(D / "minecraft/worldgen/material_rule/overworld.json", root)

# ---------------------------------------------------------------- biome JSONs, tags, modifiers, lang
vanilla_tags = {n: json.loads(jar.read(n))["values"] for n in jar.namelist()
                if n.startswith("data/minecraft/tags/worldgen/biome/") and n.endswith(".json")}
lang = read(A / "lang/en_us.json", {})
for name, (base, title, drop, fields) in BIOMES.items():
    key, template = f"mechanical:{name}", M + base
    b = json.loads(jar.read(f"data/minecraft/worldgen/biome/{base}.json"))
    b.update(fields)
    b["features"] = [[f for f in step if f not in drop] for step in b["features"]]
    w(D / f"mechanical/worldgen/biome/{name}.json", b)
    lang[f"biome.mechanical.{name}"] = title
    # tags: vanilla's (written as additions in our data/minecraft) and the mod's own
    tag_files = {D / n.removeprefix("data/") for n, v in vanilla_tags.items() if template in v}
    tag_files |= {p for p in D.glob("*/tags/worldgen/biome/**/*.json") if template in read(p, {}).get("values", [])}
    for p in tag_files:
        t = read(p, {"values": []})
        if key not in t["values"]:
            t["values"].append(key)
        w(p, t)
    for p in sorted((D / "mechanical/neoforge/biome_modifier").glob("*.json")):
        m = read(p, {})
        if isinstance(m.get("biomes"), list) and template in m["biomes"] and key not in m["biomes"]:
            m["biomes"].append(key)
            w(p, m)
w(A / "lang/en_us.json", lang, ascii=True)  # the lang file keeps its escapes
print("world biomes ok")
