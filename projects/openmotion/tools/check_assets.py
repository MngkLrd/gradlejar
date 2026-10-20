from pathlib import Path
import json
root = Path(__file__).resolve().parents[1]
res = root / 'src/main/resources'
for block in ('air_fan', 'item_cannon'):
    for path in [
        f'assets/openmotion/models/block/{block}.json',
        f'assets/openmotion/models/item/{block}.json',
        f'assets/openmotion/blockstates/{block}.json',
        f'data/openmotion/loot_table/blocks/{block}.json',
        f'data/openmotion/recipe/{block}.json',
        *(f'assets/openmotion/textures/block/{block}_{t}.png' for t in ('front','side','top'))
    ]:
        assert (res/path).is_file(), path
for file in res.rglob('*.json'):
    json.loads(file.read_text('utf8'))
print('Assets: PASS')
