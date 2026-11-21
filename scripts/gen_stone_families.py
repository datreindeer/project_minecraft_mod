"""Stone families (StoneFamilies.java; docs/world-materials-plan.md 1.1, 1.2). One row per stone in STONES.

Each row copies a vanilla block family out of the merged jar (blockstates, block models, item models, loot tables, recipes)
with the vanilla names mapped onto the stone (TEMPLATES: an explicit list of vanilla blocks per template, each with the
name pattern it becomes, "{}" = the stone's name), adds the block tags (pickaxe, slabs/stairs/walls, needs_bronze_tool for
hard stones), lang, and recolours the vanilla textures onto the row's palette. New stones also get their base block from
the template (blockstate, models, item), its loot (itself, or Chalk like Clay), base_stone_overworld /
stone_ore_replaceables and worldgen (ore blobs everywhere, more in the row's biomes). Limestone's base block and worldgen
stay in gen_iron_ores.py (own_base=False). The Bronze Chisel chains live in StoneFamilies.java (same rows).

Adding a stone: one STONES row here + one STONES row in StoneFamilies.java (+ lang/advancement/docs). Adding a template:
one TEMPLATES entry here + the same patterns in StoneFamilies.Template.

Generator order: ... gen_iron_ores.py, gen_stone_families.py, ... gen_block_items.py, gen_unlocks.py, gen_advancements.py,
gen_loot_format.py. Run: python scripts/gen_stone_families.py (writes stone_preview.png in the repo root, not committed)
"""
import glob
import io
import json
import re
import zipfile
from collections import namedtuple
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
JAVA = (ROOT / "src/main/java/com/mechanical/StoneFamilies.java").read_text(encoding="utf-8")


def family(base, names, word=None, **irregular):
    """Template: the vanilla base block and {vanilla block: our pattern}. Patterns default to the vanilla name with the
    template word swapped for "{}"; irregular names are given explicitly."""
    word = word or base
    return base, {n: irregular.get(n, n.replace(word, "{}")) for n in names.split()}


TEMPLATES = {
    "tuff": family("tuff", "tuff_slab tuff_stairs tuff_wall polished_tuff polished_tuff_slab polished_tuff_stairs polished_tuff_wall "
                   "tuff_bricks tuff_brick_slab tuff_brick_stairs tuff_brick_wall chiseled_tuff chiseled_tuff_bricks"),
    "deepslate": family("deepslate", "cobbled_deepslate cobbled_deepslate_slab cobbled_deepslate_stairs cobbled_deepslate_wall "
                        "polished_deepslate polished_deepslate_slab polished_deepslate_stairs polished_deepslate_wall deepslate_bricks "
                        "deepslate_brick_slab deepslate_brick_stairs deepslate_brick_wall deepslate_tiles deepslate_tile_slab "
                        "deepslate_tile_stairs deepslate_tile_wall cracked_deepslate_bricks cracked_deepslate_tiles chiseled_deepslate"),
    "stone": family("stone", "stone_slab stone_stairs smooth_stone smooth_stone_slab stone_bricks stone_brick_slab stone_brick_stairs "
                    "stone_brick_wall cracked_stone_bricks chiseled_stone_bricks mossy_stone_bricks mossy_stone_brick_slab "
                    "mossy_stone_brick_stairs mossy_stone_brick_wall"),
    "sandstone": family("sandstone", "sandstone_slab sandstone_stairs sandstone_wall cut_sandstone cut_sandstone_slab smooth_sandstone "
                        "smooth_sandstone_slab smooth_sandstone_stairs chiseled_sandstone"),
    # not used yet (phase 2/3/6): checked against the jar the first time a row uses them
    "quartz": family("quartz_block", "quartz_slab quartz_stairs quartz_bricks quartz_pillar chiseled_quartz_block smooth_quartz "
                     "smooth_quartz_slab smooth_quartz_stairs", word="quartz"),
    "basalt": family("basalt", "polished_basalt smooth_basalt"),
    "mud": family("packed_mud", "mud_bricks mud_brick_slab mud_brick_stairs mud_brick_wall", word="mud"),
    "bricks": family("bricks", "brick_slab brick_stairs brick_wall", word="brick", bricks="{}_bricks"),
}
# the vanilla texture whose brightness range sets the palette ramp
RANGE_TEXTURE = {"tuff": "tuff", "deepslate": "deepslate", "stone": "stone", "sandstone": "sandstone", "quartz": "quartz_block_side",
                 "basalt": "basalt_side", "mud": "packed_mud", "bricks": "bricks"}

