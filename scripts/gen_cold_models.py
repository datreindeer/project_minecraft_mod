"""The cold fauna's models and skins from one table each (docs/fauna/cold.md, com.mechanical.wildlife.cold), the way
gen_elkwight_model.py builds the Elkwight:

- src/main/java/com/mechanical/client/wildlife/cold/ColdGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the skinned carcass skins (textures/entity/skinned/wildlife/)

Every texel is painted from the point it covers on the posed model (parts' offsets and rest rotations applied), so the
markings run on across boxes: a tiger's stripes round its body and legs, a snow leopard's rosettes, an owl's bars.
The animation stays in the shared models, so the part trees keep their names and pivots: BrownBearModel and
BearCubModel (polar bear and cub), PredatorModel (arctic fox), LeopardModel (snow leopard, Siberian tiger),
ForestBirdModel (snowy owl, black grouse, lammergeier); the pika has its own PikaModel (built at twice its size and
drawn at half, for detail).

Usage: python scripts/gen_cold_models.py (also run at the end of gen_fauna_cold_textures.py). Deterministic.
"""
import math
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import _skinner  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/cold/ColdGeometry.java"


# ---------------------------------------------------------------- helpers
def h(x, y, salt=0):
    v = (int(math.floor(x)) * 73856093) ^ (int(math.floor(y)) * 19349663) ^ (salt * 83492791)
    v = ((v ^ (v >> 13)) * 1274126177) & 0xFFFFFFFF
    return (v & 0xFFFF) / 0xFFFF


def h3(x, y, z, salt=0):
    return h(x * 31 + z * 17, y * 13 - z * 7, salt)


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a[:3], b[:3]))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def cells(p, size, salt):
    """Distance to the nearest of a jittered 3D grid of points (spacing size) and that point's own random value."""
    x, y, z = (v / size for v in p)
    cx, cy, cz = math.floor(x), math.floor(y), math.floor(z)
    best, val = 9.0, 0.0
    for dx in (-1, 0, 1):
        for dy in (-1, 0, 1):
            for dz in (-1, 0, 1):
                gx, gy, gz = cx + dx, cy + dy, cz + dz
                px = gx + 0.15 + 0.7 * h3(gx, gy, gz, salt)
                py = gy + 0.15 + 0.7 * h3(gx, gy, gz, salt + 1)
                pz = gz + 0.15 + 0.7 * h3(gx, gy, gz, salt + 2)
                d = math.sqrt((x - px) ** 2 + (y - py) ** 2 + (z - pz) ** 2)
                if d < best:
                    best, val = d, h3(gx, gy, gz, salt + 3)
    return best * size, val


def rot(rx, ry, rz):
    """Rz * Ry * Rx (ModelPart.translateAndRotate's rotationZYX)."""
    cx, sx, cy, sy, cz, sz = math.cos(rx), math.sin(rx), math.cos(ry), math.sin(ry), math.cos(rz), math.sin(rz)
    X = ((1, 0, 0), (0, cx, -sx), (0, sx, cx))
    Y = ((cy, 0, sy), (0, 1, 0), (-sy, 0, cy))
    Z = ((cz, -sz, 0), (sz, cz, 0), (0, 0, 1))
    return mul(Z, mul(Y, X))


def mul(a, b):
    return tuple(tuple(sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)) for i in range(3))


