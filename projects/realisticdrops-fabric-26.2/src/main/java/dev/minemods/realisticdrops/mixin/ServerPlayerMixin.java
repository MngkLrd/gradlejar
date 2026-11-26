package dev.minemods.realisticdrops.mixin;
import dev.minemods.realisticdrops.DropPhysics;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method="tick", at=@At("HEAD"))
    private void realisticdrops$sample(CallbackInfo ci) {
        DropPhysics.sample((ServerPlayer)(Object)this);
    }
    @Inject(method="drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;", at=@At("RETURN"))
    private void realisticdrops$throw(ItemStack stack, boolean random, boolean trace, CallbackInfoReturnable<ItemEntity> cir) {
        if (!random && trace) DropPhysics.apply((ServerPlayer)(Object)this, cir.getReturnValue());
    }
}
