#!/usr/bin/env python3
"""Final 0.7 content stage: landmarks with exploration treasure and discovery ledger."""
import expand_tales as t
g=t.g
S,R,D,A,J=t.S,t.R,t.D,t.A,t.J
T=g.Template
write,text,mc,mod=t.write,t.text,t.mc,t.mod

def loot_chest(tmpl,x,y,z,table):
    tmpl.put(x,y,z,"chest",facing="north",type="single",waterlogged=False)
    tmpl.block_nbt[x,y,z]={"id":g.Str("minecraft:chest"),"LootTable":g.Str(mod("chests/"+table))}
def treasure(name,items):
    write(D/"loot_table/chests"/(name+".json"),{"type":"minecraft:chest","pools":[{"rolls":{"type":"minecraft:uniform","min":2,"max":4},"entries":[{"type":"minecraft:item","name":mc(n),"weight":w,"functions":[{"function":"minecraft:set_count","count":{"type":"minecraft:uniform","min":a,"max":b}}]} for n,w,a,b in items]}]})
# Observatory: polygonal masonry, spiral-access upper gallery and capped copper dome.
obs=T(29,29,29)
g.path(obs,11,3,17,6)
cx=cz=14
def octagon(x,z,r):return abs(x)<=r and abs(z)<=r and abs(x)+abs(z)<=r+3
for x in range(-7,8):
 for z in range(-7,8):
    if not octagon(x,z,7):continue
    obs.fill(cx+x,0,cz+z,cx+x,3,cz+z,"stone_bricks")
    edge=any(not octagon(x+dx,z+dz,7) for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)))
    if edge:
        for y in range(4,15):
            mat="stone_bricks" if y in (4,8,13,14) else "polished_andesite"
            if y in (6,7,10,11) and ((abs(x)==7 and abs(z)<=1) or (abs(z)==7 and abs(x)<=1)):mat="blue_stained_glass"
            obs.put(cx+x,y,cz+z,mat)
    else:
        obs.fill(cx+x,4,cz+z,cx+x,14,cz+z,"air")
        obs.put(cx+x,9,cz+z,"spruce_planks")
for y,half in ((4,"lower"),(5,"upper")):obs.put(14,y,7,"spruce_door",facing="north",half=half,hinge="left",open=False,powered=False)
obs.doors.append((14,4,7));obs.rooms.append((14,4,8))
# Ladder access through an open shaft, with solid backing.
for y in range(4,14):
    obs.put(19,y,14,"stone_bricks")
    obs.put(18,y,14,"ladder",facing="west",waterlogged=False)
obs.put(17,9,14,"air")
obs.put(18,9,15,"air")
for y,r in [(15,7),(16,6),(17,6),(18,5),(19,4),(20,3),(21,2),(22,1)]:
    for x in range(-r,r+1):
     for z in range(-r,r+1):
      if octagon(x,z,r):
       obs.put(cx+x,y,cz+z,"oxidized_copper" if (abs(x)+abs(z))%4 else "weathered_copper")
obs.put(14,23,14,"midnighteve:tree_star")
obs.put(11,10,14,"lectern",facing="east",has_book=False,powered=False)
obs.put(14,10,11,"cartography_table")
for x,z in ((10,10),(18,10),(10,18),(18,18)):obs.put(x,8,z,"lantern",hanging=True,waterlogged=False)
loot_chest(obs,11,4,18,"observatory")
loot_chest(obs,12,10,17,"observatory")
obs.save("stargazer_observatory")
t.register("stargazer_observatory",["crystal_crown","starlit_steppe"],44,15,712028,"Звёздная обсерватория")
treasure("observatory",[("amethyst_shard",8,2,6),("midnighteve:wintermark",10,1,4),("midnighteve:tree_star",2,1,1),("spyglass",2,1,1),("paper",6,3,8)])

# Conservatory: transparent arched roof and distinct central irrigated growing beds.
glass=T(29,22,29)
g.path(glass,12,3,16,6)
glass.fill(5,0,7,23,3,24,"stone_bricks")
glass.fill(5,4,7,23,10,24,"air")
for x in range(5,24):
 for z in range(7,25):
    if x in (5,23) or z in (7,24):
      for y in range(4,10):
        glass.put(x,y,z,"stripped_spruce_log" if y==4 or y==9 or x in (5,23) and (z-7)%4==0 else "glass",**({"axis":"y"} if (y==4 or y==9 or x in (5,23) and (z-7)%4==0) else {}))
