package dev.minemods.worldlogicterrain.tree;

import dev.minemods.worldlogicterrain.block.HangingFruitBlock;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** Region-specific fruit trees selected only during actual tree generation. */
public final class FruitEcology {
    private FruitEcology(){}
    public record Plan(TreeFruit fruit,String replacementForm){}
    private static final TagKey<Biome> EXCLUDED=TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("worldlogicterrain","fruit_excluded"));
    public static boolean winterName(String name){
        return name.contains("tundra")||name.contains("snow")||name.contains("frozen")||name.contains("arctic")
            ||name.contains("polar")||name.contains("glacier")||name.contains("ice_spikes")||name.equals("grove")
            ||name.equals("jagged_peaks")||name.equals("frozen_peaks");
    }
    public static boolean isWinter(Holder<Biome> biome){
        return biome.is(EXCLUDED)||biome.value().getBaseTemperature()<=.15f
            ||biome.unwrapKey().map(k->winterName(k.location().getPath())).orElse(false);
    }
    static boolean suitable(String name,double temperature,double coldness,double waterlogging){
        return !winterName(name)&&temperature>=8&&coldness<.65&&waterlogging<.88;
    }
    public static Optional<Plan> select(Holder<Biome> biome,String zone,String name,String originalWood,
                    double temperature,Optional<CoreHabitat.Sample> local,RandomSource random){
        if(isWinter(biome))return Optional.empty();
        return selectSite(zone,name,originalWood,temperature,local.map(CoreHabitat.Sample::coldness).orElse(0.0),
                local.map(CoreHabitat.Sample::waterlogging).orElse(.3),local.map(CoreHabitat.Sample::moisture).orElse(.5),random);
    }
    static Optional<Plan> selectSite(String zone,String name,String wood,double temperature,double coldness,double wet,double moisture,RandomSource random){
        if(!suitable(name,temperature,coldness,wet))return Optional.empty();
        if(wood.equals("palm")&&temperature>=19&&random.nextDouble()<.68)return Optional.of(new Plan(TreeFruit.COCONUT,null));
        if(wood.equals("date_palm")&&temperature>=19&&random.nextDouble()<.68)return Optional.of(new Plan(TreeFruit.DATE,null));
        if(wood.equals("baobab")&&temperature>=20&&random.nextDouble()<.60)return Optional.of(new Plan(TreeFruit.BAOBAB,null));
        if(zone.equals("TROPICAL")&&temperature>=19&&random.nextDouble()<.22)
            return Optional.of(new Plan(TreeFruit.MANGO,"jungle_canopy"));
        if((zone.equals("DRY")||zone.equals("TEMPERATE")&&temperature>=22)&&temperature>=18&&random.nextDouble()<.22)
            return Optional.of(new Plan(TreeFruit.FIG,"oak_young"));
        if((zone.equals("OPEN")||zone.equals("TEMPERATE")||zone.equals("WET")||zone.equals("BOREAL")||zone.equals("CHERRY"))
            &&temperature>=8&&temperature<=27&&wet<.72&&random.nextDouble()<(zone.equals("BOREAL")?.10:.20)){
            // Existing broadleaf architecture provides distinct fruit trees without falsely
            // assigning pears to beech or plums to pine. Wood remains vanilla oak.
            TreeFruit fruit;
            double roll=random.nextDouble();
            if(zone.equals("OPEN"))fruit=roll<.65?TreeFruit.APPLE:TreeFruit.PEAR;
            else if(zone.equals("WET"))fruit=roll<.65?TreeFruit.PLUM:TreeFruit.PEAR;
            else if(zone.equals("BOREAL"))fruit=roll<.70?TreeFruit.APPLE:TreeFruit.PLUM;
            else fruit=roll<.37?TreeFruit.APPLE:roll<.70?TreeFruit.PEAR:TreeFruit.PLUM;
            return Optional.of(new Plan(fruit,fruit==TreeFruit.PLUM?"oak_young":"oak_spreading"));
        }
        return Optional.empty();
    }
    /** Attach a small group to exposed undersides of the matching crown, never replace leaves or terrain. */
    public static int attach(WorldGenLevel level,BlockPos root,TreeFruit fruit,RandomSource random){
        var state=fruit.block().defaultBlockState();
        int limit=2+random.nextInt(3),count=0,offset=random.nextInt(121);
        for(int i=0;i<121&&count<limit;i++){
            int cell=(i+offset)%121;
            int x=root.getX()+cell%11-5,z=root.getZ()+cell/11-5;
            for(int dy=3;dy<=27;dy++){
                var pos=new BlockPos(x,root.getY()+dy,z);
                if(level.isOutsideBuildHeight(pos.above())||!level.ensureCanWrite(pos)||!level.ensureCanWrite(pos.above()))continue;
                if(!level.getBlockState(pos).isAir()||!level.getFluidState(pos).isEmpty()||!fruit.supports(level.getBlockState(pos.above())))continue;
                var fruitState=state.setValue(HangingFruitBlock.AGE,count==0||random.nextBoolean()?2:1);
                if(!fruitState.canSurvive(level,pos))continue;
                if(level.setBlock(pos,fruitState,Block.UPDATE_CLIENTS)){count++;break;}
            }
        }
        return count;
    }
}
