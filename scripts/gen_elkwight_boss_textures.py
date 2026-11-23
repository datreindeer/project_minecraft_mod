"""Paint the Elkwight boss (scripts/gen_elkwight_boss.py, com.mechanical.wildlife.elkwight; docs/elkwight-plan.md):

- textures/entity/wildlife/elkwight.png (128x128) on ElkwightModel's boxes: dark hairy giant's legs with black hooves, a
  brown-furred torso under a shaggy grey mantle, a hide belt, an elk's head (darker muzzle, black nose, the dewlap), pale
  bone antlers darkening to the base, the axe: dark wood haft with leather wraps, grey iron bits with bright edges and
  dark green rune grooves
- elkwight_glow.png: the emissive layer (eyes and the axe runes in bright green), and the skinned carcass texture
  (textures/entity/skinned/wildlife/elkwight.png: red flesh, the antlers and hooves kept)
- blocks: elkwight_axe_haft, elkwight_axe_blade, elkwight_axe_runes (glow overlay of the trophy), elkwight_antler,
  elkwight_skull (the wall trophy)
- items: Elkwight Hide, Elkwight Bone, Elkwight Cloak, Bone Charm, the spawn egg; the cloak's cape
  (textures/entity/cloak/elkwight_cloak.png) and equipment layer (textures/entity/equipment/humanoid/elkwight_hide.png),
  recoloured from the Otter Fur Cape's

Usage: python scripts/gen_elkwight_boss_textures.py (needs the merged Minecraft jar for the vanilla bases). Deterministic.
"""
import importlib.util
from pathlib import Path

from PIL import Image

_HERE = Path(__file__).resolve().parent
_spec = importlib.util.spec_from_file_location("ugt", _HERE / "gen_fauna_underground_textures.py")
ugt = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(ugt)
wit = ugt.wit
RES = ugt.RES
h, shade, lerp, faces, recolour, vanilla, mod = ugt.h, ugt.shade, ugt.lerp, ugt.faces, ugt.recolour, ugt.vanilla, ugt.mod

# part -> (u, v, w, h, d): ElkwightModel.createBodyLayer
UV = {"body": (0, 0, 10, 13, 6), "belt": (0, 20, 11, 5, 7), "mantle": (90, 0, 12, 4, 7), "neck": (70, 0, 5, 5, 5),
      "skull": (40, 0, 6, 6, 7), "muzzle": (40, 14, 4, 5, 7), "dewlap": (64, 14, 1, 3, 2), "ear": (72, 14, 3, 1, 2),
      "beam": (0, 60, 4, 2, 2), "palm": (0, 64, 13, 10, 1), "tine": (52, 60, 1, 5, 1), "arm": (24, 34, 4, 13, 4),
      "leg": (0, 34, 5, 12, 5), "haft": (0, 80, 2, 2, 30), "socket": (66, 96, 3, 4, 4), "blade": (66, 80, 1, 8, 7)}

P = dict(fur=(62, 46, 34), fur_dark=(40, 30, 24), chest=(104, 82, 62), skin=(98, 86, 76), hoof=(28, 24, 22),
         belt=(122, 92, 62), strap=(66, 48, 32), mantle=(78, 74, 68), mantle_dark=(50, 48, 44), head=(94, 68, 46),
         muzzle=(70, 50, 36), nose=(30, 26, 22), dewlap=(62, 46, 32), ear_in=(132, 100, 78), bone=(218, 206, 178),
         bone_dark=(164, 148, 118), bone_tip=(238, 232, 212), wood=(62, 44, 30), wrap=(38, 30, 22), iron=(72, 78, 84),
         edge=(168, 176, 184), groove=(36, 64, 48), socket=(54, 58, 64))
# skinned: the hide off, red-brown flesh and pale fat; bone, hooves and horn stay
S = dict(P, fur=(146, 52, 46), fur_dark=(112, 36, 34), chest=(168, 74, 64), skin=(150, 56, 48), belt=(176, 84, 70),
         strap=(120, 40, 36), mantle=(190, 150, 130), mantle_dark=(150, 96, 84), head=(150, 60, 52), muzzle=(130, 50, 44),
         dewlap=(120, 44, 40), ear_in=(170, 90, 80))
GLOW, EYE = (120, 255, 150), (200, 255, 210)


