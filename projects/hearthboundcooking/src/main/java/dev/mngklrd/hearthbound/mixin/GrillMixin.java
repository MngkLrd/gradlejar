package dev.mngklrd.hearthbound.mixin;
import dev.mngklrd.hearthbound.Quality;
import net.dries007.tfc.common.blockentities.GrillBlockEntity;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=GrillBlockEntity.class,remap=false)
public abstract class GrillMixin {
    @Unique private final int[] hearthbound$exposure=new int[5];
    @Unique private final ItemStack[] hearthbound$previous=new ItemStack[5];
    @Inject(method="handleCooking",at=@At("RETURN"))
    private void hearthbound$hold(CallbackInfo ci){
        GrillBlockEntity g=(GrillBlockEntity)(Object)this;
        if(g.getLevel()==null||g.getLevel().isClientSide)return;
        g.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(h->{
            for(int i=0;i<5&&i+4<h.getSlots();i++){
                ItemStack s=h.getStackInSlot(i+4);
                if(s!=hearthbound$previous[i]){hearthbound$previous[i]=s;hearthbound$exposure[i]=0;}
                if(!Quality.has(s)||!Quality.fresh(s)||g.getTemperature()<=300||HeatCapability.getTemperature(s)<=220){hearthbound$exposure[i]=0;continue;}
                if(++hearthbound$exposure[i]>=100){
                    Quality.set(s,Quality.get(s)-5);hearthbound$exposure[i]=0;g.markForSync();
                }
            }
        });
    }
}
