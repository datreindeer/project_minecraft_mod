"""Paint the desert and badlands fauna (scripts/gen_fauna_desert.py, com.mechanical.wildlife.desert): entity textures and
their skinned (butchered) versions, then the 16x16 items and the Puma Rug block (gen_fauna_desert_tex_items.py).

Usage: python scripts/gen_fauna_desert_textures.py [--java]
Deterministic (hashes only). Own models: the part trees below mirror ScorpionModel, FennecModel and CamelModel
(client/wildlife/desert): pivots, rotations, boxes and the UVs from pack(); --java prints their createBodyLayer() parts.
Shared models, painted with the other packages' painters and this package's colours:
- puma: LeopardModel (the leopard's LEOPARD boxes, gen_fauna_warm_tex_dry.py) without rosettes or spots: plain tawny,
  pale belly and muzzle, dark muzzle sides, ear backs and tail tip;
- sand viper, rattlesnake: SnakeModel (gen_fauna_meadow_textures.py snake_parts): the viper pale sand with faint dark
  saddles and a dark streak behind the eye, the rattlesnake grey-brown with dark pale-edged diamonds and a black-and-white
  banded tail ending in a buff rattle;
- condor: the vulture layer of ForestBirdModel (gen_fauna_forestbirds_textures.py geometry): black, a white ruff, white
  wing panels, a bare red-grey head;
- bighorn ram / ewe / lamb: BighornModel = IbexModel's cubes (gen_fauna_mountain_textures.py IBEX table): brown, a white
  rump, belly and muzzle, pale buff horns.
Looks of the own models: scorpion straw-yellow with darker banded back plates, a darker sting; fennec cream-buff with a
white face and belly, pink inner ears, a black tail tip; camel sandy-tan, the hump top darker, grey-brown pads.
"""
import importlib
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from critter_paint import cube, fur, h, lerp, pack, paint, part, save, shade, skinned, smooth  # noqa: E402
import wildlife_paint  # noqa: E402

EYE, GLINT = (14, 12, 10), (196, 200, 208)


# ======================================================================= scorpion (64x32)
SC = dict(plate=(214, 178, 96), plate_dark=(170, 132, 62), seam=(120, 90, 44), belly=(232, 206, 140), leg=(222, 190, 112),
          claw=(196, 150, 70), claw_tip=(88, 58, 30), sting=(92, 52, 30), eye=(20, 16, 12))


def sc_mat(kind):
    def mat(face, X, Y, Z, n, tx, ty):
        c = SC
        if kind == "sting":
            return shade(c["sting"], 1.0 + 0.2 * (h(tx, ty, 700) - 0.5)) if face != "top" else shade(c["sting"], 1.25)
        if kind == "claw":
            tip = Z < -9.5
            return shade(c["claw_tip"] if tip else c["claw"], 0.9 + 0.2 * h(tx, ty, 701))
        if kind == "leg":
            return shade(c["leg"], 0.85 + 0.25 * h(tx, ty, 702)) if (tx + ty) % 3 else shade(c["leg"], 0.75)
        if face == "bottom":
            return shade(c["belly"], 0.95 + 0.1 * h(tx, ty, 703))
        if kind == "head":
            if face == "top" and abs(Z + 5.5) < 0.6 and abs(abs(X) - 0.5) < 0.6:
                return c["eye"]                                            # the median eyes
            if face == "front" and abs(abs(X) - 1.5) < 0.6 and Y < 20.6:
                return c["eye"]                                            # lateral eyes
            return shade(c["plate"], 0.95 + 0.12 * h(tx, ty, 704))
        # body and tail: plates banded across with dark seams
        seam = abs(((Z + 0.5) % 1.7) - 0.85) > 0.6 if kind == "body" else ty % 3 == 0
        col = c["seam"] if seam and face in ("top", "left", "right", "back") else c["plate"]
        if face == "top" and abs(X) < 0.6:
            col = lerp(col, c["plate_dark"], 0.6)                          # the dark keel down the back
        return shade(col, 0.92 + 0.14 * h(tx, ty, 705))
    return mat


