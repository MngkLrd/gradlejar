from pathlib import Path
import sys,zipfile
p=Path(sys.argv[1])
with zipfile.ZipFile(p) as z:
    names=set(z.namelist())
    for name in ('PotionTimerBar','EffectTracker','PotionPalette','PotionHudOverlay','HudConfig'):
        assert 'dev/minemods/potiontimerbar/'+name+'.class' in names,name
    assert not any(x.endswith('.java') for x in names)
    import json
    m=json.loads(z.read('fabric.mod.json'))
    assert m['environment']=='client' and m['id']=='potiontimerbar' and m['version']=='0.3.1' and m['depends']['minecraft']=='26.2'
print('Verified potion palette, effect tracker, HUD, client registration and release metadata:',p.name)
