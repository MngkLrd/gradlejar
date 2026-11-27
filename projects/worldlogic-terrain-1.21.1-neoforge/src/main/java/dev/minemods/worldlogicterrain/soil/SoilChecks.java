package dev.minemods.worldlogicterrain.soil;

import dev.minemods.worldlogicterrain.registry.TerrainContent;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.CropBlock;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

final class SoilChecks {
    static void require(boolean value,String why){if(!value)throw new IllegalStateException(why);}
    static void register(RegisterCommandsEvent e){
        e.getDispatcher().register(Commands.literal("worldlogicterrain").requires(s->s.hasPermission(2)).then(Commands.literal("soil_check").executes(c->{
            try{verify(c.getSource().getLevel());c.getSource().sendSuccess(()->Component.literal("Soil verified: three cycles, moisture, normal crop support, fallow, persistence, natural melon marker, planted melon rejection, growth penalty and independent cells"),false);return 1;}
            catch(IllegalStateException ex){c.getSource().sendFailure(Component.literal(ex.getMessage()));return 0;}
        })));
    }
    static void verify(ServerLevel l){
        var soil=new BlockPos(5,245,5);var ground=soil.east(3);var other=soil.east(5);var data=SoilExhaustion.get(l);
        var wheat=Blocks.WHEAT.defaultBlockState();var ripe=((CropBlock)Blocks.WHEAT).getStateForAge(7);
        var melon=Blocks.MELON.defaultBlockState();
        var old=l.getBlockState(soil);var oldGround=l.getBlockState(ground);var oldOther=l.getBlockState(other);
        var oldTop=l.getBlockState(soil.above());
        try{
            data.remove(soil);data.remove(ground);data.remove(other);
            l.setBlock(soil,Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE,7),Block.UPDATE_ALL);
            l.setBlock(ground,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
            l.setBlock(other,Blocks.DIRT.defaultBlockState(),Block.UPDATE_ALL);
            require(!data.harvest(l,soil,ripe,0,false),"Untracked crop counted");
            require(!data.harvest(l,ground,melon,0,false),"Placed melon counted");
            for(int i=0;i<3;i++){
                long start=i*SoilExhaustion.DAY;
                data.plant(l,soil,wheat,start);
                require(data.harvest(l,soil,ripe,start+100,false),"Planted ripe crop rejected");
                require(!data.harvest(l,soil,ripe,start+101,false),"Duplicate crop counted");
                data.plant(l,ground,melon,start);
                require(data.harvest(l,ground,melon,start+100,false),"Natural melon harvest rejected");
                require(data.cycles(soil)==i+1&&data.cycles(ground)==i+1,"Cycle count mismatch");
                require(SoilExhaustion.exhausted(l.getBlockState(soil))==(i==2),"Premature or missing exhaustion");
            }
            require(l.getBlockState(soil).is(TerrainContent.EXHAUSTED_FARMLAND.get())&&l.getBlockState(soil).getValue(FarmBlock.MOISTURE)==7,"Moisture lost");
            require(l.getBlockState(ground).is(TerrainContent.EXHAUSTED_DIRT.get()),"Dirt did not exhaust");
            require(ripe.canSurvive(l,soil.above()),"Vanilla crop cannot survive");
            require(data.cycles(other)==0&&!SoilExhaustion.exhausted(l.getBlockState(other)),"Adjacent soil changed");
            var loaded=SoilExhaustion.load(data.save(new CompoundTag(),l.registryAccess()),l.registryAccess());
            require(loaded.cycles(soil)==3&&loaded.cycles(ground)==3,"History not persistent");
            int pass=0;var rand=RandomSource.create(147);
            for(int i=0;i<1000;i++){if(SoilExhaustion.allowGrowth(l,soil,rand))pass++;require(SoilExhaustion.allowGrowth(l,other,rand),"Normal soil penalized");}
            require(pass>580&&pass<720,"Wrong growth penalty "+pass);
            long last=2*SoilExhaustion.DAY+100;
            data.maintain(l,last+SoilExhaustion.DAY,256);
            require(SoilExhaustion.exhausted(l.getBlockState(soil)),"Recovered after one day");
            data.maintain(l,last+SoilExhaustion.REST,256);
            require(l.getBlockState(soil).is(Blocks.FARMLAND)&&l.getBlockState(soil).getValue(FarmBlock.MOISTURE)==7&&l.getBlockState(ground).is(Blocks.DIRT),"Two-day recovery failed");
            data.plant(l,soil,wheat,100);require(data.harvest(l,soil,ripe,200,false),"First crop");
            data.plant(l,soil,wheat,200+SoilExhaustion.DAY+1);require(data.harvest(l,soil,ripe,300+SoilExhaustion.DAY,false)&&data.cycles(soil)==1,"Long break did not reset");
        }finally{
            data.remove(soil);data.remove(ground);data.remove(other);
            l.setBlock(soil,old,Block.UPDATE_ALL);l.setBlock(ground,oldGround,Block.UPDATE_ALL);l.setBlock(other,oldOther,Block.UPDATE_ALL);l.setBlock(soil.above(),oldTop,Block.UPDATE_ALL);
        }
    }
}
