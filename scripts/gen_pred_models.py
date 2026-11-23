"""The older predators' and Ice Age giants' models and skins, each from one table (the gen_elkwight_model.py pattern):

- src/main/java/com/mechanical/client/wildlife/giants/GiantsGeometry.java: Woolly Mammoth, Woolly Rhinoceros, Steppe
  Bison, Musk Ox (generated, do not edit; MammothModel / RhinoModel / BisonModel / MuskOxModel animate them)
- src/main/java/com/mechanical/client/wildlife/PredGeometry.java: Grey Wolf and Cave Lion (PredatorModel animates them)
  and the bear cub (BearCubModel; the cave bear cub shares it, recoloured by gen_fauna_mountain_textures.py)
- textures/entity/wildlife/giants/*.png, wildlife/grey_wolf.png, wildlife/cave_lion.png, wildlife/brown_bear(_cub).png and the skinned carcass
  skins in textures/entity/skinned/. The adult brown bear keeps BrownBearModel's hand-written boxes: only its skin is
  painted here, on those UVs.

Mammoth: built at half size, drawn twice as big, so its skin has two texels per model pixel. A high domed skull, the
shoulder hump, a back that falls away to small hindquarters, pillar legs on round toenailed feet, a curtain of long
ginger-tipped hair, a long ringed trunk and spiralling ivory. Rhino: long low barrel, a muscled hump, head hung low, a
long flattened front horn curving up and a short second horn, square lips, shaggy flanks. Steppe bison: a tall woolly
cape over the hump and forelegs, slim hindquarters, a shaggy head with beard and long, wide horns. Musk ox: a curtain of
dark guard hair almost to the ground, a pale saddle, pale stockings, the broad horn boss with hooked horns. Grey wolf:
deep chest under a thick ruff, narrow waist, long legs, long narrow muzzle, cheek ruffs, bushy tail. Bear cub: round
and fluffy, big head with round ears and a short muzzle, chubby legs. Cave lion: a long low maneless cat with heavy
shoulders, a broad head with jowls, a real muzzle and jaw, big paws, faint stripes over the hindquarters, a pale belly
and a long tail ending in a dark tuft.

Usage: python scripts/gen_pred_models.py (after gen_fauna_giants_textures.py, gen_predator_textures.py; before
gen_fauna_mountain_textures.py, which recolours the bear skins for the cave bear, and gen_aged_textures.py).
Deterministic (hashes only).
"""
import math
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from box_paint import Model, h, lerp, lit, pack, paint, shade  # noqa: E402
from gen_skinned_textures import skin  # noqa: E402

ROOT = HERE.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife"
EYE, GLINT = (14, 10, 8), (200, 204, 210)


# ======================================================================= shared tools

def label(M, pid, name):
    """The part's name in the game when the id differs (several legs each have a child named "lower")."""
    M.by[pid]["label"] = name


def fit(M):
    for size in ((64, 32), (64, 64), (128, 64), (128, 128), (256, 128), (256, 256)):
        try:
            return pack(M.parts, *size), size
        except AssertionError:
            pass
    raise AssertionError(M.name + ": texture full")


def jf(v):
    s = f"{float(v):.4f}".rstrip("0")
    return (s + "0" if s.endswith(".") else s) + "F"


def java_method(method, M, uv, size):
    lines = []
    for p in M.parts:
        bits = ""
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            bits += (f"\n                .texOffs({u}, {v})" + (".mirror()" if c["mirror"] else "")
                     + f".addBox({jf(x0)}, {jf(y0)}, {jf(z0)}, {jf(w)}, {jf(hh)}, {jf(d)})" + (".mirror(false)" if c["mirror"] else ""))
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = (f"PartPose.offset({jf(px)}, {jf(py)}, {jf(pz)})" if (rx, ry, rz) == (0, 0, 0)
                else f"PartPose.offsetAndRotation({jf(px)}, {jf(py)}, {jf(pz)}, {jf(rx)}, {jf(ry)}, {jf(rz)})")
        parent = "root" if p["parent"] is None else p["parent"]
        lines.append(f'        PartDefinition {p["name"]} = {parent}.addOrReplaceChild("{p.get("label", p["name"])}", '
                     f'CubeListBuilder.create(){bits},\n                {pose});')
    boxes = sum(len(p["cubes"]) for p in M.parts)
    return (f"    /** {M.name}: {len(M.parts)} parts, {boxes} boxes, skin {size[0] * M.k}x{size[1] * M.k}. */\n"
            f"    static LayerDefinition {method}() {{\n        MeshDefinition mesh = new MeshDefinition();\n"
            f"        PartDefinition root = mesh.getRoot();\n" + "\n".join(lines)
            + f"\n        return LayerDefinition.create(mesh, {size[0]}, {size[1]});\n    }}\n")


def write_java(path, package, cls, methods):
    path.write_text(f"""package {package};

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_pred_models.py (with the skins it paints for these boxes). Do not edit. */
@SuppressWarnings("unused")
final class {cls} {{
{chr(10).join(methods)}
    private {cls}() {{}}
}}
""", encoding="utf-8")


def skinned(img, M, uv, keep=()):
    """The flesh version for butchered carcasses; boxes of the materials in keep (horns, hooves, ivory) stay as they are."""
    out = skin(img.copy())
    src, dst = img.load(), out.load()
    k = M.k
    for p in M.parts:
        for c in p["cubes"]:
            if c["mname"] not in keep:
                continue
            u, v = uv[c["key"]]
            _, _, _, w, hh, d = c["box"]
            for tx in range(round(u * k), round((u + 2 * (d + w)) * k)):
                for ty in range(round(v * k), round((v + d + hh) * k)):
                    dst[tx, ty] = src[tx, ty]
    return out


def save(img, rel):
    out = TEX / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print("wrote", out.relative_to(ROOT))


def rel(v, lo, hi):
    return max(0.0, min(1.0, (v - lo) / (hi - lo)))


