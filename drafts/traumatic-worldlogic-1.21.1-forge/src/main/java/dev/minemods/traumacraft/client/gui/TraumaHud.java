package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** No background: only the anatomy silhouette and small square status sprites. */
public final class TraumaHud {
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
        // Exact 1:1 native GUI pixels. Move away from the hotbar at narrow scales.
        int x=gui.guiWidth()/2-91-38, y=gui.guiHeight()-43;
        if(x<4){x=4;y=4;}
        BodyVisual.draw(gui,x,y,true);
        int bleeding=0, fractures=0;
        for(BodyPart part:BodyPart.values()){
            bleeding+=ClientBodyState.bleeding(part);
            if(ClientBodyState.fractured(part))fractures++;
        }
        int col=x+24;
        int iy=y+4;
        if(bleeding>0){draw(gui,BLEED,col,iy);iy+=11;}
        if(fractures>0){draw(gui,FRACTURE,col,iy);iy+=11;}
        if(ClientBodyState.concussionTicks()>0){draw(gui,CONCUSSION,col,iy);iy+=11;}
        if(ClientBodyState.painkillerTicks()>0)draw(gui,PAINKILLER,col,iy);
    }
    private static void draw(GuiGraphics gui,ResourceLocation icon,int x,int y) {
        gui.blit(icon,x,y,0,0,9,9,9,9);
    }
}
