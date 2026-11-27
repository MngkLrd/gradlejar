package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ApplyMedicalPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import dev.minemods.traumacraft.network.ModNetwork;
import java.util.ArrayList;
import java.util.List;

/** Vanilla inventory with an opt-in medical tab; ordinary inventory retains its original width. */
public final class TraumaInventoryScreen extends InventoryScreen {
    private static final int VANILLA_WIDTH = 176;
    private static final int PANEL_HEIGHT = 166;
    private static final int PANEL_DESIRED_WIDTH = 188;
    private static final int FIRST_ROW_Y = 21;
    private static final int ROW_HEIGHT = 19;
    private static final float SMALL_FONT = 1f;

    private int panelWidth = PANEL_DESIRED_WIDTH;
    private BodyPart hovered;
    private boolean anatomyOpen;
    private static final int TAB_WIDTH=64, TAB_HEIGHT=20;

    public TraumaInventoryScreen(Player player) {
        super(player);
        imageWidth = VANILLA_WIDTH;
        imageHeight = PANEL_HEIGHT;
    }

    @Override protected void init() {
        panelWidth = Math.max(148, Math.min(PANEL_DESIRED_WIDTH, width - VANILLA_WIDTH - 6));
        if(width < 330 || height < 202) anatomyOpen=false;
        imageWidth = anatomyOpen ? VANILLA_WIDTH + panelWidth : VANILLA_WIDTH;
        imageHeight = PANEL_HEIGHT;
        super.init();
    }

    private boolean tabAvailable() { return width>=330 && height>=202 && !getRecipeBookComponent().isVisible(); }
    private int tabX() { return leftPos + VANILLA_WIDTH - TAB_WIDTH; }
    private int tabY() { return topPos - TAB_HEIGHT; }
    private boolean onTab(double mx,double my) {
        return tabAvailable() && mx>=tabX() && mx<tabX()+TAB_WIDTH && my>=tabY() && my<tabY()+TAB_HEIGHT;
    }
    private void toggleAnatomy() {
        anatomyOpen=!anatomyOpen;
        hovered=null;
        rebuildWidgets();
    }
    private void renderTab(GuiGraphics gui,int mx,int my) {
        if(!tabAvailable())return;
        int x=tabX(),y=tabY(),r=x+TAB_WIDTH,b=y+TAB_HEIGHT;
        // Same vanilla widget skin as the temperature tab; no custom block-shaped background.
        gui.blitSprite(ResourceLocation.withDefaultNamespace(anatomyOpen||onTab(mx,my)?"widget/button_highlighted":"widget/button"),x,y,TAB_WIDTH,TAB_HEIGHT);
        gui.fill(x+6,y+6,x+9,y+14,0xFFB04443);
        gui.fill(x+4,y+8,x+11,y+11,0xFFB04443);
        gui.drawString(font,font.plainSubstrByWidth(Component.translatable("screen.traumacraft.tab").getString(),TAB_WIDTH-17),x+14,y+6,0xFFFFFFFF,true);
    }
    private int px() { return leftPos + VANILLA_WIDTH; }
    private int rowX() { return px() + 70; }
    private int rowRight() { return px() + panelWidth - 7; }
    private int bx() { return AnatomyLayout.inventoryBodyX(px()); }
    private int by() { return topPos + AnatomyLayout.INVENTORY_BODY_Y; }

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
        hovered=null;
        if(!anatomyOpen || !tabAvailable()){super.renderBg(gui,partialTick,mouseX,mouseY);return;}
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

