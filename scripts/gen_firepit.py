"""Fire Pit data (FirePit.java, FirePitBlock.java, FirePitBlockEntity.java, client/FirePitRenderer.java).

- block model over the whole 2x2, drawn by the north-west corner (part 0; the other three parts are empty models with a
  stone particle): a ring of 18 rocks (radius 12.5 px round the centre of the 32 x 32 footprint, sizes, heights and turns
  varied, the face towards the fire sooted) with 6 smaller rocks bedded on top, a round ash bed of strips, two spit sticks
  along x (z 10.5 and 21.5, at the height of FirePitBlockEntity.SPIT_Y) on forked uprights outside the ring
- logs: the base layer (charred when cold, glowing when lit or embers) and three more layers for FUEL 1-3 (fresh bark
  when cold, glowing when burning), stacked log-cabin style: x, z, x, then two crossed diagonals on top
- fire: four crossed planes of the animated fire texture, taller with FUEL 2-3; only while LIT
- Bronze Fire Grate (bars over the ash when GRATE) and its item, the grate drops with the pit; messages
- multipart blockstate over part / lit / embers / fuel / grate / big; item: a flat picture (item/fire_pit)
- big Fire Pit (FirePitBlock.BIG, 3x3, F12): every model above scaled by 1.5 across and 1.25 up round the 3x3's middle,
  drawn by the centre part (4) as fire_pit_big_*; its own taller fire planes; the Steel Hearth Ring item, recipe (4 Steel
  Plate, 4 rock chunks, a Firewood), the ring drops with a broken big pit; messages
- recipe: 8 rock chunks round a Firewood (misc); loot: the Fire Pit from part 0 (FirePitBlock drops it when a side part
  is broken); lang
Textures: scripts/gen_firepit_textures.py.
Generator order: ... gen_pottery.py, gen_cart.py, gen_firepit.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_firepit.py"""
import json
import math
import random
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
SPIT_Y = 18.5          # FirePitBlockEntity.SPIT_Y
SPIT_Z = (10.5, 21.5)  # FirePitBlockEntity.SPIT_Z
C = 16                 # centre of the footprint, in pixels from the corner


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")  # escaped, like the rest of the lang file


def r2(v):
    return round(v, 3)


def face_uv(size_u, size_v, seed):
    """A uv window of the face's size (squeezed into 16 if longer) placed by seed, so neighbouring rocks differ."""
    su, sv = min(size_u, 16), min(size_v, 16)
    u = (seed * 5) % max(1, int(16 - su) + 1)
    v = (seed * 3) % max(1, int(16 - sv) + 1)
    return [r2(u), r2(v), r2(u + su), r2(v + sv)]


def box(frm, to, tex, seed=0, rot=None, uv=None, skip=(), frot=None, **extra):
    """An element with explicit uvs on every face (auto uvs would run off the sprite past 16)."""
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    sizes = {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}
    faces = {}
    for i, (f, (su, sv)) in enumerate(sizes.items()):
        if f in skip or su <= 0 or sv <= 0:
            continue
        t = tex[f] if isinstance(tex, dict) else tex
        faces[f] = {"uv": (uv or {}).get(f) or face_uv(su, sv, seed + i), "texture": t}
        if f in (frot or {}):
            faces[f]["rotation"] = frot[f]
    e = {"from": [r2(v) for v in frm], "to": [r2(v) for v in to], "faces": faces}
    if rot:
        e["rotation"] = rot
    e.update(extra)
    return e


def yrot(origin, angle):
    return {"origin": [r2(v) for v in origin], "axis": "y", "angle": r2(angle)}


# ---------------------------------------------------------------- the ring of rocks

