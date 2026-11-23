"""Paint the dry-country warm fauna: tortoise (128x64, TortoiseModel), desert lizard (64x32, DesertLizardModel) and leopard
(128x64, LeopardModel), plus their skinned (flesh) versions for butchered carcasses.

Usage: python scripts/gen_fauna_warm_tex_dry.py   (also run by gen_fauna_warm_textures.py)
Superseded for the entity skins: scripts/gen_owarm_reptiles_models.py now builds the three models (WarmDryGeometry) and
repaints these six files after this script, so the tables below no longer match the game's cubes.
Deterministic. TORTOISE / LIZARD / LEOPARD must match the cubes in the three models (texOffs, origin, size); every texel
is painted from the 3D point it covers (wildlife_paint.py), so scutes, bands and rosettes line up across faces.
Tortoise: gold-ochre carapace, every scute with an olive-brown areola, growth rings and a dark seam, darker blotches;
pale yellow plastron with dark seams; scaly yellow-grey head, wrinkled neck, big-scaled elephantine legs with blunt claws.
Desert lizard: sandy ochre with dark crossbands and pale spots down the back, banded tail, blue throat and cheeks, pale belly.
Leopard: golden-tawny, black rosettes with darker gold centres on body and flanks, solid spots on head, legs and the white
belly, tail spots turning to rings before the dark tip, white muzzle and chin, black-backed ears with a white spot.
"""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, paint, save, shade, skinned, smooth  # noqa: E402

TRANSPARENT = (0, 0, 0, 0)
EYE_BLACK = (18, 14, 12)


def scales(c, i, j, salt, edge=0.72, size=2, var=0.12):
    """Texture-space scale pattern: offset rows of size x size plates with dark grout and per-plate tint."""
    row = j // size
    ii = i + (row % 2)
    col = ii // size
    c = shade(c, 1 + (h(col, row, salt) - 0.5) * var)
    if ii % size == 0 or j % size == 0:
        return shade(c, edge)
    return fur(c, i, j, salt + 1, 0.04, streak=False)


def nearest(p, cell, salt, jit=0.7):
    """Nearest point of a jittered 3D grid (one point per cell): (distance, offset vector, cell key)."""
    base = [math.floor(v / cell) for v in p]
    best = None
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                k = (base[0] + dx, base[1] + dy, base[2] + dz)
                c = [(k[a] + (1 - jit) / 2 + jit * h(k[0] * 31 + k[2] * 7 + a, k[1] * 13 + a, salt)) * cell for a in range(3)]
                off = [p[a] - c[a] for a in range(3)]
                d = math.sqrt(sum(o * o for o in off))
                if best is None or d < best[0]:
                    best = (d, off, k)
    return best


def face_angle(face, off):
    """Angle of an offset around a mark's centre, measured in the plane of the face."""
    if face in ("top", "bottom"):
        return math.atan2(off[2], off[0])
    if face in ("left", "right"):
        return math.atan2(off[1], off[2])
    return math.atan2(off[1], off[0])


# ====================================================================== tortoise
SH_GOLD = (210, 170, 78)
SH_OCHRE = (178, 134, 58)
SH_EDGE = (132, 96, 42)
SH_BLOTCH = (98, 76, 38)
SH_AREOLA = (112, 90, 44)
SH_SEAM = (50, 36, 20)
PL_PALE = (226, 210, 150)
PL_SHADE = (198, 178, 118)
PL_SEAM = (112, 86, 50)
PL_BLOTCH = (88, 66, 38)
T_SKIN = (156, 132, 92)
T_SKIN_D = (112, 92, 62)
T_SKIN_L = (196, 174, 124)
T_HEAD = (190, 164, 104)
T_HORN = (88, 74, 54)
T_CLAW = (150, 130, 98)
T_IRIS = (122, 82, 30)


def scute_colour(r, seam, sid, i, j, salt):
    """r: Chebyshev distance 0 (areola) .. 1 (scute edge); seam: this texel is a seam line; sid tints each scute."""
    if seam:
        return fur(SH_SEAM, i, j, salt, 0.08, streak=False)
    if r < 0.3:
        c = SH_AREOLA if h(sid, i + j, salt + 3) < 0.8 else SH_BLOTCH  # olive-brown areola, the first growth
    else:
        band = int((r - 0.3) * 6.5) % 2  # growth rings
        c = SH_GOLD if band == 0 else SH_OCHRE
        c = lerp(c, SH_EDGE, smooth((r - 0.7) / 0.3) * 0.8)  # each scute darkens towards its seam
    if h(sid, 0, salt + 4) < 0.35 and r > 0.55 and h(i, j, salt + 6) < 0.45:
        c = lerp(c, SH_BLOTCH, 0.6)  # darker blotching on some scutes
    c = shade(c, 0.94 + 0.12 * h(sid, 1, salt + 5))
    return fur(c, i, j, salt + 1, 0.05, streak=False)


