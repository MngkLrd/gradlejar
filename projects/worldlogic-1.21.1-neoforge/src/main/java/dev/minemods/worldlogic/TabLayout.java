package dev.minemods.worldlogic;
import java.util.*;
/** Client-independent tab geometry and order. */
public final class TabLayout {
 public static final int GUTTER=32, SIZE=24, STEP=26;
 private TabLayout(){}
 public static int contentWidth(int width){return Math.max(1,width-GUTTER);}
 /** Keep the native medical minimum width; narrow windows use the original panel/back button. */
 public static int nativeContentWidth(int width){return width-GUTTER>=330?width-GUTTER:width;}
 public static boolean nativeRailFits(int width,int height){return width-GUTTER>=330&&height>=202;}
 public static int capacity(int height){return Math.max(0,height/STEP);}
 public static int pageSize(int count,int rows){return rows<=0?0:count<=rows?rows:Math.max(0,rows-2);}
 public static int pages(int count,int size){return size<=0?0:Math.max(1,(count+size-1)/size);}
 public static int clampPage(int page,int count,int size){return Math.max(0,Math.min(page,Math.max(0,pages(count,size)-1)));}
 public static Comparator<String> order(java.util.function.ToIntFunction<String> priority){return Comparator.comparingInt(priority).thenComparing(Comparator.naturalOrder());}
 static void verify(){
  if(contentWidth(320)!=288||contentWidth(32)!=1||capacity(166)!=6||capacity(25)!=0||pageSize(6,6)!=6||pageSize(7,6)!=4||pages(9,4)!=3||clampPage(7,9,4)!=2||clampPage(-1,9,4)!=0||pageSize(7,2)!=0)throw new IllegalStateException("tab geometry/paging");
  if(nativeContentWidth(361)!=361||nativeContentWidth(362)!=330||nativeContentWidth(800)!=768||nativeRailFits(361,300)||nativeRailFits(400,201)||!nativeRailFits(400,202))throw new IllegalStateException("native medical rail gutter");
  var ids=new ArrayList<>(List.of("z:a","a:b","a:a"));ids.sort(order(id->id.startsWith("z")?0:10));
  if(!ids.equals(List.of("z:a","a:a","a:b")))throw new IllegalStateException("tab order");
 }
}