def scorpion():
    parts = [part("body", None, (0, 21, 0), (0, 0, 0), [cube("sc_body", -2.5, -1, -3.5, 5, 2, 7, sc_mat("body"))]),
             part("head", "body", (0, 0, -3.5), (0, 0, 0), [cube("sc_head", -2, -1, -2.5, 4, 2, 3, sc_mat("head"))])]
    for side in (1, -1):
        s = "left_" if side > 0 else "right_"
        parts.append(part(s + "arm", "head", (1.5 * side, 0.3, -2), (0, 0.6 * side, 0),
                          [cube("sc_arm", -0.5, -0.5, -3, 1, 1, 3, sc_mat("claw"), side > 0)]))
        parts.append(part(s + "claw", s + "arm", (0, 0, -3), (0, -0.9 * side, 0),
                          [cube("sc_claw", -1, -0.5, -3, 2, 1, 3, sc_mat("claw"), side > 0)]))
        for k, z in enumerate((-2.5, -1.0, 0.5, 2.0)):
            box = (0, -0.5, -0.5, 4, 1, 1) if side > 0 else (-4, -0.5, -0.5, 4, 1, 1)
            parts.append(part(f"{s}leg{k}", "body", (2.5 * side, 0.4, z), (0, (0.5 - k * 0.33) * side, 0.7 * side),
                              [cube("sc_leg", *box, sc_mat("leg"), side > 0)]))
    prev = "body"
    for k, (w, rot) in enumerate(((2, 0.75), (2, 0.75), (2, 0.75), (2, 0.7))):
        name = f"tail{k + 1}"
        parts.append(part(name, prev, (0, -0.3, 3.5 if k == 0 else 2.5), (rot, 0, 0), [cube(f"sc_tail{k + 1}", -w / 2, -1, 0, w, 2, 3, sc_mat("tail"))]))
        prev = name
    parts.append(part("sting", prev, (0, 0, 2.5), (0.9, 0, 0), [cube("sc_sting", -0.5, -0.5, 0, 1, 1, 2, sc_mat("sting"))]))
    return parts


# ======================================================================= fennec fox (64x32)
FX = dict(coat=(222, 190, 140), back=(204, 166, 112), belly=(246, 238, 220), face=(244, 234, 214), ear_in=(232, 176, 168),
          ear_back=(212, 176, 126), nose=(36, 28, 26), tip=(46, 34, 28), eye=(22, 16, 12), stripe=(178, 128, 82))


def fx_mat(kind):
    def mat(face, X, Y, Z, n, tx, ty):
        c = FX
        if kind == "body":
            if face == "bottom" or Y > 18.4:
                return fur(c["belly"], tx, ty, 710, 0.04)
            col = lerp(c["back"], c["coat"], smooth((Y - 15.0) / 3.0))
            return fur(col, tx, ty, 711, 0.07)
        if kind == "skull":
            if face in ("left", "right") or face == "front":
                if face == "front" and abs(Y - 14.0) < 0.6 and abs(abs(X) - 1.0) < 0.6:
                    return c["eye"]
                if face == "front" and abs(Y - 14.5) < 0.6 and abs(abs(X) - 1.0) < 0.6:
                    return fur(c["stripe"], tx, ty, 712, 0.04)               # the dark tear streak
                return fur(c["face"] if Y > 14.2 else c["coat"], tx, ty, 713, 0.05)
            if face == "bottom":
                return fur(c["belly"], tx, ty, 714, 0.03)
            return fur(c["coat"], tx, ty, 715, 0.06)
        if kind == "muzzle":
            if face == "front" and Y < 15.0 and abs(X) < 0.6:
                return c["nose"]
            return fur(c["face"], tx, ty, 716, 0.04)
        if kind == "ear":
            if face == "front":
                return fur(c["ear_in"] if abs(X) < 0.6 or face == "front" else c["ear_back"], tx, ty, 717, 0.05)
            return fur(c["ear_back"], tx, ty, 718, 0.05)
        if kind == "tail":
            d = math.hypot(Y - 16.0, Z - 4.5)
            return fur(c["tip"] if d > 7.0 else c["coat"], tx, ty, 719, 0.09)
        # legs
        if face == "bottom":
            return shade(c["coat"], 0.7)
        return fur(lerp(c["coat"], c["belly"], 0.3) if Y > 22.5 else c["coat"], tx, ty, 720, 0.05)
    return mat


