package dev.anvilborn.registry;

import com.mojang.serialization.Codec;
import dev.anvilborn.Anvilborn;
import dev.anvilborn.forge.ForgeData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;

public final class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> REGISTER =
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Anvilborn.MODID);

    public static final Supplier<DataComponentType<ForgeData>> FORGE_DATA = REGISTER.register(
        "forge_data", () -> DataComponentType.<ForgeData>builder().persistent(ForgeData.CODEC).build()
    );

    private ModComponents() {}
}
