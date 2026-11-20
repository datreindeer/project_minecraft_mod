"""Paint the cold hunters (data: scripts/gen_fauna_coldhunters.py; models: client/wildlife/coldhunters/*Model.java).

- Entities, painted from 3D like the other wildlife (wildlife_paint.py), with skinned (flesh) versions for butchered
  carcasses: Wolverine (64x64: dark chocolate, the pale buff band down each flank and over the rump, grey-buff brow,
  dark mask, black legs, pale claws, bushy tail), Lynx (64x64: grey-tawny with dark spots, white belly, cheek ruffs barred
  black, black ear backs and tufts, black tail tip) and its kitten (paler, fluffier, spottier), Ermine (64x32: brown above
  and cream below in summer, snow white in winter, black tail tip either way), Lemming (64x32: black head and shoulders,
  tawny back with a dark saddle, buff sides), Pine Marten (64x32: chocolate brown, darker legs and tail, creamy-orange bib,
  cream-rimmed ears).
- Items (16x16): raw and cooked meat of each, raw pelts (flesh side up, fur at the edges) and cured furs, Lemming Pelt,
  Wolverine Claw, Claw Flesher, Wolverine-Trimmed Hood, Ermine Mantle, Marten Fur Cap, Lynx Rug, the five spawn eggs.
- Equipment layers: the hood (the Fur Hood's layer with a frosty wolverine ruff round the face), the mantle and the cap.
- Block: the Lynx Rug (head north, like the Wolf Rug).
The box tables (LYNX, WOLVERINE, ERMINE, LEMMING, MARTEN) mirror the cubes in the models: keep them in sync.
Run: python scripts/gen_fauna_coldhunters_textures.py   (deterministic)
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402
from gen_wildlife_item_textures import OUT as ITEM, egg, recolour  # noqa: E402
from gen_predator_textures import PELT_MASK  # noqa: E402
from gen_wolf_pelt_textures import RUG_MASK  # noqa: E402
from gen_bronze_prep_textures import lum, vanilla  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
BLOCK, EQUIP = TEX / "block", TEX / "entity/equipment"
CLEAR = (0, 0, 0, 0)
EYE, NOSE_BLACK, PINK_NOSE = (16, 12, 10), (26, 20, 20), (186, 120, 118)
CLAW, PAD, TOOTH, TONGUE = (214, 204, 178), (40, 32, 30), (232, 226, 210), (170, 84, 86)
OUTLINE = (28, 22, 18)
HIDE_SIDE = (222, 196, 168)  # the flesh side of a fresh pelt
HIDE_DRY = (196, 162, 118)


def rgba(c):
    return (*c[:3], 255)


def spot(x, y, z, salt, size=2.0, density=0.3):
    """A spot pattern in 3D (so it runs on across faces): staggered cells, some filled with a rounded blot."""
    cy = math.floor(y / size)
    sx = x / size + (0.5 if cy % 2 else 0.0)
    cx, cz = math.floor(sx), math.floor(z / size)
    if h(cx * 7 + cz * 131, cy, salt) >= density:
        return False
    fx, fy, fz = sx - cx - 0.5, y / size - cy - 0.5, z / size - cz - 0.5
    return fx * fx + fy * fy + fz * fz < 0.32 + 0.1 * h(cx, cy + cz, salt + 1)


# =============================================================== LYNX
L_BACK, L_SIDE, L_PALE, L_BELLY = (150, 116, 84), (186, 148, 106), (212, 190, 154), (234, 226, 210)
L_SPOT, L_BLACK = (66, 46, 32), (30, 26, 24)
EYE_LYNX = (190, 160, 70)


def lynx_tone(t, kit):
    """t 0 (spine) .. 1 (belly)."""
    c = lerp(L_BACK, L_SIDE, t / 0.55) if t < 0.55 else lerp(L_SIDE, L_BELLY, smooth((t - 0.55) / 0.45))
    return lerp(c, L_PALE, 0.35) if kit else c


def lynx_fur(v, c, x, y, z, i, j, salt, spots=True):
    kit = v == "kitten"
    if spots and spot(x, y, z, salt, 1.6 if kit else 2.0, 0.42 if kit else 0.3) and sum(c) < 560:
        c = lerp(c, L_SPOT, 0.55 if kit else 0.75)
    return fur(c, i, j, salt, 0.09 if kit else 0.07)


def lynx_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            return lynx_fur(v, lynx_tone(abs(x) / 6, v == "kitten"), x, y, z, i, j, 111)
        if face == "bottom":
            return fur(L_BELLY, i, j, 112, 0.04)
        if face == "front":
            return fur(lerp(L_SIDE, L_BELLY, t), i, j, 113, 0.06)  # throat and chest
        if face == "back":
            return lynx_fur(v, lynx_tone(t, v == "kitten"), x, y, z, i, j, 114, spots=False)
        return lynx_fur(v, lynx_tone(t, v == "kitten"), x, y, z, i, j, 115)
    return mat


def lynx_tail(v, face, x, y, z, i, j):
    if y > 2.5 or face == "bottom":
        return fur(L_BLACK, i, j, 121, 0.06)  # the black tip, all round
    if face == "front":  # underside (the tail hangs back)
        return fur(L_PALE, i, j, 122, 0.05)
    return lynx_fur(v, L_SIDE, x, y, z, i, j, 123)


def lynx_skull(v, face, x, y, z, i, j):
    kit = v == "kitten"
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 1.5) < 0.1:
            return EYE_LYNX
        if abs(abs(x) - 2.5) < 0.1 and abs(y + 1.5) < 0.1:
            return EYE  # black eye line running back
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 0.5) < 0.1:
            return fur(L_BELLY, i, j, 131, 0.03)  # white below the eye
        if abs(x) < 0.6 and y < -1.5:
            return fur(L_SPOT, i, j, 132, 0.05)  # dark forehead streak
        return fur(lerp(L_SIDE, L_PALE, 0.4), i, j, 133)
    if face == "top":
        return lynx_fur(v, L_BACK, x, y, z, i, j, 134) if not (abs(x) < 0.6 and not kit) else fur(L_SPOT, i, j, 135, 0.05)
    if face == "bottom":
        return fur(L_BELLY, i, j, 136, 0.04)
    if face in ("left", "right") and abs(y + 1.5) < 0.1:
        return fur(L_BLACK, i, j, 137, 0.04)  # the eye line along the side of the head
    return lynx_fur(v, L_SIDE, x, y, z, i, j, 138)


def lynx_ruff(v, face, x, y, z, i, j):
    """Cheek ruffs: white, barred black, hanging below the jaw."""
    if face == "top":
        return fur(L_SIDE, i, j, 141, 0.06)
    if abs(x) > 2.5 and (abs(y - 0.5) < 0.1 or abs(y - 2.5) < 0.1) and face != "bottom":
        return fur(L_BLACK, i, j, 142, 0.05)
    return fur(lerp(L_BELLY, L_PALE, 0.3 if y < 1 else 0.0), i, j // 2 * 2 + i % 2, 143, 0.07)


def lynx_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        return PINK_NOSE if y < 0.5 and abs(x) < 1 else fur(L_BELLY, i, j, 151, 0.03)
    if face == "top":
        return fur(lerp(L_SIDE, L_PALE, 0.5), i, j, 152, 0.05)
    if face == "bottom":
        return shade(TONGUE, 0.8)
    return fur(L_BELLY, i, j, 153, 0.03) if y > 0.5 else EYE if z < -5.5 and y > 0 else fur(L_PALE, i, j, 154, 0.04)


def lynx_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -1.5 or abs(x) > 1 else TONGUE
    return fur(L_BELLY, i, j, 161, 0.03)


def lynx_ear(v, face, x, y, z, i, j):
    """Pointed, black-backed with a pale centre spot; pale inner ear."""
    if y < -2 and abs(x) > 0.5:
        return CLEAR
    if face == "front":
        return fur(L_BELLY, i, j, 171, 0.04) if y > -2.5 else fur(L_BLACK, i, j, 172, 0.04)
    if face == "back" and -2 < y < -1 and abs(x) < 0.6:
        return fur((210, 200, 186), i, j, 173, 0.03)  # the pale spot on the black ear back
    return fur(L_BLACK, i, j, 174, 0.05)


def lynx_tuft(v, face, x, y, z, i, j):
    if v == "kitten" and y < -4:
        return CLEAR  # a kitten's tufts are short
    return fur(L_BLACK, i, j, 181, 0.06)


def lynx_leg(top, bottom, fore):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "left" or (not fore and face == "back"):
            return fur(lerp(L_PALE, L_BELLY, t), i, j, 191, 0.05)  # inner side / back of the thigh
        return lynx_fur(v, lerp(L_SIDE, L_PALE, t * 0.5), x, y + (0 if fore else 9), z + (0 if fore else 7), i, j, 192 + fore)
    return mat


def lynx_lower(v, face, x, y, z, i, j):
    if y >= 4:  # the big furred paw
        if face == "bottom":
            return PAD
        if face == "front" and abs(x) < 0.4:
            return shade(L_PALE, 0.75)
        return fur(L_PALE, i, j, 201, 0.06)
    return lynx_fur(v, lerp(L_SIDE, L_PALE, 0.4), x, y + 20, z, i, j, 202)


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors LynxModel.createBodyLayer
LYNX = {
    "chest": (0, 0, -3, -3.5, -7, 6, 7, 8, lynx_body(-3.5, 3.5)),
    "loin": (0, 15, -2.5, -3, 1, 5, 6, 7, lynx_body(-3, 3)),
    "tail": (52, 0, -1, 0, -1, 2, 4, 2, lynx_tail),
    "skull": (28, 0, -3, -3, -4, 6, 5, 4, lynx_skull),
    "ruff": (28, 9, -4, -1, -2, 8, 4, 2, lynx_ruff),
    "muzzle": (48, 9, -1.5, -0.5, -6, 3, 2, 2, lynx_muzzle),
    "jaw": (48, 13, -1.5, 0, -2, 3, 1, 2, lynx_jaw),
    "ear": (58, 6, -1, -3, -0.5, 2, 3, 1, lynx_ear),
    "tuft": (58, 10, -0.5, -5, -0.5, 1, 2, 1, lynx_tuft),
    "fore_upper": (0, 28, -1, -1, -1.5, 2, 6, 3, lynx_leg(-1, 5, True)),
    "lower": (10, 28, -1, 0, -1, 2, 4, 2, lynx_lower),
    "paw": (18, 28, -1.5, 4, -2, 3, 1, 3, lynx_lower),
    "thigh": (30, 28, -1.5, -1, -2, 3, 7, 4, lynx_leg(-1, 6, False)),
}

# =============================================================== WOLVERINE
V_DARK, V_BROWN, V_BAND, V_BROW = (40, 28, 21), (70, 48, 33), (176, 138, 92), (148, 130, 108)
V_TIP = (92, 66, 44)


def band_at(x, y, z):
    """The pale band: from the shoulder (z -4) back along the upper flank, rising over the rump to meet at the tail."""
    if z < -4:
        return 0.0
    yb = -0.8 - max(0.0, z + 4) * 0.18
    d = abs(y - yb)
    w = 1.1 if z < 5 else 1.4
    return max(0.0, 1 - d / w) if abs(x) > 2.0 or z > 5 else 0.0


def wolverine_coat(base, x, y, z, i, j, salt):
    c = base
    b = band_at(x, y, z)
    if b > 0:
        c = lerp(c, V_BAND, smooth(b) * 0.95)
    if h(i, j, salt + 5) < 0.18:
        c = lerp(c, V_TIP, 0.5)  # long, frosted guard hairs
    return fur(c, i, j // 2 * 2 + i % 2, salt, 0.08)


def wolverine_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "bottom":
            return fur(V_DARK, i, j, 211, 0.06)
        if face == "front":
            return fur(lerp(V_BROWN, V_DARK, t), i, j, 212, 0.07)
        if face == "top":
            return wolverine_coat(V_BROWN, x, y + 0.2 * abs(x), z, i, j, 213)
        return wolverine_coat(lerp(V_BROWN, V_DARK, smooth(t)), x, y, z, i, j, 214)
    return mat


def wolverine_tail(v, face, x, y, z, i, j):
    c = lerp(V_BROWN, V_DARK, min(1.0, y / 4))
    if y < 1.5 and face in ("back", "left", "right"):
        c = lerp(c, V_BAND, 0.5)  # the band runs out onto the tail root
    return fur(c, i, j // 2 * 2 + i % 2, 221, 0.1)


def wolverine_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 0.5) < 0.1:
            return EYE
        if y < -1.5:
            return fur(V_BROW, i, j, 231, 0.07)  # the pale brow band
        return fur(V_DARK, i, j, 232, 0.06)  # dark mask
    if face == "top":
        return fur(lerp(V_BROW, V_BROWN, 0.3 + 0.1 * abs(x)), i, j, 233, 0.07)
    if face == "bottom":
        return fur(V_DARK, i, j, 234, 0.05)
    if face in ("left", "right"):
        return fur(V_BROW, i, j, 235, 0.07) if y < -1 else fur(V_BROWN, i, j, 236, 0.06)
    return fur(V_BROWN, i, j, 237, 0.07)


def wolverine_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        return NOSE_BLACK if y < 0.5 else shade(V_DARK, 1.2)
    if face == "bottom":
        return shade(TONGUE, 0.8)
    return fur(V_DARK, i, j, 241, 0.05)


def wolverine_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -1.5 or abs(x) > 1 else TONGUE
    return fur((176, 150, 120) if face == "front" else V_DARK, i, j, 251, 0.05)  # pale chin


def wolverine_ear(v, face, x, y, z, i, j):
    if y < -1 and abs(x) > 0.5:
        return CLEAR  # rounded
    return fur(V_BROW if face == "front" else V_BROWN, i, j, 261, 0.06)


def wolverine_leg(v, face, x, y, z, i, j):
    return fur(V_DARK, i, j, 271, 0.06)


def wolverine_lower(v, face, x, y, z, i, j):
    if y >= 3:  # broad paw, the long pale claws at the front
        if face == "bottom":
            return PAD
        if face == "front" or (face in ("left", "right") and z < -1.5):
            return CLAW if (i + (face == "front")) % 2 == 0 else shade(CLAW, 0.7)
        return fur(V_DARK, i, j, 281, 0.05)
    return fur(V_DARK, i, j, 282, 0.06)


WOLVERINE = {
    "chest": (0, 0, -3.5, -3.5, -6, 7, 7, 7, wolverine_body(-3.5, 3.5)),
    "rump": (0, 14, -3.5, -4, 1, 7, 7, 6, wolverine_body(-4, 3)),
    "tail": (28, 14, -1.5, 0, -1.5, 3, 6, 3, wolverine_tail),
    "skull": (28, 0, -3, -2.5, -4, 6, 5, 4, wolverine_skull),
    "muzzle": (48, 0, -1.5, -0.5, -6, 3, 2, 2, wolverine_muzzle),
    "jaw": (48, 4, -1.5, 0, -2, 3, 1, 2, wolverine_jaw),
    "ear": (58, 0, -1, -1.5, -0.5, 2, 2, 1, wolverine_ear),
    "fore_upper": (0, 28, -1.5, -1, -1.5, 3, 4, 3, wolverine_leg),
    "lower": (12, 28, -1.5, 0, -1.5, 3, 3, 3, wolverine_lower),
    "paw": (24, 28, -1.5, 3, -2.5, 3, 1, 4, wolverine_lower),
    "thigh": (38, 28, -2, -1, -2, 4, 5, 4, wolverine_leg),
}

# =============================================================== ERMINE
E_BROWN, E_DARKB, E_CREAM, E_WHITE, E_SNOW = (134, 88, 54), (104, 66, 40), (238, 226, 194), (246, 244, 238), (236, 234, 222)


def ermine_coat(v, t, i, j, salt):
    """Summer: brown above a sharp line, cream below. Winter: white all over (a faint yellow cast)."""
    if v == "winter":
        return fur(lerp(E_WHITE, E_SNOW, h(i, j, salt + 3) * 0.6), i, j, salt, 0.03)
    return fur(E_BROWN if t < 0.58 else E_CREAM, i, j, salt, 0.06 if t < 0.58 else 0.03)


def ermine_body(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            return ermine_coat(v, 0.0, i, j, 301)
        if face == "bottom":
            return ermine_coat(v, 1.0, i, j, 302)
        if face == "front":
            return ermine_coat(v, max(t, 0.6), i, j, 303)  # cream throat
        return ermine_coat(v, t, i, j, 304)
    return mat


def ermine_tail(v, face, x, y, z, i, j):
    if y >= 3.3:
        return fur(L_BLACK, i, j, 311, 0.05)
    return ermine_coat(v, 0.0, i, j, 312)


def ermine_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1) < 0.1 and abs(y + 0.5) < 0.1:
            return EYE
        return ermine_coat(v, 0.8 if y > 0.5 else 0.0, i, j, 321)
    if face == "bottom":
        return ermine_coat(v, 1.0, i, j, 322)
    if face in ("left", "right"):
        return ermine_coat(v, 1.0 if y > 0.5 else 0.0, i, j, 323)
    return ermine_coat(v, 0.0, i, j, 324)


def ermine_muzzle(v, face, x, y, z, i, j):
    if face == "front" and z < -3.5:
        return NOSE_BLACK if abs(x) < 0.6 else ermine_coat(v, 1.0, i, j, 331)
    return ermine_coat(v, 0.0 if face == "top" else 1.0, i, j, 332)


def ermine_jaw(v, face, x, y, z, i, j):
    return TOOTH if face == "top" else ermine_coat(v, 1.0, i, j, 341)


def ermine_ear(v, face, x, y, z, i, j):
    return ermine_coat(v, 0.8 if face == "front" else 0.0, i, j, 351)


def ermine_leg(v, face, x, y, z, i, j):
    return ermine_coat(v, 1.0 if face in ("left", "front") else 0.2, i, j, 361)


def ermine_foot(v, face, x, y, z, i, j):
    if face == "bottom":
        return PAD
    return ermine_coat(v, 1.0, i, j, 371)


ERMINE = {
    "front": (0, 0, -1.5, -1.5, -4, 3, 3, 4, ermine_body(-1.5, 1.5)),
    "rear": (0, 7, -1.5, -1.5, 0, 3, 3, 5, ermine_body(-1.5, 1.5)),
    "tail": (16, 0, -0.5, 0, -0.5, 1, 5, 1, ermine_tail),
    "skull": (20, 0, -1.5, -1.5, -3, 3, 3, 3, ermine_skull),
    "muzzle": (32, 0, -1, -0.5, -4, 2, 1, 1, ermine_muzzle),
    "jaw": (32, 2, -1, 0, -1, 2, 1, 1, ermine_jaw),
    "ear": (38, 0, -0.5, -1, -0.5, 1, 1, 1, ermine_ear),
    "fore_upper": (0, 15, -0.5, -0.5, -0.5, 1, 2, 1, ermine_leg),
    "lower": (4, 15, -0.5, 0, -0.5, 1, 1, 1, ermine_leg),
    "paw": (8, 15, -0.5, 1, -1, 1, 1, 2, ermine_foot),
    "thigh": (14, 15, -0.5, -0.5, -1, 1, 3, 2, ermine_leg),
}

# =============================================================== LEMMING
M_BLACK, M_TAWNY, M_SADDLE, M_BUFF, M_PALE = (32, 26, 22), (176, 120, 62), (96, 64, 38), (210, 176, 116), (226, 206, 160)


def lemming_body(v, face, x, y, z, i, j):
    t = (y + 2) / 4
    if face == "bottom":
        return fur(M_BUFF, i, j, 401, 0.05)
    if face == "front":
        return fur(M_BLACK if y < 0.5 else M_BUFF, i, j, 402, 0.06)
    shoulder = z < 0.5 - 0.6 * abs(x)  # black shoulders and nape, fading back
    if face == "top":
        if shoulder:
            return fur(M_BLACK, i, j, 403, 0.07)
        c = M_SADDLE if abs(x) < 0.9 or h(i, j, 404) < 0.3 else M_TAWNY  # dark mottled saddle down the back
        return fur(c, i, j, 405, 0.08)
    if t < 0.45:
        return fur(M_BLACK if shoulder else lerp(M_TAWNY, M_SADDLE, 0.3 * h(i, j, 406)), i, j, 407, 0.08)
    return fur(lerp(M_TAWNY, M_BUFF, (t - 0.45) / 0.55), i, j, 408, 0.06)


def lemming_head(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1) < 0.1 and abs(y + 0.5) < 0.1:
            return EYE
        return fur(M_PALE if y > 0.5 else M_BLACK, i, j, 411, 0.06)  # pale lips and cheeks
    if face in ("left", "right"):
        if abs(y + 0.5) < 0.1 and z < -1:
            return fur(M_BUFF, i, j, 412, 0.04)  # buff stripe behind the eye
        return fur(M_PALE if y > 0.5 else M_BLACK, i, j, 413, 0.06)
    if face == "bottom":
        return fur(M_PALE, i, j, 414, 0.04)
    return fur(M_BLACK, i, j, 415, 0.07)


def lemming_nose(v, face, x, y, z, i, j):
    return PINK_NOSE if face == "front" else fur(M_PALE, i, j, 421, 0.04)


def lemming_ear(v, face, x, y, z, i, j):
    return fur(lerp(M_BLACK, M_TAWNY, 0.4), i, j, 431, 0.06)


def lemming_tail(v, face, x, y, z, i, j):
    return fur(M_TAWNY, i, j, 441, 0.06)


def lemming_leg(v, face, x, y, z, i, j):
    return (150, 120, 96) if face == "bottom" else fur(M_BUFF, i, j, 451, 0.04)


LEMMING = {
    "body": (0, 0, -2, -2, -2.5, 4, 4, 6, lemming_body),
    "tail": (20, 6, -0.5, -0.5, 0, 1, 1, 1, lemming_tail),
    "head": (20, 0, -1.5, -1.5, -2.5, 3, 3, 3, lemming_head),
    "nose": (32, 0, -0.5, 0, -3, 1, 1, 1, lemming_nose),
    "ear": (36, 0, -0.5, -1, -0.5, 1, 1, 1, lemming_ear),
    "front_leg": (0, 10, -0.5, 0, -0.5, 1, 1, 1, lemming_leg),
    "hind_leg": (4, 10, -0.5, 0, -0.5, 1, 1, 1, lemming_leg),
}

# =============================================================== PINE MARTEN
P_BROWN, P_DARK, P_FACE, P_BIB, P_CREAM = (96, 60, 36), (56, 36, 24), (118, 82, 54), (230, 190, 112), (226, 208, 170)


def marten_coat(c, i, j, salt):
    if h(i, j, salt + 9) < 0.15:
        c = lerp(c, (150, 110, 70), 0.4)  # glossy, paler-tipped guard hairs
    return fur(c, i, j, salt, 0.07)


def marten_body(top, bottom, chest):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "bottom":
            return marten_coat(P_BIB if chest and z < -2.5 else P_DARK, i, j, 501)
        if face == "front" and chest:
            return fur(P_BIB, i, j, 502, 0.05) if t > 0.35 else marten_coat(P_BROWN, i, j, 503)  # the bib
        if chest and face in ("left", "right") and t > 0.6 and z < -3.5:
            return fur(P_BIB, i, j, 504, 0.05)  # the bib wraps round under the throat
        return marten_coat(lerp(P_BROWN, P_DARK, smooth(t) * 0.7), i, j, 505)
    return mat


def marten_tail(v, face, x, y, z, i, j):
    return marten_coat(lerp(P_BROWN, P_DARK, min(1.0, y / 6)), i, j // 2 * 2 + i % 2, 511)


def marten_skull(v, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 0.5) < 0.1:
            return EYE
        return fur(P_FACE, i, j, 521, 0.06)
    if face == "bottom":
        return fur(P_BIB, i, j, 522, 0.05)
    if face in ("left", "right") and y > 0.2:
        return fur(P_BIB, i, j, 523, 0.05)
    return marten_coat(P_BROWN, i, j, 524)


def marten_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        return NOSE_BLACK
    return fur(P_FACE, i, j, 531, 0.05)


def marten_jaw(v, face, x, y, z, i, j):
    return TOOTH if face == "top" and z < -1.5 else fur(P_BIB, i, j, 541, 0.05)


def marten_ear(v, face, x, y, z, i, j):
    if y < -1 and abs(x) > 0.5:
        return CLEAR  # rounded
    if face == "front":
        return fur(P_CREAM, i, j, 551, 0.04) if abs(x) > 0.4 or y < -0.8 else fur(P_DARK, i, j, 552, 0.05)  # cream rim
    return fur(P_BROWN, i, j, 553, 0.06)


def marten_leg(v, face, x, y, z, i, j):
    return fur(P_DARK, i, j, 561, 0.06)


def marten_foot(v, face, x, y, z, i, j):
    if face == "bottom":
        return PAD
    if face == "front":
        return (60, 46, 36) if i % 2 else (176, 160, 134)  # pale claws
    return fur(P_DARK, i, j, 571, 0.05)


MARTEN = {
    "front": (0, 0, -2, -2, -5, 4, 4, 5, marten_body(-2, 2, True)),
    "rear": (0, 9, -2, -2, 0, 4, 4, 6, marten_body(-2, 2, False)),
    "tail": (20, 0, -1, 0, -1, 2, 8, 2, marten_tail),
    "skull": (28, 0, -2, -2, -3, 4, 3, 3, marten_skull),
    "muzzle": (42, 0, -1, -1, -5, 2, 1, 2, marten_muzzle),
    "jaw": (42, 3, -1, 0, -2, 2, 1, 2, marten_jaw),
    "ear": (50, 0, -1, -1.5, -0.5, 2, 2, 1, marten_ear),
    "fore_upper": (0, 19, -1, -0.5, -1, 2, 2, 2, marten_leg),
    "lower": (8, 19, -1, 0, -1, 2, 1, 2, marten_leg),
    "paw": (16, 19, -1, 1, -2, 2, 1, 3, marten_foot),
    "thigh": (26, 19, -1, -0.5, -1.5, 2, 3, 3, marten_leg),
}


# =============================================================== items
def grid(rows, colour):
    """rows of 16 chars; colour(ch, x, y) -> rgb or None for clear."""
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), [len(r) for r in rows]
    im = Image.new("RGBA", (16, 16), CLEAR)
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                c = colour(ch, x, y)
                if c is not None:
                    im.putpixel((x, y), rgba(c))
    return im


RAW_MEAT = ["#3a0a0b", "#561112", "#701a19", "#862420", "#9a3027", "#ac3f31", "#bf5a46", "#d6826c"]
RAW_PALE = ["#4a1a18", "#6a2826", "#843632", "#9c463e", "#b45a4e", "#c66e60", "#d68a7a", "#e8ae9e"]  # cat meat is pale
COOKED = ["#1c0f09", "#2c190f", "#3d2416", "#4c2e1c", "#5a3722", "#684129", "#774c31", "#a07650"]

# whole small skinned bodies: s flesh, d dark flesh, f fat/sinew, b bone (the legs' ends), o outline
SMALL_LONG = [  # ermine and marten: long and slim, laid out diagonally
    "................",
    "................",
    "............oo..",
    "...........osso.",
    "..........ossdso",
    "........ooossso.",
    ".......osssfsdo.",
    "......ossdssso..",
    ".....ossssfso...",
    "....osdsssso....",
    "...ossssdso.....",
    "..obsssfso......",
    ".obo.osso.......",
    "..o..obo........",
    ".....o..........",
    "................",
]
SMALL_ROUND = [  # lemming: a round little body, head and stubby legs
    "................",
    "................",
    "................",
    "................",
    ".......oooo.....",
    ".....oossssoo...",
    "....ossdssssso..",
    "...osssssfsssso.",
    "...ossfsssdssso.",
    "...osssssssssso.",
    "....osssdsssoo..",
    ".....oobooboo...",
    "......o..o......",
    "................",
    "................",
    "................",
]


def small_meat(rows, ramp, salt):
    def col(ch, x, y):
        if ch == "o":
            return tuple(int(ramp[0][i:i + 2], 16) for i in (1, 3, 5))
        if ch == "b":
            return (226, 214, 190)
        if ch == "f":
            return (234, 210, 180) if ramp is not COOKED else (180, 132, 84)
        k = 3 if ch == "d" else 5 + (h(x, y, salt) < 0.35) - (h(x, y, salt + 1) < 0.2)
        return tuple(int(ramp[k][i:i + 2], 16) for i in (1, 3, 5))
    return grid(rows, col)


def pelt_shape(mask, fur_col, raw, salt):
    """A pelt seen from the fur side (cured) or the flesh side (raw: pale skin, a fringe of fur at the edge)."""
    im = Image.new("RGBA", (16, 16), CLEAR)
    cells = {(x, y) for y, row in enumerate(mask) for x, c in enumerate(row) if c != "."}
    for (x, y) in cells:
        edge = any((x + dx, y + dy) not in cells for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        if raw and not edge:
            c = lerp(HIDE_SIDE, (196, 120, 110), 0.25 * h(x, y, salt) + (0.25 if h(x // 2, y, salt + 1) < 0.2 else 0))
        else:
            c = fur_col(x, y)
            if edge:
                c = shade(c, 0.72)
        im.putpixel((x, y), rgba(c))
    return im


LONG_PELT = [  # a cased small-fur pelt: head at the top, forelegs, body, hind legs, long tail
    "......o..o......",
    "......xxxx......",
    ".......xx.......",
    "....o.xxxx.o....",
    ".....xxxxxx.....",
    "......xxxx......",
    "......xxxx......",
    "......xxxx......",
    "......xxxx......",
    "......xxxx......",
    "....o.xxxx.o....",
    ".....xxxxxx.....",
    ".......xx.......",
    ".......xx.......",
    ".......xx.......",
    ".......tt.......",
]
ROUND_PELT = [
    "................",
    "................",
    "................",
    "................",
    "......xxxx......",
    "....xxxxxxxx....",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "...xxxxxxxxxx...",
    "....xxxxxxxx....",
    "......xxxx......",
    "................",
    "................",
    "................",
    "................",
]


def wolverine_pelt_col(x, y):
    d = abs(x - 7.5)
    c = V_BAND if 2.5 < d < 4.5 and 4 < y < 13 else V_BROWN if d < 2.5 else V_DARK
    if y >= 13:
        c = V_DARK  # the bushy tail
    return fur(c, x, y, 601, 0.08)


def lynx_pelt_col(x, y):
    d = abs(x - 7.5)
    c = L_BACK if d < 1.5 else L_SIDE if d < 4 else L_PALE
    if spot(x * 1.0, y * 1.0, 0.0, 611, 2.0, 0.35) and d > 0.5:
        c = lerp(c, L_SPOT, 0.75)
    if y >= 14:
        c = L_BLACK if y == 15 else L_SIDE
    if y <= 1:
        c = L_BLACK  # ear tufts
    return fur(c, x, y, 612, 0.07)


def ermine_pelt_col(x, y):
    return L_BLACK if y >= 14 else fur(lerp(E_WHITE, E_SNOW, h(x, y, 621) * 0.6), x, y, 622, 0.03)


def marten_pelt_col(x, y):
    if 1 <= y <= 3 and abs(x - 7.5) < 1.6:
        return fur(P_BIB, x, y, 631, 0.05)  # the bib under the chin
    return marten_coat(P_DARK if y >= 12 else P_BROWN, x, y, 632)


def lemming_pelt_col(x, y):
    return fur(M_BLACK if y <= 6 else M_SADDLE if abs(x - 7.5) < 1.5 else M_TAWNY, x, y, 641, 0.08)


CLAW_ROWS = [
    "................",
    "................",
    "....oo..........",
    "...oWWo.....oo..",
    "...oWLWo...oWWo.",
    "....oLMWo..oWLWo",
    ".....oMLo...oLMo",
    ".....oMMo...oMLo",
    "....oMMDo..oMMo.",
    "...oDDDo..oMMDo.",
    "...oKKo...oDDo..",
    "....oo....oKKo..",
    "...........oo...",
    "................",
    "................",
    "................",
]
CLAW_PAL = {"o": (52, 40, 30), "W": (250, 244, 226), "L": (232, 222, 196), "M": (206, 192, 160), "D": (150, 130, 100), "K": (70, 50, 38)}

FLESHER_ROWS = [  # a hooked claw blade lashed to a stick, handle bottom-left
    "................",
    "...........oo...",
    "..........oWWo..",
    "..........oLWo..",
    "...........oLMo.",
    "..........oMMMo.",
    ".........oSSDo..",
    "........oSSo....",
    ".......oTTo.....",
    "......oTTo......",
    ".....oSSo.......",
    "....oSSo........",
    "...oSSo.........",
    "..oSSo..........",
    "..ooo...........",
    "................",
]
FLESHER_PAL = {**CLAW_PAL, "S": (128, 92, 56), "T": (196, 170, 120)}

HOOD_TRIM = [(4, 4), (4, 5), (4, 6), (4, 7), (4, 8), (4, 9), (4, 10), (11, 4), (11, 5), (11, 6), (11, 7), (11, 8), (11, 9), (11, 10),
             (5, 3), (6, 3), (9, 3), (10, 3), (5, 11), (10, 11)]

MANTLE_ROWS = [
    "................",
    "................",
    "....oooooooo....",
    "...oWWWWWWWWo...",
    "..oWWKWWWWKWWo..",
    "..oWWWWLLWWWWo..",
    ".oWWWWLooLWWWWo.",
    ".oWKWWLo.oLWWKWo",
    ".oWWWWo...oWWWWo",
    "oWWWWWo...oWWWWo",
    "oWWKWWo...oWKWWo",
    "oWWWWWo...oWWWWo",
    "oWWWWo.....oWWWo",
    "ooooo.......oooo",
    "................",
    "................",
]

CAP_ROWS = [
    "................",
    "................",
    "................",
    ".....oooooo.....",
    "....oBBbBBBo....",
    "...oBbBBBBbBo...",
    "...oBBBBbBBBo...",
    "..oBBBbBBBBBBo..",
    "..oBBBBBBBbBBo..",
    "..oCCCCCCCCCCo..",
    "..oCcCCCcCCCCo..",
    "...oooooooooo...",
    "................",
    "................",
    "................",
    "................",
]


def item_textures():
    out = {}
    venison = ITEM / "venison_scraps.png"
    out["raw_wolverine"] = recolour(venison, RAW_MEAT)
    out["cooked_wolverine"] = recolour(venison, COOKED)
    out["raw_lynx"] = recolour(venison, RAW_PALE)
    out["cooked_lynx"] = recolour(venison, ["#2a170d", "#3c2214", "#4f2e1b", "#5f3a23", "#6e452b", "#7c5034", "#8b5c3d", "#b48a60"])
    out["raw_ermine"] = small_meat(SMALL_LONG, RAW_PALE, 701)
    out["cooked_ermine"] = small_meat(SMALL_LONG, COOKED, 702)
    out["raw_marten"] = small_meat(SMALL_LONG, RAW_MEAT, 703)
    out["cooked_marten"] = small_meat(SMALL_LONG, COOKED, 704)
    out["raw_lemming"] = small_meat(SMALL_ROUND, RAW_PALE, 705)
    out["cooked_lemming"] = small_meat(SMALL_ROUND, COOKED, 706)
    out["raw_wolverine_pelt"] = pelt_shape(PELT_MASK, wolverine_pelt_col, True, 711)
    out["wolverine_fur"] = pelt_shape(PELT_MASK, wolverine_pelt_col, False, 712)
    out["raw_lynx_pelt"] = pelt_shape(PELT_MASK, lynx_pelt_col, True, 713)
    out["lynx_pelt"] = pelt_shape(PELT_MASK, lynx_pelt_col, False, 714)
    out["raw_ermine_pelt"] = pelt_shape(LONG_PELT, ermine_pelt_col, True, 715)
    out["ermine_fur"] = pelt_shape(LONG_PELT, ermine_pelt_col, False, 716)
    out["raw_marten_pelt"] = pelt_shape(LONG_PELT, marten_pelt_col, True, 717)
    out["marten_fur"] = pelt_shape(LONG_PELT, marten_pelt_col, False, 718)
    out["lemming_pelt"] = pelt_shape(ROUND_PELT, lemming_pelt_col, False, 719)
    out["wolverine_claw"] = grid(CLAW_ROWS, lambda ch, x, y: CLAW_PAL[ch])
    out["claw_flesher"] = grid(FLESHER_ROWS, lambda ch, x, y: FLESHER_PAL[ch])
    hood = Image.open(ITEM / "fur_hood.png").convert("RGBA")
    for p in HOOD_TRIM:  # the frosty ruff round the face opening
        hood.putpixel(p, rgba(lerp(V_BAND, (232, 222, 200), 0.5 * h(*p, 721)) if h(*p, 722) < 0.75 else V_TIP))
    out["wolverine_trimmed_hood"] = hood
    out["ermine_mantle"] = grid(MANTLE_ROWS, lambda ch, x, y: {"o": (70, 64, 58), "K": L_BLACK, "L": HIDE_DRY}.get(ch)
                                or fur(lerp(E_WHITE, E_SNOW, h(x, y, 731)), x, y, 732, 0.03))
    out["marten_fur_cap"] = grid(CAP_ROWS, lambda ch, x, y: {"o": OUTLINE, "b": (150, 110, 70), "c": shade(P_BIB, 0.85)}.get(ch)
                                 or (marten_coat(P_BROWN, x, y, 741) if ch == "B" else fur(P_BIB, x, y, 742, 0.05)))
    out["lynx_rug"] = lynx_rug()
    out.update(eggs())
    return out


def lynx_rug():
    def col(ch, x, y):
        if ch == "N":
            return PINK_NOSE
        if ch == "E":
            return EYE_LYNX
        if ch in "sS":
            return L_PALE
        if ch == "e":
            return L_BLACK  # ears with their tufts
        if ch == "w":
            return CLAW if h(x, y, 751) < 0.6 else shade(CLAW, 0.7)
        if ch == "o":
            return shade(L_BACK, 0.7)
        if ch == "t":
            return L_SIDE
        if ch == "T":
            return L_BLACK
        return lynx_pelt_col(x, min(13, max(2, y)))
    return grid(RUG_MASK, col)


def eggs():
    K = "#1e1a18"
    lynx = egg(["#d8c2a0", "#c2a882", "#a88e6a", "#8a7254", "#5e4c38", "#3e3226"],
               [(4, 6, "#5a4232"), (11, 7, "#5a4232"), (5, 11, "#5a4232"), (10, 12, "#5a4232"), (8, 9, "#5a4232")],
               [(4, 0), (11, 0), (4, 1), (11, 1)],
               [(4, 0, K), (11, 0, K), (4, 1, K), (11, 1, K), (6, 6, "#c0a040"), (9, 6, "#c0a040"),
                (3, 9, "#ece4d4"), (12, 9, "#ece4d4"), (3, 10, K), (12, 10, K)])
    wolverine = egg(["#6a4c36", "#56402e", "#463224", "#36261c", "#241912", "#160f0b"],
                    [(x, 7 + (x - 3) // 4, "#b08a5c") for x in range(3, 13)],
                    [(5, 1), (10, 1)],
                    [(5, 1, "#46342a"), (10, 1, "#46342a")] + [(x, 3, "#948470") for x in range(6, 10)]
                    + [(6, 5, "#100c0a"), (9, 5, "#100c0a")])
    ermine = egg(["#fbfaf6", "#efede6", "#e0ddd2", "#cbc6b8", "#9a958a", "#6a665e"],
                 [], [(7, 15), (8, 15)], [(7, 14, K), (8, 14, K), (7, 15, K), (8, 15, K), (6, 5, K), (9, 5, K), (7, 7, K)])
    lemming = egg(["#d8a462", "#c08848", "#a87036", "#8a5a2a", "#5e3c1c", "#3c2612"],
                  [(7, 9, "#5c3e24"), (8, 10, "#5c3e24"), (7, 11, "#5c3e24")], [],
                  [(x, y, "#262018") for x in range(4, 12) for y in (2, 3, 4)] + [(6, 4, "#e2cfa0"), (9, 4, "#e2cfa0")])
    marten = egg(["#8a5c3a", "#74492c", "#603b22", "#4c2e1a", "#321e10", "#1e120a"],
                 [], [(4, 1), (11, 1)],
                 [(4, 1, "#e2d0aa"), (11, 1, "#e2d0aa"), (6, 5, "#100c0a"), (9, 5, "#100c0a")]
                 + [(x, y, "#e6be70") for x in range(6, 10) for y in (8, 9, 10)])
    return {"lynx_spawn_egg": lynx, "wolverine_spawn_egg": wolverine, "ermine_spawn_egg": ermine,
            "lemming_spawn_egg": lemming, "pine_marten_spawn_egg": marten}


# =============================================================== equipment layers
def hood_layer():
    """The Fur Hood's layer with a thick, frosty wolverine ruff round the face opening: the brow band and the edges of the
    front face of the head, and the front columns of both sides."""
    im = Image.open(EQUIP / "humanoid/fur.png").convert("RGBA")
    px = im.load()
    out = im.copy()
    o = out.load()

    def ruff(x, y):
        o[x, y] = rgba(lerp(V_BAND, (236, 228, 210), 0.6 * h(x, y, 801)) if h(x, y, 802) < 0.72 else V_TIP)

    for y in range(8, 16):
        for x in range(8, 16):  # front face: everything round the opening
            if px[x, y][3] and (y >= 9 or x in (8, 15)):
                ruff(x, y)
        for x in (6, 7, 16, 17):  # the sides, next to the face
            if px[x, y][3]:
                ruff(x, y)
    return out


def recolour_layer(path, colour, region):
    src = vanilla(path)
    px = src.load()
    out = Image.new("RGBA", src.size, CLEAR)
    o = out.load()
    for y in range(src.height):
        for x in range(src.width):
            if px[x, y][3] and region(x, y):
                s = (lum(px[x, y]) - 153) / (214 - 153)
                o[x, y] = rgba(colour(x, y, max(0.0, min(1.0, s))))
    return out


def mantle_colour(x, y, s):
    if h(x // 2, y // 2, 811) < 0.08 and h(x, y, 812) < 0.7:
        return L_BLACK  # the black tail-tips sewn on as spots
    if y in (31, 20) and 40 <= x < 56:
        return HIDE_DRY  # the tanned hem at the cuffs
    return fur(lerp(E_SNOW, E_WHITE, s), x, y, 813, 0.03)


def cap_colour(x, y, s):
    if (y == 15 or y == 14) and x < 32:
        return fur(P_BIB, x, y, 821, 0.05)  # a cream band round the brim
    return marten_coat(lerp(P_DARK, P_BROWN, 0.4 + 0.6 * s), x, y, 822)


def build():
    lynx = paint(LYNX, "adult", (64, 64))
    wolverine = paint(WOLVERINE, "adult", (64, 64))
    ermine_s, ermine_w = paint(ERMINE, "summer", (64, 32)), paint(ERMINE, "winter", (64, 32))
    lemming = paint(LEMMING, "adult", (64, 32))
    marten = paint(MARTEN, "adult", (64, 32))
    for name, im, boxes in (("lynx", lynx, LYNX), ("wolverine", wolverine, WOLVERINE), ("ermine", ermine_s, ERMINE),
                            ("ermine_winter", ermine_w, ERMINE), ("lemming", lemming, LEMMING), ("pine_marten", marten, MARTEN)):
        save(im, f"wildlife/{name}.png")
        save(skinned(im, boxes), f"skinned/wildlife/{name}.png")
    save(paint(LYNX, "kitten", (64, 64)), "wildlife/lynx_kitten.png")
    for name, im in item_textures().items():
        assert im.size == (16, 16) and im.getbbox(), name
        im.save(ITEM / f"{name}.png")
        print("wrote item", name)
    lynx_rug().save(BLOCK / "lynx_rug.png")
    layers = {
        "wolverine_trimmed_hood": hood_layer(),
        "ermine_mantle": recolour_layer("entity/equipment/humanoid/leather", mantle_colour, lambda x, y: y >= 16),
        "marten_fur_cap": recolour_layer("entity/equipment/humanoid/leather", cap_colour, lambda x, y: y < 16),
    }
    for name, im in layers.items():
        assert im.getbbox(), name
        im.save(EQUIP / f"humanoid/{name}.png")
        print("wrote equipment", name)


if __name__ == "__main__":
    build()
