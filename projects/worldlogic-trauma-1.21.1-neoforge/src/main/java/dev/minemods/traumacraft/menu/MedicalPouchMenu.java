package dev.minemods.traumacraft.menu;

import dev.minemods.traumacraft.registry.ModMenus;
import dev.minemods.traumacraft.registry.ModItems;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Server validates placements; second cell of 1x2 medkit is genuinely unavailable. */
public final class MedicalPouchMenu extends AbstractContainerMenu {
    private final Inventory playerInventory;
    private final ItemStack pouch;
    private final MedicalPouchInventory storage;
    private final int lockedIndex;

    // Client constructor: inventory contents come through normal menu slot sync.
    public MedicalPouchMenu(int id, Inventory inventory) {
        this(id,inventory,InteractionHand.MAIN_HAND);
    }
    public MedicalPouchMenu(int id, Inventory inventory, InteractionHand hand) {
        super(ModMenus.MEDICAL_POUCH.get(),id);
        this.playerInventory=inventory;
        this.lockedIndex=hand==InteractionHand.MAIN_HAND?inventory.selected:40;
        this.pouch=inventory.player.getItemInHand(hand);
        this.storage=new MedicalPouchInventory(pouch);

        for(int row=0;row<2;row++)for(int col=0;col<2;col++){
            int index=row*2+col;
            addSlot(new Slot(storage,index,70+col*18,25+row*18){
                @Override public boolean mayPlace(ItemStack stack) {
                    return super.mayPlace(stack) && storage.canPlaceItem(index,stack);
                }
                @Override public boolean mayPickup(Player player) {
                    return index<2 || !MedicalPouchInventory.isOversized(storage.getItem(index-2));
                }
                @Override public int getMaxStackSize() { return 1; }
            });
        }

        for(int row=0;row<3;row++)for(int col=0;col<9;col++){
            int invIndex=col+row*9+9;
            addSlot(new Slot(inventory,invIndex,8+col*18,72+row*18){
                @Override public boolean mayPickup(Player player){return invIndex!=lockedIndex;}
                @Override public boolean mayPlace(ItemStack stack){return invIndex!=lockedIndex;}
            });
        }
        for(int col=0;col<9;col++){
            int invIndex=col;
            addSlot(new Slot(inventory,invIndex,8+col*18,130){
                @Override public boolean mayPickup(Player player){return invIndex!=lockedIndex;}
                @Override public boolean mayPlace(ItemStack stack){return invIndex!=lockedIndex;}
            });
        }
    }

    public boolean reserved(int index) {
        return index>=2 && index<=3 && MedicalPouchInventory.isOversized(storage.getItem(index-2));
    }
    @Override public boolean stillValid(Player player) {
        return player.isAlive() && player.getInventory().getItem(lockedIndex)==pouch
            && pouch.is(ModItems.MEDICAL_POUCH.get());
    }

    // Deliberately disable Shift-click until all special-size transfer cases are tested.
    // Normal pickup, put-down, count splitting and player slots still work.
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
