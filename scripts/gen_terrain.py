"""Terrain shape (docs/terrain-plan.md section 1): overrides vanilla's overworld density functions.

Reads vanilla's JSON from the Minecraft jar and writes our versions to
data/minecraft/worldgen/density_function/overworld/:

- mechanical:flatness (data/mechanical/worldgen/density_function/flatness.json): 0..1, how flat the ground should be
  for its climate: 1 where it should be flat (dry: humidity below -0.15, i.e. plains, steppe, chalk downs, pine heath;
  warm and dry: savanna; hot: deserts and gypsum flats; frozen and not humid: snowy plains, ice spikes). Times gate():
  only on lowland and plateaus (erosion above -0.36) and on the valley floors between mountains (pv below -0.3), so
  peaks and mountainsides keep their shape.
- offset.json: the base height. The leaf splines over peaks-and-valleys (PV, ridges_folded: -1 river valley .. 1 ridge)
  for mid inland (continentalness 0.25) and far inland (1.0) are replaced from the OFFSET table below (with new erosion
  points -0.27 and -0.2 for the plateau scarp). Coasts (continentalness up to -0.1) and swamps (erosion 0.7) keep
  vanilla's values, so coastlines and swamps stay put. Height of a value v: y ~ 63.5 + 128 * v (before noise and
  jaggedness). The lowland blends into a calm copy of that spline (hills keep FLAT_KEEP of their height, gentler river
  banks, height above sea level times CALM_SCALE): fully where flatness is 1, by CALM_BASE on other lowland, so forest
  rivers keep deeper valleys than plains rivers.
- factor.json: how strongly the 3D noise roughens the ground (higher = calmer): vanilla's factor times
  1 + FLAT_BOOST * max(flatness, FLAT_BASE on all lowland). Windswept spots (vanilla factor below 2) keep their
  shattered look.
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
# pv -1 / 0 / 1). A contrast landscape: calm lowland between ranges, mountains and plateaus rising steeply out of it.
# - E -0.85, -0.7, -0.4: mountains (vanilla erosion bands E0, E1): high, the ridges (pv 1) at y 220-260 + jaggedness;
#   in E1 the valleys between the ridges (pv up to -0.3) have flat floors and the mountainsides rise steeply from them.
# - E -0.35 and -0.27 (the latter new): flat-topped plateaus (E2: Savanna Plateau, Marble Gorge); between -0.27 and the
#   new point -0.2 the plateau drops in one scarp to the lowland, instead of sloping over the whole E2/E3 range.
# - E -0.2 (new), -0.1: lowland (E3), flat up to pv 0.3, low ridges only on the highest pv; 0.2 .. 0.58: flat lowland
#   (E4, E5).
# The river sits at pv -1 (value below 0 = under sea level, water); its banks rise steeply to pv -0.84 and level out
# by -0.5, so the valley walls are steep but take little ground. Lowland rivers run 15-25 deep, mountain rivers deeper.
_PLATEAU_1 = [(-1, -0.2, 0.5), (-0.4, 0.62, 0), (0, 0.65, 0.05), (0.4, 0.7, 0.1), (1, 0.82, 0.2)]  # -0.2 / 0.5 / 0.6
_PLATEAU_025 = [(-1, -0.25, 0.5), (-0.4, 0.5, 0), (0, 0.52, 0.05), (0.4, 0.56, 0.1), (1, 0.66, 0.2)]  # -0.25 / 0.35 / 0.42
_LOW_1 = [(-1, -0.06, 0), (-0.84, 0.13, 0.45), (-0.5, 0.16, 0.03), (0.3, 0.19, 0.05), (0.7, 0.3, 0.45), (1, 0.45, 0.5)]  # -0.05 / 0.03 / 0.6
_LOW_025 = [(-1, -0.06, 0), (-0.84, 0.1, 0.4), (-0.5, 0.12, 0.03), (0.3, 0.14, 0.05), (0.7, 0.24, 0.4), (1, 0.38, 0.45)]  # -0.1 / 0.02 / 0.42
_FLAT_1 = [(-1, -0.06, 0), (-0.84, 0.09, 0.35), (-0.5, 0.11, 0.02), (0.4, 0.13, 0.03), (1, 0.2, 0.12)]  # -0.05 / 0.01 / 0.1
_FLAT_025 = [(-1, -0.06, 0), (-0.84, 0.065, 0.3), (-0.5, 0.08, 0.02), (0.4, 0.095, 0.03), (1, 0.16, 0.1)]  # -0.1 / 0.01 / 0.1
OFFSET = {
    # far inland
    (1.0, -0.85): [(-1, 0.32, 0), (0, 0.88, 0.5), (1, 1.34, 0.5)],  # 0.35 / 0.92 / 1.5
    (1.0, -0.7): [(-1, 0.28, 0), (-0.3, 0.32, 0.1), (0.2, 0.9, 1.1), (1, 1.42, 0.5)],  # 0.2 / 0.54 / 1.0
    (1.0, -0.4): [(-1, 0.24, 0), (-0.3, 0.28, 0.1), (0.2, 0.8, 1.1), (1, 1.36, 0.5)],  # 0.2 / 0.54 / 1.0
    (1.0, -0.35): _PLATEAU_1,
    (1.0, -0.27): _PLATEAU_1,
    (1.0, -0.2): _LOW_1,
    (1.0, -0.1): _LOW_1,
    (1.0, 0.2): _FLAT_1,
    (1.0, 0.4): _FLAT_1,
    (1.0, 0.58): [(-1, -0.06, 0), (-0.84, 0.07, 0.3), (-0.5, 0.09, 0.02), (0.4, 0.11, 0.03), (1, 0.17, 0.1)],
    # mid inland
    (0.25, -0.85): [(-1, 0.27, 0), (0, 0.82, 0.5), (1, 1.28, 0.5)],  # 0.2 / 0.72 / 1.23
    (0.25, -0.7): [(-1, 0.25, 0), (-0.3, 0.28, 0.1), (0.2, 0.8, 1.05), (1, 1.34, 0.5)],  # 0.2 / 0.45 / 0.88
    (0.25, -0.4): [(-1, 0.22, 0), (-0.3, 0.25, 0.1), (0.2, 0.72, 1.0), (1, 1.26, 0.5)],  # 0.2 / 0.31 / 0.7
    (0.25, -0.35): _PLATEAU_025,
    (0.25, -0.27): _PLATEAU_025,
    (0.25, -0.2): _LOW_025,
    (0.25, -0.1): _LOW_025,
    (0.25, 0.2): _FLAT_025,
    (0.25, 0.4): _FLAT_025,
    (0.25, 0.58): [(-1, -0.06, 0), (-0.84, 0.055, 0.25), (-0.5, 0.07, 0.02), (0.4, 0.085, 0.03), (1, 0.14, 0.08)],
}
# erosion 0.45 and 0.55 (windswept spots nested inside): every value above the river floor is lifted by this much
NESTED_LIFT = {1.0: 0.1, 0.25: 0.07}

FLAT_BOOST = 4.0  # factor multiplier - 1 in the flat biomes
FLAT_BASE = 0.6  # flatness of the 3D noise damping on all other lowland: forests, taiga, Peat Bog
CALM_BASE = 0.35  # ... and of the blend into the calm height there (river valleys stay deeper than in the flat biomes)
FLAT_KEEP = 0.12  # share of a lowland hill's height (above the valley rim at pv -0.5) left where the ground is flat
CALM_SCALE = 0.55  # ... and the calm lowland's height above sea level is scaled by this: shallower river valleys
CALM = (-0.2, -0.1, 0.2, 0.4, 0.58)  # erosion points (hilly and flat lowland) whose calm copy loses its hills
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
        # erosion points vanilla doesn't have (the plateau scarp): inserted in order
        for (oc, e), leaf in OFFSET.items():
            if oc == c and (c, e) not in done:
                pv = {"coordinate": "minecraft:overworld/ridges_folded", "points": pts(leaf)}
                cp["value"]["points"].append({"derivative": 0.0, "location": e, "value": pv})
                done.add((c, e))
        cp["value"]["points"].sort(key=lambda p: p["location"])
    missing = set(OFFSET) - done
    assert not missing, f"no vanilla spline at {missing}"
    calm = json.loads(json.dumps(root))
    for cp in calm["points"]:
        if cp["location"] in NESTED_LIFT:
            for ep in cp["value"]["points"]:
                if ep["location"] in CALM:
                    flatten(ep["value"])
    alpha = {"type": "minecraft:max", "left": FLATNESS, "right": {"type": "minecraft:mul", "left": CALM_BASE, "right": gate()}}
    doc["input"]["second"]["right"] = {"type": "minecraft:lerp", "alpha": alpha, "first": {"type": "minecraft:spline", "spline": root},
                                       "second": {"type": "minecraft:spline", "spline": calm}}
    return doc


def flatten(leaf):
    """Calm copy of a lowland PV spline: values beyond the valley rim (pv -0.5) keep FLAT_KEEP of their rise above it,
    the river bank (pv -0.84) sits halfway between the river floor and the rim, and the land's height above sea level is
    scaled by CALM_SCALE: low, even plains in shallow river valleys."""
    rim = next(p["value"] for p in leaf["points"] if p["location"] == -0.5)
    floor = next(p["value"] for p in leaf["points"] if p["location"] == -1)
    for p in leaf["points"]:
        if p["location"] > -0.5:
            p["value"] = round(rim + FLAT_KEEP * (p["value"] - rim), 4)
            p["derivative"] = round(FLAT_KEEP * p["derivative"], 4)
        elif -1 < p["location"] < -0.5:
            p["value"] = round((floor + rim) / 2, 4)
            p["derivative"] = round((rim - floor) / 0.5, 4)
    for p in leaf["points"]:
        if p["location"] > -1:
            p["value"] = round(CALM_SCALE * p["value"], 4)
            p["derivative"] = round(CALM_SCALE * p["derivative"], 4)


def flatness():
    hum = {"type": "minecraft:spline", "spline": spline("minecraft:overworld/vegetation", pts([(-0.15, 1.0, 0), (-0.05, 0.0, 0)]))}
    hot = {"type": "minecraft:spline", "spline": spline("minecraft:overworld/temperature", pts([(0.5, 0.0, 0), (0.6, 1.0, 0)]))}
    # the frozen band's drier part (Snowy Plains, Mammoth Steppe, Ice Spikes; Snowy Taiga is wetter, from 0.1)
    cold = {"type": "minecraft:mul",
            "left": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/temperature", pts([(-0.5, 1.0, 0), (-0.42, 0.0, 0)]))},
            "right": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/vegetation", pts([(0.06, 1.0, 0), (0.12, 0.0, 0)]))}}
    # warm and dry: Savanna right up to its wet edge (humidity -0.1)
    warm = {"type": "minecraft:mul",
            "left": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/temperature", pts([(0.15, 0.0, 0), (0.22, 1.0, 0)]))},
            "right": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/vegetation", pts([(-0.1, 1.0, 0), (-0.04, 0.0, 0)]))}}
    mx = lambda a, b: {"type": "minecraft:max", "left": a, "right": b}
    return {"type": "minecraft:cache", "input": {"type": "minecraft:mul", "left": mx(mx(hum, warm), mx(hot, cold)), "right": gate()}}


def gate():
    """Where ground may flatten at all: lowland and plateaus (erosion above -0.36) and the valley floors between mountains (pv below -0.3)."""
    return {"type": "minecraft:max",
            "left": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/erosion", pts([(-0.42, 0.0, 0), (-0.36, 1.0, 0)]))},
            "right": {"type": "minecraft:spline", "spline": spline("minecraft:overworld/ridges_folded", pts([(-0.4, 1.0, 0), (-0.15, 0.0, 0)]))}}


def factor(doc):
    blend = doc["input"]
    assert blend["type"] == "minecraft:lerp"
    vanilla = blend["second"]
    boosted = {"type": "minecraft:mul", "left": vanilla, "right": {"type": "minecraft:add", "left": 1.0,
                                                                   "right": {"type": "minecraft:mul", "left": FLAT_BOOST, "right": {
                                                                       "type": "minecraft:max", "left": FLATNESS,
                                                                       "right": {"type": "minecraft:mul", "left": FLAT_BASE, "right": gate()}}}}}
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
