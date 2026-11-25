import argparse, io, json, hashlib, urllib.request, zipfile
from pathlib import Path
from PIL import Image, ImageDraw

p=argparse.ArgumentParser()
p.add_argument('--download',action='store_true')
p.add_argument('--vanilla',type=Path)
p.add_argument('out',type=Path)
a=p.parse_args()
root=a.out/'assets/minecraft/textures/gui'
def save(name,im):
    f=root/(name+'.png');f.parent.mkdir(parents=True,exist_ok=True);im.save(f)
def meta(name,w,h,b):
    f=root/(name+'.png.mcmeta');f.write_text(json.dumps({'gui':{'scaling':{'type':'nine_slice','width':w,'height':h,'border':b}}}))
def get(url): 
    with urllib.request.urlopen(url,timeout=120) as r:return r.read()
# Hand-authored pixel cluster coordinates, no gradients, dithering or noise.
def button(state):
    c={'normal':('#30251e','#583c2b','#745039','#906348','#b2cbd0','#eff6e9','#304b3d','#608363','#a73836'),
       'hover':('#342819','#77502d','#956337','#bc824a','#d9e7de','#fffce5','#365b3c','#79a65e','#e0583b'),
       'disabled':('#353332','#4b4842','#5a554c','#6a655c','#828e8d','#b7c3bd','#47544d','#68746a','#744c49')}[state]
    edge,shadow,base,lit,ice,snow,leaf,leaflit,berry=c
    im=Image.new('RGBA',(200,20));d=ImageDraw.Draw(im)
    d.rectangle((0,1,199,18),fill=edge);d.rectangle((1,0,198,19),fill=edge)
    d.rectangle((1,2,198,16),fill=shadow);d.rectangle((2,3,197,15),fill=base)
    d.line((3,4,196,4),fill=lit)
    for x,y,w in [(12,7,19),(44,12,23),(72,6,14),(103,13,28),(143,8,17),(174,11,11)]:
        d.line((x,y,x+w,y),fill=shadow);d.line((x+2,y+1,x+w-4,y+1),fill=lit)
    # A snow cap along the top, with distinct short drifts.
    d.line((3,1,196,1),fill=shadow)
    for x,w in {'normal':[(3,16),(27,9),(53,23),(100,12),(153,19),(184,13)],'hover':[(3,10),(35,17),(76,11),(122,26),(174,23)],'disabled':[(3,6),(66,10),(143,11),(191,6)]}[state]:
        d.rectangle((x,0,x+w-1,1),fill=snow)
        d.line((x+1,2,x+w-3,2),fill=ice)
    for x,sgn in [(2,1),(197,-1)]:
        def dot(dx,dy,color):d.point((x+dx*sgn,dy),fill=color)
        for dx,dy in [(0,5),(1,5),(2,6),(3,6),(4,7),(3,8),(2,8),(1,9),(0,9),(1,10),(2,11),(3,11),(4,12),(2,13),(0,13)]:dot(dx,dy,leaf)
        for dx,dy in [(0,5),(1,6),(2,7),(1,10),(2,11),(0,12)]:dot(dx,dy,leaflit)
        for dx,dy in [(1,8),(2,8),(1,9)]:dot(dx,dy,berry)
    return im
for name,state in [('button','normal'),('button_highlighted','hover'),('button_disabled','disabled')]:
    path='sprites/widget/'+name;save(path,button(state));meta(path,200,20,{'left':7,'right':7,'top':4,'bottom':4})

def bar(filled):
    im=Image.new('RGBA',(182,5));d=ImageDraw.Draw(im)
    d.line((1,0,180,0),fill='#2c403c');d.rectangle((0,1,181,3),fill='#26332f');d.line((1,4,180,4),fill='#182725')
    d.line((2,2,179,2),fill='#50665a')
    if filled:
        colors=[('#d94d40','#ffb179'),('#d1a34d','#fff0aa'),('#4c9a67','#b7e4a0'),('#69aeb6','#e4f7ef')]
        for i,x in enumerate(range(3,181,6)):
            body,light=colors[i%4];d.line((x,1,x+1,1),fill=light);d.rectangle((x,2,x+2,3),fill=body);d.point((x,2),fill=light)
    return im
for n,b in [('experience_bar_background',False),('experience_bar_progress',True)]:save('sprites/hud/'+n,bar(b))
def hotbar():
    im=Image.new('RGBA',(182,22));d=ImageDraw.Draw(im)
    d.rectangle((0,0,181,21),fill='#31251d');d.rectangle((1,1,180,20),fill='#886849')
    for i in range(9):
        x=1+20*i;d.rectangle((x+1,2,x+18,19),fill='#44382d');d.line((x+2,3,x+17,3),fill='#302b26');d.line((x+2,19,x+17,19),fill='#a88d68')
    for x,w in [(2,14),(24,8),(39,13),(61,10),(88,11),(109,16),(138,10),(160,18)]:
        d.line((x,0,x+w,0),fill='#eff6ed');d.line((x+1,1,x+w-2,1),fill='#b2c9ca')
    return im
