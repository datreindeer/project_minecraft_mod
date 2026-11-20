"""Skins for the Stone Age tribespeople (mechanical:tribesperson): 64x64 player-format skins, 4 per role and climate
(0, 2: men with wide arms; 1, 3: women with slim arms), painted pixel by pixel: hide and fur clothing, bone and
tooth necklaces, braids, beards, war paint. Each climate (people/ClimateZone.java) dresses its own way on top of the role:
cold: fur hoods, long hide coats with fur trim, fur trousers and mittens; temperate: hide and leather, woven aprons;
warm: light linen wraps and kilts, head cloths, sandals; jungle: loincloths and grass skirts, feather headdresses, paint.
Temperate skins go in tribesperson/, the others in tribesperson/<climate>/. Next to every skin a "_tint" mask marks the
dyed parts (headbands, sashes, coat bands, loincloths) in grey; client/people/TintedSkin multiplies them by the tribe's
colour. Run: python scripts/gen_tribe_skins.py [preview_dir]"""
import random
import sys
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/tribesperson"

# ---- player skin layout: part -> (u, v, w, h, d); slim arms are 3 wide ----
BASE = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "rarm": (40, 16, 4, 12, 4), "larm": (32, 48, 4, 12, 4),
        "rleg": (0, 16, 4, 12, 4), "lleg": (16, 48, 4, 12, 4)}
OVER = {"head": (32, 0), "body": (16, 32), "rarm": (40, 32), "larm": (48, 48), "rleg": (0, 32), "lleg": (0, 48)}


def faces(u, v, w, h, d):
    """face -> (x0, y0, fw, fh) in the texture."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, h),
            "front": (u + d, v + d, w, h), "left": (u + d + w, v + d, d, h), "back": (u + d + w + d, v + d, w, h)}


def clamp(c):
    return tuple(max(0, min(255, int(round(x)))) for x in c[:3])


def mix(a, b, t):
    return tuple(a[i] + (b[i] - a[i]) * t for i in range(3))


def shade(c, f):
    return tuple(x * f for x in c[:3])


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


SKIN = {"pale": hexc("e6bf9f"), "light": hexc("d6a37c"), "tan": hexc("bf8a5f"), "olive": hexc("ad7f53"),
        "brown": hexc("8c5d3c"), "dark": hexc("62402a")}
HAIR = {"black": hexc("1e1814"), "darkbrown": hexc("3b2617"), "brown": hexc("5e3c22"), "auburn": hexc("7a3a1c"),
        "blond": hexc("a07c48"), "grey": hexc("8f8b85"), "white": hexc("cfcac2")}
EYES = {"brown": hexc("4a2e1a"), "dark": hexc("2a1c12"), "blue": hexc("3e6a8a"), "green": hexc("4e6a3a"), "grey": hexc("6a7278")}
HIDE = hexc("a87e52")        # tanned hide
HIDE_LIGHT = hexc("c9ad80")
HIDE_DARK = hexc("845a36")
HIDE_RED = hexc("9a5532")    # ochre-rubbed hide
HIDE_GREY = hexc("a39784")   # smoked hide
LEATHER = hexc("5a3822")     # dark leather
FUR_BROWN = hexc("6e4a2c")
FUR_GREY = hexc("8a8680")
FUR_WHITE = hexc("ddd5c4")
FUR_RED = hexc("8a4a24")     # fox
STRAW = hexc("b39b5c")
BARK = hexc("7a5c40")
CORD = hexc("8e7550")
BONE = hexc("e8dfc8")
TOOTH = hexc("f4efe2")
OCHRE = hexc("9c3420")
CHARCOAL = hexc("2a2622")
BLUE_CLAY = hexc("4a6a86")
AMBER = hexc("c8862a")
LINEN = hexc("d9cba8")       # pale woven plant fibre (warm climates)
LEAF = hexc("6f8a3c")        # grass/leaf skirts and aprons (jungle)
WHITE_CLAY = hexc("e4ddd0")
FEATHERS = [hexc("c43a2a"), hexc("2f8a4a"), hexc("2e5fa8"), hexc("e0b830"), hexc("f0ece2"), hexc("3a3030")]


class Dye(tuple):
    """The tribe's colour: painted in this stand-in colour and marked in the tint mask."""


DYE = Dye(hexc("b04a30"))
ZONES = ["cold", "temperate", "warm", "jungle"]


class Skin:
    """Paints one skin. Each pixel of a part face asks the layers, bottom to top; the first overlay hit goes on the
    outer (hat/jacket/sleeve/pants) layer instead."""

    def __init__(self, seed, slim):
        self.r = random.Random(seed)
        self.slim = slim
        self.base = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.mask = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.dyed = None   # set by Look.tint while a pixel is in the tribe's colour: its brightness
        self.noise = {}

    def n(self, key, amt):
        """Stable per-pixel noise in [-amt, amt]."""
        if key not in self.noise:
            self.noise[key] = self.r.uniform(-1, 1)
        return self.noise[key] * amt

    def dims(self, part):
        u, v, w, h, d = BASE[part]
        if self.slim and part in ("rarm", "larm"):
            w = 3
        return u, v, w, h, d

    def each(self, part, overlay=False):
        """Yields (face, x, y, fw, fh, tx, ty) for every pixel of a part (or its overlay)."""
        u, v, w, h, d = self.dims(part)
        if overlay:
            u, v = OVER[part]
        for face, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    yield face, x, y, fw, fh, x0 + x, y0 + y

    def put(self, tx, ty, c):
        self.base.putpixel((tx, ty), clamp(c) + (255,))
        if self.dyed is None:
            self.mask.putpixel((tx, ty), (0, 0, 0, 0))
        else:
            g = max(0, min(255, round(235 * self.dyed)))
            self.mask.putpixel((tx, ty), (g, g, g, 255))
        self.dyed = None

    def darken(self, tx, ty, f):
        """Shades a painted pixel (and its mask grey, if dyed)."""
        self.base.putpixel((tx, ty), clamp(shade(self.base.getpixel((tx, ty)), f)) + (255,))
        m = self.mask.getpixel((tx, ty))
        if m[3]:
            g = max(0, min(255, round(m[0] * f)))
            self.mask.putpixel((tx, ty), (g, g, g, 255))

    def build(self, look):
        look.s = self
        for part in BASE:
            kinds = {}
            for face, x, y, fw, fh, tx, ty in self.each(part):
                self.kind = "cloth"
                self.dyed = None
                self.put(tx, ty, look.base(self, part, face, x, y, fw, fh))
                kinds[(face, x, y)] = (self.kind, tx, ty, fw, fh)
            # sewn edges: garment pixels next to bare skin get a darker rim
            if part != "head":
                for (face, x, y), (k, tx, ty, fw, fh) in kinds.items():
                    if k != "cloth" or face in ("top", "bottom"):
                        continue
                    nb = [kinds.get((face, x + dx, y + dy), ("", 0, 0, 0, 0))[0] for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))]
                    if "skin" in nb:
                        self.darken(tx, ty, 0.8)
            over = {}
            for face, x, y, fw, fh, tx, ty in self.each(part, overlay=True):
                self.dyed = None
                c = look.over(self, part, face, x, y, fw, fh)
                if c is not None:
                    self.put(tx, ty, c)
                    over[(face, x, y)] = (tx, ty)
            # outer layer: the lower edge of fur, hair and beads is in shadow
            for (face, x, y), (tx, ty) in over.items():
                if face not in ("top", "bottom") and (face, x, y + 1) not in over:
                    self.darken(tx, ty, 0.82)
        return self.base


