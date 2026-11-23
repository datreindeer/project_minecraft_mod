"""The older small mammals and birds remodelled ("small" review package), each from one table, the way
gen_temperate_models.py builds the temperate fauna:

- src/main/java/com/mechanical/client/wildlife/SmallGeometry.java: one LayerDefinition per animal (generated, do not
  edit). Every model keeps the part names, pivots and rest angles the animated model class expects, so walking,
  flying, swimming, pecking, roosting, the special poses and the carcass poses all work on it: BadgerModel, HareModel
  (also the desert hare and the leveret), HedgehogModel, SquirrelModel, PheasantModel, PheasantChickModel,
  FieldVoleModel, MoleModel, WildCatModel, BirdModel (crow, goose, rock dove, ptarmigan, seagull), ForestBirdModel
  (capercaillie, great bustard, eagle-owl, raven, golden eagle, griffon vulture), WaterfowlBirdModel (mallard, mute
  swan) and WaderModel (grey heron, common crane). Their createXxxLayer methods delegate here (the generated sections
  of BirdModel, ForestBirdModel, WaterfowlBirdModel and WaderModel are rewritten by this script).
- their skins (textures/entity/wildlife/...), painted per texel from where it sits on the model, and the carcass
  skins: plucked for the birds, flesh for the mammals (textures/entity/skinned/...), dried for the mammals that age
  (textures/entity/skinned_aged/...).

Each animal is built at its own texel density (texels per model pixel: 2 for the larger ones, 4 for the small ones).
Usage: python scripts/gen_small_models.py. Deterministic. Also run at the end of the older texture scripts that paint
these animals (gen_bird_textures, gen_fauna_forestbirds_textures, gen_fauna_waterfowl_textures,
gen_small_wildlife_textures, gen_forest_critter_textures, gen_fauna_meadow_textures, gen_baby_small_wildlife_textures),
so re-running one of those cannot bring the old boxes back.
"""
import math
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_temperate_models as GT  # noqa: E402
from gen_temperate_models import h, lerp, hexc  # noqa: E402
from gen_cold_models import rot, mul, apply  # noqa: E402
from wildlife_paint import _skinner  # noqa: E402
from gen_aged_textures import dry  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/mechanical/textures/entity"
JDIR = ROOT / "src/main/java/com/mechanical/client/wildlife"
JAVA = JDIR / "SmallGeometry.java"


class Tex(GT.Tex):
    """GT.Tex plus where the texel sits inside all boxes of its material (gu, gv, gw: 0..1) and that extent."""


GT.Tex = Tex  # GT.Model.paint builds its texels from the module's Tex


class M(GT.Model):
    def __init__(self, name, px, tex):
        super().__init__(name, px, tex)
        self.light = (1.12, 0.72, 0.18)

    def blob(self, p, mat, cx, cy, cz, w, hh, d, r):
        """A box with its twelve edges bevelled by r: three crossing boxes round the centre (cx, cy, cz)."""
        r = min(r, w / 2, hh / 2, d / 2)
        for bw, bh, bd in ((w, hh - 2 * r, d - 2 * r), (w - 2 * r, hh, d - 2 * r), (w - 2 * r, hh - 2 * r, d)):
            if bw > 0 and bh > 0 and bd > 0:
                self.box(p, mat, cx - bw / 2, cy - bh / 2, cz - bd / 2, bw, bh, bd)

    def pack(self):
        """GT.Model.pack, growing the declared texture (height first, then width) until every box fits."""
        def size(b):
            return math.ceil(2 * b[7] + 2 * b[5] - 1e-6), math.ceil(b[7] + b[6] - 1e-6)
        order = sorted(range(len(self.boxes)), key=lambda i: -size(self.boxes[i])[1])
        while True:
            x = y = shelf = 0
            uv = {}
            for i in order:
                bw, bh = size(self.boxes[i])
                if x + bw > self.tex[0]:
                    x, y, shelf = 0, y + shelf, 0
                uv[i] = (x, y)
                x += bw
                shelf = max(shelf, bh)
            if y + shelf <= self.tex[1]:
                break
            self.tex = (self.tex[0], self.tex[1] * 2) if self.tex[1] < self.tex[0] else (self.tex[0] * 2, self.tex[1])
        for i, b in enumerate(self.boxes):
            del b[8:]
            b.append(uv[i])

    def ext(self):
        e = {}
        for b in self.boxes:
            p, mat, x, y, z, w, hh, d = b[:8]
            o = e.setdefault(mat, [x, y, z, x + w, y + hh, z + d])
            o[:] = [min(o[0], x), min(o[1], y), min(o[2], z), max(o[3], x + w), max(o[4], y + hh), max(o[5], z + d)]
        return e

    def paint(self, colour):
        ext = self.ext()

        def wrap(mat, t):
            x0, y0, z0, x1, y1, z1 = t.ext = ext[mat]
            t.gu = (t.x - x0) / max(1e-6, x1 - x0)
            t.gv = (t.y - y0) / max(1e-6, y1 - y0)
            t.gw = (t.z - z0) / max(1e-6, z1 - z0)
            return colour(mat, t)
        return super().paint(wrap)

    def matrix(self, p):
        if p == "root":
            return ((1, 0, 0), (0, 1, 0), (0, 0, 1)), (0, 0, 0)
        parent, at, r = self.parts[p]
        pm, pt = self.matrix(parent)
        t = apply(pm, at)
        return mul(pm, rot(*r)), (pt[0] + t[0], pt[1] + t[1], pt[2] + t[2])

    def lowest(self, parts):
        low = -99
        for b in self.boxes:
            if b[0] in parts:
                mm, t = self.matrix(b[0])
                for cx in (0, b[5]):
                    for cy in (0, b[6]):
                        for cz in (0, b[7]):
                            low = max(low, apply(mm, (b[2] + cx, b[3] + cy, b[4] + cz))[1] + t[1])
        return low

    def move(self, p, dy):
        parent, at, r = self.parts[p]
        self.parts[p] = (parent, (at[0], at[1] + dy, at[2]), r)


