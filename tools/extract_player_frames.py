"""Slice the supplied character sheet into per-animation 48x48 frames.

The source sheet is a 1536x1024 RGBA image laid out as eight loosely spaced
rows of ten poses. It is *not* a strict grid: frame pitch drifts, the cyan
energy effects overlap neighbouring frames, and the alpha channel is soft.
This tool therefore:

  1. finds the solid body of each pose by colour (cyan energy and white glare
     are excluded), which gives one stable anchor per frame;
  2. places frame boundaries in the empty space between those anchors;
  3. crops each slot, downscales it with a single global scale derived from the
     idle body height, and pastes it so the lowest body pixel sits on row 48
     and the body's horizontal centre sits on column 24;
  4. hardens only the alpha edge while preserving the source colors.

Run from the repository root:

    python tools/extract_player_frames.py
    python tools/extract_player_frames.py --preview

Outputs:
    assets/players/chrono_runner/<anim>_<n>.png
    assets/players/chrono_runner/manifest.txt
    tools/previews/chrono_runner_sheet.png   (with --preview)
"""

from __future__ import annotations

import argparse
from dataclasses import dataclass
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw

try:
    import scipy.ndimage as ndi
except ImportError as exc:  # pragma: no cover - dev tool only
    raise SystemExit("scipy is required: pip install scipy") from exc


ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SOURCE = ROOT / "Lưới sprite anh hùng kiếm năng lượng pixel-art.png"
OUT_DIR = ROOT / "assets" / "players" / "chrono_runner"
PREVIEW = ROOT / "tools" / "previews" / "chrono_runner_sheet.png"

FRAME = 48
TARGET_BODY_HEIGHT = 44.0

# The eight rows, top to bottom, in the order the sheet presents them.
ROWS = [
    ("idle", 18, 142, "body"),
    ("walk", 174, 290, "body"),
    ("run", 311, 432, "body"),
    ("jump", 449, 587, "body"),
    ("attack", 602, 712, "body"),
    ("special", 739, 848, "alpha"),
    ("hurt", 865, 930, "body"),
    ("death", 949, 1008, "alpha"),
]

ALPHA_LEVELS = (36, 100, 180)  # below / between / above -> 0 / 96 / 176 / 255


@dataclass
class Box:
    x0: int
    x1: int
    y0: int
    y1: int
    area: int

    @property
    def cx(self) -> float:
        return (self.x0 + self.x1) / 2.0


@dataclass
class Piece:
    anim: str
    index: int
    slot: tuple[int, int, int, int]   # x0, x1, y0, y1 in source pixels
    body: Box                         # solid-body anchor inside the slot
    resized: Image.Image
    dest: tuple[int, int]
    body_h: int


def load_sheet(path: Path) -> np.ndarray:
    if not path.is_file():
        raise SystemExit(f"source sheet not found: {path}")
    return np.array(Image.open(path).convert("RGBA")).astype(np.int16)


def solid_body_mask(sheet: np.ndarray) -> np.ndarray:
    """Pixels that belong to the character's body, ignoring energy/glare."""
    rgb = sheet[:, :, :3]
    alpha = sheet[:, :, 3]
    r, g, b = rgb[:, :, 0], rgb[:, :, 1], rgb[:, :, 2]
    cyan = (b > 140) & (g > 120) & (b > r + 40)
    white = (r > 170) & (g > 170) & (b > 170)
    return (alpha > 150) & ~cyan & ~white


def alpha_mask(sheet: np.ndarray) -> np.ndarray:
    return sheet[:, :, 3] > 60


def components(mask: np.ndarray, min_area: int) -> list[Box]:
    closed = ndi.binary_closing(mask, np.ones((3, 3)))
    labels, _ = ndi.label(closed)
    out: list[Box] = []
    for index, sl in enumerate(ndi.find_objects(labels)):
        if sl is None:
            continue
        area = int((labels[sl] == index + 1).sum())
        if area < min_area:
            continue
        out.append(Box(sl[1].start, sl[1].stop - 1,
                       sl[0].start, sl[0].stop - 1, area))
    out.sort(key=lambda box: box.x0)
    return out