def fennec():
    return [
        part("body", None, (0, 17, 0), (0, 0, 0), [cube("fx_body", -2, -2, -4.5, 4, 4, 9, fx_mat("body"))]),
        part("head", "body", (0, -1, -4.5), (0, 0, 0), [cube("fx_skull", -2, -2, -3, 4, 3, 3, fx_mat("skull")),
                                                        cube("fx_muzzle", -1, -0.5, -5, 2, 2, 2, fx_mat("muzzle"))]),
        part("right_ear", "head", (-1.2, -2, -1.5), (0, 0, -0.35), [cube("fx_ear", -1, -5, -0.5, 2, 5, 1, fx_mat("ear"))]),
        part("left_ear", "head", (1.2, -2, -1.5), (0, 0, 0.35), [cube("fx_ear", -1, -5, -0.5, 2, 5, 1, fx_mat("ear"), True)]),
        part("tail", "body", (0, -1, 4.5), (-0.5, 0, 0), [cube("fx_tail1", -1.5, -1.5, 0, 3, 3, 4, fx_mat("tail"))]),
        part("tail_tip", "tail", (0, 0, 4), (0.2, 0, 0), [cube("fx_tail2", -1.5, -1.5, 0, 3, 3, 4, fx_mat("tail"))]),
        part("right_front_leg", None, (-1.0, 19, -3.5), (0, 0, 0), [cube("fx_fleg", -0.5, 0, -0.5, 1, 5, 1, fx_mat("leg"))]),
        part("left_front_leg", None, (1.0, 19, -3.5), (0, 0, 0), [cube("fx_fleg", -0.5, 0, -0.5, 1, 5, 1, fx_mat("leg"), True)]),
        part("right_hind_leg", None, (-1.0, 19, 3.5), (0, 0, 0), [cube("fx_hleg", -0.5, 0, -1, 1, 5, 2, fx_mat("leg"))]),
        part("left_hind_leg", None, (1.0, 19, 3.5), (0, 0, 0), [cube("fx_hleg", -0.5, 0, -1, 1, 5, 2, fx_mat("leg"), True)]),
    ]


# ======================================================================= camel (128x64)
CM = dict(coat=(196, 156, 104), dark=(160, 120, 76), light=(214, 180, 132), belly=(220, 192, 150), hump=(150, 110, 70),
          muzzle=(206, 176, 136), nose=(70, 54, 44), pad=(98, 84, 72), knee=(132, 100, 66), eye=(18, 14, 12))
BARK, BARK_DARK, BARK_LIGHT = (122, 84, 52), (78, 52, 32), (156, 114, 74)
HIDE, HIDE_DARK = (150, 108, 70), (104, 72, 46)


def cm_mat(kind):
    def mat(face, X, Y, Z, n, tx, ty):
        c = CM
        if kind == "basket":
            if face == "top":
                return fur(BARK_LIGHT, tx, ty, 730, 0.08)
            row, col = int(Y) % 2, int((Z if face in ("left", "right") else X) + 10) % 2
            return fur(BARK_LIGHT if row == col else BARK, tx, ty, 731, 0.06)
        if kind == "strap":
            return fur(HIDE_DARK if face in ("front", "back") else HIDE, tx, ty, 732, 0.05)
        if kind == "body":
            if face == "bottom":
                return fur(c["belly"], tx, ty, 733, 0.05)
            t = smooth((Y - 0.0) / 8.0)
            return fur(lerp(c["dark"], lerp(c["coat"], c["belly"], 0.4), t), tx, ty, 734, 0.07)
        if kind == "hump":
            t = smooth((Y + 5.0) / 5.0)
            return fur(lerp(c["hump"], c["coat"], t), tx, ty, 735, 0.09)
        if kind == "neck":
            if face == "front" or face == "bottom":
                return fur(c["light"], tx, ty, 736, 0.06)
            return fur(c["coat"], tx, ty, 737, 0.07)
        if kind == "skull":
            if face in ("left", "right") and abs(Y - (-9.0)) < 0.6:
                if abs(Z - (-12.6)) < 0.6:
                    return c["eye"]
            return fur(lerp(c["coat"], c["light"], 0.3), tx, ty, 738, 0.05)
        if kind == "muzzle":
            if face == "front":
                if abs(abs(X) - 1.0) < 0.6 and Y < -6.4:
                    return c["nose"]                                       # the slit nostrils
                return fur(c["muzzle"] if Y < -5.2 else shade(c["muzzle"], 0.85), tx, ty, 739, 0.04)
            return fur(c["muzzle"], tx, ty, 740, 0.05)
        if kind == "ear":
            return fur(c["dark"], tx, ty, 741, 0.05)
        if kind == "tail":
            return fur(c["dark"], tx, ty, 742, 0.06)
        if kind == "tuft":
            return fur(shade(c["hump"], 0.8), tx, ty, 743, 0.12)
        if kind == "upper":
            return fur(c["coat"] if face != "back" else c["light"], tx, ty, 744, 0.06)
        # lower legs: the knee callus at the top, pale shins, broad dark pads
        if face == "bottom" or Y > 22.8:
            return shade(c["pad"], 0.9 + 0.2 * h(tx, ty, 745))
        if Y < 17.5 and face == "front":
            return fur(c["knee"], tx, ty, 746, 0.08)
        return fur(c["light"], tx, ty, 747, 0.05)
    return mat


