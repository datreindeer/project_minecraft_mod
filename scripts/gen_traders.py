"""Trader Caravan kinds (people/TraderKinds.java, docs/traders-plan.md): one data/mechanical/trader_kind/<kind>.json per row of
KINDS below, the Coin Purse item, and the lang lines (kind names, what each leader says, the trade screen, the handler).
Prices are in copper (1 silver = 10, 1 gold = 100): copper for cheap things, silver for dearer, gold for valuable.
`age`: 0 Stone, 1 Bronze, 2 Iron (a caravan shows goods up to one age ahead of its tribe). `rare`: only big (wagon) caravans.
A new kind: add a row here (and its look in gen_tribe_skins.py KIND_LOOKS, which paints its skins), run both.
Generator order: after gen_tribes2.py (it writes the caravan dialog line this file leaves alone), before gen_advancements.py.
Run: python scripts/gen_traders.py"""
import json
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
JAR = ROOT / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
M, V = "mechanical:", "minecraft:"


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def g(item, count, price, stock=4, age=0, rare=False):
    """A good: count of item for price copper, stock times."""
    out = {"item": item, "price": price}
    if count != 1:
        out["count"] = count
    if stock != 4:
        out["stock"] = stock
    if age:
        out["age"] = age
    if rare:
        out["rare"] = True
    return out


S, G = 10, 100  # a silver and a gold coin in copper

