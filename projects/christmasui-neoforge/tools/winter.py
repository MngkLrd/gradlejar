from PIL import Image,ImageDraw
from pathlib import Path
import math,random,io,json,hashlib,urllib.request,time

def upgrade(root,z,out,download):
    def save(rel,im):
        p=root/(rel+'.png');p.parent.mkdir(parents=True,exist_ok=True);im.save(p)
    def original(rel):
        return Image.open(io.BytesIO(z.read('assets/minecraft/textures/gui/'+rel+'.png'))).convert('RGBA')
    # Palette preserves the slot wells, black text contrast, and vanilla UVs.
    ramp={0:'#28251e',55:'#3f3026',85:'#564031',139:'#72523a',198:'#d2b589',255:'#f4e5c9'}
    def panel(im):
        im=im.convert('RGBA')
        for y in range(im.height):
            for x in range(im.width):
                r,g,b,a=im.getpixel((x,y))
                if not a or max(r,g,b)-min(r,g,b)>8:continue
                v=(r+g+b)//3
                k=min(ramp,key=lambda k:abs(k-v))
                if v<12 and a==255: color=(30,25,22)
                else:color=tuple(bytes.fromhex(ramp[k][1:]))
                im.putpixel((x,y),(*color,a))
        return im
    def sprig(d,x,y,flip=1):
        for dx,dy in [(0,0),(1,0),(2,1),(3,1),(4,2),(5,2),(6,3),(7,3),(8,4),(3,2),(3,3),(2,4),(5,3),(5,4),(4,5),(7,4),(7,5),(6,6)]:
            d.point((x+dx*flip,y+dy),fill='#284938')
        for dx,dy in [(0,0),(2,1),(4,2),(6,3),(2,3),(4,4),(6,5)]:
            d.point((x+dx*flip,y+dy),fill='#527d53')
        for dx,dy in [(4,1),(5,1),(5,2)]:
            d.point((x+dx*flip,y+dy),fill='#b33b37')
        d.point((x+4*flip,y+1),fill='#e07156')
    for n in z.namelist():
        pref='assets/minecraft/textures/gui/'
        if not n.startswith(pref) or not n.endswith('.png'):continue
        rel=n[len(pref):-4]
        if rel.startswith('container/') or rel.startswith('sprites/container/creative_inventory/'):
            im=panel(original(rel));d=ImageDraw.Draw(im)
            # Draw only in verified opaque perimeter, never over slot coordinates.
            if rel=='container/inventory':
                for x,w in [(5,13),(24,8),(117,12),(152,19)]:
                    d.line((x,1,x+w,1),fill='#eaf4ed');d.line((x+1,2,x+w-2,2),fill='#a8c7ca')
                sprig(d,5,157);sprig(d,170,157,-1)
            elif rel.startswith('container/creative_inventory/'):
                for x,w in [(6,12),(34,18),(151,15),(179,10)]:
                    if im.getpixel((x,1))[3]>0:d.line((x,1,min(im.width-1,x+w),1),fill='#eaf4ed')
            save(rel,im)
        elif rel.startswith('sprites/hud/heart/'):
            im=original(rel);pix=im.load()
            # Every heart state gets a cold outline and a warm candy highlight; alpha and fill masks unchanged.
            for y in range(im.height):
                for x in range(im.width):
                    r,g,b,a=pix[x,y]
                    if not a:continue
                    if max(r,g,b)<85:
                        pix[x,y]=(44,72,79,a) if y<5 else (37,38,45,a)
                    elif min(r,g,b)>185:
                        pix[x,y]=(244,250,239,a)
                    elif r>g*1.4 and r>b*1.3:
                        pix[x,y]=(min(255,r+12),min(255,g+19),min(255,b+22),a)
            if 'container' not in rel:
                for x,y in [(1,1),(2,0),(5,0),(6,1)]:
                    if x<im.width and y<im.height and pix[x,y][3] and max(pix[x,y][:3])>85:
                        pix[x,y]=(220,241,242,pix[x,y][3])
            save(rel,im)
        elif rel.startswith('sprites/server_list/') and ('join' in rel):
            im=original(rel);pix=im.load()
            for y in range(im.height):
                for x in range(im.width):
                    r,g,b,a=pix[x,y]
                    if a:
                        v=max(r,g,b)
                        pix[x,y]=((236,213,141,a) if v>160 else (46,75,57,a) if v>75 else (38,34,27,a))
            save(rel,im)
    # Wide, wraparound pixel-art winter panorama: mountains, fir forest, cabins and frozen foreground.
    W,H=1536,768; scene=Image.new('RGB',(W,H),'#22364d');d=ImageDraw.Draw(scene)
    for y0,y1,c in [(110,185,'#30465c'),(185,250,'#455b6c'),(250,310,'#62788a'),(310,350,'#82969f'),(350,390,'#b1b9ba'),(390,767,'#c0d2d4')]:
        d.rectangle((0,y0,W-1,y1),fill=c)
    rng=random.Random(381)
    for i in range(125):
        x=rng.randrange(W);y=rng.randrange(80,275)
        d.point((x,y),fill=rng.choice(['#d7e3dd','#a9c2ce','#829da9']))
    for shift in [-W,0,W]:
        for i,(x,peak,w) in enumerate([(0,256,176),(156,235,206),(340,275,165),(493,219,220),(700,265,150),(840,234,210),(1050,270,170),(1210,221,230),(1430,266,180)]):
            x+=shift
            pts=[(x-w,410),(x-w//3,peak+55),(x,peak),(x+w//4,peak+30),(x+w,410)]
            d.polygon(pts,fill='#70899b')
            d.polygon([(x,peak),(x+w//4,peak+30),(x+w,410),(x+w//5,410)],fill='#536c83')
            d.polygon([(x-w//3,peak+55),(x,peak),(x+w//4,peak+30),(x+w//2,peak+80),(x+w//5,peak+61),(x-8,peak+41),(x-26,peak+68),(x-w//4,peak+67)],fill='#d7e5e5')
    def tree(x,y,h,back=False):
        s=max(7,h//4); trunk='#514139' if not back else '#596869'
        d.rectangle((x-2,y-h//4,x+2,y),fill=trunk)
        dark,light,snow=('#355354','#587776','#c1d5d8') if back else ('#254c48','#39675a','#edf3e9')
        for k in range(4):
            yy=y-h+k*h//6; half=s*(k+2)//4
            d.polygon([(x,yy),(x-half,yy+h//3),(x+half,yy+h//3)],fill=dark)
            d.polygon([(x,yy),(x-half,yy+h//3),(x,yy+h//4)],fill=light)
            d.polygon([(x,yy),(x-half//2,yy+h//6),(x-2,yy+h//7),(x+half//2,yy+h//5)],fill=snow)
    for i in range(95):
        x=i*17;y=418+rng.randrange(-8,14);tree(x,y,rng.randrange(26,72),True)
    # Foreground snowbanks and frozen stream.
    d.polygon([(0,430),(160,420),(310,446),(500,426),(650,445),(850,420),(1000,450),(1210,422),(1380,446),(1535,430),(1535,767),(0,767)],fill='#e2ebe9')
    d.polygon([(0,480),(210,468),(480,493),(630,466),(860,480),(1000,466),(1260,490),(1535,480),(1535,767),(0,767)],fill='#d1e0e3')
    d.polygon([(0,540),(250,510),(420,535),(710,520),(900,550),(1170,526),(1535,540),(1535,580),(1250,563),(980,589),(740,559),(460,573),(250,550),(0,580)],fill='#91b4c3')
    for x,y,w,h in [(125,430,48,37),(195,436,56,43),(720,436,62,40),(815,442,47,34),(1280,439,55,39)]:
        d.rectangle((x,y-h,x+w,y),fill='#694b35');d.rectangle((x+w-10,y-h,x+w,y),fill='#493a31')
        for yy in range(y-h+8,y,7):d.line((x,yy,x+w-11,yy),fill='#865f40')
        d.polygon([(x-6,y-h),(x+w//2,y-h-30),(x+w+6,y-h)],fill='#edf3ee')
        d.polygon([(x+w//2,y-h-30),(x+w+6,y-h),(x+w-2,y-h+5),(x+w//2,y-h-20)],fill='#b2c7cd')
        d.rectangle((x+8,y-h+13,x+17,y-h+23),fill='#e0a652');d.line((x+12,y-h+13,x+12,y-h+23),fill='#83522f')
        d.rectangle((x+w//2,y-18,x+w//2+9,y),fill='#352e29')
        d.rectangle((x+w-20,y-h-23,x+w-13,y-h-9),fill='#61514b');d.line((x+w-20,y-h-23,x+w-13,y-h-23),fill='#f4f5ec')
    for x,y,h in [(34,500,116),(330,484,92),(401,507,141),(575,468,76),(960,497,113),(1090,510,139),(1430,500,117),(1490,475,80)]:
        tree(x,y,h)
    # Match left and right seam with identical narrow snow field and horizon.
    for y in range(H):
        for i in range(4):scene.putpixel((W-1-i,y),scene.getpixel((0,y)))
    src=scene.load()
    N=256
    for face in range(6):
        im=Image.new('RGB',(N,N));p=im.load()
        for y in range(N):
            v=2*(y+.5)/N-1
            for x in range(N):
                u=2*(x+.5)/N-1
                X,Y,Z=[(u,-v,1),(1,-v,-u),(-u,-v,-1),(-1,-v,u),(u,1,v),(u,-1,-v)][face]
                lon=math.atan2(X,Z);lat=math.atan2(Y,math.hypot(X,Z))
                sx=int((lon/(2*math.pi)+.5)*W)%W;sy=max(0,min(H-1,int((.5-lat/math.pi)*H)))
                p[x,y]=src[sx,sy]
        save('title/background/panorama_'+str(face),im.resize((512,512),Image.Resampling.NEAREST))
    # Verified redistributable Ogg Vorbis menu track, never silently substitute a different recording.
    if download:
        url='https://upload.wikimedia.org/wikipedia/commons/4/4f/Jingle_Bells_%28Calm%29_%28Kevin_MacLeod%29_%28ISRC_USUAN1100188%29.oga'
        req=urllib.request.Request(url,headers={'User-Agent':'ChristmasUI/0.2.0 (Minecraft resource pack build; https://github.com/MngkLrd/gradlejar)'})
        try:
            with urllib.request.urlopen(req,timeout=90) as r:raw=r.read()
            assert hashlib.sha1(raw).hexdigest()=='df5c3c90f70da45bfd8bf7b39a0c300ac99cfb06'
            mc=out/'assets/minecraft';snd=mc/'sounds/music/menu/christmas_calm.ogg';snd.parent.mkdir(parents=True,exist_ok=True);snd.write_bytes(raw)
            (mc/'sounds.json').write_text(json.dumps({'music.menu':{'replace':True,'sounds':[{'name':'music/menu/christmas_calm','stream':True}]}}))
            (out/'MUSIC-CREDITS.txt').write_text('Jingle Bells (Calm) - Kevin MacLeod (incompetech.com)\nCC BY 3.0 https://creativecommons.org/licenses/by/3.0/\nSource: https://commons.wikimedia.org/wiki/File:Jingle_Bells_(Calm)_(Kevin_MacLeod)_(ISRC_USUAN1100188).oga\nUnmodified Ogg Vorbis recording; filename changed.\n')
        except Exception as e:
            print('Music download failed; no music packaged:',e)
