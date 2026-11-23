"""The savanna fauna's models and skins, each from one table (wildlife/savanna/SavannaFauna, docs/fauna/savanna.md):

- src/main/java/com/mechanical/client/wildlife/savanna/SavannaGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the skinned carcass skins in textures/entity/skinned/wildlife/

Spotted Hyena (sloping back, heavy forequarters and neck with a dark crest, big blunt head, round ears, spotted legs),
Cheetah (small round head with tear marks, deep chest, wasp waist, long thin legs, ringed J-tail), Gazelle (slim, rufous
with the black flank band and white rump, face stripes, ringed lyre horns), Ostrich cock / hen / chick (a big round
feather body with drooping wing and tail plumes, a long bare neck from a fluffy collar, flat head with big eyes and a
broad beak, bare drumsticks and scaled shanks on two-toed feet) and Aardvark (arched back, long tube snout with a pig
disc, tall ears, thick tapering tail, short dark legs with spade claws).

Same idea as gen_elkwight_model.py: every box is packed into the skin and painted from the 3D point each texel covers.
Part names are the ones the animation code looks up (PredatorModel, GazelleModel, OstrichModel, AardvarkModel).
Also the Cave Lion (an older mob, seen resting in the savanna shade): a broad-headed, deep-chested maneless cat with a
faint throat ruff, ghost stripes and a dark tail tuft (client/wildlife/CaveLionModel).
Usage: python scripts/gen_savanna_models.py (after gen_fauna_savanna_textures.py and gen_predator_textures.py). Deterministic.
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import face_texels, fur, h, lerp, save, shade, skinned, smooth  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/savanna/SavannaGeometry.java"
CLEAR = (0, 0, 0, 0)


def spot(p, cell, radius, salt, chance=0.8, stretch=(1, 1, 1)):
    """Solid round spots on a jittered 3D grid (model space, so they line up across faces and boxes)."""
    p = [p[a] * stretch[a] for a in range(3)]
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


class Model:
    """Parts (id -> parent, pivot, rotation, name in the game) and integer-sized boxes, each with its own skin area."""

    def __init__(self, name):
        self.name, self.parts, self.boxes = name, {}, []

    def part(self, pid, parent, at=(0, 0, 0), rot=(0, 0, 0), name=None):
        assert pid not in self.parts, pid
        self.parts[pid] = (parent, at, rot, name or pid)

    def box(self, part, mat, x, y, z, w, hh, d):
        assert part in self.parts and all(float(v).is_integer() and v > 0 for v in (w, hh, d)), (part, mat)
        self.boxes.append(dict(part=part, mat=mat, x=x, y=y, z=z, w=int(w), h=int(hh), d=int(d)))

    def origin(self, pid):
        """The part's pivot in model space, rotations ignored (good enough to line markings up)."""
        x = y = z = 0
        while pid != "root":
            parent, at, _, _ = self.parts[pid]
            x, y, z = x + at[0], y + at[1], z + at[2]
            pid = parent
        return x, y, z

    def pack(self):
        for size in ((64, 64), (128, 64), (128, 128), (256, 128), (256, 256)):
            if self._pack(*size):
                self.size = size
                return
        raise AssertionError(self.name + ": texture full")

    def _pack(self, W, H):
        x = y = shelf = 0
        for b in sorted(self.boxes, key=lambda b: (-(b["d"] + b["h"]), -(b["d"] + b["w"]))):
            bw, bh = 2 * (b["d"] + b["w"]), b["d"] + b["h"]
            if bw > W:
                return False
            if x + bw > W:
                x, y, shelf = 0, y + shelf, 0
            b["uv"] = (x, y)
            x, shelf = x + bw, max(shelf, bh)
        return y + shelf <= H

    def paint(self, colour, variant):
        """colour(variant, mat, face, local point, model point, box) -> rgb or rgba; then light from above."""
        img = Image.new("RGBA", self.size, CLEAR)
        px = img.load()
        for b in self.boxes:
            o = self.origin(b["part"])
            for face, tx, ty, x, y, z in face_texels(*b["uv"], b["x"], b["y"], b["z"], b["w"], b["h"], b["d"]):
                c = colour(variant, b["mat"], face, (x, y, z), (x + o[0], y + o[1], z + o[2]), b)
                if len(c) == 4 and c[3] == 0:
                    continue
                f = 1.06 if face == "top" else 0.82 if face == "bottom" else 1.02 - 0.14 * (y - b["y"]) / b["h"]
                px[tx, ty] = (*shade(fur(c[:3], tx, ty, 7 + len(b["mat"]), 0.045), f), 255)
        return img

    def skinned(self, img, keep=()):
        table = {i: (*b["uv"], b["x"], b["y"], b["z"], b["w"], b["h"], b["d"], None) for i, b in enumerate(self.boxes)}
        return skinned(img, table, keep=[i for i, b in enumerate(self.boxes) if b["mat"] in keep])

    def java(self, method):
        order, out = ["root"], []
        while len(order) <= len(self.parts):
            for pid, (parent, _, _, _) in self.parts.items():
                if pid not in order and parent in order:
                    order.append(pid)
        for pid in order[1:]:
            parent, at, rot, name = self.parts[pid]
            cl = "CubeListBuilder.create()"
            for b in (b for b in self.boxes if b["part"] == pid):
                cl += (f"\n                .texOffs({b['uv'][0]}, {b['uv'][1]}).addBox({jf(b['x'])}, {jf(b['y'])}, {jf(b['z'])}, "
                       f"{jf(b['w'])}, {jf(b['h'])}, {jf(b['d'])})")
            pose = f"PartPose.offsetAndRotation({', '.join(jf(v) for v in (*at, *rot))})"
            out.append(f"        PartDefinition {pid} = {parent}.addOrReplaceChild(\"{name}\", {cl},\n                {pose});")
        return (f"    /** {self.name}: {len(self.parts)} parts, {len(self.boxes)} boxes. */\n    public static LayerDefinition {method}() {{\n"
                "        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n"
                + "\n".join(out) + f"\n        return LayerDefinition.create(mesh, {self.size[0]}, {self.size[1]});\n    }}\n")


def jf(v):
    s = f"{round(v, 4):.4f}".rstrip("0")
    return (s + "0" if s.endswith(".") else s) + "F"


def rel(v, lo, hi):
    return max(0.0, min(1.0, (v - lo) / (hi - lo)))


def at_front(face, p, b):
    return face == "front" or p[2] < b["z"] + 1


def side_of(face):
    return face in ("left", "right", "front", "back")


def rounded(face, p, b, corners=1):
    """Transparent top corners on the flat faces of a thin ear/plume box, for a rounded tip."""
    edge = p[0] < b["x"] + 1 or p[0] > b["x"] + b["w"] - 1
    if face in ("front", "back", "top") and p[1] < b["y"] + corners and edge:
        return True
    return face in ("left", "right") and p[1] < b["y"] + corners


