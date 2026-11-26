package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PressurePlateBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PressurePlateBlock.class,remap=false)
public abstract class StonePlateMixin {
 @Inject(method="getSignalStrength",at=@At("HEAD"),cancellable=true,remap=false)
 private void worldlogic$weight(Level level,BlockPos pos,CallbackInfoReturnable<Integer> cir){
  if(level.isClientSide||!Rules.on("weighted_redstone"))return;
  var state=level.getBlockState(pos);
  if(state.is(Blocks.STONE_PRESSURE_PLATE)||state.is(Blocks.POLISHED_BLACKSTONE_PRESSURE_PLATE))cir.setReturnValue(PlatePhysics.mass(level,pos)>=.65?15:0);
 }
}
