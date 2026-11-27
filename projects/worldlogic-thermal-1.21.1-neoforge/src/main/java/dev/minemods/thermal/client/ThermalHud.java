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
        for(int i=0;i<7;i++){
            int left=x+i*w/7,right=x+(i+1)*w/7-1;
            g.fill(left,y,right,y+3,COLORS[i]);g.fill(left,y+3,right,y+4,0xff414143);
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
        g.fill(x+4,y+4,x+6,y+22,color);
        g.drawString(mc.font,String.format(Locale.ROOT,"%.1f°",s.body()),x+9,y+4,color,true);
        String trend=s.trend()>.12?"↑":s.trend()<-.12?"↓":"=";
        g.drawString(mc.font,trend,x+w-13,y+4,color,true);
        g.fill(x+8,y+14,x+w-5,y+15,0x504e5052);
        g.drawString(mc.font,String.format(Locale.ROOT,"%+.0f°",s.air()),x+9,y+16,0xffd3d6d7,true);
        // Small water and radiant-heat bars, no body silhouette.
        g.fill(x+w-23,y+17,x+w-16,y+22,0xff151719);
        g.fill(x+w-23,y+17,x+w-23+(int)Math.round(7*s.wet()),y+22,0xff78b4e1);
        g.fill(x+w-13,y+17,x+w-6,y+22,0xff151719);
        g.fill(x+w-13,y+17,x+w-13+(int)Math.round(7*Math.min(1,s.radiant()/30)),y+22,0xffe99b54);
        if(s.condition()!=ThermalCondition.Kind.NONE)g.fill(x+3,y+3,x+4,y+23,0xffd64b40);
        scale(g,x+5,y+27,w-10,stage.ordinal());
    }
    @EventBusSubscriber(modid=ThermalMod.ID,bus=EventBusSubscriber.Bus.GAME,value=Dist.CLIENT)
    public static final class Connection {
        private static WeakReference<InventoryScreen> open=new WeakReference<>(null);
        private static WeakReference<Button> tab=new WeakReference<>(null);
        private static ThermalLayout.Rect side(InventoryScreen screen){
            var mc=Minecraft.getInstance();
            return ThermalLayout.side(mc.getWindow().getGuiScaledWidth(),mc.getWindow().getGuiScaledHeight(),screen.getGuiLeft(),screen.getGuiTop(),screen.getXSize(),screen.getRecipeBookComponent().isVisible());
        }
        @SubscribeEvent public static void inventory(ScreenEvent.Init.Post event){
            if(!(event.getScreen() instanceof InventoryScreen screen))return;
            int x=screen.getGuiLeft()+2,y=Math.max(2,screen.getGuiTop()-20);
            Button button=Button.builder(Component.translatable("screen.worldlogic_thermal.title"),b->{
                if(open.get()==screen){open.clear();return;}
                if(side(screen)!=null)open=new WeakReference<>(screen);
                else Minecraft.getInstance().setScreen(new ThermalScreen(screen));
            }).bounds(x,y,94,20).build();
            tab=new WeakReference<>(button);button.visible=!screen.getRecipeBookComponent().isVisible();event.addListener(button);
        }
        @SubscribeEvent public static void panel(ScreenEvent.Render.Post event){
            if(!(event.getScreen() instanceof InventoryScreen screen))return;
            var button=tab.get();
            if(button!=null){button.visible=!screen.getRecipeBookComponent().isVisible();button.setPosition(screen.getGuiLeft()+2,Math.max(2,screen.getGuiTop()-20));}
            if(open.get()!=screen)return;
            var rect=side(screen);
            if(rect==null){open.clear();return;}
            var p=Minecraft.getInstance().player;
            ThermalPanel.draw(event.getGuiGraphics(),rect,p==null?null:ThermalSync.current(p.getId()));
        }
        @SubscribeEvent public static void click(ScreenEvent.MouseButtonPressed.Pre event){
            if(event.getScreen() instanceof InventoryScreen screen&&open.get()==screen){
                var rect=side(screen);
                if(rect!=null&&rect.contains(event.getMouseX(),event.getMouseY()))event.setCanceled(true);
            }
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){ThermalSync.clear();open.clear();tab.clear();}
        @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event){ThermalSync.clear();open.clear();tab.clear();}
    }
}
