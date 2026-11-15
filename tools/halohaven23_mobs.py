#!/usr/bin/env python3
"""Halo Haven 2.3 creature art: semantic UV regions, recognizable material/markings,
regional anatomy instead of twenty low-information recolors. Authored 1px clusters."""
from pathlib import Path
from PIL import Image,ImageDraw
import random,math,tarfile,tempfile
B=Path("projects/halohaven-alpha21/bundle.tar.xz")
# Base, dorsal accent, ventral material, eye & identifying unique motif
SPECIES={
"sunpuff":("#c4bb90","#f0dab1","#f1e5cb","#3d463f","fleece"),
"dawnstag":("#ad9174","#efe3cf","#d8c3a5","#353d3a","fur"),
"pollenwing":("#af91b9","#e4b45f","#c6adc9","#494053","wing"),
"chalkshell":("#a4aead","#d3d6c5","#ded6c0","#475759","shell"),
"palewing":("#b8a9b9","#d8d9c8","#a5adb2","#434c59","wing"),
"rubblelurker":("#777c7f","#a9a8a5","#969da0","#29363c","rock"),
"crownfox":("#607e6c","#c7a96b","#afbf9a","#323d32","fur"),
"mossback":("#60765d","#8aab69","#a1b18a","#253e34","moss"),
"barkstalker":("#4b5f4e","#93a887","#728b68","#d4d9a4","bark"),
"chimewing":("#739ca1","#c3e3d8","#9cb8a3","#354d62","wing"),
"resonant":("#4d6574","#a6cdc4","#86a6aa","#d7e5c0","crystal"),
"cliffprowler":("#777986","#b7aaa1","#9a97a0","#e6c884","fur"),
"littlechime":("#d3c4ab","#f3dc9a","#dedace","#557b84","etched"),
"silentchorister":("#817d96","#c6a9bb","#bdafb2","#dfe7cd","cloth"),
"choir_sentinel":("#afa6ad","#e0d4be","#c1baba","#566978","stone"),
"veilmanta":("#4e5675","#75b5b7","#747993","#d0e2bd","ray"),
"shadowstalker":("#383b5b","#786b86","#5e5779","#b7cfd6","chitin"),
"memory_wisp":("#67969d","#9ddaca","#b1b9d4","#f5eab6","ghost"),
"cloud_wanderer":("#94aeba","#d8d7c5","#b5c9c4","#344957","whale"),
"sky_swallow":("#8ea2a3","#e6e1cd","#b5cec5","#283b47","wing")
}
REGIONS={
 "head":(0,0,42,28),"body":(0,32,98,38),
 "legs":(0,72,37,35),"wings":(38,72,77,34),
 "tail":(70,108,58,20),"special":(100,0,27,48)}
def rgb(h):return tuple(int(h[i:i+2],16) for i in (1,3,5))
def shade(c,n):return tuple(max(0,min(255,round(v*n))) for v in c)+(255,)
def ramp(hexcode):
    c=rgb(hexcode)
    return [shade(c,f) for f in (.57,.70,.83,.94,1.04,1.14,1.23)]
