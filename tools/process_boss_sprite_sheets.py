"""Extract the two boss animation grids from the supplied composite artwork.

The source is a reference sheet rather than a regular sprite atlas. This script
records the inspected row bands and per-row frame counts, removes the connected
paper backdrop, and writes both per-animation PNG frames and normalized 8-row
sprite sheets. All resizing uses nearest-neighbour sampling.
"""

from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "Dual Boss Pixel Art Sprite Sheet-2.png"
OUTPUT = ROOT / "assets" / "bosses"
SHEET_COLUMNS = 8

# Source rectangles below follow the visible cell dividers in the inspected
# 1536x1024 sheet; effect-only cells are omitted from character animations.
BOSSES = (
    {
        "slug": "the_time_guardian",
        "label": "The Time Guardian",
        "frame": (48, 62),  # 96x124 world hitbox at the existing 2x render scale
        "rows": (
            ("idle",    [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539), (543, 640), (645, 738)], (22, 180)),
            ("run",     [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539), (543, 640), (645, 738)], (185, 291)),
            ("jump",    [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539), (543, 640), (645, 738)], (296, 416)),
            ("attack",  [(30, 121), (125, 225), (229, 327), (442, 539)], (421, 539)),
            ("hurt",    [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539), (543, 640), (645, 738)], (544, 647)),
            ("death",   [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539), (543, 640), (645, 738)], (653, 750)),
            ("walk",    [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539), (543, 640), (645, 738)], (756, 857)),
            ("special", [(30, 121), (125, 225), (229, 327), (332, 438), (442, 539)], (863, 996)),
        ),
    },
    {
        "slug": "the_void_core",
        "label": "The Void Core",
        "frame": (58, 70),  # 116x140 world hitbox at the existing 2x scale
        "rows": (
            ("idle",    [(802, 963), (969, 1110), (1117, 1242), (1251, 1381), (1392, 1507)], (22, 186)),
            ("run",     [(802, 892), (900, 995), (1004, 1110), (1118, 1242), (1251, 1381), (1392, 1507)], (193, 298)),
            ("jump",    [(802, 892), (900, 995), (1004, 1110), (1118, 1242), (1251, 1381), (1392, 1507)], (304, 426)),
            # Cells 1 and 3 show the boss; the others are detached effects.
            ("attack",  [(802, 892), (1004, 1110)], (432, 550)),
            ("hurt",    [(802, 892), (900, 995), (1004, 1110), (1118, 1242), (1251, 1381), (1392, 1507)], (556, 659)),
            ("death",   [(802, 892), (900, 995), (1004, 1110), (1118, 1242), (1251, 1381), (1392, 1507)], (666, 755)),
            ("walk",    [(802, 892), (900, 995), (1004, 1110), (1118, 1242), (1251, 1381), (1392, 1507)], (761, 859)),
            # The remaining cells are standalone orb, pillar, drone, and blast effects.
            ("special", [(802, 892), (900, 995)], (866, 996)),
        ),
    },
)


