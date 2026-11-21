"""Traps data (Traps.java, SnareBlock.java, FishTrapBlock.java): block models (snare set / baited / sprung, fish trap
empty to full with fish visible), blockstates, item models, loot, recipes, the snare bait tag, recipe unlock, lang.
Textures: scripts/gen_trap_textures.py. Numbers and rules: docs/ages/01-stone-age.md ("Fällor och snaror").
Generator order: ... gen_soot.py, gen_phantom.py, gen_traps.py, gen_block_items.py, gen_registry.py.
Run: python scripts/gen_traps.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
FACES = ("north", "south", "east", "west", "up", "down")
ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def tag_add(path, *values):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
    old["values"] += [v for v in values if v not in old["values"]]
    w(path, old)


def el(f, t, tex, faces=FACES, rot=None, **over):
    """A box; UVs follow the coordinates (auto). over: face -> texture to use instead, or None to leave the face out."""
    e = {"from": f, "to": t, "faces": {}}
    for face in faces:
        tx = over.get(face, tex)
        if tx:
            e["faces"][face] = {"texture": tx}
    if rot:
        e["rotation"] = rot
    return e


def model(name, textures, elements, parent="minecraft:block/block"):
    w(A / f"models/block/{name}.json", {"parent": parent, "textures": textures, "elements": elements})


# ---------------------------------------------------------------- snare
# Faces north before rotation: two stakes either side of the run, a crossbar on top, the noose hanging from it
# across the trail (the animal walks north-south through it). Bait lies on the far side of the noose.
SN_TEX = {"particle": "mechanical:block/snare_stake", "stake": "mechanical:block/snare_stake",
          "twine": "mechanical:block/snare_twine", "bait": "mechanical:block/snare_bait"}
STAKES = [el([2.5, 0, 7.25], [4, 9, 8.75], "#stake"), el([12, 0, 7.25], [13.5, 9, 8.75], "#stake"),
          el([2, 8.5, 7.5], [14, 9.75, 8.5], "#stake")]  # crossbar, whittled ends showing on top
NOOSE = [el([7.75, 7.25, 7.75], [8.25, 8.5, 8.25], "#twine"),  # hanger cord
         el([5.5, 6.75, 7.75], [10.5, 7.25, 8.25], "#twine"), el([5.5, 1.5, 7.75], [10.5, 2, 8.25], "#twine"),
         el([5, 2, 7.75], [5.5, 6.75, 8.25], "#twine"), el([10.5, 2, 7.75], [11, 6.75, 8.25], "#twine"),
         # lashings where the crossbar sits on the stakes
         el([2.25, 8, 7], [4.25, 8.5, 9], "#twine"), el([11.75, 8, 7], [13.75, 8.5, 9], "#twine")]
BAIT = [el([6.5, 0, 2.5], [9, 1.25, 5], "#bait"), el([9.25, 0, 3.5], [10.75, 1, 5], "#bait"), el([5.25, 0, 4], [6.5, 0.75, 5.25], "#bait")]
# sprung: one stake knocked flat, the crossbar fallen across, the noose pulled shut in a tight knot on the ground
SPRUNG = [el([2.5, 0, 7.25], [4, 9, 8.75], "#stake"),
          el([11, 0, 2.5], [12.5, 1.5, 11.5], "#stake", rot={"origin": [11.75, 0.75, 7], "axis": "y", "angle": 22.5}),
          el([3, 0, 9], [14, 1.25, 10], "#stake", rot={"origin": [8, 0.5, 9.5], "axis": "y", "angle": -22.5}),
          el([4, 8, 7], [4.75, 8.5, 9], "#twine"),  # the cord still tied to the standing stake ...
          el([4.25, 0.25, 7.75], [4.75, 8, 8.25], "#twine"),  # ... hanging down ...
          el([4.75, 0, 7.75], [8, 0.5, 8.25], "#twine"),  # ... to the shut noose
          el([7.5, 0, 6.75], [10, 0.75, 7.25], "#twine"), el([7.5, 0, 8.75], [10, 0.75, 9.25], "#twine"),
          el([7.5, 0, 7.25], [8, 0.75, 8.75], "#twine"), el([9.5, 0, 7.25], [10, 0.75, 8.75], "#twine")]
model("snare_set", SN_TEX, STAKES + NOOSE)
model("snare_set_baited", SN_TEX, STAKES + NOOSE + BAIT)
model("snare_sprung", SN_TEX, SPRUNG)
w(A / "blockstates/snare.json", {"variants": {
    f"baited={str(b).lower()},facing={f},sprung={str(s).lower()}": {
        "model": "mechanical:block/" + ("snare_sprung" if s else "snare_set_baited" if b else "snare_set"), **({"y": y} if y else {})}
    for f, y in ROT for s in (False, True) for b in (False, True)}})

# ---------------------------------------------------------------- fish trap
# A basket lying along the stream, funnel mouth north (upstream) before rotation, the closed end tied shut.
# The body's lattice is see-through, so the catch shows: one more fish per FISH level.
FT_TEX = {"particle": "mechanical:block/fish_trap_rim", "weave": "mechanical:block/fish_trap_weave", "rim": "mechanical:block/fish_trap_rim",
          "twine": "mechanical:block/snare_twine", "salmon": "mechanical:block/fish_trap_salmon", "cod": "mechanical:block/fish_trap_cod"}
BODY = [el([3, 0, 3], [13, 10, 14], "#weave", north=None, down="#rim"),
        # chamfers along the four long edges make it read round
        el([2, 2, 3], [3, 8, 14], "#weave", north=None, south=None), el([13, 2, 3], [14, 8, 14], "#weave", north=None, south=None),
        el([5, 10, 3], [11, 11, 14], "#weave", north=None, south=None),
        # mouth ring (tight weave) and the funnel reaching inside
        el([2, 0, 1], [14, 1.5, 3], "#rim"), el([2, 9, 1], [14, 11, 3], "#rim"),
        el([2, 1.5, 1], [4, 9, 3], "#rim"), el([12, 1.5, 1], [14, 9, 3], "#rim"),
        el([5, 3, 3], [11, 8, 7], "#weave", north=None, south=None),
        # tapered closed end, tied with twine
        el([5, 2, 14], [11, 8, 15.5], "#rim"), el([6.5, 3.5, 15.5], [9.5, 6.5, 16], "#rim"),
        el([4.75, 4.5, 14.25], [11.25, 5.5, 15.25], "#twine")]
FISH = [el([4, 0.6, 7], [12, 0.6, 13.5], "#salmon", faces=("up",)),  # lying on the bottom
        el([3.5, 4, 6], [3.5, 9, 13.5], "#cod", faces=("east", "west")),  # on its side against the west wall
        el([12.5, 3, 6.5], [12.5, 8, 13.5], "#salmon", faces=("east", "west"))]  # and the east wall
for e in FISH:  # the whole fish picture on each plane, not the slice auto-UV would cut out
    for face in e["faces"].values():
        face["uv"] = [0, 0, 16, 16]
for n in range(4):
    model(f"fish_trap_{n}", FT_TEX, BODY + FISH[:n])
w(A / "blockstates/fish_trap.json", {"variants": {
    f"facing={f},fish={n},waterlogged={wl}": {"model": f"mechanical:block/fish_trap_{n}", **({"y": y} if y else {})}
    for f, y in ROT for n in range(4) for wl in ("false", "true")}})

# ---------------------------------------------------------------- items, loot, recipes, tags, lang
for item in ("snare", "fish_trap"):
    w(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{item}"}})
    w(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{item}"}})
    w(D / f"mechanical/loot_table/blocks/{item}.json", {"type": "minecraft:block", "random_sequence": f"mechanical:blocks/{item}",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"mechanical:{item}"}],
                   "condition": {"type": "minecraft:survives_explosion"}}]})
    tag_add(D / "minecraft/tags/block/mineable/axe.json", f"mechanical:{item}")

# Snare: two sticks and a cordage noose. Fish Trap: six sticks woven with two twine.
w(D / "mechanical/recipe/snare.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["SC", "S "], "key": {"S": "minecraft:stick", "C": "mechanical:cordage"}, "result": {"id": "mechanical:snare"}})
w(D / "mechanical/recipe/fish_trap.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["STS", "S S", "STS"], "key": {"S": "minecraft:stick", "T": "mechanical:twine"}, "result": {"id": "mechanical:fish_trap"}})
w(D / "mechanical/advancement/recipes/traps.json", {"parent": "minecraft:recipes/root",
    "criteria": {"has_twine": {"trigger": "minecraft:inventory_changed", "conditions": {"items": [{"items": "mechanical:twine"}]}}},
    "requirements": [["has_twine"]], "rewards": {"recipes": ["mechanical:snare", "mechanical:fish_trap"]}})

# bait: berries, seeds and grain, small roots
w(D / "mechanical/tags/item/snare_bait.json", {"values": [
    "minecraft:sweet_berries", "minecraft:glow_berries", "minecraft:wheat_seeds", "minecraft:beetroot_seeds", "minecraft:melon_seeds",
    "minecraft:pumpkin_seeds", "minecraft:carrot", "minecraft:apple",
    *[f"mechanical:{b}" for b in ("juniper_berries", "raspberries", "blueberries", "lingonberries", "wild_strawberries",
                                  "elderberries", "cloudberries", "blackberries", "barley_grain", "einkorn_grain", "emmer_grain", "peas")]]})

merge(A / "lang/en_us.json", {"block.mechanical.snare": "Snare", "block.mechanical.fish_trap": "Fish Trap"})
print("traps ok")
