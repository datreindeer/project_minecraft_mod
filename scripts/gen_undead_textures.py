"""Skins for the varied undead (com.mechanical.Undead, client.UndeadRenderers), into
assets/mechanical/textures/entity/undead/:

- zombie_<look>.png (64x64, the vanilla zombie layout: head/hat/body/right arm/right leg in the top half, left limbs
  mirror the right). The people are painted with the tribe-skin painter (gen_tribe_skins.py: hide and fur clothing,
  braids, beards, necklaces, war paint) and then turned into corpses: green-grey rotting flesh, dark eye sockets,
  bruises and wounds, torn clothes; the bog body is brown and leathery with moss, the gaunt one shows its ribs.
  The player-format outer layers of body/arms/legs are flattened onto the base (the zombie model has only the hat).
- skeleton_<look>.png (64x32, the vanilla skeleton layout, 2x2 limbs, see-through ribcage): mossy, hide (hide
  scraps, cord, tooth necklace, feathers) and bog (tea-brown bones, a few strands of red hair). The plain look is the
  vanilla skeleton texture.

Run: python scripts/gen_undead_textures.py [preview_dir]"""
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_tribe_skins as T  # noqa: E402
from gen_tribe_skins import hexc, shade, mix, clamp, faces, side_shade  # noqa: E402

OUT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures/entity/undead"

# ---------------------------------------------------------------- zombies

ROT_DARK = hexc("3c3a2c")
BRUISE = hexc("4e3c4a")
WOUND = hexc("5c1a14")
WOUND_DEEP = hexc("2e0c0a")
MOSS = [hexc("3f5a26"), hexc("566f2f"), hexc("6f8a3a"), hexc("88a04a")]
SOCKET = hexc("16110e")
MILKY = hexc("b9bca2")


def chroma(c):
    s = sum(c) or 1
    return (c[0] / s, c[1] / s, c[2] / s), s


