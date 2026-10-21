#!/usr/bin/env python3
"""Norse Gear reference-driven 3D models. All meshes and preview read same JSON and atlas."""
from pathlib import Path
from PIL import Image,ImageDraw
import json,math,random
ROOT=Path(__file__).resolve().parents[1]
MOD=ROOT/'src/main/resources/assets/norsegear'
ITEM=MOD/'models/item';TEX=MOD/'textures/item'; ARMOR=MOD/'textures/models/armor'
ITEM.mkdir(parents=True,exist_ok=True);TEX.mkdir(parents=True,exist_ok=True)
VERSION='0.3.1'
ATLAS=Image.new('RGBA',(256,256),(0,0,0,0));D=ImageDraw.Draw(ATLAS)
# Structured 16px surfaces; deliberately grouped shading and construction, no random noise.
spec={
 'steel':('#8193a5','#c4d4dc','#43566a'), 'steel_dark':('#4e657b','#90a5b5','#273948'),
 'steel_light':('#b5cbd9','#f0f9fb','#728ba1'), 'steel_edge':('#c9d9e3','#fbfeff','#93b0c0'),
 'steel_trim':('#61798f','#c2d2de','#34485e'),'steel_etch':('#526f84','#acbfc9','#293f55'),
 'steel_socket':('#788b9c','#d3e3ea','#405263'), 'steel_boss':('#8ea8bd','#e1eff3','#405e77'),
 'wood':('#87502c','#c48b53','#4f301f'),'wood_light':('#a96e36','#e0a966','#63371e'),
 'wood_end':('#a4784d','#eac48d','#6b492b'), 'wood_dark':('#5d3925','#95673f','#38251e'),
 'leather':('#7c4932','#be7d52','#493025'),'leather_wrap':('#835039','#d09262','#442b23'),
 'leather_dark':('#5a3428','#905f45','#30201d'),'leather_trim':('#a46e45','#e4a66b','#68412e'),
 'bronze':('#ae8445','#efc679','#6b5236'), 'bronze_light':('#d0a75c','#ffdf94','#967043'),
 'rivet':('#94a9b7','#eff9f8','#506679'), 'steel_ridge':('#738eaa','#dcebf0','#3d526f'),
 'bone':('#d9c9a2','#fff1c9','#9c8668'), 'fur':('#b9a083','#e8d2ad','#78614e'),
 'mail':('#71899e','#c5d2db','#354a5a'), 'shield_wood':('#93623d','#cf9860','#573b29'),
 'paint_red':('#b34a3b','#de7660','#6e312e'), 'paint_cream':('#d4be93','#ffebbd','#9a815e'),
 'paint_green':('#54765e','#81a986','#344e3c'), 'steel_flat':('#698098','#b5c9d3','#34475b'),
 'iron_blue':('#7894ac','#d3e3ef','#3c5a6c'), 'engraved':('#435970','#8ca6b9','#1d3043'),
}
# 64x64 front shield at (0,0) and back at (64,0), materials in 16px cells from top-right and rows below.
materials={};slots=[(x,y) for y in range(0,256,16) for x in range(0,256,16) if not(y<64 and x<128)]
for (name,(base,highlight,shadow)),(px,py) in zip(spec.items(),slots):
 materials[name]=(px,py)
 D.rectangle((px,py,px+15,py+15),fill=base)
 # physical broad shadows with a clear highlight line and shadow edge
 D.rectangle((px,py+11,px+15,py+15),fill=shadow)
 D.rectangle((px,py,px+15,py+1),fill=highlight)
 D.line((px+1,py+2,px+1,py+11),fill=highlight)
 D.rectangle((px+12,py+3,px+15,py+10),fill=shadow)
 if name.startswith(('steel','iron')) or name in ('rivet','bronze','bronze_light','engraved'):
  D.rectangle((px+3,py+3,px+10,py+6),fill=base)
  D.line((px+4,py+3,px+9,py+3),fill=highlight)
  D.line((px+6,py+8,px+10,py+8),fill=shadow)
  if name in ('steel_etch','engraved','steel_socket'):
   D.line((px+4,py+5,px+7,py+8),fill=shadow,width=1)
   D.line((px+7,py+8,px+10,py+5),fill=highlight,width=1)
  if name=='steel_boss':
   D.ellipse((px+3,py+3,px+12,py+12),fill=base,outline=highlight)
   D.point((px+5,py+5),fill=highlight)
 elif name.startswith(('wood','leather')) or name=='shield_wood':
  for xoff in (4,8,11):
   D.line((px+xoff,py+2,px+xoff-1,py+7),fill=highlight if xoff==8 else shadow)
  D.rectangle((px+2,py+9,px+7,py+11),fill=base)
  if name=='leather_wrap':
   for off in (-8,-1,6,13):
    D.line((px+max(0,off),py+min(14,14-max(0,off)),px+min(15,off+14),py+max(0,-off)),fill=highlight)
 elif name=='mail':
  D.rectangle((px,py,px+15,py+15),fill=(33,43,54,255))
  for yy in range(1,15,4):
   for xx in range(1+(2 if yy%8<4 else 0),15,4):
    D.rectangle((px+xx,py+yy,px+xx+2,py+yy+2),fill=base)
    D.point((px+xx,py+yy),fill=highlight)
    D.point((px+xx+2,py+yy+2),fill=shadow)
