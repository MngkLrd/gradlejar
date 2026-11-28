package dev.minemods.worldlogic;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.fml.ModList;
import dev.minemods.worldlogic.api.WorldLogicApi;
final class HealChecks{
 private static void require(boolean value,String detail){if(!value)throw new IllegalStateException("heal: "+detail);}
 static void verify(ServerLevel l){
  var p=FakePlayerFactory.getMinecraft(l);
  float oldHealth=p.getHealth(),oldSaturation=p.getFoodData().getSaturationLevel();int oldFood=p.getFoodData().getFoodLevel();
  var tag=p.getPersistentData();var old=tag.copy();
  try{
   tag.putDouble("worldlogic_exertion",80);tag.putLong("worldlogic_injury",l.getGameTime()+500);
   tag.putInt("worldlogic_smoke",60);tag.putLong("worldlogic_arm_until",l.getGameTime()+500);tag.putDouble("worldlogic_impact_load",60);
   tag.putLong("worldlogic_combat_heal_until",l.getGameTime()+500);
   p.setHealth(Math.max(1,p.getMaxHealth()/2));p.getFoodData().setFoodLevel(5);p.getFoodData().setSaturation(0);
   var result=WorldLogicApi.adminRestore(p);
   require(result.restored().contains("vanilla/Core"),"Core reports actual reset");
   require(p.getHealth()==p.getMaxHealth()&&p.getFoodData().getFoodLevel()==20&&Survival.fatigue(p)==0,"health, food and stamina");
   for(String key:java.util.List.of("worldlogic_injury","worldlogic_smoke","worldlogic_arm_until","worldlogic_impact_load","worldlogic_combat_heal_until"))require(!tag.contains(key),"cleared "+key);
   if(java.util.stream.Stream.of("worldlogic_trauma","hydration","worldlogic_thermal","metabolism").noneMatch(id->ModList.get().isLoaded(id)))require(result.complete(),"absent optional addons do not fail heal");
  }finally{
   for(String key:java.util.List.of("worldlogic_exertion","worldlogic_injury","worldlogic_smoke","worldlogic_arm_until","worldlogic_impact_load","worldlogic_combat_heal_until")){
    tag.remove(key);if(old.contains(key))tag.put(key,old.get(key).copy());
   }
   p.setHealth(oldHealth);p.getFoodData().setFoodLevel(oldFood);p.getFoodData().setSaturation(oldSaturation);
   ExertionModifiers.apply(p);
  }
 }
}
