package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** One native-pixel front-view mannequin. Inventory uses exactly 2x, HUD 1x.
 * Anatomical left is the viewer's right. Render and selection share geometry.
 */
public final class BodyVisual {
    public record Zone(BodyPart part,int x,int y,int width,int height) {
        boolean contains(int mx,int my,int ox,int oy,int s) {
            return mx>=ox+x*s && mx<ox+(x+width)*s && my>=oy+y*s && my<oy+(y+height)*s;
        }
    }
    private static final Zone[] ZONES={
        new Zone(BodyPart.HEAD,8,2,8,8),
        new Zone(BodyPart.CHEST,8,11,8,7),
        new Zone(BodyPart.STOMACH,8,18,8,5),
        new Zone(BodyPart.RIGHT_ARM,3,11,4,12),
        new Zone(BodyPart.LEFT_ARM,17,11,4,12),
        new Zone(BodyPart.RIGHT_LEG,8,24,4,12),
        new Zone(BodyPart.LEFT_LEG,13,24,4,12)
    };
    private static final int[][] PALETTES={
        {0x34362f,0x6d7965,0xa0ae8c,0x505c48}, // intact
        {0x665437,0xb4a16d,0xd3c18d,0x89794c}, // mild
        {0x73432e,0xbe8251,0xd9a66b,0x925c3e}, // serious
        {0x642e30,0xb05d50,0xd58a70,0x803e3c}, // severe
        {0x302b2f,0x52484c,0x746568,0x3e373c}  // disabled
    };
    private BodyVisual(){}
    public static BodyPart hit(int mx,int my,int ox,int oy){
        for(Zone z:ZONES)if(z.contains(mx,my,ox,oy,2))return z.part();
        return null;
    }
    public static Component name(BodyPart part){
        return Component.translatable("body.traumacraft."+part.name().toLowerCase(Locale.ROOT));
    }
    public static String conditionKey(BodyPart part){
        float f=ClientBodyState.fraction(part);
        if(f<=0)return "disabled";
        if(f<.20f)return "severe";
        if(f<.50f)return "injured";
        if(f<.90f)return "sore";
        return "stable";
    }
    public static Component condition(BodyPart part){
        return Component.translatable("condition.traumacraft."+conditionKey(part));
    }
    private static int palette(BodyPart part){
        return switch(conditionKey(part)){
            case "disabled" -> 4;
            case "severe" -> 3;
            case "injured" -> 2;
            case "sore" -> 1;
            default -> 0;
        };
    }
    public static int hpColor(BodyPart part,boolean translucent){
        return (translucent?0xD0000000:0xFF000000)|PALETTES[palette(part)][1];
    }
    private static void pixelRect(GuiGraphics g,int ox,int oy,int s,int x,int y,int w,int h,int rgb){
        if(w>0&&h>0)g.fill(ox+x*s,oy+y*s,ox+(x+w)*s,oy+(y+h)*s,0xFF000000|rgb);
    }
    public static void draw(GuiGraphics g,int ox,int oy,boolean compact){
        int s=compact?1:2;
        for(Zone z:ZONES){
            int x=z.x(),y=z.y(),w=z.width(),h=z.height();
            int[] p=PALETTES[palette(z.part())];
            pixelRect(g,ox,oy,s,x,y,w,h,p[0]);
            pixelRect(g,ox,oy,s,x+1,y+1,w-2,h-2,p[1]);
            // Short connected clusters, not a continuous bevel or smooth gradient.
            pixelRect(g,ox,oy,s,x+1,y+1,Math.min(3,w-2),1,p[2]);
            pixelRect(g,ox,oy,s,x+w-2,y+h/2,1,Math.max(1,h/2-1),p[3]);
            if(z.part()==BodyPart.HEAD){
                pixelRect(g,ox,oy,s,x+2,y+4,1,1,p[0]);
                pixelRect(g,ox,oy,s,x+5,y+4,1,1,p[0]);
            }
            if(ClientBodyState.fractured(z.part())){
                int iy=y+h/2;
                if(ClientBodyState.stabilized(z.part())){
                    pixelRect(g,ox,oy,s,x,iy,w,2,0xd3c9ac);
                    pixelRect(g,ox,oy,s,x+w-1,iy,1,2,0xa5987e);
                }else{
                    pixelRect(g,ox,oy,s,x+1,iy-1,2,1,0xefe1c3);
                    pixelRect(g,ox,oy,s,x+2,iy,1,2,0xefe1c3);
                }
            }
            int bleeding=ClientBodyState.bleeding(z.part());
            if(bleeding>0){
                pixelRect(g,ox,oy,s,x+w-2,y+h-3,1,2,0xd14f46);
                if(bleeding>1)pixelRect(g,ox,oy,s,x+w-3,y+h-2,1,1,0xd14f46);
            }
        }
    }
}