        // Draw the body directly on the vanilla panel, without a dark portrait well.
        BodyVisual.draw(gui,bx(),by(),false);
        var thermal=ClientBodyState.thermal();
        if(thermal.stressed()){
            String label=Component.translatable("thermal.traumacraft."+thermal.name().toLowerCase(java.util.Locale.ROOT)).getString();
            String fitted=font.plainSubstrByWidth(label,AnatomyLayout.COLUMN_WIDTH-4);
            small(gui,fitted,x+(AnatomyLayout.COLUMN_WIDTH-font.width(fitted))/2,y+24,thermal.cold()?0xFF365F86:0xFFA04830);
        }
        int count=Integer.bitCount(TraumaHud.activeMask());
        int cols=Math.min(count>4?3:2,count);
        if(cols>0) {
            int iconsWidth=HudStatusLayout.ICON_SIZE+(cols-1)*HudStatusLayout.STEP;
            TraumaHud.drawActive(gui,x+(AnatomyLayout.COLUMN_WIDTH-iconsWidth)/2,
                y+AnatomyLayout.INVENTORY_ICONS_Y,cols);
        }

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
        small(gui,font.plainSubstrByWidth(footer,panelWidth-16),x+8,b-11,0xFF555555);
    }

    private void drawRow(GuiGraphics gui,BodyPart part,int x,int y,int right,boolean focus) {
        if(focus)gui.fill(x-4,y-1,right+2,y+ROW_HEIGHT-1,0x34789C7C);
        gui.fill(x-4,y+2,x-2,y+15,BodyVisual.hpColor(part,false));
        String name=BodyVisual.name(part).getString();
        small(gui,font.plainSubstrByWidth(name,Math.max(10,right-x)),x,y,0xFF333333);

        int bleeding=ClientBodyState.bleeding(part);
        boolean fracture=ClientBodyState.fractured(part);
        String key=BodyVisual.conditionKey(part);
        if(part==BodyPart.HEAD&&ClientBodyState.concussionTicks()>0)key="concussion";
        if(fracture)key=ClientBodyState.stabilized(part)?"splinted":"fracture";
        if(bleeding>0)key=bleeding==2?"heavy_bleed":"bleeding";
        int color=bleeding>0?0xFF9A353A:fracture?0xFF956F2F:
            key.equals("stable")?0xFF3D663D:key.equals("disabled")||key.equals("severe")?0xFF8C3233:0xFF84652C;
        String status=Component.translatable("condition.traumacraft."+key).getString();
        small(gui,font.plainSubstrByWidth(status,Math.max(10,right-x)),x,y+9,color);
    }

    @Override public void render(GuiGraphics gui,int mx,int my,float delta) {
        super.render(gui,mx,my,delta);
        renderTab(gui,mx,my);
        TreatmentRing.render(gui);
        if(!anatomyOpen)return;
        var thermal=ClientBodyState.thermal();
        if(thermal.stressed()&&mx>=px()&&mx<px()+AnatomyLayout.COLUMN_WIDTH&&my>=topPos+20&&my<by()){
            gui.renderComponentTooltip(font,List.of(Component.translatable("thermal.traumacraft."+thermal.name().toLowerCase(java.util.Locale.ROOT)),Component.translatable("thermal.traumacraft.recovery",Math.round(100*thermal.recoveryFactor))),mx,my);
            return;
        }
        if(hovered==null)return;
        List<Component> tooltip=new ArrayList<>();
        tooltip.add(BodyVisual.name(hovered));
        tooltip.add(BodyVisual.condition(hovered));
        if(ClientBodyState.bleeding(hovered)>0)
            tooltip.add(Component.translatable("condition.traumacraft."+
                (ClientBodyState.bleeding(hovered)==2?"heavy_bleed":"bleeding")));
        if(ClientBodyState.fractured(hovered))
            tooltip.add(Component.translatable("condition.traumacraft."+
                (ClientBodyState.stabilized(hovered)?"splinted":"fracture")));
        if(hovered==BodyPart.HEAD&&ClientBodyState.concussionTicks()>0)
            tooltip.add(Component.translatable("condition.traumacraft.concussion"));
        if(menu.getCarried().getItem() instanceof MedicalItem)
            tooltip.add(Component.translatable("screen.traumacraft.click_to_treat"));
        int widest=0;for(Component c:tooltip)widest=Math.max(widest,font.width(c));
        gui.renderComponentTooltip(font,tooltip,Math.max(7,Math.min(mx,width-widest-22)),
                Math.max(7,Math.min(my,height-tooltip.size()*11-16)));
    }

    @Override public boolean mouseClicked(double mx,double my,int button) {
        if(button==0 && onTab(mx,my)) { toggleAnatomy(); return true; }
        if(anatomyOpen && tabAvailable() && (button==0||button==1)){
            BodyPart part=findPart((int)mx,(int)my);
            if(part!=null){
                if(menu.getCarried().getItem() instanceof MedicalItem)
                    ModNetwork.request(new ApplyMedicalPacket(part.ordinal()));
                return true;
            }
            // Empty areas of the medical panel must never drop the carried stack.
            if(mx>=px() && mx<px()+panelWidth && my>=topPos && my<topPos+PANEL_HEIGHT)return true;
        }
        boolean handled=super.mouseClicked(mx,my,button);
        if(anatomyOpen && getRecipeBookComponent().isVisible()){
            anatomyOpen=false; hovered=null; rebuildWidgets();
        }
        return handled;
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
