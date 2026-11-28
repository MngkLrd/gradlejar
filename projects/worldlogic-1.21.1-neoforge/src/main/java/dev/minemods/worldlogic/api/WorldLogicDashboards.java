package dev.minemods.worldlogic.api;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
/** Server-authoritative addon statistics, drawn and synchronized by Core. No gameplay mutations. */
public final class WorldLogicDashboards{
 private WorldLogicDashboards(){}
 public static final int API_VERSION=1,API_REVISION=0;
 @FunctionalInterface public interface Catalog{List<Snapshot> snapshots(ServerPlayer player);}
 public static final WorldLogicCapabilities.Key<Catalog> CAPABILITY=new WorldLogicCapabilities.Key<>(ResourceLocation.fromNamespaceAndPath("worldlogic","dashboards"),API_VERSION,Catalog.class);
 public record Row(String label,String value,double fraction){
  public Row{Objects.requireNonNull(label);Objects.requireNonNull(value);if(label.isBlank()||label.length()>128||value.length()>128||(!Double.isNaN(fraction)&&(!Double.isFinite(fraction)||fraction<0||fraction>1)))throw new IllegalArgumentException("dashboard row");}
 }
 public record Section(String title,List<Row> rows){
  public Section{Objects.requireNonNull(title);rows=List.copyOf(rows);if(title.isBlank()||title.length()>128||rows.size()>128)throw new IllegalArgumentException("dashboard section");}
 }
 public record Snapshot(ResourceLocation id,int order,String title,ResourceLocation icon,List<Section> sections){
  public Snapshot{Objects.requireNonNull(id);Objects.requireNonNull(title);Objects.requireNonNull(icon);sections=List.copyOf(sections);if(title.isBlank()||title.length()>128||sections.size()>16)throw new IllegalArgumentException("dashboard");}
 }
 private static final Map<ResourceLocation,Function<ServerPlayer,Snapshot>> PROVIDERS=new ConcurrentHashMap<>();
 public static void register(ResourceLocation id,Function<ServerPlayer,Snapshot> provider){
  if(PROVIDERS.putIfAbsent(Objects.requireNonNull(id),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("duplicate dashboard "+id);
 }
 public static List<Snapshot> snapshots(ServerPlayer player){
  if(!player.serverLevel().getServer().isSameThread())throw new IllegalStateException("server thread required");
  var out=new ArrayList<Snapshot>();
  for(var e:PROVIDERS.entrySet())try{
   var s=e.getValue().apply(player);
   if(s!=null&&s.id().equals(e.getKey()))out.add(s);
  }catch(RuntimeException|LinkageError ex){org.slf4j.LoggerFactory.getLogger("WorldLogic").debug("Dashboard provider unavailable: "+e.getKey(),ex);}
  return out.stream().sorted(Comparator.comparingInt(Snapshot::order).thenComparing(Snapshot::id)).limit(12).toList();
 }
 public static boolean registered(ResourceLocation id){return PROVIDERS.containsKey(id);}
 public static void verify(){
  var list=new ArrayList<Row>();list.add(new Row("Health","5/10",.5));
  var section=new Section("Overview",list);list.clear();
  if(section.rows().size()!=1)throw new IllegalStateException("dashboard snapshot mutable");
  boolean reject=false;try{new Row("Invalid","",1.01);}catch(IllegalArgumentException ex){reject=true;}
  if(!reject)throw new IllegalStateException("invalid dashboard fraction");
 }
}
