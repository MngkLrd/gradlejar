package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.client.ClientBodyState;
import dev.minemods.traumacraft.client.ClientTreatmentState;
import dev.minemods.traumacraft.health.BodyPart;
import dev.minemods.traumacraft.health.OrganState;
import dev.minemods.traumacraft.item.MedicalItem;
import dev.minemods.traumacraft.network.ApplyMedicalPacket;
import dev.minemods.traumacraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

/** Same server inventory menu, separate full medical view. Hidden slots never receive input. */
public final class TraumaInventoryScreen extends InventoryScreen {
    private static final int TAB_WIDTH=74,TAB_HEIGHT=20;
    private boolean anatomyOpen,consumeRelease;
    private MedicalDashboardLayout layout;
    public TraumaInventoryScreen(Player player){super(player);imageWidth=176;imageHeight=166;}
    @Override protected void init(){
        imageWidth=176;imageHeight=166;super.init();layout=MedicalDashboardLayout.of(width,height);
        if(!MedicalDashboardLayout.available(width,height))anatomyOpen=false;
    }
    private boolean available(){return MedicalDashboardLayout.available(width,height)&&!getRecipeBookComponent().isVisible();}
    private int tabX(){return anatomyOpen?layout.backX():leftPos+176-TAB_WIDTH;}
    private int tabY(){return anatomyOpen?layout.backY():Math.max(1,topPos-TAB_HEIGHT);}
    private boolean onTab(double mx,double my){return available()&&mx>=tabX()&&mx<tabX()+TAB_WIDTH&&my>=tabY()&&my<tabY()+TAB_HEIGHT;}
    private Component t(String key){return Component.translatable("screen.traumacraft."+key);}
    private void label(GuiGraphics g,Component text,int x,int y,int w,int color){
        g.drawString(font,font.plainSubstrByWidth(text.getString(),Math.max(0,w)),x,y,color,false);
    }
    private void tab(GuiGraphics g,int mx,int my){
        if(!available())return;int x=tabX(),y=tabY();
        g.blitSprite(ResourceLocation.withDefaultNamespace(onTab(mx,my)?"widget/button_highlighted":"widget/button"),x,y,TAB_WIDTH,TAB_HEIGHT);
        g.drawCenteredString(font,font.plainSubstrByWidth(t(anatomyOpen?"inventory_back":"tab").getString(),TAB_WIDTH-8),x+TAB_WIDTH/2,y+6,0xFFFFFFFF);
    }
    private static int percentColor(float f){return f<=0?0xFF3B3538:f<.25f?0xFFB64B42:f<.60f?0xFFBD8A42:f<.99f?0xFFB2A04C:0xFF60864A;}
    private void bar(GuiGraphics g,int x,int y,int right,float f){
        g.fill(x,y,right,y+3,0xFF777777);
        int n=Math.round(Math.max(0,right-x-2)*Math.max(0,Math.min(1,f)));
        g.fill(x+1,y+1,x+1+n,y+2,percentColor(f));
    }
    private String percent(float v){return Math.round(v*100)+"%";}
    private Component organName(OrganState.Organ o){return Component.translatable("organ.traumacraft."+o.name().toLowerCase(Locale.ROOT));}
    private BodyPart bodyHit(int mx,int my){
        int s=layout.bodyScale();
        if(mx<layout.bodyX()||my<layout.bodyY()||mx>=layout.bodyX()+48*s||my>=layout.bodyY()+82*s)return null;
        return BodyVisual.hit((mx-layout.bodyX())/s,(my-layout.bodyY())/s,0,0);
    }
    private BodyPart regionHit(int mx,int my){
        var b=bodyHit(mx,my);if(b!=null)return b;
        if(mx<layout.regionX()-3||mx>=layout.regionRight()+2||my<layout.rowTop())return null;
        int i=(my-layout.rowTop())/layout.regionRow();return i<BodyPart.values().length?BodyPart.values()[i]:null;
    }
    private OrganState.Organ organHit(int mx,int my){
        if(mx<layout.organX()-3||mx>=layout.organRight()+2||my<layout.rowTop())return null;
        int i=(my-layout.rowTop())/layout.organRow();return i<OrganState.Organ.values().length?OrganState.Organ.values()[i]:null;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        if(!anatomyOpen){super.render(g,mx,my,delta);tab(g,mx,my);TreatmentRing.render(g);return;}
        var l=layout;int x=l.x(),y=l.y(),r=x+l.width(),b=y+l.height();
        renderBackground(g,mx,my,delta);
        // Existing vanilla-like gray beveled panel. No new texture or hidden slot drawing.
        g.fill(x+2,y,r-2,b,0xFF373737);g.fill(x,y+2,r,b-2,0xFF373737);
        g.fill(x+2,y+1,r-2,b-1,0xFFE9E9E9);g.fill(x+1,y+3,r-1,b-3,0xFFE9E9E9);
        g.fill(x+3,y+3,r-3,b-3,0xFFC6C6C6);g.fill(r-3,y+3,r-2,b-3,0xFF555555);
        g.fill(x+3,b-3,r-2,b-2,0xFF555555);
        label(g,t("dashboard"),x+8,y+9,l.width()-92,0xFF333333);tab(g,mx,my);
        var ill=ClientBodyState.illnessKind();
        boolean sick=ill!=dev.minemods.traumacraft.disease.DiseaseModel.Kind.NONE;
        Component illness=sick?Component.translatable("illness.traumacraft.status",Component.translatable("illness.traumacraft.kind."+ill.name().toLowerCase(Locale.ROOT)),Component.translatable("illness.traumacraft.stage."+ClientBodyState.illnessStage().name().toLowerCase(Locale.ROOT))):Component.translatable("illness.traumacraft.stage.healthy");
        label(g,illness,x+8,y+17,l.width()-16,sick?0xFF9A353A:0xFF3F503A);
        g.fill(x+6,y+26,r-6,y+27,0xFF8A8A8A);
        g.fill(l.organX()-8,y+29,l.organX()-7,l.footerY()-3,0xFF999999);
        g.pose().pushPose();g.pose().translate(l.bodyX(),l.bodyY(),0);g.pose().scale(l.bodyScale(),l.bodyScale(),1);
        BodyVisual.draw(g,0,0,false);g.pose().popPose();
        var selected=regionHit(mx,my);var selectedOrgan=organHit(mx,my);
        for(var part:BodyPart.values()){
            int yy=l.rowTop()+part.ordinal()*l.regionRow(),rx=l.regionRight();
            if(selected==part)g.fill(l.regionX()-3,yy-1,rx+2,yy+l.regionRow()-1,0x34678C64);
            String hp=(int)Math.ceil(ClientBodyState.hp(part))+"/"+part.maximum;
            int space=font.width(hp)+4;
            label(g,BodyVisual.name(part),l.regionX(),yy,rx-l.regionX()-space,0xFF333333);
            g.drawString(font,hp,rx-font.width(hp),yy,0xFF333333,false);
            bar(g,l.regionX(),yy+9,rx,ClientBodyState.fraction(part));
            if(l.regionRow()>=23)label(g,BodyVisual.condition(part),l.regionX(),yy+14,rx-l.regionX(),0xFF555555);
        }
        for(var organ:OrganState.Organ.values()){
            int yy=l.rowTop()+organ.ordinal()*l.organRow(),rx=l.organRight();
            float f=ClientBodyState.organ(organ);String num=percent(f);
            if(selectedOrgan==organ)g.fill(l.organX()-3,yy-1,rx+2,yy+l.organRow()-1,0x34678C64);
            label(g,organName(organ),l.organX(),yy,rx-l.organX()-font.width(num)-6,0xFF333333);
            g.drawString(font,num,rx-font.width(num),yy,percentColor(f),false);
            bar(g,l.organX(),yy+9,rx,f);
            if(ClientBodyState.internal(organ)>.001f)g.fill(l.organX()-3,yy+1,l.organX()-1,yy+8,0xFFAA3535);
        }
        int fy=l.footerY(),mid=l.organX();
        g.fill(x+6,fy-2,r-6,fy-1,0xFF888888);
        metric(g,"blood_volume",Math.round(ClientBodyState.bloodMl())+"/5000 ml",x+8,fy,mid-x-15,ClientBodyState.bloodMl()<3000);
        metric(g,"oxygen",percent(ClientBodyState.oxygen()),mid,fy,r-mid-8,ClientBodyState.oxygen()<.7f);
        metric(g,"perfusion",percent(ClientBodyState.perfusion()),x+8,fy+10,mid-x-15,ClientBodyState.perfusion()<.6f);
        metric(g,"internal_loss",String.format(Locale.ROOT,"%.1f ml/s",ClientBodyState.internalLoss()),mid,fy+10,r-mid-8,ClientBodyState.internalLoss()>0);
        metric(g,"shock",ClientBodyState.shock()+"/3 | "+Math.round(ClientBodyState.pain())+"/100",x+8,fy+20,mid-x-15,ClientBodyState.shock()>0);
        metric(g,"repair",percent(ClientBodyState.repair()),mid,fy+20,r-mid-8,ClientBodyState.repair()<.6f);
        if(ClientTreatmentState.active())bar(g,x+8,b-6,r-8,ClientTreatmentState.fraction());
        else label(g,ClientBodyState.unconscious()?t("unconscious"):t("dashboard_help"),x+8,b-10,l.width()-16,0xFF555555);
        List<Component> tip=new ArrayList<>();
        if(selectedOrgan!=null){
            tip.add(organName(selectedOrgan));tip.add(Component.translatable("screen.traumacraft.integrity",percent(ClientBodyState.organ(selectedOrgan))));
            tip.add(Component.translatable("screen.traumacraft.organ_loss",String.format(Locale.ROOT,"%.2f",ClientBodyState.internal(selectedOrgan))));
            tip.add(Component.translatable("screen.traumacraft.organ_region",BodyVisual.name(selectedOrgan.region)));
            if(selectedOrgan==OrganState.Organ.LEFT_KIDNEY||selectedOrgan==OrganState.Organ.RIGHT_KIDNEY)tip.add(Component.translatable("screen.traumacraft.function",Math.round(ClientBodyState.renal()*100)));
            if(selectedOrgan==OrganState.Organ.STOMACH||selectedOrgan==OrganState.Organ.INTESTINES)tip.add(Component.translatable("screen.traumacraft.function",Math.round(ClientBodyState.digestive()*100)));
        }else if(selected!=null){
            tip.add(BodyVisual.name(selected));tip.add(Component.literal(Math.round(ClientBodyState.hp(selected))+" / "+selected.maximum+" HP"));tip.add(BodyVisual.condition(selected));
            if(selected==BodyPart.STOMACH&&ClientBodyState.illnessKind()!=dev.minemods.traumacraft.disease.DiseaseModel.Kind.NONE){
                tip.add(Component.translatable("illness.traumacraft.status",Component.translatable("illness.traumacraft.kind."+ClientBodyState.illnessKind().name().toLowerCase(Locale.ROOT)),Component.translatable("illness.traumacraft.stage."+ClientBodyState.illnessStage().name().toLowerCase(Locale.ROOT))));
                tip.add(Component.translatable(ClientBodyState.illnessTreated()?"illness.traumacraft.support_active":"illness.traumacraft.support_hint"));
            }
            if(selected.isLeg()||selected==BodyPart.LEFT_ARM||selected==BodyPart.RIGHT_ARM)tip.add(Component.translatable("screen.traumacraft.function",Math.round(100*dev.minemods.traumacraft.health.PhysicalFunction.limb(ClientBodyState.fraction(selected),ClientBodyState.fractured(selected),ClientBodyState.stabilized(selected)))));
            if(selected==BodyPart.HEAD&&ClientBodyState.concussionTicks()>0)tip.add(Component.translatable("condition.traumacraft.concussion"));
            for(var kind:dev.minemods.traumacraft.health.WoundKind.values())if((ClientBodyState.wounds(selected)&kind.bit)!=0)tip.add(Component.translatable("wound.traumacraft."+kind.name().toLowerCase(Locale.ROOT)));
            if(ClientBodyState.bleeding(selected)>0)tip.add(Component.translatable("condition.traumacraft."+(ClientBodyState.bleeding(selected)==2?"heavy_bleed":"bleeding")));
            if(ClientBodyState.fractured(selected))tip.add(Component.translatable("condition.traumacraft."+(ClientBodyState.stabilized(selected)?"splinted":"fracture")));
            if(menu.getCarried().getItem() instanceof MedicalItem)tip.add(t("click_to_treat"));
        }
        if(tip.isEmpty()&&my>=y+17&&my<y+26&&mx>=x+8&&mx<r-8){
            tip.add(illness);
            if(sick)tip.add(Component.translatable(ClientBodyState.illnessTreated()?"illness.traumacraft.support_active":"illness.traumacraft.support_hint"));
        }
        if(tip.isEmpty()&&my>=fy&&my<fy+30){
            tip.add(Component.translatable("screen.traumacraft.function",Math.round(ClientBodyState.delivery()*100)));
            tip.add(Component.translatable("screen.traumacraft.physiology",Math.round(ClientBodyState.bloodMl()/50),Math.round(ClientBodyState.pain()),ClientBodyState.shock()));
            if(ClientBodyState.thermal().stressed())tip.add(Component.translatable("thermal.traumacraft."+ClientBodyState.thermal().name().toLowerCase(Locale.ROOT)));
        }
        if(!tip.isEmpty()&&menu.getCarried().isEmpty())g.renderComponentTooltip(font,tip,mx,my);
        var carried=menu.getCarried();
        if(!carried.isEmpty()){g.pose().pushPose();g.pose().translate(0,0,500);g.renderItem(carried,mx-8,my-8);g.renderItemDecorations(font,carried,mx-8,my-8);g.pose().popPose();}
    }
    private void metric(GuiGraphics g,String name,String value,int x,int y,int width,boolean warn){
        label(g,Component.translatable("screen.traumacraft."+name,value),x,y,width,warn?0xFF9A353A:0xFF3F503A);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button==0&&onTab(mx,my)){anatomyOpen=!anatomyOpen;consumeRelease=true;return true;}
        if(!anatomyOpen)return super.mouseClicked(mx,my,button);
        if(button==0||button==1){
            BodyPart target=regionHit((int)mx,(int)my);
            if(target!=null&&!ClientBodyState.unconscious()&&!ClientTreatmentState.active()&&menu.getCarried().getItem() instanceof MedicalItem)
                ModNetwork.request(new ApplyMedicalPacket(target.ordinal()));
        }
        return true;
    }
    @Override public boolean mouseReleased(double x,double y,int button){if(consumeRelease){consumeRelease=false;return true;}return anatomyOpen||super.mouseReleased(x,y,button);}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){return anatomyOpen||super.mouseDragged(x,y,button,dx,dy);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){return anatomyOpen||super.mouseScrolled(x,y,sx,sy);}
    @Override public boolean keyPressed(int key,int scan,int mods){
        if(!anatomyOpen)return super.keyPressed(key,scan,mods);
        if(key==256||minecraft.options.keyInventory.matches(key,scan)){anatomyOpen=false;return true;}
        return true;
    }
    @Override public boolean charTyped(char c,int mods){return anatomyOpen||super.charTyped(c,mods);}
}
