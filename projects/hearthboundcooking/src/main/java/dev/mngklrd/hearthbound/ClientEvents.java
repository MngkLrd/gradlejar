package dev.mngklrd.hearthbound;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.inventory.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=Hearthbound.ID,value=Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        ItemStack s=event.getItemStack();
        if(!Quality.has(s))return;
        int q=Quality.get(s);
        ChatFormatting color=q>=90?ChatFormatting.GOLD:q>=75?ChatFormatting.GREEN:q>=50?ChatFormatting.YELLOW:ChatFormatting.RED;
        String tier=q>=90?"excellent":q>=75?"good":q>=50?"ordinary":"poor";
        event.getToolTip().add(Component.translatable("hearthbound.quality",q,Component.translatable("hearthbound.tier."+tier)).withStyle(color));
    }
    @SubscribeEvent public static void noCookingClock(ScreenEvent.Render.Post event){
        if(!(event.getScreen() instanceof AbstractFurnaceScreen<?> screen))return;
        ItemStack input=screen.getMenu().getSlot(0).getItem();
        ItemStack output=screen.getMenu().getSlot(2).getItem();
        if(!Quality.food(input)&&!Quality.food(output))return;
        String path=screen instanceof FurnaceScreen?"furnace":screen instanceof SmokerScreen?"smoker":null;
        if(path==null)return;
        event.getGuiGraphics().blit(new ResourceLocation("minecraft","textures/gui/container/"+path+".png"),screen.getGuiLeft()+79,screen.getGuiTop()+34,79,34,24,17);
    }
}