def apply(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


NORMALS = {"top": (0, -1, 0), "bottom": (0, 1, 0), "right": (-1, 0, 0), "left": (1, 0, 0), "front": (0, 0, -1), "back": (0, 0, 1)}


class Model:
    def __init__(self, name, method, tw, th):
        self.name, self.method, self.tw, self.th = name, method, tw, th
        self.parts = {}
        self.boxes = []

    def part(self, name, parent="root", at=(0, 0, 0), rot=(0, 0, 0)):
        self.parts[name] = [parent, list(at), rot]
        return name

    def box(self, part, mat, x, y, z, w, hh, d):
        assert all(float(v).is_integer() for v in (w, hh, d)), (self.name, part, mat)
        self.boxes.append(dict(part=part, mat=mat, x=x, y=y, z=z, w=int(w), h=int(hh), d=int(d)))

    def pivot(self, part):
        """The part's rest pivot in model space, rotations ignored (for placing boxes by absolute coordinates)."""
        x = y = z = 0.0
        while part != "root":
            parent, at, _ = self.parts[part]
            x, y, z, part = x + at[0], y + at[1], z + at[2], parent
        return x, y, z

    def abox(self, part, mat, x, y, z, w, hh, d):
        """A box given in model space (for parts with no rotation up their chain)."""
        px, py, pz = self.pivot(part)
        self.box(part, mat, x - px, y - py, z - pz, w, hh, d)

    def matrix(self, part):
        """(rotation, translation) of the part's space into model space, rest pose."""
        if part == "root":
            return ((1, 0, 0), (0, 1, 0), (0, 0, 1)), (0, 0, 0)
        parent, at, r = self.parts[part]
        pm, pt = self.matrix(parent)
        t = apply(pm, at)
        return mul(pm, rot(*r)), (pt[0] + t[0], pt[1] + t[1], pt[2] + t[2])

    def lowest(self, parts):
        low = -99
        for b in self.boxes:
            if b["part"] in parts:
                m, t = self.matrix(b["part"])
                for cx in (0, b["w"]):
                    for cy in (0, b["h"]):
                        for cz in (0, b["d"]):
                            p = apply(m, (b["x"] + cx, b["y"] + cy, b["z"] + cz))
                            low = max(low, p[1] + t[1])
        return low

    def subtree(self, part):
        out = {part}
        changed = True
        while changed:
            changed = False
            for n, (parent, _, _) in self.parts.items():
                if parent in out and n not in out:
                    out.add(n)
                    changed = True
        return out

    # ---- texture layout: shelf-pack the boxes' UV rectangles (2d+2w) x (d+h)
    def pack(self):
        order = sorted(range(len(self.boxes)), key=lambda i: (-(self.boxes[i]["d"] + self.boxes[i]["h"]), i))
        x = y = shelf = 0
        for i in order:
            b = self.boxes[i]
            bw, bh = 2 * b["d"] + 2 * b["w"], b["d"] + b["h"]
            if x + bw > self.tw:
                x, y, shelf = 0, y + shelf, 0
            b["uv"] = (x, y)
            x += bw
            shelf = max(shelf, bh)
        assert y + shelf <= self.th, f"{self.name}: texture full ({y + shelf} > {self.th})"

    def texels(self, b):
        """(face, tx, ty, local point, i, j, face width, face height) for every texel of a box (vanilla Cube UV layout)."""
        u, v = b["uv"]
        x0, y0, z0, w, hh, d = b["x"], b["y"], b["z"], b["w"], b["h"], b["d"]
        x1, y1, z1 = x0 + w, y0 + hh, z0 + d
        for i in range(w):
            for j in range(d):
                yield "top", u + d + i, v + j, (x0 + i + .5, y0, z1 - j - .5), i, j, w, d
                yield "bottom", u + d + w + i, v + j, (x0 + i + .5, y1, z1 - j - .5), i, j, w, d
        for j in range(hh):
            y = y0 + j + .5
            for i in range(d):
                yield "right", u + i, v + d + j, (x0, y, z1 - i - .5), i, j, d, hh
                yield "left", u + d + w + i, v + d + j, (x1, y, z0 + i + .5), i, j, d, hh
            for i in range(w):
                yield "front", u + d + i, v + d + j, (x0 + i + .5, y, z0), i, j, w, hh
                yield "back", u + 2 * d + w + i, v + d + j, (x1 - i - .5, y, z1), i, j, w, hh

    def paint(self, colour, light=True):
        """colour(T) -> rgb / rgba / None for every texel T (a Texel); then light from above on top of it."""
        im = Image.new("RGBA", (self.tw, self.th), (0, 0, 0, 0))
        px = im.load()
        for k, b in enumerate(self.boxes):
            m, t = self.matrix(b["part"])
            for face, tx, ty, lp, i, j, fw, fh in self.texels(b):
                wp = apply(m, lp)
                p = (wp[0] + t[0], wp[1] + t[1], wp[2] + t[2])
                n = apply(m, NORMALS[face])
                T = Texel(b, face, p, n, i, j, fw, fh, k, lp)
                c = colour(T)
                if c is None:
                    continue
                a = c[3] if len(c) == 4 else 255
                if light:
                    f = 1.0 - 0.09 * n[1] + 0.03 * (n[0] - n[2]) * 0.5  # brighter facing up, darker facing down
                    f *= 0.97 + 0.06 * h(tx, ty, 5)
                    c = shade(c, f)
                px[tx, ty] = (*c[:3], a)
        return im

    def java(self):
        order = ["root"]
        while len(order) < len(self.parts) + 1:
            for n, (parent, _, _) in self.parts.items():
                if n not in order and parent in order:
                    order.append(n)
        lines = []
        for n in order[1:]:
            parent, at, r = self.parts[n]
            cl = "CubeListBuilder.create()"
            for b in self.boxes:
                if b["part"] == n:
                    cl += f"\n                .texOffs({b['uv'][0]}, {b['uv'][1]}).addBox({f(b['x'])}, {f(b['y'])}, {f(b['z'])}, {f(b['w'])}, {f(b['h'])}, {f(b['d'])})"
            pose = f"PartPose.offsetAndRotation({f(at[0])}, {f(at[1])}, {f(at[2])}, {f(r[0])}, {f(r[1])}, {f(r[2])})"
            var = jname(n)
            lines.append(f"        PartDefinition {var} = {jname(parent)}.addOrReplaceChild(\"{n}\", {cl},\n                {pose});")
        return (f"    /** {self.name}: {len(self.parts)} parts, {len(self.boxes)} boxes, {self.tw}x{self.th}. */\n"
                f"    public static LayerDefinition {self.method}() {{\n"
                f"        MeshDefinition mesh = new MeshDefinition();\n        PartDefinition root = mesh.getRoot();\n"
                + "\n".join(lines) + f"\n        return LayerDefinition.create(mesh, {self.tw}, {self.th});\n    }}\n")


class Texel:
    __slots__ = ("b", "mat", "face", "p", "n", "i", "j", "fw", "fh", "k", "lp")

    def __init__(self, b, face, p, n, i, j, fw, fh, k, lp):
        self.b, self.mat, self.face, self.p, self.n, self.i, self.j, self.fw, self.fh, self.k, self.lp = \
            b, b["mat"], face, p, n, i, j, fw, fh, k, lp

    @property
    def side(self):
        return self.face in ("left", "right", "front", "back")

    @property
    def edge_bottom(self):
        return self.side and self.j == self.fh - 1

    def rel(self):
        """0..1 position inside the box along x, y, z."""
        b, (x, y, z) = self.b, self.lp
        return (x - b["x"]) / max(1, b["w"]), (y - b["y"]) / max(1, b["h"]), (z - b["z"]) / max(1, b["d"])

    def noise(self, salt=0):
        return h3(self.p[0] * 2, self.p[1] * 2, self.p[2] * 2, salt + self.k)


def jname(n):
    if n == "root":
        return "root"
    parts = n.split("_")
    return parts[0] + "".join(p.capitalize() for p in parts[1:])


def f(v):
    s = f"{v:.4f}".rstrip("0").rstrip(".")
    return (s if "." in s else s + ".0") + "F"


def strands(c, dark, light, T, amount=1.0, period=4):
    """Fur: hair strands running down the sides (along +y on side faces), darker roots, lighter tips; speckled on tops."""
    col = h(T.i + T.k * 7, 0, 3)
    if T.side:
        t = (T.j + int(col * period)) % period
        c = lerp(c, dark, (0.22 * col + (0.12 if t == 0 else 0)) * amount)
        if t == period - 1:
            c = lerp(c, light, 0.25 * amount)
    else:
        c = lerp(c, dark, 0.22 * h(T.i, T.j, T.k) * amount)
        if h(T.i, T.j, T.k + 9) > 0.85:
            c = lerp(c, light, 0.3 * amount)
    return c


def belly_shade(c, T, top, bottom, under, amount=0.35):
    """Light on the back, shadow towards the belly (world height between top and bottom)."""
    t = smooth((T.p[1] - top) / (bottom - top))
    return lerp(c, under, amount * t)


def ragged(T, chance=0.45, salt=0):
    """Transparent gaps along a fringe's lower edge."""
    return T.edge_bottom and h(T.i, T.k, salt + 31) < chance


# ---------------------------------------------------------------- polar bear (BrownBearModel's part tree and pivots)
def polar_bear():
    m = Model("Polar Bear", "polarBear", 128, 128)
    m.part("body", at=(0, 12, 8))
    A = m.abox
    A("body", "shoulder", -5.5, 5, -13, 11, 10, 9)
    A("body", "barrel", -6, 4, -5, 12, 11, 8)
    A("body", "rump", -5.5, 3, 2, 11, 11, 9)
    A("body", "back", -4.5, 2, 3, 9, 1, 7)          # the high, rounded hips
    A("body", "back", -4.5, 3, -5, 9, 1, 8)
    A("body", "chest", -4.5, 8, -15, 9, 7, 2)       # the brisket under the neck
    A("body", "fringe", -6, 15, -12, 12, 2, 20)     # long hair hanging from the belly
    A("body", "rump", -4.5, 4, 11, 9, 9, 1)         # the round back end
    A("body", "shoulder", -4.5, 4.3, -12, 9, 1, 7)   # the withers, rounded
    m.part("tail", "body", (0, -8, 3), (0.7, 0, 0))
    m.box("tail", "tail", -1.5, -1, 0, 3, 3, 2)
    # the long neck carries the narrow head low in front of the shoulders (and turns with it)
    m.part("head", "body", (0, -4, -21), (0.12, 0, 0))
    B = m.box
    B("head", "neck", -3.5, -3.5, -6, 7, 8, 7)
    B("head", "neck", -3, -3, -9, 6, 7, 4)
    B("head", "skull", -3.5, -3.8, -14, 7, 6, 5)
    B("head", "brow", -3, -4.3, -13.5, 6, 1, 4)
    B("head", "cheek", -4, -1, -13, 8, 3, 3)
    B("head", "muzzle", -2.5, -2.5, -19, 5, 4, 5)
    B("head", "bridge", -1.5, -3, -18.5, 3, 1, 5)    # the long Roman nose
    B("head", "nose", -1.5, -2.3, -20, 3, 2, 1)
    m.part("jaw", "head", (0, 1.5, -14))
    B("jaw", "jaw", -2, 0, -4.5, 4, 2, 5)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.6, -3.6, -11.2), (-0.1, 0, s * 0.2))
        B(n, "ear", -1, -2, -0.5, 2, 2, 1)
    for s, side in ((-1, "right"), (1, "left")):
        fl = m.part(f"{side}_front_leg", at=(s * 3.5, 14, -7.5))
        B(fl, "upper_leg", -3, -4, -3, 6, 7, 6)
        B(fl, "leg", -2.5, 3, -2.5, 5, 5, 5)
        B(fl, "feather", -2.5, 3, 2.3, 5, 4, 1)
        B(fl, "paw", -3, 8, -4, 6, 2, 6)
        B(fl, "claws", -2.5, 9, -5, 5, 1, 1)
        hl = m.part(f"{side}_hind_leg", at=(s * 3.5, 14, 7))
        B(hl, "thigh", -3, -6, -4, 6, 9, 8)
        B(hl, "leg", -2.5, 3, -1, 5, 5, 5)
        B(hl, "paw", -3, 8, -4, 6, 2, 7)
        B(hl, "claws", -2.5, 9, -5, 5, 1, 1)
    return m


