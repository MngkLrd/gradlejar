package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.RockContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class RockHardnessMixin{
 @Inject(method="getDestroySpeed",at=@At("RETURN"),cancellable=true)
 private void worldlogic$rockHardness(BlockGetter level,BlockPos pos,CallbackInfoReturnable<Float> cir){
  cir.setReturnValue(RockContent.hardness((BlockState)(Object)this,cir.getReturnValue()));
 }
}
