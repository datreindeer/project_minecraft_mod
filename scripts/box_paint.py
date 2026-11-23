"""Box models whose geometry and skin come from one Python table (the gen_elkwight_model.py pattern), on top of
critter_paint's part tree: part(name, parent, pivot, rot, cubes) / cube(key, x0, y0, z0, w, h, d, mat, mirror).

- pack(parts, w, h) (critter_paint) packs one UV rectangle per cube key; cubes sharing a key share texels.
- paint(parts, uv, size, k) paints every texel by calling mat(q) with q: face, X Y Z (model space at rest, pixels, y down,
  -z front, +x = the creature's left), x y z (the cube's own coordinates), n (world normal), i j (texel within the face,
  i left to right as seen looking at the face, j top to bottom), fw fh (face size in texels), tx ty (texture pixel), k.
  k > 1 paints k x k texels per model pixel: the PNG is k times the declared texture size, so a model drawn k times
  bigger keeps vanilla's texel size. Box sizes must be whole texels (w * k an integer): the UV layout is vanilla's, exact. mat returns rgb, rgba or None (transparent).
- Model collects parts and boxes (box(part, material, ...), limb() for mirrored pairs).
- java_method(...) returns Java createBodyLayer() bodies; write_java(path, package, cls, doc, models) writes the file.
"""
import math
from types import SimpleNamespace

from PIL import Image

from critter_paint import NORMALS, cube, mv, pack, part, world  # noqa: F401  (re-exported)


def face_texels(u, v, x0, y0, z0, w, hh, d, k):
    """Like wildlife_paint.face_texels (vanilla box UV), k texels per pixel: (face, tx, ty, x, y, z, i, j, fw, fh)."""
    s = 1.0 / k
    x1, y1, z1 = x0 + w, y0 + hh, z0 + d
    W, H, D, U, V = (round(a * k) for a in (w, hh, d, u, v))
    assert all(abs(a * k - round(a * k)) < 1e-6 for a in (w, hh, d)), f"box size {w, hh, d} is not a whole number of texels at k={k}"
    for i in range(W):
        for j in range(D):
            yield "top", U + D + i, V + j, x0 + (i + .5) * s, y0, z1 - (j + .5) * s, i, D - 1 - j, W, D
            yield "bottom", U + D + W + i, V + j, x0 + (i + .5) * s, y1, z1 - (j + .5) * s, i, j, W, D
    for j in range(H):
        y = y0 + (j + .5) * s
        for i in range(D):
            yield "right", U + i, V + D + j, x0, y, z1 - (i + .5) * s, i, j, D, H
            yield "left", U + D + W + i, V + D + j, x1, y, z0 + (i + .5) * s, i, j, D, H
        for i in range(W):
            yield "front", U + D + i, V + D + j, x0 + (i + .5) * s, y, z0, i, j, W, H
            yield "back", U + 2 * D + W + i, V + D + j, x1 - (i + .5) * s, y, z1, i, j, W, H


def paint(parts, uv, size, k=1):
    W, H = size[0] * k, size[1] * k
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    done = set()
    for p in parts:
        R, t = world(parts, p["name"])
        for c in p["cubes"]:
            if c["key"] in done:
                continue
            done.add(c["key"])
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            for face, tx, ty, x, y, z, i, j, fw, fh in face_texels(u, v, x0, y0, z0, w, hh, d, k):
                assert 0 <= tx < W and 0 <= ty < H, (c["key"], tx, ty)
                X, Y, Z = (a + b for a, b in zip(mv(R, (x, y, z)), t))
                q = SimpleNamespace(face=face, X=X, Y=Y, Z=Z, x=x, y=y, z=z, n=mv(R, NORMALS[face]), i=i, j=j, fw=fw, fh=fh,
                                    tx=tx, ty=ty, k=k, box=c["box"], key=c["key"])
                col = c["mat"](q)
                if col is not None:
                    px[tx, ty] = col if len(col) == 4 else (*col, 255)
    return img


def _f(v):
    s = f"{float(v):.4f}".rstrip("0")
    return (s + "0" if s.endswith(".") else s) + "F"


