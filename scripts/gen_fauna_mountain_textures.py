"""Paint the mountain fauna textures (wildlife/mountain/*: Ibex, Chamois, Saiga, Marmot, Cave Hyena, Cave Bear).

Usage: python scripts/gen_fauna_mountain_textures.py
Deterministic. Writes:
- entity textures (64x64, marmot 64x32), painted texel by texel from the 3D point each covers (wildlife_paint.py), so
  markings line up across faces: ibex_{buck,nanny,kid}, chamois{,_kid}, saiga_{buck,doe,calf}, marmot{,_pup},
  cave_hyena; the cave bear (128x64) and its cub repaint the brown bear's hand-made textures darker and colder (same UV
  layout; the cave bear's domed brow box is filled in), and the skinned (flesh) versions of all adults for carcasses.
- items: meats (recoloured from the mod's hand-drawn scraps, each from the cut that suits the animal), pelts and rugs
  (recoloured from the wolf / bear pelts, with each animal's markings), horns, fang, fat, salve, Horn Bow, Chamois
  Boots and Leather, spawn eggs; blocks: the rugs, the cave bear skull, the cave bear pelt on the tall Drying Rack;
  the Chamois Leather armour layer.
The IBEX / CHAMOIS / SAIGA / MARMOT / HYENA tables mirror the cubes in wildlife/mountain/client/*Model.java.
Look: ibex buck dark grey-brown with a darker chest, flanks and leg fronts, pale belly, dark beard and great knobbed
grey-brown horns; nanny and kid paler golden-grey. Chamois: warm tawny-brown, a dark stripe down the back, black lower
legs, white face with a black band from the horn through the eye to the muzzle, white throat, black hooked horns.
Saiga: cinnamon-sand above, white below, the big nose the same colour with dark nostrils, waxy amber ringed horns.
Marmot: grizzled grey-brown back, sandy-rufous belly, dark crown, pale muzzle, dark-tipped tail. Cave hyena: ochre-grey
with dark spots, dark muzzle, dark crest and tail tuft, round ears with dark backs. Cave bear: dark umber, grizzled hump.
"""
import sys
import zipfile
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import TEX, fur, h, lerp, paint, save, shade, skinned, smooth, _skinner  # noqa: E402
from gen_wildlife_item_textures import egg  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
ITEM = ROOT / "src/main/resources/assets/mechanical/textures/item"
BLOCK = ROOT / "src/main/resources/assets/mechanical/textures/block"
JAR = next(iter(sorted((ROOT / "build/moddev/artifacts").glob("minecraft-patched-*-merged.jar"))
                + sorted((ROOT.parent / "project_minecraft_mod/build/moddev/artifacts").glob("minecraft-patched-*-merged.jar"))), None)

EYE, GLINT = (16, 12, 10), (200, 204, 210)
HOOF = (38, 34, 32)
INNER_EAR = (196, 168, 150)
TRANSPARENT = (0, 0, 0, 0)

# ---------------------------------------------------------------- shared ungulate parts (palette p per variant)

PAL = {
    # ibex
    "ibex_buck": dict(back=(84, 72, 60), coat=(112, 98, 82), light=(150, 136, 114), belly=(196, 184, 162), dark=(52, 44, 38),
                      face=(96, 84, 72), muzzle=(150, 138, 120), stripe=None, horn=((86, 76, 64), (128, 116, 98), (160, 150, 130))),
    "ibex_nanny": dict(back=(124, 102, 72), coat=(152, 128, 92), light=(180, 158, 120), belly=(214, 200, 172), dark=(98, 80, 60),
                       face=(138, 116, 88), muzzle=(186, 170, 144), stripe=None, horn=((96, 86, 72), (132, 120, 102), (160, 150, 132))),
    "ibex_kid": dict(back=(140, 122, 96), coat=(166, 148, 118), light=(188, 172, 144), belly=(222, 212, 190), dark=(118, 100, 78),
                     face=(150, 132, 106), muzzle=(196, 182, 160), stripe=None, horn=((110, 98, 82), (140, 128, 110), (164, 154, 136))),
    # chamois
    "chamois": dict(back=(104, 66, 40), coat=(146, 98, 58), light=(172, 126, 82), belly=(206, 178, 140), dark=(34, 26, 22),
                    face=(232, 224, 206), muzzle=(226, 218, 200), stripe=(40, 30, 24), horn=((24, 22, 22), (40, 36, 34), (58, 54, 50))),
    "chamois_kid": dict(back=(140, 102, 70), coat=(170, 128, 88), light=(190, 152, 112), belly=(220, 198, 166), dark=(70, 52, 40),
                        face=(232, 224, 208), muzzle=(226, 218, 202), stripe=(84, 62, 46), horn=((40, 36, 34), (52, 48, 44), (64, 60, 56))),
    # saiga
    "saiga_buck": dict(back=(150, 112, 70), coat=(184, 142, 92), light=(206, 170, 120), belly=(236, 228, 210), dark=(110, 80, 52),
                       face=(176, 136, 90), muzzle=(168, 128, 86), stripe=None, horn=((178, 128, 52), (214, 164, 78), (236, 204, 128))),
    "saiga_doe": dict(back=(158, 120, 78), coat=(190, 150, 100), light=(210, 176, 128), belly=(238, 230, 214), dark=(122, 92, 62),
                      face=(184, 146, 100), muzzle=(176, 138, 96), stripe=None, horn=((178, 128, 52), (214, 164, 78), (236, 204, 128))),
    "saiga_calf": dict(back=(176, 146, 108), coat=(198, 170, 130), light=(214, 190, 154), belly=(238, 232, 218), dark=(144, 116, 86),
                       face=(196, 166, 128), muzzle=(186, 156, 120), stripe=None, horn=((178, 128, 52), (214, 164, 78), (236, 204, 128))),
}


def body(top, bottom, z_front, z_back, belly_from=0.72):
    """Down the side: darker back (a dorsal stripe for the chamois) -> coat -> lighter -> pale belly; darker chest."""
    def mat(v, face, x, y, z, i, j):
        p = PAL[v]
        t = (y - top) / (bottom - top)
        if face == "bottom":
            return fur(p["belly"], i, j, 11, 0.03)
        if face == "top":
            if p["stripe"] and abs(x) < 1.1:
                return fur(p["stripe"], i, j, 12, 0.06)
            return fur(lerp(p["back"], p["coat"], abs(x) / abs(top) * 0.7), i, j, 13)
        if face == "front":  # chest: the buck's dark breast, others just a shade darker
            return fur(lerp(p["dark"] if v == "ibex_buck" else p["back"], p["coat"], t * 0.5), i, j, 14)
        if face == "back":
            if t > belly_from - 0.15 or abs(x) < 1.5 and y > top + 2:
                return fur(lerp(p["light"], p["belly"], 0.6), i, j, 15, 0.04)  # pale rump round the tail
            return fur(p["coat"], i, j, 16)
        if t > belly_from:
            c = lerp(p["light"], p["belly"], smooth((t - belly_from) / 0.12))
            return fur(c, i, j, 17, 0.04)
        c = lerp(p["back"], p["coat"], t / 0.3) if t < 0.3 else lerp(p["coat"], p["light"], (t - 0.3) / (belly_from - 0.3) * 0.6)
        if v == "ibex_buck" and t > 0.45 and z < z_front + (z_back - z_front) * 0.35:
            c = lerp(c, p["dark"], 0.45)  # dark lower flank and shoulder of the buck
        if v.startswith("chamois") and t > belly_from - 0.12:
            c = lerp(c, p["dark"], 0.55)  # the chamois' dark flank band above the belly
        return fur(c, i, j, 18, 0.07)
    return mat


