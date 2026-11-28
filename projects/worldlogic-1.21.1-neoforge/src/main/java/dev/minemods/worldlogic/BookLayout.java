package dev.minemods.worldlogic;
/** Server-safe geometry used by the client book, not a graphical renderer test. */
public record BookLayout(int x,int y,int w,int h,int pageW,int pageH,int left,int right,int textTop,int textW,int rows){
 public static BookLayout of(int width,int height){
  int usable=TabLayout.contentWidth(width),w=Math.min(600,usable-16),h=Math.min(350,height-16);
  int x=(usable-w)/2,y=(height-h)/2,pw=(w-22)/2,ph=h-20;
  return new BookLayout(x,y,w,h,pw,ph,x+11,x+11+pw,y+10,Math.max(1,pw-22),Math.max(1,(ph-65)/11));
 }
 public boolean readable(){return pageW>=66&&pageH>=105;}
 public int bottom(){return y+h;}
 public int leftText(){return left+11;}
 public int rightText(){return right+11;}
 public int capacity(){return Math.max(1,rows*2);}
 public int spreads(int lines,int firstCapacity){return 1+Math.max(0,lines-firstCapacity+capacity()-1)/capacity();}
 public int offset(int spread,int firstCapacity){return spread==0?0:firstCapacity+(spread-1)*capacity();}
 static void verify(){
  for(int[] s:new int[][]{{320,180},{427,240},{640,360},{1280,720}}){
   var b=of(s[0],s[1]);if(!b.readable()||b.leftText()+b.textW()>b.right()||b.rightText()+b.textW()>b.x()+b.w()||b.y()<0||b.bottom()>s[1]||b.x()+b.w()>TabLayout.contentWidth(s[0])||b.rows()<1)throw new IllegalStateException("book geometry");
   int first=Math.max(1,b.capacity()-5);if(b.spreads(first,first)!=1||b.spreads(first+1,first)!=2||b.offset(1,first)!=first||b.offset(2,first)!=first+b.capacity())throw new IllegalStateException("book text pagination");
  }
  if(of(120,80).readable())throw new IllegalStateException("tiny book fallback");
 }
}