Ore = namedtuple("Ore", "size chance lo hi count blo bhi biomes")
# size: blob size; everywhere: 1 blob in `chance` chunks at y lo..hi; in `biomes` (vanilla ids or #tags): `count` more at y blo..bhi
Stone = namedtuple("Stone", "name template palette hard chains ore base drop bands own_base", defaults=(None, "self", None, True))
# name, template, palette (5 colours dark -> light), hard (needs a Bronze Pickaxe), chisel chains (Java has the same),
# ore (None: worldgen elsewhere), base id (default name), drop ("self" or (item, count): silk touch gives the block, like
# clay; an item outside the family can be pressed 2x2 back into the block), bands (2nd palette,
# striped), own_base (False: base block files written elsewhere)

STONES = [
    Stone("limestone", "tuff", [(122, 114, 96), (150, 142, 122), (176, 168, 146), (198, 190, 168), (218, 212, 192)], False,
          [["limestone", "polished_limestone", "limestone_bricks", "chiseled_limestone_bricks"]], None, own_base=False),
    Stone("slate", "deepslate", [(44, 50, 62), (60, 68, 82), (78, 87, 102), (96, 106, 122), (116, 126, 142)], False,
          [["slate", "polished_slate", "slate_bricks", "slate_tiles", "chiseled_slate"], ["cobbled_slate", "polished_slate"]],
          Ore(56, 4, 0, 110, 2, 40, 200, ["windswept_hills", "windswept_gravelly_hills", "windswept_forest", "grove", "snowy_slopes",
                                           "frozen_peaks", "jagged_peaks", "stony_peaks"]),
          drop=("cobbled_slate", 1)),  # like deepslate: Polished Slate is crafted from Cobbled Slate
    Stone("gneiss", "stone", [(84, 84, 88), (108, 107, 110), (134, 131, 132), (160, 155, 154), (186, 180, 177)], True,
          [["gneiss", "smooth_gneiss", "gneiss_bricks", "chiseled_gneiss_bricks"]],
          Ore(64, 4, 0, 100, 2, 40, 220, ["#minecraft:is_mountain"]),
          bands=[(108, 82, 84), (138, 104, 102), (166, 128, 122), (190, 150, 142), (212, 174, 164)]),
    Stone("chalk", "sandstone", [(190, 188, 178), (208, 206, 196), (222, 220, 212), (236, 234, 228), (249, 248, 243)], False,
          [["chalk_block", "cut_chalk", "chiseled_chalk"]],
          Ore(48, 5, 50, 90, 2, 50, 90, ["#minecraft:is_beach", "stony_shore", "plains", "sunflower_plains", "meadow"]),
          base="chalk_block", drop=("chalk", 4)),
    Stone("soapstone", "tuff", [(74, 88, 80), (94, 110, 100), (116, 130, 118), (138, 150, 138), (160, 170, 156)], False,
          [["soapstone", "polished_soapstone", "soapstone_bricks", "chiseled_soapstone_bricks"]],
          Ore(40, 6, 0, 90, 1, 30, 160, ["#minecraft:is_mountain"])),
    Stone("shale", "tuff", [(50, 46, 42), (66, 60, 54), (82, 75, 68), (98, 90, 82), (116, 106, 96)], False,
          [["shale", "polished_shale", "shale_bricks", "chiseled_shale_bricks"]],
          Ore(56, 4, -8, 50, 2, 0, 55, ["plains", "sunflower_plains", "swamp", "mangrove_swamp", "#minecraft:is_river"])),
    Stone("conglomerate", "tuff", [(80, 74, 68), (106, 96, 86), (134, 120, 104), (160, 144, 122), (186, 168, 144)], False,
          [["conglomerate", "polished_conglomerate", "conglomerate_bricks", "chiseled_conglomerate_bricks"]],
          Ore(40, 6, 30, 80, 2, 40, 70, ["#minecraft:is_river"])),
    # phase 2 (lime, Iron Age): Dolomite sits with Limestone (more where its cliffs are), Travertine by springs and water,
    # Shell Bank (coquina) under beaches and shores; all three burn to Quicklime in the Clay Kiln (KilnBlockEntity)
    Stone("dolomite", "tuff", [(118, 110, 108), (148, 138, 134), (176, 166, 160), (202, 193, 186), (226, 219, 212)], False,
          [["dolomite", "polished_dolomite", "dolomite_bricks", "chiseled_dolomite_bricks"]],
          Ore(48, 5, 0, 60, 2, 56, 160, ["#mechanical:has_limestone_cliffs"])),
    Stone("travertine", "sandstone", [(156, 120, 78), (184, 146, 100), (206, 170, 124), (222, 190, 146), (236, 210, 170)], False,
          [["travertine", "cut_travertine", "chiseled_travertine"]],
          Ore(40, 8, 40, 90, 2, 40, 90, ["#minecraft:is_river", "swamp", "mangrove_swamp", "plains", "sunflower_plains"])),
    Stone("shell_bank", "sandstone", [(130, 122, 108), (162, 154, 138), (190, 182, 166), (212, 206, 190), (232, 228, 214)], False,
          [["shell_bank", "cut_shell_bank", "chiseled_shell_bank"]],
          Ore(40, 8, 40, 70, 2, 40, 70, ["#minecraft:is_beach", "stony_shore", "ocean", "lukewarm_ocean", "warm_ocean"]),
          bands=[(150, 116, 100), (180, 144, 124), (204, 170, 150), (222, 192, 172), (238, 214, 196)]),  # pink shell layers
]


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(json.loads(text), indent=2) + "\n", encoding="utf-8")


