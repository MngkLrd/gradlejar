package dev.minemods.keepmycargo.mixin;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method="addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", at=@At("HEAD"))
    private void keepmycargo$restore(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        CargoTransfer.restoreOnSpawn(entity);
    }
}
