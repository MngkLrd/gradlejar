#!/usr/bin/env python3
"""Production Wavefront OBJ mesh creation for NeoForge 1.21.1.
These textured meshes, not concept images, are consumed by the game's OBJ loader.
Coordinates in 0..1 model-space, normal MC display transforms.
"""
import math, json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSET=ROOT/'src/main/resources/assets/norsegear'
OUT=ASSET/'models/item'
OUT.mkdir(exist_ok=True, parents=True)
SHOW='norsegear:item/norse_showcase_materials'
# UV regions specified in 256x256 atlas pixel coordinates, top-left origin
UV={
'iron':(128,0,144,16), 'dark':(144,0,160,16),'light':(160,0,176,16),
'edge':(176,0,192,16),'trim':(192,0,208,16),'etch':(208,0,224,16),
'socket':(224,0,240,16),'boss':(240,0,256,16),
'wood':(128,16,144,32),'wood_light':(144,16,160,32),'wood_end':(160,16,176,32),
'wood_dark':(176,16,192,32),'leather':(192,16,208,32),'leather_wrap':(208,16,224,32),
'bronze':(128,32,144,48),'rivet':(160,32,176,48),'ridge':(176,32,192,48),
'front':(0,0,64,64),'back':(64,0,128,64)
}

class Mesh:
 def __init__(self,name):self.name=name;self.vertices=[];self.uvs=[];self.faces=[]
 def face(self,points,mat,uvs=None):
  if uvs is None:
   x0,y0,x1,y1=UV[mat];u=(x0+2)/256;v=(y0+2)/256;U=(x1-2)/256;V=(y1-2)/256
   uvs=([(u,v),(U,v),(U,V),(u,V)] if len(points)==4 else [(u,v),(U,v),(u,V)])
  a=[]
  for (x,y,z),(u,v) in zip(points,uvs):
   self.vertices.append((x/16,y/16,z/16));self.uvs.append((u,v));a.append(len(self.vertices))
  # vertices are not shared to support UV seams and per-face normal
  self.faces.append(a)
 def triangle(self,a,b,c,mat,uvs=None):self.face([a,b,c],mat,uvs)
 def quad(self,a,b,c,d,mat,uvs=None):self.face([a,b,c,d],mat,uvs)
 def box(self,lo,hi,mat):
  x0,y0,z0=lo;x1,y1,z1=hi
  quads=[((x0,y0,z1),(x1,y0,z1),(x1,y1,z1),(x0,y1,z1)),
         ((x1,y0,z0),(x0,y0,z0),(x0,y1,z0),(x1,y1,z0)),
         ((x0,y0,z0),(x0,y0,z1),(x0,y1,z1),(x0,y1,z0)),
         ((x1,y0,z1),(x1,y0,z0),(x1,y1,z0),(x1,y1,z1)),
         ((x0,y1,z1),(x1,y1,z1),(x1,y1,z0),(x0,y1,z0)),
         ((x0,y0,z0),(x1,y0,z0),(x1,y0,z1),(x0,y0,z1))]
  for q in quads:self.face(q,mat)
 def ring(self,cx,cy,r0,r1,z0,z1,mat,segments=40):
  # annular bevel in XY for shield front metal rim
  for i in range(segments):
   a=2*math.pi*i/segments;b=2*math.pi*(i+1)/segments
   def p(r,z,t):return (cx+r*math.cos(t),cy+r*math.sin(t),z)
   self.quad(p(r0,z1,a),p(r1,z1,a),p(r1,z1,b),p(r0,z1,b),mat)
   self.quad(p(r1,z0,a),p(r1,z0,b),p(r1,z1,b),p(r1,z1,a),'dark')
   self.quad(p(r0,z0,b),p(r0,z0,a),p(r0,z1,a),p(r0,z1,b),'trim')
 def cylinder(self,cx,cy,cz,rad,lo,hi,mat,segments=10,axis='y'):
  # y-axis cylinder for weapon shafts; z-axis for boss dome rivets
  def pos(t,h):
   a=2*math.pi*t/segments
   if axis=='y':return (cx+rad*math.cos(a),h,cz+rad*math.sin(a))
   return (cx+rad*math.cos(a),cy+rad*math.sin(a),h)
  for i in range(segments):
   j=(i+1)%segments
   self.quad(pos(i,lo),pos(j,lo),pos(j,hi),pos(i,hi),mat if i%4 else 'wood_light' if mat=='wood' else mat)
  centerlo=(cx,lo,cz) if axis=='y' else (cx,cy,lo)
  centerhi=(cx,hi,cz) if axis=='y' else (cx,cy,hi)
  for i in range(segments):
   j=(i+1)%segments
   self.triangle(centerlo,pos(j,lo),pos(i,lo),mat)
   self.triangle(centerhi,pos(i,hi),pos(j,hi),mat)
 def save(self,display):
  obj=OUT/(self.name+'.obj');mtl=OUT/(self.name+'.mtl')
  data=[f'# Norse Gear {self.name} real triangle mesh / source generated','mtllib '+self.name+'.mtl','o '+self.name]
  for xyz in self.vertices:data.append('v '+' '.join(f'{v:.6f}' for v in xyz))
  for uv in self.uvs:data.append('vt '+ ' '.join(f'{v:.6f}' for v in uv))
  data.append('usemtl norsegear_material')
  for ids in self.faces:data.append('f '+' '.join(f'{i}/{i}' for i in ids))
  obj.write_text('\n'.join(data)+'\n')
  mtl.write_text('newmtl norsegear_material\nKd 1.0 1.0 1.0\nKa 1.0 1.0 1.0\nillum 1\nmap_Kd #texture0\n')
  item={'loader':'neoforge:obj','model':f'norsegear:models/item/{self.name}.obj',
       'textures':{'texture0':SHOW,'particle':SHOW},
       'automatic_culling':False,'flip_v':True,'shade_quads':True,
       'display':display}
  (OUT/(self.name+'.json')).write_text(json.dumps(item,indent=2)+'\n')
  return len(self.faces)

