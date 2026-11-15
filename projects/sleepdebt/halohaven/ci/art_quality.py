#!/usr/bin/env python3
"""Halo Haven texture-style regression audit based on inspected vanilla 1.21.1.
This reads our indexed original 16×16 maps only; it never ships Mojang assets.

Color-count alone is not art direction: inspect dominant-area ratios, change
frequency, largest connected flat cluster and alpha silhouette, then visually
inspect repeated 4×4 walls. Thresholds are deliberately material-specific.
"""
from __future__ import annotations

from collections import Counter, deque
from pathlib import Path
import json
import sys


ROOT = Path(__file__).resolve().parents[1]
ART = json.loads((ROOT / "art/pixel_atlas.json").read_text(encoding="utf8"))
SIZE = 16
TEXTURES = ART["tiles"]
PALETTES = ART["palettes"]

NATURAL = (
    "cloudstone", "cloudstone_alt", "cloudstone_fractured",
    "cloudstone_top", "cloudstone_top_alt",
    "skygrass", "skygrass_sparse", "skygrass_dense",
    "skygrass_side", "skygrass_side_alt",
    "moonstone", "moonstone_alt", "moonstone_fractured",
    "sky_leaves", "sky_leaves_alt", "sky_leaves_sparse",
    "star_ore", "star_ore_alt",
)
CRAFTED = ("azure_planks", "pearl_bricks", "sungold_tiles")
EXPECTED = 33


def stats(name: str) -> dict:
    spec = TEXTURES[name]
    colors = PALETTES[spec["palette"]]
    grid = spec["rows"]
    assert len(grid) == SIZE and all(len(row) == SIZE for row in grid), name
    counts = Counter("".join(grid))
    assert all(ch.isdigit() and int(ch) < len(colors) for ch in counts), name

    transitions = sum(
        (grid[y][x] != grid[y][x + 1])
        for y in range(SIZE) for x in range(SIZE - 1)
    ) + sum(
        (grid[y][x] != grid[y + 1][x])
        for y in range(SIZE - 1) for x in range(SIZE)
    )
    visited: set[tuple[int, int]] = set()
    maximum = 0
    for y in range(SIZE):
        for x in range(SIZE):
            if (x, y) in visited:
                continue
            original = grid[y][x]
            pending = deque([(x, y)])
            visited.add((x, y))
            component = 0
            while pending:
                xx, yy = pending.popleft()
                component += 1
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = xx + dx, yy + dy
                    if 0 <= nx < SIZE and 0 <= ny < SIZE:
                        if (nx, ny) not in visited and grid[ny][nx] == original:
                            visited.add((nx, ny))
                            pending.append((nx, ny))
            maximum = max(maximum, component)

    alpha = [int(colors[int(ch)][:2], 16) for row in grid for ch in row]
    return {
        "name": name,
        "used_colors": len(counts),
        "dominant_fraction": max(counts.values()) / 256.0,
        "edge_fraction": transitions / 480.0,
        "largest_flat_cluster": maximum,
        "transparent": sum(value == 0 for value in alpha),
        "translucent": sum(value < 255 for value in alpha),
    }


def main() -> None:
    if ART.get("resolution") != 16 or len(TEXTURES) != EXPECTED:
        raise AssertionError("A 16×16, 33-face source atlas is required")
    all_stats = [stats(name) for name in sorted(TEXTURES)]
    for data in all_stats:
        n = data["name"]
        print(
            f"{n:25} colors={data['used_colors']:2} "
            f"dominant={data['dominant_fraction']:.0%} "
            f"edges={data['edge_fraction']:.0%} "
            f"largest-flat={data['largest_flat_cluster']:3} "
            f"transparent={data['transparent']:3}"
        )
        if n in NATURAL:
            assert data["used_colors"] >= 5, f"{n}: unresolved low-color flat prototype"
            assert data["dominant_fraction"] <= 0.55, f"{n}: majority is one flat shade"
            assert data["edge_fraction"] >= 0.40, f"{n}: insufficient material detail"
            assert data["largest_flat_cluster"] <= 75, f"{n}: empty patch too large"
        if n in CRAFTED:
            assert data["used_colors"] >= 5, f"{n}: unfinished material palette"
            assert data["dominant_fraction"] <= 0.55, f"{n}: overly flat crafted surface"
        if n.startswith("sky_leaves"):
            assert 35 <= data["transparent"] <= 160, f"{n}: broken foliage cutouts"
            alpha_values = {
                int(PALETTES[TEXTURES[n]["palette"]][int(ch)][:2], 16)
                for row in TEXTURES[n]["rows"] for ch in row
            }
            assert alpha_values == {0, 255}, f"{n}: foliage requires hard alpha"
        if n.endswith("_glass"):
            assert data["translucent"] >= 80, f"{n}: colored glass is opaque"

    # These are study results, not copied assets or mechanically enforced
    # universal ideals. Vanilla 1.21.1 Stone has 4 colors and ~46% dominant;
    # Calcite 6/~31%, Moss 6/~28%, Oak Planks 7/~20%. Our natural textures
    # need similarly distributed material information, not the same patterns.
    print("PASS: material-specific 16x color, clustering, and alpha checks")
    print("Human 4×4 tiling review and in-game rendering remain required.")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, ValueError) as e:
        sys.exit("FAIL: Pixel Art audit: " + str(e))
