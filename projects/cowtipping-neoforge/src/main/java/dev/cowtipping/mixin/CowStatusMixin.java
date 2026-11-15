package dev.cowtipping.mixin;
import dev.cowtipping.gameplay.CowTipState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class CowStatusMixin {
    @Inject(method="handleEntityEvent",at=@At("HEAD"),cancellable=true)
    private void cowtipping$onStatus(byte id,CallbackInfo callback){
        if(CowTipState.isTipEvent(id)&&(Object)this instanceof Cow cow){
            CowTipState.onEntityEvent(cow,id);callback.cancel();
        }
    }
}
