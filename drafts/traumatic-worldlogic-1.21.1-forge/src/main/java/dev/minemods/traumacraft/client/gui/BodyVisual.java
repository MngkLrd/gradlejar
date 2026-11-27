package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

/** Exact native-pixel sprites from the approved HUD artwork.
 * Atlas: five 48x84 condition columns, seven non-overlapping anatomical zones.
 * Body axis x=22; both legs start two pixels below the abdomen.
 */
public final class BodyVisual {
    public static final int WIDTH=44, HEIGHT=82;
    private static final ResourceLocation BODY=ResourceLocation.fromNamespaceAndPath(
        TraumaCraft.MOD_ID,"textures/gui/trauma/body.png");
    private static final ResourceLocation OVERLAYS=ResourceLocation.fromNamespaceAndPath(
        TraumaCraft.MOD_ID,"textures/gui/trauma/overlays.png");
    public record Zone(BodyPart part,int x,int y,int width,int height) {
        boolean contains(int mx,int my,int ox,int oy) {
            return mx>=ox+x && mx<ox+x+width && my>=oy+y && my<oy+y+height;
        }
    }
    private static final Zone[] ZONES={
        new Zone(BodyPart.HEAD,11,0,22,20),
        new Zone(BodyPart.CHEST,12,21,20,13),
        new Zone(BodyPart.STOMACH,12,35,20,15),
        new Zone(BodyPart.RIGHT_ARM,1,21,10,29),
        new Zone(BodyPart.LEFT_ARM,33,21,10,29),
        new Zone(BodyPart.RIGHT_LEG,12,52,9,30),
        new Zone(BodyPart.LEFT_LEG,23,52,9,30)
    };
    private static final int[] MIDTONES={0x737a5c,0xb49a60,0xbc7b4e,0xb65748,0x665a63};
    private BodyVisual(){}
    public static BodyPart hit(int mx,int my,int ox,int oy){
        for(Zone z:ZONES)if(z.contains(mx,my,ox,oy))return z.part();
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
        return (translucent?0xD0000000:0xFF000000)|MIDTONES[palette(part)];
    }
    private static void overlay(GuiGraphics g,int x,int y,int u,int v,int w,int h){
        g.blit(OVERLAYS,x,y,u,v,w,h,40,30);
    }
    public static void draw(GuiGraphics g,int ox,int oy,boolean compact){
        // HUD and anatomy panel use the exact same sprite pixels and hit rectangles.
        for(Zone z:ZONES){
            int x=ox+z.x(),y=oy+z.y(),w=z.width(),h=z.height(),p=palette(z.part());
            g.blit(BODY,x,y,p*48+z.x(),z.y(),w,h,240,84);
            boolean limb=z.part()==BodyPart.LEFT_ARM||z.part()==BodyPart.RIGHT_ARM
                ||z.part()==BodyPart.LEFT_LEG||z.part()==BodyPart.RIGHT_LEG;
            if(p==1&&limb)overlay(g,x+(w-7)/2,y+h/2,8,0,7,7);
            if(ClientBodyState.fractured(z.part())){
                if(ClientBodyState.stabilized(z.part())&&limb)
                    overlay(g,x+(w-9)/2,y+(h-16)/2,0,10,9,16);
                else overlay(g,x+(w-7)/2,y+(h-7)/2,16,0,7,7);
            }
            if(ClientBodyState.bleeding(z.part())>0)overlay(g,x+w-7,y+h-9,0,0,7,9);
        }
    }
}