NECK_X, NECK2_X, CAMEL_HEAD_X = 1.9, -1.75, 0.15


def camel():
    parts = [
        part("body", None, (0, 4, 0), (0, 0, 0), [cube("cm_body", -4, -4, -9, 8, 8, 18, cm_mat("body")),
                                                 cube("cm_hump", -3, -9, -5, 6, 5, 8, cm_mat("hump"))]),
        part("tail", "body", (0, -3, 9), (0.3, 0, 0), [cube("cm_tail", -0.5, 0, -0.5, 1, 8, 1, cm_mat("tail")),
                                                      cube("cm_tuft", -1, 7, -1, 2, 2, 2, cm_mat("tuft"))]),
        part("packs", "body", (0, 0, 0), (0, 0, 0), [cube("cm_strap", -3.5, -9.5, -2, 7, 1, 3, cm_mat("strap"))]),
        part("left_basket", "packs", (4, -3, -0.5), (0, 0, -0.08), [cube("cm_basket", 0, 0, -3.5, 3, 6, 7, cm_mat("basket"), True)]),
        part("right_basket", "packs", (-4, -3, -0.5), (0, 0, 0.08), [cube("cm_basket", -3, 0, -3.5, 3, 6, 7, cm_mat("basket"))]),
        part("neck", "body", (0, -1, -8), (NECK_X, 0, 0), [cube("cm_neck", -2, -9, -2, 4, 9, 4, cm_mat("neck"))]),
        part("neck2", "neck", (0, -8.5, 0), (NECK2_X, 0, 0), [cube("cm_neck2", -1.5, -8, -1.5, 3, 8, 3, cm_mat("neck"))]),
        part("head", "neck2", (0, -7.5, 0), (CAMEL_HEAD_X, 0, 0), [cube("cm_skull", -2, -2, -4, 4, 4, 5, cm_mat("skull")),
                                                                    cube("cm_muzzle", -1.5, -1, -9, 3, 3, 5, cm_mat("muzzle"))]),
        part("left_ear", "head", (1.5, -2, -0.5), (0, 0, 0.4), [cube("cm_ear", -0.5, -2, -0.5, 1, 2, 1, cm_mat("ear"), True)]),
        part("right_ear", "head", (-1.5, -2, -0.5), (0, 0, -0.4), [cube("cm_ear", -0.5, -2, -0.5, 1, 2, 1, cm_mat("ear"))]),
    ]
    for x, z, hind in ((2.5, -6.5, False), (-2.5, -6.5, False), (2.5, 6.5, True), (-2.5, 6.5, True)):
        name = ("left_" if x > 0 else "right_") + ("hind_leg" if hind else "front_leg")
        box = (-1.5, -1, -2, 3, 10, 4) if hind else (-1.5, -1, -1.5, 3, 10, 3)
        parts.append(part(name, None, (x, 7, z), (0, 0, 0), [cube("cm_hind" if hind else "cm_front", *box, cm_mat("upper"), x > 0)]))
        parts.append(part(name + "_lower", name, (0, 9, 0.5 if hind else 0), (0, 0, 0), [cube("cm_lower", -1, 0, -1, 2, 8, 2, cm_mat("lower"), x > 0)]))
    return parts