def draw_region(canvas,rect,material,colors,seed,region):
    x0,y0,w,h=rect;x1=x0+w-1;y1=y0+h-1
    d=ImageDraw.Draw(canvas)
    d.rectangle((x0,y0,x1,y1),fill=colors[3])
    d.line((x0,y0,x1,y0),fill=colors[5])
    d.line((x1,y0,x1,y1),fill=colors[1])
    d.line((x0,y1,x1,y1),fill=colors[1])
    # Local clusters: consistent 1-4 pixel forms instead of disconnected 1px speckle.
    for row in range(y0+2,y1-1,4):
        for col in range(x0+2,x1-1,5):
            variation=(col//5+row//4+seed)%7
            dx=(row+col+seed)%3
            dy=(col//3+seed)%2
            x=min(x1-3,col+dx-1);y=min(y1-2,row+dy-1)
            if variation<=1:fill=colors[2]
            elif variation<=3:fill=colors[4]
            elif variation==4:fill=colors[5]
            else:fill=colors[3]
            if material in ("fur","fleece","moss"):
                d.rectangle((x,y,x+2,y+1),fill=fill)
                d.point((x+1,y+2),fill=colors[2 if variation%2 else 4])
            elif material in ("rock","shell","stone","etched"):
                d.rectangle((x,y,x+3,y+1),fill=fill)
                d.line((x+1,y+2,x+3,y+2),fill=colors[2])
            elif material in ("bark","cloth"):
                d.line((x,y,x,y+2),fill=colors[2])
                d.line((x+2,y+1,x+3,y+2),fill=colors[5])
            elif material in ("chitin","crystal"):
                d.polygon(((x,y+1),(x+2,y),(x+3,y+1),(x+2,y+2)),fill=fill)
            else:
                d.line((x,y,x+3,y),fill=fill)
                d.point((x+2,y+1),fill=colors[5])
    # Material orientation and semantic shape details
    if material in ("bark","cloth"):
        for x in range(x0+6,x1-2,10):
            d.line((x,y0+2,x,y1-3),fill=colors[2])
            d.line((x+1,y0+4,x+1,y1-7),fill=colors[4])
    elif material in ("stone","rock","shell"):
        for i in range(3):
            x=x0+(i*13+seed)%max(4,w-9)
            y=y0+(i*7+3)%max(3,h-7)
            d.line((x,y,x+3,y+1,x+4,y+3),fill=colors[1],width=1)
    elif material in ("wing","ray"):
        for x in range(x0+7,x1-5,11):
            d.line((x,y0+3,x+4,y0+9),fill=colors[1])
            d.line((x+4,y0+9,x+2,min(y1-2,y0+20)),fill=colors[5])
    if region=="body":
        # Chest zone, underbelly and dorsal stripe are distinct to avoid a cuboid silhouette.
        d.rectangle((x0+12,y0+12,x0+25,min(y1-3,y0+21)),fill=colors[4])
        for x in range(x0+14,x0+26,4):
            d.line((x,y0+15,x+2,y0+15),fill=colors[5])
        for x in range(x0+38,x0+59,5):
            d.rectangle((x,y0+4,x+2,y0+5),fill=colors[1])
    if region=="wings":
        for i in range(4):
            x=x0+8+i*15
            d.line((x,y0+3,x+5,y0+12,x+1,y0+22),fill=colors[5])
            d.line((x+2,y0+12,x+8,y0+17),fill=colors[2])
    if region=="tail":
        for x in range(x0+4,x1-4,7):
            d.line((x,y0+2,x+3,y0+5,x+2,y0+13),fill=colors[5])
    if region=="legs":
        for x in range(x0+4,x1-1,9):
            d.line((x,y1-5,x+4,y1-5),fill=colors[1])
def make(spec,data,seed):
    base,accent,belly,eye,material=data
    img=Image.new("RGBA",(128,128),(0,0,0,0))
    p=ramp(base);a=ramp(accent);u=ramp(belly)
    for idx,(zone,rect) in enumerate(REGIONS.items()):
        color=p if zone in ("body","legs") else a if zone in ("wings","tail","special") else u
        draw_region(img,rect,material,color,seed+idx*7,zone)
    d=ImageDraw.Draw(img)
    eye_color=rgb(eye)+(255,)
    # UV head front surface: coordinates within head texOffs(0,0).
    for x in (13,19):
        d.rectangle((x,9,x+2,11),fill=p[0])
        d.rectangle((x+1,9,x+1,10),fill=eye_color)
        d.point((x+1,9),fill=u[6])
    # Lower muzzle marks, nose, jaw seam.
    d.rectangle((15,15,19,16),fill=p[2])
    d.point((17,15),fill=eye_color)
    if material=="shell":
        for x,y in ((15,43),(31,47),(50,43),(66,50)):
            d.polygon(((x,y),(x+5,y-2),(x+9,y),(x+6,y+4),(x+1,y+4)),outline=a[1],fill=a[4])
    if material=="moss":
        for x,y in ((15,41),(31,44),(49,40),(62,48)):
            d.rectangle((x,y,x+6,y+3),fill=a[3]);d.point((x+2,y-2),fill=a[5])
    if material=="crystal":
        for x,y in ((16,45),(40,48),(58,42)):
            d.polygon(((x,y+3),(x+3,y-4),(x+5,y+2),(x+2,y+6)),fill=a[5])
    if material=="etched":
        for x in range(10,75,16):
            d.line((x,47,x+3,43,x+6,47),fill=a[5],width=1)
    if material in ("ray","whale"):
        for x in range(44,113,12):
            d.line((x,79,x+4,82,x+2,91),fill=a[5])
    if material=="cloth":
        for x in (15,25,37,55,70):
            d.line((x,44,x+2,61),fill=a[1],width=2)
        for x in (22,31,47,64):
            d.point((x,47),fill=a[5])
    if material=="fleece":
        for x,y in ((5,38),(15,42),(25,41),(36,46),(50,44),(66,41)):
            d.rectangle((x,y,x+5,y+2),fill=u[5])
    if material=="chitin":
        for x in range(8,80,15):
            d.line((x,44,x+5,49,x,55),fill=a[1])
    if material in ("fur","rock","stone","bark","whale"):
        for x in range(10,80,14):
            d.line((x,39,x+4,39),fill=a[5],width=2)
    return img

with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    with tarfile.open(B,"r:xz") as z:
        for entry in z:
            if entry.isfile():
                rel=Path(entry.name); assert rel.parts[0]=="src" and ".." not in rel.parts
                dst=root/rel;dst.parent.mkdir(parents=True,exist_ok=True)
                dst.write_bytes(z.extractfile(entry).read())
    sprites=root/"src/main/resources/assets/halohavenalpha2/textures/entity"
    for i,(name,data) in enumerate(SPECIES.items()):
        sprite=sprites/(name+".png")
        assert sprite.exists(),sprite
        make(name,data,i*19+7).save(sprite)
        assert Image.open(sprite).size==(128,128)
    # Extra anatomy for the creatures criticized as featureless boxes:
    # these details use the unused 100,0 sheet region and don't alter hitboxes.
    model=root/"src/main/java/dev/halohaven/alpha2/client/HavenClient.java"
    text=model.read_text(encoding="utf8")
    needle="            return LayerDefinition.create(mesh,128,128);"
    assert needle in text
    extra="""            if(s.form==HavenSpecies.Form.WHALE){
                add(body,"dorsalRidge",CubeListBuilder.create().texOffs(100,0)
                    .addBox(-3,-3,-6,6,4,12),0,-bh/2,0);
                add(root,"jaw",CubeListBuilder.create().texOffs(100,24)
                    .addBox(-6,-2,-5,12,3,9),0,by+bh/2-2,-bd/2);
            }
            if(s.form==HavenSpecies.Form.MANTA){
                add(body,"mantle",CubeListBuilder.create().texOffs(100,0)
                    .addBox(-6,-2,-6,12,3,12),0,-bh/2,0);
            }
            if(s.form==HavenSpecies.Form.BIRD){
                add(root,"beak",CubeListBuilder.create().texOffs(100,24)
                    .addBox(-1,-1,-3,2,2,3),0,headY,headZ-hd/2);
            }
            if(s.form==HavenSpecies.Form.GOLEM){
                for(int i=-1;i<=1;i++)
                    add(body,"chestPlate"+i,CubeListBuilder.create().texOffs(100,24)
                        .addBox(-2,-3,-1,4,6,2),i*4,0,-bd/2);
            }
            if(s.form==HavenSpecies.Form.STALKER){
                add(root,"cloakHood",CubeListBuilder.create().texOffs(100,0)
                    .addBox(-5,-5,-4,10,4,8),0,headY-3,headZ);
            }
"""
    text=text.replace(needle,extra+needle)
    # Move tail UV into a dedicated region (in 2.2 it overlapped the wing sprite).
    text=text.replace('CubeListBuilder.create().texOffs(0,100)','CubeListBuilder.create().texOffs(70,108)')
    model.write_text(text,encoding="utf8")
    with tarfile.open(B,"w:xz",preset=6) as z:
        for file in sorted((root/"src").rglob("*")):
            if file.is_file():z.add(file,arcname=file.relative_to(root).as_posix())
print("20 individually painted 128×128 creature skins, semantic UV materials, and extra anatomy completed")
