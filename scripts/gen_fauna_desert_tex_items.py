"""16x16 item textures and the Puma Rug block for the desert fauna (part of gen_fauna_desert_textures.py).

- meats: the house scraps recoloured (gen_fauna_mountain_textures.meat): fennec on the rabbit shape, camel on the beef,
  puma on the rabbit (dark), bighorn on the mutton; the scorpion's tail meat drawn as a curled tail segment.
- hides on the house raw_hide shape (gen_fauna_warm_tex_items.hide_item): camel sandy, bighorn brown with a white rump;
  the fennec and puma pelts and the Puma Rug recoloured from the wolf pelt / rug (gen_fauna_mountain_textures.pelt).
- parts: Scorpion Venom (an amber sac with the sting), Camel Hump Fat (tallow recoloured), Viper Fang, Bighorn Horn (a
  full curl), Rattlesnake Rattle, Condor Feather (the eagle feather recoloured black with a white base).
- spawn eggs (gen_wildlife_item_textures.egg).
Deterministic. Run: python scripts/gen_fauna_desert_tex_items.py"""
import importlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from wildlife_paint import fur, h, lerp, shade  # noqa: E402

TEX = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
ITEM, BLOCK = TEX / "item", TEX / "block"

PUMA_STOPS = [(40, 28, 20), (110, 76, 46), (160, 116, 72), (192, 150, 102), (226, 206, 172)]
FENNEC_STOPS = [(70, 52, 36), (170, 136, 94), (214, 182, 134), (234, 212, 172), (248, 242, 228)]


