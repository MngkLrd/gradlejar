package dev.minemods.hydration;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import java.util.HashSet;
import java.util.Locale;

public final class VesselItem extends Item {
 private static final String KEY="hydration_vessel";
 public final int capacity;
 public final boolean heatproof;
 public VesselItem(int capacity,boolean heatproof){super(new Properties().stacksTo(1));this.capacity=capacity;this.heatproof=heatproof;}
 public WaterBatch read(ItemStack stack){
  var custom=stack.get(DataComponents.CUSTOM_DATA);
  if(custom==null)return WaterBatch.empty();
  var n=custom.copyTag().getCompound(KEY);
  int ml=n.getInt("ml");if(ml<=0)return WaterBatch.empty();
  ml=Math.min(capacity,ml);
  try{
   var q=WaterBalance.Quality.valueOf(n.getString("quality"));
   var origins=new HashSet<String>();for(Tag t:n.getList("origins",Tag.TAG_STRING))origins.add(t.getAsString());
   double salt=n.getDouble("salt");if(salt>=1)q=WaterBalance.Quality.DESALINATE;
   return new WaterBatch(ml,q,salt,n.getDouble("risk"),origins);
  }catch(IllegalArgumentException ex){return new WaterBatch(ml,WaterBalance.Quality.UNKNOWN,0,1);}
 }
 public void write(ItemStack stack,WaterBatch b){
  if(b.ml()>capacity)throw new IllegalArgumentException("Vessel overflow");
  CustomData.update(DataComponents.CUSTOM_DATA,stack,tag->{
   if(b.ml()==0){tag.remove(KEY);return;}
   CompoundTag n=new CompoundTag();n.putInt("ml",b.ml());n.putString("quality",b.quality().name());n.putDouble("salt",b.salinity());n.putDouble("risk",b.risk());
   var origins=new ListTag();b.origins().stream().sorted().forEach(x->origins.add(StringTag.valueOf(x)));n.put("origins",origins);tag.put(KEY,n);
  });
 }
 @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
  ItemStack stack=player.getItemInHand(hand);
  if(hand!=InteractionHand.MAIN_HAND)return InteractionResultHolder.pass(stack);
  var b=read(stack);
  ItemStack off=player.getOffhandItem();
  if(player.isShiftKeyDown()&&off.getItem() instanceof VesselItem target){
   if(!level.isClientSide){
    var t=WaterBatch.transfer(b,target.read(off),b.ml(),target.capacity);
    if(t.moved()>0){write(stack,t.source());target.write(off,t.target());player.getInventory().setChanged();}
   }
   return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
  }
  if(player.isShiftKeyDown()&&off.is(HydrationItems.FILTER.get())){
   if(!level.isClientSide&&b.ml()>0&&b.ml()<=3000&&b.risk()>.01){
    write(stack,WaterTreatment.filter(b));if(!player.isCreative())off.shrink(1);player.getInventory().setChanged();
   }
   return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
  }
  var hit=player.pick(Math.min(4.5,player.blockInteractionRange()),1f,true);
  boolean water=hit instanceof net.minecraft.world.phys.BlockHitResult h&&hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK&&level.getFluidState(h.getBlockPos()).is(net.minecraft.tags.FluidTags.WATER);
  if(water){
   if(player instanceof ServerPlayer p){
    var profile=Hydration.targeted(p);
    if(profile!=null&&profile.sourceBlock()&&b.ml()<capacity){
     var source=WaterBottle.fromSource(profile);var pool=new WaterBatch(capacity,source.quality(),source.salinity(),source.risk(),source.origins());
     var t=WaterBatch.transfer(pool,b,capacity,capacity);write(stack,t.target());p.getInventory().setChanged();
     level.playSound(null,p.getX(),p.getY(),p.getZ(),SoundEvents.BOTTLE_FILL,SoundSource.PLAYERS,1f,1f);
    }
   }
   return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
  }
  if(b.ml()<=0)return InteractionResultHolder.pass(stack);
  if(player instanceof ServerPlayer p&&!WaterBalance.canDrink(Hydration.state(p)))return InteractionResultHolder.fail(stack);
  player.startUsingItem(hand);return InteractionResultHolder.consume(stack);
 }
 @Override public int getUseDuration(ItemStack stack,LivingEntity entity){return 24;}
 @Override public UseAnim getUseAnimation(ItemStack stack){return UseAnim.DRINK;}
 @Override public ItemStack finishUsingItem(ItemStack stack,Level level,LivingEntity entity){
  if(entity instanceof ServerPlayer p&&Hydration.active(p)&&WaterBalance.canDrink(Hydration.state(p))){
   var b=read(stack);int ml=Math.min(250,b.ml());if(ml>0){
    var t=WaterBatch.transfer(b,WaterBatch.empty(),ml,ml);
    Hydration.applyDrink(p,b.quality(),ml,b.risk());if(!p.isCreative())write(stack,t.source());p.getInventory().setChanged();
   }
  }
  return stack;
 }
 @Override public void appendHoverText(ItemStack stack,Item.TooltipContext ctx,java.util.List<Component> lines,TooltipFlag flag){
  var b=read(stack);lines.add(Component.translatable("hydration.vessel.volume",b.ml(),capacity));
  if(b.ml()>0){
   lines.add(Component.translatable("hydration.water.quality."+b.quality().name().toLowerCase(Locale.ROOT)));
   lines.add(Component.translatable("hydration.vessel.properties",String.format(Locale.ROOT,"%.1f",b.salinity()),Math.round(b.risk()*100)));
  }
 }
 @Override public boolean isBarVisible(ItemStack stack){return read(stack).ml()>0;}
 @Override public int getBarWidth(ItemStack stack){return Math.round(13f*read(stack).ml()/capacity);}
 @Override public int getBarColor(ItemStack stack){return 0x3799d4;}
}
