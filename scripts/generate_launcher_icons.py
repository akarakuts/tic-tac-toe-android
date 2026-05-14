#!/usr/bin/env python3
"""
Generate launcher mipmaps + RuStore 512 listing icon from one layout.
Run from repo root:  .venv-icon/bin/python scripts/generate_launcher_icons.py

Layout matches res/drawable/ic_launcher_background.xml + ic_launcher_foreground.xml
"""

from __future__ import annotations

import math
from pathlib import Path

from PIL import Image, ImageDraw

# Must stay in sync with vector drawables (108dp logical space).
BG = (30, 58, 138, 255)  # #1E3A8A
GRID = (248, 250, 252, 255)  # near white
X_COLOR = (252, 165, 165, 255)  # #FCA5A5
O_COLOR = (253, 224, 71, 255)  # #FDE047

MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def draw_icon(size: int) -> Image.Image:
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    s = float(size)

    def sc(v: float) -> float:
        return v / 108.0 * s

    pad = sc(18.0)
    inner = sc(72.0)
    r_corner = s * 0.22
    xy = (0, 0, s - 1, s - 1)
    draw.rounded_rectangle(xy, radius=r_corner, fill=BG)

    # Inner grid area (18..90 in 108 space)
    x0, y0 = pad, pad
    x1, y1 = pad + inner, pad + inner
    lw = max(2.0, sc(3.0))

    # Vertical grid lines at +24 and +48 from x0
    gx1 = x0 + sc(24.0)
    gx2 = x0 + sc(48.0)
    draw.line([(gx1, y0), (gx1, y1)], fill=GRID, width=int(round(lw)))
    draw.line([(gx2, y0), (gx2, y1)], fill=GRID, width=int(round(lw)))

    gy1 = y0 + sc(24.0)
    gy2 = y0 + sc(48.0)
    draw.line([(x0, gy1), (x1, gy1)], fill=GRID, width=int(round(lw)))
    draw.line([(x0, gy2), (x1, gy2)], fill=GRID, width=int(round(lw)))

    # X in top-left cell: center (30,30) half-cell ~10 in 108 space
    cx, cy = x0 + sc(12.0), y0 + sc(12.0)
    h = sc(9.0)
    draw.line([(cx - h, cy - h), (cx + h, cy + h)], fill=X_COLOR, width=int(round(lw + 1)))
    draw.line([(cx + h, cy - h), (cx - h, cy + h)], fill=X_COLOR, width=int(round(lw + 1)))

    # O in bottom-right cell: center (78,78) in 108 space (grid origin + 60 along x/y)
    ox = x0 + sc(60.0)
    oy = y0 + sc(60.0)
    orad = sc(9.0)
    bbox = (ox - orad, oy - orad, ox + orad, oy + orad)
    draw.ellipse(bbox, outline=O_COLOR, width=int(round(lw + 1)))

    return img


def main() -> None:
    root = Path(__file__).resolve().parents[1]
    res = root / "app" / "src" / "main" / "res"
    rustore = root / "docs" / "rustore"
    rustore.mkdir(parents=True, exist_ok=True)

    for folder, px in MIPMAP_SIZES.items():
        im = draw_icon(px)
        out_dir = res / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        for name in ("ic_launcher.webp", "ic_launcher_round.webp"):
            im.save(out_dir / name, "WEBP", quality=92, method=6)

    store = draw_icon(512)
    store.save(rustore / "ic_launcher_store_512.png", "PNG")
    print("Wrote mipmaps +", rustore / "ic_launcher_store_512.png")


if __name__ == "__main__":
    main()
