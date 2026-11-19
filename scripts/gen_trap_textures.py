"""Textures for the traps (Traps.java, gen_traps.py): snare and fish trap blocks and items.
Twine colours from the mod's twine.png, bark from the vanilla stick, the fish from the mod's whole fish items.
Run: python scripts/gen_trap_textures.py"""
import zipfile
from io import BytesIO
from pathlib import Path

from PIL import Image

from gen_crude_ranged_textures import JAR

ROOT = Path(__file__).resolve().parent.parent
T = ROOT / "src/main/resources/assets/mechanical/textures"
BLOCK, ITEM = T / "block", T / "item"


def hexc(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def vanilla(path):
    jar = JAR if JAR.exists() else ROOT.parent.parent / "project_minecraft_mod/build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
    with zipfile.ZipFile(jar) as z:
        return Image.open(BytesIO(z.read(f"assets/minecraft/textures/{path}.png"))).convert("RGBA")


def colours(im):
    """Opaque colours of an image, darkest first."""
    return sorted({p[:3] for p in im.getdata() if p[3]}, key=sum)


TWINE = ["3a2c24", "534237", "634e42", "826655", "9a7d68"]  # twine.png plus a shadow and a highlight
stick = colours(vanilla("item/stick"))
BARK = ["%02x%02x%02x" % c for c in (stick[0], stick[len(stick) // 3], stick[2 * len(stick) // 3], stick[-1])]
WOOD = ["8a6a42", "a3834f", "bf9f68"]  # whittled pale wood


def new():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def stake():
    """Bark in vertical streaks with knots; the top rows are whittled pale wood (the carved notch end)."""
    im = new()
    for y in range(16):
        for x in range(16):
            if y < 3:
                c = WOOD[(x + y) % 3]
            else:
                c = BARK[1 + (x * 7 + (y // 3) * 3) % 3 if x % 4 else 0]
                if (x, y) in {(5, 7), (11, 12), (2, 13)}:
                    c = BARK[0]
            im.putpixel((x, y), hexc(c))
    return im


def twine():
    """Two plies twisted together: diagonal bands, a dark seam between them."""
    im = new()
    for y in range(16):
        for x in range(16):
            k = (x + y) % 4
            im.putpixel((x, y), hexc([TWINE[0], TWINE[2], TWINE[3], TWINE[4]][k] if k else TWINE[1]))
    return im


def bait():
    """A scatter of berries on a few leaves."""
    im = new()
    LEAF, LEAF2 = "3f6b2a", "557f34"
    BERRY = ["5a0f14", "8c1c20", "c22b2b", "e8706a"]
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), hexc(LEAF if (x * 3 + y) % 5 else LEAF2))
    for cx, cy in ((3, 3), (10, 2), (6, 8), (13, 9), (2, 12), (9, 13)):
        for dx in range(3):
            for dy in range(3):
                if (dx, dy) in {(0, 0), (2, 0), (0, 2), (2, 2)}:
                    continue
                c = BERRY[3] if (dx, dy) == (1, 0) else BERRY[2] if dy < 2 else BERRY[1]
                im.putpixel(((cx + dx) % 16, (cy + dy) % 16), hexc(c))
        im.putpixel(((cx + 2) % 16, (cy + 2) % 16), hexc(BERRY[0]))
    return im


def weave(open_gaps):
    """Stick withies: upright stakes every 4 px, weavers in 2-row bands going over and under them.
    With open_gaps the row between bands is see-through (the basket body), else tight (rim, end cap)."""
    im = new()
    for y in range(16):
        band, row = divmod(y, 4)
        for x in range(16):
            stake_col = x % 4 == 1
            if row >= 2 and open_gaps:
                if stake_col:
                    im.putpixel((x, y), hexc(BARK[2] if row == 2 else BARK[1]))
                continue
            if stake_col:
                over = (band + x // 4) % 2 == 0
                c = WOOD[1] if over else BARK[0]
            else:
                hi = (row % 2 == 0)
                c = WOOD[2] if hi else WOOD[0]
                if x % 4 in (0, 2) and not hi:
                    c = BARK[1]  # dips where it tucks under a stake
            im.putpixel((x, y), hexc(c))
    return im


def outline(im, col="2a2018"):
    """Dark outline around the drawn shape (item icons)."""
    out = im.copy()
    for y in range(16):
        for x in range(16):
            if im.getpixel((x, y))[3]:
                continue
            if any(0 <= x + dx < 16 and 0 <= y + dy < 16 and im.getpixel((x + dx, y + dy))[3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                out.putpixel((x, y), hexc(col))
    return out


def snare_item():
    """A stake leaning left with a twine noose hanging from its notch."""
    im = new()
    # stake: from (4,14) up to (6,3), 2 px wide, pale whittled tip at the top
    for i, y in enumerate(range(14, 2, -1)):
        x = 4 + (14 - y) // 6
        for dx in range(2):
            c = WOOD[2 - dx] if y < 5 else BARK[2 if dx == 0 else 1]
            im.putpixel((x + dx, y), hexc(c))
    # cord from the notch to the noose
    for x, y in ((8, 4), (9, 4), (10, 5)):
        im.putpixel((x, y), hexc(TWINE[3]))
    # noose: a ring centred (11, 9), radius ~3.5
    import math
    for a in range(0, 360, 8):
        x = round(11 + 3.4 * math.cos(math.radians(a)))
        y = round(9.5 + 3.6 * math.sin(math.radians(a)))
        if 0 <= x < 16 and 0 <= y < 16:
            im.putpixel((x, y), hexc(TWINE[2] if a < 180 else TWINE[4]))
    im.putpixel((11, 6), hexc(TWINE[1]))  # the slip knot
    im.putpixel((10, 6), hexc(TWINE[1]))
    return outline(im)


def fish_trap_item():
    """A long basket on its side: funnel mouth (left), woven body tapering to a tied end (right)."""
    im = new()
    for x in range(2, 15):
        # half height of the body at this x: full from the mouth, tapering over the last 4 px
        h = 4 if x < 11 else 4 - (x - 10)
        for y in range(8 - h, 8 + h):
            if x < 5:
                # the mouth: woven ring around a dark funnel opening
                ring = abs(y - 7.5) > h - 1.6 or x == 2
                c = (WOOD[2] if (y % 2) else WOOD[1]) if ring else ("1c1610" if x > 2 else WOOD[0])
            else:
                stake_row = y in (5, 7, 9) and h >= 3
                band = (x - 5) // 2
                if stake_row:
                    c = BARK[2]
                else:
                    c = WOOD[2] if (band + y) % 2 else WOOD[0]
                    if (x * 5 + y) % 7 == 0:
                        c = BARK[1]  # see-through gap: the dark inside
            im.putpixel((x, y), hexc(c))
    for y in (6, 7, 8, 9):  # twine binding at the closed end
        im.putpixel((14, y), hexc(TWINE[3] if y % 2 else TWINE[1]))
    # a fish tail showing through the side
    im.putpixel((8, 7), hexc("9aa8a2"))
    im.putpixel((9, 8), hexc("5d7764"))
    return outline(im)


def main():
    BLOCK.mkdir(parents=True, exist_ok=True)
    stake().save(BLOCK / "snare_stake.png")
    twine().save(BLOCK / "snare_twine.png")
    bait().save(BLOCK / "snare_bait.png")
    weave(True).save(BLOCK / "fish_trap_weave.png")
    weave(False).save(BLOCK / "fish_trap_rim.png")
    # the fish seen inside the full trap: the mod's own whole fish pictures
    Image.open(ITEM / "whole_salmon.png").convert("RGBA").save(BLOCK / "fish_trap_salmon.png")
    Image.open(ITEM / "whole_cod.png").convert("RGBA").save(BLOCK / "fish_trap_cod.png")
    snare_item().save(ITEM / "snare.png")
    fish_trap_item().save(ITEM / "fish_trap.png")
    print("trap textures ok")


if __name__ == "__main__":
    main()
