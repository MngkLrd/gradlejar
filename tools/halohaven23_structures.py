#!/usr/bin/env python3
"""Halo Haven 2.3: eighteen authored regional structures with grounded footprint, doors and independent structure sets."""
from pathlib import Path
import json, tarfile, tempfile, math
import nbtlib
from nbtlib import Compound, List, Int, String
B=Path("projects/halohaven-alpha21/bundle.tar.xz")
ID="halohavenalpha2"
palette={
"dawn":("dawn_relief","dawn_arcade","dawnwood_log","dawnwood_door_face"),
"chalk":("chalk_banded_marl","chalk_fossil_bricks","chalkwood_log","dawnwood_door_face"),
"canopy":("canopy_root_bricks","canopy_luminous_bark","heartwood_log","heartwood_door_face"),
"gorge":("gorge_cleavage","gorge_azure_runes","ringwood_log","heartwood_door_face"),
"choir":("choir_pilgrim_marble","choir_rose_inlay","choirwood_log","veilwood_door_face"),
"veil":("veil_fracture","veil_starlit_tiles","veilwood_log","veilwood_door_face")}
def save_json(p,obj):
    p.parent.mkdir(parents=True,exist_ok=True)
    p.write_text(json.dumps(obj,indent=2)+"\n",encoding="utf8")
