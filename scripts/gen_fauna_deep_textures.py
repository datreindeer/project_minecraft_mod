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
from box_paint import Model, h, java_method, lerp, lit, pack, paint, shade, write_java  # noqa: E402
from critter_paint import fur, save, skinned  # noqa: E402
from gen_wildlife_item_textures import egg, hexc, recolour  # noqa: E402

TEX = HERE.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
JAR = HERE.parent / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"


# ======================================================================= the models (box_paint: geometry + skin from one table)

def lit_low(c, q):
    return lit(c, q, top=14, bottom=24, amt=0.1)


# ---- cave salamander (olm): a long eel-like body in three pieces, a flat shovel snout, no eyes (only the faint dark spots
# under the skin where they would be), three red feathery gill fronds each side, four tiny legs, a tail with a leaf fin.
SA = dict(skin=(238, 212, 200), back=(222, 190, 178), belly=(250, 236, 228), shade=(196, 156, 146), gill=(224, 74, 92),
          gill_dk=(156, 38, 58), gill_lt=(250, 150, 156), spot=(184, 142, 138), mouth=(170, 112, 108))


def olm_colour(m, q):
    P = SA
    n = h(q.tx, q.ty, 2001)
    f = q.face
    if m in ("skin", "head", "snout", "leg"):
        c = P["belly"] if f == "bottom" else lerp(P["skin"], P["back"], 0.6 if f == "top" else 0.15 + 0.2 * n)
        if f == "top" and h(q.tx, q.ty, 2002) > 0.86:
            c = lerp(c, P["shade"], 0.5)  # faint mottling: the blood showing through the skin
        if m == "head" and f in ("left", "right") and -6.5 < q.Z < -5.5 and q.j == 0:
            c = lerp(c, P["spot"], 0.6)  # the eyes, sunk under the skin
        if m == "snout" and f in ("front", "left", "right") and q.j == q.fh - 1:
            c = P["mouth"]
        if m == "leg" and f == "front" and q.i == 0:
            c = P["shade"]  # toes
    elif m == "gill":
        t = q.i / max(1, q.fw - 1)
        if q.j % 2 == 1 and t > 0.6:
            return None  # feathery
        c = lerp(P["gill_dk"], P["gill"], 0.4 + 0.6 * t)
        if t > 0.85:
            c = lerp(c, P["gill_lt"], 0.5)
    elif m == "fin":
        # a leaf-shaped fin, widest in the middle, fading at the edge
        lz = (q.z - q.box[2]) / q.box[5]
        half = math.sin(lz * math.pi) * q.box[4] / 2
        mid = q.box[1] + q.box[4] / 2
        if abs(q.y - mid) > half:
            return None
        return (*lerp(P["skin"], P["shade"], 0.25 * n), 140)
    else:
        c = (255, 0, 255)
    return lit_low(c, q)


def olm():
    M = Model("cave_salamander", (64, 32), 1, olm_colour)
    M.part("body", None, (0, 23, 0))
    M.box("body", "skin", -1.5, -1.5, -4, 3, 2, 4)
    M.part("head", "body", (0, 0, -4))
    M.box("head", "head", -1.5, -1.5, -3, 3, 2, 3)
    M.box("head", "snout", -1, -1, -5, 2, 1, 2)
    for side in (-1, 1):
        for k in range(3):
            name = f"{'right' if side < 0 else 'left'}_gill{k}"
            M.part(name, "head", (side * 1.5, -1, -0.5 - k * 0.7), (0, side * (k - 1) * -0.45, -side * (0.5 + 0.15 * k)))
            M.limb(name, side, (-4, 0, -0.5, 4, 0, 1), mat="gill")
    for side, name, z in ((-1, "right_front_leg", -3), (1, "left_front_leg", -3)):
        M.part(name, "body", (side * 1.5, 0, z), (0, 0, side * 0.6))
        M.limb(name, side, (-2, -0.5, -0.5, 2, 1, 1), mat="leg")
    M.part("rear", "body", (0, 0, 0))
    M.box("rear", "skin", -1.5, -1.5, 0, 3, 2, 4)
    for side, name in ((-1, "right_hind_leg"), (1, "left_hind_leg")):
        M.part(name, "rear", (side * 1.5, 0, 3), (0, 0, side * 0.6))
        M.limb(name, side, (-2, -0.5, -0.5, 2, 1, 1), mat="leg")
    M.part("tail", "rear", (0, 0, 4))
    M.box("tail", "skin", -1, -1.5, 0, 2, 2, 4)
    M.box("tail", "fin", 0, -2.5, 1, 0, 1, 3)
    M.part("tail_tip", "tail", (0, 0, 4))
    M.box("tail_tip", "skin", -0.5, -1.5, 0, 1, 2, 3)
    M.box("tail_tip", "fin", 0, -2, 0, 0, 3, 4)
    return M


