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
 public static final int REVISION=2;
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
 /** Effective values for each constituent, not an invented composite breaking limit. */
 public record ComponentResult(Component component,MaterialProfile material,double moistureKgKg,
   double massKg,double heatCapacityJK,double conductivityWmK,double compressiveMPa,double tensileMPa,double modulusGPa){}
 public static Optional<ComponentResult> componentProperties(Component c,State state){
  var m=MATERIALS.get(c.materialId());if(m==null)return Optional.empty();
  double density=m.dryDensityKgM3(),specificHeat=m.heatCapacityJkgK();
  double salinity=m.id().equals(id("fresh_water"))?0:m.id().equals(id("brackish_water"))?12:m.id().equals(id("sea_water"))?35:N;
  if(Double.isFinite(salinity)&&Double.isFinite(state.temperatureC())){
   var solution=PhysicalWater.solution(salinity,state.temperatureC());
   density=solution.map(PhysicalWater.Solution::densityKgM3).orElse(N);
   specificHeat=solution.map(PhysicalWater.Solution::specificHeatJkgK).orElse(N);
  }
  double wet=m.phase()==Phase.SOLID||m.phase()==Phase.GRANULAR?(Double.isNaN(state.moistureKgKg())?m.referenceMoistureKgKg():state.moistureKgKg()):0;
  if(!WOOD_PROPERTIES.containsKey(m.id())&&Double.isFinite(wet)&&Double.isFinite(m.porosity())&&density>0)wet=Math.min(wet,m.porosity()*1000/density);
  double dry=density*c.volumeM3(),water=dry*wet;
  // Bounded game proxy: added water changes effective thermal transport in porous solids/soil.
  double k=m.conductivityWmK();
  if(Double.isFinite(k)&&Double.isFinite(wet)&&wet>0&&m.phase()!=Phase.LIQUID&&m.phase()!=Phase.GAS)k*=1+Math.min(1,wet)*.65;
  // No unsupported universal strength-vs-temperature curve; keep nominal values.
  return Optional.of(new ComponentResult(c,m,wet,dry+water,dry*specificHeat+water*4182,k,m.compressiveMPa(),m.tensileMPa(),m.modulusGPa()));
 }
 private record Key(Domain domain,ResourceLocation id){}
 private static final Map<ResourceLocation,MaterialProfile> MATERIALS=new ConcurrentHashMap<>();
 private static final Map<Key,ObjectProfile> EXACT=new ConcurrentHashMap<>();
 private static final Map<Key,ObjectProfile> TAGS=new ConcurrentHashMap<>();
 private static final Map<ResourceLocation,WoodProperties> WOOD_PROPERTIES=new ConcurrentHashMap<>();
 private static final Map<ResourceLocation,IndividualItemProvider> INDIVIDUAL_ITEMS=new ConcurrentHashMap<>();
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
 /** Janka force (kN), bending modulus of rupture (MPa), and green moisture (kg/kg dry).
  * Janka and bending at the material's reference moisture, normally 0.12. NaN means unavailable.
  * Fungal and synthetic values are gameplay estimates, not measured properties. */
 public record WoodProperties(double jankaKN,double bendingStrengthMPa,double greenMoistureKgKg){
  public WoodProperties{nonnegative(jankaKN);nonnegative(bendingStrengthMPa);nonnegative(greenMoistureKgKg);if(greenMoistureKgKg>5)throw new IllegalArgumentException("green moisture");}
 }
 public record WoodSample(MaterialProfile material,WoodProperties properties,double moistureKgKg,double densityKgM3,
  double jankaKN,double bendingStrengthMPa,double flammability){}
 public static void registerWoodProperties(ResourceLocation key,WoodProperties values){
  Objects.requireNonNull(values);
  var m=MATERIALS.get(Objects.requireNonNull(key));
  if(m==null||m.phase()!=Phase.SOLID)throw new IllegalArgumentException("register solid material first "+key);
  if(WOOD_PROPERTIES.putIfAbsent(key,values)!=null)throw new IllegalArgumentException("duplicate wood properties "+key);
 }
 public static Optional<WoodProperties> woodProperties(ResourceLocation key){return Optional.ofNullable(WOOD_PROPERTIES.get(key));}
 public static OptionalDouble woodHardnessKN(ResourceLocation key){var p=WOOD_PROPERTIES.get(key);return p==null||!Double.isFinite(p.jankaKN())?OptionalDouble.empty():OptionalDouble.of(p.jankaKN());}
 /** Same fixed-volume moisture basis as the Terrarian 12% reference. Actual moisture is caller-owned.
  * Softening below fibre saturation and fire scaling are bounded game proxies, not engineering design values. */
 public static Optional<WoodSample> woodAtMoisture(ResourceLocation key,double moistureKgKg){
  if(!Double.isFinite(moistureKgKg)||moistureKgKg<0||moistureKgKg>2.5)return Optional.empty();
  var m=MATERIALS.get(key);var p=WOOD_PROPERTIES.get(key);if(m==null||p==null)return Optional.empty();
  double ref=m.referenceMoistureKgKg(),delta=(Math.min(.30,moistureKgKg)-Math.min(.30,ref))*100;
  double hardness=p.jankaKN()*Math.max(.55,1-.015*delta),bending=p.bendingStrengthMPa()*Math.max(.55,1-.020*delta);
  double fire=m.flammability()*Math.max(0,Math.min(1,1-(moistureKgKg-ref)/.75));
  return Optional.of(new WoodSample(m,p,moistureKgKg,m.dryDensityKgM3()*(1+moistureKgKg),hardness,bending,fire));
 }
 private static void timber(String name,double densityAt12,double compression,double tension,double modulus,double janka,double bend,double green,double fire){
  m(name,Phase.SOLID,densityAt12/1.12,N,compression,tension,modulus,N,.12,.13,1700,N,fire==0?N:300,fire,.5,N,N);
  registerWoodProperties(id(name),new WoodProperties(janka,bend,green));
 }
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
 private static void soil(String n,double d,double p,double wet,double k,double perm){m(n,Phase.GRANULAR,d,N,N,N,N,p,wet,k,850,N,N,0,.6,perm,N);}
 private static void metal(String n,double d,double h,double t,double e,double k,double cp,double melt){m(n,Phase.SOLID,d,h,N,t,e,0,0,k,cp,melt,N,0,.4,0,N);}
 private static void liquid(String n,double d,double k,double cp,double visc){m(n,Phase.LIQUID,d,N,N,N,N,N,0,k,cp,N,N,0,N,N,visc);}
 static {
  m("air",Phase.GAS,1.204,N,N,N,N,N,0,.026,1005,N,N,0,N,N,1.81e-5);
  // Nominal mineral density / Mohs only. Unmeasured engineering parameters remain unknown.
  m("hematite",Phase.SOLID,5260,5.5,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("magnetite",Phase.SOLID,5170,5.5,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("cassiterite",Phase.SOLID,6900,6.5,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("galena",Phase.SOLID,7500,2.5,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("sphalerite",Phase.SOLID,4000,3.8,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("pyrite",Phase.SOLID,5000,6.2,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("malachite",Phase.SOLID,3950,3.8,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("bauxite",Phase.SOLID,2500,2.5,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("fluorite",Phase.SOLID,3180,4,N,N,N,N,0,N,N,N,N,0,N,N,N);
  m("cinnabar",Phase.SOLID,8100,2.2,N,N,N,N,0,N,N,N,N,0,N,N,N);
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
  rock("amethyst",2650,7,120,8,70,.01,5);
  rock("prismarine",2450,5,80,5,30,.12,1.5);
  rock("purpur",2300,5,70,4,25,.15,1.3);
  rock("bedrock",2900,N,N,N,N,.01,2.5);
  rock("glowstone",1900,N,20,2,10,.25,.7);
  rock("dripstone",2600,3.5,50,4,25,.12,1.8);
  rock("blackstone",2850,6,150,9,55,.05,2);
  rock("nether_brick",2050,5,45,4,20,.14,1);
  rock("coral",1550,3.5,12,1.5,7,.42,.8);
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
  soil("soul_sand",1400,.48,.04,.45,8e-11);
  soil("concrete_powder",1600,.38,.03,.5,1e-11);
  // Canonical nominal reference at 12% moisture: align with Terrarian WoodPhysicsApi prior tables.
  timber("oak",710,48,90,11,5.9,100,.65,.66);
  timber("dark_oak",770,52,100,12,6.5,110,.68,.59);
  timber("spruce",430,36,65,10,1.8,68,.85,.86);
  timber("birch",650,45,95,13,4.8,105,.72,.72);
  timber("jungle",570,42,75,10,3.5,85,.90,.70);
  timber("acacia",790,55,105,14,7.6,125,.55,.56);
  timber("mangrove",870,55,90,13,7.8,120,.95,.48);
  timber("cherry",560,43,82,10,4.2,85,.62,.68);
  timber("bamboo",680,50,140,15,6.0,95,.65,.82);
  timber("crimson",760,45,80,10,N,105,.60,0);
  timber("warped",630,40,75,9,N,82,.70,0);
  timber("generic_wood",616,40,70,10,4,80,.75,.8);
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
  // Bulk dry textile/insulation profiles, not intrinsic single-fibre density.
  m("flax",Phase.SOLID,85,N,N,N,N,.90,.08,.048,1450,N,230,.85,.5,N,N);
  m("fur",Phase.SOLID,65,N,N,N,N,.93,.12,.040,1500,N,250,.7,.5,N,N);
  m("straw",Phase.SOLID,95,N,N,N,N,.90,.10,.055,1500,N,230,.9,.5,N,N);
  m("felt",Phase.SOLID,180,N,N,N,N,.80,.10,.050,1400,N,250,.6,.5,N,N);
  // Aquatic reference profiles use wet bulk density, not oven-dry tissue; extra moisture defaults to zero.
  m("fish_tissue",Phase.SOLID,1060,N,N,N,N,N,0,.50,3500,N,N,N,N,N,N);
  m("fish_scale",Phase.SOLID,1350,N,N,N,N,N,0,.40,2000,N,N,N,N,N,N);
  m("aquatic_shell",Phase.SOLID,2600,N,N,N,N,.04,0,1.5,900,N,N,0,N,N,N);
  m("crustacean_shell",Phase.SOLID,1450,N,N,N,N,N,0,.35,1500,N,N,N,N,N,N);
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
  if(p.contains("nether_brick"))return "nether_brick";
  if(p.contains("coral"))return "coral";
  if(p.contains("terracotta")||p.equals("flower_pot"))return "ceramic";
  for(String m:new String[]{"volcanic_tuff","limestone","shale","slate","gneiss","amethyst","prismarine","purpur","bedrock","glowstone","dripstone","blackstone","deepslate","granite","diorite","andesite","sandstone","basalt","calcite","obsidian","netherrack","end_stone","quartz","concrete","lapis","redstone","diamond","emerald","netherite","copper","iron","gold","coal","glass","leather","wool","bone","paper","brick","stone"})if(named(p,m)||p.equals(m+"s"))return m;
  if(named(p,"tuff"))return "volcanic_tuff";
  if(named(p,"bricks")||named(p,"terracotta")||p.equals("flower_pot"))return "brick";
  return null;
 }
 private static String ground(String p){
  if(p.equals("soul_sand")||p.equals("soul_soil"))return "soul_sand";
  if(p.endsWith("_concrete_powder"))return "concrete_powder";
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
  if(key.getNamespace().equals("worldlogic")&&p.endsWith("_ore")){
   String mineral=p.replaceFirst("^deepslate_","").replaceFirst("_ore$","");
   if(java.util.Set.of("hematite","magnetite","cassiterite","galena","sphalerite","pyrite","malachite","bauxite","fluorite","cinnabar").contains(mineral))
    return new ObjectProfile(List.of(new Component(id(p.startsWith("deepslate_")?"deepslate":"stone"),.88),new Component(id(mineral),.12)),Evidence.ESTIMATED);
  }
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
  if(p.equals("muddy_mangrove_roots"))return part("loam",.65,"mangrove",.35);
  if(p.endsWith("_leaves")||p.endsWith("_sapling")||p.endsWith("_flower")||p.endsWith("_roots")&&!p.equals("muddy_mangrove_roots"))return single("plant",blockVolume(p),e);
  if(w!=null)return single(w,blockVolume(p),e);
  if(p.equals("furnace")||p.equals("smoker")||p.equals("blast_furnace"))return part("stone",.52,"iron",.015);
  if(p.equals("anvil")||p.endsWith("_anvil"))return single("iron",.28,Evidence.ESTIMATED);
  if(p.equals("chain"))return single("iron",.025,Evidence.ESTIMATED);
  if(p.equals("rail")||p.endsWith("_rail"))return part("iron",.014,"generic_wood",.04);
  if(p.equals("redstone_torch")||p.equals("redstone_wall_torch"))return part("generic_wood",.004,"redstone",.001);
  if(p.equals("torch")||p.endsWith("_torch"))return part("generic_wood",.004,"coal",.001);
  if(p.equals("lantern")||p.equals("soul_lantern"))return part("iron",.003,"glass",.002);
  if(p.endsWith("_bed"))return part("generic_wood",.16,"wool",.20);
  if(p.endsWith("_candle")||p.equals("candle"))return single("wax",.001,Evidence.ESTIMATED);
  if(p.equals("honey_block"))return single("honey",1,Evidence.ESTIMATED);
  if(p.equals("slime_block"))return single("rubber_like",1,Evidence.ESTIMATED);
  if(p.equals("cauldron")||p.endsWith("_cauldron"))return single("iron",.13,Evidence.ESTIMATED);
  if(p.equals("decorated_pot"))return single("ceramic",.045,Evidence.ESTIMATED);
  if(p.equals("sponge")||p.equals("wet_sponge"))return single("plant",1,Evidence.ESTIMATED);
  if(p.equals("hay_block"))return single("straw",1,Evidence.BUILTIN);
  if(p.equals("crafting_table")||p.equals("chest")||p.equals("trapped_chest")||p.equals("barrel")||p.equals("bookshelf")||p.equals("ladder"))return single("generic_wood",p.equals("ladder")?.08:.35,Evidence.ESTIMATED);
  String g=ground(p);if(g!=null)return single(g,blockVolume(p),e);
  String s=solidName(p);if(s!=null)return single(s,blockVolume(p),e);
  if(p.equals("grass")||p.equals("short_grass")||p.equals("tall_grass")||p.equals("vine")||named(p,"kelp")||named(p,"seagrass")||p.equals("lily_pad")||p.equals("cactus")||p.equals("sugar_cane")||p.equals("bamboo_sapling"))return single("plant",.01,e);
  if(p.equals("water")||p.endsWith("_water")||p.equals("groundwater")||p.equals("lava"))return builtinFluid(key);
  return UNKNOWN;
 }
 public static ObjectProfile block(BlockState state){
  ResourceLocation key=BuiltInRegistries.BLOCK.getKey(state.getBlock());
  var p=EXACT.get(new Key(Domain.BLOCK,key));
  if(p==null)p=tagged(state);
  if(p==null)p=builtinBlock(key);
  if(p.evidence()==Evidence.UNKNOWN&&state.getBlock() instanceof net.minecraft.world.level.block.BushBlock)p=single("plant",.01,Evidence.ESTIMATED);
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
  return fluidVolumeM3(fluid,milliBuckets/1_000_000);
 }
 public static ObjectProfile fluidVolumeM3(Fluid fluid,double cubicMetres){
  if(!Double.isFinite(cubicMetres)||cubicMetres<=0)throw new IllegalArgumentException("fluid volume");
  return resize(fluid(fluid),cubicMetres);
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
  if(key.getNamespace().equals("worldlogic")&&java.util.Set.of("hematite","magnetite","cassiterite","galena","sphalerite","pyrite","malachite","bauxite","fluorite","cinnabar").contains(key.getPath()))return single(key.getPath(),.00008,Evidence.ESTIMATED);
  String p=key.getPath(),s=solidName(p);
  if(p.equals("stick"))return single("generic_wood",.00015,Evidence.ESTIMATED);
  if(p.equals("feather"))return single("plant",.000005,Evidence.ESTIMATED);
  if(p.equals("cod")||p.equals("salmon")||p.equals("tropical_fish")||p.equals("pufferfish"))return single("fish_tissue",.0003,Evidence.ESTIMATED);
  if(p.equals("nautilus_shell"))return single("aquatic_shell",.00008,Evidence.ESTIMATED);
  if(p.equals("turtle_scute"))return single("aquatic_shell",.00002,Evidence.ESTIMATED);
  if(p.equals("flint"))return single("stone",.00003,Evidence.ESTIMATED);
  if(p.equals("clay_ball"))return single("clay",.00015,Evidence.ESTIMATED);
  if(p.equals("brick")||p.equals("nether_brick"))return single(p.equals("brick")?"brick":"nether_brick",.001,Evidence.ESTIMATED);
  if(p.equals("flint_and_steel"))return part("iron",.000025,"stone",.000025);
  if(p.equals("shears"))return single("iron",.00008,Evidence.ESTIMATED);
  if(p.equals("arrow")||p.equals("spectral_arrow")||p.equals("tipped_arrow"))return part("generic_wood",.00004,"stone",.000006);
  if(p.equals("string"))return single("plant",.00001,Evidence.ESTIMATED);
  if(p.equals("paper"))return single("paper",.00002,Evidence.ESTIMATED);
  if(p.equals("book")||p.equals("writable_book")||p.equals("written_book")||p.equals("enchanted_book")||p.equals("guidebook"))return part("paper",.00035,"leather",.00008);
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
 private static ObjectProfile baseItem(ItemStack stack){
  if(stack.isEmpty())return EMPTY;
  var key=BuiltInRegistries.ITEM.getKey(stack.getItem());
  var p=EXACT.get(new Key(Domain.ITEM,key));if(p==null)p=tagged(stack);
  if(p!=null)return p;
  if(stack.getItem() instanceof BlockItem b){var profile=block(b.getBlock().defaultBlockState());return b.getBlock() instanceof net.minecraft.world.level.block.DoorBlock||b.getBlock() instanceof net.minecraft.world.level.block.BedBlock?resize(profile,2):profile;}
  p=builtinItem(key);
  if(p.evidence()==Evidence.UNKNOWN&&stack.get(net.minecraft.core.component.DataComponents.FOOD)!=null)return single("food",.00025,Evidence.ESTIMATED);
  return p;
 }
 /** Individual mass in kg per one item, supplied by the owning addon, not inferred from inventory.
  * Empty composition means unknown volume/thermal properties; known mass is still returned.
  * Nonempty composition specifies relative occupied volumes; Core scales them to authoritative mass. */
 public record IndividualItem(double massKg,List<Component> composition){
  public IndividualItem{
   if(!Double.isFinite(massKg)||massKg<=0)throw new IllegalArgumentException("individual mass");
   composition=List.copyOf(composition);
   for(var c:composition)if(!MATERIALS.containsKey(c.materialId()))throw new IllegalArgumentException("unknown constituent "+c.materialId());
  }
 }
 @FunctionalInterface public interface IndividualItemProvider{
  /** Receives a copy with count one, must be pure and return empty when no individual data exist. */
  Optional<IndividualItem> resolve(ItemStack singleItem);
 }
 public static void registerIndividualItemProvider(ResourceLocation itemId,IndividualItemProvider provider){
  if(INDIVIDUAL_ITEMS.putIfAbsent(Objects.requireNonNull(itemId),Objects.requireNonNull(provider))!=null)throw new IllegalArgumentException("duplicate individual provider "+itemId);
 }
 // Package-private fixture cleanup. Only the exact QA-owned registration is removed.
 static void removeQaFixture(ResourceLocation materialId,ResourceLocation itemId,IndividualItemProvider provider){
  if(!materialId.getNamespace().equals("worldlogic_qa"))throw new IllegalArgumentException("fixture namespace");
  INDIVIDUAL_ITEMS.remove(itemId,provider);
  WOOD_PROPERTIES.remove(materialId);MATERIALS.remove(materialId);
 }
 private static Optional<IndividualItem> individual(ItemStack stack){
  if(stack.isEmpty())return Optional.empty();
  var provider=INDIVIDUAL_ITEMS.get(BuiltInRegistries.ITEM.getKey(stack.getItem()));
  if(provider==null)return Optional.empty();
  var copy=stack.copy();copy.setCount(1);
  return Objects.requireNonNull(provider.resolve(copy));
 }
 public static Result evaluateIndividual(IndividualItem individual,State state){
  Objects.requireNonNull(individual);Objects.requireNonNull(state);
  if(individual.composition().isEmpty())return new Result(UNKNOWN,state,N,individual.massKg(),N,N,N);
  var nominal=new ObjectProfile(individual.composition(),Evidence.EXPLICIT);
  double mass=evaluate(nominal,state).massKg();
  if(!Double.isFinite(mass)||mass<=0)return new Result(UNKNOWN,state,N,individual.massKg(),N,N,N);
  var scaled=resize(nominal,individual.massKg()/mass);
  var r=evaluate(scaled,state);
  return new Result(scaled,state,r.volumeM3(),individual.massKg(),individual.massKg()/r.volumeM3(),r.heatCapacityJK(),r.specificHeatJkgK());
 }
 public static ObjectProfile item(ItemStack stack){
  var individual=individual(stack);
  return individual.isPresent()?evaluateIndividual(individual.get(),storedItemState(stack).orElse(State.reference())).object():baseItem(stack);
 }
 public static ObjectProfile stack(ItemStack stack){return stack.isEmpty()?EMPTY:resize(item(stack),stack.getCount());}
 /** Item payload only, not the frame or stand that supports it. */
 public static ObjectProfile displayedItem(net.minecraft.world.entity.decoration.ItemFrame frame){return stack(frame.getItem());}
 public static ObjectProfile droppedItem(net.minecraft.world.entity.item.ItemEntity entity){return stack(entity.getItem());}
 private static final String STATE_KEY="worldlogic_physical_state";
 /** Stored only when explicitly set by an addon. ItemStack components survive normal save/copy/drop/frame operations. */
 public static Optional<State> storedItemState(ItemStack stack){
  var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
  if(data==null)return Optional.empty();
  var tag=data.copyTag();
  if(!tag.contains(STATE_KEY,net.minecraft.nbt.Tag.TAG_COMPOUND))return Optional.empty();
  var physical=tag.getCompound(STATE_KEY);
  double temp=physical.contains("temperature_c",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)?physical.getDouble("temperature_c"):N;
  double moisture=physical.contains("moisture_kg_kg",net.minecraft.nbt.Tag.TAG_ANY_NUMERIC)?physical.getDouble("moisture_kg_kg"):N;
  try{return Optional.of(new State(temp,moisture));}catch(IllegalArgumentException ex){return Optional.empty();}
 }
 /** Writes only the namespaced physical field, preserving unrelated custom item data. Caller owns state updates. */
 public static void setItemState(ItemStack stack,State state){
  if(stack.isEmpty())throw new IllegalArgumentException("empty stack");
  var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
  var tag=data==null?new net.minecraft.nbt.CompoundTag():data.copyTag();
  if(state==null)tag.remove(STATE_KEY);
  else{
   var physical=new net.minecraft.nbt.CompoundTag();
   if(Double.isFinite(state.temperatureC()))physical.putDouble("temperature_c",state.temperatureC());
   if(Double.isFinite(state.moistureKgKg()))physical.putDouble("moisture_kg_kg",state.moistureKgKg());
   tag.put(STATE_KEY,physical);
  }
  if(tag.isEmpty())stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
  else stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
 }
 public static Result itemResult(ItemStack stack){
  var state=storedItemState(stack).orElse(State.reference());var i=individual(stack);
  return i.isPresent()?evaluateIndividual(i.get(),state):evaluate(baseItem(stack),state);
 }
 public static Result stackResult(ItemStack stack){
  var r=itemResult(stack);if(stack.isEmpty()||stack.getCount()==1)return r;
  int n=stack.getCount();return new Result(resize(r.object(),n),r.state(),r.volumeM3()*n,r.massKg()*n,r.bulkDensityKgM3(),r.heatCapacityJK()*n,r.specificHeatJkgK());
 }
 public static Result displayedItemResult(net.minecraft.world.entity.decoration.ItemFrame frame){return stackResult(frame.getItem());}
 public static Result droppedItemResult(net.minecraft.world.entity.item.ItemEntity entity){return stackResult(entity.getItem());}
 /** Pure aggregate. Heat capacity uses dry component mass plus absorbed water.
  * No averaging of strength, hardness or fracture limits across unrelated parts. */
 public static Result evaluate(ObjectProfile p,State state){
  Objects.requireNonNull(p);Objects.requireNonNull(state);
  if(p.evidence()==Evidence.UNKNOWN)return new Result(p,state,N,N,N,N,N);
  double v=p.volumeM3(),mass=0,heat=0;
  for(var c:p.components()){
   var effective=componentProperties(c,state);
   if(effective.isEmpty())return new Result(p,state,v,N,N,N,N);
   mass+=effective.get().massKg();heat+=effective.get().heatCapacityJK();
  }
  return new Result(p,state,v,mass,v>0?mass/v:N,heat,mass>0?heat/mass:N);
 }
 /** Loaded positions only. Waterlogged blocks do not silently include fluid; query fluidBlock separately.
  * Soil moisture is the existing Core cell estimate, not a new block-level storage or water budget. */
 public static Optional<Result> placed(ServerLevel level,BlockPos pos){return placed(level,pos,null);}
 /** Optional caller-provided object state; no per-block ticking or forced chunk loading. */
 public static Optional<Result> placed(ServerLevel level,BlockPos pos,State supplied){
  if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  return atState(level,pos,level.getBlockState(pos),supplied);
 }
 /** Caller-owned original state at an already loaded location, e.g. an excavation proxy. Does not mutate terrain. */
 public static Optional<Result> atState(ServerLevel level,BlockPos pos,BlockState b,State supplied){
  Objects.requireNonNull(b);
  if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  if(b.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)return Optional.of(evaluate(fluidBlock(b.getFluidState()),supplied==null?State.reference():supplied));
  if(supplied!=null)return Optional.of(evaluate(block(b),supplied));
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

 /** Homogeneous plane layer with specified thickness and caller-owned physical state. */
 public record ThermalLayer(ResourceLocation materialId,double thicknessM,State state){
  public ThermalLayer{Objects.requireNonNull(materialId);Objects.requireNonNull(state);if(!Double.isFinite(thicknessM)||thicknessM<=0)throw new IllegalArgumentException("layer thickness");}
 }
 public record ThermalResistance(double areaResistanceM2KW,double conductanceWm2K,double thicknessM){}
 /** Series conduction only: no convection, thermal bridges, radiation or contact-film resistance. */
 public static Optional<ThermalResistance> thermalResistance(List<ThermalLayer> layers){
  if(layers==null||layers.isEmpty())return Optional.empty();
  double r=0,d=0;
  for(var l:layers){
   var p=componentProperties(new Component(l.materialId(),l.thicknessM()),l.state());
   if(p.isEmpty()||!Double.isFinite(p.get().conductivityWmK())||p.get().conductivityWmK()<=0)return Optional.empty();
   r+=l.thicknessM()/p.get().conductivityWmK();d+=l.thicknessM();
  }
  return Double.isFinite(r)&&Double.isFinite(d)&&r>0?Optional.of(new ThermalResistance(r,1/r,d)):Optional.empty();
 }
 /** Calibrated nominal contact stopping distance (not an injury calculation or actual collision observation).
  * Material and supporting geometry must both be recognized; unknown/complex geometries return empty. */
 public record SurfaceContact(double stoppingDistanceM,double moistureKgKg,ResourceLocation materialId,Evidence evidence){}
 public static Optional<SurfaceContact> surfaceContact(ServerLevel level,BlockPos pos){
  if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  var b=level.getBlockState(pos);
  if(!b.getFluidState().isEmpty()||b.isAir())return Optional.empty();
  var r=placed(level,pos);if(r.isEmpty())return Optional.empty();
  var p=r.get().object();if(p.components().size()!=1)return Optional.empty();
  var c=p.components().get(0);var m=MATERIALS.get(c.materialId());if(m==null)return Optional.empty();
  if(!m.id().getNamespace().equals("worldlogic")&&WOOD_PROPERTIES.get(m.id())==null)return Optional.empty();
  String n=m.id().getPath();double distance;
  boolean soft=n.equals("snow")||n.equals("wool")||n.equals("straw")||m.phase()==Phase.GRANULAR;
  if(soft){
   if(!(b.getBlock() instanceof net.minecraft.world.level.block.SnowLayerBlock)&&!b.isCollisionShapeFullBlock(level,pos)&&c.volumeM3()<.9)return Optional.empty();
   var below=pos.below();
   if(level.isOutsideBuildHeight(below)||!level.hasChunkAt(below)||!level.getBlockState(below).isCollisionShapeFullBlock(level,below))return Optional.empty();
   double wet=componentProperties(c,r.get().state()).map(ComponentResult::moistureKgKg).orElse(N);
   distance=switch(n){case "snow"->Math.min(.25,c.volumeM3()*.30);case "wool"->.08;case "straw"->.20;
    case "sand","white_sand","volcanic_sand","gravel"->.08;case "peat","humus"->.15;default->.06;};
   if(m.phase()==Phase.GRANULAR&&!n.equals("snow")&&Double.isFinite(wet))distance*=1+Math.min(.5,wet);
  }else{
   if(!b.isCollisionShapeFullBlock(level,pos))return Optional.empty();
   if(WOOD_PROPERTIES.containsKey(m.id()))distance=.025;
   else if(m.phase()==Phase.SOLID&&Double.isFinite(m.modulusGPa())&&m.modulusGPa()>=5)distance=.008;
   else return Optional.empty();
  }
  double moisture=componentProperties(c,r.get().state()).map(ComponentResult::moistureKgKg).orElse(N);
  return Optional.of(new SurfaceContact(distance,moisture,m.id(),Evidence.ESTIMATED));
 }
 /** Caller must supply actual effective moving mass and pre-contact normal speed. Never reads entity/inventory. */
 public static OptionalDouble impactEnergyJ(double effectiveMovingMassKg,double normalSpeedMps){
  if(!Double.isFinite(effectiveMovingMassKg)||effectiveMovingMassKg<=0||!Double.isFinite(normalSpeedMps)||normalSpeedMps<0)return OptionalDouble.empty();
  double e=.5*effectiveMovingMassKg*normalSpeedMps*normalSpeedMps;
  return Double.isFinite(e)?OptionalDouble.of(e):OptionalDouble.empty();
 }
 public record ItemContact(ComponentResult contactingComponent,double contactAreaM2,double kineticEnergyJ){}
 /** Caller explicitly selects the impacting component, contact area and moving mass; no automatic damage. */
 public static Optional<ItemContact> itemContact(ItemStack stack,int componentIndex,double contactAreaM2,double movingMassKg,double normalSpeedMps){
  if(!Double.isFinite(contactAreaM2)||contactAreaM2<=0)return Optional.empty();
  var r=itemResult(stack);var parts=r.object().components();
  if(componentIndex<0||componentIndex>=parts.size())return Optional.empty();
  var energy=impactEnergyJ(movingMassKg,normalSpeedMps);if(energy.isEmpty())return Optional.empty();
  return componentProperties(parts.get(componentIndex),r.state()).map(x->new ItemContact(x,contactAreaM2,energy.getAsDouble()));
 }
}
