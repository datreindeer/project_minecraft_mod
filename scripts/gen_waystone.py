"""Waystones (worldgen/Waystones.java): linked pairs of carved standing stones, 1000-10000 blocks apart; right-click one to
stand beside the other. Writes:
- block models waystone_<look>_lower/_middle/_upper for the seven looks (the stone's look follows its biome): a rough footing and
  a dressed shaft below, the shaft in the middle, the shaft, a stepped taper, head and cap on top (3 blocks tall); on the rune faces (front and back) a full-bright
  layer of just the runes a hair outside the stone (light_emission 15)
- blockstate (facing x look x part), the item model (the whole stone, small in the hand), lang
- the feature mechanical:waystone (no placement: the Java feature asks per chunk whether a pair's end stands there) added
  to every overworld biome in top_layer_modification
- block tags: wither and dragon immune, features cannot replace it. No loot table (noLootTable): it never drops.
Textures: standalone gen_waystone_textures.py. Generator order: after gen_oak_folk, before gen_block_items.
Run: python scripts/gen_waystone.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M, S = "minecraft:", "mechanical:"
LOOKS = ["mossy", "sand", "frost", "red", "dark", "chalk", "grey"]


def w(path, obj, ascii=False):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=ascii) + "\n", encoding="utf-8")


def read(path, default):
    return json.loads(path.read_text(encoding="utf-8")) if path.exists() else default


def tag(path, *values):
    t = read(path, {"values": []})
    for v in values:
        if v not in t["values"]:
            t["values"].append(v)
    w(path, t)


def face(tex, uv, cull=None):
    f = {"uv": uv, "texture": tex}
    if cull:
        f["cullface"] = cull
    return f


def box(frm, to, faces):
    return {"from": frm, "to": to, "faces": faces}


def glow(frm, to, side, uv, tex):
    return {"from": frm, "to": to, "light_emission": 15, "shade": False, "faces": {side: face(tex, uv)}}


def sides(tex, ns_uv, ew_uv):
    return {"north": face(tex, ns_uv), "south": face(tex, ns_uv), "east": face(tex, ew_uv), "west": face(tex, ew_uv)}


def lower():
    return [
        # the rough footing, two steps, and a couple of chips of stone at its foot
        box([1, 0, 1], [15, 2, 15], {**sides("#base", [1, 14, 15, 16], [1, 14, 15, 16]), "up": face("#base", [1, 1, 15, 15]),
                                     "down": face("#base", [1, 1, 15, 15], "down")}),
        box([2, 2, 2], [14, 3, 14], {**sides("#base", [2, 13, 14, 14], [2, 13, 14, 14]), "up": face("#base", [2, 2, 14, 14])}),
        box([0, 0, 5], [1, 1, 9], {**sides("#base", [5, 15, 9, 16], [5, 15, 9, 16]), "up": face("#base", [0, 5, 1, 9])}),
        box([12, 0, 15], [14.5, 1.5, 16], {**sides("#base", [12, 14.5, 14.5, 16], [15, 14.5, 16, 16]), "up": face("#base", [12, 15, 14.5, 16])}),
        # the shaft: runes front (north) and back
        box([3, 3, 3.5], [13, 16, 12.5], {"north": face("#front", [3, 0, 13, 13]), "south": face("#front", [3, 0, 13, 13]),
                                          "east": face("#side", [3.5, 0, 12.5, 13]), "west": face("#side", [3.5, 0, 12.5, 13])}),
        glow([3, 3, 3.45], [13, 16, 3.45], "north", [3, 0, 13, 13], "#glow"),
        glow([3, 3, 12.55], [13, 16, 12.55], "south", [3, 0, 13, 13], "#glow"),
    ]


def middle():
    return [
        box([3, 0, 3.5], [13, 16, 12.5], {"north": face("#front", [3, 0, 13, 16]), "south": face("#front", [3, 0, 13, 16]),
                                          "east": face("#side", [3.5, 0, 12.5, 16]), "west": face("#side", [3.5, 0, 12.5, 16])}),
        glow([3, 0, 3.45], [13, 16, 3.45], "north", [3, 0, 13, 16], "#glow"),
        glow([3, 0, 12.55], [13, 16, 12.55], "south", [3, 0, 13, 16], "#glow"),
    ]


def upper():
    return [
        box([3, 0, 3.5], [13, 8, 12.5], {"north": face("#front", [3, 8, 13, 16]), "south": face("#front", [3, 8, 13, 16]),
                                         "east": face("#side", [3.5, 8, 12.5, 16]), "west": face("#side", [3.5, 8, 12.5, 16]),
                                         "up": face("#top", [3, 3.5, 13, 12.5])}),
        box([3.5, 8, 4], [12.5, 12, 12], {"north": face("#front", [3.5, 4, 12.5, 8]), "south": face("#front", [3.5, 4, 12.5, 8]),
                                          "east": face("#side", [4, 4, 12, 8]), "west": face("#side", [4, 4, 12, 8]),
                                          "up": face("#top", [3.5, 4, 12.5, 12])}),
        box([4.5, 12, 5], [11.5, 14, 11], {"north": face("#front", [4.5, 2, 11.5, 4]), "south": face("#front", [4.5, 2, 11.5, 4]),
                                           "east": face("#side", [5, 2, 11, 4]), "west": face("#side", [5, 2, 11, 4]),
                                           "up": face("#top", [4.5, 5, 11.5, 11])}),
        box([5.5, 14, 6], [10.5, 15, 10], {**sides("#side", [5.5, 1, 10.5, 2], [6, 1, 10, 2]), "up": face("#top", [5.5, 6, 10.5, 10])}),
        glow([3, 0, 3.45], [13, 8, 3.45], "north", [3, 8, 13, 16], "#glow"),
        glow([3, 0, 12.55], [13, 8, 12.55], "south", [3, 8, 13, 16], "#glow"),
        glow([3.5, 8, 3.95], [12.5, 12, 3.95], "north", [3.5, 4, 12.5, 8], "#glow"),
        glow([3.5, 8, 12.05], [12.5, 12, 12.05], "south", [3.5, 4, 12.5, 8], "#glow"),
        glow([4.5, 12, 4.95], [11.5, 14, 4.95], "north", [4.5, 2, 11.5, 4], "#glow"),
        glow([4.5, 12, 11.05], [11.5, 14, 11.05], "south", [4.5, 2, 11.5, 4], "#glow"),
    ]


def textures(look, half):
    t = f"{S}block/waystone_{look}"
    return {"particle": f"{t}_side", "side": f"{t}_side", "top": f"{t}_top", "base": f"{t}_base",
            "front": f"{t}_front_{half}", "glow": f"{t}_glow_{half}"}


for look in LOOKS:
    for half, els in (("lower", lower()), ("middle", middle()), ("upper", upper())):
        w(A / f"models/block/waystone_{look}_{half}.json", {"parent": M + "block/block", "textures": textures(look, half), "elements": els})

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
variants = {}
for f, rot in ROT.items():
    for part in ("lower", "middle", "upper"):
        for look in LOOKS:
            v = {"model": f"{S}block/waystone_{look}_{part}"}
            if rot:
                v["y"] = rot
            variants[f"facing={f},look={look},part={part}"] = v
w(A / "blockstates/waystone.json", {"variants": variants})


# the item: the whole grey stone (each part raised to its height and keeping its own rune textures)
def retex(els, half, dy):
    out = []
    for e in els:
        e = json.loads(json.dumps(e))
        e["from"][1] += dy
        e["to"][1] += dy
        for fc in e["faces"].values():
            if fc["texture"] in ("#front", "#glow"):
                fc["texture"] += "_" + half
        out.append(e)
    return out


t = f"{S}block/waystone_grey"
w(A / "models/item/waystone.json", {
    "parent": M + "block/block",
    "textures": {"particle": f"{t}_side", "side": f"{t}_side", "top": f"{t}_top", "base": f"{t}_base", "front_lower": f"{t}_front_lower",
                 "front_middle": f"{t}_front_middle", "front_upper": f"{t}_front_upper", "glow_lower": f"{t}_glow_lower",
                 "glow_middle": f"{t}_glow_middle", "glow_upper": f"{t}_glow_upper"},
    "elements": retex(lower(), "lower", -16) + retex(middle(), "middle", 0) + retex(upper(), "upper", 16),
    "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.36, 0.36, 0.36]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 4, 0], "scale": [0.22, 0.22, 0.22]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.36, 0.36, 0.36]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 1, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, -1, 0], "scale": [0.25, 0.25, 0.25]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, -1, 0], "scale": [0.25, 0.25, 0.25]}}})
w(A / "items/waystone.json", {"model": {"type": "minecraft:model", "model": S + "item/waystone"}})

# worldgen: the Java feature decides per chunk (Waystones.Site), so no placement modifiers and every overworld biome
w(D / "mechanical/worldgen/feature/waystone.json", {"type": S + "waystone"})
w(D / "mechanical/worldgen/placed_feature/waystone.json", {"feature": S + "waystone", "placement": []})
w(D / "mechanical/neoforge/biome_modifier/add_waystone.json", {"type": "neoforge:add_features", "biomes": "#minecraft:is_overworld",
                                                               "features": S + "waystone", "step": "top_layer_modification"})

TAGS = D / "minecraft/tags/block"
for name in ("wither_immune", "dragon_immune", "features_cannot_replace"):
    tag(TAGS / f"{name}.json", S + "waystone")

lang = read(A / "lang/en_us.json", {})
lang.update({"block.mechanical.waystone": "Waystone", "block.mechanical.waystone.silent": "The runes are dark: this stone's twin is lost"})
w(A / "lang/en_us.json", lang, ascii=True)
print("waystone ok")