def cell(c, bounds):
    """Which interval of the sorted boundaries a texel centre falls in, and whether it is the 1-texel seam just past one."""
    k = sum(1 for b in bounds if c >= b)
    seam = any(0 <= c - b < 1 for b in bounds)
    return k, seam


def ring(c, lo, hi, centre=None):
    """Distance of c from the scute's centre (default the middle of lo..hi), 0..1 at the nearer edge."""
    m = (lo + hi) / 2 if centre is None else centre
    half = (hi - m) if c >= m else (m - lo)
    return min(1.0, abs(c - m) / max(half, 0.5))


# Scute layout. Costals: four per side between these z boundaries, from the rim (y -3) up to the vertebrals; the
# vertebrals run down the middle (|x| < 2.5), the first and last over the front and back of the dome.
COSTAL_Z = [-3.5, 0.0, 3.5]
CZ = [-7.5, *COSTAL_Z, 7.5]
VERT_Z = [-1.5, 1.5]
VZ = [-6.0, -4.5, *VERT_Z, 4.5, 6.0]
COSTAL_Y = (-7.5, -3.0, -5.8)  # top, bottom, areola height


def shell_tier(top_y):
    """Upper dome, costal tier and crown boxes, sharing one scute layout."""
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return fur(SH_EDGE, i, j, 300, 0.05, streak=False)  # hidden inside
        if top_y == -8 and face in ("left", "right"):
            return fur(SH_SEAM, i, j, 301, 0.08, streak=False)  # the vertebral/costal seam round the crown
        yy = top_y - 0.5 if face == "top" else y  # the visible ring of a top face counts as the tier's top row
        vertebral = (face == "top" and (top_y == -8 or abs(x) < 3)) or (face in ("front", "back") and abs(x) < 2.5)
        if top_y == -8 and face in ("front", "back"):
            return fur(SH_SEAM, i, j, 301, 0.08, streak=False)
        if vertebral:
            if face in ("front", "back"):
                k = 0 if face == "front" else 4
                seam = 0 <= abs(x) - 2.0 < 1 or (top_y == -5 and y > -3.5 and False)
                r = max(ring(x, -2.5, 2.5), ring(yy, -7.5, -3.0, -5.8))
                return scute_colour(r, seam, 10 + k, i, j, 302)
            k, seam = cell(z, VERT_Z)
            k += 1
            lo, hi = [(-6.0, -1.5), (-1.5, 1.5), (1.5, 6.0)][k - 1]
            if top_y != -8:  # the dome's top ring outside the crown: front and back ends of the first/last vertebral
                k = 0 if z < 0 else 4
                lo, hi = (-9.0, -4.5) if z < 0 else (4.5, 9.0)
                seam = False
                if abs(x) >= 2.5:
                    seam = True
            r = max(ring(x, -3.0, 3.0), ring(z, lo, hi))
            return scute_colour(r, seam, 10 + k, i, j, 302)
        # costal
        if face in ("front", "back"):
            # the corner costal wraps round onto the front/back face
            k = 0 if face == "front" else 3
            seam = 0 <= 2.5 - abs(x) < 1  # beside the vertebral
            r = max(ring(yy, *COSTAL_Y), min(1.0, 1.0 - (abs(x) - 2.5) / 4.0 * 0.6))
            return scute_colour(r, seam, 20 + k + (4 if x > 0 else 0), i, j, 303)
        k, seam = cell(z, COSTAL_Z)
        r = max(ring(z, CZ[k], CZ[k + 1]), ring(yy, *COSTAL_Y))
        if face == "top":
            seam = seam or abs(x) < 3.5 and top_y == -7  # the ring texel against the crown is the seam
        return scute_colour(r, seam, 20 + k + (4 if x > 0 else 0), i, j, 303)
    return mat


RIM_SIDE = [-4.5, -1.5, 1.5, 4.5]
RIM_END = [-3.5, -1.0, 1.0, 3.5]


