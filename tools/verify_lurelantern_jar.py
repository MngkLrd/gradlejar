"""Strict CI gate for all Lure Lantern Minecraft/loader ports."""
import json,sys
from pathlib import Path
from zipfile import ZipFile

jar=Path(sys.argv[1]);root=jar.resolve().parents[2]
profile=json.loads((root/'build-profile.json').read_text('utf-8'))
mc=profile['minecraft']
folder='recipes' if mc=='1.20.1' else 'recipe'
base='assets/lurelantern/'
with ZipFile(jar) as z:
    names=set(z.namelist())
    expected=['pack.mcmeta']
    for kind in ('lure_lantern','advanced_lure_lantern'):
        expected += [f'{base}blockstates/{kind}.json',f'{base}models/block/{kind}.json',
            f'{base}models/block/{kind}_active.json',f'{base}models/block/hanging_{kind}.json',
            f'{base}models/block/hanging_{kind}_active.json',f'{base}models/item/{kind}.json']
        expected += [f'data/lurelantern/{folder}/{kind}.json']
    expected += [f'{base}lang/en_us.json',f'{base}lang/ru_ru.json']
    for type in ('advanced_lure_lantern','lure_lantern'):
        expected += [f'{base}textures/block/{type}.png',f'{base}textures/block/{type}_active.png',
            f'{base}textures/block/{type}_glass.png',f'{base}textures/block/{type}_glass_active.png']
    if mc.startswith('26.'):
        expected += [f'{base}items/lure_lantern.json',f'{base}items/advanced_lure_lantern.json']
    missing=[p for p in expected if p not in names]
    if missing:raise ValueError(f'Incomplete Lure Lantern JAR missing {missing}')
    known={'lure_lantern':([' W ','GLG',' W '],{'W':'minecraft:wheat','G':'minecraft:glow_berries','L':'minecraft:lantern'}),
           'advanced_lure_lantern':([' A ','ELE',' A '],{'A':'minecraft:amethyst_shard','E':'minecraft:emerald','L':'lurelantern:lure_lantern'})}
    for name,(pattern,ingredients) in known.items():
        recipe=json.loads(z.read(f'data/lurelantern/{folder}/{name}.json'))
        assert recipe['type']=='minecraft:crafting_shaped' and recipe['pattern']==pattern,recipe
        for key,item in ingredients.items():
            value=recipe['key'][key]
            assert (value if isinstance(value,str) else value.get('item'))==item,(name,key,value)
            assert isinstance(value,str)==mc.startswith('26.'),'Wrong version ingredient syntax'
        result=recipe['result']
        assert result['item' if mc=='1.20.1' else 'id']=='lurelantern:'+name,result
        assert result.get('count',1)==1
        state=json.loads(z.read(f'{base}blockstates/{name}.json'))
        assert len(state['variants'])==8,(name,'missing active/hanging/waterlogged variants')
    print('PASS Lure Lantern resource/recipe validation:',mc,profile['loader'])
