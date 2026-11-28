package dev.minemods.aquaculture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.util.random.WeightedRandomList;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;
final class AquaContentChecks {
 private static void require(boolean value,String what){if(!value)throw new IllegalStateException(what);}
 static void run(ServerLevel l){
  require(Aquaculture.RAW.size()==36&&Aquaculture.EGGS.size()==44&&Aquaculture.ITEMS.getEntries().size()==80,"registered items");
  require(Aquaculture.AQUA_TAB.get()!=null,"creative tab");
  var hashes=new HashSet<String>();
  for(Species s:Species.ALL){
   egg(s.id(),Aquaculture.TYPES.get(s.id()).get());
   try(var png=AquaContentChecks.class.getResourceAsStream("/assets/aquaculture/textures/item/raw_"+s.id()+".png");
       var model=AquaContentChecks.class.getResourceAsStream("/assets/aquaculture/models/item/raw_"+s.id()+".json")){
    require(png!=null&&model!=null,"missing catch assets "+s.id());
    byte[] bytes=png.readAllBytes();
    var d=new java.io.DataInputStream(new java.io.ByteArrayInputStream(bytes));
    require(d.readLong()==0x89504e470d0a1a0aL&&d.readInt()==13&&d.readInt()==0x49484452&&d.readInt()==16&&d.readInt()==16,"catch PNG "+s.id());
    require(hashes.add(java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(bytes))),"duplicate catch sprite "+s.id());
    var json=com.google.gson.JsonParser.parseString(new String(model.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    require(json.getAsJsonObject("textures").get("layer0").getAsString().equals("aquaculture:item/raw_"+s.id()),"catch model reference "+s.id());
   }catch(Exception e){throw new IllegalStateException("catch assets "+s.id(),e);}
  }
  for(MarineSpecies s:MarineSpecies.values())egg(s.id,Aquaculture.MARINE.get(s).get());
  var types=List.of(EntityType.COD,EntityType.SALMON,EntityType.TROPICAL_FISH,EntityType.PUFFERFISH);
  var entries=new ArrayList<MobSpawnSettings.SpawnerData>();
  for(var type:types)entries.add(new MobSpawnSettings.SpawnerData(type,10,1,1));
  entries.add(new MobSpawnSettings.SpawnerData(EntityType.SQUID,10,1,1));
  var pos=new BlockPos(8,110,8);
  var candidates=new LevelEvent.PotentialSpawns(l,MobCategory.WATER_AMBIENT,pos,WeightedRandomList.create(entries));
  NeoForge.EVENT_BUS.post(candidates);
  require(candidates.getSpawnerDataList().size()==1&&candidates.getSpawnerDataList().getFirst().type==EntityType.SQUID,"natural candidate filtering");
  for(var type:types){
   for(var reason:List.of(MobSpawnType.NATURAL,MobSpawnType.CHUNK_GENERATION)){
    var check=new MobSpawnEvent.SpawnPlacementCheck(type,l,reason,pos,l.random,true);NeoForge.EVENT_BUS.post(check);
    require(!check.getPlacementCheckResult(),"vanilla natural spawn "+type);
   }
   var manual=new MobSpawnEvent.SpawnPlacementCheck(type,l,MobSpawnType.SPAWN_EGG,pos,l.random,true);NeoForge.EVENT_BUS.post(manual);
   require(manual.getPlacementCheckResult(),"manual spawn preserved "+type);
  }
  var custom=new MobSpawnEvent.SpawnPlacementCheck(Aquaculture.TYPES.get("perch").get(),l,MobSpawnType.NATURAL,pos,l.random,true);NeoForge.EVENT_BUS.post(custom);
  require(custom.getPlacementCheckResult(),"Aqua spawn untouched");
 }
 private static void egg(String id,EntityType<?> type){
  var item=Aquaculture.EGGS.get(id).get();
  require(item instanceof SpawnEggItem&&((SpawnEggItem)item).getType(new ItemStack(item))==type,"spawn egg "+id);
  require(AquaContentChecks.class.getResource("/assets/aquaculture/models/item/"+id+"_spawn_egg.json")!=null,"missing egg model "+id);
  if(Aquaculture.RAW.containsKey(id))require(AquaContentChecks.class.getResource("/assets/aquaculture/models/item/raw_"+id+".json")!=null,"missing catch model "+id);
 }
}
