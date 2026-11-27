package dev.minemods.thermal.client;
import dev.minemods.thermal.ThermalModel;
import dev.minemods.thermal.network.ThermalSync;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;
public final class ThermalScreen extends Screen {
    private final Screen parent;
    ThermalScreen(Screen parent) { super(Component.translatable("screen.worldlogic_thermal.title")); this.parent=parent; }
    @Override protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), b->onClose()).bounds(width/2-50,height/2+81,100,20).build());
    }
    @Override public void onClose(){ if(minecraft!=null)minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen(){ return false; }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partialTick) {
        renderBackground(g,mouseX,mouseY,partialTick);
        int x=width/2-112,y=height/2-90;
        g.fill(x,y,x+224,y+166,0xff292b2d);g.fill(x+1,y+1,x+223,y+165,0xff77716b);
        g.fill(x+3,y+3,x+221,y+163,0xffd1c8b7);
        g.drawCenteredString(font,title,width/2,y+9,0xff36312a);
        var p=minecraft==null?null:minecraft.player;
        var s=p==null?null:ThermalSync.current(p.getId());
        if(s==null||!s.active())g.drawCenteredString(font,Component.translatable("screen.worldlogic_thermal.inactive"),width/2,y+50,0xff423d34);
        else {
            var stage=ThermalModel.stage(s.body()); int color=ThermalHud.COLORS[stage.ordinal()];
            g.fill(x+10,y+23,x+214,y+47,0xff343638);
            g.drawString(font,Component.translatable("stage.worldlogic_thermal."+stage.name().toLowerCase(Locale.ROOT)),x+15,y+31,color,false);
            String body=String.format(Locale.ROOT,"%.2f °C",s.body());g.drawString(font,body,x+208-font.width(body),y+31,color,false);
            for(int i=0;i<7;i++)g.fill(x+12+i*29,y+50,x+39+i*29,y+55,ThermalHud.COLORS[i]);
            int m=x+25+stage.ordinal()*29;g.fill(m,y+48,m+3,y+55,0xff292b2d);
            line(g,x+14,y+64,"air",String.format(Locale.ROOT,"%+.1f °C",s.air()));
            line(g,x+14,y+77,"felt",String.format(Locale.ROOT,"%+.1f °C",s.felt()));
            line(g,x+14,y+90,"wet",String.format(Locale.ROOT,"%.0f%%",s.wet()*100));
            line(g,x+14,y+103,"insulation",String.format(Locale.ROOT,"%.0f%%",s.insulation()*100));
            line(g,x+14,y+116,"heat",String.format(Locale.ROOT,"%+.1f °C",s.radiant()));
            line(g,x+14,y+129,"trend",String.format(Locale.ROOT,"%+.2f °C/min",s.trend()));
            g.drawString(font,Component.translatable(s.sheltered()?"screen.worldlogic_thermal.covered":"screen.worldlogic_thermal.exposed"),x+14,y+144,0xff3f5042,false);
        }
        super.render(g,mouseX,mouseY,partialTick);
    }
    private void line(GuiGraphics g,int x,int y,String key,String value){
        g.drawString(font,Component.translatable("screen.worldlogic_thermal."+key),x,y,0xff37332b,false);
        g.drawString(font,value,x+196-font.width(value),y,0xff37332b,false);
    }
}
