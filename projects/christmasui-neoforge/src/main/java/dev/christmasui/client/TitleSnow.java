package dev.christmasui.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class TitleSnow {
    private TitleSnow() {}
    public static void render(ScreenEvent.Render.Post event) {
        if (!(event.getScreen() instanceof TitleScreen screen)) return;
        GuiGraphics g = event.getGuiGraphics();
        int w=screen.width, h=screen.height;
        if(w<1 || h<1) return;
        double time=(System.nanoTime()/1_000_000L % 3_600_000L)/1000.0;
        for(int i=0;i<56;i++){
            int seed=i*7919+107;
            int x=Math.floorMod((int)(seed+Math.sin(time*0.42+i)*8),w);
            int y=Math.floorMod((int)(seed/7+time*(6+i%9)),h);
            // Keep title, labels and controls unobscured.
            if(x>w/2-112 && x<w/2+112) continue;
            int size=i%11==0?2:1;
            int color=i%3==0?0xB4D9EFF4:0x80EFF9F5;
            g.fill(x,y,x+size,y+size,color);
            if(i%11==0) {
                g.fill(x-1,y,x+3,y+1,0x70DDEFF5);
                g.fill(x,y-1,x+1,y+3,0x70DDEFF5);
            }
        }
    }
}
