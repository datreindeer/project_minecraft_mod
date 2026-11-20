"""Textures for the bat and vanilla-game features (scripts/gen_wildtweaks.py):
- item/bat_wing, item/guano, item/night_eye_salve (the Marmot Salve pot filled with sooty paste)
- block/guano_pile (the heap), block/guano_splatter (droppings on the floor, with holes)
- entity/skinned/bat/bat.png and skinned/hoglin/* (flesh look of a skinned carcass, via gen_skinned_textures.skin)

Usage: python scripts/gen_wildtweaks_textures.py [VANILLA_ENTITY_TEXTURE_DIR]
(the vanilla dir is the one gen_skinned_textures.py reads; skinned textures are skipped if it is missing)"""
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_skinned_textures as skinned  # noqa: E402

RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
SRC = Path(sys.argv[1]) if len(sys.argv) > 1 else skinned.SRC


def h(x, y, salt):
    return skinned.h(x, y, salt) / 1000


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


# ---------------------------------------------------------------- bat wing

def bat_wing():
    """A spread wing: the arm along the top edge to the wrist and thumb claw, three fingers fanning down, scalloped membrane."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    shoulder, wrist = (0, 11), (12, 1)
    tips = [(15, 10), (11, 15), (5, 15)]
    body = (0, 15)
    mem = Image.new("L", (16, 16), 0)
    ImageDraw.Draw(mem).polygon([shoulder, wrist, *tips, body], fill=255)
    m = mem.load()
    # scallops: the trailing edge sags in between finger tips
    for a, b in zip([wrist] + tips[:-1], tips):
        if a == wrist:
            continue
        cx, cy = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        # push the bite toward the wrist
        dx, dy = wrist[0] - cx, wrist[1] - cy
        d = math.hypot(dx, dy)
        bx, by = cx - dx / d * 0.6, cy - dy / d * 0.6
        r = math.hypot(a[0] - b[0], a[1] - b[1]) * 0.36
        for y in range(16):
            for x in range(16):
                if math.hypot(x - bx, y - by) < r:
                    m[x, y] = 0
    # bite between the last finger and the body
    for y in range(16):
        for x in range(16):
            if math.hypot(x - 3.0, y - 16.4) < 1.8:
                m[x, y] = 0
    dark, mid, light, edge = (38, 27, 29), (60, 43, 42), (84, 62, 56), (26, 18, 20)
    px = img.load()
    for y in range(16):
        for x in range(16):
            if not m[x, y]:
                continue
            # lighter where the light shines through the thin middle; veins faintly
            dist = abs((wrist[1] - shoulder[1]) * x - (wrist[0] - shoulder[0]) * y + wrist[0] * shoulder[1] - wrist[1] * shoulder[0]) / math.hypot(wrist[1] - shoulder[1], wrist[0] - shoulder[0])
            t = max(0.0, min(1.0, 0.75 - dist * 0.09 + (h(x, y, 7) - 0.5) * 0.25))
            c = mix(dark, light, t) if t > 0.5 else mix(dark, mid, t * 2)
            edge_px = any(not (0 <= x + ox < 16 and 0 <= y + oy < 16 and m[x + ox, y + oy]) for ox, oy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            px[x, y] = (*(edge if edge_px else c), 255)
    d = ImageDraw.Draw(img)
    bone, bone_dark = (160, 136, 116, 255), (118, 96, 82, 255)
    for tip in tips:
        d.line([wrist, tip], fill=bone)
    d.line([shoulder, wrist], fill=bone)
    d.line([(shoulder[0], shoulder[1] + 1), (wrist[0] - 1, wrist[1] + 1)], fill=bone_dark)
    # wrist knuckle and hooked thumb claw
    px[wrist[0], wrist[1]] = (176, 156, 138, 255)
    px[wrist[0] + 1, wrist[1] - 1] = (60, 46, 40, 255)
    px[wrist[0] + 2, wrist[1] - 1] = (40, 30, 28, 255)
    px[wrist[0] + 2, wrist[1]] = (40, 30, 28, 255)
    return img


# ---------------------------------------------------------------- guano

GUANO = [(228, 224, 210), (196, 190, 172), (160, 150, 130), (118, 104, 86), (82, 70, 58), (246, 244, 236)]


def guano_color(x, y, shade, salt):
    """Shade 0 (lit) .. 1 (shadow): chalky off-white crust over grey-brown, with dark crumbs and bright white flecks."""
    r = h(x, y, salt)
    if r < 0.08:
        return GUANO[4]
    if r > 0.93 and shade < 0.6:
        return GUANO[5]
    i = shade * 3 + (h(x // 2, y, salt + 1) - 0.5) * 1.2
    i = max(0.0, min(3.0, i))
    lo = int(i)
    return mix(GUANO[lo], GUANO[min(3, lo + 1)], i - lo)


def guano_item():
    """A crumbly heap of droppings, lit from the top left."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    lumps = [(8, 7, 4.2), (4.5, 10.5, 3.4), (11.5, 11, 3.6), (7.5, 12, 3.2)]
    for y in range(16):
        for x in range(16):
            best = None
            for cx, cy, r in lumps:
                d = math.hypot(x + 0.5 - cx, (y + 0.5 - cy) * 1.15)
                if d < r and (best is None or cy > best[1]):
                    best = (cx, cy, r, d)
            if best is None:
                continue
            cx, cy, r, d = best
            # light from upper left; the rim of each lump darker
            shade = 0.45 + ((x + 0.5 - cx) + (y + 0.5 - cy)) / (2 * r) * 0.5 + (d / r) ** 3 * 0.35
            px[x, y] = (*guano_color(x, y, max(0.0, min(1.0, shade)), 11), 255)
    # dark outline below and right
    out = img.copy()
    o = out.load()
    for y in range(16):
        for x in range(16):
            if px[x, y][3] == 0 and any(0 <= x - ox < 16 and 0 <= y - oy < 16 and px[x - ox, y - oy][3] for ox, oy in ((1, 0), (0, 1))):
                o[x, y] = (64, 54, 46, 255)
    return out


