"""The folklore beings' models and skins from one table each (docs/fauna/folklore.md, com.mechanical.folklore.Beings):

- src/main/java/com/mechanical/client/folklore/FolkloreGeometry.java: every part and box (generated, do not edit)
- textures/entity/folklore/<being>.png (+ frost_giant_glow.png: the giant's eyes)

Huldra: a slender woman in a moss-green dress, long fair hair with flowers, a cow's tail peeking from under the hem, and
her back a hollow of rotten wood framed by bark. Tomte: a small stout old man in grey wadmal, a tall floppy red cap that
curls back, a long white beard over his belly, a red potato nose. Näcken: pale and gaunt, long wet black hair over his
face, waterweed round his hips, a fiddle under his chin and a bow. Bog Body: a hunched, shrivelled leather-brown corpse
in a pointed sheepskin cap (the Tollund Man), red peat-dyed hair, the noose still round his neck, ribs and long thin
claws. Frost Giant: a hulking rime-thurs (drawn twice its size): bowed back, long arms with huge fists and ice bracers,
crystals growing from shoulders and spine, an icicle beard, ice horns, frosted hair, glowing pale eyes. Dune Wraith: a
hooded figure of sand whose lower half is a sand devil of three rings turning at different speeds, ragged sleeves and
tatters (see-through). Jungle Spirit Mask: a carved, painted wooden mask with a long nose, bared teeth, ear discs with
bead tassels and a fan of red, yellow, green and blue feathers. Will-o'-the-wisp: a small soft orb of pale blue light
in a halo (drawn emissive and see-through; the particles stay).

Usage: python scripts/gen_folklore_models.py. Deterministic (hashes only). Standalone texture script.
"""
import math
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from box_paint import Model, h, java_method, lerp, lit, pack, paint, shade, write_java  # noqa: E402

ROOT = HERE.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity/folklore"
JAVA = ROOT / "src/main/java/com/mechanical/client/folklore/FolkloreGeometry.java"


