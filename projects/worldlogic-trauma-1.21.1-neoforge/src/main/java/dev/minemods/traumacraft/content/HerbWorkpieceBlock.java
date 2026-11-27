package dev.minemods.traumacraft.content;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;
public final class HerbWorkpieceBlock extends Block implements EntityBlock {
    public static final TagKey<Block> SURFACES=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("traumacraft","herb_surfaces"));
    public static final TagKey<Block> DRYING_LOGS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("traumacraft","drying_logs"));
    public static boolean dryingSurface(LevelReader level,BlockPos pos){var s=level.getBlockState(pos);return s.is(DRYING_LOGS)&&s.isFaceSturdy(level,pos,Direction.UP);}
    private static final VoxelShape SHAPE=Block.box(0,0,0,16,2,16);
    public HerbWorkpieceBlock(Properties properties){super(properties);}
    public static boolean surface(LevelReader level,BlockPos pos){
        var s=level.getBlockState(pos);return s.is(SURFACES)&&s.isFaceSturdy(level,pos,Direction.UP);
    }
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return SHAPE;}
    @Override protected boolean canSurvive(BlockState s,LevelReader l,BlockPos p){return surface(l,p.below())||dryingSurface(l,p.below());}
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState neighbor,LevelAccessor l,BlockPos p,BlockPos other){
        return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,neighbor,l,p,other);
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return level.isClientSide||type!=HerbContent.ENTITY.get()?null:(l,p,s,be)->{if(be instanceof HerbWorkpieceEntity work)work.dryTick();};
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new HerbWorkpieceEntity(pos,state);}
    @Override protected void onRemove(BlockState old,Level level,BlockPos pos,BlockState next,boolean moving){
        if(!old.is(next.getBlock())&&!level.isClientSide&&level.getBlockEntity(pos) instanceof HerbWorkpieceEntity work&&!work.emptySurface())
            Block.popResource(level,pos,work.result());
        super.onRemove(old,level,pos,next,moving);
    }
}
