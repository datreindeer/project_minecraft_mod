"""Textures for carcass hauling (CarcassHauling.java): the six meat cuts (Raw/Cooked Haunch, Ribs, Loin), the Travois item
and entity texture (UV boxes as in client/TravoisModel.java) and the Hanging Rack block (pole bark, log end, lashing).
Colours are sampled from the mod's own scraps, cordage, hide and tool rod textures so everything matches.
Overwrites its own outputs. Run: python scripts/gen_carcass_hauling_textures.py [preview.png]"""
import math
import random
import sys
from pathlib import Path

from PIL import Image

T = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
rng = random.Random(4711)


def palette(name, n=5):
    """The n most common opaque colours of a mod texture, dark to light."""
    img = Image.open(T / name).convert("RGBA")
    counts = {}
    for c in img.get_flattened_data() if hasattr(img, 'get_flattened_data') else img.getdata():
        if c[3] > 200:
            counts[c[:3]] = counts.get(c[:3], 0) + 1
    common = sorted(counts, key=counts.get, reverse=True)[:n]
    return sorted(common, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


RAW = palette("item/beef_scraps.png")
COOKED = palette("item/cooked_beef_scraps.png")
CORD = palette("item/cordage.png", 4)
HIDE = palette("item/scraped_hide.png", 4)
WOOD = palette("item/tool_rod.png", 4)
BONE = [(176, 160, 128), (214, 202, 170), (236, 228, 204)]
FAT_RAW = [(214, 170, 160), (238, 214, 204)]
FAT_COOKED = [(178, 120, 52), (214, 160, 78)]


# ---------------------------------------------------------------- cuts
def ellipse(x, y, cx, cy, rx, ry, a=0.0):
    ca, sa = math.cos(a), math.sin(a)
    dx, dy = x - cx, y - cy
    u, v = dx * ca + dy * sa, -dx * sa + dy * ca
    return (u / rx) ** 2 + (v / ry) ** 2


def segment(x, y, ax, ay, bx, by):
    """Distance from (x, y) to the segment a-b."""
    vx, vy = bx - ax, by - ay
    t = max(0.0, min(1.0, ((x - ax) * vx + (y - ay) * vy) / (vx * vx + vy * vy)))
    return math.hypot(x - ax - vx * t, y - ay - vy * t)


def paint(mask_fn, cooked, seed):
    """mask_fn(x, y) -> None | 'meat' | 'fat' | 'bone' at pixel centres; shaded from the top left, outlined."""
    r = random.Random(seed)
    meat, fat = (COOKED, FAT_COOKED) if cooked else (RAW, FAT_RAW)
    kinds = [[mask_fn(x + 0.5, y + 0.5) for x in range(16)] for y in range(16)]
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            k = kinds[y][x]
            if k is None:
                continue
            up = y == 0 or kinds[y - 1][x] is None
            left = x == 0 or kinds[y][x - 1] is None
            down = y == 15 or kinds[y + 1][x] is None
            right = x == 15 or kinds[y][x + 1] is None
            edge = up or left or down or right
            if k == "bone":
                c = BONE[2] if (up or left) else BONE[0] if (down or right) else BONE[1]
            elif k == "fat":
                c = fat[1] if (up or left) else shade(fat[0], 0.85) if (down or right) else fat[r.random() < 0.3]
            else:
                c = meat[3] if (up or left) else meat[0] if (down or right) else meat[1 + (r.random() < 0.45)]
                # marbling / seared crust specks
                if not edge and r.random() < 0.12:
                    c = mix(c, fat[1], 0.55)
            if edge and (down or right) and k != "bone":
                c = shade(c, 0.72)
            img.putpixel((x, y), c + (255,))
    return img


def haunch(x, y):
    """A leg: a round ham top right, tapering to the shank bone bottom left, a fat cap over the top."""
    knob = math.hypot(x - 2.2, y - 13.8) < 1.5 or math.hypot(x - 1.4, y - 12.4) < 1.0 or math.hypot(x - 3.6, y - 14.6) < 1.0
    shank = segment(x, y, 2.6, 13.4, 6.6, 9.4) < 1.15
    ham = ellipse(x, y, 10.2, 5.8, 5.2, 4.3, -0.62) <= 1
    taper = segment(x, y, 6.8, 9.0, 9.0, 7.0) < 2.1
    if ham or taper:
        return "fat" if ellipse(x, y, 10.2, 5.8, 5.2, 4.3, -0.62) > 0.6 and y < 5.8 + (x - 10.2) * 0.6 else "meat"
    if shank or knob:
        return "bone"
    return None


def ribs(x, y):
    """A rack of ribs: a slab of meat with four curved bones sticking out below."""
    slab = 2.0 <= y <= 9.5 + 0.12 * x and 1.5 <= x <= 14.5 and not (x < 3 and y < 3) and not (x > 13.2 and y < 3.2)
    if slab:
        top = y < 3.6
        return "fat" if top else "meat"
    for i in range(4):
        bx = 3.2 + i * 3.1
        # each bone curves down and a little left
        if abs(x - (bx - 0.18 * (y - 9))) < 0.85 and 9 <= y <= 14.2 + (i % 2) * 0.6:
            return "bone"
    return None


def loin(x, y):
    """A long, round loin roll lying on the diagonal, with a fat cap along its upper side."""
    d = segment(x, y, 3.0, 12.0, 12.6, 3.6)
    if d < 3.1:
        # the fat cap on the upper-left side of the roll
        side = (x - 3.0) * (3.6 - 12.0) - (y - 12.0) * (12.6 - 3.0)
        return "fat" if d > 1.9 and side > 0 else "meat"
    return None


def cut_end_ring(img, cx, cy, cooked):
    """The cut face at the loin's end: concentric meat rings."""
    meat = COOKED if cooked else RAW
    for y in range(16):
        for x in range(16):
            d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
            if d < 2.2 and img.getpixel((x, y))[3]:
                img.putpixel((x, y), (meat[2] if d < 1.0 else meat[3] if d < 1.8 else meat[1]) + (255,))
    return img


def cuts():
    out = {}
    for cooked in (False, True):
        p = "cooked" if cooked else "raw"
        out[f"item/{p}_haunch.png"] = paint(haunch, cooked, 11)
        out[f"item/{p}_ribs.png"] = paint(ribs, cooked, 12)
        out[f"item/{p}_loin.png"] = cut_end_ring(paint(loin, cooked, 13), 12.4, 3.8, cooked)
    return out


# ---------------------------------------------------------------- materials
def bark(w, h, seed, vertical=True):
    """Bark-on pole wood: streaks along the grain, a few knots."""
    r = random.Random(seed)
    img = Image.new("RGBA", (w, h))
    cols = [r.random() for _ in range(max(w, h))]
    for y in range(h):
        for x in range(w):
            g = cols[x if vertical else y]
            c = WOOD[0] if g < 0.25 else WOOD[1] if g < 0.6 else WOOD[2] if g < 0.9 else WOOD[3]
            if r.random() < 0.08:
                c = shade(c, 0.8)
            img.putpixel((x, y), c + (255,))
    for _ in range(max(1, w * h // 90)):
        kx, ky = r.randrange(w), r.randrange(h)
        img.putpixel((kx, ky), shade(WOOD[0], 0.7) + (255,))
    return img


def lashing(w, h, seed):
    """Cordage wound round and round: diagonal strands with dark gaps."""
    r = random.Random(seed)
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            k = (x + y) % 4  # strands wound on the diagonal, a dark gap between turns
            c = CORD[0] if k == 0 else CORD[1] if k == 1 else CORD[3] if k == 2 else CORD[2]
            if r.random() < 0.06:
                c = shade(c, 0.85)
            img.putpixel((x, y), c + (255,))
    return img


def log_end():
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.8:
                c = WOOD[0]
            else:
                ring = int(d * 1.3) % 2
                c = mix((196, 160, 104), (164, 128, 78), ring * 0.8)
            img.putpixel((x, y), c + (255,))
    return img


def hide(w, h, seed):
    r = random.Random(seed)
    img = Image.new("RGBA", (w, h))
    for y in range(h):
        for x in range(w):
            c = HIDE[1 + (r.random() < 0.35)]
            if r.random() < 0.05:
                c = HIDE[0]
            if r.random() < 0.03:
                c = HIDE[3]
            img.putpixel((x, y), c + (255,))
    return img


# ---------------------------------------------------------------- travois
def travois_entity():
    img = Image.new("RGBA", (128, 64))
    img.paste(bark(16, 64, 21), (0, 0))  # both poles: 8 x 63 each
    deck = hide(88, 27, 22)
    # the visible top of the hide (18 x 26 at u + d): darker stitched rim where it is lashed to the frame
    for y in range(26):
        for x in range(18):
            if x in (0, 17) or y in (0, 25):
                deck.putpixel((26 + x, y), HIDE[0] + (255,))
            elif (x in (1, 16) and y % 3 == 0) or (y in (1, 24) and x % 3 == 0):
                deck.putpixel((26 + x, y), CORD[1] + (255,))
    img.paste(deck, (16, 0))
    img.paste(bark(48, 4, 23, vertical=False), (16, 28))
    img.paste(lashing(12, 6, 24), (64, 28))
    return img


def travois_item():
    img = Image.new("RGBA", (16, 16))
    poles = ((1.0, 15.0, 10.5, 0.5), (15.0, 15.0, 5.5, 0.5))

    def pole_x(i, y):
        ax, ay, bx, by = poles[i]
        return ax + (bx - ax) * (ay - y) / (ay - by)

    # the hide stretched between the poles below where they cross
    for y in range(6, 14):
        for x in range(16):
            if pole_x(0, y + 0.5) + 1.2 < x + 0.5 < pole_x(1, y + 0.5) - 1.2:
                c = HIDE[0] if y in (6, 13) else HIDE[1 + ((x * 3 + y) % 5 == 0)]
                img.putpixel((x, y), c + (255,))
    # two poles crossing near the top
    for ax, ay, bx, by in poles:
        for i in range(60):
            t = i / 59
            x, y = round(ax + (bx - ax) * t - 0.5), round(ay + (by - ay) * t - 0.5)
            if 0 <= x < 16 and 0 <= y < 16:
                img.putpixel((x, y), (WOOD[2] if i % 7 else WOOD[1]) + (255,))
                if 0 <= x + 1 < 16:
                    img.putpixel((x + 1, y), WOOD[0] + (255,))
    for x, y in ((7, 2), (8, 2), (7, 3), (8, 3)):  # the lashing where they cross
        img.putpixel((x, y), CORD[2 if (x + y) % 2 else 1] + (255,))
    return img


def main():
    out = cuts()
    out["item/travois.png"] = travois_item()
    out["entity/travois.png"] = travois_entity()
    out["block/hanging_rack_pole.png"] = bark(16, 16, 31)
    out["block/hanging_rack_log_end.png"] = log_end()
    out["block/hanging_rack_lashing.png"] = lashing(16, 16, 32)
    for rel, img in out.items():
        p = T / rel
        p.parent.mkdir(parents=True, exist_ok=True)
        img.save(p)
    if len(sys.argv) > 1:  # contact sheet, 8x, for checking by eye
        icons = [img for rel, img in out.items() if img.size == (16, 16)]
        sheet = Image.new("RGBA", (len(icons) * 136, 136 + 520), (60, 60, 70, 255))
        for i, img in enumerate(icons):
            sheet.alpha_composite(img.resize((128, 128), Image.NEAREST), (i * 136 + 4, 4))
        sheet.alpha_composite(out["entity/travois.png"].resize((1024, 512), Image.NEAREST), (4, 140))
        sheet.save(sys.argv[1])
    print("carcass hauling textures ok:", len(out))


main()
