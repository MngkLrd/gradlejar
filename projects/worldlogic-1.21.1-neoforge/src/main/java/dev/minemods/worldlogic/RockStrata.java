package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
/** Deterministic regional lithostratigraphy. No terrain density changes. */
public final class RockStrata {
 private RockStrata(){}
 public static final int REGION=576;
 public enum Host {
  SANDSTONE(Blocks.SANDSTONE),CARBONATE(Blocks.CALCITE),VOLCANIC(Blocks.ANDESITE),
  GRANITIC(Blocks.GRANITE),DIORITIC(Blocks.DIORITE),TUFFACEOUS(Blocks.TUFF),
  BASEMENT(Blocks.STONE),DEEP_BASEMENT(Blocks.DEEPSLATE);
  public final Block block;Host(Block b){block=b;}
 }
 public record Region(int x,int z,GeologyModel.Province province,long salt){}
 public record Layer(int bottom,int top,Host host){}
 public record Column(Region region,List<Layer> layers){
  public Column{layers=List.copyOf(layers);}
  public Layer at(int y){for(var l:layers)if(y>=l.bottom&&y<=l.top)return l;throw new IllegalArgumentException("stratum outside world: "+y);}
 }
 private static double unit(long h){return (h>>>11)*0x1.0p-53;}
 public static Region region(long seed,int x,int z){
  int gx=Math.floorDiv(x,REGION),gz=Math.floorDiv(z,REGION),bx=gx,bz=gz;double best=Double.POSITIVE_INFINITY;
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
   int rx=gx+dx,rz=gz+dz;long h=GeologyModel.hash(seed^0x615c4c61796572L,rx,0,rz);
   double cx=(rx+.5)*REGION+(unit(h)-.5)*REGION*.55,cz=(rz+.5)*REGION+(unit(GeologyModel.hash(h,rx,1,rz))-.5)*REGION*.55;
   double d=(x-cx)*(x-cx)+(z-cz)*(z-cz);
   if(d<best){best=d;bx=rx;bz=rz;}
  }
  long salt=GeologyModel.hash(seed,bx,0,bz);
  return new Region(bx,bz,GeologyModel.Province.values()[(int)Math.floorMod(salt,5)],salt);
 }
 private record Stack(Host[] hosts,int[] boundaries,double angle,double phase){}
 private static Stack stack(Region r){
  var rng=new SplittableRandom(r.salt^0x14c968e2af735b1dL);
  Host[] sequence=switch(r.province){
   case SEDIMENTARY->new Host[]{Host.DEEP_BASEMENT,Host.BASEMENT,Host.SANDSTONE,Host.TUFFACEOUS,Host.SANDSTONE,Host.CARBONATE,Host.SANDSTONE,Host.BASEMENT};
   case CARBONATE->new Host[]{Host.DEEP_BASEMENT,Host.BASEMENT,Host.CARBONATE,Host.SANDSTONE,Host.CARBONATE,Host.CARBONATE,Host.TUFFACEOUS,Host.BASEMENT};
   case INTRUSIVE->new Host[]{Host.DEEP_BASEMENT,Host.GRANITIC,Host.DIORITIC,Host.GRANITIC,Host.VOLCANIC,Host.TUFFACEOUS,Host.GRANITIC,Host.BASEMENT};
   case METAMORPHIC->new Host[]{Host.DEEP_BASEMENT,Host.DIORITIC,Host.BASEMENT,Host.DIORITIC,Host.GRANITIC,Host.TUFFACEOUS,Host.DIORITIC,Host.BASEMENT};
   case CRATON->new Host[]{Host.DEEP_BASEMENT,Host.BASEMENT,Host.GRANITIC,Host.DEEP_BASEMENT,Host.DIORITIC,Host.VOLCANIC,Host.BASEMENT,Host.BASEMENT};
  };
  int[] boundary=new int[sequence.length-1];int y=-204+rng.nextInt(17);
  for(int i=0;i<boundary.length;i++){boundary[i]=y;y+=rng.nextInt(29,53);}
  return new Stack(sequence,boundary,rng.nextDouble(Math.PI*2),rng.nextDouble(Math.PI*2));
 }
 public static Column column(long seed,int x,int z){
  var region=region(seed,x,z);var s=stack(region);
  double a=x*Math.cos(s.angle)+z*Math.sin(s.angle);
  int fold=(int)Math.round(7*Math.sin(a*.008+s.phase)+5*Math.sin(z*.014+x*.006+s.phase));
  var list=new ArrayList<Layer>(s.hosts.length);int bottom=-256;
  for(int i=0;i<s.hosts.length;i++){
   int top=i<s.boundaries.length?s.boundaries[i]+fold:511;
   list.add(new Layer(bottom,top,s.hosts[i]));bottom=top+1;
  }
  return new Column(region,list);
 }
 public static Optional<Host> observed(Block block){return Arrays.stream(Host.values()).filter(h->h.block==block).findFirst();}
 static void verify(){
  var provinces=EnumSet.noneOf(GeologyModel.Province.class);var hosts=EnumSet.noneOf(Host.class);
  for(int x=-3072;x<=3072;x+=192)for(int z=-3072;z<=3072;z+=192){
   var c=column(42,x,z);provinces.add(c.region.province);require(c.equals(column(42,x,z)),"deterministic column");
   int next=-256;for(var l:c.layers){require(l.bottom==next&&l.top>=l.bottom,"no layer gaps/overlap");next=l.top+1;hosts.add(l.host);}
   require(next==512&&c.at(-256).host==Host.DEEP_BASEMENT,"complete vertical interval");
   var other=column(42,x+1,z);
   if(c.region.equals(other.region))for(int i=0;i<c.layers.size()-1;i++)require(Math.abs(c.layers.get(i).top-other.layers.get(i).top)<=1,"continuous folds in one region");
  }
  require(provinces.size()==5&&hosts.size()==8,"all rock/province classes");
  require(!column(42,0,0).equals(column(43,0,0)),"seed affects strata");
 }
 private static void require(boolean value,String label){if(!value)throw new IllegalStateException("rock strata: "+label);}
}
