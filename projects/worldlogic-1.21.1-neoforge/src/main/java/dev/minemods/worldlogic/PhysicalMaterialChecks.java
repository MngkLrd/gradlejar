package dev.minemods.worldlogic;

import dev.minemods.worldlogic.api.PhysicalMaterials;
import dev.minemods.worldlogic.api.PhysicalAddonChecks;
import dev.minemods.worldlogic.api.PhysicalMaterials.*;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluids;

public final class PhysicalMaterialChecks {
 private PhysicalMaterialChecks(){}
 private static ResourceLocation id(String key){return ResourceLocation.fromNamespaceAndPath("worldlogic",key);}
 private static void require(boolean value,String label){if(!value)throw new IllegalStateException("Physical materials: "+label);}
 private static void equal(double actual,double expected,String label){require(Math.abs(actual-expected)<1e-8*Math.max(1,Math.abs(expected)),label+" actual="+actual+" expected="+expected);}
 private static Result of(net.minecraft.world.level.block.Block b){return PhysicalMaterials.evaluate(PhysicalMaterials.block(b.defaultBlockState()),State.reference());}
 private static boolean material(ObjectProfile p,String name){return p.components().stream().anyMatch(c->c.materialId().equals(id(name)));}
 public static void verify(){
  PhysicalAddonChecks.verify();
  for(var a:NaturalFluids.families()){
   require(WaterRenderCompatibility.same(a.source.get(),net.minecraft.world.level.material.Fluids.WATER),"native/vanilla render equivalence");
   for(var b:NaturalFluids.families()){
    require(WaterRenderCompatibility.same(a.source.get(),b.flowing.get()),"continuous water render family");
    if(a!=b)require(!a.source.get().isSame(b.source.get()),"distinct fluid identity preserved");
   }
   require(!WaterRenderCompatibility.same(a.source.get(),net.minecraft.world.level.material.Fluids.LAVA),"lava not merged");
  }
  require(PhysicalMaterials.materials().size()>=65,"builtin registry");
  require(of(Blocks.IRON_BLOCK).massKg()>of(Blocks.OAK_LOG).massKg(),"iron vs timber density");
  require(of(Blocks.OAK_LOG).massKg()>of(Blocks.SPRUCE_LOG).massKg(),"independent wood species");
  require(PhysicalMaterials.woodHardnessKN(id("oak")).orElseThrow()>PhysicalMaterials.woodHardnessKN(id("spruce")).orElseThrow(),"wood hardness not Mohs");
  require(PhysicalMaterials.material(id("warped")).orElseThrow().flammability()==0,"vanilla nonburning fungal wood");
  for(var f:RockContent.FAMILIES){
   String n=f.id();
   for(String base:List.of(n,"polished_"+n,n+"_bricks")){
    require(material(of(RockContent.block(base)).object(),n),base+" material identity");
    var slab=RockContent.block(base+"_slab").defaultBlockState();
    var one=PhysicalMaterials.evaluate(PhysicalMaterials.block(slab),State.reference());
    var two=PhysicalMaterials.evaluate(PhysicalMaterials.block(slab.setValue(SlabBlock.TYPE,SlabType.DOUBLE)),State.reference());
    equal(one.volumeM3(),.5,base+" slab");equal(two.volumeM3(),1,base+" double slab");
    equal(two.massKg(),of(RockContent.block(base)).massKg(),base+" double slab mass");
    equal(of(RockContent.block(base+"_stairs")).volumeM3(),.75,base+" stair");
    require(material(of(RockContent.block(base+"_wall")).object(),n),base+" wall");
   }
  }
  for(String soil:SoilFarmland.IDS){
   var ground=PhysicalMaterials.block(Content.SOILS.get(soil).get().defaultBlockState());
   var tilled=PhysicalMaterials.block(SoilFarmland.block(soil).defaultBlockState());
   require(ground.evidence()!=Evidence.UNKNOWN&&tilled.evidence()!=Evidence.UNKNOWN,soil+" known");
   require(ground.components().get(0).materialId().equals(tilled.components().get(0).materialId()),soil+" cultivated material");
   equal(tilled.volumeM3(),.9375,soil+" cultivated volume");
  }
  require(material(of(Blocks.IRON_ORE).object(),"stone")&&material(of(Blocks.IRON_ORE).object(),"iron"),"ore host and inclusion");
  require(material(of(Blocks.DEEPSLATE_IRON_ORE).object(),"deepslate"),"deep host");
  var axe=new ItemStack(Items.IRON_AXE,1);
  var profile=PhysicalMaterials.item(axe);
  require(material(profile,"iron")&&material(profile,"generic_wood")&&profile.components().size()==2,"compound axe");
  var oak=new ItemStack(Items.OAK_PLANKS,3);
  equal(PhysicalMaterials.stackResult(oak).massKg(),PhysicalMaterials.itemResult(oak).massKg()*3,"stack scale");
  equal(PhysicalMaterials.itemResult(oak).massKg(),of(Blocks.OAK_PLANKS).massKg(),"placed vs item material");
  equal(PhysicalMaterials.stackResult(ItemStack.EMPTY).massKg(),0,"empty mass");
  equal(PhysicalMaterials.stackResult(ItemStack.EMPTY).volumeM3(),0,"empty volume");
  var soil=PhysicalMaterials.block(Blocks.DIRT.defaultBlockState());
  var dry=PhysicalMaterials.evaluate(soil,new State(20,0));var wet=PhysicalMaterials.evaluate(soil,new State(20,.2));
  equal(wet.massKg(),dry.massKg()*1.2,"added moisture mass");
  require(wet.heatCapacityJK()>dry.heatCapacityJK(),"water heat capacity");
  var c=soil.components().get(0);
  require(PhysicalMaterials.componentProperties(c,new State(20,.2)).orElseThrow().conductivityWmK()>PhysicalMaterials.componentProperties(c,new State(20,0)).orElseThrow().conductivityWmK(),"wet conductivity");
  PhysicalMaterials.setItemState(oak,new State(35,.3));
  require(PhysicalMaterials.storedItemState(oak.copy()).orElseThrow().equals(new State(35,.3)),"state survives copy");
  require(PhysicalMaterials.stackResult(oak).massKg()>of(Blocks.OAK_PLANKS).massKg()*3,"stored moisture");
  PhysicalMaterials.setItemState(oak,null);
  require(PhysicalMaterials.storedItemState(oak).isEmpty(),"state cleared");
  var fresh=PhysicalMaterials.evaluate(PhysicalMaterials.fluidVolume(Fluids.WATER,1000),State.reference());
  equal(fresh.volumeM3(),.001,"1000 mB liquid volume");
  require(fresh.massKg()>.99&&fresh.massKg()<1.01,"litre fresh water mass");
  var source=PhysicalMaterials.evaluate(PhysicalMaterials.fluidBlock(Fluids.WATER.defaultFluidState()),State.reference());
  equal(source.volumeM3(),1,"source voxel volume");
  for(var family:NaturalFluids.families()){
   var a=PhysicalMaterials.fluid(family.source.get());var b=PhysicalMaterials.fluid(family.flowing.get());
   require(a.evidence()!=Evidence.UNKNOWN&&a.components().equals(b.components()),family.id+" source/flowing match");
   require(PhysicalMaterials.block(family.block.get().defaultBlockState()).evidence()!=Evidence.UNKNOWN,family.id+" block");
   require(PhysicalMaterials.item(new ItemStack(family.bucket.get())).evidence()!=Evidence.UNKNOWN,family.id+" bucket");
  }
  var sea=PhysicalMaterials.evaluate(PhysicalMaterials.fluid(NaturalFluids.get(dev.minemods.worldlogic.api.WorldLogicApi.WaterKind.SEA).source.get()),State.reference());
  require(sea.bulkDensityKgM3()>source.bulkDensityKgM3(),"salt water density");
  var unknown=new ObjectProfile(List.of(),Evidence.UNKNOWN);
  require(Double.isNaN(PhysicalMaterials.evaluate(unknown,State.reference()).massKg()),"unknown not fake zero");
  boolean invalid=false;try{new State(20,-.1);}catch(IllegalArgumentException ex){invalid=true;}
  require(invalid,"negative moisture rejected");
  invalid=false;try{PhysicalMaterials.registerMaterial(PhysicalMaterials.material(id("iron")).orElseThrow());}catch(IllegalArgumentException ex){invalid=true;}
  require(invalid,"duplicate material rejected");
  // Legacy relative weight API is intentionally unchanged; no conversion into movement penalties.
  equal(Material.weight(new ItemStack(Items.IRON_BLOCK)),dev.minemods.worldlogic.api.WorldLogicApi.itemWeight(new ItemStack(Items.IRON_BLOCK)),"legacy API unchanged");
  int blocks=0,unknownBlocks=0,items=0,unknownItems=0,fluids=0,unknownFluids=0;
  for(var b:BuiltInRegistries.BLOCK){blocks++;if(PhysicalMaterials.block(b.defaultBlockState()).evidence()==Evidence.UNKNOWN)unknownBlocks++;}
  for(var i:BuiltInRegistries.ITEM){items++;if(PhysicalMaterials.item(new ItemStack(i)).evidence()==Evidence.UNKNOWN)unknownItems++;}
  for(var f:BuiltInRegistries.FLUID){fluids++;if(PhysicalMaterials.fluid(f).evidence()==Evidence.UNKNOWN)unknownFluids++;}
  org.slf4j.LoggerFactory.getLogger("WorldLogic").info("WORLDLOGIC_PHYSICAL_COVERAGE blocks={}/{} items={}/{} fluids={}/{}",blocks-unknownBlocks,blocks,items-unknownItems,items,fluids-unknownFluids,fluids);
 }
 public static void verifyLoaded(ServerLevel level){
  PhysicalAddonChecks.verifyLoaded(level);
  var outside=new BlockPos(0,level.getMaxBuildHeight(),0);
  require(PhysicalMaterials.placed(level,outside).isEmpty(),"out-of-height read");
  var pos=level.getSharedSpawnPos();
  if(level.hasChunkAt(pos)&&!level.isOutsideBuildHeight(pos)){
   require(PhysicalMaterials.placed(level,pos).isPresent(),"loaded position");
  }
 }
}
