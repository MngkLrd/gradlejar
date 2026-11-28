package dev.minemods.worldlogicstructures;

import dev.minemods.worldlogicstructures.api.StructuresApi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import java.util.*;

final class Architecture {
 private Architecture(){}
 record Palette(Block foundation,Block masonry,Block wall,Block frame,Block floor,Block stairs,Block slab,Block wear){}
 static Block core(String id,Block fallback){
  var key=ResourceLocation.fromNamespaceAndPath("worldlogic",id);
  return BuiltInRegistries.BLOCK.containsKey(key)?BuiltInRegistries.BLOCK.get(key):fallback;
 }
 static Palette palette(Family f){
  Block slate=core("slate_bricks",Blocks.STONE_BRICKS),gneiss=core("gneiss_bricks",Blocks.STONE_BRICKS),lime=core("limestone_bricks",Blocks.STONE_BRICKS);
  return switch(f){
   case COLD->new Palette(slate,slate,Blocks.SPRUCE_PLANKS,Blocks.SPRUCE_LOG,Blocks.SPRUCE_PLANKS,Blocks.SPRUCE_STAIRS,Blocks.SPRUCE_SLAB,Blocks.COBBLESTONE);
   case BOREAL->new Palette(slate,slate,Blocks.SPRUCE_PLANKS,Blocks.STRIPPED_SPRUCE_LOG,Blocks.SPRUCE_PLANKS,Blocks.DARK_OAK_STAIRS,Blocks.DARK_OAK_SLAB,Blocks.MOSSY_COBBLESTONE);
   case TEMPERATE->new Palette(lime,lime,Blocks.WHITE_TERRACOTTA,Blocks.STRIPPED_OAK_LOG,Blocks.OAK_PLANKS,Blocks.BRICK_STAIRS,Blocks.BRICK_SLAB,Blocks.MOSSY_COBBLESTONE);
   case ARID->new Palette(Blocks.SANDSTONE,Blocks.CUT_SANDSTONE,Blocks.PACKED_MUD,Blocks.STRIPPED_ACACIA_LOG,Blocks.SMOOTH_SANDSTONE,Blocks.SANDSTONE_STAIRS,Blocks.SANDSTONE_SLAB,Blocks.SANDSTONE);
   case TROPICAL->new Palette(lime,lime,Blocks.JUNGLE_PLANKS,Blocks.STRIPPED_JUNGLE_LOG,Blocks.JUNGLE_PLANKS,Blocks.JUNGLE_STAIRS,Blocks.JUNGLE_SLAB,Blocks.MOSSY_COBBLESTONE);
   case WETLAND->new Palette(lime,lime,Blocks.MANGROVE_PLANKS,Blocks.MANGROVE_LOG,Blocks.MANGROVE_PLANKS,Blocks.MANGROVE_STAIRS,Blocks.MANGROVE_SLAB,Blocks.MOSSY_COBBLESTONE);
   case ALPINE->new Palette(gneiss,gneiss,gneiss,Blocks.STRIPPED_SPRUCE_LOG,Blocks.SPRUCE_PLANKS,Blocks.SPRUCE_STAIRS,Blocks.SPRUCE_SLAB,Blocks.COBBLESTONE);
   case COAST->new Palette(lime,lime,Blocks.OAK_PLANKS,Blocks.STRIPPED_SPRUCE_LOG,Blocks.SPRUCE_PLANKS,Blocks.SPRUCE_STAIRS,Blocks.SPRUCE_SLAB,Blocks.MOSSY_COBBLESTONE);
  };
 }
 static BlockPos transform(int x,int y,int z,Rotation r){
  return switch(r){case NONE->new BlockPos(x,y,z);case CLOCKWISE_90->new BlockPos(-z,y,x);case CLOCKWISE_180->new BlockPos(-x,y,-z);case COUNTERCLOCKWISE_90->new BlockPos(z,y,-x);};
 }
 static int maxHeight(Design d){return d.wallHeight()+d.halfWidth()+4;}
 static final class Plan {
  final Map<BlockPos,BlockState> cells=new LinkedHashMap<>();
  final Set<BlockPos> footings=new LinkedHashSet<>();
  void block(int x,int y,int z,Block b){state(x,y,z,b.defaultBlockState());}
  void state(int x,int y,int z,BlockState s){cells.put(new BlockPos(x,y,z),s);}
  void air(int x,int y,int z){block(x,y,z,Blocks.AIR);}
  void footing(int x,int y,int z){footings.add(new BlockPos(x,y,z));}
 }
 static void stair(Plan a,Palette p,int x,int y,int z,Direction dir){
  a.state(x,y,z,p.stairs().defaultBlockState().setValue(StairBlock.FACING,dir));
 }
 static boolean edge(int x,int z,int w,int l){return Math.abs(x)==w||Math.abs(z)==l;}
 static boolean corner(int x,int z,int w,int l){return Math.abs(x)==w&&Math.abs(z)==l;}
 static void beam(Plan a,Palette p,int x,int y,int z,Direction.Axis axis){
  var state=p.frame().defaultBlockState();
  if(state.hasProperty(BlockStateProperties.AXIS))state=state.setValue(BlockStateProperties.AXIS,axis);
  a.state(x,y,z,state);
 }
 static Plan build(Design d,long seed){
  Palette p=palette(d.family());Plan a=new Plan();
  int w=d.halfWidth(),l=d.halfDepth(),h=d.wallHeight();
  boolean left=(seed&1)==0;
  if(d.kind()==Kind.PLATFORM){
   for(int x=-w;x<=w;x++)for(int z=-l;z<=l;z++){
    if(z<=-l+1&&x==(left?-w:w))continue;
    a.block(x,0,z,p.floor());
    if((Math.abs(x)==w&&Math.abs(z)%2==0)||x==0&&z==0){a.block(x,-1,z,p.frame());a.footing(x,-1,z);}
    if(Math.abs(x)==w&&z>=0)a.block(x,1,z,p.frame());
   }
   approach(a,p,d);
   return a;
  }
  // Foundations are rigid, with separate bounded support columns under raised floors.
  for(int x=-w;x<=w;x++)for(int z=-l;z<=l;z++){
   boolean ruin=d.kind()==Kind.RUIN;
   if(!ruin||edge(x,z,w,l)||Math.abs(x)<=1&&z>=0){
    a.block(x,0,z,ruin?p.foundation():p.floor());
    if(d.raised()==0)a.footing(x,0,z);
   }
   if(d.raised()>0&&(corner(x,z,w,l)||x==0&&Math.abs(z)==l||z==0&&Math.abs(x)==w||x==0&&z==0)){
    a.block(x,-1,z,p.frame());a.footing(x,-1,z);
   }
  }
  if(d.raised()>0){
   for(int x=-w;x<=w;x++){beam(a,p,x,-1,-l,Direction.Axis.X);beam(a,p,x,-1,l,Direction.Axis.X);}
   for(int z=-l+1;z<l;z++){beam(a,p,-w,-1,z,Direction.Axis.Z);beam(a,p,w,-1,z,Direction.Axis.Z);}
  }
  if(d.kind()==Kind.RUIN){
   // A connected fallen corner, not random holes through surviving supports.
   for(int x=-w;x<=w;x++)for(int z=-l;z<=l;z++)if(edge(x,z,w,l)){
    boolean collapse=(left?x<0:x>0)&&z>0;
    int height=collapse?1:Math.max(1,h-(Math.abs(x+z)%3==0?1:0));
    if(x==0&&z==l)continue;
    for(int y=1;y<=height;y++)a.block(x,y,z,y==1?p.wear():p.masonry());
   }
   int debris=left?-w+1:w-1;
   a.block(debris,1,l-1,p.wear());a.block(debris,1,l-2,p.masonry());
   a.block(-debris,1,-l+1,Blocks.CRAFTING_TABLE);
   approach(a,p,d);
   return a;
  }
  boolean tower=d.kind()==Kind.TOWER;
  boolean stoneTower=tower&&d.family()!=Family.BOREAL&&d.family()!=Family.TROPICAL;
  for(int y=1;y<=h;y++)for(int x=-w;x<=w;x++)for(int z=-l;z<=l;z++){
   if(!edge(x,z,w,l)){a.air(x,y,z);continue;}
   if(z==l&&x==0&&y<=2){a.air(x,y,z);continue;}
   boolean window=(tower?(y==3||y==6):y==2)&&(Math.abs(x)==w&&z==0||z==-l&&Math.abs(x)==1);
   if(window){a.air(x,y,z);continue;}
   boolean post=corner(x,z,w,l)||!tower&&Math.abs(x)==w&&z==0&&y!=2;
   Block material=stoneTower?p.masonry():post?p.frame():p.wall();
   if(y==1&&d.raised()==0&&(!post||stoneTower))material=p.foundation();
   // Localized damp/collapsed patch at the rear corner; preserve support posts and door.
   if(y==2&&z==-l&&x==(left?-w+1:w-1)&&!post&&!tower)material=p.wear();
   a.block(x,y,z,material);
  }
  if(!stoneTower){
   for(int x=-w;x<=w;x++){beam(a,p,x,h,-l,Direction.Axis.X);beam(a,p,x,h,l,Direction.Axis.X);}
   for(int z=-l+1;z<l;z++){beam(a,p,-w,h,z,Direction.Axis.Z);beam(a,p,w,h,z,Direction.Axis.Z);}
  }
  if(tower){
   for(int x=-w+1;x<w;x++)for(int z=-l+1;z<l;z++)if(!(x==0&&z==-l+1))a.block(x,4,z,p.floor());
   // Ladder faces south; continuous backing wall at north, no blocked floor hatch.
   for(int y=1;y<=h;y++)a.state(0,y,-l+1,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.SOUTH));
   a.block(w-1,1,-l+1,Blocks.CRAFTING_TABLE);
   a.block(-w+1,5,l-1,p.floor());
  }else{
   a.block(-w+1,1,-l+1,Blocks.FURNACE);
   a.block(w-1,1,-l+1,Blocks.CRAFTING_TABLE);
   a.block(-w+1,1,l-1,p.slab());
   a.block(w-1,1,0,Blocks.BROWN_WOOL);
   a.block(w-1,1,-1,Blocks.BROWN_WOOL);
   if(w>2){a.block(0,1,-l+1,p.floor());a.block(0,2,-l+1,Blocks.COBWEB);}
  }
  roof(a,p,d,left);
  if(!tower&&(d.family()==Family.COLD||d.family()==Family.BOREAL||d.family()==Family.ALPINE||d.family()==Family.TEMPERATE)){
   int cx=-w+1,cz=-l+1,top=h+(w+1-Math.abs(cx))+2;
   for(int y=2;y<=top;y++)a.block(cx,y,cz,p.masonry());
   a.block(cx,top+1,cz,p.slab());
  }
  approach(a,p,d);
  return a;
 }
 static void roof(Plan a,Palette p,Design d,boolean left){
  int w=d.halfWidth(),l=d.halfDepth(),h=d.wallHeight();
  if(d.roof()==Roof.FLAT){
   int roof=h+1;
   for(int x=-w;x<=w;x++)for(int z=-l;z<=l;z++){
    if(d.kind()==Kind.TOWER&&x==0&&z==-l+1)continue;
    if(d.kind()!=Kind.TOWER&&z==-l+1&&x==(left?-w+1:w-1))continue;
    a.block(x,roof,z,p.floor());
   }
   if(d.kind()==Kind.TOWER)a.state(0,roof,-l+1,Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.SOUTH));
   for(int x=-w;x<=w;x++)for(int z=-l;z<=l;z++)if(edge(x,z,w,l)&&(d.kind()!=Kind.TOWER||(x+z)%2==0||corner(x,z,w,l)))a.block(x,roof+1,z,p.masonry());
   return;
  }
  if(d.roof()==Roof.SHED){
   for(int x=-w-1;x<=w+1;x++)for(int z=-l-1;z<=l+1;z++){
    int y=h+1+(x+w+1)/2;
    if(!(z==-l&&x==w))a.block(x,y,z,p.slab());
    if(Math.abs(z)==l&&Math.abs(x)<=w)for(int k=h+1;k<y;k++)a.block(x,k,z,p.wall());
    if(x==w&&Math.abs(z)<=l)for(int k=h+1;k<y;k++)a.block(x,k,z,p.wall());
   }
   return;
  }
  for(int x=-w-1;x<=w+1;x++){
   int y=h+1+w+1-Math.abs(x);
   for(int z=-l-1;z<=l+1;z++){
    boolean fallen=d.kind()!=Kind.TOWER&&z==-l&&x==(left?-w:w);
    if(!fallen){if(x==0)a.block(x,y,z,p.slab());else stair(a,p,x,y,z,x<0?Direction.EAST:Direction.WEST);}
    if(Math.abs(z)==l&&Math.abs(x)<=w)for(int k=h+1;k<y;k++)a.block(x,k,z,p.wall());
   }
  }
 }
 static void approach(Plan a,Palette p,Design d){
  int l=d.halfDepth(),rise=d.raised();
  // A short supported stair run facing the entrance; never a floating threshold.
  for(int i=1;i<=rise+1;i++){
   int y=1-i;
   stair(a,p,0,y,l+i,Direction.NORTH);a.footing(0,y,l+i);
  }
  a.block(0,-rise-1,l+rise+2,p.foundation());a.footing(0,-rise-1,l+rise+2);
 }
}
