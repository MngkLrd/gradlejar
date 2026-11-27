package dev.minemods.traumacraft.item;
import dev.minemods.traumacraft.content.HerbMask;
import dev.minemods.traumacraft.content.HerbWorkpieceEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.List;
public final class HerbBundleItem extends Item {
    public HerbBundleItem(Properties p){super(p);}
    @Override public void appendHoverText(ItemStack s,TooltipContext c,List<Component> lines,TooltipFlag flag){
        super.appendHoverText(s,c,lines,flag);
        lines.add(Component.translatable("message.traumacraft.herbs_progress",HerbMask.progress(HerbWorkpieceEntity.fromItem(s))));
        lines.add(Component.translatable("tooltip.traumacraft.herbs_help"));
    }
}
