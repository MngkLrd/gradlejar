package dev.minemods.aquaculture.api;

import dev.minemods.aquaculture.FishPortion;
import dev.minemods.worldlogic.api.WorldLogicCapabilities;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Optional;

/** Public read-only edible serving contract. Does not report ingestion or grant nutrients. */
public final class AquaApi {
 private AquaApi(){}
 public static final int API_VERSION=1, API_REVISION=0;
 public enum Preparation { RAW, COOKED }
 public record FoodPortion(String speciesId,int edibleMassGrams,Preparation preparation){}
 @FunctionalInterface public interface FoodContract { Optional<FoodPortion> foodPortion(ItemStack stack); }
 public static final WorldLogicCapabilities.Key<FoodContract> FOOD_PORTION=new WorldLogicCapabilities.Key<>(ResourceLocation.fromNamespaceAndPath("worldlogic_aqua","food_portion"),API_VERSION,FoodContract.class);
 /** One item, regardless of stack count. Empty for whole fish, blank/invalid portions or other items. Safe on either logical side; no mutation. */
 public static Optional<FoodPortion> foodPortion(ItemStack stack){
  if(stack==null||stack.isEmpty()||!(stack.getItem() instanceof FishPortion item))return Optional.empty();
  return item.describe(stack);
 }
 public static void register(){WorldLogicCapabilities.register(FOOD_PORTION,API_REVISION,"worldlogic_aqua",AquaApi::foodPortion);}
}
