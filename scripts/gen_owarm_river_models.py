"""The beaver's and the otter's models and skins, each from one table (wildlife/river/RiverFauna, docs/fauna/river.md):

- src/main/java/com/mechanical/client/wildlife/river/RiverGeometry.java: every part and box (generated, do not edit)
- textures/entity/wildlife/<animal>.png and the skinned carcass skins in textures/entity/skinned/wildlife/

Beaver: a round, hunched body (narrow shoulders, a big rump, the back highest over the hips), a blunt broad head with
puffy pale cheeks, small round ears and small eyes, the big orange incisors under a dark nose, little clawed hands,
big webbed hind feet and the broad flat paddle of a tail, dark and netted with scales. Rich brown, darker on the back.
Otter: a long, low, sinuous tube of a body, a flat broad head with small ears, whisker pads and a dark nose, the pale
cream throat and chest, short legs with webbed feet and a thick tapering tail; glossy dark brown, paler beneath.
Part names are the ones BeaverModel / OtterModel look up (body, head, stick, tail, tail_tip, fish, the four legs).
Usage: python scripts/gen_owarm_river_models.py (after gen_fauna_river_textures.py, which paints the old skins). Deterministic.
"""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from gen_savanna_models import CLEAR, Model, rel, side_of  # noqa: E402
from wildlife_paint import h, lerp, save, shade, smooth  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
JAVA = ROOT / "src/main/java/com/mechanical/client/wildlife/river/RiverGeometry.java"


# ============================================================================ beaver
def beaver_model():
    m = Model("Beaver")
    m.part("body", "root", (0, 19, 0))
    m.box("body", "chest", -3, -2.5, -5, 6, 5, 4)          # narrow shoulders
    m.box("body", "mid", -4, -3.5, -1.5, 8, 6, 4)
    m.box("body", "rump", -4, -4, 1.5, 8, 6, 4)            # the big round rump, the highest point
    m.box("body", "rump_back", -3, -3, 5.5, 6, 4, 1)
    m.box("body", "hump", -2.5, -5, 0, 5, 1, 5)             # rounds the hunched back off
    m.box("body", "belly", -3, 2.5, -3.5, 6, 1, 7)
    m.box("body", "neck", -2.5, -2, -6.5, 5, 4, 2)
    m.part("head", "body", (0, 0, -5.5))
    m.box("head", "skull", -2.5, -2.5, -3.5, 5, 4, 4)
    m.box("head", "crown", -1.5, -3.5, -3, 3, 1, 3)
    m.box("head", "cheeks", -3, 0.5, -3, 6, 2, 2)          # puffy cheeks
    m.box("head", "muzzle", -2, -1, -5.3, 4, 3, 2)
    m.box("head", "nose", -1, -1.4, -5.6, 2, 1, 1)
    m.box("head", "teeth", -1, 2, -5.1, 2, 2, 1)           # the orange incisors
    m.box("head", "chin", -1.5, 2, -4.2, 3, 1, 2)
    m.box("head", "ear_r", -3.2, -3.2, -1.2, 2, 2, 1)
    m.box("head", "ear_l", 1.2, -3.2, -1.2, 2, 2, 1)
    m.part("stick", "head")
    m.box("stick", "stick", -5.5, 2.6, -4.4, 11, 1, 1)     # crosswise behind the incisors
    m.part("tail", "body", (0, 2.5, 5), (-0.3, 0, 0))      # low, so the paddle lies on the ground
    m.box("tail", "tail_root", -1.5, -1, -1, 3, 2, 2)
    m.box("tail", "paddle1", -2, -0.5, 1, 4, 1, 1)         # the paddle: an oval, flat and scaly
    m.box("tail", "paddle2", -3, -0.5, 2, 6, 1, 4)
    m.box("tail", "paddle3", -2, -0.5, 6, 4, 1, 1)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 2.2, 20.5, -3))
        m.box(f"{s}_front_leg", "arm", -1, -0.5, -1, 2, 3, 2)
        m.box(f"{s}_front_leg", "hand", -1, 2.5, -2.2, 2, 1, 2)
        m.part(f"{s}_hind_leg", "root", (side * 3, 20, 3))
        m.box(f"{s}_hind_leg", "thigh", -1.5, -2.5, -2, 3, 5, 4)
        m.box(f"{s}_hind_leg", "shank", -1, 2, -1, 2, 1, 2)
        m.box(f"{s}_hind_leg", "foot", -2, 3, -4, 4, 1, 5)  # the big webbed hind foot
    return m


