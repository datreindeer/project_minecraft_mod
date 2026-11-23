"""Paint the jungle and mangrove fauna (scripts/gen_fauna_jungle.py, com.mechanical.wildlife.jungle): the equipment layers
(Tiger Pelt Mantle, Crocodile Hide armour), the Tiger Rug block and every 16x16 item. The creatures' models and skins
(and their skinned carcass skins) come from gen_jungle_models.py, run from here first.

Usage: python scripts/gen_fauna_jungle_textures.py
Items: gen_fauna_jungle_tex_items.py (run from here). Deterministic: hashes only, no randomness.
"""
import importlib.util
import math
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from critter_paint import lerp, shade, smooth  # noqa: E402

TEX = HERE.parent / "src/main/resources/assets/mechanical/textures"


def load(name, alias):
    spec = importlib.util.spec_from_file_location(alias, HERE / f"{name}.py")
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod




# ======================================================================= equipment layers

def recolour_layer(src, dst, dark, light, pattern=None):
    """Re-tint an existing 64x32 equipment layer by brightness into dark..light; pattern(x, y, c) may overlay marks."""
    im = Image.open(TEX / src).convert("RGBA")
    px = im.load()
    for x in range(im.width):
        for y in range(im.height):
            r, g, b, a = px[x, y]
            if not a:
                continue
            t = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            c = lerp(dark, light, smooth(t * 1.3))
            if pattern:
                c = pattern(x, y, c)
            px[x, y] = (*c, a)
    im.save(TEX / dst)
    print("wrote", dst)


def tiger_stripes(x, y, c):
    return (26, 20, 16) if (x + int(1.5 * math.sin(y * 0.8))) % 5 == 0 else c


def croc_scales(x, y, c):
    return shade(c, 0.75) if x % 2 == 0 or y % 3 == 0 else c


def equipment():
    recolour_layer("entity/equipment/humanoid/otter_fur.png", "entity/equipment/humanoid/tiger_fur.png", (150, 64, 20), (238, 150, 70),
                   tiger_stripes)
    recolour_layer("entity/cloak/otter_fur_cape.png", "entity/cloak/tiger_pelt_mantle.png", (150, 64, 20), (238, 150, 70), tiger_stripes)
    recolour_layer("entity/equipment/humanoid/lindworm_scale.png", "entity/equipment/humanoid/crocodile_hide.png", (40, 46, 30), (128, 132, 88),
                   croc_scales)
    recolour_layer("entity/equipment/humanoid_leggings/lindworm_scale.png", "entity/equipment/humanoid_leggings/crocodile_hide.png",
                   (40, 46, 30), (128, 132, 88), croc_scales)


def main():
    load("gen_jungle_models", "jungle_models").main()
    equipment()
    load("gen_fauna_jungle_tex_items", "jungle_items").main()


if __name__ == "__main__":
    main()
