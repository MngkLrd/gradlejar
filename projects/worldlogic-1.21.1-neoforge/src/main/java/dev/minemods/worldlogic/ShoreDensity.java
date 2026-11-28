package dev.minemods.worldlogic;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
/** Biome-only continentalness. Sample unchanged terrain density at quart columns;
 * macro height alone is not reliable where the surface caves remove rock. */
public final class ShoreDensity implements DensityFunction {
 public static final MapCodec<ShoreDensity> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  DensityFunction.HOLDER_HELPER_CODEC.fieldOf("macro_offset").forGetter(s->s.macro),
  DensityFunction.HOLDER_HELPER_CODEC.fieldOf("surface_density").forGetter(s->s.terrain)
 ).apply(i,ShoreDensity::new));
 private final DensityFunction macro,terrain;
 private record Last(int x,int z,double value){}
 private final ThreadLocal<Last> last=new ThreadLocal<>();
 private final com.google.common.cache.Cache<Long,Double> columns=com.google.common.cache.CacheBuilder.newBuilder().maximumSize(32768).build();
 private ShoreDensity(DensityFunction macro,DensityFunction terrain){this.macro=macro;this.terrain=terrain;}
 private double density(int x,int y,int z){return terrain.compute(new SinglePointContext(x,y,z));}
 private double macroY(int x,int z){return 128+128*macro.compute(new SinglePointContext(x,128,z));}
 private double floor(int x,int z,double h){
  int top=Math.min(248,Math.max(80,((int)Math.ceil((h+24)/8))*8));
  double above=density(x,top,z);
  if(above>0)return top;
  for(int y=top-8;y>=-32;y-=8){
   double d=density(x,y,z);
   if(d>0)return y+Math.max(0,Math.min(8,8*d/Math.max(1e-8,d-above)));
   above=d;
  }
  return -32;
 }
 private boolean nearOpenWater(int x,int z){
  for(int dx:new int[]{-12,0,12})for(int dz:new int[]{-12,0,12}){
   if(dx==0&&dz==0)continue;
   if(density(x+dx,64,z+dz)<=0&&density(x+dx,56,z+dz)<=0)return true;
  }
  return false;
 }
 @Override public double compute(FunctionContext c){
  int x=c.blockX()&~3,z=c.blockZ()&~3;
  var previous=last.get();if(previous!=null&&previous.x==x&&previous.z==z)return previous.value;
  long key=((long)x<<32)^(z&0xffffffffL);
  try{
   double value=columns.get(key,()->classify(x,z));last.set(new Last(x,z,value));return value;
  }catch(java.util.concurrent.ExecutionException e){throw new IllegalStateException("shore classification",e.getCause());}
 }
 private double classify(int x,int z){
  double h=macroY(x,z),actual=floor(x,z,h),value;
  if(actual<61)value=actual<25?-1.08:-.70;
  else if(actual<=65&&nearOpenWater(x,z))value=-.20;
  else if(actual<63)value=-.70;
  else value=Math.min(1.10,.44+Math.max(0,actual-63)*.012);
  return value;
 }
 @Override public void fillArray(double[] a,ContextProvider p){p.fillAllDirectly(a,this);}
 @Override public DensityFunction mapAll(Visitor v){return v.apply(new ShoreDensity(macro.mapAll(v),terrain.mapAll(v)));}
 @Override public double minValue(){return -1.2;}
 @Override public double maxValue(){return 1.2;}
 @Override public KeyDispatchDataCodec<? extends DensityFunction> codec(){return KeyDispatchDataCodec.of(CODEC);}
}