def tag_add(rel, values):
    path = D / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    write(path, json.dumps(tag))


def ramp(palette, t):
    t = max(0.0, min(1.0, t)) * (len(palette) - 1)
    i = min(int(t), len(palette) - 2)
    f = t - i
    return tuple(int(palette[i][c] * (1 - f) + palette[i + 1][c] * f) for c in range(3))


def vanilla_png(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
preview = []

for s in STONES:
    base_vanilla, patterns = TEMPLATES[s.template]
    base_id = s.base or s.name
    for p in patterns.values():
        assert f'"{p}"' in JAVA, f"StoneFamilies.java has no pattern {p} for template {s.template}"
    names = {v: p.format(s.name) for v, p in patterns.items()}
    names[base_vanilla] = base_id
    token = re.compile(r"(?<![a-z])(" + "|".join(sorted(names, key=len, reverse=True)) + r")(?![a-z])")

    def sub(text):
        return token.sub(lambda m: names[m[1]], text)

    def rename(text):
        """minecraft:<family name>... -> mechanical:<our name>... (blocks, items, models, textures, loot tables)."""
        def one(m):
            new = sub(m[2])
            return f"mechanical:{m[1]}{new}" if new != m[2] else m[0]
        return re.sub(r"minecraft:((?:block/|blocks/|item/)?)([a-z_]+)", one, text)

    preview.append([])
    copy = [n for n in names if s.own_base or n != base_vanilla]
    models, textures = set(), set()
    for kind, d in (("blockstates", "assets"), ("items", "assets"), ("loot_table/blocks", "data")):
        for n in copy:
            if kind.startswith("loot") and n == base_vanilla:
                continue  # the base block's loot is written below
            text = rename(jar.read(f"{d}/minecraft/{kind}/{n}.json").decode())
            write(RES / d / "mechanical" / kind / f"{names[n]}.json", text)
            models |= set(re.findall(r'"mechanical:(block/[a-z_]+)"', text))
    for src in jar.namelist():  # copied blockstates/items name the models; models name the textures
        m = re.fullmatch(r"assets/minecraft/models/(block/[a-z_]+)\.json", src)
        if m and rename("minecraft:" + m[1]) in {"mechanical:" + x for x in models} and (s.own_base or m[1] != "block/" + base_vanilla):
            text = rename(jar.read(src).decode())
            write(A / "models" / f"{rename('minecraft:' + m[1]).split(':')[1]}.json", text)
            for t in json.loads(jar.read(src)).get("textures", {}).values():
                if rename(t) != t:
                    textures.add(t.split("/")[-1])
    for src in jar.namelist():
        m = re.fullmatch(r"data/minecraft/recipe/([a-z_]+)\.json", src)
        if not m:
            continue
        r = json.loads(jar.read(src))
        res = r.get("result")
        rid = res.get("id") if isinstance(res, dict) else res
        others = json.dumps({k: v for k, v in r.items() if k != "result"})
        if rid in {"minecraft:" + n for n in names} and any(f'"minecraft:{n}"' in others for n in names):
            write(D / "mechanical/recipe" / f"{sub(m[1])}.json", rename(jar.read(src).decode()))

    ids = sorted("mechanical:" + names[n] for n in copy)
    if s.own_base:  # base block loot: itself, or chalk like clay
        if s.drop == "self":
            loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
                    "entries": [{"type": "minecraft:item", "name": "mechanical:" + base_id}]}]}
        else:
            loot = {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "condition": "minecraft:tool/can_silk_touch", "name": "mechanical:" + base_id},
                {"type": "minecraft:item", "name": "mechanical:" + s.drop[0], "modifier": [
                    {"type": "minecraft:set_count", "count": s.drop[1]}, {"type": "minecraft:explosion_decay"}]}]}]}]}
        if s.drop != "self" and s.drop[0] not in names.values():  # 4 chalk pressed back into a block, like clay balls
            write(D / f"mechanical/recipe/{base_id}.json", json.dumps({"type": "minecraft:crafting_shaped", "category": "building",
                  "pattern": ["##", "##"], "key": {"#": "mechanical:" + s.drop[0]}, "result": {"id": "mechanical:" + base_id}}))
        loot["random_sequence"] = f"mechanical:blocks/{base_id}"
        write(D / f"mechanical/loot_table/blocks/{base_id}.json", json.dumps(loot))
        tag_add("minecraft/tags/block/base_stone_overworld.json", ["mechanical:" + base_id])
        tag_add("minecraft/tags/block/stone_ore_replaceables.json", ["mechanical:" + base_id])

    # ---- tags ----
    tag_add("minecraft/tags/block/mineable/pickaxe.json", ids)
    for kind, tag in (("slab", "slabs"), ("stairs", "stairs"), ("wall", "walls")):
        group = [i for i in ids if i.endswith("_" + kind)]
        tag_add(f"minecraft/tags/block/{tag}.json", group)
        tag_add(f"minecraft/tags/item/{tag}.json", group)
    if s.hard:
        tag_add("mechanical/tags/block/needs_bronze_tool.json", ids)

    # ---- lang ----
    lang.update({f"block.mechanical.{i[11:]}": i[11:].replace("_", " ").title() for i in ids})

    # ---- textures: recoloured by brightness onto the palette (one shared range so the family matches) ----
    rng = vanilla_png(RANGE_TEXTURE[s.template])
    base = [sum(rng.getpixel((x, y))[:3]) / 3 for y in range(16) for x in range(16)]
    lo, hi = min(base), max(base)
    for t in sorted(textures, key=lambda t: (t != base_vanilla, t)):
        if not s.own_base and t == base_vanilla:
            continue
        out = vanilla_png(t)
        for y in range(out.height):
            for x in range(out.width):
                r, g, b, a = out.getpixel((x, y))
                if not a or (t.startswith("mossy") and g > r + 8 and g > b):  # moss stays green
                    continue
                pal = s.bands if s.bands and ((y + x // 5) // 3) % 2 else s.palette
                out.putpixel((x, y), ramp(pal, ((r + g + b) / 3 - lo) / (hi - lo)) + (a,))
        name = sub(t)
        out.save(A / f"textures/block/{name}.png")
        if s.own_base:
            preview[-1].append(out)

    # ---- worldgen: blobs everywhere, more in the stone's own biomes ----
    if s.ore:
        o = s.ore
        write(D / f"mechanical/worldgen/feature/ore_{s.name}.json", json.dumps({"type": "minecraft:ore", "size": o.size,
              "discard_chance_on_air_exposure": 0.0, "targets": [{"state": "mechanical:" + base_id,
              "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:base_stone_overworld"}}]}))
        for suffix, head, lo_y, hi_y in (("", {"type": "minecraft:rarity_filter", "chance": o.chance}, o.lo, o.hi),
                                         ("_biome", {"type": "minecraft:count", "count": o.count}, o.blo, o.bhi)):
            write(D / f"mechanical/worldgen/placed_feature/ore_{s.name}{suffix}.json", json.dumps({"feature": f"mechanical:ore_{s.name}",
                  "placement": [head, {"type": "minecraft:in_square"}, {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                  "min_inclusive": {"absolute": lo_y}, "max_inclusive": {"absolute": hi_y}}}, {"type": "minecraft:biome"}]}))
        write(D / f"mechanical/tags/worldgen/biome/has_{s.name}.json",
              json.dumps({"values": [b if b.startswith("#") else "minecraft:" + b for b in o.biomes]}))
        for suffix, biomes in (("", "#minecraft:is_overworld"), ("_biome", f"#mechanical:has_{s.name}")):
            write(D / f"mechanical/neoforge/biome_modifier/add_{s.name}{suffix}.json", json.dumps({"type": "neoforge:add_features",
                  "biomes": biomes, "features": [f"mechanical:ore_{s.name}{suffix}"], "step": "underground_ores"}))
    print(f"{s.name}: {len(ids)} blocks, {len(textures)} textures")

