"""Vanilla recipes a stone-age player could craft from what they can find, but that belong to a later age.

Each one is disabled with a never-condition (recipe + its unlock advancement; registry loading fails without the latter).
Recipes disabled elsewhere (gen_data.py: tools, armour, beds, furnace, bow, arrow, candle ...) are not repeated here.
To bring one back in its age, delete its row (and the generated files).
Run: python scripts/gen_age_recipes.py
"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data/minecraft"
NEVER = {"neoforge:conditions": [{"type": "neoforge:never"}]}

# name, advancement category (None = special recipe without one), why, age it returns in
RECIPES = [
    ("tnt", "redstone", "gunpowder (creepers) + sand: explosives", "Steam"),
    ("firework_rocket_simple", "misc", "gunpowder + paper: rockets", "Steam"),
    ("firework_rocket", None, "rockets with stars", "Steam"),
    ("firework_star", None, "gunpowder + dye", "Steam"),
    ("firework_star_fade", None, "firework star colours", "Steam"),
    ("leather_horse_armor", "misc", "horse armour; riding comes later", "Bronze"),
    ("wolf_armor", "combat", "armadillo scutes (shed naturally); animal armour comes later", "Bronze"),
    ("spectral_arrow", "combat", "glowstone dust (witches) + arrow: glowing magic arrows", "Iron"),
    ("conduit", "misc", "nautilus shells (fishing) + heart of the sea (buried treasure): ocean magic", "Electric"),
    ("respawn_anchor", "decorations", "crying obsidian (ruined portal chests) + glowstone (witches): Nether tech", "Iron"),
    ("wind_charge", "misc", "breeze rods (trial chambers)", "Iron"),
    ("mace", "combat", "heavy core (ominous vault) + breeze rod", "Iron"),
    ("cartography_table", "decorations", "paper + planks; maps and compasses come later", "Iron"),
    ("fermented_spider_eye", "brewing", "only used for brewing", "Iron"),
    ("copper_ingot", "misc", "copper blocks in trial chambers (stone pickaxe) -> 9 ingots each, skips smelting", "Bronze"),
    ("copper_ingot_from_waxed_copper_block", "misc", "as above, waxed copper blocks", "Bronze"),
]

for name, cat, _why, _age in RECIPES:
    (D / f"recipe/{name}.json").write_text(json.dumps(NEVER, indent=2) + "\n")
    if cat:
        p = D / f"advancement/recipes/{cat}/{name}.json"
        p.parent.mkdir(parents=True, exist_ok=True)
        p.write_text(json.dumps(NEVER, indent=2) + "\n")

assert len({r[0] for r in RECIPES}) == len(RECIPES), "duplicate recipe name"
print(f"{len(RECIPES)} later-age recipes disabled")