def shell_rim(v, face, x, y, z, i, j):
    """Marginal scutes: a ring of small plates round the edge, each with its dark areola at the lower edge."""
    if face == "top":
        return fur(lerp(SH_EDGE, SH_SEAM, 0.6), i, j, 312, 0.07, streak=False)  # the marginal/costal seam
    if face == "bottom":
        on_end = abs(z) > 6.5
        k, seam = cell(x if on_end else z, RIM_END if on_end else RIM_SIDE)
        return fur(PL_SEAM if seam else lerp(PL_SHADE, SH_EDGE, 0.35), i, j, 311, 0.05, streak=False)
    if face in ("left", "right"):
        k, seam = cell(z, RIM_SIDE)
        bounds = [-7.5, *RIM_SIDE, 7.5]
        du = ring(z, bounds[k], bounds[k + 1])
        sid = 40 + k + (8 if x > 0 else 0)
    else:
        k, seam = cell(x, RIM_END)
        bounds = [-6.0, *RIM_END, 6.0]
        du = ring(x, bounds[k], bounds[k + 1])
        sid = 60 + k + (8 if z > 0 else 0)
    r = max(du, ring(y, -3.0, 0.0, -0.5))
    return scute_colour(r, seam, sid, i, j, 313)


def plastron(v, face, x, y, z, i, j):
    if face == "top":
        return PL_SHADE
    if face != "bottom":
        return fur(lerp(PL_SHADE, SH_EDGE, 0.3), i, j, 321, 0.05, streak=False)  # the bridge
    # paired plates: midline seam and transverse seams (gular, humeral, pectoral, abdominal, femoral, anal)
    trans = [-4.5, -2.5, 0.5, 3.5, 5.0]
    k, seam = cell(z, trans)
    seam = seam or 0 <= x < 1
    bounds = [-6.5, *trans, 6.5]
    lo, hi = bounds[k], bounds[k + 1]
    # a dark blotch on each plate, on its inner side against the midline
    if not seam and 0.5 < abs(x) < 2.6 and lo + 0.9 < z < hi - 0.6 and h(k, int(x > 0), 322) < 0.6 and h(i, j, 324) < 0.7:
        return fur(lerp(PL_BLOTCH, PL_SEAM, 0.3), i, j, 325, 0.08, streak=False)
    c = lerp(PL_PALE, PL_SHADE, abs(x) / 5 * 0.7)
    return fur(PL_SEAM if seam else c, i, j, 323, 0.05, streak=False)


def t_neck(v, face, x, y, z, i, j):
    """Loose wrinkled skin: folds across the neck, small scales, paler underneath."""
    c = T_SKIN_L if face == "bottom" else lerp(T_SKIN, T_SKIN_L, (y + 1.5) / 3 * 0.5)
    if face in ("left", "right", "top", "bottom") and int(z * 1.0 + 10) % 2 == 0 and h(i, j, 331) < 0.7:
        c = shade(c, 0.82)  # wrinkle folds
    return fur(c, i, j, 332, 0.07, streak=False)


