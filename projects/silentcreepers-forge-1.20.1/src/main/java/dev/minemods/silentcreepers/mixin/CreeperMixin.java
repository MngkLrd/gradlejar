package dev.minemods.silentcreepers.mixin;

import dev.minemods.silentcreepers.ai.SilentCreeperGoal;
import java.util.ArrayList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replace rather than stack the vanilla fuse goal. */
@Mixin(Creeper.class)
public abstract class CreeperMixin extends Monster {
    protected CreeperMixin(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void silentcreepers$installGoal(CallbackInfo ci) {
        for (WrappedGoal wrapped : new ArrayList<>(goalSelector.getAvailableGoals())) {
            if (wrapped.getGoal() instanceof SwellGoal) {
                goalSelector.removeGoal(wrapped.getGoal());
            }
        }
        goalSelector.addGoal(2, new SilentCreeperGoal((Creeper) (Object) this));
    }
}
