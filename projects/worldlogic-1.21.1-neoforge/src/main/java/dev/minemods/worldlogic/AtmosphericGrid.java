package dev.minemods.worldlogic;

import java.util.Arrays;

/** Bounded, conservative horizontal transport prototype. Air mass is normalized per cell,
 * heat is mass * temperature(K), water is mass * vapor mixing ratio. Closed edges.
 * Not a full atmospheric energy budget, Navier-Stokes solver or global forecast. */
final class AtmosphericGrid {
 static final int SIZE=9,CELL=1024,COUNT=SIZE*SIZE;
 record Cell(double pressureHpa,double airC,double mixingRatioGkg,double relativeHumidity){}
 record Budget(double mass,double heat,double water){}
 final int originX,originZ;
 final double windX,windZ;
 final double[] mass,heat,water;
 long lastTick;
 AtmosphericGrid(int x,int z,double wx,double wz,long tick,double[] mass,double[] heat,double[] water){
  originX=x;originZ=z;windX=wx;windZ=wz;lastTick=tick;this.mass=mass;this.heat=heat;this.water=water;
 }
 private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
 static AtmosphericGrid initialize(long seed,int x,int z,long tick,double baseC,double moisture,double wx,double wz){
  double[] m=new double[COUNT],h=new double[COUNT],q=new double[COUNT];
  for(int iz=0;iz<SIZE;iz++)for(int ix=0;ix<SIZE;ix++){
   int i=iz*SIZE+ix;
   var s=RegionalWeatherModel.sample(seed,x+(ix-SIZE/2)*CELL,z+(iz-SIZE/2)*CELL,tick,0,moisture,wx,wz);
   double t=clamp(baseC+s.temperatureAnomalyC(),-60,55),pressure=s.seaLevelPressureHpa();
   double rh=AtmosphereModel.humidity(t,baseC,s.moistureFraction(),0);
   m[i]=pressure/1013.25;h[i]=m[i]*(t+273.15);
   q[i]=m[i]*AtmosphericColumn.vaporMixingRatio(AtmosphericColumn.saturationHpa(t)*rh/100,pressure);
  }
  return new AtmosphericGrid(x,z,wx,wz,tick,m,h,q);
 }
 boolean contains(int x,int z){return Math.abs((long)x-originX)<=CELL*(SIZE/2)&&Math.abs((long)z-originZ)<=CELL*(SIZE/2);}
 Budget budget(){return new Budget(Arrays.stream(mass).sum(),Arrays.stream(heat).sum(),Arrays.stream(water).sum());}
 private static void exchange(int a,int b,double velocity,double[] m,double[] h,double[] q,double[] dm,double[] dh,double[] dq){
  int from=velocity>=0?a:b,to=velocity>=0?b:a;
  // Max 8% donor mass per face; even four outward faces cannot drain a cell.
  double fraction=Math.min(.08,Math.abs(velocity)*.01);
  double mm=m[from]*fraction,hh=h[from]*fraction,qq=q[from]*fraction;
  dm[from]-=mm;dm[to]+=mm;dh[from]-=hh;dh[to]+=hh;dq[from]-=qq;dq[to]+=qq;
 }
 void advance(long seed,long tick){
  if(tick<=lastTick)return;
  double[] dm=new double[COUNT],dh=new double[COUNT],dq=new double[COUNT];
  for(int z=0;z<SIZE;z++)for(int x=0;x<SIZE;x++){
   int i=z*SIZE+x,wx=originX+(x-SIZE/2)*CELL,wz=originZ+(z-SIZE/2)*CELL;
   // Synoptic wave is still prescribed steering. Pressure correction is down-gradient.
   if(x+1<SIZE){var s=RegionalWeatherModel.sample(seed,wx+CELL*.5,wz,tick,0,.5,windX,windZ);
    exchange(i,i+1,s.windX()+.12*1013.25*(mass[i]-mass[i+1]),mass,heat,water,dm,dh,dq);}
   if(z+1<SIZE){var s=RegionalWeatherModel.sample(seed,wx,wz+CELL*.5,tick,0,.5,windX,windZ);
    exchange(i,i+SIZE,s.windZ()+.12*1013.25*(mass[i]-mass[i+SIZE]),mass,heat,water,dm,dh,dq);}
  }
  for(int i=0;i<COUNT;i++){mass[i]+=dm[i];heat[i]+=dh[i];water[i]+=dq[i];}
  lastTick=tick;
 }
 private double interpolate(double[] v,int x,int z){
  double fx=clamp((x-(double)originX)/CELL+SIZE/2,0,SIZE-1),fz=clamp((z-(double)originZ)/CELL+SIZE/2,0,SIZE-1);
  int ix=(int)fx,iz=(int)fz,jx=Math.min(SIZE-1,ix+1),jz=Math.min(SIZE-1,iz+1);
  double a=fx-ix,b=fz-iz;
  return (1-b)*((1-a)*v[iz*SIZE+ix]+a*v[iz*SIZE+jx])+b*((1-a)*v[jz*SIZE+ix]+a*v[jz*SIZE+jx]);
 }
 Cell sample(int x,int z,double elevationBlocks){
  double m=interpolate(mass,x,z),t=interpolate(heat,x,z)/m-273.15,r=interpolate(water,x,z)/m;
  // Model atmospheric metres, not physical block distances. Clamp outside calibration range.
  double height=clamp(elevationBlocks*10,-1280,4480);
  double p=clamp(1013.25*m*Math.exp(-height/8434.0),500,1150);
  double e=p*r/(.622+r);
  return new Cell(p,t,r*1000,clamp(100*e/AtmosphericColumn.saturationHpa(t),0,100));
 }
 static boolean valid(double[] m,double[] h,double[] q){
  if(m.length!=COUNT||h.length!=COUNT||q.length!=COUNT)return false;
  for(int i=0;i<COUNT;i++)if(!Double.isFinite(m[i])||!Double.isFinite(h[i])||!Double.isFinite(q[i])||m[i]<=0||h[i]<=0||q[i]<0)return false;
  return true;
 }
}
