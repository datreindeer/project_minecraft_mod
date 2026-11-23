"""The herd's young (moose calf, deer fawn, reindeer calf, aurochs calf, mouflon lamb, wild horse foal, wild boar piglet):
their models and skins from one table each, with gen_cold_models.py's Model/painter (every texel painted from the
point it covers on the posed model, so coat, spots and stripes run on across boxes).

- src/main/java/com/mechanical/client/wildlife/HerdGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<young>.png (128x64, or 128x128 when the boxes need it) and skinned/wildlife/<young>.png (the butchered carcass)

All seven are built at TWICE their size (like the pika) for finer detail and drawn at half: their renderers' baby scale
is halved (MechanicalClient, TameHorseRenderer). The part trees, names and rest rotations are the ones the young
models animate (MooseCalfModel, FawnModel, ReindeerCalfModel, AurochsCalfModel, MouflonLambModel, WildHorseFoalModel,
PigletModel), so their pixel offsets in setupAnim are doubled too. The fawn model is shared by the roe deer fawn and the
red deer calf (two skins).

Usage: python scripts/gen_herd_models.py  (run gen_aged_textures.py after it). Deterministic.
"""
import importlib.util
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/HerdGeometry.java"

_spec = importlib.util.spec_from_file_location("gen_cold_models", ROOT / "scripts/gen_cold_models.py")
cm = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(cm)
Model, lerp, shade, smooth, strands, h, cells, save = cm.Model, cm.lerp, cm.shade, cm.smooth, cm.strands, cm.h, cm.cells, cm.save

BODY = {"chest", "barrel", "loin", "rump", "withers", "brisket", "back", "belly"}
LEG = {"upper", "thigh", "knee", "hock", "cannon"}


# ---------------------------------------------------------------- shared building blocks
def legs(m, x, zf, zh, y, front, hind, lower_y, hind_back=0.0):
    """Four legs hanging from the root at height y: front/hind = list of (part, mat, box) where part 'u' is the leg
    and 'l' its "lower" child at lower_y below the pivot (hind ones hind_back further back)."""
    for s, side in ((-1, "right"), (1, "left")):
        for kind, z, boxes in (("front", zf, front), ("hind", zh, hind)):
            leg = m.part(f"{side}_{kind}_leg", at=(s * x, y, z))
            low = m.part(f"{side}_{kind}_lower", leg, (0, lower_y, hind_back if kind == "hind" else 0))
            for part, mat, b in boxes:
                m.box(leg if part == "u" else low, mat, *b)


def ears(m, at, rot, box, parent="head"):
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, parent, (s * at[0], at[1], at[2]), (rot[0], s * rot[1], s * rot[2]))
        m.box(n, "ear", *box)


def check_ground(m):
    low = m.lowest({n for n in m.parts if "leg" in n or "lower" in n})
    assert abs(low - 24) < 0.01, f"{m.name}: hooves at {low}, not 24"


# ---------------------------------------------------------------- fawn (FawnModel; roe deer fawn + red deer calf)
def fawn():
    m = Model("Deer fawn", "fawn", 128, 128)
    B = m.box
    m.part("body", at=(0, 9, 0))
    B("body", "chest", -3.5, -4, -8, 7, 8, 7)
    B("body", "loin", -3, -3.5, -1, 6, 7, 4)
    B("body", "rump", -3.5, -4.5, 3, 7, 8, 6)
    B("body", "brisket", -2.5, -1, -9, 5, 5, 1)
    m.part("tail", "body", (0, -3.5, 8.5), (0.35, 0, 0))
    B("tail", "tail", -1, 0, 0, 2, 4, 1)
    m.part("neck", "body", (0, -1, -6), (0.85, 0, 0))
    B("neck", "neck", -2, -8, -2, 4, 9, 4)
    B("neck", "throat", -1.5, -6, -2.6, 3, 5, 1)
    m.part("head", "neck", (0, -7, 0), (-0.85, 0, 0))
    B("head", "skull", -3, -3.5, -5, 6, 6, 6)
    B("head", "brow", -2.5, -4, -4.5, 5, 1, 4)
    B("head", "muzzle", -2, -1.5, -9, 4, 4, 4)
    B("head", "nose", -1.5, -1.5, -10, 3, 2, 1)
    B("head", "chin", -1.5, 2.5, -8, 3, 1, 3)
    ears(m, (2.2, -3, -0.5), (-0.4, -0.45, 1.0), (-1.5, -6, -0.5, 3, 6, 1))
    legs(m, 2.2, -5.2, 5.2, 11,
         [("u", "upper", (-1.5, -2, -1.5, 3, 8, 3)), ("l", "cannon", (-1, 0, -1, 2, 6, 2)), ("l", "hoof", (-1, 6, -1.4, 2, 1, 2))],
         [("u", "thigh", (-1.5, -3, -2.5, 3, 9, 5)), ("l", "cannon", (-1, 0, -1, 2, 6, 2)), ("l", "hoof", (-1, 6, -1.4, 2, 1, 2))],
         6, 1)
    m.eye = (-0.5, -2.2, 1.0)  # skull-local y, z centre, half size
    return m


