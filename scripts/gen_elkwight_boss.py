"""Data for the Elkwight boss, phases 3-5 (phases 1-2, biome and ruin: gen_elkwight.py) (com.mechanical.wildlife.elkwight; docs/elkwight-plan.md, phases 3-5).

- lang: the boss, his axe (thrown and heavy), the trophies, Elkwight Hide / Bone / Cloak, Bone Charm, spawn egg, messages
  (knife too weak for his hide, a load that won't budge, the axe too heavy to lift)
- items: models + definitions; the Antler Crown's item shows its block model
- blocks: Elkwight Axe trophy (no item: breaking it gives the heavy axe back, no loot table) and the Antler Crown wall
  trophy (drops itself): blockstates by facing, element models with the rune overlay lit (light_emission)
- tags: block tag mechanical:elkwight_breakable (natural blocks he smashes and the Ground Wave cracks, with the Scorched
  Barrens' Charred Log and Wood Ash; never basalt, blackstone or magma, which his ruin is built of); the cloak in fur_insulation, chest_armor (enchantable like a chest
  piece), equipment/cloak and repairs_elkwight_hide; the Bone Charm in equipment/charm
- equipment asset elkwight_hide (the cloak's layer over the body)
- recipes: Elkwight Cloak (2 Elkwight Hide, 2 Sinew, Cordage), Bone Charm (3 Elkwight Bone, Cordage, a Gold Nugget)
Textures: scripts/gen_elkwight_boss_textures.py. Butchering yield and knife tier: Elkwights.init().
Generator order: ... gen_equipment.py, gen_fauna_*.py, gen_elkwight.py, gen_elkwight_boss.py, gen_block_items.py, gen_unlocks.py,
gen_advancements.py, gen_registry.py (after gen_equipment.py, whose slot lists also name the cloak and the charm).
Run: python scripts/gen_elkwight_boss.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def tag(kind, name, *values, replace=False):
    ns, path = name.split(":") if ":" in name else ("mechanical", name)
    p = D / f"{ns}/tags/{kind}/{path}.json"
    old = {"values": []} if replace or not p.exists() else json.loads(p.read_text(encoding="utf-8"))
    old["values"] += [v for v in values if v not in old["values"]]
    w(p, old)


def flat_item(item):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})


ITEMS = {"elkwight_hide": "Elkwight Hide", "elkwight_bone": "Elkwight Bone", "elkwight_cloak": "Elkwight Cloak",
         "bone_charm": "Bone Charm", "elkwight_spawn_egg": "Elkwight Spawn Egg"}
lang = {
    "entity.mechanical.elkwight": "Elkwight",
    "entity.mechanical.thrown_elkwight_axe": "Elkwight's Axe",
    "entity.mechanical.elkwight_axe": "Elkwight's Axe",
    "block.mechanical.elkwight_axe_trophy": "Elkwight's Axe",
    "block.mechanical.elkwight_antler_crown": "Elkwight Antler Crown",
    "item.mechanical.elkwight_antler_crown": "Elkwight Antler Crown",
    "message.mechanical.knife_too_weak_hide": "This hide is too tough for your knife",
    "message.mechanical.heavy.wont_budge": "It won't budge: it takes %s to move it at all (%s holding on)",
    "message.mechanical.elkwight_axe.heavy": "Far too heavy to lift. Sneak + right-click to take hold of the haft: it takes two",
}
for item, name in ITEMS.items():
    flat_item(item)
    lang[f"item.mechanical.{item}"] = name


def facing_states(model):
    return {"variants": {f"facing={f}": dict({"model": model}, **({"y": y} if y else {}))
                         for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}}


def el(frm, to, tex, light=0, faces=("north", "south", "east", "west", "up", "down")):
    """A box element; each face's UV is its size in pixels where that fits in 16, else the whole texture."""
    size = {"north": (0, 1), "south": (0, 1), "east": (2, 1), "west": (2, 1), "up": (0, 2), "down": (0, 2)}
    fs = {}
    for f in faces:
        a, b = size[f]
        wdt, hgt = abs(to[a] - frm[a]), abs(to[b] - frm[b])
        fs[f] = {"uv": [0, 0, wdt, hgt] if wdt <= 16 and hgt <= 16 else [0, 0, 16, 16], "texture": "#" + tex}
    e = {"from": frm, "to": to, "faces": fs}
    if light:
        e["light_emission"] = light
        e["shade"] = False
    return e