def ring():
    rnd = random.Random(1913)
    out = []
    n = 18
    for i in range(n):
        a = 2 * math.pi * i / n + rnd.uniform(-0.06, 0.06)
        r = 12.5 + rnd.uniform(-0.4, 0.5)
        cx, cz = C + r * math.cos(a), C + r * math.sin(a)
        tw, rd, h = rnd.uniform(4.2, 5.8), rnd.uniform(3.2, 4.6), rnd.uniform(2.6, 4.8)
        # box local x along the tangent, z along the radius; turned about y to sit on the circle
        # local x (1,0,0) turns to (cos t, -sin t) (JOML rotation about +y); match it to the tangent (-sin a, cos a)
        ang = math.degrees(math.atan2(-math.cos(a), -math.sin(a)))
        ang = ((ang + 90) % 180) - 90 + rnd.uniform(-6, 6)
        frm, to = [cx - tw / 2, 0, cz - rd / 2], [cx + tw / 2, h, cz + rd / 2]
        # which side faces the fire after the turn (JOML rotationY: x' = x cos + z sin, z' = -x sin + z cos)
        th = math.radians(ang)
        towards = (C - cx, C - cz)
        normals = {"north": (0, -1), "south": (0, 1), "east": (1, 0), "west": (-1, 0)}
        inner = max(normals, key=lambda f: (normals[f][0] * math.cos(th) + normals[f][1] * math.sin(th)) * towards[0]
                    + (-normals[f][0] * math.sin(th) + normals[f][1] * math.cos(th)) * towards[1])
        base = "#stone" if i % 3 else "#stone_dark"
        tex = {f: base for f in ("north", "south", "east", "west", "up", "down")}
        tex[inner] = "#stone_sooted"
        out.append(box(frm, to, tex, seed=i * 7, rot=yrot((cx, 0, cz), ang), skip=("down",)))
    # smaller rocks bedded on top, over the joints
    for k in range(6):
        a = 2 * math.pi * (k * 3 + 0.5) / n + rnd.uniform(-0.05, 0.05)
        r = 12.8
        cx, cz = C + r * math.cos(a), C + r * math.sin(a)
        s, h = rnd.uniform(2.6, 3.4), rnd.uniform(1.6, 2.2)
        y = 2.6 + rnd.uniform(0, 0.8)
        out.append(box([cx - s / 2, y, cz - s / 2], [cx + s / 2, y + h, cz + s / 2], "#stone_dark" if k % 2 else "#stone",
                       seed=k * 11 + 3, rot=yrot((cx, y, cz), rnd.uniform(-40, 40)), skip=("down",)))
    return out


def ash(tex):
    """Round ash bed out of 2 px strips (no overlaps, so no flicker); the texture spans the whole disc at half scale."""
    out = []
    radius = 11
    for z0 in range(5, 27, 2):
        zc = z0 + 1 - C
        half = math.sqrt(max(0, radius * radius - zc * zc))
        if half < 2:
            continue
        x0, x1 = C - half, C + half
        uv = {"up": [r2(x0 / 2), z0 / 2, r2(x1 / 2), (z0 + 2) / 2]}
        out.append(box([x0, 0, z0], [x1, 1, z0 + 2], tex, uv=uv, skip=("down", "north", "south", "east", "west")))
    return out


def spits():
    out = []
    for z in SPIT_Z:
        out.append(box([1, SPIT_Y - 0.75, z - 0.75], [31, SPIT_Y + 0.75, z + 0.75], "#spit",
                       uv={f: [0, 7, 16, 8.5] for f in ("north", "south", "up", "down")} | {f: [0, 0, 1.5, 1.5] for f in ("east", "west")}))
        for x in (1.75, 30.25):
            out.append(box([x - 0.75, 0, z - 0.75], [x + 0.75, SPIT_Y - 0.75, z + 0.75], "#spit",
                           uv={f: [2, 0, 3.5, 16] for f in ("north", "south", "east", "west")} | {f: [0, 0, 1.5, 1.5] for f in ("up", "down")}))
            # the fork: two short tines splaying out of the upright, the spit resting between them
            for side in (-1, 1):
                out.append(box([x - 0.5, SPIT_Y - 1.5, z - 0.5], [x + 0.5, SPIT_Y + 1.8, z + 0.5], "#spit",
                               rot={"origin": [x, SPIT_Y - 1.5, z], "axis": "x", "angle": 25 * side},
                               uv={f: [5, 4, 6, 7.3] for f in ("north", "south", "east", "west")} | {f: [5, 4, 6, 5] for f in ("up", "down")}))
    return out


# ---------------------------------------------------------------- logs and fire

