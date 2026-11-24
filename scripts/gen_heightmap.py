"""Mod blocks the live heightmaps see: data/minecraft/tags/block/blocks_motion_no_leaves.json.

In 26.x Heightmap.Types MOTION_BLOCKING(_NO_LEAVES), OCEAN_FLOOR and OCEAN_FLOOR_WG no longer ask the block whether it
blocks motion: they count only blocks in #minecraft:blocks_motion_in_heightmap(_no_leaves), which vanilla fills through
#minecraft:blocks_motion(_no_leaves) with its own blocks and tags (#logs, #planks, #slabs, #stairs, #walls, #dirt,
#sand, #ores, ...). A mod block outside those tags is air to the heightmaps: rain and snow fall through it, surface
features, spawns and the nature clock read the ground under it.

So every mod block that blocked motion before 26.x goes in: the old rule (BlockStateBase.calculateSolid) for the default
state, a non-empty collision shape whose bounds are at least 0.7291666 on average over the three axes, or a full block
high. The shape comes from each block's model (the default variant, or every part of a multipart); blocks without
collision (plants, litter, roots, open frames) are told apart by their models and the NO_COLLISION list. Leaves stay
out (they go into the heightmaps through #minecraft:leaves, not into the _no_leaves map). The game test heightmap_tags
(HeightmapTest) checks this list against the real collision shapes of every registered mod block and names any block
this script got wrong.

Generator order: after every generator that adds blocks or block models, before gen_advancements.py (it took over the
old gen_nature_clock.py's place and its six blocks). Run: python scripts/gen_heightmap.py
"""
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
STATES = RES / "assets/mechanical/blockstates"
TAG = RES / "data/minecraft/tags/block/blocks_motion_no_leaves.json"
JAR = zipfile.ZipFile(next((ROOT / "build/moddev/artifacts").glob("minecraft-patched-*[0-9].jar")))

# model parents that mean no collision (plants and the like)
SOFT_PARENTS = ("block/cross", "block/tinted_cross", "block/crop", "block/stem_growth", "block/attached_stem",
                "block/vine", "block/carpet", "block/rail", "block/template_torch", "block/torch")
# blocks whose models look solid but which have no collision shape (Java: .noCollision() or a custom empty shape); the
# game test heightmap_tags names any new one
NO_COLLISION = {"mechanical:" + b for b in (
    "soot fern_flower blackthorn sea_buckthorn jujube hazel_shrub juniper bramble sapling_thicket rib_cage mammoth_tusk "
    "cave_painting tripod elkwight_antler_crown rosehip crowberry bearberry barberry caper red_currant black_currant wild_rice").split()}
# blocks drawn only by a block entity renderer (empty model) that still have a collision shape: solid
SOLID = {"mechanical:" + b for b in "water_wheel wooden_shaft".split()}
# names (suffixes) of blocks drawn as crosses that still have a collision shape, like vanilla's amethyst clusters: solid
SOLID_SUFFIXES = ("_crystal",)


def model(mid):
    ns, path = mid.split(":") if ":" in mid else ("minecraft", mid)
    rel = f"assets/{ns}/models/{path}.json"
    f = RES / rel
    if f.exists():
        return json.loads(f.read_text(encoding="utf-8"))
    try:
        return json.loads(JAR.read(rel))
    except KeyError:
        return None


def chain(mid):
    """The model and its parents, nearest first: [(id, json)]."""
    out = []
    while mid and len(out) < 16:
        m = model(mid)
        if m is None:
            break
        out.append((mid, m))
        mid = m.get("parent")
    return out


def bounds(mid):
    """(x0, y0, z0, x1, y1, z1) of the model's elements in block units, 'soft' for a no-collision parent, None unknown."""
    c = chain(mid)
    for i, m in c:
        if any(i.split(":")[-1] == p or i.endswith(p) for p in SOFT_PARENTS):
            return "soft"
        if "elements" in m:
            els = m["elements"]
            if not els:
                return None
            lo = [min(min(e["from"][k], e["to"][k]) for e in els) / 16 for k in range(3)]
            hi = [max(max(e["from"][k], e["to"][k]) for e in els) / 16 for k in range(3)]
            return (*lo, *hi)
    return None


def models_of(state):
    """The models of the default state: the variant with the fewest layers/age/etc., or all parts of a multipart."""
    def ids(v):
        return [x["model"] for x in (v if isinstance(v, list) else [v])]
    if "variants" in state:
        keys = list(state["variants"])
        # default-ish: layers=1 for layered blocks, else every variant (their union)
        layered = [k for k in keys if "layers=1" in k.split(",")]
        pick = layered[:1] or keys
        return [m for k in pick for m in ids(state["variants"][k])]
    return [m for part in state.get("multipart", []) for m in ids(part["apply"])]


def leaves():
    t = RES / "data/minecraft/tags/block/leaves.json"
    return {v for v in json.loads(t.read_text(encoding="utf-8"))["values"]} if t.exists() else set()


def solid(name, warn):
    bid = "mechanical:" + name
    if name.endswith(SOLID_SUFFIXES) or bid in SOLID:
        return True
    if bid in NO_COLLISION:
        return False
    box = None
    for mid in models_of(json.loads((STATES / f"{name}.json").read_text(encoding="utf-8"))):
        b = bounds(mid)
        if b == "soft":
            return False
        if b is None:
            continue
        box = b if box is None else (*map(min, box[:3], b[:3]), *map(max, box[3:], b[3:]))
    if box is None:
        warn.append(name)
        return False
    size = [max(0.0, min(1.0, box[k + 3]) - max(0.0, box[k])) for k in range(3)]
    return sum(size) / 3 >= 0.7291666666666666 or box[4] - box[1] >= 1.0


def main():
    skip, warn = leaves(), []
    blocks = sorted(f.stem for f in STATES.glob("*.json") if "mechanical:" + f.stem not in skip and solid(f.stem, warn))
    TAG.parent.mkdir(parents=True, exist_ok=True)
    TAG.write_text(json.dumps({"replace": False, "values": [f"mechanical:{b}" for b in blocks]}, indent=2) + "\n", encoding="utf-8")
    print(f"{TAG.relative_to(ROOT)}: {len(blocks)} blocks; no model shape for {len(warn)}: {' '.join(warn)}")


if __name__ == "__main__":
    main()
