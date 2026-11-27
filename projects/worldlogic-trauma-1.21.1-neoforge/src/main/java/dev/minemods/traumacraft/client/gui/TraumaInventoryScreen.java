package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.client.ClientTreatmentState;
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
    private static final int PANEL_DESIRED_WIDTH = 300;
    private static final int FIRST_ROW_Y = 27;
    private int panelHeight=224, rowHeight=24, bodyScale=1, columnWidth=64;
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
        var layout=MedicalPanelLayout.of(width,height);
        panelWidth=layout.width();panelHeight=layout.height();rowHeight=layout.rowHeight();
        bodyScale=layout.bodyScale();columnWidth=layout.columnWidth();
        if(width < 330 || height < 212) anatomyOpen=false;
        imageWidth = anatomyOpen ? VANILLA_WIDTH + panelWidth : VANILLA_WIDTH;
        imageHeight = PANEL_HEIGHT;
        super.init();
    }

    private boolean tabAvailable() { return width>=330 && height>=212 && !getRecipeBookComponent().isVisible(); }
    private int tabX() { return leftPos + VANILLA_WIDTH - TAB_WIDTH; }
    private int tabY() { return Math.max(1,topPos - TAB_HEIGHT); }
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
    private int py(){return (height-panelHeight)/2;}
    private int rowX() { return px() + columnWidth+8; }
    private int rowRight() { return px() + panelWidth - 7; }
    private int bx() { return px()+(columnWidth-BodyVisual.WIDTH*bodyScale)/2; }
    private int by() { return py()+27; }
    private BodyPart bodyHit(int mx,int my){
        if(mx<bx()||my<by()||mx>=bx()+BodyVisual.WIDTH*bodyScale||my>=by()+BodyVisual.HEIGHT*bodyScale)return null;
        return BodyVisual.hit((mx-bx())/bodyScale,(my-by())/bodyScale,0,0);
    }

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

        int x=px(), y=py(), r=x+panelWidth, b=y+panelHeight;
        // Minecraft-style slight pixel-rounded corners with familiar beveled highlights.
        gui.fill(x+2,y,r-2,b,0xFF373737);
        gui.fill(x,y+2,r,b-2,0xFF373737);
        gui.fill(x+2,y+1,r-2,b-1,0xFFE9E9E9);
        gui.fill(x+1,y+3,r-1,b-3,0xFFE9E9E9);
        gui.fill(x+3,y+3,r-3,b-3,0xFFC6C6C6);
        gui.fill(r-3,y+3,r-2,b-3,0xFF555555);
        gui.fill(x+3,b-3,r-2,b-2,0xFF555555);
        gui.fill(x+5,y+17,r-5,y+18,0xFF777777);
        int count=Integer.bitCount(TraumaHud.activeMask());
        int cols=Math.min(3,count);
        boolean iconsBelow=cols>0&&by()+BodyVisual.HEIGHT*bodyScale+3+HudStatusLayout.ICON_SIZE+((count+cols-1)/cols-1)*HudStatusLayout.STEP<=b-49;
        int headerIcons=count==0||iconsBelow?0:HudStatusLayout.ICON_SIZE+(count-1)*HudStatusLayout.STEP+4;
        String heading=Component.translatable("screen.traumacraft.body").getString();
        small(gui,font.plainSubstrByWidth(heading,panelWidth-14-headerIcons),x+7,y+5,0xFF3F3F3F);
        if(headerIcons>0)TraumaHud.drawActive(gui,r-5-headerIcons+4,y+1,count);

        gui.pose().pushPose();
        gui.pose().translate(bx(),by(),0);
        gui.pose().scale(bodyScale,bodyScale,1f);
        BodyVisual.draw(gui,0,0,false);
        gui.pose().popPose();
        if(iconsBelow){
            int iconWidth=HudStatusLayout.ICON_SIZE+(cols-1)*HudStatusLayout.STEP;
            TraumaHud.drawActive(gui,x+(columnWidth-iconWidth)/2,by()+BodyVisual.HEIGHT*bodyScale+3,cols);
        }
        hovered=bodyHit(mouseX,mouseY);
        for(int i=0;i<7;i++) {
            int ry=y+FIRST_ROW_Y+i*rowHeight;
            if(mouseX>=rowX()-2&&mouseX<rowRight()+2&&mouseY>=ry&&mouseY<ry+rowHeight)
                hovered=BodyPart.values()[i];
        }
        for(int i=0;i<7;i++)
            drawRow(gui,BodyPart.values()[i],rowX(),y+FIRST_ROW_Y+i*rowHeight,
                    rowRight(),hovered==BodyPart.values()[i]);

        int footerY=b-46;
        gui.fill(x+6,footerY-3,r-6,footerY-2,0xFF888888);
        int max=0,dead=0,weak=0,strong=0;float hp=0;
        for(BodyPart part:BodyPart.values()){
            max+=part.maximum;hp+=ClientBodyState.hp(part);
            if(ClientBodyState.hp(part)<=0)dead++;
            if(ClientBodyState.bleeding(part)==1)weak++;
            if(ClientBodyState.bleeding(part)==2)strong++;
        }
        String summary=Component.translatable("screen.traumacraft.summary",(int)Math.ceil(hp),max,dead).getString();
        small(gui,font.plainSubstrByWidth(summary,panelWidth-16),x+8,footerY,0xFF333333);
        String physiology=Component.translatable("screen.traumacraft.physiology",Math.round(ClientBodyState.bloodMl()/50),Math.round(ClientBodyState.pain()),ClientBodyState.shock()).getString();
        small(gui,font.plainSubstrByWidth(physiology,panelWidth-16),x+8,footerY+10,ClientBodyState.shock()>0?0xFF9A353A:0xFF3D663D);
        String bleeding=Component.translatable("screen.traumacraft.blood",weak,strong).getString();
        small(gui,font.plainSubstrByWidth(bleeding,panelWidth-16),x+8,footerY+20,weak+strong>0?0xFF9A353A:0xFF3D663D);
        var thermal=ClientBodyState.thermal();
        String last=thermal.stressed()?Component.translatable("thermal.traumacraft."+thermal.name().toLowerCase(java.util.Locale.ROOT)).getString():
            Component.translatable("screen.traumacraft.drag_to_treat").getString();
        if(ClientTreatmentState.active()){
            last=Component.translatable("screen.traumacraft.treatment",String.format(java.util.Locale.ROOT,"%.1f",ClientTreatmentState.remaining()/20f)).getString();
            gui.fill(x+7,b-6,r-7,b-3,0xFF777777);
            gui.fill(x+7,b-6,x+7+Math.round((panelWidth-14)*ClientTreatmentState.fraction()),b-3,0xFF69964E);
            small(gui,font.plainSubstrByWidth(last,panelWidth-16),x+8,b-16,0xFF333333);
        }else small(gui,font.plainSubstrByWidth(last,panelWidth-16),x+8,b-10,0xFF555555);
    }

    private void drawRow(GuiGraphics gui,BodyPart part,int x,int y,int right,boolean focus) {
        if(focus)gui.fill(x-4,y-1,right+2,y+rowHeight-1,0x34789C7C);
        int color=BodyVisual.hpColor(part,false);
        gui.fill(x-4,y+1,x-2,y+rowHeight-3,color);
        String name=BodyVisual.name(part).getString();
        String hp=(int)Math.ceil(ClientBodyState.hp(part))+"/"+part.maximum;
        int numWidth=font.width(hp);
        small(gui,font.plainSubstrByWidth(name,Math.max(10,right-x-numWidth-4)),x,y,0xFF333333);
        small(gui,hp,right-numWidth,y,0xFF333333);

        int bleeding=ClientBodyState.bleeding(part);
        String key=BodyVisual.conditionKey(part);
        if(ClientBodyState.hp(part)>0){
            if(part==BodyPart.HEAD&&ClientBodyState.concussionTicks()>0)key="concussion";
            if(ClientBodyState.fractured(part))key=ClientBodyState.stabilized(part)?"splinted":"fracture";
            if(bleeding>0)key=bleeding==2?"heavy_bleed":"bleeding";
        }
        String status=Component.translatable("condition.traumacraft."+key).getString();
        if(part==BodyPart.STOMACH&&key.equals("stable")&&ClientBodyState.illnessKind()!=dev.minemods.traumacraft.disease.DiseaseModel.Kind.NONE)status=Component.translatable("illness.traumacraft.short").getString();
        if(ClientBodyState.hp(part)<=0&&bleeding>0)status+=" / "+Component.translatable("condition.traumacraft."+(bleeding==2?"heavy_bleed":"bleeding")).getString();
        if(ClientBodyState.fractured(part)&&bleeding>0)status+=" / "+Component.translatable("condition.traumacraft."+(ClientBodyState.stabilized(part)?"splinted":"fracture")).getString();
        int barWidth=rowHeight>=23?right-x:Math.min(36,Math.max(16,(right-x)/3));
        int barX=right-barWidth,barY=y+(rowHeight>=23?19:12);
        gui.fill(barX,barY,right,barY+3,0xFF777777);
        gui.fill(barX+1,barY+1,barX+1+Math.round(Math.max(0,barWidth-2)*ClientBodyState.fraction(part)),barY+2,color);
        int statusWidth=rowHeight>=23?right-x:barX-x-3;
        // A two-line compact row keeps both condition and HP visible.
        gui.pose().pushPose();
        gui.pose().translate(x,y+9,0);
        float scale=rowHeight<20?.8f:1f;
        gui.pose().scale(scale,scale,1f);
        gui.drawString(font,font.plainSubstrByWidth(status,Math.max(5,(int)(statusWidth/scale))),0,0,bleeding>0?0xFF9A353A:key.equals("stable")?0xFF3D663D:0xFF555555,false);
        gui.pose().popPose();
    }

    @Override public void render(GuiGraphics gui,int mx,int my,float delta) {
        super.render(gui,mx,my,delta);
        renderTab(gui,mx,my);
        if(!anatomyOpen){TreatmentRing.render(gui);return;}
        if(hovered==null)return;
        List<Component> tooltip=new ArrayList<>();
        tooltip.add(BodyVisual.name(hovered));
        tooltip.add(Component.literal((int)Math.ceil(ClientBodyState.hp(hovered))+" / "+hovered.maximum+" HP"));
        tooltip.add(BodyVisual.condition(hovered));
        if(hovered==BodyPart.STOMACH&&ClientBodyState.illnessKind()!=dev.minemods.traumacraft.disease.DiseaseModel.Kind.NONE){
            tooltip.add(Component.translatable("illness.traumacraft.status",Component.translatable("illness.traumacraft.kind."+ClientBodyState.illnessKind().name().toLowerCase(java.util.Locale.ROOT)),Component.translatable("illness.traumacraft.stage."+ClientBodyState.illnessStage().name().toLowerCase(java.util.Locale.ROOT))));
            tooltip.add(Component.translatable(ClientBodyState.illnessTreated()?"illness.traumacraft.support_active":"illness.traumacraft.support_hint"));
        }
        for(var kind:dev.minemods.traumacraft.health.WoundKind.values())
            if((ClientBodyState.wounds(hovered)&kind.bit)!=0)tooltip.add(Component.translatable("wound.traumacraft."+kind.name().toLowerCase(java.util.Locale.ROOT)));
        if(hovered==BodyPart.LEFT_ARM||hovered==BodyPart.RIGHT_ARM||hovered.isLeg()){
            float function=dev.minemods.traumacraft.health.PhysicalFunction.limb(ClientBodyState.fraction(hovered),ClientBodyState.fractured(hovered),ClientBodyState.stabilized(hovered));
            tooltip.add(Component.translatable("screen.traumacraft.function",Math.round(100*function)));
        }
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
                if(!ClientTreatmentState.active() && menu.getCarried().getItem() instanceof MedicalItem)
                    ModNetwork.request(new ApplyMedicalPacket(part.ordinal()));
                return true;
            }
            // Empty areas of the medical panel must never drop the carried stack.
            if(mx>=px() && mx<px()+panelWidth && my>=py() && my<py()+panelHeight)return true;
        }
        boolean handled=super.mouseClicked(mx,my,button);
        if(anatomyOpen && getRecipeBookComponent().isVisible()){
            anatomyOpen=false; hovered=null; rebuildWidgets();
        }
        return handled;
    }
    private BodyPart findPart(int mx,int my) {
        BodyPart part=bodyHit(mx,my);
        if(part!=null)return part;
        if(mx<rowX()-2||mx>=rowRight()+2)return null;
        for(int i=0;i<7;i++) {
            int y=py()+FIRST_ROW_Y+i*rowHeight;
            if(my>=y&&my<y+rowHeight)return BodyPart.values()[i];
        }
        return null;
    }
}
