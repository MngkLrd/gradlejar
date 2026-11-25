package dev.christmasui.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.neoforge.client.event.ScreenEvent;

public final class ButtonSnow {
    private ButtonSnow() {}
    public static void render(ScreenEvent.Render.Post event) {
        GuiGraphics g=event.getGuiGraphics();
        for (GuiEventListener listener : event.getScreen().children()) {
            if (!(listener instanceof AbstractButton button) || !button.visible || button.getWidth()<45 || button.getHeight()!=20) continue;
            int x=button.getX(),y=button.getY(),w=button.getWidth();
            if(x<0||y<0||x+w>event.getScreen().width||y+20>event.getScreen().height) continue;
            int seed=button.getMessage().getString().hashCode() ^ (y*7919+x*31);
            // Clear only the top edge then paint stable, distinct snowdrifts. Never cover labels.
            g.fill(x+3,y,x+w-3,y+2,button.active?0xFF634832:0xFF4F4A43);
            for(int i=0;i<Math.max(2,w/35);i++){
                int mix=Integer.rotateLeft(seed+i*0x9e3779b9,i*3);
                int px=x+4+Math.floorMod(mix,Math.max(1,w-19));
                int len=5+Math.floorMod(mix>>>5,12);
                int end=Math.min(x+w-4,px+len);
                g.fill(px,y,end,y+1,0xFFF0F6ED);
                g.fill(px+1,y+1,Math.max(px+2,end-1),y+2,0xFFAFCCD0);
            }
        }
    }
}
