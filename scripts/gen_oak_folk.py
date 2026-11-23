"""The Oak Folk at the Ancient Oaks (people/OakFolk.java, OakQuests.java, docs/living-world-plan.md F4).

Writes: the skins (tribesperson/oak_<role>_<i>.png, 4 per role, painted with gen_tribe_skins' Skin/Look in greens and
moss: gatherers in woven leaf tunics with a bark apron, worshippers in hooded sage robes, barefoot, light-bearers in pale
lichen robes, the keeper in a deep green gold-banded robe with a white fur cape; no tribe colour), the item textures
and models (Verdant Staff with an upright in-hand model like the Elder's Staff, Ancient Ritual Knife, Animal Heart, Fine
Sinew, Trophy Skull) and the lang (roles, greetings, quest titles, quest screen).
Generator order: after gen_people.py and gen_tribe_skins.py, after gen_staffs.py, before gen_advancements.py / gen_unlocks.py.
Run: python scripts/gen_oak_folk.py
"""
import json
import random
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
import gen_tribe_skins as T  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
A = ROOT / "src/main/resources/assets/mechanical"
M = "mechanical:"
hexc = T.hexc

MOSS, FERN, LEAF_DK, SAGE, LICHEN = hexc("4f6b2c"), hexc("5f8a3a"), hexc("2f4a1e"), hexc("7a9a5a"), hexc("a9bd86")
BARK, CORD, GOLD = T.BARK, T.CORD, T.GOLD


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def oak_person(role, i):
    r = random.Random(f"oak-{role}-{i}")
    woman = i % 2 == 1
    L = T.Look(r, woman, role)
    L.skin = T.SKIN[["light", "tan", "pale", "olive"][i]]
    L.eyes = T.EYES[T.pick(r, ["green", "green", "brown", "grey"])]
    L.hair = T.HAIR[T.pick(r, ["auburn", "brown", "darkbrown", "blond"])]
    L.hair_style = T.pick(r, ["braids", "long"]) if woman else T.pick(r, ["long", "shaggy"])
    L.beard = None if woman else T.pick(r, ["short", "full"])
    L.wraps = CORD
    if role == "gatherer":
        L.top, L.top_kind, L.top_mat, L.sleeves = [FERN, MOSS, FERN, MOSS][i], "tunic", "weave", 4
        L.apron, L.pouch, L.belt = BARK, T.LEATHER, CORD
        L.legs, L.legs_to, L.boots = (None, 22, BARK) if woman else (MOSS, 20, BARK)
        L.skirt_to = 16 if woman else 14
        L.headband = LICHEN
        L.necklace = "amber"
    elif role == "worshipper":
        L.top, L.top_kind, L.sleeves, L.skirt_to = SAGE, "robe", 12, 21
        L.hood, L.hood_trim = MOSS, LICHEN
        L.trim, L.belt = LICHEN, CORD
        L.legs, L.barefoot = None, True
        L.paint = (FERN, "dots")
    elif role == "lightbearer":
        L.top, L.top_kind, L.top_mat, L.sleeves, L.skirt_to = LICHEN, "robe", "weave", 12, 20
        L.fine = True
        L.band, L.belt = FERN, CORD
        L.headband = FERN
        L.legs, L.boots = None, BARK
        L.necklace = "shell"
    else:  # keeper
        L.hair = T.HAIR[T.pick(r, ["grey", "white"])]
        L.wrinkles = True
        L.top, L.top_kind, L.top_mat, L.sleeves, L.skirt_to = LEAF_DK, "robe", "weave", 12, 21
        L.fine = True
        L.band, L.trim, L.belt = GOLD, GOLD, T.LEATHER
        L.fur, L.fur_kind = T.FUR_WHITE, "cape"
        L.necklace = "amber"
        L.paint = (FERN, "dots")
        L.legs, L.boots = None, BARK
        L.hair_style = "long"
        L.beard = None if woman else "long"
    return L


ROLES = ["gatherer", "worshipper", "lightbearer", "keeper"]


def skins():
    out = T.OUT
    for role in ROLES:
        for i in range(4):
            L = oak_person(role, i)
            img = T.Skin(f"oak{role}{i}", L.woman).build(L)
            img.save(out / f"oak_{role}_{i}.png")


# ---- item textures (16x16, vanilla-like outlines) ----

