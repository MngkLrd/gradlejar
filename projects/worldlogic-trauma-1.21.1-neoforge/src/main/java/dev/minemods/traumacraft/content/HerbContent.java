package dev.minemods.traumacraft.content;
import dev.minemods.traumacraft.TraumaCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import java.util.function.Supplier;
public final class HerbContent {
    private static final DeferredRegister.Blocks BLOCKS=DeferredRegister.createBlocks(TraumaCraft.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,TraumaCraft.MOD_ID);
    public static final DeferredBlock<HerbWorkpieceBlock> WORKPIECE=BLOCKS.register("herb_workpiece",()->new HerbWorkpieceBlock(BlockBehaviour.Properties.of().strength(-1,6).noCollission().noOcclusion().noLootTable().pushReaction(PushReaction.BLOCK).sound(SoundType.GRASS)));
    public static final Supplier<BlockEntityType<HerbWorkpieceEntity>> ENTITY=ENTITIES.register("herb_workpiece",()->BlockEntityType.Builder.of(HerbWorkpieceEntity::new,WORKPIECE.get()).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);}
}
