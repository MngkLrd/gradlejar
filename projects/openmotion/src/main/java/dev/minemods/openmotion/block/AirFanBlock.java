package dev.minemods.openmotion.block;

import com.mojang.serialization.MapCodec;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Blows light entities away in a narrow cone, no block entity or per-entity allocations kept. */
public final class AirFanBlock extends HorizontalMachineBlock {
    public static final MapCodec<AirFanBlock> CODEC = simpleCodec(AirFanBlock::new);

    @Override
    protected MapCodec<AirFanBlock> codec() {
        return CODEC;
    }

    private static final int UPDATE_TICKS = 3;
    private static final double RANGE = 5.0;
    private static final double HALF_WIDTH = 0.85;

    public AirFanBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            level.scheduleTick(pos, this, UPDATE_TICKS);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block fromBlock, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (state.getValue(POWERED) != powered) {
            level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // The fan is normally active. Redstone powers it OFF, allowing simple shutoff switches.
        if (!state.getValue(POWERED)) {
            blow(level, pos, state.getValue(FACING));
        }
        level.scheduleTick(pos, this, UPDATE_TICKS);
    }

    private static void blow(ServerLevel level, BlockPos pos, Direction facing) {
        Vec3 axis = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 origin = Vec3.atCenterOf(pos).add(axis.scale(0.55));
        Vec3 middle = origin.add(axis.scale(RANGE * 0.5));
        AABB search = new AABB(middle, middle).inflate(
                facing.getAxis() == Direction.Axis.X ? RANGE * 0.5 : HALF_WIDTH + 0.7,
                HALF_WIDTH + 0.7,
                facing.getAxis() == Direction.Axis.Z ? RANGE * 0.5 : HALF_WIDTH + 0.7);
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, search,
                e -> e.isAlive() && !e.isSpectator() && !e.isPassenger());

        int processed = 0;
        for (Entity entity : entities) {
            if (processed++ >= 48) break; // hard cap for modpack mob farms
            Vec3 target = entity.position().add(0, entity.getBbHeight() * 0.5, 0);
            Vec3 offset = target.subtract(origin);
            double distance = offset.dot(axis);
            if (distance <= 0 || distance > RANGE) continue;
            double radialSquared = offset.lengthSqr() - distance * distance;
            if (radialSquared > HALF_WIDTH * HALF_WIDTH) continue;
            BlockHitResult hit = level.clip(new ClipContext(origin, target,
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, entity));
            if (hit.getType() != HitResult.Type.MISS) continue;
            double force = entity instanceof ItemEntity ? 0.15 : 0.085;
            Vec3 motion = entity.getDeltaMovement();
            double forwardSpeed = motion.dot(axis);
            if (forwardSpeed < 0.50) {
                entity.setDeltaMovement(motion.add(axis.scale(Math.min(force, 0.50 - forwardSpeed))));
                entity.hurtMarked = true;
            }
        }

        if (level.getGameTime() % 9 == 0) {
            double s = 0.75 + level.random.nextDouble() * 3.65;
            Vec3 point = origin.add(axis.scale(s));
            level.sendParticles(ParticleTypes.CLOUD, point.x, point.y, point.z,
                    1, 0.12, 0.12, 0.12, 0.005);
        }
    }
}
