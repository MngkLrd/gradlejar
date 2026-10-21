#!/usr/bin/env python3
"""Screenshot-like software preview of the exact OBJ+UV+PNG files shipped in JAR.
No image-generation or concept artwork used.
"""
import math, numpy as np, cv2
from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/norsegear'
ATLAS=np.asarray(Image.open(ASSET/'textures/item/norse_showcase_materials.png').convert('RGB'))
FONT='/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'

def load(name):
 vs=[];uv=[];tris=[]
 path=ASSET/'models/item'/f'{name}.obj'
 for line in path.read_text().splitlines():
  a=line.split();
  if not a:continue
  if a[0]=='v':vs.append([float(x)*16 for x in a[1:4]])
  if a[0]=='vt':uv.append([float(x) for x in a[1:3]])
  if a[0]=='f':
   f=[tuple(int(t) for t in x.split('/')[:2]) for x in a[1:]]
   for i in range(1,len(f)-1):tris.append([f[0],f[i],f[i+1]])
 return np.array(vs),np.array(uv),tris

def render(name,az=20,elev=12,w=410,h=420,zoom=.94):
 vs,uv,tris=load(name)
 aa=np.radians(az);ee=np.radians(elev)
 view=np.array([np.sin(aa)*np.cos(ee),np.sin(ee),np.cos(aa)*np.cos(ee)])
 front=-view;right=np.cross(front,[0,1,0]);right/=np.linalg.norm(right)
 up=np.cross(right,front);up/=np.linalg.norm(up)
 basis=np.stack([right,-up,front]);center=(vs.min(0)+vs.max(0))/2
 p=(vs-center)@basis.T
 scale=min((w-40)/np.ptp(p[:,0]),(h-45)/np.ptp(p[:,1]))*zoom
 xy=p[:,:2]*scale+np.array([w/2,h/2]);dep=p[:,2]
 dst=np.zeros((h,w,4),dtype=np.uint8);zbuf=np.full((h,w),1e9,dtype=np.float32)
 for tr in tris:
  ids=[t[0]-1 for t in tr];tids=[t[1]-1 for t in tr]
  src=vs[ids]
  cross=np.cross(src[1]-src[0],src[2]-src[0]);norm=np.linalg.norm(cross)
  if norm<1e-8:continue
  n=cross/norm
  # Obj faces sometimes winding wrong; double-sided allowed, use absolute normal.
  pt=xy[ids].astype(np.float32);z=dep[ids].astype(np.float32)
  tcoord=uv[tids]*256
  xmin=max(0,int(np.floor(pt[:,0].min())));xmax=min(w-1,int(np.ceil(pt[:,0].max())))
  ymin=max(0,int(np.floor(pt[:,1].min())));ymax=min(h-1,int(np.ceil(pt[:,1].max())))
  if xmax<=xmin or ymax<=ymin:continue
  xx,yy=np.meshgrid(np.arange(xmin,xmax+1),np.arange(ymin,ymax+1))
  a,b,c=pt;den=(b[1]-c[1])*(a[0]-c[0])+(c[0]-b[0])*(a[1]-c[1]);
  if abs(den)<1e-6:continue
  wa=((b[1]-c[1])*(xx-c[0])+(c[0]-b[0])*(yy-c[1]))/den
  wb=((c[1]-a[1])*(xx-c[0])+(a[0]-c[0])*(yy-c[1]))/den
  wc=1-wa-wb
  mask=(wa>=-.00001)&(wb>=-.00001)&(wc>=-.00001)
  depths=wa*z[0]+wb*z[1]+wc*z[2]
  layer=zbuf[ymin:ymax+1,xmin:xmax+1];mask &= depths<layer-.00001
  if not mask.any():continue
  tx=np.clip((wa*tcoord[0,0]+wb*tcoord[1,0]+wc*tcoord[2,0]).astype(int),0,255)
  ty=np.clip((wa*tcoord[0,1]+wb*tcoord[1,1]+wc*tcoord[2,1]).astype(int),0,255)
  sample=ATLAS[ty,tx]
  light=np.array([-.42,.72,.64]);light/=np.linalg.norm(light)
  intensity=.65+.35*max(0,abs(n@light))
  output=np.clip(sample.astype(float)*intensity,0,255).astype(np.uint8)
  dstcut=dst[ymin:ymax+1,xmin:xmax+1]
  dstcut[mask,:3]=output[mask];dstcut[mask,3]=255;layer[mask]=depths[mask]
 return Image.fromarray(dst,'RGBA')

if __name__=='__main__':
 out=Image.new('RGBA',(1290,920),'#111b25');dr=ImageDraw.Draw(out)
 f=ImageFont.truetype(FONT,25);sm=ImageFont.truetype(FONT,16)
 dr.text((30,25),'NORSE GEAR 0.3.2  |  REAL OBJ GAME MESHES',font=f,fill='#f2f5f7')
 dr.text((32,63),'Rendered from actual .obj / .mtl / 256px texture atlas: no generated concept images',font=sm,fill='#91adbe')
 for i,(name,az,label) in enumerate([
  ('round_shield',8,'SHIELD FRONT'),('round_shield',155,'SHIELD BACK'),('spear',36,'SPEAR 3D'),
  ('spear',4,'SPEAR PROFILE'),('javelin',38,'JAVELIN 3D'),('round_shield',55,'SHIELD 3/4'),
 ]):
  col=i%3;row=i//3;x=18+col*426;y=102+row*404
  dr.rounded_rectangle((x,y,x+410,y+390),radius=12,fill='#1a2b3a',outline='#52657b',width=2)
  preview=render(name,az=az,elev=15 if name=='round_shield' else 8,w=400,h=322)
  out.alpha_composite(preview,(x+5,y+5))
  dr.text((x+15,y+340),label,font=sm,fill='#f0f5fa')
 p=ROOT/'PREVIEW_OBJ_ACTUAL_032.png';out.convert('RGB').save(p);print('Rendered',p)