PB = dict(fur=(238, 233, 216), light=(250, 249, 240), dark=(204, 200, 186), under=(178, 178, 172), stain=(226, 214, 172),
          black=(22, 22, 26), lip=(46, 40, 44), claw=(58, 52, 48), ear_in=(150, 140, 130), pad=(40, 36, 38))


def polar_colour(P, cub=False):
    top, bottom = (14, 24) if cub else (2, 24)

    def colour(T):
        mat, p = T.mat, T.p
        if mat == "fringe" and ragged(T, 0.5):
            return None
        if mat in ("claws",):
            return P["claw"] if T.face != "top" else P["pad"]
        if mat == "nose":
            if T.face == "front" and T.j == T.fh - 1 and T.i == 1:
                return lerp(P["black"], P["fur"], 0.35)  # the groove between the nostrils
            if T.face == "top" and T.j == 0:
                return lerp(P["black"], P["dark"], 0.5)
            return P["black"]
        c = strands(P["fur"], P["dark"], P["light"], T, 0.8)
        c = belly_shade(c, T, top, bottom, P["under"], 0.45)
        # a faint straw-yellow tinge on the rump, the legs and round the muzzle, as old polar bear fur takes
        if mat in ("rump", "thigh", "leg", "paw", "upper_leg", "feather") or (mat == "back" and p[2] > 0):
            c = lerp(c, P["stain"], 0.18 + 0.18 * smooth((p[1] - 12) / 12))
        if mat == "paw" and T.face == "bottom":
            return P["pad"]
        if mat == "paw" and T.face == "front" and T.j == T.fh - 1:
            return lerp(c, P["dark"], 0.5)
        if mat == "ear" and T.face == "front":
            return lerp(P["ear_in"], P["fur"], 0.3) if 0 < T.i < T.fw - 1 or T.j > 0 else c
        if mat == "skull" and T.face in ("left", "right"):
            # the small dark eye near the front of the skull, a shadow under the brow
            front = T.i == T.fw - 1 if T.face == "right" else T.i == 0
            near = T.i == T.fw - 2 if T.face == "right" else T.i == 1
            if T.j == 1 + cub and front:
                return P["black"]
            if T.j == 1 + cub and near:
                return lerp(c, P["dark"], 0.5)
        if mat == "skull" and T.face == "front" and T.j == 1 + cub and T.i in (0, T.fw - 1):
            return P["black"]
        if mat in ("muzzle", "jaw"):
            c = lerp(c, P["stain"], 0.15)
            if mat == "jaw" and T.side and T.j == 0 and T.face != "back":
                return lerp(P["lip"], c, 0.25)  # the dark lips
            if mat == "muzzle" and T.side and T.j == T.fh - 1 and T.face in ("left", "right") and T.i >= T.fw - 3 - (T.face == "left") * 0:
                return lerp(P["lip"], c, 0.5)
        if mat == "muzzle" and T.face == "front" and T.j == T.fh - 1:
            return lerp(P["lip"], c, 0.4)
        return c
    return colour


# ---------------------------------------------------------------- polar bear cub (BearCubModel's part tree and pivots)
def polar_cub():
    m = Model("Polar Bear cub", "polarBearCub", 64, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 18, 0))
    A("body", "barrel", -3.5, 15, -5, 7, 6, 10)
    A("body", "back", -3, 14.3, -4, 6, 1, 8)
    A("body", "chest", -3, 16, -6, 6, 5, 1)
    A("body", "rump", -3, 15.5, 5, 6, 5, 1)
    m.part("tail", "body", (0, -1.5, 5.5), (0.4, 0, 0))
    B("tail", "tail", -1, -1, 0, 2, 2, 1)
    m.part("head", at=(0, 16, -5))
    B("head", "skull", -3, -3, -4, 6, 5, 5)
    B("head", "cheek", -3.5, -1, -3, 7, 3, 3)
    B("head", "muzzle", -1.5, -0.8, -6.5, 3, 3, 3)
    B("head", "nose", -1, -0.8, -7, 2, 1, 1)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.2, -2.6, -1.5), (0, 0, s * 0.3))
        B(n, "ear", -1, -2, -0.5, 2, 2, 1)
    for s, side in ((-1, "right"), (1, "left")):
        for leg, z in (("front", -3), ("hind", 3.5)):
            l = m.part(f"{side}_{leg}_leg", at=(s * 2, 20, z))
            B(l, "leg", -1.5, 0, -1.5, 3, 3, 3)
            B(l, "paw", -1.5, 3, -2.3, 3, 1, 4)
    return m


# ---------------------------------------------------------------- arctic fox (PredatorModel's part tree)
def arctic_fox():
    m = Model("Arctic Fox", "arcticFox", 64, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 17, 0))
    A("body", "chest", -2.5, 14, -5.5, 5, 5, 5)
    A("body", "loin", -2, 14.5, -1, 4, 4, 4)
    A("body", "haunch", -2.5, 14, 2.5, 5, 5, 3)
    A("body", "back", -2, 13.5, -5, 4, 1, 10)
    A("body", "ruff", -3, 13.5, -6.5, 6, 4, 2)      # the thick collar of fur
    A("body", "bib", -2, 17.5, -6, 4, 2, 2)
    A("body", "belly", -2, 18.5, -4, 4, 1, 7)
    m.part("tail", "body", (0, -1.8, 5), (1.15, 0, 0))
    B("tail", "tail", -1.5, 0, -1.5, 3, 4, 3)
    m.part("tail_mid", "tail", (0, 3.5, 0), (-0.25, 0, 0))
    B("tail_mid", "tail", -2, 0, -2, 4, 4, 4)
    m.part("tail_tip", "tail_mid", (0, 4, 0), (-0.25, 0, 0))
    B("tail_tip", "tail_tip", -1.5, 0, -1.5, 3, 3, 3)
    m.part("head", "body", (0, -1.8, -6))
    B("head", "skull", -2.5, -2.5, -4, 5, 4, 4)
    B("head", "cheek", -3.5, -1.2, -3.2, 7, 3, 3)
    B("head", "muzzle", -1.5, -0.6, -5.5, 3, 2, 2)
    B("head", "nose", -0.5, -0.8, -6, 1, 1, 1)
    m.part("jaw", "head", (0, 1.4, -4))
    B("jaw", "jaw", -1, 0, -1.8, 2, 1, 2)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 1.6, -2.3, -1.8), (-0.1, 0, s * 0.3))
        B(n, "ear", -1, -2, -0.5, 2, 2, 1)
    for s, side in ((-1, "right"), (1, "left")):
        fl = m.part(f"{side}_front_leg", at=(s * 1.5, 19, -3.8))
        B(fl, "upper", -1, -2, -1.5, 2, 4, 3)
        m.part(f"{side}_front_lower", fl, (0, 2, 0))
        B(f"{side}_front_lower", "shank", -1, 0, -1, 2, 2, 2)
        B(f"{side}_front_lower", "paw", -1, 2, -1.6, 2, 1, 3)
        hl = m.part(f"{side}_hind_leg", at=(s * 1.5, 19, 3.5))
        B(hl, "thigh", -1.5, -3, -2, 3, 5, 4)
        m.part(f"{side}_hind_lower", hl, (0, 2, 0.5))
        B(f"{side}_hind_lower", "shank", -1, 0, -1, 2, 2, 2)
        B(f"{side}_hind_lower", "paw", -1, 2, -1.6, 2, 1, 3)
    return m


