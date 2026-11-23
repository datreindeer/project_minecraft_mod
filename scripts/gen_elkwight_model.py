"""The Elkwight's model and skin from one table (docs/elkwight-plan.md, com.mechanical.wildlife.elkwight):

- src/main/java/com/mechanical/client/wildlife/ElkwightGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/elkwight.png (256x256), the skinned carcass skin and elkwight_glow.png (eyes, runes)

A hulking giant: digitigrade legs (furred thighs, sinewy shins bent back, cloven hooves with dewclaws), a deep chest and
belly under a shaggy fur mantle with a hump behind the neck, a hide belt with a bone skull buckle and hanging bones, a
loincloth, long heavy arms (bone pauldron on the right, hide bracers with runes), big hands. A moose's head: the long
Roman-nosed muzzle with its overhanging lip, the dewlap bell, wide ears, a heavy brow over glowing eyes. Two palmate
antlers, each a beam, a three-plate cupped palm and a fringe of tines, with a brow palm forward. The double-bitted axe:
long wrapped haft with a pommel, crescent bits with beards and green runes, a spike on top.

Usage: python scripts/gen_elkwight_model.py (called by gen_elkwight_boss_textures.py too). Deterministic.
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/mechanical/textures"
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/ElkwightGeometry.java"
TEX = 256


def h(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    v = (v ^ (v >> 13)) * 1274126177 & 0xFFFFFFFF
    return (v & 0xFFFF) / 0xFFFF


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


# ---- the parts: name -> (parent, (x, y, z), (rx, ry, rz)); boxes: (part, material, x, y, z, w, h, d, mirror)
PARTS = {}
BOXES = []


def part(name, parent, at=(0, 0, 0), rot=(0, 0, 0)):
    PARTS[name] = (parent, at, rot)


def box(p, mat, x, y, z, w, hh, d, mirror=False):
    BOXES.append([p, mat, x, y, z, w, hh, d, mirror])


# body: pivot at the hips, the torso leaning a little forward
part("body", "root", (0, 8, 0))
box("body", "belly", -5, -6, -3, 10, 6, 6)
box("body", "chest", -6, -13, -3.5, 12, 7, 7)
box("body", "pecs", -5, -12.5, -4.5, 10, 4, 1)
box("body", "hump", -5, -16, -1, 10, 4, 5)
box("body", "mantle", -7, -14.5, -4.5, 14, 4, 9)
box("body", "fringe", -7, -10.5, -4.5, 3, 3, 1)      # fur hanging from the mantle at the front
box("body", "fringe", 4, -10.5, -4.5, 3, 3, 1)
box("body", "fringe", -6, -10.5, 3.5, 12, 4, 1)       # and down the back
box("body", "belt", -5.5, -3, -3.5, 11, 2, 7)
box("body", "buckle", -1.5, -3.5, -4.5, 3, 3, 1)      # a small skull of bone
box("body", "cloth", -3, -1, -3.8, 6, 8, 1)           # the loincloth, front and back
box("body", "cloth", -3.5, -1, 2.8, 7, 7, 1)
box("body", "bones", 5, -1, -2, 1, 4, 1)              # bones hung from the belt
box("body", "bones", 5.2, -1, 0.5, 1, 3, 1)
box("body", "pouch", -6.5, -2, -1, 2, 3, 3)

# head on its thick neck, the muzzle long and drooping
part("head", "body", (0, -14, -2.5))
box("head", "neck", -3, -2, -3.5, 6, 5, 6)
box("head", "skull", -3, -8, -4, 6, 6, 6)
box("head", "brow", -3.5, -8, -4.8, 7, 2, 2)
box("head", "throat", -2, 1, -4.5, 4, 2, 3)
part("muzzle", "head", (0, -6, -4), (0.5, 0, 0))
box("muzzle", "snout", -2, -1, -8, 4, 4, 8)
box("muzzle", "lip", -2.5, -0.5, -10.5, 5, 4, 3)      # the moose's heavy overhanging upper lip
box("muzzle", "jaw", -1.5, 3, -8, 3, 1, 6)
part("dewlap", "head", (0, 2.5, -2.5), (-0.15, 0, 0))
box("dewlap", "bell", -0.5, 0, -1, 1, 5, 2)
box("dewlap", "tuft", -1, 4, -1.5, 2, 2, 3)
for side, name in ((-1, "right_ear"), (1, "left_ear")):
    part(name, "head", (side * 3, -7.5, 0), (0.15, side * -0.3, side * 0.55))
    box(name, "ear", 0 if side > 0 else -4, -1, -1, 4, 2, 2, side > 0)


def antler(side):
    """A palmate antler: burr, beam out of the skull, a cupped palm of three plates with tines round its rim."""
    s = "left" if side > 0 else "right"
    m = side > 0

    def bx(p, mat, x, y, z, w, hh, d):  # boxes given for the left side, mirrored across x for the right
        box(p, mat, x if m else -x - w, y, z, w, hh, d, m)

    a = s + "_antler"
    part(a, "head", (side * 2.5, -7.5, -1), (-0.1, side * 0.15, side * -0.05))
    bx(a, "burr", -0.5, -1.5, -1.5, 3, 3, 3)
    bx(a, "beam", 2, -1, -1, 4, 2, 2)
    brow = s + "_brow_palm"  # the small brow palm reaching forward
    part(brow, a, (side * 3, -0.5, -1), (0.9, side * 0.35, 0))
    bx(brow, "palm_base", 0, -2, -4, 1, 2, 4)
    bx(brow, "tine", 0, -3, -4, 1, 1, 1)
    bx(brow, "tine", 0, -2, -6, 1, 1, 2)
    bx(brow, "tine", 0, -0.5, -5.5, 1, 1, 2)
    p1 = s + "_palm1"
    part(p1, a, (side * 5.5, 0, 0), (-0.7, side * 0.25, side * -0.12))
    bx(p1, "palm_base", 0, -6, -0.5, 6, 7, 1)
    p2 = s + "_palm2"
    part(p2, p1, (side * 5.5, -0.5, 0), (0.1, side * -0.3, side * 0.15))
    bx(p2, "palm", 0, -7, -0.5, 6, 8, 1)
    p3 = s + "_palm3"
    part(p3, p2, (side * 5.5, -0.5, 0), (0.1, side * -0.35, side * 0.15))
    bx(p3, "palm_rim", 0, -6, -0.5, 4, 7, 1)
    # tines: along the top of each plate, and out from the far rim
    for p, xs, top in ((p1, (2, 4), -6), (p2, (0, 1.7, 3.4, 5), -7), (p3, (0, 1.6, 3), -6)):
        for k, x in enumerate(xs):
            ln = 1.5 + 0.8 * ((k * 7 + len(p)) % 3) + (0.6 if p == p3 else 0)
            bx(p, "tine", x, top - ln, -0.5, 1, ln, 1)
    for k, y in enumerate((-5.5, -3.8, -2, -0.3)):
        bx(p3, "tine", 4, y, -0.5, 1.5 + 0.8 * (k % 3), 1, 1)


antler(-1)
antler(1)

# arms: massive upper arms, forearms bent a little, big hands; the right shoulder under a bone pauldron
for side, name in ((-1, "right_arm"), (1, "left_arm")):
    m = side > 0
    part(name, "body", (side * 7, -12, 0))
    box(name, "shoulder_bone" if side < 0 else "shoulder_fur", -3.5 if side < 0 else -2.5, -3, -3, 6, 4, 6, m)
    box(name, "arm", -2.5, -1, -2.5, 5, 8, 5, m)
    fa = name.replace("arm", "forearm")
    part(fa, name, (0, 7, 0), (-0.2, 0, 0))
    box(fa, "forearm", -2, 0, -2, 4, 7, 4, m)
    box(fa, "bracer", -2.5, 1.5, -2.5, 5, 4, 5, m)
    box(fa, "hand", -2, 7, -2, 4, 3, 4, m)

# the axe in the right hand, the haft along the hand's -z
part("axe", "right_forearm", (0, 8.5, 0))
box("axe", "haft", -1, -1, -26, 2, 2, 30)
box("axe", "pommel", -1.5, -1.5, 4, 3, 3, 2)
box("axe", "socket", -1.5, -2.5, -28, 3, 5, 5)
box("axe", "spike", -0.5, -0.5, -32, 1, 1, 4)
for sgn in (-1, 1):  # the two crescent bits, up and down, flaring wider towards the edge
    def bit(y, hh, z, d, mat):
        box("axe", mat, -0.5, y if sgn < 0 else -y - hh, z, 1, hh, d)
    bit(-5.5, 3, -27.5, 4, "bit_neck")
    bit(-8.5, 3, -29, 7, "bit")
    bit(-11.5, 3, -30.5, 10, "bit_edge")
    bit(-12.5, 1, -31, 3, "beard")
    bit(-12.5, 1, -23.5, 3, "beard")

# legs: furred thighs, shins bent back, a short cannon and cloven hooves
for side, name in ((-1, "right_leg"), (1, "left_leg")):
    m = side > 0
    part(name, "root", (side * 3, 8, 0))
    th = name.replace("leg", "thigh")
    part(th, name, (0, 0, 0), (-0.3, 0, 0))
    box(th, "thigh", -3, -1, -3, 6, 7, 6, m)
    sh = name.replace("leg", "shin")
    part(sh, th, (0, 6, 0), (0.6, 0, 0))
    box(sh, "shin", -2, 0, -2, 4, 6, 4, m)
    box(sh, "shin_fur", -2.5, 0, 0.5, 5, 4, 2, m)   # the shag behind the knee
    ca = name.replace("leg", "cannon")
    part(ca, sh, (0, 5.7, 0), (-0.3, 0, 0))
    box(ca, "cannon", -1.5, 0, -1.5, 3, 2.5, 3, m)
    box(ca, "dewclaw", -1.5, 1.8, 1, 3, 1, 1, m)
    box(ca, "hoof", -2, 2.3, -2.5, 2, 2, 4, m)
    box(ca, "hoof", 0, 2.3, -2.5, 2, 2, 4, m)


# ---- texture layout: shelf-pack the boxes' UV rectangles (2d+2w) x (d+h), integer sizes
def size(b):
    w, hh, d = (max(1, math.ceil(v - 1e-6)) for v in b[5:8])
    return w, hh, d


def pack():
    order = sorted(range(len(BOXES)), key=lambda i: -(size(BOXES[i])[2] + size(BOXES[i])[1]))
    x = y = shelf = 0
    for i in order:
        w, hh, d = size(BOXES[i])
        bw, bh = 2 * d + 2 * w, d + hh
        if x + bw > TEX:
            x, y, shelf = 0, y + shelf, 0
        BOXES[i].append((x, y))
        x += bw
        shelf = max(shelf, bh)
    assert y + shelf <= TEX, "texture full"


def faces(u, v, w, hh, d):
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
            "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh)}


# ---- palette
P = dict(fur=(96, 70, 48), fur_dark=(50, 37, 28), fur_tip=(142, 112, 82), chest=(132, 104, 78), skin=(118, 102, 88),
         hoof=(26, 22, 20), hoof_shine=(64, 58, 52), leather=(140, 100, 62), leather_dark=(78, 52, 32), cloth=(110, 92, 62),
         cloth_dark=(52, 42, 30), mantle=(124, 116, 104), mantle_dark=(66, 60, 54), mantle_tip=(184, 176, 160),
         head=(116, 84, 56), head_dark=(66, 46, 32), muzzle=(84, 60, 42), nose=(24, 20, 18), lip=(100, 72, 50),
         ear_in=(140, 108, 86), bone=(214, 202, 172), bone_dark=(150, 132, 100), bone_tip=(240, 234, 214),
         bone_base=(112, 92, 66), wood=(66, 46, 30), wood_dark=(40, 28, 18), wrap=(34, 26, 20), wrap_light=(70, 54, 38),
         iron=(66, 72, 78), iron_dark=(40, 44, 50), edge=(180, 188, 196), groove=(30, 60, 44))
S = dict(P, fur=(146, 52, 46), fur_dark=(104, 34, 32), fur_tip=(176, 90, 80), chest=(168, 74, 64), skin=(150, 56, 48),
         mantle=(190, 150, 130), mantle_dark=(140, 90, 80), mantle_tip=(214, 186, 170), head=(150, 60, 52),
         head_dark=(110, 40, 36), muzzle=(130, 50, 44), lip=(140, 56, 48), leather=(176, 84, 70), leather_dark=(120, 40, 36),
         cloth=(170, 120, 100), cloth_dark=(130, 80, 70))
GLOW, EYE = (120, 255, 150), (210, 255, 220)


def fur(P, face, i, j, fw, fh, salt, base="fur", dark="fur_dark", tip="fur_tip", hang=True):
    """Shaggy hair: strands down each column, dark roots at the top, lighter tips, a ragged lower edge."""
    col = h(i, 0, salt)
    if face in ("top", "bottom"):
        return lerp(P[base], P[dark], 0.3 + 0.5 * h(i, j, salt))
    t = j / max(1, fh - 1)
    c = lerp(P[dark], P[base], 0.35 + 0.5 * col + 0.3 * t)
    if (j + int(col * 4)) % 4 == 0:
        c = lerp(c, P[tip], 0.35)
    if hang and j == fh - 1 and h(i, j, salt + 3) < 0.45:
        c = lerp(c, P[dark], 0.6)
    return c


def colour(P, mat, face, i, j, fw, fh, salt):
    n = h(i, j, salt)
    side = face in ("left", "right", "front", "back")
    if mat in ("belly", "chest", "pecs"):
        if face == "front":
            mid = 1 - abs((i + 0.5) / fw - 0.5) * 2
            c = lerp(P["fur"], P["chest"], 0.25 + 0.55 * mid)
            if mat == "pecs" and j == fh - 1:
                c = lerp(c, P["fur_dark"], 0.5)  # the shadow under the chest
            if mat == "belly" and i == fw // 2 and j < fh - 1:
                c = lerp(c, P["fur_dark"], 0.35)
            return lerp(c, P["fur_dark"], 0.2 * n)
        return fur(P, face, i, j, fw, fh, salt)
    if mat in ("hump", "neck", "thigh", "shin_fur", "shoulder_fur", "arm", "throat"):
        return fur(P, face, i, j, fw, fh, salt, hang=mat != "arm")
    if mat in ("mantle", "fringe"):
        return fur(P, face, i, j, fw, fh, salt, "mantle", "mantle_dark", "mantle_tip")
    if mat in ("shin", "forearm", "cannon"):
        sinew = 0.25 if side and i % 3 == 1 else 0
        return lerp(lerp(P["fur"], P["skin"], 0.4), P["fur_dark"], 0.3 * n + sinew)
    if mat == "hand" and face == "bottom":  # the fist's knuckles and fingers, seen when the arm points ahead
        if j == 0:
            return lerp(P["skin"], P["fur_tip"], 0.3)  # the knuckles
        return lerp(P["skin"], P["fur_dark"], 0.12 + 0.22 * (i % 2) + 0.1 * j / fh)  # four fingers curled under
    if mat == "hand":
        if face == "front" and j >= 1 and i % 1 == 0 and i in (0, fw - 1) and j == fh - 1:
            return P["skin"]
        if side and j == fh - 1:
            return P["hoof"] if i % 2 == 0 else lerp(P["skin"], P["fur_dark"], 0.5)  # dark claws on the fingers
        if face == "front" and j >= 1:
            return lerp(P["skin"], P["fur_dark"], 0.35 if i % 2 else 0.15)  # the knuckled fingers
        return lerp(P["skin"], P["fur_dark"], 0.3 * n)
    if mat in ("hoof",):
        return P["hoof_shine"] if face in ("front",) and j == 0 and n > 0.5 else lerp(P["hoof"], P["hoof_shine"], 0.25 * n)
    if mat == "dewclaw":
        return P["hoof"]
    if mat in ("belt", "pouch", "bracer"):
        if side and j in (0, fh - 1):
            return P["leather_dark"]
        if mat == "bracer" and side and (i + j) % 3 == 0:
            return lerp(P["leather"], P["leather_dark"], 0.6)  # cross-lacing
        return lerp(P["leather"], P["leather_dark"], 0.35 * n)
    if mat == "cloth":
        c = lerp(P["cloth"], P["cloth_dark"], 0.3 * n + (0.35 if i in (0, fw - 1) else 0))
        if j == fh - 1 and h(i, 9, salt) < 0.5:
            c = lerp(c, P["cloth_dark"], 0.7)  # frayed hem
        return c
    if mat in ("buckle", "bones", "shoulder_bone"):
        if mat == "buckle" and face == "front":
            if j == 1 and i in (0, 2):
                return P["nose"]  # eye sockets
            if j == 2 and i == 1:
                return P["bone_dark"]
        if mat == "shoulder_bone" and face in ("top", "front", "back", "left", "right") and (i + 2 * j) % 5 == 0:
            return P["bone_dark"]  # the ridges of the bone plate
        return lerp(P["bone"], P["bone_dark"], 0.35 * n + (0.2 if j == fh - 1 and side else 0))
    if mat in ("skull", "brow"):
        c = lerp(P["head"], P["head_dark"], 0.35 * n + (0.3 if face == "top" else 0))
        if mat == "brow" and face == "bottom":
            c = lerp(c, P["head_dark"], 0.6)
        if mat == "skull" and face in ("left", "right") and j in (2, 3):
            return lerp(P["head_dark"], P["nose"], 0.5)  # the deep eye socket (the eye glows over it)
        return c
    if mat in ("snout", "jaw"):
        t = 0.3 if face == "top" else 0
        return lerp(P["muzzle"], P["head"], 0.4 * n + t)
    if mat == "lip":
        if face == "front":
            if j == 1 and i in (0, 4):
                return lerp(P["nose"], P["muzzle"], 0.3)  # the nostrils, wide apart at the sides of the nose
            if j == fh - 1:
                return lerp(P["nose"], P["lip"], 0.35)  # the mouth line under the overhanging lip
            return lerp(lerp(P["lip"], P["fur_tip"], 0.25), P["muzzle"], 0.3 * n + 0.1 * j)
        if face == "bottom":
            return P["nose"]
        return lerp(P["lip"], P["muzzle"], 0.3 * n)
    if mat in ("bell", "tuft"):
        return fur(P, face, i, j, fw, fh, salt, "head", "head_dark", "fur_tip")
    if mat == "ear":
        return P["ear_in"] if face == "bottom" else lerp(P["head"], P["head_dark"], 0.4 * n)
    if mat in ("burr", "beam", "palm_base", "palm", "palm_rim", "tine"):
        base = {"burr": 0.0, "beam": 0.25, "palm_base": 0.45, "palm": 0.7, "palm_rim": 0.85, "tine": 0.9}[mat]
        if mat == "burr":
            return lerp(P["bone_base"], P["bone_dark"], n)
        if mat == "tine":
            t = 1 - j / max(1, fh - 1) if face != "top" else 1.0
            return lerp(P["bone_dark"], P["bone_tip"], 0.4 + 0.6 * t)
        # palm: ridged with veins running out from the beam, darker near it
        t = base + 0.15 * (i / max(1, fw - 1))
        c = lerp(P["bone_base"], P["bone"], t)
        if (i * 2 + j) % 6 == 0:
            c = lerp(c, P["bone_dark"], 0.45)
        return lerp(c, P["bone_dark"], 0.18 * n)
    if mat == "haft":
        along = j if face in ("top", "bottom") else i
        band = along % 6
        if along < 12 and band < 4:  # the leather-wrapped grip
            return P["wrap_light"] if band == 0 else P["wrap"]
        return lerp(P["wood"], P["wood_dark"], 0.4 * n + (0.3 if (along + int(n * 3)) % 5 == 0 else 0))
    if mat in ("pommel", "socket", "spike"):
        return lerp(P["iron"], P["iron_dark"], 0.45 * n) if not (mat == "spike" and face == "front") else P["edge"]
    if mat in ("bit_neck", "bit", "bit_edge", "beard"):
        if face in ("left", "right"):
            if mat in ("bit_edge", "beard") and (j == (0 if mat == "beard" else 0) and False):
                pass
            far = (i == 0) if face == "right" else (i == fw - 1)
            if mat == "bit_edge" and (far or n > 0.92):
                return P["edge"]
            if mat == "bit" and rune_at(i, j, fw, fh, face):
                return P["groove"]
            return lerp(P["iron"], P["iron_dark"], 0.35 * n + (0.25 if mat == "bit_neck" else 0))
        if face in ("front",) or (mat == "bit_edge" and face in ("top", "bottom")):
            return P["edge"]
        return lerp(P["iron"], P["iron_dark"], 0.4 * n)
    return P["fur"]


def rune_at(i, j, fw, fh, face):
    """A bind-rune on each bit's flat face: a stave with branches."""
    x = i if face == "right" else fw - 1 - i
    mid = fw // 2
    return (x == mid and 0 <= j <= 2) or (x == mid - 1 and j == 1) or (x == mid + 1 and j == 0) or (x == mid + 2 and j == 2)