class Corpse(T.Look):
    """A tribe-skin Look that has been dead for a while."""

    def __init__(self, r, woman, flesh):
        super().__init__(r, woman, "zombie")
        self.skin = flesh
        self.eyes = SOCKET
        self.rot = 0.3        # share of flesh in bruised blotches
        self.wounds = 0.035   # share of flesh pixels that are open wounds
        self.tear = 0.12      # share of clothing torn away
        self.moss = 0.0       # share of top surfaces under moss
        self.ribs = False
        self.milky = False    # clouded eyes
        self.body_paint = None
        self.blood = 0.04     # old blood on clothing

    def flesh(self, c):
        (a, b, d), s = chroma(c)
        (fa, fb, fd), fs = chroma(self.skin)
        return abs(a - fa) + abs(b - fb) + abs(d - fd) < 0.05 and 0.55 * fs < s < 1.2 * fs

    def decay(self, s, key, c, top=False):
        """Rot, wounds and moss on a flesh pixel."""
        part, face, x, y = key[:4]
        blot = s.n(("blot", part, face, x // 2, y // 2), 1)
        if blot > 1 - self.rot * 2:
            c = mix(c, BRUISE if s.n(("bk", part, face, x // 2, y // 2), 1) > 0.2 else ROT_DARK, 0.32 + 0.15 * (blot > 1 - self.rot))
        w = s.n(("wd", part, face, x, y), 1)
        if w > 1 - self.wounds * 2:
            c = WOUND_DEEP if s.n(("wd2", part, face, x, y), 1) > 0.4 else WOUND
        c = self.mossy(s, key, c, top)
        return c

    def mossy(self, s, key, c, top):
        if not self.moss:
            return c
        part, face, x, y = key[:4]
        m = s.n(("moss", part, face, x // 2, (y + x % 2) // 2), 1)
        if m > 1 - self.moss * (2.4 if top or y <= 1 else 1.2):
            return shade(MOSS[int((s.n(("mc", part, face, x, y), 1) + 1) * 1.99)], side_shade(face))
        return c

    def base(self, s, part, face, x, y, fw, fh):
        c = super().base(s, part, face, x, y, fw, fh)
        if part == "head":
            return c
        key = (part, face, x, y)
        Y = T.gy(part, face, y, fh)
        top = face == "top"
        if s.kind == "cloth":
            if face not in ("top", "bottom") and s.n(("tear", part, face, x // 2, (y + 1) // 2), 1) > 1 - self.tear * 2:
                s.kind = "skin"  # torn: dark flesh shows through the hole
                return self.decay(s, key, shade(self.skin_px(s, part, face, x, y, fw, fh), 0.78))
            if s.n(("bl", part, face, x // 2, y // 2), 1) > 1 - self.blood * 2:
                c = mix(c, hexc("3e1810"), 0.55)
            return self.mossy(s, key, c, top)
        # bare flesh
        if self.ribs and part == "body" and face in ("front", "back") and Y in (2, 4, 6) and x not in (3, 4):
            c = shade(c, 0.74)
        if self.ribs and part == "body" and face == "front" and Y in (8, 9) and 2 <= x <= 5:
            c = shade(c, 0.86)  # sunken belly
        if self.body_paint and part in ("rarm", "larm") and Y in (2, 4) and face != "top":
            c = self.body_paint
        if self.body_paint and part == "body" and face == "front" and Y in (3, 5) and x in (1, 6):
            c = self.body_paint
        return self.decay(s, key, c, top)

    def head(self, s, face, x, y, fw, fh):
        c = super().head(s, face, x, y, fw, fh)
        key = ("head", face, x, y)
        if face == "front":
            if y == 4 and x in (1, 6):
                return shade(self.skin, 0.42)  # sunken socket
            if y == 4 and x in (2, 5):
                return MILKY if self.milky else SOCKET
            if y == 3 and x in (1, 2, 5, 6) and not self.flesh(c):
                return c  # brows
            if y == 5 and x in (1, 2, 5, 6) and self.flesh(c):
                return shade(c, 0.8)  # hollow cheeks, dark under the eyes
            if y == 6 and x in (2, 3, 4, 5) and (self.flesh(c) or x in (3, 4)):
                return hexc("26140e") if x in (3, 4) else shade(self.skin, 0.55)  # slack open mouth
            if y == 7 and x in (3, 4) and self.flesh(c):
                return shade(c, 0.8)
        if self.flesh(c):
            return self.decay(s, key, c, face == "top")
        return self.mossy(s, key, c, face == "top")


def zombie_looks():
    looks = {}

    def make(name, seed, woman, flesh):
        L = Corpse(random.Random(seed), woman, hexc(flesh))
        looks[name] = L
        return L

    # a hunter in a hide tunic with bear fur over the shoulders
    L = make("hide_man", "zh", False, "6b8a4e")
    L.hair, L.hair_style, L.beard = T.HAIR["brown"], "short", "short"
    L.top, L.top_kind, L.sleeves = T.HIDE, "tunic", 4
    L.fur, L.fur_kind = T.FUR_BROWN, "shoulders"
    L.legs, L.boots, L.belt = T.LEATHER, T.FUR_BROWN, T.LEATHER
    L.necklace = "teeth"

    # a woman in grey smoked hide with a wolf-fur cape and braids
    L = make("fur_woman", "zf", True, "7f8c6c")
    L.hair, L.hair_style = T.HAIR["black"], "braids"
    L.top, L.top_kind, L.sleeves = T.HIDE_GREY, "tunic", 4
    L.fur, L.fur_kind = T.FUR_GREY, "cape"
    L.legs, L.skirt_to, L.boots = None, 17, T.FUR_GREY
    L.necklace, L.trim = "shell", T.STRAW
    L.rot = 0.26

    # an old man, bald and long-bearded, fur robe and a big bone necklace
    L = make("bone_elder", "ze", False, "98a184")
    L.hair, L.hair_style, L.beard, L.wrinkles = T.HAIR["white"], "bald_top", "long", True
    L.top, L.top_kind, L.sleeves, L.skirt_to = T.FUR_BROWN, "robe", 12, 21
    L.fur, L.fur_kind = T.FUR_WHITE, "collar"
    L.legs, L.boots, L.belt = None, None, T.CORD
    L.necklace = "bone"
    L.milky = True
    L.tear = 0.08

    # a young woman with ochre war paint, one-shoulder hide wrap
    L = make("painted", "zp", True, "5f7a45")
    L.hair, L.hair_style, L.headband = T.HAIR["auburn"], "long", T.LEATHER
    L.paint = (T.OCHRE, "stripes")
    L.body_paint = T.OCHRE
    L.top, L.top_kind, L.sleeves = T.HIDE_RED, "wrap", 0
    L.legs, L.boots = T.HIDE_DARK, T.LEATHER
    L.necklace = "teeth"
    L.bracers = T.LEATHER

    # an old bog body: brown, leathery, mossy, red-stained hair, a sodden hide wrap
    L = make("bog_body", "zb", True, "6a4b2e")
    L.hair, L.hair_style, L.wrinkles = hexc("6e2e16"), "long", True
    L.top, L.top_kind, L.sleeves = hexc("4e3d2a"), "wrap", 0
    L.legs, L.legs_to, L.boots = hexc("4e3d2a"), 15, None
    L.wraps, L.belt = T.CORD, T.CORD
    L.fur, L.fur_kind = None, None
    L.moss, L.rot, L.wounds, L.tear, L.blood = 0.3, 0.14, 0.01, 0.1, 0.0
    L.milky = True

    # the Brute: a huge man, bare scarred chest under a bearskin cape
    L = make("brute", "zr", False, "4f6a3c")
    L.hair, L.hair_style, L.beard = T.HAIR["darkbrown"], "short", "short"
    L.top = None
    L.fur, L.fur_kind = hexc("4e3420"), "cape"
    L.bracers, L.legs, L.boots, L.belt = T.LEATHER, T.FUR_BROWN, T.FUR_BROWN, T.LEATHER
    L.necklace = "teeth"
    L.paint = (T.CHARCOAL, "band")
    L.wounds, L.rot = 0.05, 0.24

    # the Gaunt: starved, grey, stringy hair, ribs showing, a loincloth and rags
    L = make("gaunt", "zg", False, "8e9a80")
    L.hair, L.hair_style = T.HAIR["grey"], "long"
    L.top = None
    L.ribs = True
    L.legs, L.legs_to, L.boots = T.HIDE_DARK, 14, None
    L.wraps, L.belt = T.CORD, T.CORD
    L.rot, L.wounds = 0.4, 0.03
    L.milky = True

    # the Clubber: a young warrior, blue-clay stripes, feather, leather vest and bracers
    L = make("clubber", "zc", False, "739255")
    L.hair, L.hair_style, L.headband, L.feather = T.HAIR["brown"], "short", T.CORD, True
    L.paint = (T.BLUE_CLAY, "stripes")
    L.top, L.top_kind, L.sleeves = T.LEATHER, "vest", 0
    L.bracers, L.legs, L.boots, L.strap = T.HIDE, T.HIDE, T.FUR_BROWN, T.LEATHER
    return looks


def to_zombie_layout(img):
    """Player-format skin -> vanilla zombie layout: outer layers of body/right arm/right leg flattened onto the base,
    left limbs and the bottom half dropped (the zombie model mirrors the right limbs)."""
    out = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    out.paste(img.crop((0, 0, 64, 32)), (0, 0))
    for part in ("body", "rarm", "rleg"):
        u, v, w, h, d = T.BASE[part]
        ou, ov = T.OVER[part]
        size = (2 * (w + d), h + d)
        layer = img.crop((ou, ov, ou + size[0], ov + size[1]))
        region = out.crop((u, v, u + size[0], v + size[1]))
        region.alpha_composite(layer)
        out.paste(region, (u, v))
    return out


def build_zombies():
    made = {}
    for name, L in zombie_looks().items():
        img = to_zombie_layout(T.Skin("undead-" + name, False).build(L))
        img.save(OUT / f"zombie_{name}.png")
        made[name] = img
    return made


# ---------------------------------------------------------------- skeletons

SK = {"head": (0, 0, 8, 8, 8), "body": (16, 16, 8, 12, 4), "arm": (40, 16, 2, 12, 2), "leg": (0, 16, 2, 12, 2)}
HAT = (32, 0, 8, 8, 8)
DARK = hexc("1c1916")


class Bones:
    def __init__(self, seed, bone, stain, look):
        self.r = random.Random(seed)
        self.bone, self.stain, self.look = bone, stain, look
        self.noise = {}
        self.img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))

    def n(self, key, amt=1.0):
        if key not in self.noise:
            self.noise[key] = self.r.uniform(-1, 1)
        return self.noise[key] * amt

    def bone_px(self, key, face, f=1.0):
        part, x, y = key[0], key[2], key[3]
        c = shade(self.bone, side_shade(face) * f * (1 + self.n(key + ("b",), 0.05)))
        if self.n(("st", part, face, x // 2, y // 3), 1) > 0.55:
            c = mix(c, self.stain, 0.35)  # age stains
        return c

    def put(self, x, y, c):
        if c is not None:
            self.img.putpixel((x, y), clamp(c) + (255,))

    def paint(self):
        for part, dims in SK.items():
            for face, (x0, y0, fw, fh) in faces(*dims).items():
                for y in range(fh):
                    for x in range(fw):
                        key = (part, face, x, y)
                        c = getattr(self, part)(key, face, x, y, fw, fh)
                        if c is not None:
                            c = self.dress(key, face, x, y, fw, fh, c)
                        else:
                            c = self.dress_gap(key, face, x, y, fw, fh)
                        self.put(x0 + x, y0 + y, c)
        for face, (x0, y0, fw, fh) in faces(*HAT).items():
            for y in range(fh):
                for x in range(fw):
                    self.put(x0 + x, y0 + y, self.hat(("hat", face, x, y), face, x, y))
        return self.img

    # ---- bones ----
    def head(self, key, face, x, y, fw, fh):
        b = lambda f=1.0: self.bone_px(key, face, f)
        if face == "top":
            return b(0.84) if (x == 3 + (y % 3 == 1) and 1 <= y <= 6) else b(1.04)  # suture
        if face == "bottom":
            return DARK if 3 <= x <= 4 and 3 <= y <= 5 else b(0.62)
        if face == "back":
            return b(0.72) if y == 7 and 2 <= x <= 5 else b(0.95 if y < 6 else 0.85)
        if face in ("right", "left"):
            fx = x if face == "right" else 7 - x  # 0 back, 7 front
            if fx == 3 and y == 4:
                return DARK  # ear hole
            if y >= 6 and fx >= 4:
                return b(0.78) if y == 6 else b(0.88)  # jaw hinge
            if y == 5 and fx >= 5:
                return b(1.06)  # cheekbone
            return b(0.95 if y < 3 else 0.9)
        # front: the face
        if y in (3, 4) and x in (1, 2, 5, 6):
            inner = (y == 4) or x in (2, 5)
            return DARK if inner else shade(DARK, 1.8)
        if y == 2 and x in (1, 2, 5, 6):
            return b(1.1)  # brow ridge
        if y == 5 and x in (3, 4):
            return DARK  # nose
        if y == 5 and x in (0, 1, 6, 7):
            return b(0.9)
        if y == 6:
            return b(1.04) if x % 2 == 0 and 1 <= x <= 6 else (DARK if 1 <= x <= 6 else b(0.85))  # teeth
        if y == 7:
            return b(0.86) if 1 <= x <= 6 else b(0.75)  # jaw
        return b(1.0)

    def body(self, key, face, x, y, fw, fh):
        b = lambda f=1.0: self.bone_px(key, face, f)
        if face == "top":
            if x in (0, 7) or y in (0, 3) or (x in (3, 4) and y in (1, 2)):
                return b(1.04)
            return None
        if face == "bottom":
            return b(0.7) if x <= 2 or x >= 5 else None
        Y = y
        if face in ("front", "back"):
            spine = x in (3, 4)
            if Y == 0:
                return b(1.05)  # collarbones / shoulder line
            if face == "back" and Y <= 2 and (x <= 2 or x >= 5):
                return b(0.95 if Y < 2 else 0.85)  # shoulder blades
            if Y in (2, 4, 6):
                return b(0.94 if Y == 6 else 1.0) if not spine else b(1.08 if face == "front" else 0.9)
            if Y in (1, 3, 5):
                if spine:
                    return b(0.98) if face == "front" else b(0.8)  # sternum / vertebrae
                return b(0.78) if x in (0, 7) and Y != 5 else None
            if Y in (7, 8):
                return (b(0.92 if Y == 7 else 0.82) if face == "back" else b(0.86)) if spine else None
            if Y == 9:
                return b(0.98)  # pelvis rim
            if Y == 10:
                return b(0.88) if (x <= 2 or x >= 5 or (face == "back" and spine)) else (b(0.75) if spine else None)
            return b(0.78) if (x <= 1 or x >= 6) else None  # hip joints
        # sides: rib ends
        if Y == 0 or Y in (2, 4, 6):
            return b(0.95)
        if Y in (9, 10):
            return b(0.86)
        if Y in (1, 3, 5) and x == 3:
            return b(0.75)  # spine edge seen from the side
        return None

    def limb(self, key, face, x, y, fw, fh, leg):
        b = lambda f=1.0: self.bone_px(key, face, f)
        if face == "top":
            return b(1.06)
        if face == "bottom":
            return b(0.6) if (x + y) % 2 else DARK
        if y in (0, 1):
            return b(1.06 if y == 0 else 0.98)  # joint head
        if y in (5, 6):
            return b(1.08) if y == 5 else b(0.8)  # elbow / knee
        if y >= 10:
            if leg:
                return b(0.9) if y == 10 else b(0.8)  # foot
            return b(0.95) if (x + y) % 2 else b(0.72)  # knuckles
        return b(0.94 if x == 0 else 0.88)

    def arm(self, key, face, x, y, fw, fh):
        return self.limb(key, face, x, y, fw, fh, False)

    def leg(self, key, face, x, y, fw, fh):
        return self.limb(key, face, x, y, fw, fh, True)

    # ---- per look ----
    def mossy(self, key, face, x, y, c, amt):
        part = key[0]
        top = face == "top" or y <= 1
        m = self.n(("m", part, face, x // 2, (y + x % 2) // 2))
        if m > 1 - amt * (2.6 if top else 1.0):
            return shade(MOSS[int((self.n(("mc",) + key) + 1) * 1.99)], side_shade(face))
        if self.n(("li",) + key) > 0.93:
            return hexc("a8b06a")  # lichen
        return c

    def dress(self, key, face, x, y, fw, fh, c):
        part = key[0]
        if self.look == "mossy":
            return self.mossy(key, face, x, y, c, 0.32)
        if self.look == "bog":
            if self.n(("pt", part, face, x // 2, y // 2)) > 0.78:
                return mix(c, hexc("2a2016"), 0.55)  # peat
            if self.n(("sh",) + key) > 0.9:
                return shade(c, 1.25)  # wet shine
            return c
        if self.look == "hide":
            h = self.hide(key, face, x, y)
            return h if h is not None else c
        return c

    def dress_gap(self, key, face, x, y, fw, fh):
        """Things that cover the see-through gaps (hide strips, necklace, hanging moss)."""
        part = key[0]
        if self.look == "hide":
            return self.hide(key, face, x, y)
        if self.look == "mossy" and part == "body" and face in ("front", "back", "right", "left"):
            # moss hanging between the ribs
            col = x * 5 + (face == "back") * 3
            if self.n(("hang", face, x)) > 0.55 and y <= 2 + int((self.n(("hl", face, x)) + 1) * 3) and y not in (7, 8):
                return shade(MOSS[(col + y) % 3], side_shade(face) * (0.9 if y % 2 else 1.0))
        return None

    def hide(self, key, face, x, y):
        part = key[0]
        hide = lambda col=T.HIDE_DARK, f=1.0: shade(col, side_shade(face) * f * (1 + self.n(key + ("h",), 0.07)))
        if part == "body" and face != "top":
            if face == "bottom":
                return hide(f=0.75) if (x + y) % 3 else None
            if y >= 9:  # loincloth over the pelvis, ragged
                if y == 9:
                    return hide(T.CORD, 1.1 if x % 2 else 0.85)  # cord belt
                return hide(f=0.92 if y == 10 else 0.82) if not (y == 11 and (x + 1) % 3 == 0) else None
            if face == "front" and y == 1 and 1 <= x <= 6:
                return T.TOOTH if x % 2 else shade(T.CORD, 0.85)  # tooth necklace
            if face == "front" and y == 2 and x in (3, 4):
                return T.TOOTH
            # a hide strip over the left shoulder
            if face in ("front", "back") and y <= 8:
                d = (7 - x) if face == "front" else x
                if d in (int(y * 0.7) + 0, int(y * 0.7) + 1):
                    return hide(T.HIDE, 0.85 if (x + y) % 3 == 0 else 1.0)
            if face == "left" and y <= 8 and x in (1, 2):
                return hide(T.HIDE, 0.9)
            return None
        if part == "leg" and face not in ("top", "bottom") and y <= 2:
            if face in ("front", "back") and not (y == 2 and x == 1):
                return hide(f=0.88 if y == 2 else 0.95)  # flaps hanging from the belt
            return None
        if part == "arm" and face not in ("top", "bottom") and y == 8:
            return hide(T.CORD, 1.1 if (x + face.count("o")) % 2 else 0.82)  # cord binding
        return None

    def hat(self, key, face, x, y):
        if self.look == "mossy":
            if face == "top":
                return shade(MOSS[(x * 3 + y) % 4], 1.05) if self.n(key) > -0.55 else None
            if face in ("front", "back", "right", "left"):
                fx = x if face == "right" else 7 - x
                if y == 0 and self.n(key) > -0.3:
                    return shade(MOSS[(x + 1) % 3], side_shade(face))
                if face == "left" and fx in (1, 2) and y <= 4 - fx:
                    return shade(MOSS[1], 0.9)  # trailing strand
                if face == "back" and y <= 1 + (x % 3 == 0) * 2 and self.n(key) > 0.0:
                    return shade(MOSS[0], 0.85)
            return None
        if self.look == "bog":
            hair = hexc("6a2e18")
            if face == "top" and (y >= 3 or x <= 1) and self.n(key) > -0.2:
                return shade(hair, 0.9 + self.n(key + ("h",), 0.12))
            if face == "back" and y <= 1 + int((self.n(("hb", x)) + 1) * 1.5) and self.n(key) > -0.2:
                return shade(hair, 0.8 + self.n(key + ("h",), 0.1))
            if face in ("right", "left"):
                fx = x if face == "right" else 7 - x
                if fx <= 1 and y <= 3 + fx and self.n(key) > -0.3:
                    return shade(hair, 0.78)
            return None
        if self.look == "hide":
            if face in ("right", "left", "back", "front") and y == 2 and face != "front":
                return shade(T.CORD, 0.95 if (x + y) % 2 else 0.78)  # headband
            if face == "left":
                fx = 7 - x
                if fx == 2 and y <= 2:
                    return [hexc("f0ece2"), hexc("e0dccf"), hexc("3a3030")][y]  # white feather
                if fx == 3 and y <= 2:
                    return [hexc("b0462a"), T.OCHRE, hexc("3a3030")][y]  # ochre-dyed feather
            return None
        return None


def build_skeletons():
    made = {}
    for name, bone, stain in (("mossy", "bdb59a", "7c7a58"), ("hide", "d9d2bf", "a89a7a"), ("bog", "6e5236", "3e2e1e")):
        img = Bones("skel-" + name, hexc(bone), hexc(stain), name).paint()
        img.save(OUT / f"skeleton_{name}.png")
        made[name] = img
    return made


# ---------------------------------------------------------------- previews

def zombie_front(img, back=False):
    """Front (or back) view of a zombie skin, 16x32."""
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    name = "back" if back else "front"

    def face(dims, uv=None):
        u, v, w, h, d = dims
        if uv:
            u, v = uv
        x0, y0, fw, fh = faces(u, v, w, h, d)[name]
        return img.crop((x0, y0, x0 + fw, y0 + fh))

    out.alpha_composite(face(T.BASE["head"]), (4, 0))
    out.alpha_composite(face(T.BASE["head"], (32, 0)), (4, 0))
    out.alpha_composite(face(T.BASE["body"]), (4, 8))
    arm = face(T.BASE["rarm"])
    leg = face(T.BASE["rleg"])
    out.alpha_composite(arm, (0, 8))
    out.alpha_composite(arm.transpose(Image.FLIP_LEFT_RIGHT), (12, 8))
    out.alpha_composite(leg, (4, 20))
    out.alpha_composite(leg.transpose(Image.FLIP_LEFT_RIGHT), (8, 20))
    return out


def skeleton_front(img, back=False):
    out = Image.new("RGBA", (16, 32), (0, 0, 0, 0))
    name = "back" if back else "front"

    def face(dims):
        x0, y0, fw, fh = faces(*dims)[name]
        return img.crop((x0, y0, x0 + fw, y0 + fh))

    out.alpha_composite(face(SK["head"]), (4, 0))
    out.alpha_composite(face(HAT), (4, 0))
    out.alpha_composite(face(SK["body"]), (4, 8))
    arm, leg = face(SK["arm"]), face(SK["leg"])
    out.alpha_composite(arm, (1, 8))
    out.alpha_composite(arm.transpose(Image.FLIP_LEFT_RIGHT), (13, 8))
    out.alpha_composite(leg, (5, 20))
    out.alpha_composite(leg.transpose(Image.FLIP_LEFT_RIGHT), (9, 20))
    return out


def preview(zombies, skeletons, out_dir):
    out_dir = Path(out_dir)
    s = 8
    items = [(img, zombie_front) for img in zombies.values()] + [(img, skeleton_front) for img in skeletons.values()]
    sheet = Image.new("RGBA", (len(items) * 34 * s, 34 * s), (90, 110, 120, 255))
    for i, (img, fn) in enumerate(items):
        for k, back in enumerate((False, True)):
            v = fn(img, back).resize((16 * s, 32 * s), Image.NEAREST)
            sheet.alpha_composite(v, ((i * 34 + k * 17) * s, s))
    sheet.save(out_dir / "undead_sheet.png")
    raw = Image.new("RGBA", (len(items) * 4 * 66, 4 * 66), (60, 60, 60, 255))
    for i, (img, _) in enumerate(items):
        raw.alpha_composite(img.resize((img.width * 4, img.height * 4), Image.NEAREST), (i * 264, 0))
    raw.save(out_dir / "undead_raw.png")


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    z = build_zombies()
    sk = build_skeletons()
    print(f"wrote {len(z)} zombie and {len(sk)} skeleton skins")
    if len(sys.argv) > 1:
        preview(z, sk, sys.argv[1])
