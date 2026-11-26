package dev.minemods.naturalbuoyancy.mixin;
import dev.minemods.naturalbuoyancy.BuoyancyPhysics;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Inject(method="tick",at=@At("TAIL"))
    private void naturalbuoyancy$afterTick(CallbackInfo ci) {
        BuoyancyPhysics.tick((ItemEntity)(Object)this);
    }
}
