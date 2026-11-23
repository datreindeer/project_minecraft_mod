"""Paint the deep fauna (scripts/gen_fauna_deep.py, com.mechanical.wildlife.deep): entity textures (and the spider's
skinned version for its carcass), the Cave Web block and every 16x16 item.

Usage: python scripts/gen_fauna_deep_textures.py [--java]
- Models, part trees mirroring the Java (client/wildlife/deep/DeepModels): the Cave Salamander (olm: pale pink, red
  feathery gills, no eyes), the Blind Cave Fish (pale, eyeless), one bat of the Bat Swarm (the model repeats it), the
  Cavern Spider (grey, a pale band pattern on the abdomen, small dull eyes: nothing glows, the caves stay pitch black).
  --java prints each model's createBodyLayer() parts so the Java stays in step with the paint.
- Items: hand-drawn (olm, spider leg, silk gland), recoloured from house shapes (cave fish from the pike, the
  fang from the centipede fang), the broth from vanilla's mushroom stew bowl, spawn eggs (gen_wildlife_item_textures.egg).
Deterministic: hashes only, no randomness.
"""
import io
import math
import sys
import zipfile
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from critter_paint import cube, fur, h, pack, paint, part, save, shade, skinned  # noqa: E402
from gen_wildlife_item_textures import egg, hexc, recolour  # noqa: E402

TEX = HERE.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
JAR = HERE.parent / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"


def f(v):
    return f"{float(v)}F"


def java_parts(parts, uv):
    """createBodyLayer() lines: every part, its cubes (texOffs from pack()) and pose."""
    out = []
    for p in parts:
        bits = []
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            plane = ", new CubeDeformation(0.001F)" if 0 in (w, hh, d) else ""
            bits.append(f".texOffs({u}, {v})" + (".mirror()" if c["mirror"] else "")
                        + f".addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(hh)}, {f(d)}{plane})" + (".mirror(false)" if c["mirror"] else ""))
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = (f"PartPose.offset({f(px)}, {f(py)}, {f(pz)})" if (rx, ry, rz) == (0, 0, 0)
                else f"PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})")
        parent = "root" if p["parent"] is None else p["parent"]
        out.append(f'PartDefinition {p["name"]} = {parent}.addOrReplaceChild("{p["name"]}", CubeListBuilder.create(){"".join(bits)}, {pose});')
    return out


# ======================================================================= cave salamander (olm)

SA = dict(skin=(230, 192, 186), pale=(244, 216, 210), belly=(248, 230, 224), shade=(206, 166, 162), gill=(206, 72, 80),
          gill_dark=(158, 42, 54))


