"""3D models for docs/registry.html (used by gen_registry.py).

Entity models come from the Java exporter (`./gradlew exportModels` -> build/model_export/models.json, see
src/modelexport/java/com/mechanical/devtools/ModelExport.java); which entity uses which layer and texture is read from the
renderer registrations in the Java source, so new mobs show up without changes here. Block and multiblock models are
built from the block model JSON + blockstates. Every model becomes a list of textured quads ("geo") that the page draws
with three.js, plus an isometric PNG thumbnail rendered here with numpy.
"""
import base64
import glob
import io
import json
import math
import os
import re
import subprocess

import numpy as np
from PIL import Image

THUMB = 128  # thumbnail size (rendered at 2x, then downsampled)
BABY = {"calf", "cub", "lamb", "fawn", "piglet", "foal", "chick", "leveret"}
OVERLAY = re.compile(r"_(eyes|runes|overlay|glow|emissive)$")
VANILLA_FIELDS = {"ModelLayers.PLAYER": "minecraft:player#main", "ModelLayers.PLAYER_SLIM": "minecraft:player_slim#main",
                  "ModelLayers.ZOMBIE": "minecraft:zombie#main", "ModelLayers.SKELETON": "minecraft:skeleton#main"}
SKIP_ENTITIES = {"carcass", "falling_tree"}  # draw other mobs' models / blocks, nothing of their own to show


