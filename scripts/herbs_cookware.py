"""Kitchen cookware models (textures: scripts/gen_cookware_textures.py).

Block models, blockstates, item models and item definitions for the Clay Cooking Pot, Bronze Griddle, Iron Skillet,
Fermenting Barrel, Cheese Press and Salt Pan, and the Steel Age's Steel Pan, Mortar and Pestle and Sieve; the Iron Cauldron's models for the Tripod (patch_tripod adds them to
gen_kettle.py's tripod blockstate, so run this after gen_kettle.py); flat items for the Unfired Clay Cooking Pot, Iron
Cauldron, Bronze Ladle and the dishes.

Round things are stacked octagonal layers (`disc` solid, `ring` hollow round a square or octagonal opening), each layer
a little wider or narrower than the last so bellies swell and necks pull in. Side-face uvs follow the world height
(`vtop`), so a texture's rows run down the whole pot (soot at the bottom, a decorated band at the shoulder).

Blocks that stand ON a campfire reach down to y = -16 (the ground the campfire stands on); y = -9 is the top of its logs.
Numbers the Java side relies on:
- clay_cooking_pot: opening x,z 4..12 (square), inner floor y -4.5, rim top y 2.5 (soup drawn at y -3.5..1.5)
- bronze_griddle: plate top y 1, rim 1..1.75 round a flat x,z 2.75..13.25 (items lie at y 1.2 inside 3..13)
- iron_skillet: pan floor top y 1, walls to y 3.5, inner x,z 3..13 (corners chamfered to 6.3 from the middle); the
  handle and its ring stick 3 px out past the block edge in `facing`
- iron_cauldron_pit: middle at the block's south-east corner (16, 16), opening +-5 round it, floor y 0, rim top y 6.7
  (liquid drawn at y 0.5..5.5); iron_cauldron_big: the same x1.5 round (8, 0, 8) like kettle_big
Run: python scripts/herbs_cookware.py
"""
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A = ROOT / "assets/mechanical"
M = "mechanical:"
B = "mechanical:block/"
S2 = math.sqrt(2)
PIT_LIFT = 4   # TripodBlockEntity.PIT_LIFT, as gen_kettle.py
BIG = 1.5      # TripodBlock.BIG_SCALE
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}

DISHES = ["nettle_soup", "root_pottage", "berry_mash", "pea_soup", "onion_garlic_stew", "spiced_porridge", "herbal_tea", "tea",
          "cabbage_soup", "vegetable_pottage", "herb_roasted_fish", "flatbread", "raw_honey_cake", "honey_cake",
          "elderberry_syrup", "sloe_jam", "fried_eggs_and_onion", "raw_sausage", "sausage", "sauerkraut", "cheese", "indigo",
          # phase 6 and the phase 4-5 leftovers
          "roast_with_herbs", "cider", "ale", "kitchen_pepper", "sweet_spice", "golden_spice", "pickling_spice", "herb_salt", "fine_flour",
          "sea_buckthorn_juice", "saffron_rice", "peppered_steak", "spiced_pilaf", "pickled_vegetables", "raw_gingerbread", "gingerbread",
          # flora3: oils, frying, marmalade, pickled olives, Frost Lily Tea
          "olive_oil", "rapeseed_oil", "fried_fish", "fried_roots", "pickled_olives", "bitter_orange_marmalade", "frost_lily_tea"]
DISHES += [f"{raw}{f}_pie" for f in ("apple", "plum", "rowan", "bird_cherry") for raw in ("raw_", "")]
FLAT = ["unfired_clay_cooking_pot", "iron_cauldron"] + DISHES


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def r2(v):
    return round(v, 3)


SIDES = ("north", "south", "east", "west")


def box(frm, to, tex, rot=None, skip=(), uv=None, vtop=None):
    """An element with uvs sized to each face (as gen_kettle.py's box). vtop=(y, v): side faces take their v from the
    world height, row v at height y, so a texture's rows line up across stacked boxes."""
    dx, dy, dz = (to[i] - frm[i] for i in range(3))
    sizes = {"north": (16 - to[0], dx, dy), "south": (frm[0], dx, dy), "east": (16 - to[2], dz, dy), "west": (frm[2], dz, dy),
             "up": (frm[0], dx, dz), "down": (frm[0], dx, dz)}
    faces = {}
    for f, (start, su, sv) in sizes.items():
        if f in skip or su <= 1e-6 or sv <= 1e-6:
            continue
        t = tex[f] if isinstance(tex, dict) else tex
        su, sv = min(su, 16), min(sv, 16)
        if uv and f in uv:
            u = uv[f]
        else:
            u0 = start % 16
            if u0 + su > 16:
                u0 = 16 - su
            if f in ("up", "down"):
                v0 = frm[2] % 16
                if v0 + sv > 16:
                    v0 = 16 - sv
            elif vtop:
                v0 = min(max(vtop[1] + vtop[0] - to[1], 0), 16 - sv)
            else:
                v0 = (16 - sv) / 2
            u = [u0, v0, u0 + su, v0 + sv]
        faces[f] = {"uv": [r2(x) for x in u], "texture": t}
    e = {"from": [r2(v) for v in frm], "to": [r2(v) for v in to], "faces": faces}
    if rot:
        e["rotation"] = rot
    return e


def faces_tex(out, **special):
    return {f: special.get(f, out) for f in ("north", "south", "east", "west", "up", "down")}


# ---------------------------------------------------------------- round layers

