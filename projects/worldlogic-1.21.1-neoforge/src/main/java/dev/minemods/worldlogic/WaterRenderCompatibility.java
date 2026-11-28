package dev.minemods.worldlogic;

import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.block.state.BlockState;

/** Render equivalence only. Never used by flow, source conversion, buckets or hydration. */
public final class WaterRenderCompatibility {
 private WaterRenderCompatibility(){}
 public static boolean same(Fluid a,Fluid b){
  if(a.isSame(b))return true;
  return a.defaultFluidState().is(FluidTags.WATER)&&b.defaultFluidState().is(FluidTags.WATER)
   &&(NaturalFluids.kind(a)!=dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.UNKNOWN
      ||NaturalFluids.kind(b)!=dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.UNKNOWN);
 }
 /** Renderer-only occlusion for adjacent fluid-containing blocks, including waterlogged vegetation. */
 public static boolean hidesAdjacentFace(FluidState self,BlockState neighbor){
  var other=neighbor.getFluidState();
  return !self.isEmpty()&&!other.isEmpty()&&same(self.getType(),other.getType());
 }
}

