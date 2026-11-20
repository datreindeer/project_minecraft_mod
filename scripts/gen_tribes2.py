"""Data for tribes that visit and settle (people/TribeVisits.java, Settlements.java, Neighbours.java, TribeDialog.java,
HideTentBlock.java, client/people/TribeDialogScreen.java).

- Hide Canvas (full block of sewn hides), Hide Tent Slope (a 45-degree hide panel rising toward FACING, with an optional
  triangular end wall on its left or right) and Hide Tent Ridge (two panels meeting at a ridge pole along FACING, the
  back end closed when CLOSED): models, blockstates, loot tables, recipes (building)
- lang: the blocks, the talk screen (TribeDialog), what visitors, camp traders and Trader Caravans (Caravans.java) say, messages
Textures: scripts/gen_tribes2_textures.py. Advancements: gen_advancements.py (wilderness: tribe_visit ...).
Generator order: ... gen_fauna_*.py, gen_tribes2.py, gen_block_items.py, gen_unlocks.py, gen_advancements.py, gen_registry.py.
Run: python scripts/gen_tribes2.py"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources"
A, D = ROOT / "assets/mechanical", ROOT / "data"
M = "mechanical:"
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
TEX = {"outer": M + "block/hide_canvas", "inner": M + "block/hide_canvas_inner", "particle": M + "block/hide_canvas"}


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def merge(path, obj):
    old = json.loads(path.read_text(encoding="utf-8")) if path.exists() else {}
    old.update(obj)
    path.write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")


def faces(names, tex, uv=None):
    return {f: {"texture": tex[f] if isinstance(tex, dict) else tex, **({"uv": uv[f]} if uv and f in uv else {})} for f in names}


# ---------------------------------------------------------------- models

def slope_panel():
    """A plate through the block's middle, tipped 45 degrees about x (rescaled to reach corner to corner): high at north."""
    return {"from": [0, 7.5, 0], "to": [16, 8.5, 16],
            "rotation": {"origin": [8, 8, 8], "axis": "x", "angle": 45, "rescale": True},
            "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#outer"}, "down": {"uv": [0, 0, 16, 16], "texture": "#inner"},
                      "east": {"uv": [0, 7, 16, 8], "texture": "#outer"}, "west": {"uv": [0, 7, 16, 8], "texture": "#outer"}}}


def end_wall(x0):
    """A stepped triangle of hide in the x0..x0+1 plane under the slope (high north), 2 px per step."""
    out = []
    for j in range(8):
        y0, y1, z1 = 2 * j, 2 * j + 2, 16 - 2 * j
        uv_side = [0, 16 - y1, z1, 16 - y0]
        out.append({"from": [x0, y0, 0], "to": [x0 + 1, y1, z1],
                    "faces": {"west": {"uv": uv_side, "texture": "#outer" if x0 == 0 else "#inner"},
                              "east": {"uv": uv_side, "texture": "#inner" if x0 == 0 else "#outer"},
                              "south": {"uv": [x0, 16 - y1, x0 + 1, 16 - y0], "texture": "#outer"},
                              "up": {"uv": [x0, 0, x0 + 1, z1], "texture": "#outer"}}})
    return out


def slope_model(end):
    elements = [slope_panel()]
    if end == "left":
        elements += end_wall(0)
    elif end == "right":
        elements += end_wall(15)
    return {"parent": "minecraft:block/block", "ambientocclusion": False, "textures": TEX, "elements": elements}