save('sprites/hud/hotbar',hotbar())
im=Image.new('RGBA',(24,23));d=ImageDraw.Draw(im)
d.rectangle((0,0,23,22),outline='#4a3020');d.rectangle((1,1,22,21),outline='#e9bd65');d.rectangle((2,2,21,20),outline='#806039');d.line((2,1,21,1),fill='#fff0bb')
save('sprites/hud/hotbar_selection',im)
# Gingerbread icons; silhouette deliberately fits vanilla 9x9.
cookie=['...ooo...','..olhho..','..ohhho..','.ohwhwho.','oohhhhoo.','..ohhho..','..ohwho..','..ohoho..','..oo.oo..']
for n,mode in [('food_full','full'),('food_half','half'),('food_empty','empty')]:
    im=Image.new('RGBA',(9,9));pix=im.load()
    pal={'o':'#573321','l':'#e2a55f','h':'#ae6b39','w':'#fff2d0'}
    for y,row in enumerate(cookie):
        for x,k in enumerate(row):
            if k=='.':continue
            col=pal[k] if mode=='full' or mode=='half' and x<4 else ('#604e3e' if k=='o' else '#302c28')
            pix[x,y]=tuple(bytes.fromhex(col[1:]))+(255,)
    save('sprites/hud/'+n,im)
# Optional vanilla input retains slot, button, icon and atlas geometry.
z=None
if a.vanilla:z=zipfile.ZipFile(a.vanilla)
elif a.download:
    manifest=json.loads(get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
    info=json.loads(get(next(v['url'] for v in manifest['versions'] if v['id']=='1.21.1')))
    client=info['downloads']['client'];raw=get(client['url'])
    assert hashlib.sha1(raw).hexdigest()==client['sha1']
    z=zipfile.ZipFile(io.BytesIO(raw))
if z:
    prefix='assets/minecraft/textures/gui/'
    def theme(im,kind):
        im=im.convert('RGBA')
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,al=im.getpixel((x,y))
                if not al or max(r,g,b)-min(r,g,b)>14:continue
                v=(r+g+b)//3
                # Neutral vanilla panel values -> spruce, warm parchment, cold highlight.
                # Preserve value ordering and dark text contrast.
                if kind=='panel':
                    if v<48:rgb=(v,max(0,v-3),max(0,v-7))
                    elif v<115:rgb=(min(255,v+13),max(0,v-7),max(0,v-19))
                    elif v<170:rgb=(v+15,v+3,v-17)
                    elif v<222:rgb=(min(255,v+14),min(255,v+7),max(0,v-11))
                    else:rgb=(min(255,v+2),min(255,v+5),min(255,v+8))
                else:
                    rgb=(max(0,v-12),min(255,v+4),min(255,v+1)) if v<165 else (min(255,v+1),min(255,v+7),min(255,v+12))
                im.putpixel((x,y),(*rgb,al))
        return im
    for entry in z.namelist():
        if not entry.startswith(prefix) or not entry.endswith('.png'):continue
        rel=entry[len(prefix):-4]
        if (root/(rel+'.png')).exists():continue
        kind=None
        if rel.startswith('container/') or rel.startswith('sprites/container/'):kind='panel'
        elif rel.startswith(('sprites/widget/','sprites/hud/','sprites/advancements/','sprites/toast/')):kind='frame'
        elif rel in ('title/minecraft','title/edition','menu_background','inworld_menu_background','options_background'):kind='frame'
        if kind is None:continue
        im=theme(Image.open(io.BytesIO(z.read(entry))),kind)
        # Tiny snow clusters placed only on the original opaque upper edge of the logo.
        if rel=='title/minecraft':
            d=ImageDraw.Draw(im)
            for x in range(2,im.width-2):
                for y in range(min(20,im.height)):
                    if im.getpixel((x,y))[3]>220:
                        if (x//5)%4!=2 and y>0:d.point((x,y),fill='#edf8f4')
                        break
        save(rel,im)
        if entry+'.mcmeta' in z.namelist():(root/(rel+'.png.mcmeta')).write_bytes(z.read(entry+'.mcmeta'))
    # Second pass uses exact vanilla atlas geometry and explicit seasonal artwork.
    from winter import upgrade
    upgrade(root, z, a.out, a.download)
    z.close()
assert len(list(root.rglob('*.png')))>=8
print('Christmas UI sprites:',len(list(root.rglob('*.png'))))