def paint(P, glow=False):
    im = Image.new("RGBA", (TEX, TEX), (0, 0, 0, 0))
    px = im.load()
    for k, b in enumerate(BOXES):
        mat = b[1]
        w, hh, d = size(b)
        u, v = b[9]
        for face, (x0, y0, fw, fh) in faces(u, v, w, hh, d).items():
            for j in range(fh):
                for i in range(fw):
                    if glow:
                        c = glow_at(mat, face, i, j, fw, fh)
                        if c:
                            px[x0 + i, y0 + j] = (*c, 255)
                        continue
                    c = colour(P, mat, face, i, j, fw, fh, 17 + k)
                    # light from above: tops brighter, the lower part of each side a touch darker
                    f = 1.08 if face == "top" else 0.82 if face == "bottom" else 1.0 - 0.1 * (j / max(1, fh - 1))
                    f *= 0.94 + 0.12 * h(x0 + i, y0 + j, 5)
                    px[x0 + i, y0 + j] = (*shade(c, f), 255)
    return im


def glow_at(mat, face, i, j, fw, fh):
    if mat == "skull" and face in ("left", "right") and j == 2:
        front = i == fw - 1 if face == "right" else i == 0  # the eye sits near the front of the skull side
        near = i == fw - 2 if face == "right" else i == 1
        return EYE if front else GLOW if near else None
    if mat == "skull" and face == "front" and j == 2 and i in (0, fw - 1):
        return EYE
    if mat == "bit" and face in ("left", "right") and rune_at(i, j, fw, fh, face):
        return GLOW
    if mat == "shoulder_bone" and face == "front" and 1 <= j <= 3 and i == fw // 2:
        return GLOW  # a rune stave cut into the pauldron
    if mat == "bracer" and face == "front" and j == 1 and i in (1, 3):
        return lerp(GLOW, (40, 90, 60), 0.4)
    return None


