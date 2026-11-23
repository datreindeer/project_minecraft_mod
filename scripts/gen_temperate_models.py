"""The temperate fauna's own models and skins, each from one table (docs/fauna/temperate.md, wildlife/temperate):

- src/main/java/com/mechanical/client/wildlife/temperate/TemperateGeometry.java: one LayerDefinition per animal
  (generated, do not edit). Every model keeps the part names of the animated model it is drawn with, so the existing
  animations work on it: the wisent BisonModel's, the pond turtle TortoiseModel's, the water vole FieldVoleModel's,
  the small birds BirdModel's, the tawny owl and the sea eagle ForestBirdModel's, the bittern WaderModel's, the stag
  beetle and the eel their own (StagBeetleModel, EelModel).
- their skins (textures/entity/wildlife/...), painted per texel from where it sits on the box (so markings land on
  the right spot of the head, wing or shell), and the skinned/plucked carcass skins (textures/entity/skinned/...).

Each animal is built at its own texel density (px texels per model pixel: 2 for the big ones, 4 for the small ones)
so small heads still get eyes, bills and markings. Usage: python scripts/gen_temperate_models.py. Deterministic.
Run after gen_fauna_temperate_textures.py (it overwrites the borrowed-model skins that script paints).
"""
import math
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets/mechanical/textures/entity"
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/temperate/TemperateGeometry.java"


def h(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    v = (v ^ (v >> 13)) * 1274126177 & 0xFFFFFFFF
    return (v & 0xFFFF) / 0xFFFF


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[k:k + 2], 16) for k in (0, 2, 4))


class Tex:
    """One texel: which face, where on the box (part-local x y z, model-space X Y Z ignoring rotations, and 0..1
    across the box: u from the right side (-x) to the left, v from top to bottom, w from front (-z) to back)."""
    __slots__ = ("face", "i", "j", "fw", "fh", "x", "y", "z", "X", "Y", "Z", "u", "v", "w", "n", "box", "salt", "px", "gx", "gy")

    @property
    def side(self):
        return self.face in ("left", "right")

    @property
    def vert(self):
        return self.face in ("left", "right", "front", "back")


class Model:
    def __init__(self, name, px, tex):
        self.name, self.px, self.tex = name, px, tex  # tex: declared texture size (image = tex * px)
        self.parts, self.boxes = {}, []
        self.light = (1.12, 0.7, 0.22)  # top, bottom, how much darker the sides get towards the ground

    def part(self, name, parent, at=(0, 0, 0), rot=(0, 0, 0)):
        assert name not in self.parts, name
        self.parts[name] = (parent, at, rot)

    def box(self, p, mat, x, y, z, w, hh, d):
        for v in (w, hh, d):
            assert abs(v * self.px - round(v * self.px)) < 1e-6, (self.name, p, mat, v)
        self.boxes.append([p, mat, x, y, z, w, hh, d])

    def origin(self, p):
        x = y = z = 0.0
        while p != "root":
            parent, at, _ = self.parts[p]
            x, y, z = x + at[0], y + at[1], z + at[2]
            p = parent
        return x, y, z

    # ---- layout: shelf-pack the boxes' UV rectangles, in declared texture units
    def pack(self):
        def size(b):
            w, hh, d = b[5:8]
            return math.ceil(2 * d + 2 * w - 1e-6), math.ceil(d + hh - 1e-6)
        order = sorted(range(len(self.boxes)), key=lambda i: -size(self.boxes[i])[1])
        x = y = shelf = 0
        for i in order:
            bw, bh = size(self.boxes[i])
            if x + bw > self.tex[0]:
                x, y, shelf = 0, y + shelf, 0
            self.boxes[i].append((x, y))
            x += bw
            shelf = max(shelf, bh)
        assert y + shelf <= self.tex[1], f"{self.name}: texture full ({y + shelf} > {self.tex[1]})"

    def paint(self, colour):
        px = self.px
        im = Image.new("RGBA", (self.tex[0] * px, self.tex[1] * px), (0, 0, 0, 0))
        out = im.load()
        ys = []
        for b in self.boxes:
            oy = self.origin(b[0])[1]
            ys += [oy + b[3], oy + b[3] + b[6]]
        ymin, ymax = min(ys), max(ys)
        top, bottom, fall = self.light
        for k, b in enumerate(self.boxes):
            p, mat, x0, y0, z0, w, hh, d, (u, v) = b
            ox, oy, oz = self.origin(p)
            W, H, D = (round(q * px) for q in (w, hh, d))
            U, V = u * px, v * px
            faces = {"top": (U + D, V, W, D), "bottom": (U + D + W, V, W, D), "right": (U, V + D, D, H),
                     "front": (U + D, V + D, W, H), "left": (U + D + W, V + D, D, H), "back": (U + 2 * D + W, V + D, W, H)}
            for face, (fx, fy, fw, fh) in faces.items():
                for j in range(fh):
                    for i in range(fw):
                        a, c = (i + 0.5) / fw, (j + 0.5) / fh
                        if face == "front":
                            uu, vv, ww = a, c, 0.0
                        elif face == "back":
                            uu, vv, ww = 1 - a, c, 1.0
                        elif face == "right":
                            uu, vv, ww = 0.0, c, 1 - a
                        elif face == "left":
                            uu, vv, ww = 1.0, c, a
                        elif face == "top":
                            uu, vv, ww = a, 0.0, 1 - c
                        else:
                            uu, vv, ww = a, 1.0, c
                        t = Tex()
                        t.face, t.i, t.j, t.fw, t.fh = face, i, j, fw, fh
                        t.u, t.v, t.w = uu, vv, ww
                        t.x, t.y, t.z = x0 + uu * w, y0 + vv * hh, z0 + ww * d
                        t.X, t.Y, t.Z = ox + t.x, oy + t.y, oz + t.z
                        t.n = h(fx + i, fy + j, 7 + k)
                        t.box, t.salt, t.px = (x0, y0, z0, w, hh, d), 17 + k, px
                        t.gx, t.gy = fx + i, fy + j
                        col = colour(mat, t)
                        if col is None:
                            continue
                        if len(col) == 4:  # already lit (eyes, glints)
                            out[fx + i, fy + j] = (*col[:3], 255)
                            continue
                        if face == "top":
                            f = top
                        elif face == "bottom":
                            f = bottom
                        else:
                            f = 1.06 - fall * (t.Y - ymin) / max(1e-6, ymax - ymin)
                        f *= 0.96 + 0.08 * h(fx + i, fy + j, 5)
                        out[fx + i, fy + j] = (*shade(col, f), 255)
        return im

    def java(self, method):
        self.pack()
        order = ["root"]
        while len(order) < len(self.parts) + 1:
            for n, (parent, _, _) in self.parts.items():
                if n not in order and parent in order:
                    order.append(n)
        lines = []
        for n in order[1:]:
            parent, at, rot = self.parts[n]
            cl = "CubeListBuilder.create()"
            for b in (b for b in self.boxes if b[0] == n):
                cl += f"\n                .texOffs({b[8][0]}, {b[8][1]}).addBox({f(b[2])}, {f(b[3])}, {f(b[4])}, {f(b[5])}, {f(b[6])}, {f(b[7])})"
            pose = f"PartPose.offsetAndRotation({f(at[0])}, {f(at[1])}, {f(at[2])}, {f(rot[0])}, {f(rot[1])}, {f(rot[2])})"
            var = jv(n)
            lines.append(f"        PartDefinition {var} = {jv(parent)}.addOrReplaceChild(\"{n.split("@")[0]}\", {cl},\n                {pose});")
        return f"""    /** {self.name}: {len(self.parts)} parts, {len(self.boxes)} boxes, {self.tex[0] * self.px}x{self.tex[1] * self.px} skin. */
    public static LayerDefinition {method}() {{
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
{chr(10).join(lines)}
        return LayerDefinition.create(mesh, {self.tex[0]}, {self.tex[1]});
    }}
"""


def f(v):
    s = f"{v:.4f}".rstrip("0")
    return (s + "0F") if s.endswith(".") else s + "F"


def jv(n):
    return "p_" + n.replace("@", "_") if n != "root" else "root"