def shield():
 name='round_shield';m=Mesh(name)
 # Front and back are real circular tessellated wood surfaces, not squared boxes.
 N=48;cx=cy=8
 def pt(a,r,z):return (cx+r*math.cos(a),cy+r*math.sin(a),z)
 for i in range(N):
  a=i*2*math.pi/N;b=(i+1)*2*math.pi/N
  def atlas_uv(point,front):
   x,y,z=point
   return ((4*x+(0 if front else 64))/256,(4*(16-y))/256)
  # domed wood slightly higher in center
  for r0,r1,z0,z1 in [(0,2.45,8.16,8.05),(2.45,5.6,8.05,8.0),(5.6,7.18,8.0,7.85)]:
   corners=[pt(a,r0,z0),pt(a,r1,z1),pt(b,r1,z1),pt(b,r0,z0)]
   m.face(corners,'front',[atlas_uv(q,True) for q in corners])
  back=[pt(b,0,6.8),pt(b,7.15,6.80),pt(a,7.15,6.8),pt(a,0,6.8)]
  m.face(back,'back',[atlas_uv(q,False) for q in back])
  # leather/wood thickness on edge
  m.quad(pt(a,7.17,6.8),pt(b,7.17,6.8),pt(b,7.17,7.9),pt(a,7.17,7.9),'wood_dark')
 # outer metal rim ring with stepped beveled lip
 m.ring(8,8,6.9,7.7,7.65,8.35,'trim',48)
 m.ring(8,8,7.23,7.62,8.35,8.52,'light',48)
 m.ring(8,8,6.92,7.20,8.42,8.47,'edge',48)
 # boss flange and stepped convex dome: actual curved ring geometry
 m.ring(8,8,2.0,2.9,8.18,8.75,'dark',32)
 for j,(r0,r1,z0,z1) in enumerate([
  (2.05,2.6,8.75,8.91),(1.60,2.05,8.91,9.42),(1.15,1.6,9.42,9.92),(.65,1.15,9.92,10.25),(0,.65,10.25,10.4)]):
  for i in range(32):
   a=2*math.pi*i/32;b=2*math.pi*(i+1)/32
   m.quad(pt(a,r0,z1),pt(a,r1,z0),pt(b,r1,z0),pt(b,r0,z1),'boss' if j>0 else 'socket')
 # 16 rivets on rim; small 8-sided domed bosses each
 for i in range(16):
  a=2*math.pi*i/16;xx=8+7.30*math.cos(a);yy=8+7.30*math.sin(a)
  m.cylinder(xx,yy,0,.21,8.50,8.66,'rivet',8,axis='z')
 # underside grip, raised back handle running across horizontal central axle
 m.box((4.4,7.4,6.1),(11.6,8.55,6.85),'wood_dark')
 m.box((6.58,7.2,5.65),(9.42,8.8,6.17),'leather_wrap')
 for xx in (4.4,11.2):
  m.box((xx,7.14,6.0),(xx+.45,8.9,6.93),'trim')
  m.box((xx+.11,7.9,5.91),(xx+.33,8.17,6.0),'rivet')
 display=json.loads((OUT/(name+'.json')).read_text())['display']
 display['gui']={'rotation':[18,40,0],'translation':[0,0,0],'scale':[.88,.88,.88]}
 display['thirdperson_lefthand']['translation']=[3,4,4.05]
 display['thirdperson_righthand']['translation']=[-3,4,4.05]
 return m.save(display)