def ring(cx, cz, y0, y1, A_, Ad, a, ad, out, ins, top=None, bottom=None, vtop=None):
    """A hollow octagonal layer: outer octagon with axis apothem A_ and diagonal apothem Ad, round an opening with axis
    half-width a and diagonal apothem ad (ad = a*sqrt2 is a plain square). 4 walls, 4 corner pieces (turned 45 degrees),
    and fillers where the opening's straight sides run past the outer walls."""
    up, dn = top or out, bottom or out
    eo = min(Ad * S2 - A_, A_)       # half-length of the outer straight sides
    ei = min(ad * S2 - a, a)         # half-length of the opening's straight sides
    wc = (A_ - eo) / S2              # corner piece half-width
    els = []
    # walls: north (z-), south (z+), west (x-), east (x+); the face toward the middle shows the inside
    els.append(box([cx - eo, y0, cz - A_], [cx + eo, y1, cz - a], faces_tex(out, south=ins, up=up, down=dn), vtop=vtop))
    els.append(box([cx - eo, y0, cz + a], [cx + eo, y1, cz + A_], faces_tex(out, north=ins, up=up, down=dn), vtop=vtop))
    els.append(box([cx - A_, y0, cz - eo], [cx - a, y1, cz + eo], faces_tex(out, east=ins, up=up, down=dn), vtop=vtop))
    els.append(box([cx + a, y0, cz - eo], [cx + A_, y1, cz + eo], faces_tex(out, west=ins, up=up, down=dn), vtop=vtop))
    if ei > eo + 1e-3:
        z1 = Ad * S2 - ei  # how far out the filler may reach at its outer end
        for sx in (-1, 1):
            xa, xb = sorted((cx + sx * eo, cx + sx * ei))
            els.append(box([xa, y0, cz - z1], [xb, y1, cz - a], faces_tex(out, south=ins, up=up, down=dn), vtop=vtop))
            els.append(box([xa, y0, cz + a], [xb, y1, cz + z1], faces_tex(out, north=ins, up=up, down=dn), vtop=vtop))
            els.append(box([cx - z1, y0, xa - cx + cz], [cx - a, y1, xb - cx + cz], faces_tex(out, east=ins, up=up, down=dn), vtop=vtop))
            els.append(box([cx + a, y0, xa - cx + cz], [cx + z1, y1, xb - cx + cz], faces_tex(out, west=ins, up=up, down=dn), vtop=vtop))
    if Ad - ad > 1e-3 and wc > 1e-3:
        rot = {"origin": [cx, y0, cz], "axis": "y", "angle": 45}
        # local +z turns to the south-east, local +x to the north-east
        els.append(box([cx - wc, y0, cz + ad], [cx + wc, y1, cz + Ad], faces_tex(out, north=ins, up=up, down=dn), rot=dict(rot), vtop=vtop))
        els.append(box([cx - wc, y0, cz - Ad], [cx + wc, y1, cz - ad], faces_tex(out, south=ins, up=up, down=dn), rot=dict(rot), vtop=vtop))
        els.append(box([cx + ad, y0, cz - wc], [cx + Ad, y1, cz + wc], faces_tex(out, west=ins, up=up, down=dn), rot=dict(rot), vtop=vtop))
        els.append(box([cx - Ad, y0, cz - wc], [cx - ad, y1, cz + wc], faces_tex(out, east=ins, up=up, down=dn), rot=dict(rot), vtop=vtop))
    return els


def disc(cx, cz, y0, y1, A_, out, top=None, bottom=None, Ad=None, vtop=None):
    """A solid octagonal layer: two crossed boxes and the same pair turned 45 degrees (regular when Ad is None)."""
    Ad = A_ if Ad is None else Ad
    eo = min(Ad * S2 - A_, A_)
    wc = (A_ - eo) / S2
    t = faces_tex(out, up=top or out, down=bottom or out)
    els = [box([cx - eo, y0, cz - A_], [cx + eo, y1, cz + A_], t, vtop=vtop),
           box([cx - A_, y0, cz - eo], [cx + A_, y1, cz + eo], t, vtop=vtop)]
    if wc > 1e-3:
        rot = {"origin": [cx, y0, cz], "axis": "y", "angle": 45}
        els.append(box([cx - wc, y0, cz - Ad], [cx + wc, y1, cz + Ad], t, rot=dict(rot), vtop=vtop))
        els.append(box([cx - Ad, y0, cz - wc], [cx + Ad, y1, cz + wc], t, rot=dict(rot), vtop=vtop))
    return els


def shift(els, dx=0.0, dy=0.0, dz=0.0):
    out = json.loads(json.dumps(els))
    for e in out:
        e["from"] = [r2(e["from"][0] + dx), r2(e["from"][1] + dy), r2(e["from"][2] + dz)]
        e["to"] = [r2(e["to"][0] + dx), r2(e["to"][1] + dy), r2(e["to"][2] + dz)]
        if "rotation" in e:
            o = e["rotation"]["origin"]
            e["rotation"]["origin"] = [r2(o[0] + dx), r2(o[1] + dy), r2(o[2] + dz)]
    return out


def scaled(els, k=BIG):
    """kettle_big's scaling: x and z by k round 8, y by k round 0."""
    out = json.loads(json.dumps(els))
    s = lambda p: [r2(8 + (p[0] - 8) * k), r2(p[1] * k), r2(8 + (p[2] - 8) * k)]
    for e in out:
        e["from"], e["to"] = s(e["from"]), s(e["to"])
        if "rotation" in e:
            e["rotation"]["origin"] = s(e["rotation"]["origin"])
    return out


# ---------------------------------------------------------------- Clay Cooking Pot (on a campfire)

POT_TEX = {"particle": B + "cooking_pot_clay", "clay": B + "cooking_pot_clay", "inside": B + "cooking_pot_inside",
           "rim": B + "cooking_pot_rim", "soot": B + "cooking_pot_bottom"}
POT_V = (2.5, 1.5)  # cooking_pot_clay row 1.5 is the rim top: the rows run down to the sooted bottom at row 12


