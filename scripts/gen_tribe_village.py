"""Data for tribe villages and tribes (worldgen/TribeVillageStructure.java, people/Tribe.java): structure, structure set,
biome tag, trail tag, the sharpened log palisade tops (models, carved-tip textures, loot, recipe, axe tag), the village
storage loot table and lang. Run: python scripts/gen_tribe_village.py"""
import json
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
D = ROOT / "data/mechanical"
A = ROOT / "assets/mechanical"
LANG = A / "lang/en_us.json"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


# ---------------------------------------------------------------- worldgen

w(D / "worldgen/structure/tribe_village.json", {
    "type": "mechanical:tribe_village", "biomes": "#mechanical:has_structure/tribe_village",
    "spawn_overrides": {}, "step": "top_layer_modification", "terrain_adaptation": "beard_box"})
# rarer than camps: at most one per 36x36 chunks, and many sites fail the flat-and-dry test
w(D / "worldgen/structure_set/tribe_village.json", {
    "structures": [{"structure": "mechanical:tribe_village", "weight": 1}],
    "placement": {"type": "minecraft:random_spread", "salt": 480213977, "spacing": 36, "separation": 14,
                  "exclusion_zone": {"other_set": "minecraft:villages", "chunk_count": 6}}})
w(D / "tags/worldgen/biome/has_structure/tribe_village.json", {"values": [
    "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:forest", "minecraft:birch_forest",
    "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:snowy_plains", "minecraft:savanna"]})
# the paths agent connects these with trails
# the trail tag (mechanical:trail_connected) is written by gen_trails.py, which lists the tribe village

# ---------------------------------------------------------------- loot: the village food store

