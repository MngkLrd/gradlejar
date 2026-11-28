package dev.minemods.treechopper.mixin;
import dev.minemods.treechopper.ChopSwing;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class LivingSwingMixin implements ChopSwing {
    @Unique private int wl$slowUntil=Integer.MIN_VALUE;
    @Shadow private int getCurrentSwingDuration(){throw new AssertionError();}
    @Override public void wl$markSwing(){wl$slowUntil=((LivingEntity)(Object)this).tickCount+13;}
    @Override public int wl$swingDuration(){return getCurrentSwingDuration();}
    @Inject(method="getCurrentSwingDuration",at=@At("HEAD"),cancellable=true)
    private void wl$slowChop(CallbackInfoReturnable<Integer> cir){
        if(((LivingEntity)(Object)this).tickCount<wl$slowUntil)cir.setReturnValue(12);
    }
}