# ============================================================================ legs shared by the cats and the hyena
def predator_legs(m, front, hind, upper, hind_upper, paw_w=2, lw=2, paw_d=3):
    """Front/hind leg pairs with a 'lower' child (overlapping the upper by 1) and a paw on the ground, the names
    PredatorModel looks up."""
    lower_len = 23 - (front[1] + upper[1] + upper[4] - 1)
    hind_lower_len = 23 - (hind[1] + hind_upper[1] + hind_upper[4] - 1)
    assert float(lower_len).is_integer() and float(hind_lower_len).is_integer()
    for side, s in ((-1, "right"), (1, "left")):
        fx, fy, fz = front
        m.part(f"{s}_front_leg", "root", (side * fx, fy, fz))
        m.box(f"{s}_front_leg", "upper", *upper)
        m.part(f"{s[0]}f_lower", f"{s}_front_leg", (0, upper[1] + upper[4] - 1, 0), name="lower")
        m.box(f"{s[0]}f_lower", "lower", -lw / 2, 0, -lw / 2, lw, lower_len, lw)
        m.box(f"{s[0]}f_lower", "paw", -paw_w / 2, lower_len, -lw / 2 - 1, paw_w, 1, paw_d)
        hx, hy, hz = hind
        m.part(f"{s}_hind_leg", "root", (side * hx, hy, hz))
        m.box(f"{s}_hind_leg", "thigh", *hind_upper)
        m.part(f"{s[0]}h_lower", f"{s}_hind_leg", (0, hind_upper[1] + hind_upper[4] - 1, 0.5), name="lower")
        m.box(f"{s[0]}h_lower", "lower", -lw / 2, 0, -lw / 2, lw, hind_lower_len, lw)
        m.box(f"{s[0]}h_lower", "paw", -paw_w / 2, hind_lower_len, -lw / 2 - 1, paw_w, 1, paw_d)


# ============================================================================ cave lion
def lion_model():
    m = Model("Cave lion")
    m.part("body", "root", (0, 10, 0))
    m.box("body", "chest", -4, -5, -10, 8, 9, 10)          # deep chest and powerful shoulders
    m.box("body", "shoulders", -3.5, -5.8, -9, 7, 1, 7)
    m.box("body", "ruff", -3.5, -3, -12, 7, 8, 2)          # the faint ruff down the throat and chest
    m.box("body", "belly", -3.5, -4.5, 0, 7, 8, 6)
    m.box("body", "pouch", -3, 3.5, -2, 6, 1, 7)           # the loose belly skin that swings as it walks
    m.box("body", "hips", -4, -5, 6, 8, 9, 5)
    m.part("neck", "body", (0, -2, -10), (-0.25, 0, 0))
    m.box("neck", "neck", -3, -3.5, -4, 6, 7, 4)
    m.part("head", "body", (0, -2.5, -13))
    m.box("head", "skull", -3.5, -4, -5, 7, 6, 5)
    m.box("head", "brow", -3, -4.6, -5.4, 6, 2, 1)         # the heavy brow over the eyes
    m.box("head", "cheeks", -4, -1, -4.5, 8, 3, 4)         # broad jowls
    m.box("head", "muzzle", -2, -1.5, -8, 4, 3, 3)
    m.box("head", "nose", -1, -1.8, -8.4, 2, 1, 1)
    m.part("jaw", "head", (0, 1.5, -5))
    m.box("jaw", "jaw", -2, 0, -3, 4, 1, 3)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "head", (side * 2.6, -3.8, -2), (0, 0, side * 0.2))
        m.box(f"{s}_ear", "ear", -1, -2, -0.5, 2, 2, 1)
    m.part("tail", "body", (0, -3.5, 10.5), (0.45, 0, 0))
    m.box("tail", "tail", -1, 0, -1, 2, 8, 2)
    m.part("tail_end", "tail", (0, 8, 0), (0.3, 0, 0))
    m.box("tail_end", "tail", -1, 0, -1, 2, 6, 2)
    m.box("tail_end", "tuft", -1.5, 5, -1.5, 3, 3, 3)
    predator_legs(m, (2.6, 13, -6.5), (2.6, 12, 8), (-2, -2, -2, 4, 7, 4), (-2, -2, -3, 4, 8, 5), paw_w=4, lw=3, paw_d=4)
    return m


LI = dict(back=(140, 110, 74), coat=(184, 152, 106), pale=(214, 190, 146), belly=(228, 214, 184), stripe=(132, 104, 70),
          white=(238, 232, 218), dark=(54, 42, 34), nose=(160, 102, 92), eye=(204, 150, 52), pupil=(30, 22, 18),
          ear_back=(44, 36, 30), pad=(70, 56, 48))


def lion_colour(v, mat, face, p, P, b):
    x, y, z = p
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 5, 15)
    coat = lerp(lerp(LI["back"], LI["coat"], t / 0.4), LI["pale"], (t - 0.55) / 0.3) if t < 0.85 else LI["belly"]
    # faint ghost stripes down the flanks and thighs, as cave art shows them
    stripe = side_of(face) and face not in ("front", "back") and t < 0.7 and \
        math.sin(P[2] * 0.9 + 0.8 * math.sin(P[1] * 0.5)) > 0.82
    if mat in ("chest", "shoulders", "belly", "hips", "neck", "pouch"):
        if mat == "neck" and face == "front":
            return LI["pale"]
        c = lerp(coat, LI["stripe"], 0.45) if stripe else coat
        return lerp(c, LI["back"], 0.35 * h(int(P[0] * 2), int(P[2]), 91)) if face == "top" else c
    if mat == "ruff":
        return lerp(LI["pale"], LI["coat"], 0.5 + 0.3 * h(int(x * 2), int(y), 92) + (0.2 if y < -1 else 0))  # only a faint paler ruff
    if mat in ("skull", "brow"):
        if mat == "brow":
            return lerp(LI["coat"], LI["back"], 0.4) if face != "bottom" else LI["dark"]
        if face == "front":
            if -3 <= y < -2 and (b["x"] + 1 <= x < b["x"] + 3 or b["x"] + 4 <= x < b["x"] + 6):
                # amber eyes with a dark pupil on the inner texel
                inner = b["x"] + 2 <= x < b["x"] + 3 or b["x"] + 4 <= x < b["x"] + 5
                return LI["pupil"] if inner else LI["eye"]
            if -2 <= y < -1 and (b["x"] + 1 <= x < b["x"] + 3 or b["x"] + 4 <= x < b["x"] + 6):
                return LI["white"]  # pale under the eyes
            return lerp(LI["coat"], LI["back"], 0.3)
        if face in ("left", "right") and z < b["z"] + 1 and -3 <= y < -2:
            return LI["eye"]
        return lerp(LI["coat"], LI["back"], 0.45) if face == "top" else LI["coat"]
    if mat == "cheeks":
        if face == "front" or (face in ("left", "right") and z < b["z"] + 1.5):
            return LI["white"] if y > 0 else LI["pale"]
        return lerp(LI["coat"], LI["pale"], rel(y, -1, 2))
    if mat == "muzzle":
        if face == "front":
            return LI["white"] if y > -0.5 else lerp(LI["pale"], LI["coat"], 0.3)
        if face in ("left", "right"):
            if y > 0.5 and z < -7:
                return lerp(LI["dark"], LI["white"], 0.35)  # the corner of the mouth
            if -0.5 <= y < 0.5 and z < -6:
                return lerp(LI["dark"], LI["white"], 0.55)  # the dark whisker pads
            return LI["white"] if y > -0.5 else LI["pale"]
        return lerp(LI["coat"], LI["pale"], 0.4)
    if mat == "nose":
        return LI["nose"] if face != "bottom" else LI["dark"]
    if mat == "jaw":
        return LI["white"]
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        return LI["ear_back"] if face == "back" else lerp(LI["coat"], LI["pale"], 0.5)
    if mat == "tail":
        return coat if face != "bottom" else LI["pale"]
    if mat == "tuft":
        return lerp(LI["dark"], LI["ear_back"], h(int(P[0] * 3), int(P[1] * 2), 93))
    if mat in ("upper", "thigh", "lower"):
        inner = (face == "left" and P[0] < 0) or (face == "right" and P[0] > 0) or face == "back"
        c = lerp(LI["coat"], LI["pale"], 0.55 if inner else 0.3 * rel(P[1], 12, 23))
        if mat == "thigh" and stripe:
            c = lerp(c, LI["stripe"], 0.4)
        return c
    if mat == "paw":
        if face == "front" and int(x - b["x"]) % 2 == 1:
            return lerp(LI["pale"], LI["back"], 0.4)
        return LI["pale"] if face != "bottom" else LI["pad"]
    return coat


