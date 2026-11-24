"""Plants that go grey and dull in winter (client.SeasonalFoliage WINTER_PLANTS, SeasonLook.frost): their block models get a
tintindex so the seasonal frost filter can colour them (cross -> tinted_cross, mechanical:block/shrub -> tinted_shrub).
Berry bushes' _frozen models stay as they are (they already show winter). Run after the generators that write these
plants' models (gen_undergrowth*, gen_biomes, gen_farming...). Keep the list in step with SeasonalFoliage.WINTER_PLANTS."""
import glob
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources/assets"
jar = zipfile.ZipFile(sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar")))[0])

MOD = ["hazel_shrub", "juniper", "bramble", "undergrowth", "sapling_thicket", "wild_carrot_plant", "sea_beet", "stinging_nettle",
       "blackberry_bush", "blueberry_bush", "cloudberry_bush", "elderberry_bush", "lingonberry_bush", "raspberry_bush",
       "wild_strawberry_bush",
       # Flora (gen_flora.py): flowers' bloom and leaves models; the thicket bushes have their own tinted models
       "pasque_flower", "harebell", "lady_slipper", "sea_thrift", "mountain_avens", "edelweiss", "sand_verbena", "desert_marigold",
       "great_mullein", "desert_candle", "foxglove", "yellow_adonis", "bog_asphodel", "marsh_marigold", "wood_anemone", "bluebells",
       "purple_saxifrage", "purple_loosestrife", "spiny_thrift", "cats_foot"]
VANILLA = ["sweet_berry_bush", "dandelion", "poppy", "blue_orchid", "allium", "azure_bluet", "red_tulip", "orange_tulip",
           "white_tulip", "pink_tulip", "oxeye_daisy", "cornflower", "lily_of_the_valley"]
TINTED = {"minecraft:block/cross": "minecraft:block/tinted_cross", "mechanical:block/shrub": "mechanical:block/tinted_shrub",
          "minecraft:block/tinted_cross": None, "mechanical:block/tinted_shrub": None}


def models(state):
    out = set()

    def walk(n):
        if isinstance(n, dict):
            if isinstance(n.get("model"), str):
                out.add(n["model"])
            for v in n.values():
                walk(v)
        elif isinstance(n, list):
            for v in n:
                walk(v)
    walk(state)
    return out


def read(ns, path):
    f = RES / ns / path
    if f.exists():
        return json.loads(f.read_text(encoding="utf-8"))
    return json.loads(jar.read(f"assets/{ns}/{path}").decode())


# the shrub parent with every face tinted
shrub = read("mechanical", "models/block/shrub.json")
for e in shrub["elements"]:
    for face in e["faces"].values():
        face["tintindex"] = 0
(RES / "mechanical/models/block/tinted_shrub.json").write_text(json.dumps(shrub, indent=2) + "\n", encoding="utf-8")

changed = 0
for ns, names in (("mechanical", MOD), ("minecraft", VANILLA)):
    for name in names:
        for ref in sorted(models(read(ns, f"blockstates/{name}.json"))):
            mns, mpath = ref.split(":")
            if mpath.endswith("_frozen"):
                continue
            m = read(mns, f"models/{mpath}.json")
            parent = m.get("parent", "")
            parent = parent if ":" in parent else "minecraft:" + parent
            assert parent in TINTED, f"{ref}: parent {parent} has no tinted version"
            if TINTED[parent]:
                m["parent"] = TINTED[parent]
                out = RES / mns / f"models/{mpath}.json"
                out.parent.mkdir(parents=True, exist_ok=True)
                out.write_text(json.dumps(m, indent=2) + "\n", encoding="utf-8")
                changed += 1
print(f"winter plants: {len(MOD) + len(VANILLA)} plants, {changed} models tinted")