OWN = {"desert_scorpion": (scorpion, (64, 32)), "fennec_fox": (fennec, (64, 32)), "camel": (camel, (128, 64))}


def java_parts(parts, uv):
    def f(v):
        return f"{float(v)}F"
    out = []
    for p in parts:
        bits = []
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            b = f".texOffs({u}, {v})" + (".mirror()" if c["mirror"] else "") + \
                f".addBox({f(x0)}, {f(y0)}, {f(z0)}, {f(w)}, {f(hh)}, {f(d)})" + (".mirror(false)" if c["mirror"] else "")
            bits.append(b)
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = (f"PartPose.offset({f(px)}, {f(py)}, {f(pz)})" if (rx, ry, rz) == (0, 0, 0)
                else f"PartPose.offsetAndRotation({f(px)}, {f(py)}, {f(pz)}, {f(rx)}, {f(ry)}, {f(rz)})")
        parent = "root" if p["parent"] is None else p["parent"]
        out.append(f'PartDefinition {p["name"]} = {parent}.addOrReplaceChild("{p["name"]}", CubeListBuilder.create(){"".join(bits)}, {pose});')
    return out


def own_models():
    for name, (make, size) in OWN.items():
        parts = make()
        uv = pack(parts, *size)
        if "--java" in sys.argv:
            print(f"// ---- {name} {size}")
            print("\n".join(java_parts(parts, uv)))
        img = paint(parts, uv, size)
        save(img, f"wildlife/{name}.png")
        save(skinned(img, {}, ()), f"skinned/wildlife/{name}.png")


# ======================================================================= puma (LeopardModel, 128x64)
def puma():
    dry = importlib.import_module("gen_fauna_warm_tex_dry")
    dry.LP_BACK, dry.LP_GOLD, dry.LP_PALE, dry.LP_WHITE = (150, 104, 62), (184, 136, 86), (210, 178, 136), (236, 226, 206)
    dry.LP_SPOT, dry.LP_CENTRE = (52, 38, 30), (184, 136, 86)
    dry.rosette = lambda *a, **k: None                                     # no rosettes, no spots: a plain coat
    dry.spot = lambda *a, **k: False

    def tail(start):
        def mat(v, face, x, y, z, i, j):
            d = start + y
            under = face in ("front", "top")
            if d > 17.0:
                return fur(dry.LP_SPOT, i, j, 760, 0.08)                   # the dark tail tip
            return fur(lerp(dry.LP_GOLD, dry.LP_PALE, 0.6) if under else dry.LP_GOLD, i, j, 761, 0.06)
        return mat

    boxes = dict(dry.LEOPARD)
    for name, start in (("tail0", 0), ("tail1", 8), ("tail2", 15)):
        boxes[name] = (*boxes[name][:8], tail(start))
    img = wildlife_paint.paint(boxes, "puma", (128, 64))
    save(img, "wildlife/puma.png")
    save(wildlife_paint.skinned(img, boxes, ("paw",)), "skinned/wildlife/puma.png")


# ======================================================================= snakes (SnakeModel, 64x32)
def snakes():
    ms = importlib.import_module("gen_fauna_meadow_textures")
    viper = dict(ground=(222, 198, 150), side=(206, 180, 132), zig=(150, 116, 74), belly=(236, 222, 190), eye=(196, 150, 60),
                 lip=(236, 222, 196), tongue=(30, 28, 32))
    parts = ms.snake_parts(viper_mat(ms, viper))
    uv = pack(parts, 64, 32)
    img = paint(parts, uv, (64, 32))
    save(img, "wildlife/sand_viper.png")
    save(skinned(img, {}, ()), "skinned/wildlife/sand_viper.png")
    parts = ms.snake_parts(rattle_mat(ms))
    img = paint(parts, uv, (64, 32))
    save(img, "wildlife/rattlesnake.png")
    save(skinned(img, {}, ()), "skinned/wildlife/rattlesnake.png")


