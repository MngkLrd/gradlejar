package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.DashboardSync;
import dev.minemods.worldlogic.api.WorldLogicDashboards.Snapshot;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
/** Native registered screens take precedence. Only generic provider-only dashboards use this view. */
final class DashboardScreen extends ClearScreen{
 private final Screen parent;private final ResourceLocation id;private int section,page;
 private Button previousSection,nextSection,previousPage,nextPage;
 DashboardScreen(Screen parent,ResourceLocation id,Component title){super(title);this.parent=parent;this.id=id;}
 ResourceLocation dashboardId(){return id;}
 private int usable(){return WorldLogicTabs.contentWidth(width);}
 private int panelW(){return Math.max(100,Math.min(520,usable()-16));}
 private int panelH(){return Math.max(65,Math.min(300,height-58));}
 private int left(){return (usable()-panelW())/2;}
 private int top(){return Math.max(25,(height-panelH())/2);}
 private Snapshot snapshot(){
  var mc=Minecraft.getInstance();var s=DashboardSync.client();
  if(s==null||mc.level==null||!s.dimension().equals(mc.level.dimension().location().toString()))return null;
  long age=mc.level.getGameTime()-s.tick();if(age<0||age>120)return null;
  return s.dashboards().stream().filter(d->d.id().equals(id)).findFirst().orElse(null);
 }
 @Override protected void init(){
  int x=left(),y=top(),r=x+panelW(),b=y+panelH();
  previousSection=addRenderableWidget(Button.builder(Component.literal("<"),v->{section=Math.max(0,section-1);page=0;}).bounds(x,y-22,26,20).build());
  nextSection=addRenderableWidget(Button.builder(Component.literal(">"),v->{section++;page=0;}).bounds(r-26,y-22,26,20).build());
  previousPage=addRenderableWidget(Button.builder(Component.literal("<"),v->page=Math.max(0,page-1)).bounds(x,b+3,26,20).build());
  nextPage=addRenderableWidget(Button.builder(Component.literal(">"),v->page++).bounds(r-26,b+3,26,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),v->onClose()).bounds(usable()/2-40,b+3,80,20).build());
  previousSection.setTooltip(Tooltip.create(Component.translatable("worldlogic.stats.previous_section")));
  nextSection.setTooltip(Tooltip.create(Component.translatable("worldlogic.stats.next_section")));
  TabRail.install(this,this::addRenderableWidget);
 }
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public boolean isPauseScreen(){return false;}
 private Component local(String key){
  String last=key.substring(key.lastIndexOf('.')+1);String fallback=last.replaceAll("([a-z])([A-Z])","$1 $2");
  return Component.translatableWithFallback(key.startsWith("worldlogic.")?key:"worldlogic.stats.field."+last,fallback);
 }
 private Component value(String raw){
  if(raw.matches("[A-Z_]+"))return Component.translatableWithFallback("worldlogic.stats.value."+raw.toLowerCase(Locale.ROOT),raw);
  return Component.literal(raw);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  VanillaPanels.backdrop(g,width,height);
  int x=left(),y=top(),w=panelW(),h=panelH(),r=x+w,b=y+h;
  VanillaPanels.panel(g,x,y,r,b);
  g.drawString(font,font.plainSubstrByWidth(title.getString(),w-16),x+8,y+7,VanillaPanels.TEXT,false);
  g.fill(x+7,y+20,r-7,y+21,VanillaPanels.LINE);
  var s=snapshot();Component hover=null;
  if(s==null||s.sections().isEmpty()){
   g.drawCenteredString(font,Component.translatable("worldlogic.stats.waiting"),usable()/2,y+32,VanillaPanels.MUTED);
   previousSection.active=nextSection.active=previousPage.active=nextPage.active=false;
  }else{
   section=Math.max(0,Math.min(section,s.sections().size()-1));var part=s.sections().get(section);
   String sectionLabel=local(part.title()).getString();
   g.drawCenteredString(font,font.plainSubstrByWidth(sectionLabel,Math.max(1,w-62)),usable()/2,y-15,0xffffffff);
   int columns=w>=420?2:1,cw=(w-20)/columns,rows=Math.max(1,(h-42)/24),capacity=rows*columns;
   int max=Math.max(0,(part.rows().size()-1)/capacity);page=Math.max(0,Math.min(max,page));
   previousSection.active=section>0;nextSection.active=section+1<s.sections().size();
   previousPage.active=page>0;nextPage.active=page<max;
   int start=page*capacity,end=Math.min(part.rows().size(),start+capacity);
   for(int k=start;k<end;k++){
    var row=part.rows().get(k);int pos=k-start,xx=x+10+(pos/rows)*cw,yy=y+27+(pos%rows)*24;
    String label=local(row.label()).getString();String v=value(row.value()).getString();
    int vw=Math.min(font.width(v),cw/2),vx=xx+cw-8-vw;
    g.drawString(font,font.plainSubstrByWidth(label,Math.max(1,vx-xx-4)),xx,yy,VanillaPanels.TEXT,false);
    g.drawString(font,font.plainSubstrByWidth(v,vw),vx,yy,VanillaPanels.MUTED,false);
    if(Double.isFinite(row.fraction()))VanillaPanels.bar(g,xx,yy+11,cw-8,row.fraction());
    g.fill(xx,yy+20,xx+cw-8,yy+21,VanillaPanels.LINE);
    if(mx>=xx&&mx<xx+cw-8&&my>=yy&&my<yy+21)hover=Component.literal(label+": "+v);
   }
   g.drawCenteredString(font,(section+1)+"/"+s.sections().size()+"  |  "+(page+1)+"/"+(max+1),usable()/2,b-12,VanillaPanels.MUTED);
  }
  super.render(g,mx,my,delta);
  if(hover!=null)g.renderTooltip(font,hover,mx,my);
 }
}
