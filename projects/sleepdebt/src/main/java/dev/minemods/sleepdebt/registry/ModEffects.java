package dev.minemods.sleepdebt.registry;
import dev.minemods.sleepdebt.SleepDebtMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Status icons mirror server-owned timed states; penalties are applied once by SleepEffects. */
public final class ModEffects {
 public static final DeferredRegister<MobEffect> EFFECTS=DeferredRegister.create(Registries.MOB_EFFECT,SleepDebtMod.MOD_ID);
 public static final DeferredHolder<MobEffect,MobEffect> ALERTNESS=EFFECTS.register("alertness",()->new DrinkEffect(MobEffectCategory.BENEFICIAL,0xd6a452));
 public static final DeferredHolder<MobEffect,MobEffect> CRASH=EFFECTS.register("caffeine_crash",()->new DrinkEffect(MobEffectCategory.HARMFUL,0x8c535b));
 public static final DeferredHolder<MobEffect,MobEffect> CALM=EFFECTS.register("calm",()->new DrinkEffect(MobEffectCategory.BENEFICIAL,0x8cae78));
 private static final class DrinkEffect extends MobEffect {
  private DrinkEffect(MobEffectCategory category,int color){super(category,color);}
 }
 private ModEffects(){}
}
