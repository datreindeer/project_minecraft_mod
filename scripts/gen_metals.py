# Every metal and melt in the mod: the ONE table (docs/metals.md, "Så lägger du till en metall").
# Writes src/main/resources/data/mechanical/metals.json (read once at start-up by Molten.java: melting point, density,
# colour, items, what melts into it, clay-mold casts, alloys), the metal/molten names in en_us.json, and the table in
# docs/metals.md between its markers. Other generators import CLAY_METALS from here (the Clay Mold "metal" states).
# usage: python scripts/gen_metals.py   (right after gen_data.py, before every other generator)
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
R = ROOT / "src/main/resources"
M, V = "mechanical:", "minecraft:"
INGOT, NUGGET, BLOCK, STONE = 200, 22, 1800, 250  # mB (Molten.INGOT ..., FoundryBasinBlock.STONE)

# The clay-mold shapes (ClayMoldBlock.Shape) bronze casts; each gives rough_cast_bronze_<shape> unless named here.
BRONZE_SHAPES = ["ingot", "pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "saw_blade", "plate", "spear_head",
                 "sword_blade", "kettle", "arrowheads", "bucket", "sickle_blade", "shears", "fish_hooks", "needles", "tongs",
                 "chisel_blade", "hammer_head", "anvil", "tuyere", "tool_rod", "bolt_heads"]
BRONZE_CASTS = {s: M + {"ingot": "bronze_ingot", "tuyere": "bronze_tuyere_nozzle"}.get(s, f"rough_cast_bronze_{s}") for s in BRONZE_SHAPES}
COPPER_CASTS = {"ingot": V + "copper_ingot", **{s: M + f"rough_cast_copper_{s}" for s in
                ["pickaxe_head", "axe_head", "shovel_head", "hoe_head", "knife_blade", "tool_rod"]}}

