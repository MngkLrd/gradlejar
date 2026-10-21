#!/usr/bin/env python3
"""Validate TraumaCraft 16x16 item sprites and related JSON without external libraries."""
import json
import struct
import zlib
from pathlib import Path

root = Path(__file__).resolve().parents[1] / "src/main/resources"
assets = root / "assets/traumacraft"
data = root / "data/traumacraft"
sig = b"\x89PNG\r\n\x1a\n"

for name in ("bandage", "splint", "medkit"):
    png = assets / "textures/item" / f"{name}.png"
    raw = png.read_bytes()
    assert raw.startswith(sig), f"{png}: invalid PNG signature"
    pos = len(sig)
    image_data = b""
    width = height = None
    while pos + 12 <= len(raw):
        n = struct.unpack(">I", raw[pos:pos + 4])[0]
        kind = raw[pos + 4:pos + 8]
        block = raw[pos + 8:pos + 8 + n]
        checksum = struct.unpack(">I", raw[pos + 8 + n:pos + 12 + n])[0]
        assert zlib.crc32(kind + block) == checksum, f"{png}: bad CRC"
        pos += n + 12
        if kind == b"IHDR":
            width, height, depth, color, *_ = struct.unpack(">IIBBBBB", block)
            assert (width, height, depth, color) == (16, 16, 8, 6), f"{png}: must be 16x16 RGBA"
        if kind == b"IDAT":
            image_data += block
        if kind == b"IEND":
            break
    pixels = zlib.decompress(image_data)
    assert len(pixels) == 16 * (1 + 16 * 4), f"{png}: invalid row size"

    model = json.loads((assets / "models/item" / f"{name}.json").read_text())
    assert model["textures"]["layer0"] == f"traumacraft:item/{name}"
    recipe = json.loads((data / "recipe" / f"{name}.json").read_text())
    assert recipe["result"]["id"] == f"traumacraft:{name}"
    print(f"{name}: valid 16x16 RGBA sprite, item model and recipe")

for lang in ("ru_ru", "en_us"):
    names = json.loads((assets / "lang" / f"{lang}.json").read_text())
    for item in ("bandage", "splint", "medkit"):
        assert f"item.traumacraft.{item}" in names
    assert "itemGroup.traumacraft.medical" in names
print("TraumaCraft assets validated.")
