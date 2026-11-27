package dev.minemods.worldlogicterrain.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Registry IDs are unchanged from 0.4.0; old blocks default to ripe. */
public final class BerryBushBlock extends BushBlock {
    public static final MapCodec<BerryBushBlock> CODEC=simpleCodec(BerryBushBlock::new);
    public static final BooleanProperty RIPE=BooleanProperty.create("ripe");
    private static final VoxelShape SHAPE=Block.box(1,0,1,15,14,15);
    public BerryBushBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RIPE,true));
    }
    @Override public MapCodec<BerryBushBlock> codec(){return CODEC;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(RIPE);}
    @Override protected boolean mayPlaceOn(BlockState floor,BlockGetter level,BlockPos pos){return super.mayPlaceOn(floor,level,pos)||floor.is(Blocks.MUD);}
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext ctx){return SHAPE;}
    @Override public InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit){
        if(!state.getValue(RIPE))return InteractionResult.PASS;
        if(level.isClientSide)return InteractionResult.SUCCESS;
        // Check the authoritative state, not the previously supplied state.
        BlockState current=level.getBlockState(pos);
        if(!current.is(this)||!current.getValue(RIPE)||player.isSpectator()||!player.mayBuild())return InteractionResult.PASS;
        if(!level.setBlock(pos,current.setValue(RIPE,false),Block.UPDATE_ALL))return InteractionResult.PASS;
        var id=BuiltInRegistries.BLOCK.getKey(this);
        var food=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),id.getPath().substring(5)));
        Block.popResource(level,pos,new ItemStack(food));
        var seed=BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),id.getPath().substring(5)+"_seeds"));
        Block.popResource(level,pos,new ItemStack(seed));
        level.playSound(null,pos,SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES,SoundSource.BLOCKS,1.0F,0.9F+level.random.nextFloat()*0.2F);
        return InteractionResult.CONSUME;
    }
    @Override public void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!state.getValue(RIPE)&&level.getRawBrightness(pos.above(),0)>=9&&random.nextInt(5)==0)
            level.setBlock(pos,state.setValue(RIPE,true),Block.UPDATE_ALL);
    }
}