# ---- blind cave fish: a deep-bellied little tetra, pale pink, the head without eyes (skin over the sockets), the gills
# blushing through, a forked tail, dorsal, anal and paired pectoral fins.
CF = dict(back=(222, 192, 186), side=(242, 216, 208), belly=(252, 240, 234), fin=(246, 226, 220), gill=(230, 140, 148),
          socket=(214, 178, 174), mouth=(190, 130, 128))


def fish_colour(m, q):
    P = CF
    n = h(q.tx, q.ty, 2010)
    f = q.face
    if m in ("body", "head"):
        up = q.Y < 20.5
        c = P["belly"] if f == "bottom" or q.Y > 22 else P["back"] if f == "top" or up else P["side"]
        c = lerp(c, P["back"], 0.12 * n)
        if f in ("left", "right") and abs(q.Y - 21) < 1.6 and h(q.tx // 2, q.ty, 2012) > 0.8:
            c = lerp(c, (255, 250, 248), 0.5)  # a faint sheen of scales
        if m == "head" and f in ("left", "right"):
            if q.Z > -2.6 and q.Y > 20:
                c = lerp(c, P["gill"], 0.6)  # the gill cover, pink with blood
            if abs(q.Z + 3.5) < 0.6 and abs(q.Y - 20.5) < 0.6:
                c = P["socket"]  # where an eye would be: skin
            if q.Z < -4.4 and q.Y > 21:
                c = P["mouth"]
    elif m.startswith("fin"):
        if m == "fin_tail":
            t = q.i / max(1, q.fw - 1)  # 0 at the base
            mid = abs(q.j - (q.fh - 1) / 2) / ((q.fh - 1) / 2)
            if t > 0.55 and mid < (t - 0.55) * 1.6:
                return None  # the fork
        if (q.i + q.j) % 3 == 0 and q.fh > 1:
            return (*lerp(P["fin"], P["back"], 0.4), 200)  # fin rays
        return (*P["fin"], 160)
    else:
        c = (255, 0, 255)
    return lit_low(c, q)


def cave_fish():
    M = Model("blind_cave_fish", (32, 32), 1, fish_colour)
    M.part("body", None, (0, 21, 0))
    M.box("body", "body", -1.5, -2, -2, 3, 4, 4)
    M.box("body", "fin", 0, -4, -1, 0, 2, 3)
    M.part("head", "body", (0, 0, -2))
    M.box("head", "head", -1, -1.5, -3, 2, 3, 3)
    M.box("head", "head", -0.5, -0.5, -4, 1, 2, 1)
    for side, name in ((-1, "right_fin"), (1, "left_fin")):
        M.part(name, "body", (side * 1.5, 1, -1.5), (0.3, side * -0.6, side * -0.4))
        M.limb(name, side, (-2, 0, 0, 2, 0, 2), mat="fin_pec")
    M.part("rear", "body", (0, 0, 2))
    M.box("rear", "body", -1, -1.5, 0, 2, 3, 3)
    M.box("rear", "body", -0.5, -1, 3, 1, 2, 2)
    M.box("rear", "fin", 0, 1.5, 0.5, 0, 2, 2)
    M.part("tail", "rear", (0, 0, 5))
    M.box("tail", "fin_tail", 0, -3, 0, 0, 6, 4)
    return M


# ---- one bat of the swarm (BatSwarmModel repeats it 18 times, drawn at 0.6): furry body and head, a snout, two pointed
# ears, two-piece wings of dark membrane with finger bones and a scalloped trailing edge.
BAT = dict(fur=(80, 60, 46), fur_dk=(40, 30, 24), wing=(40, 30, 28), bone=(86, 68, 56), ear=(120, 86, 76), nose=(40, 28, 24),
           eye=(14, 10, 10))


def bat_colour(m, q):
    P = BAT
    n = h(q.tx, q.ty, 2020)
    f = q.face
    if m in ("body", "head"):
        c = lerp(P["fur_dk"], P["fur"], 0.4 + 0.5 * n)
        if m == "head" and f == "front":
            c = P["eye"] if q.j == 0 and q.i in (0, q.fw - 1) else c
    elif m == "snout":
        c = P["nose"]
    elif m == "ears":
        if q.j == 0 and q.i in (0, q.fw - 1):
            return (*P["ear"], 255)  # two pointed tips
        if q.j == 1:
            return (*P["ear"], 255)
        return None
    elif m in ("wing", "wing_tip"):
        r = q.z + 1  # 0 at the leading edge, 3 at the trailing edge
        if m == "wing_tip" and r > 0.5 + (q.x + 4) / 4 * 2.5:
            return None  # the pointed tip
        if r > 2 and int(q.x * 2) % 3 == 0:
            return None  # the scalloped trailing edge
        c = P["bone"] if r < 1 or (m == "wing_tip" and abs((q.x + 4) % 1.5 - 0.75) < 0.3) else P["wing"]
        return (*shade(c, 0.95 + 0.1 * n), 255)
    else:
        c = (255, 0, 255)
    return lit_low(c, q)


def bat():
    M = Model("bat_swarm", (32, 16), 1, bat_colour)
    M.part("bat", None, (0, 16, 0))
    M.box("bat", "body", -1, -1, -1.5, 2, 2, 3)
    M.box("bat", "head", -1, -1.5, -3.5, 2, 2, 2)
    M.box("bat", "snout", -0.5, -0.5, -4.5, 1, 1, 1)
    M.box("bat", "ears", -1, -3.5, -2.5, 2, 2, 0)
    for side, name in ((-1, "right_wing"), (1, "left_wing")):
        M.part(name, "bat", (side * 1, -0.5, -0.5))
        M.limb(name, side, (-3, 0, -1, 3, 0, 3), mat="wing")
        tip = name.replace("wing", "wing_tip")
        M.part(tip, name, (side * 3, 0, 0))
        M.limb(tip, side, (-4, 0, -1, 4, 0, 3), mat="wing_tip")
    return M


# ---- cavern spider: a hairy brown-grey cephalothorax with a hump, a head with eight small dull eyes, two chelicerae with
# black fangs and two pedipalps, a big oval abdomen tilted up (pale chevrons, spinnerets), eight legs whose knees stand
# high over the body: a femur rising from the body and a long shin down to the ground, banded at the joints.
SP = dict(body=(104, 94, 82), dark=(46, 40, 36), pale=(190, 178, 152), hair=(134, 124, 108), eye=(18, 16, 14),
          shine=(110, 106, 100), fang=(30, 20, 18), chel=(70, 50, 40), band=(150, 138, 116))
SPIDER_LEGS = [(-2, 7, 10, -0.75), (-1, 6, 9, -0.25), (0, 6, 9, 0.25), (1, 7, 10, 0.7)]  # (z, femur, shin, yRot) right legs, front to back
LEG_UP, KNEE = 0.7, -1.95


def spider_colour(m, q):
    P = SP
    n = h(q.tx, q.ty, 2030)
    f = q.face
    hairy = lerp(P["dark"], P["body"], 0.45 + 0.45 * n)
    if h(q.tx, q.ty, 2031) > 0.86:
        hairy = lerp(hairy, P["hair"], 0.6)
    if m in ("ceph", "hump"):
        c = P["dark"] if f == "bottom" else hairy
        if f == "top" and abs(q.X) < 0.6:
            c = lerp(c, P["pale"], 0.4)  # the fovea line
    elif m == "head":
        c = hairy
        if f in ("front", "top"):
            ex = abs(q.X)
            # two big eyes in front, two small ones beside them, two pairs further back on top
            eye = (f == "front" and ((q.j == 1 and ex < 1) or (q.j == 0 and 1 < ex < 2))) or (f == "top" and 0.5 < ex < 1.5 and q.Z < -4.5)
            if eye:
                c = P["shine"] if h(q.tx, q.ty, 2032) > 0.75 else P["eye"]  # small, dull and dark: they don't glow
    elif m == "chel":
        c = P["fang"] if q.Y > 21 or (f in ("front", "back", "left", "right") and q.j == q.fh - 1) else P["chel"]
    elif m == "palp":
        c = lerp(hairy, P["band"], 0.4) if q.i == 0 or (f in ("left", "right") and q.i == q.fw - 1) else hairy
    elif m in ("abdomen", "abd_top", "abd_back", "abd_bottom"):
        c = P["dark"] if f == "bottom" or m == "abd_bottom" else hairy
        if f == "top" and m in ("abdomen", "abd_top"):
            lz = q.z
            if abs(abs(q.x) - (lz - 1) * 0.3) < 0.6 and int(lz) % 2 == 0 and lz > 1:
                c = P["pale"]  # pale chevrons down the back
            elif abs(q.x) < 0.5 and lz < 3:
                c = lerp(c, P["pale"], 0.5)  # the heart mark
    elif m == "spin":
        c = P["dark"]
    elif m in ("femur", "shin"):
        c = lerp(P["dark"], P["body"], 0.35 + 0.4 * n)
        if q.face in ("front", "back", "top", "bottom") and (q.i in (0,) or q.i == q.fw - 1):
            c = P["band"]  # pale joint rings
        if m == "shin" and q.face in ("front", "back", "top", "bottom") and q.i < 2:
            c = P["dark"]  # the dark foot
        if h(q.tx, q.ty, 2033) > 0.8:
            c = lerp(c, P["hair"], 0.6)  # bristles
    else:
        c = (255, 0, 255)
    return lit(c, q, top=12, bottom=24, amt=0.15)


def spider():
    M = Model("cavern_spider", (64, 64), 1, spider_colour)
    M.part("body", None, (0, 19, 0))
    M.box("body", "ceph", -2.5, -1.5, -3, 5, 3, 5)
    M.box("body", "hump", -2, -2.5, -2.5, 4, 1, 4)
    M.part("head", "body", (0, -0.5, -3))
    M.box("head", "head", -2, -1.5, -2, 4, 3, 2)
    M.box("head", "chel", -1.5, 1, -2.5, 1, 2, 1)
    M.box("head", "chel", 0.5, 1, -2.5, 1, 2, 1, True, key="head.1")
    for side, name in ((-1, "right_palp"), (1, "left_palp")):
        M.part(name, "head", (side * 1.5, 1, -2), (0.35, side * 0.35, 0))
        M.limb(name, side, (-0.5, -0.5, -3, 1, 1, 3), mat="palp")
    M.part("abdomen", "body", (0, -0.5, 2), (0.22, 0, 0))
    M.box("abdomen", "abdomen", -3.5, -3, 0, 7, 6, 8)
    M.box("abdomen", "abd_top", -3, -4, 1, 6, 1, 6)
    M.box("abdomen", "abd_back", -2.5, -2.5, 8, 5, 5, 1)
    M.box("abdomen", "abd_bottom", -3, 3, 1, 6, 1, 6)
    M.box("abdomen", "spin", -0.5, 0.5, 9, 1, 1, 1)
    for i, (z, fl, sl, yr) in enumerate(SPIDER_LEGS):
        for side in (-1, 1):
            s = "right" if side < 0 else "left"
            leg, shin = f"{s}_leg{i}", f"{s}_shin{i}"
            M.part(leg, None, (side * 2.5, 18.5, z), (0, side * -yr if side > 0 else yr, side * -LEG_UP if side > 0 else LEG_UP))
            M.limb(leg, side, (-fl, -0.5, -0.5, fl, 1, 1), mat="femur")
            M.part(shin, leg, (side * fl, 0, 0), (0, 0, KNEE if side < 0 else -KNEE))
            M.limb(shin, side, (-sl, -0.5, -0.5, sl, 1, 1), mat="shin")
    return M


MODELS = {  # make, renderer scale (for the doc), skinned version for a carcass
    "caveSalamander": (olm, False),
    "blindCaveFish": (cave_fish, False),
    "bat": (bat, False),
    "cavernSpider": (spider, True),
}
JAVA = HERE.parent / "src/main/java/com/mechanical/client/wildlife/deep/DeepGeometry.java"


def entities():
    methods = []
    for method, (make, skin) in MODELS.items():
        M = make()
        uv = pack(M.parts, *M.size)
        img = paint(M.parts, uv, M.size, M.k)
        save(img, f"wildlife/{M.name}.png")
        if skin:
            save(skinned(img, {}, ()), f"skinned/wildlife/{M.name}.png")
        methods.append(java_method(method, M.parts, uv, M.size, repeat=(18, "bat") if method == "bat" else None))
        print(f"{M.name}: {len(M.parts)} parts, {sum(len(p['cubes']) for p in M.parts)} boxes")
    write_java(JAVA, "com.mechanical.client.wildlife.deep", "DeepGeometry", "scripts/gen_fauna_deep_textures.py", methods)


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
