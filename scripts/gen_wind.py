"""Windy nights (Wind.java): the Unlit Torch / Unlit Wall Torch blocks a gust leaves behind, with a charred-tip texture
made from vanilla's torch (the 2x2 flame becomes burnt black wood, the wood under it scorched), their models,
blockstates and loot (they drop the torch), and the lang lines. Run after gen_data.py: python scripts/gen_wind.py"""
import io
import json
import sys
import zipfile
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_crude_ranged_textures import JAR  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data/mechanical"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


# ---- texture: charred tip instead of the flame ----
torch = Image.open(io.BytesIO(zipfile.ZipFile(JAR).read("assets/minecraft/textures/block/torch.png"))).convert("RGBA")
CHAR = {(7, 6): (52, 47, 44), (8, 6): (34, 30, 28), (7, 7): (70, 62, 55), (8, 7): (44, 39, 35)}  # burnt, lit side lighter
out = torch.copy()
for (x, y), c in CHAR.items():
    out.putpixel((x, y), c + (255,))
for x in (7, 8):  # wood just under the tip scorched darker
    r, g, b, a = out.getpixel((x, 8))
    out.putpixel((x, 8), (int(r * 0.55), int(g * 0.5), int(b * 0.45), a))
out.save(A / "textures/block/unlit_torch.png")

# ---- models, blockstates, loot ----
w(A / "models/block/unlit_torch.json", {"parent": "minecraft:block/template_torch", "textures": {"torch": "mechanical:block/unlit_torch"}})
w(A / "models/block/unlit_wall_torch.json", {"parent": "minecraft:block/template_torch_wall", "textures": {"torch": "mechanical:block/unlit_torch"}})
w(A / "blockstates/unlit_torch.json", {"variants": {"": {"model": "mechanical:block/unlit_torch"}}})
w(A / "blockstates/unlit_wall_torch.json", {"variants": {f"facing={f}": {"model": "mechanical:block/unlit_wall_torch", **({"y": y} if y else {})}
                                                         for f, y in (("east", 0), ("south", 90), ("west", 180), ("north", 270))}})
for block in ("unlit_torch", "unlit_wall_torch"):
    w(D / f"loot_table/blocks/{block}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
        "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": "minecraft:torch"}]}],
        "random_sequence": f"mechanical:blocks/{block}"})

# ---- lang ----
lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({
    "block.mechanical.unlit_torch": "Unlit Torch",
    "block.mechanical.unlit_wall_torch": "Unlit Torch",
    "message.mechanical.wind": "The wind is rising... fires in the open won't last tonight.",
})
lang_path.write_text(json.dumps(lang, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
print("wind ok")
