import sys,zipfile,json
with zipfile.ZipFile(sys.argv[1]) as z:
    assert z.testzip() is None
    for f in ['dev/ataflore/huss/HussMod.class','dev/ataflore/huss/client/TrainingModel.class','dev/ataflore/huss/client/TrainingRenderer.class','dev/ataflore/huss/client/ClientEvents.class','dev/ataflore/huss/training/Training.class','dev/ataflore/huss/item/DisciplineWhistle.class','assets/husshusshuss/textures/item/discipline_whistle.png','assets/husshusshuss/sounds/huss.ogg','assets/husshusshuss/sounds.json','data/husshusshuss/recipe/discipline_whistle.json']:
        assert f in z.namelist(),f
    assert z.read('assets/husshusshuss/sounds/huss.ogg').startswith(b'OggS')
    assert z.read('assets/husshusshuss/textures/item/discipline_whistle.png').startswith(b'\x89PNG')
    for f in z.namelist():
        if f.startswith(('assets/husshusshuss/','data/husshusshuss/')) and f.endswith('.json'): json.loads(z.read(f))
print('Huss Huss Huss assets and classes verified')