# ------------------------------------------------------------------ shared painting helpers
def fur(t, base, dark, light, streak=4, rough=0.5):
    """Hair: strands running down each side face (dark roots, lighter tips), mottled on top."""
    if not t.vert:
        return lerp(base, dark, 0.15 + rough * 0.6 * h(t.gx, t.gy, t.salt))
    col = h(t.gx, 0, t.salt)
    run = h(t.gx, t.gy // streak, t.salt + 1)
    c = lerp(dark, base, 0.45 + 0.4 * col * rough + 0.35 * run)
    if (t.gy + int(col * 7)) % streak == 0:
        c = lerp(c, light, 0.4)
    return c


def wool(t, base, dark, light):
    """Curly woolly hair: clumps of light tips with dark hollows between."""
    a = h(t.gx // 3, t.gy // 2, t.salt)
    a2 = h((t.gx + 1) // 3, (t.gy + 1) // 2, t.salt + 5)
    b = h(t.gx, t.gy, t.salt + 3)
    c = lerp(dark, base, 0.55 + 0.25 * a + 0.2 * a2)
    if b > 0.8:
        c = lerp(c, light, 0.35)
    elif b < 0.1:
        c = lerp(c, dark, 0.4)
    return c


def feathers(t, base, edge, cw=3, ch=2, dark=None, noise=0.12):
    """Overlapping feathers: staggered cells, each with a pale (or dark) fringe at its lower edge."""
    row = t.gy // ch
    i = t.gx + (row % 2) * (cw // 2)
    cell = h(i // cw, row, t.salt)
    c = lerp(base, dark or base, noise * cell)
    if t.gy % ch == ch - 1 and t.vert and cell > 0.3:
        c = lerp(c, edge, 0.35)
    elif i % cw == 0 and t.vert:
        c = lerp(c, edge, 0.1)
    return c


def wing_feathers(t, base, dark, light, primaries=False):
    """Folded wing: coverts in scallops on the upper part, then long flight feathers running back with pale edges and
    darker tips (on the primaries box the feathers run along its length)."""
    if not t.vert:
        return lerp(base, dark, 0.3 * t.n)
    if primaries:
        lane = int(t.z * t.px) // 2
        c = lerp(base, dark, 0.35 + 0.25 * h(lane, 3, t.salt) + 0.3 * t.v)
        return lerp(c, light, 0.45) if int(t.z * t.px) % 2 == 0 and t.side else c
    if t.v < 0.45:
        return feathers(t, lerp(base, light, 0.15), light, 3, 2, dark, 0.3)
    lane = int(t.y * t.px) // 2
    c = lerp(base, dark, 0.2 + 0.25 * h(lane, 1, t.salt) + 0.35 * t.w)
    return lerp(c, light, 0.4) if int(t.y * t.px) % 2 == 0 and t.side else c


def near(a, b, r):
    return (a - b) ** 2 <= r * r


def eye(t, cx, cy, cz, r, iris=(20, 16, 14), ring=None, glint=True):
    """An eye on the side of the head at part-local (cx, cy, |cz|): returns a lit colour or None.
    cx is the head's half width (the eye shows on whichever side face is at that x)."""
    if not t.side:
        return None
    dz, dy = t.z - cz, t.y - cy
    dd = dz * dz + dy * dy
    if dd <= r * r:
        if glint and dz < 0 and dy < 0 and dd > (r * 0.25) ** 2 and dd < (r * 0.75) ** 2 and h(t.gx, t.gy, 3) < 0.6 and t.px >= 4:
            return (235, 235, 225, 0)
        return (*iris, 0)
    if ring and dd <= (r + 1.0 / t.px * 1.2) ** 2:
        return (*ring, 0)
    return None


# ================================================================== WISENT (BisonModel parts, bison frame: ground y 24)
def wisent():
    m = Model("Wisent", 2, (128, 128))
    m.light = (1.12, 0.66, 0.3)
    m.part("body", "root", (0, 8, 0))
    # tall, deep forequarters under the hump; the back falls away to slim, lower hindquarters
    m.box("body", "fore", -7, -10, -15, 14, 16, 13)
    m.box("body", "ruff", -6.5, -9, -16.5, 13, 13, 2)        # the long hair in front of the shoulders
    m.box("body", "brisket", -5, 6, -15, 10, 2, 9)           # shaggy fringe under the chest
    m.box("body", "barrel", -6, -8.5, -2, 12, 13, 8)
    m.box("body", "belly", -5, 4.5, -2, 10, 1.5, 7)
    m.box("body", "haunch", -5.5, -7, 6, 11, 11, 8)
    m.box("body", "rump", -4.5, -6.5, 14, 9, 9, 1.5)
    m.part("hump", "body", (0, -10, -9))                    # pivot at its base: the cow's lower hump scales from here
    m.box("hump", "hump", -5.5, -3, -5.5, 11, 3, 10)
    m.box("hump", "hump", -4.5, -4.5, -5, 9, 1.5, 9)
    m.box("hump", "crest", -3, -5.5, -4, 6, 1, 6.5)
    m.box("hump", "withers", -4.5, -3, 4.5, 9, 3, 5)
    m.part("tail", "body", (0, -5.5, 15), (0.1, 0, 0))
    m.box("tail", "tail", -0.5, 0, -0.5, 1, 9, 1)
    m.box("tail", "tuft", -1, 8, -1, 2, 4, 2)
    # the head hangs low and broad in front of the shoulders (BisonModel tips it 0.5 rad down)
    m.part("head", "body", (0, -2, -16), (0.5, 0, 0))
    m.box("head", "neck", -5, -5, -3, 10, 9, 4)
    m.box("head", "skull", -5, -4.5, -8, 10, 8.5, 5.5)
    m.box("head", "poll", -4.5, -7, -7.5, 9, 2.5, 5)          # the curly mop on the crown and forehead
    m.box("head", "muzzle", -3.25, -1.5, -11.5, 6.5, 5, 4)
    m.box("head", "nose", -2.5, -1, -12, 5, 3.5, 0.5)
    m.box("head", "jaw", -2.5, 3.5, -11, 5, 1.5, 5)
    m.part("beard", "head", (0, 4.5, -7.5))
    m.box("beard", "beard", -2.5, 0, -2, 5, 5, 4)
    m.box("beard", "beard_tip", -1.5, 5, -1.5, 3, 2.5, 3)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_ear", "head", (side * 5, -1.5, -5), (0, 0, side * 0.4))
        m.box(n + "_ear", "ear", -1.5 + side * 1.5, -0.5, -1, 3, 1, 2)
    m.part("bull_horns", "head")
    m.part("cow_horns", "head")
    for side, n in ((1, "left"), (-1, "right")):
        # out sideways from the poll, then up and forward: short and thick on the bull, slimmer on the cow
        m.part(n + "_horn", "bull_horns", (side * 4.5, -4.5, -6), (0, 0, side * 1.3))
        m.box(n + "_horn", "horn_base", -1, -3, -1, 2, 3, 2)
        m.part(n + "_horn_tip", n + "_horn", (0, -2.5, 0), (0.45, 0, -side * 1.15))
        m.box(n + "_horn_tip", "horn", -0.75, -2.5, -0.75, 1.5, 2.5, 1.5)
        m.box(n + "_horn_tip", "horn_point", -0.5, -4, -0.5, 1, 1.5, 1)
        m.part(n + "_cow_horn", "cow_horns", (side * 4.3, -4.5, -6), (0, 0, side * 1.15))
        m.box(n + "_cow_horn", "horn_base", -0.75, -2.5, -0.75, 1.5, 2.5, 1.5)
        m.part(n + "_cow_horn_tip", n + "_cow_horn", (0, -2, 0), (0.4, 0, -side * 1.0))
        m.box(n + "_cow_horn_tip", "horn_point", -0.5, -3, -0.5, 1, 3, 1)
    # legs (BisonModel: pivot y 14 and a "lower" child; ground at 24): long-haired forearms, slim shanks
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_front_leg", "root", (side * 4.3, 14, -9.5))
        m.box(n + "_front_leg", "foreleg", -2.5, -4, -2.5, 5, 9, 5)
        m.box(n + "_front_leg", "chaps", -2.75, 0, -3, 5.5, 4, 2)     # the long hair down the front of the forearm
        m.part(n + "_front_lower", n + "_front_leg", (0, 5, 0))
        m.box(n + "_front_lower", "shank", -1.5, 0, -1.5, 3, 4.5, 3)
        m.box(n + "_front_lower", "hoof", -1.75, 4, -2, 3.5, 1, 3.5)
        m.part(n + "_hind_leg", "root", (side * 3.8, 14, 10.5))
        m.box(n + "_hind_leg", "thigh", -2.5, -6, -3, 5, 11, 5.5)
        m.part(n + "_hind_lower", n + "_hind_leg", (0, 5, 0.5))
        m.box(n + "_hind_lower", "shank", -1.5, 0, -1.5, 3, 4.5, 3)
        m.box(n + "_hind_lower", "hoof", -1.75, 4, -2, 3.5, 1, 3.5)
    # BisonModel looks the lower legs up by the child name "lower"
    for n in ("left_front", "right_front", "left_hind", "right_hind"):
        p = m.parts.pop(n + "_lower")
        m.parts["lower@" + n] = p
        for b in m.boxes:
            if b[0] == n + "_lower":
                b[0] = "lower@" + n
    return m


WISENT_BULL = dict(coat=hexc("#4a3020"), coat_dark=hexc("#24160e"), coat_light=hexc("#7a5434"),
                   wool=hexc("#80603e"), wool_dark=hexc("#3c2818"), wool_light=hexc("#b08a5a"),
                   head=hexc("#3a2618"), head_dark=hexc("#1c120c"), beard=hexc("#20160f"),
                   muzzle=hexc("#2a2420"), nose=hexc("#141110"), lip=hexc("#6a5e52"),
                   horn=hexc("#3a3632"), horn_tip=hexc("#151313"), horn_light=hexc("#6c665c"),
                   hoof=hexc("#1a1614"), leg=hexc("#2c1c12"), eye_ring=hexc("#7a6a5a"))
WISENT_COW = dict(WISENT_BULL, coat=hexc("#55382a"), coat_dark=hexc("#2a1a12"), coat_light=hexc("#86603e"),
                  wool=hexc("#8a6a48"), wool_light=hexc("#b89468"), head=hexc("#442c1e"))
WISENT_CALF = dict(WISENT_BULL, coat=hexc("#9a6234"), coat_dark=hexc("#5e3a1e"), coat_light=hexc("#c48a50"),
                   wool=hexc("#b07a44"), wool_dark=hexc("#6c4424"), wool_light=hexc("#d8a86c"),
                   head=hexc("#7a4a28"), head_dark=hexc("#4a2c16"), beard=hexc("#5a3a20"), leg=hexc("#6a4222"))
FLESH = dict(coat=hexc("#a8473c"), coat_dark=hexc("#6e2622"), coat_light=hexc("#d88a78"),
             wool=hexc("#b4554a"), wool_dark=hexc("#782c26"), wool_light=hexc("#e2a492"),
             head=hexc("#9a4038"), head_dark=hexc("#62221e"), beard=hexc("#8a3a32"), muzzle=hexc("#7a3430"),
             leg=hexc("#8a3a32"), fat=hexc("#ead6b8"))


def wisent_colour(P):
    skinned = "fat" in P

    def col(mat, t):
        if skinned and mat not in ("horn_base", "horn", "horn_point", "hoof", "nose"):
            c = lerp(P.get("coat"), P["coat_dark"], 0.35 * h(t.gx // 2, t.gy, t.salt))
            if (t.gy + int(4 * h(t.gx // 3, 0, t.salt))) % 7 == 0 and t.vert:
                c = lerp(c, P["fat"], 0.55)  # white fat and sinew running through the flesh
            if mat in ("skull", "muzzle", "jaw", "neck", "poll"):
                c = lerp(c, P["head"], 0.5)
            if mat in ("skull",) and (e := eye(t, 5, -1.5, -6.5, 0.8)):
                return e
            return c
        if mat in ("fore", "ruff", "hump", "crest", "withers", "neck", "poll", "chaps", "brisket"):
            c = wool(t, P["wool"], P["wool_dark"], P["wool_light"])
            if mat in ("fore",) and t.vert:
                # the wool thins out down the flanks into the short body coat
                c = lerp(c, fur(t, P["coat"], P["coat_dark"], P["coat_light"]), max(0, (t.v - 0.55) / 0.45) * 0.8 if t.face != "front" else 0)
                if t.face == "back":
                    c = lerp(c, fur(t, P["coat"], P["coat_dark"], P["coat_light"]), 0.6)
            if mat in ("brisket", "chaps") and t.vert:
                c = lerp(c, P["wool_dark"], 0.35 + 0.35 * t.v)
                if t.v > 0.75 and h(t.gx, 1, t.salt) < 0.4:
                    return None  # ragged ends of the long hair
            if mat in ("neck", "poll", "ruff"):
                c = lerp(c, P["head"], 0.3 if mat != "poll" else 0.1)
            return c
        if mat in ("barrel", "belly", "haunch", "rump"):
            c = fur(t, P["coat"], P["coat_dark"], P["coat_light"], streak=3, rough=0.6)
            if mat == "barrel" and t.vert and t.w < 0.35:
                c = lerp(c, wool(t, P["wool"], P["wool_dark"], P["wool_light"]), 0.6 * (1 - t.w / 0.35))
            if mat == "belly" or t.face == "bottom":
                c = lerp(c, P["coat_dark"], 0.45)
            if mat in ("haunch", "rump") and t.face in ("back",):
                c = lerp(c, P["coat_dark"], 0.25)
            return c
        if mat in ("tail",):
            return fur(t, P["coat"], P["coat_dark"], P["coat_light"])
        if mat == "tuft":
            c = fur(t, P["beard"], P["head_dark"], P["coat"], streak=2)
            return None if t.vert and t.v > 0.7 and h(t.gx, 2, t.salt) < 0.45 else c
        if mat in ("skull",):
            if (e := eye(t, 5, -1.5, -6.5, 0.8, iris=(24, 16, 12), ring=P["eye_ring"])):
                return e
            c = fur(t, P["head"], P["head_dark"], P["coat_light"], streak=2)
            if t.face == "front":
                c = wool(t, P["head"], P["head_dark"], P["wool"])  # curly forehead
            return c
        if mat == "muzzle":
            c = fur(t, P["head"], P["head_dark"], P["coat"], streak=2)
            return lerp(c, P["muzzle"], 0.25 + 0.6 * t.w ** 0.5 * 0 + 0.5 * (1 - t.w) ** 3)
        if mat == "nose":
            if t.face == "front":
                if near(t.x, -1.4, 0.6) and near(t.y, 0.4, 0.5) or near(t.x, 1.4, 0.6) and near(t.y, 0.4, 0.5):
                    return (10, 8, 8, 0)  # nostrils
                if t.y > 1.9:
                    return lerp(P["lip"], P["nose"], 0.4 + 0.3 * t.n)
                return lerp(P["nose"], P["muzzle"], 0.3 * t.n + 0.15 * (t.y > 0.9))
            return P["nose"]
        if mat == "jaw":
            c = lerp(P["muzzle"], P["head_dark"], 0.5 * t.n)
            return lerp(c, P["lip"], 0.4) if t.face == "front" or (t.vert and t.w < 0.15) else c
        if mat in ("beard", "beard_tip"):
            c = fur(t, P["beard"], P["head_dark"], P["head"], streak=2, rough=0.8)
            return None if mat == "beard_tip" and t.vert and t.v > 0.6 and h(t.gx, 4, t.salt) < 0.5 else c
        if mat == "ear":
            return lerp(P["head"], P["head_dark"], 0.5 * t.n) if t.face != "bottom" else lerp(P["head"], P["lip"], 0.3)
        if mat in ("horn_base", "horn", "horn_point"):
            k = {"horn_base": 0.0, "horn": 0.45, "horn_point": 0.85}[mat] + 0.15 * (1 - t.v)
            c = lerp(P["horn_light"], P["horn_tip"], k)
            if mat == "horn_base" and int(t.v * 6) % 2 == 0:
                c = lerp(c, P["horn_tip"], 0.25)  # growth rings
            return c
        if mat in ("foreleg", "thigh"):
            c = fur(t, P["coat"], P["coat_dark"], P["coat_light"], streak=3)
            return lerp(c, P["leg"], 0.6 * t.v)
        if mat == "shank":
            c = fur(t, P["leg"], P["head_dark"], P["coat"], streak=2)
            return lerp(c, P["head_dark"], 0.3 * t.v)
        if mat == "hoof":
            c = lerp(P["hoof"], P["horn_light"], 0.2 * t.n)
            if t.face == "front" and near(t.x, 0, 0.3):
                return lerp(P["hoof"], (0, 0, 0), 0.6)  # the cleft
            return c
        return P["coat"]
    return col


# ================================================================== POND TURTLE (TortoiseModel parts, ground y 24)
def pond_turtle():
    m = Model("European pond turtle", 4, (128, 64))
    m.light = (1.15, 0.65, 0.15)
    m.part("body", "root", (0, 21.5, 0))
    m.box("body", "plastron", -4.5, 0, -5.5, 9, 0.75, 11)
    m.box("body", "marginal", -5.5, -1.5, -6, 11, 1.5, 12)    # the low rim of marginal scutes, rounded at the ends
    m.box("body", "marginal", -4.5, -1.5, -7, 9, 1.5, 1)
    m.box("body", "marginal", -4.5, -1.5, 6, 9, 1.5, 1.25)
    m.box("body", "costal", -5, -2.5, -5.75, 10, 1, 11.5)
    m.box("body", "costal", -3.5, -2.5, -6.5, 7, 1, 0.75)
    m.box("body", "costal", -3.5, -2.5, 5.75, 7, 1, 0.75)
    m.box("body", "dome", -4, -3.25, -5, 8, 0.75, 10)
    m.box("body", "keel", -2, -3.75, -4, 4, 0.5, 8)            # a low ridge of vertebral scutes
    m.part("neck", "body", (0, -0.5, -6.5), (-0.25, 0, 0))
    m.box("neck", "neck", -1.25, -1.25, -3.5, 2.5, 2.5, 4)
    m.part("head", "neck", (0, -0.25, -3.5), (0.25, 0, 0))
    m.box("head", "skull", -1.5, -1.75, -3, 3, 2.5, 3)
    m.box("head", "snout", -1, -1.5, -4.25, 2, 1.75, 1.25)
    m.box("head", "jaw", -1.25, 0.75, -3.75, 2.5, 0.5, 3)
    m.part("tail", "body", (0, -0.5, 6.5), (-0.35, 0, 0))
    m.box("tail", "tail", -0.75, -0.5, 0, 1.5, 1, 3)
    m.box("tail", "tail_tip", -0.25, -0.25, 3, 0.5, 0.5, 2)
    for side, n in ((1, "left"), (-1, "right")):
        # flat paddle-legs splayed out from under the rim, webbed feet with claws
        m.part(n + "_front_leg", "root", (side * 4.5, 22, -4), (0, 0, -side * 0.4))
        m.box(n + "_front_leg", "leg", -1, -0.5, -1, 2, 2.25, 2)
        m.box(n + "_front_leg", "foot", -1.25 + side * 0.25, 1.5, -2, 2.5, 0.5, 2.5)
        m.box(n + "_front_leg", "claws", -1.25 + side * 0.25, 1.5, -2.5, 2.5, 0.5, 0.5)
        m.part(n + "_hind_leg", "root", (side * 4.25, 22, 4.25), (0, 0, -side * 0.3))
        m.box(n + "_hind_leg", "leg", -1.25, -0.5, -1.25, 2.5, 2.25, 2.5)
        m.box(n + "_hind_leg", "foot", -1.5 + side * 0.25, 1.5, -1.5, 3, 0.5, 3)
        m.box(n + "_hind_leg", "claws", -1.5 + side * 0.25, 1.5, -2, 3, 0.5, 0.5)
    return m


TURTLE = dict(shell=hexc("#1e2618"), shell_dark=hexc("#0c100a"), shell_light=hexc("#3c4a2c"), spot=hexc("#e0c64a"),
              skin=hexc("#24291e"), skin_dark=hexc("#11140e"), plastron=hexc("#b8a244"), plastron_dark=hexc("#5a4a22"),
              claw=hexc("#c8c0a0"), beak=hexc("#4a4a3a"))
TURTLE_SKINNED = dict(TURTLE, skin=hexc("#b05a4c"), skin_dark=hexc("#743228"), spot=hexc("#e8b090"))


def turtle_colour(P):
    def col(mat, t):
        if mat in ("marginal", "costal", "dome", "keel", "nuchal"):
            if t.face == "bottom":
                return lerp(P["plastron"], P["plastron_dark"], 0.4 * t.n)
            # scute plates: seams between them, fine yellow dots radiating in each
            sx, sz = (2.5, 2.75) if mat != "marginal" else (1.25, 1.4)
            gu, gz = (t.x + 20) / sx, (t.z + 20) / sz
            if t.vert and mat == "marginal":
                gu = (t.z + 20) / sz if t.side else gu
            seam = (abs(gu - round(gu)) < 0.12 * (4 / t.px)) or (not t.vert and abs(gz - round(gz)) < 0.12 * (4 / t.px))
            c = lerp(P["shell"], P["shell_dark"], 0.35 * t.n)
            if mat in ("dome", "keel"):
                c = lerp(c, P["shell_light"], 0.25)
            if seam:
                return lerp(c, P["shell_dark"], 0.75)
            if h(t.gx, t.gy, 11) < 0.035:
                return lerp(P["spot"], c, 0.2 + 0.3 * t.n)  # fine yellow flecks
            if mat == "marginal" and t.vert and t.v > 0.7:
                return lerp(c, P["plastron"], 0.35)  # the yellowish lower edge of the rim
            return c
        if mat == "plastron":
            c = lerp(P["plastron"], P["plastron_dark"], 0.3 * t.n)
            return lerp(c, P["shell_dark"], 0.5) if abs(t.x) < 0.2 or abs(t.z % 3 - 1.5) < 0.15 else c
        if mat in ("skull", "snout"):
            if mat == "skull" and (e := eye(t, 1.5, -0.7, -2.1, 0.3, iris=(40, 30, 12))):
                return e
            c = lerp(P["skin"], P["skin_dark"], 0.3 * t.n)
            if h(t.gx, t.gy, 21) < 0.1:
                return lerp(P["spot"], c, 0.3)
            if mat == "snout" and t.face == "front" and t.y > -0.6:
                return lerp(P["beak"], P["skin_dark"], 0.3)
            return c
        if mat in ("neck", "leg", "tail", "tail_tip", "jaw", "foot"):
            c = lerp(P["skin"], P["skin_dark"], 0.35 * t.n)
            if t.face == "bottom" or (mat in ("neck", "jaw") and t.v > 0.6):
                c = lerp(c, P["plastron"], 0.3)
            if h(t.gx, t.gy, 23) < 0.06:
                return lerp(P["spot"], c, 0.3)
            if mat == "foot" and t.vert and int(t.x * t.px) % 3 == 0:
                return lerp(c, P["skin_dark"], 0.5)  # the toes in the web
            return c
        if mat == "claws":
            return lerp(P["claw"], P["skin_dark"], 0.45) if int(t.x * t.px) % 3 == 0 else lerp(P["skin"], P["skin_dark"], 0.5)
        return P["shell"]
    return col


# ================================================================== WATER VOLE (FieldVoleModel parts, ground y 24)
def water_vole():
    m = Model("Water vole", 4, (32, 32))
    m.light = (1.1, 0.7, 0.18)
    m.part("body", "root", (0, 21, 0))
    m.box("body", "rump", -2, -1.75, -0.5, 4, 3.75, 3.5)
    m.box("body", "chest", -1.75, -1.5, -3, 3.5, 3.25, 2.5)
    m.box("body", "back", -1.5, -2.25, -2.5, 3, 0.5, 5)          # the rounded back
    m.box("body", "haunch", -2.25, -1, 0, 4.5, 2.5, 2.5)
    m.box("body", "seat", -1.5, -1.25, 3, 3, 2.75, 0.5)
    m.part("head", "body", (0, -0.5, -3))
    m.box("head", "skull", -1.5, -1.5, -2, 3, 2.75, 2.25)
    m.box("head", "snout", -1, -0.75, -3, 2, 1.75, 1)
    m.box("head", "nose", -0.5, -0.5, -3.25, 1, 0.5, 0.25)
    m.box("head", "cheeks", -1.75, -0.5, -1.75, 3.5, 1.5, 1.5)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_ear", "head", (side * 1.1, -1.4, -0.6), (0, 0, side * 0.3))
        m.box(n + "_ear", "ear", -0.5, -0.75, -0.25, 1, 0.75, 0.5)
        m.box("head", "whiskers", -2.25 if side < 0 else 1.0, 0.0, -2.75, 1.25, 0.0 + 0.25, 0.25)
    m.part("tail", "body", (0, -0.75, 3.25), (-0.15, 0, 0))
    m.box("tail", "tail", -0.375, -0.375, 0, 0.75, 0.75, 3.75)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_front_leg", "root", (side * 1.2, 22.75, -2.25))
        m.box(n + "_front_leg", "leg", -0.5, 0, -0.5, 1, 1, 1)
        m.box(n + "_front_leg", "paw", -0.5, 1, -1, 1, 0.25, 1.25)
        m.part(n + "_hind_leg", "root", (side * 1.5, 22.75, 1.5))
        m.box(n + "_hind_leg", "leg", -0.625, 0, -0.75, 1.25, 1, 1.25)
        m.box(n + "_hind_leg", "paw", -0.625, 1, -1.75, 1.25, 0.25, 2)
    return m


VOLE = dict(fur=hexc("#5e4630"), dark=hexc("#2e2016"), light=hexc("#8a6a48"), belly=hexc("#8a7a66"),
            snout=hexc("#3a2c22"), nose=hexc("#1a1412"), paw=hexc("#6a5a4c"), tail=hexc("#3a2c22"), ear=hexc("#4a3628"))
VOLE_SKINNED = dict(VOLE, fur=hexc("#a8473c"), dark=hexc("#6e2622"), light=hexc("#d88a78"), belly=hexc("#c07060"),
                    snout=hexc("#9a4038"), tail=hexc("#9a4038"), ear=hexc("#9a4038"))


def vole_colour(P):
    def col(mat, t):
        if mat in ("rump", "chest", "back", "haunch", "seat", "skull", "cheeks", "snout"):
            if mat == "skull" and (e := eye(t, 1.5, -0.55, -1.2, 0.36, iris=(12, 10, 10))):
                return e
            c = fur(t, P["fur"], P["dark"], P["light"], streak=3, rough=0.7)
            if t.face == "bottom" or (t.vert and t.Y > 22.4):
                c = lerp(c, P["belly"], 0.55)  # greyer, paler belly
            if mat == "snout":
                c = lerp(c, P["snout"], 0.35 + 0.4 * (1 - t.w))
            return c
        if mat == "nose":
            return P["nose"]
        if mat == "whiskers":
            return (200, 190, 175) if h(t.gx, t.gy, 2) < 0.5 else None
        if mat == "ear":
            return lerp(P["ear"], P["dark"], 0.3 * t.n)
        if mat == "tail":
            return lerp(P["tail"], P["dark"], 0.3 * t.n + 0.2 * (t.face == "top"))
        if mat in ("leg",):
            return lerp(P["fur"], P["dark"], 0.4)
        if mat == "paw":
            return lerp(P["paw"], P["dark"], 0.3 * t.n) if not (t.face == "front" and int(t.x * t.px) % 2) else (200, 190, 170)
        return P["fur"]
    return col


# ================================================================== STAG BEETLE (StagBeetleModel parts, ground y 24)
def stag_beetle():
    m = Model("Giant stag beetle", 4, (64, 64))
    m.light = (1.25, 0.6, 0.1)
    m.part("body", "root", (0, 22, 0.5))
    # abdomen under the two domed wing cases (elytra), split by the suture down the middle
    m.box("body", "abdomen", -2.25, 0, -1, 4.5, 0.75, 6)
    for side, n in ((1, "left"), (-1, "right")):
        x0 = 0 if side > 0 else -2.75
        m.box("body", "elytron", x0, -1.5, -1.25, 2.75, 1.75, 6.25)
        m.box("body", "elytron_top", x0 + (0.25 if side > 0 else 0.25), -2, -0.75, 2.25, 0.5, 5.25)
    m.box("body", "elytron_tip", -2, -1.25, 5, 4, 1.25, 0.75)
    m.part("thorax", "body", (0, -0.5, -1.25))
    m.box("thorax", "pronotum", -2.75, -1.25, -2.5, 5.5, 1.75, 2.5)
    m.box("thorax", "pronotum_top", -2.25, -1.5, -2.25, 4.5, 0.25, 2)
    m.box("thorax", "waist", -1.5, -0.5, -0.25, 3, 1, 0.5)
    m.part("head", "thorax", (0, -0.25, -2.5))
    m.box("head", "headplate", -2.5, -0.75, -2, 5, 1.25, 2)       # the male's broad, flat head
    m.box("head", "brow", -2, -1, -1.75, 4, 0.25, 1.25)
    for side, n in ((1, "left"), (-1, "right")):
        m.box("head", "eye", (2.5 if side > 0 else -2.75), -0.5, -1.5, 0.25, 0.75, 0.75)
        # elbowed antennae with a comb-like club
        m.part(n + "_antenna", "head", (side * 2.1, -0.25, -1.9), (0.3, side * -0.6, 0))
        m.box(n + "_antenna", "antenna", -0.125, -0.125, -1.75, 0.25, 0.25, 1.75)
        m.part(n + "_antenna_club", n + "_antenna", (0, 0, -1.75), (0, side * 0.9, 0))
        m.box(n + "_antenna_club", "antenna", -0.125, -0.125, -1, 0.25, 0.25, 1)
        m.box(n + "_antenna_club", "club", -0.25, -0.25, -1.75, 0.5, 0.5, 0.75)
    for side, n in ((1, "left"), (-1, "right")):
        # the male's antler-jaws: a thick base, curving in, an inner tine halfway, forked tip pointing in
        jaw = n + "_jaw"
        m.part(jaw, "head", (side * 1.4, -0.25, -1.9), (0.2, -side * 0.3, 0))
        m.box(jaw, "mandible", -0.5, -0.5, -2.5, 1, 1, 2.5)
        m.part(jaw + "_mid", jaw, (0, 0, -2.5), (-0.1, side * 0.35, 0))
        m.box(jaw + "_mid", "mandible", -0.375, -0.375, -2.25, 0.75, 0.75, 2.25)
        m.box(jaw + "_mid", "tine", (-1.25 if side > 0 else 0.25), -0.25, -1.5, 1, 0.5, 0.5)
        m.part(jaw + "_tip", jaw + "_mid", (0, 0, -2.25), (0, side * 0.6, 0))
        m.box(jaw + "_tip", "mandible_tip", -0.25, -0.25, -1.5, 0.5, 0.5, 1.5)
        m.box(jaw + "_tip", "mandible_tip", (-0.75 if side > 0 else 0.25), -0.5, -1.25, 0.5, 0.25, 0.5)
    m.part("small_jaws", "head", (0, 0, -2))
    for side in (1, -1):
        m.box("small_jaws", "mandible", (0.25 if side > 0 else -1.25), -0.25, -1.25, 1, 0.5, 1.25)
    legz = (-2.5, 1.0, 3.25)
    for k in range(3):
        for side, n in ((1, "left"), (-1, "right")):
            leg = f"{n}_leg_{k}"
            # femur out sideways (StagBeetleModel swings it with yRot), tibia bent down to the ground
            m.part(leg, "root", (side * 2.1, 22.25, legz[k]), (0, side * (k - 1) * 0.5, side * 0.15))
            m.box(leg, "femur", 0 if side > 0 else -2.5, -0.375, -0.375, 2.5, 0.75, 0.75)
            m.part(leg + "_tibia", leg, (side * 2.5, 0, 0), (0, 0, side * 0.95))
            m.box(leg + "_tibia", "tibia", 0 if side > 0 else -2, -0.25, -0.25, 2, 0.5, 0.5)
            m.box(leg + "_tibia", "spur", (1.0 if side > 0 else -1.5), -0.5, -0.125, 0.5, 0.25, 0.25)
            m.part(leg + "_tarsus", leg + "_tibia", (side * 2, 0, 0), (0, 0, -side * 0.8))
            m.box(leg + "_tarsus", "tarsus", 0 if side > 0 else -1.5, -0.125, -0.125, 1.5, 0.25, 0.25)
    return m


BEETLE_M = dict(elytra=hexc("#5a2a14"), elytra_dark=hexc("#2a120a"), elytra_light=hexc("#9a5a30"), black=hexc("#141010"),
                black_light=hexc("#4a4040"), jaw=hexc("#6a3018"), jaw_dark=hexc("#2a1008"), leg=hexc("#1a1414"))
BEETLE_F = dict(BEETLE_M, elytra=hexc("#2e1a10"), elytra_light=hexc("#5a3a24"), jaw=hexc("#241814"))


def beetle_colour(P):
    def col(mat, t):
        if mat in ("elytron", "elytron_top", "elytron_tip"):
            c = lerp(P["elytra"], P["elytra_dark"], 0.25 * t.n)
            if t.face == "top" or mat == "elytron_top":
                # a glossy sheen along each wing case, the suture dark
                glint = 1 - abs((abs(t.x) - 1.4) / 1.4)
                c = lerp(c, P["elytra_light"], 0.5 * glint * (1 - abs(t.w - 0.35)))
                if abs(t.x) < 0.2:
                    return lerp(P["elytra_dark"], P["black"], 0.5)
                if h(t.gx, t.gy, 3) > 0.96:
                    return lerp(c, (240, 220, 200), 0.6)
            if t.vert and t.v > 0.7:
                c = lerp(c, P["elytra_dark"], 0.6)
            return c
        if mat in ("pronotum", "pronotum_top", "headplate", "brow", "waist", "abdomen"):
            c = lerp(P["black"], P["black_light"], 0.25 * t.n)
            if (t.face == "top" or mat in ("pronotum_top", "brow")) and h(t.gx, t.gy, 5) > 0.9:
                c = lerp(c, (150, 140, 135), 0.5)  # glossy highlights on the black
            if mat == "headplate" and t.face == "top" and abs(t.w - 0.4) < 0.1:
                c = lerp(c, P["black_light"], 0.5)  # the ridge across the male's head
            return c
        if mat == "eye":
            return (60, 52, 40, 0) if h(t.gx, t.gy, 2) < 0.7 else (150, 130, 100, 0)
        if mat in ("mandible", "tine", "mandible_tip"):
            c = lerp(P["jaw"], P["jaw_dark"], 0.3 * t.n + (0.3 if mat == "mandible_tip" else 0))
            if t.face == "top" and h(t.gx, t.gy, 6) > 0.8:
                c = lerp(c, P["elytra_light"], 0.6)
            if t.face in ("bottom",) or (t.vert and t.v > 0.7):
                c = lerp(c, P["jaw_dark"], 0.4)
            return c
        if mat in ("antenna", "club", "femur", "tibia", "tarsus", "spur"):
            c = lerp(P["leg"], P["black_light"], 0.25 * t.n)
            if mat == "club":
                c = lerp(c, P["elytra"], 0.35)
            return c
        return P["black"]
    return col


# ================================================================== small birds (BirdModel parts, ground y 24)
def songbird(name, body, neck, head, bill, wing, tip, tail, legs, crest=None, tilt=0.0):
    """A small bird on BirdModel's part tree. body (w, h, l, pivot y), neck (w, h, l), head (w, h, l), bill (w, h, l),
    wing (span, chord), tip (span, chord), tail (w, l), legs (length, x, z); crest: a chain of (w, h, l, rx) feathers
    starting at the back of the crown, each turned rx (up) from the one before."""
    m = Model(name, 4, (64, 64))
    m.light = (1.15, 0.7, 0.16)
    bw, bh, bl, by = body
    # a rounded body: the main box, a fuller breast, a belly bulge, the back tapering into the tail
    m.part("body", "root", (0, by, 0), (tilt, 0, 0))
    m.box("body", "body", -bw / 2, -bh / 2, -bl / 2, bw, bh, bl)
    m.box("body", "breast", -bw / 2 + 0.25, -bh / 2 + 0.5, -bl / 2 - 0.5, bw - 0.5, bh - 1, 0.5)
    m.box("body", "belly", -bw / 2 + 0.5, bh / 2, -bl / 2 + 0.75, bw - 1, 0.25, bl - 2)
    m.box("body", "rump", -bw / 2 + 0.5, -bh / 2 + 0.25, bl / 2, bw - 1, bh - 1.25, 0.5)
    nw, nh, nl = neck
    m.part("neck", "body", (0, -bh / 2 + 0.75, -bl / 2 + nl / 2 - 0.25), (-tilt, 0, 0))
    m.box("neck", "neck", -nw / 2, -nh, -nl / 2, nw, nh, nl)
    hw, hh, hl = head
    m.part("head", "neck", (0, -nh + 0.25, 0))
    m.box("head", "head", -hw / 2, -hh, -hl / 2 - 0.25, hw, hh, hl)
    m.box("head", "cheek", -hw / 2 + 0.25, -hh + 0.25, -hl / 2 - 0.5, hw - 0.5, hh - 0.5, 0.25)   # rounds the face
    bw_, bh_, bl_ = bill
    zb = -hl / 2 - 0.5
    yb = -hh * 0.42
    m.box("head", "bill", -bw_ / 2, yb - bh_ / 2, zb - bl_ / 2, bw_, bh_, bl_ / 2)
    m.box("head", "bill", -bw_ / 2 + 0.125, yb - bh_ / 2 + 0.125, zb - bl_, bw_ - 0.25, bh_ - 0.25, bl_ / 2)
    parent, at = "head", (0, -hh + 0.25, hl / 2 - 0.75)
    for k, (w, hh2, l, rx) in enumerate(crest or []):
        c = f"crest{k}"
        m.part(c, parent, at, (rx, 0, 0))
        m.box(c, "crest", -w / 2, -hh2, 0, w, hh2, l)
        parent, at = c, (0, 0, l - 0.25)
    span, chord = wing
    tspan, tchord = tip
    for side, n in ((1, "left"), (-1, "right")):
        # folded wings lie snug along the flanks, the primaries crossing back over the rump
        m.part(n + "_wing", "body", (side * (bw / 2 - 0.25), -bh / 2 + 0.25, -bl / 2 + 0.5))
        m.box(n + "_wing", "wing", 0 if side > 0 else -0.5, 0, 0, 0.5, span, chord)
        m.part(n + "_wing_tip", n + "_wing", (0, span, 0.75), (1.5708, 0, 0))
        m.box(n + "_wing_tip", "primaries", 0 if side > 0 else -0.5, 0, 0, 0.5, tspan, tchord)
    tw, tl = tail
    m.part("tail", "body", (0, -bh / 2 + 0.75, bl / 2 - 0.25), (-0.12, 0, 0))
    m.box("tail", "tail", -tw / 2, 0, 0, tw, 0.5, tl)
    m.box("tail", "tail", -tw / 2 + 0.25, 0, tl, tw - 0.5, 0.5, 0.25)
    ll, lx, lz = legs
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_leg", "root", (side * lx, 24 - ll, lz))
        m.box(n + "_leg", "thigh", -0.5, -0.5, -0.5, 1, 1, 1)
        m.box(n + "_leg", "leg", -0.25, 0.5, -0.25, 0.5, ll - 0.75, 0.5)
        for dx in (-0.375, 0.125):
            m.box(n + "_leg", "toes", dx, ll - 0.25, -1.25, 0.25, 0.25, 1.25)
        m.box(n + "_leg", "hind_toe", -0.125, ll - 0.25, 0, 0.25, 0.25, 0.75)
    return m


def woodpecker():
    # great spotted woodpecker: stocky, a stiff wedge tail it props itself on, a straight chisel bill
    return songbird("Great spotted woodpecker", (3, 3.25, 5, 20.75), (1.75, 1.25, 1.75), (2.25, 2.25, 2.5), (0.75, 0.75, 2),
                    (2.25, 4.5), (3.5, 2), (2, 4), (2.25, 0.75, 0.25), tilt=-0.35)


def skylark():
    # skylark: streaky brown, a short blunt crest it raises, a long hind claw
    return songbird("Skylark", (2.75, 2.5, 4.25, 21.25), (1.25, 0.75, 1.25), (1.75, 1.75, 2.25), (0.5, 0.5, 1),
                    (1.75, 3.75), (3, 1.75), (1.5, 3.25), (1.75, 0.6, 0.25),
                    crest=[(0.75, 0.5, 1.5, 0.3)])


def lapwing():
    # lapwing: a plover on longer pink legs, broad rounded wings, the long wispy crest swept back and curling up
    return songbird("Lapwing", (3, 3, 5, 19.75), (1.5, 1.25, 1.5), (2, 2, 2.5), (0.5, 0.5, 1.5),
                    (2.25, 4.75), (3.5, 2.25), (2, 2.75), (3.25, 0.75, 0.25),
                    crest=[(0.5, 0.5, 2, 0.05), (0.25, 0.5, 2, 0.3)])


WOODPECKER = dict(black=hexc("#141418"), black2=hexc("#2a2a32"), white=hexc("#ecebe4"), buff=hexc("#d8d0c0"),
                  red=hexc("#c8202a"), red2=hexc("#8a1018"), bill=hexc("#2e2e30"), leg=hexc("#5a5a5e"))
SKYLARK = dict(brown=hexc("#8a6a42"), dark=hexc("#3e2c18"), light=hexc("#c4a674"), belly=hexc("#e2d6bc"),
               streak=hexc("#5a4024"), bill=hexc("#b49a78"), leg=hexc("#c8a080"), white=hexc("#ece6d8"))
LAPWING = dict(green=hexc("#28503c"), green2=hexc("#3c7050"), purple=hexc("#4a3a5a"), black=hexc("#101412"),
               white=hexc("#eceae4"), orange=hexc("#d88a4a"), bill=hexc("#181818"), leg=hexc("#c88a8a"), grey=hexc("#c8c8c0"))
PLUCKED = dict(skin=hexc("#e8b8a0"), skin_dark=hexc("#b07a64"), pore=hexc("#d09a84"), leg=hexc("#c8a080"), bill=hexc("#8a8070"))


def plucked(t, mat, P):
    if mat in ("bill",):
        return P["bill"]
    if mat in ("leg", "toes", "hind_toe"):
        return P["leg"]
    if mat == "head" and (e := eye(t, 1, -1.1, -0.6, 0.3, iris=(30, 24, 24))):
        return e
    c = lerp(P["skin"], P["skin_dark"], 0.25 * t.n)
    return lerp(c, P["pore"], 0.6) if h(t.gx, t.gy, 9) < 0.15 else c


def woodpecker_colour(P):
    def col(mat, t):
        mat = {"cheek": "head", "rump": "body", "thigh": "body"}.get(mat, mat)
        if mat == "head":
            if (e := eye(t, 1.25, -1.15, -0.75, 0.35, iris=(40, 10, 12), ring=P["black"])):
                return e
            if t.face == "top":
                return P["black"] if t.w < 0.75 else P["red"]  # black crown, the male's red nape
            if t.face == "back":
                return P["red"] if t.v < 0.5 else P["black"]
            if t.face == "front" and abs(t.x) > t.box[3] / 2 - 0.4 and t.y > -1.0:
                return P["black"]  # the moustache wrapping round under the cheeks
            if t.vert:
                # white cheek and the black moustache stripe running back to the nape
                if t.side and abs(t.y - (-0.45)) < 0.3 and t.w > 0.25:
                    return P["black"]
                if t.side and t.y < -1.75 and t.w > 0.6:
                    return P["black"]
                return lerp(P["white"], P["buff"], 0.3 * t.n) if t.y > -1.9 else P["black"]
            return P["buff"]
        if mat == "bill":
            return lerp(P["bill"], P["black"], 0.3 * t.n)
        if mat in ("body", "neck"):
            if t.face == "front" and abs(abs(t.x) - (t.box[3] / 2 - 0.3)) < 0.3 and t.y < 0.3:
                return P["black"]  # the black stripe from the moustache down the side of the breast
            if t.face == "top" or (t.vert and t.y < -0.3 and t.face != "front"):
                if t.side and mat == "body" and t.w < 0.45 and t.y > -1.2:
                    return lerp(P["white"], P["buff"], 0.2 * t.n)  # the white shoulder patch
                return lerp(P["black"], P["black2"], 0.3 * t.n)
            if t.face == "back" and t.y > 0.5:
                return P["red"]  # red under the tail
            c = lerp(P["buff"], P["white"], 0.4 * t.n)
            return lerp(c, P["red"], 0.85) if mat == "body" and t.w > 0.75 and t.y > 0.4 else c
        if mat in ("breast", "belly"):
            if t.face == "front" and abs(abs(t.x) - (t.box[3] / 2 - 0.3)) < 0.3 and t.y < 0.3:
                return P["black"]
            c = lerp(P["buff"], P["white"], 0.5 * t.n)
            return lerp(c, P["red"], 0.85) if t.w > 0.7 else c
        if mat in ("wing", "primaries"):
            c = lerp(P["black"], P["black2"], 0.25 * t.n)
            # white bars of spots across the black flight feathers
            if t.side and (int((t.z if mat == "wing" else t.y) * 4) % 4 == 0) and int(t.gy) % 3 == 1:
                return lerp(P["white"], P["buff"], 0.2)
            if mat == "wing" and t.side and t.v < 0.45 and t.w < 0.5:
                return lerp(P["white"], P["buff"], 0.25 * t.n)  # scapular patch carries on to the wing
            return c
        if mat == "tail":
            c = lerp(P["black"], P["black2"], 0.3 * t.n)
            return lerp(c, P["white"], 0.7) if (abs(t.u - 0.5) > 0.38 and int(t.z * 4) % 3 == 0) else c
        if mat in ("leg", "toes", "hind_toe"):
            return lerp(P["leg"], P["black"], 0.3 * t.n)
        return P["black"]
    return col


def skylark_colour(P):
    def streaky(t, base):
        c = lerp(base, P["light"], 0.25 * h(t.gx, t.gy, t.salt))
        if t.gx % 2 == 0 and h(t.gx, t.gy // 3, t.salt + 2) < 0.3:
            c = lerp(c, P["dark"], 0.6)  # dark centres of the feathers: thin streaks
        return c

    def col(mat, t):
        mat = {"cheek": "head", "rump": "body", "thigh": "body"}.get(mat, mat)
        if mat == "head":
            if (e := eye(t, 1, -1.05, -0.6, 0.3, iris=(24, 18, 14), ring=P["belly"])):
                return e
            if t.side and abs(t.y + 0.6) < 0.2:
                return P["belly"]  # pale stripe over the eye
            if t.side and t.y > -0.7 and t.w > 0.45 and t.w < 0.85:
                return lerp(P["brown"], P["dark"], 0.2)  # brown ear coverts
            if t.y > -0.4:
                return lerp(P["belly"], P["light"], 0.3 * t.n)
            return streaky(t, P["brown"])
        if mat == "crest":
            return streaky(t, P["brown"])
        if mat == "bill":
            return lerp(P["bill"], P["dark"], 0.25 * t.n + 0.3 * (t.face == "top"))
        if mat in ("body", "neck"):
            if t.face == "bottom" or (t.vert and t.y > 0.35 and t.face != "back"):
                return lerp(P["belly"], P["light"], 0.2 * t.n)
            if t.face == "front" or (mat == "neck" and t.vert and t.y > -0.6):
                c = lerp(P["belly"], P["light"], 0.5)
                return lerp(c, P["streak"], 0.7) if t.gx % 2 == 0 and h(t.gx, t.gy // 2, t.salt) < 0.35 else c  # the streaked breast
            return lerp(feathers(t, P["brown"], P["light"], 3, 3, P["dark"], 0.3), streaky(t, P["brown"]), 0.6)
        if mat in ("breast",):
            c = lerp(P["belly"], P["light"], 0.6)
            return lerp(c, P["streak"], 0.7) if t.gx % 2 == 0 and h(t.gx, t.gy // 2, t.salt) < 0.4 else c
        if mat == "belly":
            return P["belly"]
        if mat in ("wing", "primaries"):
            c = wing_feathers(t, P["brown"], P["dark"], P["light"], mat == "primaries")
            if mat == "wing" and t.side and t.v > 0.85:
                return lerp(P["white"], P["light"], 0.3)  # the pale trailing edge
            return c
        if mat == "tail":
            c = lerp(P["dark"], P["brown"], 0.3 * t.n)
            return P["white"] if t.face in ("top", "bottom") and abs(t.u - 0.5) > 0.32 else c  # white outer tail feathers
        if mat in ("leg", "toes", "hind_toe"):
            return lerp(P["leg"], P["dark"], 0.2 * t.n)
        return P["brown"]
    return col


def lapwing_colour(P):
    def gloss(t):
        c = lerp(P["green"], P["green2"], 0.5 * h(t.gx, t.gy, t.salt))
        if h(t.gx // 2, t.gy // 2, t.salt + 4) < 0.18:
            c = lerp(c, P["purple"], 0.6)  # purple and bronze sheen in the green
        return c

    def col(mat, t):
        mat = {"cheek": "head", "rump": "body", "thigh": "body"}.get(mat, mat)
        if mat == "head":
            if (e := eye(t, 1.125, -1.15, -0.7, 0.3, iris=(18, 14, 12))):
                return e
            if t.face == "top" or t.y < -1.5:
                return lerp(P["black"], P["green"], 0.2 * t.n)  # black crown
            if t.face == "front" and t.y > -0.9:
                return P["black"]  # black throat
            if t.side:
                # white face with a black line under the eye
                if abs(t.y + 0.55) < 0.2 and t.w < 0.7:
                    return P["black"]
                return lerp(P["white"], P["grey"], 0.2 * t.n)
            return P["white"] if t.face != "back" else P["black"]
        if mat == "crest":
            return lerp(P["black"], P["green"], 0.3 * t.n)
        if mat == "bill":
            return P["bill"]
        if mat in ("neck",):
            return P["black"] if t.face != "back" else gloss(t)
        if mat in ("breast",):
            return P["black"]  # the black breast band
        if mat == "body":
            if t.face == "top":
                return feathers(t, gloss(t), P["green2"], 3, 3)
            if t.face == "front":
                return P["black"] if t.y < 0 else P["white"]
            if t.face == "back":
                return P["orange"] if t.y > 0 else gloss(t)  # rusty-orange under the tail
            if t.face == "bottom":
                return P["white"] if t.w < 0.8 else P["orange"]
            if t.y < -0.2:
                return feathers(t, gloss(t), P["green2"], 3, 3)
            if t.w < 0.18:
                return P["black"]
            return lerp(P["white"], P["grey"], 0.15 * t.n) if t.w < 0.8 else P["orange"]
        if mat == "belly":
            return P["white"]
        if mat in ("wing", "primaries"):
            c = feathers(t, gloss(t), P["green2"], 3, 3)
            if mat == "primaries" and t.side and t.v > 0.7:
                return lerp(P["black"], P["white"], 0.15)  # black wingtips (white-spotted when spread)
            return c
        if mat == "tail":
            if t.w < 0.55:
                return P["white"]
            return P["black"] if t.w < 0.92 else P["white"]  # white tail with a broad black band
        if mat in ("leg", "toes", "hind_toe"):
            return lerp(P["leg"], (120, 80, 80), 0.2 * t.n)
        return P["green"]
    return col


# ================================================================== TAWNY OWL / SEA EAGLE (ForestBirdModel parts)
def tawny_owl():
    m = Model("Tawny owl", 2, (64, 64))
    m.light = (1.12, 0.72, 0.15)
    tilt = -1.1
    # body upright (the tilt is baked into its rest pose), pivot placed so the belly sits on the legs
    m.part("body", "root", (0, 17.0, 0), (tilt, 0, 0))
    m.box("body", "body", -3, -3.5, -3.25, 6, 7, 6.5)
    m.box("body", "breast", -2.5, -2.5, -4, 5, 6, 1)
    m.box("body", "flank", -3.25, -2.5, -2.25, 6.5, 5.5, 5)
    m.part("neck", "body", (0, -1.5, -3), (-tilt, 0, 0))
    m.box("neck", "neck", -2, -1, -2, 4, 2, 4)
    m.part("head", "neck", (0, -1, 0))
    # a big round head, no ear tufts: the facial disc in front with its rim, the bill set low in it
    m.box("head", "head", -3.25, -5.5, -3, 6.5, 5.5, 5.5)
    m.box("head", "crown", -2.75, -6, -2.5, 5.5, 0.5, 4.5)
    m.box("head", "disc", -3, -5, -3.5, 6, 5, 0.5)
    m.box("head", "bill", -0.5, -2.75, -4, 1, 1.5, 0.5)
    m.box("head", "bill_hook", -0.5, -1.25, -3.75, 1, 0.5, 0.5)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_wing", "body", (side * 2.75, -3, -2.5))
        m.box(n + "_wing", "wing", 0 if side > 0 else -1, 0, 0, 1, 4.5, 6)
        m.part(n + "_wing_tip", n + "_wing", (0, 4.5, 0), (1.5708, 0, 0))
        m.box(n + "_wing_tip", "primaries", 0 if side > 0 else -1, 0, 0, 1, 7, 3)
    m.part("tail", "body", (0, -1.5, 3.25), (-0.2, 0, 0))
    m.box("tail", "tail", -2, 0, 0, 4, 1, 4)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_leg", "root", (side * 1.5, 21, 0.5))
        m.box(n + "_leg", "leg", -1, 0, -1, 2, 2, 2)                # feathered right down to the toes
        m.box(n + "_leg", "talons", -1, 2, -1.5, 2, 1, 2)
        m.box(n + "_leg", "claws", -1, 2.5, -2, 2, 0.5, 0.5)
    return m


OWL = dict(rufous=hexc("#8a5228"), dark=hexc("#3e2412"), light=hexc("#c08a52"), cream=hexc("#e2cca2"),
           disc=hexc("#b08050"), disc_rim=hexc("#4a2c16"), bill=hexc("#cfc8a0"), white=hexc("#ece4d0"), claw=hexc("#3a3430"))


def owl_colour(P):
    def mottled(t, base):
        c = lerp(base, P["light"], 0.35 * h(t.gx, t.gy, t.salt))
        k = h(t.gx, t.gy // 2, t.salt + 3)
        if k < 0.2:
            c = lerp(c, P["dark"], 0.75)  # dark shaft streaks
        elif k > 0.88:
            c = lerp(c, P["cream"], 0.6)
        return c

    def col(mat, t):
        if mat == "disc":
            # facial disc: pale browns in rings round each big dark eye, a dark rim, pale "eyebrows" meeting over the bill
            for ex in (-1.5, 1.5):
                d = math.hypot(t.x - ex, t.y + 2.9)
                if d < 0.9:
                    return (12, 8, 8, 0) if not (d < 0.45 and t.x - ex < 0 and t.y + 2.9 < 0) else (200, 200, 190, 0)
                if d < 1.15:
                    return (40, 22, 12, 0)
            if t.face == "front":
                if abs(t.x) < 0.5 and t.y < -2.7:
                    return P["cream"]
                if abs(abs(t.x) - 2.85) < 0.25 or t.y > -0.3:
                    return P["disc_rim"]
                return lerp(P["disc"], P["light"], 0.4 * h(t.gx, t.gy, 8) + 0.25 * (math.hypot(abs(t.x) - 1.5, t.y + 2.9) > 1.8))
            return mottled(t, P["rufous"])
        if mat in ("head", "crown", "neck"):
            if t.face == "front" and mat == "head":
                return P["disc_rim"]
            return mottled(t, P["rufous"])
        if mat in ("bill", "bill_hook"):
            return lerp(P["bill"], (120, 110, 80), 0.3 * (t.face != "front"))
        if mat in ("breast", "flank"):
            c = lerp(P["cream"], P["light"], 0.5 * h(t.gx, t.gy, t.salt))
            if int(t.gx) % 3 == 1 and h(t.gx, t.gy // 3, t.salt) < 0.7:
                c = lerp(c, P["dark"], 0.65)  # streaks with fine cross-bars on the pale breast
            elif t.gy % 3 == 0 and h(t.gx, t.gy, t.salt + 1) < 0.4:
                c = lerp(c, P["rufous"], 0.5)
            return c
        if mat == "body":
            if t.face in ("front", "bottom"):
                return col("breast", t)
            return feathers(t, mottled(t, P["rufous"]), P["dark"], 3, 2)
        if mat in ("wing", "primaries"):
            c = lerp(wing_feathers(t, P["rufous"], P["dark"], P["light"], mat == "primaries"), mottled(t, P["rufous"]), 0.45)
            if mat == "wing" and t.side and t.v < 0.25 and int(t.gx) % 4 == 0 and h(t.gx, t.gy, 3) < 0.6:
                return lerp(P["white"], P["cream"], 0.4)  # the row of pale spots on the shoulder ("braces")
            if mat == "primaries" and t.side and int(t.y * 2) % 3 == 0:
                return lerp(c, P["dark"], 0.35)  # barred flight feathers
            return c
        if mat == "tail":
            c = mottled(t, P["rufous"])
            return lerp(c, P["dark"], 0.6) if int(t.z * 2) % 3 == 0 else c
        if mat in ("leg", "talons"):
            return lerp(P["cream"], P["light"], 0.4 * t.n)
        if mat == "claws":
            return P["claw"]
        return P["rufous"]
    return col


def sea_eagle():
    m = Model("White-tailed sea eagle", 2, (128, 64))
    m.light = (1.12, 0.72, 0.15)
    tilt = -0.45
    m.part("body", "root", (0, 15.6, 0), (tilt, 0, 0))
    m.box("body", "body", -3.5, -3, -5.5, 7, 6, 11)
    m.box("body", "breast", -3, -2.5, -6.25, 6, 5, 1)
    m.box("body", "trousers", -3, 2.5, -2.5, 6, 1.5, 4)       # shaggy feathered thighs
    m.part("neck", "body", (0, -2, -4.5), (-tilt, 0, 0))
    m.box("neck", "neck", -2, -2.5, -2, 4, 3.5, 4)
    m.box("neck", "hackles", -2.25, -1.5, -1, 4.5, 2.5, 3)
    m.part("head", "neck", (0, -2.25, 0))
    m.box("head", "head", -2, -3.5, -3.5, 4, 4, 5)
    m.box("head", "brow", -2.25, -3.25, -3.25, 4.5, 1, 2)        # the jutting brow over the eye
    # the massive yellow bill: deep at the base, the cere, a hooked tip
    m.box("head", "cere", -1.25, -2.75, -4.5, 2.5, 2, 1)
    m.box("head", "bill", -1, -2.5, -6.5, 2, 2, 2)
    m.box("head", "bill_hook", -0.75, -0.5, -6.5, 1.5, 1, 1)
    m.box("head", "lower_bill", -0.75, -0.5, -5.5, 1.5, 0.5, 1.5)
    for side, n in ((1, "left"), (-1, "right")):
        # broad, plank-like wings ("a flying barn door"), long fingered primaries
        m.part(n + "_wing", "body", (side * 3.25, -2.5, -4.5))
        m.box(n + "_wing", "wing", 0 if side > 0 else -1, 0, 0, 1, 6, 10)
        m.part(n + "_wing_tip", n + "_wing", (0, 6, 0), (1.5708, 0, 0))
        m.box(n + "_wing_tip", "primaries", 0 if side > 0 else -1, 0, 0, 1, 11, 5)
    # a short, wedge-shaped white tail
    m.part("tail", "body", (0, -2, 5), (-0.15, 0, 0))
    m.box("tail", "tail", -2.5, 0, 0, 5, 1, 4.5)
    m.box("tail", "tail_wedge", -1.5, 0, 4.5, 3, 1, 1)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_leg", "root", (side * 1.75, 19.5, 0.5))
        m.box(n + "_leg", "leg", -0.75, 0, -0.75, 1.5, 3.5, 1.5)    # bare yellow tarsus
        m.box(n + "_leg", "feet", -1.5, 3.5, -2.5, 3, 1, 3.5)
        m.box(n + "_leg", "claws", -1.5, 3.75, -3, 3, 0.5, 0.5)
    return m


EAGLE = dict(brown=hexc("#5a4030"), dark=hexc("#2a1e16"), light=hexc("#8a6a4c"), head=hexc("#b09468"),
             head_dark=hexc("#6e5a3e"), bill=hexc("#e8c040"), bill_dark=hexc("#a07820"), white=hexc("#f0ece4"),
             leg=hexc("#e0b830"), claw=hexc("#1a1614"))


def eagle_colour(P):
    def col(mat, t):
        if mat in ("head", "brow"):
            if mat == "head" and (e := eye(t, 2, -2.2, -2.2, 0.55, iris=(200, 150, 40), ring=(60, 40, 20))):
                if e[:3] == (200, 150, 40) and math.hypot(t.z + 2.2, t.y + 2.2) < 0.3:
                    return (10, 8, 6, 0)
                return e
            c = fur(t, P["head"], P["head_dark"], P["white"], streak=2)
            return lerp(c, P["head_dark"], 0.35) if mat == "brow" and t.face == "bottom" else c
        if mat in ("neck", "hackles"):
            # the pale head fades into the brown body down the neck
            c = fur(t, P["head"], P["head_dark"], P["white"], streak=2)
            return lerp(c, P["light"], 0.6 * t.v)
        if mat in ("cere", "bill", "bill_hook", "lower_bill"):
            c = lerp(P["bill"], P["bill_dark"], 0.25 * t.n + (0.25 if mat in ("bill_hook", "lower_bill") else 0))
            if mat == "cere" and t.side and near(t.y, -2.0, 0.3) and near(t.z, -4.1, 0.3):
                return (40, 30, 10, 0)  # nostril
            if mat == "bill" and t.side and t.y > -0.75:
                return lerp(P["bill_dark"], (60, 40, 10), 0.4)  # the gape line
            return c
        if mat in ("body", "breast", "trousers"):
            c = feathers(t, lerp(P["brown"], P["dark"], 0.3 * h(t.gx, t.gy, t.salt)), P["light"], 3, 2)
            if t.face == "top" or (t.vert and t.y < -1.5):
                c = lerp(c, P["light"], 0.2)
            return c
        if mat in ("wing", "primaries"):
            c = wing_feathers(t, P["brown"], P["dark"], P["light"], mat == "primaries")
            if mat == "wing" and t.v < 0.45:
                c = lerp(c, P["light"], 0.25)  # paler coverts
            if mat == "primaries":
                c = lerp(c, P["dark"], 0.45)
            return c
        if mat in ("tail", "tail_wedge"):
            return lerp(P["white"], P["head"], 0.25 * t.n + (0.4 if t.w < 0.15 and mat == "tail" else 0))
        if mat in ("leg", "feet"):
            c = lerp(P["leg"], P["bill_dark"], 0.3 * t.n)
            return lerp(c, P["bill_dark"], 0.4) if mat == "feet" and int(t.x * 2) % 2 == 0 and t.face == "front" else c
        if mat == "claws":
            return P["claw"]
        return P["brown"]
    return col


# ================================================================== BITTERN (WaderModel parts, ground y 24)
def bittern():
    m = Model("Bittern", 4, (64, 64))
    m.light = (1.12, 0.72, 0.15)
    # a stocky, hunched heron on short legs, a thick neck clothed in long feathers, the dagger bill
    m.part("body", "root", (0, 14.5, 0), (-0.35, 0, 0))
    m.box("body", "body", -2.5, -2.5, -4.5, 5, 5.5, 9)
    m.box("body", "breast", -2, -2, -5.25, 4, 5, 1)
    m.part("neck", "body", (0, -1.5, -4), (0.45, 0, 0))
    m.box("neck", "neck", -1.5, -3.5, -1.5, 3, 4.5, 3)
    m.box("neck", "ruff", -1.75, -3, -2.25, 3.5, 4, 1.25)       # long loose feathers down the front of the neck
    m.part("neck2", "neck", (0, -3.5, 0), (-0.6, 0, 0))
    m.box("neck2", "neck2", -1.25, -3, -1.25, 2.5, 3.5, 2.5)
    m.part("head", "neck2", (0, -3, 0), (0.15, 0, 0))
    m.box("head", "head", -1.25, -1.75, -1.75, 2.5, 2.25, 3.25)
    m.box("head", "bill", -0.5, -0.75, -5.25, 1, 1, 3.5)
    m.box("head", "lower_bill", -0.375, 0.25, -4.75, 0.75, 0.25, 3)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_wing", "body", (side * 2.5, -2, -3.5))
        m.box(n + "_wing", "wing", 0 if side > 0 else -1, 0, 0, 1, 4.5, 8)
        m.part(n + "_wing_tip", n + "_wing", (0, 4.5, 0), (1.5708, 0, 0))
        m.box(n + "_wing_tip", "primaries", 0 if side > 0 else -1, 0, 0, 1, 6, 3)
    m.part("tail", "body", (0, -1.5, 4), (-0.25, 0, 0))
    m.box("tail", "tail", -1.5, 0, 0, 3, 1, 2.5)
    for side, n in ((1, "left"), (-1, "right")):
        m.part(n + "_leg", "root", (side * 1.25, 17.5, 0.5), (0.15, 0, 0))
        m.box(n + "_leg", "thigh", -0.75, -1, -0.75, 1.5, 3, 1.5)
        m.part(n + "_shin", n + "_leg", (0, 2, 0), (-0.15, 0, 0))
        m.box(n + "_shin", "leg", -0.5, 0, -0.5, 1, 4, 1)
        m.box(n + "_shin", "toes", -0.25, 4, -3.5, 0.5, 0.5, 4)
        m.part(n + "_toe_l", n + "_shin", (0.25, 4.25, 0), (0, -0.5, 0))
        m.box(n + "_toe_l", "toes", -0.25, -0.25, -3, 0.5, 0.5, 3)
        m.part(n + "_toe_r", n + "_shin", (-0.25, 4.25, 0), (0, 0.5, 0))
        m.box(n + "_toe_r", "toes", -0.25, -0.25, -3, 0.5, 0.5, 3)
        m.box(n + "_shin", "hind_toe", -0.25, 4, 0.5, 0.5, 0.5, 1.5)
    return m


BITTERN = dict(buff=hexc("#b8945c"), dark=hexc("#2e2214"), mid=hexc("#7a5a32"), light=hexc("#dcc08a"),
               crown=hexc("#1e1a14"), bill=hexc("#c8c070"), bill_dark=hexc("#6a6a3a"), leg=hexc("#9aa060"), white=hexc("#e8dcc0"))


def bittern_colour(P):
    def streaked(t, base, bars=True):
        c = lerp(base, P["light"], 0.3 * h(t.gx, t.gy, t.salt))
        k = h(t.gx, t.gy // 2, t.salt + 2)
        if bars and (t.gy + int(3 * h(t.gx // 3, 7, t.salt))) % 3 == 0 and h(t.gx // 2, t.gy, t.salt) < 0.4:
            c = lerp(c, P["dark"], 0.45)  # fine dark vermiculation
        if k < 0.18:
            c = lerp(c, P["dark"], 0.7)
        return c

    def col(mat, t):
        if mat == "head":
            if (e := eye(t, 1.25, -0.9, -1.0, 0.4, iris=(200, 170, 50), ring=(90, 80, 40))):
                return e
            if t.face == "top" or t.y < -1.3:
                return lerp(P["crown"], P["mid"], 0.2 * t.n)  # black crown
            if t.side and t.y > -0.3 and t.w < 0.55:
                return P["crown"]  # the black moustache stripe
            return streaked(t, P["buff"], False)
        if mat in ("bill", "lower_bill"):
            c = lerp(P["bill"], P["bill_dark"], 0.25 * t.n + 0.4 * (t.face == "top"))
            return c
        if mat in ("neck", "neck2", "ruff"):
            c = streaked(t, P["buff"], False)
            if t.face == "front" or mat == "ruff":
                c = lerp(P["white"], P["buff"], 0.4)
                if t.gx % 3 == 0 and h(t.gx, t.gy // 3, t.salt) < 0.65:
                    c = lerp(c, P["mid"], 0.75)  # bold brown streaks down the fore-neck, broken
                if mat == "ruff" and t.vert and t.v > 0.8 and h(t.gx, 0, t.salt) < 0.5:
                    return None
            return c
        if mat in ("body", "breast"):
            if t.face == "top" or (t.vert and t.y < -1 and t.face != "front"):
                c = streaked(t, P["mid"])
                return lerp(c, P["dark"], 0.5) if h(t.gx // 2, t.gy // 2, t.salt + 9) < 0.3 else c
            c = streaked(t, P["buff"])
            if t.face == "front" or mat == "breast":
                c = lerp(P["light"], P["buff"], 0.4)
                if t.gx % 3 == 0 and h(t.gx, t.gy // 3, t.salt) < 0.6:
                    c = lerp(c, P["mid"], 0.65)
            return c
        if mat in ("wing", "primaries"):
            c = lerp(wing_feathers(t, P["buff"], P["mid"], P["light"], mat == "primaries"), streaked(t, P["buff"]), 0.5)
            if mat == "primaries":
                c = lerp(c, P["mid"], 0.4)
                if int(t.y * 2) % 3 == 0:
                    c = lerp(c, P["dark"], 0.6)  # barred flight feathers
            elif t.v < 0.4:
                c = lerp(c, P["mid"], 0.4)
            return c
        if mat == "tail":
            return streaked(t, P["mid"])
        if mat == "thigh":
            return streaked(t, P["buff"], False)
        if mat in ("leg", "toes", "hind_toe"):
            return lerp(P["leg"], P["bill_dark"], 0.3 * t.n)
        return P["buff"]
    return col


BITTERN_PLUCKED = dict(PLUCKED)


def bittern_plucked(P):
    def col(mat, t):
        if mat in ("bill", "lower_bill"):
            return hexc("#c8c070")
        if mat in ("leg", "toes", "hind_toe"):
            return hexc("#9aa060")
        if mat == "head" and (e := eye(t, 1.25, -0.9, -1.0, 0.4, iris=(60, 50, 30))):
            return e
        c = lerp(P["skin"], P["skin_dark"], 0.25 * t.n)
        return lerp(c, P["pore"], 0.6) if h(t.gx, t.gy, 9) < 0.15 else c
    return col


def plucked_colour(P, base_col, eye_args):
    def col(mat, t):
        if mat in ("bill", "leg", "toes", "hind_toe", "crest"):
            return base_col(mat, t) if mat != "crest" else None
        if mat == "head" and (e := eye(t, *eye_args)):
            return e
        c = lerp(P["skin"], P["skin_dark"], 0.25 * t.n)
        return lerp(c, P["pore"], 0.6) if h(t.gx, t.gy, 9) < 0.15 else c
    return col


# ================================================================== EEL (EelModel parts)
def eel():
    m = Model("European eel", 4, (64, 64))
    m.light = (1.15, 0.8, 0.0)
    # a long round body in four segments, a narrow pointed head with a jutting lower jaw, small pectoral fins and one
    # continuous fin running round the tail
    m.part("body", "root", (0, 22.5, -4))
    m.box("body", "body", -1, -1, -2, 2, 2, 6)
    m.box("body", "head", -0.875, -0.875, -4, 1.75, 1.5, 2)
    m.box("body", "snout", -0.625, -0.75, -5, 1.25, 1, 1)
    m.box("body", "jaw", -0.5, 0.25, -5.25, 1, 0.5, 1.75)
    for side in (1, -1):
        m.box("body", "pectoral", (1 if side > 0 else -1.75), -0.25, -1.75, 0.75, 1, 0.25)
    m.part("segment1", "body", (0, 0, 4))
    m.box("segment1", "body", -1, -1, 0, 2, 2, 6)
    m.box("segment1", "fin", -0.125, -1.75, 1.5, 0.25, 0.75, 4.5)
    m.part("segment2", "segment1", (0, 0, 6))
    m.box("segment2", "body", -0.75, -0.875, 0, 1.5, 1.75, 6)
    m.box("segment2", "fin", -0.125, -1.75, 0, 0.25, 1, 6)
    m.box("segment2", "fin", -0.125, 0.875, 1, 0.25, 0.75, 5)
    m.part("segment3", "segment2", (0, 0, 6))
    m.box("segment3", "body", -0.5, -0.75, 0, 1, 1.5, 5)
    m.box("segment3", "fin", -0.125, -1.75, 0, 0.25, 3.5, 6.5)
    return m


EEL = dict(back=hexc("#2e3020"), back2=hexc("#46482a"), belly=hexc("#c8b860"), belly2=hexc("#8a7a3a"), fin=hexc("#5a5434"))


def eel_colour(P):
    def col(mat, t):
        if mat in ("body", "head", "snout"):
            if mat == "head" and (e := eye(t, 0.875, -0.3, -3.4, 0.28, iris=(20, 18, 12), ring=(200, 180, 90))):
                return e
            # dark olive back shading into the yellow belly, a faint lateral line
            k = (t.y - t.box[1]) / t.box[4]
            c = lerp(lerp(P["back"], P["back2"], 0.4 * t.n), lerp(P["belly"], P["belly2"], 0.4 * t.n), max(0, (k - 0.45) * 2.2))
            if t.side and abs(k - 0.48) < 0.06:
                c = lerp(c, P["back2"], 0.5)
            if t.face == "bottom":
                c = lerp(P["belly"], P["belly2"], 0.3 * t.n)
            return c
        if mat == "jaw":
            return lerp(P["belly2"], P["back"], 0.3)
        if mat in ("fin", "pectoral"):
            c = lerp(P["fin"], P["back"], 0.4 * t.n)
            return c if not (t.vert and h(t.gx, t.gy, 4) < 0.08) else None
        return P["back"]
    return col


# ------------------------------------------------------------------ output
def main():
    T = "wildlife/temperate/"
    jobs = [  # model, java method, [(texture path under entity/, colour fn)]
        (wisent(), "wisent", [(T + "wisent_bull", wisent_colour(WISENT_BULL)), (T + "wisent_cow", wisent_colour(WISENT_COW)),
                              (T + "wisent_calf", wisent_colour(WISENT_CALF))]),
        (pond_turtle(), "pondTurtle", [(T + "pond_turtle", turtle_colour(TURTLE))]),
        (water_vole(), "waterVole", [(T + "water_vole", vole_colour(VOLE))]),
        (stag_beetle(), "stagBeetle", [(T + "stag_beetle", beetle_colour(BEETLE_M)), (T + "stag_beetle_female", beetle_colour(BEETLE_F))]),
        (woodpecker(), "woodpecker", [("wildlife/woodpecker", woodpecker_colour(WOODPECKER))]),
        (skylark(), "skylark", [("wildlife/skylark", skylark_colour(SKYLARK))]),
        (lapwing(), "lapwing", [("wildlife/lapwing", lapwing_colour(LAPWING))]),
        (tawny_owl(), "tawnyOwl", [("wildlife/tawny_owl", owl_colour(OWL))]),
        (sea_eagle(), "seaEagle", [("wildlife/sea_eagle", eagle_colour(EAGLE))]),
        (bittern(), "bittern", [("wildlife/bittern", bittern_colour(BITTERN))]),
        (eel(), "eel", [(T + "eel", eel_colour(EEL))]),
    ]
    skinned = {  # the carcass skins (CarcassRenderer: textures/entity/skinned/<same path>)
        "wisent_bull": wisent_colour(dict(WISENT_BULL, **FLESH)), "wisent_cow": wisent_colour(dict(WISENT_COW, **FLESH)),
        "wisent_calf": wisent_colour(dict(WISENT_CALF, **FLESH)),
        "pond_turtle": turtle_colour(TURTLE_SKINNED), "water_vole": vole_colour(VOLE_SKINNED),
        "skylark": plucked_colour(PLUCKED, skylark_colour(SKYLARK), (1, -1.05, -0.6, 0.3)),
        "lapwing": plucked_colour(PLUCKED, lapwing_colour(LAPWING), (1.125, -1.15, -0.7, 0.3)),
        "bittern": bittern_plucked(PLUCKED),
    }
    methods = []
    for model, method, skins in jobs:
        methods.append(model.java(method))
        for path, colour in skins:
            out = RES / (path + ".png")
            out.parent.mkdir(parents=True, exist_ok=True)
            model.paint(colour).save(out)
            name = path.rsplit("/", 1)[1]
            if name in skinned:
                s = RES / "skinned" / (path + ".png")
                s.parent.mkdir(parents=True, exist_ok=True)
                model.paint(skinned[name]).save(s)
        print(f"{model.name}: {len(model.parts)} parts, {len(model.boxes)} boxes")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.temperate;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * GENERATED by scripts/gen_temperate_models.py (with the skins it paints for them): the temperate animals' own models.
 * Each keeps the part names of the model class that animates it (TemperateClient). Do not edit.
 */
public final class TemperateGeometry {{
{chr(10).join(methods)}
    private TemperateGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
