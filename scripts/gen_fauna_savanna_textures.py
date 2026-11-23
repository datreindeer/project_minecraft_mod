"""All textures for the savanna fauna (wildlife/savanna/SavannaFauna.java).

Entities (textures/entity/wildlife/, their skinned carcass versions in textures/entity/skinned/wildlife/):
- spotted_hyena: the cave hyena's texture (CaveHyenaModel, gen_fauna_mountain_textures.HYENA) repainted warmer and
  sandier with bolder spots.
- cheetah (64x64, CheetahModel): golden tan, small solid black spots all over, white belly and throat, black tear marks
  from the eyes to the mouth, tail spots turning to black rings before the white tip.
- gazelle (64x32, GazelleModel): cinnamon back, a bold black flank stripe over the white belly, white rump with a black
  tail, white eye-ring and a dark stripe down the face, black ringed horns.
- ostrich_male / ostrich_female / ostrich_chick (64x64, OstrichModel): black cock with white wing plumes and tail and a
  pink bare neck and legs; grey-brown hen with dull pale plumes; buff, dark-streaked chick.
- aardvark (64x32, AardvarkModel): sparse grey-pink hide, darker legs, pale head, pink ears, dark claws.
Items (16x16): meats, hides, the cheetah pelt and cloak, Gazelle Horn, Signal Horn, Ostrich Feather and Egg, Aardvark
Claw, Termites, Chitin Paste, spawn eggs. Blocks: termite_mound (side) and termite_mound_top. Equipment: the Cheetah
Cloak's humanoid layer and cape (entity/cloak/cheetah_cloak.png).
The CHEETAH / GAZELLE / OSTRICH / AARDVARK tables mirror the cubes in client/wildlife/savanna/*Model.java.
Deterministic. Run: python scripts/gen_fauna_savanna_textures.py"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import TEX, fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import egg  # noqa: E402
import gen_fauna_mountain_textures as mt  # noqa: E402
from gen_fauna_warm_tex_items import hide_item, meat, raw_item, tube, put, CLEAR  # noqa: E402
from gen_fauna_warm_tex_items import LEOPARD_RAW, LEOPARD_COOKED, ONAGER_RAW, ONAGER_COOKED, BUFFALO_RAW, BUFFALO_COOKED  # noqa: E402
from gen_fauna_warm_tex_items import TAPIR_RAW, TAPIR_COOKED  # noqa: E402
from gen_fauna_coldhunters_textures import recolour_layer  # noqa: E402

ITEM, BLOCK = TEX.parent / "item", TEX.parent / "block"
TRANSPARENT = (0, 0, 0, 0)
EYE = (16, 12, 10)
BLACK = (28, 24, 22)


def spot(p, cell, radius, salt, chance=0.8):
    """Solid round spots on a jittered 3D grid (so they line up across faces)."""
    base = [math.floor(v / cell) for v in p]
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                k = (base[0] + dx, base[1] + dy, base[2] + dz)
                if h(k[0] * 31 + k[2], k[1], salt + 9) > chance:
                    continue
                c = [(k[a] + 0.2 + 0.6 * h(k[0] * 7 + a, k[1] * 13 + k[2] * 3, salt + a)) * cell for a in range(3)]
                if sum((p[a] - c[a]) ** 2 for a in range(3)) < radius * radius:
                    return True
    return False


# ====================================================================== spotted hyena (cave hyena repainted)
def spotted_hyena():
    mt.Y_COAT, mt.Y_LIGHT, mt.Y_BELLY = (184, 150, 98), (206, 176, 126), (222, 204, 166)
    mt.Y_SPOT, mt.Y_DARK, mt.Y_MUZZLE = (52, 36, 26), (42, 32, 26), (46, 36, 30)
    return paint(mt.HYENA, "spotted_hyena", (64, 64))


# ====================================================================== cheetah
C_BACK, C_GOLD, C_PALE, C_WHITE = (196, 150, 78), (216, 172, 96), (232, 206, 156), (240, 234, 220)
C_SPOT, C_EYE, C_NOSE = (26, 22, 18), (196, 150, 52), (40, 30, 28)
TONGUE, TOOTH = (176, 82, 86), (232, 226, 208)


def c_coat(t):
    if t < 0.5:
        return lerp(C_BACK, C_GOLD, t / 0.5)
    if t < 0.78:
        return lerp(C_GOLD, C_PALE, (t - 0.5) / 0.28)
    return lerp(C_PALE, C_WHITE, smooth((t - 0.78) / 0.15))


def c_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = 0.1 if face == "top" else 1.0 if face == "bottom" else (y - top) / (bottom - top)
        if face == "front" and z < -10.5:
            t = 0.4 + t * 0.6  # pale throat
        base = c_coat(t)
        if t < 0.95 and spot((x * 1.3, y * 1.2, z), 1.9, 0.62, 301, 0.85):
            return fur(C_SPOT, i, j, 302, 0.08)
        return fur(base, i, j, 303, 0.06)
    return mat


def c_tail(v, face, x, y, z, i, j):
    if y > 14.5:
        return fur(C_WHITE, i, j, 311, 0.04)  # white tip
    if y > 9:
        return fur(C_SPOT, i, j, 312, 0.08) if (y - 9) % 2.0 < 1.0 else fur(C_PALE, i, j, 313, 0.05)
    if spot((x * 1.4, y, z * 1.4), 1.7, 0.55, 314):
        return fur(C_SPOT, i, j, 315, 0.08)
    return fur(C_GOLD if face not in ("front", "top") else C_PALE, i, j, 316, 0.05)


def c_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1) < 0.1 and abs(y + 1.5) < 0.1:
            return C_EYE
        if abs(abs(x) - 1) < 0.1 and y > -1:
            return C_SPOT  # the tear marks run down from the inner corner of each eye
        if y < -2:
            return fur(C_BACK, i, j, 321, 0.05)
        return fur(C_PALE if abs(x) < 0.5 else C_GOLD, i, j, 322, 0.05)
    if face == "top":
        return C_SPOT if spot((x, y, z), 1.3, 0.4, 323, 0.7) else fur(C_BACK, i, j, 324, 0.05)
    if face == "bottom":
        return fur(C_WHITE, i, j, 325, 0.03)
    if face in ("left", "right") and z < -3 and y > -1:
        return C_SPOT  # the tear line at the side of the face
    return C_SPOT if y < -1 and spot((x, y, z), 1.4, 0.45, 326, 0.7) else fur(lerp(C_GOLD, C_PALE, 0.4 if y > -1 else 0.0), i, j, 327, 0.05)


def c_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        return C_NOSE if y < 0 and abs(x) < 1 else C_SPOT if abs(abs(x) - 1) < 0.1 else fur(C_WHITE, i, j, 331, 0.03)
    if face in ("left", "right"):
        return C_SPOT if y < 0 else fur(C_WHITE, i, j, 332, 0.03)  # the tear mark ends at the lip
    if face == "bottom":
        return shade(TONGUE, 0.8)
    return fur(lerp(C_GOLD, C_PALE, 0.4), i, j, 333, 0.05)


def c_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -1 else TONGUE
    return fur(C_WHITE, i, j, 341, 0.03)


def c_ear(v, face, x, y, z, i, j):
    if y < -1 and abs(x) > 0.5 and face != "bottom":
        return TRANSPARENT  # rounded
    if face == "front":
        return fur(C_WHITE, i, j, 351, 0.04)
    return fur(C_SPOT, i, j, 352, 0.08)


def c_leg(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            return C_GOLD
        base = lerp(C_PALE, C_WHITE, 0.5) if face == "left" else lerp(C_GOLD, C_PALE, t * 0.6)
        if spot((x * 1.4, y, z * 1.4), 1.8, 0.55, 361 + int(top), 0.8):
            return fur(C_SPOT, i, j, 362, 0.08)
        return fur(base, i, j, 363, 0.05)
    return mat


def c_lower(v, face, x, y, z, i, j):
    if y >= 6:
        return (44, 34, 30) if face == "bottom" else fur(C_PALE, i, j, 371, 0.04)
    if face != "left" and spot((x * 1.5, y, z * 1.5), 1.5, 0.45, 372, 0.7):
        return C_SPOT
    return fur(lerp(C_GOLD, C_PALE, y / 6), i, j, 373, 0.05)


CHEETAH = {
    "chest": (0, 0, -3, -3.5, -8, 6, 7, 9, c_body(-3.5, 3.5)),
    "waist": (0, 16, -2.5, -3, 1, 5, 6, 9, c_body(-3, 3)),
    "neck": (30, 0, -2, -3, -11, 4, 5, 4, c_body(-3, 2)),
    "tail": (56, 16, -1, 0, -1, 2, 16, 2, c_tail),
    "skull": (46, 0, -2.5, -3, -4, 5, 4, 4, c_skull),
    "muzzle": (46, 8, -1.5, -1, -6, 3, 2, 2, c_muzzle),
    "jaw": (46, 12, -1.5, 0, -2, 3, 1, 2, c_jaw),
    "ear": (56, 8, -1, -2, -0.5, 2, 2, 1, c_ear),
    "fore_upper": (0, 31, -1, -1, -1.5, 2, 6, 3, c_leg(-1, 5)),
    "lower": (10, 31, -1, 0, -1, 2, 6, 2, c_lower),
    "paw": (18, 31, -1, 6, -2, 2, 1, 3, c_lower),
    "thigh": (28, 31, -1.5, -1, -2, 3, 7, 4, c_leg(-1, 6)),
}

# ====================================================================== gazelle
G_BACK, G_COAT, G_LIGHT = (170, 116, 64), (192, 138, 80), (210, 162, 106)
G_STRIPE, G_WHITE, G_FACE = (40, 30, 24), (238, 232, 220), (178, 126, 72)
G_HORN, G_HORN_L, HOOF = (40, 36, 32), (78, 70, 62), (34, 30, 28)


def g_body(v, face, x, y, z, i, j):
    t = (y + 3) / 6  # 0 at the back .. 1 at the belly
    if face == "bottom":
        return fur(G_WHITE, i, j, 401, 0.03)
    if face == "top":
        return fur(G_BACK, i, j, 402, 0.06)
    if face == "back":
        return fur(G_WHITE if abs(x) < 2.5 and t > 0.15 else G_COAT, i, j, 403, 0.04)  # white rump patch
    if face == "front":
        return fur(lerp(G_COAT, G_LIGHT, t), i, j, 404, 0.05)
    if t > 0.75:
        return fur(G_WHITE, i, j, 405, 0.03)
    if t > 0.55:
        return fur(G_STRIPE, i, j, 406, 0.07)  # the bold black flank band
    if t > 0.45:
        return fur(G_LIGHT, i, j, 407, 0.05)  # a pale line above it
    if z > 4.5 and t > 0.1:
        return fur(G_WHITE, i, j, 408, 0.03)  # the white of the rump wraps round the haunch
    return fur(lerp(G_BACK, G_COAT, t / 0.45), i, j, 409, 0.06)


def g_neck(v, face, x, y, z, i, j):
    if face == "front":
        return fur(G_WHITE if y > -3 else G_LIGHT, i, j, 411, 0.03)  # white throat
    return fur(lerp(G_COAT, G_BACK, 0.3 if face == "back" else 0.0), i, j, 412, 0.06)


def g_head(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        if abs(y + 1.5) < 0.6 and abs(z + 1.5) < 0.6:
            return EYE
        if abs(y + 2.5) < 0.6 or abs(y + 0.5) < 0.6 and z > -2:
            return fur(G_WHITE, i, j, 421, 0.03)  # white ring round the eye, on to the brow
        return fur(G_FACE, i, j, 422, 0.05)
    if face == "front":
        return fur(G_STRIPE if abs(x) < 0.5 and y < -1 else G_FACE, i, j, 423, 0.05)  # dark blaze down the forehead
    if face == "bottom":
        return fur(G_WHITE, i, j, 424, 0.03)
    return fur(G_BACK, i, j, 425, 0.06)


def g_snout(v, face, x, y, z, i, j):
    if face == "front":
        return BLACK if y < 0 else fur(G_WHITE, i, j, 431, 0.03)
    if face in ("left", "right"):
        return fur(G_STRIPE if y < 0 else G_WHITE, i, j, 432, 0.05)  # the dark face stripe runs to the nose
    if face == "bottom":
        return fur(G_WHITE, i, j, 433, 0.03)
    return fur(G_FACE, i, j, 434, 0.05)


def g_ear(v, face, x, y, z, i, j):
    if y < -3 and abs(x) > 0.6 and face != "bottom":
        return TRANSPARENT  # pointed
    if face == "front":
        return fur((226, 206, 188), i, j, 441, 0.04)
    return fur(G_STRIPE if y < -3 else G_COAT, i, j, 442, 0.05)


def g_tail(v, face, x, y, z, i, j):
    return fur(G_STRIPE, i, j, 451, 0.07)


def g_leg(top, bottom):
    def mat(v, face, x, y, z, i, j):
        if face == "top":
            return G_COAT
        t = (y - top) / (bottom - top)
        return fur(lerp(G_WHITE, G_LIGHT, 0.4) if face == "left" or face == "back" else lerp(G_COAT, G_LIGHT, t), i, j, 461, 0.05)
    return mat


def g_cannon(top):
    def mat(v, face, x, y, z, i, j):
        if face == "bottom" or y > top + 5:
            return HOOF
        return fur(lerp(G_LIGHT, G_WHITE, 0.3) if face != "front" else G_COAT, i, j, 471, 0.05)
    return mat


def g_horn(v, face, x, y, z, i, j):
    if face in ("top", "bottom"):
        return G_HORN
    return G_HORN_L if j % 2 == 0 else G_HORN  # ringed


GAZELLE = {
    "body": (0, 0, -3, -3, -7, 6, 6, 13, g_body),
    "neck": (50, 9, -1.5, -6, -2, 3, 6, 4, g_neck),
    "head": (38, 0, -2, -3, -3, 4, 4, 5, g_head),
    "snout": (38, 9, -1.5, -1.5, -6, 3, 3, 3, g_snout),
    "ear": (56, 0, -1.5, -4, -0.5, 3, 4, 1, g_ear),
    "tail": (56, 5, -1, 0, 0, 2, 2, 1, g_tail),
    "front_upper": (0, 19, -1.5, -1, -1.5, 3, 5, 3, g_leg(-1, 4)),
    "cannon": (12, 19, -1, 4, -1, 2, 6, 2, g_cannon(4)),
    "hind_upper": (20, 19, -1.5, -1, -2, 3, 6, 4, g_leg(-1, 5)),
    "shank": (34, 19, -1, 5, -0.5, 2, 6, 2, g_cannon(5)),
    "horn": (42, 19, -0.5, -4, -0.5, 1, 4, 1, g_horn),
    "horn_tip": (46, 19, -0.5, -3, -0.5, 1, 3, 1, g_horn),
}

# ====================================================================== ostrich
O_PAL = {
    "male": dict(body=(30, 28, 28), body2=(46, 42, 42), plume=(240, 238, 230), plume2=(214, 210, 200), skin=(206, 148, 138),
                 leg=(200, 120, 108), head=(190, 160, 150), streak=None),
    "female": dict(body=(118, 102, 82), body2=(138, 120, 98), plume=(196, 186, 168), plume2=(170, 160, 142), skin=(184, 160, 140),
                   leg=(170, 150, 132), head=(176, 158, 140), streak=None),
    "chick": dict(body=(186, 156, 108), body2=(206, 178, 130), plume=(196, 168, 120), plume2=(176, 146, 100), skin=(196, 170, 128),
                  leg=(190, 164, 124), head=(200, 176, 136), streak=(92, 70, 46)),
}
O_BEAK, O_NAIL = (214, 196, 166), (60, 52, 46)


def o_feathers(c1, c2, i, j, salt):
    """Loose, shaggy plumage: soft rows of feathers with ragged lighter tips."""
    c = c1 if (j + (i // 2) % 2) % 3 else c2
    return fur(c, i, j, salt, 0.09)


def o_body(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if p["streak"] and (int(x + 8) % 3 == 0 or face == "top" and int(x + 8) % 3 == 0):
        return fur(p["streak"], i, j, 501, 0.08)  # the chick's dark streaks along the back
    if face == "bottom" and v == "male":
        return fur(p["body2"], i, j, 502, 0.06)
    return o_feathers(p["body"], p["body2"], i, j, 503)


def o_plume(v, face, x, y, z, i, j):
    p = O_PAL[v]
    c = o_feathers(p["plume"], p["plume2"], i, j, 511)
    if v == "male" and face in ("top", "right", "left") and y < 1:
        return o_feathers(p["body"], p["body2"], i, j, 512)  # the wing's black coverts over the white plumes
    return c


def o_neck(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if p["streak"] and (j + i) % 4 == 0:
        return fur(p["streak"], i, j, 521, 0.06)
    if v == "female" and y > -2 and face != "front":
        return o_feathers(p["body"], p["body2"], i, j, 522)  # feathers creep up the hen's neck base
    return fur(p["skin"], i, j, 523, 0.05)


def o_upper_neck(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if p["streak"] and (j + i) % 4 == 0:
        return fur(p["streak"], i, j, 531, 0.06)
    return fur(lerp(p["skin"], p["head"], 0.3), i, j, 532, 0.05)


def o_head(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if face in ("left", "right") and abs(y + 1) < 0.6 and abs(z + 0.5) < 0.6:
        return EYE  # the huge eye
    if face in ("left", "right") and abs(y + 2) < 0.6 and abs(z + 0.5) < 0.6:
        return shade(p["head"], 0.6)  # lashes over it
    if face == "top":
        return fur(shade(p["head"], 0.85), i, j, 541, 0.07)  # bristly crown
    return fur(p["head"], i, j, 542, 0.05)


def o_beak(v, face, x, y, z, i, j):
    if face == "front" or face == "bottom":
        return shade(O_BEAK, 0.85)
    if face in ("left", "right") and y > -0.5:
        return shade(O_BEAK, 0.7)  # the gape line
    return O_BEAK


def o_thigh(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if y < 1.5:
        return o_feathers(p["body"], p["body2"], i, j, 551)  # feathered top
    return fur(p["skin"], i, j, 552, 0.05)


def o_shank(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if face == "front" and j % 2 == 0:
        return shade(p["leg"], 0.85)  # scaled front
    return fur(p["leg"], i, j, 561, 0.04)


def o_foot(v, face, x, y, z, i, j):
    p = O_PAL[v]
    if z < -2.5 and face in ("front", "top"):
        return O_NAIL  # the big toenail
    if face == "top" and abs(x) < 0.5:
        return shade(p["leg"], 0.8)  # the gap between the two toes
    return fur(shade(p["leg"], 0.92), i, j, 571, 0.04)


OSTRICH = {
    "body": (0, 0, -4, -4, -6, 8, 8, 11, o_body),
    "tail": (38, 0, -3, -2, 0, 6, 4, 3, o_plume),
    "wing": (38, 7, 0, 0, -4, 1, 5, 8, o_plume),
    "neck": (0, 19, -1, -6, -1, 2, 6, 2, o_neck),
    "upper_neck": (8, 19, -1, -6, -1, 2, 6, 2, o_upper_neck),
    "head": (16, 19, -1.5, -2.5, -2.5, 3, 3, 4, o_head),
    "beak": (40, 20, -1, -1, -5.5, 2, 1, 3, o_beak),
    "thigh": (0, 27, -1.5, -1, -1.5, 3, 5, 3, o_thigh),
    "shank": (12, 27, -0.5, 0, -0.5, 1, 8, 1, o_shank),
    "foot": (16, 27, -1, 8, -3, 2, 1, 4, o_foot),
}

# ====================================================================== aardvark
A_SKIN, A_SKIN_D, A_HEAD, A_LEG = (160, 128, 112), (130, 102, 90), (184, 154, 138), (92, 74, 66)
A_EAR_IN, A_CLAW, A_NOSE = (212, 160, 150), (60, 50, 42), (120, 88, 84)


def a_hide(c, i, j, salt):
    """Sparse coarse hair over bare skin: a few darker bristle flecks."""
    c = fur(c, i, j, salt, 0.05, streak=False)
    return shade(c, 0.78) if h(i, j, salt + 1) < 0.12 else c


def a_body(v, face, x, y, z, i, j):
    t = (y + 3.5) / 7
    if face == "bottom":
        return a_hide(lerp(A_SKIN, A_EAR_IN, 0.3), i, j, 601)
    return a_hide(lerp(A_SKIN_D, A_SKIN, t) if face != "top" else A_SKIN_D, i, j, 602)


def a_skull(v, face, x, y, z, i, j):
    if face in ("left", "right") and abs(y + 0.5) < 0.6 and abs(z + 2.5) < 0.6:
        return EYE
    return a_hide(A_HEAD, i, j, 611)


def a_snout(v, face, x, y, z, i, j):
    if face == "front":
        return A_NOSE if abs(x) < 0.6 or y < 0.5 else shade(A_NOSE, 0.7)  # the pig-like disc with nostrils
    return a_hide(lerp(A_HEAD, A_EAR_IN, 0.25), i, j, 621)


def a_ear(v, face, x, y, z, i, j):
    if y < -4 and abs(z) > 0.5 and face != "bottom":
        return TRANSPARENT  # pointed tip
    if face == "front":
        return fur(A_EAR_IN, i, j, 631, 0.04)
    return a_hide(A_HEAD, i, j, 632)


def a_tail(v, face, x, y, z, i, j):
    return a_hide(lerp(A_SKIN, A_HEAD, z / 8), i, j, 641)  # pales towards the tip


def a_leg(length):
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return A_CLAW
        if y > length - 1.5:
            return A_CLAW if face == "front" or (i % 2 == 0 and face != "back") else shade(A_LEG, 0.8)  # the digging claws
        return a_hide(lerp(A_SKIN_D, A_LEG, y / length), i, j, 651)
    return mat


AARDVARK = {
    "body": (0, 0, -3.5, -3.5, -6, 7, 7, 13, a_body),
    "skull": (40, 0, -2, -2, -4, 4, 4, 4, a_skull),
    "snout": (40, 8, -1, -0.5, -9, 2, 2, 5, a_snout),
    "ear": (56, 0, -0.5, -5, -1, 1, 5, 2, a_ear),
    "tail": (0, 20, -1.5, -1.5, 0, 3, 3, 8, a_tail),
    "front": (22, 20, -1.5, 0, -1.5, 3, 6, 3, a_leg(6)),
    "hind": (34, 20, -1.5, 0, -2, 3, 7, 4, a_leg(7)),
}


# ====================================================================== items
CHEETAH_STOPS = [(30, 24, 20), (150, 108, 52), (196, 150, 78), (216, 172, 96), (236, 214, 170)]


def cheetah_pelt():
    return mt.pelt("wolf_pelt", CHEETAH_STOPS, spots=741, salt=742)


def hyena_fur(x, y):
    return (52, 36, 26) if h(x // 2, y // 2, 751) < 0.3 and h(x, y, 752) < 0.6 else fur((184, 150, 98), x, y, 753, 0.07)


def gazelle_fur(x, y):
    if y in (9, 10):
        return fur((40, 30, 24), x, y, 761, 0.06)  # the flank band across the hide
    return fur((192, 138, 80) if y < 9 else (236, 228, 214), x, y, 762, 0.06)


def ostrich_skin(x, y):
    c = fur((206, 176, 150), x, y, 771, 0.04)
    return shade(c, 0.75) if (x + 2 * y) % 4 == 0 and h(x, y, 772) < 0.7 else c  # the quill bumps of ostrich leather


def aardvark_skin(x, y):
    c = fur((160, 128, 112), x, y, 781, 0.05, streak=False)
    return shade(c, 0.78) if h(x, y, 782) < 0.12 else c


def horn_icon(base, light, ring=True, band=None):
    """A curved horn from lower left to upper right, ringed, the tip curling forward."""
    pts = [(3.0, 13.5), (5.5, 10.0), (7.0, 6.5), (8.5, 3.5), (11.0, 2.0), (12.5, 2.5)]
    width = lambda t: 3.6 - 2.6 * t

    def colour(t, s, x, y):
        c = lerp(light, base, 0.3 + 0.5 * t) if s < 0 else lerp(base, shade(base, 0.7), t)
        if band and 0.08 < t < 0.2:
            return band
        if ring and int(t * 14) % 2 == 0 and t < 0.85:
            c = shade(c, 0.78)
        return c

    return tube(pts, width, colour, lambda t, x, y: shade(base, 0.55))


def feather_icon():
    """A soft white ostrich plume curving up and right on a dark quill."""
    pts = [(2.5, 14.0), (5.0, 10.0), (8.0, 6.5), (11.0, 4.0), (13.5, 2.5)]
    width = lambda t: 1.0 + 6.0 * math.sin(min(1.0, t * 1.15) * math.pi) ** 0.7

    def colour(t, s, x, y):
        if abs(s) < 0.2:
            return (90, 84, 78)  # quill
        c = lerp((248, 246, 240), (206, 200, 190), abs(s))
        return shade(c, 0.9) if (x + y) % 3 == 0 else c

    return tube(pts, width, colour, lambda t, x, y: (176, 170, 160))


def claw_icon():
    pts = [(4.0, 12.5), (6.0, 8.5), (8.5, 5.0), (11.5, 3.5), (13.0, 4.5)]
    return tube(pts, lambda t: 4.0 - 3.0 * t, lambda t, s, x, y: lerp((96, 82, 66), (60, 50, 42), t) if s > 0 else lerp((150, 132, 108), (96, 82, 66), t),
                lambda t, x, y: (40, 32, 26))


def ostrich_egg(cooked):
    if cooked:  # roasted in its shell: browned, the top cracked open on the yellow
        im = egg(["#c8a070", "#b08858", "#966f44", "#7a5634", "#5a4028", "#3e2c1c"])
        for x, y in ((6, 3), (7, 2), (8, 3), (9, 2), (7, 3), (8, 2)):
            put(im, x, y, (236, 196, 80))
        for x in range(5, 11):
            put(im, x, 4, (90, 64, 40))
        return im
    im = egg(["#fbf7ea", "#f1ead4", "#e2d8bc", "#cbbf9f", "#a99c7c", "#857858"])
    px = im.load()
    for x in range(16):
        for y in range(16):
            if px[x, y][3] and h(x, y, 791) < 0.08:
                put(im, x, y, (214, 204, 176))  # pores in the thick shell
    return im


def termites():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for k, (x, y) in enumerate(((3, 4), (9, 3), (6, 8), (11, 9), (4, 12), (10, 13))):
        for dx in range(3):
            put(im, x + dx, y, (236, 226, 200) if dx else (196, 112, 48))  # pale body, orange head
        put(im, x + 1, y + 1, (208, 196, 168))
    return im


def chitin_paste():
    im = Image.new("RGBA", (16, 16), CLEAR)
    for x in range(16):
        for y in range(16):
            d = math.hypot((x - 7.5) / 6.0, (y - 9.0) / 4.5)
            if d < 1:
                c = lerp((206, 150, 70), (130, 84, 40), d)
                if h(x, y, 801) < 0.15:
                    c = shade(c, 1.2)
                put(im, x, y, c if d < 0.85 else (96, 62, 30))
    for x, y in ((5, 7), (6, 6), (9, 7)):
        put(im, x, y, (250, 222, 160))  # glossy highlights
    return im


def signal_horn():
    im = horn_icon((50, 44, 38), (96, 86, 74), band=(186, 150, 104))
    put(im, 3, 14, (186, 150, 104))  # the sinew-bound mouthpiece
    put(im, 2, 14, (150, 116, 76))
    return im


def cheetah_cloak():
    im = cheetah_pelt()
    for x in range(2, 14):
        put(im, x, 1, (120, 80, 46))  # a leather tie across the shoulders
    put(im, 1, 1, (86, 56, 30))
    put(im, 14, 1, (86, 56, 30))
    return im


def items():
    return {
        "cheetah_meat": meat("chicken_scraps", LEOPARD_RAW, fibre=0.8, salt=701),
        "cooked_cheetah_meat": meat("cooked_chicken_scraps", LEOPARD_COOKED, fibre=0.8, salt=702),
        "gazelle_meat": meat("venison_scraps", ONAGER_RAW, salt=703),
        "cooked_gazelle_meat": meat("cooked_venison_scraps", ONAGER_COOKED, salt=704),
        "ostrich_meat": meat("beef_scraps", BUFFALO_RAW, salt=705),
        "cooked_ostrich_meat": meat("cooked_beef_scraps", BUFFALO_COOKED, salt=706),
        "aardvark_meat": meat("porkchop_scraps", TAPIR_RAW, fat=(226, 200, 186), salt=707),
        "cooked_aardvark_meat": meat("cooked_porkchop_scraps", TAPIR_COOKED, fat=(170, 130, 92), salt=708),
        "hyena_hide": hide_item(hyena_fur, 711, lip=True, raw_edge=0.4),
        "raw_gazelle_hide": hide_item(gazelle_fur, 712),
        "raw_ostrich_hide": hide_item(ostrich_skin, 713, fold=21),
        "raw_aardvark_hide": hide_item(aardvark_skin, 714, lip=True, raw_edge=0.3),
        "cheetah_pelt": cheetah_pelt(),
        "raw_cheetah_pelt": raw_item(cheetah_pelt(), 715),
        "cheetah_cloak": cheetah_cloak(),
        "gazelle_horn": horn_icon((40, 36, 32), (96, 88, 78)),
        "signal_horn": signal_horn(),
        "ostrich_feather": feather_icon(),
        "aardvark_claw": claw_icon(),
        "ostrich_egg": ostrich_egg(False),
        "cooked_ostrich_egg": ostrich_egg(True),
        "termites": termites(),
        "chitin_paste": chitin_paste(),
        "spotted_hyena_spawn_egg": egg(["#e0c48e", "#c8a46c", "#ac8a56", "#8a6c42", "#5c4630", "#3c2e20"],
                                       spots=[(5, 5, "#3a2a1e"), (9, 7, "#3a2a1e"), (6, 10, "#3a2a1e"), (10, 11, "#3a2a1e")]),
        "cheetah_spawn_egg": egg(["#f4dca6", "#e2bc76", "#cc9e58", "#a87c40", "#7a5a2e", "#4e3a1e"],
                                 spots=[(5, 4, "#1e1a16"), (8, 5, "#1e1a16"), (10, 8, "#1e1a16"), (6, 8, "#1e1a16"), (8, 11, "#1e1a16"),
                                        (5, 12, "#1e1a16")]),
        "gazelle_spawn_egg": egg(["#e6b884", "#cc9a60", "#b07e48", "#8e6236", "#604228", "#40301e"],
                                 paint=[(x, 9, "#2a201a") for x in range(3, 13)] + [(x, 10, "#2a201a") for x in range(3, 13)]
                                 + [(x, y, "#f0eade") for x in range(4, 12) for y in (11, 12)]),
        "ostrich_spawn_egg": egg(["#5a5452", "#3e3a38", "#2c2826", "#1e1a1a", "#141010", "#0a0808"],
                                 spots=[(6, 9, "#f0eee6"), (7, 9, "#f0eee6"), (9, 10, "#f0eee6"), (10, 10, "#f0eee6"), (8, 4, "#d49a8c")]),
        "aardvark_spawn_egg": egg(["#d8bcae", "#bc9e8e", "#a08272", "#84685a", "#5e4a40", "#40322a"],
                                  spots=[(7, 4, "#e8a89c"), (8, 4, "#e8a89c")]),
    }


# ====================================================================== blocks, equipment
def mound(x, y, salt, top=False):
    """Baked red-brown earth with tunnel openings and vertical runnels from the rain."""
    c = lerp((176, 98, 52), (146, 78, 42), h(x, y // 3, salt))
    if top:
        d = math.hypot(x - 7.5, y - 7.5)
        if d < 1.6:
            return (40, 22, 14)  # the shaft at the top
        c = lerp(c, (196, 120, 70), 0.3 * (1 - d / 11))
    elif (x * 5 + salt) % 7 == 0:
        c = shade(c, 0.86)  # runnels
    if h(x // 2, y // 2, salt + 1) < 0.1 and h(x, y, salt + 2) < 0.7:
        return (48, 26, 16)  # tunnel holes
    return fur(c, x, y, salt + 3, 0.06)


def blocks():
    side, top = Image.new("RGBA", (16, 16)), Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            side.putpixel((x, y), (*mound(x, y, 811), 255))
            top.putpixel((x, y), (*mound(x, y, 821, True), 255))
    return {"termite_mound": side, "termite_mound_top": top}


def cloak_layer():
    def colour(x, y, s):
        if h(x // 2, y // 2, 831) < 0.22 and h(x, y, 832) < 0.75:
            return C_SPOT
        return fur(lerp(C_GOLD, C_PALE, s * 0.6), x, y, 833, 0.05)
    return recolour_layer("entity/equipment/humanoid/leather", colour, lambda x, y: y >= 16)


def cape_mat(v, face, x, y, z, i, j):
    if face == "back":
        return fur((150, 108, 68), i, j, 841, 0.05)  # tanned leather lining
    if y > 13 and face == "front":
        return fur(C_SPOT if (y - 13) % 2 < 1 else C_PALE, i, j, 842, 0.06)  # the ringed tail hanging at the hem
    if spot((x * 1.2, y, z * 3), 1.9, 0.6, 843, 0.85):
        return fur(C_SPOT, i, j, 844, 0.08)
    return fur(lerp(C_GOLD, C_PALE, y / 20), i, j, 845, 0.05)


def main():
    hyena = spotted_hyena()
    save(hyena, "wildlife/spotted_hyena.png")
    save(skinned(hyena, mt.HYENA), "skinned/wildlife/spotted_hyena.png")
    cheetah = paint(CHEETAH, "cheetah", (64, 64))
    save(cheetah, "wildlife/cheetah.png")
    save(skinned(cheetah, CHEETAH, keep=("paw",)), "skinned/wildlife/cheetah.png")
    gazelle = paint(GAZELLE, "gazelle", (64, 32))
    save(gazelle, "wildlife/gazelle.png")
    save(skinned(gazelle, GAZELLE, keep=("horn", "horn_tip")), "skinned/wildlife/gazelle.png")
    for v in O_PAL:
        ostrich = paint(OSTRICH, v, (64, 64))
        save(ostrich, f"wildlife/ostrich_{v}.png")
        save(skinned(ostrich, OSTRICH, keep=("beak", "foot")), f"skinned/wildlife/ostrich_{v}.png")
    aardvark = paint(AARDVARK, "aardvark", (64, 32))
    save(aardvark, "wildlife/aardvark.png")
    save(skinned(aardvark, AARDVARK), "skinned/wildlife/aardvark.png")
    for folder, made in ((ITEM, items()), (BLOCK, blocks())):
        for name, im in made.items():
            assert im.size == (16, 16) and im.getbbox(), name
            im.save(folder / f"{name}.png")
    layer = cloak_layer()
    assert layer.getbbox()
    layer.save(TEX / "equipment/humanoid/cheetah_cloak.png")
    save(paint({"cape": (0, 0, -5, 0, -0.5, 10, 16, 1, cape_mat)}, "cape", (64, 32)), "cloak/cheetah_cloak.png")
    print("fauna savanna textures ok")


if __name__ == "__main__":
    main()