def t_head(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        if abs(z + 2.5) < 0.1 and abs(y + 0.5) < 0.1:
            return EYE_BLACK
        if abs(z + 1.5) < 0.1 and abs(y + 0.5) < 0.1:
            return T_IRIS
        if abs(z + 2.0) < 0.6 and abs(y + 1.5) < 0.1:
            return shade(T_HEAD, 1.08)  # pale lid scales above the eye
        if y > 0:
            return scales(lerp(T_HEAD, T_SKIN_L, 0.5), i, j, 341, 0.8)
        return scales(T_HEAD, i, j, 342, 0.75)
    if face == "top":
        return scales(shade(T_HEAD, 1.05), i, j, 343, 0.7)  # large prefrontal plates
    if face == "bottom":
        return fur(T_SKIN_L, i, j, 344, 0.05, streak=False)
    if face == "front":
        return fur(T_HEAD, i, j, 345, 0.06, streak=False)
    return fur(T_SKIN, i, j, 346, 0.06, streak=False)


def t_snout(v, face, x, y, z, i, j):
    """Horny beak: the upper jaw's sharp edge darker, nostrils on top."""
    if face == "front":
        return T_HORN if y > -0.5 else shade(T_HEAD, 0.9)
    if face == "top":
        return EYE_BLACK if z < -4.5 else fur(T_HEAD, i, j, 351, 0.05)
    if face == "bottom":
        return shade(T_HORN, 0.8)
    return T_HORN if y > -0.5 else fur(T_HEAD, i, j, 352, 0.05)


def t_leg(spur):
    def mat(v, face, x, y, z, i, j):
        if face == "bottom":
            return fur((70, 60, 46), i, j, 361, 0.06, streak=False)  # sole
        if face == "top":
            return T_SKIN_D
        if y > 2.9 and face == "front":
            return T_CLAW if i % 3 != 1 or not spur else shade(T_CLAW, 0.75)  # blunt claws
        if y > 2.9:
            return fur(T_SKIN_D, i, j, 362, 0.06, streak=False)
        if spur and face == "back" and y < 0.5 and abs(x) < 0.6:
            return shade(T_SKIN_L, 1.05)  # the conical thigh spur
        big = face == "front" or (not spur and face == "right")
        return scales(lerp(T_HEAD, T_SKIN, 0.4 if big else 0.8), i, j, 363 + spur, 0.62 if big else 0.8)
    return mat


def t_tail(v, face, x, y, z, i, j):
    if face == "back":
        return T_HORN  # horny tip
    return scales(T_SKIN, i, j, 371, 0.8)


TORTOISE = {
    "plastron": (0, 0, -5, -0.5, -6.5, 10, 1, 13, plastron),
    "rim": (0, 14, -6, -3, -7.5, 12, 3, 15, shell_rim),
    "costal": (0, 32, -5.5, -5, -7, 11, 2, 14, shell_tier(-5)),
    "dome": (0, 48, -4.5, -7, -6, 9, 2, 12, shell_tier(-7)),
    "crown": (54, 14, -3, -8, -4.5, 6, 1, 9, shell_tier(-8)),
    "neck": (46, 0, -1.5, -1.5, -4, 3, 3, 4, t_neck),
    "head": (60, 0, -1.5, -2, -4, 3, 3, 4, t_head),
    "snout": (74, 0, -1, -1.5, -5, 2, 2, 1, t_snout),
    "front_leg": (80, 0, -1.5, -0.5, -1.5, 3, 4, 3, t_leg(False)),
    "hind_leg": (92, 0, -1.5, -0.5, -1.5, 3, 4, 3, t_leg(True)),
    "tail": (104, 0, -0.5, -0.5, 0, 1, 1, 2, t_tail),
}
TORTOISE_SHELL = ("plastron", "rim", "costal", "dome", "crown")

# ====================================================================== desert lizard
LZ_SAND = (206, 170, 112)
LZ_OCHRE = (178, 134, 78)
LZ_BAND = (112, 80, 50)
LZ_SPOT = (238, 220, 176)
LZ_BELLY = (232, 222, 194)
LZ_BLUE = (84, 118, 170)
LZ_BLUE_L = (138, 166, 204)


def lz_back(t, i, j, salt):
    """Down the side, t 0 (spine) .. 1 (belly)."""
    c = lerp(LZ_OCHRE, LZ_SAND, t / 0.55) if t < 0.55 else lerp(LZ_SAND, LZ_BELLY, smooth((t - 0.55) / 0.45))
    if h(i, j, salt) < 0.12:
        c = shade(c, 1.12)  # spiny scales catching the light
    return fur(c, i, j, salt + 1, 0.06, streak=False)


def lz_body(v, face, x, y, z, i, j):
    if face == "bottom":
        return fur(LZ_BELLY, i, j, 401, 0.04, streak=False)
    t = 0.0 if face == "top" else (y + 1) / 2
    if face == "front":
        return lz_back(0.5 + t * 0.5, i, j, 402)
    c = lz_back(t if face != "top" else abs(x) / 1.5 * 0.3, i, j, 403)
    # dark crossbands, broken at the flanks, with paired pale spots between them
    band = min(abs(z - b) for b in (-2.0, 0.5, 3.0))
    if band < 0.6 and t < 0.6:
        return fur(LZ_BAND, i, j, 404, 0.08, streak=False)
    if face == "top" and abs(abs(x) - 1.0) < 0.1 and min(abs(z - s) for s in (-3.0, -0.75, 1.75)) < 0.6:
        return fur(LZ_SPOT, i, j, 405, 0.04, streak=False)
    if face in ("left", "right") and abs(y + 0.5) < 0.1 and min(abs(z - s) for s in (-0.8, 1.7)) < 0.6:
        return fur(LZ_SPOT, i, j, 406, 0.04, streak=False)  # a lateral row of pale spots
    return c


def lz_head(v, face, x, y, z, i, j):
    if face in ("left", "right"):
        if abs(z + 1.5) < 0.1 and abs(y + 1) < 0.1:
            return EYE_BLACK
        if abs(z + 0.5) < 0.1 and abs(y + 1) < 0.1:
            return fur(LZ_SPOT, i, j, 411, 0.03, streak=False)  # pale eyelid scales behind the eye
        if abs(z + 0.5) < 0.1 and y > -0.5:
            return (62, 50, 40)  # ear opening
        if y > -0.5:
            return fur(lerp(LZ_BLUE_L, LZ_SAND, 0.35), i, j, 412, 0.06, streak=False)  # blue cheeks of the male
        return fur(LZ_SAND, i, j, 413, 0.06, streak=False)
    if face == "top":
        if abs(z + 1.5) < 0.6 and abs(x) < 0.6:
            return fur(LZ_SPOT, i, j, 414, 0.04, streak=False)  # pale parietal spot
        return scales(LZ_OCHRE, i, j, 415, 0.85, size=1 + (i % 2), var=0.1)
    if face == "bottom":
        return fur(LZ_BLUE_L, i, j, 416, 0.06, streak=False)
    return fur(LZ_SAND, i, j, 417, 0.05, streak=False)


def lz_snout(v, face, x, y, z, i, j):
    if face == "front":
        return shade(LZ_OCHRE, 0.85)
    if face == "bottom":
        return fur(LZ_BELLY, i, j, 421, 0.04, streak=False)
    if face in ("left", "right"):
        return (96, 72, 50) if z < -4 else fur(LZ_SAND, i, j, 422, 0.05, streak=False)
    return fur(LZ_OCHRE, i, j, 423, 0.06, streak=False)


def lz_throat(v, face, x, y, z, i, j):
    """The pulsing throat pouch: blue with a darker blue reticulation."""
    c = LZ_BLUE if (i + j) % 2 == 0 else LZ_BLUE_L
    if face == "top":
        c = LZ_BLUE
    return fur(c, i, j, 431, 0.08, streak=False)


def lz_tail(start, w):
    """Tail segment starting `start` px down the tail: rings that tighten towards the tip, pale underneath."""
    def mat(v, face, x, y, z, i, j):
        d = start + z
        if face == "bottom":
            return fur(LZ_BELLY, i, j, 441, 0.04, streak=False)
        if face == "front":
            return lz_back(0.4, i, j, 442)
        if face == "back":
            return fur(LZ_BAND, i, j, 443, 0.08, streak=False) if start >= 12 else lz_back(0.3, i, j, 444)
        if int(d) % 3 == 1 and not (face in ("left", "right") and y > 0.2 and start < 4):
            return fur(lerp(LZ_BAND, LZ_OCHRE, d / 30), i, j, 445, 0.08, streak=False)
        t = 0.0 if face == "top" else (y + w / 2) / w * 0.7
        return lz_back(t, i, j, 446)
    return mat


def lz_limb(v, face, x, y, z, i, j):
    if face == "top":
        return LZ_OCHRE
    if face == "bottom" or face == "left":
        return fur(LZ_BELLY, i, j, 451, 0.04, streak=False)
    if face == "front" and j % 2 == 0:
        return fur(LZ_BAND, i, j, 452, 0.07, streak=False)
    return fur(lerp(LZ_OCHRE, LZ_SAND, 0.4), i, j, 453, 0.08, streak=False)


def lz_foot(v, face, x, y, z, i, j):
    """Flat foot plane, toes splayed: the corners cut away so the long toes read."""
    if (z < -1.5 and x > 0) or (z > -0.5 and x < 0):
        return TRANSPARENT
    if z < -1.5:
        return (88, 70, 52)  # claw tips
    return fur(LZ_SAND if face == "top" else LZ_BELLY, i, j, 461, 0.06, streak=False)


LIZARD = {
    "body": (0, 0, -1.5, -1, -3.5, 3, 2, 7, lz_body),
    "head": (20, 0, -1.5, -1.5, -3, 3, 2, 3, lz_head),
    "snout": (32, 0, -1, -1, -5, 2, 1, 2, lz_snout),
    "throat": (40, 0, -1, 0, -1, 2, 1, 2, lz_throat),
    "tail0": (0, 9, -1, -1, 0, 2, 2, 4, lz_tail(0, 2)),
    "tail1": (12, 9, -1, -0.5, 0, 2, 1, 4, lz_tail(4, 1)),
    "tail2": (24, 9, -0.5, -0.5, 0, 1, 1, 4, lz_tail(8, 1)),
    "tail3": (34, 9, -0.5, -0.5, 0, 1, 1, 3, lz_tail(12, 1)),
    "front_upper": (0, 15, -0.5, 0, -0.5, 1, 2, 1, lz_limb),
    "front_lower": (4, 15, -0.5, 0, -0.5, 1, 2, 1, lz_limb),
    "front_foot": (8, 15, -1, 1.98, -2.5, 2, 0, 3, lz_foot),
    "hind_upper": (0, 21, -0.5, 0, -0.5, 1, 2, 1, lz_limb),
    "hind_lower": (4, 21, -0.5, 0, -0.5, 1, 2, 1, lz_limb),
    "hind_foot": (8, 21, -1, 1.98, -2.5, 2, 0, 3, lz_foot),
}

# ====================================================================== leopard
LP_BACK = (192, 140, 62)
LP_GOLD = (212, 164, 84)
LP_PALE = (228, 196, 136)
LP_WHITE = (240, 234, 218)
LP_CENTRE = (170, 112, 46)
LP_SPOT = (30, 24, 18)
LP_NOSE = (168, 108, 96)
LP_EYE = (206, 184, 70)
TONGUE, TOOTH = (176, 82, 86), (232, 226, 208)
PAD = (44, 34, 30)


def lp_coat(t):
    """Ground colour down the side, t 0 (spine) .. 1 (belly)."""
    if t < 0.45:
        return lerp(LP_BACK, LP_GOLD, t / 0.45)
    if t < 0.75:
        return lerp(LP_GOLD, LP_PALE, (t - 0.45) / 0.3)
    return lerp(LP_PALE, LP_WHITE, smooth((t - 0.75) / 0.2))


def spot(p, cell, radius, salt, chance=0.85):
    d, _off, k = nearest(p, cell, salt)
    return d < radius * (0.8 + 0.4 * h(k[0], k[1] * 7 + k[2], salt + 1)) and h(k[2], k[0] + k[1], salt + 2) < chance


def nearest2(u, w, cell, salt, jit=0.7):
    """Nearest point of a jittered 2D grid: (distance, du, dw, cell key)."""
    bu, bw = math.floor(u / cell), math.floor(w / cell)
    best = None
    for du in (-1, 0, 1):
        for dw in (-1, 0, 1):
            k = (bu + du, bw + dw)
            cu = (k[0] + (1 - jit) / 2 + jit * h(k[0], k[1], salt)) * cell
            cw = (k[1] + (1 - jit) / 2 + jit * h(k[1] + 17, k[0], salt + 1)) * cell
            d = math.hypot(u - cu, w - cw)
            if best is None or d < best[0]:
                best = (d, u - cu, w - cw, k)
    return best


def rosette(u, w, base, i, j, salt, cell=4.3):
    """Ring of black spots, broken into 3-5 pieces, round a darker gold centre; None between rosettes. (u, w) is a point
    on the unwrapped coat: along the body and round its cross-section."""
    d, du, dw, k = nearest2(u, w, cell, salt)
    s = 0.85 + 0.3 * h(k[0] + 5, k[1] * 3, salt + 2)
    r_in, r_out = 0.9 * s, 2.0 * s
    if d < r_in:
        return fur(lerp(base, LP_CENTRE, 0.8), i, j, salt + 3, 0.06)
    if d < r_out:
        n = 3 + int(h(k[1], k[0] * 5, salt + 4) * 3)
        a = (math.atan2(dw, du) / (2 * math.pi) + 0.5 + h(k[1] + 3, k[0], salt + 5)) * n
        if a % 1.0 < 0.74:
            return fur(LP_SPOT, i, j, salt + 6, 0.1)
    return None


def unwrap(face, x, y, z, half_w, top, bottom):
    """Arc position round a box's cross-section from the spine (0): top, down the flank, under the belly."""
    hgt = bottom - top
    if face == "top":
        return x
    side = 1 if x > 0 else -1
    if face in ("left", "right"):
        return side * (half_w + (y - top))
    if face == "bottom":
        return side * (half_w + hgt + (half_w - abs(x)))
    return x * 1.2 + (y - top) * (1 if x >= 0 else -1)  # front/back: good enough on the small end faces


def lp_body(half_w, top, bottom):
    def mat(v, face, x, y, z, i, j):
        p = (x, y, z)
        if face == "bottom":
            return LP_SPOT if spot(p, 2.4, 0.75, 501, 0.6) else fur(LP_WHITE, i, j, 502, 0.04)
        t = 0.0 if face == "top" else (y - top) / (bottom - top)
        if face == "top":
            t = abs(x) / 4 * 0.25
        base = lp_coat(t)
        if face == "front" and z < -11.5:  # front of the neck: pale throat
            base = lp_coat(0.3 + t * 0.7)
        if face == "top" and abs(x) < 1.6 or face in ("front", "back") and t < 0.2:
            if spot(p, 2.0, 0.6, 503, 0.75):
                return fur(LP_SPOT, i, j, 504, 0.08)  # small solid spots along the spine
        elif t < 0.74:
            c = rosette(z, unwrap(face, x, y, z, half_w, top, bottom), base, i, j, 505)
            if c:
                return c
        elif spot(p, 2.3, 0.7, 506, 0.7):
            return fur(LP_SPOT, i, j, 507, 0.08)  # the belly's solid spots
        return fur(base, i, j, 508, 0.06)
    return mat


def lp_tail(start):
    """Tail segment starting `start` px down the tail (20 long): spots, then rings, then the black-topped, white-tipped end."""
    def mat(v, face, x, y, z, i, j):
        d = start + y
        under = face == "front" or face == "top"
        base = lerp(LP_GOLD, LP_PALE, 0.7) if under else LP_GOLD
        if d > 18.5:
            return fur(LP_SPOT if not under else LP_WHITE, i, j, 511, 0.08)
        if d > 11:
            ring = (d - 11) % 2.5 < 1.0
            return fur(LP_SPOT, i, j, 512, 0.08) if ring else fur(base, i, j, 513, 0.06)
        if spot((x * 1.2, y, z * 1.2), 1.9, 0.62, 514, 0.8):
            return fur(LP_SPOT, i, j, 515, 0.08)
        return fur(base, i, j, 516, 0.06)
    return mat


def lp_skull(v, face, x, y, z, i, j):
    p = (x, y, z)
    if face == "front":
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 2) < 0.1:
            return LP_EYE
        if abs(abs(x) - 2.5) < 0.1 and abs(y + 2) < 0.1:
            return EYE_BLACK  # dark outer corner / pupil slit
        if abs(abs(x) - 1.5) < 0.1 and abs(y + 1) < 0.1:
            return fur(LP_WHITE, i, j, 521, 0.03)  # pale patch under the eye
        if abs(abs(x) - 2.5) < 0.1 and abs(y + 1) < 0.1:
            return LP_SPOT  # cheek line running down from the eye
        if y > 0.5:
            return fur(LP_WHITE, i, j, 522, 0.03)
        if y < -2.5 and spot(p, 1.6, 0.45, 523, 0.7):
            return LP_SPOT
        return fur(LP_GOLD, i, j, 524, 0.05)
    if face == "top":
        return LP_SPOT if spot(p, 1.6, 0.5, 525, 0.75) else fur(LP_BACK, i, j, 526, 0.05)
    if face == "bottom":
        return fur(LP_WHITE, i, j, 527, 0.03)
    if face == "back":
        return LP_SPOT if spot(p, 1.8, 0.5, 528) else fur(LP_BACK, i, j, 529, 0.05)
    if y > 0.5:
        return fur(LP_WHITE, i, j, 530, 0.03)
    return LP_SPOT if spot(p, 1.7, 0.5, 531, 0.7) else fur(lerp(LP_GOLD, LP_PALE, 0.3 if y > -1 else 0.0), i, j, 532, 0.05)


