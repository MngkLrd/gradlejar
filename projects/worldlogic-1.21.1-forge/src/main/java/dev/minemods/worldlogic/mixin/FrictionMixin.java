package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value=LivingEntity.class,remap=false)
public abstract class FrictionMixin {
 @Redirect(method="travel",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/block/state/BlockState;getFriction(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/Entity;)F"),remap=false,require=0)
 private float worldlogic$friction(BlockState state,LevelReader reader,BlockPos pos,Entity entity){
  float vanilla=state.getFriction(reader,pos,entity);
  if(Rules.on("slippery_rain")&&reader instanceof Level l&&l.isRainingAt(pos.above())&&Material.block(state,"slippery"))return Math.max(vanilla,.78f);
  return vanilla;
 }
}
