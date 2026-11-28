package dev.minemods.worldlogic;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
/** A shaderpack's own mapping supplies the numeric IDs. Never changes blocks or gameplay tags. */
public final class ShaderPlantMapping{
 private ShaderPlantMapping(){}
 public static final TagKey<Block> GRASS=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("worldlogic","shader/waving_grass"));
 public static final TagKey<Block> TALL=TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("worldlogic","shader/waving_tall"));
 public static void apply(Object2IntMap<BlockState> ids){
  // Iris resolves rendered fluids through their legacy block-state material.
  // Match the pack's own water ID, not a hardcoded shader ID.
  var water=Blocks.WATER.defaultBlockState();
  int waterId=ids.containsKey(water)?ids.getInt(water):-1;
  for(var f:NaturalFluids.families())for(var state:f.block.get().getStateDefinition().getPossibleStates()){
   var vanilla=water.setValue(LiquidBlock.LEVEL,state.getValue(LiquidBlock.LEVEL));
   inherit(ids,state,ids.containsKey(vanilla)?ids.getInt(vanilla):waterId);
  }
  var shortGrass=Blocks.SHORT_GRASS.defaultBlockState();
  int grass=ids.containsKey(shortGrass)?ids.getInt(shortGrass):-1;
  for(var b:BuiltInRegistries.BLOCK)for(var s:b.getStateDefinition().getPossibleStates()){
   if(ids.containsKey(s)||(!s.is(GRASS)&&!s.is(TALL)))continue;
   int target=grass;
   if(s.is(TALL)&&s.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)){
    var vanilla=Blocks.TALL_GRASS.defaultBlockState().setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,s.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF));
    if(ids.containsKey(vanilla))target=ids.getInt(vanilla);
   }
   inherit(ids,s,target);
  }
 }
 static void inherit(Object2IntMap<BlockState> ids,BlockState state,int target){if(target>=0&&!ids.containsKey(state))ids.put(state,target);}
 public static void verify(){
  var ids=new it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap<BlockState>();
  var explicit=Blocks.DANDELION.defaultBlockState();var unmapped=Blocks.POPPY.defaultBlockState();
  ids.put(explicit,42);inherit(ids,explicit,12);inherit(ids,unmapped,-1);
  if(ids.getInt(explicit)!=42||ids.containsKey(unmapped))throw new IllegalStateException("shader mapping override or fabricated ID");
  inherit(ids,unmapped,12);if(ids.getInt(unmapped)!=12)throw new IllegalStateException("shader inheritance");
  var water=Blocks.WATER.defaultBlockState();
  ids.put(water,100);
  var nativeWater=NaturalFluids.get(dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.SEA).block.get().defaultBlockState();
  var explicitNative=NaturalFluids.get(dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.RIVER).block.get().defaultBlockState();
  ids.put(explicitNative,101);apply(ids);
  if(ids.getInt(nativeWater)!=100||ids.getInt(explicitNative)!=101)throw new IllegalStateException("water shader inheritance");
  if(ids.containsKey(Blocks.LAVA.defaultBlockState()))throw new IllegalStateException("lava shader alias");
 }
}
