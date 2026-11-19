"""Textures for the bird overhaul (gen_birds2.py, Birds.java, BirdNestBlock.java).

- item/: Raw and Cooked Goose, Pheasant, Ptarmigan, Pigeon (a cut each: goose leg quarter, pheasant drumstick, pigeon
  breast, a pair of ptarmigan thighs; cooked = the same cut roasted), the five Plucked birds (side views: long-necked goose,
  long-tailed pheasant with its red face, small round pigeon, plump ptarmigan, oven-ready chicken on its back) and the
  four eggs (big cream goose egg, olive pheasant egg, blotched ptarmigan egg, small glossy white pigeon egg).
- block/: bird_nest (woven twigs), bird_nest_lining (straw and down), bird_egg_<bird> (shell colour for the eggs in the
  nest model, one per nest bird; chicken = vanilla egg cream).
- entity/skinned/chicken/chicken_<variant>.png: plucked chicken for the carcass (replaces the generic flesh texture of
  gen_skinned_textures.py): bare skin with follicles, beak, comb, wattle, eyes and legs kept, cold chicken's crest
  plucked and its tail fin left as quill stubs. Plucked flock birds: gen_bird_textures.py; pheasant: gen_small_wildlife_textures.py.
Deterministic (hashes only). Run: python scripts/gen_birds2_textures.py
"""
import glob
import io
import zipfile
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/mechanical/textures"