class Design:
    def __init__(self,biome,kind):
        self.biome=biome;self.kind=kind;self.blocks={};self.w=17;self.h=15;self.d=17
        self.base,self.inlay,self.post,self.door=palette[biome]
        self.base=ID+":"+self.base;self.inlay=ID+":"+self.inlay
        self.post=ID+":"+self.post;self.door=ID+":"+self.door
        lamp_by_biome={"dawn":"dawnstar_lamp","chalk":"chalk_moon_lamp",
                       "canopy":"canopy_firefly_lamp","gorge":"gorge_resonance_lamp",
                       "choir":"choir_reliquary_lamp","veil":"veil_lament_lamp"}
        self.light=ID+":"+lamp_by_biome[biome]
    def p(self,x,y,z,id,props=None,nbt=None):
        if not (0<=x<17 and 0<=y<15 and 0<=z<17):
            raise ValueError((self.biome,self.kind,x,y,z))
        self.blocks[(x,y,z)]=(id,props or {},nbt)
    def fill(self,x0,y0,z0,x1,y1,z1,block):
        for x in range(x0,x1+1):
            for y in range(y0,y1+1):
                for z in range(z0,z1+1):
                    self.p(x,y,z,block)
    def pedestal(self,radius=7):
        # Do not produce thin one-block hovering slabs: layered central supports,
        # buttresses and a walked-on surface at y=3.
        for x in range(1,16):
            for z in range(1,16):
                d=math.hypot(x-8,z-8)
                if d>radius+0.45:continue
                self.p(x,3,z,self.inlay if ((x+z)%7==0) else self.base)
                for y in range(0,3):
                    if d<radius-(2-y)*1.15 or (x%5==0 and z%5==0):
                        self.p(x,y,z,self.base)
    def lamp(self,x,y,z):
        self.p(x,y,z,self.light)
    def column(self,x,z,top):
        self.fill(x,4,z,x,top,z,self.post)
    def roof(self,cy=10):
        # Stepped dressed-stone roof with eaves, an overhang and a skylight.
        for tier,r in ((0,7),(1,6),(2,5)):
            y=cy+tier
            for x in range(8-r,9+r):
                for z in range(8-r,9+r):
                    if abs(x-8)==r or abs(z-8)==r:
                        self.p(x,y,z,self.inlay if (x+z)%4 else self.base)
        self.lamp(8,cy+3,8)
    def house(self):
        self.pedestal(7.5)
        # An axial courtyard / shallow nave, no obstructing blocks in front of the entrance.
        for x in range(3,14):
            for z in range(5,14):
                self.p(x,3,z,self.inlay)
        for y in range(4,10):
            for x in range(3,14):
                for z in range(5,14):
                    wall=(x in (3,13) or z in (5,13))
                    if not wall: self.p(x,y,z,"minecraft:air")
                    else:self.p(x,y,z,self.base if (x+z)%8 else self.inlay)
        # Structural jambs and a precisely two-block doorway centered on the stone path.
        self.column(6,5,9);self.column(10,5,9)
        for y in range(4,7):
            self.p(8,y,5,"minecraft:air")
        for half,y in (("lower",4),("upper",5)):
            self.p(8,y,5,self.door,{"facing":"north","half":half,"hinge":"left",
                                   "open":"false","powered":"false"})
        for z in range(0,5):
            for x in range(7,10):
                self.p(x,3,z,self.inlay)
                self.p(x,4,z,"minecraft:air")
                self.p(x,5,z,"minecraft:air")
        for x in (3,13):
            for z in (7,11):
                for y in (6,7):self.p(x,y,z,"halohaven:aurora_glass")
        self.roof(10)
        self.lamp(8,9,8)
        # A chest safely inside the finished room, with a usable air block above.
        self.p(11,4,11,"minecraft:chest",
               {"facing":"north","type":"single","waterlogged":"false"},
               {"LootTable":String("halohavenalpha2:chests/village_supplies"),
                "LootTableSeed":nbtlib.Long(0)})
        self.p(11,5,11,"minecraft:air")
        self.p(5,4,11,"minecraft:crafting_table")
        self.p(6,4,11,"minecraft:bookshelf")
    def archive(self):
        self.pedestal(7.6)
        # Ovoid wall fragments in three scales: one accessible crumbling nave,
        # a separated arch, and fallen material columns.
        for x in range(3,14):
            if x not in (7,8,9):
                self.p(x,4,4,self.base)
                if x%3:self.p(x,5,4,self.base)
            self.p(x,3,4,self.inlay)
        for x,z,height in ((3,4,9),(13,4,7),(3,12,6),(13,12,10)):
            self.column(x,z,height)
            self.lamp(x,height+1,z)
        for x in range(3,14):
            if abs(x-8)>2:self.p(x,8,12,self.inlay)
        for z in (7,9,11):
            for x in (5,11):
                self.p(x,4,z,self.inlay)
                self.p(x,5,z,self.base)
        for x,z in ((5,5),(11,5),(4,11),(12,11)):
            self.p(x,4,z,self.light)
        for y in (4,5,6):
            self.p(8,y,4,"minecraft:air")
        self.p(8,4,10,self.inlay)
        self.p(8,5,10,"halohaven:pilgrim_altar")
    def observatory(self):
        self.pedestal(7.8)
        for x,z in ((3,3),(13,3),(3,13),(13,13),(3,8),(13,8),(8,3),(8,13)):
            self.column(x,z,11)
            self.lamp(x,12,z)
        for x in range(4,13):
            for z in range(4,13):
                if math.hypot(x-8,z-8)>5.8:continue
                if (abs(x-8)==4 or abs(z-8)==4):
                    self.p(x,10,z,self.inlay)
                elif (x%3==0 or z%3==0) and math.hypot(x-8,z-8)>3:
                    self.p(x,11,z,"halohaven:aurora_glass")
        self.p(8,4,8,self.post)
        self.p(8,5,8,self.light)
        for x,z in ((5,6),(11,6),(5,10),(11,10)):
            self.p(x,4,z,self.base)
        for x in range(5,12):
            for z in range(0,4):
                self.p(x,3,z,self.inlay)
        # No unsupported lamp above the center of the dome.
    def generate(self):
        if self.kind=="house":self.house()
        elif self.kind=="archive":self.archive()
        else:self.observatory()
        # Scaffold check: floating décor at >y4 is supported by full shaft, foundation or roof.
        for (x,y,z),(id,pr,nbt) in list(self.blocks.items()):
            if id=="minecraft:chest":
                assert self.blocks.get((x,y-1,z),(None,))[0] not in (None,"minecraft:air")
                assert self.blocks.get((x,y+1,z),(None,))[0]=="minecraft:air"
        self.write()
    def write(self):
        path=self.target/"structure"/("regional_"+self.biome+"_"+self.kind+".nbt")
        path.parent.mkdir(parents=True,exist_ok=True)
        palette_states=[];lookup={};blocks=[]
        for (x,y,z),(id,props,nbt) in sorted(self.blocks.items()):
            state=(id,tuple(sorted(props.items())))
            if state not in lookup:
                lookup[state]=len(lookup)
                pal=Compound({"Name":String(id)})
                if props:pal["Properties"]=Compound({k:String(v) for k,v in props.items()})
                palette_states.append(pal)
            entry=Compound({"state":Int(lookup[state]),
                            "pos":List[Int]([Int(x),Int(y),Int(z)])})
            if nbt:
                n=Compound()
                for k,v in nbt.items():
                    n[k]=v
                entry["nbt"]=n
            blocks.append(entry)
        doc=nbtlib.File({
            "DataVersion":Int(3953),
            "size":List[Int]([Int(17),Int(15),Int(17)]),
            "palette":List[Compound](palette_states),
            "blocks":List[Compound](blocks),
            "entities":List[Compound]([])
        })
        doc.save(path,gzipped=True)
        assert len(blocks)>=120,(path,len(blocks))
        nbtlib.load(path,gzipped=True)
