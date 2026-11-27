package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.Rules;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=AbstractArrow.class,remap=false)
public abstract class ArrowLifeMixin {
 @Inject(method="tickDespawn",at=@At("HEAD"),cancellable=true,remap=false)
 private void worldlogic$extend(CallbackInfo ci){AbstractArrow a=(AbstractArrow)(Object)this;if(!a.level().isClientSide&&Rules.on("arrow_recovery")&&a.getPersistentData().getBoolean("worldlogic_recoverable")&&a.tickCount<2400)ci.cancel();}
}
