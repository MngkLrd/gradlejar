package dev.minemods.sleepdebt.registry;

import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.blockentity.CoffeeMachineBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
public final class ModBlockEntities {
 public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES=DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE,SleepDebtMod.MOD_ID);
 public static final DeferredHolder<BlockEntityType<?>,BlockEntityType<CoffeeMachineBlockEntity>> COFFEE_MACHINE=
  BLOCK_ENTITIES.register("coffee_machine",()->BlockEntityType.Builder.of(CoffeeMachineBlockEntity::new,ModBlocks.COFFEE_MACHINE.get()).build(null));
 private ModBlockEntities(){}
}

