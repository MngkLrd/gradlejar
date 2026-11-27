package dev.minemods.worldlogic;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
public final class AnimalCare {
 private static final String KEY="worldlogic_hunger";
 private static final TagKey<EntityType<?>> FARM=TagKey.create(Registries.ENTITY_TYPE,ResourceLocation.fromNamespaceAndPath("worldlogic","farm_animals"));
 static boolean farm(Animal a){return a.getType().is(FARM);}
 static int hunger(Animal a){return Math.max(0,Math.min(100,a.getPersistentData().getInt(KEY)));}
 public static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){
  if(!Rules.on("animal_hunger")||!(e.getEntity() instanceof Animal a)||!(a.level() instanceof ServerLevel l)||!farm(a)||a.isBaby()||a.tickCount%200!=0)return;
  int v=hunger(a);
  // Grass access supplies passive grazing without destroying the landscape.
  boolean grazing=l.getBlockState(a.getOnPos()).is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)&&l.getMaxLocalRawBrightness(a.blockPosition())>=7;
  int next=grazing?Math.max(0,v-2):Math.min(100,v+1);if(next!=v)a.getPersistentData().putInt(KEY,next);
 }
 public static void interact(PlayerInteractEvent.EntityInteract e){
  if(e.isCanceled()||!Rules.on("animal_hunger")||!(e.getTarget() instanceof Animal a)||!farm(a)||a.isBaby()||!Survival.active(e.getEntity()))return;
  int h=hunger(a);if(h<60)return;Player p=e.getEntity();
  if(a.isFood(e.getItemStack())){
   // Feeding a hungry adult restores condition before normal breeding can resume.
   e.getItemStack().shrink(1);a.getPersistentData().putInt(KEY,Math.max(0,h-45));
   if(a.level() instanceof ServerLevel l)l.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,a.getX(),a.getY()+.7,a.getZ(),5,.2,.2,.2,.01);
   p.displayClientMessage(Component.translatable("message.worldlogic.animal_fed"),true);
   e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
  }else if(a instanceof Cow&&e.getItemStack().is(Items.BUCKET)){
   p.displayClientMessage(Component.translatable("message.worldlogic.animal_hungry"),true);
   e.setCancellationResult(InteractionResult.SUCCESS);e.setCanceled(true);
  }
 }
}
