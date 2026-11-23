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

BODY = {"chest", "barrel", "loin", "rump", "withers", "brisket", "back", "belly", "pins"}
LEG = {"upper", "thigh", "knee", "hock", "cannon", "pastern"}


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
    m = Model("Moose calf", "mooseCalf", 128, 64)
    B = m.box
    # the body tilts nose-up a little (rest pose kept, MooseCalfModel never sets body.xRot): high withers, low rump
    m.part("body", at=(0, 0, 0), rot=(-0.07, 0, 0))
    B("body", "chest", -5.5, -8.5, -12, 11, 15, 8)
    B("body", "withers", -3.5, -10, -11, 7, 2, 7)
    B("body", "barrel", -5, -7.5, -4, 10, 14, 8)
    B("body", "belly", -4, 6.5, -6, 8, 1, 9)
    B("body", "rump", -4.5, -6.5, 4, 9, 12, 7)
    B("body", "back", -3, -7, 3, 6, 1, 8)
    B("body", "brisket", -4, -2, -13, 8, 8, 1)
    m.part("tail", "body", (0, -5.5, 11), (0.3, 0, 0))
    B("tail", "tail", -1, 0, 0, 2, 4, 2)
    m.part("neck", "body", (0, -4, -11), (0.9, 0, 0))
    B("neck", "neck", -2.5, -10, -3.5, 5, 12, 7)
    B("neck", "throat", -2, -7, -4.5, 4, 7, 1)
    B("neck", "mane", -1, -10, 3, 2, 10, 1)
    m.part("head", "neck", (0, -9, 0), (-0.55, 0, 0))
    B("head", "skull", -3, -4, -6, 6, 7, 7)
    B("head", "brow", -2.5, -4.5, -5.5, 5, 1, 5)
    B("head", "muzzle", -2.5, -3, -13, 5, 6, 7)
    B("head", "bridge", -2, -3.5, -12, 4, 1, 6)
    # the long drooping nose: the upper lip hangs out over the chin
    B("head", "nose", -3, -2.5, -16, 6, 7, 3)
    B("head", "chin", -2, 3, -13, 4, 1, 6)
    ears(m, (2.6, -3, 0), (-0.2, 0, 0.9), (-1.5, -8, -0.5, 3, 8, 1))
    legs(m, 3.2, -8, 8, 4,
         [("u", "upper", (-2, -2, -2, 4, 11, 4)), ("l", "knee", (-2, -1.5, -2, 4, 3, 4)),
          ("l", "cannon", (-1, 1.5, -1, 2, 7, 2)), ("l", "pastern", (-1.5, 8, -1.5, 3, 1, 3)), ("l", "hoof", (-1.5, 9, -2, 3, 1, 3))],
         [("u", "thigh", (-2, -2, -3.5, 4, 11, 7)), ("l", "hock", (-2, -1.5, -2, 4, 3, 4)),
          ("l", "cannon", (-1, 1.5, -1, 2, 7, 2)), ("l", "pastern", (-1.5, 8, -1.5, 3, 1, 3)), ("l", "hoof", (-1.5, 9, -2, 3, 1, 3))],
         10, 1)
    m.eye = (-2, -4.2, 1.0)
    return m