def hump(v, face, x, y, z, i, j):
    p = PAL[v]
    return fur(lerp(p["back"], p["dark"], 0.25 if v == "ibex_buck" else 0.0), i, j, 21, 0.08)


def neck(top, bottom, z_front, z_back):
    def mat(v, face, x, y, z, i, j):
        p = PAL[v]
        t = (y - top) / (bottom - top)  # 0 at the head .. 1 at the shoulders
        if face == "front":  # throat
            if v.startswith("chamois") and t < 0.45:
                return fur(p["face"], i, j, 22, 0.03)  # white throat patch
            if v == "ibex_buck":
                return fur(lerp(p["dark"], p["back"], 0.3), i, j, 23, 0.09)
            return fur(lerp(p["light"], p["belly"], 0.4), i, j, 24, 0.05)
        if face in ("top", "bottom"):
            return fur(p["back"], i, j, 25)
        crest = (z - z_front) / (z_back - z_front)
        if p["stripe"] and crest > 0.8:
            return fur(p["stripe"], i, j, 26, 0.06)
        return fur(lerp(lerp(p["face"] if not v.startswith("chamois") else p["coat"], p["coat"], t), p["back"], crest * 0.5), i, j, 27)
    return mat


def skull(eye_y, eye_z):
    def mat(v, face, x, y, z, i, j):
        p = PAL[v]
        cham = v.startswith("chamois")
        if face in ("left", "right"):
            if abs(y - eye_y) < 0.6 and abs(z - eye_z) < 0.6:
                return EYE
            if abs(y - eye_y) < 0.6 and abs(z - eye_z + 1) < 0.6 and not cham:
                return shade(EYE, 1.6)
            if cham and abs((y - eye_y) - (z - eye_z) * 0.25) < 0.75:
                return fur(p["stripe"], i, j, 41, 0.05)  # black band through the eye
            c = p["face"]
            if not cham and y > eye_y + 1.2:
                c = lerp(c, p["muzzle"], 0.4)
            return fur(c, i, j, 42, 0.05)
        if face == "front":
            if cham:
                return fur(p["face"] if abs(x) < 0.8 else p["stripe"], i, j, 43, 0.05)
            return fur(lerp(p["face"], p["dark"], 0.35 if y < -1 else 0.0), i, j, 44, 0.05)
        if face == "top":
            return fur(p["stripe"] if cham and abs(x) > 0.7 else lerp(p["face"], p["back"], 0.4), i, j, 45, 0.06)
        if face == "bottom":
            return fur(lerp(p["face"], p["belly"], 0.5), i, j, 46, 0.04)
        return fur(p["face"], i, j, 47)
    return mat


def muzzle(top, bottom, nose=(52, 44, 40)):
    def mat(v, face, x, y, z, i, j):
        p = PAL[v]
        cham = v.startswith("chamois")
        if face == "front":
            if y < top + 1.0:
                return shade(nose, 0.85) if abs(x) > 0.4 else nose
            if abs(x) < 0.6 and y < bottom - 0.5:
                return nose
            return fur(p["muzzle"], i, j, 50, 0.03)
        if face == "top":
            if cham:
                return fur(p["face"], i, j, 51, 0.04)
            return fur(lerp(p["face"], p["muzzle"], 0.4), i, j, 52, 0.05)
        if face in ("left", "right"):
            if cham and y < (top + bottom) / 2:
                return fur(p["stripe"], i, j, 53, 0.05)  # the band runs on to the muzzle
            if abs(y - (bottom - 0.5)) < 0.6 and z < -5:
                return shade(nose, 1.3)  # mouth line
            return fur(p["muzzle"], i, j, 54, 0.04)
        return fur(p["muzzle"], i, j, 55, 0.03)
    return mat


def ear(v, face, x, y, z, i, j):
    """Sticks out along x; -x is the outer tip (mirrored onto the left ear)."""
    p = PAL[v]
    tip = x < -0.8
    if face == "front":
        return fur(INNER_EAR if not tip else p["dark"], i, j, 60, 0.05)
    return fur(p["dark"] if tip and not v.startswith("saiga") else p["back"], i, j, 61)


def horn(t0, t1, length, knobs=False, amber=False):
    """Ridged horn. Ibex: big knobs across the front face every other row. Saiga: waxy, translucent-looking amber
    with pale rings. Others: fine ridges."""
    def mat(v, face, x, y, z, i, j):
        base, mid, tip = PAL[v]["horn"]
        t = t0 if face == "bottom" else t1 if face == "top" else t0 + (t1 - t0) * (-y / length)
        c = lerp(base, mid, t / 0.5) if t < 0.5 else lerp(mid, tip, (t - 0.5) / 0.5)
        if face in ("top", "bottom"):
            return c
        if knobs and face == "front":
            return shade(c, 1.22) if j % 2 == 0 else shade(c, 0.7)  # the ibex's knobbed ridges
        if amber:
            c = shade(c, 1.12 if (i + j) % 3 == 0 else 1.0)  # waxy sheen
            if j % 2 == 0 and t < 0.8:
                c = lerp(c, (244, 226, 170), 0.35)  # pale growth rings
            return c
        c = shade(c, 0.88 if j % 2 else 1.04)
        if face == "right":
            c = shade(c, 0.9)
        return c
    return mat


def tail(v, face, x, y, z, i, j):
    p = PAL[v]
    if face in ("front", "bottom"):
        return fur(p["belly"] if not v.startswith("chamois") else p["dark"], i, j, 80, 0.03)
    return fur(p["dark"] if y > 1 or v.startswith("ibex") else p["back"], i, j, 81)