def log_x(x0, x1, y0, zc, tex, seed, th=4.0, rot=None):
    return box([x0, y0, zc - th / 2], [x1, y0 + th, zc + th / 2], {"north": tex[0], "south": tex[0], "up": tex[0], "down": tex[0], "east": tex[1], "west": tex[1]},
               seed=seed, rot=rot, uv={f: [0, (seed * 4) % 12, 16, (seed * 4) % 12 + th] for f in ("north", "south", "up", "down")}
               | {f: [6, 6, 6 + th, 6 + th] for f in ("east", "west")})


def log_z(z0, z1, y0, xc, tex, seed, th=4.0, rot=None):
    return box([xc - th / 2, y0, z0], [xc + th / 2, y0 + th, z1], {"east": tex[0], "west": tex[0], "up": tex[0], "down": tex[0], "north": tex[1], "south": tex[1]},
               seed=seed, rot=rot, uv={f: [0, (seed * 4) % 12, 16, (seed * 4) % 12 + th] for f in ("east", "west")}
               | {f: [0, (seed * 4) % 12, 16, (seed * 4) % 12 + th] for f in ("up", "down")}
               | {f: [6, 6, 6 + th, 6 + th] for f in ("north", "south")}, frot={"up": 90, "down": 90})  # grain along z


LAYERS = {
    # base layer (always there): two big logs along x, half sunk in the ash, and a burnt stub across
    0: lambda t: [log_x(6, 26, 0.5, 11.5, t, 1), log_x(7, 25, 0.5, 20.5, t, 2),
                  log_z(10, 22, 0.8, 16, t, 3, th=3, rot=yrot((16, 0.8, 16), 22.5))],
    1: lambda t: [log_z(6, 26, 4.5, 11, t, 4), log_z(7, 25, 4.5, 21, t, 5)],
    2: lambda t: [log_x(8, 24, 8.5, 13, t, 6, th=3.5), log_x(8, 24, 8.5, 19, t, 7, th=3.5)],
    3: lambda t: [log_x(9, 23, 12, 16, t, 8, th=3.5, rot=yrot((16, 12, 16), 45)),
                  log_x(9, 23, 12.2, 16, t, 9, th=3.5, rot=yrot((16, 12, 16), -45))],
}
LOGS = {"charred": ("#log_charred", "#log_end_charred"), "fresh": ("#log", "#log_end"), "lit": ("#log_lit", "#log_end_lit")}


def fire(height):
    out = []
    for i, ang in enumerate((0, 0, 45, 45)):  # x plane, z plane, then the same two turned onto the diagonals
        plane_x = i % 2 == 0
        frm = [7, 0.5, C] if plane_x else [C, 0.5, 7]
        to = [25, 0.5 + height, C] if plane_x else [C, 0.5 + height, 25]
        faces = {f: {"uv": [0, 0, 16, 16], "texture": "#fire"} for f in (("north", "south") if plane_x else ("east", "west"))}
        e = {"from": frm, "to": to, "shade": False, "light_emission": 15, "faces": faces}
        if ang:
            e["rotation"] = yrot((C, 0, C), ang)
        out.append(e)
    return out


TEX = {
    "particle": M + "block/fire_pit_stone", "stone": M + "block/fire_pit_stone", "stone_dark": M + "block/fire_pit_stone_dark",
    "stone_sooted": M + "block/fire_pit_stone_sooted", "spit": M + "block/fire_pit_spit",
    "ash": M + "block/fire_pit_ash", "ash_hot": M + "block/fire_pit_ash_hot",
    "log": M + "block/fire_pit_log", "log_end": M + "block/fire_pit_log_end",
    "log_lit": M + "block/fire_pit_log_lit", "log_end_lit": M + "block/fire_pit_log_end_lit",
    "log_charred": M + "block/fire_pit_log_charred", "log_end_charred": M + "block/fire_pit_log_end_charred",
    "fire": M + "block/fire_pit_fire",
    "grate": M + "block/bronze_fire_grate",
}


BIG_XZ, BIG_Y = 1.5, 1.25  # FirePitBlockEntity.BIG_SCALE, BIG_SPIT_Y / SPIT_Y