for x in range(4,25):
    y=10+min(x-4,24-x)//2
    for z in range(6,26):
        glass.put(x,y,z,"spruce_planks" if z in (6,10,15,20,25) else "glass")
        if 5<=x<=23 and z in (7,24):
            for yy in range(10,y):glass.put(x,yy,z,"glass")
for y,half in ((4,"lower"),(5,"upper")):glass.put(14,y,7,"spruce_door",facing="north",half=half,hinge="left",open=False,powered=False)
glass.doors.append((14,4,7));glass.rooms.append((14,4,8))
for x0,x1 in ((7,11),(17,21)):
 for x in range(x0,x1+1):
    for z in range(10,22):
        if x==(x0+x1)//2:glass.put(x,3,z,"water",level=0)
        else:
            glass.put(x,3,z,"farmland",moisture=7)
            glass.put(x,4,z,"wheat" if z<16 else "carrots",age=7)
for x,z in ((12,11),(16,11),(12,20),(16,20)):glass.put(x,4,z,"midnighteve:candle_cluster")
glass.put(13,4,22,"composter",level=0)
glass.resident(14,4,19,"Baker","farmer")
loot_chest(glass,15,4,22,"conservatory")
glass.save("winter_conservatory")
t.register("winter_conservatory",["peppermint_grove","firlight_woods"],43,15,712029,"Зимняя оранжерея")
treasure("conservatory",[("wheat",10,4,12),("cocoa_beans",8,2,6),("sweet_berries",5,3,7),("midnighteve:gingerbread",6,2,5),("midnighteve:wintermark",4,1,3)])

# Sheltered snow gallery: long timber arcade and a small closed checkpoint.
gallery=T(23,20,39)
g.path(gallery,9,3,13,34)
gallery.fill(8,0,5,14,3,30,"stone_bricks")
for z in range(5,31):
    for x in (8,14):
        if (z-5)%5==0 or z==30:
            gallery.fill(x,4,z,x,9,z,"stripped_spruce_log",axis="y")
        else:gallery.put(x,4,z,"stone_bricks")
    gallery.put(11,3,z,"spruce_planks")
for x in range(7,16):
 for z in range(4,32):
    h=10+min(x-7,15-x)
    if x==11:gallery.put(x,h,z,"dark_oak_slab",type="bottom",waterlogged=False)
    else:gallery.put(x,h,z,"dark_oak_stairs",facing="east" if x<11 else "west",half="bottom",shape="straight",waterlogged=False)
for z in (10,20,30):
    gallery.put(11,10,z,"spruce_planks")
    gallery.put(11,9,z,"lantern",hanging=True,waterlogged=False)
    gallery.put(8,5,z,"midnighteve:garland")
g.cabin(gallery,3,32,7,5,"Postmaster","spruce_planks","spruce","librarian")
loot_chest(gallery,12,4,28,"gallery")
gallery.save("snow_gallery")
t.register("snow_gallery",["silent_snowfields","firlight_woods"],41,14,712030,"Снежная галерея")
treasure("gallery",[("midnighteve:sealed_parcel",4,1,2),("midnighteve:hot_cocoa",7,1,3),("paper",9,4,12),("midnighteve:wintermark",8,1,4),("bread",8,2,6)])

# Solstice shrine: layered stone ring, arch, recessed central altar and buried cache.
shrine=T(27,22,27)
g.path(shrine,11,2,15,6)
for x in range(-9,10):
 for z in range(-9,10):
    d=x*x+z*z
    if d<=81:
        shrine.fill(13+x,0,13+z,13+x,2,13+z,"cobblestone")
        shrine.put(13+x,3,13+z,"polished_andesite" if d<32 else "stone_bricks")
        if 55<=d<=81 and ((x+z)%3!=0):shrine.put(13+x,4,13+z,"stone_brick_slab",type="bottom",waterlogged=False)