# id: also the lang key, the save id and (for crucible metals) the Metal enum constant and the Clay Mold "metal" state.
# melt: degrees a furnace must reach to melt it (real_melt only for the docs). density: g/cm³, decides the High Furnace's
#   layers (heaviest at the bottom); real values except where noted. color: molten glow, RGB (every renderer and screen).
# ingot/nugget/block/plate: item ids (None = it has none). ores: other items that melt into it in the High Furnace (mB).
#   Its ingot, nugget and block melt too (200/22/1800) unless remelt_nugget is False (a nugget shared with another metal).
# reduce: iron ores reduced at reduce_heat into it: item -> (mB, mB with flux). crucible: ore item -> units in a Crucible /
#   Smelting Furnace (makes it a crucible metal: it needs a Metal enum constant, see Metal.java). casts: clay-mold shape ->
#   item. flask: Molding Flask pattern -> item (the non-clay patterns). poor: casts are marked Poor Bronze. coin: minted
#   from its ingot on the Bronze Anvil. nonmetal: glass, stone and slag (cast as a 250 mB block, no ingots).
#   also: other furnaces that make it (docs only).
# solid: the cold metal's colour; scripts/gen_metal_textures.py grows one palette from it for its nugget, ingot and block
#   (ingot_shape="crude": the rough library ingot; marks: "sand" / "fibres" on the ingot; dents: knocks in the block).
# Every mechanical: nugget / ingot / block here gets its model, item, loot, lang (if missing), pickaxe + stone-tool tags
# and the 9 <-> 1 crafting recipes from this script (Poor Bronze keeps its mark both ways; no poor block). Vanilla items
# (copper, gold, the shared iron nugget) keep vanilla's recipes, or none where an age generator takes them away.
METALS = [
    dict(id="gold", name="Gold", melt=1100, real_melt="1064", density=19.3, color=0xFFC21E,
         ingot=V + "gold_ingot", nugget=V + "gold_nugget", block=V + "gold_block", ores={V + "raw_gold": 300}, coin=M + "gold_coin",
         also="Clay Kiln 1100° (Raw Gold, 9 Gold Nugget)"),
    # gold and silver, 1:1 (ALLOYS), High Furnace only; real electrum is 12.5-15.5
    dict(id="electrum", name="Electrum", melt=1100, real_melt="≈1000–1060", density=14.9, color=0xFFE478,
         ingot=M + "electrum_ingot", nugget=M + "electrum_nugget", block=M + "electrum_block", solid=0xDCC46A),
    dict(id="lead", name="Lead", melt=330, real_melt="327", density=11.34, color=0x8C93A8,
         ingot=M + "lead_ingot", nugget=M + "lead_nugget", block=M + "lead_block", solid=0x5E6474, dents=3, ores={M + "raw_galena": 300}, also="Clay Kiln 800° (Raw Galena)"),
    dict(id="silver", name="Silver", melt=1000, real_melt="962", density=10.49, color=0xF4F6FF,
         ingot=M + "silver_ingot", nugget=M + "silver_nugget", block=M + "silver_block", solid=0xD4D8E0, coin=M + "silver_coin", also="Clay Kiln 1000° (Leaded Cupel)"),
    dict(id="copper", name="Copper", melt=1085, real_melt="1085", density=8.96, color=0xFF7A2A,
         ingot=V + "copper_ingot", nugget=V + "copper_nugget", block=V + "copper_block", coin=M + "copper_coin",
         ores={M + "crushed_copper_ore": 200, M + "copper_concentrate": 400, M + "native_copper": 400, V + "raw_copper": 400},
         crucible={M + "crushed_copper_ore": 1, M + "copper_concentrate": 2, M + "native_copper": 2}, casts=COPPER_CASTS),
    dict(id="bronze", name="Bronze", melt=950, real_melt="≈950 (10 % tenn)", density=8.8, color=0xFFB13B,
         ingot=M + "bronze_ingot", nugget=M + "bronze_nugget", block=M + "bronze_block", plate=M + "bronze_plate", casts=BRONZE_CASTS,
         solid=0xB07A30),
    # poor bronze has no block: a Bronze Block would remelt as good bronze (docs/metals.md, fix 7)
    dict(id="poor_bronze", name="Poor Bronze", melt=950, real_melt="900–1000", density=8.8, color=0xE09A44, poor=True,
         ingot=M + "bronze_ingot", nugget=M + "bronze_nugget", plate=M + "bronze_plate", casts=BRONZE_CASTS),
    dict(id="steel", name="Steel", melt=1500, real_melt="1425–1540", density=7.85, color=0xFFCE4A,
         ingot=M + "steel_ingot", nugget=V + "iron_nugget", remelt_nugget=False, block=M + "steel_block", plate=M + "steel_plate", solid=0x7E8A9A,
         ores={M + "blister_steel": INGOT}, also="Bloomery 1000° (cementering)"),
    dict(id="wrought_iron", name="Wrought Iron", melt=1550, real_melt="≈1510–1540", density=7.75, color=0xFFC040,
         ingot=M + "wrought_iron_ingot", nugget=V + "iron_nugget", remelt_nugget=False, block=M + "wrought_iron_block",
         plate=M + "iron_plate", solid=0x72706D, marks="fibres",
         also="Bloomery 1350° (bloom), Finery Hearth 1300°"),
    dict(id="tin", name="Tin", melt=232, real_melt="232", density=7.29, color=0xE8ECF2,
         ingot=M + "tin_ingot", nugget=M + "tin_nugget", block=M + "tin_block", solid=0x96978F, ores={M + "crushed_tin_ore": 200, M + "tin_concentrate": 400},
         crucible={M + "crushed_tin_ore": 1, M + "tin_concentrate": 2}),
    # iron nuggets (shared by the three irons) remelt as pig iron: scrap iron takes up carbon in the furnace
    dict(id="pig_iron", name="Pig Iron", melt=1200, real_melt="1150–1200", density=7.1, color=0xFFA33A,
         ingot=M + "pig_iron_ingot", nugget=V + "iron_nugget", block=M + "pig_iron_block", solid=0x4A4A4E,
         ingot_shape="crude", marks="sand", reduce_heat=1450,
         reduce={V + "raw_iron": (100, 200), M + "roasted_iron_ore": (200, 300), M + "raw_bog_iron": (65, 130), M + "roasted_bog_iron": (130, 200)},
         flask={"ingot_mold": M + "iron_ingot_mold", "plate_mold": M + "iron_plate_mold", "nugget_mold": M + "iron_nugget_mold",
                "gear": M + "cast_iron_gear", "pot": M + "cast_iron_pot"}),
    dict(id="basalt", name="Basalt", melt=1250, real_melt="1100–1250", density=2.9, color=0xC0402A, nonmetal=True,
         block=M + "cast_basalt", ores={V + "basalt": STONE, V + "polished_basalt": STONE, V + "smooth_basalt": STONE}),
    dict(id="glass", name="Glass", melt=1400, real_melt="≈1000–1400", density=2.5, color=0xB8F0A0, nonmetal=True,
         block=V + "glass", ores={M + "glass_batch": STONE}),
    dict(id="obsidian", name="Obsidian", melt=1700, real_melt="≈1000–1200 (spelvärde: toppvärme)", density=2.4, color=0x6A2AA8,
         nonmetal=True, block=V + "obsidian", ores={V + "obsidian": STONE}),
    # real slag is 2.8-3.3, but it is skimmed off the top: the lightest value keeps it there
    dict(id="slag", name="Slag", melt=1200, real_melt="1200–1400", density=2.3, real_density="2,8–3,3", color=0x3A4A2A, nonmetal=True,
         block=M + "slag_stone"),
]

