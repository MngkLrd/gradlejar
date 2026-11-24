import sys,zipfile,json,struct
with zipfile.ZipFile(sys.argv[1]) as z:
    names=set(z.namelist())
    for n in ['CatchTheBlast','item/BlastShieldItem','item/Charge','combat/BlastCombat','combat/BlastFx','client/ClientSetup']:
        assert 'dev/ataflore/catchtheblast/'+n+'.class' in names,n
    a='assets/catchtheblast/'
    for n in ['shield_front','shield_back','core_0','core_1','core_2','core_3']:
        data=z.read(a+'textures/item/'+n+'.png')
        assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(16,16),n
    for n in names:
        if n.startswith(a) and n.endswith('.json'): json.loads(z.read(n))
    assert len(json.loads(z.read(a+'models/item/blast_shield.json'))['overrides'])==7
    assert json.loads(z.read('data/catchtheblast/recipe/blast_shield.json'))['result']['id']=='catchtheblast:blast_shield'
    assert not any(n.endswith('.png.b64') for n in names)
print('Catch the Blast assets and compiled classes verified')
