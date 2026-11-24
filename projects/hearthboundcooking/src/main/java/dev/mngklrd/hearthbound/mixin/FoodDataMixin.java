package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.capabilities.food.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=TFCFoodData.class,remap=false)
public abstract class FoodDataMixin {
    @Inject(method="eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",at=@At("RETURN"))
    private void hearthbound$quality(Item item,ItemStack s,LivingEntity entity,CallbackInfo ci){
        if(!(entity instanceof ServerPlayer)||!Quality.has(s))return;
        IFood f=FoodCapability.get(s);if(f==null||f.isRotten())return;
        TFCFoodData data=(TFCFoodData)(Object)this;
        float delta=f.getData().saturation()*(Quality.saturationMultiplier(Quality.get(s))-1f);
        data.setSaturation(Math.max(0,Math.min(data.getFoodLevel(),data.getSaturationLevel()+delta)));
    }
}
