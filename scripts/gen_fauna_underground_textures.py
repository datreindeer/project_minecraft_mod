"""Paint the underground fauna (scripts/gen_fauna_underground.py, com.mechanical.wildlife.underground): the Giant Centipede's
entity texture and its skinned (butchered) version, the Chitin Vest equipment layer and the 16x16 item textures.

Usage: python scripts/gen_fauna_underground_textures.py
The UV table below mirrors the texOffs and box sizes in GiantCentipedeModel (client/wildlife/underground). Deterministic:
hashes only, no randomness.
"""
import importlib.util
from pathlib import Path

from PIL import Image

_spec = importlib.util.spec_from_file_location("wit", Path(__file__).resolve().parent / "gen_wildlife_item_textures.py")
wit = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wit)
RES = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"


def h(x, y, salt):
    v = (x * 73856093) ^ (y * 19349663) ^ (salt * 83492791)
    v = (v ^ (v >> 13)) * 1274126177 & 0xFFFFFFFF
    return (v & 0xFFFF) / 0xFFFF


def shade(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c)


def lerp(a, b, t):
    return tuple(round(x + (y - x) * t) for x, y in zip(a, b))


# Scolopendra: glossy dark red-brown back plates with a black rear rim, amber-orange legs, a red-orange head, amber antennae,
# black-tipped venom claws, a tan underside.
C = dict(plate=(104, 40, 26), rim=(38, 18, 14), shine=(156, 70, 40), side=(128, 58, 32), belly=(186, 150, 96),
         head=(170, 64, 30), leg=(214, 150, 52), leg_dark=(160, 100, 34), claw=(120, 34, 22), tip=(30, 20, 16))
# skinned: the plates off, pale cream-grey flesh with the segment lines showing
S = dict(C, plate=(206, 194, 160), rim=(150, 132, 104), shine=(226, 218, 190), side=(196, 180, 146), belly=(214, 204, 172),
         head=(196, 150, 120))

# part -> (u, v, w, h, d), as GiantCentipedeModel's texOffs/addBox
UV = {"head": (0, 0, 4, 2, 3), "seg": (0, 6, 4, 2, 2), "tail": (0, 11, 3, 1, 2), "leg": (16, 0, 3, 1, 1),
      "claw": (16, 3, 1, 1, 2), "antenna": (28, 0, 1, 1, 5), "cerci": (40, 0, 1, 1, 3)}


def faces(u, v, w, hh, d):
    """Vanilla box UV: face -> (x0, y0, width, height)."""
    return {"top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d), "right": (u, v + d, d, hh), "front": (u + d, v + d, w, hh),
            "left": (u + d + w, v + d, d, hh), "back": (u + 2 * d + w, v + d, w, hh)}


def colour(P, part, face, i, j, fw, fh):
    if part in ("seg", "tail", "head"):
        base = P["head"] if part == "head" else P["plate"]
        if face == "bottom":
            return P["belly"]
        if face == "top":
            # the plate: a shine down the middle, a dark rim at the back edge (plates overlap front to back)
            c = lerp(base, P["shine"], 0.5) if part != "head" and i in (1, 2) and j == 0 else base
            if part != "head" and j == fh - 1:
                c = P["rim"]
            return c
        if face in ("left", "right"):
            return P["rim"] if part != "head" and i == fh - 1 else lerp(base, P["side"], 0.6)
        return shade(base, 0.85)
    if part == "leg":
        return P["leg_dark"] if face == "bottom" or (face in ("left", "right") and fw == 1) else P["leg"]
    if part == "claw":
        return P["tip"] if face in ("front",) or (face in ("top", "bottom", "left", "right") and j == 0 and fh > 1) else P["claw"]
    return P["leg"] if (i + j) % 3 else P["leg_dark"]  # antennae, cerci: ringed


def entity(P, salt):
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()
    for part, box in UV.items():
        for face, (x0, y0, fw, fh) in faces(*box).items():
            for j in range(fh):
                for i in range(fw):
                    c = colour(P, part, face, i, j, fw, fh)
                    n = 0.92 + 0.16 * h(x0 + i, y0 + j, salt)
                    px[x0 + i, y0 + j] = (*shade(c, n), 255)
    return im


def chitin_vest():
    """Humanoid chest layer: overlapping red-brown chitin plates in rows on the body box (u 16, v 16, 8x12x4) and the
    shoulders of the arm box (u 40, v 16, 4x12x4), laced with sinew at the sides."""
    im = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    px = im.load()

    def plate(x, y, row_y):
        rim = (y - row_y) % 3 == 2
        c = C["rim"] if rim else lerp(C["plate"], C["shine"], 0.35) if (y - row_y) % 3 == 0 else C["plate"]
        return (*shade(c, 0.9 + 0.2 * h(x, y, 77)), 255)

    for face, (x0, y0, fw, fh) in faces(16, 16, 8, 12, 4).items():
        if face in ("top", "bottom"):
            continue
        for j in range(fh - 1):
            for i in range(fw):
                px[x0 + i, y0 + j] = plate(x0 + i, y0 + j, y0)
        if face in ("left", "right"):
            for j in range(1, fh - 1, 2):
                px[x0 + fw // 2, y0 + j] = (222, 206, 164, 255)  # sinew lacing
    for face, (x0, y0, fw, fh) in faces(40, 16, 4, 12, 4).items():
        for j in range(fh if face in ("top",) else 4):
            for i in range(fw):
                px[x0 + i, y0 + j] = plate(x0 + i, y0 + j, y0)
    out = RES / "entity/equipment/humanoid/chitin_vest.png"
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)


