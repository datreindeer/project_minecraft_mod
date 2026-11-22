"""Paint the jungle and mangrove fauna (scripts/gen_fauna_jungle.py, com.mechanical.wildlife.jungle): entity textures and
their skinned (butchered) versions, the equipment layers (Tiger Pelt Mantle, Crocodile Hide armour), the Tiger Rug block
and every 16x16 item.

Usage: python scripts/gen_fauna_jungle_textures.py [--java]
- Tiger: the LeopardModel UVs (gen_fauna_warm_tex_dry.LEOPARD) painted orange with black stripes (the leopard's
  materials with their colours and rosettes swapped for stripes). Python: the SnakeModel UVs (meadow snake_parts), a
  reticulated net. Monitor Lizard: the DesertLizardModel UVs (dry LIZARD), dark olive with yellow beading.
- New models, part trees mirroring the Java (client/wildlife/jungle): PrimateModel (orangutan; the proboscis monkey with
  tail and nose), CrocodileModel, DartFrogModel, MudskipperModel, FiddlerCrabModel. --java prints each model's
  createBodyLayer() parts so the Java stays in step with the paint.
Items: gen_fauna_jungle_tex_items.py (run from here). Deterministic: hashes only, no randomness.
"""
import importlib.util
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from critter_paint import cube, fur, h, lerp, pack, paint, part, save, shade, skinned, smooth  # noqa: E402

TEX = HERE.parent / "src/main/resources/assets/mechanical/textures"


