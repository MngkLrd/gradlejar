#!/usr/bin/env python3
"""Holiday Villages: replace the main hamlet, ground all templates, furnish old landmarks."""
import revise_evergreen_qa as q
import json,gzip
e,w,f,g,t=q.e,q.w,q.f,q.g,q.t
S,R,D,A,J=q.S,q.R,q.D,q.A,q.J
write,text,mod,edit=q.write,q.text,q.mod,q.edit

# Main settlement: two independent plans, with meaningful destinations rather than
# identical cabins. Each building is authored around its function and frontage.
def put(t,x,y,z,n,**p):t.put(x,y,z,n,**p)
def bed(t,x,z,color):
    put(t,x,4,z,color+"_bed",facing="south",part="foot",occupied=False)
    put(t,x,4,z+1,color+"_bed",facing="south",part="head",occupied=False)
def chest(t,x,z):put(t,x,4,z,"chest",facing="north",type="single",waterlogged=False)
def seat(t,x,z,facing="north"):put(t,x,4,z,"spruce_stairs",facing=facing,half="bottom",shape="straight",waterlogged=False)
def table(t,x,z):
    put(t,x,4,z,"spruce_fence",north=False,south=False,east=False,west=False,waterlogged=False)
    put(t,x,5,z,"spruce_pressure_plate",powered=False)
def light(t,x,y,z):put(t,x,y,z,"lantern",hanging=True,waterlogged=False)
def hearth(t,x,z):
    put(t,x,4,z,"bricks");put(t,x,5,z,"campfire",facing="north",lit=True,signal_fire=False,waterlogged=False)
    put(t,x,6,z,"iron_bars",north=False,south=False,east=True,west=True,waterlogged=False)
    for y in range(7,17):put(t,x,y,z,"bricks")
def resident(t,x,z,role,job):
    t.resident(x,4,z,role,job)
    # Persistence and fixed profession/trades remain; experience prevents
    # unemployed resets before the villager claims the nearby POI.
    t.entities[-1]["nbt"][1]["Xp"]=g.I(10)

