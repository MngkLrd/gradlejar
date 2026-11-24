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

# The baked model must use the converted legacy shield origin in all states.
with zipfile.ZipFile(sys.argv[1]) as z:
    base='assets/catchtheblast/models/item/'
    for suffix in ('','_blocking'):
        ref=None
        for stage in range(4):
            m=json.loads(z.read(f'{base}blast_stage_{stage}{suffix}.json'))
            assert m['elements'][0]['from']==[2,0,8]
            assert m['elements'][0]['to']==[14,16,10]
            assert m['elements'][-1]['from']==[7,5,4]
            assert m['elements'][-1]['to']==[9,11,8]
            assert m['elements'][0]['faces']['south']['uv']==[0.5,0.5,3.5,4.5]
            assert set(m['display'])=={'thirdperson_righthand','thirdperson_lefthand','firstperson_righthand','firstperson_lefthand','gui','fixed','ground'}
            if not suffix:
                assert m['display']['thirdperson_lefthand']['translation']==[2,-2,4]
                assert m['display']['thirdperson_righthand']['translation']==[2,-2,4]
            else:
                assert m['display']['thirdperson_lefthand']['rotation']==[45,155,0]
            if ref is None: ref=m['display']
            assert m['display']==ref
print('Converted 1.21.1 shield geometry and all eight display transforms validated')