def viper_mat(ms, P):
    """Pale sand: faint darker saddles across the back, a dark streak behind the eye, a raised 'horn' scale over each eye."""
    def m(kind):
        def mat(face, X, Y, Z, n, tx, ty):
            if kind == "tongue":
                fork = Z < -11.0 and abs(X) < 0.5
                return (0, 0, 0, 0) if fork and tx % 2 == 0 else P["tongue"]
            if face == "bottom":
                return ms.scales(P["belly"], tx, ty, 770)
            if kind == "head":
                if face in ("left", "right"):
                    if abs(Y - 22.5) < 0.5 and abs(Z - (-12.5)) < 0.5:
                        return P["eye"]
                    if abs(Y - 22.5) < 0.5 and Z > -12.0:
                        return P["zig"]
                    return ms.scales(P["lip"] if Y > 23.0 else P["side"], tx, ty, 771)
                if face == "top" and abs(abs(X) - 1.0) < 0.5 and abs(Z + 12.5) < 0.5:
                    return shade(P["zig"], 0.8)                               # the horn scales over the eyes
                return ms.scales(P["ground"], tx, ty, 772)
            s = ms.snake_size(Z)
            saddle = (s * 0.6) % 2 < 0.7
            if face == "top" or abs(n[1]) > 0.7:
                return ms.scales(lerp(P["ground"], P["zig"], 0.55) if saddle else P["ground"], tx, ty, 773)
            return ms.scales(lerp(P["side"], P["zig"], 0.35) if saddle and Y < 23.4 else P["side"], tx, ty, 774)
        return mat
    return m


RS = dict(ground=(150, 134, 108), side=(138, 122, 98), diamond=(62, 50, 40), edge=(222, 210, 176), belly=(214, 202, 170),
          eye=(190, 150, 70), lip=(214, 204, 178), band_dark=(30, 26, 24), band_light=(226, 220, 204), rattle=(206, 184, 140),
          tongue=(30, 28, 32))


def rattle_mat(ms):
    """Diamondback: dark diamonds with pale borders down the back, the tail ringed black and white, the rattle buff."""
    P = RS

    def m(kind):
        def mat(face, X, Y, Z, n, tx, ty):
            if kind == "tongue":
                fork = Z < -11.0 and abs(X) < 0.5
                return (0, 0, 0, 0) if fork and tx % 2 == 0 else P["tongue"]
            if kind == "tail":
                return shade(P["rattle"], 1.1 if ty % 2 else 0.85)            # the segmented rattle
            if face == "bottom":
                return ms.scales(P["belly"], tx, ty, 780)
            if kind == "head":
                if face in ("left", "right"):
                    if abs(Y - 22.5) < 0.5 and abs(Z - (-12.5)) < 0.5:
                        return P["eye"]
                    if abs(Y - 22.5) < 0.5 and Z > -12.0:
                        return P["diamond"]
                    if abs(Y - 23.3) < 0.4:
                        return P["edge"]                                   # the pale line from the eye to the jaw
                    return ms.scales(P["side"], tx, ty, 781)
                return ms.scales(P["ground"], tx, ty, 782)
            s = ms.snake_size(Z)
            if s > 19.0:                                                   # the coon tail: black and white rings
                return P["band_dark"] if int(s * 1.4) % 2 == 0 else P["band_light"]
            if face == "top" or abs(n[1]) > 0.7:
                k = abs(((s * 0.5) % 2) - 1)                               # diamond half-width along the body
                d = abs(X) - (1.0 - k) * 1.6
                if d < -0.15:
                    return ms.scales(P["diamond"], tx, ty, 783)
                if d < 0.45:
                    return P["edge"]
                return ms.scales(P["ground"], tx, ty, 784)
            spot = abs(((s * 0.5 + 1) % 2) - 1) < 0.3 and Y < 23.6
            return ms.scales(lerp(P["side"], P["diamond"], 0.6) if spot else P["side"], tx, ty, 785)
        return mat
    return m