def scaled(elements):
    """The 2x2 model's elements for the big pit: x/z scaled round the footprint's middle (16) onto the centre block's middle (8),
    y scaled up; a 32 px footprint becomes 48 (-16..32 from the centre block, the most a block model may reach)."""
    out = []
    for e in json.loads(json.dumps(elements)):
        sx = lambda v: r2(8 + (v - C) * BIG_XZ)
        sy = lambda v: r2(v * BIG_Y)
        e["from"] = [sx(e["from"][0]), sy(e["from"][1]), sx(e["from"][2])]
        e["to"] = [sx(e["to"][0]), sy(e["to"][1]), sx(e["to"][2])]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [sx(o[0]), sy(o[1]), sx(o[2])]
        out.append(e)
    return out


def model(name, elements, glow=False):
    m = {"ambientocclusion": False, "textures": TEX, "elements": elements}
    if glow:  # glowing logs and coals: lit from inside
        for e in m["elements"]:
            e.setdefault("light_emission", 10)
    w(A / f"models/block/{name}.json", m)
    if not name.startswith("fire_pit_fire"):
        big = {"ambientocclusion": False, "textures": TEX, "elements": scaled(m["elements"])}
        w(A / f"models/block/{name.replace('fire_pit_', 'fire_pit_big_', 1)}.json", big)


B = "mechanical:block/"
w(A / "models/block/fire_pit_part.json", {"textures": {"particle": TEX["particle"]}})
model("fire_pit_base_cold", ring() + ash("#ash") + spits())
model("fire_pit_base_hot", ring() + spits())
model("fire_pit_ash_hot", ash("#ash_hot"), glow=True)
for state, tex in LOGS.items():
    for layer, make in LAYERS.items():
        if layer == 0 and state == "fresh" or layer > 0 and state == "charred":
            continue  # the base layer is never fresh wood; fuel stacked on top is never charred
        model(f"fire_pit_logs{layer}_{state}", make(tex), glow=state == "lit")
# Bronze Fire Grate (FirePitBlock.GRATE): a frame with six bars along z, just over the ash, under the logs
GRATE = [box([7, 1, 6.5], [25, 2, 7.5], "#grate", seed=1), box([7, 1, 24.5], [25, 2, 25.5], "#grate", seed=2)] + [
    box([x, 1.2, 7.5], [x + 1, 1.9, 24.5], "#grate", seed=3 + i) for i, x in enumerate((8, 11, 14, 17, 20, 23))]
model("fire_pit_grate", GRATE)
model("fire_pit_fire", fire(20))
model("fire_pit_fire_tall", fire(27))
# the big pit's fire: the same planes spread over the scaled ash, as tall as a block model allows
model("fire_pit_fire_big", scaled(fire(20)))
model("fire_pit_fire_big_tall", [e | {"to": [e["to"][0], 31.5, e["to"][2]]} for e in scaled(fire(27))])

parts = [{"when": {"OR": [{"big": "false", "part": "1|2|3|4|5|6|7|8"}, {"big": "true", "part": "0|1|2|3|5|6|7|8"}]},
          "apply": {"model": B + "fire_pit_part"}}]
for big, P0, pre in ((False, {"part": "0", "big": "false"}, "fire_pit_"), (True, {"part": "4", "big": "true"}, "fire_pit_big_")):
    HOT = [P0 | {"lit": "true"}, P0 | {"embers": "true"}]
    COLD = P0 | {"lit": "false", "embers": "false"}
    parts += [
        {"when": COLD, "apply": {"model": B + pre + "base_cold"}},
        {"when": {"OR": HOT}, "apply": {"model": B + pre + "base_hot"}},
        {"when": {"OR": HOT}, "apply": {"model": B + pre + "ash_hot"}},
        {"when": COLD, "apply": {"model": B + pre + "logs0_charred"}},
        {"when": {"OR": HOT}, "apply": {"model": B + pre + "logs0_lit"}},
    ]
    for layer in (1, 2, 3):
        fuel = "|".join(str(f) for f in range(layer, 4))
        parts.append({"when": COLD | {"fuel": fuel}, "apply": {"model": B + pre + f"logs{layer}_fresh"}})
        parts.append({"when": {"OR": [h | {"fuel": fuel} for h in HOT]}, "apply": {"model": B + pre + f"logs{layer}_lit"}})
    parts.append({"when": P0 | {"grate": "true"}, "apply": {"model": B + pre + "grate"}})
    parts.append({"when": P0 | {"lit": "true", "fuel": "0|1"}, "apply": {"model": B + ("fire_pit_fire_big" if big else "fire_pit_fire")}})
    parts.append({"when": P0 | {"lit": "true", "fuel": "2|3"}, "apply": {"model": B + ("fire_pit_fire_big_tall" if big else "fire_pit_fire_tall")}})
