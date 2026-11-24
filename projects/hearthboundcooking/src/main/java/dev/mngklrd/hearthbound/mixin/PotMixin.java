package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.blockentities.PotBlockEntity;
import net.dries007.tfc.common.recipes.PotRecipe;
import net.dries007.tfc.common.recipes.SoupPotRecipe;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PotBlockEntity.class,remap=false)
public abstract class PotMixin {
    @Unique private int hearthbound$hot,hearthbound$excess;
    @Unique private boolean hearthbound$empty;
    @Inject(method="handleCooking",at=@At("HEAD"))
    private void hearthbound$before(CallbackInfo ci){
        PotBlockEntity p=(PotBlockEntity)(Object)this;
        hearthbound$empty=p.getOutput()==null;
        if(p.isBoiling()){hearthbound$hot++;if(p.getTemperature()>470)hearthbound$excess++;}
        else if(hearthbound$empty){hearthbound$hot=0;hearthbound$excess=0;}
    }
    @Inject(method="handleCooking",at=@At("RETURN"))
    private void hearthbound$after(CallbackInfo ci){
        PotBlockEntity p=(PotBlockEntity)(Object)this;
        if(hearthbound$empty&&p.getOutput() instanceof SoupPotRecipe.SoupOutput out){
            Quality.set(out.stack(),90-Math.round(40f*hearthbound$excess/Math.max(1,hearthbound$hot)));
            hearthbound$hot=0;hearthbound$excess=0;p.setChanged();
        }
    }
}