class Models:
    def __init__(self, g, use_gradle=True):
        self.g = g  # the gen_registry module: read_res, read_json, model_textures, warn, IMAGES, ...
        self.geo = {}       # geo key -> {"t": [image keys], "q": [flat quads]}
        self._geo_ids = {}  # json of quads -> geo key (identical geometry stored once)
        self.export = self._load_export(use_gradle)

    # ------------------------------------------------------------ Java export

    def _load_export(self, use_gradle):
        g = self.g
        path = os.path.join(g.ROOT, "build", "model_export", "models.json")
        sources = [f for d in ("src/main/java", "src/modelexport/java") for f in glob.glob(os.path.join(g.ROOT, d, "**", "*.java"), recursive=True)]
        sources += glob.glob(os.path.join(g.ROOT, "src", "main", "resources", "assets", "*", "textures", "entity", "**", "*.png"), recursive=True)
        newest = max((os.path.getmtime(f) for f in sources), default=0)
        stale = not os.path.isfile(path) or os.path.getmtime(path) < newest
        if stale and use_gradle:
            gradlew = os.path.join(g.ROOT, "gradlew.bat" if os.name == "nt" else "gradlew")
            print("Modell-exporten är gammal eller saknas: kör gradlew exportModels ...")
            try:
                r = subprocess.run([gradlew, "exportModels", "-q"], cwd=g.ROOT, capture_output=True, text=True,
                                   encoding="utf-8", errors="replace", timeout=900)
                if r.returncode != 0:
                    g.warn("gradlew exportModels misslyckades: " + (r.stderr or r.stdout).strip()[-800:])
            except Exception as e:
                g.warn(f"kunde inte köra gradlew exportModels: {e}")
            stale = not os.path.isfile(path) or os.path.getmtime(path) < newest
        if not os.path.isfile(path):
            g.warn("build/model_export/models.json saknas: inga 3D-modeller för mobs (kör ./gradlew exportModels)")
            return None
        if stale:
            g.warn("build/model_export/models.json är äldre än källkoden: mob-modellerna kan vara inaktuella (kör ./gradlew exportModels)")
        try:
            data = json.loads(open(path, encoding="utf-8").read())
        except Exception as e:
            g.warn(f"trasig models.json: {e}")
            return None
        seen = set()
        for e in data.get("errors", []):  # one line per layer: a failing setupAnim fails the same way for every variant
            if e.split(" ")[0] not in seen:
                seen.add(e.split(" ")[0])
                g.warn(f"modell-export: {e}")
        data["fields"] = {**VANILLA_FIELDS, **data.get("fields", {})}
        return data

    # ------------------------------------------------------------ shared: textures, geo, thumbnails

    def _texture(self, ref):
        """'ns:textures/....png' -> (image key, PIL image), embedded once in IMAGES."""
        key = "tex:" + ref
        ns, p = ref.split(":", 1)
        raw = self.g.read_res(f"assets/{ns}/{p}")
        if raw is None:
            return None, None
        img = Image.open(io.BytesIO(raw)).convert("RGBA")
        if key not in self.g.IMAGES:
            self.g.IMAGES[key] = _png_uri(img)
        return key, img

    def _block_texture(self, ref):
        ns, p = self.g.split_id(ref)
        img = self.g.load_texture(f"{ns}:{p}")  # first frame of animated strips
        if img is None:
            return None, None
        key = f"tex:{ns}:textures/{p}.png"
        if key not in self.g.IMAGES:
            self.g.IMAGES[key] = _png_uri(img)
        return key, img

    def add_geo(self, quads, tex_keys, tex_imgs, thumb_key):
        """quads: [(texture index, [12 floats xyz], [8 floats uv 0..1])] -> (geo key, thumbnail image key) or (None, None).
        thumb_key None: no thumbnail (the page shows the item picture before the viewer loads)."""
        if not quads:
            return None, None
        flat = []
        for ti, pos, uv in quads:
            flat.append(ti)
            flat += [round(v, 4) for v in pos]
            flat += [round(v, 5) for v in uv]
        sig = json.dumps([tex_keys, flat])
        gk = self._geo_ids.get(sig)
        if gk is None:
            gk = self._geo_ids[sig] = f"g{len(self.geo)}"
            self.geo[gk] = {"t": tex_keys, "q": flat}
        if thumb_key:
            self.g.IMAGES[thumb_key] = _png_uri(render_thumb(quads, tex_imgs))
        return gk, thumb_key

    # ------------------------------------------------------------ entities

    def entities(self):
        """[{id, name, vanilla, creature, variants: [{label, geo, img}]}] for every entity with an exported model."""
        g, ex = self.g, self.export
        if not ex:
            return []
        layers, fields = ex["layers"], ex["fields"]
        java = {os.path.splitext(os.path.basename(f))[0]: f for f in glob.glob(os.path.join(g.JAVA, "**", "*.java"), recursive=True)}
        src = lambda name: g.strip_comments(open(java[name], encoding="utf-8").read()) if name in java else ""
        tex_root = os.path.join(g.RES, "assets", g.MOD, "textures", "entity")
        all_tex = sorted(os.path.relpath(f, tex_root).replace("\\", "/") for f in glob.glob(os.path.join(tex_root, "**", "*.png"), recursive=True))
        all_tex = [t for t in all_tex if not t.split("/")[0].startswith("skinned")]  # carcass skins, not live looks

        def textures_for(eid):
            out = []
            for t in all_tex:
                stem, parts = t.rsplit("/", 1)[-1][:-4], t.split("/")[:-1]
                if (stem == eid or stem.startswith(eid + "_") or eid in parts) and not OVERLAY.search(stem):
                    out.append(f"{g.MOD}:textures/entity/{t}")
            return out

        out = []
        for name in sorted(java):
            code = src(name)
            if "registerEntityRenderer(" not in code:
                continue
            for m in re.finditer(r"registerEntityRenderer\s*\(", code):
                body = g.balanced(code, m.end() - 1)
                if not body:
                    continue
                args = g.split_top(body)
                ent = re.search(r"\.([A-Z][A-Z0-9_]*)(?:\.get\(\))?\s*$", args[0]) if args else None
                if not ent:
                    continue
                eid, vanilla = ent.group(1).lower(), "EntityTypes." in args[0]
                if eid in SKIP_ENTITIES:
                    continue
                try:
                    out.append(self._entity(eid, vanilla, ",".join(args[1:]), code, src, java, layers, fields, textures_for))
                except Exception as e:
                    g.warn(f"3D-modell för {eid} misslyckades: {e}")
        return [e for e in out if e]

    def _entity(self, eid, vanilla, rend, file_code, src, java, layers, fields, textures_for):
        g = self.g
        ns = "minecraft" if vanilla else g.MOD

        def tokens(text):
            found = [fields[f"{a}.{b}"] for a, b in re.findall(r"\b([A-Z]\w*)\.([A-Z][A-Z0-9_]*)\b", text) if f"{a}.{b}" in fields]
            return [l for l in dict.fromkeys(found) if l in layers]

        rcls = re.search(r"\b(?:new\s+)?([A-Z]\w*Renderer)\b\s*(?:::|<|\()", rend)
        rcode = src(rcls.group(1)) if rcls else ""
        found = tokens(rend) or [l for l in (f"{ns}:{eid}#main",) if l in layers] or tokens(rcode)
        if not found:
            g.warn(f"ingen 3D-modell hittades för entity {eid}")
            return None
        adult, baby = found[0], (found[1] if len(found) > 1 and found[1].endswith("#main") else found[0])
        baby_scale = 1.0
        war = re.search(r"new\s+WildAnimalRenderer<>\s*\(", rend)
        if war:
            wargs = g.split_top(g.balanced(rend, war.end() - 1) or "")
            if len(wargs) >= 4:
                baby_scale = float(re.sub(r"[fF]$", "", wargs[3]) or 1)
        sc = re.search(r"poseStack\.scale\(\s*([\d.]+)F\s*,\s*([\d.]+)F\s*,\s*([\d.]+)F", rend)
        scale = tuple(float(v) for v in sc.groups()) if sc else (1.0, 1.0, 1.0)
        ecode = src("".join(w.title() for w in eid.split("_")))

        # variants: (label, texture ref, layer, horns, scale)
        variants = []
        look = re.search(r"enum\s+" + "".join(w.title() for w in eid.split("_")) + r"Look\s*\{([^;}]*)", src("Undead")) if vanilla else None
        if look:  # the mod's undead looks: own skin, or the vanilla one
            per_look = {k: tuple(float(v) for v in s) for k, *s in re.findall(
                r"Look\.(\w+)\)\s*poseStack\.scale\(\s*([\d.]+)F\s*,\s*([\d.]+)F\s*,\s*([\d.]+)F", file_code)}
            for lk in re.findall(r"\b([A-Z][A-Z0-9_]*)\b\s*(?:\(|,|$)", look.group(1)):
                ref = f"{g.MOD}:textures/entity/undead/{eid}_{lk.lower()}.png"
                if g.read_res(f"assets/{g.MOD}/textures/entity/undead/{eid}_{lk.lower()}.png") is None:
                    ref = f"minecraft:textures/entity/{eid}/{eid}.png"
                variants.append((lk.replace("_", " ").title(), ref, adult, True, per_look.get(lk, scale)))
        else:
            texes = textures_for(eid)
            if not texes:  # e.g. TameHorse: TEXTURE = ...("textures/entity/wildlife/wild_horse.png")
                texes = [f"{g.MOD}:{t}" for t in dict.fromkeys(re.findall(r'"(textures/entity/[^"]+\.png)"', ecode))]
            if not texes:  # or a constant of another entity class
                other = re.search(r"\b([A-Z]\w+)\.(?:[A-Z_]*_)?TEXTURE\b", ecode)
                if other:
                    texes = textures_for(re.sub(r"(?<!^)([A-Z])", r"_\1", other.group(1)).lower())
            if not texes and vanilla:
                texes = [f"minecraft:textures/entity/{eid}/{eid}.png"]
            hb = re.search(r"boolean\s+showsHorns\s*\(\s*\)\s*\{(.*?)\}", ecode, re.S)
            needs = [x.lower() for x in re.findall(r"\bis([A-Z]\w*)\(\)", hb.group(1)) if x != "Baby"] if hb else None
            for ref in texes:
                stem = ref.rsplit("/", 1)[-1][:-4]
                rest = stem[len(eid) + 1:] if stem.startswith(eid + "_") else ("" if stem == eid else stem)
                suffix = stem.rsplit("_", 1)[-1]
                is_baby = suffix in BABY
                horns = needs is not None and not is_baby and (not needs or suffix in needs)
                layer = baby if is_baby else adult
                s = scale
                if is_baby and baby == adult:
                    s = tuple(v * baby_scale for v in scale)
                if layer == "minecraft:player#main" and rest[-1:].isdigit() and int(rest[-1]) % 2 == 1:
                    # shortcut: Tribesperson.isWoman() is "odd skin number" -> slim arms; upgrade if other player-model mobs appear
                    layer = "minecraft:player_slim#main"
                variants.append((rest.replace("_", " ").title() or "Standard", ref, layer, horns, s))
        if not variants:
            variants.append(("Standard", None, adult, True, scale))
        variants.sort(key=lambda v: v[1] is not None and v[1].rsplit("/", 1)[-1][:-4].rsplit("_", 1)[-1] in BABY)  # adults first

        res = []
        for label, ref, layer, horns, s in variants:
            L = layers[layer]
            poses = L["poses"]
            quads_src = poses.get(f"{ref}|{'horns' if horns else 'nohorns'}") or poses.get(ref or "") or poses["default"]
            tkey, timg = self._texture(ref) if ref else (None, None)
            if tkey is None:
                self.g.warn(f"{eid}: textur saknas {ref}")
                continue
            tw, th = L["tw"] or 64, L["th"] or 64
            quads = []
            for q in quads_src:
                pos = []
                for i in range(4):  # renderer flips x and y (scale -1,-1,1); model pixels -> blocks
                    pos += [-q[3 * i] / 16 * s[0], -q[3 * i + 1] / 16 * s[1], q[3 * i + 2] / 16 * s[2]]
                uv = [q[12 + i] / (tw if i % 2 == 0 else th) for i in range(8)]
                quads.append((0, pos, uv))
            gk, ik = self.add_geo(quads, [tkey], [timg], f"thumb:entity:{eid}:{label}")
            if gk:
                res.append({"label": label, "geo": gk, "img": ik})
        creature = vanilla or "WildAnimalRenderer" in rend or "BirdRenderer" in rend or "MobRenderer" in rcode
        lang = g.VAN_LANG if vanilla else g.MOD_LANG
        return {"id": eid, "vanilla": vanilla, "creature": creature, "variants": res,
                "name": lang.get(f"entity.{ns}.{eid}", eid.replace("_", " ").title())}

    # ------------------------------------------------------------ blocks

    def block_quads(self, layout, model_ref=None):
        """layout: [(dx, dy, dz, block id, {prop: value})] -> (quads, texture keys, texture images).
        model_ref: draw this model (the item's block model) instead of the blockstate's for a one-block layout."""
        quads, keys, imgs = [], [], []
        for dx, dy, dz, block, given in layout:
            for model, rx, ry in [(model_ref, 0, 0)] if model_ref else self.state_models(block, given):
                tex = self.g.model_textures(model)
                for el in model_elements(self.g, model):
                    for face, pos, uv, tref in element_faces(el, tex):
                        k, img = self._block_texture(tref)
                        if k is None:
                            continue
                        if k not in keys:
                            keys.append(k)
                            imgs.append(img)
                        p = []
                        for i in range(4):
                            x, y, z = rotate_block(pos[3 * i:3 * i + 3], rx, ry)
                            p += [x / 16 + dx, y / 16 + dy, z / 16 + dz]
                        quads.append((keys.index(k), p, uv))
        return quads, keys, imgs

    def state_models(self, block, given):
        ns, p = self.g.split_id(block)
        bs = self.g.read_json(f"assets/{ns}/blockstates/{p}.json")
        if not bs:
            return []
        st = default_state(blockstate_props(bs), given)
        out = []
        if "variants" in bs:
            for key, v in bs["variants"].items():
                pairs = [kv.split("=", 1) for kv in key.split(",") if "=" in kv]
                if all(st.get(k) == val for k, val in pairs):
                    v = v[0] if isinstance(v, list) else v
                    out.append((v["model"], v.get("x", 0), v.get("y", 0)))
                    break
        for part in bs.get("multipart", []):
            if "when" not in part or match_when(part["when"], st):
                a = part["apply"][0] if isinstance(part["apply"], list) else part["apply"]
                out.append((a["model"], a.get("x", 0), a.get("y", 0)))
        return out

    def blocks(self, block_ids):
        """{block id: {"geo", "img", "multi", "thumb"}} for every block with a blockstate. Two- and three-high blocks
        (half / part = lower, middle, upper) are stacked, the Fire Pit laid out 2x2; "thumb" says whether the rendered
        picture should replace the item picture (multiblocks and blocks whose item shows the non-cube block model)."""
        out = {}
        for rid in sorted(block_ids):
            try:
                props = self.block_props(rid)
                if props is None:
                    continue
                layout = [(0, 0, 0, rid, {})]
                for key in ("half", "part"):
                    order = [v for v in ("lower", "middle", "upper") if v in props.get(key, [])]
                    if "lower" in order and "upper" in order:
                        layout = [(0, dy, 0, rid, {key: v}) for dy, v in enumerate(order)]
                if rid in FIXED_LAYOUTS:
                    layout = [(dx, dy, dz, rid, st) for dx, dy, dz, st in FIXED_LAYOUTS[rid]]
                item_model = self._item_block_model(rid)
                # one block: the item's model when it is a block model (fence/button inventory models look right)
                quads, keys, imgs = self.block_quads(layout, item_model if len(layout) == 1 else None)
                thumb = len(layout) > 1 or bool(item_model)
                gk, ik = self.add_geo(quads, keys, imgs, f"thumb:block:{rid}" if thumb else None)
                if not gk:
                    continue
                # doors and tall plants are two blocks high too, but not what "multiblock" means on the page
                multi = len(layout) > 1 and "hinge" not in props and not any("cross" in k for k in self.g.model_textures(
                    self.state_models(rid, layout[0][4])[0][0]) if self.state_models(rid, layout[0][4]))
                out[rid] = {"geo": gk, "img": ik, "multi": multi, "thumb": thumb}
            except Exception as e:
                self.g.warn(f"3D-modell för {rid} misslyckades: {e}")
        return out

    def _item_block_model(self, rid):
        """The item's model when it is a non-cube block model (else None: flat sprite or plain cube, keep the old picture)."""
        ns, p = self.g.split_id(rid)
        idef = self.g.read_json(f"assets/{ns}/items/{p}.json")
        m = self.g.first_model(idef.get("model")) if idef else None
        if not m or "block/" not in m:
            return None
        els = model_elements(self.g, m)
        full = len(els) == 1 and els[0]["from"] == [0, 0, 0] and els[0]["to"] == [16, 16, 16]
        return m if els and not full else None

    def smelting_furnace(self):
        """The Smelting Furnace from FurnaceTapBlockEntity.check(): bricks in the loop ranges, the middle of the centre
        column empty, the Furnace Chute on top of it, the tap in front of the bottom middle (shaft = tap pos + opposite of
        facing; facing north), the side and back walls' bottom middles shown as the tuyeres the formed furnace marks."""
        path = os.path.join(self.g.JAVA, "FurnaceTapBlockEntity.java")
        code = self.g.strip_comments(open(path, encoding="utf-8").read()) if os.path.isfile(path) else ""
        body = re.search(r"void\s+check\s*\(.*?\{(.*?)\n    \}", code, re.S)
        rng = dict.fromkeys(("dx", "dy", "dz"), (-1, 1))
        for v, lo, hi in re.findall(r"for\s*\(\s*int\s+(d[xyz])\s*=\s*(-?\d+)\s*;\s*\1\s*<=\s*(-?\d+)", body.group(1) if body else ""):
            rng[v] = (int(lo), int(hi))
        if not body:
            self.g.warn("hittade inte FurnaceTapBlockEntity.check(): Smelting Furnace ritas 3x3x3 som standard")
        layout = [(0, 0, -1, f"{self.g.MOD}:furnace_tap", {"facing": "north"})]
        for dy in range(rng["dy"][0], rng["dy"][1] + 1):
            for dx in range(rng["dx"][0], rng["dx"][1] + 1):
                for dz in range(rng["dz"][0], rng["dz"][1] + 1):
                    if (dx, dy, dz) == (0, 0, -1) or ((dx, dz) == (0, 0) and dy > rng["dy"][0]):
                        continue
                    tuyere = {(1, 0): "east", (-1, 0): "west", (0, 1): "south"}.get((dx, dz)) if dy == rng["dy"][0] else None
                    layout.append((dx, dy, dz, f"{self.g.MOD}:furnace_bricks", {"facing": tuyere, "tuyere": "cold"} if tuyere else {}))
        layout.append((0, rng["dy"][1], 0, f"{self.g.MOD}:furnace_chute", {}))
        quads, keys, imgs = self.block_quads(layout)
        return self.add_geo(quads, keys, imgs, "thumb:multiblock:smelting_furnace")

    def block_props(self, block):
        ns, p = self.g.split_id(block)
        bs = self.g.read_json(f"assets/{ns}/blockstates/{p}.json")
        return blockstate_props(bs) if bs else None