# ============================================================================ spotted hyena
def hyena_model():
    m = Model("Spotted hyena")
    m.part("body", "root", (0, 13, 0))
    m.box("body", "chest", -3.5, -2, -8, 7, 7, 8)          # deep, high forequarters
    m.box("body", "hump", -3, -3, -7.5, 6, 1, 7)
    m.box("body", "crest", -1, -4, -7, 2, 1, 6)            # the bristly dark mane over the shoulders
    m.part("haunch", "body", (0, -1, 0), (-0.2, 0, 0))     # the back slopes down to a low rump
    m.box("haunch", "back", -3, -1, 0, 6, 6, 5)
    m.box("haunch", "rump", -2.5, 0, 5, 5, 5, 3)
    m.part("neck", "body", (0, -0.5, -7.5), (-0.45, 0, 0))
    m.box("neck", "neck", -2.5, -2.5, -5, 5, 5, 5)
    m.box("neck", "neck_crest", -1, -3.5, -4.5, 2, 1, 5)
    m.part("head", "body", (0, -1.5, -11))
    m.box("head", "skull", -3, -3, -4, 6, 5, 4)
    m.box("head", "cheek", -3.5, -1, -3.5, 7, 3, 3)        # the heavy jaw muscles
    m.box("head", "muzzle", -2, -1.5, -7, 4, 3, 3)
    m.box("head", "nose", -1, -1.8, -7.4, 2, 1, 1)
    m.part("jaw", "head", (0, 1.5, -4))
    m.box("jaw", "jaw", -1.5, 0, -3, 3, 1, 3)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "head", (side * 2.2, -2.6, -1.5), (0, 0, side * 0.25))
        m.box(f"{s}_ear", "ear", -1.5, -3, -0.5, 3, 3, 1)
    m.part("tail", "body", (0, 0.5, 8), (0.55, 0, 0))
    m.box("tail", "tail", -1, 0, -1, 2, 5, 2)
    m.box("tail", "tuft", -1.5, 4, -1.5, 3, 3, 3)
    predator_legs(m, (2.2, 14, -5), (2.0, 15.5, 6), (-1.5, -2, -1.5, 3, 6, 3), (-1.5, -1.5, -2.5, 3, 5, 4), paw_w=3)
    return m


HY = dict(back=(150, 122, 84), coat=(190, 158, 108), pale=(214, 192, 148), belly=(226, 208, 170), spot=(66, 44, 30),
          dark=(52, 40, 32), muzzle=(112, 86, 62), nose=(22, 18, 16), eye=(120, 82, 34), ear_in=(196, 170, 134),
          tooth=(232, 226, 208), pad=(44, 36, 30))


def hyena_colour(v, mat, face, p, P, b):
    x, y, z = p
    t = rel(P[1], 10.5, 18.5)
    coat = lerp(lerp(HY["back"], HY["coat"], t / 0.45), HY["pale"], (t - 0.45) / 0.4) if t < 0.85 else lerp(HY["pale"], HY["belly"], (t - 0.85) / 0.15)
    if face == "bottom":
        coat = HY["belly"]
    spotted = spot(P, 2.5, 0.95, 31, 0.72)
    if mat in ("chest", "back", "rump", "hump", "neck"):
        if mat == "neck" and face == "front":
            return lerp(HY["pale"], HY["coat"], 0.3)  # pale throat
        if face == "top" or (side_of(face) and t < 0.18):
            c = lerp(coat, HY["dark"], 0.35 + 0.25 * h(int(P[0] * 2), int(P[2]), 3))  # darker along the spine
            return c
        if spotted and t < 0.9:
            return lerp(HY["spot"], coat, 0.15 * h(int(P[0]), int(P[1] * 3), 4))
        return coat
    if mat in ("crest", "neck_crest"):
        if face == "top" and h(int(P[2] * 2), int(P[0] * 2), 6) < 0.3:
            return CLEAR  # ragged bristles
        return lerp(HY["dark"], HY["back"], 0.3 * h(int(P[2] * 3), int(P[1] * 2), 7))
    if mat in ("skull", "cheek"):
        if mat == "skull" and side_of(face) and face != "back":
            # eye: near the front of the side faces, one row under the brow
            if face in ("left", "right") and z < b["z"] + 1 and -2 <= y < -1:
                return HY["eye"]
            if face == "front" and -2 <= y < -1 and (x < b["x"] + 1.2 or x > b["x"] + b["w"] - 1.2):
                return HY["eye"]
            if face in ("left", "right") and z < b["z"] + 2 and -2.5 <= y < 0:
                return lerp(HY["dark"], HY["coat"], 0.3)  # dark eye patch
        if face == "top":
            return lerp(HY["coat"], HY["back"], 0.5)
        if mat == "cheek" and y > 1:
            return lerp(HY["coat"], HY["muzzle"], 0.4)
        return lerp(HY["coat"], HY["back"], 0.25) if not spot(P, 1.8, 0.5, 33, 0.5) else lerp(HY["spot"], HY["coat"], 0.3)
    if mat in ("muzzle", "jaw"):
        snout = lerp(HY["coat"], HY["muzzle"], 0.35 + 0.5 * rel(z, -4, -7))  # darkening towards the nose
        if mat == "jaw":
            return lerp(HY["muzzle"], HY["pale"], 0.3)
        if face in ("left", "right") and y > 0.5 and z < -6:
            return lerp(HY["nose"], HY["muzzle"], 0.3)  # the corner of the mouth
        if face == "front":
            return lerp(HY["nose"], HY["muzzle"], 0.35) if y > 0.5 else lerp(HY["muzzle"], HY["dark"], 0.3)
        if face in ("left", "right") and z < -6:
            return lerp(snout, HY["dark"], 0.35)
        return lerp(snout, HY["back"], 0.2) if face == "top" else snout
    if mat == "nose":
        return lerp(HY["nose"], HY["pad"], 0.3 * h(int(x * 2), int(y * 2), 9))
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        if face == "front":
            edge = x < b["x"] + 0.6 or x > b["x"] + b["w"] - 0.6
            return HY["dark"] if edge else lerp(HY["ear_in"], HY["coat"], 0.3 * rel(y, -1, 0))
        return lerp(HY["dark"], HY["back"], 0.4)
    if mat == "tail":
        return lerp(coat, HY["dark"], 0.2) if not spotted else HY["spot"]
    if mat == "tuft":
        return lerp(HY["nose"], HY["dark"], 0.6 * h(int(P[0] * 3), int(P[1] * 2), 11))
    if mat in ("upper", "thigh", "lower"):
        c = lerp(HY["coat"], HY["pale"], 0.5 if face in ("back",) else 0.0)
        if spot(P, 2.0, 0.8, 35, 0.7):
            return HY["spot"]
        return lerp(c, HY["dark"], 0.25 * rel(P[1], 18, 23))
    if mat == "paw":
        if face == "front" or (face == "top" and z < b["z"] + 1):
            return HY["pad"] if int(x + 10) % 1 == 0 and int((x - b["x"]) * 1) % 2 == 1 else lerp(HY["dark"], HY["coat"], 0.3)
        return lerp(HY["dark"], HY["coat"], 0.25)
    return coat


