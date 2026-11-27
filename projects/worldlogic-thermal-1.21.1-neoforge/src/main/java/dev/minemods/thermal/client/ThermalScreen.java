package dev.minemods.thermal.client;
import dev.minemods.thermal.network.ThermalSync;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class ThermalScreen extends Screen {
    private final Screen parent;
    ThermalScreen(Screen parent) { super(Component.translatable("screen.worldlogic_thermal.title")); this.parent=parent; }
    @Override protected void init(){
        int y=Math.max(2,(height-190)/2);
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width/2-50,y+169,100,20).build());
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){
        // No post-processing of previously drawn GUI. The world alone gets a dark tint.
        g.fill(0,0,width,height,0x99000000);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        super.render(g,mx,my,delta);
        int w=Math.min(ThermalLayout.PANEL_W,Math.max(80,width-4));
        int y=Math.max(2,(height-190)/2);
        var p=minecraft==null?null:minecraft.player;
        ThermalPanel.draw(g,new ThermalLayout.Rect((width-w)/2,y,w,ThermalLayout.PANEL_H),p==null?null:ThermalSync.current(p.getId()));
    }
}