FOX_W = dict(fur=(244, 244, 240), dark=(206, 208, 214), light=(255, 255, 252), under=(190, 194, 204), cream=(236, 230, 214))
FOX_S = dict(fur=(112, 90, 70), dark=(72, 56, 44), light=(150, 124, 96), under=(204, 186, 150), cream=(214, 196, 158))
FOX_EYE, FOX_NOSE, FOX_PAD = (30, 24, 20), (20, 18, 20), (52, 44, 40)


def fox_colour(P, summer):
    def colour(T):
        mat, p = T.mat, T.p
        if mat == "nose":
            return FOX_NOSE
        if mat == "paw" and T.face == "bottom":
            return FOX_PAD
        c = strands(P["fur"], P["dark"], P["light"], T, 0.9 if summer else 0.6)
        if summer:
            # dark back fading to buff flanks and a cream belly, throat and cheeks
            low = smooth((p[1] - 15.5) / 3.5)
            if mat in ("chest", "loin", "haunch", "back", "ruff", "belly", "bib", "thigh", "upper", "tail"):
                c = lerp(c, P["under"], low * 0.9)
            if mat in ("bib", "belly"):
                c = lerp(c, P["cream"], 0.7)
            if mat in ("cheek", "jaw") or (mat == "muzzle" and T.face in ("bottom",)):
                c = lerp(c, P["cream"], 0.75)
            if mat in ("shank", "paw"):
                c = lerp(c, P["dark"], 0.35)
            if mat == "tail_tip":
                c = lerp(c, P["dark"], 0.45)
            if mat == "ear" and T.face == "back":
                c = lerp(c, P["dark"], 0.5)
        else:
            c = belly_shade(c, T, 13.5, 24, P["under"], 0.35)
            if mat in ("cheek", "ruff", "bib"):
                c = lerp(c, P["cream"], 0.3)
        if mat == "ear" and T.face == "front":
            inner = T.j == 1 and T.i == (1 if T.p[0] < 0 else 0)
            return lerp(c, (160, 140, 132) if not summer else (196, 176, 150), 0.5) if inner else c
        if mat == "ear" and T.side and T.j == 0 and T.i in (0, T.fw - 1) and T.face in ("front", "back"):
            return None  # rounded tips
        if mat == "skull" and T.face == "front" and T.j == 1 and T.i in (1, T.fw - 2):
            return FOX_EYE
        if mat == "skull" and T.face == "front" and T.j == 2 and T.i in (1, T.fw - 2):
            return lerp(c, P["dark"], 0.3)
        if mat == "muzzle" and T.face == "front" and T.j == 1:
            return lerp(c, FOX_NOSE, 0.35)
        if mat == "jaw" and T.side and T.j == 0:
            return lerp(c, FOX_NOSE, 0.45)
        return c
    return colour


# ---------------------------------------------------------------- big cats (LeopardModel's part tree and pivots)
def big_cat(name, method, tiger):
    m = Model(name, method, 128, 128)
    A, B = m.abox, m.box
    m.part("body", at=(0, 13, 0))
    if tiger:
        A("body", "chest", -4, 8.5, -9.5, 8, 9, 10)
        A("body", "shoulder", -4.5, 9, -8.5, 9, 6, 6)
        A("body", "belly", -3.5, 9, 0, 7, 8, 5)
        A("body", "rump", -4, 8.5, 4.5, 8, 8, 6)
        A("body", "back", -3, 7.8, -8, 6, 1, 18)
        A("body", "skinfold", -3.5, 17, -6, 7, 1, 11)
        A("body", "neck", -3, 8.5, -12.5, 6, 7, 4)
        tail = ((-1, 0, -1, 2, 8, 2), (-1, 0, -1, 2, 7, 2), (-1, 0, -1, 2, 6, 2))
        rest = (0.55, 0.45, 0.75)
    else:
        A("body", "chest", -3.5, 9, -9, 7, 8, 9)
        A("body", "shoulder", -4, 10, -8, 8, 5, 5)
        A("body", "belly", -3.5, 9.5, -1, 7, 7, 6)
        A("body", "rump", -3.5, 9, 4, 7, 7, 6)
        A("body", "back", -2.5, 8.5, -8, 5, 1, 17)
        A("body", "skinfold", -3, 16.3, -7, 6, 1, 12)  # the long belly fur
        A("body", "neck", -2.5, 9, -12, 5, 6, 4)
        tail = ((-1.5, 0, -1.5, 3, 8, 3), (-1.5, 0, -1.5, 3, 8, 3), (-1.5, 0, -1.5, 3, 7, 3))
        rest = (0.45, 0.35, 0.8)
    parent, y = "body", 0.0
    for k, (bx, by, bz, w, hh, d) in enumerate(tail):
        at = (0, -2.5, 9.5) if k == 0 else (0, tail[k - 1][4] - 0.2, 0)
        parent = m.part(f"tail{k}", parent, at, (rest[k], 0, 0))
        B(parent, "tail" if k < 2 else "tail_tip", bx, by, bz, w, hh, d)
    hy, hz = (10, -12.5) if tiger else (10.5, -12)
    m.part("head", "body", (0, hy - 13, hz))
    if tiger:
        B("head", "skull", -3.5, -4, -5, 7, 6, 5)
        B("head", "ruff", -4.5, -1.5, -4, 9, 4, 3)     # the white whiskered cheeks
        B("head", "brow", -3, -4.5, -4.5, 6, 1, 4)
        B("head", "muzzle", -2.5, -1.5, -8, 5, 3, 3)
        B("head", "nose", -1.5, -1.8, -8.5, 3, 1, 2)
        m.part("jaw", "head", (0, 1.5, -5))
        B("jaw", "jaw", -2, 0, -3, 4, 2, 3)
        ear = (-1.5, -2, -0.5, 3, 2, 1)
        ex, ey = 2.8, -3.6
    else:
        B("head", "skull", -3, -3.5, -4.5, 6, 5, 5)
        B("head", "ruff", -3.5, -1, -3.5, 7, 3, 3)
        B("head", "brow", -2.5, -4, -4, 5, 1, 4)
        B("head", "muzzle", -1.5, -1, -6.5, 3, 2, 2)
        B("head", "nose", -1, -1.2, -7, 2, 1, 1)
        m.part("jaw", "head", (0, 1, -4.5))
        B("jaw", "jaw", -1.5, 0, -2, 3, 1, 2)
        ear = (-1, -2, -0.5, 2, 2, 1)
        ex, ey = 2.4, -3.1
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * ex, ey, -1.5), (0, 0, s * 0.25))
        B(n, "ear", *ear)
    lx = 2.5 if tiger else 2.2
    for s, side in ((-1, "right"), (1, "left")):
        fl = m.part(f"{side}_front_leg", at=(s * lx, 14, -6))
        lo = m.part(f"{side}_front_lower", fl, (0, 4, 0))
        hl = m.part(f"{side}_hind_leg", at=(s * lx, 13, 7.5))
        hlo = m.part(f"{side}_hind_lower", hl, (0, 5, 1))
        if tiger:
            B(fl, "upper", -2, -2.5, -2.5, 4, 7, 5)
            B(lo, "shank", -1.5, 0, -1.5, 3, 4, 3)
            B(lo, "paw", -2, 4, -3, 4, 2, 4)
            B(hl, "thigh", -2, -2.5, -3.5, 4, 8, 6)
            B(hlo, "shank", -1.5, 0, -1.5, 3, 4, 3)
            B(hlo, "paw", -2, 4, -2.5, 4, 2, 4)
        else:
            B(fl, "upper", -1.5, -2, -2, 3, 6, 4)
            B(lo, "shank", -1, 0, -1, 2, 4, 2)
            B(lo, "paw", -2, 4, -2.5, 4, 2, 4)
            B(hl, "thigh", -1.5, -2, -3, 3, 7, 5)
            B(hlo, "shank", -1, 0, -1, 2, 4, 2)
            B(hlo, "paw", -2, 4, -2.5, 4, 2, 4)
    return m


