package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WeightedPressurePlateBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=WeightedPressurePlateBlock.class,remap=false)
public abstract class WeightedPlateMixin {
 @Inject(method="getSignalStrength",at=@At("HEAD"),cancellable=true,remap=false)
 private void worldlogic$weight(Level level,BlockPos pos,CallbackInfoReturnable<Integer> cir){
  if(level.isClientSide||!Rules.on("weighted_redstone"))return;
  var state=level.getBlockState(pos);
  if(!state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)&&!state.is(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE))return;
  double mass=PlatePhysics.mass(level,pos);
  double full=state.is(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE)?4.5:18;
  cir.setReturnValue(Math.min(15,(int)Math.ceil(mass*15/full)));
 }
}