# ---- the axe stood on its haft: haft, socket, two flared bits; the runes lit on both flats of each bit
axe = [el([7, 0, 7], [9, 28, 9], "haft"), el([6.5, 21, 6.5], [9.5, 29, 9.5], "blade"),
       el([0, 22, 7.5], [6.5, 28, 8.5], "blade"), el([-5, 19, 7.5], [0, 31, 8.5], "blade"),
       el([9.5, 22, 7.5], [16, 28, 8.5], "blade"), el([16, 19, 7.5], [21, 31, 8.5], "blade")]
for x0, x1 in ((-5, 6.5), (9.5, 21)):
    axe.append(el([x0, 19, 7.48], [x1, 31, 7.48], "runes", 15, ("north",)))
    axe.append(el([x0, 19, 8.52], [x1, 31, 8.52], "runes", 15, ("south",)))
w(A / "models/block/elkwight_axe_trophy.json", {"textures": {"particle": "mechanical:block/elkwight_axe_blade",
    "haft": "mechanical:block/elkwight_axe_haft", "blade": "mechanical:block/elkwight_axe_blade", "runes": "mechanical:block/elkwight_axe_runes"},
    "elements": axe})
w(A / "blockstates/elkwight_axe_trophy.json", facing_states("mechanical:block/elkwight_axe_trophy"))

# ---- the Antler Crown on a wall (facing north: the wall is to the south): skull plate, muzzle, beams, two palms, tines
crown = [el([5, 4, 12], [11, 12, 16], "skull"), el([6, 2, 7], [10, 7, 12], "skull"),
         el([11, 9, 13], [14, 11, 15], "antler"), el([2, 9, 13], [5, 11, 15], "antler"),
         el([13, 10, 13], [31, 21, 14], "antler"), el([-15, 10, 13], [3, 21, 14], "antler")]
for i in range(5):
    crown.append(el([15 + i * 3.5, 21, 13], [16 + i * 3.5, 25 + (i % 2), 14], "antler"))
    crown.append(el([-14 + i * 3.5, 21, 13], [-13 + i * 3.5, 25 + ((i + 1) % 2), 14], "antler"))
w(A / "models/block/elkwight_antler_crown.json", {"textures": {"particle": "mechanical:block/elkwight_antler",
    "antler": "mechanical:block/elkwight_antler", "skull": "mechanical:block/elkwight_skull"}, "elements": crown,
    "display": {"gui": {"rotation": [0, 180, 0], "translation": [0, -2, 0], "scale": [0.36, 0.36, 0.36]},
                "ground": {"translation": [0, 2, 0], "scale": [0.25, 0.25, 0.25]},
                "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]},
                "thirdperson_righthand": {"rotation": [75, 180, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
                "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [0.3, 0.3, 0.3]}}})
w(A / "blockstates/elkwight_antler_crown.json", facing_states("mechanical:block/elkwight_antler_crown"))
w(A / "items/elkwight_antler_crown.json", {"model": {"type": "minecraft:model", "model": "mechanical:block/elkwight_antler_crown"}})
w(D / "mechanical/loot_table/blocks/elkwight_antler_crown.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
    "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": M + "elkwight_antler_crown"}]}],
    "random_sequence": "mechanical:blocks/elkwight_antler_crown"})

# ---- tags
tag("block", "elkwight_breakable", "#minecraft:base_stone_overworld", "#minecraft:dirt", "#minecraft:sand", "minecraft:gravel",
    "#minecraft:leaves", "minecraft:snow", "minecraft:snow_block", "minecraft:powder_snow", "minecraft:clay", "minecraft:mud",
    M + "charred_log", M + "wood_ash", replace=True)  # not basalt/blackstone/magma: his ruin is built of them
tag("block", "minecraft:mineable/pickaxe", M + "elkwight_axe_trophy")
tag("item", "fur_insulation", M + "elkwight_cloak")
tag("item", "minecraft:chest_armor", M + "elkwight_cloak")
tag("item", "equipment/cloak", M + "elkwight_cloak")
tag("item", "equipment/charm", M + "bone_charm")
tag("item", "repairs_elkwight_hide", M + "elkwight_hide", replace=True)
w(A / "equipment/elkwight_hide.json", {"layers": {"humanoid": [{"texture": "mechanical:elkwight_hide"}]}})

# ---- recipes
w(D / "mechanical/recipe/elkwight_cloak.json", {"type": "minecraft:crafting_shapeless", "category": "equipment",
    "ingredients": [M + "elkwight_hide", M + "elkwight_hide", M + "sinew", M + "sinew", M + "cordage"],
    "result": {"id": M + "elkwight_cloak"}})
w(D / "mechanical/recipe/bone_charm.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": [" C ", "BGB", " B "], "key": {"C": M + "cordage", "B": M + "elkwight_bone", "G": "minecraft:gold_nugget"},
    "result": {"id": M + "bone_charm"}})

merge(A / "lang/en_us.json", lang)
print("elkwight ok")
