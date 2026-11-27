package dev.minemods.worldlogicterrain.water;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class FloatingLeafBlock extends WaterlilyBlock {
    public FloatingLeafBlock(Properties properties){super(properties);}
    @Override protected boolean mayPlaceOn(BlockState floor,BlockGetter level,BlockPos pos){
        return AquaticHabitat.supportsFloating(floor);
    }
}