# FirePitBlock: PART 0 is the north-west corner, x offset = part & 1, z offset = part >> 1
FIXED_LAYOUTS = {"mechanical:fire_pit": [(p & 1, 0, p >> 1, {"part": str(p)}) for p in range(4)]}


# ---------------------------------------------------------------- blockstates

def blockstate_props(bs):
    props = {}

    def add(k, v):
        for x in str(v).lower().split("|"):
            props.setdefault(k, [])
            if x not in props[k]:
                props[k].append(x)

    def walk(when):
        if isinstance(when, dict):
            for k, v in when.items():
                if k in ("OR", "AND"):
                    for w in v:
                        walk(w)
                else:
                    add(k, v)

    for key in bs.get("variants", {}):
        for kv in key.split(","):
            if "=" in kv:
                add(*kv.split("=", 1))
    for part in bs.get("multipart", []):
        walk(part.get("when"))
    return props


def default_state(props, given):
    st = {}
    for k, vals in props.items():
        if k in given:
            st[k] = str(given[k]).lower()
        elif k == "facing" and "north" in vals:
            st[k] = "north"
        elif "false" in vals or vals == ["true"]:  # multipart fences list only "true"
            st[k] = "false"
        elif "0" in vals:
            st[k] = "0"
        else:
            st[k] = sorted(vals, key=lambda v: (not v.isdigit(), int(v) if v.isdigit() else 0, v))[0]
    return st