def h(x, y, salt):
    n = (x * 374761393 + y * 668265263 + salt * 2147483647) & 0xFFFFFFFF
    n = ((n ^ (n >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((n ^ (n >> 16)) % 1000) / 1000.0


def lerp(a, b, t):
    t = max(0.0, min(1.0, t))
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def hexc(s):
    s = s.lstrip("#")
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def grid(rows, pal):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                im.putpixel((x, y), (*hexc(pal[ch]), 255))
    return im


def save(im, rel):
    out = TEX / rel
    out.parent.mkdir(parents=True, exist_ok=True)
    im.save(out)
    print("wrote", out.relative_to(ROOT))


# ---------------------------------------------------------------- meat cuts: o outline, d dark, m mid, l light, h highlight, s skin/fat, b bone, B bone shade
RAW = {  # per bird: goose meat is dark red, pheasant pink, ptarmigan and pigeon deep maroon (game birds)
    "goose": dict(o="#4e1a1e", d="#8a2e34", m="#b24a4a", l="#d07a72", h="#e8a89c", s="#ecd2b0", b="#eee6d2", B="#c8bca4"),
    "pheasant": dict(o="#5a2226", d="#a8484c", m="#cc6e6a", l="#e4988e", h="#f2c0b2", s="#f0d8bc", b="#eee6d2", B="#c8bca4"),
    "ptarmigan": dict(o="#3e1216", d="#6e2026", m="#963238", l="#b85650", h="#d48474", s="#e2c4a8", b="#eee6d2", B="#c8bca4"),
    "pigeon": dict(o="#36100f", d="#64191c", m="#8a2a2c", l="#ac4a46", h="#cc766c", s="#dcbca4", b="#eee6d2", B="#c8bca4"),
}
COOKED = {  # roasted: golden brown skin, darker for the dark meats
    "goose": dict(o="#3c200e", d="#7a4220", m="#a8622e", l="#c88440", h="#e2ac62", s="#d89a4c", b="#e8dcc0", B="#bcae90"),
    "pheasant": dict(o="#44240f", d="#8a4e22", m="#b67234", l="#d4964c", h="#ecbe74", s="#e0a858", b="#e8dcc0", B="#bcae90"),
    "ptarmigan": dict(o="#341a0c", d="#6a361a", m="#94522a", l="#b4723a", h="#d29a58", s="#c88c48", b="#e8dcc0", B="#bcae90"),
    "pigeon": dict(o="#2e160a", d="#5e2e16", m="#844624", l="#a66434", h="#c68a4e", s="#b87c40", b="#e8dcc0", B="#bcae90"),
}
CUTS = {
    "goose": [  # a big leg quarter: the thigh with its skin up top, the drumstick down to the knuckle
        "................",
        "......ooooo.....",
        "....oosssssoo...",
        "...osshhlllmmo..",
        "..oshhlllmmmmdo.",
        "..oshllmmmmmmdo.",
        ".oshllmmmmmmmddo",
        ".osllmmmmmmmmddo",
        ".oslmmmmmmmmddo.",
        "..osmmmmmmmddoo.",
        "...oommmmmddo...",
        ".....odmmddo....",
        "....obodddo.....",
        "...obbboo.......",
        "..obbBo.........",
        "...ooo..........",
    ],
    "pheasant": [  # a drumstick
        "................",
        "................",
        "........ooooo...",
        ".......osllmmo..",
        "......oshhlmmdo.",
        "......oshlmmmdo.",
        "......oslmmmmdo.",
        "......osmmmmddo.",
        ".......ommmddo..",
        "......oommddo...",
        ".....obodoo.....",
        "....obbbo.......",
        "...obbBo........",
        "..obBBo.........",
        "...ooo..........",
        "................",
    ],
    "pigeon": [  # the small dark breast, both halves either side of the keel
        "................",
        "................",
        "................",
        ".....oooooo.....",
        "....osssssso....",
        "...oshlmmlhso...",
        "...ohllmmllho...",
        "..ohllmbbmllmo..",
        "..omllmbBmllmo..",
        "..ommlmdBmlmmo..",
        "..ommmmddmmmmo..",
        "...odmmddmmdo...",
        "...oddmddmddo...",
        "....ooddddoo....",
        "......oooo......",
        "................",
    ],
    "ptarmigan": [  # a pair of small plump thighs
        "................",
        "................",
        "...oooo..oooo...",
        "..osslmooosslmo.",
        ".oshhlmmdoshlmdo",
        ".oshllmmdohllmdo",
        ".oslmmmddolmmmdo",
        ".osmmmmddommmddo",
        "..ommmddo.ommddo",
        "...ommdo..oomdo.",
        "....obo....obo..",
        "...obbo...obbo..",
        "...oBo....oBo...",
        "....o......o....",
        "................",
        "................",
    ],
}

# ---------------------------------------------------------------- plucked birds: o outline, s skin, l light, d shade, f follicle,
# k beak, e eye, r red (comb, wattle, face), g leg, G leg shade, q quill stubs
SKIN = dict(o="#7e5246", s="#ecc4a6", l="#f6dcc4", d="#c89478", f="#d4a488", q="#e8dcc0")
PLUCKED = {
    "goose": ([  # long neck, big body, orange bill and pink legs
        "................",
        ".ooo............",
        "okeso...........",
        ".ooso...........",
        "...osd..........",
        "...osd..........",
        "...oslo.........",
        "...osloooooooo..",
        "..oslllssssssdo.",
        "..oslsfssssfsddo",
        "..ossssssfssssdo",
        "...osfssssssddo.",
        "....oddssssddqo.",
        ".....ooogooooq..",
        "......ogo.ogo...",
        ".....oGG..GGo...",
    ], dict(SKIN, k="#e8863a", e="#1c1410", g="#e8a096", G="#c47a72")),
    "pheasant": ([  # red face, longer neck, long body and the stubs of its long tail
        "................",
        "................",
        "..ooo...........",
        ".kerro..........",
        "..orro..........",
        "...oso..........",
        "...osloooooo....",
        "..oslllssssso...",
        "..oslsfsssfsdooo",
        "..osssssfssssdqq",
        "...osfssssssddoq",
        "....oddsssddoo..",
        ".....oooogoo....",
        "........ogo.....",
        ".......oGG......",
        "................",
    ], dict(SKIN, k="#d8cca0", e="#1c1410", r="#c4302c", g="#8a7a6a", G="#665a4e")),
    "pigeon": ([  # small and round, dark pink, little dark bill and red feet
        "................",
        "................",
        "................",
        "................",
        "....ooo.........",
        "...kesoo........",
        "....oslooooo....",
        "...osllssssso...",
        "...oslsfssssdo..",
        "...osssssfssdqo.",
        "....osfsssdddo..",
        ".....oddddoo....",
        "......ogoog.....",
        ".....oGGoGG.....",
        "................",
        "................",
    ], dict(SKIN, s="#dca094", l="#ecbcb0", d="#b47268", f="#c08478", o="#6a3632", k="#3a2e2e", e="#e07a2a",
            g="#d04a4a", G="#a03434")),
    "ptarmigan": ([  # plump and short-necked, red comb over the eye, stubby black bill
        "................",
        "................",
        "................",
        "....ooo.........",
        "...orroo........",
        "..keslso........",
        "...osllooooo....",
        "..osllsssssso...",
        "..oslsfsssfsdo..",
        "..osssssfssssdo.",
        "..osfssssfssddo.",
        "...ossssssddqo..",
        "....oddddddoq...",
        ".....ooggooo....",
        "......oGGo......",
        "................",
    ], dict(SKIN, s="#e2b09c", l="#f0cab8", d="#bc8070", f="#c8907e", k="#1e1a1a", e="#1c1410", r="#cc2a26",
            g="#d8a090", G="#b07c6c")),
    "chicken": ([  # oven-ready on its back: plump breast up, drumsticks raised, wing tips tucked
        "................",
        "................",
        "........oo..oo..",
        ".......obo.obo..",
        ".......oso.oso..",
        "......osso.osso.",
        "....ooosloosloo.",
        "...osllssllssdo.",
        "..oslllsfssssddo",
        "..oslsssssfsssdo",
        "..ossfssssssfsdo",
        "..odssssssssssdo",
        "...odssfsssssddo",
        "....oddddddddoo.",
        ".....oooooooo...",
        "................",
    ], dict(SKIN, s="#f0d0a8", l="#fae6c8", d="#cca07a", f="#dcb08a", b="#eee6d2")),
}

# ---------------------------------------------------------------- eggs: o outline, w shell, l light, h shine, d shade, x/y markings
EGGS = {
    "goose": ([  # big, cream-white, matte
        "................",
        "......oooo......",
        ".....owwwwo.....",
        "....owlhwwwo....",
        "...owlhhwwwdo...",
        "...owlhwwwwdo...",
        "..owlwwwwwwwdo..",
        "..owwwwwwwwwdo..",
        "..owwwwwwwwwdo..",
        "..owwwwwwwwddo..",
        "..owwwwwwwwddo..",
        "...owwwwwwddo...",
        "...odwwwwdddo...",
        "....oddddddo....",
        ".....oooooo.....",
        "................",
    ], dict(o="#8a8270", w="#efe8d6", l="#f8f4e8", h="#ffffff", d="#cfc6ae")),
    "pheasant": ([  # olive-brown, a little pointed
        "................",
        "................",
        "................",
        ".......oo.......",
        "......owwo......",
        ".....owlhwo.....",
        "....owlhwwwo....",
        "....owlwwwwo....",
        "...owwwwwwwdo...",
        "...owwwwwwwdo...",
        "...owwwwwwddo...",
        "...odwwwwwddo...",
        "....odwwwddo....",
        ".....odddddo....",
        "......ooooo.....",
        "................",
    ], dict(o="#4a4024", w="#9a8a5a", l="#b4a674", h="#ccc08e", d="#7a6c42")),
    "ptarmigan": ([  # buff, blotched dark red-brown
        "................",
        "................",
        "................",
        ".......oo.......",
        "......owwo......",
        ".....owlxwo.....",
        "....owhwwyxo....",
        "....owlwxwwo....",
        "...owxwwwwyxo...",
        "...owwwyxwwdo...",
        "...oxwwwwwxdo...",
        "...odwyxwwddo...",
        "....odwwwxdo....",
        ".....odddddo....",
        "......ooooo.....",
        "................",
    ], dict(o="#5a3a22", w="#d8b880", l="#e8cc96", h="#f4e0b4", d="#b4945e", x="#6a2a1a", y="#8e4626")),
    "pigeon": ([  # small, pure glossy white
        "................",
        "................",
        "................",
        "................",
        "................",
        ".......oo.......",
        "......owwo......",
        ".....owhlwo.....",
        ".....owhwwo.....",
        ".....owwwwo.....",
        ".....owwwdo.....",
        ".....odwwdo.....",
        "......oddo......",
        ".......oo.......",
        "................",
        "................",
    ], dict(o="#8c8c94", w="#f2f2f4", l="#fafafc", h="#ffffff", d="#d0d0d8")),
}

# shell colour (base, speckle) of the eggs in the nest model
SHELLS = {"goose": ("#efe8d6", "#d8d0bc"), "pheasant": ("#9a8a5a", "#7a6c42"), "ptarmigan": ("#d8b880", "#6a2a1a"),
          "rock_dove": ("#f2f2f4", "#dcdce2"), "chicken": ("#e8dcb4", "#d2c49a")}


def shell(base, spot, ptarmigan=False):
    im = Image.new("RGBA", (16, 16))
    b, s = hexc(base), hexc(spot)
    for y in range(16):
        for x in range(16):
            r = h(x, y, 71)
            c = lerp(b, (255, 255, 255), 0.25) if y < 4 else b  # lit top
            if ptarmigan and (r < 0.18 or h(x // 2, y // 2, 72) < 0.12):
                c = s if r < 0.1 else lerp(b, s, 0.6)  # big dark blotches
            elif r < 0.12:
                c = lerp(c, s, 0.7)
            im.putpixel((x, y), (*c, 255))
    return im


def nest_twigs():
    """Woven twigs: overlapping diagonal strands in a few browns over a dark gap colour."""
    GAP, TW = hexc("#3a2614"), [hexc(c) for c in ("#6e4a26", "#86603a", "#9c7448", "#5e3e1e", "#a8845a")]
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = GAP
            for k, (dx, off) in enumerate(((1, 0), (-1, 5), (1, 9), (-1, 13), (2, 3))):
                if ((x * dx + y * (1 if dx > 0 else -1) * (2 if abs(dx) == 2 else 1) + off) % 7) in (0, 1):
                    c = TW[k]
            if h(x, y, 81) < 0.12:
                c = lerp(c, (0, 0, 0), 0.25)
            elif h(x, y, 82) < 0.08:
                c = lerp(c, (200, 170, 120), 0.3)  # a bark highlight
            im.putpixel((x, y), (*c, 255))
    return im


def nest_lining():
    """Straw strands with white down tufts in the hollow."""
    BASE, STRAW = hexc("#8a6e36"), [hexc(c) for c in ("#d8b860", "#c4a44c", "#e8cc78", "#b08e3e")]
    DOWN = hexc("#f2eee6")
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = BASE
            if (x + 2 * y + int(h(0, y, 83) * 5)) % 4 < 2 or (2 * x - y) % 5 == 0:
                c = STRAW[int(h(x, y, 84) * 4)]
            for cx, cy in ((4, 4), (11, 6), (6, 11), (12, 12)):
                if (x - cx) ** 2 + (y - cy) ** 2 <= 2 and h(x, y, 85) < 0.8:
                    c = lerp(DOWN, (220, 214, 204), h(x, y, 86) * 0.5)
            im.putpixel((x, y), (*c, 255))
    return im


# ---------------------------------------------------------------- plucked chicken (vanilla adult chicken textures, 64x32)
JAR = sorted(glob.glob(str(ROOT / "build/moddev/artifacts/minecraft-patched-*-merged.jar"))
             + glob.glob(str(Path.home() / "Documents/project_minecraft_mod/build/moddev/artifacts/minecraft-patched-*-merged.jar")))
# UV regions of AdultChickenModel / ColdChickenModel (box u, v, w, h, d -> 2(w+d) x (d+h))
KEEP = [(14, 0, 12, 4), (14, 4, 8, 4), (26, 0, 12, 8)]  # beak, wattle, legs
QUILLS = [(38, 9, 10, 8)]  # cold chicken's tail fin
SKIN_PINK, SKIN_YELLOW, FOLLICLE = (240, 196, 170), (238, 210, 160), (206, 150, 126)


def inside(x, y, regions):
    return any(u <= x < u + w and v <= y < v + hh for u, v, w, hh in regions)


def pluck_chicken(src):
    out = src.copy()
    px = out.load()
    for y in range(32):
        for x in range(64):
            r, g, b, a = px[x, y]
            if not a or inside(x, y, KEEP):
                continue
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255
            if r > 180 and g < 80 and b < 80:
                continue  # comb and wattle stay red
            if lum < 0.16 and x < 14 and y < 9:
                continue  # eyes
            if inside(x, y, QUILLS):
                c = (234, 224, 198) if x % 2 else (206, 190, 158)
            else:
                c = lerp(SKIN_PINK, SKIN_YELLOW, h(x // 2, y, 30))
                c = lerp(c, (182, 132, 108), max(0.0, 0.55 - lum) * 0.5)  # a hint of the old shading in the folds
                if h(x, y, 31) < 0.16:
                    c = lerp(c, FOLLICLE, 0.6)
            px[x, y] = (*c, a)
    return out


def main():
    for bird in CUTS:
        save(grid(CUTS[bird], RAW[bird]), f"item/raw_{bird}.png")
        save(grid(CUTS[bird], COOKED[bird]), f"item/cooked_{bird}.png")
    for bird, (rows, pal) in PLUCKED.items():
        save(grid(rows, pal), f"item/plucked_{bird}.png")
    for bird, (rows, pal) in EGGS.items():
        save(grid(rows, pal), f"item/{bird}_egg.png")
    for bird, (base, spot) in SHELLS.items():
        save(shell(base, spot, bird == "ptarmigan"), f"block/bird_egg_{bird}.png")
    save(nest_twigs(), "block/bird_nest.png")
    save(nest_lining(), "block/bird_nest_lining.png")
    assert JAR, "no merged minecraft jar (run a gradle build first)"
    with zipfile.ZipFile(JAR[0]) as jar:
        for variant in ("temperate", "warm", "cold"):
            src = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/entity/chicken/chicken_{variant}.png"))).convert("RGBA")
            out = pluck_chicken(src)
            assert out.getchannel("A").tobytes() == src.getchannel("A").tobytes()
            save(out, f"entity/skinned/chicken/chicken_{variant}.png")


if __name__ == "__main__":
    main()