# Two-part alloys, mixed in the High Furnace's pool in exact parts at heat. crucible: the Crucible and Smelting Furnace
# mix by share instead (percent of the second part): good -> result, poor -> poor_result, else the first part alone and the
# second is lost as slag. Only one crucible alloy is supported (CrucibleContents / FurnaceMelt are copper + tin).
ALLOYS = [
    dict(result="bronze", parts=[("copper", 9), ("tin", 1)], heat=950, crucible=dict(good=(9.0, 11.5), poor=(8.0, 15.0), poor_result="poor_bronze")),
    dict(result="steel", parts=[("pig_iron", 1), ("wrought_iron", 2)], heat=1500),
    dict(result="electrum", parts=[("gold", 1), ("silver", 1)], heat=1100),
]

CLAY_METALS = [m["id"] for m in METALS if m.get("casts")]  # ClayMoldBlock.METAL's values
POOR_KEY = M + "poor_bronze"  # the data component Bronze.POOR


def mine(item):
    """The path of a mechanical: item, else None (vanilla items aren't ours to draw or give recipes)."""
    return item.split(":")[1] if item and item.startswith(M) else None


def poor_twin(m):
    """Does a Poor metal share this metal's ingot (so its pieces can be poor, and recipes must keep the mark)?"""
    return not m.get("poor") and any(p.get("poor") and p.get("ingot") == m.get("ingot") for p in METALS)


def poor_only(item):
    return {"neoforge:ingredient_type": "neoforge:components", "items": item, "components": {POOR_KEY: {}}}


def not_poor(item):
    return {"neoforge:ingredient_type": "neoforge:difference", "base": item, "subtracted": poor_only(item)}


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def items_and_recipes(lang):
    """Models, items, loot, tags, lang and the 9 <-> 1 recipes for the table's own (mechanical:) nuggets, ingots and blocks."""
    A, D = R / "assets/mechanical", R / "data"
    recipe = lambda name, obj: write(D / f"mechanical/recipe/{name}.json", obj)
    blocks, done = [], set()
    for m in METALS:
        if m.get("nonmetal"):
            continue
        ingot, nugget, block = m.get("ingot"), m.get("nugget"), m.get("block")
        for key, item in (("ingot", ingot), ("nugget", nugget)):
            if (n := mine(item)) and n not in done:
                write(A / f"models/item/{n}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"mechanical:item/{n}"}})
                write(A / f"items/{n}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:item/{n}"}})
                lang.setdefault(f"item.mechanical.{n}", f"{m['name']} {key.title()}")
                done.add(n)
        if (b := mine(block)) and b not in done:
            write(A / f"models/block/{b}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"mechanical:block/{b}"}})
            write(A / f"blockstates/{b}.json", {"variants": {"": {"model": f"mechanical:block/{b}"}}})
            write(A / f"items/{b}.json", {"model": {"type": "minecraft:model", "model": f"mechanical:block/{b}"}})
            write(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1,
                  "condition": {"type": "minecraft:survives_explosion"}, "entries": [{"type": "minecraft:item", "name": block}]}],
                  "random_sequence": f"mechanical:blocks/{b}"})
            lang.setdefault(f"block.mechanical.{b}", f"Block of {m['name']}")
            blocks.append(block)
            done.add(b)
        i = mine(ingot)
        if m.get("poor") or not i:
            continue
        plain = not_poor if poor_twin(m) else (lambda x: x)
        if n := mine(nugget):
            recipe(n, {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [plain(ingot)], "result": {"id": nugget, "count": 9}})
            recipe(f"{i}_from_nuggets", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["NNN", "NNN", "NNN"],
                                         "key": {"N": plain(nugget)}, "result": {"id": ingot}})
            if poor_twin(m):  # Poor Bronze stays poor: a poor ingot makes poor nuggets and back
                poor = {POOR_KEY: {}}
                recipe(f"{n}_poor", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [poor_only(ingot)],
                                     "result": {"id": nugget, "count": 9, "components": poor}})
                recipe(f"{i}_from_poor_nuggets", {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["NNN", "NNN", "NNN"],
                                                  "key": {"N": poor_only(nugget)}, "result": {"id": ingot, "components": poor}})
        if b := mine(block):  # poor ingots make no block (it would remelt as good bronze: docs/metals.md, fix 7)
            recipe(b, {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["III", "III", "III"],
                       "key": {"I": plain(ingot)}, "result": {"id": block}})
            recipe(f"{i}_from_{b}", {"type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [block],
                                     "result": {"id": ingot, "count": 9}})
    for tag in ("mineable/pickaxe", "needs_stone_tool"):
        path = D / f"minecraft/tags/block/{tag}.json"
        old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {"values": []}
        old["values"] += [b for b in blocks if b not in old["values"]]
        write(path, old)


