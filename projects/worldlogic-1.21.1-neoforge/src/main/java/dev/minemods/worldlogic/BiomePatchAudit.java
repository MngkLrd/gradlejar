package dev.minemods.worldlogic;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;
/** Independent final chunk palette audit, including borders against loaded
 * neighbouring chunks. Reads actual biomes at the generated surface. */
final class BiomePatchAudit{
 private record Point(String id,int rank,int height){}
 private static Point at(ServerLevel l,int qx,int qz){
  int x=qx*4+2,z=qz*4+2;
  var c=l.getChunkSource().getChunkNow(x>>4,z>>4);if(c==null)return null;
  int h=c.getHeight(Heightmap.Types.OCEAN_FLOOR,x&15,z&15);
  var id=c.getNoiseBiome(qx,Math.max(h,l.getSeaLevel()-1)>>2,qz).unwrapKey().orElseThrow().location().getPath();
  return new Point(id,RegionalBiomeEdges.rank(id),h);
 }
 static String audit(ServerLevel l,BlockPos origin){
  int qx=(origin.getX()>>4)*4,qz=(origin.getZ()>>4)*4,land=0,links=0,extreme=0,isolated=0,same=0;
  var ids=new TreeSet<String>();
  int[][] steps={{1,0},{-1,0},{0,1},{0,-1}};
  for(int x=0;x<4;x++)for(int z=0;z<4;z++){
   var p=at(l,qx+x,qz+z);if(p==null)return "BIOME_PATCH unavailable";
   if(p.rank<0||p.height<l.getSeaLevel()+3)continue;
   land++;ids.add(p.id);int peers=0,matching=0;
   for(var d:steps){
    var n=at(l,qx+x+d[0],qz+z+d[1]);if(n==null)return "BIOME_PATCH unavailable";
    if(n.rank<0||n.height<l.getSeaLevel()+3)continue;
    links++;peers++;if(p.id.equals(n.id)){matching++;same++;}
    if(RegionalBiomeEdges.incompatible(p.rank,n.rank))extreme++;
   }
   if(peers==4&&matching==0)isolated++;
  }
  return "BIOME_PATCH land="+land+" links="+links+" same="+same+" extreme="+extreme+" isolated="+isolated+" classes="+ids.size();
 }
}
