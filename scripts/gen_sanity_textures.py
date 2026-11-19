"""Textures for fear of the dark (Sanity.java, client/SanityClient.java):
- textures/misc/sanity_vignette.png (+ .mcmeta blur): drawn with the vanilla VIGNETTE pipeline, which darkens the screen
  by the texture's brightness. Black middle, white edges, with ragged dark fingers creeping in from the rim.
- gui/sprites/hud/sanity/eye_{uneasy,afraid,mad,broken}.png (16x16): the eye next to the hotbar, worse at each stage.
- gui/sprites/hud/sanity/shadow_{0,1,2}.png (32x64): soft black figures seen from the corner of the eye
  (standing, hunched with long arms, thin and tall). Drawn at 8x and scaled down so the edges are soft.
Run: python scripts/gen_sanity_textures.py"""
import math
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mechanical/textures"
SPRITES = ROOT / "gui/sprites/hud/sanity"
SPRITES.mkdir(parents=True, exist_ok=True)
(ROOT / "misc").mkdir(parents=True, exist_ok=True)
rng = random.Random(1337)


# ---- vignette ----
def vignette(size=256):
    # smooth periodic angular noise: a few sines with random phases -> fingers of darkness of different length
    waves = [(k, rng.uniform(0, math.tau), rng.uniform(0.3, 1.0) / k ** 0.6) for k in (3, 5, 7, 11, 17, 23)]
    norm = sum(a for _, _, a in waves)
    img = Image.new("L", (size, size))
    px = img.load()
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            dx, dy = (x - c) / c, (y - c) / c
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            n = sum(a * math.sin(k * ang + p) for k, p, a in waves) / norm  # -1..1
            inner = 0.58 - 0.32 * max(0.0, n)  # fingers reach further in where the noise is high
            t = min(1.0, max(0.0, (r - inner) / (1.25 - inner)))
            v = t * t * (3 - 2 * t)
            px[x, y] = int(255 * v)
    img = img.filter(ImageFilter.GaussianBlur(3))
    img.convert("RGBA").save(ROOT / "misc/sanity_vignette.png")
    (ROOT / "misc/sanity_vignette.png.mcmeta").write_text('{\n  "texture": {\n    "blur": true\n  }\n}\n')


# ---- eyes (16x16) ----
WHITE, SHADE, OUTLINE = (226, 220, 206, 255), (168, 160, 148, 255), (28, 22, 24, 255)
IRIS = {"uneasy": (96, 120, 104, 255), "afraid": (96, 120, 104, 255), "mad": (150, 60, 40, 255), "broken": (20, 14, 18, 255)}
VEIN = (176, 38, 34, 255)


def eye(stage):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    open_h = {"uneasy": 4, "afraid": 5, "mad": 5, "broken": 5}[stage]  # half height of the lids
    top, bot = 8 - open_h, 8 + open_h
    # almond: two arcs, outline then white
    d.chord((1, top, 14, bot + 0), 180, 360, fill=OUTLINE)
    d.chord((1, top - 1, 14, bot), 0, 180, fill=OUTLINE)
    d.ellipse((2, top + 1, 13, bot - 1), fill=WHITE)
    # lower white shaded
    for x in range(2, 14):
        for y in range(9, bot):
            if img.getpixel((x, y)) == WHITE:
                img.putpixel((x, y), SHADE)
    if stage in ("afraid", "mad"):  # bloodshot veins from the corners
        for (x, y) in [(3, 7), (4, 8), (3, 9), (12, 7), (11, 8), (12, 9)] + ([(4, 6), (11, 10), (5, 10), (10, 6)] if stage == "mad" else []):
            if img.getpixel((x, y))[3]:
                img.putpixel((x, y), VEIN)
    if stage == "broken":  # the iris swallows the eye: a black hole with a red rim
        d.ellipse((4, 5, 11, 12), fill=(110, 20, 20, 255))
        d.ellipse((5, 6, 10, 11), fill=IRIS["broken"])
    else:
        r = {"uneasy": 3, "afraid": 3, "mad": 2}[stage]
        d.ellipse((8 - r, 8 - r, 7 + r, 7 + r), fill=IRIS[stage])
        pupil = {"uneasy": 1, "afraid": 0, "mad": 0}[stage]  # fear shrinks the pupil to a pinprick
        d.rectangle((8 - pupil - 1, 8 - pupil - 1, 7 + pupil, 7 + pupil) if pupil else (7, 7, 7, 7), fill=OUTLINE)
        img.putpixel((9, 6), (255, 255, 255, 255))  # glint
    # lid lines over the outline
    d.arc((1, top, 14, bot), 180, 360, fill=OUTLINE)
    img.save(SPRITES / f"eye_{stage}.png")


