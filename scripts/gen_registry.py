"""Generate docs/registry.html: a searchable, self-contained registry of every item/block the mod adds or changes.

Run from the project root:  python scripts/gen_registry.py
Reads lang, models, textures, recipes (JSON + the Java-coded Tool Station / Clay Kiln / Fletching recipes), tags and
docs/registry_notes.json. Never crashes on bad input: problems are collected as warnings and printed at the end.
"""
import base64
import glob
import io
import json
import os
import re
import sys
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "src", "main", "resources")
JAVA = os.path.join(ROOT, "src", "main", "java", "com", "mechanical")
JAR_GLOB = os.path.join(ROOT, "build", "moddev", "artifacts", "minecraft-patched-*-merged.jar")
NOTES = os.path.join(ROOT, "docs", "registry_notes.json")
OUT = os.path.join(ROOT, "docs", "registry.html")
MOD = "mechanical"
ICON = 64  # embedded picture size (nearest-neighbour)

WARN = []


def warn(msg):
    WARN.append(msg)


# ---------------------------------------------------------------- resources (mod folder first, then vanilla jar)

_jar = None
for _p in sorted(glob.glob(JAR_GLOB)):
    try:
        _jar = zipfile.ZipFile(_p)
    except Exception as e:
        warn(f"kan inte öppna {_p}: {e}")
if _jar is None:
    warn("hittade ingen merged minecraft-jar: vanilla-bilder och taggar saknas")
_jar_names = set(_jar.namelist()) if _jar else set()


def read_res(path):
    """path like 'assets/minecraft/models/item/x.json'. Mod resources win over the jar."""
    p = os.path.join(RES, *path.split("/"))
    if os.path.isfile(p):
        with open(p, "rb") as f:
            return f.read()
    if path in _jar_names:
        return _jar.read(path)
    return None


def read_json(path):
    raw = read_res(path)
    if raw is None:
        return None
    try:
        return json.loads(raw.decode("utf-8"))
    except Exception as e:
        warn(f"trasig JSON {path}: {e}")
        return None


def split_id(rid, default="minecraft"):
    rid = rid.lstrip("#")
    return rid.split(":", 1) if ":" in rid else (default, rid)


def full_id(rid):
    ns, p = split_id(rid)
    return f"{ns}:{p}"


# ---------------------------------------------------------------- tags

_tag_cache = {}


def tag_members(tag, depth=0):
    """'#ns:path' -> list of item ids (mod tag files merged with the jar's, nested tags expanded)."""
    tag = "#" + full_id(tag)
    if tag in _tag_cache:
        return _tag_cache[tag]
    _tag_cache[tag] = []  # guards against cycles
    ns, p = split_id(tag)
    out = []
    sources = []
    modfile = os.path.join(RES, "data", ns, "tags", "item", *p.split("/")) + ".json"
    jarpath = f"data/{ns}/tags/item/{p}.json"
    mod = None
    if os.path.isfile(modfile):
        try:
            with open(modfile, encoding="utf-8") as f:
                mod = json.load(f)
            sources.append(mod)
        except Exception as e:
            warn(f"trasig tagg {modfile}: {e}")
    if (mod is None or not mod.get("replace")) and jarpath in _jar_names:
        try:
            sources.append(json.loads(_jar.read(jarpath)))
        except Exception as e:
            warn(f"trasig tagg {jarpath}: {e}")
    if not sources:
        warn(f"okänd tagg {tag}")
    for src in sources:
        for v in src.get("values", []):
            v = v.get("id") if isinstance(v, dict) else v
            if not isinstance(v, str):
                continue
            if v.startswith("#"):
                if depth < 10:
                    out += tag_members(v, depth + 1)
            else:
                out.append(full_id(v))
    out = list(dict.fromkeys(out))
    _tag_cache[tag] = out
    return out


# ---------------------------------------------------------------- pictures

_img_cache = {}  # texture ref -> PIL image or None
IMAGES = {}      # key -> data uri (embedded once each)


def load_texture(ref):
    ns, p = split_id(ref)
    key = f"{ns}:{p}"
    if key in _img_cache:
        return _img_cache[key]
    raw = read_res(f"assets/{ns}/textures/{p}.png")
    img = None
    if raw:
        try:
            img = Image.open(io.BytesIO(raw)).convert("RGBA")
            if img.height > img.width:  # animated strip: first frame
                img = img.crop((0, 0, img.width, img.width))
        except Exception as e:
            warn(f"kan inte läsa textur {key}: {e}")
    _img_cache[key] = img
    return img


def model_textures(model_ref, depth=0):
    """Texture map of a model with its parents merged in, '#var' references resolved."""
    ns, p = split_id(model_ref)
    m = read_json(f"assets/{ns}/models/{p}.json")
    if m is None or depth > 15:
        return {}
    tex = model_textures(m["parent"], depth + 1) if "parent" in m and not m["parent"].startswith("builtin") else {}
    tex.update(m.get("textures", {}))
    for _ in range(5):
        for k, v in tex.items():
            if isinstance(v, str) and v.startswith("#"):
                tex[k] = tex.get(v[1:], v)
    return {k: v for k, v in tex.items() if isinstance(v, str) and not v.startswith("#")}


