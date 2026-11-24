package dev.minemods.keepmycargo.mixin;
import dev.minemods.keepmycargo.CargoTransfer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;
import java.util.ArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemStack.class)
public abstract class ItemTooltipMixin {
    @Inject(method="getTooltipLines", at=@At("RETURN"), cancellable=true)
    private void keepmycargo$tooltip(Item.TooltipContext context, Player player, TooltipFlag flag, CallbackInfoReturnable<List<Component>> cir) {
        ItemStack stack=(ItemStack)(Object)this;
        if (!CargoTransfer.isPacked(stack)) return;
        List<Component> lines=new ArrayList<>(cir.getReturnValue());
        lines.add(Component.translatable("tooltip.keepmycargo.loaded", CargoTransfer.packedCount(stack)).withStyle(ChatFormatting.GOLD));
        cir.setReturnValue(lines);
    }
}
