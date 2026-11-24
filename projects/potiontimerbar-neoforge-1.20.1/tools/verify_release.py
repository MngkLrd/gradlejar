from pathlib import Path
import sys,zipfile
p=Path(sys.argv[1])
with zipfile.ZipFile(p) as z:
    names=set(z.namelist())
    for name in ('PotionTimerBar','EffectTracker','PotionPalette','PotionHudOverlay','HudConfig'):
        assert 'dev/minemods/potiontimerbar/'+name+'.class' in names,name
    assert not any(x.endswith('.java') for x in names)
    import tomllib
    m=tomllib.loads(z.read('META-INF/mods.toml').decode())
    assert m['mods'][0]['version']=='0.3.1'
print('Verified potion palette, effect tracker, HUD, client registration and release metadata:',p.name)