def spear(name):
 m=Mesh(name)
 small=name=='javelin'
 y_end=11.5 if small else 11.0
 m.cylinder(8,0,8,.43 if small else .50,.4,y_end,'wood',12)
 # butt, leather grip and iron bands
 m.cylinder(8,0,8,.63,.50,.84,'socket',12)
 for y in [1.2,2.0,2.8,3.6,4.4,5.2]:
  m.cylinder(8,0,8,.51,y,y+.19,'leather_wrap',12)
 for y in [5.8,7.4,y_end-.48]:m.cylinder(8,0,8,.60,y,y+.22,'trim',12)
 m.cylinder(8,0,8,.62,y_end-.35,y_end+.62,'socket',12)
 m.cylinder(8,0,8,.72,y_end-.3,y_end-.12,'rivet',12)
 # Leaf blade: triangulated double-sided high-relief with a diamond ridge.
 base=y_end+.42
 profile=[(0,.45),(.45,.80),(1.1,1.35),(1.65,1.75),(2.05,1.85),(2.55,1.48),(3.1,1.05),(3.66,.55),(4.12,.12)]
 if small:profile=[(t*.88,w*.81) for t,w in profile]
 # front/back wedge profiles per segment (normal axis = Z)
 def v(x,y,z):return (8+x,y,z)
 for (t0,w0),(t1,w1) in zip(profile[:-1],profile[1:]):
  yy0,yy1=base+t0,base+t1
  # taper tips within [0,16]
  if yy1>15.96:break
  # two facets on each side with structural central spine
  for sign in (-1,1):
   a=v(0,yy0,8.77);b=v(sign*w0,yy0,8.0);c=v(sign*w1,yy1,8.0);d=v(0,yy1,8.77)
   m.quad(a,b,c,d,'light' if sign==-1 else 'iron')
   a2=v(0,yy0,7.45);b2=v(sign*w0,yy0,8.0);c2=v(sign*w1,yy1,8.0);d2=v(0,yy1,7.45)
   m.quad(d2,c2,b2,a2,'dark' if sign==1 else 'steel_edge' if False else 'edge')
  # edge faces at sides through front-back
  for sign in (-1,1):
   m.quad(v(sign*w0,yy0,7.97),v(sign*w0,yy0,8.04),v(sign*w1,yy1,8.04),v(sign*w1,yy1,7.97),'edge')
 # engraved tip collar below leaf
 m.cylinder(8,0,8,.77,base-.13,base+.26,'trim',12)
 display=json.loads((OUT/(name+'.json')).read_text())['display']
 display['gui']={'rotation':[18,35,-30],'translation':[0,0,0],'scale':[1,1,1]}
 display['fixed']={'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}
 return m.save(display)

if __name__=='__main__':
 summary={k:f() for k,f in [('round_shield',shield),('spear',lambda:spear('spear')),('javelin',lambda:spear('javelin'))]}
 (ROOT/'tools/obj_mesh_summary_032.json').write_text(json.dumps(summary,indent=2)+'\n')
 print('Created real NeoForge OBJ triangle meshes:',summary)