MOOSE = dict(coat=(150, 84, 44), dark=(82, 44, 24), light=(198, 130, 76), under=(104, 66, 42), spot=None,
             dorsal=(92, 50, 28), muzzle=(84, 52, 36), nose=(44, 30, 24), chin=(118, 82, 58), ear_in=(168, 128, 100),
             ear_rim=(62, 36, 22), hoof=(36, 28, 24), leg=(78, 54, 40), rump=None, eye=(26, 18, 14), spots=0,
             face=(104, 60, 36), fur=1.4)


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
    m = Model("Aurochs calf", "aurochsCalf", 128, 64)
    B = m.box
    m.part("body", at=(0, 5, 0))
    B("body", "chest", -6, -6, -11, 12, 13, 8)
    B("body", "withers", -4, -7, -10, 8, 1, 7)
    B("body", "brisket", -4, 1, -12, 8, 6, 1)
    B("body", "barrel", -5.5, -5.5, -3, 11, 12, 7)
    B("body", "belly", -4.5, 6.5, -3, 9, 1, 6)
    B("body", "rump", -5, -6, 4, 10, 10, 6)
    B("body", "back", -3.5, -6.5, 3, 7, 1, 6)
    B("body", "pins", -4, -5, 10, 8, 7, 1)
    m.part("tail", "body", (0, -5, 10.6), (0.15, 0, 0))
    B("tail", "tail", -1, 0, -1, 2, 10, 2)
    B("tail", "tuft", -1.5, 9, -1.5, 3, 3, 3)
    m.part("neck", "body", (0, -2, -10), (-0.2, 0, 0))
    B("neck", "neck", -3.5, -4.5, -7, 7, 9, 9)
    B("neck", "dewlap", -1.5, 4, -6, 3, 2, 6)
    m.part("head", "neck", (0, -1, -7), (0.6, 0, 0))
    # the head tapers: broad poll and cheeks, narrower face, a wide wet nose
    B("head", "skull", -4, -5, -6, 8, 7, 6)
    B("head", "poll", -3, -6, -5, 6, 1, 4)
    B("head", "cheek", -3.5, 2, -5.5, 7, 2, 4)
    B("head", "face", -3, -4.5, -9, 6, 6, 3)
    B("head", "muzzle", -2.5, -2.5, -12, 5, 5, 3)
    B("head", "nose", -3, -2, -13, 6, 4, 1)
    B("head", "chin", -2, 2.5, -11.5, 4, 1, 4)
    ears(m, (4.5, -3, -2), (0, 0, 0.5), (-2.5, -1, -1.5, 5, 2, 3))
    legs(m, 3.6, -7, 7, 10,
         [("u", "upper", (-2.5, -2, -2.5, 5, 8, 5)), ("l", "knee", (-2, -1, -2, 4, 2, 4)),
          ("l", "cannon", (-1.5, 1, -1.5, 3, 6, 3)), ("l", "hoof", (-2, 7, -2.4, 4, 1, 4))],
         [("u", "thigh", (-2.5, -2, -3.5, 5, 8, 7)), ("l", "hock", (-2, -1, -2, 4, 2, 4)),
          ("l", "cannon", (-1.5, 1, -1.5, 3, 6, 3)), ("l", "hoof", (-2, 7, -2.4, 4, 1, 4))],
         6, 0)
    m.eye = (-2, -4, 1.0)
    return m


AUR = dict(coat=(156, 86, 46), dark=(106, 54, 30), light=(190, 120, 70), under=(176, 126, 86), spot=None,
           dorsal=(186, 134, 84), muzzle=(222, 208, 184), nose=(116, 84, 80), chin=(214, 198, 172), ear_in=(196, 146, 116),
           ear_rim=(96, 50, 28), hoof=(46, 36, 30), leg=(128, 70, 40), rump=None, eye=(24, 16, 12), spots=0)


# ---------------------------------------------------------------- mouflon lamb (MouflonLambModel)
def mouflon_lamb():
    m = Model("Mouflon lamb", "mouflonLamb", 128, 64)
    B = m.box
    m.part("body", at=(0, 8, 0))
    # rounded: a fuller barrel between a narrower chest and rump, every edge rounded off with a rim box
    B("body", "chest", -3.5, -3.5, -8, 7, 7, 6)
    B("body", "barrel", -4.5, -4, -2, 9, 8, 6)
    B("body", "rump", -4, -4, 4, 8, 7, 4)
    B("body", "back", -3.5, -5, -7, 7, 1, 14)
    B("body", "belly", -3.5, 4, -6, 7, 1, 10)
    B("body", "brisket", -2.5, -2.5, -9, 5, 5, 1)
    B("body", "pins", -3, -3, 8, 6, 5, 1)
    m.part("tail", "body", (0, -3.4, 8.6), (0.3, 0, 0))
    B("tail", "tail", -1, 0, -1, 2, 4, 2)
    m.part("neck", "body", (0, -1, -7), (0.5, 0, 0))
    B("neck", "neck", -2, -6, -2, 4, 8, 4)
    m.part("head", "neck", (0, -5, 0), (-0.2, 0, 0))
    # a narrow, wedge-shaped face
    B("head", "skull", -2.5, -3, -5, 5, 6, 6)
    B("head", "brow", -2, -3.5, -4, 4, 1, 4)
    B("head", "muzzle", -1.5, -1.5, -9, 3, 4, 4)
    B("head", "nose", -1.5, -1, -9.6, 3, 2, 1)
    B("head", "chin", -1, 2.5, -8, 2, 1, 3)
    ears(m, (3, -2, -0.5), (0, 0.3, 0.2), (-1.5, -1, -1, 4, 2, 2))
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
    m = Model("Wild boar piglet", "piglet", 128, 64)
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
    B("head", "skull", -4, -4.5, -5, 8, 8, 5)
    B("head", "cheek", -4.5, -0.5, -4, 9, 4, 3)
    B("head", "brow", -3, -5, -4.5, 6, 1, 4)
    B("head", "face", -3, -3.5, -7, 6, 6, 2)
    # the wedge: each step of the snout lower and narrower towards the disc
    m.part("snout", "head", (0, 1, -6))
    B("snout", "muzzle", -2.5, -3, -3, 5, 5, 2)
    B("snout", "snout_tip", -2.5, -2, -5, 5, 4, 2)
    B("snout", "nose", -2.5, -2, -6, 5, 4, 1)
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



