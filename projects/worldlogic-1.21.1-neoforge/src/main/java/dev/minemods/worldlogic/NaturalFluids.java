package dev.minemods.worldlogic;
import java.util.*;
import java.util.function.Supplier;
import dev.minemods.worldlogic.api.WorldLogicApi.WaterKind;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.*;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.common.SoundActions;
import net.minecraft.sounds.SoundEvents;
public final class NaturalFluids{
 private NaturalFluids(){}
 private static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(Registries.FLUID,"worldlogic");
 private static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES,"worldlogic");
 private static final Map<WaterKind,Family> FAMILIES=new EnumMap<>(WaterKind.class);
 public static final class Family{
  public final WaterKind kind;
  public final String id;
  public final int tint;
  public final Supplier<FluidType> type;
  public final Supplier<FlowingFluid> source,flowing;
  public final Supplier<LiquidBlock> block;
  public final Supplier<Item> bucket;
  Family(WaterKind kind,String id,int tint){
   this.kind=kind;this.id=id;this.tint=tint;
   type=TYPES.register(id,()->new FluidType(FluidType.Properties.create().descriptionId("fluid.worldlogic."+id)
     .density(kind==WaterKind.SEA?1025:kind==WaterKind.BRACKISH?1010:1000).viscosity(1000)
     .canSwim(true).canDrown(true).canPushEntity(true).canExtinguish(true).canHydrate(true).supportsBoating(true)
     .canConvertToSource(true).fallDistanceModifier(0).motionScale(.014)
     .sound(SoundActions.BUCKET_FILL,SoundEvents.BUCKET_FILL).sound(SoundActions.BUCKET_EMPTY,SoundEvents.BUCKET_EMPTY)));
   source=FLUIDS.register(id,()->new BaseFlowingFluid.Source(properties()));
   flowing=FLUIDS.register("flowing_"+id,()->new BaseFlowingFluid.Flowing(properties()));
   block=Content.BLOCKS.register(id,()->new LiquidBlock(source.get(),BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
   bucket=Content.ITEMS.register(id+"_bucket",()->new BucketItem(source.get(),new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)));
  }
  private BaseFlowingFluid.Properties properties(){return new BaseFlowingFluid.Properties(type,source,flowing).block(block).bucket(bucket).slopeFindDistance(4).levelDecreasePerBlock(1).tickRate(5).explosionResistance(100);}
 }
 static{
  add(WaterKind.SEA,"sea_water",0xff315f91);
  add(WaterKind.RIVER,"river_water",0xff417e9b);
  add(WaterKind.LAKE,"lake_water",0xff3d7697);
  add(WaterKind.SPRING,"spring_water",0xff4c9fbc);
  add(WaterKind.GROUNDWATER,"groundwater",0xff527e96);
  add(WaterKind.WETLAND,"wetland_water",0xff647d56);
  add(WaterKind.POND,"pond_water",0xff577e79);
  add(WaterKind.BRACKISH,"brackish_water",0xff547f80);
 }
 private static void add(WaterKind k,String id,int tint){FAMILIES.put(k,new Family(k,id,tint));}
 public static Collection<Family> families(){return Collections.unmodifiableCollection(FAMILIES.values());}
 public static Family get(WaterKind k){return FAMILIES.get(k);}
 public static WaterKind kind(Fluid fluid){
  for(var f:FAMILIES.values())if(fluid==f.source.get()||fluid==f.flowing.get())return f.kind;
  return WaterKind.UNKNOWN;
 }
 public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
}