# ============================================================================ cheetah
def cheetah_model():
    m = Model("Cheetah")
    m.part("body", "root", (0, 12, 0))
    m.box("body", "chest", -3, -2, -9, 6, 6, 8)            # deep, narrow chest
    m.box("body", "withers", -2.5, -2.8, -8, 5, 1, 5)
    m.box("body", "waist", -2.5, -1.8, -1, 5, 4, 6)        # the wasp waist, belly tucked up
    m.box("body", "hips", -2.5, -2, 5, 5, 5, 5)
    m.part("neck", "body", (0, -1, -8), (-0.3, 0, 0))
    m.box("neck", "neck", -2, -2, -4, 4, 4, 4)
    m.part("head", "body", (0, -1.2, -11.3))
    m.box("head", "cranium", -2.5, -3.5, -3.5, 5, 4, 4)    # small and round
    m.box("head", "cheeks", -3, -1, -3.3, 6, 2, 3)
    m.box("head", "muzzle", -1.5, -1.5, -5.5, 3, 2, 2)
    m.part("jaw", "head", (0, 0.5, -3.5))
    m.box("jaw", "jaw", -1.5, 0, -2, 3, 1, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "head", (side * 2, -3.3, -1.5), (0, 0, side * 0.35))
        m.box(f"{s}_ear", "ear", -1, -2, -0.5, 2, 2, 1)
    m.part("tail", "body", (0, -1.5, 9.5), (0.5, 0, 0))
    m.box("tail", "tail", -1, 0, -1, 2, 8, 2)
    m.part("tail_tip", "tail", (0, 8, 0), (0.6, 0, 0))
    m.box("tail_tip", "tail_tip", -1, 0, -1, 2, 6, 2)
    predator_legs(m, (1.8, 12, -6.5), (1.8, 12, 7.5), (-1, -2, -1.5, 2, 7, 3), (-1.5, -2, -2.5, 3, 7, 4))
    return m


CH = dict(back=(190, 140, 66), gold=(214, 168, 88), pale=(232, 206, 154), white=(240, 234, 220), spot=(26, 22, 18),
          eye=(236, 178, 40), nose=(40, 30, 28), ear_back=(36, 30, 26))


def ch_coat(t):
    if t < 0.5:
        return lerp(CH["back"], CH["gold"], t / 0.5)
    if t < 0.78:
        return lerp(CH["gold"], CH["pale"], (t - 0.5) / 0.28)
    return lerp(CH["pale"], CH["white"], smooth((t - 0.78) / 0.15))


def cheetah_colour(v, mat, face, p, P, b):
    x, y, z = p
    t = 0.05 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 10, 17.5)
    if mat in ("chest", "withers", "waist", "hips", "neck"):
        if mat == "neck":
            t = 0.75 if face == "front" else 0.05 if face == "top" else rel(y, -2, 2) * 0.7
        if mat == "chest" and face == "front":
            t = max(t, 0.55)
        c = ch_coat(t)
        if t < 0.88 and spot(P, 1.55, 0.52, 41, 0.8):
            return CH["spot"]
        return c
    if mat in ("cranium", "cheeks", "muzzle", "jaw"):
        tear = CH["spot"]
        if mat == "cranium":
            if face == "front":
                if -2.5 <= y < -1.5 and (b["x"] + 1 <= x < b["x"] + 2 or b["x"] + 3 <= x < b["x"] + 4):
                    return CH["eye"]
                if -2.5 <= y < -1.5 and (x < b["x"] + 1 or x > b["x"] + 4):
                    return CH["white"]
                if y >= -1.5:
                    return tear if (b["x"] + 1 <= x < b["x"] + 2 or b["x"] + 3 <= x < b["x"] + 4) else CH["pale"]
                return CH["gold"] if not spot(P, 1.2, 0.35, 43, 0.6) else CH["spot"]
            if face in ("left", "right"):
                if z < b["z"] + 1 and -2.5 <= y < -1.5:
                    return CH["eye"]
                if z < b["z"] + 1 and y >= -1.5:
                    return tear
            if face == "top" and spot(P, 1.3, 0.4, 44, 0.65):
                return CH["spot"]
            return CH["gold"] if face != "bottom" else CH["white"]
        if mat == "cheeks":
            if face == "front" and abs(x) < 2:
                return CH["white"] if y < 0 else CH["pale"]
            return lerp(CH["gold"], CH["pale"], 0.35 + 0.4 * rel(y, -1, 1))
        if mat == "muzzle":
            if face == "front":
                if (x < b["x"] + 1 or x > b["x"] + b["w"] - 1) and y > b["y"] + 1:
                    return tear  # the tear marks end at the corners of the mouth
                return CH["nose"] if y < b["y"] + 1 and abs(x) < 0.6 else CH["pale"] if y < b["y"] + 1 else CH["white"]
            if face in ("left", "right"):
                return tear if z > b["z"] + 1 else CH["white"]  # the tear line down the side of the nose
            return lerp(CH["gold"], CH["pale"], 0.5) if face == "top" else CH["white"]
        return CH["white"]
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        if face == "back":
            return CH["ear_back"]
        return lerp(CH["gold"], CH["pale"], 0.4)
    if mat == "tail":
        c = ch_coat(0.25 if face != "bottom" else 0.8)
        return CH["spot"] if spot(P, 1.6, 0.6, 45, 0.8) else c
    if mat == "tail_tip":
        if y >= b["y"] + 4:
            return CH["white"]
        return CH["spot"] if int(y - b["y"]) % 2 == 0 else CH["pale"]
    if mat in ("upper", "thigh", "lower"):
        inner = (face == "left" and P[0] < 0) or (face == "right" and P[0] > 0) or face == "back"
        c = lerp(CH["gold"], CH["pale"], 0.55 if inner else 0.15 * rel(P[1], 12, 23))
        if spot(P, 1.7, 0.55, 46, 0.7 if mat != "lower" else 0.45):
            return CH["spot"]
        return c
    if mat == "paw":
        if face == "front" and int(x - b["x"]) % 2 == 1:
            return lerp(CH["pale"], CH["back"], 0.5)
        return CH["pale"]
    return CH["gold"]


