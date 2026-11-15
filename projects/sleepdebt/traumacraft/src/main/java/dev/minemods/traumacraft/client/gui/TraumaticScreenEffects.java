package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

/** Restrained 2D tints; no invasive post-processing and no shader dependencies. */
public final class TraumaticScreenEffects {
    private TraumaticScreenEffects() {}
    public static void render(GuiGraphics gui, DeltaTracker ignored) {
        Minecraft mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen!=null)return;
        int w=gui.guiWidth(),h=gui.guiHeight();
        if(ClientBodyState.painkillerTicks()>0){
            gui.fill(0,0,w,h,0x119C774A); // understated warm sepia
        }
        if(ClientBodyState.concussionTicks()>0){
            gui.fill(0,0,w,9,0x32483E42);
            gui.fill(0,h-9,w,h,0x32483E42);
            gui.fill(0,0,9,h,0x2A483E42);
            gui.fill(w-9,0,w,h,0x2A483E42);
            // Short quiet horizontal visual disturbances, not permanent blur.
            if(mc.player.tickCount%45<6){
                int cy=h/2+(mc.player.tickCount%7-3)*3;
                gui.fill(0,cy,w,cy+1,0x1D9A827A);
            }
        }
    }
}
