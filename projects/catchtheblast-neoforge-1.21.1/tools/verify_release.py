import sys,zipfile,json,struct
with zipfile.ZipFile(sys.argv[1]) as z:
    names=set(z.namelist())
    for n in ['CatchTheBlast','item/BlastShieldItem','item/Charge','combat/BlastCombat','combat/BlastFx','combat/TravellingWave','client/ClientSetup','client/BlastShieldRenderer','client/BlastShieldGeometry','client/BlastShieldClientExtensions','client/ChargeHud','config/BlastConfig','config/BlastCommands']:
        assert 'dev/ataflore/catchtheblast/'+n+'.class' in names,n
    a='assets/catchtheblast/'
    for i in range(4):
        for prefix in ['blast_shield_','blast_glow_']:
            data=z.read(a+'textures/entity/'+prefix+str(i)+'.png')
            assert data[:8]==b'\x89PNG\r\n\x1a\n' and struct.unpack('>II',data[16:24])==(64,64)
    for n in names:
        if n.startswith(a) and n.endswith('.json'): json.loads(z.read(n))
    model=json.loads(z.read(a+'models/item/blast_shield.json'))
    assert model['parent']=='builtin/entity' and len(model['overrides'])==1
    assert json.loads(z.read('data/catchtheblast/recipe/blast_shield.json'))['result']['id']=='catchtheblast:blast_shield'
    assert not any(n.endswith('.png.b64') for n in names)
print('Native shield renderer, four textures, HUD and travelling wave verified')
