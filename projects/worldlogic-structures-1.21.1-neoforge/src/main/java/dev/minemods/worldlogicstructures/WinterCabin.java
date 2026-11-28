package dev.minemods.worldlogicstructures;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BedPart;

/** Authored cabin based on the owner-reviewed coordinate mockup. Local origin is x=4,z=5. */
final class WinterCabin {
 static final int RADIUS=10, HEIGHT=13;
 private final Architecture.Plan plan=new Architecture.Plan();
 private final Block stone=Architecture.core("slate_bricks",Blocks.STONE_BRICKS);
 private void block(int x,int y,int z,Block b){plan.block(x-4,y,z-5,b);}
 private void state(int x,int y,int z,BlockState s){plan.state(x-4,y,z-5,s);}
 private void root(int x,int y,int z){plan.footing(x-4,y,z-5);}
 private void log(int x,int y,int z,Block b,Direction.Axis axis){state(x,y,z,b.defaultBlockState().setValue(BlockStateProperties.AXIS,axis));}
 private void stair(int x,int y,int z,Block b,Direction dir){state(x,y,z,b.defaultBlockState().setValue(StairBlock.FACING,dir));}
 private void pane(int x,int y,int z,boolean alongX){state(x,y,z,Blocks.GLASS_PANE.defaultBlockState().setValue(BlockStateProperties.EAST,alongX).setValue(BlockStateProperties.WEST,alongX).setValue(BlockStateProperties.NORTH,!alongX).setValue(BlockStateProperties.SOUTH,!alongX));}
 static Architecture.Plan build(){return new WinterCabin().make();}
 private Architecture.Plan make(){
  // Stone perimeter, durable posts and a repaired patch in the plank floor.
  for(int x=0;x<=8;x++)for(int z=0;z<=10;z++){
   boolean edge=x==0||x==8||z==0||z==10;
   block(x,0,z,edge?stone:(x>=5&&z>=7?Blocks.OAK_PLANKS:Blocks.SPRUCE_PLANKS));root(x,0,z);
   if(edge){block(x,-1,z,(z<=2&&x<=2)?Blocks.MOSSY_COBBLESTONE:stone);root(x,-1,z);}
   if(!edge)for(int y=1;y<10-Math.abs(x-4);y++)block(x,y,z,Blocks.AIR);
  }
  for(int y=1;y<=4;y++)for(int x=0;x<=8;x++)for(int z=0;z<=10;z++){
   if(x!=0&&x!=8&&z!=0&&z!=10)continue;
   if(z==10&&x==6&&y<=2){block(x,y,z,Blocks.AIR);continue;}
   boolean window=y==2&&((x==0||x==8)&&(z==3||z==7)||z==10&&(x==2||x==3)||z==0&&x==5);
   if(window){if(x==3&&z==10||x==0&&z==3)block(x,y,z,Blocks.AIR);else pane(x,y,z,z==0||z==10);continue;}
   boolean post=(x==0||x==8)&&(z==0||z==5||z==10);
   if(post)log(x,y,z,Blocks.SPRUCE_LOG,Direction.Axis.Y);
   else if(y==1)block(x,y,z,z==0&&x<=2?Blocks.COBBLESTONE:stone);
   else log(x,y,z,(z<=3&&x==0&&y>=3)?Blocks.SPRUCE_LOG:Blocks.STRIPPED_SPRUCE_LOG,x==0||x==8?Direction.Axis.Z:Direction.Axis.X);
  }
  // Sills, lintels and real end-gables.
  for(int x=0;x<=8;x++)for(int y=5;y<10-Math.abs(x-4);y++)for(int z:new int[]{0,10})
   log(x,y,z,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.X);
  for(int z:new int[]{4,8})for(int x=0;x<=8;x++)log(x,4,z,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.X);
  for(int z=0;z<=10;z++)if(z!=1&&z!=2)log(4,9,z,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.Z);
  // One connected roof collapse. Surviving courses and eaves remain coherent.
  for(int x=-1;x<=9;x++)for(int z=-1;z<=11;z++){
   int y=10-Math.abs(x-4);
   if(x<=2&&z>=0&&z<=3){block(x,y,z,Blocks.AIR);continue;}
   if(x==4)block(x,y,z,Blocks.SPRUCE_PLANKS);
   else stair(x,y,z,Blocks.SPRUCE_STAIRS,x<4?Direction.EAST:Direction.WEST);
   if(x==4&&(z==4||z==5||z==6||z==9))block(x,y+1,z,Blocks.SNOW);
  }
  for(int z:new int[]{4,8})for(int x=0;x<=8;x++){
   if(x==4)continue;
   log(x,9-Math.abs(x-4),z,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.X);
  }
  // Lower attached porch, roof, posts and stone approach.
  for(int x=5;x<=8;x++)for(int z=11;z<=13;z++){
   block(x,0,z,Blocks.SPRUCE_PLANKS);root(x,0,z);
   if(x==5||x==8){block(x,-1,z,stone);root(x,-1,z);}
   for(int y=1;y<=2;y++)block(x,y,z,Blocks.AIR);
  }
  for(int x:new int[]{5,8})for(int y=1;y<=3;y++)log(x,y,13,Blocks.SPRUCE_LOG,Direction.Axis.Y);
  for(int x=5;x<=8;x++)log(x,3,13,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.X);
  for(int x=4;x<=9;x++)for(int z=11;z<=14;z++){
   if(x==4&&z==14)continue;
   if(z==11||z==13)stair(x,z<=12?4:3,z,Blocks.SPRUCE_STAIRS,Direction.NORTH);
   else block(x,z<=12?4:3,z,Blocks.SPRUCE_SLAB);
  }
  for(int x:new int[]{6,7}){stair(x,-1,14,Blocks.COBBLESTONE_STAIRS,Direction.NORTH);root(x,-1,14);}
  // Hearth and continuous masonry flue. Cold ash, no fuel or loot.
  block(1,0,2,stone);
  state(1,1,2,Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING,Direction.SOUTH));
  for(int y=2;y<=11;y++)block(1,y,2,y<=3?Blocks.STONE_BRICKS:stone);
  block(1,12,2,Blocks.COBBLESTONE_SLAB);
  block(2,0,2,Blocks.STONE_BRICKS);block(1,0,3,Blocks.STONE_BRICKS);
  // Sleeping niche: actual two-block bed, low shelves and a rug.
  state(6,1,1,Blocks.BROWN_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.HEAD));
  state(6,1,2,Blocks.BROWN_BED.defaultBlockState().setValue(BedBlock.FACING,Direction.NORTH).setValue(BedBlock.PART,BedPart.FOOT));
  block(7,1,1,Blocks.OAK_PLANKS);block(7,2,1,Blocks.CANDLE);
  block(7,1,3,Blocks.SPRUCE_SLAB);block(6,1,4,Blocks.BROWN_CARPET);block(7,1,4,Blocks.BROWN_CARPET);
  // Work zone by the right wall, shelves with pottery and washing vessel.
  block(7,1,8,Blocks.CRAFTING_TABLE);block(7,1,7,Blocks.CAULDRON);
  block(7,1,9,Blocks.OAK_PLANKS);block(7,2,9,Blocks.FLOWER_POT);
  block(7,3,8,Blocks.SPRUCE_SLAB);block(7,3,9,Blocks.SPRUCE_SLAB);
  // Dining bench and table with a solid leg. Open central aisle x=3..5.
  stair(1,1,5,Blocks.SPRUCE_STAIRS,Direction.WEST);stair(1,1,6,Blocks.SPRUCE_STAIRS,Direction.WEST);
  block(2,1,5,Blocks.SPRUCE_FENCE);block(2,2,5,Blocks.OAK_PRESSURE_PLATE);
  block(2,1,6,Blocks.OAK_PLANKS);block(2,2,6,Blocks.FLOWER_POT);
  // Firewood, spare boards and localized debris beneath the damaged roof.
  log(1,1,7,Blocks.SPRUCE_LOG,Direction.Axis.Z);log(1,1,8,Blocks.SPRUCE_LOG,Direction.Axis.Z);
  log(1,2,8,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.Z);
  block(3,1,1,Blocks.COBBLESTONE_SLAB);stair(3,1,3,Blocks.SPRUCE_STAIRS,Direction.WEST);
  log(2,1,3,Blocks.SPRUCE_LOG,Direction.Axis.X);
  block(2,3,1,Blocks.COBWEB);
  block(3,0,12,Blocks.COBBLESTONE_SLAB);root(3,0,12);
  block(4,0,13,Blocks.COBBLESTONE_SLAB);root(4,0,13);
  log(-1,0,2,Blocks.SPRUCE_LOG,Direction.Axis.Z);root(-1,0,2);
  log(-1,0,3,Blocks.STRIPPED_SPRUCE_LOG,Direction.Axis.Z);root(-1,0,3);
  block(-2,-1,3,Blocks.COBBLESTONE_SLAB);root(-2,-1,3);
  return plan;
 }
}
