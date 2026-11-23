"""Paint the desert and badlands fauna (scripts/gen_fauna_desert.py, com.mechanical.wildlife.desert): the entity models
and textures with their skinned (butchered) versions come from gen_desert_models.py (one table per animal: the Java
geometry DesertGeometry and the skin painted for it), then the 16x16 items and the Puma Rug block
(gen_fauna_desert_tex_items.py).

Usage: python scripts/gen_fauna_desert_textures.py
Deterministic (hashes only).
"""
import importlib
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))


def main():
    importlib.import_module("gen_desert_models").main()
    importlib.import_module("gen_fauna_desert_tex_items").main()
    print("fauna desert textures ok")


if __name__ == "__main__":
    main()
