package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.capabilities.food.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TFCFoodData.class,remap=false)
public abstract class FoodDataMixin {
    @Unique private float hearthbound$factor=1f;
    @Inject(method="eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",at=@At("HEAD"))
    private void hearthbound$start(Item item,ItemStack s,LivingEntity entity,CallbackInfo ci){
        hearthbound$factor=1f;
        if(entity instanceof ServerPlayer&&Quality.has(s)&&Quality.fresh(s))
            hearthbound$factor=Quality.saturationMultiplier(Quality.get(s));
    }
    @ModifyVariable(method="eat(Lnet/dries007/tfc/common/capabilities/food/FoodData;)V",at=@At("HEAD"),argsOnly=true)
    private FoodData hearthbound$mealData(FoodData d){
        float factor=hearthbound$factor;
        hearthbound$factor=1f;
        if(factor==1f||d.hunger()<=0)return d;
        return new FoodData(d.hunger(),d.water(),d.saturation()*factor,d.grain(),d.fruit(),d.vegetables(),d.protein(),d.dairy(),d.decayModifier());
    }
    @Inject(method="eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",at=@At("RETURN"))
    private void hearthbound$end(Item item,ItemStack s,LivingEntity entity,CallbackInfo ci){hearthbound$factor=1f;}
}
