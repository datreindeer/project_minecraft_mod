"""Skins for the Stone Age tribespeople (mechanical:tribesperson): 64x64 player-format skins, 4 per role and climate
(0, 2: men with wide arms; 1, 3: women with slim arms), painted pixel by pixel: hide and fur clothing, bone and
tooth necklaces, braids, beards, war paint. Each climate (people/ClimateZone.java) dresses its own way on top of the role:
cold: fur hoods, long hide coats with fur trim, fur trousers and mittens; temperate: hide and leather, woven aprons;
warm: light linen wraps and kilts, head cloths, sandals; jungle: loincloths and grass skirts, feather headdresses, paint.
Temperate skins go in tribesperson/, the others in tribesperson/<climate>/. Next to every skin a "_tint" mask marks the
dyed parts (headbands, sashes, coat bands, loincloths) in grey; client/people/TintedSkin multiplies them by the tribe's
colour. Trader Caravans (trader, guard) get one temperate set: fine gem-set robes and clean linen under armour. The Miner
(people/Miners.java) too, men only (miner_0, miner_2): soot-smudged face, worn leather, a leather apron, the tribe's colour
on the headband.
Next to most skins a "_gear" texture paints the parts that stand out from the body (client/TribeGearModel, drawn by
EquipmentLayer): fur hoods with a ruff round the face and fur mantles in the cold, hide cloaks, big necklaces standing off
the chest, top-knots, braids, beards down the chest, feather crests.
Run: python scripts/gen_tribe_skins.py [preview_dir]"""
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
GOLD = hexc("d4a838")
GEMS = [hexc("c0283a"), hexc("2c5cc8"), hexc("2a9a5a"), hexc("8a3ac0"), hexc("e8c030")]  # ruby, sapphire, emerald, amethyst, topaz
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
        look.contrast()
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