# Hand-painted large shield front: physical wood grain, directional paint and iron rim via model.
R=31.2
for yy in range(64):
 for xx in range(64):
  dx,dy=xx-31.5,yy-31.5
  if dx*dx+dy*dy>R*R:continue
  woodstripe=int(xx//9)
  grain=(woodstripe%3)-1
  base=(191+grain*5,146+grain*5,94+grain*3)
  # pinwheel swirl: large purposefully placed painted swathes
  t=math.atan2(dy,dx)
  swirl=(t+0.055*math.hypot(dx,dy))%(2*math.pi)
  if swirl<2.9:base=(176,66,54)
  elif swirl<3.85:base=(225,207,168)
  # value variations by large contiguous patches only
  if dy>18:base=tuple(max(0,int(c*.76)) for c in base)
  elif dy < -22:base=tuple(min(255,int(c*1.12)) for c in base)
  ATLAS.putpixel((xx,yy),(*base,255))
  # backside simpler planks
  b=(126+woodstripe%4*5,82+woodstripe%4*4,49+woodstripe%4*3)
  if xx%9 in (0,1):b=(75,47,31)
  if yy>22:b=tuple(int(c*.86) for c in b)
  ATLAS.putpixel((64+xx,yy),(*b,255))
# simple Norse angular mark, not tiny busy tracery
for region,paint in [((0,0),'front'),((64,0),'back')]:
 px,py=region
 D.line((px+17,py+21,px+29,py+11,px+38,py+20),fill=(235,214,166) if paint=='front' else (162,119,76),width=2)
 D.line((px+20,py+40,px+28,py+47,px+42,py+34),fill=(226,207,161) if paint=='front' else (156,113,75),width=2)
ATLAS.save(TEX/'norse_showcase_materials.png')

def uv(name,region=None):
 if name in ('front','back'):
  px=0 if name=='front' else 64
  if region is None:region=(0,0,16,16)
  x0,y0,x1,y1=region
  # region world x/y expressed with top down screen mapping
  return [round((px+x0*4)/16,4),round(y0/4,4),round((px+x1*4)/16,4),round(y1/4,4)]
 px,py=materials[name]
 return [px/16,py/16,px/16+1,py/16+1]

DISPLAY={
 'gui':{'rotation':[25,220,0],'translation':[0,0,0],'scale':[1,1,1]},
 'ground':{'rotation':[0,0,0],'translation':[0,2.5,0],'scale':[0.55,0.55,0.55]},
 'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[0.9,0.9,0.9]},
 'thirdperson_righthand':{'rotation':[0,-90,55],'translation':[0,2.0,1],'scale':[0.85,0.85,0.85]},
 'thirdperson_lefthand':{'rotation':[0,90,-55],'translation':[0,2.0,1],'scale':[0.85,0.85,0.85]},
 'firstperson_righthand':{'rotation':[0,-90,25],'translation':[1.13,3.2,1.13],'scale':[0.85,0.85,0.85]},
 'firstperson_lefthand':{'rotation':[0,90,-25],'translation':[-1.13,3.2,1.13],'scale':[0.85,0.85,0.85]},
}
class Model:
 def __init__(self,name):self.name=name;self.elements=[]
 def box(self,fr,to,main,*,name=None,faces=None,front=None,back=None):
  assert all(-0.1<=v<=16.1 for v in [*fr,*to]),(self.name,name,fr,to)
  assert all(fr[k]<to[k] for k in range(3)),(self.name,name)
  mats={'north':main,'south':main,'east':main,'west':main,'up':main,'down':main}
  if faces:mats.update(faces)
  face_obj={key:{'uv':uv(mat),'texture':'#atlas'} for key,mat in mats.items()}
  # Front/back image crop aligned to the exact cuboid face, full 64px painted shield.
  if front:
   x0,y0,x1,y1=fr[0],16-to[1],to[0],16-fr[1]
   face_obj['south']={'uv':uv('front',(x0,y0,x1,y1)),'texture':'#atlas'}
  if back:
   x0,y0,x1,y1=fr[0],16-to[1],to[0],16-fr[1]
   face_obj['north']={'uv':uv('back',(x0,y0,x1,y1)),'texture':'#atlas'}
  self.elements.append({'name':name or main,'from':list(fr),'to':list(to),'faces':face_obj})
  return self
 def b(self,x0,y0,z0,x1,y1,z1,main,**kwargs):return self.box((x0,y0,z0),(x1,y1,z1),main,**kwargs)
 def save(self):
  d={'textures':{'atlas':'norsegear:item/norse_showcase_materials','particle':'norsegear:item/norse_showcase_materials'},
     'elements':self.elements,'display':json.loads(json.dumps(DISPLAY)),'ambientocclusion':True}
  if self.name in ('spear','javelin'):
   d['display']['fixed']={'rotation':[0,0,0],'translation':[0,0,0],'scale':[1,1,1]}
  if self.name=='round_shield':
   d['display']['gui']={'rotation':[20,35,0],'translation':[0,0,0],'scale':[.85,.85,.85]}
   d['display']['thirdperson_lefthand']={'rotation':[0,-90,0],'translation':[3,4,4.4],'scale':[.9,.9,.9]}
   d['display']['thirdperson_righthand']={'rotation':[0,90,0],'translation':[-3,4,4.4],'scale':[.9,.9,.9]}
  (ITEM/(self.name+'.json')).write_text(json.dumps(d,indent=2)+'\n')
  return d

def shaft(m,_,y1,small=False):
 w=.84 if small else 1.15;x=8-w/2
 m.b(x,.45,8-w/2,8+w/2,y1,8+w/2,'wood',name='ash-wood-grain',faces={'up':'wood_end'})
 m.b(x-.07,.65,8-w/2-.07,8+w/2+.07,1.1,8+w/2+.07,'steel_socket',name='pommel')
 # Leather criss-cross heel; sculpted strips are more visible than black pixel noise
 for y in (1.6,2.5,3.4,4.3,5.2):
  if y+0.28<y1:
   m.b(x-.10,y,8-w/2-.12,8+w/2+.10,y+.33,8+w/2+.12,'leather_wrap',name='leather-helix-band')
 m.b(x-.10,6.0 if y1>9 else 5.4,8-w/2-.1,8+w/2+.1,6.5 if y1>9 else 5.85,8+w/2+.1,'bronze',name='handle-ferrule')

def create_bearded():
 m=Model('bearded_axe');shaft(m,'',14.55)
 m.b(6.5,11.7,7.12,9.6,14.68,8.88,'steel_socket',name='collared-eye')
 m.b(7.2,12.35,6.98,9.2,14.15,7.12,'steel_dark',name='eye-inset')
 m.b(9.3,12.65,7.30,11.95,14.42,8.7,'steel_dark',name='forge-web')
 m.b(11.5,12.5,7.38,13.0,14.90,8.62,'steel',name='upper-wing')
 m.b(12.9,13.95,7.48,14.55,15.3,8.52,'steel_light',name='flared-top-tip')
 m.b(11.15,10.1,7.37,13.1,12.66,8.63,'steel_dark',name='deep-beard-neck')
 m.b(12.35,8.88,7.41,13.35,11.35,8.59,'steel',name='hanging-beard')
 m.b(13.1,9.22,7.42,14.03,14.85,8.58,'steel_light',name='cutting-edge')
 m.b(13.90,9.9,7.49,14.38,14.43,8.51,'steel_edge',name='bright-razor-edge')
 m.b(9.75,12.7,8.71,12.2,14.1,8.9,'steel_etch',name='forged-engraving')
 m.b(7.15,13.45,8.95,7.7,14.0,9.3,'rivet',name='upper-socket-rivet')
 m.b(7.15,12.1,8.95,7.7,12.65,9.3,'rivet',name='lower-socket-rivet')
 m.save()

def create_dane():
 m=Model('dane_axe');shaft(m,'',14.6)
 m.b(6.6,11.9,7.03,9.6,14.8,8.97,'steel_socket',name='socket')
 m.b(9.4,12.1,7.32,12.6,14.45,8.68,'steel_dark',name='slender-neck')
 m.b(11.0,13.0,7.38,13.5,14.85,8.62,'steel',name='blade-wing')
 m.b(12.4,13.6,7.42,14.25,15.38,8.58,'steel_light',name='upper-flare')
 m.b(13.8,14.55,7.49,14.85,15.88,8.51,'steel_edge',name='upper-corner')
 m.b(12.4,11.8,7.36,14.4,13.85,8.64,'steel',name='lower-curve')
 m.b(11.35,11.25,7.4,12.75,12.85,8.6,'steel_dark',name='lower-shoulder')
 m.b(13.92,11.82,7.44,14.9,14.88,8.56,'steel_light',name='cutting-face')
 m.b(14.68,12.12,7.49,15.1,14.89,8.51,'steel_edge',name='razor')
 m.b(9.9,12.3,8.69,11.5,13.65,8.88,'steel_etch',name='rune-fluting')
 for y in (12.45,13.85):m.b(7.13,y,8.98,7.65,y+.43,9.28,'rivet',name='socket-rivet')
 m.save()

def create_spear(name='spear'):
 m=Model(name);shaft(m,'',11.1 if name=='spear' else 11.7,small=True)
 m.b(7.19,9.95,7.20,8.81,11.85,8.8,'steel_socket',name='socket',faces={'up':'bronze'})
 m.b(7.06,11.62,7.4,8.94,13.70,8.6,'steel_dark',name='leaf-base')
 m.b(6.48,12.68,7.41,9.52,14.30,8.59,'steel',name='broad-leaf')
 m.b(7.0,13.98,7.47,9.0,14.88,8.53,'steel_light',name='tapered-leaf')
 m.b(7.55,14.75,7.55,8.45,15.64,8.45,'steel_edge',name='piercing-tip')
 m.b(7.78,12.15,8.62,8.23,14.56,8.85,'steel_ridge',name='central-ridge')
 m.b(7.2,11.55,8.81,8.8,11.82,9.13,'bronze',name='socket-collar')
 m.save()

def create_shield():
 m=Model('round_shield')
 R=6.95;columns=16
 for i in range(columns):
  x0=8-R+i*2*R/columns;x1=8-R+(i+1)*2*R/columns
  middle=(x0+x1)/2
  ymax=math.sqrt(max(.01,R*R-(middle-8)**2));y0=8-ymax;y1=8+ymax
  m.b(x0,y0,7.0,x1,y1,8.13,'shield_wood',name='oak-board',front=True,back=True)
  # front painted surface: texture uses full image region; 3D plate thin with wood beneath
  m.b(x0+.02,y0+.02,8.13,x1-.02,y1-.02,8.24,'shield_wood',name='hand-painted-board',front=True)
 # perimeter iron rim: 16 short segments approximating the circular edge in voxel steps.
 for i in range(columns):
  x0=8-R+i*2*R/columns;x1=8-R+(i+1)*2*R/columns
  middle=(x0+x1)/2;h=math.sqrt(max(.01,R*R-(middle-8)**2));
  ylo=8-h;yhi=8+h
  m.b(x0,ylo-.13,6.90,x1,ylo+.24,8.40,'steel_trim',name='forged-iron-rim')
  m.b(x0,yhi-.24,6.90,x1,yhi+.13,8.40,'steel_trim',name='forged-iron-rim')
 # central steel umbon with stepped volumes
 m.b(6.18,6.18,8.28,9.82,9.82,8.75,'steel_trim',name='boss-base')
 m.b(6.58,6.58,8.75,9.42,9.42,9.40,'steel_boss',name='embossed-cone')
 m.b(7.07,7.07,9.4,8.93,8.93,9.84,'steel_light',name='boss-crown')
 m.b(7.48,7.48,9.84,8.52,8.52,10.05,'steel_edge',name='boss-highlight')
 # Period-style rivets around boss, and back wooden grip.
 for xx,yy in [(5.9,7.9),(10.05,7.9),(7.9,5.9),(7.9,10.05)]:
  m.b(xx,yy,8.75,xx+.32,yy+.32,9.02,'rivet',name='boss-rivet')
 m.b(6.7,6.9,6.5,9.3,9.1,7.01,'leather_dark',name='back-grip')
 m.b(7.15,4.65,6.60,8.85,11.35,6.90,'wood_dark',name='back-stave')
 m.save()

create_bearded();create_dane();create_spear('spear');create_spear('javelin');create_shield()
# Also create a compact editable construction spec to pair game models with armor design.
armor_spec={'texture':'assets/norsegear/textures/models/armor/ironweave_layer_1.png','texture_size':[128,64],
 'parts':[
  {'name':'cap','from':[-4.2,-8.55,-4.3],'to':[4.2,-6.0,4.3],'material':'steel'},
  {'name':'raised-spine','from':[-.6,-9.15,-4.38],'to':[.6,-7.3,4.38],'material':'steel_trim'},
  {'name':'brow-crest','from':[-4.6,-6.15,-5.06],'to':[4.6,-5.1,-3.95],'material':'steel_light'},
  {'name':'nasal','from':[-.65,-5.5,-5.28],'to':[.65,-2.2,-4.37],'material':'steel_light'},
  {'name':'left-temple','from':[3.55,-6.0,-3.6],'to':[4.6,-2.6,3.4],'material':'steel_trim'},
  {'name':'right-temple','from':[-4.6,-6.0,-3.6],'to':[-3.55,-2.6,3.4],'material':'steel_trim'},
  {'name':'leather-collar','from':[-4.45,-.45,-3.3],'to':[4.45,1.65,3.35],'material':'leather'},
  {'name':'mail-front','from':[-3.7,1.5,-3.28],'to':[3.7,8.25,-2.92],'material':'mail'},
  {'name':'strap-left','from':[-3.65,1.3,-3.67],'to':[-2.6,8.4,-3.16],'material':'leather'},
  {'name':'strap-right','from':[2.6,1.3,-3.67],'to':[3.65,8.4,-3.16],'material':'leather'},
  {'name':'belt','from':[-4.42,8.25,-3.22],'to':[4.42,10.12,3.22],'material':'leather'},
  {'name':'belt-buckle','from':[-1.15,8.0,-4.05],'to':[1.15,10.25,-3.05],'material':'bronze'},
 ]}
(ROOT/'tools/armor_construction_031.json').write_text(json.dumps(armor_spec,indent=2)+'\n')
# Handpaint worn armor UVs, rather than 4-color placeholders. Source texture 128x64 with large coherent clusters.
for fn in ('ironweave_layer_1.png','ironweave_layer_2.png'):
 img=Image.new('RGBA',(128,64),(0,0,0,0));p=img.load()
 for y in range(64):
  for x in range(128):
   if x<38 and y<17: # helmet cap
    c=(85,113,141) if y>8 else (135,164,185)
    if x%15==0 or y in(1,2): c=(187,205,215)
    if (x//7+y//4)%4==0:c=(103,134,157)
   elif y<20 and x>=40: # mail front
    c=(69,85,104)
    if (x+(y//3)%2*2)%4 in (0,1) and y%4 in(0,1):c=(179,199,209)
    elif (x+y)%4==0:c=(104,126,141)
    elif x%4==3 and y%4==3:c=(0,0,0,0)
   elif 15<=y<45 and x<45: # leather straps over steel
    c=(120,78,50) if (x//6)%3==0 else (86,104,124)
    if y%11==0:c=(191,139,84)
    if x%13==0:c=(205,215,213)
   else: # steel/greaves
    c=(86,113,135)
    if y%13 in (0,1):c=(160,179,195)
    if x%11==0:c=(56,76,96)
    if x%17==1 and y%17==1:c=(220,228,228)
   p[x,y]=(*c,255) if len(c)==3 else c
 img.save(ARMOR/fn)
print('Built 5 detailed cuboid models, 256px UV atlas, armor source maps')
