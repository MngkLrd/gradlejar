package dev.minemods.sleepdebt.blockentity;
import dev.minemods.sleepdebt.registry.ModBlockEntities;
import dev.minemods.sleepdebt.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
public final class CoffeeMachineBlockEntity extends BlockEntity implements Container,MenuProvider {
 private NonNullList<ItemStack> items=NonNullList.withSize(27,ItemStack.EMPTY);
 private int progress=0;
 public CoffeeMachineBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.COFFEE_MACHINE.get(),pos,state);}
 public static void serverTick(Level level,BlockPos pos,BlockState state,CoffeeMachineBlockEntity machine){
  ItemStack water=machine.items.get(0),grounds=machine.items.get(1),fuel=machine.items.get(2),output=machine.items.get(3);
  boolean ready=water.is(Items.WATER_BUCKET)&&grounds.is(ModItems.GROUNDS.get())&&fuel.is(Items.COAL)
    &&(output.isEmpty()||(output.is(ModItems.COFFEE.get())&&output.getCount()<output.getMaxStackSize()));
  if(!ready){if(machine.progress>0){machine.progress=0;machine.setChanged();}return;}
  machine.progress++;
  if(machine.progress%15==0 && level instanceof ServerLevel server)
    server.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,pos.getX()+.5,pos.getY()+.93,pos.getZ()+.5,2,.08,.03,.08,.004);
  if(machine.progress>=200){
   machine.progress=0;grounds.shrink(1);fuel.shrink(1);
   machine.items.set(0,new ItemStack(Items.BUCKET));
   if(output.isEmpty())machine.items.set(3,new ItemStack(ModItems.COFFEE.get())); else output.grow(1);
   level.playSound(null,pos,SoundEvents.BREWING_STAND_BREW,SoundSource.BLOCKS,.8f,1f);
  }
  machine.setChanged();
 }
 @Override public int getContainerSize(){return 27;}
 @Override public boolean isEmpty(){return items.stream().allMatch(ItemStack::isEmpty);}
 @Override public ItemStack getItem(int slot){return items.get(slot);}
 @Override public ItemStack removeItem(int slot,int amount){
  ItemStack stack=ContainerHelper.removeItem(items,slot,amount);
  if(!stack.isEmpty())setChanged();return stack;
 }
 @Override public ItemStack removeItemNoUpdate(int slot){return ContainerHelper.takeItem(items,slot);}
 @Override public void setItem(int slot,ItemStack stack){items.set(slot,stack);setChanged();}
 @Override public boolean stillValid(Player p){return level!=null && level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5)<=64;}
 @Override public void clearContent(){items=NonNullList.withSize(27,ItemStack.EMPTY);setChanged();}
 @Override public Component getDisplayName(){return Component.translatable("container.sleepdebt.coffee_machine");}
 @Nullable @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return ChestMenu.threeRows(id,inv,this);}
 @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.loadAdditional(tag,provider);items=NonNullList.withSize(27,ItemStack.EMPTY);
  ContainerHelper.loadAllItems(tag,items,provider);progress=tag.getInt("progress");
 }
 @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){
  super.saveAdditional(tag,provider);ContainerHelper.saveAllItems(tag,items,provider);tag.putInt("progress",progress);
 }
}

