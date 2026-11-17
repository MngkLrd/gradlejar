package com.kirill.lurelantern.block;

import com.kirill.lurelantern.blockentity.LureLanternBlockEntity;
import com.kirill.lurelantern.registry.ModBlockEntities;
import com.kirill.lurelantern.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class LureLanternBlock extends LanternBlock implements EntityBlock {
    public static final MapCodec<LanternBlock> CODEC = simpleCodec(LureLanternBlock::new);
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public LureLanternBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    @Override public MapCodec<LanternBlock> codec() { return CODEC; }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!level.isClientSide()) {
            if (isAdvancedLantern(state) && level.getBlockEntity(pos) instanceof LureLanternBlockEntity lantern
                    && lantern.hasCapturedMob()) {
                if (player.isShiftKeyDown() && level instanceof ServerLevel server) {
                    boolean released = lantern.releaseCapturedMob(server, pos);
                    level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F,
                            released ? 1.35F : 0.65F);
                    player.displayClientMessage(Component.translatable(released
                            ? "message.lurelantern.mob_released" : "message.lurelantern.mob_release_failed"), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.lurelantern.mob_inside",
                            lantern.getCapturedMobName()), true);
                }
                return ItemInteractionResult.SUCCESS;
            }
            boolean active = !state.getValue(ACTIVE);
            level.setBlock(pos, state.setValue(ACTIVE, active), 3);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6F,
                    active ? 1.35F : 0.65F);
            player.displayClientMessage(Component.translatable(
                    active ? "message.lurelantern.lure_on" : "message.lurelantern.lure_off"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                                      LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide() && isAdvancedLantern(state)
                && level.getBlockEntity(pos) instanceof LureLanternBlockEntity lantern) {
            lantern.loadCapturedMobFromStack(stack);
        }
    }

    @Override public ItemStack getCloneItemStack(BlockState state, HitResult target,
            LevelReader level, BlockPos pos, Player player) {
        ItemStack stack = new ItemStack(this);
        if (isAdvancedLantern(state) && level.getBlockEntity(pos) instanceof LureLanternBlockEntity lantern)
            lantern.saveCapturedMobToStack(stack);
        return stack;
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LureLanternBlockEntity(pos, state);
    }

    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.LURE_LANTERN.get()) return null;
        return (lvl, pos, s, entity) ->
                LureLanternBlockEntity.serverTick(lvl, pos, s, (LureLanternBlockEntity) entity);
    }

    public static boolean isAdvancedLantern(BlockState state) {
        return state.is(ModBlocks.ADVANCED_LURE_LANTERN.get());
    }
}
