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
 public static final List<String> BUILTIN=List.of("core","water","hydration","weather","thermal","trauma","treechopper","terrarian","geology","biomes");
 public record Local(String ru,String en){public String get(boolean russian){return russian?ru:en;}}
 public record Text(List<String> ru,List<String> en){public List<String> get(boolean russian){return russian?ru:en;}}
 public record Page(String id,Local title,List<ResourceLocation> models,Text body,List<ResourceLocation> icons){}
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
   var icons=new ArrayList<ResourceLocation>();if(b.has("icons")){var a=b.getAsJsonArray("icons");if(a.size()>8)throw new IllegalArgumentException("icons");for(var e:a){var loc=ResourceLocation.tryParse(e.getAsString());if(loc==null||!loc.getPath().startsWith("textures/")||!loc.getPath().endsWith(".png"))throw new IllegalArgumentException("icon path");icons.add(loc);}}
   var text=b.getAsJsonObject("body");result.add(new Page(key,local(b.getAsJsonObject("title")),List.copyOf(models),new Text(paragraphs(text.getAsJsonArray("ru")),paragraphs(text.getAsJsonArray("en"))),List.copyOf(icons)));
  }
  return new Chapter(id(o,"id"),o.get("order").getAsInt(),id(o,"requires"),local(o.getAsJsonObject("title")),string(o,"source",120),List.copyOf(result));
 }
 public static List<Chapter> visible(Collection<Chapter> chapters,Predicate<String> installed){
  return chapters.stream().filter(c->installed.test(c.requires())).sorted(Comparator.comparingInt(Chapter::order).thenComparing(Chapter::id)).toList();
 }
 /** Built-in reference articles remain readable even without their optional addon. */
 public static List<Chapter> catalog(Collection<Chapter> chapters,Predicate<String> installed){
  return chapters.stream().filter(c->BUILTIN.contains(c.id())||installed.test(c.requires())).sorted(Comparator.comparingInt(Chapter::order).thenComparing(Chapter::id)).toList();
 }
 public record Entry(int chapter,int page){}
 public static List<Entry> search(List<Chapter> chapters,String query,boolean ru){
  var words=Arrays.stream(query.toLowerCase(Locale.ROOT).strip().split("\\s+")).filter(w->!w.isEmpty()).toList();var out=new ArrayList<Entry>();
  for(int i=0;i<chapters.size();i++){var c=chapters.get(i);for(int k=0;k<c.pages().size();k++){var p=c.pages().get(k);String text=(c.title().get(ru)+" "+p.title().get(ru)+" "+String.join(" ",p.body().get(ru))+" "+p.models()).toLowerCase(Locale.ROOT);if(words.stream().allMatch(text::contains))out.add(new Entry(i,k));}}
  return List.copyOf(out);
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
  if(visible(all,"worldlogic"::equals).size()!=5||visible(all,v->true).size()!=10||visible(all,v->false).size()!=0||catalog(all,v->false).size()!=10||!catalog(all,v->false).getLast().id().equals("biomes"))throw new IllegalStateException("addon visibility");
  for(var c:all)for(var page:c.pages())for(var model:page.models())if(model.getNamespace().equals("minecraft")||model.getNamespace().equals("worldlogic"))if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(model))throw new IllegalStateException("missing core guide item "+model);
  var external=read(new StringReader("{\"schema\":1,\"id\":\"future\",\"requires\":\"futuremod\",\"order\":90,\"source\":\"fixture\",\"title\":{\"ru\":\"Дополнение\",\"en\":\"Addon\"},\"pages\":[{\"id\":\"intro\",\"title\":{\"ru\":\"Начало\",\"en\":\"Intro\"},\"models\":[\"minecraft:book\"],\"body\":{\"ru\":[\"Тест\"],\"en\":[\"Test\"]}}]}"));
  if(visible(List.of(external),"futuremod"::equals).size()!=1||!visible(List.of(external),"worldlogic"::equals).isEmpty())throw new IllegalStateException("external chapter filtering");
  if(search(all,"",true).size()!=all.stream().mapToInt(c->c.pages().size()).sum()||search(all,"zz_nonexistent_guide",true).size()!=0||search(all,"перелом",true).isEmpty()||search(all,"fracture",false).isEmpty())throw new IllegalStateException("guide search");
  var trauma=all.stream().filter(c->c.id().equals("trauma")).findFirst().orElseThrow();if(trauma.pages().size()<20||trauma.pages().stream().flatMap(p->p.icons().stream()).distinct().count()<6)throw new IllegalStateException("medical article and icon inventory");
  for(String name:List.of("water","hydration","weather","thermal","treechopper","geology","biomes"))if(catalog(all,v->false).stream().noneMatch(c->c.id().equals(name)))throw new IllegalStateException("missing guide category "+name);
  var atlas=all.stream().filter(c->c.id().equals("biomes")).findFirst().orElseThrow();if(atlas.pages().size()!=58)throw new IllegalStateException("active biome inventory");
  boolean rejected=false;try{read(new StringReader("{\"schema\":0}"));}catch(RuntimeException e){rejected=true;}if(!rejected)throw new IllegalStateException("bad schema accepted");
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_GUIDE chapters={} pages={} models={}",all.size(),all.stream().mapToInt(c->c.pages().size()).sum(),all.stream().flatMap(c->c.pages().stream()).mapToInt(v->v.models().size()).sum());
 }
}