def img16():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def rgba(h, a=255):
    return hexc(h) + (a,)


def knife():
    """Diagonal like a vanilla sword: a dark, green-edged blade, a gold guard, a carved antler grip with green runes."""
    im = img16()
    p = im.putpixel
    for t in range(9):  # blade from (6,9) up to (14,1)
        x, y = 6 + t, 9 - t
        p((x, y), rgba("2b3a30"))
        p((x, y - 1) if y > 0 else (x, y), rgba("8fe07a" if t % 2 else "6fc35a"))  # glowing edge
        if t < 7:
            p((x - 1, y), rgba("1c2620"))
    p((14, 0), rgba("b8f0a0"))
    for d in ((4, 9), (5, 10), (6, 11), (5, 8), (7, 10)):  # gold guard
        p(d, rgba("d4a838"))
    p((6, 10), rgba("f2d470"))
    for t in range(4):  # antler grip
        x, y = 4 - t, 11 + t
        p((x, y), rgba("e8dfc8"))
        p((x + 1, y), rgba("b9ad8e"))
    p((3, 12), rgba("5fd35a"))
    p((2, 13), rgba("5fd35a"))
    p((0, 15), rgba("8a7c60"))
    return im


def heart():
    im = img16()
    rows = ["................", "................", ".....##..##.....", "....#aa##bb#....", "...#aRRaabRR#...",
            "...#RRRRRRRR#...", "...#RRRRRRdR#...", "...#RRRRRRdR#...", "....#RRRRdd#....", ".....#RRRRd#....",
            "......#RRd#.....", ".......#R#......", "........#.......", "................", "................", "................"]
    pal = {"#": "3a0a0c", "R": "8e1a1e", "a": "c2484a", "b": "6a2a40", "d": "5e1014"}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), rgba(pal[ch]))
    return im


