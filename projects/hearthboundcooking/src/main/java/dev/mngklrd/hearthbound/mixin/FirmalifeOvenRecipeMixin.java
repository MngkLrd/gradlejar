package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo
@Mixin(targets="com.eerussianguy.firmalife.common.recipes.OvenRecipe",remap=false)
public abstract class FirmalifeOvenRecipeMixin {
    @Shadow public abstract float getTemperature();
    @Inject(method="assemble(Lnet/dries007/tfc/common/recipes/inventory/ItemStackInventory;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;",at=@At("RETURN"))
    private void hearthbound$bake(ItemStackInventory in,RegistryAccess access,CallbackInfoReturnable<ItemStack> cir){Quality.completedHeating(in.getStack(),cir.getReturnValue(),getTemperature());}
}
