package dev.minemods.metabolism.mixin;
import dev.minemods.metabolism.Metabolism;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Player.class)
public abstract class PlayerFoodMixin {
 @Inject(method="canEat",at=@At("RETURN"),cancellable=true)
 private void metabolism$allowFood(boolean alwaysEat,CallbackInfoReturnable<Boolean> cir){
  if(!cir.getReturnValue()&&Metabolism.canEatMore((Player)(Object)this))cir.setReturnValue(true);
 }
}
