"""Firewood Piles and unique dried meats (FirewoodPileBlock, FirewoodItem, Preserving.DRYING, DryingRackBlock, RackFood).

- Firewood Piles, one block per wood (<wood>_firewood_pile, no item: it drops its firewood): layers 1-8, two billets each,
  four billets a row (4 px square, 16 long, a hair of gap between them), four rows a block; axis x/z is the billets' run.
  Models: firewood_pile_<n> (parents, textures #end / #side) and <wood>_firewood_pile_<n>; blockstates; loot (n firewood);
  mineable/axe; lang. One layer alone (firewood_pile_piece_<k>, <wood>_firewood_pile_piece_<k>) for mixed piles
- Rough Log Pile (RoughLogPileBlock, one block, woods mixed per log): rough_log_piece_<i> + <wood>_rough_log_piece_<i>
  (the wood's Fallen Log textures), plain oak rough_log_pile_<n>, blockstate, mineable/axe, lang (no loot table: it drops
  its logs from code)
- What dries into what: item tags mechanical:dryable_meat/<product> (one per family; mechanical:dryable_meat holds them all),
  mechanical:not_dryable (big cuts: too thick; fat-rich meat: goes rancid); anything else smokable dries into plain Jerky /
  Dried Fish (Raw Meat Strips cut from a big cut, and meat added later without a family). mechanical:dried_meat (all of them,
  for the advancement). Shelf lives, rots_to_flesh, Raw Meat Strips in smokable_meat
- items: the nine products, Raw Meat Strips, Smoked Haunch / Ribs / Loin (models, lang); knife recipes: a big cut into
  strips, a smoked cut into Smoked Meat (as many as its meat units)
- Drying Rack: the fowl look (hide 24/25, hide2 14/15): models for the single rack and both tall spots, blockstate parts,
  loot (Dried Fowl); messages for meat that won't go on
Textures: scripts/gen_woodmeat_textures.py. Run after gen_tribes2.py: python scripts/gen_woodmeat.py
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
TAGS = D / "mechanical/tags/item"
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "pale_oak", "poplar",
         "aspen", "alder", "beech", "linden", "willow", "ash", "hazel", "pine", "yew"]


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def read(path):
    return json.loads(path.read_text(encoding="utf-8"))


def merge(path, obj):
    old = read(path) if path.exists() else {}
    old.update(obj)
    w(path, old)


def tag_add(path, *values):
    old = read(path) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


lang = {}

# ---------------------------------------------------------------- firewood piles
FACES = ("north", "south", "east", "west", "up", "down")


def billet(c, r):
    """Billet in column c (x), row r (y) of a pile built along z: end grain on north/south, bark or split wood on the rest."""
    x0, x1, y0, y1 = c * 4 + 0.2, c * 4 + 3.8, r * 4, r * 4 + 4
    tile = ((c + 2 * r) % 4) * 4, ((r + c // 2) % 4) * 4  # which end in the end sheet: varied, never two alike side by side
    band = ((c * 3 + r) % 4) * 4  # which band of the side sheet
    end = {"uv": [tile[0], tile[1], tile[0] + 4, tile[1] + 4], "texture": "#end"}
    side = {"uv": [band, 0, band + 4, 16], "texture": "#side"}
    faces = {"north": {**end, "cullface": "north"}, "south": {**end, "cullface": "south"},
             "east": {**side, "rotation": 90}, "west": {**side, "rotation": 90},
             "up": dict(side), "down": dict(side)}
    if r % 2:  # every other row lies crosswise, like a real stack: run along x, end grain east/west
        faces = {"east": {**end, "cullface": "east"}, "west": {**end, "cullface": "west"},
                 "north": {**side, "rotation": 90}, "south": {**side, "rotation": 90},
                 "up": {**side, "rotation": 90}, "down": {**side, "rotation": 90}}
    if y1 == 16:
        faces["up"]["cullface"] = "up"
    if y0 == 0:
        faces["down"]["cullface"] = "down"
    if r % 2:
        return {"from": [0, y0, x0], "to": [16, y1, x1], "faces": faces}
    return {"from": [x0, y0, 0], "to": [x1, y1, 16], "faces": faces}


for n in range(1, 9):
    w(A / f"models/block/firewood_pile_{n}.json", {"parent": "minecraft:block/block", "textures": {"particle": "#side"},
                                                   "elements": [billet(i % 4, i // 4) for i in range(2 * n)]})
for wood in WOODS:
    name = f"{wood}_firewood_pile"
    for n in range(1, 9):
        w(A / f"models/block/{name}_{n}.json", {"parent": f"mechanical:block/firewood_pile_{n}", "textures": {
            "end": f"mechanical:block/{name}_end", "side": f"mechanical:block/{name}_side"}})
    w(A / f"blockstates/{name}.json", {"variants": {
        f"axis={a},layers={n}": {"model": f"mechanical:block/{name}_{n}", **({"y": 90} if a == "x" else {})}
        for a in ("x", "z") for n in range(1, 9)}})
    w(D / f"mechanical/loot_table/blocks/{name}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
        {"type": "minecraft:item", "name": f"{M}{wood}_firewood",
         "condition": {"type": "minecraft:match_block", "blocks": M + name, "state": {"layers": str(n)}},
         **({"modifier": {"type": "minecraft:set_count", "count": n}} if n > 1 else {})} for n in range(1, 9)]}],
        "random_sequence": f"mechanical:blocks/{name}"})
    lang[f"block.mechanical.{name}"] = wood.replace("_", " ").title() + " Firewood Pile"
# one layer (two billets) alone, for mixed piles drawn layer by layer (client/WoodPileModels; turned y:90 there for axis x)
for k in range(1, 9):
    w(A / f"models/block/firewood_pile_piece_{k}.json", {"parent": "minecraft:block/block", "textures": {"particle": "#side"},
                                                         "elements": [billet(i % 4, i // 4) for i in (2 * k - 2, 2 * k - 1)]})
    for wood in WOODS:
        name = f"{wood}_firewood_pile"
        w(A / f"models/block/{wood}_firewood_pile_piece_{k}.json", {"parent": f"mechanical:block/firewood_pile_piece_{k}", "textures": {
            "end": f"mechanical:block/{name}_end", "side": f"mechanical:block/{name}_side"}})
lang["message.mechanical.firewood_pile.too_high"] = "The pile is as high as it goes"
tag_add(D / "minecraft/tags/block/mineable/axe.json", *[f"{M}{wood}_firewood_pile" for wood in WOODS])

# ---------------------------------------------------------------- rough log pile (RoughLogPileBlock)
# Four logs a block: log i in column i % 2 (x 0-8 / 8-16) and row i // 2 up, 8 px thick and 16 long along z, the corners
# taken off so they read round: a tall box and a wide one. Bark (#side, grain along the log) and end grain (#end, the
# whole log top squeezed onto the 8 px end, rings to the bark). Textures per wood: those of its Fallen Log.


def rough_log(i):
    c, r = i % 2 * 8, i // 2 * 8
    out = []
    for x0, x1, y0, y1 in ((c + 1, c + 7, r, r + 8), (c, c + 8, r + 1, r + 7)):
        u0, u1, v0, v1 = (x0 - c) * 2, (x1 - c) * 2, (y0 - r) * 2, (y1 - r) * 2
        end = {"uv": [u0, v0, u1, v1], "texture": "#end"}
        faces = {"north": {**end, "cullface": "north"}, "south": {**end, "cullface": "south"},
                 "east": {"uv": [0, 0, y1 - y0, 16], "texture": "#side", "rotation": 90},
                 "west": {"uv": [0, 0, y1 - y0, 16], "texture": "#side", "rotation": 90},
                 "up": {"uv": [8 - (x1 - x0) / 2, 0, 8 + (x1 - x0) / 2, 16], "texture": "#side"},
                 "down": {"uv": [8 - (x1 - x0) / 2, 0, 8 + (x1 - x0) / 2, 16], "texture": "#side"}}
        if x1 == 16: faces["east"]["cullface"] = "east"
        if x0 == 0: faces["west"]["cullface"] = "west"
        if y1 == 16: faces["up"]["cullface"] = "up"
        if y0 == 0: faces["down"]["cullface"] = "down"
        out.append({"from": [x0, y0, 0], "to": [x1, y1, 16], "faces": faces})
    return out


def log_textures(wood):
    t = read(A / f"models/block/{wood}_fallen_log_0.json")["textures"]
    return {"particle": t["side"], "side": t["side"], "end": t["end"]}


for i in range(4):
    w(A / f"models/block/rough_log_piece_{i}.json", {"parent": "minecraft:block/block", "textures": {"particle": "#side"},
                                                     "elements": rough_log(i)})
    for wood in WOODS:
        w(A / f"models/block/{wood}_rough_log_piece_{i}.json", {"parent": f"mechanical:block/rough_log_piece_{i}",
                                                                 "textures": log_textures(wood)})
# the plain models (oak): what shows before the woods arrive, and the break particles
for n in range(1, 5):
    w(A / f"models/block/rough_log_pile_{n}.json", {"parent": "minecraft:block/block", "textures": log_textures("oak"),
                                                    "elements": [e for i in range(n) for e in rough_log(i)]})
w(A / "blockstates/rough_log_pile.json", {"variants": {
    f"axis={a},logs={n}": {"model": f"mechanical:block/rough_log_pile_{n}", **({"y": 90} if a == "x" else {})}
    for a in ("x", "z") for n in range(1, 5)}})
tag_add(D / "minecraft/tags/block/mineable/axe.json", M + "rough_log_pile")
lang |= {"block.mechanical.rough_log_pile": "Rough Log Pile",
         "message.mechanical.rough_log_pile.too_high": "No room: the pile is as high as it goes",
         "message.mechanical.rough_log_pile.rolled": "The log rolls off the pile",
         "message.mechanical.rough_log_pile.fell_off": "The log rolls off and falls"}

# ---------------------------------------------------------------- what each meat dries into
FAMILIES = {
    "venison_jerky": ["venison_scraps", "saiga_meat"],
    "beef_jerky": ["minecraft:beef", "beef_scraps", "raw_bison", "bison_tongue", "buffalo_meat", "raw_musk_ox"],
    "dried_pork": ["minecraft:porkchop", "porkchop_scraps", "tapir_meat"],
    "dried_mutton": ["minecraft:mutton", "mutton_scraps", "ibex_meat", "chamois_meat"],
    "game_jerky": ["game_meat_scraps", "raw_mammoth", "raw_rhino", "onager_meat", "hyena_meat", "leopard_meat", "raw_lynx",
                   "raw_wolverine", "raw_wildcat", "raw_polar_bear", "raw_snow_leopard"],
    "small_game_jerky": ["minecraft:rabbit", "rabbit_scraps", "raw_ermine", "raw_arctic_fox", "raw_lemming", "raw_marten", "raw_mole", "raw_vole",
                         "marmot_meat", "raw_otter", "raw_snake", "lizard_tail", "tortoise_meat"],
    "dried_fowl": ["minecraft:chicken", "chicken_scraps", "raw_goose", "raw_pheasant", "raw_ptarmigan", "raw_pigeon",
                   "raw_capercaillie", "raw_bustard", "raw_hornbill", "raw_jungle_fowl", "raw_peafowl", "raw_mallard",
                   "raw_swan", "raw_heron", "raw_crane"],
    "dried_seal_meat": ["raw_seal_meat"],
    "stockfish": ["minecraft:cod", "raw_pike", "raw_sturgeon"],
}
BIG_CUTS = ["raw_haunch", "raw_ribs", "raw_loin"]
FATTY = ["cave_bear_meat", "raw_beaver", "beaver_tail", "raw_walrus_meat"]
mid = lambda i: i if ":" in i else M + i

for product, meats in FAMILIES.items():
    w(TAGS / f"dryable_meat/{product}.json", {"values": [mid(m) for m in meats]})
w(TAGS / "dryable_meat.json", {"values": [f"#{M}dryable_meat/{p}" for p in FAMILIES]})
w(TAGS / "not_dryable.json", {"values": [mid(m) for m in BIG_CUTS + FATTY]})
# every meat and fish the racks take has exactly one rule; strips and salmon fall back to Jerky / Dried Fish on purpose
smokable = read(TAGS / "smokable_meat.json")["values"] + read(TAGS / "smokable_fish.json")["values"]
ruled = [mid(m) for ms in FAMILIES.values() for m in ms] + [mid(m) for m in BIG_CUTS + FATTY]
assert len(ruled) == len(set(ruled)), "a meat is in two families"
assert set(ruled) <= set(smokable), set(ruled) - set(smokable)
for m in sorted(set(smokable) - set(ruled) - {"minecraft:salmon", M + "raw_meat_strips"}):
    print("woodmeat: no drying family (dries into plain Jerky / Dried Fish):", m)

PRODUCTS = {"venison_jerky": "Venison Jerky", "beef_jerky": "Beef Jerky", "dried_pork": "Dried Pork", "dried_mutton": "Dried Mutton",
            "game_jerky": "Game Jerky", "small_game_jerky": "Small Game Jerky", "dried_fowl": "Dried Fowl",
            "dried_seal_meat": "Dried Seal Meat", "stockfish": "Stockfish"}
assert set(PRODUCTS) == set(FAMILIES)
OTHER = {"raw_meat_strips": "Raw Meat Strips", "smoked_haunch": "Smoked Haunch", "smoked_ribs": "Smoked Ribs", "smoked_loin": "Smoked Loin"}
for item, name in {**PRODUCTS, **OTHER}.items():
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    lang[f"item.mechanical.{item}"] = name
w(TAGS / "dried_meat.json", {"values": [M + "jerky", M + "dried_fish"] + [M + p for p in PRODUCTS]})
# shelf life: lean dried meat 30 days, fat-streaked pork and seal 20, stockfish dried bone-hard keeps; strips spoil like raw meat
LONG = [M + p for p in PRODUCTS if p not in ("dried_pork", "dried_seal_meat", "stockfish")] + [M + "smoked_" + c for c in ("haunch", "ribs", "loin")]
tag_add(TAGS / "perishes_30_days.json", *LONG)
tag_add(TAGS / "perishes_20_days.json", M + "dried_pork", M + "dried_seal_meat")
tag_add(TAGS / "perishes_1_day.json", M + "raw_meat_strips")
tag_add(TAGS / "rots_to_flesh.json", *LONG, M + "dried_pork", M + "dried_seal_meat", M + "raw_meat_strips")
tag_add(TAGS / "smokable_meat.json", M + "raw_meat_strips")

# a knife: big cut -> strips for the rack, whole smoked cut -> Smoked Meat (its meat units: CarcassHauling.smokedPieces)
UNITS = {"haunch": 3, "ribs": 2, "loin": 2}
for cut, n in UNITS.items():
    w(D / f"mechanical/recipe/raw_meat_strips_from_raw_{cut}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [f"{M}raw_{cut}", "#mechanical:knives"], "result": {"id": M + "raw_meat_strips", "count": n}})
    w(D / f"mechanical/recipe/smoked_meat_from_smoked_{cut}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [f"{M}smoked_{cut}", "#mechanical:knives"], "result": {"id": M + "smoked_meat", "count": n}})

# ---------------------------------------------------------------- Drying Rack: the fowl look
FOWL = {24: "fowl", 25: "dried_fowl"}  # hide value -> texture; hide2 stores them as 14 / 15
rack0 = read(A / "models/block/drying_rack_0.json")
plane = {"from": [3, 4, 8], "to": [13, 13, 8], "faces": {"north": {"uv": [3, 3, 13, 12], "texture": "#food"},
                                                       "south": {"uv": [13, 3, 3, 12], "texture": "#food"}}}
for stage, tex in FOWL.items():
    w(A / f"models/block/drying_rack_{stage}.json", {"parent": "minecraft:block/block",
        "textures": {**rack0["textures"], "food": f"mechanical:block/drying_rack_{tex}"}, "elements": rack0["elements"] + [plane]})
    for side, x0 in (("left", 3), ("right", 8)):
        w(A / f"models/block/drying_rack_{side}_{tex}.json", {"textures": {"particle": "mechanical:block/drying_rack",
            "spot": f"mechanical:block/drying_rack_narrow_{tex}"}, "elements": [{"from": [x0, 4, 8], "to": [x0 + 5, 13, 8], "faces": {
                "north": {"uv": [0, 3, 5, 12], "texture": "#spot"}, "south": {"uv": [5, 3, 0, 12], "texture": "#spot"}}}]})
ROT = [("north", 0), ("east", 90), ("south", 180), ("west", 270)]
rot = lambda y: {"y": y} if y else {}
bs_path = A / "blockstates/drying_rack.json"
bs = read(bs_path)
parts = [p for p in bs["multipart"] if not p["apply"]["model"].endswith(("_fowl", "rack_24", "rack_25"))]
parts += [{"when": {"facing": f, "part": "single", "hide": str(h)}, "apply": {"model": f"mechanical:block/drying_rack_{h}", **rot(y)}}
          for f, y in ROT for h in FOWL]
parts += [{"when": {"facing": f, "part": "lower|upper", prop: str(h - shift)}, "apply": {"model": f"mechanical:block/drying_rack_{side}_{tex}", **rot(y)}}
          for f, y in ROT for prop, side, shift in (("hide", "left", 0), ("hide2", "right", 10)) for h, tex in FOWL.items()]
w(bs_path, {"multipart": parts})
loot_path = D / "mechanical/loot_table/blocks/drying_rack.json"
loot = read(loot_path)
for pool, prop, value in ((loot["pools"][1], "hide", "25"), (loot["pools"][2], "hide2", "15")):
    assert any(prop in e.get("condition", {}).get("state", {}) for e in pool["entries"]), prop
    pool["entries"] = [e for e in pool["entries"] if e["name"] != M + "dried_fowl"]
    pool["entries"].append({"type": "minecraft:item", "name": M + "dried_fowl", "condition": {
        "type": "minecraft:match_block", "blocks": M + "drying_rack", "state": {prop: value}}})
w(loot_path, loot)
lang |= {"message.mechanical.drying_rack.too_thick": "Too thick to dry whole: smoke it, or cut it into strips with a knife",
         "message.mechanical.drying_rack.too_fatty": "Too fatty to dry, it would go rancid: smoke it instead"}

merge(A / "lang/en_us.json", lang)
print("woodmeat ok")
