package dev.minemods.worldlogic.api;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
public final class WorldLogicCapabilities {
 private WorldLogicCapabilities(){}
 public record Key<T>(ResourceLocation id,int version,Class<T> contract){
  public Key{Objects.requireNonNull(id);Objects.requireNonNull(contract);if(version<1)throw new IllegalArgumentException("version");}
 }
 public record Descriptor(ResourceLocation id,int version,int revision,String provider){}
 private record Entry(Descriptor descriptor,Class<?> contract,Object instance){}
 private static final Map<ResourceLocation,Entry> ENTRIES=new ConcurrentHashMap<>();
 public static <T> void register(Key<T> key,int revision,String provider,T instance){
  if(revision<0||!key.id().getNamespace().equals(provider))throw new IllegalArgumentException("provider identity");
  var e=new Entry(new Descriptor(key.id(),key.version(),revision,provider),key.contract(),key.contract().cast(Objects.requireNonNull(instance)));
  if(ENTRIES.putIfAbsent(key.id(),e)!=null)throw new IllegalArgumentException("duplicate capability "+key.id());
 }
 public static <T> Optional<T> find(Key<T> key,int minimumRevision){
  var e=ENTRIES.get(key.id());
  return e==null||e.descriptor.version()!=key.version()||e.descriptor.revision()<minimumRevision||e.contract!=key.contract()?Optional.empty():Optional.of(key.contract().cast(e.instance));
 }
 public static List<Descriptor> available(){return ENTRIES.values().stream().map(Entry::descriptor).sorted(Comparator.comparing(x->x.id().toString())).toList();}
 public static void verify(){
  var key=new Key<>(ResourceLocation.fromNamespaceAndPath("worldlogic","qa_capability"),1,String.class);
  register(key,2,"worldlogic","fixture");
  try{if(!find(key,2).orElseThrow().equals("fixture")||find(key,3).isPresent()||find(new Key<>(key.id(),2,String.class),0).isPresent()||find(new Key<>(key.id(),1,Integer.class),0).isPresent())throw new IllegalStateException("capability negotiation");}
  finally{ENTRIES.remove(key.id());}
 }
}
