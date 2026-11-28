package dev.minemods.hydration;
import dev.minemods.hydration.api.HydrationApi;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Optional;

/** Exact external water use; no intake, fatigue or foreign state writes. */
public final class WaterWithdrawal {
 private WaterWithdrawal(){}
 static record Plan(WaterBatch dose,ItemStack remaining){}
 static Optional<Plan> plan(ItemStack input,int volume,long now){
  if(input.isEmpty()||input.getCount()!=1||volume<=0)return Optional.empty();
  if(input.getItem() instanceof VesselItem v){
   var b=v.current(input,now);if(b.mB()<volume)return Optional.empty();
   var rest=input.copy();v.write(rest,b.withVolume(b.mB()-volume));return Optional.of(new Plan(b.withVolume(volume),rest));
  }
  var b=WaterBottle.read(input);if(b.isEmpty()||volume!=b.get().mB())return Optional.empty();
  return Optional.of(new Plan(b.get().aged(now,false),new ItemStack(Items.GLASS_BOTTLE)));
 }
 private static HydrationApi.LiquidStatus status(WaterBatch b){return new HydrationApi.LiquidStatus(b.mB(),b.quality().name(),b.salinity(),b.risk(),b.origins());}
 public static final class Quote {
  private final ServerPlayer owner;private final InteractionHand hand;private final ItemStack identity,snapshot;private final long tick;private final Plan plan;private boolean used;
  private Quote(ServerPlayer p,InteractionHand h,ItemStack stack,long now,Plan plan){owner=p;hand=h;identity=stack;snapshot=stack.copy();tick=now;this.plan=plan;}
  public HydrationApi.LiquidStatus water(){return status(plan.dose());}
  /** Detached result, not a mutable reference to the actual hand. */
  public ItemStack remainder(){return plan.remaining().copy();}
 }
 private static boolean valid(ServerPlayer p){return p!=null&&p.serverLevel().getServer().isSameThread()&&Hydration.active(p)&&!p.isUsingItem();}
 public static Optional<Quote> preview(ServerPlayer p,InteractionHand hand,int volume){
  if(!valid(p)||hand==null)return Optional.empty();var s=p.getItemInHand(hand);
  if(VesselTreatment.activeJob(p,s))return Optional.empty();
  long now=Hydration.clock(p);return plan(s,volume,now).map(x->new Quote(p,hand,s,now,x));
 }
 public static Optional<HydrationApi.LiquidStatus> commit(ServerPlayer p,Quote q){
  if(!valid(p)||q==null||q.used||q.owner!=p||q.tick!=Hydration.clock(p))return Optional.empty();
  var s=p.getItemInHand(q.hand);
  if(s!=q.identity||!ItemStack.matches(s,q.snapshot)||VesselTreatment.activeJob(p,s))return Optional.empty();
  q.used=true;p.setItemInHand(q.hand,q.plan.remaining().copy());p.getInventory().setChanged();
  return Optional.of(q.water());
 }
}