def ridge_model(closed):
    pole = "minecraft:block/stripped_spruce_log"
    elements = [
        # the two panels, each tipped 45 degrees about z, meeting at x 8, y 8
        {"from": [0, 3.5, 0], "to": [8, 4.5, 16], "rotation": {"origin": [4, 4, 8], "axis": "z", "angle": 45, "rescale": True},
         "faces": {"up": {"uv": [0, 0, 8, 16], "texture": "#outer"}, "down": {"uv": [0, 0, 8, 16], "texture": "#inner"},
                   "north": {"uv": [0, 7, 8, 8], "texture": "#outer"}, "south": {"uv": [0, 7, 8, 8], "texture": "#outer"}}},
        {"from": [8, 3.5, 0], "to": [16, 4.5, 16], "rotation": {"origin": [12, 4, 8], "axis": "z", "angle": -45, "rescale": True},
         "faces": {"up": {"uv": [8, 0, 16, 16], "texture": "#outer"}, "down": {"uv": [8, 0, 16, 16], "texture": "#inner"},
                   "north": {"uv": [8, 7, 16, 8], "texture": "#outer"}, "south": {"uv": [8, 7, 16, 8], "texture": "#outer"}}},
        # the ridge pole, poking out a little at both ends
        {"from": [7, 7.5, -1], "to": [9, 9.5, 17],
         "faces": {"up": {"uv": [7, 0, 9, 16], "texture": "#pole", "rotation": 0}, "east": {"uv": [0, 7, 16, 9], "texture": "#pole"},
                   "west": {"uv": [0, 7, 16, 9], "texture": "#pole"}, "down": {"uv": [7, 0, 9, 16], "texture": "#pole"},
                   "north": {"uv": [7, 7, 9, 9], "texture": "#pole_top"}, "south": {"uv": [7, 7, 9, 9], "texture": "#pole_top"}}},
    ]
    if closed:
        # the back (south) end: a stepped triangle of hide, 1 px per step
        for j in range(8):
            elements.append({"from": [j, j, 15], "to": [16 - j, j + 1, 16],
                             "faces": {"south": {"uv": [j, 15 - j, 16 - j, 16 - j], "texture": "#outer"},
                                       "north": {"uv": [j, 15 - j, 16 - j, 16 - j], "texture": "#inner"},
                                       "up": {"uv": [j, 15, 16 - j, 16], "texture": "#outer"}}})
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": TEX | {"pole": pole, "pole_top": pole + "_top"}, "elements": elements}


def models():
    b = A / "models/block"
    w(b / "hide_canvas.json", {"parent": "minecraft:block/cube_all", "textures": {"all": M + "block/hide_canvas"}})
    w(b / "hide_tent_slope.json", slope_model("none"))
    w(b / "hide_tent_slope_left.json", slope_model("left"))
    w(b / "hide_tent_slope_right.json", slope_model("right"))
    w(b / "hide_tent_ridge.json", ridge_model(False))
    w(b / "hide_tent_ridge_closed.json", ridge_model(True))
    w(A / "blockstates/hide_canvas.json", {"variants": {"": {"model": M + "block/hide_canvas"}}})
    slope = {}
    for f, y in FACINGS.items():
        for end in ("none", "left", "right"):
            m = {"model": M + "block/hide_tent_slope" + ("" if end == "none" else "_" + end)}
            if y:
                m["y"] = y
            slope[f"end={end},facing={f}"] = m
    w(A / "blockstates/hide_tent_slope.json", {"variants": slope})
    ridge = {}
    for f, y in FACINGS.items():
        for closed in ("false", "true"):
            m = {"model": M + "block/hide_tent_ridge" + ("_closed" if closed == "true" else "")}
            if y:
                m["y"] = y
            ridge[f"closed={closed},facing={f}"] = m
    w(A / "blockstates/hide_tent_ridge.json", {"variants": ridge})


# ---------------------------------------------------------------- recipes, loot

def recipes():
    r = D / "mechanical/recipe"
    w(r / "hide_canvas.json", {"type": "minecraft:crafting_shapeless", "category": "building",
                               "ingredients": [M + "scraped_hide", M + "scraped_hide", M + "cordage"], "result": {"id": M + "hide_canvas"}})
    w(r / "hide_tent_slope.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": ["C ", "SC"],
                                   "key": {"C": M + "hide_canvas", "S": "minecraft:stick"}, "result": {"id": M + "hide_tent_slope", "count": 2}})
    w(r / "hide_tent_ridge.json", {"type": "minecraft:crafting_shaped", "category": "building", "pattern": [" S ", "C C"],
                                   "key": {"C": M + "hide_canvas", "S": "minecraft:stick"}, "result": {"id": M + "hide_tent_ridge", "count": 2}})
    for b in ("hide_canvas", "hide_tent_slope", "hide_tent_ridge"):
        w(D / f"mechanical/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
            {"type": "minecraft:item", "name": M + b}], "condition": {"type": "minecraft:survives_explosion"}}],
            "random_sequence": f"mechanical:blocks/{b}"})


# ---------------------------------------------------------------- lang