ROE = dict(coat=(152, 92, 50), dark=(104, 60, 32), light=(186, 128, 78), under=(222, 196, 160), spot=(238, 226, 200),
           dorsal=(98, 58, 32), muzzle=(118, 84, 62), nose=(26, 20, 18), chin=(228, 218, 202), ear_in=(214, 186, 160),
           ear_rim=(52, 36, 28), hoof=(38, 30, 26), leg=(132, 88, 56), rump=(226, 206, 172), eye=(34, 22, 16), spots=1.0)
RED = dict(ROE, coat=(140, 92, 60), dark=(96, 62, 40), light=(176, 124, 84), under=(208, 184, 150), spot=(226, 206, 170),
           dorsal=(84, 54, 36), muzzle=(110, 80, 60), leg=(124, 86, 60), rump=(210, 182, 136), spots=0.8)


# ---------------------------------------------------------------- moose calf (MooseCalfModel)
def moose_calf():
    m = Model("Moose calf", "mooseCalf", 128, 128)
    B = m.box
    m.part("body", at=(0, 0, 0))
    B("body", "chest", -5.5, -8, -12, 11, 15, 9)
    B("body", "barrel", -5, -7, -3, 10, 13, 8)
    B("body", "rump", -5, -6.5, 5, 10, 12, 7)
    B("body", "withers", -3.5, -9, -11, 7, 1, 7)
    B("body", "brisket", -4, -2, -13, 8, 8, 1)
    m.part("tail", "body", (0, -5.5, 12), (0.3, 0, 0))
    B("tail", "tail", -1, 0, 0, 2, 4, 2)
    m.part("neck", "body", (0, -4, -11), (0.9, 0, 0))
    B("neck", "neck", -3, -10, -3.5, 6, 12, 7)
    B("neck", "mane", -1, -10, 3, 2, 10, 1)
    m.part("head", "neck", (0, -9, 0), (-0.55, 0, 0))
    B("head", "skull", -3.5, -4, -6, 7, 7, 7)
    B("head", "muzzle", -2.5, -2.5, -12, 5, 6, 6)
    B("head", "nose", -3, -2, -15, 6, 5, 3)
    B("head", "chin", -2, 3.5, -13, 4, 1, 7)
    ears(m, (2.8, -3, 0), (-0.2, 0, 0.9), (-1.5, -8, -0.5, 3, 8, 1))
    legs(m, 3.2, -8, 8, 4,
         [("u", "upper", (-2, -2, -2, 4, 12, 4)), ("l", "knee", (-1.5, -1, -1.5, 3, 2, 3)),
          ("l", "cannon", (-1, 1, -1, 2, 8, 2)), ("l", "hoof", (-1.5, 9, -2, 3, 1, 3))],
         [("u", "thigh", (-2, -2, -3, 4, 12, 6)), ("l", "hock", (-1.5, -1, -1.5, 3, 2, 3)),
          ("l", "cannon", (-1, 1, -1, 2, 8, 2)), ("l", "hoof", (-1.5, 9, -2, 3, 1, 3))],
         10, 1)
    m.eye = (-2, -4.2, 1.0)
    return m


