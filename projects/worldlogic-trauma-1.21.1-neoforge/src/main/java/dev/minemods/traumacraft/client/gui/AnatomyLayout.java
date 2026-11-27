package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.health.BodyPart;
import java.util.List;

/** Shared pixel geometry. No client runtime dependency; tested independently. */
public final class AnatomyLayout {
    public static final int WIDTH=48, HEIGHT=82, ATLAS_WIDTH=240, ATLAS_HEIGHT=84;
    public static final int COLUMN_WIDTH=64, INVENTORY_BODY_Y=37, INVENTORY_ICONS_Y=121;
    public record Zone(BodyPart part,int x,int y,int width,int height) {
        public boolean contains(int mx,int my,int ox,int oy){
            return mx>=ox+x && mx<ox+x+width && my>=oy+y && my<oy+y+height;
        }
    }
    public static final List<Zone> ZONES=List.of(
        new Zone(BodyPart.HEAD,15,0,18,18),
        new Zone(BodyPart.CHEST,12,19,24,16),
        new Zone(BodyPart.STOMACH,14,36,20,14),
        new Zone(BodyPart.RIGHT_ARM,1,20,10,31),
        new Zone(BodyPart.LEFT_ARM,37,20,10,31),
        new Zone(BodyPart.RIGHT_LEG,13,52,10,30),
        new Zone(BodyPart.LEFT_LEG,25,52,10,30)
    );
    private AnatomyLayout(){}
    public static BodyPart hit(int mx,int my,int ox,int oy){
        for(Zone z:ZONES)if(z.contains(mx,my,ox,oy))return z.part();
        return null;
    }
    public static int inventoryBodyX(int panelX){return panelX+(COLUMN_WIDTH-WIDTH)/2;}
    public static int hudWidth(int mask){return WIDTH+(mask==0?0:4+HudStatusLayout.ICON_SIZE);}
    public static int hudX(int screenWidth,int mask){
        return Math.max(4,screenWidth/2-91-6-hudWidth(mask));
    }
    public static int hudY(int screenWidth,int screenHeight,int mask){
        // Narrow screens cannot fit beside the hotbar: keep the display above it, not at the top.
        int clearance=screenWidth/2-91-6-hudWidth(mask)>=4?4:48;
        return Math.max(4,screenHeight-HEIGHT-clearance);
    }
    public static int palette(float fraction,boolean bleeding,boolean fractured){
        if(fraction<=0)return 4;
        if(fraction<.20f)return 3;
        if(fraction<.50f)return 2;
        return fraction<1f||bleeding||fractured?1:0;
    }
}
