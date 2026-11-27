package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;

/** Peripheral blood/pain vignette and progressive shock tunnel vision. */
public final class TraumaticScreenEffects {
    private static float dark,red,concussion;
    private static long last;
    private static Object player;
    private TraumaticScreenEffects(){}
    public static void reset(){dark=red=concussion=0;last=0;player=null;}
    public static void render(GuiGraphics gui,DeltaTracker ignored){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!p.isAlive()||p.isCreative()||p.isSpectator()||!ClientBodyState.hasReceived()){reset();return;}
        if(player!=p){reset();player=p;}
        long now=System.nanoTime();float dt=last==0?1/60f:Math.min(.1f,(now-last)/1_000_000_000f);last=now;
        if(mc.options.hideGui||mc.screen!=null||mc.isPaused())return;
        int wounds=0;
        for(var part:BodyPart.values())wounds+=ClientBodyState.bleeding(part);
        float blood=ClientBodyState.bloodMl()/5000f;
        dark=InjuryVisualProfile.ease(dark,InjuryVisualProfile.dark(blood,ClientBodyState.shock(),ClientBodyState.concussionTicks()>0),dt);
        red=InjuryVisualProfile.ease(red,InjuryVisualProfile.red(ClientBodyState.pain(),wounds),dt);
        concussion=InjuryVisualProfile.ease(concussion,ClientBodyState.concussionTicks()>0?1:0,dt);
        int w=gui.guiWidth(),h=gui.guiHeight(),min=Math.min(w,h);
        if(min<32)return;
        float phase=(now%8_000_000_000L)/1_000_000_000f;
        float pulse=.91f+.09f*(float)Math.sin(phase*Math.PI/2);
        // Low-opacity warm border for active bleeding and pain, not a full red flash.
        if(red>.005f)edge(gui,w,h,Math.max(2,(int)(min*(.10f+.12f*red))),0x802A29,red*pulse,105);
        // Gradual darkening and widening of peripheral vision loss.
        if(dark>.005f){
            edge(gui,w,h,Math.max(2,(int)(min*(.14f+.27f*dark))),0x11151B,dark,205);
            if(dark>.6f)gui.fill(0,0,w,h,((int)((dark-.6f)*60)<<24)|0x12151B);
        }
        // Quiet cool-gray disturbance on concussion. No rapid flicker or nausea shader.
        if(concussion>.005f){
            int band=Math.max(2,min/12);
            gui.fillGradient(0,0,w,band,((int)(24*concussion)<<24)|0xA6B4BF,0x00A6B4BF);
        }
    }
    private static void edge(GuiGraphics g,int w,int h,int e,int rgb,float power,int max){
        int steps=24,prev=0;
        for(int i=0;i<steps;i++){
            int next=Math.max(prev,(i+1)*e/steps);
            if(next<=prev)continue;
            float t=1-i/(float)steps;
            int color=(Math.min(max,Math.max(0,Math.round(max*power*t*t)))<<24)|rgb;
            g.fill(prev,prev,w-prev,next,color);
            g.fill(prev,h-next,w-prev,h-prev,color);
            g.fill(prev,next,next,h-next,color);
            g.fill(w-next,next,w-prev,h-next,color);
            prev=next;
        }
    }
}
