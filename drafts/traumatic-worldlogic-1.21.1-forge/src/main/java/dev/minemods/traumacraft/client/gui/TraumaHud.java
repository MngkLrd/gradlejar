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
    // Ordered exactly as HudStatusLayout bits. No vanilla-effect duplicates.
    private static final ResourceLocation[] ICONS={
        icon("bleeding"),icon("fracture"),icon("concussion"),icon("painkiller")
    };

    private static ResourceLocation icon(String name) {
        return ResourceLocation.fromNamespaceAndPath(TraumaCraft.MOD_ID, "textures/gui/status/"+name+".png");
    }
    private TraumaHud() {}

    public static void render(GuiGraphics gui, DeltaTracker ignored) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null || !mc.player.isAlive() || mc.options.hideGui || mc.screen!=null || !ClientBodyState.hasReceived())return;
        TreatmentRing.render(gui);
        if(mc.player.isCreative() || mc.player.isSpectator())return;
        boolean bleeding=false, fracture=false;
        for(BodyPart part:BodyPart.values()){
            bleeding|=ClientBodyState.bleeding(part)>0;
            fracture|=ClientBodyState.fractured(part);
        }
        int mask=HudStatusLayout.mask(bleeding,fracture,
            ClientBodyState.concussionTicks(),ClientBodyState.painkillerTicks());
        if(!HudStatusLayout.visible(ClientBodyState.hasAnyInjuries(),mask))return;
        int px=gui.guiWidth()/2-91-80, py=gui.guiHeight()-98;
        if(px<4){px=4;py=4;}
        gui.blit(PANEL,px,py,0,0,76,94,76,94);
        int x=px+4,y=py+4;
        BodyVisual.draw(gui,x,y,true);
        int col=x+48;
        for(int i=0;i<ICONS.length;i++){
            int row=HudStatusLayout.row(mask,i);
            if(row>=0)draw(gui,ICONS[i],col,y+row*HudStatusLayout.STEP);
        }
    }
    private static void draw(GuiGraphics gui,ResourceLocation icon,int x,int y) {
        gui.blit(icon,x,y,0,0,20,20,20,20);
    }
}