def iso_cube(top, side):
    """Small isometric block picture from a top and a side texture."""
    out = Image.new("RGBA", (ICON, ICON))
    px = out.load()
    h = ICON / 2
    q = ICON / 4
    for y in range(ICON):
        for x in range(ICON):
            fx, fy = x + 0.5, y + 0.5
            u = ((fx - h) / h + fy / q) / 2
            v = (fy / q - (fx - h) / h) / 2
            if 0 <= u < 1 and 0 <= v < 1:
                tex, shade = top, 1.0
            elif fx < h:
                u = fx / h
                v = (fy - q - q * u) / h
                tex, shade = side, 0.8
            else:
                u = (fx - h) / h
                v = (fy - h + q * u) / h
                tex, shade = side, 0.62
            if not (0 <= u < 1 and 0 <= v < 1):
                continue
            r, g, b, a = tex.getpixel((int(u * tex.width), int(v * tex.height)))
            px[x, y] = (int(r * shade), int(g * shade), int(b * shade), a)
    return out


def picture_from_model(model_ref):
    tex = model_textures(model_ref)
    if not tex:
        return None
    if "layer0" not in tex and "cross" not in tex:
        top = tex.get("top") or tex.get("end") or tex.get("all")
        side = tex.get("side") or tex.get("all")
        if top and side:
            t, s = load_texture(top), load_texture(side)
            if t and s:
                return iso_cube(t, s)
    for k in ("layer0", "cross", "front", "side", "all", "top", "texture", "particle"):
        if k in tex and load_texture(tex[k]):
            return load_texture(tex[k])
    for v in tex.values():
        if load_texture(v):
            return load_texture(v)
    return None


def first_model(node):
    """First model reference in an item-model definition (fallback / on_false first, like the default look)."""
    if isinstance(node, dict):
        for key in ("fallback", "on_false"):
            if key in node:
                r = first_model(node[key])
                if r:
                    return r
        if isinstance(node.get("model"), str):
            return node["model"]
        if isinstance(node.get("base"), str):
            return node["base"]
        for v in node.values():
            r = first_model(v)
            if r:
                return r
    elif isinstance(node, list):
        for v in node:
            r = first_model(v)
            if r:
                return r
    return None


