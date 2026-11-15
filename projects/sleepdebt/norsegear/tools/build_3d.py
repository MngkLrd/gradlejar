#!/usr/bin/env python3
"""Generate purposeful true cuboid models for every Norse Gear item.
No image-based downscaling, billboards, or opaque single-block fallbacks.
Materials are grouped into a hand-selected Minecraft-like texture palette.
"""
from pathlib import Path
from PIL import Image, ImageDraw
import json, math, collections
ROOT=Path(__file__).resolve().parents[1]
MODELS=ROOT/'src/main/resources/assets/norsegear/models/item'
TEX=ROOT/'src/main/resources/assets/norsegear/textures/item'
COLORS={
 'steel':('#9aa3a9','#c2cbd0','#626a74','#3e4650'),
 'steel_dark':('#65727f','#9ba6b0','#3a424f','#262d36'),
 'iron':('#7d909e','#bac8d0','#526373','#303945'),
 'edge':('#c4d4d8','#f0f7f6','#84959f','#697983'),
 'wood':('#a26832','#c58c48','#724522','#443121'),
 'wood_dark':('#734522','#ac6e32','#50341c','#2f2118'),
 'leather':('#a66c3f','#d1975d','#74452b','#462f26'),
 'leather_dark':('#643c2d','#a46b45','#43271f','#302020'),
 'bronze':('#b38b39','#e0bd60','#765625','#4b3724'),
 'gold':('#dfbb57','#fff0a0','#ac7a28','#704e1f'),
 'bone':('#e1d3a7','#fff1cd','#ad9c77','#796c55'),
 'stone':('#848b91','#b1b7bc','#636b70','#414a50'),
 'stone_light':('#a3a6a1','#dbd8cd','#71766f','#525956'),
 'green':('#49775b','#6a9870','#315440','#223c30'),
 'red':('#ab4840','#d36d51','#732f31','#492526'),
 'linen':('#ccaa7d','#efcda0','#96734e','#5a4736'),
 'food':('#bd7148','#e19b68','#823e31','#512d23'),
 'pitch':('#2c3b42','#576873','#1b252b','#131b22'),
 'rust':('#985e37','#c47c40','#694329','#452d22'),
 'hide':('#8f6b48','#b99368','#674c39','#423327'),
 'mail':('#8397a3','#b6c5d0','#516271','#313e49'),
 'glass':('#738f98','#a5c1c2','#4d6972','#344c55'),
 'water':('#4b757e','#78adb2','#355862','#23414c'),
}
# 64x64 atlas: 16 columns * 16 rows of 4px blocks. Three lightness steps by face.
ATLAS=Image.new('RGBA',(64,64),(0,0,0,0)); d=ImageDraw.Draw(ATLAS)
uvs={}; tile=0
for mat,shades in COLORS.items():
 for idx in (0,1,2,3):
  ix=tile%16;iy=tile//16;assert iy<16, tile
  x,y=ix*4,iy*4
  c=shades[idx];d.rectangle((x,y,x+3,y+3),fill=c)
  # Occasional purposeful two-pixel cluster: highlights restricted to natural materials.
  if idx==0 and mat in ('steel','edge','bone','stone','glass','bronze','mail'):
   from PIL import ImageColor
   hi=ImageColor.getrgb(shades[1]);base=ImageColor.getrgb(c)
   mix=tuple(round(hi[z]*.62+base[z]*.38) for z in range(3))+(255,)
   d.line((x,y,x+1,y),fill=mix)
  uvs[(mat,idx)]=[x/4,y/4,x/4+1,y/4+1];tile+=1
