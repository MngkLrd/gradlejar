package dev.minemods.worldlogic.client;
import dev.minemods.worldlogic.AnalyticsSync;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
public final class AnalyticsScreen extends Screen{
 private final Screen parent;private int tab,page;
 private static final String[] TABS={"air","water","soil","impact","history"};
 public AnalyticsScreen(Screen parent){super(Component.translatable("worldlogic.analytics.title"));this.parent=parent;}
 private Component tr(String s){return Component.translatable("worldlogic.analytics."+s);}
 @Override protected void init(){
  int left=12,w=(width-24)/5;
  for(int i=0;i<5;i++){final int k=i;addRenderableWidget(Button.builder(tr(TABS[i]),b->{tab=k;page=0;}).bounds(left+i*w,32,w-2,20).build());}
  addRenderableWidget(Button.builder(Component.literal("<"),b->page=Math.max(0,page-1)).bounds(12,height-28,26,20).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->page=Math.min(50,page+1)).bounds(42,height-28,26,20).build());
  addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(width-94,height-28,82,20).build());
 }
 @Override public boolean isPauseScreen(){return false;}
 @Override public void onClose(){Minecraft.getInstance().setScreen(parent);}
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  g.fill(0,0,width,height,0xff171c20);
  g.drawCenteredString(font,title,width/2,10,0xffeeeeee);
  var state=AnalyticsSync.client();var level=Minecraft.getInstance().level;
  g.fill(12,58,width-12,height-34,0xff222a2e);
  if(state==null||level==null||!state.dimension().equals(level.dimension().location().toString())){
   g.drawCenteredString(font,tr("waiting"),width/2,76,0xffcccccc);
  }else{
   g.drawString(font,Component.literal("X "+state.x()+"  Y "+state.y()+"  Z "+state.z()+"  |  t "+state.tick()),18,64,0xffacb9bf,false);
   if(tab<4&&state.tabs().size()>tab){
    var lines=new ArrayList<net.minecraft.util.FormattedCharSequence>();
    for(String row:state.tabs().get(tab)){
     var kv=row.split("\\|",2);
     var c=Component.translatable("worldlogic.analytics."+kv[0]);
     if(kv.length>1&&!kv[1].isEmpty())c.append(": ").append(kv[1]);
     lines.addAll(font.split(c,Math.max(30,width-40)));
    }
    if(lines.isEmpty())lines.addAll(font.split(tr("no_data"),width-40));
    int visible=Math.max(1,(height-116)/12);int maxPage=Math.max(0,(lines.size()-1)/visible);page=Math.min(page,maxPage);
    int end=Math.min(lines.size(),(page+1)*visible),y=82;
    for(int i=page*visible;i<end;i++,y+=12)g.drawString(font,lines.get(i),18,y,0xffe6ebed,false);
    g.drawString(font,(page+1)+"/"+(maxPage+1),76,height-22,0xffbac4c9,false);
   }else if(tab==4){history(g,state);}
  }
  super.render(g,mx,my,delta);
 }
 private int color(float v){int r=(int)(55+170*v),green=(int)(180-120*v);return 0xff000000|(r<<16)|(green<<8)|65;}
 private void history(GuiGraphics g,AnalyticsSync.State s){
  int x=18,y=82,cell=Math.max(9,Math.min(23,(height-146)/4));
  g.drawString(font,tr(page%2==0?"map_air":"map_soil"),x,y,0xffe6ebed,false);
  y+=13;for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
   float v=-1;for(var c:s.nearby())if(c.dx()==dx&&c.dz()==dz)v=page%2==0?c.air():c.soil();
   int xx=x+(dx+1)*cell,yy=y+(dz+1)*cell;g.fill(xx,yy,xx+cell-1,yy+cell-1,v<0?0xff434b50:color(v));
   if(dx==0&&dz==0)g.renderOutline(xx,yy,cell-1,cell-1,0xffffffff);
  }
  int gx=x+cell*3+12,gw=width-gx-22,gh=Math.max(10,height-151),gy=95;
  if(gw>16){
   g.drawString(font,tr("history_chart"),gx,82,0xffe6ebed,false);
   g.fill(gx,gy,gx+gw,gy+gh,0xff13191d);
   var h=s.history();int n=h.size();
   if(n==0)g.drawString(font,tr("no_data"),gx+3,gy+3,0xffaaaaaa,false);
   else for(int i=0;i<n;i++){int xx=gx+i*gw/n,xx2=gx+(i+1)*gw/n;var point=h.get(i);int ah=(int)(point.air()*gh),sh=(int)(point.soil()*gh);
    if(ah>0)g.fill(xx,gy+gh-ah,Math.max(xx+1,xx2-1),gy+gh,0xffdc9861);
    if(sh>0)g.fill(xx,gy+gh-sh,Math.max(xx+1,xx2-1),gy+gh-sh+1,0xff82bc8c);
   }
  }
  g.drawString(font,tr("legend"),18,height-46,0xffb7c3c6,false);
 }
}
