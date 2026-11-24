package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=HeatingRecipe.class,remap=false)
public abstract class HeatingRecipeMixin {
    @Shadow public abstract float getTemperature();
    @Inject(method="assemble(Lnet/dries007/tfc/common/recipes/inventory/ItemStackInventory;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN"))
    private void hearthbound$assemble(ItemStackInventory in,RegistryAccess access,CallbackInfoReturnable<ItemStack> cir){Quality.heating(in.getStack(),cir.getReturnValue(),getTemperature());}
    @Inject(method="assembleStacked",at=@At("RETURN"))
    private void hearthbound$stacked(ItemStackInventory in,int cap,float chance,CallbackInfoReturnable<ItemStack> cir){Quality.heating(in.getStack(),cir.getReturnValue(),getTemperature());}
}