# ------------------------------------------------------------------ shared painting helpers
def fur(t, base, dark, light, streak=4, rough=0.5):
    """gen_temperate_models.fur at half the texel resolution (strands two texels wide) and gentler: at four texels per
    pixel the full-resolution version reads as static."""
    gx, gy = t.gx // 2, t.gy // 2
    if not t.vert:
        return lerp(base, dark, 0.08 + rough * 0.35 * h(gx, gy, t.salt))
    col = h(gx, 0, t.salt)
    run = h(gx, gy // streak, t.salt + 1)
    c = lerp(dark, base, 0.62 + 0.22 * col * rough + 0.2 * run)
    if (gy + int(col * 7)) % streak == 0:
        c = lerp(c, light, 0.3)
    return c


def eye(t, r, ew=0.3, ev=0.42, iris=(22, 16, 12), ring=None, pupil=None, glint=True):
    """An eye on the outer side faces of the texel's material (t.ext), centred ew of the way back and ev of the way
    down it, radius r (model pixels): a lit colour (4-tuple) or None."""
    if t.face not in ("left", "right"):
        return None
    x0, y0, z0, x1, y1, z1 = t.ext
    if abs(abs(t.x) - max(abs(x0), abs(x1))) > 1e-4:
        return None
    cz, cy = z0 + ew * (z1 - z0), y0 + ev * (y1 - y0)
    dz, dy = t.z - cz, t.y - cy
    dd = dz * dz + dy * dy
    half = 0.5 / t.px
    if dd <= r * r:
        if glint and abs(dz + r * 0.42) <= half + 1e-6 and abs(dy + r * 0.42) <= half + 1e-6:
            return (236, 236, 228, 0)
        if pupil and dd <= (r * 0.5) ** 2:
            return (*pupil, 0)
        return (*iris, 0)
    if ring and dd <= (r + 1.0 / t.px) ** 2:
        return (*ring, 0)
    return None


def mottle(t, base, dark, light, k=0.5):
    c = lerp(base, light, k * 0.5 * h(t.gx, t.gy, t.salt))
    v = h(t.gx // 2, t.gy // 2, t.salt + 3)
    if v < 0.18:
        c = lerp(c, dark, 0.6 * k + 0.2)
    return c


def scallop(t, base, edge, dark=None, cw=None, ch=None, k=0.12):
    """Overlapping feathers in model units (independent of the texel density): rows of staggered cells with a pale
    (or dark) lower fringe."""
    cw = cw or 1.5
    ch = ch or 1.0
    py = (t.Y if t.vert else t.Z) / ch
    row = math.floor(py)
    pxv = (t.Z if t.side else t.X) / cw + (row % 2) * 0.5
    cell = h(math.floor(pxv) + 50, row + 50, t.salt)
    c = lerp(base, dark or base, k * cell * 2)
    fy = py - row
    fx = pxv - math.floor(pxv)
    if fy > 1 - 1.0 / (t.px * ch) - 1e-6:
        c = lerp(c, edge, 0.45)
    elif fx < 1.0 / (t.px * cw):
        c = lerp(c, edge, 0.12)
    return c


def flight_feathers(t, base, dark, light, along="y", lane=1.0, tipdark=0.35):
    """Long feathers lying side by side: lanes across the box (pale shaft edge on one side of each), darkening to
    the tip end (along: the axis they run along, 'y' for the wing slab, 'z' for the tail)."""
    a = t.y if along == "y" else t.z
    across = t.z if along == "y" else t.x
    k = math.floor(across / lane)
    f = across / lane - k
    c = lerp(base, dark, 0.15 + 0.25 * h(k + 70, 3, t.salt))
    s = t.gv if along == "y" else t.gw
    c = lerp(c, dark, tipdark * s)
    if f < 1.0 / (t.px * lane) + 1e-6:
        c = lerp(c, light, 0.5)
    return c


def ragged(t, frac=0.5, salt=0):
    return h(t.gx, 3, t.salt + salt) < frac


def plucked(P, keep, eye_args, base=None):
    """The plucked carcass skin: goose-pimpled skin, darker on the back; keep the bill, legs and eye."""
    skin, dark, pore = hexc("#e8b8a0"), hexc("#b07a64"), hexc("#d09a84")

    def col(mat, t):
        if mat in keep:
            return base(mat, t)
        if mat in ("tail", "primaries", "crest", "plume", "skirt", "fringe", "bustle", "whisker", "ruff_fringe", "tuft"):
            return None if mat != "tail" else lerp(skin, dark, 0.4)
        if eye_args and mat == eye_args[0] and (e := eye(t, *eye_args[1:], iris=(40, 30, 30), glint=False)):
            return e
        c = lerp(skin, dark, 0.22 * t.n + 0.25 * (t.face == "top"))
        return lerp(c, pore, 0.6) if h(t.gx, t.gy, 9) < 0.15 else c
    return col


# ================================================================== MAMMALS
# ------------------------------------------------------------------ badger (BadgerModel: body[tail, head], legs on the root)
def badger():
    m = M("Badger", 4, (64, 64))
    m.light = (1.14, 0.6, 0.3)
    m.part("body", "root", (0, 18.5, 0))
    # a low wedge: narrow shoulders, broad heavy rump, the back rising to the hips
    m.blob("body", "body", 0, 0.25, -3.25, 6, 5, 6.5, 0.5)
    m.blob("body", "body", 0, -0.1, 0.5, 6.5, 5.5, 5, 0.5)
    m.blob("body", "rump", 0, -0.2, 3.75, 7, 5.5, 5.5, 1.0)
    m.box("body", "back", -2.5, -3.25, -4.5, 5, 0.5, 10)
    m.box("body", "belly", -2.5, 2.5, -5, 5, 0.5, 9.5)
    for s in (-1, 1):
        # the long guard hairs hanging below the flanks
        m.box("body", "skirt", (3.25 if s > 0 else -3.75), 1.0, -5.5, 0.5, 2.5, 11.5)
    m.part("tail", "body", (0, -2, 6.5), (-0.5, 0, 0))
    m.box("tail", "tail", -1.25, -0.75, 0, 2.5, 1.5, 3.5)
    m.box("tail", "tail_tip", -1, -0.5, 3.5, 2, 1, 0.5)
    m.part("head", "body", (0, -0.5, -6))
    m.blob("head", "skull", 0, -0.25, -1.75, 4.5, 4, 4, 0.75)
    m.blob("head", "cheek", 0, 0.75, -2.5, 5, 2.5, 3, 0.5)
    m.box("head", "snout", -1.5, -1.0, -5.5, 3, 2.5, 2)
    m.box("head", "snout", -1.0, -0.75, -7, 2, 2, 1.5)
    m.box("head", "nose", -1.0, -0.75, -7.75, 2, 1.25, 0.75)
    m.box("head", "jaw", -1.0, 1.25, -6.5, 2, 0.75, 3)
    for s in (-1, 1):
        m.box("head", "ear", (1.5 if s > 0 else -2.5), -2.5, -1.0, 1, 1, 0.5)
    for s, side in ((-1, "right"), (1, "left")):
        fl = side + "_front_leg"
        m.part(fl, "root", (s * 2, 20, -4))
        m.box(fl, "upper_leg", -1.5, -1.5, -1.5, 3, 3.5, 3)
        m.box(fl, "leg", -1.25, 1.5, -1.25, 2.5, 1.75, 2.5)
        m.box(fl, "paw", -1.5, 3, -2.0, 3, 1, 3)
        m.box(fl, "claws", -1.0, 3.5, -2.75, 2, 0.5, 0.75)
        hl = side + "_hind_leg"
        m.part(hl, "root", (s * 2.3, 20, 4.5))
        m.box(hl, "thigh", -1.75, -2, -1.75, 3.5, 4, 3.5)
        m.box(hl, "leg", -1.25, 1.5, -1.25, 2.5, 1.75, 2.5)
        m.box(hl, "paw", -1.5, 3, -2.0, 3, 1, 3)
        m.box(hl, "claws", -1.25, 3.5, -2.5, 2.5, 0.5, 0.5)
    return m


BADGER = dict(grey=hexc("#9c9a96"), tip=hexc("#dcdad2"), under=hexc("#3a3634"), black=hexc("#18161a"),
              white=hexc("#ecebe4"), cream=hexc("#d8d2c0"), nose=hexc("#141214"), claw=hexc("#a89c88"), pink=hexc("#8a6a68"))
BADGER_SK = dict(BADGER)


def badger_colour(P):
    def grizzled(t):
        # each guard hair pale at the tip, black in a band below it, the woolly grey under-fur showing between
        c = fur(t, P["grey"], P["under"], P["tip"], streak=3, rough=0.7)
        if t.vert and h(t.gx // 2, t.gy // 3, t.salt + 4) < 0.14:
            c = lerp(c, P["black"], 0.4)
        return c

    def col(mat, t):
        if mat in ("body", "rump", "back"):
            c = grizzled(t)
            low = max(0.0, (t.Y - 19.5) / 2.5)
            return lerp(c, P["under"], min(1.0, low) * 0.8)
        if mat == "skirt":
            if t.vert and t.v > 0.55 and ragged(t, 0.5 + 0.3 * (t.v - 0.55)):
                return None
            return lerp(grizzled(t), P["under"], 0.3 * t.v)
        if mat == "belly":
            return lerp(P["black"], P["under"], 0.4 * t.n)
        if mat in ("tail", "tail_tip"):
            return lerp(grizzled(t), P["tip"], 0.25 if mat == "tail_tip" else 0.0)
        if mat in ("skull", "cheek", "snout", "jaw"):
            # the white face with the two black stripes from the nose back over the eyes to the ears
            if mat == "skull" and (e := eye(t, 0.5, 0.18, 0.45, iris=(20, 14, 12))):
                return e
            stripe = abs(abs(t.X) - 1.25) < 0.62 if mat in ("skull", "snout") else False
            if mat == "skull" and t.face in ("left", "right"):
                stripe = 0.25 < t.gv < 0.75
            if mat == "snout" and t.face in ("left", "right"):
                stripe = t.y < -0.1
            if mat == "cheek":
                c = lerp(P["white"], P["cream"], 0.3 * t.n)
                return lerp(c, P["black"], 0.85) if t.face in ("left", "right") and t.gv < 0.35 and t.gw < 0.7 else c
            if mat == "jaw":
                return lerp(P["black"], P["under"], 0.3)
            if stripe and not (mat == "skull" and t.face == "top" and abs(t.X) < 0.6):
                return lerp(P["black"], P["under"], 0.25 * t.n)
            return lerp(P["white"], P["cream"], 0.35 * t.n)
        if mat == "nose":
            return lerp(P["nose"], (60, 50, 50), 0.4) if t.face == "top" else P["nose"]
        if mat == "ear":
            return P["white"] if t.face in ("top", "front") or t.v < 0.4 else P["black"]
        if mat in ("upper_leg", "thigh"):
            return lerp(fur(t, P["under"], P["black"], P["grey"], streak=3), P["black"], 0.3 + 0.5 * t.v)
        if mat in ("leg", "paw"):
            return lerp(P["black"], P["under"], 0.3 * t.n)
        if mat == "claws":
            return lerp(P["claw"], (120, 110, 96), 0.4 * (t.face != "top") + 0.2 * t.n)
        return P["grey"]
    return col


# ------------------------------------------------------------------ hare (HareModel; pivot at the hips)
def hare(name="Hare"):
    m = M(name, 4, (64, 64))
    m.light = (1.14, 0.68, 0.22)
    m.part("body", "root", (0, 16.5, 3.5))
    m.blob("body", "chest", 0, 0.0, -6.5, 4, 4.5, 4, 0.75)
    m.blob("body", "loin", 0, -0.4, -3.5, 4, 4.5, 3, 0.5)
    m.blob("body", "rump", 0, -0.25, -1.0, 5, 5.5, 4.5, 1.0)
    m.box("body", "back", -1.5, -3.25, -6.5, 3, 0.5, 7)
    m.box("body", "belly", -1.5, 2.25, -7.5, 3, 0.5, 5)
    m.part("head", "body", (0, -1.5, -7))
    m.blob("head", "skull", 0, -1.6, -1.25, 3.5, 3.5, 4, 0.75)
    m.blob("head", "cheek", 0, -0.75, -1.75, 4, 2.5, 2.5, 0.5)
    m.box("head", "brow", -1.25, -3.6, -2.75, 2.5, 0.5, 2.5)
    m.part("nose", "head", (0, -1, -3.5))
    m.box("nose", "muzzle", -1.25, -1.0, -1.0, 2.5, 2, 1.5)
    m.box("nose", "nosepad", -0.75, -1.0, -1.25, 1.5, 0.75, 0.25)
    m.box("nose", "chin", -0.75, 1.0, -0.5, 1.5, 0.5, 1)
    m.box("nose", "whisker", -2.25, 0.0, -0.75, 4.5, 0, 0.5)
    for s, n in ((1, "left_ear"), (-1, "right_ear")):
        m.part(n, "head", (s * 1.0, -3.25, -0.25), (-0.3, 0, s * 0.12))
        m.box(n, "ear", -0.875, -7.0, -0.25, 1.75, 7, 0.5)
        m.box(n, "ear", -0.625, -7.5, -0.25, 1.25, 0.5, 0.5)
    m.part("tail", "body", (0, -1.5, 0.75), (-0.5, 0, 0))
    m.blob("tail", "tail", 0, 0, 0.75, 2.5, 2.5, 2, 0.5)
    for s, side in ((-1, "right"), (1, "left")):
        fl = side + "_front_leg"
        m.part(fl, "body", (s * 1.1, 2.0, -7))
        m.box(fl, "foreleg", -0.5, -0.5, -0.5, 1.0, 5.0, 1.0)
        m.box(fl, "forepaw", -0.75, 4.25, -1.25, 1.5, 0.75, 1.75)
        hn = side + "_haunch"
        m.part(hn, "body", (s * 2.0, 0.5, -2))
        m.blob(hn, "haunch", s * 0.25, 0.75, 0, 2, 6, 5.5, 0.75)
        ft = side + "_hind_foot"
        m.part(ft, hn, (0, 2, 2))
        m.box(ft, "shank", -0.75, -0.5, -0.75, 1.5, 4.5, 1.5)
        m.box(ft, "hindfoot", -0.875, 4.0, -5.25, 1.75, 1, 6.25)
    return m


HARE = dict(fur=hexc("#9a7650"), dark=hexc("#4e3820"), light=hexc("#c8a676"), belly=hexc("#ece6da"), rufous=hexc("#b07a48"),
            black=hexc("#1c1612"), eye=hexc("#c88a2c"), nose=hexc("#6a4a3c"), ear_in=hexc("#b4987c"), white=hexc("#f2efe8"))
DESERT_HARE = dict(HARE, fur=hexc("#c0a070"), dark=hexc("#7a5e3c"), light=hexc("#e2c896"), rufous=hexc("#c8a070"),
                   ear_in=hexc("#d4b494"))
LEVERET = dict(HARE, fur=hexc("#8a6a48"), dark=hexc("#4a3420"), light=hexc("#b49470"), rufous=hexc("#9a7048"))


def hare_colour(P):
    def agouti(t):
        # banded hairs: brown with black-tipped flecks and pale ticking, running back along the body
        c = fur(t, P["fur"], P["dark"], P["light"], streak=2, rough=0.8)
        k = h(t.gx // 2, t.gy // 2, t.salt + 6)
        if k < 0.1:
            c = lerp(c, P["black"], 0.35)
        elif k > 0.9:
            c = lerp(c, P["light"], 0.4)
        return c

    def col(mat, t):
        if mat in ("chest", "loin", "rump", "back", "haunch"):
            c = agouti(t)
            if mat == "chest" and t.face == "front":
                c = lerp(c, P["rufous"], 0.4)
            if t.face == "bottom" or (t.vert and t.Y > 17.6 and mat != "haunch"):
                return lerp(P["belly"], P["light"], 0.25 * t.n)
            return c
        if mat == "belly":
            return lerp(P["belly"], P["light"], 0.25 * t.n)
        if mat in ("skull", "cheek", "brow"):
            if mat == "skull" and (e := eye(t, 0.7, 0.3, 0.5, iris=P["eye"], ring=P["black"], pupil=(16, 12, 10))):
                return e
            c = agouti(t)
            if mat == "skull" and t.side and eye(t, 1.05, 0.3, 0.5):
                return lerp(P["belly"], P["light"], 0.4)  # pale ring round the eye
            if mat == "cheek" and t.face != "top":
                c = lerp(c, P["rufous"], 0.35)
                if t.gv > 0.6:
                    c = lerp(c, P["belly"], 0.6)
            return c
        if mat == "muzzle":
            c = lerp(agouti(t), P["light"], 0.35)
            if t.face == "front" and abs(t.x) < 0.2 and t.y > -0.2:
                return P["dark"]  # the split lip
            return lerp(c, P["belly"], 0.5) if t.y > 0.3 else c
        if mat == "nosepad":
            return P["nose"]
        if mat == "chin":
            return P["belly"]
        if mat == "whisker":
            return (232, 226, 210) if (t.face in ("top", "bottom") and abs(t.x) > 1.3 and h(t.gx, t.gy, 2) < 0.4) else None
        if mat == "ear":
            # long ears: brown outside, pale buff edges and inside, the black tip
            tip = t.y < -5.6
            if t.face == "front" and not tip:
                return lerp(P["ear_in"], P["light"], 0.5) if 0.2 < t.u < 0.8 else P["light"]
            return lerp(P["black"], P["dark"], 0.3 * t.n) if tip else agouti(t)
        if mat == "tail":
            return P["white"] if (t.face in ("bottom", "back") or t.y > 0.2) else P["black"]
        if mat in ("foreleg", "shank"):
            return lerp(agouti(t), P["light"], 0.3)
        if mat in ("forepaw", "hindfoot"):
            # buff feet, paler only along the soles and toes
            if t.face == "bottom":
                return lerp(P["dark"], P["fur"], 0.5)
            return lerp(agouti(t), P["light"], 0.55 if (t.face == "front" or t.v > 0.6) else 0.3)
        return P["fur"]
    return col


# ------------------------------------------------------------------ hedgehog (HedgehogModel)
def hedgehog():
    m = M("Hedgehog", 4, (64, 64))
    m.light = (1.15, 0.6, 0.25)
    m.part("body", "root", (0, 21, 0))
    # the spiny mantle: a low dome in three tiers, a ragged fringe of spines all round its edge
    m.blob("body", "spines", 0, -1.0, 0, 6.5, 3.5, 8.5, 1.0)
    m.blob("body", "spines", 0, -3.0, 0.25, 5.25, 1.5, 7.0, 0.5)
    m.box("body", "spines", -1.75, -4.25, -2.5, 3.5, 0.75, 5.5)
    m.box("body", "fringe", -3.5, -1.5, -4.75, 7, 3.0, 9.75)
    m.box("body", "crown", -2.5, -3.0, -4.75, 5, 1.5, 0.5)
    m.box("body", "underfur", -2.75, 0.5, -3.75, 5.5, 1.25, 7.5)
    m.part("head", "body", (0, 0.5, -4))
    m.blob("head", "face", 0, -0.25, -1.0, 3.25, 2.75, 2.5, 0.5)
    m.box("head", "snout", -0.875, -0.25, -3.0, 1.75, 1.5, 1.0)
    m.box("head", "snout", -0.5, 0.0, -3.75, 1, 1, 0.75)
    m.box("head", "nose", -0.375, 0.0, -4.0, 0.75, 0.5, 0.25)
    for s in (-1, 1):
        m.box("head", "ear", (1.25 if s > 0 else -2.0), -1.75, -1.0, 0.75, 0.75, 0.5)
    for s, side in ((-1, "right"), (1, "left")):
        for leg, z in (("front", -2.5), ("hind", 2.5)):
            n = f"{side}_{leg}_leg"
            m.part(n, "root", (s * 1.8, 22, z))
            m.box(n, "leg", -0.5, -0.5, -0.5, 1, 2.0, 1)
            m.box(n, "paw", -0.625, 1.5, -1.0, 1.25, 0.5, 1.5)
    return m


HEDGEHOG = dict(spine=hexc("#5a4632"), band=hexc("#2a2018"), tip=hexc("#e4d8bc"), face=hexc("#8a6e52"), face_l=hexc("#b49a78"),
                under=hexc("#a08868"), nose=hexc("#141012"), paw=hexc("#3e3028"), eye=hexc("#0e0a0a"))


def hedgehog_colour(P):
    def spines(t):
        # each spine banded: pale tip, dark band, brown base; they run back (the bands stripe along the length)
        lane = math.floor((t.X if t.face != "front" and t.face != "back" else t.Y) * t.px)
        ph = ((t.Z if t.face != "front" and t.face != "back" else t.X) * t.px + 3 * h(lane, 1, t.salt)) % 5
        c = P["spine"] if ph < 2 else P["band"] if ph < 3.5 else P["tip"]
        if h(t.gx, t.gy, t.salt + 2) < 0.15:
            c = lerp(c, P["tip"], 0.6)
        return lerp(c, P["band"], 0.25 * h(lane, 3, t.salt))

    def col(mat, t):
        if mat == "spines":
            return spines(t)
        if mat == "fringe":
            if t.face in ("top", "bottom"):
                return None
            if t.v > 0.45 and ragged(t, 0.25 + (t.v - 0.45) * 1.4):
                return None
            return spines(t)
        if mat == "crown":
            return None if (t.face == "front" and t.v < 0.5 and ragged(t, 0.5)) else spines(t)
        if mat == "underfur":
            return lerp(P["under"], P["face"], 0.3 * t.n)
        if mat == "face":
            if (e := eye(t, 0.4, 0.15, 0.45, iris=P["eye"])):
                return e
            c = lerp(P["face"], P["face_l"], 0.5 * h(t.gx, t.gy, t.salt))
            return lerp(c, P["face_l"], 0.35) if t.y > 0.3 else c
        if mat == "snout":
            return lerp(P["face"], P["paw"], 0.3 + 0.3 * t.n)
        if mat == "nose":
            return P["nose"] if t.face != "top" else lerp(P["nose"], (120, 120, 120), 0.3)
        if mat == "ear":
            return lerp(P["face"], P["paw"], 0.4)
        if mat in ("leg", "paw"):
            return lerp(P["paw"], P["face"], 0.25 * t.n)
        return P["spine"]
    return col


# ------------------------------------------------------------------ red squirrel (SquirrelModel; tail rest angles kept)
def squirrel():
    m = M("Red squirrel", 4, (64, 64))
    m.light = (1.15, 0.7, 0.2)
    m.part("body", "root", (0, 20, 2))
    m.blob("body", "chest", 0, 0.0, -4.0, 3, 3, 2.5, 0.5)
    m.blob("body", "loin", 0, -0.1, -2.5, 2.75, 3, 1.75, 0.5)
    m.blob("body", "rump", 0, -0.2, -1.25, 3.5, 3.5, 2.75, 0.75)
    m.box("body", "belly", -1.0, 1.25, -4.75, 2, 0.5, 3.5)
    m.part("head", "body", (0, -0.5, -5))
    m.blob("head", "skull", 0, -1.0, -1.0, 3, 2.75, 3, 0.5)
    m.blob("head", "cheek", 0, -0.35, -1.6, 3.25, 1.75, 2, 0.5)
    m.box("head", "muzzle", -0.875, -1.25, -3.25, 1.75, 1.5, 1)
    m.box("head", "nose", -0.5, -1.25, -3.5, 1, 0.5, 0.25)
    m.box("head", "chin", -0.625, 0.25, -3.0, 1.25, 0.5, 1)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 1.0, -2.25, -0.5), (0, 0, s * 0.15))
        m.box(n, "ear", -0.5, -1.75, -0.25, 1, 1.75, 0.5)
        m.box(n, "tuft", -0.25, -3.0, -0.125, 0.5, 1.5, 0.25)   # the long ear tufts
    for s, side in ((-1, "right"), (1, "left")):
        fl = side + "_front_leg"
        m.part(fl, "body", (s * 1.0, 1.0, -4))
        m.box(fl, "foreleg", -0.375, -0.5, -0.375, 0.75, 3.0, 0.75)
        m.box(fl, "paw", -0.5, 2.5, -0.75, 1, 0.5, 1)
        hn = side + "_haunch"
        m.part(hn, "body", (s * 1.5, 0.5, 0.5))
        m.blob(hn, "haunch", 0, 0, -0.5, 1.25, 3, 3, 0.25)
        ft = side + "_hind_foot"
        m.part(ft, hn, (0, 1.5, 0))
        m.box(ft, "shank", -0.5, -0.5, -0.5, 1, 1.75, 1)
        m.box(ft, "foot", -0.5, 1.0, -2.5, 1, 0.5, 3)
    # the bushy tail: three segments (rest angles as SquirrelModel animates them), each wrapped in a ragged fluff of hair
    m.part("tail", "body", (0, -0.5, -0.5), (1.0, 0, 0))
    m.box("tail", "tail", -1.0, -1.0, 0, 2, 2, 4)
    m.box("tail", "fluff", -1.5, -1.5, 0.5, 3, 3, 3.5)
    m.part("tail_mid", "tail", (0, 0, 3.5), (0.9, 0, 0))
    m.box("tail_mid", "tail", -1.5, -1.5, 0, 3, 3, 5)
    m.box("tail_mid", "fluff", -2.25, -2.0, 0.25, 4.5, 4, 4.75)
    m.part("tail_tip", "tail_mid", (0, 0, 4.5), (0.9, 0, 0))
    m.box("tail_tip", "tail", -1.5, -1.5, 0, 3, 3, 3)
    m.box("tail_tip", "fluff", -2.0, -2.0, 0, 4, 4, 3.75)
    return m


SQUIRREL = dict(fur=hexc("#b2582a"), dark=hexc("#6e3014"), light=hexc("#d88a4e"), belly=hexc("#f0e8d8"), grey=hexc("#8a6a5a"),
                black=hexc("#141010"), ring=hexc("#e8d8c0"), nose=hexc("#4a2c22"), tuft=hexc("#5a2a14"))


def squirrel_colour(P):
    def col(mat, t):
        if mat in ("chest", "loin", "rump", "haunch"):
            c = fur(t, P["fur"], P["dark"], P["light"], streak=3, rough=0.7)
            if mat != "haunch" and (t.face == "bottom" or (t.vert and t.Y > 20.6 and t.face != "back")):
                return lerp(P["belly"], P["light"], 0.15 * t.n)  # the white belly with a sharp edge
            if mat == "chest" and t.face == "front" and t.y > -0.6:
                return lerp(P["belly"], P["light"], 0.15 * t.n)
            return c
        if mat == "belly":
            return P["belly"]
        if mat in ("skull", "cheek"):
            if mat == "skull" and (e := eye(t, 0.34, 0.3, 0.4, iris=(14, 10, 8), ring=lerp(P["ring"], P["light"], 0.7))):
                return e
            c = fur(t, P["fur"], P["dark"], P["light"], streak=2, rough=0.6)
            if mat == "cheek" and (t.face == "bottom" or t.gv > 0.65):
                return P["belly"]
            return c
        if mat == "muzzle":
            c = lerp(P["light"], P["fur"], 0.4)
            return lerp(c, P["belly"], 0.6) if t.y > -0.4 else c
        if mat == "nose":
            return P["nose"]
        if mat == "chin":
            return P["belly"]
        if mat == "ear":
            return lerp(P["light"], P["fur"], 0.4) if t.face == "front" else fur(t, P["fur"], P["dark"], P["light"])
        if mat == "tuft":
            if t.face in ("top", "bottom") or (t.v < 0.6 and ragged(t, 0.25 + (0.6 - t.v))):
                return None
            return lerp(P["tuft"], P["dark"], 0.4 * t.n)
        if mat == "tail":
            return fur(t, P["fur"], P["dark"], P["light"], streak=3)
        if mat == "fluff":
            # long hairs: transparent gaps all round the outline, darker deep in
            r = max(abs(t.gu - 0.5), abs(t.gv - 0.5)) if t.face in ("front", "back") else max(abs(t.gw - 0.5), abs(t.gv - 0.5)) if t.side else max(abs(t.gu - 0.5), abs(t.gw - 0.5))
            if r > 0.3 and ragged(t, 0.35 + (r - 0.3) * 2.0):
                return None
            c = fur(t, P["fur"], P["dark"], P["light"], streak=4, rough=0.9)
            return lerp(c, P["dark"], 0.25) if h(t.gx, t.gy // 3, t.salt) < 0.25 else c
        if mat in ("foreleg", "shank"):
            return lerp(P["fur"], P["light"], 0.3 * t.n)
        if mat in ("paw", "foot"):
            return lerp(P["dark"], P["fur"], 0.5) if t.face != "front" else P["black"]
        return P["fur"]
    return col


# ------------------------------------------------------------------ field vole (FieldVoleModel)
def field_vole():
    m = M("Field vole", 4, (32, 32))
    m.light = (1.12, 0.68, 0.2)
    m.part("body", "root", (0, 21.5, 0))
    m.blob("body", "body", 0, -0.1, 0.25, 3.5, 3.25, 4.25, 0.75)
    m.blob("body", "body", 0, 0.1, -1.4, 3.0, 2.75, 2.0, 0.5)
    m.blob("body", "rump", 0, 0.0, 1.9, 3.25, 3.0, 1.5, 0.75)
    m.part("head", "body", (0, -0.5, -2.5))
    m.blob("head", "skull", 0, -0.1, -0.9, 2.75, 2.5, 2.25, 0.5)
    m.box("head", "snout", -0.875, -0.25, -2.75, 1.75, 1.5, 1)
    m.box("head", "nose", -0.5, -0.25, -3.0, 1, 0.5, 0.25)
    m.box("head", "whiskers", -1.75, 0.5, -2.5, 3.5, 0, 0.5)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 1.0, -1.0, -0.5), (0, 0, s * 0.3))
        m.box(n, "ear", -0.375, -0.5, 0, 0.75, 0.5, 0.25)
    for s, side in ((-1, "right"), (1, "left")):
        for leg, z in (("front", -1.5), ("hind", 1.5)):
            n = f"{side}_{leg}_leg"
            m.part(n, "root", (s * 1.0, 23, z))
            m.box(n, "leg", -0.375, -0.25, -0.375, 0.75, 1.0, 0.75)
            m.box(n, "toes", -0.5, 0.75, -0.75 if leg == "front" else -1.0, 1, 0.25, 1 if leg == "front" else 1.25)
    m.part("tail", "body", (0, -0.5, 2.5), (-0.3, 0, 0))
    m.box("tail", "tail", -0.25, -0.25, 0, 0.5, 0.5, 2.25)
    return m


VOLE = dict(fur=hexc("#7a5e40"), dark=hexc("#3e2c1c"), light=hexc("#a6865e"), belly=hexc("#a49a8a"), nose=hexc("#3a2620"),
            toes=hexc("#b4a08c"), tail=hexc("#4e3a2a"), ear=hexc("#6a5038"))


def vole_colour(P):
    def col(mat, t):
        if mat in ("body", "rump", "skull", "snout"):
            if mat == "skull" and (e := eye(t, 0.36, 0.25, 0.4, iris=(10, 8, 8))):
                return e
            c = fur(t, P["fur"], P["dark"], P["light"], streak=3, rough=0.8)
            if t.face == "bottom" or (t.vert and t.Y > 22.6):
                c = lerp(c, P["belly"], 0.6)
            if mat == "snout":
                c = lerp(c, P["dark"], 0.25)
            return c
        if mat == "nose":
            return P["nose"]
        if mat == "whiskers":
            return (210, 200, 186) if (abs(t.x) > 1.0 and h(t.gx, t.gy, 2) < 0.4) else None
        if mat == "ear":
            return lerp(P["ear"], P["dark"], 0.3 * t.n)
        if mat == "tail":
            return lerp(P["tail"], P["belly"], 0.4) if t.face == "bottom" else P["tail"]
        if mat == "leg":
            return lerp(P["fur"], P["dark"], 0.4)
        if mat == "toes":
            return P["toes"]
        return P["fur"]
    return col


# ------------------------------------------------------------------ mole (MoleModel)
def mole():
    m = M("Mole", 4, (64, 32))
    m.light = (1.2, 0.6, 0.2)
    m.part("body", "root", (0, 21.5, 0))
    m.blob("body", "body", 0, 0, 0, 4.5, 4.0, 7.5, 1.25)
    m.blob("body", "body", 0, 0.1, -3.25, 4.0, 3.5, 2.0, 0.75)
    m.part("head", "body", (0, 0.5, -3.5))
    m.blob("head", "head", 0, -0.1, -1.0, 3.0, 2.75, 2.25, 0.75)
    m.box("head", "snout", -0.75, -0.25, -3.25, 1.5, 1.25, 1.25)
    m.box("head", "snout", -0.5, -0.125, -3.75, 1, 0.75, 0.5)
    m.box("head", "tip", -0.375, -0.125, -4.0, 0.75, 0.75, 0.25)
    m.box("head", "whiskers", -1.5, 0.5, -3.25, 3, 0, 0.5)
    for s, n in ((-1, "right_hand"), (1, "left_hand")):
        m.part(n, "body", (s * 2.25, 1.0, -3.0), (0, 0, s * 0.4))
        # the spade: a broad pink palm turned out, five long pale claws along its lower edge
        x0 = 0 if s > 0 else -1.0
        m.box(n, "arm", x0, -1.0, -1.0, 1, 1.5, 2)
        m.box(n, "palm", x0 + (0.25 if s > 0 else 0), 0.25, -1.75, 0.75, 2.5, 3.25)
        m.box(n, "claws", x0 + (0.25 if s > 0 else 0), 2.75, -1.75, 0.75, 0.75, 3.25)
    for s, n in ((-1, "right_foot"), (1, "left_foot")):
        m.part(n, "root", (s * 1.5, 23, 2.5))
        m.box(n, "foot", -0.5, -0.25, -1.0, 1, 1.25, 2)
    m.part("tail", "body", (0, -1, 3.5), (0.6, 0, 0))
    m.box("tail", "tail", -0.375, -0.375, 0, 0.75, 0.75, 1.5)
    return m


MOLE = dict(fur=hexc("#26242a"), sheen=hexc("#4a4a56"), dark=hexc("#121216"), pink=hexc("#d8a098"), pink_d=hexc("#a8706a"),
            claw=hexc("#ece4d2"), tip=hexc("#e8a8a0"))


def mole_colour(P):
    def col(mat, t):
        if mat in ("body", "head"):
            # short velvet that shines grey-silver where the light catches it
            c = lerp(P["fur"], P["sheen"], 0.35 * h(t.gx, t.gy, t.salt) + (0.25 if t.face == "top" else 0))
            if mat == "head" and t.side and abs(t.gw - 0.25) < 0.08 and abs(t.gv - 0.4) < 0.06:
                return (8, 8, 10, 0)  # the tiny eye, lost in the fur
            return c
        if mat == "snout":
            return lerp(P["pink_d"], P["pink"], 0.4 + 0.3 * t.n) if t.z < -3.25 else lerp(P["fur"], P["pink_d"], 0.25 + 0.5 * (1 - t.gw))
        if mat == "tip":
            if t.face == "front" and abs(t.x) > 0.05 and abs(t.y - 0.25) < 0.15:
                return P["pink_d"]  # nostrils
            return P["tip"]
        if mat == "whiskers":
            return (96, 90, 88) if (abs(t.x) > 0.8 and h(t.gx, t.gy, 2) < 0.35) else None
        if mat == "arm":
            return lerp(P["fur"], P["sheen"], 0.3 * t.n)
        if mat == "palm":
            c = lerp(P["pink"], P["pink_d"], 0.3 * t.n + 0.2 * (t.face == "back"))
            return lerp(c, P["pink_d"], 0.5) if t.side and int(t.z * 4) % 3 == 0 and t.v > 0.5 else c
        if mat == "claws":
            return P["claw"] if not (t.side and int(t.z * 4 + 10) % 3 == 0) else lerp(P["claw"], P["pink_d"], 0.6)
        if mat == "foot":
            return lerp(P["pink"], P["pink_d"], 0.3 * t.n) if t.y > 0.25 else P["fur"]
        if mat == "tail":
            return lerp(P["fur"], P["sheen"], 0.3)
        return P["fur"]
    return col


# ------------------------------------------------------------------ wild cat (WildCatModel; the kitten is it scaled down)
def wild_cat():
    m = M("Wild cat", 4, (64, 64))
    m.light = (1.14, 0.66, 0.24)
    m.part("body", "root", (0, 15, 0))
    m.blob("body", "chest", 0, 0.75, -3.75, 5.5, 6.5, 5, 1.25)
    m.blob("body", "belly", 0, 0.75, 0, 5, 6, 4, 1.0)
    m.blob("body", "haunch", 0, 0.5, 3.75, 5.5, 6.5, 5, 1.25)
    m.box("body", "back", -2, -2.6, -5.5, 4, 1.5, 11)  # fills the dips between the three body blobs
    m.box("body", "belly_fur", -2, 3.75, -4, 4, 0.5, 8)
    m.part("head", "body", (0, -1.5, -6))
    m.blob("head", "skull", 0, -1.0, -2.0, 4.5, 4, 4, 0.75)
    m.blob("head", "ruff", 0, 0.25, -2.25, 5.5, 2.5, 3, 0.75)   # the broad whiskered cheeks
    m.box("head", "brow", -1.75, -3.25, -3.75, 3.5, 0.5, 2)
    m.box("head", "muzzle", -1.25, -0.5, -5.0, 2.5, 1.75, 1.25)
    m.box("head", "nose", -0.5, -0.75, -5.25, 1, 0.5, 0.5)
    m.box("head", "chin", -0.75, 1.25, -4.5, 1.5, 0.5, 1)
    m.box("head", "whiskers", -3.5, 0.5, -4.75, 7, 0, 1)
    for s, n in ((-1, "right_ear"), (1, "left_ear")):
        m.part(n, "head", (s * 2.0, -2.5, -1.75), (0, 0, s * 0.45))
        m.box(n, "ear", -1.25, -1.25, -0.25, 2.5, 1.25, 0.5)
        m.box(n, "ear", -0.75, -1.75, -0.25, 1.5, 0.5, 0.5)
    m.part("tail", "body", (0, -1.5, 6), (-0.9, 0, 0))
    m.blob("tail", "tail", 0, 0, 2.5, 3.5, 3.5, 5, 0.75)
    m.part("tail_tip", "tail", (0, 0, 5), (0.35, 0, 0))
    m.blob("tail_tip", "tail_tip", 0, 0, 2.0, 3.5, 3.5, 4, 1.0)
    for s, side in ((-1, "right"), (1, "left")):
        fl = side + "_front_leg"
        m.part(fl, "root", (s * 1.5, 17, -4.5))
        m.box(fl, "upper", -1.5, -1.0, -1.5, 3, 4.5, 3)
        m.box(fl, "foreleg", -1.0, 2.5, -1.0, 2, 3.75, 2)
        m.box(fl, "paw", -1.25, 6, -1.75, 2.5, 1, 2.75)
        hl = side + "_hind_leg"
        m.part(hl, "root", (s * 1.5, 16, 4.5))
        m.box(hl, "thigh", -1.5, -1.5, -2.25, 3, 5.5, 4.5)
        m.box(hl, "hock", -1.0, 3.5, -0.75, 2, 3.75, 2)
        m.box(hl, "paw", -1.25, 7, -2.0, 2.5, 1, 3)
    return m


WILDCAT = dict(fur=hexc("#8e8270"), warm=hexc("#a69070"), dark=hexc("#3a3028"), light=hexc("#c4b498"), stripe=hexc("#2a221c"),
               belly=hexc("#d4c4a4"), nose=hexc("#c88a80"), eye=hexc("#b8b040"), white=hexc("#ece4d4"), pad=hexc("#3a2a26"))
KITTEN = dict(WILDCAT, fur=hexc("#9a8c78"), light=hexc("#cabca2"), stripe=hexc("#3a2e24"), eye=hexc("#7a98a8"))


def wild_cat_colour(P):
    def tabby(t, base):
        return fur(t, base, P["dark"], P["light"], streak=3, rough=0.6)

    def col(mat, t):
        if mat in ("chest", "belly", "haunch", "back", "belly_fur"):
            c = tabby(t, lerp(P["fur"], P["warm"], 0.3 * h(t.gx // 3, t.gy // 3, 4)))
            low = (t.Y - 15.5) / 2.5
            if t.face == "bottom" or low > 0.35 or mat == "belly_fur":
                return lerp(P["belly"], P["light"], 0.25 * t.n)
            # broken, wavy stripes down the flanks, the dark line along the spine
            if t.face == "top" or (t.vert and t.Y < 12.6):
                if abs(t.X) < 0.6 or (t.face == "top" and abs(t.X) < 0.9):
                    return lerp(P["stripe"], c, 0.15)
            if t.side and (t.Z + 1.2 * math.sin(t.Y * 0.9)) % 2.6 < 0.9 and h(math.floor(t.Z / 2.6), math.floor(t.Y), 3) < 0.8:
                return lerp(P["stripe"], c, 0.25 + 0.3 * low)
            return c
        if mat in ("skull", "brow", "ruff"):
            if mat == "skull" and (e := eye(t, 0.6, 0.15, 0.55, iris=P["eye"], ring=P["dark"], pupil=(14, 12, 10))):
                return e
            c = tabby(t, P["fur"])
            if mat == "skull" and t.face == "top" and int(t.X * 2 + 10) % 2 == 0 and abs(t.X) < 1.6:
                return lerp(P["stripe"], c, 0.2)  # the four lines on the forehead
            if mat == "skull" and t.face == "front" and t.gv < 0.55 and int(t.X * 2 + 10) % 2 == 0 and abs(t.X) < 1.6:
                return lerp(P["stripe"], c, 0.2)
            if mat == "ruff":
                c = lerp(c, P["light"], 0.4)
                if t.side and abs(t.gv - 0.45) < 0.1:
                    return lerp(P["stripe"], c, 0.3)  # the cheek stripe
                if t.gv > 0.6:
                    return lerp(P["white"], P["belly"], 0.3)
            return c
        if mat == "muzzle":
            return lerp(P["white"], P["belly"], 0.35 * t.n)
        if mat == "nose":
            return P["nose"]
        if mat == "chin":
            return P["white"]
        if mat == "whiskers":
            return (236, 232, 222) if (abs(t.x) > 2.3 and h(t.gx, t.gy, 2) < 0.35) else None
        if mat == "ear":
            if t.box[4] < 1 and t.vert and t.v < 0.5 and (t.u < 0.17 or t.u > 0.83):
                return None  # the rounded tip
            if t.face == "front":
                return lerp(P["light"], P["nose"], 0.25) if 0.25 < t.u < 0.75 and t.v > 0.25 else P["light"]
            return lerp(P["warm"], P["dark"], 0.3 * t.n)
        if mat in ("tail", "tail_tip"):
            # the thick blunt tail: dark rings, the black blunt tip
            c = tabby(t, P["fur"])
            z = t.z + (5 if mat == "tail_tip" else 0)
            if mat == "tail_tip" and t.z > 2.3:
                return lerp(P["stripe"], P["dark"], 0.3 * t.n)
            if (z + 0.4) % 2.4 < 0.9:
                return lerp(P["stripe"], c, 0.15)
            return c
        if mat in ("upper", "thigh", "foreleg", "hock"):
            c = tabby(t, P["fur"])
            if t.side and (t.y * 1.0) % 2.2 < 0.6 and mat in ("upper", "thigh"):
                c = lerp(P["stripe"], c, 0.35)
            if mat == "hock" and t.face == "back":
                return lerp(P["stripe"], P["dark"], 0.3)
            return c
        if mat == "paw":
            return P["pad"] if t.face == "bottom" else lerp(P["light"], P["belly"], 0.3)
        return P["fur"]
    return col


# ------------------------------------------------------------------ pheasant (PheasantModel: body[head, wings, tail], legs)
def pheasant():
    m = M("Pheasant", 4, (64, 64))
    m.part("body", "root", (0, 18, 0), (-0.15, 0, 0))
    m.blob("body", "body", 0, 0, 0, 4.5, 4.5, 8, 1.0)
    m.blob("body", "breast", 0, 0.4, -3.6, 4.0, 4.0, 2.5, 1.0)
    m.box("body", "back", -1.75, -2.75, -2.5, 3.5, 0.5, 6.5)
    m.box("body", "vent", -1.25, 1.5, 3.5, 2.5, 1, 1.5)
    # the head part is the neck too (PheasantModel turns it as one): the neck, the green head, red wattles, ear tufts
    m.part("head", "body", (0, -1.5, -3.5), (0.15, 0, 0))
    m.box("head", "neck", -1.25, -2.0, -0.75, 2.5, 2.5, 2)          # the neck curving forward up to the head
    m.box("head", "neck", -1.0, -3.25, -1.25, 2, 2, 2)
    m.box("head", "neck", -0.875, -4.25, -2.0, 1.75, 1.75, 1.75)
    m.blob("head", "head", 0, -4.75, -2.25, 2.5, 2.5, 3, 0.5)
    for s in (-1, 1):
        m.box("head", "wattle", (1.0 if s > 0 else -1.25), -4.25, -3.25, 0.25, 1.0, 0.75)   # the lappet under the eye
        m.box("head", "tuft", (0.25 if s > 0 else -0.75), -6.5, -1.5, 0.5, 0.75, 1.0)
    m.box("head", "bill", -0.5, -5.0, -4.5, 1, 1, 1)
    m.box("head", "bill", -0.375, -4.75, -5.0, 0.75, 0.75, 0.5)
    for s, n in ((1, "left_wing"), (-1, "right_wing")):
        m.part(n, "body", (s * 2.0, -1.75, -3))
        x0 = 0 if s > 0 else -0.5
        m.box(n, "wing", x0, 0, 0, 0.5, 4.5, 7)
        m.box(n, "covert", -0.5, 0, 0, 1, 2.5, 4)
    m.part("tail", "body", (0, -1, 4), (0.45, 0, 0))
    m.box("tail", "tail", -1.5, -0.25, 0, 3, 0.5, 6)
    m.box("tail", "tail", -1.0, -0.25, 6, 2, 0.5, 5)
    m.box("tail", "tail", -0.5, -0.25, 11, 1, 0.5, 3)
    for s, n in ((1, "left_leg"), (-1, "right_leg")):
        m.part(n, "root", (s * 1.0, 20, 0.5))
        m.box(n, "thigh", -0.75, -0.75, -0.75, 1.5, 1.5, 1.5)
        m.box(n, "leg", -0.25, 0.5, -0.25, 0.5, 3.0, 0.5)
        m.box(n, "toes", -0.75, 3.5, -2.0, 1.5, 0.5, 2)
        m.box(n, "toes", -0.25, 3.5, 0, 0.5, 0.5, 0.75)
    return m


PHEASANT = dict(copper=hexc("#c0642a"), copper_d=hexc("#6e2a12"), gold=hexc("#e0a050"), green=hexc("#1e4a3a"), purple=hexc("#3a2a5a"),
                white=hexc("#ecebe4"), red=hexc("#c81e1e"), bill=hexc("#d8ccaa"), leg=hexc("#7a6e64"), rump=hexc("#7a8a6a"),
                bar=hexc("#2a1a10"), tail=hexc("#b48a5a"), wing=hexc("#a88a64"), wing_d=hexc("#5a4630"))


def pheasant_colour(P):
    def col(mat, t):
        if mat in ("body", "breast", "back", "vent"):
            # copper flanks and breast, each feather tipped with a dark crescent; the grey-green rump
            if mat == "body" and t.face == "top" and t.z > 1.0:
                return scallop(t, P["rump"], lerp(P["rump"], P["white"], 0.4), cw=1.0, ch=0.75)
            base = lerp(P["copper"], P["gold"], 0.25 * h(t.gx // 2, t.gy // 2, 4))
            c = scallop(t, base, P["copper_d"], P["copper_d"], cw=1.25, ch=1.0, k=0.15)
            if mat == "vent":
                return lerp(P["copper_d"], P["bar"], 0.4)
            if t.face == "bottom":
                return lerp(P["copper_d"], P["bar"], 0.3)
            return c
        if mat == "neck":
            # the white collar ring at the base of the green neck
            if t.Y > 15.4 and t.Y < 16.3:
                return lerp(P["white"], (200, 200, 196), 0.2 * t.n)
            if t.Y >= 16.3:
                return scallop(t, P["copper"], P["copper_d"], cw=1.0, ch=0.75)
            return lerp(P["green"], P["purple"], 0.35 * h(t.gx, t.gy, t.salt))
        if mat == "head":
            if (e := eye(t, 0.38, 0.25, 0.42, iris=(200, 150, 40), ring=(40, 20, 10))):
                return e
            if eye(t, 0.95, 0.25, 0.45):
                return lerp(P["red"], (150, 16, 16), 0.3 * t.n)  # the bare red face round the eye
            return lerp(P["green"], P["purple"], 0.3 * h(t.gx, t.gy, t.salt) + 0.2 * (t.face == "top"))
        if mat == "wattle":
            return lerp(P["red"], (120, 10, 10), 0.3 * t.n)
        if mat == "tuft":
            return lerp(P["green"], P["purple"], 0.5)
        if mat == "bill":
            return lerp(P["bill"], (130, 120, 96), 0.3 * (t.face != "top"))
        if mat in ("wing", "covert"):
            if t.face == "right" and mat == "wing" and t.x < 0.01 or t.face == "left" and t.x < 0.01 and mat == "wing" and t.box[0] < 0:
                pass
            c = flight_feathers(t, P["wing"], P["wing_d"], lerp(P["wing"], P["white"], 0.5), lane=1.0) if mat == "wing" else \
                scallop(t, lerp(P["wing"], P["copper"], 0.3), P["wing_d"], cw=1.0, ch=0.75)
            if mat == "wing" and t.side and int(t.y * 2) % 2 == 0 and t.v > 0.3:
                c = lerp(c, P["white"], 0.35)  # barred flight feathers
            return c
        if mat == "tail":
            # the long pointed tail: buff with dark cross bars, a copper wash at the root
            if t.face in ("top", "bottom") and abs(t.u - 0.5) > 0.5 - 0.5 * max(0, (t.z - 11) / 3.0):
                return None
            c = lerp(P["tail"], P["copper"], 0.4 * max(0.0, 1 - t.z / 6))
            if (t.z * 1.0 + 0.3 * math.sin(t.x * 3)) % 1.5 < 0.4:
                c = lerp(P["bar"], c, 0.2)
            if abs(t.x) < 0.15 and t.face in ("top",):
                c = lerp(c, P["bar"], 0.3)
            return c
        if mat == "thigh":
            return lerp(P["copper_d"], P["bar"], 0.3)
        if mat in ("leg", "toes"):
            return lerp(P["leg"], P["bar"], 0.25 * t.n)
        return P["copper"]
    return col


def pheasant_chick():
    m = M("Pheasant chick", 4, (32, 32))
    m.part("body", "root", (0, 21, 0))
    m.blob("body", "body", 0, -0.25, 0.25, 3.75, 3.25, 4.0, 1.0)
    m.blob("body", "breast", 0, 0.25, -1.4, 3.0, 2.5, 1.5, 0.75)
    m.part("head", "body", (0, -0.5, -1.5))
    m.blob("head", "head", 0, -1.25, -0.5, 3.0, 2.75, 3.0, 0.75)
    m.box("head", "bill", -0.375, -1.25, -2.5, 0.75, 0.5, 0.75)
    m.box("head", "bill", -0.25, -1.0, -2.75, 0.5, 0.5, 0.25)
    for s, n in ((1, "left_wing"), (-1, "right_wing")):
        m.part(n, "body", (s * 1.75, -1.0, -1.0))
        m.box(n, "wing", 0 if s > 0 else -0.5, 0, 0.25, 0.5, 1.5, 1.75)
    m.part("tail", "body", (0, -1.0, 2.0), (0.6, 0, 0))
    m.box("tail", "tail", -0.5, -0.5, 0, 1, 0.75, 0.75)
    for s, n in ((1, "left_leg"), (-1, "right_leg")):
        m.part(n, "root", (s * 0.9, 22, 0.5))
        m.box(n, "leg", -0.25, 0, -0.25, 0.5, 1.75, 0.5)
        m.box(n, "toes", -0.75, 1.75, -1.5, 1.5, 0.25, 1.75)
        m.box(n, "toes", -0.25, 1.75, 0.25, 0.5, 0.25, 0.5)
    return m


CHICK = dict(down=hexc("#d8bc86"), light=hexc("#f0e2bc"), dark=hexc("#5a3c22"), rufous=hexc("#a8703a"), bill=hexc("#d8a878"),
             leg=hexc("#e0b088"))


def chick_colour(P):
    def col(mat, t):
        if mat in ("body", "breast", "tail"):
            c = lerp(P["down"], P["light"], 0.35 * h(t.gx, t.gy, t.salt))
            # the dark stripes down the back between rufous bands
            if t.face == "top" or (t.vert and t.Y < 20.2 and t.face != "front"):
                ax = abs(t.X)
                if ax < 0.3 or abs(ax - 1.2) < 0.2:
                    return lerp(P["dark"], P["rufous"], 0.3 * t.n)
                return lerp(c, P["rufous"], 0.6)
            if t.face == "bottom" or mat == "breast":
                return lerp(P["light"], P["down"], 0.3 * t.n)
            return c
        if mat == "head":
            if (e := eye(t, 0.4, 0.3, 0.5, iris=(16, 12, 10))):
                return e
            c = lerp(P["down"], P["light"], 0.3 * h(t.gx, t.gy, t.salt))
            if t.face == "top":
                return lerp(P["rufous"], P["dark"], 0.3 + 0.3 * (abs(t.x) < 0.3))  # the rufous cap
            if t.side and abs(t.gv - 0.62) < 0.07 and t.gw > 0.4:
                return P["dark"]  # the stripe back from the eye
            return c
        if mat == "bill":
            return P["bill"]
        if mat == "wing":
            return lerp(P["rufous"], P["dark"], 0.3 * t.n + 0.3 * (int(t.z * 4) % 3 == 0))
        if mat in ("leg", "toes"):
            return P["leg"]
        return P["down"]
    return col


# ================================================================== BIRDS on BirdModel's part tree
def flier(name, px, tex, body, wing, tip, tail, legs, tilt=0.0, r=0.5, lead=0.5, wing_z=0.15, tip_z=0.3, tail_rot=-0.12,
          tail_y=0.6, wing_y=0.5, feet="perch"):
    """body (w, h, l); wing (span, chord); tip (span, chord); tail (w, l); legs (length, x, z, width, toe length).
    The neck and head go on with neck()/head parts in the species function. The body pivot is set afterwards with
    seat() so the bird stands on its feet."""
    m = M(name, px, tex)
    w, hh, l = body
    q = 1.0 / px
    m.bw, m.bh, m.bl, m.tilt = w, hh, l, tilt
    m.part("body", "root", (0, 18, 0), (-tilt, 0, 0))
    # a round chest, the body tapering behind it to the tail, a fuller breast, the mantle, the belly, the rump
    r2 = max(r, sn(min(w, hh) * 0.3, px))
    if tilt >= 0.4:
        # sitting upright, the steps between chest and tail blobs would face the viewer: one rounder body
        m.blob("body", "body", 0, 0, 0, w, hh, l, sn(min(w, hh) * 0.15, px))
        m.blob("body", "body", 0, 0, 0, w - 1, hh - 1, l - 1, sn(min(w, hh) * 0.3, px))  # a second, inner bevel rounds it off
    else:
        m.blob("body", "body", 0, 0, -l * 0.18, w, hh, sn(l * 0.64, px), r2)
        m.blob("body", "body", 0, -hh * 0.06, l * 0.22, sn(w * 0.86, px), sn(hh * 0.82, px), sn(l * 0.56, px), r2)
        m.blob("body", "breast", 0, hh * 0.1, -l / 2 + l * 0.12, w - 2 * q, sn(hh * 0.75, px), sn(l * 0.3, px), r2)
    if tilt < 0.4:
        m.box("body", "back", -sn(w * 0.8, px) / 2, -hh / 2 - q, -l / 2 + l * 0.18, sn(w * 0.8, px), 2 * q, sn(l * 0.55, px))
        m.box("body", "belly", -(w / 2 - 2 * q), hh / 2 - q, -l / 2 + l * 0.25, w - 4 * q, 2 * q, sn(l * 0.5, px))
    m.blob("body", "rump", 0, -hh * 0.12, l / 2 + 0.25, sn(w * 0.7, px), sn(hh * 0.65, px), 1.0, r * 0.5)
    if tilt < 0.4:
        m.box("body", "vent", -(w * 0.3), hh * 0.15, l / 2 - 0.5, sn(w * 0.6, px), sn(hh * 0.3, px), 1.25)
    span, chord = wing
    tspan, tchord = tip
    for s, n in ((1, "left"), (-1, "right")):
        m.part(n + "_wing", "body", (s * (w / 2 - q), -hh / 2 + wing_y, -l / 2 + l * wing_z))
        m.box(n + "_wing", "wing", 0 if s > 0 else -0.5, 0, 0, 0.5, span, chord)
        if lead:
            m.box(n + "_wing", "covert", -lead if s > 0 else -0.5, 0, 0, lead + 0.5, sn(span * 0.55, px), sn(chord * 0.45, px))
        m.part(n + "_wing_tip", n + "_wing", (s * 0.0, span, chord * tip_z), (1.5708, 0, 0))
        m.box(n + "_wing_tip", "primaries", 0 if s > 0 else -0.5, 0, 0, 0.5, tspan, tchord)
    tw, tl = tail
    m.part("tail", "body", (0, -hh / 2 + tail_y, l / 2 + 0.5), (tail_rot, 0, 0))
    m.box("tail", "tail", -tw / 2, 0, 0, tw, 0.5, tl)
    ll, lx, lz, lw, toe = legs
    m.leg_y = 24 - ll
    for s, n in ((1, "left_leg"), (-1, "right_leg")):
        m.part(n, "root", (s * lx, 24 - ll, lz))
        tw_ = sn(lw * 1.5 + 0.5, px)
        m.box(n, "thigh", -tw_ / 2, -1.0, -tw_ / 2, tw_, 1.25, tw_)
        m.box(n, "leg", -lw / 2, 0, -lw / 2, lw, ll - 0.5, lw)
        tt = 0.5 if px <= 2 else 0.25
        if feet == "web":
            m.box(n, "web", -toe * 0.45, ll - tt, -toe, sn(toe * 0.9, px), tt, toe)
            m.box(n, "toes", -0.25, ll - tt, 0, 0.5, tt, sn(toe * 0.3, px))
        elif feet == "feathered":
            m.box(n, "toes", -lw / 2 - 0.25, ll - 1.0, -toe, lw + 0.5, 1.0, toe + 0.5)
            m.box(n, "claws", -lw / 2 - 0.25, ll - tt, -toe - tt, lw + 0.5, tt, tt)
        else:
            tw2 = tt if px >= 4 else 0.5
            for dx in (-1, 0, 1):
                m.box(n, "toes", dx * (lw / 2 + tw2 * 0.5) - tw2 / 2, ll - tt, -toe, tw2, tt, toe)
            m.box(n, "toes", -tw2 / 2, ll - tt, 0, tw2, tt, sn(toe * 0.5, px))
            if feet == "raptor":
                m.box(n, "claws", -lw / 2 - tw2, ll - tt, -toe - tt, lw + 2 * tw2, tt, tt)
    return m


def sn(v, px):
    return max(1.0 / px, round(v * px) / px)


def neck(m, w, hh, d, at_y=None, at_z=None, rx=0.0):
    """The neck part on the front top of the body (turned back by the body's tilt), its box rising from the pivot."""
    y = -m.bh / 2 + (m.bh * 0.3 if at_y is None else at_y)
    z = -m.bl / 2 + (d / 2 if at_z is None else at_z)
    m.part("neck", "body", (0, y, z), (m.tilt + rx, 0, 0))
    m.box("neck", "neck", -w / 2, -hh, -d / 2, w, hh, d)
    return hh


def seat(m, gap=0.5):
    """Lower or raise the body so its lowest point (wings, tail and neck aside) sits gap above the leg pivots."""
    skip = {"left_wing", "right_wing", "left_wing_tip", "right_wing_tip", "tail", "neck", "head", "left_leg", "right_leg"}
    parts = {p for p in m.parts if p not in skip and m.parts[p][0] not in skip}
    parts = {p for p in parts if p == "body" or m.parts[p][0] == "body"}
    low = m.lowest(parts)
    m.move("body", (m.leg_y + gap) - low)


def bird_colour(P, over=None, eye_r=0.35, eye_at=(0.3, 0.42), eye_mat="head", iris=(24, 18, 14), ring=None, pupil=None):
    """A bird's skin from its palette: back/upperparts, under, breast, head, wing (coverts and flight feathers, paler
    under), tail, bill, legs; over: {material: fn(t) -> colour, or False to fall through}."""
    over = over or {}

    def upper_under(t, up, under, cut=0.55):
        if t.face == "top":
            return up
        if t.face == "bottom":
            return under
        return up if t.gv < cut else under

    def col(mat, t):
        if mat in over:
            c = over[mat](t)
            if c is not False:
                return c
        if mat == eye_mat and (e := eye(t, eye_r, *eye_at, iris=iris, ring=ring, pupil=pupil)):
            return e
        if mat in ("body", "back", "rump"):
            c = upper_under(t, scallop(t, P["back"], P.get("edge", P["back"]), P.get("dark"), k=0.15), P["under"])
            return c
        if mat in ("breast", "belly", "vent"):
            return lerp(P.get("breast", P["under"]), P["under"], 0.3 * t.n) if mat == "breast" else P["under"]
        if mat in ("neck", "head", "crown", "cheek"):
            return lerp(P.get("head", P["back"]), P.get("dark", P["back"]), 0.15 * t.n)
        if mat == "covert":
            return scallop(t, P.get("covert", P["wing"]), P.get("wing_edge", P.get("edge", P["wing"])), P.get("dark"), cw=1.0, ch=0.75, k=0.2)
        if mat == "wing":
            inner = (t.face == "right" and t.box[0] >= 0) or (t.face == "left" and t.box[0] < 0)
            if inner:
                return lerp(P.get("underwing", P["under"]), P["wing"], 0.25 * t.n)
            if t.gv < 0.4 and t.vert:
                return scallop(t, P.get("covert", P["wing"]), P.get("wing_edge", P.get("edge", P["wing"])), P.get("dark"), cw=1.0, ch=0.75, k=0.2)
            return flight_feathers(t, P["wing"], P.get("wing_dark", P["wing"]), P.get("wing_edge", P["wing"]), lane=0.75 if t.px >= 4 else 1.0)
        if mat == "primaries":
            inner = (t.face == "right" and t.box[0] >= 0) or (t.face == "left" and t.box[0] < 0)
            base = P.get("prim", P.get("wing_dark", P["wing"]))
            if inner:
                return lerp(P.get("underwing", base), base, 0.4)
            return flight_feathers(t, base, P.get("prim_dark", base), P.get("wing_edge", base), lane=0.75 if t.px >= 4 else 1.0, tipdark=0.2)
        if mat == "tail":
            return flight_feathers(t, P.get("tail", P["back"]), P.get("tail_dark", P.get("dark", P["back"])), P.get("edge", P["back"]), along="z", lane=0.75 if t.px >= 4 else 1.0)
        if mat == "bill":
            return lerp(P["bill"], P.get("bill_dark", P["bill"]), 0.2 * t.n)
        if mat == "bill_lower":
            return lerp(P["bill"], P.get("bill_tip", P["bill"]), 0.3)
        if mat == "bill_tip":
            return P.get("bill_tip", P["bill"])
        if mat == "thigh":
            return P.get("thigh", P["under"])
        if mat in ("leg", "toes", "web"):
            if mat == "web" and t.face in ("top", "bottom"):
                # the web: a fan narrowing back to the ankle
                if abs(t.u - 0.5) > 0.5 * (1 - t.w) + 0.08:
                    return None
            return lerp(P["leg"], P.get("leg_dark", P["leg"]), 0.25 * t.n)
        if mat == "claws":
            return P.get("claw", (30, 26, 24))
        return P["back"]
    return col


def outer(t):
    """On the wing's upper side (the outer face at rest, on top in flight)."""
    return not ((t.face == "right" and t.box[0] >= 0) or (t.face == "left" and t.box[0] < 0))


def finger(t, frac=0.62, lane=1.0, slot=0.5):
    """Primaries spread like fingers: slots cut into the outer part of the feather box (both broad faces alike)."""
    return t.gv >= frac and (t.z / lane) % 1.0 < slot / lane


def wedge(t, cut):
    """The tail's corners cut away towards its end (cut: how much of each side goes at the very end)."""
    return t.face in ("top", "bottom", "back") and abs(t.gu - 0.5) > 0.5 - cut * max(0.0, (t.gw - 0.55) / 0.45)


def ink(t, P):
    return lerp(P["black"], P["sheen"], 0.4 * h(t.gx, t.gy, t.salt) + 0.25 * (t.face == "top"))


# ------------------------------------------------------------------ hooded crow
def crow():
    m = flier("Hooded crow", 4, (64, 64), body=(4, 4, 7), wing=(3.25, 6), tip=(4.5, 2.5), tail=(2.75, 4.5),
              legs=(3.5, 1.0, 0.5, 0.5, 2.0), tilt=0.3, lead=0.25)
    neck(m, 2.5, 1.75, 2.5, at_z=1.0)
    m.part("head", "neck", (0, -1.5, 0))
    m.blob("head", "head", 0, -1.25, -0.4, 3, 2.75, 3.25, 0.5)
    m.box("head", "throat", -1.0, -0.25, -1.25, 2, 1.0, 2)                   # shaggy throat feathers
    m.box("head", "bill", -0.5, -1.75, -3.5, 1.0, 1.0, 1.5)
    m.box("head", "bill", -0.375, -1.5, -4.25, 0.75, 0.75, 0.75)
    m.box("head", "bill_lower", -0.375, -0.75, -4.0, 0.75, 0.25, 2.0)
    seat(m)
    return m


CROW = dict(back=hexc("#8e8e92"), edge=hexc("#a4a4a8"), dark=hexc("#5e5e64"), under=hexc("#9a9a9c"), breast=hexc("#9a9a9e"),
            head=hexc("#141418"), black=hexc("#141418"), sheen=hexc("#2c3446"), wing=hexc("#18181e"), wing_dark=hexc("#0e0e12"),
            wing_edge=hexc("#2e3242"), covert=hexc("#1c1c24"), underwing=hexc("#26262c"), prim=hexc("#121216"), tail=hexc("#16161c"),
            tail_dark=hexc("#0c0c10"), bill=hexc("#1a1a1c"), leg=hexc("#1c1c1e"), thigh=hexc("#1a1a1e"))


def crow_colour(P):
    return bird_colour(P, {"neck": lambda t: ink(t, P), "throat": lambda t: ink(t, P),
                           "head": lambda t: False if eye(t, 0.42, 0.3, 0.42) else ink(t, P),
                           "bill_lower": lambda t: P["bill"]},
                       eye_r=0.42, eye_at=(0.3, 0.42), iris=(26, 18, 14))


# ------------------------------------------------------------------ greylag goose
def goose():
    m = flier("Greylag goose", 4, (64, 64), body=(5, 5, 9.5), wing=(4, 8), tip=(5, 3), tail=(3, 3),
              legs=(3.5, 1.5, 0.75, 1.0, 2.5), tilt=0.12, feet="web")
    neck(m, 2, 6.5, 2, at_z=1.25)
    m.box("neck", "neck", -1.125, -2.5, -1.125, 2.25, 2.5, 2.25)          # thicker at the base
    m.part("head", "neck", (0, -6.25, 0))
    m.blob("head", "head", 0, -1.25, -0.5, 2.5, 2.5, 3.5, 0.5)
    m.box("head", "bill", -0.75, -1.5, -3.5, 1.5, 1.5, 1.5)
    m.box("head", "bill", -0.625, -1.25, -4.75, 1.25, 1.0, 1.25)
    m.box("head", "bill_tip", -0.5, -1.25, -5.0, 1.0, 0.75, 0.5)
    m.box("head", "bill_lower", -0.5, -0.25, -4.5, 1.0, 0.5, 2.0)
    seat(m)
    return m


GOOSE = dict(back=hexc("#7a7468"), edge=hexc("#c8c2b4"), dark=hexc("#4e4a42"), under=hexc("#bcb8ae"), breast=hexc("#a8a49a"),
             head=hexc("#8a8478"), wing=hexc("#6e6a62"), wing_dark=hexc("#3e3c38"), wing_edge=hexc("#c4c0b6"), covert=hexc("#9a988e"),
             underwing=hexc("#a8a8a2"), prim=hexc("#4e4c48"), tail=hexc("#5e5a52"), bill=hexc("#e8883a"), bill_tip=hexc("#ece4d0"),
             leg=hexc("#e0a090"), thigh=hexc("#8a857a"), white=hexc("#f0eee8"))


def goose_colour(P):
    def neck_c(t):
        # the neck feathers lie in furrows: dark grooves running down it
        c = lerp(P["head"], P["dark"], 0.15 * t.n)
        return lerp(c, P["dark"], 0.35) if t.vert and int((t.x + t.z) * 2 + 20) % 3 == 0 else c

    def body(t):
        if t.face == "bottom" and t.gw > 0.6:
            return P["white"]
        if t.vert and t.gv > 0.55 and t.face != "front":
            # the flanks: rows of dark feathers with pale fringes
            return scallop(t, lerp(P["back"], P["under"], 0.4), P["edge"], P["dark"], cw=1.5, ch=1.0, k=0.25)
        return False

    def tail(t):
        return P["white"] if t.gw > 0.8 or abs(t.gu - 0.5) > 0.38 else False
    return bird_colour(P, {"neck": neck_c, "vent": lambda t: lerp(P["white"], P["under"], 0.2 * t.n), "body": body, "tail": tail,
                           "rump": lambda t: P["white"] if t.face in ("back", "top") else False,
                           "bill_lower": lambda t: lerp(P["bill"], (180, 90, 40), 0.3)},
                       eye_r=0.4, eye_at=(0.32, 0.42), iris=(40, 26, 16), ring=(230, 140, 80))


# ------------------------------------------------------------------ rock dove
def rock_dove():
    m = flier("Rock dove", 4, (64, 64), body=(3.5, 3.5, 6), wing=(2.75, 5), tip=(3.75, 2), tail=(2.5, 3.5),
              legs=(2.25, 0.75, 0.5, 0.5, 1.25), tilt=0.28, lead=0.25)
    neck(m, 2.25, 1.75, 2.25, at_z=0.9)
    m.box("neck", "neck", -1.375, -1.0, -1.125, 2.75, 1.25, 2.0)    # the full, glossy neck
    m.part("head", "neck", (0, -1.5, 0))
    m.blob("head", "head", 0, -1.0, -0.3, 2.25, 2.25, 2.5, 0.5)
    m.box("head", "cere", -0.375, -1.25, -1.75, 0.75, 0.5, 0.5)
    m.box("head", "bill", -0.25, -1.0, -2.5, 0.5, 0.5, 0.75)
    seat(m)
    return m


DOVE = dict(back=hexc("#8e96a2"), edge=hexc("#a6aebb"), dark=hexc("#6a707c"), under=hexc("#8a92a0"), breast=hexc("#8e8a9c"),
            head=hexc("#6e7486"), wing=hexc("#a2aab6"), wing_dark=hexc("#5a5e68"), wing_edge=hexc("#c0c6d0"), covert=hexc("#aab2be"),
            underwing=hexc("#e0e2e8"), prim=hexc("#52565e"), tail=hexc("#7e8694"), tail_dark=hexc("#22242a"), bill=hexc("#2a2a2e"),
            leg=hexc("#d07878"), thigh=hexc("#7e8494"), green=hexc("#4a8a6a"), purple=hexc("#7a4a8a"), cere=hexc("#eae6e0"),
            bar=hexc("#1e1e24"), white=hexc("#e8eaee"))


def dove_colour(P):
    def neck_c(t):
        # the iridescent neck: green and purple patches shifting
        return lerp(lerp(P["green"], P["purple"], h(t.gx // 2, t.gy // 2, t.salt)), P["head"], 0.3 * t.n)

    def wing(t):
        if outer(t) and t.vert and 0.3 < t.gw < 0.95 and (abs(t.gv - 0.55) < 0.07 or abs(t.gv - 0.78) < 0.07):
            return P["bar"]  # the two black bars across the folded wing
        return False
    return bird_colour(P, {"neck": neck_c, "wing": wing, "tail": lambda t: lerp(P["tail_dark"], P["bar"], 0.3) if t.gw > 0.78 else False,
                           "cere": lambda t: P["cere"], "rump": lambda t: P["white"] if t.face == "top" else False,
                           "breast": lambda t: lerp(neck_c(t), P["breast"], 0.6) if t.gv < 0.4 else False},
                       eye_r=0.4, eye_at=(0.32, 0.45), iris=(220, 110, 40), ring=(150, 150, 150), pupil=(20, 16, 14))


# ------------------------------------------------------------------ willow ptarmigan
def ptarmigan():
    m = flier("Willow ptarmigan", 4, (64, 64), body=(4, 4, 6), wing=(3, 5), tip=(3.5, 2), tail=(3, 2.5),
              legs=(2.25, 1.0, 0.5, 0.75, 1.25), tilt=0.15, feet="feathered", lead=0.25)
    neck(m, 2.5, 1.25, 2.5, at_z=1.0)
    m.part("head", "neck", (0, -1.0, 0))
    m.blob("head", "head", 0, -1.2, -0.3, 2.5, 2.5, 2.75, 0.5)
    m.box("head", "comb", -1.375, -2.25, -1.25, 2.75, 0.5, 1.0)    # the red combs over the eyes
    m.box("head", "bill", -0.375, -1.25, -2.25, 0.75, 0.75, 0.75)
    m.box("head", "bill", -0.25, -1.0, -2.5, 0.5, 0.5, 0.25)
    seat(m)
    return m


PTARMIGAN = dict(back=hexc("#8a5a34"), edge=hexc("#c49060"), dark=hexc("#3e2a1a"), under=hexc("#f2f0ea"), breast=hexc("#9a6238"),
                 head=hexc("#9a6238"), wing=hexc("#f2f0ea"), wing_dark=hexc("#d8d8d4"), wing_edge=hexc("#ffffff"), covert=hexc("#e8e6e0"),
                 underwing=hexc("#ffffff"), prim=hexc("#eceae4"), prim_dark=hexc("#c8c8c4"), tail=hexc("#1e1a18"),
                 bill=hexc("#1e1a18"), leg=hexc("#f0eee8"), thigh=hexc("#f0eee8"), comb=hexc("#d02a20"), claw=hexc("#3a3430"))
PTARMIGAN_W = dict(PTARMIGAN, back=hexc("#f4f4f0"), edge=hexc("#ffffff"), dark=hexc("#d4d6da"), breast=hexc("#f4f4f0"),
                   head=hexc("#f4f4f0"))


def ptarmigan_colour(P, winter):
    def mott(t, base):
        if winter:
            return lerp(base, P["dark"], 0.18 * h(t.gx, t.gy, t.salt))
        c = lerp(base, P["edge"], 0.35 * h(t.gx, t.gy, t.salt))
        return lerp(c, P["dark"], 0.65) if h(t.gx // 2, t.gy, t.salt + 3) < 0.3 else c

    def body(t):
        if t.face == "top" or (t.vert and t.gv < 0.45 and t.face != "front"):
            return mott(t, P["back"])
        return P["under"]

    def head(t):
        if eye(t, 0.4, 0.3, 0.45):
            return False
        if winter and t.side and abs(t.gv - 0.45) < 0.09 and t.gw < 0.3:
            return P["tail"]  # the black lores of the winter bird
        return mott(t, P["head"])
    return bird_colour(P, {"body": body, "back": lambda t: mott(t, P["back"]), "rump": lambda t: mott(t, P["back"]),
                           "breast": lambda t: mott(t, P["breast"]) if t.gv < 0.6 else P["under"],
                           "neck": lambda t: mott(t, P["head"]), "comb": lambda t: P["comb"], "head": head,
                           "tail": lambda t: lerp(P["tail"], (60, 50, 40), 0.3 * t.n) if t.gw < 0.85 else P["under"],
                           "toes": lambda t: lerp(P["leg"], (210, 205, 196), 0.3 * t.n)},
                       eye_r=0.4, eye_at=(0.3, 0.45), iris=(20, 16, 14))


# ------------------------------------------------------------------ herring gull
def seagull():
    m = flier("Herring gull", 4, (64, 64), body=(4, 4, 7.5), wing=(3.5, 6), tip=(6, 2.5), tail=(3, 3),
              legs=(3.0, 1.0, 0.5, 0.5, 2.0), tilt=0.22, feet="web", lead=0.25, tip_z=0.25)
    neck(m, 2.5, 1.75, 2.5, at_z=1.1)
    m.part("head", "neck", (0, -1.5, 0))
    m.blob("head", "head", 0, -1.3, -0.4, 2.75, 2.75, 3.25, 0.5)
    m.box("head", "bill", -0.5, -1.5, -4.25, 1.0, 0.75, 2.5)
    m.box("head", "bill_lower", -0.375, -0.625, -4.0, 0.75, 0.5, 2.25)
    m.box("head", "bill_tip", -0.375, -0.75, -4.5, 0.75, 0.5, 0.5)       # the hooked tip
    seat(m)
    return m


GULL = dict(back=hexc("#aab4c0"), edge=hexc("#c2cad4"), dark=hexc("#8a94a0"), under=hexc("#f4f4f2"), breast=hexc("#f6f6f4"),
            head=hexc("#f6f6f4"), wing=hexc("#aab4c0"), wing_dark=hexc("#96a0ac"), wing_edge=hexc("#f0f2f4"), covert=hexc("#b4bec8"),
            underwing=hexc("#f0f2f4"), prim=hexc("#1a1a1e"), tail=hexc("#f4f4f2"), bill=hexc("#ecc444"), bill_tip=hexc("#e0b838"), red=hexc("#d8402a"),
            leg=hexc("#e8a8a0"), thigh=hexc("#f4f4f2"), black=hexc("#16161a"), white=hexc("#fafafa"))


def gull_colour(P):
    def body(t):
        if t.face == "top" or (t.vert and t.gv < 0.35 and t.face != "front"):
            return lerp(P["back"], P["edge"], 0.3 * t.n)
        return lerp(P["under"], P["back"], 0.08 * t.n)

    def prim(t):
        # black wingtips with white "mirrors", the grey running out into them
        if t.gv < 0.3:
            return lerp(P["wing"], P["black"], t.gv / 0.3 * 0.7)
        if t.gv > 0.86 and 0.25 < t.gw < 0.8:
            return P["white"]
        return lerp(P["black"], (50, 50, 56), 0.3 * t.n)
    return bird_colour(P, {"body": body, "rump": lambda t: P["under"], "back": lambda t: P["back"], "primaries": prim,
                           "wing": lambda t: P["white"] if outer(t) and t.vert and t.gw > 0.86 else False,
                           "tail": lambda t: lerp(P["tail"], P["edge"], 0.15 * t.n),
                           "bill_lower": lambda t: P["red"] if -4.0 < t.z < -3.25 and t.face != "top" else P["bill"]},
                       eye_r=0.4, eye_at=(0.3, 0.42), iris=(232, 220, 150), ring=(220, 120, 40), pupil=(20, 18, 16))


# ================================================================== FOREST BIRDS (ForestBirdModel: the tilt baked into the body)
def capercaillie():
    m = flier("Capercaillie", 4, (64, 64), body=(6, 6, 10), wing=(5, 8.5), tip=(5, 3), tail=(7, 8),
              legs=(3.0, 1.75, 0.5, 1.5, 2.5), tilt=0.0, feet="feathered", tail_rot=-0.3, tail_y=1.0)
    neck(m, 3.5, 3.0, 3.5, at_z=1.5)
    m.box("neck", "beard", -1.25, -1.5, -2.25, 2.5, 2.5, 1.0)            # the shaggy beard under the bill
    m.part("head", "neck", (0, -2.5, 0))
    m.blob("head", "head", 0, -1.75, -0.6, 4, 4, 4.5, 0.75)
    m.box("head", "comb", -2.25, -3.25, -2.0, 4.5, 1.0, 1.5)                # the red brows over the eyes
    m.box("head", "bill", -0.875, -2.0, -4.25, 1.75, 1.75, 1.5)
    m.box("head", "bill_tip", -0.625, -1.5, -5.0, 1.25, 1.5, 0.75)
    seat(m)
    return m


CAPER = dict(back=hexc("#3a3c40"), edge=hexc("#5a5c62"), dark=hexc("#1c1c20"), under=hexc("#1e1e22"), breast=hexc("#1a2a2c"),
             head=hexc("#2e3034"), wing=hexc("#5a4632"), wing_dark=hexc("#3a2c1e"), wing_edge=hexc("#7a644a"), covert=hexc("#64503a"),
             underwing=hexc("#d8d4cc"), prim=hexc("#3e3226"), tail=hexc("#1c1c20"), bill=hexc("#e2dcc4"), bill_tip=hexc("#c8c0a4"),
             leg=hexc("#3e3a36"), thigh=hexc("#2a2a2e"), comb=hexc("#d42a1e"), white=hexc("#ecebe4"), green=hexc("#24463e"))
CAPER_HEN = dict(back=hexc("#8a6440"), edge=hexc("#d4b080"), dark=hexc("#2e2014"), under=hexc("#d4b88e"), breast=hexc("#c8844a"),
                 head=hexc("#9a6e44"), wing=hexc("#8a6440"), wing_dark=hexc("#4a3420"), wing_edge=hexc("#d4b080"), covert=hexc("#8e6a44"),
                 underwing=hexc("#e2d6c0"), prim=hexc("#6a4c30"), tail=hexc("#9a6a3a"), bill=hexc("#4a4038"), bill_tip=hexc("#3a3028"),
                 leg=hexc("#cabca6"), thigh=hexc("#d4c4a6"), comb=hexc("#b84a30"), white=hexc("#f0ece2"), green=hexc("#c8844a"))


def caper_colour(P, hen):
    def vermic(t, base):
        # fine wavy grey vermiculation on the cock; bold black-and-buff barring on the hen
        if hen:
            c = lerp(base, P["edge"], 0.3 * h(t.gx, t.gy, t.salt))
            return lerp(c, P["dark"], 0.7) if (t.Y * 2 + 0.6 * math.sin(t.X * 2 + t.Z)) % 1.5 < 0.45 else c
        c = lerp(base, P["edge"], 0.45 * h(t.gx, t.gy, t.salt))
        return lerp(c, P["dark"], 0.5) if (t.gy + int(3 * h(t.gx // 2, 1, t.salt))) % 3 == 0 else c

    def body(t):
        if t.face == "bottom" or (t.vert and t.gv > 0.6 and t.face != "front"):
            if hen:
                return scallop(t, P["under"], P["white"], cw=1.0, ch=0.75)
            if h(t.gx // 2, t.gy // 2, 7) < 0.12:
                return P["white"]  # white spots on the black belly
            return lerp(P["under"], P["dark"], 0.3 * t.n)
        return vermic(t, P["back"])

    def breast(t):
        if hen:
            return lerp(P["breast"], P["edge"], 0.3 * h(t.gx, t.gy, t.salt)) if t.gv < 0.6 else body(t)
        return lerp(P["breast"], P["green"], 0.6 * h(t.gx // 2, t.gy, t.salt))  # the glossy green breast shield

    def wing(t):
        if outer(t) and t.vert and t.gv < 0.15 and t.gw < 0.25:
            return P["white"]  # the white shoulder spot
        return False if hen else (vermic(t, P["wing"]) if outer(t) and t.gv < 0.5 else False)

    def tail(t):
        if t.face in ("top", "bottom") and abs(t.gu - 0.5) > 0.5 - 0.15 * max(0, (t.gw - 0.75) / 0.25) - 0.12 * (t.gw > 0.92):
            return None  # the rounded fan
        c = lerp(P["tail"], P["dark"], 0.3 * t.n)
        if hen:
            return lerp(c, P["dark"], 0.7) if int(t.z * 2) % 3 == 0 else lerp(P["tail"], P["edge"], 0.3 * t.n)
        if t.face == "bottom" and h(t.gx // 2, t.gy // 2, 4) < 0.25:
            return P["white"]
        return lerp(c, P["white"], 0.6) if t.face == "top" and t.gw > 0.55 and int(t.x * 2 + 20) % 3 == 0 and int(t.z * 2) % 4 == 0 else c
    return bird_colour(P, {"body": body, "back": lambda t: vermic(t, P["back"]), "rump": lambda t: vermic(t, P["back"]),
                           "breast": breast, "belly": body, "wing": wing, "tail": tail,
                           "head": lambda t: False if eye(t, 0.45, 0.32, 0.42) else vermic(t, P["head"]),
                           "neck": lambda t: vermic(t, P["head"]) if t.face != "front" or hen else lerp(P["breast"], P["green"], 0.5),
                           "beard": lambda t: None if (t.vert and t.v > 0.5 and ragged(t, 0.5)) else lerp(P["dark"], P["head"], 0.3 * t.n),
                           "comb": lambda t: P["comb"] if t.face != "bottom" else lerp(P["comb"], P["dark"], 0.4),
                           "vent": lambda t: P["white"] if not hen else P["under"],
                           "toes": lambda t: lerp(P["thigh"], P["dark"], 0.2 * t.n)},
                       eye_r=0.45, eye_at=(0.32, 0.42), iris=(30, 22, 18))


def great_bustard():
    m = flier("Great bustard", 4, (64, 64), body=(6, 6, 11), wing=(5, 9), tip=(6, 3.5), tail=(4.5, 5),
              legs=(6.0, 1.75, 0.75, 1.25, 2.0), tilt=0.0, tail_rot=-0.2)
    neck(m, 3, 6.5, 3, at_z=1.5, rx=-0.1)
    m.box("neck", "neck", -1.75, -2.0, -1.75, 3.5, 2.5, 3.5)        # the thick, puffed base of the neck
    m.part("head", "neck", (0, -6.25, 0))
    m.blob("head", "head", 0, -1.4, -0.6, 3, 3, 4, 0.5)
    m.box("head", "bill", -0.5, -1.5, -4.0, 1, 1, 1.75)
    m.box("head", "bill_lower", -0.375, -0.5, -3.75, 0.75, 0.5, 1.5)
    m.box("head", "whisker", -2.25, -0.5, -2.75, 4.5, 1.5, 1.5)        # the cock's white whiskers
    seat(m)
    return m


BUSTARD = dict(back=hexc("#c08a4a"), edge=hexc("#dcb070"), dark=hexc("#2a1c10"), under=hexc("#f0ece4"), breast=hexc("#b8723a"),
               head=hexc("#a8a8a6"), wing=hexc("#f0eee8"), wing_dark=hexc("#c8c4bc"), wing_edge=hexc("#ffffff"), covert=hexc("#e6e2da"),
               underwing=hexc("#f4f2ee"), prim=hexc("#3a3430"), tail=hexc("#c88a4a"), bill=hexc("#8a8478"), leg=hexc("#9a9078"),
               thigh=hexc("#ece8e0"), white=hexc("#f6f4f0"))


def bustard_colour(P):
    def back(t):
        c = lerp(P["back"], P["edge"], 0.3 * h(t.gx, t.gy, t.salt))
        return lerp(c, P["dark"], 0.75) if (t.Z * 1.5 + 0.5 * math.sin(t.X * 1.7)) % 1.3 < 0.35 else c  # black cross-bars

    def body(t):
        if t.face == "top" or (t.vert and t.gv < 0.45 and t.face != "front"):
            return back(t)
        return lerp(P["under"], P["edge"], 0.08 * t.n)

    def wing(t):
        if outer(t) and t.vert and t.gv < 0.3:
            return back(t)  # the barred scapulars over the white wing
        if outer(t) and t.gw > 0.82:
            return lerp(P["prim"], P["wing_dark"], 0.3)
        return False

    def neck_c(t):
        if t.gv > 0.75 and t.face != "back":
            return lerp(P["breast"], P["edge"], 0.3 * t.n)  # the chestnut breast band at the base of the neck
        return lerp(P["head"], (150, 150, 148), 0.3 * t.n)

    def tail(t):
        if t.gw > 0.85:
            return P["white"]
        c = lerp(P["tail"], P["edge"], 0.3 * t.n)
        return lerp(c, P["dark"], 0.7) if int(t.z * 2) % 4 == 0 else c

    def whisker(t):
        if t.face in ("top", "bottom", "front", "back") or abs(t.x) < 1.5 or (t.v > 0.3 and ragged(t, 0.3 + t.v * 0.5)):
            return None
        return lerp(P["white"], P["head"], 0.2 * t.n)
    return bird_colour(P, {"body": body, "back": back, "rump": back, "wing": wing, "neck": neck_c, "tail": tail, "whisker": whisker,
                           "breast": lambda t: lerp(P["breast"], P["edge"], 0.3 * t.n) if t.gv < 0.4 else P["under"],
                           "covert": back},
                       eye_r=0.45, eye_at=(0.3, 0.4), iris=(60, 40, 24), ring=(80, 76, 70))


def eagle_owl():
    m = flier("Eagle-owl", 4, (64, 64), body=(6.5, 6, 7.5), wing=(5, 7), tip=(6, 3), tail=(4.5, 4),
              legs=(2.5, 1.75, 0.5, 2.0, 2.0), tilt=0.9, feet="feathered", tail_rot=-0.25)
    neck(m, 4.5, 1.5, 4.5, at_z=2.25, at_y=1.5)
    m.part("head", "neck", (0, -1.25, 0))
    m.blob("head", "head", 0, -2.75, -0.25, 6.5, 5.5, 5.5, 1.0)
    m.box("head", "disc", -2.75, -4.75, -3.25, 5.5, 4.25, 0.5)            # the facial disc
    m.box("head", "bill", -0.5, -2.75, -3.75, 1, 1.5, 0.75)
    m.box("head", "bill_tip", -0.375, -1.5, -3.5, 0.75, 0.5, 0.5)
    for s in (-1, 1):
        n = "left_tuft" if s > 0 else "right_tuft"
        m.part(n, "head", (s * 2.0, -5.0, -1.0), (0.15, 0, s * 0.4))           # the ear tufts, splayed outward
        m.box(n, "tuft", -0.75, -1.5, -0.5, 1.5, 1.75, 1.0)
        m.box(n, "tuft", -0.5, -2.5, -0.375, 1.0, 1.0, 0.75)
        m.box(n, "tuft", -0.25, -3.0, -0.25, 0.5, 0.5, 0.5)
    m.boxes = [b for b in m.boxes if b[1] != "breast"]  # upright, the breast bulge would jut out under the face
    seat(m, 0.75)
    return m


OWL = dict(back=hexc("#8a6440"), edge=hexc("#c8a070"), dark=hexc("#2a1c12"), under=hexc("#d0a466"), breast=hexc("#d8ae70"),
           head=hexc("#9a7048"), wing=hexc("#8a6440"), wing_dark=hexc("#4a3420"), wing_edge=hexc("#c8a070"), covert=hexc("#7a5636"),
           underwing=hexc("#e0c49a"), prim=hexc("#7a5a3a"), tail=hexc("#7a5a3a"), bill=hexc("#2a2420"), leg=hexc("#d8b480"),
           thigh=hexc("#d8b480"), disc=hexc("#c49868"), rim=hexc("#2e2016"), claw=hexc("#1e1a18"), white=hexc("#f0e8d8"))


def owl_colour(P):
    def mott(t, base):
        c = lerp(base, P["edge"], 0.35 * h(t.gx, t.gy, t.salt))
        return lerp(c, P["dark"], 0.7) if h(t.gx // 2, t.gy // 2, t.salt + 3) < 0.22 else c

    def streaked(t):
        # the tawny breast with bold black streaks, finer bars below
        c = lerp(P["breast"], P["edge"], 0.3 * h(t.gx, t.gy, t.salt))
        if int(t.gx) % 4 == 0 and h(t.gx, t.gy // 4, t.salt) < 0.7 and t.gv < 0.6:
            return lerp(c, P["dark"], 0.8)
        if t.gy % 3 == 0 and h(t.gx // 2, t.gy, t.salt + 1) < 0.5:
            return lerp(c, P["dark"], 0.4)
        return c

    def body(t):
        if t.face in ("front", "bottom") or (t.vert and t.gv > 0.5):
            return streaked(t)
        return mott(t, P["back"])

    def disc(t):
        if t.face != "front":
            return mott(t, P["head"])
        for ex in (-1.35, 1.35):
            d = math.hypot(t.x - ex, t.y + 3.0)
            if d < 1.05:
                if abs(t.x - ex + 0.35) < 0.26 and abs(t.y + 3.35) < 0.26:
                    return (240, 236, 220, 0)
                if d < 0.42:
                    return (14, 10, 8, 0)
                return (236, 120, 24, 0)  # the big orange eyes
            if d < 1.3:
                return (30, 20, 12, 0)
        if abs(t.x) < 0.5 and t.y < -3.6:
            return P["white"]
        if abs(abs(t.x) - 2.5) < 0.3 or t.y > -0.8:
            return P["rim"]
        return lerp(P["disc"], P["edge"], 0.4 * h(t.gx, t.gy, 8))

    def tail(t):
        c = mott(t, P["tail"])
        return lerp(c, P["dark"], 0.6) if int(t.z * 2) % 3 == 0 else c
    return bird_colour(P, {"body": body, "breast": streaked, "belly": streaked, "back": lambda t: mott(t, P["back"]),
                           "rump": lambda t: mott(t, P["back"]), "disc": disc,
                           "tuft": lambda t: lerp(P["dark"], P["edge"], 0.45 * (int(t.x * 4 + 8) % 2) + 0.2 * t.n) if t.vert else mott(t, P["head"]),
                           "head": lambda t: mott(t, P["head"]) if t.face != "front" else P["rim"],
                           "neck": lambda t: streaked(t) if t.face == "front" else mott(t, P["head"]),
                           "covert": lambda t: mott(t, P["covert"]), "tail": tail,
                           "toes": lambda t: lerp(P["leg"], P["edge"], 0.3 * t.n)},
                       eye_mat="none")


def raven():
    m = flier("Raven", 4, (64, 64), body=(5, 5, 9), wing=(4, 8), tip=(6, 3), tail=(4, 6),
              legs=(3.5, 1.25, 0.5, 1.0, 2.5), tilt=0.35, tail_rot=-0.1)
    neck(m, 3, 2.5, 3, at_z=1.25)
    m.box("neck", "hackles", -1.75, -1.0, -2.0, 3.5, 2.5, 1.25)          # the shaggy throat
    m.part("head", "neck", (0, -2.25, 0))
    m.blob("head", "head", 0, -1.6, -0.6, 3.5, 3.25, 4, 0.5)
    m.box("head", "bill", -0.75, -2.5, -4.25, 1.5, 2.0, 1.75)
    m.box("head", "bill", -0.5, -2.25, -5.75, 1.0, 1.5, 1.5)
    m.box("head", "bill_tip", -0.375, -1.5, -6.25, 0.75, 1.0, 0.5)
    m.box("head", "bill_lower", -0.625, -0.5, -5.5, 1.25, 0.5, 2.5)
    m.box("head", "bristles", -0.875, -2.5, -3.5, 1.75, 1.0, 1.0)
    seat(m)
    return m


RAVEN = dict(back=hexc("#141418"), edge=hexc("#2a2e3e"), dark=hexc("#08080a"), under=hexc("#16161c"), head=hexc("#121216"),
             wing=hexc("#16161c"), wing_dark=hexc("#0a0a0e"), wing_edge=hexc("#2e3446"), covert=hexc("#1a1a22"), underwing=hexc("#26262e"),
             prim=hexc("#101014"), tail=hexc("#141418"), bill=hexc("#18181a"), leg=hexc("#1a1a1c"), thigh=hexc("#141418"),
             sheen=hexc("#3a3a5e"), purple=hexc("#3a2a4a"), black=hexc("#141418"))


def raven_colour(P):
    def gloss(t):
        c = ink(t, P)
        return lerp(c, P["purple"], 0.4) if h(t.gx // 2, t.gy // 2, t.salt + 4) < 0.15 else c
    return bird_colour(P, {"body": lambda t: scallop(t, gloss(t), P["edge"], cw=1.25, ch=1.0), "breast": gloss, "back": gloss, "rump": gloss,
                           "neck": gloss, "hackles": lambda t: None if (t.vert and t.v > 0.6 and ragged(t, 0.5)) else gloss(t),
                           "head": lambda t: False if eye(t, 0.4, 0.3, 0.4) else gloss(t),
                           "tail": lambda t: None if wedge(t, 0.35) else False,
                           "primaries": lambda t: None if finger(t, 0.78, 1.25, 0.3) else False,
                           "bristles": lambda t: lerp(P["dark"], P["sheen"], 0.3 * t.n),
                           "bill_lower": lambda t: P["bill"]},
                       eye_r=0.4, eye_at=(0.3, 0.4), iris=(30, 22, 18))


def golden_eagle():
    m = flier("Golden eagle", 4, (128, 64), body=(6, 6, 11), wing=(6, 10), tip=(11, 5), tail=(5, 6.5),
              legs=(4.0, 1.75, 0.5, 1.5, 2.5), tilt=0.45, feet="raptor", tail_rot=-0.12)
    neck(m, 4, 3, 4, at_z=1.75)
    m.box("neck", "hackles", -2.25, -2.0, -1.25, 4.5, 2.5, 3.5)
    m.part("head", "neck", (0, -2.75, 0))
    m.blob("head", "head", 0, -1.9, -0.75, 4, 4, 5, 0.75)
    m.box("head", "brow", -2.25, -3.5, -3.0, 4.5, 1, 2)
    m.box("head", "cere", -1.25, -2.75, -4.25, 2.5, 1.75, 1)
    m.box("head", "bill", -0.75, -2.5, -5.75, 1.5, 1.75, 1.5)
    m.box("head", "bill_tip", -0.5, -0.75, -5.75, 1.0, 1.0, 0.75)
    m.box("head", "bill_lower", -0.625, -0.75, -5.0, 1.25, 0.5, 1.25)
    for n in ("left_leg", "right_leg"):
        m.box(n, "trousers", -1.5, -1.0, -1.5, 3, 3.0, 3)      # feathered right down the leg
    seat(m)
    return m


GEAGLE = dict(back=hexc("#4a3424"), edge=hexc("#7a5a3e"), dark=hexc("#22160e"), under=hexc("#3e2a1c"), breast=hexc("#4a3222"),
              head=hexc("#c8963e"), wing=hexc("#3e2a1c"), wing_dark=hexc("#1e140c"), wing_edge=hexc("#6e5038"), covert=hexc("#7a5a38"),
              underwing=hexc("#3a281a"), prim=hexc("#1e1610"), tail=hexc("#4a3a2e"), bill=hexc("#6a6662"), bill_tip=hexc("#1c1a1e"),
              leg=hexc("#e8b830"), thigh=hexc("#5a4230"), claw=hexc("#141210"), cere=hexc("#e8c040"), gold=hexc("#d8a446"))


def geagle_colour(P):
    def gold(t):
        return fur(t, P["gold"], P["back"], lerp(P["gold"], (250, 220, 150), 0.5), streak=2, rough=0.6)

    def tail(t):
        if t.face == "bottom":
            c = lerp(P["underwing"], P["wing"], 0.1 + 0.4 * t.gw)
            return (*(lerp(c, P["wing"], 0.45) if (t.z * 0.7) % 1.0 < 0.2 else c), 0)
        c = flight_feathers(t, P["tail"], P["dark"], P["edge"], along="z")
        return lerp(c, (120, 110, 100), 0.4) if t.gw < 0.6 and int(t.z * 2) % 3 == 0 else c  # grey bars at the base
    return bird_colour(P, {"head": lambda t: False if eye(t, 0.5, 0.42, 0.42) else gold(t),
                           "neck": lambda t: lerp(gold(t), P["back"], 0.6 * t.gv),
                           "hackles": lambda t: None if (t.vert and t.v > 0.6 and ragged(t, 0.45)) else lerp(gold(t), P["back"], 0.3 + 0.4 * t.v),
                           "brow": lambda t: lerp(P["back"], P["dark"], 0.3),
                           "cere": lambda t: (40, 30, 10) if (t.side and abs(t.gv - 0.4) < 0.15 and t.gw > 0.5) else P["cere"],
                           "bill_lower": lambda t: P["bill"], "tail": tail,
                           "primaries": lambda t: None if finger(t, 0.78, 1.25, 0.3) else False,
                           "trousers": lambda t: None if (t.vert and t.v > 0.7 and ragged(t, 0.45)) else fur(t, P["thigh"], P["dark"], P["edge"], streak=2),
                           "toes": lambda t: lerp(P["leg"], (160, 120, 20), 0.3 * t.n)},
                       eye_r=0.5, eye_at=(0.42, 0.42), iris=(170, 100, 30), ring=(40, 28, 18), pupil=(14, 10, 8))


def vulture():
    m = flier("Griffon vulture", 4, (128, 64), body=(7, 6.5, 11), wing=(6.5, 11), tip=(12, 5.5), tail=(5, 5),
              legs=(4.0, 2.0, 0.5, 1.5, 3.0), tilt=0.5, feet="raptor", tail_rot=-0.1)
    neck(m, 2.5, 5, 2.5, at_z=2.0, rx=0.3)
    m.blob("neck", "ruff", 0, -0.5, 0.25, 5.5, 2.5, 5.0, 0.75)           # the buff ruff round the base of the neck
    m.part("head", "neck", (0, -4.75, 0))
    m.blob("head", "head", 0, -1.4, -0.6, 3, 3, 4, 0.5)
    m.box("head", "cere", -0.75, -2.0, -3.25, 1.5, 1.25, 0.75)
    m.box("head", "bill", -0.75, -2.0, -5.0, 1.5, 1.75, 1.75)
    m.box("head", "bill_tip", -0.5, -0.5, -5.0, 1.0, 0.75, 0.75)
    m.box("head", "bill_lower", -0.5, -0.5, -4.25, 1.0, 0.5, 1.25)
    seat(m)
    return m


VULTURE = dict(back=hexc("#b08a5e"), edge=hexc("#cfae80"), dark=hexc("#5a4028"), under=hexc("#a07a50"), breast=hexc("#a8825a"),
               head=hexc("#ece6da"), wing=hexc("#3a2c20"), wing_dark=hexc("#1e1610"), wing_edge=hexc("#5a4632"), covert=hexc("#b48c60"),
               underwing=hexc("#8a6a48"), prim=hexc("#1a1410"), tail=hexc("#2a2018"), bill=hexc("#c8bca0"), bill_tip=hexc("#8a8070"),
               leg=hexc("#8a8a86"), thigh=hexc("#a07a50"), claw=hexc("#2a2622"), ruff=hexc("#e6d8bc"), cere=hexc("#5a5650"))


def vulture_colour(P):
    def down(t):
        return lerp(P["head"], (200, 196, 190), 0.4 * h(t.gx, t.gy, t.salt))

    def body(t):
        c = scallop(t, P["back"], P["edge"], P["dark"], cw=1.5, ch=1.0, k=0.2)
        return lerp(c, P["under"], 0.4) if t.face == "bottom" or t.gv > 0.6 else c

    def covert(t):
        return scallop(t, P["covert"], P["edge"], P["dark"], cw=1.0, ch=0.75, k=0.2)
    return bird_colour(P, {"body": body, "back": body, "rump": body, "head": lambda t: False if eye(t, 0.42, 0.3, 0.42) else down(t),
                           "neck": down, "covert": covert,
                           "ruff": lambda t: None if (t.face == "bottom" or (t.vert and t.v > 0.65 and ragged(t, 0.4))) else
                           lerp(P["ruff"], P["edge"], 0.4 * h(t.gx, t.gy, t.salt)),
                           "wing": lambda t: covert(t) if outer(t) and t.vert and t.gv < 0.4 else False,
                           "primaries": lambda t: None if finger(t, 0.78, 1.25, 0.3) else False, "cere": lambda t: P["cere"],
                           "bill_lower": lambda t: P["bill"], "breast": lambda t: lerp(P["breast"], P["edge"], 0.4 * h(t.gx // 2, t.gy, t.salt))},
                       eye_r=0.42, eye_at=(0.3, 0.42), iris=(120, 70, 30), ring=(60, 50, 40), pupil=(12, 10, 8))


# ================================================================== MOUNTAIN WATCHER (client.folklore.MountainWatcherModel)
# the six spread finger feathers at the end of each hand: (z across the hand, length, fan angle: - forward, + back)
# the crest plumes: (x on the crown, yaw, length, lift)
WATCHER_CREST = ((0, 0.0, 6.0, 0.32), (0.6, 0.17, 5.0, 0.28), (-0.6, -0.17, 5.0, 0.28), (1.15, 0.36, 3.75, 0.22), (-1.15, -0.36, 3.75, 0.22))
WATCHER_FINGERS = ((0.75, 6.5, -0.26), (2.25, 8.5, -0.13), (3.75, 9.0, -0.02), (5.25, 8.25, 0.09), (6.75, 7.0, 0.2), (8.25, 5.5, 0.31))


def mountain_watcher():
    """The Mountain Watcher: an eagle's build drawn out into something older. A broad body, very long wings whose hands
    end in six spread finger feathers over a fringe of secondaries, a crest of three plumes and two long racket-tipped
    tail streamers. The extra parts hang under the ForestBirdModel part tree, so it flies like the eagle; the finger,
    crest and streamer parts are moved by MountainWatcherModel."""
    m = flier("Mountain Watcher", 4, (128, 128), body=(8, 7, 14), wing=(9, 12), tip=(14, 9), tail=(6, 9),
              legs=(4.5, 2.0, 0.5, 1.75, 3.0), tilt=0.45, lead=0, tip_z=0.05, feet="raptor", tail_rot=-0.12)
    for s, n in ((1, "left"), (-1, "right")):
        x0 = 0 if s > 0 else -0.5
        # the coverts stand a quarter proud of both faces of the arm (no shared faces to flicker)
        m.box(n + "_wing", "covert", x0 - 0.25, 0, 0, 1.0, 6.5, 6)
        m.box(n + "_wing", "secondaries", x0, 0.25, 12, 0.5, 8.75, 1.5)     # their rounded tips along the trailing edge
        m.box(n + "_wing_tip", "secondaries", x0, 0, 9, 0.5, 9.0, 1.25)
        for i, (z, ln, a) in enumerate(WATCHER_FINGERS):
            m.part(f"{n}_finger{i}", n + "_wing_tip", (0, 13.5, z), (a, 0, 0))
            m.box(f"{n}_finger{i}", "finger", x0, 0, -0.75, 0.5, ln, 1.5)
    neck(m, 5, 3.5, 5, at_z=2.25)
    m.box("neck", "hackles", -2.75, -2.5, -1.5, 5.5, 3.25, 4.5)          # a silver mane
    m.part("head", "neck", (0, -3.25, 0))
    m.blob("head", "head", 0, -2.4, -0.75, 5, 5, 6, 1.0)
    m.box("head", "brow", -2.75, -4.5, -4.0, 5.5, 1, 2.5)
    m.box("head", "cere", -1.25, -3.5, -5.0, 2.5, 1.75, 1.0)
    m.box("head", "bill", -0.875, -3.25, -6.75, 1.75, 2.0, 1.75)
    m.box("head", "bill_tip", -0.625, -1.25, -6.75, 1.25, 1.0, 0.75)
    m.box("head", "bill_lower", -0.625, -1.25, -6.0, 1.25, 0.5, 1.25)
    # the crest: five flat plumes swept back off the crown, fanned, the middle one longest
    for i, (dx, yr, ln, xr) in enumerate(WATCHER_CREST):
        m.part(f"crest{i}", "head", (dx, -4.6, 0.0), (xr, yr, 0))
        m.box(f"crest{i}", "plume", -0.625, -0.125, 0, 1.25, 0.25, ln)
        m.box(f"crest{i}", "plume", -0.375, -0.125, ln, 0.75, 0.25, 2.5)
    for n in ("left_leg", "right_leg"):
        m.box(n, "trousers", -1.75, -1.0, -1.75, 3.5, 3.5, 3.5)
    # two streamers out of the middle of the tail, each in two lengths so they can ripple, ending in a racket
    for s, n in ((1, "left"), (-1, "right")):
        m.part(n + "_streamer", "tail", (s * 0.75, 0.0, 8.5), (0, s * 0.13, 0))
        m.box(n + "_streamer", "streamer", -0.375, 0, 0, 0.75, 0.25, 6)
        m.part(n + "_streamer_end", n + "_streamer", (0, 0, 6))
        m.box(n + "_streamer_end", "streamer", -0.25, 0, 0, 0.5, 0.25, 5)
        m.box(n + "_streamer_end", "racket", -0.75, 0, 4.5, 1.5, 0.25, 4.5)
    seat(m)
    return m


WATCHER = dict(back=hexc("#8f9bad"), edge=hexc("#dfe6ee"), dark=hexc("#2c3446"), under=hexc("#e4e8ed"), breast=hexc("#eff0f1"),
               head=hexc("#f3f2ec"), mask=hexc("#242a3a"), wing=hexc("#6c788c"), wing_dark=hexc("#283042"), wing_edge=hexc("#d2dbe6"),
               covert=hexc("#a9b5c3"), underwing=hexc("#e2e6ec"), prim=hexc("#222a3c"), prim_dark=hexc("#121622"), tail=hexc("#3a4558"),
               bill=hexc("#e6c76e"), bill_tip=hexc("#383844"), leg=hexc("#d8bc68"), thigh=hexc("#e6e9ee"), claw=hexc("#15171e"),
               cere=hexc("#c4d0da"), teal=hexc("#3cc6c0"), violet=hexc("#8670e6"), rune=hexc("#c4f2ff"))

# five-by-five rune glyphs painted in rows along the upper wing (rows run front to back across the chord)
GLYPHS = [g.split() for g in (
    "#.#.# .###. ..#.. ..#.. ..#..", "..#.. .#.#. #...# .#.#. #...#", "###.. #..#. ###.. #.#.. #..#.",
    "#...# ##.## #.#.# ##.## #...#", "..#.. .#.#. #.#.# ..#.. ..#..", "..#.. .#.#. #...# .#.#. ..#..", "#.... #.#.. ##.#. #.#.. #....")]
WATCHER_EYE = dict(r=0.55, ew=0.42, ev=0.44)


def watcher_rune(t, along, across, a0, a1, b0, step=2.25, salt=0):
    """Is this texel on a rune? Glyphs every {step} model pixels from {a0} to {a1} along the span, their top row at
    {b0} across the chord, about one cell in five left blank."""
    if not (a0 <= along < a1):
        return False
    cell = math.floor((along - a0) / step)
    if h(cell, 11, salt) < 0.22:
        return False
    i = math.floor((along - a0 - cell * step - 0.5) * t.px)
    j = math.floor((across - b0) * t.px)
    if not (0 <= i < 5 and 0 <= j < 5):
        return False
    g = GLYPHS[int(h(cell, 12, salt) * len(GLYPHS)) % len(GLYPHS)]
    return g[j][i] == "#"


def wing_up(t):
    """The wing's upper face (outer at rest, on top in flight), judged by which side of the body the box lies on."""
    left = t.box[0] + t.box[3] / 2 > 0
    return t.face == ("left" if left else "right")


def racket_cut(t):
    """The racket at the streamer's end: a long leaf, pointed at both ends."""
    du, dw = abs(t.u - 0.5) * 2, (t.w - 0.5) * 2
    return not t.vert and du > math.sqrt(max(0.0, 1 - dw * dw)) * (1.0 - 0.35 * max(0.0, dw))


def racket_eye(t):
    du, dw = abs(t.u - 0.5) * 2, (t.w - 0.62) * 2
    return du * du * 0.8 + dw * dw * 3.0


def covert_rune(t):
    return t.side and wing_up(t) and watcher_rune(t, t.y, t.z, 0.5, 6.25, 3.5, salt=int(t.box[0] > -0.5))


def hand_rune(t):
    return t.side and wing_up(t) and watcher_rune(t, t.y, t.z, 1.0, 12.5, 1.75, salt=7 + int(t.box[0] > -0.5))


def watcher_feathers(t, base, edge, dark, cw=1.25, ch=1.0, k=0.18):
    """Overlapping feathers with rounded ends (model units): staggered rows, each feather a little darker along its
    shaft and edged pale round its rounded tip."""
    py = (t.Y if t.vert else t.Z) / ch
    row = math.floor(py)
    pxv = (t.Z if t.side else t.X) / cw + (row % 2) * 0.5
    cell = math.floor(pxv)
    fx, fy = pxv - cell, py - row
    c = lerp(base, dark, k * h(cell + 50, row + 50, t.salt))
    end = 0.72 + 0.28 * math.sqrt(max(0.0, 1 - (2 * fx - 1) ** 2))
    if fy > end:  # the next row's feathers show below the rounded tip
        return lerp(base, dark, k * h(cell + 51, row + 51, t.salt) + 0.05)
    if fy > end - 1.6 / (t.px * ch):
        return lerp(c, edge, 0.28)
    if abs(fx - 0.5) < 0.5 / (t.px * cw):
        return lerp(c, dark, 0.12)
    return c


def watcher_irid(t, P, phase=0.0):
    """The feather-tip shimmer: teal sliding into violet across the feathers."""
    k = 0.5 + 0.5 * math.sin(t.Z * 0.55 + t.Y * 0.18 + phase)
    return lerp(P["teal"], P["violet"], k)


def watcher_parts(P):
    """The painted colours per material: fn(t) -> colour, None (cut away) or False (fall through to bird_colour)."""
    def body(t):
        cut = 0.5 + 0.12 * h(t.gx // 2, t.gy // 2, t.salt + 4)  # a ragged line where the grey mantle meets the white
        if t.face == "bottom" or (t.vert and t.gv > cut):
            c = lerp(P["breast"], P["under"], 0.4 * h(t.gx // 2, t.gy // 3, t.salt))
            # fine slate chevrons on the breast and flanks
            row = (t.Y * 1.6 + abs(t.X) * 0.5) % 1.0
            return lerp(c, P["dark"], 0.28) if row < 0.16 and h(math.floor(t.Y * 1.6), math.floor(t.X), 5) < 0.7 else c
        return watcher_feathers(t, P["back"], P["edge"], P["dark"])

    def head(t):
        if (e := eye(t, WATCHER_EYE["r"], WATCHER_EYE["ew"], WATCHER_EYE["ev"], iris=(170, 232, 248), ring=(28, 34, 48))):
            return e
        c = lerp(P["head"], P["covert"], 0.25 * h(t.gx // 2, t.gy // 4, t.salt))
        # a fine dark line from the eye back, thinning to a point, with a soft grey shadow round it
        if t.side and 0.3 < t.gw < 0.95:
            back = max(0.0, t.gw - 0.42)
            d = abs(t.gv - (0.45 + 0.1 * back))
            w = 0.05 * (1.0 - back / 0.55) + 0.012
            if d < w:
                return lerp(P["mask"], P["dark"], 0.3 * t.n)
            if d < w + 0.07:
                c = lerp(c, P["covert"], 0.6)
        return lerp(c, P["edge"], 0.3) if t.face == "top" else c

    def neck(t):
        return lerp(lerp(P["head"], P["covert"], 0.25 * h(t.gx // 2, t.gy // 4, t.salt)), P["back"], 0.45 * t.gv)

    def mane(t):
        if t.vert and t.v > 0.55 and ragged(t, 0.5):
            return None
        return lerp(lerp(P["head"], P["back"], 0.15 + 0.55 * t.v), P["edge"], 0.3 * h(t.gx, 3, t.salt))

    def covert(t):
        up = wing_up(t) if t.side else t.face == "top"
        if not up:
            return (*lerp(P["underwing"], P["covert"], 0.3 * t.n), 0)
        c = watcher_feathers(t, P["covert"], P["wing_edge"], P["dark"], cw=1.0, ch=0.8)
        if covert_rune(t):
            return lerp(c, P["rune"], 0.55)
        return lerp(c, P["wing"], 0.4 * t.gw)  # greying into the flight feathers

    def arm(t):
        if not t.side:
            return lerp(P["wing_dark"], P["wing"], 0.4)
        if not wing_up(t):
            c = lerp(P["underwing"], P["wing"], 0.05 + 0.2 * t.w * t.w + 0.06 * t.n)
            return (*(lerp(c, P["wing"], 0.4) if (t.z * 1.4) % 1.0 < 0.15 else c), 0)  # finely barred underneath
        # the secondaries: feathers lying back along the chord, darkening towards their tips
        k = math.floor(t.y)
        c = lerp(P["wing"], P["wing_dark"], 0.12 + 0.2 * h(k + 70, 3, t.salt) + 0.3 * t.w)
        if t.y - k < 1.0 / t.px + 1e-6:
            c = lerp(c, P["wing_edge"], 0.35)
        return c

    def fringe(t):
        f = (t.y % 1.25) / 1.25
        if t.side and t.w > 0.35 + 0.65 * math.sqrt(max(0.0, 1 - (2 * f - 1) ** 2)):
            return None
        c = lerp(P["wing_dark"], P["prim"], 0.4 * t.n)
        return lerp(c, P["wing_edge"], 0.45) if t.side and t.w > 0.62 else c

    def hand(t):
        if not t.side:
            return P["prim_dark"]
        if not wing_up(t):
            c = lerp(P["underwing"], P["prim"], 0.1 + 0.45 * t.gv * t.gv + 0.06 * t.n)
            return (*(lerp(c, P["wing"], 0.45) if (t.y * 0.9) % 1.0 < 0.14 else c), 0)
        c = flight_feathers(t, P["prim"], P["prim_dark"], P["wing_edge"], lane=0.75, tipdark=0.2)
        if hand_rune(t):
            return lerp(c, P["rune"], 0.5)
        return lerp(c, watcher_irid(t, P), 0.55 * max(0.0, (t.gv - 0.62) / 0.38))

    def finger(t):
        if t.side and t.v > 0.8:  # the rounded end of each feather
            r = (t.v - 0.8) / 0.2
            if abs(t.w - 0.5) * 2 > math.sqrt(max(0.0, 1 - r * r)):
                return None
        if not t.side:
            return P["prim_dark"]
        if abs(t.w - 0.5) < 0.09:  # the shaft
            return lerp(P["wing_edge"], P["prim"], 0.35 + 0.4 * t.v)
        if not wing_up(t):
            base = lerp(P["underwing"], P["prim"], 0.3 + 0.6 * t.v)
            return (*(lerp(base, P["wing"], 0.4) if (t.y * 0.9) % 1.0 < 0.14 and t.v < 0.6 else base), 0)
        base = lerp(P["prim"], P["prim_dark"], 0.25 + 0.5 * abs(t.w - 0.5))
        return lerp(base, watcher_irid(t, P, 1.3), 0.8 * max(0.0, (t.v - 0.55) / 0.45))

    def tail(t):
        c = flight_feathers(t, P["tail"], P["dark"], P["edge"], along="z")
        return lerp(c, P["covert"], 0.35) if (t.z * 0.7) % 1.0 < 0.2 else c  # pale bars

    def streamer(t):
        if t.face == "bottom":
            return (*lerp(P["underwing"], P["covert"], 0.5 * t.gw), 0)
        if not t.vert and abs(t.u - 0.5) < 0.2:
            return lerp(P["wing_edge"], P["tail"], 0.3 + 0.5 * t.gw)
        return lerp(P["tail"], P["prim_dark"], 0.2 * t.n + 0.4 * t.gw)

    def racket(t):
        if t.vert:
            return P["prim_dark"]
        if racket_cut(t):
            return None
        if racket_eye(t) < 0.12:
            return lerp(P["rune"], P["teal"], 0.3)              # the eye
        if racket_eye(t) < 0.26:
            return P["prim_dark"]
        if abs(t.u - 0.5) < 0.12:
            return lerp(P["wing_edge"], P["tail"], 0.5)         # the shaft through the vane
        return lerp(watcher_irid(t, P, 2.0 + 3 * t.w), P["prim"], 0.2 + 0.25 * t.n)

    def plume(t):
        tip = t.box[5] < 1.0
        g = 0.7 + 0.3 * t.w if tip else 0.7 * t.w
        if not t.vert:
            if tip and abs(t.u - 0.5) * 2 > 1.0 - max(0.0, t.w - 0.4) * 1.6:   # tapering to a point
                return None
            if abs(t.u - 0.5) < 0.12:
                return lerp(P["edge"], P["covert"], g)
        c = lerp(P["head"], P["covert"], 0.2 + 0.6 * g)
        return lerp(c, watcher_irid(t, P, 0.6), 0.75 * max(0.0, (g - 0.55) / 0.45))

    return {"body": body, "back": body, "rump": body, "head": head, "neck": neck, "hackles": mane,
            "brow": lambda t: lerp(P["head"], P["covert"], 0.4), "covert": covert, "wing": arm,
            "secondaries": fringe, "primaries": hand, "finger": finger, "tail": tail, "streamer": streamer, "racket": racket,
            "plume": plume, "cere": lambda t: P["cere"], "bill_lower": lambda t: P["bill"],
            "bill": lambda t: lerp(P["bill"], P["bill_tip"], 0.75 * (1 - t.gw) ** 2.5),
            "bill_tip": lambda t: lerp(P["bill"], P["bill_tip"], 0.55 + 0.45 * t.v),
            "trousers": lambda t: None if (t.vert and t.v > 0.7 and ragged(t, 0.45)) else lerp(P["thigh"], P["covert"], 0.4 * t.v * h(t.gx, 2, t.salt)),
            "toes": lambda t: lerp(P["leg"], (150, 120, 40), 0.3 * t.n)}


def watcher_colour(P):
    return bird_colour(P, watcher_parts(P), eye_r=WATCHER_EYE["r"], eye_at=(WATCHER_EYE["ew"], WATCHER_EYE["ev"]), iris=(170, 232, 248))


def watcher_glow(P):
    """The skin drawn full bright over the base (the eyes layer in FolkloreClient, additive): the eyes, the runes, a
    breath of light at the feather tips and the racket eyes. Black adds nothing, so the marks are kept dim on purpose."""
    shape = watcher_parts(P)
    rune = (58, 146, 166)

    def col(mat, t):
        if mat in shape and shape[mat](t) is None:
            return None
        if mat == "head" and (e := eye(t, WATCHER_EYE["r"], WATCHER_EYE["ew"], WATCHER_EYE["ev"], iris=(120, 205, 235), glint=False)):
            return e
        if (mat == "covert" and covert_rune(t)) or (mat == "primaries" and hand_rune(t)):
            return (*rune, 0)
        if mat == "finger" and t.side and t.v > 0.72:
            return (*lerp((0, 0, 0), watcher_irid(t, P, 1.3), (0.4 if wing_up(t) else 0.28) * (t.v - 0.72) / 0.28), 0)
        if mat == "racket" and not t.vert:
            e = racket_eye(t)
            return (70, 170, 185, 0) if e < 0.12 else (*lerp((0, 0, 0), watcher_irid(t, P, 2.0 + 3 * t.w), 0.22), 0) if e > 0.26 else None
        if mat == "secondaries" and t.side and t.w > 0.62:
            return (*lerp((0, 0, 0), P["teal"], 0.16), 0)
        if mat == "plume" and t.box[5] < 1.0 and t.w > 0.5:
            return (*lerp((0, 0, 0), watcher_irid(t, P, 0.6), 0.3 * (t.w - 0.5) / 0.5), 0)
        return None
    return col


# ================================================================== WATERFOWL (WaterfowlBirdModel)
def mallard():
    m = flier("Mallard", 4, (64, 64), body=(4.5, 4, 8), wing=(3.25, 6.5), tip=(4, 2.5), tail=(3, 2),
              legs=(2.5, 1.25, 0.75, 0.5, 2.0), tilt=0.0, feet="web", lead=0.25, tail_rot=-0.05)
    neck(m, 2, 2.5, 2, at_z=1.1)
    m.part("head", "neck", (0, -2.25, 0))
    m.blob("head", "head", 0, -1.25, -0.5, 2.5, 2.75, 3.5, 0.5)
    m.box("head", "bill", -0.75, -1.0, -3.75, 1.5, 0.75, 1.5)
    m.box("head", "bill", -0.875, -0.75, -4.75, 1.75, 0.5, 1.0)
    m.box("head", "bill_tip", -0.5, -0.75, -5.0, 1.0, 0.5, 0.25)
    m.box("head", "bill_lower", -0.625, -0.25, -4.5, 1.25, 0.25, 2.0)
    m.box("tail", "curl", -0.375, -1.0, 0.25, 0.75, 1.0, 0.5)          # the drake's curled black tail feathers
    seat(m)
    return m


DRAKE = dict(back=hexc("#8a8478"), edge=hexc("#a8a294"), dark=hexc("#3e3a34"), under=hexc("#c8c6c0"), breast=hexc("#6a3a2a"),
             head=hexc("#1e5a3a"), wing=hexc("#7a7468"), wing_dark=hexc("#4a4640"), wing_edge=hexc("#a4a094"), covert=hexc("#8a867c"),
             underwing=hexc("#e8e6e2"), prim=hexc("#5e5a54"), tail=hexc("#e2e0da"), tail_dark=hexc("#9a9890"), bill=hexc("#d8c040"),
             bill_tip=hexc("#2a2a20"), leg=hexc("#e88a3a"), thigh=hexc("#c0beb8"), blue=hexc("#2e3ea0"), white=hexc("#f2f2ee"),
             black=hexc("#141418"), green2=hexc("#2a7a5a"), purple=hexc("#3a2a6a"))
DUCK = dict(DRAKE, back=hexc("#7a5a3a"), edge=hexc("#c49a6a"), dark=hexc("#3a2614"), under=hexc("#b4946c"), breast=hexc("#a07a52"),
            head=hexc("#9a7a56"), wing=hexc("#6e5438"), covert=hexc("#7a5e40"), tail=hexc("#8a6a48"), tail_dark=hexc("#4a3420"),
            bill=hexc("#d08a3a"), bill_tip=hexc("#3a2a20"), thigh=hexc("#a0805a"))


def mallard_colour(P, drake):
    def hen_mott(t, base):
        c = lerp(base, P["edge"], 0.3 * h(t.gx, t.gy, t.salt))
        return lerp(c, P["dark"], 0.6) if h(t.gx // 2, t.gy // 2, t.salt + 2) < 0.3 else c

    def body(t):
        if drake:
            if t.face == "back" or (t.face == "bottom" and t.gw > 0.8):
                return P["black"]
            if t.face == "front" or (t.vert and t.gw < 0.18 and t.gv > 0.2):
                return lerp(P["breast"], (120, 70, 50), 0.3 * h(t.gx, t.gy, t.salt))  # the chestnut breast
            if t.face == "top" or (t.vert and t.gv < 0.4 and t.face != "front"):
                return lerp(P["back"], P["edge"], 0.3 * t.n)
            # the pale grey flanks, finely vermiculated
            c = lerp(P["under"], P["white"], 0.3 * h(t.gx, t.gy, t.salt))
            return lerp(c, P["dark"], 0.18) if t.gy % 2 == 0 else c
        return scallop(t, hen_mott(t, P["back"] if t.gv < 0.5 or t.face == "top" else P["under"]), P["edge"], P["dark"], cw=1.0, ch=0.75, k=0.25)

    def head(t):
        if eye(t, 0.38, 0.32, 0.42):
            return False
        if drake:
            return lerp(P["head"], P["green2"], 0.4 * h(t.gx, t.gy, t.salt) + 0.2 * (t.face == "top"))
        if t.side and abs(t.gv - 0.45) < 0.07:
            return P["dark"]  # the hen's dark eye stripe
        return hen_mott(t, P["head"]) if t.face == "top" or t.gv < 0.35 else lerp(P["head"], P["edge"], 0.4 * t.n)

    def neck_c(t):
        if drake:
            return P["white"] if t.gv > 0.72 and t.gv < 0.9 else lerp(P["head"], P["green2"], 0.4 * h(t.gx, t.gy, t.salt))
        return lerp(P["head"], P["edge"], 0.4 * t.n)

    def wing(t):
        if outer(t) and t.vert and t.gv > 0.45 and 0.35 < t.gw < 0.8:
            # the blue speculum, white-bordered
            if abs(t.gw - 0.35) < 0.05 or abs(t.gw - 0.8) < 0.05:
                return P["white"]
            return lerp(P["blue"], P["purple"], 0.4 * h(t.gx, t.gy, t.salt))
        return False

    def bill(t):
        if not drake and t.face == "top" and 0.2 < t.gu < 0.8 and t.gw < 0.7:
            return lerp(P["bill_tip"], P["bill"], 0.3)  # the hen's dark saddle
        return lerp(P["bill"], (140, 120, 40), 0.2 * t.n)

    def breast(t):
        if drake and t.gv > 0.7:
            return body(t)
        return lerp(P["breast"], P["edge"] if not drake else (120, 70, 50), 0.3 * h(t.gx, t.gy, t.salt))
    over = {"body": body, "head": head, "neck": neck_c, "wing": wing, "bill": bill, "breast": breast, "back": body,
            "rump": lambda t: P["black"] if drake else hen_mott(t, P["back"]),
            "vent": lambda t: P["black"] if drake else P["under"],
            "curl": lambda t: P["black"] if drake else None,
            "tail": lambda t: lerp(P["tail"], P["tail_dark"], 0.3 * t.n + 0.3 * (t.gw < 0.3)) if drake else hen_mott(t, P["tail"])}
    return bird_colour(P, over, eye_r=0.38, eye_at=(0.32, 0.42), iris=(30, 20, 14))


def mute_swan():
    m = flier("Mute swan", 4, (64, 64), body=(7, 6, 12), wing=(6, 10), tip=(7, 3.5), tail=(4, 3.5),
              legs=(3.0, 2.0, 1.0, 1.25, 3.5), tilt=0.05, feet="web", tail_rot=-0.25, wing_y=0.25)
    # the long neck in a gentle S: a thick base, then the slimmer upper neck stepping forward
    neck(m, 2.5, 10, 2.5, at_z=1.5)
    m.box("neck", "neck", -1.5, -3.0, -1.0, 3, 3.0, 3)
    m.box("neck", "neck", -1.25, -11.0, -2.0, 2.5, 2.5, 2.25)
    m.part("head", "neck", (0, -10.5, -1.0))
    m.blob("head", "head", 0, -1.25, -0.5, 2.5, 2.5, 3.5, 0.5)
    m.box("head", "knob", -0.75, -2.25, -2.75, 1.5, 1.25, 1.25)
    m.box("head", "bill", -0.75, -1.25, -4.5, 1.5, 1.25, 2.0)
    m.box("head", "bill", -0.625, -1.0, -5.75, 1.25, 0.75, 1.25)
    m.box("head", "bill_tip", -0.5, -1.0, -6.0, 1.0, 0.75, 0.25)
    m.box("head", "lore", -0.875, -1.5, -3.0, 1.75, 1.25, 0.75)
    seat(m)
    return m


SWAN = dict(back=hexc("#f4f4f0"), edge=hexc("#ffffff"), dark=hexc("#d6d6d2"), under=hexc("#eeeeea"), head=hexc("#f6f6f2"),
            wing=hexc("#f2f2ee"), wing_dark=hexc("#d8d8d4"), wing_edge=hexc("#ffffff"), covert=hexc("#f6f6f2"), underwing=hexc("#e8e8e4"),
            prim=hexc("#e4e4e0"), prim_dark=hexc("#c8c8c4"), tail=hexc("#eeeeea"), bill=hexc("#e8702a"), bill_tip=hexc("#1a1a1a"),
            leg=hexc("#1c1c1e"), thigh=hexc("#e8e8e4"), black=hexc("#141414"))


def swan_colour(P):
    def soft(t):
        return lerp(P["back"], P["dark"], 0.25 * h(t.gx, t.gy, t.salt))
    return bird_colour(P, {"knob": lambda t: P["black"], "lore": lambda t: P["black"],
                           "neck": soft, "head": lambda t: False if eye(t, 0.35, 0.28, 0.42) else soft(t)},
                       eye_r=0.35, eye_at=(0.28, 0.42), iris=(26, 20, 16))


# ================================================================== WADERS (WaderModel: neck[neck2[head]], legs[shin])
def wader(name, body, tilt, necks, wing, tip, tail, tibia, shin, toe, lx=1.0):
    m = M(name, 4, (64, 64))
    w, hh, l = body
    m.bw, m.bh, m.bl, m.tilt = w, hh, l, tilt
    m.part("body", "root", (0, 10, 0), (-tilt, 0, 0))
    m.blob("body", "body", 0, 0, 0, w, hh, l, 0.75)
    m.blob("body", "breast", 0, 0.5, -l / 2 + 1.5, w - 0.5, hh - 1, 3.5, 0.75)
    m.box("body", "back", -(w / 2 + 0.5), -hh / 2 - 0.5, -l / 2 + 1.5, w + 1, 1, sn(l * 0.5, 2))
    (n1, r1), (n2, r2), hr = necks
    m.part("neck", "body", (0, -hh / 2 + 1, -l / 2 + 1), (r1, 0, 0))
    m.box("neck", "neck", -1.0, -n1, -1.0, 2, n1 + 1, 2)
    m.box("neck", "neck", -1.25, -1.5, -1.25, 2.5, 2.5, 2.5)
    m.part("neck2", "neck", (0, -n1, 0), (r2, 0, 0))
    m.box("neck2", "neck2", -0.875, -n2, -0.875, 1.75, n2 + 0.75, 1.75)
    m.part("head", "neck2", (0, -n2, 0), (hr, 0, 0))
    span, chord = wing
    tspan, tchord = tip
    for s, n in ((1, "left"), (-1, "right")):
        m.part(n + "_wing", "body", (s * (w / 2 - 0.25), -hh / 2 + 0.5, -l / 2 + 1))
        m.box(n + "_wing", "wing", 0 if s > 0 else -0.5, 0, 0, 0.5, span, chord)
        m.box(n + "_wing", "covert", 0 if s > 0 else -1.0, 0, 0, 1.0, sn(span * 0.55, 2), sn(chord * 0.45, 2))
        m.part(n + "_wing_tip", n + "_wing", (0, span, chord * 0.3), (1.5708, 0, 0))
        m.box(n + "_wing_tip", "primaries", 0 if s > 0 else -0.5, 0, 0, 0.5, tspan, tchord)
    tw, tl, tr = tail
    m.part("tail", "body", (0, -hh / 2 + 1, l / 2 - 0.5), (tr, 0, 0))
    m.box("tail", "tail", -tw / 2, 0, 0, tw, 0.5, tl)
    m.leg_y = 24 - tibia - shin
    for s, n in ((1, "left"), (-1, "right")):
        m.part(n + "_leg", "root", (s * lx, 24 - tibia - shin, 0.5), (0.2, 0, 0))
        m.box(n + "_leg", "thigh", -1.0, -1.5, -1.0, 2, 2.0, 2)
        m.box(n + "_leg", "leg", -0.5, 0, -0.5, 1, tibia, 1)
        m.part(n + "_shin", n + "_leg", (0, tibia, 0), (-0.2, 0, 0))
        m.box(n + "_shin", "leg", -0.5, 0, -0.5, 1, shin - 0.5, 1)
        m.box(n + "_shin", "toes", -0.25, shin - 0.5, -toe, 0.5, 0.5, toe)
        for side, ry in (("l", -0.55), ("r", 0.55)):
            t = f"{n}_toe_{side}"
            m.part(t, n + "_shin", ((0.25 if side == "l" else -0.25), shin - 0.25, 0), (0, ry, 0))
            m.box(t, "toes", -0.25, -0.25, -toe + 0.5, 0.5, 0.5, toe - 0.5)
        m.box(n + "_shin", "toes", -0.25, shin - 0.5, 0, 0.5, 0.5, 1.5)
    seat_wader(m)
    return m


def seat_wader(m):
    """The body's underside a little below the top of the legs (the thighs tuck into it)."""
    low = m.lowest({"body"})
    m.move("body", (m.leg_y + 1.0) - low)


def grey_heron():
    m = wader("Grey heron", (4.5, 5, 9), 0.3, ((6, 0.55), (6, -0.9), 0.65), (4.5, 8), (6.5, 3), (3, 3, -0.3), 5, 6.5, 3.5)
    m.blob("head", "head", 0, -0.6, -0.4, 2.25, 2.25, 3.5, 0.5)
    m.box("head", "bill", -0.5, -0.875, -4.25, 1, 1.0, 2.5)
    m.box("head", "bill", -0.375, -0.75, -6.25, 0.75, 0.75, 2.0)
    m.box("head", "bill_tip", -0.25, -0.625, -7.0, 0.5, 0.5, 0.75)
    m.box("head", "plume", -0.25, -1.5, 0.75, 0.5, 0.5, 3.5)            # the black crest plumes trailing back
    m.box("neck", "breast_plume", -1.0, -3.0, -1.5, 2, 3.5, 0.5)        # the long plumes hanging from the fore-neck
    return m


HERON = dict(back=hexc("#9aa2aa"), edge=hexc("#b8bec6"), dark=hexc("#5a626a"), under=hexc("#e8e8e6"), breast=hexc("#eaeae8"),
             head=hexc("#f2f2f0"), wing=hexc("#8a929c"), wing_dark=hexc("#3a3e46"), wing_edge=hexc("#aeb4bc"), covert=hexc("#a4acb4"),
             underwing=hexc("#9aa0a8"), prim=hexc("#2a2c32"), tail=hexc("#8a929c"), bill=hexc("#e2b444"), bill_tip=hexc("#c88a2a"),
             leg=hexc("#b49a5a"), leg_dark=hexc("#8a7440"), thigh=hexc("#c0b48a"), black=hexc("#18181c"), white=hexc("#f6f6f4"))


def heron_colour(P):
    def neck_c(t):
        if t.face == "front" and abs(t.x) < 0.45 and h(t.gx, t.gy // 2, t.salt) < 0.6:
            return lerp(P["black"], P["dark"], 0.3)  # the line of black streaks down the fore-neck
        return lerp(P["head"], P["edge"], 0.15 * t.n)

    def head(t):
        if eye(t, 0.35, 0.28, 0.42):
            return False
        if t.side and t.gw > 0.25 and abs(t.gv - 0.38) < 0.16:
            return P["black"]  # the black stripe from the eye back into the crest
        return lerp(P["head"], P["edge"], 0.1 * t.n)

    def body(t):
        if t.face in ("front", "bottom"):
            return lerp(P["under"], P["edge"], 0.15 * t.n)
        if t.side and t.gv < 0.3 and t.gw < 0.25:
            return P["black"]  # the black shoulder patch
        return lerp(P["back"], P["edge"], 0.3 * h(t.gx, t.gy, t.salt))
    return bird_colour(P, {"neck": neck_c, "neck2": neck_c, "head": head, "body": body,
                           "plume": lambda t: None if (ragged(t, 0.3) and t.gw > 0.6) else P["black"],
                           "breast_plume": lambda t: None if (t.vert and t.v > 0.5 and ragged(t, 0.4)) else lerp(P["white"], P["edge"], 0.3 * t.n),
                           "breast": lambda t: lerp(P["under"], P["edge"], 0.2 * t.n), "back": lambda t: lerp(P["back"], P["edge"], 0.2 * t.n),
                           "covert": lambda t: scallop(t, P["covert"], P["edge"], cw=1.0, ch=0.75),
                           "bill_lower": lambda t: P["bill"]},
                       eye_r=0.35, eye_at=(0.28, 0.42), iris=(232, 210, 60), ring=(200, 180, 60), pupil=(14, 12, 10))


def common_crane():
    m = wader("Common crane", (5, 5, 10), 0.1, ((7, 0.15), (6, 0.05), -0.1), (4.5, 9), (7, 3.5), (4, 4, -0.5), 6, 7, 3)
    m.blob("head", "head", 0, -1.0, -0.4, 2.25, 3.0, 3.0, 0.5)
    m.box("head", "crown", -0.75, -2.75, -1.25, 1.5, 0.5, 1.5)          # the red crown patch
    m.box("head", "bill", -0.5, -1.25, -3.5, 1, 1.0, 1.75)
    m.box("head", "bill", -0.375, -1.125, -5.0, 0.75, 0.75, 1.5)
    m.box("head", "bill_tip", -0.25, -1.0, -5.5, 0.5, 0.5, 0.5)
    m.box("body", "bustle", -2.75, -2.5, 2.5, 5.5, 4.0, 5.0)            # the drooping bustle of long tertials over the tail
    return m


CRANE = dict(back=hexc("#8e949a"), edge=hexc("#a6acb2"), dark=hexc("#5a5e64"), under=hexc("#8a9096"), breast=hexc("#8a9096"),
             head=hexc("#16161a"), wing=hexc("#8a9096"), wing_dark=hexc("#2a2c30"), wing_edge=hexc("#a8aeb4"), covert=hexc("#969ca2"),
             underwing=hexc("#7e848a"), prim=hexc("#18181c"), tail=hexc("#4a4e54"), bill=hexc("#a8a48a"), bill_tip=hexc("#7a7660"),
             leg=hexc("#2a2a2c"), leg_dark=hexc("#141416"), thigh=hexc("#7e848a"), black=hexc("#141418"), white=hexc("#eeeeec"),
             red=hexc("#c8282a"))


def crane_colour(P):
    def head(t):
        if eye(t, 0.35, 0.3, 0.45):
            return False
        if t.side and t.gw > 0.45 and t.gv > 0.35:
            return P["white"]
        return lerp(P["black"], (40, 40, 44), 0.3 * t.n)

    def bustle(t):
        if t.face == "top" and t.gw < 0.3:
            return lerp(P["back"], P["dark"], 0.3)
        if t.vert and t.v > 0.55 and ragged(t, 0.3 + (t.v - 0.55)):
            return None
        c = lerp(P["dark"], P["back"], 0.4 + 0.3 * h(t.gx, t.gy // 3, t.salt))
        return lerp(c, P["black"], 0.5 * t.gw)
    return bird_colour(P, {"neck": lambda t: P["back"] if t.gv > 0.8 else (P["black"] if t.face == "front" else lerp(P["back"], P["dark"], 0.2)),
                           "neck2": lambda t: P["black"] if (t.face == "front" or (t.side and t.gw < 0.35)) else P["white"],
                           "head": head, "crown": lambda t: P["red"], "bustle": bustle,
                           "body": lambda t: lerp(P["back"], P["edge"], 0.25 * h(t.gx, t.gy, t.salt)),
                           "bill_lower": lambda t: P["bill"]},
                       eye_r=0.35, eye_at=(0.3, 0.45), iris=(200, 60, 30), pupil=(14, 12, 10))


# ================================================================== OUTPUT
MAMMAL_KEEP = ("claws", "nose", "nosepad", "claw", "tip")
BIRD_KEEP = ("bill", "bill_lower", "bill_tip", "leg", "toes", "web", "claws", "cere", "knob", "lore", "comb", "wattle")


def mammal_skin(model, im):
    """Flesh (gen_skinned_textures' skinner), the claws and noses copied back."""
    out = _skinner().skin(im.copy())
    src, dst = im.load(), out.load()
    px = model.px
    for b in model.boxes:
        if b[1] in MAMMAL_KEEP:
            u, v = b[8]
            w, hh, d = b[5:8]
            for tx in range(int(u * px), int(u * px + round((2 * d + 2 * w) * px))):
                for ty in range(int(v * px), int(v * px + round((d + hh) * px))):
                    dst[tx, ty] = src[tx, ty]
    return out


def jobs():
    """(model, java method, java class it delegates from, [(texture name, colour, carcass: None | 'flesh' | 'plucked' | 'aged')])"""
    return [
        (badger(), "badger", [("badger", badger_colour(BADGER), "aged")]),
        (hare(), "hare", [("hare", hare_colour(HARE), "aged"), ("desert_hare", hare_colour(DESERT_HARE), "aged"),
                          ("hare_leveret", hare_colour(LEVERET), None)]),
        (hedgehog(), "hedgehog", [("hedgehog", hedgehog_colour(HEDGEHOG), "aged")]),
        (squirrel(), "squirrel", [("squirrel", squirrel_colour(SQUIRREL), "aged")]),
        (field_vole(), "fieldVole", [("field_vole", vole_colour(VOLE), "aged")]),
        (mole(), "mole", [("mole", mole_colour(MOLE), "aged")]),
        (wild_cat(), "wildCat", [("wild_cat", wild_cat_colour(WILDCAT), "aged"), ("wild_cat_kitten", wild_cat_colour(KITTEN), "aged")]),
        (pheasant(), "pheasant", [("pheasant", pheasant_colour(PHEASANT), ("aged", "head", 0.3, 0.25, 0.42))]),
        (pheasant_chick(), "pheasantChick", [("pheasant_chick", chick_colour(CHICK), None)]),
        (crow(), "crow", [("crow", crow_colour(CROW), None)]),
        (goose(), "goose", [("goose", goose_colour(GOOSE), ("plucked", "head", 0.4, 0.32, 0.42))]),
        (rock_dove(), "rockDove", [("rock_dove", dove_colour(DOVE), ("plucked", "head", 0.4, 0.32, 0.45))]),
        (ptarmigan(), "ptarmigan", [("ptarmigan", ptarmigan_colour(PTARMIGAN, False), ("plucked", "head", 0.4, 0.3, 0.45)),
                                    ("ptarmigan_winter", ptarmigan_colour(PTARMIGAN_W, True), ("plucked", "head", 0.4, 0.3, 0.45))]),
        (seagull(), "seagull", [("seagull", gull_colour(GULL), None)]),
        (capercaillie(), "capercaillie", [("capercaillie", caper_colour(CAPER, False), ("plucked", "head", 0.45, 0.32, 0.42)),
                                          ("capercaillie_hen", caper_colour(CAPER_HEN, True), ("plucked", "head", 0.45, 0.32, 0.42))]),
        (great_bustard(), "greatBustard", [("great_bustard", bustard_colour(BUSTARD), ("plucked", "head", 0.45, 0.3, 0.4))]),
        (eagle_owl(), "owl", [("owl", owl_colour(OWL), None)]),
        (raven(), "raven", [("raven", raven_colour(RAVEN), None)]),
        (golden_eagle(), "goldenEagle", [("golden_eagle", geagle_colour(GEAGLE), None)]),
        (vulture(), "vulture", [("vulture", vulture_colour(VULTURE), None)]),
        (mountain_watcher(), "mountainWatcher", [("mountain_watcher", watcher_colour(WATCHER), None),
                                                 ("mountain_watcher_glow", watcher_glow(WATCHER), None)]),
        (mallard(), "mallard", [("mallard_drake", mallard_colour(DRAKE, True), ("plucked", "head", 0.38, 0.32, 0.42)),
                                ("mallard_hen", mallard_colour(DUCK, False), ("plucked", "head", 0.38, 0.32, 0.42))]),
        (mute_swan(), "muteSwan", [("mute_swan", swan_colour(SWAN), ("plucked", "head", 0.35, 0.28, 0.42))]),
        (grey_heron(), "greyHeron", [("grey_heron", heron_colour(HERON), ("plucked", "head", 0.35, 0.28, 0.42))]),
        (common_crane(), "commonCrane", [("common_crane", crane_colour(CRANE), ("plucked", "head", 0.35, 0.3, 0.45))]),
    ]


# the generated sections of the shared bird models: their createXxxLayer methods delegate here
DELEGATES = {
    "BirdModel.java": ("gen_bird_textures.py", [("Crow", "crow"), ("Goose", "goose"), ("RockDove", "rockDove"),
                                                ("Ptarmigan", "ptarmigan"), ("Seagull", "seagull")]),
    "forestbirds/ForestBirdModel.java": ("gen_fauna_forestbirds_textures.py",
                                         [("Capercaillie", "capercaillie"), ("GreatBustard", "greatBustard"), ("Owl", "owl"),
                                          ("Raven", "raven"), ("GoldenEagle", "goldenEagle"), ("Vulture", "vulture")]),
    "waterfowl/WaterfowlBirdModel.java": ("gen_fauna_waterfowl_textures.py", [("Mallard", "mallard"), ("MuteSwan", "muteSwan")]),
    "waterfowl/WaderModel.java": ("gen_fauna_waterfowl_textures.py", [("GreyHeron", "greyHeron"), ("CommonCrane", "commonCrane")]),
}


def main():
    methods = []
    for model, method, skins in jobs():
        methods.append(model.java(method))
        for name, colour, carcass in skins:
            im = model.paint(colour)
            out = RES / "wildlife" / (name + ".png")
            im.save(out)
            if carcass is None:
                continue
            kind = carcass if isinstance(carcass, str) else carcass[0]
            if kind == "plucked" or (not isinstance(carcass, str) and model.name == "Pheasant"):
                sk = model.paint(plucked(None, BIRD_KEEP, carcass[1:], colour))
            else:
                sk = mammal_skin(model, im)
            sk.save(RES / "skinned/wildlife" / (name + ".png"))
            if kind == "aged":
                dry(sk).save(RES / "skinned_aged/wildlife" / (name + ".png"))
        print(f"{model.name}: {len(model.parts)} parts, {len(model.boxes)} boxes, {model.tex[0] * model.px}x{model.tex[1] * model.px}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by scripts/gen_small_models.py (with the skins it paints for them): the older small mammals' and birds' own
 * models. Each keeps the part names of the model class that animates it; their createXxxLayer methods return these.
 * Do not edit.
 */
public final class SmallGeometry {{
{chr(10).join(methods)}
    private SmallGeometry() {{}}
}}
""", encoding="utf-8")
    for rel, (script, layers) in DELEGATES.items():
        path = JDIR / rel
        src = path.read_text(encoding="utf-8")
        begin = f"    // BEGIN GENERATED by scripts/{script}\n"
        end = "    // END GENERATED\n"
        assert begin in src and end in src, f"markers missing in {rel}"
        body = "\n".join(f"    public static LayerDefinition create{c}Layer() {{\n        return com.mechanical.client.wildlife.SmallGeometry.{m}();\n    }}\n"
                         for c, m in layers)
        new = re.sub(re.escape(begin) + ".*?" + re.escape(end), lambda _: begin + body + end, src, flags=re.S)
        path.write_text(new, encoding="utf-8")


if __name__ == "__main__":
    main()