BV = dict(back=(70, 42, 24), coat=(116, 72, 40), flank=(134, 88, 52), belly=(148, 108, 72), cheek=(156, 120, 86),
          cheek_lt=(176, 144, 108), muzzle=(110, 80, 58), dark=(46, 30, 20), nose=(24, 18, 16), eye=(12, 9, 8),
          glint=(150, 146, 140), rim=(60, 40, 28), tooth=(226, 122, 34), tooth_dk=(168, 78, 22), ear=(60, 38, 24),
          tail=(46, 42, 42), scale=(76, 70, 68), scale_line=(26, 22, 22), foot=(54, 42, 36), web=(84, 70, 62),
          claw=(28, 22, 20), bark=(110, 86, 58), bark_dk=(74, 56, 38), wood=(222, 196, 146))


def bv_coat(face, P, lo=14, hi=22.5):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], lo, hi)
    c = lerp(BV["back"], BV["coat"], smooth(t / 0.45))
    c = lerp(c, BV["flank"], (t - 0.45) / 0.3)
    c = lerp(c, BV["belly"], (t - 0.75) / 0.25)
    # dark guard hairs and a sheen: the coat reads as fur, not felt
    g = h(int(P[0] * 2 + 40), int(P[1] * 2 + P[2] * 5 + 90), 21)
    if g < 0.08:
        c = shade(c, 0.85)
    elif g > 0.93:
        c = shade(c, 1.16)
    return c


