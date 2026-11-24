"""Textures for water power and the Sawmill (WaterPower.java, Sawmill.java; models in gen_waterpower.py).

Block textures (16x16) start from vanilla's oak and iron so they sit with the rest of the world, then get weathered:
- water_wheel_wood_v / _h: the wheel's boards (spokes, rims, paddles), oak darkened by water, a green slime film and
  black wet streaks; _v has the grain running up the texture, _h across (gen_waterpower.py picks by the board's length)
- water_wheel_hub_end: end grain of the hub, growth rings round a dark heart, cracked
- water_wheel_iron: forged strap iron, hammered, with rivets
- wooden_shaft_v / _h, wooden_shaft_end: an oiled oak shaft, grain along it, end grain with a centring mark
- gearbox_frame_v / _h: timber of the gearbox frame and the saw bench; gear_wood: dense end-grain-ish cog wood
- log_flume_side / _inside / _top: plank trough, iron bands outside; the inside wet and dark with a slime line
- saw_blade: the mill saw's steel blade, polished face, hooked teeth down the left edge
Items: sawdust, sawdust_briquette, water_wheel (the wheel seen face on).
Deterministic, overwrites its outputs. Run: python scripts/gen_waterpower_textures.py"""
import io
import math
import zipfile

from PIL import Image

from gen_crude_ranged_textures import JAR
from wildlife_paint import h, lerp, shade

from pathlib import Path

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
jar = zipfile.ZipFile(JAR)


def vanilla(name):
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/{name}.png"))).convert("RGBA")


def save(img, rel):
    p = OUT / f"{rel}.png"
    p.parent.mkdir(parents=True, exist_ok=True)
    img.save(p)


def px(img):
    return img.load()


SLIME = (74, 92, 46)
WET = (40, 34, 26)
IRON = [(34, 33, 36), (52, 51, 55), (74, 72, 76), (104, 102, 106), (140, 138, 140)]


