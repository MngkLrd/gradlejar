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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

/** Server-owned 18 slots, serialized into the owning bear's persistent entity NBT. */
public final class BearCargo {
    private static final String TAG="BearbackCargo";
    public static final int SLOTS=18;
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
        if(!(event.getEntity() instanceof PolarBear bear)||bear.level().isClientSide)return;
        if(BearEvents.hasBags(bear)){
            CargoContainer cargo=inventory(bear);
            for(int i=0;i<SLOTS;i++){
                ItemStack stack=cargo.removeItemNoUpdate(i);
                if(!stack.isEmpty())
                    event.getDrops().add(new ItemEntity(bear.level(),bear.getX(),bear.getY()+.35,bear.getZ(),stack));
            }
            cargo.setChanged();
            event.getDrops().add(new ItemEntity(bear.level(),bear.getX(),bear.getY()+.35,bear.getZ(),
                    new ItemStack(Bearback.SADDLEBAGS.get())));
        }
        if(BearEvents.isSaddled(bear)){
            event.getDrops().add(new ItemEntity(bear.level(),bear.getX(),bear.getY()+.35,bear.getZ(),
                    new ItemStack(Items.SADDLE)));
        }
    }
    public static final class CargoContainer extends SimpleContainer {
        private final WeakReference<PolarBear> owner;
        private boolean loading=true;
        private CargoContainer(PolarBear bear){
            super(SLOTS);
            owner=new WeakReference<>(bear);
            CompoundTag data=bear.getPersistentData();
            if(data.contains(TAG,Tag.TAG_LIST))fromTag(data.getList(TAG,Tag.TAG_COMPOUND));
            loading=false;
        }
        @Override public void setChanged(){
            super.setChanged();
            PolarBear bear=owner.get();
            if(!loading&&bear!=null&&!bear.level().isClientSide){
                bear.getPersistentData().put(TAG,createTag());
            }
        }
        @Override public boolean stillValid(net.minecraft.world.entity.player.Player user){
            PolarBear bear=owner.get();
            return bear!=null&&bear.isAlive()&&BearEvents.hasBags(bear)
                    &&(user.getVehicle()==bear||user.distanceToSqr(bear)<=64);
        }
    }
}
