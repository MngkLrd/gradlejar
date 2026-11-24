package dev.mngklrd.hearthbound;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import javax.annotation.Nullable;

public final class VesselMenu extends AbstractContainerMenu {
    private final SimpleContainer preview=new SimpleContainer(5);
    private final ContainerData data;
    @Nullable private final VesselEntity vessel;
    public VesselMenu(int id,Inventory inventory){this(id,inventory,null);}
    public VesselMenu(int id,Inventory inventory,@Nullable VesselEntity vessel){
        super(Hearthbound.VESSEL_MENU.get(),id);
        this.vessel=vessel;
        this.data=vessel==null?new SimpleContainerData(7):new ContainerData(){
            @Override public int get(int i){return vessel.displayData(i);}
            @Override public void set(int i,int v){}
            @Override public int getCount(){return 7;}
        };
        for(int i=0;i<5;i++)addSlot(new Slot(preview,i,43+i*22,39){
            @Override public boolean mayPickup(Player p){return false;}
            @Override public boolean mayPlace(ItemStack s){return false;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,9+row*9+col,8+col*18,112+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,8+col*18,170));
        addDataSlots(data);refresh();
    }
    private void refresh(){if(vessel!=null)for(int i=0;i<5;i++)preview.setItem(i,vessel.preview(i));}
    @Override public void broadcastChanges(){refresh();super.broadcastChanges();}
    public int value(int i){return data.get(i);}
    @Override public boolean stillValid(Player p){return vessel==null||vessel.canUse(p);}
    @Override public boolean clickMenuButton(Player p,int id){if(id==0&&vessel!=null&&vessel.canUse(p)){vessel.menuToggle(p);return true;}return false;}
    @Override public void clicked(int index,int button,ClickType click,Player p){
        if(index>=0&&index<5){
            if(vessel!=null&&click==ClickType.PICKUP&&vessel.canUse(p)){
                ItemStack carried=getCarried();
                if(!carried.isEmpty())vessel.addIngredient(p,carried,button==0);
                else if(button==1)vessel.removeIngredient(p,index);
                broadcastChanges();
            }
            return;
        }
        super.clicked(index,button,click,p);
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(vessel==null||index<5||index>=slots.size()||!vessel.canUse(player))return ItemStack.EMPTY;
        Slot slot=slots.get(index);ItemStack s=slot.getItem();if(s.isEmpty())return ItemStack.EMPTY;
        ItemStack original=s.copy();if(vessel.addIngredient(player,s,true)==0)return ItemStack.EMPTY;
        if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        broadcastChanges();return original;
    }
}
