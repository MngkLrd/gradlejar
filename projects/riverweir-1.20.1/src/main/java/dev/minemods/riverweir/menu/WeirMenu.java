package dev.minemods.riverweir.menu;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.blockentity.WeirPostBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class WeirMenu extends AbstractContainerMenu {
    private final BlockPos masterPos;
    public WeirMenu(int id, Inventory inv, FriendlyByteBuf buffer) {this(id,inv,buffer.readBlockPos());}
    public WeirMenu(int id, Inventory inv, BlockPos pos) {
        super(RiverWeirMod.WEIR_MENU.get(),id);
        this.masterPos=pos;
        IItemHandler handler=inv.player.level().getBlockEntity(pos) instanceof WeirPostBlockEntity be ? be.items : new ItemStackHandler(9);
        for(int i=0;i<9;i++) addSlot(new SlotItemHandler(handler,i,8+i*18,18) {
            @Override public boolean mayPlace(ItemStack stack) {return false;}
        });
        for(int row=0;row<3;row++) for(int col=0;col<9;col++)
            addSlot(new Slot(inv,col+row*9+9,8+col*18,86+row*18));
        for(int col=0;col<9;col++) addSlot(new Slot(inv,col,8+col*18,144));
    }
    @Override public boolean stillValid(Player player) {
        if (!(player.level().getBlockEntity(masterPos) instanceof WeirPostBlockEntity be) || !be.master || be.other==null) return false;
        double x=Mth.clamp(player.getX(),Math.min(masterPos.getX(),be.other.getX()),Math.max(masterPos.getX(),be.other.getX())+1);
        double z=Mth.clamp(player.getZ(),Math.min(masterPos.getZ(),be.other.getZ()),Math.max(masterPos.getZ(),be.other.getZ())+1);
        double y=Mth.clamp(player.getY(),be.netY-2,masterPos.getY()+2);
        double dx=player.getX()-x,dy=player.getY()-y,dz=player.getZ()-z;
        return dx*dx+dy*dy+dz*dz<=64;
    }
    @Override public ItemStack quickMoveStack(Player player,int index) {
        Slot slot=slots.get(index);
        if(!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),before=stack.copy();
        if(index<9) {
            if(!moveItemStackTo(stack,9,45,true)) return ItemStack.EMPTY;
        } else return ItemStack.EMPTY;
        if(stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player,stack);
        return before;
    }
}