# ---------------------------------------------------------------- roe deer (RoeDeerModel, adult, 1x)
def roe_deer():
    m = Model("Roe deer", "roeDeer", 128, 64)
    B = m.box
    m.part("body", at=(0, 13, 0))
    B("body", "chest", -3, -3, -7, 6, 6, 6)
    B("body", "barrel", -3, -2.5, -1, 6, 5, 3)
    B("body", "rump", -3, -3.5, 2, 6, 6, 4)
    B("body", "brisket", -2, -1, -8, 4, 4, 1)
    m.part("tail", "body", (0, -2, 6), (0.3, 0, 0))
    B("tail", "tail", -1, 0, 0, 2, 2, 1)
    m.part("neck", "body", (0, -1, -5), (0.35, 0, 0))
    B("neck", "neck", -1.5, -6, -2, 3, 6, 4)
    B("neck", "throat", -1, -5, -2.5, 2, 4, 1)
    m.part("head", "neck", (0, -5, 0), (-0.35, 0, 0))
    B("head", "skull", -2, -3, -3, 4, 4, 5)
    B("head", "brow", -1.5, -3.5, -2.5, 3, 1, 3)
    B("head", "muzzle", -1.5, -1.5, -6, 3, 3, 3)
    B("head", "nose", -1, -1.5, -7, 2, 2, 1)
    B("head", "chin", -1, 1, -5.5, 2, 1, 2)
    ears(m, (1.5, -2.5, 1), (-0.2, -0.5, 0.55), (-1.5, -4, -0.5, 3, 4, 1))
    for s, side in ((-1, "right"), (1, "left")):
        f = m.part(f"{side}_front_leg", at=(s * 1.8, 14, -4.5))
        B(f, "upper", -1.5, -1, -1.5, 3, 5, 3)
        B(f, "cannon", -1, 4, -1, 2, 5, 2)
        B(f, "hoof", -1, 9, -1.2, 2, 1, 2)
        hl = m.part(f"{side}_hind_leg", at=(s * 1.8, 13, 4))
        B(hl, "thigh", -1.5, -1, -2, 3, 6, 4)
        B(hl, "cannon", -1, 5, -0.5, 2, 5, 2)
        B(hl, "hoof", -1, 10, -0.7, 2, 1, 2)
    m.eye = (-1.5, -1, 0.5)
    return m


ROEA = dict(coat=(166, 92, 46), dark=(112, 60, 30), light=(196, 124, 70), under=(214, 178, 140), spot=None,
            dorsal=(128, 70, 36), muzzle=(118, 96, 82), nose=(22, 18, 16), chin=(234, 226, 212), ear_in=(204, 178, 150),
            ear_rim=(46, 34, 28), hoof=(36, 30, 26), leg=(126, 88, 62), rump=(238, 230, 214), eye=(30, 20, 16), spots=0,
            face=(132, 104, 84))