SL = dict(fur=(196, 190, 176), warm=(202, 190, 166), dark=(150, 146, 138), light=(222, 220, 212), white=(236, 234, 228),
          spot=(58, 56, 58), inner=(160, 154, 142), eye=(176, 192, 164), nose=(146, 112, 110), pad=(70, 62, 62))
TG = dict(fur=(220, 124, 46), deep=(190, 96, 30), light=(236, 158, 80), white=(242, 236, 222), stripe=(28, 22, 22),
          eye=(222, 176, 64), nose=(186, 118, 108), pad=(64, 52, 50), dark=(170, 90, 36))


def cat_colour(P, tiger):
    def colour(T):
        mat, p = T.mat, T.p
        x, y, z = p
        if mat == "paw" and T.face == "bottom":
            return P["pad"]
        if mat == "nose":
            return P["nose"] if T.face in ("front", "top") or T.j == 0 else lerp(P["nose"], (40, 30, 30), 0.4)
        if tiger:
            base = lerp(P["fur"], P["deep"], 0.35 * smooth((10 - y) / 3)) if y < 12 else P["fur"]
            c = strands(base, P["dark"], P["light"], T, 0.5)
            # white: belly, inside of the legs, chest, cheeks, chin and the spots over the eyes
            white = 0.0
            if mat in ("chest", "belly", "skinfold", "shoulder", "rump", "neck"):
                white = smooth((y - 14.5) / 2.0)
            if mat in ("upper", "thigh", "shank") and (T.face in ("left", "right") and (x * (1 if T.face == "left" else -1)) < 0):
                white = 0.8  # inner face of the leg
            if mat in ("ruff", "jaw"):
                white = 0.9
            if mat == "muzzle":
                white = 0.85 if T.face != "top" else 0.2
            if mat == "skull" and T.face == "front" and T.j == 1 and T.i in (1, T.fw - 2):
                white = 1.0
            if mat == "paw":
                white = max(white, 0.15)
            c = lerp(c, P["white"], white)
            # stripes: rings round the body and the tail, bands across the legs, a pattern on the face
            stripe = False
            if mat in ("chest", "shoulder", "belly", "rump", "back", "neck", "skinfold"):
                along = z if T.face not in ("front", "back") else abs(x) * 1.3 + 40  # chest and rump faces: across
                k = math.floor((along + 40) / 3.4)
                wob = 1.3 * math.sin(y * 0.33 + k * 1.7) + (0.6 * math.sin(abs(x) * 0.5 + k) if T.face == "top" else 0)
                band = (along + wob + 40) % 3.4
                end = 14.2 + 2.0 * h(k, 1, 9) if T.face != "top" else 99
                stripe = band < (1.25 if abs(y - 11) < 3 else 0.9) and y < end and white < 0.6
            elif mat in ("upper", "thigh", "shank"):
                stripe = (y + 0.7 * math.sin(z * 1.3 + x)) % 2.8 < 0.8 and white < 0.5 and y < 21
            elif mat in ("tail", "tail_tip"):
                stripe = (T.lp[1] + (0 if mat == "tail" else 0.5)) % 2.6 < 1.0 or (mat == "tail_tip" and T.lp[1] > 4)
            elif mat in ("skull", "brow"):
                stripe = (T.face in ("left", "right") and T.j in (0, 3) and T.i % 2 == 0) or \
                         (T.face in ("top", "front") and mat == "brow" and T.i in (1, T.fw - 2)) or \
                         (T.face == "top" and mat == "skull" and T.i in (2, T.fw - 3) and T.j % 2 == 1) or \
                         (T.face == "front" and mat == "skull" and T.j == 0 and T.i in (1, 3, T.fw - 2, T.fw - 4))
            elif mat == "ruff":
                stripe = T.side and T.face in ("left", "right") and T.j == 1 and T.i % 2 == 0
            if stripe:
                c = lerp(P["stripe"], c, 0.12 * T.noise())
            if mat == "ear":
                if T.face == "back":
                    return P["white"] if T.j == 1 and T.i == 1 else P["stripe"]
                return lerp(P["white"], P["fur"], 0.3) if T.face == "front" else P["stripe"]
        else:
            c = strands(lerp(P["fur"], P["warm"], 0.4 * smooth((11 - y) / 3)), P["dark"], P["light"], T, 0.6)
            white = smooth((y - 14.0) / 2.5) if mat in ("chest", "belly", "skinfold", "shoulder", "rump", "neck") else 0
            if mat in ("ruff", "jaw"):
                white = 0.6
            if mat == "muzzle":
                white = 0.7 if T.face != "top" else 0.2
            if mat in ("upper", "thigh", "shank", "paw"):
                white = 0.25 + 0.35 * smooth((y - 18) / 5)
            c = lerp(c, P["white"], white)
            # rosettes: open dark rings round a slightly darker middle on the body, solid spots on the head,
            # legs and lower flanks, broken rings on the tail
            small = mat in ("skull", "brow", "ruff", "shank", "paw", "neck", "upper")
            d, v = cells(p, 4.6 if not small else 2.2, 11)
            r = 1.45 + 0.35 * v
            if mat in ("chest", "shoulder", "belly", "rump", "back", "thigh", "tail") and white < 0.55:
                ring = abs(d - r) < 0.5 and h3(round(x * 2), round(y * 2), round(z * 2), 4) > 0.25
                if ring:
                    c = lerp(P["spot"], c, 0.15 + 0.15 * T.noise())
                elif d < r - 0.4:
                    c = lerp(c, P["inner"], 0.5)
            elif small and d < 0.5 + 0.25 * v and white < 0.7:
                c = lerp(P["spot"], c, 0.2)
            if mat == "tail_tip" and T.lp[1] > 4.5:
                c = lerp(c, P["spot"], 0.8)
            if mat == "ear":
                if T.face == "back":
                    return P["spot"] if T.j == 0 or T.i != 1 else P["light"]
                return lerp(P["white"], P["fur"], 0.4)
        # the eyes, on the sides at the front of the skull, under the brow
        if mat == "skull" and T.face in ("left", "right"):
            front = T.i == T.fw - 1 if T.face == "right" else T.i == 0
            if T.j == 2 and front:
                return P["eye"]
        if mat == "skull" and T.face == "front" and T.j == 2 and T.i in (1, T.fw - 2):
            return P["eye"]
        if mat == "skull" and T.face == "front" and T.j == 2 and T.i in (0, T.fw - 1):
            return (30, 26, 24)
        if mat == "muzzle" and T.face == "front" and T.j == T.fh - 1 and T.i == T.fw // 2:
            return lerp(c, (40, 30, 30), 0.6)  # the cleft down to the mouth
        if mat == "jaw" and T.side and T.j == 0:
            return lerp(c, (40, 30, 30), 0.5)
        return c
    return colour