MOOSE = dict(coat=(150, 86, 46), dark=(98, 52, 28), light=(184, 118, 68), under=(118, 78, 50), spot=None,
             dorsal=(110, 62, 34), muzzle=(96, 60, 40), nose=(44, 32, 26), chin=(130, 92, 66), ear_in=(168, 128, 100),
             ear_rim=(70, 42, 26), hoof=(40, 32, 28), leg=(176, 138, 102), rump=None, eye=(26, 18, 14), spots=0)


# ---------------------------------------------------------------- reindeer calf (ReindeerCalfModel)
def reindeer_calf():
    m = Model("Reindeer calf", "reindeerCalf", 128, 128)
    B = m.box
    m.part("body", at=(0, 7, 0))
    B("body", "chest", -5, -6, -10, 10, 12, 8)
    B("body", "barrel", -4.5, -5.5, -2, 9, 11, 6)
    B("body", "rump", -5, -6, 4, 10, 11, 6)
    B("body", "brisket", -3.5, -1, -11, 7, 7, 1)
    m.part("tail", "body", (0, -5, 10), (0.3, 0, 0))
    B("tail", "tail", -1.5, 0, 0, 3, 3, 2)
    m.part("neck", "body", (0, -3, -9), (0.8, 0, 0))
    B("neck", "neck", -2.5, -8, -3, 5, 10, 6)
    B("neck", "ruff", -2, -4, -4, 4, 6, 1)
    m.part("head", "neck", (0, -7, 0), (-0.7, 0, 0))
    B("head", "skull", -3.5, -3.5, -6, 7, 7, 7)
    B("head", "muzzle", -2.5, -1.5, -11, 5, 5, 5)
    B("head", "nose", -3, -1, -12, 6, 4, 1)
    B("head", "chin", -2, 3.5, -10, 4, 1, 4)
    ears(m, (3, -2.5, 0.5), (0, 0, 1.0), (-1.5, -5, -0.5, 3, 5, 1))
    legs(m, 2.8, -7, 7, 11,
         [("u", "upper", (-2, -2, -2, 4, 8, 4)), ("l", "cannon", (-1, 0, -1, 2, 5, 2)), ("l", "hoof", (-2, 5, -2.6, 4, 2, 4))],
         [("u", "thigh", (-2, -2, -3, 4, 8, 6)), ("l", "cannon", (-1, 0, -1, 2, 5, 2)), ("l", "hoof", (-2, 5, -2.6, 4, 2, 4))],
         6, 1)
    m.eye = (-1.5, -4, 1.0)
    return m


REIN = dict(coat=(126, 100, 78), dark=(80, 62, 50), light=(160, 136, 112), under=(200, 188, 168), spot=None,
            dorsal=(88, 70, 56), muzzle=(176, 160, 138), nose=(46, 38, 34), chin=(206, 196, 180), ear_in=(176, 160, 140),
            ear_rim=(58, 46, 38), hoof=(52, 46, 42), leg=(70, 56, 46), rump=(214, 206, 192), eye=(22, 16, 14), spots=0,
            face=(84, 66, 54), band=(212, 204, 190))


# ---------------------------------------------------------------- aurochs calf (AurochsCalfModel)
def aurochs_calf():
    m = Model("Aurochs calf", "aurochsCalf", 128, 128)
    B = m.box
    m.part("body", at=(0, 5, 0))
    B("body", "chest", -6, -6, -11, 12, 12, 9)
    B("body", "barrel", -5.5, -5.5, -2, 11, 11, 6)
    B("body", "rump", -5.5, -6, 4, 11, 11, 7)
    B("body", "withers", -4, -7, -10, 8, 1, 7)
    B("body", "brisket", -4, 0, -12, 8, 7, 1)
    m.part("tail", "body", (0, -5, 10.6), (0.15, 0, 0))
    B("tail", "tail", -1, 0, -1, 2, 10, 2)
    B("tail", "tuft", -1.5, 9, -1.5, 3, 3, 3)
    m.part("neck", "body", (0, -2, -10), (-0.2, 0, 0))
    B("neck", "neck", -3.5, -4.5, -7, 7, 9, 9)
    B("neck", "dewlap", -1.5, 4, -6, 3, 2, 6)
    m.part("head", "neck", (0, -1, -7), (0.6, 0, 0))
    B("head", "skull", -4, -5, -7, 8, 7, 7)
    B("head", "poll", -3, -6, -6, 6, 1, 4)
    B("head", "cheek", -3.5, 2, -6.5, 7, 2, 5)
    B("head", "muzzle", -3, -2.5, -11, 6, 6, 4)
    B("head", "nose", -3.5, -2, -12, 7, 5, 1)
    B("head", "chin", -2.5, 3.5, -11, 5, 1, 4)
    ears(m, (4.5, -3, -2), (0, 0, 0.5), (-2.5, -1, -1.5, 5, 2, 3))
    legs(m, 3.6, -7, 7, 10,
         [("u", "upper", (-2.5, -2, -2.5, 5, 8, 5)), ("l", "knee", (-2, -1, -2, 4, 2, 4)),
          ("l", "cannon", (-1.5, 1, -1.5, 3, 6, 3)), ("l", "hoof", (-2, 7, -2.4, 4, 1, 4))],
         [("u", "thigh", (-2.5, -2, -3.5, 5, 8, 7)), ("l", "hock", (-2, -1, -2, 4, 2, 4)),
          ("l", "cannon", (-1.5, 1, -1.5, 3, 6, 3)), ("l", "hoof", (-2, 7, -2.4, 4, 1, 4))],
         6, 0)
    m.eye = (-2, -5, 1.0)
    return m