def match_when(when, st):
    if "OR" in when:
        return any(match_when(w, st) for w in when["OR"])
    if "AND" in when:
        return all(match_when(w, st) for w in when["AND"])
    return all(st.get(k) in str(v).lower().split("|") for k, v in when.items())


# ---------------------------------------------------------------- block model geometry

def model_elements(g, ref, depth=0):
    ns, p = g.split_id(ref)
    m = g.read_json(f"assets/{ns}/models/{p}.json")
    if m is None or depth > 15:
        return []
    if "elements" in m:
        return m["elements"]
    par = m.get("parent")
    return model_elements(g, par, depth + 1) if par and not par.startswith("builtin") else []


# corners of each face as seen from outside: top-left, top-right, bottom-right, bottom-left
_FACES = {
    "north": lambda a, b: [(b[0], b[1], a[2]), (a[0], b[1], a[2]), (a[0], a[1], a[2]), (b[0], a[1], a[2])],
    "south": lambda a, b: [(a[0], b[1], b[2]), (b[0], b[1], b[2]), (b[0], a[1], b[2]), (a[0], a[1], b[2])],
    "west": lambda a, b: [(a[0], b[1], a[2]), (a[0], b[1], b[2]), (a[0], a[1], b[2]), (a[0], a[1], a[2])],
    "east": lambda a, b: [(b[0], b[1], b[2]), (b[0], b[1], a[2]), (b[0], a[1], a[2]), (b[0], a[1], b[2])],
    "up": lambda a, b: [(a[0], b[1], a[2]), (b[0], b[1], a[2]), (b[0], b[1], b[2]), (a[0], b[1], b[2])],
    "down": lambda a, b: [(a[0], a[1], b[2]), (b[0], a[1], b[2]), (b[0], a[1], a[2]), (a[0], a[1], a[2])],
}
_DEFAULT_UV = {
    "north": lambda a, b: [16 - b[0], 16 - b[1], 16 - a[0], 16 - a[1]],
    "south": lambda a, b: [a[0], 16 - b[1], b[0], 16 - a[1]],
    "west": lambda a, b: [a[2], 16 - b[1], b[2], 16 - a[1]],
    "east": lambda a, b: [16 - b[2], 16 - b[1], 16 - a[2], 16 - a[1]],
    "up": lambda a, b: [a[0], a[2], b[0], b[2]],
    "down": lambda a, b: [a[0], 16 - b[2], b[0], 16 - a[2]],
}


