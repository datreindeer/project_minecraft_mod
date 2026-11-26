"""Tool rod sprites for the dynamic tools: the Iron Tool Rod's drawing (same diagonal, edge / two highlights / shadow)
recoloured per material, plus item definitions, models and lang. Run: python scripts/gen_tool_rods.py"""
import json
from pathlib import Path
from PIL import Image

R = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"
IRON = {"444444": 0, "d8d8d8": 1, "c1c1c1": 2, "181818": 3}  # edge, highlight, mid, shadow
# edge, highlight, mid, shadow
PALETTES = {
    "bone": ("a69a7c", "f4f1e2", "d9d2b6", "5e5440"),
    "copper": ("9c4e31", "fc9982", "d66d48", "421c11"),
    "bronze": ("7e5721", "e2bf71", "c59949", "34200c"),
    "steel": ("3a3f46", "9aa3ae", "747d89", "14171b"),
}
NAMES = {"bone": "Bone", "copper": "Copper", "bronze": "Bronze", "steel": "Steel"}

src = Image.open(R / "textures/item/iron_tool_rod.png").convert("RGBA")
for mat, pal in PALETTES.items():
    img = Image.new("RGBA", src.size)
    for x in range(16):
        for y in range(16):
            r, g, b, a = src.getpixel((x, y))
            if a:
                img.putpixel((x, y), tuple(int(pal[IRON["%02x%02x%02x" % (r, g, b)]][i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    if mat == "bone":  # a knuckle at each end where the bone was cut
        for x, y, c in ((13, 2, 1), (14, 3, 1), (2, 13, 2), (3, 13, 2), (2, 14, 3), (3, 14, 3)):
            img.putpixel((x, y), tuple(int(pal[c][i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    name = f"{mat}_tool_rod"
    img.save(R / f"textures/item/{name}.png")
    (R / f"items/{name}.json").write_text(json.dumps({"model": {"type": "minecraft:model", "model": f"mechanical:item/{name}"}}, indent=2) + "\n")
    (R / f"models/item/{name}.json").write_text(json.dumps({"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{name}"}}, indent=2) + "\n")

# recipes: bone from a knife over a bone (the knife wears as in every knife recipe); copper and bronze drawn out of ingots (no mold shape or anvil pattern for them: simplest honest path).
# Steel has no recipe file: it is forged on the anvil (Smithing.Pattern.TOOL_ROD), like the iron one.
D = R.parent.parent / "data/mechanical/recipe"
def shaped(name, pattern, key, count=1):
    (D / f"{name}.json").write_text(json.dumps({"type": "minecraft:crafting_shaped", "category": "equipment", "pattern": pattern, "key": key,
                                                "result": {"id": f"mechanical:{name}", "count": count}}, indent=2) + "\n")
# shaped, knife above two bones: one Bone + knife in any layout is already the Bone Needle
shaped("bone_tool_rod", ["K", "B", "B"], {"K": "#mechanical:knives", "B": "minecraft:bone"})
# two ingots in a column draw out two rods (one ingot alone is the nugget recipe)
shaped("copper_tool_rod", ["I", "I"], {"I": "minecraft:copper_ingot"}, 2)
shaped("bronze_tool_rod", ["I", "I"], {"I": "mechanical:bronze_ingot"}, 2)

# lang: only our keys, inserted after the Iron Tool Rod's line
lang = R / "lang/en_us.json"
text = lang.read_bytes().decode("utf-8")
nl = "\r\n" if "\r\n" in text else "\n"
anchor = '  "item.mechanical.iron_tool_rod": "Iron Tool Rod",' + nl
if '"item.mechanical.bone_tool_rod"' not in text:
    text = text.replace(anchor, anchor + "".join(f'  "item.mechanical.{m}_tool_rod": "{n} Tool Rod",' + nl for m, n in NAMES.items()), 1)
    lang.write_bytes(text.encode("utf-8"))
