package dev.minemods.sleepdebt.gameplay;

import dev.minemods.sleepdebt.registry.ModBlocks;
import dev.minemods.sleepdebt.SleepSettings;
import dev.minemods.sleepdebt.registry.ModItems;
import dev.minemods.sleepdebt.blockentity.IncenseBurnerBlockEntity;
import dev.minemods.sleepdebt.integration.SleepAccessoriesBridge;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;

/**
 * Environment-based quality. Scans only when sleep completes or a rest starts,
 * not per player tick. Supports modded biomes through vanilla temperature.
 */
public final class RestEnvironment {
 public record Conditions(int quality,boolean sheltered,boolean threatened,
  boolean warm,boolean incense,boolean dreamcatcher,boolean pillow,boolean mask,
  int reasons) {}
 public static final int RAIN=1,COLD=2,HEAT=4,HOSTILES=8,OPEN_AIR=16,
  COMFORT=32,PROTECTION=64,STRESS=128,CAFFEINE=256;

 public static boolean threatened(ServerPlayer p,double radius){
  return p.serverLevel().getEntitiesOfClass(Monster.class,p.getBoundingBox().inflate(radius),m->m.isAlive()&&(m.getTarget()==p||m.hasLineOfSight(p))).stream().anyMatch(m->m.hasLineOfSight(p));
 }
 public static Conditions evaluate(ServerPlayer player,BlockPos pos){
  var level=player.serverLevel();
  boolean sheltered=!level.canSeeSky(pos.above());
  boolean threatened=threatened(player,11);
  boolean rain=!sheltered&&level.isRainingAt(pos.above());
  float temperature=level.getBiome(pos).value().getBaseTemperature();
  boolean cold=!sheltered&&temperature<.15f;
  boolean hot=!sheltered&&temperature>1.8f;
  boolean campfire=false,lantern=false,incense=false,dreamcatcher=false;
  for(BlockPos scan:BlockPos.betweenClosed(pos.offset(-5,-2,-5),pos.offset(5,3,5))){
   var state=level.getBlockState(scan);
   if((state.is(Blocks.CAMPFIRE)||state.is(Blocks.SOUL_CAMPFIRE))&&state.getValue(net.minecraft.world.level.block.CampfireBlock.LIT))campfire=true;
   if(state.is(ModBlocks.CAMP_LANTERN.get()))lantern=true;
   if(state.is(ModBlocks.DREAMCATCHER.get()))dreamcatcher=true;
   if(!incense&&state.is(ModBlocks.INCENSE_BURNER.get())&&
      level.getBlockEntity(scan) instanceof IncenseBurnerBlockEntity burner
      &&burner.remainingTicks()>0)incense=true;
  }
  boolean pillow=player.getInventory().items.stream()
    .anyMatch(stack->stack.is(ModItems.PILLOW.get()));
  boolean mask=player.getInventory().items.stream()
    .anyMatch(stack->stack.is(ModItems.MASK.get()));
  if(!mask&&ModList.get().isLoaded("accessories")){
   mask=SleepAccessoriesBridge.hasMask(player);
  }
  boolean warm=campfire;
  int quality=100,reasons=0;
  if(sheltered){if(SleepSettings.SHELTER_BONUS.get())quality+=14;reasons|=COMFORT;}else{quality-=8;reasons|=OPEN_AIR;}
  if(threatened){if(SleepSettings.MONSTER_PENALTY.get())quality-=24;reasons|=HOSTILES;}
  if(rain){quality-=18;reasons|=RAIN;}
  if(cold){quality-=17;reasons|=COLD;}
  if(hot){quality-=10;reasons|=HEAT;}
  if(warm){quality+=6;reasons|=COMFORT;}
  if(lantern){quality+=3;reasons|=COMFORT;}
  if(dreamcatcher){quality+=10;reasons|=PROTECTION;}
  if(incense){quality+=10;reasons|=COMFORT;}
  if(pillow){quality+=6;reasons|=COMFORT;}
  if(mask){quality+=6;reasons|=COMFORT;}
  int stress=SleepEngine.getStress(player);
  if(stress>350){quality-=Math.min(32,(int)((stress-300)/23.0*SleepSettings.STRESS_PENALTY.get()));reasons|=STRESS;}
  int caffeine=SleepEngine.getCaffeine(player);
  if(caffeine>=65){quality-=Math.min(17,(caffeine-40)/4);reasons|=CAFFEINE;}
  return new Conditions(Math.max(35,Math.min(150,quality)),sheltered,threatened,
   warm,incense,dreamcatcher,pillow,mask,reasons);
 }
 private RestEnvironment(){}
}
