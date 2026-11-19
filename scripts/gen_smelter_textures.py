"""Smelting Furnace textures. Run: python scripts/gen_smelter_textures.py

- Furnace Bricks: buff fireclay bricks in offset courses with light lime mortar, each brick its own tone, lit from the
  top left, with dark iron specks; the Furnace Tap's front is the same wall with a breast of darker rammed clay, the
  tap hole (sooty above) and a peephole.
- Furnace Brick / Unfired Furnace Brick items: vanilla's brick shape recoloured (buff fired, grey raw clay with sand).
- Clay Channel: smooth fired clay outside, a darker sooty trough inside with a few metal splashes; the plug (raw
  clay); the Unfired Clay Channel item (a short trough seen from above).
- Glows: the fire through the peephole / tap hole (animated), the metal running down the spout (animated), and
  molten_flow.png, the near-white strip client/MoltenRenderer tints per metal and scrolls along channels.
- GUI: smelting_furnace.png, the kiln's thermometer, a tank for the melt (marks every 10 units) and the flame socket.
"""
import json
import random
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
if not JAR.exists():
    JAR = Path.home() / "Documents/project_minecraft_mod/build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
TEX = ROOT / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"
jar = zipfile.ZipFile(JAR)


def vanilla(path):
    return Image.open(BytesIO(jar.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def ramp(pal, t):
    t = max(0.0, min(1.0, t)) * (len(pal) - 1)
    i = min(int(t), len(pal) - 2)
    f = t - i
    return tuple(int(pal[i][c] * (1 - f) + pal[i + 1][c] * f) for c in range(3))


def save(img, path, frames=None):
    path.parent.mkdir(parents=True, exist_ok=True)
    img.save(path)
    mc = path.with_suffix(".png.mcmeta")
    if frames:
        mc.write_text(json.dumps({"animation": {"frametime": frames}}) + "\n")
    elif mc.exists():
        mc.unlink()


FIREBRICK = [(92, 66, 40), (142, 108, 70), (186, 152, 104), (214, 186, 136), (236, 216, 172)]  # buff fireclay
MORTAR = [(150, 144, 132), (188, 184, 172), (214, 210, 198)]
FIRED = [(84, 40, 26), (128, 64, 40), (160, 84, 54), (186, 108, 72)]                          # the kiln's fired clay
RAW = [(96, 100, 116), (126, 132, 148), (156, 162, 178), (176, 182, 196)]                     # vanilla clay ball greys
MOLTEN = [(130, 34, 4), (210, 84, 10), (250, 152, 30), (255, 214, 90), (255, 248, 204)]
SOOT = (34, 26, 22)


def mix(a, b, f):
    return tuple(int(a[c] * (1 - f) + b[c] * f) for c in range(3))


# ---------------- Furnace Bricks ----------------
def bricks(seed):
    """Courses 4 px high (3 brick + 1 mortar), bricks 8 wide, every other course offset by 4."""
    rng = random.Random(seed)
    img = Image.new("RGBA", (16, 16))
    tones = {}
    for y in range(16):
        course, row = y // 4, y % 4
        off = 4 if course % 2 else 0
        for x in range(16):
            col = (x + off) % 8
            if row == 3 or col == 7:
                t = 0.55 + rng.uniform(-0.1, 0.1) - (0.35 if row == 3 and col != 7 and rng.random() < 0.5 else 0)
                img.putpixel((x, y), ramp(MORTAR, t) + (255,))
                continue
            key = (course, (x + off) // 8)
            tone = tones.setdefault(key, rng.uniform(-0.12, 0.12))
            t = 0.6 + tone + rng.uniform(-0.07, 0.07)
            if row == 0:
                t += 0.14  # lit top edge
            if row == 2:
                t -= 0.14  # shadowed bottom edge above the mortar
            if col == 0:
                t += 0.06
            if col == 6:
                t -= 0.08
            c = ramp(FIREBRICK, t)
            if rng.random() < 0.06:  # iron specks of the fireclay
                c = mix(c, (70, 46, 30), 0.6)
            img.putpixel((x, y), c + (255,))
    return img


wall = bricks(71)
save(wall, BLOCK / "furnace_bricks.png")

# ---------------- Furnace Tap front ----------------
rng = random.Random(72)
front = wall.copy()
for y in range(16):
    for x in range(16):
        half = 3 if y == 6 else 4 if y >= 7 else -1  # an arched breast of rammed clay, x 4..11
        if half < 0 or abs(x - 7.5) > half:
            continue
        edge = abs(x - 7.5) > half - 1 or y == 6
        c = ramp(FIRED, 0.62 + rng.uniform(-0.08, 0.08) + (0.18 if edge else 0))
        front.putpixel((x, y), c + (255,))
for x in range(6, 10):  # the tap hole: rows 9..11 = model y 4..7, glowing deep inside, dark at the rim
    for y in range(9, 12):
        inner = 1 if 7 <= x <= 8 and y >= 10 else 0
        front.putpixel((x, y), ((88, 30, 12) if inner else (26, 14, 10)) + (255,))
for x in range(5, 11):  # soot licking up from the hole
    for y in range(6, 9):
        if rng.random() < 0.55 - (8 - y) * 0.12 + (0.2 if 6 <= x <= 9 else 0):
            front.putpixel((x, y), mix(front.getpixel((x, y))[:3], SOOT, 0.5 + rng.uniform(0, 0.2)) + (255,))
for x in range(6, 10):  # peephole ring and the hole (rows 2..3 = model y 12..14)
    for y in range(1, 5):
        ring = not (7 <= x <= 8 and 2 <= y <= 3)
        front.putpixel((x, y), (ramp(FIRED, 0.75 + rng.uniform(-0.05, 0.05)) if ring else (24, 14, 10)) + (255,))
save(front, BLOCK / "furnace_tap_front.png")

# ---------------- clay: channel outside, inside, plug ----------------
rng = random.Random(73)
clay = Image.new("RGBA", (16, 16))
for y in range(16):
    streak = rng.uniform(-0.05, 0.05)  # smoothed by hand along its length
    for x in range(16):
        clay.putpixel((x, y), ramp(FIRED, 0.62 + streak + rng.uniform(-0.06, 0.06)) + (255,))
save(clay, BLOCK / "clay_channel.png")
inside = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        c = mix(ramp(FIRED, 0.45 + rng.uniform(-0.08, 0.08)), SOOT, 0.35 + rng.uniform(0, 0.15))
        inside.putpixel((x, y), c + (255,))
for _ in range(5):  # splashes of metal that set in the trough
    x, y = rng.randrange(16), rng.randrange(16)
    for dx, dy in ((0, 0), (1, 0), (0, 1)):
        if rng.random() < 0.8:
            inside.putpixel(((x + dx) % 16, (y + dy) % 16), rng.choice([(176, 104, 60), (200, 160, 84), (150, 90, 52)]) + (255,))
save(inside, BLOCK / "clay_channel_inside.png")
plug = Image.new("RGBA", (16, 16))
for y in range(16):
    for x in range(16):
        c = ramp(RAW, 0.5 + rng.uniform(-0.12, 0.1))
        c = mix(c, (110, 84, 62), 0.35)  # grey clay browned by the heat
        if (x * 7 + y * 3) % 11 == 0:
            c = mix(c, (60, 50, 44), 0.5)  # thumb marks
        plug.putpixel((x, y), c + (255,))
save(plug, BLOCK / "furnace_tap_plug.png")

# ---------------- glows ----------------
glow = Image.new("RGBA", (16, 64))
for f in range(4):
    r = random.Random(600 + f)
    for x in range(16):
        for y in range(16):
            glow.putpixel((x, y + f * 16), ramp(MOLTEN, 0.45 + r.uniform(-0.25, 0.45)) + (255,))
save(glow, BLOCK / "furnace_tap_glow.png", frames=4)

# metal down the spout: streaks that move one row a frame toward v = 16 (the spout's mouth)
r = random.Random(601)
base = [[0.6 + r.uniform(-0.25, 0.35) for _ in range(16)] for _ in range(16)]
molten = Image.new("RGBA", (16, 128))
for f in range(8):
    for y in range(16):
        for x in range(16):
            t = base[(y - f * 2) % 16][x]
            molten.putpixel((x, y + f * 16), ramp(MOLTEN, t) + (255,))
save(molten, BLOCK / "furnace_tap_molten.png", frames=2)

# molten_flow: near white, tinted per metal by the renderer; streaky along v and seamless when it repeats
r = random.Random(602)
flow = Image.new("RGBA", (16, 16))
crust = [[0.0] * 16 for _ in range(16)]
for _ in range(6):  # floating crust streaks, a few px long along the flow
    x, y, n = r.randrange(16), r.randrange(16), r.randint(2, 5)
    for i in range(n):
        crust[(y + i) % 16][x] = max(crust[(y + i) % 16][x], 0.5 - i * 0.06)
for y in range(16):
    for x in range(16):
        v = 0.88 + r.uniform(-0.08, 0.12) - crust[y][x] * 0.6
        edge = 0.1 if x in (0, 15) or x in (4, 11) else 0  # the strip uses u 0.25..0.75: darker at its banks
        g = max(0, min(255, int(255 * (v - edge))))
        flow.putpixel((x, y), (min(255, g + 10), g, max(0, g - 18), 255))
save(flow, BLOCK / "molten_flow.png")

# ---------------- items ----------------
brick = vanilla("item/brick")
lum = lambda c: (c[0] * 3 + c[1] * 6 + c[2]) / 10
lums = [lum(brick.getpixel((x, y))) for y in range(16) for x in range(16) if brick.getpixel((x, y))[3]]
lo, hi = min(lums), max(lums)
for name, pal, speck in (("furnace_brick", FIREBRICK, (90, 62, 40)), ("unfired_furnace_brick", RAW, (196, 178, 128))):
    r = random.Random(name)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            p = brick.getpixel((x, y))
            if not p[3]:
                continue
            c = ramp(pal, (lum(p) - lo) / (hi - lo) * 0.9 + 0.1)
            if r.random() < 0.12 and lum(p) > lo + 10:
                c = mix(c, speck, 0.55)  # iron specks / sand grains
            img.putpixel((x, y), c + (p[3],))
    save(img, ITEM / f"{name}.png")

# Unfired Clay Channel: a short trough lying left-right, seen from above and a little in front
img = Image.new("RGBA", (16, 16))
r = random.Random(74)
for x in range(1, 15):
    for y in range(4, 13):
        if y in (4, 5):
            t = 0.85  # far wall top
        elif y in (10, 11):
            t = 0.8  # near wall top
        elif y == 12:
            t = 0.35  # near wall front
        else:
            t = 0.22 + (y - 6) * 0.05  # inside, shaded under the far wall
        if x in (1, 14):
            t -= 0.12
        img.putpixel((x, y), ramp(RAW, t + r.uniform(-0.05, 0.05)) + (255,))
for x in range(1, 15):
    img.putpixel((x, 13), (70, 72, 84, 255))
save(img, ITEM / "unfired_clay_channel.png")

# ---------------- GUI ----------------
W, H = 220, 96
gui = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
BG, DARK, LIGHT, INNER, BLACK = (198, 198, 198, 255), (85, 85, 85, 255), (255, 255, 255, 255), (30, 22, 20, 255), (0, 0, 0, 255)
for x in range(W):  # a vanilla-style panel: black rim with rounded corners, white top-left bevel, dark bottom-right
    for y in range(H):
        rim = ((x in (0, W - 1)) and 2 <= y <= H - 3) or ((y in (0, H - 1)) and 2 <= x <= W - 3) or (x, y) in ((1, 1), (W - 2, 1), (1, H - 2), (W - 2, H - 2))
        if rim:
            gui.putpixel((x, y), BLACK)
        elif 0 < x < W - 1 and 0 < y < H - 1:
            light, dark = x <= 2 or y <= 2, x >= W - 3 or y >= H - 3
            gui.putpixel((x, y), BG if light == dark else LIGHT if light else DARK)
GLASS_DARK = (55, 55, 55, 255)
# thermometer: the kiln's (inside x 22..27, y 18..61, bulb below)
for y in range(17, 63):
    gui.putpixel((21, y), GLASS_DARK)
    gui.putpixel((28, y), LIGHT)
for x in range(21, 29):
    gui.putpixel((x, 17), GLASS_DARK)
for x in range(22, 28):
    for y in range(18, 62):
        gui.putpixel((x, y), INNER)
for x in range(20, 30):
    for y in range(62, 69):
        dx, dy = x - 24.5, y - 65.5
        d = (dx * dx) / 25 + (dy * dy) / 12.25
        if d <= 1.0:
            gui.putpixel((x, y), INNER if d < 0.62 else GLASS_DARK if x < 25 else LIGHT)
for deg, col in ((232, (200, 204, 216, 255)), (950, (240, 122, 16, 255)), (1085, (255, 210, 80, 255))):  # tin, bronze, copper
    y = 62 - round(deg / 1300 * 44)
    for x in (29, 30, 31):
        gui.putpixel((x, y), col)
    gui.putpixel((29, y + 1), GLASS_DARK)
# tank for the melt: inside x 40..51, y 18..61 (160 quarters)
for y in range(17, 63):
    gui.putpixel((39, y), GLASS_DARK)
    gui.putpixel((52, y), LIGHT)
for x in range(39, 53):
    gui.putpixel((x, 17), GLASS_DARK)
    gui.putpixel((x, 62), LIGHT)
for x in range(40, 52):
    for y in range(18, 62):
        gui.putpixel((x, y), INNER)
for units in (10, 20, 30):
    y = 62 - round(units / 40 * 44)
    for x in (53, 54):
        gui.putpixel((x, y), GLASS_DARK)
# flame socket: vanilla furnace's unlit flame
gui.paste(vanilla("gui/container/furnace").crop((56, 36, 70, 50)), (22, 76))
gui.save(TEX / "gui/container/smelting_furnace.png")
print("smelter textures ok")
