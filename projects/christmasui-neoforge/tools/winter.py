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
        edge='#ffda86' if hover else '#29251e';wood='#8c5d32' if hover else ('#56534a' if off else '#68503b');light='#c28c54' if hover else '#826247';dark='#694227' if hover else '#483a2c'
        d.rectangle((0,1,199,18),fill=edge);d.rectangle((1,0,198,19),fill=edge)
        d.rectangle((1,2,198,17),fill=dark);d.rectangle((2,3,197,15),fill=wood)
        d.line((3,16,196,16),fill='#e6b36b' if hover else '#8e6f50')
        if hover:
            d.line((1,3,1,16),fill='#fff0ba')
            d.line((198,3,198,16),fill='#e6b36b')
            d.line((2,18,197,18),fill='#ffd77d')
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
    # Spruce cabinet: same material ramps as normal buttons and the hotbar.
    # Retain vanilla slot geometry, light-facing rims, and readable dark screen labels.
    palette={0:'#29251e',31:'#382b23',55:'#483a2c',85:'#68503b',139:'#44382d',198:'#b59672',255:'#e6c69c'}
    def panel(im):
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,a=im.getpixel((x,y))
                if not a or max(r,g,b)-min(r,g,b)>8:continue
                v=(r+g+b)//3;k=min(palette,key=lambda k:abs(k-v))
                im.putpixel((x,y),(*bytes.fromhex(palette[k][1:]),a))
        return im
    # These ornaments are manually specified pixel clusters, not randomized noise.
    branch=[
        '....g.......g....',
        '...gg......gg....',
        'g..gg.....glg....',
        '.gglgg...glg.....',
        '..gglgg.ggg..g...',
        '...ggggrrgggg....',
        '....ggrrrlgg.....',
        '.....grryggggg...',
        '......gggg..ggg..',
        '.......ggg....gg.',
        '.........gg......',
    ]
    festive={'g':'#284438','l':'#648957','r':'#a63432','y':'#ef8a6d'}
    bell=[
        '...o...',
        '..oho..',
        '..oyoo.',
        '.ohyy o'.replace(' ',''),
        '.oyyso.',
        'oyyyyso',
        'ooooooo',
        '...s...',
    ]
    gold={'o':'#654126','h':'#fff1bc','y':'#dab16a','s':'#a27137'}
    def safe_draw(im,source,paint):
        # Restrict interior ornamentation to the original flat panel material.
        overlay=Image.new('RGBA',im.size)
        paint(overlay,ImageDraw.Draw(overlay))
        pix=overlay.load();src=source.load()
        for y in range(im.height):
            for x in range(im.width):
                if pix[x,y][3] and src[x,y][3] and src[x,y][0:3] in ((198,198,198),(197,197,197)):
                    im.putpixel((x,y),pix[x,y])
    def grain(d,x,y,w):
        d.line((x,y,x+w,y),fill='#9a7653')
        if w>5:d.line((x+2,y+1,x+w-2,y+1),fill='#c6a57c')
    def inventory_art(im,d):
        # Rails between storage rows and the top crafting area, clear of the item wells.
        for x,y,w in [(4,5,16),(82,18,10),(82,37,8),(143,55,20),(94,72,8),(128,74,15),(8,79,29),(47,79,31),(106,79,27),(146,79,20),(12,138,30),(52,138,37),(110,138,40),(10,161,30),(47,161,25),(96,161,35),(143,161,22)]:
            grain(d,x,y,w)
        # Tall evergreen ornament in the divider between the character and crafting slots.
        pixels(im,branch,festive,73,16)
        pixels(im,[r[::-1] for r in branch],festive,74,33)
        pixels(im,bell,gold,84,49)
        # A small hanging ribbon and bell in the open space under the craft grid.
        d.line((150,55,150,62),fill='#86392f')
        d.line((149,55,151,55),fill='#c45542')
        pixels(im,bell,gold,147,63)
        pixels(im,branch,festive,128,62)
        # Paired carved motifs on the lower rail, not on item squares.
        for x in (44,86,128):
            d.polygon([(x,137),(x+2,135),(x+4,137),(x+2,139)],fill='#765338')
            d.point((x+2,137),fill='#e1bb7e')
    for n in names:
        if not n.startswith(prefix) or not n.endswith('.png'):continue
        rel=n[len(prefix):-4]
        if not (rel.startswith('container/') or rel.startswith('sprites/container/creative_inventory/')):continue
        src=original(rel)
        im=panel(src.copy());d=ImageDraw.Draw(im)
        if rel=='container/inventory':
            safe_draw(im,src,inventory_art)
            d=ImageDraw.Draw(im)
            # Keep snowy caps outside armor and crafting cells.
            for x,w,deep in [(3,23,4),(36,18,3),(72,19,4),(132,22,4),(163,11,3)]:
                snow(d,x,0,w,deep)
            # Brass corner pins and carved wooden perimeter.
            for x,y in [(3,9),(172,9),(3,156),(172,156)]:
                d.point((x,y),fill='#edca8a')
                d.point((x,y+1),fill='#896036')
            d.line((3,11,3,153),fill='#826247')
            d.line((172,11,172,153),fill='#68503b')
            # Edge ornamentation stays within the lower frame; leave all slots alone.
            for x,y,flip in [(3,157,False),(163,157,True)]:
                pixels(im,[r[::-1] for r in sprig] if flip else sprig,sp,x,y)
            d.line((13,164,162,164),fill='#68503b')
        elif rel.startswith('container/creative_inventory/'):
            def creative_art(ov,od):
                for x,y,w in [(9,5,22),(48,5,18),(112,5,25),(148,5,23),(8,130,25),(42,130,31),(102,130,32),(145,130,27)]:
                    grain(od,x,y,w)
                pixels(ov,branch,festive,4,136)
                pixels(ov,[r[::-1] for r in branch],festive,173,136)
            safe_draw(im,src,creative_art)
            d=ImageDraw.Draw(im)
            for x,w in [(5,21),(35,12),(158,21)]:
                if x<im.width and src.getpixel((x,1))[3]>0:snow(d,x,0,w,3)
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
        im=original('sprites/hud/'+rel);pix=im.load()
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,a=pix[x,y]
                if a and max(r,g,b)-min(r,g,b)<15:
                    v=(r+g+b)//3;col=palette[min(palette,key=lambda k:abs(k-v))]
                    pix[x,y]=(*bytes.fromhex(col[1:]),a)
        d=ImageDraw.Draw(im)
        snow(d,2,0,min(14,im.width-3),3);save('sprites/hud/'+rel,im)
    # Requested first choice: Winter GUI Pack's original snowy sunset cubemap.
    # Only extract panorama faces, never replace our UI, music, or logo.
    if download:
        from PIL import ImageStat
        api='https://api.modrinth.com/v2/'
        project=json.loads(get(api+'project/ScxwaXQI'))
        assert project['slug']=='winter-gui-pack',project['slug']
        assert project['license']['id']=='CC-BY-4.0',project['license']
        versions=json.loads(get(api+'project/'+project['id']+'/version'))
        versions.sort(key=lambda v:('1.21.1' in v['game_versions'],v['date_published']),reverse=True)
        selected=None
        for version in versions:
            for f in sorted(version['files'],key=lambda x:not x.get('primary',False)):
                if not f['filename'].lower().endswith('.zip'):continue
                data=get(f['url'])
                assert hashlib.sha512(data).hexdigest()==f['hashes']['sha512']
                with zipfile.ZipFile(io.BytesIO(data)) as pack:
                    faces=[]
                    for i in range(6):
                        target='panorama_'+str(i)+'.png'
                        candidates=[n for n in pack.namelist() if n.endswith('/title/background/'+target)]
                        if len(candidates)!=1:break
                        im=Image.open(io.BytesIO(pack.read(candidates[0]))).convert('RGB')
                        if im.width!=im.height or im.width<256 or max(ImageStat.Stat(im).stddev)<3:break
                        faces.append(im.copy())
                    if len(faces)==6 and len({im.size for im in faces})==1 and len({hashlib.sha256(im.tobytes()).hexdigest() for im in faces})==6:
                        selected=(version,f,faces);break
            if selected:break
        assert selected,'Winter GUI Pack has no usable six-face cubemap; do not silently publish another panorama'
        version,f,faces=selected
        for i,im in enumerate(faces):
            save('title/background/panorama_'+str(i),im.convert('RGBA'))
        members=json.loads(get(api+'team/'+project['team']+'/members'))
        authors=', '.join(m['user']['username'] for m in members)
        assert authors,'Missing panorama attribution'
        (out/'PANORAMA-CREDITS.txt').write_text('Winter GUI Pack by '+authors+'\nSource: https://modrinth.com/resourcepack/winter-gui-pack\nVersion: '+version['version_number']+'\nOriginal archive: '+f['url']+'\nCC BY 4.0 https://creativecommons.org/licenses/by/4.0/\nChanges: only six panorama images extracted; converted to RGBA PNG for Minecraft 1.21.1; scene unmodified.\n',encoding='utf-8')
        print('Verified Winter GUI Pack cubemap:',faces[0].size,'source',version['version_number'],'author',authors)
        # Required music: abort the build rather than publish a silent archive.
        url='https://upload.wikimedia.org/wikipedia/commons/4/4f/Jingle_Bells_%28Calm%29_%28Kevin_MacLeod%29_%28ISRC_USUAN1100188%29.oga'
        raw=get(url);assert hashlib.sha1(raw).hexdigest()=='df5c3c90f70da45bfd8bf7b39a0c300ac99cfb06'
        assert raw.startswith(b'OggS')
        mc=out/'assets/minecraft';snd=mc/'sounds/music/christmas_calm.ogg';snd.parent.mkdir(parents=True,exist_ok=True);snd.write_bytes(raw)
        events=['music.menu','music.game','music.creative','music.under_water','music.overworld.badlands','music.overworld.bamboo_jungle','music.overworld.cherry_grove','music.overworld.deep_dark','music.overworld.desert','music.overworld.dripstone_caves','music.overworld.flower_forest','music.overworld.forest','music.overworld.frozen_peaks','music.overworld.grove','music.overworld.jagged_peaks','music.overworld.jungle','music.overworld.lush_caves','music.overworld.meadow','music.overworld.old_growth_taiga','music.overworld.snowy_slopes','music.overworld.sparse_jungle','music.overworld.stony_peaks','music.overworld.swamp']
        # Each game event uses the same five-track pool; menu remains a single dedicated track.
        from urllib.parse import quote
        tracks=[
            ('silent_night','Silent Night','Silent Night (Kevin MacLeod) (ISRC USUAN1100075).oga','3.0'),
            ('deck_the_halls','Deck the Halls A','Deck the Halls A (Kevin MacLeod) (ISRC USUAN1100263).oga','2.0'),
            ('oh_christmas_tree','Oh Christmas Tree','Oh Christmas Tree.ogg','2.0'),
            ('we_wish_you','We Wish You a Merry Christmas','We Wish you a Merry Christmas (Kevin MacLeod ) (ISRC USUAN1100369).oga','3.0'),
            ('what_child_is_this','What Child Is This','What Child is This (Kevin MacLeod) (ISRC USUAN1100012).oga','3.0'),
        ]
        credits=['Jingle Bells (Calm) - Kevin MacLeod (incompetech.com)\nCC BY 3.0 https://creativecommons.org/licenses/by/3.0/\nSource: https://commons.wikimedia.org/wiki/File:Jingle_Bells_(Calm)_(Kevin_MacLeod)_(ISRC_USUAN1100188).oga\nUnmodified recording; filename changed.\n']
        pool=[]
        hashes={hashlib.sha256(raw).hexdigest()}
        for key,title,filename,license_version in tracks:
            filename=filename.replace(' ','_')
            digest=hashlib.md5(filename.encode('utf-8')).hexdigest()
            url='https://upload.wikimedia.org/wikipedia/commons/'+digest[0]+'/'+digest[:2]+'/'+quote(filename,safe='')
            data=get(url)
            assert data.startswith(b'OggS') and b'\x01vorbis' in data[:512] and len(data)>100000, title
            digest=hashlib.sha256(data).hexdigest()
            assert digest not in hashes, 'Duplicate recording: '+title
            hashes.add(digest)
            path=mc/'sounds/music/christmas'/ (key+'.ogg')
            path.parent.mkdir(parents=True,exist_ok=True)
            path.write_bytes(data)
            pool.append({'name':'minecraft:music/christmas/'+key,'stream':True,'weight':1})
            credits.append(title+' - Kevin MacLeod (incompetech.com)\nCC BY '+license_version+' https://creativecommons.org/licenses/by/'+license_version+'/\nSource: https://commons.wikimedia.org/wiki/File:'+quote(filename,safe='')+'\nUnmodified recording; filename changed.\n')
            print('Verified music',title,len(data),'bytes',digest)
        sound_events={e:{'replace':True,'sounds':pool} for e in events if e!='music.menu'}
        sound_events['music.menu']={'replace':True,'sounds':[{'name':'minecraft:music/christmas_calm','stream':True}]}
        (mc/'sounds.json').write_text(json.dumps(sound_events,indent=2))
        (out/'MUSIC-CREDITS.txt').write_text('\n'.join(credits),encoding='utf-8')