def lp_muzzle(v, face, x, y, z, i, j):
    if face == "front":
        if y < -0.5:
            return LP_NOSE if abs(x) < 1 else fur(LP_WHITE, i, j, 541, 0.03)
        return LP_SPOT if abs(x) < 1 else fur(LP_WHITE, i, j, 542, 0.03)  # philtrum and lip
    if face == "top":
        return LP_NOSE if z < -7 and abs(x) < 1 else fur(lerp(LP_GOLD, LP_BACK, 0.4), i, j, 543, 0.05)
    if face == "bottom":
        return shade(TONGUE, 0.8)
    if y > -0.5:
        return LP_SPOT if z < -6 else fur(LP_WHITE, i, j, 544, 0.03)  # black lip line
    return LP_SPOT if (i + j) % 2 == 0 and z < -5.5 else fur(LP_WHITE, i, j, 545, 0.03)  # whisker spots on the pads


def lp_jaw(v, face, x, y, z, i, j):
    if face == "top":
        return TOOTH if z < -2 or (abs(x) > 1 and z < -1) else TONGUE
    return fur(LP_WHITE, i, j, 551, 0.03)


def lp_ear(v, face, x, y, z, i, j):
    """Small round ears: outer top corner cut away; pale furred inside, black back with a white spot."""
    if y < -1 and x < 0 and face in ("front", "back", "top", "right"):
        return TRANSPARENT
    if face == "front":
        return fur(LP_WHITE, i, j, 561, 0.05)
    if face == "back" and y > -1 and x < 0.5:
        return fur(LP_WHITE, i, j, 562, 0.03)
    return fur(LP_SPOT, i, j, 563, 0.1)