LANG = {
    "block.mechanical.hide_canvas": "Hide Canvas",
    "block.mechanical.hide_tent_slope": "Hide Tent Slope",
    "block.mechanical.hide_tent_ridge": "Hide Tent Ridge",
    "gui.mechanical.tribe_dialog.trade": "Trade",
    "gui.mechanical.tribe_dialog.yes": "Yes, stay",
    "gui.mechanical.tribe_dialog.no": "No",
    "gui.mechanical.tribe_dialog.farewell": "Farewell",
    "dialog.mechanical.tribe.greet.peaceful": "Peace to your fire. We are the %s. We saw your smoke from the hills and came to meet you. Will you trade with us?",
    "dialog.mechanical.tribe.greet.wary": "We are the %s. Your smoke has risen here for many days, so we came to see who keeps this fire. We have goods, if you are fair.",
    "dialog.mechanical.tribe.greet.traders": "Ho, fire-keeper! The %s, traders from far valleys: furs, bone, flint and things you have never seen. Let us trade!",
    "dialog.mechanical.tribe.greet.raiders": "We are the %s. Today we come to trade. Only to trade.",
    "dialog.mechanical.tribe.ask": "The sun goes down and the night is long. May the %s stay by your fire tonight? We will keep to the fire and leave at dawn.",
    "dialog.mechanical.tribe.staying": "Thank you for your fire. The %s will keep watch with you and go at first light.",
    "dialog.mechanical.tribe.leaving": "The %s go now. Walk well.",
    "dialog.mechanical.tribe.trader": "I come from our camp, %s of here. I have goods; trade with me before I walk back.",
    "dialog.mechanical.tribe.standing": "The %s think of you as: %s",
    "dialog.mechanical.tribe.no_trade": "(They will not trade with you.)",
    "chat.mechanical.visit.greet.peaceful": "Peace to your fire! We are the %s. Come and talk to me, we would trade.",
    "chat.mechanical.visit.greet.wary": "We are the %s. We mean no harm. Talk to me if you want to trade.",
    "chat.mechanical.visit.greet.traders": "Ho there! The %s have come with goods! Talk to me!",
    "chat.mechanical.visit.greet.raiders": "The %s come to trade. Talk to me.",
    "chat.mechanical.visit.ask": "The sun is going down. May we stay by your fire tonight? (Right-click %s to answer.)",
    "chat.mechanical.visit.move_on": "No answer... Then we move on. Keep your fire well.",
    "chat.mechanical.visit.thanks": "Thank you, friend. We will keep to your fire.",
    "chat.mechanical.visit.refused": "As you wish. We go now.",
    "chat.mechanical.visit.gift": "Dawn. Thank you for your fire, friend. Take these, from all of us.",
    "chat.mechanical.visit.gift_left": "Dawn. We leave a gift by your fire for the night. Thank you.",
    "chat.mechanical.visit.settle": "This is good land. We will make our camp not far from here, to the %s. Come and see us!",
    "chat.mechanical.settlement.trader": "Greetings from the camp of the %s! I have goods; talk to me before I walk back.",
    "message.mechanical.visit.approach": "People are coming toward your camp from the %s...",
    "message.mechanical.visit.answered": "Someone has already answered them.",
    "message.mechanical.visit.invited": "%s invited the %s to stay the night.",
    "message.mechanical.visit.sent_away": "%s sent the %s away.",
    "message.mechanical.settlement.founded": "The %s have lit a fire and made camp to the %s of your camp.",
    "message.mechanical.settlement.stage2": "The camp of the %s grows: more tents, drying racks and stores.",
    "message.mechanical.settlement.stage3": "The %s have raised a palisade around their camp.",
    # Trader Caravans (people/Caravans.java): they take the Wandering Trader's place
    "entity.mechanical.tribesperson.role.trader": "Trader",
    "entity.mechanical.tribesperson.role.guard": "Caravan Guard",
    "chat.mechanical.tribesperson.greet.trader.0": "Goods from far lands, friend! Come, look.",
    "chat.mechanical.tribesperson.greet.trader.1": "A gemstone, perhaps? I pay in silver. In gold, for the best.",
    "chat.mechanical.tribesperson.greet.trader.2": "Copper, silver, gold: all good coin with me.",
    "chat.mechanical.tribesperson.greet.guard.0": "Keep your hands where I can see them, stranger.",
    "chat.mechanical.tribesperson.greet.guard.1": "Talk to the old one if you want to trade.",
    "chat.mechanical.tribesperson.greet.guard.2": "The road is long and full of thieves. Do not be one.",
    "dialog.mechanical.tribe.caravan": "Greetings, traveller! The caravan of the %s has come a long way with goods from far lands, for coin. And gemstones... if you have gemstones, I pay well, in silver and gold.",
    "message.mechanical.cart.caravan": "The caravan's cart: its guards would not like that",
}


if __name__ == "__main__":
    models()
    recipes()
    merge(A / "lang/en_us.json", LANG)
    print("tribes2: 3 blocks, 3 recipes, %d lang keys" % len(LANG))
