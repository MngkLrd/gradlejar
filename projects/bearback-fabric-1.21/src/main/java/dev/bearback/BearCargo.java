package dev.bearback;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.animal.PolarBear;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BearCargo {
 private static final String TAG="BearbackCargo";
 private static final Map<PolarBear,CargoContainer> CACHE=new WeakHashMap<>();
 private BearCargo(){}
 public static CargoContainer inventory(PolarBear bear){return CACHE.computeIfAbsent(bear,CargoContainer::new);}
 public static void open(ServerPlayer player,PolarBear bear){
   if(!bear.isAlive()||bear.isBaby()||!BearEvents.owned(bear,player)||!BearData.bags(bear)
           ||(player.getVehicle()!=bear&&player.distanceToSqr(bear)>64))return;
   player.openMenu(new SimpleMenuProvider(
           (id,inv,user)->new ChestMenu(MenuType.GENERIC_9x2,id,inv,inventory(bear),2),
           Component.translatable("container.bearback.saddlebags")));
 }
 public static void drop(PolarBear bear){
   if(!(bear.level() instanceof net.minecraft.server.level.ServerLevel))return;
   if(BearData.bags(bear)){
       CargoContainer inv=inventory(bear);
       for(int i=0;i<18;i++){ItemStack s=inv.removeItemNoUpdate(i);if(!s.isEmpty())bear.spawnAtLocation(s);}
       inv.setChanged();bear.spawnAtLocation(new ItemStack(Bearback.SADDLEBAGS));BearData.bags(bear,false);
   }
   if(BearData.saddled(bear)){bear.spawnAtLocation(Items.SADDLE);BearData.saddle(bear,false);}
 }
 public static final class CargoContainer extends SimpleContainer {
   private final WeakReference<PolarBear> bearer;
   private boolean loading=true;
   private CargoContainer(PolarBear bear){
       super(18);bearer=new WeakReference<>(bear);
       CompoundTag nbt=BearData.get(bear);
       if(nbt.contains(TAG,Tag.TAG_LIST))fromTag(nbt.getList(TAG,Tag.TAG_COMPOUND),bear.level().registryAccess());
       loading=false;
   }
   @Override public void setChanged(){
       super.setChanged();PolarBear bear=bearer.get();
       if(!loading&&bear!=null&&!bear.level().isClientSide()){
           BearData.get(bear).put(TAG,createTag(bear.level().registryAccess()));BearData.changed(bear);
       }
   }
   @Override public boolean stillValid(net.minecraft.world.entity.player.Player p){
       PolarBear bear=bearer.get();
       return bear!=null&&bear.isAlive()&&BearData.bags(bear)
               &&(p.getVehicle()==bear||p.distanceToSqr(bear)<=64);
   }
 }
}