def clay_pot():
    c, o, i, V = "#clay", "#inside", 8, POT_V
    els = []
    # a rounded bottom resting in the embers
    els += disc(i, i, -8, -7.3, 3.4, c, bottom="#soot", vtop=V)
    els += disc(i, i, -7.3, -6.5, 4.7, c, bottom="#soot", vtop=V)
    els += disc(i, i, -6.5, -5.5, 5.5, c, bottom="#soot", vtop=V)
    els += disc(i, i, -5.5, -4.5, 5.9, o, bottom="#soot", vtop=V)  # its top is the inner floor
    for e in els[-4:]:
        for f in SIDES:
            e["faces"][f]["texture"] = c
    # the belly, shoulder, neck and the flared rolled rim round the square opening 4..12
    sq = 4 * S2
    els += ring(i, i, -4.5, -2.0, 6.0, 6.0, 4, sq, c, o, vtop=V)
    els += ring(i, i, -2.0, -0.6, 5.65, 5.95, 4, sq, c, o, vtop=V)
    els += ring(i, i, -0.6, 0.6, 5.1, 5.9, 4, sq, c, o, vtop=V)
    els += ring(i, i, 0.6, 1.3, 4.7, 5.8, 4, sq, c, o, vtop=V)
    els += ring(i, i, 1.3, 1.9, 5.45, 6.15, 4, sq, c, o, top="#rim", vtop=V)
    els += ring(i, i, 1.9, 2.5, 5.75, 6.35, 4, sq, c, o, top="#rim", vtop=V)
    return els


# ---------------------------------------------------------------- Bronze Griddle (on a campfire)

GRIDDLE_TEX = {"particle": B + "griddle_plate", "plate": B + "griddle_plate", "bronze": B + "kettle_bronze",
               "leg": B + "griddle_leg"}


def griddle():
    """Facing north: the hanging tab points north; legs south-west, south-east and north."""
    p, b, l = "#plate", "#bronze", "#leg"
    els = []
    # the plate: a square with rounded (chamfered) corners, 2..14, 0.4 px thick under a 1 px raised rim
    els += disc(8, 8, 0, 1, 6.0, b, top=p, Ad=7.6)
    for e in els:
        e["faces"]["up"]["uv"] = [r2(e["from"][0]), r2(e["from"][2]), r2(e["to"][0]), r2(e["to"][2])]
    els += ring(8, 8, 1, 1.75, 6.0, 7.6, 5.25, 6.95, b, b)
    # a lip under the plate's edge (the thickened, hammered-over border)
    els += ring(8, 8, -0.6, 0, 5.6, 7.1, 4.6, 6.0, b, b)
    # three thin legs, tapering, splayed a little, down to the ground beside the logs
    for x, z, ax, ang in ((3.2, 12.8, "x", -6), (12.8, 12.8, "x", -6), (8, 2.6, "x", 6)):
        els.append(box([x - 0.55, -9, z - 0.55], [x + 0.55, -0.6, z + 0.55], l, vtop=(0, 0), rot={"origin": [x, -0.6, z], "axis": ax, "angle": ang}))
        els.append(box([x - 0.4, -16, z - 0.4], [x + 0.4, -9, z + 0.4], l, vtop=(-9, 9), rot={"origin": [x, -0.6, z], "axis": ax, "angle": ang}))
        els.append(box([x - 0.8, -1.2, z - 0.8], [x + 0.8, -0.6, z + 0.8], b))  # rivet boss under the plate
    # the hanging tab with its hole, north
    els.append(box([6.5, 0.3, 0.6], [9.5, 0.9, 2.1], b))
    els.append(box([6.5, 0.3, 0.0], [7.3, 0.9, 0.6], b))
    els.append(box([8.7, 0.3, 0.0], [9.5, 0.9, 0.6], b))
    els.append(box([7.3, 0.3, -0.4], [8.7, 0.9, 0.0], b))
    return els


# ---------------------------------------------------------------- Iron Skillet (on a campfire)

SKILLET_TEX = {"particle": B + "skillet_iron", "iron": B + "skillet_iron", "floor": B + "skillet_floor"}


def skillet():
    """Facing north: the handle runs north to the block edge, its ring hanging just past it."""
    ir, fl = "#iron", "#floor"
    els = []
    els += disc(8, 8, -0.4, 0.2, 5.5, ir, Ad=5.8)
    els += disc(8, 8, 0.2, 1.0, 6.0, ir, top=fl, Ad=6.7)
    for e in els[-4:]:
        e["faces"]["up"]["uv"] = [r2(e["from"][0]), r2(e["from"][2]), r2(e["to"][0]), r2(e["to"][2])]
    els += ring(8, 8, 1.0, 2.6, 6.2, 6.95, 5, 6.3, ir, ir)
    els += ring(8, 8, 2.6, 3.5, 6.45, 7.15, 5, 6.3, ir, ir)  # the rim thickens and flares a little
    # pour spouts east and west
    els.append(box([1.15, 2.9, 7.0], [1.6, 3.5, 9.0], ir))
    els.append(box([14.4, 2.9, 7.0], [14.85, 3.5, 9.0], ir))
    # three short legs, splayed, down to the ground
    for x, z, ang in ((4.0, 11.8, -7), (12.0, 11.8, -7), (8, 3.0, 7)):
        els.append(box([x - 0.6, -16, z - 0.6], [x + 0.6, -0.4, z + 0.6], ir, vtop=(0, 0), rot={"origin": [x, -0.4, z], "axis": "x", "angle": ang}))
        els.append(box([x - 0.9, -16, z - 0.9], [x + 0.9, -15.4, z + 0.9], ir, rot={"origin": [x, -0.4, z], "axis": "x", "angle": ang}))
    # the long handle: a root block cast onto the wall, a flat bar rising a little out past the block edge, a ring
    els.append(box([7.0, 1.4, 1.0], [9.0, 3.4, 2.0], ir))
    els.append(box([7.25, 2.6, -2.2], [8.75, 3.4, 1.2], ir, rot={"origin": [8, 3.0, 1.2], "axis": "x", "angle": -12}))
    els.append(box([7.1, 3.15, -2.9], [8.9, 3.95, -1.9], ir))  # the flattened, pierced end
    # the ring through the end, hanging down: four bars round a hole
    els.append(box([7.0, 2.7, -3.0], [9.0, 3.3, -2.4], ir))
    els.append(box([6.7, 0.6, -2.95], [7.3, 2.7, -2.45], ir))
    els.append(box([8.7, 0.6, -2.95], [9.3, 2.7, -2.45], ir))
    els.append(box([7.0, 0.1, -3.0], [9.0, 0.7, -2.4], ir))
    return els