w(A / "blockstates/fire_pit.json", {"multipart": parts})

w(A / "models/item/steel_hearth_ring.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/steel_hearth_ring"}})
w(A / "items/steel_hearth_ring.json", {"model": {"type": "minecraft:model", "model": M + "item/steel_hearth_ring"}})
w(D / "mechanical/recipe/steel_hearth_ring.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["PRP", "RFR", "PRP"], "key": {"P": "mechanical:steel_plate", "R": "#mechanical:rock_chunks", "F": "#mechanical:firewood"},
    "result": {"id": M + "steel_hearth_ring"}})

w(A / "models/item/fire_pit.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/fire_pit"}})
w(A / "items/fire_pit.json", {"model": {"type": "minecraft:model", "model": M + "item/fire_pit"}})

w(A / "models/item/bronze_fire_grate.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/bronze_fire_grate"}})
w(A / "items/bronze_fire_grate.json", {"model": {"type": "minecraft:model", "model": M + "item/bronze_fire_grate"}})

w(D / "mechanical/recipe/fire_pit.json", {"type": "minecraft:crafting_shaped", "category": "misc",
    "pattern": ["RRR", "RFR", "RRR"], "key": {"R": "#mechanical:rock_chunks", "F": "#mechanical:firewood"},
    "result": {"id": M + "fire_pit"}})

w(D / "mechanical/loot_table/blocks/fire_pit.json", {"type": "minecraft:block", "pools": [{
    "rolls": 1, "condition": {"type": "minecraft:survives_explosion"},
    "entries": [{"type": "minecraft:item", "name": M + "fire_pit",
                 "condition": {"type": "minecraft:match_block", "blocks": M + "fire_pit", "state": {"part": "0"}}}]}, {
    "rolls": 1, "condition": {"type": "minecraft:match_block", "blocks": M + "fire_pit", "state": {"part": "0", "grate": "true"}},
    "entries": [{"type": "minecraft:item", "name": M + "bronze_fire_grate"}]}, {
    "rolls": 1, "condition": {"type": "minecraft:match_block", "blocks": M + "fire_pit", "state": {"part": "0", "big": "true"}},
    "entries": [{"type": "minecraft:item", "name": M + "steel_hearth_ring"}]}],
    "random_sequence": "mechanical:blocks/fire_pit"})

merge(A / "lang/en_us.json", {
    "block.mechanical.fire_pit": "Fire Pit",
    "message.mechanical.fire_pit.no_room": "A Fire Pit needs a flat 2×2 of solid ground in front of you",
    "message.mechanical.fire_pit.full": "The Fire Pit holds no more firewood (%s)",
    "message.mechanical.fire_pit.fuel": "Firewood stacked on the fire: %s/%s",
    "item.mechanical.bronze_fire_grate": "Bronze Fire Grate",
    "message.mechanical.fire_pit.needs_grate": "Charcoal needs a grate to draw air: lay a Bronze Fire Grate in the pit",
    "message.mechanical.fire_grate.ingots": "A Bronze Fire Grate needs 3 Bronze Ingots (%s/%s)",
    "item.mechanical.steel_hearth_ring": "Steel Hearth Ring",
    "message.mechanical.fire_pit.big_blocked": "The Fire Pit can't grow to 3×3: %s in the way",
    "message.mechanical.fire_pit.big_blocker": "%s at %s %s %s",
    "message.mechanical.fire_pit.big_more": " and %s more",
    "message.mechanical.fire_pit.roast_big": "Only a big Fire Pit (3×3) can roast a whole animal",
    "message.mechanical.fire_pit.roast_busy": "Something is already roasting over this fire",
    "message.mechanical.fire_pit.roast_skin": "Skin it first: hide, wool or feathers would burn",
    "message.mechanical.fire_pit.roast_cannot": "That meat can't be roasted",
    "message.mechanical.tripod.big_middle": "On a big Fire Pit the Tripod stands over the middle",
})
print("firepit ok")
