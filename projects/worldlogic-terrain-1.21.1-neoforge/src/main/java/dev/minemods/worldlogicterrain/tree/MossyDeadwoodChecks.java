package dev.minemods.worldlogicterrain.tree;

import dev.minemods.worldlogicterrain.api.WoodPhysicsApi;
import dev.minemods.worldlogicterrain.registry.TerrainContent;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

final class MossyDeadwoodChecks {
    private MossyDeadwoodChecks(){}
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    private static CoreHabitat.Sample sample(double temp,double moisture,double dryness,double waterlogging,String zone){
        return new CoreHabitat.Sample(temp,moisture,.6,dryness,waterlogging,.1,1,zone,"NORMAL",2,.2,1,0,.2);
    }
    static void verify(ServerLevel l,BlockPos center){
        var wet=sample(14,.76,.14,.3,"OPEN_PLAINS");
        require(MossyDeadwood.suitable(wet),"Humid moss habitat rejected");
        require(!MossyDeadwood.suitable(sample(14,.3,.2,.3,"OPEN_PLAINS"))
            &&!MossyDeadwood.suitable(sample(14,.8,.8,.3,"OPEN_PLAINS"))
            &&!MossyDeadwood.suitable(sample(-3,.8,.1,.3,"OPEN_PLAINS"))
            &&!MossyDeadwood.suitable(sample(14,.8,.1,.99,"OPEN_PLAINS"))
            &&!MossyDeadwood.suitable(sample(14,.8,.1,.3,"MARINE")),"Moss climate rejection");
        var east=Direction.EAST;
        for(String wood:MossyDeadwood.SPECIES){
            var originalId=MossyDeadwood.originalId(wood);
            var original=BuiltInRegistries.BLOCK.get(originalId);
            var moss=BuiltInRegistries.BLOCK.get(MossyDeadwood.mossyId(wood));
            var state=moss.defaultBlockState();
            require(!state.isAir()&&state.is(BlockTags.LOGS)&&state.is(BlockTags.MINEABLE_WITH_AXE),"Mossy log tags "+wood);
            require(TerrainContent.allItems().stream().anyMatch(x->x.get()==moss.asItem()),"Mossy log item "+wood);
            require(MossyDeadwood.original(state).orElseThrow().equals(originalId),"Original species "+wood);
            if(WoodPhysicsApi.available())
                require(WoodPhysicsApi.profile(state).orElseThrow().equals(WoodPhysicsApi.profile(original.defaultBlockState()).orElseThrow()),"Core material alias "+wood);
            require(MossyDeadwood.canMoss(l,center,wood,east,4,p->true),"Mossy site rejected "+wood);
            require(!MossyDeadwood.canMoss(l,center,wood,east,4,p->!p.equals(center.east(2))),"Mixed dry trunk accepted "+wood);
            require(EcologicalTreeFeature.fallen(l,center,wood,east,4,true),"Mossy trunk failed "+wood);
            require(l.getBlockState(center).is(moss)&&l.getBlockState(center).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.Y,"Mossy stump "+wood);
            for(int i=1;i<=4;i++)require(l.getBlockState(center.east(i)).is(moss)&&l.getBlockState(center.east(i)).getValue(RotatedPillarBlock.AXIS)==Direction.Axis.X,"Mossy horizontal axis "+wood);
            var drops=Block.getDrops(l.getBlockState(center),l,center,null);
            require(drops.size()==1&&drops.getFirst().is(moss.asItem()),"Mossy log loot "+wood);
            var input=CraftingInput.of(1,1,List.of(new ItemStack(moss)));
            var recipe=l.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,l).orElseThrow();
            var out=recipe.value().assemble(input,l.registryAccess());
            var planks=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(originalId.getNamespace(),wood+"_planks"));
            require(out.is(planks)&&out.getCount()==4,"Mossy log crafting "+wood);
            if(wood.equals("oak")){
                // Existing flowers/obstacles and dry ground must remain untouched.
                var obstacle=center.north();
                l.setBlock(obstacle,Blocks.DANDELION.defaultBlockState(),Block.UPDATE_ALL);
                var dry=center.south(2);
                var count=MossyDeadwood.decorate(l,center,east,4,RandomSource.create(81),p->!p.equals(dry));
                int top=0,around=0;
                for(int i=1;i<=4;i++)if(l.getBlockState(center.east(i).above()).is(Blocks.MOSS_CARPET))top++;
                for(int x=0;x<=4;x++)for(int z=-2;z<=2;z++)if(z!=0&&l.getBlockState(center.offset(x,0,z)).is(Blocks.MOSS_CARPET))around++;
                require(top>=1&&around>=1&&count==top+around,"Missing top/ground moss: "+top+"/"+around);
                require(l.getBlockState(obstacle).is(Blocks.DANDELION)&&l.getBlockState(dry).isAir(),"Moss replaced protected or dry ground");
            }
            for(int x=0;x<=4;x++)for(int z=-2;z<=2;z++)for(int y=1;y>=0;y--)l.setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        }
        l.setBlock(center.east(2),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
        require(!MossyDeadwood.canMoss(l,center,"oak",east,4,p->true)&&!EcologicalTreeFeature.fallen(l,center,"oak",east,4,true)&&l.getBlockState(center).isAir(),"Mossy trunk overwrote obstacle");
        l.setBlock(center.east(2),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
        l.setBlock(center.east(2).below(),Blocks.STONE.defaultBlockState(),Block.UPDATE_ALL);
        require(!MossyDeadwood.canMoss(l,center,"oak",east,4,p->true),"Mossy trunk on stone");
        l.setBlock(center.east(2).below(),Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
        require(!MossyDeadwood.canMoss(l,new BlockPos(14,center.getY(),8),"oak",east,4,p->true),"Mossy trunk crossed chunk");
    }
}
