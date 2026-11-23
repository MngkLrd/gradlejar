package dev.minemods.riverweir.menu;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import dev.minemods.riverweir.logic.WeirTier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class WeirMenu extends AbstractContainerMenu {
    private final BlockPos masterPos;
    private final int tierId;
    private final ContainerData data;
    private final int netSlots, rows;

    public WeirMenu(int id, Inventory inv, FriendlyByteBuf buf) {
        this(id,inv,buf.readBlockPos(),buf.readVarInt());
    }
    public WeirMenu(int id, Inventory inv, BlockPos pos, int tierId) {
        super(RiverWeirMod.WEIR_MENU.get(),id);
        this.masterPos=pos;
        this.tierId=tierId;
        netSlots=WeirTier.byId(tierId).storage;
        rows=netSlots/9;
        WeirPostBlockEntity be=inv.player.level().getBlockEntity(pos) instanceof WeirPostBlockEntity found?found:null;
        IItemHandler handler=be!=null?be.items:new ItemStackHandler(27);
        data=be!=null && !inv.player.level().isClientSide?new ContainerData(){
            @Override public int get(int index){
                return switch(index){
                    case 0 -> be.wear;
                    case 1 -> be.clog;
                    case 2 -> be.baitCharges;
                    default -> 0;
                };
            }
            @Override public void set(int index,int value){
                if(index==0)be.wear=value;
                else if(index==1)be.clog=value;
                else if(index==2)be.baitCharges=value;
            }
            @Override public int getCount(){return 3;}
        }:new SimpleContainerData(3);
        addDataSlots(data);
        for(int i=0;i<netSlots;i++)addSlot(new SlotItemHandler(handler,i,8+(i%9)*18,18+(i/9)*18){
            @Override public boolean mayPlace(ItemStack stack){return false;}
        });
        int y=104+(rows-1)*18;
        for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,8+c*18,y+r*18));
        for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,162+(rows-1)*18));
    }
    public int getRows(){return rows;}
    public int getCondition(){return Math.max(0,100-(100*data.get(0))/WeirTier.byId(tierId).endurance);}
    public int getClog(){return data.get(1);}
    public int getBaitCharges(){return data.get(2);}
    public WeirTier getTier(){return WeirTier.byId(tierId);}

    @Override public boolean stillValid(Player player){
        if(!(player.level().getBlockEntity(masterPos) instanceof WeirPostBlockEntity be)||!be.master||be.other==null)return false;
        double x=Mth.clamp(player.getX(),Math.min(masterPos.getX(),be.other.getX()),Math.max(masterPos.getX(),be.other.getX())+1);
        double z=Mth.clamp(player.getZ(),Math.min(masterPos.getZ(),be.other.getZ()),Math.max(masterPos.getZ(),be.other.getZ())+1);
        double y=Mth.clamp(player.getY(),be.netY-be.maxDepth(),masterPos.getY()+2);
        double dx=player.getX()-x,dy=player.getY()-y,dz=player.getZ()-z;
        return dx*dx+dy*dy+dz*dz<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        Slot slot=slots.get(index);
        if(!slot.hasItem())return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),original=stack.copy();
        if(index<netSlots){
            if(!moveItemStackTo(stack,netSlots,slots.size(),true))return ItemStack.EMPTY;
        }else return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player,stack);
        return original;
    }
}