def entry(m):
    melts = dict(m.get("ores", {}))
    for key, mb in (("ingot", INGOT), ("nugget", NUGGET), ("block", BLOCK)):
        if m.get(key) and (key != "nugget" or m.get("remelt_nugget", True)):
            melts.setdefault(m[key], mb)
    out = {"id": m["id"], "melt": m["melt"], "density": m["density"], "color": f"{m['color']:06X}", "melts": melts}
    for k in ("ingot", "nugget", "block", "plate", "coin", "crucible", "casts", "flask", "reduce_heat"):
        if m.get(k):
            out[k] = m[k]
    if m.get("reduce"):
        out["reduce"] = {k: list(v) for k, v in m["reduce"].items()}
    for k in ("poor", "nonmetal"):
        if m.get(k):
            out[k] = True
    return out


def check():
    ids = [m["id"] for m in METALS]
    assert len(ids) == len(set(ids)), "duplicate metal id"
    for a in ALLOYS:
        assert a["result"] in ids and all(p in ids for p, _ in a["parts"]) and len(a["parts"]) == 2, a
    assert sum(1 for a in ALLOYS if a.get("crucible")) <= 1, "only one crucible alloy is supported"
    assert min(METALS, key=lambda m: m["density"])["id"] == "slag", "slag must stay the lightest (it floats on top)"
    for m in METALS:
        assert m.get("nonmetal") or m.get("ingot"), f"{m['id']}: a metal needs an ingot"
        assert set(m.get("casts", {})) <= set(BRONZE_SHAPES), f"{m['id']}: unknown clay-mold shape"
        assert m.get("solid") or m.get("nonmetal") or m.get("poor") or not any(mine(m.get(k)) for k in ("ingot", "nugget", "block")), \
            f"{m['id']}: its own nugget/ingot/block need a solid= colour (gen_metal_textures.py draws them)"


def doc_table():
    rows = ["| Smälta | Smälter | Verklig | Densitet | Färg | Ingot / Nugget / Block | Ugnar |", "|---|---|---|---|---|---|---|"]
    short = lambda i: i.split(":")[1] if i else "–"
    for m in sorted(METALS, key=lambda m: -m["density"]):
        ovens = ["HF"]
        if m.get("crucible") or any(a.get("crucible") and a["result"] in (m["id"],) for a in ALLOYS) or m.get("poor"):
            ovens = ["Crucible", "Smelting Furnace"] + ovens
        dens = str(m["density"]).replace(".", ",") + (f" (verklig {m['real_density']})" if m.get("real_density") else "")
        items = " / ".join(short(m.get(k)) for k in ("ingot", "nugget", "block")) if not m.get("nonmetal") else f"block: {short(m.get('block'))}"
        real = re.sub(r"^([≈\d–]+)", lambda g: g[1] + "°", m["real_melt"])
        ovens += [m["also"]] if m.get("also") else []
        rows.append(f"| {m['name']} | {m['melt']}° | {real} | {dens} | `{m['color']:06X}` | {items} | {', '.join(ovens)} |")
    for a in ALLOYS:
        (p, n), (q, k) = a["parts"]
        extra = f"; i degeln {a['crucible']['good'][0]}–{a['crucible']['good'][1]} % {q} = {a['result']}, " \
                f"{a['crucible']['poor'][0]}–{a['crucible']['poor'][1]} % = {a['crucible']['poor_result']}" if a.get("crucible") else ""
        rows.append(f"\nLegering: {n} {p} + {k} {q} → {n + k} {a['result']} vid {a['heat']}°{extra}.")
    return "\n".join(rows)


if __name__ == "__main__":
    check()
    data = {"metals": [entry(m) for m in METALS],
            "alloys": [{"result": a["result"], "parts": [list(p) for p in a["parts"]], "heat": a["heat"],
                        **({"crucible": {"good": list(a["crucible"]["good"]), "poor": list(a["crucible"]["poor"]),
                                         "poor_result": a["crucible"]["poor_result"]}} if a.get("crucible") else {})} for a in ALLOYS]}
    out = R / "data/mechanical/metals.json"
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    lang_path = R / "assets/mechanical/lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    for m in METALS:
        lang[f"molten.mechanical.{m['id']}"] = f"Molten {m['name']}"
        if not m.get("nonmetal"):
            lang[f"metal.mechanical.{m['id']}"] = m["name"]
    items_and_recipes(lang)
    lang_path.write_text(json.dumps(lang, indent=2) + "\n")

    doc = ROOT / "docs/metals.md"
    if doc.exists():
        text = doc.read_text(encoding="utf-8")
        new = re.sub(r"(<!-- metals:start -->).*?(<!-- metals:end -->)", lambda g: g[1] + "\n" + doc_table() + "\n" + g[2], text, flags=re.S)
        doc.write_text(new, encoding="utf-8")
    print("metals: ok")
