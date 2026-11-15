package dev.minemods.ropefences;

import dev.minemods.ropefences.block.RopeSpanBlock;
import dev.minemods.ropefences.logic.RopeInteractions;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.common.MinecraftForge;

@Mod(RopeFences.MOD_ID)
public final class RopeFences {
    public static final String MOD_ID = "ropefences";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MOD_ID);
    public static final RegistryObject<Block> ROPE_SPAN = BLOCKS.register("rope_span", RopeSpanBlock::new);

    public RopeFences() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        BLOCKS.register(modBus);
        MinecraftForge.EVENT_BUS.register(RopeInteractions.class);
    }
}
