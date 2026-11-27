package dev.minemods.worldlogicterrain.tree;

import java.util.List;
import java.util.Locale;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class TreeChecks {
    private TreeChecks(){}
    private static final BlockPos CENTER=new BlockPos(8,260,8);
    private record Form(String feature,String namespace,String wood,int minHeight){}
    private static final List<Form> FORMS=List.of(
        new Form("oak_young","minecraft","oak",5),
        new Form("birch_pioneer","minecraft","birch",4),
        new Form("larch_exposed","worldlogicterrain","larch",5),
        new Form("spruce_young","minecraft","spruce",5),
        new Form("oak_spreading","minecraft","oak",6),
        new Form("oak_ancient","minecraft","oak",10),
        new Form("oak_windswept","minecraft","oak",4),
        new Form("birch_young","minecraft","birch",6),
        new Form("birch_tall","minecraft","birch",10),
        new Form("pine_tall","worldlogicterrain","pine",12),
        new Form("spruce_dense","minecraft","spruce",8),
        new Form("spruce_old","minecraft","spruce",17),
        new Form("larch_young","worldlogicterrain","larch",7),
        new Form("larch_tall","worldlogicterrain","larch",13),
        new Form("willow_young","worldlogicterrain","willow",5),
        new Form("willow_spreading","worldlogicterrain","willow",8),
        new Form("acacia_stunted","minecraft","acacia",4),
        new Form("acacia_spreading","minecraft","acacia",6),
        new Form("jungle_canopy","minecraft","jungle",8),
        new Form("jungle_giant","minecraft","jungle",18),
        new Form("dark_oak_old","minecraft","dark_oak",8),
        new Form("cherry_spreading","minecraft","cherry",8),
        new Form("mangrove_coast","minecraft","mangrove",7),
        new Form("pine_young","worldlogicterrain","pine",7),
        new Form("pine_windswept","worldlogicterrain","pine",6),
        new Form("fir_young","worldlogicterrain","fir",7),
        new Form("fir_tall","worldlogicterrain","fir",15),
        new Form("cedar_young","worldlogicterrain","cedar",8),
        new Form("cedar_spreading","worldlogicterrain","cedar",13),
        new Form("maple_young","worldlogicterrain","maple",6),
        new Form("maple_spreading","worldlogicterrain","maple",9),
        new Form("beech_young","worldlogicterrain","beech",7),
        new Form("beech_tall","worldlogicterrain","beech",14),
        new Form("alder_young","worldlogicterrain","alder",6),
        new Form("alder_spreading","worldlogicterrain","alder",9),
        new Form("palm_young","worldlogicterrain","palm",7),
        new Form("palm_leaning","worldlogicterrain","palm",11),
        new Form("date_palm_young","worldlogicterrain","date_palm",7),
        new Form("date_palm_tall","worldlogicterrain","date_palm",12),
        new Form("baobab_young","worldlogicterrain","baobab",7),
        new Form("baobab_ancient","worldlogicterrain","baobab",12),
        new Form("ebony_young","worldlogicterrain","ebony",7),
        new Form("ebony_spreading","worldlogicterrain","ebony",10),
        new Form("mahogany_young","worldlogicterrain","mahogany",8),
        new Form("mahogany_canopy","worldlogicterrain","mahogany",13)
    );
    private record Shape(int logs,int leaves,int branches,int height,int connectedLeaves){}
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    private static Block block(String ns,String name){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath(ns,name));}
    private static boolean place(ServerLevel l,String name,long seed){
        var key=ResourceKey.create(Registries.CONFIGURED_FEATURE,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","tree_"+name));
        return l.registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(key).value()
            .place(l,l.getChunkSource().getGenerator(),RandomSource.create(seed),CENTER);
    }
    public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("habitat_check").executes(c->{
            ServerLevel l=c.getSource().getLevel();
            var pos=l.getSharedSpawnPos();
            if(!l.hasChunkAt(pos)){c.getSource().sendFailure(Component.literal("Spawn not loaded"));return 0;}
            pos=new BlockPos(pos.getX(),l.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos.getX(),pos.getZ()),pos.getZ());
            var result=CoreHabitat.batch(l,l.getChunkSource().getGenerator()).sample(pos,l.getBiome(pos),l.getBlockState(pos.below()));
            if(result.isEmpty()){c.getSource().sendFailure(Component.literal("Core habitat unavailable for this generator"));return 0;}
            var h=result.get();
            if(!Double.isFinite(h.temperature())||h.moisture()<0||h.moisture()>1||h.slope()<0||h.wind()<0||h.wind()>1){
                c.getSource().sendFailure(Component.literal("Invalid habitat sample"));return 0;
            }
            c.getSource().sendSuccess(()->Component.literal(String.format(Locale.ROOT,"Core habitat sampled: zone=%s temp=%.2f moisture=%.3f fertility=%.3f slope=%.2f wind=%.3f",h.zone(),h.temperature(),h.moisture(),h.fertility(),h.slope(),h.wind())),false);
            return 1;
          })));
    }
    public static void verify(ServerLevel l){
        // High, previously unused flat-world volume. Never edit a player's world from a normal feature.
        for(var pos:BlockPos.betweenClosed(-6,258,-6,22,307,22))
            require(l.getBlockState(pos).isAir(),"Tree QA fixture not empty: "+pos);
        try{
            for(var pos:BlockPos.betweenClosed(-6,258,-6,22,259,22))l.setBlock(pos,Blocks.DIRT.defaultBlockState(),2);
            int young=0,ancient=0;
            for(var form:FORMS){
                Shape shape=null;
                for(int seed=83;seed<87;seed++){
                    clear(l);
                    if(!place(l,form.feature(),seed))continue;
                    shape=shape(l,block(form.namespace(),form.wood()+"_log"),block(form.namespace(),form.wood()+"_leaves"));
                    boolean branched=List.of("oak_spreading","oak_ancient","oak_windswept","willow_young","willow_spreading","acacia_stunted","acacia_spreading","cherry_spreading","jungle_canopy","jungle_giant","pine_windswept","cedar_spreading","maple_spreading","alder_spreading","palm_leaning","baobab_ancient","ebony_spreading","mahogany_canopy").contains(form.feature());
                    if(shape.logs()>=4&&shape.leaves()>=6&&shape.height()>=form.minHeight()-1&&shape.connectedLeaves()>0&&(!branched||shape.branches()>0))break;
                    shape=null;
                }
                require(shape!=null,"Invalid actual tree form "+form.feature());
                if(form.feature().equals("oak_young"))young=shape.height();
                if(form.feature().equals("oak_ancient"))ancient=shape.height();
            }
            require(ancient>young,"Old oak does not exceed young oak height");
            for(String species:new String[]{"willow","larch","pine","fir","cedar","maple","beech","alder","palm","date_palm","baobab","ebony","mahogany"}){
                clear(l);
                Block log=block("worldlogicterrain",species+"_log");
                Block leaves=block("worldlogicterrain",species+"_leaves");
                Block planks=block("worldlogicterrain",species+"_planks");
                Block sapling=block("worldlogicterrain",species+"_sapling");
                require(log.defaultBlockState().is(BlockTags.LOGS)&&leaves.defaultBlockState().is(BlockTags.LEAVES),"Wood tags "+species);
                require(sapling instanceof SaplingBlock,"Sapling missing "+species);
                for(Block b:List.of(log,planks,sapling)){
                    var drops=Block.getDrops(b.defaultBlockState(),l,CENTER,null,null,ItemStack.EMPTY);
                    require(drops.size()==1&&drops.getFirst().is(b.asItem())&&drops.getFirst().getCount()==1,"Wrong wood drop "+b);
                }
                var leafDrops=Block.getDrops(leaves.defaultBlockState(),l,CENTER,null,null,new ItemStack(Items.SHEARS));
                require(leafDrops.size()==1&&leafDrops.getFirst().is(leaves.asItem()),"Shears leaf drop "+species);
                boolean saplingDrop=false;
                for(int n=0;n<128&&!saplingDrop;n++)saplingDrop=Block.getDrops(leaves.defaultBlockState(),l,CENTER,null,null,ItemStack.EMPTY).stream().anyMatch(i->i.is(sapling.asItem()));
                require(saplingDrop,"Leaf sapling drop "+species);
                var input=CraftingInput.of(1,1,List.of(new ItemStack(log)));
                var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l);
                require(recipe.isPresent(),"Planks recipe missing "+species);
                var result=recipe.orElseThrow().value().assemble(input,l.registryAccess());
                require(result.is(planks.asItem())&&result.getCount()==4,"Planks recipe wrong "+species);
                l.setBlock(CENTER,sapling.defaultBlockState().setValue(SaplingBlock.STAGE,1),2);
                ((SaplingBlock)sapling).advanceTree(l,CENTER,l.getBlockState(CENTER),RandomSource.create(91));
                var grown=shape(l,log,leaves);
                require(grown.logs()>3&&grown.leaves()>5,"Sapling did not grow "+species);
            }
            clear(l);
            l.setBlock(CENTER.below(),Blocks.SAND.defaultBlockState(),2);
            require(block("worldlogicterrain","palm_sapling").defaultBlockState().canSurvive(l,CENTER),"Palm sapling rejected sand");
            require(place(l,"palm_young",83),"Palm failed on sand");
            clear(l);
            l.setBlock(CENTER.below(),Blocks.STONE.defaultBlockState(),2);
            require(!place(l,"palm_young",83),"Palm grew on stone");
            clear(l);
            l.setBlock(CENTER.above(3),Blocks.STONE.defaultBlockState(),2);
            require(!place(l,"palm_young",83)&&l.getBlockState(CENTER).isAir(),"Palm overwrote obstacle or partially placed");
            clear(l);
            require(EcologicalTreeFeature.fallen(l,CENTER,"willow",Direction.EAST,4),"Fallen trunk did not generate");
            require(l.getBlockState(CENTER).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y,"Missing vertical stump");
            for(int i=1;i<=4;i++)require(l.getBlockState(CENTER.east(i)).is(block("worldlogicterrain","willow_log"))&&l.getBlockState(CENTER.east(i)).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X,"Wrong fallen log "+i);
            clear(l);
            l.setBlock(CENTER.east(2),Blocks.STONE.defaultBlockState(),2);
            require(!EcologicalTreeFeature.fallen(l,CENTER,"willow",Direction.EAST,4)&&l.getBlockState(CENTER).isAir(),"Fallen log overwrote obstacle");
            clear(l);
            for(String species:List.of("oak","birch","spruce","willow","larch","pine","fir","cedar","maple","beech","alder","palm","date_palm","baobab","ebony","mahogany")){
                require(EcologicalTreeFeature.recruit(l,CENTER,species),"Forest recruitment failed "+species);
                require(l.getBlockState(CENTER).getBlock() instanceof SaplingBlock,"Recruit not a real sapling "+species);
                require(!EcologicalTreeFeature.recruit(l,CENTER,species),"Recruit replaced existing sapling");
                clear(l);
            }
            for(String cover:List.of("forest_litter","fallen_leaves")){
                require(EcologicalTreeFeature.understory(l,CENTER,cover)&&l.getBlockState(CENTER).is(block("worldlogicterrain",cover)),"Missing forest cover "+cover);
                require(!EcologicalTreeFeature.understory(l,CENTER,cover),"Cover replaced occupied ground");
                clear(l);
            }
            l.setBlock(CENTER.below(),Blocks.STONE.defaultBlockState(),2);
            require(!EcologicalTreeFeature.recruit(l,CENTER,"larch")&&!EcologicalTreeFeature.understory(l,CENTER,"forest_litter")&&l.getBlockState(CENTER).isAir(),"Forest renewal on stone");
            clear(l);
            double min=1,max=0;
            for(int x=-256;x<=256;x+=16)for(int z=-256;z<=256;z+=16){
                double a=EcologicalTreeFeature.grove(24,x,z);
                require(a==EcologicalTreeFeature.grove(24,x,z)&&a>=0&&a<=1,"Non-deterministic grove field");
                min=Math.min(min,a);max=Math.max(max,a);
            }
            require(min<.25&&max>.75,"No actual forest gaps/stands in seeded field");
            require(!EcologicalTreeFeature.soil(Blocks.STONE.defaultBlockState())&&!EcologicalTreeFeature.soil(Blocks.ICE.defaultBlockState())&&EcologicalTreeFeature.soil(Blocks.GRASS_BLOCK.defaultBlockState()),"Tree substrate policy");
            if(CoreHabitat.available())require(CoreHabitat.batch(l,l.getChunkSource().getGenerator()).sample(CENTER,l.getBiome(CENTER),l.getBlockState(CENTER.below())).isEmpty(),"Unexpected core sampler on flat generator");
        }finally{
            for(var pos:BlockPos.betweenClosed(-6,258,-6,22,307,22))if(!l.getBlockState(pos).isAir())l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
    }
    private static void clear(ServerLevel l){
        for(var pos:BlockPos.betweenClosed(-6,260,-6,22,307,22))
            if(!l.getBlockState(pos).isAir())l.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        for(var pos:BlockPos.betweenClosed(-6,258,-6,22,259,22))
            if(!l.getBlockState(pos).is(Blocks.DIRT))l.setBlock(pos,Blocks.DIRT.defaultBlockState(),2);
    }
    private static Shape shape(ServerLevel l,Block log,Block foliage){
        int logs=0,leaves=0,branches=0,height=0,connected=0;
        for(var pos:BlockPos.betweenClosed(-6,260,-6,22,307,22)){
            var b=l.getBlockState(pos);
            if(b.is(log)){
                logs++;height=Math.max(height,pos.getY()-CENTER.getY()+1);
                if(pos.getX()!=CENTER.getX()||pos.getZ()!=CENTER.getZ())branches++;
            }
            if(b.is(foliage)){
                leaves++;
                if(b.hasProperty(LeavesBlock.DISTANCE)&&b.getValue(LeavesBlock.DISTANCE)<7)connected++;
            }
        }
        return new Shape(logs,leaves,branches,height,connected);
    }
}