def beaver_colour(v, mat, face, p, P, b):
    x, y, z = p
    if mat in ("chest", "mid", "rump", "rump_back", "hump", "belly", "neck", "thigh"):
        c = bv_coat(face, P)
        if face == "top":
            c = lerp(c, BV["dark"], 0.12 * h(int(P[0] * 2), int(P[2] * 2), 22))  # mottled darker spine
        return c
    if mat in ("skull", "crown"):
        if face in ("left", "right") and mat == "skull":
            zi, yi = z - b["z"], y
            if -1.6 < yi < 0.4 and 0.4 < zi < 1.6:
                # small black eye, a glint in the upper front corner
                return BV["glint"] if (yi < -0.5 and zi < 1.5) else BV["eye"]
            if -1.6 < yi < 0.4 and 1.6 < zi < 2.6:
                return BV["rim"]
            if yi > 0:
                return lerp(BV["coat"], BV["cheek"], 0.6)
        if face == "top" or mat == "crown":
            return lerp(BV["back"], BV["coat"], 0.35 * h(int(x * 2), int(z * 2), 23))
        if face == "front":
            return lerp(BV["coat"], BV["muzzle"], rel(y, -3, 1))
        return BV["coat"]
    if mat == "cheeks":
        if face == "top":
            return lerp(BV["coat"], BV["cheek"], 0.4)
        if face == "front" or face == "bottom":
            return BV["cheek"]
        return lerp(lerp(BV["coat"], BV["cheek"], 0.6), BV["cheek_lt"], rel(y, 0.5, 2.5))
    if mat == "muzzle":
        if face in ("left", "right"):
            if y > -0.5 and int((z + 10) * 1) % 2 == 0 and h(int(z * 3), int(y * 3), 24) < 0.5:
                return BV["dark"]  # whisker spots
            return BV["cheek_lt"] if y > 0 else BV["cheek"]
        if face == "front":
            return BV["cheek_lt"] if y > 0.5 else BV["muzzle"]
        return BV["muzzle"] if face == "top" else BV["cheek_lt"]
    if mat == "nose":
        if face == "front":
            return BV["nose"]
        return lerp(BV["nose"], BV["muzzle"], 0.35) if face == "top" else BV["nose"]
    if mat == "teeth":
        if face == "front":
            return BV["tooth_dk"] if y < 2 else BV["tooth"]
        return BV["tooth_dk"]
    if mat == "chin":
        return BV["cheek"] if face != "bottom" else lerp(BV["cheek"], BV["dark"], 0.3)
    if mat in ("ear_r", "ear_l"):
        outer = (mat == "ear_r" and x < b["x"] + 1) or (mat == "ear_l" and x > b["x"] + b["w"] - 1)
        if y < b["y"] + 1 and outer and face in ("front", "back", "top", "left", "right"):
            return CLEAR  # a rounded little ear
        return BV["dark"] if face == "front" else BV["ear"]
    if mat == "stick":
        if face in ("left", "right"):
            return BV["wood"]  # pale gnawed ends
        if int(x + 20) % 3 == 0:
            return BV["bark_dk"]
        return lerp(BV["bark"], BV["bark_dk"], 0.4 * h(int(x * 2), int(y + z), 25))
    if mat == "tail_root":
        return bv_coat(face, P) if z < b["z"] + 1 else lerp(BV["tail"], BV["coat"], 0.35)
    if mat.startswith("paddle"):
        if face in ("top", "bottom"):
            i, j = int(x + 20), int(z + 20)
            # a net of diamond scales: light centres, dark seams
            c = BV["scale"] if (i + j) % 2 == 0 else BV["tail"]
            if (i + 2 * j) % 4 == 0:
                c = BV["scale_line"]
            return c if face == "top" else shade(c, 0.85)
        return BV["scale_line"]
    if mat == "arm":
        return lerp(bv_coat(face, P), BV["foot"], rel(y, 0.5, 2.5))
    if mat == "shank":
        return lerp(BV["coat"], BV["foot"], 0.6)
    if mat in ("hand", "foot"):
        front = z < b["z"] + 1
        if face == "front" or (face == "top" and front):
            return BV["claw"] if int(x - b["x"]) % 2 == 0 or mat == "hand" else BV["web"]
        if face == "top":
            return BV["foot"] if int(x - b["x"]) % 2 == 0 else BV["web"]  # long toes, webs between
        return BV["foot"] if face != "bottom" else BV["claw"]
    return BV["coat"]


