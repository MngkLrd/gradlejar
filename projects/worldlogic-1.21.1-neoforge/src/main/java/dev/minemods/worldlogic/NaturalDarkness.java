package dev.minemods.worldlogic;
/** Lightmap-only contrast. Full light, emissive pixels, GUI and server light levels remain untouched. */
public final class NaturalDarkness{
 private NaturalDarkness(){}
 public static double gamma(double gamma){return Math.min(.10,Math.max(0,gamma));}
 public static int darken(int rgba){
  int r=rgba&255,g=(rgba>>>8)&255,b=(rgba>>>16)&255;
  int peak=Math.max(r,Math.max(g,b));
  double x=Math.min(1,peak/150.0),factor=.32+.68*x*x*(3-2*x);
  return (rgba&0xff000000)|((int)Math.round(b*factor)<<16)|((int)Math.round(g*factor)<<8)|(int)Math.round(r*factor);
 }
 static void verify(){if(gamma(1)!=.10||gamma(0)!=0||darken(0xffffffff)!=0xffffffff||(darken(0xff303030)&255)>=48||darken(0xff000000)!=0xff000000)throw new IllegalStateException("natural darkness curve");}
}