def salamander():
    def skin(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return SA["belly"]
        if face == "top" and h(tx, ty, 2001) < 0.12:
            return SA["shade"]                                       # faint mottling: the skin shows the blood beneath
        return fur(SA["pale"] if face == "top" else SA["skin"], tx, ty, 2002, 0.05, streak=False)

    def gill(face, X, Y, Z, n, tx, ty):
        return SA["gill_dark"] if (tx + ty) % 2 else SA["gill"]      # feathery fronds

    def fin(face, X, Y, Z, n, tx, ty):
        return (*SA["pale"], 170)

    return [
        part("body", None, (0, 24, 0), (0, 0, 0), [cube("sa_body", -1, -1, -3, 2, 1, 6, skin)]),
        part("head", "body", (0, 0, -3), (0, 0, 0), [cube("sa_head", -1, -1, -3, 2, 1, 3, skin)]),
        part("right_gill", "head", (-1, -0.5, -0.5), (0, 0, 0), [cube("sa_gill", -1, -1.5, 0, 1, 2, 0, gill)]),
        part("left_gill", "head", (1, -0.5, -0.5), (0, 0, 0), [cube("sa_gill", 0, -1.5, 0, 1, 2, 0, gill, True)]),
        part("tail", "body", (0, 0, 3), (0, 0, 0), [cube("sa_tail", -0.5, -1, 0, 1, 1, 5, skin), cube("sa_fin", 0, -2, 1, 0, 1, 4, fin)]),
        part("right_front_leg", "body", (-1, -1, -2), (0, 0, 0), [cube("sa_leg", -1, 0, -0.5, 1, 1, 1, skin)]),
        part("left_front_leg", "body", (1, -1, -2), (0, 0, 0), [cube("sa_leg", 0, 0, -0.5, 1, 1, 1, skin, True)]),
        part("right_hind_leg", "body", (-1, -1, 2), (0, 0, 0), [cube("sa_leg", -1, 0, -0.5, 1, 1, 1, skin)]),
        part("left_hind_leg", "body", (1, -1, 2), (0, 0, 0), [cube("sa_leg", 0, 0, -0.5, 1, 1, 1, skin, True)]),
    ]


# ======================================================================= blind cave fish

CF = dict(back=(214, 182, 178), side=(236, 208, 202), belly=(248, 234, 228), fin=(238, 214, 208, 180), gill=(222, 150, 150))


def cave_fish():
    def body(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return CF["belly"]
        if face in ("left", "right") and abs(Z - (-1.5)) < 0.5 and Y > 20.5:
            return CF["gill"]                                        # the pink gill slit; no eye where an eye would be
        return fur(CF["back"] if face == "top" or Y < 21 else CF["side"], tx, ty, 2010, 0.05, streak=False)

    def fin(face, X, Y, Z, n, tx, ty):
        return CF["fin"] if h(tx, ty, 2011) < 0.75 else (*shade(CF["fin"], 0.85), 180)

    return [
        part("body", None, (0, 22, 0), (0, 0, 0), [cube("cf_body", -1, -1.5, -2.5, 2, 3, 5, body), cube("cf_dorsal", 0, -2.5, -1, 0, 1, 3, fin)]),
        part("tail", "body", (0, 0, 2.5), (0, 0, 0), [cube("cf_tail", 0, -1.5, 0, 0, 3, 3, fin)]),
    ]


# ======================================================================= one bat (BatSwarmModel repeats it)

BAT = dict(body=(74, 56, 44), dark=(46, 34, 28), wing=(58, 44, 38), bone=(98, 78, 64))


def bat():
    def body(face, X, Y, Z, n, tx, ty):
        return BAT["dark"] if face == "front" else fur(BAT["body"], tx, ty, 2020, 0.08)

    def wing(face, X, Y, Z, n, tx, ty):
        return BAT["bone"] if (tx % 3 == 0) else BAT["wing"]       # finger bones through the membrane

    return [
        part("bat", None, (0, 18, 0), (0, 0, 0), [cube("bat_body", -0.5, -0.5, -1, 1, 1, 2, body)]),
        part("right_wing", "bat", (-0.5, 0, 0), (0, 0, 0), [cube("bat_wing", -3, 0, -1, 3, 0, 2, wing)]),
        part("left_wing", "bat", (0.5, 0, 0), (0, 0, 0), [cube("bat_wing", 0, 0, -1, 3, 0, 2, wing, True)]),
    ]


# ======================================================================= cavern spider

SP = dict(grey=(96, 92, 86), dark=(50, 46, 44), pale=(178, 170, 154), leg=(78, 74, 70), joint=(124, 118, 108), eye=(30, 28, 26),
          fang=(36, 28, 26))
LEG_Z = (-2, -1, 0, 1)
LEG_REST = [(-0.5, -0.6), (-0.45, -0.25), (-0.45, 0.25), (-0.5, 0.6)]  # right legs, front to back (zRot, yRot); the left ones mirrored


def spider():
    def thorax(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return SP["dark"]
        return fur(SP["grey"], tx, ty, 2030, 0.1)

    def head(face, X, Y, Z, n, tx, ty):
        if face == "front" and Y < 18.5 and abs(abs(X) - 1.0) < 0.6:
            return SP["eye"]                                         # small, dull, dark eyes: they don't glow
        return fur(shade(SP["grey"], 0.9), tx, ty, 2031, 0.1)

    def abdomen(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return SP["dark"]
        # pale chevrons down the back
        if face == "top" and abs(abs(X) - (Z - 2) * 0.35) < 0.6 and int(Z) % 2 == 0:
            return SP["pale"]
        if face == "top" and abs(X) < 0.5 and Z > 2.5:
            return shade(SP["pale"], 0.85)
        return fur(SP["grey"] if face == "top" else shade(SP["grey"], 0.8), tx, ty, 2032, 0.12)

    def leg(face, X, Y, Z, n, tx, ty):
        return SP["joint"] if tx % 3 == 0 else fur(SP["leg"], tx, ty, 2033, 0.08)

    def fang(face, X, Y, Z, n, tx, ty):
        return SP["fang"]

    parts = [
        part("body", None, (0, 20, 0), (0, 0, 0), [cube("sp_body", -2.5, -2, -3, 5, 4, 5, thorax)]),
        part("head", "body", (0, 0, -3), (0, 0, 0), [cube("sp_head", -2, -1.5, -3, 4, 3, 3, head),
                                                     cube("sp_fang", -1.5, 1.5, -3, 1, 1, 1, fang),
                                                     cube("sp_fang", 0.5, 1.5, -3, 1, 1, 1, fang, True)]),
        part("abdomen", "body", (0, -0.5, 2), (0.25, 0, 0), [cube("sp_abdomen", -3.5, -3.5, 0, 7, 6, 8, abdomen)]),
    ]
    for i, z in enumerate(LEG_Z):
        zr, yr = LEG_REST[i]
        parts.append(part(f"right_leg{i}", None, (-2.5, 19.5, z), (0, yr, zr), [cube("sp_leg", -9, -0.5, -0.5, 9, 1, 1, leg)]))
        parts.append(part(f"left_leg{i}", None, (2.5, 19.5, z), (0, -yr, -zr), [cube("sp_leg", 0, -0.5, -0.5, 9, 1, 1, leg, True)]))
    return parts


MODELS = {  # name -> (parts, texture size, skinned version for a carcass)
    "cave_salamander": (salamander, (32, 32), False),
    "blind_cave_fish": (cave_fish, (32, 32), False),
    "bat_swarm": (bat, (16, 16), False),
    "cavern_spider": (spider, (64, 32), True),
}


def entities():
    for name, (make, size, skin) in MODELS.items():
        parts = make()
        uv = pack(parts, *size)
        if "--java" in sys.argv:
            print(f"// ---- {name} {size}")
            print("\n".join(java_parts(parts, uv)))
        img = paint(parts, uv, size)
        save(img, f"wildlife/{name}.png")
        if skin:
            save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")


# ======================================================================= Cave Web block

def cave_web():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    silk, thin = (226, 226, 218, 235), (200, 202, 196, 190)
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            r, a = math.hypot(dx, dy), math.atan2(dy, dx)
            spoke = min(abs(math.sin((a + 0.2) * 4)) * r, 9) < 0.55     # 8 spokes from the hub
            ring = r > 1.5 and abs((r + 0.3 * math.sin(a * 4)) % 2.6 - 1.3) < 0.32  # the sagging spiral
            if spoke:
                px[x, y] = silk
            elif ring and r < 8.2:
                px[x, y] = thin
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = silk                                              # the dense hub
    im.save(BLOCK / "cave_web.png")
    print("wrote block/cave_web.png")


# ======================================================================= items

def grid(rows, colours):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), hexc(colours[ch]) if isinstance(colours[ch], str) else colours[ch])
    return im


OLM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "..r.............",
    ".rRr............",
    "..HHHMM.........",
    ".rHHLLMMMM......",
    "..r.MMLLLMMMM...",
    "....l.l.DMMMMMd.",
    "..........l.lDDd",
    "...............d",
    "................",
    "................",
    "................",
]
OLM_C = {"H": "#f0d4ce", "L": "#f6e2dc", "M": "#e2bab4", "D": "#c89c96", "d": "#a87c78", "l": "#d8aaa4", "r": "#c84650",
         "R": "#9a2a36"}

LEG = [
    "................",
    "................",
    "............OO..",
    "...........OLMO.",
    "..........OLMO..",
    ".........OMMO...",
    "........OJJO....",
    ".......OLMO.....",
    "......OLMO......",
    ".....OMMO.......",
    "....OJJO........",
    "...OLMO.........",
    "..OLMO..........",
    "..OMO...........",
    "...O............",
    "................",
]
LEG_C = {"O": "#2a2624", "L": "#7c7670", "M": "#5a5450", "J": "#9a9286"}
LEG_ROAST = {"O": "#2a1a10", "L": "#b0844c", "M": "#8a5e30", "J": "#c8a070"}

GLAND = [
    "................",
    "................",
    "................",
    "......OOOO......",
    ".....OPPPLO.....",
    "....OPPLLLLO....",
    "....OPPPPLLO....",
    "....OMPPPPPO....",
    "....OMMPPPPO....",
    ".....OMMPPO.....",
    "......OOMO......",
    "........SS......",
    ".........SS.....",
    "..........S.....",
    "................",
    "................",
]
GLAND_C = {"O": "#6a4a4a", "P": "#d8c8bc", "L": "#f2ece4", "M": "#b0a094", "S": "#ecece6"}


def broth():
    im = Image.open(io.BytesIO(zipfile.ZipFile(JAR).read("assets/minecraft/textures/item/mushroom_stew.png"))).convert("RGBA")
    px = im.load()
    for y in range(6, 9):
        for x in range(3, 13):
            r, g, b, a = px[x, y]
            if a and not (r > b + 30 and g < 120):                 # the soup, not the bowl's rim
                k = h(x, y, 2040)
                px[x, y] = (150, 210, 170, 255) if k < 0.18 else (226, 176, 170, 255) if k < 0.3 else \
                    (62, 82, 92, 255) if (x + y) % 2 else (78, 98, 104, 255)   # murky broth, glow lichen and olm bits
    return im


FISH_RAW = ["#7a5a58", "#987672", "#b4928c", "#c8a8a2", "#d8bab4", "#e6ccc6", "#f0dcd6", "#faece8"]
FISH_COOKED = ["#4a3020", "#6a4a30", "#866240", "#9c7852", "#b08e66", "#c2a47c", "#d2b892", "#e4d0b0"]
FANG = ["#2a2422", "#3c3430", "#524842", "#6a5e56", "#867a70", "#a09488", "#bcb2a6", "#d8d0c4"]


def items():
    out = {
        "cave_salamander": grid(OLM, OLM_C),
        "dark_sight_broth": broth(),
        "blind_cave_fish": recolour(ITEM / "whole_pike.png", FISH_RAW),
        "cooked_cave_fish": recolour(ITEM / "cooked_pike.png", FISH_COOKED),
        "spider_leg": grid(LEG, LEG_C),
        "roasted_spider_leg": grid(LEG, LEG_ROAST),
        "silk_gland": grid(GLAND, GLAND_C),
        "cavern_spider_fang": recolour(ITEM / "centipede_fang.png", FANG),
        # spawn eggs: hi, light, mid, dark, outline light, outline dark; spots
        "cave_salamander_spawn_egg": egg(("#fbeeea", "#f0d4ce", "#e2bab4", "#c89c96", "#a87c78", "#7a5654"),
                                         [(5, 6, "#c84650"), (10, 9, "#c84650"), (7, 11, "#9a2a36")]),
        "blind_cave_fish_spawn_egg": egg(("#fff4f0", "#f0dcd6", "#d8bab4", "#b4928c", "#987672", "#6a504e"),
                                         [(6, 7, "#e2a0a0"), (9, 10, "#e2a0a0")]),
        "bat_swarm_spawn_egg": egg(("#7a6250", "#5a4636", "#4a382e", "#34261e", "#22180f", "#140e08"),
                                   [(5, 6, "#1a120c"), (9, 5, "#1a120c"), (8, 9, "#1a120c"), (6, 11, "#1a120c"), (10, 12, "#1a120c")]),
        "cavern_spider_spawn_egg": egg(("#a8a296", "#7c7670", "#605a56", "#46423e", "#34302e", "#1e1c1a"),
                                       [(6, 6, "#c8c0ac"), (9, 8, "#c8c0ac"), (7, 10, "#c8c0ac"), (10, 11, "#2a2624")]),
    }
    for name, im in out.items():
        im.save(ITEM / f"{name}.png")
        print("wrote item/" + name + ".png")


def main():
    entities()
    cave_web()
    items()


if __name__ == "__main__":
    main()