# ============================================================================ otter
def otter_model():
    m = Model("Otter")
    m.part("body", "root", (0, 19.5, 0))
    m.box("body", "chest", -2, -2, -6, 4, 4, 4)
    m.box("body", "trunk", -2.5, -2.5, -2.5, 5, 5, 9)      # long and low, heaviest at the hips
    m.box("body", "arch", -1.5, -3.5, -1, 3, 1, 6)
    m.box("body", "under", -1.5, 2.5, -4, 3, 1, 9)
    m.box("body", "rear", -1.5, -1.5, 6.5, 3, 3, 1)
    m.box("body", "neck", -2, -2, -8.5, 4, 4, 3)           # thick neck, as broad as the head
    m.part("head", "body", (0, -0.5, -7.5))
    m.box("head", "skull", -2.5, -2, -3.5, 5, 3, 4)        # flat and broad
    m.box("head", "muzzle", -2, -1, -5.5, 4, 2, 2)         # the whisker pads
    m.box("head", "nose", -1, -1.3, -5.8, 2, 1, 1)
    m.box("head", "chin", -1.5, 1, -5, 3, 1, 2)
    m.box("head", "ear_r", -3, -2.5, -1.5, 1, 1, 1)
    m.box("head", "ear_l", 2, -2.5, -1.5, 1, 1, 1)
    m.part("fish", "head")
    m.box("fish", "fish", -3.5, 0.5, -4.6, 7, 2, 1)        # crosswise in the jaws
    m.box("fish", "fin", 3.5, 0, -4.6, 1, 3, 1)
    m.part("tail", "body", (0, -0.5, 6.5), (-0.25, 0, 0))
    m.box("tail", "tail1", -2, -1.5, -1, 4, 3, 3)
    m.box("tail", "tail2", -1.5, -1, 2, 3, 2, 3)
    m.part("tail_tip", "tail", (0, 0, 5), (-0.1, 0, 0))
    m.box("tail_tip", "tail3", -1, -1, 0, 2, 2, 3)
    m.box("tail_tip", "tail4", -0.5, -0.5, 3, 1, 1, 2)
    for side, s in ((-1, "right"), (1, "left")):
        m.part(f"{s}_front_leg", "root", (side * 1.8, 21, -4))
        m.box(f"{s}_front_leg", "foreleg", -1, -0.5, -1, 2, 3, 2)
        m.box(f"{s}_front_leg", "paw", -1, 2, -2, 2, 1, 2)
        m.part(f"{s}_hind_leg", "root", (side * 2.2, 20.5, 4.5))
        m.box(f"{s}_hind_leg", "haunch", -1, -1.5, -1.5, 2, 3, 3)
        m.box(f"{s}_hind_leg", "shank", -1, 1.5, -1, 2, 1, 2)
        m.box(f"{s}_hind_leg", "hind_foot", -1.5, 2.5, -3, 3, 1, 4)
    return m


OT = dict(back=(62, 40, 26), coat=(92, 62, 40), flank=(130, 94, 62), belly=(168, 140, 106), cream=(212, 192, 156),
          cream_dk=(188, 164, 126), sheen=(150, 118, 86), nose=(22, 18, 18), eye=(10, 8, 8), glint=(136, 134, 132),
          pad=(170, 150, 122), whisker=(70, 56, 44), foot=(46, 34, 26), web=(80, 62, 48), claw=(26, 20, 18),
          ear=(42, 28, 20), fish_back=(60, 82, 74), fish=(184, 192, 186), fish_belly=(228, 230, 222), fish_eye=(16, 16, 16),
          fin=(96, 108, 100))


def ot_coat(face, P, lo=16, hi=22.5):
    t = 0.0 if face == "top" else 1.0 if face == "bottom" else rel(P[1], lo, hi)
    c = lerp(OT["back"], OT["coat"], smooth(t / 0.4))
    c = lerp(c, OT["flank"], (t - 0.4) / 0.35)
    c = lerp(c, OT["belly"], (t - 0.75) / 0.25)
    if face == "top" and abs(P[0]) < 1:
        c = lerp(c, OT["sheen"], 0.45 + 0.25 * h(int(P[2] * 2), 3, 31))  # the wet gloss along the spine
    elif side_of(face) and 0.15 < t < 0.3:
        c = lerp(c, OT["sheen"], 0.3)
    if h(int(P[0] * 2 + 40), int(P[1] * 2 + P[2] * 5 + 90), 32) < 0.1:
        c = shade(c, 0.8)
    return c


