package dev.minemods.worldlogic.api;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
/** Core-owned material/contact inputs for discrete excavation addons. No break progress, drops or mutation. */
public final class WorldLogicExcavation{
 private WorldLogicExcavation(){}
 public static final int REVISION=0;
 public enum Kind{ROCK_OR_ORE,SOIL,OTHER}
 public record Target(BlockState state,Kind kind,float vanillaResistance,boolean requiresCorrectTool,
  boolean toolSuitableForDrops,PhysicalMaterials.Result composition,List<PhysicalMaterials.ComponentResult> constituents,
  WorldLogicApi.GroundProfile soil){
  public Target{constituents=List.copyOf(constituents);}
 }
 /** J/m² is incident energy per contact area, NOT fracture toughness, stress, damage or failure threshold. */
 public record Strike(PhysicalMaterials.ItemContact contact,double incidentEnergyPerAreaJm2){}
 public interface Contract{
  Optional<Target> inspect(ServerLevel level,BlockPos pos,ItemStack tool);
  Optional<Strike> strike(ItemStack tool,int componentIndex,double contactAreaM2,double movingMassKg,double normalSpeedMps);
 }
 public static final WorldLogicCapabilities.Key<Contract> KEY=new WorldLogicCapabilities.Key<>(ResourceLocation.fromNamespaceAndPath("worldlogic","excavation"),1,Contract.class);
 public static void register(){WorldLogicCapabilities.register(KEY,REVISION,"worldlogic",new Contract(){
  public Optional<Target> inspect(ServerLevel level,BlockPos pos,ItemStack tool){return WorldLogicExcavation.inspect(level,pos,tool);}
  public Optional<Strike> strike(ItemStack tool,int index,double area,double mass,double speed){return WorldLogicExcavation.strike(tool,index,area,mass,speed);}
 });}
 public static Optional<Target> inspect(ServerLevel level,BlockPos pos,ItemStack tool){
  Objects.requireNonNull(tool);
  if(!level.getServer().isSameThread())throw new IllegalStateException("server thread required");
  if(level.isOutsideBuildHeight(pos)||!level.hasChunkAt(pos))return Optional.empty();
  var state=level.getBlockState(pos);
  if(state.isAir()||!state.getFluidState().isEmpty())return Optional.empty();
  float hardness=state.getDestroySpeed(level,pos);
  if(hardness<0||!Float.isFinite(hardness))return Optional.empty();
  Kind kind=state.is(BlockTags.MINEABLE_WITH_PICKAXE)?Kind.ROCK_OR_ORE:state.is(BlockTags.MINEABLE_WITH_SHOVEL)?Kind.SOIL:Kind.OTHER;
  // Tags are Minecraft tool roles, not a mineralogical diagnosis.
  var physical=PhysicalMaterials.placed(level,pos).orElseThrow();
  var parts=physical.object().components().stream().map(c->PhysicalMaterials.componentProperties(c,physical.state())).flatMap(Optional::stream).toList();
  return Optional.of(new Target(state,kind,hardness,state.requiresCorrectToolForDrops(),
   !state.requiresCorrectToolForDrops()||tool.isCorrectToolForDrops(state),physical,parts,WorldLogicApi.soilProfile(state)));
 }
 public static Optional<Strike> strike(ItemStack tool,int componentIndex,double contactAreaM2,double movingMassKg,double normalSpeedMps){
  return PhysicalMaterials.itemContact(tool,componentIndex,contactAreaM2,movingMassKg,normalSpeedMps)
   .flatMap(c->{double incident=c.kineticEnergyJ()/c.contactAreaM2();return Double.isFinite(incident)?Optional.of(new Strike(c,incident)):Optional.empty();});
 }
}
