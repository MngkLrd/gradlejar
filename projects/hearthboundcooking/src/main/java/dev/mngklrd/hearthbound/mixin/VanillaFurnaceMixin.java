package dev.mngklrd.hearthbound.mixin;

import dev.mngklrd.hearthbound.Quality;
import dev.mngklrd.hearthbound.QualityMath;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class VanillaFurnaceMixin {
    @Unique private int hearthbound$count,hearthbound$oldGrade,hearthbound$inherited;
    @Unique private ItemStack hearthbound$input=ItemStack.EMPTY;
    @Unique private boolean hearthbound$eligible(){
        Object be=this;return be instanceof FurnaceBlockEntity||be instanceof SmokerBlockEntity;
    }
    @Redirect(method="canBurn",at=@At(value="INVOKE",target="Lnet/minecraft/world/item/ItemStack;isSameItemSameTags(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean hearthbound$compare(ItemStack existing,ItemStack result){
        return hearthbound$eligible()?Quality.furnaceCompatible(existing,result):ItemStack.isSameItemSameTags(existing,result);
    }
    @Inject(method="burn",at=@At("HEAD"))
    private void hearthbound$before(RegistryAccess access,Recipe<?> recipe,NonNullList<ItemStack> items,int max,CallbackInfoReturnable<Boolean> ci){
        if(!hearthbound$eligible())return;
        ItemStack out=items.get(2);
        hearthbound$count=out.getCount();
        hearthbound$oldGrade=Quality.has(out)?Quality.get(out):-1;
        hearthbound$input=items.get(0).copyWithCount(1);
        hearthbound$inherited=Quality.inherited(List.of(hearthbound$input));
    }
    @Inject(method="burn",at=@At("RETURN"))
    private void hearthbound$after(RegistryAccess access,Recipe<?> recipe,NonNullList<ItemStack> items,int max,CallbackInfoReturnable<Boolean> ci){
        if(!hearthbound$eligible())return;
        ItemStack input=hearthbound$input;hearthbound$input=ItemStack.EMPTY;
        if(!ci.getReturnValue())return;
        ItemStack out=items.get(2);
        int added=out.getCount()-hearthbound$count;
        if(added<=0||!Quality.transformed(input,out)||!Quality.fresh(input))return;
        int q=QualityMath.finish(100,hearthbound$inherited);
        Quality.set(out,QualityMath.merged(hearthbound$oldGrade,hearthbound$count,q,added));
        ((AbstractFurnaceBlockEntity)(Object)this).setChanged();
    }
}