def beard(v, face, x, y, z, i, j):
    return fur(lerp(PAL[v]["dark"], (30, 24, 20), 0.4), i, j // 2 * 2, 85, 0.1)


def leg_upper(top, bottom, hind):
    def mat(v, face, x, y, z, i, j):
        p = PAL[v]
        t = (y - top) / (bottom - top)
        if face == "top":
            return p["coat"]
        if v.startswith("chamois"):
            return fur(lerp(p["coat"], p["dark"], smooth((t - 0.2) / 0.6)), i, j, 90 + hind, 0.05)
        c = lerp(p["coat"], p["light"], t * 0.5)
        if face == "left" or (hind and face == "back"):
            c = lerp(c, p["belly"], 0.5)  # inner side
        if face == "front" and (v == "ibex_buck" or not hind):
            c = lerp(c, p["dark"], 0.55)  # dark leg fronts
        return fur(c, i, j, 92 + hind, 0.05)
    return mat


def leg_lower(length):
    def mat(v, face, x, y, z, i, j):
        p = PAL[v]
        if face == "bottom":
            return HOOF if abs(x) > 0.2 else shade(HOOF, 0.6)
        if face == "top":
            return p["coat"]
        if y > length - 1:
            return shade(HOOF, 1.0 + 0.2 * (h(i, j, 95) - 0.5))
        if v.startswith("chamois"):
            return fur(p["dark"], i, j, 96, 0.06)
        if v.startswith("saiga"):
            return fur(p["light"] if face != "front" else p["coat"], i, j, 97, 0.05)
        c = lerp(p["light"], p["dark"], 0.5 if face == "front" else 0.15)
        return fur(c, i, j, 98, 0.05)
    return mat


# mirrors IbexModel.createBodyLayer: name -> (u, v, x0, y0, z0, w, h, d, material)
IBEX = {
    "body": (0, 0, -4, -4, -8, 8, 9, 16, body(-4, 5, -8, 8)),
    "hump": (0, 25, -3, -5, -8, 6, 2, 7, hump),
    "tail": (48, 11, -1, 0, -0.5, 2, 3, 1, tail),
    "neck": (26, 25, -2, -6, -2.5, 4, 7, 5, neck(-6, 1, -2.5, 2.5)),
    "skull": (44, 25, -2, -2.5, -4, 4, 4, 5, skull(-1.0, -2.5)),
    "muzzle": (48, 0, -1.5, -1.5, -7, 3, 3, 3, muzzle(-1.5, 1.5)),
    "beard": (48, 6, -0.5, 0, -1, 1, 3, 2, beard),
    "ear": (56, 6, -1.5, -0.5, -0.5, 3, 1, 1, ear),
    "horn": (54, 11, -1, -4, -1, 2, 4, 2, horn(0.0, 0.5, 4, knobs=True)),
    "horn_thin": (54, 17, -0.5, -4, -0.5, 1, 4, 1, horn(0.5, 1.0, 4, knobs=True)),
    "nanny_horn": (58, 17, -0.5, -3, -0.5, 1, 3, 1, horn(0.2, 0.9, 3)),
    "front_upper": (0, 37, -1.5, -1, -1.5, 3, 5, 3, leg_upper(-1, 4, 0)),
    "lower": (12, 37, -1, 0, -1, 2, 5, 2, leg_lower(5)),
    "hind_upper": (20, 37, -1.5, -1, -2, 3, 6, 4, leg_upper(-1, 5, 1)),
}
CHAMOIS = {
    "body": (0, 0, -3, -3.5, -6.5, 6, 7, 13, body(-3.5, 3.5, -6.5, 6.5, 0.74)),
    "tail": (52, 7, -0.5, 0, -0.5, 1, 2, 1, tail),
    "neck": (38, 0, -1.5, -6, -2, 3, 7, 4, neck(-6, 1, -2, 2)),
    "skull": (38, 11, -1.5, -2, -3.5, 3, 3, 4, skull(-0.8, -2.2)),
    "muzzle": (52, 0, -1, -1, -6, 2, 2, 3, muzzle(-1, 1)),
    "ear": (52, 5, -1.5, -0.5, -0.5, 3, 1, 1, ear),
    "horn": (56, 7, -0.5, -3, -0.5, 1, 3, 1, horn(0.0, 0.6, 3)),
    "hook": (60, 7, -0.5, -2, -0.5, 1, 2, 1, horn(0.6, 1.0, 2)),
    "front_upper": (0, 20, -1, -1, -1, 2, 5, 2, leg_upper(-1, 4, 0)),
    "lower": (8, 20, -1, 0, -1, 2, 5, 2, leg_lower(5)),
    "hind_upper": (16, 20, -1, -1, -1.5, 2, 5, 3, leg_upper(-1, 4, 1)),
}


def saiga_nose(v, face, x, y, z, i, j):
    """The swollen proboscis: coat-coloured, wrinkled across, dark nostrils opening downward at the front."""
    p = PAL[v]
    if face == "front":
        if y > 1.0 and abs(x) > 0.3:
            return (44, 34, 30)  # nostrils
        return fur(shade(p["muzzle"], 0.95 if j % 2 else 1.05), i, j, 101, 0.04)
    if face == "bottom":
        return (60, 46, 40) if abs(x) > 0.3 else shade(p["muzzle"], 0.7)
    if face in ("left", "right"):
        return fur(shade(p["muzzle"], 0.9 if j % 2 == 0 else 1.0), i, j, 102, 0.05)  # wrinkles
    return fur(lerp(p["muzzle"], p["face"], 0.5), i, j, 103, 0.05)


def saiga_lip(v, face, x, y, z, i, j):
    return fur(lerp(PAL[v]["belly"], PAL[v]["muzzle"], 0.4), i, j, 104, 0.04)


SAIGA = {
    "body": (0, 0, -3.5, -3.5, -7, 7, 7, 14, body(-3.5, 3.5, -7, 7, 0.66)),
    "tail": (36, 21, -1, 0, -0.5, 2, 2, 1, tail),
    "neck": (42, 0, -1.5, -5, -2, 3, 6, 4, neck(-5, 1, -2, 2)),
    "skull": (42, 10, -2, -2, -4, 4, 4, 5, skull(-0.6, -1.8)),
    "nose": (0, 21, -1.5, -1.5, -7, 3, 4, 3, saiga_nose),
    "lip": (12, 21, -1, 2, -6.5, 2, 1, 2, saiga_lip),
    "ear": (28, 21, -1.5, -0.5, -0.5, 3, 1, 1, ear),
    "horn": (20, 21, -0.5, -5, -0.5, 1, 5, 1, horn(0.0, 0.7, 5, amber=True)),
    "horn_tip": (24, 21, -0.5, -2, -0.5, 1, 2, 1, horn(0.7, 1.0, 2, amber=True)),
    "front_upper": (0, 28, -1, -1, -1, 2, 5, 2, leg_upper(-1, 4, 0)),
    "lower": (8, 28, -1, 0, -1, 2, 5, 2, leg_lower(5)),
    "hind_upper": (16, 28, -1, -1, -1.5, 2, 5, 3, leg_upper(-1, 4, 1)),
}

# ---------------------------------------------------------------- marmot

MARMOT_PAL = {
    "marmot": dict(back=(110, 96, 78), tip=(176, 164, 140), belly=(176, 128, 78), crown=(58, 48, 40), muzzle=(214, 204, 186),
                   tail=(92, 76, 60), tail_tip=(40, 32, 28)),
    "marmot_pup": dict(back=(120, 112, 100), tip=(170, 164, 150), belly=(176, 146, 110), crown=(84, 76, 68), muzzle=(208, 200, 188),
                       tail=(108, 98, 86), tail_tip=(70, 62, 56)),
}


def grizzle(base, tip, i, j, salt):
    """Agouti guard hairs: a share of the texels are pale tips."""
    return fur(lerp(base, tip, 0.55), i, j, salt + 1, 0.06) if h(i, j, salt) < 0.28 else fur(base, i, j, salt + 2, 0.08)


def m_body(v, face, x, y, z, i, j):
    p = MARMOT_PAL[v]
    t = (y + 6) / 6  # 0 back .. 1 belly
    if face == "bottom":
        return fur(p["belly"], i, j, 111, 0.05)
    if face == "top":
        return grizzle(p["back"], p["tip"], i, j, 112)
    if face == "front":
        return fur(lerp(p["back"], p["belly"], smooth(t)), i, j, 113)
    if t > 0.7:
        return fur(lerp(p["back"], p["belly"], smooth((t - 0.7) / 0.3)), i, j, 114)
    return grizzle(p["back"], p["tip"], i, j, 115)


def m_skull(v, face, x, y, z, i, j):
    p = MARMOT_PAL[v]
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 0.5) < 0.1:
            return EYE
        if y < -0.5:
            return fur(p["crown"], i, j, 121, 0.06)
        return fur(lerp(p["muzzle"], p["back"], 0.3), i, j, 122, 0.04)
    if face in ("left", "right"):
        if abs(y + 0.5) < 0.6 and abs(z + 2.5) < 0.6:
            return EYE
        if y < -1:
            return fur(p["crown"], i, j, 123, 0.06)
        return fur(lerp(p["back"], p["muzzle"], 0.35 if y > 0.5 else 0.0), i, j, 124)
    if face == "top":
        return fur(p["crown"], i, j, 125, 0.06)
    if face == "bottom":
        return fur(p["muzzle"], i, j, 126, 0.04)
    return grizzle(p["back"], p["tip"], i, j, 127)


