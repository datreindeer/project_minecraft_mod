"""Paint the reindeer textures (bull, cow for ReindeerModel; calf for ReindeerCalfModel) and the skinned adult versions.

Usage: python scripts/gen_reindeer_texture.py
Deterministic. BOXES / CALF_BOXES must match the cubes in ReindeerModel / ReindeerCalfModel (texOffs, origin, size).
Same approach and helpers as gen_moose_texture.py / gen_red_deer_texture.py.
"""
import importlib.util
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"


def _load(name):
    spec = importlib.util.spec_from_file_location(name, ROOT / "scripts" / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


mo = _load("gen_moose_texture")
h, lerp, shade, fur = mo.h, mo.lerp, mo.shade, mo.fur
paint, skinned, save, shaggy = mo.paint, mo.skinned, mo.save, mo.shaggy

SHARED = dict(EYE=(16, 12, 11), GLINT=(190, 196, 204), HOOF=(38, 33, 30), NOSE=(44, 38, 36), WHITE=(238, 235, 226),
              WHITE_SH=(208, 202, 190), EAR_IN=(196, 184, 170), ANTLER_BASE=(88, 70, 50), ANTLER_MID=(176, 156, 122),
              ANTLER_TIP=(232, 222, 196))
# grey-brown body, pale neck and mane, white rump/belly, dark legs with white socks above the hooves
BULL = dict(SHARED, COAT=(104, 88, 74), COAT_DARK=(70, 58, 50), COAT_LIGHT=(134, 118, 102), PALE=(206, 198, 184),
            FACE=(82, 68, 58), FACE_LIGHT=(150, 138, 124), MUZZLE=(184, 176, 164), LEG=(68, 56, 48), LEG_LIGHT=(112, 96, 84))
COW = dict(SHARED, COAT=(118, 104, 90), COAT_DARK=(86, 74, 64), COAT_LIGHT=(146, 132, 116), PALE=(212, 204, 190),
           FACE=(96, 82, 70), FACE_LIGHT=(160, 148, 134), MUZZLE=(190, 182, 170), LEG=(80, 68, 58), LEG_LIGHT=(122, 108, 94))
# calves: plain dark warm brown, a little paler underneath
CALF = dict(SHARED, COAT=(108, 80, 58), COAT_DARK=(80, 58, 42), COAT_LIGHT=(130, 100, 74), PALE=(156, 132, 108),
            FACE=(76, 56, 42), FACE_LIGHT=(120, 96, 76), MUZZLE=(124, 104, 88), LEG=(82, 60, 44), LEG_LIGHT=(110, 84, 64))


def flank(p, y, z, top, bottom):
    """Side of the barrel: dark back, grey-brown flank, a dark band low down, a pale line, then the white belly."""
    t = (y - top) / (bottom - top)
    if t < 0.15:
        c = lerp(p["COAT_DARK"], p["COAT"], t / 0.15)
    elif t < 0.6:
        c = lerp(p["COAT"], p["COAT_LIGHT"], (t - 0.15) / 0.45 * 0.5)
    elif t < 0.78:
        c = lerp(lerp(p["COAT"], p["COAT_LIGHT"], 0.5), p["COAT_DARK"], (t - 0.6) / 0.18)
    elif t < 0.86:
        c = p["PALE"]                                    # the light flank stripe
    else:
        c = p["WHITE"]
    return c


def body_mat(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["WHITE"], i, j, 11, 0.04)
    if face == "back":
        d = (x / 3.6) ** 2 + ((y - 0.8) / 4.2) ** 2
        if d < 1.0:
            return fur(p["WHITE"] if d < 0.8 else p["WHITE_SH"], i, j, 12, 0.03)
        return fur(flank(p, y, z, -4.5, 4.5), i, j, 13)
    if face == "top":
        c = lerp(p["COAT_DARK"], p["COAT"], abs(x) / 4.5 + 0.2)
        if z > 7.5 and abs(x) < 2:
            c = lerp(c, p["WHITE_SH"], 0.4)              # white creeps onto the croup round the tail
        if z < -6:
            c = lerp(c, p["PALE"], (-6 - z) / 4)          # pale towards the neck
        return fur(c, i, j, 14)
    c = flank(p, y, z, -4.5, 4.5)
    if z > 6.5 and y > -2:                                # white rump wraps round the hip
        c = lerp(c, p["WHITE"], min(1.0, (z - 6.5) / 2.5) * (0.9 if y > 0 else 0.5))
    if z < -5:
        c = lerp(c, p["PALE"], (-5 - z) / 5 * (1.0 if y < 2.5 else 0.3))
    return fur(c, i, j, 15)


def chest_mat(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["WHITE_SH"], i, j, 21, 0.04)
    if face == "front":
        return fur(shaggy(lerp(shade(p["PALE"], 0.94), p["WHITE"], (y + 5) / 10 * 0.6), i, j, 22, 0.9), i, j, 23, 0.06)
    if face == "top":
        return fur(p["PALE"], i, j, 24)
    if face == "back":
        return fur(flank(p, y, z, -5, 5), i, j, 25)
    c = flank(p, y, z, -5, 5)
    c = lerp(c, p["PALE"], max(0.0, (-4 - z) / 6) * (1.0 if y < 2 else 0.4))
    return fur(c, i, j, 26)


def neck_mat(p, face, x, y, z, i, j):
    # once tipped forward: +z ("back") is the top of the neck, -z ("front") the throat under the mane
    c = p["PALE"]
    if face == "back":
        c = lerp(p["PALE"], p["COAT_LIGHT"], 0.35)
    elif face == "front":
        c = p["WHITE"]
    elif face in ("left", "right"):
        c = lerp(p["PALE"], p["COAT_LIGHT"], max(0.0, (y + 3) / -5) * 0.5)   # browner towards the head
    return fur(shaggy(c, i, j, 31, 0.92), i, j, 32, 0.06)


def mane_mat(p, face, x, y, z, i, j):
    # long white hair: ragged where it hangs free (front face edge and the head end)
    if face in ("left", "right") and z < -3.5 and h(i, j // 2, 41) < 0.45:
        return (0, 0, 0, 0)
    if face == "front" and y < -4 and h(i, j, 42) < 0.4:
        return (0, 0, 0, 0)
    if face in ("top", "back"):
        return fur(p["PALE"], i, j, 43)
    c = p["WHITE"] if h(i, j // 3, 44) > 0.3 else p["WHITE_SH"]
    return fur(c, i, j, 45, 0.04)


def head_mat(p, face, x, y, z, i, j):
    c = p["FACE"]
    if face == "top":
        return fur(lerp(p["FACE"], p["COAT_DARK"], 0.3), i, j, 51)
    if face == "bottom":
        return fur(p["PALE"], i, j, 52, 0.05)
    if face == "back":
        return fur(lerp(p["FACE"], p["PALE"], 0.5), i, j, 53)
    if face == "front":
        return fur(lerp(p["FACE"], p["COAT_DARK"], 0.2), i, j, 54, 0.05)
    # eye with a glint and a pale ring; pale cheek below
    if abs(y + 1.5) < 0.6 and abs(z + 2.5) < 0.6:
        return p["EYE"]
    if abs(y + 1.5) < 0.6 and abs(z + 1.5) < 0.6:
        return p["GLINT"] if face == "right" else p["EYE"]
    if abs(y + 1.5) < 1.6 and -3.5 < z < -0.5:
        c = lerp(c, p["FACE_LIGHT"], 0.5)
    if y > 0.5:
        c = lerp(c, p["PALE"], 0.35)
    return fur(c, i, j, 55, 0.05)


def snout_mat(p, face, x, y, z, i, j):
    t = (-4 - z) / 4
    m = p["MUZZLE"]
    if face == "front":
        if abs(abs(x) - 1.0) < 0.6 and -1 < y < 0.5:
            return p["NOSE"]                               # nostrils in the furred nose
        if y > 1.0:
            return fur(p["WHITE_SH"], i, j, 61, 0.04)
        return fur(m, i, j, 62, 0.04)
    if face == "top":
        return fur(lerp(p["FACE"], m, t * 0.8), i, j, 63, 0.05)   # dark bridge fading into the pale nose
    if face == "bottom":
        return fur(p["WHITE_SH"], i, j, 64, 0.04)
    if face in ("left", "right"):
        if y > 1.0:
            return fur(lerp(m, p["WHITE_SH"], 0.5), i, j, 65, 0.04)
        if y < -1.0:
            return fur(lerp(p["FACE"], m, t), i, j, 66, 0.05)
    return fur(lerp(p["FACE_LIGHT"], m, t), i, j, 67, 0.05)


def ear_mat(p, face, x, y, z, i, j):
    if face == "front":
        rim = abs(x) > 0.6 or y < -2.4
        return p["FACE"] if rim else fur(p["EAR_IN"], i, j, 71, 0.05)
    return fur(lerp(p["FACE"], p["COAT"], 0.4), i, j, 72)


def tail_mat(p, face, x, y, z, i, j):
    if face in ("top", "back") and y < 1:
        return fur(p["COAT"], i, j, 81)
    return fur(p["WHITE"], i, j, 82, 0.03)


def leg_upper_mat(length, hind):
    def mat(p, face, x, y, z, i, j):
        t = (y + 1) / length
        c = lerp(p["COAT"], p["LEG"], t * 1.3) if hind else lerp(lerp(p["COAT"], p["PALE"], 0.2), p["LEG"], t * 1.2)
        if hind and face == "back" and t < 0.45:
            c = lerp(p["WHITE"], c, t / 0.45)               # white rump runs down the back of the thigh
        if face == "front":
            c = shade(c, 0.9)
        return fur(c, i, j, 90 + int(hind))
    return mat


def leg_lower_mat(length):
    def mat(p, face, x, y, z, i, j):
        if face in ("top", "bottom"):
            return p["LEG"]
        if y > length - 1.2:
            return fur(p["WHITE_SH"], i, j, 95, 0.04)      # white sock above the hoof
        return fur(lerp(p["LEG"], p["LEG_LIGHT"], 0.2 if face != "back" else 0.5), i, j, 96)
    return mat


def hoof_mat(top):
    def mat(p, face, x, y, z, i, j):
        if face == "top":
            return fur(p["WHITE_SH"], i, j, 97, 0.05) if abs(x) < 1.1 and -1.5 < z < 0.5 else p["HOOF"]
        if face == "front" and abs(x) < 0.1:
            return shade(p["HOOF"], 0.6)                      # cleft between the two broad toes
        if face == "back" and y < top + 1:
            return shade(p["HOOF"], 1.3)                      # dewclaws
        return shade(p["HOOF"], 1.0 + 0.16 * (h(i, j, 98) - 0.5))
    return mat


def antler_col(p, t, i, j):
    c = lerp(p["ANTLER_BASE"], p["ANTLER_MID"], t / 0.55) if t < 0.55 else lerp(p["ANTLER_MID"], p["ANTLER_TIP"], (t - 0.55) / 0.45)
    if t < 0.8 and h(i, j, 99) < 0.28:
        c = shade(c, 0.85)
    return c


def beam_mat(t0, t1, length):
    def mat(p, face, x, y, z, i, j):
        if face == "top":
            return antler_col(p, t1, i, j)
        if face == "bottom":
            return antler_col(p, t0, i, j)
        if t0 == 0 and y > -1:
            return shade(p["ANTLER_BASE"], 0.85)          # burr at the skull
        return antler_col(p, t0 + (t1 - t0) * (-y / length), i, j)
    return mat


def plate_mat(t0, t1, length):
    """Palmate crown / brow shovel: grooved faces, pale worn rims."""
    def mat(p, face, x, y, z, i, j):
        if face in ("left", "right"):
            c = antler_col(p, t0 + (t1 - t0) * (-y / length), i, j)
            return shade(c, 0.9) if h(i, j // 2, 100) < 0.2 else c
        return antler_col(p, t1, i, j)
    return mat


# name: (u, v, x0, y0, z0, w, h, d, material) -- mirrors ReindeerModel.createBodyLayer
BOXES = {
    "body": (0, 0, -4.5, -4.5, -9, 9, 9, 18, body_mat),
    "chest": (54, 0, -5, -5, -10, 10, 10, 7, chest_mat),
    "neck": (88, 0, -2.5, -8, -2.5, 5, 9, 5, neck_mat),
    "mane": (54, 17, -2, -7, -4.5, 4, 8, 2, mane_mat),
    "head": (108, 0, -2.5, -3, -4, 5, 5, 5, head_mat),
    "snout": (108, 10, -2, -2, -8, 4, 4, 4, snout_mat),
    "ear": (66, 17, -1, -3, -0.5, 2, 3, 1, ear_mat),
    "tail": (72, 17, -1, 0, 0, 2, 3, 1, tail_mat),
    "beam_low": (78, 17, -0.5, -7, -0.5, 1, 7, 1, beam_mat(0.0, 0.45, 7)),
    "beam_high": (82, 17, -0.5, -7, -0.5, 1, 7, 1, beam_mat(0.4, 0.75, 7)),
    "crown": (86, 17, -0.5, -4, -2, 1, 4, 3, plate_mat(0.6, 0.85, 4)),
    "tine": (94, 17, -0.5, -3, -0.5, 1, 3, 1, beam_mat(0.65, 1.0, 3)),
    "shovel": (96, 21, -0.5, -2, -5, 1, 3, 5, plate_mat(0.45, 0.9, 3)),
    "long_tine": (110, 18, -0.5, -4, -0.5, 1, 4, 1, beam_mat(0.5, 1.0, 4)),
    "front_upper": (0, 27, -1.5, -1, -1.5, 3, 7, 3, leg_upper_mat(7, False)),
    "front_lower": (12, 27, -1, 0, -1, 2, 3, 2, leg_lower_mat(3)),
    "hoof": (20, 27, -1.5, 2, -2, 3, 2, 3, hoof_mat(2)),
    "hind_upper": (32, 27, -1.5, -1, -2, 3, 8, 4, leg_upper_mat(8, True)),
    "hind_lower": (46, 27, -1, 0, -1, 2, 2, 2, leg_lower_mat(2)),
}
ANTLER_BOXES = ("beam_low", "beam_high", "crown", "tine", "shovel", "long_tine")


# --- calf ---------------------------------------------------------------------

def calf_body(p, face, x, y, z, i, j):
    if face == "bottom":
        return fur(p["PALE"], i, j, 151, 0.05)
    if face == "top":
        return fur(lerp(p["COAT_DARK"], p["COAT"], abs(x) / 2.5 * 0.7 + 0.2), i, j, 152)
    if face == "back":
        return fur(lerp(p["COAT"], p["PALE"], 0.5) if abs(x) < 1.6 and y > -1.5 else p["COAT"], i, j, 153)
    return fur(lerp(p["COAT"], p["PALE"], max(0.0, (y - 1.2) / 1.8)), i, j, 154)


def calf_neck(p, face, x, y, z, i, j):
    c = p["COAT_DARK"] if face == "back" else lerp(p["COAT"], p["PALE"], 0.4) if face == "front" else p["COAT"]
    return fur(c, i, j, 155)


def calf_head(p, face, x, y, z, i, j):
    if face in ("left", "right"):
        if abs(y + 0.5) < 0.6 and abs(z + 1.5) < 0.6:
            return p["EYE"]
    c = lerp(p["FACE"], p["COAT_DARK"], 0.3) if face == "top" else p["FACE"]
    return fur(c, i, j, 156, 0.05)


def calf_snout(p, face, x, y, z, i, j):
    if face == "front":
        if abs(abs(x) - 0.75) < 0.4 and -0.5 < y < 0.8:
            return p["NOSE"]
        return fur(p["MUZZLE"] if y < 1.2 else p["PALE"], i, j, 157, 0.05)
    if face == "bottom":
        return fur(p["PALE"], i, j, 158, 0.05)
    return fur(lerp(p["FACE"], p["MUZZLE"], (-3 - z) / 3), i, j, 159, 0.05)


def calf_ear(p, face, x, y, z, i, j):
    if face == "front":
        return p["FACE"] if abs(x) > 0.6 or y < -2.4 else fur(p["EAR_IN"], i, j, 160, 0.05)
    return fur(p["FACE"], i, j, 161)


def calf_leg(upper):
    def mat(p, face, x, y, z, i, j):
        c = p["COAT"] if upper and y < 1 else p["LEG"]
        if not upper and y > 2:
            c = p["PALE"]                                  # pale sock
        return fur(c, i, j, 162)
    return mat


CALF_BOXES = {
    "body": (0, 0, -2.5, -3, -5, 5, 6, 10, calf_body),
    "neck": (30, 0, -1.5, -4, -1.5, 3, 5, 3, calf_neck),
    "head": (42, 0, -2, -2, -3, 4, 4, 4, calf_head),
    "snout": (30, 8, -1.5, -1, -6, 3, 3, 3, calf_snout),
    "ear": (42, 8, -1, -3, -0.5, 2, 3, 1, calf_ear),
    "tail": (48, 8, -1, 0, 0, 2, 2, 1, tail_mat),
    "front_upper": (0, 16, -1, -1, -1, 2, 4, 2, calf_leg(True)),
    "lower": (8, 16, -0.5, 0, -0.5, 1, 3, 1, calf_leg(False)),
    "hoof": (12, 16, -1, 2.5, -1.5, 2, 1, 2, hoof_mat(2.5)),
    "hind_upper": (20, 16, -1, -1, -1.5, 2, 4, 3, calf_leg(True)),
}


def main():
    skinner = mo.rd.load_skinner()
    for variant, palette in (("bull", BULL), ("cow", COW)):
        img = paint(BOXES, palette, (128, 64))
        save(img, TEX / "wildlife" / f"reindeer_{variant}.png")
        save(skinned(img, skinner, BOXES, ANTLER_BOXES), TEX / "skinned" / "wildlife" / f"reindeer_{variant}.png")
    # the young's skin now comes with its model from gen_herd_models.py (the *_BOXES table above is unused)


if __name__ == "__main__":
    main()