# ======================================================================= condor (ForestBirdModel vulture layer, 64x64)
def condor():
    fb = importlib.import_module("gen_fauna_forestbirds_textures")
    S = fb.SPECIES["vulture"]
    parts = fb.geometry(S)
    uv = pack(parts, *fb.SIZE)
    BLACK, BLACK_L, WHITE, HEAD, BEAK, LEG = (26, 24, 26), (52, 50, 54), (240, 238, 232), (176, 96, 84), (214, 204, 180), (120, 116, 112)

    def body(p):
        return fb.mottle(BLACK, BLACK_L, p, 801, 0.25)

    def neck(p):
        return fb.fe(lerp(HEAD, (120, 92, 88), 0.4 + 0.4 * h(p.tx, p.ty, 802)), p, 803, 0.05)  # bare, wrinkled

    def head(p):
        if fb.is_eye(p, S):
            return (120, 40, 30)
        return fb.fe(HEAD, p, 804, 0.06)

    def beak(p):
        return BEAK if p.fz > 0.4 else (60, 56, 52)

    def ruff(p):
        return fb.fe(WHITE, p, 805, 0.05)

    def wing(p):
        if not p.upper:
            return fb.fe(BLACK_L, p, 806)
        if 0.35 < p.s < 0.75 and p.c > 0.45:
            return fb.fe(WHITE, p, 807, 0.05)                                 # the white panel on the upper wing
        return fb.feathers_along(p, BLACK, 808)

    def tail(p):
        return fb.fe(BLACK, p, 809)

    def leg(p):
        return fb.fe(LEG, p, 810, 0.05)

    painters = dict(body=body, neck=neck, head=head, beak=beak, ruff=ruff, wing=wing, tip=wing, tail=tail, leg=leg, foot=leg)
    save(fb.gbt.paint(parts, uv, S, painters), "wildlife/condor.png")


# ======================================================================= bighorn sheep (BighornModel = IbexModel's cubes, 64x64)
def bighorn():
    mt = importlib.import_module("gen_fauna_mountain_textures")
    horn = ((150, 132, 100), (190, 172, 134), (214, 200, 166))
    mt.PAL["bighorn_ram"] = dict(back=(110, 82, 56), coat=(138, 104, 70), light=(166, 132, 96), belly=(232, 224, 206), dark=(80, 60, 42),
                                 face=(128, 98, 68), muzzle=(226, 218, 200), stripe=None, horn=horn)
    mt.PAL["bighorn_ewe"] = dict(mt.PAL["bighorn_ram"], back=(126, 96, 66), coat=(152, 118, 82), face=(142, 110, 78),
                                 horn=((138, 122, 96), (172, 156, 124), (196, 184, 156)))
    mt.PAL["bighorn_lamb"] = dict(mt.PAL["bighorn_ram"], back=(150, 122, 92), coat=(172, 142, 108), face=(162, 132, 100),
                                  light=(190, 164, 132), horn=((150, 136, 112), (176, 162, 136), (196, 186, 160)))
    plain_body = mt.IBEX["body"][8]

    def body(v, face, x, y, z, i, j):
        p = mt.PAL[v]
        t = (y + 4) / 9
        if face == "back" or (face in ("left", "right") and z > 5.5 and t > 0.25):
            return fur(p["belly"], i, j, 820, 0.04)                           # the white rump patch
        return plain_body(v, face, x, y, z, i, j)

    def tail(v, face, x, y, z, i, j):
        return fur(mt.PAL[v]["dark"] if face == "back" else mt.PAL[v]["belly"], i, j, 821, 0.05)

    boxes = dict(mt.IBEX)
    boxes["body"] = (*boxes["body"][:8], body)
    boxes["tail"] = (*boxes["tail"][:8], tail)
    boxes["horn"] = (*boxes["horn"][:8], mt.horn(0.0, 0.5, 4))               # fine growth ridges, no knobs
    boxes["horn_thin"] = (*boxes["horn_thin"][:8], mt.horn(0.5, 1.0, 4))
    for v in ("bighorn_ram", "bighorn_ewe", "bighorn_lamb"):
        img = wildlife_paint.paint(boxes, v, (64, 64))
        save(img, f"wildlife/{v}.png")
        if v != "bighorn_lamb":
            save(wildlife_paint.skinned(img, boxes, ("horn", "horn_thin", "nanny_horn")), f"skinned/wildlife/{v}.png")


def main():
    own_models()
    puma()
    snakes()
    condor()
    bighorn()
    importlib.import_module("gen_fauna_desert_tex_items").main()
    print("fauna desert textures ok")


if __name__ == "__main__":
    main()