# ---------------------------------------------------------------- mouflon ram and ewe (MouflonModel, adult, 1x)
def mouflon():
    m = Model("Mouflon", "mouflon", 128, 64)
    B = m.box
    m.part("body", at=(0, 11.5, 0))
    B("body", "chest", -3.5, -3.5, -7, 7, 7, 6)
    B("body", "barrel", -4, -3.5, -1, 8, 7, 4)
    B("body", "rump", -3.5, -3.5, 3, 7, 7, 4)
    B("body", "back", -2.5, -4.5, -6, 5, 1, 12)
    B("body", "belly", -2.5, 3.5, -5, 5, 1, 9)
    B("body", "brisket", -2.5, -1.5, -8, 5, 5, 1)
    B("body", "pins", -2.5, -2.5, 7, 5, 5, 1)
    m.part("tail", "body", (0, -3, 7.8), (0.3, 0, 0))
    B("tail", "tail", -1, 0, -0.5, 2, 3, 1)
    m.part("neck", "body", (0, -1.5, -6), (0.55, 0, 0))
    B("neck", "neck", -1.5, -6, -2, 3, 7, 4)
    B("neck", "ruff", -1.5, -3, -2.6, 3, 5, 1)
    m.part("head", "neck", (0, -5.5, 0), (-0.1, 0, 0))
    B("head", "skull", -2, -2, -4, 4, 4, 5)
    B("head", "muzzle", -1.5, -1, -7, 3, 3, 3)
    B("head", "nose", -1, -0.5, -7.5, 2, 2, 1)
    ears(m, (3, -1.2, 0.3), (0, 0.3, -0.3), (-1.5, -0.5, -0.5, 3, 1, 1))
    m.part("ram_horns", "head")
    for s, p in ((1, "left_"), (-1, "right_")):
        b = m.part(p + "horn", "ram_horns", (1.3 * s, -1.6, -0.8), (-0.55, 0, 0.5 * s))
        B(b, "horn_big", -1.5, -3, -1.5, 3, 3, 3)
        b2 = m.part(p + "horn_2", b, (0, -2.5, 0), (-1.15, 0, 0.3 * s))
        B(b2, "horn_big", -1.5, -3, -1.5, 3, 3, 3)
        b3 = m.part(p + "horn_3", b2, (0, -2.5, 0), (-1.15, 0, 0.1 * s))
        B(b3, "horn_mid", -1, -3, -1, 2, 3, 2)
        b4 = m.part(p + "horn_tip", b3, (0, -2.5, 0), (-1.0, 0, -0.25 * s))
        B(b4, "horn_tip", -0.5, -3, -0.5, 1, 3, 1)
    m.part("ewe_horns", "head")
    for s, n in ((1, "left_horn_e"), (-1, "right_horn_e")):
        m.part(n, "ewe_horns", (1.1 * s, -1.8, -1), (-0.6, 0, 0.35 * s))
        B(n, "ewe_horn", -0.5, -2, -0.5, 1, 2, 1)
    legs(m, 2, -5, 5, 14,
         [("u", "upper", (-1.5, -1, -1.5, 3, 5, 3)), ("l", "cannon", (-1, 0, -1, 2, 5, 2)), ("l", "hoof", (-1, 5, -1.2, 2, 1, 2))],
         [("u", "thigh", (-1.5, -1, -2, 3, 5, 4)), ("l", "cannon", (-1, 0, -1, 2, 5, 2)), ("l", "hoof", (-1, 5, -1.2, 2, 1, 2))],
         4, 0.5)
    m.eye = (-0.5, -2.5, 0.5)
    return m


RAM = dict(coat=(142, 82, 42), dark=(84, 46, 24), light=(176, 112, 64), under=(234, 226, 210), spot=None,
           dorsal=(98, 56, 30), muzzle=(230, 222, 206), nose=(70, 56, 48), chin=(232, 224, 210), ear_in=(206, 176, 148),
           ear_rim=(70, 44, 28), hoof=(42, 36, 30), leg=(228, 222, 208), rump=(232, 224, 208), eye=(168, 120, 40),
           spots=0, flank=(46, 30, 20), saddle=(222, 214, 196), bib=(52, 34, 22), horn=(140, 124, 98))
EWE = dict(RAM, coat=(168, 120, 78), dark=(118, 82, 52), light=(196, 154, 108), dorsal=(132, 92, 58), flank=(90, 62, 40),
           saddle=None, bib=None, horn=(150, 134, 108))