def gy(part, face, y, fh):
    """Global height from the shoulders down: body/arms 0-11, legs 12-23; tops -1, bottoms one past the end."""
    if face == "top":
        y = -1
    elif face == "bottom":
        y = fh
    return y + (12 if part in ("rleg", "lleg") else 0)


def side_shade(face):
    return {"front": 1.0, "top": 1.06, "right": 0.9, "left": 0.9, "back": 0.86, "bottom": 0.78}[face]


class Look:
    """One person's appearance. Fields are set by the role builders below."""

    def __init__(self, r, woman, role):
        self.r = r
        self.woman = woman
        self.role = role
        self.skin = SKIN["tan"]
        self.hair = HAIR["brown"]
        self.eyes = EYES["brown"]
        self.hair_style = "short"      # short, long, braids, bun, shaggy, bald_top
        self.beard = None              # None, stubble, short, full, long
        self.paint = None              # war paint: (colour, style)
        self.headband = None           # colour
        self.feather = False
        self.wrinkles = False
        # clothing
        self.top = HIDE                # tunic colour, or None for bare chest
        self.top_kind = "tunic"        # tunic, vest (sleeveless), wrap (one shoulder), robe
        self.sleeves = 0               # rows of sleeve on the arms (0 bare, 4 short, 12 long)
        self.fur = None                # fur colour for cape/shoulders
        self.fur_kind = None           # shoulders, cape, collar
        self.belt = LEATHER
        self.skirt_to = 14             # tunic hem (global row)
        self.legs = HIDE               # leggings colour or None (bare legs)
        self.legs_to = 22              # leggings end row (rest: wraps/boots)
        self.boots = FUR_BROWN
        self.wraps = CORD              # leg/arm wraps
        self.bracers = None
        self.necklace = None           # bone, teeth, fishbone, amber, shell
        self.apron = None              # woven apron colour (gatherers)
        self.strap = None              # diagonal strap (quiver / bag)
        self.quiver = False
        self.pouch = None
        self.trim = None               # fringe/edge colour
        self.top_mat = "hide"          # what the top is made of: hide, fur, weave
        self.skirt = None              # loincloth / kilt / grass skirt from the belt down (colour), drawn over everything
        self.skirt_mat = "hide"
        self.hood = None               # fur hood (colour) over the head; hood_trim rims the face
        self.hood_trim = None
        self.headwrap = None           # cloth wound round the head (colour)
        self.headdress = None          # feathers on the crown and down the back (list of colours)
        self.band = None               # a band across the coat and round the cuffs (colour)
        self.mittens = None            # fur mittens (colour)
        self.armband = None            # a band round the upper arms (colour)
        self.body_paint = None         # dots and bars on bare skin (colour)
        self.barefoot = False
        self.s = None

    def tint(self, col, f):
        """col shaded by f; the tribe's colour (DYE) also goes into the tint mask."""
        self.s.dyed = f if isinstance(col, Dye) else None
        return shade(col, f)

    def sh(self, c, f):
        """Shades a pixel already painted (keeps a dyed pixel's mask in step)."""
        if self.s.dyed is not None:
            self.s.dyed *= f
        return shade(c, f)

    # ---------- base layer ----------
    def skin_px(self, s, part, face, x, y, fw, fh):
        c = shade(self.skin, side_shade(face))
        return tuple(v + s.n((part, face, x, y, "sk"), 5) for v in c)

    def base(self, s, part, face, x, y, fw, fh):
        if part == "head":
            return self.head(s, face, x, y, fw, fh)
        Y = gy(part, face, y, fh)
        c = self.skin_px(s, part, face, x, y, fw, fh)
        # muscle/knee shading
        if part in ("rarm", "larm") and face in ("front", "right", "left", "back") and y in (4, 5):
            c = shade(c, 0.95)
        cloth = self.cloth(s, part, face, x, y, fw, fh, Y)
        if cloth is None:
            s.kind = "skin"
        return cloth if cloth is not None else c

    def material(self, s, col, kind, key, x, y, face):
        """A pixel of a material, with texture."""
        f = side_shade(face)
        if kind == "fur":
            # streaky fur: vertical tufts, light tips
            streak = s.n(("fs",) + key[:3] + (x,), 0.12) + s.n(key + ("f",), 0.08)
            tip = 1.08 if (y + x * 3) % 4 == 0 else 1.0
            return self.tint(col, f * (1 + streak) * tip)
        if kind == "weave":
            # woven grass/bark: checker of light and dark strands
            k = 1.1 if (x + y) % 2 == 0 else 0.88
            return self.tint(col, f * k * (1 + s.n(key + ("w",), 0.05)))
        if kind == "cord":
            k = 1.12 if (x + y) % 2 == 0 else 0.84
            return self.tint(col, f * k)
        # hide/leather: soft mottling and the odd darker patch
        m = s.n(key + ("h",), 0.06) + (s.n(("patch",) + key[:2] + (x // 2, y // 3), 0.05))
        return self.tint(col, f * (1 + m))

    def cloth(self, s, part, face, x, y, fw, fh, Y):
        key = (part, face, x, y)
        arm = part in ("rarm", "larm")
        leg = part in ("rleg", "lleg")
        tm = self.top_mat
        if face == "top" and part == "body":
            return None if self.top is None else self.material(s, self.top, tm, key, x, y, face)
        if face == "bottom" and leg:
            if self.barefoot:
                return shade(self.skin, 0.8)
            return self.material(s, self.boots or LEATHER, "fur", key, x, y, face)
        if arm:
            if self.mittens and (Y >= 11 or face == "bottom"):
                return self.material(s, self.mittens, "fur", key, x, y, face)
            if self.bracers and 8 <= Y <= 10 and face != "top":
                c = self.material(s, self.bracers, "hide", key, x, y, face)
                return self.sh(c, 0.8) if Y == 8 else c
            if self.wraps and Y == 9 and not self.bracers and face != "top":
                return self.material(s, self.wraps, "cord", key, x, y, face)
            if self.band and self.sleeves >= 12 and Y == 10 and face != "top":
                return self.material(s, self.band, "weave", key, x, y, face)  # dyed cuff
            if self.top is not None and (Y < self.sleeves or (face == "top" and self.sleeves)):
                c = self.material(s, self.top, tm, key, x, y, face)
                return self.sh(c, 0.82) if Y == self.sleeves - 1 else c
            if self.armband and Y in (2, 3) and face != "top":
                return self.material(s, self.armband, "cord" if Y == 3 else "hide", key, x, y, face)
            if face == "bottom":  # hands
                return shade(self.skin, 0.85)
            if self.body_paint and Y in (5, 6) and face in ("front", "right", "left") and (x + Y) % 2 == 0:
                return shade(self.body_paint, side_shade(face))
            return None
        if part == "body":
            if self.top is None:
                c = None
            elif self.top_kind == "vest" and face == "front" and 2 <= x <= 5 and Y < 7 - abs(x * 2 - 7) // 2:
                c = None  # open neck of the vest
            elif self.top_kind == "wrap" and face in ("front", "back") and Y < 5:
                # one-shoulder wrap: over the left shoulder only (texture x: front left side is x>=4)
                edge = (x - 4 + Y) if face == "front" else (3 - x + Y)
                c = self.material(s, self.top, tm, key, x, y, face) if edge >= 0 else None
            else:
                c = self.material(s, self.top, tm, key, x, y, face)
                if face == "front" and Y == 0 and 2 <= x <= 5 and self.top_kind != "robe":
                    c = None  # neckline
                if c is not None and face == "front" and self.top_kind == "robe" and x == 3 and 3 <= Y:
                    c = self.sh(c, 0.78)  # robe overlap seam
            if c is None:
                s.kind = "skin"
                s.dyed = None
                c = self.skin_px(s, part, face, x, y, fw, fh)
                if face == "front" and Y in (3, 4) and x in (1, 2, 5, 6):  # chest shading
                    c = shade(c, 0.94)
                if self.body_paint and face in ("front", "back") and ((Y == 2 and x in (1, 6)) or (Y == 6 and 2 <= x <= 5 and x % 2 == 0)):
                    c = shade(self.body_paint, side_shade(face))
            if self.band and self.top is not None and Y == 5 and face in ("front", "back", "left", "right"):
                c = self.material(s, self.band, "weave", key, x, y, face)  # dyed band across the coat
            if self.skirt and Y >= 9:
                c = self.material(s, self.skirt, self.skirt_mat, key, x, y, face)
            if self.apron and face == "front" and 8 <= Y <= 11 and 1 <= x <= 6:
                c = self.material(s, self.apron, "weave", key, x, y, face)
            if self.strap and face in ("front", "back") and self.strap_hit(face, x, Y):
                c = self.material(s, self.strap, "hide", key, x, y, face)
                if (Y + x) % 3 == 0:
                    c = self.sh(c, 0.8)  # stitches
            if self.belt and Y == 8:
                c = self.material(s, self.belt, "cord" if self.belt == CORD else "hide", key, x, y, face)
                if face == "front" and x == 3:
                    c = self.tint(BONE, 1.0)  # bone toggle
            if self.pouch and face == "front" and Y in (9, 10) and x in (5, 6):
                c = self.tint(self.pouch, 0.9 if Y == 10 else 1.0)
            if self.trim and Y == 11 and face in ("front", "back", "left", "right") and x % 2 == 0:
                c = self.tint(self.trim, side_shade(face))
            return c
        if leg:
            if self.skirt and Y < self.skirt_to:
                c = self.material(s, self.skirt, self.skirt_mat, key, x, y, face)
                if Y == self.skirt_to - 1:
                    c = self.sh(c, 0.78) if (x + y) % 2 == 0 else self.sh(c, 0.9)  # ragged hem
                return c
            if Y < self.skirt_to and self.top is not None and self.top_kind != "vest":
                c = self.material(s, self.top, tm, key, x, y, face)
                if Y == self.skirt_to - 1:
                    c = self.sh(c, 0.78) if (x + y) % 2 == 0 else self.sh(c, 0.9)  # ragged hem
                return c
            if self.top_kind == "vest" and Y < 14 and self.legs:
                return self.material(s, shade(self.legs, 0.92), "hide", key, x, y, face)
            if self.boots and Y >= 21:
                c = self.material(s, self.boots, "fur", key, x, y, face)
                return self.sh(c, 0.85) if Y == 21 else c
            if self.legs and Y < self.legs_to:
                c = self.material(s, self.legs, "hide", key, x, y, face)
                if face in ("right", "left") and x == 2:  # side seam
                    c = self.sh(c, 0.85)
                return c
            if self.wraps and Y >= self.legs_to and (Y + x // 2) % 2 == 0:
                return self.material(s, self.wraps, "cord", key, x, y, face)
            if self.body_paint and Y in (16, 17) and face == "front" and x in (1, 2):
                return shade(self.body_paint, side_shade(face))
            return None
        return None

    def strap_hit(self, face, x, Y):
        # from the right shoulder down to the left hip (front), mirrored on the back
        if face == "front":
            return Y <= 8 and x in (int(Y * 0.8), int(Y * 0.8) + 1) and x < 8
        return Y <= 8 and (7 - x) in (int(Y * 0.8), int(Y * 0.8) + 1)

    # ---------- head ----------
    def hair_px(self, s, key, x, y, f=1.0):
        streak = s.n(("hs",) + key[:2] + (x,), 0.08) + s.n(key + ("hp",), 0.06)
        return shade(self.hair, f * (1 + streak))

    def head(self, s, face, x, y, fw, fh):
        key = ("head", face, x, y)
        skin = shade(self.skin, side_shade(face))
        skin = tuple(v + s.n(key + ("sk",), 4) for v in skin)
        long_hair = self.hair_style in ("long", "braids", "bun", "shaggy")
        bald = self.hair_style == "bald_top"
        if self.hood:
            # the inside of a fur hood: all round the head, a rim of fur round the face
            if face in ("top", "back", "right", "left"):
                return self.material(s, self.hood, "fur", key, x, y, face)
            if face == "front" and (y == 0 or x in (0, 7)):
                return self.material(s, self.hood_trim or self.hood, "fur", key, x, y, face)
        if self.headwrap and face != "bottom":
            # a cloth wound round the head down to the brows; long hair hangs out below it at the back
            fx = x if face == "right" else 7 - x
            if face == "top" or (face == "back" and y < 4) or (face in ("right", "left") and (y < 3 or (y == 3 and fx <= 2))) \
                    or (face == "front" and y < 2):
                c = self.material(s, self.headwrap, "weave", key, x, y, face)
                lower = (face == "front" and y == 1) or (face == "back" and y == 3) or (face in ("right", "left") and y == 2)
                return self.sh(c, 0.84) if lower else c
        if face == "top":
            if bald:
                return shade(skin, 1.02) if 2 <= x <= 5 and 1 <= y <= 6 else self.hair_px(s, key, x, y, 1.05)
            if self.hair_style == "braids" and x in (3, 4):
                return self.hair_px(s, key, x, y, 0.82)  # parting
            return self.hair_px(s, key, x, y, 1.08)
        if face == "bottom":
            return self.beard_px(s, key, x, y, 0.8) if self.beard in ("full", "long") else shade(self.skin, 0.75)
        if face == "back":
            rows = 8 if long_hair else 6 if self.hair_style == "shaggy" else 5
            if y < rows:
                return self.hair_px(s, key, x, y, 0.92 if y < rows - 1 else 0.78)
            return shade(skin, 0.95)
        if face in ("right", "left"):
            # x on the side: 0 = back for "right"? the right face runs back->front left to right
            fx = x if face == "right" else 7 - x  # 0 = back edge, 7 = front edge
            hair_rows = 8 if long_hair else 3
            if self.hair_style == "short" and fx <= 2:
                hair_rows = 5
            if y < hair_rows and not (y >= 2 and fx >= 6) and not (y >= 3 and fx in (3, 4) and not long_hair):
                return self.hair_px(s, key, x, y, 0.9)
            if fx in (3, 4) and y in (4, 5):  # ear
                return shade(skin, 0.88 if y == 5 else 0.95)
            if self.beard and y >= 5 and fx >= 3 + (self.beard == "stubble"):
                return self.beard_px(s, key, x, y, 0.9)
            if self.beard and y == 4 and fx >= 6 and self.beard in ("full", "long"):
                return self.beard_px(s, key, x, y, 0.9)  # sideburns
            return self.face_paint(face, fx, y, skin)
        # ---- front: the face ----
        fringe = {"short": (0, 1), "long": (0, 1), "braids": (0,), "bun": (0,), "shaggy": (0, 1, 2), "bald_top": ()}[self.hair_style]
        if y in fringe:
            if y == 1 and self.hair_style in ("long", "short") and 2 <= x <= 5 and (x + self.r.random() > 3.5 or self.woman):
                pass  # forehead shows through a parted fringe
            elif y == 2 and x in (3, 4):
                pass
            else:
                return self.hair_px(s, key, x, y, 1.0)
        if long_hair and x in (0, 7) and y <= 6:
            return self.hair_px(s, key, x, y, 0.86)  # hair framing the face
        if self.headband and y == 1 and self.hair_style not in ("bald_top",):
            return self.tint(self.headband, 1.0 if x % 3 else 0.82)
        if y == 3 and x in (1, 2, 5, 6):  # brows
            return shade(self.hair, 1.15 if self.hair in (HAIR["white"], HAIR["grey"]) else 0.9)
        if y == 4:
            if x in (1, 6):
                return hexc("efe9e0")
            if x in (2, 5):
                return self.eyes
        if self.wrinkles and ((y == 2 and x in (2, 3, 5)) or (y == 5 and x in (1, 6))):
            return shade(skin, 0.86)
        if self.beard:
            b = self.beard
            if b == "stubble" and y >= 6 and (x + y) % 2 == 0:
                return mix(skin, self.hair, 0.45)
            if b in ("short", "full", "long") and (y == 7 or (y == 6 and x not in (3, 4)) or (y == 5 and x in (0, 1, 6, 7) and b != "short")):
                return self.beard_px(s, key, x, y, 1.0)
            if b in ("full", "long") and y == 5 and x in (2, 3, 4, 5):
                return self.beard_px(s, key, x, y, 1.05)  # moustache
        if y == 5 and x in (3, 4):
            return shade(skin, 0.86)  # nose shadow
        if y == 6 and x in (3, 4):
            return mix(shade(skin, 0.7), OCHRE, 0.25)  # mouth
        if y == 5 and x in (1, 6) and self.woman:
            return mix(skin, hexc("c46a5a"), 0.18)  # cheeks
        return self.face_paint("front", x, y, skin)

    def beard_px(self, s, key, x, y, f):
        return self.hair_px(s, key, x, y, f * (1.08 if self.hair in (HAIR["grey"], HAIR["white"]) else 0.95))

    def face_paint(self, face, x, y, skin):
        if not self.paint:
            return skin
        col, style = self.paint
        if face == "front":
            if style == "stripes" and y == 5 and x in (0, 1, 6, 7):
                return col
            if style == "band" and y == 4 and x in (0, 3, 4, 7):
                return col
            if style == "dots" and ((y == 5 and x in (1, 6)) or (y == 2 and x in (3, 4))):
                return col
            if style == "chin" and y == 7 and x in (2, 3, 4, 5):
                return col
        elif style == "stripes" and y == 5 and x >= 6:
            return col
        return skin

    # ---------- outer layer ----------
    def over(self, s, part, face, x, y, fw, fh):
        key = ("o", part, face, x, y)
        if part == "head":
            return self.hat(s, face, x, y, key)
        Y = gy(part, face, y, fh)
        arm = part in ("rarm", "larm")
        leg = part in ("rleg", "lleg")
        if part == "body":
            if self.quiver and face == "back" and 1 <= x <= 3 and Y <= 9 - x:
                if Y <= 1 - (x == 2):
                    return [hexc("e8e2d0"), OCHRE, hexc("3a3430")][x - 1]  # fletching sticking out
                c = self.material(s, BARK, "hide", key, x, y, face)
                return shade(c, 0.8) if x == 3 else c
            if self.fur_kind == "cape" and face in ("back", "right", "left") and Y <= 10:
                c = self.material(s, self.fur, "fur", key, x, y, face)
                if Y == 10 or (face != "back" and Y > 6):
                    return None if (x + Y) % 2 else self.sh(c, 0.8)
                return c
            if self.fur_kind in ("cape", "shoulders", "collar") and Y <= (1 if self.fur_kind == "collar" else 2) and face != "bottom":
                if face == "front" and 2 <= x <= 5 and Y >= 1 and self.fur_kind != "collar":
                    pass
                else:
                    c = self.material(s, self.fur, "fur", key, x, y, face)
                    return self.sh(c, 0.84) if Y >= 1 and (x % 2) else c
            if self.necklace and face == "front":
                c = self.neck_px(x, Y)
                if c is not None:
                    return c
            if self.beard == "long" and face == "front" and 2 <= x <= 5 and Y <= (2 if x in (2, 5) else 3):
                return self.beard_px(s, key, x, y, 0.96)
            if self.hair_style == "braids" and face == "back" and x in (1, 6) and Y <= 3:
                return self.braid_px(Y, x)
            if self.hair_style == "long" and face == "back" and 1 <= x <= 6 and Y <= (2 if x in (1, 6) else 3):
                return self.hair_px(s, key, x, y, 0.85 if Y == 3 or x in (1, 6) else 0.95)
            if self.trim and Y == 11 and face in ("front", "back"):
                return self.material(s, self.trim, "cord", key, x, y, face) if x % 2 == 1 else None
            return None
        if arm:
            if self.fur_kind in ("cape", "shoulders") and Y <= 1 and face != "bottom":
                c = self.material(s, self.fur, "fur", key, x, y, face)
                return c if Y == 0 or face == "top" or x % 2 == 0 else None
            if self.bracers and face != "top" and Y == 10 and x % 2 == 0:
                return self.tint(self.bracers, 0.75)  # laced edge
            return None
        if leg:
            if self.fur_kind == "cape" and False:
                return None
            if self.boots and Y >= 20 and face not in ("top",) and (Y > 20 or (x + part.count("r")) % 2 == 0):
                if face == "bottom":
                    return None
                return self.material(s, self.boots, "fur", key, x, y, face)
            hem = self.skirt or (self.top if self.top is not None and self.top_kind != "vest" else None)
            if hem is not None and Y == self.skirt_to and face not in ("top", "bottom") and x % 2 == 0:
                return self.tint(hem, 0.88 * side_shade(face))  # fringe
            return None
        return None

    def braid_px(self, Y, x):
        k = 1.0 if (Y + x) % 2 == 0 else 0.72
        if Y == 3:
            return self.tint(self.headband or CORD, 1.0)  # tied off
        return self.tint(self.hair, k)

    def neck_px(self, x, Y):
        kind = self.necklace
        # V-shaped string over the chest
        on_string = (Y == 0 and x in (0, 7)) or (Y == 1 and x in (1, 6)) or (Y == 2 and x in (2, 5))
        if kind == "amber":
            if Y == 3 and x in (3, 4):
                return AMBER if x == 3 else shade(AMBER, 0.8)
            return shade(CORD, 0.9) if on_string or (Y == 2 and x in (3, 4)) else None
        if kind == "teeth":
            if on_string:
                return TOOTH if x % 2 == 0 else shade(CORD, 0.8)
            if Y == 2 and x in (3, 4):
                return shade(CORD, 0.8)
            if Y == 3 and x in (3, 4):
                return TOOTH  # a big fang
            return None
        if kind == "fishbone":
            if on_string or (Y == 2 and x in (3, 4)):
                return BONE if (x + Y) % 2 == 0 else hexc("b8b0a0")
            return None
        if kind == "shell":
            if on_string:
                return shade(CORD, 0.85)
            if Y == 2 and x in (3, 4):
                return hexc("e9d9c6") if x == 3 else hexc("c9a98e")
            return None
        # bone beads
        if on_string or (Y == 2 and x in (3, 4)):
            return BONE if (x + Y) % 2 else shade(BONE, 0.82)
        return None

    def hat(self, s, face, x, y, key):
        style = self.hair_style
        fx = x if face == "right" else 7 - x
        if self.hood:
            # the hood's outer fur; the trim stands out round the face
            if face == "bottom":
                return None
            if face == "front":
                return self.material(s, self.hood_trim or self.hood, "fur", key, x, y, face) if (y == 0 or x in (0, 7)) else None
            if face != "top" and y == 7 and x % 2:
                return None  # shaggy lower edge
            return self.material(s, self.hood, "fur", key, x, y, face)
        if self.headwrap:
            if face == "top" and 1 <= x <= 6 and 1 <= y <= 6:
                return self.material(s, self.headwrap, "weave", key, x, y, face)  # wound up high
            if face == "back" and x in (3, 4) and 1 <= y <= 5:
                return self.tint(self.headwrap, (0.82 if x == 4 else 0.95) * (0.9 if y == 5 else 1.0))  # the loose end
            if face in ("right", "left", "front", "back") and y == 1 and x % 3 != 0:
                return self.material(s, self.headwrap, "weave", key, x, y, face)  # folds
        if self.headdress:
            feathers = self.headdress
            if face == "top" and y >= 5 and 1 <= x <= 6:
                return self.tint(feathers[x % len(feathers)], 1.0 if y == 5 else 0.88)
            if face == "back" and y <= 4 and x % 2 == 0:
                col = feathers[(x // 2) % len(feathers)]
                return self.tint(col, 1.12 if y == 0 else 1.0 - 0.05 * y)  # light tips
        if face == "top":
            if style == "shaggy" and (x + y) % 3 != 0:
                return self.hair_px(s, key, x, y, 1.12)
            if style == "bun" and 2 <= x <= 5 and 4 <= y <= 7:
                return self.hair_px(s, key, x, y, 1.05 if (x + y) % 2 else 0.85)
            if self.feather and x == 6 and y in (5, 6):
                return hexc("f0ece2") if y == 5 else hexc("3a3030")
            return None
        if face == "bottom":
            return None
        if self.headband and y == 2 and face in ("right", "left", "back") and style != "bald_top":
            return self.tint(self.headband, 0.95 if (x + y) % 2 else 0.8)
        if self.feather and face == "left" and fx == 1 and y <= 2:
            return [hexc("f0ece2"), hexc("e0dccf"), hexc("3a3030")][y]
        if style == "long" and face in ("right", "left") and fx <= 4 and y >= 2:
            return self.hair_px(s, key, x, y, 0.82 if fx == 4 else 0.9)
        if style == "long" and face == "back" and y >= 1:
            return self.hair_px(s, key, x, y, 0.85 + (0.08 if x % 2 else 0))
        if style == "braids" and face in ("right", "left") and fx in (4, 5) and y >= 3:
            return self.braid_px(y, fx) if y < 7 else self.tint(self.headband or CORD, 1.0)
        if style == "bun" and face == "back" and 2 <= x <= 5 and y <= 2:
            return self.hair_px(s, key, x, y, 1.0 if (x + y) % 2 else 0.8)
        if style == "shaggy" and face in ("right", "left", "back") and y <= 4 and (x * 7 + y) % 3 != 0:
            return self.hair_px(s, key, x, y, 0.95)
        if self.beard == "long" and face == "front" and y >= 6 and 1 <= x <= 6:
            return self.beard_px(s, key, x, y, 1.02)
        if self.beard == "full" and face == "front" and y == 7 and 1 <= x <= 6 and x % 2:
            return self.beard_px(s, key, x, y, 0.92)
        return None


# ---------------- the people ----------------

def pick(r, xs):
    return xs[r.randrange(len(xs))]


def person(role, i):
    r = random.Random(f"{role}-{i}")
    woman = i % 2 == 1
    L = Look(r, woman, role)
    tones = {"hunter": ["tan", "olive", "brown", "light"], "gatherer": ["light", "brown", "dark", "tan"],
             "fisher": ["pale", "tan", "olive", "light"], "woodcutter": ["brown", "pale", "tan", "dark"],
             "elder": ["olive", "light", "dark", "tan"]}
    L.skin = SKIN[tones[role][i]]
    L.eyes = EYES[pick(r, ["brown", "dark", "brown", "blue", "green", "grey"])]
    L.hair = HAIR[pick(r, ["black", "darkbrown", "brown", "auburn", "blond", "darkbrown"])]
    if woman:
        L.hair_style = pick(r, ["braids", "long", "bun", "braids"])
    else:
        L.hair_style = pick(r, ["short", "shaggy", "long", "short"])
        L.beard = pick(r, ["full", "short", "stubble", "full"])
    if role == "hunter":
        L.top, L.top_kind, L.sleeves = [HIDE_DARK, HIDE_RED, LEATHER, HIDE][i], pick(r, ["tunic", "wrap"]), 0
        L.fur, L.fur_kind = pick(r, [FUR_GREY, FUR_BROWN, FUR_RED]), pick(r, ["cape", "shoulders"])
        L.paint = (pick(r, [OCHRE, CHARCOAL, BLUE_CLAY]), pick(r, ["stripes", "band", "stripes"]))
        L.necklace = "teeth"
        L.strap = LEATHER
        L.quiver = True
        L.legs, L.boots, L.bracers = LEATHER if L.top != LEATHER else HIDE_DARK, FUR_BROWN, LEATHER
        L.skirt_to = 13
        L.headband = pick(r, [None, CORD, LEATHER])
        L.feather = i == 0
    elif role == "gatherer":
        L.top, L.top_kind, L.sleeves = [HIDE_LIGHT, HIDE_GREY, HIDE_LIGHT, HIDE_GREY][i], "tunic", 4
        L.apron = pick(r, [STRAW, BARK])
        L.pouch = LEATHER
        L.necklace = pick(r, ["shell", "bone"])
        L.legs, L.legs_to, L.boots = (None, 22, FUR_BROWN) if woman else (HIDE_DARK, 20, None)
        L.skirt_to = 16 if woman else 14
        L.headband = pick(r, [CORD, STRAW, None])
        L.feather = i == 3
        L.trim = STRAW if woman else None
    elif role == "fisher":
        L.top, L.top_kind, L.sleeves = [HIDE_GREY, HIDE_LIGHT, HIDE, HIDE_GREY][i], "vest", 0
        L.necklace = "fishbone"
        L.legs, L.legs_to, L.boots = HIDE_DARK, 18, None  # rolled to the knee: bare shins in the water
        L.wraps = CORD
        L.belt = CORD
        L.pouch = BARK
        L.headband = CORD
        L.paint = (BLUE_CLAY, "dots") if i == 2 else None
        if not woman:
            L.beard = pick(r, ["short", "stubble"])
    elif role == "woodcutter":
        L.top, L.top_kind, L.sleeves = [HIDE_GREY, LEATHER, HIDE_RED, LEATHER][i], pick(r, ["vest", "tunic"]), 0
        L.fur, L.fur_kind = FUR_GREY if L.top == LEATHER else FUR_BROWN, "collar"
        L.bracers = HIDE
        L.belt = hexc("4a2e1c")
        L.legs, L.boots = hexc("7a5434"), FUR_BROWN
        L.necklace = pick(r, ["bone", None])
        L.skin = shade(L.skin, 0.96)  # weathered
        if not woman:
            L.beard = pick(r, ["full", "full", "short"])
            L.hair_style = pick(r, ["shaggy", "short"])
        else:
            L.hair_style = pick(r, ["braids", "bun"])
    elif role == "elder":
        L.hair = HAIR[pick(r, ["grey", "white"])]
        L.wrinkles = True
        L.top, L.top_kind, L.sleeves = [FUR_BROWN, HIDE_RED, HIDE_DARK, FUR_BROWN][i], "robe", 12
        L.skirt_to = 21
        L.fur, L.fur_kind = pick(r, [FUR_WHITE, FUR_GREY]), "cape"
        L.necklace = pick(r, ["amber", "teeth"])
        L.legs, L.boots = None, FUR_WHITE
        L.belt = CORD
        L.paint = (OCHRE, "dots")
        L.trim = BONE
        if woman:
            L.hair_style = "long"
        else:
            L.hair_style = pick(r, ["bald_top", "long"])
            L.beard = "long"
    return L


def dress(L, zone, i):
    """Dresses a person (role details already set by person()) for a climate. The role's tools of the trade stay: the
    hunter's quiver, strap, fang necklace and war paint, the gatherer's apron and pouch, the fisher's fishbone necklace,
    the woodcutter's bracers, the elder's robe and amber; what changes is the clothing under them. Every skin carries
    some of the tribe's colour (DYE)."""
    r = random.Random(f"{L.role}-{i}-{zone}")
    role, woman = L.role, L.woman
    if zone == "temperate":
        # as before; the headband, the gatherer women's hem and the elder's trim take the tribe's colour
        if L.headband is not None:
            L.headband = DYE
        if L.trim is not None:
            L.trim = DYE
        if L.headband is None and L.trim is None:
            L.belt = DYE
        return L
    if zone == "cold":
        L.skin = SKIN[["pale", "light", "tan", "light"][i]]
        L.hair = HAIR[pick(r, ["black", "darkbrown", "brown", "blond", "darkbrown"])] if role != "elder" else L.hair
        coat = {"hunter": [HIDE_DARK, HIDE_GREY, HIDE_RED, HIDE_DARK], "gatherer": [HIDE_GREY, HIDE_LIGHT, HIDE, HIDE_LIGHT],
                "fisher": [HIDE_GREY, HIDE, HIDE_DARK, HIDE_GREY], "woodcutter": [LEATHER, HIDE_DARK, HIDE_RED, LEATHER],
                "elder": [FUR_BROWN, FUR_GREY, FUR_WHITE, FUR_BROWN]}[role][i]
        L.top, L.top_mat = coat, "fur" if role == "elder" else "hide"
        L.top_kind = "robe" if role == "elder" else "tunic"
        L.sleeves = 12
        L.skirt_to = 21 if role == "elder" else 15
        L.legs, L.legs_to = pick(r, [FUR_BROWN, LEATHER, FUR_GREY]), 22
        L.boots = pick(r, [FUR_WHITE, FUR_GREY, FUR_BROWN])
        L.wraps = None
        L.trim = FUR_WHITE
        L.band = DYE
        L.mittens = pick(r, [FUR_BROWN, FUR_GREY]) if i % 2 == 0 else None
        if i == 3 and role != "elder":
            # hood down: a fur collar and the tribe's headband show
            L.hood = None
            L.fur, L.fur_kind = pick(r, [FUR_WHITE, FUR_GREY]), "collar"
            L.headband = DYE
        else:
            L.hood = pick(r, [FUR_BROWN, FUR_GREY, FUR_WHITE, FUR_RED]) if role != "elder" else FUR_WHITE
            L.hood_trim = FUR_WHITE if L.hood != FUR_WHITE else FUR_GREY
            L.headband = None
            if L.fur_kind == "collar":
                L.fur_kind = None  # the hood covers it
        if role == "hunter":
            L.fur_kind = "cape" if L.fur_kind == "cape" else "shoulders"
            L.fur = pick(r, [FUR_GREY, FUR_WHITE, FUR_BROWN])
        if L.top_kind == "vest":
            L.top_kind = "tunic"
        L.feather = False
        return L
    if zone == "warm":
        L.skin = SKIN[[["brown", "dark", "olive", "tan"], ["olive", "brown", "dark", "tan"]][i % 2 == 1][i]]
        L.hair = HAIR[pick(r, ["black", "black", "darkbrown"])] if role != "elder" else L.hair
        L.fur, L.fur_kind = None, None
        L.top_mat = "weave"
        if role == "elder":
            L.top, L.top_kind, L.sleeves, L.skirt_to = LINEN, "robe", 4, 21
            L.trim = DYE
        elif woman:
            L.top, L.top_kind, L.sleeves = pick(r, [LINEN, HIDE_LIGHT]), "wrap", 0
            L.skirt, L.skirt_mat, L.skirt_to = LINEN, "weave", 17
        else:
            L.top = None if role in ("hunter", "fisher") else LINEN
            L.top_kind, L.sleeves = "vest", 0
            L.skirt, L.skirt_mat, L.skirt_to = pick(r, [LINEN, HIDE_LIGHT]), "weave", 15
        L.legs, L.boots = None, None
        L.wraps, L.legs_to = CORD, 22  # sandal straps
        L.hood = None
        L.headband = None
        if i in (0, 2):
            L.headwrap = DYE
            L.strap = L.strap or LINEN
        else:
            L.headwrap = pick(r, [LINEN, HIDE_LIGHT])
            if L.strap is None:
                L.strap = DYE  # a sash in the tribe's colour
            else:
                L.belt = DYE
        L.feather = False
        return L
    # jungle
    L.skin = SKIN[[["brown", "dark", "olive", "dark"], ["dark", "brown", "olive", "brown"]][role in ("gatherer", "elder")][i]]
    L.hair = HAIR[pick(r, ["black", "black", "darkbrown"])] if role != "elder" else L.hair
    L.fur, L.fur_kind = None, None
    L.legs, L.boots, L.wraps = None, None, None
    L.barefoot = True
    L.sleeves = 0
    L.hood = None
    L.top_mat = "hide"
    if role == "elder":
        L.top, L.top_kind = (HIDE_LIGHT, "wrap") if woman else (None, "vest")
        L.fur, L.fur_kind = FEATHERS[1], "cape"  # a cloak of parrot feathers
        L.skirt, L.skirt_mat, L.skirt_to = DYE, "hide", 20
        L.trim = None
    elif woman:
        L.top, L.top_kind = pick(r, [HIDE_LIGHT, HIDE]), "wrap"
        L.skirt, L.skirt_mat, L.skirt_to = LEAF, "weave", 17
        L.trim = DYE
    else:
        L.top, L.top_kind = None, "vest"
        L.skirt, L.skirt_mat, L.skirt_to = DYE, "hide", 14  # loincloth
        L.armband = DYE if L.bracers is None else None
        L.trim = None
    if L.apron is not None:
        L.apron = LEAF
    L.headband = DYE
    L.headdress = [FEATHERS[(i + k) % 4] for k in range(3)] + [DYE]
    L.body_paint = pick(r, [WHITE_CLAY, OCHRE, CHARCOAL])
    if L.necklace is None:
        L.necklace = "shell"
    L.feather = False
    return L


ROLES = ["hunter", "gatherer", "fisher", "woodcutter", "elder"]


def flat_view(img, slim, back=False):
    """Front (or back) of the body laid out flat, 16x32, for previews."""
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    aw = 3 if slim else 4

    def face(part, overlay, name):
        u, v, w, h, d = BASE[part]
        if slim and part in ("rarm", "larm"):
            w = 3
        if overlay:
            u, v = OVER[part]
        x0, y0, fw, fh = faces(u, v, w, h, d)[name]
        return img.crop((x0, y0, x0 + fw, y0 + fh))

    name = "back" if back else "front"
    layout = [("head", 4, 0), ("body", 4, 8), ("rleg", 4, 20), ("lleg", 8, 20)]
    # seen from the front, the right arm is on the viewer's left
    ra_x, la_x = (4 - aw, 12) if not back else (12, 4 - aw)
    if back:
        layout = [("head", 4, 0), ("body", 4, 8), ("rleg", 8, 20), ("lleg", 4, 20)]
    layout += [("rarm", ra_x, 8), ("larm", la_x, 8)]
    for part, x, y in layout:
        for overlay in (False, True):
            f = face(part, overlay, name)
            out.alpha_composite(f, (x, y))
    return out


def build():
    made = {}
    for zone in ZONES:
        out = OUT if zone == "temperate" else OUT / zone
        out.mkdir(parents=True, exist_ok=True)
        for role in ROLES:
            for i in range(4):
                L = dress(person(role, i), zone, i)
                sk = Skin(f"{role}{i}{zone}", L.woman)
                img = sk.build(L)
                assert sk.mask.getbbox() is not None, f"{zone}/{role}_{i}: nothing in the tribe's colour"
                img.save(out / f"{role}_{i}.png")
                sk.mask.save(out / f"{role}_{i}_tint.png")
                made[(zone, role, i)] = (img, sk.mask, L.woman)
    return made


def dyed(img, mask, rgb):
    """img with the masked pixels in colour rgb, as client/people/TintedSkin does in game."""
    out = img.copy()
    for y in range(64):
        for x in range(64):
            m = mask.getpixel((x, y))
            if m[3]:
                a = out.getpixel((x, y))[3]
                out.putpixel((x, y), tuple(c * m[0] // 255 for c in rgb) + (a,))
    return out


# a few DyeColor texture colours for the preview
PREVIEW_DYES = [(0xb0, 0x2e, 0x26), (0x3c, 0x44, 0xaa), (0x5e, 0x7c, 0x16), (0xf9, 0x80, 0x1d), (0xf9, 0xff, 0xfe)]


def preview(made, out_dir):
    out_dir = Path(out_dir)
    scale = 5
    cols = 8  # 4 variants x (front, back)
    for zone in ZONES:
        sheet = Image.new("RGBA", (cols * 18 * scale, len(ROLES) * 34 * scale), (70, 90, 70, 255))
        for row, role in enumerate(ROLES):
            for i in range(4):
                img, mask, slim = made[(zone, role, i)]
                img = dyed(img, mask, PREVIEW_DYES[(row + i) % len(PREVIEW_DYES)])
                for k, back in enumerate((False, True)):
                    v = flat_view(img, slim, back).resize((16 * scale, 32 * scale), Image.NEAREST)
                    sheet.alpha_composite(v, (((i * 2 + k) * 18 + 1) * scale, (row * 34 + 1) * scale))
        sheet.save(out_dir / f"tribe_sheet_{zone}.png")


if __name__ == "__main__":
    made = build()
    print(f"wrote {len(made)} skins")
    if len(sys.argv) > 1:
        preview(made, sys.argv[1])
