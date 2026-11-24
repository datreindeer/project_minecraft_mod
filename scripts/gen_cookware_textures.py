"""Kitchen cookware and dish textures (models: scripts/herbs_cookware.py). Standalone, deterministic.
Run: python scripts/gen_cookware_textures.py

Block (textures/block):
- cooking_pot_clay: the pot's outside, its rows running down the pot (herbs_cookware.POT_V): a lit rolled rim, the
  shadow under it, an incised line and a zigzag band on the shoulder, cord marks under it, fired clay with dark firing
  clouds, and soot creeping up in tongues to a black bottom. cooking_pot_inside (stained, a tide line of old soup, a
  crusted floor), cooking_pot_rim (finger-smoothed top of the rim), cooking_pot_bottom (soot and ash).
- griddle_plate (laid over the plate by position: hammered bronze, a scorched brown-black middle ringed with heat tint,
  a brighter rim), griddle_leg (bronze rod, sooted where the flames lick it, ashy at the foot).
- skillet_iron (cast iron skin, sandy grain, a little rust), skillet_floor (seasoned black, an oily sheen, rings).
- cauldron_iron (rows down the cauldron: worn rim, cast grain, rust freckles, soot to a black bottom), cauldron_inside,
  cauldron_chain (one iron link, cutout).
- barrel_staves (oak staves, dark seams, grain, damp at the foot, a brine run), barrel_hoop (black iron, rivets, rust),
  barrel_end (stave ends), barrel_inside (wet dark oak), barrel_lid (boards with nail heads), barrel_brine (dark brine
  with pale cabbage shreds and bubbles).
- press_wood (beams, grain along), press_post (grain up), press_wood_end, press_screw (helical thread), press_hoop
  (slats with gaps), press_hoop_inside, press_board (grooved board: a ring groove round the hoop, a channel to the
  spout), press_cheese (pale curd with the cheesecloth's weave).
- salt_pan_clay, salt_pan_rim, salt_pan_floor (wet clay), salt_pan_wall_1..4 (salt tide lines on the inner wall),
  salt_pan_brine_1..3 (blue-grey brine, a white crust growing in from the edges), salt_pan_salt (crystalline crust).
- Steel Age (phase 6): steel_pan_steel (spun steel with heat colours and soot), steel_pan_floor (polished, seasoned
  toward the walls), steel_pan_trivet; mortar_marble, mortar_rim, mortar_inside (stained with spice), pestle_steel,
  pestle_wood; sieve_hoop (bent ash, a nailed scarf joint), sieve_mesh (woven steel wire, cutout), sieve_floor (flour).
Item (textures/item): unfired_clay_cooking_pot, iron_cauldron, bronze_ladle and the dishes (the vanilla bowl filled for
the soups and pottages; the rest painted). Phase 6 and leftovers: steel_knife_set, the five spice blends in a little
olivewood bowl, fine_flour, sea_buckthorn_juice, cider, ale, roast_with_herbs, saffron_rice, peppered_steak, spiced_pilaf,
pickled_vegetables, (raw_)gingerbread, raw_/baked pies of apple, plum, rowan and bird cherry.
"""
import math
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR_NAME = "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
JAR = ROOT / JAR_NAME
if not JAR.exists():  # a git worktree without its own build: use the main checkout's
    JAR = Path(r"C:\Users\Yngve\Documents\project_minecraft_mod") / JAR_NAME
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"


def vanilla(path):
    with zipfile.ZipFile(JAR) as jar:
        return Image.open(BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def clamp(t):
    return max(0.0, min(1.0, t))


def ramp(pal, t):
    t = clamp(t) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def mix(a, b, t):
    t = clamp(t)
    return tuple(int(a[i] * (1 - t) + b[i] * t) for i in range(3))


def new(w=16, h=16):
    return Image.new("RGBA", (w, h))


def put(img, x, y, c, a=255):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), tuple(c[:3]) + (a,))


def get(img, x, y):
    return img.getpixel((x, y))[:3]


def solid(img, x, y):
    return 0 <= x < img.width and 0 <= y < img.height and img.getpixel((x, y))[3] > 0


def save(img, name, kind="block"):
    path = (BLOCK if kind == "block" else ITEM) / f"{name}.png"
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if mc.exists():
        mc.unlink()
    WRITTEN.append(path)


WRITTEN = []


