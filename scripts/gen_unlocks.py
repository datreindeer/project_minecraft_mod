"""Recipe book unlocks for every recipe that has none: an advancement that hands out the recipe the first time the player
carries one of its telling ingredients (common ones like planks, sticks and twine only count when the recipe has nothing
else). Without this, recipes such as the Bellows never show up in the recipe book.

Run after every generator that writes recipes, before gen_advancements.py and gen_registry.py: python scripts/gen_unlocks.py
"""
import json
from pathlib import Path

D = Path(__file__).resolve().parent.parent / "src/main/resources/data"
OUT = D / "mechanical/advancement/recipes/auto"
COMMON = {"#minecraft:planks", "minecraft:stick", "#minecraft:logs", "#minecraft:logs_that_burn", "mechanical:twine",
          "mechanical:plant_fibre", "mechanical:cordage", "minecraft:clay_ball"}
# vanilla recipes the mod changed whose vanilla unlock no longer fits (it waits for string / hay / an Iron Ingot)
VANILLA = {"minecraft:lead", "minecraft:straw_bed"}


def ingredients(recipe):
    """Every ingredient of a recipe as item ids / #tags (lists of alternatives flattened)."""
    raw = list(recipe.get("key", {}).values()) + recipe.get("ingredients", [])
    for k in ("ingredient", "base", "addition", "template"):
        if k in recipe:
            raw.append(recipe[k])
    out = []
    for r in raw:
        for one in r if isinstance(r, list) else [r]:
            while isinstance(one, dict):  # a NeoForge ingredient (components, difference): the item it is made of
                one = one.get("items", one.get("base"))
            if isinstance(one, str) and one not in out:
                out.append(one)
    return out


rewarded = set()
for f in D.glob("*/advancement/**/*.json"):
    if OUT in f.parents:
        continue
    rewarded.update(json.loads(f.read_text(encoding="utf-8")).get("rewards", {}).get("recipes", []))

if OUT.exists():
    for f in OUT.glob("*.json"):
        f.unlink()
OUT.mkdir(parents=True, exist_ok=True)
made = 0
for f in sorted(D.glob("*/recipe/**/*.json")):
    rel = f.relative_to(D).parts
    rid = f"{rel[0]}:{'/'.join(rel[2:])[:-5]}"
    recipe = json.loads(f.read_text(encoding="utf-8"))
    if "type" not in recipe or rid in rewarded or not (rid.startswith("mechanical:") or rid in VANILLA):
        continue
    ings = ingredients(recipe)
    telling = [i for i in ings if i not in COMMON] or ings
    if not telling:
        continue
    criteria = {f"has_{n}": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": i}]}}
                for n, i in enumerate(telling)}
    criteria["has_the_recipe"] = {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipes": rid}}
    adv = {"parent": "minecraft:recipes/root", "criteria": criteria, "requirements": [list(criteria)],
           "rewards": {"recipes": [rid]}}
    (OUT / f"{rid.replace(':', '_').replace('/', '_')}.json").write_text(json.dumps(adv, indent=2) + "\n")
    made += 1
assert made == len(list(OUT.glob("*.json")))
print(f"{made} recipe unlocks")