def colour(P):
    def c(part, face, i, j, fw, fh):
        n = h(i, j, 7)
        if part == "leg":
            if face in ("bottom",) or (face not in ("top",) and j >= fh - 2):
                return P["hoof"]
            return lerp(P["fur_dark"], P["fur"], 0.3 + 0.4 * n)
        if part == "body":
            if face == "front" and 2 < i < fw - 3 and 1 < j < 10:
                return lerp(P["chest"], P["fur"], 0.3 * n)
            return lerp(P["fur"], P["fur_dark"], 0.5 * n + (0.3 if j > 9 else 0))
        if part == "belt":
            if face in ("front", "back", "left", "right") and j in (0, 1):
                return P["strap"]
            if face == "front" and 3 <= i <= 7 and j >= 2:  # the loin flap
                return lerp(P["belt"], P["strap"], 0.25 * n)
            return lerp(P["belt"], P["strap"], 0.4 * n)
        if part == "mantle":
            shag = h(i, j // 2, 9)
            return lerp(P["mantle"], P["mantle_dark"], 0.6 * shag + (0.3 if j == fh - 1 else 0))
        if part == "arm":
            if face != "top" and j >= fh - 3:  # bare wrists and hands
                return lerp(P["skin"], P["fur_dark"], 0.3 * n)
            return lerp(P["fur"], P["fur_dark"], 0.5 * n)
        if part == "neck":
            return lerp(P["fur"], P["head"], 0.5 + 0.3 * n)
        if part == "skull":
            return lerp(P["head"], P["fur_dark"], 0.3 * n + (0.25 if face == "top" else 0))
        if part == "muzzle":
            if face == "front":
                return P["nose"] if 0 < i < 3 and 1 <= j <= 3 else P["muzzle"]
            return lerp(P["muzzle"], P["head"], 0.35 * n + (0.3 if j < 2 else 0))
        if part == "dewlap":
            return P["dewlap"]
        if part == "ear":
            return P["ear_in"] if face == "bottom" else P["head"]
        if part in ("beam", "palm", "tine"):
            if part == "beam":
                return lerp(P["bone_dark"], P["bone"], 0.4 * n)
            if part == "tine":
                return lerp(P["bone"], P["bone_tip"], j / max(1, fh - 1) * -1 + 1 if face != "top" else 1)
            # the palm: pale, ridged, darker towards the beam and at its rim
            ridge = 0.2 if (i + j) % 5 == 0 else 0
            return lerp(P["bone"], P["bone_dark"], 0.25 * n + ridge)
        if part == "haft":
            along = j if face in ("top", "bottom") else i
            return P["wrap"] if (along // 3) % 4 == 0 else lerp(P["wood"], P["wrap"], 0.3 * n)
        if part == "socket":
            return lerp(P["socket"], P["iron"], 0.4 * n)
        if part == "blade":
            if face in ("left", "right"):
                edge = i == 0 if face == "right" else i == fw - 1  # the bit's cutting edge faces away from the haft
                if edge or j in (0, fh - 1) and n > 0.5:
                    return P["edge"]
                if rune(i, j, face):
                    return P["groove"]
            return lerp(P["iron"], P["socket"], 0.4 * n)
        return P["fur"]
    return c


def rune(i, j, face):
    """A carved rune on each bit's flat face (7x8): a staff with two branches, like a bind-rune."""
    x = i if face == "right" else 6 - i
    return (x == 3 and 1 <= j <= 6) or (x == 4 and j == 2) or (x == 5 and j == 3) or (x == 2 and j == 4) or (x == 1 and j == 5)


def entity(P):
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    return ugt.paint(im, UV, colour(P), 41)


def glow():
    """Transparent but for the eyes (sides of the skull, near the front) and the runes on the axe bits."""
    im = Image.new("RGBA", (128, 128), (0, 0, 0, 0))
    px = im.load()
    sk = faces(*UV["skull"])
    x0, y0, fw, fh = sk["right"]
    for dx, dy, c in ((fw - 2, 2, EYE), (fw - 3, 2, GLOW)):
        px[x0 + dx, y0 + dy] = (*c, 255)
    x0, y0, fw, fh = sk["left"]
    for dx, dy, c in ((1, 2, EYE), (2, 2, GLOW)):
        px[x0 + dx, y0 + dy] = (*c, 255)
    x0, y0, fw, fh = sk["front"]  # seen from in front too: the outer corners of the face
    for dx in (0, fw - 1):
        px[x0 + dx, y0 + 2] = (*EYE, 255)
    for face in ("left", "right"):
        x0, y0, fw, fh = faces(*UV["blade"])[face]
        for j in range(fh):
            for i in range(fw):
                if rune(i, j, face):
                    px[x0 + i, y0 + j] = (*GLOW, 255)
    return im


def blocks():
    out = RES / "block"
    out.mkdir(parents=True, exist_ok=True)
    recolour(vanilla("block/stripped_dark_oak_log"), [(24, 18, 12), (44, 32, 22), (66, 48, 32), (90, 66, 44)], salt=3).save(out / "elkwight_axe_haft.png")
    blade = recolour(vanilla("block/iron_block"), [(40, 44, 50), (64, 70, 76), (90, 96, 104), (150, 158, 166), (196, 204, 212)], salt=5)
    px = blade.load()
    for i in range(16):  # bright edges along both bits, dark rune grooves in the middle
        px[i, 0] = px[i, 15] = (184, 192, 200, 255)
    runes = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rp = runes.load()
    glyph = ["................",
             "................",
             "...#.......#....",
             "...##.....##....",
             "...#.#...#.#....",
             "...#..#.#..#....",
             "...#...#...#....",
             "...#..#.#..#....",
             "...#.#...#.#....",
             "...##.....##....",
             "...#.......#....",
             "................",
             "......####......",
             "................",
             "................",
             "................"]
    for y, row in enumerate(glyph):
        for x, ch in enumerate(row):
            if ch == "#":
                rp[x, y] = (*GLOW, 255)
                px[x, y] = (36, 64, 48, 255)
    blade.save(out / "elkwight_axe_blade.png")
    runes.save(out / "elkwight_axe_runes.png")
    recolour(vanilla("block/bone_block_side"), [(150, 134, 104), (190, 176, 146), (218, 206, 178), (240, 234, 216)], salt=7).save(out / "elkwight_antler.png")
    recolour(vanilla("block/bone_block_top"), [(120, 108, 86), (176, 162, 134), (210, 198, 170), (236, 228, 208)], salt=8).save(out / "elkwight_skull.png")


BONE_CHARM = ["................",
              "......3333......",
              ".....3....3.....",
              "....3......3....",
              "....3......3....",
              ".....3....3.....",
              "......3..3......",
              "......1221......",
              ".....122221.....",
              ".....12g221.....",
              ".....122g21.....",
              ".....12g221.....",
              ".....122221.....",
              "......1221......",
              ".......11.......",
              "................"]


def items():
    it = RES / "item"
    HIDE = [(30, 20, 14), (64, 46, 32), (98, 72, 50), (130, 100, 72), (162, 132, 100)]
    out = {
        "elkwight_hide": recolour(mod("item/raw_hide"), HIDE, salt=11),
        "elkwight_bone": recolour(vanilla("item/bone"), [(110, 96, 72), (170, 156, 126), (214, 202, 174), (240, 234, 214)], salt=12),
        "elkwight_cloak": recolour(mod("item/otter_fur_cape"), HIDE, salt=13),
        "bone_charm": wit.from_grid(BONE_CHARM, {"3": "#5a3e26", "1": "#8c7a5a", "2": "#e2d6b6", "g": "#4ad07a"}),
        # dark brown with pale antlers and green eyes
        "elkwight_spawn_egg": wit.egg(["#7a5a40", "#5e4430", "#463224", "#33241a", "#281c14", "#160f0a"],
                                      [(3, 3, "#e8dcc0"), (4, 2, "#e8dcc0"), (11, 2, "#e8dcc0"), (12, 3, "#e8dcc0"),
                                       (6, 6, "#7cff9a"), (9, 6, "#7cff9a")]),
    }
    for name, im in out.items():
        im.save(it / f"{name}.png")
    HIDE_E = [(24, 16, 12), (52, 38, 26), (84, 62, 42), (116, 88, 62), (146, 116, 86)]
    recolour(mod("entity/cloak/otter_fur_cape"), HIDE_E, salt=14).save(RES / "entity/cloak/elkwight_cloak.png")
    recolour(mod("entity/equipment/humanoid/otter_fur"), HIDE_E, salt=15).save(RES / "entity/equipment/humanoid/elkwight_hide.png")


def main():
    for rel, pal in (("entity/wildlife/elkwight.png", P), ("entity/skinned/wildlife/elkwight.png", S)):
        out = RES / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        entity(pal).save(out)
    glow().save(RES / "entity/wildlife/elkwight_glow.png")
    blocks()
    items()
    print("elkwight textures ok")


if __name__ == "__main__":
    main()
