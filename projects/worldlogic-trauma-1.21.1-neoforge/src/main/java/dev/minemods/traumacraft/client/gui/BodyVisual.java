package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.TraumaCraft;
import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.Locale;

/** Transparent anatomy sprites, drawn by independently selected body zones. */
public final class BodyVisual {
    public static final int WIDTH=AnatomyLayout.WIDTH, HEIGHT=AnatomyLayout.HEIGHT;
    private static final ResourceLocation BODY=ResourceLocation.fromNamespaceAndPath(
        TraumaCraft.MOD_ID,"textures/gui/trauma/body.png");
    private static final ResourceLocation OVERLAYS=ResourceLocation.fromNamespaceAndPath(
        TraumaCraft.MOD_ID,"textures/gui/trauma/overlays.png");
    private static final int[] MIDTONES={0x729954,0xc5a35b,0xce965b,0xc66553,0x29272C};
    private BodyVisual(){}
    public static BodyPart hit(int mx,int my,int ox,int oy){
        return AnatomyLayout.hit(mx,my,ox,oy);
    }
    public static Component name(BodyPart part){
        return Component.translatable("body.traumacraft."+part.name().toLowerCase(Locale.ROOT));
    }
    public static String conditionKey(BodyPart part){
        float f=ClientBodyState.fraction(part);
        if(f<=0)return "disabled";
        if(f<.20f)return "severe";
        if(f<.50f)return "injured";
        if(f<1f)return "sore";
        return "stable";
    }
    public static Component condition(BodyPart part){
        return Component.translatable("condition.traumacraft."+conditionKey(part));
    }
    private static int palette(BodyPart part){
        return AnatomyLayout.palette(ClientBodyState.fraction(part),ClientBodyState.bleeding(part)>0,ClientBodyState.fractured(part));
    }
    public static int hpColor(BodyPart part,boolean translucent){
        return (translucent?0xD0000000:0xFF000000)|MIDTONES[palette(part)];
    }
    private static void overlay(GuiGraphics g,int x,int y,int u,int v,int w,int h){
        g.blit(OVERLAYS,x,y,u,v,w,h,40,30);
    }
    public static void draw(GuiGraphics g,int ox,int oy,boolean compact){
        // HUD and anatomy panel use the exact same sprite pixels and hit rectangles.
        for(AnatomyLayout.Zone z:AnatomyLayout.ZONES){
            int x=ox+z.x(),y=oy+z.y(),w=z.width(),h=z.height(),p=palette(z.part());
            if(p==4)g.setColor(.32f,.32f,.35f,1f);
            try{g.blit(BODY,x,y,p*AnatomyLayout.WIDTH+z.x(),z.y(),w,h,AnatomyLayout.ATLAS_WIDTH,AnatomyLayout.ATLAS_HEIGHT);}
            finally{if(p==4)g.setColor(1f,1f,1f,1f);}
            boolean limb=z.part()==BodyPart.LEFT_ARM||z.part()==BodyPart.RIGHT_ARM
                ||z.part()==BodyPart.LEFT_LEG||z.part()==BodyPart.RIGHT_LEG;
            if(p==1&&limb&&ClientBodyState.bleeding(z.part())==0&&!ClientBodyState.fractured(z.part()))overlay(g,x+(w-7)/2,y+h/2,8,0,7,7);
            if(ClientBodyState.fractured(z.part())){
                if(ClientBodyState.stabilized(z.part())&&limb)
                    overlay(g,x+(w-9)/2,y+(h-16)/2,0,10,9,16);
                else overlay(g,x+(w-7)/2,y+(h-7)/2,16,0,7,7);
            }
            int bleeding=ClientBodyState.bleeding(z.part());
            if(bleeding>0)overlay(g,x+w-7,y+h-9,0,0,7,9);
            if(bleeding>1)overlay(g,x+(w-7)/2,y+2,0,0,7,9);
        }
    }
}