def guano_block():
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            # clumps: a slow wave of shade with crumbly noise
            shade = 0.35 + 0.25 * math.sin(x * 0.9 + math.sin(y * 0.7) * 2) * math.cos(y * 0.8) + (h(x, y, 21) - 0.5) * 0.4
            px[x, y] = (*guano_color(x, y, max(0.0, min(1.0, shade)), 23), 255)
    return img


def guano_splatter():
    """Droppings fallen on the floor: round splats and drips, transparent in between."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    splats = [(4, 4, 2.4), (11, 3, 1.6), (9, 9, 3.0), (3, 12, 1.9), (13, 12, 2.2), (7, 14, 1.2), (14, 7, 1.1), (1, 8, 1.0)]
    for y in range(16):
        for x in range(16):
            for cx, cy, r in splats:
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d < r + (h(x, y, 31) - 0.5) * 0.8:
                    shade = 0.2 + d / r * 0.6
                    px[x, y] = (*guano_color(x, y, min(1.0, shade), 33), 255)
                    break
    return img


# ---------------------------------------------------------------- night-eye salve

def night_eye_salve():
    """The salve pot of the Marmot Salve, filled with black soot paste with a greasy shine."""
    img = Image.open(RES / "item/marmot_salve.png").convert("RGBA")
    px = img.load()
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            if not a:
                continue
            # the paste: the green-cream top of the marmot salve (greenish or pale, unlike the brown pot)
            if g >= r - 4 or (r > 200 and g > 180):
                lum = (r + g + b) / 3 / 255
                c = mix((22, 20, 24), (78, 74, 86), lum)
                if lum > 0.8 and h(x, y, 41) > 0.4:
                    c = (120, 116, 132)
                px[x, y] = (*c, a)
    return img


def skinned_textures():
    if not SRC.exists():
        print("  vanilla textures not found, skinned bat/hoglin skipped:", SRC)
        return 0
    n = 0
    for rel in ("bat/bat.png", "hoglin/hoglin.png", "hoglin/hoglin_baby.png"):
        src = SRC / rel
        dst = RES / "entity/skinned" / rel
        dst.parent.mkdir(parents=True, exist_ok=True)
        orig = Image.open(src)
        out = skinned.skin(orig)
        assert out.getchannel("A").tobytes() == orig.convert("RGBA").getchannel("A").tobytes()
        out.save(dst)
        n += 1
    return n


def main():
    out = {"item/bat_wing": bat_wing(), "item/guano": guano_item(), "item/night_eye_salve": night_eye_salve(),
           "block/guano_pile": guano_block(), "block/guano_splatter": guano_splatter()}
    for name, img in out.items():
        assert img.size == (16, 16)
        img.save(RES / f"{name}.png")
    print("wildtweaks textures: %d, skinned: %d" % (len(out), skinned_textures()))


if __name__ == "__main__":
    main()
