"""Terrain shape (docs/terrain-plan.md section 1): overrides vanilla's overworld density functions.

Reads vanilla's JSON from the Minecraft jar and writes our versions to
data/minecraft/worldgen/density_function/overworld/:

- mechanical:flatness (data/mechanical/worldgen/density_function/flatness.json): 0..1, how flat the ground should be.
  1 in the biomes that should be flat (dry: humidity below -0.15, i.e. plains, savanna, steppe, chalk downs, pine heath;
  hot: temperature above 0.6, deserts and gypsum flats; frozen and not humid: snowy plains, ice spikes), FLAT_BASE on moderately eroded land anywhere (erosion above
  0.1, e.g. Peat Bog); faded out towards the mountains (erosion -0.3 .. -0.45), so peaks and their slopes keep their shape.
- offset.json: the base height. The leaf splines over peaks-and-valleys (PV, ridges_folded: -1 river valley .. 1 ridge)
  for mid inland (continentalness 0.25) and far inland (1.0) are replaced from the OFFSET table below. Coasts
  (continentalness up to -0.1) and swamps (erosion 0.7) keep vanilla's values, so coastlines and swamps stay put.
  Height of a value v: y ~ 63.5 + 128 * v (before noise and jaggedness). Where the ground should be flat, the offset
  blends (by flatness) into a calm copy of that spline, whose lowland hills keep only FLAT_KEEP of their height above
  the valley rim and whose river banks are gentler: long gentle slopes instead of hills.
- factor.json: how strongly the 3D noise roughens the ground (higher = calmer): vanilla's factor times
  1 + FLAT_BOOST * flatness. Windswept spots (vanilla factor below 2) keep their shattered look.
- jaggedness.json: vanilla's times JAG_SCALE (slightly spikier tops on the highest ridges).
- final_density.json and preliminary_surface_level.json: vanilla's top slide (terrain squeezed out between y 240 and 256)
  moves up to TOP_SLIDE so peaks can reach ~280.

Biome placement is untouched (the climate noises are the same); only heights change. Run after nothing in particular
(no other generator writes these files); place it after gen_world_biomes in the chain.
"""
import json
import zipfile
from pathlib import Path

BASE = Path(__file__).resolve().parent.parent
JAR = BASE / "build/moddev/artifacts/minecraft-patched-26.3.0.95-merged.jar"
OUT = BASE / "src/main/resources/data/minecraft/worldgen/density_function/overworld"
DF = "data/minecraft/worldgen/density_function/overworld/"
OWN = BASE / "src/main/resources/data/mechanical/worldgen/density_function/flatness.json"

