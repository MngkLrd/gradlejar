package dev.minemods.worldlogic.mixin;
import java.util.ArrayDeque;
import dev.minemods.worldlogic.Survival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ServerPlayerGameMode.class,remap=false)
public abstract class CompletedMiningMixin {
 @Shadow @Final protected ServerPlayer player;
 private record Attempt(BlockState state,float hardness){}
 @Unique private final ArrayDeque<Attempt> worldlogic$attempts=new ArrayDeque<>();
 @Inject(method="destroyBlock",at=@At("HEAD"),remap=false)
 private void worldlogic$start(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
  var l=player.serverLevel();var state=l.getBlockState(pos);
  worldlogic$attempts.addLast(new Attempt(state,state.getDestroySpeed(l,pos)));
 }
 @Inject(method="destroyBlock",at=@At("RETURN"),remap=false)
 private void worldlogic$finish(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
  var attempt=worldlogic$attempts.pollLast();
  if(attempt!=null&&Boolean.TRUE.equals(cir.getReturnValue())&&!player.serverLevel().getBlockState(pos).equals(attempt.state()))
   Survival.completedMining(player,attempt.hardness());
 }
}
