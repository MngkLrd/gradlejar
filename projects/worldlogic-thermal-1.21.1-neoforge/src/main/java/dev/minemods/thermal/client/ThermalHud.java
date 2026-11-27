package dev.minemods.thermal.client;
import dev.minemods.thermal.ThermalMod;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.ThermalCondition;
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
import java.lang.ref.WeakReference;
@EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class ThermalHud {
    static final int[] COLORS={0xff5483c4,0xff75b1e2,0xffa4cfe5,0xff83c17b,0xffe6ce78,0xffe99151,0xffd64b40};
    @SubscribeEvent public static void layers(RegisterGuiLayersEvent event){
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ThermalMod.ID,"temperature"),ThermalHud::render);
    }
    static void scale(GuiGraphics g,int x,int y,int w,int stage){
        g.fill(x-1,y-1,x+w+1,y+5,0xff1a1b1c);
        for(int i=0;i<w;i++){
            int c=ThermalColors.gradient((double)i/Math.max(1,w-1));
            g.fill(x+i,y,x+i+1,y+4,c);
        }
        int m=x+(stage*w/7+(stage+1)*w/7)/2;
        g.fill(m-1,y-2,m+2,y,0xffeeeeee);g.fill(m,y,m+1,y+4,0xffeeeeee);
    }
    private static void render(GuiGraphics g,DeltaTracker delta){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!p.isAlive()||mc.options.hideGui||mc.screen!=null||p.isCreative()||p.isSpectator())return;
        var s=ThermalSync.current(p.getId());if(s==null||!s.active())return;
        var r=ThermalLayout.hud(g.guiWidth(),g.guiHeight());int x=r.x(),y=r.y(),w=r.w();
        var stage=ThermalModel.stage(s.body());int color=COLORS[stage.ordinal()];
        g.fill(x+1,y+1,x+w+1,y+r.h()+1,0x70000000);
        g.fill(x,y,x+w,y+r.h(),0xcf252729);
        g.fill(x+1,y+1,x+w-1,y+2,0xff67696a);g.fill(x+1,y+2,x+2,y+r.h()-1,0xff4c4e50);
        g.fill(x+2,y+r.h()-2,x+w-1,y+r.h()-1,0xff171819);g.fill(x+w-2,y+2,x+w-1,y+r.h()-1,0xff171819);
        g.drawString(mc.font,String.format(Locale.ROOT,"%.1f°",s.body()),x+6,y+4,color,true);
        String trend=ThermalColors.arrow(s.trend());
        g.drawString(mc.font,trend,x+w-13,y+4,ThermalColors.trend(s.trend()),true);
        g.fill(x+8,y+14,x+w-5,y+15,0x504e5052);
        g.drawString(mc.font,String.format(Locale.ROOT,"%+.0f°",s.air()),x+6,y+16,0xffd3d6d7,true);
        // Only show a recognizable drop when wet and a sun glyph when local radiant heat exists.
        if(s.wet()>.05){int dx=x+w-23,dy=y+16;g.fill(dx+2,dy,dx+3,dy+2,0xff78b4e1);g.fill(dx+1,dy+2,dx+4,dy+4,0xff78b4e1);g.fill(dx,dy+4,dx+5,dy+6,0xff78b4e1);g.fill(dx+1,dy+6,dx+4,dy+7,0xff78b4e1);}
        if(s.radiant()>.5){int dx=x+w-12,dy=y+16;g.fill(dx+2,dy,dx+4,dy+7,0xffe99b54);g.fill(dx,dy+2,dx+6,dy+5,0xffe99b54);g.fill(dx+2,dy+2,dx+4,dy+5,0xffffd78a);}
        if(s.condition()!=ThermalCondition.Kind.NONE)g.fill(x+3,y+3,x+4,y+23,0xffd64b40);
        scale(g,x+5,y+27,w-10,stage.ordinal());
    }
    @EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.GAME,value=Dist.CLIENT)
    public static final class Connection {
        private static WeakReference<Button> tab=new WeakReference<>(null);
        @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event){
            if(!(event.getScreen() instanceof InventoryScreen screen))return;
            int x=screen.getGuiLeft()+2,y=Math.max(2,screen.getGuiTop()-20);
            Button button=Button.builder(Component.translatable("screen.worldlogic_thermal.title"),b->{
                if(screen.getMenu().getCarried().isEmpty())Minecraft.getInstance().setScreen(new ThermalScreen(screen));
            }).bounds(x,y,94,20).build();
            tab=new WeakReference<>(button);button.visible=!screen.getRecipeBookComponent().isVisible();event.addListener(button);
        }
        @SubscribeEvent public static void panel(ScreenEvent.Render.Post event){
            if(!(event.getScreen() instanceof InventoryScreen screen))return;
            var button=tab.get();
            if(button!=null){button.visible=!screen.getRecipeBookComponent().isVisible();button.active=screen.getMenu().getCarried().isEmpty();button.setPosition(screen.getGuiLeft()+2,Math.max(2,screen.getGuiTop()-20));}
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){ThermalSync.clear();tab.clear();}
        @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event){ThermalSync.clear();tab.clear();}
    }
}
