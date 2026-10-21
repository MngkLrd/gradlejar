#!/usr/bin/env python3
"""Software rasterizer for the actual Minecraft JSON cuboids and their atlas UVs.
No generated image inputs are read. 3D preview rendered from shipped gameplay model JSON.
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import numpy as np, cv2, json, math, re
ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/norsegear'
ATLAS=np.array(Image.open(ASSET/'textures/item/norse_showcase_materials.png').convert('RGBA'))
# Each face list uses consistent quad 3D vertices, UV mapping based on Minecraft face uv.
def faces(fr,to):
 x0,y0,z0=fr;x1,y1,z1=to
 return {
 'south':[(x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)],
 'north':[(x1,y0,z0),(x0,y0,z0),(x0,y1,z0),(x1,y1,z0)],
 'east':[(x1,y0,z1),(x1,y0,z0),(x1,y1,z0),(x1,y1,z1)],
 'west':[(x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)],
 'up':[(x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0)],
 'down':[(x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1)]}
normal={'south':(0,0,1),'north':(0,0,-1),'east':(1,0,0),'west':(-1,0,0),'up':(0,1,0),'down':(0,-1,0)}
shade={'south':1,'north':.67,'east':.70,'west':.88,'up':1.12,'down':.52}
fontbase='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'
font=ImageFont.truetype(fontbase,24);small=ImageFont.truetype(fontbase,15)

def render(name,az=35,elev=18,w=420,h=410,zoom=1.0,data=None,texture=None):
 d=data if data is not None else json.loads((ASSET/'models/item'/f'{name}.json').read_text()); els=d['elements']
 atlas=ATLAS if texture is None else texture
 # Camera: x positive, z positive, y positive; 3/4 shows Z front and X sides.
 aa=math.radians(az);ee=math.radians(elev)
 v=np.array([math.sin(aa)*math.cos(ee),math.sin(ee),math.cos(aa)*math.cos(ee)])
 front=-v;right=np.cross(front,[0,1,0]);right/=np.linalg.norm(right)
 up=np.cross(right,front);up/=np.linalg.norm(up)
 # image coordinate x right & y down
 basis=np.stack([right,-up,front],axis=0)
 allp=np.array([p for e in els for p in (e['from'],e['to'])],float)
 mins=allp.min(axis=0);maxs=allp.max(axis=0);center=(maxs+mins)/2
 delta=allp-center;proj=delta@basis.T
 maxdim=max(np.ptp(proj[:,0]),np.ptp(proj[:,1]));scale=min((w-70)/maxdim,(h-70)/maxdim)*zoom
 rgb=np.zeros((h,w,4),np.uint8);depth=np.full((h,w),1e9,np.float32)
 # rgba alpha fill omitted, cards draw own background
 light=np.array([-0.3,.7,.6]);light/=np.linalg.norm(light)
 for el in els:
  vs=faces(el['from'],el['to'])
  for direction,vertices in vs.items():
   norm=np.array(normal[direction],float)
   if norm@v<=0.000001:continue # hidden faces
   pts=np.array(vertices,dtype=float);tv=(pts-center)@basis.T
   xy=tv[:,:2]*scale+np.array([w/2,h/2]); zz=tv[:,2]
   f=el['faces'].get(direction)
   if f is None:continue
   u0,v0,u1,v1=f['uv'];uv=np.array([[u0,v1],[u1,v1],[u1,v0],[u0,v0]],dtype=float)*16
   # Minecraft UV uses 0..16 grid; atlas 256px => 16px per uv unit.
   illumination=(.72+.28*max(0,float(norm@light)))*shade[direction]
   illumination=min(1.3,max(.38,illumination))
   for ids in ((0,1,2),(0,2,3)):
    tri=xy[list(ids)].astype(np.float32); z=zz[list(ids)].astype(np.float32); tex=uv[list(ids)]
    xmin=max(0,int(np.floor(tri[:,0].min())));xmax=min(w-1,int(np.ceil(tri[:,0].max())))
    ymin=max(0,int(np.floor(tri[:,1].min())));ymax=min(h-1,int(np.ceil(tri[:,1].max())))
    if xmin>=xmax or ymin>=ymax:continue
    xx,yy=np.meshgrid(np.arange(xmin,xmax+1),np.arange(ymin,ymax+1))
    A=tri[0];B=tri[1];C=tri[2]
    denom=(B[1]-C[1])*(A[0]-C[0])+(C[0]-B[0])*(A[1]-C[1])
    if abs(denom)<1e-4:continue
    wa=((B[1]-C[1])*(xx-C[0])+(C[0]-B[0])*(yy-C[1]))/denom
    wb=((C[1]-A[1])*(xx-C[0])+(A[0]-C[0])*(yy-C[1]))/denom
    wc=1-wa-wb
    mask=(wa>=-0.0005)&(wb>=-0.0005)&(wc>=-0.0005)
    zzpix=wa*z[0]+wb*z[1]+wc*z[2]
    destdepth=depth[ymin:ymax+1,xmin:xmax+1]
    mask &= zzpix<destdepth-0.00004
    if not mask.any():continue
    uvpix=wa[...,None]*tex[0]+wb[...,None]*tex[1]+wc[...,None]*tex[2]
    xi=np.clip(uvpix[...,0].astype(int),0,255);yi=np.clip(uvpix[...,1].astype(int),0,255)
    sampled=atlas[yi,xi]
    mask &= sampled[:,:,3]>10
    out=rgb[ymin:ymax+1,xmin:xmax+1]
    out[mask,:3]=np.clip(sampled[mask,:3].astype(float)*illumination,0,255).astype('uint8')
    out[mask,3]=255
    destdepth[mask]=zzpix[mask]
 return Image.fromarray(rgb,'RGBA')

def card(base,pos,name,subtitle,az=35,elev=18,w=440,h=480):
 x,y=pos;d=ImageDraw.Draw(base)
 d.rounded_rectangle((x,y,x+w,y+h),radius=15,fill='#18232f',outline='#3b536a',width=2)
 g=render(name,az=az,elev=elev,w=w-12,h=h-85,zoom=.91)
 base.alpha_composite(g,(x+6,y+10))
 d.text((x+20,y+h-77),name.replace('_',' ').upper(),font=font,fill='#e9f2f8')
 d.text((x+20,y+h-40),subtitle,font=small,fill='#9bb3c2')

def load_equipped_armor_from_java():
    # Read actual Minecraft Java ModelPart geometry, rather than re-draw a costume.
    source=(ROOT/'src/main/java/dev/norsegear/client/model/NorseArmorModel.java').read_text()
    pattern=r'(head|body|root\.getChild\("(?:left_arm|right_arm|left_leg|right_leg)"\))\.addOrReplaceChild\("([^"]+)", CubeListBuilder\.create\(\)\.texOffs\((\d+),\s*(\d+)\)\s*\.addBox\(([^)]*)\)'
    armor=Image.new('RGBA',(256,256),(0,0,0,0))
    img=Image.open(ASSET/'textures/models/armor/ironweave_layer_1.png').convert('RGBA')
    armor.paste(img,(0,0))
    dr=ImageDraw.Draw(armor)
    dr.rectangle((180,180,195,195),fill='#242d37')
    dr.line((180,180,195,180),fill='#536376')
    mask={'head':'head','body':'body'}
    els=[]
    def box_from(name,fr,to,faceUV=None):
       if faceUV is None:faceUV={dir:{'uv':[180/16,180/16,196/16,196/16],'texture':'#tex'} for dir in normal}
       els.append({'name':name,'from':fr,'to':to,'faces':faceUV})
    for parent,name,u,v,argstr in re.findall(pattern,source):
       u=float(u);v=float(v)
       values=[float(n.replace('F','')) for n in re.findall(r'-?\d+(?:\.\d+)?F?',argstr)]
       if len(values)!=6:continue
       x,y,z,dx,dy,dz=values
       ox=oy=0
       if 'right_arm' in parent:ox=-5;oy=2
       if 'left_arm' in parent:ox=5;oy=2
       if 'left_leg' in parent:ox=-1.9;oy=12
       if 'right_leg' in parent:ox=1.9;oy=12
       fr=[8+x+ox,15-(y+dy+oy),8-(z+dz)]
       to=[8+x+dx+ox,15-(y+oy),8-z]
       # UV derived from the same CubeListBuilder texOffs used in Java.
       def rect(x0,y0,w,h):return [x0/16,y0/16,(x0+w)/16,(y0+h)/16]
       faceUV={
          'west':{'uv':rect(u,v+dz,dz,dy),'texture':'#tex'},
          'north':{'uv':rect(u+dz,v+dz,dx,dy),'texture':'#tex'},
          'east':{'uv':rect(u+dz+dx,v+dz,dz,dy),'texture':'#tex'},
          'south':{'uv':rect(u+2*dz+dx,v+dz,dx,dy),'texture':'#tex'},
          'up':{'uv':rect(u+dz,v,dx,dz),'texture':'#tex'},
          'down':{'uv':rect(u+dz+dx,v,dx,dz),'texture':'#tex'},
       }
       box_from(name,fr,to,faceUV)
    # underlying mannequin: skin remains visibly exposed through open-face helm
    box_from('mannequin-head',[4,15,4],[12,23,12])
    box_from('mannequin-torso',[4,3,6],[12,15,10])
    box_from('mannequin-left-arm',[12,3,6],[16,13,10])
    box_from('mannequin-right-arm',[0,3,6],[4,13,10])
    box_from('mannequin-left-leg',[8,-9,6],[12,3,10])
    box_from('mannequin-right-leg',[4,-9,6],[8,3,10])
    assert len(els)>25,('Java armor parsing failed',len(els))
    return {'elements':els},np.array(armor),len(els)-6

if __name__=='__main__':
 canvas=Image.new('RGBA',(1450,1140),'#0e1720')
 dr=ImageDraw.Draw(canvas)
 dr.text((36,18),'NORSE GEAR 0.3.1 / ACTUAL 3D MODEL PREVIEW',font=ImageFont.truetype(fontbase,28),fill='#ebf2f7')
 dr.text((36,57),'Rendered from shipped item JSON + UV material atlas · not AI-generated artwork',font=small,fill='#9fb4c2')
 card(canvas,(35,100),'bearded_axe','front / 3D cuboid model',w=450,h=475)
 card(canvas,(500,100),'dane_axe','front / 3D cuboid model',w=450,h=475)
 card(canvas,(965,100),'spear','front / 3D cuboid model',w=450,h=475)
 card(canvas,(35,595),'round_shield','front / painted face',az=8,elev=14,w=450,h=470)
 card(canvas,(500,595),'round_shield','back / grip',az=168,elev=17,w=450,h=470)
 card(canvas,(965,595),'javelin','front / 3D cuboid model',az=35,elev=22,w=450,h=470)
 dr.text((35,1092),'Source: assets/norsegear/models/item/*.json + textures/item/norse_showcase_materials.png',font=small,fill='#93acbe')
 dst=ROOT/'PREVIEW_3D_ACTUAL_031.png';canvas.convert('RGB').save(dst)
 print('Rendered actual JSON models:',dst)
 model,atex,count=load_equipped_armor_from_java()
 preview=Image.new('RGBA',(1030,570),'#0e1720');d=ImageDraw.Draw(preview)
 d.text((25,20),'EQUIPPED ARMOR / JAVA MODELPART GEOMETRY',font=ImageFont.truetype(fontbase,26),fill='#e8f3f9')
 d.text((25,65),f'{count} actual armor cubes parsed from NorseArmorModel.java. Texturing from ironweave_layer_1.png.',font=small,fill='#abc0cd')
 p=render('armor',data=model,texture=atex,az=-30,elev=15,w=480,h=450,zoom=.95)
 q=render('armor',data=model,texture=atex,az=25,elev=10,w=480,h=450,zoom=.95)
 preview.alpha_composite(p,(25,90));preview.alpha_composite(q,(525,90))
 ImageDraw.Draw(preview).text((30,542),'Actual Java geometry, not a generated concept',font=small,fill='#a7bac8')
 armorpath=ROOT/'PREVIEW_ARMOR_ACTUAL_031.png';preview.convert('RGB').save(armorpath)
 print('Rendered equipped armor Java source:',armorpath)

