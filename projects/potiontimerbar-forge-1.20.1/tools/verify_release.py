import sys,zipfile,json,tomllib
with zipfile.ZipFile(sys.argv[1]) as z:
    names=set(z.namelist())
    for cls in ('PotionTimerBar','PotionPalette','EffectTracker','PotionHudOverlay','HudConfig'):
        assert 'dev/minemods/potiontimerbar/'+cls+'.class' in names,cls
    assert b'collect' in z.read('dev/minemods/potiontimerbar/EffectTracker.class')
    assert not any(n.endswith('.java') for n in names)
    m=tomllib.loads(z.read('META-INF/mods.toml').decode())
    assert m['mods'][0]['version']=='0.4.0'
print('Verified stacked HUD and release metadata')