def element_faces(el, tex):
    """Yields (face, 12 positions in pixels, 8 uvs 0..1, texture ref) for one model element."""
    a, b = el["from"], el["to"]
    rot = el.get("rotation")
    for face, f in el.get("faces", {}).items():
        if face not in _FACES:
            continue
        t = f.get("texture", "")
        t = tex.get(t[1:], None) if t.startswith("#") else t
        if not t:
            continue
        u1, v1, u2, v2 = f.get("uv") or _DEFAULT_UV[face](a, b)
        corners = [(u1, v1), (u2, v1), (u2, v2), (u1, v2)]
        k = (f.get("rotation", 0) // 90) % 4
        corners = [corners[(i - k) % 4] for i in range(4)]
        pos = []
        for c in _FACES[face](a, b):
            pos += list(rotate_element(c, rot) if rot else c)
        yield face, pos, [v / 16 for c in corners for v in c], t


def rotate_element(p, rot):
    ang = math.radians(rot.get("angle", 0))
    o = rot.get("origin", [8, 8, 8])
    axis = rot.get("axis", "y")
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(ang), math.sin(ang)
    k = 1 / c if rot.get("rescale") and c else 1
    if axis == "x":
        y, z = (y * c - z * s) * k, (y * s + z * c) * k
    elif axis == "y":
        x, z = (x * c + z * s) * k, (-x * s + z * c) * k
    else:
        x, y = (x * c - y * s) * k, (x * s + y * c) * k
    return x + o[0], y + o[1], z + o[2]


def rotate_block(p, rx, ry):
    """Blockstate x/y rotation (clockwise, in 90 degree steps) around the block centre."""
    x, y, z = p[0] - 8, p[1] - 8, p[2] - 8
    for _ in range((rx // 90) % 4):
        y, z = z, -y
    for _ in range((ry // 90) % 4):
        x, z = -z, x
    return x + 8, y + 8, z + 8


# ---------------------------------------------------------------- thumbnail renderer

def render_thumb(quads, textures, size=THUMB, ss=2):
    """Isometric-ish orthographic view from the front-left (north-west) and above, z-buffered, alpha cut-out."""
    S = size * ss
    cam = np.array([-1.0, 0.85, -1.0])
    f = -cam / np.linalg.norm(cam)
    r = np.cross(f, [0.0, 1.0, 0.0])
    r /= np.linalg.norm(r)
    u = np.cross(r, f)
    light = np.array([-0.45, 1.0, -0.7])
    light /= np.linalg.norm(light)
    P = np.array([q[1] for q in quads], dtype=np.float64).reshape(-1, 4, 3)
    UV = np.array([q[2] for q in quads], dtype=np.float64).reshape(-1, 4, 2)
    sx, sy, sd = P @ r, -(P @ u), P @ f
    minx, maxx, miny, maxy = sx.min(), sx.max(), sy.min(), sy.max()
    scale = S * 0.9 / max(maxx - minx, maxy - miny, 1e-6)
    ox, oy = (S - (maxx - minx) * scale) / 2 - minx * scale, (S - (maxy - miny) * scale) / 2 - miny * scale
    X, Y = sx * scale + ox, sy * scale + oy
    color = np.zeros((S, S, 4), dtype=np.uint8)
    zbuf = np.full((S, S), np.inf)
    texs = [np.asarray(t, dtype=np.uint8) for t in textures]
    for qi, (ti, _, _) in enumerate(quads):
        tex = texs[ti]
        th, tw = tex.shape[:2]
        n = np.cross(P[qi, 1] - P[qi, 0], P[qi, 2] - P[qi, 0])
        if np.linalg.norm(n) < 1e-12:
            n = np.cross(P[qi, 2] - P[qi, 0], P[qi, 3] - P[qi, 0])
        nn = np.linalg.norm(n)
        if nn < 1e-12:
            continue
        n /= nn
        if n @ f > 0:
            n = -n
        shade = 0.55 + 0.45 * max(0.0, float(n @ light))
        for tri in ((0, 1, 2), (0, 2, 3)):
            x0, y0 = X[qi, tri[0]], Y[qi, tri[0]]
            x1, y1 = X[qi, tri[1]], Y[qi, tri[1]]
            x2, y2 = X[qi, tri[2]], Y[qi, tri[2]]
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if abs(area) < 1e-9:
                continue
            bx0, bx1 = max(int(min(x0, x1, x2)), 0), min(int(max(x0, x1, x2)) + 1, S)
            by0, by1 = max(int(min(y0, y1, y2)), 0), min(int(max(y0, y1, y2)) + 1, S)
            if bx0 >= bx1 or by0 >= by1:
                continue
            gx, gy = np.meshgrid(np.arange(bx0, bx1) + 0.5, np.arange(by0, by1) + 0.5)
            w0 = ((x1 - gx) * (y2 - gy) - (x2 - gx) * (y1 - gy)) / area
            w1 = ((x2 - gx) * (y0 - gy) - (x0 - gx) * (y2 - gy)) / area
            w2 = 1 - w0 - w1
            inside = (w0 >= -1e-6) & (w1 >= -1e-6) & (w2 >= -1e-6)
            if not inside.any():
                continue
            d = w0 * sd[qi, tri[0]] + w1 * sd[qi, tri[1]] + w2 * sd[qi, tri[2]]
            uu = w0 * UV[qi, tri[0], 0] + w1 * UV[qi, tri[1], 0] + w2 * UV[qi, tri[2], 0]
            vv = w0 * UV[qi, tri[0], 1] + w1 * UV[qi, tri[1], 1] + w2 * UV[qi, tri[2], 1]
            tx = np.clip((uu * tw).astype(int), 0, tw - 1)
            ty = np.clip((vv * th).astype(int), 0, th - 1)
            px = tex[ty, tx]
            zb = zbuf[by0:by1, bx0:bx1]
            ok = inside & (px[..., 3] >= 128) & (d < zb - 1e-7)
            if not ok.any():
                continue
            zb[ok] = d[ok]
            c = color[by0:by1, bx0:bx1]
            rgb = (px[..., :3].astype(np.float64) * shade).clip(0, 255).astype(np.uint8)
            c[ok, :3] = rgb[ok]
            c[ok, 3] = 255
    return Image.fromarray(color, "RGBA").resize((size, size), Image.BOX)


def _png_uri(img):
    buf = io.BytesIO()
    img.save(buf, "PNG", optimize=True)
    return "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")


if __name__ == "__main__":  # self-check of the pure helpers
    assert rotate_block((8, 8, 0), 0, 90) == (16, 8, 8)  # north face -> east with y=90
    assert match_when({"OR": [{"part": "0", "lit": "true"}]}, {"part": "0", "lit": "true"})
    assert not match_when({"part": "1|2|3"}, {"part": "0"})
    assert default_state({"facing": ["east", "north"], "lit": ["true", "false"], "part": ["2", "0"]}, {}) == {"facing": "north", "lit": "false", "part": "0"}
    q = [(0, [0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0], [0, 0, 1, 0, 1, 1, 0, 1])]
    img = render_thumb(q, [Image.new("RGBA", (2, 2), (255, 0, 0, 255))], size=16)
    assert img.getbbox() is not None
    print("ok")