# ---------------------------------------------------------------- Iron Cauldron (in the Tripod)

CAULDRON_TEX = {"particle": B + "cauldron_iron", "iron": B + "cauldron_iron", "inside": B + "cauldron_inside",
                "chain": B + "cauldron_chain", "bronze": B + "kettle_bronze"}
LINK = {f: [6, 4, 10, 12] for f in SIDES}  # the one oval link drawn in cauldron_chain.png (as kettle_chain)


def cauldron(cx, cz, lift):
    """Round-bellied cast iron pot, middle at (cx, cz), lifted by lift (gen_kettle.kettle's frame: floor at -4 + lift)."""
    T = lambda y: y + lift
    ir, ins, V = "#iron", "#inside", (T(2.7), 1)
    els = []
    # three stubby feet
    for k in range(3):
        a = math.radians(90 + 120 * k)
        fx, fz = cx + 3.4 * math.cos(a), cz + 3.4 * math.sin(a)
        els.append(box([fx - 0.9, T(-6.0), fz - 0.9], [fx + 0.9, T(-5.3), fz + 0.9], ir))
        els.append(box([fx - 0.65, T(-6.7), fz - 0.65], [fx + 0.65, T(-6.0), fz + 0.65], ir))
    # the pot belly: a rounded bottom swelling out, then pulling in under a thick rolled rim
    els += disc(cx, cz, T(-5.5), T(-5.0), 4.2, ir, vtop=V)
    els += disc(cx, cz, T(-5.0), T(-4.5), 5.6, ir, vtop=V)
    els += disc(cx, cz, T(-4.5), T(-4.0), 6.5, ir, top=ins, vtop=V)
    sq = 5 * S2
    els += ring(cx, cz, T(-4.0), T(-3.2), 7.1, 7.5, 5, sq, ir, ins, vtop=V)
    els += ring(cx, cz, T(-3.2), T(-1.2), 7.45, 7.65, 5, sq, ir, ins, vtop=V)
    els += ring(cx, cz, T(-1.2), T(0.2), 7.2, 7.55, 5, sq, ir, ins, vtop=V)
    els += ring(cx, cz, T(0.2), T(1.2), 6.6, 7.45, 5, sq, ir, ins, vtop=V)
    els += ring(cx, cz, T(1.2), T(1.7), 6.8, 7.6, 5, sq, ir, ins, vtop=V)
    els += ring(cx, cz, T(1.7), T(2.7), 7.05, 7.8, 5, sq, ir, ins, vtop=V)  # the thick rolled rim
    # ear lugs on the rim, west and east: a loop standing out of the rim
    for sx in (-1, 1):
        x0, x1 = sorted((cx + sx * 6.6, cx + sx * 8.2))
        els.append(box([x0, T(0.4), cz - 1.1], [x1, T(1.4), cz + 1.1], ir))
        els.append(box([x0, T(1.4), cz - 1.1], [x1, T(3.6), cz - 0.5], ir))
        els.append(box([x0, T(1.4), cz + 0.5], [x1, T(3.6), cz + 1.1], ir))
        els.append(box([x0, T(3.6), cz - 1.1], [x1, T(4.3), cz + 1.1], ir))
    # the bail: an upright from each lug leaning in, and a bar across
    L, ang = 6.0, 25
    for sx in (-1, 1):
        bx = cx + sx * 7.4
        els.append(box([bx - 0.4, T(3.4), cz - 0.4], [bx + 0.4, T(3.4 + L), cz + 0.4], ir,
                       rot={"origin": [bx, T(3.4), cz], "axis": "z", "angle": sx * ang}))
    bar_y = T(3.4 + L * math.cos(math.radians(ang)))
    reach = 7.4 - L * math.sin(math.radians(ang))
    els.append(box([cx - reach - 0.3, bar_y - 0.7, cz - 0.45], [cx + reach + 0.3, bar_y + 0.1, cz + 0.45], ir))
    # the chain of alternating iron links up to the tripod's hook
    y, n = bar_y - 0.3, 0
    while y < 15 - 0.5:
        h = min(2.4, 15 - y)
        if n % 2 == 0:
            els.append(box([cx - 0.6, y, cz - 0.05], [cx + 0.6, y + h, cz + 0.05], "#chain", skip=("up", "down", "east", "west"), uv=LINK))
        else:
            els.append(box([cx - 0.05, y, cz - 0.6], [cx + 0.05, y + h, cz + 0.6], "#chain", skip=("up", "down", "north", "south"), uv=LINK))
        y += 1.8
        n += 1
    # the tripod's bronze hook (part of the hanging models, as in gen_kettle.py)
    els.append(box([cx - 0.35, 13.6, cz - 0.35], [cx + 0.35, 15.2, cz + 0.35], "#bronze"))
    return els


# ---------------------------------------------------------------- Fermenting Barrel

BARREL_TEX = {"particle": B + "barrel_staves", "staves": B + "barrel_staves", "hoop": B + "barrel_hoop", "end": B + "barrel_end",
              "inside": B + "barrel_inside", "lid": B + "barrel_lid", "brine": B + "barrel_brine", "bottom": B + "barrel_lid"}