def sinew():
    """A coil of pale, fine sinew thread."""
    im = img16()
    import math
    for k in range(3):
        for a in range(0, 360, 8):
            r = 4.2 - k * 1.1
            x, y = 8 + r * math.cos(math.radians(a)), 8 + r * 0.8 * math.sin(math.radians(a))
            im.putpixel((int(round(x)), int(round(y))), rgba(["f2e8cc", "dccfa8", "c4b48a"][(a // 40 + k) % 3]))
    for t in range(4):  # loose end
        im.putpixel((12 + t // 2, 11 + t), rgba("dccfa8"))
    return im


def skull():
    """A small horned trophy skull."""
    im = img16()
    rows = ["................", ".h............h.", ".hh..........hh.", "..hh........hh..", "...hh######hh...",
            "....#BBBBBBBB#..", "....#BBBBBBBB#..", "....#BEEBBEEB#..", "....#BEEBBEEB#..", ".....#BBBBBB#...",
            ".....#BBNNBB#...", "......#BBBB#....", "......#BTBT#....", ".......####.....", "................", "................"]
    pal = {"#": "6b6450", "B": "e8dfc8", "E": "2a2620", "N": "8a8270", "T": "f4efe2", "h": "a88c5c"}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x - 1 if x > 0 else x, y), rgba(pal[ch]))
    return im


WOOD = ["2a1c10", "47301a", "634426", "7d5a34"]
GLOW = ["2f8a3a", "5fd35a", "9be36a", "e2ffd0"]
LEAF = ["2f5a1e", "4f7f2c", "6fa33a"]


def staff_icon():
    im = img16()
    p = im.putpixel
    for t in range(11):
        x, y = 1 + t, 14 - t
        p((x, y), rgba(WOOD[3 if t % 3 else 2]))
        p((x + 1, y), rgba(WOOD[1]))
    for (x, y), c in {(12, 1): 1, (13, 1): 2, (14, 1): 1, (12, 2): 2, (13, 2): 3, (14, 2): 2, (12, 3): 1, (13, 3): 2, (14, 3): 1,
                      (13, 0): 0, (15, 2): 0, (11, 2): 0, (13, 4): 0}.items():
        p((x, y), rgba(GLOW[c]))
    for (x, y), c in {(10, 3): 1, (9, 3): 0, (10, 4): 2, (14, 5): 1, (15, 5): 0, (14, 6): 2}.items():  # leaves
        p((x, y), rgba(LEAF[c]))
    return im


def staff_parts():
    """UV sheet like gen_staffs' staff_parts: pole, end grain, twine grip, the glowing orb, leaves."""
    im = img16()
    p = im.putpixel
    for y in range(16):
        p((0, y), rgba(WOOD[2] if y % 5 else WOOD[1]))
        p((1, y), rgba(WOOD[3] if y % 6 != 2 else WOOD[2]))
    for q, c in (((2, 0), 2), ((3, 0), 1), ((2, 1), 1), ((3, 1), 0)):
        p(q, rgba(WOOD[c]))
    for x in range(4, 7):
        for y in range(7):
            p((x, y), rgba(["5a4632", "6e5842", "826a50"][(x + y) % 3]))
    for x in range(8, 16):  # orb faces (8,0)-(12,4) and sides (12,0)-(16,4)
        for y in range(4):
            edge = x in (8, 11, 12, 15) or y in (0, 3)
            p((x, y), rgba(GLOW[1] if edge else GLOW[3 if (x + y) % 3 == 0 else 2]))
    leaf = ["..L.", ".LLl", "LLll", "LLlg", ".Llg", "..g.", "..g.", "...."]
    pal = {"L": LEAF[2], "l": LEAF[1], "g": LEAF[0]}
    for y, row in enumerate(leaf):
        for x, ch in enumerate(row):
            if ch != ".":
                p((8 + x, 4 + y), rgba(pal[ch]))
    return im


def box(frm, to, side, end=None):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    return {"from": frm, "to": to, "faces": {names[f]: {"uv": end if f in "ud" and end else side, "texture": "#parts"} for f in "nsewud"}}


def plane(frm, to, uv, axis):
    f = ("east", "west") if axis == "x" else ("north", "south")
    return {"from": frm, "to": to, "faces": {n: {"uv": uv, "texture": "#parts"} for n in f}}


STAFF = [box([7.25, -4, 7.25], [8.75, 24, 8.75], [0, 0, 2, 16], [2, 0, 4, 2]),
         box([6.75, 6, 6.75], [9.25, 10, 9.25], [4, 0, 7, 4], [4, 4, 7, 7]),
         box([6.75, 22, 6.75], [9.25, 24, 9.25], [4, 0, 7, 4], [4, 4, 7, 7]),
         box([6, 24, 6], [10, 28, 10], [12, 0, 16, 4], [8, 0, 12, 4]),
         plane([6.4, 19, 7], [6.4, 25, 10], [8, 4, 12, 12], "x"),
         plane([7, 18.5, 9.6], [10, 24.5, 9.6], [8, 4, 12, 12], "z")]
STAFF_DISPLAY = {
    "thirdperson_righthand": {"rotation": [72, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [72, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, -90, 8], "translation": [1.5, -2, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 90, -8], "translation": [1.5, -2, 0], "scale": [0.6, 0.6, 0.6]},
}


def items():
    tex = A / "textures/item"
    knife().save(tex / "ancient_ritual_knife.png")
    heart().save(tex / "animal_heart.png")
    sinew().save(tex / "fine_sinew.png")
    skull().save(tex / "trophy_skull.png")
    staff_icon().save(tex / "verdant_staff.png")
    staff_parts().save(tex / "verdant_staff_parts.png")
    for item, parent in (("ancient_ritual_knife", "minecraft:item/handheld"), ("animal_heart", "minecraft:item/generated"),
                         ("fine_sinew", "minecraft:item/generated"), ("trophy_skull", "minecraft:item/generated")):
        write(A / f"models/item/{item}.json", {"parent": parent, "textures": {"layer0": M + "item/" + item}})
        write(A / f"items/{item}.json", {"model": {"type": "minecraft:model", "model": M + "item/" + item}})
    write(A / "models/item/verdant_staff.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/verdant_staff"}})
    write(A / "models/item/verdant_staff_in_hand.json", {"textures": {"parts": M + "item/verdant_staff_parts", "particle": M + "item/verdant_staff"},
                                                         "elements": STAFF, "display": STAFF_DISPLAY})
    write(A / "items/verdant_staff.json", {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
        "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": M + "item/verdant_staff"}}],
        "fallback": {"type": "minecraft:model", "model": M + "item/verdant_staff_in_hand"}}})


