#!/usr/bin/env python3
"""WorldGen-only 2.4: 4 colossal tree silhouettes + 4 sacred Choir structures.
Uses ONLY block IDs already present in Halo Haven 2.3.
"""
from pathlib import Path
import json,tarfile,tempfile,math,random
import nbtlib
from nbtlib import Compound,List,Int,String
BUNDLE=Path("projects/halohaven-alpha21/bundle.tar.xz")
NS="halohavenalpha2"
assert BUNDLE.is_file()
def write(p,obj):
 p.parent.mkdir(parents=True,exist_ok=True)
 p.write_text(json.dumps(obj,indent=2)+"\n")
class Template:
 def __init__(self,w,h,d):self.w=w;self.h=h;self.d=d;self.blocks={}
 def put(self,x,y,z,b,props=None):
  x,y,z=map(int,(x,y,z));assert 0<=x<self.w and 0<=y<self.h and 0<=z<self.d,(x,y,z,self.w,self.h,self.d)
  self.blocks[(x,y,z)]=(b,tuple(sorted((props or {}).items())))
 def fill(self,x1,y1,z1,x2,y2,z2,b,props=None):
  for x in range(x1,x2+1):
   for y in range(y1,y2+1):
    for z in range(z1,z2+1):self.put(x,y,z,b,props)
 def save(self,path):
  path.parent.mkdir(parents=True,exist_ok=True)
  ids={};states=[];blocks=[]
  for (x,y,z),info in sorted(self.blocks.items()):
   if info not in ids:
    ids[info]=len(ids)
    b,props=info
    state=Compound({"Name":String(b)})
    if props:state["Properties"]=Compound({k:String(v) for k,v in props})
    states.append(state)
   blocks.append(Compound({"pos":List[Int]([Int(x),Int(y),Int(z)]),"state":Int(ids[info])}))
  obj=nbtlib.File({"DataVersion":Int(3953),"size":List[Int]([Int(self.w),Int(self.h),Int(self.d)]),
                   "palette":List[Compound](states),"blocks":List[Compound](blocks),"entities":List[Compound]([])})
  obj.save(path,gzipped=True)
  assert nbtlib.load(path,gzipped=True)["size"][0]==self.w
  return len(blocks)

LOG=NS+":heartwood_log";LEAF=NS+":heartwood_leaves"
LEAF_PROP={"distance":"7","persistent":"true","waterlogged":"false"}
def crown(t,cx,cy,cz,rx,ry,rz,seed):
 for x in range(max(0,int(cx-rx-1)),min(t.w-1,int(cx+rx+1))+1):
  for y in range(max(0,int(cy-ry-1)),min(t.h-1,int(cy+ry+1))+1):
   for z in range(max(0,int(cz-rz-1)),min(t.d-1,int(cz+rz+1))+1):
    value=((x-cx)/rx)**2+((y-cy)/ry)**2+((z-cz)/rz)**2
    value-=.065*math.sin(x*1.17+seed)+.07*math.cos(z*.79+seed*.07)
    if value>1 or (x,y,z) in t.blocks and t.blocks[(x,y,z)][0]==LOG:continue
    t.put(x,y,z,LEAF,LEAF_PROP)
def giant_tree(variation):
 rng=random.Random(8659+variation*717)
 height=62+variation*2
 t=Template(45,height+6,45);cx=cz=22
 trunk_height=38+variation*4
 for y in range(trunk_height+1):
  radius=4 if y<8 and variation%2==0 else 3 if y<trunk_height*.5 else 2
  bx=round(1.5*math.sin(y*.079+variation*.63))
  bz=round(1.4*math.cos(y*.071+variation*.43))
  for x in range(cx+bx-radius,cx+bx+radius+1):
   for z in range(cz+bz-radius,cz+bz+radius+1):
    if (x-cx-bx)**2+(z-cz-bz)**2 <= (radius+.18)**2:t.put(x,y,z,LOG,{"axis":"y"})
 # Structural roots 7-12m outward
 for k in range(7):
  angle=math.tau*k/7+variation*.13
  length=8+rng.randrange(5)
  for distance in range(length+1):
   x=round(cx+math.cos(angle)*distance);z=round(cz+math.sin(angle)*distance)
   y=max(1,int(8-distance*.72))
   width=2 if distance<4 else 1
   for dx in range(-width,width+1):
    for dz in range(-width,width+1):
     if abs(dx)+abs(dz)>width+1:continue
     for down in range(max(0,y-2),y+1):
      t.put(x+dx,down,z+dz,LOG,{"axis":"x" if abs(math.cos(angle))>abs(math.sin(angle)) else "z"})
 # Broad branches and irregular secondary crowns
 for arm in range(8):
  angle=math.tau*arm/8+variation*.3+rng.uniform(-.24,.24)
  start=23+arm%3*6+rng.randint(-2,2)
  extent=10+rng.randint(1,6)
  for i in range(extent+1):
   x=round(cx+math.cos(angle)*i);z=round(cz+math.sin(angle)*i)
   y=start+round(i*.36)+round(1.5*math.sin(i*.41))
   for dy in (-1,0,1):
    t.put(x,y+dy,z,LOG,{"axis":"x" if abs(math.cos(angle))>abs(math.sin(angle)) else "z"})
  crown(t,x,y+2,z,5.5+rng.random()*2,4.4+rng.random()*2,6.4+rng.random(),127+arm+variation*17)
  if arm%2==0:crown(t,x-2,y-2,z,4.2,4,4.6,254+arm+variation*17)
 crown(t,cx,trunk_height+2,cz,9.5,7.2,9.5,90+variation)
 crown(t,cx+3,trunk_height-1,cz-3,7,6,8.5,130+variation)
 return t

