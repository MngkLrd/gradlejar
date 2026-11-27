package dev.minemods.traumacraft.mixin;

import dev.minemods.traumacraft.client.ClientClimbState;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ClientClimbTravelMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void traumatic$climbTravel(Vec3 input,CallbackInfo ci){
        if((Object)this instanceof LocalPlayer p&&ClientClimbState.pose(p,0)!=null){
            p.setDeltaMovement(Vec3.ZERO);
            ci.cancel();
        }
    }
}
