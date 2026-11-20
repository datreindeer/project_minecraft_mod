# Farmland shows its fertility (Fertility.java, 0-3): exhausted soil is pale and grey, a new field (2) looks like
# vanilla, rich soil (3) is dark humus with flecks of compost. Recolours vanilla's farmland tops (dry and moist) and
# overrides minecraft:farmland's blockstate (multipart): fertility x moisture, plus four corner posts and a twine line
# when Crop Stakes stand on it (Fertility.STAKED). Also the Crop Stakes item texture. Standalone, like the other texture scripts.
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


parts = []
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
        parts.append({"when": {"fertility": str(f), "moisture": "7" if moist else "0|1|2|3|4|5|6"}, "apply": {"model": model}})


# ---- Crop Stakes: posts at the corners of the crop above, twine run round them
def el(lo, hi, t):
    dx, dy, dz = (hi[i] - lo[i] for i in range(3))
    uv = {"north": [0, 0, dx, dy], "south": [0, 0, dx, dy], "east": [0, 0, dz, dy], "west": [0, 0, dz, dy],
          "up": [0, 0, dx, dz], "down": [0, 0, dx, dz]}
    return {"from": lo, "to": hi, "faces": {d: {"texture": t, "uv": [min(v, 16) for v in u]} for d, u in uv.items()}}


POSTS = [el([x, 15, z], [x + 1.5, 29, z + 1.5], "#stake") for x in (0.25, 14.25) for z in (0.25, 14.25)]
TWINE = [el([1.75, 25, 0.75], [14.25, 25.5, 1.25], "#twine"), el([1.75, 25, 14.75], [14.25, 25.5, 15.25], "#twine"),
         el([0.75, 25, 1.75], [1.25, 25.5, 14.25], "#twine"), el([14.75, 25, 1.75], [15.25, 25.5, 14.25], "#twine")]
w(A / "mechanical/models/block/crop_stakes.json", {"textures": {"particle": "mechanical:block/snare_stake",
    "stake": "mechanical:block/snare_stake", "twine": "mechanical:block/snare_twine"}, "elements": POSTS + TWINE})
parts.append({"when": {"staked": "true"}, "apply": {"model": "mechanical:block/crop_stakes"}})
w(A / "minecraft/blockstates/farmland.json", {"multipart": parts})

# item: three stakes with a twine wrap
ITEM = ["................", "..a....a....a...", "..b....b....b...", "..b....b....b...", "..c....c....c...",
        ".tttttttttttttt.", "..c....c....c...", "..b....b....b...", "..b....b....b...", "..c....c....c...",
        "..b....b....b...", "..b....b....b...", "..c....c....c...", "..b....b....b...", "..d....d....d...", "................"]
PAL = {"a": (196, 160, 112), "b": (140, 102, 62), "c": (110, 78, 46), "d": (84, 60, 36), "t": (214, 196, 150)}
img = Image.new("RGBA", (16, 16))
for y, row in enumerate(ITEM):
    for x, ch in enumerate(row):
        if ch != ".": img.putpixel((x, y), PAL[ch] + (255,))
img.save(A / "mechanical/textures/item/crop_stakes.png")
