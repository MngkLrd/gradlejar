package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/** Aligned with the bottom of Minecraft's 182x22 hotbar, with injury badges at right. */
public final class TraumaHud {
    private TraumaHud() {}
    public static void render(GuiGraphics gui, DeltaTracker ignored) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen!=null||!ClientBodyState.hasReceived())return;
        TreatmentRing.render(gui);
        int x=Math.max(3,gui.guiWidth()/2-91-74);
        int y=gui.guiHeight()-78;
        boolean bad=ClientBodyState.hasAnyInjuries()||ClientBodyState.concussionTicks()>0;
        gui.fill(x,y,x+68,y+76,bad?0xAC101A1E:0x65101A1E);
        gui.fill(x,y,x+68,y+1,0x77667479);
        gui.drawString(mc.font,"HP",x+5,y+4,bad?0xFFE4E4E0:0xAAB3B8B4,false);

        gui.pose().pushPose();
        gui.pose().translate(x+11,y+19,0);
        gui.pose().scale(0.40f,0.40f,1f);
        BodyVisual.draw(gui,0,0,true);
        gui.pose().popPose();

        int totalBleed=0,totalFracture=0;
        for(BodyPart p:BodyPart.values()){
            totalBleed+=ClientBodyState.bleeding(p);
            if(ClientBodyState.fractured(p))totalFracture++;
        }
        int ry=y+18;
        if(totalBleed>0){badge(gui,mc,"BLD"+totalBleed,x+34,ry,0xFFF0A3A3);ry+=11;}
        if(totalFracture>0){badge(gui,mc,"FX"+totalFracture,x+34,ry,0xFFF3D49B);ry+=11;}
        if(ClientBodyState.concussionTicks()>0){badge(gui,mc,"CON",x+34,ry,0xFFE1B79A);ry+=11;}
        if(ClientBodyState.painkillerTicks()>0){badge(gui,mc,"MED",x+34,ry,0xFFD7C6A5);}
    }
    private static void badge(GuiGraphics gui,Minecraft mc,String value,int x,int y,int color){
        gui.fill(x-2,y-1,x+mc.font.width(value)+2,y+10,0x77363F42);
        gui.drawString(mc.font,value,x,y,color,false);
    }
}
