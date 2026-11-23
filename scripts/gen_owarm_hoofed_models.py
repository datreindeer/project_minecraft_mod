"""The water buffalo's and the Malayan tapir's models and skins (adults and young), each from one table
(wildlife/warm/WarmFauna, docs/fauna/warm.md):

- src/main/java/com/mechanical/client/wildlife/warm/WarmHoofGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the adults' skinned carcass skins in textures/entity/skinned/wildlife/

Water buffalo: a massive deep body with the hump of the withers and a sagging belly, a thick wrinkled neck, the long
low head carried forward with a broad wet muzzle, droopy ears under the horns, and the huge flat ridged crescent horns
sweeping out and back from the brow; slate-grey to blackish sparse-haired skin, lighter grey stockings, the two pale
chevrons on the throat, mud caked on the legs, a thin tasselled tail. Calf: brownish-grey and hairier, horn buds, big
ears, leggy. Tapir: a heavy rounded pear-shaped body rising to the rump, the short trunk over the upper lip, small eyes,
rounded white-rimmed ears, short stout legs with splayed toes, a stub tail; black with the soft-edged off-white saddle
over the back half and the belly. Calf: chestnut with the cream "watermelon" stripes and spots.
Part names are the ones WaterBuffaloModel / WaterBuffaloCalfModel / TapirModel / TapirCalfModel look up (body, neck,
head, horns, trunk, trunk2, trunk_tip, the ears, tail, the four legs each with a "lower" child for MammalPose).
Usage: python scripts/gen_owarm_hoofed_models.py (after gen_fauna_warm_textures.py, whose gen_fauna_warm_tex_mammals.py
still paints these files first). Deterministic.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import CLEAR, Model, rel, side_of  # noqa: E402
from wildlife_paint import h, lerp, save, shade, smooth  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/warm/WarmHoofGeometry.java"
SIDES = (("left", 1), ("right", -1))
GLINT = (214, 218, 222)


def eye(face, p, y0, z0, pal, w=2, hh=2):
    """A small eye on the side faces, top-front corner at (y0, z0): a glint over the dark pupil, the iris behind, a lid rim."""
    if face not in ("left", "right"):
        return None
    y, z = p[1], p[2]
    if y0 <= y < y0 + hh and z0 <= z < z0 + w:
        top, front = y < y0 + 1, z < z0 + 1
        return GLINT if top and front else pal["iris"] if (not front and not top) else pal["pupil"]
    if y0 - 1 <= y < y0 + hh + 0.5 and z0 - 0.5 <= z < z0 + w + 0.5:
        return pal["lid"]
    return None


def legs(m, front, hind, fbox, hbox, flower, hlower, hind_back=0.5):
    """Leg pairs, each a root part with a "lower" child; boxes are (mat, x, y, z, w, h, d) tuples."""
    for s, sd in SIDES:
        for key, at, upper, lower, back in (("f", front, fbox, flower, 0.0), ("h", hind, hbox, hlower, hind_back)):
            name = f"{s}_{'front' if key == 'f' else 'hind'}_leg"
            m.part(name, "root", (sd * at[0], at[1], at[2]))
            for b in upper:
                m.box(name, *b)
            low = f"{s[0]}{key}_lower"
            m.part(low, name, (0, at[3], back), name="lower")
            for b in lower:
                m.box(low, *b)


# ============================================================================ water buffalo
def buffalo_model():
    m = Model("Water buffalo")
    m.part("body", "root", (0, 8, 0))
    m.box("body", "chest", -7, -8, -15, 14, 12, 12)        # deep chest, top of the back at y 0
    m.box("body", "brisket", -4, 0, -16.5, 8, 5, 2)        # the low chest front, the lower chevron
    m.box("body", "hump", -5, -10, -14, 10, 2, 9)          # the hump of the withers (1.6 m at the shoulder)
    m.box("body", "hump_slope", -4, -9, -5, 8, 1, 5)
    m.box("body", "barrel", -8, -7, -3, 16, 11, 11)        # the widest part, over the belly
    m.box("body", "belly", -6.5, 3.5, -10, 13, 1, 16)      # the sagging belly
    m.box("body", "paunch", -5, 4.3, -7, 10, 1, 10)
    m.box("body", "rump", -7, -8, 8, 14, 11, 5)
    m.box("body", "hips", -6.5, -8.6, 9, 13, 1, 4)         # the hip bones standing up under the skin
    m.box("body", "rump_end", -6, -7.6, 13, 12, 9, 1)     # the rounded buttocks
    m.box("body", "rump_back", -4.5, -7, 14, 9, 7, 1)
    m.part("tail", "body", (0, -6.5, 14.6), (0.1, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 12, 1)
    m.box("tail", "tassel", -1, 11, -1, 2, 4, 2)
    m.part("neck", "body", (0, -3, -14.5), (0.2, 0, 0))
    m.box("neck", "neck", -5, -5, -7, 10, 10, 8)           # short and very thick
    m.box("neck", "crest", -4, -6, -6, 8, 2, 6)            # the muscle running up into the withers
    m.box("neck", "throat", -3, 4, -7, 6, 3, 6)            # loose throat skin, the upper chevron
    m.part("head", "neck", (0, 0.5, -6.5), (0.35, 0, 0))
    m.box("head", "poll", -4.5, -5, -3, 9, 3, 3)           # the broad boss the horns grow from
    m.box("head", "skull", -4, -3.5, -5.5, 8, 6, 5)
    m.box("head", "jowl", -3.5, 1.5, -6, 7, 3, 4)
    m.box("head", "face", -3, -3, -9, 6, 6, 4)             # long, straight face
    m.box("head", "muzzle", -3.5, -1.5, -12, 7, 5, 3)      # broad wet muzzle
    m.box("head", "nose", -3, -1.8, -12.4, 6, 3, 1)
    m.box("head", "jaw", -2.5, 3, -11.5, 5, 2, 6)
    for s, sd in SIDES:
        # droopy leaf ears set under the horn roots (the model droops them further by EAR_Z)
        m.part(f"{s}_ear", "head", (sd * 4, -1.5, -2.5), (0, 0, sd * 0.6))
        m.box(f"{s}_ear", "ear", 0 if sd > 0 else -4, -0.5, -1.5, 4, 1, 3)
    m.part("horns", "head")
    for s, sd in SIDES:
        # four flat, ridged segments: out from the poll, then sweeping back in a wide crescent, the tip turned up and in
        segs = (("horn1", (sd * 3.8, -4.2, -1.5), (0.1, -sd * 0.1, sd * 0.08), (6, 2, 3)),
                ("horn2", (sd * 5.5, 0, 0), (0, -sd * 0.32, -sd * 0.16), (5, 2, 2)),
                ("horn3", (sd * 4.6, 0, 0), (0, -sd * 0.4, -sd * 0.32), (4, 2, 2)),
                ("horn4", (sd * 3.6, 0, 0), (0, -sd * 0.45, -sd * 0.5), (3, 1, 1)))
        parent = "horns"
        for mat, at, rot, (w, hh, d) in segs:
            pid = f"{s}_{mat}"
            m.part(pid, parent, at, rot)
            m.box(pid, mat, 0 if sd > 0 else -w, -hh / 2, -d / 2, w, hh, d)
            parent = pid
    legs(m, (5, 11, -10, 6), (5, 10, 10, 7),
         [("arm", -2.5, -2, -2.5, 5, 8, 5)],
         [("thigh", -2.5, -2, -3.5, 5, 9, 7)],
         [("knee", -2, -0.5, -2, 4, 2, 4), ("cannon", -1.5, 1, -1.5, 3, 5, 3), ("hoof", -2, 5, -3, 4, 2, 5)],
         [("hock", -1.5, -0.5, -1, 3, 2, 4), ("cannon", -1.5, 1, -1.5, 3, 5, 3), ("hoof", -2, 5, -3, 4, 2, 5)],
         hind_back=1)
    return m


def buffalo_calf_model():
    m = Model("Water buffalo calf")
    m.part("body", "root", (0, 13, 0))
    m.box("body", "chest", -4, -3.5, -7.5, 8, 7, 7)
    m.box("body", "barrel", -4.5, -3.5, -1, 9, 7, 5)
    m.box("body", "rump", -4, -4, 4, 8, 7, 4)              # calves stand a little higher at the rump
    m.box("body", "ridge", -3, -4.4, -6.5, 6, 1, 13)       # the fuzzy top line
    m.box("body", "belly", -3.5, 3, -4, 7, 1, 8)
    m.part("tail", "body", (0, -3, 7.8), (0.15, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 6, 1)
    m.box("tail", "tassel", -1, 5, -1, 2, 2, 2)
    m.part("neck", "body", (0, -1.5, -7), (0.25, 0, 0))
    m.box("neck", "neck", -2.5, -3, -4.5, 5, 6, 5)
    m.box("neck", "throat", -1.5, 2.5, -4.5, 3, 1, 4)
    m.part("head", "neck", (0, -0.5, -4), (0.45, 0, 0))
    m.box("head", "skull", -3, -3, -4.5, 6, 5, 5)          # a big baby head
    m.box("head", "forelock", -2, -3.6, -3.5, 4, 1, 2)
    m.box("head", "face", -2, -2, -7.5, 4, 4, 3)
    m.box("head", "muzzle", -2.5, -0.8, -9, 5, 3, 2)
    m.box("head", "jaw", -1.5, 2.2, -8.5, 3, 1, 4)
    m.box("head", "bud", 1.2, -3.8, -3.6, 1, 1, 1)         # horn buds
    m.box("head", "bud", -2.2, -3.8, -3.6, 1, 1, 1)
    for s, sd in SIDES:
        m.part(f"{s}_ear", "head", (sd * 2.8, -1.5, -1.5), (0, 0, sd * 0.55))
        m.box(f"{s}_ear", "ear", 0 if sd > 0 else -3, -0.5, -1, 3, 1, 2)  # big floppy ears
    legs(m, (2.3, 15, -4.5, 4), (2.3, 14.5, 4.5, 4.5),
         [("arm", -1.5, -1, -1.5, 3, 5, 3)],
         [("thigh", -1.5, -1.5, -2, 3, 6, 4)],
         [("cannon", -1, 0, -1, 2, 4, 2), ("hoof", -1.5, 4, -2, 3, 1, 3)],
         [("cannon", -1, 0, -1, 2, 4, 2), ("hoof", -1.5, 4.5 - 0.5, -2, 3, 1, 3)],
         hind_back=0.3)
    return m


WB = {
    None: dict(top=-2, bot=13, dark=(54, 55, 60), skin=(78, 79, 85), low=(92, 92, 97), belly=(108, 106, 108),
               hair_hi=(110, 111, 118), stock=(146, 142, 140), stock_dk=(118, 114, 112), chevron=(204, 200, 192),
               muzzle=(50, 50, 55), wet=(120, 124, 132), nose=(22, 22, 26), lip=(164, 158, 152), ear_in=(140, 124, 124),
               tuft=(20, 18, 18), hoof=(34, 30, 28), hoof_hi=(64, 58, 54), mud=(92, 76, 56), mud_dk=(66, 54, 40),
               mud_lt=(122, 104, 80), pupil=(14, 10, 10), iris=(64, 40, 26), lid=(126, 124, 128), hair=0.05, mudline=20.6,
               horn_root=(92, 88, 82), horn=(50, 48, 46), horn_tip=(24, 22, 22)),
    "calf": dict(top=9.5, bot=17, dark=(116, 98, 84), skin=(142, 122, 104), low=(154, 136, 118), belly=(170, 154, 136),
                 hair_hi=(172, 152, 132), stock=(176, 162, 148), stock_dk=(152, 138, 124), chevron=(220, 210, 194),
                 muzzle=(70, 60, 56), wet=(120, 112, 110), nose=(44, 38, 36), lip=(184, 172, 160), ear_in=(176, 150, 140),
                 tuft=(60, 48, 40), hoof=(50, 44, 40), hoof_hi=(80, 72, 66), mud=(110, 92, 70), mud_dk=(84, 70, 52),
                 mud_lt=(134, 116, 90), pupil=(14, 10, 10), iris=(70, 44, 28), lid=(170, 152, 136), hair=0.12,
                 mudline=21.8, horn_root=(120, 110, 96), horn=(96, 88, 78), horn_tip=(70, 64, 58)),
}


def wb_coat(pal, face, P):
    """Sparse-haired hide, near black on the back, greyer down the flanks and belly; a few pale hairs and darker pores."""
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], pal["top"], pal["bot"])
    c = lerp(pal["dark"], pal["skin"], smooth(t / 0.45))
    c = lerp(c, pal["low"], (t - 0.45) / 0.3)
    c = lerp(c, pal["belly"], (t - 0.75) / 0.25)
    g = h(int(P[0] * 2 + 50), int(P[1] * 2 + P[2] * 7 + 90), 41)
    if g < pal["hair"]:
        c = lerp(c, pal["hair_hi"], 0.5)  # the sparse coarse hairs
    elif g > 0.95:
        c = shade(c, 0.82)
    return c


def wb_mud(pal, P, c):
    """Caked mud up the legs (and splashed under the belly): an uneven crusted edge, darker wet patches."""
    line = pal["mudline"] + 2.2 * h(int(P[0] * 2 + 9), int(P[2] * 2 + 33), 42) - 1.1
    splash = h(int(P[0] * 2 + 3), int(P[1] * 2 + P[2] * 3), 51) < 0.22 and P[1] > line - 3
    if P[1] < line and not splash:
        return c
    n = h(int(P[0] * 3 + 5), int(P[1] * 3 + P[2] * 5), 43)
    return pal["mud_dk"] if n < 0.25 else pal["mud_lt"] if n > 0.85 else pal["mud"]


def buffalo_colour(v, mat, face, p, P, b):
    pal = WB[v]
    x, y, z = p
    if mat in ("chest", "hump", "hump_slope", "barrel", "rump", "hips", "rump_end", "rump_back", "crest"):
        c = wb_coat(pal, face, P)
        if face in ("left", "right") and v is None:
            t = rel(P[1], pal["top"], pal["bot"])
            if 0.14 < t < 0.3 and h(int(P[2] * 1.5), int(P[1] * 2), 52) < 0.6:
                c = shade(c, 1.12)                          # the dull gloss along the upper flank
            if abs(P[2] + 10.5) < 3 and t < 0.55:
                c = shade(c, 1.06)                          # the shoulder blade catching the light
            fold = -4.6 + (P[1] - 6) * 0.25                 # the skin fold behind the elbow
            if 4 < P[1] < 12 and abs(P[2] - fold) < 0.5:
                c = shade(c, 0.72)
            flank = 7 - (P[1] - 8) * 0.5                    # the stifle fold in front of the thigh
            if 6 < P[1] < 12.5 and abs(P[2] - flank) < 0.5:
                c = shade(c, 0.75)
        return c
    if mat in ("belly", "paunch"):
        return wb_mud(pal, (P[0], P[1] + 4.5, P[2]), wb_coat(pal, face, P)) if face == "bottom" and h(int(x), int(z), 44) < 0.35 else wb_coat(pal, face, P)
    if mat == "brisket":
        if face == "front":
            # the lower pale chevron across the chest front, a V opening up
            k = (y - b["y"]) - (4 - abs(x) * 0.9)
            if -1.2 < k < 0.4:
                return pal["chevron"]
        return wb_coat(pal, face, P)
    if mat == "neck":
        c = wb_coat(pal, face, P)
        if side_of(face) and face not in ("front", "back") and int(z + 20) % 3 == 0 and h(int(y), int(z), 45) < 0.7:
            c = shade(c, 0.78)  # the deep folds around the neck
        if face == "bottom":
            return lerp(c, pal["belly"], 0.4)
        return c
    if mat == "throat":
        if face == "bottom":
            # the upper chevron: a pale V under the throat, the point towards the chest
            k = (z - b["z"]) - abs(x) * 1.3
            if 1.0 < k < 2.6:
                return pal["chevron"]
            return lerp(pal["low"], pal["belly"], 0.5)
        if face == "front" and abs(x) > 1.0:
            return pal["chevron"]
        return lerp(wb_coat(pal, face, P), pal["low"], 0.4)
    if mat in ("poll", "skull", "face", "jowl"):
        e = eye(face, p, -2.5, -5.0, pal) if mat == "skull" else None
        if e:
            return e
        c = lerp(pal["dark"], pal["skin"], 0.45)
        if face == "top" and mat == "poll":
            return shade(c, 0.9) if int(x + 9) % 2 == 0 else c  # the coarse whorl on the brow
        if face == "bottom" or (mat == "jowl" and y > 3):
            return lerp(c, pal["low"], 0.6)
        if mat == "face" and side_of(face) and face != "front" and y < -1.5:
            return shade(c, 1.1)  # light along the bridge of the nose
        if mat == "skull" and face == "front":
            return shade(c, 0.9)
        return c
    if mat in ("muzzle", "nose"):
        if face == "front":
            if mat == "nose":
                # the wet nose pad: nostrils at the sides, glossy highlights
                if 1.0 < abs(x) < 2.6 and y < -0.6:
                    return pal["nose"]                      # the nostrils
                return pal["wet"] if (h(int(x * 2), int(y * 2), 46) < 0.08 and y > -0.5) else shade(pal["muzzle"], 0.9)
            return pal["lip"] if y > 2.5 else pal["muzzle"]
        if mat == "muzzle" and y > 2.5:
            return pal["lip"]  # pale lips round the mouth
        if face == "top":
            return lerp(pal["muzzle"], pal["wet"], 0.25 * h(int(x * 2), int(z * 2), 47))
        return pal["muzzle"]
    if mat == "jaw":
        if face == "bottom" or (side_of(face) and y > 4):
            return pal["chevron"] if face == "bottom" and z < b["z"] + 2.5 else pal["lip"]  # pale chin
        return lerp(pal["muzzle"], pal["low"], 0.4)
    if mat == "ear":
        if face == "bottom":
            edge = abs(z) > 1 or abs(x) > 3
            return lerp(pal["ear_in"], pal["hair_hi"], 0.4) if edge else pal["ear_in"]  # hairy rim, pinkish inside
        return lerp(pal["dark"], pal["skin"], 0.5)
    if mat.startswith("horn"):
        # dark keratin, a paler worn base, transverse ridges across the flat face, black at the tip
        k = {"horn1": 0.0, "horn2": 0.3, "horn3": 0.62, "horn4": 0.9}[mat] + abs(x) / 16
        c = lerp(pal["horn_root"], pal["horn"], k / 0.45)
        c = lerp(c, pal["horn_tip"], (k - 0.6) / 0.4)
        if mat in ("horn1", "horn2", "horn3") and int(abs(x) * 1 + 0.5) % 2 == 0 and face in ("top", "front", "back"):
            c = shade(c, 0.72)
        if face == "top":
            c = shade(c, 1.12)
        return c
    if mat == "tail":
        return wb_coat(pal, face, (P[0], pal["top"] + y * 0.4, P[2]))
    if mat == "tassel":
        return pal["tuft"] if h(int(x * 2), int(y), 48) > 0.2 else shade(pal["tuft"], 1.5)
    if mat in ("arm", "thigh"):
        c = wb_coat(pal, face, (P[0], P[1] - 2, P[2]))
        return lerp(c, pal["stock_dk"], rel(P[1], pal["mudline"] - 3.5, pal["mudline"] - 1))
    if mat in ("knee", "hock", "cannon"):
        c = lerp(pal["stock_dk"], pal["stock"], 0.6 + 0.4 * h(int(x * 2), int(y * 2), 49))  # the pale stockings
        return wb_mud(pal, P, c)
    if mat == "hoof":
        if face == "front" and abs(x) < 0.5:
            return shade(pal["hoof"], 0.6)  # the cleft between the two big splayed toes
        if face == "top":
            return wb_mud(pal, (P[0], 30, P[2]), pal["hoof"])
        return pal["hoof_hi"] if y < b["y"] + 1 and face != "bottom" else pal["hoof"]
    if mat == "bud":
        return pal["horn"] if face == "top" else pal["horn_root"]
    if mat == "forelock" or mat == "ridge":
        c = wb_coat(pal, face, P)
        return shade(c, 0.88) if h(int(x * 2), int(z * 2), 50) < 0.4 else c  # tufted calf hair
    return wb_coat(pal, face, P)


# ============================================================================ tapir
def tapir_model():
    m = Model("Malayan tapir")
    m.part("body", "root", (0, 13, 0))
    m.box("body", "chest", -4, -3, -11, 8, 8, 7)           # the narrow front of the pear
    m.box("body", "chest_front", -3, -1.5, -12, 6, 5, 1)
    m.box("body", "shoulder", -3, -3.6, -10, 6, 1, 6)
    m.box("body", "mid", -5.5, -4.5, -4, 11, 9, 8)
    m.box("body", "belly", -4.5, 4.5, -7, 9, 1, 14)        # the heavy round belly
    m.box("body", "rump", -6, -6, 4, 12, 10, 7)            # wide and high: the back rises to the hips
    m.box("body", "haunch", -5.5, 4, 4.5, 11, 1, 5)          # rounds the bottom of the rump
    m.box("body", "back", -4, -6.8, 2, 8, 1, 9)            # the arched back, highest over the hips
    m.box("body", "back_slope", -3.5, -5.4, -4, 7, 1, 6)
    m.box("body", "rump_back", -4.5, -5, 11, 9, 8, 1)
    m.part("tail", "body", (0, -4, 11.6), (0.3, 0, 0))
    m.box("tail", "tail", -1, 0, -1, 2, 3, 2)              # a short stub
    m.part("neck", "body", (0, -1, -10.5), (0.15, 0, 0))
    m.box("neck", "neck", -3.5, -3.5, -5, 7, 7, 6)         # thick, merging into the head
    m.box("neck", "nape", -2, -4.5, -5, 4, 1, 5)           # the low crest on the nape
    m.part("head", "neck", (0, 0.5, -4.5), (0.35, 0, 0))
    m.box("head", "skull", -3, -3, -4, 6, 6, 4)            # rounded cranium
    m.box("head", "dome", -2, -4, -3.5, 4, 1, 3)
    m.box("head", "cheek", -3.5, 0, -4.5, 7, 3, 4)         # heavy jowls
    m.box("head", "face", -2.5, -2.5, -7.5, 5, 5, 4)       # tapering face
    m.box("head", "lip", -1.5, 2, -7.5, 3, 2, 3)           # short lower lip under the trunk
    m.part("trunk", "head", (0, -0.6, -7.2), (0.25, 0, 0))
    m.box("trunk", "trunk", -2, -1.5, -2.5, 4, 3, 3)       # nose and upper lip drawn out into a trunk
    m.part("trunk2", "trunk", (0, 0.2, -2.3), (0.3, 0, 0))
    m.box("trunk2", "trunk2", -1.5, -1, -2.2, 3, 2, 3)
    m.part("trunk_tip", "trunk2", (0, 0.1, -2), (0.35, 0, 0))
    m.box("trunk_tip", "trunk_tip", -1, -1, -1.2, 2, 2, 2)
    for s, sd in SIDES:
        m.part(f"{s}_ear", "head", (sd * 2.3, -2.6, -1), (-0.35, 0, sd * 0.25))
        m.box(f"{s}_ear", "ear", -1.5, -3, -0.5, 3, 3, 1)  # rounded, white-rimmed
    legs(m, (3.2, 14.5, -7.5, 4.5), (3.2, 13.5, 7, 4.5),
         [("arm", -2, -1, -2, 4, 6, 4)],
         [("thigh", -2.5, -2, -2.5, 5, 7, 5)],
         [("shin", -1.5, 0, -1.5, 3, 3, 3), ("foot", -2, 3, -2, 4, 2, 4),
          ("toe", -2, 4, -2.6, 1, 1, 1), ("toe", -0.6, 4, -2.8, 1, 1, 1), ("toe", 0.8, 4, -2.6, 1, 1, 1)],
         [("shin", -1.5, 0, -1.5, 3, 4, 3), ("foot", -2, 4, -2, 4, 2, 4),
          ("toe", -1.9, 5, -2.6, 1, 1, 1), ("toe", -0.5, 5, -2.8, 1, 1, 1), ("toe", 0.9, 5, -2.6, 1, 1, 1)],
         hind_back=0.5)
    return m


def tapir_calf_model():
    m = Model("Tapir calf")
    m.part("body", "root", (0, 16.5, 0))
    m.box("body", "chest", -3, -3, -6, 6, 6, 5)
    m.box("body", "barrel", -3.5, -3.5, -1.5, 7, 7, 5.0)
    m.box("body", "rump", -3.5, -4, 3.5, 7, 7, 4)
    m.box("body", "back", -2.5, -4.5, -2, 5, 1, 9)
    m.box("body", "belly", -2.5, 3.2, -4, 5, 1, 7)
    m.part("tail", "body", (0, -2.5, 7.4), (0.35, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 2, 1)
    m.part("neck", "body", (0, -0.5, -5.5), (0.15, 0, 0))
    m.box("neck", "neck", -2.5, -2.5, -4, 5, 5, 4)
    m.part("head", "neck", (0, 0.3, -3.5), (0.3, 0, 0))
    m.box("head", "skull", -2.5, -2.5, -4, 5, 5, 4)
    m.box("head", "face", -2, -1.8, -6, 4, 3, 2)
    m.box("head", "lip", -1, 1.2, -5.6, 2, 1, 2)
    m.part("trunk", "head", (0, -0.6, -5.8), (0.3, 0, 0))
    m.box("trunk", "trunk", -1.5, -1, -2, 3, 2, 2)
    m.part("trunk_tip", "trunk", (0, 0.1, -1.8), (0.35, 0, 0))
    m.box("trunk_tip", "trunk_tip", -1, -0.5, -1.6, 2, 1, 2)
    for s, sd in SIDES:
        m.part(f"{s}_ear", "head", (sd * 1.9, -2.2, -0.8), (-0.3, 0, sd * 0.35))
        m.box(f"{s}_ear", "ear", -1.5, -3, -0.5, 3, 3, 1)
    legs(m, (2, 18, -4, 3), (2.2, 17.5, 4, 3.5),
         [("arm", -1.5, -1, -1.5, 3, 4, 3)],
         [("thigh", -1.5, -1.5, -1.5, 3, 5, 3)],
         [("shin", -1, 0, -1, 2, 2, 2), ("foot", -1.5, 2, -1.5, 3, 1, 3)],
         [("shin", -1, 0, -1, 2, 2, 2), ("foot", -1.5, 2, -1.5, 3, 1, 3)],
         hind_back=0.3)
    return m


TP = dict(black=(26, 25, 28), black_hi=(58, 58, 66), black_lo=(36, 34, 38), saddle=(226, 222, 212),
          saddle_lo=(198, 192, 180), edge=(120, 118, 116), rim=(232, 230, 224), skin=(60, 56, 60), lip=(112, 104, 104),
          nose=(16, 15, 18), toe=(150, 140, 126), toe_dk=(96, 90, 82), pupil=(10, 8, 8), iris=(70, 46, 30),
          lid=(52, 50, 56))
TC = dict(brown=(116, 66, 36), brown_dk=(80, 44, 24), brown_hi=(146, 90, 52), cream=(236, 218, 180),
          cream_dk=(206, 184, 146), belly=(214, 190, 150), nose=(40, 28, 24), toe=(120, 104, 88), toe_dk=(80, 66, 54),
          rim=(236, 222, 190), pupil=(10, 8, 8), iris=(66, 40, 22), lid=(70, 40, 22))


def tp_saddle(P, face):
    """How white a point is (0..1): the saddle from behind the shoulders to the tail root and down over the belly, a
    soft mottled edge; the hind legs, rump end and the front half stay black."""
    x, y, z = P
    front = -3.5 + (y - 7) * 0.45                          # the front edge slants forward towards the belly
    back = 11.8 - max(0.0, y - 11) * 1.1                    # behind, the black of the thighs comes up the rump
    if face == "bottom" and y > 16:
        front = -6.5                                        # the white runs a little further forward underneath
    d = min(z - front, back - z)
    n = h(int(x * 0.7 + 40), int(y * 0.7) * 7 + int(z * 0.7), 61) - 0.5
    return smooth((d + n * 1.2) / 1.6 + 0.5)


def tp_black(face, P, top=7, bot=19):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], top, bot)
    c = lerp(TP["black_hi"], TP["black"], smooth(t / 0.35))  # a cool sheen along the back
    c = lerp(c, TP["black_lo"], (t - 0.7) / 0.3)
    if h(int(P[0] * 2 + 7), int(P[1] * 2 + P[2] * 5), 62) < 0.06:
        c = shade(c, 1.4)
    return c


def tapir_colour(v, mat, face, p, P, b):
    x, y, z = p
    if mat in ("chest", "chest_front", "shoulder", "mid", "belly", "rump", "back", "back_slope", "haunch", "rump_back"):
        k = tp_saddle(P, face)
        if mat == "rump_back":
            k *= rel(-y, -1, 3)                             # the rump end goes black down towards the thighs
        t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 7, 19)
        white = lerp(TP["saddle"], TP["saddle_lo"], (t - 0.5) / 0.5)
        if 0.25 < k < 0.75:
            return lerp(TP["edge"], white, k)               # a grey fringe where the two meet
        return white if k >= 0.75 else tp_black(face, P)
    if mat in ("neck", "nape"):
        return tp_black(face, P)
    if mat in ("skull", "dome", "cheek", "face"):
        e = eye(face, p, -1.8, -3.8, TP) if mat == "skull" else None
        if e:
            return e
        c = tp_black(face, (P[0], P[1] - 2, P[2]))
        if mat == "cheek" and face in ("bottom", "front"):
            return lerp(c, TP["skin"], 0.4)
        return c
    if mat == "lip":
        return TP["lip"] if face in ("front", "bottom") or y > 3 else TP["skin"]  # the pale-rimmed lower lip
    if mat in ("trunk", "trunk2"):
        c = lerp(TP["black"], TP["skin"], 0.3 if mat == "trunk" else 0.5)
        if face in ("top", "left", "right", "bottom") and int((z - b["z"]) * 2) % 2 == 0:
            c = shade(c, 0.8)                                # the wrinkled rings of the proboscis
        return c
    if mat == "trunk_tip":
        if face == "front":
            return TP["nose"] if abs(x) > 0.2 and y < 0.5 else TP["skin"]  # two nostrils at the tip
        return lerp(TP["skin"], TP["lip"], 0.3) if face == "bottom" else TP["skin"]
    if mat == "ear":
        if face in ("front", "back", "top") and rounded_tip(p, b):
            return CLEAR
        rim = face == "top" or y < b["y"] + 1.5 or (face in ("front", "back") and y < b["y"] + 2.5
                                                     and (x < b["x"] + 1 or x > b["x"] + b["w"] - 1))
        if rim:
            return TP["rim"]                                  # the white rim round the top of the ear
        return TP["skin"] if face == "front" else TP["black"]
    if mat == "tail":
        return tp_black(face, P)
    if mat in ("arm", "thigh", "shin"):
        return tp_black(face, (P[0], P[1] - 6, P[2]))
    if mat == "foot":
        c = shade(TP["black_lo"], 0.9)
        return TP["toe_dk"] if face == "bottom" else c
    if mat == "toe":
        return TP["toe"] if face in ("front", "top") else TP["toe_dk"]  # pale little hooves on the splayed toes
    return tp_black(face, P)


def rounded_tip(p, b):
    edge = p[0] < b["x"] + 1 or p[0] > b["x"] + b["w"] - 1
    return p[1] < b["y"] + 1 and edge


def tc_pattern(P, face, salt, top=12.0):
    """The watermelon coat: rows of cream stripes running along the body, broken into dashes, and spots lower down."""
    x, y, z = P
    if face == "top":
        k, off = int(abs(x) // 2), abs(x) % 2 - 1.2         # stripes along the back either side of the spine
        hit = abs(off) < 0.5 and abs(x) > 0.6
    else:
        if y > top + 6.5:
            return h(int(x * 1.2 + 30), int(z * 1.2) * 5 + int(y * 1.2), salt + 64) < 0.3  # spotted lower flanks
        wob = 0.45 * math.sin(z * 0.45 + salt)
        k = int((y - top + wob) // 2.2)
        hit = (y - top + wob) % 2.2 < 0.95
    return hit and h(k * 13 + salt, int((z + 20) / 2.5), salt + 63) > 0.22


def tapir_calf_colour(v, mat, face, p, P, b):
    x, y, z = p
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 12, 21)
    base = lerp(TC["brown_hi"], TC["brown"], smooth(t / 0.4))
    base = lerp(base, TC["brown_dk"], (t - 0.6) / 0.4)
    cream = lerp(TC["cream"], TC["cream_dk"], t)
    if mat in ("chest", "barrel", "rump", "back"):
        return cream if tc_pattern(P, face, 1) else base
    if mat in ("belly", "paunch"):
        return TC["belly"] if face == "bottom" else (cream if tc_pattern(P, face, 1) else base)
    if mat == "neck":
        return cream if tc_pattern((P[0], P[1] - 1, P[2]), face, 3) else base
    if mat in ("skull", "face"):
        e = eye(face, p, -1.5, -3.6, TC) if mat == "skull" else None
        if e:
            return e
        if face == "top" and mat == "skull" and abs(x) < 0.8:
            return TC["cream"]                                # a pale blaze line up the forehead
        if side_of(face) and h(int(y * 1.5 + 9), int(z * 1.5 + 4), 65) < 0.25:
            return TC["cream"]                                # spotted cheeks
        return base
    if mat == "lip":
        return TC["rim"]
    if mat == "trunk":
        return lerp(TC["brown_dk"], TC["nose"], 0.3)
    if mat == "trunk_tip":
        return TC["nose"] if face == "front" and abs(x) > 0.2 else lerp(TC["brown_dk"], TC["nose"], 0.5)
    if mat == "ear":
        if face in ("front", "back", "top") and rounded_tip(p, b):
            return CLEAR
        if face == "top" or y < b["y"] + 1.5:
            return TC["rim"]
        return TC["brown_dk"] if face == "front" else base
    if mat == "tail":
        return base
    if mat in ("arm", "thigh", "shin"):
        spot = h(int(P[0] * 1.4 + 11), int(P[1] * 1.4 + P[2] * 1.4), 66) < 0.28
        return TC["cream"] if spot else lerp(base, TC["brown_dk"], 0.2)
    if mat == "foot":
        return TC["toe_dk"] if face == "bottom" else TC["toe"]
    return base


# ============================================================================
MODELS = {
    # name -> (builder result, colour, java method, keep on the skinned carcass skin or None for no carcass skin)
    "water_buffalo": lambda: (buffalo_model(), buffalo_colour, "waterBuffalo", ("horn1", "horn2", "horn3", "horn4", "hoof"), None),
    "water_buffalo_calf": lambda: (buffalo_calf_model(), lambda v, *a: buffalo_colour("calf", *a), "waterBuffaloCalf", None, "calf"),
    "tapir": lambda: (tapir_model(), tapir_colour, "tapir", ("toe", "foot"), None),
    "tapir_calf": lambda: (tapir_calf_model(), tapir_calf_colour, "tapirCalf", None, None),
}


def main():
    methods = []
    for name, build in MODELS.items():
        m, colour, method, keep, _ = build()
        m.pack()
        methods.append(m.java(method))
        img = m.paint(colour, None)
        save(img, f"wildlife/{name}.png")
        if keep is not None:
            save(m.skinned(img, keep), f"skinned/wildlife/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.warm;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_owarm_hoofed_models.py (with the skins it paints): the water buffalo's and the tapir's parts and boxes. Do not edit. */
public final class WarmHoofGeometry {{
{chr(10).join(methods)}
    private WarmHoofGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