KINDS = {
    "woodsman": {
        "name": "Timber & Tools", "animal": "horse",
        "dialog": "Well met! The %s bring timber goods down from the great forests: saplings of every tree, good axes and saws, resin and bark. "
                  "Everything for coin, and we pay for good cordage and glue.",
        "cargo": [V + "oak_log", V + "spruce_log", V + "birch_log", M + "pine_log", V + "spruce_log", M + "aspen_log",
                  M + "bronze_saw", V + "oak_sapling", M + "pine_sapling", M + "flint_axe", M + "aspen_sapling", M + "oak_bark"],
        "purse": [150, 260],
        "sells": [g(M + "aspen_sapling", 2, 5), g(M + "beech_sapling", 2, 5), g(M + "linden_sapling", 2, 6), g(M + "ash_sapling", 2, 5),
                  g(M + "hazel_sapling", 2, 6), g(M + "pine_sapling", 2, 4), g(M + "yew_sapling", 1, 9, 3), g(M + "willow_sapling", 2, 5),
                  g(V + "cherry_sapling", 1, 12, 2), g(M + "flint_axe", 1, 6), g(M + "crude_stone_axe", 1, 9, 3),
                  g(M + "pine_resin", 4, 5), g(M + "oak_bark", 8, 3, 6), g(M + "hide_glue", 2, 6),
                  g(M + "bronze_axe", 1, 3 * S, 2, 1), g(M + "bronze_saw", 1, 3 * S, 2, 1), g(M + "bronze_chisel", 1, 2 * S, 2, 1),
                  g(M + "iron_saw", 1, 1 * G, 1, 2, True), g(M + "steel_axe", 1, 2 * G, 1, 2, True), g(V + "dark_oak_sapling", 2, 2 * S, 2, 0, True)],
        "buys": [g(M + "cordage", 6, 4, 6), g(M + "hide_glue", 2, 4, 4), g(M + "pine_resin", 6, 4, 4)],
    },
    "mining": {
        "name": "Mining Equipment", "animal": "horse",
        "dialog": "Ho there! The %s carry what a miner needs: picks, torches, lamps, coal and good ore from the deep veins. "
                  "Got raw ore of your own? We buy it.",
        "cargo": [V + "coal_block", V + "raw_copper_block", V + "barrel", M + "tin_ore", V + "raw_iron_block", V + "coal_block",
                  M + "bronze_pickaxe", V + "torch", M + "malachite", M + "flint_pickaxe", V + "coal", M + "cassiterite"],
        "purse": [180, 300],
        "sells": [g(M + "flint_pickaxe", 1, 6), g(M + "crude_stone_pickaxe", 1, 9, 3), g(V + "torch", 8, 4, 6), g(V + "coal", 8, 8, 4),
                  g(M + "malachite", 3, 6, 4, 1), g(M + "cassiterite", 2, 8, 4, 1), g(M + "crushed_tin_ore", 4, 9, 3, 1),
                  g(M + "tin_ingot", 1, 2 * S, 3, 1), g(M + "oil_lamp", 1, 1 * S, 2, 1), g(M + "bronze_pickaxe", 1, 3 * S, 2, 1),
                  g(V + "raw_iron", 4, 6 * S, 2, 2), g(M + "lead_ingot", 2, 3 * S, 2, 2),
                  g(M + "steel_pickaxe", 1, 3 * G, 1, 2, True), g(V + "raw_gold", 2, 2 * G, 1, 1, True)],
        "buys": [g(V + "raw_copper", 4, 3, 8), g(M + "raw_galena", 3, 4, 4, 1), g(V + "raw_iron", 3, 4, 6, 2), g(V + "raw_gold", 1, 1 * S, 4, 1)],
    },
    "jewelry": {
        "name": "Jewelry & Gemstones", "animal": "camel",
        "dialog": "Peace be with you! The %s have crossed the sands with gold and stones for the great and the wise. "
                  "And if you have gemstones... ah, gemstones! I pay well, in silver and gold.",
        "cargo": [V + "chest", V + "decorated_pot", V + "chest", V + "decorated_pot", V + "amethyst_block", V + "chest",
                  M + "gold_necklace", M + "ruby", M + "gold_ring", V + "amethyst_shard", M + "gold_chain", M + "sapphire"],
        "purse": [400, 700],
        "sells": [g(M + "gold_ring", 1, 3 * S, 2), g(M + "gold_chain", 1, 2 * S, 2), g(M + "gold_necklace", 1, 6 * S, 1),
                  g(M + "amber", 1, 2 * S, 3), g(M + "jade", 1, 4 * S, 2), g(M + "turquoise", 1, 4 * S, 2), g(M + "garnet", 1, 3 * S, 2),
                  g(M + "ruby", 1, 5 * S, 2), g(M + "sapphire", 1, 5 * S, 2), g(M + "silver_nugget", 4, 1 * S, 3, 1),
                  g(M + "silver_ingot", 1, 3 * S, 2, 2), g(M + "silk_thread", 4, 3 * S, 2, 2), g(M + "flawless_ruby", 1, 2 * G, 1, 0, True), g(M + "flawless_sapphire", 1, 2 * G, 1, 0, True)],
        "buys": [g("#mechanical:gemstones", 1, 4 * S, 3), g(V + "diamond", 1, 2 * G, 3), g(V + "emerald", 1, 1 * G, 3), g(V + "amethyst_shard", 1, 2 * S, 6)],
    },
    "hunting": {
        "name": "Hunting Gear & Furs", "animal": "horse",
        "dialog": "Greetings, hunter! The %s trade in bows and arrows, spears and snares, and the finest furs of the north. "
                  "Bring us raw pelts, horn and claw, and we pay in coin.",
        "cargo": [V + "barrel", V + "barrel", M + "wolf_pelt", M + "lynx_pelt", M + "antler", M + "sinew_bow",
                  M + "bear_pelt", M + "bone_arrow", M + "marten_fur", M + "horn"],
        "purse": [180, 320],
        "sells": [g(M + "crude_bow", 1, 8, 2), g(M + "bone_arrow", 8, 6, 4), g(M + "barbed_arrow", 8, 9, 3), g(M + "snare", 2, 4, 4),
                  g(M + "flint_spear", 1, 5, 2), g(M + "sinew_bow", 1, 2 * S, 2), g(M + "wolf_pelt", 1, 1 * S, 2), g(M + "lynx_pelt", 1, 2 * S, 1),
                  g(M + "marten_fur", 1, 1 * S, 2), g(M + "bronze_arrow", 8, 2 * S, 3, 1), g(M + "bronze_spear", 1, 4 * S, 1, 1),
                  g(M + "horn_bow", 1, 4 * S, 1, 0, True), g(M + "yew_longbow", 1, 6 * S, 1, 0, True), g(M + "bear_pelt", 1, 5 * S, 1, 0, True)],
        "buys": [g(M + "raw_wolf_pelt", 1, 4, 4), g(M + "raw_lynx_pelt", 1, 6, 3), g(M + "raw_marten_pelt", 1, 4, 3), g(M + "raw_ermine_pelt", 1, 5, 3),
                 g(M + "raw_bear_pelt", 1, 1 * S, 2), g(M + "antler", 2, 3, 4), g(M + "horn", 2, 3, 4), g(M + "bear_claw", 2, 3, 3), g(M + "sinew", 4, 2, 6)],
    },
    "herbs": {
        "name": "Herbs & Remedies", "animal": "horse",
        "dialog": "Be well, friend! The %s gather what heals and helps: salves and poultices, honey, berries of the juniper and seeds to sow. "
                  "We buy good mushrooms and the beaver's musk.",
        "cargo": [V + "decorated_pot", V + "poppy", V + "decorated_pot", V + "cornflower", V + "dandelion", V + "allium",
                  M + "bark_basket", M + "juniper_berries", M + "dried_mushrooms", M + "wild_honey", M + "bark_basket", M + "elderberries"],
        "purse": [150, 260],
        "sells": [g(M + "marmot_salve", 1, 1 * S, 2), g(M + "night_eye_salve", 1, 2 * S, 1), g(M + "castoreum_salve", 1, 1 * S, 2),
                  g(M + "snakebite_poultice", 1, 8, 3), g(M + "juniper_berries", 6, 4, 4), g(M + "pine_resin", 4, 5, 3),
                  g(M + "dried_mushrooms", 4, 4, 4), g(M + "wild_honey", 2, 6, 3), g(M + "nettle_seeds", 4, 4, 4), g(M + "flax_seeds", 4, 8, 3, 1),
                  g(V + "potato", 3, 2 * S, 1, 0, True), g(M + "morel", 4, 1 * S, 2, 0, True)],
        "buys": [g(M + "morel", 2, 3, 6), g(M + "oyster_mushroom", 4, 3, 6), g(M + "castoreum", 1, 6, 3), g(M + "wild_honey", 2, 4, 4),
                 g(M + "elderberries", 8, 3, 4)],
    },
    "foraging": {
        "name": "Forager's Goods", "animal": "horse",
        "dialog": "Good day! The %s live off the land and bring its gifts: baskets and seed bags, nuts, dried berries and good cord. "
                  "Full baskets of berries we always buy.",
        "cargo": [V + "barrel", V + "brown_mushroom", V + "red_mushroom", V + "barrel", V + "brown_mushroom", V + "fern",
                  M + "bark_basket", M + "raspberries", M + "hazelnuts", M + "blueberries", M + "bark_basket", M + "beechnuts"],
        "purse": [120, 220],
        "sells": [g(M + "bark_basket", 1, 4, 3), g(M + "seed_bag", 1, 6, 2), g(M + "hazelnuts", 8, 3, 4), g(M + "beechnuts", 8, 3, 4),
                  g(M + "roasted_hazelnuts", 6, 4, 3), g(M + "dried_berries", 6, 6, 3), g(M + "plant_fibre", 16, 2, 6), g(M + "cordage", 4, 4, 4),
                  g(M + "twine", 6, 3, 4), g(M + "flint_knife", 1, 4, 2), g(M + "leather_pouch", 1, 8, 2),
                  g(V + "potato", 3, 2 * S, 1, 0, True), g(M + "wild_strawberries", 8, 8, 2, 0, True)],
        "buys": [g(M + "raspberries", 16, 3, 6), g(M + "blueberries", 16, 3, 6), g(M + "lingonberries", 16, 3, 4), g(M + "cloudberries", 8, 5, 3),
                 g(M + "hazelnuts", 12, 3, 4), g(M + "wild_strawberries", 8, 4, 3)],
    },
    "eastern": {
        # flora and food plan 4b: the spice traders from the far east, on camels; rare (weight 1 against the others' 4)
        "name": "Eastern Caravan", "animal": "camel", "weight": 1,
        "dialog": "Peace and plenty to you! The %s have walked a year of roads from the far east, past the great mountains and the "
                  "deserts, with what never grows in these lands: pepper and cinnamon, ginger, rice, saffron and tea, bitter orange, lemon, "
                  "clementine, pomegranate and almond trees for warm gardens, and silk and indigo for your looms. We pay in silver for gemstones, and gladly for jujubes and sea buckthorn: a taste of home.",
        "cargo": [V + "decorated_pot", V + "barrel", V + "decorated_pot", V + "barrel", M + "black_pepper", M + "cinnamon_bark",
                  M + "rice", M + "tea_leaves", M + "silk_thread", M + "dried_dates", M + "indigo", M + "saffron"],
        "purse": [300, 520],
        "sells": [g(M + "black_pepper", 2, 3, 6, 1), g(M + "cinnamon_bark", 2, 4, 5, 1), g(M + "ginger_root", 2, 3, 4, 1),
                  g(M + "rice", 4, 4, 5, 1), g(M + "sesame_seeds", 4, 2, 5, 1), g(M + "dried_dates", 4, 2, 5, 1), g(M + "dried_figs", 4, 2, 5, 1),
                  g(M + "saffron_corm", 2, 2 * S, 2, 2), g(M + "tea_leaves", 4, 1 * S, 3, 2), g(M + "silk_thread", 4, 3 * S, 2, 2),
                  g(M + "indigo", 2, 1 * S, 3, 1), g(V + "lapis_lazuli", 2, 2 * S, 2, 1),
                  g(M + "bitter_orange_sapling", 1, 3 * S, 2, 2),  # the plan's citrus: grows only in a warm biome
                  # the orchard's southern trees (lemon, clementine and pomegranate take only in a warm biome)
                  g(M + "lemon_sapling", 1, 3 * S, 2, 2), g(M + "clementine_sapling", 1, 3 * S, 2, 2),
                  g(M + "pomegranate_sapling", 1, 3 * S, 1, 2), g(M + "almond_sapling", 1, 2 * S, 2, 2),
                  g(M + "saffron", 2, 4 * S, 1, 2, True), g(M + "flawless_sapphire", 1, 2 * G, 1, 0, True)],
        "buys": [g("#mechanical:gemstones", 1, 4 * S, 3), g(M + "jujubes", 6, 4, 6), g(M + "sea_buckthorn_berries", 8, 3, 6),
                 g(V + "amethyst_shard", 1, 2 * S, 4)],
    },
}

