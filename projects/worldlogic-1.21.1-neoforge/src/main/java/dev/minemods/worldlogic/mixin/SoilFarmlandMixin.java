package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.SoilFarmland;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Restore the actual parent soil instead of losing identity on drying/trampling. */
@Mixin(FarmBlock.class)
public abstract class SoilFarmlandMixin{
 @Inject(method="turnToDirt",at=@At("HEAD"),cancellable=true)
 private static void worldlogic$restoreSoil(Entity entity,BlockState state,Level level,BlockPos pos,CallbackInfo ci){
  if(!(state.getBlock() instanceof SoilFarmland.Tilled))return;
  var restored=Block.pushEntitiesUp(state,SoilFarmland.original(state),level,pos);
  level.setBlockAndUpdate(pos,restored);
  level.gameEvent(GameEvent.BLOCK_CHANGE,pos,GameEvent.Context.of(entity,restored));
  ci.cancel();
 }
}