AUR = dict(coat=(156, 86, 46), dark=(106, 54, 30), light=(190, 120, 70), under=(176, 126, 86), spot=None,
           dorsal=(186, 134, 84), muzzle=(222, 208, 184), nose=(116, 84, 80), chin=(214, 198, 172), ear_in=(196, 146, 116),
           ear_rim=(96, 50, 28), hoof=(46, 36, 30), leg=(128, 70, 40), rump=None, eye=(24, 16, 12), spots=0)


# ---------------------------------------------------------------- mouflon lamb (MouflonLambModel)
def mouflon_lamb():
    m = Model("Mouflon lamb", "mouflonLamb", 128, 128)
    B = m.box
    m.part("body", at=(0, 8, 0))
    B("body", "chest", -4, -4, -8, 8, 8, 7)
    B("body", "barrel", -4, -4, -1, 8, 8, 4)
    B("body", "rump", -4, -4.5, 3, 8, 8, 5)
    B("body", "back", -3, -5, -6, 6, 1, 12)
    B("body", "brisket", -3, -1, -9, 6, 5, 1)
    m.part("tail", "body", (0, -3.4, 7.6), (0.3, 0, 0))
    B("tail", "tail", -1, 0, -1, 2, 4, 2)
    m.part("neck", "body", (0, -1, -7), (0.5, 0, 0))
    B("neck", "neck", -2, -6, -2, 4, 8, 4)
    m.part("head", "neck", (0, -5, 0), (-0.2, 0, 0))
    B("head", "skull", -3, -3, -5, 6, 6, 6)
    B("head", "brow", -2.5, -3.5, -4, 5, 1, 4)
    B("head", "muzzle", -2, -1, -9, 4, 4, 4)
    B("head", "nose", -1.5, -1, -9.6, 3, 2, 1)
    ears(m, (3.5, -2, -0.5), (0, 0.3, 0.2), (-1.5, -1, -1, 4, 2, 2))
    legs(m, 2.2, -5.2, 5.2, 10,
         [("u", "upper", (-2, -2, -2, 4, 6, 4)), ("l", "cannon", (-1, 0, -1, 2, 9, 2)), ("l", "hoof", (-1, 9, -1.4, 2, 1, 2))],
         [("u", "thigh", (-2, -2, -3, 4, 6, 6)), ("l", "cannon", (-1, 0, -1, 2, 9, 2)), ("l", "hoof", (-1, 9, -1.4, 2, 1, 2))],
         4, 0.6)
    m.eye = (-1, -2.2, 1.0)
    return m


LAMB = dict(coat=(166, 120, 78), dark=(120, 82, 50), light=(196, 156, 110), under=(232, 222, 204), spot=None,
            dorsal=(132, 92, 58), muzzle=(228, 220, 204), nose=(64, 50, 44), chin=(232, 224, 210), ear_in=(208, 178, 150),
            ear_rim=(92, 64, 42), hoof=(46, 38, 32), leg=(226, 218, 204), rump=(230, 220, 202), eye=(150, 108, 40),
            spots=0, flank=(96, 66, 42))


