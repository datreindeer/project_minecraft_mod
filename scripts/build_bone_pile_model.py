import json, math
from PIL import Image, ImageDraw

PROJ = "C:/Users/Yngve/Documents/project_minecraft_mod/src/main/resources/assets/mechanical"
MODEL = PROJ + "/models/block/bone_pile.json"
TEX = {"bone": Image.open(PROJ + "/textures/block/bone_pile_bone.png").convert("RGB"),
       "skull": Image.open(PROJ + "/textures/block/bone_pile_skull.png").convert("RGB")}
PREVIEW = "C:/Users/Yngve/AppData/Local/Temp/claude/43d83a9a335e657f/9ee37f73-4613-4afd-a1ce-400b3a021c61/scratchpad/bone_pile_preview.png"

els = []
seq = [0]


def r3(v):
    return round(v, 3)


def add(frm, to, name="", rot=None, tex="bone", uvs=None):
    """Bone element; uv windows sized to the face (1 texel = 1 px), offset per element for variety."""
    seq[0] += 1
    k = seq[0]
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    dims = {"up": (dx, dz), "north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy)}
    faces = {}
    for i, (f, (w, h)) in enumerate(dims.items()):
        if uvs and f in uvs:
            uv = uvs[f]
        else:
            u0 = (k * 7 + i * 3) % max(1, int(16 - w))
            v0 = (k * 5 + i * 7) % max(1, int(16 - h))
            uv = [u0, v0, u0 + w, v0 + h]
        faces[f] = {"uv": [r3(c) for c in uv], "texture": "#" + (tex if not (uvs and f in uvs) else tex)}
    e = {"name": name, "from": [r3(c) for c in frm], "to": [r3(c) for c in to], "faces": faces}
    if rot:
        e["rotation"] = {"angle": rot[0], "axis": "y", "origin": [r3(c) for c in rot[1]]}
    els.append(e)


# --- skull (faces west), snout ---
add([2, 0, 7], [6, 3, 12], "skull", tex="skull",
    uvs={"up": [0, 0, 4, 5], "west": [4, 0, 9, 3], "north": [0, 6, 4, 9], "south": [0, 6, 4, 9], "east": [0, 6, 5, 9]})
add([0.75, 0, 8], [2.25, 2, 11], "snout", tex="skull",
    uvs={"west": [9, 0, 12, 2]})
# --- spine: cord + vertebrae ---
add([6, 0, 9.25], [14.6, 0.75, 9.75], "spinal_cord")
for i, x in enumerate([6.5, 7.8, 9.1, 10.4, 11.7, 13.0]):
    s = 1.0 if i < 5 else 0.8
    add([x, 0, 9.5 - 0.6 * s], [x + s, 1.2 - 0.05 * i, 9.5 + 0.6 * s], f"vertebra_{i}")
# --- north ribs: two segments each, sweeping toward the tail (+x) ---
for i, (c, la, lb) in enumerate([(8.3, 3.0, 2.0), (9.6, 3.2, 2.3), (10.9, 3.0, 2.0), (12.2, 2.6, 1.6)]):
    a = math.radians(-22.5)
    add([c - 0.35, 0, 9 - la], [c + 0.35, 0.7, 9], f"rib_n{i}_a", rot=(-22.5, [c, 0, 9]))
    jx, jz = c + (-la) * math.sin(a), 9 + (-la) * math.cos(a)  # rotated far end of segment a
    add([jx - 0.32, 0, jz - lb], [jx + 0.32, 0.6, jz + 0.3], f"rib_n{i}_b", rot=(-45, [jx, 0, jz]))
# --- south ribs: shorter, single segment ---
for i, (c, l) in enumerate([(8.3, 2.0), (9.6, 2.3), (10.9, 1.9)]):
    add([c - 0.35, 0, 10], [c + 0.35, 0.65, 10 + l], f"rib_s{i}", rot=(22.5, [c, 0, 10]))
# --- femur (north, angled) ---
fo = [7, 0, 3]
add([3, 0, 2.6], [11, 0.9, 3.4], "femur_shaft", rot=(22.5, fo))
add([2, 0, 2], [3.4, 1.4, 4], "femur_head", rot=(22.5, fo))
add([10.6, 0, 2.2], [12, 1.3, 3.8], "femur_knob", rot=(22.5, fo))
# --- tibia (south, straight) ---
add([3.6, 0, 13.25], [10, 0.8, 13.95], "tibia_shaft")
add([2.6, 0, 12.75], [3.8, 1.2, 14.45], "tibia_head")
add([9.8, 0, 12.85], [11, 1.1, 14.35], "tibia_knob")
# --- scattered small bones crossing each other ---
add([12.6, 0, 12.7], [15, 0.6, 13.3], "small_bone_a", rot=(45, [13.8, 0, 13]))
add([12.9, 0, 12.75], [14.7, 0.9, 13.35], "small_bone_b", rot=(-45, [13.8, 0, 13.05]))

for e in els:  # sanity: coordinates in range
    assert all(0 <= c <= 16 for c in e["from"] + e["to"]), e["name"]
    for f in e["faces"].values():
        assert all(0 <= c <= 16 for c in f["uv"]), (e["name"], f)

model = {"parent": "minecraft:block/block",
         "textures": {"particle": "mechanical:block/bone_pile_bone",
                      "bone": "mechanical:block/bone_pile_bone",
                      "skull": "mechanical:block/bone_pile_skull"},
         "elements": els}
with open(MODEL, "w") as fh:
    json.dump(model, fh, indent=2)
json.load(open(MODEL))
print(len(els), "elements")

# --- preview: top-down (up faces, painter's order by top y) + front (south faces, by z) ---
S = 32
img = Image.new("RGB", (16 * S * 2 + 40, 16 * S), (74, 92, 52))
px = img.load()


def rot_inv(X, Z, e):
    if "rotation" not in e:
        return X, Z
    a = math.radians(e["rotation"]["angle"])
    ox, _, oz = e["rotation"]["origin"]
    x, z = X - ox, Z - oz  # inverse of x'=x cos+z sin, z'=-x sin+z cos
    return ox + x * math.cos(a) - z * math.sin(a), oz + x * math.sin(a) + z * math.cos(a)


def corners(e):
    (x0, _, z0), (x1, _, z1) = e["from"], e["to"]
    a = math.radians(e.get("rotation", {}).get("angle", 0))
    ox, _, oz = e.get("rotation", {}).get("origin", [0, 0, 0])
    out = []
    for x, z in ((x0, z0), (x1, z0), (x1, z1), (x0, z1)):
        x, z = x - ox, z - oz
        out.append((ox + x * math.cos(a) + z * math.sin(a), oz - x * math.sin(a) + z * math.cos(a)))
    return out


def sample(e, face, s, t):
    tex = TEX["skull" if e["faces"][face]["texture"] == "#skull" else "bone"]
    u0, v0, u1, v1 = e["faces"][face]["uv"]
    return tex.getpixel((min(15, int(u0 + s * (u1 - u0))), min(15, int(v0 + t * (v1 - v0)))))


for e in sorted(els, key=lambda e: e["to"][1]):
    cs = corners(e)
    xs, zs = [c[0] for c in cs], [c[1] for c in cs]
    (x0, y0, z0), (x1, y1, z1) = e["from"], e["to"]
    shade = 0.75 + 0.25 * min(1, y1 / 1.5)
    for PX in range(int(min(xs) * S), int(max(xs) * S) + 1):
        for PZ in range(int(min(zs) * S), int(max(zs) * S) + 1):
            x, z = rot_inv((PX + 0.5) / S, (PZ + 0.5) / S, e)
            if x0 <= x <= x1 and z0 <= z <= z1 and 0 <= PX < 16 * S and 0 <= PZ < 16 * S:
                c = sample(e, "up", (x - x0) / (x1 - x0), (z - z0) / (z1 - z0))
                px[PX, PZ] = tuple(int(v * shade) for v in c)
    ImageDraw.Draw(img).polygon([(a * S, b * S) for a, b in cs], outline=(60, 55, 45))

# front view (looking north): x-extent of footprint, scaled up 4x vertically for readability
OX, VS = 16 * S + 40, S * 3
d = ImageDraw.Draw(img)
d.rectangle([OX, 0, OX + 16 * S, 16 * S], fill=(120, 160, 200))
base = 16 * S - 40
d.line([OX, base, OX + 16 * S, base], fill=(70, 60, 40), width=3)
for e in sorted(els, key=lambda e: max(c[1] for c in corners(e))):
    cs = corners(e)
    xa, xb = min(c[0] for c in cs), max(c[0] for c in cs)
    y0, y1 = e["from"][1], e["to"][1]
    for PX in range(int(xa * S), int(xb * S)):
        for PY in range(int(y0 * VS), int(y1 * VS)):
            c = sample(e, "south", (PX / S - xa) / (xb - xa), 1 - (PY / VS - y0) / (y1 - y0))
            px[OX + PX, base - PY - 1] = c
    d.rectangle([OX + xa * S, base - y1 * VS, OX + xb * S, base - y0 * VS], outline=(60, 55, 45))
img.save(PREVIEW)
