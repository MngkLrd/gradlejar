package dev.minemods.worldlogic;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.*;
public final class GeoDensity implements DensityFunction {
 public static final DeferredRegister<MapCodec<? extends DensityFunction>> TYPES=DeferredRegister.create(Registries.DENSITY_FUNCTION_TYPE,"worldlogic");
 public static final MapCodec<GeoDensity> CODEC=RecordCodecBuilder.mapCodec(i->i.group(
  Codec.STRING.fieldOf("field").forGetter(g->g.field),
  NoiseHolder.CODEC.fieldOf("continental").forGetter(g->g.continental),
  NoiseHolder.CODEC.fieldOf("tectonic").forGetter(g->g.tectonic),
  NoiseHolder.CODEC.fieldOf("detail").forGetter(g->g.detail),
  NoiseHolder.CODEC.fieldOf("moisture").forGetter(g->g.moisture)
 ).apply(i,GeoDensity::new));
 static{TYPES.register("geography",()->CODEC);}
 private final String field;
 private final NoiseHolder continental,tectonic,detail,moisture;
 private final GeoModel model;
 private record Key(NoiseHolder c,NoiseHolder t,NoiseHolder d,NoiseHolder m){}
 private static final LinkedHashMap<Key,GeoModel> MODELS=new LinkedHashMap<>(8,.75f,true);
 private static synchronized GeoModel model(NoiseHolder c,NoiseHolder t,NoiseHolder d,NoiseHolder m){
  Key k=new Key(c,t,d,m);GeoModel existing=MODELS.get(k);if(existing!=null)return existing;
  if(MODELS.size()>=8)MODELS.remove(MODELS.keySet().iterator().next());
  GeoModel value=new GeoModel(c,t,d,m);MODELS.put(k,value);return value;
 }
 public GeoDensity(String field,NoiseHolder continental,NoiseHolder tectonic,NoiseHolder detail,NoiseHolder moisture){
  if(!Set.of("offset","continents","erosion","ridges","temperature","humidity","climate_temperature","climate_humidity").contains(field))throw new IllegalArgumentException("Unknown geography field: "+field);
  this.field=field;this.continental=continental;this.tectonic=tectonic;this.detail=detail;this.moisture=moisture;this.model=model(continental,tectonic,detail,moisture);
 }
 /** Internal server adapter for the public API; uses the exact existing seeded noise instances. */
 public static java.util.Optional<dev.minemods.worldlogic.api.WorldLogicApi.RegionalClimate> regional(net.minecraft.server.level.ServerLevel level,int x,int z){
  var cache=level.getChunkSource();
  if(!(cache.getGenerator() instanceof net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator gen)
    ||!gen.generatorSettings().unwrapKey().map(k->k.location().equals(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("worldlogic","earthlike_climate"))).orElse(false))
   return java.util.Optional.empty();
  var state=cache.randomState();var registry=level.registryAccess().lookupOrThrow(Registries.NOISE);
  java.util.function.Function<net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.synth.NormalNoise.NoiseParameters>,NoiseHolder> noise=
   k->new NoiseHolder(registry.getOrThrow(k),state.getOrCreateNoise(k));
  var c=noise.apply(net.minecraft.world.level.levelgen.Noises.CONTINENTALNESS);
  var t=noise.apply(net.minecraft.world.level.levelgen.Noises.RIDGE);
  var de=noise.apply(net.minecraft.world.level.levelgen.Noises.EROSION);
  var m=noise.apply(net.minecraft.world.level.levelgen.Noises.VEGETATION);
  return java.util.Optional.of(model(c,t,de,m).regional(x,z));
 }
 @Override public double compute(FunctionContext c){return model.value(field,c.blockX(),c.blockZ());}
 @Override public void fillArray(double[] a,ContextProvider p){p.fillAllDirectly(a,this);}
 @Override public DensityFunction mapAll(Visitor v){return v.apply(new GeoDensity(field,v.visitNoise(continental),v.visitNoise(tectonic),v.visitNoise(detail),v.visitNoise(moisture)));}
 @Override public double minValue(){return -2;}
 @Override public double maxValue(){return 2;}
 @Override public KeyDispatchDataCodec<? extends DensityFunction> codec(){return KeyDispatchDataCodec.of(CODEC);}
}
