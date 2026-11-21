"""Limestone building blocks (IronOres.java): the vanilla Tuff family copied file for file with tuff -> limestone.
Polished Limestone, Limestone Bricks, Chiseled Limestone, Chiseled Limestone Bricks, and slab / stairs / wall of Limestone,
Polished Limestone and Limestone Bricks. Copies blockstates, models, item models, loot tables and recipes (crafting and
stonecutting) from the merged jar, adds the block tags (pickaxe, slabs, stairs, walls), lang and the textures (vanilla tuff
textures recoloured into the Limestone palette). The plain Limestone block keeps its own files (gen_iron_ores.py).
The Bronze Chisel carves Limestone -> Polished -> Bricks -> Chiseled Bricks (BronzeTools.ChiselItem).
Run after gen_iron_ores.py: python scripts/gen_limestone_blocks.py"""
import glob
import io
import json
import re
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])
# the same palette as the Limestone block (gen_iron_ores_textures.py)
LIMESTONE = [(122, 114, 96), (150, 142, 122), (176, 168, 146), (198, 190, 168), (218, 212, 192)]


def rename(text):
    """minecraft:...tuff... -> mechanical:...limestone... (only ids that name a tuff block, item, model, texture or loot table)."""
    return re.sub(r"minecraft:([a-z_/]*tuff[a-z_]*)", lambda m: "mechanical:" + m[1].replace("tuff", "limestone"), text)


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(json.loads(text), indent=2) + "\n", encoding="utf-8")


names = []
for src in jar.namelist():
    m = re.fullmatch(r"(assets|data)/minecraft/(blockstates|models/block|items|loot_table/blocks|recipe)/([a-z_]*tuff[a-z_]*)\.json", src)
    if not m or m[3] == "tuff":  # plain Limestone has its own files
        continue
    new = m[3].replace("tuff", "limestone")
    write(RES / m[1] / "mechanical" / m[2] / f"{new}.json", rename(jar.read(src).decode()))
    if m[2] == "blockstates":
        names.append(new)
names.sort()
assert len(names) == 13, names


# ---- tags ----
def tag_add(rel, values):
    path = RES / "data" / rel
    tag = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    tag["values"] += [v for v in values if v not in tag["values"]]
    write(path, json.dumps(tag))


ids = ["mechanical:" + n for n in names]
tag_add("minecraft/tags/block/mineable/pickaxe.json", ids)
for kind, tag in (("slab", "slabs"), ("stairs", "stairs"), ("wall", "walls")):
    group = [i for i in ids if i.endswith("_" + kind)]
    tag_add(f"minecraft/tags/block/{tag}.json", group)
    tag_add(f"minecraft/tags/item/{tag}.json", group)

# ---- lang ----
lang_path = RES / "assets/mechanical/lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
lang.update({f"block.mechanical.{n}": n.replace("_", " ").title() for n in names})
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")


# ---- textures: tuff recoloured by brightness onto the limestone ramp (one shared range so the family matches) ----
def ramp(t):
    t = max(0.0, min(1.0, t)) * (len(LIMESTONE) - 1)
    i = min(int(t), len(LIMESTONE) - 2)
    f = t - i
    return tuple(int(LIMESTONE[i][c] * (1 - f) + LIMESTONE[i + 1][c] * f) for c in range(3))


TEX = ["polished_tuff", "tuff_bricks", "chiseled_tuff", "chiseled_tuff_top", "chiseled_tuff_bricks", "chiseled_tuff_bricks_top"]
imgs = {t: Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{t}.png"))).convert("RGBA") for t in TEX + ["tuff"]}
base = [sum(imgs["tuff"].getpixel((x, y))[:3]) / 3 for y in range(16) for x in range(16)]
lo, hi = min(base), max(base)
for t in TEX:
    out = imgs[t].copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if a:
                out.putpixel((x, y), ramp(((r + g + b) / 3 - lo) / (hi - lo)) + (a,))
    out.save(RES / f"assets/mechanical/textures/block/{t.replace('tuff', 'limestone')}.png")

print("limestone blocks ok:", len(names), "blocks")
