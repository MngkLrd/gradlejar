package dev.minemods.traumacraft.mixin;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="dev.minemods.worldlogic.Trauma",remap=false)
public abstract class TraumaMixin {
    @Inject(method="arm",at=@At("HEAD"),cancellable=true)
    private static void traumatic$arm(Player p,CallbackInfoReturnable<Boolean> ci) {
        if(p instanceof ServerPlayer) ci.setReturnValue(WorldLogicBridge.armInjury(p));
    }
    @Inject(method="impact",at=@At("RETURN"))
    private static void traumatic$collision(Player p,double severity,String reason,CallbackInfo ci) {
        WorldLogicBridge.collision(p,severity,reason);
    }
}