def barrel(open_):
    s, V = "#staves", (16, 0)
    els = []
    profile = [(0, 1, 5.5), (1, 2.5, 6.0), (2.5, 4.5, 6.35), (4.5, 7.5, 6.6), (7.5, 10.5, 6.6), (10.5, 12.5, 6.35)]
    for y0, y1, A_ in profile:
        els += disc(8, 8, y0, y1, A_, s, bottom="#bottom", vtop=V)
    # the top: the staves' ends standing up round the head (the chime), a brine surface or the lid inside
    els += ring(8, 8, 12.5, 14.0, 6.0, 6.0, 4.9, 4.9, s, "#inside", top="#end", vtop=V)
    els += ring(8, 8, 14.0, 15.0, 5.6, 5.6, 4.9, 4.9, s, "#inside", top="#end", vtop=V)
    els += disc(8, 8, 12.5, 13.6 if open_ else 12.7, 5.0, s, top="#brine" if open_ else "#inside")  # opened: brine up near the top
    for e in els[-4:]:
        e["faces"]["up"]["uv"] = [r2(e["from"][0]), r2(e["from"][2]), r2(e["to"][0]), r2(e["to"][2])]
    # three iron hoops
    for y0, y1, A_ in ((1.4, 2.4, 6.25), (6.3, 7.2, 6.85), (11.2, 12.2, 6.5)):
        els += disc(8, 8, y0, y1, A_, "#hoop")
    if not open_:
        # the head: boards across, square with its corners cut, sitting in the chime's croze (the dark corners)
        for frm, to in (([3.15, 13.6, 4.4], [12.85, 14.4, 11.6]), ([4.4, 13.6, 3.15], [11.6, 14.4, 12.85])):
            els.append(box(frm, to, faces_tex(s, up="#lid"), uv={"up": [frm[0], frm[2], to[0], to[2]]}))
        els.append(box([5.4, 14.4, 7.3], [10.6, 15.3, 8.7], faces_tex(s, east="#end", west="#end")))  # the handle batten
        els.append(box([5.0, 14.4, 7.1], [6.0, 14.8, 8.9], "#hoop"))
        els.append(box([10.0, 14.4, 7.1], [11.0, 14.8, 8.9], "#hoop"))
    else:
        els += leaning_lid()
    return els


def leaning_lid():
    """The lid lifted off and stood on its edge against the barrel's south side (the side a player facing north sees),
    tipped back 5 degrees onto the staves: upright boards making a round lid, the handle batten on the outside."""
    t, zf = 0.8, 15.45  # thickness, the inner face's z at the foot
    rot = {"origin": [8, 0, zf + t], "axis": "x", "angle": -5}
    els = []
    edges = (-4.7, -3.4, -2.0, -0.7, 0.7, 2.0, 3.4, 4.7)
    for xa, xb in zip(edges, edges[1:]):
        far = max(abs(xa), abs(xb)) - 0.45
        half = max(math.sqrt(max(4.85 ** 2 - far ** 2, 0)), 1.6)
        els.append(box([8 + xa, 4.85 - half, zf], [8 + xb, 4.85 + half, zf + t],
                       faces_tex("#staves", north="#lid", south="#lid", up="#end", down="#end"), rot=dict(rot)))
    els.append(box([8 - 2.6, 4.2, zf + t], [8 + 2.6, 5.5, zf + t + 0.7], "#staves", rot=dict(rot)))
    return els


# ---------------------------------------------------------------- Cheese Press

PRESS_TEX = {"particle": B + "press_wood", "wood": B + "press_wood", "post": B + "press_post", "end": B + "press_wood_end", "screw": B + "press_screw",
             "hoop": B + "press_hoop", "board": B + "press_board", "cheese": B + "press_cheese", "cord": B + "tripod_lashing",
             "inside": B + "press_hoop_inside"}


def cheese_press(pressing):
    wd, en = "#wood", "#end"
    endy = faces_tex(wd, east=en, west=en)
    els = []
    # the sill: a heavy beam across, under the posts and the board
    els.append(box([0, 0, 6.2], [16, 2, 9.8], endy))
    els.append(box([1, 0, 2.5], [3.5, 1.2, 13.5], faces_tex(wd, north=en, south=en)))  # cross feet
    els.append(box([12.5, 0, 2.5], [15, 1.2, 13.5], faces_tex(wd, north=en, south=en)))
    # the posts and the top beam (the screw runs through it)
    els.append(box([1.2, 2, 6.7], [3.4, 13.2, 9.3], faces_tex("#post", up=en, down=en)))
    els.append(box([12.6, 2, 6.7], [14.8, 13.2, 9.3], faces_tex("#post", up=en, down=en)))
    els.append(box([0.4, 13.2, 6.3], [15.6, 15.6, 9.7], endy))
    for x in (2.3, 13.7):  # wedges through the post tenons above the beam
        els.append(box([x - 0.6, 15.6, 7.4], [x + 0.6, 16.4, 8.6], en))
    # the grooved board with its whey spout to the south
    board = box([3.6, 2, 2.4], [12.4, 3, 13.6], faces_tex(wd, up="#board"))
    board["faces"]["up"]["uv"] = [3.6, 2.4, 12.4, 13.6]
    els.append(board)
    els.append(box([7.2, 2.1, 13.6], [8.8, 2.8, 15.6], faces_tex(wd, up="#board"), uv={"up": [7.2, 13.6, 8.8, 15.6]}))
    # the slatted hoop (mould) and its withy band
    els += ring(8, 8, 3, 7.4, 3.9, 3.9, 3.25, 3.25, "#hoop", "#inside", top=en, vtop=(8, 4))
    els += ring(8, 8, 4.6, 5.5, 4.15, 4.15, 3.6, 3.6, "#cord", "#cord")
    # cheese, follower and screw
    if pressing:
        els += disc(8, 8, 3, 4.9, 3.25, "#cheese")
        foll_y = 4.9
    else:
        foll_y = 8.6
    els += disc(8, 8, foll_y, foll_y + 0.9, 3.15, wd, top=en, bottom=en)
    els.append(box([7, foll_y + 0.9, 7], [9, foll_y + 1.6, 9], en))  # the screw's socket on the follower
    screw_top = foll_y + 1.6 + 12.2
    els += disc(8, 8, foll_y + 1.6, screw_top, 0.95, "#screw", top=en, vtop=(screw_top, 0))
    els.append(box([6.9, screw_top, 6.9], [9.1, screw_top + 1.4, 9.1], faces_tex(wd, up=en, down=en)))  # the head
    els.append(box([2.2, screw_top + 0.3, 7.55], [13.8, screw_top + 1.1, 8.45], endy))  # the turning bar
    return els


