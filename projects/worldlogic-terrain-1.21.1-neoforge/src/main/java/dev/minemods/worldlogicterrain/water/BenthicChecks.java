package dev.minemods.worldlogicterrain.water;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import java.util.LinkedHashMap;

final class BenthicChecks {
    private static void require(boolean v,String msg){if(!v)throw new IllegalStateException("Benthic: "+msg);}
    static void verify(ServerLevel l){
        require(BenthicSpecies.values().length==21,"species count");
        var p=new BlockPos(6,250,6);var saved=new LinkedHashMap<BlockPos,BlockState>();
        for(int i=-1;i<=7;i++){var q=p.above(i);saved.put(q,l.getBlockState(q));}
        try{
            for(var sp:BenthicSpecies.values()){
                var kind=AquaticHabitat.Kind.valueOf(sp.waters.split(" ")[0]);
                require(BenthicEcology.habitat(sp,kind,(sp.minTemp+sp.maxTemp)/2,sp.minDepth),"suitable habitat "+sp.id);
                require(!BenthicEcology.habitat(sp,AquaticHabitat.Kind.GROUNDWATER,1,sp.minDepth),"groundwater "+sp.id);
                if(sp==BenthicSpecies.IRISH_MOSS)require(!sp.accepts(AquaticHabitat.Kind.RIVER)&&!sp.accepts(AquaticHabitat.Kind.LAKE)
                    &&sp.accepts(AquaticHabitat.Kind.SEA),"Irish moss is marine only");
                require(!BenthicEcology.habitat(sp,kind,sp.maxTemp+1,sp.minDepth)&&!BenthicEcology.habitat(sp,kind,1,sp.maxDepth+1),"climate/depth boundary "+sp.id);
                int nativeId=BenthicBlock.nativeIndex(kind);
                int f=BenthicBlock.nativeAvailable()?nativeId:0;
                var fluid=BenthicBlock.fluid(f);require(fluid!=Fluids.EMPTY,"fluid registry "+sp.id);
                var water=fluid.defaultFluidState().createLegacyBlock();
                var support=sp.substrate.equals("ROCK")?Blocks.STONE.defaultBlockState():Blocks.SAND.defaultBlockState();
                l.setBlock(p.below(),support,2|16);
                for(int i=0;i<=7;i++)l.setBlock(p.above(i),water,2|16);
                require(BenthicEcology.placeColumn(l,p,sp,f,sp.height,0,0),"placement "+sp.id);
                var b=BenthicEcology.block(sp);
                for(int i=0;i<sp.height;i++){
                    var state=l.getBlockState(p.above(i));
                    require(state.is(b)&&state.getValue(BenthicBlock.SEGMENT)==i&&state.getFluidState().getType()==fluid&&state.canSurvive(l,p.above(i)),"segment or fluid "+sp.id);
                }
                require(l.getBlockState(p.above(sp.height)).equals(water),"height cap "+sp.id);
                require(!BenthicEcology.placeColumn(l,p,sp,f,1,0,0),"overwrote plant "+sp.id);
                var drops=Block.getDrops(l.getBlockState(p),l,p,null,null,ItemStack.EMPTY);
                require(drops.size()==1&&drops.getFirst().is(b.asItem()),"loot "+sp.id);
                l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
                for(int i=0;i<sp.height;i++)require(l.getBlockState(p.above(i)).equals(water),"support removal/source restoration "+sp.id+" segment "+i);
                require(!BenthicEcology.placeColumn(l,p,sp,f,1,0,0),"unsupported planting "+sp.id);
            }
            // Exercise the real upward growth path in a source column and reject another fluid.
            var growing=BenthicSpecies.PONDWEED;var b=BenthicEcology.block(growing);
            int f=BenthicBlock.nativeAvailable()?4:0;
            var fluid=BenthicBlock.fluid(f);var water=fluid.defaultFluidState().createLegacyBlock();
            l.setBlock(p.below(),Blocks.SAND.defaultBlockState(),2|16);
            for(int i=0;i<5;i++)l.setBlock(p.above(i),water,2|16);
            require(BenthicEcology.placeColumn(l,p,growing,f,1,0,0),"growth fixture");
            // Vanilla flat fixture may have arbitrary biome temperatures, so assert the actual native/fresh classification
            // and the live growth path only in a compatible fixture.
            var biome=l.getBiome(p).value().getBaseTemperature();
            var habitat=AquaticHabitat.hint(water,l.getBiome(p));
            if(growing.accepts(habitat)&&biome>=growing.minTemp&&biome<=growing.maxTemp){
                require(b.grow(l,p,l.getBlockState(p)),"live first growth");
                require(b.grow(l,p.above(),l.getBlockState(p.above())),"live second growth");
                require(!b.grow(l,p.above(2),l.getBlockState(p.above(2))),"growth beyond limit");
                require(l.getBlockState(p.above(2)).getFluidState().getType()==fluid,"live growth changed fluid");
            }
            l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            require(!b.grow(l,p,l.getBlockState(p)),"growth into air");
            if(BenthicBlock.nativeAvailable()){
                l.setBlock(p.above(),BenthicBlock.fluid(1).defaultFluidState().createLegacyBlock(),2|16);
                require(!b.grow(l,p,l.getBlockState(p)),"growth into sea water");
                l.setBlock(p,water,2|16);
                require(!BenthicEcology.placeColumn(l,p,BenthicSpecies.RED_SEA_STAR,4,1,0,0),"marine star accepted lake water");
            }
            require(!BenthicSpecies.RED_SEA_STAR.accepts(AquaticHabitat.Kind.RIVER)&&!BenthicSpecies.PONDWEED.accepts(AquaticHabitat.Kind.SEA),"salinity boundaries");
        }finally{saved.forEach((q,s)->l.setBlock(q,s,2|16));}
    }
}
