package dev.minemods.metabolism.client;
import dev.minemods.metabolism.MetabolicModel;
import dev.minemods.metabolism.MetabolismSync;
import dev.minemods.worldlogic.api.client.WorldLogicTabs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;
public final class MetabolismScreen extends Screen{
 private final Screen parent;private int tab,page;private final ArrayList<Hit> hits=new ArrayList<>();
 private record Hit(int x,int y,int w,int h,Component text,Component description,Component value){}
 private record Row(String key,String value,float fraction,boolean risk,Item icon){}
 private static final String[] TABS={"overview","nutrition","energy","digestion","recovery"};
 private static final Item[] ICONS={Items.COOKED_BEEF,Items.SALMON,Items.BREAD,Items.CARROT,Items.GOLDEN_CARROT,Items.APPLE,Items.COOKED_COD,Items.IRON_NUGGET,Items.BONE_MEAL,Items.BEETROOT};
 private static final int N=MetabolicModel.N;
 public MetabolismScreen(Screen parent){super(Component.translatable("metabolism.title"));this.parent=parent;}
 private Component tr(String key){return Component.translatable("metabolism."+key);}
 private static String pct(float x){return Math.round(x*100)+"%";}
 private static String number(float x){return String.format(Locale.ROOT,"%.0f",x);}
 private static String factor(float x){return String.format(Locale.ROOT,"%.2f×",x);}
 private int usable(){return WorldLogicTabs.contentWidth(width);}
 @Override protected void init(){
  int w=Math.max(15,(usable()-16)/TABS.length);
  for(int i=0;i<TABS.length;i++){final int k=i;addRenderableWidget(Button.builder(tr("tab."+TABS[i]),b->{tab=k;page=0;}).bounds(8+i*w,27,w-2,20).build());}
  addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(8,height-25,24,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->page++).bounds(34,height-25,24,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(Math.max(60,usable()-86),height-25,78,20).build());
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 private Row nutrient(int i,float[] v){return new Row(MetabolicModel.KEYS[i],pct(v[i]),v[i],false,ICONS[i]);}
 private List<Row> rows(float[] v,boolean anatomical){
  Row health=new Row(anatomical?"anatomical_hp":"vanilla_hp",number(v[N+10])+" / "+number(v[N+11])+" HP",v[N+11]>0?v[N+10]/v[N+11]:0,false,Items.GOLDEN_APPLE);
  Row diet=new Row("quality",pct(v[N+4]),v[N+4],false,Items.BOWL);
  Row energy=new Row("energy",pct(v[N+5]),v[N+5],false,Items.BLAZE_POWDER);
  Row toxins=new Row("toxins",pct(v[N+3]),v[N+3],true,Items.FERMENTED_SPIDER_EYE);
  Row fast=new Row("glycogen",number(v[N])+" / 1800 kcal",v[N]/1800,false,Items.SUGAR);
  Row fat=new Row("fat_reserve",number(v[N+1])+" / 30000 kcal",v[N+1]/30000,false,Items.HONEYCOMB);
  Row stomach=new Row("stomach",pct(v[N+2]/1800),v[N+2]/1800,false,Items.BOWL);
  Row absorption=new Row("absorption",pct(v[N+8]),v[N+8],false,Items.COOKED_BEEF);
  Row recovery=new Row("recovery",factor(v[N+6]),v[N+6],false,Items.GLISTERING_MELON_SLICE);
  Row resistance=new Row("resistance",factor(v[N+7]),v[N+7],false,Items.SHIELD);
  Row burn=new Row("burn",number(v[N+9])+" kcal/h",-1,false,Items.BLAZE_POWDER);
  Row fatigue=new Row("fatigue",v[N+12]<0?"—":number(v[N+12])+"%",v[N+12]<0?-1:v[N+12]/100,true,Items.IRON_PICKAXE);
  return switch(tab){
   case 1->{var a=new ArrayList<Row>();for(int i=0;i<N;i++)a.add(nutrient(i,v));yield a;}
   case 2->List.of(energy,fast,fat,burn,fatigue);
   case 3->List.of(stomach,absorption,toxins,diet);
   case 4->List.of(health,recovery,resistance,diet,energy,toxins,fatigue);
   default->List.of(health,diet,energy,toxins,fast,fat,stomach,absorption,recovery,resistance,burn,fatigue);
  };
 }
 private void row(GuiGraphics g,Row r,int x,int y,int w,int mx,int my){
  boolean hovered=mx>=x&&mx<x+w&&my>=y&&my<y+32;
  g.fill(x,y,x+w,y+32,hovered?0xff41494d:0xff2e3437);
  g.fill(x,y,x+w,y+1,0xff626b70);g.fill(x,y+31,x+w,y+32,0xff171b1d);
  g.renderItem(new ItemStack(r.icon),x+4,y+7);
  String label=font.plainSubstrByWidth(tr("stat."+r.key).getString(),Math.max(1,w-30));
  g.drawString(font,label,x+24,y+3,0xffeeeeee,false);
  int c=r.fraction<0?0xffdddddd:color(r.risk?1-r.fraction:r.fraction);
  g.drawString(font,font.plainSubstrByWidth(r.value,Math.max(1,w-30)),x+24,y+14,c,false);
  if(r.fraction>=0){int bar=w-30;g.fill(x+24,y+25,x+24+bar,y+29,0xff171b1d);int filled=Math.round(Math.max(0,Math.min(1,r.fraction))*bar);if(filled>0)g.fill(x+24,y+25,x+24+filled,y+28,c);}
  hits.add(new Hit(x,y,w,32,tr("stat."+r.key),tr("help."+r.key),Component.literal(r.value)));
 }
 private int color(float v){return v<.4?0xffe3796b:v<.8?0xffe2bd62:0xff9fca72;}
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  hits.clear();g.fill(0,0,width,height,0xff1a1e21);g.drawCenteredString(font,title,usable()/2,10,0xfff0eee7);
  int left=8,right=usable()-8,top=53,bottom=height-31;
  g.fill(left,top,right,bottom,0xff252a2d);g.renderOutline(left,top,right-left,bottom-top,0xff697276);
  var state=MetabolismSync.client;var player=Minecraft.getInstance().player;
  if(state==null||player==null||!state.player().equals(player.getUUID()))g.drawCenteredString(font,tr("waiting"),usable()/2,top+8,0xffdddddd);
  else if(!state.active())g.drawCenteredString(font,tr("inactive"),usable()/2,top+8,0xffdddddd);
  else {
   var data=rows(state.values(),state.anatomical());int cols=usable()>=430?3:usable()>=280?2:1;
   int lines=Math.max(1,(bottom-top-8)/35),capacity=cols*lines,maxPage=Math.max(0,(data.size()-1)/capacity);
   page=Math.min(page,maxPage);int cell=(right-left-8)/cols;
   for(int i=page*capacity;i<Math.min(data.size(),(page+1)*capacity);i++){
    int local=i-page*capacity;row(g,data.get(i),left+4+(local%cols)*cell,top+4+(local/cols)*35,cell-3,mx,my);
   }
   g.drawString(font,(page+1)+"/"+(maxPage+1),63,height-18,0xffd0d0d0,false);
  }
  super.render(g,mx,my,delta);
  for(var h:hits)if(mx>=h.x&&mx<h.x+h.w&&my>=h.y&&my<h.y+h.h){
   var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
   lines.add(h.text.getVisualOrderText());lines.addAll(font.split(h.description,Math.max(80,Math.min(230,usable()-20))));lines.add(h.value.getVisualOrderText());
   g.renderTooltip(font,lines,mx,my);break;
  }
 }
}