def smooth_noise(seed, cells=4, size=16):
    """Tileable smooth noise in 0..1: a cells x cells random grid, bilinearly wrapped."""
    r = random.Random(seed)
    g = [[r.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * size for _ in range(size)]
    for y in range(size):
        for x in range(size):
            fx, fy = x * cells / size, y * cells / size
            x0, y0 = int(fx), int(fy)
            tx, ty = fx - x0, fy - y0
            tx, ty = tx * tx * (3 - 2 * tx), ty * ty * (3 - 2 * ty)
            a, b = g[y0 % cells][x0 % cells], g[y0 % cells][(x0 + 1) % cells]
            c, d = g[(y0 + 1) % cells][x0 % cells], g[(y0 + 1) % cells][(x0 + 1) % cells]
            out[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


# ---------------------------------------------------------------- palettes

CLAY = [(46, 26, 18), (84, 46, 28), (124, 70, 42), (158, 96, 60), (186, 124, 82), (210, 154, 108), (228, 184, 140)]
SOOT = [(16, 13, 12), (30, 25, 22), (46, 38, 32), (64, 54, 46)]
RAWCLAY = [(62, 56, 52), (92, 84, 76), (122, 112, 100), (148, 138, 124), (172, 162, 148), (194, 186, 172)]
BRONZE = [(52, 32, 12), (92, 60, 22), (140, 98, 38), (184, 138, 60), (218, 178, 96), (244, 218, 150)]  # gen_bronze_textures.py
IRON = [(14, 14, 17), (26, 26, 30), (40, 40, 45), (56, 56, 62), (78, 78, 84), (108, 108, 112), (146, 146, 148)]
RUST = (118, 56, 28)
OAK = [(48, 30, 16), (76, 50, 28), (106, 74, 42), (136, 100, 60), (164, 128, 82), (190, 156, 108), (214, 186, 140)]
GREYOAK = [(52, 42, 32), (80, 66, 50), (110, 92, 70), (138, 118, 92), (164, 144, 116), (190, 172, 144)]
SALTCLAY = [(70, 52, 42), (104, 80, 62), (136, 108, 84), (162, 134, 108), (186, 162, 136), (208, 190, 166)]
SALT = [(150, 156, 160), (186, 192, 196), (214, 218, 220), (234, 236, 236), (250, 250, 248)]
BRINE = [(46, 64, 74), (66, 88, 98), (90, 114, 124), (118, 140, 148), (156, 176, 182)]


# ---------------------------------------------------------------- Clay Cooking Pot

def cooking_pot():
    """Burnished hand-built clay, rows running down the pot (herbs_cookware.POT_V): soft firing mottles, one quiet band of
    cord impressions on the shoulder, and soot darkening smoothly down the belly to near black at the bottom."""
    n = smooth_noise(101, 4)
    n2 = smooth_noise(102, 8)
    r = random.Random(103)
    img = new()
    for y in range(16):
        for x in range(16):
            t = 0.55 + (n[y][x] - 0.5) * 0.12 + (n2[y][x] - 0.5) * 0.04 - 0.02 * max(0, y - 5) / 3
            if y <= 1:
                t = 0.64 + (n2[y][x] - 0.5) * 0.04   # the rolled rim's lit top
            elif y == 2:
                t = 0.52
            elif y == 3:
                t = 0.42                              # soft shadow under the roll (the neck)
            if 4 <= y <= 7:
                t += 0.04 * (1 - abs(y - 5.5) / 2)    # burnished shoulder, a faint sheen
            c = ramp(CLAY, t)
            # soot: a smooth gradient from the middle of the belly to the bottom, its upper edge wavering gently
            edge = 6.4 + 0.9 * math.sin(x * 2 * math.pi / 16 + 0.6) + 0.6 * (n[(y + 5) % 16][x] - 0.5)
            soot = clamp((y - edge) / 4.6)
            c = mix(c, SOOT[1], soot * soot * (3 - 2 * soot) * 0.93)
            put(img, x, y, c)
    for x in range(16):  # one band of slanted cord impressions on the shoulder, only a little darker than the clay
        put(img, x, 5 + (x % 2), mix(get(img, x, 5 + (x % 2)), CLAY[2], 0.28))
        if x % 2 == 0:
            put(img, x, 7, mix(get(img, x, 7), CLAY[4], 0.12))  # the light catching each twist's lower edge
    save(img, "cooking_pot_clay")

    ins = new()
    for y in range(16):
        for x in range(16):
            t = 0.42 + (n[x][y] - 0.5) * 0.2 + r.uniform(-0.03, 0.03) - 0.12 * clamp((y - 3) / 8)
            c = ramp(CLAY, t)
            c = mix(c, (58, 34, 18), 0.35 + 0.25 * clamp((y - 5) / 6))     # old soup soaked into the clay
            if y == 4:
                c = mix(c, (44, 26, 14), 0.55)                              # the tide line
            if y == 3 and x % 3 == 0:
                c = mix(c, CLAY[4], 0.3)                                    # splashes above it
            put(ins, x, y, c)
    for x, y in ((6, 7), (9, 9), (7, 10), (10, 6), (5, 11)):               # burnt-on crumbs on the floor
        put(ins, x, y, mix(get(ins, x, y), (30, 18, 10), 0.45))
    save(ins, "cooking_pot_inside")

    rim = new()
    for y in range(16):
        for x in range(16):
            # finger-smoothed: soft arcs following the round of the rim
            d = math.hypot(x - 7.5, y - 7.5)
            t = 0.48 + 0.06 * math.sin(d * 2.2) + (n2[y][x] - 0.5) * 0.08 + r.uniform(-0.02, 0.02)
            c = ramp(CLAY, t)
            c = mix(c, SOOT[2], clamp((n[y][x] - 0.68) * 3) * 0.3)  # soft smoke stains where the fire licked over
            put(rim, x, y, c)
    save(rim, "cooking_pot_rim")

    bot = new()
    for y in range(16):
        for x in range(16):
            c = ramp(SOOT, 0.35 + 0.5 * n2[y][x] + r.uniform(-0.08, 0.08))
            if n[y][x] > 0.75:
                c = mix(c, (110, 104, 96), 0.35)  # grey ash clinging
            if r.random() < 0.03:
                c = mix(c, CLAY[2], 0.5)          # clay showing through
            put(bot, x, y, c)
    save(bot, "cooking_pot_bottom")


# ---------------------------------------------------------------- Bronze Griddle

def griddle():
    r = random.Random(201)
    n = smooth_noise(202, 8)
    img = new()
    for y in range(16):
        for x in range(16):
            dx, dz = x + 0.5 - 8, y + 0.5 - 8
            d = math.hypot(dx, dz)
            t = 0.58 + (n[y][x] - 0.5) * 0.12 + r.uniform(-0.03, 0.03)
            cx, cy = (x + (y // 3) * 2) % 3, y % 3   # hammer dimples, a fine staggered grid
            t += 0.12 if (cx, cy) == (0, 0) else -0.08 if (cx, cy) == (1, 1) else 0
            c = ramp(BRONZE, t)
            scorch = clamp(1 - d / 5.2)
            c = mix(c, (44, 26, 12), scorch * 0.8)                    # burnt brown-black middle
            tint = clamp(1 - abs(d - 4.6) / 1.2)
            c = mix(c, (122, 64, 70), tint * 0.35)                    # heat tint ring (purple-red)
            if max(abs(dx), abs(dz)) > 5.25:
                c = ramp(BRONZE, 0.72 + r.uniform(-0.05, 0.05))         # the raised rim, worn bright
            put(img, x, y, c)
    for x, y in ((5, 5), (6, 5), (5, 6), (10, 9), (9, 11)):            # oil glints
        put(img, x, y, mix(get(img, x, y), BRONZE[5], 0.45))
    save(img, "griddle_plate")

    leg = new()
    for y in range(16):
        for x in range(16):
            t = 0.6 - 0.12 * (x % 2) + r.uniform(-0.04, 0.04)
            c = ramp(BRONZE, t)
            c = mix(c, SOOT[1], clamp((y - 3) / 6) * 0.75 * (1 - clamp((y - 12) / 3)))  # sooted in the flames
            if y >= 13:
                c = mix(c, (112, 106, 98), 0.45 + 0.1 * (y - 13))       # ash at the foot
            put(leg, x, y, c)
    save(leg, "griddle_leg")


# ---------------------------------------------------------------- Iron Skillet

def cast_iron(seed, base, rust=0.0):
    r = random.Random(seed)
    n = smooth_noise(seed + 1, 4)
    img = new()
    for y in range(16):
        for x in range(16):
            t = base + (n[y][x] - 0.5) * 0.12 + r.uniform(-0.07, 0.07)  # sandy cast grain
            if r.random() < 0.06:
                t += 0.14  # bright grains in the skin
            c = ramp(IRON, t)
            if rust and n[(y * 7) % 16][(x * 3) % 16] > 1 - rust:
                c = mix(c, RUST, 0.45)
            put(img, x, y, c)
    return img


def skillet():
    img = cast_iron(301, 0.36, rust=0.08)
    save(img, "skillet_iron")
    r = random.Random(303)
    fl = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            t = 0.3 + 0.03 * math.sin(d * 3.1) + r.uniform(-0.03, 0.03)
            # an oily sheen arcing round the upper left
            ang = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            sheen = clamp(1 - abs(d - 3.6) / 1.4) * clamp(math.cos(ang + 2.36))
            t += sheen * 0.32
            c = ramp(IRON, t)
            c = mix(c, (40, 30, 22), clamp(1 - d / 3) * 0.4)  # brown seasoning in the middle
            put(fl, x, y, c)
    for x, y in ((5, 6), (6, 5), (10, 10)):
        put(fl, x, y, IRON[5])
    save(fl, "skillet_floor")


# ---------------------------------------------------------------- Iron Cauldron

def cauldron():
    img = cast_iron(401, 0.32, rust=0.1)
    n = smooth_noise(403, 4)
    for y in range(16):
        for x in range(16):
            c = get(img, x, y)
            if y in (1, 2):
                c = mix(c, IRON[5], 0.3 if y == 1 else 0.12)   # the rolled rim, worn bright on top
            elif y == 3:
                c = mix(c, IRON[0], 0.4)                       # shadow under the rim
            soot = clamp((y - 6 - 1.5 * n[y][x]) / 3)
            c = mix(c, SOOT[0], soot * 0.7)
            put(img, x, y, c)
    save(img, "cauldron_iron")
    ins = cast_iron(405, 0.22)
    for x in range(16):
        put(ins, x, 3, mix(get(ins, x, 3), (70, 52, 36), 0.4))  # a scale line where the water stands
    save(ins, "cauldron_inside")
    ch = new()
    for y in range(4, 12):
        for x in range(6, 10):
            ex, ey = (x - 7.5) / 2, (y - 7.5) / 4
            q = ex * ex + ey * ey
            if 0.32 < q <= 1.05:
                put(ch, x, y, ramp(IRON, 0.75 - 0.45 * (x - 6) / 3 + (0.1 if y < 6 else 0)))
    save(ch, "cauldron_chain")


# ---------------------------------------------------------------- Fermenting Barrel

SEAMS = (0, 3, 5, 8, 11, 13)


def staves(seed, base, pal, wet=0.0):
    r = random.Random(seed)
    n = smooth_noise(seed + 1, 8)
    img = new()
    tone = {s: base + r.uniform(-0.07, 0.07) for s in SEAMS}
    for y in range(16):
        for x in range(16):
            s = max(k for k in SEAMS if k <= x)
            t = tone[s] + (n[(y // 4) % 16][s] - 0.5) * 0.08 + r.uniform(-0.025, 0.025)
            t += 0.07 if x == s + 1 else 0                 # each stave's rounded face catches the light on its left
            t += 0.05 * math.sin((x * 3.7 + y * 0.35) * 1.3) # grain running up the stave
            c = ramp(pal, t)
            if x == s:
                c = ramp(pal, 0.1)                          # the seam
            if wet:
                c = mix(c, (30, 22, 14), wet)
            put(img, x, y, c)
    return img


def barrel():
    r = random.Random(501)
    img = staves(502, 0.5, OAK)
    for x in range(16):
        for y in range(13, 16):  # damp, darker at the foot
            put(img, x, y, mix(get(img, x, y), (40, 28, 18), 0.25 * (y - 12)))
    for y in range(0, 10):  # a brine run from the lid down one stave
        put(img, 9, y, mix(get(img, 9, y), (68, 64, 40), 0.35 - y * 0.03))
    put(img, 6, 5, OAK[1]); put(img, 6, 6, OAK[0]); put(img, 7, 6, OAK[2])  # a knot
    save(img, "barrel_staves")

    hoop = new()
    for y in range(16):
        for x in range(16):
            t = (0.42 if y < 8 else 0.26) + r.uniform(-0.05, 0.05)
            c = ramp(IRON, t)
            if r.random() < 0.08:
                c = mix(c, RUST, 0.5)
            put(hoop, x, y, c)
    for x in (2, 10):  # rivets
        put(hoop, x, 7, IRON[6]); put(hoop, x, 8, IRON[2])
    save(hoop, "barrel_hoop")

    end = new()
    for y in range(16):
        for x in range(16):
            t = 0.62 + 0.04 * ((x * 3 + y * 5) % 4 == 0) + r.uniform(-0.03, 0.03)
            c = ramp(OAK, t)
            if x % 3 == 0 and (y * 7 + x) % 5 != 0:
                c = ramp(OAK, 0.32)  # the joints between stave ends
            put(end, x, y, c)
    save(end, "barrel_end")

    save(staves(504, 0.32, OAK, wet=0.35), "barrel_inside")

    lid = new()
    lseams = (3, 6, 9, 12)
    n = smooth_noise(505, 8)
    for y in range(16):
        for x in range(16):
            s = max([k for k in lseams if k <= x] or [0])
            t = 0.56 + 0.06 * ((s * 7) % 3 - 1) + (n[y][(x * 2) % 16] - 0.5) * 0.14 + r.uniform(-0.03, 0.03)
            t += 0.04 * math.sin(y * 1.1 + x * 0.4)
            c = ramp(OAK, t)
            if x in lseams:
                c = ramp(OAK, 0.14)
            put(lid, x, y, c)
    for x, y in ((4, 5), (7, 5), (10, 5), (4, 10), (7, 10), (10, 10)):  # nail heads (where the battens hold the boards)
        put(lid, x, y, mix(get(lid, x, y), IRON[3], 0.55))
    save(lid, "barrel_lid")

    br = new()
    n = smooth_noise(506, 4)
    for y in range(16):
        for x in range(16):
            c = mix((40, 42, 22), (70, 70, 36), n[y][x] * 0.8 + r.uniform(-0.05, 0.05))
            put(br, x, y, c)
    shreds = [((4, 5), (1, 0)), ((9, 4), (1, 1)), ((6, 9), (1, 0)), ((11, 8), (0, 1)), ((5, 12), (1, 0)), ((10, 11), (1, 0)),
              ((7, 6), (1, 1)), ((12, 5), (1, 0)), ((3, 8), (0, 1)), ((8, 13), (1, 0))]
    for i, (x, y), in enumerate(s for s, _ in shreds):  # pale cabbage shreds just under the surface
        dx, dy = shreds[i][1]
        put(br, x, y, mix(get(br, x, y), (176, 172, 108), 0.6))
        put(br, x + dx, y + dy, mix(get(br, x + dx, y + dy), (150, 150, 86), 0.45))
    for x, y in ((8, 6), (5, 8), (11, 10)):  # bubbles from the ferment
        put(br, x, y, (150, 150, 100)); put(br, x + 1, y + 1, (30, 30, 16))
    save(br, "barrel_brine")


# ---------------------------------------------------------------- Cheese Press

def wood_grain(seed, base, pal=GREYOAK, along_x=True):
    r = random.Random(seed)
    n = smooth_noise(seed + 1, 8)
    img = new()
    for y in range(16):
        for x in range(16):
            a, b = (x, y) if along_x else (y, x)
            t = base + 0.09 * math.sin(b * 1.9 + math.sin(a * 0.45) * 1.4) + (n[b][(a // 4) % 16] - 0.5) * 0.14 + r.uniform(-0.03, 0.03)
            put(img, x, y, ramp(pal, t))
    return img


def press():
    r = random.Random(601)
    beam = wood_grain(602, 0.55)
    for x0 in (2, 9):  # a check (drying crack) along the grain
        for x in range(x0, x0 + 4):
            put(beam, x, 5 + (x0 // 9), GREYOAK[0])
    save(beam, "press_wood")
    post = wood_grain(604, 0.52, along_x=False)
    for y in range(4, 9):
        put(post, 6, y, GREYOAK[0])
    save(post, "press_post")
    end = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 5.5, y - 6.5)
            t = 0.62 - 0.14 * (int(d * 1.2) % 2) + r.uniform(-0.03, 0.03)
            put(end, x, y, ramp(GREYOAK, t))
    save(end, "press_wood_end")
    sc = new()
    for y in range(16):
        for x in range(16):
            ph = (x + 2 * y) % 5  # the thread winding round: a ridge, its lit flank, the groove
            t = (0.78, 0.66, 0.5, 0.2, 0.36)[ph] + r.uniform(-0.03, 0.03)
            put(sc, x, y, ramp(GREYOAK, t))
    save(sc, "press_screw")
    hoop = new()
    for y in range(16):
        for x in range(16):
            t = 0.62 if x % 3 == 0 else 0.54
            t += r.uniform(-0.03, 0.03) - 0.1 * clamp((y - 6) / 3)
            c = ramp(GREYOAK, t)
            if x % 3 == 2:
                c = (36, 28, 20)  # the gap between slats
            put(hoop, x, y, c)
    save(hoop, "press_hoop")
    hi = new()
    for y in range(16):
        for x in range(16):
            c = mix(ramp(GREYOAK, 0.3 + r.uniform(-0.04, 0.04)), (200, 196, 160), 0.18 if (x * 3 + y) % 7 == 0 else 0)
            put(hi, x, y, c)
    save(hi, "press_hoop_inside")
    bd = new()
    for y in range(16):
        for x in range(16):
            t = 0.5 + 0.07 * math.sin(x * 2.1 + math.sin(y * 0.5)) + r.uniform(-0.03, 0.03)
            c = ramp(GREYOAK, t)
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            if d < 4.4:
                c = mix(c, (60, 50, 36), 0.35)       # wet with whey under the hoop
            if 4.4 <= d < 5.2 or (7 <= x <= 8 and y >= 12):
                c = (44, 34, 24)                      # the groove and the channel to the spout
                if (4.4 <= d < 4.8 and y < 8) or (x == 7 and y >= 12):
                    c = (30, 22, 16)
            put(bd, x, y, c)
    save(bd, "press_board")
    ch = new()
    for y in range(16):
        for x in range(16):
            t = 0.5 + r.uniform(-0.05, 0.05) + (0.12 if (x % 2 == 0 and y % 2 == 0) else 0)  # the cloth's weave pressed in
            put(ch, x, y, ramp([(196, 176, 118), (226, 210, 160), (244, 236, 200)], t))
    save(ch, "press_cheese")


# ---------------------------------------------------------------- Salt Pan

def salt_pan():
    r = random.Random(701)
    n = smooth_noise(702, 4)
    n2 = smooth_noise(703, 8)
    clay = new()
    for y in range(16):
        for x in range(16):
            t = 0.55 + (n[y][x] - 0.5) * 0.2 + r.uniform(-0.04, 0.04)
            c = ramp(SALTCLAY, t)
            if y == 13:
                c = ramp(SALTCLAY, 0.72 + r.uniform(-0.04, 0.04))  # the wall's top edge, lit
            if r.random() < 0.02 + 0.08 * (y >= 13):
                c = mix(c, SALT[3], 0.5)  # salt bloom
            put(clay, x, y, c)
    save(clay, "salt_pan_clay")
    rim = new()
    for y in range(16):
        for x in range(16):
            c = ramp(SALTCLAY, 0.66 + (n2[y][x] - 0.5) * 0.16 + r.uniform(-0.03, 0.03))
            if n2[(y * 5) % 16][(x * 3) % 16] > 0.78:
                c = mix(c, SALT[3], 0.4)  # crust drying on the rim
            put(rim, x, y, c)
    save(rim, "salt_pan_rim")
    fl = new()
    for y in range(16):
        for x in range(16):
            c = ramp(SALTCLAY, 0.3 + (n[y][x] - 0.5) * 0.18 + r.uniform(-0.03, 0.03))
            if n2[y][x] > 0.66:
                c = mix(c, (120, 128, 132), 0.35)  # a wet sheen of the sky
            put(fl, x, y, c)
    for x, y in ((4, 9), (11, 5), (9, 12)):  # a shell, a pebble
        put(fl, x, y, (200, 186, 166)); put(fl, x + 1, y, (150, 134, 116))
    save(fl, "salt_pan_floor")
    # inner wall faces (rows 13..15 = y 3..1): crust lines left as the brine went down
    for stage in range(1, 5):
        wl = clay.copy()
        rows = {1: [(13, 0.35)], 2: [(13, 0.7)], 3: [(13, 0.85), (14, 0.6)], 4: [(13, 0.95), (14, 0.9), (15, 0.85)]}[stage]
        for y, k in rows:
            for x in range(16):
                put(wl, x, y, mix(get(wl, x, y), ramp(SALT, 0.6 + r.uniform(-0.2, 0.3)), k * (0.8 + 0.2 * r.random())))
        save(wl, f"salt_pan_wall_{stage}")
    # brine: a calm blue-grey surface with a soft diagonal sheen; a white crystalline crust grows in from the rim and,
    # faster, from the corners (1 a thin edge, 2 a wide edge and rafts, 3 mostly crust round a last pool)
    nb = smooth_noise(704, 4)
    for stage in range(1, 4):
        br = new()
        width = {1: 1.0, 2: 1.8, 3: 4.1}[stage]
        rafts = {1: [], 2: [(6.4, 6.6, 1.2), (9.8, 9.6, 1.0)], 3: []}[stage]
        for y in range(16):
            for x in range(16):
                q = (abs(x + 0.5 - 8) ** 2.4 + abs(y + 0.5 - 8) ** 2.4) ** (1 / 2.4)  # roundish: the corners crust first
                e = 6.5 - q
                e += (nb[y][x] - 0.5) * (0.6 + 0.3 * stage)          # a gently ragged edge
                sheen = math.exp(-((x - y) / 3.5) ** 2)              # soft sky reflection across the water
                c = ramp(BRINE, 0.48 + 0.12 * sheen - 0.05 * (stage == 3) + (nb[x][y] - 0.5) * 0.06)
                crust = e < width or any(math.hypot(x + 0.5 - rx, y + 0.5 - ry) < rr for rx, ry, rr in rafts)
                if crust:
                    # crystalline: soft lit and shaded facets, a touch greyer where it meets the water
                    c = ramp(SALT, 0.74 + (n2[y][x] - 0.5) * 0.3 - 0.12 * clamp(1 - (width - e)))
                elif e < width + 0.7:
                    c = mix(c, SALT[2], 0.45)  # crust forming just under the surface at its edge
                put(br, x, y, c)
        for x, y in ((3, 2), (12, 13), (2, 11)):  # a few glints on the crust
            if get(br, x, y)[0] > 200:
                put(br, x, y, (255, 255, 255))
        save(br, f"salt_pan_brine_{stage}")
    s = new()
    for y in range(16):
        for x in range(16):
            t = 0.66 + (n2[y][x] - 0.5) * 0.25 + r.uniform(-0.05, 0.05)
            c = ramp(SALT, t)
            # crack lines of a dried crust: a coarse polygon net
            if abs(math.sin(x * 0.9 + y * 0.4) + math.sin(y * 0.8 - x * 0.3)) < 0.1 and (x + y) % 2:
                c = SALT[1]
            put(s, x, y, c)
    for x, y in ((3, 4), (10, 3), (6, 9), (12, 11), (4, 13), (9, 6)):  # sparkle
        put(s, x, y, (255, 255, 255)); put(s, x + 1, y, (226, 236, 244))
    save(s, "salt_pan_salt")


# ---------------------------------------------------------------- item helpers

def lathe(img, profile, pal, seed, outline=0.06, hl=0.15, light=0.0):
    """A turned shape lit from the left: profile = {row: half-width} round x = 8. Each pixel gets a tone from where it
    sits across the row (bright left third, dark right), the outermost pixels darkened to an outline."""
    r = random.Random(seed)
    for y, hw in profile.items():
        xs = [x for x in range(16) if abs(x + 0.5 - 8) <= hw + 1e-6]
        if not xs:
            continue
        a, b = xs[0], xs[-1]
        for x in xs:
            u = (x + 0.5 - 8) / max(hw, 0.5)
            t = 0.58 - 0.38 * u - 0.08 * u * u + light + r.uniform(-0.03, 0.03)
            if -0.75 < u < -0.3:
                t += hl
            if x in (a, b):
                t = outline + (0.12 if x == a else 0)
            put(img, x, y, ramp(pal, t))
    return img


def ellipse_px(cx, cy, rx, ry):
    return [(x, y) for y in range(16) for x in range(16) if ((x + 0.5 - cx) / rx) ** 2 + ((y + 0.5 - cy) / ry) ** 2 <= 1]


def outline_dark(img, color, only_bottom_right=False):
    """Darkens edge pixels (those with an empty neighbour) toward color."""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if not solid(src, x, y):
                continue
            nb = [(1, 0), (0, 1)] if only_bottom_right else [(1, 0), (-1, 0), (0, 1), (0, -1)]
            if any(not solid(src, x + dx, y + dy) for dx, dy in nb):
                put(img, x, y, mix(get(src, x, y), color, 0.55))


# ---------------------------------------------------------------- cookware items

def cookware_items():
    # Unfired Clay Cooking Pot: grey-brown raw clay, the opening dark, the zigzag incised but no soot
    img = new()
    prof = {3: 4.6, 4: 5.0, 5: 4.4, 6: 4.0, 7: 4.8, 8: 5.6, 9: 6.0, 10: 6.0, 11: 5.7, 12: 5.0, 13: 3.9, 14: 2.6}
    lathe(img, prof, RAWCLAY, 801)
    for x in range(5, 11):  # the opening seen over the front lip
        put(img, x, 3, RAWCLAY[0] if 6 <= x <= 9 else RAWCLAY[1])
    for x in range(4, 12):
        put(img, x, 4, ramp(RAWCLAY, 0.85 - 0.06 * abs(x - 6)))  # the front of the rolled rim, lit
    for x in range(3, 13):  # zigzag on the shoulder
        zy = (7, 8)[x % 2]
        put(img, x, zy, mix(get(img, x, zy), RAWCLAY[0], 0.5))
    for x, y in ((5, 10), (4, 11), (10, 12)):  # finger marks
        put(img, x, y, mix(get(img, x, y), RAWCLAY[4], 0.3))
    save(img, "unfired_clay_cooking_pot", "item")

    # Iron Cauldron: pot belly, thick rim, lugs and the bail arching over, three feet
    img = new()
    prof = {6: 6.0, 7: 5.2, 8: 6.2, 9: 6.6, 10: 6.6, 11: 6.3, 12: 5.6, 13: 4.4}
    lathe(img, prof, IRON, 802, hl=0.18)
    for x in range(3, 13):
        put(img, x, 6, ramp(IRON, 0.75 - 0.05 * abs(x - 6)))  # the rim's lit top
    for x in range(4, 12):
        put(img, x, 5, IRON[0] if 5 <= x <= 10 else IRON[1])   # the opening
    for x in (1, 14):  # lugs
        put(img, x, 6, IRON[3]); put(img, x, 7, IRON[2])
    bail = [(2, 5), (2, 4), (3, 3), (4, 2), (5, 1), (6, 1), (7, 1), (8, 1), (9, 1), (10, 1), (11, 2), (12, 3), (13, 4), (13, 5)]
    for i, (x, y) in enumerate(bail):
        put(img, x, y, ramp(IRON, 0.62 if x < 8 else 0.42))
    for x, y in ((4, 14), (8, 14), (11, 14)):  # feet
        put(img, x, y, IRON[2]); put(img, x - 1, y, IRON[3]) if x == 4 else None
    for x, y in ((4, 12), (11, 10), (9, 13)):
        put(img, x, y, mix(get(img, x, y), RUST, 0.4))
    save(img, "iron_cauldron", "item")

    # Bronze Ladle: deep bowl top right, a long handle down to the bottom left ending in a hook
    img = new()
    bowl = ellipse_px(11.0, 5.0, 3.6, 3.3)
    for x, y in bowl:
        u = (x + 0.5 - 11.0) / 3.6
        v = (y + 0.5 - 5.0) / 3.3
        t = 0.55 - 0.3 * u - 0.25 * v
        put(img, x, y, ramp(BRONZE, t))
    for x, y in ellipse_px(11.0, 4.0, 2.6, 1.5):  # the dark inside of the bowl, seen over the far rim
        put(img, x, y, ramp(BRONZE, 0.18 + 0.12 * ((x + 0.5 - 11) / 2.6 < -0.3)))
    for x in range(9, 14):
        if solid(img, x, 2):
            put(img, x, 2, ramp(BRONZE, 0.85))  # the far rim's lip, lit
    handle = [(7, 7), (6, 8), (5, 9), (4, 10), (3, 11), (2, 12), (2, 13), (3, 14)]
    for i, (x, y) in enumerate(handle):
        put(img, x, y, ramp(BRONZE, 0.74 - 0.03 * i))
        if i < 6:
            put(img, x + 1, y, ramp(BRONZE, 0.36))
    put(img, 8, 7, ramp(BRONZE, 0.45))
    put(img, 4, 14, ramp(BRONZE, 0.5)); put(img, 4, 13, ramp(BRONZE, 0.3))  # the hook curling back
    outline_dark(img, BRONZE[0], only_bottom_right=True)
    save(img, "bronze_ladle", "item")


# ---------------------------------------------------------------- dishes

SOUP_ROWS = {6: range(5, 11), 7: range(3, 13), 8: range(3, 13), 9: range(5, 11)}  # where the vanilla bowl holds its stew


def soup_px():
    return [(x, y) for y, xs in SOUP_ROWS.items() for x in xs]


def bowl_dish(name, pal, seed, base=0.5, bits=(), heap=False, swirl=None):
    """The vanilla bowl filled: a surface lit from the upper left (lighter at the back left, darker to the front right),
    bits = [(x, y, colour)] floating in it; heap = a mound rising over the back rim."""
    r = random.Random(seed)
    img = vanilla("item/bowl")
    px = soup_px()
    if heap:
        px = px + [(x, 5) for x in range(6, 10)] + [(7, 4), (8, 4)]
    for x, y in px:
        t = base + 0.12 * (y <= 6) - 0.1 * (y >= 9) - 0.06 * (x >= 10) + 0.05 * (x <= 5) + r.uniform(-0.07, 0.07)
        if heap and y <= 5:
            t = base + 0.18 - 0.08 * (x >= 8) + r.uniform(-0.04, 0.04)
        put(img, x, y, ramp(pal, t))
    if swirl:
        for x, y in swirl[0]:
            put(img, x, y, swirl[1])
    for x, y, c in bits:
        put(img, x, y, c)
    save(img, name, "item")


def dishes():
    G = (98, 128, 52)
    bowl_dish("nettle_soup", [(34, 58, 22), (58, 92, 34), (84, 124, 48), (120, 156, 70), (164, 192, 110)], 901,
              bits=[(5, 7, (30, 52, 20)), (8, 8, (30, 52, 20)), (11, 7, (40, 66, 26)), (7, 6, (176, 200, 120)), (10, 9, (186, 170, 120))])
    bowl_dish("root_pottage", [(140, 90, 40), (186, 128, 62), (216, 166, 96), (234, 202, 140), (246, 228, 184)], 902,
              bits=[(5, 7, (214, 112, 34)), (6, 7, (234, 140, 50)), (9, 8, (226, 214, 170)), (10, 7, (214, 112, 34)),
                    (7, 9, (196, 96, 30)), (11, 8, (236, 226, 190)), (8, 6, (246, 236, 200))])
    bowl_dish("berry_mash", [(56, 14, 30), (96, 24, 48), (136, 38, 66), (170, 66, 92), (204, 112, 132)], 903, heap=True,
              bits=[(6, 7, (150, 104, 60)), (9, 8, (176, 128, 76)), (11, 7, (150, 104, 60)), (7, 4, (224, 150, 170)), (5, 8, (40, 10, 30))])
    bowl_dish("pea_soup", [(70, 96, 26), (102, 132, 36), (132, 162, 54), (160, 186, 80), (192, 210, 120)], 904, base=0.55,
              bits=[(6, 6, (196, 214, 128)), (9, 8, (96, 126, 34)), (5, 8, (110, 140, 40)), (11, 8, (96, 126, 34))])
    bowl_dish("onion_garlic_stew", [(58, 32, 14), (90, 54, 24), (124, 80, 40), (156, 110, 60), (190, 150, 96)], 905,
              bits=[(4, 7, (226, 196, 130)), (5, 7, (196, 160, 96)), (5, 8, (226, 196, 130)), (9, 7, (230, 204, 140)), (10, 7, (226, 196, 130)),
                    (10, 8, (196, 160, 96)), (7, 9, (232, 226, 210)), (12, 8, G)])
    bowl_dish("spiced_porridge", [(170, 140, 96), (204, 178, 130), (226, 206, 164), (240, 226, 194), (250, 244, 224)], 906, heap=True,
              swirl=([(6, 7), (7, 8), (8, 8), (9, 7), (10, 7)], (226, 160, 40)),
              bits=[(5, 6, (150, 90, 44)), (8, 5, (150, 90, 44)), (11, 8, (130, 76, 36)), (7, 4, (166, 104, 52)), (9, 6, (246, 196, 80))])
    bowl_dish("herbal_tea", [(170, 140, 50), (204, 174, 76), (224, 200, 110), (238, 222, 150), (250, 240, 196)], 907, base=0.55,
              bits=[(6, 7, (110, 150, 70)), (10, 8, (130, 160, 80)), (5, 6, (252, 246, 210)), (8, 6, (252, 246, 210))])
    bowl_dish("tea", [(84, 40, 12), (124, 66, 22), (160, 96, 36), (192, 130, 56), (222, 170, 96)], 908,
              bits=[(9, 8, (70, 70, 30)), (5, 6, (232, 190, 120)), (7, 6, (222, 176, 106))])
    bowl_dish("cabbage_soup", [(150, 156, 100), (184, 190, 128), (210, 214, 160), (228, 230, 190), (244, 244, 220)], 909,
              bits=[(4, 7, (140, 176, 80)), (5, 8, (150, 186, 90)), (6, 8, (140, 176, 80)), (9, 7, (160, 196, 100)), (10, 7, (140, 176, 80)),
                    (11, 8, (150, 186, 90)), (8, 9, (140, 176, 80))])
    bowl_dish("vegetable_pottage", [(140, 60, 16), (188, 92, 26), (222, 128, 44), (238, 166, 80), (248, 204, 130)], 910,
              bits=[(5, 7, (236, 228, 200)), (6, 7, (206, 196, 168)), (9, 8, (240, 224, 170)), (10, 8, (210, 190, 140)),
                    (7, 9, (240, 224, 170)), (11, 7, (184, 120, 160)), (8, 6, (120, 160, 70))])
    painted_dishes()


def puck(img, cx, cy, rx, ry, h, top, side, seed, top_t=0.6):
    """A round flat thing seen from the front and above: an elliptic top and h rows of rim below it."""
    r = random.Random(seed)
    for dy in range(h, 0, -1):
        for x, y in ellipse_px(cx, cy + dy, rx, ry):
            u = (x + 0.5 - cx) / rx
            put(img, x, y, ramp(side, 0.5 - 0.35 * u - 0.12 * (dy / h) + r.uniform(-0.04, 0.04)))
    for x, y in ellipse_px(cx, cy, rx, ry):
        u, v = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
        put(img, x, y, ramp(top, top_t - 0.18 * u - 0.12 * v + r.uniform(-0.05, 0.05)))


def fish(r, raw):
    """The herb fish lying head left: roasted (golden-brown skin, grill bars) or raw and stuffed (silvery skin, herbs
    spilling from the belly slit, a clear eye)."""
    # ---- herb roasted fish: lying head left, crisp golden-brown skin lit along the back, grill scorch bars, herb flecks
    img = new()
    SKIN = ([(40, 48, 58), (74, 88, 100), (116, 132, 142), (160, 172, 176), (204, 206, 198), (236, 228, 214)] if raw
            else [(52, 28, 10), (96, 54, 20), (142, 90, 38), (184, 130, 62), (218, 174, 102), (240, 212, 152)])
    for x in range(1, 12):
        u = (x - 6.2) / 5.4
        hh = 3.3 * math.sqrt(max(0.0, 1 - u * u)) + (0.6 if x <= 3 else 0)  # a blunt head
        top, bot = round(7.5 - hh), round(7.5 + hh * 0.9)
        for y in range(top, bot + 1):
            v = (y - top) / max(1, bot - top)  # 0 back .. 1 belly
            t = 0.75 - 0.5 * v + r.uniform(-0.04, 0.04)
            if y in (top, bot):
                t = 0.12 if y == bot else 0.3
            put(img, x, y, ramp(SKIN, t))
    tail = {12: [(6, 9)], 13: [(5, 10)], 14: [(4, 6), (9, 11)], 15: [(3, 5), (10, 12)]}  # flaring out to a forked tail
    for x, spans in tail.items():
        for a_, b_ in spans:
            for y in range(a_, b_ + 1):
                t = 0.5 - 0.3 * (y - 3) / 9 - (0.18 if y in (3, 12) or (x == 15) else 0)
                put(img, x, y, ramp(SKIN, t))
    for x in (5, 6, 7, 8):  # the dorsal fin
        put(img, x, 3 if x in (6, 7) else 4, ramp(SKIN, 0.35))
    if raw:
        put(img, 3, 6, (20, 22, 28)); put(img, 4, 6, (230, 236, 240))  # a clear raw eye
    else:
        put(img, 3, 6, (250, 246, 232)); put(img, 3, 7, (40, 26, 12))  # the cooked eye
    for y in range(5, 11):  # the gill line
        if solid(img, 5, y):
            put(img, 5, y, mix(get(img, 5, y), SKIN[1], 0.6))
    for x0 in (() if raw else (7, 10)):  # grill bars, slanted
        for k in range(6):
            x, y = x0 + k // 3, 5 + k
            if solid(img, x, y):
                put(img, x, y, mix(get(img, x, y), (36, 18, 6), 0.6))
    if raw:  # the belly slit, packed with herb sprigs that stick out below it
        for x in range(6, 11):
            put(img, x, 9, (150, 70, 70) if x % 2 else (176, 96, 92))
        for x, y in ((6, 10), (7, 11), (8, 10), (9, 11), (10, 10), (7, 10), (9, 10), (8, 12)):
            put(img, x, y, (54, 112, 32) if (x + y) % 2 else (104, 160, 56))
    else:
        for x, y in ((4, 5), (6, 6), (8, 5), (9, 8), (11, 7), (7, 9), (12, 7)):
            if solid(img, x, y):
                put(img, x, y, (64, 124, 36) if (x + y) % 2 else (112, 164, 58))
    save(img, "herb_stuffed_fish" if raw else "herb_roasted_fish", "item")


def painted_dishes():
    r = random.Random(950)
    fish(r, False)
    fish(random.Random(951), True)

    # ---- flatbread: a round blistered loaf, scorch spots
    img = new()
    DOUGH = [(120, 74, 30), (170, 116, 56), (208, 158, 92), (230, 194, 130), (244, 222, 170)]
    puck(img, 8, 7.5, 6.5, 4.2, 2, DOUGH, DOUGH[:3] + [DOUGH[2]], 951, top_t=0.66)
    for x, y in ((5, 6), (9, 5), (11, 8), (7, 9), (4, 8)):  # blisters: a bright top, a scorched rim
        put(img, x, y, DOUGH[4]); put(img, x + 1, y, DOUGH[3]); put(img, x + 1, y + 1, (150, 96, 44))
    for x, y in ((12, 6), (6, 10)):  # scorch spots
        put(img, x, y, (120, 72, 30))
    outline_dark(img, (70, 40, 16))
    save(img, "flatbread", "item")

    # ---- raw honey cake: pale dough round, a honey swirl; honey cake: golden, glazed, a drip down the side
    def cake(top_pal, side_pal, seed):
        rr = random.Random(seed)
        im = new()
        cx, cy, rx, ry, h = 8, 6.5, 6.2, 2.7, 4
        for x, y in set(p for dy in range(1, h + 1) for p in ellipse_px(cx, cy + dy, rx, ry)):
            u = (x + 0.5 - cx) / rx
            put(im, x, y, ramp(side_pal, 0.6 - 0.4 * u - 0.12 * (y - cy - 1) / h + rr.uniform(-0.03, 0.03)))
        top = ellipse_px(cx, cy, rx, ry)
        for x, y in top:
            u, v = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
            put(im, x, y, ramp(top_pal, 0.68 - 0.15 * u - 0.1 * v + rr.uniform(-0.03, 0.03)))
        for x, y in top:  # the front edge of the top, where it turns down, catches the light
            if (x, y + 1) not in top:
                put(im, x, y, ramp(top_pal, 0.88 - 0.12 * (x > cx)))
        outline_dark(im, side_pal[0])
        return im
    PALE = [(150, 124, 86), (194, 170, 126), (222, 202, 162), (238, 226, 196), (250, 244, 226)]
    img = cake(PALE, PALE[:4], 952)
    for x, y in ((5, 6), (6, 5), (7, 5), (8, 6), (9, 7), (10, 6), (10, 5)):
        put(img, x, y, mix(get(img, x, y), (226, 166, 50), 0.6))  # a swirl of honey on the raw dough
    save(img, "raw_honey_cake", "item")
    CAKE = [(92, 46, 12), (142, 82, 24), (190, 124, 44), (222, 166, 70), (244, 206, 116)]
    GLAZE = [(150, 84, 10), (206, 132, 20), (236, 176, 40), (250, 214, 90), (255, 240, 170)]
    img = cake(GLAZE, CAKE, 953)
    for x, y in ((5, 5), (6, 5), (7, 4), (4, 6)):
        put(img, x, y, GLAZE[4])  # the glaze's shine
    for x, y0, y1 in ((10, 9, 12), (5, 9, 11), (12, 8, 10)):  # glaze drips down the side
        for y in range(y0, y1 + 1):
            put(img, x, y, GLAZE[3] if y == y0 else GLAZE[2] if y < y1 else GLAZE[1])
    save(img, "honey_cake", "item")

    # ---- elderberry syrup: a small stoppered clay flask, a dark purple drip
    img = new()
    FL = [(54, 30, 20), (96, 56, 34), (140, 86, 54), (176, 118, 78), (204, 152, 108)]
    lathe(img, {4: 1.6, 5: 1.6, 6: 2.4, 7: 3.6, 8: 4.6, 9: 5.0, 10: 5.0, 11: 4.8, 12: 4.2, 13: 3.0}, FL, 954)
    for x, y in ((7, 1), (8, 1), (7, 2), (8, 2), (7, 3), (8, 3)):  # the cork
        put(img, x, y, (196, 156, 100) if x == 7 else (150, 112, 66))
    put(img, 6, 3, (122, 90, 52)); put(img, 9, 3, (122, 90, 52))
    PURPLE = [(28, 6, 26), (54, 12, 48), (84, 22, 74), (130, 50, 116)]
    for x, y in ((9, 4), (10, 5), (10, 6), (11, 7), (11, 8), (12, 9), (12, 10)):  # the drip from the lip
        put(img, x, y, ramp(PURPLE, 0.75 if y < 6 else 0.45))
    put(img, 12, 11, PURPLE[3]); put(img, 12, 12, PURPLE[1])  # its bead
    for x in range(4, 12):  # a stained band where the label of string ties on
        if solid(img, x, 6):
            put(img, x, 6, mix(get(img, x, 6), (200, 180, 140), 0.4))
    save(img, "elderberry_syrup", "item")

    # ---- sloe jam: small clay pot, cloth cover tied with string, blue-black jam showing at the front
    img = new()
    POT = [(70, 40, 24), (112, 68, 40), (152, 98, 62), (186, 130, 88), (212, 164, 120)]
    lathe(img, {8: 4.6, 9: 5.0, 10: 5.2, 11: 5.2, 12: 4.8, 13: 4.0, 14: 3.0}, POT, 955)
    CLOTH = [(130, 112, 86), (176, 156, 122), (210, 194, 160), (234, 224, 196)]
    for y, (a, b) in {3: (6, 9), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12)}.items():
        for x in range(a, b + 1):
            t = 0.75 - 0.4 * (x - a) / max(1, b - a) + (0.12 if (x + y) % 3 == 0 else 0) - 0.1 * (y == 7)
            put(img, x, y, ramp(CLOTH, t))
    for x in range(3, 13):  # the string round the neck
        put(img, x, 8, (150, 110, 60) if x % 2 else (196, 156, 96))
    put(img, 12, 9, (196, 156, 96)); put(img, 13, 10, (150, 110, 60)); put(img, 2, 9, (150, 110, 60))  # ends of the tie
    for x, y in ((5, 4), (6, 4), (7, 4), (4, 5), (5, 5), (6, 5)):  # the cloth turned back at the front left: the jam
        put(img, x, y, (24, 20, 44) if y == 5 else (40, 34, 70))
    put(img, 5, 4, (110, 104, 160))  # its glossy highlight
    for x, y in ((8, 4), (7, 5)):  # the turned-back fold of cloth
        put(img, x, y, CLOTH[3])
    for x, y in ((3, 9), (4, 9), (3, 10)):  # jam oozing out under the cloth
        put(img, x, y, (30, 26, 52) if y == 9 else (52, 44, 88))
    put(img, 4, 9, (88, 80, 130))
    save(img, "sloe_jam", "item")

    # ---- fried eggs and onion on a wooden board
    img = new()
    for y in range(9, 14):  # the board, seen from above at a slant
        for x in range(1, 15):
            if (y in (9, 13) and x in (1, 14)):
                continue
            t = 0.6 - 0.25 * (y == 13) + 0.05 * math.sin(x * 1.7) + r.uniform(-0.04, 0.04)
            put(img, x, y, ramp(OAK, t if y < 13 else 0.3))
    WHITE = [(196, 186, 160), (226, 220, 200), (244, 242, 232), (255, 255, 250)]
    for cx, cy in ((5.5, 8.5), (10.5, 9.5)):
        for x, y in ellipse_px(cx, cy, 3.4, 2.4):
            put(img, x, y, ramp(WHITE, 0.7 - 0.25 * ((x + 0.5 - cx) / 3.4) - 0.2 * ((y + 0.5 - cy) / 2.4)))
        yx, yy = int(cx), int(cy)
        put(img, yx - 1, yy - 1, (246, 176, 30)); put(img, yx, yy - 1, (232, 150, 20))
        put(img, yx - 1, yy, (220, 132, 14)); put(img, yx, yy, (196, 110, 10))
        put(img, yx - 1, yy - 1, (255, 214, 90))
    for x, y in ellipse_px(5.5, 8.5, 3.4, 2.4) + ellipse_px(10.5, 9.5, 3.4, 2.4):  # crispy brown lace at the edges
        if any(not (abs((x + dx + 0.5 - 5.5) / 3.4) ** 2 + abs((y + dy + 0.5 - 8.5) / 2.4) ** 2 <= 1 or
                    abs((x + dx + 0.5 - 10.5) / 3.4) ** 2 + abs((y + dy + 0.5 - 9.5) / 2.4) ** 2 <= 1) for dx, dy in ((1, 0), (0, 1), (-1, 0), (0, -1))):
            if r.random() < 0.6:
                put(img, x, y, (196, 150, 80))
    for cx, cy in ((3, 12), (13, 7), (8, 12)):  # onion rings, golden
        for x, y in ((cx - 1, cy), (cx + 1, cy), (cx, cy - 1), (cx, cy + 1)):
            put(img, x, y, (214, 160, 70) if y <= cy else (170, 112, 40))
    save(img, "fried_eggs_and_onion", "item")

    # ---- sausages: a chain of links coiled round, pinched at the twists
    def links(pal, seed, path):
        rr = random.Random(seed)
        im = new()
        for i, (x, y) in enumerate(path):
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    if abs(dx) + abs(dy) <= 1 or i % 4 != 0:
                        if abs(dx) + abs(dy) == 2:
                            continue
                        t = 0.55 - 0.18 * dx - 0.2 * dy + rr.uniform(-0.04, 0.04)
                        if not solid(im, x + dx, y + dy) or dy < 0:
                            put(im, x + dx, y + dy, ramp(pal, t))
        for i, (x, y) in enumerate(path):
            if i % 4 == 0 and 0 < i < len(path) - 1:
                put(im, x, y, ramp(pal, 0.15))  # the twist between links
            elif i % 4 == 2:
                put(im, x - 1, y - 1, ramp(pal, 0.95))  # the skin's shine
        outline_dark(im, pal[0])
        return im
    path = [(2, 12), (3, 13), (4, 13), (5, 13), (6, 13), (7, 13), (8, 13), (9, 13), (10, 12), (11, 11), (12, 10), (12, 9),
            (12, 8), (11, 7), (10, 6), (9, 5), (8, 5), (7, 5), (6, 5), (5, 6), (4, 7), (4, 8), (5, 9), (6, 9), (7, 9), (8, 9)]
    path = [p for i, p in enumerate(path)]
    RAWS = [(120, 52, 58), (176, 92, 96), (214, 134, 132), (234, 172, 166), (248, 210, 204)]
    COOKS = [(54, 24, 10), (96, 46, 18), (140, 74, 30), (180, 110, 52), (220, 160, 98)]
    save(links(RAWS, 956, path), "raw_sausage", "item")
    ck = links(COOKS, 957, path)
    for x, y in ((5, 12), (9, 12), (11, 9), (7, 4)):  # char marks
        if solid(ck, x, y):
            put(ck, x, y, (40, 18, 8))
    save(ck, "sausage", "item")

    # ---- sauerkraut: a small brown crock heaped with pale shredded cabbage
    img = new()
    CROCK = [(52, 34, 22), (88, 60, 38), (124, 88, 56), (156, 118, 80), (184, 150, 108)]
    lathe(img, {8: 5.2, 9: 5.6, 10: 5.8, 11: 5.8, 12: 5.4, 13: 4.6, 14: 3.4}, CROCK, 958)
    for x in range(3, 13):
        put(img, x, 8, ramp(CROCK, 0.85 - 0.05 * abs(x - 6)))  # the rim
    KRAUT = [(150, 146, 80), (190, 186, 110), (220, 214, 150), (238, 234, 190)]
    for y, (a, b) in {3: (6, 9), 4: (4, 11), 5: (3, 12), 6: (3, 12), 7: (3, 12)}.items():
        for x in range(a, b + 1):
            t = 0.65 - 0.35 * (x - a) / max(1, b - a) + (0.2 if (x * 2 + y) % 5 == 0 else 0) - (0.25 if (x + y * 3) % 7 == 0 else 0)
            put(img, x, y, ramp(KRAUT, t))
    for x, y in ((4, 9), (12, 10)):  # brine wet on the crock
        put(img, x, y, mix(get(img, x, y), (60, 60, 30), 0.4))
    save(img, "sauerkraut", "item")

    # ---- cheese: a wedge cut from a wheel, rind on the round back, holes on the cut face
    img = new()
    CH = [(178, 140, 52), (214, 178, 80), (236, 206, 112), (248, 230, 156), (255, 246, 204)]
    RIND = [(106, 70, 24), (146, 100, 36), (182, 134, 54)]
    top = [(x, y) for y in range(3, 9) for x in range(2, 15) if y >= 3 + (14 - x) * 0.45 and y <= 8]  # the top face, a fan
    for x, y in top:
        put(img, x, y, ramp(CH, 0.78 - 0.04 * (y - 3) + r.uniform(-0.03, 0.03)))
    for y in range(8, 13):  # the cut face, front
        for x in range(2, 15):
            if y - 8 <= (x - 2) * 0.0 + 5:
                put(img, x, y, ramp(CH, 0.55 - 0.03 * (y - 8) + r.uniform(-0.03, 0.03)))
    for x, y in ((14, 3), (14, 4), (14, 5), (14, 6), (14, 7), (13, 3)):
        put(img, x, y, RIND[2])
    for y in range(8, 13):
        put(img, 14, y, RIND[1]); put(img, 15, y, RIND[0])
    for x, y in [(x, y) for x, y in top if y == min(yy for xx, yy in top if xx == x)]:
        put(img, x, y, ramp(CH, 0.9))  # the knife edge catching the light
    for x in range(2, 15):
        put(img, x, 12, ramp(CH, 0.3))
        put(img, x, 8, ramp(CH, 0.92))
    for x, y in ((5, 10), (9, 9), (11, 11), (7, 11)):  # holes
        put(img, x, y, CH[0]); put(img, x + 1, y, CH[1])
    put(img, 6, 5, CH[1]); put(img, 10, 6, CH[1])
    outline_dark(img, RIND[0], only_bottom_right=True)
    save(img, "cheese", "item")

    # ---- indigo: a dark blue dye cake, broken, faceted, powdery
    img = new()
    IND = [(12, 14, 40), (24, 28, 72), (38, 48, 112), (60, 76, 150), (100, 120, 190), (150, 170, 220)]
    shape = {4: (6, 10), 5: (4, 11), 6: (3, 12), 7: (3, 13), 8: (3, 13), 9: (3, 12), 10: (4, 12), 11: (5, 11), 12: (6, 9)}
    for y, (a, b) in shape.items():
        for x in range(a, b + 1):
            # three facets: a lit top-left face, a mid front, a dark right
            if x + y < 13:
                t = 0.7
            elif x - y > 2:
                t = 0.25
            else:
                t = 0.45
            t += r.uniform(-0.06, 0.06)
            if x in (a, b) or y in (4, 12):
                t = 0.08
            put(img, x, y, ramp(IND, t))
    for x, y in ((6, 6), (8, 5), (5, 7), (10, 9), (7, 10)):
        put(img, x, y, IND[5] if x + y < 13 else IND[3])  # coppery-blue bloom and dust
    put(img, 9, 7, (120, 90, 140))  # the coppery sheen good indigo has
    save(img, "indigo", "item")


# ---------------------------------------------------------------- Steel Age kitchen (phase 6): blocks

STEEL = [(30, 34, 40), (56, 62, 70), (88, 96, 106), (124, 132, 142), (164, 172, 180), (204, 210, 216), (236, 240, 244)]
MARBLE = [(132, 128, 122), (170, 166, 160), (200, 196, 190), (222, 218, 212), (238, 235, 230), (250, 248, 244)]
ASH = [(84, 62, 38), (118, 90, 58), (152, 120, 82), (184, 152, 108), (210, 182, 136), (230, 208, 166)]


def marble(seed, base=0.62, veins=1.0):
    """Polished white marble: soft clouding and a few thin grey veins wandering across."""
    r = random.Random(seed)
    n = smooth_noise(seed + 1, 4)
    n2 = smooth_noise(seed + 2, 8)
    nv = smooth_noise(seed + 3, 4)
    img = new()
    for y in range(16):
        for x in range(16):
            t = base + (n[y][x] - 0.5) * 0.18 + (n2[y][x] - 0.5) * 0.06 + r.uniform(-0.015, 0.015)
            c = ramp(MARBLE, t)
            band = lambda xx, yy: int(nv[yy % 16][xx % 16] * 5 * veins)
            if band(x, y) != band(x + 1, y) or band(x, y) != band(x, y + 1):  # a vein where the clouding crosses a step
                c = mix(c, (104, 108, 116), 0.62 if band(x, y) % 2 else 0.38)
            put(img, x, y, c)
    return img


def steel_kitchen_blocks():
    r = random.Random(1201)
    n = smooth_noise(1202, 4)
    # ---- Steel Pan: spun bright steel, the lines of the lathe running round, heat colours toward the flames, soot below
    out = new()
    for y in range(16):
        for x in range(16):
            t = 0.66 + 0.06 * ((y * 5 + x // 4) % 3 == 0) - 0.05 * (y % 3 == 2) + (n[y][x] - 0.5) * 0.1 + r.uniform(-0.02, 0.02)
            c = ramp(STEEL, t)
            # heat colours climbing from the bottom edge: straw, bronze, purple, blue
            h = clamp((y - 7 + 1.2 * (n[(y + 4) % 16][x] - 0.5)) / 7)
            if h > 0:
                tint = ramp([(196, 170, 110), (170, 110, 70), (120, 70, 120), (60, 80, 150), (40, 44, 60)], h)
                c = mix(c, tint, 0.25 + 0.4 * h)
            if y >= 14:
                c = mix(c, SOOT[1], 0.6 + 0.2 * (y - 14))
            put(out, x, y, c)
    for x in range(16):  # the rolled lip catches the light
        put(out, x, 0, ramp(STEEL, 0.9))
        put(out, x, 1, ramp(STEEL, 0.75))
    save(out, "steel_pan_steel")
    fl = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            t = 0.6 + 0.05 * math.sin(d * 4.2) + r.uniform(-0.02, 0.02)
            ang = math.atan2(y + 0.5 - 8, x + 0.5 - 8)
            t += 0.18 * clamp(1 - abs(d - 4.5) / 1.6) * clamp(math.cos(ang + 2.36))  # polished sheen upper left
            c = ramp(STEEL, t)
            c = mix(c, (96, 70, 40), clamp((d - 2.5) / 4) * 0.45)  # seasoning: a golden-brown patina toward the walls
            c = mix(c, (120, 90, 140), clamp(1 - abs(d - 1.5) / 1.5) * 0.15)  # a faint heat rainbow over the flame
            put(fl, x, y, c)
    save(fl, "steel_pan_floor")
    tr = cast_iron(1205, 0.34)
    for y in range(16):
        for x in range(16):
            put(tr, x, y, mix(get(tr, x, y), SOOT[1], clamp((y - 4) / 10) * 0.6))
    save(tr, "steel_pan_trivet")

    # ---- Mortar and Pestle: polished marble, the inside stained by years of spice
    save(marble(1211, 0.5), "mortar_marble")
    rim = marble(1212, 0.62, 0.6)
    save(rim, "mortar_rim")
    ins = marble(1213, 0.5, 0.5)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            c = get(ins, x, y)
            c = mix(c, (170, 120, 40), clamp(1 - d / 6) * 0.45)          # saffron yellow ground into the floor
            c = mix(c, (96, 70, 48), clamp(1 - abs(d - 3.5) / 1.4) * 0.3)  # a ring of pepper and cinnamon dust
            put(ins, x, y, c)
    for x, y in ((6, 7), (9, 6), (8, 10), (10, 9), (5, 9)):
        put(ins, x, y, (60, 44, 32))  # crushed peppercorn
    save(ins, "mortar_inside")
    pe = new()
    for y in range(16):
        for x in range(16):
            t = 0.72 - 0.42 * abs((x % 4) - 1.2) / 2.8 + r.uniform(-0.02, 0.02)  # a round rod lit from the left
            put(pe, x, y, ramp(STEEL, t))
    for x in range(16):
        put(pe, x, 15, ramp(STEEL, 0.35))
    save(pe, "pestle_steel")
    pw = wood_grain(1215, 0.58, OAK, along_x=False)
    save(pw, "pestle_wood")

    # ---- Sieve: a bent ash hoop with a nailed scarf joint, woven steel wire mesh, a tub dusted with flour below
    hp = new()
    for y in range(16):
        for x in range(16):
            t = 0.62 + 0.06 * math.sin(y * 2.3 + math.sin(x * 0.4) * 1.5) + r.uniform(-0.025, 0.025) - 0.12 * (y in (0, 15))
            c = ramp(ASH, t)
            put(hp, x, y, c)
    for y in range(16):  # the scarf joint: a slanted seam with two copper nails
        put(hp, 10 + y // 6, y, ramp(ASH, 0.18))
    put(hp, 9, 5, (196, 120, 64)); put(hp, 12, 10, (196, 120, 64))
    for y in range(16):  # flour caught in the grain near the bottom
        if r.random() < 0.3:
            put(hp, r.randrange(16), 12 + r.randrange(4), (226, 220, 206))
    save(hp, "sieve_hoop")
    mesh = new()
    for y in range(16):
        for x in range(16):
            wx, wy = x % 2 == 0, y % 2 == 0
            if wx or wy:
                over = (x // 2 + y // 2) % 2 == 0  # the weave: a wire over, then under
                t = 0.5 if (wx and over) or (wy and not over) else 0.3
                c = ramp(STEEL, t + r.uniform(-0.04, 0.04))
                if r.random() < 0.12:
                    c = mix(c, (236, 232, 222), 0.6)  # flour in the wires
                put(mesh, x, y, c)
    save(mesh, "sieve_mesh")
    fd = new()
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - 8, y + 0.5 - 8)
            t = clamp(1 - d / 7.5)
            c = mix(ramp(OAK, 0.4 + r.uniform(-0.04, 0.04)), (240, 236, 226), clamp(t * 1.4 + r.uniform(-0.1, 0.1)))  # a soft heap of sifted flour
            put(fd, x, y, c)
    save(fd, "sieve_floor")


# ---------------------------------------------------------------- Steel Age kitchen (phase 6) and the phase 4-5 leftovers: items

LEATHER = [(46, 26, 14), (76, 44, 22), (108, 66, 34), (140, 92, 52), (170, 122, 76)]
STONEWARE = [(70, 60, 50), (108, 96, 80), (146, 132, 110), (180, 168, 144), (206, 196, 174), (226, 218, 200)]


def blade(img, pts, pal=STEEL, edge=6):
    """A blade along pts (lit spine first, edge pixel bright)."""
    for i, (x, y) in enumerate(pts):
        put(img, x, y, ramp(pal, 0.55 - 0.02 * i))
        put(img, x + 1, y + 1, pal[edge])


def knife_set():
    """A leather knife roll, unrolled, its pockets holding three steel blades upright: a broad cleaver, a long carving
    knife and a small paring knife; the rolled end on the right, a tie strap hanging."""
    img = new()
    r = random.Random(1301)
    # blades first (the pockets cover their handles)
    for y in range(2, 10):  # the cleaver: a broad blade, the edge bright on the left, a hanging hole
        for x in range(1, 6):
            t = 0.72 - 0.08 * (x - 1) + r.uniform(-0.03, 0.03)
            put(img, x, y, STEEL[6] if x == 1 else ramp(STEEL, t))
        put(img, 5, y, STEEL[2])
    for x in range(1, 6):
        put(img, x, 2, STEEL[5] if x < 5 else STEEL[3])
    put(img, 4, 3, STEEL[1])
    for y in range(1, 10):  # the carving knife: long, a point at the top, spine dark right, edge bright left
        if y >= 2:
            put(img, 7, y, STEEL[6] if y > 2 else STEEL[5])
        put(img, 8, y, ramp(STEEL, 0.6 - 0.02 * y))
        if y >= 3:
            put(img, 9, y, STEEL[2])
    for y in range(5, 10):  # the paring knife
        put(img, 11, y, STEEL[6] if y > 5 else STEEL[4])
        if y >= 6:
            put(img, 12, y, STEEL[2])
    # the leather roll across the bottom, pockets with stitched seams
    for y in range(9, 15):
        for x in range(0, 13):
            t = 0.55 + 0.15 * (y == 9) - 0.12 * (y >= 13) + r.uniform(-0.04, 0.04)
            put(img, x, y, ramp(LEATHER, t))
    for x in range(0, 13):
        put(img, x, 9, ramp(LEATHER, 0.82 if x % 4 else 0.7))  # the pockets' turned edge
    for x in (6, 10):
        for y in range(10, 14):
            if y % 2 == 0:
                put(img, x, y, (206, 178, 126))  # stitching between pockets
    for x, y in ((2, 10), (8, 10), (11, 10)):
        put(img, x, y, ramp(OAK, 0.3))  # handle ends peeking out of the pockets
    # the rolled-up end
    for y in range(8, 16):
        for x in range(13, 16):
            put(img, x, y, ramp(LEATHER, 0.75 - 0.22 * (x - 13) + r.uniform(-0.03, 0.03)))
    put(img, 13, 8, LEATHER[2]); put(img, 15, 8, LEATHER[1])
    for y in range(12, 16):
        put(img, 12 if y < 14 else 11, y, ramp(LEATHER, 0.4))  # the tie strap
    outline_dark(img, (20, 16, 14), only_bottom_right=True)
    save(img, "steel_knife_set", "item")


def spice_bowl(name, pal, seed, specks=(), seeds=()):
    """A small turned olivewood spice bowl heaped with a ground blend: the heap lit from the upper left, specks of the
    coarser parts, whole seeds on top."""
    r = random.Random(seed)
    img = new()
    BOWL = [(56, 38, 22), (92, 64, 36), (130, 94, 56), (164, 126, 82), (194, 160, 112)]
    lathe(img, {10: 6.0, 11: 6.2, 12: 5.8, 13: 5.0, 14: 3.4}, BOWL, seed)
    for x in range(2, 14):
        put(img, x, 10, ramp(BOWL, 0.85 - 0.05 * abs(x - 5)))  # the rim's lit front
    put(img, 6, 15, BOWL[1]); put(img, 7, 15, BOWL[1]); put(img, 8, 15, BOWL[0]); put(img, 9, 15, BOWL[0])  # foot
    heap = {4: (7, 8), 5: (5, 10), 6: (4, 11), 7: (3, 12), 8: (3, 12), 9: (2, 13)}
    for y, (a, b) in heap.items():
        for x in range(a, b + 1):
            u = (x - (a + b) / 2) / max(1, (b - a) / 2)
            t = 0.62 - 0.3 * u - 0.05 * (y - 4) + r.uniform(-0.06, 0.06)
            if y == min(yy for yy, (aa, bb) in heap.items() if aa <= x <= bb):
                t += 0.12  # the crest of the heap
            put(img, x, y, ramp(pal, t))
    cells = [(x, y) for y, (a, b) in heap.items() for x in range(a, b + 1)]
    for c in specks:
        for _ in range(6):
            x, y = r.choice(cells)
            put(img, x, y, c)
    for x, y, c in seeds:
        put(img, x, y, c)
    save(img, name, "item")


def plate(img, wood=OAK, cy=10.5):
    """A round wooden trencher seen from the front and a little above."""
    for x, y in ellipse_px(8, cy + 1, 7.4, 3.6):
        put(img, x, y, ramp(wood, 0.3))
    for x, y in ellipse_px(8, cy, 7.4, 3.6):
        u, v = (x + 0.5 - 8) / 7.4, (y + 0.5 - cy) / 3.6
        put(img, x, y, ramp(wood, 0.62 - 0.1 * u - 0.08 * v + 0.04 * math.sin(x * 1.3)))
    for x, y in ellipse_px(8, cy, 5.6, 2.6):
        put(img, x, y, ramp(wood, 0.5 + 0.03 * math.sin(x * 1.3 + y)))


def heap_on(img, cx, cy, rx, ry, h, pal, seed):
    """A mound rising h px over (cx, cy), lit from the upper left."""
    r = random.Random(seed)
    for x, y in ellipse_px(cx, cy - h / 2, rx, ry + h / 2):
        if y > cy + ry:
            continue
        u, v = (x + 0.5 - cx) / rx, (y + 0.5 - (cy - h / 2)) / (ry + h / 2)
        put(img, x, y, ramp(pal, 0.66 - 0.25 * u - 0.15 * v + r.uniform(-0.07, 0.07)))


def jug(img, body, pal, seed, handle=True):
    lathe(img, body, pal, seed)
    if handle:
        ys = sorted(body)
        top, bot = ys[2], ys[-4]
        for y in range(top, bot + 1):
            put(img, 13 + (0 if y in (top, bot) else 1), y, ramp(pal, 0.3))
        put(img, 12, top, ramp(pal, 0.4)); put(img, 12, bot, ramp(pal, 0.3))


def pie(name, filling, raw, seed):
    """A round pie seen from the front and above: a crimped crust rim, a lattice top with the filling showing through
    (bubbling over at one edge once baked); raw: pale dough and brighter filling."""
    r = random.Random(seed)
    img = new()
    CRUST = ([(150, 128, 92), (192, 170, 128), (220, 202, 162), (238, 226, 194), (250, 244, 224)] if raw
             else [(110, 60, 20), (160, 96, 38), (200, 138, 64), (226, 176, 96), (244, 212, 140)])
    TIN = [(40, 40, 44), (70, 70, 76), (104, 104, 110), (140, 140, 146)]
    cx, cy, rx, ry = 8, 8.5, 7.2, 4.8
    # the tin under it
    for dy in range(1, 3):
        for x, y in ellipse_px(cx, cy + dy, rx - 0.4, ry - 0.2):
            put(img, x, y, ramp(TIN, 0.6 - 0.35 * (x + 0.5 - cx) / rx))
    top = ellipse_px(cx, cy, rx, ry)
    for x, y in top:
        u, v = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
        inner = u * u + v * v < 0.62
        if inner and x % 3 != 1 and y % 3 != 1:
            put(img, x, y, ramp(filling, 0.55 - 0.2 * u - 0.15 * v + r.uniform(-0.06, 0.06)))  # between the lattice strips
        else:
            t = 0.62 - 0.18 * u - 0.12 * v + r.uniform(-0.04, 0.04)
            if not inner:  # the crimped rim: alternate bright ridges and shadowed dents
                t += 0.14 if (x + int(v * 8)) % 2 else -0.08
            put(img, x, y, ramp(CRUST, t))
    if not raw:
        for x, y in ((11, 9), (12, 9), (12, 10)):  # juice bubbled over the edge and caramelised
            put(img, x, y, filling[1])
        for x, y in ((5, 5), (8, 5), (6, 8)):
            put(img, x, y, CRUST[4])  # sugary shine on the lattice
    else:
        for x, y in ((4, 8), (12, 6), (7, 10)):
            put(img, x, y, (236, 230, 214))  # flour dusted on the dough
    outline_dark(img, (30, 22, 16), only_bottom_right=True)
    save(img, name, "item")


def steel_kitchen_items():
    knife_set()
    # spice blends: each its own colour in the same little bowl
    spice_bowl("kitchen_pepper", [(46, 34, 28), (76, 58, 44), (110, 84, 60), (140, 110, 80), (170, 140, 104)], 1311,
               specks=[(20, 16, 14), (190, 120, 60)])
    spice_bowl("sweet_spice", [(96, 46, 18), (140, 74, 30), (178, 106, 50), (206, 140, 76), (228, 176, 112)], 1312,
               specks=[(230, 196, 120), (90, 40, 16)])
    spice_bowl("golden_spice", [(150, 70, 6), (200, 110, 10), (232, 152, 24), (246, 192, 60), (252, 226, 120)], 1313,
               specks=[(176, 30, 20), (40, 30, 24)])
    spice_bowl("pickling_spice", [(110, 92, 50), (146, 126, 72), (178, 158, 98), (204, 186, 128), (226, 212, 160)], 1314,
               specks=[(30, 26, 22), (98, 130, 52)],
               seeds=[(5, 6, (222, 184, 70)), (8, 5, (226, 190, 76)), (10, 7, (210, 170, 60)), (7, 8, (26, 22, 18)), (4, 8, (104, 140, 56)),
                      (11, 9, (26, 22, 18)), (9, 9, (222, 184, 70))])
    spice_bowl("herb_salt", [(176, 182, 172), (206, 212, 204), (230, 234, 228), (244, 246, 242), (252, 252, 250)], 1315,
               specks=[(70, 120, 46), (110, 150, 60), (60, 90, 40)])

    # fine flour: a heap of very white flour on a folded linen cloth
    img = new()
    LINEN = [(150, 140, 118), (190, 180, 156), (218, 210, 188), (236, 230, 212)]
    for y in range(9, 15):
        for x in range(1, 15):
            if abs(x - 7.5) + (14 - y) * 0.9 < 9:
                put(img, x, y, ramp(LINEN, 0.7 - 0.08 * (y - 9) + (0.12 if (x + y) % 3 == 0 else 0)))
    heap_on(img, 8, 11, 6.0, 2.4, 3, [(196, 198, 196), (222, 224, 222), (240, 240, 238), (250, 250, 248), (255, 255, 255)], 1316)
    outline_dark(img, (120, 112, 96), only_bottom_right=True)
    save(img, "fine_flour", "item")

    # sea buckthorn juice: a stoppered stoneware flask, orange juice running from the lip, a berry sprig tied on
    img = new()
    lathe(img, {4: 1.6, 5: 1.6, 6: 2.6, 7: 3.8, 8: 4.6, 9: 5.0, 10: 5.0, 11: 4.8, 12: 4.2, 13: 3.0}, STONEWARE, 1321)
    for x, y in ((7, 1), (8, 1), (7, 2), (8, 2), (7, 3), (8, 3)):
        put(img, x, y, (196, 156, 100) if x == 7 else (150, 112, 66))
    for x, y in ((9, 4), (10, 5), (10, 6), (11, 7)):
        put(img, x, y, (238, 140, 20) if y < 6 else (210, 110, 10))
    put(img, 11, 8, (250, 176, 40))
    for x in range(4, 12):
        if solid(img, x, 7):
            put(img, x, 7, mix(get(img, x, 7), (160, 120, 70), 0.5))  # the string
    for x, y in ((4, 8), (3, 9), (4, 9), (3, 10), (5, 9)):  # the sprig: orange berries, a silver leaf
        put(img, x, y, (246, 150, 24) if (x + y) % 2 else (214, 112, 10))
    put(img, 2, 8, (150, 170, 150)); put(img, 2, 9, (120, 140, 120))
    save(img, "sea_buckthorn_juice", "item")

    # cider: a pale stoneware jug with a handle, a cork, a cider drip, a crab apple leaning on it
    img = new()
    jug(img, {2: 1.6, 3: 1.8, 4: 2.4, 5: 3.6, 6: 4.6, 7: 5.2, 8: 5.4, 9: 5.4, 10: 5.2, 11: 4.8, 12: 4.0, 13: 2.8}, STONEWARE, 1331)
    for x in range(4, 12):
        if solid(img, x, 6):
            put(img, x, 6, mix(get(img, x, 6), (110, 80, 50), 0.45))  # a brown salt-glaze band
    put(img, 7, 1, (196, 156, 100)); put(img, 8, 1, (150, 112, 66))
    for x, y in ((6, 3), (5, 4)):
        put(img, x, y, (236, 196, 90))  # the cider's drip
    for x, y in ellipse_px(4.0, 12.5, 2.6, 2.4):
        put(img, x, y, (214, 186, 70) if x < 4 else (186, 60, 40))  # a crab apple: yellow with a red cheek
    put(img, 3, 11, (240, 220, 120)); put(img, 4, 10, (90, 60, 30)); put(img, 5, 9, (90, 140, 50))
    save(img, "cider", "item")

    # ale: a wooden tankard of staves and two iron hoops, a lid hinge, foam spilling over the rim
    img = new()
    for y in range(5, 15):
        for x in range(3, 12):
            s_ = (x - 3) // 2
            t = 0.55 + 0.08 * (x % 2 == 1) - 0.22 * (x == 11) + 0.1 * (x == 4) + r_unif(1341, x, y)
            c = ramp(OAK, t)
            if x in (3, 11):
                c = ramp(OAK, 0.22)
            if y in (6, 12):
                c = ramp(IRON, 0.5 - 0.3 * (x > 8))  # the hoops
            put(img, x, y, c)
    for y in range(7, 12):  # the handle
        put(img, 12 if y in (7, 11) else 13, y, ramp(OAK, 0.35))
    put(img, 12, 7, ramp(OAK, 0.45))
    FOAM = [(200, 186, 150), (230, 220, 192), (246, 240, 222), (255, 252, 240)]
    for y, (a, b) in {2: (5, 9), 3: (3, 11), 4: (2, 12), 5: (3, 11)}.items():
        for x in range(a, b + 1):
            put(img, x, y, ramp(FOAM, 0.75 - 0.4 * (x - a) / max(1, b - a) + (0.15 if (x + y) % 3 == 0 else 0)))
    for x, y in ((3, 6), (3, 7), (10, 6)):
        put(img, x, y, FOAM[2])  # foam running down the side
    for x in range(4, 11):
        put(img, x, 14, ramp(OAK, 0.18))
    save(img, "ale", "item")

    # roast with herbs: a browned joint on a board, rosemary sprigs and thyme, a bone end showing
    img = new()
    plate(img, OAK, 11)
    ROAST = [(54, 24, 10), (92, 44, 16), (136, 72, 30), (176, 106, 50), (212, 150, 84)]
    heap_on(img, 7.5, 10, 5.2, 2.6, 6, ROAST, 1351)
    for x, y in ((11, 6), (12, 6), (13, 5), (13, 6)):
        put(img, x, y, (236, 226, 200))  # the bone
    put(img, 13, 5, (250, 244, 228))
    for x, y in ((5, 6), (6, 5), (7, 7), (8, 6), (9, 8), (4, 8)):
        put(img, x, y, (64, 112, 40) if (x + y) % 2 else (100, 150, 58))  # herbs pressed into the crust
    for x, y in ((2, 12), (3, 12), (4, 13), (5, 13), (12, 12), (13, 12), (11, 13)):
        put(img, x, y, (50, 96, 40) if x % 2 else (90, 140, 60))  # sprigs on the board
    for x, y in ((6, 9), (9, 10)):
        put(img, x, y, (230, 170, 100))  # glistening fat
    save(img, "roast_with_herbs", "item")

    # saffron rice: golden rice heaped on a wooden plate, green herbs and onion
    img = new()
    plate(img, OAK, 11)
    RICE = [(170, 100, 10), (214, 148, 24), (238, 192, 56), (250, 222, 110), (255, 242, 180)]
    heap_on(img, 8, 10.5, 4.4, 1.8, 4, RICE, 1361)
    r = random.Random(1362)
    for _ in range(14):  # single grains catching the light
        x, y = r.randrange(4, 13), r.randrange(6, 12)
        if solid(img, x, y) and get(img, x, y)[0] > 150:
            put(img, x, y, RICE[4] if r.random() < 0.6 else RICE[1])
    for x, y in ((6, 7), (10, 8), (8, 10)):
        put(img, x, y, (80, 140, 50))
    for x, y in ((5, 9), (11, 10)):
        put(img, x, y, (236, 232, 210))
    put(img, 9, 6, (176, 30, 20))  # a saffron thread
    save(img, "saffron_rice", "item")

    # peppered steak: two seared steaks on a plate, a black pepper crust, grill bars
    img = new()
    plate(img, OAK, 11)
    STEAK = [(64, 28, 14), (110, 52, 28), (150, 80, 44), (186, 112, 66), (216, 150, 100)]
    for cx, cy in ((6, 9.5), (10, 10.5)):
        for dy in (1, 0):
            for x, y in ellipse_px(cx, cy + dy, 3.3, 2.0):
                u = (x + 0.5 - cx) / 3.3
                put(img, x, y, ramp(STEAK, (0.25 if dy else 0.62 - 0.2 * u) + random.Random(x * 31 + y).uniform(-0.05, 0.05)))
        for x, y in ellipse_px(cx, cy, 3.3, 2.0):
            if (x - y) % 4 == 0:
                put(img, x, y, (60, 28, 14))  # grill bars
            elif (x * 7 + y * 3) % 6 == 0:
                put(img, x, y, (24, 22, 20))  # cracked pepper
    for x, y in ((7, 8), (11, 9)):
        put(img, x, y, (220, 160, 120))  # fat edge
    save(img, "peppered_steak", "item")

    # spiced pilaf: the bowl of rice with meat, onion, pepper flecks
    bowl_dish("spiced_pilaf", [(150, 110, 60), (190, 150, 92), (222, 190, 132), (240, 220, 172), (250, 240, 210)], 1371, heap=True,
              bits=[(5, 7, (120, 60, 30)), (9, 8, (110, 54, 26)), (11, 7, (130, 66, 34)), (7, 4, (40, 30, 24)), (8, 6, (40, 30, 24)),
                    (6, 6, (236, 230, 200)), (10, 6, (90, 140, 50)), (4, 8, (40, 30, 24))])

    # pickled vegetables: an open stoneware crock, brine with onions, carrot and dill showing at the surface
    img = new()
    lathe(img, {7: 5.6, 8: 6.0, 9: 6.2, 10: 6.2, 11: 6.0, 12: 5.6, 13: 4.8, 14: 3.6}, STONEWARE, 1381)
    for y, (a, b) in {4: (4, 11), 5: (3, 12), 6: (3, 12)}.items():
        for x in range(a, b + 1):
            put(img, x, y, mix((120, 120, 70), (170, 168, 110), 0.5 + 0.3 * (x < 7) - 0.2 * (y == 6)))
    for x in range(2, 14):
        put(img, x, 7, ramp(STONEWARE, 0.9 - 0.05 * abs(x - 5)))  # the rim
    for x, y in ((5, 5), (6, 5), (9, 4), (10, 5)):
        put(img, x, y, (236, 232, 214))  # pickled onions
    put(img, 6, 4, (250, 248, 236))
    for x, y in ((8, 5), (11, 6), (4, 6)):
        put(img, x, y, (222, 120, 40))  # carrot coins
    for x, y in ((7, 3), (8, 2), (7, 4), (9, 3), (12, 4)):
        put(img, x, y, (80, 130, 50) if (x + y) % 2 else (120, 160, 70))  # dill fronds
    for x in range(4, 12):
        put(img, x, 10, mix(get(img, x, 10), (90, 60, 40), 0.35))  # a brown glaze band
    save(img, "pickled_vegetables", "item")

    # gingerbread: two stamped spice biscuits, raw pale and baked dark
    def biscuits(top, side, seed, icing):
        im = new()
        for cx, cy in ((6, 7), (10, 10)):
            puck(im, cx, cy, 4.6, 2.9, 2, top, side, seed + cx)
            for x, y in ((cx - 2, cy), (cx + 2, cy), (cx, cy - 1), (cx, cy + 1), (cx, cy)):  # the stamped star
                put(im, x, y, icing if icing else top[1])
        outline_dark(im, side[0])
        return im
    save(biscuits([(150, 110, 70), (184, 144, 98), (210, 176, 128), (228, 202, 158), (242, 226, 192)],
                  [(120, 84, 50), (150, 110, 70), (180, 140, 96), (200, 162, 116)], 1391, None), "raw_gingerbread", "item")
    save(biscuits([(70, 34, 14), (106, 56, 24), (142, 82, 38), (172, 108, 56), (200, 140, 80)],
                  [(50, 24, 10), (80, 40, 18), (110, 60, 28), (130, 76, 36)], 1392, (246, 236, 214)), "gingerbread", "item")

    # pies: one per fruit tree, raw and baked
    FILL = {"apple": [(120, 110, 40), (160, 150, 70), (196, 188, 110), (222, 216, 150), (240, 236, 196)],
            "plum": [(40, 10, 40), (74, 22, 70), (110, 40, 104), (146, 66, 136), (180, 110, 170)],
            "rowan": [(120, 30, 10), (176, 56, 16), (214, 90, 26), (236, 130, 50), (248, 176, 96)],
            "bird_cherry": [(14, 8, 16), (34, 16, 34), (58, 28, 56), (86, 46, 82), (120, 76, 112)]}
    for k, (f, pal) in enumerate(FILL.items()):
        pie(f"raw_{f}_pie", pal, True, 1400 + k)
        pie(f"{f}_pie", pal, False, 1410 + k)


def r_unif(seed, x, y, a=0.03):
    return random.Random(seed * 1000 + x * 17 + y).uniform(-a, a)


def main():
    cooking_pot()
    griddle()
    skillet()
    cauldron()
    barrel()
    press()
    salt_pan()
    cookware_items()
    dishes()
    steel_kitchen_blocks()
    steel_kitchen_items()
    print(f"cookware textures ok ({len(WRITTEN)} files)")


if __name__ == "__main__":
    main()