def lp_leg(top, bottom):
    def mat(v, face, x, y, z, i, j):
        t = (y - top) / (bottom - top)
        if face == "top":
            return LP_GOLD
        inner = face == "left"  # inner side (mirrored onto the left leg)
        c = lerp(LP_GOLD, LP_PALE, t * 0.6) if not inner else lerp(LP_PALE, LP_WHITE, 0.5)
        if t < 0.3 and not inner and face != "back":
            m = rosette(y * 1.2, unwrap(face, x, y, z, 1.5, -2, 5) * 1.2, c, i, j, 571, cell=3.6)
            if m:
                return m
        if spot((x, y, z), 1.9, 0.6, 572, 0.8):
            return fur(LP_SPOT, i, j, 573, 0.08)
        return fur(c, i, j, 574, 0.05)
    return mat


def lp_lower(v, face, x, y, z, i, j):
    if y >= 5:  # big paw
        if face == "bottom":
            return PAD
        if face == "front":
            return LP_SPOT if abs(x) < 0.4 else fur(LP_PALE, i, j, 581, 0.04)  # the gap between the toes
        return fur(LP_PALE, i, j, 582, 0.04)
    if face == "left":
        return fur(LP_WHITE, i, j, 583, 0.03)
    if spot((x, y, z), 1.7, 0.5, 584, 0.8):
        return LP_SPOT
    return fur(lerp(LP_GOLD, LP_PALE, y / 5), i, j, 585, 0.05)