# ---------------------------------------------------------------- wheel wood
def wet_board(salt):
    """Stripped oak turned grey-brown by years in the race; grain runs up the texture (v)."""
    src = vanilla("block/stripped_oak_log")
    img = Image.new("RGBA", (16, 16))
    s, d = px(src), px(img)
    for y in range(16):
        for x in range(16):
            r, g, b, _ = s[x, y]
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255
            c = lerp((90, 74, 52), (178, 154, 116), lum * 1.1)          # desaturated, older wood
            c = shade(c, 0.92 + (h(x, 0, salt) - 0.5) * 0.12)       # board-long streaks
            if x in (0, 15) or (x == 8 and h(8, y // 4, salt) > 0.4):  # board seams
                c = shade(c, 0.7)
            # water stain: black streaks running down, a green film low on the board
            if h(x, salt, 7) > 0.78 and h(x, y // 3, salt + 1) > 0.35:
                c = lerp(c, WET, 0.45)
            if y > 10 or h(x, y, salt + 3) > 0.93:
                c = lerp(c, SLIME, 0.25 + 0.05 * (y - 10) if y > 10 else 0.3)
            d[x, y] = c + (255,)
    # nail heads at the board ends
    for x in (3, 12):
        for y in (1, 14):
            d[x, y] = IRON[1] + (255,)
    return img


v = wet_board(11)
save(v, "block/water_wheel_wood_v")
save(v.transpose(Image.Transpose.ROTATE_90), "block/water_wheel_wood_h")


def end_grain(dark, light, salt, cracks=True, mark=False):
    img = Image.new("RGBA", (16, 16))
    d = px(img)
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - 7.5, y - 7.5)
            ring = (math.sin(r * 2.1 + h(x // 3, y // 3, salt) * 0.8) + 1) / 2
            c = lerp(dark, light, 0.35 + ring * 0.45 - r * 0.012)
            c = shade(c, 0.95 + (h(x, y, salt) - 0.5) * 0.08)
            if r < 1.6:
                c = shade(dark, 0.8)                                   # the heart
            if cracks and (abs(x - y) < 0.6 and 2 < r < 7 and x > 7 or abs(x - 7.5) < 0.6 and y < 5 and r > 2.5):
                c = shade(dark, 0.6)                                   # drying checks
            if r > 7.3:
                c = shade(dark, 0.85)                                  # bark-free rim, darker
            d[x, y] = c + (255,)
    if mark:
        for x, y in ((7, 7), (8, 8), (7, 8), (8, 7)):
            d[x, y] = IRON[2] + (255,)                                  # the iron centring pin
    return img


save(end_grain((92, 72, 48), (164, 136, 98), 3), "block/water_wheel_hub_end")


# ---------------------------------------------------------------- iron
def strap_iron():
    img = Image.new("RGBA", (16, 16))
    d = px(img)
    for y in range(16):
        for x in range(16):
            c = IRON[1 + (h(x // 2, y // 2, 5) > 0.55) + (h(x, y, 6) > 0.9)]   # hammer facets
            if y in (0, 15):
                c = IRON[0]
            if y == 1:
                c = IRON[3]                                               # worn top edge catches the light
            if h(x, y, 9) > 0.96:
                c = lerp(c, (110, 62, 36), 0.35)                           # rust freckles
            d[x, y] = c + (255,)
    for x in (2, 7, 13):                                                 # rivets with a highlight
        d[x, 7] = IRON[4] + (255,)
        d[x, 8] = IRON[0] + (255,)
        d[x + 1, 8] = IRON[0] + (255,)
        d[x + 1, 7] = IRON[3] + (255,)
    return img


save(strap_iron(), "block/water_wheel_iron")


# ---------------------------------------------------------------- shaft and gears
def oiled(src_name, salt, base=((78, 54, 30), (176, 132, 82))):
    src = vanilla(src_name)
    img = Image.new("RGBA", (16, 16))
    s, d = px(src), px(img)
    for y in range(16):
        for x in range(16):
            r, g, b, _ = s[x, y]
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255
            c = lerp(base[0], base[1], lum * 1.05)
            c = shade(c, 0.94 + (h(x, 0, salt) - 0.5) * 0.1)
            if h(x, y // 5, salt + 2) > 0.94:
                c = shade(c, 0.75)                                        # knots and oil-dark patches
            d[x, y] = c + (255,)
    return img


shaft = oiled("block/stripped_oak_log", 21)
save(shaft, "block/wooden_shaft_v")
save(shaft.transpose(Image.Transpose.ROTATE_90), "block/wooden_shaft_h")
save(end_grain((88, 60, 34), (180, 138, 88), 8, cracks=False, mark=True), "block/wooden_shaft_end")

frame = oiled("block/stripped_spruce_log", 31, ((64, 46, 30), (150, 116, 78)))
save(frame, "block/gearbox_frame_v")
save(frame.transpose(Image.Transpose.ROTATE_90), "block/gearbox_frame_h")
save(end_grain((70, 50, 30), (150, 112, 70), 13, cracks=False), "block/gear_wood")


# ---------------------------------------------------------------- flume
def flume_side():
    """Horizontal planks (the trough's walls run along it), an iron band down the middle."""
    src = vanilla("block/oak_planks")
    img = Image.new("RGBA", (16, 16))
    s, d = px(src), px(img)
    for y in range(16):
        for x in range(16):
            r, g, b, _ = s[x, y]
            c = lerp((96, 74, 44), (170, 136, 88), (r * 0.3 + g * 0.59 + b * 0.11) / 255 * 1.1)
            if y > 11:
                c = lerp(c, WET, 0.25 + (y - 11) * 0.06)                  # damp foot
            if 7 <= x <= 8:
                c = IRON[1 + (h(x, y, 4) > 0.6)]
                if y in (2, 6, 10, 14):
                    c = IRON[4] if x == 7 else IRON[0]
            d[x, y] = c + (255,)
    return img


def flume_inside():
    """The wet floor and inner walls: dark soaked planks, a slime line, silt in the seams."""
    src = vanilla("block/oak_planks")
    img = Image.new("RGBA", (16, 16))
    s, d = px(src), px(img)
    for y in range(16):
        for x in range(16):
            r, g, b, _ = s[x, y]
            lum = (r * 0.3 + g * 0.59 + b * 0.11) / 255
            c = lerp((44, 38, 28), (112, 92, 62), lum * 1.15)
            if h(x, y, 17) > 0.8:
                c = lerp(c, SLIME, 0.45)
            if y in (3, 7, 11, 15) and h(x, y, 18) > 0.3:
                c = lerp(c, (88, 80, 62), 0.4)                            # silt in the plank seams
            d[x, y] = c + (255,)
    return img


def flume_top():
    """The wall tops: end-on board edges, worn pale where logs scrape."""
    img = flume_side().copy()
    d = px(img)
    for y in range(16):
        for x in range(16):
            c = d[x, y][:3]
            if 7 <= x <= 8:
                c = lerp((140, 112, 72), (170, 140, 96), h(x, y, 2))
            d[x, y] = lerp(c, (182, 156, 112), 0.25 * h(x // 2, y, 3)) + (255,)
    return img


save(flume_side(), "block/log_flume_side")
save(flume_inside(), "block/log_flume_inside")
save(flume_top(), "block/log_flume_top")


# ---------------------------------------------------------------- saw blade
def saw_blade():
    """Polished steel, darker at the back edge, hooked teeth down the left side (x 0..2), a few file marks."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = px(img)
    for y in range(16):
        for x in range(16):
            if x >= 3:
                t = (x - 3) / 12
                c = lerp((206, 214, 222), (132, 140, 150), t)              # bright near the teeth
                c = shade(c, 0.97 + (h(x, y // 2, 41) - 0.5) * 0.06)
                if (x + y) % 7 == 0:
                    c = shade(c, 1.06)                                     # polish streaks
                d[x, y] = c + (255,)
            else:
                k = y % 4                                                  # one tooth every 4 px, hooked down
                if x >= 2 - (k if k < 3 else 0) // 1 and x >= max(0, 2 - k):
                    d[x, y] = (lerp((180, 188, 196), (236, 240, 244), k / 3)) + (255,)
                if k == 3 and x == 2:
                    d[x, y] = (110, 116, 124, 255)                         # gullet
    return img


save(saw_blade(), "block/saw_blade")


# ---------------------------------------------------------------- items
def sawdust():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = px(img)
    pal = [(150, 118, 74), (186, 150, 98), (212, 180, 126), (232, 206, 156)]
    for y in range(16):
        for x in range(16):
            dx, dy = (x - 7.5) / 6.8, (y - 10.5) / 4.2
            top = (x - 7.5) ** 2 / 30 + 5.5                                # a heap: rounded top
            if dx * dx + dy * dy <= 1.0 and y >= top:
                light = 1 - (y - top) / 10 + (7.5 - x) * 0.03
                k = min(3, max(0, int(light * 3 + (h(x, y, 51) - 0.5) * 1.6)))
                d[x, y] = pal[k] + (255,)
    for x, y in ((3, 13), (12, 12), (2, 14), (13, 14), (5, 6)):           # loose flakes round it
        d[x, y] = pal[2] + (255,)
    for x in range(3, 13):                                               # shadowed base line
        if d[x, 14][3]:
            d[x, 14] = pal[0] + (255,)
    return img


def briquette():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = px(img)
    dark, mid, light = (52, 38, 26), (86, 64, 42), (122, 94, 62)
    for y in range(3, 14):
        for x in range(2, 14):
            # a pressed brick seen a little from above: top face y 3..6, front y 7..13
            if y <= 6 and (x < 3 + (6 - y) or x > 12 - (6 - y) - 0):
                continue
            face = light if y <= 6 else mid
            c = shade(face, 0.9 + (h(x, y, 61) - 0.5) * 0.3)
            if h(x, y, 62) > 0.85:
                c = (168, 132, 84)                                       # flecks of sawdust in the clay
            if y == 13 or x in (2, 13):
                c = dark
            d[x, y] = c + (255,)
    return img


def wheel_icon():
    """The water wheel face on: rim, eight spokes, sixteen paddles, the hub."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = px(img)
    wood = [(64, 50, 34), (96, 76, 52), (132, 108, 76), (160, 134, 98)]
    c0 = 7.5
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - c0, y - c0)
            a = math.atan2(y - c0, x - c0)
            lit = 1 if (x - c0) - (y - c0) < 0 else 0                      # light from the top left
            if 5.4 <= r <= 6.8:
                d[x, y] = wood[2 + lit] + (255,)                             # rim
            elif r > 6.8 and r <= 7.9 and (a * 16 / (2 * math.pi)) % 1 < 0.42:
                d[x, y] = wood[1 + lit] + (255,)                             # paddle ends
            elif r < 1.8:
                d[x, y] = IRON[2 + lit] + (255,)                             # iron hub
            elif r < 5.4 and min(abs(math.sin(a - k * math.pi / 4)) for k in range(4)) * r < 0.62:
                d[x, y] = wood[1 + lit] + (255,)                             # spokes
    for x, y in ((7, 7), (8, 8)):
        d[x, y] = IRON[0] + (255,)
    return img


save(sawdust(), "item/sawdust")
save(briquette(), "item/sawdust_briquette")
save(wheel_icon(), "item/water_wheel")
print("waterpower textures ok")