# ---------------------------------------------------------------- wild horse foal (WildHorseFoalModel)
def foal():
    m = Model("Wild horse foal", "wildHorseFoal", 128, 128)
    B = m.box
    m.part("body", at=(0, 1, 0))
    B("body", "chest", -5, -5, -10, 10, 11, 8)
    B("body", "barrel", -4.5, -4.5, -2, 9, 10, 5)
    B("body", "rump", -5, -5.5, 3, 10, 10, 7)
    B("body", "brisket", -3.5, -1, -11, 7, 6, 1)
    m.part("tail", "body", (0, -4, 9.6), (0.5, 0, 0))
    B("tail", "tail", -1.5, 0, -1, 3, 9, 2)
    m.part("neck", "body", (0, -2, -8), (0.45, 0, 0))
    B("neck", "neck", -2.5, -12, -3, 5, 14, 6)
    B("neck", "mane", -1, -13, 2.5, 2, 12, 2)
    m.part("head", "neck", (0, -11, 0), (0.3, 0, 0))
    B("head", "skull", -3.5, -4, -6.5, 7, 7, 7)
    B("head", "forelock", -1.5, -5, -4, 3, 1, 3)
    B("head", "muzzle", -2.5, -2.5, -13, 5, 6, 7)
    B("head", "nose", -2.5, -2, -14, 5, 5, 1)
    B("head", "chin", -2, 3.5, -10, 4, 1, 4)
    ears(m, (2.2, -3.5, -1), (-0.7, 0, 0.25), (-1, -5, -0.5, 2, 5, 1))
    legs(m, 3, -7, 7, 4,
         [("u", "upper", (-2, -2, -2, 4, 10, 4)), ("l", "knee", (-1.5, -1, -1.5, 3, 2, 3)),
          ("l", "cannon", (-1, 1, -1, 2, 9, 2)), ("l", "hoof", (-1.5, 10, -1.6, 3, 2, 3))],
         [("u", "thigh", (-2, -2, -3, 4, 10, 6)), ("l", "hock", (-1.5, -1, -1.5, 3, 2, 3)),
          ("l", "cannon", (-1, 1, -1, 2, 9, 2)), ("l", "hoof", (-1.5, 10, -1.6, 3, 2, 3))],
         8, 0.6)
    m.eye = (-2, -4.5, 1.0)
    return m


FOAL = dict(coat=(200, 164, 114), dark=(154, 120, 80), light=(226, 198, 152), under=(232, 216, 184), spot=None,
            dorsal=(116, 84, 54), muzzle=(234, 220, 192), nose=(78, 62, 52), chin=(230, 216, 190), ear_in=(214, 190, 156),
            ear_rim=(80, 58, 40), hoof=(50, 42, 36), leg=(96, 72, 52), rump=None, eye=(24, 18, 14), spots=0,
            mane=(82, 58, 38), mane_tip=(176, 140, 96))


# ---------------------------------------------------------------- piglet (PigletModel)
def piglet():
    m = Model("Wild boar piglet", "piglet", 128, 128)
    B = m.box
    m.part("body", at=(0, 14, 0))
    B("body", "barrel", -5.5, -4.5, -8, 11, 9, 16)
    B("body", "back", -4.5, -5.5, -7, 9, 1, 14)
    B("body", "belly", -4.5, 4.5, -6, 9, 1, 12)
    B("body", "chest", -4.5, -3.5, -9, 9, 7, 1)
    B("body", "rump", -4.5, -3.5, 8, 9, 7, 1)
    m.part("tail", "body", (0, -2, 9), (0.3, 0, 0))
    B("tail", "tail", -0.5, 0, -0.5, 1, 5, 1)
    m.part("head", "body", (0, -1, -8), (0.1, 0, 0))
    B("head", "skull", -4, -4.5, -6, 8, 8, 6)
    B("head", "cheek", -4.5, -0.5, -5, 9, 4, 4)
    B("head", "brow", -3, -5, -5, 6, 1, 4)
    m.part("snout", "head", (0, 1, -6))
    B("snout", "muzzle", -2.5, -2, -4, 5, 4, 4)
    B("snout", "nose", -2.5, -2, -5, 5, 4, 1)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 3, -4, -2.4), (-0.25, 0, s * 0.6))
        B(n, "ear", -1.5, -4, -0.5, 3, 4, 1)
    for s, side in ((-1, "right"), (1, "left")):
        for leg, z in (("front", -5), ("hind", 6)):
            l = m.part(f"{side}_{leg}_leg", at=(s * 3.6, 18, z))
            B(l, "upper" if leg == "front" else "thigh", -2, -1, -2, 4, 5, 4)
            B(l, "cannon", -1.5, 4, -1.5, 3, 1, 3)
            B(l, "hoof", -1.5, 5, -1.7, 3, 1, 3)
    m.eye = (-2, -3.5, 0.5)
    return m


