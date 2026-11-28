package dev.minemods.worldlogic;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** Game-scale seeded geological provinces and deposits; not literal USGS tonnages. */
public final class GeologyModel {
 private GeologyModel(){}
 public static final int DISTRICT=192;
 public enum Province { SEDIMENTARY, CARBONATE, INTRUSIVE, METAMORPHIC, CRATON }
 public enum Ore { COAL, IRON, COPPER, GOLD, DIAMOND, EMERALD, LAPIS, REDSTONE, HEMATITE, MAGNETITE, CASSITERITE, GALENA, SPHALERITE, PYRITE, MALACHITE, BAUXITE, FLUORITE, CINNABAR;
  public boolean additional(){return ordinal()>REDSTONE.ordinal();}
 }
 public record Body(Ore ore,double x,double y,double z,double along,double across,double vertical,double angle,double dip,double grade,long salt){
  public double score(int px,int py,int pz){
   double dx=px-x,dz=pz-z,a=dx*Math.cos(angle)+dz*Math.sin(angle),b=-dx*Math.sin(angle)+dz*Math.cos(angle);
   double h=py-y-a*dip-Math.min(1.8,vertical*.38)*Math.sin(a*.075+salt%13);
   return a*a/(along*along)+b*b/(across*across)+h*h/(vertical*vertical);
  }
  public boolean contains(int px,int py,int pz){double s=score(px,py,pz);return s<=1&&unit(hash(salt,px,py,pz))<grade*(1-.65*s);}
  public int minY(){return (int)Math.floor(y-vertical-Math.abs(dip)*along-2);}
  public int maxY(){return (int)Math.ceil(y+vertical+Math.abs(dip)*along+2);}
 }
 public record Cave(double x,double y,double z,double along,double across,double vertical,double angle,double dip,int waterY,boolean shaft){
  public double score(int px,int py,int pz){
   double dx=px-x,dz=pz-z,a=dx*Math.cos(angle)+dz*Math.sin(angle),b=-dx*Math.sin(angle)+dz*Math.cos(angle);
   b-=Math.min(4,across*.65)*Math.sin(a*.07);
   double h=py-y-a*dip-3*Math.sin(a*.06);
   return a*a/(along*along)+b*b/(across*across)+h*h/(vertical*vertical);
  }
  public int minY(){return (int)Math.floor(y-vertical-Math.abs(dip)*along-4);}
  public int maxY(){return (int)Math.ceil(y+vertical+Math.abs(dip)*along+4);}
  /** Preserve all existing underground shafts; only a deterministic 1/32 can reach daylight. */
  public boolean surfaceOpening(){
   return shaft&&Math.floorMod(hash(0x57c2b19d91e2L,(int)Math.floor(x),(int)Math.floor(y),(int)Math.floor(z)),32)==0;
  }
  public int shaftTop(int surface){return surfaceOpening()?surface+1:surface-12;}
  public boolean shaftAt(int px,int py,int pz,int surface){
   if(!shaft||surface<67||py<y||py>shaftTop(surface))return false;
   double distance=Math.hypot(px-x,pz-z);
   double radius=surfaceOpening()?1.6+Math.max(0,Math.min(1,(py-(surface-6))/6.0))*1.6:1.6;
   return distance<radius;
  }
 }
 public record District(Province province,List<Body> ores,List<Cave> caves){}
 public record Pocket(Body body,boolean relative){}
 public static final int POCKET_CELL=16;
 private static Pocket pocket(SplittableRandom r,Ore ore,int gx,int gz,double y,boolean relative){
  double a=r.nextDouble(2.4,4.4),b=r.nextDouble(1.6,2.7),v=r.nextDouble(1.35,2.4);
  if(ore==Ore.DIAMOND||ore==Ore.EMERALD){a*=.64;b*=.72;v*=.85;}
  return new Pocket(new Body(ore,gx*(double)POCKET_CELL+r.nextDouble(2,14),y,gz*(double)POCKET_CELL+r.nextDouble(2,14),a,b,v,r.nextDouble(2*Math.PI),r.nextDouble(-.22,.22),r.nextDouble(.72,.93),r.nextLong()),relative);
 }
 /** Independent small-body background: common ores near the surface in every district,
  * rarer ores deeper. Coordinates are deterministic and never depend on chunk order. */
 public static List<Pocket> pockets(long seed,int gx,int gz){
  var r=new SplittableRandom(hash(seed^0x65b3a5cc8d143ab1L,gx,2,gz));
  var p=province(seed,gx*POCKET_CELL+8,gz*POCKET_CELL+8);
  var list=new ArrayList<Pocket>(7);
  list.add(pocket(r,Ore.COAL,gx,gz,-r.nextDouble(6,23),true));
  list.add(pocket(r,Ore.IRON,gx,gz,-r.nextDouble(12,39),true));
  list.add(pocket(r,p==Province.INTRUSIVE||r.nextBoolean()?Ore.COPPER:Ore.COAL,gx,gz,-r.nextDouble(27,56),true));
  list.add(pocket(r,r.nextDouble()<.68?Ore.IRON:Ore.COPPER,gx,gz,-r.nextDouble(45,92),true));
  list.add(pocket(r,r.nextDouble()<.63?Ore.IRON:Ore.LAPIS,gx,gz,r.nextDouble(-115,-35),false));
  list.add(pocket(r,r.nextDouble()<.65?Ore.REDSTONE:Ore.GOLD,gx,gz,r.nextDouble(-215,-80),false));
  double chance=p==Province.CRATON?.24:.12;
  if(r.nextDouble()<chance)list.add(pocket(r,Ore.DIAMOND,gx,gz,r.nextDouble(-225,-105),false));
  if(p==Province.METAMORPHIC&&r.nextDouble()<.18)list.add(pocket(r,Ore.EMERALD,gx,gz,-r.nextDouble(8,50),true));
  return List.copyOf(list);
 }
 public static List<Pocket> nearbyPockets(long seed,int x,int z){
  int gx=Math.floorDiv(x,POCKET_CELL),gz=Math.floorDiv(z,POCKET_CELL);var out=new ArrayList<Pocket>(72);
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)out.addAll(pockets(seed,gx+dx,gz+dz));
  return out;
 }
 public static long hash(long seed,int x,int y,int z){
  long h=seed^(x*0x9e3779b97f4a7c15L)^(y*0x632be59bd9b4e019L)^(z*0xc2b2ae3d27d4eb4fL);
  h=(h^(h>>>30))*0xbf58476d1ce4e5b9L;h=(h^(h>>>27))*0x94d049bb133111ebL;return h^(h>>>31);
 }
 static double unit(long h){return (h>>>11)*0x1.0p-53;}
 public static Province province(long seed,int x,int z){
  return Province.values()[(int)Math.floorMod(hash(seed,Math.floorDiv(x,DISTRICT*3),0,Math.floorDiv(z,DISTRICT*3)),5)];
 }
 private static double normal(SplittableRandom r){return Math.sqrt(-2*Math.log(Math.max(1e-12,r.nextDouble())))*Math.cos(2*Math.PI*r.nextDouble());}
 private static Body body(SplittableRandom r,Ore ore,double x,double y,double z,double a,double b,double v,double angle,double dip,double grade){
  // USGS grade/tonnage models frequently use lognormal distributions. The medians and
  // truncation below are game calibration values; not direct conversions of economic grades.
  double size=Math.max(.72,Math.min(1.20,Math.exp(.23*normal(r))));
  double richness=Math.max(.70,Math.min(1.25,Math.exp(.28*normal(r))));
  double g=Math.max(.06,Math.min(.90,grade*richness));
  // Cap the expected ore yield of regional bodies, not just their width.
  double target=switch(ore){case DIAMOND->18;case EMERALD->24;case GOLD,LAPIS->85;default->140;};
  double aa=a*size,bb=b*size,vv=v;
  double estimate=4*Math.PI/3*aa*bb*vv*g*.61;
  double scale=Math.min(1,Math.cbrt(target/estimate));
  return new Body(ore,x,y,z,aa*scale,bb*scale,vv*scale,angle,dip,g,r.nextLong());
 }
 public static District district(long seed,int gx,int gz){
  var r=new SplittableRandom(hash(seed,gx,1,gz));
  double x=gx*(double)DISTRICT+DISTRICT*(.3+.4*r.nextDouble()),z=gz*(double)DISTRICT+DISTRICT*(.3+.4*r.nextDouble());
  var p=province(seed,(int)x,(int)z);
  double a=r.nextDouble(2*Math.PI),dip=r.nextDouble(-.18,.18);
  var ores=new ArrayList<Body>();var caves=new ArrayList<Cave>();
  if(p==Province.SEDIMENTARY||p==Province.CARBONATE){
   ores.add(body(r,Ore.COAL,x,r.nextDouble(20,68),z,62,38,2.4,a,dip,.82));
   ores.add(body(r,Ore.COAL,x+8,r.nextDouble(-38,10),z-8,43,30,1.9,a,dip,.72));
   ores.add(body(r,Ore.IRON,x,r.nextDouble(-65,-12),z,46,22,4,a,dip,.48));
  }
  if(p==Province.INTRUSIVE){
   ores.add(body(r,Ore.COPPER,x,r.nextDouble(-24,48),z,42,26,20,a,.2,.29));
   ores.add(body(r,Ore.GOLD,x+12,r.nextDouble(-75,-20),z,48,3.8,7,a,.48,.43));
   ores.add(body(r,Ore.IRON,x-18,r.nextDouble(-80,-30),z,32,14,9,a,.18,.46));
  }
  if(p==Province.METAMORPHIC){
   ores.add(body(r,Ore.GOLD,x,r.nextDouble(-125,-40),z,56,3,7,a,.52,.45));
   ores.add(body(r,Ore.EMERALD,x+10,r.nextDouble(10,65),z-12,32,4,10,a,.35,.25));
   ores.add(body(r,Ore.LAPIS,x-18,r.nextDouble(-55,15),z+8,27,12,8,a,.18,.34));
  }
  if(p==Province.CRATON){
   ores.add(body(r,Ore.IRON,x,r.nextDouble(-145,-70),z,58,27,5,a,dip,.56));
   if(r.nextDouble()<.35)ores.add(body(r,Ore.DIAMOND,x,r.nextDouble(-188,-140),z,8,7,48,a,.04,.16));
   ores.add(body(r,Ore.REDSTONE,x-16,r.nextDouble(-190,-120),z+10,45,13,12,a,.24,.46));
  }
  ores.add(body(r,Ore.IRON,x+23,r.nextDouble(-25,55),z-20,20,10,7,a,.15,.36));
  if(r.nextDouble()<.55)ores.add(body(r,Ore.COPPER,x-18,r.nextDouble(5,48),z+15,24,10,8,a,.18,.32));
  if(r.nextDouble()<.5)ores.add(body(r,Ore.LAPIS,x+8,r.nextDouble(-85,-20),z+16,16,8,6,a,.1,.32));
  if(r.nextDouble()<.65)ores.add(body(r,Ore.REDSTONE,x,r.nextDouble(-205,-125),z,30,10,8,a,.1,.35));
  if(p==Province.CARBONATE){
   double y=r.nextDouble(-42,15);int water=(int)Math.floor(y-4);
   caves.add(new Cave(x,y,z,82,5,4,a,.12,water,r.nextDouble()<.08));
   caves.add(new Cave(x,y,z,60,4,6,a+Math.PI*.48,-.10,water,false));
   // Chamber straddles the shared static water horizon. A lateral connected lake
   // extends away from the intersection; not every cave is a giant cavern.
   caves.add(new Cave(x,y-4,z,16,13,10,a,0,water,false));
   double lx=x+28*Math.cos(a),lz=z+28*Math.sin(a);
   caves.add(new Cave(lx,y-5,lz,25,15,9,a,.02,water,false));
  }else if(p==Province.METAMORPHIC||p==Province.INTRUSIVE){
   caves.add(new Cave(x,r.nextDouble(-82,-15),z,64,3,13,a,.24,-100,r.nextDouble()<.02));
  }else caves.add(new Cave(x,r.nextDouble(-120,-40),z,42,3,3,a,.12,-160,false));
  // Independent random stream after existing ores and caves.
  var extra=new SplittableRandom(hash(seed^0x56b73d9a41c2efL,gx,19,gz));
  switch(p){
   case SEDIMENTARY -> {extra(ores,extra,Ore.HEMATITE,x,z,-60,18,24,12,3,.10,.48);extra(ores,extra,Ore.BAUXITE,x,z,30,57,20,12,2,.03,.43);}
   case CARBONATE -> {extra(ores,extra,Ore.GALENA,x,z,-75,18,26,4,6,.22,.46);extra(ores,extra,Ore.FLUORITE,x,z,-55,35,20,5,5,.25,.38);extra(ores,extra,Ore.SPHALERITE,x,z,-90,12,17,5,6,.15,.37);}
   case INTRUSIVE -> {extra(ores,extra,Ore.CASSITERITE,x,z,-110,10,24,4,8,.35,.36);extra(ores,extra,Ore.MAGNETITE,x,z,-125,-15,20,10,8,.10,.46);extra(ores,extra,Ore.CINNABAR,x,z,-30,43,14,3,5,.30,.32);extra(ores,extra,Ore.MALACHITE,x,z,22,57,14,5,3,.12,.43);}
   case METAMORPHIC -> {extra(ores,extra,Ore.PYRITE,x,z,-95,30,23,6,6,.25,.46);extra(ores,extra,Ore.SPHALERITE,x,z,-90,0,20,4,6,.35,.36);extra(ores,extra,Ore.HEMATITE,x,z,-125,-20,22,8,4,.18,.4);}
   case CRATON -> {extra(ores,extra,Ore.MAGNETITE,x,z,-175,-60,24,10,4,.08,.42);extra(ores,extra,Ore.HEMATITE,x,z,-150,-50,21,11,4,.12,.44);}
  }
  return new District(p,List.copyOf(ores),List.copyOf(caves));
 }
 private static void extra(List<Body> ores,SplittableRandom r,Ore ore,double x,double z,double ymin,double ymax,double a,double b,double v,double dip,double grade){
  if(r.nextDouble()>.82)return;
  ores.add(body(r,ore,x+r.nextDouble(-24,24),r.nextDouble(ymin,ymax),z+r.nextDouble(-24,24),a,b,v,r.nextDouble(2*Math.PI),dip,grade));
 }
 public static List<District> around(long seed,int x,int z){
  int gx=Math.floorDiv(x,DISTRICT),gz=Math.floorDiv(z,DISTRICT);var out=new ArrayList<District>(9);
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)out.add(district(seed,gx+dx,gz+dz));return out;
 }
}