def items():
    mt = importlib.import_module("gen_fauna_mountain_textures")
    wi = importlib.import_module("gen_fauna_warm_tex_items")
    wit = importlib.import_module("gen_wildlife_item_textures")
    grid = wi.grid

    def camel_fur(x, y):
        d = abs(x - 8)
        c = lerp((150, 110, 70), (200, 160, 108), min(1.0, d / 5))  # darker down the spine and hump
        return fur(c, x, y, 901, 0.07)

    def bighorn_fur(x, y):
        c = (138, 104, 70) if y < 10 else (232, 224, 206)          # the white rump at the tail end
        return fur(lerp(c, (110, 82, 56), 0.4) if abs(x - 8) <= 1 and y < 10 else c, x, y, 902, 0.07)

    scorpion_tail = [
        "................",
        "................",
        ".......oo.......",
        "......okko......",
        ".....oklmko.....",
        "....oklmmdo.....",
        "....olmmddo.....",
        "....oommddoo....",
        ".....olmmmdo....",
        ".....oolmmddo...",
        "......olmmmdo...",
        "......oolmddo...",
        ".......olmddoss.",
        "........oddoss..",
        ".........ooos...",
        "................",
    ]
    raw_sc = dict(o="#5a3a20", k="#e8c890", l="#f0d8a8", m="#d8b880", d="#a8844c", s="#5c3420")
    cooked_sc = dict(o="#3a2010", k="#d89a50", l="#e8b46a", m="#c8843c", d="#8a5424", s="#3a2010")
    venom = [
        "................",
        "................",
        ".........ss.....",
        "........sSs.....",
        "........ss......",
        "......oooo......",
        ".....oyYYyo.....",
        "....oyYwYYyo....",
        "....oyYYYYyo....",
        "....oayYYyao....",
        "....oaayyaao....",
        ".....oaaaao.....",
        "......oooo......",
        "................",
        "................",
        "................",
    ]
    venom_pal = dict(o="#5a3a14", y="#d8a030", Y="#f0c850", w="#fff0b0", a="#a8701c", s="#4a2414", S="#7a4420")
    fang = [
        "................",
        "................",
        "...........ooo..",
        "..........owwo..",
        ".........owwlo..",
        "........owwlo...",
        ".......owwlo....",
        "......owwlo.....",
        ".....owllo......",
        "....owllo.......",
        "....olldo.......",
        "...oldo.........",
        "...odo..........",
        "...oo...........",
        "................",
        "................",
    ]
    fang_pal = dict(o="#6e6252", w="#f4eee0", l="#ddd2bc", d="#b4a68c")
    horn = [
        "................",
        "....oooooo......",
        "...ohhlllhoo....",
        "..ohlmmmmmlho...",
        ".ohlmoooooomlo..",
        ".olmo......omlo.",
        "ohlo..oooo..olo.",
        "olmo.ohlmmo.omo.",
        "olmo.olo.mo.omo.",
        "olmo..oo.mo.odo.",
        ".olmo...omo.odo.",
        ".odmmoooodo.odo.",
        "..oddddddo.odo..",
        "...ooooooo.oo...",
        "................",
        "................",
    ]
    horn_pal = dict(o="#5e4c34", h="#e8dcbc", l="#d4c49c", m="#b8a47c", d="#8c7854")
    rattle = [
        "................",
        "................",
        "..........oo....",
        ".........oddo...",
        "........olddo...",
        ".......oollo....",
        "......olddo.....",
        ".....olldo......",
        "....oollo.......",
        "....olddo.......",
        "...olldo........",
        "...ollo.........",
        "..obbo..........",
        "..oko...........",
        "...o............",
        "................",
    ]
    rattle_pal = dict(o="#5c4a32", l="#e0cc9c", d="#b49c6c", b="#2a2420", k="#d8d0c0")
    out = {
        "raw_scorpion": grid(scorpion_tail, raw_sc),
        "cooked_scorpion": grid(scorpion_tail, cooked_sc),
        "raw_fennec": mt.meat("rabbit_scraps", (170, 70, 60)),
        "cooked_fennec": mt.meat("cooked_rabbit_scraps", (150, 96, 56)),
        "camel_meat": mt.meat("beef_scraps", (156, 46, 40)),
        "cooked_camel_meat": mt.meat("cooked_beef_scraps", (124, 78, 46)),
        "puma_meat": mt.meat("rabbit_scraps", (120, 34, 32), 0.8),
        "cooked_puma_meat": mt.meat("cooked_rabbit_scraps", (100, 62, 40), 0.8),
        "bighorn_mutton": mt.meat("mutton_scraps", (160, 50, 44)),
        "cooked_bighorn_mutton": mt.meat("cooked_mutton_scraps", (128, 80, 46)),
        "scorpion_venom": grid(venom, venom_pal),
        "raw_fennec_pelt": mt.pelt("raw_wolf_pelt", FENNEC_STOPS, salt=911),
        "fennec_pelt": mt.pelt("wolf_pelt", FENNEC_STOPS, salt=912),
        "raw_camel_hide": wi.hide_item(camel_fur, 913, lip=True),
        "raw_bighorn_hide": wi.hide_item(bighorn_fur, 914, fold=22),
        "camel_hump_fat": mt.recolour(mt.load("item/tallow"), [(120, 92, 60), (196, 164, 116), (226, 204, 160), (240, 226, 196), (252, 246, 230)]),
        "viper_fang": grid(fang, fang_pal),
        "raw_puma_pelt": mt.pelt("raw_wolf_pelt", PUMA_STOPS, salt=915),
        "puma_pelt": mt.pelt("wolf_pelt", PUMA_STOPS, salt=916),
        "puma_rug": mt.pelt("wolf_rug", PUMA_STOPS, salt=917),
        "bighorn_horn": grid(horn, horn_pal),
        "rattlesnake_rattle": grid(rattle, rattle_pal),
        "condor_feather": mt.recolour(mt.load("item/eagle_feather"), [(10, 10, 12), (28, 26, 30), (46, 44, 50), (70, 68, 74), (238, 236, 230)]),
    }
    egg = wit.egg
    out.update({
        "desert_scorpion_spawn_egg": egg(["#f0d8a0", "#d8b878", "#c09c58", "#a07c40", "#7a5a2c", "#4e3418"],
                                         [(6, 5, "#7a4a24"), (9, 7, "#7a4a24"), (7, 10, "#7a4a24"), (10, 11, "#5c3420")]),
        "fennec_fox_spawn_egg": egg(["#f8f0e0", "#ecd8b4", "#dcc094", "#c4a474", "#8a6e4c", "#5c4630"],
                                    [(5, 3, "#e8a8a0"), (10, 3, "#e8a8a0"), (8, 12, "#2e2420")]),
        "wild_camel_spawn_egg": egg(["#e0c08c", "#cca474", "#b88c5c", "#9c7448", "#6e5034", "#4a3422"],
                                    [(7, 2, "#8a6038"), (8, 2, "#8a6038"), (7, 3, "#8a6038"), (8, 3, "#8a6038")]),
        "sand_viper_spawn_egg": egg(["#f0e2c0", "#e2cea4", "#d0b888", "#b89c6c", "#8a7250", "#5c4c34"],
                                    [(6, 5, "#a0784c"), (9, 8, "#a0784c"), (6, 11, "#a0784c")]),
        "puma_spawn_egg": egg(["#e0c094", "#c8a070", "#b08454", "#946a40", "#6a4a2c", "#40281a"],
                              [(7, 11, "#f0e4cc"), (8, 11, "#f0e4cc"), (5, 9, "#3a2a20"), (10, 9, "#3a2a20")]),
        "bighorn_sheep_spawn_egg": egg(["#b4906a", "#9c7856", "#846446", "#6c5038", "#4c3828", "#30241a"],
                                       [(7, 12, "#f0eadc"), (8, 12, "#f0eadc"), (8, 13, "#f0eadc"), (5, 4, "#d8ccac"), (10, 4, "#d8ccac")]),
        "rattlesnake_spawn_egg": egg(["#b8a888", "#a09070", "#8c7c5e", "#74664c", "#4e4434", "#2e2820"],
                                     [(7, 4, "#3a3028"), (8, 5, "#3a3028"), (7, 8, "#3a3028"), (8, 9, "#3a3028"), (7, 12, "#3a3028")]),
        "condor_spawn_egg": egg(["#5a585e", "#403e44", "#2c2a30", "#1c1a20", "#141216", "#0a090c"],
                                [(6, 3, "#f0eee8"), (7, 3, "#f0eee8"), (8, 3, "#f0eee8"), (9, 3, "#f0eee8"), (8, 1, "#b06054")]),
    })
    return out


def blocks():
    mt = importlib.import_module("gen_fauna_mountain_textures")
    return {"puma_rug": mt.pelt_block("wolf_rug", PUMA_STOPS, None)}


def main():
    for folder, made in ((ITEM, items()), (BLOCK, blocks())):
        for name, im in made.items():
            assert im.size == (16, 16) and im.getbbox(), name
            im.save(folder / f"{name}.png")
    print("fauna desert item textures ok")


if __name__ == "__main__":
    main()
