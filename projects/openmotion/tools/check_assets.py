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

# 0.1.3: a visible modeled rotor, with correctly reversed redstone states.
fan = json.loads((res / 'assets/openmotion/models/block/air_fan.json').read_text('utf8'))
blades = [e for e in fan['elements'] if any(f['texture'] == '#om_blade' for f in e['faces'].values())]
assert len(blades) == 4
assert all(3.0 < e['from'][2] < 5.5 for e in blades)
assert (res / 'assets/openmotion/textures/block/om_blade.png').is_file()
states = json.loads((res / 'assets/openmotion/blockstates/air_fan.json').read_text('utf8'))['variants']
assert all(v['model'].endswith('air_fan' if 'powered=true' in k else 'air_fan_stopped') for k,v in states.items())
print('0.1.3 redstone and rotor assets: PASS')