def merge_overlapping(boxes: list[Box], max_gap: int = 12) -> list[Box]:
    merged: list[Box] = []
    for box in boxes:
        if merged:
            prev = merged[-1]
            overlaps_y = box.y0 <= prev.y1 and prev.y0 <= box.y1
            if overlaps_y and box.x0 - prev.x1 <= max_gap:
                merged[-1] = Box(prev.x0, max(prev.x1, box.x1),
                                 min(prev.y0, box.y0), max(prev.y1, box.y1),
                                 prev.area + box.area)
                continue
        merged.append(box)
    return merged


def frame_boxes(sheet: np.ndarray, y0: int, y1: int, mode: str) -> list[Box]:
    if mode == "body":
        boxes = components(solid_body_mask(sheet)[y0:y1], 600)
        if len(boxes) == 10:
            return [Box(b.x0, b.x1, b.y0 + y0, b.y1 + y0, b.area) for b in boxes]
    boxes = components(alpha_mask(sheet)[y0:y1], 250)
    boxes = merge_overlapping(boxes)
    return [Box(b.x0, b.x1, b.y0 + y0, b.y1 + y0, b.area) for b in boxes]


def slots_from_boxes(boxes: list[Box], width: int, margin: int = 24) -> list[tuple[int, int]]:
    bounds = [max(0, boxes[0].x0 - margin)]
    for prev, cur in zip(boxes, boxes[1:]):
        bounds.append(int(round((prev.x1 + cur.x0) / 2.0)))
    bounds.append(min(width, boxes[-1].x1 + margin))
    bounds[0] = max(0, bounds[0])
    return list(zip(bounds, bounds[1:]))


def body_anchor(body_mask: np.ndarray, slot: tuple[int, int, int, int]) -> Box:
    xa, xb, ya, yb = slot
    ys, xs = np.where(body_mask[ya:yb, xa:xb])
    if len(ys) == 0:
        ys, xs = np.where(alpha_mask_global[ya:yb, xa:xb])
    if len(ys) == 0:
        return Box(xa, xb, ya, yb, 0)
    return Box(xs.min() + xa, xs.max() + xa, ys.min() + ya, ys.max() + ya, len(ys))


def build_pieces(sheet: np.ndarray) -> list[Piece]:
    global alpha_mask_global
    alpha_mask_global = alpha_mask(sheet)
    body_mask = solid_body_mask(sheet)
    height, width = sheet.shape[:2]

    idle_heights = []
    for anim, row0, row1, mode in ROWS:
        boxes = frame_boxes(sheet, row0, row1, mode)
        if anim == "idle":
            idle_heights = [b.y1 - b.y0 + 1 for b in boxes]
    if not idle_heights:
        raise SystemExit("could not find idle frames; cannot derive a scale")
    scale = TARGET_BODY_HEIGHT / float(np.median(idle_heights))
    print(f"global scale: {scale:.4f} (idle body height {np.median(idle_heights):.1f}px)")

    pieces: list[Piece] = []
    for anim, row0, row1, mode in ROWS:
        boxes = frame_boxes(sheet, row0, row1, mode)
        if not boxes:
            raise SystemExit(f"no frames found for animation {anim!r}")
        slots = slots_from_boxes(boxes, width)
        ya = max(0, row0 - 8)
        yb = min(height, row1 + 8)
        print(f"{anim:8s} {len(boxes):2d} frames  "
              f"bodies x[{boxes[0].x0},{boxes[-1].x1}]")

        for index, box in enumerate(boxes):
            xa, xb = slots[index]
            slot = (xa, xb, ya, yb)
            crop = Image.fromarray(
                sheet[ya:yb, xa:xb].astype(np.uint8), "RGBA")
            new_w = max(1, int(round(crop.width * scale)))
            new_h = max(1, int(round(crop.height * scale)))
            # Keep the source's hard pixel edges when fitting each verified
            # pose crop into the 48x48 runtime frame. LANCZOS introduces
            # blended colors and soft edges that are out of place in pixel art.
            resized = crop.resize((new_w, new_h), Image.Resampling.NEAREST)

            sx = new_w / float(crop.width)
            sy = new_h / float(crop.height)
            cx = (box.cx - xa) * sx
            bottom = (box.y1 + 1 - ya) * sy
            dest_x = int(round(FRAME / 2.0 - cx))
            dest_y = int(round(FRAME - bottom))

            pieces.append(Piece(anim, index + 1, slot, box, resized,
                                (dest_x, dest_y), box.y1 - box.y0 + 1))
    return pieces


