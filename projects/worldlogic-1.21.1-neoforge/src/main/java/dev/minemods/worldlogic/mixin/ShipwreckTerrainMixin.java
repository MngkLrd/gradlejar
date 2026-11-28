package dev.minemods.worldlogic.mixin;
import dev.minemods.worldlogic.NaturalWaterGeneration;
import java.util.Optional;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.structures.ShipwreckStructure;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ShipwreckStructure.class)
public abstract class ShipwreckTerrainMixin{
 @Shadow @Final private boolean isBeached;
 @Inject(method="findGenerationPoint",at=@At("HEAD"),cancellable=true)
 private void worldlogic$physicalShore(Structure.GenerationContext c,CallbackInfoReturnable<Optional<Structure.GenerationStub>> cir){
  var g=c.chunkGenerator();if(!NaturalWaterGeneration.enabled(g))return;
  int x=c.chunkPos().getMiddleBlockX(),z=c.chunkPos().getMiddleBlockZ(),sea=g.getSeaLevel();
  int center=g.getBaseHeight(x,z,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState());
  if(!isBeached&&center>=sea-2){cir.setReturnValue(Optional.empty());return;}
  if(isBeached){
   if(center<sea-3||center>sea+3){cir.setReturnValue(Optional.empty());return;}
   boolean water=false;
   for(int dx:new int[]{-12,0,12})for(int dz:new int[]{-12,0,12})
    if(g.getBaseHeight(x+dx,z+dz,Heightmap.Types.OCEAN_FLOOR_WG,c.heightAccessor(),c.randomState())<sea-2)water=true;
   if(!water)cir.setReturnValue(Optional.empty());
  }
 }
}
