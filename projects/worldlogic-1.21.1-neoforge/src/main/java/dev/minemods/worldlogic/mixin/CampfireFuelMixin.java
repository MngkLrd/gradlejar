package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.CampfireFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=CampfireBlockEntity.class,remap=false)
public abstract class CampfireFuelMixin {
 @Inject(method="cookTick",at=@At("HEAD"),cancellable=true,remap=false)
 private static void worldlogic$burn(Level level,BlockPos pos,BlockState state,CampfireBlockEntity campfire,CallbackInfo ci){if(level instanceof ServerLevel l&&!CampfireFuel.tick(l,pos,state))ci.cancel();}
}
