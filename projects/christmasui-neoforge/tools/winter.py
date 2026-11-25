from PIL import Image,ImageDraw
from pathlib import Path
import io,json,hashlib,urllib.request,zipfile
def upgrade(root,z,out,download):
    prefix='assets/minecraft/textures/gui/'
    names=set(z.namelist())
    def save(rel,im):
        f=root/(rel+'.png');f.parent.mkdir(parents=True,exist_ok=True);im.save(f)
        m=prefix+rel+'.png.mcmeta'
        if m in names:(root/(rel+'.png.mcmeta')).write_bytes(z.read(m))
    def original(rel):
        return Image.open(io.BytesIO(z.read(prefix+rel+'.png'))).convert('RGBA')
    def get(url):
        req=urllib.request.Request(url,headers={'User-Agent':'ChristmasUI/0.3.0 (https://github.com/MngkLrd/gradlejar)'})
        with urllib.request.urlopen(req,timeout=120) as r:return r.read()
    def pixels(im,rows,pal,x,y):
        d=ImageDraw.Draw(im)
        for j,row in enumerate(rows):
            for i,c in enumerate(row):
                if c in pal and 0<=x+i<im.width and 0<=y+j<im.height:d.point((x+i,y+j),fill=pal[c])
    sprig=['..g..g....','..gg.g....','gglggg....','.ggglgg...','..gggrrg..','...ggrrlg.','...g.ggggg','.....g.gg.','......g...']
    sp={'g':'#2b5038','l':'#6b9761','r':'#bc4439'}
    def snow(d,x,y,w,deep=3):
        if w<5:return
        d.line((x+1,y,x+w-2,y),fill='#bed6dc')
        d.rectangle((x,y+1,x+w-1,y+2),fill='#f5f8eb')
        d.line((x+1,y+3,x+w-3,y+3),fill='#c3d9dc')
        d.line((x+2,y+4,x+4,y+4),fill='#dcebe9')
        if deep>3:
            d.line((x+w-5,y+3,x+w-4,y+5),fill='#dcebe9')
            d.point((x+w-5,y+6),fill='#b2c9d0')
    # Same palette across all GUI: spruce wood, muted pine, cream, berry.
    for rel in ['button','button_highlighted','button_disabled']:
        im=Image.new('RGBA',(200,20));d=ImageDraw.Draw(im)
        hover='highlighted' in rel;off='disabled' in rel
        edge='#29251e';wood='#68503b' if not off else '#56534a';light='#9e7952' if hover else '#826247';dark='#483a2c'
        d.rectangle((0,1,199,18),fill=edge);d.rectangle((1,0,198,19),fill=edge)
        d.rectangle((1,2,198,17),fill=dark);d.rectangle((2,3,197,15),fill=wood)
        d.line((3,16,196,16),fill='#ad885e' if hover else '#8e6f50')
        for x,y,w in [(10,9,19),(42,12,15),(71,7,15),(105,13,22),(146,10,19),(174,7,12)]:
            d.line((x,y,x+w,y),fill=dark);d.line((x+2,y+1,x+w-3,y+1),fill=light)
        for i,(x,w) in enumerate([(2,21),(33,13),(59,30),(103,19),(146,25),(184,14)]):
            snow(d,x,0,w,4 if i in (0,2,4) else 3)
        pixels(im,sprig,sp,1,7);pixels(im,[s[::-1] for s in sprig],sp,189,7)
        save('sprites/widget/'+rel,im)
        # Keep 0.2.0's custom scaling rather than vanilla's 3px cap.
        (root/('sprites/widget/'+rel+'.png.mcmeta')).write_text(json.dumps({'gui':{'scaling':{'type':'nine_slice','width':200,'height':20,'border':{'left':11,'right':11,'top':7,'bottom':4}}}}))
    # Artwork for all interactive sliders, locks and both join sprite families.
    for n in names:
        if not n.startswith(prefix) or not n.endswith('.png'):continue
        rel=n[len(prefix):-4]
        if rel.startswith(('sprites/world_list/','sprites/server_list/')) and 'join' in rel:
            im=original(rel);w,h=im.size
            im=Image.new('RGBA',(w,h));d=ImageDraw.Draw(im);s=min(w,h)/32
            def poly(points,c):d.polygon([(round(x*s),round(y*s)) for x,y in points],fill=c)
            poly([(6,3),(10,3),(26,16),(10,29),(6,29)],'#34271f')
            poly([(8,6),(22,16),(8,26)],'#b73539' if 'highlighted' not in rel else '#e25342')
            for i in range(3):
                yy=7+i*6
                poly([(8,yy),(min(19,13+i*2),yy+3),(min(22,15+i*2),yy+5),(8,yy+3)],'#f5eed9')
            poly([(8,6),(11,8),(8,10)],'#fff9e9')
            if 'marked' in rel:d.rectangle((1,1,max(2,round(4*s)),max(2,round(4*s))),fill='#e7bd63')
            save(rel,im)
        elif rel.startswith('sprites/widget/slider'):
            im=original(rel);w,h=im.size;im=Image.new('RGBA',(w,h));d=ImageDraw.Draw(im)
            if 'handle' in rel:
                d.rectangle((0,0,w-1,h-1),fill='#33291e');d.rectangle((1,1,w-2,h-2),fill='#b9453d' if 'highlighted' not in rel else '#e27353')
                for y in range(3,h-2,5):
                    d.line((1,y,w-2,min(h-2,y+2)),fill='#f2ead3',width=2)
                d.line((1,1,w-2,1),fill='#fff4d9')
            else:
                d.rectangle((0,0,w-1,h-1),fill='#302a22');d.rectangle((1,1,w-2,h-2),fill='#4b4937')
                d.rectangle((2,max(2,h//2-2),w-3,min(h-3,h//2+2)),fill='#273f35')
                d.line((2,h-3,w-3,h-3),fill='#92744e')
            save(rel,im)
        elif rel.startswith('sprites/widget/') and ('locked_button' in rel or 'unlocked_button' in rel):
            im=original(rel);w,h=im.size;d=ImageDraw.Draw(im)
            # Retain recognizable lock silhouette, replace its neutral material with brass and green.
            for y in range(h):
                for x in range(w):
                    r,g,b,a=im.getpixel((x,y))
                    if not a:continue
                    v=(r+g+b)//3
                    if v<65:c='#322b22'
                    elif v<120:c='#53684e'
                    elif v<185:c='#a88043'
                    else:c='#f2d38c'
                    im.putpixel((x,y),(*bytes.fromhex(c[1:]),a))
            if w>=16 and h>=16:snow(d,2,0,8,3)
            save(rel,im)
    # Distinct star-shaped peppermint HP tokens. Container, half, absorption, status and hardcore masks.
    star=['....o....','...olo...','...oloo..','ooorwrooo','.owrrwro.','..orwro..','..owrwo..','..oo.oo..','..o...o..']
    for n in names:
        if not n.startswith(prefix+'sprites/hud/heart/') or not n.endswith('.png'):continue
        rel=n[len(prefix):-4];key=rel.split('/')[-1]
        old=original(rel);im=Image.new('RGBA',old.size)
        if 'vehicle' in key:continue
        container='container' in key;half='half' in key;blink='blinking' in key;hard='hardcore' in key
        if 'poisoned' in key:base='#769a43';lit='#d5e5a4'
        elif 'withered' in key:base='#655d73';lit='#b6aabb'
        elif 'frozen' in key:base='#5899bf';lit='#ddf7ff'
        elif 'absorbing' in key:base='#d99b35';lit='#fff0a3'
        else:base='#d24c48';lit='#fff0d7'
        for y,row in enumerate(star):
            for x,k in enumerate(row):
                if k=='.' or x>=im.width or y>=im.height:continue
                if not container and half and x>=5:continue
                if container:c=('#a9bdb0' if blink else '#42544b') if k=='o' else '#292f2c'
                elif k=='o':c='#4f2527' if not hard else '#d89d53'
                elif k in 'wl':c=lit
                else:c=('#ed8d76' if blink and base=='#d24c48' else base)
                im.putpixel((x,y),(*bytes.fromhex(c[1:]),255))
        save(rel,im)
    # Inventory surfaces: warm parchment for vanilla dark labels; pine-green slot wells and red/brown framing.
    palette={0:'#29271f',31:'#39382a',55:'#433e2e',85:'#52604a',139:'#69765a',198:'#c8b68d',255:'#f0e1bd'}
    def panel(im):
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,a=im.getpixel((x,y))
                if not a or max(r,g,b)-min(r,g,b)>8:continue
                v=(r+g+b)//3;k=min(palette,key=lambda k:abs(k-v))
                im.putpixel((x,y),(*bytes.fromhex(palette[k][1:]),a))
        return im
    for n in names:
        if not n.startswith(prefix) or not n.endswith('.png'):continue
        rel=n[len(prefix):-4]
        if not (rel.startswith('container/') or rel.startswith('sprites/container/creative_inventory/')):continue
        im=panel(original(rel));d=ImageDraw.Draw(im)
        if rel=='container/inventory':
            # Header decoration stays above crafting/armor slots.
            for x,w in [(5,21),(40,17),(74,12),(135,24),(166,7)]:
                snow(d,x,0,w,3)
            for x,y,flip in [(5,158,False),(161,158,True),(83,4,False)]:
                pixels(im,[s[::-1] for s in sprig] if flip else sprig,sp,x,y)
            d.line((5,161,170,161),fill='#76553a')
        elif rel.startswith('container/creative_inventory/'):
            for x,w in [(5,21),(35,12),(158,21)]:
                if im.getpixel((x,1))[3]>0:snow(d,x,0,w,3)
        elif rel.startswith('sprites/container/creative_inventory/tab_'):
            if im.width>=12 and im.height>=10:
                d.line((3,2,im.width-4,2),fill='#e9f2e5')
                d.line((4,3,im.width-5,3),fill='#a6c5c4')
        save(rel,im)
    # Equipment and offhand placeholders are item-atlas sprites, not container PNGs.
    for n in names:
        if not n.startswith('assets/minecraft/textures/item/empty_armor_slot_') or not n.endswith('.png'):continue
        im=Image.open(io.BytesIO(z.read(n))).convert('RGBA')
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,a=im.getpixel((x,y))
                if not a:continue
                v=max(r,g,b);c='#dcc9a2' if v>95 else '#a18b65'
                im.putpixel((x,y),(*bytes.fromhex(c[1:]),a))
        f=out/n;f.parent.mkdir(parents=True,exist_ok=True);im.save(f)
    for rel in ['hotbar_offhand_left','hotbar_offhand_right']:
        im=panel(original('sprites/hud/'+rel));d=ImageDraw.Draw(im)
        snow(d,2,0,min(14,im.width-3),3);save('sprites/hud/'+rel,im)
    # Use the official, actual Minecraft 1.18 mountain-world cubemap, not a painted approximation.
    if download:
        man=json.loads(get('https://piston-meta.mojang.com/mc/game/version_manifest_v2.json'))
        info=json.loads(get(next(v['url'] for v in man['versions'] if v['id']=='1.18.2')))
        client=info['downloads']['client'];raw=get(client['url']);assert hashlib.sha1(raw).hexdigest()==client['sha1']
        with zipfile.ZipFile(io.BytesIO(raw)) as old:
            for i in range(6):
                rel='title/background/panorama_'+str(i)
                save(rel,Image.open(io.BytesIO(old.read(prefix+rel+'.png'))).convert('RGBA'))
        # Required music: abort the build rather than publish a silent archive.
        url='https://upload.wikimedia.org/wikipedia/commons/4/4f/Jingle_Bells_%28Calm%29_%28Kevin_MacLeod%29_%28ISRC_USUAN1100188%29.oga'
        raw=get(url);assert hashlib.sha1(raw).hexdigest()=='df5c3c90f70da45bfd8bf7b39a0c300ac99cfb06'
        assert raw.startswith(b'OggS')
        mc=out/'assets/minecraft';snd=mc/'sounds/music/christmas_calm.ogg';snd.parent.mkdir(parents=True,exist_ok=True);snd.write_bytes(raw)
        events=['music.menu','music.game','music.creative','music.under_water','music.overworld.badlands','music.overworld.bamboo_jungle','music.overworld.cherry_grove','music.overworld.deep_dark','music.overworld.desert','music.overworld.dripstone_caves','music.overworld.flower_forest','music.overworld.forest','music.overworld.frozen_peaks','music.overworld.grove','music.overworld.jagged_peaks','music.overworld.jungle','music.overworld.lush_caves','music.overworld.meadow','music.overworld.old_growth_taiga','music.overworld.snowy_slopes','music.overworld.sparse_jungle','music.overworld.stony_peaks','music.overworld.swamp']
        (mc/'sounds.json').write_text(json.dumps({e:{'replace':True,'sounds':[{'name':'minecraft:music/christmas_calm','stream':True}]} for e in events}))
        (out/'MUSIC-CREDITS.txt').write_text('Jingle Bells (Calm) - Kevin MacLeod (incompetech.com)\nCC BY 3.0 https://creativecommons.org/licenses/by/3.0/\nSource: https://commons.wikimedia.org/wiki/File:Jingle_Bells_(Calm)_(Kevin_MacLeod)_(ISRC_USUAN1100188).oga\nUnmodified recording; filename changed.\n')
