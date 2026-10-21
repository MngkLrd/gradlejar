package dev.minemods.traumacraft.client.gui;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public final class TraumaHud {
    private TraumaHud(){}
    public static void render(GuiGraphicsExtractor gui){
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen!=null||!ClientBodyState.hasReceived())return;
        int x=gui.guiWidth()/2-157,y=gui.guiHeight()-68;
        boolean bad=ClientBodyState.hasAnyInjuries();
        gui.fill(x,y,x+56,y+61,bad?0xA90D1822:0x620D1822);
        gui.fill(x,y,x+56,y+1,0x775F7777);
        gui.text(mc.font,"HP",x+4,y+4,bad?0xFFE4E8E3:0xAAB3B8B4,false);
        // Render at native HUD resolution: 7 zones, no fractional sampling.
        mini(gui,BodyPart.HEAD,x+25,y+14,7,7);
        mini(gui,BodyPart.CHEST,x+23,y+23,11,11);
        mini(gui,BodyPart.STOMACH,x+23,y+35,11,5);
        mini(gui,BodyPart.LEFT_ARM,x+18,y+23,4,18);
        mini(gui,BodyPart.RIGHT_ARM,x+35,y+23,4,18);
        mini(gui,BodyPart.LEFT_LEG,x+23,y+42,5,12);
        mini(gui,BodyPart.RIGHT_LEG,x+29,y+42,5,12);
        if(bad)gui.text(mc.font,"!",x+43,y+3,0xFFFFBA6B,true);
    }
    private static void mini(GuiGraphicsExtractor gui,BodyPart p,int x,int y,int w,int h){
        gui.fill(x-1,y-1,x+w+1,y+h+1,0xCC12191E);
        gui.fill(x,y,x+w,y+h,BodyVisual.hpColor(p,true));
        if(ClientBodyState.bleeding(p)>0)gui.fill(x+w-2,y+h-3,x+w,y+h,0xFFF03F44);
        if(ClientBodyState.fractured(p))gui.fill(x+w/2,y+2,x+w/2+1,y+h-2,0xFFEEEEEE);
    }
}
