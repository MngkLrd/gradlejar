package dev.minemods.worldlogicterrain.water;

import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class AquaticChecks {
    private AquaticChecks(){}
    private static void require(boolean value,String msg){if(!value)throw new IllegalStateException(msg);}
    public static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2))
          .then(Commands.literal("aquatic_check").executes(c->{
            try{int nativeCount=verify(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("Aquatic checks passed; native fluids="+nativeCount),false);return 1;}
            catch(IllegalStateException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
          })));
    }
    static int verify(ServerLevel l){
        BlockPos p=new BlockPos(8,245,8);int tested=0;
        var lily=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain","water_lily"));
        var saved=l.getBlockState(p);var floor=l.getBlockState(p.below());
        var upper=l.getBlockState(p.above());
        require(saved.isAir()&&floor.isAir()&&upper.isAir(),"Aquatic fixture not empty");
        try{
            l.setBlock(p.below(),Blocks.WATER.defaultBlockState(),2);
            require(AquaticFeature.placePlant(l,p,"water_lily",0,0),"Lily rejected vanilla source");
            require(l.getBlockState(p.below()).is(Blocks.WATER),"Lily replaced water");
            var drops=Block.getDrops(l.getBlockState(p),l,p,null,null,ItemStack.EMPTY);
            require(drops.size()==1&&drops.getFirst().is(lily.asItem()),"Lily loot");
            l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);
            require(l.getBlockState(p).isAir(),"Lily remained without water");
            l.setBlock(p.below(),Blocks.STONE.defaultBlockState(),2);
            require(!AquaticFeature.placePlant(l,p,"water_lily",0,0),"Lily on stone");
            l.setBlock(p.below(),Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL,1),2);
            require(!AquaticFeature.placePlant(l,p,"water_lily",0,0),"Lily on flowing water");
            l.setBlock(p.below(),Blocks.GRASS_BLOCK.defaultBlockState(),2);
            for(String id:new String[]{"reed","cattail","sedge","marsh_marigold","dry_grass","tall_dry_grass"}){
                require(AquaticFeature.placePlant(l,p,id,0,0),"Shore plant failed "+id);
                require(!AquaticFeature.placePlant(l,p,id,0,0),"Shore plant replaced occupied block");
                l.setBlock(p.above(),Blocks.AIR.defaultBlockState(),2);
                l.setBlock(p,Blocks.AIR.defaultBlockState(),2);
            }
            var random=RandomSource.create(31);
            for(int i=0;i<32;i++){
                require(java.util.Set.of("dry_grass","tall_dry_grass").contains(AquaticFeature.shoreSpecies(AquaticHabitat.Kind.SEA,random)),"Fresh species in marine water");
                require(java.util.Set.of("reed","sedge").contains(AquaticFeature.shoreSpecies(AquaticHabitat.Kind.SPRING,random)),"Spring flora");
                require(AquaticFeature.shoreSpecies(AquaticHabitat.Kind.GROUNDWATER,random)==null,"Groundwater surface flora");
            }
            String[] ids={"sea_water","brackish_water","river_water","lake_water","pond_water","spring_water","wetland_water","groundwater"};
            AquaticHabitat.Kind[] kinds={AquaticHabitat.Kind.SEA,AquaticHabitat.Kind.BRACKISH,AquaticHabitat.Kind.RIVER,AquaticHabitat.Kind.LAKE,AquaticHabitat.Kind.POND,AquaticHabitat.Kind.SPRING,AquaticHabitat.Kind.WETLAND,AquaticHabitat.Kind.GROUNDWATER};
            for(int i=0;i<ids.length;i++){
                var b=BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogic",ids[i]));
                if(!(b instanceof LiquidBlock))continue;
                tested++;l.setBlock(p,Blocks.AIR.defaultBlockState(),2);l.setBlock(p.below(),b.defaultBlockState(),2);
                require(AquaticHabitat.intrinsic(l.getBlockState(p.below()))==kinds[i],"Native identity "+ids[i]);
                boolean expected=AquaticHabitat.floating(kinds[i]);
                require(AquaticFeature.placePlant(l,p,"water_lily",0,0)==expected,"Native lily tolerance "+ids[i]);
                require(l.getBlockState(p.below()).is(b),"Native water replaced "+ids[i]);
                if(expected){l.setBlock(p.below(),Blocks.AIR.defaultBlockState(),Block.UPDATE_ALL);require(l.getBlockState(p).isAir(),"Native water support removal "+ids[i]);}
            }
            require(tested==0||tested==8,"Incomplete native fluid registry");
            return tested;
        }finally{
            l.setBlock(p.above(),upper,2);l.setBlock(p,saved,2);l.setBlock(p.below(),floor,2);
        }
    }
}
