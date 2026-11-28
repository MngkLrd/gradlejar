package dev.minemods.worldlogic;
/** Continuous local valley cross-sections. All profiles preserve their shared
 * water surface and meet original terrain at the outer edge. */
final class RiverProfiles{
 enum Kind{FLOODPLAIN,ALLUVIAL,UPLAND,ROCKY_GORGE}
 record Section(Kind kind,double bed,double water,double bankDistance,boolean wet){}
 static Kind kind(RegionalDrainage.Hit h){
  if(h.slope()>.15)return Kind.ROCKY_GORGE;
  if(h.slope()>.065)return Kind.UPLAND;
  return h.tributaries()>=3?Kind.FLOODPLAIN:Kind.ALLUVIAL;
 }
 private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
 private static double smooth(double t){t=clamp(t,0,1);return t*t*(3-2*t);}
 static Section sample(RegionalDrainage.Hit h,double terrain){
  Kind kind=kind(h);
  // Blend profile parameters by grade rather than jumping at named categories.
  double steep=smooth((h.slope()-.035)/.15),broad=smooth((h.tributaries()-1)/3.0);
  double r=h.radius(),d=h.distance(),stage=h.stage();
  double bedPower=1.35+.75*steep+.4*broad;
  if(d<=r){
   double depth=1+(h.depth()-1)*(1-Math.pow(clamp(d/r,0,1),bedPower));
   return new Section(kind,stage-depth,stage,h.bank(),true);
  }
  double u=clamp((d-r)/(h.bank()-r),0,1);
  // Lowland floodplain has an inner shelf; steep valley grades into hillslope.
  double low=smooth(u*u),high=smooth(Math.sqrt(u)),blend=low+(high-low)*steep;
  double target=stage+1+(terrain-stage-1)*blend;
  return new Section(kind,target,stage,h.bank(),false);
 }
 static void verify(){
  for(double slope:new double[]{0,.07,.18})for(int flow:new int[]{1,4}){
   var center=new RegionalDrainage.Hit(0,.5,90,8,6,26,1,0,slope,flow);
   var edge=new RegionalDrainage.Hit(8,.5,90,8,6,26,1,0,slope,flow);
   var outer=new RegionalDrainage.Hit(26,.5,90,8,6,26,1,0,slope,flow);
   var a=sample(center,110);var b=sample(edge,110);var c=sample(outer,110);
   if(a.bed()!=84||b.bed()!=89||c.bed()!=110||!a.wet()||c.wet())throw new IllegalStateException("river profile endpoints");
   double previous=a.bed();
   for(int i=1;i<=26;i++){
    var s=sample(new RegionalDrainage.Hit(i,.5,90,8,6,26,1,0,slope,flow),110);
    if(s.bed()<previous-1e-8||s.water()!=90)throw new IllegalStateException("river section monotonic");
    previous=s.bed();
   }
  }
 }
}
