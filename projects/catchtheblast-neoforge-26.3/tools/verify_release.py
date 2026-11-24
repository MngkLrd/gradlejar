import zipfile,sys,json
with zipfile.ZipFile(sys.argv[1]) as z:
 n=set(z.namelist()); assert 'dev/ataflore/catchtheblast/CatchTheBlast.class' in n
 assert 'assets/catchtheblast/textures/entity/blast_shield_3.png' in n
 assert 'assets/catchtheblast/items/blast_shield.json' in n
 assert isinstance(json.loads(z.read('data/catchtheblast/recipe/blast_shield.json'))['key']['I'],str)
 assert not any(p.endswith('.b64') for p in n)
print('Catch the Blast modern jar verified')
