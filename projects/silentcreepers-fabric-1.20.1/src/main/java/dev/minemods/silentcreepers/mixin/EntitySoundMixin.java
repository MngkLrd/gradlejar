package dev.minemods.silentcreepers.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mute only the fuse warning; hurt, death, footsteps and explosion remain. */
@Mixin(Entity.class)
public abstract class EntitySoundMixin {
    @Inject(method = "playSound(Lnet/minecraft/sounds/SoundEvent;FF)V",
            at = @At("HEAD"), cancellable = true)
    private void silentcreepers$muteFuse(SoundEvent event, float volume, float pitch,
                                         CallbackInfo ci) {
        if ((Object) this instanceof Creeper && event == SoundEvents.CREEPER_PRIMED) {
            ci.cancel();
        }
    }
}
