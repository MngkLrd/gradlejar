package dev.minemods.worldlogic;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
/** Data-only guide contract. Shared with server QA; no client or addon class references. */
public final class GuideBook{
 private GuideBook(){}
 public static final List<String> BUILTIN=List.of("core","hydration","thermal","trauma","terrarian","treechopper");
 public record Local(String ru,String en){public String get(boolean russian){return russian?ru:en;}}
 public record Text(List<String> ru,List<String> en){public List<String> get(boolean russian){return russian?ru:en;}}
 public record Page(String id,Local title,List<ResourceLocation> models,Text body){}
 public record Chapter(String id,int order,String requires,Local title,String source,List<Page> pages){}
 private static String string(JsonObject o,String key,int max){
  var e=o.get(key);if(e==null||!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException(key);
  String s=e.getAsString();if(s.isBlank()||s.length()>max)throw new IllegalArgumentException(key);return s;
 }
 private static String id(JsonObject o,String key){String s=string(o,key,80);if(!s.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException(key);return s;}
 private static Local local(JsonObject o){return new Local(string(o,"ru",90),string(o,"en",90));}
 private static List<String> paragraphs(JsonArray a){if(a==null||a.isEmpty()||a.size()>12)throw new IllegalArgumentException("paragraphs");var result=new ArrayList<String>();for(var e:a){if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isString())throw new IllegalArgumentException("paragraph");String s=e.getAsString();if(s.isBlank()||s.length()>1800)throw new IllegalArgumentException("paragraph length");result.add(s);}return List.copyOf(result);}
 public static Chapter read(Reader reader){
  var o=JsonParser.parseReader(reader).getAsJsonObject();if(o.get("schema").getAsInt()!=1)throw new IllegalArgumentException("schema");
  var pages=o.getAsJsonArray("pages");if(pages==null||pages.isEmpty()||pages.size()>100)throw new IllegalArgumentException("pages");
  var result=new ArrayList<Page>();var ids=new HashSet<String>();
  for(var v:pages){var b=v.getAsJsonObject();String key=id(b,"id");if(!ids.add(key))throw new IllegalArgumentException("duplicate page "+key);
   var m=b.getAsJsonArray("models");if(m==null||m.size()>8)throw new IllegalArgumentException("models");var models=new ArrayList<ResourceLocation>();for(var x:m){var loc=ResourceLocation.tryParse(x.getAsString());if(loc==null)throw new IllegalArgumentException("model ID");models.add(loc);}
   var text=b.getAsJsonObject("body");result.add(new Page(key,local(b.getAsJsonObject("title")),List.copyOf(models),new Text(paragraphs(text.getAsJsonArray("ru")),paragraphs(text.getAsJsonArray("en")))));
  }
  return new Chapter(id(o,"id"),o.get("order").getAsInt(),id(o,"requires"),local(o.getAsJsonObject("title")),string(o,"source",120),List.copyOf(result));
 }
 public static List<Chapter> visible(Collection<Chapter> chapters,Predicate<String> installed){
  return chapters.stream().filter(c->installed.test(c.requires())).sorted(Comparator.comparingInt(Chapter::order).thenComparing(Chapter::id)).toList();
 }
 static void verify()throws IOException{
  TabLayout.verify();
  var all=new ArrayList<Chapter>();var ids=new HashSet<String>();
  for(String name:BUILTIN){try(var in=GuideBook.class.getResourceAsStream("/assets/worldlogic/worldlogic_guide/"+name+".json")){
   if(in==null)throw new IllegalStateException("missing guide chapter "+name);
   var c=read(new InputStreamReader(in,StandardCharsets.UTF_8));if(!c.id().equals(name)||!ids.add(c.id()))throw new IllegalStateException("chapter ID "+name);
   if(c.pages().stream().anyMatch(page->page.models().isEmpty()))throw new IllegalStateException("missing illustrations "+name);
   all.add(c);
  }}
  if(visible(all,"worldlogic"::equals).size()!=1||visible(all,v->true).size()!=6||visible(all,v->false).size()!=0)throw new IllegalStateException("addon visibility");
  for(var c:all)for(var page:c.pages())for(var model:page.models())if(model.getNamespace().equals("minecraft")||model.getNamespace().equals("worldlogic"))if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(model))throw new IllegalStateException("missing core guide item "+model);
  var external=read(new StringReader("{\"schema\":1,\"id\":\"future\",\"requires\":\"futuremod\",\"order\":90,\"source\":\"fixture\",\"title\":{\"ru\":\"Дополнение\",\"en\":\"Addon\"},\"pages\":[{\"id\":\"intro\",\"title\":{\"ru\":\"Начало\",\"en\":\"Intro\"},\"models\":[\"minecraft:book\"],\"body\":{\"ru\":[\"Тест\"],\"en\":[\"Test\"]}}]}"));
  if(visible(List.of(external),"futuremod"::equals).size()!=1||!visible(List.of(external),"worldlogic"::equals).isEmpty())throw new IllegalStateException("external chapter filtering");
  boolean rejected=false;try{read(new StringReader("{\"schema\":0}"));}catch(RuntimeException e){rejected=true;}if(!rejected)throw new IllegalStateException("bad schema accepted");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GUIDE chapters={} pages={} models={}",all.size(),all.stream().mapToInt(c->c.pages().size()).sum(),all.stream().flatMap(c->c.pages().stream()).mapToInt(v->v.models().size()).sum());
 }
}