# ---------------------------------------------------------------- Salt Pan

SALT_TEX = {"particle": B + "salt_pan_clay", "clay": B + "salt_pan_clay", "rim": B + "salt_pan_rim", "floor": B + "salt_pan_floor"}
BRINE_Y = {1: 2.5, 2: 2.0, 3: 1.5}


def salt_pan(stage):
    c, rim = "#clay", "#rim"
    els = []
    floor = box([0, 0, 0], [16, 1, 16], faces_tex(c, up="#floor"))
    els.append(floor)
    walls = [([0, 1, 0], [16, 3, 1.5], "south"), ([0, 1, 14.5], [16, 3, 16], "north"),
             ([0, 1, 1.5], [1.5, 3, 14.5], "east"), ([14.5, 1, 1.5], [16, 3, 14.5], "west")]
    for f, t, inner in walls:
        els.append(box(f, t, faces_tex(c, up=rim, **{inner: "#wall"}), vtop=(3, 13)))
    # a rounded lip along the top, a little inside the outer edge
    for f, t in (([0.5, 3, 0.5], [15.5, 3.4, 1.2]), ([0.5, 3, 14.8], [15.5, 3.4, 15.5]),
                 ([0.5, 3, 1.2], [1.2, 3.4, 14.8]), ([14.8, 3, 1.2], [15.5, 3.4, 14.8])):
        els.append(box(f, t, rim))
    if stage in BRINE_Y:
        y = BRINE_Y[stage]
        els.append({"from": [1.5, y, 1.5], "to": [14.5, y, 14.5], "faces": {"up": {"uv": [1.5, 1.5, 14.5, 14.5], "texture": "#brine"}}})
    elif stage == 4:
        els.append(box([1.5, 1, 1.5], [14.5, 1.5, 14.5], faces_tex("#salt"), uv={"up": [1.5, 1.5, 14.5, 14.5]}))
    return els


def salt_tex(stage):
    t = dict(SALT_TEX)
    t["wall"] = B + (f"salt_pan_wall_{stage}" if stage else "salt_pan_clay")
    if stage in BRINE_Y:
        t["brine"] = B + f"salt_pan_brine_{stage}"
    if stage == 4:
        t["salt"] = B + "salt_pan_salt"
    return t


# ---------------------------------------------------------------- Steel Age (phase 6): Steel Pan, Mortar and Pestle, Sieve

STEEL_PAN_TEX = {"particle": B + "steel_pan_steel", "steel": B + "steel_pan_steel", "floor": B + "steel_pan_floor", "trivet": B + "steel_pan_trivet"}


def steel_pan():
    """A wide, shallow spun-steel pan with two loop handles (east and west), on a forged trivet ring with three legs down
    beside the campfire's logs. Floor top y 1 (CookwareRenderer.SURFACE), walls to y 2.6, lip to 3.1."""
    st, fl, tv = "#steel", "#floor", "#trivet"
    els = []
    # the trivet: a ring under the pan and three splayed legs to the ground
    els += ring(8, 8, -1.0, -0.4, 6.2, 6.6, 5.4, 5.8, tv, tv)
    for k in range(3):
        a = math.radians(90 + 120 * k)
        x, z = 8 + 5.8 * math.cos(a), 8 + 5.8 * math.sin(a)
        axis, ang = ("x", 8 if math.sin(a) < 0 else -8) if abs(math.sin(a)) > 0.5 else ("z", -8 if math.cos(a) < 0 else 8)
        els.append(box([x - 0.5, -16, z - 0.5], [x + 0.5, -0.4, z + 0.5], tv, vtop=(0, 0), rot={"origin": [x, -0.4, z], "axis": axis, "angle": ang}))
    # the pan: a flat floor, low flaring walls, a rolled lip
    els += disc(8, 8, -0.4, 0.3, 5.9, st, Ad=6.4, vtop=(3.1, 0))
    els += disc(8, 8, 0.3, 1.0, 6.6, st, top=fl, Ad=7.1, vtop=(3.1, 0))
    for e in els[-4:]:
        e["faces"]["up"]["uv"] = [r2(e["from"][0]), r2(e["from"][2]), r2(e["to"][0]), r2(e["to"][2])]
    els += ring(8, 8, 1.0, 2.0, 6.85, 7.35, 6.15, 6.65, st, st, vtop=(3.1, 0))
    els += ring(8, 8, 2.0, 2.6, 7.05, 7.55, 6.35, 6.85, st, st, vtop=(3.1, 0))
    els += ring(8, 8, 2.6, 3.1, 7.3, 7.8, 6.45, 6.95, st, st, vtop=(3.1, 0))
    # loop handles riveted on the east and west walls
    for sx in (-1, 1):
        x0, x1 = sorted((8 + sx * 7.2, 8 + sx * 9.0))
        els.append(box([x0, 1.6, 6.2], [x1, 2.2, 6.9], st))
        els.append(box([x0, 1.6, 9.1], [x1, 2.2, 9.8], st))
        xo = (x1 - 0.7, x1) if sx > 0 else (x0, x0 + 0.7)
        els.append(box([xo[0], 1.6, 6.9], [xo[1], 2.2, 9.1], st))
        xr = 8 + sx * 7.15
        for z in (6.55, 9.45):
            els.append(box([xr - 0.3, 1.5, z - 0.3], [xr + 0.3, 2.3, z + 0.3], tv))  # rivet heads
    return els


