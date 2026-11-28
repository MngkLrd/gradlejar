package dev.minemods.metabolism.mixin;
import dev.minemods.metabolism.Metabolism;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(CakeBlock.class)
public abstract class CakeFoodMixin {
 @Inject(method="eat",at=@At("RETURN"))
 private static void metabolism$eatSlice(LevelAccessor level,BlockPos pos,BlockState state,Player p,CallbackInfoReturnable<InteractionResult> cir){
  if(p instanceof ServerPlayer sp&&cir.getReturnValue().consumesAction())Metabolism.cake(sp);
 }
}