def load(name, alias):
    spec = importlib.util.spec_from_file_location(alias, HERE / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


EYE = (16, 12, 10)


def f(v):
    return f"{float(v)}F"


def java_parts(parts, uv):
    """createBodyLayer() lines: every part, its cubes (texOffs from pack()) and pose, in the Java model's shape."""
    out = []
    for p in parts:
        bits = []
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            plane = ", new CubeDeformation(0.001F)" if 0 in (w, hh, d) else ""
            b = f".texOffs({u}, {v})" + (".mirror()" if c["mirror"] else "") + \
                f".addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(hh)}, {f(d)}{plane})" + (".mirror(false)" if c["mirror"] else "")
            bits.append(b)
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = (f"PartPose.offset({f(px)}, {f(py)}, {f(pz)})" if (rx, ry, rz) == (0, 0, 0)
                else f"PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})")
        parent = "root" if p["parent"] is None else p["parent"]
        out.append(f'PartDefinition {p["name"]} = {parent}.addOrReplaceChild("{p["name"]}", CubeListBuilder.create(){"".join(bits)}, {pose});')
    return out


# ======================================================================= tiger (LeopardModel UVs)

def tiger_textures():
    dry = load("gen_fauna_warm_tex_dry", "dry_tiger")
    dry.LP_BACK, dry.LP_GOLD, dry.LP_PALE = (188, 92, 30), (220, 120, 40), (238, 196, 150)
    dry.LP_WHITE, dry.LP_SPOT, dry.LP_CENTRE = (244, 240, 230), (24, 18, 16), (220, 120, 40)
    dry.LP_EYE, dry.LP_NOSE = (214, 190, 70), (198, 120, 112)

    def stripe_at(u, w, salt):
        """Narrow black stripes round the body, wavering and forking."""
        k = math.floor(u / 3.2)
        off = (h(k, 3, salt) - 0.5) * 1.6
        s = (u + off + math.sin(w * 0.55 + k) * 0.9) % 3.2
        return s < 0.85 and h(k, int(w // 3), salt + 1) < 0.9

    def rosette(u, w, base, i, j, salt, cell=4.3):
        return fur(dry.LP_SPOT, i, j, salt + 6, 0.1) if stripe_at(u, w, salt) else None

    def spot(p, cell, radius, salt, chance=0.85):
        # head, legs, belly and tail: stripes across the limb (they run with y on the legs, z on the head)
        return stripe_at(p[1] * 0.9 + p[2] * 0.6 + p[0] * 0.3, p[0] * 2 + p[2], salt)

    dry.rosette, dry.spot = rosette, spot
    img = dry.paint(dry.LEOPARD, "tiger", (128, 64))
    dry.save(img, "wildlife/tiger.png")
    dry.save(skinned(img, dry.LEOPARD, keep=("paw",)), "skinned/wildlife/tiger.png")


# ======================================================================= python (SnakeModel UVs)

PY = dict(ground=(156, 132, 84), gold=(196, 168, 102), net=(38, 30, 22), pale=(206, 192, 150), belly=(220, 210, 178),
          head=(132, 110, 68), eye=(196, 150, 60), tongue=(40, 30, 34))


def python_textures():
    meadow = load("gen_fauna_meadow_textures", "meadow_py")

    def m(kind):
        def mat(face, X, Y, Z, n, tx, ty):
            if kind == "tongue":
                fork = Z < -11.0 and abs(X) < 0.5
                return (0, 0, 0, 0) if fork and tx % 2 == 0 else PY["tongue"]
            if face == "bottom":
                return meadow.scales(PY["belly"], tx, ty, 900)
            if kind == "head":
                if face in ("left", "right"):
                    if abs(Y - 22.5) < 0.5 and abs(Z - (-12.5)) < 0.5:
                        return PY["eye"]
                    if abs(Y - 22.5) < 0.5 and Z > -12.0:
                        return PY["net"]                                     # the dark stripe back from the eye
                    return meadow.scales(PY["pale"] if Y > 23.0 else PY["head"], tx, ty, 901)
                if face == "top" and abs(X) < 0.5 and Z > -13:
                    return PY["net"]                                         # fine line down the crown
                return meadow.scales(PY["head"], tx, ty, 902)
            s = meadow.snake_size(Z)
            # the reticulated net: dark lines round irregular gold cells, pale centres low on the flanks
            u = s * 0.9
            w = (X * 1.4 if face == "top" else (2.0 + (Y - 22) * 1.2) * (1 if X > 0 else -1))
            cell_u, cell_w = u % 2.4, (w + (math.floor(u / 2.4) % 2) * 1.2) % 2.4
            if cell_u < 0.55 or cell_w < 0.5:
                return meadow.scales(PY["net"], tx, ty, 903)
            c = PY["gold"] if face == "top" else PY["ground"]
            if face in ("left", "right") and Y > 23.2 and cell_u > 1.2:
                c = PY["pale"]
            return meadow.scales(c, tx, ty, 904)
        return mat

    parts = meadow.snake_parts(m)
    uv = pack(parts, 64, 32)
    img = paint(parts, uv, (64, 32))
    save(img, "wildlife/python.png")
    save(skinned(img, {}, ()), "skinned/wildlife/python.png")


# ======================================================================= monitor lizard (DesertLizardModel UVs)

def monitor_textures():
    dry = load("gen_fauna_warm_tex_dry", "dry_monitor")
    dry.LZ_SAND, dry.LZ_OCHRE, dry.LZ_BAND = (92, 92, 70), (66, 66, 52), (34, 34, 28)
    dry.LZ_SPOT, dry.LZ_BELLY = (214, 196, 104), (168, 160, 118)
    dry.LZ_BLUE, dry.LZ_BLUE_L = (84, 80, 62), (126, 120, 92)
    img = dry.paint(dry.LIZARD, "monitor", (64, 32))
    # yellow beading: scattered pale scale rows over the dark back
    px = img.load()
    for x in range(64):
        for y in range(32):
            c = px[x, y]
            if c[3] and sum(c[:3]) < 300 and h(x, y, 950) < 0.16:
                px[x, y] = (*dry.LZ_SPOT, 255)
    dry.save(img, "wildlife/monitor_lizard.png")
    dry.save(skinned(img, dry.LIZARD, keep=("front_foot", "hind_foot")), "skinned/wildlife/monitor_lizard.png")


# ======================================================================= primates (PrimateModel)
# One part tree for both: a stooping body, a broad head with a muzzle, long arms reaching the ground, short legs; the
# proboscis monkey adds a long tail and the male's drooping nose (both hidden on the orangutan).
# Model space: y down, ground at 24, -z the front.

ORANG = dict(hair=(176, 74, 30), hair_dark=(122, 44, 18), hair_light=(210, 110, 50), skin=(78, 60, 54), skin_light=(118, 92, 80),
             eye=(30, 20, 14), nose=(78, 60, 54), tail=(176, 74, 30))
PROBOSCIS = dict(hair=(196, 120, 64), hair_dark=(150, 84, 44), hair_light=(222, 176, 120), skin=(222, 150, 120),
                 skin_light=(236, 178, 150), eye=(30, 20, 14), nose=(214, 128, 100), limb=(198, 196, 190), rump=(236, 232, 224),
                 tail=(206, 202, 194))


def primate(P, monkey):
    def hair(c, tx, ty, salt):
        return fur(c, tx, ty, salt, 0.12)

    def body(face, X, Y, Z, n, tx, ty):
        if monkey:
            if Y > 15.5 or face == "bottom":
                return hair(P["rump"], tx, ty, 901)                  # the white rump and belly band
            return hair(lerp(P["hair_dark"], P["hair"], (Y - 7) / 6), tx, ty, 902)
        c = P["hair_dark"] if face == "top" else P["hair"]
        if h(tx, ty // 2, 903) < 0.25:
            c = P["hair_light"]                                       # long shaggy strands
        return hair(c, tx, ty, 904)

    def head(face, X, Y, Z, n, tx, ty):
        if face == "front":
            if abs(abs(X) - 1.5) < 0.6 and abs(Y - 5.5) < 0.6:
                return P["eye"]
            if abs(X) < 2.6 and Y > 3.5:
                return fur(P["skin"], tx, ty, 905, 0.05)              # the bare face
            if not monkey and abs(X) > 2.6:
                return fur(P["skin_light"], tx, ty, 906, 0.05)        # cheek flanges
        if monkey and face == "top":
            return hair(P["hair_dark"], tx, ty, 907)                  # the reddish cap
        return hair(P["hair"], tx, ty, 908)

    def muzzle(face, X, Y, Z, n, tx, ty):
        if face == "front" and Y > 8.5:
            return shade(P["skin"], 0.7)                              # the mouth line
        return fur(P["skin_light"] if face in ("front", "bottom") else P["skin"], tx, ty, 909, 0.05)

    def nose(face, X, Y, Z, n, tx, ty):
        return fur(P["nose"], tx, ty, 910, 0.05)

    def arm(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["skin"]                                          # palms
        if monkey:
            return hair(P["limb"] if Y > 11 else P["hair"], tx, ty, 911)
        return hair(P["hair"] if h(tx, ty // 2, 912) > 0.2 else P["hair_light"], tx, ty, 913)

    def leg(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["skin"]
        return hair(P["limb"] if monkey else P["hair_dark"], tx, ty, 914)

    def tail(face, X, Y, Z, n, tx, ty):
        return hair(P["tail"], tx, ty, 915)

    return [
        part("body", None, (0, 16, 1), (0.6, 0, 0), [cube("pr_body", -4, -9, -3, 8, 10, 6, body)]),
        part("head", "body", (0, -9, -1), (-0.6, 0, 0), [cube("pr_head", -3.5, -6, -3.5, 7, 6, 6, head),
                                                         cube("pr_muzzle", -2, -3, -5.5, 4, 3, 2, muzzle)]),
        part("nose", "head", (0, -2.5, -5.5), (0, 0, 0), [cube("pr_nose", -1, 0, -1, 2, 3, 1, nose)]),
        part("right_arm", "body", (-5, -8, -0.5), (-0.6, 0, 0), [cube("pr_arm", -1.5, 0, -1.5, 3, 15, 3, arm)]),
        part("left_arm", "body", (5, -8, -0.5), (-0.6, 0, 0), [cube("pr_arm", -1.5, 0, -1.5, 3, 15, 3, arm, True)]),
        part("right_leg", None, (-2.5, 16, 2), (0, 0, 0), [cube("pr_leg", -1.5, 0, -1.5, 3, 8, 3, leg)]),
        part("left_leg", None, (2.5, 16, 2), (0, 0, 0), [cube("pr_leg", -1.5, 0, -1.5, 3, 8, 3, leg, True)]),
        part("tail", "body", (0, 0, 2.5), (-1.6, 0, 0), [cube("pr_tail", -0.5, -0.5, 0, 1, 1, 13, tail)]),
    ]


# ======================================================================= crocodile (CrocodileModel)
# Long and low: armoured back with rows of raised scutes (the texture), a long tapering four-part tail, a long snout and
# a lower jaw that opens, sprawled legs. Olive-grey above, dark crossbands on the tail, a pale yellowish belly.
CR = dict(back=(74, 80, 52), back_dark=(48, 54, 36), scute=(98, 104, 70), side=(110, 112, 74), belly=(206, 196, 150),
          eye=(196, 170, 60), tooth=(232, 226, 200), mouth=(176, 96, 88), claw=(52, 46, 38))


def crocodile():
    def armour(face, X, Y, Z, n, tx, ty, salt):
        if face == "bottom":
            return fur(CR["belly"], tx, ty, salt, 0.05, streak=False) if (tx + ty) % 3 else shade(CR["belly"], 0.88)
        if face == "top":
            # raised scutes: two-pixel plates in rows, ridged pairs either side of the spine
            plate = (tx // 2 + ty // 2) % 2
            c = CR["scute"] if plate else CR["back"]
            if tx % 2 == 0 or ty % 2 == 0:
                c = shade(c, 0.78)
            return c
        c = lerp(CR["back"], CR["side"], (Y - 16) / 5)
        return fur(c, tx, ty, salt, 0.08, streak=False) if (tx + ty) % 2 else shade(c, 0.86)

    def body(face, X, Y, Z, n, tx, ty):
        return armour(face, X, Y, Z, n, tx, ty, 920)

    def tail(face, X, Y, Z, n, tx, ty):
        c = armour(face, X, Y, Z, n, tx, ty, 921)
        if face != "bottom" and int(Z) % 5 == 0:
            return shade(c, 0.6)                                      # dark crossbands down the tail
        return c

    def head(face, X, Y, Z, n, tx, ty):
        if face in ("left", "right") and abs(Z + 13) < 0.6 and Y < 16.5:
            return CR["eye"]
        return armour(face, X, Y, Z, n, tx, ty, 922)

    def eyes(face, X, Y, Z, n, tx, ty):
        return CR["eye"] if face in ("front", "left", "right") and Y < 14.6 else CR["back_dark"]

    def snout(face, X, Y, Z, n, tx, ty):
        if face in ("left", "right") and Y > 17.0:
            return CR["tooth"] if tx % 2 == 0 else CR["mouth"]       # the ragged row of teeth
        if face == "bottom":
            return CR["mouth"]
        return armour(face, X, Y, Z, n, tx, ty, 923)

    def jaw(face, X, Y, Z, n, tx, ty):
        if face == "top":
            return CR["mouth"]
        if face in ("left", "right") and Y < 18.3:
            return CR["tooth"] if tx % 2 == 1 else CR["side"]
        return fur(CR["belly"], tx, ty, 924, 0.05, streak=False)

    def leg(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return CR["claw"]
        return armour("left", X, 18, Z, n, tx, ty, 925)

    return [
        part("body", None, (0, 19, 0), (0, 0, 0), [cube("cr_body", -5, -3, -9, 10, 5, 18, body)]),
        part("head", "body", (0, -1, -9), (0, 0, 0), [cube("cr_head", -3.5, -2, -6, 7, 3, 6, head),
                                                      cube("cr_snout", -2.5, -1.5, -15, 5, 2, 9, snout),
                                                      cube("cr_eye", -3, -3, -5, 2, 1, 2, eyes),
                                                      cube("cr_eye", 1, -3, -5, 2, 1, 2, eyes, True)]),
        part("jaw", "head", (0, 1, -5), (0, 0, 0), [cube("cr_jaw", -2.5, 0, -10, 5, 1, 10, jaw)]),
        part("tail0", "body", (0, -1, 9), (0, 0, 0), [cube("cr_tail0", -4, -2, 0, 8, 4, 8, tail)]),
        part("tail1", "tail0", (0, 0.5, 8), (0, 0, 0), [cube("cr_tail1", -3, -2, 0, 6, 3, 8, tail)]),
        part("tail2", "tail1", (0, 0, 8), (0, 0, 0), [cube("cr_tail2", -2, -1.5, 0, 4, 2, 8, tail)]),
        part("tail3", "tail2", (0, 0, 8), (0, 0, 0), [cube("cr_tail3", -1, -1, 0, 2, 2, 7, tail)]),
        part("right_front_leg", "body", (-4.5, 1, -6), (0, 0, 0.7), [cube("cr_leg", -1.5, 0, -1.5, 3, 5, 3, leg)]),
        part("left_front_leg", "body", (4.5, 1, -6), (0, 0, -0.7), [cube("cr_leg", -1.5, 0, -1.5, 3, 5, 3, leg, True)]),
        part("right_hind_leg", "body", (-4.5, 1, 6), (0, 0, 0.7), [cube("cr_leg", -1.5, 0, -1.5, 3, 5, 3, leg)]),
        part("left_hind_leg", "body", (4.5, 1, 6), (0, 0, -0.7), [cube("cr_leg", -1.5, 0, -1.5, 3, 5, 3, leg, True)]),
    ]


# ======================================================================= poison dart frog (DartFrogModel)

FROG_BLUE = dict(main=(40, 96, 214), light=(96, 150, 236), mark=(14, 14, 20), belly=(54, 84, 176))
FROG_YELLOW = dict(main=(236, 200, 30), light=(250, 228, 90), mark=(14, 14, 20), belly=(200, 160, 30))


def dart_frog(P):
    def skin(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return P["belly"]
        if h(int(X * 2 + 10), int(Z * 2 + 10), 930) < 0.3 or (face == "top" and abs(X) < 0.4 and int(Z) % 2 == 0):
            return P["mark"]                                          # black blotches / the dorsal line
        return P["light"] if face == "top" and h(tx, ty, 931) < 0.15 else P["main"]

    def eye(face, X, Y, Z, n, tx, ty):
        return P["mark"]

    def limb(face, X, Y, Z, n, tx, ty):
        return P["mark"] if (tx + ty) % 2 else P["main"]

    return [
        part("body", None, (0, 22.5, 0), (-0.2, 0, 0), [cube("fr_body", -1.5, -1, -1.5, 3, 2, 4, skin)]),
        part("head", "body", (0, -0.5, -1.5), (0.2, 0, 0), [cube("fr_head", -1.5, -1, -2, 3, 2, 2, skin),
                                                           cube("fr_eye", -1.5, -1.5, -1.5, 1, 1, 1, eye),
                                                           cube("fr_eye", 0.5, -1.5, -1.5, 1, 1, 1, eye, True)]),
        part("right_front_leg", None, (-1, 23, -1.5), (0, 0, 0), [cube("fr_arm", -0.5, 0, -0.5, 1, 1, 1, limb)]),
        part("left_front_leg", None, (1, 23, -1.5), (0, 0, 0), [cube("fr_arm", -0.5, 0, -0.5, 1, 1, 1, limb, True)]),
        part("right_hind_leg", None, (-1.5, 23, 1.5), (0, 0, 0), [cube("fr_leg", -1, 0, -2, 1, 1, 3, limb)]),
        part("left_hind_leg", None, (1.5, 23, 1.5), (0, 0, 0), [cube("fr_leg", 0, 0, -2, 1, 1, 3, limb, True)]),
    ]


# ======================================================================= mudskipper (MudskipperModel)

MS = dict(back=(104, 96, 76), side=(140, 128, 100), spot=(206, 214, 236), belly=(196, 186, 160), eye=(40, 34, 28),
          eye_ring=(150, 140, 110), fin=(120, 96, 70), fin_edge=(70, 150, 200))


def mudskipper():
    def body(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return MS["belly"]
        if h(tx, ty, 940) < 0.14:
            return MS["spot"]                                         # little pale-blue spangles
        return fur(MS["back"] if face == "top" else MS["side"], tx, ty, 941, 0.08, streak=False)

    def eye(face, X, Y, Z, n, tx, ty):
        return MS["eye"] if face in ("front", "left", "right") else MS["eye_ring"]

    def fin(face, X, Y, Z, n, tx, ty):
        return MS["fin_edge"] if Y < 19.6 else MS["fin"]

    def tailfin(face, X, Y, Z, n, tx, ty):
        return MS["fin"] if h(tx, ty, 942) < 0.7 else shade(MS["fin"], 0.75)

    def pectoral(face, X, Y, Z, n, tx, ty):
        return MS["fin"]

    return [
        part("body", None, (0, 22.5, 0), (0, 0, 0), [cube("ms_body", -1, -1, -2, 2, 2, 5, body),
                                                     cube("ms_dorsal", 0, -3, -1, 0, 2, 4, fin)]),
        part("head", "body", (0, 0, -2), (0, 0, 0), [cube("ms_head", -1.5, -1, -3, 3, 2, 3, body),
                                                     cube("ms_eye", -1.5, -2, -2, 1, 1, 1, eye),
                                                     cube("ms_eye", 0.5, -2, -2, 1, 1, 1, eye, True)]),
        part("tail", "body", (0, 0, 3), (0, 0, 0), [cube("ms_tail", 0, -1.5, 0, 0, 3, 3, tailfin)]),
        part("right_fin", "body", (-1, 0.5, -1.5), (0, 0, 0.5), [cube("ms_fin", -0.5, 0, -0.5, 1, 1, 1, pectoral)]),
        part("left_fin", "body", (1, 0.5, -1.5), (0, 0, -0.5), [cube("ms_fin", -0.5, 0, -0.5, 1, 1, 1, pectoral, True)]),
    ]


# ======================================================================= fiddler crab (FiddlerCrabModel)

FC = dict(shell=(70, 74, 96), shell_light=(110, 130, 170), shell_dark=(40, 42, 58), claw=(236, 128, 40), claw_light=(250, 186, 96),
          small=(200, 180, 150), leg=(150, 120, 96), eye=(20, 18, 16), stalk=(170, 150, 120))


def fiddler_crab():
    def shell(face, X, Y, Z, n, tx, ty):
        if face == "bottom":
            return FC["small"]
        if face == "top":
            return FC["shell_light"] if h(tx, ty, 960) < 0.35 else FC["shell"]
        return FC["shell_dark"] if Y > 22.4 else FC["shell"]

    def claw(face, X, Y, Z, n, tx, ty):
        return FC["claw_light"] if face == "top" or h(tx, ty, 961) < 0.2 else FC["claw"]

    def small(face, X, Y, Z, n, tx, ty):
        return FC["small"]

    def leg(face, X, Y, Z, n, tx, ty):
        return FC["leg"] if (tx + ty) % 2 else shade(FC["leg"], 0.8)

    def stalk(face, X, Y, Z, n, tx, ty):
        return FC["eye"] if Y < 19.5 else FC["stalk"]

    parts = [part("body", None, (0, 22, 0), (0, 0, 0), [cube("fc_body", -2, -1, -1.5, 4, 2, 3, shell),
                                                        cube("fc_stalk", -1.5, -3, -1.5, 1, 2, 1, stalk),
                                                        cube("fc_stalk", 0.5, -3, -1.5, 1, 2, 1, stalk, True)]),
             part("big_claw", "body", (-2.5, 0, -1.5), (0, 0.3, 0), [cube("fc_claw", -2, -1, -3, 2, 2, 3, claw)]),
             part("small_claw", "body", (2, 0, -1.5), (0, -0.3, 0), [cube("fc_small", 0, -0.5, -2, 1, 1, 2, small)])]
    for i, z in enumerate((-0.5, 0.5, 1.5)):
        parts.append(part(f"right_leg{i}", "body", (-2, 0.5, z), (0, 0, 0.6), [cube("fc_leg", -3, 0, -0.5, 3, 1, 1, leg)]))
        parts.append(part(f"left_leg{i}", "body", (2, 0.5, z), (0, 0, -0.6), [cube("fc_leg", 0, 0, -0.5, 3, 1, 1, leg, True)]))
    return parts


# ======================================================================= build

MODELS = {  # name -> (parts, texture size)
    "orangutan": (lambda: primate(ORANG, False), (64, 64)),
    "saltwater_crocodile": (crocodile, (128, 64)),
    "poison_dart_frog": (lambda: dart_frog(FROG_BLUE), (32, 32)),
    "mudskipper": (mudskipper, (32, 32)),
    "fiddler_crab": (fiddler_crab, (32, 32)),
}
VARIANTS = {  # same UVs as their base model, other paint
    "proboscis_monkey": ("orangutan", lambda: primate(PROBOSCIS, True)),
    "poison_dart_frog_yellow": ("poison_dart_frog", lambda: dart_frog(FROG_YELLOW)),
}


def entities():
    uvs = {}
    for name, (make, size) in MODELS.items():
        parts = make()
        uv = pack(parts, *size)
        uvs[name] = (uv, size)
        if "--java" in sys.argv:
            print(f"// ---- {name} {size}")
            print("\n".join(java_parts(parts, uv)))
        img = paint(parts, uv, size)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")
    for name, (base, make) in VARIANTS.items():
        uv, size = uvs[base]
        img = paint(make(), uv, size)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")


# ======================================================================= equipment layers

def recolour_layer(src, dst, dark, light, pattern=None):
    """Re-tint an existing 64x32 equipment layer by brightness into dark..light; pattern(x, y, c) may overlay marks."""
    im = Image.open(TEX / src).convert("RGBA")
    px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if not a:
                continue
            t = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            c = lerp(dark, light, smooth(t * 1.3))
            if pattern:
                c = pattern(x, y, c)
            px[x, y] = (*c, a)
    im.save(TEX / dst)
    print("wrote", dst)


def tiger_stripes(x, y, c):
    return (26, 20, 16) if (x + int(1.5 * math.sin(y * 0.8))) % 5 == 0 else c


def croc_scales(x, y, c):
    return shade(c, 0.75) if x % 2 == 0 or y % 3 == 0 else c


def equipment():
    recolour_layer("entity/equipment/humanoid/otter_fur.png", "entity/equipment/humanoid/tiger_fur.png", (150, 64, 20), (238, 150, 70),
                   tiger_stripes)
    recolour_layer("entity/cloak/otter_fur_cape.png", "entity/cloak/tiger_pelt_mantle.png", (150, 64, 20), (238, 150, 70), tiger_stripes)
    recolour_layer("entity/equipment/humanoid/lindworm_scale.png", "entity/equipment/humanoid/crocodile_hide.png", (40, 46, 30), (128, 132, 88),
                   croc_scales)
    recolour_layer("entity/equipment/humanoid_leggings/lindworm_scale.png", "entity/equipment/humanoid_leggings/crocodile_hide.png",
                   (40, 46, 30), (128, 132, 88), croc_scales)


def main():
    tiger_textures()
    python_textures()
    monitor_textures()
    entities()
    equipment()
    load("gen_fauna_jungle_tex_items", "jungle_items").main()


if __name__ == "__main__":
    main()
