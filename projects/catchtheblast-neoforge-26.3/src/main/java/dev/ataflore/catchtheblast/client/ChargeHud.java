package dev.ataflore.catchtheblast.client;

import dev.ataflore.catchtheblast.CatchTheBlast;
import dev.ataflore.catchtheblast.item.BlastShieldItem;
import dev.ataflore.catchtheblast.item.Charge;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ChargeHud {
    private static final int WIDTH=82;
    private static final int GREEN=0x61C647, GOLD=0xF5C74A, RED=0xEC4938;
    private ChargeHud() {}
    private static int heldCharge(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof BlastShieldItem ? Charge.get(stack) : 0;
    }
    public static void draw(GuiGraphicsExtractor g) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.level==null || !mc.player.isAlive() ||
                mc.player.isSpectator() || mc.screen!=null) return;
        ItemStack main=mc.player.getMainHandItem(), off=mc.player.getOffhandItem();
        int a=heldCharge(main), b=heldCharge(off);
        ItemStack shield=a>=b?main:off;
        int charge=Math.max(a,b);
        if(charge<=0) return;
        int x=(g.guiWidth()-WIDTH)/2, y=g.guiHeight()/2+14;
        boolean overload=charge>100;
        bar(g,x,y,Math.min(charge,100)/100f,false);
        if(overload) {
            long ticks=Math.max(0,Math.min(70,Charge.deadline(shield)-mc.level.getGameTime()));
            bar(g,x,y+7,ticks/70f,true);
            g.centeredText(mc.font,Component.translatable("hud.catchtheblast.overload",
                String.format(Locale.ROOT,"%.2f",ticks/20.0),ticks),x+WIDTH/2,y+14,0xFFFF9565);
        }
    }
    // Pixel-sharp five-pixel XP-style track; both states use the same palette.
    private static void bar(GuiGraphicsExtractor g,int x,int y,float amount,boolean countdown) {
        int n=Math.max(0,Math.min(WIDTH-2,Math.round(amount*(WIDTH-2))));
        if(amount>0) n=Math.max(1,n);
        g.fill(x+1,y,x+WIDTH-1,y+5,0xFF171512);
        g.fill(x,y+1,x+WIDTH,y+4,0xFF171512);
        g.fill(x+1,y+1,x+WIDTH-1,y+4,0xFF49423B);
        for(int i=0;i<n;i++) {
            float p=i/(float)(WIDTH-3);
            // Countdown heats up as it empties: its visible end always reaches red.
            if(countdown) p=1f-amount+i/(float)(Math.max(1,n-1))*amount;
            int c=p<.5f?mix(GREEN,GOLD,p*2):mix(GOLD,RED,(p-.5f)*2);
            g.fill(x+1+i,y+1,x+2+i,y+2,0xFF000000|mix(c,0xFFFFFF,.24f));
            g.fill(x+1+i,y+2,x+2+i,y+3,0xFF000000|c);
            g.fill(x+1+i,y+3,x+2+i,y+4,0xFF000000|mix(c,0x000000,.34f));
        }
    }
    private static int mix(int a,int b,float t) {
        t=Math.max(0,Math.min(1,t));
        int r=Math.round(((a>>16)&255)*(1-t)+((b>>16)&255)*t);
        int g=Math.round(((a>>8)&255)*(1-t)+((b>>8)&255)*t);
        int bl=Math.round((a&255)*(1-t)+(b&255)*t);
        return (r<<16)|(g<<8)|bl;
    }
}