def m_snout(v, face, x, y, z, i, j):
    p = MARMOT_PAL[v]
    if face == "front":
        if y < 0.5:
            return (60, 44, 40) if abs(x) < 0.6 else fur(p["muzzle"], i, j, 131, 0.03)  # nose
        if abs(x) < 0.6:
            return (236, 214, 140)  # the orange-white incisors
        return fur(p["muzzle"], i, j, 132, 0.03)
    return fur(p["muzzle"], i, j, 133, 0.04)


def m_ear(v, face, x, y, z, i, j):
    return fur(MARMOT_PAL[v]["crown"], i, j, 141, 0.06)


def m_tail(v, face, x, y, z, i, j):
    p = MARMOT_PAL[v]
    if z > 2.5 or face == "back":
        return fur(p["tail_tip"], i, j, 151, 0.08)
    return grizzle(p["tail"], p["tip"], i, j // 2 * 2 + i % 2, 152)


def m_leg(v, face, x, y, z, i, j):
    p = MARMOT_PAL[v]
    if face == "bottom" or y > 2:
        return (52, 42, 36)  # dark feet with claws
    return fur(lerp(p["back"], p["belly"], 0.5), i, j, 161, 0.06)


MARMOT = {
    "body": (0, 0, -3, -6, -7.5, 6, 6, 9, m_body),
    "skull": (30, 0, -2, -2, -3, 4, 4, 3, m_skull),
    "snout": (44, 0, -1.5, -0.5, -4, 3, 2, 1, m_snout),
    "ear": (52, 0, -0.5, -1, -0.5, 1, 1, 1, m_ear),
    "tail": (30, 7, -1, -0.5, 0, 2, 2, 4, m_tail),
    "leg": (0, 15, -1, 0, -1, 2, 3, 2, m_leg),
}

# ---------------------------------------------------------------- cave hyena

Y_COAT, Y_LIGHT, Y_BELLY = (160, 138, 100), (186, 166, 128), (204, 190, 160)
Y_SPOT, Y_DARK, Y_MUZZLE = (70, 54, 40), (44, 36, 30), (60, 50, 44)
TOOTH, TONGUE = (230, 222, 200), (164, 80, 80)


def spotted(c, x, y, z, i, j, salt, size=1.6):
    """Dark spots on a jittered grid in 3D (so they line up across faces)."""
    gx, gy, gz = round(x / 2.6), round(y / 2.4), round(z / 2.6)
    if h(gx * 7 + gz, gy, salt) < 0.55:
        cx = gx * 2.6 + (h(gx, gz, salt + 1) - 0.5) * 1.2
        cy = gy * 2.4 + (h(gz, gx, salt + 2) - 0.5) * 1.0
        cz = gz * 2.6 + (h(gx + 3, gz, salt + 3) - 0.5) * 1.2
        if (x - cx) ** 2 + (y - cy) ** 2 + (z - cz) ** 2 < size:
            return fur(Y_SPOT, i, j, salt + 4, 0.08)
    return c


def y_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "bottom":
            return fur(Y_BELLY, i, j, 201, 0.04)
        if face == "top":
            return fur(shade(Y_COAT, 0.85), i, j, 202, 0.07)
        c = fur(lerp(Y_COAT, Y_LIGHT, t * 0.8) if t < 0.85 else Y_BELLY, i, j, 203, 0.07)
        return spotted(c, x, y, z, i, j, 204) if t < 0.85 else c
    return mat


def y_crest(v, face, x, y, z, i, j):
    return fur(Y_DARK, i // 1, j // 2 * 2 + i % 2, 211, 0.1)  # bristly dark mane


def y_neck(v, face, x, y, z, i, j):
    if face in ("front", "bottom"):
        return fur(lerp(Y_LIGHT, Y_BELLY, 0.5), i, j, 221, 0.05)
    return spotted(fur(Y_COAT, i, j, 222, 0.07), x, y, z, i, j, 223, 1.0)


def y_tail(v, face, x, y, z, i, j):
    return fur(lerp(Y_COAT, Y_DARK, 0.3 + y / 12), i, j, 231, 0.07)


def y_tuft(v, face, x, y, z, i, j):
    return fur(Y_DARK, i, j, 232, 0.12)


def y_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 1.5) < 0.1:
            return (150, 110, 50)  # brown-amber eyes
        if abs(abs(x) - 2.5) < 0.1 and abs(y + 1.5) < 0.1:
            return Y_DARK
        if y > 0:
            return fur(Y_MUZZLE, i, j, 241, 0.06)  # dark face blending into the muzzle
        return fur(lerp(Y_COAT, Y_LIGHT, 0.3), i, j, 242, 0.06)
    if face == "top":
        return fur(Y_COAT, i, j, 243, 0.06)
    if face == "bottom":
        return fur(Y_LIGHT, i, j, 244, 0.05)
    return spotted(fur(lerp(Y_COAT, Y_LIGHT, 0.3 if y > 0 else 0.0), i, j, 245, 0.06), x, y, z, i, j, 246, 0.8)


def y_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        if y < 0:
            return (30, 26, 24) if abs(x) < 1.2 else fur(Y_MUZZLE, i, j, 251, 0.04)  # broad black nose
        return (34, 28, 26) if abs(x) < 0.5 else fur(Y_MUZZLE, i, j, 252, 0.05)
    if face == "bottom":
        return shade(TONGUE, 0.8)
    if face == "top":
        return fur(lerp(Y_MUZZLE, Y_COAT, 0.3 if z > -6 else 0.0), i, j, 253, 0.05)
    if y > 1:
        return (36, 30, 28)  # lip line
    return fur(Y_MUZZLE, i, j, 254, 0.05)


def y_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -3 or abs(x) > 1.2 else TONGUE
    return fur(lerp(Y_MUZZLE, Y_LIGHT, 0.4), i, j, 261, 0.05)


def y_ear(v, face, x, y, z, i, j):
    """Big round ears: top corners cut away; pale inside, dark backs and rims."""
    if y < -2 and abs(x) > 0.8:
        return TRANSPARENT
    if face == "front":
        return fur((176, 156, 130), i, j, 271, 0.05) if abs(x) < 0.8 and y > -2 else fur(Y_DARK, i, j, 272, 0.06)
    return fur(Y_DARK, i, j, 273, 0.07)


def y_leg(top, bottom, hind):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            return Y_COAT
        c = fur(lerp(Y_COAT, Y_LIGHT, t * 0.6), i, j, 281 + hind, 0.06)
        return spotted(c, x * 1.5, y + hind * 7, z * 1.5 + (5 if hind else 0), i, j, 283, 0.7)
    return mat


def y_lower(v, face, x, y, z, i, j):
    if y >= 4:
        if face == "bottom":
            return (42, 34, 30)
        if face == "front":
            return Y_DARK if abs(x) < 0.4 else fur(lerp(Y_LIGHT, Y_DARK, 0.5), i, j, 291, 0.05)
        return fur(lerp(Y_LIGHT, Y_DARK, 0.45), i, j, 292, 0.05)
    return fur(lerp(Y_LIGHT, Y_DARK, 0.15 + y / 12), i, j, 293, 0.06)


HYENA = {
    "chest": (0, 0, -3.5, -4, -8, 7, 8, 9, y_body(-4, 4)),
    "hind": (0, 17, -3, -2, 1, 6, 6, 7, y_body(-2, 4)),
    "crest": (32, 0, -1, -5.5, -9, 2, 2, 9, y_crest),
    "neck": (26, 17, -2.5, -3.5, -11, 5, 6, 4, y_neck),
    "tail": (16, 30, -1, 0, -1, 2, 6, 2, y_tail),
    "tuft": (24, 30, -1.5, 5, -1.5, 3, 3, 3, y_tuft),
    "skull": (44, 11, -3, -3, -4, 6, 5, 4, y_skull),
    "muzzle": (44, 20, -2, -1, -8, 4, 3, 4, y_muzzle),
    "jaw": (0, 30, -2, 0, -4, 4, 1, 4, y_jaw),
    "ear": (54, 0, -1.5, -3, -0.5, 3, 3, 1, y_ear),
    "front_upper": (0, 38, -1.5, -1, -1.5, 3, 6, 3, y_leg(-1, 5, 0)),
    "lower": (12, 38, -1, 0, -1, 2, 4, 2, y_lower),
    "paw": (20, 38, -1, 4, -2, 2, 1, 3, y_lower),
    "hind_upper": (30, 38, -1.5, -1, -2, 3, 5, 4, y_leg(-1, 4, 1)),
}

# ---------------------------------------------------------------- recolouring helpers (cave bear, items)


def load(rel):
    return Image.open(ROOT / "src/main/resources/assets/mechanical/textures" / f"{rel}.png").convert("RGBA")


def vanilla(rel):
    assert JAR, "no merged minecraft jar (run a gradle build first)"
    with zipfile.ZipFile(JAR) as z:
        from io import BytesIO
        return Image.open(BytesIO(z.read(f"assets/minecraft/textures/{rel}.png"))).convert("RGBA")


def lum(c):
    return (0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]) / 255


