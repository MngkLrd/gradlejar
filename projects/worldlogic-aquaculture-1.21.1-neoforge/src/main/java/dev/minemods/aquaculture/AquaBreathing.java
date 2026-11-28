package dev.minemods.aquaculture;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.AbstractFish;
import net.neoforged.neoforge.event.entity.living.LivingBreatheEvent;
import net.neoforged.neoforge.event.entity.living.LivingDrownEvent;

/** WaterAnimal.handleAirSupply owns air for fish; MarineAnimal owns its independent breath counter.
 * Neutralize only NeoForge's generic LivingEntity air transaction for these entities, not other mobs. */
final class AquaBreathing {
 private AquaBreathing(){}
 private static boolean ownsBreath(LivingEntity e){
  return !e.level().isClientSide&&(e instanceof AbstractFish||e instanceof MarineAnimal);
 }
 static void breathe(LivingBreatheEvent e){
  if(ownsBreath(e.getEntity())){
   e.setConsumeAirAmount(0);
   e.setRefillAirAmount(0);
  }
 }
 static void drown(LivingDrownEvent e){
  if(ownsBreath(e.getEntity()))e.setDrowning(false);
 }
}
