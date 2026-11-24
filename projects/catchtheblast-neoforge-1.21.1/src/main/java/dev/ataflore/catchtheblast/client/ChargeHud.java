package dev.ataflore.catchtheblast.client;

import dev.ataflore.catchtheblast.CatchTheBlast;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.item.Charge;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid=CatchTheBlast.ID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.GAME)
public final class ChargeHud {
    @SubscribeEvent public static void draw(RenderGuiEvent.Post event) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null || mc.options.hideGui || mc.player.isSpectator() || mc.screen!=null) return;
        ItemStack stack=mc.player.getMainHandItem();
        if(!(stack.getItem() instanceof BlastShieldItem) || Charge.get(stack)<=100) stack=mc.player.getOffhandItem();
        if(!(stack.getItem() instanceof BlastShieldItem) || Charge.get(stack)<=100) return;
        GuiGraphics g=event.getGuiGraphics();
        int charge=Charge.get(stack), width=92, x=(g.guiWidth()-width)/2;
        int y=Math.min(g.guiHeight()/2+34,g.guiHeight()-82);
        int fill=Math.round(Math.min(charge,100)*width/100f);
        g.fill(x-2,y-2,x+width+2,y+8,0xD0231D19);
        g.fill(x,y,x+width,y+6,0xC03C3330);
        for(int i=0;i<fill;i++) {
            float p=i/(float)(width-1);
            int r=(int)(p<.5f?85+300*p:235), gr=(int)(p<.5f?185+80*p:225-340*(p-.5f));
            int color=0xFF000000|(r<<16)|(gr<<8)|47;
            g.fill(x+i,y,x+i+1,y+5,color);
        }
        for(int i=1;i<10;i++) { int mark=x+i*width/10; g.fill(mark,y,mark+1,y+6,0x603A241E); }
        boolean over=charge>100;
        Component label=Component.translatable("hud.catchtheblast.charge",charge);
        g.drawCenteredString(mc.font,label,x+width/2,y-11,over?0xFFFF7652:0xFFF4E3B5);
        if(over) {
            long ticks=Math.max(0,Math.min(70,Charge.deadline(stack)-mc.level.getGameTime()));
            int remaining=(int)Math.ceil(width*ticks/70f);
            int yy=y+11;
            g.fill(x-2,yy-2,x+width+2,yy+6,0xE03A1914);
            g.fill(x,yy,x+width,yy+4,0xFF4D211B);
            g.fill(x,yy,x+remaining,yy+4,ticks%10<5?0xFFFF6336:0xFFFFA44B);
            for(int i=1;i<7;i++){int mark=x+i*width/7;g.fill(mark,yy,mark+1,yy+4,0x90271913);}
            g.drawCenteredString(mc.font,Component.literal(String.format(Locale.ROOT,"%.2fs  |  %dt",ticks/20.0,ticks)),x+width/2,yy+7,0xFFFF8862);
        }
    }
}
