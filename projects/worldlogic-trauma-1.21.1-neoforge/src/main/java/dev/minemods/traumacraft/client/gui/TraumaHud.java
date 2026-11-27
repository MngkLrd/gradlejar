package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Frameless native-pixel anatomy HUD. Only active effect glyphs occupy space. */
public final class TraumaHud {
    // Ordered exactly as HudStatusLayout bits. No vanilla-effect duplicates.
    private static final ResourceLocation[] ICONS={
        icon("bleeding"),icon("fracture"),icon("concussion"),icon("painkiller"),icon("thermal_cold"),icon("thermal_heat")
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
        int mask=activeMask();
        if(!HudStatusLayout.visible(ClientBodyState.hasAnyInjuries(),mask))return;
        int x=AnatomyLayout.hudX(gui.guiWidth(),mask), y=AnatomyLayout.hudY(gui.guiWidth(),gui.guiHeight(),mask);
        BodyVisual.draw(gui,x,y,true);
        int count=Integer.bitCount(mask);
        if(count>0){
            int cols=HudStatusLayout.hudColumns(mask),rows=(count+cols-1)/cols;
            drawActive(gui,x+BodyVisual.WIDTH+4,y+BodyVisual.HEIGHT-HudStatusLayout.ICON_SIZE-(rows-1)*HudStatusLayout.STEP,cols);
        }
    }
    public static int activeMask(){
        boolean bleeding=false, fracture=false;
        for(BodyPart part:BodyPart.values()){
            bleeding|=ClientBodyState.bleeding(part)>0;
            fracture|=ClientBodyState.fractured(part);
        }
        return HudStatusLayout.withThermal(HudStatusLayout.mask(bleeding,fracture,ClientBodyState.concussionTicks(),ClientBodyState.painkillerTicks()),ClientBodyState.thermal());
    }
    public static void drawActive(GuiGraphics gui,int x,int y,int columns){
        if(!ClientBodyState.hasReceived())return;
        int mask=activeMask();
        for(int i=0;i<ICONS.length;i++){
            int row=HudStatusLayout.row(mask,i);
            if(row>=0){
                int dx=x+(row%columns)*HudStatusLayout.STEP,dy=y+(row/columns)*HudStatusLayout.STEP;
                if(i>=4)drawThermal(gui,dx,dy);
                else draw(gui,ICONS[i],dx,dy);
            }
        }
    }
    private static void drawThermal(GuiGraphics g,int x,int y){
        var state=ClientBodyState.thermal();
        // Clear snow crystal vs flame; clinical sunstroke uses the distinct solar glyph.
        ResourceLocation sprite=switch(state){
            case SUNSTROKE -> icon("thermal_sun");
            case HEATSTROKE -> icon("thermal_stroke");
            default -> ICONS[state.cold()?4:5];
        };
        draw(g,sprite,x,y);
        int shade=state.cold()?0xFFC0E5F4:0xFFFFC16A;
        for(int i=0;i<state.severity();i++){
            g.fill(x+2+i*4,y+14,x+5+i*4,y+16,0xFF3E4146);
            g.fill(x+2+i*4,y+14,x+4+i*4,y+15,shade);
        }
    }
    private static void draw(GuiGraphics gui,ResourceLocation icon,int x,int y) {
        gui.blit(icon,x,y,0,0,HudStatusLayout.ICON_SIZE,HudStatusLayout.ICON_SIZE,HudStatusLayout.ICON_SIZE,HudStatusLayout.ICON_SIZE);
    }
}