MORTAR_TEX = {"particle": B + "mortar_marble", "marble": B + "mortar_marble", "rim": B + "mortar_rim", "inside": B + "mortar_inside",
              "pestle": B + "pestle_steel", "wood": B + "pestle_wood"}


def mortar():
    """A turned marble mortar (inner floor y 3.5, CookwareRenderer) with a steel pestle leaning in it toward the north
    (the blockstate turns it a quarter round per click)."""
    m, ins, V = "#marble", "#inside", (6.8, 2)
    els = []
    els += disc(8, 8, 0, 0.8, 3.7, m, vtop=V)                     # the foot
    els += disc(8, 8, 0.8, 1.6, 4.2, m, vtop=V)
    els += disc(8, 8, 1.6, 3.5, 4.6, m, top=ins, vtop=V)           # solid under the floor
    for e in els[-4:]:
        e["faces"]["up"]["uv"] = [r2(e["from"][0]), r2(e["from"][2]), r2(e["to"][0]), r2(e["to"][2])]
    els += ring(8, 8, 3.5, 4.8, 4.85, 5.0, 3.0, 3.6, m, ins, vtop=V)
    els += ring(8, 8, 4.8, 6.0, 4.95, 5.1, 3.3, 3.9, m, ins, vtop=V)
    els += ring(8, 8, 6.0, 6.8, 5.15, 5.35, 3.5, 4.2, m, ins, top="#rim", vtop=V)  # the thick lip
    for sx in (-1, 1):  # two lugs to grip it by
        x0, x1 = sorted((8 + sx * 4.8, 8 + sx * 5.8))
        els.append(box([x0, 4.2, 7.1], [x1, 5.6, 8.9], m))
    # the pestle: a bulb in the bowl, a steel shaft leaning out over the north lip, a turned wooden knob
    rot = {"origin": [8, 3.6, 9.2], "axis": "x", "angle": 28}
    els.append(box([7.0, 3.6, 8.2], [9.0, 5.4, 10.2], "#pestle", rot=dict(rot)))
    els.append(box([7.35, 5.4, 8.55], [8.65, 12.5, 9.85], "#pestle", rot=dict(rot), vtop=(12.5, 0)))
    els.append(box([7.1, 12.5, 8.3], [8.9, 14.3, 10.1], faces_tex("#wood"), rot=dict(rot)))
    return els


SIEVE_TEX = {"particle": B + "sieve_hoop", "hoop": B + "sieve_hoop", "mesh": B + "sieve_mesh", "floor": B + "sieve_floor",
             "tub": B + "barrel_staves", "band": B + "barrel_hoop", "cord": B + "tripod_lashing"}


def sieve():
    """A round riddle of bent ash with a woven steel mesh (y 3.05..3.25, items lie on it), resting on a low oak tub that
    catches what falls through."""
    els = []
    # the tub: a bottom, stave walls, an iron band, flour on its floor
    els += disc(8, 8, 0, 0.8, 6.0, "#tub", top="#floor", Ad=6.4, vtop=(3, 13))
    for e in els[-4:]:
        e["faces"]["up"]["uv"] = [r2(e["from"][0]), r2(e["from"][2]), r2(e["to"][0]), r2(e["to"][2])]
    els += ring(8, 8, 0.8, 3.0, 6.2, 6.6, 5.4, 5.8, "#tub", "#tub", vtop=(3, 13))
    els += ring(8, 8, 1.4, 2.2, 6.35, 6.75, 6.0, 6.4, "#band", "#band")
    # the sieve: mesh, the bent hoop round it, a withy lashing at the scarf
    # the mesh as axis-aligned steps filling the octagon, uvs by position so the overlaps match and the weave runs straight
    for hx, hz in ((3.26, 6.5), (4.1, 5.66), (4.88, 4.88), (5.66, 4.1), (6.5, 3.26)):
        els.append(box([8 - hx, 3.05, 8 - hz], [8 + hx, 3.25, 8 + hz], "#mesh", skip=SIDES,
                       uv={f: [r2(8 - hx), r2(8 - hz), r2(8 + hx), r2(8 + hz)] for f in ("up", "down")}))
    els += ring(8, 8, 2.6, 5.8, 7.0, 7.5, 6.4, 6.85, "#hoop", "#hoop", vtop=(5.8, 4))
    els += ring(8, 8, 3.8, 4.6, 7.15, 7.65, 6.9, 7.4, "#cord", "#cord")
    return els


# ---------------------------------------------------------------- items