def ramp(stops, t):
    """Piecewise colour ramp over 0..1."""
    t = max(0.0, min(1.0, t))
    n = len(stops) - 1
    k = min(int(t * n), n - 1)
    return lerp(stops[k], stops[k + 1], t * n - k)


def recolour(img, stops, keep=lambda c: False, lo=0.05, hi=0.9, salt=0):
    """Map every pixel's lightness (stretched lo..hi) onto the ramp; pixels where keep(c) stay as they are."""
    out = img.copy()
    px = out.load()
    for x in range(img.width):
        for y in range(img.height):
            c = px[x, y]
            if c[3] == 0 or keep(c):
                continue
            t = (lum(c) - lo) / (hi - lo)
            r = ramp(stops, t)
            if salt:
                r = shade(r, 1 + (h(x, y, salt) - 0.5) * 0.08)
            px[x, y] = (*r, c[3])
    return out


def is_raw(c):
    """The red raw rim and flesh on a raw pelt item."""
    return c[0] > c[1] * 1.45 and c[0] > 90


def is_eye(c):
    return c[0] > 150 and c[1] > 100 and c[2] < 80 and c[0] - c[2] > 100


def cave_bear():
    """The brown bear's texture made darker, colder umber with a grizzled hump; the domed brow box filled from the skull."""
    stops = [(14, 10, 8), (38, 28, 22), (62, 46, 36), (88, 70, 56), (124, 104, 86), (170, 150, 128)]
    src = load("entity/wildlife/brown_bear")
    keep = lambda c: (c[0] > 150 and c[1] > 140 and c[2] > 120) or c[0] > c[1] * 1.5  # claws, teeth, tongue
    img = recolour(src, stops, keep, 0.06, 0.62, salt=301)
    px = img.load()
    # dome box at (108, 0): 7 wide, 4 tall, 2 deep; paint it from the skull top's palette with a lighter crown
    for face, tx, ty, x, y, z in __import__("wildlife_paint").face_texels(108, 0, -3.5, -5, -7.5, 7, 4, 2):
        base = (64, 48, 38) if face != "top" else (78, 60, 48)
        if face == "front" and y > -2.5:
            base = (52, 40, 32)  # the stop, where the dome meets the muzzle
        px[tx, ty] = (*fur(base, tx, ty, 302, 0.09), 255)
    # grizzled pale tips over the hump (48, 0) .. (73, 14)
    for x in range(48, 74):
        for y in range(0, 15):
            c = px[x, y]
            if c[3] and h(x, y, 303) < 0.18:
                px[x, y] = (*lerp(c[:3], (150, 132, 108), 0.5), 255)
    return img


