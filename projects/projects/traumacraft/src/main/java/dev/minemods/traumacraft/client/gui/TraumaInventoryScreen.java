package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ApplyMedicalPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Vanilla-width inventory artwork plus one beveled, 166px tall medical extension. */
public final class TraumaInventoryScreen extends InventoryScreen {
    private static final int VANILLA_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    private static final int PANEL_DESIRED_WIDTH = 188;
    private static final int FIRST_ROW_Y = 21;
    private static final int ROW_HEIGHT = 19;
    private static final float SMALL_FONT = 0.78f;

    private int panelWidth = PANEL_DESIRED_WIDTH;
    private BodyPart hovered;

    public TraumaInventoryScreen(Player player) {
        super(player);
        imageWidth = VANILLA_WIDTH + PANEL_DESIRED_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override protected void init() {
        panelWidth = Math.max(148, Math.min(PANEL_DESIRED_WIDTH, width - VANILLA_WIDTH - 6));
        imageWidth = VANILLA_WIDTH + panelWidth;
        imageHeight = PANEL_HEIGHT;
        super.init();
    }

    private int px() { return leftPos + VANILLA_WIDTH; }
    private int rowX() { return px() + 64; }
    private int rowRight() { return px() + panelWidth - 7; }
    private int bx() { return px() + 9; }
    private int by() { return topPos + 26; }

    private void small(GuiGraphics gui, String value, int x, int y, int color) {
        gui.pose().pushPose();
        gui.pose().translate(x, y, 0);
        gui.pose().scale(SMALL_FONT, SMALL_FONT, 1f);
        gui.drawString(font, value, 0, 0, color, false);
        gui.pose().popPose();
    }
    private int smallWidth(String s) { return (int)Math.ceil(font.width(s) * SMALL_FONT); }

    @Override
    protected void renderBg(GuiGraphics gui, float partialTick, int mouseX, int mouseY) {
        // The vanilla PNG is 176px wide; do not attempt to blit the entire expanded screen.
        int saved = imageWidth;
        imageWidth = VANILLA_WIDTH;
        try { super.renderBg(gui, partialTick, mouseX, mouseY); }
        finally { imageWidth = saved; }

        int x=px(), y=topPos, r=x+panelWidth, b=y+PANEL_HEIGHT;
        // Minecraft-style slight pixel-rounded corners with familiar beveled highlights.
        gui.fill(x+2,y,r-2,b,0xFF373737);
        gui.fill(x,y+2,r,b-2,0xFF373737);
        gui.fill(x+2,y+1,r-2,b-1,0xFFE9E9E9);
        gui.fill(x+1,y+3,r-1,b-3,0xFFE9E9E9);
        gui.fill(x+3,y+3,r-3,b-3,0xFFC6C6C6);
        gui.fill(r-3,y+3,r-2,b-3,0xFF555555);
        gui.fill(x+3,b-3,r-2,b-2,0xFF555555);
        gui.fill(x+5,y+17,r-5,y+18,0xFF777777);
        gui.drawString(font, Component.translatable("screen.traumacraft.body"),
                x+7,y+5,0xFF3F3F3F,false);

        gui.fill(x+5,y+21,x+59,y+151,0xFF5D6567);
        gui.fill(x+7,y+23,x+57,y+149,0xFF262B2E);
        BodyVisual.draw(gui,bx(),by(),false);
        drawTotals(gui,x+10,y+127);

        hovered=BodyVisual.hit(mouseX,mouseY,bx(),by());
        for(int i=0;i<7;i++) {
            int ry=y+FIRST_ROW_Y+i*ROW_HEIGHT;
            if(mouseX>=rowX()-2&&mouseX<rowRight()+2&&mouseY>=ry&&mouseY<ry+ROW_HEIGHT)
                hovered=BodyPart.values()[i];
        }
        for(int i=0;i<7;i++)
            drawRow(gui,BodyPart.values()[i],rowX(),y+FIRST_ROW_Y+i*ROW_HEIGHT,
                    rowRight(),hovered==BodyPart.values()[i]);

        String footer=Component.translatable("screen.traumacraft.drag_to_treat").getString();
        small(gui,footer,x+8,b-11,0xFF555555);
    }

    private void drawTotals(GuiGraphics gui,int x,int y) {
        int blood=0,broken=0;
        for(BodyPart p:BodyPart.values()) {
            blood+=ClientBodyState.bleeding(p);
            if(ClientBodyState.fractured(p))broken++;
        }
        small(gui,"BLD "+blood,x,y,blood>0?0xFFD77979:0xFFA6B0B0);
        small(gui,"FX "+broken,x,y+9,broken>0?0xFFF0CE91:0xFFA6B0B0);
    }

    private void drawRow(GuiGraphics gui,BodyPart part,int x,int y,int right,boolean focus) {
        if(focus)gui.fill(x-3,y-1,right+2,y+ROW_HEIGHT-1,0x4A789C7C);
        String name=switch(part) {
            case HEAD->"HEAD"; case CHEST->"CHEST"; case STOMACH->"ABDOMEN";
            case LEFT_ARM->"L.ARM"; case RIGHT_ARM->"R.ARM";
            case LEFT_LEG->"L.LEG"; case RIGHT_LEG->"R.LEG";
        };
        String hp=Math.round(ClientBodyState.hp(part))+"/"+part.maximum;
        small(gui,name,x,y+1,0xFF333333);
        small(gui,hp,right-smallWidth(hp),y+1,0xFF333333);

        int bleeding=ClientBodyState.bleeding(part);
        boolean fracture=ClientBodyState.fractured(part);
        String status;
        int color;
        if(part==BodyPart.HEAD&&ClientBodyState.concussionTicks()>0) {
            status="CONCUSSION"; color=0xFF985A4F;
        } else if(bleeding>0&&fracture) {
            status="BLEED "+bleeding+" / FX";color=0xFF9A353A;
        } else if(fracture) {
            status="FRACTURE";color=0xFF956F2F;
        } else if(bleeding==2) {
            status="HEAVY BLEED";color=0xFF9A353A;
        } else if(bleeding==1) {
            status="BLEEDING";color=0xFFA2513C;
        } else if(ClientBodyState.hp(part)<=0) {
            status="BLACKED";color=0xFF933A42;
        } else if(ClientBodyState.hp(part)<part.maximum) {
            status="INJURED";color=0xFF84652C;
        } else {
            status="STABLE";color=0xFF747E7E;
        }
        small(gui,status,x,y+9,color);
        gui.fill(x,y+17,right,y+19,0xFF454A4B);
        int filled=Math.round((right-x)*ClientBodyState.fraction(part));
        if(filled>0)gui.fill(x,y+17,x+filled,y+19,BodyVisual.hpColor(part,false));
    }

    @Override public void render(GuiGraphics gui,int mx,int my,float delta) {
        super.render(gui,mx,my,delta);
        TreatmentRing.render(gui);
        if(hovered==null)return;
        List<Component> tooltip=new ArrayList<>();
        tooltip.add(Component.literal(hovered.label));
        tooltip.add(Component.literal(String.format(Locale.ROOT,"HP: %.0f / %d",
                ClientBodyState.hp(hovered),hovered.maximum)));
        if(ClientBodyState.hp(hovered)<=0)tooltip.add(Component.literal("Blacked out"));
        if(ClientBodyState.bleeding(hovered)>0)
            tooltip.add(Component.literal(ClientBodyState.bleeding(hovered)==2?"Heavy bleeding":"Light bleeding"));
        if(ClientBodyState.fractured(hovered))tooltip.add(Component.literal("Fracture"));
        if(hovered==BodyPart.HEAD&&ClientBodyState.concussionTicks()>0)
            tooltip.add(Component.literal("Concussion"));
        if(menu.getCarried().getItem() instanceof MedicalItem)
            tooltip.add(Component.translatable("screen.traumacraft.click_to_treat"));
        int widest=0;for(Component c:tooltip)widest=Math.max(widest,font.width(c));
        gui.renderComponentTooltip(font,tooltip,Math.max(7,Math.min(mx,width-widest-22)),
                Math.max(7,Math.min(my,height-tooltip.size()*11-16)));
    }

    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button==0||button==1){
            BodyPart part=findPart((int)mx,(int)my);
            if(part!=null){
                if(menu.getCarried().getItem() instanceof MedicalItem)
                    PacketDistributor.sendToServer(new ApplyMedicalPacket(part.ordinal()));
                return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    private BodyPart findPart(int mx,int my) {
        BodyPart part=BodyVisual.hit(mx,my,bx(),by());
        if(part!=null)return part;
        if(mx<rowX()-2||mx>=rowRight()+2)return null;
        for(int i=0;i<7;i++) {
            int y=topPos+FIRST_ROW_Y+i*ROW_HEIGHT;
            if(my>=y&&my<y+ROW_HEIGHT)return BodyPart.values()[i];
        }
        return null;
    }
}