BLOCK_DISPLAY = {  # minecraft:block/block's
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def item_3d(name, textures, els, **extra):
    """An item model of the block's geometry moved so its bounding box sits in the middle of the 16 px cube, and scaled
    down in every display when it is bigger than the cube."""
    lo = [min(min(e["from"][i], e["to"][i]) for e in els) for i in range(3)]
    hi = [max(max(e["from"][i], e["to"][i]) for e in els) for i in range(3)]
    moved = shift(els, *(8 - (lo[i] + hi[i]) / 2 for i in range(3)))
    k = min(1.0, 15.0 / max(hi[i] - lo[i] for i in range(3)))
    display = {d: {**v, "scale": [r2(s * k) for s in v["scale"]]} for d, v in BLOCK_DISPLAY.items()}
    w(A / f"models/item/{name}.json", {"parent": "minecraft:block/block", **extra, "display": display, "textures": textures, "elements": moved})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{name}"}})


def flat_item(name, parent="minecraft:item/generated"):
    w(A / f"models/item/{name}.json", {"parent": parent, "textures": {"layer0": M + f"item/{name}"}})
    w(A / f"items/{name}.json", {"model": {"type": "minecraft:model", "model": M + f"item/{name}"}})


# ---------------------------------------------------------------- tripod blockstate

def patch_tripod(A_=A):
    """Adds the Iron Cauldron to gen_kettle.py's tripod blockstate: kettle parts get iron=false, and for the Fire Pit parts
    (pit 1-4 turned like the kettle, pit 5 the big pit) iron=true shows the cauldron instead. Idempotent."""
    path = A_ / "blockstates/tripod.json"
    bs = json.loads(path.read_text(encoding="utf-8"))
    parts = [p for p in bs["multipart"] if p.get("when", {}).get("iron") != "true"]
    new = []
    for p in parts:
        when = p.get("when", {})
        if when.get("kettle") == "true":
            when["iron"] = "false"
            if when.get("pit") != "0":
                model = p["apply"]["model"].replace("kettle_pit", "iron_cauldron_pit").replace("kettle_big", "iron_cauldron_big")
                new.append({"when": {**when, "iron": "true"}, "apply": {**p["apply"], "model": model}})
    bs["multipart"] = parts + new
    w(path, bs)


def main():
    pot = clay_pot()
    w(A / "models/block/clay_cooking_pot.json", {"textures": POT_TEX, "elements": pot})
    w(A / "blockstates/clay_cooking_pot.json", {"variants": {"": {"model": B + "clay_cooking_pot"}}})
    item_3d("clay_cooking_pot", POT_TEX, pot)

    g = griddle()
    w(A / "models/block/bronze_griddle.json", {"textures": GRIDDLE_TEX, "elements": g})
    w(A / "blockstates/bronze_griddle.json", {"variants": {f"facing={f}": {"model": B + "bronze_griddle", **({"y": y} if y else {})}
                                                           for f, y in FACINGS.items()}})
    item_3d("bronze_griddle", GRIDDLE_TEX, g)

    s = skillet()
    w(A / "models/block/iron_skillet.json", {"textures": SKILLET_TEX, "elements": s})
    w(A / "blockstates/iron_skillet.json", {"variants": {f"facing={f}": {"model": B + "iron_skillet", **({"y": y} if y else {})}
                                                         for f, y in FACINGS.items()}})
    item_3d("iron_skillet", SKILLET_TEX, s)

    w(A / "models/block/iron_cauldron_pit.json", {"textures": CAULDRON_TEX, "elements": cauldron(16, 16, PIT_LIFT)})
    w(A / "models/block/iron_cauldron_big.json", {"textures": CAULDRON_TEX, "elements": scaled(cauldron(8, 8, PIT_LIFT))})
    patch_tripod(A)

    for o in (False, True):
        w(A / f"models/block/fermenting_barrel{'_open' if o else ''}.json", {"textures": BARREL_TEX, "elements": barrel(o)})
    w(A / "blockstates/fermenting_barrel.json", {"variants": {f"open={str(o).lower()}": {"model": B + "fermenting_barrel" + ("_open" if o else "")}
                                                              for o in (False, True)}})
    item_3d("fermenting_barrel", BARREL_TEX, barrel(False))

    for p in (False, True):
        w(A / f"models/block/cheese_press{'_pressing' if p else ''}.json", {"textures": PRESS_TEX, "elements": cheese_press(p)})
    w(A / "blockstates/cheese_press.json", {"variants": {f"pressing={str(p).lower()}": {"model": B + "cheese_press" + ("_pressing" if p else "")}
                                                         for p in (False, True)}})
    item_3d("cheese_press", PRESS_TEX, cheese_press(False))

    for st in range(5):
        w(A / f"models/block/salt_pan_{st}.json", {"textures": salt_tex(st), "elements": salt_pan(st)})
    w(A / "blockstates/salt_pan.json", {"variants": {f"stage={st}": {"model": B + f"salt_pan_{st}"} for st in range(5)}})
    item_3d("salt_pan", salt_tex(0), salt_pan(0))

    for name in FLAT:
        flat_item(name)
    flat_item("bronze_ladle", "minecraft:item/handheld")
    flat_item("steel_knife_set", "minecraft:item/handheld")

    sp = steel_pan()
    w(A / "models/block/steel_pan.json", {"textures": STEEL_PAN_TEX, "elements": sp})
    w(A / "blockstates/steel_pan.json", {"variants": {f"facing={f}": {"model": B + "steel_pan", **({"y": y} if y else {})} for f, y in FACINGS.items()}})
    item_3d("steel_pan", STEEL_PAN_TEX, sp)
    mo = mortar()
    w(A / "models/block/mortar_and_pestle.json", {"textures": MORTAR_TEX, "elements": mo})
    w(A / "blockstates/mortar_and_pestle.json", {"variants": {f"facing={f}": {"model": B + "mortar_and_pestle", **({"y": y} if y else {})}
                                                              for f, y in FACINGS.items()}})
    item_3d("mortar_and_pestle", MORTAR_TEX, mo)
    sv = sieve()
    w(A / "models/block/sieve.json", {"render_type": "minecraft:cutout", "textures": SIEVE_TEX, "elements": sv})
    w(A / "blockstates/sieve.json", {"variants": {"": {"model": B + "sieve"}}})
    item_3d("sieve", SIEVE_TEX, sv, render_type="minecraft:cutout")
    print("cookware models ok")


if __name__ == "__main__":
    main()