# ---------------------------------------------------------------- painting
def colour_for(m, P, kind):
    body_boxes = [b for b in m.boxes if b["mat"] in BODY]
    top = min(m.matrix(b["part"])[1][1] + b["y"] for b in body_boxes)
    bottom = max(m.matrix(b["part"])[1][1] + b["y"] + b["h"] for b in body_boxes)
    ey, ez, er = m.eye

    def coat(T, amount=0.8):
        c = strands(P["coat"], P["dark"], P["light"], T, P.get("fur", amount))
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
        if mat.startswith("horn") or mat == "ewe_horn":
            base = {"horn_big": 0.0, "horn_mid": 0.45, "horn_tip": 0.75, "ewe_horn": 0.3}[mat]
            c = lerp(P["horn"], (210, 196, 168), base * 0.6 + 0.1 * h(T.i, T.j, T.k))
            if T.side and (T.j + T.k) % 2 == 0:
                c = shade(c, 0.82)  # the annual ridges
            return c
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
                c = lerp(c, P["under"], (0.75 if kind in ("lamb", "fawn", "rein", "foal", "roe", "mouf") else 0.35) * smooth((t - 0.45) / 0.55))
            if kind in ("lamb", "mouf") and face != "bottom" and abs(t - 0.62) < 0.12 and mat in BODY:
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
            if kind == "mouf" and P["saddle"] and face in ("left", "right") and mat == "barrel":
                e = ((p[2] - 1.2) / 2.6) ** 2 + ((p[1] - 11.2) / 2.3) ** 2 + 0.5 * (T.noise(41) - 0.5)
                if e < 1.25:
                    c = lerp(c, P["saddle"], 0.85 * smooth((1.25 - e) / 0.45))  # the ram's pale saddle patch
            if kind == "mouf" and P["bib"] and (mat in ("ruff", "brisket") or (mat == "neck" and face == "front")):
                c = lerp(c, P["bib"], 0.75)  # the dark ruff down the throat and chest
            if kind == "roe" and mat == "throat" and face == "front" and T.rel()[1] < 0.5:
                c = lerp(c, P["chin"], 0.6)  # the pale throat patch
            if kind == "lamb" and face != "bottom":
                d, v = cells(p, 1.3, 23)  # short fleece in little curls: bright tufts, darker gaps
                c = lerp(c, P["light"] if d < 0.45 else P["dark"], 0.35 * (1 - min(d, 0.9) / 0.9) if d < 0.45 else 0.2)
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
            if kind in ("fawn", "rein", "lamb", "roe", "mouf") and face in ("back", "bottom"):
                return P["rump"]
            if kind in ("lamb", "mouf") and (T.rel()[1] > 0.7 or (kind == "mouf" and face == "front")):
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
            if kind == "mouf" and mat in ("upper", "thigh") and face == "front":
                c = lerp(c, P["flank"], 0.6)  # dark stripe down the front of the leg
            if kind in ("lamb", "mouf") and mat in ("knee", "cannon") and T.side and T.j < 2:
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
        if mat in ("skull", "brow", "cheek", "poll", "face", "bridge"):
            if kind in ("rein", "roe"):
                c = lerp(c, P["face"], 0.6)
            if kind == "moose":
                c = lerp(c, P["face"], 0.45)
            if kind == "pig" and face == "top":
                band = int(abs(p[0]) / 1.0) % 4  # the humbug's stripes run on up the forehead
                c = lerp(c, (218, 190, 136), 0.75) if band == 1 else lerp(c, (62, 40, 26), 0.55) if band == 3 else c
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
        if mat in ("muzzle", "nose", "chin", "snout_tip"):
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
                if kind == "roe" and face in ("left", "right"):
                    if T.j == T.fh - 1 and k < 0.7:
                        c = P["chin"]  # the white lips
                    elif k < 0.4 and T.j >= T.fh - 2:
                        c = lerp(c, (30, 22, 18), 0.75)  # the black moustache behind the nose
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
        (roe_deer(), [(ROEA, "roe", "roe_deer")]),
        (mouflon(), [(RAM, "mouf", "mouflon_ram"), (EWE, "mouf", "mouflon_ewe")]),
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
            save(cm.skinned(m, im, ("hoof", "horn_big", "horn_mid", "horn_tip", "ewe_horn")), f"skinned/wildlife/{name}.png")
    body = "\n".join(m.java() for m, _ in models)
    for old, new in cm.LOWER.items():
        body = body.replace(f'"{old}"', f'"{new}"')
    body = body.replace('"left_horn_e"', '"left_horn"').replace('"right_horn_e"', '"right_horn"')
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
