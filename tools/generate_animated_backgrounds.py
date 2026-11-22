"""Generate real, seamless scenic GIF loops; UI, text and controls are never baked in.

Periodic mesh displacement moves foliage/water by a few pixels. Two tiny seasonal
particles fade before wrapping. A single palette prevents frame-to-frame colour drift.
Run from the repository root. Requires Pillow; this is asset authoring, not a test/build.
"""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
from math import sin, cos, pi, exp
from PIL import Image, ImageDraw
import json

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/res/drawable-nodpi"
OUTPUT = ROOT / "app/src/main/res/raw"
NAMES = ["game_lake_day", "game_forest_river", "game_lake_evening", "game_autumn_valley",
         "game_night_mountains", "game_sea_coast", "game_canyon", "game_flower_meadow",
         "game_lotus_lake", "game_snow_valley", "play_lake_reference", "map_lake_reference",
         "welcome_lake_reference", "launch_lake_reference"]
NAMES += ['journey_altai', 'journey_bamboo', 'journey_sahara', 'journey_moscow', 'journey_aurora', 'journey_mediterranean', 'journey_canyon', 'journey_tuscany', 'journey_tropics', 'journey_alps']
MAP_NAMES = ['map_bamboo', 'map_sahara', 'map_moscow', 'map_aurora', 'map_mediterranean',
             'map_canyon', 'map_tuscany', 'map_tropics', 'map_alps']
NAMES += MAP_NAMES
FRAMES, DURATION, WIDTH = 48, 120, 640


def generate(name):
    path = next(p for p in SOURCE.glob(name + ".*") if p.suffix in (".webp", ".png"))
    original = Image.open(path).convert("RGB")
    w = min(WIDTH, original.width)
    h = round(original.height * w / original.width)
    source = original.resize((w, h), Image.Resampling.LANCZOS)
    palette = source.quantize(colors=255, method=Image.Quantize.MEDIANCUT)
    # Reserve one index for transparent delta pixels, rather than encoding 48 full pictures.
    palette.putpalette(palette.getpalette()[:765] + [255, 0, 255])
    water = not any(word in name for word in ("canyon", "snow_valley", "sahara", "moscow", "tuscany", "alps", "aurora"))
    frames = []
    for index in range(FRAMES):
        phase = index / FRAMES
        t = phase * 2 * pi

        def sample(x, y):
            nx, ny = x / w, y / h
            edge = min(1., x / 18, (w - x) / 18, y / 18, (h - y) / 18)
            edge = max(0., edge)
            side_leaves = exp(-((nx - .05) / .18) ** 2) + exp(-((nx - .98) / .15) ** 2)
            side_leaves *= max(0., min(1., (ny - .48) * 5))
            branch = exp(-((nx - .16) / .30) ** 2 - ((ny - .05) / .15) ** 2)
            foliage = min(1., side_leaves + branch)
            if foliage < .01:
                foliage = 0.
            lake = exp(-((nx - .5) / .39) ** 4) * max(0., min(1., (ny - .49) * 8))
            dx = foliage * w * .0022 * sin(t + ny * 4)
            dy = foliage * h * .0007 * sin(t + nx * 3 + .7)
            if water:
                dx += lake * w * .0015 * sin(t + ny * 36)
                dy += lake * h * .00045 * sin(2 * t + nx * 18 + ny * 5)
            return (max(0., min(w - 1., x + dx * edge)), max(0., min(h - 1., y + dy * edge)))

        mesh = []
        for y in range(0, h, 40):
            for x in range(0, w, 40):
                right, bottom = min(w, x + 40), min(h, y + 40)
                quad = sample(x, y) + sample(x, bottom) + sample(right, bottom) + sample(right, y)
                mesh.append(((x, y, right, bottom), quad))
        frame = source.transform((w, h), Image.Transform.MESH, mesh, Image.Resampling.BILINEAR)
        draw = ImageDraw.Draw(frame, "RGBA")
        for particle in range(2):
            p = (phase + particle * .47) % 1.
            opacity = min(1., p * 9, (1 - p) * 9) * .47
            x = w * (.16 + particle * .65 + .035 * sin(t + particle * pi))
            y = h * (-.03 + 1.06 * p)
            radius = w * (.005 + particle * .0015)
            if any(word in name for word in ("snow", "night", "alps", "aurora")):
                draw.ellipse((x-radius/2, y-radius/2, x+radius/2, y+radius/2), fill=(245, 252, 225, round(180*opacity)))
            else:
                color = (229, 157, 26) if "autumn" in name or "canyon" in name else (137, 185, 48)
                if "flower" in name or "lotus" in name:
                    color = (255, 166, 215)
                angle = t + particle * 1.8
                points = []
                for px, py in ((-1., 0.), (-.12, -.52), (1., 0.), (.12, .52)):
                    points.append((x + radius*(px*cos(angle)-py*sin(angle)), y + radius*(px*sin(angle)+py*cos(angle))))
                draw.polygon(points, fill=(*color, round(255*opacity)))
        indexed=frame.quantize(palette=palette, dither=Image.Dither.NONE)
        frames.append(indexed.point(lambda value: 0 if value==255 else value))
    output = OUTPUT / ("scene_" + name + ".gif")
    frames[0].save(output, save_all=True, append_images=frames[1:], duration=DURATION,
                   loop=0, disposal=1, transparency=255, optimize=True)
    result = {"source": str(path.relative_to(ROOT)), "target": str(output.relative_to(ROOT)),
              "width": w, "height": h, "frames": FRAMES, "durationMs": FRAMES*DURATION,
              "bytes": output.stat().st_size}
    print(f"{name}: {w}x{h}, {FRAMES} frames, {output.stat().st_size//1024} KiB", flush=True)
    return result


if __name__ == "__main__":
    OUTPUT.mkdir(parents=True, exist_ok=True)
    import sys
    selected=MAP_NAMES if "--map-only" in sys.argv else ([name for name in NAMES if name.startswith("journey_")] if "--world-only" in sys.argv else NAMES)
    with ThreadPoolExecutor(max_workers=2) as executor:
        results = list(executor.map(generate, selected))
    if "--world-only" in sys.argv or "--map-only" in sys.argv:
        previous=json.loads((ROOT / "docs/animated-backgrounds.json").read_text(encoding="utf-8"))
        updated={entry["target"]:entry for entry in previous["files"]}
        updated.update({entry["target"]:entry for entry in results})
        results=list(updated.values())
    (ROOT / "docs/animated-backgrounds.json").write_text(json.dumps({
        "method": "Periodic native mesh displacement and fading seasonal particles; generated by tools/generate_animated_backgrounds.py",
        "periodMs": FRAMES*DURATION, "files": results}, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
