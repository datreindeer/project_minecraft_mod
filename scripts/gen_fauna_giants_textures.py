"""Paint the textures of the Ice Age giants (GiantsFauna): the four animals for their models (adult, calf, and the
skinned flesh version for butchered carcasses), the 16x16 items, the pegged raw hides, the rugs and the Ivory Figurine.

Usage: python scripts/gen_fauna_giants_textures.py
Deterministic. MAMMOTH / RHINO / BISON / MUSK_OX must match the cubes in client/wildlife/giants/*Model.java (texOffs,
origin, size); paint() asserts the boxes fit and never share a texel.
Looks: the mammoth a shaggy dark red-brown with ginger tips, grey trunk tip and soles, yellowed ivory; the rhino a warm
brown shag with a dark grey-brown horn worn pale on the front edge; the bison bull near-black with a golden-brown
woolly cape over the hump and shoulders and a black beard (cow lighter, calf rusty orange); the musk ox near-black guard
hair with a pale saddle, pale stockings, a cream horn boss with dark tips.
"""
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import egg, from_grid, hexc  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
CLEAR = (0, 0, 0, 0)
EYE, GLINT = (12, 9, 8), (196, 200, 206)

# ---------------------------------------------------------------- shared materials


def hair(c, i, j, salt, amt=0.1):
    """Long hair: strong vertical locks (texture-space streaks three texels long) over a fine speckle."""
    n = (h(i, j // 3, salt) - 0.5) * 2 * amt + (h(i, j, salt + 1) - 0.5) * amt * 0.6
    return shade(c, 1 + n)


def coat(pal, top, bottom, salt, spine=None, lock=0.1):
    """A coat box: darker along the back (or a pale saddle: spine colour), the flanks shading down to the belly."""
    def mat(v, face, x, y, z, i, j):
        p = pal[v]
        if face == "bottom":
            return hair(p["belly"], i, j, salt, lock * 0.6)
        if face == "top":
            c = lerp(p["dark"], p["coat"], min(1.0, abs(x) / 6))
            if spine and spine in p:
                c = lerp(c, p[spine], 0.75 * (1 - min(1.0, abs(x) / 5)))
            if h(i, j, salt + 3) < 0.06:
                c = lerp(c, p["tip"], 0.5)  # pale hair tips
            return hair(c, i, j, salt + 4, lock)
        t = (y - top) / max(1e-3, bottom - top)
        c = lerp(p["coat"], p["light"], 0.4 * (1 - abs(t - 0.3) / 0.3)) if t < 0.6 else lerp(p["coat"], p["dark"], (t - 0.6) / 0.4)
        if t < 0.15:
            c = lerp(p["dark"] if not (spine and spine in p) else p[spine], c, t / 0.15)
        if h(i, j // 2, salt + 5) < 0.1:
            c = lerp(c, p["tip"], 0.5)
        return hair(c, i, j, salt + 6, lock)
    return mat


def skirt(pal, length, salt, key="dark", jag=3):
    """Hanging hair, one texel thick: lighter at the root, darker locks towards the ragged, see-through ends."""
    def mat(v, face, x, y, z, i, j):
        p = pal[v]
        if face == "bottom":
            return CLEAR
        cut = length - int(h(round(x * 3 + z * 7), 3, salt) * jag + 0.2)
        if face != "top" and y > cut:
            return CLEAR
        t = y / length
        c = lerp(p["coat"], p[key], smooth(t))
        if h(round(z if face in ("left", "right") else x), j // 2, salt + 1) < 0.18:
            c = lerp(c, p["tip"], 0.45)
        return hair(c, i, j, salt + 2, 0.14)
    return mat


def solid(key, salt, amt=0.08, darken=1.0):
    def mat(v, face, x, y, z, i, j):
        return hair(shade(PAL_OF[v][key], darken), i, j, salt, amt)
    return mat


def gradient(cols, t0, t1, length, salt, axis="y", wear=None):
    """Horn / ivory along its length (t = t0 at the root .. t1 at the tip). axis: the cube's length axis, built towards -axis."""
    def mat(v, face, x, y, z, i, j):
        # built towards -axis; a negative length means built towards +y (the musk ox's horns hang down)
        tip_face, root_face = ("front", "back") if axis == "z" else ("bottom", "top") if length < 0 else ("top", "bottom")
        along = -z if axis == "z" else -y
        t = t1 if face == tip_face else t0 if face == root_face else t0 + (t1 - t0) * (along / length)
        k = t * (len(cols) - 1)
        a = min(int(k), len(cols) - 2)
        c = lerp(cols[a], cols[a + 1], k - a)
        if h(i, j, salt) < 0.25 and t < 0.75:
            c = shade(c, 0.9)  # growth rings
        if wear and face == wear:
            c = lerp(c, cols[-1], 0.3)  # worn pale edge
        if face in ("left", "top"):
            c = shade(c, 1.06)
        return c
    return mat


def skull_with_eyes(pal, eye_y, eye_z, salt, key="coat", front=None):
    """Hairy head box with an eye on each side (and a glint on the right one); front = material for the face."""
    def mat(v, face, x, y, z, i, j):
        p = pal[v]
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return EYE
            if abs(y - eye_y + 1) < 0.6 and abs(z - eye_z) < 0.6 and face == "right":
                return GLINT
            if abs(y - eye_y - 1) < 0.6 and abs(z - eye_z) < 1.6:
                return shade(p["dark"], 0.8)  # lower lid / eye hollow
        if face == "front" and front:
            return front(v, face, x, y, z, i, j)
        c = p[key] if face != "top" else lerp(p[key], p["tip"], 0.3)
        if face == "bottom":
            c = p["dark"]
        return hair(c, i, j, salt, 0.1)
    return mat


def leg(pal, top, bottom, salt, hoof=None, stocking=None, nails=False):
    """Hair on the upper part, a hoof (cloven) or a round foot with toenails at the bottom."""
    def mat(v, face, x, y, z, i, j):
        p = pal[v]
        t = (y - top) / (bottom - top)
        if face == "top":
            return p["dark"]
        if face == "bottom":
            return shade(p.get("hoof", p.get("skin")), 0.7 if abs(x) < 0.4 and hoof else 1.0)
        if hoof and y > bottom - 1:
            return shade(p["hoof"], 1.0 + 0.2 * (h(i, j, salt) - 0.5))
        if nails and y > bottom - 1:
            if face == "front" and int(x + 10) % 2 == 0:
                return p["nail"]
            return p["skin"]
        c = lerp(p["coat"], p["dark"], t)
        if stocking is not None and t > stocking:
            c = lerp(c, p["stocking"], min(1.0, (t - stocking) / 0.2))
        return hair(c, i, j, salt, 0.1)
    return mat


PAL_OF = {}  # variant -> palette, filled per animal


def pal(**kw):
    return {k: v for k, v in kw.items()}


# ---------------------------------------------------------------- woolly mammoth (128x128, MammothModel)

MAMMOTH_PAL = {
    "adult": pal(coat=(96, 58, 36), dark=(56, 34, 22), light=(130, 84, 50), tip=(176, 120, 70), belly=(62, 40, 27),
                 skin=(64, 58, 54), nail=(190, 180, 158), hoof=(52, 46, 42)),
    "calf": pal(coat=(150, 96, 56), dark=(102, 64, 38), light=(184, 128, 78), tip=(214, 164, 108), belly=(110, 72, 44),
                skin=(92, 82, 76), nail=(200, 190, 170), hoof=(70, 62, 56)),
}
IVORY = [(150, 122, 78), (206, 182, 132), (234, 220, 186), (244, 236, 214)]


def trunk_mat(t0, t1, length, salt):
    """Hair at the root fading into bare, ridged grey skin towards the tip; the tip segment's end is the dark 'finger'."""
    def mat(v, face, x, y, z, i, j):
        p = MAMMOTH_PAL[v]
        t = t0 + (t1 - t0) * (y / length) if face not in ("top", "bottom") else (t0 if face == "top" else t1)
        if face == "bottom" and t1 >= 1.0:
            return shade(p["skin"], 0.55) if abs(x) < 0.6 else shade(p["skin"], 0.8)
        c = lerp(p["coat"], p["skin"], smooth((t - 0.3) / 0.5))
        if t > 0.35 and int(y * 2 + t * 10) % 2 == 0:
            c = shade(c, 0.88)  # skin rings
        if face == "back":
            c = shade(c, 0.9)
        return hair(c, i, j, salt, 0.12 * (1 - t) + 0.03)
    return mat


def mammoth_face(v, face, x, y, z, i, j):
    """The face (skull front, the trunk hangs from its lower middle): hair, darker round the trunk root."""
    p = MAMMOTH_PAL[v]
    c = lerp(p["coat"], p["dark"], 0.3 + 0.4 * max(0.0, 1 - (abs(x) + abs(y - 4)) / 4))
    return hair(c, i, j, 30, 0.12)


def dome_mat(v, face, x, y, z, i, j):
    p = MAMMOTH_PAL[v]
    c = lerp(p["coat"], p["tip"], 0.35 if face == "top" else 0.15)  # the sun-bleached topknot
    return hair(c, i, j, 31, 0.13)


MAMMOTH = {
    "body": (0, 0, -7, -4, -10, 14, 12, 21, coat(MAMMOTH_PAL, -4, 8, 10)),
    "chest": (0, 33, -7.5, -7, -12, 15, 14, 9, coat(MAMMOTH_PAL, -7, 7, 14)),
    "hump": (82, 33, -5, -10, -11, 10, 4, 9, coat(MAMMOTH_PAL, -10, -6, 18)),
    "skirt": (70, 0, -1, 0, -11, 1, 7, 20, skirt(MAMMOTH_PAL, 7, 20)),
    "bib": (50, 68, -5, 0, -0.5, 10, 5, 1, skirt(MAMMOTH_PAL, 5, 22)),
    "tail": (82, 56, -1, 0, -1, 2, 6, 2, solid("dark", 24)),
    "tuft": (72, 68, -1.5, 5, -1.5, 3, 3, 3, solid("dark", 25, 0.15, 0.6)),
    "skull": (48, 33, -4.5, -6, -7, 9, 11, 8, skull_with_eyes(MAMMOTH_PAL, -1, -5, 26, front=mammoth_face)),
    "dome": (40, 56, -3.5, -9, -6, 7, 3, 6, dome_mat),
    "trunk1": (66, 56, -2, 0, -2, 4, 5, 4, trunk_mat(0.0, 0.3, 5, 40)),
    "trunk2": (90, 56, -1.5, 0, -1.5, 3, 5, 3, trunk_mat(0.3, 0.6, 5, 41)),
    "trunk3": (16, 68, -1.5, 0, -1.5, 3, 4, 3, trunk_mat(0.6, 0.85, 4, 42)),
    "trunk4": (84, 68, -1, 0, -1, 2, 3, 2, trunk_mat(0.85, 1.0, 3, 43)),
    "tusk1": (102, 56, -1, -1, -5, 2, 2, 5, gradient(IVORY, 0.0, 0.3, 5, 50, "z")),
    "tusk2": (0, 68, -1, -1, -5, 2, 2, 5, gradient(IVORY, 0.3, 0.6, 5, 51, "z")),
    "tusk3": (28, 68, -1, -1, -4, 2, 2, 4, gradient(IVORY, 0.6, 0.85, 4, 52, "z")),
    "tusk4": (92, 68, -0.5, -0.5, -3, 1, 1, 3, gradient(IVORY, 0.85, 1.0, 3, 53, "z")),
    "ear": (42, 68, -1, -2, -1.5, 1, 4, 3, solid("dark", 27, 0.1)),
    "leg_upper": (0, 56, -2.5, -1, -2.5, 5, 7, 5, leg(MAMMOTH_PAL, -1, 6, 60)),
    "leg_lower": (20, 56, -2.5, 0, -2.5, 5, 5, 5, leg(MAMMOTH_PAL, -1, 5, 61, nails=True)),
}
MAMMOTH_KEEP = ("tusk1", "tusk2", "tusk3", "tusk4")

# ---------------------------------------------------------------- woolly rhinoceros (128x64, RhinoModel)

RHINO_PAL = {
    "adult": pal(coat=(122, 84, 52), dark=(74, 50, 32), light=(156, 112, 72), tip=(190, 150, 100), belly=(84, 58, 38),
                 skin=(70, 62, 56), hoof=(46, 40, 36)),
    "calf": pal(coat=(160, 116, 74), dark=(112, 78, 48), light=(190, 146, 98), tip=(214, 178, 128), belly=(120, 86, 56),
                skin=(96, 86, 78), hoof=(70, 60, 54)),
}
RHINO_HORN = [(52, 44, 38), (84, 70, 56), (116, 100, 82), (150, 134, 112)]


def rhino_snout(v, face, x, y, z, i, j):
    """Bare grey-brown snout skin under the horn, nostrils and a wide square lip at the front."""
    p = RHINO_PAL[v]
    if face == "front":
        if y > 2.5:
            return shade(p["skin"], 0.7)  # lip line
        if abs(abs(x) - 1.5) < 0.6 and abs(y - 0.5) < 0.6:
            return shade(p["skin"], 0.45)  # nostrils
        return fur(p["skin"], i, j, 70, 0.08)
    if face == "top":
        return fur(lerp(p["skin"], p["dark"], 0.3), i, j, 71, 0.1)  # horn base, worn bare
    if face in ("left", "right") and y > 2.5:
        return shade(p["skin"], 0.75)
    return fur(lerp(p["skin"], p["coat"], 0.35 if y < 0 else 0.1), i, j, 72, 0.08)


RHINO = {
    "body": (0, 0, -7, -5, -12, 14, 13, 24, coat(RHINO_PAL, -5, 8, 110)),
    "hump": (32, 37, -6, -9, -13, 12, 5, 11, coat(RHINO_PAL, -9, -4, 114)),
    "skirt": (76, 0, -1, 0, -11, 1, 6, 21, skirt(RHINO_PAL, 6, 120)),
    "tail": (56, 54, -0.5, 0, -0.5, 1, 6, 1, solid("dark", 124)),
    "tuft": (96, 54, -1, 5, -1, 2, 2, 2, solid("dark", 125, 0.15, 0.6)),
    "neck": (0, 37, -4.5, -4, -6, 9, 10, 7, coat(RHINO_PAL, -4, 6, 126, lock=0.12)),
    "skull": (78, 37, -3.5, -3.5, -8, 7, 7, 9, skull_with_eyes(RHINO_PAL, 0.5, -6, 127)),
    "snout": (0, 54, -3, -2, -12, 6, 6, 4, rhino_snout),
    "horn1": (60, 54, -1, -4, -1.5, 2, 4, 3, gradient(RHINO_HORN, 0.0, 0.4, 4, 130, "y", wear="front")),
    "horn2": (70, 54, -1, -4, -1, 2, 4, 2, gradient(RHINO_HORN, 0.4, 0.75, 4, 131, "y", wear="front")),
    "horn3": (78, 54, -0.5, -4, -0.5, 1, 4, 1, gradient(RHINO_HORN, 0.75, 1.0, 4, 132, "y", wear="front")),
    "horn_rear": (82, 54, -1, -3, -1, 2, 3, 2, gradient(RHINO_HORN, 0.0, 0.7, 3, 133, "y")),
    "ear": (90, 54, -0.5, -3, -1, 1, 3, 2, solid("dark", 134)),
    "leg_upper": (20, 54, -2.5, -1, -2.5, 5, 5, 5, leg(RHINO_PAL, -1, 4, 140)),
    "leg_lower": (40, 54, -2, 0, -2, 4, 4, 4, leg(RHINO_PAL, -1, 4, 141, hoof=True)),
}
RHINO_KEEP = ("horn1", "horn2", "horn3", "horn_rear")

# ---------------------------------------------------------------- steppe bison (128x64, BisonModel)

BISON_PAL = {
    "bull": pal(coat=(66, 42, 28), dark=(34, 24, 18), light=(98, 66, 42), tip=(150, 108, 62), belly=(44, 30, 22),
                cape=(132, 92, 52), beard=(22, 16, 13), hoof=(30, 26, 24), skin=(40, 34, 32)),
    "cow": pal(coat=(88, 58, 38), dark=(50, 34, 24), light=(120, 84, 54), tip=(164, 122, 76), belly=(60, 42, 30),
               cape=(146, 106, 64), beard=(36, 26, 20), hoof=(34, 30, 28), skin=(50, 42, 40)),
    "calf": pal(coat=(168, 96, 48), dark=(124, 70, 36), light=(196, 128, 72), tip=(216, 158, 98), belly=(148, 90, 50),
                cape=(190, 120, 62), beard=(118, 66, 34), hoof=(60, 50, 44), skin=(80, 60, 50)),
}
BISON_HORN = [(40, 36, 34), (64, 58, 54), (30, 27, 26)]


def cape(top, bottom, salt):
    """The bison's woolly forequarters: the golden-brown cape over hump and shoulders, darker below."""
    def mat(v, face, x, y, z, i, j):
        p = BISON_PAL[v]
        if face == "bottom":
            return hair(p["belly"], i, j, salt, 0.08)
        t = (y - top) / (bottom - top)
        curl = h(i // 2 + (j % 2), j // 2, salt + 1)  # tight curls
        if face == "top":
            c = lerp(p["cape"], p["tip"], 0.25 if curl < 0.3 else 0.0)
        elif face == "front":
            c = lerp(p["cape"], p["dark"], smooth(t / 0.7))  # dark chest under the cape
        else:
            behind = max(0.0, (z + 4) / 4)  # the cape thins out towards the flank
            c = lerp(p["cape"], p["coat"], min(1.0, smooth(t / 0.65) + behind * 0.5))
            if t > 0.65:
                c = lerp(c, p["dark"], (t - 0.65) / 0.35)
        if curl < 0.2:
            c = shade(c, 0.86)
        return hair(c, i, j, salt + 2, 0.08)
    return mat


def bison_skull(v, face, x, y, z, i, j):
    p = BISON_PAL[v]
    if face in ("left", "right"):
        if abs(y - 0.5) < 0.6 and abs(z + 4.5) < 0.6:
            return EYE
        if abs(y - 0.5) < 0.6 and abs(z + 3.5) < 0.6 and face == "right":
            return GLINT
    curl = h(i // 2 + (j % 2), j // 2, 151)
    if face in ("front", "top"):
        c = lerp(p["cape"], p["dark"], 0.35) if curl < 0.55 else lerp(p["cape"], p["tip"], 0.2)  # the woolly forelock
        if face == "front" and y > 1.5:
            c = lerp(c, p["dark"], 0.6)
        return hair(c, i, j, 152, 0.1)
    return hair(lerp(p["dark"], p["coat"], 0.3), i, j, 153, 0.1)


def bison_muzzle(v, face, x, y, z, i, j):
    p = BISON_PAL[v]
    if face == "front":
        if y > 2.5:
            return shade(p["skin"], 0.8)
        if abs(abs(x) - 1.2) < 0.5 and y < 0.5:
            return shade(p["skin"], 0.5)  # nostrils
        return fur(p["skin"], i, j, 154, 0.08)
    return hair(lerp(p["dark"], p["skin"], 0.3), i, j, 155, 0.08)


BISON = {
    "fore": (0, 0, -7, -8, -12, 14, 15, 11, cape(-8, 7, 160)),
    "hind": (50, 0, -5.5, -4, -2, 11, 10, 12, coat(BISON_PAL, -4, 6, 162, lock=0.07)),
    "hump": (0, 26, -5, -4, -4, 10, 5, 8, cape(-4, 1, 164)),
    "tail": (74, 26, -0.5, 0, -0.5, 1, 7, 1, solid("dark", 166)),
    "tuft": (120, 26, -1, 6, -1, 2, 3, 2, solid("beard", 167, 0.15)),
    "skull": (96, 0, -4.5, -4.5, -6, 9, 9, 7, bison_skull),
    "muzzle": (78, 26, -2.5, -1.5, -9, 5, 5, 3, bison_muzzle),
    "beard": (106, 26, -2, 0, -1.5, 4, 4, 3, solid("beard", 168, 0.14)),
    "horn": (0, 39, -1, -3, -1, 2, 3, 2, gradient(BISON_HORN, 0.0, 0.55, 3, 170)),
    "cow_horn": (8, 39, -0.5, -3, -0.5, 1, 3, 1, gradient(BISON_HORN, 0.0, 0.55, 3, 171)),
    "horn_tip": (12, 39, -0.5, -3, -0.5, 1, 3, 1, gradient(BISON_HORN, 0.55, 1.0, 3, 172)),
    "ear": (16, 39, -1.5, -0.5, -1, 3, 1, 2, solid("dark", 173)),
    "front_upper": (36, 26, -2.5, -1, -2.5, 5, 6, 5, cape(-1, 8, 174)),  # shaggy chaps
    "hind_upper": (56, 26, -2, -1, -2.5, 4, 6, 5, leg(BISON_PAL, -1, 8, 176)),
    "lower": (94, 26, -1.5, 0, -1.5, 3, 5, 3, leg(BISON_PAL, -3, 5, 178, hoof=True)),
}
BISON_KEEP = ("horn", "cow_horn", "horn_tip")

# ---------------------------------------------------------------- musk ox (128x64, MuskOxModel)

MUSK_PAL = {
    "adult": pal(coat=(44, 33, 26), dark=(24, 18, 15), light=(70, 54, 42), tip=(112, 90, 70), belly=(30, 23, 19),
                 saddle=(176, 156, 124), stocking=(206, 196, 176), hoof=(36, 32, 30), skin=(58, 52, 50), muzzle=(150, 140, 126)),
    "calf": pal(coat=(78, 58, 42), dark=(52, 38, 28), light=(110, 84, 62), tip=(140, 112, 86), belly=(60, 44, 32),
                saddle=(150, 124, 96), stocking=(196, 184, 162), hoof=(56, 48, 44), skin=(80, 70, 66), muzzle=(160, 148, 132)),
}
MUSK_HORN = [(176, 160, 128), (214, 200, 166), (226, 214, 184), (60, 52, 46)]


def musk_muzzle(v, face, x, y, z, i, j):
    p = MUSK_PAL[v]
    if face == "front":
        if y > 2:
            return shade(p["skin"], 0.8)
        if abs(abs(x) - 1) < 0.5 and y < 0.5:
            return shade(p["skin"], 0.5)
        return fur(p["muzzle"], i, j, 180, 0.08)
    return fur(lerp(p["muzzle"], p["coat"], 0.4 if face == "top" else 0.2), i, j, 181, 0.08)


MUSK_OX = {
    "body": (42, 0, -6, -5, -10, 12, 10, 20, coat(MUSK_PAL, -5, 5, 190, spine="saddle", lock=0.12)),
    "hump": (0, 31, -5, -8, -10, 10, 4, 8, coat(MUSK_PAL, -8, -4, 192, lock=0.12)),
    "skirt": (0, 0, -1, 0, -10, 1, 11, 20, skirt(MUSK_PAL, 11, 194)),
    "skirt_rear": (36, 31, -6, 0, 0, 12, 11, 1, skirt(MUSK_PAL, 11, 195)),
    "skirt_front": (86, 31, -5, 0, -1, 10, 9, 1, skirt(MUSK_PAL, 9, 196)),
    "skull": (62, 31, -3, -3, -6, 6, 6, 6, skull_with_eyes(MUSK_PAL, 0.5, -4.5, 197)),
    "muzzle": (0, 43, -2, -1, -9, 4, 4, 3, musk_muzzle),
    "chin": (108, 31, -2.5, 0, -2, 5, 4, 4, solid("dark", 198, 0.15)),
    "boss": (38, 43, -4, -4, -4, 8, 2, 4, gradient(MUSK_HORN, 0.0, 0.2, 2, 200)),
    "horn_a": (62, 43, -1, 0, -1, 2, 4, 2, gradient(MUSK_HORN, 0.2, 0.6, -4, 201)),
    "horn_b": (70, 43, -0.5, 0, -0.5, 1, 3, 1, gradient(MUSK_HORN, 0.6, 1.0, -3, 202)),
    "leg_upper": (14, 43, -1.5, -1, -1.5, 3, 4, 3, leg(MUSK_PAL, -1, 3, 204, stocking=0.0)),
    "leg_lower": (26, 43, -1.5, 0, -1.5, 3, 4, 3, leg(MUSK_PAL, -2, 4, 206, hoof=True, stocking=-1.0)),
}
MUSK_KEEP = ("boss", "horn_a", "horn_b")


def entity(boxes, palette, variant, size):
    PAL_OF.clear()
    PAL_OF.update(palette)
    return paint(boxes, variant, size)


def entities():
    ent = "wildlife/giants/"
    out = {}
    for name, boxes, palette, variants, size, keep in (
            ("woolly_mammoth", MAMMOTH, MAMMOTH_PAL, ("adult", "calf"), (128, 128), MAMMOTH_KEEP),
            ("woolly_rhino", RHINO, RHINO_PAL, ("adult", "calf"), (128, 64), RHINO_KEEP),
            ("steppe_bison", BISON, BISON_PAL, ("bull", "cow", "calf"), (128, 64), BISON_KEEP),
            ("musk_ox", MUSK_OX, MUSK_PAL, ("adult", "calf"), (128, 64), MUSK_KEEP)):
        for v in variants:
            img = entity(boxes, palette, v, size)
            file = name + ("" if v == "adult" else f"_{v}")
            save(img, ent + file + ".png")
            out[file] = img
            if v != "calf":  # calves never leave a carcass
                save(skinned(img, boxes, keep), "skinned/wildlife/giants/" + file + ".png")
    return out


# ---------------------------------------------------------------- 16x16 items

ITEM = TEX / "item"
JAR = Path(__file__).resolve().parent.parent / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"


def vanilla_item(name):
    import io
    import zipfile
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(f"assets/minecraft/textures/item/{name}.png"))).convert("RGBA")


def recolour(im, ramp):
    """Map the picture's colours, ranked by brightness, onto a dark->light ramp (keeps the shape and shading)."""
    im = im.copy()
    px = im.load()
    xy = [(x, y) for y in range(16) for x in range(16) if px[x, y][3]]
    cols = sorted({px[p] for p in xy}, key=lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2])
    mapping = {c: hexc(ramp[round(i * (len(ramp) - 1) / max(len(cols) - 1, 1))]) for i, c in enumerate(cols)}
    for p in xy:
        px[p] = mapping[px[p]]
    return im


MEATS = {
    # raw / cooked from a vanilla shape, recoloured: fatty mammoth (yellow fat), lean dark rhino, deep red bison, musk ox
    "mammoth": ("beef", ["#2a0606", "#4a0c0c", "#6a1414", "#8a1e1c", "#a62e28", "#c45a44", "#e2c48a", "#f2e2b4"],
                ["#1e1009", "#2e1a0e", "#422614", "#58341c", "#6e4426", "#8a5a34", "#c8a064", "#e4c88c"]),
    "rhino": ("porkchop", ["#200606", "#3a0b0b", "#521110", "#681816", "#7c201c", "#92302a", "#b65a4c", "#d8a090"],
              ["#1a0e08", "#2a170d", "#3a2112", "#4a2a18", "#5a341e", "#6c4028", "#906042", "#b88a64"]),
    "bison": ("mutton", ["#240404", "#3e0808", "#580e0e", "#701414", "#861c1a", "#9c2a24", "#c25248", "#e6b4a0"],
              ["#1c0f08", "#2c180d", "#3e2414", "#4e2e1a", "#603a20", "#74482a", "#9a6a44", "#c49a6c"]),
    "musk_ox": ("rabbit", ["#260707", "#400c0c", "#5a1312", "#701a18", "#86241f", "#9c3329", "#be5c4c", "#e0aa96"],
                ["#1c1009", "#2c190f", "#3c2415", "#4c2e1b", "#5e3a22", "#70472b", "#946848", "#bc9470"]),
}

IVORY_GRID = [
    "................",
    "....OO..........",
    "...OWLO.........",
    "...OLMO.........",
    "...OLMDO........",
    "....OLMDO.......",
    "....OLLMDO......",
    ".....OLLMDOO....",
    "......OLLMMDOO..",
    ".......OOLLMMDO.",
    ".........OOLLMDO",
    "...........OYYDO",
    "..........ORYYO.",
    "..........ORRO..",
    "...........OO...",
    "................",
]
IVORY_PAL = {"O": "#4a3c26", "W": "#fffbee", "L": "#f2e8cc", "M": "#dccca2", "D": "#b4a078", "Y": "#d6bc7e", "R": "#9a7a48"}

HORN_GRID = [
    "................",
    "..OO............",
    "..OPO...........",
    "..OPLO..........",
    "...OPLO.........",
    "...OPLMO........",
    "...OPLMMO.......",
    "....OPLMMO......",
    "....OPLMMDO.....",
    ".....OPLMMDO....",
    ".....OPLMMDDO...",
    "......OPLMMDDO..",
    "......ORRRRRRRO.",
    ".......OOOOOOOO.",
    "................",
    "................",
]
HORN_PAL = {"O": "#1c1612", "P": "#a89678", "L": "#7a6a54", "M": "#5c4e40", "D": "#40362e", "R": "#2e2620"}

TONGUE_GRID = [
    "................",
    "................",
    "................",
    "................",
    "...........OOO..",
    ".........OOPPLO.",
    ".......OOPPPPLO.",
    ".....OOPPMPPPLO.",
    "....OPPMPPPPPO..",
    "...OPPPPMPPOO...",
    "...ODPPPPOO.....",
    "...ODDDOO.......",
    "....OOO.........",
    "................",
    "................",
    "................",
]
TONGUE_PAL = {"O": "#4a2228", "P": "#b47478", "L": "#d49aa0", "M": "#8e545c", "D": "#6e3a42"}
TONGUE_COOKED = ["#2a140c", "#4a2a18", "#6a4026", "#8a5a36", "#a8784e"]

FAT_GRID = [
    "................",
    "................",
    "................",
    ".....OOOOO......",
    "...OOWWWLLOO....",
    "..OLWWLLLLLMO...",
    ".OLWLLLLLLLMMO..",
    ".OMLLLLLLMMMMO..",
    ".OYMMMMMMMMMDO..",
    "OYYYMYYYYYYDDO..",
    "OYYYYYYYYDYDDO..",
    "ODYYDYYYYYDDdO..",
    "ODDDDDDDDDDdOO..",
    ".OOOOOOOOOOOO...",
    "................",
    "................",
]
FAT_PAL = {"O": "#8a6a3a", "W": "#fff8de", "L": "#f8eabc", "M": "#efd896", "Y": "#e2c470", "D": "#c8a454", "d": "#a8863e"}

KNIFE_GRID = [
    "................",
    "............OO..",
    "...........OWO..",
    "..........OWLO..",
    ".........OWLO...",
    "........OWLDO...",
    ".......OWLDO....",
    "......OWLDO.....",
    ".....OYLDO......",
    "....OOYDO.......",
    "...OcCOO........",
    "..OCcCO.........",
    ".OcCcO..........",
    ".OCcO...........",
    "..OO............",
    "................",
]
KNIFE_PAL = {"O": "#3c3020", "W": "#fffbee", "L": "#ece0c0", "D": "#c4b088", "Y": "#a88e5c", "C": "#8a6a44", "c": "#6a4e30"}

CLUB_GRID = [
    "..........OO....",
    ".........OPO....",
    "........OPLO....",
    "........OPMO....",
    ".......OWWWWO...",
    "......OWwccwWO..",
    "......OwWccWwO..",
    ".......OwWWwO...",
    "......OwwO......",
    ".....OwWO.......",
    "....OwWO........",
    "...OwWO.........",
    "..OcCO..........",
    ".OCcO...........",
    ".OcO............",
    "..O.............",
]
CLUB_PAL = {"O": "#1e1610", "P": "#a89678", "L": "#7a6a54", "M": "#5c4e40", "W": "#8a6a44", "w": "#6a4e30",
            "c": "#c4a878", "C": "#9c8058"}

# spread hide seen from above: neck at the top, legs to the corners, tail at the bottom
HIDE_MASK = [
    "......xxxx......",
    ".....xxxxxx.....",
    ".xx.xxxxxxxx.xx.",
    "xxxxxxxxxxxxxxxx",
    ".xxxxxxxxxxxxxx.",
    "...xxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "...xxxxxxxxxx...",
    ".xxxxxxxxxxxxxx.",
    "xxxxxxxxxxxxxxxx",
    ".xx.xxxxxxxx.xx.",
    ".....xxxxxx.....",
    ".......xx.......",
]


def hide_fur(kind):
    """Fur colour at (x, y) of a spread hide, fur side up."""
    def c(x, y):
        strand = h(x, y // 2 + (x % 3), 300)
        if kind == "mammoth":
            base = lerp((60, 36, 24), (150, 100, 60), 0.3 + 0.6 * strand)
            return lerp(base, (186, 130, 76), 0.6) if h(x, y, 301) < 0.1 else base
        if kind == "rhino":
            return lerp((84, 58, 36), (160, 116, 76), 0.35 + 0.5 * strand)
        if kind == "bison":
            cape = max(0.0, 1 - y / 9)  # the woolly cape at the neck end
            base = lerp((40, 28, 20), (98, 66, 42), 0.3 + 0.5 * strand)
            return lerp(base, (150, 106, 60), cape * (0.6 + 0.4 * strand))
        # musk ox: near-black with the pale saddle down the middle
        base = lerp((26, 20, 16), (80, 62, 48), 0.3 + 0.5 * strand)
        if 6 <= x <= 9 and 4 <= y <= 9:
            base = lerp(base, (178, 158, 126), 0.7)
        return base
    return c


def blob(rows):
    return {(x, y) for y, r in enumerate(rows) for x, ch in enumerate(r) if ch != "."}


def edge_of(cells, x, y):
    return any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))


def spread_hide(kind, mask=HIDE_MASK, outline=(24, 16, 12)):
    im = Image.new("RGBA", (16, 16), CLEAR)
    cells = blob(mask)
    fur_at = hide_fur(kind)
    if kind == "mammoth":  # long hair: a ragged, shaggy rim
        cells = {(x, y) for x, y in cells if not (edge_of(cells, x, y) and h(x, y, 303) < 0.3)}
    for x, y in cells:
        c = fur_at(x, y)
        if edge_of(cells, x, y):
            c = outline if h(x, y, 302) < 0.65 else shade(c, 0.6)
        else:
            c = shade(c, 1.0 + 0.08 * (7.5 - x) / 7.5 + 0.06 * (7 - y) / 7)  # light from the top left
        im.putpixel((x, y), (*c, 255))
    return im


RAW_FLESH = [(132, 44, 42), (168, 70, 64), (194, 104, 92)]
FAT = (232, 214, 188)


def raw_hide_item(img, salt):
    """The cured hide picture made wet and dark, a raw red rim and the bottom right corner folded back (flesh side)."""
    out = Image.new("RGBA", (16, 16), CLEAR)
    px = img.load()
    cells = {(x, y) for x in range(16) for y in range(16) if px[x, y][3]}
    fold = 20
    for x, y in cells:
        c = px[x, y][:3]
        if x + y > fold:
            col = lerp(RAW_FLESH[0], RAW_FLESH[2], 0.35 + 0.5 * h(x, y, salt))
            if h(x // 2, y, salt + 1) < 0.3:
                col = lerp(col, FAT, 0.7)
            if x + y == fold + 1:
                col = FAT
        elif edge_of(cells, x, y) and h(x, y, salt + 2) < 0.45:
            col = lerp(RAW_FLESH[0], RAW_FLESH[2], h(x, y, salt + 3))
        else:
            col = shade(lerp(c, (60, 40, 34), 0.1), 0.85)
        out.putpixel((x, y), (*col, 255))
    return out


def fluff(base, light, dark, salt, cells):
    im = Image.new("RGBA", (16, 16), CLEAR)
    for x, y in cells:
        t = 0.5 - 0.03 * (x - 7.5) - 0.03 * (y - 7.5) + 0.35 * (h(x // 2 + y % 2, y // 2, salt) - 0.5)
        c = shade(dark, 0.75) if edge_of(cells, x, y) else lerp(dark, light, t)
        if not edge_of(cells, x, y) and h(x, y, salt + 1) < 0.12:
            c = lerp(c, base, 0.5)
        im.putpixel((x, y), (*c, 255))
    return im


QIVIUT_SHAPE = [
    "................",
    "................",
    "......xxxx......",
    "....xxxxxxxx....",
    "...xxxxxxxxxx...",
    "..xxxxxxxxxxxx..",
    "..xxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxxx.",
    ".xxxxxxxxxxxxx..",
    "..xxxxxxxxxxxx..",
    "...xxxxxxxxxx...",
    "....xxx..xxx....",
    "................",
    "................",
    "................",
]


def mammoth_wool():
    """A hank of long ginger-brown hair, tied at the top left and splaying out to the bottom right."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            a, b = x + y - 15, x - y  # a along the hank, b across it
            width = 1.5 + max(0, a + 6) * 0.16 + 0.6 * (h(a, 0, 311) - 0.5)
            if not -11 <= a <= 13 or abs(b) > width:
                continue
            if a > 9 and (b + a) % 2:
                continue  # loose ends
            t = 0.5 + 0.4 * (h(b + 20, a // 3, 310) - 0.5) - 0.1 * b / max(width, 1)
            c = lerp((70, 40, 24), (196, 136, 78), t)
            if -9 <= a <= -7:
                c = (58, 42, 30) if (a + b) % 2 else (96, 70, 46)  # a twist of cord round the end
            elif abs(b) > width - 1:
                c = shade(c, 0.7)
            im.putpixel((x, y), (*c, 255))
    return im


def spawn_eggs():
    eggs = {}
    I1, I2 = "#f4ead0", "#cbb88c"
    tusks = [(3, 12, I1), (2, 13, I1), (2, 14, I2), (12, 12, I1), (13, 13, I1), (13, 14, I2)]
    trunk = [(7, 10, "#3a2c24"), (8, 10, "#4a3a30"), (7, 11, "#3a2c24"), (8, 12, "#3a2c24"), (8, 13, "#2a201a")]
    eggs["woolly_mammoth_spawn_egg"] = egg(["#a86a40", "#8a5232", "#6c3e26", "#52301e", "#3a2216", "#26160e"],
                                            [(5, 4, "#c48a56"), (10, 5, "#c48a56"), (4, 8, "#4a2a18"), (11, 9, "#4a2a18")],
                                            [(x, y) for x, y, _ in tusks], tusks + trunk)
    H1, H2 = "#9a8a70", "#5c4e40"
    horn = [(7, 0, H1), (7, 1, H1), (8, 1, H2), (7, 2, H2)]
    eggs["woolly_rhino_spawn_egg"] = egg(["#b48a5a", "#9a7046", "#7e5836", "#62442a", "#463020", "#2e2016"],
                                          [(5, 6, "#c8a070"), (10, 7, "#c8a070"), (6, 11, "#5a3e28"), (10, 12, "#5a3e28"),
                                           (6, 3, "#4a4038"), (7, 3, "#4a4038"), (8, 3, "#4a4038")],
                                          [(x, y) for x, y, _ in horn], horn)
    K = "#1a1410"
    bhorns = [(2, 1, K), (13, 1, K), (3, 2, K), (12, 2, K)]
    cape = [(x, y, "#b88a50" if (x + y) % 3 else "#d0a466") for y in range(3, 7) for x in range(4, 12)]
    eggs["steppe_bison_spawn_egg"] = egg(["#6e4a30", "#583a26", "#46301f", "#362418", "#261a12", "#18100b"],
                                          [(7, 12, K), (8, 12, K), (7, 13, K)],
                                          [(x, y) for x, y, _ in bhorns], cape + bhorns)
    B1, B2 = "#e2d6b8", "#a8987a"
    boss = [(x, 2, B1) for x in range(4, 12)] + [(3, 3, B1), (12, 3, B1), (3, 4, B2), (12, 4, B2), (4, 5, B2), (11, 5, B2)]
    saddle = [(x, 7, "#b8a07c") for x in range(5, 11)] + [(x, 8, "#a08a68") for x in range(6, 10)]
    socks = [(x, 14, "#e0d8c6") for x in range(6, 10)] + [(4, 13, "#d0c8b4"), (11, 13, "#d0c8b4")]
    eggs["musk_ox_spawn_egg"] = egg(["#4a3a2e", "#3a2c22", "#2c2018", "#201712", "#16100c", "#0e0a08"],
                                     [], [], boss + saddle + socks)
    return eggs


def items():
    out = {}
    cooked_of = {"beef": "cooked_beef", "porkchop": "cooked_porkchop", "mutton": "cooked_mutton", "rabbit": "cooked_rabbit"}
    for animal, (shape, raw, cooked) in MEATS.items():
        out[f"raw_{animal}"] = recolour(vanilla_item(shape), raw)
        out[f"cooked_{animal}"] = recolour(vanilla_item(cooked_of[shape]), cooked)
    out["bison_tongue"] = from_grid(TONGUE_GRID, TONGUE_PAL)
    out["cooked_bison_tongue"] = recolour(out["bison_tongue"], TONGUE_COOKED)
    out["mammoth_ivory"] = from_grid(IVORY_GRID, IVORY_PAL)
    out["rhino_horn"] = from_grid(HORN_GRID, HORN_PAL)
    out["mammoth_fat"] = from_grid(FAT_GRID, FAT_PAL)
    out["mammoth_wool"] = mammoth_wool()
    out["qiviut"] = fluff((150, 130, 108), (188, 170, 146), (96, 80, 66), 320, blob(QIVIUT_SHAPE))
    out["rhino_horn_club"] = from_grid(CLUB_GRID, CLUB_PAL)
    for kind, salt in (("mammoth", 400), ("rhino", 410), ("bison", 420), ("musk_ox", 430)):
        cured = spread_hide(kind)
        out[f"{kind}_hide"] = cured
        out[f"raw_{kind}_hide"] = raw_hide_item(cured, salt)
    for kind in ("mammoth", "bison", "musk_ox"):
        out[f"{kind}_rug"] = spread_hide(kind)
    out.update(spawn_eggs())
    for name, im in out.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
    print(f"wrote {len(out)} item textures")
    return out


# ---------------------------------------------------------------- blocks: pegged hides, rugs, figurine

BLOCK = TEX / "block"
# a hide pegged out flat, flesh side up, stretched to the four corner pegs
PEG_MASK = {
    "mammoth": [
        ".xxx........xxx.",
        ".xxxxxxxxxxxxxx.",
        ".xxxxxxxxxxxxxx.",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxxx.",
        ".xxxxxxxxxxxxxx.",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        "..xxxxxxxxxxxx..",
        ".xxxxxxxxxxxxxx.",
        ".xxxxxxxxxxxxxx.",
        ".xxx........xxx.",
    ],
    "small": [
        ".xx..........xx.",
        ".xxx..xxxx..xxx.",
        "..xxxxxxxxxxxx..",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "...xxxxxxxxxx...",
        "..xxxxxxxxxxxx..",
        ".xxx..xxxx..xxx.",
        ".xx....xx....xx.",
        ".......xx.......",
    ],
}
RAWHIDE = [(150, 116, 74), (186, 150, 104), (214, 186, 140)]


def pegged(kind, cured, salt):
    cells = blob(PEG_MASK["mammoth" if kind == "mammoth" else "small"])
    fur_at = hide_fur(kind)
    im = Image.new("RGBA", (16, 16), CLEAR)
    for x, y in cells:
        if edge_of(cells, x, y) and h(x, y, salt) < 0.7:
            c = shade(fur_at(x, y), 0.85)  # the fur shows round the rim (fur side down)
        elif cured:
            k = 0.45 + 0.35 * (h(x // 2, y, salt + 1) - 0.5) + 0.1 * smooth(1 - abs(x - 7.5) / 8)
            c = lerp(RAWHIDE[0], RAWHIDE[2], k)
            if h(x, y // 2, salt + 2) < 0.08:
                c = shade(c, 0.85)  # dried veins
        else:
            c = lerp(RAW_FLESH[0], RAW_FLESH[2], 0.3 + 0.55 * h(x // 2, y, salt + 3))
            if h(x // 3, y, salt + 4) < 0.22:
                c = lerp(c, FAT, 0.65)  # fat and membrane still on it
        im.putpixel((x, y), (*c, 255))
    return im


def figurine():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y in range(16):
        for x in range(16):
            t = 0.55 + 0.3 * (h(x, y // 2, 500) - 0.5) - 0.012 * (x + y)
            c = lerp((196, 176, 130), (248, 240, 218), t)
            if (x * 3 + y * 5) % 11 == 0 and h(x, y, 501) < 0.6:
                c = shade(c, 0.8)  # carved hatching
            im.putpixel((x, y), (*c, 255))
    return im


def blocks(item_pics):
    out = {}
    for kind, salt in (("mammoth", 600), ("rhino", 610), ("bison", 620), ("musk_ox", 630)):
        out[f"raw_{kind}_hide"] = pegged(kind, False, salt)
        out[f"raw_{kind}_hide_cured"] = pegged(kind, True, salt)
    for kind in ("mammoth", "bison", "musk_ox"):
        out[f"{kind}_rug"] = item_pics[f"{kind}_rug"]
    out["ivory_figurine"] = figurine()
    for name, im in out.items():
        im.save(BLOCK / f"{name}.png")
    print(f"wrote {len(out)} block textures")


def main():
    entities()
    blocks(items())


if __name__ == "__main__":
    main()
