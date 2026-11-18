"""Shared helpers for the open-country wildlife texture painters (gen_aurochs_texture.py, gen_wild_horse_texture.py,
gen_mouflon_texture.py). Same approach as gen_red_deer_texture.py: every texel is painted from the 3D point it covers
on its cube, so markings line up across faces.

A box table maps name -> (u, v, x0, y0, z0, w, h, d, material); material(variant, face, x, y, z, i, j) -> rgb/rgba.
Coordinates are the cube's local model coordinates (pixels; y down, -z = front, +x = the animal's left).
"""
import importlib.util
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures/entity"


def h(x, y, salt):
    n = (int(x) * 374761393 + int(y) * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


def fur(c, i, j, salt, amt=0.07, streak=True):
    """Per-texel speckle plus faint vertical hair streaks (in texture space, so it reads as hair on every face)."""
    n = (h(i, j, salt) - 0.5) * 2 * amt
    if streak:
        n += (h(i, j // 2, salt + 7) - 0.5) * amt
    return shade(c, 1 + n)


def smooth(t):
    t = max(0.0, min(1.0, t))
    return t * t * (3 - 2 * t)


def face_texels(u, v, x0, y0, z0, w, hh, d):
    """Yield (face, tx, ty, x, y, z) for every texel of a box-UV cube (vanilla ModelPart.Cube layout)."""
    x1, y1, z1 = x0 + w, y0 + hh, z0 + d
    for i in range(w):
        for j in range(d):
            yield "top", u + d + i, v + j, x0 + i + .5, y0, z1 - j - .5
            yield "bottom", u + d + w + i, v + j, x0 + i + .5, y1, z1 - j - .5
    for j in range(hh):
        y = y0 + j + .5
        for i in range(d):
            yield "right", u + i, v + d + j, x0, y, z1 - i - .5          # -x side = entity's right
            yield "left", u + d + w + i, v + d + j, x1, y, z0 + i + .5   # +x side = entity's left
        for i in range(w):
            yield "front", u + d + i, v + d + j, x0 + i + .5, y, z0
            yield "back", u + 2 * d + w + i, v + d + j, x1 - i - .5, y, z1


def paint(boxes, variant, size):
    """Paint every box; asserts the boxes fit the texture and no two boxes share a texel."""
    W, H = size
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = img.load()
    owner = {}
    for name, (u, v, x0, y0, z0, w, hh, d, mat) in boxes.items():
        for face, tx, ty, x, y, z in face_texels(u, v, x0, y0, z0, w, hh, d):
            assert 0 <= tx < W and 0 <= ty < H, (name, tx, ty)
            assert owner.setdefault((tx, ty), name) == name, f"UV overlap {name} / {owner[(tx, ty)]} at {tx},{ty}"
            c = mat(variant, face, x, y, z, tx, ty)
            px[tx, ty] = c if len(c) == 4 else (*c, 255)
    return img


def _skinner():
    spec = importlib.util.spec_from_file_location("gen_skinned_textures", ROOT / "scripts/gen_skinned_textures.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def skinned(img, boxes, keep=()):
    """Flesh version (for butchered carcasses); boxes named in keep (horns, hooves...) are copied back unchanged."""
    out = _skinner().skin(img.copy())
    src, dst = img.load(), out.load()
    for name in keep:
        u, v, _x, _y, _z, w, hh, d, _m = boxes[name]
        for tx in range(u, u + 2 * d + 2 * w):
            for ty in range(v, v + d + hh):
                dst[tx, ty] = src[tx, ty]
    assert out.size == img.size and out.getchannel("A").tobytes() == img.getchannel("A").tobytes()
    return out


def save(img, rel):
    out = TEX / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    img.save(out)
    print("wrote", out.relative_to(ROOT))
