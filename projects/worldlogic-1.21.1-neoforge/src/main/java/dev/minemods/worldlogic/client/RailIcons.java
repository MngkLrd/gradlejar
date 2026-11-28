package dev.minemods.worldlogic.client;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
/** Pixel-aligned 16px vanilla-style semantic glyphs. Vanilla item icons remain for inventory/book/food. */
final class RailIcons{
 private RailIcons(){}
 private static final String[] THERMOMETER={
 "......ddd.......",".....dwwsd......",".....dw.sd......",".....dw.sd......",
 ".....drrsd......",".....dr.sd......",".....dr.sd......",".....drrsd......",
 ".....dr.sd......","....ddrrsdd.....","...drrrrrsdd....","...drrrrrrsd....",
 "...drrrrrrsd....","....drrrsdd.....",".....dddd.......","................"};
 private static final String[] MEDICAL={
 "................","....dddddddd....","...dwwwwwwwsd...","..dwwwwwwwwssd..",
 "..dwwrrrwwwssd..","..dwwrrrwwwssd..","..dwrrrrrrwssd..","..dwrrrrrrwssd..",
 "..dwrrrrrrwssd..","..dwwrrrwwwssd..","..dwwrrrwwwssd..","..dwwwwwwwwssd..",
 "...dsssssssssd..","....dddddddd....","................","................"};
 private static final String[] CHART={
 "................","..dddddddddddd..","..dwwwwwwwwwsd..","..dwaaaaaaawsd..",
 "..dwa.....awsd..","..dwa....gawsd..","..dwa...ggaasd..","..dwa..gg.aasd..",
 "..dwa.gg..awsd..","..dwagg...awsd..","..dwa.....awsd..","..dwaaaaaaawsd..",
 "..dssssssssssd..","..dddddddddddd..","................","................"};
 static boolean draw(GuiGraphics g,ResourceLocation id,int x,int y){
  String[] art=switch(id.getNamespace()){case "worldlogic_thermal"->THERMOMETER;case "worldlogic_trauma"->MEDICAL;case "worldlogic"->id.getPath().equals("analytics")?CHART:null;default->null;};
  if(art==null)return false;
  for(int yy=0;yy<art.length;yy++)for(int xx=0;xx<Math.min(16,art[yy].length());xx++){
   int c=switch(art[yy].charAt(xx)){case 'd'->0xff383838;case 'w'->0xffeeeeee;case 's'->0xffa0a0a0;case 'r'->0xffc3433e;case 'a'->0xff737373;case 'g'->0xff62884a;default->0;};
   if(c!=0)g.fill(x+xx,y+yy,x+xx+1,y+yy+1,c);
  }return true;
 }
}
