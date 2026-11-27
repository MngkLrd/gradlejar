package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Approved framed anatomical display; original pixel dimensions, no fractional scaling. */
public final class TraumaHud {
    private static final ResourceLocation PANEL=ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID,"textures/gui/trauma/panel.png");
    private static final ResourceLocation BLEED=icon("bleeding");
    private static final ResourceLocation FRACTURE=icon("fracture");
    private static final ResourceLocation CONCUSSION=icon("concussion");
    private static final ResourceLocation PAINKILLER=icon("painkiller");

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "textures/gui/status/"+name+".png");
    }
    private TraumaHud() {}

    public static void render(GuiGraphics gui, DeltaTracker ignored) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || mc.options.hideGui || mc.screen!=null || !ClientBodyState.hasReceived())return;
        TreatmentRing.render(gui);
        if(mc.player.isCreative() || mc.player.isSpectator())return;
        if(!ClientBodyState.hasAnyInjuries() && ClientBodyState.concussionTicks()==0
                && ClientBodyState.painkillerTicks()==0)return;
        int px=gui.guiWidth()/2-91-80, py=gui.guiHeight()-98;
        if(px<4){px=4;py=4;}
        gui.blit(PANEL,px,py,0,0,76,94,76,94);
        int x=px+4,y=py+4;
        BodyVisual.draw(gui,x,y,true);
        int bleeding=0, fractures=0;
        for(BodyPart part:BodyPart.values()){
            bleeding+=ClientBodyState.bleeding(part);
            if(ClientBodyState.fractured(part))fractures++;
        }
        int col=x+48;
        int iy=y;
        if(bleeding>0){draw(gui,BLEED,col,iy);iy+=22;}
        if(fractures>0){draw(gui,FRACTURE,col,iy);iy+=22;}
        if(ClientBodyState.concussionTicks()>0){draw(gui,CONCUSSION,col,iy);iy+=22;}
        if(ClientBodyState.painkillerTicks()>0)draw(gui,PAINKILLER,col,iy);
    }
    private static void draw(GuiGraphics gui,ResourceLocation icon,int x,int y) {
        gui.blit(icon,x,y,0,0,20,20,20,20);
    }
}
