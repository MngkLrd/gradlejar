package dev.minemods.worldlogic;
import dev.minemods.worldlogic.api.WorldLogicDashboards;
import dev.minemods.worldlogic.api.WorldLogicDashboards.*;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Migration bridge for inspected public addon APIs, no private fields, gameplay writes or addon dependencies.
 * A registered typed provider replaces the corresponding bridge. */
final class PeerDashboardSnapshots {
 private PeerDashboardSnapshots(){}
 private record Peer(String mod,String api,String title,String icon,int order,List<String> queries){}
 private static final List<Peer> PEERS=List.of(
  new Peer("metabolism","dev.minemods.metabolism.api.MetabolismApi","worldlogic.stats.metabolism","minecraft:cooked_beef",30,List.of("physiology","nutrients","digestion","clinical")),
  new Peer("worldlogic_trauma","dev.minemods.traumacraft.api.TraumaApi","worldlogic.stats.trauma","minecraft:redstone",40,List.of("status","physiology","illnessStatus","passiveRecoveryHpPerMinute")),
  new Peer("worldlogic_thermal","dev.minemods.thermal.api.ThermalApi","worldlogic.stats.temperature","minecraft:blaze_powder",50,List.of("status","physiology","roomClimate")));
 private static final Map<String,Map<String,Method>> METHODS=new HashMap<>();
 private static final Set<String> WARNED=new HashSet<>();
 private static ResourceLocation id(Peer p){return ResourceLocation.fromNamespaceAndPath(p.mod(),"dashboard");}
 private static Map<String,Method> methods(Peer p){
  return METHODS.computeIfAbsent(p.mod(),k->{
   Map<String,Method> map=new LinkedHashMap<>();
   try{
    Class<?> c=Class.forName(p.api());
    if(c.getField("API_VERSION").getInt(null)!=1)return Map.of();
    for(String q:p.queries())try{map.put(q,c.getMethod(q,ServerPlayer.class));}catch(NoSuchMethodException ignored){}
   }catch(ReflectiveOperationException|LinkageError ex){if(WARNED.add(p.mod()))org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Dashboard API unavailable for "+p.mod(),ex);}
   return map;
  });
 }
 static List<Snapshot> collect(ServerPlayer player){
  var out=new ArrayList<>(WorldLogicDashboards.snapshots(player));
  for(var p:PEERS){
   if(!ModList.get().isLoaded(p.mod())||WorldLogicDashboards.registered(id(p)))continue;
   List<Section> sections=new ArrayList<>();
   for(var e:methods(p).entrySet())try{
    var rows=new ArrayList<Row>();flatten(e.getValue().invoke(null,player),"",rows,0);
    if(!rows.isEmpty())sections.add(new Section("worldlogic.stats.section."+e.getKey(),rows));
   }catch(ReflectiveOperationException|RuntimeException|LinkageError ex){if(WARNED.add(p.mod()+"/"+e.getKey()))org.slf4j.LoggerFactory.getLogger("WorldLogic").warn("Dashboard query unavailable: "+p.mod()+"/"+e.getKey(),ex);}
   out.add(new Snapshot(id(p),p.order(),p.title(),ResourceLocation.parse(p.icon()),sections));
  }
  return out.stream().sorted(Comparator.comparingInt(Snapshot::order).thenComparing(Snapshot::id)).limit(12).toList();
 }
 private static String bounded(String s){return s.length()>128?s.substring(0,128):s;}
 private static void flatten(Object value,String key,List<Row> rows,int depth)throws ReflectiveOperationException{
  if(value==null||depth>5||rows.size()>=128)return;
  if(value instanceof Optional<?> opt){if(opt.isPresent())flatten(opt.get(),key,rows,depth+1);return;}
  if(value instanceof Collection<?> list){
   int i=0;for(Object v:list){if(rows.size()>=128)break;flatten(v,key.isEmpty()?Integer.toString(++i):key+"."+ ++i,rows,depth+1);}return;
  }
  if(value.getClass().isRecord()){
   for(var component:value.getClass().getRecordComponents()){
    String child=key.isEmpty()?component.getName():key+"."+component.getName();
    flatten(component.getAccessor().invoke(value),child,rows,depth+1);
   }return;
  }
  String v;if(value instanceof Number n){
   double d=n.doubleValue();
   v=!Double.isFinite(d)||d==-1?"?":value instanceof Integer||value instanceof Long?value.toString():String.format(Locale.ROOT,"%.2f",d);
   String last=key.substring(key.lastIndexOf('.')+1);
   double f=Double.isFinite(d)&&d>=0&&d<=1&&Set.of("integrity","function","oxygenFraction","dietQuality","absorptionEfficiency","adequacy","ventilation","perfusion","renalFunction","digestiveFunction").contains(last)?d:Double.NaN;
   rows.add(new Row(bounded(key.isEmpty()?"value":key),v,f));return;
  }
  if(value instanceof Enum<?> e)v=e.name();else if(value instanceof Boolean||value instanceof String)v=value.toString();else return;
  rows.add(new Row(bounded(key.isEmpty()?"value":key),bounded(v),Double.NaN));
 }
}
