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
    @Mod.EventBusSubscriber(modid=Hearthbound.ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void colors(net.minecraftforge.client.event.RegisterColorHandlersEvent.Block event){
            event.register((state,world,pos,index)->{
                if(index!=0)return -1;
                return switch(state.getValue(VesselBlock.STAGE)){case 1->0xb8bfc9;case 2->0xe1cfb2;case 3->0xffffff;case 4->0x725046;default->0xffffff;};
            },net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Hearthbound.ID,"clay_stockpot")),
              net.minecraftforge.registries.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Hearthbound.ID,"bronze_cauldron")));
        }
        @SubscribeEvent public static void setup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event){
            event.enqueueWork(()->net.minecraft.client.gui.screens.MenuScreens.register(Hearthbound.VESSEL_MENU.get(),VesselScreen::new));
        }
    }
    @SubscribeEvent public static void tooltip(ItemTooltipEvent event){
        ItemStack s=event.getItemStack();
        if(!Quality.has(s))return;
        int q=Quality.get(s);
        ChatFormatting color=q>=90?ChatFormatting.GOLD:q>=75?ChatFormatting.GREEN:q>=50?ChatFormatting.YELLOW:ChatFormatting.RED;
        String tier=CookingReport.tier(q);
        event.getToolTip().add(Component.literal(CookingReport.starsText(q)+"  ").withStyle(ChatFormatting.GOLD).append(Component.translatable("hearthbound.quality",q,Component.translatable("hearthbound.tier."+tier)).withStyle(color)));
        if(!net.minecraft.client.gui.screens.Screen.hasShiftDown()){
            event.getToolTip().add(Component.translatable("hearthbound.report.shift").withStyle(ChatFormatting.DARK_GRAY));return;
        }
        CookingReport report=CookingReport.read(s);
        if(report==null){event.getToolTip().add(Component.translatable("hearthbound.report.legacy").withStyle(ChatFormatting.GRAY));return;}
        String[] keys={"under","over","heat","late","prior"};
        int[] values={report.under(),report.over(),report.heat(),report.late(),report.prior()};
        for(int i=0;i<keys.length;i++){
            if(values[i]<=0)continue;
            event.getToolTip().add(Component.translatable("hearthbound.report."+keys[i],values[i]).withStyle(ChatFormatting.RED));
            if(i<2){
                Component names=CookingReport.names(s,i==0?"UnderFoods":"OverFoods");
                if(!names.getString().isEmpty())event.getToolTip().add(Component.literal("  ").append(names).withStyle(ChatFormatting.GRAY));
            }
        }
        int[] scores={100-report.under()-report.over(),100-report.heat(),100-report.late()};
        String[] factors={"ingredients","temperature","serving"};
        for(int i=0;i<factors.length;i++){
            int n=Math.max(0,Math.min(10,(scores[i]+5)/10));
            event.getToolTip().add(Component.translatable("hearthbound.factor."+factors[i]).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" "+"|".repeat(n)).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("|".repeat(10-n)).withStyle(ChatFormatting.DARK_GRAY))
                .append(Component.literal(" "+scores[i]+"%").withStyle(ChatFormatting.WHITE)));
        }
        if(report.score()==100)event.getToolTip().add(Component.translatable("hearthbound.report.perfect").withStyle(ChatFormatting.GREEN));
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
