package dev.minemods.worldlogic.api;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

/** RC53 data-only physical contract. SI units; nominal game estimates, never measured values.
 * No tick handlers, attributes, inventory constraints, entity movement, or world mutation. */
public final class PhysicalMaterials {
 private PhysicalMaterials() {}
 public static final int REVISION=1;
 public enum Phase { SOLID, GRANULAR, LIQUID, GAS, UNKNOWN }
 public enum Evidence { EXPLICIT, BUILTIN, TAG, ESTIMATED, UNKNOWN }
 public enum Domain { BLOCK, ITEM, FLUID }
 public record MaterialProfile(ResourceLocation id, Phase phase, double dryDensityKgM3,
   double mohs, double compressiveMPa, double tensileMPa, double modulusGPa,
   double porosity, double referenceMoistureKgKg, double conductivityWmK,
   double heatCapacityJkgK, double meltingC, double ignitionC,
   double flammability, double friction, double permeabilityM2, double viscosityPaS) {
  public MaterialProfile {
   Objects.requireNonNull(id); Objects.requireNonNull(phase);
   nonnegative(dryDensityKgM3); nonnegative(mohs); nonnegative(compressiveMPa);
   nonnegative(tensileMPa); nonnegative(modulusGPa); fractionOrUnknown(porosity);
   nonnegative(referenceMoistureKgKg); nonnegative(conductivityWmK);
   nonnegative(heatCapacityJkgK); temperature(meltingC); temperature(ignitionC);
   fractionOrUnknown(flammability); nonnegative(friction);
   nonnegative(permeabilityM2); nonnegative(viscosityPaS);
   if(!Double.isNaN(mohs)&&mohs>10)throw new IllegalArgumentException("Mohs > 10");
  }
 }
 /** Each component is an actual occupied material volume, not a mass fraction.
  * For granular material density is dry bulk density including intrinsic pores. */
 public record Component(ResourceLocation materialId, double volumeM3) {
  public Component { Objects.requireNonNull(materialId); if(!Double.isFinite(volumeM3)||volumeM3<=0)throw new IllegalArgumentException("volume"); }
 }
 public record ObjectProfile(List<Component> components, Evidence evidence) {
  public ObjectProfile { components=List.copyOf(components); Objects.requireNonNull(evidence); if(components.isEmpty()&&evidence!=Evidence.UNKNOWN&&evidence!=Evidence.BUILTIN)throw new IllegalArgumentException("empty composition"); }
  public double volumeM3(){return evidence==Evidence.UNKNOWN?Double.NaN:components.stream().mapToDouble(Component::volumeM3).sum();}
 }
 /** Moisture override is kg added water per kg dry material, NaN uses each material's reference.
  * Temperature is supplied by caller, not silently inferred as core body or object temperature. */
 public record State(double temperatureC,double moistureKgKg) {
  public State {temperature(temperatureC);nonnegative(moistureKgKg);if(!Double.isNaN(moistureKgKg)&&moistureKgKg>5)throw new IllegalArgumentException("moisture > 5");}
  public static State reference(){return new State(20,Double.NaN);}
 }
 public record Result(ObjectProfile object,State state,double volumeM3,double massKg,
   double bulkDensityKgM3,double heatCapacityJK,double specificHeatJkgK) {}
 private record Key(Domain domain,ResourceLocation id){}
 private static final Map<ResourceLocation,MaterialProfile> MATERIALS=new ConcurrentHashMap<>();
 private static final Map<Key,ObjectProfile> EXACT=new ConcurrentHashMap<>();
 private static final Map<Key,ObjectProfile> TAGS=new ConcurrentHashMap<>();
 private static final double N=Double.NaN;
 private static final ObjectProfile UNKNOWN=new ObjectProfile(List.of(),Evidence.UNKNOWN);
 private static final ObjectProfile EMPTY=new ObjectProfile(List.of(),Evidence.BUILTIN);
 private static void nonnegative(double x){if(!Double.isNaN(x)&&(!Double.isFinite(x)||x<0))throw new IllegalArgumentException("nonnegative value");}
 private static void fractionOrUnknown(double x){nonnegative(x);if(x>1)throw new IllegalArgumentException("fraction");}
 private static void temperature(double x){if(!Double.isNaN(x)&&(!Double.isFinite(x)||x< -273.15))throw new IllegalArgumentException("temperature");}
 private static ResourceLocation id(String s){return ResourceLocation.fromNamespaceAndPath("worldlogic",s);}
 /** Addons register their own IDs during common setup; duplicate IDs are rejected, not silently replaced. */
 public static void registerMaterial(MaterialProfile material){Objects.requireNonNull(material);if(MATERIALS.putIfAbsent(material.id(),material)!=null)throw new IllegalArgumentException("duplicate material "+material.id());}
 public static Optional<MaterialProfile> material(ResourceLocation materialId){return Optional.ofNullable(MATERIALS.get(materialId));}
 public static Map<ResourceLocation,MaterialProfile> materials(){return Map.copyOf(MATERIALS);}
 public static void registerObject(Domain domain,ResourceLocation objectId,List<Component> parts){register(EXACT,domain,objectId,parts);}
 public static void registerTag(Domain domain,ResourceLocation tagId,List<Component> parts){register(TAGS,domain,tagId,parts);}
 private static void register(Map<Key,ObjectProfile> map,Domain domain,ResourceLocation key,List<Component> parts){
  Objects.requireNonNull(domain);Objects.requireNonNull(key);
  ObjectProfile p=new ObjectProfile(parts,Evidence.EXPLICIT);
  for(var c:p.components())if(!MATERIALS.containsKey(c.materialId()))throw new IllegalArgumentException("unknown material "+c.materialId());
  if(map.putIfAbsent(new Key(domain,key),p)!=null)throw new IllegalArgumentException("duplicate mapping "+key);
 }
 private static void m(String name,Phase phase,double d,double h,double c,double t,double e,double por,double wet,double k,double cp,double melt,double ignite,double burn,double friction,double perm,double visc){
  registerMaterial(new MaterialProfile(id(name),phase,d,h,c,t,e,por,wet,k,cp,melt,ignite,burn,friction,perm,visc));
 }
 private static void rock(String n,double d,double h,double c,double t,double e,double p,double k){m(n,Phase.SOLID,d,h,c,t,e,p,0,k,850,N,N,0,.65,N,N);}
 private static void wood(String n,double d,double c,double t,double e,double wet){m(n,Phase.SOLID,d,N,c,t,e,N,wet,.13,1700,N,300,.8,.5,N,N);}
 private static void soil(String n,double d,double p,double wet,double k,double perm){m(n,Phase.GRANULAR,d,N,N,N,N,p,wet,k,850,N,N,0,.6,perm,N);}
 private static void metal(String n,double d,double h,double t,double e,double k,double cp,double melt){m(n,Phase.SOLID,d,h,N,t,e,0,0,k,cp,melt,N,0,.4,0,N);}
 private static void liquid(String n,double d,double k,double cp,double visc){m(n,Phase.LIQUID,d,N,N,N,N,N,0,k,cp,N,N,0,N,N,visc);}
 static {
  m("air",Phase.GAS,1.204,N,N,N,N,N,0,.026,1005,N,N,0,N,N,1.81e-5);
  rock("stone",2650,6,120,8,45,.03,2.5);
  rock("granite",2700,6.5,160,10,55,.015,3);
  rock("diorite",2800,6,170,11,60,.02,2.7);
  rock("andesite",2650,6,130,9,45,.05,2);
  rock("deepslate",2800,5.5,170,10,60,.02,2.5);
  rock("limestone",2550,3.5,65,5,30,.1,1.8);
  rock("shale",2400,3,35,3,18,.12,1.5);
  rock("slate",2750,4,125,8,50,.025,2.2);
  rock("gneiss",2800,6.5,180,11,60,.02,2.8);
  rock("volcanic_tuff",1650,3,25,2,9,.35,.8);
  rock("basalt",2950,6,200,12,70,.04,2);
  rock("calcite",2710,3,55,4,28,.03,3.2);
  rock("sandstone",2200,5,65,4,25,.18,2);
  rock("obsidian",2400,5.5,110,7,50,.01,1.5);
  rock("netherrack",1900,3,25,2,10,.2,1.1);
  rock("end_stone",2400,5,90,6,35,.09,1.6);
  rock("quartz",2650,7,120,8,70,.01,6);
  rock("brick",1850,5,35,3,15,.2,.7);
  rock("concrete",2350,5,30,3,25,.12,1.4);
  rock("coal",1350,2.5,20,2,5,.1,.3);
  rock("diamond",3510,10,N,N,1050,0,1500);
  rock("emerald",2760,7.5,N,N,110,0,3);
  rock("lapis",2700,5.5,N,N,40,.02,2);
  rock("redstone",3100,N,N,N,N,.1,1);
  soil("loam",1350,.49,.18,.7,1e-12);
  soil("humus",650,.65,.30,.3,2e-12);
  soil("peat",300,.78,.70,.2,5e-13);
  soil("silt",1450,.46,.20,.8,1e-13);
  soil("sand",1550,.40,.03,.35,1e-10);
  soil("white_sand",1550,.42,.03,.4,1e-10);
  soil("volcanic_sand",1650,.42,.04,.5,8e-11);
  soil("volcanic_ash",800,.60,.12,.25,1e-12);
  soil("gravel",1700,.36,.02,.7,1e-8);
  soil("clay",1500,.45,.25,.9,1e-16);
  soil("chernozem",1250,.52,.21,.6,1e-12);
  soil("sandy_loam",1450,.43,.12,.6,1e-11);
  soil("laterite",1750,.35,.12,1,1e-13);
  wood("oak",670,48,90,11,.12);
  wood("dark_oak",730,52,100,12,.12);
  wood("spruce",430,36,65,10,.12);
  wood("birch",620,45,95,13,.12);
  wood("jungle",590,42,75,10,.16);
  wood("acacia",760,55,105,14,.12);
  wood("mangrove",820,55,90,13,.25);
  wood("cherry",560,43,82,10,.12);
  wood("bamboo",650,50,140,15,.12);
  m("crimson",Phase.SOLID,650,N,45,80,10,N,.15,.13,1700,N,N,0,.5,N,N);
  m("warped",Phase.SOLID,600,N,40,75,9,N,.15,.13,1700,N,N,0,.5,N,N);
  wood("generic_wood",550,40,70,10,.12);
  metal("iron",7870,4,250,200,80,450,1538);
  metal("gold",19320,2.5,120,79,315,129,1064);
  metal("copper",8960,3,210,117,390,385,1085);
  metal("netherite",9500,N,500,200,45,450,N);
  m("glass",Phase.SOLID,2500,5.5,600,35,70,0,0,1,840,1400,N,0,.35,0,N);
  m("ice",Phase.SOLID,917,1.5,5,1,9,0,0,2.2,2100,0,N,0,.05,0,N);
  m("snow",Phase.GRANULAR,150,N,N,N,N,.8,0,.1,2100,0,N,0,.3,1e-10,N);
  m("wool",Phase.SOLID,35,N,N,N,N,.95,.1,.04,1300,N,570,.6,.6,N,N);
  m("leather",Phase.SOLID,850,N,N,15,.1,.2,.1,.15,1500,N,300,.7,.5,N,N);
  m("plant",Phase.SOLID,400,N,N,N,N,.5,.5,.12,1800,N,250,.8,.5,N,N);
  m("paper",Phase.SOLID,800,N,N,20,2,.3,.06,.08,1400,N,233,.9,.5,N,N);
  m("bone",Phase.SOLID,1800,5,130,80,15,.12,.05,.4,1300,N,N,.1,.5,N,N);
  liquid("fresh_water",998,.60,4182,.001002);
  liquid("sea_water",1025,.60,3990,.00108);
  liquid("brackish_water",1010,.60,4050,.00104);
  liquid("lava",2700,1.5,1200,100);
  liquid("milk",1030,.53,3900,.002);
  liquid("honey",1420,.50,2300,8);
  m("rubber_like",Phase.SOLID,1100,N,N,10,.01,.04,.01,.15,1700,N,320,.8,.9,N,N);
  m("wax",Phase.SOLID,960,N,N,2,.1,.02,0,.25,2100,63,200,.9,.3,N,N);
  m("food",Phase.SOLID,700,N,N,N,N,.30,.40,.45,2100,N,N,.2,.4,N,N);
  m("ceramic",Phase.SOLID,2200,6,100,10,35,.10,0,1.5,850,N,N,0,.5,N,N);
  m("charcoal",Phase.SOLID,300,1.5,N,N,N,.70,.05,.1,1000,N,350,.95,.6,N,N);
 }
 private static ObjectProfile single(String m,double v,Evidence e){return new ObjectProfile(List.of(new Component(id(m),v)),e);}
 private static ObjectProfile resize(ObjectProfile p,double f){
  if(p.evidence()==Evidence.UNKNOWN)return p;
  if(f<=0)return EMPTY;
  if(f==1)return p;
  return new ObjectProfile(p.components().stream().map(c->new Component(c.materialId(),c.volumeM3()*f)).toList(),p.evidence());
 }
 private static ObjectProfile tagged(BlockState s){
  return TAGS.entrySet().stream().filter(e->e.getKey().domain()==Domain.BLOCK&&s.is(TagKey.create(Registries.BLOCK,e.getKey().id())))
   .sorted(Comparator.comparing(e->e.getKey().id().toString())).map(Map.Entry::getValue).findFirst().orElse(null);
 }
 private static ObjectProfile tagged(ItemStack s){
  return TAGS.entrySet().stream().filter(e->e.getKey().domain()==Domain.ITEM&&s.is(TagKey.create(Registries.ITEM,e.getKey().id())))
   .sorted(Comparator.comparing(e->e.getKey().id().toString())).map(Map.Entry::getValue).findFirst().orElse(null);
 }
 private static ObjectProfile tagged(FluidState s){
  return TAGS.entrySet().stream().filter(e->e.getKey().domain()==Domain.FLUID&&s.is(TagKey.create(Registries.FLUID,e.getKey().id())))
   .sorted(Comparator.comparing(e->e.getKey().id().toString())).map(Map.Entry::getValue).findFirst().orElse(null);
 }
 private static boolean named(String p,String base){return p.equals(base)||p.startsWith(base+"_")||p.endsWith("_"+base)||p.contains("_"+base+"_");}
 private static final String[] WOODS={"dark_oak","oak","spruce","birch","jungle","acacia","mangrove","cherry","bamboo","crimson","warped"};
 private static String woodName(String p){for(String w:WOODS)if(named(p,w))return w;return null;}
 private static String solidName(String p){
  for(String m:new String[]{"volcanic_tuff","limestone","shale","slate","gneiss","deepslate","granite","diorite","andesite","sandstone","basalt","calcite","obsidian","netherrack","end_stone","quartz","concrete","lapis","redstone","diamond","emerald","netherite","copper","iron","gold","coal","glass","leather","wool","bone","paper","brick","stone"})if(named(p,m)||p.equals(m+"s"))return m;
  if(named(p,"tuff"))return "volcanic_tuff";
  if(named(p,"bricks")||named(p,"terracotta")||p.equals("flower_pot"))return "brick";
  return null;
 }
 private static String ground(String p){
  for(String n:new String[]{"volcanic_ash","volcanic_sand","white_sand","sandy_loam","chernozem","laterite","humus","peat","silt","loam","gravel","clay","sand"})if(named(p,n))return n;
  if(named(p,"dirt")||p.equals("grass_block")||p.equals("farmland")||p.equals("podzol")||p.equals("mycelium")||p.equals("mud")||p.equals("muddy_mangrove_roots"))return "loam";
  if(p.equals("frozen_soil")||p.equals("dirt_path"))return "loam";
  return null;
 }
 private static double blockVolume(String p){
  if(p.endsWith("_slab"))return .5;
  if(p.endsWith("_stairs"))return .75;
  if(p.endsWith("_wall"))return .35;
  if(p.endsWith("_fence")||p.endsWith("_fence_gate"))return .20;
  if(p.endsWith("_door"))return .1875;
  if(p.endsWith("_trapdoor"))return .1875;
  if(p.endsWith("_pane")||p.endsWith("_bars"))return .12;
  if(p.endsWith("_button"))return .005;
  if(p.endsWith("_pressure_plate")||p.endsWith("_carpet"))return .0625;
  if(p.endsWith("_sign")||p.endsWith("_banner"))return .04;
  if(p.endsWith("_leaves"))return .10;
  if(p.endsWith("_sapling")||p.endsWith("_flower")||p.equals("grass")||p.equals("short_grass")||p.equals("tall_grass"))return .01;
  if(p.endsWith("_farmland")||p.equals("farmland")||p.equals("dirt_path"))return .9375;
  if(p.equals("torch")||p.endsWith("_torch"))return .005;
  if(p.equals("chain"))return .025;
  return 1;
 }
 private static ObjectProfile builtinBlock(ResourceLocation key){
  String p=key.getPath(); boolean known=key.getNamespace().equals("minecraft")||key.getNamespace().equals("worldlogic");
  Evidence e=known?Evidence.BUILTIN:Evidence.ESTIMATED;
  if(p.equals("air")||p.equals("cave_air")||p.equals("void_air"))return single("air",1,Evidence.BUILTIN);
  if(p.endsWith("_ore")||p.equals("ancient_debris")){
   String host=p.startsWith("deepslate_")?"deepslate":p.startsWith("nether_")||p.equals("ancient_debris")?"netherrack":"stone";
   String mineral=solidName(p.replace("deepslate_","").replace("nether_","").replace("_ore",""));
   if(p.equals("ancient_debris"))mineral="netherite";
   if(mineral!=null)return new ObjectProfile(List.of(new Component(id(host),.88),new Component(id(mineral),.12)),Evidence.ESTIMATED);
  }
  if(p.equals("raw_iron_block")||p.equals("raw_gold_block")||p.equals("raw_copper_block"))return single(solidName(p),.65,Evidence.ESTIMATED);
  if(p.equals("ice")||p.equals("packed_ice")||p.equals("blue_ice")||p.equals("frosted_ice"))return single("ice",1,e);
  if(p.equals("snow")||p.equals("snow_block")||p.equals("powder_snow"))return single("snow",p.equals("snow")?.125:1,e);
  String w=woodName(p);
  if(p.endsWith("_leaves")||p.endsWith("_sapling")||p.endsWith("_flower")||p.endsWith("_roots")&&!p.equals("muddy_mangrove_roots"))return single("plant",blockVolume(p),e);
  if(w!=null)return single(w,blockVolume(p),e);
  if(p.equals("furnace")||p.equals("smoker")||p.equals("blast_furnace"))return part("stone",.52,"iron",.015);
  if(p.equals("anvil")||p.endsWith("_anvil"))return single("iron",.28,Evidence.ESTIMATED);
  if(p.equals("chain"))return single("iron",.025,Evidence.ESTIMATED);
  if(p.equals("rail")||p.endsWith("_rail"))return part("iron",.014,"generic_wood",.04);
  if(p.equals("torch")||p.endsWith("_torch"))return part("generic_wood",.004,"coal",.001);
  if(p.equals("lantern")||p.equals("soul_lantern"))return part("iron",.003,"glass",.002);
  if(p.endsWith("_bed"))return part("generic_wood",.16,"wool",.20);
  if(p.endsWith("_candle")||p.equals("candle"))return single("wax",.001,Evidence.ESTIMATED);
  if(p.equals("honey_block"))return single("honey",1,Evidence.ESTIMATED);
  if(p.equals("slime_block"))return single("rubber_like",1,Evidence.ESTIMATED);
  if(p.equals("cauldron")||p.endsWith("_cauldron"))return single("iron",.13,Evidence.ESTIMATED);
  if(p.equals("decorated_pot"))return single("ceramic",.045,Evidence.ESTIMATED);
  if(p.equals("sponge")||p.equals("wet_sponge"))return single("plant",1,Evidence.ESTIMATED);
  if(p.equals("crafting_table")||p.equals("chest")||p.equals("trapped_chest")||p.equals("barrel")||p.equals("bookshelf")||p.equals("ladder"))return single("generic_wood",p.equals("ladder")?.08:.35,Evidence.ESTIMATED);
  String g=ground(p);if(g!=null)return single(g,blockVolume(p),e);
  String s=solidName(p);if(s!=null)return single(s,blockVolume(p),e);
  if(p.equals("grass")||p.equals("short_grass")||p.equals("tall_grass")||p.equals("vine")||p.equals("kelp")||p.equals("seagrass")||p.equals("lily_pad"))return single("plant",.01,e);
  if(p.equals("water")||p.endsWith("_water")||p.equals("groundwater")||p.equals("lava"))return builtinFluid(key);
  return UNKNOWN;
 }
 public static ObjectProfile block(BlockState state){
  ResourceLocation key=BuiltInRegistries.BLOCK.getKey(state.getBlock());
  var p=EXACT.get(new Key(Domain.BLOCK,key));
  if(p==null)p=tagged(state);
  if(p==null)p=builtinBlock(key);
  if(p.evidence()==Evidence.UNKNOWN){
   for(String tag:new String[]{"logs","planks","leaves","sand","dirt","base_stone_overworld"}){
    if(state.is(TagKey.create(Registries.BLOCK,ResourceLocation.fromNamespaceAndPath("minecraft",tag)))) {
     p=single(switch(tag){case "logs","planks"->"generic_wood";case "leaves"->"plant";case "sand"->"sand";case "dirt"->"loam";default->"stone";},blockVolume(key.getPath()),Evidence.TAG);break;
    }
   }
  }
  if(state.getBlock() instanceof SlabBlock&&state.getValue(SlabBlock.TYPE)==SlabType.DOUBLE)return resize(p,2);
  if(state.hasProperty(net.minecraft.world.level.block.SnowLayerBlock.LAYERS)&&key.getPath().equals("snow"))return resize(p,state.getValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS));
  return p;
 }
 private static ObjectProfile builtinFluid(ResourceLocation key){
  String p=key.getPath();if(p.startsWith("flowing_"))p=p.substring(8);
  String m=switch(p){case "water","river_water","lake_water","spring_water","groundwater","wetland_water","pond_water"->"fresh_water";case "sea_water"->"sea_water";case "brackish_water"->"brackish_water";case "lava"->"lava";case "milk"->"milk";case "honey"->"honey";default->null;};
  return m==null?UNKNOWN:single(m,1,key.getNamespace().equals("minecraft")||key.getNamespace().equals("worldlogic")?Evidence.BUILTIN:Evidence.ESTIMATED);
 }
 /** Profile of a cubic metre of fluid; use fluidVolume for actual container volume. */
 public static ObjectProfile fluid(Fluid fluid){ResourceLocation k=BuiltInRegistries.FLUID.getKey(fluid);var p=EXACT.get(new Key(Domain.FLUID,k));if(p==null)p=tagged(fluid.defaultFluidState());return p==null?builtinFluid(k):p;}
 /** Millibuckets, 1000 mB = 1 L = 0.001 m3; not the visual size of a source block. */
 public static ObjectProfile fluidVolume(Fluid fluid,double milliBuckets){
  if(!Double.isFinite(milliBuckets)||milliBuckets<=0)throw new IllegalArgumentException("fluid volume");
  return resize(fluid(fluid),milliBuckets/1_000_000);
 }
 /** Terrain fluid fraction, source = 1 m3, flowing = nominal level/8. */
 public static ObjectProfile fluidBlock(FluidState fluid){
  if(fluid.isEmpty())return UNKNOWN;
  return resize(fluid(fluid.getType()),fluid.isSource()?1:Math.max(1,fluid.getAmount())/8.0);
 }
 private static ObjectProfile part(String a,double va,String b,double vb){
  return new ObjectProfile(List.of(new Component(id(a),va),new Component(id(b),vb)),Evidence.ESTIMATED);
 }
 private static ObjectProfile builtinItem(ResourceLocation key){
  String p=key.getPath(),s=solidName(p);
  if(p.equals("stick"))return single("generic_wood",.00015,Evidence.ESTIMATED);
  if(p.equals("feather"))return single("plant",.000005,Evidence.ESTIMATED);
  if(p.equals("string"))return single("plant",.00001,Evidence.ESTIMATED);
  if(p.equals("paper"))return single("paper",.00002,Evidence.ESTIMATED);
  if(p.equals("book"))return part("paper",.00035,"leather",.00008);
  if(p.equals("shield"))return part("generic_wood",.006,"iron",.00008);
  if(p.equals("bow")||p.equals("crossbow"))return part("generic_wood",.001,"plant",.00002);
  if(p.equals("fishing_rod"))return part("generic_wood",.0005,"plant",.00002);
  if(p.equals("item_frame")||p.equals("glow_item_frame"))return part("generic_wood",.001,"leather",.0003);
  if(p.equals("armor_stand"))return part("generic_wood",.015,"stone",.006);
  if(p.equals("painting"))return part("generic_wood",.0004,"wool",.0002);
  if(p.equals("charcoal"))return single("charcoal",.0004,Evidence.ESTIMATED);
  if(p.equals("honey_bottle"))return part("glass",.00007,"honey",.00025);
  if(p.equals("glass_bottle"))return single("glass",.00007,Evidence.ESTIMATED);
  if(p.equals("bucket"))return single("iron",.00012,Evidence.ESTIMATED);
  if(p.endsWith("_bucket")){
   String f=p.substring(0,p.length()-7);
   ObjectProfile fp=builtinFluid(ResourceLocation.fromNamespaceAndPath(key.getNamespace(),f));
   if(fp.evidence()!=Evidence.UNKNOWN)return part("iron",.00012,fp.components().get(0).materialId().getPath(),.001);
  }
  if(p.equals("wooden_sword")||p.startsWith("wooden_"))s="generic_wood";
  if(p.startsWith("golden_"))s="gold";
  if(p.startsWith("chainmail_"))s="iron";
  if(s!=null){
   double tool=p.endsWith("_sword")?.00013:p.endsWith("_axe")?.00016:p.endsWith("_pickaxe")?.00018:p.endsWith("_shovel")?.00011:p.endsWith("_hoe")?.00009:0;
   if(tool>0)return part(s,tool,"generic_wood",.00055);
   if(p.endsWith("_helmet")||p.endsWith("_chestplate")||p.endsWith("_leggings")||p.endsWith("_boots"))return single(s,p.endsWith("_chestplate")?.0018:p.endsWith("_leggings")?.0013:.00065,Evidence.ESTIMATED);
   if(p.endsWith("_ingot"))return single(s,.00011,Evidence.ESTIMATED);
   if(p.endsWith("_nugget"))return single(s,.00011/9,Evidence.ESTIMATED);
   if(p.startsWith("raw_")||p.equals("diamond")||p.equals("emerald")||p.equals("coal")||p.equals("lapis_lazuli")||p.equals("redstone")||p.equals("quartz")||p.equals("bone"))return single(s,.0001,Evidence.ESTIMATED);
  }
  return UNKNOWN;
 }
 /** One item, regardless of count. BlockItems retain their block material composition. */
 public static ObjectProfile item(ItemStack stack){
  if(stack.isEmpty())return EMPTY;
  var key=BuiltInRegistries.ITEM.getKey(stack.getItem());
  var p=EXACT.get(new Key(Domain.ITEM,key));if(p==null)p=tagged(stack);
  if(p!=null)return p;
  if(stack.getItem() instanceof BlockItem b)return block(b.getBlock().defaultBlockState());
  p=builtinItem(key);
  if(p.evidence()==Evidence.UNKNOWN&&stack.get(net.minecraft.core.component.DataComponents.FOOD)!=null)return single("food",.00025,Evidence.ESTIMATED);
  return p;
 }
 public static ObjectProfile stack(ItemStack stack){return stack.isEmpty()?EMPTY:resize(item(stack),stack.getCount());}
 /** Item payload only, not the frame or stand that supports it. */
 public static ObjectProfile displayedItem(net.minecraft.world.entity.decoration.ItemFrame frame){return stack(frame.getItem());}
 public static ObjectProfile droppedItem(net.minecraft.world.entity.item.ItemEntity entity){return stack(entity.getItem());}
 /** Pure aggregate. Heat capacity uses dry component mass plus absorbed water.
  * No averaging of strength, hardness or fracture limits across unrelated parts. */
 public static Result evaluate(ObjectProfile p,State state){
  Objects.requireNonNull(p);Objects.requireNonNull(state);
  if(p.evidence()==Evidence.UNKNOWN)return new Result(p,state,N,N,N,N,N);
  double v=p.volumeM3(),mass=0,heat=0;
  for(var c:p.components()){
   var m=MATERIALS.get(c.materialId());if(m==null)return new Result(p,state,v,N,N,N,N);
   double density=m.dryDensityKgM3();
   if(m.id().equals(id("fresh_water"))&&Double.isFinite(state.temperatureC())){
    double t=Math.max(0,Math.min(100,state.temperatureC()));
    density=1000*(1-((t+288.9414)/(508929.2*(t+68.12963)))*Math.pow(t-3.9863,2));
   }
   double wet=m.phase()==Phase.SOLID||m.phase()==Phase.GRANULAR?
      (Double.isNaN(state.moistureKgKg())?m.referenceMoistureKgKg():state.moistureKgKg()):0;
   if(Double.isFinite(wet)&&Double.isFinite(m.porosity())&&density>0)wet=Math.min(wet,m.porosity()*1000/density);
   double dry=density*c.volumeM3();
   double water=dry*wet;
   mass+=dry+water;heat+=dry*m.heatCapacityJkgK()+water*4182;
  }
  return new Result(p,state,v,mass,v>0?mass/v:N,heat,mass>0?heat/mass:N);
 }
 /** Loaded positions only. Waterlogged blocks do not silently include fluid; query fluidBlock separately.
  * Soil moisture is the existing Core cell estimate, not a new block-level storage or water budget. */
 public static Optional<Result> placed(ServerLevel level,BlockPos pos){
  if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  BlockState b=level.getBlockState(pos);
  if(b.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)return Optional.of(evaluate(fluidBlock(b.getFluidState()),State.reference()));
  ObjectProfile p=block(b);double wet=N;
  if(p.components().size()==1){
   var m=MATERIALS.get(p.components().get(0).materialId());
   if(m!=null&&m.phase()==Phase.GRANULAR&&m.dryDensityKgM3()>0&&Double.isFinite(m.porosity())&&!m.id().equals(id("snow"))){
    var h=WorldLogicApi.hydrology(level,pos);
    if(h.isPresent()&&h.get().simulated())wet=Math.max(0,Math.min(1,h.get().saturation()))*m.porosity()*1000/m.dryDensityKgM3();
   }
  }
  return Optional.of(evaluate(p,new State(20,wet)));
 }
}
