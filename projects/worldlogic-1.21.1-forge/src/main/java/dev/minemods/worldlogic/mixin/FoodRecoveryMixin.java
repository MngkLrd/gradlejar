package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.CombatConsequences;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=FoodData.class,remap=false)
public abstract class FoodRecoveryMixin {
 @Inject(method="tick",at=@At("HEAD"),remap=false)
 private void worldlogic$begin(Player p,CallbackInfo ci){CombatConsequences.enterFoodTick(p);}
 @Inject(method="tick",at=@At("RETURN"),remap=false)
 private void worldlogic$end(Player p,CallbackInfo ci){CombatConsequences.exitFoodTick();}
}
