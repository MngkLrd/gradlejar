package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.MarinePlants;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin({KelpBlock.class,SeagrassBlock.class})
public abstract class MarinePlantPlacementMixin {
 @Inject(method="getStateForPlacement",at=@At("HEAD"),cancellable=true)
 private void worldlogic$marinePlacement(BlockPlaceContext ctx,CallbackInfoReturnable<BlockState> cir){
  Object self=this;if(self!=Blocks.KELP&&self!=Blocks.SEAGRASS)return;
  var state=MarinePlants.placement(ctx,self==Blocks.KELP);
  if(state!=null)cir.setReturnValue(state);
 }
}
