package dev.minemods.worldlogic;
import java.util.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.world.level.levelgen.DensityFunction.NoiseHolder;
/** Seeded macro terrain and bounded D8 drainage. No world access or chunk loads. */
public final class GeoModel {
 static final int STEP=16,CORE=48,HALO=24,N=CORE+HALO*2+1,BLEND=8;
 private final NoiseHolder continental,tectonic,detail,moisture;
 // Retain hot entries instead of evicting an arbitrary hash bucket. Raw pages
 // are shared between overlapping drainage tiles, preserving all sample values.
 private static final int PAGE=24;
 private final Cache<Long,Tile> tiles=CacheBuilder.newBuilder().maximumSize(192).build();
 private final Cache<Long,RawPage> rawPages=CacheBuilder.newBuilder().maximumSize(768).build();
 private final AtomicBoolean checked=new AtomicBoolean();
 private record RawPage(double[] height,double[] rain){}
 private static long key(int x,int z){
  long k=((long)x<<32)^(z&0xffffffffL);
  k=(k^(k>>>30))*0xbf58476d1ce4e5b9L;
  k=(k^(k>>>27))*0x94d049bb133111ebL;
  return k^(k>>>31);
 }
 private final LongAdder builds=new LongAdder(),buildNanos=new LongAdder();
 private record Sample(int x,int z,Surface surface){}
 private final ThreadLocal<Sample> lastSample=new ThreadLocal<>();
 private final double climatePhase;
 private record ClimateSample(int x,int z,double thermal,double humidity,double biomeTemperature,double latitude,double windX,double windZ){}
 private final ThreadLocal<ClimateSample> lastClimate=new ThreadLocal<>();
 private record Tile(double[] height,double[] flow){}
 private record Surface(double height,double flow){}
 GeoModel(NoiseHolder c,NoiseHolder t,NoiseHolder d,NoiseHolder m){
  continental=c;tectonic=t;detail=d;moisture=m;
  // A deterministic phase from the actual seeded noise, not world creation order.
  long k=key((int)(c.getValue(912.5,0,-177.25)*1000000000),(int)(m.getValue(-651.25,0,391.5)*1000000000));
  climatePhase=((k>>>11)*0x1.0p-53-.5)*44000;
 }
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
 private RawPage buildPage(int px,int pz){
  double[] h=new double[PAGE*PAGE],rain=new double[h.length];
  for(int z=0;z<PAGE;z++)for(int x=0;x<PAGE;x++){
   int i=z*PAGE+x;double wx=((long)px*PAGE+x)*STEP,wz=((long)pz*PAGE+z)*STEP;
   h[i]=raw(wx,wz);rain[i]=h[i]>63?clamp(.65+m(wx,wz)*.5,.15,1.3):0;
  }
  return new RawPage(h,rain);
 }
 private RawPage page(int px,int pz){
  try{return rawPages.get(key(px,pz),()->buildPage(px,pz));}
  catch(ExecutionException e){throw new IllegalStateException("geography raw page",e.getCause());}
 }
 private Tile build(int tx,int tz){
  double[] h=new double[N*N],rain=new double[h.length];
  int x0=tx*CORE-HALO,z0=tz*CORE-HALO;
  // Copy rectangular intersections of the immutable globally aligned pages.
  for(int pz=Math.floorDiv(z0,PAGE);pz<=Math.floorDiv(z0+N-1,PAGE);pz++){
   int fromZ=Math.max(z0,pz*PAGE),toZ=Math.min(z0+N,(pz+1)*PAGE);
   for(int px=Math.floorDiv(x0,PAGE);px<=Math.floorDiv(x0+N-1,PAGE);px++){
    int fromX=Math.max(x0,px*PAGE),toX=Math.min(x0+N,(px+1)*PAGE);
    RawPage p=page(px,pz);
    for(int z=fromZ;z<toZ;z++){
     int src=(z-pz*PAGE)*PAGE+fromX-px*PAGE,dst=(z-z0)*N+fromX-x0;
     System.arraycopy(p.height,src,h,dst,toX-fromX);
     System.arraycopy(p.rain,src,rain,dst,toX-fromX);
    }
   }
  }
  if(Boolean.getBoolean("worldlogic.ci")&&checked.compareAndSet(false,true)){
   verifyRaw(h,rain,x0,z0);
   // Also exercise a negative-coordinate grid, including page boundaries.
   verifyPage(-1,-1);
   org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_CACHE_EXACT_PASSED");
  }
  double[] flow=accumulate(h,rain,N);
  for(int i=0;i<h.length;i++)h[i]-=incision(h[i],flow[i]);
  return new Tile(h,flow);
 }
 private void verifyRaw(double[] h,double[] rain,int x0,int z0){
  for(int z=0;z<N;z++)for(int x=0;x<N;x++){
   int i=z*N+x;double wx=((long)x0+x)*STEP,wz=((long)z0+z)*STEP;
   double expected=raw(wx,wz),r=expected>63?clamp(.65+m(wx,wz)*.5,.15,1.3):0;
   if(Double.doubleToLongBits(expected)!=Double.doubleToLongBits(h[i])||Double.doubleToLongBits(r)!=Double.doubleToLongBits(rain[i]))
    throw new IllegalStateException("Cached terrain diverges from original raw samples");
  }
 }
 private void verifyPage(int px,int pz){
  RawPage p=page(px,pz);
  for(int z=0;z<PAGE;z++)for(int x=0;x<PAGE;x++){
   double wx=((long)px*PAGE+x)*STEP,wz=((long)pz*PAGE+z)*STEP;
   double h=raw(wx,wz),r=h>63?clamp(.65+m(wx,wz)*.5,.15,1.3):0;
   int i=z*PAGE+x;
   if(Double.doubleToLongBits(h)!=Double.doubleToLongBits(p.height[i])||Double.doubleToLongBits(r)!=Double.doubleToLongBits(p.rain[i]))
    throw new IllegalStateException("Negative-coordinate cached samples diverge");
  }
 }
 private Tile tile(int tx,int tz){
  try{return tiles.get(key(tx,tz),()->{long start=System.nanoTime();Tile made=build(tx,tz);
   builds.increment();buildNanos.add(System.nanoTime()-start);return made;});}
  catch(ExecutionException e){throw new IllegalStateException("geography drainage tile",e.getCause());}
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
 // RC19 only. Climate calculations must not change the legacy drainage or terrain.
 static double biomeTemperature(double thermal,double humidity,double height){
  // The vanilla hottest land row is desert at ALL humidity levels.
  // Route humid tropical land to its warm jungle/savanna row; keep ocean thermal classes.
  return height>=63?lerp(thermal,Math.min(thermal,.48),smooth(-.28,.25,humidity)):thermal;
 }
 private ClimateSample climate(int x,int z,double h){
  ClimateSample cached=lastClimate.get();if(cached!=null&&cached.x==x&&cached.z==z)return cached;
  double theta=(z+climatePhase)*Math.PI/22000.0;
  double latitude=Math.acos(clamp(Math.cos(theta),-1,1))/Math.PI;
  double thermal=clamp(.72*Math.cos(theta)+d(x,z,.11)*.24-Math.max(0,h-65)*.004,-1,1);
  // Alternating tropical easterlies, temperate westerlies, polar easterlies.
  double wind=-Math.cos(latitude*3*Math.PI);
  double dx=wind,dz=.45*Math.sin(theta);
  double length=Math.max(.35,Math.hypot(dx,dz));dx/=length;dz/=length;
  double near=raw(x-dx*192,z-dz*192),mid=raw(x-dx*576,z-dz*576),far=raw(x-dx*1536,z-dz*1536);
  double ocean=.24*(1-smooth(59,67,near))+.15*(1-smooth(59,67,mid))+.09*(1-smooth(59,67,far));
  double equatorial=.34*Math.exp(-Math.pow(latitude/.19,2));
  double subtropical=-.27*Math.exp(-Math.pow((latitude-.35)/.15,2));
  double temperate=.12*Math.exp(-Math.pow((latitude-.62)/.20,2));
  double lift=clamp((h-near)*.006,-.12,.24);
  double shadow=clamp((Math.max(near,Math.max(mid,far))-h-16)*.006,0,.60);
  double humidity=clamp(m(x,z)*.78+equatorial+subtropical+temperate+ocean-.10+lift-shadow,-1,1);
  ClimateSample made=new ClimateSample(x,z,thermal,humidity,biomeTemperature(thermal,humidity,h),latitude,dx,dz);
  lastClimate.set(made);return made;
 }
 dev.minemods.worldlogic.api.WorldLogicApi.RegionalClimate regional(int x,int z){
  Surface s=surface(x,z);ClimateSample c=climate(x,z,s.height);
  return new dev.minemods.worldlogic.api.WorldLogicApi.RegionalClimate(c.thermal,(c.humidity+1)*.5,c.latitude*90,c.windX,c.windZ,s.height);
 }
 double value(String field,int x,int z){
  Sample previous=lastSample.get();
  Surface s;if(previous!=null&&previous.x==x&&previous.z==z)s=previous.surface;
  else{s=surface(x,z);lastSample.set(new Sample(x,z,s));}
  double h=s.height;
  return switch(field){
   case "offset" -> (h-128)/128;
   case "continents" -> h<63?clamp(-.18-(63-h)*.014,-1.15,-.18):clamp(-.10+(h-63)*.013,-.10,.85);
   case "erosion" -> clamp(.58-Math.max(0,h-75)*.011+d(x,z,.12)*.18,-.9,.85);
   case "ridges" -> h<78&&s.flow>30?0:clamp(t(x,z)*1.25,-1,1);
   case "climate_temperature" -> climate(x,z,h).biomeTemperature;
   case "climate_humidity" -> climate(x,z,h).humidity;
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
  double[] f=accumulate(h,rain,3);double[] two={2,4,9,4,2,1,3,8,3,1,0,2,7,2,0,1,3,8,3,1,2,4,9,4,2};double[] r=new double[25];Arrays.fill(r,1);double[] split=accumulate(two,r,5);return Math.abs(f[8]-9)<1e-9&&Math.abs(split[10]+split[14]-25)<1e-9&&split[10]>1&&split[14]>1&&incision(100,50)>incision(100,1)&&incision(50,50)==0;
 }
}