for x,z in ((6,8),(20,8),(6,18),(20,18)):
    shrine.fill(x,4,z,x,10,z,"stone_bricks")
    shrine.put(x,11,z,"stone_brick_slab",type="bottom",waterlogged=False)
    shrine.put(x,12,z,"midnighteve:candle_cluster")
for x in range(9,18):
    y=8+min(x-9,17-x)
    shrine.put(x,y,20,"stone_bricks")
    shrine.put(x,y+1,20,"stone_brick_slab",type="bottom",waterlogged=False)
for x in (9,17):shrine.fill(x,4,20,x,8,20,"stone_bricks")
shrine.fill(11,4,17,15,4,19,"polished_andesite")
shrine.put(13,5,18,"midnighteve:tree_star")
loot_chest(shrine,11,4,19,"solstice")
loot_chest(shrine,15,4,19,"solstice")
for x,z in ((10,10),(16,10),(10,16),(16,16)):t.lamp(shrine,x,z)
shrine.save("solstice_shrine")
t.register("solstice_shrine",["crystal_crown","silent_snowfields","starlit_steppe"],47,16,712031,"Святилище солнцестояния")
treasure("solstice",[("midnighteve:wintermark",12,2,5),("midnighteve:wrapped_gift",7,1,3),("midnighteve:tree_star",4,1,1),("gold_ingot",3,1,2),("amethyst_shard",6,2,5)])