LEOPARD = {
    "chest": (0, 0, -3.5, -4, -9, 7, 8, 10, lp_body(3.5, -4, 4)),
    "loin": (0, 18, -3, -3.5, 1, 6, 7, 9, lp_body(3, -3.5, 3.5)),
    "shoulders": (34, 0, -4, -3, -8, 8, 6, 5, lp_body(4, -4, 4)),
    "neck": (60, 0, -2.5, -3.5, -12, 5, 6, 4, lp_body(2.5, -3.5, 2.5)),
    "tail0": (34, 11, -1, 0, -1, 2, 8, 2, lp_tail(0)),
    "tail1": (42, 11, -1, 0, -1, 2, 7, 2, lp_tail(8)),
    "tail2": (50, 11, -1, 0, -1, 2, 5, 2, lp_tail(15)),
    "skull": (78, 0, -3, -3.5, -4.5, 6, 5, 5, lp_skull),
    "muzzle": (100, 0, -2, -1.5, -7.5, 4, 2, 3, lp_muzzle),
    "jaw": (100, 5, -1.5, 0, -3, 3, 1, 3, lp_jaw),
    "ear": (114, 0, -1, -2, -0.5, 2, 2, 1, lp_ear),
    "fore_upper": (60, 10, -1.5, -2, -2, 3, 6, 4, lp_leg(-2, 4)),
    "lower": (74, 10, -1, 0, -1, 2, 5, 2, lp_lower),
    "paw": (82, 10, -1.5, 5, -2.5, 3, 1, 4, lp_lower),
    "thigh": (30, 21, -1.5, -2, -2.5, 3, 7, 5, lp_leg(-2, 5)),
}


def main():
    tortoise = paint(TORTOISE, "tortoise", (128, 64))
    save(tortoise, "wildlife/tortoise.png")
    save(skinned(tortoise, TORTOISE, keep=TORTOISE_SHELL), "skinned/wildlife/tortoise.png")
    lizard = paint(LIZARD, "lizard", (64, 32))
    save(lizard, "wildlife/desert_lizard.png")
    save(skinned(lizard, LIZARD, keep=("front_foot", "hind_foot")), "skinned/wildlife/desert_lizard.png")
    leopard = paint(LEOPARD, "leopard", (128, 64))
    save(leopard, "wildlife/leopard.png")
    save(skinned(leopard, LEOPARD, keep=("paw",)), "skinned/wildlife/leopard.png")


if __name__ == "__main__":
    main()