CHITIN = [  # a curved plate of chitin, glossy, the dark rim along its edge
    "................",
    "................",
    "................",
    ".....OOOOOO.....",
    "...OOHHSSSSOO...",
    "..OHHSSPPPPPRO..",
    "..OHSPPPPPPPRO..",
    ".OHSPPPPPPPPRRO.",
    ".OSPPPPPPPPPRRO.",
    ".OPPPPPPPPPRRRO.",
    ".ORRPPPPPPRRRO..",
    "..ORRRRRRRRRO...",
    "...OOOOOOOOO....",
    "................",
    "................",
    "................",
]
CHITIN_PAL = {"O": "#1e100c", "H": "#c8794a", "S": "#9c4a2c", "P": "#6a281a", "R": "#3a1610"}

FANG = [  # a hooked venom claw, black at the tip
    "................",
    "................",
    "..........OO....",
    ".........OTTO...",
    "........OTTO....",
    ".......ODDO.....",
    "......ORRDO.....",
    ".....ORRRO......",
    "....ORLRRO......",
    "...ORLLRO.......",
    "...OLLRRO.......",
    "...ORRRO........",
    "....OOO.........",
    "................",
    "................",
    "................",
]
FANG_PAL = {"O": "#1a0e0a", "T": "#16100c", "D": "#4a1c12", "R": "#8a2c1c", "L": "#c25a34"}

VENOM_SAC = [  # a translucent sac of yellow-green venom, the duct trailing off
    "................",
    "................",
    "......OOOO......",
    "....OOWLLLOO....",
    "...OWLLLMMMMO...",
    "...OLLLMMMMMO...",
    "..OLLMMMMMMDDO..",
    "..OLMMMMMMDDDO..",
    "..OMMMMMMDDDDO..",
    "...OMMMMDDDDO...",
    "....OMMDDDDO....",
    ".....OODDOO.....",
    ".......ORO......",
    "........ORO.....",
    ".........OO.....",
    "................",
]
VENOM_PAL = {"O": "#2c3018", "W": "#f6fadc", "L": "#d8e698", "M": "#a8c050", "D": "#70862c", "R": "#8a3c24"}

MEAT = [  # a length of centipede: plated segments, legs along the sides
    "................",
    "................",
    "............OO..",
    "..........OOAO..",
    "........OOAAO.L.",
    ".......OAABBO...",
    "....L.OBBAAO.L..",
    ".....OAABBO.....",
    "...LOBBAAO.L....",
    "....OAABBO......",
    "..LOBBAAO.L.....",
    "...OAABO........",
    "..OBBAO..L......",
    "..OOOO..........",
    "................",
    "................",
]

VEST = [  # the vest: rows of overlapping plates, laced up the front
    "................",
    "...OOO....OOO...",
    "..OPPPO..OPPPO..",
    "..OPRRPOOPRRPO..",
    "..OOPPPPPPPPOO..",
    "...OPPPWWPPPO...",
    "...ORRRWWRRRO...",
    "...OPPPWWPPPO...",
    "...OPPPWWPPPO...",
    "...ORRRWWRRRO...",
    "...OPPPWWPPPO...",
    "...OPPPWWPPPO...",
    "...ORRRWWRRRO...",
    "...OOOOOOOOOO...",
    "................",
    "................",
]
VEST_PAL = {"O": "#1e100c", "P": "#7a3020", "R": "#3a1610", "W": "#ded0a4"}


def items():
    out = {
        "centipede_chitin": wit.from_grid(CHITIN, CHITIN_PAL),
        "centipede_fang": wit.from_grid(FANG, FANG_PAL),
        "centipede_venom_sac": wit.from_grid(VENOM_SAC, VENOM_PAL),
        "raw_centipede": wit.from_grid(MEAT, {"O": "#3a1a12", "A": "#c9a088", "B": "#a66a52", "L": "#d89a48"}),
        "cooked_centipede": wit.from_grid(MEAT, {"O": "#2a1408", "A": "#d0884a", "B": "#9a5428", "L": "#8a5a2a"}),
        "chitin_vest": wit.from_grid(VEST, VEST_PAL),
        # egg: the dark red-brown of the plates, dark rims between the segments, amber legs out at the sides
        "giant_centipede_spawn_egg": wit.egg(["#b8603a", "#8c3c24", "#6a281a", "#4a1a10", "#3a140c", "#240c08"],
                                             [(x, y, "#2a0e08") for y in (5, 8, 11) for x in range(4, 12)],
                                             [(x, y) for y in (4, 7, 10, 13) for x in (1, 14)],
                                             [(x, y, "#e0a040") for y in (4, 7, 10, 13) for x in (1, 14)]),
    }
    for name, im in out.items():
        im.save(wit.OUT / f"{name}.png")


def main():
    for rel, P, salt in (("entity/wildlife/giant_centipede.png", C, 11), ("entity/skinned/wildlife/giant_centipede.png", S, 12)):
        out = RES / rel
        out.parent.mkdir(parents=True, exist_ok=True)
        entity(P, salt).save(out)
    chitin_vest()
    items()
    print("underground textures ok")


if __name__ == "__main__":
    main()