BASE="halohaven:choirstone";STONE=NS+":choir_pilgrim_marble"
TILE="halohaven:choir_tiles";INLAY=NS+":choir_rose_inlay"
PILLAR="halohaven:gilded_pillar";GLASS="halohaven:dawn_glass"
LIGHT=NS+":choir_reliquary_lamp";ALTAR="halohaven:pilgrim_altar"
def plaza(t,r=15):
 c=t.w//2;d=t.d//2
 for x in range(t.w):
  for z in range(t.d):
   rr=math.hypot(x-c,z-d)
   if rr>r+(.35*math.cos(x*.45+z*.23)):continue
   t.put(x,3,z,STONE if (x+z)%9 else INLAY)
   if rr<r-2 or (x+z)%7==0:t.fill(x,0,z,x,2,z,BASE)
def ziggurat():
 t=Template(43,35,43);c=21
 for stage,r in enumerate((18,15,12,9,6,3)):
  floor=2+stage*3
  for x in range(c-r,c+r+1):
   for z in range(c-r,c+r+1):
    if abs(x-c)+abs(z-c)>int(r*1.7):continue
    for y in range(max(0,floor-2),floor+1):
     edge=abs(x-c)>=r-1 or abs(z-c)>=r-1
     if edge or y==floor or (x+z)%6==0:
      t.put(x,y,z,INLAY if edge and y==floor else STONE if y==floor else BASE)
  # Wide ceremonial staircase, with partial intervals between tiers
  for z in range(c-r,c+1):
   for x in (c-1,c,c+1):
    t.put(x,floor,z,TILE)
    if z==c-r:
     for dy in range(1,3):
      if x!=c:t.put(x,floor+dy,z,PILLAR)
 t.fill(c-2,20,c-2,c+2,20,c+2,INLAY)
 t.put(c,21,c,ALTAR)
 for x,z in ((19,19),(23,19),(19,23),(23,23)):
  t.fill(x,21,z,x,23,z,PILLAR)
  t.put(x,24,z,LIGHT)
 return t
def sanctuary():
 t=Template(35,27,35);c=17
 plaza(t,15)
 for x in range(5,30):
  for z in (6,29):t.put(x,3,z,INLAY)
 for z in range(6,30):
  for x in (5,29):t.put(x,3,z,INLAY)
 for x in (6,11,17,23,28):
  for z in (7,15,27):
   top=16+(x+z)%5
   for y in range(4,top):
    if (x-17)**2+(z-17)**2>=18:t.put(x,y,z,PILLAR,{"axis":"y"})
   t.put(x,top,z,INLAY)
 for x in range(7,28):t.put(x,19,17,INLAY if x%3 else PILLAR)
 for y in range(7,20):
  for x in range(10,25):
   distance=(x-c)**2+(y-13)**2
   if 53<=distance<=78 and (x+y)%11!=0:t.put(x,y,27,PILLAR)
   if 32<=distance<=45 and (x+y)%4==0:t.put(x,y,27,GLASS)
 t.fill(c-3,4,21,c+3,4,24,STONE);t.put(c,5,24,ALTAR)
 for x in (c-4,c+4):
  t.fill(x,4,20,x,9,20,PILLAR,{"axis":"y"});t.put(x,10,20,LIGHT)
 return t
