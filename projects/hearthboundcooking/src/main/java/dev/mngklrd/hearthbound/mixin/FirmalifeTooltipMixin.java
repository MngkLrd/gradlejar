package dev.mngklrd.hearthbound.mixin;
import net.dries007.tfc.compat.jade.common.BlockEntityTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.eerussianguy.firmalife.compat.tooltip.FLTooltips$BlockEntities",remap=false)
public abstract class FirmalifeTooltipMixin {
    @Shadow @Final @Mutable public static BlockEntityTooltip OVEN;
    @Inject(method="<clinit>",at=@At("RETURN"))
    private static void hearthbound$hideClock(CallbackInfo ci){
        BlockEntityTooltip original=OVEN;
        OVEN=(level,state,pos,entity,tooltip)->original.display(level,state,pos,entity,line->{
            if(line.getContents() instanceof TranslatableContents tr&&
                (tr.getKey().equals("firmalife.jade.cook_left")||tr.getKey().equals("firmalife.jade.cook_left_temp"))){
                Object[] args=tr.getArgs();
                Component status=Component.translatable("hearthbound.cooking_status",args[0]);
                for(Component sibling:line.getSiblings())status=status.copy().append(sibling);
                tooltip.accept(status);
            }else tooltip.accept(line);
        });
    }
}
