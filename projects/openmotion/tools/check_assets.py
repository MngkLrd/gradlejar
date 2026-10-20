from pathlib import Path
import json
import struct

root = Path(__file__).resolve().parents[1]
res = root / "src/main/resources"
models = ["air_fan", "air_fan_stopped", "item_cannon", "item_cannon_powered"]

for name in models:
    model = json.loads((res / f"assets/openmotion/models/block/{name}.json").read_text("utf8"))
    elements = model["elements"]
    assert 12 <= len(elements) <= 36, (name, len(elements))
    for el in elements:
        assert all(0 <= x <= 16 for x in el["from"] + el["to"]), el
        assert all(a < b for a, b in zip(el["from"], el["to"])), el
        for face in el["faces"].values():
            assert face["texture"] in {"#" + key for key in model["textures"]}, face
    for t in model["textures"].values():
        namespace, path = t.split(":")
        file = res / f"assets/{namespace}/textures/{path}.png"
        assert file.exists(), file
        raw = file.read_bytes()
        assert raw[:8] == b"\\x89PNG\\r\\n\\x1a\\n".decode("unicode_escape").encode("latin-1"), file
        assert struct.unpack(">II", raw[16:24]) == (32, 32), file

for block in ("air_fan", "item_cannon"):
    for suffix in ("models/item", "blockstates"):
        assert (res / f"assets/openmotion/{suffix}/{block}.json").exists()
    states = json.loads((res / f"assets/openmotion/blockstates/{block}.json").read_text("utf8"))["variants"]
    assert len(states) == 8, block
    for state in states.values():
        assert (res / f'assets/openmotion/models/{state["model"].split(":")[1]}.json').exists()

for file in res.rglob("*.json"):
    json.loads(file.read_text("utf8"))
print("OpenMotion 3D models and pixel textures: PASS")
