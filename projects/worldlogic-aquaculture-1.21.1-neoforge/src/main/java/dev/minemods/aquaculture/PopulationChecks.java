package dev.minemods.aquaculture;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import dev.minemods.worldlogic.api.WorldLogicApi.SeasonKind;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import java.util.*;

final class PopulationChecks {
 private static void require(boolean b,String m){if(!b)throw new IllegalStateException("Population: "+m);}
 static void run(ServerLevel l){
  var data=new PopulationData();var s=Species.BY_ID.get("roach");
  var a=PopulationData.area(new BlockPos(-1,60,0),WaterKind.LAKE);
  var b=PopulationData.area(new BlockPos(0,60,0),WaterKind.LAKE);
  UUID fish=UUID.randomUUID(),egg=UUID.randomUUID(),child=UUID.randomUUID();
  data.observe(fish,a,s);data.observe(fish,a,s);
  require(data.total(a,s.id())==1&&data.seeded(a,s),"idempotent observation");
  require(data.reserve(egg,a,s,2)&&!data.reserve(egg,a,s,2),"unique egg reservation");
  require(data.total(a,s.id())==3&&!data.reserve(UUID.randomUUID(),a,s,100),"capacity");
  var saved=data.save(new CompoundTag(),l.registryAccess());data=PopulationData.load(saved,l.registryAccess());
  require(data.total(a,s.id())==3&&data.restoreClutch(egg,a,s,2),"save/reload without duplicate");
  require(data.takeEgg(egg),"consume reservation");data.observe(child,a,s);
  require(data.total(a,s.id())==3,"birth conservation");
  data.remove(child);data.remove(fish);data.remove(egg);
  require(data.total(a)==0&&data.seeded(a,s),"extinction persists");
  data.observe(fish,a,s);data.observe(fish,b,s);require(data.total(a)==0&&data.total(b)==1,"migration");
  require(FishLife.seasonAllows(s,SeasonKind.SPRING)&&!FishLife.seasonAllows(s,SeasonKind.WINTER),"season gate");
  for(Species species:Species.ALL){
   var e=Aquaculture.TYPES.get(species.id()).get().create(l);require(e instanceof Fish,"fish "+species.id());
   Fish f=(Fish)e;FishLife.juvenile(f,species,l.getGameTime());
   require(!FishLife.mature(f,species)&&f.getPersistentData().getInt("wl_fish_age_years")==0,"juvenile "+species.id());
   float length=f.visualLength(species);FishFeeding.feedNatural(f,species,l.getGameTime()+23000);
   FishLife.age(f,species,l.getGameTime()+24000);
   require(f.getPersistentData().getLong("wl_life_age")==24000&&f.visualLength(species)>length,"age "+species.id());
   f.discard();
   var roe=Aquaculture.ROE.get().create(l);require(roe!=null,"roe factory");
   roe.initialize(species,WaterKind.LAKE,species.hatchMin());
   var nbt=new CompoundTag();roe.saveWithoutId(nbt);var copy=Aquaculture.ROE.get().create(l);copy.load(nbt);
   require(copy.species()==species&&copy.eggs()==species.hatchMin()&&copy.nursery().equals(species.nursery()),"roe reload "+species.id());
   roe.discard();copy.discard();
  }
 }
}