QUESTS = {  # OakQuests.QUESTS ids -> titles
    "berries": "Berries for the Roots", "mushrooms": "Gifts of the Moss", "fibre": "Threads for the Wreaths",
    "bark": "Shed Bark", "hides": "Warm Hides for Winter", "sinew": "Sinew for Bowstrings", "antlers": "Crowns of the Deer",
    "bronze": "Bright Metal", "bear": "The Bear's Coat", "iron": "Iron from the Bog", "giant_antler": "The Great Stag's Crown",
    "steel": "Steel for the Ritual Blade",
}

LANG = {
    "item.mechanical.verdant_staff": "Verdant Staff",
    "item.mechanical.ancient_ritual_knife": "Ancient Ritual Knife",
    "item.mechanical.animal_heart": "Animal Heart",
    "item.mechanical.fine_sinew": "Fine Sinew",
    "item.mechanical.trophy_skull": "Trophy Skull",
    "tribe.mechanical.oak_folk": "Children of the Oak",
    "entity.mechanical.tribesperson.role.oak_gatherer": "Gatherer",
    "entity.mechanical.tribesperson.role.oak_worshipper": "Worshipper",
    "entity.mechanical.tribesperson.role.oak_lightbearer": "Light-Bearer",
    "entity.mechanical.tribesperson.role.oak_keeper": "Keeper",
    "chat.mechanical.tribesperson.greet.oak_gatherer.0": "The Mother feeds us all. Take only what you need.",
    "chat.mechanical.tribesperson.greet.oak_gatherer.1": "Where a berry is picked, a bush is planted.",
    "chat.mechanical.tribesperson.greet.oak_gatherer.2": "Mind the glowcaps, they light the night for the old ones.",
    "chat.mechanical.tribesperson.greet.oak_worshipper.0": "Hush. She is listening.",
    "chat.mechanical.tribesperson.greet.oak_worshipper.1": "Older than the hills, and she still grows.",
    "chat.mechanical.tribesperson.greet.oak_worshipper.2": "Sit a while. Breathe with her.",
    "chat.mechanical.tribesperson.greet.oak_lightbearer.0": "No night falls on the Mother while we walk.",
    "chat.mechanical.tribesperson.greet.oak_lightbearer.1": "Keep your fire far from her roots.",
    "chat.mechanical.tribesperson.greet.oak_lightbearer.2": "Round and round, till the stars go out.",
    "chat.mechanical.tribesperson.greet.oak_keeper.0": "Speak, traveller. The Oak has tasks for those who listen.",
    "chat.mechanical.tribesperson.greet.oak_keeper.1": "Bring what the forest asks, and the forest gives back.",
    "chat.mechanical.tribesperson.greet.oak_keeper.2": "Your tribe's deeds are remembered here, good and ill.",
    "chat.mechanical.oak_folk.harm": "Stay your hand! You wound the Mother Oak!",
    "message.mechanical.oak_quest.done": "The Keeper is pleased: %s is done.",
    "gui.mechanical.oak_quests.hand_in": "Hand in",
    "gui.mechanical.oak_quests.standing": "Children of the Oak: %s (%s)",
    "gui.mechanical.oak_quests.hidden": "A task yet to come",
    "gui.mechanical.oak_quests.closed": "The Keeper turns away. \"You have harmed the Mother and her children. There is nothing more for you here.\"",
    "gui.mechanical.oak_quests.all_done": "\"Every task is done. The Mother remembers your tribe.\"",
    "gui.mechanical.oak_quests.bring": "Bring",
    "gui.mechanical.oak_quests.reward": "Reward",
}
LANG.update({f"quest.mechanical.oak.{k}": v for k, v in QUESTS.items()})


def lang():
    path = A / "lang/en_us.json"
    text = path.read_text(encoding="utf-8")
    data = json.loads(text)
    new = {k: v for k, v in LANG.items() if data.get(k) != v}
    if not new:
        return
    # raw insertion before the closing brace keeps the file's formatting
    body = text.rstrip().rstrip("}").rstrip()
    for k, v in new.items():
        if k in data:
            raise SystemExit(f"{k} already set to something else; edit it by hand")
        body += ",\n  " + json.dumps(k) + ": " + json.dumps(v, ensure_ascii=False)
    path.write_text(body + "\n}\n", encoding="utf-8")


if __name__ == "__main__":
    skins()
    items()
    lang()
    print("oak folk: skins, items, lang written")
