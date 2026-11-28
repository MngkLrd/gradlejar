package dev.minemods.aquaculture;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import java.util.List;
/** Remove only the four vanilla fish from natural candidate lists; never delete existing fish. */
final class AquaSpawns {
 static boolean vanillaFish(EntityType<?> type){return type==EntityType.COD||type==EntityType.SALMON||type==EntityType.TROPICAL_FISH||type==EntityType.PUFFERFISH;}
 static void filter(LevelEvent.PotentialSpawns event){
  for(var data:List.copyOf(event.getSpawnerDataList()))if(vanillaFish(data.type))event.removeSpawnerData(data);
 }
 static void placement(MobSpawnEvent.SpawnPlacementCheck event){
  if(vanillaFish(event.getEntityType())&&(event.getSpawnType()==MobSpawnType.NATURAL||event.getSpawnType()==MobSpawnType.CHUNK_GENERATION))
   event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
 }
}
