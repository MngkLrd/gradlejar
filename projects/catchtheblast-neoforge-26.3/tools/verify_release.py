import json, sys, zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    assert z.testzip() is None
    n = set(z.namelist())
    assert 'dev/ataflore/catchtheblast/CatchTheBlast.class' in n
    assert 'assets/catchtheblast/textures/item/blast_shield_3.png' in n
    assert 'assets/catchtheblast/items/blast_shield.json' in n
    r = json.loads(z.read('data/catchtheblast/recipe/blast_shield.json'))
    assert all(isinstance(v,str) for v in r['key'].values())
    a = json.loads(z.read('data/catchtheblast/advancement/recipes/combat/blast_shield.json'))
    assert set(a['criteria']) == {k for group in a['requirements'] for k in group}
    assert all(c['trigger'] != 'minecraft:recipe_unlocked' for c in a['criteria'].values())
    assert a['rewards']['recipes'] == ['catchtheblast:blast_shield']
    meta = json.loads(z.read('pack.mcmeta'))['pack']
    assert 'min_format' in meta and 'max_format' in meta
    assert not any(p.endswith('.b64') for p in n)
print('Catch the Blast: recipe, advancement and pack metadata validated')
