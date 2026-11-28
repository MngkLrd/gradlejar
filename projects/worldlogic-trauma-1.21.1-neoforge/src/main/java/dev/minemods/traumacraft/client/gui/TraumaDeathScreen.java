package dev.minemods.traumacraft.client.gui;

import dev.minemods.traumacraft.health.*;
import dev.minemods.traumacraft.health.DeathReport.Cause;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Original vanilla death screen is retained for respawn, hardcore and leave-world controls. */
public final class TraumaDeathScreen extends Screen {
    private static final ResourceLocation BODY=ResourceLocation.fromNamespaceAndPath("traumacraft","textures/gui/trauma/body.png");
    private static final int TEXT=0xFF333333,FATAL=0xFFC52F31,BLEED=0xFFAA652D;
    private static final int[] PALETTE={0xFF729954,0xFFC5A35B,0xFFCE965B,0xFFC66553,0xFF29272C};
    private final DeathScreen original;
    private final DeathReport report;
    private DeathReportLayout l;
    public TraumaDeathScreen(DeathScreen original,DeathReport report){
        super(Component.translatable("deathScreen.title"));this.original=original;this.report=report;
    }
    @Override protected void init(){
        l=DeathReportLayout.of(width,height);
        addRenderableWidget(Button.builder(t("continue"),b->onClose()).bounds(l.buttonX(),l.buttonY(),116,18).build());
    }
    @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(original);}
    @Override public boolean isPauseScreen(){return false;}
    private static Component t(String key){return Component.translatable("death.traumacraft."+key);}
    private static Component cause(DeathReport.Cause c){return t("cause."+c.name().toLowerCase(Locale.ROOT));}
    private static Component origin(DeathReport.Origin o){return t("origin."+o.name().toLowerCase(Locale.ROOT));}
    private static Component organName(OrganState.Organ o){return Component.translatable("organ.traumacraft."+o.name().toLowerCase(Locale.ROOT));}
    private float hp(BodyPart p){return report.body().hp()[p.ordinal()];}
    private float integrity(OrganState.Organ o){return report.body().clinical().integrity()[o.ordinal()];}
    private int palette(BodyPart p){int i=p.ordinal();return AnatomyLayout.palette(hp(p)/p.maximum,report.body().bleeding()[i]>0,report.body().fracture()[i]);}
    private static boolean bloodCause(Cause c){return c==Cause.BLOOD_LOSS||c==Cause.INTERNAL_BLEEDING||c==Cause.EXTERNAL_BLEEDING;}
    private void label(GuiGraphics g,Component text,int x,int y,int w,int color){
        g.drawString(font,font.plainSubstrByWidth(text.getString(),Math.max(0,w)),x,y,color,false);
    }
    private void outline(GuiGraphics g,int x,int y,int w,int h,int color){
        g.fill(x,y,x+w,y+1,color);g.fill(x,y+h-1,x+w,y+h,color);
        g.fill(x,y+1,x+1,y+h-1,color);g.fill(x+w-1,y+1,x+w,y+h-1,color);
    }
    private void bar(GuiGraphics g,int x,int y,int right,float f,int color){
        if(right<=x+2)return;g.fill(x,y,right,y+3,0xFF777777);
        g.fill(x+1,y+1,x+1+Math.round((right-x-2)*Math.max(0,Math.min(1,f))),y+2,color);
    }
    private void drawBody(GuiGraphics g){
        g.pose().pushPose();g.pose().translate(l.bodyX(),l.bodyY(),0);g.pose().scale(l.scale(),l.scale(),1);
        for(var z:AnatomyLayout.ZONES){
            var p=z.part();int k=palette(p);
            g.blit(BODY,z.x(),z.y(),k*AnatomyLayout.WIDTH+z.x(),z.y(),z.width(),z.height(),AnatomyLayout.ATLAS_WIDTH,AnatomyLayout.ATLAS_HEIGHT);
            if(report.critical(p))outline(g,z.x(),z.y(),z.width(),z.height(),FATAL);
            else if(report.bleeding(p))outline(g,z.x(),z.y(),z.width(),z.height(),BLEED);
        }
        g.pose().popPose();
    }
    private BodyPart regionHit(int mx,int my){
        if(mx>=l.bodyX()&&mx<l.bodyX()+48*l.scale()&&my>=l.bodyY()&&my<l.bodyY()+82*l.scale())
            return AnatomyLayout.hit((mx-l.bodyX())/l.scale(),(my-l.bodyY())/l.scale(),0,0);
        if(mx<l.regionX()-3||mx>=l.regionRight()+2||my<l.rowTop())return null;
        int i=(my-l.rowTop())/l.regionRow();return i<BodyPart.values().length?BodyPart.values()[i]:null;
    }
    private OrganState.Organ organHit(int mx,int my){
        if(mx<l.organX()-3||mx>=l.organRight()+2||my<l.rowTop())return null;
        int i=(my-l.rowTop())/l.organRow();return i<OrganState.Organ.values().length?OrganState.Organ.values()[i]:null;
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        if(!DeathReportLayout.available(width,height)){onClose();return;}
        renderBackground(g,mx,my,delta);
        int x=l.x(),y=l.y(),r=x+l.width(),b=y+l.height();
        g.fill(x+2,y,r-2,b,0xFF343434);g.fill(x,y+2,r,b-2,0xFF343434);
        g.fill(x+2,y+1,r-2,b-1,0xFFE9E9E9);g.fill(x+3,y+3,r-3,b-3,0xFFC6C6C6);
        g.fill(r-3,y+3,r-2,b-3,0xFF555555);g.fill(x+3,b-3,r-2,b-2,0xFF555555);
        g.drawCenteredString(font,title,x+l.width()/2,y+5,0xFF9B2529);
        label(g,Component.translatable("death.traumacraft.reason",cause(report.cause())),x+7,y+16,l.width()-14,FATAL);
        label(g,Component.translatable("death.traumacraft.trigger",origin(report.origin())),x+7,y+27,l.width()-14,TEXT);
        g.fill(x+5,y+37,r-5,y+38,0xFF888888);
        g.fill(l.organX()-7,l.rowTop(),l.organX()-6,l.footerY()-2,0xFF999999);
        drawBody(g);
        var selected=regionHit(mx,my);var selectedOrgan=organHit(mx,my);
        for(var p:BodyPart.values()){
            int yy=l.rowTop()+p.ordinal()*l.regionRow(),rx=l.regionRight();
            boolean critical=report.critical(p),bleed=report.bleeding(p);
            if(selected==p)g.fill(l.regionX()-4,yy,rx+1,yy+l.regionRow(),0x30666666);
            if(critical||bleed)g.fill(l.regionX()-4,yy,l.regionX()-2,yy+Math.min(8,l.regionRow()),critical?FATAL:BLEED);
            String hp=(int)Math.ceil(hp(p))+"/"+p.maximum;
            label(g,BodyVisual.name(p),l.regionX(),yy,rx-l.regionX()-font.width(hp)-3,critical?FATAL:TEXT);
            g.drawString(font,hp,rx-font.width(hp),yy,critical?FATAL:TEXT,false);
            if(l.regionRow()>=13)bar(g,l.regionX(),yy+9,rx,hp(p)/p.maximum,PALETTE[palette(p)]);
        }
        for(var o:OrganState.Organ.values()){
            int yy=l.rowTop()+o.ordinal()*l.organRow(),rx=l.organRight();
            float f=integrity(o);boolean critical=report.critical(o),bleed=report.bleeding(o);
            int k=f<=0?4:f<.2f?3:f<.5f?2:f<.99f?1:0;
            if(selectedOrgan==o)g.fill(l.organX()-4,yy,rx+1,yy+l.organRow(),0x30666666);
            if(critical||bleed)g.fill(l.organX()-4,yy,l.organX()-2,yy+Math.min(8,l.organRow()),critical?FATAL:BLEED);
            String value=Math.round(f*100)+"%";
            label(g,organName(o),l.organX(),yy,rx-l.organX()-font.width(value)-4,critical?FATAL:TEXT);
            g.drawString(font,value,rx-font.width(value),yy,critical?FATAL:PALETTE[k],false);
            if(l.organRow()>=13)bar(g,l.organX(),yy+9,rx,f,PALETTE[k]);
        }
        int fy=l.footerY();
        g.fill(x+5,fy-2,r-5,fy-1,0xFF888888);
        label(g,t("legend_fatal"),x+7,fy,l.width()/3-10,FATAL);
        label(g,t("legend_bleeding"),x+l.width()/3,fy,l.width()/3-7,BLEED);
        label(g,t("legend_destroyed"),x+l.width()*2/3,fy,l.width()/3-7,TEXT);
        super.render(g,mx,my,delta);
        List<Component> tip=new ArrayList<>();
        if(selectedOrgan!=null){
            var o=selectedOrgan;tip.add(organName(o));
            tip.add(Component.translatable("screen.traumacraft.integrity",Math.round(integrity(o)*100)+"%"));
            if(report.critical(o))tip.add(t("fatal_injury"));
            if(integrity(o)<=0)tip.add(t("destroyed"));
            if(report.bleeding(o)){
                tip.add(Component.translatable("screen.traumacraft.organ_loss",String.format(Locale.ROOT,"%.2f",report.body().clinical().internalBleeding()[o.ordinal()])));
                if(bloodCause(report.cause()))tip.add(t("contributing_bleed"));
            }
            tip.add(Component.translatable("screen.traumacraft.organ_region",BodyVisual.name(o.region)));
        }else if(selected!=null){
            var p=selected;int i=p.ordinal();tip.add(BodyVisual.name(p));tip.add(Component.literal(Math.round(hp(p))+" / "+p.maximum+" HP"));
            if(report.critical(p))tip.add(t("fatal_injury"));
            if(hp(p)<=0)tip.add(t("destroyed"));
            if(report.bleeding(p)){tip.add(Component.translatable("condition.traumacraft."+(report.body().bleeding()[i]==2?"heavy_bleed":"bleeding")));if(bloodCause(report.cause()))tip.add(t("contributing_bleed"));}
            if(report.body().fracture()[i])tip.add(Component.translatable("condition.traumacraft."+(report.body().stabilized()[i]?"splinted":"fracture")));
            for(var k:WoundKind.values())if((report.body().wounds()[i]&k.bit)!=0)tip.add(Component.translatable("wound.traumacraft."+k.name().toLowerCase(Locale.ROOT)));
        }else if(mx>=x&&mx<r&&my>=y&&my<l.rowTop()){
            tip.add(cause(report.cause()));tip.add(origin(report.origin()));
            if(!report.sourceId().isEmpty())tip.add(Component.literal(report.sourceId()));
            tip.add(Component.translatable("screen.traumacraft.blood_volume",Math.round(report.body().bloodMl())+"/5000 ml"));
            tip.add(Component.translatable("screen.traumacraft.oxygen",Math.round(report.body().clinical().oxygen()*100)+"%"));
            tip.add(Component.translatable("screen.traumacraft.perfusion",Math.round(report.body().clinical().perfusion()*100)+"%"));
            tip.add(Component.translatable("screen.traumacraft.shock",report.body().shock()+"/3"));
        }
        if(!tip.isEmpty())g.renderComponentTooltip(font,tip,mx,my);
    }
}