LANG = {
    "item.mechanical.coin_purse": "Coin Purse",
    "entity.mechanical.tribesperson.role.handler": "Caravan Handler",
    "entity.mechanical.tribesperson.role.trader": "Caravan Leader",
    "chat.mechanical.tribesperson.greet.handler.0": "Easy, easy... they spook at strangers.",
    "chat.mechanical.tribesperson.greet.handler.1": "Don't touch the animals. Talk to the old one if you want to trade.",
    "chat.mechanical.tribesperson.greet.handler.2": "Long road behind us. Let them graze a while.",
    "message.mechanical.caravan.animal": "The caravan's animal: its guards would not like that",
    "gui.mechanical.caravan.big": "%s · Rich Caravan",
    "gui.mechanical.caravan.purse": "Their purse",
    "gui.mechanical.caravan.for_sale": "For Sale",
    "gui.mechanical.caravan.they_buy": "They Buy",
    "gui.mechanical.caravan.your_coins": "Your coins:",
    "gui.mechanical.caravan.hint": "Click to trade · change given",
    "gui.mechanical.caravan.nothing": "Nothing",
    "gui.mechanical.caravan.left": "%s left",
    "gui.mechanical.caravan.wants": "wants %s more · you have %s",
    "gui.mechanical.caravan.sold_out": "Sold out",
    "gui.mechanical.caravan.enough": "They have enough of this",
    "gui.mechanical.caravan.too_dear": "You don't have enough coins",
    "gui.mechanical.caravan.purse_empty": "Their purse can't pay for it",
    "gui.mechanical.caravan.none_to_sell": "You don't have enough of it",
    "gui.mechanical.caravan.buy_for": "Buy for %s gold, %s silver, %s copper",
    "gui.mechanical.caravan.sell_for": "Sell for %s gold, %s silver, %s copper",
    "gui.mechanical.caravan.click_buy": "Click to buy one",
    "gui.mechanical.caravan.click_sell": "Click to sell one",
}


