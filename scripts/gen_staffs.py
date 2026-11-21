"""Staffs (Mechanical.WALKING_STAFF / ELDERS_STAFF, arm pose in client/HoldPoses.java).

Walking Staff = Tool Rod / Twine / Tool Rod (crafting, a column). Elder's Staff = Feather Bone Feather over a Walking Staff.
Traders carry the Walking Staff, Elders the Elder's Staff (people/Tribesperson.setup). In the GUI, on the ground and in
frames they are flat icons; in hand a 3D model about body height, held upright with the tip at the ground.
Writes the textures (drawn here, colours from vanilla stick/bone/feather and the mod's twine), models, item definitions,
recipes and lang. Generator order: anywhere after gen_data.py, before gen_unlocks.py. Run: python scripts/gen_staffs.py
"""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
A, D = RES / "assets/mechanical", RES / "data"
M = "mechanical:"

WOOD = ["281e0b", "493615", "684e1e", "896727"]   # vanilla stick, dark -> light
TWINE = ["534237", "634e42", "826655"]            # twine.png
BONE = ["7b7e6b", "cbc6a5", "e8e5d2", "fcfbed"]    # vanilla bone
FEATHER = ["969696", "bebebe", "d3d3d3", "ffffff"]  # vanilla feather


def write(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def rgba(h):
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def icon(elder):
    """Diagonal staff, bottom-left to top-right like vanilla's stick, with a twine grip; the elder's has a bone head and feathers."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.putpixel
    top = 10 if elder else 12
    for t in range(top + 1):
        x, y = 1 + t, 14 - t
        grip = 5 <= t <= 7
        px((x, y), rgba(TWINE[2] if grip else WOOD[3 if t % 4 else 2]))
        px((x + 1, y), rgba(TWINE[0] if grip else WOOD[1]))
        if t == 0:
            px((x, y + 1), rgba(WOOD[0]))
    if not elder:
        for p, c in (((13, 1), 2), ((14, 1), 1), ((14, 2), 1), ((13, 2), 2)):  # knob
            px(p, rgba(WOOD[c]))
        return img
    for x in range(11, 15):  # bone head, eye sockets dark
        for y in range(1, 5):
            px((x, y), rgba(BONE[3 if y == 1 else 2 if x < 14 else 1]))
    px((12, 2), rgba(BONE[0]))
    px((13, 2), rgba(BONE[0]))
    px((11, 1), (0, 0, 0, 0))
    for y in range(5, 11):  # two feathers hanging from the binding
        px((10, y), rgba(FEATHER[3 if y < 9 else 1]))
        if y > 6:
            px((14, y), rgba(FEATHER[2 if y < 10 else 0]))
    px((12, 5), rgba(TWINE[2]))
    return img


def parts():
    """UV sheet for the in-hand models (see the UV comments in MODELS)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.putpixel
    for y in range(16):  # (0,0)-(2,16) pole side, wood grain
        px((0, y), rgba(WOOD[2] if y % 5 else WOOD[1]))
        px((1, y), rgba(WOOD[3] if y % 7 != 3 else WOOD[2]))
    for p, c in (((2, 0), 2), ((3, 0), 1), ((2, 1), 1), ((3, 1), 0)):  # (2,0)-(4,2) end grain
        px(p, rgba(WOOD[c]))
    for x in range(4, 7):  # (4,0)-(7,4) twine wrap side, (4,4)-(7,7) top
        for y in range(7):
            px((x, y), rgba(TWINE[(x + y) % 2 + (1 if y < 4 and y % 2 else 0)] if y < 4 else TWINE[1]))
    for x in range(8, 12):  # (8,0)-(12,4) carved bone head, face; (12,0)-(16,4) sides/top
        for y in range(4):
            px((x, y), rgba(BONE[3 if y == 0 else 2]))
            px((x + 4, y), rgba(BONE[2 if (x + y) % 3 else 1]))
    for p in ((9, 1), (10, 1)):
        px(p, rgba(BONE[0]))
    px((9, 3), rgba(BONE[1]))
    px((10, 3), rgba(BONE[1]))
    feather = ["..W.", ".WWl", ".WLl", "WWLl", "WLLg", "WLlg", ".Llg", "..g."]  # (8,4)-(12,12)
    pal = {"W": FEATHER[3], "L": FEATHER[2], "l": FEATHER[1], "g": FEATHER[0]}
    for y, row in enumerate(feather):
        for x, ch in enumerate(row):
            if ch != ".":
                px((8 + x, 4 + y), rgba(pal[ch]))
    for x in range(12, 15):  # (12,4)-(15,6) knob side, (12,6)-(15,9) knob top
        for y in range(4, 9):
            px((x, y), rgba(WOOD[1] if y in (4, 8) else WOOD[2]))
    return img


def box(frm, to, side, end=None, faces="nsewud"):
    names = {"n": "north", "s": "south", "e": "east", "w": "west", "u": "up", "d": "down"}
    return {"from": frm, "to": to, "faces": {names[f]: {"uv": end if f in "ud" and end else side, "texture": "#parts"}
                                             for f in faces}}


def plane(frm, to, uv, axis):
    """A flat, double-sided card (feathers)."""
    f = ("east", "west") if axis == "x" else ("north", "south")
    return {"from": frm, "to": to, "faces": {n: {"uv": uv, "texture": "#parts"} for n in f}}


POLE, END, WRAP, WRAP_END = [0, 0, 2, 16], [2, 0, 4, 2], [4, 0, 7, 4], [4, 4, 7, 7]
GRIP = box([6.75, 6, 6.75], [9.25, 10, 9.25], WRAP, WRAP_END)  # twine wrap where the hand holds it (y=8)
MODELS = {
    "walking_staff": [box([7.25, -4, 7.25], [8.75, 25, 8.75], POLE, END), GRIP,
                      box([6.75, 25, 6.75], [9.25, 27, 9.25], [12, 4, 15, 6], [12, 6, 15, 9])],  # knob
    "elders_staff": [box([7.25, -4, 7.25], [8.75, 24, 8.75], POLE, END), GRIP,
                     box([6.75, 22, 6.75], [9.25, 24, 9.25], WRAP, WRAP_END),  # binding under the head
                     box([6, 24, 6], [10, 28, 10], [12, 0, 16, 4], [12, 0, 16, 4]),  # carved bone head, faces set below
                     plane([6.6, 16, 7], [6.6, 23, 10], [8, 4, 12, 12], "x"),
                     plane([9.4, 15, 6], [9.4, 22, 9], [8, 4, 12, 12], "x"),
                     plane([7, 16.5, 9.4], [10, 23, 9.4], [8, 4, 12, 12], "z")],
}
MODELS["elders_staff"][3]["faces"]["north"]["uv"] = [8, 0, 12, 4]
MODELS["elders_staff"][3]["faces"]["south"]["uv"] = [8, 0, 12, 4]

# Upright in the hand, tip just above the ground (ItemInHandLayer + HumanoidModel ITEM arm angle: rotation X 72 turns the
# model's Y axis straight up; the grip at model y=8 sits in the fist). First person: held upright at the right edge.
DISPLAY = {
    "thirdperson_righthand": {"rotation": [72, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [72, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "firstperson_righthand": {"rotation": [0, -90, 8], "translation": [1.5, -2, 0], "scale": [0.6, 0.6, 0.6]},
    "firstperson_lefthand": {"rotation": [0, 90, -8], "translation": [1.5, -2, 0], "scale": [0.6, 0.6, 0.6]},
}
NAMES = {"walking_staff": "Walking Staff", "elders_staff": "Elder's Staff"}

tex = A / "textures/item"
tex.mkdir(parents=True, exist_ok=True)
icon(False).save(tex / "walking_staff.png")
icon(True).save(tex / "elders_staff.png")
parts().save(tex / "staff_parts.png")

lang_path = A / "lang/en_us.json"
lang = json.loads(lang_path.read_text(encoding="utf-8"))
for item, elements in MODELS.items():
    write(A / f"models/item/{item}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": M + "item/" + item}})
    write(A / f"models/item/{item}_in_hand.json", {"textures": {"parts": M + "item/staff_parts", "particle": M + "item/" + item},
                                                   "elements": elements, "display": DISPLAY})
    write(A / f"items/{item}.json", {"model": {"type": "minecraft:select", "property": "minecraft:display_context",
        "cases": [{"when": ["gui", "ground", "fixed", "on_shelf"], "model": {"type": "minecraft:model", "model": M + "item/" + item}}],
        "fallback": {"type": "minecraft:model", "model": M + "item/" + item + "_in_hand"}}})
    lang[f"item.mechanical.{item}"] = NAMES[item]
lang_path.write_text(json.dumps(lang, indent=2) + "\n", encoding="utf-8")

write(D / "mechanical/recipe/walking_staff.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["R", "T", "R"], "key": {"R": M + "tool_rod", "T": M + "twine"}, "result": {"id": M + "walking_staff", "count": 1}})
write(D / "mechanical/recipe/elders_staff.json", {"type": "minecraft:crafting_shaped", "category": "equipment",
    "pattern": ["FBF", " S "], "key": {"F": "minecraft:feather", "B": "minecraft:bone", "S": M + "walking_staff"},
    "result": {"id": M + "elders_staff", "count": 1}})