def java_method(name, parts, uv, size, repeat=None):
    """A static createBodyLayer() for one model. repeat=(n, top): the parts are made n times, the part named top (and
    its subtree) as top + "_" + i."""
    lines = []
    for p in parts:
        bits = []
        for c in p["cubes"]:
            u, v = uv[c["key"]]
            x0, y0, z0, w, hh, d = c["box"]
            plane = ", PLANE" if 0 in (w, hh, d) else ""
            bits.append(f"\n                .texOffs({u}, {v})" + (".mirror()" if c["mirror"] else "")
                        + f".addBox({_f(x0)}, {_f(y0)}, {_f(z0)}, {_f(w)}, {_f(hh)}, {_f(d)}{plane})" + (".mirror(false)" if c["mirror"] else ""))
        px, py, pz = p["pivot"]
        rx, ry, rz = p["rot"]
        pose = (f"PartPose.offset({_f(px)}, {_f(py)}, {_f(pz)})" if (rx, ry, rz) == (0, 0, 0)
                else f"PartPose.offsetAndRotation({_f(px)}, {_f(py)}, {_f(pz)}, {_f(rx)}, {_f(ry)}, {_f(rz)})")
        parent = "root" if p["parent"] is None else p["parent"]
        label = f'"{p["name"]}"' if not repeat or p["name"] != repeat[1] else f'"{p["name"]}_" + i'
        lines.append(f'PartDefinition {p["name"]} = {parent}.addOrReplaceChild({label}, CubeListBuilder.create(){"".join(bits)},\n                {pose});')
    ind = "        "
    body = "\n".join(ind + ln for ln in lines)
    if repeat:
        body = f"{ind}for (int i = 0; i < {repeat[0]}; i++) {{\n" + "\n".join(ind + "    " + ln for ln in lines) + f"\n{ind}}}"
    return (f"    static LayerDefinition {name}() {{\n{ind}MeshDefinition mesh = new MeshDefinition();\n{ind}PartDefinition root = mesh.getRoot();\n"
            f"{body}\n{ind}return LayerDefinition.create(mesh, {size[0]}, {size[1]});\n    }}\n")


def write_java(path, package, cls, doc, methods):
    src = f"""package {package};

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by {doc} (with the skins it paints for these boxes). Do not edit. */
@SuppressWarnings("unused")
final class {cls} {{
    private static final CubeDeformation PLANE = new CubeDeformation(0.001F);

{chr(10).join(methods)}
    private {cls}() {{}}
}}
"""
    path.write_text(src, encoding="utf-8")


class Model:
    """A model under construction: parts and boxes; colour(material, q) paints a box of that material."""
    def __init__(self, name, size, k, colour):
        self.name, self.size, self.k, self.colour = name, size, k, colour
        self.parts, self.by = [], {}

    def part(self, name, parent, pivot, rot=(0, 0, 0)):
        p = part(name, parent, pivot, rot)
        self.parts.append(p)
        self.by[name] = p

    def box(self, pname, mat, x, y, z, w, hh, d, mirror=False, key=None):
        cubes = self.by[pname]["cubes"]
        key = key or f"{pname}.{len(cubes)}"
        cubes.append(cube(key, x, y, z, w, hh, d, lambda q, m=mat: self.colour(m, q), mirror))
        cubes[-1]["mname"] = mat

    def limb(self, name, side, *boxes, mat):
        """Boxes given for the right side (-x); the left one mirrors them and shares the texels."""
        for i, (x, y, z, w, hh, d) in enumerate(boxes):
            m = mat[i] if isinstance(mat, (list, tuple)) else mat
            key = f"{name.replace('left_', 'right_')}.{i}"
            if side < 0:
                self.box(name, m, x, y, z, w, hh, d, key=key)
            else:
                self.box(name, m, -x - w, y, z, w, hh, d, True, key=key)


# ---- small paint helpers shared by the painters

def h(x, y, salt):
    n = (int(x) * 374761393 + int(y) * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


def lit(c, q, top=-8.0, bottom=24.0, amt=0.12):
    """Light from above: tops brighter, undersides darker, and a gentle fall-off from the head to the feet."""
    f = 1.0 - 0.14 * q.n[1]
    f *= 1.0 + amt * 0.5 - amt * max(0.0, min(1.0, (q.Y - top) / (bottom - top)))
    out = shade(c, f)
    return (*out, c[3]) if len(c) == 4 else out
