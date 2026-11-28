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
    @Inject(method="mining",at=@At("HEAD"),cancellable=true)
    private static void traumatic$mining(Player p,CallbackInfoReturnable<Double> ci){
        if(p instanceof ServerPlayer && WorldLogicBridge.functionsEnabled(p))
            ci.setReturnValue((double)WorldLogicBridge.miningFactor(p));
    }
    @Inject(method="melee",at=@At("HEAD"),cancellable=true)
    private static void traumatic$melee(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e,CallbackInfo ci){
        if(!e.isCanceled()&&e.getSource().getDirectEntity() instanceof ServerPlayer p&&WorldLogicBridge.functionsEnabled(p)){
            e.setAmount(e.getAmount()*WorldLogicBridge.strikeFactor(p));
            ci.cancel();
        }
    }
    @Inject(method="impact",at=@At("RETURN"))
    private static void traumatic$collision(Player p,double severity,String reason,CallbackInfo ci) {
        WorldLogicBridge.collision(p,severity,reason);
    }
}