def entry(name, weight, lo=1, hi=1):
    e = {"type": "minecraft:item", "name": name, "weight": weight}
    if hi > 1:
        e["modifier"] = [{"type": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e


w(D / "loot_table/chests/tribe_village.json", {"type": "minecraft:chest", "pools": [
    {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "entries": [
        entry("mechanical:cooked_venison_scraps", 8, 2, 5), entry("mechanical:cooked_game_meat_scraps", 8, 2, 5),
        entry("mechanical:raspberries", 6, 3, 8), entry("mechanical:blueberries", 6, 3, 8), entry("mechanical:lingonberries", 5, 3, 8),
        entry("mechanical:juniper_berries", 3, 2, 6), entry("minecraft:dried_kelp", 2, 2, 6)]},
    {"rolls": {"type": "minecraft:uniform", "min": 1, "max": 2}, "entries": [
        entry("mechanical:raw_hide", 4, 1, 2), entry("mechanical:scraped_hide", 3), entry("mechanical:cordage", 5, 1, 3),
        entry("mechanical:twine", 5, 2, 4), entry("mechanical:plant_fibre", 6, 3, 8), entry("minecraft:flint", 6, 2, 5),
        entry("mechanical:sinew", 3, 1, 2), entry("mechanical:bone_arrow", 3, 2, 6)]}],
    "random_sequence": "mechanical:chests/tribe_village"})

# ---------------------------------------------------------------- sharpened logs (palisade tops)

H = lambda x, y, s=0: (((x * 374761393 + y * 668265263 + s * 2147483647) & 0xFFFFFFFF) * 1274126177 >> 7) % 1000 / 1000.0


def tip_texture(light, mid, dark, bark):
    """Carved wood: pale fresh-cut faces with axe facets running down to the point (top rows = the point), dark grain
    lines, and a ragged strip of bark left at the bottom of the lowest step."""
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            facet = (x + (y // 3)) % 5           # slanted axe cuts
            t = 0.55 + 0.45 * (facet / 4)
            c = tuple(round(mid[i] + (light[i] - mid[i]) * t) for i in range(3))
            if facet == 0:
                c = tuple(round(v * 0.86) for v in c)    # edge of a cut
            if x % 4 == 1 and H(x, y, 3) < 0.55:
                c = tuple(round(dark[i] * 0.6 + c[i] * 0.4) for i in range(3))   # grain
            n = (H(x, y) - 0.5) * 14
            c = tuple(max(0, min(255, round(v + n))) for v in c)
            if y >= 14 + (H(x, 0, 7) < 0.4):
                c = bark[(x + y) % len(bark)]
            img.putpixel((x, y), c + (255,))
    return img


WOODS = {
    "oak": dict(light=(196, 160, 104), mid=(168, 133, 82), dark=(110, 84, 50), bark=[(92, 72, 44), (74, 58, 36), (106, 84, 52)]),
    "spruce": dict(light=(160, 122, 80), mid=(132, 98, 60), dark=(82, 58, 34), bark=[(58, 40, 24), (46, 32, 20), (70, 50, 30)]),
}
names = {}
for wood, pal in WOODS.items():
    name = f"sharpened_{wood}_log"
    tip_texture(**pal).save(A / f"textures/block/{name}_tip.png")
    tip = f"mechanical:block/{name}_tip"

    def faces(uv, tex="#tip", up=True):
        f = {d: {"uv": uv, "texture": tex} for d in ("north", "south", "east", "west")}
        if up:
            f["up"] = {"uv": [uv[0], uv[0], uv[2], uv[2]], "texture": "#tip"}
        return f

    base = faces([0, 7, 16, 16], "#side")
    base["up"] = {"uv": [0, 0, 16, 16], "texture": "#tip"}
    base["down"] = {"uv": [0, 0, 16, 16], "texture": "#end", "cullface": "down"}
    w(A / f"models/block/{name}.json", {
        "parent": "minecraft:block/block",
        "textures": {"side": f"minecraft:block/{wood}_log", "end": f"minecraft:block/{wood}_log_top", "tip": tip,
                     "particle": f"minecraft:block/{wood}_log"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 9, 16], "faces": base},
            {"from": [2, 9, 2], "to": [14, 12, 14], "faces": faces([2, 9, 14, 12])},
            {"from": [4, 12, 4], "to": [12, 14, 12], "faces": faces([4, 5, 12, 7])},
            {"from": [6, 14, 6], "to": [10, 16, 10], "faces": faces([6, 0, 10, 2])}]})
    w(A / f"blockstates/{name}.json", {"variants": {"": [{"model": f"mechanical:block/{name}"},
                                                          {"model": f"mechanical:block/{name}", "y": 90}]}})
    w(D / f"loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"mechanical:{name}"}], "condition": {"type": "minecraft:survives_explosion"}}],
        "random_sequence": f"mechanical:blocks/{name}"})
    # a log shaped to a point with flint
    w(D / f"recipe/{name}.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                                  "ingredients": [f"minecraft:{wood}_log", "minecraft:flint"], "result": {"id": f"mechanical:{name}"}})
    names[f"block.mechanical.{name}"] = f"Sharpened {wood.capitalize()} Log"

axe = D.parent / "minecraft/tags/block/mineable/axe.json"
tag = json.loads(axe.read_text(encoding="utf-8"))
for wood in WOODS:
    if f"mechanical:sharpened_{wood}_log" not in tag["values"]:
        tag["values"].append(f"mechanical:sharpened_{wood}_log")
w(axe, tag)

# ---------------------------------------------------------------- lang

TOTEMS = ["raven", "wolf", "bear", "elk", "salmon", "owl", "fox", "boar", "lynx", "eagle", "otter", "badger", "stag", "heron",
          "mammoth", "aurochs", "hare", "crane", "wolverine", "bison"]
lang = json.loads(LANG.read_text(encoding="utf-8"))
lang.update(names)
lang.update({f"tribe.mechanical.totem.{t}": t.capitalize() for t in TOTEMS})
lang.update({
    "entity.mechanical.tribesperson.named": "%s the %s of the %s",
    "tribe.mechanical.name": "%s Tribe",
    "tribe.mechanical.banner": "Banner of the %s",
    "tribe.mechanical.standing.hostile": "Hostile",
    "tribe.mechanical.standing.unfriendly": "Unfriendly",
    "tribe.mechanical.standing.neutral": "Neutral",
    "tribe.mechanical.standing.friendly": "Friendly",
    "tribe.mechanical.standing.honoured": "Honoured",
    "message.mechanical.tribe.standing": "%s: %s",
    "chat.mechanical.tribesperson.greet.friend": "Welcome back, friend. Sit by our fire.",
    "chat.mechanical.tribesperson.warn": "This is our land. Turn back, stranger, or bleed.",
    "chat.mechanical.tribesperson.thief": "Thief! Those are our stores!",
    "chat.mechanical.tribesperson.gift": "A gift? You are kind. We will remember.",
    "chat.mechanical.tribesperson.no_trade_stranger": "We do not trade with strangers. Earn our trust first.",
})
w(LANG, lang)
print("tribe village data ok")
