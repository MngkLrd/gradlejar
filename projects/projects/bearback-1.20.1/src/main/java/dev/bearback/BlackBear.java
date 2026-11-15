package dev.bearback;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BlackBear extends PolarBear {
    public BlackBear(EntityType<? extends PolarBear> type, Level level) {
        super(type, level);
    }

    @Override
    @Nullable
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return Bearback.BLACK_BEAR.get().create(level);
    }
}
