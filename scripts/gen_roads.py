"""Tribe roads (people/RoadBuilding.java, RoadMarkerItem.java; docs/living-world-plan.md A5-A9): the Road Marker's
texture, item model and definition, and the lang lines for the marker, the dialog buttons and the road messages.
Lang lines are inserted as raw text (formatting kept). Standalone; in the chain anywhere before gen_advancements.
Run: python scripts/gen_roads.py"""
import json
from pathlib import Path
from PIL import Image

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical"

# a stake driven in at an angle with a hide pennant: tribe colours vary, so a neutral dyed hide with a dark stripe
GRID = [
    "................",
    "......OOOOOOO...",
    ".....OphhhhhhO..",
    ".....OphssshhhO.",
    ".....OphhhhhhhhO",
    ".....OphhssshhO.",
    ".....OphhhhhO...",
    ".....OpOOOOO....",
    ".....Op.........",
    ".....Op.........",
    ".....Op.........",
    ".....Op.........",
    ".....Op.........",
    "....OOpO........",
    "...OggggO.......",
    "....OOOO........",
]
PAL = {"O": "2e2419", "p": "8a6a44", "h": "c9b48a", "s": "7a3b2a", "g": "5f7a3a"}


def texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(GRID):
        assert len(row) == 16, row
        for x, ch in enumerate(row):
            if ch != ".":
                c = PAL[ch]
                img.putpixel((x, y), tuple(int(c[i:i + 2], 16) for i in (0, 2, 4)) + (255,))
    img.save(A / "textures/item/road_marker.png")


def w(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


LANG = {
    "item.mechanical.road_marker": "Road Marker",
    "item.mechanical.road_marker.of": "Road Marker of the %s",
    "gui.mechanical.tribe_dialog.road": "Build a road",
    "dialog.mechanical.tribe.leader": "Welcome to the camp of the %s. If our people are friends, a road between our fires would serve us both.",
    "gui.mechanical.tribe_dialog.upgrade": "Trade road",
    "chat.mechanical.road.marker": "Set this marker down where you want the road to end, at most %s blocks from our camp. "
                                   "Up to %s blocks we build it for nothing; beyond that, one Silver Coin for every %s blocks.",
    "chat.mechanical.road.need_silver": "A trade road that long costs %s Silver Coins (a Gold Coin is worth 10). Come back with the coin.",
    "chat.mechanical.road.upgrade": "Good. We will widen the road and tread it hard, wide enough for traders and carts.",
    "message.mechanical.road.no_camp": "The %s have no camp to build a road from any more",
    "message.mechanical.road.bad_spot": "Set the Road Marker on firm ground with room above it",
    "message.mechanical.road.busy": "The %s are already building a road",
    "message.mechanical.road.too_far": "%s blocks from their camp: too far (at most %s)",
    "message.mechanical.road.need_silver": "This road costs %s Silver Coins (a Gold Coin counts as 10)",
    "message.mechanical.road.placed": "The %s will build a road to here: %s blocks from their camp, for %s Silver Coins",
    "message.mechanical.road.blocked": "The road builders of the %s found no way on: the road ends where they got to",
}


def lang():
    path = A / "lang/en_us.json"
    text = path.read_text(encoding="utf-8")
    have = json.loads(text)
    add = "".join(f",\n  {json.dumps(k)}: {json.dumps(v, ensure_ascii=False)}" for k, v in LANG.items() if k not in have)
    changed = [k for k, v in LANG.items() if k in have and have[k] != v]
    for k in changed:  # update in place
        old = f"{json.dumps(k)}: {json.dumps(have[k], ensure_ascii=False)}"
        assert old in text, k
        text = text.replace(old, f"{json.dumps(k)}: {json.dumps(LANG[k], ensure_ascii=False)}")
    if add:
        end = text.rstrip().rfind("}")
        text = text[:end].rstrip() + add + "\n}\n"
    path.write_text(text, encoding="utf-8")


texture()
w(A / "models/item/road_marker.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": "mechanical:item/road_marker"}})
w(A / "items/road_marker.json", {"model": {"type": "minecraft:model", "model": "mechanical:item/road_marker"}})
lang()
print("roads ok")