PIG = dict(coat=(122, 84, 52), dark=(76, 50, 32), light=(152, 110, 70), under=(176, 140, 100), spot=None,
           stripe=(214, 184, 130), stripe_dark=(70, 46, 30), muzzle=(104, 78, 62), nose=(112, 86, 80), chin=(150, 116, 90),
           ear_in=(168, 128, 104), ear_rim=(60, 40, 26), hoof=(40, 30, 24), leg=(84, 58, 40), rump=None, eye=(20, 14, 10),
           dorsal=None, spots=0)


# ---------------------------------------------------------------- painting
def colour_for(m, P, kind):
    body_boxes = [b for b in m.boxes if b["mat"] in BODY]
    top = min(m.matrix(b["part"])[1][1] + b["y"] for b in body_boxes)
    bottom = max(m.matrix(b["part"])[1][1] + b["y"] + b["h"] for b in body_boxes)
    ey, ez, er = m.eye

    def coat(T, amount=0.8):
        c = strands(P["coat"], P["dark"], P["light"], T, amount)
        t = smooth((T.p[1] - top) / (bottom - top))
        c = lerp(c, P["light"], 0.3 * (1 - t))  # sunlit back
        c = lerp(c, P["dark"], 0.3 * t)
        mott = cells(T.p, 3.0, 11)[1]
        c = lerp(c, P["dark"] if mott < 0.5 else P["light"], 0.12 * abs(mott - 0.5) * 2)
        if T.edge_bottom:
            c = shade(c, 0.86)
        return c

    def colour(T):
        mat, p, face = T.mat, T.p, T.face
        if mat == "hoof":
            c = P["hoof"]
            if face == "top":
                return lerp(c, P["leg"], 0.4)
            return shade(c, 0.85) if face == "bottom" else lerp(c, (120, 110, 100), 0.12 * (T.j == 0))
        c = coat(T)
        # ---- body: belly, flank line, spots, dorsal stripe, rump patch
        if mat in BODY or mat in ("neck", "throat", "ruff", "dewlap"):
            t = smooth((p[1] - (top + bottom) / 2) / ((bottom - top) / 2))
            if face == "bottom":
                c = lerp(c, P["under"], 0.85)
            else:
                c = lerp(c, P["under"], (0.75 if kind in ("lamb", "fawn", "rein", "foal") else 0.35) * smooth((t - 0.45) / 0.55))
            if kind == "lamb" and face != "bottom" and abs(t - 0.62) < 0.12 and mat in BODY:
                c = lerp(c, P["flank"], 0.7)  # the dark line between the brown flank and the white belly
            if P.get("dorsal") and face == "top" and abs(p[0]) < 1.1 and mat in BODY | {"neck"}:
                c = lerp(c, P["dorsal"], 0.7)
            if P.get("rump") and mat == "rump" and (face == "back" or (face in ("left", "right") and T.rel()[2] > 0.7)):
                if T.rel()[1] < 0.75:
                    c = lerp(c, P["rump"], 0.8 if face == "back" else 0.45)
            if P["spots"] and mat in ("chest", "loin", "rump") and face != "bottom":
                d, v = cells(p, 2.2, 7)
                band = smooth(1 - abs(p[1] - (top + 3.0)) / 4.0)  # rows along the upper flank
                if abs(p[0]) > 0.9 and d < 0.85 * P["spots"] and band > 0.2 and v > 0.15:
                    c = lerp(c, P["spot"], 0.9)
            if kind == "pig":
                c = pig_stripes(T, c, top)
            if kind == "foal" and mat in ("chest",) and face != "bottom":
                pass
        if mat in ("neck", "throat", "ruff") and kind == "rein":
            c = lerp(c, P["light"], 0.25)
        if mat == "dewlap":
            c = lerp(c, P["dark"], 0.2)
        if mat in ("mane", "forelock"):
            if "mane" not in P:
                return lerp(c, P["dark"], 0.45)
            return lerp(P["mane"], P["mane_tip"], 0.35 * (T.j == 0) + 0.25 * h(T.i, T.j, T.k + 3))
        if mat in ("tail", "tuft"):
            if kind == "foal":
                return lerp(P["mane"], P["mane_tip"], 0.3 * h(T.i, T.j, 4))
            if kind == "aur" and mat == "tuft":
                return lerp(P["dark"], (40, 26, 18), 0.6)
            if kind in ("fawn", "rein", "lamb") and face in ("back", "bottom"):
                return P["rump"]
            if kind == "lamb" and T.rel()[1] > 0.7:
                return P["ear_rim"]
            return lerp(c, P["dark"], 0.2)
        # ---- legs: darker / paler towards the hoof
        if mat in LEG:
            t = smooth((p[1] - (bottom - 2)) / (24 - (bottom - 2)))
            c = lerp(c, P["leg"], 0.85 * t)
            if kind == "rein" and mat == "cannon" and T.side and T.j == T.fh - 1:
                c = P["band"]
            if kind == "foal" and mat == "cannon" and T.side and T.j % 3 == 0 and T.j < 5:
                c = lerp(c, (60, 44, 32), 0.35)  # faint primitive leg bars
            if kind in ("fawn", "lamb") and mat in ("thigh", "upper") and face in ("back", "right", "left") and T.rel()[2] > 0.6:
                c = lerp(c, P["under"], 0.25 * smooth(t * 2))
            if kind == "lamb" and mat in ("knee", "cannon") and T.side and T.j < 2:
                c = lerp(c, P["ear_rim"], 0.6)  # a dark band above the white lower leg
            if face == "bottom" and mat in ("upper", "thigh"):
                c = shade(c, 0.85)
            return c
        # ---- head
        if mat == "ear":
            inner = face == "front" if kind != "aur" and kind != "lamb" else face == "top" or face == "front"
            if inner and 0 < T.i < T.fw - 1 and T.j > 0:
                return lerp(P["ear_in"], c, 0.25 + 0.2 * h(T.i, T.j, 9))
            if T.side and T.j == 0 or (T.side and T.i in (0, T.fw - 1)):
                return lerp(c, P["ear_rim"], 0.65)
            return c
        if mat in ("skull", "brow", "cheek", "poll"):
            if kind == "rein":
                c = lerp(c, P["face"], 0.6)
            if kind == "lamb":
                c = lerp(c, P["light"], 0.2)
            if mat == "skull" and face in ("left", "right"):
                ly, lz = T.lp[1], T.lp[2]
                dy, dz = ly - ey, lz - ez
                if abs(dy) < er and abs(dz) < er:
                    glint = dy < 0 and dz > 0
                    return (215, 210, 200) if glint else (P["eye"] if er > 0.5 and dy < 0 else (14, 10, 8))
                if abs(dy + er + 0.5) < 0.5 and abs(dz) < er + 0.5:
                    return lerp(c, (20, 14, 10), 0.6)  # the upper lid
                if kind == "lamb" and abs(dy) < er + 1 and abs(dz) < er + 1:
                    return lerp(c, P["chin"], 0.3)  # pale eye ring
                if kind == "rein" and lz < ez - er and ly > ey - 1:
                    return lerp(c, P["face"], 0.5)
            if face == "front" and mat == "skull" and kind == "aur":
                if (h(int(p[0] * 2), int(p[1] * 2), 21) > 0.6) and p[1] < 0:
                    c = lerp(c, P["light"], 0.4)  # curly forelock
            return c
        if mat in ("muzzle", "nose", "chin"):
            mc = P["muzzle"]
            if mat == "chin":
                return lerp(P["chin"], c, 0.15)
            if mat == "nose" and face == "front":
                if kind == "pig":
                    cx, cy = T.i - (T.fw - 1) / 2, T.j
                    if abs(abs(cx) - 1) < 0.6 and 1 <= cy <= 2:
                        return (40, 26, 24)
                    return lerp(P["nose"], (190, 150, 140), 0.15 * (T.j == 0))
                if kind in ("fawn", "lamb"):
                    if T.j < T.fh and abs(T.i - (T.fw - 1) / 2) >= 0.5 and T.j == 1 and kind == "fawn":
                        return (12, 10, 9)
                    return P["nose"] if kind == "fawn" else lerp(P["nose"], mc, 0.35)
                if kind == "aur" and (T.i in (0, T.fw - 1) or T.j in (0, T.fh - 1)):
                    return lerp(mc, P["coat"], 0.1 * h(T.i, T.j, 5))  # the mealy ring round the dark nose
                nostril = T.j == 1 and T.i in (0, T.fw - 1)
                return (30, 22, 18) if nostril else lerp(P["nose"], mc, 0.3 if kind in ("aur", "foal", "rein") else 0.55 if kind == "moose" else 0.1)
            if mat == "nose":
                if kind == "pig":
                    return lerp(P["muzzle"], P["nose"], 0.5)
                if kind == "fawn" and face == "top":
                    return P["nose"]
                c = lerp(c, mc, 0.8)
            else:
                k = T.rel()[2]  # 0 = nose end
                c = lerp(c, mc, 0.85 * (1 - k) + 0.25 if kind in ("aur", "lamb", "foal", "rein") else 0.55 * (1 - k))
                if kind == "fawn" and face in ("left", "right") and k < 0.45 and T.j == T.fh - 1:
                    c = lerp(c, P["chin"], 0.6)  # pale upper lip
                if kind == "fawn" and face == "top" and k < 0.35:
                    c = lerp(c, P["nose"], 0.7)
            if face in ("left", "right") and T.j == T.fh - 1 and mat in ("muzzle", "nose"):
                c = lerp(c, (30, 22, 18), 0.35)  # mouth line
            return c
        return c

    return colour