# (continentalness, erosion) -> new PV spline points (pv, value, derivative). Vanilla in the comment (value at
# pv -1 / 0 / 1). E -0.85, -0.7, -0.4: mountains (vanilla erosion bands E0, E1); -0.35: plateaus (E2); -0.1: hilly
# lowland (E3); 0.2 .. 0.58: flat lowland (E4, E5). The river sits at pv -1 (value below 0 = under sea level, water);
# its banks rise steeply to pv -0.82 and level out by -0.5, so lowland rivers run in valleys 20-30 deep and mountain
# rivers far deeper.
OFFSET = {
    # far inland
    (1.0, -0.85): [(-1, 0.32, 0), (0, 0.88, 0.5), (1, 1.34, 0.5)],  # 0.35 / 0.92 / 1.5
    (1.0, -0.7): [(-1, 0.28, 0), (0, 0.88, 0.55), (1, 1.38, 0.5)],  # 0.2 / 0.54 / 1.0
    (1.0, -0.4): [(-1, 0.24, 0), (0, 0.78, 0.55), (1, 1.3, 0.5)],  # 0.2 / 0.54 / 1.0
    (1.0, -0.35): [(-1, -0.2, 0.5), (-0.4, 0.62, 0), (0, 0.65, 0.05), (0.4, 0.7, 0.2), (1, 1.05, 0.45)],  # -0.2 / 0.5 / 0.6
    (1.0, -0.1): [(-1, -0.06, 0), (-0.82, 0.14, 0.5), (-0.5, 0.17, 0.08), (0, 0.2, 0.1), (0.4, 0.66, 0.9), (1, 0.95, 0.3)],  # -0.05 / 0.03 / 0.6
    (1.0, 0.2): [(-1, -0.06, 0), (-0.82, 0.09, 0.4), (-0.5, 0.11, 0.06), (0, 0.13, 0.05), (0.4, 0.16, 0.08), (1, 0.24, 0.1)],  # -0.05 / 0.01 / 0.1
    (1.0, 0.4): [(-1, -0.06, 0), (-0.82, 0.09, 0.4), (-0.5, 0.11, 0.06), (0, 0.13, 0.05), (0.4, 0.16, 0.08), (1, 0.24, 0.1)],
    (1.0, 0.58): [(-1, -0.06, 0), (-0.82, 0.075, 0.35), (-0.5, 0.09, 0.05), (0, 0.11, 0.04), (0.4, 0.13, 0.06), (1, 0.2, 0.1)],
    # mid inland
    (0.25, -0.85): [(-1, 0.27, 0), (0, 0.82, 0.5), (1, 1.28, 0.5)],  # 0.2 / 0.72 / 1.23
    (0.25, -0.7): [(-1, 0.25, 0), (0, 0.8, 0.52), (1, 1.3, 0.5)],  # 0.2 / 0.45 / 0.88
    (0.25, -0.4): [(-1, 0.22, 0), (0, 0.68, 0.5), (1, 1.2, 0.5)],  # 0.2 / 0.31 / 0.7
    (0.25, -0.35): [(-1, -0.25, 0.5), (-0.4, 0.5, 0), (0, 0.52, 0.05), (0.4, 0.57, 0.2), (1, 0.9, 0.4)],  # -0.25 / 0.35 / 0.42
    (0.25, -0.1): [(-1, -0.06, 0), (-0.82, 0.1, 0.4), (-0.5, 0.12, 0.06), (0, 0.15, 0.08), (0.4, 0.54, 0.75), (1, 0.8, 0.25)],  # -0.1 / 0.02 / 0.42
    (0.25, 0.2): [(-1, -0.06, 0), (-0.82, 0.065, 0.35), (-0.5, 0.08, 0.05), (0, 0.095, 0.03), (0.4, 0.115, 0.05), (1, 0.18, 0.08)],  # -0.1 / 0.01 / 0.1
    (0.25, 0.4): [(-1, -0.06, 0), (-0.82, 0.065, 0.35), (-0.5, 0.08, 0.05), (0, 0.095, 0.03), (0.4, 0.115, 0.05), (1, 0.18, 0.08)],
    (0.25, 0.58): [(-1, -0.06, 0), (-0.82, 0.055, 0.3), (-0.5, 0.07, 0.04), (0, 0.08, 0.03), (0.4, 0.1, 0.05), (1, 0.16, 0.08)],
}
# erosion 0.45 and 0.55 (windswept spots nested inside): every value above the river floor is lifted by this much
NESTED_LIFT = {1.0: 0.1, 0.25: 0.07}

FLAT_BOOST = 4.0  # factor multiplier - 1 in the flat biomes
FLAT_BASE = 0.4  # ... and on moderately eroded land anywhere
FLAT_KEEP = 0.12  # share of a lowland hill's height (above the valley rim at pv -0.5) left where the ground is flat
CALM = (-0.1, 0.2, 0.4, 0.58)  # erosion points (hilly and flat lowland) whose calm copy loses its hills
FLATNESS = "mechanical:flatness"
TOP_SLIDE = (272, 296)  # vanilla (240, 256)
JAG_SCALE = 1.1  # jaggedness (the spiky tops of the highest ridges) times this


def pts(points):
    return [{"derivative": d, "location": p, "value": v} for p, v, d in points]


def spline(coord, points):
    return {"coordinate": coord, "points": points}


def lift(node, by):
    """Adds `by` to every leaf value of a PV spline above the river floor (pv -1)."""
    for p in node["points"]:
        if isinstance(p["value"], dict):
            lift(p["value"], by)
        elif p["location"] > -1:
            p["value"] = round(p["value"] + by, 4)


def offset(doc):
    root = doc["input"]["second"]["right"]["spline"]
    assert root["coordinate"] == "minecraft:overworld/continents"
    done = set()
    for cp in root["points"]:
        c = cp["location"]
        if c not in NESTED_LIFT:
            continue
        for ep in cp["value"]["points"]:
            e = ep["location"]
            if (c, e) in OFFSET:
                assert ep["value"]["coordinate"] == "minecraft:overworld/ridges_folded"
                ep["value"]["points"] = pts(OFFSET[(c, e)])
                done.add((c, e))
            elif e in (0.45, 0.55):
                lift(ep["value"], NESTED_LIFT[c])
                done.add((c, e))
    missing = set(OFFSET) - done
    assert not missing, f"no vanilla spline at {missing}"
    calm = json.loads(json.dumps(root))
    for cp in calm["points"]:
        if cp["location"] in NESTED_LIFT:
            for ep in cp["value"]["points"]:
                if ep["location"] in CALM:
                    flatten(ep["value"])
    doc["input"]["second"]["right"] = {"type": "minecraft:lerp", "alpha": FLATNESS, "first": {"type": "minecraft:spline", "spline": root},
                                       "second": {"type": "minecraft:spline", "spline": calm}}
    return doc