def otter_colour(v, mat, face, p, P, b):
    x, y, z = p
    if mat in ("chest", "neck"):
        # the pale cream throat and chest, running back along the underside
        if face == "front" or face == "bottom" or (side_of(face) and P[1] > 20.6):
            return lerp(OT["cream"], OT["cream_dk"], 0.4 * h(int(P[0] * 2), int(P[2] * 2), 33))
        return ot_coat(face, P)
    if mat == "under":
        return lerp(OT["cream_dk"], OT["belly"], rel(P[2], -4, 4))
    if mat in ("trunk", "arch", "rear", "haunch"):
        c = ot_coat(face, P)
        if mat == "trunk" and face == "bottom":
            return lerp(OT["cream_dk"], OT["belly"], rel(P[2], -2, 3))
        return c
    if mat == "skull":
        if face in ("left", "right"):
            zi = z - b["z"]
            if -2 < y < -1 and zi < 2:
                return OT["glint"] if zi < 1 else OT["eye"]
            if y > 0 and zi < 2.5:
                return OT["cream_dk"]  # pale lower cheeks
            if y > 0:
                return OT["flank"]
            return OT["coat"]
        if face == "front":
            if -2 < y < -1 and abs(x) > 1.6:
                return OT["eye"]  # the eyes sit high and look forward
            return OT["coat"] if y < 0 else OT["cream"]
        if face == "bottom":
            return OT["cream"]
        return lerp(OT["back"], OT["sheen"], 0.35 * h(int(x * 2), int(z * 2), 34))
    if mat == "muzzle":
        if face in ("left", "right", "front"):
            if y > 0:
                return OT["cream"]
            if face != "front" and int(z * 2 + 20) % 2 == 0:
                return OT["whisker"]  # whisker roots on the swollen pads
            return OT["pad"]
        return lerp(OT["coat"], OT["pad"], 0.35) if face == "top" else OT["cream"]
    if mat == "nose":
        return OT["nose"]
    if mat == "chin":
        return OT["cream"]
    if mat in ("ear_r", "ear_l"):
        return OT["cream_dk"] if face == "top" else OT["ear"]
    if mat == "fish":
        if face in ("front", "back"):
            if x < -2.5 and y < 1.5:
                return OT["fish_eye"]
            return OT["fish_back"] if y < 1 else OT["fish"] if y < 2 else OT["fish_belly"]
        return OT["fish_back"] if face == "top" else OT["fish_belly"] if face == "bottom" else OT["fish"]
    if mat == "fin":
        return OT["fin"] if 0.5 < y - b["y"] < 2.5 else shade(OT["fin"], 0.7)
    if mat in ("tail1", "tail2", "tail3", "tail4"):
        k = {"tail1": 0.0, "tail2": 0.3, "tail3": 0.6, "tail4": 0.9}[mat]
        if face == "bottom":
            return lerp(OT["belly"], OT["coat"], k)
        c = lerp(OT["coat"], OT["back"], k)
        if face == "top" and abs(x) < 0.6:
            c = lerp(c, OT["sheen"], 0.4)
        return c
    if mat == "foreleg":
        return lerp(ot_coat(face, P), OT["foot"], rel(y, 0, 2.5))
    if mat == "shank":
        return OT["foot"]
    if mat in ("paw", "hind_foot"):
        if face == "front":
            return OT["claw"]
        if face == "top":
            return OT["foot"] if int(x - b["x"]) % 2 == 0 or mat == "paw" else OT["web"]
        return OT["foot"]
    return OT["coat"]


# ============================================================================
def main():
    models = [(beaver_model(), "beaver", beaver_colour, "beaver", ("teeth", "nose", "hand", "foot", "stick", "paddle1", "paddle2", "paddle3")),
              (otter_model(), "otter", otter_colour, "otter", ("nose", "paw", "hind_foot", "fish", "fin"))]
    methods = []
    for m, method, colour, name, keep in models:
        m.pack()
        methods.append(m.java(method))
        img = m.paint(colour, None)
        save(img, f"wildlife/{name}.png")
        save(m.skinned(img, keep), f"skinned/wildlife/{name}.png")
        print(f"{m.name}: {len(m.parts)} parts, {len(m.boxes)} boxes, {m.size[0]}x{m.size[1]}")
    JAVA.write_text(f"""package com.mechanical.client.wildlife.river;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** GENERATED by scripts/gen_owarm_river_models.py (with the skins it paints): the beaver's and the otter's parts and boxes. Do not edit. */
public final class RiverGeometry {{
{chr(10).join(methods)}
    private RiverGeometry() {{}}
}}
""", encoding="utf-8")


if __name__ == "__main__":
    main()
