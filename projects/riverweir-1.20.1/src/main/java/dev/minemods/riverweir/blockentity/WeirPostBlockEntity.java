package dev.minemods.riverweir.blockentity;

import dev.minemods.riverweir.RiverWeirMod;
import dev.minemods.riverweir.logic.WeirLogic;
import dev.minemods.riverweir.menu.WeirMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public final class WeirPostBlockEntity extends BlockEntity implements MenuProvider {
    public final ItemStackHandler items=new ItemStackHandler(9) {
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    public BlockPos other;
    public boolean master;
    public int wear;
    public int netY;
    private long lastScan;

    public WeirPostBlockEntity(BlockPos pos,BlockState state) { super(RiverWeirMod.POST_BE.get(),pos,state); }
    public void link(BlockPos linked, boolean isMaster,int y) {
        other=linked.immutable();master=isMaster;wear=0;netY=y;lastScan=0;setChanged();
    }
    public void clearLink() { other=null;master=false;wear=0;netY=0;lastScan=0;setChanged(); }

    @Override protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if(other!=null) tag.putLong("OtherPost",other.asLong());
        tag.putBoolean("Master",master);
        tag.putInt("Wear",wear);
        tag.putInt("NetY",netY);
        tag.putLong("LastScan",lastScan);
        tag.put("Inventory",items.serializeNBT());
    }
    @Override public void load(CompoundTag tag) {
        super.load(tag);
        other=tag.contains("OtherPost")?BlockPos.of(tag.getLong("OtherPost")):null;
        master=tag.getBoolean("Master");
        wear=tag.getInt("Wear");
        netY=tag.getInt("NetY");
        lastScan=tag.getLong("LastScan");
        if(tag.contains("Inventory")) items.deserializeNBT(tag.getCompound("Inventory"));
    }
    @Override public Component getDisplayName() { return Component.translatable("container.riverweir.weir"); }
    @Nullable @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new WeirMenu(id,inventory,worldPosition);
    }
    public static void serverTick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,WeirPostBlockEntity be) {
        if (!(level instanceof ServerLevel server) || !be.master || be.other==null || be.wear>=100) return;
        long tick=server.getGameTime();
        if (tick-be.lastScan<100 || tick<be.lastScan) return;
        be.lastScan=tick;
        if (!WeirLogic.isFishingEnvironment(server,be)) return;
        if (tick%2400<100) WeirLogic.damage(server,be,1);
        if(be.wear<100) WeirLogic.catchNearbyFish(server,be);
    }
}