ATLAS.save(TEX/'norse_3d_materials.png')
# face lighting controlled by palette mapping, no AO-fighting black outlines
DEFAULT_DISPLAY={
 'gui':{'rotation':[30,225,0],'translation':[0,0,0],'scale':[0.92,0.92,0.92]},
 'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[0.5,0.5,0.5]},
 'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[0.85,0.85,0.85]},
 'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[0.75,0.75,0.75]},
 'thirdperson_lefthand':{'rotation':[0,0,0],'translation':[0,2,1],'scale':[0.75,0.75,0.75]},
 'firstperson_righthand':{'rotation':[0,0,0],'translation':[1.13,3.2,1.13],'scale':[0.8,0.8,0.8]},
 'firstperson_lefthand':{'rotation':[0,0,0],'translation':[-1.13,3.2,1.13],'scale':[0.8,0.8,0.8]},
}
# Full 3D model composed of cuboids: one JSON model for each of 24 items.
class Model:
 def __init__(self,id,kind='held'):
  self.id=id;self.elements=[];self.kind=kind
 def box(self,a,b,mat,note='',emphasis=None):
  assert all(-0.01<=x<=16.01 for x in a+b),(self.id,a,b)
  assert all(float(a[i])<float(b[i]) for i in range(3)),(self.id,a,b)
  faces={}
  for face in ('north','south','east','west','up','down'):
   idx={'north':2,'south':0,'east':2,'west':1,'up':1,'down':3}[face]
   if emphasis and face in emphasis:idx=emphasis[face]
   faces[face]={'texture':'#mat','uv':uvs[mat,idx]}
  self.elements.append({'from':list(a),'to':list(b),'faces':faces})
  return self
 def b(self,x0,y0,z0,x1,y1,z1,mat):return self.box((x0,y0,z0),(x1,y1,z1),mat)
 def save(self):
  disp=json.loads(json.dumps(DEFAULT_DISPLAY))
  if self.kind=='held':
   disp['gui']['rotation']=[20,225,-5];disp['gui']['scale']=[0.94]*3
   disp['thirdperson_righthand']['rotation']=[0,-90,55]
   disp['thirdperson_lefthand']['rotation']=[0,90,-55]
   disp['firstperson_righthand']['rotation']=[0,-90,25]
   disp['firstperson_lefthand']['rotation']=[0,90,-25]
  if self.kind=='shield':
   disp['gui']['rotation']=[20,35,0];disp['gui']['scale']=[.86]*3
   disp['thirdperson_lefthand']={'rotation':[0,-90,0],'translation':[3.0,4.0,3.0],'scale':[.9,.9,.9]}
   disp['thirdperson_righthand']={'rotation':[0,90,0],'translation':[-3.0,4.0,3.0],'scale':[.9,.9,.9]}
   disp['firstperson_lefthand']={'rotation':[0,-25,0],'translation':[1.6,3,2.0],'scale':[.91,.91,.91]}
   disp['firstperson_righthand']={'rotation':[0,25,0],'translation':[-1.6,3,2.0],'scale':[.91,.91,.91]}
  if self.id in ('spear','javelin','throwing_axe'):
   # Flight renderer uses ItemDisplayContext.FIXED; keep shaft aligned along Y.
   disp['fixed']={'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}
  d={'textures':{'mat':'norsegear:item/norse_3d_materials','particle':'norsegear:item/norse_3d_materials'},'elements':self.elements,'display':disp,'ambientocclusion':True}
  (MODELS/(self.id+'.json')).write_text(json.dumps(d,indent=2)+'\n')
  assert len(self.elements)>=3,(self.id,len(self.elements))
  return {'name':self.id,'elements':len(self.elements),'materials':len({mat for mat in COLORS})}

def handle(m,y0=0.6,y1=9.9,x=7.5,depth=.9):
 m.b(x,y0,7.6,x+1,y1,8.4,'wood')
 m.b(x-.13,y0+.2,7.5,x+1.13,y0+.65,8.5,'leather')
 for h in (2.3,4.4,6.7):
  if h<y1-.5:m.b(x-.12,h,7.53,x+1.12,h+.28,8.47,'leather_dark')

def sword(id,short=False):
 m=Model(id,'held');handle(m,.75,5.5)
 m.b(7.2,.65,7.4,8.8,1.4,8.6,'bronze')
 m.b(5.3,5.2,7.35,10.7,6.15,8.65,'bronze')
 m.b(7.1,5.9,7.45,8.9,14.1 if not short else 12.5,8.55,'steel')
 m.b(7.75,6.1,8.57,8.25,13.4 if not short else 11.8,8.75,'edge')
 if short:
  m.b(7.25,12.5,7.5,8.9,13.35,8.5,'edge');m.b(7.25,13.35,7.55,8.0,14.0,8.4,'edge')
 else:
  m.b(7.4,14.1,7.55,8.6,14.65,8.45,'edge');m.b(7.65,14.65,7.7,8.35,15.3,8.3,'edge')
 return m

def axe(id,type='bearded',throw=False):
 m=Model(id,'held');handle(m,.5,14 if not throw else 12.4)
 m.b(7.05,11.1 if not throw else 9.7,7.15,9.8,14.3 if not throw else 12.6,8.8,'steel_dark')
 top=14.6 if not throw else 13.0
 if type=='bearded':
  m.b(9.3,12.4 if not throw else 11.15,7.25,13.3,top,8.75,'steel')
  m.b(12.7,11.0 if not throw else 9.75,7.36,14.3,top-.3,8.64,'steel')
  m.b(12.3,8.85 if not throw else 8.0,7.4,13.4,11.15 if not throw else 9.95,8.6,'steel')
  m.b(13.6,9 if not throw else 8.2,7.47,14.35,top-.2,8.53,'edge')
 else:
  m.b(9.0,12.6,7.2,12.2,15.1,8.8,'steel')
  m.b(11.8,12.05,7.25,14.4,15.0,8.75,'steel')
  m.b(13.9,10.3,7.39,15.1,14.65,8.61,'steel')
  m.b(14.7,11.1,7.46,15.25,14.8,8.54,'edge')
 return m

def spear(id,jav=False):
 m=Model(id,'held');length=10.75 if jav else 10.3
 m.b(7.52,.5,7.52,8.48,length,8.48,'wood')
 m.b(7.4,2.2,7.43,8.6,2.6,8.57,'leather_dark')
 m.b(7.39,length-.5,7.39,8.61,length+.5,8.61,'bronze')
 m.b(7.2,length+.4,7.3,8.8,length+2.5,8.7,'steel')
 m.b(6.75,length+1.55,7.5,9.25,length+3.0,8.5,'steel')
 m.b(7.2,length+2.9,7.6,8.8,length+3.75,8.4,'edge')
 m.b(7.7,length+3.75,7.76,8.3,length+4.4,8.24,'edge')
 return m

def shield(id):
 m=Model(id,'shield');paint='green' if id=='celtic_shield' else ('red' if id=='round_shield' else 'wood')
 # Scalloped 8-strip disk, 3D thickness; face features actually separate cubes.
 profile=[(1.5,5.25,10.75),(3.2,3.7,12.3),(5.0,2.4,13.6),(7,1.9,14.1),(9,1.9,14.1),(11,2.4,13.6),(12.8,3.7,12.3),(14.5,5.25,10.75)]
 for y,x0,x1 in profile:
  if y==14.5:hi=14.9
  else:hi=min(y+1.65,15.25)
  m.b(x0,y,6.7,x1,hi,9.2,'wood_dark')
  m.b(x0+.18,y+.08,9.21,x1-.18,hi-.08,9.58,paint if y%2<1.5 else 'wood')
 # Thin metallic rim segments at perimeter; no opaque 1px black outlines.
 for y,x0,x1 in profile:
  hi=14.9 if y==14.5 else min(y+1.65,15.25)
  m.b(x0,y,9.6,min(x0+.34,x1),hi,9.9,'steel')
  m.b(max(x1-.34,x0),y,9.6,x1,hi,9.9,'steel')
 m.b(7.05,6.8,9.67,8.95,9.2,10.5,'iron')
 m.b(7.42,7.1,10.5,8.58,8.9,10.85,'edge')
 if id=='celtic_shield':
  m.b(4.1,6.8,9.61,5,9.3,9.82,'bronze')
  m.b(11,6.8,9.61,11.9,9.3,9.82,'bronze')
 if id=='bossed_shield':
  m.b(7.2,3.0,9.65,8.8,12.8,9.94,'bronze')
 return m

def helmet():
 m=Model('norse_helmet');
 m.b(3.3,10.5,4.1,12.7,13.15,11.9,'steel');m.b(4,12.9,4.7,12,14.75,11.4,'steel')
 m.b(4.2,8.0,4.3,5.3,10.5,11.7,'iron');m.b(10.7,8,4.3,11.8,10.5,11.7,'iron')
 m.b(3.5,9.85,3.85,12.5,10.5,12.15,'edge');m.b(7.52,6.9,3.55,8.48,10.15,4.25,'bronze')
 m.b(5.0,7.6,10.5,11.0,8.5,12,'leather_dark')
 return m

def chest():
 m=Model('norse_chestplate');m.b(4.6,4,6.1,11.4,12.2,10.3,'mail')
 m.b(3,8.2,6.2,4.6,12,10.2,'steel');m.b(11.4,8.2,6.2,13,12,10.2,'steel')
 m.b(4,11.5,6.1,12,12.65,10.6,'leather')
 m.b(4.5,4.2,5.8,11.5,5.4,10.75,'leather_dark')
 m.b(7.1,4.1,5.65,8.9,5.7,5.95,'bronze')
 m.b(5.5,7,5.68,6.25,11,6.1,'leather');m.b(9.75,7,5.68,10.5,11,6.1,'leather')
 return m

def legging():
 m=Model('norse_leggings');m.b(4.2,10.9,6.4,11.8,13.4,9.8,'leather_dark')
 for x in [4.4,9.1]:
  m.b(x,3,6.4,x+2.5,10.9,9.6,'mail')
  m.b(x-.15,3.5,6.2,x+2.8,5.0,9.85,'leather')
 m.b(4.3,10,6,11.7,10.8,10.2,'bronze')
 return m

def boots():
 m=Model('norse_boots')
 for x in [3.4,8.95]:
  m.b(x,3,6.0,x+3.0,8.6,9.7,'leather')
  m.b(x-.3,2.4,4.7,x+3.3,4.3,10.1,'wood_dark')
  m.b(x,7.0,5.9,x+3.0,7.6,9.85,'bronze')
 return m

def horn(id):
 m=Model(id)
 # stepped, gently bowed horn pointing up/right, hollow mouth distinct from a cube
 points=[(3,2,5.2,5.3),(4.4,4,6.5,7.2),(6.0,6.7,8.7,9.5),(8.1,9.5,11.1,12.4),(10,11.6,13.15,14)]
 for x0,y0,x1,y1 in points:m.b(x0,y0,7.1,x1,y1,8.95,'bone')
 m.b(2.4,1.7,6.9,5.6,3.0,9.1,'bronze')
 m.b(9.6,11.5,6.85,13.2,14.15,9.05,'leather_dark' if id=='war_horn' else 'bronze')
 m.b(10.35,12.95,7.05,12.8,14.55,8.95,'leather_dark')
 return m

def sling():
 m=Model('sling');
 for x0,y0,x1,y1 in [(4,3,5,7),(5,7,6,10),(6,10,7,12.7),(11,3,12,7),(10,7,11,10),(9,10,10,12.7)]:m.b(x0,y0,7.8,x1,y1,8.2,'leather')
 m.b(6.6,2.6,7.3,9.6,4.5,8.7,'leather_dark')
 m.b(7.0,2.9,8.71,9.0,4.0,8.85,'leather')
 return m

def pebble():
 m=Model('stone_shot')
 m.b(6,3.5,6.6,10,5.7,10.4,'stone')
 m.b(4.65,5.3,5.9,11.35,10.2,10.95,'stone_light')
 m.b(5.6,10.0,6.75,10.5,12.4,10.3,'stone')
 m.b(6.1,7.1,5.4,9.4,9.8,5.9,'stone')
 m.b(7.1,11.1,8.4,9.4,12.6,10.4,'stone_light')
 return m

def whetstone():
 m=Model('field_whetstone');m.b(4.0,4.5,6.5,12,10.6,9.5,'stone')
 m.b(4.4,10.35,6.75,11.7,11.1,9.25,'stone_light')
 m.b(3.6,4.5,6.25,12.4,5.2,9.75,'leather')
 return m

def ration():
 m=Model('travel_ration');m.b(4,5,5.2,12,11,10.8,'food')
 m.b(3.9,6.4,5,12.1,7.35,11,'linen')
 m.b(4.2,9.6,4.85,11.8,10.55,11.15,'linen')
 m.b(5,5.4,10.81,8,9.1,11.3,'food')
 return m

def iron(id):
 m=Model(id)
 if id=='forged_iron':
  m.b(2.8,4,5.6,13.2,9.5,10.4,'iron')
  m.b(3.4,9.4,6.1,12.6,10.35,9.9,'edge')
  m.b(2.75,4,5.55,13.25,4.65,10.45,'steel_dark')
 else:
  m.b(3.2,3.6,4.5,12.8,10,11.8,'rust')
  m.b(4.4,9.7,4.8,11.1,11.8,10.7,'stone')
  m.b(5,5.5,11.82,7,7,12.05,'bronze')
  m.b(9,6.5,11.82,11,8.2,12.05,'rust')
 return m

def binding():
 m=Model('leather_binding')
 m.b(4,4.5,5.8,12,11.1,10.2,'leather_dark')
 m.b(3.4,5,5.2,12.6,6.4,10.8,'leather')
 m.b(4.1,8.8,5.15,11.9,10.2,10.85,'leather')
 m.b(11.7,3.9,6.8,13.7,7,9.2,'leather')
 return m

def flask():
 m=Model('pitch_flask')
 m.b(4.0,3.3,5.2,12,10.5,10.8,'glass')
 m.b(4.5,3.9,5.0,11.5,9.2,11,'pitch')
 m.b(6.8,10.4,6.55,9.2,12,9.45,'bone')
 m.b(6.3,12,6.3,9.7,13,9.7,'leather_dark')
 m.b(4,5.2,10.8,12,6,11,'leather')
 return m

# 24 unique real cuboid geometries, split logically (no all-one-cube stand-ins).
models=[sword('seax',True),axe('bearded_axe'),axe('dane_axe','dane'),sword('viking_sword'),spear('spear'),spear('javelin',True),axe('throwing_axe','bearded',True),shield('round_shield'),sling(),pebble(),shield('celtic_shield'),shield('bossed_shield'),helmet(),chest(),legging(),boots(),whetstone(),horn('war_horn'),ration(),flask(),iron('bog_iron'),iron('forged_iron'),binding(),horn('carved_horn')]
assert len(models)==24
reports=[m.save() for m in models]
(ROOT/'3D_MODEL_REPORT.json').write_text(json.dumps(reports,indent=2)+'\n')
print('GENERATED',len(models),'models',sum(r['elements'] for r in reports),'cuboids',len(COLORS),'material groups, 64x64 atlas')
