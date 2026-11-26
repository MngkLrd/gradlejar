package dev.minemods.fragileglass.mixin;
import dev.minemods.fragileglass.gameplay.GlassRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Block.class)
public abstract class FallMixin {
 @Inject(method="fallOn",at=@At("HEAD"))
 private void fragileglass$fall(Level level,BlockState state,BlockPos pos,Entity entity,double distance,CallbackInfo ci){
   if(distance>=3 && entity instanceof LivingEntity && level instanceof ServerLevel server) GlassRules.shatter(server,pos,entity);
 }
}
