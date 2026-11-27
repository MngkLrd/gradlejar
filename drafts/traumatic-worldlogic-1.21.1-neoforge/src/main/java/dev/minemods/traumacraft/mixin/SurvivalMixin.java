package dev.minemods.traumacraft.mixin;
import dev.minemods.traumacraft.integration.WorldLogicBridge;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="dev.minemods.worldlogic.Survival",remap=false)
public abstract class SurvivalMixin {
    // Replace just the duplicate injury timer, not the NeoForge event or fall physics.
    @Inject(method="fall",at=@At("HEAD"),cancellable=true)
    private static void traumatic$fall(LivingFallEvent event,CallbackInfo ci) {
        if(event.getEntity() instanceof ServerPlayer) ci.cancel();
    }
    @Inject(method="injured",at=@At("HEAD"),cancellable=true)
    private static void traumatic$injured(Player p,long now,CallbackInfoReturnable<Boolean> ci) {
        if(p instanceof ServerPlayer) ci.setReturnValue(WorldLogicBridge.legInjury(p));
    }
    @Inject(method="penalty",at=@At("RETURN"),cancellable=true)
    private static void traumatic$penalty(Player p,long now,CallbackInfoReturnable<Double> ci) {
        if(p instanceof ServerPlayer) ci.setReturnValue(WorldLogicBridge.mergePenalty(p,ci.getReturnValue()));
    }
    @Inject(method="aggravate",at=@At("HEAD"),cancellable=true)
    private static void traumatic$aggravate(Player p,int ticks,CallbackInfo ci) {
        if(p instanceof ServerPlayer) { WorldLogicBridge.aggravate(p,ticks); ci.cancel(); }
    }
}