def processional_way():
 t=Template(43,30,43);c=21
 for z in range(2,41):
  rise=min(14,z//3)
  for x in range(c-5,c+6):
   for y in range(max(0,rise-2),rise+1):
    if y==rise or x in (c-5,c+5):t.put(x,y,z,TILE if y==rise else BASE)
  if z%6==0:
   for x in (c-7,c+7):
    t.fill(x,rise,z,x,rise+5,z,PILLAR,{"axis":"y"})
    t.put(x,rise+6,z,LIGHT)
 for x in range(c-10,c+11):
  for z in range(35,43):
   t.put(x,14,z,STONE if (x+z)%4 else INLAY)
   for y in range(14):
    if x in (c-10,c+10) or (x+z)%7==0:t.put(x,y,z,BASE)
 t.put(c,15,40,ALTAR)
 return t
def ossuary():
 t=Template(35,28,35);c=17
 plaza(t,15)
 for ring in (7,11,14):
  for step in range(80):
   angle=step*math.tau/80
   x=round(c+ring*math.cos(angle));z=round(c+ring*math.sin(angle))
   if ring==14 and step%9 not in (0,1):continue
   height=12 if ring==7 else 8 if ring==11 else 4
   if step%11==0:
    t.fill(x,4,z,x,height,z,PILLAR,{"axis":"y"})
    t.put(x,height+1,z,LIGHT)
   else:t.put(x,4,z,INLAY)
 for x in range(11,24):
  for z in range(11,24):
   if math.hypot(x-c,z-c)<5:t.put(x,3,z,INLAY)
 t.put(c,4,c,ALTAR)
 for x in range(6,29):
  for z in (7,27):
   if x%4:t.put(x,16,z,BASE)
 return t
temples={"ziggurat":ziggurat,"sanctuary":sanctuary,"processional_way":processional_way,"ossuary":ossuary}
def generate(root):
 d=root/"src/main/resources/data"/NS;world=d/"worldgen"
 tree_names=[]
 for variation in range(4):
  name="colossal_canopy_"+str(variation+1)
  count=giant_tree(variation).save(d/"structure"/(name+".nbt"))
  assert count>4000,(name,count)
  tree_names.append(NS+":"+name)
 write(world/"template_pool/canopy_colossal.json",{
  "name":NS+":canopy_colossal","fallback":"minecraft:empty",
  "elements":[{"weight":1,"element":{"element_type":"minecraft:single_pool_element",
   "location":name,"processors":"minecraft:empty","projection":"rigid"}} for name in tree_names]})
 write(world/"structure/canopy_colossal.json",{
  "type":"minecraft:jigsaw","biomes":"#halohavenalpha2:canopy",
  "step":"surface_structures","terrain_adaptation":"none",
  "start_pool":NS+":canopy_colossal","size":1,
  "start_height":{"absolute":-2},"project_start_to_heightmap":"WORLD_SURFACE_WG",
  "max_distance_from_center":70,"use_expansion_hack":False,"spawn_overrides":{}})
 write(world/"structure_set/canopy_colossal.json",{
  "structures":[{"structure":NS+":canopy_colossal","weight":1}],
  "placement":{"type":"minecraft:random_spread","spacing":19,"separation":9,
   "salt":28381451,"spread_type":"triangular",
   "exclusion_zone":{"other_set":NS+":settlements","chunk_count":7}}})
 temple_names=[]
 for name,create in temples.items():
  id="choir_"+name
  count=create().save(d/"structure"/(id+".nbt"))
  assert count>300,(name,count)
  temple_names.append(NS+":"+id)
 write(world/"template_pool/choir_monuments.json",{
  "name":NS+":choir_monuments","fallback":"minecraft:empty",
  "elements":[{"weight":1,"element":{"element_type":"minecraft:single_pool_element",
   "location":name,"processors":"minecraft:empty","projection":"rigid"}} for name in temple_names]})
 write(world/"structure/choir_monuments.json",{
  "type":"minecraft:jigsaw","biomes":"#halohavenalpha2:choir",
  "step":"surface_structures","terrain_adaptation":"beard_thin",
  "start_pool":NS+":choir_monuments","size":1,
  "start_height":{"absolute":-2},"project_start_to_heightmap":"WORLD_SURFACE_WG",
  "max_distance_from_center":70,"use_expansion_hack":False,"spawn_overrides":{}})
 write(world/"structure_set/choir_monuments.json",{
  "structures":[{"structure":NS+":choir_monuments","weight":1}],
  "placement":{"type":"minecraft:random_spread","spacing":20,"separation":10,
   "salt":28381453,"spread_type":"triangular",
   "exclusion_zone":{"other_set":NS+":settlements","chunk_count":7}}})
 print("2.4: NBT compiled 4 giant trees and 4 mythological Choir monuments")
with tempfile.TemporaryDirectory() as td:
 root=Path(td)
 with tarfile.open(BUNDLE,"r:xz") as z:
  for e in z:
   if not e.isfile():continue
   p=Path(e.name);assert p.parts[0]=="src" and ".." not in p.parts
   dest=root/p;dest.parent.mkdir(parents=True,exist_ok=True)
   dest.write_bytes(z.extractfile(e).read())
 generate(root)
 with tarfile.open(BUNDLE,"w:xz",preset=6) as z:
  for file in sorted((root/"src").rglob("*")):
   if file.is_file():z.add(file,arcname=file.relative_to(root).as_posix())