def strands(dark, base, light, q, salt, wet=False):
    """Hair falling in strands: per-column tone, light streaks, darker roots."""
    col = h(int(q.X * 2 + q.Z * 2), 0, salt) if q.face in ("front", "back", "left", "right") else h(q.tx, q.ty, salt)
    c = lerp(dark, base, 0.25 + 0.6 * col)
    if (q.tx * 3 + q.ty // 2) % 7 == 0:
        c = lerp(c, light, 0.35)
    if wet and h(q.tx, q.ty, salt + 5) > 0.975:
        c = lerp(c, (200, 220, 220), 0.5)  # a wet glint
    return c


def weave(c, q, salt, amt=0.06):
    n = h(q.tx, q.ty, salt) - 0.5
    return shade(c, 1 + n * amt * 2 - (0.03 if (q.tx + q.ty) % 2 else 0))


# ======================================================================================= Huldra

HU = dict(skin=(236, 206, 184), skin_sh=(200, 158, 134), hair=(228, 198, 122), hair_dk=(168, 126, 60), hair_lt=(252, 236, 176),
          dress=(84, 114, 58), dress_dk=(46, 68, 34), dress_lt=(130, 158, 84), bark=(118, 104, 88), bark_dk=(64, 54, 46),
          bark_lt=(164, 152, 132), rot=(40, 26, 16), rot_lt=(98, 66, 36), moss=(92, 128, 50), iris=(64, 116, 80),
          white=(238, 234, 222), lip=(204, 132, 124), comb=(228, 216, 182), comb_dk=(150, 128, 92), tail=(184, 140, 84),
          tail_dk=(112, 80, 44), flower=(246, 244, 232), flower2=(240, 206, 84), leaf=(96, 140, 58))


def huldra_colour(m, q):
    P = HU
    n = h(q.tx, q.ty, 31)
    f, X, Y = q.face, q.X, q.Y
    if m == "skin":
        c = lerp(P["skin"], P["skin_sh"], 0.12 + 0.2 * n)
        if Y > 23 and f == "front":
            c = lerp(c, P["skin_sh"], 0.3) if q.i % 2 else c  # toes
    elif m == "head":
        c = lerp(P["skin"], P["skin_sh"], 0.08 * n)
        if f == "front":
            ax = abs(X)
            if -6 < Y < -5:
                c = lerp(c, P["hair_dk"], 0.55) if ax > 1 else c                                 # brows
            elif -5 < Y < -4:
                c = P["iris"] if 1 < ax < 2 else lerp(P["white"], c, 0.15) if ax > 2 else lerp(c, P["white"], 0.15)
            elif -4 < Y < -3:
                c = lerp(c, P["skin_sh"], 0.25) if ax > 2 else c
            elif -3 < Y < -2:
                c = lerp(c, P["skin_sh"], 0.3) if ax < 1 else lerp(c, P["lip"], 0.12) if ax > 2 else c   # nose tip, blush
            elif -2 < Y < -1:
                c = lerp(c, P["lip"], 0.75) if ax < 1 else c
            elif Y > -1:
                c = lerp(c, P["skin_sh"], 0.3) if ax > 2 else c
        elif f in ("left", "right"):
            if q.Z > -1 or Y < -5:
                c = strands(P["hair_dk"], P["hair"], P["hair_lt"], q, 32)
            elif -4 < Y < -2 and -1.5 < q.Z < -0.5:
                c = lerp(c, P["skin_sh"], 0.4)  # the ear
        elif f == "back" or f == "top":
            c = strands(P["hair_dk"], P["hair"], P["hair_lt"], q, 32)
        else:
            c = lerp(c, P["skin_sh"], 0.4)
    elif m == "hair":
        c = strands(P["hair_dk"], P["hair"], P["hair_lt"], q, 33)
        if f == "top" and abs(X) < 0.5:
            c = lerp(c, P["hair_dk"], 0.6)  # the parting
        if f == "front" and abs(X) < 1:
            c = lerp(c, P["hair_dk"], 0.35)
        if f == "top" and h(q.tx, q.ty, 34) > 0.9:
            c = P["flower"] if h(q.tx, q.ty, 35) > 0.5 else P["leaf"]  # small flowers and leaves woven in
    elif m in ("flower", "flower2"):
        c = P[m] if f != "bottom" else P["leaf"]
    elif m in ("dress", "skirt", "sleeve"):
        c = weave(P["dress"], q, 36)
        fold = 0.5 + 0.5 * math.sin(X * 1.7 + q.Z * 1.3 + 0.6)
        if m == "skirt" and f in ("front", "back", "left", "right"):
            c = lerp(c, P["dress_dk"], 0.35 * fold)
            if Y > 21:  # the embroidered hem: little pale leaves
                c = lerp(P["dress_dk"], P["dress_lt"], 0.8) if (q.i + (1 if Y > 21.5 else 0)) % 3 == 0 else lerp(c, P["dress_dk"], 0.3)
        if m == "dress" and f == "front" and abs(X) < 0.6 and 2 < Y < 9 and int(Y * 2) % 2 == 0:
            c = P["comb_dk"]  # the bodice lacing
        if m == "sleeve" and Y > 7.5:
            c = lerp(P["skin"], P["skin_sh"], 0.15 + 0.2 * n)  # bare forearm below the sleeve
            if Y > 9.5:
                c = lerp(c, P["skin_sh"], 0.3)  # the hand
    elif m == "girdle":
        c = lerp(P["bark_lt"], P["bark"], 0.4 * n)
    elif m == "back":  # the chest/waist: dress, but the back is the hollow: rotten wood fibres, dark inside
        if f == "back" and abs(X) < 2.2:
            depth = 1 - abs(X) / 2.2
            c = lerp(P["rot_lt"], P["rot"], 0.35 + 0.5 * depth + 0.3 * h(int(X * 3), int(Y / 2), 37))
            if h(q.tx, q.ty, 38) > 0.95:
                c = P["moss"]
            if h(q.tx, q.ty, 39) > 0.97:
                c = (196, 170, 120)  # a pale shelf fungus fleck
        else:
            c = weave(P["dress"], q, 36)
            if f == "front" and abs(X) < 0.6 and int(Y * 2) % 2 == 0:
                c = P["comb_dk"]
    elif m == "bark":
        furrow = h(int(X * 2), int(Y), 40)
        c = lerp(P["bark_dk"], P["bark"], 0.3 + 0.6 * furrow)
        if (int(Y * 2) + int(X * 3)) % 5 == 0:
            c = lerp(c, P["bark_lt"], 0.5)
        if h(q.tx, q.ty, 41) > 0.9:
            c = lerp(c, P["moss"], 0.7)
    elif m == "tail":
        c = lerp(P["tail_dk"], P["tail"], 0.4 + 0.5 * n)
    elif m == "tuft":
        c = strands(P["tail_dk"], P["tail"], P["hair_lt"], q, 42)
    elif m == "comb":
        c = lerp(P["comb"], P["comb_dk"], 0.3 * n)
    elif m == "teeth":
        return (*P["comb"], 255) if q.i % 2 == 0 else None
    else:
        c = (255, 0, 255)
    return lit(c, q, top=-8)


def huldra():
    M = Model("huldra", (128, 64), 1, huldra_colour)
    for side, name in ((-1, "right_leg"), (1, "left_leg")):
        M.part(name, None, (side * 1.5, 12, 0))
        M.limb(name, side, (-1.5, 0, -1.5, 3, 11, 3), (-1.5, 11, -2.5, 3, 1, 4), mat="skin")
    M.part("body", None, (0, 12, 0))
    M.box("body", "skirt", -3.5, -1, -2.5, 7, 5, 5)
    M.box("body", "skirt", -4, 4, -3, 8, 4, 6)
    M.box("body", "skirt", -4.5, 8, -3.5, 9, 2, 7)
    M.box("body", "girdle", -3, -2, -2, 6, 1, 4)
    M.box("body", "back", -2.5, -5, -1.5, 5, 4, 3)              # waist (back at z 1.5: the hollow)
    M.box("body", "back", -3, -9, -2, 6, 4, 3)                  # chest (back at z 1)
    M.box("body", "dress", -2.5, -8.5, -3, 5, 2, 1)             # bust
    M.box("body", "dress", -3.5, -11, -1.5, 7, 2, 3)            # shoulders
    M.box("body", "bark", -3.5, -10, 1, 1, 9, 2)               # the bark rim round the hollow
    M.box("body", "bark", 2.5, -10, 1, 1, 9, 2)
    M.box("body", "bark", -2.5, -10, 1, 5, 1, 2)
    M.box("body", "bark", -2.5, -2, 1, 5, 1, 2)
    M.part("tail", "body", (0, 6, 2.5), (0.35, 0, 0))
    M.box("tail", "tail", -0.5, 0, -0.5, 1, 4, 1)
    M.part("tail_end", "tail", (0, 4, 0), (0.35, 0, 0))
    M.box("tail_end", "tail", -0.5, 0, -0.5, 1, 3, 1)
    M.box("tail_end", "tuft", -1, 2, -1, 2, 3, 2)
    M.part("head", "body", (0, -11, 0))
    M.box("head", "skin", -1, -1, -1, 2, 1, 2)
    M.box("head", "head", -3, -8, -3, 6, 7, 6)
    M.box("head", "hair", -3.5, -9, -3.5, 7, 2, 7)             # crown and fringe
    M.box("head", "hair", -3.5, -7, 2.5, 7, 7, 1)               # down the back to the shoulders: the hollow shows below
    M.box("head", "hair", -2.5, 0, 2.6, 5, 2, 1)
    M.box("head", "hair", -3.6, -7, -1, 1, 9, 4)                # over the ears
    M.box("head", "hair", 2.6, -7, -1, 1, 9, 4)
    M.box("head", "hair", -3.5, -2, -2.75, 1, 9, 2)             # two tresses in front of the shoulders
    M.box("head", "hair", 2.5, -2, -2.75, 1, 8, 2)
    M.box("head", "flower", -4, -9.5, -2.5, 1, 1, 1)
    M.box("head", "flower2", -4, -9, -1, 1, 1, 1)
    M.box("head", "flower", 3, -9.5, -0.5, 1, 1, 1)
    for side, name in ((-1, "right_arm"), (1, "left_arm")):
        M.part(name, "body", (side * 4.5, -10, 0))
        M.limb(name, side, (-1, -1, -1, 2, 11, 2), mat="sleeve")
    M.box("right_arm", "comb", -0.5, 8.5, -3.5, 1, 1, 3)
    M.box("right_arm", "teeth", 0, 9.5, -3.5, 0, 1, 3)
    return M


# ======================================================================================= Tomte

TO = dict(wool=(132, 128, 118), wool_dk=(80, 78, 72), wool_lt=(172, 168, 156), red=(176, 32, 28), red_dk=(108, 16, 16),
          red_lt=(220, 64, 48), beard=(238, 236, 228), beard_dk=(178, 176, 168), skin=(216, 160, 128), nose=(204, 100, 86),
          boot=(72, 48, 30), boot_dk=(40, 26, 16), belt=(98, 64, 36), brass=(196, 156, 72), eye=(24, 20, 18))


def tomte_colour(m, q):
    P = TO
    n = h(q.tx, q.ty, 51)
    f, X, Y = q.face, q.X, q.Y
    if m == "wool":
        c = weave(P["wool"], q, 52, 0.09)
        if (q.tx + 2 * q.ty) % 4 == 0:
            c = lerp(c, P["wool_dk"], 0.3)  # twill
        if f in ("front", "back", "left", "right") and q.j == q.fh - 1 and Y > 20:
            c = lerp(c, P["wool_dk"], 0.4)  # the hem
    elif m == "mitten":
        c = lerp(P["red_dk"], P["red"], 0.5 + 0.3 * n) if Y > 19 else weave(P["wool"], q, 52, 0.09)
    elif m in ("cap", "brim"):
        c = lerp(P["red_dk"], P["red"], 0.55 + 0.35 * n)
        if (q.tx + q.ty) % 3 == 0:
            c = lerp(c, P["red_lt"], 0.25)  # knitted
        if m == "brim" and f != "top" and f != "bottom":
            c = lerp(P["red_dk"], P["red_lt"], 0.15 + 0.5 * (q.i % 2))  # ribbing
    elif m == "head":
        c = lerp(P["skin"], P["nose"], 0.15 * n)
        if f == "front":
            ax = abs(X)
            if 12 < Y < 13:
                c = P["eye"] if 0.5 < ax < 1.5 else lerp(P["beard"], c, 0.3) if ax > 1.5 else c
            elif Y > 13:
                c = lerp(c, P["nose"], 0.35) if ax > 1 else c  # ruddy cheeks
        elif f in ("left", "right", "back"):
            c = strands(P["beard_dk"], P["beard"], (255, 255, 255), q, 53)
    elif m == "nose":
        c = lerp(P["nose"], P["skin"], 0.2 * n)
        if f == "top":
            c = lerp(c, (240, 170, 150), 0.4)
    elif m == "beard":
        c = strands(P["beard_dk"], P["beard"], (255, 255, 255), q, 54)
        if f == "front" and 14 < Y < 15:
            c = lerp(c, P["beard_dk"], 0.3) if abs(X) < 0.5 else lerp(c, (255, 255, 255), 0.4)  # the moustache over the beard
    elif m == "belt":
        c = lerp(P["belt"], P["boot_dk"], 0.3 * n)
        if f == "front" and abs(X) < 1:
            c = P["brass"] if abs(X) > 0.5 or q.j == 0 else lerp(P["brass"], P["boot_dk"], 0.5)
    elif m == "boot":
        c = lerp(P["boot"], P["boot_dk"], 0.3 + 0.3 * n)
        if Y > 23.5 or f == "bottom":
            c = P["boot_dk"]
    elif m == "leg":
        c = lerp(P["wool_dk"], P["wool"], 0.4 + 0.3 * n)
    else:
        c = (255, 0, 255)
    return lit(c, q, top=6)


def tomte():
    M = Model("tomte", (64, 64), 1, tomte_colour)
    for side, name in ((-1, "right_leg"), (1, "left_leg")):
        M.part(name, None, (side * 1.4, 20, 0))
        M.limb(name, side, (-1, 0, -1, 2, 3, 2), (-1.5, 2, -2.5, 3, 2, 4), mat=("leg", "boot"))
    M.part("body", None, (0, 20, 0))
    M.box("body", "wool", -3, -5, -2, 6, 5, 4)
    M.box("body", "wool", -2.5, -4, -3, 5, 3, 1)                # the belly
    M.box("body", "wool", -3.5, -1, -2.5, 7, 2, 5)              # the tunic's skirt
    M.box("body", "belt", -3.5, -2.5, -3.5, 7, 1, 7)
    M.part("head", "body", (0, -5, 0))
    M.box("head", "head", -2.5, -4, -2.5, 5, 4, 5)
    M.box("head", "brim", -3, -5, -3, 6, 2, 6)                   # the knitted band
    M.box("head", "nose", -1, -2, -3.5, 2, 1, 1)
    M.box("head", "beard", -2.5, -1, -3.25, 5, 4, 1)
    M.box("head", "beard", -1.5, 3, -3.25, 3, 2, 1)
    M.box("head", "beard", -0.5, 5, -3.2, 1, 1, 1)
    M.box("head", "beard", -3, -2.5, -2.5, 1, 4, 4)             # side whiskers
    M.box("head", "beard", 2, -2.5, -2.5, 1, 4, 4)
    M.part("cap", "head", (0, -5, 0.25))
    M.box("cap", "cap", -2, -2, -2, 4, 2, 4)
    prev, sizes, rots = "cap", (3, 3, 2, 1), (-0.35, -0.45, -0.55, -0.6)
    for i, (s, r) in enumerate(zip(sizes, rots)):
        name = f"cap{i + 1}"
        M.part(name, prev, (0, -2 if i == 0 else -1.8, 0.3), (r, 0, 0))
        M.box(name, "cap", -s / 2, -2, -s / 2, s, 2, s)
        prev = name
    for side, name in ((-1, "right_arm"), (1, "left_arm")):
        M.part(name, "body", (side * 4, -4.5, 0))
        M.limb(name, side, (-1, -0.5, -1, 2, 5, 2), mat="mitten")
    return M


# ======================================================================================= Näcken

NA = dict(skin=(184, 206, 190), skin_sh=(104, 136, 124), hair=(30, 44, 38), hair_dk=(12, 20, 18), hair_lt=(70, 98, 84),
          weed=(58, 88, 46), weed_dk=(30, 50, 30), weed_lt=(100, 130, 64), eye=(214, 240, 228), socket=(70, 92, 86),
          lip=(120, 140, 150), wood=(150, 82, 38), wood_dk=(88, 42, 18), wood_lt=(196, 124, 62), string=(230, 232, 220),
          ebony=(30, 22, 18))


def nacken_colour(m, q):
    P = NA
    n = h(q.tx, q.ty, 61)
    f, X, Y = q.face, q.X, q.Y
    if m == "skin":
        c = lerp(P["skin"], P["skin_sh"], 0.1 + 0.25 * n)
        if h(q.tx, q.ty, 62) > 0.95:
            c = lerp(c, (236, 248, 246), 0.6)  # water drops
    elif m == "torso":
        c = lerp(P["skin"], P["skin_sh"], 0.1 + 0.2 * n)
        if f == "front" and 2.5 < Y < 6 and abs(X) > 0.6 and int(Y * 1) % 2 == 0:
            c = lerp(c, P["skin_sh"], 0.45)  # the ribs
        if f == "front" and abs(X) < 0.5:
            c = lerp(c, P["skin_sh"], 0.3)
        if f == "back" and abs(X) < 0.5:
            c = lerp(c, P["skin_sh"], 0.4)  # the spine
        if h(q.tx, q.ty, 62) > 0.95:
            c = lerp(c, (236, 248, 246), 0.6)
    elif m == "head":
        c = lerp(P["skin"], P["skin_sh"], 0.1 * n)
        if f == "front":
            ax = abs(X)
            if -6 < Y < -5:
                c = lerp(c, P["socket"], 0.55) if 1 < ax < 2 else lerp(c, P["socket"], 0.2)
            elif -5 < Y < -4:
                c = P["eye"] if 1 < ax < 2 else lerp(c, P["skin_sh"], 0.2)
            elif -4 < Y < -3:
                c = lerp(c, P["socket"], 0.45) if 1 < ax < 2 else c                          # dark under the eyes
            elif -3 < Y < -2:
                c = lerp(c, P["skin_sh"], 0.5) if ax < 1 else lerp(c, P["socket"], 0.3)       # nose, hollow cheeks
            elif -2 < Y < -1:
                c = P["lip"] if ax < 1 else c
        elif f != "bottom":
            c = strands(P["hair_dk"], P["hair"], P["hair_lt"], q, 63, wet=True)
    elif m == "hair":
        c = strands(P["hair_dk"], P["hair"], P["hair_lt"], q, 64, wet=True)
        if h(q.tx, q.ty, 65) > 0.985:
            c = P["weed"]  # a strand of weed caught in it
        if f in ("front", "back", "left", "right") and q.j == q.fh - 1 and h(q.tx, 0, 67) > 0.5:
            return None  # ragged, dripping ends
    elif m == "weed":
        if f in ("front", "back", "left", "right") and Y > 13 and h(int(q.X * 3 + q.Z * 3), 0, 66) < 0.35 and q.j > 1:
            return None  # ragged strands
        c = lerp(P["weed_dk"], P["weed"], 0.3 + 0.6 * h(int(q.X * 3 + q.Z * 3), 0, 66))
        if (q.tx + q.ty) % 5 == 0:
            c = lerp(c, P["weed_lt"], 0.5)
    elif m in ("fiddle", "bout"):
        c = lerp(P["wood_dk"], P["wood"], 0.5 + 0.4 * n)
        if f == "top":
            c = lerp(P["wood"], P["wood_lt"], 0.3 + 0.3 * math.sin(q.z * 2.5) ** 2)  # the flamed spruce top, varnished
            if m == "bout" and abs(q.x) < 0.5:
                c = P["string"] if q.i % 2 == 0 else c
        if f in ("left", "right", "front", "back"):
            c = lerp(c, P["wood_dk"], 0.4)
    elif m == "neck":
        c = P["ebony"] if f == "top" else lerp(P["wood"], P["wood_dk"], 0.4)
    elif m == "bow":
        c = lerp(P["wood_dk"], P["wood"], 0.4 * n)
        if f == "bottom":
            c = P["string"]
    else:
        c = (255, 0, 255)
    return lit(c, q, top=-8)


def nacken():
    M = Model("nacken", (128, 64), 1, nacken_colour)
    for side, name in ((-1, "right_leg"), (1, "left_leg")):
        M.part(name, None, (side * 1.6, 12, 0))
        M.limb(name, side, (-1.5, 0, -1.5, 3, 11, 3), (-1.5, 11, -2.5, 3, 1, 4), mat="skin")
    M.part("body", None, (0, 12, 0))
    M.box("body", "weed", -3.5, -2, -2, 7, 3, 4)
    M.box("body", "weed", -3, 1, -2.5, 6, 5, 1)
    M.box("body", "weed", -3, 1, 1.5, 6, 4, 1)
    M.box("body", "torso", -3, -6, -1.5, 6, 4, 3)
    M.box("body", "torso", -3.5, -10, -2, 7, 4, 4)
    M.box("body", "torso", -4, -11, -1.5, 8, 1, 3)
    M.part("head", "body", (0, -11, 0))
    M.box("head", "skin", -1, -1, -1, 2, 1, 2)
    M.box("head", "head", -3, -8, -3, 6, 7, 6)
    M.box("head", "hair", -3.5, -9, -3.5, 7, 2, 7)
    M.box("head", "hair", -3.6, -7, -2.5, 1, 10, 6)
    M.box("head", "hair", 2.6, -7, -2.5, 1, 9, 6)
    M.box("head", "hair", -3.5, -7, 2.6, 7, 9, 1)
    M.box("body", "hair", -3, -9.5, 2.4, 6, 7, 1)               # the rest of the mane lies on his back
    M.box("head", "hair", -3, -7, -3.4, 1, 8, 1)                # wet strands over the face
    M.box("head", "hair", 2, -7, -3.4, 1, 9, 1)
    M.box("head", "hair", -0.5, -7, -3.3, 1, 3, 1)
    for side, name in ((-1, "right_arm"), (1, "left_arm")):
        M.part(name, "body", (side * 5, -10, 0))
        M.limb(name, side, (-1, -1, -1, 2, 11, 2), mat="skin")
    M.part("bow", "right_arm", (0, 9, -0.5), (0, -1.57, 0))
    M.box("bow", "bow", -0.5, -0.5, -11, 1, 1, 12)
    M.part("fiddle", "body", (2, -11.5, -2), (0.15, -0.55, 0.3))
    M.box("fiddle", "bout", -1.5, -1, -2, 3, 1, 3)
    M.box("fiddle", "fiddle", -1, -1, -3, 2, 1, 1)
    M.box("fiddle", "bout", -1.5, -1, -5, 3, 1, 2)
    M.box("fiddle", "neck", -0.5, -1.5, -8, 1, 1, 3)
    M.box("fiddle", "fiddle", -0.5, -2, -9, 1, 2, 1)
    return M


# ======================================================================================= Bog Body

BO = dict(skin=(100, 66, 36), skin_dk=(48, 30, 16), skin_lt=(156, 110, 62), hair=(156, 62, 24), hair_dk=(96, 34, 12),
          cap=(96, 72, 46), cap_dk=(54, 38, 22), cap_lt=(140, 112, 76), rope=(180, 150, 92), rope_dk=(112, 88, 50),
          belt=(54, 36, 18), mouth=(16, 8, 4), tooth=(170, 150, 100), peat=(40, 30, 20))


def bog_colour(m, q):
    P = BO
    n = h(q.tx, q.ty, 71)
    f, X, Y = q.face, q.X, q.Y
    if m in ("skin", "limb"):
        c = lerp(P["skin_dk"], P["skin"], 0.45 + 0.4 * n)
        if h(q.tx, q.ty // 2, 72) > 0.85:
            c = lerp(c, P["skin_lt"], 0.5)  # taut, shiny leather
        if h(q.tx // 2, q.ty, 73) > 0.9:
            c = lerp(c, P["skin_dk"], 0.6)  # cracks and creases
        if m == "limb" and (q.tx + q.ty * 3) % 6 == 0:
            c = lerp(c, P["skin_dk"], 0.35)  # wrinkled
    elif m == "ribs":
        c = lerp(P["skin_dk"], P["skin"], 0.45 + 0.4 * n)
        if f in ("front", "left", "right") and int(Y) % 2 == 0:
            c = lerp(c, P["skin_lt"], 0.45)  # ribs pressing through the skin
        elif f in ("front", "left", "right"):
            c = lerp(c, P["skin_dk"], 0.4)
        if f == "back" and abs(X) < 0.6:
            c = lerp(c, P["skin_lt"], 0.5) if int(Y * 2) % 2 == 0 else c  # the spine's knobs
    elif m == "disc":
        r = math.hypot(q.x, q.z) / max(q.box[3], q.box[5]) * 2  # 0 at the middle, 1 at the rim
        ang = math.atan2(q.z, q.x)
        if r > 1 or r < 0.45:
            return None
        band = 0.5 + 0.5 * math.sin(ang * 3 + r * 9)  # spiral arms of dust
        c = lerp(P["sand_dk"], P["sand_lt"], band)
        a = int(170 * band * (1 - abs(r - 0.72) / 0.3)) if abs(r - 0.72) < 0.3 else 0
        if a < 20:
            return None
    elif m == "hand":
        c = lerp(P["skin_dk"], P["skin"], 0.4 + 0.4 * n)
        if f in ("front", "back", "left", "right") and q.j >= q.fh // 2 and q.i % 2 == 1:
            c = P["mouth"]  # long thin fingers, dark gaps
    elif m == "head":
        c = lerp(P["skin_dk"], P["skin"], 0.5 + 0.35 * n)
        if f == "front":
            ax = abs(X)
            row = int(math.floor(-q.y - 2))  # 0 = the bottom row (the head box spans local y -8..-2)
            if row == 4:
                c = lerp(c, P["skin_dk"], 0.3) if ax > 1.5 else c
            elif row == 3:
                c = lerp(P["mouth"], P["skin_dk"], 0.35) if 0.5 < ax < 1.5 else lerp(c, P["skin_dk"], 0.3)  # sunken eye sockets
            elif row == 2:
                c = lerp(c, P["skin_dk"], 0.55) if ax < 0.6 else lerp(c, P["skin_lt"], 0.25) if ax > 1.5 else c  # flattened nose
            elif row == 1:
                c = P["mouth"] if ax < 1.5 else c
                if 0.5 < ax < 1.5:
                    c = P["tooth"]
            elif row == 0:
                c = lerp(c, P["skin_dk"], 0.3)
        elif f in ("left", "right"):
            if q.Z > 0.5:
                c = strands(P["hair_dk"], P["hair"], (196, 96, 44), q, 74)
            if abs(q.Z + 1.5) < 0.5:
                c = P["cap_dk"]  # the chin strap
        elif f == "back":
            c = strands(P["hair_dk"], P["hair"], (196, 96, 44), q, 74)
    elif m == "hair":
        c = strands(P["hair_dk"], P["hair"], (196, 96, 44), q, 75)
        if f in ("front", "back", "left", "right") and q.j == q.fh - 1 and h(q.tx, 0, 76) > 0.5:
            return None
    elif m == "cap":
        c = lerp(P["cap_dk"], P["cap"], 0.4 + 0.5 * n)
        if (q.tx * 2 + q.ty) % 5 == 0:
            c = lerp(c, P["cap_lt"], 0.4)  # sheepskin, fur turned in, stitched seams
        if f == "front" and abs(X) < 0.5:
            c = P["cap_dk"]
    elif m == "rope":
        c = P["rope"] if (q.tx + q.ty) % 2 == 0 else P["rope_dk"]
    elif m == "belt":
        c = lerp(P["belt"], P["skin_dk"], 0.3 * n)
        if f in ("front", "back", "left", "right") and q.j == q.fh - 1 and h(q.tx, 1, 77) > 0.4:
            return None
    else:
        c = (255, 0, 255)
    if h(q.tx, q.ty, 78) > 0.97 and m not in ("rope",):
        c = lerp(c, P["peat"], 0.7)  # clinging peat
    return lit(c, q, top=-6)


def bog_body():
    M = Model("bog_body", (64, 64), 1, bog_colour)
    for side, name in ((-1, "right_leg"), (1, "left_leg")):
        M.part(name, None, (side * 1.5, 13, 0))
        M.limb(name, side, (-1, 0, -1, 2, 10, 2), (-1, 10, -2.5, 2, 1, 4), mat="limb")
    M.part("body", None, (0, 13, 0))
    M.box("body", "skin", -2.5, -2, -1.5, 5, 3, 3)
    M.box("body", "belt", -3, -2, -2, 6, 2, 4)
    M.box("body", "skin", -2, -6, -1, 4, 4, 2)                  # the sunken belly
    M.box("body", "ribs", -3, -10, -1.5, 6, 4, 3)
    M.box("body", "skin", -3.5, -11, -1.5, 7, 1, 3)
    M.part("head", "body", (0, -11, -0.5))
    M.box("head", "limb", -1, -2, -1, 2, 2, 2)
    M.box("head", "rope", -1.5, -1.5, -1.5, 3, 1, 3)            # the noose
    M.box("head", "rope", 0.5, -1, -2, 1, 5, 1)
    M.box("head", "head", -2.5, -8, -3, 5, 6, 5)
    M.box("head", "cap", -3, -9, -3.5, 6, 3, 6)
    M.box("head", "cap", -2, -10, -2.5, 4, 1, 4)
    M.box("head", "cap", -1, -11, -1.5, 2, 1, 2)
    M.box("head", "hair", -2.5, -6, 2, 5, 4, 1)
    M.part("jaw", "head", (0, -2.5, -0.5), (0.25, 0, 0))
    M.box("jaw", "skin", -2, -0.5, -2.5, 4, 1, 3)
    for side, name in ((-1, "right_arm"), (1, "left_arm")):
        M.part(name, "body", (side * 4.5, -10, 0))
        M.limb(name, side, (-1, -1, -1, 2, 7, 2), mat="limb")
        fa = name.replace("arm", "forearm")
        M.part(fa, name, (0, 6, 0), (-0.25, 0, 0))
        M.limb(fa, side, (-0.5, 0, -0.5, 1, 6, 1), (-1, 6, -1, 2, 3, 2), mat=("limb", "hand"))
    return M


# ======================================================================================= Frost Giant

FG = dict(skin=(104, 134, 166), skin_dk=(50, 70, 102), skin_lt=(162, 192, 220), ice=(176, 228, 252), ice_dk=(90, 156, 214),
          ice_lt=(240, 252, 255), fur=(226, 232, 236), fur_dk=(156, 168, 180), fur_tip=(252, 254, 255), hair=(226, 238, 248),
          hair_dk=(150, 172, 196), leather=(92, 76, 64), leather_dk=(52, 42, 36), socket=(24, 36, 60), nail=(60, 80, 110),
          eye=(220, 248, 255), glow=(150, 220, 255))


def ice(P, q, salt):
    """Clear blue ice: brighter edges, inner cracks and a few pale facets."""
    e = min(q.i, q.fw - 1 - q.i, q.j, q.fh - 1 - q.j)
    c = lerp(P["ice_dk"], P["ice"], 0.45 + 0.4 * h(q.tx // 2, q.ty // 3, salt))
    if e == 0:
        c = lerp(c, P["ice_lt"], 0.55)
    if (q.tx - q.ty) % 9 == 0 and h(q.tx, q.ty, salt + 1) > 0.4:
        c = lerp(c, P["ice_lt"], 0.7)  # a crack catching the light
    return c


def frost_colour(m, q):
    P = FG
    n = h(q.tx, q.ty, 81)
    f, X, Y = q.face, q.X, q.Y
    if m in ("skin", "arm"):
        c = lerp(P["skin_dk"], P["skin"], 0.6 + 0.25 * n)
        if (q.tx + 3 * (q.ty // 3)) % 11 == 0 and h(q.tx // 3, q.ty // 3, 82) > 0.5:
            c = lerp(c, P["skin_dk"], 0.55)  # deep cold cracks
        if h(q.tx // 2, q.ty // 2, 83) > 0.93:
            c = lerp(c, P["skin_lt"], 0.6)  # patches of hoarfrost
        if m == "arm" and f in ("front", "back", "left", "right") and (q.ty // 3) % 4 == 0 and h(q.tx, q.ty // 3, 84) > 0.5:
            c = lerp(c, P["skin_dk"], 0.3)  # sinew bands
    elif m == "chest":
        c = lerp(P["skin_dk"], P["skin"], 0.6 + 0.25 * n)
        if f == "front":
            lx = abs(q.x)
            if q.y > -9 and abs(q.y + 9) < 0.6 and lx > 0.6:
                c = lerp(c, P["skin_dk"], 0.5)  # under the pectorals
            if lx < 0.6:
                c = lerp(c, P["skin_dk"], 0.35)
            if q.y > -8 and abs(lx - 2.5) < 2.5 and int(q.y) % 2 == 0:
                c = lerp(c, P["skin_dk"], 0.2)  # the belly's ridges
        if h(q.tx // 2, q.ty // 2, 83) > 0.93:
            c = lerp(c, P["skin_lt"], 0.6)
    elif m in ("fur", "loin"):
        col = h(q.tx // 2, 0, 85)
        c = lerp(P["fur_dk"], P["fur"], 0.4 + 0.5 * col)
        if f != "top" and (q.ty + int(col * 5)) % 5 == 0:
            c = lerp(c, P["fur_tip"], 0.5)
        if f in ("front", "back", "left", "right") and q.j >= q.fh - 2 and h(q.tx // 2, 3, 86) < 0.45:
            return None if m == "loin" and q.j == q.fh - 1 else lerp(c, P["fur_dk"], 0.6)  # ragged edge
    elif m == "leather":
        c = lerp(P["leather_dk"], P["leather"], 0.4 + 0.4 * n)
        if q.j in (0, q.fh - 1) and f not in ("top", "bottom"):
            c = P["leather_dk"]
    elif m in ("ice", "crystal", "icicle", "bracer", "horn"):
        c = ice(P, q, 87)
        if m == "icicle" and f not in ("top", "bottom"):
            t = q.j / max(1, q.fh - 1)
            c = lerp(c, P["ice_lt"], 0.5 * t)  # clearer to the tip
        if m == "bracer" and f in ("front", "back", "left", "right") and q.j in (q.fh // 2,):
            c = lerp(c, P["ice_dk"], 0.6)
    elif m == "head":
        c = lerp(P["skin_dk"], P["skin"], 0.5 + 0.35 * n)
        if f == "front":
            lx, ly = abs(q.x), q.y
            if -5 < ly < -3.5 and 0.8 < lx < 3.2:
                c = P["socket"]  # deep sockets under the brow, the eyes glow in them
            elif -3.5 < ly < -1.5 and lx < 1:
                c = lerp(c, P["skin_dk"], 0.3)
            elif ly > -1.5:
                c = lerp(P["hair_dk"], P["hair"], 0.5 * n)  # moustache over the beard
        elif f in ("left", "right", "back", "top"):
            if f == "top" or q.z > -1 or q.y < -6:
                c = strands(P["hair_dk"], P["hair"], P["ice_lt"], q, 88)
    elif m == "brow":
        c = lerp(P["skin_dk"], P["skin"], 0.45 + 0.3 * n)
        if f == "front" and q.j == q.fh - 1:
            c = lerp(c, P["socket"], 0.5)
        if f == "top" or (f == "front" and q.j == 0):
            c = lerp(c, P["ice_lt"], 0.5)  # rime on the brow
    elif m == "nose":
        c = lerp(P["skin_dk"], P["skin"], 0.5 + 0.3 * n)
        if f == "bottom" or (f == "front" and q.j >= q.fh - 2):
            c = lerp(c, P["socket"], 0.4)
    elif m in ("hair", "beard"):
        c = strands(P["hair_dk"], P["hair"], P["ice_lt"], q, 89)
        if m == "beard" and h(q.tx, q.ty, 90) > 0.8:
            c = lerp(c, P["ice"], 0.6)
    elif m == "fist":
        c = lerp(P["skin_dk"], P["skin"], 0.5 + 0.35 * n)
        if f in ("front", "bottom") and q.i % (2 * q.k) == 0:
            c = lerp(c, P["skin_dk"], 0.5)  # knuckles and fingers
        if f == "front" and q.j < q.k:
            c = lerp(c, P["skin_lt"], 0.35)
        if f == "front" and q.j >= q.fh - q.k:
            c = P["nail"]
    elif m == "foot":
        c = lerp(P["skin_dk"], P["skin"], 0.45 + 0.3 * n)
        if f == "front" and q.i % (2 * q.k) == 0:
            c = lerp(c, P["skin_dk"], 0.5)  # toes
        if f == "front" and q.j < q.k:
            c = P["nail"]
    else:
        c = (255, 0, 255)
    if m in ("skin", "arm", "chest", "fist", "foot", "head") and f in ("front", "back", "left", "right"):
        edge = min(q.i, q.fw - 1 - q.i) / q.k
        c = lerp(c, P["skin_dk"], 0.35 * max(0.0, 1 - edge / 1.5))  # rounded: darker towards the box's sides
    return lit(c, q, top=-16, amt=0.16)


def frost_glow(m, q):
    if m == "head" and q.face == "front" and -4.6 < q.y < -3.6 and 1.1 < abs(q.x) < 2.9:
        return FG["eye"] if 1.6 < abs(q.x) < 2.4 else FG["glow"]
    return None


def frost_giant():
    M = Model("frost_giant", (256, 256), 2, frost_colour)
    for side, name in ((-1, "right_leg"), (1, "left_leg")):
        M.part(name, None, (side * 4.5, 8, 0))
        M.limb(name, side, (-3.5, 0, -3.5, 7, 8, 7), (-4, 4, -4, 8, 3, 8), mat=("fur", "leather"))
        sh = name.replace("leg", "shin")
        M.part(sh, name, (0, 8, 0))
        M.limb(sh, side, (-3, 0, -3, 6, 7, 6), (-3.5, -0.5, -3.75, 7, 2.5, 3), (-3.5, 7, -5, 7, 1, 8), mat=("skin", "ice", "foot"))
        M.limb(sh, side, (-2.5, 2, -4, 1, 3, 1), (0.5, 2, -4, 1, 4, 1), mat="icicle")
    M.part("body", None, (0, 8, 0))
    M.box("body", "chest", -6, -7, -4, 12, 7, 8)                # belly
    M.box("body", "leather", -6.5, -2, -4.5, 13, 2, 9)          # belt
    M.box("body", "loin", -6, 0, -4.75, 12, 5, 9.5)             # fur loincloth
    M.box("body", "loin", -3.5, 4, -5, 7, 4, 1)                 # its front flap
    M.box("body", "chest", -7.5, -16, -5, 15, 9, 10)            # chest
    M.box("body", "chest", -6.5, -15, -5.5, 13, 4.5, 1)         # pectorals
    M.box("body", "fur", -8, -18, -3, 16, 3, 8)                 # a fur mantle on the shoulders and back
    M.box("body", "fur", -7, -16, 4, 14, 7, 1.5)
    for i, (x, y, z, s, rx, rz) in enumerate(((-6, -17.5, 1, 4, 0.25, -0.5), (-3, -18, 3, 3, 0.55, -0.25), (-7.5, -16, -1, 2, -0.2, -0.9),
                                              (6, -17.5, 1.5, 4, 0.3, 0.55), (3, -17.5, 3.5, 3, 0.5, 0.25), (7.5, -16, -1, 2, -0.2, 0.9),
                                              (0, -15, 5, 3, 0.9, 0.05), (0.5, -10, 5, 2, 1.1, -0.1), (-1, -6, 4.5, 2, 1.2, 0.3))):
        name = f"crystal{i}"
        M.part(name, "body", (x, y, z), (rx, 0.4 * i, rz))
        M.box(name, "crystal", -s / 2, -s * 2, -s / 2, s, s * 2, s)
        M.box(name, "crystal", -s / 4, -s * 3, -s / 4, s / 2, s, s / 2)
    M.part("head", "body", (0, -16, -3.5))
    M.box("head", "head", -4.5, -9, -4.5, 9, 9, 8)
    M.box("head", "brow", -5, -6.5, -5.5, 10, 2, 1.5)
    M.box("head", "nose", -1, -5, -6, 2, 3.5, 1.5)
    M.box("head", "hair", -5, -9.75, -4.75, 10, 3, 9)
    M.box("head", "hair", -5.25, -7, -1, 1, 8, 5)
    M.box("head", "hair", 4.25, -7, -1, 1, 8, 5)
    M.box("head", "hair", -5, -7, 3.5, 10, 13, 1.5)
    for side in (-1, 1):
        name = "horn_" + ("r" if side < 0 else "l")
        M.part(name, "head", (side * 4, -9, -2), (-0.35, 0, side * 0.6))
        M.box(name, "horn", -1.5, -4, -1.5, 3, 4, 3)
        M.box(name, "horn", -1, -7, -1, 2, 3, 2)
        M.box(name, "horn", -0.5, -9.5, -0.5, 1, 2.5, 1)
    M.part("beard", "head", (0, -1.5, -4.5))
    M.box("beard", "beard", -4.5, -0.5, -1.5, 9, 5, 3)
    M.box("beard", "beard", -3.5, 4.5, -1.25, 7, 3, 2.5)
    for i, (x, ln) in enumerate(((-4, 5), (-3, 9), (-2, 7), (-1, 12), (0, 8), (1, 11), (2, 6), (3, 9), (4, 5))):
        M.box("beard", "icicle", x - 0.5, 4.5 if abs(x) > 3 else 7.5, -1 + (i % 2) * 0.75, 1, ln, 1)
    for side, name in ((-1, "right_arm"), (1, "left_arm")):
        M.part(name, "body", (side * 10, -14, 0))
        M.limb(name, side, (-3, -2, -3, 6, 10, 6), (-4, -3.5, -4, 8, 4, 8), mat=("arm", "ice"))
        fa = name.replace("arm", "forearm")
        M.part(fa, name, (0, 8, 0), (-0.2, 0, 0))
        M.limb(fa, side, (-3.75, 0, -3.75, 7.5, 9, 7.5), (-4.25, 2, -4.25, 8.5, 3.5, 8.5), (-1.5, 5, 3.5, 1, 4, 1), (1, 6, 3.5, 1, 3, 1),
               (-4, 9, -4, 8, 6, 8), mat=("arm", "bracer", "icicle", "icicle", "fist"))
    return M


# ======================================================================================= Dune Wraith

DW = dict(sand=(244, 210, 136), sand_dk=(196, 154, 88), sand_lt=(255, 240, 190), void=(18, 12, 6), eye=(250, 232, 180),
          rag=(214, 176, 112), rag_dk=(150, 116, 68))


def wraith_colour(m, q):
    P = DW
    n = h(q.tx, q.ty, 91)
    f, X, Y = q.face, q.X, q.Y
    streak = h(int(Y * 2), int((X + q.Z) * 0.6), 92)  # wind streaks running round the figure
    c = lerp(P["sand_dk"], P["sand_lt"], 0.2 + 0.7 * streak)
    a = 200
    if m == "ring":
        # a sand devil: streaks running round it, thinner and fainter at the bottom
        c = lerp(P["sand_dk"], P["sand_lt"], 0.2 + 0.7 * h(q.ty, (q.tx + q.ty * 2) // 4, 93))
        a = int(120 + 90 * max(0.0, min(1.0, (24 - Y) / 11))) if h(q.tx, q.ty, 94) > 0.2 else 0
    elif m in ("rag", "tatter", "sleeve"):
        c = lerp(P["rag_dk"], P["rag"], 0.4 + 0.5 * n)
        if f in ("front", "back", "left", "right") and q.j >= q.fh - 3 and h(q.tx, 0, 95) * 3 < (q.j - (q.fh - 4)):
            return None  # ragged, torn hem
        if h(q.tx, q.ty, 96) > 0.92:
            return None  # holes
        a = 205
    elif m == "face":
        if f == "front":
            c, a = P["void"], 245
            if -4.6 < q.y < -3.8 and 0.8 < abs(q.x) < 1.6:
                c = P["eye"]  # two faint pale points in the dark of the hood
    elif m == "hood":
        if f == "front" and abs(q.x) < 2.5 and q.y > -6:
            return None
        if f == "bottom":
            c = P["void"]
    elif m == "disc":
        r = math.hypot(q.x, q.z) / max(q.box[3], q.box[5]) * 2  # 0 at the middle, 1 at the rim
        ang = math.atan2(q.z, q.x)
        if r > 1 or r < 0.45:
            return None
        band = 0.5 + 0.5 * math.sin(ang * 3 + r * 9)  # spiral arms of dust
        c = lerp(P["sand_dk"], P["sand_lt"], band)
        a = int(170 * band * (1 - abs(r - 0.72) / 0.3)) if abs(r - 0.72) < 0.3 else 0
        if a < 20:
            return None
    elif m == "hand":
        c = lerp(P["sand_dk"], P["sand"], 0.4 + 0.4 * n)
        if f != "top" and q.i % 2 == 1 and q.j > 0:
            return None  # thin fingers of sand
    return (*lit(c, q, top=-7), a)


def dune_wraith():
    M = Model("dune_wraith", (128, 64), 1, wraith_colour)
    M.part("ring0", None, (0, 24, 0))
    M.box("ring0", "ring", -1.5, -3, -1.5, 3, 3, 3)
    M.part("ring1", "ring0", (0, -3, 0))
    M.box("ring1", "ring", -2.5, -4, -2.5, 5, 4, 5)
    M.part("ring2", "ring1", (0, -4, 0))
    M.box("ring2", "ring", -3.5, -4, -3.5, 7, 4, 7)
    M.part("swirl1", "ring1", (0, -2, 0), (0.18, 0, 0.1))
    M.box("swirl1", "disc", -6, 0, -6, 12, 0, 12)
    M.part("swirl2", "ring2", (0, -3, 0), (-0.12, 0, 0.15))
    M.box("swirl2", "disc", -8, 0, -8, 16, 0, 16)
    M.part("body", None, (0, 13, 0))
    M.box("body", "rag", -4, -6, -2.5, 8, 6, 5)
    M.box("body", "sand", -3.5, -11, -2, 7, 5, 4)
    M.box("body", "rag", -4.5, -12, -2.5, 9, 2, 5)
    for i, (x, z, w, d, rx, rz) in enumerate(((-4, -2.75, 8, 0, 0.2, 0), (-4, 2.75, 8, 0, -0.2, 0), (-4.25, -2.5, 0, 5, 0, -0.2),
                                              (4.25, -2.5, 0, 5, 0, 0.2))):
        name = f"tatter{i}"
        M.part(name, "body", (0, 0, 0), (rx, 0, rz))
        M.box(name, "tatter", x, 0, z, w, 6, d)
    M.part("head", "body", (0, -12, 0))
    M.box("head", "face", -2.5, -6, -1.5, 5, 6, 4)
    M.box("head", "hood", -3.5, -8, -3.5, 7, 8, 7)
    M.box("head", "hood", -1.5, -9, -1, 3, 1, 4)
    M.box("head", "rag", -3.5, -8, -4.5, 7, 1, 1)               # the hood's brim, shading the dark inside
    for side, name in ((-1, "right_arm"), (1, "left_arm")):
        M.part(name, "body", (side * 5.5, -11, 0))
        M.limb(name, side, (-1.5, -1, -1.5, 3, 9, 3), (-1, 8, -1, 2, 3, 2), mat=("sleeve", "hand"))
    return M


# ======================================================================================= Jungle Spirit Mask

JM = dict(wood=(128, 76, 38), wood_dk=(78, 42, 18), wood_lt=(170, 110, 58), clay=(232, 226, 206), ochre=(178, 44, 30),
          yellow=(228, 182, 52), hole=(14, 8, 4), tooth=(236, 230, 210), red=(196, 40, 34), green=(48, 150, 70),
          blue=(40, 104, 186), gold=(240, 196, 60), bead=(40, 160, 150), cord=(90, 60, 30))


def mask_colour(m, q):
    P = JM
    n = h(q.tx, q.ty, 101)
    f, X, Y = q.face, q.X, q.Y
    grain = h(int(q.X * 2), q.ty // 2, 102)
    c = lerp(P["wood_dk"], P["wood"], 0.45 + 0.45 * grain)
    if m == "plate" and f == "front":
        ax = abs(X)
        if 14 < Y < 16.5 and 1 < ax < 3.2:
            return P["hole"]                                            # the eye holes
        if 13 < Y < 14 and 0.8 < ax < 3.4:
            c = P["clay"]                                               # white above the eyes
        elif 16.5 < Y < 18 and 1.5 < ax < 3.5:
            c = P["ochre"] if (q.i % 2 == 0) else P["yellow"]           # painted cheeks
        elif Y < 12.5:
            c = P["yellow"] if abs(X) < 1 else P["ochre"] if int(Y) % 2 == 0 else c   # forehead bands
        elif Y > 21:
            c = P["clay"] if q.i % 2 == 0 else c
        if f == "front" and (abs(ax - 3.6) < 0.3) and Y > 12:
            c = lerp(c, P["wood_dk"], 0.6)  # carved grooves at the edge
    elif m == "brow":
        c = P["ochre"] if f == "front" else c
        if f == "front" and q.i % 3 == 1:
            c = P["clay"]
    elif m == "nose":
        if f == "front" and q.j == q.fh - 1:
            c = P["hole"] if q.i in (0, q.fw - 1) else c   # nostrils
        if f in ("left", "right"):
            c = P["yellow"] if q.j % 2 == 0 else c
    elif m == "lips":
        if f == "front":
            c = P["tooth"] if q.j == 1 and q.i % 2 == 0 else P["hole"] if q.j == 1 else P["red"]
    elif m == "disc":
        c = P["yellow"] if f in ("left", "right") and (q.i + q.j) % 2 == 0 else P["ochre"]
    elif m == "band":
        c = P["red"] if q.i % 4 < 2 else P["gold"]
    elif m == "bead":
        c = P["bead"] if q.j % 2 == 0 else P["gold"] if q.j % 4 == 1 else P["cord"]
    elif m.startswith("feather"):
        col = P[m.split("_")[1]]
        w = q.fw
        mid = (w - 1) / 2
        t = q.j / max(1, q.fh - 1)  # 0 at the tip
        half = (0.5 + 0.5 * math.sin(min(1.0, t * 1.2) * math.pi * 0.5)) * (w / 2)
        if abs(q.i - mid) > half:
            return None
        if abs(q.i - mid) < 0.6:
            return (*P["clay"], 255)  # the shaft
        if t < 0.18:
            return (*lerp(col, (20, 16, 12), 0.6), 255)  # dark tip
        return (*lerp(col, (255, 255, 255), 0.2 * h(q.tx, q.ty, 103)), 255)
    return lit(c, q, top=8, bottom=24)


def jungle_spirit_mask():
    M = Model("jungle_spirit_mask", (64, 64), 1, mask_colour)
    M.part("mask", None, (0, 17, 0))
    M.box("mask", "plate", -4, -6, -1, 8, 11, 2)
    M.box("mask", "brow", -4.5, -4, -2, 9, 1, 1)
    M.box("mask", "nose", -1, -2, -3, 2, 4, 2)
    M.box("mask", "lips", -2.5, 2, -2, 5, 2, 1)
    M.box("mask", "plate", -2.5, 5, -1.5, 5, 1, 2)
    M.box("mask", "band", -4.5, -7, -1.5, 9, 1, 3)
    M.box("mask", "disc", -5, -2, -1, 1, 3, 2)
    M.box("mask", "disc", 4, -2, -1, 1, 3, 2)
    for side in (-1, 1):
        name = "tassel_" + ("r" if side < 0 else "l")
        M.part(name, "mask", (side * 4.5, 1, 0))
        M.box(name, "bead", -0.5, 0, -0.5, 1, 5, 1)
    colours = ("red", "yellow", "green", "blue", "green", "yellow", "red")
    for i, col in enumerate(colours):
        a = (i - 3) * 0.32
        name = f"feather{i}"
        M.part(name, "mask", (math.sin(a) * 3.5, -6, 0.25), (0, 0, a))
        M.box(name, f"feather_{col}", -1.5, -10 + abs(i - 3), 0, 3, 10 - abs(i - 3), 0)
    return M


# ======================================================================================= Will-o'-the-wisp

def wisp_colour(m, q):
    # three crossed discs each fading out from its middle, so the light looks round from every side
    d = math.sqrt(q.x * q.x + q.y * q.y + q.z * q.z) / (max(q.box[3], q.box[4], q.box[5]) / 2)
    if m == "core":
        return (240, 254, 255, 255)
    if d >= 1:
        return None
    if m == "glow":
        return (190, 240, 255, int(235 * (1 - d) ** 0.8))
    return (110, 190, 255, int(120 * (1 - d) ** 1.6))


def will_o_wisp():
    M = Model("will_o_wisp", (64, 64), 1, wisp_colour)
    M.part("orb", None, (0, 20, 0))
    M.box("orb", "core", -0.5, -0.5, -0.5, 1, 1, 1)
    for w, mat, parent in ((5, "glow", "orb"), (11, "halo", "halo")):
        if parent == "halo":
            M.part("halo", "orb", (0, 0, 0))
        M.box(parent, mat, -w / 2, -w / 2, 0, w, w, 0)
        M.box(parent, mat, 0, -w / 2, -w / 2, 0, w, w)
        M.box(parent, mat, -w / 2, 0, -w / 2, w, 0, w)
    return M


MODELS = (huldra, tomte, nacken, bog_body, frost_giant, dune_wraith, jungle_spirit_mask, will_o_wisp)


def camel(s):
    a, *b = s.split("_")
    return a + "".join(x.title() for x in b)


def main():
    TEX.mkdir(parents=True, exist_ok=True)
    methods = []
    for make in MODELS:
        M = make()
        uv = pack(M.parts, *M.size)
        img = paint(M.parts, uv, M.size, M.k)
        img.save(TEX / f"{M.name}.png")
        if M.name == "frost_giant":  # the glow layer: the same boxes, only the eyes painted
            glow = [dict(p, cubes=[dict(c, mat=lambda q, mn=c["mname"]: frost_glow(mn, q)) for c in p["cubes"]]) for p in M.parts]
            paint(glow, uv, M.size, M.k).save(TEX / "frost_giant_glow.png")
        methods.append(java_method(camel(M.name), M.parts, uv, M.size))
        print(f"{M.name}: {len(M.parts)} parts, {sum(len(p['cubes']) for p in M.parts)} boxes")
    write_java(JAVA, "com.mechanical.client.folklore", "FolkloreGeometry", "scripts/gen_folklore_models.py", methods)


if __name__ == "__main__":
    main()
