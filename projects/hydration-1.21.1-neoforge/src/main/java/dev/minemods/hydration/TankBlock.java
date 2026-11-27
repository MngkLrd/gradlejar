package dev.minemods.hydration;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class TankBlock extends BaseEntityBlock {
 public enum Kind{
  BASIN(3000,12),BARREL(16000,16),COLLECTOR(8000,16),FILTER(6000,16),INTAKE(1000,10);
  final int capacity,height;Kind(int capacity,int height){this.capacity=capacity;this.height=height;}
 }
 public static final IntegerProperty FILL=IntegerProperty.create("fill",0,3);
 public final Kind kind;
 public TankBlock(Kind kind,BlockBehaviour.Properties properties){super(properties);this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FILL,0));}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return switch(kind){
  case BASIN->simpleCodec(p->new TankBlock(Kind.BASIN,p));case BARREL->simpleCodec(p->new TankBlock(Kind.BARREL,p));
  case COLLECTOR->simpleCodec(p->new TankBlock(Kind.COLLECTOR,p));case FILTER->simpleCodec(p->new TankBlock(Kind.FILTER,p));
  case INTAKE->simpleCodec(p->new TankBlock(Kind.INTAKE,p));};}
 @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(FILL);}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.MODEL;}
 @Override protected VoxelShape getShape(BlockState s,BlockGetter level,BlockPos pos,CollisionContext ctx){return Block.box(1,0,1,15,kind.height,15);}
 @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new TankEntity(p,s);}
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return l.isClientSide?null:createTickerHelper(t,WaterInfrastructure.TANK_ENTITY.get(),TankEntity::tick);}
}