def remove_pale_matte(image: Image.Image) -> Image.Image:
    """Remove the pale checkerboard matte, including areas enclosed by art."""
    image = image.convert("RGBA")
    width, height = image.size
    px = image.load()

    def paper(x: int, y: int) -> bool:
        r, g, b, _ = px[x, y]
        return min(r, g, b) >= 185 and max(r, g, b) - min(r, g, b) <= 62

    # The source uses a pale checkerboard inside a printed panel, so matte
    # removal must also cover areas enclosed by the decorative cell borders.
    matte = bytearray(width * height)
    for y in range(height):
        for x in range(width):
            matte[y * width + x] = paper(x, y)
    alpha = image.getchannel("A")
    alpha.putdata([0 if matte[i] else alpha.getpixel((i % width, i // width))
                   for i in range(width * height)])
    image.putalpha(alpha)
    return image


def remove_guardian_clock_ring(image: Image.Image) -> Image.Image:
    """Erase the bright decorative dial baked behind Guardian poses."""
    image = image.convert("RGBA")
    pixels = image.load()
    width, height = image.size
    for y in range(height):
        ny = (y + 0.5) / height
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if not a or r < 165 or g < 65 or b > 145:
                continue
            nx = (x + 0.5) / width
            dx = (nx - 0.5) / 0.39
            dy = (ny - 0.34) / 0.30
            radius = (dx * dx + dy * dy) ** 0.5
            if ny < 0.60 and 0.78 <= radius <= 1.22:
                pixels[x, y] = (r, g, b, 0)
    return image


def normalize_frame(source: Image.Image, size: tuple[int, int], floating: bool,
                    guardian: bool) -> Image.Image:
    frame_w, frame_h = size
    image = remove_pale_matte(source)
    if guardian:
        image = remove_guardian_clock_ring(image)
    alpha = image.getchannel("A")
    bbox = alpha.getbbox()
    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    if bbox is None:
        return canvas

    subject = image.crop(bbox)
    max_w, max_h = frame_w - 2, frame_h - 2
    scale = min(max_w / subject.width, max_h / subject.height)
    target = (max(1, round(subject.width * scale)), max(1, round(subject.height * scale)))
    subject = subject.resize(target, Image.Resampling.NEAREST)
    x = (frame_w - target[0]) // 2
    y = (frame_h - target[1]) // 2 if floating else frame_h - target[1] - 1
    canvas.alpha_composite(subject, (x, y))
    return canvas


def build_boss(source: Image.Image, spec: dict) -> None:
    slug = spec["slug"]
    frame_size = spec["frame"]
    floating = slug == "the_void_core"
    guardian = slug == "the_time_guardian"
    folder = OUTPUT / slug
    folder.mkdir(parents=True, exist_ok=True)

    for animation, *_ in spec["rows"]:
        for stale in folder.glob(f"{animation}_*.png"):
            stale.unlink()

    sheet = Image.new("RGBA", (frame_size[0] * SHEET_COLUMNS,
                                frame_size[1] * len(spec["rows"])), (0, 0, 0, 0))
    details = [
        f"Boss: {spec['label']}",
        f"Source: {SOURCE.name} ({source.width}x{source.height}, RGBA)",
        f"Output frame: {frame_size[0]}x{frame_size[1]} px",
        f"Sprite sheet: {sheet.width}x{sheet.height} px",
        f"Grid: {SHEET_COLUMNS} columns x {len(spec['rows'])} animation rows",
        "Row order: idle, run, jump, attack, hurt, death, walk, special",
        "Frame anchor: center-x; feet for Time Guardian, center for Void Core",
        "Scale: nearest-neighbour; pale checkerboard matte removed to alpha 0",
        "",
        "Animation frame counts:",
    ]

    for row_index, (animation, source_cells, y_range) in enumerate(spec["rows"]):
        y0, y1 = y_range
        row_sheet = Image.new("RGBA", (frame_size[0] * SHEET_COLUMNS, frame_size[1]),
                              (0, 0, 0, 0))
        for i, (left, right) in enumerate(source_cells):
            crop = source.crop((left, y0, right, y1))
            frame = normalize_frame(crop, frame_size, floating, guardian)
            frame.save(folder / f"{animation}_{i + 1}.png", optimize=True)
            row_sheet.alpha_composite(frame, (i * frame_size[0], 0))
        sheet.alpha_composite(row_sheet, (0, row_index * frame_size[1]))
        details.append(
            f"  {row_index + 1}. {animation}: {len(source_cells)} frames; "
            f"source x={source_cells}, y={y0}:{y1}; "
            f"frame={frame_size[0]}x{frame_size[1]} px"
        )

    sheet.save(folder / "sprite_sheet.png", optimize=True)
    (folder / "manifest.txt").write_text("\n".join(details) + "\n", encoding="utf-8")


def main() -> None:
    if not SOURCE.is_file():
        raise SystemExit(f"Missing source sheet: {SOURCE}")
    source = Image.open(SOURCE).convert("RGBA")
    if source.size != (1536, 1024):
        raise SystemExit(f"Unexpected source dimensions {source.size}; inspect and update row map first")
    for spec in BOSSES:
        build_boss(source, spec)
        print(f"Wrote {OUTPUT / spec['slug']}")


if __name__ == "__main__":
    main()