def vary(col, light, warm):
    """A hide or fur tanned a little differently for each person: lighter or darker, warmer or greyer."""
    f = 1 + 0.09 * light
    return (col[0] * f * (1 + 0.05 * warm), col[1] * f, col[2] * f * (1 - 0.06 * warm))


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
        self.fine = False              # well-made clothes: straight hems, not ragged (caravan traders and guards)
        self.gems = None               # gemstones set along the band, the headband and the necklace (list of colours)
        self.armour = None             # metal armour over the tunic (caravan guards): see ARMOUR
        self.cloak = None              # gear: a cloak down the back in this colour (caravan folk), instead of the role's
        self.mantle = None             # gear: a fur mantle round the shoulders in this colour (caravan folk)
        self.folds = False             # cloth hangs in folds (caravan folk): darker valleys, lit ridges
        self.plaid = None              # top_mat "plaid": the two stripe colours (dark, light) over the top's colour
        self.patches = None            # top_mat "patch": colours of the sewn-on patches
        self.apron_mat = "weave"       # what the apron is: weave or hide (a leather work apron)
        self.apron_to = 11             # the apron's hem (global row): past 11 it hangs down over the thighs
        self.belt_bundles = None       # dried herb bundles tucked in the belt (colour)
        self.freckles = False
        self.scar = False
        self.brow = None               # None, "heavy", "thin"
        self.back_item = None          # gear: "axe" or "quiver" on the back (painted on the cloak box)
        self.pack = None               # gear: "basket" carried on the back
        self.hips = ()                 # gear: things hanging at the hips: "lantern", "satchel", "bundle"
        self.cap = None                # gear: a padded cap (colour) over the hair
        self.cap_style = None          # gear: None a padded cap; "tall" a tall embroidered felt cap with a raised crown
        self.cap_work = None           # the tall cap's embroidery (list of colours)
        self.cap_trim = None           # a fur band round the tall cap's brim (colour)
        self.ikat = None               # top_mat "ikat": (stripe colours, stripe width), the stripe edges blurred
        self.sash = None               # a wide silk sash wound round the waist (colour), knotted at the side
        self.collar = None             # an embroidered collar and edging down the front of the coat: (colour, colour)
        self.satchel = None            # outer layer: a satchel at the left hip (colour), on the strap
        self.cloak_border = None       # gear: a silk cloak (no fur at the shoulders) edged in this colour
        self.s = None

    def contrast(self):
        """Leggings a clear step darker (or lighter) than the tunic over them, boots darker than the leggings."""
        def lum(c):
            return 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2]
        if self.top is not None and self.legs is not None and not isinstance(self.legs, Dye) and not isinstance(self.top, Dye):
            if abs(lum(self.top) - lum(self.legs)) < 30:
                self.legs = shade(self.legs, 0.7) if lum(self.top) >= lum(self.legs) or lum(self.legs) > 150 else shade(self.legs, 1.3)
        if self.legs is not None and self.boots is not None and not isinstance(self.boots, Dye) and abs(lum(self.legs) - lum(self.boots)) < 20:
            self.boots = shade(self.boots, 0.72)

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
        if not isinstance(col, Dye) and kind in ("hide", "fur") and col not in (GOLD, BONE, TOOTH):
            col = vary(col, s.n(("tone", col), 1.0), s.n(("warm", col), 1.0))
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
        if kind == "plaid":
            # woven wool in a check: dark bands every 4 px both ways, darkest where they cross, a thin light line between
            dark, light = self.plaid
            bx, by = x % 4 == 0, y % 4 == 0
            c = mix(col, dark, 0.66) if bx and by else mix(col, dark, 0.42) if bx or by else col
            if x % 8 == 2 and not by:
                c = mix(c, light, 0.3)  # a thin light thread through the check
            return self.tint(c, f * (1.04 if (x + y) % 2 == 0 else 0.96) * (1 + s.n(key + ("pl",), 0.03)))
        if kind == "patch":
            # patchwork: a different cloth in each 3 x 3 square, stitched round
            pal = self.patches
            idx = (x // 3 * 7 + y // 3 * 3 + len(key[1])) % len(pal)
            c = pal[idx]
            g = 0.82 if x % 3 == 0 or y % 3 == 0 else 1.0
            return self.tint(c, f * g * (1.05 if (x + y) % 2 == 0 else 0.95) * (1 + s.n(key + ("pa",), 0.04)))
        if kind == "ikat":
            # silk-road ikat: bold vertical stripes, the threads dyed before weaving so each edge frays in a zigzag
            cols, w = self.ikat
            sx = x + (1 if (y // 2 + x // w) % 2 else 0)
            c = cols[(sx // w + len(key[1])) % len(cols)]
            if sx % w == 0:
                c = mix(c, cols[(sx // w - 1 + len(key[1])) % len(cols)], 0.3)  # the blurred edge
            return self.tint(c, f * (1.05 if (x + y) % 2 == 0 else 0.97) * (1 + s.n(key + ("ik",), 0.03)))
        # hide/leather: soft mottling and the odd darker patch
        m = s.n(key + ("h",), 0.06) + (s.n(("patch",) + key[:2] + (x // 2, y // 3), 0.05))
        return self.tint(col, f * (1 + m))

    def fold(self, c, face, x, Y):
        """Cloth hanging in folds: a darker valley every third column below the chest, the column beside it lit."""
        if not self.folds or face not in ("front", "back") or Y < 3:
            return c
        k = (x + (Y > 8)) % 3
        return self.sh(c, 0.84) if k == 0 else self.sh(c, 1.08) if k == 1 else c

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
                if Y == self.sleeves - 1:
                    return self.sh(c, 0.82)
                if Y in (5, 6) and face != "top" and (x + Y) % 2 == 0 and tm != "weave":
                    return self.sh(c, 0.88)  # creases at the elbow
                if face in ("right", "left") and x == 1 and Y % 2 == 0 and tm == "hide":
                    return self.sh(c, 1.12)  # the stitched seam down the sleeve
                return c
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
                if c is not None and face == "front" and self.top_kind == "robe" and x == 3 and 3 <= Y and not self.collar:
                    c = self.sh(c, 0.78)  # robe overlap seam
                if c is not None and self.collar and face != "bottom" and (Y == 0 or face == "front" and x in (3, 4)):
                    c = self.collar_px(face, x, Y)  # the embroidered collar and front edging
            if c is None:
                s.kind = "skin"
                s.dyed = None
                c = self.skin_px(s, part, face, x, y, fw, fh)
                if face == "front" and Y in (3, 4) and x in (1, 2, 5, 6):  # chest shading
                    c = shade(c, 0.94)
                if self.body_paint and face in ("front", "back") and ((Y == 2 and x in (1, 6)) or (Y == 6 and 2 <= x <= 5 and x % 2 == 0)):
                    c = shade(self.body_paint, side_shade(face))
            if c is not None and s.kind != "skin" and self.top is not None and tm == "hide":
                if face in ("right", "left") and x == 2 and Y % 2 == 1:
                    c = self.sh(c, 1.12)  # stitches down the side seam
                if face in ("front", "back") and Y == 1 and x in (0, 7):
                    c = self.sh(c, 0.88)  # the shoulder seam
                if self.top_kind == "tunic" and face == "front" and 1 <= Y <= 4 and x in (3, 4) and not self.necklace_at(x, Y):
                    # the laced neck opening: a dark slit crossed by a thong
                    c = self.tint(CORD, 1.0) if (x == 3) == (Y % 2 == 1) else self.sh(c, 0.7)
            if self.armour and Y <= 10 and face != "bottom" and not (face == "front" and Y == 0 and 2 <= x <= 5):
                c = self.armour_px(s, key, face, x, Y, 10)  # the armour coat over the tunic, the collar open at the neck
            if c is not None and s.kind != "skin" and not self.armour:
                c = self.fold(c, face, x, Y)
            if self.band and self.top is not None and Y == 5 and face in ("front", "back", "left", "right"):
                c = self.material(s, self.band, "weave", key, x, y, face)  # dyed band across the coat
                if self.gems and x % 3 == 1:
                    c = self.tint(self.gems[(x // 3 + len(face)) % len(self.gems)], side_shade(face))  # set with gemstones
            if self.skirt and Y >= 9:
                c = self.material(s, self.skirt, self.skirt_mat, key, x, y, face)
            if self.apron and face == "front" and (8 if self.apron_to <= 11 else 4) <= Y <= 11 and 1 <= x <= 6:
                c = self.material(s, self.apron, self.apron_mat, key, x, y, face)
                if self.apron_to > 11 and Y == 4 and x in (1, 6):
                    c = self.tint(LEATHER, 0.8)  # the bib's neck strap
            if self.belt_bundles is not None and face == "front" and Y in (8, 9, 10) and x in (0, 7):
                c = self.tint(self.belt_bundles, (1.0 if (x + Y) % 2 else 0.8) * (0.85 if Y == 8 else 1.0))  # dried herbs in the belt
            if self.strap and face in ("front", "back") and self.strap_hit(face, x, Y):
                c = self.material(s, self.strap, "hide", key, x, y, face)
                if (Y + x) % 3 == 0:
                    c = self.sh(c, 0.8)  # stitches
            if self.sash and 7 <= Y <= 9 and face != "top":
                # a wide silk sash wound three times round: each turn lit at the top, the folds running aslant
                c = self.material(s, self.sash, "hide", key, x, y, face)
                c = self.sh(c, (1.1, 1.0, 0.82)[Y - 7] * (0.84 if (x + Y) % 4 == 0 else 1.0))
                if face == "front" and x in (1, 2):
                    c = self.sh(c, 1.14 if Y == 8 else 0.9)  # the knot
            if self.sash and face == "front" and 10 <= Y <= 11 and x in (1, 2) and (x == 1 or Y == 10):
                c = self.tint(self.sash, 0.92 if x == 1 else 0.78)  # the ends hanging from the knot
            if self.belt and Y == 8:
                c = self.material(s, self.belt, "cord" if self.belt == CORD else "hide", key, x, y, face)
                if face == "front" and x == 3:
                    c = self.tint(BONE, 1.0)  # bone toggle
            if self.pouch and face == "front" and Y in (9, 10) and x in (5, 6):
                c = self.tint(self.pouch, 0.9 if Y == 10 else 1.0)
                if Y == 9 and x == 6:
                    c = self.tint(BONE, 0.9)  # the toggle closing the pouch
            if self.role in ("hunter", "woodcutter", "guard", "miner") and face == "front" and x == 1 and 8 <= Y <= 11 and not self.skirt:
                c = self.tint(BONE if Y == 8 else (LEATHER if Y > 9 else hexc("6a4a30")), 1.0 if Y < 11 else 0.8)  # knife: antler hilt, sheath
            if self.trim and Y == 11 and face in ("front", "back", "left", "right") and x % 2 == 0:
                c = self.tint(self.trim, side_shade(face))
            return c
        if leg:
            inner = (x >= 1) if part == "rleg" else (x <= 2)
            if self.apron and self.apron_to > 11 and face == "front" and Y <= self.apron_to and inner:
                c = self.material(s, self.apron, self.apron_mat, key, x, y, face)  # a work apron down over the thighs
                return self.sh(c, 0.8) if Y == self.apron_to else c
            if self.armour and 12 <= Y <= 15 and face not in ("top", "bottom"):
                a = self.armour  # tassets: hanging strips of plates, laced at the top, scalloped at the hem
                if Y == 12:
                    return self.tint(a["lace"], side_shade(face) * (1.0 if x % 2 else 0.85))
                if Y == 15 and x % 2:
                    c = self.material(s, self.top, self.top_mat, key, x, y, face) if self.top is not None else None
                    if c is not None:
                        return self.sh(c, 0.85)
                strip = a["hi"] if x % 2 == 0 and Y == 13 else a["base"] if x % 2 == 0 else a["lo"]
                return self.tint(strip, side_shade(face) * (1 + s.n(key + ("ar",), 0.05)) * (0.86 if Y == 15 else 1.0))
            if self.skirt and Y < self.skirt_to:
                c = self.material(s, self.skirt, self.skirt_mat, key, x, y, face)
                if Y == self.skirt_to - 1:
                    c = self.sh(c, 0.85) if self.fine else self.sh(c, 0.78) if (x + y) % 2 == 0 else self.sh(c, 0.9)  # ragged hem
                return c
            if Y < self.skirt_to and self.top is not None and self.top_kind != "vest":
                c = self.fold(self.material(s, self.top, tm, key, x, y, face), face, x, Y)
                if self.collar and face == "front" and x == (3 if part == "rleg" else 0):
                    c = self.collar_px(face, 3 if part == "rleg" else 4, Y)  # the edging down the skirt
                if Y == self.skirt_to - 1:
                    c = self.sh(c, 0.85) if self.fine else self.sh(c, 0.78) if (x + y) % 2 == 0 else self.sh(c, 0.9)  # ragged hem
                return c
            if self.top_kind == "vest" and Y < 14 and self.legs:
                return self.material(s, shade(self.legs, 0.92), "hide", key, x, y, face)
            if self.boots and Y >= 21:
                c = self.material(s, self.boots, "fur", key, x, y, face)
                if Y == 23:
                    return self.sh(c, 0.68)  # the worn hide sole
                if face == "front" and Y == 22 and x in (1, 2):
                    return self.sh(c, 1.1)  # the toe
                return self.sh(c, 0.85) if Y == 21 else c
            if self.legs and Y < self.legs_to:
                c = self.material(s, self.legs, "hide", key, x, y, face)
                if face in ("right", "left") and x == 2:  # side seam
                    c = self.sh(c, 0.85 if Y % 2 else 1.1)
                if Y in (17, 18) and face == "front" and (x + Y) % 2 == 0:
                    c = self.sh(c, 0.88)  # creases at the knee
                return c
            if self.wraps and Y >= self.legs_to and (Y + x // 2) % 2 == 0:
                return self.material(s, self.wraps, "cord", key, x, y, face)
            if self.body_paint and Y in (16, 17) and face == "front" and x in (1, 2):
                return shade(self.body_paint, side_shade(face))
            return None
        return None

    def necklace_at(self, x, Y):
        """Whether the necklace covers this front pixel of the body (it goes on the outer layer)."""
        return self.necklace is not None and self.neck_px(x, Y) is not None

    def armour_px(self, s, key, face, x, Y, last):
        """A pixel of the armour coat (rows 0..last of the body): overlapping scales, or lamellae laced in rows."""
        a = self.armour
        f = side_shade(face)
        if face == "top":
            return self.tint(a["base"], f * (1.08 if x % 2 else 0.96))
        if a["style"] == "scale":
            # rows of round-bottomed scales, each row a scale's width offset: lit at the top left, shadowed at the bottom right
            # (3 pixels wide, 2 tall): lit along the top left, the lower row in the shade of the row below, dark at the right
            row = Y // 2
            pos = (x + row * 2) % 3
            if Y % 2 == 0:
                c = a["hi"] if pos == 0 else mix(a["base"], a["hi"], 0.25) if pos == 1 else a["base"]
            else:
                c = a["lo"] if pos == 2 else mix(a["base"], a["lo"], 0.4)
        else:
            # lamellar: narrow vertical plates in bands of three rows, a row of lacing between the bands
            if Y % 3 == 2:
                c = a["lace"] if (x + Y) % 2 else shade(a["lace"], 0.8)
            else:
                c = a["hi"] if x % 2 == 0 and Y % 3 == 0 else a["base"] if x % 2 == 0 else mix(a["base"], a["lo"], 0.55)
        if Y == 0:
            c = shade(a["lace"], 0.9)  # the leather edging at the shoulders
        if Y == last:
            c = shade(c, 0.78)  # the lower edge, in shadow
        return self.tint(c, f * (1 + s.n(key + ("ar",), 0.06)))

    def collar_px(self, face, x, Y):
        """The embroidered edging: the two colours in a running chain, lit along the collar."""
        a, b = self.collar
        c = a if (x + Y) % 2 == 0 else b
        return self.tint(c, side_shade(face) * (1.08 if Y == 0 else 1.0))

    def strap_hit(self, face, x, Y):
        # from the right shoulder down to the left hip (front), mirrored on the back
        if face == "front":
            return Y <= 8 and x in (int(Y * 0.8), int(Y * 0.8) + 1) and x < 8
        return Y <= 8 and (7 - x) in (int(Y * 0.8), int(Y * 0.8) + 1)

    # ---------- head ----------
    def hair_px(self, s, key, x, y, f=1.0):
        streak = s.n(("hs",) + key[:2] + (x,), 0.1) + s.n(key + ("hp",), 0.05)
        if (x * 3 + y) % 7 == 0:
            streak += 0.14  # a lighter strand catching the light
        elif (x * 5 + y * 2) % 9 == 0:
            streak -= 0.12
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
            if self.gems and x in (3, 4):
                return self.gems[0] if x == 3 else shade(self.gems[0], 0.8)  # a stone on the brow
            return self.tint(self.headband, 1.0 if x % 3 else 0.82)
        if self.brow == "heavy" and y == 3 and x in (0, 1, 2, 5, 6, 7):
            return shade(self.hair, 0.8)
        if self.brow == "thin" and y == 3 and x in (1, 6):
            return shade(skin, 0.92)
        if self.scar and x == 5 and y in (3, 5) or self.scar and x == 6 and y == 6:
            return mix(skin, hexc("e8b8a8"), 0.55)  # an old scar across the cheek
        if self.freckles and y in (5, 6) and x in (1, 2, 5, 6) and (x + y) % 2 == 0:
            return shade(skin, 0.86)
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
        if y == 4 and x in (3, 4):
            return shade(skin, 1.06 if x == 3 else 0.97)  # the bridge of the nose, lit from one side
        if y == 5 and x in (3, 4):
            return shade(skin, 0.8 if x == 4 else 0.88)  # nose and its shadow
        if y == 6 and x in (3, 4):
            return mix(shade(skin, 0.7), hexc("a04838") if self.woman else OCHRE, 0.35 if self.woman else 0.25)  # mouth
        if y == 6 and x in (2, 5):
            return shade(skin, 0.9)  # the corners of the mouth
        if y == 5 and x in (1, 6) and self.woman:
            return mix(skin, hexc("c46a5a"), 0.18)  # cheeks
        if y == 5 and x in (2, 5):
            return shade(skin, 0.95)  # under the eyes
        if x in (0, 7) and 4 <= y <= 6:
            return self.face_paint("front", x, y, shade(skin, 0.92))  # the cheekbones turning away
        if y == 7:
            return self.face_paint("front", x, y, shade(skin, 0.9 if 2 <= x <= 5 else 0.86))  # the jaw in shadow
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
            if self.satchel and 8 <= Y <= 11 and (face == "front" and x >= 5 or face == "left" and x <= 2):
                # a satchel at the left hip on its strap: flap with a stitched edge and a toggle, bulging at the bottom
                c = self.material(s, self.satchel, "hide", key, x, y, face)
                if Y == 9:
                    return self.sh(c, 0.72)  # under the flap
                if Y == 8 and face == "front" and x == 6:
                    return self.tint(BONE, 0.9)
                return self.sh(c, 0.85) if Y == 11 or face == "left" and x == 2 else c
            return None
        if arm:
            if self.armour and Y <= 3 and face != "bottom":
                # pauldrons: scales over the shoulder, a scalloped lower edge
                if Y == 3 and x % 2:
                    return None
                return self.armour_px(s, key, face, x, Y + 1, 4)
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
        if kind == "gems":
            # a gold chain with a stone on every other link and a big one below
            if on_string or (Y == 2 and x in (3, 4)):
                return self.gems[(x + Y) % len(self.gems)] if (x + Y) % 2 == 0 else GOLD
            if Y == 3 and x in (3, 4):
                return self.gems[0] if x == 3 else shade(self.gems[0], 0.8)
            return None
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
        if style == "short" and not self.headband and face in ("right", "left", "back") and y <= (3 if face == "back" else 1) \
                and (face == "back" or fx <= 3) and (y < 3 or x % 2 == 0):
            return self.hair_px(s, key, x, y, 1.0 if y == 0 else 0.9)
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
        L.top, L.top_kind, L.sleeves = [HIDE_GREY, HIDE_LIGHT, HIDE, HIDE_GREY][i], "tunic" if woman else "vest", 0
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
        # the headband, the gatherer women's hem and the elder's trim take the tribe's colour; two of every four wear a
        # tunic dyed in it, against hide leggings and a dark belt
        if i in (1, 2) and role != "hunter":
            L.top = DYE
            L.belt = LEATHER if L.belt != CORD else CORD
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
        L.top, L.top_mat = (DYE if i == 2 and role != "elder" else coat), "fur" if role == "elder" else "hide"
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
# Trader Caravans (people/Caravans.java) travel far: one temperate set, no climate variants
CARAVAN_ROLES = ["trader", "guard"]


def caravan_person(role, i):
    """The caravan's old trader in a fine robe of the tribe's colour, gold-banded and set with gemstones; or a guard in
    clean linen and leather (the armour goes on top in game, never a helmet), the tribe's colour on the headband."""
    r = random.Random(f"{role}-{i}")
    woman = i % 2 == 1
    L = Look(r, woman, role)
    L.fine = True
    L.eyes = EYES[pick(r, ["brown", "dark", "blue", "green", "grey"])]
    L.wraps = None
    if role == "trader":
        L.skin = SKIN[["light", "olive", "tan", "pale"][i]]
        L.hair = HAIR[pick(r, ["grey", "white"])]
        L.wrinkles = True
        L.top, L.top_kind, L.top_mat, L.sleeves, L.skirt_to = DYE, "robe", "weave", 12, 21
        L.band, L.trim, L.belt = GOLD, GOLD, LEATHER
        L.gems = [GEMS[(i + k) % len(GEMS)] for k in range(3)]
        L.necklace = "gems"
        L.headband = GOLD
        L.legs, L.boots = None, LEATHER
        L.hair_style = pick(r, ["long", "bun"]) if woman else pick(r, ["bald_top", "short"])
        L.beard = None if woman else pick(r, ["long", "full"])
    else:
        L.skin = SKIN[["tan", "light", "brown", "olive"][i]]
        L.hair = HAIR[pick(r, ["black", "darkbrown", "brown", "auburn", "blond"])]
        L.top, L.top_kind, L.top_mat, L.sleeves, L.skirt_to = LINEN, "tunic", "weave", 4, 14
        L.belt, L.bracers = LEATHER, LEATHER
        L.legs, L.legs_to, L.boots = LEATHER, 22, LEATHER
        L.headband = DYE
        L.hair_style = pick(r, ["braids", "bun"]) if woman else pick(r, ["short", "short", "long"])
        L.beard = None if woman else pick(r, ["short", "stubble", "full"])
    return L


# ---- Trader Caravan kinds (people/TraderKinds.java, scripts/gen_traders.py): each kind dresses its leader (role "trader",
# an old man or woman), its animal handler and its guards in its own way; skins go in caravan/<kind>/<role>_<i>.png. The
# guards wear metal armour painted on the skin (scales or lamellae, pauldrons on the sleeve layer, tassets on the thighs),
# never leather, never a helmet; the tribe's colour (DYE) is on headbands, sashes and bands as for every tribe.
ARMOUR = {
    "bronze_scale": {"style": "scale", "base": hexc("a8743a"), "hi": hexc("e0b46c"), "lo": hexc("664020"), "lace": hexc("4a2e1a")},
    "bronze_lamellar": {"style": "lamellar", "base": hexc("9c6c36"), "hi": hexc("d4a462"), "lo": hexc("5c3a1c"), "lace": hexc("3e2a1a")},
    "iron_lamellar": {"style": "lamellar", "base": hexc("7a7e82"), "hi": hexc("b6babe"), "lo": hexc("46494c"), "lace": hexc("5a3822")},
    "iron_scale": {"style": "scale", "base": hexc("6c7074"), "hi": hexc("aaaeb2"), "lo": hexc("3e4246"), "lace": hexc("4a3020")},
    "gilded_scale": {"style": "scale", "base": hexc("b88a2c"), "hi": hexc("f4d47a"), "lo": hexc("6e4c14"), "lace": hexc("3a2440")},
}
MOSS, BARK_BROWN, SOOT_GREY, ROYAL, PLUM, SAND, SAGE, HERB_GREEN, WHEAT = (hexc("4e6a32"), hexc("6a4e30"), hexc("5c5a56"),
    hexc("2c3c8a"), hexc("5a2a5e"), hexc("c8b088"), hexc("8a9a6a"), hexc("5a7a3a"), hexc("c8a860"))
CARAVAN_KINDS = ["woodsman", "mining", "jewelry", "hunting", "herbs", "foraging", "eastern"]
INDIGO, CRIMSON, SAFFRON, EMERALD, SILK_CREAM = (hexc("2a3478"), hexc("a41e2e"), hexc("e2a224"), hexc("1e7a4c"),
    hexc("ece0c4"))
CARAVAN_KIND_ROLES = ["trader", "handler", "guard"]


def kind_person(kind, role, i):
    """A caravan member of {kind}: 0, 2 men, 1, 3 women. No tribe colour: each kind has its own colours and its own
    silhouette, read from afar (docs/traders-plan.md C). The leader is old and robed, the handler dressed for the road,
    the guards in armour over the kind's cloth with the kind's gear."""
    r = random.Random(f"{kind}-{role}-{i}")
    woman = i % 2 == 1
    L = Look(r, woman, role)
    L.fine = True
    L.folds = True
    L.wraps = None
    L.headband = None
    L.eyes = EYES[pick(r, ["brown", "dark", "blue", "green", "grey"])]
    L.skin = SKIN[pick(r, ["light", "tan", "olive", "pale", "brown", "dark"])]
    L.freckles = r.random() < 0.3
    L.scar = role == "guard" and r.random() < 0.5
    L.brow = pick(r, [None, "heavy", "thin", None])
    leader, handler, guard = role == "trader", role == "handler", role == "guard"
    if leader:
        L.hair = HAIR[pick(r, ["grey", "white", "grey"])]
        L.wrinkles = True
        L.hair_style = pick(r, ["long", "bun", "braids"]) if woman else pick(r, ["bald_top", "short", "long"])
        L.beard = None if woman else pick(r, ["long", "full", "long"])
        L.top_kind, L.sleeves, L.skirt_to = "robe", 12, 21
        L.legs, L.boots, L.belt = None, LEATHER, LEATHER
    else:
        L.hair = HAIR[pick(r, ["black", "darkbrown", "brown", "auburn", "blond"])]
        L.hair_style = pick(r, ["braids", "bun", "long"]) if woman else pick(r, ["short", "shaggy", "short", "long"])
        L.beard = None if woman else pick(r, ["stubble", "short", "full", None])
        L.top_kind, L.sleeves, L.skirt_to = "tunic", 4 if guard else 12, 14
        L.legs, L.legs_to, L.boots, L.belt = LEATHER, 22, LEATHER, LEATHER
        if guard:
            L.bracers = LEATHER
    if kind == "woodsman":  # timber folk: red-and-green plaid wool, a leather apron, an axe across the back
        L.top, L.top_mat = pick(r, [hexc("9a3a2a"), hexc("3e5a2e")]), "plaid"
        L.plaid = (hexc("1e2a1a"), hexc("c8b890"))
        L.legs = hexc("5a4630") if L.legs else None
        if leader:
            L.top = hexc("6a4a2e")  # a long brown wool coat over the plaid sleeves
            L.top_mat = "weave"
            L.fur, L.fur_kind, L.mantle = FUR_BROWN, "collar", None
            L.apron, L.apron_mat, L.apron_to = LEATHER, "hide", 16
        elif handler:
            L.apron, L.apron_mat, L.apron_to = LEATHER, "hide", 17
            L.bracers = HIDE
            L.back_item = "axe"
        else:
            L.armour, L.cloak = ARMOUR["bronze_scale"], None
            L.back_item = "axe"
    elif kind == "mining":  # miners: sooty leather aprons, padded caps, a lantern at the belt, iron lamellae
        L.top, L.top_mat = SOOT_GREY, "hide"
        L.paint = (CHARCOAL, "dots")
        L.skin = shade(L.skin, 0.9)
        L.cap = pick(r, [hexc("6a5a46"), hexc("4e463c")])
        L.hips = ("lantern",)
        if leader:
            L.apron, L.apron_mat, L.apron_to = hexc("2e2620"), "hide", 18
            L.necklace = "amber"
        elif handler:
            L.apron, L.apron_mat, L.apron_to = hexc("2e2620"), "hide", 17
            L.pouch = LEATHER
        else:
            L.armour, L.cloak = ARMOUR["iron_lamellar"], hexc("3e3a36")
    elif kind == "jewelry":  # from the desert: fine dyed robes with gold trim and gemstones, turbans, gilded scales
        L.skin = SKIN[pick(r, ["olive", "brown", "tan", "dark"])]
        if not leader:
            L.hair = HAIR[pick(r, ["black", "darkbrown"])]
        robe = pick(r, [ROYAL, PLUM, hexc("8a1e2e")])
        L.top, L.top_mat = robe, "weave"
        L.band, L.trim = GOLD, GOLD
        L.gems = [GEMS[(i + k) % len(GEMS)] for k in range(3)]
        L.headwrap = pick(r, [SAND, hexc("e8dcc0"), GOLD])
        L.belt = GOLD
        if leader:
            L.necklace = "gems"  # fine gem-studded clothes
        elif handler:
            L.top_kind, L.skirt_to = "robe", 18
            L.legs, L.boots, L.wraps, L.legs_to = LINEN, None, CORD, 20  # sandals
            L.necklace = "gems"
        else:
            L.armour, L.cloak = ARMOUR["gilded_scale"], hexc("8a1e2e")
            L.band, L.gems = None, None
    elif kind == "hunting":  # hunters: fur-trimmed hoods and capes, a quiver, ochre stripes
        L.top, L.top_mat = HIDE_DARK, "hide"
        L.paint = (OCHRE, "stripes")
        L.hood, L.hood_trim = pick(r, [HIDE_DARK, HIDE_RED, HIDE]), pick(r, [FUR_WHITE, FUR_GREY])
        L.mantle = pick(r, [FUR_GREY, FUR_BROWN, FUR_RED])
        L.cloak = pick(r, [HIDE_DARK, LEATHER, HIDE_RED])
        L.back_item = "quiver"
        if leader:
            L.top, L.top_mat = FUR_BROWN, "fur"
            L.necklace = "teeth"
            L.back_item = None  # his staff, not a bow
        elif handler:
            L.necklace = "teeth"
        else:
            L.armour = ARMOUR["bronze_lamellar"]
    elif kind == "herbs":  # herb folk: green or ochre robes, satchels with herbs sticking out, dried bundles in the belt
        L.top, L.top_mat = pick(r, [HERB_GREEN, hexc("b0862e")]), "weave"
        L.top_kind, L.skirt_to = "robe", 19 if not leader else 21
        L.trim = pick(r, [STRAW, LINEN])
        L.belt = BARK
        L.belt_bundles = hexc("8a8a4a")
        L.hips = ("satchel", "bundle")
        L.necklace = "amber" if leader else None
        L.legs = HIDE_LIGHT if L.legs else None
        if guard:
            L.armour, L.cloak = ARMOUR["bronze_scale"], HERB_GREEN
            L.top_kind, L.skirt_to = "tunic", 14
            L.belt_bundles = None
    elif kind == "eastern":  # spice traders from the far east, along the silk road: ikat-striped quilted coats, a wide
        # silk sash, tall embroidered felt caps (some fur-trimmed), indigo trousers and soft red boots
        L.skin = SKIN[pick(r, ["light", "tan", "olive", "pale", "tan"])]
        if not leader:
            L.hair = HAIR[pick(r, ["black", "darkbrown", "black"])]
        # a ground colour with two others between its stripes, so the coat reads as one colour from afar
        palettes = [[CRIMSON, SAFFRON, CRIMSON, INDIGO], [INDIGO, SILK_CREAM, INDIGO, CRIMSON],
                    [EMERALD, SAFFRON, EMERALD, CRIMSON], [SAFFRON, CRIMSON, SAFFRON, EMERALD]]
        L.top_mat, L.ikat = "ikat", (palettes[(i + {"trader": 0, "handler": 1, "guard": 2}[role]) % 4], 3)
        L.top = L.ikat[0][0]
        L.sash = pick(r, [c for c in (SAFFRON, EMERALD, CRIMSON, SILK_CREAM) if c not in L.ikat[0][:2]])
        L.belt = None
        L.legs = hexc("262a48") if L.legs else None
        L.boots = pick(r, [hexc("7a2822"), hexc("5a3020")])
        L.cap, L.cap_style = pick(r, [hexc("1e2a5a"), hexc("6a1a24"), hexc("1a4a32"), hexc("e8dcc0")]), "tall"
        L.cap_work = [GOLD, pick(r, [SILK_CREAM, CRIMSON, EMERALD])]
        L.cap_trim = pick(r, [None, FUR_BROWN, None, hexc("3a2a1e")])
        if leader:
            L.collar = (GOLD, CRIMSON if L.ikat[0][0] != CRIMSON else INDIGO)
            L.cap_trim = pick(r, [FUR_WHITE, FUR_GREY, None])
        elif handler:
            L.skirt_to = 17
            L.strap, L.satchel = LEATHER, hexc("6e4a2a")
        else:
            L.armour, L.sleeves, L.skirt_to = ARMOUR["iron_lamellar"], 12, 17
            L.cloak = pick(r, [CRIMSON, INDIGO, EMERALD])
            L.cloak_border = SAFFRON if L.cloak != SAFFRON else CRIMSON
    else:  # foraging (and any kind without a look of its own): earthy patchwork, a woven basket on the back
        L.top, L.top_mat = hexc("8a6a46"), "patch"
        L.patches = [hexc("8a6a46"), hexc("6a5a3a"), hexc("a08a5a"), hexc("5e4a32"), hexc("7a7046")]
        L.belt = CORD
        L.pouch = BARK
        if leader:
            L.pack = "basket"
            L.necklace = "shell"
        elif handler:
            L.pack = "basket"
        else:
            L.armour, L.cloak = ARMOUR["iron_scale"], hexc("6a5a3a")
            L.top_mat = "hide"
    if guard:
        L.band = None  # the armour covers the coat
    return L


MINER_SKINS = [0, 2]


def miner_person(i):
    """The Miner: a sooty, bearded man in worn smoked hide and a leather apron, the tribe's colour on his headband."""
    r = random.Random(f"miner-{i}")
    L = Look(r, False, "miner")
    L.skin = SKIN[["light", "olive", "tan", "brown"][i]]
    L.eyes = EYES[pick(r, ["brown", "dark", "grey", "blue"])]
    L.hair = HAIR[pick(r, ["darkbrown", "black", "grey", "auburn"])]
    L.hair_style = pick(r, ["shaggy", "short"])
    L.beard = pick(r, ["full", "long"])
    L.paint = (CHARCOAL, "dots")  # soot
    L.top, L.top_kind, L.sleeves = HIDE_GREY, "tunic", 4
    L.apron = LEATHER
    L.belt, L.pouch = LEATHER, LEATHER
    L.legs, L.boots, L.bracers = HIDE_DARK, LEATHER, LEATHER
    L.headband = DYE
    L.skin = shade(L.skin, 0.92)  # grimy
    return L


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


# ---------------- gear: the parts that stand out from the body (client/TribeGearModel, drawn by EquipmentLayer) ----------
# name: (u, v, w, h, d) -- mirrors the texOffs and box sizes in client/TribeGearModel.java: keep them in sync.
GEAR_BOXES = {
    "hood": (0, 0, 8, 8, 8), "ruff": (32, 0, 10, 10, 1),
    "topknot": (0, 16, 3, 3, 3), "beard": (12, 16, 6, 5, 1), "braid_r": (26, 16, 1, 9, 1), "braid_l": (30, 16, 1, 9, 1),
    "crest_l": (34, 16, 1, 6, 1), "crest_m": (38, 16, 1, 8, 1), "crest_r": (42, 16, 1, 6, 1), "feather": (46, 16, 1, 5, 1),
    "mantle": (0, 32, 12, 4, 6), "cloak": (36, 32, 9, 13, 1), "neck": (0, 48, 8, 5, 1),
    "pack": (22, 48, 6, 7, 4), "hip_a": (44, 48, 2, 3, 2), "hip_b": (52, 48, 2, 3, 2),
}
IRON, LANTERN_GLOW, HERB_GREENS = hexc("4a4a4e"), hexc("ffd56a"), [hexc("5a8a3a"), hexc("7aa04a"), hexc("8a9a4a")]


class Gear:
    """One person's gear texture (64x64), painted from their Look; empty parts stay see-through (not drawn)."""

    def __init__(self, L, seed):
        self.L = L
        self.r = random.Random(seed)
        self.img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
        self.used = False

    def fill(self, name, paint):
        u, v, w, h, d = GEAR_BOXES[name]
        for face, (x0, y0, fw, fh) in faces(u, v, w, h, d).items():
            for y in range(fh):
                for x in range(fw):
                    c = paint(face, x, y, fw, fh)
                    if c is not None:
                        self.img.putpixel((x0 + x, y0 + y), clamp(c) + (255,))
                        self.used = True

    def fur(self, col, face, x, y, salt=0):
        """Long fur: vertical locks, dark roots, light tips, darker on the shaded faces."""
        lock = h_(x * 7 + salt, 3) * 0.22 - 0.11
        tip = 1.12 if (y + (x * 5 + salt) % 3) % 4 == 3 else 1.0
        return shade(col, side_shade(face) * (1 + lock) * tip * (0.9 + 0.1 * h_(x * 13 + y * 7 + salt, 5)))

    def hair(self, face, x, y, salt=0):
        k = 1.0 + (0.16 if (x * 3 + y + salt) % 5 == 0 else 0) - (0.12 if (x + y * 2 + salt) % 7 == 0 else 0)
        return shade(self.L.hair, side_shade(face) * k)


def h_(n, salt):
    n = (n * 374761393 + salt * 668265263) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def gear(L, zone, seed):
    """The gear image for a person, or None when they wear nothing that stands out."""
    g = Gear(L, seed)
    cold, warm, jungle = zone == "cold", zone == "warm", zone == "jungle"
    # ---- a fur hood: over the head with an open face, and a thick ruff of fur round the face
    if L.hood:
        trim = L.hood_trim or FUR_WHITE
        leather = L.hood in (HIDE, HIDE_LIGHT, HIDE_DARK, HIDE_RED, HIDE_GREY, LEATHER)

        def towards_face(face, x, y, fw, fh):
            """How many pixels this one is from the hood's front edge (None on the back)."""
            if face == "top":
                return fh - 1 - y
            if face in ("right", "left"):
                return fw - 1 - x if face == "right" else x
            return None

        def hood(face, x, y, fw, fh):
            if face == "bottom":
                return None
            if face == "front":
                return g.fur(L.hood, face, x, y) if y == 0 or x in (0, fw - 1) else None
            if not leather:
                return g.fur(L.hood, face, x, y, 3)
            # a hood sewn of hide: two halves with a stitched seam over the crown and down the back, the back running
            # down to a point between the shoulders, darker towards the back and below, the trim's fur wrapping round
            # onto the sides and the crown from the ruff (its hairs reaching back over the hide)
            d = towards_face(face, x, y, fw, fh)
            if d is not None and (d <= 1 or d == 2 and h_(x * 11 + y * 5, 23) > 0.45):
                return g.fur(trim, face, x, y, 7)
            if face == "back" and (y == fh - 1 and x not in (fw // 2 - 1, fw // 2) or y == fh - 2 and x in (0, fw - 1)):
                return None
            if face in ("right", "left") and y == fh - 1 and x % 2:
                return None  # the edge lying in folds on the shoulder
            f = side_shade(face) * (1 + 0.07 * (h_(x * 17 + y * 3, 29) - 0.5))
            f *= 1.0 - 0.03 * (y if face != "top" else 0)  # the crown catches the light, the lower hood less
            if d is not None:
                f *= 1.0 - 0.025 * max(0, d - 3)
            if face == "top" and x in (fw // 2 - 1, fw // 2) or face == "back" and x == fw // 2:
                f *= 0.72 if y % 2 else 0.88  # the seam, stitched
            return shade(L.hood, f)
        g.fill("hood", hood)

        def ruff(face, x, y, fw, fh):
            # a thick ruff round the face: ragged where it frames the face (hairs reaching in), darker deep in the
            # fur and lighter at the tips, shaggy below; open under the chin
            if face == "bottom":
                return None
            if face == "front" or face == "back":
                edge = min(x, fw - 1 - x, y)
                inside = 2 <= x <= fw - 3 and 2 <= y <= fh - 2
                if inside and not (edge == 2 and y < fh - 3 and h_(x * 7 + y * 13, 31) > 0.6):
                    return None
                if y == fh - 1 and (x + g.r.randrange(2)) % 2:
                    return None  # a shaggy lower edge
                return shade(g.fur(trim, face, x, y, 7), 0.86 if edge >= 1 and not (edge == 1 and y == 1) else 1.06)
            return g.fur(trim, face, x, y, 7)
        g.fill("ruff", ruff)
    # ---- hair with volume: a top-knot, braids hanging over the shoulders, a beard down the chest
    if L.hair_style == "bun" and not L.hood and not L.headwrap:
        band = L.headband if L.headband is not None and not isinstance(L.headband, Dye) else CORD
        g.fill("topknot", lambda face, x, y, fw, fh: shade(band, side_shade(face)) if face != "top" and y == fh - 1
               else g.hair(face, x, y, 1))
    if L.hair_style == "braids" and not L.hood:
        for name in ("braid_r", "braid_l"):
            def braid(face, x, y, fw, fh):
                if face == "top":
                    return None
                if y >= fh - 2:
                    tie = BONE if L.role in ("elder", "hunter") else (FEATHERS[2] if jungle else CORD)
                    return shade(tie, side_shade(face)) if y == fh - 2 else g.hair(face, x, y, 2)
                return g.hair(face, x, y, 4) if (y + (face in ("left", "right"))) % 2 == 0 else shade(g.hair(face, x, y, 4), 0.72)
            g.fill(name, braid)
    if L.beard in ("full", "long"):
        long_ = L.beard == "long"

        def beard(face, x, y, fw, fh):
            if face in ("top",):
                return None
            if y >= fh - (1 if long_ else 3):
                return None if not long_ or (x in (0, fw - 1)) or (x + y) % 2 else g.hair(face, x, y, 6)
            if y >= fh - 2 - (0 if long_ else 1) and x in (0, fw - 1):
                return None  # tapering to a point
            c = g.hair(face, x, y, 6)
            return shade(c, 1.08) if L.hair in (HAIR["grey"], HAIR["white"]) else c
        g.fill("beard", beard)
    # ---- feathers: a crest standing up from the back of the head (jungle), or one eagle feather
    if L.headdress:
        cols = L.headdress
        for k, name in enumerate(("crest_l", "crest_m", "crest_r")):
            col = cols[k % len(cols)]
            col = DYE_STAND if isinstance(col, Dye) else col

            def crest(face, x, y, fw, fh, col=col):
                if y == 0:
                    return shade(col, 1.15)
                if y >= fh - 1:
                    return shade(BONE, 0.9)  # the quill
                return shade(col, side_shade(face) * (1.0 if y % 3 else 0.82))
            g.fill(name, crest)
    elif L.feather:
        g.fill("feather", lambda face, x, y, fw, fh: hexc("3a3030") if y == 0 else (hexc("f0ece2") if y < fh - 1 else BONE))
    # ---- a fur mantle round the shoulders: everyone in the cold, the elders everywhere but the hot lands
    mantle = L.mantle
    if mantle is not None:
        pass
    elif cold:
        mantle = L.fur if L.fur and L.fur not in FEATHERS else pick(g.r, [FUR_WHITE, FUR_GREY, FUR_BROWN, FUR_RED])
    elif L.role == "elder" and not warm and not jungle:
        mantle = L.fur or FUR_GREY
    elif L.role == "woodcutter" and zone == "temperate":
        mantle = FUR_BROWN if L.top != FUR_BROWN else FUR_GREY
    if mantle is not None:
        def mant(face, x, y, fw, fh):
            if face == "bottom":
                return shade(mantle, 0.6)
            if face != "top" and y == fh - 1 and (x + g.r.randrange(3)) % 3 == 0:
                return None  # ragged hem
            return g.fur(mantle, face, x, y, 11)
        g.fill("mantle", mant)
    # ---- a hide cloak down the back: hunters (temperate) and guards; a linen one in the warm lands for elders
    cloak = L.cloak
    if cloak is not None:
        pass
    elif L.role in ("hunter", "guard") and zone == "temperate":
        cloak = pick(g.r, [HIDE_DARK, HIDE_GREY, LEATHER, HIDE_RED])
    elif warm and L.role == "elder":
        cloak = LINEN
    if cloak is not None and mantle is None and not L.quiver:
        edge = FUR_GREY if not warm else shade(LINEN, 0.85)

        def clk(face, x, y, fw, fh):
            if face in ("bottom", "top"):
                return shade(cloak, 0.7)
            if y == fh - 1 and (x + g.r.randrange(2)) % 2:
                return None
            if L.cloak_border is not None:
                # silk, gathered at the shoulders into soft folds that open out toward the hem, an embroidered border
                if y >= fh - 3:
                    if y == fh - 3 and x % 2:
                        return shade(cloak, side_shade(face) * 0.9)  # the border's stitched upper edge
                    return shade(L.cloak_border, side_shade(face) * (0.8 if y == fh - 2 else 1.0 if (x + y) % 2 else 0.9))
                k = 1.0 if y == 0 else 0.82 if (x + (y > 5)) % 3 == 0 else 1.1 if (x + (y > 5)) % 3 == 1 else 1.0
                return shade(cloak, side_shade(face) * k * (1.08 if y == 0 else 1.0))
            if y == 0:
                return g.fur(edge, face, x, y, 13)  # fur along the shoulders
            c = shade(cloak, side_shade(face) * (1 + 0.06 * (h_(x * 31 + y * 7, 17) - 0.5)) * (0.9 if x % 3 == 0 else 1.0))
            return shade(c, 0.85) if y == fh - 2 else c
        g.fill("cloak", clk)
    # ---- a big necklace standing off the chest: teeth, claws, bone, shells, amber, fishbone or gems
    kind = L.necklace
    if kind and mantle is None:
        bead = {"teeth": TOOTH, "bone": BONE, "fishbone": BONE, "shell": hexc("e9d9c6"), "amber": AMBER, "gems": GOLD}[kind]
        cord = GOLD if kind == "gems" else hexc("5a4630")

        def neck(face, x, y, fw, fh):
            if face != "front":
                return None
            lo = 3 - abs(x * 2 - 7) // 2  # the V: low in the middle
            if y == lo - 1 or (y < lo and x in (0, fw - 1) and y == 0):
                return cord
            if y == lo + 1 and x % 2 == 1 and kind in ("teeth", "bone", "shell") and lo < 3:
                return shade(bead, 0.85)  # every other tooth hangs longer
            if y == lo:
                if kind == "gems":
                    return L.gems[x % len(L.gems)]
                if kind == "shell":
                    return hexc("e9d9c6") if x % 4 == 0 else hexc("c96a4a")
                if kind == "amber":
                    return AMBER if x in (3, 4) else (hexc("8a6a3a") if x % 2 else hexc("c8a060"))
                return bead if x % 2 == 0 else ([OCHRE, hexc("2e5fa8"), hexc("e8e2d0")][x % 3] if kind != "fishbone" else bead)
            if kind in ("teeth", "fishbone") and y == lo + 1 and x in (3, 4):
                return TOOTH if kind == "teeth" else BONE  # the big fang (or a fish spine) at the bottom
            if kind == "amber" and y == lo + 1 and x in (3, 4):
                return shade(AMBER, 0.8)
            return None
        g.fill("neck", neck)
    caravan_gear(g, L)
    return g.img if g.used else None


def caravan_gear(g, L):
    """What caravan folk carry that shows from afar: an axe or a quiver on the back, a basket on the back, a padded cap,
    things at the hips (a lantern, a satchel with herbs sticking out, a dried bundle)."""
    if L.cap is not None and L.cap_style == "tall":
        cap, work, trim = L.cap, L.cap_work, L.cap_trim

        def tall(face, x, y, fw, fh):
            if face == "bottom":
                return None
            if face == "top":
                ring = max(abs(x * 2 - 7), abs(y * 2 - 7))  # rings of embroidery round the crown
                return shade(work[ring // 2 % len(work)] if ring in (3, 7) else cap, 1.06 if ring > 3 else 0.94)
            low = 3 if face != "front" else 2
            if y > low:
                return None
            if y == low:
                return g.fur(trim, face, x, y, 21) if trim else shade(work[0], side_shade(face) * 0.8)  # the brim
            if y == 1 and (x + (face in ("left", "right"))) % 3 == 1:
                return shade(work[1 % len(work)], side_shade(face))  # a running band of almond motifs
            if y == 0 and x % 3 == 1 or y == 2 and x % 3 == 1:
                return shade(work[0], side_shade(face) * 0.95)
            return shade(cap, side_shade(face) * (1.04 if (x + y) % 2 else 0.96))
        g.fill("hood", tall)

        def crown(face, x, y, fw, fh):
            if face == "bottom":
                return None
            if face == "top":
                return shade(work[0], 1.1 if x == 1 and y == 1 else 0.9)  # the knot of the tassel
            if y == fh - 1:
                return shade(work[0], side_shade(face) * 0.8)
            return shade(cap, side_shade(face) * (1.08 if y == 0 else 1.0) * (0.9 if x == fw - 1 else 1.0))
        g.fill("topknot", crown)
    elif L.cap is not None:
        cap = L.cap

        def capf(face, x, y, fw, fh):
            if face == "bottom":
                return None
            if face == "top":
                return shade(cap, 1.08 if (x + y) % 3 else 0.86)  # quilted: stitched in a diamond
            if y <= 2:
                return shade(cap, side_shade(face) * (0.8 if y == 2 else 1.0 if (x + y) % 3 else 0.88))
            return None
        g.fill("hood", capf)
    if L.back_item == "axe":
        def axe(face, x, y, fw, fh):
            if face not in ("back", "front"):
                return None
            fx = x if face == "back" else fw - 1 - x
            if fx == fw - 2 - (y * (fw - 3)) // (fh - 1) and y >= 2:
                if y >= fh - 3:
                    return shade(LEATHER, 0.9 if y % 2 else 1.1)  # the grip, bound in leather
                return shade(hexc("b8925a"), 1.0 if y % 2 else 0.88)  # the pale ash haft, slung across the back
            if y <= 3 and fw - 4 <= fx <= fw - 1:
                if y == 0 or y == 3 or fx == fw - 4:
                    return hexc("4a4c50")  # the bit's dark outline (it reads against any cloth)
                return hexc("dfe3e6") if fx == fw - 1 else hexc("a8acb0")  # the honed edge bright, the cheek grey
            return None
        g.fill("cloak", axe)
    if L.back_item == "quiver":
        def quiver(face, x, y, fw, fh):
            # a birch-bark quiver up by the left shoulder (pale: it reads against the hide), three fletched arrows
            # standing out of it, leather bands, and its strap running across the back to the right hip
            if face == "top":
                fx = x
                return [hexc("f2ece0"), OCHRE, hexc("2e2824")][fx - 1] if fx in (1, 2, 3) else None
            if face not in ("back", "front"):
                return None
            fx = x if face != "front" else fw - 1 - x
            if fx in (1, 2, 3) and y <= 10:
                if y <= 1:
                    feather = [hexc("f2ece0"), OCHRE, hexc("2e2824")][(fx - 1 + y) % 3]
                    return shade(feather, 1.0 if y == 0 else 0.86)  # fletching, the vanes crossed
                if y in (2, 6, 10):
                    return shade(LEATHER, 0.9 if fx == 3 else 1.05)  # the rim and the bands
                birch = shade(hexc("e6dcc4"), 0.9 if y % 4 == 1 else 1.0)  # bark rings between the bands
                return shade(birch, 0.82 if fx == 3 else 0.94 if fx == 1 else 1.0)
            if 4 <= fx <= fw - 2 and y >= 3 and fx == 4 + (y - 3) * (fw - 6) // (fh - 4):
                return shade(LEATHER, 0.85 if y % 2 else 1.0)  # the strap
            return None
        g.fill("cloak", quiver)
    if L.pack == "basket":
        def basket(face, x, y, fw, fh):
            if face == "top":
                if x in (0, fw - 1) or y in (0, fh - 1):
                    return shade(STRAW, 0.9)  # the rim
                return [hexc("b0302a"), hexc("3a4a8a"), hexc("a07850"), hexc("6a8a3a")][(x * 3 + y) % 4]  # berries, nuts, greens
            k = 1.12 if (x + y) % 2 == 0 else 0.84
            if y == 0:
                k *= 1.1  # the rim
            return shade(STRAW if face != "front" else BARK, side_shade(face) * k)
        g.fill("pack", basket)
    for box, what in zip(("hip_a", "hip_b"), L.hips):
        if what == "lantern":
            def lantern(face, x, y, fw, fh):
                if face in ("top", "bottom"):
                    return IRON
                return IRON if y == 0 or y == fh - 1 or x in (0, fw - 1) and fw > 1 and y == 1 else LANTERN_GLOW
            g.fill(box, lantern)
        elif what == "satchel":
            def satchel(face, x, y, fw, fh):
                if face == "top":
                    return HERB_GREENS[(x + y) % 3]  # herbs sticking out
                if y == 0:
                    return HERB_GREENS[x % 3] if (x + len(face)) % 2 else shade(LEATHER, 1.1)
                return shade(hexc("7a5434"), side_shade(face) * (0.85 if y == fh - 1 else 1.0))
            g.fill(box, satchel)
        elif what == "bundle":
            def bundle(face, x, y, fw, fh):
                if face == "bottom":
                    return None
                if y == 0 or face == "top":
                    return CORD
                return shade(hexc("8a8a4a") if (x + y) % 2 else hexc("6a6a3a"), side_shade(face))  # dried stalks
            g.fill(box, bundle)


WOOD_HAFT = hexc("7a5434")


DYE_STAND = hexc("b04a30")  # a crest feather in the tribe's colour (gear is not dyed in game: a fixed red)


def save_gear(L, zone, seed, path):
    """Writes the person's gear texture next to the skin (or removes a stale one)."""
    g = gear(L, zone, seed)
    if g is not None:
        g.save(path)
    elif path.exists():
        path.unlink()


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
                save_gear(L, zone, f"gear{role}{i}{zone}", out / f"{role}_{i}_gear.png")
                made[(zone, role, i)] = (img, sk.mask, L.woman)
    for role in CARAVAN_ROLES:
        for i in range(4):
            L = caravan_person(role, i)
            sk = Skin(f"{role}{i}", L.woman)
            img = sk.build(L)
            assert sk.mask.getbbox() is not None, f"{role}_{i}: nothing in the tribe's colour"
            img.save(OUT / f"{role}_{i}.png")
            sk.mask.save(OUT / f"{role}_{i}_tint.png")
            save_gear(L, "temperate", f"gear{role}{i}", OUT / f"{role}_{i}_gear.png")
            made[("caravan", role, i)] = (img, sk.mask, L.woman)
    for kind in CARAVAN_KINDS:
        out = OUT / "caravan" / kind
        out.mkdir(parents=True, exist_ok=True)
        for role in CARAVAN_KIND_ROLES:
            for i in range(4):
                L = kind_person(kind, role, i)
                sk = Skin(f"{kind}{role}{i}", L.woman)
                img = sk.build(L)
                assert sk.mask.getbbox() is None, f"caravan/{kind}/{role}_{i}: caravans wear their kind's colours, not the tribe's"
                img.save(out / f"{role}_{i}.png")
                (out / f"{role}_{i}_tint.png").unlink(missing_ok=True)
                save_gear(L, "temperate", f"gear{kind}{role}{i}", out / f"{role}_{i}_gear.png")
                made[("caravan_" + kind, role, i)] = (img, sk.mask, L.woman)
    for i in MINER_SKINS:
        L = miner_person(i)
        sk = Skin(f"miner{i}", L.woman)
        img = sk.build(L)
        assert sk.mask.getbbox() is not None, f"miner_{i}: nothing in the tribe's colour"
        img.save(OUT / f"miner_{i}.png")
        sk.mask.save(OUT / f"miner_{i}_tint.png")
        save_gear(L, "temperate", f"gearminer{i}", OUT / f"miner_{i}_gear.png")
        made[("miner", "miner", i)] = (img, sk.mask, L.woman)
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
    sheet = Image.new("RGBA", (cols * 18 * scale, len(CARAVAN_ROLES) * 34 * scale), (70, 90, 70, 255))
    for row, role in enumerate(CARAVAN_ROLES):
        for i in range(4):
            img, mask, slim = made[("caravan", role, i)]
            img = dyed(img, mask, PREVIEW_DYES[(row + i) % len(PREVIEW_DYES)])
            for k, back in enumerate((False, True)):
                v = flat_view(img, slim, back).resize((16 * scale, 32 * scale), Image.NEAREST)
                sheet.alpha_composite(v, (((i * 2 + k) * 18 + 1) * scale, (row * 34 + 1) * scale))
    sheet.save(out_dir / "tribe_sheet_caravan.png")
    sheet = Image.new("RGBA", (len(MINER_SKINS) * 2 * 18 * scale, 34 * scale), (70, 90, 70, 255))
    for n, i in enumerate(MINER_SKINS):
        img, mask, slim = made[("miner", "miner", i)]
        img = dyed(img, mask, PREVIEW_DYES[n])
        for k, back in enumerate((False, True)):
            v = flat_view(img, slim, back).resize((16 * scale, 32 * scale), Image.NEAREST)
            sheet.alpha_composite(v, (((n * 2 + k) * 18 + 1) * scale, scale))
    sheet.save(out_dir / "tribe_sheet_miner.png")
    for kind in CARAVAN_KINDS:
        sheet = Image.new("RGBA", (cols * 18 * scale, len(CARAVAN_KIND_ROLES) * 34 * scale), (70, 90, 70, 255))
        for row, role in enumerate(CARAVAN_KIND_ROLES):
            for i in range(4):
                img, mask, slim = made[("caravan_" + kind, role, i)]
                img = dyed(img, mask, PREVIEW_DYES[(row + i) % len(PREVIEW_DYES)])
                for k, back in enumerate((False, True)):
                    v = flat_view(img, slim, back).resize((16 * scale, 32 * scale), Image.NEAREST)
                    sheet.alpha_composite(v, (((i * 2 + k) * 18 + 1) * scale, (row * 34 + 1) * scale))
        sheet.save(out_dir / f"tribe_sheet_caravan_{kind}.png")


if __name__ == "__main__":
    made = build()
    print(f"wrote {len(made)} skins")
    if len(sys.argv) > 1:
        preview(made, sys.argv[1])