# ---- Lime Mortar (phase 2): 4 of what a bricks block is laid from + 1 Lime Mortar -> twice the bricks, for every
# family's 2x2 bricks recipe and vanilla's. Cheaper, never required (world-materials-plan, default answer 3).
def mortar(result, recipe_json):
    r = json.loads(recipe_json)
    if r.get("type") != "minecraft:crafting_shaped" or r.get("pattern") != ["##", "##"]:
        return
    res = r["result"]
    write(D / f"mechanical/recipe/{result.split(':')[1]}_with_mortar.json", json.dumps({"type": "minecraft:crafting_shapeless",
          "category": "building", "ingredients": [r["key"]["#"]] * 4 + ["mechanical:lime_mortar"],
          "result": {"id": res["id"], "count": 2 * res.get("count", 1)}}))


for s in STONES:
    for p in TEMPLATES[s.template][1].values():
        f = D / f"mechanical/recipe/{p.format(s.name)}.json"
        if p == "{}_bricks" and f.exists():
            mortar("mechanical:" + p.format(s.name), f.read_text(encoding="utf-8"))
for v in ("stone_bricks", "deepslate_bricks", "bricks", "mud_bricks"):
    mortar("minecraft:" + v, jar.read(f"data/minecraft/recipe/{v}.json").decode())

lang["itemGroup.mechanical.building_blocks"] = "Building Blocks"
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

# preview of the new textures, 16x scaled (not committed)
preview = [row for row in preview if row]
sheet = Image.new("RGBA", (max(map(len, preview)) * 16, len(preview) * 16))
for y, row in enumerate(preview):
    for x, img in enumerate(row):
        sheet.paste(img, (x * 16, y * 16))
sheet.resize((sheet.width * 16, sheet.height * 16), Image.NEAREST).save(ROOT / "stone_preview.png")
print("stone families ok")
