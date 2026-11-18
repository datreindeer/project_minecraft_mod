"""Shared helpers for gen_forest_critter_textures.py and gen_giant_deer_texture.py.

A model is a list of parts mirroring the Java createBodyLayer(): part(name, parent, pivot, rot, cubes). Each cube has a
UV key; cubes sharing a key share texels (left/right limbs, the left one mirrored). UVs are packed deterministically
(pack()); java_cubes() prints the matching CubeListBuilder lines so the Java model can be kept in sync.

Painting: every texel is painted from the point it covers, given in MODEL space at the rest pose (pixels; y down,
-z = front, +x = the animal's left), so markings line up across cubes. material(face, X, Y, Z, n, tx, ty) -> rgb(a),
where face is the cube face and n the face normal in model space.
"""
import math
from pathlib import Path

from PIL import Image

from wildlife_paint import face_texels, fur, h, lerp, shade, skinned, smooth  # noqa: F401  (re-exported for painters)

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"


def cube(key, x0, y0, z0, w, hh, d, mat, mirror=False):
    return dict(key=key, box=(x0, y0, z0, w, hh, d), mat=mat, mirror=mirror)


def part(name, parent, pivot, rot, cubes=()):
    return dict(name=name, parent=parent, pivot=pivot, rot=rot, cubes=list(cubes))


def rot_matrix(xr, yr, zr):
    """ModelPart order: rotate Z, then Y, then X (applied to the point: Rz * Ry * Rx * p)."""
    cx, sx, cy, sy, cz, sz = math.cos(xr), math.sin(xr), math.cos(yr), math.sin(yr), math.cos(zr), math.sin(zr)
    Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
    Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
    Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
    return mm(mm(Rz, Ry), Rx)


def mm(A, B):
    return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def mv(A, v):
    return [sum(A[i][k] * v[k] for k in range(3)) for i in range(3)]


def world(parts, name, pose=None):
    """(R, t): part-local -> model space. pose: {part: (dpivot, rot_override_or_delta)} applied on top of rest."""
    by = {p["name"]: p for p in parts}
    p = by[name]
    piv, rot = list(p["pivot"]), list(p["rot"])
    if pose and name in pose:
        dp, dr = pose[name]
        piv = [a + b for a, b in zip(piv, dp)]
        rot = [a + b for a, b in zip(rot, dr)]
    R, t = rot_matrix(*rot), piv
    if p["parent"]:
        PR, Pt = world(parts, p["parent"], pose)
        t = [a + b for a, b in zip(mv(PR, piv), Pt)]
        R = mm(PR, R)
    return R, t


NORMALS = {"top": (0, -1, 0), "bottom": (0, 1, 0), "right": (-1, 0, 0), "left": (1, 0, 0), "front": (0, 0, -1), "back": (0, 0, 1)}


def size_of(c):
    _, _, _, w, hh, d = c["box"]
    return 2 * (math.ceil(w) + math.ceil(d)), math.ceil(d) + math.ceil(hh)


def pack(parts, tex_w, tex_h):
    """Shelf-pack every UV key: tallest first. Returns {key: (u, v)}."""
    keys = {}
    for p in parts:
        for c in p["cubes"]:
            keys.setdefault(c["key"], size_of(c))
    order = sorted(keys, key=lambda k: (-keys[k][1], -keys[k][0], k))
    uv, x, y, row = {}, 0, 0, 0
    for k in order:
        w, hh = keys[k]
        if x + w > tex_w:
            x, y, row = 0, y + row, 0
        uv[k] = (x, y)
        x += w
        row = max(row, hh)
    assert y + row <= tex_h, f"UVs overflow the texture ({y + row} > {tex_h})"
    return uv


def paint(parts, uv, size):
    W, H = size
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    done, owner = set(), {}
    for p in parts:
        R, t = world(parts, p["name"])
        for c in p["cubes"]:
            if c["key"] in done:
                continue
            done.add(c["key"])
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            for face, tx, ty, x, y, z in face_texels(u, v, x0, y0, z0, int(w), int(hh), int(d)):
                assert 0 <= tx < W and 0 <= ty < H, (c["key"], tx, ty)
                assert owner.setdefault((tx, ty), c["key"]) == c["key"], f"UV overlap {c['key']} / {owner[(tx, ty)]}"
                X, Y, Z = (a + b for a, b in zip(mv(R, (x, y, z)), t))
                n = mv(R, NORMALS[face])
                col = c["mat"](face, X, Y, Z, n, tx, ty)
                if col is not None:
                    px[tx, ty] = col if len(col) == 4 else (*col, 255)
    return img


def java_cubes(parts, uv):
    """Prints the CubeListBuilder content per part, for checking the Java model against the script."""
    for p in parts:
        bits = []
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            m = ".mirror()" if c["mirror"] else ""
            bits.append(f".texOffs({u}, {v}){m}.addBox({x0}F, {y0}F, {z0}F, {w}F, {hh}F, {d}F)")
        print(f"{p['name']:>22}: pivot {p['pivot']} rot {p['rot']}  " + "".join(bits))


def save(img, rel):
    out = TEX / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print("wrote", out.relative_to(ROOT))