def java():
    pack()
    lines = []
    order = ["root"]
    while len(order) < len(PARTS) + 1:
        for n, (parent, _, _) in PARTS.items():
            if n not in order and parent in order:
                order.append(n)
    f = lambda v: f"{v:.4g}F".replace("e", "E") if "." in f"{v:.4g}" or "e" in f"{v:.4g}" else f"{v:.4g}.0F"
    for n in order[1:]:
        parent, at, rot = PARTS[n]
        bs = [b for b in BOXES if b[0] == n]
        cl = "CubeListBuilder.create()"
        for b in bs:
            cl += (f"\n                .texOffs({b[9][0]}, {b[9][1]})" + (".mirror()" if b[8] else ".mirror(false)")
                   + f".addBox({f(b[2])}, {f(b[3])}, {f(b[4])}, {f(b[5])}, {f(b[6])}, {f(b[7])})")
        pose = f"PartPose.offsetAndRotation({f(at[0])}, {f(at[1])}, {f(at[2])}, {f(rot[0])}, {f(rot[1])}, {f(rot[2])})"
        lines.append(f"        PartDefinition {n} = {parent}.addOrReplaceChild(\"{n}\", {cl},\n                {pose});")
    src = f"""package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_elkwight_model.py (with the skin it is painted for): the Elkwight's parts and boxes. Do not edit. */
final class ElkwightGeometry {{
    static LayerDefinition create() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(lines)}
        return LayerDefinition.create(mesh, {TEX}, {TEX});
    }}

    private ElkwightGeometry() {{}}
}}
"""
    JAVA.write_text(src, encoding="utf-8")


def main():
    java()
    (RES / "entity/wildlife").mkdir(parents=True, exist_ok=True)
    (RES / "entity/skinned/wildlife").mkdir(parents=True, exist_ok=True)
    paint(P).save(RES / "entity/wildlife/elkwight.png")
    paint(S).save(RES / "entity/skinned/wildlife/elkwight.png")
    paint(P, glow=True).save(RES / "entity/wildlife/elkwight_glow.png")
    print(f"elkwight model: {len(PARTS)} parts, {len(BOXES)} boxes")


if __name__ == "__main__":
    main()