def pig_stripes(T, c, top):
    """The humbug's cream stripes, running nose to tail along the back and flanks."""
    p, face = T.p, T.face
    if face == "bottom":
        return c
    u = abs(p[0]) if face == "top" else 5.5 + max(0.0, p[1] - (top + 1))
    u += 0.4 * math.sin(p[2] * 0.45)
    band = int(u / 1.0) % 4
    if band == 1 and u < 13:
        return lerp(c, (218, 190, 136), 0.8)
    if band == 3 and u < 13:
        return lerp(c, (62, 40, 26), 0.6)
    return c


# ---------------------------------------------------------------- main
def main():
    models = [
        (fawn(), [(ROE, "fawn", "roe_deer_fawn"), (RED, "fawn", "red_deer_calf")]),
        (moose_calf(), [(MOOSE, "moose", "moose_calf")]),
        (reindeer_calf(), [(REIN, "rein", "reindeer_calf")]),
        (aurochs_calf(), [(AUR, "aur", "aurochs_calf")]),
        (mouflon_lamb(), [(LAMB, "lamb", "mouflon_lamb")]),
        (foal(), [(FOAL, "foal", "wild_horse_foal")]),
        (piglet(), [(PIG, "pig", "wild_boar_piglet")]),
    ]
    for m, skins in models:
        try:  # 128x64 when the boxes fit, else 128x128
            m.th = 64
            m.pack()
        except AssertionError:
            m.th = 128
            m.pack()
        check_ground(m)
        for P, kind, name in skins:
            im = m.paint(colour_for(m, P, kind))
            save(im, f"wildlife/{name}.png")
            save(cm.skinned(m, im, ("hoof",)), f"skinned/wildlife/{name}.png")
    body = "\n".join(m.java() for m, _ in models)
    for old, new in cm.LOWER.items():
        body = body.replace(f'"{old}"', f'"{new}"')
    JAVA.write_text(f"""package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_herd_models.py (with the skins it paints for them): the herd's young, built at twice their size. Do not edit. */
public final class HerdGeometry {{
{body}
    private HerdGeometry() {{}}
}}
""", encoding="utf-8")
    for m, _ in models:
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes")


if __name__ == "__main__":
    main()
