package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.CombatConsequences;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(value=FoodData.class,remap=false)
public abstract class FoodRecoveryMixin {
 @Redirect(method="tick",at=@At(value="INVOKE",target="Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"),remap=false,require=1)
 private boolean worldlogic$naturalHealing(GameRules rules,GameRules.Key<GameRules.BooleanValue> key,Player player){
  return rules.getBoolean(key)&&(key!=GameRules.RULE_NATURAL_REGENERATION||CombatConsequences.canNaturallyHeal(player));
 }
}
