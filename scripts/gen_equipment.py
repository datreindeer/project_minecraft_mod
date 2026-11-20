"""Accessory slots in the player inventory (com.mechanical.equipment: Equipment, Accessory, PackMenu, Quiver, CloakItem;
docs/ages/01-stone-age.md "Utrustning").

- item tags mechanical:equipment/<slot> = what each slot takes: cloak (Otter Fur Cape, Ermine Mantle, Glider Cloak),
  back (Leather Satchel), hip (Quiver), neck / ring / charm (empty for now: later ages add theirs)
- the cloaks stay in #minecraft:chest_armor: that tag is what makes them enchantable like a chest piece (Protection,
  Thorns, Unbreaking, Mending), and Equipment applies those enchantments from the Cloak slot
- Quiver: item model + definition, lang. Sewn at the Leatherworking Bench (LeatherworkingRecipes, Java)
- lang: the B key, slot names for the "Worn in the ... slot" tooltip, the message when nothing is worn to open
Textures: scripts/gen_equipment_textures.py. Advancements (worn_satchel, quiver, worn_cloak): scripts/gen_advancements.py.
Generator order: ... gen_pots.py, gen_equipment.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_equipment.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


SLOTS = {
    "cloak": [M + "otter_fur_cape", M + "ermine_mantle", M + "glider_cloak"],
    "neck": [],
    "back": [M + "leather_satchel"],
    "ring": [],
    "hip": [M + "quiver"],
    "charm": [],
}
for slot, items in SLOTS.items():
    w(D / f"mechanical/tags/item/equipment/{slot}.json", {"values": items})
tag_add(D / "minecraft/tags/item/chest_armor.json", *SLOTS["cloak"])

# ---------------------------------------------------------------- quiver
w(A / "models/item/quiver.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/quiver"}})
w(A / "items/quiver.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/quiver"}})

# ---------------------------------------------------------------- lang
lang = {
    "item.mechanical.quiver": "Quiver",
    "key.mechanical.open_pack": "Open Satchel & Quiver",
    "message.mechanical.nothing_to_open": "Nothing to open: wear a Leather Satchel (Back slot) or a Quiver (Hip slot)",
    "tooltip.mechanical.worn_in": "Worn in the %s slot",
    "slot.mechanical.cloak": "Cloak",
    "slot.mechanical.neck": "Neck",
    "slot.mechanical.back": "Back",
    "slot.mechanical.ring": "Ring",
    "slot.mechanical.hip": "Hip",
    "slot.mechanical.charm": "Charm",
}
lang_path = A / "lang/en_us.json"
full = json.loads(lang_path.read_text(encoding="utf-8"))
full.update(lang)
lang_path.write_text(json.dumps(full, indent=2) + "\n", encoding="utf-8")
print(f"equipment: {len(SLOTS)} slot tags, quiver, {len(lang)} lang keys")
