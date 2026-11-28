package dev.minemods.worldlogic;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
/** Grades are persisted in the block palette; player placement defaults to poor.
 * Loot, not block hardness or generation density, determines the raw yield. */
public final class MineralOreBlock extends Block {
 public static final IntegerProperty RICHNESS=IntegerProperty.create("richness",1,3);
 private final MineralDeposits.Mineral mineral;
 public MineralOreBlock(MineralDeposits.Mineral mineral,BlockBehaviour.Properties properties){
  super(properties);this.mineral=mineral;registerDefaultState(defaultBlockState().setValue(RICHNESS,1));
 }
 public MineralDeposits.Mineral mineral(){return mineral;}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(RICHNESS);}
}