# ============================================================================ gazelle
def gazelle_model():
    m = Model("Gazelle")
    m.part("body", "root", (0, 13, 0))
    m.box("body", "chest", -3, -3, -7, 6, 6, 6)
    m.box("body", "withers", -2.5, -3.6, -6.5, 5, 1, 4)
    m.box("body", "barrel", -2.5, -2.8, -1, 5, 5, 5)       # the belly tucked up behind the chest
    m.box("body", "haunch", -3, -3.3, 4, 6, 6, 4)
    m.box("body", "rump", -2.5, -2.8, 8, 5, 4, 1)          # white rump patch
    m.part("neck", "body", (0, -1.5, -6), (0.35, 0, 0))
    m.box("neck", "neck", -1.5, -4, -2, 3, 4, 4)
    m.box("neck", "throat", -1, -6.5, -1.5, 2, 3, 3)
    m.part("head", "neck", (0, -6, -0.3), (-0.35, 0, 0))
    m.box("head", "cranium", -2, -3, -2, 4, 4, 4)
    m.box("head", "face", -1.5, -2.5, -4.5, 3, 3, 3)
    m.box("head", "nose", -1, -2, -6, 2, 2, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "head", (side * 1.5, -2.5, 1), (-0.2, side * -0.5, side * 0.55))
        m.box(f"{s}_ear", "ear", -1, -4, -0.5, 2, 4, 1)
        # lyre horns: up and back from the brow, out at the middle, the tips hooking forward and in
        m.part(f"{s}_horn", "head", (side * 0.8, -2.8, -0.5), (-0.5, 0, side * 0.12))
        m.box(f"{s}_horn", "horn", -0.5, -3, -0.5, 1, 3, 1)
        m.part(f"{s}_horn_mid", f"{s}_horn", (0, -3, 0), (0.15, 0, side * 0.18), name="mid")
        m.box(f"{s}_horn_mid", "horn", -0.5, -3, -0.5, 1, 3, 1)
        m.part(f"{s}_horn_tip", f"{s}_horn_mid", (0, -3, 0), (0.6, 0, side * -0.35), name="tip")
        m.box(f"{s}_horn_tip", "horn_tip", -0.5, -2, -0.5, 1, 2, 1)
    m.part("tail", "body", (0, -2.5, 8.6), (0.3, 0, 0))
    m.box("tail", "tail", -1, 0, -0.5, 2, 4, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 1.7, 15, -5))
        m.box(f"{s}_front_leg", "forearm", -1, -1, -1.5, 2, 5, 3)
        m.box(f"{s}_front_leg", "cannon", -0.5, 4, -0.5, 1, 4, 1)
        m.box(f"{s}_front_leg", "hoof", -1, 8, -1.5, 2, 1, 2)
        m.part(f"{s}_hind_leg", "root", (side * 1.7, 13.5, 5))
        m.box(f"{s}_hind_leg", "thigh", -1.5, -1.5, -2, 3, 6, 4)
        m.box(f"{s}_hind_leg", "gaskin", -1, 4.5, -0.5, 2, 3, 2)   # the hock set back
        m.box(f"{s}_hind_leg", "cannon", -0.5, 7.5, 0, 1, 2, 1)
        m.box(f"{s}_hind_leg", "hoof", -1, 9.5, -1, 2, 1, 2)
    return m


GZ = dict(cheek=(116, 70, 40), back=(186, 120, 66), rufous=(170, 98, 52), buff=(222, 188, 140), band=(34, 26, 22), white=(242, 238, 228),
          dark=(52, 38, 30), horn=(44, 40, 36), horn_ring=(74, 66, 58), horn_tip=(120, 110, 96), hoof=(26, 22, 20),
          eye=(24, 18, 16), nose=(30, 24, 22))


def gazelle_colour(v, mat, face, p, P, b):
    x, y, z = p
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 10, 16)
    flank = face in ("left", "right")
    if mat in ("chest", "withers", "barrel", "haunch"):
        if mat == "chest" and face == "front":
            return GZ["white"] if P[1] > 13 else lerp(GZ["back"], GZ["buff"], 0.4)
        if mat == "haunch" and face == "back":
            if P[1] < 10.5:
                return GZ["back"]
            return GZ["white"] if abs(x) < 2.4 else lerp(GZ["band"], GZ["back"], 0.4)  # rump patch edged dark
        if flank and -6 < P[2] < 6.5:
            if 0.5 <= t < 0.62:
                return GZ["buff"]   # the pale line over the band
            if 0.62 <= t < 0.82:
                return GZ["band"]   # the black flank band
        if t >= 0.82 or (flank and t >= 0.62):
            return GZ["white"]
        return lerp(GZ["back"], GZ["rufous"], 0.25 * h(int(P[2]), int(P[1] * 2), 51))
    if mat == "rump":
        return GZ["back"] if face == "top" else GZ["white"]
    if mat in ("neck", "throat"):
        if face == "front":
            return lerp(GZ["buff"], GZ["white"], 0.6) if mat == "throat" else GZ["buff"]
        return lerp(GZ["back"], GZ["rufous"], 0.2)
    if mat in ("cranium", "face", "nose"):
        if face == "bottom":
            return GZ["white"]
        if mat == "cranium" and face in ("left", "right"):
            front = z < b["z"] + 1
            if front and -1.5 <= y < -0.5:
                return GZ["eye"]
            if (front and -2.5 <= y < -1.5) or (b["z"] + 1 <= z < b["z"] + 2 and -1.5 <= y < -0.5):
                return lerp(GZ["white"], GZ["buff"], 0.3)  # the pale ring over and behind the eye
            if front and -0.5 <= y < 0.5:
                return GZ["cheek"]  # where the cheek stripe starts
            return GZ["back"] if y < 0.5 else GZ["buff"]
        if mat == "face" and face in ("left", "right"):
            return GZ["buff"] if y < -1.5 else GZ["cheek"] if y < -0.5 else lerp(GZ["white"], GZ["buff"], 0.3)
        if mat == "nose" and face in ("left", "right"):
            return GZ["buff"] if y < -1 else GZ["white"]
        if mat == "nose" and face == "front":
            return GZ["nose"] if y < -1 else GZ["white"]
        if face == "top" or face == "front":
            if mat == "nose":
                return GZ["dark"]  # the dark spot on the nose
            return lerp(GZ["rufous"], GZ["dark"], 0.25)  # rufous blaze down the face
        return GZ["back"]
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        if face == "front":
            edge = x < b["x"] + 0.5 or x > b["x"] + b["w"] - 0.5
            return GZ["dark"] if edge and y < b["y"] + 2 else GZ["white"]
        return GZ["dark"] if y < b["y"] + 1.2 else lerp(GZ["back"], GZ["buff"], 0.3)
    if mat in ("horn", "horn_tip"):
        if mat == "horn_tip" and y < b["y"] + 1:
            return GZ["horn_tip"]
        return GZ["horn_ring"] if int((y - b["y"]) * 2) % 2 == 0 and mat == "horn" else GZ["horn"]
    if mat == "tail":
        return GZ["back"] if y < 1 else GZ["band"]
    if mat in ("forearm", "thigh", "gaskin", "cannon"):
        inner = (face == "left" and P[0] < 0) or (face == "right" and P[0] > 0) or face == "back"
        c = GZ["buff"] if inner else lerp(GZ["back"], GZ["buff"], 0.3 * rel(P[1], 13, 23))
        if mat == "thigh" and face == "back":
            return GZ["white"]
        if mat == "forearm" and face == "front" and y > b["y"] + b["h"] - 1:
            return lerp(GZ["dark"], GZ["back"], 0.3)  # knee tuft
        return c
    if mat == "hoof":
        return GZ["hoof"]
    return GZ["back"]