# ---- shadow figures (32x64, drawn at 8x) ----
S = 8
DARK = (8, 6, 10)


def figure(kind):
    W, H = 32 * S, 64 * S
    mask = Image.new("L", (W, H), 0)
    d = ImageDraw.Draw(mask)

    def e(cx, cy, rx, ry, v=255):
        d.ellipse(((cx - rx) * S, (cy - ry) * S, (cx + rx) * S, (cy + ry) * S), fill=v)

    def limb(x0, y0, x1, y1, w0, w1, v=255):
        steps = 24
        for i in range(steps + 1):
            t = i / steps
            e(x0 + (x1 - x0) * t, y0 + (y1 - y0) * t, (w0 + (w1 - w0) * t) / 2, (w0 + (w1 - w0) * t) / 2, v)

    if kind == 0:  # standing, arms hanging
        e(16, 9, 4, 4.6)
        limb(16, 14, 16, 34, 9, 7)  # torso
        limb(11.5, 16, 9.5, 36, 3, 2.2)
        limb(20.5, 16, 22.5, 36, 3, 2.2)
        limb(14, 33, 13, 62, 3.6, 2.6)
        limb(18, 33, 19, 62, 3.6, 2.6)
    elif kind == 1:  # hunched, head forward and low, arms too long
        e(19, 17, 4, 4.2)
        limb(17, 20, 15, 38, 9, 7)
        limb(13, 22, 9, 52, 2.8, 1.6)
        limb(21, 22, 23, 54, 2.8, 1.6)
        limb(13, 37, 11, 62, 3.6, 2.6)
        limb(17, 37, 19, 62, 3.6, 2.6)
    else:  # thin and too tall, head tilted
        e(15, 5, 3.2, 4)
        limb(15.5, 9, 16, 34, 6, 4.5)
        limb(12.5, 11, 11, 40, 2.2, 1.4)
        limb(19.5, 11, 21, 40, 2.2, 1.4)
        limb(15, 33, 14, 63, 2.6, 2)
        limb(17, 33, 18, 63, 2.6, 2)
    # ragged smoky lower half: eat holes so the legs fray into the dark
    for _ in range(90):
        x, y = rng.uniform(4, 28), rng.uniform(40, 64)
        e(x, y, rng.uniform(0.4, 1.2), rng.uniform(0.4, 1.2), 0)
    mask = mask.filter(ImageFilter.GaussianBlur(S * 0.9)).resize((32, 64), Image.LANCZOS)
    # denser in the middle of the body, wispy at the edges
    out = Image.new("RGBA", (32, 64), DARK + (0,))
    px, mp = out.load(), mask.load()
    for y in range(64):
        for x in range(32):
            a = mp[x, y] / 255
            fade = 1.0 if y < 44 else max(0.0, 1 - (y - 44) / 22)
            px[x, y] = DARK + (int(255 * min(1.0, a * 1.15) * fade),)
    out.save(SPRITES / f"shadow_{kind}.png")


vignette()
for st in ("uneasy", "afraid", "mad", "broken"):
    eye(st)
for k in range(3):
    figure(k)
print("sanity textures ok")
