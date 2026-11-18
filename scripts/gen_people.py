"""Data for the Stone Age people (com.mechanical.people): camp and hunting band worldgen placement, the spawn egg
(model + texture) and lang. Skins: scripts/gen_tribe_skins.py. Run: python scripts/gen_people.py"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_wildlife_item_textures import OUT as ITEM_TEX, egg  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
D = ROOT / "data/mechanical"
A = ROOT / "assets/mechanical"
LANG = A / "lang/en_us.json"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def anyof(*sets):
    return {"type": "neoforge:or", "values": list(sets)}


# name: (biomes, 1-in-N chunks). Camps need flat dry ground and bail out often, so the real rate is lower than 1/N.
FEATURES = {
    "tribe_camp": (anyof("#minecraft:is_forest", "#minecraft:is_taiga",
                         ["minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:river"]), 150),
    "hunting_band": (anyof("#minecraft:is_forest", "#minecraft:is_taiga"), 200),
}
for name, (biomes, rarity) in FEATURES.items():
    w(D / f"worldgen/feature/{name}.json", {"type": f"mechanical:{name}"})
    w(D / f"worldgen/placed_feature/{name}.json", {"feature": f"mechanical:{name}", "placement": [
        {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"}, {"type": "minecraft:biome"}]})
    w(D / f"neoforge/biome_modifier/add_{name}.json", {"type": "neoforge:add_features", "biomes": biomes,
        "features": f"mechanical:{name}", "step": "surface_structures"})

# spawn egg: a weathered face in a fur hood
w(A / "models/item/tribesperson_spawn_egg.json", {"parent": "minecraft:item/generated",
    "textures": {"layer0": "mechanical:item/tribesperson_spawn_egg"}})
w(A / "items/tribesperson_spawn_egg.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/tribesperson_spawn_egg"}})
SK, SKD, EY, HR, BN = "#bf8a5f", "#9a6a44", "#2a1c12", "#3b2617", "#e8dfc8"
paint = [(x, y, SK) for x in range(5, 11) for y in range(6, 11)]
paint += [(x, 5, HR) for x in range(5, 11)] + [(6, 7, EY), (9, 7, EY), (6, 6, HR), (9, 6, HR)]
paint += [(7, 8, SKD), (8, 8, SKD), (7, 9, "#6a3a2a"), (8, 9, "#6a3a2a"), (5, 8, "#9c3420"), (10, 8, "#9c3420")]
paint += [(x, 11, BN if x % 2 else "#8e7550") for x in range(5, 11)]
egg(["#b0a596", "#8f8578", "#726a60", "#5a3822", "#4a3020", "#2e1e14"], (), (), paint).save(ITEM_TEX / "tribesperson_spawn_egg.png")

lang = json.loads(LANG.read_text(encoding="utf-8"))
lang.update({
    "entity.mechanical.tribesperson": "Tribesperson",
    "entity.mechanical.tribesperson.named": "%s the %s",
    "entity.mechanical.tribesperson.role.hunter": "Hunter",
    "entity.mechanical.tribesperson.role.gatherer": "Gatherer",
    "entity.mechanical.tribesperson.role.fisher": "Fisher",
    "entity.mechanical.tribesperson.role.woodcutter": "Woodcutter",
    "entity.mechanical.tribesperson.role.elder": "Elder",
    "item.mechanical.tribesperson_spawn_egg": "Tribesperson Spawn Egg",
    "chat.mechanical.tribesperson.says": "<%s> %s",
    "chat.mechanical.tribesperson.greet.hunter.0": "Good hunting, stranger.",
    "chat.mechanical.tribesperson.greet.hunter.1": "Tread softly. The deer hear everything.",
    "chat.mechanical.tribesperson.greet.hunter.2": "The herd went that way. Follow the blood, not the tracks.",
    "chat.mechanical.tribesperson.greet.gatherer.0": "The berries are sweet this moon.",
    "chat.mechanical.tribesperson.greet.gatherer.1": "Leave some for the bears, stranger.",
    "chat.mechanical.tribesperson.greet.gatherer.2": "Not the red mushroom with white spots. Never that one.",
    "chat.mechanical.tribesperson.greet.fisher.0": "Quiet. You will scare the fish.",
    "chat.mechanical.tribesperson.greet.fisher.1": "The river gives, if you are patient.",
    "chat.mechanical.tribesperson.greet.fisher.2": "A bone hook and a good line. That is all you need.",
    "chat.mechanical.tribesperson.greet.woodcutter.0": "Mind the falling trees.",
    "chat.mechanical.tribesperson.greet.woodcutter.1": "The fire is always hungry.",
    "chat.mechanical.tribesperson.greet.woodcutter.2": "Strong arms, sharp flint. Good wood.",
    "chat.mechanical.tribesperson.greet.elder.0": "Welcome, stranger. The fire is warm, sit with us.",
    "chat.mechanical.tribesperson.greet.elder.1": "Bring me what you have. We will trade.",
    "chat.mechanical.tribesperson.greet.elder.2": "I have seen many winters. This one will be hard.",
    "chat.mechanical.tribesperson.greet.night": "Stay close to the fire. Things walk in the dark.",
    "chat.mechanical.tribesperson.angry": "You will regret that!",
    "chat.mechanical.tribesperson.killer": "Murderer! You will never be welcome here!",
    "chat.mechanical.tribesperson.no_trade": "We do not trade with you.",
})
w(LANG, lang)
print(f"{len(FEATURES)} features, lang ok")