# ============================================================================ ostrich
def ostrich_model():
    m = Model("Ostrich")
    m.part("body", "root", (0, 7, 1))
    # an egg of layered boxes: deepest in the middle, narrowing to the breast in front and the rump behind
    m.box("body", "core", -4.5, -3.5, -5, 9, 7, 10)
    m.box("body", "breast", -3.5, -2.5, -7, 7, 6, 2)
    m.box("body", "breast2", -2.5, -1.5, -8, 5, 4, 1)
    m.box("body", "back_round", -3.5, -4.5, -4, 7, 1, 9)
    m.box("body", "belly_round", -3.5, 3.5, -4, 7, 1, 8)
    m.box("body", "rump", -3.5, -4, 5, 7, 6, 2)
    m.box("body", "rump2", -2.5, -3.5, 7, 5, 4, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "body", (side * 4.5, -3, 0.5))
        m.box(f"{s}_wing", "wing", -1 if side < 0 else 0, 0, -3.5, 1, 5, 8)
        m.part(f"{s}_plume", f"{s}_wing", (0, 3.5, 2.5), (0.35, 0, 0), name="plume")
        m.box(f"{s}_plume", "plume", -1.3 if side < 0 else 0.3, 0, -2, 1, 4, 7)
    m.part("tail", "body", (0, -3, 7.5), (0.35, 0, 0))
    m.box("tail", "tail", -2.5, -2, 0, 5, 4, 3)
    m.box("tail", "tail_plume", -2, -0.5, 2.5, 4, 4, 2)
    m.part("neck", "body", (0, -2, -6.5), (0.15, 0, 0))
    m.box("neck", "collar", -2.5, -2, -2.5, 5, 3, 5)       # the fluffy ruff where the feathers stop
    m.box("neck", "neck_low", -1.5, -7, -1.5, 3, 7, 3)
    m.part("upper_neck", "neck", (0, -6.8, 0), (-0.1, 0, 0))
    m.box("upper_neck", "neck_up", -1, -7, -1, 2, 7, 2)
    m.part("head", "upper_neck", (0, -7, 0), (-0.05, 0, 0))
    m.box("head", "skull", -1.5, -3, -2, 3, 3, 4)
    m.box("head", "beak", -1.5, -1.8, -5, 3, 1, 3)
    m.box("head", "mandible", -1, -0.8, -4.5, 2, 1, 3)
    ostrich_legs(m, 10, 7)
    return m


def ostrich_legs(m, top, knee):
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_leg", "root", (side * 2.2, top, 1.5))
        m.box(f"{s}_leg", "thigh", -1.5, -1, -1.5, 3, 5, 3)
        m.box(f"{s}_leg", "drum", -1, 3.5, -1, 2, int(knee - 3), 2)
        m.part(f"{s[0]}_lower", f"{s}_leg", (0, knee, 0.5), name="lower")
        m.box(f"{s[0]}_lower", "shank", -1, 0, -1, 2, 23 - top - knee, 2)
        m.box(f"{s[0]}_lower", "toe", -1, 23 - top - knee, -3.5, 2, 1, 4)   # the big inner toe
        m.box(f"{s[0]}_lower", "small_toe", -2 if side < 0 else 1, 23 - top - knee, -2, 1, 1, 2)


