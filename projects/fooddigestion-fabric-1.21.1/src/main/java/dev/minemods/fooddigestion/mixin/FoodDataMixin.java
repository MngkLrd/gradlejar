package dev.minemods.fooddigestion.mixin;

import dev.minemods.fooddigestion.Digestion;
import net.minecraft.world.food.FoodData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @Shadow private int foodLevel;
    @Shadow private float saturationLevel;
    @Unique private final Digestion fooddigestion$queue = new Digestion();

    @Inject(method="add(IF)V", at=@At("HEAD"), cancellable=true)
    private void fooddigestion$split(int nutrition, float saturation, CallbackInfo ci) {
        if (nutrition < 0 || saturation < 0 || !Float.isFinite(saturation)) return;
        float points = saturation;
        Digestion.Intake intake = fooddigestion$queue.eat(foodLevel, saturationLevel, nutrition, points);
        foodLevel = Math.min(20, foodLevel + intake.food());
        saturationLevel = Math.min(foodLevel, saturationLevel + intake.saturation());
        ci.cancel();
    }

    @Inject(method="tick", at=@At("TAIL"))
    private void fooddigestion$tick(Player player, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer)) return;
        Digestion.Intake intake = fooddigestion$queue.tick(foodLevel, saturationLevel);
        foodLevel = Math.min(20, foodLevel + intake.food());
        saturationLevel = Math.min(foodLevel, saturationLevel + intake.saturation());
    }

    @Inject(method="addAdditionalSaveData", at=@At("TAIL"))
    private void fooddigestion$save(CompoundTag tag, CallbackInfo ci) {
        for (int i = 0; i < 3; i++) {
            tag.putInt("FoodDigestionH" + i, fooddigestion$queue.hunger(i));
            tag.putFloat("FoodDigestionS" + i, fooddigestion$queue.saturation(i));
            tag.putInt("FoodDigestionT" + i, fooddigestion$queue.timer(i));
        }
    }

    @Inject(method="readAdditionalSaveData", at=@At("TAIL"))
    private void fooddigestion$load(CompoundTag tag, CallbackInfo ci) {
        for (int i = 0; i < 3; i++) {
            fooddigestion$queue.restore(i, tag.getInt("FoodDigestionH" + i), tag.getFloat("FoodDigestionS" + i), tag.getInt("FoodDigestionT" + i));
        }
    }
}
