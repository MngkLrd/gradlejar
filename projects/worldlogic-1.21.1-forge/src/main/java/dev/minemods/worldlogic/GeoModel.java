package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
/** Seeded macro terrain and bounded D8 drainage. No world access or chunk loads. */
public final class GeoModel {
 static final int STEP=16,CORE=48,HALO=24,N=CORE+HALO*2+1,BLEND=8;
 private final NoiseHolder continental,tectonic,detail,moisture;
 private final LinkedHashMap<Long,Tile> tiles=new LinkedHashMap<>(32,.75f,true);
 private record Tile(double[] height,double[] flow){}
 private record Surface(double height,double flow){}
 GeoModel(NoiseHolder c,NoiseHolder t,NoiseHolder d,NoiseHolder m){continental=c;tectonic=t;detail=d;moisture=m;}
 static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
 static double smooth(double a,double b,double x){double t=clamp((x-a)/(b-a),0,1);return t*t*(3-2*t);}
 static double lerp(double a,double b,double t){return a+(b-a)*t;}
 private double c(double x,double z){return continental.getValue(x*.115,0,z*.115);}
 private double t(double x,double z){return tectonic.getValue(x*.115,0,z*.115);}
 private double d(double x,double z,double s){return detail.getValue(x*s,0,z*s);}
 private double m(double x,double z){return moisture.getValue(x*.16,0,z*.16);}
 /** Broad ocean basins, shelves, quiet interiors and narrow uplift belts. */
 double raw(double x,double z){
  double warp=d(x,z,.075)*130;double cont=c(x+warp,z-warp);
  double coast=.075,land=smooth(coast,coast+.19,cont);
  double shelf=smooth(-.52,coast,cont);
  double sea=lerp(11,63,Math.pow(shelf,.66));
  if(cont<=coast)return sea+2.2*d(x,z,.65)*smooth(coast,-.40,cont);
  double fault=t(x+warp*.7,z-warp*.7);
  double belt=Math.exp(-Math.abs(fault)*9);
  double province=smooth(-.30,.30,d(x,z,.12));
  double upland=land*(12+18*province+128*belt*belt*(.45+.55*province));
  double small=land*(1.8+5*belt)*d(x,z,.9);
  return clamp(63+upland+small,63,238);
 }
 /** Strict downhill D8 flow, no cycles; sinks retain their drainage. */
 static double[] accumulate(double[] h,double[] rain,int n){
  int size=n*n;Integer[] order=new Integer[size];int[] next=new int[size];Arrays.fill(next,-1);
  for(int z=0;z<n;z++)for(int x=0;x<n;x++){
   int i=z*n+x;order[i]=i;double best=0;
   for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
    if(dx==0&&dz==0||x+dx<0||x+dx>=n||z+dz<0||z+dz>=n)continue;
    int j=(z+dz)*n+x+dx;double slope=(h[i]-h[j])/(dx!=0&&dz!=0?1.41421356237:1);
    if(slope>best+1e-8){best=slope;next[i]=j;}
   }
  }
  Arrays.sort(order,Comparator.comparingDouble((Integer i)->h[i]).reversed().thenComparingInt(i->i));
  double[] flow=rain.clone();for(int i:order)if(next[i]>=0)flow[next[i]]+=flow[i];
  return flow;
 }
 static double incision(double height,double flow){return Math.min(12,Math.max(0,Math.log1p(flow/8)*2.8))*smooth(63,80,height);}
 private Tile build(int tx,int tz){
  double[] h=new double[N*N],rain=new double[h.length];
  for(int z=0;z<N;z++)for(int x=0;x<N;x++){int i=z*N+x;double wx=((long)tx*CORE+x-HALO)*STEP,wz=((long)tz*CORE+z-HALO)*STEP;h[i]=raw(wx,wz);rain[i]=h[i]>63?clamp(.65+m(wx,wz)*.5,.15,1.3):0;}
  double[] flow=accumulate(h,rain,N);
  for(int i=0;i<h.length;i++)h[i]-=incision(h[i],flow[i]);
  return new Tile(h,flow);
 }
 private synchronized Tile tile(int tx,int tz){
  long key=((long)tx<<32)^(tz&0xffffffffL);Tile a=tiles.get(key);if(a!=null)return a;
  a=build(tx,tz);if(tiles.size()>=32)tiles.remove(tiles.keySet().iterator().next());tiles.put(key,a);return a;
 }
 private static double interpolate(double[] a,double x,double z){
  int ix=(int)Math.floor(x),iz=(int)Math.floor(z);double u=x-ix,v=z-iz;
  return lerp(lerp(a[iz*N+ix],a[iz*N+ix+1],u),lerp(a[(iz+1)*N+ix],a[(iz+1)*N+ix+1],u),v);
 }
 private Surface local(int tx,int tz,double gx,double gz){
  Tile t=tile(tx,tz);double x=gx-(long)tx*CORE+HALO,z=gz-(long)tz*CORE+HALO;
  return new Surface(interpolate(t.height,x,z),interpolate(t.flow,x,z));
 }
 private static Surface mix(Surface a,Surface b,double v){return new Surface(lerp(a.height,b.height,v),lerp(a.flow,b.flow,v));}
 Surface surface(double x,double z){
  double gx=x/STEP,gz=z/STEP;int tx=(int)Math.floor(gx/CORE),tz=(int)Math.floor(gz/CORE);
  double u=gx-(long)tx*CORE,v=gz-(long)tz*CORE;
  int lx=tx,rx=tx,lz=tz,rz=tz;double wx=0,wz=0;
  if(u<BLEND){lx=tx-1;wx=smooth(-BLEND,BLEND,u);}else if(u>CORE-BLEND){rx=tx+1;wx=smooth(CORE-BLEND,CORE+BLEND,u);}
  if(v<BLEND){lz=tz-1;wz=smooth(-BLEND,BLEND,v);}else if(v>CORE-BLEND){rz=tz+1;wz=smooth(CORE-BLEND,CORE+BLEND,v);}
  Surface a=local(lx,lz,gx,gz);if(lx!=rx)a=mix(a,local(rx,lz,gx,gz),wx);
  if(lz==rz)return a;
  Surface b=local(lx,rz,gx,gz);if(lx!=rx)b=mix(b,local(rx,rz,gx,gz),wx);
  return mix(a,b,wz);
 }
 double value(String field,int x,int z){
  Surface s=surface(x,z);double h=s.height;
  return switch(field){
   case "offset" -> (h-128)/128;
   case "continents" -> h<63?clamp(-.18-(63-h)*.014,-1.15,-.18):clamp(-.10+(h-63)*.013,-.10,.85);
   case "erosion" -> clamp(.58-Math.max(0,h-75)*.011+d(x,z,.12)*.18,-.9,.85);
   case "ridges" -> h<78&&s.flow>30?0:clamp(t(x,z)*1.25,-1,1);
   case "temperature" -> clamp(.66*Math.cos(z*Math.PI/22000.0)+d(x,z,.11)*.30-Math.max(0,h-65)*.004,-1,1);
   case "humidity" -> {
    // Prevailing west wind approximation: upwind relief and coastal moisture.
    double up=raw(x-192,z),far=raw(x-576,z);
    double ocean=up<63||far<63?.25:0;
    double windward=clamp((h-up)*.008,-.25,.25);
    double shadow=clamp((Math.max(up,far)-h-20)*.006,0,.60);
    yield clamp(m(x,z)*.75+ocean+windward-shadow,-1,1);
   }
   default -> throw new IllegalStateException(field);
  };
 }
 static boolean drainageFixture(){
  double[] h={9,8,7,8,6,4,7,4,0};double[] rain=new double[9];Arrays.fill(rain,1);
  double[] f=accumulate(h,rain,3);return Math.abs(f[8]-9)<1e-9&&incision(100,50)>incision(100,1)&&incision(50,50)==0;
 }
}