# ---------------------------------------------------------------- birds (ForestBirdModel's part tree)
def bird(name, method, tw, th, body, tilt, neck, head_boxes, wing, tip, tail_boxes, tail_at, legs, leg_y, ground=21.6):
    m = Model(name, method, tw, th)
    m.part("body", at=(0, 18, 0), rot=(-tilt, 0, 0))
    for b in body:
        m.box("body", *b)
    m.part("neck", "body", neck[0], (tilt, 0, 0))
    m.box("neck", "neck", *neck[1])
    m.part("head", "neck", neck[2])
    for b in head_boxes:
        m.box("head", *b)
    bw = max(-b[1] for b in body if b[0] == "body")  # half width of the main body box
    (wy, wz), (ww, wh, wd) = wing[0], wing[1]
    for s, side in ((-1, "right"), (1, "left")):
        w = m.part(f"{side}_wing", "body", (s * bw, wy, wz))
        m.box(w, "wing", -1 if s < 0 else 0, 0, 0, 1, wh, wd)
        t = m.part(f"{side}_wing_tip", w, (s * 0.5, wh, 0), (1.5708, 0, 0))
        m.box(t, "primaries", -1 if s < 0 else 0, 0, 0, 1, tip[0], tip[1])
    m.part("tail", "body", tail_at[0], (tail_at[1], 0, 0))
    for b in tail_boxes:
        m.box("tail", *b)
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_leg", at=(s * legs[0], leg_y, 0.5))
        for b in legs[1]:
            m.box(l, *b)
    # sit the (tilted) body just on the legs
    low = m.lowest(m.subtree("body") - m.subtree("left_wing") - m.subtree("right_wing") - {"tail"})
    m.parts["body"][1][1] += ground - low
    return m


def snowy_owl():
    return bird("Snowy Owl", "snowyOwl", 64, 64,
                body=[("body", -3.5, -3, -4, 7, 6, 8), ("breast", -3, 2.5, -4.5, 6, 1, 7), ("breast", -3, -1, -5, 6, 4, 1),
                      ("flank", -2.5, -3.6, -3, 5, 1, 6), ("flank", -2.5, -2, 4, 5, 4, 1)],
                tilt=1.45,
                neck=((0, -1.5, -3.2), ("neck", -2, -1, -2, 4, 2, 4)[1:], (0, -1, 0)),
                head_boxes=[("crown", -3.5, -5, -3, 7, 5, 5), ("disc", -3, -4.6, -3.6, 6, 4, 1),
                            ("crown", -2.5, -5.5, -2.5, 5, 1, 4), ("beak", -0.5, -2.4, -4.3, 1, 1, 1),
                            ("chin", -2.5, -0.2, -3, 5, 1, 4)],
                wing=((-2.2, -3.2), (1, 6, 7)), tip=(6, 4),
                tail_boxes=[("tail", -2.5, 0, 0, 5, 1, 4)], tail_at=((0, -1.5, 3.5), -0.25),
                legs=(1.5, [("leg", -1.5, 0, -1.5, 3, 2, 3), ("foot", -1.5, 2, -2.2, 3, 1, 3)]), leg_y=21)


def black_grouse():
    return bird("Black Grouse", "blackGrouse", 64, 64,
                body=[("body", -2.5, -2.5, -4, 5, 5, 8), ("breast", -2, -1.5, -5, 4, 4, 1), ("back", -2, -3, -3, 4, 1, 6)],
                tilt=0.1,
                neck=((0, -1.6, -3.6), ("neck", -1.5, -2, -1.5, 3, 3, 3)[1:], (0, -1.8, -0.3)),
                head_boxes=[("crown", -1.5, -3, -2, 3, 3, 3), ("beak", -0.5, -1.6, -3, 1, 1, 1),
                            ("wattle", -1.8, -2.9, -1.8, 1, 1, 2), ("wattle", 0.8, -2.9, -1.8, 1, 1, 2)],
                wing=((-2.2, -3.5), (1, 4, 7)), tip=(4, 3),
                tail_boxes=[("tail", -2, 0, 0, 4, 1, 5), ("lyre", -3.5, -0.5, 3, 2, 1, 3), ("lyre", 1.5, -0.5, 3, 2, 1, 3),
                            ("lyre_tip", -4.5, 0, 5, 1, 1, 2), ("lyre_tip", 3.5, 0, 5, 1, 1, 2), ("undertail", -1.5, 1, 0, 3, 1, 3)],
                tail_at=((0, -1.6, 3.5), -0.2),
                legs=(1.2, [("leg", -1, 0, -1, 2, 2, 2), ("foot", -1.5, 2, -2, 3, 1, 3)]), leg_y=21, ground=21.3)


def lammergeier():
    return bird("Lammergeier", "lammergeier", 128, 64,
                body=[("body", -3, -3, -5.5, 6, 6, 11), ("breast", -2.5, -2, -6.5, 5, 5, 1), ("back", -2.5, -3.5, -4, 5, 1, 8)],
                tilt=0.5,
                neck=((0, -2, -4.8), ("neck", -2, -3, -2, 4, 4, 4)[1:], (0, -2.8, -0.2)),
                head_boxes=[("crown", -2, -3.5, -3.5, 4, 4, 5), ("beak", -1, -2.2, -6.2, 2, 1, 3),
                            ("hook", -0.5, -1.2, -6.2, 1, 1, 1), ("beard", -0.5, 0.3, -4.6, 1, 2, 1)],
                wing=((-2.6, -4.8), (1, 6, 9)), tip=(12, 5),
                tail_boxes=[("tail", -2.5, 0, 0, 5, 1, 10)], tail_at=((0, -2.2, 5), -0.12),
                legs=(1.6, [("leg", -1.5, 0, -1.5, 3, 3, 3), ("foot", -1.5, 3, -2.5, 3, 1, 3)]), leg_y=20, ground=20.6)


OWL = dict(white=(246, 246, 242), shade=(214, 216, 222), bar=(52, 50, 56), eye=(244, 202, 40), black=(24, 22, 26))


