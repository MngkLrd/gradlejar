package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.DashboardSync;
import dev.minemods.worldlogic.api.WorldLogicDashboards.Snapshot;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import java.util.*;
/** Core-owned read-only dashboard. Metrics are authoritative server snapshots, not local calculations. */
final class DashboardScreen extends ClearScreen{
 private final Screen parent;private final ResourceLocation id;private int section,page;
 DashboardScreen(Screen parent,ResourceLocation id,Component title){super(title);this.parent=parent;this.id=id;}
 ResourceLocation dashboardId(){return id;}
 private int usable(){return WorldLogicTabs.contentWidth(width);}
 private Snapshot snapshot(){
  var mc=Minecraft.getInstance();var s=DashboardSync.client();
  if(s==null||mc.level==null||!s.dimension().equals(mc.level.dimension().location().toString()))return null;
  long age=mc.level.getGameTime()-s.tick();if(age<0||age>120)return null;
  return s.dashboards().stream().filter(d->d.id().equals(id)).findFirst().orElse(null);
 }
 @Override protected void init(){
  addRenderableWidget(Button.builder(Component.literal("<"),b->{section=Math.max(0,section-1);page=0;}).bounds(12,28,30,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->{var s=snapshot();if(s!=null){section=Math.min(Math.max(0,s.sections().size()-1),section+1);page=0;}}).bounds(46,28,30,20).build());
  addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(12,height-27,30,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->page++).bounds(46,height-27,30,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(usable()-90,height-27,78,20).build());
  WorldLogicTabs.legacyFor(id).ifPresent(t->addRenderableWidget(Button.builder(Component.translatable("worldlogic.stats.details"),b->{
   var screen=t.factory().apply(parent);if(screen!=null)Minecraft.getInstance().setScreen(screen);
  }).bounds(Math.max(82,usable()-180),28,Math.min(96,Math.max(1,usable()-90)),20).build()));
  TabRail.install(this,this::addRenderableWidget);
 }
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public boolean isPauseScreen(){return false;}
 private Component local(String key){
  String last=key.substring(key.lastIndexOf('.')+1);String fallback=last.replaceAll("([a-z])([A-Z])","$1 $2");
  return Component.translatableWithFallback(key.startsWith("worldlogic.")?key:"worldlogic.stats.field."+last,fallback);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0xff171c20);int u=usable();g.drawCenteredString(font,title,u/2,10,0xffeeeeee);
  g.fill(10,53,u-10,height-32,0xff242a2e);var s=snapshot();
  if(s==null||s.sections().isEmpty())g.drawCenteredString(font,Component.translatable("worldlogic.stats.waiting"),u/2,70,0xffcccccc);
  else{
   section=Math.max(0,Math.min(section,s.sections().size()-1));var part=s.sections().get(section);
   g.drawCenteredString(font,local(part.title()),u/2,56,0xffe6ebed);
   int columns=u>=570?3:u>=350?2:1, cw=Math.max(1,(u-32)/columns), rows=Math.max(1,(height-109)/32),capacity=rows*columns;
   int max=Math.max(0,(part.rows().size()-1)/capacity);page=Math.max(0,Math.min(max,page));
   int start=page*capacity,end=Math.min(part.rows().size(),start+capacity);
   for(int k=start;k<end;k++){
    var row=part.rows().get(k);int pos=k-start,x=16+(pos%columns)*cw,y=76+(pos/columns)*32;
    String label=local(row.label()).getString();String prefix=row.label().contains(".")&&!row.label().startsWith("worldlogic.")?row.label().substring(0,row.label().lastIndexOf('.')+1):"";
    g.drawString(font,font.plainSubstrByWidth(prefix+label,Math.max(1,cw-12)),x,y,0xffb7c3c6,false);
    g.drawString(font,font.plainSubstrByWidth(row.value(),Math.max(1,cw-12)),x,y+11,0xffeeeeee,false);
    if(Double.isFinite(row.fraction())){int w=Math.max(1,cw-14);g.fill(x,y+24,x+w,y+28,0xff41494c);g.fill(x,y+24,x+(int)(w*row.fraction()),y+28,0xff82bc8c);}
   }
   g.drawCenteredString(font,(section+1)+"/"+s.sections().size()+"  |  "+(page+1)+"/"+(max+1),u/2,height-20,0xffb7c3c6);
  }
  super.render(g,mx,my,delta);
 }
}
