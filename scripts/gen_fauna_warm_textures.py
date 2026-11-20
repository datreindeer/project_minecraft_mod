"""All textures for the warm-country fauna (wildlife/warm/WarmFauna.java): runs the painters below in turn.

- gen_fauna_warm_tex_mammals.py: onager (+ foal, tamed one's pack baskets), water buffalo (+ calf), tapir (+ striped calf)
- gen_fauna_warm_tex_dry.py: tortoise, desert lizard, leopard
- gen_fauna_warm_tex_birds.py: hornbill, jungle fowl (rooster, hen, chick), peafowl (peacock, peahen)
- gen_fauna_warm_tex_items.py: the 16x16 items, spawn eggs, the Leopard Rug and the leopard pelt on the Drying Rack
Entity textures in textures/entity/wildlife/, their skinned/plucked carcass versions in textures/entity/skinned/wildlife/.
Deterministic. Run: python scripts/gen_fauna_warm_textures.py"""
import importlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

PARTS = ["gen_fauna_warm_tex_mammals", "gen_fauna_warm_tex_dry", "gen_fauna_warm_tex_birds", "gen_fauna_warm_tex_items"]

if __name__ == "__main__":
    for name in PARTS:
        importlib.import_module(name).main()
    print("fauna warm textures ok")
