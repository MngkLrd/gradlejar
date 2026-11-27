package dev.minemods.worldlogicterrain.worldgen;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TerrainPatchChecks {
    private static final Set<String> MEADOW=Set.of("meadow_grass","dense_meadow_grass","wild_clover","wild_chamomile","wild_cornflower");
    private TerrainPatchChecks(){}
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("patch_check").executes(c->{
            try{
                check(c.getSource().getLevel());
                dev.minemods.worldlogicterrain.tree.TreeChecks.verify(c.getSource().getLevel());
                c.getSource().sendSuccess(()->Component.literal("Patch verified: mixed meadow, clear gaps, frosted grass, substrate, loot and support; all 20 tree forms, saplings, wood recipes, fallen logs and habitat checks"),false);
                return 1;
            }catch(IllegalStateException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
          })));
    }
    private static boolean place(ServerLevel l,String name,BlockPos origin,long seed){
        var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","patch_"+name));
        return l.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key).value()
            .place(l,l.getChunkSource().getGenerator(),RandomSource.create(seed),origin);
    }
    private static void check(ServerLevel l){
        BlockPos center=new BlockPos(8,231,8);
        require(l.hasChunkAt(center),"patch fixture not loaded");
        Map<BlockPos,BlockState> saved=new HashMap<>();
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)for(int y=230;y<=232;y++){
            var pos=new BlockPos(x,y,z);
            saved.put(pos,l.getBlockState(pos));
        }
        try{
            reset(l,Blocks.GRASS_BLOCK.defaultBlockState(),true);
            require(place(l,"open_meadow",center,81),"mixed patch did not place");
            Set<String> kinds=new HashSet<>(); int count=0,gaps=0;
            for(int x=1;x<=14;x++)for(int z=1;z<=14;z++){
                var pos=new BlockPos(x,231,z);
                var b=l.getBlockState(pos);
                var key=BuiltInRegistries.BLOCK.getKey(b.getBlock());
                if(key.getNamespace().equals("worldlogicterrain")){
                    require(MEADOW.contains(key.getPath()),"unexpected meadow species");
                    require(Math.abs(x-8)<=3&&Math.abs(z-8)<=3,"patch outside its compact radius");
                    count++;kinds.add(key.getPath());
                }else if(Math.abs(x-8)>3||Math.abs(z-8)>3){
                    require(b.is(Blocks.SHORT_GRASS),"grass outside group changed");
                    gaps++;
                }
            }
            require(count>=4&&kinds.size()>=2&&gaps>=100,"mixed patch density or variety failed "+count+" "+kinds);
            reset(l,Blocks.GRASS_BLOCK.defaultBlockState(),true);
            var grass=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","frosted_grass"));
            require(place(l,"frosted_grass",center,82),"frost patch failed");
            BlockPos sample=null;int frost=0;
            for(int x=1;x<=14;x++)for(int z=1;z<=14;z++){
                var pos=new BlockPos(x,231,z);
                if(l.getBlockState(pos).is(grass)){
                    require(Math.abs(x-8)<=3&&Math.abs(z-8)<=3,"frost outside compact patch");
                    frost++;sample=pos;
                }
            }
            require(frost>=4&&sample!=null,"missing frost group");
            var drops=Block.getDrops(l.getBlockState(sample),l,sample,null,null,ItemStack.EMPTY);
            require(drops.size()==1&&drops.getFirst().is(grass.asItem())&&drops.getFirst().getCount()==1,"frost loot");
            l.setBlock(sample.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            require(l.getBlockState(sample).isAir(),"frost support removal");
            reset(l,Blocks.GRASS_BLOCK.defaultBlockState(),false);
            for(int x=1;x<=14;x++)for(int z=1;z<=14;z++)
                l.setBlock(new BlockPos(x,231,z),Blocks.SNOW.defaultBlockState(),Block.UPDATE_ALL);
            require(place(l,"frosted_grass",center,82),"frost patch failed through snow layer");
            reset(l,Blocks.STONE.defaultBlockState(),false);
            require(!place(l,"open_meadow",center,81)&&!place(l,"frosted_grass",center,82),"plants generated on stone");
            reset(l,Blocks.ICE.defaultBlockState(),false);
            require(!place(l,"frosted_grass",center,82),"frost grew on ice");
        }finally{
            // Restore floor first, then plant cells.
            saved.entrySet().stream().sorted(java.util.Comparator.comparingInt(v->v.getKey().getY())).forEach(v->l.setBlock(v.getKey(),v.getValue(),Block.UPDATE_ALL));
        }
    }
    private static void reset(ServerLevel l,BlockState floor,boolean grass){
        for(int x=1;x<=14;x++)for(int z=1;z<=14;z++){
            l.setBlock(new BlockPos(x,232,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            l.setBlock(new BlockPos(x,231,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            l.setBlock(new BlockPos(x,230,z),floor,Block.UPDATE_ALL);
            if(grass)l.setBlock(new BlockPos(x,231,z),Blocks.SHORT_GRASS.defaultBlockState(),Block.UPDATE_ALL);
        }
    }
}
