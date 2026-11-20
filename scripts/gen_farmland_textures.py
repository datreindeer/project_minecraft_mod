# Farmland shows its fertility (Fertility.java, 0-3): exhausted soil is pale and grey, a new field (2) looks like
# vanilla, rich soil (3) is dark humus with flecks of compost. Recolours vanilla's farmland tops (dry and moist) and
# overrides minecraft:farmland's blockstate with fertility x moisture variants. Standalone, like the other texture scripts.
# usage: python scripts/gen_farmland_textures.py
import io, json, random, zipfile
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
A = ROOT / "src/main/resources/assets"
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
z = zipfile.ZipFile(JAR)


def mix(c, to, t):
    return tuple(round(a + (b - a) * t) for a, b in zip(c[:3], to)) + (c[3],)


PALE, HUMUS = (168, 150, 124), (38, 26, 18)
FLECKS = [(92, 84, 40), (70, 78, 34), (120, 96, 52)]  # straw and compost bits


def tex(moist, fertility):
    img = Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/block/farmland{moist}.png"))).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            c = px[x, y]
            if fertility == 0: c = mix(c, PALE, 0.5)
            elif fertility == 1: c = mix(c, PALE, 0.3)
            elif fertility == 3: c = mix(c, HUMUS, 0.35)
            px[x, y] = c
    if fertility == 3:
        rng = random.Random(3)
        for _ in range(9):
            x, y = rng.randrange(16), rng.randrange(16)
            px[x, y] = FLECKS[rng.randrange(3)] + (255,)
    return img


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


variants = {}
for f in range(4):
    for moist in ("", "_moist"):
        if f == 2:
            model = f"minecraft:block/farmland{moist}"  # a new field: vanilla's look
        else:
            name = f"farmland{moist}_fertility{f}"
            tex(moist, f).save(A / f"mechanical/textures/block/{name}.png")
            w(A / f"mechanical/models/block/{name}.json", {"parent": "minecraft:block/template_cube_bottom_top_indented", "textures": {
                "bottom": "minecraft:block/dirt", "side": "minecraft:block/dirt", "top": f"mechanical:block/{name}"}})
            model = f"mechanical:block/{name}"
        for m in range(8):
            if (m == 7) == (moist == "_moist"):
                variants[f"fertility={f},moisture={m}"] = {"model": model}
w(A / "minecraft/blockstates/farmland.json", {"variants": variants})
