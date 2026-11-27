package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.resources.ResourceLocation;
import com.mojang.blaze3d.systems.RenderSystem;

/** One client-only layer. Vanilla ice frame plus animated heat shimmer at the edges.
 * No global shader changes, no camera motion or flashing.
 */
public final class ThermalScreenEffects {
    private static final ResourceLocation ICE=ResourceLocation.withDefaultNamespace("textures/misc/powder_snow_outline.png");
    private static float cold,hot;
    private static long last;
    private static Object player;
    private ThermalScreenEffects(){}
    private static int tint(int rgb,float strength,int max){return (ThermalVisualProfile.alpha(strength,max)<<24)|rgb;}
    public static void reset(){cold=hot=0;last=0;player=null;}
    public static void render(GuiGraphics g,DeltaTracker delta){
        var mc=Minecraft.getInstance();var p=mc.player;
        if(p==null||!p.isAlive()||p.isCreative()||p.isSpectator()||!ClientBodyState.hasReceived()){reset();return;}
        if(player!=p){reset();player=p;}
        long now=System.nanoTime();float dt=last==0?1f/60:Math.min(.1f,(now-last)/1_000_000_000f);last=now;
        if(mc.isPaused()||mc.options.hideGui||mc.screen!=null)return;
        var state=ClientBodyState.thermal();
        cold=ThermalVisualProfile.approach(cold,ThermalVisualProfile.target(state,true),dt);
        hot=ThermalVisualProfile.approach(hot,ThermalVisualProfile.target(state,false),dt);
        int w=g.guiWidth(),h=g.guiHeight();
        if(w<32||h<32)return;
        if(cold>.005f){
            int edge=Math.max(2,Math.round(Math.min(w,h)*(.04f+.13f*cold)));
            edges(g,w,h,edge,0xABCDEB,cold,60);
            // Existing Minecraft frost texture, stretched like the vanilla freezing overlay.
            RenderSystem.enableBlend();
            g.setColor(1f,1f,1f,.72f*cold);
            try{g.blit(ICE,0,0,w,h,0f,0f,256,256,256,256);}
            finally{g.setColor(1f,1f,1f,1f);RenderSystem.disableBlend();}
        }
        if(hot>.005f){
            int edge=Math.max(2,Math.round(Math.min(w,h)*(.05f+.18f*hot)));
            edges(g,w,h,edge,0xDD6C32,hot,66);
            float time=(now%20_000_000_000L)/1_000_000_000f;
            // Slow, irregular warm bands only within peripheral areas; center remains clear.
            int step=Math.max(2,edge/10);
            for(int y=0;y<edge;y+=step){
                float phase=(float)(.5+.5*Math.sin(time*1.6+y*.29));
                int span=Math.max(1,(int)(edge*(.35+.55*phase)));
                int col=tint(0xEFB16B,hot*phase,22);
                g.fill(0,y,span,y+1,col);g.fill(w-span,y,w,y+1,col);
                g.fill(0,h-1-y,span,h-y,col);g.fill(w-span,h-1-y,w,h-y,col);
            }
            // Low-frequency broad moving gradient, not true framebuffer refraction.
            float glow=(float)(.8+.2*Math.sin(time*1.1));
            g.fillGradient(0,h-edge,w,h,0x00E8A15C,tint(0xE8A15C,hot*glow,36));
        }
    }
    private static void edges(GuiGraphics g,int w,int h,int edge,int rgb,float power,int max){
        int clear=rgb;
        g.fillGradient(0,0,w,edge,tint(rgb,power,max),clear);
        g.fillGradient(0,h-edge,w,h,clear,tint(rgb,power,max));
        int step=Math.max(1,edge/20);
        for(int x=0;x<edge;x+=step){
            int c=tint(rgb,power*(1f-x/(float)edge),max);
            int next=Math.min(edge,x+step);
            g.fill(x,edge,next,h-edge,c);
            g.fill(w-next,edge,w-x,h-edge,c);
        }
    }
}