def locks(c, q, salt, amt=0.1, run=3):
    """Long hair: locks a few texels long running down the face (along it on tops), over a fine speckle."""
    n = (h(q.tx, q.ty // run, salt) - 0.5) * 2 * amt + (h(q.tx, q.ty, salt + 1) - 0.5) * amt * 0.7
    return shade(c, 1 + n)


def short_fur(c, q, salt, amt=0.06):
    return shade(c, 1 + (h(q.tx, q.ty, salt) - 0.5) * 2 * amt + (h(q.tx, q.ty // 2, salt + 3) - 0.5) * amt)


def side(q):
    return q.face in ("left", "right", "front", "back")


def ragged(q, length, salt, run=1):
    """Jagged hair ends: the bottom few texel rows of a hanging side face, a different length per column."""
    if q.face in ("top", "bottom"):
        return True  # hair hangs from under the body: no ledge on top, no floor under it
    cut = round(length * q.k * h(q.i // run, q.fw, salt))
    return q.j >= q.fh - cut


def eye_at(q, y, z, axis_side=True, size=0.5):
    """On a side face (part-local x faces), the eye at local (y, z): True for the eye, 'glint' on its top front texel."""
    if q.face not in ("left", "right") or not axis_side:
        return False
    if abs(q.y - y) <= size and abs(q.z - z) <= size:
        return "glint" if q.y < y and q.z < z and q.k > 1 else True
    return False


def gradient(stops, t):
    t = max(0.0, min(1.0, t)) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    return lerp(stops[i], stops[i + 1], t - i)


def legs4(M, front, hind, boxes_front, boxes_hind, mats_front, mats_hind, lower_front=None, lower_hind=None):
    """Four legs, right side given (x < 0), the left mirrored: a root child per leg (names the models look up) and,
    when lower boxes are given, a child "lower" at (0, ly, lz) with those boxes."""
    for (fx, fy, fz), boxes, mats, lower, pre in ((front, boxes_front, mats_front, lower_front, "front"),
                                                  (hind, boxes_hind, mats_hind, lower_hind, "hind")):
        for s, sign in (("right", -1), ("left", 1)):
            leg = f"{s}_{pre}_leg"
            M.part(leg, None, (sign * fx, fy, fz))
            M.limb(leg, sign, *boxes, mat=mats)
            if lower:
                (ly, lz), lboxes, lmats = lower
                lid = f"{s}_{pre}_lower"
                M.part(lid, leg, (0, ly, lz))
                label(M, lid, "lower")
                M.limb(lid, sign, *lboxes, mat=lmats)


def build(M, keep, skins, method):
    """Packs and paints one model: skins = {file: variant}; returns the Java method."""
    uv, size = fit(M)
    M.size = size
    for rel_path, variant in skins.items():
        M.variant = variant
        img = paint(M.parts, uv, size, M.k)
        save(img, f"wildlife/{rel_path}.png")
        if variant != "calf":  # calves never leave a carcass
            save(skinned(img, M, uv, keep), f"skinned/wildlife/{rel_path}.png")
    print(f"{M.name}: {len(M.parts)} parts, {sum(len(p['cubes']) for p in M.parts)} boxes, {size[0] * M.k}x{size[1] * M.k}")
    return java_method(method, M, uv, size)


# ======================================================================= woolly mammoth (half size, k = 2)

MA = dict(root=(38, 24, 18), dark=(62, 38, 24), base=(96, 60, 36), warm=(128, 80, 44), ginger=(170, 112, 58),
          tip=(196, 140, 78), face=(74, 50, 36), skin=(66, 58, 54), skin_lt=(98, 88, 80), sole=(52, 46, 44),
          nail=(178, 168, 146), ear=(58, 40, 30))
IVORY = [(150, 118, 70), (204, 178, 128), (232, 216, 178), (244, 236, 212)]
MA_CALF = dict(MA, dark=(92, 54, 30), base=(140, 86, 46), warm=(172, 110, 58), ginger=(200, 140, 76), tip=(222, 172, 104),
               face=(120, 78, 48))


def mammoth_colour(m, q):
    P = MA_CALF if M_MAMMOTH.variant == "calf" else MA
    Y = q.Y
    if m in ("fore", "hump", "hump_top", "barrel", "slope", "rump", "rump_top", "belly"):
        # long hair: dark at the roots high on the back, ginger at the tips lower down; tufts on top
        t = rel(Y, -3, 15)
        if q.face == "top":
            c = lerp(P["dark"], P["base"], 0.35 + 0.4 * h(q.tx // 2, q.ty // 3, 11))
        elif q.face == "bottom":
            c = P["root"]
        else:
            c = gradient([P["dark"], P["base"], P["warm"], P["ginger"]], t * 0.9 + 0.1 * h(q.tx // 2, 0, 12))
        if side(q) and h(q.tx // 2, q.ty // 6, 13) > 0.86:
            c = lerp(c, P["tip"], 0.45)  # a ginger lock catching the light
        return lit(locks(c, q, 14, 0.12, 6), q, -3, 24)
    if m in ("skirt", "bib", "chap"):
        if ragged(q, 2.5 if m != "chap" else 1.5, 15):
            return None
        t = q.j / max(1, q.fh - 1) if side(q) else 0.5
        c = gradient([P["base"], P["warm"], P["ginger"], P["tip"]], t)
        if h(q.tx // 2, 0, 16) > 0.7:
            c = lerp(c, P["dark"], 0.4)  # darker locks between the pale ones
        return lit(locks(c, q, 17, 0.14, 8), q, -3, 24)
    if m in ("skull", "dome", "peak", "brow", "face", "lip"):
        if m in ("dome", "peak") or (m == "skull" and q.face == "top"):
            c = lerp(P["dark"], P["warm"], 0.3 + 0.5 * h(q.tx // 2, q.ty // 2, 21))  # the hair tuft on the crown
            return lit(locks(c, q, 22, 0.14, 4), q, -10, 24)
        if m == "skull" and q.face in ("left", "right"):
            dy, dz = q.y + 1.25, q.z + 5.75
            if abs(dy) < 0.1 and abs(dz) < 0.1:
                return (10, 6, 4)  # a small dark eye
            if abs(dy) < 0.6 and abs(dz) < 0.6:
                return lit(lerp(P["face"], P["root"], 0.6), q, -10, 24)  # the bare skin round it
            if -1.6 < dy < -0.4 and abs(dz) < 1.4:
                # a fringe of long hair hanging over the eye, lock by lock
                return lit(lerp(P["dark"], P["warm"], 0.25 + 0.5 * h(q.tx, 0, 30)), q, -10, 24) if h(q.tx, q.ty, 31) > 0.2 else \
                    lit(lerp(P["face"], P["root"], 0.6), q, -10, 24)
        if m == "lip":
            return lit(lerp(P["skin"], P["root"], 0.3), q, -10, 24)
        c = lerp(P["face"], P["base"], 0.25 * h(q.tx // 2, q.ty // 3, 23))
        if m == "face" and q.face == "front":
            c = lerp(c, P["skin"], 0.35 * rel(q.y, 1, 6))  # the hair thins towards the trunk
        return lit(locks(c, q, 24, 0.08, 3), q, -10, 24)
    if m == "ear":
        if q.face in ("left", "right", "front", "back") and (q.j < q.k or q.j >= q.fh - q.k) and (q.i < q.k or q.i >= q.fw - q.k):
            return None  # rounded corners
        return lit(lerp(P["ear"], P["dark"], 0.3 * h(q.tx, q.ty, 25)), q, -10, 24)
    if m.startswith("trunk"):
        seg = int(m[-1])
        # hairy at the root, bare wrinkled grey skin to the tip, rings of wrinkles all down
        t = (seg - 1 + (q.j / max(1, q.fh - 1) if side(q) else 0.5)) / 4
        c = lerp(lerp(P["face"], P["base"], 0.3), P["skin"], rel(t, 0.15, 0.6))
        if side(q) and (q.j // q.k) % 2 == 1:
            c = shade(c, 0.88)
        if m == "trunk5":
            c = P["skin_lt"] if q.face != "bottom" else P["sole"]
        return lit(short_fur(c, q, 26, 0.05), q, -10, 24)
    if m.startswith("tusk"):
        seg = int(m[-1])
        t = (seg - 1) / 3.0
        c = gradient(IVORY, 0.15 + 0.85 * t)
        if seg == 1 and q.face in ("left", "right", "top", "bottom"):
            c = lerp(c, IVORY[0], 0.35 * (1 - rel(q.z, -5, 0)))  # stained where it leaves the lip
        if (q.tx + q.ty * 3) % 7 == 0:
            c = shade(c, 0.93)  # growth lines
        return lit(c, q, -10, 24)
    if m in ("tail", "tuft"):
        c = P["base"] if m == "tail" else lerp(P["root"], P["dark"], h(q.tx, q.ty, 27))
        return lit(locks(c, q, 28, 0.12, 3), q, -3, 24)
    if m in ("upper", "lower"):
        t = rel(Y, 12, 23)
        c = gradient([P["base"], P["dark"], P["root"]], t)
        return lit(locks(c, q, 29, 0.1, 4), q, -3, 24)
    if m == "foot":
        if q.face == "bottom":
            return P["sole"]
        nail = side(q) and q.j >= q.fh - q.k and (q.i // q.k) % 2 == 1 and q.face != "back"
        return lit(P["nail"] if nail else lerp(P["skin"], P["root"], 0.4), q, -3, 24)
    return P["base"]


def mammoth():
    M = Model("Woolly mammoth", None, 2, mammoth_colour)
    M.part("body", None, (0, 8, 0))
    M.box("body", "fore", -7, -7, -13, 14, 13, 11)
    M.box("body", "hump", -5.5, -10, -12, 11, 3, 8.5)
    M.box("body", "hump_top", -4, -11, -10.5, 8, 1, 5)
    M.box("body", "barrel", -6.5, -6, -2, 13, 12, 9)
    M.box("body", "slope", -5.5, -8, -4, 11, 2, 7)
    M.box("body", "rump", -6, -4.5, 7, 12, 10, 5.5)
    M.box("body", "rump_top", -5, -5.5, 7, 10, 1, 4.5)
    M.box("body", "belly", -6, 6, -11, 12, 1, 18)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_skirt", "body", (sign * 6.5, 3, 0))
        M.limb(f"{s}_skirt", sign, (-1, 0, -12.5, 1.5, 8.5, 23), mat="skirt")
    M.part("bib", "body", (0, 4, -13))
    M.box("bib", "bib", -5, 0, -1, 10, 7, 1.5)
    M.part("tail", "body", (0, -3, 12.5), (0.15, 0, 0))
    M.box("tail", "tail", -1, 0, -1, 2, 5, 2)
    M.box("tail", "tuft", -1.5, 4.5, -1.5, 3, 3, 3)

    M.part("head", "body", (0, -5, -12.5), (0.2, 0, 0))
    M.box("head", "skull", -4.5, -5, -7, 9, 9, 7)
    # the domed crown in thin layers, each a little smaller: a smooth rise instead of steps
    M.box("head", "dome", -4.25, -6.5, -6.75, 8.5, 1.5, 6.5)
    M.box("head", "dome", -3.75, -7.5, -6.25, 7.5, 1, 5.5)
    M.box("head", "dome", -3, -8.5, -5.5, 6, 1, 4)
    M.box("head", "peak", -2, -9, -4.75, 4, 0.5, 2.5)
    M.box("head", "brow", -4, -4, -8, 8, 5, 1)
    M.box("head", "face", -3, 1, -7.5, 6, 5, 5)
    M.box("head", "lip", -1.5, 6, -6, 3, 1, 3)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_ear", "head", (sign * 4.5, -1.5, -1.5), (0, -0.3 * sign, -0.2 * sign))
        M.limb(f"{s}_ear", sign, (-1, -2, -1.5, 1, 4.5, 3.5), mat="ear")
    M.part("trunk1", "head", (0, 4.5, -7), (-0.2, 0, 0))
    M.box("trunk1", "trunk1", -2.25, 0, -2.25, 4.5, 5.5, 4.5)
    M.part("trunk2", "trunk1", (0, 5, 0))
    M.box("trunk2", "trunk2", -1.75, 0, -1.75, 3.5, 5, 3.5)
    M.part("trunk3", "trunk2", (0, 4.5, 0))
    M.box("trunk3", "trunk3", -1.5, 0, -1.5, 3, 4.5, 3)
    M.part("trunk4", "trunk3", (0, 4, 0), (-0.4, 0, 0))
    M.box("trunk4", "trunk4", -1.25, 0, -1.25, 2.5, 3, 2.5)
    M.box("trunk4", "trunk5", -1.5, 3, -1.5, 3, 1, 3)
    M.part("tusks", "head", (0, 0, 0))
    for s, sign in (("right", -1), ("left", 1)):
        p = s + "_"
        M.part(p + "tusk1", "tusks", (3 * sign, 4.5, -6.5), (1.05, -0.45 * sign, 0))
        M.limb(p + "tusk1", sign, (-1.25, -1.25, -5, 2.5, 2.5, 5), mat="tusk1")
        M.part(p + "tusk2", p + "tusk1", (0, 0, -4.5), (-0.6, -0.25 * sign, 0))
        M.limb(p + "tusk2", sign, (-1, -1, -5.5, 2, 2, 5.5), mat="tusk2")
        M.part(p + "tusk3", p + "tusk2", (0, 0, -5), (-0.75, 0.45 * sign, 0))
        M.limb(p + "tusk3", sign, (-1, -1, -5, 2, 2, 5), mat="tusk3")
        M.part(p + "tusk4", p + "tusk3", (0, 0, -4.5), (-0.6, 0.6 * sign, 0))
        M.limb(p + "tusk4", sign, (-0.75, -0.75, -4, 1.5, 1.5, 4), mat="tusk4")
    legs4(M, (4.5, 12, -7), (4.5, 12, 8),
          [(-3, -1, -3, 6, 8, 6), (-3.25, 3, -3.25, 6.5, 4, 6.5)], [(-3, -1, -3, 6, 8, 6)], ["upper", "chap"], ["upper"],
          ((7, 0), [(-2.75, 0, -2.75, 5.5, 4, 5.5), (-3, 4, -3, 6, 1, 6)], ["lower", "foot"]),
          ((7, 0), [(-2.75, 0, -2.75, 5.5, 4, 5.5), (-3, 4, -3, 6, 1, 6)], ["lower", "foot"]))
    return M


M_MAMMOTH = None


# ======================================================================= woolly rhinoceros

RH = dict(root=(46, 32, 22), dark=(70, 48, 30), base=(108, 74, 44), warm=(140, 100, 60), pale=(170, 132, 86),
          face=(96, 70, 48), skin=(78, 66, 56), lip=(62, 52, 46), horn=[(44, 38, 32), (72, 62, 50), (108, 94, 76), (142, 128, 104)],
          hoof=(46, 40, 36), ear=(84, 60, 40))
RH_CALF = dict(RH, dark=(104, 72, 44), base=(146, 104, 64), warm=(176, 134, 86), pale=(204, 170, 120), face=(132, 98, 66))


def rhino_colour(m, q):
    P = RH_CALF if M_RHINO.variant == "calf" else RH
    Y = q.Y
    if m in ("chest", "hump", "hump_top", "barrel", "back", "rump", "rump_top", "belly", "neck"):
        t = rel(Y, -2, 17)
        if q.face == "top":
            c = lerp(P["dark"], P["base"], 0.3 + 0.4 * h(q.tx, q.ty // 2, 31))
        elif q.face == "bottom":
            c = P["root"]
        else:
            c = gradient([P["base"], P["warm"], P["base"], P["dark"]], t)
        if side(q) and h(q.tx, q.ty // 4, 32) > 0.88:
            c = lerp(c, P["pale"], 0.4)
        return lit(locks(c, q, 33, 0.12, 4), q, -2, 24)
    if m == "skirt":
        if ragged(q, 2.5, 34):
            return None
        c = gradient([P["base"], P["warm"], P["pale"]], q.j / max(1, q.fh - 1) if side(q) else 0.4)
        if h(q.tx, 0, 35) > 0.7:
            c = lerp(c, P["dark"], 0.45)
        return lit(locks(c, q, 36, 0.14, 6), q, -2, 24)
    if m in ("skull", "crown", "snout", "boss", "lip"):
        e = eye_at(q, -0.5, -7.5, size=0.5)
        if m == "skull" and e:
            return EYE
        if m == "lip" or (m == "snout" and q.face == "front" and q.y > 1.5):
            return lit(P["lip"], q, -2, 24)  # the square grazing lips
        if m == "snout" and q.face in ("left", "right") and abs(q.y - 0.5) < 0.6 and q.z < -13.5:
            return lit(lerp(P["lip"], EYE, 0.4), q, -2, 24)  # nostril
        if m == "crown":
            c = lerp(P["dark"], P["base"], 0.5 * h(q.tx, q.ty, 37))
            return lit(locks(c, q, 38, 0.14, 3), q, -2, 24)
        c = lerp(P["face"], P["skin"], 0.5 * rel(-q.z, 8, 14))  # the hair gives way to bare skin on the nose
        return lit(locks(c, q, 39, 0.08, 3), q, -2, 24)
    if m == "ear":
        c = P["ear"] if q.face != "front" else lerp(P["ear"], P["root"], 0.4)
        return lit(locks(c, q, 40, 0.1, 2), q, -2, 24)
    if m.startswith("horn"):
        seg = {"horn1": 0, "horn2": 1, "horn3": 2, "horn_rear": 0.6}[m]
        t = (seg + (1 - q.j / max(1, q.fh - 1) if side(q) else 0.5)) / 3
        c = gradient(P["horn"], t)
        if q.face == "front" and m != "horn_rear":
            c = lerp(c, P["horn"][3], 0.35)  # the front edge worn pale by sweeping the snow
        if (q.ty * 2 + q.tx) % 5 == 0:
            c = shade(c, 0.9)
        return lit(c, q, -2, 24)
    if m in ("tail", "tuft"):
        c = P["base"] if m == "tail" else P["root"]
        return lit(locks(c, q, 41, 0.12, 2), q, -2, 24)
    if m in ("upper", "lower"):
        c = gradient([P["base"], P["dark"], P["root"]], rel(Y, 14, 23))
        return lit(locks(c, q, 42, 0.1, 3), q, -2, 24)
    if m == "foot":
        if q.face == "bottom":
            return P["hoof"]
        toe = side(q) and q.face != "back" and q.i % 2 == 0
        return lit(lerp(P["hoof"], (120, 110, 96), 0.4) if toe else P["hoof"], q, -2, 24)
    return P["base"]


def rhino():
    M = Model("Woolly rhinoceros", None, 1, rhino_colour)
    M.part("body", None, (0, 8, 0))
    M.box("body", "chest", -8, -6, -16, 16, 14, 12)
    M.box("body", "hump", -6, -9, -15, 12, 3, 9)
    M.box("body", "hump_top", -4, -10, -13, 8, 1, 5)
    M.box("body", "barrel", -7.5, -5, -4, 15, 13, 12)
    M.box("body", "rump", -7, -4, 8, 14, 11, 7)
    M.box("body", "rump_top", -5.5, -5, 8, 11, 1, 6)
    M.box("body", "belly", -6.5, 8, -13, 13, 1, 20)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_skirt", "body", (sign * 7.5, 4, 0))
        M.limb(f"{s}_skirt", sign, (-1, 0, -15, 1, 7, 28), mat="skirt")
    M.part("tail", "body", (0, -3, 15), (0.2, 0, 0))
    M.box("tail", "tail", -0.5, 0, -0.5, 1, 6, 1)
    M.box("tail", "tuft", -1, 5, -1, 2, 3, 2)
    M.part("neck", "body", (0, 0, -15), (0.25, 0, 0))
    M.box("neck", "neck", -5, -5, -6, 10, 11, 7)
    M.part("head", "neck", (0, 1.5, -5.5), (0.55, 0, 0))
    M.box("head", "skull", -4.5, -4.5, -10, 9, 8, 10)
    M.box("head", "crown", -3.5, -5.5, -9, 7, 1, 7)
    M.box("head", "snout", -4, -2.5, -15, 8, 7, 5)
    M.box("head", "boss", -3, -3.5, -14.5, 6, 1, 4)
    M.box("head", "lip", -3.5, 4.5, -14, 7, 1, 4)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_ear", "head", (sign * 3.5, -5, -2), (-0.4, 0, 0.4 * sign))
        M.limb(f"{s}_ear", sign, (-1, -3, -0.5, 2, 3, 1), mat="ear")
    M.part("horns", "head", (0, 0, 0))
    M.part("front_horn", "horns", (0, -3, -12.5), (-0.35, 0, 0))
    M.box("front_horn", "horn1", -1, -5, -2, 2, 5, 4)
    M.part("front_horn_mid", "front_horn", (0, -4.5, 0), (-0.2, 0, 0))
    M.box("front_horn_mid", "horn2", -1, -5, -1.5, 2, 5, 3)
    M.part("front_horn_tip", "front_horn_mid", (0, -4.5, 0), (-0.25, 0, 0))
    M.box("front_horn_tip", "horn3", -0.5, -4, -1, 1, 4, 2)
    M.part("rear_horn", "horns", (0, -4.5, -8), (0.1, 0, 0))
    M.box("rear_horn", "horn_rear", -1, -3, -1.5, 2, 3, 3)
    legs4(M, (5, 15, -10), (5, 15, 10),
          [(-3, -1, -3, 6, 6, 6)], [(-3, -1, -3, 6, 6, 6)], ["upper"], ["upper"],
          ((5, 0), [(-2.5, 0, -2.5, 5, 3, 5), (-3, 3, -3, 6, 1, 6)], ["lower", "foot"]),
          ((5, 0), [(-2.5, 0, -2.5, 5, 3, 5), (-3, 3, -3, 6, 1, 6)], ["lower", "foot"]))
    return M


M_RHINO = None


# ======================================================================= steppe bison

BI = {
    "bull": dict(cape=(150, 104, 58), cape_lt=(184, 136, 78), body=(62, 42, 28), dark=(30, 22, 18), beard=(24, 18, 15),
                 face=(48, 34, 26), nose=(30, 26, 24), hoof=(30, 26, 24)),
    "cow": dict(cape=(160, 116, 70), cape_lt=(192, 148, 92), body=(92, 64, 42), dark=(52, 38, 28), beard=(44, 32, 24),
                face=(72, 52, 38), nose=(36, 30, 28), hoof=(36, 30, 28)),
    "calf": dict(cape=(196, 120, 60), cape_lt=(220, 150, 84), body=(170, 100, 50), dark=(120, 70, 36), beard=(110, 66, 34),
                 face=(150, 90, 46), nose=(60, 44, 36), hoof=(60, 50, 44)),
}
BISON_HORN = [(92, 78, 62), (66, 56, 46), (44, 38, 32), (22, 19, 17)]  # pale ridged base to a black tip


def tufts(c, q, salt):
    """Matted wool in tufts: clumps two texels wide and three long, staggered, each with a pale tip and a dark gap."""
    col = q.tx // 2
    y = q.ty + int(h(col, 0, salt) * 4)  # every tuft column starts at its own height
    layer, p = y // 4, y % 4
    c = shade(c, 0.86 + 0.26 * h(col, layer, salt + 1))
    if p == 0 and h(col, layer, salt + 2) < 0.7:
        c = shade(c, 0.8)  # in the shadow of the layer above
    elif p == 3:
        c = shade(c, 1.08)  # the tip of the tuft catches the light
    return shade(c, 1 + (h(q.tx, q.ty, salt + 3) - 0.5) * 0.08)


def bison_colour(m, q):
    P = BI[M_BISON.variant]
    Y, Z = q.Y, q.Z
    if m in ("cape", "hump1", "hump2", "hump_tuft", "cape_edge", "cape_layer", "hump_layer", "brisket", "barrel", "rump", "back"):
        # the golden woolly cape over hump, shoulders and forelegs; the hindquarters short, dark and sleek
        cape = rel(-Z, -3, 3)  # 0 behind the shoulders, 1 in front
        if m in ("hump1", "hump2", "hump_tuft", "cape_edge", "cape_layer", "hump_layer"):
            cape = 1.0
        if m in ("cape_edge", "cape_layer", "hump_layer") and ragged(q, 3 if m != "hump_layer" else 2, 50 + len(m)):
            return None
        if m == "brisket":
            if ragged(q, 2, 51):
                return None
            return lit(locks(lerp(P["beard"], P["body"], 0.3 * h(q.tx, 0, 52)), q, 53, 0.14, 4), q, -5, 24)
        wool = lerp(P["cape"], P["cape_lt"], 0.3 * h(q.tx // 2, q.ty // 2, 54) + 0.7 * (1 - rel(Y, -5, 6)))
        wool = lerp(wool, P["body"], rel(Y, 3, 15) * 0.9)  # darker down the legs and chest
        sleek = lerp(P["body"], P["dark"], rel(Y, 6, 15) * 0.6)
        c = lerp(sleek, wool, cape)
        if q.face == "bottom":
            c = P["dark"]
        if m in ("cape_layer", "hump_layer", "cape_edge") and side(q):
            c = shade(c, 0.78 + 0.34 * q.j / max(1, q.fh - 1))  # each layer: shadowed roots, pale tips
        if cape > 0.5:
            return lit(tufts(c, q, 55), q, -5, 24)
        return lit(locks(c, q, 55, 0.06, 3), q, -5, 24)
    if m in ("skull", "wool", "cheek", "muzzle", "nose"):
        e = eye_at(q, 0.5, -4.5, size=0.5)
        if m == "skull" and e:
            return EYE
        if m == "nose" or (m == "muzzle" and q.face == "front"):
            c = P["nose"]
            if m == "nose" and q.face == "front" and abs(abs(q.x) - 1) < 0.6 and q.y < 0.5:
                c = shade(c, 0.6)  # nostrils
            return lit(c, q, -5, 24)
        if m in ("wool", "cheek"):
            c = lerp(P["dark"], P["cape"], 0.35 + 0.35 * h(q.tx, q.ty // 2, 56)) if m == "wool" else P["face"]
            return lit(locks(c, q, 57, 0.18, 2), q, -5, 24)
        return lit(locks(P["face"], q, 58, 0.1, 2), q, -5, 24)
    if m == "beard":
        if ragged(q, 2, 59):
            return None
        return lit(locks(P["beard"], q, 60, 0.15, 4), q, -5, 24)
    if m == "ear":
        return lit(P["face"] if q.face != "top" else P["dark"], q, -5, 24)
    if m.startswith("horn") or m.startswith("cowhorn"):
        seg = int(m[-1])
        along = (1 - q.j / max(1, q.fh - 1)) if side(q) else 0.5  # boxes run along -y: row 0 is the far end
        t = (seg - 1 + along) / (3 if m.startswith("horn") else 2)
        c = gradient(BISON_HORN, t)
        if side(q) and seg == 1 and q.j % 2 == 0:
            c = shade(c, 0.88)  # growth ridges near the base
        return lit(c, q, -5, 24)
    if m in ("tail", "tuft"):
        c = P["dark"] if m == "tail" else P["beard"]
        return lit(locks(c, q, 61, 0.12, 2), q, -5, 24)
    if m in ("upper", "hupper", "chap"):
        if m == "chap" and ragged(q, 2, 62):
            return None
        if m == "chap":
            return lit(tufts(lerp(P["cape"], P["beard"], 0.35 + 0.3 * h(q.tx, 0, 63)), q, 64), q, -5, 24)
        c = lerp(P["cape"], P["body"], 0.5) if m != "hupper" else lerp(P["body"], P["dark"], 0.3)
        if m == "chap":
            c = lerp(P["cape"], P["beard"], 0.35 + 0.3 * h(q.tx, 0, 63))
        return lit(locks(c, q, 64, 0.14 if m == "chap" else 0.07, 3), q, -5, 24)
    if m == "lower":
        return lit(short_fur(P["dark"], q, 65), q, -5, 24)
    if m == "hoof":
        return lit(P["hoof"] if not (q.face == "front" and q.i == q.fw // 2) else shade(P["hoof"], 0.5), q, -5, 24)
    return P["body"]


def bison():
    M = Model("Steppe bison", None, 1, bison_colour)
    M.part("body", None, (0, 8, 0))
    M.box("body", "cape", -7, -7, -13, 14, 15, 12)
    M.box("body", "cape_edge", -7.5, -6, -2.5, 15, 13, 2)  # the cape's ragged back edge, hanging over the barrel
    M.box("body", "cape_layer", -8, 1, -14, 16, 8, 12)  # a lower layer of longer wool, ragged, over chest and forelegs
    M.box("body", "brisket", -5, 8, -13, 10, 3, 8)
    M.box("body", "barrel", -6, -4, -2, 12, 11, 9)
    M.box("body", "back", -5, -5, -1, 10, 1, 11)
    M.box("body", "rump", -5.5, -4, 7, 11, 10, 5)
    M.part("hump", "body", (0, -7, -7))
    M.box("hump", "hump1", -5.5, -4, -5, 11, 4, 10)
    M.box("hump", "hump2", -4, -6, -3.5, 8, 2, 6)
    M.box("hump", "hump_tuft", -2.5, -7, -2.5, 5, 1, 4)
    M.box("hump", "hump_layer", -6.5, -3, -6, 13, 4, 12)  # the hump's own fringe overlapping the shoulders
    M.part("tail", "body", (0, -3, 12), (0.1, 0, 0))
    M.box("tail", "tail", -0.5, 0, -0.5, 1, 7, 1)
    M.box("tail", "tuft", -1, 6, -1, 2, 4, 2)
    M.part("head", "body", (0, 3, -13), (0.5, 0, 0))
    M.box("head", "skull", -4, -4, -6, 8, 8, 6)
    M.box("head", "wool", -4.5, -6, -5.5, 9, 4, 6)
    M.box("head", "cheek", -4.5, -2, -4.5, 9, 6, 4)
    M.box("head", "muzzle", -2.5, -1, -9, 5, 5, 3)
    M.box("head", "nose", -2, -0.5, -9.5, 4, 3, 1)
    M.part("beard", "head", (0, 4, -5.5))
    M.box("beard", "beard", -2, 0, -1.5, 4, 5, 3)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_ear", "head", (sign * 5, -0.5, -2), (0, 0, 0.4 * sign))
        M.limb(f"{s}_ear", sign, (-1.5, -0.5, -1, 3, 1, 2), mat="ear")
    M.part("bull_horns", "head", (0, 0, 0))
    M.part("cow_horns", "head", (0, 0, 0))
    for s, sign in (("right", -1), ("left", 1)):
        p = s + "_"
        # out sideways from the poll, then up, the tip turning forward and in
        M.part("b" + p + "horn", "bull_horns", (4 * sign, -4, -3), (0, 0.45 * sign, 1.35 * sign))
        label(M, "b" + p + "horn", p + "horn")
        M.limb("b" + p + "horn", sign, (-1, -5, -1, 2, 5, 2), mat="horn1")
        M.part("b" + p + "horn_mid", "b" + p + "horn", (0, -4.5, 0), (0, -0.5 * sign, -1.0 * sign))
        label(M, "b" + p + "horn_mid", p + "horn_mid")
        M.limb("b" + p + "horn_mid", sign, (-1, -4, -1, 2, 4, 2), mat="horn2")
        M.part("b" + p + "horn_tip", "b" + p + "horn_mid", (0, -3.5, 0), (0, 0, -0.6 * sign))
        label(M, "b" + p + "horn_tip", p + "horn_tip")
        M.limb("b" + p + "horn_tip", sign, (-0.5, -4, -0.5, 1, 4, 1), mat="horn3")
        M.part("c" + p + "horn", "cow_horns", (3.8 * sign, -4, -3), (0, 0, 1.2 * sign))
        label(M, "c" + p + "horn", p + "horn")
        M.limb("c" + p + "horn", sign, (-0.5, -3, -0.5, 1, 3, 1), mat="cowhorn1")
        M.part("c" + p + "horn_tip", "c" + p + "horn", (0, -2.5, 0), (0.25, 0, -1.0 * sign))
        label(M, "c" + p + "horn_tip", p + "horn_tip")
        M.limb("c" + p + "horn_tip", sign, (-0.5, -3, -0.5, 1, 3, 1), mat="cowhorn2")
    legs4(M, (3.5, 14, -7), (3, 14, 7.5),
          [(-2.5, -1, -2.5, 5, 6, 5), (-3, 1, -3, 6, 5, 6)], [(-2, -1, -2.5, 4, 6, 5)], ["upper", "chap"], ["hupper"],
          ((5, 0), [(-1.5, 0, -1.5, 3, 4, 3), (-1.5, 4, -2, 3, 1, 4)], ["lower", "hoof"]),
          ((5, 0), [(-1.5, 0, -1.5, 3, 4, 3), (-1.5, 4, -2, 3, 1, 4)], ["lower", "hoof"]))
    return M


M_BISON = None


# ======================================================================= musk ox

MU = {
    "adult": dict(guard=(36, 26, 20), guard_lt=(64, 46, 32), tip=(96, 70, 48), saddle=(150, 128, 98), face=(46, 34, 26),
                  nose=(40, 34, 30), stocking=(196, 184, 160), hoof=(40, 36, 32)),
    "calf": dict(guard=(70, 52, 38), guard_lt=(98, 74, 52), tip=(130, 98, 68), saddle=(164, 140, 108), face=(80, 60, 44),
                 nose=(60, 50, 44), stocking=(204, 192, 168), hoof=(60, 54, 48)),
}
MUSK_HORN = [(222, 210, 180), (196, 180, 148), (120, 104, 84), (40, 34, 30)]


def musk_colour(m, q):
    P = MU[M_MUSK.variant]
    Y = q.Y
    if m in ("core", "hump", "hump_top", "saddle"):
        saddle = q.face == "top" and abs(q.X) < 4.5 and -2 < q.Z < 8
        if m == "saddle" or saddle:
            c = lerp(P["saddle"], P["guard_lt"], 0.35 * h(q.tx, q.ty // 2, 71) + rel(abs(q.X), 2.5, 5) * 0.6)
            return lit(locks(c, q, 72, 0.1, 2), q, 1, 24)
        c = lerp(P["guard"], P["guard_lt"], 0.3 + 0.4 * h(q.tx, q.ty // 3, 73))
        return lit(locks(c, q, 74, 0.14, 4), q, 1, 24)
    if m == "skirt":
        if ragged(q, 3, 75):
            return None
        t = q.j / max(1, q.fh - 1) if side(q) else 0.3
        c = lerp(P["guard"], P["guard_lt"], 0.2 + 0.5 * h(q.tx, 0, 76))
        c = lerp(c, P["tip"], rel(t, 0.6, 1.0) * 0.5)
        return lit(locks(c, q, 77, 0.16, 7), q, 1, 24)
    if m in ("skull", "face", "nose", "forelock"):
        if m == "skull" and eye_at(q, 0, -4.5, size=0.5):
            return EYE
        if m == "nose" or (m == "face" and q.face == "front" and q.y > 0):
            return lit(P["nose"] if m == "nose" else lerp(P["face"], P["stocking"], 0.5), q, 1, 24)
        if m == "face" and q.face == "front":
            return lit(lerp(P["face"], P["stocking"], 0.25), q, 1, 24)
        c = P["face"] if m != "forelock" else lerp(P["guard"], P["saddle"], 0.3 * h(q.tx, q.ty, 78))
        return lit(locks(c, q, 79, 0.12, 2), q, 1, 24)
    if m == "chin":
        if ragged(q, 2, 80):
            return None
        return lit(locks(P["guard"], q, 81, 0.15, 4), q, 1, 24)
    if m in ("boss", "horn1", "horn2", "horn3"):
        t = {"boss": 0.1, "horn1": 0.4, "horn2": 0.7, "horn3": 0.95}[m]
        c = gradient(MUSK_HORN, t + 0.1 * h(q.tx, q.ty, 82))
        if m == "boss" and q.face == "top" and (q.i + q.j) % 3 == 0:
            c = shade(c, 0.88)  # the rough boss
        return lit(c, q, 1, 24)
    if m in ("upper", "lower"):
        c = lerp(P["guard"], P["stocking"], rel(Y, 18, 21)) if m == "upper" else P["stocking"]
        return lit(short_fur(c, q, 83), q, 1, 24)
    if m == "hoof":
        return lit(P["hoof"], q, 1, 24)
    return P["guard"]


def musk_ox():
    M = Model("Musk ox", None, 1, musk_colour)
    M.part("body", None, (0, 10, 0))
    M.box("body", "core", -6, -6, -10, 12, 11, 20)
    M.box("body", "hump", -5, -8, -10, 10, 2, 9)
    M.box("body", "hump_top", -3.5, -9, -8.5, 7, 1, 6)
    M.box("body", "saddle", -4.5, -7, -1, 9, 1, 10)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_skirt", "body", (sign * 6, -2, 0))
        M.limb(f"{s}_skirt", sign, (-1, 0, -10.5, 1, 13, 21), mat="skirt")
    M.part("rear_skirt", "body", (0, -2, 10))
    M.box("rear_skirt", "skirt", -6.5, 0, -0.5, 13, 13, 1)
    M.part("front_skirt", "body", (0, 1, -10))
    M.box("front_skirt", "skirt", -5.5, 0, -1, 11, 10, 1)
    M.part("head", "body", (0, -1, -10), (0.6, 0, 0))
    M.box("head", "skull", -3, -3, -6, 6, 6, 6)
    M.box("head", "forelock", -3.5, -3.5, -3, 7, 2, 4)
    M.box("head", "face", -2.5, -1, -9, 5, 5, 3)
    M.box("head", "nose", -2, 0.5, -9.5, 4, 3, 1)
    M.part("chin", "head", (0, 2, -6))
    M.box("chin", "chin", -2.5, 0, -2, 5, 5, 4)
    M.part("horns", "head", (0, 0, 0))
    M.box("horns", "boss", -4.5, -4.5, -4.5, 9, 2, 4)
    for s, sign in (("right", -1), ("left", 1)):
        p = s + "_"
        # down beside the face, then forward, the tip hooking up and out
        M.part(p + "horn", "horns", (4 * sign, -3.5, -2.5), (-0.15, 0, -0.35 * sign))
        M.limb(p + "horn", sign, (-1, 0, -1, 2, 4, 2), mat="horn1")
        M.part(p + "horn_mid", p + "horn", (0, 3.5, 0), (-1.3, 0, -0.1 * sign))
        M.limb(p + "horn_mid", sign, (-1, 0, -1, 2, 3, 2), mat="horn2")
        M.part(p + "horn_tip", p + "horn_mid", (0, 2.5, 0), (-1.0, 0, 0.5 * sign))
        M.limb(p + "horn_tip", sign, (-0.5, 0, -0.5, 1, 3, 1), mat="horn3")
    legs4(M, (3.5, 17, -6), (3.5, 17, 6),
          [(-1.5, -1, -1.5, 3, 4, 3)], [(-1.5, -1, -1.5, 3, 4, 3)], ["upper"], ["upper"],
          ((3, 0), [(-1.5, 0, -1.5, 3, 3, 3), (-1.5, 3, -2, 3, 1, 3)], ["lower", "hoof"]),
          ((3, 0), [(-1.5, 0, -1.5, 3, 3, 3), (-1.5, 3, -2, 3, 1, 3)], ["lower", "hoof"]))
    return M


M_MUSK = None


# ======================================================================= grey wolf (PredatorModel)

WO = dict(saddle=(56, 54, 52), grizzle=(124, 118, 108), tawny=(150, 122, 88), buff=(184, 160, 122), cream=(226, 216, 196),
          dark=(30, 28, 27), nose=(22, 20, 20), amber=(198, 142, 42), pad=(40, 34, 32), mouth=(70, 30, 34), tooth=(230, 224, 206))


def agouti(c, q, salt):
    """Banded guard hairs: dark and pale ticking over the base colour."""
    r = h(q.tx, q.ty, salt)
    if r > 0.93:
        return lerp(c, WO["dark"], 0.3)
    if r < 0.06:
        return lerp(c, WO["cream"], 0.25)
    return c


def wolf_coat(q):
    """Grizzled saddle over the back and down the shoulders, tawny flanks, cream belly."""
    if q.face == "bottom" or q.n[1] > 0.7:
        return WO["cream"]
    up = -q.n[1]
    t = rel(q.Y, 8, 16)
    c = gradient([WO["saddle"], WO["grizzle"], WO["tawny"], WO["buff"], WO["cream"]], t * 0.85 + (0.0 if up > 0.5 else 0.15))
    if up > 0.5:
        c = lerp(WO["saddle"], WO["grizzle"], 0.45 * h(q.tx, q.ty // 2, 91))
    return c


def wolf_colour(m, q):
    if m in ("chest", "brisket", "mane", "waist", "hips", "neck"):
        c = wolf_coat(q)
        if m in ("chest", "brisket", "neck") and q.face == "front" or (m == "brisket" and q.face != "top"):
            # a pale stripe down the throat and chest, buff to either side
            c = lerp(WO["cream"], WO["buff"], 0.3 * h(q.tx, q.ty, 92)) if abs(q.X) < 1.2 else lerp(WO["buff"], WO["grizzle"], 0.35)
        if m == "mane" and side(q) and q.face != "back":
            c = lerp(c, WO["saddle"], 0.25)  # the dark cape over the shoulders
        return lit(agouti(locks(c, q, 93, 0.09, 2), q, 94), q, 6, 24)
    if m == "skull":
        # the mask: grizzled dark over the brow, amber eyes, pale under them running into the cheeks
        if q.face == "front":
            ax = abs(q.x)
            if q.y < -2:
                return lit(agouti(lerp(WO["saddle"], WO["grizzle"], 0.35), q, 95), q, 6, 24)
            if q.y < -1:
                return WO["amber"] if ax > 1.5 else EYE
            return lit(WO["cream"] if ax > 1.5 and -1 < q.y < 0 else lerp(WO["buff"], WO["grizzle"], 0.35), q, 6, 24)
        if q.face in ("left", "right") and q.z < -2.5:
            if -2 < q.y < -1:
                return WO["amber"]
            if q.y < -2:
                return lit(lerp(WO["saddle"], WO["grizzle"], 0.4), q, 6, 24)
        if q.face == "top":
            return lit(agouti(lerp(WO["grizzle"], WO["saddle"], 0.35), q, 95), q, 6, 24)
        return lit(agouti(lerp(WO["tawny"], WO["grizzle"], 0.4), q, 96), q, 6, 24)
    if m == "cheeks":
        c = lerp(WO["buff"], WO["grizzle"], 0.35)  # the ruff, grizzled
        if q.y > 1:
            c = WO["cream"]  # pale under the jaw
        elif q.face == "front" and abs(q.x) > 2:
            c = lerp(WO["cream"], WO["buff"], 0.4)
        return lit(agouti(locks(c, q, 98, 0.08, 2), q, 97), q, 6, 24)
    if m in ("snout", "muzzle"):
        if q.face == "top":
            return lit(lerp(WO["tawny"], WO["saddle"], 0.5 if m == "snout" else 0.35), q, 6, 24)  # the dark bridge
        if q.face == "bottom" or q.y > 0:
            return lit(WO["cream"], q, 6, 24)  # pale lips and chin
        return lit(lerp(WO["buff"], WO["tawny"], 0.45), q, 6, 24)
    if m == "nose":
        if q.face == "top":
            return lit(lerp(WO["tawny"], WO["saddle"], 0.55), q, 6, 24)  # the bridge runs on to the tip; the black shows in front
        return WO["nose"]
    if m == "jaw":
        if q.face == "top":
            return WO["mouth"] if abs(q.x) < 1 else WO["tooth"]
        return lit(WO["cream"], q, 6, 24)
    if m.startswith("ear_"):
        # grey-brown, the outer edges and the tip dark, pale fur inside the base
        rim_c = lerp(WO["saddle"], WO["grizzle"], 0.25)
        if m == "ear_tip":
            return lit(rim_c, q, 6, 24)
        if q.face == "front":
            edge = q.i in (0, q.fw - 1)
            if m == "ear_base":
                return rim_c if edge else lerp(WO["cream"], WO["buff"], 0.5)
            return lerp(WO["buff"], WO["tawny"], 0.5) if not edge or q.fw < 2 else lerp(rim_c, WO["tawny"], 0.4)
        return lit(lerp(WO["grizzle"], WO["tawny"], 0.35 if m == "ear_base" else 0.1), q, 6, 24)
    if m == "ruff":
        if ragged(q, 1.5, 102):
            return None
        c = lerp(WO["cream"], WO["buff"], 0.3 * h(q.tx, 0, 103)) if q.face == "front" and abs(q.x) < 1.5 else lerp(WO["buff"], WO["grizzle"], 0.45)
        return lit(agouti(locks(c, q, 104, 0.12, 3), q, 105), q, 6, 24)
    if m.startswith("tail"):
        tip = m == "tail3"
        c = WO["dark"] if tip and q.j >= 1 else (lerp(WO["saddle"], WO["grizzle"], 0.4) if q.n[1] < 0.3 and q.face != "bottom"
                                                 else lerp(WO["buff"], WO["cream"], 0.5))
        if m == "tail2" and q.face in ("back", "top") and q.j < 2:
            c = WO["dark"]  # the dark violet-gland spot on top of the tail
        return lit(agouti(locks(c, q, 99, 0.12, 2), q, 100), q, 6, 24)
    if m in ("upper", "thigh", "lower"):
        c = lerp(WO["tawny"], WO["buff"], 0.5 + 0.4 * rel(q.Y, 14, 23))
        if m == "thigh" and q.face in ("left", "right", "back"):
            c = lerp(c, WO["grizzle"], 0.35 * (1 - rel(q.Y, 11, 16)))
        if m == "upper" and q.face == "front":
            c = lerp(c, WO["saddle"], 0.55)  # the dark stripe down the front of the foreleg
        if (q.face == "left" and q.X < 0) or (q.face == "right" and q.X > 0):
            c = lerp(c, WO["cream"], 0.45)  # pale inner legs
        return lit(short_fur(c, q, 101), q, 6, 24)
    if m == "paw":
        if q.face == "bottom":
            return WO["pad"]
        return lit(WO["buff"] if not (q.face == "front" and q.i % 2) else lerp(WO["buff"], WO["dark"], 0.5), q, 6, 24)
    return WO["tawny"]


def grey_wolf():
    M = Model("Grey wolf", None, 1, wolf_colour)
    M.part("body", None, (0, 12, 0))
    M.box("body", "chest", -3, -3.5, -8, 6, 7, 7)
    M.box("body", "brisket", -2.5, 3.5, -8, 5, 1, 5)
    M.box("body", "mane", -3.5, -4.5, -8.5, 7, 5, 5)
    M.box("body", "waist", -2.5, -3, -1, 5, 5, 6)
    M.box("body", "hips", -2.5, -3.5, 5, 5, 6, 4)
    M.part("neck", "body", (0, -1.5, -7.5), (-0.55, 0, 0))
    M.box("neck", "neck", -2.5, -3, -3, 5, 6, 3)
    M.box("neck", "ruff", -3.5, -2, -3.5, 7, 7, 4)
    M.part("head", "body", (0, -3, -9.5))
    M.box("head", "skull", -2.5, -3, -3.5, 5, 4, 4)
    M.box("head", "cheeks", -3, -1, -2, 6, 3, 2)
    M.box("head", "snout", -1.5, -1.5, -5.5, 3, 3, 2)  # the muzzle tapers: skull 5, snout 3, muzzle 2 wide
    M.box("head", "muzzle", -1, -1, -8.5, 2, 2, 3)
    M.box("head", "nose", -1, -1.75, -8.5, 2, 1, 1)  # flush with the muzzle tip, sitting on top of it
    M.part("jaw", "head", (0, 1, -5.5))
    M.box("jaw", "jaw", -1, 0, -3, 2, 1, 3)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_ear", "head", (sign * 1.6, -2.8, -1.5), (0.15, 0, 0.25 * sign))
        M.limb(f"{s}_ear", sign, (-1.5, -1, -0.5, 3, 1, 1), (-1, -2, -0.5, 2, 1, 1), (-0.5, -3, -0.5, 1, 1, 1),
               mat=["ear_base", "ear_mid", "ear_tip"])
    M.part("tail", "body", (0, -2.5, 8.5), (0.75, 0, 0))
    M.box("tail", "tail1", -1, 0, -1, 2, 3, 2)
    M.part("tail_mid", "tail", (0, 2.5, 0), (0.12, 0, 0))
    M.box("tail_mid", "tail2", -1.5, 0, -1.5, 3, 4, 3)
    M.part("tail_tip", "tail_mid", (0, 3.5, 0), (0.1, 0, 0))
    M.box("tail_tip", "tail3", -1, 0, -1, 2, 3, 2)
    legs4(M, (1.6, 14, -5), (1.6, 13, 6.5),
          [(-1, -1, -1.5, 2, 6, 3)], [(-1.5, -2, -2, 3, 7, 4)], ["upper"], ["thigh"],
          ((5, 0), [(-1, 0, -1, 2, 4, 2), (-1, 4, -2, 2, 1, 3)], ["lower", "paw"]),
          ((5, 0.5), [(-1, 0, -1, 2, 5, 2), (-1, 5, -2, 2, 1, 3)], ["lower", "paw"]))
    return M


# ======================================================================= bears

BE = dict(root=(34, 22, 16), dark=(54, 35, 23), base=(82, 54, 34), warm=(110, 74, 45), tip=(156, 120, 82),
          muzzle=(140, 108, 76), nose=(26, 20, 18), pad=(38, 30, 26), claw=(196, 186, 162), eye=EYE, ear_in=(80, 56, 40))
CUB = dict(BE, dark=(70, 46, 30), base=(108, 72, 44), warm=(136, 96, 60), tip=(170, 132, 90), muzzle=(150, 116, 84))


def bear_coat(P, q, salt, top=0, bottom=24):
    """Dense brown fur: darker on the legs and belly, warmer on the back, grizzled pale tips over the hump and shoulders."""
    t = rel(q.Y, top, bottom)
    c = gradient([P["warm"], P["base"], P["dark"], P["root"]], t)
    if q.face == "top":
        c = lerp(P["warm"], P["base"], 0.4 * h(q.tx, q.ty // 2, salt))
    if q.face == "bottom":
        c = P["dark"]
    # pale tips: clumped, over the shoulders and back, not spread as single specks
    grizzle = (1 - t) * (0.8 if q.Z < 0 else 0.4)
    if h(q.tx, q.ty, salt + 1) < 0.14 * grizzle:
        c = lerp(c, P["tip"], 0.28)
    return locks(c, q, salt + 2, 0.08, 2)


def bear_colour(m, q, P=BE, top=-1, bottom=24, cub=False):
    if m in ("chest", "hump", "hump2", "rump", "core", "shoulder", "back", "belly", "tail"):
        return lit(bear_coat(P, q, 111, top, bottom), q, top, bottom)
    if m == "fringe":
        if ragged(q, 2, 112):
            return None
        return lit(locks(lerp(P["dark"], P["base"], 0.3 * h(q.tx, 0, 113)), q, 114, 0.12, 3), q, top, bottom)
    if m == "ruff" or m == "cheeks":
        c = lerp(P["base"], P["warm"], 0.5 * h(q.tx, q.ty // 2, 115))
        if h(q.tx, q.ty, 116) < 0.15:
            c = lerp(c, P["tip"], 0.4)
        return lit(locks(c, q, 117, 0.1, 2), q, top, bottom)
    if m in ("skull", "brow", "crown"):
        if q.face == "front" and m == ("skull" if cub else "brow"):
            # small dark eyes set wide on the dished face, a paler brow over them
            ex, ey = (1.5, -1.5) if cub else (2.5, -1.5)
            if abs(abs(q.x) - ex) < 0.1 and abs(q.y - ey) < 0.1:
                return P["eye"]
            if abs(abs(q.x) - ex) < 1.1 and abs(q.y - ey + 1) < 0.1:
                return lit(lerp(P["base"], P["tip"], 0.3), q, top, bottom)
        return lit(bear_coat(P, q, 118, top, bottom) if q.face != "front" else locks(lerp(P["base"], P["warm"], 0.3), q, 119, 0.06, 2),
                   q, top, bottom)
    if m == "muzzle":
        c = P["muzzle"] if q.face != "top" else lerp(P["muzzle"], P["base"], 0.35)
        if q.face in ("left", "right", "front") and q.j == q.fh - 1:
            c = lerp(c, P["root"], 0.5)  # the mouth line
        return lit(short_fur(c, q, 120), q, top, bottom)
    if m == "jaw":
        return lit(short_fur(lerp(P["muzzle"], P["base"], 0.3), q, 121), q, top, bottom)
    if m == "nose":
        return P["nose"] if not (q.face == "front" and q.j == 0) else lerp(P["nose"], (90, 80, 76), 0.4)
    if m == "ear":
        if side(q) and q.fw >= 3 and q.j == 0 and (q.i == 0 or q.i == q.fw - 1):
            return None  # round
        if q.face == "front":
            return lerp(P["ear_in"], P["dark"], 0.3)
        return lit(lerp(P["dark"], P["base"], 0.4), q, top, bottom)
    if m in ("front_leg", "hind_leg", "leg"):
        c = gradient([P["base"], P["dark"], P["root"]], rel(q.Y, top + 12, bottom))
        return lit(locks(c, q, 122, 0.08, 2), q, top, bottom)
    if m in ("front_paw", "hind_paw", "paw"):
        if q.face == "bottom":
            return P["pad"]
        c = P["root"]
        if cub and q.face == "front" and q.j == q.fh - 1 and q.i % 2 == 0:
            c = P["claw"]
        return lit(short_fur(c, q, 123), q, top, bottom)
    if m == "claws":
        return P["claw"] if q.face != "bottom" else lerp(P["claw"], P["root"], 0.4)
    return P["base"]


def bear_cub():
    M = Model("Bear cub", None, 1, lambda m, q: bear_colour(m, q, CUB, 13, 24, True))
    M.part("body", None, (0, 18, 0))
    M.box("body", "core", -3.5, -3, -4.5, 7, 6, 9)
    M.box("body", "shoulder", -3, -2.5, -5.5, 6, 5, 1)
    M.box("body", "back", -3, -4, -3.5, 6, 1, 7)
    M.box("body", "belly", -3, 3, -3.5, 6, 1, 7)
    M.box("body", "rump", -3, -2.5, 4.5, 6, 5, 1)
    M.part("tail", "body", (0, -1.5, 5.5), (0.4, 0, 0))
    M.box("tail", "tail", -1, -1, 0, 2, 2, 1)
    M.part("head", None, (0, 16, -5))
    M.box("head", "skull", -3, -3, -4, 6, 5, 4)
    M.box("head", "crown", -2, -4, -3.5, 4, 1, 3)
    M.box("head", "cheeks", -3.5, -1, -3, 7, 3, 3)
    M.box("head", "muzzle", -1.5, -0.5, -6, 3, 2, 2)
    M.box("head", "nose", -1, -0.6, -6.5, 2, 1, 1)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_ear", "head", (sign * 2.4, -2.6, -1.5), (0, 0, 0.3 * sign))
        M.limb(f"{s}_ear", sign, (-1, -2, -0.5, 2, 2, 1), mat="ear")
    legs4(M, (2, 20, -3), (2, 20, 3.5), [(-1.5, 0, -1.5, 3, 3, 3), (-1.5, 3, -2, 3, 1, 4)],
          [(-1.5, 0, -1.5, 3, 3, 3), (-1.5, 3, -2, 3, 1, 4)], ["leg", "paw"], ["leg", "paw"])
    return M


def brown_bear_skin():
    """BrownBearModel's own boxes (hand-written Java, shared by the polar and cave bears), painted on its UVs."""
    M = Model("Brown bear", (128, 64), 1, lambda m, q: bear_colour(m, q, BE, -1, 24))
    uv = {}

    def box(part, mat, u, v, *b, key=None, mirror=False):
        key = key or f"{part}.{mat}"
        if key not in uv:
            uv[key] = (u, v)
        M.box(part, mat, *b, mirror=mirror, key=key)

    M.part("body", None, (0, 12, 8))
    box("body", "chest", 0, 0, -6, -9, -20, 12, 13, 12)
    box("body", "hump", 48, 0, -4.5, -11, -18, 9, 3, 7)
    box("body", "hump2", 48, 10, -3, -12, -16.5, 6, 1, 4)
    box("body", "rump", 0, 25, -5.5, -7, -9, 11, 11, 13)
    box("body", "fringe", 48, 25, -6.2, 3, -19, 0, 4, 10)
    box("body", "fringe", 48, 25, 6.2, 3, -19, 0, 4, 10, key="body.fringe", mirror=True)
    M.part("tail", "body", (0, -5, 3.5), (0.5, 0, 0))
    box("tail", "tail", 114, 20, -1.5, -1, 0, 3, 3, 2)
    M.part("head", "body", (0, -5, -20), (0.12, 0, 0))
    box("head", "skull", 80, 0, -4, -4, -6, 8, 7, 6)
    box("head", "ruff", 48, 15, -5, -2, -2.5, 10, 6, 3)
    box("head", "brow", 108, 0, -3, -3, -7, 6, 2, 1)
    box("head", "muzzle", 80, 13, -2.5, -1, -10, 5, 4, 4)
    box("head", "nose", 114, 13, -1.5, -1.5, -11, 3, 2, 1)
    M.part("jaw", "head", (0, 2.5, -6))
    box("jaw", "jaw", 98, 13, -2, 0, -3.5, 4, 2, 4)
    M.part("ear", "head", (-3, -3.5, -2), (0, 0, -0.25))
    box("ear", "ear", 114, 16, -1.5, -2.5, -0.5, 3, 3, 1)
    for s, x, z in (("rf", -3.5, -7.5), ("rh", -3.5, 7.0)):
        M.part(s, None, (x, 14 if s == "rf" else 14, z))
    box("rf", "front_leg", 0, 49, -2.5, -1, -2.5, 5, 8, 5)
    box("rf", "front_paw", 20, 49, -2.5, 7, -3.5, 5, 3, 6)
    box("rf", "claws", 68, 59, -2, 9, -4.5, 4, 1, 1)
    box("rh", "hind_leg", 42, 49, -3, -2, -3.5, 6, 8, 7)
    box("rh", "hind_paw", 68, 49, -2.5, 6, -3.5, 5, 4, 6)
    return M, uv


# ======================================================================= cave lion (PredatorModel)

LI = dict(back=(132, 100, 62), coat=(184, 150, 102), pale=(214, 190, 146), belly=(232, 218, 188), stripe=(108, 78, 48),
          white=(236, 228, 210), dark=(50, 38, 30), nose=(150, 96, 86), eye=(210, 152, 48), ear_back=(44, 34, 28),
          pad=(64, 50, 42), lip=(72, 54, 44), tuft=(46, 34, 26))


def lion_coat(q):
    """Tawny, darker along the spine, a pale belly; faint ghost stripes over the hindquarters and thighs (as cave art)."""
    if q.face == "bottom" or q.n[1] > 0.7:
        return LI["belly"]
    t = rel(q.Y, 4, 15)
    c = gradient([LI["back"], LI["coat"], LI["coat"], LI["pale"]], t)
    if -q.n[1] > 0.7:
        c = lerp(LI["back"], LI["coat"], 0.3 * h(q.tx, q.ty // 2, 131))
    if q.Z > 1 and abs(q.n[0]) > 0.5 and t < 0.85:
        if math.sin(q.Z * 1.15 + 0.7 * math.sin(q.Y * 0.6)) > 0.55:
            c = lerp(c, LI["stripe"], 0.7)
    return c


def lion_colour(m, q):
    if m in ("shoulders", "withers", "brisket", "waist", "pouch", "hips", "rump", "neck"):
        c = lion_coat(q)
        if m in ("brisket", "pouch"):
            c = lerp(LI["pale"], LI["belly"], 0.5)
        if m == "neck" and (q.face == "bottom" or (q.face == "front" and q.y > 0)):
            c = LI["pale"]  # pale throat
        return lit(short_fur(c, q, 132, 0.07), q, 3, 24)
    if m == "skull":
        if q.face == "front":
            ax = abs(q.x)
            if -2 < q.y < -1 and 2 < ax < 3:
                return LI["eye"]
            if -2 < q.y < -1 and 3 < ax:
                return LI["dark"]  # the dark line running back from the eye
            if -3 < q.y < -2 and 1.5 < ax:
                return lit(lerp(LI["back"], LI["dark"], 0.3), q, 3, 24)  # heavy brow
            if -1 < q.y < 0 and 2 < ax < 4:
                return LI["white"]  # pale patch under the eye
            return lit(lerp(LI["coat"], LI["back"], 0.25), q, 3, 24)
        if q.face == "top":
            return lit(short_fur(lerp(LI["back"], LI["coat"], 0.4), q, 133), q, 3, 24)
        return lit(short_fur(LI["coat"], q, 134), q, 3, 24)
    if m == "bridge":
        return lit(lerp(LI["coat"], LI["back"], 0.3) if q.face == "top" else LI["pale"], q, 3, 24)
    if m == "jowls":
        c = lerp(LI["coat"], LI["pale"], rel(q.y, -1, 3))  # cheeks pale only towards the throat
        if q.y > 2:
            c = LI["white"]
        return lit(short_fur(c, q, 135), q, 3, 24)
    if m == "muzzle":
        if q.face == "top":
            return lit(lerp(LI["coat"], LI["back"], 0.2), q, 3, 24)
        if q.face == "front" and abs(q.x) < 0.6 and 0 < q.y < 1:
            return LI["lip"]  # the short dark line below the nose
        if q.y < 0:
            return lit(lerp(LI["coat"], LI["pale"], 0.5), q, 3, 24)  # tawny above
        if q.face in ("left", "right") and q.y < 0.5:
            dot = q.z < -5.5 and round(q.z * 2) % 3 == 0
            return lit(lerp(LI["white"], LI["dark"], 0.35) if dot else LI["white"], q, 3, 24)  # whisker rows
        if q.y > 0.5 and q.face in ("left", "right"):
            return LI["lip"] if q.z < -6.5 else lit(LI["white"], q, 3, 24)  # the dark lip line at the corner
        return lit(LI["white"], q, 3, 24)
    if m == "nose":
        return LI["nose"] if q.face != "bottom" else LI["dark"]
    if m == "jaw":
        if q.face == "top":
            return (120, 52, 52) if abs(q.x) < 1.5 else LI["white"]
        return lit(short_fur(LI["white"], q, 137), q, 3, 24)
    if m == "ear":
        if q.face == "back":
            return LI["ear_back"] if q.j < q.fh - 1 else LI["coat"]
        return lit(lerp(LI["pale"], LI["coat"], 0.4) if q.face == "front" else LI["coat"], q, 3, 24)
    if m == "tail":
        return lit(short_fur(lion_coat(q) if q.n[1] < 0.5 else LI["pale"], q, 138), q, 3, 24)
    if m == "tuft":
        return lit(locks(LI["tuft"], q, 139, 0.15, 2), q, 3, 24)
    if m in ("upper", "thigh", "lower"):
        inner = (q.face == "left" and q.X < 0) or (q.face == "right" and q.X > 0) or q.face == "back"
        c = lion_coat(q) if m == "thigh" and not inner else LI["coat"]
        c = lerp(c, LI["pale"], 0.55 if inner else 0.35 * rel(q.Y, 14, 23))
        return lit(short_fur(c, q, 140), q, 3, 24)
    if m == "paw":
        if q.face == "bottom":
            return LI["pad"]
        if q.face == "front" and q.i % 2 == 1:
            return lit(lerp(LI["pale"], LI["back"], 0.45), q, 3, 24)  # the toes
        return lit(LI["pale"], q, 3, 24)
    return LI["coat"]


def cave_lion():
    M = Model("Cave lion", None, 1, lion_colour)
    M.part("body", None, (0, 10, 0))
    M.box("body", "shoulders", -5, -6, -11, 10, 10, 10)    # heavy forequarters
    M.box("body", "withers", -3.5, -7, -10, 7, 1, 7)
    M.box("body", "brisket", -4, 4, -10.5, 8, 1, 6)
    M.box("body", "waist", -4, -5, -1, 8, 8, 8)
    M.box("body", "pouch", -3.5, 3, 0, 7, 1, 7)            # the loose belly skin
    M.box("body", "hips", -4.5, -5.5, 7, 9, 9, 6)
    M.box("body", "rump", -3.5, -6.5, 7.5, 7, 1, 5)
    M.part("neck", "body", (0, -3, -10.5), (-0.2, 0, 0))
    M.box("neck", "neck", -3.5, -3.5, -4, 7, 7, 4)
    M.part("head", "body", (0, -3.5, -13.5))
    M.box("head", "skull", -4, -4, -5, 8, 6, 5)
    M.box("head", "jowls", -4.5, -1, -4, 9, 4, 4)          # broad cheeks
    M.box("head", "bridge", -1.5, -2, -7, 3, 1, 2)
    M.box("head", "muzzle", -2.5, -1, -8, 5, 3, 3)
    M.box("head", "nose", -1, -1, -8.5, 2, 1, 1)
    M.part("jaw", "head", (0, 2, -5))
    M.box("jaw", "jaw", -2, 0, -3, 4, 1, 3)
    for s, sign in (("right", -1), ("left", 1)):
        M.part(f"{s}_ear", "head", (sign * 3, -4, -2), (0, 0, 0.25 * sign))
        M.limb(f"{s}_ear", sign, (-1.5, -2, -0.5, 3, 2, 1), mat="ear")
    M.part("tail", "body", (0, -4.5, 12.5), (0.45, 0, 0))
    M.box("tail", "tail", -1, 0, -1, 2, 8, 2)
    M.part("tail_end", "tail", (0, 7.5, 0), (0.3, 0, 0))
    M.box("tail_end", "tail", -1, 0, -1, 2, 7, 2)
    M.box("tail_end", "tuft", -1.5, 6, -1.5, 3, 3, 3)
    legs4(M, (2.75, 13, -6.5), (2.75, 12, 9.5),
          [(-2, -2, -2.5, 4, 9, 5)], [(-2, -2, -3, 4, 9, 6)], ["upper"], ["thigh"],
          ((7, 0), [(-1.5, 0, -1.5, 3, 3, 3), (-2, 3, -3, 4, 1, 5)], ["lower", "paw"]),
          ((7, 0.5), [(-1.5, 0, -1.5, 3, 4, 3), (-2, 4, -3, 4, 1, 5)], ["lower", "paw"]))
    return M


# ======================================================================= main

def main():
    global M_MAMMOTH, M_RHINO, M_BISON, M_MUSK
    giants = []
    M_MAMMOTH = mammoth()
    giants.append(build(M_MAMMOTH, ("tusk1", "tusk2", "tusk3", "tusk4", "foot"), {"giants/woolly_mammoth": "adult",
                                                                                 "giants/woolly_mammoth_calf": "calf"}, "mammoth"))
    M_RHINO = rhino()
    giants.append(build(M_RHINO, ("horn1", "horn2", "horn3", "horn_rear", "foot"), {"giants/woolly_rhino": "adult",
                                                                                   "giants/woolly_rhino_calf": "calf"}, "rhino"))
    M_BISON = bison()
    giants.append(build(M_BISON, ("horn1", "horn2", "horn3", "cowhorn1", "cowhorn2", "hoof"),
                        {"giants/steppe_bison_bull": "bull", "giants/steppe_bison_cow": "cow", "giants/steppe_bison_calf": "calf"}, "bison"))
    M_MUSK = musk_ox()
    giants.append(build(M_MUSK, ("boss", "horn1", "horn2", "horn3", "hoof"), {"giants/musk_ox": "adult", "giants/musk_ox_calf": "calf"}, "muskOx"))
    write_java(JAVA / "giants/GiantsGeometry.java", "com.mechanical.client.wildlife.giants", "GiantsGeometry", giants)

    preds = [build(grey_wolf(), ("paw",), {"grey_wolf": "adult"}, "greyWolf"),
             build(bear_cub(), (), {"brown_bear_cub": "adult"}, "bearCub"),
             build(cave_lion(), ("paw", "nose"), {"cave_lion": "adult"}, "caveLion")]
    write_java(JAVA / "PredGeometry.java", "com.mechanical.client.wildlife", "PredGeometry", preds)

    bear, uv = brown_bear_skin()
    img = paint(bear.parts, uv, (128, 64), 1)
    save(img, "wildlife/brown_bear.png")
    save(skinned(img, bear, uv, ("claws",)), "skinned/wildlife/brown_bear.png")


if __name__ == "__main__":
    main()
