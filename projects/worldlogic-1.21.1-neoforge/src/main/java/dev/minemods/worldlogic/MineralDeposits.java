package dev.minemods.worldlogic;
import java.util.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.util.concurrent.ExecutionException;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Block;
import static dev.minemods.worldlogic.RockStrata.Host.*;
/** Independent regional mineral deposits. Geometry, host eligibility, grade
 * and placement are distinct, inspired by data-oriented TFC vein families.
 * Existing vanilla ore bodies and survival pockets keep precedence. */
public final class MineralDeposits{
 private MineralDeposits(){}
 public enum Shape { CLUSTER,DISC,PIPE }
 public enum Mineral{
  CASSITERITE("tin",Shape.CLUSTER,-155,50,16,10,9,.31,.52,Set.of(GRANITIC,DIORITIC)),
  GALENA("lead",Shape.PIPE,-155,50,7,5,21,.36,.48,Set.of(CARBONATE,SANDSTONE)),
  SPHALERITE("zinc",Shape.CLUSTER,-135,65,17,11,8,.35,.48,Set.of(CARBONATE,SANDSTONE)),
  GARNIERITE("nickel",Shape.CLUSTER,-45,85,14,9,7,.32,.55,Set.of(VOLCANIC,DIORITIC)),
  GRAPHITE("graphite",Shape.DISC,-195,45,24,14,3,.52,.56,Set.of(DIORITIC,BASEMENT,DEEP_BASEMENT)),
  HALITE("salt",Shape.DISC,-100,55,24,18,3,.58,.52,Set.of(SANDSTONE,CARBONATE)),
  SULFUR("sulfur",Shape.PIPE,-90,70,7,6,16,.39,.45,Set.of(VOLCANIC,TUFFACEOUS));
  public final String resource;public final Shape shape;public final int minY,maxY;
  final double along,across,vertical,density,chance;public final Set<RockStrata.Host> hosts;
  Mineral(String resource,Shape shape,int minY,int maxY,double a,double b,double v,double density,double chance,Set<RockStrata.Host> hosts){
   this.resource=resource;this.shape=shape;this.minY=minY;this.maxY=maxY;along=a;across=b;vertical=v;this.density=density;this.chance=chance;this.hosts=Set.copyOf(hosts);
  }
  public String id(){return name().toLowerCase(Locale.ROOT);}
  public boolean host(Block block){for(var h:hosts)if(h.block==block)return true;return false;}
  boolean province(GeologyModel.Province p){return switch(this){
   case GRAPHITE->p==GeologyModel.Province.METAMORPHIC||p==GeologyModel.Province.CRATON;
   case HALITE->p==GeologyModel.Province.CARBONATE||p==GeologyModel.Province.SEDIMENTARY;
   case GARNIERITE,CASSITERITE->p==GeologyModel.Province.INTRUSIVE||p==GeologyModel.Province.METAMORPHIC||p==GeologyModel.Province.CRATON;
   default->true;
  };}
 }
 public record Deposit(Mineral mineral,double x,double y,double z,double along,double across,double vertical,double angle,double dip,long salt){
  public int minY(){return (int)Math.floor(y-vertical-Math.abs(dip)*along-2);}
  public int maxY(){return (int)Math.ceil(y+vertical+Math.abs(dip)*along+2);}
  public double reach(){return Math.max(along,across)+5;}
  public double score(int px,int py,int pz){
   double dx=px-x,dz=pz-z,a=dx*Math.cos(angle)+dz*Math.sin(angle),b=-dx*Math.sin(angle)+dz*Math.cos(angle);
   double h=py-y-a*dip;
   return switch(mineral.shape){
    case DISC->a*a/(along*along)+b*b/(across*across)+Math.pow((h-1.2*Math.sin(a*.12+salt%11))/vertical,2);
    case CLUSTER->Math.min(a*a/(along*along)+b*b/(across*across)+h*h/(vertical*vertical),
      Math.pow((a-along*.35)/(along*.60),2)+Math.pow((b+across*.16)/(across*.75),2)+Math.pow((h-vertical*.15)/(vertical*.72),2));
    case PIPE->{double t=h/vertical;double taper=1-.33*Math.min(1,Math.abs(t));double side=a-2.0*Math.sin(t*2+salt%5),cross=b-1.7*Math.sin(t*3);
     yield Math.pow(side/(along*taper),2)+Math.pow(cross/(across*taper),2)+t*t;}
   };
  }
  /** Grade 0 = no ore; 1,2,3 = poor/normal/rich. Rich blocks favour deposit interiors. */
  public int grade(int px,int py,int pz){
   if(py<mineral.minY||py>mineral.maxY)return 0;
   double s=score(px,py,pz);if(s>1)return 0;
   double occupancy=mineral.density*(1-.54*s);
   if(GeologyModel.unit(GeologyModel.hash(salt,px,py,pz))>=occupancy)return 0;
   double quality=GeologyModel.unit(GeologyModel.hash(salt^0x6a37ccad1205L,px,py,pz))+.55*(1-s);
   return quality>1.10?3:quality>.56?2:1;
  }
 }
 private record DistrictKey(long seed,int x,int z){}
 private static final Cache<DistrictKey,List<Deposit>> CACHE=CacheBuilder.newBuilder().maximumSize(4096).build();
 public static List<Deposit> district(long seed,int gx,int gz){
  var key=new DistrictKey(seed,gx,gz);
  try{return CACHE.get(key,()->build(seed,gx,gz));}catch(ExecutionException e){throw new IllegalStateException("mineral district",e.getCause());}
 }
 private static List<Deposit> build(long seed,int gx,int gz){
  var out=new ArrayList<Deposit>();
  for(var m:Mineral.values()){
   var r=new SplittableRandom(GeologyModel.hash(seed^0x14cf94aa823dL,gx,m.ordinal()+31,gz));
   if(r.nextDouble()>=m.chance)continue;
   int x=gx*GeologyModel.DISTRICT+r.nextInt(32,160),z=gz*GeologyModel.DISTRICT+r.nextInt(32,160);
   var column=RockStrata.column(seed,x,z);if(!m.province(column.region().province()))continue;
   var possible=new ArrayList<int[]>();int length=0;
   for(var layer:column.layers()){
    if(!m.hosts.contains(layer.host()))continue;
    int lo=Math.max(m.minY,layer.bottom())+2,hi=Math.min(m.maxY,layer.top())-2;
    if(lo<=hi){possible.add(new int[]{lo,hi});length+=hi-lo+1;}
   }
   if(length==0)continue;
   int pick=r.nextInt(length),y=0;
   for(var range:possible){int n=range[1]-range[0]+1;if(pick<n){y=range[0]+pick;break;}pick-=n;}
   double size=r.nextDouble(.78,1.15),a=m.along*size,b=m.across*size,v=m.vertical*r.nextDouble(.85,1.1);
   // Cap material abundance at game scale while preserving distinct aspect ratios.
   double volume=4*Math.PI/3*a*b*v*m.density*.68;
   double scale=Math.min(1,Math.cbrt(225/Math.max(1,volume)));
   out.add(new Deposit(m,x,y,z,a*scale,b*scale,v*scale,r.nextDouble(2*Math.PI),m.shape==Shape.DISC?r.nextDouble(-.12,.12):r.nextDouble(-.08,.08),r.nextLong()));
  }
  return List.copyOf(out);
 }
 static void mineralize(long seed,ChunkAccess chunk){
  int x=chunk.getPos().getMinBlockX(),z=chunk.getPos().getMinBlockZ();
  int gx=Math.floorDiv(x+8,GeologyModel.DISTRICT),gz=Math.floorDiv(z+8,GeologyModel.DISTRICT);
  for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++)for(var d:district(seed,gx+dx,gz+dz))place(chunk,d);
 }
 static void place(ChunkAccess c,Deposit d){
  int x0=c.getPos().getMinBlockX(),z0=c.getPos().getMinBlockZ();
  int minX=Math.max(x0,(int)Math.floor(d.x-d.reach())),maxX=Math.min(x0+15,(int)Math.ceil(d.x+d.reach()));
  int minZ=Math.max(z0,(int)Math.floor(d.z-d.reach())),maxZ=Math.min(z0+15,(int)Math.ceil(d.z+d.reach()));
  var pos=new BlockPos.MutableBlockPos();
  for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
   int surface=c.getHeight(Heightmap.Types.OCEAN_FLOOR_WG,x&15,z&15);
   int low=Math.max(c.getMinBuildHeight()+8,d.minY()),high=Math.min(surface-6,Math.min(c.getMaxBuildHeight()-1,d.maxY()));
   for(int y=low;y<=high;y++){
    int grade=d.grade(x,y,z);if(grade==0)continue;
    pos.set(x,y,z);if(!d.mineral.host(c.getBlockState(pos).getBlock()))continue;
    c.setBlockState(pos,MineralContent.state(d.mineral,grade),false);
   }
  }
 }
}