def house(t,x,z,W,L,role,job,wall,roof,style,extra=False):
    X=x+W-1;Z=z+L-1;cx=x+W//2;cz=z+L//2
    t.fill(x,0,z,X,2,Z,"cobblestone")
    t.fill(x,3,z,X,3,Z,"spruce_planks")
    H=7 if style=="workshop" else 9 if style=="hall" else 8
    t.fill(x,4,z,X,H,Z,"air")
    for xx in range(x,X+1):
      for zz in range(z,Z+1):
        if xx in (x,X) or zz in (z,Z):
          for yy in range(4,H+1):
            beam=(xx in (x,X) and zz in (z,Z)) or yy==H
            put(t,xx,yy,zz,"stripped_spruce_log" if beam else ("stone_bricks" if yy==4 else wall),**({"axis":"y"} if beam else {}))
    # Different roof ridge axes, slopes and eaves. Brick chimneys on actual workshops only.
    axis="z" if style in ("hall","shop") else "x"
    for xx in range(x-1,X+2):
      for zz in range(z-1,Z+2):
        a,b=(xx-(x-1),X+1-xx) if axis=="x" else (zz-(z-1),Z+1-zz)
        yy=H+1+min(a,b)
        if a==b:put(t,xx,yy,zz,roof+"_slab",type="bottom",waterlogged=False)
        else:put(t,xx,yy,zz,roof+"_stairs",facing=("east" if a<b else "west") if axis=="x" else ("south" if a<b else "north"),half="bottom",shape="straight",waterlogged=False)
        if x<=xx<=X and z<=zz<=Z and ((axis=="x" and zz in (z,Z)) or (axis=="z" and xx in (x,X))):
            for y in range(H+1,yy):put(t,xx,y,zz,wall)
    t.fill(x+1,H+1,z+1,X-1,H+1,Z-1,"spruce_planks")
    for xx in range(x+2,X-1,3):
        if xx==cx:continue
        for zz in (z,Z):
            for yy in (5,6):put(t,xx,yy,zz,"glass_pane",north=False,south=False,east=True,west=True,waterlogged=False)
    for zz in range(z+2,Z-1,3):
        for xx in (x,X):
            for yy in (5,6):put(t,xx,yy,zz,"glass_pane",north=True,south=True,east=False,west=False,waterlogged=False)
    for yy,half in ((4,"lower"),(5,"upper")):put(t,cx,yy,z,"spruce_door",facing="north",half=half,hinge="left",open=False,powered=False)
    t.doors.append((cx,4,z));t.rooms.append((cx,4,z+1))
    # Porch: flush with the street, awnings, two vertical wreaths of leaves.
    g.path(t,cx-1,z-3,cx+1,z-1)
    for xx in (cx-2,cx+2):
        put(t,xx,4,z-1,"spruce_fence",north=False,south=False,east=False,west=False,waterlogged=False)
        put(t,xx,5,z-1,"lantern",hanging=False,waterlogged=False)
    for xx in range(cx-2,cx+3):
        put(t,xx,7,z-1,"midnighteve:garland")
        if style=="shop":put(t,xx,8,z-1,"red_wool" if xx%2 else "white_wool")
    # Keep a 2-wide route from the entrance to the rear.
    for xx in range(cx-1,cx+2):
        for zz in range(z+2,Z-1):
            put(t,xx,4,zz,("red_carpet" if (zz+xx)%4 else "white_carpet"))
    # Leave a two-block clear door approach; lighting and actual workstation.
    light(t,cx,H,cz);light(t,cx,H,Z-2)
    bed(t,x+1,Z-3,"red" if style=="shop" else "green")
    if extra:bed(t,X-1,Z-3,"white")
    chest(t,x+2,Z-1)
    station={"Baker":("composter",{"level":0}),"Giftkeeper":("lectern",{"facing":"north","has_book":False,"powered":False}),"Woodworker":("smithing_table",{}),"Ranger":("smoker",{"facing":"north","lit":False}),"Postmaster":("lectern",{"facing":"north","has_book":False,"powered":False}),"Chandler":("brewing_stand",{"has_bottle_0":False,"has_bottle_1":False,"has_bottle_2":False}),"Fisher":("barrel",{"facing":"up","open":False})}[role]
    put(t,X-2,4,Z-1,station[0],**station[1])
    if extra:put(t,X-3,4,Z-1,station[0],**station[1])
    if role=="Baker":
        put(t,x+1,4,z+2,"smoker",facing="east",lit=False)
        put(t,x+1,4,z+3,"barrel",facing="up",open=False)
        put(t,x+2,4,z+2,"spruce_planks");put(t,x+2,5,z+2,"cake",bites=0)
        put(t,X-1,4,z+2,"hay_block",axis="y")
        put(t,X-2,4,z+2,"cauldron")
    elif role=="Giftkeeper":
        for zz in (z+2,z+4):
            put(t,x+1,4,zz,"midnighteve:gift_crate");put(t,x+1,5,zz,"midnighteve:ornament")
        put(t,X-1,4,z+2,"loom");put(t,X-2,4,z+2,"crafting_table")
    elif role=="Woodworker":
        put(t,x+1,4,z+2,"stonecutter",facing="east")
        put(t,x+1,4,z+3,"crafting_table")
        put(t,X-1,4,z+2,"stripped_spruce_log",axis="z")
        put(t,X-1,4,z+3,"stripped_spruce_log",axis="z")
    elif role=="Chandler":
        for xx in (x+1,X-1):
            put(t,xx,4,z+2,"spruce_planks");put(t,xx,5,z+2,"midnighteve:candle_cluster")
        put(t,X-1,4,z+3,"barrel",facing="up",open=False)
    elif role=="Postmaster":
        for zz in (z+2,z+3,z+4):
            put(t,x+1,4,zz,"bookshelf");put(t,x+1,5,zz,"bookshelf")
        put(t,X-1,4,z+2,"cartography_table");put(t,X-1,4,z+3,"barrel",facing="up",open=False)
    elif role=="Fisher":
        put(t,x+1,4,z+2,"barrel",facing="up",open=False);put(t,x+2,4,z+2,"smoker",facing="north",lit=False)
        put(t,X-1,4,z+2,"cauldron")
    else:
        put(t,x+1,4,z+2,"barrel",facing="up",open=False);put(t,x+1,4,z+3,"blast_furnace",facing="east",lit=False)
        table(t,X-2,z+3);seat(t,X-1,z+3,"west")
    # A room can be occupied without turning the entire floor into furniture.
    resident(t,cx,z+2,role,job)
    if extra:resident(t,cx,Z-2,role,job)
    # Roof snow on exposed full timber ledges, never on top of a stair.
    for xx in (x-1,X+1):
        for zz in (z-1,Z+1):
            # small decorated ground-level planter, outside the entrance route
            if zz==Z+1:
                put(t,xx,3,zz,"grass_block",snowy=False);put(t,xx,4,zz,"midnighteve:holly_bush",age=3)

