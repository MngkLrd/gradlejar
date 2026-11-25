package dev.christmasui.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.neoforged.neoforge.client.event.ScreenEvent;
public final class ButtonSnow {
    private ButtonSnow() {}
    public static void render(ScreenEvent.Render.Post event) {
        GuiGraphics g=event.getGuiGraphics();
        for(GuiEventListener listener:event.getScreen().children()){
            if(!(listener instanceof AbstractButton b)||!b.visible||!b.active||b.getWidth()<65||b.getHeight()!=20)continue;
            int x=b.getX(),y=b.getY(),w=b.getWidth();
            if(x<0||y<0||x+w>event.getScreen().width||y+20>event.getScreen().height)continue;
            int seed=b.getMessage().getString().hashCode()^(y*7919+x*31);
            // Add tiny icicles to existing snow instead of erasing the hand-shaped cap.
            for(int i=0;i<2;i++){
                int px=x+13+Math.floorMod(Integer.rotateLeft(seed,i*13),w-27);
                g.fill(px,y+1,px+5,y+3,0xFFEAF4EA);
                g.fill(px+1,y+3,px+3,y+5,0xFFC1D9DE);
                g.fill(px+1,y+5,px+2,y+6,0xFF9CBBC5);
            }
        }
    }
}