def owl_colour(T):
    P, mat = OWL, T.mat
    c = lerp(P["white"], P["shade"], 0.25 * T.noise() + 0.12 * max(0, T.n[1]))
    if mat == "beak":
        return P["black"]
    if mat == "foot":
        if T.face == "front" and T.j == 0 and T.i in (0, 2):
            return P["black"]  # the talons poking out of the feathered toes
        return c
    if mat == "disc" and T.face == "front":
        if T.j == 1 and T.i in (1, 4):
            return P["eye"]
        if T.j == 1 and T.i in (2, 3):
            return P["black"]  # the pupils, inside the yellow
        if T.j == 2 and T.i in (1, 4):
            return lerp(P["eye"], P["white"], 0.25)
        if T.j == 0 and T.i in (1, 2, 3, 4):
            return lerp(P["white"], P["shade"], 0.5)  # the brow over the eyes
        return P["white"]
    # sparse dark bars: chevrons on the back, wings and tail, fine dots on the crown, a few on the breast
    bars = {"wing": 0.55, "primaries": 0.6, "tail": 0.6, "crown": 0.12, "body": 0.32, "flank": 0.4, "breast": 0.15}.get(mat, 0)
    if bars and T.face not in ("bottom",):
        row = (T.j if T.side else T.i) % 3
        if mat in ("primaries", "tail"):
            row = (T.j + T.i) % 3 if mat == "tail" else T.j % 3
        if row == 0 and h(T.i // 2, T.j, T.k + 3) < bars:
            c = lerp(P["bar"], c, 0.25)
    return c


GR = dict(black=(22, 24, 34), sheen=(44, 58, 96), brown=(52, 44, 40), white=(240, 240, 236), red=(214, 36, 36),
          beak=(34, 30, 30), leg=(120, 112, 104))
GH = dict(base=(146, 110, 70), dark=(60, 44, 30), buff=(200, 168, 120), light=(176, 140, 96))


def grouse_colour(hen):
    def colour(T):
        mat = T.mat
        if mat == "beak":
            return GR["beak"]
        if hen:
            if mat == "wattle":
                return lerp(GH["base"], (170, 70, 50), 0.3)
            c = lerp(GH["base"], GH["light"], 0.4 * T.noise())
            if mat in ("breast",) or (T.face == "bottom" and mat == "body"):
                c = lerp(c, GH["buff"], 0.45)
            if (T.j + (T.i % 2)) % 2 == 0 and h(T.i, T.j, T.k) < 0.75 and mat not in ("foot", "leg"):
                c = lerp(c, GH["dark"], 0.55 if mat in ("wing", "primaries", "tail", "body", "back") else 0.35)
            if mat in ("lyre", "lyre_tip"):
                return None
            if mat == "undertail":
                return lerp(GR["white"], GH["buff"], 0.4)
            if mat in ("leg",):
                return lerp(GH["buff"], GR["leg"], 0.4)
            if mat == "foot":
                return GR["leg"]
            if mat == "wing" and T.side and T.j == 1 and T.face in ("left", "right"):
                return lerp(GR["white"], GH["buff"], 0.5)  # her faint wing bar
            if mat == "crown" and T.face in ("left", "right") and T.j == 1 and T.i == T.fw - 2:
                return GR["beak"]  # eye
            return c
        c = lerp(GR["black"], GR["sheen"], 0.55 * max(0, -T.n[1]) + 0.25 * T.noise())
        if mat in ("wing", "primaries"):
            c = lerp(GR["brown"], GR["black"], 0.3 * T.noise())
            if mat == "wing" and T.face in ("left", "right") and T.j in (1, 2):
                return GR["white"]  # the white wing bar
            if mat == "wing" and T.face == "top":
                return GR["white"] if T.i == 0 else c
        if mat == "undertail":
            return GR["white"]
        if mat == "wattle":
            return GR["red"] if T.face != "bottom" else lerp(GR["red"], GR["black"], 0.4)
        if mat == "crown" and T.face in ("left", "right") and T.j == 1 and T.i == T.fw - 2:
            return (10, 10, 12)
        if mat == "crown" and T.face in ("left", "right") and T.j == 0:
            return lerp(GR["red"], c, 0.35)
        if mat == "leg":
            return lerp(GR["leg"], GR["white"], 0.3) if T.j < T.fh - 1 else GR["leg"]
        if mat == "foot":
            return GR["leg"]
        if mat == "body" and T.face == "bottom" and T.j > T.fh - 3:
            return GR["white"]
        return c
    return colour


LG = dict(slate=(66, 66, 72), slate_dark=(40, 40, 46), shaft=(176, 170, 160), cream=(240, 228, 204), rust=(224, 148, 80),
          rust_dark=(184, 106, 56), black=(20, 18, 18), beak=(118, 114, 108), red=(206, 40, 30), iris=(236, 226, 190),
          leg=(124, 120, 116))


def lammergeier_colour(T):
    P, mat = LG, T.mat
    if mat in ("beak",):
        return P["beak"] if not (T.face in ("left", "right") and T.j == 0) else lerp(P["beak"], P["black"], 0.3)
    if mat == "hook":
        return lerp(P["beak"], P["black"], 0.6)
    if mat == "beard":
        if ragged(T, 0.4, 3):
            return None
        return P["black"]
    if mat in ("wing", "primaries", "tail", "back"):
        c = lerp(P["slate"], P["slate_dark"], 0.35 * T.noise() + (0.25 if mat == "primaries" else 0))
        # each feather with its pale shaft streak
        if T.side and mat != "back" and (T.i + (T.j // 3)) % 3 == 1 and h(T.i, T.j // 2, T.k) > 0.35:
            c = lerp(c, P["shaft"], 0.45)
        if mat == "tail" and T.face in ("top", "bottom"):
            # the wedge: corners of the far end cut away
            r = T.rel()
            if r[2] > 0.7 and abs(r[0] - 0.5) > (1 - r[2]) * 1.6 + 0.15:
                return None
            if T.i in (0, T.fw - 1) and T.j % 2 == 0:
                c = lerp(c, P["shaft"], 0.3)
        if mat == "tail" and T.side and T.face in ("left", "right", "back"):
            r = T.rel()
            if T.face == "back" and abs(r[0] - 0.5) > 0.25:
                return None
        return c
    if mat in ("body", "breast", "neck", "leg"):
        c = lerp(P["rust"], P["rust_dark"], 0.3 * T.noise() + 0.25 * max(0, T.n[1]))
        if mat == "body" and (T.face == "top" or (T.side and T.n[1] < -0.4 and T.face != "front")):
            c = lerp(P["slate"], P["slate_dark"], 0.3 * T.noise())
        if mat == "leg" and T.edge_bottom and h(T.i, 0, T.k) < 0.5:
            c = lerp(c, P["rust_dark"], 0.5)
        return c
    if mat == "foot":
        return P["black"] if T.face == "front" and T.j == 0 and T.i % 2 == 0 else P["leg"]
    if mat == "crown":
        c = lerp(P["cream"], P["rust"], 0.25 * T.noise() + 0.2 * smooth(T.rel()[1]))
        # the black mask from the beak through the eye, the red-ringed pale eye
        if T.face in ("left", "right"):
            front = (T.fw - 1 - T.i) if T.face == "right" else T.i  # 0 at the front
            if T.j == 2 and front == 1:
                return P["iris"]
            if T.j == 2 and front in (0, 2):
                return P["red"]
            if T.j == 2 and front == 3:
                return P["black"]
            if T.j == 3 and front <= 1:
                return P["black"]
        if T.face == "front" and T.j in (2, 3) and T.i in (0, T.fw - 1):
            return P["black"]
        return c
    return P["cream"]


# ---------------------------------------------------------------- pika (PikaModel; built at twice its size)
def pika():
    m = Model("Pika", "pika", 64, 64)
    A, B = m.abox, m.box
    m.part("body", at=(0, 19.5, 1))
    A("body", "body", -3.5, 16, -3, 7, 7, 8)
    A("body", "back", -3, 15.3, -2, 6, 1, 6)
    A("body", "rump", -3, 16.5, 5, 6, 6, 1)
    A("body", "chest", -3, 17, -4, 6, 5, 1)
    A("body", "flank", -4, 18, -2, 8, 4, 6)
    m.part("head", at=(0, 18.5, -3))
    B("head", "skull", -3, -3.5, -5, 6, 5, 5)
    B("head", "cheek", -3.5, -1, -4.5, 7, 3, 3)
    B("head", "snout", -2, -1.5, -6, 4, 3, 1)
    B("head", "chin", -1.5, 1.5, -5.5, 3, 1, 2)
    B("head", "whiskers", -5, 0, -6.5, 10, 0, 2)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.2, -3.2, -2), (-0.25, s * -0.25, s * 0.45))
        B(n, "ear", -1.5, -3, -0.5, 3, 3, 1)
    for s, side in ((-1, "right"), (1, "left")):
        l = m.part(f"{side}_front_leg", at=(s * 2, 22, -2.5))
        B(l, "leg", -1, 0, -1, 2, 2, 2)
        B(l, "toes", -1, 1, -1.8, 2, 1, 1)
        l = m.part(f"{side}_hind_leg", at=(s * 2.3, 21.5, 3))
        B(l, "haunch", -1.5, -1, -2, 3, 3, 4)
        B(l, "foot", -1, 1.5, -3.5, 2, 1, 2)
    return m


PK = dict(fur=(150, 124, 94), dark=(104, 84, 62), light=(186, 160, 124), belly=(222, 208, 182), ear=(70, 58, 50),
          rim=(236, 230, 214), eye=(16, 14, 14), shine=(200, 200, 200), nose=(70, 50, 44), foot=(196, 182, 160), whisker=(30, 26, 24))


def pika_colour(T):
    P, mat, p = PK, T.mat, T.p
    if mat == "whiskers":
        r = T.rel()
        return P["whisker"] if T.face in ("top", "bottom") and T.j == (T.i % 2) and abs(r[0] - 0.5) > 0.32 else None
    if mat in ("foot", "toes"):
        return P["foot"]
    if mat == "ear":
        if T.face in ("front", "back") and T.j == 0 and T.i in (0, 2):
            return None  # rounded
        if T.face == "front":
            return P["rim"] if T.j == 0 or T.i in (0, 2) else lerp(P["ear"], (150, 110, 100), 0.35)
        return P["rim"] if T.j == 0 else lerp(P["ear"], P["dark"], 0.3)
    c = P["fur"]
    # fine dark ticking in the coat, buff belly and throat
    c = lerp(c, P["dark"], 0.5 * (h3(p[0] * 2, p[1] * 2, p[2] * 2, 4) > 0.72))
    c = lerp(c, P["light"], 0.4 * (h3(p[0] * 2, p[1] * 2, p[2] * 2, 6) > 0.8))
    c = belly_shade(c, T, 15, 24, P["belly"], 0.75)
    if mat in ("chin", "cheek") and T.face != "top":
        c = lerp(c, P["belly"], 0.45)
    if mat == "skull" and T.face in ("left", "right"):
        front = (T.fw - 1 - T.i) if T.face == "right" else T.i
        if T.j == 1 and front in (1, 2):
            return P["shine"] if front == 1 and T.face == "left" else P["eye"]
        if T.j == 2 and front in (1, 2):
            return P["eye"]
    if mat == "snout" and T.face == "front":
        if T.j == 0 and T.i in (1, 2):
            return P["nose"]
        if T.j == 1 and T.i in (1, 2):
            return lerp(c, P["nose"], 0.5)
        if T.j == 2:
            return lerp(c, P["belly"], 0.4)
    return c


# ---------------------------------------------------------------- the hay pile (block model + texture)
def hay_pile():
    """Pika Hay Pile's textures (its heap model: gen_fauna_cold.py): cut grass going from green to straw, and the stalks."""
    hay_textures().save(ROOT / "src/main/resources/assets/mechanical/textures/block/pika_hay_pile.png")
    stalks_texture().save(ROOT / "src/main/resources/assets/mechanical/textures/block/pika_hay_pile_stalks.png")


HAY = [(170, 176, 86), (196, 186, 104), (214, 198, 122), (146, 160, 74), (224, 210, 146), (124, 142, 64), (188, 160, 92)]
FLOWERS = [(236, 214, 70), (244, 244, 236), (236, 214, 70)]


def hay_textures():
    """Cut grass going from green to straw: short stalks lying every which way, a few dried flowers among them."""
    im = Image.new("RGBA", (16, 16))
    px = im.load()
    for y in range(16):
        for x in range(16):
            px[x, y] = (*shade(HAY[1], 0.72 + 0.1 * h(x, y, 1)), 255)
    for k in range(70):  # stalks: short strokes, random direction
        x, y = h(k, 1, 2) * 16, h(k, 2, 2) * 16
        a = h(k, 3, 2) * math.pi
        c = HAY[int(h(k, 4, 2) * len(HAY))]
        ln = 2 + int(h(k, 5, 2) * 4)
        for t in range(ln):
            xx, yy = int(x + math.cos(a) * t) % 16, int(y + math.sin(a) * t) % 16
            px[xx, yy] = (*shade(c, 0.85 + 0.25 * (t / ln)), 255)
    for k in range(4):
        x, y = int(h(k, 6, 3) * 16), int(h(k, 7, 3) * 16)
        px[x, y] = (*FLOWERS[k % len(FLOWERS)], 255)
    return im


def stalks_texture():
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = im.load()
    for k, x in enumerate((0, 1, 2, 3)):
        top = int(h(k, 1, 9) * 2)
        for y in range(top, 4):
            px[x, y] = (*HAY[(k * 3 + y) % len(HAY)], 255)
    px[1, 0] = (*FLOWERS[0], 255)
    return im


# ---------------------------------------------------------------- output
# PredatorModel / LeopardModel find each leg's second segment as its child "lower"
LOWER = {f"{s}_{l}_lower": "lower" for s in ("right", "left") for l in ("front", "hind")}


def skinned(m, im, keep):
    out = _skinner().skin(im.copy())
    src, dst = im.load(), out.load()
    for b in m.boxes:
        if b["mat"] in keep:
            u, v = b["uv"]
            for tx in range(u, u + 2 * b["d"] + 2 * b["w"]):
                for ty in range(v, v + b["d"] + b["h"]):
                    dst[tx, ty] = src[tx, ty]
    return out


def save(im, rel):
    p = TEX / rel
    p.parent.mkdir(parents=True, exist_ok=True)
    im.save(p)


def main():
    models = []
    bear = polar_bear()
    cub = polar_cub()
    fox = arctic_fox()
    leopard = big_cat("Snow Leopard", "snowLeopard", False)
    tiger = big_cat("Siberian Tiger", "siberianTiger", True)
    owl, grouse, lamm, pk = snowy_owl(), black_grouse(), lammergeier(), pika()
    for m in (bear, cub, fox, leopard, tiger, owl, grouse, lamm, pk):
        m.pack()
        models.append(m)
    claws = ("claws", "nose", "beak", "hook", "foot", "toes")
    out = [
        (bear, polar_colour(PB), "polar_bear", True),
        (cub, polar_colour(PB, True), "polar_bear_cub", False),
        (fox, fox_colour(FOX_W, False), "arctic_fox", True),
        (fox, fox_colour(FOX_S, True), "arctic_fox_summer", True),
        (leopard, cat_colour(SL, False), "snow_leopard", True),
        (tiger, cat_colour(TG, True), "siberian_tiger", True),
        (owl, owl_colour, "snowy_owl", True),
        (grouse, grouse_colour(False), "black_grouse", True),
        (grouse, grouse_colour(True), "black_grouse_hen", True),
        (lamm, lammergeier_colour, "lammergeier", True),
        (pk, pika_colour, "pika", True),
    ]
    for m, colour, name, skin in out:
        im = m.paint(colour)
        save(im, f"wildlife/{name}.png")
        if skin:
            save(skinned(m, im, claws), f"skinned/wildlife/{name}.png")
    body = "\n".join(m.java() for m in models)
    for old, new in LOWER.items():
        body = body.replace(f'"{old}"', f'"{new}"')
    src = f"""package com.mechanical.client.wildlife.cold;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_cold_models.py (with the skins it paints for them): the cold fauna's parts and boxes. Do not edit. */
public final class ColdGeometry {{
{body}
    private ColdGeometry() {{}}
}}
"""
    JAVA.write_text(src, encoding="utf-8")
    hay_pile()
    for m in models:
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes")


if __name__ == "__main__":
    main()