def village(name,variant):
    T=g.Template(86,30,82)
    # Main axes and a pedestrian market plaza, no repeated corridor of six cabins.
    g.path(T,6,3,79,6);g.path(T,5,24,80,29);g.path(T,5,50,80,55);g.path(T,39,6,46,75)
    g.path(T,29,30,55,49)
    # Off-axis west/east streets, plus a plaza with an actual tree and bell.
    g.path(T,4,8,7,74);g.path(T,78,8,81,74)
    roles=[
      ("Baker","farmer","white_terracotta","dark_oak","shop"),
      ("Woodworker","toolsmith","spruce_planks","spruce","workshop"),
      ("Chandler","cleric","red_terracotta","dark_oak","shop"),
      ("Postmaster","librarian","stripped_birch_wood","spruce","hall"),
      ("Giftkeeper","librarian","white_terracotta","dark_oak","shop"),
      ("Fisher","fisherman","spruce_planks","spruce","workshop"),
      ("Ranger","butcher","spruce_planks","dark_oak","house")]
    plans=[(10,10,13,12),(25,10,11,12),(49,10,13,12),(64,10,12,12),
           (10,33,12,14),(24,33,12,14),(56,33,10,14),(68,33,9,14),
           (10,60,12,13),(25,60,11,13),(49,60,12,13),(65,60,11,13)]
    # Plans vary in roles, materials, roof ridge axes and furniture.
    for i,(x,z,W,L) in enumerate(plans):
        role,job,wall,roof,style=roles[(i+variant*3)%len(roles)]
        house(T,x,z,W,L,role,job,wall,roof,style,extra=(i%3==0))
    # Central public square: festive tree, hearth, bell, stalls, benches and lighting.
    g.tree(T,42,37)
    g.festival(T,42,46)
    put(T,35,4,41,"midnighteve:celebration_bell",ringing=False)
    for x,z in ((31,31),(52,31),(31,48),(53,48),(8,26),(76,26),(8,52),(76,52),(42,59),(42,70)):
        t.lamp(T,x,z)
    for x,z in ((32,35),(51,35),(32,43),(51,43)):
        seat(T,x,z,"north");table(T,x,z+1)
    for x,z in ((36,32),(48,32),(34,45),(50,45)):
        put(T,x,4,z,"midnighteve:gift_crate")
    for x in (30,54):
      for z in (33,36,39,42):
        put(T,x,3,z,"grass_block",snowy=False);put(T,x,4,z,"midnighteve:frostberry_bush" if variant else "midnighteve:holly_bush",age=3)
    # A sheltered market counter and seasonal stalls along the approach.
    for x,z in ((8,75),(25,75),(51,75),(68,75)):
        # 7x4 counter footprints fit inside the template; not another house.
        t.stall(T,x,z,"cake" if x%2 else "midnighteve:gift_crate")
    # No overlap of interior beds/workstations with doors. Population 16.
    assert len(T.entities)==16 and len(T.doors)==12
    T.save(name)
village("everlight_hamlet",0);village("everlight_hamlet_b",1)
p=D/"worldgen/template_pool/everlight_hamlet.json";pool=json.loads(p.read_text())
pool["elements"].append({"weight":1,"element":{"element_type":"minecraft:single_pool_element","location":mod("everlight_hamlet_b"),"processors":"minecraft:empty","projection":"rigid"}})
write(p,pool)

