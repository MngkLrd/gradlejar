import sys,zipfile,json,tomllib
with zipfile.ZipFile(sys.argv[1]) as z:
    names=set(z.namelist())
    for cls in ('PotionTimerBar','PotionPalette','EffectTracker','PotionHudOverlay','HudConfig'):
        assert 'dev/minemods/potiontimerbar/'+cls+'.class' in names,cls
    assert b'collect' in z.read('dev/minemods/potiontimerbar/EffectTracker.class')
    assert not any(n.endswith('.java') for n in names)
    m=json.loads(z.read('fabric.mod.json'))
    assert m['version']=='0.4.0' and m['depends']['minecraft']=='1.20.1' and m['environment']=='client'
print('Verified stacked HUD and release metadata')
