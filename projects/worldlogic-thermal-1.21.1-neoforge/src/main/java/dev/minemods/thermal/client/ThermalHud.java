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
    static void scale(GuiGraphics g,int x,int y,int w,double body){
        g.fill(x-1,y-1,x+w+1,y+4,0xff121212);
        for(int i=0;i<w;i++){
            double c=30+13.0*i/Math.max(1,w-1);
            g.fill(x+i,y,x+i+1,y+3,COLORS[ThermalModel.stage(c).ordinal()]);
        }
        int marker=x+(int)Math.round(dev.minemods.thermal.HudSignals.bodyPosition(body)*(w-1));
        g.fill(marker-1,y-2,marker+2,y,0xfffaf5df);g.fill(marker,y,marker+1,y+4,0xfffaf5df);
    }
    private static void render(GuiGraphics g,DeltaTracker delta){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!p.isAlive()||mc.options.hideGui||mc.screen!=null||p.isCreative()||p.isSpectator())return;
        var s=ThermalSync.current(p.getId());if(s==null||!s.active())return;
        var r=ThermalLayout.hud(g.guiWidth(),g.guiHeight());int x=r.x(),y=r.y(),w=r.w();
        var stage=ThermalModel.stage(s.body());int color=COLORS[stage.ordinal()];
        // Pixel-aligned bevel and inset, based on the vanilla inventory/status widget.
        g.fill(x+1,y+1,x+w+1,y+r.h()+1,0x80000000);
        g.fill(x,y,x+w,y+r.h(),0xee171717);
        g.fill(x+1,y+1,x+w-1,y+r.h()-1,0xff858585);
        g.fill(x+1,y+1,x+w-2,y+2,0xffdedede);g.fill(x+1,y+2,x+2,y+r.h()-2,0xffdedede);
        g.fill(x+w-2,y+2,x+w-1,y+r.h()-1,0xff383838);g.fill(x+2,y+r.h()-2,x+w-1,y+r.h()-1,0xff383838);
        g.fill(x+3,y+3,x+w-3,y+r.h()-3,0xe5232525);
        var font=mc.font;
        String body=String.format(Locale.ROOT,"%.1f°",s.body())+" "+dev.minemods.thermal.HudSignals.trend(s.trend());
        row(g,font,x,y+5,w,"body",body,color);
        boolean water=Double.isFinite(s.waterC());
        double local=water?s.waterC():s.room().indoor()?s.room().airC():s.air();
        row(g,font,x,y+15,w,water?"water":s.room().indoor()?"room":"air",String.format(Locale.ROOT,"%+.1f°",local),water?0xff91c8f0:0xffe0ded4);
        row(g,font,x,y+25,w,"felt",String.format(Locale.ROOT,"%+.1f°",s.felt()),s.felt()<12?0xff91c8f0:s.felt()>28?0xffffa363:0xffc7dfb4);
        scale(g,x+6,y+36,w-12,s.body());
        row(g,font,x,y+43,w,"rate",String.format(Locale.ROOT,"%+.2f°/m",s.trend()),ThermalColors.trend(s.trend()));
        double jump=ThermalSync.recentJump(p.getId());
        if(s.condition()!=ThermalCondition.Kind.NONE)g.fill(x+3,y+4,x+4,y+34,0xffd64b40);
        if(jump!=0)row(g,font,x,y+53,w,"jump",String.format(Locale.ROOT,"%s %+.1f°",jump>0?"↑↑":"↓↓",jump),ThermalColors.trend(jump));
        else if(s.wet()>.05)row(g,font,x,y+53,w,"wet",String.format(Locale.ROOT,"%.0f%%",s.wet()*100),0xff92c9ef);
        else row(g,font,x,y+53,w,"state",Component.translatable("stage.worldlogic_thermal."+stage.name().toLowerCase(Locale.ROOT)).getString(),color);
    }
    private static void row(GuiGraphics g,net.minecraft.client.gui.Font font,int x,int y,int w,String label,String value,int color){
        int right=x+w-6;
        String name=Component.translatable("hud.worldlogic_thermal."+label).getString();
        int maxValue=Math.max(0,w-16-Math.min(30,font.width(name)));
        String v=font.plainSubstrByWidth(value,maxValue);
        int vx=right-font.width(v);
        g.drawString(font,font.plainSubstrByWidth(name,Math.max(0,vx-x-9)),x+6,y,0xffbdbdb7,true);
        g.drawString(font,v,vx,y,color,true);
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
