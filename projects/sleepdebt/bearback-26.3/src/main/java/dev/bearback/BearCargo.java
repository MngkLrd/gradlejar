package dev.bearback;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

public final class BearCargo {
    private static final String TAG="BearbackCargo";
    private static final int SLOTS=18;
    private static final Map<PolarBear,CargoContainer> INVENTORIES=new WeakHashMap<>();
    private BearCargo(){}
    public static CargoContainer inventory(PolarBear bear){
        return INVENTORIES.computeIfAbsent(bear,CargoContainer::new);
    }
    public static void open(ServerPlayer player,PolarBear bear){
        if(!bear.isAlive()||bear.isBaby()||!BearEvents.ownedBy(bear,player)
                ||!BearEvents.hasBags(bear)
                ||(player.getVehicle()!=bear&&player.distanceToSqr(bear)>64))return;
        CargoContainer cargo=inventory(bear);
        player.openMenu(new SimpleMenuProvider(
                (id,inv,user)->new ChestMenu(MenuType.GENERIC_9x2,id,inv,cargo,2),
                Component.translatable("container.bearback.saddlebags")));
    }
    public static void onDrops(LivingDropsEvent event){
        if(!(event.getEntity() instanceof PolarBear bear)||bear.level().isClientSide())return;
        if(BearEvents.hasBags(bear)){
            CargoContainer cargo=inventory(bear);
            for(int i=0;i<SLOTS;i++){
                ItemStack stack=cargo.removeItemNoUpdate(i);
                if(!stack.isEmpty())event.getDrops().add(new ItemEntity(bear.level(),
                        bear.getX(),bear.getY()+.35,bear.getZ(),stack));
            }
            cargo.setChanged();
            event.getDrops().add(new ItemEntity(bear.level(),bear.getX(),bear.getY()+.35,bear.getZ(),
                    new ItemStack(Bearback.SADDLEBAGS.get())));
        }
        if(BearEvents.isSaddled(bear))
            event.getDrops().add(new ItemEntity(bear.level(),bear.getX(),bear.getY()+.35,bear.getZ(),
                    new ItemStack(Items.SADDLE)));
    }
    public static final class CargoContainer extends SimpleContainer {
        private final WeakReference<PolarBear> bearer;
        private boolean loading=true;
        private CargoContainer(PolarBear bear){
            super(SLOTS);
            bearer=new WeakReference<>(bear);
            var ops=RegistryOps.create(NbtOps.INSTANCE,bear.level().registryAccess());
            ListTag items=bear.getPersistentData().getListOrEmpty(TAG);
            for(int i=0;i<items.size();i++){
                CompoundTag entry=items.getCompoundOrEmpty(i);
                int slot=entry.getIntOr("Slot",-1);
                if(slot<0||slot>=SLOTS)continue;
                var value=entry.get("Stack");
                if(value!=null){
                    ItemStack.CODEC.parse(ops,value).result().ifPresent(stack->super.setItem(slot,stack));
                }
            }
            loading=false;
        }
        @Override public void setChanged(){
            super.setChanged();
            PolarBear bear=bearer.get();
            if(loading||bear==null||bear.level().isClientSide())return;
            var ops=RegistryOps.create(NbtOps.INSTANCE,bear.level().registryAccess());
            ListTag items=new ListTag();
            for(int i=0;i<SLOTS;i++){
                ItemStack stack=getItem(i);
                if(stack.isEmpty())continue;
                int slot=i;
                ItemStack.CODEC.encodeStart(ops,stack).result().ifPresent(value->{
                    CompoundTag entry=new CompoundTag();
                    entry.putInt("Slot",slot);
                    entry.put("Stack",value);
                    items.add(entry);
                });
            }
            bear.getPersistentData().put(TAG,items);
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player user){
            PolarBear bear=bearer.get();
            return bear!=null&&bear.isAlive()&&BearEvents.hasBags(bear)
                    &&(user.getVehicle()==bear||user.distanceToSqr(bear)<=64);
        }
    }
}
