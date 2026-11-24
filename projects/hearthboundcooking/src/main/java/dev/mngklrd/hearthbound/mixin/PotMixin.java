package dev.mngklrd.hearthbound.mixin;

import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.SoupPotRecipe;
import net.dries007.tfc.common.recipes.JamPotRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=PotBlockEntity.class,remap=false)
public abstract class PotMixin {
    @Unique private int hearthbound$hot,hearthbound$excess,hearthbound$score;
    @Inject(method="handleCooking",at=@At("HEAD"))
    private void hearthbound$before(CallbackInfo ci){
        PotBlockEntity p=(PotBlockEntity)(Object)this;
        if(p.isBoiling()){hearthbound$hot++;if(p.getTemperature()>470)hearthbound$excess++;}
        else {hearthbound$hot=0;hearthbound$excess=0;}
    }
    @Inject(method="saveAdditional",at=@At("RETURN"))
    private void hearthbound$save(CompoundTag tag,CallbackInfo ci){
        tag.putInt("hearthbound_hot",hearthbound$hot);
        tag.putInt("hearthbound_excess",hearthbound$excess);
    }
    @Inject(method="loadAdditional",at=@At("RETURN"))
    private void hearthbound$load(CompoundTag tag,CallbackInfo ci){
        hearthbound$hot=Math.max(0,tag.getInt("hearthbound_hot"));
        hearthbound$excess=Math.max(0,Math.min(hearthbound$hot,tag.getInt("hearthbound_excess")));
    }
    // This call is executed only when native cooking completes. Never touch inputs.
    @Redirect(method="handleCooking",at=@At(value="INVOKE",target="Lnet/dries007/tfc/common/recipes/PotRecipe;getOutput(Lnet/dries007/tfc/common/blockentities/PotBlockEntity$PotInventory;)Lnet/dries007/tfc/common/recipes/PotRecipe$Output;"))
    private PotRecipe.Output hearthbound$finish(PotRecipe recipe,PotBlockEntity.PotInventory inventory){
        List<ItemStack> inputs=new ArrayList<>(5);
        for(int i=4;i<=8;i++)inputs.add(inventory.getStackInSlot(i));
        int process=95-Math.round(40f*hearthbound$excess/Math.max(1,hearthbound$hot));
        hearthbound$score=Quality.score(process,inputs);
        PotRecipe.Output out=recipe.getOutput(inventory);
        if(out instanceof SoupPotRecipe.SoupOutput soup)Quality.set(soup.stack(),hearthbound$score);
        if(out instanceof JamPotRecipe.JamOutput jam)Quality.set(jam.stack(),hearthbound$score);
        hearthbound$hot=0;hearthbound$excess=0;
        return out;
    }
    @Redirect(method="handleCooking",at=@At(value="INVOKE",target="Lnet/dries007/tfc/common/recipes/PotRecipe$Output;onFinish(Lnet/dries007/tfc/common/blockentities/PotBlockEntity$PotInventory;)V"))
    private void hearthbound$finishItems(PotRecipe.Output out,PotBlockEntity.PotInventory inventory){
        // Native remaining containers are already present. Grade only new food outputs.
        ItemStack[] before=new ItemStack[5];
        for(int i=0;i<5;i++)before[i]=inventory.getStackInSlot(i+4).copy();
        out.onFinish(inventory);
        for(int i=0;i<5;i++){
            ItemStack s=inventory.getStackInSlot(i+4);
            if(!s.isEmpty()&&!ItemStack.matches(before[i],s))Quality.set(s,hearthbound$score);
        }
    }
}
