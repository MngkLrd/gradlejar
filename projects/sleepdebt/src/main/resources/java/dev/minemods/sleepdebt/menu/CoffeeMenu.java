package dev.minemods.sleepdebt.menu;

import dev.minemods.sleepdebt.registry.ModMenus;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.function.Predicate;

public final class CoffeeMenu extends AbstractContainerMenu {
 private final Container container;
 private final ContainerData data;
 private static final int MACHINE_SLOTS=4;
 public CoffeeMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(MACHINE_SLOTS),new SimpleContainerData(2));}
 public CoffeeMenu(int id,Inventory inv,Container container,ContainerData data) {
  super(ModMenus.COFFEE.get(),id);
  checkContainerSize(container,MACHINE_SLOTS);
  checkContainerDataCount(data,2);
  this.container=container;this.data=data;
  container.startOpen(inv.player);
  addSlot(new RestrictedSlot(container,0,28,44,s->s.is(Items.WATER_BUCKET)));
  addSlot(new RestrictedSlot(container,1,55,44,s->s.is(ModItems.GROUNDS.get())||s.is(ModItems.DRIED_HERBS.get())));
  addSlot(new RestrictedSlot(container,2,82,44,s->s.is(Items.COAL)||s.is(Items.CHARCOAL)));
  addSlot(new Slot(container,3,136,44){
   @Override public boolean mayPlace(ItemStack stack){return false;}
  });
  for(int row=0;row<3;row++)for(int col=0;col<9;col++)
   addSlot(new Slot(inv,col+row*9+9,8+col*18,84+row*18));
  for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));
  addDataSlots(data);
 }
 public int progress(){return data.get(0);}
 public int total(){return Math.max(1,data.get(1));}
 @Override public boolean stillValid(Player p){return container.stillValid(p);}
 @Override public void removed(Player player){super.removed(player);container.stopOpen(player);}
 @Override public ItemStack quickMoveStack(Player player,int clicked){
  ItemStack empty=ItemStack.EMPTY;
  Slot slot=slots.get(clicked);
  if(!slot.hasItem())return empty;
  ItemStack source=slot.getItem();
  ItemStack snapshot=source.copy();
  if(clicked<MACHINE_SLOTS){
   if(!moveItemStackTo(source,MACHINE_SLOTS,slots.size(),true))return empty;
  }else{
   int target=source.is(Items.WATER_BUCKET)?0:
     (source.is(ModItems.GROUNDS.get())||source.is(ModItems.DRIED_HERBS.get()))?1:
     (source.is(Items.COAL)||source.is(Items.CHARCOAL))?2:-1;
   if(target>=0){
    if(!moveItemStackTo(source,target,target+1,false))return empty;
   }else if(clicked<31){
    if(!moveItemStackTo(source,31,40,false))return empty;
   }else if(!moveItemStackTo(source,4,31,false))return empty;
  }
  if(source.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
  if(source.getCount()==snapshot.getCount())return empty;
  slot.onTake(player,source);
  return snapshot;
 }
 private static final class RestrictedSlot extends Slot {
  private final Predicate<ItemStack> predicate;
  RestrictedSlot(Container c,int i,int x,int y,Predicate<ItemStack> predicate){super(c,i,x,y);this.predicate=predicate;}
  @Override public boolean mayPlace(ItemStack stack){return predicate.test(stack);}
 }
}

