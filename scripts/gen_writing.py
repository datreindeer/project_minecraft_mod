"""Writing returns in the Bronze Age, with ink made from soot.

- Soot Ink (Mechanical.SOOT_INK): 2 Soot + Tallow or Lard (the binder) -> 2, shapeless; texture (a black ink cake with a
  pressed mark), item model, lang.
- Paper (minecraft:paper, replacing vanilla's): 3 Sugar Cane cut and pressed with a metal knife (Bronze or Iron Knife,
  which stays in the grid as a tool) -> 3. The knife is what makes it Bronze Age.
- Book and Quill (minecraft:writable_book, replacing vanilla's): Book + Soot Ink + Feather (no Ink Sac).
- Book comes back as vanilla's (3 Paper + Leather): gen_age_recipes.py no longer disables paper / book / writable_book,
  and the never-files it left for them are removed here.
- Soot itself is also a light fertiliser (+1, Java: FertilizerItem), nothing to generate for that.
Run after gen_age_recipes.py and gen_soot.py, before gen_unlocks.py: python scripts/gen_writing.py
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    w(path, old)


# ---- Soot Ink: a rounded black cake, lit from the top left, a pressed square on its face
def soot_ink_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    x0, x1, y0, y1 = 3, 12, 4, 11  # face
    for y in range(y0, y1 + 3):
        for x in range(x0, x1 + 1):
            corner = (x in (x0, x1)) and (y in (y0, y1 + 2))
            if corner:
                continue
            if y > y1:  # the cake's front edge, thickness 2
                px[x, y] = (14, 14, 13, 255) if y == y1 + 2 else (20, 20, 18, 255)
            else:
                shade = 34 - (x - x0) - (y - y0)  # lighter top left
                px[x, y] = (shade, shade, shade - 2, 255)
    for x in range(x0 + 1, x1):  # highlight along the top edge
        px[x, y0] = (58, 58, 55, 255)
    for y in range(y0 + 1, y1):
        px[x0, y] = (48, 48, 45, 255)
    for x in range(6, 10):  # the pressed mark
        for y in range(6, 10):
            if x in (6, 9) or y in (6, 9):
                px[x, y] = (12, 12, 11, 255)
    px[7, 7] = px[8, 8] = (44, 44, 42, 255)  # a glint of the sheen
    return img


tex = A / "textures/item/soot_ink.png"
tex.parent.mkdir(parents=True, exist_ok=True)
soot_ink_texture().save(tex)
w(A / "models/item/soot_ink.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/soot_ink"}})
w(A / "items/soot_ink.json", {"model": {"type": "minecraft:model", "model": M + "item/soot_ink"}})

# ---- recipes
w(D / "mechanical/recipe/soot_ink.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": [M + "soot", M + "soot", [M + "tallow", M + "lard"]], "result": {"id": M + "soot_ink", "count": 2}})
w(D / "minecraft/recipe/paper.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:sugar_cane", "minecraft:sugar_cane", "minecraft:sugar_cane", [M + "bronze_knife", M + "iron_knife"]],
    "result": {"id": "minecraft:paper", "count": 3}})
w(D / "minecraft/recipe/writable_book.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
    "ingredients": ["minecraft:book", M + "soot_ink", "minecraft:feather"], "result": {"id": "minecraft:writable_book", "count": 1}})
# vanilla's unlock advancements for these fit again (sugar cane, a book); drop the never-overrides gen_age_recipes.py left
for name in ("paper", "book", "writable_book"):
    adv = D / f"minecraft/advancement/recipes/misc/{name}.json"
    if adv.exists() and "neoforge:never" in adv.read_text(encoding="utf-8"):
        adv.unlink()
book = D / "minecraft/recipe/book.json"
if book.exists() and "neoforge:never" in book.read_text(encoding="utf-8"):
    book.unlink()

merge(A / "lang/en_us.json", {"item.mechanical.soot_ink": "Soot Ink"})
print("writing ok")