def check(item, lang, vanilla):
    ns, path = item.lstrip("#").split(":")
    if item.startswith("#"):
        return
    ok = path in vanilla if ns == "minecraft" else f"item.mechanical.{path}" in lang or f"block.mechanical.{path}" in lang
    assert ok, f"unknown item {item}"  # a typo would only show as a missing good in game


def main():
    lang_path = A / "lang/en_us.json"
    lang = json.loads(lang_path.read_text(encoding="utf-8"))
    vanilla = {Path(n).stem for n in zipfile.ZipFile(JAR).namelist() if n.startswith("assets/minecraft/items/") and n.endswith(".json")}
    out = D / "mechanical/trader_kind"
    for old in out.glob("*.json"):
        old.unlink()
    for kind, k in KINDS.items():
        for item in k["cargo"]:
            check(item, lang, vanilla)
        for good in k["sells"] + k["buys"]:
            check(good["item"], lang, vanilla)
            assert good["price"] > 0 and good.get("stock", 4) > 0, (kind, good)
        assert len(k["cargo"]) <= 12, kind  # CargoRender.MAX
        assert sum(1 for s in k["sells"] if not s.get("rare")) >= 6, f"{kind}: too few goods for a small caravan"
        data = {"animal": k["animal"], "cargo": k["cargo"], "purse": k["purse"], "sells": k["sells"], "buys": k["buys"]}
        if "weight" in k:  # how often the kind comes against the others (TraderKinds.WEIGHT = 4)
            data["weight"] = k["weight"]
        w(out / f"{kind}.json", data)
        lang[f"trader_kind.mechanical.{kind}"] = k["name"]
        lang[f"dialog.mechanical.tribe.caravan.{kind}"] = k["dialog"]
    lang.update(LANG)
    w(lang_path, lang)
    # held big and hanging from the hand, so the caravan leader's purse reads from afar
    held = {"rotation": [-80, 0, 0], "translation": [0, -1.5, 2.5], "scale": [0.9, 0.9, 0.9]}
    w(A / "models/item/coin_purse.json", {"parent": "minecraft:item/generated", "textures": {"layer0": "mechanical:item/coin_purse"},
                                          "display": {"thirdperson_righthand": held, "thirdperson_lefthand": held}})
    w(A / "items/coin_purse.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/coin_purse"}})
    print(f"trader kinds: {', '.join(KINDS)}")


if __name__ == "__main__":
    main()