def harden_alpha(arr: np.ndarray) -> np.ndarray:
    out = arr.copy()
    alpha = arr[:, :, 3]
    levels = np.zeros_like(alpha)
    levels[alpha >= ALPHA_LEVELS[0]] = 96
    levels[alpha >= ALPHA_LEVELS[1]] = 176
    levels[alpha >= ALPHA_LEVELS[2]] = 255
    out[:, :, 3] = levels
    return out


def compose(piece: Piece) -> Image.Image:
    canvas = Image.new("RGBA", (FRAME, FRAME), (0, 0, 0, 0))
    resized = piece.resized
    # Crop the resized slot to the destination rectangle before compositing;
    # anything that falls outside the 48x48 canvas is intentionally clipped.
    left = max(0, -piece.dest[0])
    top = max(0, -piece.dest[1])
    right = min(resized.width, FRAME - piece.dest[0])
    bottom = min(resized.height, FRAME - piece.dest[1])
    if right <= left or bottom <= top:
        return canvas
    clipped = resized.crop((left, top, right, bottom))
    arr = harden_alpha(np.array(clipped))
    canvas.alpha_composite(Image.fromarray(arr, "RGBA"),
                           (piece.dest[0] + left, piece.dest[1] + top))
    return canvas


def preview(pieces: list[Piece], path: Path,
            scale: int = 4) -> None:
    order = [row[0] for row in ROWS]
    rows = {anim: [p for p in pieces if p.anim == anim] for anim in order}
    max_frames = max(len(v) for v in rows.values())
    cell = FRAME * scale
    gap = 6
    label_h = 18
    width = max_frames * (cell + gap) + gap
    height = len(order) * (cell + gap + label_h) + gap
    sheet = Image.new("RGBA", (width, height), (24, 20, 30, 255))
    draw = ImageDraw.Draw(sheet)
    for row_index, anim in enumerate(order):
        y = gap + row_index * (cell + gap + label_h)
        draw.text((gap, y), anim.upper(), fill=(240, 220, 180, 255))
        for col, piece in enumerate(rows[anim]):
            frame = compose(piece)
            frame = frame.resize((cell, cell), Image.NEAREST)
            x = gap + col * (cell + gap)
            sheet.alpha_composite(frame, (x, y + label_h))
    path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(path)
    print(f"preview: {path}")


def write_frames(pieces: list[Piece]) -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for old in OUT_DIR.glob("*.png"):
        old.unlink()

    manifest = ["# Generated by tools/extract_player_frames.py",
                "# anim frame source_slot body_bbox source_body_h",
                f"# frame_size {FRAME}x{FRAME}"]
    for piece in pieces:
        name = f"{piece.anim}_{piece.index}.png"
        compose(piece).save(OUT_DIR / name)
        xa, xb, ya, yb = piece.slot
        manifest.append(
            f"{piece.anim} {piece.index} {xa},{xb},{ya},{yb} "
            f"{piece.body.x0},{piece.body.x1},{piece.body.y0},{piece.body.y1} "
            f"{piece.body_h}")
    (OUT_DIR / "manifest.txt").write_text("\n".join(manifest) + "\n",
                                           encoding="utf-8")
    print(f"wrote {len(pieces)} frames to {OUT_DIR}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--preview", action="store_true",
                        help="also write a labelled montage for visual review")
    args = parser.parse_args()

    sheet = load_sheet(args.source)
    pieces = build_pieces(sheet)
    write_frames(pieces)
    if args.preview:
        preview(pieces, PREVIEW)


if __name__ == "__main__":
    main()