def flatten(leaf):
    """Calm copy of a lowland PV spline: values beyond the valley rim (pv -0.5) keep FLAT_KEEP of their rise above it,
    and the river bank (pv -0.82) sits halfway between the river floor and the rim, so the valley opens out more gently."""
    rim = next(p["value"] for p in leaf["points"] if p["location"] == -0.5)
    floor = next(p["value"] for p in leaf["points"] if p["location"] == -1)
    for p in leaf["points"]:
        if p["location"] > -0.5:
            p["value"] = round(rim + FLAT_KEEP * (p["value"] - rim), 4)
            p["derivative"] = round(FLAT_KEEP * p["derivative"], 4)
        elif p["location"] == -0.82:
            p["value"] = round((floor + rim) / 2, 4)
            p["derivative"] = round((rim - floor) / 0.5, 4)


def flatness():
    hum = {"type": "minecraft:spline", "spline": spline("minecraft:overworld/vegetation", pts([(-0.15, 1.0, 0), (-0.05, 0.0, 0)]))}
    hot = {"type": "minecraft:spline", "spline": spline("minecraft:overworld/temperature", pts([(0.5, 0.0, 0), (0.6, 1.0, 0)]))}
    # the frozen band's dry half (Snowy Plains, Mammoth Steppe, Ice Spikes; Snowy Taiga is wetter)
    cold = {"type": "minecraft:mul",
            "left": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/temperature", pts([(-0.5, 1.0, 0), (-0.42, 0.0, 0)]))},
            "right": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/vegetation", pts([(0.0, 1.0, 0), (0.08, 0.0, 0)]))}}
    eroded = {"type": "minecraft:spline", "spline": spline("minecraft:overworld/erosion", pts([(-0.05, 0.0, 0), (0.1, FLAT_BASE, 0)]))}
    gate = {"type": "minecraft:spline", "spline": spline("minecraft:overworld/erosion", pts([(-0.45, 0.0, 0), (-0.3, 1.0, 0)]))}
    mx = lambda a, b: {"type": "minecraft:max", "left": a, "right": b}
    return {"type": "minecraft:cache", "input": {"type": "minecraft:mul", "left": mx(mx(hum, hot), mx(cold, eroded)), "right": gate}}


def factor(doc):
    blend = doc["input"]
    assert blend["type"] == "minecraft:lerp"
    vanilla = blend["second"]
    boosted = {"type": "minecraft:mul", "left": vanilla, "right": {"type": "minecraft:add", "left": 1.0,
                                                                   "right": {"type": "minecraft:mul", "left": FLAT_BOOST, "right": FLATNESS}}}
    # windswept hills and savanna (vanilla factor 0.625 there) keep their shattered look
    blend["second"] = {"type": "minecraft:range_choice", "input": vanilla, "min_inclusive": 2.0, "max_exclusive": 1000.0,
                       "when_in_range": boosted, "when_out_of_range": vanilla}
    return doc


def jaggedness(doc):
    def scale(node):
        for p in node["points"]:
            if isinstance(p["value"], dict):
                scale(p["value"])
            else:
                p["value"] = round(p["value"] * JAG_SCALE, 4)
    scale(doc["input"]["second"]["spline"])
    return doc


def top_slide(node):
    """Moves the gradient that squeezes terrain out at the top (240 -> 256) to TOP_SLIDE; returns how many were moved."""
    n = 0
    if isinstance(node, dict):
        if node.get("type") == "minecraft:gradient" and node.get("from_coordinate") == 240 and node.get("to_coordinate") == 256:
            node["from_coordinate"], node["to_coordinate"] = TOP_SLIDE
            n += 1
        for v in node.values():
            n += top_slide(v)
    elif isinstance(node, list):
        for v in node:
            n += top_slide(v)
    return n


def main():
    with zipfile.ZipFile(JAR) as jar:
        read = lambda name: json.loads(jar.read(DF + name + ".json"))
        out = {"offset": offset(read("offset")), "factor": factor(read("factor")), "jaggedness": jaggedness(read("jaggedness"))}
        for name in ("final_density", "preliminary_surface_level"):
            doc = read(name)
            assert top_slide(doc) == 1, name
            out[name] = doc
    OUT.mkdir(parents=True, exist_ok=True)
    for name, doc in out.items():
        (OUT / f"{name}.json").write_text(json.dumps(doc, indent=2) + "\n", encoding="utf-8")
    OWN.parent.mkdir(parents=True, exist_ok=True)
    OWN.write_text(json.dumps(flatness(), indent=2) + "\n", encoding="utf-8")
    print("gen_terrain: wrote", ", ".join(out), "to", OUT.relative_to(BASE), "and", OWN.relative_to(BASE))


if __name__ == "__main__":
    main()