# Existing landmarks: add role-specific furnishings in valid air spaces near residents,
# avoiding doors, beds, main aisles and existing workstations. Each landmark gets its own
# type-specific furnishing choices; no identical furniture stamp.
themes={
"frostmarket":["midnighteve:gift_crate","midnighteve:candle_cluster","bookshelf"],
"evergreen_lodge":["barrel","bookshelf","midnighteve:garland"],
"northwatch":["barrel","crafting_table","bookshelf"],
"winter_post":["bookshelf","cartography_table","midnighteve:gift_crate"],
"candle_hall":["midnighteve:candle_cluster","midnighteve:ornament","bookshelf"],
"fjord_trading_post":["barrel","crafting_table","midnighteve:gift_crate"],
"lantern_fair":["midnighteve:gift_crate","midnighteve:candle_cluster"],
"mountain_refuge":["barrel","bookshelf","midnighteve:garland"],
"yuletide_longhouse":["midnighteve:candle_cluster","midnighteve:gift_crate"],
"icefish_station":["barrel","crafting_table"],
"stargazer_observatory":["bookshelf","cartography_table","midnighteve:candle_cluster"],
"winter_conservatory":["composter","barrel","midnighteve:holly_bush"],
"snow_gallery":["midnighteve:ornament","midnighteve:gift_crate"],
"solstice_shrine":["midnighteve:candle_cluster","midnighteve:ornament"],
"hollycroft_farm":["barrel","composter","midnighteve:gift_crate"]}
solid={"minecraft:spruce_planks","minecraft:dark_oak_planks","minecraft:cobblestone","minecraft:stone_bricks","minecraft:white_terracotta","minecraft:red_terracotta","minecraft:stripped_spruce_log","minecraft:stripped_birch_wood","minecraft:polished_andesite",mod("froststone_bricks")}
furnish=0
for file in sorted((D/"structure").glob("*.nbt")):
    root=f.read_nbt(gzip.decompress(file.read_bytes()));palette=root["palette"][1][1];blocks=root["blocks"][1][1]
    idx={tuple(b["pos"][1][1]):b for b in blocks}
    def name(pos):
        b=idx.get(pos);return palette[b["state"][1]]["Name"][1] if b else ""
    def setstate(pos,n,props=None):
        entry={"Name":g.Str(g.mc(n))}
        if props:entry["Properties"]=g.C({k:g.Str(str(v).lower()) for k,v in props.items()})
        if entry not in palette:palette.append(entry)
        idx[pos]["state"]=g.I(palette.index(entry))
    if file.stem in themes:
        for ent in root["entities"][1][1]:
            nbt=ent["nbt"][1]
            if nbt["id"][1]!="minecraft:villager":continue
            x,y,z=ent["blockPos"][1][1]
            nbt["Xp"]=g.I(10)
            candidates=[]
            for dx in range(-5,6):
              for dz in range(-5,6):
                if not 2<=abs(dx)+abs(dz)<=5:continue
                p=(x+dx,y,z+dz);px,py,pz=p
                if name(p)!="minecraft:air" or name((px,py+1,pz))!="minecraft:air" or name((px,py-1,pz)) not in solid:continue
                sides=[(px+1,py,pz),(px-1,py,pz),(px,py,pz+1),(px,py,pz-1)]
                if any(name(a).endswith("_door") or name(a).endswith("_bed") for a in sides):continue
                if not any(name(a) in solid for a in sides) or sum(name(a)=="minecraft:air" for a in sides)<2:continue
                candidates.append((abs(dx)+abs(dz),p))
            for n,(_,p) in zip(themes[file.stem],sorted(candidates)[:2]):
                if n.endswith("holly_bush"):continue
                props={"facing":"up","open":False} if n=="barrel" else {"level":0} if n=="composter" else {}
                setstate(p,n,props);furnish+=1
    # Remove two buried foundation courses, not merely lowering structure height.
    # The native ground level delta is one; floor at local Y=1 aligns with that
    # plane instead of sitting two blocks above it. All entities and doors shift too.
    kept=[]
    for b in blocks:
        x,y,z=b["pos"][1][1]
        if y>=2:
            b["pos"]=g.L(3,[x,y-2,z]);kept.append(b)
    for ent in root["entities"][1][1]:
        x,y,z=ent["pos"][1][1];ent["pos"]=g.L(6,[x,y-2,z])
        x,y,z=ent["blockPos"][1][1];ent["blockPos"]=g.L(3,[x,y-2,z])
    size=root["size"][1][1];root["size"]=g.L(3,[size[0],size[1]-2,size[2]])
    root["blocks"]=g.L(10,kept)
    file.write_bytes(gzip.compress(b"\x0a\x00\x00"+g.payload(g.C(root)),mtime=0))
# Favor the native terrain blending surface instead of the old raised foundation.
# Keep positive jigsaw depth and all placement IDs.
for path in (D/"worldgen/structure").glob("*.json"):
    d=json.loads(path.read_text())
    if d.get("type")=="minecraft:jigsaw":
        d["terrain_adaptation"]="beard_thin"
        d["start_height"]={"absolute":-1}
        write(path,d)
print("PASS: 12-building Everlight variants, 16 residents each, all template floors at local y=1, landmark furnishings",furnish)
