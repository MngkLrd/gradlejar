package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.container.SaladContainer;
import net.dries007.tfc.common.capabilities.food.*;
import net.dries007.tfc.util.calendar.Calendars;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=SaladContainer.class,remap=false)
public abstract class SaladMixin {
    @Shadow @Final private ItemStackHandler inventory;
    @Inject(method="setAndUpdateSlots",at=@At("RETURN"))
    private void hearthbound$assemble(int slot,CallbackInfo ci){
        ItemStack result=inventory.getStackInSlot(6);
        if(result.isEmpty()||Quality.has(result))return;
        int sum=0,n=0;
        for(int i=0;i<5;i++){
            ItemStack s=inventory.getStackInSlot(i);IFood f=FoodCapability.get(s);
            if(s.isEmpty()||f==null||f.isRotten())continue;
            long a=f.getCreationDate(),b=f.getRottenDate();double freshness=1;
            if(a>=0&&b>a&&b<Long.MAX_VALUE)freshness=Math.max(0,Math.min(1,((double)b-Calendars.get().getTicks())/((double)b-a)));
            sum+=(int)Math.round((60+40*freshness)*.7+(Quality.has(s)?Quality.get(s):85)*.3);n++;
        }
        Quality.set(result,n==0?70:sum/n);
    }
}
