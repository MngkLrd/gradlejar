package dev.minemods.worldlogicterrain.moss;

import dev.minemods.worldlogicterrain.tree.CoreHabitat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Ecological tolerances, not a second climate simulation. Moisture and temperature belong to Core. */
public enum MossSpecies {
    SHEET("sheet_moss","moss_woodland",-2,32,.60,.92,false,4),
    CUSHION("cushion_moss","moss_woodland",-6,26,.42,.90,false,6),
    SPHAGNUM("sphagnum_moss","season_bog",-3,24,.76,.98,false,8),
    ICELAND("iceland_moss","moss_cool",-14,19,.25,.78,true,10),
    REINDEER("reindeer_moss","moss_cool",-20,17,.19,.72,true,11);

    public final String id,habitat;
    public final double minTemp,maxTemp,minMoisture,maxWaterlogging;
    public final boolean edible;
    public final int height;
    MossSpecies(String id,String habitat,double minTemp,double maxTemp,double minMoisture,double maxWaterlogging,boolean edible,int height){
        this.id=id;this.habitat=habitat;this.minTemp=minTemp;this.maxTemp=maxTemp;this.minMoisture=minMoisture;
        this.maxWaterlogging=maxWaterlogging;this.edible=edible;this.height=height;
    }
    public Block block(){return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("worldlogicterrain",id));}
    public boolean supports(BlockGetter l,BlockPos p){
        var s=l.getBlockState(p);
        if(!s.isFaceSturdy(l,p,Direction.UP)||!s.getFluidState().isEmpty())return false;
        boolean soil=s.is(BlockTags.DIRT)||s.is(Blocks.MUD)||s.is(Blocks.CLAY);
        boolean rock=s.is(Blocks.STONE)||s.is(Blocks.ANDESITE)||s.is(Blocks.GRANITE)||s.is(Blocks.DIORITE)||s.is(Blocks.COBBLESTONE);
        // Core ground blocks are authoritative; no texture/name-based classification.
        var id=BuiltInRegistries.BLOCK.getKey(s.getBlock());
        if(id.getNamespace().equals("worldlogic")&&java.util.Set.of("loam","silt","sandy_loam","peat","clay_soil","silty_loam","sandy_soil").contains(id.getPath()))soil=true;
        return switch(this){
            case SHEET -> soil||s.is(BlockTags.LOGS)||rock;
            case CUSHION -> soil||rock||s.is(BlockTags.LOGS);
            case SPHAGNUM -> soil;
            case ICELAND,REINDEER -> soil||rock;
        };
    }
    public boolean suitable(CoreHabitat.Sample h){
        return Double.isFinite(h.temperature())&&Double.isFinite(h.moisture())&&Double.isFinite(h.waterlogging())&&Double.isFinite(h.dryness())
            &&h.temperature()>=minTemp&&h.temperature()<=maxTemp&&h.moisture()>=minMoisture&&h.waterlogging()<=maxWaterlogging
            &&h.dryness()<=(edible?.82:.55)&&!h.zone().equals("MARINE")&&!h.zone().equals("SUBTERRANEAN");
    }
}
