package dev.mngklrd.hearthbound.mixin;

import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.recipes.RecipeHelpers;
import net.dries007.tfc.common.capabilities.food.*;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="net.dries007.tfc.common.recipes.outputs.MealModifier",remap=false)
public abstract class MealModifierMixin {
    @Inject(method="apply",at=@At("RETURN"))
    private void hearthbound$meal(ItemStack stack,ItemStack input,CallbackInfoReturnable<ItemStack> ci){
        ItemStack out=ci.getReturnValue();
        if(!(FoodCapability.get(out) instanceof FoodHandler.Dynamic))return;
        List<ItemStack> ingredients=new ArrayList<>();
        for(ItemStack s:RecipeHelpers.getCraftingInput()){
            if(!Quality.food(s))continue;
            if(!Quality.fresh(s))return;
            ingredients.add(s.copyWithCount(1));
        }
        if(!ingredients.isEmpty())Quality.finish(out,90,ingredients);
    }
}