def ostrich_chick_model():
    """A downy ball on long legs: a round fluffy body, a stubby wing each side, a short thick neck and a big head with big
    eyes and a short beak. Built at adult size (the renderer scales it down); the part names are the adult's."""
    m = Model("Ostrich chick")
    m.part("body", "root", (0, 7.5, 1))
    m.box("body", "core", -4, -3.5, -4.5, 8, 7, 9)
    m.box("body", "back_round", -3, -4.5, -3.5, 6, 1, 7)
    m.box("body", "belly_round", -3, 3.5, -3.5, 6, 1, 7)
    m.box("body", "breast", -3, -2.5, -5.5, 6, 5, 1)
    m.box("body", "rump", -3, -3, 4.5, 6, 5, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_wing", "body", (side * 4, -2.5, 0))
        m.box(f"{s}_wing", "plume", -1 if side < 0 else 0, 0, -2.5, 1, 4, 5)
    m.part("tail", "body", (0, -2.5, 5.5), (0.5, 0, 0))
    m.box("tail", "tail_plume", -2, -1.5, 0, 4, 3, 2)
    m.part("neck", "body", (0, -2.5, -4.5), (0.15, 0, 0))
    m.box("neck", "neck_low", -1.5, -3, -1.5, 3, 3, 3)
    m.part("upper_neck", "neck", (0, -2.8, 0), (-0.1, 0, 0))
    m.box("upper_neck", "neck_up", -1, -3, -1, 2, 3, 2)
    m.part("head", "upper_neck", (0, -3, 0), (-0.05, 0, 0))
    m.box("head", "skull", -2, -4, -2.5, 4, 4, 5)
    m.box("head", "beak", -1, -2.2, -5.5, 2, 1, 3)
    m.box("head", "mandible", -1, -1.2, -5, 2, 1, 3)
    ostrich_legs(m, 10.5, 6.5)
    return m


OS = {
    "male": dict(feather=(54, 48, 48), edge=(116, 106, 100), plume=(242, 240, 232), plume_dark=(200, 196, 188),
                 skin=(206, 136, 128), skin_dark=(160, 96, 92), scale=(214, 90, 84), beak=(222, 176, 146),
                 foot=(180, 128, 118), bristle=(120, 96, 92)),
    "female": dict(feather=(116, 98, 80), edge=(150, 132, 112), plume=(196, 184, 164), plume_dark=(150, 138, 120),
                   skin=(176, 160, 146), skin_dark=(132, 118, 106), scale=(158, 142, 128), beak=(196, 170, 140),
                   foot=(150, 136, 122), bristle=(116, 102, 90)),
    "chick": dict(feather=(196, 164, 116), edge=(146, 112, 74), plume=(214, 190, 146), plume_dark=(120, 92, 62),
                  skin=(206, 176, 132), skin_dark=(120, 90, 60), scale=(184, 160, 128), beak=(208, 180, 140),
                  foot=(176, 152, 120), bristle=(96, 70, 44)),
}
EYE_OS, LASH, CLAW = (34, 26, 22), (20, 16, 14), (60, 54, 48)


def ostrich_colour(v, mat, face, p, P, b):
    c = OS[v]
    x, y, z = p
    if mat in ("core", "breast", "breast2", "back_round", "belly_round", "rump", "rump2", "collar"):
        if v == "chick":
            streak = int(P[0] * 1.5 + 20) % 3 == 0 and h(int(P[0] * 2), int(P[2] / 2), 61) < 0.7
            return lerp(c["feather"], c["edge"], 0.85 if streak else 0.3 * h(int(P[0] * 2), int(P[1] * 2 + P[2]), 62))
        if mat == "collar":
            if side_of(face) and y < b["y"] + 1 and h(int(x * 2 + z * 3 + 9), 3, 74) < 0.5:
                return CLEAR  # ragged feather tips round the bare neck
            return lerp(c["plume"], c["feather"], 0.35 + 0.4 * h(int(x * 2 + z * 3 + 9), int(y * 2), 75))
        # loose body feathers: soft, irregular paler fringes, rows offset
        row = int(P[1] + 20)
        col = int(P[2] + (row % 2) * 1.5 + 20) if face in ("left", "right") else int(P[0] + (row % 2) * 1.5 + 20)
        edge = h(col // 2, row, 63) < 0.35
        return lerp(c["feather"], c["edge"], (0.45 if edge else 0.0) + 0.15 * h(col, row, 64))
    if mat in ("wing", "plume", "tail", "tail_plume"):
        if mat == "plume" and (face in ("left", "right") and y > b["y"] + b["h"] - 1 and h(int(z * 2), 1, 64) < 0.4):
            return CLEAR  # ragged plume ends
        if mat == "tail_plume" and face in ("back", "left", "right") and y > b["y"] + b["h"] - 1 and h(int(x * 2 + z), 2, 65) < 0.45:
            return CLEAR
        if v == "chick":
            return lerp(c["feather"], c["plume_dark"], 0.5 * h(int(P[2] * 2), int(P[1]), 66))
        if mat == "wing" and y < b["y"] + 2.5:
            return lerp(c["feather"], c["edge"], 0.4 * h(int(P[2] * 2), int(P[1]), 67))  # dark coverts on top
        strand = int((P[2] if face in ("left", "right") else P[0]) * 1 + 30) % 2
        return lerp(c["plume"], c["plume_dark"], 0.45 if strand else 0.1 * h(int(P[2]), int(P[1] * 2), 68))
    if mat in ("neck_low", "neck_up", "skull"):
        base = lerp(c["skin"], c["skin_dark"], 0.25 * h(int(P[1] * 2), int(P[0] * 3 + P[2]), 69))
        if v == "chick" and mat != "skull":
            if face in ("back", "left", "right") and int(P[0] * 2 + 40) % 3 == 0 and h(int(P[1]), int(P[0]), 73) < 0.75:
                return c["bristle"]  # dark lengthwise streaks down the chick's neck
        if mat == "neck_low" and y < b["y"] + 2.5 and h(int(P[0] * 3 + P[2] * 2), int(P[1] * 2), 70) < 0.35:
            return c["bristle"]  # sparse down near the collar
        if mat == "skull":
            if v == "chick" and face == "top" and h(int(x * 2 + 5), int(z * 2 + 5), 76) < 0.45:
                return c["bristle"]  # the chick's dark-spotted crown
            if face in ("left", "right") and -2.2 <= y < -1.2 and b["z"] + 0.5 <= z < b["z"] + 2.5:
                return EYE_OS if z < b["z"] + 2 else lerp(EYE_OS, c["skin"], 0.5)  # the big dark eye
            if face in ("left", "right") and y < -2.2 and b["z"] + 0.5 <= z < b["z"] + 2.5:
                return LASH  # lashes over the eye
            if face == "top":
                return lerp(base, c["bristle"], 0.5 if h(int(x * 2), int(z * 2), 71) < 0.4 else 0.1)
        return base
    if mat in ("beak", "mandible"):
        if mat == "beak" and (face == "front" or (v == "chick" and z < b["z"] + 1)):
            return lerp(c["beak"], CLAW, 0.5)
        if mat == "beak" and face == "top" and z > -3 and abs(x) < 1:
            return lerp(c["beak"], CLAW, 0.4) if int(z * 2) % 3 == 0 else c["beak"]  # nostril
        if mat == "mandible" and face in ("left", "right") and y < b["y"] + 0.5:
            return LASH  # the gape line
        return c["beak"] if mat == "beak" else lerp(c["beak"], c["skin_dark"], 0.25)
    if mat in ("thigh", "drum"):
        if mat == "thigh" and y < b["y"] + 1.5:
            return lerp(c["feather"], c["edge"], 0.3)  # feathers reach down over the thigh top
        return lerp(c["skin"], c["skin_dark"], 0.2 * rel(y, b["y"], b["y"] + b["h"]))
    if mat == "shank":
        if face == "front":
            return c["scale"] if int(y * 1) % 2 == 0 else lerp(c["scale"], c["skin_dark"], 0.45)  # the scutes
        return lerp(c["foot"], c["skin_dark"], 0.3 * h(int(y * 2), int(z * 2), 72))
    if mat in ("toe", "small_toe"):
        if z < b["z"] + 1 and mat == "toe":
            return CLAW
        return lerp(c["foot"], c["skin_dark"], 0.35 if face == "top" and int(z * 2) % 2 == 0 else 0.1)
    return c["feather"]


# ============================================================================ aardvark
def aardvark_model():
    m = Model("Aardvark")
    m.part("body", "root", (0, 15, 0))
    m.box("body", "fore", -3, -3, -6, 6, 6, 5)
    m.box("body", "mid", -3.5, -4, -1, 7, 7, 5)
    m.box("body", "arch", -3, -5, 0, 6, 1, 4)              # the arched back peaks over the loins
    m.box("body", "hips", -3, -4, 4, 6, 6, 3)
    m.box("body", "rump", -2.5, -3, 7, 5, 5, 2)
    m.box("body", "neck", -2, -2, -8, 4, 4, 2)
    m.part("head", "body", (0, 0, -7.5), (0.25, 0, 0))
    m.box("head", "skull", -2, -2, -4, 4, 4, 4)
    m.box("head", "snout1", -1.5, -1.2, -7, 3, 3, 3)
    m.box("head", "snout2", -1, -0.6, -10, 2, 2, 3)
    m.box("head", "disc", -1.5, -0.8, -11, 3, 2, 1)        # the pig-like nose disc
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_ear", "head", (side * 1.3, -1.6, -1), (-0.25, 0, side * 0.2))
        m.box(f"{s}_ear", "ear", -1, -6, -0.5, 2, 6, 1)
    m.part("tail", "body", (0, -1, 8.5), (-0.55, 0, 0))
    m.box("tail", "tail_base", -2, -1.5, 0, 4, 4, 4)
    m.part("tail_mid", "tail", (0, 0.5, 4), (-0.2, 0, 0))
    m.box("tail_mid", "tail_mid", -1.5, -1, 0, 3, 3, 4)
    m.part("tail_tip", "tail_mid", (0, 0, 4), (-0.2, 0, 0))
    m.box("tail_tip", "tail_tip", -1, -0.8, 0, 2, 2, 4)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 2.3, 17, -4))
        m.box(f"{s}_front_leg", "forearm", -1.5, -1, -1.5, 3, 5, 3)
        m.box(f"{s}_front_leg", "wrist", -1, 4, -1, 2, 2, 2)
        m.box(f"{s}_front_leg", "foot", -1.5, 6, -2, 3, 1, 3)
        m.box(f"{s}_front_leg", "claws", -1.5, 6, -3, 3, 1, 1)
        m.part(f"{s}_hind_leg", "root", (side * 2.3, 16, 5))
        m.box(f"{s}_hind_leg", "thigh", -1.5, -1, -2, 3, 5, 4)
        m.box(f"{s}_hind_leg", "shank", -1, 4, -1, 2, 3, 2)
        m.box(f"{s}_hind_leg", "foot", -1.5, 7, -2.5, 3, 1, 4)
        m.box(f"{s}_hind_leg", "claws", -1.5, 7, -3.5, 3, 1, 1)
    return m