def cave_bear_cub():
    stops = [(20, 14, 10), (46, 34, 26), (72, 56, 44), (100, 82, 66), (140, 120, 100)]
    return recolour(load("entity/wildlife/brown_bear_cub"), stops, lambda c: c[0] > c[1] * 1.5 or min(c[:3]) > 150, 0.06, 0.62, salt=304)


# ---------------------------------------------------------------- items

BEAR_STOPS = [(16, 12, 10), (40, 30, 24), (66, 50, 40), (96, 78, 62), (140, 120, 100)]
IBEX_STOPS = [(30, 26, 22), (70, 60, 50), (106, 94, 78), (146, 132, 112), (192, 180, 158)]
SAIGA_STOPS = [(70, 50, 32), (130, 98, 62), (176, 138, 92), (206, 174, 128), (236, 226, 206)]
HYENA_STOPS = [(40, 32, 26), (100, 84, 62), (150, 130, 96), (180, 160, 124), (206, 192, 162)]
MARMOT_STOPS = [(36, 30, 26), (84, 72, 60), (118, 104, 86), (160, 146, 124), (196, 160, 116)]
CHAMOIS_LEATHER = [(110, 84, 46), (170, 138, 84), (214, 186, 128), (236, 214, 160), (248, 236, 196)]


def with_spots(img, salt, colour=(58, 44, 34), density=0.3):
    """Dark spots (2x2 cells) over the fur inside the outline; the raw rim and the outline stay."""
    out = img.copy()
    px, src = out.load(), img.load()
    for x in range(1, img.width - 1):
        for y in range(1, img.height - 1):
            c = src[x, y]
            inside = all(src[x + dx, y + dy][3] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if c[3] and inside and not is_raw(c) and not is_eye(c) and h(x // 2, y // 2, salt) < density and h(x, y, salt + 1) < 0.75:
                px[x, y] = (*lerp(c[:3], colour, 0.75), 255)
    return out


def pelt(base, stops, spots=None, raw=False, salt=0):
    img = recolour(load(f"item/{base}"), stops, lambda c: is_raw(c) or is_eye(c), salt=salt)
    return with_spots(img, spots) if spots else img


def meat(base, tint, dark=1.0):
    """A hand-drawn scraps icon, shifted towards the animal's meat colour."""
    img = load(f"item/{base}")
    px = img.load()
    for x in range(16):
        for y in range(16):
            c = px[x, y]
            if c[3]:
                n = lerp(c[:3], shade(tint, lum(c) * 2.0 * dark), 0.45)
                px[x, y] = (*n, c[3])
    return img


def grid(rows, pal):
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = pal[ch]
                im.putpixel((x, y), c if len(c) == 4 else (*c, 255))
    return im


IBEX_HORN = [
    "................",
    "..........kK....",
    ".........kHK....",
    "........kHHK....",
    ".......kHHK.....",
    "......kHHHK.....",
    ".....kHHHK......",
    "....kHHHK.......",
    "...kHHHHK.......",
    "...kHHHK........",
    "..kHHHHK........",
    "..kHHHK.........",
    "..kHHHHK........",
    "..kbbbbK........",
    "...kkkk.........",
    "................",
]


def ibex_horn():
    im = grid(IBEX_HORN, {"k": (44, 38, 32), "K": (66, 58, 48), "H": (128, 116, 98), "b": (164, 150, 128)})
    px = im.load()
    for y in range(16):  # the knobbed ridges across the front of the horn
        for x in range(16):
            c = px[x, y]
            if c[3] and c[:3] == (128, 116, 98):
                px[x, y] = (*shade(c[:3], 1.25 if y % 2 == 0 else 0.82), 255)
    return im


def chamois_horn():
    return grid([
        "................",
        "................",
        "......kkk.......",
        ".....kHHHk......",
        ".....kHkkHk.....",
        ".....kHk.kHk....",
        ".....kHk..kk....",
        ".....kHk........",
        ".....kHk........",
        ".....kHk........",
        "....kHHk........",
        "....kHHk........",
        "....kHHHk.......",
        "....kbbbk.......",
        ".....kkk........",
        "................"], {"k": (16, 14, 14), "H": (52, 48, 46), "b": (110, 100, 88)})


def saiga_horn():
    im = grid([
        "................",
        ".........aK.....",
        "........aAK.....",
        "........aAK.....",
        ".......aAAK.....",
        ".......rrrK.....",
        ".......aAAK.....",
        "......aAAAK.....",
        "......rrrrK.....",
        "......aAAAK.....",
        ".....aAAAAK.....",
        ".....rrrrrK.....",
        ".....aAAAAK.....",
        ".....dddddK.....",
        "......KKKK......",
        "................"], {"a": (238, 206, 132), "A": (212, 162, 76), "r": (244, 228, 176), "K": (146, 100, 40), "d": (176, 126, 52)})
    return im


def hyena_fang():
    return grid([
        "................",
        "................",
        "................",
        "........k.......",
        ".......kwk......",
        ".......kwk......",
        "......kwwsk.....",
        "......kwwsk.....",
        ".....kwwwsk.....",
        ".....kwwwsk.....",
        ".....kwwssk.....",
        ".....kyyyyk.....",
        ".....kyrryk.....",
        "......kkkk......",
        "................",
        "................"], {"k": (60, 54, 46), "w": (238, 232, 214), "s": (200, 192, 170), "y": (214, 196, 150), "r": (176, 120, 110)})


def marmot_fat():
    im = load("item/bear_fat")
    return recolour(im, [(120, 100, 60), (200, 180, 130), (236, 222, 180), (250, 244, 220)], lambda c: is_raw(c), 0.3, 0.95)


def marmot_salve():
    return grid([
        "................",
        "................",
        "................",
        "....gGGGGGg.....",
        "...gYYyYYYYg....",
        "...cyYYYyYYc....",
        "...kcccccccK....",
        "...kCCCCCCCK....",
        "...kCcCCCcCK....",
        "...kCCCCCCCK....",
        "...kCCcCCCCK....",
        "....kCCCCCK.....",
        ".....kkkkk......",
        "................",
        "................",
        "................"], {"g": (92, 120, 60), "G": (120, 150, 76), "Y": (236, 214, 150), "y": (214, 190, 120),
                              "c": (150, 88, 56), "C": (176, 106, 66), "k": (92, 54, 36), "K": (70, 40, 28)})


def horn_bow(base="sinew_bow"):
    """The Sinew Bow (each pulling stage) with its limbs faced in dark ibex horn, knobbed; the string kept."""
    im = load(f"item/{base}")
    px = im.load()
    for x in range(16):
        for y in range(16):
            c = px[x, y]
            if not c[3]:
                continue
            if c[0] > 170 and c[1] > 160 and abs(c[0] - c[2]) < 40:
                continue  # the string
            t = lum(c)
            n = ramp([(36, 30, 26), (74, 64, 54), (118, 106, 90), (150, 138, 118)], (t - 0.15) / 0.6)
            if (x + y) % 3 == 0 and t > 0.35:
                n = shade(n, 1.18)
            px[x, y] = (*n, c[3])
    return im


def chamois_item(base):
    return recolour(vanilla(base), CHAMOIS_LEATHER, lo=0.15, hi=0.95)


# ---------------------------------------------------------------- blocks


def skull_block():
    """Cave bear skull texture: old ivory bone with hairline cracks (most faces), dark socket and teeth in the corners."""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for x in range(16):
        for y in range(16):
            c = lerp((200, 186, 152), (228, 218, 190), h(x, y, 401))
            if h(x // 3, y, 402) < 0.08 or h(x, y // 3, 403) < 0.06:
                c = shade(c, 0.78)  # cracks
            px[x, y] = (*c, 255)
    for x in range(12, 16):  # eye socket / nasal opening region (u 12-16, v 0-4)
        for y in range(0, 4):
            px[x, y] = (*lerp((34, 28, 22), (60, 50, 40), h(x, y, 404)), 255)
    for x in range(12, 16):  # teeth (u 12-16, v 12-16)
        for y in range(12, 16):
            px[x, y] = (*((240, 232, 210) if (x + y) % 2 else (210, 200, 176)), 255)
    return im


def horn_window():
    """A wooden lattice (frame and a cross of bars) holding four panes of thin-scraped saiga horn: amber, see-through,
    with the horn's growth rings and a waxy sheen."""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for x in range(16):
        for y in range(16):
            frame = x in (0, 15) or y in (0, 15)
            bar = x in (7, 8) or y in (7, 8)
            if frame or bar:
                c = lerp((92, 64, 38), (124, 88, 54), h(x, y // 3, 421))
                if frame and (x in (0, 15)) != (y in (0, 15)) or bar and (x == 8 or y == 8):
                    c = shade(c, 0.78)  # the shaded side of each bar
                px[x, y] = (*c, 255)
                continue
            lx, ly = x % 8, y % 8  # within a pane: rings round its centre, lighter towards the top left
            d = ((lx - 3.5) ** 2 + (ly - 3.5) ** 2) ** 0.5
            c = lerp((224, 168, 76), (244, 214, 140), 0.5 + 0.5 * (1 - (lx + ly) / 12))
            if int(d * 1.3) % 2 == 0:
                c = shade(c, 0.9)
            a = 150 if (lx + ly) % 5 else 175
            px[x, y] = (*c, a)
    return im


def main():
    for v in ("ibex_buck", "ibex_nanny", "ibex_kid"):
        img = paint(IBEX, v, (64, 64))
        save(img, f"wildlife/{v}.png")
        if v != "ibex_kid":
            save(skinned(img, IBEX, ("horn", "horn_thin", "nanny_horn")), f"skinned/wildlife/{v}.png")
    for v in ("chamois", "chamois_kid"):
        img = paint(CHAMOIS, v, (64, 64))
        save(img, f"wildlife/{v}.png")
        if v == "chamois":
            save(skinned(img, CHAMOIS, ("horn", "hook")), f"skinned/wildlife/{v}.png")
    for v in ("saiga_buck", "saiga_doe", "saiga_calf"):
        img = paint(SAIGA, v, (64, 64))
        save(img, f"wildlife/{v}.png")
        if v != "saiga_calf":
            save(skinned(img, SAIGA, ("horn", "horn_tip")), f"skinned/wildlife/{v}.png")
    for v in ("marmot", "marmot_pup"):
        img = paint(MARMOT, v, (64, 32))
        save(img, f"wildlife/{v}.png")
        if v == "marmot":
            save(skinned(img, MARMOT), "skinned/wildlife/marmot.png")
    img = paint(HYENA, "hyena", (64, 64))
    save(img, "wildlife/cave_hyena.png")
    save(skinned(img, HYENA), "skinned/wildlife/cave_hyena.png")
    bear = cave_bear()
    save(bear, "wildlife/cave_bear.png")
    flesh = _skinner().skin(bear.copy())
    save(flesh, "skinned/wildlife/cave_bear.png")
    save(cave_bear_cub(), "wildlife/cave_bear_cub.png")

    items = {
        # meats: each from the hand-drawn cut that suits it, shifted to the animal's colour
        "ibex_meat": meat("mutton_scraps", (150, 40, 36), 0.9), "cooked_ibex_meat": meat("cooked_mutton_scraps", (120, 76, 44)),
        "chamois_meat": meat("venison_scraps", (160, 36, 40)), "cooked_chamois_meat": meat("cooked_venison_scraps", (132, 84, 50)),
        "marmot_meat": meat("rabbit_scraps", (196, 110, 96)), "cooked_marmot_meat": meat("cooked_rabbit_scraps", (150, 104, 60)),
        "saiga_meat": meat("venison_scraps", (176, 56, 48), 1.1), "cooked_saiga_meat": meat("cooked_venison_scraps", (150, 100, 58)),
        "cave_bear_meat": meat("beef_scraps", (110, 24, 26), 0.8), "cooked_cave_bear_meat": meat("cooked_beef_scraps", (90, 56, 34), 0.85),
        "hyena_meat": meat("game_meat_scraps", (110, 40, 52), 0.85), "cooked_hyena_meat": meat("cooked_game_meat_scraps", (96, 66, 50), 0.85),
        # pelts and hides
        "raw_ibex_pelt": pelt("raw_wolf_pelt", IBEX_STOPS, salt=501), "ibex_pelt": pelt("wolf_pelt", IBEX_STOPS, salt=502),
        "raw_saiga_pelt": pelt("raw_wolf_pelt", SAIGA_STOPS, salt=503), "saiga_pelt": pelt("wolf_pelt", SAIGA_STOPS, salt=504),
        "raw_hyena_pelt": pelt("raw_wolf_pelt", HYENA_STOPS, spots=505, salt=506), "hyena_pelt": pelt("wolf_pelt", HYENA_STOPS, spots=505, salt=507),
        "raw_cave_bear_pelt": pelt("raw_bear_pelt", BEAR_STOPS, salt=508), "cave_bear_pelt": pelt("bear_pelt", BEAR_STOPS, salt=509),
        "marmot_pelt": recolour(load("item/squirrel_pelt"), MARMOT_STOPS, is_eye, salt=510),
        "raw_chamois_hide": recolour(load("item/raw_hide"), [(70, 40, 24), (120, 76, 44), (156, 106, 64), (186, 140, 96), (214, 182, 140)],
                                     lambda c: is_raw(c) and c[0] > 150, salt=511),
        "chamois_leather": chamois_item("item/leather"), "chamois_boots": chamois_item("item/leather_boots"),
        # rugs
        "ibex_rug": pelt("wolf_rug", IBEX_STOPS, salt=512), "saiga_rug": pelt("wolf_rug", SAIGA_STOPS, salt=513),
        "hyena_rug": pelt("wolf_rug", HYENA_STOPS, spots=514, salt=515), "cave_bear_rug": pelt("bear_rug", BEAR_STOPS, salt=516),
        # parts and things made from them
        "ibex_horn": ibex_horn(), "chamois_horn": chamois_horn(), "saiga_horn": saiga_horn(), "hyena_fang": hyena_fang(),
        "marmot_fat": marmot_fat(), "marmot_salve": marmot_salve(), "horn_bow": horn_bow(),
        **{f"horn_bow_pulling_{n}": horn_bow(f"sinew_bow_pulling_{n}") for n in range(3)},
        # spawn eggs: hi, light, mid, dark, outline light, outline dark
        "ibex_spawn_egg": egg(["#a89a84", "#8c7c66", "#706252", "#584c40", "#3e352c", "#28221c"],
                              [(5, 6, "#c8baa0"), (10, 9, "#c8baa0"), (6, 12, "#c8baa0")], [(4, 0), (11, 0), (4, 1), (11, 1)],
                              [(4, 0, "#5a5040"), (11, 0, "#5a5040"), (4, 1, "#807058"), (11, 1, "#807058"), (7, 13, "#2c241e"), (8, 13, "#2c241e")]),
        "chamois_spawn_egg": egg(["#c08a56", "#a4703e", "#8a5a30", "#6e4424", "#4c2e18", "#301c10"],
                                 [(x, y, "#2a1e18") for x, y in ((7, 3), (8, 3), (7, 5), (8, 6), (7, 8), (8, 10))],
                                 [(6, 1), (9, 1)], [(6, 1, "#181414"), (9, 1, "#181414"), (5, 5, "#ece6d6"), (10, 5, "#ece6d6"),
                                                    (5, 6, "#1e1816"), (10, 6, "#1e1816")]),
        "saiga_spawn_egg": egg(["#e2c494", "#cea870", "#b88e58", "#9c7446", "#6e5032", "#4a3420"],
                               [(5, 12, "#f0e8d8"), (10, 12, "#f0e8d8"), (7, 13, "#f0e8d8")], [(6, 0), (9, 0), (6, 1), (9, 1)],
                               [(6, 0, "#eccc84"), (9, 0, "#eccc84"), (6, 1, "#d4a24c"), (9, 1, "#d4a24c"), (7, 9, "#3c2c24"), (8, 9, "#3c2c24")]),
        "marmot_spawn_egg": egg(["#b0a490", "#968872", "#7c6e5a", "#645644", "#463c30", "#2c2620"],
                                [(5, 12, "#c08850"), (10, 12, "#c08850"), (7, 13, "#c08850"), (8, 13, "#c08850")], [],
                                [(6, 4, "#3a3028"), (9, 4, "#3a3028"), (7, 6, "#e0d8c8"), (8, 6, "#e0d8c8"), (7, 3, "#3a3028"), (8, 3, "#3a3028")]),
        "cave_bear_spawn_egg": egg(["#6a5444", "#54402e", "#402e22", "#30221a", "#201610", "#140e0a"],
                                   [(5, 6, "#8c7660"), (10, 5, "#8c7660"), (8, 9, "#8c7660")], [(4, 1), (11, 1)],
                                   [(4, 1, "#201610"), (11, 1, "#201610"), (7, 12, "#1a120e"), (8, 12, "#1a120e")]),
        "cave_hyena_spawn_egg": egg(["#cdb994", "#b49e74", "#9a845e", "#7c6848", "#544632", "#382e20"],
                                    [(x, y, "#3e3024") for x, y in ((5, 5), (10, 4), (4, 9), (11, 8), (7, 7), (6, 12), (10, 12))],
                                    [(4, 0), (11, 0), (4, 1), (11, 1)], [(4, 0, "#2c241c"), (11, 0, "#2c241c"), (7, 13, "#241c16"), (8, 13, "#241c16")]),
    }
    for name, im in items.items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
    print("wrote", len(items), "items")

    # rugs as blocks (the same flat pelt seen from above), the skull, the cave bear pelt on the tall Drying Rack
    blocks = {"ibex_rug": pelt_block("wolf_rug", IBEX_STOPS, None), "saiga_rug": pelt_block("wolf_rug", SAIGA_STOPS, None),
              "hyena_rug": pelt_block("wolf_rug", HYENA_STOPS, 514), "cave_bear_rug": pelt_block("bear_rug", BEAR_STOPS, None),
              "cave_bear_skull": skull_block(), "horn_window": horn_window()}
    for stage in ("raw", "dry"):
        for half in ("lower", "upper"):
            for side in ("", "_back"):
                src = f"drying_rack_bear_{stage}_{half}{side}"
                blocks[f"drying_rack_cave_bear_{stage}_{half}{side}"] = recolour(load(f"block/{src}"), BEAR_STOPS,
                                                                                 lambda c: is_raw(c) or side == "_back", salt=520)
    for name, im in blocks.items():
        im.save(BLOCK / f"{name}.png")
    print("wrote", len(blocks), "blocks")

    # the Chamois Boots on the body: vanilla's (grey, dyeable) leather layer in chamois yellow
    armour = recolour(vanilla("entity/equipment/humanoid/leather"), CHAMOIS_LEATHER, lo=0.1, hi=0.95)
    out = TEX / "equipment/humanoid/chamois_leather.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    armour.save(out)
    print("wrote", out.relative_to(ROOT))


def pelt_block(base, stops, spots):
    img = recolour(load(f"block/{base}"), stops, is_eye, salt=530)
    return with_spots(img, spots) if spots else img


if __name__ == "__main__":
    main()
