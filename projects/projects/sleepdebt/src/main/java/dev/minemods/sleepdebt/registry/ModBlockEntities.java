package dev.minemods.sleepdebt.registry;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.blockentity.CoffeeMachineBlockEntity;
import dev.minemods.sleepdebt.blockentity.IncenseBurnerBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=
  DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,SleepDebtMod.MOD_ID);
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CoffeeMachineBlockEntity>> COFFEE_MACHINE=
  BLOCK_ENTITIES.register("coffee_machine",()->BlockEntityType.Builder.of(
   CoffeeMachineBlockEntity::new,ModBlocks.COFFEE_MACHINE.get()).build(null));
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<IncenseBurnerBlockEntity>> INCENSE_BURNER=
  BLOCK_ENTITIES.register("incense_burner",()->BlockEntityType.Builder.of(
   IncenseBurnerBlockEntity::new,ModBlocks.INCENSE_BURNER.get()).build(null));
 private ModBlockEntities(){}
}

