package dev.minemods.worldlogic.mixin;
import java.util.ArrayDeque;
import dev.minemods.worldlogic.Survival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=ServerPlayerGameMode.class,remap=false)
public abstract class CompletedMiningMixin {
 @Shadow @Final protected ServerPlayer player;
 @Unique private final ArrayDeque<BlockState> worldlogic$original=new ArrayDeque<>();
 @Unique private final ArrayDeque<Float> worldlogic$hardness=new ArrayDeque<>();
 @Inject(method="destroyBlock",at=@At("HEAD"),remap=false)
 private void worldlogic$start(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
  var l=player.serverLevel();
  boolean loaded=!l.isOutsideBuildHeight(pos)&&l.hasChunkAt(pos);
  var state=loaded?l.getBlockState(pos):Blocks.AIR.defaultBlockState();
  worldlogic$original.addLast(state);worldlogic$hardness.addLast(loaded?state.getDestroySpeed(l,pos):0f);
 }
 @Inject(method="destroyBlock",at=@At("RETURN"),remap=false)
 private void worldlogic$finish(BlockPos pos,CallbackInfoReturnable<Boolean> cir){
  var original=worldlogic$original.pollLast();var hardness=worldlogic$hardness.pollLast();
  if(original!=null&&hardness!=null&&hardness>0&&Boolean.TRUE.equals(cir.getReturnValue())&&player.serverLevel().hasChunkAt(pos)
   &&!player.serverLevel().getBlockState(pos).equals(original))Survival.completedMining(player,hardness);
 }
}
