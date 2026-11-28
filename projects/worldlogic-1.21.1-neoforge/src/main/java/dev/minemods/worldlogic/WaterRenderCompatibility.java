package dev.minemods.worldlogic;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;

/** Render equivalence only. Never used by flow, source conversion, buckets or hydration. */
public final class WaterRenderCompatibility {
 private WaterRenderCompatibility(){}
 public static boolean same(Fluid a,Fluid b){
  if(a.isSame(b))return true;
  return a.defaultFluidState().is(FluidTags.WATER)&&b.defaultFluidState().is(FluidTags.WATER)
   &&(NaturalFluids.kind(a)!=dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.UNKNOWN
      ||NaturalFluids.kind(b)!=dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.UNKNOWN);
 }
}
