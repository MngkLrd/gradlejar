package dev.minemods.aquaculture;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Per-fish feeding state. Never spawns fish or increments future population ledgers. */
public final class FishFeeding {
 private FishFeeding(){}
 public static final long DIGEST_TICKS=4800L;
 public static final int FEED_COOLDOWN=100;
 public enum Result { FED, WRONG_FOOD, DRY, FULL, COOLDOWN }
 public static TagKey<Item> foodTag(Species s){
  return TagKey.create(Registries.ITEM,ResourceLocation.fromNamespaceAndPath(Aquaculture.ID,"fish_food/"+s.id()));
 }
 public static int satiety(Fish fish,long now){
  long remaining=Math.max(0,fish.getPersistentData().getLong("wl_fish_fed_until")-now);
  return (int)Math.min(100,(remaining*100L+DIGEST_TICKS-1)/DIGEST_TICKS);
 }
 public static boolean mature(Fish fish,Species s){
  return Aquaculture.traits(fish,s).getInt("wl_fish_age_years")>=s.maturityYears();
 }
 public static Result offer(Fish fish,Species s,ItemStack stack,boolean creative){
  if(stack.isEmpty()||!stack.is(foodTag(s)))return Result.WRONG_FOOD;
  if(!(fish.level() instanceof ServerLevel level)||!fish.isAlive()||!Aquaculture.water(level,fish.blockPosition()))return Result.DRY;
  long now=level.getGameTime();
  var tag=Aquaculture.traits(fish,s);
  long remaining=Math.max(0,Math.min(DIGEST_TICKS,tag.getLong("wl_fish_fed_until")-now));
  if(remaining>=DIGEST_TICKS*3/4)return Result.FULL;
  if(tag.contains("wl_fish_last_feed")&&now-tag.getLong("wl_fish_last_feed")<FEED_COOLDOWN)return Result.COOLDOWN;
  // A portion restores at most 50%; state persists in entity NeoForge NBT.
  tag.putLong("wl_fish_fed_until",now+Math.min(DIGEST_TICKS,remaining+DIGEST_TICKS/2));
  tag.putLong("wl_fish_last_feed",now);
  if(!creative)stack.shrink(1);
  return Result.FED;
 }
}
