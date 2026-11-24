package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.blockentities.GrillBlockEntity;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.RegistryAccess;
import net.dries007.tfc.common.recipes.HeatingRecipe;
import net.dries007.tfc.common.recipes.inventory.ItemStackInventory;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GrillBlockEntity.class,remap=false)
public abstract class GrillMixin {
    // Native grill recipe completion applies WOOD_GRILLED to the result, even for a same-ID food.
    @Redirect(method="handleCooking",at=@At(value="INVOKE",target="Lnet/dries007/tfc/common/recipes/HeatingRecipe;assemble(Lnet/dries007/tfc/common/recipes/inventory/ItemStackInventory;Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack hearthbound$grilled(HeatingRecipe recipe,ItemStackInventory in,RegistryAccess access){
        ItemStack output=recipe.assemble(in,access);
        Quality.completedHeating(in.getStack(),output,recipe.getTemperature());
        return output;
    }
    @Inject(method="handleCooking",at=@At("RETURN"))
    private void hearthbound$hold(CallbackInfo ci){
        GrillBlockEntity g=(GrillBlockEntity)(Object)this;
        if(dev.mngklrd.hearthbound.HoldTracker.tick(g,4,5))g.markForSync();
    }
}
