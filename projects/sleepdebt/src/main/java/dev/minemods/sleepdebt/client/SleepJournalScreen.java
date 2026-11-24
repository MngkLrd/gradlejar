package dev.minemods.sleepdebt.client;

import com.mojang.math.Axis;
import dev.minemods.sleepdebt.gameplay.RestEnvironment;
import dev.minemods.sleepdebt.network.SleepReportPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

/** Five pages, pixel-cluster frame, single background pass and sub-GUI-pixel plots. */
public final class SleepJournalScreen extends Screen {
 private static final int TEXT=0xFF393939, MUTED=0xFF626262, RED=0xFFA65C67, GOLD=0xFFC18A50,
   VIOLET=0xFF927BB1, TEAL=0xFF54857F, GRID=0xFFB0B0B0;
 private static final ResourceLocation FRAME=ResourceLocation.fromNamespaceAndPath("sleepdebt","textures/gui/journal_frame.png");
 private SleepReportPayload report;
 private int refreshTicks;
 private final Screen previous;
 private int left,top,w,h,page;
 private final String[] pages={"overview","history","activity","recovery","caffeine"};
 public SleepJournalScreen(SleepReportPayload report,Screen previous){
  super(Component.translatable("journal.sleepdebt.title"));this.report=report;this.previous=previous;
 }
 public static void open(SleepReportPayload report){
  Minecraft mc=Minecraft.getInstance();
  if(mc.screen instanceof SleepJournalScreen old){old.report=report;}
  else if(mc.screen==null||mc.screen instanceof InventoryScreen)mc.setScreen(new SleepJournalScreen(report,mc.screen));
 }
 private Component tr(String key,Object... args){return Component.translatable("journal.sleepdebt."+key,args);}
 @Override protected void init(){
  w=Math.min(520,width-12);h=Math.min(310,height-12);left=(width-w)/2;top=(height-h)/2;
  int bw=(w-18)/pages.length;
  for(int i=0;i<pages.length;i++){final int n=i;
   addRenderableWidget(Button.builder(tr(pages[i]),b->{page=n;}).bounds(left+9+i*bw,top+24,bw-1,20).build());
  }
  addRenderableWidget(Button.builder(Component.literal("<"),b->page=(page+pages.length-1)%pages.length).bounds(left+9,top+h-25,22,18).build());
  addRenderableWidget(Button.builder(Component.literal(">"),b->page=(page+1)%pages.length).bounds(left+33,top+h-25,22,18).build());
  addRenderableWidget(Button.builder(tr("back"),b->onClose()).bounds(left+w-89,top+h-25,80,18).build());
 }
 @Override public void onClose(){if(minecraft!=null)minecraft.setScreen(previous);}
 @Override public boolean isPauseScreen(){return false;}
 @Override public void tick(){if(++refreshTicks>=100){refreshTicks=0;net.neoforged.neoforge.network.PacketDistributor.sendToServer(new dev.minemods.sleepdebt.network.JournalRequest());}}
 private void label(GuiGraphics g,String key,int x,int y,Object...args){g.drawString(font,tr(key,args),x,y,TEXT,false);}
 private void raw(GuiGraphics g,String s,int x,int y,int col){g.drawString(font,s,x,y,col,false);}
 private void panel(GuiGraphics g,int x,int y,int x2,int y2){
  g.fill(x,y,x2,y2,0xFFA4A4A4);
  g.fill(x+1,y+1,x2-1,y2-1,0xFFE3E0D9);
  g.fill(x,y,x2,y+1,0xFF777777);g.fill(x,y,x+1,y2,0xFF777777);
  g.fill(x,y2-1,x2,y2,0xFFFFFFFF);g.fill(x2-1,y,x2,y2,0xFFFFFFFF);
 }
 private void bar(GuiGraphics g,int x,int y,int width,int amount,int max,int col){
  g.fill(x,y,x+width,y+9,0xFF777777);g.fill(x+1,y+1,x+width-1,y+8,0xFFB7B7B7);
  int len=Math.max(0,Math.min(width-2,(int)((long)(width-2)*amount/Math.max(1,max))));
  if(len>0){g.fill(x+1,y+1,x+1+len,y+8,col);g.fill(x+1,y+1,x+1+len,y+2,col|0x00202020);}
 }
 private void line(GuiGraphics g,float x,float y,float x2,float y2,int color){
  float dx=x2-x,dy=y2-y;
  float length=(float)Math.sqrt(dx*dx+dy*dy);
  g.pose().pushPose();g.pose().translate(x,y,0);g.pose().mulPose(Axis.ZP.rotation((float)Math.atan2(dy,dx)));
  g.pose().scale(.5f,.5f,1f);g.fill(0,0,Math.max(1,(int)(length*2+.5f)),1,color);g.pose().popPose();
 }
 private int val(int[] a,int index){return index<a.length?a[index]:0;}
 private void overview(GuiGraphics g,int x,int y,int cw,int ch){
  int[] values={report.debt(),report.fatigue(),report.stress()};int[] colors={RED,GOLD,VIOLET};
  String[] names={"sleep","body","stress"};int split=cw/2;
  panel(g,x,y,x+split-4,y+ch);
  panel(g,x+split,y,x+cw,y+ch);
  int spacing=Math.max(24,(ch-20)/3);
  for(int i=0;i<3;i++){
   int yy=y+8+i*spacing;label(g,names[i],x+7,yy,values[i]/10);
   bar(g,x+7,yy+12,split-18,values[i],1000,colors[i]);
  }
  label(g,"penalties",x+split+7,y+7);
  String[] keys={"attack","mining","movement","damage","energy"};
  for(int i=0;i<4;i++)label(g,keys[i],x+split+7,y+22+i*12,val(report.penalties(),i));
  label(g,"energy",x+split+7,y+70,String.format(java.util.Locale.ROOT,"%.3f",val(report.penalties(),4)/1000.0));
 }
 private void plot(GuiGraphics g,int x,int y,int cw,int ch,int[] channels,int[] colors,int maximum){
  int gx=x+35,gy=y+8,gw=cw-44,gh=ch-28;
  panel(g,x,y,x+cw,y+ch);
  for(int i=0;i<=4;i++){
   int yy=gy+gh*i/4;
   line(g,gx,yy,gx+gw,yy,GRID);raw(g,Integer.toString(100*(4-i)/4)+"%",x+3,yy-4,MUTED);
  }
  for(int i=0;i<=6;i++){float xx=gx+gw*i/6f;line(g,xx,gy,xx,gy+gh,GRID);}
  int[] data=report.history();int n=data.length/5;
  if(n==0){label(g,"no_history",gx+3,gy+gh/2);return;}
  for(int c=0;c<channels.length;c++)for(int i=0;i<n;i++){
   float px=gx+gw*i/(float)Math.max(1,n-1);
   float py=gy+gh-gh*Math.max(0,Math.min(maximum,data[i*5+channels[c]]))/(float)maximum;
   if(i>0){float oldx=gx+gw*(i-1)/(float)Math.max(1,n-1);
    float oldy=gy+gh-gh*Math.max(0,Math.min(maximum,data[(i-1)*5+channels[c]]))/(float)maximum;
    line(g,oldx,oldy,px,py,colors[c]);}
   if(n<15)g.fill((int)px-1,(int)py-1,(int)px+1,(int)py+1,colors[c]);
  }
  label(g,"samples",gx,y+ch-12,n);
 }
 private void history(GuiGraphics g,int x,int y,int cw,int ch){
  raw(g,tr("sleep",report.debt()/10).getString(),x+3,y+2,RED);
  raw(g,tr("body",report.fatigue()/10).getString(),x+cw/3,y+2,GOLD);
  raw(g,tr("stress",report.stress()/10).getString(),x+2*cw/3,y+2,VIOLET);
  plot(g,x,y+14,cw,ch-14,new int[]{0,1,2},new int[]{RED,GOLD,VIOLET},1000);
 }
 private void activity(GuiGraphics g,int x,int y,int cw,int ch){
  panel(g,x,y,x+cw,y+ch);
  int[] a=report.activity();int maximum=1;for(int v:a)maximum=Math.max(maximum,v);
  String[] keys={"walk","swim","jump","mine","fight","sprint"};
  int gx=x+8,gy=y+6,gh=ch-34,group=(cw-16)/6;
  for(int i=0;i<6;i++){
   int v=val(a,i),bh=(int)((long)gh*v/maximum),xx=gx+i*group;
   int bw=Math.max(5,group-12);
   g.fill(xx+4,gy+gh-bh,xx+4+bw,gy+gh, i%2==0?GOLD:TEAL);
   raw(g,Integer.toString(v),xx+3,Math.max(gy,gy+gh-bh-10),TEXT);
   var category=tr(keys[i]);
   g.drawString(font,category,xx+(group-font.width(category))/2,gy+gh+4,TEXT,false);
  }
  line(g,gx,gy+gh,gx+cw-16,gy+gh,GRID);
  label(g,"lifetime",x+8,y+ch-12);
 }
 private void recovery(GuiGraphics g,int x,int y,int cw,int ch){
  int full=cw;boolean chart=cw>=300&&ch>=105;if(chart)cw=cw/2;
  panel(g,x,y,x+cw,y+ch);
  label(g,"quality",x+9,y+8,report.quality());
  label(g,"forecast",x+9,y+22,report.forecast());
  bar(g,x+9,y+35,cw-18,report.forecast(),150,TEAL);
  label(g,"recovered",x+9,y+49,report.recovered());
  label(g,"days",x+9,y+62,report.days());
  int r=report.forecastReasons(),col=0,row=0;
  String[] k={"rain","cold","heat","hostiles","outdoors","comfort","protection","stress_reason","caffeine_reason"};
  for(int i=0;i<k.length;i++)if((r&(1<<i))!=0){
   if(y+76+row*11<y+ch-4)label(g,k[i],x+9+col*(cw-18)/2,y+76+row*11);
   if(++col==2){col=0;row++;}
  }
  if(chart){
   int xx=x+cw+4,ww=full-cw-4;
   panel(g,xx,y,xx+ww,y+ch);label(g,"sleep_history",xx+5,y+7);
   int[] nights=report.sleepHistory();int n=nights.length/2;
   int gx=xx+6,gy=y+24,gh=ch-40,gw=ww-12;
   for(int i=0;i<=3;i++)line(g,gx,gy+gh*i/3f,gx+gw,gy+gh*i/3f,GRID);
   if(n==0)label(g,"no_history",gx,gy+8);
   else for(int i=0;i<n;i++){
    int colW=Math.max(1,gw/n),bh=gh*Math.min(150,Math.max(0,nights[2*i]))/150;
    g.fill(gx+i*colW,gy+gh-bh,gx+i*colW+Math.max(1,colW-2),gy+gh,TEAL);
   }
   label(g,"nights",gx,y+ch-12,n);
  }
 }
 private void caffeine(GuiGraphics g,int x,int y,int cw,int ch){
  label(g,"load",x+3,y+2,report.caffeine());label(g,"tolerance",x+cw/2,y+2,report.tolerance());
  int top=y+15;
  bar(g,x+3,top,cw/2-10,report.caffeine(),100,TEAL);bar(g,x+cw/2,top,cw/2-4,report.tolerance(),100,VIOLET);
  label(g,"duration",x+3,y+28,report.maskTicks()/20,report.crashTicks()/20);
  label(g,"potency",x+3,y+39,report.maskPower());
  if(ch>75)plot(g,x,y+52,cw,ch-52,new int[]{3,4},new int[]{TEAL,VIOLET},100);
 }
 // 16x16 atlas, 4px corners; edges tile at native size, never stretched.
 private void drawFrame(GuiGraphics g){
  for(int dx=4;dx<w-4;dx+=8){
   int n=Math.min(8,w-4-dx);
   g.blit(FRAME,left+dx,top,4,0,n,4,16,16);
   g.blit(FRAME,left+dx,top+h-4,4,12,n,4,16,16);
  }
  for(int dy=4;dy<h-4;dy+=8){
   int n=Math.min(8,h-4-dy);
   g.blit(FRAME,left,top+dy,0,4,4,n,16,16);
   g.blit(FRAME,left+w-4,top+dy,12,4,4,n,16,16);
  }
  g.blit(FRAME,left,top,0,0,4,4,16,16);
  g.blit(FRAME,left+w-4,top,12,0,4,4,16,16);
  g.blit(FRAME,left,top+h-4,0,12,4,4,16,16);
  g.blit(FRAME,left+w-4,top+h-4,12,12,4,4,16,16);
 }
 @Override public void render(GuiGraphics g,int mx,int my,float delta){
  // Never call Screen.render after custom contents: in 1.21.1 it can
  // run the background blur again, smearing everything except its widgets.
  // Explicitly paint a vanilla-style dim backdrop once, WITHOUT blur.
  g.fill(0,0,width,height,0xB0101010);
  g.flush();
  // Native grey inventory material, with a pixel-authored segmented frame.
  g.fill(left,top,left+w,top+h,0xFF171717);
  g.fill(left+1,top+1,left+w-1,top+h-1,0xFFC6C6C6);
  drawFrame(g);
  g.fill(left+8,top+19,left+w-8,top+20,0xFF8C8C8C);
  g.fill(left+8,top+20,left+w-8,top+21,0xFFECECEC);
  g.drawString(font,title,left+(w-font.width(title))/2,top+9,TEXT,false);
  int x=left+9,y=top+50,cw=w-18,ch=h-81;
  switch(page){case 0->overview(g,x,y,cw,ch);case 1->history(g,x,y,cw,ch);
   case 2->activity(g,x,y,cw,ch);case 3->recovery(g,x,y,cw,ch);case 4->caffeine(g,x,y,cw,ch);}
  raw(g,(page+1)+" / "+pages.length,left+62,top+h-20,MUTED);
  g.flush();
  // Render registered buttons only; input/narration still use Screen.children.
  for(var child:children())if(child instanceof Renderable widget)widget.render(g,mx,my,delta);
 }
}
