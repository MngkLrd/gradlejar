package dev.minemods.worldlogicterrain.forest;

import dev.minemods.worldlogicterrain.registry.TerrainContent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.LinkedHashMap;

/** Real block placements, support removal, loot and the bounded vine growth path. */
public final class ForestChecks {
    private static void require(boolean v,String msg){if(!v)throw new IllegalStateException("Forest: "+msg);}
    public static void verify(ServerLevel l){
        var p=new BlockPos(10,260,10);var saved=new LinkedHashMap<BlockPos,BlockState>();
        for(int y=-5;y<=1;y++){var q=p.above(y);saved.put(q,l.getBlockState(q));for(var d:Direction.Plane.HORIZONTAL)saved.put(q.relative(d),l.getBlockState(q.relative(d)));}
        try{
            require(ForestEcology.epiphyteHabitat(12,.7,.2)&&!ForestEcology.epiphyteHabitat(12,.1,.9)&&!ForestEcology.vineHabitat(-12,.8,.2),"habitat");
            require(ForestSpecies.root("pine")==ForestSpecies.CONIFER_ROOT&&ForestSpecies.root("birch")==ForestSpecies.PALE_ROOT&&ForestSpecies.root("oak")==ForestSpecies.ROOT,"root identity");
            for(var sp:ForestSpecies.values()){
                var b=(ForestBlock)sp.block();var item=b.asItem();
                require(TerrainContent.allItems().stream().anyMatch(x->x.get()==item),"creative registration "+sp.id);
                for(var facing:Direction.Plane.HORIZONTAL){
                    var host=sp.wall()?p.relative(facing):p.below();
                    l.setBlock(p,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    l.setBlock(host,sp.wall()?Blocks.OAK_PLANKS.defaultBlockState():Blocks.GLASS.defaultBlockState(),Block.UPDATE_ALL);
                    require(!ForestEcology.place(l,p,sp,facing,0,0,0),"invalid support "+sp.id);
                    l.setBlock(host,sp.wall()?Blocks.OAK_LOG.defaultBlockState():Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
                    require(ForestEcology.place(l,p,sp,facing,0,0,0),"placement "+sp.id);
                    require(!ForestEcology.place(l,p,sp,facing,0,0,0),"overwrote occupant "+sp.id);
                    var state=l.getBlockState(p);require(state.getValue(ForestBlock.FACING)==facing&&state.canSurvive(l,p),"orientation "+sp.id);
                    var loot=Block.getDrops(state,l,p,null,null,ItemStack.EMPTY);
                    require(loot.size()==1&&loot.getFirst().is(item),"loot "+sp.id);
                    l.setBlock(host,Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                    require(l.getBlockState(p).isAir(),"support removal "+sp.id);
                }
                require(!ForestEcology.place(l,p,sp,Direction.NORTH,0,16,0),"cross chunk write "+sp.id);
            }
            var b=(ForestBlock)ForestSpecies.IVY.block();
            for(int y=0;y<5;y++){l.setBlock(p.below(y),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);l.setBlock(p.below(y).north(),Blocks.OAK_LOG.defaultBlockState(),Block.UPDATE_ALL);}
            require(ForestEcology.place(l,p,ForestSpecies.IVY,Direction.NORTH,0,0,0),"vine fixture");
            for(int i=0;i<3;i++)require(b.growDown(l,p.below(i),l.getBlockState(p.below(i))),"vine growth "+i);
            require(!b.growDown(l,p.below(3),l.getBlockState(p.below(3)))&&l.getBlockState(p.below(4)).isAir(),"vine cap");
            require(ForestSpecies.THORN.contactDamage==1&&ForestSpecies.THORN.movement<ForestSpecies.NETTLE.movement&&ForestSpecies.NEEDLES.contactDamage==0,"contact policy");
        }finally{
            saved.entrySet().stream().sorted(java.util.Comparator.comparingInt(x->x.getKey().getY())).forEach(x->l.setBlock(x.getKey(),x.getValue(),Block.UPDATE_ALL));
        }
    }
}
