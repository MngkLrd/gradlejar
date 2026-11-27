package dev.minemods.hydration;

import dev.minemods.worldlogic.api.WorldLogicApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.HashSet;
import java.util.Set;

public final class TankEntity extends BlockEntity {
 private WaterBatch water=WaterBatch.empty();
 private int filterCharge;
 public TankEntity(BlockPos p,BlockState s){super(WaterInfrastructure.TANK_ENTITY.get(),p,s);}
 public TankBlock.Kind kind(){return ((TankBlock)getBlockState().getBlock()).kind;}
 public int capacity(){return kind().capacity;}
 public WaterBatch water(){return water;}
 private boolean exposed(){return kind()!=TankBlock.Kind.BARREL;}
 private long now(){return level instanceof ServerLevel l?l.getServer().overworld().getGameTime():-1;}
 private void refresh(long now,boolean force){if(water.ml()==0)return;if(force||water.needsRefresh(now)){var aged=water.aged(now,exposed());if(!aged.equals(water)){water=aged;setChanged();}}}
 public int charge(){return filterCharge;}
 public boolean filter(){return kind()==TankBlock.Kind.FILTER;}
 public String diagnostic(){return "Hydration tank="+kind()+" volume="+water.mB()+" mB capacity="+capacity()+" mB quality="+water.quality()+" charge="+filterCharge+" mB";}
 public boolean installFilter(){
  if(!filter()||filterCharge>GravityFlow.FILTER_CAPACITY)return false;
  filterCharge+=GravityFlow.FILTER_CAPACITY;changed();return true;
 }
 public void setWater(WaterBatch b){if(b.ml()>capacity())throw new IllegalArgumentException("Tank overflow");water=b;changed();}
 private void changed(){
  setChanged();
  if(level instanceof ServerLevel l&&l.getBlockState(worldPosition).getBlock() instanceof TankBlock){
   int fill=water.ml()==0?0:Math.min(3,1+(water.ml()-1)*3/capacity());
   var state=l.getBlockState(worldPosition);
   if(state.getValue(TankBlock.FILL)!=fill)l.setBlock(worldPosition,state.setValue(TankBlock.FILL,fill),Block.UPDATE_CLIENTS);
  }
 }
 public WaterBatch insert(WaterBatch input){
  long now=now();refresh(now,true);
  if(input.updatedTick()<0)input=input.aged(now,false);
  var t=WaterBatch.transfer(input,water,input.ml(),capacity());
  if(t.moved()>0)setWater(t.target());
  return t.source();
 }
 public WaterBatch extract(WaterBatch target,int capacity,int max){
  refresh(now(),true);
  var t=GravityFlow.transfer(water,target,capacity,max,filterCharge,filter());
  if(t.moved()>0){filterCharge=t.remainingFilter();setWater(t.source());}
  return t.target();
 }
 @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider registries){
  super.saveAdditional(n,registries);
  n.putInt("volume",water.ml());n.putString("quality",water.quality().name());n.putDouble("salt",water.salinity());n.putDouble("risk",water.risk());n.putLong("updated_tick",water.updatedTick());n.putInt("filter_charge",filterCharge);
  var origins=new ListTag();water.origins().stream().sorted().forEach(x->origins.add(StringTag.valueOf(x)));n.put("origins",origins);
 }
 @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider registries){
  super.loadAdditional(n,registries);
  int ml=Math.max(0,Math.min(capacity(),n.getInt("volume")));
  filterCharge=filter()?Math.max(0,Math.min(2*GravityFlow.FILTER_CAPACITY,n.getInt("filter_charge"))):0;
  if(ml==0){water=WaterBatch.empty();return;}
  try{
   if(!n.contains("salt")||!n.contains("risk"))throw new IllegalArgumentException("Missing profile");
   var origins=new HashSet<String>();for(Tag t:n.getList("origins",Tag.TAG_STRING))origins.add(t.getAsString());
   double salt=n.getDouble("salt");var q=salt>=1?WaterBalance.Quality.DESALINATE:WaterBalance.Quality.valueOf(n.getString("quality"));
   water=new WaterBatch(ml,q,salt,n.getDouble("risk"),origins,n.contains("updated_tick")?Math.max(-1,n.getLong("updated_tick")):-1);
  }catch(IllegalArgumentException ex){water=new WaterBatch(ml,WaterBalance.Quality.UNKNOWN,0,1);}
 }
 public static void tick(Level level,BlockPos p,BlockState s,TankEntity tank){
  if(!(level instanceof ServerLevel l)||Math.floorMod(l.getGameTime()+p.asLong(),20)!=0)return;
  tank.step(l);
 }
 void step(ServerLevel l){
  refresh(now(),false);
  if(water.ml()<capacity()){
   if(kind()==TankBlock.Kind.COLLECTOR){
    BlockPos top=worldPosition.above();
    if(l.hasChunkAt(top)&&l.getBlockState(top).isAir()&&l.canSeeSky(top)&&l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,top.getX(),top.getZ())<=top.getY()){
     var at=WorldLogicApi.atmosphere(l,top);
     int amount=at.map(a->GravityFlow.rainMl(!a.environment().covered(),a.precipitation()==WorldLogicApi.Precipitation.RAIN,a.precipitationIntensity())).orElse(0);
     if(amount>0)insert(new WaterBatch(amount,WaterBalance.Quality.TREAT_RECOMMENDED,0,.20,Set.of("RAIN")));
    }
   }else if(kind()==TankBlock.Kind.INTAKE){
    var above=worldPosition.above();
    if(l.hasChunkAt(above)&&l.getFluidState(above).is(FluidTags.WATER)&&l.getFluidState(above).isSource()&&l.getBlockState(above).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock){
     var profile=WorldLogicApi.waterProfile(l,above);
     if(profile.quality()!=WorldLogicApi.WaterQuality.NOT_WATER){
      var b=WaterBottle.fromSource(profile);insert(b.withVolume(GravityFlow.RATE));
     }
    }
   }
  }
  if(water.ml()==0||(filter()&&filterCharge==0))return;
  for(Direction d:Direction.values()){
   if(d==Direction.UP)continue;
   var target=findOutlet(l,worldPosition.relative(d),worldPosition.getY(),d.getOpposite());
   if(target==null)continue;
   long now=now();refresh(now,true);target.refresh(now,true);
   var result=GravityFlow.move(water,target.water,target.capacity(),worldPosition.getY(),target.worldPosition.getY(),filterCharge,filter());
   if(result.moved()>0){filterCharge=result.remainingFilter();setWater(result.source());target.setWater(result.target());return;}
  }
 }
 static TankEntity findOutlet(ServerLevel l,BlockPos start,int sourceY,Direction inlet){
  BlockPos pos=start;var seen=new HashSet<BlockPos>();
  for(int n=0;n<GravityFlow.MAX_PIPE_LENGTH;n++){
   if(!l.hasChunkAt(pos)||pos.getY()>sourceY||!seen.add(pos))return null;
   var state=l.getBlockState(pos);
   if(!(state.getBlock() instanceof ClayPipeBlock)||l.hasNeighborSignal(pos)||state.getValue(ClayPipeBlock.IN)!=inlet)return null;
   Direction out=state.getValue(ClayPipeBlock.OUT);if(out==Direction.UP||out==inlet)return null;
   inlet=out.getOpposite();pos=pos.relative(out);
   if(!l.hasChunkAt(pos))return null;
   if(l.getBlockEntity(pos) instanceof TankEntity target)return pos.getY()<sourceY?target:null;
  }
  return null;
 }
}