# Native structure discovery tag: only the mod's own structures are tracked.
structures=sorted(p.stem for p in (D/"worldgen/structure").glob("*.json"))
write(D/"tags/worldgen/structure/discoverable.json",{"replace":False,"values":[mod(n) for n in structures]})
regions=",".join('"'+n+'"' for n in g.names)
landmarks=",".join('"'+n+'"' for n in structures)
text(J/"quest/WinterExploration.java",'''package dev.mngklrd.midnighteve.quest;
import dev.mngklrd.midnighteve.MidnightEve;
import dev.mngklrd.midnighteve.content.TalesContent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import java.util.List;

public final class WinterExploration {
    private static final String DATA="midnighteve_exploration_v1";
    private static final List<String> REGIONS=List.of('''+regions+''');
    private static final List<String> LANDMARKS=List.of('''+landmarks+''');
    private static final TagKey<Structure> DISCOVERABLE=TagKey.create(Registries.STRUCTURE,ResourceLocation.fromNamespaceAndPath("midnighteve","discoverable"));
    private static CompoundTag data(Player p){
        var root=p.getPersistentData();
        if(!root.contains(DATA))root.put(DATA,new CompoundTag());
        return root.getCompound(DATA);
    }
    public static void clonePlayer(PlayerEvent.Clone e){
        if(e.getOriginal().getPersistentData().contains(DATA))
            e.getEntity().getPersistentData().put(DATA,e.getOriginal().getPersistentData().getCompound(DATA).copy());
    }
    private static void give(ServerPlayer p,ItemStack stack){if(!p.addItem(stack))p.drop(stack,false);}
    private static int count(CompoundTag tag,List<String> names,String prefix){
        int n=0;for(String name:names)if(tag.getBoolean(prefix+name))n++;return n;
    }
    public static void visit(PlayerTickEvent.Post event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.isSpectator()||p.tickCount%80!=0)return;
        var level=p.serverLevel();
        var key=level.getBiome(p.blockPosition()).unwrapKey();
        if(key.isEmpty()||!key.get().location().getNamespace().equals("midnighteve"))return;
        String biome=key.get().location().getPath();
        if(!REGIONS.contains(biome))return;
        CompoundTag data=data(p);
        if(!data.getBoolean("biome_"+biome)){
            data.putBoolean("biome_"+biome,true);
            give(p,new ItemStack(MidnightEve.WINTERMARK.get()));
            p.displayClientMessage(Component.translatable("explore.midnighteve.region",Component.translatable("biome.midnighteve."+biome),count(data,REGIONS,"biome_"),REGIONS.size()),false);
        }
        // The structure manager reads native generated piece references, not a global block scan.
        var start=level.structureManager().getStructureWithPieceAt(p.blockPosition(),DISCOVERABLE);
        if(start.isValid()){
            var id=level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
            if(id!=null&&id.getNamespace().equals("midnighteve")&&LANDMARKS.contains(id.getPath())){
                String name=id.getPath();
                if(!data.getBoolean("structure_"+name)){
                    data.putBoolean("structure_"+name,true);
                    give(p,new ItemStack(MidnightEve.WINTERMARK.get(),2));
                    p.displayClientMessage(Component.translatable("explore.midnighteve.landmark",Component.translatable("structure.midnighteve."+name),count(data,LANDMARKS,"structure_"),LANDMARKS.size()),false);
                }
            }
        }
        if(count(data,REGIONS,"biome_")==REGIONS.size()&&!data.getBoolean("all_regions")){
            data.putBoolean("all_regions",true);give(p,new ItemStack(TalesContent.STAR_ITEM.get(),2));
            p.displayClientMessage(Component.translatable("explore.midnighteve.all_regions"),false);
        }
        if(count(data,LANDMARKS,"structure_")==LANDMARKS.size()&&!data.getBoolean("all_landmarks")){
            data.putBoolean("all_landmarks",true);give(p,new ItemStack(MidnightEve.WRAPPED_GIFT.get(),8));
            p.displayClientMessage(Component.translatable("explore.midnighteve.all_landmarks"),false);
        }
    }
    public static void journal(ServerPlayer p){
        CompoundTag d=data(p);
        p.sendSystemMessage(Component.translatable("explore.midnighteve.summary",count(d,REGIONS,"biome_"),REGIONS.size(),count(d,LANDMARKS,"structure_"),LANDMARKS.size()));
        for(String n:REGIONS)if(!d.getBoolean("biome_"+n))p.sendSystemMessage(Component.translatable("explore.midnighteve.unvisited",Component.translatable("biome.midnighteve."+n)));
        for(String n:LANDMARKS)if(!d.getBoolean("structure_"+n))p.sendSystemMessage(Component.translatable("explore.midnighteve.unvisited",Component.translatable("structure.midnighteve."+n)));
    }
}
''')
p=J/"content/TalesContent.java"
t.edit(p,"        NeoForge.EVENT_BUS.addListener(WinterQuests::clonePlayer);","        NeoForge.EVENT_BUS.addListener(WinterQuests::clonePlayer);\n        NeoForge.EVENT_BUS.addListener(dev.mngklrd.midnighteve.quest.WinterExploration::visit);\n        NeoForge.EVENT_BUS.addListener(dev.mngklrd.midnighteve.quest.WinterExploration::clonePlayer);")
p=J/"item/WinterJournal.java"
t.edit(p,"if(player instanceof ServerPlayer p)WinterQuests.journal(p);","if(player instanceof ServerPlayer p){if(p.isShiftKeyDown())dev.mngklrd.midnighteve.quest.WinterExploration.journal(p);else WinterQuests.journal(p);}")
g.en.update({"explore.midnighteve.region":"Region discovered: %s (%s/%s)","explore.midnighteve.landmark":"Landmark discovered: %s (%s/%s)","explore.midnighteve.all_regions":"Winter Wanderer: all regions discovered!","explore.midnighteve.all_landmarks":"Master Explorer: all landmarks discovered!","explore.midnighteve.summary":"Winter Atlas — regions %s/%s, landmarks %s/%s","explore.midnighteve.unvisited":"Not discovered: %s"})
g.ru.update({"explore.midnighteve.region":"Открыт регион: %s (%s/%s)","explore.midnighteve.landmark":"Открыта достопримечательность: %s (%s/%s)","explore.midnighteve.all_regions":"Зимний странник: открыты все регионы!","explore.midnighteve.all_landmarks":"Мастер исследования: открыты все достопримечательности!","explore.midnighteve.summary":"Зимний атлас — регионы %s/%s, достопримечательности %s/%s","explore.midnighteve.unvisited":"Ещё не открыто: %s"})
g.en["quest.midnighteve.instructions"]+=" Sneak + use the journal to view exploration."
g.ru["quest.midnighteve.instructions"]+=" Shift + ПКМ с журналом открывает список исследований."
for n,lang in [("en_us",g.en),("ru_ru",g.ru)]:write(A/"lang"/(n+".json"),lang)
assert len(structures)==15,structures
print("PASS: 15 native structure types, loot-table chests and 21 exploration milestones")
