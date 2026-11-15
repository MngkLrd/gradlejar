package dev.minemods.sleepdebt.registry;
import dev.minemods.sleepdebt.SleepDebtMod;
import dev.minemods.sleepdebt.entity.SleeplessEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
public final class ModEntities {
 public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(Registries.ENTITY_TYPE,SleepDebtMod.MOD_ID);
 public static final DeferredHolder<EntityType<?>,EntityType<SleeplessEntity>> SLEEPLESS=ENTITIES.register("sleepless",
   ()->EntityType.Builder.of(SleeplessEntity::new,MobCategory.MONSTER).sized(.65f,1.95f).clientTrackingRange(8).build("sleepdebt:sleepless"));
 private ModEntities(){}
}

