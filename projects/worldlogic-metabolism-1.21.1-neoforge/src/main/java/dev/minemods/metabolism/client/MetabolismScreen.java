package dev.minemods.metabolism.client;
import dev.minemods.metabolism.MetabolicModel;
import dev.minemods.metabolism.MetabolismSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import java.util.*;
public final class MetabolismScreen extends Screen{
 private final Screen parent;private int tab,page;private final Button[] tabs=new Button[6];private final ArrayList<Hit> hits=new ArrayList<>();
 private record Hit(int x,int y,int w,int h,Component text,Component description,Component value){}
 private record Row(String key,String value,float fraction,boolean risk,Item icon){}
 private static final String[] TABS={"overview","nutrition","energy","digestion","recovery","history"};
 private static final Item[] ICONS={Items.COOKED_BEEF,Items.SALMON,Items.BREAD,Items.CARROT,Items.GOLDEN_CARROT,Items.APPLE,Items.COOKED_COD,Items.IRON_NUGGET,Items.BONE_MEAL,Items.BEETROOT};
 private static final int N=MetabolicModel.N;
 public MetabolismScreen(Screen parent){super(Component.translatable("metabolism.title"));this.parent=parent;}
 private Component tr(String key){return Component.translatable("metabolism."+key);}
 private static String pct(float x){return Math.round(x*100)+"%";}
 private static String number(float x){return String.format(Locale.ROOT,"%.0f",x);}
 private static String hours(float x){return String.format(Locale.ROOT,"%.1f h",x);}
 private static String factor(float x){return String.format(Locale.ROOT,"%.2f×",x);}
 private int usable(){return ClientSetup.contentWidth(width);}
 @Override protected void init(){
  int w=Math.max(15,(usable()-16)/TABS.length);
  for(int i=0;i<TABS.length;i++){final int k=i;var button=Button.builder(Component.literal(font.plainSubstrByWidth(tr("tab."+TABS[i]).getString(),Math.max(1,w-10))),b->{tab=k;page=0;}).bounds(8+i*w,27,w-2,20).build();button.setTooltip(Tooltip.create(tr("tab."+TABS[i])));tabs[i]=addRenderableWidget(button);}
  addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(8,height-25,24,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->page++).bounds(34,height-25,24,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(Math.max(60,usable()-86),height-25,78,20).build());
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 private Row nutrient(int i,float[] v){float d=v[N+18+i];String trend=d>.025?" ▲":d<-.025?" ▼":" =";return new Row(MetabolicModel.KEYS[i],pct(v[i])+trend,v[i],false,ICONS[i]);}
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
  Row deficits=new Row("deficits",number(v[N+13])+" / "+N,v[N+13]/N,true,Items.BEETROOT);
  Row critical=new Row("critical",number(v[N+14])+" / "+N,v[N+14]/N,true,Items.REDSTONE);
  Row fastTime=new Row("fast_time",hours(v[N+15]),-1,false,Items.CLOCK);
  Row totalTime=new Row("total_time",hours(v[N+16]),-1,false,Items.CLOCK);
  Row digestionTime=new Row("digestion_time",hours(v[N+17]),-1,false,Items.CLOCK);
  int k=2*N+18;
  Row stamina=new Row("stamina",v[N+12]<0?"?":number(100-v[N+12])+" / 100",v[N+12]<0?-1:1-v[N+12]/100,false,Items.IRON_PICKAXE);
  Row intake=new Row("intake_boost",String.format(Locale.ROOT,"+%.1f",v[k+34]),-1,false,Items.HONEY_BOTTLE);
  Row digestBoost=new Row("digest_boost",String.format(Locale.ROOT,"+%.2f/s",v[k+35]),-1,false,Items.BREAD);
  Row overeating=new Row("overeating",pct(v[k+33]),v[k+33],true,Items.BOWL);
  Row water=new Row("water",v[k]<0?"?":number(v[k])+"%",v[k]<0?-1:v[k]/100,false,Items.WATER_BUCKET);
  Row salts=new Row("salts",v[k+1]<0?"?":number(v[k+1]),v[k+1]<0?-1:1-Math.abs(v[k+1]-50)/50,false,Items.SUGAR);
  Row temp=new Row("body_temp",v[k+2]<0?"?":String.format(Locale.ROOT,"%.1f °C",v[k+2]),-1,false,Items.BLAZE_POWDER);
  Row thermal=new Row("thermal_cost",factor(v[k+3]),-1,false,Items.CAMPFIRE);
  Row waterFactor=new Row("water_factor",factor(v[k+4]),v[k+4],false,Items.POTION);
  Row hpRate=new Row("hp_rate",v[k+32]<0?"?":String.format(Locale.ROOT,"%.2f HP/min",v[k+32]),-1,false,Items.GOLDEN_APPLE);
  Row tissue=new Row("tissue_factor",factor(v[k+5]),v[k+5],false,Items.GLISTERING_MELON_SLICE);
  Row bone=new Row("bone_factor",factor(v[k+6]),v[k+6],false,Items.BONE);
  Row blood=new Row("blood_factor",factor(v[k+7]),v[k+7],false,Items.REDSTONE);
  return switch(tab){
   case 1->{var a=new ArrayList<Row>();for(int i=0;i<N;i++)a.add(nutrient(i,v));yield a;}
   case 2->List.of(stamina,intake,digestBoost,energy,fast,fat,burn,fastTime,totalTime,fatigue,temp,thermal);
   case 3->List.of(stomach,overeating,absorption,digestionTime,toxins,diet,deficits,critical,water,salts,waterFactor);
   case 4->List.of(health,hpRate,recovery,tissue,bone,blood,resistance,diet,deficits,critical,energy,toxins,fatigue);
   case 5->{var out=new ArrayList<Row>();out.add(new Row("today_intake",number(v[k+8])+" kcal",-1,false,Items.BREAD));out.add(new Row("today_burn",number(v[k+9])+" kcal",-1,false,Items.BLAZE_POWDER));for(int i=0;i<7;i++){int off=k+11+i*3;if(v[off]<0)continue;out.add(new Row("history_day_"+(i+1),number(v[off])+" / "+number(v[off+1])+" kcal",v[off+2],false,Items.BOOK));}yield out;}
   default->List.of(health,diet,stamina,energy,toxins,deficits,critical,fast,fat,stomach,absorption,recovery,resistance,burn,fatigue);
  };
 }
 private void row(GuiGraphics g,Row r,int x,int y,int w,int mx,int my){
  boolean hovered=mx>=x&&mx<x+w&&my>=y&&my<y+32;
  g.fill(x,y,x+w,y+32,0xff777777);
  g.fill(x+1,y+1,x+w,y+32,0xffeeeeee);
  g.fill(x+1,y+1,x+w-1,y+31,hovered?0xffe3e3e3:0xffb7b7b7);
  g.renderItem(new ItemStack(r.icon),x+4,y+7);
  String label=font.plainSubstrByWidth(tr("stat."+r.key).getString(),Math.max(1,w-30));
  g.drawString(font,label,x+24,y+3,0xff383838,false);
  int c=r.fraction<0?0xff555555:textColor(r.risk?1-r.fraction:r.fraction);
  g.drawString(font,font.plainSubstrByWidth(r.value,Math.max(1,w-30)),x+24,y+14,c,false);
  if(r.fraction>=0){int bar=w-30;g.fill(x+24,y+25,x+24+bar,y+29,0xff777777);g.fill(x+25,y+26,x+24+bar-1,y+28,0xff383838);int filled=Math.round(Math.max(0,Math.min(1,r.fraction))*Math.max(0,bar-2));if(filled>0)g.fill(x+25,y+26,x+25+filled,y+28,barColor(r.risk?1-r.fraction:r.fraction));}
  hits.add(new Hit(x,y,w,32,tr("stat."+r.key),tr("help."+r.key),Component.literal(r.value)));
 }
 private void line(GuiGraphics g,int x,int y,int x2,int y2,int c){
  int dx=Math.abs(x2-x),dy=-Math.abs(y2-y),sx=x<x2?1:-1,sy=y<y2?1:-1,err=dx+dy;
  while(true){g.fill(x,y,x+1,y+1,c);if(x==x2&&y==y2)break;int e=err*2;if(e>=dy){err+=dy;x+=sx;}if(e<=dx){err+=dx;y+=sy;}}
 }
 private void radar(GuiGraphics g,float[] v,int x,int y,int w,int h){
  int cx=x+w/2,cy=y+h/2+5,r=Math.min(w/2-24,h/2-25);
  g.fill(x,y,x+w,y+h,0xff777777);g.fill(x+1,y+1,x+w,y+h,0xffeeeeee);g.fill(x+1,y+1,x+w-1,y+h-1,0xffb7b7b7);
  center(g,tr("profile"),cx,y+7,0xff383838);
  float[] values={v[0],v[1],v[2],v[3],(v[4]+v[5]+v[6])/3,(v[7]+v[8]+v[9])/3};
  String[] keys={"protein","fat","carb","fiber","vitamins","minerals"};
  Item[] icons={Items.COOKED_BEEF,Items.SALMON,Items.BREAD,Items.CARROT,Items.APPLE,Items.IRON_NUGGET};
  int[] px=new int[6],py=new int[6];
  for(int ring=1;ring<=4;ring++)for(int i=0;i<6;i++){
   double a=i*Math.PI/3-Math.PI/2,b=(i+1)*Math.PI/3-Math.PI/2;
   double d=r*ring/4.0;line(g,cx+(int)Math.round(Math.cos(a)*d),cy+(int)Math.round(Math.sin(a)*d),cx+(int)Math.round(Math.cos(b)*d),cy+(int)Math.round(Math.sin(b)*d),0xff888888);
  }
  for(int i=0;i<6;i++){
   double a=i*Math.PI/3-Math.PI/2;double xx=Math.cos(a),yy=Math.sin(a);
   line(g,cx,cy,cx+(int)Math.round(xx*r),cy+(int)Math.round(yy*r),0xff888888);
   px[i]=cx+(int)Math.round(xx*r*Math.min(1,values[i]));py[i]=cy+(int)Math.round(yy*r*Math.min(1,values[i]));
   int ix=cx+(int)Math.round(xx*(r+11))-8,iy=cy+(int)Math.round(yy*(r+11))-8;
   g.renderItem(new ItemStack(icons[i]),ix,iy);
   hits.add(new Hit(ix,iy,16,16,tr("stat."+keys[i]),tr("help."+keys[i]),Component.literal(pct(values[i]))));
  }
  for(int yy=cy-r;yy<=cy+r;yy++){
   var intersections=new ArrayList<Integer>();
   for(int i=0;i<6;i++){int j=(i+1)%6;if((py[i]<=yy&&py[j]>yy)||(py[j]<=yy&&py[i]>yy))intersections.add(px[i]+(int)Math.round((double)(yy-py[i])*(px[j]-px[i])/(py[j]-py[i])));}
   Collections.sort(intersections);for(int i=0;i+1<intersections.size();i+=2)g.fill(intersections.get(i),yy,intersections.get(i+1)+1,yy+1,0x60729954);
  }
  for(int i=0;i<6;i++){int j=(i+1)%6;line(g,px[i],py[i],px[j],py[j],0xff38663a);g.fill(px[i]-1,py[i]-1,px[i]+2,py[i]+2,0xff38663a);}
 }
 private int textColor(float v){return v<.4?0xff9c3030:v<.8?0xff80501e:0xff315c33;}
 private int barColor(float v){return v<.4?0xffc66553:v<.8?0xffc5a35b:0xff729954;}
 private void center(GuiGraphics g,Component text,int x,int y,int c){g.drawString(font,text,x-font.width(text)/2,y,c,false);}
 private void frame(GuiGraphics g,int x,int y,int w,int h){
  if(w<6||h<6)return;
  g.fill(x+2,y,x+w-2,y+h,0xff343434);g.fill(x,y+2,x+w,y+h-2,0xff343434);
  g.fill(x+2,y+1,x+w-2,y+h-1,0xffeeeeee);g.fill(x+1,y+3,x+w-1,y+h-3,0xffeeeeee);
  g.fill(x+3,y+3,x+w-3,y+h-3,0xffc6c6c6);
  g.fill(x+w-3,y+3,x+w-2,y+h-3,0xff555555);g.fill(x+3,y+h-3,x+w-3,y+h-2,0xff555555);
 }
 @Override public void renderBackground(GuiGraphics g,int mx,int my,float delta){g.fill(0,0,width,height,0x99000000);}
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  hits.clear();for(int i=0;i<tabs.length;i++)if(tabs[i]!=null)tabs[i].active=i!=tab;
  super.render(g,mx,my,delta);
  frame(g,8,4,usable()-16,20);center(g,title,usable()/2,10,0xff383838);
  int left=8,right=usable()-8,top=53,bottom=height-31;
  frame(g,left,top,right-left,bottom-top);
  var state=MetabolismSync.client;var player=Minecraft.getInstance().player;
  if(state==null||player==null||!state.player().equals(player.getUUID()))center(g,tr("waiting"),usable()/2,top+8,0xff444444);
  else if(!state.active())center(g,tr("inactive"),usable()/2,top+8,0xff444444);
  else {
   var values=state.values();var data=rows(values,state.anatomical());int cols=usable()>=430?3:usable()>=280?2:1;
   int start=left+4;
   if((tab==0||tab==1)&&usable()>=480&&bottom-top>=144){int chart=Math.min(190,(right-left)/3);radar(g,values,start,top+4,chart-4,bottom-top-8);start+=chart;cols=2;}
   int lines=Math.max(1,(bottom-top-8)/35),capacity=cols*lines,maxPage=Math.max(0,(data.size()-1)/capacity);
   page=Math.min(page,maxPage);int cell=(right-start-4)/cols;
   for(int i=page*capacity;i<Math.min(data.size(),(page+1)*capacity);i++){
    int local=i-page*capacity;row(g,data.get(i),start+(local%cols)*cell,top+4+(local/cols)*35,cell-3,mx,my);
   }
   g.drawString(font,(page+1)+"/"+(maxPage+1),63,height-18,0xffd0d0d0,false);
  }
  for(var h:hits)if(mx>=h.x&&mx<h.x+h.w&&my>=h.y&&my<h.y+h.h){
   var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
   lines.add(h.text.getVisualOrderText());lines.addAll(font.split(h.description,Math.max(80,Math.min(230,usable()-20))));lines.add(h.value.getVisualOrderText());
   if(h.value.getString().endsWith("▲")||h.value.getString().endsWith("▼")||h.value.getString().endsWith("="))lines.addAll(font.split(tr("trend_help"),Math.max(80,Math.min(230,usable()-20))));
   g.renderTooltip(font,lines,mx,my);break;
  }
 }
}
