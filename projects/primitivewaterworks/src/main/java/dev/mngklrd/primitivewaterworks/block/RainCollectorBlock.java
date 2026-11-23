package dev.mngklrd.primitivewaterworks.block;

import dev.mngklrd.primitivewaterworks.PrimitiveWaterworks;
import dev.mngklrd.primitivewaterworks.blockentity.RainCollectorBlockEntity;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class RainCollectorBlock extends BaseEntityBlock {
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST = BooleanProperty.create("east");
    public static final BooleanProperty WEST = BooleanProperty.create("west");
    public static final IntegerProperty WATER_LEVEL = IntegerProperty.create("water_level", 0, 4);
    public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM, new ResourceLocation("tfc", "knives"));
    private static final VoxelShape SHAPE = box(1, 1, 1, 15, 13, 15);

    public RainCollectorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(NORTH, false).setValue(SOUTH, false).setValue(EAST, false)
            .setValue(WEST, false).setValue(WATER_LEVEL, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, WATER_LEVEL);
    }
    public static BooleanProperty side(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case EAST -> EAST;
            case WEST -> WEST;
            default -> null;
        };
    }
    public static boolean hasOutlet(BlockState state, Direction direction) {
        BooleanProperty property = side(direction);
        return property != null && state.getValue(property);
    }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }
    @Nullable @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RainCollectorBlockEntity(pos, state);
    }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide ? null
            : createTickerHelper(type, PrimitiveWaterworks.COLLECTOR_BE.get(), RainCollectorBlockEntity::serverTick);
    }

    @Override public InteractionResult use(BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        ItemStack held = player.getItemInHand(hand);
        BooleanProperty chosenSide = side(hit.getDirection());
        if (chosenSide != null && held.is(KNIVES)) {
            if (!level.isClientSide) {
                boolean opened = !state.getValue(chosenSide);
                level.setBlock(pos, state.setValue(chosenSide, opened), 3);
                if (!player.getAbilities().instabuild)
                    held.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                player.displayClientMessage(Component.translatable(
                    opened ? "message.primitivewaterworks.opened" : "message.primitivewaterworks.closed",
                    hit.getDirection().getName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!held.isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof RainCollectorBlockEntity collector)
            player.displayClientMessage(Component.translatable("message.primitivewaterworks.stored",
                collector.getWater(), collector.getTotalCollected(), collector.getTotalDelivered()), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockEntity(pos) instanceof RainCollectorBlockEntity collector)
            collector.refreshVisual(false);
    }
}
