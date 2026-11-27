package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.DoubleBinaryOperator;
/**
 * Independent regional drainage planner. The existing RC22 elevation and climate
 * functions are inputs, not modified. A regional graph is rooted at ocean cells;
 * selected upland sources have a complete downstream path or are not emitted.
 * Region boundaries are conservative: no speculative exits to another tile.
 */
final class RegionalDrainage{
 static final int SIZE=1536,STEP=64,N=SIZE/STEP;
 private static final int SEA=63,MAX_SOURCES=6,MAX_TRIALS=28,MAX_HEIGHT_SAMPLES=320;
 private static final double INF=Double.POSITIVE_INFINITY;
 record Node(int x,int z,double height){}
 record Hit(double distance,double fraction,double stage,double radius,double depth,double bank,double dx,double dz,double slope,int tributaries){}
 record Edge(Node from,Node to,double start,double end,int tributaries,double radius,double depth,double bank,double bend){
  double x(double u){double dx=to.x-from.x,dz=to.z-from.z;return from.x+dx*u-dz/Math.hypot(dx,dz)*bend*Math.sin(Math.PI*u);}
  double z(double u){double dx=to.x-from.x,dz=to.z-from.z;return from.z+dz*u+dx/Math.hypot(dx,dz)*bend*Math.sin(Math.PI*u);}
  Hit hit(double px,double pz){
   double best=INF,fraction=0;double ax=x(0),az=z(0);
   for(int k=1;k<=12;k++){
    double u=k/12.0,bx=x(u),bz=z(u),dx=bx-ax,dz=bz-az;
    double f=Math.max(0,Math.min(1,((px-ax)*dx+(pz-az)*dz)/(dx*dx+dz*dz)));
    double d=Math.hypot(px-ax-dx*f,pz-az-dz*f);
    if(d<best){best=d;fraction=(k-1+f)/12.0;}ax=bx;az=bz;
   }
   double a=Math.max(0,fraction-.02),b=Math.min(1,fraction+.02),dx=x(b)-x(a),dz=z(b)-z(a),len=Math.hypot(dx,dz);
   return new Hit(best,fraction,start+(end-start)*fraction,radius,depth,bank,dx/len,dz/len,(start-end)/Math.hypot(to.x-from.x,to.z-from.z),tributaries);
  }
  boolean near(double x,double z,double distance){
   double margin=distance+bank+Math.abs(bend);
   return x>=Math.min(from.x,to.x)-margin&&x<=Math.max(from.x,to.x)+margin&&z>=Math.min(from.z,to.z)-margin&&z<=Math.max(from.z,to.z)+margin;
  }
 }
 // Separate regional network from local query partitions. Immutable index:
 // query complexity depends on nearby river reaches, not total graph size.
 private static final int PART=64;
 private static long partKey(int x,int z){return ((long)x<<32)^(z&0xffffffffL);}
 record Network(List<Edge> edges,int sources,int mouths,int samples,Map<Long,List<Edge>> partitions){
  Network(List<Edge> edges,int sources,int mouths,int samples){this(List.copyOf(edges),sources,mouths,samples,index(edges));}
  private static Map<Long,List<Edge>> index(List<Edge> edges){
   var result=new HashMap<Long,List<Edge>>();
   for(var e:edges){
    double margin=e.bank+Math.abs(e.bend)+2;
    int x0=(int)Math.floor((Math.min(e.from.x,e.to.x)-margin)/PART),x1=(int)Math.floor((Math.max(e.from.x,e.to.x)+margin)/PART);
    int z0=(int)Math.floor((Math.min(e.from.z,e.to.z)-margin)/PART),z1=(int)Math.floor((Math.max(e.from.z,e.to.z)+margin)/PART);
    for(int z=z0;z<=z1;z++)for(int x=x0;x<=x1;x++)result.computeIfAbsent(partKey(x,z),k->new ArrayList<>()).add(e);
   }
   result.replaceAll((k,v)->List.copyOf(v));return Map.copyOf(result);
  }
  List<Edge> candidates(double x,double z){return partitions.getOrDefault(partKey((int)Math.floor(x/PART),(int)Math.floor(z/PART)),List.of());}
  Hit nearest(double x,double z){return nearestFrom(candidates(x,z),x,z);}
  private static Hit nearestFrom(List<Edge> edges,double x,double z){
   Hit best=null;double score=INF;
   for(var e:edges){if(!e.near(x,z,0))continue;var h=e.hit(x,z);double s=h.distance/h.radius;if(h.distance<=h.bank&&s<score){score=s;best=h;}}
   return best;
  }
 }
 private record Visit(int index,double cost){}
 private static long hash(int x,int z){
  long n=((long)x<<32)^(z&0xffffffffL);n=(n^(n>>>30))*0xbf58476d1ce4e5b9L;n=(n^(n>>>27))*0x94d049bb133111ebL;return n^(n>>>31);
 }
 private static double unit(long h){return (h>>>11)*0x1.0p-53;}
 private static double bend(Node a,Node b){return (unit(hash(a.x^b.z,a.z^b.x))-.5)*18;}
 private static double clamp(double x,double a,double b){return Math.max(a,Math.min(b,x));}
 private static double radius(int branches){return Math.min(13,3.8+3.1*Math.sqrt(branches));}
 private static double depth(int branches){return Math.min(8,3.5+1.6*Math.sqrt(branches));}
 private static double bank(int branches){return radius(branches)*3.2;}
 static Network build(int rx,int rz,DoubleBinaryOperator macro,DoubleBinaryOperator actual){
  Node[] nodes=new Node[N*N];
  double[] cost=new double[nodes.length],stage=new double[nodes.length];
  int[] parent=new int[nodes.length],hops=new int[nodes.length],flow=new int[nodes.length];
  Arrays.fill(cost,INF);Arrays.fill(stage,Double.NaN);Arrays.fill(parent,-1);
  int x0=rx*SIZE,z0=rz*SIZE;
  for(int z=0;z<N;z++)for(int x=0;x<N;x++){
   int gx=rx*N+x,gz=rz*N+z;long h=hash(gx,gz);
   int wx=x0+x*STEP+STEP/2+(int)Math.round((unit(h)-.5)*34),wz=z0+z*STEP+STEP/2+(int)Math.round((unit(hash(gz,gx^0x639a))-.5)*34);
   nodes[z*N+x]=new Node(wx,wz,macro.applyAsDouble(wx,wz));
  }
  var queue=new PriorityQueue<Visit>(Comparator.comparingDouble(Visit::cost).thenComparingInt(Visit::index));
  // Root only at genuine macro water inside the region. Reserve an outer cell
  // as a dry seam guard; no branch can terminate at a tile border by accident.
  for(int z=1;z<N-1;z++)for(int x=1;x<N-1;x++){
   int i=z*N+x;
   if(nodes[i].height<=SEA-1){cost[i]=0;queue.add(new Visit(i,0));}
  }
  while(!queue.isEmpty()){
   var v=queue.poll();int i=v.index;if(v.cost!=cost[i])continue;
   int x=i%N,z=i/N;var down=nodes[i];
   for(int dz=-1;dz<=1;dz++)for(int dx=-1;dx<=1;dx++){
    int xx=x+dx,zz=z+dz;if(dx==0&&dz==0||xx<1||zz<1||xx>=N-1||zz>=N-1)continue;
    int k=zz*N+xx;var up=nodes[k];if(up.height<=SEA-1)continue;
    double length=Math.hypot(up.x-down.x,up.z-down.z),rise=up.height-down.height;
    // Prefer valleys and gentle upstream ascent; allow mild local depressions
    // but make steep divides and reverse slopes expensive.
    double weight=length*(1+.022*Math.max(0,rise)+.12*Math.max(0,-rise))+
     12*unit(hash(up.x,down.z));
    double next=cost[i]+weight;
    if(next<cost[k]){cost[k]=next;parent[k]=i;hops[k]=hops[i]+1;queue.add(new Visit(k,next));}
   }
  }
  var candidates=new ArrayList<Integer>();
  for(int i=0;i<nodes.length;i++)if(hops[i]>=4&&hops[i]<=22&&nodes[i].height>=SEA+6)candidates.add(i);
  candidates.sort(Comparator.comparingDouble((Integer i)->-(hops[i]*45+Math.min(90,nodes[i].height-SEA)*1.6+unit(hash(nodes[i].x,nodes[i].z))*55)).thenComparingInt(i->i));
  Map<Long,Double> heights=new HashMap<>();
  DoubleBinaryOperator measured=(x,z)->{
   int xx=(int)Math.round(x),zz=(int)Math.round(z);long key=((long)xx<<32)^(zz&0xffffffffL);
   var existing=heights.get(key);if(existing!=null)return existing;
   if(heights.size()>=MAX_HEIGHT_SAMPLES)return Double.NaN;
   double h=actual.applyAsDouble(xx,zz);heights.put(key,h);return h;
  };
  var sources=new ArrayList<Integer>();var mouths=new HashSet<Integer>();
  int trials=0;
  for(int source:candidates){
   if(sources.size()>=MAX_SOURCES||trials>=MAX_TRIALS||heights.size()>=MAX_HEIGHT_SAMPLES)break;
   var node=nodes[source];
   if(sources.stream().anyMatch(i->Math.hypot(nodes[i].x-node.x,nodes[i].z-node.z)<192))continue;
   var path=new ArrayList<Integer>();int k=source;
   while(k>=0){path.add(k);k=parent[k];}
   int mouth=path.getLast();
   if(nodes[mouth].height>SEA-1)continue;
   // A selected tributary may join an existing trunk. Independent mouths
   // must be separated, preventing a fan of channels at every coastal cell.
   if(!mouths.contains(mouth)&&mouths.stream().anyMatch(i->Math.hypot(nodes[i].x-nodes[mouth].x,nodes[i].z-nodes[mouth].z)<176))continue;
   trials++;
   double root=measured.applyAsDouble(nodes[mouth].x,nodes[mouth].z);
   if(!Double.isFinite(root)||root>SEA-1)continue;
   var proposed=new HashMap<Integer,Double>();proposed.put(mouth,(double)SEA-1);
   boolean valid=true;double previous=SEA-1;
   for(int at=path.size()-2;at>=0;at--){
    int u=path.get(at),v=path.get(at+1);var from=nodes[u];var to=nodes[v];
    if(flow[u]>0){previous=stage[u];continue;}
    double actualFrom=measured.applyAsDouble(from.x,from.z);
    double length=Math.hypot(from.x-to.x,from.z-to.z);
    double start=clamp(actualFrom-2,previous,previous+length*.23);
    if(!Double.isFinite(actualFrom)||actualFrom<start-1||actualFrom-start>15){valid=false;break;}
    var edge=new Edge(from,to,start,previous,1,radius(1),depth(1),bank(1),bend(from,to));
    for(double f:new double[]{.25,.5,.75}){
     double h=measured.applyAsDouble(edge.x(f),edge.z(f)),water=start+(previous-start)*f;
     if(!Double.isFinite(h)||h<water-1.5&&!(v==mouth&&h<=SEA-1&&water<=SEA+1)||h-water>15){valid=false;break;}
    }
    if(!valid)break;
    proposed.put(u,start);previous=start;
   }
   if(!valid)continue;
   sources.add(source);mouths.add(mouth);
   proposed.forEach((i,y)->stage[i]=y);
   for(int i:path)flow[i]++;
  }
  var edges=new ArrayList<Edge>();
  for(int i=0;i<nodes.length;i++){
   int k=parent[i];if(flow[i]==0||k<0)continue;
   double r=radius(flow[i]),d=depth(flow[i]),b=bank(flow[i]);
   // Steep reaches retain a narrower bed; broad rivers get wider floodplains.
   double slope=(stage[i]-stage[k])/Math.hypot(nodes[i].x-nodes[k].x,nodes[i].z-nodes[k].z);
   if(slope>.12){r*=.83;b*=.8;}
   edges.add(new Edge(nodes[i],nodes[k],stage[i],stage[k],flow[i],r,d,b,bend(nodes[i],nodes[k])));
  }
  return new Network(edges,sources.size(),mouths.size(),heights.size());
 }
 static void verify(){
  // A 1536-block region with a coastal strip, sloping inland catchment,
  // and a slight valley. Same inputs must yield the exact same network.
  DoubleBinaryOperator coast=(x,z)->x<180?48:62+(x-180)*.055+3*Math.sin(z*.004);
  var a=build(0,0,coast,coast);var b=build(0,0,coast,coast);
  require(a.equals(b)&&a.sources>0&&a.mouths>0&&!a.edges.isEmpty(),"deterministic coastal network");
  var byFrom=new HashMap<Node,Edge>();for(var e:a.edges){require(byFrom.put(e.from,e)==null,"unique downstream edge");require(e.start>=e.end&&e.depth>=4&&e.bank>e.radius*2,"grade and floodplain");}
  for(var e:a.edges){
   var seen=new HashSet<Node>();var n=e.from;Edge next;
   while((next=byFrom.get(n))!=null){require(seen.add(n),"no loops");n=next.to;}
   require(n.height<=SEA-1,"every route ends at ocean");
  }
  require(build(0,0,(x,z)->100,(x,z)->100).edges.isEmpty(),"no unverified inland outlet");
  require(build(0,0,coast,(x,z)->200).edges.isEmpty(),"actual terrain veto");
  require(a.samples<=MAX_HEIGHT_SAMPLES,"bounded physical samples");
  for(int x=-32;x<=SIZE+32;x+=13)for(int z=-32;z<=SIZE+32;z+=17)require(Objects.equals(a.nearest(x,z),Network.nearestFrom(a.edges,x,z)),"partition exactness");
  var edge=a.edges.getFirst();var hit=edge.hit(edge.x(.5),edge.z(.5));
  require(hit.distance<.05&&hit.fraction>.4&&hit.fraction<.6,"centerline projection");
 }
 private static void require(boolean v,String s){if(!v)throw new IllegalStateException("regional drainage: "+s);}
}
