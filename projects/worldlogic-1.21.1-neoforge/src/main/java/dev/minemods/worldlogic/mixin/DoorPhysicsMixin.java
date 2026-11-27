package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.ObjectsPhysics;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=DoorBlock.class,remap=false)
public abstract class DoorPhysicsMixin {
 @Inject(method="neighborChanged",at=@At("RETURN"),remap=false)
 private void worldlogic$redstone(BlockState old,Level level,BlockPos pos,Block source,BlockPos sourcePos,boolean moving,CallbackInfo ci){worldlogic$closed(old,level,pos);}
 @Inject(method="setOpen",at=@At("RETURN"),remap=false)
 private void worldlogic$mob(Entity entity,Level level,BlockState old,BlockPos pos,boolean open,CallbackInfo ci){worldlogic$closed(old,level,pos);}
 @Inject(method="useWithoutItem",at=@At("RETURN"),remap=false)
 private void worldlogic$manual(BlockState old,Level level,BlockPos pos,Player player,BlockHitResult hit,CallbackInfoReturnable<InteractionResult> ci){worldlogic$closed(old,level,pos);}
 @org.spongepowered.asm.mixin.Unique
 private void worldlogic$closed(BlockState old,Level level,BlockPos pos){if(level instanceof ServerLevel l&&old.getValue(DoorBlock.OPEN)&&l.getBlockState(pos).getBlock() instanceof DoorBlock&&!l.getBlockState(pos).getValue(DoorBlock.OPEN))ObjectsPhysics.queueDoor(l,pos);}
}
