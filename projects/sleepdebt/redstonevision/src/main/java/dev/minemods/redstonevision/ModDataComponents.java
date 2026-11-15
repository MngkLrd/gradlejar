package dev.minemods.redstonevision;

import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    private ModDataComponents() {}
    public static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RedstoneVision.MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FUEL_TICKS =
        COMPONENTS.registerComponentType("fuel_ticks", builder ->
            builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> VISION_ENABLED =
        COMPONENTS.registerComponentType("vision_enabled", builder ->
            builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
}
