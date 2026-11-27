package dev.minemods.thermal.client;
import dev.minemods.thermal.ThermalMod;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.network.ThermalSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.components.Button;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.Locale;
@EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ThermalHud {
    static final int[] COLORS={0xff5483c4,0xff75b1e2,0xffa4cfe5,0xff83c17b,0xffe6ce78,0xffe99151,0xffd64b40};
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ThermalMod.ID,"temperature"),ThermalHud::render);
    }
    private static void render(GuiGraphics g,DeltaTracker delta) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen!=null||mc.player.isCreative()||mc.player.isSpectator()) return;
        var s=ThermalSync.current(mc.player.getId()); if(s==null||!s.active())return;
        int x=g.guiWidth()/2+97,y=g.guiHeight()-39;
        if(x+72>g.guiWidth()) {x=5;y=g.guiHeight()-40;}
        var stage=ThermalModel.stage(s.body()); int color=COLORS[stage.ordinal()];
        g.fill(x,y,x+72,y+31,0xb0191b1c); g.fill(x,y,x+72,y+1,0xff535558);
        int px=x+6, py=y+4; int dark=0xff12171b;
        g.fill(px+3,py,px+9,py+7,dark); g.fill(px+4,py+1,px+8,py+6,color);
        g.fill(px+1,py+7,px+11,py+15,dark); g.fill(px+2,py+8,px+10,py+14,color);
        g.fill(px+3,py+15,px+9,py+21,dark); g.fill(px+4,py+15,px+6,py+20,color);g.fill(px+7,py+15,px+9,py+20,color);
        g.drawString(mc.font,String.format(Locale.ROOT,"%.1f°",s.body()),x+20,y+4,color,true);
        String trend=s.trend()>.12?"+":s.trend()<-.12?"-":"=";g.drawString(mc.font,trend,x+62,y+4,color,true);
        g.drawString(mc.font,String.format(Locale.ROOT,"%+.0f°",s.air()),x+20,y+14,0xffd5e0e4,true);
        for(int i=0;i<7;i++)g.fill(x+20+i*7,y+25,x+26+i*7,y+28,COLORS[i]);
        int mark=x+22+stage.ordinal()*7;g.fill(mark,y+23,mark+2,y+25,0xffeeeeee);
    }
    @EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.GAME,value=Dist.CLIENT)
    public static final class Connection {
        @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event) {
            if (!(event.getScreen() instanceof InventoryScreen screen)) return;
            int x=screen.getGuiLeft()+screen.getXSize()-76, y=Math.max(2,screen.getGuiTop()-20);
            event.addListener(Button.builder(Component.translatable("screen.worldlogic_thermal.title"), b->Minecraft.getInstance().setScreen(new ThermalScreen(screen))).bounds(x,y,76,18).build());
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){ThermalSync.clear();}
        @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event){ThermalSync.clear();}
    }
}
