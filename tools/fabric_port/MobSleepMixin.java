package dev.minemods.blowpipes.mixin;

import dev.minemods.blowpipes.SleepState;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobSleepMixin implements SleepState {
    @Unique private static final String BP_UNTIL = "blowpipes:sleep_until";
    @Unique private static final String BP_ORIGINAL = "blowpipes:old_no_ai";
    @Unique private long blowpipes$sleepUntil;
    @Unique private boolean blowpipes$previousNoAi;

    @Override
    public boolean blowpipes$startSleep(int durationTicks) {
        Mob mob = (Mob)(Object)this;
        if (mob.level().isClientSide()) return false;
        if (blowpipes$sleepUntil <= 0) {
            if (mob.isNoAi()) return false; // Preserve other mods' AI states.
            blowpipes$previousNoAi = mob.isNoAi();
        }
        blowpipes$sleepUntil = mob.level().getGameTime() + Math.max(1, durationTicks);
        mob.setTarget(null);
        mob.getNavigation().stop();
        mob.setNoAi(true);
        return true;
    }

    @Override
    public void blowpipes$wakeUp() {
        if (blowpipes$sleepUntil <= 0) return;
        Mob mob = (Mob)(Object)this;
        boolean original = blowpipes$previousNoAi;
        blowpipes$sleepUntil = 0;
        blowpipes$previousNoAi = false;
        if (!mob.level().isClientSide()) mob.setNoAi(original);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void blowpipes$tickSleep(CallbackInfo info) {
        Mob mob = (Mob)(Object)this;
        if (mob.level().isClientSide() || blowpipes$sleepUntil <= 0) return;
        if (!mob.isAlive() || mob.level().getGameTime() >= blowpipes$sleepUntil) {
            blowpipes$wakeUp();
            return;
        }
        if (!mob.isNoAi()) mob.setNoAi(true);
        if (mob.tickCount % 15 == 0 && mob.level() instanceof ServerLevel world) {
            world.sendParticles(ParticleTypes.WITCH, mob.getX(),
                    mob.getY() + mob.getBbHeight() + 0.25,
                    mob.getZ(), 2, 0.15, 0.05, 0.15, 0.0);
        }
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void blowpipes$saveSleep(CompoundTag tag, CallbackInfo info) {
        if (blowpipes$sleepUntil > 0) {
            tag.putLong(BP_UNTIL, blowpipes$sleepUntil);
            tag.putBoolean(BP_ORIGINAL, blowpipes$previousNoAi);
        } else {
            tag.remove(BP_UNTIL);
            tag.remove(BP_ORIGINAL);
        }
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void blowpipes$readSleep(CompoundTag tag, CallbackInfo info) {
        if (tag.contains(BP_UNTIL, Tag.TAG_LONG)) {
            blowpipes$sleepUntil = tag.getLong(BP_UNTIL);
            blowpipes$previousNoAi = tag.getBoolean(BP_ORIGINAL);
        } else {
            blowpipes$sleepUntil = 0;
            blowpipes$previousNoAi = false;
        }
    }
}
