package dev.minemods.worldlogicterrain.growth.client;

import dev.minemods.worldlogicterrain.growth.GrowthSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid="worldlogicterrain",bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class GrowthHud {
    @SubscribeEvent public static void register(RegisterGuiLayersEvent e){
        e.registerAbove(VanillaGuiLayers.CROSSHAIR,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","growth"),GrowthHud::render);
    }
    private static Component tr(String id,Object... args){return Component.translatable("hud.worldlogicterrain.growth."+id,args);}
    private static String pct(int n){return n<0?"—":Math.max(0,Math.min(100,n))+"%";}
    public static void render(GuiGraphics g,DeltaTracker tick){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||mc.options.hideGui||mc.screen!=null)return;
        if(!(mc.hitResult instanceof BlockHitResult hit)||hit.getType()!=HitResult.Type.BLOCK)return;
        var s=GrowthSync.client();if(s==null||!s.pos().equals(hit.getBlockPos())||!s.dimension().equals(mc.level.dimension().location().toString()))return;
        var block=mc.level.getBlockState(s.pos()).getBlock();
        Component title=block.getName(),status=tr("status."+s.status()),season=tr("season."+s.season());
        var lines=new Component[]{title,tr("progress",pct(s.progress()),status),tr("speed",pct(s.speed())),tr("quality",pct(s.quality())),tr("season_line",season)};
        int max=0;for(var line:lines)max=Math.max(max,mc.font.width(line));
        int w=Math.max(108,max+14),h=72;
        if(w>g.guiWidth()-8||h>g.guiHeight()-8)return;
        int x=g.guiWidth()/2+13,y=Math.max(4,Math.min(g.guiHeight()/2-36,g.guiHeight()-h-4));
        if(x+w>g.guiWidth()-4)x=g.guiWidth()/2-w-13;
        x=Math.max(4,Math.min(x,g.guiWidth()-w-4));
        g.fill(x,y,x+w,y+h,0xd0181818);g.fill(x,y,x+w,y+1,0xff59654d);
        g.fill(x,y+h-1,x+w,y+h,0xff59654d);
        int col=s.progress()==100?0xffa4d17c:s.speed()==0?0xffd7a47b:0xffe6e2d7;
        g.drawString(mc.font,lines[0],x+7,y+6,0xffeee8d6,false);
        g.drawString(mc.font,lines[1],x+7,y+18,col,false);
        g.fill(x+7,y+29,x+w-7,y+33,0xff3d4139);
        g.fill(x+7,y+29,x+7+(w-14)*Math.max(0,Math.min(100,s.progress()))/100,y+33,0xff88a968);
        for(int i=2;i<5;i++)g.drawString(mc.font,lines[i],x+7,y+36+(i-2)*10,0xffe4e0d4,false);
    }
}