def picture(item_id):
    """Returns an image key (into IMAGES) or None."""
    if item_id in _pic_done:
        return _pic_done[item_id]
    ns, p = split_id(item_id)
    img = None
    try:
        idef = read_json(f"assets/{ns}/items/{p}.json")
        if idef:
            m = first_model(idef.get("model"))
            if m:
                img = picture_from_model(m)
        if img is None:
            bs = read_json(f"assets/{ns}/blockstates/{p}.json")
            if bs:
                m = first_model(bs.get("variants") or bs.get("multipart"))
                if m:
                    img = picture_from_model(m)
        if img is None:
            for d in ("item", "block"):
                img = load_texture(f"{ns}:{d}/{p}")
                if img:
                    break
    except Exception as e:
        warn(f"bild för {item_id} misslyckades: {e}")
        img = None
    key = None
    if img is not None:
        if img.width < ICON:
            f = max(1, ICON // img.width)
            img = img.resize((img.width * f, img.height * f), Image.NEAREST)
        buf = io.BytesIO()
        img.save(buf, "PNG", optimize=True)
        key = item_id
        IMAGES[key] = "data:image/png;base64," + base64.b64encode(buf.getvalue()).decode("ascii")
    else:
        warn(f"ingen bild för {item_id}")
    _pic_done[item_id] = key
    return key


_pic_done = {}

# ---------------------------------------------------------------- names

MOD_LANG = read_json(f"assets/{MOD}/lang/en_us.json") or {}
VAN_LANG = read_json("assets/minecraft/lang/en_us.json") or {}


def name_of(item_id):
    ns, p = split_id(item_id)
    lang = MOD_LANG if ns == MOD else VAN_LANG
    for kind in ("item", "block"):
        n = lang.get(f"{kind}.{ns}.{p}")
        if n:
            return n
    return p.replace("_", " ").title()


# ---------------------------------------------------------------- JSON recipes

RECIPES = []      # each: {station, kind, ...}
REMOVED = {}      # vanilla item id -> [recipe file names]
OVERRIDDEN = set()  # vanilla item ids whose vanilla recipe was replaced by a different one

STATIONS = {
    "crafting_shaped": "Crafting", "crafting_shapeless": "Crafting", "campfire_cooking": "Campfire",
    "smoking": "Smoker", "smelting": "Furnace", "blasting": "Blast Furnace", "stonecutting": "Stonecutter",
}


def ingredient(v, label=None, count=1):
    """JSON ingredient (string, '#tag', list or object) -> {opts, tag, count, label}."""
    tag = None
    opts = []
    vals = v if isinstance(v, list) else [v]
    for x in vals:
        if isinstance(x, dict):
            x = ("#" + x["tag"]) if "tag" in x else x.get("item") or x.get("id")
        if not isinstance(x, str):
            continue
        if x.startswith("#"):
            tag = tag or "#" + full_id(x)
            opts += tag_members(x)
        else:
            opts.append(full_id(x))
    if not opts:
        return None
    return {"opts": list(dict.fromkeys(opts)), "tag": tag, "count": count, "label": label}


def result_of(r):
    res = r.get("result")
    if isinstance(res, str):
        return {"id": full_id(res), "count": 1}
    if isinstance(res, dict) and (res.get("id") or res.get("item")):
        return {"id": full_id(res.get("id") or res.get("item")), "count": res.get("count", 1)}
    return None


def parse_json_recipe(r, src):
    rtype = split_id(r.get("type", ""))[1]
    result = result_of(r)
    if result is None:
        warn(f"{src}: inget resultat, hoppar över")
        return None
    rec = {"station": STATIONS.get(rtype, rtype.replace("_", " ").title()), "result": result, "src": src}
    if "pattern" in r and "key" in r:
        key = {k: ingredient(v) for k, v in r["key"].items()}
        rows = [list(row.ljust(3))[:3] for row in r["pattern"]][:3]
        while len(rows) < 3:
            rows.append([" "] * 3)
        rec["kind"] = "grid"
        rec["grid"] = [[key.get(c) if c != " " else None for c in row] for row in rows]
    elif "ingredients" in r:
        rec["kind"] = "list"
        rec["ings"] = [i for i in (ingredient(x) for x in r["ingredients"]) if i]
    elif "ingredient" in r:
        rec["kind"] = "cook"
        rec["ings"] = [i for i in [ingredient(r["ingredient"])] if i]
        if "cookingtime" in r:
            rec["time"] = r["cookingtime"] / 20
    else:
        warn(f"{src}: okänt receptformat ({rtype})")
        return None
    return rec


def jar_recipe_result(name):
    path = f"data/minecraft/recipe/{name}.json"
    if path in _jar_names:
        try:
            res = result_of(json.loads(_jar.read(path)))
            if res:
                return res["id"]
        except Exception:
            pass
    return None


def load_json_recipes():
    for ns in sorted(os.listdir(os.path.join(RES, "data"))):
        for f in sorted(glob.glob(os.path.join(RES, "data", ns, "recipe", "**", "*.json"), recursive=True)):
            name = os.path.splitext(os.path.relpath(f, os.path.join(RES, "data", ns, "recipe")))[0].replace("\\", "/")
            src = f"{ns}:{name}"
            try:
                with open(f, encoding="utf-8") as fh:
                    r = json.load(fh)
            except Exception as e:
                warn(f"trasigt recept {src}: {e}")
                continue
            conds = r.get("neoforge:conditions", [])
            if any(isinstance(c, dict) and c.get("type") == "neoforge:never" for c in conds):
                item = jar_recipe_result(name) or f"minecraft:{name}"
                REMOVED.setdefault(item, []).append(src)
                continue
            rec = parse_json_recipe(r, src)
            if rec:
                RECIPES.append(rec)
                if ns == "minecraft" and rec["result"]["id"].startswith("minecraft:"):
                    OVERRIDDEN.add(rec["result"]["id"])


# ---------------------------------------------------------------- Java recipes (regex)

def strip_comments(src):
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    return re.sub(r"(?m)//[^\n\"]*$", "", src)


def java_constants():
    """CONSTANT_NAME -> item id, and CONSTANT_NAME -> '#tag', from every Java file."""
    items, tags = {}, {}
    for f in glob.glob(os.path.join(JAVA, "**", "*.java"), recursive=True):
        try:
            src = strip_comments(open(f, encoding="utf-8").read())
        except Exception as e:
            warn(f"kan inte läsa {f}: {e}")
            continue
        for name, rid in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*=\s*[\w.]*\(\s*\"([a-z0-9_]+)\"", src):
            items.setdefault(name, []).append(f"{MOD}:{rid}")
        for name, rid in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*=\s*TagKey\.create\(\s*Registries\.ITEM\s*,\s*Identifier\.fromNamespaceAndPath\(\s*(?:\w+\.)?MODID\s*,\s*\"([a-z0-9_/]+)\"", src):
            tags[name] = f"#{MOD}:{rid}"
        for name, rid in re.findall(r"\b([A-Z][A-Z0-9_]*)\s*=\s*TagKey\.create\(\s*Registries\.ITEM\s*,\s*Identifier\.(?:withDefaultNamespace|parse)\(\s*\"([a-z0-9_:/]+)\"", src):
            tags[name] = "#" + full_id(rid)
    return items, tags


def balanced(src, start):
    """Text between the '(' at src[start] and its matching ')'."""
    depth, i = 0, start
    while i < len(src):
        c = src[i]
        if c == '"':  # skip string literals
            j = src.find('"', i + 1)
            i = (j if j > 0 else i) + 1
            continue
        if c in "([{":
            depth += 1
        elif c in ")]}":
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
        i += 1
    return None


def split_top(s, angle=False):
    """Split on top-level commas; angle=True also nests <generics> (not for code, where '->' has a '>')."""
    opn, cls = ("([{<", ")]}>") if angle else ("([{", ")]}")
    parts, depth, cur = [], 0, ""
    for c in s:
        if c in opn:
            depth += 1
        elif c in cls:
            depth -= 1
        if c == "," and depth == 0:
            parts.append(cur.strip())
            cur = ""
        else:
            cur += c
    if cur.strip():
        parts.append(cur.strip())
    return parts


REF_RE = re.compile(r"(?:\b([A-Za-z_]\w*)\.)?\b([A-Z][A-Z0-9_]{2,})\b(?!\s*\()")


def java_refs(arg, consts, known):
    """Item ids / '#tags' named in a Java expression (Items.X, ItemTags.X, Mechanical.X, Wildlife.X.get() ...)."""
    items, tags = consts
    out = []
    for cls, name in REF_RE.findall(arg):
        if cls == "Items" or cls == "Blocks":
            out.append("minecraft:" + name.lower())
        elif cls == "ItemTags":
            out.append("#minecraft:" + name.lower())
        elif name in tags:
            out.append(tags[name])
        elif name in items:
            cands = items[name]
            out.append(next((c for c in cands if c in known), cands[0]))
    return list(dict.fromkeys(out))


def ing_from_refs(refs, label=None, count=1):
    opts, tag = [], None
    for r in refs:
        if r.startswith("#"):
            tag = tag or r
            opts += tag_members(r)
        else:
            opts.append(r)
    if not opts:
        return None
    return {"opts": list(dict.fromkeys(opts)), "tag": tag, "count": count, "label": label, "refs": refs}


RESULT_NAMES = ("result", "output", "out", "product")
UNLOCK_NAMES = ("material", "unlock", "unlocks", "requirement", "unlockedby")
NUM_TYPES = ("int", "long", "float", "double", "short")
LABELS = {"head": "Huvud", "binding": "Bindning", "rod": "Skaft", "input": "In", "shaft": "Skaft",
          "tip": "Spets", "feather": "Fjäder", "fletching": "Fjäder", "item": "Bränsle",
          "first": "", "second": "", "third": ""}


def parse_java_recipes(path, station, consts, known):
    """Generic: reads each `record X(...)` header, then every `new X(...)`, mapping args by parameter name."""
    try:
        src = strip_comments(open(path, encoding="utf-8").read())
    except Exception as e:
        warn(f"kan inte läsa {path}: {e}")
        return [], []
    recipes, fuels = [], []
    for m in re.finditer(r"\brecord\s+(\w+)\s*\(", src):
        rec_name = m.group(1)
        header = balanced(src, m.end() - 1)
        if header is None:
            continue
        params = []
        for p in split_top(header, angle=True):
            bits = re.sub(r"@\w+\s*", "", p).split()
            if len(bits) >= 2:
                params.append((" ".join(bits[:-1]), bits[-1]))
        for n in re.finditer(r"\bnew\s+" + re.escape(rec_name) + r"\s*\(", src):
            body = balanced(src, n.end() - 1)
            if body is None:
                continue
            args = split_top(body)
            if len(args) != len(params):
                warn(f"{os.path.basename(path)}: new {rec_name}(...) har {len(args)} argument, väntade {len(params)}")
                continue
            try:
                result, ings, nums, last = None, [], {}, None
                lname = [pn.lower() for _, pn in params]
                res_idx = next((i for i, pn in enumerate(lname) if pn in RESULT_NAMES), None)
                for i, ((ptype, pname), arg) in enumerate(zip(params, args)):
                    low = pname.lower()
                    if ptype.split("<")[0] in NUM_TYPES:
                        num = re.search(r"-?\d+(?:\.\d+)?", arg)
                        if num:
                            nums[low] = float(num.group())
                            # a count belongs to the parameter just before it (result count or input count)
                            if low in ("count", "amount") and last is not None:
                                last["count"] = int(nums[low])
                        continue
                    if low in UNLOCK_NAMES:
                        continue
                    refs = java_refs(arg, consts, known)
                    cnt = re.search(r"new\s+ItemStack\([^,]+,\s*(\d+)\s*\)", arg)
                    cnt = int(cnt.group(1)) if cnt else 1
                    if i == res_idx:
                        if refs:
                            result = last = {"id": refs[0], "count": cnt}
                        continue
                    if not refs:
                        continue
                    label = LABELS.get(low, pname)
                    if "List.of" in arg or "new " in arg and "[]" in arg:
                        for r in refs:
                            ing = ing_from_refs([r], label)
                            if ing:
                                ings.append(ing)
                    else:
                        ing = ing_from_refs(refs, label, cnt)
                        if ing:
                            ings.append(ing)
                            last = ing
                if "resultcount" in nums or "outputcount" in nums:
                    if result:
                        result["count"] = int(nums.get("resultcount") or nums.get("outputcount"))
                ticks = next((nums[k] for k in nums if k in ("ticks", "time", "cooktime", "burntime", "duration")), None)
                if result is None and "fuel" in rec_name.lower():
                    if ings:
                        fuels.append({"ing": ings[0], "time": ticks / 20 if ticks else None})
                    continue
                if result is None:
                    if res_idx is not None:
                        warn(f"{os.path.basename(path)}: okänt resultat i new {rec_name}({args[res_idx][:60]})")
                    continue
                rec = {"station": station, "result": result, "src": os.path.basename(path),
                       "kind": "slots" if station in ("Tool Station", "Fletching Table") else "cook", "ings": ings}
                if ticks:
                    rec["time"] = ticks / 20
                recipes.append(rec)
            except Exception as e:
                warn(f"{os.path.basename(path)}: kunde inte tolka new {rec_name}(...): {e}")
    return recipes, fuels


# ---------------------------------------------------------------- categories

CATS = [
    ("tools", "Verktyg"), ("materials", "Material"), ("food", "Mat"), ("blocks", "Block"),
    ("wildlife", "Från djur"), ("foraging", "Plocka"), ("changed", "Ändrat i vanilla"),
    ("removed", "Borttaget från vanilla"), ("vanilla", "Vanilla i recept"),
]
TOOL_TAGS = ("axes", "pickaxes", "shovels", "hoes", "swords", "spears")


def guess_category(item_id, is_block, tool_ids):
    p = split_id(item_id)[1]
    if re.search(r"scraps$|^cooked_|^roasted_|burnt_food", p):
        return "food"
    if re.search(r"berr|bush|chanterelle|porcini|morel|mushroom|agaric|puffball", p):
        return "foraging"
    if is_block:
        return "blocks"
    if item_id in tool_ids or re.search(r"pickaxe$|_axe$|shovel$|_hoe$|knife$|spear$|bow$|fishing_rod$|hook_rod$|fire_drill$|basket$|arrow$", p):
        return "tools"
    if re.search(r"hide|pelt|sinew|antler|lard|tusk|horn|bear_|wool_thread|^whole_|fish_bones|feather|claw|_fat$", p):
        return "wildlife"
    return "materials"


# ---------------------------------------------------------------- main

def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    try:
        with open(NOTES, encoding="utf-8") as f:
            notes_raw = json.load(f)
    except FileNotFoundError:
        notes_raw = {}
        warn("docs/registry_notes.json saknas")
    except Exception as e:
        notes_raw = {}
        warn(f"docs/registry_notes.json är trasig: {e}")
    notes = {}
    for k, v in notes_raw.items():
        if k.startswith("_"):
            continue
        notes[k if ":" in k else f"{MOD}:{k}"] = {"text": v} if isinstance(v, str) else v

    # Mod items and blocks from the lang file (an item and a block with the same id are one entry).
    ids, eggs = {}, 0
    for key in MOD_LANG:
        m = re.match(r"(item|block)\.mechanical\.([a-z0-9_]+)$", key)
        if not m:
            continue
        if m.group(2).endswith("_spawn_egg"):
            eggs += 1
            continue
        rid = f"{MOD}:{m.group(2)}"
        ids.setdefault(rid, set()).add(m.group(1))
    known = set(ids)

    load_json_recipes()
    consts = java_constants()
    fuels = []
    for path, station in [(os.path.join(JAVA, "ToolStationRecipes.java"), "Tool Station"),
                          (os.path.join(JAVA, "KilnBlockEntity.java"), "Clay Kiln")] + \
            [(p, "Fletching Table") for p in sorted(glob.glob(os.path.join(JAVA, "**", "*Fletch*.java"), recursive=True))]:
        if not os.path.isfile(path):
            warn(f"saknas: {os.path.relpath(path, ROOT)}")
            continue
        recs, fl = parse_java_recipes(path, station, consts, known)
        if not recs and not fl and station != "Fletching Table":
            warn(f"inga recept hittades i {os.path.basename(path)}")
        RECIPES.extend(recs)
        fuels.extend(fl)

    tool_ids = set()
    for t in TOOL_TAGS:
        tool_ids.update(tag_members(f"#minecraft:{t}"))

    # Every id that shows up anywhere gets an entry so recipe slots are always clickable.
    def ing_ids(rec):
        ings = rec.get("ings", []) + [c for row in rec.get("grid", []) for c in row if c]
        return ings

    referenced = set()
    for rec in RECIPES:
        referenced.add(rec["result"]["id"])
        for ing in ing_ids(rec):
            referenced.update(ing["opts"])
    for f in fuels:
        referenced.update(f["ing"]["opts"])

    entries = {}

    def add(rid, cats):
        e = entries.get(rid)
        if e is None:
            e = entries[rid] = {"id": rid, "name": name_of(rid), "cats": []}
        for c in cats:
            if c not in e["cats"]:
                e["cats"].append(c)
        return e

    for rid in ids:
        add(rid, [guess_category(rid, "block" in ids[rid], tool_ids)])
    for rid, n in notes.items():
        if n.get("vanilla") or not rid.startswith(MOD + ":"):
            add(rid, ["changed"])
    for rid in OVERRIDDEN:
        add(rid, ["changed"])
    for rid in REMOVED:
        add(rid, ["removed"])
    for rid in referenced:
        if rid not in entries:
            add(rid, ["vanilla" if not rid.startswith(MOD + ":") else "materials"])
    for rid, n in notes.items():
        if rid in entries:
            e = entries[rid]
            if n.get("category"):
                cats = n["category"] if isinstance(n["category"], list) else [n["category"]]
                e["cats"] = cats + [c for c in e["cats"] if c in ("changed", "removed")]
                e["cats"] = list(dict.fromkeys(e["cats"]))
            if n.get("name"):
                e["name"] = n["name"]
            if n.get("text"):
                e["note"] = n["text"]
    for rid, srcs in REMOVED.items():
        entries[rid]["removedRecipes"] = srcs

    for f in fuels:
        for rid in f["ing"]["opts"]:
            if rid in entries and f["time"]:
                entries[rid].setdefault("fuel", f["time"])

    for e in entries.values():
        e["img"] = picture(e["id"])
    for rec in RECIPES:  # pictures for tag members shown in slots are only needed for the first option
        for ing in ing_ids(rec):
            picture(ing["opts"][0])

    data = {
        "items": sorted(entries.values(), key=lambda e: e["name"].lower()),
        "recipes": RECIPES,
        "fuels": fuels,
        "cats": CATS,
        "images": IMAGES,
    }
    html = TEMPLATE.replace("/*DATA*/null", json.dumps(data, ensure_ascii=False, separators=(",", ":")).replace("</", "<\\/"))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        f.write(html)

    mod_count = sum(1 for e in entries.values() if e["id"].startswith(MOD + ":"))
    by_station = {}
    for r in RECIPES:
        by_station[r["station"]] = by_station.get(r["station"], 0) + 1
    print(f"Skrev {os.path.relpath(OUT, ROOT)} ({os.path.getsize(OUT) / 1024:.0f} KB)")
    print(f"  {mod_count} mod-items/block (+{eggs} spawn eggs överhoppade), {len(entries) - mod_count} vanilla-poster")
    print(f"  {len(RECIPES)} recept: " + ", ".join(f"{k} {v}" for k, v in sorted(by_station.items())))
    print(f"  {len(fuels)} bränslen, {len(REMOVED)} borttagna vanilla-items, {len(IMAGES)} bilder")
    missing = sorted(rid for rid in ids if rid not in notes)
    if missing:
        print(f"  {len(missing)} mod-items utan anteckning: " + ", ".join(m.split(':')[1] for m in missing))
    if WARN:
        print(f"  {len(WARN)} varningar:")
        for w in WARN:
            print("   - " + w)


TEMPLATE = r"""<!doctype html>
<html lang="sv">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Mechanical register</title>
<style>
:root{--bg:#f6f5f2;--panel:#fff;--ink:#1d1d1f;--mute:#6b6b70;--line:#e3e1dc;--slot:#ecebe7;--accent:#2f7d4f;--chip:#fff;--shadow:0 1px 2px rgba(0,0,0,.06),0 4px 16px rgba(0,0,0,.05)}
@media (prefers-color-scheme:dark){:root{--bg:#141517;--panel:#1d1f22;--ink:#ececee;--mute:#9a9ca3;--line:#2c2f33;--slot:#26292d;--accent:#5fbf86;--chip:#1d1f22;--shadow:0 1px 2px rgba(0,0,0,.4)}}
*{box-sizing:border-box}
html,body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
img{image-rendering:pixelated;image-rendering:crisp-edges}
a{color:inherit}
header{position:sticky;top:0;z-index:5;background:color-mix(in srgb,var(--bg) 92%,transparent);backdrop-filter:blur(8px);border-bottom:1px solid var(--line);padding:14px 16px 10px}
.top{display:flex;gap:12px;align-items:center;max-width:1400px;margin:0 auto}
h1{font-size:18px;margin:0;white-space:nowrap;letter-spacing:-.01em}
h1 span{color:var(--mute);font-weight:400}
#q{flex:1;min-width:0;font:inherit;padding:9px 12px;border-radius:10px;border:1px solid var(--line);background:var(--panel);color:var(--ink);outline:none}
#q:focus{border-color:var(--accent)}
.chips{display:flex;gap:6px;overflow-x:auto;max-width:1400px;margin:10px auto 0;padding-bottom:2px;scrollbar-width:none}
.chip{flex:none;border:1px solid var(--line);background:var(--chip);color:var(--ink);border-radius:999px;padding:4px 11px;font:inherit;font-size:13px;cursor:pointer}
.chip[aria-pressed=true]{background:var(--accent);border-color:var(--accent);color:#fff}
.chip small{opacity:.7;margin-left:4px}
main{max-width:1400px;margin:0 auto;padding:16px;display:grid;grid-template-columns:1fr;gap:16px}
@media (min-width:980px){main.open{grid-template-columns:1fr 440px}}
#grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(116px,1fr));gap:10px;align-content:start}
.card{display:flex;flex-direction:column;align-items:center;gap:6px;text-decoration:none;background:var(--panel);border:1px solid var(--line);border-radius:12px;padding:12px 8px 10px;text-align:center;font-size:13px;line-height:1.25;box-shadow:var(--shadow);transition:transform .08s}
.card:hover{transform:translateY(-1px);border-color:var(--accent)}
.card.sel{outline:2px solid var(--accent)}
.card img,.ph{width:48px;height:48px}
.ph{display:grid;place-items:center;border-radius:8px;background:var(--slot);color:var(--mute);font-weight:600}
.empty{color:var(--mute);padding:40px 0;text-align:center;grid-column:1/-1}
#detail{display:none;background:var(--panel);border:1px solid var(--line);border-radius:14px;padding:18px;box-shadow:var(--shadow);align-self:start;position:sticky;top:120px;max-height:calc(100vh - 136px);overflow:auto}
main.open #detail{display:block}
@media (max-width:979px){#detail{position:fixed;inset:0;top:0;max-height:none;border-radius:0;z-index:10;border:0}}
.dh{display:flex;gap:14px;align-items:center}
.dh img,.dh .ph{width:80px;height:80px;flex:none;background:var(--slot);border-radius:12px;padding:8px}
.dh h2{margin:0;font-size:20px;line-height:1.2}
.id{font:12px ui-monospace,Consolas,monospace;color:var(--mute);user-select:all}
.tags{display:flex;gap:4px;flex-wrap:wrap;margin-top:4px}
.tag{font-size:11px;padding:1px 7px;border-radius:999px;background:var(--slot);color:var(--mute)}
#close{margin-left:auto;align-self:flex-start;border:0;background:var(--slot);color:var(--ink);border-radius:8px;width:32px;height:32px;font-size:18px;cursor:pointer;flex:none}
h3{font-size:12px;text-transform:uppercase;letter-spacing:.06em;color:var(--mute);margin:20px 0 8px}
.note p{margin:0 0 8px}
.note a{color:var(--accent);text-decoration:none;font-weight:500}
.rec{border:1px solid var(--line);border-radius:12px;padding:12px;margin-bottom:10px}
.st{display:flex;justify-content:space-between;font-size:12px;color:var(--mute);margin-bottom:10px}
.st b{color:var(--ink);font-weight:600}
.row{display:flex;align-items:center;gap:12px;flex-wrap:wrap}
.g3{display:grid;grid-template-columns:repeat(3,44px);gap:3px}
.line{display:flex;gap:6px;flex-wrap:wrap}
.slot{position:relative;display:grid;place-items:center;width:44px;height:44px;background:var(--slot);border-radius:7px;text-decoration:none}
.slot img{width:32px;height:32px}
.slot .ph{width:32px;height:32px;font-size:12px;background:none}
.slot i{position:absolute;right:3px;bottom:0;font-style:normal;font-size:12px;font-weight:700;text-shadow:0 0 3px var(--panel),0 0 3px var(--panel)}
.slot.tagged::after{content:"#";position:absolute;left:4px;top:1px;font-size:10px;color:var(--accent);font-weight:700}
a.slot:hover{outline:2px solid var(--accent)}
.lab{display:flex;flex-direction:column;align-items:center;gap:2px;font-size:11px;color:var(--mute)}
.arrow{font-size:22px;color:var(--mute)}
.out{width:52px;height:52px}
.out img{width:40px;height:40px}
.uses{display:flex;flex-wrap:wrap;gap:6px}
.use{display:flex;align-items:center;gap:6px;padding:4px 10px 4px 4px;border:1px solid var(--line);border-radius:999px;text-decoration:none;font-size:13px}
.use img,.use .ph{width:24px;height:24px;font-size:10px}
.use:hover{border-color:var(--accent)}
.muted{color:var(--mute);font-size:14px}
table{border-collapse:collapse;width:100%;font-size:14px}
td{padding:4px 6px;border-top:1px solid var(--line)}
td a{display:flex;align-items:center;gap:6px;text-decoration:none}
td img{width:24px;height:24px}
</style>
</head>
<body>
<header>
  <div class="top"><h1>Mechanical <span>register</span></h1><input id="q" type="search" placeholder="Sök item eller block…" autocomplete="off" aria-label="Sök"></div>
  <div class="chips" id="chips"></div>
</header>
<main id="main"><section id="grid" aria-live="polite"></section><aside id="detail"></aside></main>
<script>
const D=/*DATA*/null;
const byId=Object.fromEntries(D.items.map(i=>[i.id,i]));
const catName=Object.fromEntries(D.cats);
const esc=s=>String(s).replace(/[&<>"]/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;"}[c]));
const nm=id=>(byId[id]&&byId[id].name)||id.split(":")[1].replace(/_/g," ");
const pic=(id,cls="")=>{const k=(byId[id]&&byId[id].img)||(D.images[id]?id:null);return k?`<img src="${D.images[k]}" alt="" class="${cls}" loading="lazy">`:`<span class="ph ${cls}">${esc(nm(id)[0]||"?")}</span>`};
const allIngs=r=>(r.ings||[]).concat((r.grid||[]).flat().filter(Boolean));
const makes={},uses={};
D.recipes.forEach((r,i)=>{(makes[r.result.id]??=[]).push(i);new Set(allIngs(r).flatMap(g=>g.opts)).forEach(id=>(uses[id]??=[]).push(i))});
let cat="all",sel=null;
const chips=document.getElementById("chips"),grid=document.getElementById("grid"),q=document.getElementById("q"),det=document.getElementById("detail"),main=document.getElementById("main");
const inAll=i=>!i.cats.every(x=>x==="vanilla"||x==="removed");
const count=c=>D.items.filter(i=>c==="all"?inAll(i):i.cats.includes(c)).length;
function drawChips(){chips.innerHTML=[["all","Alla"],...D.cats].filter(([c])=>count(c)).map(([c,n])=>`<button class="chip" data-c="${c}" aria-pressed="${c===cat}">${esc(n)}<small>${count(c)}</small></button>`).join("")}
chips.onclick=e=>{const b=e.target.closest(".chip");if(b){cat=b.dataset.c;drawChips();drawGrid()}};
function drawGrid(){
  const s=q.value.trim().toLowerCase();
  const list=D.items.filter(i=>(cat==="all"?(s||inAll(i)):i.cats.includes(cat))&&(!s||i.name.toLowerCase().includes(s)||i.id.includes(s)));
  grid.innerHTML=list.length?list.map(i=>`<a class="card${i.id===sel?" sel":""}" href="#${i.id}">${pic(i.id)}<span>${esc(i.name)}</span></a>`).join(""):`<p class="empty">Inget hittades.</p>`;
}
q.oninput=drawGrid;
function slot(g,cls=""){
  if(!g)return `<span class="slot ${cls}"></span>`;
  const id=g.opts[0],alt=g.opts.length>1?" (eller "+g.opts.slice(1,6).map(nm).join(", ")+(g.opts.length>6?" …":"")+")":"";
  return `<a class="slot ${cls}${g.tag?" tagged":""}" href="#${id}" title="${esc((g.tag?g.tag+": ":"")+nm(id)+alt)}">${pic(id)}${g.count>1?`<i>${g.count}</i>`:""}</a>`;
}
const out=r=>`<span class="arrow">→</span>${slot({opts:[r.result.id],count:r.result.count},"out")}`;
function recipe(r){
  let body;
  if(r.kind==="grid")body=`<div class="g3">${r.grid.flat().map(g=>slot(g)).join("")}</div>`;
  else if(r.kind==="slots")body=`<div class="line">${r.ings.map(g=>`<span class="lab">${slot(g)}${esc(g.label||"")}</span>`).join("")}</div>`;
  else if(r.kind==="cook")body=`<div class="line">${r.ings.map(g=>slot(g)).join("")}</div><svg class="fire" viewBox="0 0 16 16" width="22" height="22" aria-label="värme"><path d="M8 1c1 3 4 4.5 4 8.5A4 4 0 0 1 4 9.5C4 7 5.5 6 6 4c.8 1.2 1 2 1 3 1-1.5 1.3-3.5 1-6z" fill="#e8772e"/></svg>`;
  else body=`<div class="line">${r.ings.map(g=>slot(g)).join("")}</div>`;
  const t=r.time?`${r.time>=60?(r.time/60).toFixed(r.time%60?1:0)+" min":r.time+" s"}`:"";
  return `<div class="rec"><div class="st"><b>${esc(r.station)}</b><span>${[r.kind==="list"?"formlöst":"",t].filter(Boolean).join(" · ")}</span></div><div class="row">${body}${out(r)}</div></div>`;
}
function noteHtml(t){
  return t.split(/\n\s*\n|\n/).map(p=>"<p>"+esc(p).replace(/\[\[([a-z0-9_:]+)\]\]/g,(m,id)=>{id=id.includes(":")?id:"mechanical:"+id;return `<a href="#${id}">${esc(nm(id))}</a>`}).replace(/\*\*(.+?)\*\*/g,"<b>$1</b>")+"</p>").join("");
}
const fmtT=s=>s>=60?(s/60).toFixed(s%60?1:0)+" min":s+" s";
function show(id){
  sel=id;const i=byId[id];
  if(!i){det.innerHTML="";main.classList.remove("open");drawGrid();return}
  const mk=makes[id]||[],us=[...new Set(uses[id]||[])];
  let h=`<div class="dh">${pic(id)}<div><h2>${esc(i.name)}</h2><div class="id">${esc(id)}</div><div class="tags">${i.cats.map(c=>`<span class="tag">${esc(catName[c]||c)}</span>`).join("")}</div></div><button id="close" aria-label="Stäng">×</button></div>`;
  if(i.note)h+=`<h3>Beskrivning</h3><div class="note">${noteHtml(i.note)}</div>`;
  if(i.removedRecipes)h+=`<h3>Borttaget från vanilla</h3><p class="muted">Vanilla-receptet är borttaget (${i.removedRecipes.map(esc).join(", ")}).</p>`;
  if(i.fuel)h+=`<h3>Bränsle</h3><p class="muted">Brinner i ${fmtT(i.fuel)} i en <a href="#mechanical:kiln">Clay Kiln</a>.</p>`;
  if(id==="mechanical:kiln"&&D.fuels.length)h+=`<h3>Bränsle i Clay Kiln</h3><table>${D.fuels.map(f=>`<tr><td><a href="#${f.ing.opts[0]}">${pic(f.ing.opts[0])}${esc((f.ing.refs||[f.ing.opts[0]]).map(r=>r[0]==="#"?r:nm(r)).join(", "))}</a></td><td>${f.time?fmtT(f.time):""}</td></tr>`).join("")}</table>`;
  h+=`<h3>Så tillverkas det</h3>`+(mk.length?mk.map(n=>recipe(D.recipes[n])).join(""):`<p class="muted">Inget recept.</p>`);
  if(us.length){
    const res=[...new Set(us.map(n=>D.recipes[n].result.id))];
    h+=`<h3>Används i</h3><div class="uses">${res.map(r=>`<a class="use" href="#${r}">${pic(r)}${esc(nm(r))}</a>`).join("")}</div>`;
    h+=`<details><summary class="muted" style="cursor:pointer;margin-top:10px">Visa recepten (${us.length})</summary><div style="margin-top:10px">${us.map(n=>recipe(D.recipes[n])).join("")}</div></details>`;
  }
  det.innerHTML=h;det.scrollTop=0;main.classList.add("open");
  document.getElementById("close").onclick=()=>{history.pushState("",document.title,location.pathname+location.search);show(null)};
  drawGrid();
}
const route=()=>show(decodeURIComponent(location.hash.slice(1))||null);
addEventListener("hashchange",route);
addEventListener("keydown",e=>{if(e.key==="Escape"&&sel)document.getElementById("close").click();if(e.key==="/"&&document.activeElement!==q){e.preventDefault();q.focus()}});
drawChips();route();if(!sel)drawGrid();
</script>
</body>
</html>
"""

if __name__ == "__main__":
    try:
        main()
    except Exception as e:  # last resort: never leave the user with a traceback and no output
        import traceback
        traceback.print_exc()
        print(f"FEL: {e}")
        sys.exit(1)