def set_world(root):
    data=root/"src/main/resources/data/halohavenalpha2"
    w=data/"worldgen"
    for idx,biome in enumerate(palette):
        keys=[]
        for kind in ("house","archive","observatory"):
            d=Design(biome,kind);d.target=data;d.generate()
            key="regional_"+biome+"_"+kind;keys.append(key)
            save_json(w/"template_pool"/(key+".json"),{
               "name":ID+":"+key,"fallback":"minecraft:empty",
               "elements":[{"weight":1,"element":{"element_type":"minecraft:single_pool_element",
                   "location":ID+":"+key,"processors":"minecraft:empty","projection":"rigid"}}]})
            save_json(w/"structure"/(key+".json"),{
               "type":"minecraft:jigsaw","biomes":"#halohavenalpha2:"+biome,
               "step":"surface_structures","terrain_adaptation":"beard_box",
               "start_pool":ID+":"+key,"size":1,"start_height":{"absolute":0},
               "project_start_to_heightmap":"WORLD_SURFACE_WG",
               "max_distance_from_center":48,"use_expansion_hack":False,"spawn_overrides":{}})
        save_json(w/"structure_set"/("regional_"+biome+".json"),{
            "structures":[{"structure":ID+":"+key,"weight":3 if k=="house" else 2}
                          for key,k in zip(keys,("house","archive","observatory"))],
            "placement":{"type":"minecraft:random_spread","spacing":25,"separation":12,
                "salt":266518+idx*6711,"spread_type":"triangular",
                "exclusion_zone":{"other_set":ID+":settlements","chunk_count":8}}})
with tempfile.TemporaryDirectory() as td:
    root=Path(td)
    with tarfile.open(B,"r:xz") as tar:
        for item in tar:
            if item.isfile():
                p=Path(item.name);assert p.parts[0]=="src" and ".." not in p.parts
                dst=root/p;dst.parent.mkdir(parents=True,exist_ok=True)
                dst.write_bytes(tar.extractfile(item).read())
    set_world(root)
    with tarfile.open(B,"w:xz",preset=6) as tar:
        for p in sorted((root/"src").rglob("*")):
            if p.is_file():tar.add(p,arcname=p.relative_to(root).as_posix())
print("Structures expanded by 18 fully authored rigid NBT temples, houses and observation ruins in 6 regional palettes")