AV = dict(hide=(172, 152, 124), hide_dark=(146, 124, 100), bristle=(118, 98, 78), belly=(186, 168, 142),
          head=(184, 164, 144), snout=(196, 162, 148), disc=(178, 132, 124), nostril=(70, 44, 40),
          ear=(150, 126, 104), ear_in=(204, 156, 146), leg=(98, 80, 64), leg_dark=(70, 56, 46), claw=(150, 138, 118),
          eye=(18, 14, 12))


def aardvark_colour(v, mat, face, p, P, b):
    x, y, z = p
    bristle = h(int(P[0] * 2 + 50), int(P[1] * 2 + P[2] * 3 + 50), 81) < 0.1
    if mat in ("fore", "mid", "arch", "hips", "rump", "neck"):
        t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], 10, 18)
        c = lerp(lerp(AV["hide_dark"], AV["hide"], t / 0.5), AV["belly"], (t - 0.6) / 0.4)
        if mat == "fore" and face in ("left", "right") and t > 0.5:
            c = lerp(c, AV["leg"], 0.45 * rel(P[1], 15, 18))  # the dark legs' hair creeps up onto the shoulders
        return lerp(c, AV["bristle"], 0.6) if bristle and t < 0.85 else c
    if mat == "skull":
        if face in ("left", "right") and -1 <= y < 0 and z < b["z"] + 1.5:
            return AV["eye"]
        if face in ("left", "right") and -1.5 <= y < 0.5 and z < b["z"] + 2:
            return lerp(AV["head"], AV["hide_dark"], 0.35)  # wrinkled lid
        return AV["head"] if not bristle else AV["hide"]
    if mat in ("snout1", "snout2"):
        if face in ("left", "right", "front", "top") and int((z if face != "front" else y) * 1 + 40) % 2 == 0:
            return lerp(AV["snout"], AV["hide_dark"], 0.18)  # rings of wrinkles down the tube
        if mat == "snout2" and face in ("left", "right") and y > b["y"] + 1:
            return lerp(AV["snout"], AV["nostril"], 0.3)  # mouth line
        return AV["snout"] if mat == "snout2" else lerp(AV["head"], AV["snout"], 0.5)
    if mat == "disc":
        if face == "front":
            return AV["nostril"] if (abs(x) > 0.5 and y < 0.5) else AV["disc"]
        return AV["disc"]
    if mat == "ear":
        if rounded(face, p, b):
            return CLEAR
        if face == "front":
            edge = x < b["x"] + 0.5 or x > b["x"] + b["w"] - 0.5
            return AV["ear"] if edge else lerp(AV["ear_in"], AV["ear"], 0.3 * rel(y, -2, 0))
        return lerp(AV["ear"], AV["hide_dark"], 0.3 * h(int(y * 2), int(x * 2), 83))
    if mat in ("tail_base", "tail_mid", "tail_tip"):
        t = {"tail_base": 0.0, "tail_mid": 0.35, "tail_tip": 0.7}[mat] + 0.3 * rel(z, b["z"], b["z"] + b["d"])
        c = lerp(AV["hide"], AV["belly"], t)
        return AV["bristle"] if bristle and t < 0.6 else c
    if mat in ("forearm", "thigh"):
        top = rel(y, b["y"], b["y"] + 2.5)
        c = lerp(AV["hide_dark"], AV["leg"], top)
        return lerp(c, AV["leg_dark"], 0.5) if bristle else c
    if mat in ("wrist", "shank", "foot"):
        return lerp(AV["leg"], AV["leg_dark"], 0.5 + 0.3 * h(int(x * 2), int(y * 2 + z), 85))
    if mat == "claws":
        return AV["claw"] if face == "front" else lerp(AV["claw"], AV["leg_dark"], 0.45)
    return AV["hide"]


# ============================================================================
def main():
    models = [(lion_model(), "caveLion", lion_colour, {"cave_lion": None}, ("nose", "paw")),
              (hyena_model(), "hyena", hyena_colour, {"spotted_hyena": None}, ("nose", "paw")),
              (cheetah_model(), "cheetah", cheetah_colour, {"cheetah": None}, ("paw",)),
              (gazelle_model(), "gazelle", gazelle_colour, {"gazelle": None}, ("horn", "horn_tip", "hoof")),
              (ostrich_model(), "ostrich", ostrich_colour, {"ostrich_male": "male", "ostrich_female": "female"}, ("beak", "mandible", "toe", "small_toe")),
              (ostrich_chick_model(), "ostrichChick", ostrich_colour, {"ostrich_chick": "chick"}, ("beak", "mandible", "toe", "small_toe")),
              (aardvark_model(), "aardvark", aardvark_colour, {"aardvark": None}, ("claws",))]
    methods = []
    for m, method, colour, skins, keep in models:
        m.pack()
        methods.append(m.java(method))
        for name, variant in skins.items():
            img = m.paint(colour, variant)
            save(img, f"wildlife/{name}.png")
            save(m.skinned(img, keep), f"skinned/wildlife/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.savanna;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_savanna_models.py (with the skins it paints): the savanna animals' parts and boxes. Do not edit. */
public final class SavannaGeometry {{
{chr(10).join(methods)}
    private SavannaGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
